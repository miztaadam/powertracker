import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday } from './helpers'
import { MOTION_ON } from './storage'

// Stage 2's motion-off switch: data-motion="off" on <html>, from the e2e storage key or the system's reduced motion.
const motion = (page: Page) => page.evaluate(() => document.documentElement.getAttribute('data-motion'))

test('every e2e page runs with motion off', async ({ page }) => {
  await openOnWednesday(page)
  expect(await motion(page)).toBe('off')
  await page.reload()
  expect(await motion(page)).toBe('off')
})

test.describe('without the test flag', () => {
  test.use({ storageState: MOTION_ON })

  test('motion is on, and follows reduced motion live', async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'no-preference' })
    await openOnWednesday(page)
    expect(await motion(page)).toBeNull()
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await expect.poll(() => motion(page)).toBe('off')
    await page.emulateMedia({ reducedMotion: 'no-preference' })
    await expect.poll(() => motion(page)).toBeNull()
  })

  test('reduced motion turns it off from the first paint', async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await openOnWednesday(page)
    expect(await motion(page)).toBe('off')
  })
})
