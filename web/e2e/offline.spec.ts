import { expect, test } from '@playwright/test'
import { openOnWednesday, seedFlatBench, tap } from './helpers'

test('worksOffline: load, go offline, reload, log a set', async ({ page, context }) => {
  await openOnWednesday(page)
  await seedFlatBench(page)
  // The service worker has installed, cached the app and taken over this page.
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready
  })
  await page.reload()
  await expect.poll(() => page.evaluate(() => navigator.serviceWorker.controller != null)).toBe(true)

  await context.setOffline(true)
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Today' })).toBeVisible()
  await expect(page.getByText('Wednesday · 1 exercise')).toBeVisible()

  await tap(page, 'Start session')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  await expect(page.getByRole('timer', { name: 'Rest, 3:00 left' })).toBeVisible()

  // Other screens open offline too, straight from the address.
  await page.goto('/#/progress')
  await page.reload()
  await expect(page.getByRole('heading', { level: 1, name: 'Progress' })).toBeVisible()
})
