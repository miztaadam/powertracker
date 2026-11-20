import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { E2E_BUILD_ID } from '../playwright.config'
import { shownVersion } from '../src/screens/settings/appVersion'
import { pastTheTapWindow } from './helpers'

// Plan and Settings with the same sample story as the Android mockups (docs/design/screens/plan-dark.png,
// settings-dark.png). A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites
// the committed PNGs in docs/design/web/screens/. Look at them after a visual change.
const PACKAGE_VERSION = (JSON.parse(readFileSync(resolve(process.cwd(), 'package.json'), 'utf8')) as { version: string }).version
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

type SamplePlanItem = [weekday: number, exercise: string, sets: number, reps: number, targetRpe: number]

const MOCKUP_PLAN: SamplePlanItem[] = [
  [1, 'Squat', 5, 5, 8],
  [1, 'Bench', 4, 6, 7.5],
  [1, 'Row', 3, 8, 8],
  [3, 'Deadlift', 3, 3, 8],
  [3, 'Close-grip bench', 3, 5, 8],
  [5, 'Bench', 3, 5, 8],
  [5, 'Pause squat', 3, 4, 7.5],
]

/** Opens the Plan tab on the sample Wednesday; `plan` and `sessionDates` go straight into IndexedDB. */
async function openPlan(page: Page, plan: SamplePlanItem[] = [], sessionDates: string[] = []) {
  await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
  await page.goto('/#/plan')
  await expect(page.getByRole('region', { name: 'Monday' })).toBeVisible()
  if (plan.length === 0 && sessionDates.length === 0) return
  await page.evaluate(
    async ({ plan, sessionDates }) => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'planItems', 'sessions'], 'readwrite')
      const settled = new Promise<void>((done, fail) => {
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
        const all = tx.objectStore('exercises').getAll()
        all.onsuccess = () => done(all.result)
      })
      const order = new Map<number, number>()
      for (const [weekday, name, sets, reps, targetRpe] of plan) {
        const next = order.get(weekday) ?? 0
        order.set(weekday, next + 1)
        tx.objectStore('planItems').add({ weekday, exerciseId: exercises.find((e) => e.name === name)!.id, sets, reps, targetRpe, order: next })
      }
      for (const date of sessionDates) tx.objectStore('sessions').add({ date, bodyweightKg: null, readiness: null })
      await settled
      db.close()
    },
    { plan, sessionDates },
  )
  await page.reload()
  await expect(page.getByRole('region', { name: 'Monday' })).toBeVisible()
}

/** The sets and reps form a pick on Plan's Add opens: the dialog with the Sets stepper (the picker has the same title). */
const addForm = (page: Page) => page.getByRole('dialog').filter({ has: page.getByRole('group', { name: 'Sets' }) })

/** The plan as the screen reads it out: one line per item, per day. */
const rowsOf = (page: Page, day: string) =>
  page
    .getByRole('region', { name: day })
    .getByRole('listitem')
    .evaluateAll((rows) => rows.map((row) => row.getAttribute('aria-label')))

/** Buttons, links and toggle chips smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page, { hiddenPickerSkipped = false } = {}) =>
  page.evaluate(
    (hiddenPickerSkipped) =>
      Array.from(document.querySelectorAll<HTMLElement>('button, a[href], .gr-chip, .gr-segment'))
        // The picker that opened Plan's add form stays mounted under it, hidden (display: none): not a target.
        .filter((el) => !(hiddenPickerSkipped && el.closest('.gr-dialog-screen')?.checkVisibility() === false))
        .map((el) => ({ el, box: el.getBoundingClientRect() }))
        .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
        .map(({ el, box }) => `${el.className} ${el.textContent} ${Math.round(box.width)}×${Math.round(box.height)}`),
    hiddenPickerSkipped,
  )

/** Things wider than the phone: the page, the scrolling region, or any element reaching past either edge. */
const overflowing = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    const main = document.querySelector('.shell-main')!
    if (main.scrollWidth > main.clientWidth) found.push('.shell-main')
    for (const el of document.querySelectorAll<HTMLElement>('.shell-main *, .gr-dialog *')) {
      const box = el.getBoundingClientRect()
      // The picker's chip rows scroll sideways on purpose (e2e/library.spec.ts checks every chip can be reached).
      if (el.closest('.lib-chips') && !el.classList.contains('lib-chips')) continue
      if (box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5) && !el.closest('.sr-only')) found.push(`${el.tagName.toLowerCase()}.${el.className}`)
    }
    return found
  })

/** Text that does not fit its own box: a chip or segment label spilling out of its face. */
const spilledText = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('.gr-chip-face, .gr-segment-face, .gr-btn-text, .gr-stepper-value'))
      .filter((el) => el.scrollWidth > el.clientWidth + 1)
      .map((el) => `${el.className}: ${el.textContent}`),
  )

async function shoot(page: Page, name: string, height: number) {
  // The shell scrolls inside the viewport: make the phone tall enough to show the whole screen, from the top.
  await page.setViewportSize({ width: 390, height })
  await page.locator('.shell-main').evaluate((el) => el.scrollTo(0, 0))
  await page.evaluate(() => document.fonts.ready)
  mkdirSync(outDir, { recursive: true })
  await page.screenshot({ path: resolve(outDir, `${name}.png`), fullPage: true, animations: 'disabled' })
}

const narrowPhone = { width: 375, height: 812 }
// Tall enough for the whole Settings screen, the guide's row (1.16) and the version line included.
const SETTINGS_HEIGHT = 1990

for (const scheme of ['dark', 'light'] as const) {
  test(`plan and settings match the mockup story (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openPlan(page, MOCKUP_PLAN)

    // The narrowest iPhone.
    await page.setViewportSize(narrowPhone)
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Squat  5 × 5 @ 8', 'Bench  4 × 6 @ 7.5', 'Row  3 × 8 @ 8'])
    await expect(page.getByRole('region', { name: 'Monday' }).getByText('3 exercises · 12 sets')).toBeVisible()
    await expect.poll(() => rowsOf(page, 'Wednesday')).toEqual(['Deadlift  3 × 3 @ 8', 'Close-grip bench  3 × 5 @ 8'])
    await expect.poll(() => rowsOf(page, 'Friday')).toEqual(['Bench  3 × 5 @ 8', 'Pause squat  3 × 4 @ 7.5'])
    await expect(page.getByRole('heading', { name: 'Known bests' })).toBeVisible()
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await shoot(page, `plan-${scheme}`, 1200)

    // The menu of the last row opens inside the phone.
    await page.setViewportSize(narrowPhone)
    await page.getByRole('region', { name: 'Friday' }).getByRole('button', { name: 'More' }).last().click()
    await expect(page.getByRole('menuitem', { name: 'Delete' })).toBeInViewport({ ratio: 1 })
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await page.keyboard.press('Escape')
    await expect(page.getByRole('menu')).toHaveCount(0)

    await page.getByRole('button', { name: 'Settings' }).click()
    await expect(page).toHaveURL(/#\/settings$/)
    await expect(page.getByRole('heading', { level: 1, name: 'Settings' })).toBeVisible()
    await page.getByText('Change plates').click()
    await expect(page.getByRole('switch', { name: 'Change plates' })).toBeChecked()
    await expect(page.getByText('Storage: not protected')).toBeVisible()
    await expect(page.getByText('Last backup: never')).toBeVisible()
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    expect(await spilledText(page)).toEqual([])
    // Last of all, the version line, with the fixed id the e2e build gets (playwright.config.ts).
    const version = page.getByText(`Grit ${shownVersion(PACKAGE_VERSION)} · ${E2E_BUILD_ID} · AppsbyAdam`, { exact: true })
    await version.scrollIntoViewIfNeeded()
    await expect(version).toBeInViewport({ ratio: 1 })
    await expect(page.getByText(`Grit version ${shownVersion(PACKAGE_VERSION)}, build ${E2E_BUILD_ID}, by AppsbyAdam`)).toHaveCount(1)
    await shoot(page, `settings-${scheme}`, SETTINGS_HEIGHT)

    // The strict press switch: its own group, the whole row a target, and the screen still fits with it on.
    await page.setViewportSize(narrowPhone)
    await expect(page.getByRole('switch', { name: 'Strict press' })).not.toBeChecked()
    await page.getByText('Show it on Progress and add it to your total.').click()
    await expect(page.getByRole('switch', { name: 'Strict press' })).toBeChecked()
    expect((await page.locator('label.settings-row', { hasText: 'Strict press' }).boundingBox())!.height).toBeGreaterThanOrEqual(48)
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    expect(await spilledText(page)).toEqual([])

    // The RPE info button beside "Default RPE": a full target, clear of the stepper, and the label still on one line.
    const info = (await page.getByRole('button', { name: 'What RPE means' }).boundingBox())!
    const minus = (await page.getByRole('button', { name: 'Decrease Default RPE' }).boundingBox())!
    expect(Math.min(info.width, info.height)).toBeGreaterThanOrEqual(47.5)
    expect(info.x).toBeGreaterThanOrEqual(0)
    expect(info.x + info.width).toBeLessThanOrEqual(minus.x + 0.5)
    expect((await page.getByText('Default RPE', { exact: true }).boundingBox())!.height).toBeLessThan(30)
    await shoot(page, `settings-strict-press-${scheme}`, SETTINGS_HEIGHT)

    await page.setViewportSize(narrowPhone)
    await page.getByRole('button', { name: 'What RPE means' }).click()
    await expect(page.getByRole('dialog', { name: 'RPE: reps in reserve' })).toBeVisible()
    await page.getByRole('dialog', { name: 'RPE: reps in reserve' }).getByRole('button', { name: 'Close' }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await expect(page.getByRole('group', { name: 'Default RPE' })).toContainText('8')

    // Back is one step back in history, not a new entry.
    await page.getByRole('button', { name: 'Back', exact: true }).click()
    await expect(page).toHaveURL(/#\/plan$/)
    await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
    await page.goForward()
    await expect(page).toHaveURL(/#\/settings$/)
  })
}

// The Training style group's switch puts the app on the bodybuilding side at once: the blue look, the label
// (bodybuilding mode spec §7.1, §7.2).
for (const scheme of ['dark', 'light'] as const) {
  test(`settings on the bodybuilding side (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openPlan(page, MOCKUP_PLAN)
    await page.setViewportSize(narrowPhone)
    await expect(page.getByTestId('mode-label')).toHaveCount(0)
    await expect(page.locator('html')).not.toHaveAttribute('data-mode')
    await page.getByRole('button', { name: 'Settings' }).click()
    await expect(page.getByRole('heading', { level: 2 }).first()).toHaveText('Training style')
    const toggle = page.getByRole('switch', { name: 'Bodybuilding mode' })
    await expect(toggle).not.toBeChecked()
    await page.getByText('Rep ranges, a physique coach and a blue look. Your powerlifting plan is kept.').click()
    await expect(toggle).toBeChecked()
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
    await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute('content', scheme === 'dark' ? '#070B14' : '#EDF2FA')
    await expect(page.getByTestId('mode-label')).toBeVisible()
    // No powerlifting rows on this side; its one rest is every exercise's.
    await expect(page.getByText('Default RPE')).toHaveCount(0)
    await expect(page.getByRole('switch', { name: 'Strict press' })).toHaveCount(0)
    await expect(page.getByText('Main lift rest')).toHaveCount(0)
    await expect(page.getByText('Every exercise')).toBeVisible()
    expect((await page.locator('label.settings-row', { hasText: 'Bodybuilding mode' }).boundingBox())!.height).toBeGreaterThanOrEqual(48)
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    expect(await spilledText(page)).toEqual([])
    await shoot(page, `settings-bodybuilding-${scheme}`, SETTINGS_HEIGHT)

    // It lasts: a reload opens on the same side, and the label is on Plan too.
    await page.reload()
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
    await page.getByRole('button', { name: 'Back', exact: true }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
    await expect(page.getByTestId('mode-label')).toBeVisible()

    // And off again, back to the powerlifting look.
    await page.getByRole('button', { name: 'Settings' }).click()
    await toggle.click()
    await expect(toggle).not.toBeChecked()
    await expect(page.locator('html')).not.toHaveAttribute('data-mode')
    await expect(page.getByTestId('mode-label')).toHaveCount(0)
  })
}

test.describe('on a 375 px iPhone', () => {
  test.use({ viewport: narrowPhone })

  test('the plan is edited through its dialogs and survives a reload', async ({ page }) => {
    await openPlan(page)
    const monday = page.getByRole('region', { name: 'Monday' })
    const dialog = page.getByRole('dialog')

    // Picking from the library opens the form with its default sets and reps; Add stores them.
    await monday.getByRole('button', { name: 'Add' }).click()
    await expect(page.getByRole('dialog', { name: 'Add to Monday' })).toBeVisible()
    await dialog.getByRole('textbox', { name: 'Search exercises' }).fill('squat')
    await dialog.getByRole('listitem', { name: 'Squat', exact: true }).getByRole('button').first().click()
    await expect(addForm(page).getByRole('group', { name: 'Reps' })).toContainText('5')
    expect(await overflowing(page)).toEqual([])
    // The picker it came from is still mounted under the form, hidden: only its controls are left out.
    expect(await smallTargets(page, { hiddenPickerSkipped: true })).toEqual([])
    await pastTheTapWindow(page)
    await addForm(page).getByRole('button', { name: 'Add', exact: true }).click()
    await expect(dialog).toHaveCount(0)
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Squat  3 × 5 @ 8'])

    // A custom variation, created from inside the picker.
    await monday.getByRole('button', { name: 'Add' }).click()
    await dialog.getByRole('button', { name: 'Create your own' }).click()
    const custom = page.getByRole('dialog', { name: 'Add custom exercise' })
    await custom.getByRole('textbox', { name: 'Name' }).fill('Cluster squat')
    await custom.getByText('Variation').click()
    await custom.getByText('Squat', { exact: true }).click()
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await custom.getByRole('button', { name: 'Create' }).click()
    await expect(addForm(page)).toBeVisible()
    await pastTheTapWindow(page)
    await addForm(page).getByRole('button', { name: 'Add', exact: true }).click()
    await expect(monday.getByRole('listitem')).toHaveCount(2)

    // Its sets are changed from the row.
    await monday.getByRole('button', { name: 'More' }).last().click()
    await page.getByRole('menuitem', { name: 'Edit' }).click()
    await expect(page.getByRole('dialog', { name: 'Edit Monday' }).getByRole('group', { name: 'Exercise' })).toContainText('Cluster squat')
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await dialog.getByRole('button', { name: 'Increase Sets' }).click()
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Squat  3 × 5 @ 8', 'Cluster squat  4 × 5 @ 8'])

    await monday.getByRole('button', { name: 'Move up' }).last().click()
    await expect(monday.getByRole('listitem').first()).toHaveAttribute('aria-label', 'Cluster squat  4 × 5 @ 8')

    await page.getByRole('group', { name: 'Deadlift' }).getByRole('button', { name: 'Set best' }).click()
    await dialog.getByRole('textbox', { name: 'Estimated 1RM (kg)' }).fill('501')
    await expect(dialog.getByRole('button', { name: 'Save' })).toBeDisabled()
    await dialog.getByRole('textbox', { name: 'Estimated 1RM (kg)' }).fill('220')
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect(dialog).toHaveCount(0)

    await page.reload()
    await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem')).toHaveCount(2)
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Cluster squat  4 × 5 @ 8', 'Squat  3 × 5 @ 8'])
  })

  test('planAdd_chooseSetsReps_storedAndShownInRow', async ({ page }) => {
    await openPlan(page)
    const monday = page.getByRole('region', { name: 'Monday' })
    await monday.getByRole('button', { name: 'Add' }).click()
    const picker = page.getByRole('dialog', { name: 'Add to Monday' })
    await picker.getByRole('textbox', { name: 'Search exercises' }).fill('bench')
    await picker.getByRole('listitem', { name: 'Bench', exact: true }).getByRole('button').first().click()

    // Bench opens as 3 × 5; made 5 × 3 before anything is stored.
    const form = addForm(page)
    await expect(form.getByRole('group', { name: 'Exercise' })).toContainText('Bench')
    await form.getByRole('button', { name: 'Increase Sets' }).click()
    await form.getByRole('button', { name: 'Increase Sets' }).click()
    await form.getByRole('button', { name: 'Decrease Reps' }).click()
    await form.getByRole('button', { name: 'Decrease Reps' }).click()
    await expect(form.getByRole('group', { name: 'Sets' })).toContainText('5')
    await expect(form.getByRole('group', { name: 'Reps' })).toContainText('3')
    expect(await rowsOf(page, 'Monday')).toEqual([])

    await pastTheTapWindow(page)
    await form.getByRole('button', { name: 'Add', exact: true }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Bench  5 × 3 @ 8'])
    await expect(monday.getByText('5 × 3')).toBeVisible()
    // Stored, not only drawn.
    await page.reload()
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Bench  5 × 3 @ 8'])
  })

  test('a backup is exported, and restoring it asks before replacing everything', async ({ page }) => {
    await openPlan(page, MOCKUP_PLAN)
    await page.goto('/#/settings')

    const downloading = page.waitForEvent('download')
    await page.getByRole('button', { name: 'Export backup' }).click()
    const download = await downloading
    expect(download.suggestedFilename()).toBe('grit-backup-2026-11-11.json')
    await expect(page.getByText('Backup saved.')).toBeVisible()
    await expect(page.getByText('Last backup: Nov 11, 2026')).toBeVisible()
    const backupPath = resolve(process.cwd(), 'test-results/grit-backup.json')
    mkdirSync(resolve(process.cwd(), 'test-results'), { recursive: true })
    await download.saveAs(backupPath)
    expect(JSON.parse(readFileSync(backupPath, 'utf8')).planItems).toHaveLength(7)

    // The plan changes after the backup.
    await page.goto('/#/plan')
    await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'More' }).first().click()
    await page.getByRole('menuitem', { name: 'Delete' }).click()
    await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem')).toHaveCount(2)
    await page.goto('/#/settings')

    // A file that is not a backup changes nothing.
    const notABackup = resolve(process.cwd(), 'test-results/not-a-backup.json')
    writeFileSync(notABackup, '{"hello":"world"}')
    await page.getByLabel('Backup file').setInputFiles(notABackup)
    await expect(page.getByText("This file isn't a valid Grit backup.")).toBeVisible()
    await expect(page.getByRole('dialog')).toHaveCount(0)

    await page.getByLabel('Backup file').setInputFiles(backupPath)
    const dialog = page.getByRole('dialog', { name: 'Replace all data?' })
    await expect(dialog).toBeVisible()
    expect(await overflowing(page)).toEqual([])
    await dialog.getByRole('button', { name: 'Cancel' }).click()
    await page.getByLabel('Backup file').setInputFiles(backupPath)
    await dialog.getByRole('button', { name: 'Replace' }).click()
    await expect(page.getByText('Backup restored.')).toBeVisible()

    await page.getByRole('button', { name: 'Back', exact: true }).click()
    await expect(page).toHaveURL(/#\/plan$/)
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Squat  5 × 5 @ 8', 'Bench  4 × 6 @ 7.5', 'Row  3 × 8 @ 8'])
  })

  test('Today reminds about a backup and leads to Settings', async ({ page }) => {
    await openPlan(page, [], ['2026-10-01'])
    await page.goto('/#/today')
    await expect(page.getByRole('heading', { name: 'Back up your log' })).toBeVisible()
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await page.getByRole('button', { name: 'Back up' }).click()
    await expect(page).toHaveURL(/#\/settings$/)
    await expect(page.getByRole('button', { name: 'Export backup' })).toBeVisible()
  })
})

// ---- 1.16.1: Known bests show each lift's best, and strict press once its switch is on ----

/** Turns the strict press switch on (or off) in Settings and comes back to Plan. */
async function strictPressOn(page: Page, on = true) {
  await page.getByRole('button', { name: 'Settings' }).click()
  await page.getByText('Show it on Progress and add it to your total.').click()
  await expect(page.getByRole('switch', { name: 'Strict press' })).toBeChecked({ checked: on })
  await page.getByRole('button', { name: 'Back', exact: true }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
}

/** Types a known best for `lift` through its Set best. */
async function setBest(page: Page, lift: string, text: string) {
  await page.getByRole('group', { name: lift, exact: true }).getByRole('button', { name: 'Set best' }).click()
  const dialog = page.getByRole('dialog', { name: `Known best: ${lift}` })
  await dialog.getByRole('textbox', { name: 'Estimated 1RM (kg)' }).fill(text)
  await dialog.getByRole('button', { name: 'Save' }).click()
  await expect(dialog).toHaveCount(0)
}

/**
 * Each Known bests row whose name or best line reaches the Set best button, measured on the glyphs themselves (a
 * Range's client rects over every text node of the row's text column, so text spilling out of its box counts), any
 * of whose text boxes is narrower than its text (scrollWidth > clientWidth), or whose button leaves the phone.
 */
const crowdedBests = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('.plan-best')).flatMap((row) => {
      const name = row.getAttribute('aria-label')
      const button = row.querySelector('button')!.getBoundingClientRect()
      const column = row.querySelector<HTMLElement>('.plan-grow')!
      const found: string[] = []
      const walker = document.createTreeWalker(column, NodeFilter.SHOW_TEXT)
      for (let node = walker.nextNode(); node != null; node = walker.nextNode()) {
        const range = document.createRange()
        range.selectNodeContents(node)
        const right = Math.max(...Array.from(range.getClientRects(), (r) => r.right))
        if (right > button.left + 0.5) found.push(`${name}: "${node.textContent}" runs under Set best (to ${Math.round(right)}, button at ${Math.round(button.left)})`)
      }
      for (const box of [column, ...Array.from(column.querySelectorAll<HTMLElement>('*'))]) {
        if (box.scrollWidth > box.clientWidth + 1) found.push(`${name}: "${box.textContent}" is wider than its box`)
      }
      if (button.right > window.innerWidth + 0.5) found.push(`${name}: Set best leaves the phone`)
      return found
    }),
  )

for (const scheme of ['dark', 'light'] as const) {
  test(`Known bests: each lift's best, strict press with its switch on (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openPlan(page, MOCKUP_PLAN)
    await page.setViewportSize(narrowPhone)
    // Switch off and no strict press row: the three lifts, no lines.
    await expect(page.getByRole('group', { name: 'Strict press' })).toHaveCount(0)
    await expect(page.locator('.plan-bests').getByText(/^Best e1RM /)).toHaveCount(0)

    await strictPressOn(page)
    await expect(page.getByRole('group', { name: 'Strict press' })).toBeVisible()
    await setBest(page, 'Squat', '182,5')
    await setBest(page, 'Bench', '120')
    await setBest(page, 'Strict press', '72.5')

    for (const [lift, line] of [['Squat', 'Best e1RM 182.5 kg'], ['Bench', 'Best e1RM 120 kg'], ['Strict press', 'Best e1RM 72.5 kg']] as const) {
      await expect(page.getByRole('group', { name: lift, exact: true })).toContainText(line)
    }
    await expect(page.getByRole('group', { name: 'Deadlift' }).getByText(/^Best e1RM /)).toHaveCount(0)
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    expect(await crowdedBests(page)).toEqual([])

    // The owner's look: written as plan-known-bests-{dark,light}.png (test-results/screens/, or with
    // UPDATE_SCREENSHOTS=1 docs/design/web/screens/).
    await shoot(page, `plan-known-bests-${scheme}`, 1300)

    // The switch off again keeps its row and its best (the coach still uses it), as for any strict press with a row.
    await strictPressOn(page, false)
    await page.reload()
    await expect(page.getByRole('group', { name: 'Strict press' })).toContainText('Best e1RM 72.5 kg')
  })
}

/** A name with no space to break at: too wide for a 320 px row's text column on one line. */
const UNBROKEN = 'Seatedstrictpressfromthesafetypins'

test.describe('on a 320 × 568 phone', () => {
  test.use({ viewport: { width: 320, height: 568 } })

  // Proof by hand: remove `overflow-wrap: anywhere;` from `.plan-best-text` in src/screens/plan/plan.css and the
  // unbroken name runs under Set best, so `crowdedBests` names it.
  test('a Known bests row keeps Set best beside a two-line name and its best', async ({ page }) => {
    await openPlan(page, MOCKUP_PLAN)
    // Strict press under long names of its own (as the strict press upgrade keeps other names), each with a typed
    // best: one with spaces, and one with none, which wraps only by `overflow-wrap: anywhere`.
    await page.evaluate(async (names) => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'baselines'], 'readwrite')
      const settled = new Promise<void>((done, fail) => {
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      for (const name of names) {
        const add = tx.objectStore('exercises').add({ name, category: 'STRICT_PRESS', parentLift: null, muscles: [], isBuiltIn: false, archived: false })
        add.onsuccess = () => tx.objectStore('baselines').put({ exerciseId: add.result as number, e1rmKg: 72.5, date: '2026-11-11' })
      }
      await settled
      db.close()
    }, ['Seated strict press from the safety pins', UNBROKEN])
    await page.reload()
    await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
    await strictPressOn(page)
    await setBest(page, 'Squat', '182.5')

    const long = page.getByRole('group', { name: 'Seated strict press from the safety pins' })
    await long.scrollIntoViewIfNeeded()
    await expect(long).toContainText('Best e1RM 72.5 kg')
    await expect(page.getByRole('group', { name: 'Squat', exact: true })).toContainText('Best e1RM 182.5 kg')
    // The switch on adds no second strict press: this one is it.
    await expect(page.getByRole('group', { name: 'Strict press', exact: true })).toHaveCount(0)
    // Its name takes two lines, beside the button.
    const name = (await long.locator('.title-medium').boundingBox())!
    const squatName = (await page.getByRole('group', { name: 'Squat', exact: true }).locator('.title-medium').boundingBox())!
    expect(name.height).toBeGreaterThan(squatName.height * 1.5)
    // The unbroken name wraps too, and keeps its best line under it.
    const unbroken = page.getByRole('group', { name: UNBROKEN })
    await unbroken.scrollIntoViewIfNeeded()
    await expect(unbroken).toContainText('Best e1RM 72.5 kg')
    expect((await unbroken.locator('.title-medium').boundingBox())!.height).toBeGreaterThan(squatName.height * 1.5)
    expect(await crowdedBests(page)).toEqual([])
    expect(await overflowing(page)).toEqual([])
    // Every Set best is still a full target (the rest of Plan's targets at this width are narrow.spec.ts's).
    for (const box of await page.locator('.plan-best button').evaluateAll((all) => all.map((b) => b.getBoundingClientRect().toJSON() as DOMRect))) {
      expect(Math.min(box.width, box.height)).toBeGreaterThanOrEqual(47.5)
    }
  })
})
