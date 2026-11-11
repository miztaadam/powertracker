import { expect, test } from '@playwright/test'
import { openOnWednesday, seedFlatBench, tap } from './helpers'

test('reloadMidSession_keepsEverything', async ({ page }) => {
  await openOnWednesday(page)
  await seedFlatBench(page)

  await tap(page, 'Start session')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  await tap(page, 'Log set 2')
  await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
  // Numbers typed for the next set, and the rest after the last one.
  await tap(page, 'Increase Weight', 2)
  await tap(page, 'Decrease RPE')
  await expect(page.getByLabel('Weight 105 kg, type Weight')).toBeVisible()
  await expect(page.getByRole('timer', { name: 'Rest, 3:00 left' })).toBeVisible()

  await page.reload()

  // Still in the session, at set 3, with the numbers and the rest as they were.
  await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Finish', exact: true })).toBeVisible()
  await expect(page.getByLabel('Weight 105 kg, type Weight')).toBeVisible()
  await expect(page.getByLabel('RPE 7.5')).toBeVisible()
  await expect(page.getByRole('timer', { name: 'Rest, 3:00 left' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Start session' })).toHaveCount(0)

  // The two sets are in the log: finishing counts them with the third.
  await tap(page, 'Log set 3')
  await tap(page, 'Finish')
  await expect(page.getByRole('heading', { name: 'Session saved' })).toBeVisible()
  await expect(page.locator('.today-summary-stats')).toContainText('3')
})
