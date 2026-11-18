import { act, cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { encodeBackup, exportBackup } from '../../backup/backup'
import { planFor, resolveCatalogue } from '../../db/catalogue'
import { updateSettings, type GritDb } from '../../db/db'
import { setTapHaptics } from '../../platform/haptics'
import { checkInfoButtonOpensGuide } from '../../test/guideChecks'
import { TODAY, exerciseNamed, plan, todayFixture } from '../../test/todayFixture'
import { PlanScreen } from './PlanScreen'
import { pastTheTapWindow } from '../../test/tapWindow'
import { createYourOwn } from '../../test/wholeList'
import {
  OWN_NAME,
  createForm,
  expectCustomExerciseEmpty,
  expectCustomExerciseKept,
  expectSaveFailedInTheForm,
  fillCustomExercise,
  openCreateForm,
  tapCreate,
} from '../../test/customExerciseChecks'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes.
afterEach(async () => {
  vi.useRealTimers()
  cleanup()
  await fx.dispose()
})

const show = (onOpenSettings: () => void = () => {}) => render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={onOpenSettings} />)
const day = (name: string) => screen.findByRole('region', { name })
const dialog = () => within(screen.getByRole('dialog'))
const rowsOf = (section: HTMLElement) => within(section).queryAllByRole('listitem').map((row) => row.getAttribute('aria-label'))
/** The part of a picker row that picks the exercise, once the row is drawn. */
const pickerRow = async (name: string) => within(await dialog().findByRole('listitem', { name })).getAllByRole('button')[0]!
const exerciseRowsNamed = async (name: string) => (await db.exercises.toArray()).filter((e) => e.name === name)
const storedPlan = async () => (await db.planItems.toArray()).map(({ id: _id, ...rest }) => rest)

describe('PlanScreen', () => {
  it('addPlanItem_showsUnderWeekday', async () => {
    show()
    expect(screen.getByRole('heading', { level: 1, name: 'Plan' })).toBeInTheDocument()
    const monday = await day('Monday')
    expect(within(monday).getByText('Rest day')).toBeInTheDocument()

    await userEvent.click(within(monday).getByRole('button', { name: 'Add' }))
    expect(screen.getByRole('dialog', { name: 'Add to Monday' })).toBeInTheDocument()
    await userEvent.type(dialog().getByRole('textbox', { name: 'Search exercises' }), 'Squat')
    // Picking opens the form with a compound lift's 3 sets × 5 reps @ the default RPE 8; Add stores it.
    await userEvent.click(await pickerRow('Squat'))
    await userEvent.click((await addForm()).getByRole('button', { name: 'Add' }))

    await waitFor(() => expect(rowsOf(monday)).toEqual(['Squat  3 × 5 @ 8']))
    expect(within(monday).getByText('1 exercise · 3 sets')).toBeInTheDocument()
    expect(rowsOf(await day('Wednesday'))).toEqual([])
    expect(rowsOf(await day('Friday'))).toEqual([])
    expect(screen.queryByRole('dialog')).toBeNull()
    const squat = await exerciseNamed(db, 'Squat')
    expect(await storedPlan()).toEqual([{ weekday: 1, exerciseId: squat.id, sets: 3, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' }])
  })

  it('moreMenu_editAndDelete', async () => {
    const bench = await exerciseNamed(db, 'Bench')
    await plan(db, bench.id, { weekday: 1, sets: 4, reps: 6, targetRpe: 7.5 })
    show()
    const monday = await day('Monday')
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Bench  4 × 6 @ 7.5']))

    await userEvent.click(within(monday).getByRole('button', { name: 'More' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Edit' }))
    expect(screen.getByRole('dialog', { name: 'Edit Monday' })).toBeInTheDocument()
    expect(dialog().getByRole('group', { name: 'Exercise' })).toHaveTextContent('Bench')
    expect(dialog().getByRole('group', { name: 'Sets' })).toHaveTextContent('4')
    expect(dialog().getByRole('group', { name: 'Reps' })).toHaveTextContent('6')
    expect(dialog().getByRole('group', { name: 'RPE' })).toHaveTextContent('7.5')
    await userEvent.click(dialog().getByRole('button', { name: 'Cancel' }))
    expect(await storedPlan()).toHaveLength(1)

    // Editing keeps the row's place and changes its numbers.
    await userEvent.click(within(monday).getByRole('button', { name: 'More' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Edit' }))
    await userEvent.click(dialog().getByRole('button', { name: 'Increase Sets' }))
    await userEvent.click(dialog().getByRole('button', { name: 'Save' }))
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Bench  5 × 6 @ 7.5']))

    await userEvent.click(within(monday).getByRole('button', { name: 'More' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Delete' }))
    await waitFor(() => expect(rowsOf(monday)).toEqual([]))
    expect(await storedPlan()).toEqual([])
  })

  it('moreMenu_closesOnEscapeWithoutChanging', async () => {
    await plan(db, (await exerciseNamed(db, 'Bench')).id, { weekday: 1 })
    show()
    const monday = await day('Monday')
    await userEvent.click(await within(monday).findByRole('button', { name: 'More' }))
    expect(screen.getByRole('menu')).toBeInTheDocument()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('menu')).toBeNull()
    expect(await storedPlan()).toHaveLength(1)
  })

  it('upDown_reorderWithinTheDay', async () => {
    for (const [order, name] of ['Squat', 'Bench', 'Row'].entries()) await plan(db, (await exerciseNamed(db, name)).id, { weekday: 1, order })
    show()
    const monday = await day('Monday')
    await waitFor(() => expect(rowsOf(monday)).toHaveLength(3))
    const up = (row: number) => within(monday).getAllByRole('button', { name: 'Move up' })[row]!
    const down = (row: number) => within(monday).getAllByRole('button', { name: 'Move down' })[row]!
    expect(up(0)).toBeDisabled()
    expect(down(2)).toBeDisabled()

    await userEvent.click(down(0))
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Bench  5 × 5 @ 8', 'Squat  5 × 5 @ 8', 'Row  5 × 5 @ 8']))
    await userEvent.click(up(2))
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Bench  5 × 5 @ 8', 'Row  5 × 5 @ 8', 'Squat  5 × 5 @ 8']))
    // A new item goes after the last one.
    await userEvent.click(within(monday).getByRole('button', { name: 'Add' }))
    await userEvent.click(await pickerRow('Deadlift'))
    await userEvent.click((await addForm()).getByRole('button', { name: 'Add' }))
    await waitFor(() => expect(rowsOf(monday)[3]).toBe('Deadlift  3 × 5 @ 8'))
  })

  it('sections_trainingDaysPlusPlannedDays_inWeekOrder', async () => {
    await plan(db, (await exerciseNamed(db, 'Squat')).id, { weekday: 7 })
    show()
    await day('Sunday')
    expect(screen.getAllByRole('region').map((r) => r.getAttribute('aria-label'))).toEqual(['Monday', 'Wednesday', 'Friday', 'Sunday'])
  })

  it('customExercise_isCreatedAndAdded', async () => {
    show()
    await userEvent.click(within(await day('Wednesday')).getByRole('button', { name: 'Add' }))
    await userEvent.click(await createYourOwn(dialog()))
    const custom = () => within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
    expect(custom().getByRole('button', { name: 'Create' })).toBeDisabled()
    await userEvent.type(custom().getByRole('textbox', { name: 'Name' }), '  Cluster squat ')
    // A variation needs its parent lift.
    await userEvent.click(custom().getByRole('checkbox', { name: 'Variation' }))
    expect(custom().getByRole('button', { name: 'Create' })).toBeDisabled()
    await userEvent.click(custom().getByRole('checkbox', { name: 'Squat' }))
    await userEvent.type(custom().getByRole('textbox', { name: 'Muscles (comma-separated)' }), 'quads, , glutes')
    await userEvent.click(custom().getByRole('button', { name: 'Create' }))
    await userEvent.click((await addForm()).getByRole('button', { name: 'Add' }))

    // Made, and added to the day like a picked one: a variation is a compound lift, 3 × 5.
    await waitFor(async () => expect(rowsOf(await day('Wednesday'))).toEqual(['Cluster squat  3 × 5 @ 8']))
    expect(screen.queryByRole('dialog')).toBeNull()
    const made = await db.exercises.where('name').equals('Cluster squat').toArray()
    expect(made).toHaveLength(1)
    expect(made[0]).toMatchObject({ category: 'VARIATION', parentLift: 'SQUAT', muscles: ['quads', 'glutes'], isBuiltIn: false, archived: false })
    expect(await storedPlan()).toEqual([{ weekday: 3, exerciseId: made[0]!.id, sets: 3, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' }])
  })

  it('edit_changeExercise_swapsItThroughThePicker_onSave', async () => {
    const bench = await exerciseNamed(db, 'Bench')
    await plan(db, bench.id, { weekday: 1, sets: 4, reps: 6, targetRpe: 7.5 })
    // A library exercise with no row yet.
    await db.exercises.delete((await exerciseNamed(db, 'Curl')).id)
    const before = await db.exercises.toArray()
    show()
    const monday = await day('Monday')
    const edit = async () => {
      await userEvent.click(await within(monday).findByRole('button', { name: 'More' }))
      await userEvent.click(screen.getByRole('menuitem', { name: 'Edit' }))
    }
    const editDialog = () => within(screen.getByRole('dialog', { name: 'Edit Monday' }))

    // Changed, then cancelled: nothing is stored, not even the exercise's row.
    await edit()
    await userEvent.click(editDialog().getByRole('button', { name: 'Change' }))
    await userEvent.click(within(await within(screen.getByRole('dialog', { name: 'Change exercise' })).findByRole('listitem', { name: 'Curl' })).getAllByRole('button')[0]!)
    expect(screen.getAllByRole('dialog')).toHaveLength(1)
    expect(editDialog().getByRole('group', { name: 'Exercise' })).toHaveTextContent('Curl')
    await userEvent.click(editDialog().getByRole('button', { name: 'Cancel' }))
    expect(await db.exercises.toArray()).toEqual(before)
    expect(rowsOf(monday)).toEqual(['Bench  4 × 6 @ 7.5'])

    // Changed and saved: the row comes with the item, which keeps its place and its numbers.
    await edit()
    await userEvent.click(editDialog().getByRole('button', { name: 'Change' }))
    await userEvent.click(within(await within(screen.getByRole('dialog', { name: 'Change exercise' })).findByRole('listitem', { name: 'Curl' })).getAllByRole('button')[0]!)
    await userEvent.click(editDialog().getByRole('button', { name: 'Decrease Sets' }))
    await userEvent.click(editDialog().getByRole('button', { name: 'Save' }))
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Curl  3 × 6 @ 7.5']))
    const curl = await exerciseNamed(db, 'Curl')
    expect(curl.isBuiltIn).toBe(true)
    expect(await storedPlan()).toEqual([{ weekday: 1, exerciseId: curl.id, sets: 3, reps: 6, targetRpe: 7.5, order: 0 }])
  })

  it('infoButton_opensGuide_fromEveryPlace', async () => {
    await plan(db, (await exerciseNamed(db, 'Bench')).id, { weekday: 1 })
    // The lifter's own exercise keeps the button; its sheet says there is no guide.
    const sled = (await db.exercises.add({ name: 'Tyre flip', category: 'ACCESSORY', parentLift: null, muscles: [], isBuiltIn: false, archived: false })) as number
    await plan(db, sled, { weekday: 1, order: 1 })
    show()
    const monday = await day('Monday')
    await waitFor(() => expect(rowsOf(monday)).toHaveLength(2))
    const before = await storedPlan()

    await checkInfoButtonOpensGuide('Bench')
    await checkInfoButtonOpensGuide('Tyre flip')

    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await storedPlan()).toEqual(before)
  })

  it('baseline_rejectsInfinityAndOver500', async () => {
    show()
    const bests = await screen.findByRole('group', { name: 'Squat' })
    await userEvent.click(within(bests).getByRole('button', { name: 'Set best' }))
    expect(screen.getByRole('dialog', { name: 'Known best: Squat' })).toBeInTheDocument()
    const field = dialog().getByRole('textbox', { name: 'Estimated 1RM (kg)' })
    expect(field).toHaveAttribute('inputmode', 'decimal')
    for (const text of ['Infinity', '1e400', 'NaN', '501', '0', '-5']) {
      await userEvent.clear(field)
      await userEvent.type(field, text)
      expect(dialog().getByRole('button', { name: 'Save' })).toBeDisabled()
    }
    await userEvent.clear(field)
    await userEvent.type(field, '182,5')
    await userEvent.click(dialog().getByRole('button', { name: 'Save' }))

    await waitFor(async () => expect(await db.baselines.toArray()).toHaveLength(1))
    const squat = await exerciseNamed(db, 'Squat')
    expect(await db.baselines.toArray()).toEqual([{ exerciseId: squat.id, e1rmKg: 182.5, date: TODAY }])
    // Every stored number is finite, so the backup still encodes.
    expect(encodeBackup(await exportBackup(db, 0))).toContain('182.5')
  })

  it('settingsButton_opensSettings', async () => {
    const onOpenSettings = vi.fn()
    show(onOpenSettings)
    await userEvent.click(screen.getByRole('button', { name: 'Settings' }))
    expect(onOpenSettings).toHaveBeenCalledOnce()
  })

  it('failedSave_saysSo', async () => {
    show()
    vi.spyOn(db.planItems, 'add').mockRejectedValue(new Error('disk full'))
    await userEvent.click(within(await day('Monday')).getByRole('button', { name: 'Add' }))
    await userEvent.click(await pickerRow('Squat'))
    await userEvent.click((await addForm()).getByRole('button', { name: 'Add' }))
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    expect(await storedPlan()).toEqual([])
  })

  // ---- Strict press: the known bests ----

  it('knownBests_includeStrictPress_onceItHasARow', async () => {
    show()
    await screen.findByRole('group', { name: 'Squat' })
    expect(screen.queryByRole('group', { name: 'Strict press' })).toBeNull()

    await resolveCatalogue(db, 'Strict press')

    // It is a main lift like the other three: it can be given a known best.
    expect(await screen.findByRole('group', { name: 'Strict press' })).toBeInTheDocument()
    for (const lift of ['Squat', 'Bench', 'Deadlift']) expect(screen.getByRole('group', { name: lift })).toBeInTheDocument()
  })
})

// ---- 1.12: picking on Plan's Add opens the sets and reps form; Add stores what it shows ----

/** The add form, once a pick has opened it: the dialog with the Sets stepper (the picker has the same title). */
const addFormNow = async () => within((await screen.findByRole('group', { name: 'Sets' })).closest<HTMLElement>('[role="dialog"]')!)
/** The add form, with the clock moved past the moment in which it ignores taps (the second tap of a double tap). */
const addForm = async () => {
  const form = await addFormNow()
  pastTheTapWindow()
  return form
}
/** A tap on the scrim around the open add form: outside it. */
const tapOutside = async () => userEvent.click((await screen.findByRole('group', { name: 'Sets' })).closest<HTMLElement>('[role="dialog"]')!.parentElement!)
const formOpen = () => screen.queryByRole('group', { name: 'Sets' }) != null
/** Opens Add on `day` and picks `name` from the picker. */
async function pickOn(day: string, name: string) {
  await userEvent.click(within(await screen.findByRole('region', { name: day })).getByRole('button', { name: 'Add' }))
  await userEvent.type(dialog().getByRole('textbox', { name: 'Search exercises' }), name)
  await userEvent.click(await pickerRow(name))
}
/** Fills in the custom exercise form opened by "Create your own", and creates it. */
async function createOwn(name: string, variationOf?: string) {
  await userEvent.click(await createYourOwn())
  const custom = () => within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
  await userEvent.type(custom().getByRole('textbox', { name: 'Name' }), name)
  if (variationOf) {
    await userEvent.click(custom().getByRole('checkbox', { name: 'Variation' }))
    await userEvent.click(custom().getByRole('checkbox', { name: variationOf }))
  }
  await userEvent.click(custom().getByRole('button', { name: 'Create' }))
}

describe('PlanScreen add form', () => {
  afterEach(() => {
    setTapHaptics(false)
    Reflect.deleteProperty(navigator, 'vibrate')
  })

  it('planAdd_pick_opensFormWithDefaults', async () => {
    // A default RPE that is not the fallback, so the form is seen to read it.
    await updateSettings(db, (s) => ({ ...s, defaultRpe: 7 }))
    show()
    await pickOn('Monday', 'Squat')

    // A compound lift: 3 × 5 at the default RPE, in a form titled for the day, with Add in place of Save.
    const form = await addForm()
    expect(screen.getByRole('dialog', { name: 'Add to Monday' })).toHaveTextContent('Sets')
    expect(form.getByRole('group', { name: 'Exercise' })).toHaveTextContent('Squat')
    expect(form.getByRole('group', { name: 'Sets' })).toHaveTextContent('3')
    expect(form.getByRole('group', { name: 'Reps' })).toHaveTextContent('5')
    expect(form.getByRole('group', { name: 'RPE' })).toHaveTextContent('7')
    expect(form.queryByRole('group', { name: 'Reps from' })).toBeNull()
    expect(form.getByRole('button', { name: 'Add' })).toBeInTheDocument()
    expect(form.queryByRole('button', { name: 'Save' })).toBeNull()
    expect(screen.getAllByRole('dialog')).toHaveLength(1)
    expect(await storedPlan()).toEqual([])

    // An accessory: 3 × 10.
    await userEvent.click(form.getByRole('button', { name: 'Cancel' }))
    await pickOn('Monday', 'Curl')
    const accessory = await addForm()
    expect(accessory.getByRole('group', { name: 'Exercise' })).toHaveTextContent('Curl')
    expect(accessory.getByRole('group', { name: 'Reps' })).toHaveTextContent('10')
    expect(accessory.getByRole('group', { name: 'RPE' })).toHaveTextContent('7')
  })

  it('planAdd_changeNumbers_addStoresThem', async () => {
    const squat = await exerciseNamed(db, 'Squat')
    await plan(db, squat.id, { weekday: 1, order: 0 })
    show()
    const monday = await day('Monday')
    await waitFor(() => expect(rowsOf(monday)).toHaveLength(1))
    await pickOn('Monday', 'Bench')
    const form = await addForm()
    await userEvent.click(form.getByRole('button', { name: 'Increase Sets' }))
    await userEvent.click(form.getByRole('button', { name: 'Increase Reps' }))
    await userEvent.click(form.getByRole('button', { name: 'Decrease RPE' }))
    expect(form.getByRole('group', { name: 'Sets' })).toHaveTextContent('4')
    expect(form.getByRole('group', { name: 'Reps' })).toHaveTextContent('6')
    expect(form.getByRole('group', { name: 'RPE' })).toHaveTextContent('7.5')
    // Nothing is stored before Add.
    expect(await storedPlan()).toHaveLength(1)

    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    // At the end of the day, with exactly those numbers.
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Squat  5 × 5 @ 8', 'Bench  4 × 6 @ 7.5']))
    expect(screen.queryByRole('dialog')).toBeNull()
    const bench = await exerciseNamed(db, 'Bench')
    expect((await storedPlan()).find((p) => p.exerciseId === bench.id)).toEqual({ weekday: 1, exerciseId: bench.id, sets: 4, reps: 6, targetRpe: 7.5, order: 1, mode: 'POWERLIFTING' })
  })

  it('planAdd_cancel_storesNothing', async () => {
    // A library exercise with no row yet: Change to it, then Cancel, stores no row either.
    await db.exercises.delete((await exerciseNamed(db, 'Curl')).id)
    const before = await db.exercises.toArray()
    show()
    const monday = await day('Monday')

    // Cancel, Escape: nothing stored.
    await pickOn('Monday', 'Squat')
    await userEvent.click((await addForm()).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    await pickOn('Monday', 'Squat')
    await addForm()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).toBeNull()

    // Change, then Cancel: nothing added, and the changed-to exercise gets no row.
    await pickOn('Monday', 'Squat')
    await userEvent.click((await addForm()).getByRole('button', { name: 'Change' }))
    await userEvent.click(within(await within(screen.getByRole('dialog', { name: 'Change exercise' })).findByRole('listitem', { name: 'Curl' })).getAllByRole('button')[0]!)
    const changed = await addForm()
    expect(changed.getByRole('group', { name: 'Exercise' })).toHaveTextContent('Curl')
    await userEvent.click(changed.getByRole('button', { name: 'Cancel' }))
    expect(await db.exercises.toArray()).toEqual(before)

    // Change to one of your own, made there, then Cancel: it stays in the library, and nothing is added.
    await pickOn('Monday', 'Squat')
    await userEvent.click((await addForm()).getByRole('button', { name: 'Change' }))
    await createOwn('Tyre flip')
    await waitFor(async () => expect((await addForm()).getByRole('group', { name: 'Exercise' })).toHaveTextContent('Tyre flip'))
    await userEvent.click((await addForm()).getByRole('button', { name: 'Cancel' }))
    expect(await db.exercises.where('name').equals('Tyre flip').count()).toBe(1)

    expect(screen.queryByRole('dialog')).toBeNull()
    expect(rowsOf(monday)).toEqual([])
    expect(await storedPlan()).toEqual([])
  })

  it('planAdd_changeExercise_keepsTheNumbers_andAddsTheNewOne', async () => {
    show()
    await pickOn('Monday', 'Squat')
    const form = await addForm()
    await userEvent.click(form.getByRole('button', { name: 'Increase Sets' }))
    await userEvent.click(form.getByRole('button', { name: 'Change' }))
    await userEvent.click(within(await within(screen.getByRole('dialog', { name: 'Change exercise' })).findByRole('listitem', { name: 'Deadlift' })).getAllByRole('button')[0]!)
    expect(form.getByRole('group', { name: 'Exercise' })).toHaveTextContent('Deadlift')
    expect(form.getByRole('group', { name: 'Sets' })).toHaveTextContent('4')
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    await waitFor(async () => expect(rowsOf(await day('Monday'))).toEqual(['Deadlift  4 × 5 @ 8']))
  })

  it('planAdd_bodybuilding_storesRange', async () => {
    await updateSettings(db, (s) => ({ ...s, trainingMode: 'BODYBUILDING', defaultRpe: 7, bodybuildingTrainingDays: [2, 3, 6] }))
    const curl = await exerciseNamed(db, 'Curl')
    show()

    // 3 × 8–12 @ 8 by default, whatever the default RPE, and Add stores it as shown.
    await pickOn('Wednesday', 'Curl')
    const form = await addForm()
    expect(form.getByRole('group', { name: 'Sets' })).toHaveTextContent('3')
    expect(form.getByRole('group', { name: 'Reps from' })).toHaveTextContent('8')
    expect(form.getByRole('group', { name: 'Reps to' })).toHaveTextContent('12')
    expect(form.getByRole('group', { name: 'RPE' })).toHaveTextContent('8')
    expect(form.queryByRole('group', { name: 'Reps' })).toBeNull()
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    await waitFor(async () => expect(rowsOf(await day('Wednesday'))).toEqual(['Curl  3 × 8–12 @ 8']))
    expect((await planFor(db, 'BODYBUILDING')).map((p) => [p.weekday, p.exerciseId, p.sets, p.reps, p.repsHigh, p.targetRpe, p.mode])).toEqual([[3, curl.id, 3, 8, 12, 8, 'BODYBUILDING']])

    // A changed range is stored as chosen.
    await pickOn('Saturday', 'Curl')
    const changed = await addForm()
    await userEvent.click(changed.getByRole('button', { name: 'Decrease Reps from' }))
    await userEvent.click(changed.getByRole('button', { name: 'Increase Reps to' }))
    await userEvent.click(changed.getByRole('button', { name: 'Increase Reps to' }))
    await userEvent.click(changed.getByRole('button', { name: 'Add' }))
    await waitFor(async () => expect(rowsOf(await day('Saturday'))).toEqual(['Curl  3 × 7–14 @ 8']))
    expect((await planFor(db, 'BODYBUILDING')).find((p) => p.weekday === 6)).toMatchObject({ sets: 3, reps: 7, repsHigh: 14, targetRpe: 8, mode: 'BODYBUILDING' })
    expect(await planFor(db, 'POWERLIFTING')).toEqual([])
  })

  it('planAdd_ownExercise_opensForm', async () => {
    show()
    await userEvent.click(within(await day('Wednesday')).getByRole('button', { name: 'Add' }))
    await createOwn('Cluster squat', 'Squat')

    // Made first, then the form: a variation is a compound lift, 3 × 5.
    const form = await addForm()
    expect(screen.getByRole('dialog', { name: 'Add to Wednesday' })).toHaveTextContent('Cluster squat')
    expect(form.getByRole('group', { name: 'Exercise' })).toHaveTextContent('Cluster squat')
    expect(form.getByRole('group', { name: 'Sets' })).toHaveTextContent('3')
    expect(form.getByRole('group', { name: 'Reps' })).toHaveTextContent('5')
    const made = await db.exercises.where('name').equals('Cluster squat').toArray()
    expect(made).toHaveLength(1)
    expect(await storedPlan()).toEqual([])

    await userEvent.click(form.getByRole('button', { name: 'Increase Sets' }))
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    await waitFor(async () => expect(rowsOf(await day('Wednesday'))).toEqual(['Cluster squat  4 × 5 @ 8']))
    expect(await storedPlan()).toEqual([{ weekday: 3, exerciseId: made[0]!.id, sets: 4, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' }])

    // Cancelled: the exercise stays in the library, and nothing more is added.
    await userEvent.click(within(await day('Wednesday')).getByRole('button', { name: 'Add' }))
    await createOwn('Tyre flip')
    await userEvent.click((await addForm()).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await db.exercises.where('name').equals('Tyre flip').count()).toBe(1)
    expect(await storedPlan()).toHaveLength(1)
  })

  it('planAdd_ownExerciseFails_opensNoForm', async () => {
    show()
    vi.spyOn(db.exercises, 'add').mockRejectedValue(new Error('disk full'))
    await userEvent.click(within(await day('Wednesday')).getByRole('button', { name: 'Add' }))
    await createOwn('Cluster squat', 'Squat')
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    // Since 1.12.2 the picker and its create form stay open, to try again; no add form opens.
    expect(screen.getByRole('dialog', { name: 'Add custom exercise' })).toBeInTheDocument()
    expect(screen.queryByRole('group', { name: 'Sets' })).toBeNull()
    expect(await storedPlan()).toEqual([])
  })

  it('planAdd_ownExerciseFails_createFormKeepsEverything_saysSoInIt_andASecondCreateWorks', async () => {
    show()
    const add = vi.spyOn(db.exercises, 'add').mockRejectedValueOnce(new Error('disk full'))
    await userEvent.click(within(await day('Monday')).getByRole('button', { name: 'Add' }))
    await openCreateForm()
    await fillCustomExercise()
    await tapCreate()
    await expectSaveFailedInTheForm()
    expectCustomExerciseKept()
    expect(formOpen()).toBe(false)
    expect(await exerciseRowsNamed(OWN_NAME)).toEqual([])

    // Once the disk has room, Create works again: stored as typed, and the add form opens on it.
    await tapCreate()
    expect((await addFormNow()).getByRole('group', { name: 'Exercise' })).toHaveTextContent(OWN_NAME)
    expect(createForm()).toBeNull()
    expect(add).toHaveBeenCalledTimes(2)
    expect(await exerciseRowsNamed(OWN_NAME)).toEqual([expect.objectContaining({ category: 'VARIATION', parentLift: 'BENCH', muscles: ['Lats', 'Grip'] })])
  })

  it('planAdd_ownExerciseCreated_theNextCreateFormStartsEmpty', async () => {
    show()
    const monday = await day('Monday')
    await userEvent.click(within(monday).getByRole('button', { name: 'Add' }))
    await userEvent.type(dialog().getByRole('textbox', { name: 'Search exercises' }), 'zzz')
    await openCreateForm()
    await fillCustomExercise()
    await tapCreate()
    await userEvent.click((await addForm()).getByRole('button', { name: 'Cancel' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())

    await userEvent.click(within(monday).getByRole('button', { name: 'Add' }))
    await userEvent.type(dialog().getByRole('textbox', { name: 'Search exercises' }), 'zzz')
    await openCreateForm()
    expectCustomExerciseEmpty('zzz')
  })

  it('planAdd_ownExerciseFails_afterTheFormWasCancelled_thePickerSaysSo', async () => {
    show()
    let fail!: () => void
    vi.spyOn(db.exercises, 'add').mockImplementation((() => new Promise((_, reject) => (fail = () => reject(new Error('disk full'))))) as never)
    await userEvent.click(within(await day('Monday')).getByRole('button', { name: 'Add' }))
    await openCreateForm()
    await fillCustomExercise()
    await tapCreate()
    await userEvent.click(within(createForm()!).getByRole('button', { name: 'Cancel' }))
    await act(async () => fail())
    // The form is gone, so the picker says it over its list.
    expect(await within(screen.getByRole('dialog', { name: 'Add to Monday' })).findByText("Couldn't save — try again.")).toBeInTheDocument()
    expect(screen.getAllByText("Couldn't save — try again.")).toHaveLength(1)
  })

  it('planAdd_ownExerciseFails_afterThePickerWasClosed_planSaysSo', async () => {
    show()
    let fail!: () => void
    vi.spyOn(db.exercises, 'add').mockImplementation((() => new Promise((_, reject) => (fail = () => reject(new Error('disk full'))))) as never)
    await userEvent.click(within(await day('Monday')).getByRole('button', { name: 'Add' }))
    await openCreateForm()
    await fillCustomExercise()
    await tapCreate()
    await userEvent.click(within(createForm()!).getByRole('button', { name: 'Cancel' }))
    await userEvent.click(screen.getByRole('button', { name: 'Close' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    await act(async () => fail())
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
  })

  it('planChange_toOwnExerciseFails_createFormKeepsEverything_saysSoInIt', async () => {
    show()
    await pickOn('Monday', 'Squat')
    const form = await addForm()
    vi.spyOn(db.exercises, 'add').mockRejectedValueOnce(new Error('disk full'))
    await userEvent.click(form.getByRole('button', { name: 'Change' }))
    await openCreateForm()
    await fillCustomExercise()
    await tapCreate()
    await expectSaveFailedInTheForm()
    expectCustomExerciseKept()

    await tapCreate()
    await waitFor(() => expect(createForm()).toBeNull())
    expect(screen.queryByRole('dialog', { name: 'Change exercise' })).toBeNull()
    expect((await addFormNow()).getByRole('group', { name: 'Exercise' })).toHaveTextContent(OWN_NAME)
    expect(await storedPlan()).toEqual([])
  })

  it('planAdd_pick_swapsStraightToTheForm_focusInside_andBackToAddOnClose', async () => {
    show()
    const monday = await day('Monday')
    const add = within(monday).getByRole('button', { name: 'Add' })
    add.focus()
    await userEvent.click(add)
    await userEvent.type(dialog().getByRole('textbox', { name: 'Search exercises' }), 'Squat')
    const row = await pickerRow('Squat')

    // One act, no waiting: the form is there, the picker gone, and the focus inside the form, never back on Add.
    const focused: Element[] = []
    const track = (e: FocusEvent) => focused.push(e.target as Element)
    document.addEventListener('focusin', track)
    act(() => row.click())
    document.removeEventListener('focusin', track)
    const dialogs = screen.getAllByRole('dialog')
    expect(dialogs).toHaveLength(1)
    expect(within(dialogs[0]!).getByRole('group', { name: 'Sets' })).toBeInTheDocument()
    expect(screen.queryByRole('textbox', { name: 'Search exercises' })).toBeNull()
    expect(dialogs[0]!.contains(document.activeElement)).toBe(true)
    expect(focused).not.toContain(add)

    // Closed, by Cancel or by Add: the focus is back on the day's Add.
    pastTheTapWindow()
    await userEvent.click(within(dialogs[0]!).getByRole('button', { name: 'Cancel' }))
    expect(document.activeElement).toBe(add)
    add.focus()
    await pickOn('Monday', 'Squat')
    await userEvent.click((await addForm()).getByRole('button', { name: 'Add' }))
    expect(document.activeElement).toBe(add)
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Squat  3 × 5 @ 8']))
  })

  it('planAdd_ownExercise_pickerStaysUntilTheFormOpens', async () => {
    show()
    await userEvent.click(within(await day('Wednesday')).getByRole('button', { name: 'Add' }))
    let made!: () => void
    const original = db.exercises.add.bind(db.exercises)
    vi.spyOn(db.exercises, 'add').mockImplementation(((row: never) => new Promise((done) => (made = () => done(original(row))))) as never)
    await createOwn('Tyre flip')
    // Still being made: the picker is still there, never an empty moment.
    expect(screen.getByRole('textbox', { name: 'Search exercises' })).toBeInTheDocument()
    await act(async () => made())
    const dialogs = await waitFor(() => {
      const open = screen.getAllByRole('dialog')
      expect(within(open[0]!).getByRole('group', { name: 'Exercise' })).toHaveTextContent('Tyre flip')
      return open
    })
    expect(dialogs).toHaveLength(1)
    expect(screen.queryByRole('textbox', { name: 'Search exercises' })).toBeNull()
  })

  it('planAdd_sideChangedWhileOpen_storesTheFormsNumbersOnTheStoredSide', async () => {
    show()
    await pickOn('Monday', 'Squat')
    const form = await addForm()
    await updateSettings(db, (s) => ({ ...s, trainingMode: 'BODYBUILDING' }))
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    await waitFor(async () => expect(await storedPlan()).toHaveLength(1))
    const squat = await exerciseNamed(db, 'Squat')
    // The form's 3 × 5 @ 8 (no range), on the side as stored at the tap.
    expect(await storedPlan()).toEqual([{ weekday: 1, exerciseId: squat.id, sets: 3, reps: 5, targetRpe: 8, order: 0, mode: 'BODYBUILDING' }])
  })

  it('planAdd_twoClicksBeforeARender_storeOne', async () => {
    show()
    const add = vi.spyOn(db.planItems, 'add')
    await pickOn('Monday', 'Squat')
    const button = (await addForm()).getByRole('button', { name: 'Add' })
    // Both clicks reach the button before React renders the form away: only the guard stops the second.
    act(() => {
      button.click()
      button.click()
    })
    await waitFor(async () => expect(await storedPlan()).toHaveLength(1))
    await new Promise((done) => setTimeout(done, 50))
    expect(await storedPlan()).toHaveLength(1)
    expect(add).toHaveBeenCalledOnce()
  })

  it('planAdd_doubleTapAdd_storesOne', async () => {
    show()
    await pickOn('Monday', 'Squat')
    await userEvent.dblClick((await addForm()).getByRole('button', { name: 'Add' }))
    await waitFor(async () => expect(rowsOf(await day('Monday'))).toEqual(['Squat  3 × 5 @ 8']))
    // Give a second write every chance to land.
    await new Promise((done) => setTimeout(done, 50))
    expect(await storedPlan()).toHaveLength(1)
  })

  it('planAdd_secondTapOfADoubleTap_isIgnored', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    show()
    await pickOn('Monday', 'Squat')
    const form = await addFormNow()

    // A tap on Add, on Cancel or outside, landing just after the form opened: nothing happens.
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    expect(formOpen()).toBe(true)
    await userEvent.click(form.getByRole('button', { name: 'Cancel' }))
    expect(formOpen()).toBe(true)
    await tapOutside()
    expect(formOpen()).toBe(true)
    vi.setSystemTime(Date.now() + 299)
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    expect(formOpen()).toBe(true)
    await new Promise((done) => setTimeout(done, 20))
    expect(await storedPlan()).toEqual([])

    // The steppers stay live, and the keyboard is not held: Enter on Add stores at once.
    await userEvent.click(form.getByRole('button', { name: 'Increase Sets' }))
    expect(form.getByRole('group', { name: 'Sets' })).toHaveTextContent('4')
    form.getByRole('button', { name: 'Add' }).focus()
    await userEvent.keyboard('{Enter}')
    await waitFor(async () => expect((await storedPlan()).map((p) => [p.sets, p.reps])).toEqual([[4, 5]]))
    expect(formOpen()).toBe(false)

    // Nor is Escape.
    await pickOn('Monday', 'Squat')
    await addFormNow()
    await userEvent.keyboard('{Escape}')
    expect(formOpen()).toBe(false)
    expect(await storedPlan()).toHaveLength(1)
  })

  it('planAdd_tapAfterTheWindow_works', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    show()
    await pickOn('Monday', 'Squat')
    const form = await addFormNow()
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    expect(formOpen()).toBe(true)

    // Once the moment has passed, Add stores.
    vi.setSystemTime(Date.now() + 300)
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    await waitFor(async () => expect((await storedPlan()).map((p) => [p.sets, p.reps])).toEqual([[3, 5]]))
    expect(formOpen()).toBe(false)

    // And a tap outside closes the form, storing nothing more.
    await pickOn('Monday', 'Squat')
    await addFormNow()
    await tapOutside()
    expect(formOpen()).toBe(true)
    vi.setSystemTime(Date.now() + 300)
    await tapOutside()
    expect(formOpen()).toBe(false)
    expect(await storedPlan()).toHaveLength(1)
  })

  it('planAdd_ticksOnceWhenStored', async () => {
    const vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
    setTapHaptics(true)
    show()
    const monday = await day('Monday')

    // Picking and cancelling: no tick.
    await pickOn('Monday', 'Squat')
    await userEvent.click((await addForm()).getByRole('button', { name: 'Cancel' }))
    expect(vibrate).not.toHaveBeenCalled()

    // Picking opens the form without a tick; Add ticks once, when it is stored.
    await pickOn('Monday', 'Squat')
    const form = await addForm()
    expect(vibrate).not.toHaveBeenCalled()
    await userEvent.click(form.getByRole('button', { name: 'Add' }))
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Squat  3 × 5 @ 8']))
    await waitFor(() => expect(vibrate).toHaveBeenCalledExactlyOnceWith(10))
  })
})

// ---- Stage 2: adding from the picker ticks once (platform/haptics.ts) ----
describe('PlanScreen haptics', () => {
  afterEach(() => {
    setTapHaptics(false)
    Reflect.deleteProperty(navigator, 'vibrate')
  })

  it('pickerAdd_ticksOnce', async () => {
    const vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
    setTapHaptics(true)
    show()
    const monday = await day('Monday')
    await userEvent.click(within(monday).getByRole('button', { name: 'Add' }))
    await userEvent.type(dialog().getByRole('textbox', { name: 'Search exercises' }), 'Squat')
    await userEvent.dblClick(await pickerRow('Squat'))
    await userEvent.click((await addForm()).getByRole('button', { name: 'Add' }))
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Squat  3 × 5 @ 8']))
    await waitFor(() => expect(vibrate).toHaveBeenCalledExactlyOnceWith(10))
  })

  it('aFailedPickerAdd_doesNotBuzz', async () => {
    const vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
    setTapHaptics(true)
    const failing = vi.spyOn(db.planItems, 'add').mockImplementation(() => {
      throw new Error('disk full')
    })
    show()
    const monday = await day('Monday')
    await userEvent.click(within(monday).getByRole('button', { name: 'Add' }))
    await userEvent.type(dialog().getByRole('textbox', { name: 'Search exercises' }), 'Squat')
    await userEvent.click(await pickerRow('Squat'))
    await userEvent.click((await addForm()).getByRole('button', { name: 'Add' }))
    await waitFor(() => expect(failing).toHaveBeenCalled())
    await waitFor(() => expect(screen.getByText("Couldn't save — try again.")).toBeInTheDocument())
    expect(vibrate).not.toHaveBeenCalled()
  })

  // ---- 1.12.2: every Plan write says a failure ----

  it('failedEditSave_saysSo_rowUnchanged', async () => {
    await plan(db, (await exerciseNamed(db, 'Bench')).id, { weekday: 1, sets: 4, reps: 6, targetRpe: 7.5 })
    show()
    const monday = await day('Monday')
    await waitFor(() => expect(rowsOf(monday)).toEqual(['Bench  4 × 6 @ 7.5']))
    vi.spyOn(db.planItems, 'update').mockRejectedValue(new Error('disk full'))
    await userEvent.click(within(monday).getByRole('button', { name: 'More' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Edit' }))
    await userEvent.click(dialog().getByRole('button', { name: 'Increase Sets' }))
    await userEvent.click(dialog().getByRole('button', { name: 'Save' }))
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    expect((await storedPlan()).map((p) => p.sets)).toEqual([4])
  })

  it('failedDelete_saysSo_rowKept', async () => {
    await plan(db, (await exerciseNamed(db, 'Bench')).id, { weekday: 1 })
    show()
    const monday = await day('Monday')
    await waitFor(() => expect(rowsOf(monday)).toHaveLength(1))
    vi.spyOn(db.planItems, 'delete').mockRejectedValue(new Error('disk full'))
    await userEvent.click(within(monday).getByRole('button', { name: 'More' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Delete' }))
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    expect(await storedPlan()).toHaveLength(1)
  })

  it('failedMove_saysSo_andNeitherRowMoved', async () => {
    for (const [order, name] of ['Squat', 'Bench'].entries()) await plan(db, (await exerciseNamed(db, name)).id, { weekday: 1, order })
    show()
    const monday = await day('Monday')
    await waitFor(() => expect(rowsOf(monday)).toHaveLength(2))
    // Only the second of the move's writes fails: the first must not be kept on its own.
    const update = db.planItems.update.bind(db.planItems)
    let calls = 0
    vi.spyOn(db.planItems, 'update').mockImplementation(((...args: Parameters<typeof update>) =>
      ++calls === 2 ? Promise.reject(new Error('disk full')) : update(...args)) as typeof update)
    await userEvent.click(within(monday).getAllByRole('button', { name: 'Move down' })[0]!)
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    expect(calls).toBe(2)
    const squat = await exerciseNamed(db, 'Squat')
    const bench = await exerciseNamed(db, 'Bench')
    expect((await storedPlan()).map((p) => [p.exerciseId, p.order])).toEqual([[squat.id, 0], [bench.id, 1]])
  })

  it('failedBaseline_saysSo_storesNothing', async () => {
    show()
    vi.spyOn(db.baselines, 'put').mockRejectedValue(new Error('disk full'))
    const bests = await screen.findByRole('group', { name: 'Squat' })
    await userEvent.click(within(bests).getByRole('button', { name: 'Set best' }))
    await userEvent.type(dialog().getByRole('textbox', { name: 'Estimated 1RM (kg)' }), '180')
    await userEvent.click(dialog().getByRole('button', { name: 'Save' }))
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    expect(await db.baselines.toArray()).toEqual([])
  })
})
