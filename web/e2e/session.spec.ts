import { expect, test } from '@playwright/test'
import { openOnWednesday, readAllTables, seedFlatBench, tap, typeInto, weightField } from './helpers'

test('fullSession: start, log 3 sets, finish, take the coach card, see the accessories in the plan', async ({ page }) => {
  await openOnWednesday(page)
  await seedFlatBench(page)

  await tap(page, 'Start session')
  // The same 100 x 5 @ 8.5 as every week before: another flat week.
  await tap(page, 'Increase RPE')
  // The weight is typed straight into the card, and logged from the keypad without leaving the field.
  await expect(weightField(page)).toHaveValue('100')
  await typeInto(page, weightField(page), '100')
  for (const n of [1, 2, 3]) {
    await tap(page, `Log set ${n}`)
    if (n < 3) await expect(page.getByRole('button', { name: `Log set ${n + 1}` })).toBeVisible()
  }
  const todays = async () => ((await readAllTables(page)).sets as { sessionId: number; weightKg: number; reps: number; rpe: number }[]).slice(-3).map((s) => [s.weightKg, s.reps, s.rpe])
  await expect.poll(todays).toEqual([[100, 5, 8.5], [100, 5, 8.5], [100, 5, 8.5]])
  await tap(page, 'Finish')
  await expect(page.getByRole('heading', { name: 'Session saved' })).toBeVisible()

  await page.getByRole('button', { name: /coach notes?/ }).click()
  await expect(page.getByRole('heading', { name: 'Bench has stalled' })).toBeVisible()
  await tap(page, 'Add to plan')
  await expect(page.getByRole('heading', { name: 'Bench has stalled' })).toBeHidden()

  await page.getByRole('link', { name: 'Plan' }).click()
  const wednesday = page.getByRole('region', { name: 'Wednesday' })
  await expect(wednesday.getByText('3 exercises · 9 sets')).toBeVisible()
  await expect(wednesday.getByRole('listitem', { name: 'Spoto press 3 × 5 @ 8' })).toBeVisible()
})
