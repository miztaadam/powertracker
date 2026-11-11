import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'

// Plan and Settings with the same sample story as the Android mockups (docs/design/screens/plan-dark.png,
// settings-dark.png). A normal run writes the screenshots to test-results/; UPDATE_SCREENSHOTS=1 rewrites
// the committed PNGs in docs/design/web/screens/. Look at them after a visual change.
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

/** The plan as the screen reads it out: one line per item, per day. */
const rowsOf = (page: Page, day: string) =>
  page
    .getByRole('region', { name: day })
    .getByRole('listitem')
    .evaluateAll((rows) => rows.map((row) => row.getAttribute('aria-label')))

/** Buttons, links and toggle chips smaller than 48 px in either direction (to within sub-pixel rounding). */
const smallTargets = (page: Page) =>
  page.evaluate(() =>
    Array.from(document.querySelectorAll<HTMLElement>('button, a[href], .gr-chip, .gr-segment'))
      .map((el) => ({ el, box: el.getBoundingClientRect() }))
      .filter(({ box }) => box.width < 47.5 || box.height < 47.5)
      .map(({ el, box }) => `${el.className} ${el.textContent} ${Math.round(box.width)}×${Math.round(box.height)}`),
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
    await shoot(page, `settings-${scheme}`, 1390)

    // Back is one step back in history, not a new entry.
    await page.getByRole('button', { name: 'Back', exact: true }).click()
    await expect(page).toHaveURL(/#\/plan$/)
    await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
    await page.goForward()
    await expect(page).toHaveURL(/#\/settings$/)
  })
}

test.describe('on a 375 px iPhone', () => {
  test.use({ viewport: narrowPhone })

  test('the plan is edited through its dialogs and survives a reload', async ({ page }) => {
    await openPlan(page)
    const monday = page.getByRole('region', { name: 'Monday' })
    const dialog = page.getByRole('dialog')

    await monday.getByRole('button', { name: 'Add' }).click()
    await expect(page.getByRole('dialog', { name: 'Add to Monday' })).toBeVisible()
    await dialog.getByRole('textbox', { name: 'Search exercises' }).fill('squat')
    await dialog.getByRole('button', { name: 'Squat', exact: true }).click()
    await expect(dialog.getByText('Selected: Squat')).toBeVisible()
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Squat  3 × 5 @ 8'])

    // A custom variation, created from inside the picker.
    await monday.getByRole('button', { name: 'Add' }).click()
    await dialog.getByRole('button', { name: 'Add custom exercise' }).click()
    const custom = page.getByRole('dialog', { name: 'Add custom exercise' })
    await custom.getByRole('textbox', { name: 'Name' }).fill('Tempo squat')
    await custom.getByText('Variation').click()
    await custom.getByText('Squat', { exact: true }).click()
    expect(await overflowing(page)).toEqual([])
    expect(await smallTargets(page)).toEqual([])
    await custom.getByRole('button', { name: 'Create' }).click()
    await expect(dialog.getByText('Selected: Tempo squat')).toBeVisible()
    await dialog.getByRole('button', { name: 'Increase Sets' }).click()
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect(monday.getByRole('listitem')).toHaveCount(2)

    await monday.getByRole('button', { name: 'Move up' }).last().click()
    await expect(monday.getByRole('listitem').first()).toHaveAttribute('aria-label', 'Tempo squat  4 × 5 @ 8')

    await page.getByRole('group', { name: 'Deadlift' }).getByRole('button', { name: 'Set best' }).click()
    await dialog.getByRole('textbox', { name: 'Estimated 1RM (kg)' }).fill('501')
    await expect(dialog.getByRole('button', { name: 'Save' })).toBeDisabled()
    await dialog.getByRole('textbox', { name: 'Estimated 1RM (kg)' }).fill('220')
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect(dialog).toHaveCount(0)

    await page.reload()
    await expect(page.getByRole('region', { name: 'Monday' }).getByRole('listitem')).toHaveCount(2)
    await expect.poll(() => rowsOf(page, 'Monday')).toEqual(['Tempo squat  4 × 5 @ 8', 'Squat  3 × 5 @ 8'])
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
