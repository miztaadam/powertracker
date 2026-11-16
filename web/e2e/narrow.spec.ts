import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, tabBar, tap, weightField } from './helpers'

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
