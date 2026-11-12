import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { openOnWednesday, readAllTables, seedFlatBench, tabBar, tap } from './helpers'

// The Exercises tab on the narrowest iPhone: five tabs fit the bar, the browser and its sheets fit
// the phone with full-size targets, and an exercise goes from its guide into the plan or today's session.

const grit = GritDomain.getInstance()

test.use({ viewport: { width: 375, height: 812 } })

type PlanItem = { weekday: number; exerciseId: number; sets: number; reps: number; targetRpe: number; order: number }
type Exercise = { id: number; name: string; isBuiltIn: boolean }

const planItems = async (page: Page) => ((await readAllTables(page)).planItems as (PlanItem & { id: number })[]).map(({ id: _id, ...rest }) => rest)
const exercises = async (page: Page) => (await readAllTables(page)).exercises as Exercise[]
const shownNames = (page: Page) => page.evaluate(() => Array.from(document.querySelectorAll('.shell-main .lib-row')).map((row) => row.getAttribute('aria-label')))
const snackbar = (page: Page) => page.getByRole('status')
const guide = (page: Page, name: string) => page.getByRole('dialog', { name: `Form guide for ${name}` })
const openRow = (page: Page, name: string) => page.getByRole('button', { name: `Open ${name}`, exact: true }).click()

/** Buttons, links, toggle chips and tick rows smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page, within: string) =>
  page.evaluate(
    (within) =>
      Array.from(document.querySelectorAll<HTMLElement>(`${within} button, ${within} a[href], ${within} .gr-chip, ${within} .lib-option`))
        .map((el) => ({ el, box: el.getBoundingClientRect() }))
        .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
        .map(({ el, box }) => `${el.className} ${el.getAttribute('aria-label') ?? el.textContent} ${Math.round(box.width)}×${Math.round(box.height)}`),
    within,
  )

/**
 * Things wider than the phone: the page, or any element of the tab or the open sheet reaching past
 * either edge, and text that does not fit its own box. The chip row scrolls sideways on purpose.
 */
const overflowing = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    for (const el of document.querySelectorAll<HTMLElement>('.shell-main *, .gr-tabbar *, .gr-snackbar, .gr-snackbar *, .gr-dialog, .gr-dialog *')) {
      if (el.closest('.sr-only') || (el.closest('.lib-chips') && !el.classList.contains('lib-chips'))) continue
      const box = el.getBoundingClientRect()
      if (box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5)) found.push(`${el.tagName.toLowerCase()}.${el.className}`)
      if (el.matches('li, p, h1, h2, h3, a, .lib-row-name, .lib-row-muscles, .lib-option, .lib-ticked-face, .lib-dropdown-face, .gr-btn-text, .gr-chip-face, .gr-snackbar-message') && el.scrollWidth > el.clientWidth + 1)
        found.push(`clipped ${el.tagName.toLowerCase()}.${el.className}: ${el.textContent}`)
    }
    return found
  })

async function tick(page: Page, ...groups: string[]) {
  await page.getByRole('button', { name: /^Muscles/ }).click()
  for (const group of groups) await page.getByRole('dialog', { name: 'Muscles' }).getByRole('checkbox', { name: grit.muscleGroupLabel(group), exact: true }).check()
}

test('exercisesTab_isTheFourthOfFiveTabs_andFits', async ({ page }) => {
  await openOnWednesday(page)
  const links = tabBar(page).getByRole('link')
  await expect(links).toHaveText(['Today', 'Coach', 'Progress', 'Exercises', 'Plan'])

  for (const tab of ['Today', 'Coach', 'Progress', 'Exercises', 'Plan']) {
    await tabBar(page).getByRole('link', { name: tab, exact: true }).click()
    await expect(tabBar(page).getByRole('link', { name: tab, exact: true })).toHaveAttribute('aria-current', 'page')
    await expect(page.getByRole('heading', { level: 1, name: tab })).toBeVisible()
    const tabs = await links.evaluateAll((all) =>
      all.map((a) => {
        const box = a.getBoundingClientRect()
        const text = document.createRange()
        text.selectNodeContents(a)
        const label = Array.from(text.getClientRects()).at(-1)!
        const pill = a.querySelector('.gr-tab-indicator')!.getBoundingClientRect()
        return { name: a.textContent, left: box.left, right: box.right, width: box.width, height: box.height, labelLeft: label.left, labelRight: label.right, pillLeft: pill.left, pillRight: pill.right, clipped: a.scrollWidth > a.clientWidth, oneLine: label.height < 24 }
      }),
    )
    expect(tabs).toHaveLength(5)
    for (const [i, t] of tabs.entries()) {
      // A full-size target, on the phone, beside its neighbour and not over it.
      expect(t.width, t.name!).toBeGreaterThanOrEqual(48)
      expect(t.height, t.name!).toBeGreaterThanOrEqual(48)
      expect(t.left, t.name!).toBeGreaterThanOrEqual(0)
      expect(t.right, t.name!).toBeLessThanOrEqual(375)
      if (i > 0) expect(t.left, t.name!).toBeGreaterThanOrEqual(tabs[i - 1]!.right - 0.5)
      // The label is whole, on one line, inside its own tab; so is the indicator pill.
      expect(t.clipped, t.name!).toBe(false)
      expect(t.oneLine, t.name!).toBe(true)
      expect(t.labelLeft, t.name!).toBeGreaterThanOrEqual(t.left)
      expect(t.labelRight, t.name!).toBeLessThanOrEqual(t.right)
      expect(t.pillLeft, t.name!).toBeGreaterThanOrEqual(t.left)
      expect(t.pillRight, t.name!).toBeLessThanOrEqual(t.right)
    }
    expect(await overflowing(page)).toEqual([])
  }
  expect(await page.evaluate(() => getComputedStyle(document.querySelector('.gr-tab')!).textOverflow)).not.toBe('ellipsis')
})

test('the Exercises tab fits a 375 px phone with full-size targets', async ({ page }) => {
  await openOnWednesday(page)
  await seedFlatBench(page)
  await tap(page, 'Start session')
  await tabBar(page).getByRole('link', { name: 'Exercises' }).click()

  await expect(page.getByText(`${grit.library().length} exercises`)).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Your exercises' })).toBeVisible()
  expect(await shownNames(page)).toHaveLength(grit.library().length)
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.shell-main')).toEqual([])
  // The search box is at least 16 px, or iOS zooms the page when it is tapped.
  const search = page.getByRole('textbox', { name: 'Search exercises' })
  expect(await search.evaluate((el) => parseFloat(getComputedStyle(el).fontSize))).toBeGreaterThanOrEqual(16)

  // The list scrolls under the header and the search; the page itself does not.
  const last = page.getByRole('listitem', { name: grit.searchLibrary('', ['CORE'], []).at(-1)!.name, exact: true })
  await last.scrollIntoViewIfNeeded()
  await expect(last).toBeInViewport({ ratio: 0.99 })
  await expect(search).toBeInViewport()
  await expect(page.getByRole('heading', { level: 1, name: 'Exercises' })).toBeInViewport()
  expect(await page.locator('.shell-main').evaluate((el) => el.scrollHeight <= el.clientHeight)).toBe(true)

  // The drop-down: 18 full-size rows to tick, two to a row.
  await tick(page, 'FRONT_DELTS', 'SIDE_DELTS', 'REAR_DELTS', 'UPPER_BACK', 'LOWER_BACK', 'HAMSTRINGS')
  const sheet = page.getByRole('dialog', { name: 'Muscles' })
  await expect(sheet.getByRole('checkbox')).toHaveCount(18)
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog')).toEqual([])
  await sheet.getByRole('button', { name: 'Close' }).click()

  // Six ticked: the chips wrap instead of running off the phone, and a new filter starts from the top.
  const chosen = page.getByRole('group', { name: 'Chosen muscles' })
  await expect(chosen.getByRole('button', { name: /^Remove / })).toHaveCount(6)
  await expect(page.getByRole('button', { name: 'Muscles · 6' })).toBeVisible()
  await expect(chosen.getByRole('button', { name: 'Clear' })).toBeInViewport({ ratio: 0.99 })
  expect(await shownNames(page)).toEqual(grit.exercisesByMuscles(['FRONT_DELTS', 'SIDE_DELTS', 'REAR_DELTS', 'UPPER_BACK', 'LOWER_BACK', 'HAMSTRINGS'], []).map((e) => e.name))
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.shell-main')).toEqual([])
  await chosen.getByRole('button', { name: 'Clear' }).click()
  await expect(chosen).toHaveCount(0)

  // The guide with both actions, in reach under a long guide; then the day chooser.
  await openRow(page, 'Bulgarian split squat')
  const sheetGuide = guide(page, 'Bulgarian split squat')
  await expect(sheetGuide.getByRole('button', { name: 'Add to today' })).toBeInViewport({ ratio: 0.99 })
  await expect(sheetGuide.getByRole('button', { name: 'Add to plan' })).toBeInViewport({ ratio: 0.99 })
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog')).toEqual([])
  await sheetGuide.evaluate((el) => el.scrollTo(0, el.scrollHeight))
  await expect(sheetGuide.getByRole('button', { name: 'Add to plan' })).toBeInViewport({ ratio: 0.99 })
  await sheetGuide.getByRole('button', { name: 'Add to plan' }).click()
  const chooser = page.getByRole('dialog', { name: 'Add to plan' })
  await expect(chooser.getByRole('button', { name: 'Friday' })).toBeVisible()
  expect(await overflowing(page)).toEqual([])
  expect(await smallTargets(page, '.gr-dialog')).toEqual([])
  await chooser.getByRole('button', { name: 'Friday' }).click()

  // The message and its action fit above the tab bar.
  await expect(snackbar(page)).toContainText('Added to Friday')
  expect(await overflowing(page)).toEqual([])
  const message = (await page.locator('.gr-snackbar').boundingBox())!
  expect(message.y + message.height).toBeLessThanOrEqual((await tabBar(page).boundingBox())!.y)
})

test('an exercise is added to the plan from its guide, once per day', async ({ page }) => {
  await openOnWednesday(page, '#/exercises')
  const before = await exercises(page)
  expect(before.some((e) => e.name === 'Hack squat')).toBe(false)

  await page.getByRole('textbox', { name: 'Search exercises' }).fill('hack')
  await openRow(page, 'Hack squat')
  await expect(guide(page, 'Hack squat').getByRole('button', { name: 'Add to today' })).toHaveCount(0)
  await guide(page, 'Hack squat').getByRole('button', { name: 'Add to plan' }).click()
  const chooser = page.getByRole('dialog', { name: 'Add to plan' })
  // The default training days, in weekday order.
  await expect(chooser.locator('.lib-days button')).toHaveText(['Monday', 'Wednesday', 'Friday'])
  await chooser.getByRole('button', { name: 'Wednesday' }).click()

  await expect(snackbar(page)).toContainText('Added to Wednesday')
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect.poll(() => planItems(page)).toHaveLength(1)
  const hack = (await exercises(page)).find((e) => e.name === 'Hack squat')!
  expect(hack.isBuiltIn).toBe(true)
  expect(await planItems(page)).toEqual([{ weekday: 3, exerciseId: hack.id, sets: 3, reps: 5, targetRpe: 8, order: 0 }])

  // "Open Plan" leads to it.
  await snackbar(page).getByRole('button', { name: 'Open Plan' }).click()
  await expect(tabBar(page).getByRole('link', { name: 'Plan' })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('region', { name: 'Wednesday' }).getByRole('listitem')).toHaveAttribute('aria-label', 'Hack squat  3 × 5 @ 8')

  // Back on the tab the search is as it was left. The same day again adds nothing.
  await tabBar(page).getByRole('link', { name: 'Exercises' }).click()
  await expect(page.getByRole('textbox', { name: 'Search exercises' })).toHaveValue('hack')
  await openRow(page, 'Hack squat')
  await guide(page, 'Hack squat').getByRole('button', { name: 'Add to plan' }).click()
  await chooser.getByRole('button', { name: 'Wednesday' }).click()
  await expect(snackbar(page)).toContainText('Already in your plan on Wednesday.')
  await expect(snackbar(page).getByRole('button')).toHaveCount(0)
  await expect.poll(() => planItems(page)).toHaveLength(1)

  // It is stored: a reload keeps the plan, and starts the tab's search afresh.
  await page.reload()
  await expect(page.getByRole('textbox', { name: 'Search exercises' })).toHaveValue('')
  await expect.poll(() => planItems(page)).toEqual([{ weekday: 3, exerciseId: hack.id, sets: 3, reps: 5, targetRpe: 8, order: 0 }])
})

test('an exercise is added to the open session from its guide, with no row until its first set', async ({ page }) => {
  await openOnWednesday(page)
  await seedFlatBench(page)
  await tap(page, 'Start session')
  const before = await exercises(page)

  await tabBar(page).getByRole('link', { name: 'Exercises' }).click()
  await openRow(page, 'Hack squat')
  await guide(page, 'Hack squat').getByRole('button', { name: 'Add to today' }).click()
  await expect(snackbar(page)).toContainText('Added to today')
  await snackbar(page).getByRole('button', { name: 'Open Today' }).click()

  // Its card is on the Today tab, and nothing is stored for it yet.
  await expect(tabBar(page).getByRole('link', { name: 'Today' })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('button', { name: /^Hack squat/ })).toBeVisible()
  expect(await exercises(page)).toEqual(before)

  // Finished: the tab no longer offers today.
  await tap(page, 'Finish')
  await expect(page.getByRole('heading', { name: 'Session saved' })).toBeVisible()
  await tabBar(page).getByRole('link', { name: 'Exercises' }).click()
  await openRow(page, 'Leg press')
  await expect(guide(page, 'Leg press').getByRole('button', { name: 'Add to plan' })).toBeVisible()
  await expect(guide(page, 'Leg press').getByRole('button', { name: 'Add to today' })).toHaveCount(0)
})
