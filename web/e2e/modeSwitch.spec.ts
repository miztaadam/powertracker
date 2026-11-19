import { existsSync, mkdirSync, readFileSync } from 'node:fs'
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

  // What made the reveal slow, pinned on the held clock so no timing is involved (1.15 speed fix). Each would show as
  // dropped frames only some of the time; these show it every time.
  // - The old side's picture is moved by an animation on its own pseudo-element, not through <html> (a custom property
  //   on <html> written each frame restyled the whole page): over the reveal, <html>'s attributes change a few times
  //   (the classes, the circle's centre), never once a frame.
  // - The view transition's update lays nothing out: the blocks are measured, and the stamp placed, at the next frame.
  for (const kind of ['pump', 'drop'] as const) {
    test(`the ${kind}'s reveal: <html> is not written each frame, and its update measures nothing`, async ({ page }) => {
      await page.addInitScript(() => {
        const w = window as Window & { __pin?: { starts: number; updates: number; rects: number; inUpdate: boolean; mutations: string[]; revealing: boolean } }
        const pin = (w.__pin = { starts: 0, updates: 0, rects: 0, inUpdate: false, mutations: [] as string[], revealing: false })
        const rawRect = Element.prototype.getBoundingClientRect
        Element.prototype.getBoundingClientRect = function (this: Element) {
          if (pin.inUpdate) pin.rects++
          return rawRect.call(this)
        }
        const d = document as unknown as { startViewTransition?: (update: () => void) => unknown }
        const rawStart = d.startViewTransition
        if (typeof rawStart === 'function')
          d.startViewTransition = function (this: unknown, update: () => void) {
            pin.starts++
            return rawStart.call(this, () => {
              pin.updates++
              pin.inUpdate = true
              try {
                return update()
              } finally {
                pin.inUpdate = false
              }
            })
          }
      })
      await openSettings(page)
      // <html>'s own attributes only, not the page under it.
      await page.evaluate(() => {
        const pin = (window as Window & { __pin?: { mutations: string[]; revealing: boolean } }).__pin!
        new MutationObserver((records) => {
          for (const r of records) {
            pin.mutations.push(r.attributeName ?? '')
            if (r.attributeName === 'class' && document.documentElement.classList.contains('ms-revealing')) pin.revealing = true
          }
        }).observe(document.documentElement, { attributes: true })
      })
      await hold(page)
      if (kind === 'drop') {
        await flip(page, 'BODYBUILDING')
        await page.clock.runFor(PUMP.end + 100)
        expect(await dataMode(page)).toBe('bodybuilding')
      }
      const pin = () =>
        page.evaluate(() => {
          const p = (window as Window & { __pin?: { starts: number; updates: number; rects: number; mutations: string[]; revealing: boolean } }).__pin!
          const reveal = document.getAnimations().filter((a) => (a.effect as KeyframeEffect | null)?.pseudoElement === '::view-transition-new(root)' && a.playState !== 'finished')
          return { starts: p.starts, updates: p.updates, rects: p.rects, mutations: p.mutations.length, names: [...p.mutations], revealing: p.revealing, revealLive: reveal.length, className: document.documentElement.className }
        })
      const before = await pin()
      await flip(page, kind === 'pump' ? 'BODYBUILDING' : 'POWERLIFTING')
      const end = kind === 'pump' ? PUMP.end : DROP.end
      const moment = kind === 'pump' ? THIRD : DROP.impact
      // On to the moment and two frames past it: the reveal is under way, its circle a live animation.
      await page.clock.runFor(moment + FRAME * 2)
      await expect.poll(() => dataMode(page)).toBe(kind === 'pump' ? 'bodybuilding' : null)
      await expect.poll(async () => (await pin()).revealLive, { message: 'the reveal is under way' }).toBe(1)
      const under = await pin()
      expect(under.className.split(/\s+/), 'the reveal is the view transition one').toContain('ms-revealing')
      expect(under.starts - before.starts, 'one view transition').toBe(1)
      expect(under.updates - before.updates, 'its update ran').toBe(1)
      expect.soft(under.rects - before.rects, 'getBoundingClientRect calls inside the update').toBe(0)
      // Frame by frame through the reveal, while the content shakes or swells: <html> is not touched.
      const frames = kind === 'pump' ? 12 : 6
      for (let i = 0; i < frames; i++) await page.clock.runFor(FRAME)
      const mid = await pin()
      expect(mid.revealLive, 'still revealing').toBe(1)
      expect.soft(mid.names.slice(under.mutations), `<html>'s attributes written over ${frames} frames of the reveal`).toEqual([])
      // And over the whole switch, a few writes, not one a frame.
      await page.clock.runFor(end)
      const after = await pin()
      expect(after.revealing, 'the reveal ran').toBe(true)
      expect(after.className).not.toMatch(/\bms-/)
      expect.soft(after.mutations - before.mutations, `<html>'s attributes written over the whole ${kind} (${after.names.slice(before.mutations).join(', ')})`).toBeLessThanOrEqual(16)
    })
  }
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
// requestAnimationFrame).
//
// What is measured, in the page, before the app loads:
// - **Frames:** a bare frame loop of the test's own (requestAnimationFrame as the browser gives it) notes every frame's
//   timestamp. It does no work, so a gap between two of its frames is a frame the browser did not draw.
// - **The app's script:** every callback the app gets is wrapped and timed: animation frames, timers, message ports
//   (React's scheduler), idle callbacks, event listeners, IndexedDB handlers and the view transition's update. A
//   frame's script is the time those callbacks ran between its timestamp and the next frame's (overlaps counted once).
//   Promise continuations are not timed.
// - **The tap:** from the press going down to the end of the click's handlers.
//
// Windows: an idle control, then an animation, three times: the pump and the drop with their shapes worked out ahead
// (Settings' idle warm), and a pump straight after a scroll, before the warm that follows a scroll could run (the cold
// path: the switch has moved, so the shapes are worked out in the animation's first frames). An idle window is the same
// page, measured the same way for as long, with a tap on the screen's title (which does nothing) in place of the switch.
// Nothing polls the page inside a window: the test waits in the page for the window to end, and checks what it has to
// once the window is over.
//
// The rules, for every frame of every animation, from the tap to its end:
// (a) the app's script is at most 16 ms;
// (b) no gap between frames over one and a half intervals, so no frame is skipped, with one exception: at the moment
//     (the drop's impact, the pump's third beat) the browser may skip one frame, and only one, while it takes the new
//     side's picture for the reveal;
// (c) as many frames as the animation's length allows, the first within one and a half intervals of the tap, and the
//     tap itself within 16 ms.
// The idle control must itself be clean: every frame there, none of the test's own frames late, and the machine's CPU
// mostly idle while it runs (an idle page leaves it so; on a machine where something else is using it, the animations
// starve while the idle page's frames still come on time). If it is not, the machine is busy, and the run fails saying
// so, whatever the animations did. It never passes on a busy machine.
test.describe('mode switch speed', { tag: '@speed' }, () => {
  /** Over this share of the machine's CPU in use during an idle control, the machine is busy. */
  const BUSY_CPU = 0.3
  /** The machine's CPU time so far, all cores, from /proc/stat; null where there is none. */
  const cpuTimes = () => {
    if (!existsSync('/proc/stat')) return null
    const f = readFileSync('/proc/stat', 'utf8').split('\n')[0]!.trim().split(/\s+/).slice(1).map(Number)
    return { total: f.reduce((a, b) => a + b, 0), idle: f[3]! + (f[4] ?? 0) }
  }

  test.use({ storageState: { cookies: [], origins: [] } })

  /** A task the app ran: its kind, start and end (performance.now). */
  type Task = [string, number, number]
  /** One of the test's own frames: its timestamp, and when its callback ran. */
  type Frame = { ts: number; cb: number }
  /** `cpu`: the share of the machine's CPU time used over the window, by anything (Linux; null elsewhere). */
  type Span = { frames: Frame[]; tasks: Task[]; tap: number; tapMs: number; cpu: number | null }
  type Probe = {
    on: boolean
    tap: number
    tapMs: number
    tasks: Task[]
    run: Promise<Frame[]> | null
    start: (ms: number) => void
  }

  test('every frame of the pump and the drop is drawn on time, warm and cold, against an idle control', async ({ page }) => {
    test.setTimeout(60_000)
    await page.addInitScript(() => {
      const now = () => performance.now()
      const rawRaf = window.requestAnimationFrame.bind(window)
      const rawTimeout = window.setTimeout.bind(window)
      const rawAdd = EventTarget.prototype.addEventListener
      const rawRemove = EventTarget.prototype.removeEventListener
      const P: Probe = {
        on: false,
        tap: 0,
        tapMs: -1,
        tasks: [],
        run: null,
        start(ms) {
          P.tasks = []
          P.tap = 0
          P.tapMs = -1
          P.on = true
          P.run = new Promise((done) => {
            const frames: Frame[] = []
            const t0 = now()
            const loop = (ts: number) => {
              frames.push({ ts, cb: now() })
              if (now() - t0 < ms) rawRaf(loop)
              // A moment more, so the last frame's tasks are in; then the window is over.
              else
                rawTimeout(() => {
                  P.on = false
                  done(frames)
                }, 50)
            }
            rawRaf(loop)
          })
        },
      }
      ;(window as Window & { __probe?: Probe }).__probe = P
      const timed = <F extends (...a: never[]) => unknown>(kind: string, fn: F): F =>
        function (this: unknown, ...a: never[]) {
          const s = now()
          try {
            return fn.apply(this, a)
          } finally {
            if (P.on) P.tasks.push([kind, s, now()])
          }
        } as F
      window.requestAnimationFrame = (cb) => rawRaf(timed('frame', cb))
      window.setTimeout = ((fn: unknown, delay?: number, ...a: unknown[]) =>
        rawTimeout(typeof fn === 'function' ? timed('timer', fn as () => void) : (fn as string), delay, ...a)) as typeof window.setTimeout
      const w = window as Window & { requestIdleCallback?: (cb: IdleRequestCallback, o?: IdleRequestOptions) => number }
      if (typeof w.requestIdleCallback === 'function') {
        const rawIdle = w.requestIdleCallback.bind(window)
        w.requestIdleCallback = (cb, o) => rawIdle(timed('idle', cb), o)
      }
      const handler = (proto: object, prop: string, kind: string) => {
        const d = Object.getOwnPropertyDescriptor(proto, prop)
        if (d?.set == null) return
        Object.defineProperty(proto, prop, {
          ...d,
          set(fn: unknown) {
            d.set!.call(this, typeof fn === 'function' ? timed(kind, fn as () => void) : fn)
          },
        })
      }
      handler(MessagePort.prototype, 'onmessage', 'message')
      handler(IDBRequest.prototype, 'onsuccess', 'idb')
      handler(IDBRequest.prototype, 'onerror', 'idb')
      handler(IDBTransaction.prototype, 'oncomplete', 'idb')
      handler(IDBOpenDBRequest.prototype, 'onupgradeneeded', 'idb')
      const wrapped = new WeakMap<object, EventListener>()
      EventTarget.prototype.addEventListener = function (type: string, fn: EventListenerOrEventListenerObject | null, o?: boolean | AddEventListenerOptions) {
        if (typeof fn !== 'function') return rawAdd.call(this, type, fn, o)
        let t = wrapped.get(fn)
        if (t == null) {
          t = timed(`event:${type}`, fn)
          wrapped.set(fn, t)
        }
        return rawAdd.call(this, type, t, o)
      }
      EventTarget.prototype.removeEventListener = function (type: string, fn: EventListenerOrEventListenerObject | null, o?: boolean | EventListenerOptions) {
        return rawRemove.call(this, type, (typeof fn === 'function' && wrapped.get(fn)) || fn, o)
      }
      const d = document as unknown as { startViewTransition?: (update: unknown) => unknown }
      const rawStart = d.startViewTransition
      if (typeof rawStart === 'function')
        d.startViewTransition = function (this: unknown, update: unknown) {
          return rawStart.call(this, typeof update === 'function' ? timed('view transition update', update as () => void) : update)
        }
      // The tap, not timed as the app's: from the press going down to the end of the click's handlers (the switch's
      // included; this listener is the last on the way back up).
      rawAdd.call(document, 'pointerdown', () => (P.tap = now()), { capture: true })
      rawAdd.call(window, 'click', () => {
        if (P.on && P.tap > 0) P.tapMs = now() - P.tap
      })
    })
    await page.emulateMedia({ reducedMotion: 'no-preference' })
    await page.goto('/#/settings')
    await expect(modeSwitch(page)).toBeVisible()
    const warms = () => page.evaluate(() => performance.getEntriesByName('ms-warm').map((e) => e.startTime))
    // Settings' warm has run (it waits for an idle moment).
    await expect.poll(async () => (await warms()).length).toBeGreaterThan(0)
    await page.waitForTimeout(500)

    /** One window: the probe started, the tap at `at`, then nothing until the window has ended in the page. */
    async function measure(ms: number, at: { x: number; y: number }): Promise<Span> {
      const before = cpuTimes()
      await page.evaluate((length) => (window as Window & { __probe?: Probe }).__probe!.start(length), ms)
      await page.mouse.click(at.x, at.y)
      const span = await page.evaluate(async () => {
        const p = (window as Window & { __probe?: Probe }).__probe!
        const frames = await p.run!
        return { frames, tasks: p.tasks, tap: p.tap, tapMs: p.tapMs }
      })
      const after = cpuTimes()
      const cpu = before != null && after != null && after.total > before.total ? 1 - (after.idle - before.idle) / (after.total - before.total) : null
      return { ...span, cpu }
    }
    const centre = async (selector: string) => {
      const box = (await page.locator(selector).first().boundingBox())!
      return { x: box.x + box.width / 2, y: box.y + box.height / 2 }
    }
    const title = async () => {
      const box = (await page.locator('.gr-screen-title').first().boundingBox())!
      return { x: box.x + 4, y: box.y + box.height / 2 }
    }

    type Run = { name: string; end: number; moment: number; idle: Span; anim: Span }
    const runs: Run[] = []
    async function play(name: string, kind: 'pump' | 'drop', before?: () => Promise<void>) {
      const end = kind === 'pump' ? PUMP.end : DROP.end
      const idle = await measure(end + 200, await title())
      await page.waitForTimeout(400)
      const at = await centre('.settings-mode-switch .gr-switch-track')
      const warmsAtTap = (await warms()).length
      if (before != null) await before()
      const anim = await measure(end + 200, at)
      runs.push({ name, end, moment: kind === 'pump' ? THIRD : DROP.impact, idle, anim })
      // Over, on the new side; then Settings works the shapes out again for the switch where it now sits.
      expect(await drawn(page), `${name}: over by the window's end`).toBe(0)
      expect(await dataMode(page), `${name}: the new side`).toBe(kind === 'pump' ? 'bodybuilding' : null)
      await expect.poll(async () => (await warms()).length, { message: `${name}: the shapes worked out again after the side changed` }).toBeGreaterThan(warmsAtTap)
      await page.waitForTimeout(400)
      return anim
    }

    await play('pump (warm)', 'pump')
    await play('drop (warm)', 'drop')
    // The cold path: Settings scrolled a little and tapped at once, before the warm that follows a scroll at rest. The
    // switch has moved, so the shapes for it are new; no warm comes between the scroll and the animation's end (the
    // warm marks are read afterwards, by their times, so nothing polls inside the window).
    const switchY = () => page.locator('.settings-mode-switch .gr-switch-track').evaluate((el) => el.getBoundingClientRect().top)
    let movedBy = 0
    let scrolledAt = 0
    const cold = await play('pump (cold)', 'pump', async () => {
      const y = await switchY()
      scrolledAt = await page.locator('.shell-main').evaluate((el) => {
        el.scrollBy(0, 9)
        return performance.now()
      })
      movedBy = y - (await switchY())
    })
    expect(movedBy, 'the scroll moved the switch').toBeGreaterThan(4)
    const coldWarms = (await warms()).filter((t) => t > scrolledAt && t < cold.tap + PUMP.end)
    expect(coldWarms, 'no warm between the scroll and the animation: the shapes were cold').toEqual([])
    // A scroll that comes to rest works them out again.
    const beforeRest = (await warms()).length
    await page.locator('.shell-main').evaluate((el) => el.scrollBy(0, -9))
    await expect.poll(async () => (await warms()).length, { message: 'the shapes worked out again after a scroll at rest' }).toBeGreaterThan(beforeRest)

    // The display's frame interval: the idle control's median gap.
    const idleGaps = runs.flatMap((r) => r.idle.frames.slice(1).map((f, i) => f.ts - r.idle.frames[i]!.ts)).sort((a, b) => a - b)
    const interval = idleGaps[Math.floor(idleGaps.length / 2)]!
    expect(interval, 'the display frame interval').toBeGreaterThan(5)

    /** The frames from the tap to the end, each with the app's script in it and the gap after the one before. */
    const framesOf = (w: Span, end: number) => {
      const frames = w.frames.filter((f) => f.ts - w.tap > 0 && f.ts - w.tap <= end)
      return frames.map((f, i) => {
        const next = frames[i + 1]?.ts ?? f.ts + interval
        const spans = w.tasks
          .map(([, s, e]) => [Math.max(s, f.ts), Math.min(e, next)] as const)
          .filter(([s, e]) => e > s)
          .sort((a, b) => a[0] - b[0])
        let script = 0
        let to = -Infinity
        for (const [s, e] of spans) {
          if (e <= to) continue
          script += e - Math.max(s, to)
          to = e
        }
        return { at: f.ts - w.tap, script, gap: i === 0 ? 0 : f.ts - frames[i - 1]!.ts, late: f.cb - f.ts }
      })
    }

    // The control first: a busy machine fails the run on its own account, before anything is said about the app.
    const busy: string[] = []
    const control: string[] = []
    for (const r of runs) {
      const frames = framesOf(r.idle, r.end)
      expect(r.idle.tap, `${r.name}, idle control: the tap was seen`).toBeGreaterThan(0)
      const cpu = r.idle.cpu == null ? 'unknown' : `${Math.round(r.idle.cpu * 100)} %`
      control.push(`${r.name} control: ${frames.length} frames, widest gap ${Math.max(...frames.map((f) => f.gap)).toFixed(1)} ms, latest callback ${Math.max(...frames.map((f) => f.late)).toFixed(1)} ms, machine CPU ${cpu}`)
      if (frames.length < Math.floor(r.end / interval) - 2) busy.push(`${r.name}, idle control: only ${frames.length} frames`)
      // Something else on the machine using the CPU starves the animation long before it shows in an idle page's
      // frames: an idle page on a quiet machine uses a few per cent.
      if (r.idle.cpu != null && r.idle.cpu > BUSY_CPU) busy.push(`${r.name}, idle control: the machine's CPU was ${Math.round(r.idle.cpu * 100)} % in use`)
      for (const f of frames.slice(1)) if (f.gap > interval * 1.5) busy.push(`${r.name}, idle control: a ${f.gap.toFixed(1)} ms gap before ${Math.round(f.at)} ms`)
      for (const f of frames) if (f.late > 16) busy.push(`${r.name}, idle control: a frame's callback ran ${f.late.toFixed(1)} ms late at ${Math.round(f.at)} ms`)
    }

    const late: string[] = []
    const summary: string[] = []
    for (const r of runs) {
      const frames = framesOf(r.anim, r.end)
      expect(r.anim.tapMs, `${r.name}: the tap was timed`).toBeGreaterThanOrEqual(0)
      if (r.anim.tapMs > 16) late.push(`${r.name}: the tap took ${r.anim.tapMs.toFixed(1)} ms`)
      // (c) Every frame there: as many as the animation's length allows, less one at each end and the moment's.
      const want = Math.floor(r.end / interval) - 2
      if (frames.length < want) late.push(`${r.name}: ${frames.length} frames drawn, want ${want}`)
      if (frames.length === 0) continue
      if (frames[0]!.at > interval * 1.5) late.push(`${r.name}: the first frame ${frames[0]!.at.toFixed(1)} ms after the tap`)
      if (frames.at(-1)!.at <= r.end - interval * 2) late.push(`${r.name}: the last frame timed at ${frames.at(-1)!.at.toFixed(1)} ms`)
      // (a) The app's own script, every frame.
      for (const f of frames) if (f.script > 16) late.push(`${r.name}: ${f.script.toFixed(1)} ms of the app's script in the frame at ${Math.round(f.at)} ms`)
      // (b) No skipped frame, but for one at the moment.
      let skips = 0
      let momentGap = 0
      for (let i = 1; i < frames.length; i++) {
        const { gap } = frames[i]!
        if (gap <= interval * 1.5) continue
        const from = frames[i - 1]!.at
        const atMoment = from >= r.moment - interval && from <= r.moment + interval * 2 && gap <= interval * 2.5
        if (atMoment && skips++ === 0) {
          momentGap = gap
          continue
        }
        late.push(`${r.name}: a ${gap.toFixed(1)} ms gap before ${Math.round(frames[i]!.at)} ms`)
      }
      summary.push(`${r.name}: ${frames.length} frames, script max ${Math.max(...frames.map((f) => f.script)).toFixed(1)} ms, widest gap ${Math.max(...frames.map((f) => f.gap)).toFixed(1)} ms, moment skip ${momentGap.toFixed(1)} ms, tap ${r.anim.tapMs.toFixed(1)} ms`)
    }
    const said = `interval ${interval.toFixed(1)} ms; ${summary.join('; ')}; ${control.join('; ')}`
    console.log(`mode switch speed: ${said}`)
    expect(busy, `MACHINE BUSY: the idle control, with nothing animating, was not clean (below), so this run says nothing about the app; rerun it on a quiet machine (${said})`).toEqual([])
    expect(late, `late frames (${said})`).toEqual([])
  })
})
