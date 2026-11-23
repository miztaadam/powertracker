import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test } from '@playwright/test'

// The showcase is dev-only, so it is served by the Vite dev server (see playwright.config.ts).
// A normal run writes the screenshots to test-results/ as a smoke check; UPDATE_SCREENSHOTS=1
// rewrites the committed PNGs in docs/design/web/screens/. Look at them after a visual change.
const SHOWCASE_URL = 'http://localhost:4174/#/showcase'
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

for (const scheme of ['dark', 'light'] as const) {
  test(`showcase renders every component (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await page.goto(SHOWCASE_URL)
    await expect(page.getByRole('heading', { level: 1, name: 'Today' })).toBeVisible()
    await expect(page.getByText('25 + 25 + 25 + 5 + 1.25')).toBeVisible()
    await page.evaluate(() => document.fonts.ready)

    // Touch targets: every button, link and choice is at least 48 px in both directions (to within sub-pixel rounding).
    const small = await page.evaluate(() =>
      Array.from(document.querySelectorAll<HTMLElement>('button, a[href], label.gr-segment, label.gr-chip'))
        .map((el) => ({ el, box: el.getBoundingClientRect() }))
        .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
        .map(({ el, box }) => `${el.className} ${Math.round(box.width)}×${Math.round(box.height)}`),
    )
    expect(small).toEqual([])

    // Inputs are at least 16 px, so iOS does not zoom on focus.
    const sizes = await page.locator('input:not([type=radio]):not([type=checkbox])').evaluateAll((els) => els.map((el) => parseFloat(getComputedStyle(el).fontSize)))
    expect(sizes.length).toBeGreaterThan(0)
    expect(Math.min(...sizes)).toBeGreaterThanOrEqual(16)

    // Nothing is wider than the phone.
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)

    mkdirSync(outDir, { recursive: true })
    await page.screenshot({ path: resolve(outDir, `showcase-${scheme}.png`), fullPage: true, animations: 'disabled' })
  })
}

test('the showcase is not in the production build', async ({ page }) => {
  await page.goto('/#/showcase')
  await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible()
  await expect(page.getByRole('heading', { level: 1, name: 'Today' })).toBeVisible()
  await expect(page.getByText('Coach suggests')).toHaveCount(0)
})
