import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { resolveCatalogue } from '../db/catalogue'
import { loadHistory, type GritDb } from '../db/db'
import type { PlateCount, TrainingMode } from '../db/schema'
import { grit } from '../domain'
import { TODAY, addDays, exerciseNamed, logSet, set, setsOn, startSession, targets, todayFixture } from '../test/todayFixture'
import { isNewBest, useToday, type SetInput, type TodayActions, type TodayState } from './useToday'

/**
 * 1.17: counting plates on Today (plate-count plan, Task 8 Step 1), the web twin of Android's `TodayPlatesViewModelTest`.
 * Lat pulldown is a catalogue machine exercise: plates are offered on it.
 */

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

type Vm = { current: TodayState & TodayActions }

const show = (database: GritDb = db): Vm => renderHook(() => useToday(database, () => TODAY)).result

const input = (weightKg: number, over: Partial<SetInput> = {}): SetInput => ({ weightKg, reps: 8, rpe: 8, failed: false, ...over })
/** A set typed in the PLATE field: the editor's unit comes with it. */
const plateInput = (n: number, over: Partial<SetInput> = {}): SetInput => input(n, { ...over, unit: 'PLATES' })

/** Lat pulldown's row (a catalogue exercise, made as a plan item or a set would make it). */
const pulldownRow = () => resolveCatalogue(db, 'Lat pulldown')

/** Plans `exerciseId` today (a Wednesday) 3 × 8 @ 8 on `mode`'s side. */
const planToday = (exerciseId: number, order = 0, mode: TrainingMode = 'POWERLIFTING') =>
  db.planItems.add({ weekday: 3, exerciseId, sets: 3, reps: 8, targetRpe: 8, order, mode, ...(mode === 'BODYBUILDING' ? { repsHigh: 12 } : {}) })

const plates = (n: number, reps = 8) => ({ unit: 'PLATES' as const, reps, weightKg: n })

/** A past session of `exerciseId` on `date`: each set's weight (kg, or a plate number with `plates`). */
async function past(date: string, exerciseId: number, ...sets: { weightKg: number; reps?: number; unit?: 'PLATES' }[]) {
  const session = await startSession(db, date)
  for (const s of sets) await logSet(db, session, set(exerciseId, s.weightKg, { reps: s.reps ?? 8, ...(s.unit ? { unit: s.unit } : {}) }))
}

const itemOf = (vm: Vm, exerciseId: number) => vm.current.items.find((i) => i.exercise.id === exerciseId)

const setsShown = (vm: Vm) => vm.current.items.reduce((n, i) => n + i.sets.length, 0)

/**
 * Logs `s` (or confirms the held set) as a tap does, without waiting inside `act`, and waits until the set is on screen or
 * held for the typo dialog: the screen must render for the log to finish.
 */
async function log(vm: Vm, key: number, s: SetInput | 'confirm') {
  const before = setsShown(vm)
  act(() => {
    void (s === 'confirm' ? vm.current.confirmPending() : vm.current.logSet(key, s))
  })
  await waitFor(() => expect(setsShown(vm) > before || (s !== 'confirm' && vm.current.pending != null)).toBe(true))
  // Let the screen's effects release the log's guard.
  await act(() => new Promise<void>((r) => setTimeout(r, 0)))
}

async function choose(vm: Vm, key: number, value: PlateCount) {
  await act(() => vm.current.setPlateCount!(key, value))
}

/** Every stored TARGET's payload for today. */
const todaysPayloads = async () => (await targets(db)).filter((a) => a.createdAt === TODAY).map((a) => grit.advicePayload(a.payloadJson)!)

const editorRows = () => db.meta.where('key').startsWith('editor:').toArray()

describe('plates on Today', () => {
  it('turningPlatesOn_emptiesTheField_whateverWasInIt: 40 typed and kept', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    act(() => vm.current.edit(id, { weightKg: 40, reps: 8, rpe: 8 }))
    await waitFor(async () => expect(await editorRows()).toHaveLength(1))
    expect(itemOf(vm, id)!.editor.weightKg).toBe(40)

    await choose(vm, id, 'WHOLE')

    await waitFor(() => expect(itemOf(vm, id)!.editor).toStrictEqual({ weightKg: null, reps: 8, rpe: 8, unit: 'PLATES' }))
    expect(await editorRows()).toEqual([])
  })

  it('turningPlatesOn_emptiesTheField_whateverWasInIt: a kg target applied', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -7), id, { weightKg: 40 })
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)?.suggestion).toBeTruthy())
    await act(() => vm.current.applyTarget(itemOf(vm, id)!.suggestion!.id))
    await waitFor(() => expect(itemOf(vm, id)!.editor.weightKg).not.toBeNull())

    await choose(vm, id, 'WHOLE')

    await waitFor(() => expect(itemOf(vm, id)!.editor.weightKg).toBeNull())
    expect(itemOf(vm, id)!.editor.unit).toBe('PLATES')
  })

  it('turningPlatesOn_emptiesTheField_whateverWasInIt: a kg set logged today', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await logSet(db, await startSession(db, TODAY), set(id, 40, { reps: 8 }))
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)?.editor.weightKg).toBe(40))

    await choose(vm, id, 'WHOLE')

    await waitFor(() => expect(itemOf(vm, id)!.editor.weightKg).toBeNull())
    expect(itemOf(vm, id)!.editor.unit).toBe('PLATES')
    expect(itemOf(vm, id)!.lastWeightKg).toBeNull()
  })

  it('theNextSetIsLoggedInPlates_andEarlierSetsKeepTheirUnit', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    await log(vm, id, input(40))
    await log(vm, id, input(40))
    await choose(vm, id, 'WHOLE')
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBe('PLATES'))
    await log(vm, id, plateInput(7))

    const stored = await setsOn(db, TODAY)
    expect(stored.map((s) => [s.weightKg, 'unit' in s ? s.unit : 'no key'])).toEqual([
      [40, 'no key'],
      [40, 'no key'],
      [7, 'PLATES'],
    ])
  })

  it('aKgTargetIsNotShownOrAppliedForAPlateExercise_andNoStoredAdviceChanges', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -7), id, { weightKg: 40 })
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)?.suggestion).toBeTruthy())
    const before = await db.advice.toArray()

    await choose(vm, id, 'WHOLE')

    await waitFor(() => expect(itemOf(vm, id)!.unit).toBe('PLATES'))
    expect(itemOf(vm, id)!.suggestion).toBeNull()
    expect(itemOf(vm, id)!.editor.weightKg).toBeNull()
    // No plate set yet: the coach has nothing to go on, and what it stored in kg is as it was.
    expect(await db.advice.toArray()).toEqual(before)
  })

  it('switchingBackToKgTheSameDay_showsTheKgTargetAgain', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -7), id, { weightKg: 40 })
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)?.suggestion).toBeTruthy())
    const kgTarget = itemOf(vm, id)!.suggestion!

    await choose(vm, id, 'WHOLE')
    await waitFor(() => expect(itemOf(vm, id)!.suggestion).toBeNull())
    await choose(vm, id, 'OFF')

    await waitFor(() => expect(itemOf(vm, id)!.suggestion?.id).toBe(kgTarget.id))
    expect(itemOf(vm, id)!.suggestion!.target).toStrictEqual(kgTarget.target)
    expect(await todaysPayloads()).toHaveLength(1)
  })

  it('turningPlatesBackOnWithOlderPlateHistory_storesAPlateTargetThatDay_besideTheKgOne', async () => {
    const id = await pulldownRow()
    await planToday(id)
    // Plates a fortnight ago, then back to kg last week.
    await past(addDays(TODAY, -14), id, plates(7))
    await past(addDays(TODAY, -7), id, { weightKg: 40 })
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)?.suggestion).toBeTruthy())
    expect(itemOf(vm, id)!.suggestion!.target.unit).toBeUndefined()

    await choose(vm, id, 'WHOLE')

    await waitFor(() => expect(itemOf(vm, id)!.suggestion?.target.unit).toBe('PLATES'))
    expect(itemOf(vm, id)!.suggestion!.target).toMatchObject({ exerciseId: id, weightKg: 7, reps: 9 })
    expect(itemOf(vm, id)!.suggestion!.reason).toContain('plate 7')
    const stored = await todaysPayloads()
    expect(stored.map((p) => [p.exerciseId, p.unit])).toEqual([
      [id, 'KG'],
      [id, 'PLATES'],
    ])
  })

  it('theFieldFills_fromThisUnitOnly', async () => {
    const id = await pulldownRow()
    await db.exercises.update(id, { plateCount: 'WHOLE' })
    // Added on Today, not planned: no target, so the field comes from the last top set in plates.
    await past(addDays(TODAY, -14), id, plates(7))
    await past(addDays(TODAY, -7), id, { weightKg: 60 })
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    act(() => vm.current.addExercise(id))
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    expect(itemOf(vm, id)!.editor).toMatchObject({ weightKg: 7, unit: 'PLATES' })
    expect(itemOf(vm, id)!.lastWeightKg).toBe(7)
  })

  it('theFirstPlateSetAfterKg_raisesNoBigJump_andPlate3To5Does', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -7), id, { weightKg: 60 })
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    await choose(vm, id, 'WHOLE')
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBe('PLATES'))

    await log(vm, id, plateInput(7))
    expect(vm.current.pending).toBeNull()
    expect(await setsOn(db, TODAY)).toHaveLength(1)

    await log(vm, id, plateInput(3))
    expect(vm.current.pending).toBeNull()
    await log(vm, id, plateInput(5))
    expect(vm.current.pending).toMatchObject({ exerciseId: id, warnings: ['BIG_JUMP'], lastWeightKg: 3, set: { weightKg: 5, unit: 'PLATES' } })
    // Confirmed, the held set is saved as it was checked: in plates.
    await log(vm, id, 'confirm')
    expect((await setsOn(db, TODAY)).map((s) => [s.weightKg, s.unit])).toEqual([
      [7, 'PLATES'],
      [3, 'PLATES'],
      [5, 'PLATES'],
    ])
  })

  it('plate 7 after 5 kg raises no big jump', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -7), id, { weightKg: 5 })
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    await choose(vm, id, 'WHOLE')
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBe('PLATES'))
    // Read as kg against 5 kg, plate 7 would be a jump.
    await log(vm, id, plateInput(7))
    expect(vm.current.pending).toBeNull()
    expect(await setsOn(db, TODAY)).toHaveLength(1)
  })

  it('back in kg the same day, a set is judged against the last kg set, never against plate 7', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -7), id, { weightKg: 50 })
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    await choose(vm, id, 'WHOLE')
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBe('PLATES'))
    await log(vm, id, plateInput(7))
    await choose(vm, id, 'OFF')
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBeUndefined())
    expect(itemOf(vm, id)!.lastWeightKg).toBe(50)
    expect(itemOf(vm, id)!.editor.weightKg).toBe(50)
    await log(vm, id, input(55))
    expect(vm.current.pending).toBeNull()
    expect((await setsOn(db, TODAY)).map((s) => [s.weightKg, s.unit])).toEqual([
      [7, 'PLATES'],
      [55, undefined],
    ])
  })

  it('aStandIn_getsItsRowWhenPlatesAreChosen_andItsCardStays', async () => {
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    let standIn: number | null = null
    act(() => {
      standIn = vm.current.addFromCatalogue('Lat pulldown')
    })
    expect(standIn).toBeLessThan(0)
    await waitFor(() => expect(itemOf(vm, standIn!)).toBeDefined())
    expect(itemOf(vm, standIn!)!.plateOffered).toBe(true)
    expect(await db.exercises.where('name').equals('Lat pulldown').count()).toBe(0)

    await choose(vm, standIn!, 'WHOLE')

    const row = await exerciseNamed(db, 'Lat pulldown')
    expect(row).toMatchObject({ isBuiltIn: true, archived: false, plateCount: 'WHOLE' })
    await waitFor(() => expect(vm.current.resolved).toEqual({ [standIn!]: row.id }))
    expect(vm.current.items.map((i) => i.exercise.id)).toEqual([row.id])
    expect(itemOf(vm, row.id)).toMatchObject({ unit: 'PLATES', plateCount: 'WHOLE', plateOffered: true })
    expect(itemOf(vm, row.id)!.editor.weightKg).toBeNull()
    // Only the one row: a second choice goes to it.
    await choose(vm, row.id, 'HALF')
    expect((await db.exercises.where('name').equals('Lat pulldown').toArray()).map((e) => e.plateCount)).toEqual(['HALF'])
  })

  it('plateOffered_isFalseForMainLifts_variations_andAssisted', async () => {
    const squat = (await exerciseNamed(db, 'Squat')).id
    const pause = (await exerciseNamed(db, 'Pause squat')).id
    const dip = await resolveCatalogue(db, 'Assisted dip')
    const ownVariation = (await db.exercises.add({ name: 'My squat', category: 'VARIATION', parentLift: 'SQUAT', muscles: ['QUADS'], isBuiltIn: false, archived: false })) as number
    const ownAccessory = (await db.exercises.add({ name: 'Sled drag', category: 'ACCESSORY', parentLift: null, muscles: ['QUADS'], isBuiltIn: false, archived: false })) as number
    const pulldown = await pulldownRow()
    // A main lift with a setting stored (a hand-edited backup) still counts kg.
    await db.exercises.update(squat, { plateCount: 'WHOLE' })
    const ids = [squat, pause, dip, ownVariation, ownAccessory, pulldown]
    for (const [i, id] of ids.entries()) await planToday(id, i)
    const vm = show()
    await waitFor(() => expect(vm.current.items).toHaveLength(6))
    expect(ids.map((id) => itemOf(vm, id)!.plateOffered ?? false)).toEqual([false, false, false, false, true, true])
    // Not offered: the key is not there at all, and the squat's stored setting does not make it plates.
    for (const id of [pause, dip, ownVariation]) expect('plateOffered' in itemOf(vm, id)!).toBe(false)
    expect('unit' in itemOf(vm, squat)!).toBe(false)
    expect(itemOf(vm, squat)!.editor.unit).toBeUndefined()
  })

  it('an editor kept in meta from before the switch is not put in the PLATE field after a reload', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await startSession(db, TODAY)
    // Kept from before plates were turned on (by this tab or another): a kg 40, and the setting changed under it.
    await db.meta.put({ key: `editor:${TODAY}:${id}`, value: { weightKg: 40, reps: 8, rpe: 8 } })
    await db.exercises.update(id, { plateCount: 'WHOLE' })
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    expect(vm.current.loading).toBe(false)
    expect(itemOf(vm, id)!.editor).toStrictEqual({ weightKg: null, reps: 8, rpe: 8, unit: 'PLATES' })
  })

  it('a second tab never puts its kg number in the PLATE field', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await startSession(db, TODAY)
    const a = show()
    const b = show()
    await waitFor(() => expect(itemOf(a, id)).toBeDefined())
    await waitFor(() => expect(itemOf(b, id)).toBeDefined())
    act(() => b.current.edit(id, { weightKg: 40, reps: 8, rpe: 8 }))
    expect(itemOf(b, id)!.editor.weightKg).toBe(40)

    await choose(a, id, 'WHOLE')

    await waitFor(() => expect(itemOf(b, id)!.unit).toBe('PLATES'))
    expect(itemOf(b, id)!.editor).toStrictEqual({ weightKg: null, reps: 8, rpe: 8, unit: 'PLATES' })
  })

  it('the kept editor rows for the exercise and its stand-in are deleted on a change of unit', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    await choose(vm, id, 'WHOLE')
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBe('PLATES'))
    act(() => vm.current.edit(id, { weightKg: 7, reps: 8, rpe: 8, unit: 'PLATES' }))
    await waitFor(async () => expect((await editorRows()).map((r) => r.value)).toEqual([{ weightKg: 7, reps: 8, rpe: 8, unit: 'PLATES' }]))

    // Whole plates to half steps is no change of unit, and still the kept numbers go: the field starts afresh.
    await choose(vm, id, 'HALF')

    await waitFor(() => expect(itemOf(vm, id)!.editor.weightKg).toBeNull())
    expect(await editorRows()).toEqual([])

    // A stand-in's numbers are kept on screen only; they go too, under its stand-in id, when its row is made.
    let standIn: number | null = null
    act(() => {
      standIn = vm.current.addFromCatalogue('Leg extension')
    })
    await waitFor(() => expect(itemOf(vm, standIn!)).toBeDefined())
    act(() => vm.current.edit(standIn!, { weightKg: 30, reps: 8, rpe: 8 }))
    expect(itemOf(vm, standIn!)!.editor.weightKg).toBe(30)
    await choose(vm, standIn!, 'OFF')
    const row = await exerciseNamed(db, 'Leg extension')
    expect('plateCount' in row).toBe(false)
    await waitFor(() => expect(itemOf(vm, row.id)).toBeDefined())
    expect(itemOf(vm, row.id)!.editor.weightKg).toBeNull()
  })

  it('aSetLoggedBeforeTheSwitch_keepsItsUnit_whenItsMissPointIsSetOrItIsDeleted', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    await log(vm, id, input(60, { failed: true }))
    await choose(vm, id, 'WHOLE')
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBe('PLATES'))
    await log(vm, id, plateInput(7))
    const [kgSet, plateSet] = await setsOn(db, TODAY)

    await act(() => vm.current.setMissPoint(kgSet!.id!, 'MID'))
    expect(await db.sets.get(kgSet!.id!)).toStrictEqual({ ...kgSet, missPoint: 'MID' })
    expect('unit' in (await db.sets.get(kgSet!.id!))!).toBe(false)

    await act(() => vm.current.deleteSet(plateSet!.id!))
    expect(await setsOn(db, TODAY)).toStrictEqual([{ ...kgSet, missPoint: 'MID' }])
    await waitFor(() => expect(itemOf(vm, id)!.sets).toHaveLength(1))
    // The kg set never fills the PLATE field.
    expect(itemOf(vm, id)!.editor.weightKg).toBeNull()
  })

  it('a kg editor, target and set input carry no unit key', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -7), id, { weightKg: 40 })
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)?.suggestion).toBeTruthy())
    const item = itemOf(vm, id)!
    // Offered, so `plateOffered` is there; in kg and off, `unit` and `plateCount` are not.
    expect(Object.keys(item).filter((k) => ['unit', 'plateCount', 'plateOffered'].includes(k))).toEqual(['plateOffered'])
    expect(Object.keys(item.editor)).toEqual(['weightKg', 'reps', 'rpe'])
    expect(Object.keys(item.suggestion!.target)).toEqual(['exerciseId', 'weightKg', 'reps', 'rpe', 'sets', 'repsHigh'])

    await act(() => vm.current.applyTarget(item.suggestion!.id))
    await waitFor(async () => expect(await editorRows()).toHaveLength(1))
    expect(Object.keys((await editorRows())[0]!.value as object)).toEqual(['weightKg', 'reps', 'rpe'])
    expect(Object.keys(itemOf(vm, id)!.editor)).toEqual(['weightKg', 'reps', 'rpe'])

    // A set held for the typo dialog, and the set stored: neither has a unit key.
    await log(vm, id, input(400))
    expect(Object.keys(vm.current.pending!.set)).toEqual(['weightKg', 'reps', 'rpe', 'failed'])
    act(() => vm.current.cancelPending())
    await log(vm, id, input(40))
    const [stored] = await setsOn(db, TODAY)
    expect(stored!.weightKg).toBe(40)
    expect(Object.keys(stored!)).not.toContain('unit')
  })

  it('a plate set is never judged a kg best', async () => {
    const squat = (await exerciseNamed(db, 'Squat')).id
    await past(addDays(TODAY, -7), squat, { weightKg: 100, reps: 5 })
    const history = await loadHistory(db)
    const row = { exerciseId: squat, weightKg: 300, reps: 5, rpe: 8, failed: false }
    expect(isNewBest(history, row, TODAY, 'POWERLIFTING')).toBe(true)
    expect(isNewBest(history, { ...row, unit: 'PLATES' as const }, TODAY, 'POWERLIFTING')).toBe(false)
  })

  it('a set submitted in the unit the exercise has just left is refused, not relabelled', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    // The setting changes between the render and the tap (another tab): the kg 40 on screen is not stored as plate 40.
    await db.exercises.update(id, { plateCount: 'WHOLE' })
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBe('PLATES'))
    await act(() => vm.current.logSet(id, input(40)))
    expect(vm.current.pending).toBeNull()
    expect(await setsOn(db, TODAY)).toEqual([])
    // Nor the other way: a plate number after the switch back to kg.
    await db.exercises.where(':id').equals(id).modify((r) => void delete r.plateCount)
    await waitFor(() => expect(itemOf(vm, id)!.unit).toBeUndefined())
    await act(() => vm.current.logSet(id, plateInput(7)))
    expect(await setsOn(db, TODAY)).toEqual([])
    // A set held for the typo dialog keeps its own unit when confirmed.
    await log(vm, id, input(40, { reps: 30 }))
    expect(vm.current.pending).not.toBeNull()
    await log(vm, id, 'confirm')
    expect((await setsOn(db, TODAY)).map((s) => [s.weightKg, s.unit])).toEqual([[40, undefined]])
  })

  it('the day began in plates with a plate target, then plates turned off on the last plate exercise: a kg target the same day', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -14), id, { weightKg: 40 })
    await past(addDays(TODAY, -7), id, plates(7))
    await db.exercises.update(id, { plateCount: 'WHOLE' })
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)?.suggestion?.target.unit).toBe('PLATES'))
    // Turned off elsewhere: no exercise counts plates any more.
    await db.exercises.where(':id').equals(id).modify((r) => void delete r.plateCount)
    await waitFor(() => expect(itemOf(vm, id)!.suggestion).not.toBeNull())
    await waitFor(() => expect(itemOf(vm, id)!.suggestion!.target.unit).toBeUndefined())
    expect((await todaysPayloads()).map((p) => p.unit)).toEqual(['PLATES', 'KG'])
  })

  it('a plate-unit editor under a kg exercise never shows in the kg field', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await startSession(db, TODAY)
    await db.meta.put({ key: `editor:${TODAY}:${id}`, value: { weightKg: 7, reps: 8, rpe: 8, unit: 'PLATES' } })
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)).toBeDefined())
    expect(itemOf(vm, id)!.editor).toStrictEqual({ weightKg: null, reps: 8, rpe: 8 })
  })

  it('a change of setting asks the coach again, also when made elsewhere', async () => {
    const id = await pulldownRow()
    await planToday(id)
    await past(addDays(TODAY, -14), id, plates(7))
    await past(addDays(TODAY, -7), id, { weightKg: 40 })
    const vm = show()
    await waitFor(() => expect(itemOf(vm, id)?.suggestion).toBeTruthy())
    // Another tab (or the Plan screen) changes the setting: this screen's coach pass runs again by itself.
    await db.exercises.update(id, { plateCount: 'WHOLE' })
    await waitFor(() => expect(itemOf(vm, id)!.suggestion?.target.unit).toBe('PLATES'))
  })
})
