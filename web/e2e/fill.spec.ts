import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, tabBar } from './helpers'

// Stage 4, charts that fill in (charts.css, RatioBar.css): with motion on, Progress's lines draw in from the left in
// 600 ms, the volume bars grow from the baseline 30 ms apart (the strip done by 700 ms) and the ratio markers slide in
// in 450 ms. CSS only, so the checks below seek the browser's own animations instead of sleeping, which keeps them
// steady on a busy machine. Every other spec runs still; the last test here checks that still means no animation.

/** Weeks of sessions, `weeks` back from the sample Wednesday's week, written straight into IndexedDB. */
async function seed(page: Page, weeks: number) {
  await page.evaluate(async (weeks) => {
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
    for (let week = 0; week < weeks; week++) {
      // Mondays, the last one two days before the sample Wednesday (11 November 2026).
      const date = new Date(Date.UTC(2026, 10, 9 - 7 * (weeks - 1 - week))).toISOString().slice(0, 10)
      const session = tx.objectStore('sessions').add({ date, bodyweightKg: 92 + week / 2, readiness: null, mode: 'POWERLIFTING' })
      session.onsuccess = () => {
        for (const [name, kg] of [['Squat', 140 + 5 * week], ['Bench', 100 + 2.5 * week], ['Deadlift', 180 + 5 * week]] as const) {
          tx.objectStore('sets').add({ sessionId: session.result, exerciseId: idOf(name), weightKg: kg, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: true })
        }
      }
    }
    await settled
    db.close()
  }, weeks)
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

const trend = (page: Page) => page.locator('.gr-card', { hasText: 'Weekly best e1RM' }).locator('.gr-linechart')

/** Opens Progress from the tab bar, as the lifter does, and waits for its first chart. */
async function openProgress(page: Page) {
  await tabBar(page).getByRole('link', { name: 'Progress' }).click()
  await expect(trend(page)).toBeAttached()
}

/** The fill-in animations on Progress's charts (not the page's other motion, such as the tab indicator). */
const chartAnimations = () =>
  document.getAnimations().filter((a) => {
    const target = (a.effect as KeyframeEffect | null)?.target
    return target instanceof Element && target.closest('.gr-fill-in') != null
  })

/** Whether every chart is drawn complete and still: no animation left, no clip on a line, every bar and marker in place. */
const complete = (page: Page) =>
  page.evaluate(() => {
    const charts = document.querySelectorAll('.gr-fill-in')
    if (charts.length === 0) return 'no charts'
    const running = document.getAnimations().filter((a) => {
      const target = (a.effect as KeyframeEffect | null)?.target
      return target instanceof Element && target.closest('.gr-fill-in') != null
    })
    if (running.length > 0) return `${running.length} running`
    for (const svg of document.querySelectorAll('.gr-fill-in svg, .gr-spark.gr-fill-in')) if (getComputedStyle(svg).clipPath !== 'none') return 'clipped line'
    for (const el of document.querySelectorAll('.gr-fill-in .gr-bar, .gr-fill-in .gr-ratio-marker')) if (getComputedStyle(el).transform !== 'none') return `moved ${el.className}`
    for (const el of document.querySelectorAll('.gr-fill-in .gr-plot-marker, .gr-fill-in .gr-spark-fill')) if (getComputedStyle(el).opacity !== '1') return `faded ${el.className}`
    return 'complete'
  })

test.describe('with motion on', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test.beforeEach(async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'no-preference' })
    await openOnWednesday(page)
  })

  test('the charts fill in when Progress opens: lines draw, bars grow, markers slide', async ({ page }) => {
    await seed(page, 6)
    // Tap Progress and pause every fill-in animation the moment the charts are in the page, so a busy machine cannot
    // let one finish (and leave the list) before it is looked at.
    await page.evaluate(
      () =>
        new Promise<void>((done) => {
          const pause = () => {
            if (document.querySelectorAll('.gr-fill-in').length < 8) return false
            document.getAnimations().forEach((a) => {
              const target = (a.effect as KeyframeEffect | null)?.target
              if (target instanceof Element && target.closest('.gr-fill-in') != null) a.pause()
            })
            return true
          }
          const watch = new MutationObserver(() => {
            if (pause()) {
              watch.disconnect()
              done()
            }
          })
          watch.observe(document.body, { childList: true, subtree: true })
          document.querySelector<HTMLAnchorElement>('nav[aria-label="Main"] a[href="#/progress"]')!.click()
        }),
    )
    // The words are all there from the first frame: a screen reader never hears a half-drawn chart.
    await expect(trend(page).getByRole('img', { name: /^Line chart\. Latest: Squat \d/ })).toBeAttached()
    await expect(page.getByText('Squat volume this week: 825 kg')).toBeAttached()

    const seen = await page.evaluate(() => {
      const all = document.getAnimations().filter((a) => {
        const target = (a.effect as KeyframeEffect | null)?.target
        return target instanceof Element && target.closest('.gr-fill-in') != null
      })
      all.forEach((a) => a.pause())
      const of = (selector: string) => all.filter((a) => ((a.effect as KeyframeEffect).target as Element).matches(selector))
      const at = (list: Animation[], ms: number) => list.forEach((a) => (a.currentTime = ms))
      const ends = (list: Animation[]) => list.map((a) => a.effect!.getComputedTiming().endTime as number)
      const lines = of('.gr-fill-in svg, .gr-spark.gr-fill-in')
      const bars = of('.gr-fill-in .gr-bar')
      const markers = of('.gr-fill-in .gr-ratio-marker')
      const svg = document.querySelector('.gr-card .gr-linechart svg')!
      const bar = document.querySelector<HTMLElement>('.gr-fill-in .gr-bar:not(.gr-bar-stub)')!
      const marker = document.querySelector<HTMLElement>('.gr-fill-in .gr-ratio-marker')!
      const lineAnimation = lines.find((a) => (a.effect as KeyframeEffect).target === svg)!
      const spark = document.querySelector<HTMLElement>('.gr-spark.gr-fill-in')!
      const sparkFill = spark.querySelector('.gr-spark-fill')!
      const scale = () => new DOMMatrix(getComputedStyle(bar).transform).d
      const shift = () => new DOMMatrix(getComputedStyle(marker).transform).e

      at(all, 300)
      // The total sparkline is wiped whole: its shaded area is revealed with the line, never shown ahead of it.
      const sparkMid = { clip: getComputedStyle(spark).clipPath, fillOpacity: getComputedStyle(sparkFill).opacity, animated: sparkFill.getAnimations().length }
      const mid = { clip: getComputedStyle(svg).clipPath, line: lineAnimation.effect!.getComputedTiming().progress as number, bar: scale(), marker: shift() }
      at(all, 0)
      // The marker starts at the start of the bar: its left edge on the track's.
      const startGap = marker.getBoundingClientRect().left - marker.parentElement!.getBoundingClientRect().left
      at(all, 750)
      const done = { clip: getComputedStyle(svg).clipPath, bar: getComputedStyle(bar).transform, marker: getComputedStyle(marker).transform }
      all.forEach((a) => a.finish())
      return { sparkMid, counts: [lines.length, bars.length, markers.length], lineEnds: ends(lines), barEnds: ends(bars), markerEnds: ends(markers), mid, startGap, done }
    })
    // Two lines (trend, bodyweight) and the total sparkline; three strips of eight bars; two ratio markers.
    expect(seen.counts).toEqual([3, 24, 2])
    expect(Math.max(...seen.lineEnds)).toBe(600)
    expect(Math.max(...seen.barEnds)).toBeLessThanOrEqual(700)
    expect(Math.min(...seen.barEnds)).toBe(450)
    expect(Math.max(...seen.markerEnds)).toBe(450)
    // Mid-way the line is partly drawn, the bars are partly grown and the marker is on its way.
    expect(seen.mid.clip).not.toBe('none')
    expect(seen.sparkMid.clip).not.toBe('none')
    expect(seen.sparkMid).toMatchObject({ fillOpacity: '1', animated: 0 })
    expect(seen.mid.line).toBeGreaterThan(0)
    expect(seen.mid.line).toBeLessThan(1)
    expect(seen.mid.bar).toBeGreaterThan(0)
    expect(seen.mid.bar).toBeLessThan(1)
    expect(seen.mid.marker).toBeLessThan(0)
    expect(seen.startGap).toBeLessThan(1)
    // By 750 ms every one is done and left as the still chart.
    expect(seen.done).toEqual({ clip: 'none', bar: 'none', marker: 'none' })
    await expect.poll(() => complete(page)).toBe('complete')
  })

  test('on its own clock too, the charts end complete', async ({ page }) => {
    await seed(page, 6)
    await openProgress(page)
    await expect.poll(() => complete(page), { timeout: 3000 }).toBe('complete')
  })

  test('new data while Progress is open does not draw the charts again', async ({ page }) => {
    await seed(page, 6)
    await openProgress(page)
    await expect.poll(() => complete(page)).toBe('complete')
    const before = await trend(page).elementHandle()
    // A set logged elsewhere (another tab, a restored backup) reaches the open screen as another tab's write does:
    // straight into IndexedDB, then Dexie's cross-tab message, which re-runs every live query.
    await page.evaluate(async () => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'sessions', 'sets'], 'readwrite')
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => {
        const squat = (all.result as { id: number; name: string }[]).find((e) => e.name === 'Squat')!.id
        const session = tx.objectStore('sessions').add({ date: '2026-11-10', bodyweightKg: 95, readiness: null, mode: 'POWERLIFTING' })
        session.onsuccess = () => tx.objectStore('sets').add({ sessionId: session.result, exerciseId: squat, weightKg: 100, reps: 5, rpe: 7, failed: false, missPoint: null, isTopSet: false })
      }
      await new Promise<void>((done) => (tx.oncomplete = () => done()))
      db.close()
      const channel = new BroadcastChannel('x-storagemutated-1')
      channel.postMessage({ all: true })
      channel.close()
    })
    await expect(page.getByText('Squat volume this week: 1,325 kg')).toBeAttached()
    expect(await page.evaluate(chartAnimations)).toHaveLength(0)
    expect(await trend(page).evaluate((el, old) => el === old, before)).toBe(true)
  })

  test('changing range twice quickly ends with the last range drawn complete', async ({ page }) => {
    await seed(page, 6)
    await openProgress(page)
    await expect.poll(() => complete(page)).toBe('complete')
    const before = await trend(page).elementHandle()
    await page.getByText('8 wk').click()
    await page.getByText('All', { exact: true }).click()
    await expect(page.getByRole('radio', { name: 'All' })).toBeChecked()
    // A new line, drawn in again.
    expect(await trend(page).evaluate((el, old) => el !== old, before)).toBe(true)
    await expect.poll(() => complete(page)).toBe('complete')
    await expect(trend(page).locator('polyline')).toHaveCount(3)
  })

  test('the bodyweight line draws again on a range change, and ends complete', async ({ page }) => {
    await seed(page, 6)
    await openProgress(page)
    await expect.poll(() => complete(page)).toBe('complete')
    // The bodyweight chart is the last line chart on Progress.
    const bodyweight = page.locator('.gr-linechart').last()
    const before = await bodyweight.elementHandle()
    // Choose 8 wk and catch the new bodyweight line as it mounts, pausing its drawing to look at it.
    const drawing = await page.evaluate(
      () =>
        new Promise<{ animations: number; clip: string }>((done) => {
          const last = () => Array.from(document.querySelectorAll('.gr-linechart svg')).at(-1)
          const old = last()
          const watch = new MutationObserver(() => {
            const svg = last()
            if (svg == null || svg === old) return
            watch.disconnect()
            const animations = svg.getAnimations()
            animations.forEach((a) => {
              a.pause()
              a.currentTime = 300
            })
            const seen = { animations: animations.length, clip: getComputedStyle(svg).clipPath }
            animations.forEach((a) => a.finish())
            done(seen)
          })
          watch.observe(document.body, { childList: true, subtree: true })
          Array.from(document.querySelectorAll<HTMLElement>('.gr-segment')).find((r) => r.textContent === '8 wk')!.click()
        }),
    )
    expect(drawing.animations).toBe(1)
    expect(drawing.clip).not.toBe('none')
    await expect(page.getByRole('radio', { name: '8 wk' })).toBeChecked()
    expect(await bodyweight.evaluate((el, old) => el !== old, before)).toBe(true)
    await expect.poll(() => complete(page)).toBe('complete')
  })

  test('leaving Progress mid-fill and coming back starts cleanly', async ({ page }) => {
    await seed(page, 6)
    await openProgress(page)
    await tabBar(page).getByRole('link', { name: 'Today' }).click()
    await openProgress(page)
    await expect.poll(() => complete(page)).toBe('complete')
  })

  test('one point, or no line at all, still renders', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', (e) => errors.push(e.message))
    await seed(page, 1)
    await openProgress(page)
    // One session: each lift and the bodyweight are a single point, with no total trend to draw.
    await expect(trend(page).locator('polyline')).toHaveCount(3)
    await expect(trend(page).locator('.gr-plot-marker')).toHaveCount(3)
    await expect.poll(() => complete(page)).toBe('complete')
    // A range with no sessions has no line: the note says so.
    await page.clock.setFixedTime(new Date('2027-06-02T09:41:00'))
    await page.reload()
    await expect(page.getByText('No sessions in this range. Try a longer one.')).toBeVisible()
    await expect.poll(() => complete(page)).toBe('complete')
    expect(errors).toEqual([])
  })
})

test('with motion off, nothing animates on Progress', async ({ page }) => {
  await openOnWednesday(page)
  await seed(page, 6)
  const animations = () => page.evaluate(() => document.getAnimations().map((a) => `${a.constructor.name} ${((a.effect as KeyframeEffect).target as Element)?.className}`))
  // Opened straight from its address: not one animation on the page.
  await page.goto('/#/progress')
  await page.reload()
  await expect(trend(page)).toBeAttached()
  await expect(page.locator('html')).toHaveAttribute('data-motion', 'off')
  expect(await animations()).toEqual([])
  expect(await complete(page)).toBe('complete')
  // Opened from the tab bar: only the tab bar's own 150 ms colour change (as in 1.8), none on the charts.
  await tabBar(page).getByRole('link', { name: 'Today' }).click()
  await openProgress(page)
  expect((await animations()).filter((a) => !a.endsWith('gr-tab-indicator'))).toEqual([])
  await expect.poll(animations).toEqual([])
  expect(await complete(page)).toBe('complete')
})
