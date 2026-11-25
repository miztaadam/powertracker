import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'

// Progress and lift history with a sample story like the Android mockups (docs/design/screens/progress-dark.png,
// lift-history-dark.png): twelve weeks of all three lifts, squat and deadlift rising, bench flat.
// A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites the committed
// PNGs in docs/design/web/screens/. Look at them after a visual change.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

/**
 * Opens Progress on the sample Wednesday and writes twelve weeks of sessions straight into IndexedDB.
 * With `strictPress`, the lift has its row and a session a week too, and its Progress setting is on.
 */
async function openWithSampleData(page: Page, strictPress = false) {
  await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
  await page.goto('/#/progress')
  await expect(page.getByText('No lifts logged yet')).toBeVisible()
  await page.evaluate(async (strictPress) => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    if (strictPress) {
      // A new install has no strict press row until the lift is first used.
      const first = db.transaction(['exercises', 'settings'], 'readwrite')
      first.objectStore('exercises').add({ name: 'Strict press', category: 'STRICT_PRESS', parentLift: null, muscles: ['Front delts', 'Side delts', 'Triceps'], isBuiltIn: true, archived: false })
      first.objectStore('settings').put({ key: 'showStrictPress', value: true })
      await new Promise<void>((done, fail) => {
        first.oncomplete = () => done()
        first.onerror = () => fail(first.error)
      })
    }
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
      if (strictPress) {
        session(week * 7 + 2, [
          ['Strict press', 62.5 + 2.5 * Math.floor(week / 2), 3, 8.5, 1, true],
          ['Strict press', 52.5, 5, 7, 3],
        ])
      }
    }
    await settled
    db.close()
  }, strictPress)
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

    // 1.19: History, at the top right of Progress, lists every workout, newest first, twenty at a time.
    await page.setViewportSize({ width: 375, height: 812 })
    const history = page.getByRole('button', { name: 'History', exact: true })
    const [button, title] = await Promise.all([history.boundingBox(), page.getByRole('heading', { level: 1, name: 'Progress' }).boundingBox()])
    // At the right of the title, on its line.
    expect(button!.x).toBeGreaterThan(title!.x + title!.width - 1)
    expect(button!.y).toBeLessThan(title!.y + title!.height)
    expect(button!.y + button!.height).toBeGreaterThan(title!.y)
    await history.click()
    await expect(page).toHaveURL(/#\/history$/)
    await expect(page.getByRole('heading', { level: 1, name: 'History' })).toBeVisible()
    await expect(page.getByText('35 workouts')).toBeVisible()
    const workouts = page.locator('.workout')
    await expect(workouts).toHaveCount(20)
    await expect(workouts.first().getByRole('heading', { level: 2 })).toHaveText('Tue 10 Nov 2026')
    await expect(workouts.first().getByText('Powerlifting · 1 exercise · 2 sets · 93.3 kg bodyweight')).toBeVisible()
    await expect(workouts.first().getByRole('heading', { level: 3 })).toHaveText('Bench')
    await expect(workouts.first().getByText('Set 1: 107.5 kg × 3 at RPE 9', { exact: true })).toBeAttached()
    await expect(workouts.nth(1).getByRole('heading', { level: 3 })).toHaveText(['Squat', 'Pause squat'])
    await expect(workouts.nth(1).getByRole('listitem')).toHaveCount(8)
    expect(await overflowing(page)).toEqual([])
    expect(await clippedText(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await shoot(page, `history-${scheme}`, 1500)

    await page.setViewportSize({ width: 375, height: 812 })
    const older = page.getByRole('button', { name: 'Show older workouts' })
    await older.click()
    await expect(workouts).toHaveCount(35)
    await expect(older).toHaveCount(0)
    await expect(workouts.last().getByRole('heading', { level: 2 })).toHaveText('Mon 24 Aug 2026')
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])

    await page.getByRole('button', { name: 'Back' }).click()
    await expect(page).toHaveURL(/#\/progress$/)
    await expect(page.getByText('Weekly best e1RM')).toBeVisible()
  })
}

for (const scheme of ['dark', 'light'] as const) {
  test(`progress with strict press fits the narrowest iPhone (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openWithSampleData(page, true)

    await page.setViewportSize({ width: 375, height: 812 })
    await expect(page.getByText('Total (S+B+D+P)')).toBeVisible()
    await expect(page.getByText('in 4 weeks')).toBeVisible()
    await expect(page.getByRole('img', { name: /Line chart. Latest: Squat 190.5 kg, Bench .* kg, Deadlift .* kg, Strict press .* kg/ })).toBeVisible()
    await expect(page.getByText(/^Strict press record: /)).toBeAttached()
    await expect(page.getByText(/^Strict press volume this week: /)).toBeAttached()
    expect(await overflowing(page)).toEqual([])
    expect(await clippedText(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])

    // Four lift tiles, two to a row, none over another.
    // The lifts' tiles ("… squat history"), not the header's History button (1.19).
    const tiles = await page.getByRole('button', { name: / history$/i }).evaluateAll((els) => els.map((el) => el.getBoundingClientRect().toJSON() as DOMRect))
    expect(tiles).toHaveLength(4)
    for (const [i, a] of tiles.entries()) {
      for (const b of tiles.slice(i + 1)) expect(a.right <= b.left + 0.5 || b.right <= a.left + 0.5 || a.bottom <= b.top + 0.5 || b.bottom <= a.top + 0.5).toBe(true)
    }
    expect(tiles.map((t) => Math.round(t.top))).toEqual([tiles[0]!.top, tiles[0]!.top, tiles[2]!.top, tiles[2]!.top].map(Math.round))
    expect(tiles[2]!.top).toBeGreaterThan(tiles[0]!.bottom)

    // The four legend entries stay whole, with their figures.
    const legend = await page.locator('.gr-legend').first().locator('.gr-legend-item').evaluateAll((els) => els.map((el) => ({ text: el.textContent, right: el.getBoundingClientRect().right })))
    expect(legend.map((l) => l.text?.replace(/[\d. kg]+$/, ''))).toEqual(['Squat', 'Bench', 'Deadlift', 'Strict press'])

    // "Strict press · this week" stays on one line inside its column, and the captions under the bars neither wrap nor meet.
    const names = await page.locator('.progress-volume-name').evaluateAll((els) =>
      els.map((el) => {
        const text = document.createRange()
        text.selectNodeContents(el)
        return { text: el.textContent, height: el.getBoundingClientRect().height, right: text.getBoundingClientRect().right, column: el.parentElement!.getBoundingClientRect().right }
      }),
    )
    expect(names.map((n) => n.text)).toEqual(['Squat · this week', 'Bench · this week', 'Deadlift · this week', 'Strict press · this week'])
    for (const n of names) {
      expect(n.height, n.text ?? '').toBeLessThan(20)
      expect(n.right, n.text ?? '').toBeLessThanOrEqual(n.column + 0.5)
    }
    const [from, to] = await page.locator('.progress-volume-span > span').evaluateAll((els) => els.map((el) => el.getBoundingClientRect().toJSON() as DOMRect))
    expect(from!.right).toBeLessThan(to!.left)
    expect(from!.height).toBeLessThan(20)
    expect(to!.height).toBeLessThan(20)
    await shoot(page, `progress-strict-press-${scheme}`, 2020)

    // The fourth tile opens the lift's own history.
    await page.setViewportSize({ width: 375, height: 812 })
    await page.getByRole('button', { name: /strict press history/i }).click()
    await expect(page).toHaveURL(/#\/lift\/STRICT_PRESS$/)
    await expect(page.getByRole('heading', { level: 1, name: 'Strict press history' })).toBeVisible()
    await expect(page.getByText('12 sessions')).toBeVisible()
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
  })
}

test('the strict press switch in Settings changes Progress and its total', async ({ page }) => {
  await openWithSampleData(page, true)
  await expect(page.getByText('Total (S+B+D+P)')).toBeVisible()
  const withPress = await page.locator('.progress-total-figure .gr-number-value').first().textContent()

  await page.goto('/#/settings')
  await page.getByText('Show it on Progress and add it to your total.').click()
  await expect(page.getByRole('switch', { name: 'Strict press' })).not.toBeChecked()
  await page.goto('/#/progress')

  // Off: the screen is the three-lift one, as if strict press had never been logged.
  await expect(page.getByText('Total (S+B+D)', { exact: true })).toBeVisible()
  await expect(page.getByText(/strict press/i)).toHaveCount(0)
  await expect(page.getByRole('button', { name: / history$/i })).toHaveCount(3)
  await expect.poll(() => page.locator('.progress-total-figure .gr-number-value').first().textContent()).not.toBe(withPress)
})

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

test('back from History does not leave it behind in history', async ({ page }) => {
  await openWithSampleData(page)
  await page.getByRole('link', { name: 'Today' }).click()
  await page.getByRole('link', { name: 'Progress' }).click()
  await page.getByRole('button', { name: 'History', exact: true }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'History' })).toBeVisible()

  await page.getByRole('button', { name: 'Back' }).click()
  await expect(page).toHaveURL(/#\/progress$/)
  // The swipe back in the installed app: on from Progress to where the lifter was before, not to History again.
  await page.goBack()
  await expect(page).toHaveURL(/#\/today$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Today' })).toBeVisible()
})

test('the phone\'s Back on History returns to Progress', async ({ page }) => {
  await openWithSampleData(page)
  await page.getByRole('button', { name: 'History', exact: true }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'History' })).toBeVisible()
  await page.goBack()
  await expect(page).toHaveURL(/#\/progress$/)
  await expect(page.getByText('Weekly best e1RM')).toBeVisible()
})

test('History opened directly: Back goes to Progress and replaces it', async ({ page }) => {
  await openWithSampleData(page)
  await page.goto('about:blank')
  await page.goto('/#/history')
  await expect(page.getByRole('heading', { level: 1, name: 'History' })).toBeVisible()
  await expect(page.getByText('35 workouts')).toBeVisible()
  const entries = await page.evaluate(() => window.history.length)

  await page.getByRole('button', { name: 'Back' }).click()
  await expect(page).toHaveURL(/#\/progress$/)
  await expect(page.getByText('Weekly best e1RM')).toBeVisible()
  expect(await page.evaluate(() => window.history.length)).toBe(entries)
  await page.goBack()
  await expect(page).not.toHaveURL(/#\/history/)
})

// 1.19: beside the History button the header's caption and title have less room. At 246 px (a 320 px phone at 130%
// zoom) the button is still beside the title, and nothing in the header is cut, on either side.
for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
  test(`the Progress header with its History button is whole at 246 px, ${side.toLowerCase()} side`, async ({ page }) => {
    await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
    await page.goto('/#/progress')
    await expect(page.getByText('No lifts logged yet')).toBeVisible()
    if (side === 'BODYBUILDING') {
      await page.evaluate(async () => {
        const db = await new Promise<IDBDatabase>((done, fail) => {
          const request = indexedDB.open('grit')
          request.onsuccess = () => done(request.result)
          request.onerror = () => fail(request.error)
        })
        const tx = db.transaction(['settings'], 'readwrite')
        tx.objectStore('settings').put({ key: 'trainingMode', value: 'BODYBUILDING' })
        await new Promise<void>((done, fail) => {
          tx.oncomplete = () => done()
          tx.onerror = () => fail(tx.error)
        })
        db.close()
      })
      await page.reload()
      await expect(page.getByText('No exercises in your plan yet')).toBeVisible()
    }
    await page.setViewportSize({ width: 246, height: 624 })
    const history = page.getByRole('button', { name: 'History', exact: true })
    await expect(history).toBeVisible()
    const cut = await page.evaluate(() =>
      [...document.querySelectorAll<HTMLElement>('.gr-screen-header, .gr-screen-header *')]
        .filter((el) => !el.closest('.sr-only') && getComputedStyle(el).display !== 'inline' && el.scrollWidth > el.clientWidth + 1)
        .map((el) => `${el.className}: ${el.textContent}`),
    )
    expect(cut).toEqual([])
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    // Still beside the title, not under it.
    const [button, title] = await Promise.all([history.boundingBox(), page.getByRole('heading', { level: 1, name: 'Progress' }).boundingBox()])
    expect(button!.x).toBeGreaterThan(title!.x + title!.width - 1)
  })
}
