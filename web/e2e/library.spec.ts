import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { openOnWednesday, readAllTables, tap, typeInto, weightField } from './helpers'

// The exercise library on the narrowest iPhone: the picker stays fast with the whole catalogue,
// nothing overflows, every target is a full 48 px, and the form guide can be read to its last line
// at 130% zoom.

const grit = GritDomain.getInstance()
const catalogue = grit.library()
const SAFETY = 'Stop if you feel sharp pain. For an injury or a condition, check with a doctor or physiotherapist.'
const NO_MATCH = 'No exercises match. Try fewer filters, or create your own.'

/** How long the picker may take to settle after it opens, after a keystroke and after a filter change. */
const SETTLE_MS = 100

const narrowPhone = { width: 375, height: 812 }
test.use({ viewport: narrowPhone })

type Row = { id?: number; name: string; isBuiltIn: boolean; archived: boolean }
type StoredSet = { exerciseId: number; weightKg: number }

/** Changes the exercises table straight in IndexedDB, then reloads. */
async function editExercises(page: Page, change: { rename?: Record<string, string>; add?: Omit<Row, 'id'>[] }) {
  await page.evaluate(async ({ rename = {}, add = [] }) => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction('exercises', 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const store = tx.objectStore('exercises')
    const all = store.getAll()
    all.onsuccess = () => {
      for (const row of all.result as { id: number; name: string }[]) if (rename[row.name] != null) store.put({ ...row, name: rename[row.name], isBuiltIn: false })
      for (const row of add) store.add({ category: 'ACCESSORY', parentLift: null, muscles: ['Quads', 'Glutes', 'Hamstrings', 'Calves'], ...row })
    }
    await settled
    db.close()
  }, change)
  await page.reload()
}

const picker = (page: Page) => page.getByRole('dialog', { name: /^(Add to |Add exercise)/ })
const pickerRow = (page: Page, name: string) => picker(page).getByRole('listitem', { name, exact: true })
const shownNames = (page: Page) => picker(page).getByRole('listitem').evaluateAll((rows) => rows.map((row) => row.getAttribute('aria-label')))

async function openPlanPicker(page: Page) {
  await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
  await expect(picker(page)).toBeVisible()
}

/**
 * Does one thing to the picker inside the page and measures, on the page's own clock, how long it
 * takes until the result has been drawn: the change, its render, and two frames. Returns the time
 * and the rows then on screen.
 */
function settle(page: Page, action: { open: true } | { type: string } | { chip: string } | { muscle: string } | { allMuscles: true }) {
  return page.evaluate(async (action) => {
    const frame = () => new Promise<void>((done) => requestAnimationFrame(() => done()))
    const started = performance.now()
    if ('open' in action) {
      document.querySelector<HTMLElement>('section[aria-label="Monday"] button')!.click()
    } else if ('type' in action) {
      const input = document.querySelector<HTMLInputElement>('.gr-dialog-screen input[type="text"]')!
      // What a keystroke does: the field's value changes and it fires `input`.
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(input, action.type)
      input.dispatchEvent(new Event('input', { bubbles: true }))
    } else if ('muscle' in action) {
      Array.from(document.querySelectorAll<HTMLElement>('.lib-muscle')).find((tile) => tile.textContent === action.muscle)!.click()
    } else if ('allMuscles' in action) {
      document.querySelector<HTMLElement>('.lib-muscle-back button')!.click()
    } else {
      const chip = Array.from(document.querySelectorAll<HTMLElement>('.lib-chips .gr-chip')).find((c) => c.textContent === action.chip)!
      chip.querySelector('input')!.click()
    }
    await frame()
    await frame()
    const ms = performance.now() - started
    return {
      ms,
      rows: Array.from(document.querySelectorAll('.lib-row')).map((row) => row.getAttribute('aria-label')),
      tiles: Array.from(document.querySelectorAll('.lib-muscle')).map((tile) => tile.textContent),
    }
  }, action)
}

/** Buttons, links and toggle chips smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page, within = 'body') =>
  page.evaluate(
    (within) =>
      Array.from(document.querySelectorAll<HTMLElement>(`${within} button, ${within} a[href], ${within} .gr-chip`))
        .map((el) => ({ el, box: el.getBoundingClientRect() }))
        .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
        .map(({ el, box }) => `${el.className} ${el.getAttribute('aria-label') ?? el.textContent} ${Math.round(box.width)}×${Math.round(box.height)}`),
    within,
  )

/**
 * Things wider than the phone: the page, or any element of the open dialog reaching past either
 * edge. The chip rows scroll sideways on purpose, so their chips are checked separately.
 */
const overflowing = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    for (const el of document.querySelectorAll<HTMLElement>('.shell-main *, .gr-dialog, .gr-dialog *')) {
      if (el.closest('.sr-only') || (el.closest('.lib-chips') && !el.classList.contains('lib-chips'))) continue
      const box = el.getBoundingClientRect()
      if (box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5)) found.push(`${el.tagName.toLowerCase()}.${el.className}`)
      // Text that does not fit its own box.
      if (el.matches('li, p, h1, h2, h3, .lib-row-name, .lib-row-muscles, .lib-muscle, .gr-btn-text, .gr-chip-face') && el.scrollWidth > el.clientWidth + 1) found.push(`clipped ${el.tagName.toLowerCase()}.${el.className}: ${el.textContent}`)
    }
    return found
  })

// The timing checks run one after another, and after every other spec has finished (the "speed" project in
// playwright.config.ts): beside busy workers they would time the machine, not the picker.
test.describe('picker speed', { tag: '@speed' }, () => {
  test.describe.configure({ mode: 'serial' })

  test('the picker settles within 100 ms when it opens, on every keystroke and on every filter change', async ({ page }) => {
    await openOnWednesday(page, '#/plan')
    await expect(page.getByRole('region', { name: 'Monday' })).toBeVisible()
    expect(catalogue.length).toBeGreaterThanOrEqual(26)

    // Opening it lists the whole catalogue.
    const opened = await settle(page, { open: true })
    expect(opened.rows).toEqual(grit.searchLibrary('', [], []).map((e) => e.name))
    expect(opened.rows).toHaveLength(catalogue.length)
    expect(opened.ms).toBeLessThan(SETTLE_MS)

    // Typing, one key at a time; then deleting it again.
    for (const typed of ['s', 'sq', 'squ', 'squa', 'squat', 'squa', '', 'pull up', 'RDL', 'romanian']) {
      const result = await settle(page, { type: typed })
      expect(result.rows, `"${typed}"`).toEqual(grit.searchLibrary(typed, [], []).map((e) => e.name))
      expect(result.ms, `"${typed}" took ${result.ms} ms`).toBeLessThan(SETTLE_MS)
    }
    expect(await shownNames(page)).toEqual(grit.searchLibrary('romanian', [], []).map((e) => e.name))
    expect((await shownNames(page))[0]).toBe('RDL')
    await settle(page, { type: '' })

    // Filters: one body area at a time, any of the chosen equipment.
    const steps: [chip: string, areas: string[], equipment: string[]][] = [
      ['Back', ['BACK'], []],
      ['Bodyweight', ['BACK'], ['BODYWEIGHT']],
      ['Barbell', ['BACK'], ['BODYWEIGHT', 'BARBELL']],
      ['Main lifts', ['MAIN_LIFTS'], ['BODYWEIGHT', 'BARBELL']],
      ['All', [], ['BODYWEIGHT', 'BARBELL']],
      ['Bodyweight', [], ['BARBELL']],
      ['Barbell', [], []],
    ]
    for (const [chip, areas, equipment] of steps) {
      const result = await settle(page, { chip })
      expect(result.rows, chip).toEqual(grit.searchLibrary('', areas, equipment).map((e) => e.name))
      expect(result.ms, `${chip} took ${result.ms} ms`).toBeLessThan(SETTLE_MS)
    }
    expect(await shownNames(page)).toHaveLength(catalogue.length)
  })

  test('the picker still settles within 100 ms with a few hundred exercises to list', async ({ page }) => {
    // The catalogue grows to about 250 entries; until it has, the lifter's own exercises stand in for them.
    const many = Array.from({ length: 250 }, (_, i) => ({ name: `Exercise ${String(i + 1).padStart(3, '0')}`, isBuiltIn: false, archived: false }))
    await openOnWednesday(page, '#/plan')
    await editExercises(page, { add: many })
    await expect(page.getByRole('region', { name: 'Monday' })).toBeVisible()

    const opened = await settle(page, { open: true })
    expect(opened.rows).toHaveLength(catalogue.length + 250)
    expect(opened.ms, `opening took ${opened.ms} ms`).toBeLessThan(SETTLE_MS)
    for (const typed of ['e', 'ex', 'exercise 1', 'ex', '']) {
      const result = await settle(page, { type: typed })
      expect(result.rows.length, `"${typed}"`).toBeGreaterThanOrEqual(typed === 'exercise 1' ? 100 : 250)
      expect(result.ms, `"${typed}" took ${result.ms} ms`).toBeLessThan(SETTLE_MS)
    }
    // A filter hides the lifter's own exercises and brings them back.
    const filtered = await settle(page, { chip: 'Back' })
    expect(filtered.rows).toEqual(grit.searchLibrary('', ['BACK'], []).map((e) => e.name))
    expect(filtered.ms).toBeLessThan(SETTLE_MS)
    const cleared = await settle(page, { chip: 'All' })
    expect(cleared.rows).toHaveLength(catalogue.length + 250)
    expect(cleared.ms, `clearing the filter took ${cleared.ms} ms`).toBeLessThan(SETTLE_MS)
  })

  test('browsing by muscle settles within 100 ms: opening on the grid, a muscle, its equipment and the way back', async ({ page }) => {
    await openOnWednesday(page, '#/plan')
    await expect(page.getByRole('region', { name: 'Monday' })).toBeVisible()
    const labels = grit.muscleGroups().map((g) => grit.muscleGroupLabel(g))
    const inGroup = (group: string, equipment: string[] = []) => grit.exercisesByMuscle(group, equipment).map((e) => e.name)

    // Opening it draws the grid above the whole catalogue.
    const opened = await settle(page, { open: true })
    expect(opened.tiles).toEqual(labels)
    expect(opened.rows).toHaveLength(catalogue.length)
    expect(opened.ms, `opening took ${opened.ms} ms`).toBeLessThan(SETTLE_MS)

    // Every muscle: tap it, read its list, go back to the grid.
    for (const group of grit.muscleGroups()) {
      const label = grit.muscleGroupLabel(group)
      const listed = await settle(page, { muscle: label })
      expect(listed.rows, label).toEqual(inGroup(group))
      expect(listed.tiles, label).toEqual([])
      expect(listed.ms, `${label} took ${listed.ms} ms`).toBeLessThan(SETTLE_MS)
      const back = await settle(page, { allMuscles: true })
      expect(back.tiles, label).toEqual(labels)
      expect(back.rows, label).toHaveLength(catalogue.length)
      expect(back.ms, `back from ${label} took ${back.ms} ms`).toBeLessThan(SETTLE_MS)
    }

    // Equipment inside a muscle, then typing, which leaves it.
    await settle(page, { muscle: 'Glutes' })
    const dumbbell = await settle(page, { chip: 'Dumbbell' })
    expect(dumbbell.rows).toEqual(inGroup('GLUTES', ['DUMBBELL']))
    expect(dumbbell.ms, `Dumbbell took ${dumbbell.ms} ms`).toBeLessThan(SETTLE_MS)
    const all = await settle(page, { chip: 'Dumbbell' })
    expect(all.rows).toEqual(inGroup('GLUTES'))
    expect(all.ms, `clearing Dumbbell took ${all.ms} ms`).toBeLessThan(SETTLE_MS)
    const typed = await settle(page, { type: 'squat' })
    expect(typed.rows).toEqual(grit.searchLibrary('squat', [], []).map((e) => e.name))
    expect(typed.ms, `typing took ${typed.ms} ms`).toBeLessThan(SETTLE_MS)
  })
})

test('the muscle grid fits a 375 px phone with full-size targets', async ({ page }) => {
  await openOnWednesday(page, '#/plan')
  await editExercises(page, { add: [{ name: 'Tyre flip', isBuiltIn: false, archived: false }] })
  await openPlanPicker(page)

  // The grid: every group, above the lifter's own exercises, two or three whole tiles to a row.
  const grid = picker(page).getByRole('group', { name: 'Browse by muscle' })
  const tiles = grid.getByRole('button')
  await expect(tiles).toHaveText(grit.muscleGroups().map((g) => grit.muscleGroupLabel(g)))
  const boxes = await tiles.evaluateAll((els) =>
    els.map((el) => {
      const box = el.getBoundingClientRect()
      return { label: el.textContent, left: box.left, right: box.right, top: box.top, width: box.width, height: box.height, clipped: el.scrollWidth > el.clientWidth + 1 || el.scrollHeight > el.clientHeight + 1 }
    }),
  )
  for (const box of boxes) {
    expect(box.width, box.label!).toBeGreaterThanOrEqual(48)
    expect(box.height, box.label!).toBeGreaterThanOrEqual(48)
    // One line of text: no tile grew to fit a wrapped name.
    expect(box.height, box.label!).toBeLessThan(60)
    expect(box.clipped, box.label!).toBe(false)
    expect(box.left, box.label!).toBeGreaterThanOrEqual(0)
    expect(box.right, box.label!).toBeLessThanOrEqual(375)
  }
  const perRow = new Map<number, number>()
  for (const box of boxes) perRow.set(Math.round(box.top), (perRow.get(Math.round(box.top)) ?? 0) + 1)
  expect([...perRow.values()].every((n) => n === 3)).toBe(true)
  expect((await grid.boundingBox())!.y).toBeLessThan((await picker(page).getByRole('heading', { name: 'Your exercises' }).boundingBox())!.y)
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog')).toEqual([])

  // Inside a muscle: the heading, the way back and the rows all fit. The lifter's own exercise names quads.
  await tiles.filter({ hasText: 'Quads' }).click()
  await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()
  await expect(grid).toHaveCount(0)
  const names = [...grit.exercisesByMuscle('QUADS', []).map((e) => e.name), 'Tyre flip'].sort((a, b) => (a.toLowerCase() < b.toLowerCase() ? -1 : 1))
  expect(await shownNames(page)).toEqual(names)
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog')).toEqual([])

  // The guide opens over the muscle's list and picks nothing.
  await pickerRow(page, 'Squat').getByRole('button', { name: 'Form guide for Squat' }).click()
  const guide = page.getByRole('dialog', { name: 'Form guide for Squat' })
  await expect(guide.getByText(SAFETY)).toBeAttached()
  await page.keyboard.press('Escape')
  await expect(guide).toHaveCount(0)
  await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()
  await expect.poll(async () => (await readAllTables(page)).planItems).toEqual([])

  // Equipment narrows it; "All muscles" is back at the grid.
  await picker(page).getByRole('group', { name: 'Equipment' }).locator('.gr-chip', { hasText: 'Kettlebell' }).click()
  expect(await shownNames(page)).toEqual(grit.exercisesByMuscle('QUADS', ['KETTLEBELL']).map((e) => e.name))
  await picker(page).getByRole('group', { name: 'Equipment' }).locator('.gr-chip', { hasText: 'Kettlebell' }).click()
  await picker(page).getByRole('button', { name: 'All muscles' }).click()
  await expect(grid).toBeVisible()
  expect(await overflowing(page)).toEqual([])

  // Picking from a muscle adds the exercise like any other row.
  await tiles.filter({ hasText: 'Calves' }).click()
  const calf = grit.exercisesByMuscle('CALVES', [])[0]!.name
  await pickerRow(page, calf).getByRole('button').first().click()
  await expect(picker(page)).toHaveCount(0)
  await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem')).toHaveAttribute('aria-label', new RegExp(`^${calf}  3 × `))
})

test('the picker fits a 375 px phone with full-size targets', async ({ page }) => {
  await openOnWednesday(page, '#/plan')
  await editExercises(page, { add: [{ name: 'Tyre flip with a very long name that has to wrap somewhere', isBuiltIn: false, archived: false }] })
  await openPlanPicker(page)

  // The lifter's own exercise leads the list; the catalogue follows under its own heading.
  await expect(picker(page).getByRole('heading', { name: 'Your exercises' })).toBeVisible()
  await expect(pickerRow(page, 'Tyre flip with a very long name that has to wrap somewhere')).toContainText('Your own')
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog')).toEqual([])
  expect(await picker(page).getByRole('button', { name: /^Form guide for / }).count()).toBe(catalogue.length + 1)

  // The search box is at least 16 px, or iOS zooms the page when it is tapped.
  const search = picker(page).getByRole('textbox', { name: 'Search exercises' })
  expect(await search.evaluate((el) => parseFloat(getComputedStyle(el).fontSize))).toBeGreaterThanOrEqual(16)

  // Every chip can be brought on screen whole, by scrolling its row sideways.
  for (const group of ['Body area', 'Equipment']) {
    const chips = picker(page).getByRole('group', { name: group }).locator('.gr-chip')
    for (let i = 0; i < (await chips.count()); i++) {
      await chips.nth(i).evaluate((chip) => chip.scrollIntoView({ inline: 'center', block: 'nearest' }))
      // Whole, to within sub-pixel rounding.
      await expect(chips.nth(i)).toBeInViewport({ ratio: 0.99 })
    }
  }
  expect(await overflowing(page)).toEqual([])

  // With the clear button in the search box, and with nothing found.
  await search.fill('zercher carry')
  await expect(picker(page).getByText(NO_MATCH)).toBeVisible()
  await expect(picker(page).getByRole('button', { name: 'Create your own' })).toBeVisible()
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog')).toEqual([])
  await picker(page).getByRole('button', { name: 'Clear search' }).click()
  await expect(search).toHaveValue('')

  // The guide opens over the picker and leaves it as it was; the row was not picked.
  await pickerRow(page, 'Squat').getByRole('button', { name: 'Form guide for Squat' }).click()
  const guide = page.getByRole('dialog', { name: 'Form guide for Squat' })
  await expect(guide.getByText(SAFETY)).toBeAttached()
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog-sheet')).toEqual([])
  await page.keyboard.press('Escape')
  await expect(guide).toHaveCount(0)
  await expect(picker(page)).toBeVisible()
  await expect.poll(async () => (await readAllTables(page)).planItems).toEqual([])

  // The long name is added and still fits its plan row, beside four full-size controls.
  await pickerRow(page, 'Tyre flip with a very long name that has to wrap somewhere').getByRole('button').first().click()
  await expect(picker(page)).toHaveCount(0)
  await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem')).toHaveAttribute('aria-label', 'Tyre flip with a very long name that has to wrap somewhere  3 × 10 @ 8')
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page)).toEqual([])
  // The numbers are whole and clear of the buttons.
  const dose = page.locator('.plan-row-dose')
  const info = page.getByRole('button', { name: /^Form guide for Tyre flip/ })
  expect((await dose.boundingBox())!.x + (await dose.boundingBox())!.width).toBeLessThanOrEqual((await info.boundingBox())!.x + 0.5)
})

test('the form guide scrolls to the safety line at 130% zoom', async ({ page }) => {
  // What 130% zoom does to a 375 × 812 phone: the page is laid out 1.3 times narrower and shorter.
  await page.setViewportSize({ width: Math.floor(375 / 1.3), height: Math.floor(812 / 1.3) })
  // The longest guide in the catalogue: the one most likely not to fit.
  const length = (e: (typeof catalogue)[number]) => [e.guide.purpose, ...e.guide.setup, ...e.guide.steps, ...e.guide.cues, ...e.guide.mistakes].join('').length
  const longest = catalogue.reduce((a, b) => (length(b) > length(a) ? b : a))

  await openOnWednesday(page, '#/plan')
  await openPlanPicker(page)
  await picker(page).getByRole('textbox', { name: 'Search exercises' }).fill(longest.name)
  await pickerRow(page, longest.name).getByRole('button', { name: `Form guide for ${longest.name}` }).click()

  const sheet = page.getByRole('dialog', { name: `Form guide for ${longest.name}` })
  const safety = sheet.getByText(SAFETY)
  await expect(sheet.getByRole('heading', { level: 2, name: longest.name })).toBeVisible()
  // The guide is longer than the screen, so it has to scroll: the safety line starts out of view.
  expect(await sheet.evaluate((el) => el.scrollHeight - el.clientHeight)).toBeGreaterThan(0)
  await expect(safety).not.toBeInViewport()

  // Every line of the guide is 16 px or more, as wide as the sheet allows and not clipped.
  const lines = [longest.guide.purpose, ...longest.guide.setup, ...longest.guide.steps, ...longest.guide.cues, ...longest.guide.mistakes, SAFETY]
  for (const line of lines) {
    const el = sheet.getByText(line, { exact: true })
    await el.scrollIntoViewIfNeeded()
    await expect(el, line).toBeInViewport()
    const fit = await el.evaluate((node) => {
      const box = node.getBoundingClientRect()
      return { left: box.left, right: box.right, clipped: node.scrollWidth > node.clientWidth + 1, fontSize: parseFloat(getComputedStyle(node).fontSize) }
    })
    expect(fit.left, line).toBeGreaterThanOrEqual(0)
    expect(fit.right, line).toBeLessThanOrEqual(page.viewportSize()!.width)
    expect(fit.clipped, line).toBe(false)
    expect(fit.fontSize, line).toBeGreaterThanOrEqual(16)
  }

  // Scrolled to the end: the whole safety line is on screen, last, and Close is still in reach.
  await sheet.evaluate((el) => el.scrollTo(0, el.scrollHeight))
  await expect(safety).toBeInViewport({ ratio: 1 })
  expect(await sheet.evaluate((el) => (el.querySelector('.lib-guide')!.lastElementChild as HTMLElement).innerText)).toBe(SAFETY)
  await expect(sheet.getByRole('button', { name: 'Close guide' })).toBeInViewport({ ratio: 1 })
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog-sheet')).toEqual([])

  await sheet.getByRole('button', { name: 'Close guide' }).click()
  await expect(sheet).toHaveCount(0)
  await expect(picker(page)).toBeVisible()
})

test('a library exercise gets its row with its first set on Today, and keeps it after a reload', async ({ page }) => {
  await openOnWednesday(page)
  // A library exercise with no row of its name. (A deleted built-in comes back when the app opens, so the
  // row is renamed instead: it is the lifter's own now.)
  await editExercises(page, { rename: { Curl: 'Curl of my own' } })
  const before = (await readAllTables(page)).exercises as Row[]
  expect(before.some((e) => e.name === 'Curl')).toBe(false)

  await tap(page, 'Start session')
  await tap(page, 'Add exercise')
  await picker(page).getByRole('textbox', { name: 'Search exercises' }).fill('curl')
  await expect(pickerRow(page, 'Curl of my own')).toContainText('Your own')
  await pickerRow(page, 'Curl').getByRole('button').first().click()
  await expect(page.getByRole('heading', { name: 'Curl' })).toBeVisible()
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page)).toEqual([])
  // On the card, not in the data.
  await expect.poll(async () => (await readAllTables(page)).exercises).toEqual(before)

  await typeInto(page, weightField(page), '20')
  await page.keyboard.press('Enter')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Curl' })).toBeVisible()

  // One built-in row, and the set points at it.
  await expect.poll(async () => ((await readAllTables(page)).exercises as Row[]).filter((e) => e.name === 'Curl').map((e) => [e.isBuiltIn, e.archived])).toEqual([[true, false]])
  const curl = ((await readAllTables(page)).exercises as Row[]).find((e) => e.name === 'Curl')!
  await expect.poll(async () => ((await readAllTables(page)).sets as StoredSet[]).map((s) => [s.exerciseId, s.weightKg])).toEqual([[curl.id, 20]])
  expect((await readAllTables(page)).exercises).toHaveLength(before.length + 1)

  await page.reload()
  await expect(page.getByRole('heading', { name: 'Curl' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
})
