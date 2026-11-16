import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, tabBar, tap, weightField } from './helpers'

// The narrowest phones in use: many Android phones are 320–360 px wide (an iPhone is 375 px and up). On each
// screen, on both training sides, the page must not scroll sideways and nothing may reach past the screen or
// out of its own box.

test.use({ viewport: { width: 320, height: 568 } })

/**
 * Things wider than the phone: the page, or any element of the screen, the tab bar, the snackbar or an open
 * sheet reaching past either edge, and text that does not fit its own box. A row that scrolls sideways on
 * purpose (overflow-x auto or scroll) may hold more than the screen.
 */
const overflowing = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    const scrollsSideways = (el: Element) => ['auto', 'scroll'].includes(getComputedStyle(el).overflowX)
    for (const el of document.querySelectorAll<HTMLElement>('.shell-main *, .gr-tabbar *, .gr-snackbar, .gr-snackbar *, .gr-dialog, .gr-dialog *')) {
      if (el.closest('.sr-only')) continue
      let inScroller = false
      for (let up = el.parentElement; up != null && up !== document.body; up = up.parentElement) {
        if (scrollsSideways(up)) {
          inScroller = true
          break
        }
      }
      const box = el.getBoundingClientRect()
      if (!inScroller && box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5)) found.push(`${el.tagName.toLowerCase()}.${el.className}`)
      // Text that does not fit its own box.
      if (
        el.matches('li, p, h1, h2, h3, a, label, .gr-btn-text, .gr-chip-face, .gr-segment-face, .gr-snackbar-message, .lib-row-name, .lib-row-muscles') &&
        !scrollsSideways(el) &&
        el.scrollWidth > el.clientWidth + 1
      )
        found.push(`clipped ${el.tagName.toLowerCase()}.${el.className}: ${el.textContent}`)
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

for (const side of ['powerlifting', 'bodybuilding'] as const) {
  test.describe(`a 320 px phone, ${side} side`, () => {
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
