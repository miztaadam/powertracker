import { mkdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { devices, expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { BACK, DONE, EXAMPLE, NEXT, PHONE_HEIGHT, PHONE_WIDTH, SKIP, pictureDescription } from '../src/screens/guide/guideLabels'
import { formatKg, formatRpe } from '../src/ui/components/format'
import { WEDNESDAY, tabBar } from './helpers'

// The guide (1.16, Task 2): its route, its lazy chunk, and screen 4's picture on both sides. A normal run writes the
// screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites the committed PNGs in docs/design/web/screens/.
// GUIDE_FRAMES=<dir> also writes one frame per step of screen 4 with motion on, on a held clock, for the owner's look.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')
const grit = GritDomain.getInstance()
const SIDES = ['POWERLIFTING', 'BODYBUILDING'] as const
type Side = (typeof SIDES)[number]
const sideName = (side: Side) => side.toLowerCase()

/** The guide's lazy chunk (vite names it after the module). */
const GUIDE_CHUNK = /\/assets\/GuideScreen-[^/]*\.js$/

const picture = (page: Page) => page.getByTestId('guide-picture')

/** Opens Grit on the sample Wednesday, on `side`, then the guide on screen 4 through Next (nothing else opens it yet). */
async function openGuideOnToday(page: Page, side: Side) {
  await page.clock.setFixedTime(WEDNESDAY)
  await page.goto('/#/today')
  await expect(tabBar(page)).toBeVisible()
  if (side === 'BODYBUILDING') {
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
  }
  await page.evaluate(() => {
    window.location.hash = '#/guide'
  })
  await expect(page.getByTestId('guide')).toBeVisible()
  const screens = grit.guideScreens()
  for (let i = 0; i < screens.indexOf('TODAY'); i++) await page.getByTestId('guide-next').click()
  await expect(page.getByTestId('guide-title')).toHaveText(grit.guideScreen('TODAY', side).title)
}

async function shoot(page: Page, dir: string, name: string) {
  mkdirSync(dir, { recursive: true })
  await page.evaluate(() => document.fonts.ready)
  await page.screenshot({ path: resolve(dir, `${name}.png`), animations: 'disabled' })
}

type Box = { x: number; y: number; width: number; height: number }

/** Whether box `a` lies wholly inside `b` (to within sub-pixel rounding). */
const within = (a: Box, b: Box) => a.x >= b.x - 0.5 && a.y >= b.y - 0.5 && a.x + a.width <= b.x + b.width + 0.5 && a.y + a.height <= b.y + b.height + 0.5

/** Whether two boxes share any area (touching edges is not overlapping). */
const overlap = (a: Box, b: Box) => a.x < b.x + b.width - 0.5 && b.x < a.x + a.width - 0.5 && a.y < b.y + b.height - 0.5 && b.y < a.y + a.height - 0.5

/** Whether `inner`'s box lies wholly inside `outer`'s (to within sub-pixel rounding). */
async function inside(page: Page, inner: ReturnType<Page['locator']>, outer: ReturnType<Page['locator']>) {
  const a = (await inner.boundingBox())!
  const b = (await outer.boundingBox())!
  return a.x >= b.x - 0.5 && a.y >= b.y - 0.5 && a.x + a.width <= b.x + b.width + 0.5 && a.y + a.height <= b.y + b.height + 0.5
}

test('theGuidesChunk_isNotRequestedBeforeItOpens', async ({ page }) => {
  // The page's own requests: the service worker precaches every chunk for offline use once it installs, but that is
  // the worker's fetch, not the page loading the guide (its requests are not reported on the page).
  const requested: string[] = []
  page.on('request', (request) => requested.push(request.url()))
  await page.clock.setFixedTime(WEDNESDAY)
  await page.goto('/#/today')
  await expect(tabBar(page)).toBeVisible()
  await expect(page.getByRole('heading', { level: 1, name: 'Today' })).toBeVisible()
  expect(requested.filter((url) => GUIDE_CHUNK.test(new URL(url).pathname))).toEqual([])
  await page.evaluate(() => {
    window.location.hash = '#/guide'
  })
  await expect(page.getByTestId('guide')).toBeVisible()
  expect(requested.filter((url) => GUIDE_CHUNK.test(new URL(url).pathname))).toHaveLength(1)
  // Full screen: no tab bar under it.
  await expect(tabBar(page)).toHaveCount(0)
})

// What the exit is (Escape: 'skipped') is pinned by the unit test of that name; here, only that both leave the guide.
// The browser's Back calls no exit of the guide's yet: Task 3 handles it.
test('guide_browserBackAndEscape_leaveTheGuide_forTheScreenItWasOpenedFrom', async ({ page }) => {
  await page.clock.setFixedTime(WEDNESDAY)
  await page.goto('/#/today')
  await expect(tabBar(page)).toBeVisible()
  await page.evaluate(() => {
    window.location.hash = '#/guide'
  })
  await expect(page.getByTestId('guide')).toBeVisible()
  await page.goBack()
  await expect(page.getByTestId('guide')).toHaveCount(0)
  await expect(page.getByRole('heading', { level: 1, name: 'Today' })).toBeVisible()
  await page.goForward()
  await expect(page.getByTestId('guide')).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByTestId('guide')).toHaveCount(0)
  await expect(page.getByRole('heading', { level: 1, name: 'Today' })).toBeVisible()
})

test('guide_showsEightDots_andMovesWithNextBackAndSwipe, a swipe starting on the picture included', async ({ page }) => {
  await openGuideOnToday(page, 'POWERLIFTING')
  await expect(page.getByTestId('guide-dots').locator('.guide-dot')).toHaveCount(8)
  const box = (await picture(page).boundingBox())!
  const y = box.y + box.height / 2
  await page.mouse.move(box.x + box.width * 0.9, y)
  await page.mouse.down()
  await page.mouse.move(box.x + box.width * 0.1, y, { steps: 5 })
  await page.mouse.up()
  await expect(page.getByTestId('guide-title')).toHaveText(grit.guideScreen('TARGETS', 'POWERLIFTING').title)
  await page.getByTestId('guide-back').click()
  await expect(page.getByTestId('guide-title')).toHaveText(grit.guideScreen('TODAY', 'POWERLIFTING').title)
})

test('guide_pictureIsNotInteractive: taps across the picture change nothing, open nothing and scroll nothing', async ({ page }) => {
  await openGuideOnToday(page, 'POWERLIFTING')
  const last = grit.guideSteps('TODAY', 'POWERLIFTING').at(-1)!
  await expect(picture(page)).toHaveAttribute('data-step', last)
  const box = (await picture(page).boundingBox())!
  for (let x = 1; x < 10; x++) for (let y = 1; y < 10; y++) await page.mouse.click(box.x + (box.width * x) / 10, box.y + (box.height * y) / 10)
  await expect(picture(page)).toHaveAttribute('data-step', last)
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.getByTestId('guide-title')).toHaveText(grit.guideScreen('TODAY', 'POWERLIFTING').title)
  // Nothing of the drawn screen reaches the accessibility tree, and nothing in it took focus or scrolled.
  await expect(page.getByTestId('guide').getByRole('button', { name: /Log set/ })).toHaveCount(0)
  expect(await page.evaluate(() => document.activeElement?.closest('.guide-picture') == null)).toBe(true)
  expect(await picture(page).evaluate((el) => [el.scrollTop, el.scrollLeft, window.scrollY])).toEqual([0, 0, 0])
  await expect(page.getByRole('img', { name: pictureDescription('TODAY') })).toBeVisible()
})

test('the picture is never painted at full size: from its first frame it sits above the dots', async ({ page }) => {
  await openGuideOnToday(page, 'BODYBUILDING')
  await page.getByTestId('guide-back').click()
  // Every frame from the tap on Next until the picture has been up a while: where its frame ends, and where the dots start.
  const watching = page.evaluate(
    () =>
      new Promise<string[]>((done) => {
        const seen: string[] = []
        const started = performance.now()
        const frame = () => {
          const picture = document.querySelector<HTMLElement>('[data-testid="guide-picture"][data-step]')
          const dots = document.querySelector<HTMLElement>('[data-testid="guide-dots"]')
          if (picture && dots && getComputedStyle(picture).visibility !== 'hidden') {
            const bottom = picture.getBoundingClientRect().bottom
            const top = dots.getBoundingClientRect().top
            if (bottom > top + 0.5) seen.push(`${Math.round(performance.now() - started)} ms: ${bottom} over ${top}`)
          }
          if (performance.now() - started < 600) requestAnimationFrame(frame)
          else done(seen)
        }
        requestAnimationFrame(frame)
      }),
  )
  await page.getByTestId('guide-next').click()
  expect(await watching).toEqual([])
})

for (const side of SIDES) {
  for (const scheme of ['dark', 'light'] as const) {
    test(`screen 4 at its last step: the ticked set and the skipped line, inside the picture (${sideName(side)}, ${scheme})`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
      await openGuideOnToday(page, side)
      // Motion off (every e2e page): the last step, still.
      await expect(picture(page)).toHaveAttribute('data-step', grit.guideSteps('TODAY', side).at(-1)!)
      await expect(page.getByTestId('guide-example')).toHaveText(EXAMPLE)
      const sample = grit.guideSampleTodaySet(side)
      const drawn = picture(page).locator('.guide-picture-screen')
      // The ticked set: one done row, with the sample's numbers as the app writes them.
      const done = drawn.locator('.gr-set-done')
      await expect(done).toHaveCount(1)
      for (const part of [formatKg(sample.doneWeightKg), `× ${sample.doneReps}`, formatRpe(sample.doneRpe)]) await expect(done).toContainText(part)
      // The skipped line names the day's last exercise but the lift (screen 4's day is the domain's).
      const exercises = grit.guideSampleExercises(side)
      const liftId = exercises.find((e) => e.name === sample.exerciseId)!.id
      const skippedItem = grit.guideSampleTodayItems(side).filter((p) => p.exerciseId !== liftId).at(-1)!
      const skipped = drawn.locator('.today-skipped')
      await expect(skipped).toContainText(exercises.find((e) => e.id === skippedItem.exerciseId)!.name)
      expect(await inside(page, done, picture(page))).toBe(true)
      expect(await inside(page, skipped, picture(page))).toBe(true)
      // The frame has its phone's shape: one phone for both sides.
      const frame = (await picture(page).boundingBox())!
      expect(Math.abs(frame.width / frame.height - PHONE_WIDTH / PHONE_HEIGHT)).toBeLessThan(0.01)
      // The skipped line ends with a wrapped line of its text to spare above the phone's foot (real fonts, as drawn).
      const scale = frame.height / PHONE_HEIGHT
      const line = await skipped.locator('.today-skipped-row').first().evaluate((el) => parseFloat(getComputedStyle(el).lineHeight) || el.getBoundingClientRect().height)
      const foot = (await skipped.boundingBox())!
      expect(foot.y + foot.height, `${side}: the skipped line's foot, with a ${line} px line to spare`).toBeLessThanOrEqual(frame.y + frame.height - line * scale + 0.5)
      // Nothing reaches past the phone's edges.
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
      await shoot(page, outDir, `guide-today-${sideName(side)}-${scheme}`)
    })
  }
}

// What 200% text does to a phone: everything is laid out in half its width and height (as pictures.spec does). The
// iPhone the e2e runs as (390 × 664), and the narrowest phones (320 × 568).
for (const [width, height] of [
  [390, 664],
  [320, 568],
] as const) {
  // The text box scrolls when it is taller than its share; across, it wraps and is never cut.
  test(`guide_atTwiceTheTextSize_onA${width}pxPhone_keepsItsControlsWhole_andCutsNoText`, async ({ page }) => {
    await page.clock.setFixedTime(WEDNESDAY)
    await page.setViewportSize({ width: Math.floor(width / 2), height: Math.floor(height / 2) })
    await page.goto('/#/today')
    await expect(tabBar(page)).toBeVisible()
    await page.evaluate(() => {
      window.location.hash = '#/guide'
    })
    await expect(page.getByTestId('guide')).toBeVisible()
    const screens = grit.guideScreens()
    for (const [i, id] of screens.entries()) {
      await expect(page.getByTestId('guide-title')).toHaveText(grit.guideScreen(id, 'POWERLIFTING').title)
      const controls: [string, string][] = [
        ['guide-skip', SKIP],
        ['guide-back', BACK],
        ['guide-next', i === screens.length - 1 ? DONE : NEXT],
      ]
      const viewport = page.viewportSize()!
      const screenBox = { x: 0, y: 0, width: viewport.width, height: viewport.height }
      const boxes: Box[] = []
      for (const [testId, label] of controls) {
        const control = page.getByTestId(testId)
        const text = control.locator('.gr-btn-text')
        // The label reads in full: not cut to "Ba…", and drawn inside its own button, on screen.
        await expect(text).toHaveText(label)
        const [scrollWidth, clientWidth] = await text.evaluate((el) => [el.scrollWidth, el.clientWidth])
        expect(scrollWidth, `${id}: ${label} is cut`).toBeLessThanOrEqual(clientWidth!)
        const button = (await control.boundingBox())!
        const words = (await text.boundingBox())!
        expect(within(words, button), `${id}: ${label} runs out of its button`).toBe(true)
        expect(within(button, screenBox), `${id}: ${label} is off screen`).toBe(true)
        boxes.push(button)
      }
      // No control covers another.
      for (const [a, b] of [
        [0, 1],
        [0, 2],
        [1, 2],
      ] as const)
        expect(overlap(boxes[a]!, boxes[b]!), `${id}: ${controls[a]![1]} and ${controls[b]![1]} overlap`).toBe(false)
      // The title and lines wrap: nothing runs past the side of the page.
      const cut = await page.locator('.guide-title, .guide-line').evaluateAll((els) => els.filter((el) => el.scrollWidth > el.clientWidth + 0.5).map((el) => el.textContent))
      expect(cut, id).toEqual([])
      // Down the page, the text comes first: the picture gives up its room before the text has to scroll.
      const room = await page.evaluate(() => {
        const text = document.querySelector<HTMLElement>('.guide-text')!
        const art = document.querySelector<HTMLElement>('.guide-art')!
        const style = getComputedStyle(art)
        return {
          scrolls: text.scrollHeight > text.clientHeight + 1,
          scrollable: getComputedStyle(text).overflowY === 'auto',
          art: art.getBoundingClientRect().height - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom),
        }
      })
      expect(room.scrollable, id).toBe(true)
      if (room.scrolls) expect(room.art, `${id}: the text scrolls while the picture keeps ${room.art} px`).toBeLessThanOrEqual(1)
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
      if (i < screens.length - 1) await page.getByTestId('guide-next').click()
    }
  })
}

test.describe('with motion on', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  /**
   * Opens screen 4 on a held clock, its picture starting afresh at time 0 (Back and Next again, the clock paused), and
   * gives a way to move the picture's time to `ms` from its start.
   */
  async function heldToday(page: Page, side: Side) {
    await page.clock.install({ time: WEDNESDAY })
    await openGuideOnToday(page, side)
    const now = await page.evaluate(() => Date.now())
    await page.clock.pauseAt(now + 1000)
    await page.getByTestId('guide-back').click()
    await page.getByTestId('guide-next').click()
    await expect(picture(page)).toHaveAttribute('data-step', 'TODAY_EMPTY')
    let pos = 0
    return async (ms: number) => {
      if (ms > pos) await page.clock.runFor(ms - pos)
      pos = Math.max(pos, ms)
    }
  }

  const runs: [Side, 'dark' | 'light'][] = [
    ['POWERLIFTING', 'dark'],
    ['BODYBUILDING', 'light'],
  ]
  for (const [side, scheme] of runs) {
    test(`guide_screen4_stepsThroughLoggingASet_onAHeldClock (${sideName(side)}, ${scheme})`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
      const at = await heldToday(page, side)
      const steps = grit.guideSteps('TODAY', side)
      const frames = process.env.GUIDE_FRAMES
      const sample = grit.guideSampleTodaySet(side)
      const liftId = grit.guideSampleExercises(side).find((e) => e.name === sample.exerciseId)!.id
      const item = grit.guideSampleTodayItems(side).find((p) => p.exerciseId === liftId)!
      const drawn = picture(page).locator('.guide-picture-screen')
      const field = (name: string) => drawn.getByRole('textbox', { name, includeHidden: true }).first()
      for (const [i, step] of steps.entries()) {
        // Each step once its move has ended (and the page's own slide-in has run its course).
        await at(Math.max(300, grit.guideSettledMs(i) + 20))
        await expect(picture(page)).toHaveAttribute('data-step', step)
        await expect(picture(page)).toHaveAttribute('data-progress', '1')
        // What is drawn: the lift's editor fills field by field, then the set is ticked, then Skip today.
        const reached = (s: string) => i >= steps.indexOf(s)
        await expect(field('Weight, kilograms')).toHaveValue(reached('TODAY_WEIGHT') ? formatKg(sample.doneWeightKg) : '')
        await expect(field('Reps')).toHaveValue(String(reached('TODAY_REPS') ? sample.doneReps : item.reps))
        await expect(field('RPE')).toHaveValue(formatRpe(reached('TODAY_RPE') ? sample.doneRpe : item.targetRpe))
        await expect(drawn.locator('.gr-set-done')).toHaveCount(reached('TODAY_TICKED') ? 1 : 0)
        await expect(drawn.locator('.today-skipped')).toHaveCount(reached('TODAY_SKIP') ? 1 : 0)
        // Settled: nothing of the guide's own motion is left on the rows.
        expect(await picture(page).evaluate((el) => el.getAnimations({ subtree: true }).filter((a) => !(a instanceof CSSAnimation || a instanceof CSSTransition)).length)).toBe(0)
        await expect(picture(page).locator('.guide-flip-ghosts > *')).toHaveCount(0)
        if (frames) await shoot(page, frames, `web-today-${sideName(side)}-${scheme}-step${i + 1}`)
      }
      // It holds on the last step.
      await at(grit.guideDoneMs(steps.length) + grit.guideStepMs() * 3)
      await expect(picture(page)).toHaveAttribute('data-step', steps.at(-1)!)
    })
  }

  test('guide_today_skippedRow_slidesAndTheRestCloseUp', async ({ page }) => {
    const side = 'POWERLIFTING'
    const at = await heldToday(page, side)
    const steps = grit.guideSteps('TODAY', side)
    const skip = steps.indexOf('TODAY_SKIP')
    const drawn = picture(page).locator('.guide-picture-screen')
    const add = drawn.locator('.today-add')
    await at(grit.guideSettledMs(skip - 1))
    await expect(picture(page)).toHaveAttribute('data-step', steps[skip - 1]!)
    const before = (await add.boundingBox())!
    await expect(drawn.locator('.today-item')).toHaveCount(grit.guideSampleTodayItems(side).length)
    // Mid-move: Add exercise slides up from where it was, the skipped row fades where it was, the line fades in.
    await at(grit.guideStepStartMs(skip) + grit.guideMoveMs() / 4)
    await expect(picture(page)).toHaveAttribute('data-step', 'TODAY_SKIP')
    await expect(drawn.locator('.today-item')).toHaveCount(grit.guideSampleTodayItems(side).length - 1)
    const motion = await page.evaluate(() => {
      const screen = document.querySelector('.guide-picture-screen')!
      const own = (el: Element | null) => (el?.getAnimations() ?? []).filter((a) => !(a instanceof CSSAnimation || a instanceof CSSTransition)).map((a) => ({
        time: Number(a.currentTime),
        duration: Number(a.effect!.getTiming().duration),
        state: a.playState,
        properties: (a.effect as KeyframeEffect).getKeyframes().flatMap((k) => Object.keys(k).filter((p) => !['offset', 'easing', 'composite', 'computedOffset'].includes(p))),
      }))
      return {
        add: own(screen.querySelector('.today-add')),
        skipped: own(screen.querySelector('.today-skipped')),
        ghosts: [...document.querySelectorAll('.guide-flip-ghosts > *')].map((g) => ({ text: g.textContent, animations: own(g) })),
      }
    })
    expect(motion.add).toHaveLength(1)
    expect(motion.add[0]!.properties).toEqual(['transform', 'transform'])
    expect(motion.add[0]!.time).toBeGreaterThan(0)
    expect(motion.add[0]!.time).toBeLessThan(motion.add[0]!.duration)
    expect(motion.add[0]!.duration).toBe(grit.guideMoveMs())
    expect(motion.skipped.map((a) => a.properties)).toEqual([['opacity', 'opacity']])
    const exercises = grit.guideSampleExercises(side)
    const liftId = exercises.find((e) => e.name === grit.guideSampleTodaySet(side).exerciseId)!.id
    const skippedName = exercises.find((e) => e.id === grit.guideSampleTodayItems(side).filter((p) => p.exerciseId !== liftId).at(-1)!.exerciseId)!.name
    expect(motion.ghosts).toHaveLength(1)
    expect(motion.ghosts[0]!.text).toContain(skippedName)
    expect(motion.ghosts[0]!.animations.map((a) => a.properties)).toEqual([['opacity', 'opacity']])
    // Where it is drawn: on the way from its old box to its new, as far as the move's curve says.
    const mid = (await add.boundingBox())!
    const progress = grit.guideEase(motion.add[0]!.time / motion.add[0]!.duration)
    await at(grit.guideSettledMs(skip) + 50)
    const after = (await add.boundingBox())!
    expect(after.y).toBeLessThan(before.y - 10)
    expect(Math.abs(mid.y - (before.y + (after.y - before.y) * progress))).toBeLessThan(1.5)
    expect(Math.abs(mid.x - after.x)).toBeLessThan(0.5)
    // Settled: no motion left, no ghost.
    expect(await picture(page).evaluate((el) => el.getAnimations({ subtree: true }).filter((a) => !(a instanceof CSSAnimation || a instanceof CSSTransition)).length)).toBe(0)
    await expect(picture(page).locator('.guide-flip-ghosts > *')).toHaveCount(0)
  })

  test('guide_replayOrPageReturn_restartsWithoutPlayingBackwards', async ({ page }) => {
    const at = await heldToday(page, 'POWERLIFTING')
    const steps = grit.guideSteps('TODAY', 'POWERLIFTING')
    await at(grit.guideDoneMs(steps.length) + 100)
    await expect(picture(page)).toHaveAttribute('data-step', 'TODAY_SKIP')
    await page.getByTestId('guide-next').click()
    await expect(page.getByTestId('guide-title')).toHaveText(grit.guideScreen('TARGETS', 'POWERLIFTING').title)
    await page.getByTestId('guide-back').click()
    // At once on its first step, with nothing of the guide's motion on it: no row moving back from where it was.
    await expect(picture(page)).toHaveAttribute('data-step', 'TODAY_EMPTY')
    expect(await picture(page).evaluate((el) => el.getAnimations({ subtree: true }).filter((a) => !(a instanceof CSSAnimation || a instanceof CSSTransition)).length)).toBe(0)
    await expect(picture(page).locator('.guide-flip-ghosts > *')).toHaveCount(0)
    await expect(picture(page).locator('.gr-set-done')).toHaveCount(0)
  })

  test('guideFlip_readsLayoutOncePerStepChange_neverPerFrame', async ({ page }) => {
    // Every box read inside the picture's phone, counted.
    await page.addInitScript(() => {
      const read = Element.prototype.getBoundingClientRect
      const w = window as unknown as { __guideReads: number }
      w.__guideReads = 0
      Element.prototype.getBoundingClientRect = function (this: Element) {
        if (this.closest('.guide-phone') != null) w.__guideReads += 1
        return read.call(this)
      }
    })
    const at = await heldToday(page, 'POWERLIFTING')
    const reads = () => page.evaluate(() => (window as unknown as { __guideReads: number }).__guideReads)
    const steps = grit.guideSteps('TODAY', 'POWERLIFTING')
    for (let i = 1; i < steps.length; i++) {
      await at(grit.guideStepStartMs(i) - 30)
      const held = await reads()
      // The step change: its boxes read before and after the commit, once each.
      await at(grit.guideStepStartMs(i) + 1)
      await expect(picture(page)).toHaveAttribute('data-step', steps[i]!)
      const change = (await reads()) - held
      expect(change, `${steps[i]}: reads at the step change`).toBeGreaterThan(0)
      // Then the move's frames, and the hold: no layout read at all.
      const moving = await reads()
      await at(grit.guideSettledMs(i) + 100)
      expect((await reads()) - moving, `${steps[i]}: reads during the move`).toBe(0)
      const targets = await page.locator('.guide-picture-screen .today-item, .guide-picture-screen .today-add, .guide-picture-screen .today-skipped').count()
      expect(change, `${steps[i]}: one pass before and one after`).toBeLessThanOrEqual(2 * (targets + 20))
    }
  })

  for (const side of SIDES) {
    test(`the weight rolls in, its plates cross-fading with the note from its first frame: nothing under the editor moves on the way (${sideName(side)})`, async ({ page }) => {
      const at = await heldToday(page, side)
      const weight = grit.guideSteps('TODAY', side).indexOf('TODAY_WEIGHT')
      const card = picture(page).locator('.guide-picture-screen .today-item').first()
      const seen: { kg: string; plates: string; log: number }[] = []
      for (let into = 1; into <= grit.guideMoveMs(); into += 40) {
        await at(grit.guideStepStartMs(weight) + into)
        // "No weight yet" fades out where it was as the plates fade in: never a frame with neither.
        if (into < grit.guideFadeMs()) await expect(picture(page).locator('.guide-flip-ghosts > .gr-plates-note'), `${side} ${into} ms`).toHaveCount(1)
        seen.push(
          await card.evaluate((el) => ({
            kg: el.querySelector<HTMLInputElement>('input[aria-label="Weight, kilograms"], .today-stepper-weight input')?.value ?? '',
            plates: el.querySelector('.gr-plates')?.className ?? 'none',
            log: (el.querySelector('.today-log') as HTMLElement).offsetTop,
          })),
        )
      }
      expect(new Set(seen.map((s) => s.kg)).size, `${side}: the weight rolls`).toBeGreaterThan(3)
      expect(new Set(seen.map((s) => s.plates)), `${side}: ${seen.map((s) => s.plates)}`).toEqual(new Set([seen[0]!.plates]))
      expect(seen[0]!.plates).not.toContain('gr-plates-note')
      expect(new Set(seen.map((s) => s.log)), `${side}: the log row's place`).toEqual(new Set([seen[0]!.log]))
    })
  }
})

// For the owner (GUIDE_VIDEO=<dir>): screen 4 playing in real time with motion on, from its first step until a second
// after its last has settled, recorded at the e2e phone's full screen (390 × 844). `<name>.json` beside each video says where in it
// screen 4 opened (its page's own slide-in takes the next quarter second), to trim from.
test.describe('for the owner: screen 4 in real time', () => {
  const dir = process.env.GUIDE_VIDEO
  test.skip(dir == null, 'GUIDE_VIDEO is not set')
  const runs: [Side, 'dark' | 'light'][] = [
    ['POWERLIFTING', 'dark'],
    ['BODYBUILDING', 'light'],
  ]
  for (const [side, scheme] of runs) {
    test(`screen 4 video (${sideName(side)}, ${scheme})`, async ({ browser, baseURL }) => {
      // Its own context, recording from its first frame: the e2e phone at its full screen height, so the picture is drawn
      // as large as on the phone itself; motion on (no stored "off").
      const { defaultBrowserType: _, ...phone } = devices['iPhone 13']
      const size = { width: 390, height: 844 }
      const context = await browser.newContext({ ...phone, viewport: size, baseURL, storageState: { cookies: [], origins: [] }, recordVideo: { dir: resolve(dir!, 'raw'), size } })
      const page = await context.newPage()
      const started = Date.now()
      await page.emulateMedia({ colorScheme: scheme })
      await openGuideOnToday(page, side)
      const opened = (Date.now() - started) / 1000
      await expect(picture(page)).toHaveAttribute('data-step', 'TODAY_EMPTY')
      await page.waitForTimeout(grit.guideDoneMs(grit.guideSteps('TODAY', side).length) + 1500)
      await expect(picture(page)).toHaveAttribute('data-step', 'TODAY_SKIP')
      await page.close()
      await context.close()
      const name = `web-guide-today-${sideName(side)}-${scheme}`
      mkdirSync(dir!, { recursive: true })
      await page.video()!.saveAs(resolve(dir!, `${name}.webm`))
      writeFileSync(resolve(dir!, `${name}.json`), JSON.stringify({ screen4OpenedAtSeconds: opened }))
    })
  }
})
