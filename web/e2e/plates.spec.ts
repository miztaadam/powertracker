import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { openOnWednesday, pastTheTapWindow, tabBar, tap, weightField } from './helpers'

const grit = GritDomain.getInstance()

// 1.17: counting plates on a stack machine (plate-count plan, Task 8 Step 6). A normal run writes the screenshots to
// test-results/screens/; UPDATE_SCREENSHOTS=1 rewrites the committed PNGs in docs/design/web/screens/.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

/**
 * Today scrolls the open card into view with a smooth scroll that can start a moment after the card shows: waits until
 * the screen has stood still for half a second, never part way through (or before it starts).
 */
async function stillScreen(page: Page) {
  await expect
    .poll(() =>
      page.locator('.shell-main').evaluate(
        (el) =>
          new Promise<boolean>((done) => {
            const before = el.scrollTop
            setTimeout(() => done(el.scrollTop === before), 500)
          }),
      ),
    )
    .toBe(true)
}

async function shoot(page: Page, name: string) {
  mkdirSync(outDir, { recursive: true })
  await page.evaluate(() => document.fonts.ready)
  await stillScreen(page)
  await page.screenshot({ path: resolve(outDir, `${name}.png`), animations: 'disabled' })
}

type Side = 'POWERLIFTING' | 'BODYBUILDING'

/** A set to seed: a weight in kg, or a plate number with `plates`. */
type Seeded = { weightKg: number; reps: number; plates?: boolean }

/**
 * Writes, straight into IndexedDB, `names` planned on Wednesdays on `side`'s side (a catalogue exercise with no row gets
 * one, as the app makes it), Lat pulldown's plate setting, a past session a week before and a session today with their
 * sets of Lat pulldown, and puts the app on that side; then reloads.
 */
async function seed(
  page: Page,
  side: Side,
  names: string[],
  what: { plateCount?: 'WHOLE' | 'HALF'; lastWeek?: Seeded[]; today?: Seeded[]; session?: boolean } = {},
) {
  const catalogue = names.flatMap((name) => {
    const e = grit.findInLibrary(name)
    return e ? [{ name: e.name, category: e.category, parentLift: e.parentLift ?? null, muscles: [...e.muscles], isBuiltIn: true, archived: false }] : []
  })
  await page.evaluate(
    async ({ side, names, catalogue, what }) => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'planItems', 'settings', 'sessions', 'sets'], 'readwrite')
      const settled = new Promise<void>((done, fail) => {
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      const add = <T>(store: string, row: T) =>
        new Promise<number>((done) => {
          const request = tx.objectStore(store).add(row)
          request.onsuccess = () => done(request.result as number)
        })
      const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
        const all = tx.objectStore('exercises').getAll()
        all.onsuccess = () => done(all.result)
      })
      for (const row of catalogue) {
        if (exercises.some((e) => e.name === row.name)) continue
        // The setting is written only when it is not off, as the app writes it.
        const plateCount = row.name === 'Lat pulldown' && what.plateCount ? { plateCount: what.plateCount } : {}
        exercises.push({ id: await add('exercises', { ...row, ...plateCount }), name: row.name })
      }
      names.forEach((name, order) => {
        const exerciseId = exercises.find((e) => e.name === name)!.id
        const item = side === 'BODYBUILDING' ? { sets: 3, reps: 8, repsHigh: 12 } : { sets: 3, reps: 8 }
        tx.objectStore('planItems').add({ weekday: 3, exerciseId, targetRpe: 8, order, mode: side, ...item })
      })
      tx.objectStore('settings').put({ key: 'trainingMode', value: side })
      const pulldown = exercises.find((e) => e.name === 'Lat pulldown')?.id
      const day = async (date: string, sets: { weightKg: number; reps: number; plates?: boolean }[]) => {
        const sessionId = await add('sessions', { date, bodyweightKg: null, readiness: null, mode: side })
        for (const s of sets) {
          await add('sets', { sessionId, exerciseId: pulldown, weightKg: s.weightKg, reps: s.reps, rpe: 8, failed: false, missPoint: null, isTopSet: false, ...(s.plates ? { unit: 'PLATES' } : {}) })
        }
      }
      if (what.lastWeek) await day('2026-11-04', what.lastWeek)
      if (what.today || what.session) await day('2026-11-11', what.today ?? [])
      await settled
      db.close()
    },
    { side, names, catalogue, what },
  )
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

const plateField = (page: Page) => page.getByRole('textbox', { name: 'Weight, plates' })
const sheetOf = (page: Page, name: string) => page.getByRole('dialog', { name: `Count plates on ${name}` })

/** Opens `name`'s row menu and taps Count plates on this machine; the sheet slides up and, after its moment, takes taps. */
async function openPlateSheet(page: Page, name: string) {
  await page.getByRole('button', { name: `Options for ${name}` }).click()
  await page.getByRole('menuitem', { name: 'Count plates on this machine' }).click()
  const sheet = sheetOf(page, name)
  await expect(sheet).toBeVisible()
  // Its choices take taps 300 ms after it has finished sliding up, as the skip sheet's answers do.
  await sheet.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
  await pastTheTapWindow(page)
  return sheet
}

/** What a screen reader hears for each logged set on the open card. */
const setLines = (page: Page) => page.locator('.gr-set .sr-only').allTextContents()

test('count plates end to end', async ({ page }) => {
  await openOnWednesday(page)
  await seed(page, 'POWERLIFTING', ['Lat pulldown'])
  await tap(page, 'Start session')
  await weightField(page).fill('60')
  await weightField(page).blur()
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()

  const sheet = await openPlateSheet(page, 'Lat pulldown')
  await expect(sheet.getByRole('heading', { name: 'Lat pulldown' })).toBeVisible()
  await expect(sheet.getByRole('radiogroup', { name: 'How does this machine show its weight?' })).toBeVisible()
  await expect(sheet.getByRole('radio', { name: 'In kg' })).toBeChecked()
  await sheet.getByRole('radio', { name: 'Whole plates' }).click()
  await expect(sheet.getByText('The coach starts this exercise fresh in plates. Your kg sets stay as they are.')).toBeVisible()
  await sheet.getByRole('button', { name: 'Done' }).click()
  await expect(sheetOf(page, 'Lat pulldown')).toBeHidden()

  // The field is empty and reads PLATE; + gives plate 1.
  await expect(plateField(page)).toHaveValue('')
  await expect(plateField(page)).toHaveAttribute('placeholder', 'plate')
  await expect(weightField(page)).toHaveCount(0)
  await expect(page.locator('.gr-stepper-field .eyebrow').first()).toHaveText(/plate/i)
  await page.getByRole('button', { name: 'Increase Plate' }).click()
  await expect(plateField(page)).toHaveValue('1')
  await plateField(page).fill('7')
  await plateField(page).blur()
  await tap(page, 'Log set 2')
  await expect(page.getByRole('button', { name: 'Log set 3' })).toBeVisible()
  expect(await setLines(page)).toEqual(['Set 1: 60 kg × 8 at RPE 8, done', 'Set 2: plate 7 × 8 at RPE 8, done'])
  await expect(page.locator('.gr-set-line').nth(1)).toContainText('plate 7')

  // After a reload: still plates, and the kg set still reads 60 kg.
  await page.reload()
  await expect(plateField(page)).toBeVisible()
  expect(await setLines(page)).toEqual(['Set 1: 60 kg × 8 at RPE 8, done', 'Set 2: plate 7 × 8 at RPE 8, done'])
  await expect(plateField(page)).toHaveValue('7')
})

test('half steps', async ({ page }) => {
  await openOnWednesday(page)
  await seed(page, 'POWERLIFTING', ['Lat pulldown'], { plateCount: 'WHOLE', session: true })
  await expect(plateField(page)).toBeVisible()
  await (await openPlateSheet(page, 'Lat pulldown')).getByRole('radio', { name: 'Half steps too' }).click()
  // Whole ↔ half is no change of unit: no note.
  await expect(sheetOf(page, 'Lat pulldown').getByTestId('plate-note')).toHaveCount(0)
  await sheetOf(page, 'Lat pulldown').getByRole('button', { name: 'Done' }).click()
  await plateField(page).fill('7')
  await plateField(page).blur()
  await page.getByRole('button', { name: 'Increase Plate' }).click()
  await expect(plateField(page)).toHaveValue('7.5')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  await expect(page.locator('.gr-set-line').first()).toContainText('plate 7½')
  expect(await setLines(page)).toEqual(['Set 1: plate 7 and a half × 8 at RPE 8, done'])

  // Back on whole plates: 7.5 is refused, and − and + from 7½ go to 7 and 8.
  await (await openPlateSheet(page, 'Lat pulldown')).getByRole('radio', { name: 'Whole plates' }).click()
  await sheetOf(page, 'Lat pulldown').getByRole('button', { name: 'Done' }).click()
  await expect(plateField(page)).toHaveValue('7.5')
  await page.getByRole('button', { name: 'Increase Plate' }).click()
  await expect(plateField(page)).toHaveValue('8')
  await plateField(page).fill('7.5')
  await plateField(page).blur()
  await expect(page.getByText('Whole plates only')).toBeVisible()
})

test('no plate item on Bench or on Assisted dip', async ({ page }) => {
  await openOnWednesday(page)
  await seed(page, 'POWERLIFTING', ['Bench', 'Assisted dip', 'Lat pulldown'])
  for (const name of ['Bench', 'Assisted dip']) {
    await page.getByRole('button', { name: `Options for ${name}` }).click()
    await expect(page.getByRole('menuitem')).toHaveText(['Skip today'])
    await page.keyboard.press('Escape')
  }
  await page.getByRole('button', { name: 'Options for Lat pulldown' }).click()
  await expect(page.getByRole('menuitem')).toHaveText(['Skip today', 'Count plates on this machine'])
  await expect(page.getByTestId('exercise-menu-plates')).toBeVisible()
})

test('a stand-in gets its row', async ({ page }) => {
  await openOnWednesday(page)
  await seed(page, 'POWERLIFTING', [], { session: true })
  await tap(page, 'Add exercise')
  const picker = page.getByRole('dialog', { name: /^(Add to |Add exercise)/ })
  await picker.getByRole('textbox', { name: 'Search exercises' }).fill('lat pulldown')
  await picker.getByRole('listitem', { name: 'Lat pulldown', exact: true }).getByRole('button').first().click()
  await expect(page.getByRole('heading', { name: 'Lat pulldown' })).toBeVisible()
  const rowsBefore = await page.evaluate(() => new Promise<number>((done) => {
    const request = indexedDB.open('grit')
    request.onsuccess = () => {
      const all = request.result.transaction('exercises').objectStore('exercises').getAll()
      all.onsuccess = () => {
        done(all.result.filter((e: { name: string }) => e.name === 'Lat pulldown').length)
        request.result.close()
      }
    }
  }))
  expect(rowsBefore).toBe(0)
  const sheet = await openPlateSheet(page, 'Lat pulldown')
  await sheet.getByRole('radio', { name: 'Whole plates' }).click()
  // The sheet stays while the card moves across to its new row, and the field turns into PLATE.
  await expect(plateField(page)).toBeVisible()
  await expect(sheetOf(page, 'Lat pulldown')).toBeVisible()
  await sheet.getByRole('button', { name: 'Done' }).click()
  const rows = await page.evaluate(() => new Promise<{ name: string; plateCount?: string }[]>((done) => {
    const request = indexedDB.open('grit')
    request.onsuccess = () => {
      const all = request.result.transaction('exercises').objectStore('exercises').getAll()
      all.onsuccess = () => {
        done(all.result.filter((e: { name: string }) => e.name === 'Lat pulldown'))
        request.result.close()
      }
    }
  }))
  expect(rows.map((r) => r.plateCount)).toEqual(['WHOLE'])
  await expect(page.getByRole('heading', { name: 'Lat pulldown' })).toHaveCount(1)
})

// The shots: Today with a plate-counted exercise (the design's sketch: the coach's chip in plates, one logged plate set,
// the PLATE stepper) on each side, and the sheet, dark and light.
for (const scheme of ['dark', 'light'] as const) {
  test.describe(`plates on Today (${scheme})`, () => {
    test.beforeEach(async ({ page }) => {
      await page.emulateMedia({ colorScheme: scheme })
    })

    for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
      test(`today-plates-${side.toLowerCase()}`, async ({ page }) => {
        await openOnWednesday(page)
        const lastWeek: Seeded[] = side === 'BODYBUILDING' ? [12, 12, 11].map((reps) => ({ weightKg: 7, reps, plates: true })) : [{ weightKg: 7, reps: 8, plates: true }]
        await seed(page, side, ['Lat pulldown'], { plateCount: 'WHOLE', lastWeek, today: [{ weightKg: 7, reps: 12, plates: true }] })
        await expect(page.getByText(side === 'BODYBUILDING' ? 'plate 7 × 8–12 @ 8' : /^plate 7 × \d+ @ 8$/)).toBeVisible()
        await expect(plateField(page)).toHaveValue('7')
        await expect(page.locator('.gr-set-line').first()).toContainText('plate 7')
        // Whether the open card's scroll into view moves the screen depends on when it fires against the chip coming in
        // (the bodybuilding card is taller than the phone): let it finish, then draw the screen from its top, every time.
        await stillScreen(page)
        await page.locator('.shell-main').evaluate((el) => el.scrollTo({ top: 0, behavior: 'instant' }))
        await shoot(page,`today-plates-${side.toLowerCase()}-${scheme}`)
      })
    }

    // The ⋮ menu on Lat pulldown, still in kg, planned and with no set yet: Skip today with Count plates under it.
    test('today-plates-menu', async ({ page }) => {
      await openOnWednesday(page)
      await seed(page, 'POWERLIFTING', ['Lat pulldown'], { session: true })
      await expect(weightField(page)).toBeVisible()
      await page.getByRole('button', { name: 'Options for Lat pulldown' }).click()
      await expect(page.getByRole('menuitem')).toHaveText(['Skip today', 'Count plates on this machine'])
      await page.getByRole('menu').evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
      await shoot(page, `today-plates-menu-${scheme}`)
    })

    test('plate-sheet', async ({ page }) => {
      await openOnWednesday(page)
      await seed(page, 'POWERLIFTING', ['Lat pulldown'], { today: [{ weightKg: 40, reps: 8 }] })
      const sheet = await openPlateSheet(page, 'Lat pulldown')
      await sheet.getByRole('radio', { name: 'Whole plates' }).click()
      await expect(sheet.getByTestId('plate-note')).toBeVisible()
      await shoot(page, `plate-sheet-${scheme}`)
    })
  })
}
