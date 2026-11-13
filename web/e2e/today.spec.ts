import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { repsField, rpeField, typeInto, weightField } from './helpers'

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
  await expect(weightField(page)).toHaveValue('155')
}

/** Buttons, links, choices and typed figures smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('button, a[href], label.gr-segment, label.gr-chip, label.gr-stepper-figure'))
      .map((el) => ({ el, box: el.getBoundingClientRect() }))
      .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
      .map(({ el, box }) => `${el.className} ${Math.round(box.width)}×${Math.round(box.height)}`),
  )

/** The three typed figures on the exercise card: big enough that iOS does not zoom, readable in full, and clear of each other. */
const typedFields = (page: Page) =>
  page.evaluate(() => {
    const inputs = Array.from(document.querySelectorAll<HTMLInputElement>('.today-steppers input'))
    return {
      count: inputs.length,
      smallestFontPx: Math.min(...inputs.map((i) => parseFloat(getComputedStyle(i).fontSize))),
      // The value is readable in full: the field is as wide as its figures.
      cutOff: inputs.some((i) => i.scrollWidth > i.clientWidth),
      // A wide figure may reach into the gap beside its column, never into its neighbour or out of the card.
      colliding: (() => {
        const card = inputs[0]!.closest<HTMLElement>('.gr-card')!.getBoundingClientRect()
        const rows = inputs.map((i) => i.closest<HTMLElement>('.gr-numfield-row')!.getBoundingClientRect())
        return rows.some((r, n) => r.left < card.left || r.right > card.right || (n > 0 && r.left < rows[n - 1]!.right))
      })(),
      keypads: inputs.map((i) => i.inputMode),
    }
  })

/** Today's logged sets as [kg, reps, RPE], read from IndexedDB. */
const loggedSets = (page: Page) =>
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
    const sets = (await read<{ sessionId: number; weightKg: number; reps: number; rpe: number }>('sets')).filter((s) => s.sessionId === today?.id)
    db.close()
    return sets.map((s) => [s.weightKg, s.reps, s.rpe])
  })

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

// The bodybuilding side's Today, in its own story (the mockup's column C), is in bodybuilding.spec.ts.

test.describe('on a 375 px iPhone', () => {
  test.use({ viewport: { width: 375, height: 812 } })

  test('the three steppers fit the exercise card with full-size buttons', async ({ page }) => {
    await openWithSampleData(page)
    await logMockupSets(page)
    // The widest value the weight stepper shows in practice.
    await typeInto(page, weightField(page), '182.5')
    // The plates follow the typing, before the field is left.
    await expect(page.getByText('25 + 25 + 25 + 5 + 1.25')).toBeVisible()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await page.keyboard.press('Enter')
    await expect(weightField(page)).not.toBeFocused()
    await expect(weightField(page)).toHaveValue('182.5')

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
    expect(await typedFields(page)).toEqual({ count: 3, smallestFontPx: 44, cutOff: false, colliding: false, keypads: ['decimal', 'numeric', 'decimal'] })

    mkdirSync(resolve(process.cwd(), 'test-results/screens'), { recursive: true })
    await page.screenshot({ path: resolve(process.cwd(), 'test-results/screens/today-375.png'), fullPage: true, animations: 'disabled' })

    // A refused entry and its message, and far too many figures: the row and the buttons stay as they were.
    await typeInto(page, weightField(page), '1234567.25')
    await typeInto(page, repsField(page), '0')
    await typeInto(page, rpeField(page), '11')
    await page.getByRole('heading', { level: 1, name: 'Today' }).click()
    await expect(page.getByText('0 to 1000 kg')).toBeVisible()
    await expect(page.getByText('Whole, 1 to 99')).toBeVisible()
    await expect(page.getByText('6 to 10', { exact: true })).toBeVisible()
    expect(await fitsThePhone(page)).toBe(true)
    expect(await smallTargets(page)).toEqual([])
    const refused = await page.evaluate(() => {
      const row = document.querySelector<HTMLElement>('.today-steppers')!
      const buttons = Array.from(row.querySelectorAll<HTMLElement>('.gr-step-btn')).map((b) => b.getBoundingClientRect())
      return { rowOverflows: row.scrollWidth > row.clientWidth, narrowest: Math.min(...buttons.map((b) => b.width)), buttonTops: new Set(buttons.map((b) => Math.round(b.top))).size }
    })
    expect(refused).toEqual({ rowOverflows: false, narrowest: expect.any(Number), buttonTops: 1 })
    expect(refused.narrowest).toBeGreaterThanOrEqual(48)
    expect((await typedFields(page)).colliding).toBe(false)
    await page.screenshot({ path: resolve(process.cwd(), 'test-results/screens/today-375-refused.png'), fullPage: true, animations: 'disabled' })
  })

  test('typing on the card: tap selects, the keypad replaces, and Log set takes what is typed', async ({ page }) => {
    await openWithSampleData(page)
    await tap(page, 'Start session')
    await expect(weightField(page)).toHaveValue('150')

    // A tap selects the value, so the first key replaces it; no dialog opens.
    await typeInto(page, weightField(page), '140')
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await expect(page.getByText('25 + 25 + 10', { exact: true })).toBeVisible()
    // Straight from the keypad to Log set.
    await tap(page, 'Log set 1')
    await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Set 1: 140 kg × 5 at RPE 8, done' })).toBeVisible()
    await expect.poll(() => loggedSets(page)).toEqual([[140, 5, 8]])

    // Typed, then +: the step starts from the typed number.
    await typeInto(page, weightField(page), '102,5')
    await tap(page, 'Increase Weight')
    await expect(weightField(page)).toHaveValue('105')
    await typeInto(page, rpeField(page), '8.3')
    await page.keyboard.press('Enter')
    await expect(rpeField(page)).toHaveValue('8.5')
    await typeInto(page, repsField(page), '3')
    await tap(page, 'Log set 2')
    await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
    await expect.poll(() => loggedSets(page)).toEqual([[140, 5, 8], [105, 3, 8.5]])

    // A number the card refuses is not logged at the old weight: the message shows and the cursor goes back to it.
    await typeInto(page, weightField(page), '1200')
    await tap(page, 'Log set 3')
    await expect(page.getByText('0 to 1000 kg')).toBeVisible()
    await expect(weightField(page)).toBeFocused()
    await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
    await expect.poll(() => loggedSets(page)).toEqual([[140, 5, 8], [105, 3, 8.5]])
    await page.keyboard.type('120')
    await tap(page, 'Log set 3')
    await expect.poll(() => loggedSets(page)).toEqual([[140, 5, 8], [105, 3, 8.5], [120, 3, 8.5]])
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
