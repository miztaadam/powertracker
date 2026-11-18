import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, pastTheTapWindow, tabBar } from './helpers'

// 1.13: skipping a planned exercise on Today, and the coach's card for it. A normal run writes the screenshots to
// test-results/screens/; UPDATE_SCREENSHOTS=1 rewrites the committed PNGs in docs/design/web/screens/.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

async function shoot(page: Page, name: string) {
  mkdirSync(outDir, { recursive: true })
  await page.evaluate(() => document.fonts.ready)
  await page.screenshot({ path: resolve(outDir, `${name}.png`), animations: 'disabled' })
}

type Side = 'POWERLIFTING' | 'BODYBUILDING'

/** Plans `names` on Wednesdays, in that order, on `side`'s side, and puts the app on that side; then reloads. */
async function planWednesday(page: Page, side: Side, names: string[]) {
  await page.evaluate(
    async ({ side, names }) => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'planItems', 'settings'], 'readwrite')
      const settled = new Promise<void>((done, fail) => {
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
        const all = tx.objectStore('exercises').getAll()
        all.onsuccess = () => done(all.result)
      })
      names.forEach((name, order) => {
        const exerciseId = exercises.find((e) => e.name === name)!.id
        const item = side === 'BODYBUILDING' ? { sets: 3, reps: 8, repsHigh: 12 } : { sets: 3, reps: 5 }
        tx.objectStore('planItems').add({ weekday: 3, exerciseId, targetRpe: 8, order, mode: side, ...item })
      })
      tx.objectStore('settings').put({ key: 'trainingMode', value: side })
      await settled
      db.close()
    },
    { side, names },
  )
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

/** Opens `name`'s row menu and taps Skip today; the reason sheet opens and, after its moment, takes taps. */
async function openSheet(page: Page, name: string) {
  await page.getByRole('button', { name: `Options for ${name}` }).click()
  await page.getByRole('menuitem', { name: 'Skip today' }).click()
  const sheet = page.getByRole('dialog', { name: `Why skip ${name}?` })
  await expect(sheet).toBeVisible()
  await pastTheTapWindow(page)
  return sheet
}

const skippedLine = (page: Page) => page.getByRole('region', { name: 'Skipped today' })

test('skip_painOnBench_rowLeaves_cardShows_undoTakesBoth', async ({ page }) => {
  await openOnWednesday(page)
  await planWednesday(page, 'POWERLIFTING', ['Squat', 'Bench'])
  await expect(page.getByText('Wednesday · 2 exercises')).toBeVisible()

  // Back cancels: nothing is stored, the row stays.
  await openSheet(page, 'Bench')
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog', { name: 'Why skip Bench?' })).toBeHidden()
  await expect(page.getByText('Wednesday · 2 exercises')).toBeVisible()

  // Pain: the row leaves, before any session, and the snackbar offers Undo.
  await (await openSheet(page, 'Bench')).getByRole('button', { name: 'Pain' }).click()
  await expect(page.getByText('Bench skipped.')).toBeVisible()
  await expect(page.getByText('Wednesday · 1 exercise')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Options for Bench' })).toBeHidden()
  await expect(skippedLine(page).getByText('Bench')).toBeVisible()

  // Undo takes both back: the skip and its card.
  await page.getByRole('button', { name: 'Undo' }).click()
  await expect(page.getByText('Wednesday · 2 exercises')).toBeVisible()
  await expect(skippedLine(page)).toBeHidden()
  await tabBar(page).getByRole('link', { name: 'Coach' }).click()
  await expect(page.getByText('Nothing open')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Bench skipped' })).toBeHidden()

  // Skipped again: the coach's card shows, a main lift's, Dismiss only.
  await tabBar(page).getByRole('link', { name: 'Today' }).click()
  await (await openSheet(page, 'Bench')).getByRole('button', { name: 'Pain' }).click()
  await expect(skippedLine(page).getByText('Bench')).toBeVisible()
  await tabBar(page).getByRole('link', { name: 'Coach' }).click()
  const card = page.locator('.gr-card').filter({ has: page.getByRole('heading', { name: 'Bench skipped' }) })
  await expect(card.getByText("You skipped Bench because of pain. Its weight won't go up until you do it again. If the pain persists or gets worse, see a physician.")).toBeVisible()
  await expect(card.getByRole('button')).toHaveText(['Dismiss'])

  // Bring back, the same as Undo once the snackbar has gone: the card goes with the skip.
  await tabBar(page).getByRole('link', { name: 'Today' }).click()
  await skippedLine(page).getByRole('button', { name: 'Bring back Bench' }).click()
  await expect(page.getByText('Wednesday · 2 exercises')).toBeVisible()
  await tabBar(page).getByRole('link', { name: 'Coach' }).click()
  await expect(page.getByText('Nothing open')).toBeVisible()
})

for (const scheme of ['dark', 'light'] as const) {
  for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
    const suffix = `${side === 'BODYBUILDING' ? '-bodybuilding' : ''}-${scheme}`
    test(`screenshots: skipping on Today and the coach's cards, ${side.toLowerCase()} side (${scheme})`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
      await openOnWednesday(page)
      await planWednesday(page, side, ['Squat', 'Bench', 'Curl'])
      await page.getByRole('button', { name: 'Start session' }).click()

      // The row menu, open on Curl.
      await page.getByRole('button', { name: 'Options for Curl' }).click()
      await expect(page.getByRole('menuitem', { name: 'Skip today' })).toBeVisible()
      await shoot(page, `today-skip-menu${suffix}`)

      // Squat first, a main lift: its card is the hold notice, Dismiss only.
      await page.keyboard.press('Escape')
      await (await openSheet(page, 'Squat')).getByRole('button', { name: 'Pain' }).click()
      await expect(skippedLine(page).getByRole('listitem')).toHaveCount(1)

      // The reason sheet, for Curl.
      await page.getByRole('button', { name: 'Options for Curl' }).click()
      await page.getByRole('menuitem', { name: 'Skip today' }).click()
      const sheet = page.getByRole('dialog', { name: 'Why skip Curl?' })
      await expect(sheet).toBeVisible()
      await sheet.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
      await shoot(page, `today-skip-sheet${suffix}`)
      await pastTheTapWindow(page)
      await sheet.getByRole('button', { name: 'Pain' }).click()
      await expect(skippedLine(page).getByRole('listitem')).toHaveCount(2)
      // The skipped line, once the snackbar has gone.
      await expect(page.getByText('Curl skipped.')).toBeHidden({ timeout: 10_000 })
      await skippedLine(page).scrollIntoViewIfNeeded()
      await shoot(page, `today-skipped${suffix}`)

      // Coach: Curl's card with its swaps first (the newer), then Squat's, from the top.
      await tabBar(page).getByRole('link', { name: 'Coach' }).click()
      await expect(page.getByRole('heading', { name: 'Curl skipped' })).toBeVisible()
      await expect(page.getByRole('heading', { name: 'Squat skipped' })).toBeVisible()
      await expect(page.locator('.gr-card').filter({ has: page.getByRole('heading', { name: 'Curl skipped' }) }).getByRole('button', { name: /^Swap for / }).first()).toBeVisible()
      // From the top: the header too (whatever scrolled while Today was open).
      await page.evaluate(() => {
        for (const el of document.querySelectorAll('*')) if (el.scrollTop > 0) el.scrollTop = 0
        window.scrollTo(0, 0)
      })
      await shoot(page, `coach-skip${suffix}`)

      // And the main lift's pain card, the hold notice with Dismiss only.
      const squatCard = page.locator('.gr-card').filter({ has: page.getByRole('heading', { name: 'Squat skipped' }) })
      await expect(squatCard.getByRole('button')).toHaveText(['Dismiss'])
      await squatCard.evaluate((el) => el.scrollIntoView({ block: 'center' }))
      await shoot(page, `coach-skip-pain${suffix}`)
    })
  }
}
