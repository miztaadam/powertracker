import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { encodeBackup, exportBackup } from '../../backup/backup'
import type { GritDb } from '../../db/db'
import { checkInfoButtonOpensGuide } from '../../test/guideChecks'
import { TODAY, exerciseNamed, plan, todayFixture } from '../../test/todayFixture'
import { PlanScreen } from './PlanScreen'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const show = (onOpenSettings: () => void = () => {}) => render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={onOpenSettings} />)
const day = (name: string) => screen.findByRole('region', { name })
const dialog = () => within(screen.getByRole('dialog'))
const rowsOf = (section: HTMLElement) => within(section).queryAllByRole('listitem').map((row) => row.getAttribute('aria-label'))
/** The part of a picker row that picks the exercise. */
const pickerRow = (name: string) => within(dialog().getByRole('listitem', { name })).getAllByRole('button')[0]!
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
    // Picking adds it straight away: a compound lift at 3 sets × 5 reps @ the default RPE 8.
    await userEvent.click(pickerRow('Squat'))

    await waitFor(() => expect(rowsOf(monday)).toEqual(['Squat  3 × 5 @ 8']))
    expect(within(monday).getByText('1 exercise · 3 sets')).toBeInTheDocument()
    expect(rowsOf(await day('Wednesday'))).toEqual([])
    expect(rowsOf(await day('Friday'))).toEqual([])
    expect(screen.queryByRole('dialog')).toBeNull()
    const squat = await exerciseNamed(db, 'Squat')
    expect(await storedPlan()).toEqual([{ weekday: 1, exerciseId: squat.id, sets: 3, reps: 5, targetRpe: 8, order: 0 }])
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
    await userEvent.click(pickerRow('Deadlift'))
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
    await userEvent.click(dialog().getByRole('button', { name: 'Create your own' }))
    const custom = () => within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
    expect(custom().getByRole('button', { name: 'Create' })).toBeDisabled()
    await userEvent.type(custom().getByRole('textbox', { name: 'Name' }), '  Cluster squat ')
    // A variation needs its parent lift.
    await userEvent.click(custom().getByRole('checkbox', { name: 'Variation' }))
    expect(custom().getByRole('button', { name: 'Create' })).toBeDisabled()
    await userEvent.click(custom().getByRole('checkbox', { name: 'Squat' }))
    await userEvent.type(custom().getByRole('textbox', { name: 'Muscles (comma-separated)' }), 'quads, , glutes')
    await userEvent.click(custom().getByRole('button', { name: 'Create' }))

    // Made, and added to the day like a picked one: a variation is a compound lift, 3 × 5.
    await waitFor(async () => expect(rowsOf(await day('Wednesday'))).toEqual(['Cluster squat  3 × 5 @ 8']))
    expect(screen.queryByRole('dialog')).toBeNull()
    const made = await db.exercises.where('name').equals('Cluster squat').toArray()
    expect(made).toHaveLength(1)
    expect(made[0]).toMatchObject({ category: 'VARIATION', parentLift: 'SQUAT', muscles: ['quads', 'glutes'], isBuiltIn: false, archived: false })
    expect(await storedPlan()).toEqual([{ weekday: 3, exerciseId: made[0]!.id, sets: 3, reps: 5, targetRpe: 8, order: 0 }])
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
    await userEvent.click(within(within(screen.getByRole('dialog', { name: 'Change exercise' })).getByRole('listitem', { name: 'Curl' })).getAllByRole('button')[0]!)
    expect(screen.getAllByRole('dialog')).toHaveLength(1)
    expect(editDialog().getByRole('group', { name: 'Exercise' })).toHaveTextContent('Curl')
    await userEvent.click(editDialog().getByRole('button', { name: 'Cancel' }))
    expect(await db.exercises.toArray()).toEqual(before)
    expect(rowsOf(monday)).toEqual(['Bench  4 × 6 @ 7.5'])

    // Changed and saved: the row comes with the item, which keeps its place and its numbers.
    await edit()
    await userEvent.click(editDialog().getByRole('button', { name: 'Change' }))
    await userEvent.click(within(within(screen.getByRole('dialog', { name: 'Change exercise' })).getByRole('listitem', { name: 'Curl' })).getAllByRole('button')[0]!)
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
    await userEvent.click(pickerRow('Squat'))
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    expect(await storedPlan()).toEqual([])
  })
})
