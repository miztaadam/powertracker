import { expect, test, type Locator, type Page } from '@playwright/test'
import { openOnWednesday, pastTheTapWindow, tabBar, tap, weightField } from './helpers'
import { MOTION_ON } from './storage'

// The narrowest phones in use: many Android phones are 306–360 px wide (an iPhone is 375 px and up). On each
// screen, on both training sides, at 306 and 320 px (and 390, a common iPhone), the page must not scroll sideways and nothing may reach past
// the screen or out of its own box.

/** The rows that scroll sideways on purpose: one line of chips. Everything else that scrolls, scrolls only up and down. */
const SIDEWAYS_ROWS = '.lib-chips'

/**
 * Things wider than the phone:
 * - the page, or any scroll container (the screen, a sheet) that scrolls sideways, other than a chip row;
 * - any element of the screen, the tab bar, the snackbar or an open sheet reaching past either edge, other than
 *   inside a chip row;
 * - text that does not fit its own box.
 * A scroll container's overflow-x computes to auto whenever its overflow-y is auto, so being one says nothing about
 * scrolling sideways: its scrollWidth does.
 */
const overflowing = (page: Page, sidewaysRows = SIDEWAYS_ROWS) =>
  page.evaluate((rows) => {
    const found: string[] = []
    const name = (el: Element) => `${el.tagName.toLowerCase()}.${el.className}`
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    for (const el of document.querySelectorAll<HTMLElement>('.shell-main, .shell-main *, .gr-tabbar *, .gr-snackbar, .gr-snackbar *, .gr-dialog, .gr-dialog *')) {
      if (el.closest('.sr-only')) continue
      const style = getComputedStyle(el)
      const scroller = ['auto', 'scroll'].includes(style.overflowX)
      if (scroller && !el.matches(rows) && el.scrollWidth > el.clientWidth + 1) found.push(`scrolls sideways ${name(el)}: ${el.scrollWidth} > ${el.clientWidth}`)
      if (el.parentElement?.closest(rows)) continue
      const box = el.getBoundingClientRect()
      if (box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5)) found.push(`outside ${name(el)}`)
      // Text that does not fit its own box. An open menu inside a row is drawn over the screen, not in the row's text.
      if (
        el.matches('li, p, h1, h2, h3, a, label, .gr-btn-text, .gr-chip-face, .gr-segment-face, .gr-snackbar-message, .lib-row-name, .lib-row-muscles') &&
        !scroller &&
        !el.querySelector('.gr-menu-list') &&
        el.scrollWidth > el.clientWidth + 1
      )
        found.push(`clipped ${name(el)}: ${el.textContent}`)
    }
    return found
  }, sidewaysRows)

/** Puts the app on the bodybuilding side, straight in IndexedDB, and reloads. */
async function bodybuilding(page: Page) {
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    await new Promise<void>((done, fail) => {
      const tx = db.transaction('settings', 'readwrite')
      tx.objectStore('settings').put({ key: 'trainingMode', value: 'BODYBUILDING' })
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    db.close()
  })
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
  await expect(tabBar(page)).toBeVisible()
}

for (const width of [306, 320, 390])
for (const side of ['powerlifting', 'bodybuilding'] as const) {
  test.describe(`a ${width} px phone, ${side} side`, () => {
    test.use({ viewport: { width, height: 568 } })
    test.beforeEach(async ({ page }) => {
      await openOnWednesday(page)
      if (side === 'bodybuilding') await bodybuilding(page)
    })

    test('Today with a session open fits', async ({ page }) => {
      await tap(page, 'Start session')
      // An exercise on it, so its card is measured too.
      await tap(page, 'Add exercise')
      const picker = page.getByRole('dialog', { name: /^(Add to |Add exercise)/ })
      await picker.getByRole('textbox', { name: 'Search exercises' }).fill('squat')
      await picker.getByRole('listitem', { name: 'Squat', exact: true }).getByRole('button').first().click()
      await expect(page.getByRole('heading', { name: 'Squat' })).toBeVisible()
      // The exercise card with its three steppers, the widest thing on Today.
      await expect(weightField(page)).toBeVisible()
      expect(await overflowing(page)).toEqual([])
    })

    test('the exercise picker from Plan fits', async ({ page }) => {
      await page.goto('/#/plan')
      await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
      const picker = page.getByRole('dialog', { name: /^(Add to |Add exercise)/ })
      await expect(picker).toBeVisible()
      // Every row drawn before the dialog is measured.
      const guides = picker.getByRole('button', { name: /^Form guide for / })
      await expect.poll(() => guides.count()).toBeGreaterThan(50)
      expect(await overflowing(page)).toEqual([])
    })

    test('Settings fits', async ({ page }) => {
      await page.goto('/#/settings')
      await expect(page.getByRole('heading', { level: 1, name: 'Settings' })).toBeVisible()
      await expect(page.getByRole('radiogroup', { name: 'Rest alert' })).toBeVisible()
      expect(await overflowing(page)).toEqual([])
    })

    test('Progress fits', async ({ page }) => {
      await page.goto('/#/progress')
      await expect(page.getByRole('heading', { level: 1, name: 'Progress' })).toBeVisible()
      expect(await overflowing(page)).toEqual([])
    })

    test('Coach fits', async ({ page }) => {
      await page.goto('/#/coach')
      await expect(page.getByRole('heading', { level: 1, name: 'Coach' })).toBeVisible()
      expect(await overflowing(page)).toEqual([])
    })
  })
}

// Stage 3, with motion on (every check above runs still): logging a set and moving a plan row slide into place, and
// nothing reaches past the screen while they move or once they have landed. At 306 and 320 px, and at 130% zoom on a
// 375 px phone (laid out 1.3 times narrower and shorter).
const sliding = () => document.getAnimations().some((a) => (a.effect as KeyframeEffect | null)?.pseudoElement?.startsWith('::view-transition') === true)

for (const [label, viewport] of [
  ['a 306 px phone', { width: 306, height: 568 }],
  ['a 320 px phone', { width: 320, height: 568 }],
  ['130% zoom', { width: Math.floor(375 / 1.3), height: Math.floor(812 / 1.3) }],
] as const) {
  test.describe(`${label}, with motion on`, () => {
    test.use({ viewport, storageState: MOTION_ON })
    test.beforeEach(async ({ page }) => {
      await page.emulateMedia({ reducedMotion: 'no-preference' })
      await openOnWednesday(page)
      expect(await page.evaluate(() => document.documentElement.getAttribute('data-motion'))).toBeNull()
    })

    test('a logged set slides in and nothing overflows', async ({ page }) => {
      await tap(page, 'Start session')
      await tap(page, 'Add exercise')
      const picker = page.getByRole('dialog', { name: /^(Add to |Add exercise)/ })
      await picker.getByRole('textbox', { name: 'Search exercises' }).fill('squat')
      await picker.getByRole('listitem', { name: 'Squat', exact: true }).getByRole('button').first().click()
      await expect(page.getByRole('heading', { name: 'Squat' })).toBeVisible()
      await weightField(page).fill('100')
      await weightField(page).blur()
      await page.getByRole('button', { name: 'Log set 1' }).click()
      await page.waitForFunction(sliding)
      expect(await overflowing(page)).toEqual([])
      await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
      await page.waitForFunction(() => !document.getAnimations().some((a) => (a.effect as KeyframeEffect | null)?.pseudoElement?.startsWith('::view-transition')))
      expect(await overflowing(page)).toEqual([])
    })

    test('a plan row moves and nothing overflows', async ({ page }) => {
      await page.goto('/#/plan')
      const monday = page.getByRole('region', { name: 'Monday' })
      for (const name of ['Squat', 'Bench']) {
        await monday.getByRole('button', { name: 'Add' }).click()
        const picker = page.getByRole('dialog', { name: /^Add to / })
        await picker.getByRole('textbox', { name: 'Search exercises' }).fill(name)
        await picker.getByRole('listitem', { name, exact: true }).getByRole('button').first().click()
        // The sets and reps form the pick opens: its Add stores the item.
        const form = page.getByRole('dialog').filter({ has: page.getByRole('group', { name: 'Sets' }) })
        await expect(form).toBeVisible()
        await pastTheTapWindow(page)
        await form.getByRole('button', { name: 'Add', exact: true }).click()
        await expect(picker).toHaveCount(0)
      }
      await expect(monday.getByRole('listitem')).toHaveCount(2)
      await page.waitForFunction(() => !document.getAnimations().some((a) => (a.effect as KeyframeEffect | null)?.pseudoElement?.startsWith('::view-transition')))
      expect(await overflowing(page)).toEqual([])
      await monday.getByRole('button', { name: 'Move down' }).first().click()
      await page.waitForFunction(sliding)
      expect(await overflowing(page)).toEqual([])
      await expect(monday.getByRole('listitem').first()).toHaveAttribute('aria-label', /^Bench/)
      await page.waitForFunction(() => !document.getAnimations().some((a) => (a.effect as KeyframeEffect | null)?.pseudoElement?.startsWith('::view-transition')))
      expect(await overflowing(page)).toEqual([])
    })
  })
}

// 1.12.3: Settings' rest alerts on a phone that can vibrate (an Android browser), where "Sound + vibrate" is offered.
// Each segment is as wide as its own label (as on Android); the segments still fill the row edge to edge and stay at
// least 44 px tall, and every label keeps its text at 15 px, whole (no clipping, no ellipsis). At 306, 320, 360 and 390 px
// each label is on one line. At 130% zoom on a 320 px phone (laid out 1.3 times narrower and shorter) there is not
// room for that, so a label may wrap, but only at a space ("Sound +" / "vibrate"), never inside a word.
for (const [label, viewport, oneLine] of [
  ['a 306 px phone', { width: 306, height: 640 }, true],
  ['a 320 px phone', { width: 320, height: 640 }, true],
  ['a 360 px phone', { width: 360, height: 640 }, true],
  ['a 390 px phone', { width: 390, height: 640 }, true],
  ['a 320 px phone at 130% zoom', { width: Math.floor(320 / 1.3), height: Math.floor(640 / 1.3) }, false],
] as const) {
  test.describe(`${label} that can vibrate`, () => {
    test.use({ viewport })
    test('restAlert_eachLabelWhole', async ({ page }) => {
      await page.addInitScript(() => Object.defineProperty(Navigator.prototype, 'vibrate', { configurable: true, value: () => true }))
      await openOnWednesday(page)
      await page.goto('/#/settings')
      const alerts = page.getByRole('radiogroup', { name: 'Rest alert' })
      // Something to measure: Sound + vibrate is drawn.
      await expect(alerts.getByText('Sound + vibrate', { exact: true })).toBeVisible()
      const row = await alerts.evaluate((group) => {
        const style = getComputedStyle(group)
        const inner = group.getBoundingClientRect()
        const segments = [...group.querySelectorAll('.gr-segment')].map((s) => s.getBoundingClientRect())
        return {
          left: inner.left + parseFloat(style.paddingLeft),
          right: inner.right - parseFloat(style.paddingRight),
          first: segments[0]!.left,
          last: segments[segments.length - 1]!.right,
        }
      })
      expect(row.first, `the row starts at its edge at ${label}`).toBeCloseTo(row.left, 0)
      expect(row.last, `the row reaches its edge at ${label}`).toBeCloseTo(row.right, 0)
      const faces = await alerts.locator('.gr-segment-face').evaluateAll((all) =>
        all.map((face) => {
          const lineCount = (range: Range) => new Set([...range.getClientRects()].filter((r) => r.width > 0).map((r) => Math.round(r.top))).size
          const whole = document.createRange()
          whole.selectNodeContents(face)
          // Each word on its own: one that spans two lines was broken inside it.
          const node = face.firstChild as Text
          const brokenWords = [...node.data.matchAll(/\S+/g)]
            .filter((m) => {
              const word = document.createRange()
              word.setStart(node, m.index)
              word.setEnd(node, m.index + m[0].length)
              return lineCount(word) > 1
            })
            .map((m) => m[0])
          const text = whole.getBoundingClientRect()
          const box = face.getBoundingClientRect()
          return {
            text: face.textContent,
            lines: lineCount(whole),
            brokenWords,
            px: parseFloat(getComputedStyle(face).fontSize),
            scrollWidth: face.scrollWidth,
            clientWidth: face.clientWidth,
            inside: text.left >= box.left - 0.5 && text.right <= box.right + 0.5,
            ellipsis: getComputedStyle(face).textOverflow === 'ellipsis',
            height: face.closest('.gr-segment')!.getBoundingClientRect().height,
          }
        }),
      )
      expect(faces.map((f) => f.text)).toEqual(['Sound', 'Sound + vibrate', 'Silent', 'Off'])
      for (const face of faces) {
        if (oneLine) expect(face.lines, `${face.text} on one line at ${label}`).toBe(1)
        expect(face.brokenWords, `${face.text} breaks only at a space at ${label}`).toEqual([])
        expect(face.scrollWidth, `${face.text} not clipped at ${label}`).toBeLessThanOrEqual(face.clientWidth)
        expect(face.inside, `${face.text} inside its segment at ${label}`).toBe(true)
        expect(face.ellipsis, `${face.text} has no ellipsis at ${label}`).toBe(false)
        expect(face.px, `${face.text} at ${label}`).toBe(15)
        expect(face.height, `${face.text}'s tap target at ${label}`).toBeGreaterThanOrEqual(44)
      }
      // At 130% zoom on a 320 px phone other parts of the screen (the day chips, the tab bar) are not yet held to
      // this check, so there it looks at the rest alerts only.
      const found = await overflowing(page)
      expect(oneLine ? found : found.filter((f) => /gr-segment/.test(f))).toEqual([])
    })
  })
}

// 1.17 (plate-count plan, Review Focus 5): a plate-counted machine on a 320 px phone at 200% text, laid out in half its
// width and height (as guide.spec and pictures.spec do). The chip "plate 12½ × 8–12 @ 8", the PLATE stepper showing 12.5
// and the plate sheet's three choices with their hints are each inside the screen; the page does not scroll sideways;
// Done is reached and closes the sheet.
test.describe('plates at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  test('plates at 320 px and 200 % text', async ({ page }) => {
    await openOnWednesday(page)
    // Lat pulldown on half steps, planned on Wednesdays on the bodybuilding side, with last week at plate 12½ and today's
    // session open: the coach's chip and the stepper both read 12½.
    await page.evaluate(async () => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'planItems', 'settings', 'sessions', 'sets'], 'readwrite')
      const settled = new Promise<void>((done, fail) => {
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      const add = (store: string, row: unknown) =>
        new Promise<number>((done) => {
          const request = tx.objectStore(store).add(row)
          request.onsuccess = () => done(request.result as number)
        })
      const pulldown = await add('exercises', { name: 'Lat pulldown', category: 'ACCESSORY', parentLift: null, muscles: ['LATS', 'BICEPS'], isBuiltIn: true, archived: false, plateCount: 'HALF' })
      await add('planItems', { weekday: 3, exerciseId: pulldown, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' })
      tx.objectStore('settings').put({ key: 'trainingMode', value: 'BODYBUILDING' })
      const lastWeek = await add('sessions', { date: '2026-11-04', bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' })
      for (const reps of [12, 11, 10]) await add('sets', { sessionId: lastWeek, exerciseId: pulldown, weightKg: 12.5, reps, rpe: 8, failed: false, missPoint: null, isTopSet: false, unit: 'PLATES' })
      await add('sessions', { date: '2026-11-11', bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' })
      await settled
      db.close()
    })
    await page.reload()
    await expect(tabBar(page)).toBeVisible()

    const inside = async (what: string, locator: Locator) => {
      await locator.scrollIntoViewIfNeeded()
      const box = (await locator.boundingBox())!
      expect(box, what).not.toBeNull()
      expect(box.x, `${what} starts inside the screen`).toBeGreaterThanOrEqual(-0.5)
      expect(box.x + box.width, `${what} ends inside the screen`).toBeLessThanOrEqual(160.5)
      const cut = await locator.evaluate((el) => el.scrollWidth > el.clientWidth + 1)
      expect(cut, `${what} is not cut`).toBe(false)
    }
    const field = page.getByRole('textbox', { name: 'Weight, plates' })
    await expect(field).toHaveValue('12.5')
    await inside('the chip', page.getByText('plate 12½ × 8–12 @ 8', { exact: true }))
    // At this size the chip puts Apply and Dismiss under its text: both stay inside the chip and the screen.
    const chip = page.locator('.gr-suggestion')
    for (const name of ['Apply', 'Dismiss']) {
      const button = chip.getByRole('button', { name })
      await inside(name, button)
      const [b, c] = [(await button.boundingBox())!, (await chip.boundingBox())!]
      expect(b.x >= c.x - 0.5 && b.x + b.width <= c.x + c.width + 0.5, `${name} is inside the chip`).toBe(true)
    }
    await inside('the PLATE stepper', page.locator('.today-stepper-weight'))
    await inside('Increase Plate', page.getByRole('button', { name: 'Increase Plate' }))
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)

    await page.getByRole('button', { name: 'Options for Lat pulldown' }).click()
    await page.getByRole('menuitem', { name: 'Count plates on this machine' }).click()
    const sheet = page.getByRole('dialog', { name: 'Count plates on Lat pulldown' })
    await expect(sheet).toBeVisible()
    for (const [value, text] of [['OFF', 'In kg'], ['WHOLE', 'Whole plates'], ['HALF', 'Half steps too']] as const) {
      await inside(`the choice ${text}`, sheet.getByTestId(`plate-option-${value}`))
      await expect(sheet.getByTestId(`plate-option-${value}`)).toContainText(text)
    }
    await inside('the hint 1, 2, 3 …', sheet.getByText('1, 2, 3 …', { exact: true }))
    await inside('the hint 1, 1½, 2 …', sheet.getByText('1, 1½, 2 …', { exact: true }))
    // The sheet's own parts only: the rest of Today at this size was not held to this check before plates.
    expect((await overflowing(page)).filter((f) => /gr-dialog|today-plate|today-skip/.test(f))).toEqual([])
    // Done is reached by scrolling the sheet, and closes it.
    const done = sheet.getByRole('button', { name: 'Done' })
    await done.scrollIntoViewIfNeeded()
    await expect(done).toBeInViewport()
    await done.click()
    await expect(sheet).toBeHidden()
  })
})

// 1.17.1: Today itself on a 320 px phone at 200 % text (laid out at 160 × 284), in kg and in plates on both sides, in
// every state of the day: nothing planned; not started (with the install, backup and start cards); started with the
// coach's chip; after Apply; after a set is logged with the rest bar up; after Finish. Checked on the whole page with
// `overflowing`, and with `alsoBroken` for what it does not see.

/**
 * What `overflowing` does not see, on the screen, the rest bar and the tab bar:
 * - anything of the rest bar reaching past the screen, or its text not fitting its box;
 * - a button, link, field or chip overlapping another one, or overlapping text, or text overlapping text, inside the
 *   screen (more than 1 px each way; one inside the other does not count; a stepper's caption action, laid over its figure by design, is left out);
 * - a button, link, field or chip reaching past the card (or the rest bar) it sits in;
 * - text squeezed to nothing (an element holding text that is drawn 0 px wide or high), or spilling out of its own box;
 * - a word broken across lines. The rule: in any text, a run of four or more characters with no space, hyphen or dash
 *   in it must sit on one line. Text broken letter by letter breaks every such word; text that wraps at its spaces
 *   breaks none. Only a word wider than the whole of its card (or list row, or screen) may break.
 */
const alsoBroken = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    const name = (el: Element) => `${el.tagName.toLowerCase()}.${el.getAttribute('class') ?? ''}`
    const shown = (el: Element) => {
      if (el.closest('.sr-only, .gr-tab-label')) return false
      const style = getComputedStyle(el)
      const box = el.getBoundingClientRect()
      return style.visibility !== 'hidden' && style.opacity !== '0' && box.width > 0 && box.height > 0
    }
    for (const el of document.querySelectorAll<HTMLElement>('.shell-rest, .shell-rest *')) {
      if (!shown(el)) continue
      const box = el.getBoundingClientRect()
      if (box.right > window.innerWidth + 0.5 || box.left < -0.5) found.push(`outside ${name(el)}`)
      if (el.matches('p, span, .gr-btn-text') && el.children.length === 0 && el.scrollWidth > el.clientWidth + 1) found.push(`clipped ${name(el)}: ${el.textContent}`)
    }
    const everything = [...document.querySelectorAll<HTMLElement>('.shell-main *, .shell-rest *, .gr-tabbar *')]
    for (const el of everything) {
      if (el.closest('.sr-only, .gr-tab-label') || getComputedStyle(el).display === 'none') continue
      const holdsText = [...el.childNodes].some((n) => n.nodeType === Node.TEXT_NODE && n.textContent!.trim() !== '')
      const box = el.getBoundingClientRect()
      if (holdsText && (box.width < 1 || box.height < 1)) found.push(`squeezed ${name(el)}: ${el.textContent}`)
    }
    const all = everything.filter(shown)
    // A stepper's caption action (the RPE info button) is laid over the caption's line and reaches over the figure by
    // design, at every width (its 48 px target): it is not held to the overlap rule.
    const controls = all.filter((el) => el.matches('button, a, input, label.gr-chip, [role="menuitem"]'))
    // Settings' RPE info button beside "Default RPE" is the same: a 48 px target centred on its icon's 24 px.
    const designedOver = (a: Element, b: Element) => a.closest('.gr-stepper-action, .lib-rpe-inline') != null || b.closest('.gr-stepper-action, .lib-rpe-inline') != null
    const texts = all.filter((el) => [...el.childNodes].some((n) => n.nodeType === Node.TEXT_NODE && n.textContent!.trim() !== ''))
    // Text spilling out of its own box (a pill squeezed into a narrow grid column draws past it), any element but inline ones.
    for (const el of texts) {
      const style = getComputedStyle(el)
      if (style.display === 'inline' || ['auto', 'scroll'].includes(style.overflowX)) continue
      if (el.scrollWidth > el.clientWidth + 1) found.push(`spills ${name(el)}: ${el.textContent}`)
    }
    // Within one region only: the screen scrolls under the rest bar and the tab bar, and an open menu and a message
    // are drawn over the screen (each held to the rule within itself).
    const region = (el: Element) => el.closest('.gr-menu-list, .gr-snackbar-host, .shell-main, .shell-rest, .gr-tabbar')
    // Text over text too (a pill over a set's numbers), each pair once.
    for (const [i, a] of texts.entries()) {
      const ra = a.getBoundingClientRect()
      for (const b of texts.slice(i + 1)) {
        if (a.contains(b) || b.contains(a) || region(a) !== region(b) || designedOver(a, b)) continue
        const rb = b.getBoundingClientRect()
        const x = Math.min(ra.right, rb.right) - Math.max(ra.left, rb.left)
        const y = Math.min(ra.bottom, rb.bottom) - Math.max(ra.top, rb.top)
        if (x > 1 && y > 1) found.push(`overlap ${name(a)} "${(a.textContent ?? '').trim().slice(0, 20)}" × ${name(b)} "${(b.textContent ?? '').trim().slice(0, 20)}"`)
      }
    }
    for (const a of controls) {
      const ra = a.getBoundingClientRect()
      const box = a.parentElement?.closest('.gr-card, .gr-rest')?.getBoundingClientRect()
      if (box && (ra.left < box.left - 0.5 || ra.right > box.right + 0.5)) found.push(`outside its card ${name(a)} "${(a.textContent ?? '').trim().slice(0, 20)}"`)
      for (const b of [...controls, ...texts]) {
        if (a === b || a.contains(b) || b.contains(a) || region(a) !== region(b) || designedOver(a, b)) continue
        const rb = b.getBoundingClientRect()
        const x = Math.min(ra.right, rb.right) - Math.max(ra.left, rb.left)
        const y = Math.min(ra.bottom, rb.bottom) - Math.max(ra.top, rb.top)
        if (x > 1 && y > 1) found.push(`overlap ${name(a)} "${(a.textContent ?? '').trim().slice(0, 20)}" × ${name(b)} "${(b.textContent ?? '').trim().slice(0, 20)}"`)
      }
    }
    // A word wider than the whole of its card (or list row, or the screen) cannot be kept on one line: it may break.
    const room = (el: Element) => {
      const box = el.closest('.gr-card, .lib-row, .shell-main, .shell-rest') as HTMLElement | null
      if (!box) return Infinity
      const style = getComputedStyle(box)
      return box.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight)
    }
    for (const el of texts) {
      for (const node of el.childNodes) {
        if (node.nodeType !== Node.TEXT_NODE) continue
        const text = node.textContent!
        for (const m of text.matchAll(/[^\s\-‐–—]{4,}/g)) {
          const range = document.createRange()
          range.setStart(node, m.index)
          range.setEnd(node, m.index + m[0].length)
          const rects = [...range.getClientRects()].filter((r) => r.width > 0)
          const lines = new Set(rects.map((r) => Math.round(r.top)))
          const width = rects.reduce((sum, r) => sum + r.width, 0)
          if (lines.size > 1 && width <= room(el)) found.push(`broken word ${name(el)}: ${m[0]}`)
        }
      }
    }
    return [...new Set(found)]
  })

/** Everything wrong at this size: `overflowing` and `alsoBroken`. */
const wrong = async (page: Page) => [...(await overflowing(page)), ...(await alsoBroken(page))]

/** The day's plan on `side`: Bench (kg) or Lat pulldown on half steps (plates), planned on Wednesdays, with four weeks of history. */
async function seedToday(page: Page, side: 'POWERLIFTING' | 'BODYBUILDING', plates: boolean) {
  await page.evaluate(
    async ({ side, plates }) => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'planItems', 'settings', 'sessions', 'sets'], 'readwrite')
      const settled = new Promise<void>((done, fail) => {
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      const add = (store: string, row: unknown) =>
        new Promise<number>((done) => {
          const request = tx.objectStore(store).add(row)
          request.onsuccess = () => done(request.result as number)
        })
      const all = await new Promise<{ id: number; name: string }[]>((done) => {
        const request = tx.objectStore('exercises').getAll()
        request.onsuccess = () => done(request.result)
      })
      const id = plates
        ? await add('exercises', { name: 'Lat pulldown', category: 'ACCESSORY', parentLift: null, muscles: ['LATS', 'BICEPS'], isBuiltIn: true, archived: false, plateCount: 'HALF' })
        : all.find((e) => e.name === 'Bench')!.id
      const bodybuilding = side === 'BODYBUILDING'
      await add('planItems', { weekday: 3, exerciseId: id, sets: 3, reps: 8, ...(bodybuilding ? { repsHigh: 12 } : {}), targetRpe: 8, order: 0, mode: side })
      tx.objectStore('settings').put({ key: 'trainingMode', value: side })
      for (const date of ['2026-10-14', '2026-10-21', '2026-10-28', '2026-11-04']) {
        const day = await add('sessions', { date, bodyweightKg: null, readiness: null, mode: side })
        for (const reps of bodybuilding ? [12, 11, 10] : [5, 5, 5])
          await add('sets', { sessionId: day, exerciseId: id, weightKg: plates ? 12.5 : 112.5, reps, rpe: 8, failed: false, missPoint: null, isTopSet: false, ...(plates ? { unit: 'PLATES' } : {}) })
      }
      await settled
      db.close()
    },
    { side, plates },
  )
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

test.describe('Today at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
    test(`nothing planned, ${side.toLowerCase()} side, fits`, async ({ page }) => {
      await openOnWednesday(page)
      if (side === 'BODYBUILDING') await bodybuilding(page)
      await expect(page.getByText('Nothing planned', { exact: true })).toBeVisible()
      expect(await wrong(page)).toEqual([])
    })

    for (const load of ['kg', 'plates'] as const) {
      test(`Today in ${load}, ${side.toLowerCase()} side, fits all day`, async ({ page }) => {
        await openOnWednesday(page)
        await seedToday(page, side, load === 'plates')
        // Not started: the install card, the backup card and the start card.
        await expect(page.getByRole('heading', { name: 'Install Grit' })).toBeVisible()
        await expect(page.getByRole('heading', { name: 'Back up your log' })).toBeVisible()
        await expect(page.getByRole('heading', { name: 'Ready to train?' })).toBeVisible()
        expect(await wrong(page), 'not started').toEqual([])
        // The readiness numbers keep their 48 px targets, so they take three rows: 2 + 2 + 1.
        const chips = await page.locator('.today-readiness-chips > .gr-chip').evaluateAll((all) => all.map((c) => c.getBoundingClientRect()).map((r) => ({ top: Math.round(r.top), width: r.width, height: r.height })))
        expect(new Set(chips.map((c) => c.top)).size, 'readiness rows').toBe(3)
        for (const c of chips) expect(Math.min(c.width, c.height), 'a readiness target').toBeGreaterThanOrEqual(48)

        await tap(page, 'Start session')
        await expect(page.locator('.gr-suggestion')).toBeVisible()
        expect(await wrong(page), 'with the chip').toEqual([])
        // The header reads as before: the title, then the side's label, then Finish (in that order in the page).
        const order = await page.locator('.gr-screen-header').evaluate((header) =>
          [...header.querySelectorAll('h1, .gr-mode-label, button')].map((el) => (el.matches('h1') ? 'title' : el.matches('button') ? el.textContent : 'side')),
        )
        expect(order).toEqual(side === 'BODYBUILDING' ? ['side', 'title', 'Finish'] : ['title', 'Finish'])

        await page.locator('.gr-suggestion').getByRole('button', { name: 'Apply' }).click()
        await expect(page.locator('.gr-suggestion')).toHaveCount(0)
        expect(await wrong(page), 'applied').toEqual([])

        await tap(page, 'Log set 1')
        await expect(page.getByRole('timer')).toBeVisible()
        await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
        expect(await wrong(page), 'a set logged, the rest bar up').toEqual([])

        // A missed set, then (where the row asks) where it was missed.
        await tap(page, 'Missed')
        await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
        expect(await wrong(page), 'a missed set').toEqual([])
        // The powerlifting side asks where it stalled; the bodybuilding side never does.
        const missRow = page.getByTestId('miss-row-2')
        await expect(missRow).toHaveCount(side === 'POWERLIFTING' ? 1 : 0)
        if (side === 'POWERLIFTING') {
          await missRow.locator('label.gr-segment').first().click()
          expect(await wrong(page), 'a missed set with its miss point').toEqual([])
        }

        await tap(page, 'Finish')
        await expect(page.getByRole('heading', { name: 'Session saved' })).toBeVisible()
        expect(await wrong(page), 'finished').toEqual([])
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
      })
    }
  }

  for (const [load, side] of [
    ['kg', 'POWERLIFTING'],
    ['plates', 'POWERLIFTING'],
    ['plates', 'BODYBUILDING'],
  ] as const) {
    test(`every kind of logged set fits, ${load}, ${side.toLowerCase()} side`, async ({ page }) => {
      await openOnWednesday(page)
      await seedToday(page, side, load === 'plates')
      // Today's session with one set of each kind: a top set; no RPE; 0 (bodyweight) in kg or a half plate; missed with
      // and without a miss point; a grind (RPE 9.5, which asks where it slowed on the powerlifting side).
      await page.evaluate(
        async ({ plates, side }) => {
          const db = await new Promise<IDBDatabase>((done, fail) => {
            const request = indexedDB.open('grit')
            request.onsuccess = () => done(request.result)
            request.onerror = () => fail(request.error)
          })
          const tx = db.transaction(['exercises', 'sessions', 'sets'], 'readwrite')
          const settled = new Promise<void>((done, fail) => {
            tx.oncomplete = () => done()
            tx.onerror = () => fail(tx.error)
          })
          const all = await new Promise<{ id: number; name: string }[]>((done) => {
            const request = tx.objectStore('exercises').getAll()
            request.onsuccess = () => done(request.result)
          })
          const id = all.find((e) => e.name === (plates ? 'Lat pulldown' : 'Bench'))!.id
          const session = await new Promise<number>((done) => {
            const request = tx.objectStore('sessions').add({ date: '2026-11-11', bodyweightKg: null, readiness: null, mode: side })
            request.onsuccess = () => done(request.result as number)
          })
          const unit = plates ? { unit: 'PLATES' } : {}
          const rows = plates
            ? [[13, 8, 8, false, null], [12.5, 8, null, false, null], [7.5, 12, 8, false, null], [13.5, 5, 10, true, 'MID'], [13.5, 4, 10, true, null], [12, 8, 9.5, false, null]]
            : [[120, 5, 8, false, null], [110, 5, null, false, null], [0, 10, 8, false, null], [125, 3, 10, true, 'LOCKOUT'], [125, 2, 10, true, null], [115, 5, 9.5, false, null]]
          for (const [weightKg, reps, rpe, failed, missPoint] of rows)
            tx.objectStore('sets').add({ sessionId: session, exerciseId: id, weightKg, reps, rpe, failed, missPoint, isTopSet: false, ...unit })
          await settled
          db.close()
        },
        { plates: load === 'plates', side },
      )
      await page.reload()
      // Six sets of three planned: the card is done.
      await expect(page.locator('.gr-set')).toHaveCount(6)
      // The powerlifting side marks the top set and asks where a missed set or a grind stalled.
      await expect(page.getByText('Top set', { exact: true })).toHaveCount(side === 'POWERLIFTING' ? 1 : 0)
      await expect(page.locator('[data-testid^="miss-row-"]')).toHaveCount(side === 'POWERLIFTING' ? 3 : 0)
      await expect(page.getByText('Failed', { exact: true }).first()).toBeVisible()
      expect(await wrong(page)).toEqual([])
    })
  }

  // Each main lift's own miss points: "Out of the hole", "Off the chest", "Off the floor", "Off the shoulders" (the longest).
  test('every lift asks where it stalled, and its points fit', async ({ page }) => {
    await openOnWednesday(page)
    const lifts = ['Squat', 'Bench', 'Deadlift', 'Strict press']
    await page.evaluate(async (lifts) => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets'], 'readwrite')
      const settled = new Promise<void>((done, fail) => {
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      const all = await new Promise<{ id: number; name: string }[]>((done) => {
        const request = tx.objectStore('exercises').getAll()
        request.onsuccess = () => done(request.result)
      })
      const session = await new Promise<number>((done) => {
        const request = tx.objectStore('sessions').add({ date: '2026-11-11', bodyweightKg: null, readiness: null, mode: 'POWERLIFTING' })
        request.onsuccess = () => done(request.result as number)
      })
      for (const [order, name] of lifts.entries()) {
        // Strict press is not among the rows a new log starts with: it is added as the catalogue adds it.
        const id =
          all.find((e) => e.name === name)?.id ??
          (await new Promise<number>((done) => {
            const request = tx.objectStore('exercises').add({ name, category: 'STRICT_PRESS', parentLift: null, muscles: ['FRONT_DELTS', 'TRICEPS'], isBuiltIn: true, archived: false })
            request.onsuccess = () => done(request.result as number)
          }))
        tx.objectStore('planItems').add({ weekday: 3, exerciseId: id, sets: 3, reps: 5, targetRpe: 8, order, mode: 'POWERLIFTING' })
        tx.objectStore('sets').add({ sessionId: session, exerciseId: id, weightKg: 100, reps: 3, rpe: 10, failed: true, missPoint: 'FLOOR', isTopSet: false })
      }
      await settled
      db.close()
    }, lifts)
    await page.reload()
    for (const name of lifts) {
      // Open its card (the first is open already).
      const row = page.locator('.today-row', { hasText: name })
      if (await row.count()) await row.first().click()
      await expect(page.locator('.today-card-head').getByRole('heading', { name, exact: true })).toBeVisible()
      await expect(page.getByTestId('miss-row-1')).toBeVisible()
      expect(await wrong(page), name).toEqual([])
      // Each point at least 48 px tall.
      for (const h of await page.getByTestId('miss-row-1').locator('label.gr-segment').evaluateAll((all) => all.map((l) => l.getBoundingClientRect().height)))
        expect(h, `${name}: a point's height`).toBeGreaterThanOrEqual(48)
    }
  })

  test('the tab bar shows its icons only, and keeps its names, its current tab and its touch targets', async ({ page }) => {
    await openOnWednesday(page)
    const nav = tabBar(page)
    for (const name of ['Today', 'Coach', 'Progress', 'Exercises', 'Plan']) {
      const tab = nav.getByRole('link', { name, exact: true })
      await expect(tab).toBeVisible()
      const box = (await tab.boundingBox())!
      // As tall as at any width (the bar's 80 px); as wide as five tabs in 160 px allow, which is what they have today.
      expect(box.height, `${name}'s height`).toBeGreaterThanOrEqual(80)
      expect(box.width, `${name}'s width`).toBeGreaterThanOrEqual(31.5)
      // The label is not drawn: only the icon is seen. Every bit of the tab's text is inside 1 px or less.
      const drawn = await tab.evaluate((a) => {
        const walker = document.createTreeWalker(a, NodeFilter.SHOW_TEXT)
        let widest = 0
        for (let n = walker.nextNode(); n; n = walker.nextNode()) {
          if (n.textContent!.trim() === '') continue
          const range = document.createRange()
          range.selectNodeContents(n)
          const box = range.getBoundingClientRect()
          const clip = (n.parentElement as Element).getBoundingClientRect()
          widest = Math.max(widest, Math.min(box.width, clip.width))
        }
        return widest
      })
      expect(drawn, `${name}'s label is not drawn`).toBeLessThanOrEqual(1)
    }
    await expect(nav.getByRole('link', { name: 'Today', exact: true })).toHaveAttribute('aria-current', 'page')
    expect(await wrong(page)).toEqual([])
  })
})

// 1.17.2: the other tabs on a 320 px phone at 200 % text (laid out at 160 × 284): Coach, Progress, Exercises, Plan and
// Settings, on both sides, empty and full. Every state is checked on the whole page with `overflowing` and `alsoBroken`,
// with nothing left out. (The rest bar shows on Today only, so it is not over these tabs.)

/** A long name that wraps at its spaces, and a 42-letter one-word name that can only break inside itself. */
const LONG_NAME = 'Single-arm cable lateral raise'
const ONE_WORD = 'Pneumonoultramicroscopicsilicovolcanoconio'

type StorySet = [name: string, kg: number, reps: number, rpe: number | null, extra?: { failed?: boolean; top?: boolean; plates?: boolean }]
type Story = {
  side: 'POWERLIFTING' | 'BODYBUILDING'
  /** Exercises written first when missing: [name, category, muscles, own exercise?, plate count?]. Any other name is added as an accessory. */
  exercises?: [name: string, category: string, muscles: string[], own?: boolean, plateCount?: 'WHOLE' | 'HALF'][]
  settings?: Record<string, unknown>
  /** [weekday, name, sets, reps, rpe, repsHigh?], on the story's side. */
  plan?: [number, string, number, number, number, number?][]
  sessions?: { date: string; bodyweightKg?: number; mode?: 'POWERLIFTING' | 'BODYBUILDING'; sets: StorySet[] }[]
  baselines?: [name: string, e1rmKg: number][]
  /** Payload fields; `exercise` names the exercise whose id the payload takes, `ids` names set additions by their names. */
  advice?: { type: string; lift?: string | null; reason: string; payload: string; fields?: Record<string, unknown>; exercise?: string; mode?: 'POWERLIFTING' | 'BODYBUILDING'; createdAt?: string }[]
  checkIns?: { date: string; lagging: string[]; growingWell: string[] }[]
}

/** Writes a story straight into IndexedDB and reloads. */
async function seedStory(page: Page, story: Story) {
  await page.evaluate(async (story) => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets', 'baselines', 'advice', 'settings', 'checkIns'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const add = (store: string, row: unknown) =>
      new Promise<number>((done) => {
        const request = tx.objectStore(store).add(row)
        request.onsuccess = () => done(request.result as number)
      })
    const all = await new Promise<{ id: number; name: string }[]>((done) => {
      const request = tx.objectStore('exercises').getAll()
      request.onsuccess = () => done(request.result)
    })
    const ids = new Map(all.map((e) => [e.name, e.id]))
    for (const [name, category, muscles, own, plateCount] of story.exercises ?? [])
      if (!ids.has(name)) ids.set(name, await add('exercises', { name, category, parentLift: null, muscles, isBuiltIn: !own, archived: false, ...(plateCount ? { plateCount } : {}) }))
    const idOf = async (name: string) => {
      if (!ids.has(name)) ids.set(name, await add('exercises', { name, category: 'ACCESSORY', parentLift: null, muscles: [], isBuiltIn: true, archived: false }))
      return ids.get(name)!
    }
    tx.objectStore('settings').put({ key: 'trainingMode', value: story.side })
    for (const [key, value] of Object.entries(story.settings ?? {})) tx.objectStore('settings').put({ key, value })
    const order = new Map<number, number>()
    for (const [weekday, name, sets, reps, targetRpe, repsHigh] of story.plan ?? []) {
      const next = order.get(weekday) ?? 0
      order.set(weekday, next + 1)
      await add('planItems', { weekday, exerciseId: await idOf(name), sets, reps, targetRpe, order: next, mode: story.side, ...(repsHigh != null ? { repsHigh } : {}) })
    }
    for (const s of story.sessions ?? []) {
      const sessionId = await add('sessions', { date: s.date, bodyweightKg: s.bodyweightKg ?? null, readiness: null, mode: s.mode ?? story.side })
      for (const [name, weightKg, reps, rpe, extra = {}] of s.sets)
        await add('sets', { sessionId, exerciseId: await idOf(name), weightKg, reps, rpe, failed: extra.failed ?? false, missPoint: null, isTopSet: extra.top ?? false, ...(extra.plates ? { unit: 'PLATES' } : {}) })
    }
    for (const [name, e1rmKg] of story.baselines ?? []) tx.objectStore('baselines').put({ exerciseId: await idOf(name), e1rmKg, date: '2026-10-12' })
    for (const a of story.advice ?? []) {
      const fields: Record<string, unknown> = { ...(a.fields ?? {}) }
      if (a.exercise) fields.exerciseId = await idOf(a.exercise)
      if (Array.isArray(fields.addSets)) for (const s of fields.addSets as { name: string; exerciseId?: number }[]) s.exerciseId = await idOf(s.name)
      await add('advice', {
        type: a.type,
        lift: a.lift ?? null,
        createdAt: a.createdAt ?? '2026-11-11',
        reason: a.reason,
        severity: 2,
        status: 'OPEN',
        payloadJson: JSON.stringify({ type: `com.powertracker.domain.model.AdvicePayload.${a.payload}`, ...fields }),
        mode: a.mode ?? story.side,
      })
    }
    for (const c of story.checkIns ?? []) await add('checkIns', c)
    await settled
    db.close()
  }, story)
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

/** Every date `days` after `from`, as `YYYY-MM-DD`, up to the sample Wednesday. */
const dayAfter = (from: string, days: number) => new Date(Date.parse(`${from}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10)

/** Twelve weeks of squat, bench and deadlift (and strict press) with bodyweights, and a deadlift record from last year. */
function liftHistory(strictPress: boolean): NonNullable<Story['sessions']> {
  const sessions: NonNullable<Story['sessions']> = [{ date: '2025-10-12', bodyweightKg: 90.4, sets: [['Deadlift', 212.5, 10, 9.5, { top: true }]] }]
  for (let week = 0; week < 12; week++) {
    const monday = dayAfter('2026-08-24', week * 7)
    const squat = 142.5 + 2.5 * week
    sessions.push({ date: monday, bodyweightKg: 91.6 + week / 10, sets: [['Squat', squat, 3, 9, { top: true }], ['Squat', squat - 30, 5, 6], ['Pause squat', squat - 27.5, 3, 7.5]] })
    sessions.push({ date: dayAfter(monday, 1), sets: [['Bench', 107.5, 3, 9, { top: true }], ['Bench', 90, 5, 7]] })
    if (strictPress) sessions.push({ date: dayAfter(monday, 2), sets: [['Strict press', 62.5 + 2.5 * Math.floor(week / 2), 3, 8.5, { top: true }], ['Strict press', 52.5, 5, 7]] })
    sessions.push({ date: dayAfter(monday, 4), bodyweightKg: 91.9 + week / 10, sets: [['Deadlift', 185 + 2.5 * week, 3, 7.5, { top: true }], ['Deadlift', 160, 5, 6]] })
  }
  return sessions.filter((s) => s.date <= '2026-11-11')
}

const STRICT_PRESS: NonNullable<Story['exercises']>[number] = ['Strict press', 'STRICT_PRESS', ['Front delts', 'Side delts', 'Triceps']]
const OWN_EXERCISES: NonNullable<Story['exercises']> = [
  [LONG_NAME, 'ACCESSORY', ['Side delts'], true],
  [ONE_WORD, 'ACCESSORY', ['Pecs', 'Front delts', 'Triceps'], true],
]

/** The powerlifting side with everything: twelve weeks of the four lifts, a plan with long names, bests, and every card the coach shows there. */
const powerliftingFull = (): Story => ({
  side: 'POWERLIFTING',
  exercises: [STRICT_PRESS, ...OWN_EXERCISES],
  settings: { showStrictPress: true, powerliftingLeftOn: '2026-09-20' },
  plan: [
    [1, 'Squat', 5, 5, 8],
    [1, 'Bench', 4, 6, 7.5],
    [1, LONG_NAME, 3, 12, 8],
    [1, ONE_WORD, 3, 10, 8],
    [3, 'Deadlift', 3, 3, 8],
    [3, 'Close-grip bench', 3, 5, 8],
    [3, 'Strict press', 3, 5, 8.5],
    [5, 'Bench', 3, 5, 8],
    [5, 'Pause squat', 3, 4, 7.5],
  ],
  sessions: liftHistory(true),
  baselines: [['Squat', 190], ['Bench', 130], ['Deadlift', 250], ['Strict press', 80]],
  advice: [
    { type: 'DELOAD', reason: 'Signs of fatigue: RPE rising at the same load on squat and deadlift; 3 failed reps. Take a deload week: about 60% of the volume at about 90% of the load.', payload: 'Deload' },
    {
      type: 'STALL',
      lift: 'BENCH',
      reason: "Bench e1RM hasn't improved for 3 weeks. You've mostly missed at lockout — this works the triceps.",
      payload: 'Accessories',
      fields: { items: [{ exerciseName: 'Close-grip bench', sets: 3, repsLow: 3, repsHigh: 5 }, { exerciseName: LONG_NAME, sets: 3, repsLow: 12, repsHigh: 15 }, { exerciseName: ONE_WORD, sets: 3, repsLow: 8, repsHigh: 10 }] },
    },
    { type: 'WEAK_POINT', lift: 'SQUAT', reason: 'Your squat is low relative to your deadlift (76% vs a typical 80–90%). Prioritise squat.', payload: 'WeakPoint', fields: { lift: 'SQUAT', missPoint: null } },
    { type: 'WEAK_POINT', lift: 'STRICT_PRESS', reason: "You've mostly missed off the shoulders. Prioritise strict press.", payload: 'WeakPoint', fields: { lift: 'STRICT_PRESS', missPoint: 'FLOOR' } },
    { type: 'RE_ENTRY', reason: 'Two weeks since your last session. Ease back in: about 85% of your last top sets for 7 days.', payload: 'ReEntry', fields: { loadFactor: 0.85 } },
    { type: 'SKIP', reason: `${LONG_NAME} skipped for pain. Try something easier on it for now.`, payload: 'Skipped', exercise: LONG_NAME, fields: { skipId: 1, name: LONG_NAME, reason: 'PAIN', alternatives: [ONE_WORD, 'Cable lateral raise', 'Machine lateral raise'] } },
  ],
})

/** Five weeks of bodybuilding sessions from 7 October, a plate-counted pulldown at plate 12½ among them, and one from the end of August. */
function bodybuildingHistory(): NonNullable<Story['sessions']> {
  const sessions: NonNullable<Story['sessions']> = [{ date: '2026-08-31', sets: [['Incline DB press', 24, 10, 8]] }]
  for (let week = 0; week < 5; week++) {
    const monday = dayAfter('2026-10-05', week * 7)
    sessions.push({ date: monday, sets: [['Incline DB press', 26, 10 + (week % 2), 8], [LONG_NAME, 12.5, 12, 8], [ONE_WORD, 40, 10, 8]] })
    sessions.push({ date: dayAfter(monday, 2), sets: [['Lat pulldown', 12.5, 10, 8, { plates: true }], ['Cable crossover', 15, 12, 8]] })
  }
  return sessions
}

const BODYBUILDING_EXERCISES: NonNullable<Story['exercises']> = [...OWN_EXERCISES, ['Lat pulldown', 'ACCESSORY', ['Lats', 'Biceps'], false, 'HALF']]
const BODYBUILDING_PLAN: NonNullable<Story['plan']> = [
  [1, 'Incline DB press', 3, 8, 8, 12],
  [1, LONG_NAME, 3, 12, 8, 15],
  [1, ONE_WORD, 3, 8, 8, 12],
  [3, 'Lat pulldown', 3, 8, 8, 12],
  [3, 'Cable crossover', 3, 10, 8, 15],
]

/** The bodybuilding side with every card the coach shows there: welcome back, a check-in due, more work, a stuck exercise, a skip, a deload and easing back in. */
const bodybuildingCoach = (): Story => ({
  side: 'BODYBUILDING',
  exercises: BODYBUILDING_EXERCISES,
  settings: { bodybuildingTrainingDays: [1, 3, 5], bodybuildingLeftOn: '2026-09-20', powerliftingLeftOn: '2026-09-21' },
  plan: BODYBUILDING_PLAN,
  sessions: bodybuildingHistory(),
  advice: [
    {
      type: 'MORE_WORK',
      reason: `You marked side delts as lagging. One more set of ${LONG_NAME} on Monday.`,
      payload: 'MoreWork',
      fields: { group: 'SIDE_DELTS', addSets: [{ weekday: 'MONDAY', name: LONG_NAME, fromSets: 3, toSets: 4 }, { weekday: 'MONDAY', name: ONE_WORD, fromSets: 3, toSets: 4 }], newExercise: null, weeklyBefore: 6, weeklyAfter: 8 },
    },
    {
      type: 'MORE_WORK',
      reason: `You marked chest as lagging. Add ${ONE_WORD} on Wednesday.`,
      payload: 'MoreWork',
      fields: { group: 'CHEST', addSets: [], newExercise: { weekday: 'WEDNESDAY', name: ONE_WORD, sets: 3, repsLow: 8, repsHigh: 12, rpe: 8 }, weeklyBefore: 0, weeklyAfter: 3 },
    },
    {
      type: 'SWAP',
      reason: `${ONE_WORD} hasn't moved in 3 weeks.`,
      payload: 'Swap',
      exercise: ONE_WORD,
      createdAt: '2026-11-04',
      fields: { name: ONE_WORD, group: 'CHEST', since: '2026-10-14', alternatives: [LONG_NAME, 'Seated cable fly', 'Incline DB fly'] },
    },
    { type: 'SKIP', reason: `${LONG_NAME} skipped: busy. Here are two that need less waiting.`, payload: 'Skipped', exercise: LONG_NAME, fields: { skipId: 1, name: LONG_NAME, reason: 'BUSY', alternatives: [ONE_WORD, 'Cable lateral raise'] } },
    { type: 'DELOAD', reason: 'Signs of fatigue: low readiness. Take a deload week: about 60% of the volume at about 90% of the load.', payload: 'Deload' },
    { type: 'RE_ENTRY', reason: 'Two weeks since your last session. Ease back in for 7 days.', payload: 'ReEntry', fields: { loadFactor: 0.85 } },
  ],
})

/** The bodybuilding side's Progress with check-ins: the body parts with their ratings, and the rows (a plate row among them). */
const bodybuildingProgress = (): Story => ({
  side: 'BODYBUILDING',
  exercises: BODYBUILDING_EXERCISES,
  settings: { bodybuildingTrainingDays: [1, 3, 5] },
  plan: BODYBUILDING_PLAN,
  sessions: bodybuildingHistory(),
  checkIns: [
    { date: '2026-10-07', lagging: ['SIDE_DELTS', 'CHEST'], growingWell: [] },
    { date: '2026-11-04', lagging: ['SIDE_DELTS'], growingWell: ['CHEST', 'LATS'] },
  ],
})

/** Opens a hash and waits for its h1. */
async function openTab(page: Page, hash: string, title: string) {
  await page.goto(`/#/${hash}`)
  await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
  await page.evaluate(() => document.fonts.ready)
}

/**
 * Checks the whole page, softly, so one test reports every state that is wrong. The Exercises tab's rows are drawn
 * only once scrolled to (`content-visibility: auto`, at a guessed 65 px each): every row is drawn here as it is on
 * screen, so each is measured at its real size.
 */
async function fits(page: Page, state: string) {
  await page.evaluate(() => {
    if (document.getElementById('all-rows-drawn')) return
    const style = document.createElement('style')
    style.id = 'all-rows-drawn'
    style.textContent = '.lib-row { content-visibility: visible !important; }'
    document.head.append(style)
  })
  expect.soft(await wrong(page), state).toEqual([])
}

/** The open menu is wholly inside what the screen shows: below its top, above the tab bar, inside both edges. */
async function menuInRoom(page: Page, state: string) {
  const box = await page.evaluate(() => {
    const menu = document.querySelector('.gr-menu-list')!.getBoundingClientRect()
    const screen = document.querySelector('.shell-main')!.getBoundingClientRect()
    const tabs = document.querySelector('.gr-tabbar')!.getBoundingClientRect()
    return { menu: { top: menu.top, bottom: menu.bottom, left: menu.left, right: menu.right }, top: screen.top, bottom: Math.min(screen.bottom, tabs.top), width: window.innerWidth }
  })
  const { menu } = box
  expect.soft(menu.top >= box.top - 0.5 && menu.bottom <= box.bottom + 0.5 && menu.left >= -0.5 && menu.right <= box.width + 0.5, `${state}: the menu ${JSON.stringify(menu)} is inside the room ${JSON.stringify(box)}`).toBe(true)
}

test.describe('the other tabs at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
    const s = side.toLowerCase()
    test(`every tab with nothing in it, ${s} side, fits`, async ({ page }) => {
      await openOnWednesday(page)
      if (side === 'BODYBUILDING') await bodybuilding(page)
      await openTab(page, 'coach', 'Coach')
      await expect(page.getByText('No advice right now. Keep logging.')).toBeVisible()
      await fits(page, 'Coach, empty')
      await openTab(page, 'progress', 'Progress')
      await expect(page.locator('.gr-empty')).toBeVisible()
      await fits(page, 'Progress, empty')
      await openTab(page, 'exercises', 'Exercises')
      await expect(page.getByRole('listitem', { name: 'Squat', exact: true })).toBeVisible()
      await fits(page, 'Exercises')
      await openTab(page, 'plan', 'Plan')
      await expect(page.locator('.plan-empty')).toBeVisible()
      await fits(page, 'Plan, empty')
      await openTab(page, 'settings', 'Settings')
      await fits(page, 'Settings')
    })
  }

  test(`Coach with every card, powerlifting side, fits`, async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, powerliftingFull())
    await openTab(page, 'coach', 'Coach')
    await expect(page.getByRole('region', { name: 'Welcome back' })).toBeVisible()
    for (const title of ['Take a deload week', 'Bench has stalled', 'Squat is lagging your deadlift', 'Strict press is lagging', 'Welcome back', `${LONG_NAME} skipped`])
      await expect(page.getByRole('heading', { name: title, exact: true }).first()).toBeVisible()
    await fits(page, 'Coach, powerlifting, every card')
    // Its message after a deload is applied, over the screen.
    await page.getByRole('button', { name: 'Apply deload' }).click()
    await expect(page.getByText('Deload week on — targets lightened for 7 days')).toBeVisible()
    await fits(page, 'Coach, powerlifting, the deload applied, with its message')
  })

  test(`Coach with every card, bodybuilding side, fits`, async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, bodybuildingCoach())
    await openTab(page, 'coach', 'Coach')
    await expect(page.getByRole('region', { name: 'Welcome back' })).toBeVisible()
    await expect(page.getByRole('region', { name: 'Check-in' })).toBeVisible()
    for (const title of ['Side delts is lagging', 'Chest is lagging', `${ONE_WORD} hasn't moved in 3 weeks`, `${LONG_NAME} skipped`, 'Take a deload week', 'Welcome back'])
      await expect(page.getByRole('heading', { name: title, exact: true }).first()).toBeVisible()
    await fits(page, 'Coach, bodybuilding, every card')
  })

  test(`Progress with history, powerlifting side, strict press on, fits in every range`, async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, powerliftingFull())
    await openTab(page, 'progress', 'Progress')
    await expect(page.getByText('Weekly best e1RM')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Records' })).toBeVisible()
    await fits(page, 'Progress, 8 wk')
    for (const range of ['6 mo', '1 yr', 'All']) {
      // Checked as a tap on its label would: before 1.17.2 the label could be cut to nothing at this width.
      const radio = page.getByRole('radiogroup', { name: 'Chart range' }).getByRole('radio', { name: range })
      await radio.check({ force: true })
      await expect(radio).toBeChecked()
      await fits(page, `Progress, ${range}`)
    }
  })

  test(`Progress with history, bodybuilding side, fits`, async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, bodybuildingProgress())
    await openTab(page, 'progress', 'Progress')
    await expect(page.getByRole('region', { name: 'Body parts' })).toBeVisible()
    await expect(page.getByText('plate 12½ × 10').first()).toBeVisible()
    await fits(page, 'Progress, bodybuilding, with check-ins')
  })

  test(`Plan with long names, powerlifting side, fits, its menus too`, async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, powerliftingFull())
    await openTab(page, 'plan', 'Plan')
    await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem')).toHaveCount(4)
    await expect(page.getByText(/^Best e1RM/)).toHaveCount(4)
    await fits(page, 'Plan, powerlifting, with bests')
    // Every row's ⋮, from the top of the screen to the last row of Friday, then the Plan ⋮: each menu fits and is
    // wholly on screen.
    const more = page.getByRole('button', { name: 'More', exact: true })
    await expect(more).toHaveCount(9)
    for (let i = 0; i < 9; i++) {
      await more.nth(i).click()
      await expect(page.getByRole('menuitem', { name: 'Edit' })).toBeVisible()
      await fits(page, `Plan, row ${i + 1}'s menu open`)
      await menuInRoom(page, `Plan, row ${i + 1}'s menu`)
      await page.keyboard.press('Escape')
      await expect(page.getByRole('menuitem')).toHaveCount(0)
    }
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 0))
    await page.getByRole('button', { name: 'Plan options' }).click()
    await expect(page.getByRole('menuitem').first()).toBeVisible()
    await fits(page, 'Plan, its options menu open')
    await menuInRoom(page, 'Plan, its options menu')
  })

  test(`Plan with long names, bodybuilding side, fits`, async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, bodybuildingProgress())
    await openTab(page, 'plan', 'Plan')
    await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem')).toHaveCount(3)
    await fits(page, 'Plan, bodybuilding')
    const more = page.getByRole('button', { name: 'More', exact: true })
    await expect(more).toHaveCount(5)
    for (let i = 0; i < 5; i++) {
      await more.nth(i).click()
      await expect(page.getByRole('menuitem', { name: 'Delete' })).toBeVisible()
      await fits(page, `Plan, bodybuilding, row ${i + 1}'s menu open`)
      await menuInRoom(page, `Plan, bodybuilding, row ${i + 1}'s menu`)
      await page.keyboard.press('Escape')
      await expect(page.getByRole('menuitem')).toHaveCount(0)
    }
  })

  test(`Exercises with own exercises, every step, fits`, async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, { ...powerliftingFull(), advice: [] })
    await openTab(page, 'exercises', 'Exercises')
    await expect(page.getByRole('listitem', { name: LONG_NAME })).toBeVisible()
    await fits(page, 'Exercises, the list')
    const search = page.getByRole('textbox', { name: 'Search exercises' })
    await search.fill('raise')
    await expect(page.getByRole('listitem', { name: 'Cable lateral raise' }).first()).toBeVisible()
    await fits(page, 'Exercises, searched "raise"')
    await search.fill(ONE_WORD.slice(0, 8))
    await expect(page.getByRole('listitem', { name: ONE_WORD })).toBeVisible()
    await fits(page, 'Exercises, searched the one-word name')
    await search.fill('zzzz')
    await expect(page.locator('.gr-empty')).toBeVisible()
    await fits(page, 'Exercises, nothing matches')
    await search.fill('')
    // Every body area, then one piece of equipment.
    const areas = page.getByRole('group', { name: 'Body area' }).locator('label.gr-chip')
    await expect(areas).not.toHaveCount(0)
    for (let i = 0; i < (await areas.count()); i++) {
      await areas.nth(i).click()
      await fits(page, `Exercises, body area ${(await areas.nth(i).textContent())?.trim()}`)
    }
    await areas.first().click()
    const equipment = page.getByRole('group', { name: 'Equipment' }).locator('label.gr-chip')
    await equipment.nth(1).click()
    await expect(equipment.nth(1).getByRole('checkbox')).toBeChecked()
    await fits(page, `Exercises, equipment ${(await equipment.nth(1).textContent())?.trim()}`)
    await equipment.nth(1).click()
    // The whole tab scrolls at this width: to its end.
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, el.scrollHeight))
    await fits(page, 'Exercises, scrolled to the end')
  })

  // In forced colours (Windows high contrast) a card's border is drawn: the days card, which gives up its sides at this
  // width so three days fit, keeps an outline on all four sides, and each day stays 48 px.
  test('Settings in forced colours: the days card is outlined all round, its days still 48 px', async ({ page }) => {
    await page.emulateMedia({ forcedColors: 'active' })
    await openOnWednesday(page)
    await openTab(page, 'settings', 'Settings')
    const card = page.locator('.settings-days')
    const style = await card.evaluate((el) => {
      const s = getComputedStyle(el)
      return { outline: [s.outlineStyle, s.outlineWidth], top: s.borderTopWidth, bottom: s.borderBottomWidth, sides: [s.borderLeftWidth, s.borderRightWidth] }
    })
    // An outline is drawn on every side, inside the card; its top and bottom border stay.
    expect(style.outline).toEqual(['solid', '1px'])
    expect([style.top, style.bottom]).toEqual(['1px', '1px'])
    const days = await card.locator('label.gr-chip').evaluateAll((all) => all.map((c) => c.getBoundingClientRect()).map((r) => [r.width, r.height]))
    expect(days).toHaveLength(7)
    for (const [width, height] of days) {
      expect(width).toBeGreaterThanOrEqual(47.5)
      expect(height).toBeGreaterThanOrEqual(47.5)
    }
    await fits(page, 'Settings in forced colours')
  })

  // The whole tab scrolls at this width, so a row is near when it is near the screen: the rows far below it make no
  // picture (their tiles are hidden under 320 px, and a few hundred drawings would slow the tab down).
  test('Exercises draws the pictures of the rows near the screen only', async ({ page }) => {
    await openOnWednesday(page)
    await openTab(page, 'exercises', 'Exercises')
    await expect(page.getByRole('listitem', { name: 'Squat', exact: true })).toBeVisible()
    // The whole list is drawn (the first screenful comes first, the rest straight after).
    await expect.poll(() => page.locator('.lib-row').count()).toBeGreaterThan(200)
    // Each row is at least 68 px tall here: the screen and 600 px past it each way hold fewer than 20.
    await expect.poll(() => page.locator('.lib-thumb svg').count()).toBeGreaterThan(0)
    expect(await page.locator('.lib-thumb svg').count()).toBeLessThan(20)
    // Scrolled to the end, the last rows draw theirs.
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, el.scrollHeight))
    await expect.poll(() => page.locator('.lib-row').last().locator('.lib-thumb svg').count()).toBe(1)
  })

  for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
    test(`Exercises with two muscles chosen, ${side.toLowerCase()} side, fits`, async ({ page }) => {
      await openOnWednesday(page)
      if (side === 'BODYBUILDING') await bodybuilding(page)
      await openTab(page, 'exercises', 'Exercises')
      // The tab chooses muscles from its drop-down (it has no grid of muscles to browse).
      await page.locator('.lib-dropdown').click()
      const sheet = page.getByRole('dialog', { name: 'Muscles' })
      await sheet.getByRole('checkbox').nth(0).check()
      await sheet.getByRole('checkbox').nth(5).check()
      await sheet.getByRole('button', { name: 'Close' }).click()
      await expect(page.getByRole('group', { name: 'Chosen muscles' })).toBeVisible()
      await fits(page, 'Exercises, two muscles chosen')
      await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, el.scrollHeight))
      await fits(page, 'Exercises, two muscles chosen, at the end of the list')
    })

    test(`Settings, every row, ${side.toLowerCase()} side, on a phone that can vibrate, fits`, async ({ page }) => {
      await page.addInitScript(() => Object.defineProperty(Navigator.prototype, 'vibrate', { configurable: true, value: () => true }))
      await openOnWednesday(page)
      if (side === 'BODYBUILDING') await bodybuilding(page)
      await openTab(page, 'settings', 'Settings')
      await expect(page.getByRole('radiogroup', { name: 'Rest alert' }).getByText('Sound + vibrate', { exact: true })).toBeVisible()
      await fits(page, 'Settings')
      if (side === 'POWERLIFTING') {
        await page.getByText('Show it on Progress and add it to your total.', { exact: true }).click()
        await expect(page.getByRole('switch', { name: 'Strict press' })).toHaveAttribute('aria-checked', 'true')
        await page.getByRole('button', { name: 'Increase Default RPE' }).click()
      }
      await page.getByRole('radiogroup', { name: 'Rest alert' }).getByText('Sound + vibrate', { exact: true }).click()
      await fits(page, 'Settings, changed')
      // A backup out and back in: its messages, then the safety copy.
      const download = page.waitForEvent('download')
      await page.getByRole('button', { name: 'Export backup' }).click()
      const file = await download
      await expect(page.getByText('Backup saved.')).toBeVisible()
      await fits(page, 'Settings, backup saved')
      await page.getByLabel('Backup file').setInputFiles(await file.path())
      await page.getByRole('dialog').getByRole('button', { name: 'Replace' }).click()
      await expect(page.getByText('Backup restored.')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Restore safety copy' })).toBeVisible()
      await fits(page, 'Settings, restored, with the safety copy')
    })
  }

  // Every button, link, field, chip and segment on the five screens is at 160 px at least as large as at 390 px, up to
  // 48 px each way: a target that is 48 px on a phone at normal text stays 48 px here.
  for (const [side, story] of [
    ['powerlifting', powerliftingFull],
    ['bodybuilding', bodybuildingProgress],
  ] as const) {
    test(`every touch target on the five screens keeps its size, ${side} side`, async ({ page }) => {
      await page.addInitScript(() => Object.defineProperty(Navigator.prototype, 'vibrate', { configurable: true, value: () => true }))
      await openOnWednesday(page)
      await seedStory(page, story())
      const sizes = () =>
        page.evaluate(() => {
          const out: Record<string, { width: number; height: number }> = {}
          for (const el of document.querySelectorAll<HTMLElement>('.shell-main :is(button, a[href], input:not([type=radio], [type=checkbox]), label.gr-chip, label.gr-segment, label.settings-row)')) {
            if (!el.checkVisibility()) continue
            const name = (el.getAttribute('aria-label') ?? el.textContent ?? '').trim()
            let key = `${el.tagName.toLowerCase()} ${name}`
            for (let n = 2; out[key] != null; n++) key = `${el.tagName.toLowerCase()} ${name} #${n}`
            const { width, height } = el.getBoundingClientRect()
            out[key] = { width, height }
          }
          return out
        })
      for (const [hash, title] of [['coach', 'Coach'], ['progress', 'Progress'], ['exercises', 'Exercises'], ['plan', 'Plan'], ['settings', 'Settings']] as const) {
        await page.setViewportSize({ width: 390, height: 844 })
        await openTab(page, hash, title)
        await page.waitForTimeout(200)
        const wide = await sizes()
        await page.setViewportSize({ width: 160, height: 284 })
        await page.waitForTimeout(200)
        const narrow = await sizes()
        expect.soft(Object.keys(narrow).sort(), `${title}: the same targets`).toEqual(Object.keys(wide).sort())
        const smaller = Object.entries(narrow)
          .filter(([key, box]) => wide[key] != null && (box.width < Math.min(48, wide[key]!.width) - 0.5 || box.height < Math.min(48, wide[key]!.height) - 0.5))
          .map(([key, box]) => `${key}: ${Math.round(box.width)} × ${Math.round(box.height)}, at 390 px ${Math.round(wide[key]!.width)} × ${Math.round(wide[key]!.height)}`)
        expect.soft(smaller, `${title}: targets smaller at 160 px`).toEqual([])
      }
    })
  }
})
