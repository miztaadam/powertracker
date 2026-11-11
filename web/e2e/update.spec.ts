import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, seedFlatBench, tap } from './helpers'

// The built service worker script, as `vite preview` serves it.
const SW_PATH = resolve(process.cwd(), 'dist/sw.js')

// It rewrites the built dist/sw.js for a moment; nothing else in this file runs beside it.
test.describe.configure({ mode: 'serial' })

test('updateDoesNotReloadDuringSession', async ({ page }) => {
  const original = readFileSync(SW_PATH, 'utf8')
  try {
    await runSession(page, original)
  } finally {
    writeFileSync(SW_PATH, original)
  }
})

async function runSession(page: Page, original: string) {
  await openOnWednesday(page)
  await seedFlatBench(page)
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready
  })
  await page.reload()
  await expect.poll(() => page.evaluate(() => navigator.serviceWorker.controller != null)).toBe(true)

  // Mid-session: a marker in the page stands for "nothing reloaded".
  await tap(page, 'Start session')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  await page.evaluate(() => {
    ;(window as unknown as { notReloaded: boolean }).notReloaded = true
  })

  // A second deploy: the script changes by a comment, so the browser sees a new version, installs it and leaves it waiting.
  writeFileSync(SW_PATH, `${original}\n// next version`)
  await page.evaluate(async () => {
    await (await navigator.serviceWorker.getRegistration())!.update()
  })
  await expect.poll(() => page.evaluate(async () => (await navigator.serviceWorker.getRegistration())!.waiting != null)).toBe(true)

  // The new version waits; the session is not touched and no prompt interrupts it.
  // The page hears about the waiting version a moment after the browser has it: let that settle. The
  // prompt appearing after Finish below proves it was heard.
  await page.evaluate(() => new Promise((done) => requestAnimationFrame(() => requestAnimationFrame(done))))
  await expect(page.getByText('Update ready — tap to refresh')).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  expect(await page.evaluate(() => (window as unknown as { notReloaded?: boolean }).notReloaded)).toBe(true)

  // Finished: Finish clears the session's note, so now the prompt shows. Still nothing reloads until it is tapped.
  await tap(page, 'Finish')
  await expect(page.getByRole('heading', { name: 'Session saved' })).toBeVisible()
  await expect(page.getByText('Update ready — tap to refresh')).toBeVisible()
  await expect(page.getByText('Update ready — tap to refresh')).toBeVisible()
  expect(await page.evaluate(() => (window as unknown as { notReloaded?: boolean }).notReloaded)).toBe(true)

  const reloaded = page.waitForEvent('load')
  await page.getByText('Update ready — tap to refresh').click()
  await reloaded
  expect(await page.evaluate(() => (window as unknown as { notReloaded?: boolean }).notReloaded)).toBeUndefined()
  await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible()
}
