import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, tabBar, tap } from './helpers'
import { wrongHere } from './narrowChecks'

// 1.24, supersets: two exercises linked on the Plan tab are done back to back on Today. The story: Wednesday's squat,
// bench and row, each done last week; the bench is linked to the squat above it.
// A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites the committed PNGs.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

/** Wednesday: squat 3 × 5, bench 3 × 5, row 3 × 8, each with a set last week; `linked` puts the bench in a superset with the squat. */
async function seed(page: Page, linked: boolean) {
  await page.evaluate(async (linked) => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => done(all.result)
    })
    const idOf = (name: string) => exercises.find((e) => e.name === name)!.id
    const rows: [string, number, number][] = [
      ['Squat', 5, 150],
      ['Bench', 5, 100],
      ['Row', 8, 60],
    ]
    rows.forEach(([name, reps], order) =>
      tx.objectStore('planItems').add({ weekday: 3, exerciseId: idOf(name), sets: 3, reps, targetRpe: 8, order, mode: 'POWERLIFTING', ...(linked && name === 'Bench' ? { supersetWithAbove: true } : {}) }),
    )
    const session = tx.objectStore('sessions').add({ date: '2026-11-04', bodyweightKg: null, readiness: null, mode: 'POWERLIFTING' })
    session.onsuccess = () =>
      rows.forEach(([name, reps, kg]) => tx.objectStore('sets').add({ sessionId: session.result, exerciseId: idOf(name), weightKg: kg, reps, rpe: 8, failed: false, missPoint: null, isTopSet: false }))
    await settled
    db.close()
  }, linked)
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

/** The open card on Today: its exercise and its eyebrow. */
const openCard = (page: Page) => page.locator('.gr-card', { has: page.getByRole('button', { name: /^Log set \d+$/ }) })
const sets = (page: Page) =>
  page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const read = <T,>(store: string) =>
      new Promise<T[]>((done, fail) => {
        const all = db.transaction(store).objectStore(store).getAll()
        all.onsuccess = () => done(all.result as T[])
        all.onerror = () => fail(all.error)
      })
    const today = (await read<{ id: number; date: string }>('sessions')).find((s) => s.date === '2026-11-11')
    const exercises = await read<{ id: number; name: string }>('exercises')
    const logged = (await read<{ sessionId: number; exerciseId: number }>('sets')).filter((s) => s.sessionId === today?.id)
    db.close()
    return logged.map((s) => exercises.find((e) => e.id === s.exerciseId)!.name)
  })

test('a superset is linked on the Plan tab, shown as a pair, and unlinked again', async ({ page }) => {
  await openOnWednesday(page, '#/plan')
  await seed(page, false)
  const wednesday = page.getByRole('region', { name: 'Wednesday' })
  const rows = wednesday.getByRole('listitem')
  await expect(rows).toHaveCount(3)
  // The first row has none above it; the second can be linked to it.
  await rows.nth(0).getByRole('button', { name: 'More' }).click()
  await expect(page.getByRole('menuitem')).toHaveText(['Edit', 'Delete'])
  await page.keyboard.press('Escape')
  await rows.nth(1).getByRole('button', { name: 'More' }).click()
  await page.getByRole('menuitem', { name: 'Superset with the one above' }).click()

  await expect(rows.nth(0)).toHaveAccessibleName('Squat  3 × 5 @ 8, superset with Bench')
  await expect(rows.nth(1)).toHaveAccessibleName('Bench  3 × 5 @ 8, superset with Squat')
  await expect(rows.nth(0).locator('.plan-row-superset')).toHaveText('Superset A')
  await expect(rows.nth(1).locator('.plan-row-superset')).toHaveText('Superset B')
  await expect(rows.nth(2).locator('.plan-row-superset')).toHaveCount(0)
  // The link is kept: the same after a reload.
  await page.reload()
  await expect(rows.nth(1).locator('.plan-row-superset')).toHaveText('Superset B')
  expect(await wrongHere(page)).toEqual([])

  // Moving the row up into the pair's place breaks it: the row is now above the bench.
  await rows.nth(2).getByRole('button', { name: 'Move up' }).click()
  await expect(rows.nth(1)).toHaveAccessibleName('Row  3 × 8 @ 8')
  await expect(wednesday.locator('.plan-row-superset')).toHaveCount(0)
})

for (const scheme of ['dark', 'light'] as const) {
  test(`a superset on Today hands over from one card to the other, and rests after the second (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page, '#/plan')
    await seed(page, true)
    await page.evaluate(() => document.fonts.ready)
    mkdirSync(outDir, { recursive: true })
    await page.screenshot({ path: resolve(outDir, `plan-superset-${scheme}.png`), animations: 'disabled' })

    await page.goto('/#/today')
    await tap(page, 'Start session')
    await expect(openCard(page).getByRole('heading', { level: 2 })).toHaveText('Squat')
    await expect(openCard(page).locator('.eyebrow').first()).toHaveText('Exercise 1 of 3 · Superset A')
    await expect(page.getByRole('button', { name: /^Bench/ }).getByText('Superset B')).toBeVisible()
    expect(await wrongHere(page)).toEqual([])

    // The squat's set: the bench's card opens, and no rest starts.
    await tap(page, 'Log set 1')
    await expect(openCard(page).getByRole('heading', { level: 2 })).toHaveText('Bench')
    await expect(openCard(page).locator('.eyebrow').first()).toHaveText('Exercise 2 of 3 · Superset B')
    await expect(page.getByRole('timer')).toHaveCount(0)
    expect(await sets(page)).toEqual(['Squat'])
    expect(await wrongHere(page)).toEqual([])
    await page.evaluate(() => document.fonts.ready)
    await page.setViewportSize({ width: 390, height: 1040 })
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 0))
    await page.screenshot({ path: resolve(outDir, `today-superset-${scheme}.png`), animations: 'disabled' })
    await page.setViewportSize({ width: 390, height: 664 })

    // A second tap in the same place, straight after the hand-over, is not the bench's set.
    await page.getByRole('button', { name: 'Log set 1' }).click()
    await page.waitForTimeout(150)
    expect(await sets(page)).toEqual(['Squat'])
    // Past that moment, the bench's set: the rest starts, and the squat's card is back for round two.
    await page.waitForTimeout(1000)
    await tap(page, 'Log set 1')
    await expect(page.getByRole('timer')).toBeVisible()
    await expect(openCard(page).getByRole('heading', { level: 2 })).toHaveText('Squat')
    await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
    expect(await sets(page)).toEqual(['Squat', 'Bench'])

    // Closed and opened mid-round: with the squat a set ahead, the bench's card is the one that opens.
    await page.waitForTimeout(1100)
    await tap(page, 'Log set 2')
    await expect(openCard(page).getByRole('heading', { level: 2 })).toHaveText('Bench')
    await page.reload()
    await expect(openCard(page).getByRole('heading', { level: 2 })).toHaveText('Bench')
    await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  })
}

test('a superset makes its open card no taller: the tag takes the count\'s place, on the line the card already has', async ({ page }) => {
  // The same day, without the link and then with it: the open squat card is the same height at an iPhone 13's width,
  // so a card that fitted the screen with its Log button still does. (The tag after the count was 52 px taller: the
  // heading line no longer fitted beside the pill and the buttons, and they went to a line of their own.)
  const height = async (eyebrow: string) => {
    await expect(openCard(page).getByRole('heading', { level: 2 })).toHaveText('Squat')
    await expect(openCard(page).locator('.eyebrow').first()).toHaveText(eyebrow)
    await expect(openCard(page).getByRole('region', { name: 'Warm-up' })).toBeVisible()
    return Math.round((await openCard(page).boundingBox())!.height)
  }
  await openOnWednesday(page)
  await seed(page, false)
  await tap(page, 'Start session')
  const plain = await height('Exercise 1 of 3')

  await page.goto('/#/plan')
  await page.getByRole('region', { name: 'Wednesday' }).getByRole('listitem').nth(1).getByRole('button', { name: 'More' }).click()
  await page.getByRole('menuitem', { name: 'Superset with the one above' }).click()
  await expect(page.getByRole('region', { name: 'Wednesday' }).locator('.plan-row-superset')).toHaveText(['Superset A', 'Superset B'])
  await page.goto('/#/today')
  const paired = await height('Exercise 1 of 3 · Superset A')
  // On screen the tag alone: the count is there for a screen reader only.
  expect(await openCard(page).locator('.eyebrow').first().evaluate((p) => [...p.childNodes].filter((n) => n.nodeType === Node.TEXT_NODE).map((n) => n.textContent).join(''))).toBe('Superset A')
  expect(paired, `the paired card ${paired} px, the plain one ${plain} px`).toBe(plain)
})

test.describe('supersets at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  test('the Plan pair and the Today tags fit', async ({ page }) => {
    await openOnWednesday(page, '#/plan')
    await seed(page, true)
    await expect(page.getByRole('region', { name: 'Wednesday' }).locator('.plan-row-superset')).toHaveText(['Superset A', 'Superset B'])
    expect(await wrongHere(page)).toEqual([])
    await page.goto('/#/today')
    await tap(page, 'Start session')
    await expect(openCard(page).locator('.eyebrow').first()).toHaveText('Exercise 1 of 3 · Superset A')
    expect(await wrongHere(page)).toEqual([])
    await tap(page, 'Log set 1')
    await expect(openCard(page).getByRole('heading', { level: 2 })).toHaveText('Bench')
    expect(await wrongHere(page)).toEqual([])
  })
})
