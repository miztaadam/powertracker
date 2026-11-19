import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { WEDNESDAY, tabBar } from './helpers'

// The mode switch animations (1.15): the hammer drop (Bodybuilding mode off) and the pump (on). Every other e2e page
// runs with motion off (e2e/motion.spec.ts), so these turn it on (spec "Settled" 8). The timing checks hold the
// page's clock (Playwright's) and step it frame by frame; the speed check at the end runs on real frames.

const grit = GritDomain.getInstance()
const DROP = grit.modeSwitchDropTimeline()
const PUMP = grit.modeSwitchPumpTimeline()
const THIRD = PUMP.beats[2]!

const outDir = resolve(process.cwd(), 'test-results/screens')

const modeSwitch = (page: Page) => page.getByRole('switch', { name: 'Bodybuilding mode' })
const dataMode = (page: Page) => page.evaluate(() => document.documentElement.getAttribute('data-mode'))
const drawn = (page: Page) => page.locator('.ms-overlay > svg').count()

/** The side the settings hold, read straight from the database. */
const storedMode = (page: Page) =>
  page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const row = await new Promise<{ value: string } | undefined>((done, fail) => {
      const get = db.transaction('settings').objectStore('settings').get('trainingMode')
      get.onsuccess = () => done(get.result as { value: string } | undefined)
      get.onerror = () => fail(get.error)
    })
    db.close()
    return row?.value ?? 'POWERLIFTING'
  })

/** Settings on the sample Wednesday, motion on, the page's clock under the test's hand (not yet held). */
async function openSettings(page: Page) {
  await page.emulateMedia({ reducedMotion: 'no-preference' })
  await page.clock.install({ time: WEDNESDAY })
  await page.goto('/#/settings')
  await expect(modeSwitch(page)).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.getAttribute('data-motion'))).toBeNull()
}

/** Holds the page's clock: from here on time moves only with `page.clock.runFor`. */
async function hold(page: Page) {
  const now = await page.evaluate(() => Date.now())
  await page.clock.pauseAt(now + 1000)
}

/** Flips the switch and waits for the setting to be stored, which happens at the tap, whatever the clock does. */
async function flip(page: Page, to: 'BODYBUILDING' | 'POWERLIFTING') {
  // A press where the switch is, as a finger does: during a reveal the browser gives it to <html> (the page is a
  // picture then), and it must still flip the switch.
  const box = (await modeSwitch(page).boundingBox())!
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
  await expect.poll(() => storedMode(page)).toBe(to)
  // The switch shows it once the database has told the screen, on a timer the held clock runs in the steps that follow.
}

/** The fake clock's frames come every 16 ms. */
const FRAME = 16

/**
 * Flips at a moment chosen so that one of the animation's frames falls exactly on `frameAt` ms from the tap: the clock
 * is stepped to a frame, then on by the remainder, before the press.
 */
async function flipLanding(page: Page, to: 'BODYBUILDING' | 'POWERLIFTING', frameAt: number) {
  await page.evaluate(() => {
    const w = window as Window & { __frame?: number }
    w.__frame = 0
    const tick = () => {
      w.__frame!++
      requestAnimationFrame(tick)
    }
    requestAnimationFrame(tick)
  })
  const frame = () => page.evaluate(() => (window as Window & { __frame?: number }).__frame!)
  const start = await frame()
  for (let i = 0; i < FRAME + 1 && (await frame()) === start; i++) await page.clock.runFor(1)
  await page.clock.runFor((FRAME - (frameAt % FRAME)) % FRAME)
  await flip(page, to)
}

async function shoot(page: Page, name: string) {
  mkdirSync(outDir, { recursive: true })
  await page.evaluate(() => document.fonts.ready)
  // Animations allowed: the reveal is a held animation, and "disabled" would run it to its end.
  await page.screenshot({ path: resolve(outDir, `${name}.png`), animations: 'allow' })
}

test.describe('with motion on', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  for (const scheme of ['dark', 'light'] as const) {
    test(`the pump: stored at the tap, blue at the third beat (${scheme})`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
      await openSettings(page)
      await hold(page)
      // The held frame: the third beat + 120 ms. Frames then fall every 16 ms back from it: …, 742, 758, …, 870.
      const shot = THIRD + 120
      await flipLanding(page, 'BODYBUILDING', shot)
      // The shown side lags: still grey, the veins already running from the switch.
      expect(await dataMode(page)).toBeNull()
      await page.clock.runFor(THIRD - 5)
      expect(await drawn(page)).toBe(1)
      expect(await dataMode(page)).toBeNull()
      await expect(page.getByRole('switch', { name: 'Strict press' })).toBeVisible()
      await page.clock.runFor(FRAME)
      // The new side goes on inside the reveal, which the browser starts on its next frame.
      await expect.poll(() => dataMode(page)).toBe('bodybuilding')
      await page.clock.runFor(shot - THIRD - FRAME + 5)
      await shoot(page, `mode-switch-pump-beat3-${scheme}`)
      // The content is the new side's too.
      await expect(page.getByRole('switch', { name: 'Strict press' })).toHaveCount(0)
      await expect(page.getByTestId('mode-label')).toBeVisible()
      await page.clock.runFor(PUMP.end)
      expect(await drawn(page)).toBe(0)
      expect(await dataMode(page)).toBe('bodybuilding')
    })

    test(`the drop: stored at the tap, powerlifting at the impact (${scheme})`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
      await openSettings(page)
      await hold(page)
      await flip(page, 'BODYBUILDING')
      await page.clock.runFor(PUMP.end + 100)
      expect(await dataMode(page)).toBe('bodybuilding')

      // The held frame: the impact + 20 ms. Frames fall on …, 238, 254, 270.
      const shot = DROP.impact + 20
      await flipLanding(page, 'POWERLIFTING', shot)
      expect(await dataMode(page)).toBe('bodybuilding')
      await page.clock.runFor(DROP.impact - 10)
      expect(await dataMode(page)).toBe('bodybuilding')
      await page.clock.runFor(FRAME)
      await expect.poll(() => dataMode(page)).toBeNull()
      await page.clock.runFor(shot - DROP.impact - FRAME + 10)
      await shoot(page, `mode-switch-drop-impact-${scheme}`)
      // The POWERLIFTING stamp is there at the impact, and gone by the end.
      await expect(page.locator('.ms-stamp')).toHaveText('POWERLIFTING')
      // From the impact to the end, the stamp sits just below the title and never covers it. Measured in the header's
      // own layout (offsets), since the shake moves, turns and scales the header and the stamp alike.
      const stampAgainstTitle = () =>
        page.evaluate(() => {
          const stamp = document.querySelector<HTMLElement>('.ms-stamp')
          const title = stamp?.parentElement?.querySelector<HTMLElement>('.gr-screen-title')
          if (stamp == null || title == null) return null
          return { stampTop: stamp.offsetTop, titleBottom: title.offsetTop + title.offsetHeight, inHeader: stamp.parentElement!.classList.contains('gr-screen-header') }
        })
      for (let i = 0; i < 6; i++) {
        if (i > 0) await page.clock.runFor(FRAME * 9)
        const at = await stampAgainstTitle()
        expect(at, `the stamp, ${i} steps after the impact`).not.toBeNull()
        expect(at!.inHeader).toBe(true)
        expect(at!.stampTop).toBeGreaterThanOrEqual(at!.titleBottom)
        if (i === 2) {
          // Once the shake has died down (before the lift), the stamp is on screen.
          await shoot(page, `mode-switch-drop-stamp-${scheme}`)
          const stamp = (await page.locator('.ms-stamp').boundingBox())!
          expect(stamp.y).toBeGreaterThanOrEqual(0)
        }
      }
      await page.clock.runFor(DROP.end)
      expect(await drawn(page)).toBe(0)
      await expect(page.locator('.ms-stamp')).toHaveCount(0)
      expect(await page.locator('.shell').evaluate((el) => (el as HTMLElement).style.transform)).toBe('')
    })
  }

  test('the overlay and the stamp are hidden from a screen reader, and the switch says its new state', async ({ page }) => {
    await openSettings(page)
    await hold(page)
    // Onto bodybuilding first, with no animation to wait for; then the drop, which has the stamp.
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(PUMP.end + 100)
    await flip(page, 'POWERLIFTING')
    await expect(page.locator('.ms-overlay')).toHaveAttribute('aria-hidden', 'true')
    await page.clock.runFor(DROP.impact + 40)
    const stamp = page.locator('.ms-stamp')
    await expect(stamp).toHaveText('POWERLIFTING')
    await expect(stamp).toHaveAttribute('aria-hidden', 'true')
    const tree = await page.locator('body').ariaSnapshot()
    expect(tree).not.toContain('POWERLIFTING')
    expect(await page.locator('.ms-overlay').ariaSnapshot()).toBe('')
    // The switch says its new state as soon as the setting is read back, as before, long before the end.
    await expect(modeSwitch(page)).toHaveAttribute('aria-checked', 'false')
  })

  test('a tap skips to the end, and does nothing else', async ({ page }) => {
    await openSettings(page)
    await hold(page)
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(200)
    const plates = page.getByRole('switch', { name: 'Change plates' })
    await expect(plates).toHaveAttribute('aria-checked', 'false')
    await page.getByText('Change plates').click()
    expect(await drawn(page)).toBe(0)
    expect(await dataMode(page)).toBe('bodybuilding')
    // The tap only skipped (the database's answer runs on the held clock, so it is given time).
    await page.clock.runFor(200)
    await expect(plates).toHaveAttribute('aria-checked', 'false')
    await page.getByText('Change plates').click()
    await expect
      .poll(async () => {
        await page.clock.runFor(50)
        return plates.getAttribute('aria-checked')
      })
      .toBe('true')
  })

  test('three flips in a second: one animation, and the side of the last flip', async ({ page }) => {
    await openSettings(page)
    await hold(page)
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(300)
    await flip(page, 'POWERLIFTING')
    await page.clock.runFor(FRAME)
    expect(await drawn(page)).toBe(1)
    await page.clock.runFor(300)
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(FRAME)
    expect(await drawn(page)).toBe(1)
    await page.clock.runFor(PUMP.end + 100)
    expect(await drawn(page)).toBe(0)
    expect(await storedMode(page)).toBe('BODYBUILDING')
    expect(await dataMode(page)).toBe('bodybuilding')
  })

  test('leaving Settings mid-animation ends it at its end state', async ({ page }) => {
    await openSettings(page)
    await hold(page)
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(150)
    await page.evaluate(() => (window.location.hash = '#/plan'))
    await expect(tabBar(page)).toBeVisible()
    expect(await drawn(page)).toBe(0)
    expect(await dataMode(page)).toBe('bodybuilding')
    expect(await page.locator('.shell').evaluate((el) => (el as HTMLElement).style.transform)).toBe('')
    expect(await page.evaluate(() => document.documentElement.className)).not.toMatch(/ms-/)
  })

  test('the page going to the background ends it', async ({ page }) => {
    await openSettings(page)
    await hold(page)
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(150)
    await page.evaluate(() => {
      Object.defineProperty(document, 'visibilityState', { value: 'hidden', configurable: true })
      document.dispatchEvent(new Event('visibilitychange'))
    })
    expect(await drawn(page)).toBe(0)
    expect(await dataMode(page)).toBe('bodybuilding')
  })

  test('with no sound files, no Web Audio and no vibration, the animation still plays and nothing is thrown', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', (e) => errors.push(e.message))
    await page.route('**/sounds/*', (route) => route.abort())
    await page.addInitScript(() => {
      Object.defineProperty(navigator, 'vibrate', { value: undefined, configurable: true })
      // No Web Audio at all.
      Object.defineProperty(window, 'AudioContext', { value: undefined, configurable: true })
      Object.defineProperty(window, 'webkitAudioContext', { value: undefined, configurable: true })
    })
    await openSettings(page)
    await hold(page)
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(FRAME)
    expect(await drawn(page)).toBe(1)
    await page.clock.runFor(THIRD + 40)
    await expect.poll(() => dataMode(page)).toBe('bodybuilding')
    await page.clock.runFor(PUMP.end)
    expect(errors).toEqual([])
  })

  test('without view transitions, the new side grows in a circle from the switch, and a skip clears it', async ({ page }) => {
    await page.addInitScript(() => {
      delete (Document.prototype as { startViewTransition?: unknown }).startViewTransition
    })
    await openSettings(page)
    expect(await page.evaluate(() => 'startViewTransition' in document)).toBe(false)
    await hold(page)
    const box = (await page.locator('.settings-mode-switch .gr-switch-track').boundingBox())!
    const oldBackground = await page.evaluate(() => getComputedStyle(document.body).backgroundColor)
    await flipLanding(page, 'BODYBUILDING', THIRD + 120)
    await page.clock.runFor(THIRD + 120)
    await expect.poll(() => dataMode(page)).toBe('bodybuilding')
    const clipPath = () => page.locator('.shell').evaluate((el) => getComputedStyle(el).clipPath)
    const clip = await clipPath()
    expect(clip).toMatch(/^circle\(/)
    // Round the switch, where the flood starts; outside it, the old side's colour.
    const [, r, x, y] = /circle\(([\d.]+)px at ([\d.]+)px ([\d.]+)px\)/.exec(clip)!.map(Number)
    expect(x).toBeCloseTo(box.x + box.width / 2, 0)
    expect(y).toBeCloseTo(box.y + box.height / 2, 0)
    expect(r).toBeGreaterThan(30)
    expect(await page.evaluate(() => getComputedStyle(document.body).backgroundColor)).toBe(oldBackground)
    await shoot(page, 'mode-switch-pump-plain-reveal')
    // A tap skips, wherever it lands (here outside the circle, where the page shows only the old colour): the circle
    // and the old colour go at once.
    const viewport = page.viewportSize()!
    await page.mouse.click(12, viewport.height - 40)
    expect(await drawn(page)).toBe(0)
    expect(await clipPath()).toBe('none')
    expect(await page.evaluate(() => document.documentElement.className)).not.toMatch(/ms-/)
    expect(await page.evaluate(() => getComputedStyle(document.body).backgroundColor)).not.toBe(oldBackground)
  })

  test('320 px at 130%, Settings scrolled: the veins and the flood start from the switch', async ({ page }) => {
    await page.setViewportSize({ width: Math.floor(320 / 1.3), height: Math.floor(640 / 1.3) })
    await openSettings(page)
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 30))
    await expect.poll(() => page.locator('.shell-main').evaluate((el) => el.scrollTop)).toBeGreaterThan(0)
    await hold(page)
    const box = (await page.locator('.settings-mode-switch .gr-switch-track').boundingBox())!
    const centre = { x: box.x + box.width / 2, y: box.y + box.height / 2 }
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(FRAME)
    // The halos and rings are centred on the switch…
    const halo = page.locator('.ms-overlay circle').first()
    expect(Number(await halo.getAttribute('cx'))).toBeCloseTo(centre.x, 0)
    expect(Number(await halo.getAttribute('cy'))).toBeCloseTo(centre.y, 0)
    // …the veins start there…
    const vein = await page.locator('.ms-overlay .ms-vein').first().getAttribute('d')
    const [x, y] = vein!.replace(/^M\s*/, '').split(/[\s,L]+/).map(Number)
    expect(x).toBeCloseTo(centre.x, 0)
    expect(y).toBeCloseTo(centre.y, 0)
    // …and the flood grows from it.
    await page.clock.runFor(THIRD + 40)
    const flood = await page.evaluate(() => [getComputedStyle(document.documentElement).getPropertyValue('--ms-x'), getComputedStyle(document.documentElement).getPropertyValue('--ms-y')])
    expect(parseFloat(flood[0]!)).toBeCloseTo(centre.x, 0)
    expect(parseFloat(flood[1]!)).toBeCloseTo(centre.y, 0)
    await page.clock.runFor(PUMP.end)
  })
})

test('the service worker keeps both sounds, in both formats, for offline', async ({ page }) => {
  await page.goto('/#/settings')
  const sw = await page.evaluate(() => fetch('/sw.js').then((r) => r.text()))
  for (const file of ['grit_hammer_drop.ogg', 'grit_pump.ogg', 'grit_hammer_drop.wav', 'grit_pump.wav']) expect(sw).toContain(`sounds/${file}`)
})

test('with reduced motion: no animation, the side changes at the tap', async ({ page }) => {
  // The default e2e page: motion off, as with the system's reduced motion.
  await page.clock.setFixedTime(WEDNESDAY)
  await page.goto('/#/settings')
  await flip(page, 'BODYBUILDING')
  await expect.poll(() => dataMode(page)).toBe('bodybuilding')
  expect(await drawn(page)).toBe(0)
})

// Real frames, the production build, no fake clock (installing Playwright's clock, even a fixed date, replaces
// requestAnimationFrame). A frame's time is from its rAF timestamp to the end of the frame's callbacks, the animation's
// drawing among them; each callback is timed by a wrapper put in before the app loads. The tap's own work is timed from
// the press to the end of its click handlers.
//
// A bare frame loop is timed first for a second, the same way: it gives the display's frame interval, and if the
// machine cannot hold 16 ms with nothing animating, the run fails there and says so (rerun on a quiet machine).
//
// Then three runs: the pump and the drop with their shapes worked out ahead (Settings' idle warm), and a pump straight
// after a scroll, before the warm that follows a scroll could run (the cold path: the switch has moved, so the shapes
// are worked out in the animation's first frames). For each: every frame of the animation is there (none dropped:
// the count, and no gap between frames over one and a half intervals, so 30 fps fails; the one exception is the frame
// the browser skips at the moment to capture the reveal's new side), the first within one and a half intervals of the
// tap, and every frame and the tap within 16 ms.
test.describe('mode switch speed', { tag: '@speed' }, () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  type Sample = { at: number; ts: number; ms: number }
  type Frames = { samples: Sample[]; tap: number; tapMs: number }
  type Rec = { on: boolean; tap: number; tapMs: number; samples: Sample[] }

  test('every frame of the pump and the drop is drawn on time, warm and cold', async ({ page }) => {
    await page.addInitScript(() => {
      const w = window as Window & { __rec?: Rec }
      w.__rec = { on: false, tap: 0, tapMs: -1, samples: [] }
      const raf = window.requestAnimationFrame.bind(window)
      window.requestAnimationFrame = (cb) =>
        raf((ts) => {
          cb(ts)
          const rec = w.__rec!
          if (rec.on) rec.samples.push({ at: ts - rec.tap, ts, ms: performance.now() - ts })
        })
      // The tap: from the press going down to the end of the click's handlers (the switch's included).
      document.addEventListener('pointerdown', () => (w.__rec!.tap = performance.now()), { capture: true })
      window.addEventListener('click', () => {
        if (w.__rec!.on) w.__rec!.tapMs = performance.now() - w.__rec!.tap
      })
    })
    await page.emulateMedia({ reducedMotion: 'no-preference' })
    await page.goto('/#/settings')
    await expect(modeSwitch(page)).toBeVisible()
    // Settings' warm has run (it waits for an idle moment).
    await expect.poll(() => page.evaluate(() => performance.getEntriesByName('ms-warm').length)).toBeGreaterThan(0)

    // The baseline: a bare frame loop, nothing animating.
    const baseline = await page.evaluate(
      () =>
        new Promise<{ ms: number[]; ts: number[] }>((done) => {
          const ms: number[] = []
          const ts: number[] = []
          const start = performance.now()
          const loop = (t: number) => {
            ms.push(performance.now() - t)
            ts.push(t)
            if (performance.now() - start < 1000) requestAnimationFrame(loop)
            else done({ ms, ts })
          }
          requestAnimationFrame(loop)
        }),
    )
    const worstBaseline = Math.max(...baseline.ms)
    expect(worstBaseline, `the machine itself is over budget with nothing animating (${baseline.ms.length} frames): rerun when it is quiet`).toBeLessThanOrEqual(16)
    const gaps = baseline.ts.slice(1).map((t, i) => t - baseline.ts[i]!).sort((a, b) => a - b)
    const interval = gaps[Math.floor(gaps.length / 2)]!
    expect(interval, 'the display frame interval').toBeGreaterThan(5)

    const warms = () => page.evaluate(() => performance.getEntriesByName('ms-warm').length)

    async function play(to: 'BODYBUILDING' | 'POWERLIFTING', before?: () => Promise<void>): Promise<Frames> {
      await page.evaluate(() => {
        const rec = (window as Window & { __rec?: Rec }).__rec!
        rec.samples = []
        rec.tapMs = -1
        rec.on = true
      })
      if (before != null) await before()
      await modeSwitch(page).click()
      // Started (the shapes are built in the first frame after the tap), then over.
      await expect.poll(() => drawn(page)).toBe(1)
      await expect.poll(() => drawn(page), { timeout: 5000 }).toBe(0)
      await expect.poll(() => dataMode(page)).toBe(to === 'BODYBUILDING' ? 'bodybuilding' : null)
      const out = await page.evaluate(() => {
        const rec = (window as Window & { __rec?: Rec }).__rec!
        rec.on = false
        return { samples: rec.samples, tap: rec.tap, tapMs: rec.tapMs }
      })
      // Settings' warm for the new side runs in the pause.
      await page.waitForTimeout(800)
      return out
    }

    const runs: [string, number, Frames][] = []
    runs.push(['pump (warm)', PUMP.end, await play('BODYBUILDING')])
    runs.push(['drop (warm)', DROP.end, await play('POWERLIFTING')])
    // The cold path: Settings scrolled a little and tapped at once, before the warm that follows a scroll at rest.
    let warmsAtScroll = -1
    let warmsAtTap = -1
    runs.push([
      'pump (cold)',
      PUMP.end,
      await play('BODYBUILDING', async () => {
        warmsAtScroll = await warms()
        await page.locator('.shell-main').evaluate((el) => el.scrollBy(0, 9))
        warmsAtTap = await warms()
      }),
    ])
    expect(warmsAtTap, 'no warm between the scroll and the tap: the shapes were cold').toBe(warmsAtScroll)

    const late: string[] = []
    for (const [name, end, run] of runs) {
      const frames = run.samples.filter((s) => s.at > 0 && s.at <= end).sort((a, b) => a.ts - b.ts)
      expect(run.tapMs, `${name}: the tap was timed`).toBeGreaterThanOrEqual(0)
      if (run.tapMs > 16) late.push(`${name}: the tap took ${run.tapMs.toFixed(1)} ms`)
      // Every frame: as many as the animation's length allows, less one at each end.
      const want = Math.floor(end / interval) - 2
      expect(frames.length, `${name}: frames drawn (want ${want} at ${interval.toFixed(1)} ms)`).toBeGreaterThanOrEqual(want)
      expect(frames[0]!.at, `${name}: the first frame after the tap`).toBeLessThanOrEqual(interval * 1.5)
      expect(Math.max(...frames.map((s) => s.at)), `${name}: the last frame timed`).toBeGreaterThan(end - interval * 2)
      // One frame may be missing at the moment, and only there: the browser draws no frame while it captures the new
      // side for the reveal (a view transition does this however short its update is). Any other gap fails.
      const moment = end === DROP.end ? DROP.impact : THIRD
      let captureFrame = 0
      for (let i = 1; i < frames.length; i++) {
        const gap = frames[i]!.ts - frames[i - 1]!.ts
        if (gap <= interval * 1.5) continue
        const atMoment = frames[i - 1]!.at >= moment - interval && frames[i - 1]!.at <= moment + interval * 2 && gap <= interval * 2.5
        if (atMoment && captureFrame++ === 0) continue
        late.push(`${name}: a ${gap.toFixed(1)} ms gap before ${Math.round(frames[i]!.at)} ms`)
      }
      for (const f of frames) if (f.ms > 16) late.push(`${name}: ${f.ms.toFixed(1)} ms at ${Math.round(f.at)} ms`)
    }
    expect(late, `late frames (baseline worst ${worstBaseline.toFixed(1)} ms, interval ${interval.toFixed(1)} ms)`).toEqual([])
  })
})
