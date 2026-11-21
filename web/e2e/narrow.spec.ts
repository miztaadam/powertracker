import { expect, test, type Locator, type Page } from '@playwright/test'
import { openOnWednesday, pastTheTapWindow, seedFlatBench, tabBar, tap, weightField } from './helpers'
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
      // Text that does not fit its own box.
      if (
        el.matches('li, p, h1, h2, h3, a, label, .gr-btn-text, .gr-chip-face, .gr-segment-face, .gr-snackbar-message, .lib-row-name, .lib-row-muscles') &&
        !scroller &&
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

// 1.17.1: Today itself on a 320 px phone at 200 % text (laid out at 160 × 284), in kg and in plates, with a session
// open, the coach's chip showing and after a set is logged with the rest bar up: the page does not scroll sideways and
// nothing reaches past the screen. The whole page is checked, with the parts still left to the owner named below.
/**
 * Not yet held to this check at this size, each a design choice handed back to the owner (1.17.1 report): the tab
 * bar's labels ("Progress", "Exercises" clipped), the screen header (the date eyebrow, the side's label and the title
 * "Today" squeezed beside Finish), the chip's "Coach suggests" eyebrow and the weight stepper's "Weight kg" caption.
 */
const handedBack = (found: string[]) =>
  found.filter((f) => !/^clipped (a\.gr-tab |p\.eyebrow: Wednesday 11 November|p\.gr-mode-label|h1\.gr-screen-title|p\.eyebrow: Coach suggests|label\.gr-stepper-figure: Weight)/.test(f))

/** Lat pulldown on half steps, planned on Wednesdays on the bodybuilding side, last week at plate 12½. */
async function seedPlates(page: Page) {
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
    await settled
    db.close()
  })
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

test.describe('Today at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  for (const load of ['kg', 'plates'] as const) {
    test(`Today in ${load} at 320 px and 200 % text does not scroll sideways`, async ({ page }) => {
      await openOnWednesday(page)
      if (load === 'kg') await seedFlatBench(page)
      else await seedPlates(page)
      await tap(page, 'Start session')
      await expect(page.locator('.gr-suggestion')).toBeVisible()
      expect(handedBack(await overflowing(page)), 'with the chip').toEqual([])
      await page.locator('.gr-suggestion').getByRole('button', { name: 'Apply' }).click()
      await expect(page.locator('.gr-suggestion')).toHaveCount(0)
      expect(handedBack(await overflowing(page)), 'applied').toEqual([])
      await tap(page, 'Log set 1')
      // The rest bar's clock shows, and its buttons are inside the screen (the bar is not in the shell-main check).
      await expect(page.getByRole('timer')).toBeVisible()
      for (const name of ['−30 s', '+30 s', 'Skip']) {
        const box = (await page.locator('.shell-rest').getByRole('button', { name, exact: true }).boundingBox())!
        expect(box.x, `${name} starts inside`).toBeGreaterThanOrEqual(-0.5)
        expect(box.x + box.width, `${name} ends inside`).toBeLessThanOrEqual(160.5)
      }
      await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
      expect(handedBack(await overflowing(page)), 'a set logged, the rest bar up').toEqual([])
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    })
  }
})
