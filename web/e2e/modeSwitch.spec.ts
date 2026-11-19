import { existsSync, mkdirSync, readFileSync, readdirSync } from 'node:fs'
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
        const pin = (w.__pin = { starts: 0, updates: 0, rects: 0, inUpdate: false as boolean, mutations: [] as string[], revealing: false })
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

  test('a reveal cut short leaves nothing behind: no animation on its pictures, no class, no transform', async ({ page }) => {
    await openSettings(page)
    await hold(page)
    await settled(page, 'at the start')
    // A pump cut short by a tap, once the old side's picture is moving.
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(THIRD)
    await untilOldSideMoves(page, 'the pump')
    const title = (await page.locator('.gr-screen-title').first().boundingBox())!
    await page.mouse.click(title.x + 4, title.y + title.height / 2)
    expect(await dataMode(page)).toBe('bodybuilding')
    await settled(page, 'the pump, cut short by a tap')
    await page.clock.runFor(PUMP.end + 100)
    await settled(page, 'the pump, cut short by a tap, later')
    // A drop cut short by the next switch, once the old side's picture is moving.
    await flip(page, 'POWERLIFTING')
    await page.clock.runFor(DROP.impact)
    await untilOldSideMoves(page, 'the drop')
    await flip(page, 'BODYBUILDING')
    await page.clock.runFor(FRAME)
    // The pump that follows has started (its own reveal comes at its third beat); nothing of the drop's is left.
    expect(await page.evaluate(() => document.documentElement.classList.contains('ms-pumping')), 'the pump has started').toBe(true)
    expect(await revealLeft(page), 'the drop, cut short by the next switch').toEqual({ classes: [], pictures: [] })
    await page.clock.runFor(PUMP.end + 100)
    expect(await dataMode(page)).toBe('bodybuilding')
    await settled(page, 'the drop, cut short by the next switch, and the pump after it')
    // The next switch, left to run: it reveals as a first one does, and leaves nothing either, its finished circle included.
    await flip(page, 'POWERLIFTING')
    await page.clock.runFor(DROP.impact)
    await untilOldSideMoves(page, 'the drop after')
    await page.clock.runFor(DROP.end)
    expect(await dataMode(page)).toBeNull()
    expect(await drawn(page)).toBe(0)
    await settled(page, 'a drop run to its end')
  })

  // The reveal's circle, on the held clock: from the moment it grows frame by frame, and it reaches the whole screen
  // (the reveal ends) before the animation does.
  for (const kind of ['pump', 'drop'] as const) {
    test(`the ${kind}'s reveal circle grows, frame by frame, to the whole screen`, async ({ page }) => {
      await openSettings(page)
      await hold(page)
      if (kind === 'drop') {
        await flip(page, 'BODYBUILDING')
        await page.clock.runFor(PUMP.end + 100)
      }
      await flip(page, kind === 'pump' ? 'BODYBUILDING' : 'POWERLIFTING')
      const end = kind === 'pump' ? PUMP.end : DROP.end
      const moment = kind === 'pump' ? THIRD : DROP.impact
      await page.clock.runFor(moment)
      await untilOldSideMoves(page, `the ${kind}`)
      const radii = await circleGrowth(page, Math.ceil(end / FRAME))
      // A circle on screen for several frames, growing every one of them.
      expect(radii.length, `frames the circle was drawn (${radii.join(', ')})`).toBeGreaterThanOrEqual(kind === 'pump' ? 10 : 4)
      for (let i = 1; i < radii.length; i++) expect(radii[i]!, `the circle's radius at frame ${i} (${radii.join(', ')})`).toBeGreaterThan(radii[i - 1]!)
      expect(radii.at(-1)!, 'the radius it had reached').toBeGreaterThan(200)
      // Then whole: the reveal is over (the browser ends the transition on its next frame), its circle gone, before the
      // animation ends.
      await expect
        .poll(
          async () => {
            const left = await revealLeft(page)
            if (left.classes.length + left.pictures.length > 0) await page.clock.runFor(FRAME)
            return left
          },
          { message: 'the reveal, once the circle is whole', intervals: [0] },
        )
        .toEqual({ classes: [], pictures: [] })
      expect(await drawn(page), 'the animation is still running when the reveal is over').toBe(1)
      await page.clock.runFor(end)
      expect(await drawn(page)).toBe(0)
    })
  }

  // Where `animate` cannot hold an animation on a view transition's pseudo-element (it throws, or it ignores the
  // pseudo-element and would move <html> itself), the switch still completes: without the old side's moving picture,
  // or, without the circle, the new side at once.
  for (const [side, how] of [
    ['old', 'throws'],
    ['old', 'ignores the pseudo-element'],
    ['new', 'throws'],
    ['new', 'ignores the pseudo-element'],
  ] as const) {
    test(`where animate ${how} for the ${side} side's picture, the switch completes and leaves nothing moved`, async ({ page }) => {
      const errors: string[] = []
      page.on('pageerror', (e) => errors.push(e.message))
      await page.addInitScript(
        ([pseudo, throws]) => {
          const w = window as Window & { __stubbed?: number; __atReady?: { revealing: boolean; circles: number } }
          w.__stubbed = 0
          // Just after the app has had the reveal's `ready` (its handler is queued after this one).
          const rawStart = Document.prototype.startViewTransition
          Document.prototype.startViewTransition = function (this: Document, update?: ViewTransitionUpdateCallback | StartViewTransitionOptions) {
            const transition = rawStart.call(this, update as ViewTransitionUpdateCallback)
            transition.ready
              .then(() =>
                Promise.resolve().then(() => {
                  w.__atReady = {
                    revealing: document.documentElement.classList.contains('ms-revealing'),
                    circles: document.getAnimations().filter((a) => (a.effect as KeyframeEffect | null)?.pseudoElement === '::view-transition-new(root)').length,
                  }
                }),
              )
              .catch(() => {})
            return transition
          }
          const raw = Element.prototype.animate
          Element.prototype.animate = function (this: Element, keyframes: Keyframe[] | PropertyIndexedKeyframes | null, options?: number | KeyframeAnimationOptions) {
            if (typeof options === 'object' && options.pseudoElement === pseudo) {
              w.__stubbed!++
              if (throws) throw new DOMException('Not supported', 'NotSupportedError')
              const { pseudoElement: _, ...rest } = options
              return raw.call(this, keyframes, rest)
            }
            return raw.call(this, keyframes, options)
          }
        },
        [`::view-transition-${side}(root)`, how === 'throws'] as const,
      )
      await openSettings(page)
      await hold(page)
      await flip(page, 'BODYBUILDING')
      const atReady = () => page.evaluate(() => (window as Window & { __atReady?: { revealing: boolean; circles: number } }).__atReady ?? null)
      if (side === 'old') {
        // Without the old side's moving picture, the circle still runs: the reveal is held, and it grows.
        await page.clock.runFor(THIRD)
        await expect.poll(async () => (await atReady()) ?? (await page.clock.runFor(FRAME), null), { intervals: [0] }).toEqual({ revealing: true, circles: 1 })
        const radii = await circleGrowth(page, 6)
        expect(radii.length, `frames the circle was drawn (${radii.join(', ')})`).toBe(6)
        for (let i = 1; i < radii.length; i++) expect(radii[i]!, `the circle's radius at frame ${i} (${radii.join(', ')})`).toBeGreaterThan(radii[i - 1]!)
      } else {
        // Without the circle, the reveal ends at its `ready`: no class left, no circle, the new side on, at once.
        await page.clock.runFor(THIRD)
        await expect.poll(async () => (await atReady()) ?? (await page.clock.runFor(FRAME), null), { intervals: [0] }).toEqual({ revealing: false, circles: 0 })
        expect(await dataMode(page)).toBe('bodybuilding')
      }
      // Frame by frame through the moment and the reveal: <html> itself is never animated, and the shell moves only by
      // the swell's own transform.
      let at = 0
      while (at < PUMP.end + 100) {
        await page.clock.runFor(FRAME * 4)
        at += FRAME * 4
        expect(await page.evaluate(() => document.getAnimations().filter((a) => (a.effect as KeyframeEffect | null)?.target === document.documentElement && (a.effect as KeyframeEffect).pseudoElement == null).length), `animations on <html> itself at ${at} ms`).toBe(0)
      }
      expect(await page.evaluate(() => (window as Window & { __stubbed?: number }).__stubbed), 'the stub was asked').toBe(1)
      expect(await dataMode(page)).toBe('bodybuilding')
      expect(await storedMode(page)).toBe('BODYBUILDING')
      expect(await drawn(page)).toBe(0)
      await settled(page, 'after the switch')
      expect(await page.evaluate(() => getComputedStyle(document.documentElement).transform)).toBe('none')
      expect(errors).toEqual([])
      // And the next switch works the same way.
      await flip(page, 'POWERLIFTING')
      await page.clock.runFor(DROP.end + 100)
      expect(await dataMode(page)).toBeNull()
      await settled(page, 'after the next switch')
      expect(errors).toEqual([])
    })
  }
})

/** What a reveal has left: the overlay's classes on <html>, and any animation on a view transition's pseudo-element. */
const revealLeft = (page: Page) =>
  page.evaluate(() => ({
    classes: [...document.documentElement.classList].filter((c) => c.startsWith('ms-revealing')),
    pictures: document
      .getAnimations()
      .map((a) => ({ a, pseudo: (a.effect as KeyframeEffect | null)?.pseudoElement ?? '' }))
      .filter(({ pseudo }) => pseudo.startsWith('::view-transition'))
      .map(({ a, pseudo }) => `${pseudo} ${a.playState}`),
  }))

/** Nothing of a switch is left: no reveal, no class of the overlay's, the shell and <html> not transformed. */
async function settled(page: Page, when: string) {
  expect(await revealLeft(page), when).toEqual({ classes: [], pictures: [] })
  expect(await page.evaluate(() => [...document.documentElement.classList].filter((c) => c.startsWith('ms-'))), `${when}: the overlay's classes`).toEqual([])
  expect(await page.locator('.shell').evaluate((el) => (el as HTMLElement).style.transform), `${when}: the shell's transform`).toBe('')
  expect(await page.locator('.shell').evaluate((el) => getComputedStyle(el).transform), `${when}: the shell's transform`).toBe('none')
}

/**
 * The new side's circle, a frame at a time on the held clock, for at most `frames` frames or until it is gone or whole:
 * its radius at each (the clip animation's time is its radius in px).
 */
async function circleGrowth(page: Page, frames: number): Promise<number[]> {
  const radii: number[] = []
  for (let i = 0; i < frames; i++) {
    const r = await page.evaluate(() => {
      const a = document.getAnimations().find((x) => (x.effect as KeyframeEffect | null)?.pseudoElement === '::view-transition-new(root)')
      return a == null || a.playState === 'finished' ? null : Number(a.currentTime)
    })
    if (r == null) break
    radii.push(r)
    if (i < frames - 1) await page.clock.runFor(FRAME)
  }
  return radii
}

/** Steps the held clock a frame at a time until the old side's picture has its moving animation. */
async function untilOldSideMoves(page: Page, what: string) {
  await expect
    .poll(
      async () => {
        const moving = await page.evaluate(() => document.getAnimations().some((a) => (a.effect as KeyframeEffect | null)?.pseudoElement === '::view-transition-old(root)'))
        if (!moving) await page.clock.runFor(FRAME)
        return moving
      },
      { message: `${what}: the old side's picture moves`, intervals: [0] },
    )
    .toBe(true)
}

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
//   timestamp, and posts itself a message that runs once the frame's rendering is done. It does no work, so a gap
//   between two of its frames is a frame the browser did not draw; from a frame's timestamp to its message is the
//   frame's work on the page's thread (the app's script, style, layout, paint), the frame's "work".
// - **The app's script:** every callback the app gets is wrapped and timed: animation frames, timers, message ports
//   (React's scheduler), idle callbacks, event listeners, IndexedDB handlers and the view transition's update. A
//   frame's script is the time those callbacks ran between its timestamp and the next frame's (overlaps counted once).
//   Promise continuations are not timed.
// - **The tap:** from the press going down to the end of the click's handlers.
// - **The reveal:** the calls to startViewTransition, and when <html> has the ms-revealing class.
// - **The machine's CPU** (Linux): all of it, the share stolen by the host, and the share of the test's own processes
//   (this worker and the browser it runs), over each window.
//
// Windows: an idle control, then an animation: the pump and the drop five times each with their shapes worked out
// ahead (Settings' idle warm), then a pump straight after a scroll, before the warm that follows a scroll could run
// (the cold path: the switch has moved, so the shapes are worked out in the animation's first frames). An idle window
// is the same page, measured the same way for as long, with a tap on the screen's title (which does nothing) in place
// of the switch. Nothing polls the page inside a window: the test waits in the page for the window to end, and checks
// what it has to once the window is over.
//
// What fails the run, in this order:
// 1. Motion off (the app's setting or the system's reduced motion): nothing animates, so nothing is measured.
// 2. The reveal not run, in any animation: startViewTransition not called exactly once, <html> not ms-revealing for
//    80 ms or more, or far fewer frames than the animation's length. Checked straight after each window, so it fails at the first.
// 3. The calibration: the display's frame interval (the idle controls' median gap) over 20 ms.
// 4. MACHINE BUSY, when the app's checks below fail and the machine was not quiet: an idle control not clean (a frame
//    missed or late, or the machine's CPU over 30 % in use), or the animations' own windows had the CPU taken by
//    something other than the test's own processes. It never blames the app for a busy machine. Also MACHINE BUSY,
//    whatever the app did, where noise could have helped the run pass: idle controls not clean while their interval
//    is off 60 Hz's (it sets every gap threshold), or the cold pump's animation window with its CPU taken (a busy
//    machine can hold back the warm the cold proof looks for). Idle controls not clean while the app passes: a loud MACHINE NOISE warning,
//    and the run passes.
// 5. Reported, not failed: two or more bad animations of one kind (the pump, warm and cold; the drop). An animation is bad when any frame
//    of it, from the tap to its end, breaks a rule: (a) the app's script over 16 ms; (b) a gap between frames over
//    one and a half intervals, but for one skip at the moment (the drop's impact, the pump's third beat) of up to two
//    and a half, while the browser takes the new side's picture; (c) fewer frames than the length allows, the first
//    over one and a half intervals after the tap, or the tap itself over 16 ms. One stray lost frame does not fail
//    the run; the same fault twice in five is a LATE FRAMES warning.
// 6. The reveal's work: the median work per frame over every reveal's frames (from three frames after the moment to
//    the reveal's end), pooled across the eleven animations, over WORK_LIMIT; or either kind's own median over its
//    WORK_LIMIT_OF (each kind's: a SLOW KIND warning, not a failure). The limit is in milliseconds of this
//    machine (the e2e container): it sits between the build before the 1.15 speed fix and the build after it.
test.describe('mode switch speed', { tag: '@speed' }, () => {
  /** Over this share of the machine's CPU in use during an idle control, the machine is busy. */
  const BUSY_CPU = 0.3
  /** Over this share of the machine's CPU used, during an animation, by anything but the test's own processes. */
  const OTHER_CPU = 0.25
  /** Over this share of the machine's CPU stolen by the host during an animation. */
  const STEAL_CPU = 0.05
  /** The warm pumps and drops. */
  const REPEATS = 5
  /** Bad animations of one kind that fail the run. */
  const BAD = 2
  /** The display's frame interval may be at most this; above, the thresholds would scale to a slow display. */
  const MAX_INTERVAL = 20
  /** 60 Hz's frame interval, ms. */
  const HZ60 = 1000 / 60
  /** Over this, the idle controls' interval is not 60 Hz's; if they were noisy too, the noise may have set it. */
  const NOT_60HZ = HZ60 + 0.8
  /** The shortest reveal, ms (the drop's runs about 170 ms, the pump's about 390). */
  const MIN_REVEAL = 80
  /** The pooled median of the reveal's work per frame, ms (see 6 above). */
  const WORK_LIMIT = 5.5
  /**
   * Each kind's own median, ms, so a regression in one kind is not diluted by the other (the drop is about a quarter
   * of the pool). Midway between the speed fix's worst median here (pump 5.3, drop 4.9) and the build before it's best
   * (6.6, 6.2).
   */
  const WORK_LIMIT_OF = { pump: 6.0, drop: 5.5 }

  /** The machine's CPU time so far, all cores, from /proc/stat, in clock ticks; null where there is none. */
  const procStat = () => {
    if (!existsSync('/proc/stat')) return null
    const f = readFileSync('/proc/stat', 'utf8').split('\n')[0]!.trim().split(/\s+/).slice(1).map(Number)
    return { total: f.reduce((a, b) => a + b, 0), idle: f[3]! + (f[4] ?? 0), steal: f[7] ?? 0 }
  }
  /** The CPU time, in clock ticks, of this worker and every process under it (the browser and its children). */
  const ownTicks = () => {
    const children = new Map<number, number[]>()
    const ticks = new Map<number, number>()
    for (const name of readdirSync('/proc')) {
      if (!/^\d+$/.test(name)) continue
      let stat: string
      try {
        stat = readFileSync(`/proc/${name}/stat`, 'utf8')
      } catch {
        continue
      }
      // After the command's name, which may hold spaces and brackets: state, ppid, … utime (14th), stime (15th).
      const f = stat.slice(stat.lastIndexOf(')') + 2).split(' ')
      const pid = Number(name)
      const ppid = Number(f[1])
      ticks.set(pid, Number(f[11]) + Number(f[12]))
      children.set(ppid, [...(children.get(ppid) ?? []), pid])
    }
    let sum = 0
    const todo = [process.pid]
    while (todo.length > 0) {
      const pid = todo.pop()!
      sum += ticks.get(pid) ?? 0
      todo.push(...(children.get(pid) ?? []))
    }
    return sum
  }
  type CpuAt = { total: number; idle: number; steal: number; own: number }
  const cpuAt = (): CpuAt | null => {
    const s = procStat()
    return s == null ? null : { ...s, own: ownTicks() }
  }
  /** Shares of the machine's CPU over a window: in use, by the test's own processes, stolen, and by anything else. */
  type Cpu = { busy: number; own: number; steal: number; other: number }
  const cpuOver = (a: CpuAt | null, b: CpuAt | null): Cpu | null => {
    if (a == null || b == null || b.total <= a.total) return null
    const total = b.total - a.total
    const busy = total - (b.idle - a.idle)
    const own = b.own - a.own
    const steal = b.steal - a.steal
    return { busy: busy / total, own: own / total, steal: steal / total, other: Math.max(0, busy - own - steal) / total }
  }
  const pc = (x: number) => `${Math.round(x * 100)} %`

  test.use({ storageState: { cookies: [], origins: [] } })

  /** A task the app ran: its kind, start and end (performance.now). */
  type Task = [string, number, number]
  /** One of the test's own frames: its timestamp, when its callback ran, and when its rendering was done (0: not seen). */
  type Frame = { ts: number; cb: number; after: number }
  type Measured = {
    frames: Frame[]
    tasks: Task[]
    tap: number
    tapMs: number
    /** Calls to startViewTransition. */
    starts: number
    /** When <html> took the ms-revealing class, and lost it (performance.now; 0: not seen). */
    revealOn: number
    revealOff: number
  }
  type Span = Measured & { cpu: Cpu | null }
  type Probe = {
    on: boolean
    tap: number
    tapMs: number
    tasks: Task[]
    starts: number
    revealOn: number
    revealOff: number
    run: Promise<Frame[]> | null
    start: (ms: number) => void
  }

  test('every frame of the pump and the drop is drawn on time, warm and cold, against an idle control', async ({ page }) => {
    test.setTimeout(240_000)
    const cpuCheck = procStat() != null
    if (!cpuCheck) {
      const say = "CPU CHECK UNAVAILABLE: no /proc/stat here, so a busy machine cannot be told from a slow app; this run's MACHINE BUSY checks see only the idle control's frames"
      console.warn(`\n!!! ${say} !!!\n`)
      test.info().annotations.push({ type: 'warning', description: say })
    }
    await page.addInitScript(() => {
      const now = () => performance.now()
      const rawRaf = window.requestAnimationFrame.bind(window)
      const rawTimeout = window.setTimeout.bind(window)
      const rawAdd = EventTarget.prototype.addEventListener
      const rawRemove = EventTarget.prototype.removeEventListener
      // The probe's own message, made before message ports are wrapped, so it is never timed as the app's.
      const channel = new MessageChannel()
      let rendering: Frame | null = null
      channel.port1.onmessage = () => {
        if (rendering != null) rendering.after = now()
        rendering = null
      }
      let watching = false
      const P: Probe = {
        on: false,
        tap: 0,
        tapMs: -1,
        tasks: [],
        starts: 0,
        revealOn: 0,
        revealOff: 0,
        run: null,
        start(ms) {
          P.tasks = []
          P.tap = 0
          P.tapMs = -1
          P.starts = 0
          P.revealOn = 0
          P.revealOff = 0
          P.on = true
          if (!watching) {
            watching = true
            // The exact class (the reveal without view transitions has ms-revealing-plain, which is not it).
            new MutationObserver((records) => {
              if (!P.on) return
              const had = records.some((r) => (r.oldValue ?? '').split(/\s+/).includes('ms-revealing'))
              const has = document.documentElement.classList.contains('ms-revealing')
              if ((had || has) && P.revealOn === 0) P.revealOn = now()
              if (!has && P.revealOn > 0 && P.revealOff === 0) P.revealOff = now()
            }).observe(document.documentElement, { attributes: true, attributeFilter: ['class'], attributeOldValue: true })
          }
          P.run = new Promise((done) => {
            const frames: Frame[] = []
            const t0 = now()
            const loop = (ts: number) => {
              const f = { ts, cb: now(), after: 0 }
              frames.push(f)
              rendering = f
              channel.port2.postMessage(0)
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
          if (P.on) P.starts++
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
    // 1. Motion on.
    const motion = await page.evaluate(() => ({ setting: document.documentElement.getAttribute('data-motion'), reduced: matchMedia('(prefers-reduced-motion: reduce)').matches }))
    expect(motion, `MOTION IS OFF (data-motion ${motion.setting ?? 'not set'}, reduced motion ${motion.reduced ? 'on' : 'off'}): no animation runs, so this run measures nothing`).toEqual({ setting: null, reduced: false })
    const warms = () => page.evaluate(() => performance.getEntriesByName('ms-warm').map((e) => e.startTime))
    // Settings' warm has run (it waits for an idle moment).
    await expect.poll(async () => (await warms()).length).toBeGreaterThan(0)
    await page.waitForTimeout(500)

    /** One window: the probe started, the tap at `at`, then nothing until the window has ended in the page. */
    async function measure(ms: number, at: { x: number; y: number }): Promise<Span> {
      const before = cpuAt()
      await page.evaluate((length) => (window as Window & { __probe?: Probe }).__probe!.start(length), ms)
      await page.mouse.click(at.x, at.y)
      const span = await page.evaluate(async (): Promise<Measured> => {
        const p = (window as Window & { __probe?: Probe }).__probe!
        const frames = await p.run!
        return { frames, tasks: p.tasks, tap: p.tap, tapMs: p.tapMs, starts: p.starts, revealOn: p.revealOn, revealOff: p.revealOff }
      })
      return { ...span, cpu: cpuOver(before, cpuAt()) }
    }
    const centre = async (selector: string) => {
      const box = (await page.locator(selector).first().boundingBox())!
      return { x: box.x + box.width / 2, y: box.y + box.height / 2 }
    }
    const title = async () => {
      const box = (await page.locator('.gr-screen-title').first().boundingBox())!
      return { x: box.x + 4, y: box.y + box.height / 2 }
    }

    type Run = { name: string; kind: 'pump' | 'drop'; end: number; moment: number; idle: Span; anim: Span }
    const runs: Run[] = []
    async function play(name: string, kind: 'pump' | 'drop', before?: () => Promise<void>) {
      const end = kind === 'pump' ? PUMP.end : DROP.end
      const idle = await measure(end + 200, await title())
      await page.waitForTimeout(400)
      const at = await centre('.settings-mode-switch .gr-switch-track')
      const warmsAtTap = (await warms()).length
      if (before != null) await before()
      const anim = await measure(end + 200, at)
      runs.push({ name, kind, end, moment: kind === 'pump' ? THIRD : DROP.impact, idle, anim })
      // 2. The reveal ran, as a view transition, and the animation drew its frames.
      expect(anim.starts, `THE REVEAL DID NOT RUN: ${name} called startViewTransition ${anim.starts} times, want once`).toBe(1)
      expect(anim.revealOn > 0, `THE REVEAL DID NOT RUN: <html> never had the ms-revealing class in ${name}`).toBe(true)
      const lasted = anim.revealOff > 0 ? anim.revealOff - anim.revealOn : Infinity
      expect(lasted >= MIN_REVEAL && lasted < Infinity, `THE REVEAL DID NOT RUN: <html> had the ms-revealing class for ${lasted.toFixed(0)} ms in ${name}, want ${MIN_REVEAL} ms or more and over by the window's end`).toBe(true)
      const inside = anim.frames.filter((f) => f.ts > anim.tap && f.ts - anim.tap <= end).length
      // On 60 Hz's interval, not the idle control's: a noisy control would lower the floor. 0.8 of it still holds at 20 ms.
      const roughly = Math.floor((0.8 * end) / HZ60)
      expect(inside, `THE ANIMATION DID NOT RUN: ${name} drew ${inside} frames, want about ${end} ms / 16.7 ms, at least ${roughly}`).toBeGreaterThanOrEqual(roughly)
      // Over, on the new side; then Settings works the shapes out again for the switch where it now sits.
      expect(await drawn(page), `${name}: over by the window's end`).toBe(0)
      expect(await dataMode(page), `${name}: the new side`).toBe(kind === 'pump' ? 'bodybuilding' : null)
      await expect.poll(async () => (await warms()).length, { message: `${name}: the shapes worked out again after the side changed` }).toBeGreaterThan(warmsAtTap)
      await page.waitForTimeout(400)
      return anim
    }

    for (let i = 1; i <= REPEATS; i++) {
      await play(`pump ${i} (warm)`, 'pump')
      await play(`drop ${i} (warm)`, 'drop')
    }
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

    // 3. The display's frame interval: the idle controls' median gap.
    const idleGaps = runs.flatMap((r) => r.idle.frames.slice(1).map((f, i) => f.ts - r.idle.frames[i]!.ts)).sort((a, b) => a - b)
    const interval = idleGaps[Math.floor(idleGaps.length / 2)]!
    expect(interval, 'the display frame interval').toBeGreaterThan(5)
    expect(interval, `CALIBRATION: the display's frame interval is ${interval.toFixed(1)} ms, over ${MAX_INTERVAL} ms (60 Hz is 16.7): every threshold here scales with it, so this run would hold the app to a slower display`).toBeLessThanOrEqual(MAX_INTERVAL)

    /** The frames from the tap to the end, each with the app's script in it, its work, and the gap after the one before. */
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
        return { ts: f.ts, at: f.ts - w.tap, script, work: f.after > 0 ? f.after - f.ts : null, gap: i === 0 ? 0 : f.ts - frames[i - 1]!.ts, late: f.cb - f.ts }
      })
    }
    const cpuSaid = (c: Cpu | null) => (c == null ? 'CPU unknown' : `CPU ${pc(c.busy)} (own ${pc(c.own)}, other ${pc(c.other)}, steal ${pc(c.steal)})`)

    // 4. The control: what the machine did with nothing animating. Noise there fails the run only where it could have
    // helped it pass (below); otherwise it is a warning.
    const busy: string[] = []
    const control: string[] = []
    for (const r of runs) {
      const frames = framesOf(r.idle, r.end)
      expect(r.idle.tap, `${r.name}, idle control: the tap was seen`).toBeGreaterThan(0)
      control.push(`${r.name}: widest gap ${Math.max(...frames.map((f) => f.gap)).toFixed(1)}, latest callback ${Math.max(...frames.map((f) => f.late)).toFixed(1)}, ${cpuSaid(r.idle.cpu)}`)
      if (frames.length < Math.floor(r.end / interval) - 2) busy.push(`${r.name}, idle control: only ${frames.length} frames`)
      // Something else on the machine using the CPU starves the animation long before it shows in an idle page's
      // frames: an idle page on a quiet machine uses a few per cent.
      if (r.idle.cpu != null && r.idle.cpu.busy > BUSY_CPU) busy.push(`${r.name}, idle control: the machine's CPU was ${pc(r.idle.cpu.busy)} in use`)
      for (const f of frames.slice(1)) if (f.gap > interval * 1.5) busy.push(`${r.name}, idle control: a ${f.gap.toFixed(1)} ms gap before ${Math.round(f.at)} ms`)
      for (const f of frames) if (f.late > 16) busy.push(`${r.name}, idle control: a frame's callback ran ${f.late.toFixed(1)} ms late at ${Math.round(f.at)} ms`)
    }

    // 5. Each animation, against the rules; 6. the reveal's frames' work, pooled.
    const bad = new Map<string, string[]>()
    const summary: string[] = []
    const revealWork: number[] = []
    const workOf: Record<string, number[]> = { pump: [], drop: [] }
    for (const r of runs) {
      const frames = framesOf(r.anim, r.end)
      const why: string[] = []
      if (r.anim.tapMs < 0) why.push('the tap was not timed')
      if (r.anim.tapMs > 16) why.push(`the tap took ${r.anim.tapMs.toFixed(1)} ms`)
      // (c) Every frame there: as many as the animation's length allows, less one at each end and the moment's.
      const want = Math.floor(r.end / interval) - 2
      if (frames.length < want) why.push(`${frames.length} frames drawn, want ${want}`)
      if (frames[0]!.at > interval * 1.5) why.push(`the first frame ${frames[0]!.at.toFixed(1)} ms after the tap`)
      if (frames.at(-1)!.at <= r.end - interval * 2) why.push(`the last frame timed at ${frames.at(-1)!.at.toFixed(1)} ms`)
      // (a) The app's own script, every frame.
      for (const f of frames) if (f.script > 16) why.push(`${f.script.toFixed(1)} ms of the app's script in the frame at ${Math.round(f.at)} ms`)
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
        why.push(`a ${gap.toFixed(1)} ms gap before ${Math.round(frames[i]!.at)} ms`)
      }
      if (why.length > 0) bad.set(r.name, why)
      // The reveal's frames: from three intervals after it began to its end.
      const from = r.anim.revealOn + interval * 3
      const to = r.anim.revealOff > 0 ? r.anim.revealOff : Infinity
      const work = frames.filter((f) => f.ts >= from && f.ts < to && f.work != null).map((f) => f.work!)
      revealWork.push(...work)
      workOf[r.kind]!.push(...work)
      summary.push(
        `${r.name}: ${frames.length} frames, script max ${Math.max(...frames.map((f) => f.script)).toFixed(1)} ms, widest gap ${Math.max(...frames.map((f) => f.gap)).toFixed(1)} ms, moment skip ${momentGap.toFixed(1)} ms, tap ${r.anim.tapMs.toFixed(1)} ms, reveal ${Math.round(r.anim.revealOn - r.anim.tap)}-${Math.round(r.anim.revealOff - r.anim.tap)} ms, reveal work median ${median(work).toFixed(1)} ms over ${work.length} frames, ${cpuSaid(r.anim.cpu)}${why.length > 0 ? `; BAD: ${why.join(', ')}` : ''}`,
      )
    }
    const work = median(revealWork)
    const badOf = (kind: 'pump' | 'drop') => runs.filter((r) => r.kind === kind && bad.has(r.name))
    const slowKinds = (['pump', 'drop'] as const).filter((k) => !(median(workOf[k]!) <= WORK_LIMIT_OF[k]))
    const failing = [
      ...(work > WORK_LIMIT ? [`the reveal's work ${work.toFixed(2)} ms a frame`] : []),
    ]
    // When the app's checks fail: was the CPU the animation needed taken by something else?
    const busyDuring = failing.length === 0 ? [] : runs.filter((r) => r.anim.cpu != null && (r.anim.cpu.other > OTHER_CPU || r.anim.cpu.steal > STEAL_CPU)).map((r) => `${r.name}: ${cpuSaid(r.anim.cpu)}`)
    const said = `interval ${interval.toFixed(1)} ms; reveal work median ${work.toFixed(2)} ms over ${revealWork.length} frames (pump ${median(workOf.pump!).toFixed(2)}, drop ${median(workOf.drop!).toFixed(2)}), limits ${WORK_LIMIT} (pump ${WORK_LIMIT_OF.pump}, drop ${WORK_LIMIT_OF.drop}); bad animations: pump ${badOf('pump').length}, drop ${badOf('drop').length}; ${summary.join('; ')}; idle controls: ${control.join('; ')}${cpuCheck ? '' : '; CPU CHECK UNAVAILABLE'}`
    console.log(`mode switch speed: ${said}`)
    // The app's checks failed: on a noisy or busy machine that says nothing about the app.
    expect(failing.length > 0 ? [...busy, ...busyDuring] : [], `MACHINE BUSY: the app's checks failed (${failing.join(', ')}) while the machine was not quiet (below), so this run says nothing about the app; rerun it on a quiet machine (${said})`).toEqual([])
    // Every check of the app's is an upper bound (script, gaps, work, bad animations) or a floor that noise can only
    // fail, so noise can push a run towards failing, never help a slow app pass. Except where the idle controls feed a
    // check, and the cold proof:
    // - the interval: noisy controls can lift their median gap, which widens every gap threshold;
    expect(interval > NOT_60HZ ? busy : [], `MACHINE BUSY: the idle controls were not clean and their interval reads ${interval.toFixed(1)} ms, not 60 Hz's, so the noise may have loosened every threshold; rerun it on a quiet machine (${said})`).toEqual([])
    // - the cold proof: a busy machine during the cold pump's own window can hold back the warm that would have come too
    //   early. (Its idle control comes before the scroll, so noise there cannot; it is a warning like the others.)
    const coldRun = runs.find((r) => r.name === 'pump (cold)')!
    const coldNoise = coldRun.anim.cpu != null && (coldRun.anim.cpu.other > OTHER_CPU || coldRun.anim.cpu.steal > STEAL_CPU) ? [`pump (cold): ${cpuSaid(coldRun.anim.cpu)}`] : []
    expect(coldNoise, `MACHINE BUSY: the cold pump's window was not quiet, so the cold proof (no warm before the animation's end) cannot be trusted; rerun it on a quiet machine (${said})`).toEqual([])
    // Late frames are reported, not failed (the owner's call for 1.15): on the fixed build they came in about one quiet
    // run in nine, and the reveal's work below is what tells a slow build from a fixed one.
    const late = [...bad].filter(([name]) => badOf(runs.find((r) => r.name === name)!.kind).length >= BAD).map(([name, why]) => `${name}: ${why.join(', ')}`)
    if (late.length > 0) {
      const said2 = `LATE FRAMES (warning): ${BAD} or more animations of one kind lost frames: ${late.join('; ')}`
      console.warn(`\n!!! ${said2} !!!\n`)
      test.info().annotations.push({ type: 'warning', description: said2 })
    }
    expect(work, `THE REVEAL'S FRAMES ARE SLOW: the median work per frame over every reveal is ${work.toFixed(2)} ms, over ${WORK_LIMIT} ms (${said})`).toBeLessThanOrEqual(WORK_LIMIT)
    // Each kind's own limit is reported, not failed (the owner's call for 1.15): the drop's median rests on about 34
    // frames a run, and on the fixed build it once read 6.30 against 5.5 on a quiet machine. The pooled limit above
    // stays hard. Follow-up: give the drop a bigger sample and set its limit again.
    if (slowKinds.length > 0) {
      const slow = `SLOW KIND (warning): ${slowKinds.map((k) => `the ${k}: ${median(workOf[k]!).toFixed(2)} ms over ${workOf[k]!.length} frames, limit ${WORK_LIMIT_OF[k]} ms`).join('; ')}`
      console.warn(`\n!!! ${slow} !!!\n`)
      test.info().annotations.push({ type: 'warning', description: slow })
    }
    // The app passed: noise in the idle controls is said, loudly, and the run passes.
    if (busy.length > 0) {
      const noise = `MACHINE NOISE (warning): the app's checks all passed, but the idle controls were not clean: ${busy.join('; ')}. Their CPU: ${runs.map((r) => `${r.name} ${cpuSaid(r.idle.cpu)}`).join('; ')}`
      console.warn(`\n!!! ${noise} !!!\n`)
      test.info().annotations.push({ type: 'warning', description: noise })
    }
  })
})

function median(values: number[]): number {
  const s = [...values].sort((a, b) => a - b)
  return s.length === 0 ? NaN : s[Math.floor(s.length / 2)]!
}
