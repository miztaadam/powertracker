import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'

// Progress and lift history with a sample story like the Android mockups (docs/design/screens/progress-dark.png,
// lift-history-dark.png): twelve weeks of all three lifts, squat and deadlift rising, bench flat.
// A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites the committed
// PNGs in docs/design/web/screens/. Look at them after a visual change.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

/** Opens Progress on the sample Wednesday and writes twelve weeks of sessions straight into IndexedDB. */
async function openWithSampleData(page: Page) {
  await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
  await page.goto('/#/progress')
  await expect(page.getByText('No lifts logged yet')).toBeVisible()
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'sessions', 'sets'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => done(all.result)
    })
    const idOf = (name: string) => exercises.find((e) => e.name === name)!.id
    type Sets = [name: string, kg: number, reps: number, rpe: number, count: number, top?: boolean][]
    let logged = 0
    /** One session `day` days after Monday 24 August 2026, unless that is after the sample Wednesday. */
    const session = (day: number, sets: Sets) => {
      const date = new Date(Date.UTC(2026, 7, 24 + day)).toISOString().slice(0, 10)
      if (date > '2026-11-11') return
      const bodyweightKg = 91.6 + Math.round(day / 5) / 10 + [0.3, -0.2, 0.1, -0.4][logged++ % 4]!
      const added = tx.objectStore('sessions').add({ date, bodyweightKg: Math.round(bodyweightKg * 10) / 10, readiness: null })
      added.onsuccess = () => {
        for (const [name, weightKg, reps, rpe, count, top = false] of sets) {
          for (let i = 0; i < count; i++) {
            tx.objectStore('sets').add({ sessionId: added.result, exerciseId: idOf(name), weightKg, reps, rpe, failed: false, missPoint: null, isTopSet: top })
          }
        }
      }
    }
    for (let week = 0; week < 12; week++) {
      const squat = 142.5 + 2.5 * week
      session(week * 7, [
        ['Squat', squat, 3, 9, 1, true],
        ['Squat', squat - 30, 5, 6, 4],
        ['Pause squat', squat - 27.5, 3, 7.5, 3],
      ])
      session(week * 7 + 1, [
        ['Bench', 107.5, 3, week % 3 === 0 ? 8.5 : 9, 1, true],
        ['Bench', 90, 5, 7, week === 11 ? 1 : 4],
      ])
      session(week * 7 + 4, [
        ['Deadlift', 185 + 2.5 * week, 3, 7.5, 1, true],
        ['Deadlift', 160, 5, 6, 2 + (week % 2)],
      ])
    }
    await settled
    db.close()
  })
  await page.reload()
  await expect(page.getByText('Weekly best e1RM')).toBeVisible()
}

/** Buttons and links smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('button, a[href]'))
      .map((el) => ({ el, box: el.getBoundingClientRect() }))
      .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
      .map(({ el, box }) => `${el.className} ${Math.round(box.width)}×${Math.round(box.height)}`),
  )

/** Things wider than the phone: the page, the scrolling region, or any element reaching past the right edge. */
const overflowing = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    const main = document.querySelector('.shell-main')!
    if (main.scrollWidth > main.clientWidth) found.push('.shell-main')
    for (const el of main.querySelectorAll<HTMLElement>('*')) {
      const box = el.getBoundingClientRect()
      if (box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5) && !el.closest('.sr-only')) found.push(`${el.tagName.toLowerCase()}.${el.className}`)
    }
    return found
  })

/** Text cut off inside its box (an ellipsis or a clipped figure). */
const clippedText = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('.gr-stat *, .gr-number-row, .progress-volume-name, .gr-screen-title'))
      .filter((el) => el.scrollWidth > el.clientWidth + 1 && getComputedStyle(el).overflow !== 'visible')
      .map((el) => `${el.className}: ${el.textContent}`),
  )

async function shoot(page: Page, name: string, height: number) {
  // The shell scrolls inside the viewport: make the phone tall enough to show the whole screen, from the top.
  await page.setViewportSize({ width: 390, height })
  await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 0))
  await page.evaluate(() => document.fonts.ready)
  mkdirSync(outDir, { recursive: true })
  await page.screenshot({ path: resolve(outDir, `${name}.png`), fullPage: true, animations: 'disabled' })
}

for (const scheme of ['dark', 'light'] as const) {
  test(`progress and lift history match the mockup story (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openWithSampleData(page)

    // The narrowest iPhone.
    await page.setViewportSize({ width: 375, height: 812 })
    await expect(page.getByRole('heading', { level: 1, name: 'Progress' })).toBeVisible()
    await expect(page.getByText('in 4 weeks')).toBeVisible()
    for (const section of ['Records', 'Weekly volume', 'Lift ratios', 'Bodyweight']) await expect(page.getByRole('heading', { name: section })).toBeVisible()
    await expect(page.getByRole('img', { name: /Line chart. Latest: Squat 190.5 kg, Bench .* kg, Deadlift .* kg/ })).toBeVisible()
    expect(await overflowing(page)).toEqual([])
    expect(await clippedText(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await shoot(page, `progress-${scheme}`, 1840)

    await page.setViewportSize({ width: 375, height: 812 })
    await page.getByRole('button', { name: /squat history/i }).click()
    await expect(page).toHaveURL(/#\/lift\/SQUAT$/)
    await expect(page.getByRole('heading', { level: 1, name: 'Squat history' })).toBeVisible()
    await expect(page.getByText('12 sessions')).toBeVisible()
    await expect(page.getByRole('heading', { level: 2 }).first()).toHaveText('Mon 9 Nov 2026')
    await expect(page.getByText('Set 1: 170 kg × 3 at RPE 9, top set, personal record, done')).toBeAttached()
    expect(await overflowing(page)).toEqual([])
    expect(await clippedText(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await shoot(page, `lift-history-${scheme}`, 1500)

    await page.getByRole('button', { name: 'Back' }).click()
    await expect(page).toHaveURL(/#\/progress$/)
    await expect(page.getByText('Weekly best e1RM')).toBeVisible()
  })
}

test('the chart range is chosen with one tap', async ({ page }) => {
  await openWithSampleData(page)
  await expect(page.getByRole('radio', { name: '6 mo' })).toBeChecked()
  const trend = page.getByRole('img', { name: /Line chart. Latest: Squat/ })
  await expect(trend.getByText('24 Aug')).toBeVisible()

  await page.getByText('8 wk').click()
  await expect(page.getByRole('radio', { name: '8 wk' })).toBeChecked()
  await expect(trend.getByText('21 Sep')).toBeVisible()
  await expect(trend.getByText('24 Aug')).toHaveCount(0)
})

test('back from a lift does not leave the lift page behind in history', async ({ page }) => {
  await openWithSampleData(page)
  await page.getByRole('link', { name: 'Today' }).click()
  await page.getByRole('link', { name: 'Progress' }).click()
  await page.getByRole('button', { name: /squat history/i }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Squat history' })).toBeVisible()

  await page.getByRole('button', { name: 'Back' }).click()
  await expect(page).toHaveURL(/#\/progress$/)
  // The swipe back in the installed app: on from Progress to where the lifter was before, not to the lift again.
  await page.goBack()
  await expect(page).toHaveURL(/#\/today$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Today' })).toBeVisible()
})

test('back from a lift page opened directly goes to Progress and replaces it', async ({ page }) => {
  await openWithSampleData(page)
  await page.goto('about:blank')
  await page.goto('/#/lift/BENCH')
  await expect(page.getByRole('heading', { level: 1, name: 'Bench history' })).toBeVisible()
  const entries = await page.evaluate(() => window.history.length)

  await page.getByRole('button', { name: 'Back' }).click()
  await expect(page).toHaveURL(/#\/progress$/)
  await expect(page.getByText('Weekly best e1RM')).toBeVisible()
  expect(await page.evaluate(() => window.history.length)).toBe(entries)
  await page.goBack()
  await expect(page).not.toHaveURL(/#\/lift/)
})
