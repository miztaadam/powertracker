import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
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

test('guide_systemBack_closesWithSkipped: the browser Back leaves the guide, as Escape does', async ({ page }) => {
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
  await expect(page.getByRole('img', { name: /^Example of the Today screen/ })).toBeVisible()
})

for (const side of SIDES) {
  for (const scheme of ['dark', 'light'] as const) {
    test(`screen 4 at its last step: the ticked set and the skipped line, inside the picture (${sideName(side)}, ${scheme})`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
      await openGuideOnToday(page, side)
      // Motion off (every e2e page): the last step, still.
      await expect(picture(page)).toHaveAttribute('data-step', grit.guideSteps('TODAY', side).at(-1)!)
      await expect(page.getByTestId('guide-example')).toHaveText('Example')
      const sample = grit.guideSampleTodaySet(side)
      const drawn = picture(page).locator('.guide-picture-screen')
      // The ticked set, as its row reads: the sample's numbers, done.
      const done = drawn.locator('.gr-set-done').filter({ hasText: `Set 1: ${sample.doneWeightKg} kg × ${sample.doneReps} at RPE ${sample.doneRpe}, done` })
      await expect(done).toHaveCount(1)
      const skipped = drawn.getByText('Skipped today')
      await expect(skipped).toBeAttached()
      expect(await inside(page, done, picture(page))).toBe(true)
      expect(await inside(page, drawn.locator('.today-skipped'), picture(page))).toBe(true)
      expect(await inside(page, skipped, picture(page))).toBe(true)
      // Nothing reaches past the phone's edges.
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
      await shoot(page, outDir, `guide-today-${sideName(side)}-${scheme}`)
    })
  }
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
      for (const [i, step] of steps.entries()) {
        await expect(picture(page)).toHaveAttribute('data-step', step)
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
