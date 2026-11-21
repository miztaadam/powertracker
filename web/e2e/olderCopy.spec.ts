import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { tabBar } from './helpers'

// 1.17: this copy of Grit opened on a log a newer Grit wrote (here: the same stores at IndexedDB version 70) shows
// "Update Grit to carry on" and nothing of the log. A normal run writes the screenshots to test-results/screens/;
// UPDATE_SCREENSHOTS=1 rewrites the committed PNGs in docs/design/web/screens/.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

async function shoot(page: Page, name: string) {
  mkdirSync(outDir, { recursive: true })
  await page.evaluate(() => document.fonts.ready)
  await page.screenshot({ path: resolve(outDir, `${name}.png`), animations: 'disabled' })
}

const TITLE = 'Update Grit to carry on'
const BODY = "This copy of Grit is older than your log, so it can't open it safely. Nothing has been changed."
const FAILED = "Couldn't update. Check your connection and try again."

/** Opens Grit once (its database and service worker set up), then moves the database on to version 70, stores unchanged, and reloads. */
async function openOnANewerLog(page: Page, before?: () => Promise<void>) {
  await page.goto('/#/today')
  await expect(tabBar(page)).toBeVisible()
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready
  })
  await page.evaluate(
    () =>
      new Promise<void>((done, fail) => {
        // The open page lets go of its connection when asked (its versionchange handler), so this is not blocked.
        const req = indexedDB.open('grit', 70)
        req.onupgradeneeded = () => {}
        req.onsuccess = () => {
          req.result.close()
          done()
        }
        req.onerror = () => fail(req.error)
      }),
  )
  await before?.()
  await page.reload()
  await expect(page.getByRole('heading', { level: 1, name: TITLE })).toBeVisible()
}

/** The database's IndexedDB version, read without changing it. */
const nativeVersion = (page: Page) =>
  page.evaluate(
    () =>
      new Promise<number>((done, fail) => {
        const req = indexedDB.open('grit')
        req.onsuccess = () => {
          const version = req.result.version
          req.result.close()
          done(version)
        }
        req.onerror = () => fail(req.error)
      }),
  )

for (const scheme of ['dark', 'light'] as const) {
  test(`older copy: the screen, and nothing of the log (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnANewerLog(page)
    await expect(page.getByText(BODY)).toBeVisible()
    await expect(page.getByRole('button')).toHaveText(['Update and reload'])
    await expect(tabBar(page)).toHaveCount(0)
    await expect(page.getByRole('heading', { name: /^Grit couldn't/ })).toHaveCount(0)
    await expect(page.getByRole('button', { name: /export/i })).toHaveCount(0)
    await expect(page.locator('main')).toHaveCount(0)
    // Its look follows the system scheme.
    const background = await page.locator('.gate').evaluate((el) => getComputedStyle(el).backgroundColor)
    const [r = 0, g = 0, b = 0] = background.match(/\d+/g)!.map(Number)
    expect((r + g + b) / 3 > 128).toBe(scheme === 'light')
    await shoot(page, `older-copy-${scheme}`)
    expect(await nativeVersion(page)).toBe(70)
  })
}

test('older copy: no newer version on the server, so it says it could not update, and the button comes back', async ({ page }) => {
  await openOnANewerLog(page)
  const button = page.getByRole('button', { name: 'Update and reload' })
  await button.click()
  await expect(page.getByRole('status')).toHaveText(FAILED)
  await expect(button).toBeEnabled()
  await expect(page.getByRole('heading', { level: 1, name: TITLE })).toBeVisible()
  expect(await nativeVersion(page)).toBe(70)
})

test('older copy: on the bodybuilding side its look is the one remembered from the last visit', async ({ page }) => {
  await openOnANewerLog(page, () => page.evaluate(() => localStorage.setItem('grit:trainingMode', 'BODYBUILDING')))
  await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
})

// 200% text on a 320 × 568 phone: everything laid out in half its width and height (as guide.spec and pictures.spec
// do). Nothing is cut or reaches past the screen; the screen scrolls to its button, which is whole.
test.describe('older copy at 200% text on a 320 px phone', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  test('nothing is cut, and the button is reached whole', async ({ page }) => {
    await openOnANewerLog(page)
    const problems = await page.evaluate(() => {
      const out: string[] = []
      const width = document.documentElement.clientWidth
      if (document.documentElement.scrollWidth > width) out.push(`page scrolls sideways: ${document.documentElement.scrollWidth} > ${width}`)
      for (const el of Array.from(document.querySelectorAll<HTMLElement>('.gate *'))) {
        const box = el.getBoundingClientRect()
        if (box.width === 0) continue
        const name = `${el.tagName.toLowerCase()}.${el.className}`
        if (box.left < -0.5 || box.right > width + 0.5) out.push(`${name} reaches past the screen: ${box.left}..${box.right}`)
        if (el.scrollWidth > el.clientWidth + 1 && getComputedStyle(el).overflowX !== 'visible') out.push(`${name} cuts its text across`)
        if (getComputedStyle(el).textOverflow === 'ellipsis' && el.scrollWidth > el.clientWidth) out.push(`${name} ends in an ellipsis`)
      }
      return out
    })
    expect(problems).toEqual([])
    const button = page.getByRole('button', { name: 'Update and reload' })
    await button.scrollIntoViewIfNeeded()
    const box = (await button.boundingBox())!
    expect(box.y).toBeGreaterThanOrEqual(0)
    expect(box.y + box.height).toBeLessThanOrEqual(284 + 0.5)
    expect(box.height).toBeGreaterThanOrEqual(44)
    // The button's words are all in it.
    expect(await button.evaluate((el) => el.scrollWidth <= el.clientWidth && el.scrollHeight <= el.clientHeight + 1)).toBe(true)
    await expect(page.getByText(BODY)).toBeVisible()
  })
})
