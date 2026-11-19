import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { BACK, DONE, EXAMPLE, NEXT, PHONE_WIDTH, SKIP, phoneHeight, pictureDescription } from '../src/screens/guide/guideLabels'
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
      // The frame has its phone's shape.
      const frame = (await picture(page).boundingBox())!
      expect(Math.abs(frame.width / frame.height - PHONE_WIDTH / phoneHeight(side))).toBeLessThan(0.01)
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

  const runs: [Side, 'dark' | 'light'][] = [
    ['POWERLIFTING', 'dark'],
    ['BODYBUILDING', 'light'],
  ]
  for (const [side, scheme] of runs) {
    test(`guide_screen4_stepsThroughLoggingASet_onAHeldClock (${sideName(side)}, ${scheme})`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
      await page.clock.install({ time: WEDNESDAY })
      await openGuideOnToday(page, side)
      const now = await page.evaluate(() => Date.now())
      await page.clock.pauseAt(now + 1000)
      // Back and forward again: screen 4 starts its picture afresh, on the held clock.
      await page.getByTestId('guide-back').click()
      await page.getByTestId('guide-next').click()
      const steps = grit.guideSteps('TODAY', side)
      const stepMs = grit.guideStepMs()
      const frames = process.env.GUIDE_FRAMES
      const sample = grit.guideSampleTodaySet(side)
      const liftId = grit.guideSampleExercises(side).find((e) => e.name === sample.exerciseId)!.id
      const item = grit.guideSampleTodayItems(side).find((p) => p.exerciseId === liftId)!
      const drawn = picture(page).locator('.guide-picture-screen')
      const field = (name: string) => drawn.getByRole('textbox', { name, includeHidden: true }).first()
      for (const [i, step] of steps.entries()) {
        await expect(picture(page)).toHaveAttribute('data-step', step)
        // What is drawn: the lift's editor fills field by field, then the set is ticked, then Skip today.
        const at = (s: string) => i >= steps.indexOf(s)
        await expect(field('Weight, kilograms')).toHaveValue(at('TODAY_WEIGHT') ? formatKg(sample.doneWeightKg) : '')
        await expect(field('Reps')).toHaveValue(String(at('TODAY_REPS') ? sample.doneReps : item.reps))
        await expect(field('RPE')).toHaveValue(formatRpe(at('TODAY_RPE') ? sample.doneRpe : item.targetRpe))
        await expect(drawn.locator('.gr-set-done')).toHaveCount(at('TODAY_TICKED') ? 1 : 0)
        await expect(drawn.locator('.today-skipped')).toHaveCount(at('TODAY_SKIP') ? 1 : 0)
        // The page's slide-in has run its course: the frame shows the picture, not the move.
        await page.clock.runFor(300)
        if (frames) await shoot(page, frames, `web-today-${sideName(side)}-${scheme}-step${i + 1}`)
        await page.clock.runFor(stepMs - 300)
      }
      // It holds on the last step.
      await page.clock.runFor(stepMs * 3)
      await expect(picture(page)).toHaveAttribute('data-step', steps.at(-1)!)
    })
  }
})
