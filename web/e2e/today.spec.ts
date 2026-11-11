import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'

// The Today screen with the same sample story as the Android mockup (docs/design/screens/today-dark.png):
// Wednesday 11 November, squat 5 × 5 @ 8 planned, last week's top set 150 × 5 @ 7.
// A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites the committed
// PNGs in docs/design/web/screens/. Look at them after a visual change.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

/** Opens the app on the sample Wednesday and writes the plan and last week's session straight into IndexedDB. */
async function openWithSampleData(page: Page) {
  await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
  await page.goto('/#/today')
  await expect(page.getByRole('heading', { name: 'Ready to train?' })).toBeVisible()
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets', 'baselines'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const exercises = await new Promise<{ id: number; name: string; category: string }[]>((done) => {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => done(all.result)
    })
    const idOf = (name: string) => exercises.find((e) => e.name === name)!.id
    const accessory = exercises.find((e) => e.category === 'ACCESSORY')!.id
    const squat = idOf('Squat')
    ;[squat, idOf('Bench'), idOf('Deadlift'), accessory].forEach((exerciseId, order) =>
      tx.objectStore('planItems').add({ weekday: 3, exerciseId, sets: order === 0 ? 5 : 3, reps: order === 3 ? 10 : 5, targetRpe: 8, order }),
    )
    tx.objectStore('baselines').put({ exerciseId: squat, e1rmKg: 190, date: '2026-10-12' })
    const session = tx.objectStore('sessions').add({ date: '2026-11-04', bodyweightKg: null, readiness: null })
    session.onsuccess = () =>
      tx.objectStore('sets').add({ sessionId: session.result, exerciseId: squat, weightKg: 150, reps: 5, rpe: 7, failed: false, missPoint: null, isTopSet: false })
    await settled
    db.close()
  })
  await page.reload()
  await expect(page.getByText('Wednesday · 4 exercises')).toBeVisible()
  await expect(page.getByText('Coach suggests 155 × 5 @ 8')).toBeVisible()
}

async function tap(page: Page, name: string, times = 1) {
  for (let i = 0; i < times; i++) await page.getByRole('button', { name, exact: true }).click()
}

/** Logs the mockup's three squat sets (150 × 5 @ 7.5, 155 × 5 @ 8.5, a missed 155 × 3 @ 10) and leaves 155 × 5 @ 8 in the editor. */
async function logMockupSets(page: Page) {
  await tap(page, 'Start session')
  await tap(page, 'Decrease RPE')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  await tap(page, 'Increase Weight', 2)
  await tap(page, 'Increase RPE', 2)
  await tap(page, 'Log set 2')
  await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
  await tap(page, 'Decrease Reps', 2)
  await tap(page, 'Increase RPE', 3)
  await tap(page, 'Missed')
  await expect(page.getByRole('button', { name: 'Log set 4' })).toBeVisible()
  await page.getByTestId('miss-row-3').getByText('Floor').click()
  await expect(page.getByTestId('miss-row-3').getByRole('radio', { name: 'Floor' })).toBeChecked()
  await tap(page, 'Increase Reps', 2)
  await tap(page, 'Decrease RPE', 4)
  await expect(page.getByLabel('Weight 155 kg, type Weight')).toBeVisible()
}

/** Buttons, links and choices smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('button, a[href], label.gr-segment, label.gr-chip'))
      .map((el) => ({ el, box: el.getBoundingClientRect() }))
      .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
      .map(({ el, box }) => `${el.className} ${Math.round(box.width)}×${Math.round(box.height)}`),
  )

const fitsThePhone = (page: Page) => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)

for (const scheme of ['dark', 'light'] as const) {
  test(`today matches the mockup story (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openWithSampleData(page)
    expect(await smallTargets(page)).toEqual([])
    await logMockupSets(page)

    await expect(page.getByText('Exercise 1 of 4')).toBeVisible()
    await expect(page.getByText('Set 4 of 5')).toBeVisible()
    await expect(page.getByText('25 + 25 + 15 + 2.5')).toBeVisible()
    expect(await smallTargets(page)).toEqual([])
    expect(await fitsThePhone(page)).toBe(true)

    // The shell scrolls inside the viewport: make the phone tall enough to show the whole card, from the top.
    await page.setViewportSize({ width: 390, height: 1040 })
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 0))
    await page.evaluate(() => document.fonts.ready)
    mkdirSync(outDir, { recursive: true })
    await page.screenshot({ path: resolve(outDir, `today-${scheme}.png`), fullPage: true, animations: 'disabled' })
  })
}

test.describe('on a 375 px iPhone', () => {
  test.use({ viewport: { width: 375, height: 812 } })

  test('the three steppers fit the exercise card with full-size buttons', async ({ page }) => {
    await openWithSampleData(page)
    await logMockupSets(page)
    // The widest value the weight stepper shows in practice.
    await page.getByLabel('Weight 155 kg, type Weight').click()
    await page.getByRole('dialog').getByRole('textbox').fill('182.5')
    await page.getByRole('dialog').getByRole('button', { name: 'Set' }).click()
    await expect(page.getByText('25 + 25 + 25 + 5 + 1.25')).toBeVisible()

    expect(await fitsThePhone(page)).toBe(true)
    expect(await smallTargets(page)).toEqual([])
    const layout = await page.evaluate(() => {
      const row = document.querySelector<HTMLElement>('.today-steppers')!
      const card = row.closest<HTMLElement>('.gr-card')!.getBoundingClientRect()
      const buttons = Array.from(row.querySelectorAll<HTMLElement>('.gr-step-btn')).map((b) => b.getBoundingClientRect())
      return {
        count: buttons.length,
        rowOverflows: row.scrollWidth > row.clientWidth,
        narrowest: Math.min(...buttons.map((b) => b.width)),
        insideCard: buttons.every((b) => b.left >= card.left && b.right <= card.right),
        overlapping: buttons.some((b, i) => i > 0 && b.left < buttons[i - 1]!.right),
        // Every plate is readable: the list wraps, it is never cut off.
        platesCutOff: Array.from(document.querySelectorAll<HTMLElement>('.gr-plates-label')).some((l) => l.scrollWidth > l.clientWidth),
      }
    })
    expect(layout).toEqual({ count: 6, rowOverflows: false, narrowest: expect.any(Number), insideCard: true, overlapping: false, platesCutOff: false })
    expect(layout.narrowest).toBeGreaterThanOrEqual(48)

    mkdirSync(resolve(process.cwd(), 'test-results/screens'), { recursive: true })
    await page.screenshot({ path: resolve(process.cwd(), 'test-results/screens/today-375.png'), fullPage: true, animations: 'disabled' })
  })
})

test('a logged set starts the rest timer, and it survives a reload', async ({ page }) => {
  const at = (seconds: number) => page.clock.setFixedTime(new Date(new Date('2026-11-11T09:41:00').getTime() + seconds * 1000))
  await openWithSampleData(page)
  await tap(page, 'Start session')
  await expect(page.getByRole('timer')).toHaveCount(0)
  await tap(page, 'Log set 1')
  await expect(page.getByRole('timer', { name: 'Rest, 3:00 left' })).toBeVisible()
  expect(await smallTargets(page)).toEqual([])
  expect(await fitsThePhone(page)).toBe(true)
  // Docked above the tab bar, inside the screen.
  const layout = await page.evaluate(() => {
    const bar = document.querySelector('.gr-rest')!.getBoundingClientRect()
    const tabs = document.querySelector('.gr-tabbar')!.getBoundingClientRect()
    return { aboveTabs: bar.bottom <= tabs.top, onScreen: bar.top >= 0 && tabs.bottom <= window.innerHeight }
  })
  expect(layout).toEqual({ aboveTabs: true, onScreen: true })
  mkdirSync(resolve(process.cwd(), 'test-results/screens'), { recursive: true })
  await page.screenshot({ path: resolve(process.cwd(), 'test-results/screens/today-rest.png'), animations: 'disabled' })

  // A message never covers the bar's buttons, however tall the bar is (larger text makes it taller).
  await page.evaluate(() => {
    IDBObjectStore.prototype.add = () => {
      throw new DOMException('full', 'QuotaExceededError')
    }
  })
  await tap(page, 'Log set 2')
  const message = page.getByText("Couldn't save — try again.")
  await expect(message).toBeVisible()
  const clearance = () =>
    page.evaluate(() => {
      const snackbar = document.querySelector('.gr-snackbar')!.getBoundingClientRect()
      const bar = document.querySelector('.gr-rest')!.getBoundingClientRect()
      return { barHeight: Math.round(bar.height), gap: Math.round(bar.top - snackbar.bottom) }
    })
  const normal = await clearance()
  expect(normal.gap).toBeGreaterThanOrEqual(4)
  await page.addStyleTag({ content: '.gr-rest-row { padding-block: 40px !important }' })
  await expect.poll(async () => (await clearance()).barHeight).toBeGreaterThan(normal.barHeight + 40)
  await expect.poll(async () => (await clearance()).gap).toBeGreaterThanOrEqual(4)

  // The time left follows the clock, and a reload mid-rest keeps the countdown.
  await at(60)
  await expect(page.getByRole('timer', { name: 'Rest, 2:00 left' })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('timer', { name: 'Rest, 2:00 left' })).toBeVisible()
  await tap(page, '+30 s')
  await expect(page.getByRole('timer', { name: 'Rest, 2:30 left' })).toBeVisible()

  // The bar belongs to Today; the rest runs on behind the other tabs.
  await page.getByRole('link', { name: 'Coach' }).click()
  await expect(page.getByRole('timer')).toHaveCount(0)
  await page.getByRole('link', { name: 'Today' }).click()
  await expect(page.getByRole('timer', { name: 'Rest, 2:30 left' })).toBeVisible()

  // Zero: the bar goes and the screen flashes.
  await at(210)
  await expect(page.getByTestId('rest-flash')).toBeAttached()
  await expect(page.getByRole('timer')).toHaveCount(0)
  await expect(page.getByTestId('rest-flash')).toHaveCount(0)

  // A missed set rests too, and Skip ends it.
  await tap(page, 'Missed')
  await expect(page.getByRole('timer', { name: 'Rest, 3:00 left' })).toBeVisible()
  await tap(page, 'Skip')
  await expect(page.getByRole('timer')).toHaveCount(0)
})
