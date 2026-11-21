import { expect, test, type Locator, type Page } from '@playwright/test'
import { openOnWednesday, pastTheTapWindow, tabBar, tap } from './helpers'
import { LONG_NAME, ONE_WORD, bodybuildingStory, clearOfBars, drawAllRows, fitsHere, keepsTargets, powerliftingStory, reachable, seedStory } from './narrowChecks'

// 1.17.3: the screens one step deeper (lift and exercise history, the check-in, the plan builder) and the dialogs and
// sheets (the exercise picker, the form guide, the Muscles sheet, Create your own, Plan's add and edit forms, Set best,
// the builder's swap sheet) on a 320 px phone at 200 % text, laid out at 160 × 284. Each state is checked on what is
// on top, the open dialog or else the screen, with `wrongHere` (nothing left out by name); each dialog's or screen's
// primary action and its close control can be scrolled to and tapped; nothing has to stay under a bar that stays put;
// and no target is smaller than at 390 px (up to 48 px). Elements are found by their roles and names, so the same
// checks run on an earlier build.

/** The open dialog named `name`. */
const dialog = (page: Page, name: string | RegExp) => page.getByRole('dialog', { name })

/** Everything a state must be at this size: fits, its action and close reachable, nothing under a bar. */
async function holds(page: Page, state: string, primary?: Locator, close?: Locator) {
  await fitsHere(page, state)
  expect.soft(await clearOfBars(page), `${state}: nothing under a bar`).toEqual([])
  if (primary) await reachable(primary, `${state}: its action`)
  if (close) await reachable(close, `${state}: its close control`)
}

/** Today with a session open. */
async function startToday(page: Page) {
  await tap(page, 'Start session')
  await expect(page.getByRole('button', { name: 'Add exercise' })).toBeVisible()
}

/** Opens Today's picker. */
async function todayPicker(page: Page) {
  await page.getByRole('button', { name: 'Add exercise' }).click()
  const picker = dialog(page, /^(Add to |Add exercise)/)
  await expect(picker).toBeVisible()
  return picker
}

/** Every state of an open picker: the grid with recents, every body area, a muscle, a search, no match, the end with Create your own. */
async function pickerStates(page: Page, picker: Locator, where: string) {
  const close = picker.getByRole('button', { name: 'Close', exact: true })
  await holds(page, `${where}: the picker`, undefined, close)
  const search = picker.getByRole('textbox', { name: 'Search exercises' })
  const areas = picker.getByRole('group', { name: 'Body area' }).locator('label.gr-chip')
  for (let i = 1; i < (await areas.count()); i++) {
    await areas.nth(i).click()
    await fitsHere(page, `${where}: the picker, body area ${(await areas.nth(i).textContent())?.trim()}`)
  }
  await areas.first().click()
  const tiles = picker.getByRole('group', { name: 'Browse by muscle' }).getByRole('button')
  const count = await tiles.count()
  expect(count).toBeGreaterThan(5)
  for (let i = 0; i < count; i++) {
    const label = (await tiles.nth(i).textContent())?.trim()
    await tiles.nth(i).click()
    await expect(picker.getByRole('button', { name: 'All muscles' })).toBeVisible()
    await fitsHere(page, `${where}: the picker, ${label}`)
    if (i === 0) {
      await picker.getByRole('group', { name: 'Equipment' }).locator('label.gr-chip').nth(1).click()
      await fitsHere(page, `${where}: the picker, ${label} with equipment`)
      await picker.getByRole('group', { name: 'Equipment' }).locator('label.gr-chip').nth(1).click()
    }
    await picker.getByRole('button', { name: 'All muscles' }).click()
  }
  await search.fill('raise')
  await expect(picker.getByRole('listitem', { name: LONG_NAME })).toBeVisible()
  await fitsHere(page, `${where}: the picker, searched "raise"`)
  await search.fill(ONE_WORD.slice(0, 9))
  await expect(picker.getByRole('listitem', { name: ONE_WORD })).toBeVisible()
  await fitsHere(page, `${where}: the picker, searched the one-word name`)
  await search.fill('zzzz')
  await expect(picker.getByRole('button', { name: 'Create your own' })).toBeVisible()
  await holds(page, `${where}: the picker, nothing matches`, picker.getByRole('button', { name: 'Create your own' }))
  await search.fill('')
  // The end of the list, with Create your own under it.
  await expect(picker.getByRole('button', { name: 'Create your own' })).toBeAttached()
  await holds(page, `${where}: the picker, its end`, picker.getByRole('button', { name: 'Create your own' }))
}

/** Create your own from an open picker, with the one-word name, then as a variation (its parent lift to choose). */
async function createYourOwn(page: Page, picker: Locator, where: string) {
  await picker.getByRole('textbox', { name: 'Search exercises' }).fill('')
  const create = picker.getByRole('button', { name: 'Create your own' })
  await create.scrollIntoViewIfNeeded()
  await create.click()
  const form = dialog(page, 'Add custom exercise')
  await expect(form).toBeVisible()
  const cancel = form.getByRole('button', { name: 'Cancel' })
  await holds(page, `${where}: Create your own`, form.getByRole('button', { name: 'Create' }), cancel)
  await form.getByRole('textbox', { name: 'Name' }).fill(ONE_WORD)
  await form.getByRole('textbox', { name: 'Muscles (comma-separated)' }).fill('Side delts, Front delts, Upper chest')
  await form.getByText('Variation', { exact: true }).click()
  await expect(form.getByRole('group', { name: 'Parent lift' })).toBeVisible()
  // Create is off until a parent lift is chosen: the form's only way of saying something is missing.
  await expect(form.getByRole('button', { name: 'Create' })).toBeDisabled()
  await holds(page, `${where}: Create your own, a variation without its lift`, form.getByRole('button', { name: 'Create' }), cancel)
  await cancel.click()
  await expect(form).toHaveCount(0)
}

test.describe('the deeper screens and dialogs at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })
  // Each test walks every state of its screen or dialog.
  test.describe.configure({ timeout: 180_000 })

  test('lift history, every lift, powerlifting side, fits', async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, powerliftingStory())
    for (const [lift, name] of [['SQUAT', 'Squat'], ['BENCH', 'Bench'], ['DEADLIFT', 'Deadlift'], ['STRICT_PRESS', 'Strict press']] as const) {
      await page.goto(`/#/lift/${lift}`)
      await expect(page.getByRole('heading', { level: 1, name: `${name} history` })).toBeVisible()
      await expect(page.locator('.gr-set').first()).toBeVisible()
      await holds(page, `${name} history`, undefined, page.getByRole('button', { name: 'Back' }))
    }
    // Every kind of row is there: a failed set, a top set, a record, no RPE, bodyweight.
    await page.goto('/#/lift/SQUAT')
    await expect(page.getByText('Failed', { exact: true }).first()).toBeAttached()
    await expect(page.getByText('Top set', { exact: true }).first()).toBeAttached()
    await expect(page.getByText('PR', { exact: true }).first()).toBeAttached()
  })

  test('exercise history, bodybuilding side, plates and the one-word name, fits', async ({ page }) => {
    await openOnWednesday(page)
    const ids = await seedStory(page, bodybuildingStory())
    for (const name of ['Lat pulldown', ONE_WORD, LONG_NAME]) {
      await page.goto(`/#/exercise/${ids[name]}`)
      await expect(page.getByRole('heading', { level: 1, name: `${name} history` })).toBeVisible()
      await expect(page.locator('.gr-set').first()).toBeVisible()
      await holds(page, `${name} history`, undefined, page.getByRole('button', { name: 'Back' }))
    }
    await page.goto(`/#/exercise/${ids['Lat pulldown']}`)
    await expect(page.getByText('plate 12½', { exact: true }).first()).toBeAttached()
  })

  // The check-in and the plan builder are the bodybuilding side's own: on the powerlifting side their pages lead back.
  test('the check-in, every step, fits', async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, bodybuildingStory())
    await page.goto('/#/checkin')
    const next = page.getByRole('button', { name: 'Next' })
    await expect(next).toBeVisible()
    await holds(page, 'check-in, lagging', next, page.getByRole('button', { name: 'Close', exact: true }))
    for (const group of ['Side delts', 'Chest', 'Hamstrings']) await page.getByRole('checkbox', { name: group }).check()
    await holds(page, 'check-in, lagging, three ticked', next)
    await next.click()
    const save = page.getByRole('button', { name: 'Save' })
    await expect(save).toBeVisible()
    await page.getByRole('checkbox', { name: 'Lats' }).check()
    await holds(page, 'check-in, growing well', save, page.getByRole('button', { name: 'Back' }))
    await save.click()
    const done = page.getByRole('button', { name: 'Done' })
    await expect(done).toBeVisible()
    await holds(page, 'check-in, saved', done)
  })

  test('the plan builder, every step and its dialogs, fits', async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, bodybuildingStory())
    await page.goto('/#/plan/build')
    await expect(page.getByText('Step 1 of 4')).toBeVisible()
    const next = page.getByRole('button', { name: /^(Next|Choose for me|Skip)$/ })
    await holds(page, 'builder, days', next, page.getByRole('button', { name: 'Close', exact: true }))
    await page.getByText('4 days', { exact: true }).click()
    const days = page.getByRole('group', { name: 'Which days?' })
    await days.getByText('Sat', { exact: true }).click()
    await holds(page, 'builder, four days', next)
    await next.click()
    await expect(page.getByRole('heading', { name: /^Which exercises/ })).toBeVisible()
    await expect.poll(() => page.locator('.lib-row').count()).toBeGreaterThan(100)
    await holds(page, 'builder, exercises', next)
    await page.getByRole('textbox', { name: 'Search exercises' }).fill('raise')
    await page.getByRole('listitem', { name: LONG_NAME }).last().getByRole('checkbox').click({ force: true })
    await holds(page, 'builder, exercises, searched and ticked', next)
    await page.getByRole('textbox', { name: 'Search exercises' }).fill('')
    // Its Muscles sheet, two ticked.
    await page.locator('.lib-dropdown').click()
    const muscles = dialog(page, 'Muscles')
    await muscles.getByRole('checkbox').nth(0).check()
    await muscles.getByRole('checkbox').nth(5).check()
    await holds(page, 'builder, its Muscles sheet', undefined, muscles.getByRole('button', { name: 'Close', exact: true }))
    await muscles.getByRole('button', { name: 'Close', exact: true }).click()
    await holds(page, 'builder, exercises, two muscles chosen', next)
    await next.click()
    await expect(page.getByRole('heading', { name: /^Which body parts/ })).toBeVisible()
    await page.getByRole('checkbox', { name: 'Side delts' }).check()
    await page.getByRole('checkbox', { name: 'Chest' }).check()
    await holds(page, 'builder, body parts', next)
    await next.click()
    await expect(page.getByRole('heading', { name: 'Your draft week' })).toBeVisible()
    const use = page.getByRole('button', { name: 'Use this plan' })
    await holds(page, 'builder, the draft', use)
    // The swap sheet of the first exercise.
    const swap = page.getByRole('button', { name: /^Swap / }).first()
    await swap.scrollIntoViewIfNeeded()
    const swapName = (await swap.getAttribute('aria-label'))!
    await swap.click()
    const sheet = dialog(page, swapName)
    await expect(sheet).toBeVisible()
    await holds(page, 'builder, the swap sheet', sheet.getByRole('listitem').first().getByRole('button').first(), sheet.getByRole('button', { name: 'Close', exact: true }))
    await sheet.getByRole('button', { name: 'Close', exact: true }).click()
    // Replacing the side's plan asks first.
    await use.click()
    const confirm = dialog(page, 'Use this plan?')
    await expect(confirm).toBeVisible()
    await holds(page, 'builder, Use this plan?', confirm.getByRole('button', { name: 'Replace' }), confirm.getByRole('button', { name: 'Cancel' }))
  })

  for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
    const s = side.toLowerCase()
    test(`the picker from Today, every step, and Create your own, ${s} side, fits`, async ({ page }) => {
      await openOnWednesday(page)
      await seedStory(page, side === 'BODYBUILDING' ? bodybuildingStory() : powerliftingStory())
      await startToday(page)
      const picker = await todayPicker(page)
      // Recently used exercises lead the list.
      await expect(picker.getByRole('heading', { name: 'Your exercises' })).toBeVisible()
      await pickerStates(page, picker, `Today, ${s}`)
      await createYourOwn(page, picker, `Today, ${s}`)
    })

    test(`the picker from Plan, every step, and Create your own, ${s} side, fits`, async ({ page }) => {
      await openOnWednesday(page)
      await seedStory(page, side === 'BODYBUILDING' ? bodybuildingStory() : powerliftingStory())
      await page.goto('/#/plan')
      await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
      const picker = dialog(page, 'Add to Monday')
      await expect(picker).toBeVisible()
      await pickerStates(page, picker, `Plan, ${s}`)
      await createYourOwn(page, picker, `Plan, ${s}`)
    })

    test(`Plan's add and edit forms, Change's picker and the RPE sheet's opener, ${s} side, fit`, async ({ page }) => {
      await openOnWednesday(page)
      await seedStory(page, side === 'BODYBUILDING' ? bodybuildingStory() : powerliftingStory())
      await page.goto('/#/plan')
      await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
      const picker = dialog(page, 'Add to Monday')
      await picker.getByRole('textbox', { name: 'Search exercises' }).fill(ONE_WORD.slice(0, 9))
      await picker.getByRole('listitem', { name: ONE_WORD }).getByRole('button').first().click()
      const add = page.getByRole('dialog').filter({ has: page.getByRole('group', { name: 'Sets' }) })
      await expect(add).toBeVisible()
      await pastTheTapWindow(page)
      await holds(page, `Plan's add form, the one-word name`, add.getByRole('button', { name: 'Add', exact: true }), add.getByRole('button', { name: 'Cancel' }))
      for (const label of side === 'BODYBUILDING' ? ['Sets', 'Reps from', 'Reps to', 'RPE'] : ['Sets', 'Reps', 'RPE'])
        await reachable(add.getByRole('button', { name: `Increase ${label}` }), `Plan's add form: Increase ${label}`)
      // Change: its picker over the form.
      await add.getByRole('button', { name: 'Change' }).click()
      const change = dialog(page, 'Change exercise')
      await expect(change).toBeVisible()
      await change.getByRole('textbox', { name: 'Search exercises' }).fill('raise')
      await fitsHere(page, `Plan's add form, Change's picker`)
      await change.getByRole('listitem', { name: LONG_NAME }).getByRole('button').first().click()
      await expect(change).toHaveCount(0)
      await holds(page, `Plan's add form, changed to the long name`, add.getByRole('button', { name: 'Add', exact: true }))
      await add.getByRole('button', { name: 'Cancel' }).click()
      // The edit form of the one-word row.
      const row = page.getByRole('listitem', { name: new RegExp(`^${ONE_WORD}`) })
      await row.getByRole('button', { name: 'More', exact: true }).click()
      await page.getByRole('menuitem', { name: 'Edit' }).click()
      const edit = page.getByRole('dialog').filter({ has: page.getByRole('group', { name: 'Sets' }) })
      await expect(edit).toBeVisible()
      await holds(page, `Plan's edit form`, edit.getByRole('button', { name: 'Save' }), edit.getByRole('button', { name: 'Cancel' }))
      for (let i = 0; i < 4; i++) await edit.getByRole('button', { name: 'Increase RPE' }).click()
      await holds(page, `Plan's edit form, RPE raised`, edit.getByRole('button', { name: 'Save' }))
    })
  }

  test('Set best, its field and its line, fits', async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, powerliftingStory())
    await page.goto('/#/plan')
    await page.getByRole('button', { name: 'Set best' }).first().click()
    const best = dialog(page, /^Known best: /)
    await expect(best).toBeVisible()
    await holds(page, 'Set best', best.getByRole('button', { name: 'Save' }), best.getByRole('button', { name: 'Cancel' }))
    await best.getByRole('textbox').fill('9999')
    await holds(page, 'Set best, an impossible best typed', best.getByRole('button', { name: 'Save' }))
    await best.getByRole('textbox').fill('182.5')
    await holds(page, 'Set best, a best typed', best.getByRole('button', { name: 'Save' }))
  })

  for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
    const s = side.toLowerCase()
    test(`the form guide from Today, the Exercises tab and the picker, with and without pictures, ${s} side, fits`, async ({ page }) => {
      await openOnWednesday(page)
      await seedStory(page, side === 'BODYBUILDING' ? bodybuildingStory() : powerliftingStory())
      const guideStates = async (name: string, where: string, actions: string[]) => {
        const guide = dialog(page, `Form guide for ${name}`)
        await expect(guide).toBeVisible()
        const close = guide.getByRole('button', { name: 'Close guide' })
        await holds(page, `${where}: the form guide for ${name}`, actions.length ? guide.getByRole('button', { name: actions.at(-1), exact: true }) : undefined, close)
        for (const action of actions) await reachable(guide.getByRole('button', { name: action, exact: true }), `${where}: the form guide's ${action}`)
        await close.click()
        await expect(guide).toHaveCount(0)
      }
      // The Exercises tab, no session: Add to plan.
      await page.goto('/#/exercises')
      for (const name of ['Squat', 'Single-arm cable lateral raise', ONE_WORD]) {
        await page.getByRole('textbox', { name: 'Search exercises' }).fill(name.slice(0, 12))
        await page.getByRole('button', { name: `Open ${name}`, exact: true }).click()
        await guideStates(name, 'the Exercises tab', ['Add to plan'])
      }
      // A session open: Add to today and Add to plan; then with the exercise already in today's session.
      await tabBar(page).getByRole('link', { name: 'Today' }).click()
      await startToday(page)
      await tabBar(page).getByRole('link', { name: 'Exercises' }).click()
      await page.getByRole('textbox', { name: 'Search exercises' }).fill('Squat')
      await page.getByRole('button', { name: 'Open Squat', exact: true }).click()
      await guideStates('Squat', 'the Exercises tab, a session open', ['Add to today', 'Add to plan'])
      await page.getByRole('button', { name: 'Open Squat', exact: true }).click()
      await dialog(page, 'Form guide for Squat').getByRole('button', { name: 'Add to today' }).click()
      await expect(page.getByText('Added to today')).toBeVisible()
      await page.getByRole('button', { name: 'Open Squat', exact: true }).click()
      await guideStates('Squat', 'the Exercises tab, Squat already added', ['Add to today', 'Add to plan'])
      // Its Add to plan's day chooser.
      await page.getByRole('button', { name: 'Open Squat', exact: true }).click()
      await dialog(page, 'Form guide for Squat').getByRole('button', { name: 'Add to plan' }).click()
      const days = dialog(page, 'Add to plan')
      await expect(days).toBeVisible()
      await holds(page, 'the day chooser', days.getByRole('button').last(), days.getByRole('button', { name: 'Close', exact: true }))
      await days.getByRole('button', { name: 'Close', exact: true }).click()
      // Today's ⓘ on the exercise card.
      await tabBar(page).getByRole('link', { name: 'Today' }).click()
      await page.getByRole('button', { name: 'Form guide for Squat' }).first().click()
      await guideStates('Squat', 'Today', [])
      // From the picker's ⓘ.
      const picker = await todayPicker(page)
      await picker.getByRole('textbox', { name: 'Search exercises' }).fill('raise')
      await picker.getByRole('button', { name: `Form guide for ${LONG_NAME}` }).click()
      await guideStates(LONG_NAME, 'the picker', [])
      await picker.getByRole('button', { name: 'Form guide for Cable lateral raise' }).click()
      await guideStates('Cable lateral raise', 'the picker', [])
    })

    test(`the Muscles sheet, some ticked, ${s} side, fits`, async ({ page }) => {
      await openOnWednesday(page)
      if (side === 'BODYBUILDING') await seedStory(page, bodybuildingStory())
      await page.goto('/#/exercises')
      await page.locator('.lib-dropdown').click()
      const sheet = dialog(page, 'Muscles')
      await expect(sheet).toBeVisible()
      await holds(page, 'the Muscles sheet', undefined, sheet.getByRole('button', { name: 'Close', exact: true }))
      for (const n of [0, 3, 7]) await sheet.getByRole('checkbox').nth(n).check()
      await holds(page, 'the Muscles sheet, three ticked', sheet.getByRole('checkbox').last(), sheet.getByRole('button', { name: 'Close', exact: true }))
    })
  }

  // Every target in these screens and dialogs is at 160 px at least as large as at 390 px, up to 48 px each way.
  test('every touch target in the deeper screens and dialogs keeps its size', async ({ page }) => {
    await openOnWednesday(page)
    const ids = await seedStory(page, bodybuildingStory())
    // Each state opened afresh: Today first, so no dialog of the last state is left open.
    const at = async (what: string, open: () => Promise<void>) =>
      keepsTargets(page, what, async () => {
        await page.goto('/#/today')
        await expect(tabBar(page)).toBeVisible()
        await open()
      })
    await at('exercise history', async () => {
      await page.goto(`/#/exercise/${ids['Lat pulldown']}`)
      await expect(page.locator('.gr-set').first()).toBeVisible()
    })
    await at('check-in', async () => {
      await page.goto('/#/checkin')
      await expect(page.getByRole('button', { name: 'Next' })).toBeVisible()
    })
    await at('plan builder, days', async () => {
      await page.goto('/#/plan/build')
      await expect(page.getByText('Step 1 of 4')).toBeVisible()
    })
    await at('the picker from Plan', async () => {
      await page.goto('/#/plan')
      await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
      await expect(dialog(page, 'Add to Monday')).toBeVisible()
    })
    await at('the Muscles sheet', async () => {
      await page.goto('/#/exercises')
      await page.locator('.lib-dropdown').click()
      await expect(dialog(page, 'Muscles')).toBeVisible()
    })
    await at('the form guide', async () => {
      await page.goto('/#/exercises')
      await page.getByRole('button', { name: 'Open Squat', exact: true }).click()
      await expect(dialog(page, 'Form guide for Squat')).toBeVisible()
    })
    await at("Plan's add form", async () => {
      await page.goto('/#/plan')
      await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
      const picker = dialog(page, 'Add to Monday')
      await picker.getByRole('textbox', { name: 'Search exercises' }).fill('Squat')
      await picker.getByRole('listitem', { name: 'Squat', exact: true }).getByRole('button').first().click()
      await expect(page.getByRole('group', { name: 'Sets' })).toBeVisible()
    })
    await at('Create your own', async () => {
      await page.goto('/#/plan')
      await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
      const picker = dialog(page, 'Add to Monday')
      await picker.getByRole('textbox', { name: 'Search exercises' }).fill('zzzz')
      await picker.getByRole('button', { name: 'Create your own' }).click()
      await expect(dialog(page, 'Add custom exercise')).toBeVisible()
    })
  })
})


// 1.17.3: a new search or filter takes the list back to its top. At this width the Exercises tab and the builder's
// exercises step scroll as a whole, the search field and the chips with the list: the screen goes back to where the
// field's top meets the screen's top (no further), so the field being typed in stays on screen and the list's first
// row is under it. At 390 px the list scrolls on its own and goes back to its own top, as before.

/** Where the field and the list's first row are on the screen, and how far the screen and the list are scrolled. */
const scrollState = (page: Page) =>
  page.evaluate(() => {
    const screen = document.querySelector('.shell-main')!
    const list = document.querySelector('.shell-main .lib-scroll')!
    const field = document.querySelector('.shell-main .lib-search .gr-field-box')!.getBoundingClientRect()
    const row = document.querySelector('.shell-main .lib-scroll .lib-row')!.getBoundingClientRect()
    const room = screen.getBoundingClientRect()
    const bottom = Math.min(room.bottom, document.querySelector('.gr-tabbar')!.getBoundingClientRect().top, document.querySelector('.pb-actions')?.getBoundingClientRect().top ?? Infinity)
    return {
      screen: screen.scrollTop,
      list: list.scrollTop,
      fieldInView: field.top >= room.top - 0.5 && field.bottom <= bottom + 0.5,
      firstRowInView: row.top >= room.top - 0.5 && row.top < bottom - 16,
      firstRowBelowTop: row.top >= room.top - 0.5,
      firstRow: document.querySelector('.shell-main .lib-scroll .lib-row')!.getAttribute('aria-label'),
    }
  })

test.describe('a new search or filter at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  test('on the Exercises tab, the list goes back to its top under the search field, which stays on screen', async ({ page }) => {
    await openOnWednesday(page)
    await page.goto('/#/exercises')
    const search = page.getByRole('textbox', { name: 'Search exercises' })
    await expect(page.getByRole('listitem', { name: 'Squat', exact: true })).toBeVisible()
    // Typing, far down the list: the field is brought on screen to type in, and the list starts under it.
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 3000))
    await search.fill('curl')
    await expect.poll(async () => (await scrollState(page)).firstRow).toMatch(/curl/i)
    let state = await scrollState(page)
    expect(state.fieldInView, 'the field is on screen').toBe(true)
    expect(state.firstRowInView, 'the first row is on screen').toBe(true)
    // A filter, with the field scrolled just off the top: back to the field, no further.
    await page.locator('.shell-main').evaluate((el) => {
      const field = el.querySelector('.lib-search')!
      el.scrollTop += field.getBoundingClientRect().bottom - el.getBoundingClientRect().top + 400
    })
    const before = (await scrollState(page)).screen
    await page.getByRole('group', { name: 'Equipment' }).locator('label.gr-chip').first().click()
    await expect.poll(async () => (await scrollState(page)).screen).toBeLessThan(before)
    state = await scrollState(page)
    expect(state.fieldInView, 'the field is on screen after a filter').toBe(true)
    expect(state.firstRowInView, 'the first row is on screen after a filter').toBe(true)
    // A screen scrolled less than that stays put: the header still shows.
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 10))
    await search.fill('row')
    await expect.poll(async () => (await scrollState(page)).firstRow).toMatch(/row/i)
    expect((await scrollState(page)).screen).toBe(10)
  })

  test("in the plan builder's exercises step, the same", async ({ page }) => {
    await openOnWednesday(page)
    // Nothing logged, so nothing is ticked and listed under "Chosen" above the list.
    await seedStory(page, { side: 'BODYBUILDING' })
    await page.goto('/#/plan/build')
    await page.getByRole('button', { name: 'Next' }).click()
    await expect(page.getByRole('heading', { name: /^Which exercises/ })).toBeVisible()
    await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 3000))
    await page.getByRole('textbox', { name: 'Search exercises' }).fill('raise')
    await expect.poll(async () => (await scrollState(page)).firstRow).toMatch(/raise/i)
    const state = await scrollState(page)
    expect(state.fieldInView, 'the field is on screen').toBe(true)
    // Here Back and Next and the tab bar leave about 130 px: the field and the chips fill it, and the list's first row
    // starts right under them (not above the screen), a short swipe away.
    expect(state.firstRowBelowTop, 'the first row is not above the screen').toBe(true)
  })
})

test.describe('a new search or filter on a 390 px phone', () => {
  test.use({ viewport: { width: 390, height: 844 } })

  test('the list goes back to its own top, and the screen does not move', async ({ page }) => {
    await openOnWednesday(page)
    await page.goto('/#/exercises')
    await expect(page.getByRole('listitem', { name: 'Squat', exact: true })).toBeVisible()
    await expect.poll(() => page.locator('.lib-row').count()).toBeGreaterThan(100)
    await page.locator('.shell-main .lib-scroll').evaluate((el) => el.scrollTo(0, 3000))
    expect((await scrollState(page)).list).toBeGreaterThan(1000)
    await page.getByRole('textbox', { name: 'Search exercises' }).fill('curl')
    await expect.poll(async () => (await scrollState(page)).list).toBe(0)
    expect((await scrollState(page)).screen).toBe(0)
    await page.locator('.shell-main .lib-scroll').evaluate((el) => el.scrollTo(0, 600))
    await page.getByRole('group', { name: 'Equipment' }).locator('label.gr-chip').first().click()
    await expect.poll(async () => (await scrollState(page)).list).toBe(0)
    expect((await scrollState(page)).screen).toBe(0)
  })
})

// 1.17.3: at this width the picker and Create your own look the same from Today as from Plan. Today's own narrow
// rules (its eyebrows and button labels wrap) reach into a dialog drawn inside Today; the dialogs' own rules say the
// same for every opener.
test.describe('the picker and Create your own at 320 px and 200 % text, from Today and from Plan', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  /** Each row's and heading's size, and the size of Create your own's parts, in the dialog on top. */
  const layout = (page: Page) =>
    page.evaluate(() => {
      const dialogs = [...document.querySelectorAll<HTMLElement>('.gr-dialog')].filter((d) => d.closest('[inert]') == null)
      const root = dialogs.at(-1)!
      const size = (el: Element) => {
        const r = el.getBoundingClientRect()
        const s = getComputedStyle(el)
        return `${Math.round(r.width)}×${Math.round(r.height)} ${s.whiteSpace} ${s.overflowWrap}`
      }
      return [...root.querySelectorAll('.lib-row, .lib-row-name, .lib-row-muscles, .lib-heading, .eyebrow, .gr-btn, .gr-btn-text, .lib-muscle, .gr-field, .gr-chip, .gr-dialog-actions')].map(
        (el) => `${el.className} "${(el.textContent ?? '').trim().slice(0, 30)}": ${size(el)}`,
      )
    })

  test('the same rows, headings, buttons and form', async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, bodybuildingStory())
    const states = async (picker: Locator) => {
      // Drawn alike: rows, then searched rows, then Create your own.
      await drawAllRows(page)
      const grid = await layout(page)
      await picker.getByRole('textbox', { name: 'Search exercises' }).fill('raise')
      await expect(picker.getByRole('listitem', { name: LONG_NAME })).toBeVisible()
      const searched = await layout(page)
      await picker.getByRole('textbox', { name: 'Search exercises' }).fill('zzzz')
      await picker.getByRole('button', { name: 'Create your own' }).click()
      await expect(dialog(page, 'Add custom exercise')).toBeVisible()
      const create = await layout(page)
      return { grid, searched, create }
    }
    await tap(page, 'Start session')
    const fromToday = await states(await todayPicker(page))
    await page.goto('/#/plan')
    await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
    const fromPlan = await states(dialog(page, 'Add to Monday'))
    expect(fromToday.create).toEqual(fromPlan.create)
    expect(fromToday.searched).toEqual(fromPlan.searched)
    // The grid is under "Your exercises" on Today and Plan alike; only the rows each list as recent may differ.
    const withoutRows = (all: string[]) => all.filter((l) => !l.startsWith('lib-row'))
    expect(withoutRows(fromToday.grid)).toEqual(withoutRows(fromPlan.grid))
  })
})
