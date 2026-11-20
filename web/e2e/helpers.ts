import { expect, type Locator, type Page } from '@playwright/test'

/** The sample Wednesday the flows run on. */
export const WEDNESDAY = new Date('2026-11-11T09:41:00')

/** How long Plan's add form ignores taps on Add, Cancel and outside once it opens (ADD_FORM_TAP_WINDOW_MS in PlanDialogs). */
const ADD_FORM_TAP_WINDOW_MS = 300

/** Waits until Plan's add form, open now, takes taps: it ignores them at first, as the second tap of a double tap on a row. */
export const pastTheTapWindow = (page: Page) => page.waitForTimeout(ADD_FORM_TAP_WINDOW_MS)

/**
 * Puts the guide's seam back (e2e/storage.ts) after a test cleared the site's storage: the next load is a new user
 * again, and without it the first-launch gate would open the guide.
 */
export const keepTheGuideShut = (page: Page) => page.evaluate(() => localStorage.setItem('grit:guide', 'off'))

export const tabBar = (page: Page) => page.getByRole('navigation', { name: 'Main' })

export async function tap(page: Page, name: string, times = 1) {
  for (let i = 0; i < times; i++) await page.getByRole('button', { name, exact: true }).click()
}

/** The exercise card's typed fields, by their spoken names. */
export const weightField = (page: Page) => page.getByRole('textbox', { name: 'Weight, kilograms' })
export const repsField = (page: Page) => page.getByRole('textbox', { name: 'Reps', exact: true })
export const rpeField = (page: Page) => page.getByRole('textbox', { name: 'RPE', exact: true })

/** Taps a field on the exercise card and types over its value, as a thumb on the keypad does. Nothing is committed yet. */
export async function typeInto(page: Page, field: Locator, text: string) {
  await field.tap()
  await expect(field).toBeFocused()
  await page.keyboard.type(text)
  await expect(field).toHaveValue(text)
}

/**
 * Opens Grit on the sample Wednesday, with the database created and the first screen showing. With
 * E2E_TRAINING_MODE=BODYBUILDING in the environment the app is put on the bodybuilding side first, so a run can
 * check that the flows (the @speed checks above all) hold there too; unset, nothing changes.
 */
export async function openOnWednesday(page: Page, hash = '#/today') {
  await page.clock.setFixedTime(WEDNESDAY)
  await page.goto(`/${hash}`)
  await expect(tabBar(page)).toBeVisible()
  if (process.env.E2E_TRAINING_MODE === 'BODYBUILDING') {
    await page.evaluate(async () => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      await new Promise<void>((done, fail) => {
        const tx = db.transaction('settings', 'readwrite')
        tx.objectStore('settings').put({ key: 'trainingMode', value: 'BODYBUILDING' })
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      db.close()
    })
    await page.reload()
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
    await expect(tabBar(page)).toBeVisible()
  }
}

/**
 * Writes a bench day on Wednesdays and six flat weeks of bench (100 x 5 @ 8.5) straight into
 * IndexedDB, then reloads: finishing one more such session makes the coach call a stall.
 */
export async function seedFlatBench(page: Page) {
  await page.evaluate(async () => {
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
    const bench = exercises.find((e) => e.name === 'Bench')!.id
    // Stamped with their side, as the app writes them (schema version 3).
    tx.objectStore('planItems').add({ weekday: 3, exerciseId: bench, sets: 3, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })
    for (let week = 0; week < 6; week++) {
      const date = new Date(Date.UTC(2026, 9, 5 + week * 7)).toISOString().slice(0, 10)
      const session = tx.objectStore('sessions').add({ date, bodyweightKg: null, readiness: null, mode: 'POWERLIFTING' })
      session.onsuccess = () => tx.objectStore('sets').add({ sessionId: session.result, exerciseId: bench, weightKg: 100, reps: 5, rpe: 8.5, failed: false, missPoint: null, isTopSet: false })
    }
    await settled
    db.close()
  })
  await page.reload()
  await expect(page.getByText('Wednesday · 1 exercise')).toBeVisible()
}

/** Every row of every table, as the app stores it: what the "same data" checks compare. */
export function readAllTables(page: Page) {
  return page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const out: Record<string, unknown[]> = {}
    for (const name of ['exercises', 'sessions', 'sets', 'planItems', 'baselines', 'advice', 'settings']) {
      out[name] = await new Promise<unknown[]>((done, fail) => {
        const all = db.transaction(name).objectStore(name).getAll()
        all.onsuccess = () => done(all.result)
        all.onerror = () => fail(all.error)
      })
    }
    db.close()
    return out
  })
}
