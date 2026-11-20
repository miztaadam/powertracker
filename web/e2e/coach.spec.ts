import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'

// The Coach screen with the same sample story as the Android mockup (docs/design/screens/coach-dark.png):
// a deload, a bench stall with three accessories, and a squat that lags the deadlift.
// A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites the committed
// PNGs in docs/design/web/screens/. Look at them after a visual change.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

/** Opens the Coach tab on the sample Wednesday and writes the advice and the history behind it straight into IndexedDB. */
async function openWithSampleData(page: Page) {
  await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
  await page.goto('/#/coach')
  await expect(page.getByText('No advice right now. Keep logging.')).toBeVisible()
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets', 'baselines', 'advice'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => done(all.result)
    })
    const idOf = (name: string) => exercises.find((e) => e.name === name)!.id
    ;[idOf('Squat'), idOf('Bench')].forEach((exerciseId, order) => tx.objectStore('planItems').add({ weekday: 3, exerciseId, sets: 5, reps: 5, targetRpe: 8, order }))
    tx.objectStore('baselines').put({ exerciseId: idOf('Squat'), e1rmKg: 190, date: '2026-10-12' })
    tx.objectStore('baselines').put({ exerciseId: idOf('Deadlift'), e1rmKg: 250, date: '2026-10-12' })
    // Six flat weeks of bench: 100 × 5, give or take a rep of effort.
    const bench = idOf('Bench')
    ;[8, 8.5, 8.5, 8, 8.5, 8.5].forEach((rpe, week) => {
      const date = new Date(Date.UTC(2026, 9, 5 + week * 7)).toISOString().slice(0, 10)
      const session = tx.objectStore('sessions').add({ date, bodyweightKg: null, readiness: null })
      session.onsuccess = () => tx.objectStore('sets').add({ sessionId: session.result, exerciseId: bench, weightKg: 100, reps: 5, rpe, failed: false, missPoint: null, isTopSet: false })
    })
    const payload = (kind: string, fields: object = {}) => JSON.stringify({ type: `com.powertracker.domain.model.AdvicePayload.${kind}`, ...fields })
    const advice = (type: string, lift: string | null, severity: number, reason: string, payloadJson: string) =>
      tx.objectStore('advice').add({ type, lift, createdAt: '2026-11-11', reason, severity, status: 'OPEN', payloadJson })
    advice('DELOAD', null, 2, 'Signs of fatigue: RPE rising at the same load on squat and deadlift; 3 failed reps. Take a deload week: about 60% of the volume at about 90% of the load.', payload('Deload'))
    advice(
      'STALL',
      'BENCH',
      3,
      "Bench e1RM hasn't improved for 3 weeks. You've mostly missed at lockout — this works the triceps.",
      payload('Accessories', {
        items: [
          { exerciseName: 'Close-grip bench', sets: 3, repsLow: 3, repsHigh: 5 },
          { exerciseName: 'JM press', sets: 3, repsLow: 8, repsHigh: 10 },
          { exerciseName: 'Board press', sets: 3, repsLow: 3, repsHigh: 5 },
        ],
      }),
    )
    advice('WEAK_POINT', 'SQUAT', 4, 'Your squat is low relative to your deadlift (76% vs a typical 80–90%). Prioritise squat.', payload('WeakPoint', { lift: 'SQUAT', missPoint: null }))
    await settled
    db.close()
  })
  await page.reload()
  await expect(page.getByText('3 open suggestions')).toBeVisible()
}

/** How many plan items are stored. */
const planSize = (page: Page) =>
  page.evaluate(
    () =>
      new Promise<number>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onerror = () => fail(request.error)
        request.onsuccess = () => {
          const count = request.result.transaction('planItems').objectStore('planItems').count()
          count.onsuccess = () => {
            request.result.close()
            done(count.result)
          }
        }
      }),
  )

/** The stored advice's statuses, sorted. */
const adviceStatuses = (page: Page) =>
  page.evaluate(
    () =>
      new Promise<string[]>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onerror = () => fail(request.error)
        request.onsuccess = () => {
          const all = request.result.transaction('advice').objectStore('advice').getAll()
          all.onsuccess = () => {
            request.result.close()
            done((all.result as { status: string }[]).map((a) => a.status).sort())
          }
        }
      }),
  )

/** Buttons and links smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('button, a[href]'))
      .map((el) => ({ el, box: el.getBoundingClientRect() }))
      .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
      .map(({ el, box }) => `${el.className} ${Math.round(box.width)}×${Math.round(box.height)}`),
  )

const fitsThePhone = (page: Page) => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)

for (const scheme of ['dark', 'light'] as const) {
  test(`coach matches the mockup story (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openWithSampleData(page)

    await expect(page.getByRole('heading', { name: 'Take a deload week' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Bench has stalled' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Squat is lagging your deadlift' })).toBeVisible()
    await expect(page.getByText('Below range')).toBeVisible()
    await expect(page.getByText('Pause squat')).toBeVisible()
    expect(await smallTargets(page)).toEqual([])
    expect(await fitsThePhone(page)).toBe(true)

    // The shell scrolls inside the viewport: make the phone tall enough to show the whole feed, from the top.
    await page.setViewportSize({ width: 390, height: 1420 })
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 0))
    await page.evaluate(() => document.fonts.ready)
    mkdirSync(outDir, { recursive: true })
    await page.screenshot({ path: resolve(outDir, `coach-${scheme}.png`), fullPage: true, animations: 'disabled' })
  })
}

test('a suggestion changes nothing until it is applied', async ({ page }) => {
  await openWithSampleData(page)
  await expect.poll(() => planSize(page)).toBe(2)

  await page.getByRole('button', { name: 'Add to plan' }).click()
  await expect(page.getByText('Added 3 exercises to Wednesday')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Bench has stalled' })).toHaveCount(0)
  await expect(page.getByText('2 open suggestions')).toBeVisible()
  await expect.poll(() => planSize(page)).toBe(5)

  await page.getByRole('button', { name: 'Dismiss' }).first().click()
  await expect(page.getByText('1 open suggestion')).toBeVisible()
  await expect.poll(() => planSize(page)).toBe(5)

  // Applied and dismissed advice stays gone after a reload. The card leaves at the tap, before its status is stored:
  // reload once both are stored, not while the dismiss is still being written (a busy machine was seen to reload first).
  await expect.poll(async () => (await adviceStatuses(page)).filter((s) => s !== 'OPEN')).toEqual(['APPLIED', 'DISMISSED'])
  await page.reload()
  await expect(page.getByText('1 open suggestion')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Squat is lagging your deadlift' })).toBeVisible()
})

test('the finish summary opens the coach', async ({ page }) => {
  await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
  await page.goto('/#/today')
  await page.getByRole('button', { name: 'Start session' }).click()
  await page.getByRole('button', { name: 'Finish', exact: true }).click()
  await page.getByRole('button', { name: /coach/i }).click()
  await expect(page).toHaveURL(/#\/coach$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Coach' })).toBeVisible()
})
