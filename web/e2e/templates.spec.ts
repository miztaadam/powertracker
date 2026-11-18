import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, tabBar } from './helpers'

// Choosing a plan template (1.14, Task 4), as on Android: from an empty plan and from the Plan menu on both sides, the
// list, the draft with its weekdays and Swap, the confirm over a plan, and the narrow phone at 130% zoom.
// A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites the committed PNGs in
// docs/design/web/screens/. Look at them after a visual change.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

async function shoot(page: Page, name: string, height = 844) {
  await page.setViewportSize({ width: 390, height })
  await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 0))
  await page.evaluate(() => document.fonts.ready)
  mkdirSync(outDir, { recursive: true })
  await page.screenshot({ path: resolve(outDir, `${name}.png`), fullPage: true, animations: 'disabled' })
}

/** The whole draft, as the builder's draft shot is taken. */
async function shootTall(page: Page, name: string) {
  const tall = await page.locator('.pb-screen').evaluate((el) => Math.ceil(el.scrollHeight))
  await shoot(page, name, tall + 90)
}

/** The page's own sideways scroll. */
const fitsThePhone = (page: Page) => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)

/**
 * As `overflowing` in narrow.spec.ts (copied, so that spec is unchanged), with the templates' names, lines and headings
 * held to their boxes too: the page or a container scrolling sideways, anything past an edge, text cut off.
 */
const EVERYTHING = '.shell-main, .shell-main *, .gr-tabbar *, .gr-snackbar, .gr-snackbar *, .gr-dialog, .gr-dialog *, .gr-menu, .gr-menu *'
const overflowing = (page: Page, measured = EVERYTHING) =>
  page.evaluate((measured) => {
    const rows = '.lib-chips'
    const found: string[] = []
    const name = (el: Element) => `${el.tagName.toLowerCase()}.${el.className}`
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    for (const el of document.querySelectorAll<HTMLElement>(measured)) {
      if (el.closest('.sr-only')) continue
      const style = getComputedStyle(el)
      const scroller = ['auto', 'scroll'].includes(style.overflowX)
      if (scroller && !el.matches(rows) && el.scrollWidth > el.clientWidth + 1) found.push(`scrolls sideways ${name(el)}: ${el.scrollWidth} > ${el.clientWidth}`)
      if (el.parentElement?.closest(rows)) continue
      const box = el.getBoundingClientRect()
      if (box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5)) found.push(`outside ${name(el)}`)
      // The tab bar's labels are not held to their boxes at 130% zoom on a 320 px phone (narrow.spec.ts says so too).
      if (el.closest('.gr-tabbar')) continue
      if (
        el.matches('li, p, h1, h2, h3, a, label, .gr-btn-text, .gr-chip-face, .gr-segment-face, .gr-snackbar-message, .lib-row-name, .lib-row-muscles, .tpl-name, .tpl-days, .tpl-titles, .plan-row-name') &&
        !scroller &&
        el.scrollWidth > el.clientWidth + 1
      )
        found.push(`clipped ${name(el)}: ${el.textContent}`)
    }
    return found
  }, measured)

/** Plan's own screen is not this task's: on it only the snackbar and the open menu are measured. */
const PLAN_MENU = '.gr-snackbar, .gr-snackbar *, .gr-menu-list, .gr-menu-list *'

/** With the parts of Plan this task changed or found clipping: the header (and its menu button), the empty plan's line and buttons, and the day heads. */
const PLAN_PARTS = `${PLAN_MENU}, .gr-screen-header, .gr-screen-header *, .plan-empty, .plan-empty *, .plan-day-head, .plan-day-head *`

/** The next plan item written fails, once. */
const failNextPlanItem = (page: Page) =>
  page.evaluate(() => {
    const add = IDBObjectStore.prototype.add
    IDBObjectStore.prototype.add = function (this: IDBObjectStore, ...args: Parameters<IDBObjectStore['add']>) {
      if (this.name === 'planItems') {
        IDBObjectStore.prototype.add = add
        throw new DOMException('Broken', 'UnknownError')
      }
      return add.apply(this, args)
    }
  })

/** Exercise and template names, and the day headings, that break inside a word rather than at a space. */
const brokenWords = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    for (const el of document.querySelectorAll<HTMLElement>('.tpl-name, .plan-row-name, .plan-day-head h2, .lib-row-name, .gr-dialog h2')) {
      for (const node of el.childNodes) {
        if (node.nodeType !== Node.TEXT_NODE) continue
        const text = node.textContent ?? ''
        // A break at a space or a hyphen is a line break; inside a word is not.
        const words = /[^\s\-–]+/g
        for (let m = words.exec(text); m != null; m = words.exec(text)) {
          const range = document.createRange()
          range.setStart(node, m.index)
          range.setEnd(node, m.index + m[0].length)
          const lines = new Set([...range.getClientRects()].filter((r) => r.width > 0).map((r) => Math.round(r.top)))
          if (lines.size > 1) found.push(`${m[0]} in "${text}"`)
        }
      }
    }
    return found
  })

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

/** Every stored plan item, as "MODE weekday order name sets×reps-repsHigh @ rpe", in week and plan order. */
const storedPlan = (page: Page) =>
  page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const all = <T,>(store: string) =>
      new Promise<T[]>((done) => {
        const request = db.transaction(store).objectStore(store).getAll()
        request.onsuccess = () => done(request.result as T[])
      })
    const exercises = await all<{ id: number; name: string }>('exercises')
    const items = await all<{ weekday: number; order: number; exerciseId: number; sets: number; reps: number; repsHigh?: number; targetRpe: number; mode?: string }>('planItems')
    const settings = await all<{ key: string; value: unknown }>('settings')
    db.close()
    const names = new Map(exercises.map((e) => [e.id, e.name]))
    return {
      items: items
        .sort((a, b) => (a.mode ?? '').localeCompare(b.mode ?? '') || a.weekday - b.weekday || a.order - b.order)
        .map((p) => `${p.mode ?? 'POWERLIFTING'} ${p.weekday} ${p.order} ${names.get(p.exerciseId)} ${p.sets}x${p.reps}-${'repsHigh' in p ? p.repsHigh : 'none'} @ ${p.targetRpe}`),
      trainingDays: settings.find((s) => s.key === 'trainingDays')?.value,
      bodybuildingTrainingDays: settings.find((s) => s.key === 'bodybuildingTrainingDays')?.value,
    }
  })

const header = (page: Page) => page.locator('.gr-screen-header')
const days = (page: Page) => page.getByRole('group', { name: 'Which days?' })

for (const scheme of ['dark', 'light'] as const) {
  test.describe(`templates (${scheme})`, () => {
    test.beforeEach(async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
    })

    test('powerlifting: from the empty plan to a saved template, then a replace from the menu', async ({ page }) => {
      await openOnWednesday(page, '#/plan')
      await expect(page.getByText('Your plan is empty. Start from a template or add exercises to each day.')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Plan options' })).toHaveCount(0)
      await page.getByRole('button', { name: 'Choose a template' }).click()
      await expect(page).toHaveURL(/#\/plan\/templates\/powerlifting$/)
      await expect(header(page).getByRole('heading', { level: 1, name: 'Choose a template' })).toBeVisible()
      await expect(page.getByRole('list', { name: 'Templates' }).getByRole('listitem')).toHaveCount(3)
      expect(await fitsThePhone(page)).toBe(true)
      await shoot(page, `templates-list-${scheme}`)

      // The 4-day draft on the fixed spread, as the side trains three days.
      await page.getByRole('button', { name: '4-day powerlifting' }).click()
      await expect(header(page).getByRole('heading', { level: 1, name: '4-day powerlifting' })).toBeVisible()
      await expect(page.getByText('Mon, Tue, Thu, Fri')).toBeVisible()
      await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem', { name: 'Squat 4 × 4 @ 8' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Swap Squat' })).toHaveCount(0)
      expect(await fitsThePhone(page)).toBe(true)
      await shootTall(page, `template-draft-${scheme}`)
      await page.setViewportSize({ width: 390, height: 844 })

      // One day off: Use this plan waits for the count, and the cards say Day 1, Day 2, ….
      await days(page).getByText('Fri', { exact: true }).click()
      await expect(page.getByText('Choose 4 days.')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Use this plan' })).toBeDisabled()
      await expect(page.getByRole('heading', { name: 'Day 4 · Deadlift' })).toBeVisible()
      await days(page).getByText('Sat', { exact: true }).click()
      await page.getByRole('button', { name: 'Use this plan' }).click()

      // Back on Plan with the new week and the snackbar.
      await expect(page).toHaveURL(/#\/plan$/)
      await expect(page.getByText('Plan set: 4-day powerlifting.')).toBeVisible()
      await expect(page.getByRole('region', { name: 'Saturday' }).getByRole('listitem', { name: /^Deadlift\s+4 × 4 @ 8$/ })).toBeVisible()
      const saved = await storedPlan(page)
      expect(saved.trainingDays).toEqual([1, 2, 4, 6])
      expect(saved.bodybuildingTrainingDays ?? [1, 3, 5]).toEqual([1, 3, 5])
      expect(saved.items.every((i) => i.startsWith('POWERLIFTING') && i.includes('-none @'))).toBe(true)

      // Over a plan: the menu, then the confirm; Cancel keeps it, Replace replaces it.
      await page.getByRole('button', { name: 'Plan options' }).click()
      await page.getByRole('menuitem', { name: 'Choose a template' }).click()
      await page.getByRole('button', { name: '3-day powerlifting' }).click()
      await page.getByRole('button', { name: 'Use this plan' }).click()
      const confirm = page.getByRole('dialog', { name: 'Replace your powerlifting plan?' })
      await expect(confirm.getByText('Your history and bests are kept.')).toBeVisible()
      await confirm.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
      await shoot(page, `template-confirm-${scheme}`)
      await confirm.getByRole('button', { name: 'Cancel' }).click()
      await expect(confirm).toHaveCount(0)
      expect(await storedPlan(page)).toEqual(saved)
      await page.getByRole('button', { name: 'Use this plan' }).click()
      await page.getByRole('dialog', { name: 'Replace your powerlifting plan?' }).getByRole('button', { name: 'Replace' }).click()
      await expect(page.getByText('Plan set: 3-day powerlifting.')).toBeVisible()
      // The side trains four days now, so the 3-day template started on its fixed spread, which is what is stored.
      expect((await storedPlan(page)).trainingDays).toEqual([1, 3, 5])
    })

    test('bodybuilding: Choose a template over Build my plan, and its draft', async ({ page }) => {
      await openOnWednesday(page, '#/plan')
      await bodybuilding(page)
      await expect(page.getByText('Your bodybuilding plan is empty. Add exercises to each training day.')).toBeVisible()
      const choose = page.getByRole('button', { name: 'Choose a template' })
      const build = page.getByRole('button', { name: 'Build my plan' })
      const [a, b] = [await choose.boundingBox(), await build.boundingBox()]
      expect(a!.y).toBeLessThan(b!.y)
      await choose.click()
      await expect(page).toHaveURL(/#\/plan\/templates\/bodybuilding$/)
      await page.getByRole('button', { name: '4-day upper/lower' }).click()
      await expect(page.getByRole('region', { name: 'Weekly sets' })).toBeVisible()
      await expect(page.getByRole('listitem', { name: 'DB press 4 × 8–12 @ 8' }).first()).toBeVisible()
      expect(await fitsThePhone(page)).toBe(true)
      await shootTall(page, `template-draft-bodybuilding-${scheme}`)
    })
  })
}

test('Plan never shows the empty line before a stored plan is read', async ({ page }) => {
  await openOnWednesday(page, '#/plan')
  await page.getByRole('button', { name: 'Choose a template' }).click()
  await page.getByRole('button', { name: '3-day powerlifting' }).click()
  await page.getByRole('button', { name: 'Use this plan' }).click()
  await expect(page.getByText('Plan set: 3-day powerlifting.')).toBeVisible()
  // From the very first frame of a reload, the empty line and its button never appear over the stored plan.
  await page.addInitScript(() => {
    const seen: string[] = []
    ;(window as unknown as { gritSeen: string[] }).gritSeen = seen
    new MutationObserver(() => {
      if (document.body?.textContent?.includes('Your plan is empty')) seen.push('empty line')
    }).observe(document, { childList: true, subtree: true, characterData: true })
  })
  await page.reload()
  await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem', { name: /^Squat/ })).toBeVisible()
  expect(await page.evaluate(() => (window as unknown as { gritSeen: string[] }).gritSeen)).toEqual([])
})

test('a failed save says so at the top of the draft, and trying again saves', async ({ page }) => {
  await openOnWednesday(page, '#/plan/templates/powerlifting')
  await page.getByRole('button', { name: '3-day powerlifting' }).click()
  // The next plan item written fails, once.
  await page.evaluate(() => {
    const add = IDBObjectStore.prototype.add
    IDBObjectStore.prototype.add = function (this: IDBObjectStore, ...args: Parameters<IDBObjectStore['add']>) {
      if (this.name === 'planItems') {
        IDBObjectStore.prototype.add = add
        throw new DOMException('Broken', 'UnknownError')
      }
      return add.apply(this, args)
    }
  })
  await page.getByRole('button', { name: 'Use this plan' }).click()
  await expect(page.getByRole('alert')).toHaveText('The plan could not be saved. Nothing was changed.')
  expect((await storedPlan(page)).items).toEqual([])
  await page.getByRole('button', { name: 'Use this plan' }).click()
  await expect(page.getByText('Plan set: 3-day powerlifting.')).toBeVisible()
})

test("the header's mode label is the route's side, not the app's", async ({ page }) => {
  await openOnWednesday(page)
  await bodybuilding(page)
  await page.goto('/#/plan/templates/powerlifting')
  await expect(header(page).getByRole('heading', { level: 1, name: 'Choose a template' })).toBeVisible()
  await expect(page.getByRole('button', { name: '3-day powerlifting' })).toBeVisible()
  await expect(page.getByTestId('mode-label')).toHaveCount(0)
  // And the other way: the bodybuilding templates, with the app on powerlifting.
  await page.goto('/#/plan')
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    await new Promise<void>((done, fail) => {
      const tx = db.transaction('settings', 'readwrite')
      tx.objectStore('settings').put({ key: 'trainingMode', value: 'POWERLIFTING' })
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    db.close()
  })
  await page.goto('/#/plan/templates/bodybuilding')
  await page.reload()
  // The powerlifting side draws no data-mode.
  await expect(page.locator('html')).not.toHaveAttribute('data-mode', 'bodybuilding')
  await expect(page.getByRole('button', { name: '3-day full body' })).toBeVisible()
  await expect(header(page).getByTestId('mode-label')).toBeVisible()
})

test.describe('browser back, as Android', () => {
  const list = (page: Page) => page.getByRole('list', { name: 'Templates' })
  // Each starts on Today, then Plan, so a step back from Plan has somewhere to land.

  test('from the draft returns to the list; from the list it leaves for Plan', async ({ page }) => {
    await openOnWednesday(page)
    await page.goto('/#/plan')
    await page.getByRole('button', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '3-day powerlifting' }).click()
    await expect(header(page).getByRole('heading', { level: 1, name: '3-day powerlifting' })).toBeVisible()
    await page.goBack()
    await expect(list(page)).toBeVisible()
    await expect(page).toHaveURL(/#\/plan\/templates\/powerlifting$/)
    await page.goBack()
    await expect(page).toHaveURL(/#\/plan$/)
    await expect(page.getByText('Your plan is empty. Start from a template or add exercises to each day.')).toBeVisible()
  })

  test("the draft's own Back, then the browser's, leaves; and after Close no entry is left behind", async ({ page }) => {
    await openOnWednesday(page)
    await page.goto('/#/plan')
    await page.getByRole('button', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '4-day powerlifting' }).click()
    await page.getByRole('button', { name: 'Back', exact: true }).click()
    await expect(list(page)).toBeVisible()
    await page.goBack()
    await expect(page).toHaveURL(/#\/plan$/)
    // Close from the draft: back on Plan, and the browser's Back does not reopen the templates.
    await page.getByRole('button', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '4-day powerlifting' }).click()
    await page.getByRole('button', { name: 'Close', exact: true }).click()
    await expect(page).toHaveURL(/#\/plan$/)
    await page.goBack()
    await expect(page).not.toHaveURL(/templates/)
  })

  test('after a reload on the draft, a save still goes to Plan and Close leaves in one tap', async ({ page }) => {
    await openOnWednesday(page)
    await page.goto('/#/plan')
    await page.getByRole('button', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '3-day powerlifting' }).click()
    await page.reload()
    await expect(list(page)).toBeVisible()
    await page.getByRole('button', { name: '3-day powerlifting' }).click()
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await expect(page.getByText('Plan set: 3-day powerlifting.')).toBeVisible()
    await expect(page).toHaveURL(/#\/plan$/)

    await page.getByRole('button', { name: 'Plan options' }).click()
    await page.getByRole('menuitem', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '4-day powerlifting' }).click()
    await page.reload()
    await expect(list(page)).toBeVisible()
    await page.getByRole('button', { name: 'Close', exact: true }).click()
    await expect(page).toHaveURL(/#\/plan$/)
  })

  test('after a save, back from Plan never returns to the templates, opened from Plan or from a link', async ({ page }) => {
    await openOnWednesday(page)
    await page.goto('/#/plan')
    await page.getByRole('button', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '3-day powerlifting' }).click()
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await expect(page.getByText('Plan set: 3-day powerlifting.')).toBeVisible()
    await expect(page).toHaveURL(/#\/plan$/)
    await page.goBack()
    await expect(page).toHaveURL(/#\/today$/)

    // A link straight to the templates, from Today: the save puts Plan in its place.
    await page.goto('/#/plan/templates/powerlifting')
    await page.getByRole('button', { name: '4-day powerlifting' }).click()
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await page.getByRole('dialog', { name: 'Replace your powerlifting plan?' }).getByRole('button', { name: 'Replace' }).click()
    await expect(page.getByText('Plan set: 4-day powerlifting.')).toBeVisible()
    await expect(page).toHaveURL(/#\/plan$/)
    await page.goBack()
    await expect(page).toHaveURL(/#\/today$/)
  })
})

// The narrowest phone, and the same at 130% zoom (laid out 1.3 times narrower and shorter): the list, the drafts, the
// Swap sheet, the confirm, the save-failed line and the open Plan menu never scroll sideways or cut a name off, and
// no name breaks inside a word.
for (const [label, viewport] of [
  ['a 320 px phone', { width: 320, height: 640 }],
  ['a 320 px phone at 130% zoom', { width: Math.floor(320 / 1.3), height: Math.floor(640 / 1.3) }],
] as const) {
  test.describe(`templates on ${label}`, () => {
    test.use({ viewport })

    test('the powerlifting list, draft, Swap sheet, save-failed line, confirm and Plan menu fit', async ({ page }) => {
      await openOnWednesday(page, '#/plan/templates/powerlifting')
      await expect(page.getByRole('list', { name: 'Templates' }).getByRole('listitem')).toHaveCount(3)
      expect(await overflowing(page)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
      for (const name of ['3-day powerlifting', '4-day powerlifting', '5-day powerlifting']) {
        await page.getByRole('button', { name }).click()
        await expect(header(page).getByRole('heading', { level: 1, name })).toBeVisible()
        expect(await overflowing(page), name).toEqual([])
        expect(await brokenWords(page), name).toEqual([])
        // The cards' headings while the count is off.
        await days(page).getByText('Mon', { exact: true }).click()
        await expect(page.getByRole('heading', { name: /^Day 1 · / })).toBeVisible()
        expect(await overflowing(page), `${name}, days off`).toEqual([])
        await days(page).getByText('Mon', { exact: true }).click()
        if (name !== '5-day powerlifting') await page.getByRole('button', { name: 'Back', exact: true }).click()
      }
      await page.getByRole('button', { name: 'Swap Lat pulldown' }).first().click()
      const sheet = page.getByRole('dialog', { name: 'Swap Lat pulldown' })
      await expect(sheet.getByRole('listitem').first()).toBeVisible()
      await sheet.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
      expect(await overflowing(page)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
      await sheet.getByRole('button', { name: 'Close', exact: true }).click()

      // The save-failed line.
      await failNextPlanItem(page)
      await page.getByRole('button', { name: 'Use this plan' }).click()
      await expect(page.getByRole('alert')).toBeVisible()
      expect(await overflowing(page)).toEqual([])
      await page.getByRole('button', { name: 'Use this plan' }).click()
      await expect(page.getByText('Plan set: 5-day powerlifting.')).toBeVisible()
      // Plan's header with its menu icon, and the day heads ("Wednesday" is the longest).
      await expect(header(page).getByRole('button', { name: 'Plan options' })).toBeVisible()
      await expect(page.getByRole('heading', { level: 2, name: 'Wednesday' })).toBeVisible()
      expect(await overflowing(page, PLAN_PARTS)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)

      // The open Plan menu, then the confirm over a plan.
      await page.getByRole('button', { name: 'Plan options' }).click()
      await expect(page.getByRole('menuitem', { name: 'Choose a template' })).toBeVisible()
      expect(await overflowing(page, PLAN_PARTS)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await page.getByRole('menuitem', { name: 'Choose a template' }).click()
      await page.getByRole('button', { name: '3-day powerlifting' }).click()
      await page.getByRole('button', { name: 'Use this plan' }).click()
      const confirm = page.getByRole('dialog', { name: 'Replace your powerlifting plan?' })
      await confirm.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
      expect(await overflowing(page)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
    })

    test('the bodybuilding list, drafts, Swap sheet, save-failed line, confirm and Plan menu fit', async ({ page }) => {
      await openOnWednesday(page)
      await bodybuilding(page)
      await page.goto('/#/plan/templates/bodybuilding')
      await expect(page.getByRole('list', { name: 'Templates' }).getByRole('listitem')).toHaveCount(3)
      expect(await overflowing(page)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
      for (const name of ['3-day full body', '4-day upper/lower', '5-day body-part split']) {
        await page.getByRole('button', { name }).click()
        await expect(page.getByRole('region', { name: 'Weekly sets' })).toBeVisible()
        expect(await overflowing(page), name).toEqual([])
        expect(await brokenWords(page), name).toEqual([])
        // The cards' headings while the count is off.
        await days(page).getByText('Mon', { exact: true }).click()
        await expect(page.getByRole('heading', { name: /^Day 1 · / })).toBeVisible()
        expect(await overflowing(page), `${name}, days off`).toEqual([])
        expect(await brokenWords(page), `${name}, days off`).toEqual([])
        await days(page).getByText('Mon', { exact: true }).click()
        if (name !== '5-day body-part split') await page.getByRole('button', { name: 'Back', exact: true }).click()
      }
      await page.getByRole('button', { name: 'Swap Neutral-grip pull-up' }).click()
      const sheet = page.getByRole('dialog', { name: 'Swap Neutral-grip pull-up' })
      await expect(sheet.getByRole('listitem').first()).toBeVisible()
      await sheet.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
      expect(await overflowing(page)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
      await sheet.getByRole('button', { name: 'Close', exact: true }).click()

      // The save-failed line.
      await failNextPlanItem(page)
      await page.getByRole('button', { name: 'Use this plan' }).click()
      await expect(page.getByRole('alert')).toBeVisible()
      expect(await overflowing(page)).toEqual([])
      await page.getByRole('button', { name: 'Use this plan' }).click()
      await expect(page.getByText('Plan set: 5-day body-part split.')).toBeVisible()
      await expect(header(page).getByRole('button', { name: 'Plan options' })).toBeVisible()
      await expect(page.getByRole('heading', { level: 2, name: 'Wednesday' })).toBeVisible()
      expect(await overflowing(page, PLAN_PARTS)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)

      // The open Plan menu with its two items, then the confirm over a plan.
      await page.getByRole('button', { name: 'Plan options' }).click()
      await expect(page.getByRole('menuitem')).toHaveText(['Rebuild my plan', 'Choose a template'])
      expect(await overflowing(page, PLAN_PARTS)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await page.getByRole('menuitem', { name: 'Choose a template' }).click()
      await page.getByRole('button', { name: '3-day full body' }).click()
      await page.getByRole('button', { name: 'Use this plan' }).click()
      const confirm = page.getByRole('dialog', { name: 'Replace your bodybuilding plan?' })
      await expect(confirm.getByText('Your history and bests are kept.')).toBeVisible()
      await confirm.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
      expect(await overflowing(page)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
    })

    // The bodybuilding builder's bar, which the templates' draft shares: on every step each label is whole, each face
    // inside its own button, and the buttons side by side without overlapping.
    test("the plan builder's bar, and the templates', fit on every step", async ({ page }) => {
      const bar = '.pb-actions, .pb-actions *'
      const barProblems = async () => [
        ...(await overflowing(page, bar)),
        ...(await page.evaluate(() => {
          const found: string[] = []
          const buttons = [...document.querySelectorAll<HTMLElement>('.pb-actions > .gr-btn')]
          for (const b of buttons) {
            const box = b.getBoundingClientRect()
            const face = b.querySelector('.gr-btn-face')!.getBoundingClientRect()
            if (face.left < box.left - 0.5 || face.right > box.right + 0.5) found.push(`face wider than its button: ${b.textContent}`)
          }
          for (let i = 1; i < buttons.length; i++) {
            if (buttons[i]!.getBoundingClientRect().left < buttons[i - 1]!.getBoundingClientRect().right - 0.5) found.push(`overlap: ${buttons[i]!.textContent}`)
          }
          return found
        })),
      ]
      await openOnWednesday(page, '#/plan')
      await bodybuilding(page)
      await page.getByRole('button', { name: 'Build my plan' }).click()
      await expect(page.locator('.pb-actions').getByRole('button', { name: 'Next' })).toBeVisible()
      expect(await barProblems(), 'days').toEqual([])
      await page.getByRole('button', { name: 'Next' }).click()
      await expect(page.locator('.pb-actions').getByRole('button', { name: 'Choose for me' })).toBeVisible()
      expect(await barProblems(), 'exercises').toEqual([])
      await page.getByRole('button', { name: 'Choose for me' }).click()
      await expect(page.locator('.pb-actions').getByRole('button', { name: 'Skip' })).toBeVisible()
      expect(await barProblems(), 'body parts').toEqual([])
      await page.getByRole('button', { name: 'Skip' }).click()
      await expect(page.locator('.pb-actions').getByRole('button', { name: 'Use this plan' })).toBeVisible()
      expect(await barProblems(), 'draft').toEqual([])
      expect(await fitsThePhone(page)).toBe(true)

      await page.goto('/#/plan/templates/bodybuilding')
      await page.getByRole('button', { name: '3-day full body' }).click()
      await expect(page.locator('.pb-actions').getByRole('button', { name: 'Use this plan' })).toBeVisible()
      expect(await barProblems(), 'a template').toEqual([])
    })

    test('the empty plan, on both sides, fits: its line and its buttons one over the other', async ({ page }) => {
      await openOnWednesday(page, '#/plan')
      const choose = page.locator('.plan-empty').getByRole('button', { name: 'Choose a template' })
      await expect(page.getByText('Your plan is empty. Start from a template or add exercises to each day.')).toBeVisible()
      await expect(choose).toBeVisible()
      expect(await overflowing(page, PLAN_PARTS)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)

      await bodybuilding(page)
      await expect(page.getByText('Your bodybuilding plan is empty. Add exercises to each training day.')).toBeVisible()
      const build = page.locator('.plan-empty').getByRole('button', { name: 'Build my plan' })
      await expect(build).toBeVisible()
      const [a, b] = [await choose.boundingBox(), await build.boundingBox()]
      expect(a!.y + a!.height).toBeLessThanOrEqual(b!.y)
      expect(await overflowing(page, PLAN_PARTS)).toEqual([])
      expect(await brokenWords(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
    })
  })
}
