import { expect, test, type Locator, type Page } from '@playwright/test'
import { openOnWednesday, tabBar } from './helpers'

// Stage 2's press feedback (src/ui/press.css): a pressed control sinks (scale below 1) with motion on, and never with
// motion off or when it is disabled. Read from the computed transform while the mouse button is held down on it.

/** The x scale of the element's computed transform: 1 when it has none. */
const scaleOf = (target: Locator) =>
  target.evaluate((el) => {
    const t = getComputedStyle(el).transform
    return t === 'none' ? 1 : new DOMMatrixReadOnly(t).a
  })

/** Holds the mouse button down on the middle of `target`, once the cards above it have stopped arriving. */
async function pressOn(page: Page, target: Locator) {
  await target.scrollIntoViewIfNeeded()
  let box = await target.boundingBox()
  await expect
    .poll(async () => {
      const last = box
      await page.waitForTimeout(150)
      box = await target.boundingBox()
      return JSON.stringify(box) === JSON.stringify(last)
    })
    .toBe(true)
  await page.mouse.move(box!.x + box!.width / 2, box!.y + box!.height / 2)
  await page.mouse.down()
}

/**
 * Lets go away from the press, so no click fires and the screen stays as it is (as when a finger slides off a
 * button before lifting).
 */
async function release(page: Page) {
  await page.mouse.move(2, 2)
  await page.mouse.up()
}

/** Today's "Start session", marked so the locator keeps naming the same element when the test disables it. */
async function aButton(page: Page): Promise<Locator> {
  const start = page.getByRole('button', { name: 'Start session' })
  await expect(start).toBeVisible()
  await start.evaluate((el) => el.setAttribute('data-press-test', ''))
  return page.locator('[data-press-test]')
}

test.describe('with motion on', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test.beforeEach(async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'no-preference' })
    await openOnWednesday(page)
    expect(await page.evaluate(() => document.documentElement.getAttribute('data-motion'))).toBeNull()
  })

  test('a pressed button sinks, and springs back on release', async ({ page }) => {
    const button = await aButton(page)
    expect(await scaleOf(button)).toBe(1)
    await pressOn(page, button)
    expect(await button.evaluate((el) => el.matches(':active'))).toBe(true)
    await expect.poll(() => scaleOf(button)).toBeLessThan(1)
    await expect.poll(() => scaleOf(button)).toBeCloseTo(0.97, 3)
    await release(page)
    await expect.poll(() => scaleOf(button)).toBe(1)
  })

  test('a pressed tab icon sinks further', async ({ page }) => {
    const icon = tabBar(page).getByRole('link', { name: 'Plan' }).locator('.gr-tab-indicator')
    await pressOn(page, icon)
    await expect.poll(() => scaleOf(icon)).toBeCloseTo(0.92, 3)
    await release(page)
  })

  test('a disabled button never sinks', async ({ page }) => {
    const button = await aButton(page)
    for (const disable of ['disabled', 'aria-disabled'] as const) {
      await button.evaluate((el, how) => (how === 'disabled' ? el.setAttribute('disabled', '') : el.setAttribute('aria-disabled', 'true')), disable)
      await pressOn(page, button)
      // A disabled button never becomes :active in Chromium; an aria-disabled one does, so check the press landed.
      if (disable === 'aria-disabled') expect(await button.evaluate((el) => el.matches(':active'))).toBe(true)
      // Longer than the 90 ms press.
      await page.waitForTimeout(200)
      expect(await scaleOf(button), disable).toBe(1)
      await release(page)
      await button.evaluate((el) => {
        el.removeAttribute('disabled')
        el.removeAttribute('aria-disabled')
      })
    }
  })
})

test('with motion off, a pressed button does not sink', async ({ page }) => {
  await openOnWednesday(page)
  expect(await page.evaluate(() => document.documentElement.getAttribute('data-motion'))).toBe('off')
  const button = await aButton(page)
  await pressOn(page, button)
  expect(await button.evaluate((el) => el.matches(':active'))).toBe(true)
  await page.waitForTimeout(200)
  expect(await scaleOf(button)).toBe(1)
  await release(page)
})
