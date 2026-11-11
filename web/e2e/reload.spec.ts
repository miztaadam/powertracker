import { expect, test } from '@playwright/test'
import { openOnWednesday, repsField, rpeField, seedFlatBench, tap, typeInto, weightField } from './helpers'

test('reloadMidSession_keepsEverything', async ({ page }) => {
  await openOnWednesday(page)
  await seedFlatBench(page)

  await tap(page, 'Start session')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  await tap(page, 'Log set 2')
  await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
  // Numbers typed for the next set, and the rest after the last one.
  await typeInto(page, weightField(page), '102,5')
  await tap(page, 'Increase Weight')
  await typeInto(page, repsField(page), '4')
  await page.keyboard.press('Enter')
  await tap(page, 'Decrease RPE')
  await expect(weightField(page)).toHaveValue('105')
  await expect(page.getByRole('timer', { name: 'Rest, 3:00 left' })).toBeVisible()

  await page.reload()

  // Still in the session, at set 3, with the numbers and the rest as they were.
  await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Finish', exact: true })).toBeVisible()
  await expect(weightField(page)).toHaveValue('105')
  await expect(repsField(page)).toHaveValue('4')
  await expect(rpeField(page)).toHaveValue('7.5')
  await expect(page.getByRole('timer', { name: 'Rest, 3:00 left' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Start session' })).toHaveCount(0)

  // The two sets are in the log: finishing counts them with the third.
  await tap(page, 'Log set 3')
  await tap(page, 'Finish')
  await expect(page.getByRole('heading', { name: 'Session saved' })).toBeVisible()
  await expect(page.locator('.today-summary-stats')).toContainText('3')
})
