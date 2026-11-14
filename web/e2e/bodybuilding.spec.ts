import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, readAllTables, repsField, rpeField, tabBar, weightField } from './helpers'

// The bodybuilding side's screens (bodybuilding mode spec §7.3 to §7.7) with a sample story like the owner's mockup
// (column C of .superpowers/brainstorm/bodybuilding/bodybuilding-look-*.png): Wednesday 11 November, incline dumbbell
// press 3 × 8–12 @ 8, last week 26 × 11, 10, 9.
// A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites the committed PNGs in
// docs/design/web/screens/. Look at them after a visual change.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

async function shoot(page: Page, name: string, height: number) {
  await page.setViewportSize({ width: 390, height })
  await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 0))
  await page.evaluate(() => document.fonts.ready)
  mkdirSync(outDir, { recursive: true })
  await page.screenshot({ path: resolve(outDir, `${name}.png`), fullPage: true, animations: 'disabled' })
}

/** Buttons, links, choices and typed figures smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('.shell-main button, .shell-main a[href], .shell-main label.gr-segment, .shell-main label.gr-chip, .shell-main label.gr-stepper-figure'))
      .map((el) => ({ el, box: el.getBoundingClientRect() }))
      .filter(({ box }) => box.width > 0 && (box.width < 47.5 || box.height < 47.5))
      .map(({ el, box }) => `${el.className} ${Math.round(box.width)}×${Math.round(box.height)}`),
  )
const fitsThePhone = (page: Page) => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)

type Story = {
  /** Today's session on the bodybuilding side, with the mockup's two sets of incline dumbbell press done. */
  todaySession?: boolean
  /** The day the bodybuilding side was last left (ISO), for the welcome-back card. */
  bodybuildingLeftOn?: string | null
  /** An open deload from low readiness, the one way a deload comes on this side in stage 1. */
  deload?: boolean
  /**
   * Stage 3: a check-in four weeks ago (side delts lagging) with its open MORE_WORK advice, a session that day, and the
   * cable crossover stuck since, with its open SWAP advice: a check-in is due again. The payloads are the domain's.
   */
  checkInDue?: boolean
  /** Two check-ins (chest lagging four weeks ago, then growing well with lats two days ago), for Progress's ratings. */
  checkIns?: boolean
}

/**
 * Writes the bodybuilding story straight into IndexedDB and reloads, with the app on the bodybuilding side:
 * the bodybuilding plan (Monday, Wednesday, Friday), a powerlifting plan beside it, and two weeks of sessions.
 */
async function seedStory(page: Page, story: Story = {}) {
  await page.evaluate(async ({ todaySession = false, bodybuildingLeftOn = null, deload = false, checkInDue = false, checkIns = false }) => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets', 'settings', 'advice', 'checkIns'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const added = (request: IDBRequest<IDBValidKey>) => new Promise<number>((done) => (request.onsuccess = () => done(request.result as number)))
    const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => done(all.result)
    })
    const idOf = (name: string) => exercises.find((e) => e.name === name)!.id
    // Catalogue exercises get their row with the first plan item or set, as resolveCatalogue writes it.
    const catalogue = async (name: string, muscles: string[]) =>
      added(tx.objectStore('exercises').add({ name, category: 'ACCESSORY', parentLift: null, muscles, isBuiltIn: true, archived: false }))
    const incline = await catalogue('Incline DB press', ['Pecs', 'Front delts', 'Triceps'])
    const pulldown = await catalogue('Lat pulldown', ['Lats', 'Biceps'])
    const crossover = await catalogue('Cable crossover', ['Pecs', 'Front delts'])
    const plan = (weekday: number, exerciseId: number, order: number, reps = 8, repsHigh = 12) =>
      tx.objectStore('planItems').add({ weekday, exerciseId, sets: 3, reps, repsHigh, targetRpe: 8, order, mode: 'BODYBUILDING' })
    ;[incline, pulldown, crossover].forEach((id, order) => plan(3, id, order))
    ;[idOf('Squat'), idOf('RDL'), idOf('Curl')].forEach((id, order) => plan(1, id, order, order === 0 ? 10 : 8, order === 0 ? 15 : 12))
    ;[idOf('DB press'), idOf('Row')].forEach((id, order) => plan(5, id, order))
    tx.objectStore('planItems').add({ weekday: 3, exerciseId: idOf('Squat'), sets: 5, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })

    const session = async (date: string, sets: [number, number, number][]) => {
      const id = await added(tx.objectStore('sessions').add({ date, bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' }))
      for (const [exerciseId, weightKg, reps] of sets) tx.objectStore('sets').add({ sessionId: id, exerciseId, weightKg, reps, rpe: 8, failed: false, missPoint: null, isTopSet: false })
    }
    await session('2026-10-28', [
      [incline, 24, 12],
      [incline, 24, 11],
      [incline, 24, 10],
      [pulldown, 55, 12],
      [pulldown, 55, 11],
      [pulldown, 55, 11],
      [crossover, 15, 12],
      [crossover, 15, 12],
      [crossover, 15, 11],
    ])
    await session('2026-11-04', [
      [incline, 26, 11],
      [incline, 26, 10],
      [incline, 26, 9],
      [pulldown, 55, 12],
      [pulldown, 55, 12],
      [pulldown, 55, 11],
      [crossover, 15, 12],
      [crossover, 15, 11],
      [crossover, 15, 10],
    ])
    if (checkInDue) {
      await session('2026-10-14', [
        [incline, 24, 11],
        [crossover, 15, 12],
        [crossover, 15, 12],
        [crossover, 15, 11],
      ])
      tx.objectStore('checkIns').add({ date: '2026-10-14', lagging: ['SIDE_DELTS'], growingWell: [] })
      const P = 'com.powertracker.domain.model.AdvicePayload'
      tx.objectStore('advice').add({
        type: 'MORE_WORK',
        lift: null,
        createdAt: '2026-10-14',
        reason: 'You marked side delts as lagging. Add Lateral raise on Wednesday, 3 × 8–12 @ 8. That takes it from 0 to 3 sets a week.',
        severity: 0,
        status: 'OPEN',
        payloadJson: `{"type":"${P}.MoreWork","group":"SIDE_DELTS","addSets":[],"newExercise":{"weekday":"WEDNESDAY","name":"Lateral raise","sets":3,"repsLow":8,"repsHigh":12,"rpe":8},"weeklyBefore":0,"weeklyAfter":3}`,
        mode: 'BODYBUILDING',
      })
      tx.objectStore('advice').add({
        type: 'SWAP',
        lift: null,
        createdAt: '2026-11-04',
        reason: "Cable crossover hasn't moved in 3 weeks. Same weight and no more reps since 14 Oct.",
        severity: 0,
        status: 'OPEN',
        payloadJson: `{"type":"${P}.Swap","exerciseId":${crossover},"name":"Cable crossover","group":"CHEST","since":"2026-10-14","alternatives":["DB fly","Seated cable fly","Incline DB fly"]}`,
        mode: 'BODYBUILDING',
      })
    }
    if (checkIns) {
      tx.objectStore('checkIns').add({ date: '2026-10-14', lagging: ['CHEST'], growingWell: [] })
      tx.objectStore('checkIns').add({ date: '2026-11-09', lagging: [], growingWell: ['CHEST', 'LATS'] })
    }
    if (todaySession)
      await session('2026-11-11', [
        [incline, 26, 11],
        [incline, 26, 10],
      ])

    if (deload)
      tx.objectStore('advice').add({
        type: 'DELOAD',
        lift: null,
        createdAt: '2026-11-11',
        reason: 'Signs of fatigue: low readiness. Take a deload week: about 60% of the volume at about 90% of the load.',
        severity: 1,
        status: 'OPEN',
        payloadJson: '{"type":"com.powertracker.domain.model.AdvicePayload.Deload"}',
        mode: 'BODYBUILDING',
      })

    const settings = tx.objectStore('settings')
    settings.put({ key: 'trainingMode', value: 'BODYBUILDING' })
    settings.put({ key: 'bodybuildingTrainingDays', value: [1, 3, 5] })
    settings.put({ key: 'powerliftingLeftOn', value: '2026-11-11' })
    settings.put({ key: 'bodybuildingLeftOn', value: bodybuildingLeftOn })
    await settled
    db.close()
  }, story)
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
}

const header = (page: Page, title: string) => page.locator('header', { has: page.getByRole('heading', { level: 1, name: title }) })

for (const scheme of ['dark', 'light'] as const) {
  test.describe(`the bodybuilding side (${scheme})`, () => {
    test.beforeEach(async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
    })

    test('today: the range, the coach card and Apply', async ({ page }) => {
      await openOnWednesday(page)
      await seedStory(page, { todaySession: true })
      const card = page.locator('.gr-card', { has: page.getByRole('heading', { name: 'Incline DB press' }) })
      await expect(card.getByText('Set 3 of 3')).toBeVisible()
      await expect(card.getByText('26 kg × 8–12 @ 8')).toBeVisible()
      await expect(card.getByText('Last time: 26 × 11, 10, 9. Beat it.')).toBeVisible()
      await expect(page.getByText('Top set')).toHaveCount(0)
      await expect(page.getByRole('button', { name: /^Lat pulldown/ })).toContainText('3 × 8–12 @ 8')
      await expect(header(page, 'Today').getByTestId('mode-label')).toHaveText('BODYBUILDINGBodybuilding mode')
      await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute('content', scheme === 'dark' ? '#070B14' : '#EDF2FA')
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)

      // The mockup's editor: 26 × 10 @ 8 for the third set.
      await expect(weightField(page)).toHaveValue('26')
      await expect(repsField(page)).toHaveValue('10')
      await expect(rpeField(page)).toHaveValue('8')
      await shoot(page, `today-bodybuilding-${scheme}`, 1070)

      // Apply: the weight, and the reps to beat for set 3 (9 last time, so 10).
      await card.getByRole('button', { name: 'Apply' }).click()
      await expect(card.getByText('Last time: 26 × 11, 10, 9. Beat it.')).toHaveCount(0)
      await expect(weightField(page)).toHaveValue('26')
      await expect(repsField(page)).toHaveValue('10')
    })

    test('plan: the side’s own plan and days, rows as ranges', async ({ page }) => {
      await openOnWednesday(page, '#/plan')
      await seedStory(page)
      const wednesday = page.getByRole('region', { name: 'Wednesday' })
      await expect(wednesday.getByRole('listitem')).toHaveText([/Incline DB press\s*3 × 8–12 @ 8/, /Lat pulldown\s*3 × 8–12 @ 8/, /Cable crossover\s*3 × 8–12 @ 8/])
      await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem').first()).toHaveAttribute('aria-label', 'Squat  3 × 10–15 @ 8')
      // The powerlifting squat 5 × 5 on Wednesday is kept for that side, not shown here.
      await expect(page.getByRole('listitem', { name: 'Squat  5 × 5 @ 8' })).toHaveCount(0)
      // The squat, bench and deadlift baselines belong to the powerlifting side.
      await expect(page.getByRole('heading', { name: 'Known bests' })).toHaveCount(0)
      await expect(header(page, 'Plan').getByTestId('mode-label')).toBeVisible()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await shoot(page, `plan-bodybuilding-${scheme}`, 1560)
    })

    test('coach: the welcome back after more than a month away', async ({ page }) => {
      await openOnWednesday(page, '#/coach')
      await seedStory(page, { bodybuildingLeftOn: '2026-10-01', deload: true })
      await expect(page.getByRole('heading', { name: 'Welcome back to the better side.' })).toBeVisible()
      // The deload card speaks of exercises and weights on this side, not lifts and top sets.
      await expect(page.getByText('Deload · All exercises')).toBeVisible()
      await expect(page.getByText('of your last weights')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Resume my plan' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'My goals have changed' })).toBeVisible()
      await expect(header(page, 'Coach').getByTestId('mode-label')).toBeVisible()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await shoot(page, `coach-bodybuilding-${scheme}`, 844)

      // My goals have changed: the card goes and Plan opens; it does not come back.
      await page.getByRole('button', { name: 'My goals have changed' }).click()
      await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
      await tabBar(page).getByRole('link', { name: 'Coach' }).click()
      await expect(page.getByText('Deload · All exercises')).toBeVisible()
      await expect(page.getByRole('heading', { name: 'Welcome back to the better side.' })).toHaveCount(0)
    })

    test('plan builder: the four steps', async ({ page }) => {
      await openOnWednesday(page, '#/plan')
      await seedStory(page)
      await page.getByRole('button', { name: 'Plan options' }).click()
      await page.getByRole('menuitem', { name: 'Rebuild my plan' }).click()
      await expect(page).toHaveURL(/#\/plan\/build$/)
      const builder = header(page, 'Build my plan')

      // Days: the side's own three days to start from.
      await expect(page.getByText('Step 1 of 4')).toBeVisible()
      await expect(page.getByRole('radio', { name: '3 days' })).toBeChecked()
      await expect(builder.getByTestId('mode-label')).toBeVisible()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await shoot(page, `plan-builder-days-bodybuilding-${scheme}`, 844)
      await page.getByRole('button', { name: 'Next' }).click()

      // Exercises: the plans' exercises and the last month's ticked.
      await expect(page.getByRole('heading', { name: 'Which exercises do you do now?' })).toBeVisible()
      await expect(page.getByText('8 chosen')).toBeVisible()
      // Every tick under "Chosen" at the top, then the list with the same rows ticked.
      const chosen = page.getByRole('region', { name: 'Chosen' })
      await expect(chosen.getByRole('listitem')).toHaveCount(8)
      await expect(chosen.getByRole('listitem', { name: 'Incline DB press' }).getByRole('checkbox')).toHaveAttribute('aria-checked', 'true')
      await expect(page.getByRole('listitem', { name: 'Incline DB press' }).last().getByRole('checkbox')).toHaveAttribute('aria-checked', 'true')
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await page.setViewportSize({ width: 390, height: 844 })
      await page.evaluate(() => document.fonts.ready)
      mkdirSync(outDir, { recursive: true })
      await page.screenshot({ path: resolve(outDir, `plan-builder-exercises-bodybuilding-${scheme}.png`), animations: 'disabled' })
      await page.getByRole('button', { name: 'Next' }).click()

      // Body parts: two chosen.
      await expect(page.getByRole('heading', { name: 'Which body parts do you most want to improve?' })).toBeVisible()
      await page.getByRole('checkbox', { name: 'Side delts' }).check()
      await page.getByRole('checkbox', { name: 'Chest' }).check()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await shoot(page, `plan-builder-body-parts-bodybuilding-${scheme}`, 844)
      await page.getByRole('button', { name: 'Next' }).click()

      // The draft.
      await expect(page.getByRole('heading', { name: 'Your draft week' })).toBeVisible()
      await expect(page.getByRole('region', { name: 'Weekly sets' })).toBeVisible()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      const tall = await page.locator('.pb-screen').evaluate((el) => Math.ceil(el.scrollHeight))
      await shoot(page, `plan-builder-draft-bodybuilding-${scheme}`, tall + 90)
    })

    test('coach: the check-in card, more work and a stuck exercise', async ({ page }) => {
      await openOnWednesday(page, '#/coach')
      await seedStory(page, { checkInDue: true })
      await expect(page.getByRole('heading', { name: 'Time for your check-in' })).toBeVisible()
      const more = page.locator('.gr-card', { has: page.getByRole('heading', { name: 'Side delts is lagging' }) })
      await expect(more.getByText('Add Lateral raise on Wednesday, 3 × 8–12 @ 8')).toBeVisible()
      await expect(more.getByText('Side delts: 0 → 3 sets a week')).toBeVisible()
      const stuck = page.locator('.gr-card', { has: page.getByRole('heading', { name: "Cable crossover hasn't moved in 3 weeks" }) })
      await expect(stuck.getByText('Same weight and no more reps since 14 Oct. Another exercise for chest can get it moving.')).toBeVisible()
      await expect(stuck.getByRole('button')).toHaveText(['Swap for DB fly', 'Swap for Seated cable fly', 'Swap for Incline DB fly', 'Dismiss'])
      await expect(page.getByText('2 open suggestions')).toBeVisible()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      const tall = await page.locator('.shell-main .screen').evaluate((el) => Math.ceil(el.scrollHeight))
      await shoot(page, `coach-checkin-bodybuilding-${scheme}`, Math.max(844, tall + 90))
    })

    test('check-in: lagging, growing well, saved', async ({ page }) => {
      await openOnWednesday(page, '#/progress')
      await seedStory(page)
      await page.getByRole('button', { name: 'Check in' }).click()
      await expect(page).toHaveURL(/#\/checkin$/)
      const title = header(page, 'Check-in')
      await expect(title.getByTestId('mode-label')).toBeVisible()
      await expect(page.getByText('Step 1 of 2')).toBeVisible()
      await page.getByRole('checkbox', { name: 'Chest' }).check()
      await page.getByRole('checkbox', { name: 'Side delts' }).check()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await shoot(page, `checkin-lagging-bodybuilding-${scheme}`, 844)
      await page.getByRole('button', { name: 'Next' }).click()
      // The browser's Back steps back to step 1, ticks kept, and Next goes on again.
      await expect(page.getByRole('heading', { name: 'Which are growing well?' })).toBeVisible()
      await page.goBack()
      await expect(page.getByRole('heading', { name: 'Which body parts are lagging?' })).toBeVisible()
      await expect(page.getByRole('checkbox', { name: 'Chest' })).toBeChecked()
      await page.getByRole('button', { name: 'Next' }).click()

      await expect(page.getByRole('heading', { name: 'Which are growing well?' })).toBeVisible()
      await expect(page.getByRole('checkbox', { name: 'Chest' })).toBeDisabled()
      await page.getByRole('checkbox', { name: 'Lats' }).check()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await shoot(page, `checkin-growing-bodybuilding-${scheme}`, 844)
      await page.getByRole('button', { name: 'Save' }).click()

      await expect(page.getByRole('heading', { name: 'Check-in saved' })).toBeVisible()
      const lines = page.getByRole('list', { name: 'Lagging body parts' })
      await expect(lines.getByRole('listitem')).toHaveCount(2)
      await expect(lines.getByRole('listitem').filter({ hasText: 'Chest' })).toContainText('More work suggested on Coach')
      await expect(page.getByRole('button', { name: 'See Coach' })).toBeVisible()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      await shoot(page, `checkin-saved-bodybuilding-${scheme}`, 844)

      // Done goes back to Progress, where it was opened from; the check-in is there.
      await page.getByRole('button', { name: 'Done' }).click()
      await expect(page).toHaveURL(/#\/progress$/)
      const parts = page.getByRole('region', { name: 'Body parts' })
      await expect(parts.getByRole('listitem').filter({ has: page.getByRole('heading', { name: 'Chest' }) })).toContainText('Lagging')
    })

    test('progress: each exercise’s best sets and whether it is going up', async ({ page }) => {
      await openOnWednesday(page, '#/progress')
      await seedStory(page, { checkIns: true })
      // The body parts: each one's latest rating, and the earlier ones, oldest first.
      const parts = page.getByRole('region', { name: 'Body parts' })
      const chest = parts.getByRole('listitem').filter({ has: page.getByRole('heading', { name: 'Chest', exact: true }) })
      await expect(chest).toContainText('Growing well')
      await expect(chest).toContainText('Earlier: Lagging')
      await expect(parts.getByRole('listitem').filter({ has: page.getByRole('heading', { name: 'Upper back', exact: true }) })).toContainText('On track')
      const incline = page.getByRole('button', { name: /^Incline DB press/ })
      await expect(incline).toContainText('24 × 12')
      await expect(incline).toContainText('26 × 11')
      // The best sets are separated by a middle dot, and the row says what it opens.
      await expect(incline).toContainText('24 × 12 · ')
      await expect(incline).toHaveAccessibleName(/^Incline DB press\. 24 × 12, .*\. Going up\. Open history$/)
      await expect(incline.getByRole('img', { name: 'Going up' })).toBeVisible()
      await expect(page.getByRole('button', { name: /^Cable crossover/ }).getByRole('img', { name: 'Not going up' })).toBeVisible()
      await expect(page.getByRole('button', { name: /^Row/ })).toContainText('Not logged yet')
      await expect(header(page, 'Progress').getByTestId('mode-label')).toBeVisible()
      expect(await smallTargets(page)).toEqual([])
      expect(await fitsThePhone(page)).toBe(true)
      // Tall enough for the body parts (stage 3) and the exercise rows under them.
      const tall = await page.locator('.shell-main .screen').evaluate((el) => Math.ceil(el.scrollHeight))
      await shoot(page, `progress-bodybuilding-${scheme}`, Math.max(844, tall + 90))

      // A row opens the exercise's history, which has its sets from both sides.
      await incline.click()
      await expect(page.getByRole('heading', { level: 1, name: 'Incline DB press history' })).toBeVisible()
      await expect(page.getByText('2 sessions')).toBeVisible()
    })
  })
}

test('switching sides swaps the plan, keeps one log, and a day already trained stays on its session', async ({ page }) => {
  await openOnWednesday(page, '#/settings')
  await page.getByRole('switch', { name: 'Bodybuilding mode' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')

  // An empty bodybuilding plan says so; adding gives 3 × 8–12 @ 8.
  await tabBar(page).getByRole('link', { name: 'Plan' }).click()
  await expect(page.getByText('Your bodybuilding plan is empty. Add exercises to each training day.')).toBeVisible()
  await page.getByRole('region', { name: 'Wednesday' }).getByRole('button', { name: 'Add' }).click()
  const picker = page.getByRole('dialog', { name: 'Add to Wednesday' })
  await picker.getByRole('textbox', { name: 'Search exercises' }).fill('Curl')
  await picker.getByRole('listitem', { name: 'Curl' }).getByRole('button').first().click()
  await expect(page.getByRole('listitem', { name: 'Curl  3 × 8–12 @ 8' })).toBeVisible()

  // Today: the range; log a set on this side.
  await tabBar(page).getByRole('link', { name: 'Today' }).click()
  await expect(page.getByText('3 × 8–12 @ 8')).toBeVisible()
  await page.getByRole('button', { name: 'Start session' }).click()
  await weightField(page).fill('12')
  await weightField(page).press('Enter')
  await page.getByRole('button', { name: 'Log set 1' }).click()
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()

  // Back to the powerlifting side on the same day: Today stays on this session, with the line.
  await page.goto('/#/settings')
  await page.getByRole('switch', { name: 'Bodybuilding mode' }).click()
  await expect(page.locator('html')).not.toHaveAttribute('data-mode', 'bodybuilding')
  await tabBar(page).getByRole('link', { name: 'Today' }).click()
  await expect(page.getByText("Today's session is on the bodybuilding side. Powerlifting starts with your next session.")).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Curl' })).toBeVisible()
  await expect(page.getByTestId('mode-label')).toHaveCount(0)

  // Plan is the powerlifting side's again; the log has one session for the day, with its set.
  await tabBar(page).getByRole('link', { name: 'Plan' }).click()
  await expect(page.getByRole('listitem', { name: /^Curl/ })).toHaveCount(0)
  const tables = (await readAllTables(page)) as { sessions: { date: string; mode: string }[]; sets: unknown[] }
  expect(tables.sessions.filter((s) => s.date === '2026-11-11').map((s) => s.mode)).toEqual(['BODYBUILDING'])
  expect(tables.sets).toHaveLength(1)
})

test.describe('the head start', () => {
  // The app's script is held back below; a service worker would serve it from its cache instead.
  test.use({ serviceWorkers: 'block' })

  test('a reload on the bodybuilding side is blue from the first paint', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' })
    await openOnWednesday(page, '#/settings')
    await page.getByRole('switch', { name: 'Bodybuilding mode' }).click()
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')

    // With the app's own code held back, the page as first drawn is already on the bodybuilding side.
    await page.route(/\/(assets\/index-[^/]*\.js|src\/main\.tsx)(\?.*)?$/, (route) => route.abort())
    await page.reload({ waitUntil: 'domcontentloaded' })
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
    expect(await page.evaluate(() => getComputedStyle(document.body).backgroundColor)).toBe('rgb(7, 11, 20)')
    await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute('content', '#070B14')
    await expect(page.getByRole('heading', { level: 1 })).toHaveCount(0)

    // The stored settings stay the truth: back on the powerlifting side, the next load is not blue.
    await page.unroute(/\/(assets\/index-[^/]*\.js|src\/main\.tsx)(\?.*)?$/)
    await page.reload()
    await page.getByRole('switch', { name: 'Bodybuilding mode' }).click()
    await expect(page.locator('html')).not.toHaveAttribute('data-mode', 'bodybuilding')
    await page.route(/\/assets\/index-[^/]*\.js(\?.*)?$/, (route) => route.abort())
    await page.reload({ waitUntil: 'domcontentloaded' })
    await expect(page.locator('html')).not.toHaveAttribute('data-mode', 'bodybuilding')
  })
})

test('the plan builder: four days from three exercises and a priority, a swap, then Plan and Today', async ({ page }) => {
  await openOnWednesday(page, '#/settings')
  await page.getByRole('switch', { name: 'Bodybuilding mode' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
  await tabBar(page).getByRole('link', { name: 'Plan' }).click()
  await page.getByRole('button', { name: 'Build my plan' }).click()

  // Four days: Wednesday, Thursday, Saturday and Sunday.
  await page.getByText('4 days', { exact: true }).click()
  await expect(page.getByRole('radio', { name: '4 days' })).toBeChecked()
  const days = page.getByRole('group', { name: 'Which days?' })
  for (const day of ['Mon', 'Fri']) await days.getByText(day, { exact: true }).click()
  await expect(page.getByRole('button', { name: 'Next' })).toBeDisabled()
  for (const day of ['Thu', 'Sat', 'Sun']) await days.getByText(day, { exact: true }).click()
  await expect(days.getByRole('checkbox', { checked: true })).toHaveCount(4)
  await page.getByRole('button', { name: 'Next' }).click()

  // Nothing logged yet: nothing ticked. Three exercises.
  await expect(page.getByRole('heading', { name: 'Which exercises would you like to do?' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Choose for me' })).toBeVisible()
  const search = page.getByRole('textbox', { name: 'Search exercises' })
  for (const name of ['Hack squat', 'Cable curl', 'T-bar row']) {
    await search.fill(name)
    await page.getByRole('listitem', { name, exact: true }).getByRole('checkbox').click()
  }
  await expect(page.getByText('3 chosen')).toBeVisible()
  await page.getByRole('button', { name: 'Next' }).click()

  await page.getByRole('checkbox', { name: 'Side delts' }).check()
  await page.getByRole('button', { name: 'Next' }).click()

  // The draft: upper on Wednesday, the picks in it, then one swap.
  const wednesday = page.getByRole('region', { name: 'Wednesday' })
  await expect(wednesday.getByRole('heading', { name: 'Wednesday · Upper' })).toBeVisible()
  await expect(page.getByRole('region', { name: 'Thursday' }).getByRole('listitem', { name: /^Hack squat/ })).toBeVisible()
  await expect(wednesday.getByRole('listitem', { name: /^T-bar row/ })).toBeVisible()
  const lead = wednesday.getByRole('listitem').first()
  const leadName = (await lead.getAttribute('aria-label'))!.split('  ')[0]!
  await lead.getByRole('button', { name: `Swap ${leadName}`, exact: true }).click()
  const sheet = page.getByRole('dialog', { name: `Swap ${leadName}` })
  const swapTo = (await sheet.getByRole('listitem').first().getAttribute('aria-label'))!
  await sheet.getByRole('listitem').first().getByRole('button').click()
  await expect(wednesday.getByRole('listitem').first()).toHaveAttribute('aria-label', new RegExp(`^${swapTo}  4 × 8–12 @ 8$`))
  const planned = await wednesday.getByRole('listitem').evaluateAll((rows) => rows.map((r) => r.getAttribute('aria-label')))
  await page.getByRole('button', { name: 'Use this plan' }).click()

  // Plan: the draft as it was accepted, on the four days.
  await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
  await expect(page).toHaveURL(/#\/plan$/)
  await expect(page.getByRole('region', { name: 'Wednesday' }).getByRole('listitem')).toHaveCount(planned.length)
  expect(await page.getByRole('region', { name: 'Wednesday' }).getByRole('listitem').evaluateAll((rows) => rows.map((r) => r.getAttribute('aria-label')))).toEqual(planned)
  for (const day of ['Thursday', 'Saturday', 'Sunday']) await expect(page.getByRole('region', { name: day }).getByRole('listitem').first()).toBeVisible()
  await expect(page.getByRole('region', { name: 'Monday' })).toHaveCount(0)

  // Today is Wednesday, the first training day: its session is the upper day with the swap.
  await tabBar(page).getByRole('link', { name: 'Today' }).click()
  await expect(page.getByText('Wednesday · 9 exercises')).toBeVisible()
  const todayRow = (name: string) => page.getByRole('listitem').filter({ has: page.getByText(name, { exact: true }) })
  await expect(todayRow(swapTo)).toContainText('4 × 8–12 @ 8')
  await expect(todayRow('T-bar row')).toContainText('3 × 8–12 @ 8')
  const tables = (await readAllTables(page)) as { planItems: { mode: string }[]; settings: { key: string; value: unknown }[] }
  expect(tables.planItems.every((p) => p.mode === 'BODYBUILDING')).toBe(true)
  expect(tables.settings.find((s) => s.key === 'bodybuildingTrainingDays')?.value).toEqual([3, 4, 6, 7])
})

test('a check-in story: build a plan, train four weeks, check in, apply the coach’s card, see it on Plan and Progress', async ({ page }) => {
  await openOnWednesday(page, '#/settings')
  await page.getByRole('switch', { name: 'Bodybuilding mode' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')

  // The builder's own plan: three days, its choice of exercises, no priorities.
  await tabBar(page).getByRole('link', { name: 'Plan' }).click()
  await page.getByRole('button', { name: 'Build my plan' }).click()
  await page.getByRole('button', { name: 'Next' }).click()
  await page.getByRole('button', { name: 'Choose for me' }).click()
  await page.getByRole('button', { name: 'Skip' }).click()
  await page.getByRole('button', { name: 'Use this plan' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
  await expect(page).toHaveURL(/#\/plan$/)
  await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem', { name: 'DB press  3 × 8–12 @ 8' })).toBeVisible()

  // No session yet: no check-in is due.
  await tabBar(page).getByRole('link', { name: 'Coach' }).click()
  await expect(page.getByText('No advice right now. Keep logging.')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Time for your check-in' })).toHaveCount(0)

  // Four weeks of training: the first bodybuilding session was 30 days ago.
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
    const press = exercises.find((e) => e.name === 'DB press')!.id
    for (const date of ['2026-10-12', '2026-10-26', '2026-11-09']) {
      const session = tx.objectStore('sessions').add({ date, bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' })
      session.onsuccess = () => tx.objectStore('sets').add({ sessionId: session.result, exerciseId: press, weightKg: 26, reps: 10, rpe: 8, failed: false, missPoint: null, isTopSet: false })
    }
    await settled
    db.close()
  })
  await page.reload()

  // The card on Coach opens the check-in: chest lagging, quads growing well.
  await page.getByRole('button', { name: 'Check in' }).click()
  await page.getByRole('checkbox', { name: 'Chest' }).check()
  await page.getByRole('button', { name: 'Next' }).click()
  await page.getByRole('checkbox', { name: 'Quads' }).check()
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByRole('heading', { name: 'Check-in saved' })).toBeVisible()
  await expect(page.getByRole('list', { name: 'Lagging body parts' }).getByRole('listitem')).toHaveText(['ChestMore work suggested on Coach'])

  // See Coach: the chest card; nothing is due any more. Add to plan.
  await page.getByRole('button', { name: 'See Coach' }).click()
  await expect(page).toHaveURL(/#\/coach$/)
  await expect(page.getByRole('heading', { name: 'Time for your check-in' })).toHaveCount(0)
  const chest = page.locator('.gr-card', { has: page.getByRole('heading', { name: 'Chest is lagging' }) })
  await expect(chest.getByText('One more set of DB press on Monday (3 → 4)')).toBeVisible()
  await expect(chest.getByText('One more set of Incline DB press on Wednesday (3 → 4)')).toBeVisible()
  await chest.getByRole('button', { name: 'Add to plan' }).click()
  await expect(page.getByText('Added a set to 2 exercises')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Chest is lagging' })).toHaveCount(0)

  // Plan has the extra sets.
  await tabBar(page).getByRole('link', { name: 'Plan' }).click()
  await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem', { name: 'DB press  4 × 8–12 @ 8' })).toBeVisible()
  await expect(page.getByRole('region', { name: 'Wednesday' }).getByRole('listitem', { name: 'Incline DB press  4 × 8–12 @ 8' })).toBeVisible()

  // Progress has the ratings.
  await tabBar(page).getByRole('link', { name: 'Progress' }).click()
  const parts = page.getByRole('region', { name: 'Body parts' })
  const row = (name: string) => parts.getByRole('listitem').filter({ has: page.getByRole('heading', { name, exact: true }) })
  await expect(row('Chest')).toContainText('Lagging')
  await expect(row('Quads')).toContainText('Growing well')
  await expect(row('Lats')).toContainText('On track')
  await expect(page.getByText('Your first check-in is four weeks after your first bodybuilding session. You can check in any time.')).toHaveCount(0)
})

test('the check-in: the browser’s Back from step 1 leaves for where it was opened', async ({ page }) => {
  await openOnWednesday(page, '#/progress')
  await seedStory(page)
  await page.getByRole('button', { name: 'Check in' }).click()
  await page.getByRole('button', { name: 'Next' }).click()
  await page.goBack()
  await expect(page.getByRole('heading', { name: 'Which body parts are lagging?' })).toBeVisible()
  await page.goBack()
  await expect(page).toHaveURL(/#\/progress$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Progress' })).toBeVisible()
})
