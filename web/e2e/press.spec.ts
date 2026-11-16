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

  test('a quick tap still shows its sink', async ({ page }) => {
    const button = await aButton(page)
    await button.scrollIntoViewIfNeeded()
    const box = (await button.boundingBox())!
    // Watch the scale every frame from before the tap; the tap's down and up come with no wait between them.
    const smallest = button.evaluate(
      (el) =>
        new Promise<number>((done) => {
          let min = 1
          const start = performance.now()
          const frame = () => {
            const t = getComputedStyle(el).transform
            min = Math.min(min, t === 'none' ? 1 : new DOMMatrixReadOnly(t).a)
            if (performance.now() - start < 400) requestAnimationFrame(frame)
            else done(min)
          }
          requestAnimationFrame(frame)
        }),
    )
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    await page.mouse.down()
    await page.mouse.move(2, 2)
    await page.mouse.up()
    expect(await smallest).toBeLessThan(0.99)
    await expect.poll(() => scaleOf(button)).toBe(1)
  })

  test('chips, muscle tiles, icon buttons and the Muscles drop-down sink too', async ({ page }) => {
    await page.goto('/#/plan')
    await page.locator('section[aria-label="Monday"]').getByRole('button', { name: 'Add' }).click()
    const picker = page.getByRole('dialog', { name: 'Add to Monday' })
    for (const target of [
      picker.locator('.gr-chip').first(),
      picker.locator('.lib-muscle').first(),
      picker.getByRole('button', { name: 'Close', exact: true }).first(),
    ]) {
      await pressOn(page, target)
      await expect.poll(() => scaleOf(target)).toBeCloseTo(0.97, 3)
      await release(page)
      await expect.poll(() => scaleOf(target)).toBe(1)
    }
    await page.goto('/#/exercises')
    const dropdown = page.getByRole('button', { name: /^Muscles/ })
    await pressOn(page, dropdown)
    await expect.poll(() => scaleOf(dropdown)).toBeCloseTo(0.97, 3)
    await release(page)
  })

  test('a Settings switch sinks when its row is pressed', async ({ page }) => {
    await page.goto('/#/settings')
    const row = page.locator('label.settings-row').filter({ hasText: 'Change plates' })
    const toggle = row.getByRole('switch')
    await pressOn(page, row.locator('.settings-supporting, p').first())
    await expect.poll(() => scaleOf(toggle)).toBeCloseTo(0.97, 3)
    await release(page)
    await expect.poll(() => scaleOf(toggle)).toBe(1)
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
