import { expect, test, type Locator, type Page } from '@playwright/test'
import { tabBar } from './helpers'

// Stage 3, things slide into place (src/ui/viewTransition.ts, src/ui/vt.css): with motion on, Log set, Delete set,
// opening another exercise, Coach's Apply and Dismiss and Plan's moves run in a view transition. These run with motion
// on (every other spec runs still) and check that a transition really ran, that no tap is lost to one, and that the
// header and the tab bar keep still.

test.use({ storageState: { cookies: [], origins: [] } })

test.beforeEach(async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' })
  // Counts the view transitions the page starts, so a test can tell one ran (an aborted one still changes the screen).
  await page.addInitScript(() => {
    const start = document.startViewTransition
    if (typeof start !== 'function') return
    const w = window as unknown as { vtStarted: number }
    w.vtStarted = 0
    document.startViewTransition = function (this: Document, ...args: Parameters<typeof start>) {
      w.vtStarted++
      return start.apply(this, args)
    } as typeof start
  })
})

const started = (page: Page) => page.evaluate(() => (window as unknown as { vtStarted: number }).vtStarted)

/** Whether a view transition is animating now. */
const sliding = () => document.getAnimations().some((a) => (a.effect as KeyframeEffect | null)?.pseudoElement?.startsWith('::view-transition') === true)

/** A finger tap on the middle of `target`, where it is on the page, without waiting for anything first (Playwright's own tap waits until nothing covers it). */
async function tapNow(page: Page, target: Locator) {
  const box = await target.evaluate((el) => {
    const r = el.getBoundingClientRect()
    return { x: r.x + r.width / 2, y: r.y + r.height / 2 }
  })
  await page.touchscreen.tap(box.x, box.y)
}

/** The ids of the logged sets on the open card, top to bottom, from their transition names. */
const setIdsOnScreen = (page: Page) =>
  page.evaluate(() => [...document.querySelectorAll<HTMLElement>('[data-vt]')].map((el) => el.style.getPropertyValue('--vt-name')).filter((name) => name.startsWith('set-')).map((name) => Number(name.slice(4))))

/** Today's logged sets as { id, reps }, read from IndexedDB. */
const storedSets = (page: Page) =>
  page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const read = <T,>(store: string) =>
      new Promise<T[]>((done, fail) => {
        const all = db.transaction(store).objectStore(store).getAll()
        all.onsuccess = () => done(all.result as T[])
        all.onerror = () => fail(all.error)
      })
    const today = (await read<{ id: number; date: string }>('sessions')).find((s) => s.date === '2026-11-11')
    const sets = (await read<{ id: number; sessionId: number; reps: number }>('sets')).filter((s) => s.sessionId === today?.id)
    db.close()
    return sets.map((s) => ({ id: s.id, reps: s.reps }))
  })

/** Today on the sample Wednesday with a plan: squat 5 × 5 with a target from the coach, then bench, deadlift and an accessory. */
async function openWithPlan(page: Page, hash = '#/today') {
  await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
  await page.goto('/#/today')
  await expect(page.getByRole('heading', { name: 'Ready to train?' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.getAttribute('data-motion'))).toBeNull()
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const exercises = await new Promise<{ id: number; name: string; category: string }[]>((done) => {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => done(all.result)
    })
    const idOf = (name: string) => exercises.find((e) => e.name === name)!.id
    const accessory = exercises.find((e) => e.category === 'ACCESSORY')!.id
    ;[idOf('Squat'), idOf('Bench'), idOf('Deadlift'), accessory].forEach((exerciseId, order) =>
      tx.objectStore('planItems').add({ weekday: 3, exerciseId, sets: order === 0 ? 5 : 3, reps: order === 3 ? 10 : 5, targetRpe: 8, order, mode: 'POWERLIFTING' }),
    )
    const session = tx.objectStore('sessions').add({ date: '2026-11-04', bodyweightKg: null, readiness: null, mode: 'POWERLIFTING' })
    session.onsuccess = () => tx.objectStore('sets').add({ sessionId: session.result, exerciseId: idOf('Squat'), weightKg: 150, reps: 5, rpe: 7, failed: false, missPoint: null, isTopSet: false })
    await settled
    db.close()
  })
  await page.goto(`/${hash}`)
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

async function startSession(page: Page) {
  await page.getByRole('button', { name: 'Start session' }).click()
  await expect(page.getByRole('button', { name: 'Log set 1' })).toBeVisible()
  // The card then scrolls into view, smoothly (TodayScreen's afterTransition). tapNow aims where the button is when it
  // looks: aimed mid-scroll, the finger lands on what has scrolled under that point by then. So wait until the card is
  // in view and has kept still for a few frames (the scroll starts a few frames after the tap, so stillness alone is
  // not enough).
  await page.evaluate(async () => {
    const frame = () => new Promise((done) => requestAnimationFrame(done))
    const main = document.querySelector('.shell-main')!
    const log = [...document.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Log set 1'))!
    const card = log.closest('.today-item')!
    let last = ''
    let still = 0
    while (still < 5) {
      await frame()
      const box = card.getBoundingClientRect()
      const view = main.getBoundingClientRect()
      // Where scrollIntoView({ block: 'nearest' }) leaves it: top not cut off, and the whole card in view if it fits.
      const inView = box.top >= view.top - 1 && (box.bottom <= view.bottom + 1 || box.top <= view.top + 1)
      const now = `${box.top}:${main.scrollTop}`
      still = inView && now === last ? still + 1 : 0
      last = now
    }
  })
}

test('three quick Log set taps log three sets, in order, each sliding in once', async ({ page }) => {
  await openWithPlan(page)
  await startSession(page)
  // Every set row put on the page, by its transition name: a row drawn twice would be counted twice.
  await page.evaluate(() => {
    const w = window as unknown as { inserted: string[] }
    w.inserted = []
    new MutationObserver((records) => {
      for (const record of records)
        for (const node of record.addedNodes)
          if (node instanceof HTMLElement)
            for (const el of [node, ...node.querySelectorAll<HTMLElement>('[data-vt]')]) {
              const name = el.style.getPropertyValue('--vt-name')
              if (name.startsWith('set-')) w.inserted.push(name)
            }
    }).observe(document.body, { childList: true, subtree: true })
  })

  await tapNow(page, page.getByRole('button', { name: 'Log set 1' }))
  for (const n of [2, 3]) {
    // The next tap lands while the set before it is still sliding in.
    await page.waitForFunction((count) => document.querySelectorAll('button.gr-set').length === count && (window as unknown as { vtStarted: number }).vtStarted > 0, n - 1)
    await page.waitForFunction(sliding)
    expect(await page.evaluate(sliding)).toBe(true)
    await tapNow(page, page.getByRole('button', { name: `Log set ${n}` }))
  }

  await expect(page.getByRole('button', { name: 'Log set 4' })).toBeVisible()
  const stored = await storedSets(page)
  expect(stored).toHaveLength(3)
  const ids = stored.map(({ id }) => id)
  expect(await setIdsOnScreen(page)).toEqual(ids)
  expect([...ids].sort((a, b) => a - b)).toEqual(ids)
  expect(await page.evaluate(() => (window as unknown as { inserted: string[] }).inserted)).toEqual(ids.map((id) => `set-${id}`))
  expect(await started(page)).toBeGreaterThanOrEqual(3)
})

test('a tap during a slide reaches the page: a stepper steps', async ({ page }) => {
  await openWithPlan(page)
  await startSession(page)
  const reps = page.getByRole('textbox', { name: 'Reps', exact: true })
  await expect(reps).toHaveValue('5')
  await tapNow(page, page.getByRole('button', { name: 'Log set 1' }))
  await page.waitForFunction(sliding)
  await tapNow(page, page.getByRole('button', { name: 'Increase Reps' }))
  await expect(reps).toHaveValue('6')
})

test('a mouse click during a slide lands, once', async ({ page }) => {
  await openWithPlan(page)
  await startSession(page)
  const reps = page.getByRole('textbox', { name: 'Reps', exact: true })
  await expect(reps).toHaveValue('5')
  const at = await page.getByRole('button', { name: 'Increase Reps' }).evaluate((el) => {
    const r = el.getBoundingClientRect()
    return { x: r.x + r.width / 2, y: r.y + r.height / 2 }
  })
  await page.mouse.move(at.x, at.y)
  await page.evaluate(() => [...document.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Log set 1'))!.click())
  await page.waitForFunction(sliding)
  // Where the stepper is now, after the set slid in above it; the mouse goes down without moving first.
  const now = await page.getByRole('button', { name: 'Increase Reps' }).evaluate((el) => {
    const r = el.getBoundingClientRect()
    return { x: r.x + r.width / 2, y: r.y + r.height / 2 }
  })
  await page.mouse.move(now.x, now.y)
  expect(await page.evaluate(sliding)).toBe(true)
  await page.mouse.down()
  await page.mouse.up()
  await expect(reps).toHaveValue('6')
  await page.waitForTimeout(400)
  await expect(reps).toHaveValue('6')
})

test('delete the last set, then log one straight away: the rows end right', async ({ page }) => {
  await openWithPlan(page)
  await startSession(page)
  await page.getByRole('button', { name: 'Log set 1' }).click()
  await expect(page.locator('button.gr-set')).toHaveCount(1)
  await page.locator('button.gr-set').click()
  await page.getByRole('button', { name: 'Delete set' }).click()
  await page.waitForFunction(sliding)
  await tapNow(page, page.getByRole('button', { name: 'Log set 1' }))
  await expect(page.locator('button.gr-set')).toHaveCount(1)
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  const stored = await storedSets(page)
  expect(stored).toHaveLength(1)
  expect(await setIdsOnScreen(page)).toEqual(stored.map(({ id }) => id))
})

test('the header and the tab bar keep still during a slide, and nothing runs past 350 ms', async ({ page }) => {
  await openWithPlan(page)
  await startSession(page)
  const header = page.locator('.shell-main > .screen > .gr-screen-header')
  // Once the card has scrolled into view after the start.
  let last = ''
  await expect
    .poll(async () => {
      const now = JSON.stringify(await header.boundingBox())
      const still = now === last
      last = now
      return still
    }, { intervals: [200] })
    .toBe(true)
  const before = { nav: await tabBar(page).boundingBox(), header: await header.boundingBox() }
  // Samples every frame of the slide after Log set: where the bar and header are, what the browser animates and how.
  const frames = await page.evaluate(async () => {
    const boxOf = (selector: string) => {
      const { x, y, width, height } = document.querySelector(selector)!.getBoundingClientRect()
      return JSON.stringify({ x, y, width, height })
    }
    const log = [...document.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Log set 1'))!
    log.click()
    const seen: { nav: string; header: string; animated: string[]; ends: number[]; oldNav: string; oldHeader: string; navZ: string }[] = []
    for (let i = 0; i < 60; i++) {
      await new Promise((done) => requestAnimationFrame(done))
      const animations = document.getAnimations().filter((a) => (a.effect as KeyframeEffect | null)?.pseudoElement?.startsWith('::view-transition'))
      if (animations.length === 0 && seen.length > 0) break
      if (animations.length === 0) continue
      const root = document.documentElement
      seen.push({
        nav: boxOf('.gr-tabbar'),
        header: boxOf('.shell-main > .screen > .gr-screen-header'),
        animated: animations.map((a) => (a.effect as KeyframeEffect).pseudoElement!),
        ends: animations.map((a) => Number(a.effect!.getComputedTiming().endTime)),
        oldNav: getComputedStyle(root, '::view-transition-old(app-nav)').display,
        oldHeader: getComputedStyle(root, '::view-transition-old(app-header)').display,
        navZ: getComputedStyle(root, '::view-transition-group(app-nav)').zIndex,
      })
    }
    return seen
  })
  expect(frames.length).toBeGreaterThan(3)
  for (const frame of frames) {
    expect(frame.nav).toBe(frames[0]!.nav)
    expect(frame.header).toBe(frames[0]!.header)
    expect(frame.animated.filter((name) => /\((app-nav|app-header|app-dock)\)/.test(name))).toEqual([])
    expect(Math.max(...frame.ends)).toBeLessThanOrEqual(350)
    expect(frame.oldNav).toBe('none')
    expect(frame.oldHeader).toBe('none')
    expect(frame.navZ).toBe('50')
  }
  // The page's own picture: the old one is gone in 80 ms.
  expect(frames[0]!.animated).toContain('::view-transition-old(root)')
  expect(await tabBar(page).boundingBox()).toEqual(before.nav)
  // The header was where it had been before the tap all through the slide (afterwards the card may scroll into view).
  expect(JSON.parse(frames[0]!.header)).toEqual(before.header)
  expect(JSON.parse(frames[0]!.nav)).toEqual(before.nav)
})

test('opening another exercise slides; the picker and the Exercises tab never do', async ({ page }) => {
  await openWithPlan(page)
  await startSession(page)
  const before = await started(page)
  await page.locator('.today-row').filter({ has: page.getByText('Bench', { exact: true }) }).click()
  await expect(page.getByRole('heading', { level: 2, name: 'Bench' })).toBeVisible()
  expect(await started(page)).toBe(before + 1)

  // Picking an exercise slides it in; opening the picker does not move anything.
  await page.waitForFunction(() => !document.getAnimations().some((a) => (a.effect as KeyframeEffect | null)?.pseudoElement?.startsWith('::view-transition')))
  const beforePicker = await started(page)
  await page.getByRole('button', { name: 'Add exercise' }).click()
  const picker = page.getByRole('dialog', { name: 'Add exercise' })
  await expect(picker).toBeVisible()
  expect(await started(page)).toBe(beforePicker)
  await picker.getByRole('textbox', { name: 'Search exercises' }).fill('front squat')
  await picker.getByRole('listitem', { name: 'Front squat', exact: true }).getByRole('button').first().click()
  await expect(page.getByRole('heading', { level: 2, name: 'Front squat' })).toBeVisible()
  expect(await started(page)).toBe(beforePicker + 1)

  // One of the lifter's own, made from the picker, slides in the same way, with its card open.
  await page.waitForFunction(() => !document.getAnimations().some((a) => (a.effect as KeyframeEffect | null)?.pseudoElement?.startsWith('::view-transition')))
  const beforeCustom = await started(page)
  await page.getByRole('button', { name: 'Add exercise' }).click()
  await picker.getByRole('button', { name: 'Create your own' }).click()
  const custom = page.getByRole('dialog', { name: 'Add custom exercise' })
  await custom.getByRole('textbox', { name: 'Name' }).fill('Pin squat')
  await custom.getByRole('button', { name: 'Create' }).click()
  await expect(page.getByRole('heading', { level: 2, name: 'Pin squat' })).toBeVisible()
  expect(await started(page)).toBe(beforeCustom + 1)

  const beforeTab = await started(page)
  await tabBar(page).getByRole('link', { name: 'Exercises' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Exercises' })).toBeVisible()
  await tabBar(page).getByRole('link', { name: 'Plan' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
  expect(await started(page)).toBe(beforeTab)
})

test('Plan: Move down and Delete slide, and the plan is right', async ({ page }) => {
  await openWithPlan(page, '#/plan')
  const wednesday = page.getByRole('region', { name: 'Wednesday' })
  const rows = () => wednesday.getByRole('listitem').evaluateAll((items) => items.map((li) => li.getAttribute('aria-label')))
  await expect.poll(rows).toEqual(['Squat  5 × 5 @ 8', 'Bench  3 × 5 @ 8', 'Deadlift  3 × 5 @ 8', expect.stringContaining('3 × 10 @ 8')])
  const before = await started(page)

  await wednesday.getByRole('button', { name: 'Move down' }).first().click()
  await expect.poll(rows).toEqual(['Bench  3 × 5 @ 8', 'Squat  5 × 5 @ 8', 'Deadlift  3 × 5 @ 8', expect.stringContaining('3 × 10 @ 8')])
  expect(await started(page)).toBe(before + 1)

  await wednesday.getByRole('button', { name: 'More' }).first().click()
  await page.getByRole('menuitem', { name: 'Delete' }).click()
  await expect.poll(rows).toEqual(['Squat  5 × 5 @ 8', 'Deadlift  3 × 5 @ 8', expect.stringContaining('3 × 10 @ 8')])
  expect(await started(page)).toBe(before + 2)

  await page.reload()
  await expect.poll(rows).toEqual(['Squat  5 × 5 @ 8', 'Deadlift  3 × 5 @ 8', expect.stringContaining('3 × 10 @ 8')])
})

test('Coach: an applied and a dismissed card slide away, and the advice is applied', async ({ page }) => {
  await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
  await page.goto('/#/coach')
  await expect(page.getByText('No advice right now. Keep logging.')).toBeVisible()
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'advice'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => done(all.result)
    })
    const idOf = (name: string) => exercises.find((e) => e.name === name)!.id
    ;[idOf('Squat'), idOf('Bench')].forEach((exerciseId, order) => tx.objectStore('planItems').add({ weekday: 3, exerciseId, sets: 5, reps: 5, targetRpe: 8, order, mode: 'POWERLIFTING' }))
    const payload = (kind: string, fields: object = {}) => JSON.stringify({ type: `com.powertracker.domain.model.AdvicePayload.${kind}`, ...fields })
    const advice = (type: string, lift: string | null, severity: number, reason: string, payloadJson: string) =>
      tx.objectStore('advice').add({ type, lift, createdAt: '2026-11-11', reason, severity, status: 'OPEN', payloadJson, mode: 'POWERLIFTING' })
    advice('DELOAD', null, 2, 'Signs of fatigue. Take a deload week.', payload('Deload'))
    advice(
      'STALL',
      'BENCH',
      3,
      "Bench e1RM hasn't improved for 3 weeks.",
      payload('Accessories', { items: [{ exerciseName: 'Close-grip bench', sets: 3, repsLow: 3, repsHigh: 5 }] }),
    )
    await settled
    db.close()
  })
  await page.reload()
  await expect(page.getByText('2 open suggestions')).toBeVisible()
  const before = await started(page)

  await page.getByRole('button', { name: 'Add to plan' }).click()
  await expect(page.getByText('1 open suggestion')).toBeVisible()
  await expect(page.getByText('Added 1 exercise to Wednesday')).toBeVisible()
  expect(await started(page)).toBe(before + 1)

  await page.getByRole('button', { name: 'Dismiss' }).first().click()
  await expect(page.getByText('Nothing open')).toBeVisible()
  expect(await started(page)).toBe(before + 2)

  await page.reload()
  await expect(page.getByText('Nothing open')).toBeVisible()
})
