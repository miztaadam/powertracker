import { expect, test } from '@playwright/test'

test('app loads and shows the tab bar', async ({ page }) => {
  await page.goto('/')
  const nav = page.getByRole('navigation', { name: 'Main' })
  await expect(nav).toBeVisible()
  for (const name of ['Today', 'Coach', 'Progress', 'Plan']) {
    await expect(nav.getByRole('link', { name })).toBeVisible()
  }
})
