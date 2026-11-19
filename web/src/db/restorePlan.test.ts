import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { TODAY, exerciseNamed, logSet, set, startSession, todayFixture } from '../test/todayFixture'
import { addPlanItem, planFor, planSnapshot, replacePlan, restorePlan, type NewPlanItem, type PlanSnapshot } from './catalogue'
import type { GritDb } from './db'
import type { TrainingMode } from './schema'
import { getSettings, updateSettings } from './settings'

/** 1.14.1: Undo after a whole-plan replace puts back exactly what was there. */

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  await fx.dispose()
})

const sorted = async (mode: TrainingMode) => (await planFor(db, mode)).sort((a, b) => a.id - b.id)
const daysOf = async (mode: TrainingMode) => {
  const s = await getSettings(db)
  return mode === 'BODYBUILDING' ? s.bodybuildingTrainingDays : s.trainingDays
}

/** A replace as the template and builder saves run it: snapshot, replace, days, in one transaction. */
async function replace(mode: TrainingMode, items: NewPlanItem[], days: number[]): Promise<PlanSnapshot | null> {
  return db.transaction('rw', db.exercises, db.planItems, db.skips, db.settings, async () => {
    const before = await planSnapshot(db, mode)
    const added = await replacePlan(db, mode, items)
    await updateSettings(db, (s) => (mode === 'BODYBUILDING' ? { ...s, bodybuildingTrainingDays: days } : { ...s, trainingDays: days }))
    return before && { ...before, replacedBy: added }
  })
}

const item = (weekday: number, exerciseId: number, sets = 3): NewPlanItem => ({ item: { weekday, exerciseId, sets, reps: 8, repsHigh: 12, targetRpe: 8 }, catalogueName: null })

/** A bodybuilding plan of three items on Monday and Thursday, a powerlifting item, a skip on one item, and days. */
async function oldPlan() {
  const curl = (await exerciseNamed(db, 'Curl')).id
  const squat = (await exerciseNamed(db, 'Squat')).id
  const a = await addPlanItem(db, { weekday: 1, exerciseId: curl, sets: 4, reps: 8, repsHigh: 12, targetRpe: 7.5 }, null, 'BODYBUILDING')
  const b = await addPlanItem(db, { weekday: 1, exerciseId: squat, sets: 3, reps: 10, repsHigh: 15, targetRpe: 8 }, null, 'BODYBUILDING')
  const c = await addPlanItem(db, { weekday: 4, exerciseId: curl, sets: 2, reps: 12, repsHigh: 20, targetRpe: 9 }, null, 'BODYBUILDING')
  // Reordered: Monday's second item first, so the order is not the id order.
  await db.planItems.update(a, { order: 1 })
  await db.planItems.update(b, { order: 0 })
  const pl = await addPlanItem(db, { weekday: 2, exerciseId: squat, sets: 5, reps: 5, targetRpe: 8 }, null, 'POWERLIFTING')
  const skipId = (await db.skips.add({ date: TODAY, mode: 'BODYBUILDING', exerciseId: curl, planItemId: c, reason: null })) as number
  await updateSettings(db, (s) => ({ ...s, bodybuildingTrainingDays: [1, 4], trainingDays: [2] }))
  return { curl, squat, ids: [a, b, c], pl, skipId }
}

describe('restorePlan', () => {
  it('putsBackTheIds_order_numbers_skipLinks_andDays', async () => {
    const { curl, skipId } = await oldPlan()
    const before = await sorted('BODYBUILDING')
    const powerlifting = await sorted('POWERLIFTING')

    const undo = await replace('BODYBUILDING', [item(2, curl), item(3, curl), item(5, curl)], [2, 3, 5])
    expect(await db.skips.get(skipId)).toMatchObject({ planItemId: null })
    expect(undo).not.toBeNull()

    expect(await restorePlan(db, 'BODYBUILDING', undo!)).toBe('restored')
    expect(await sorted('BODYBUILDING')).toEqual(before)
    expect((await db.skips.get(skipId))!.planItemId).toBe(before[2]!.id)
    expect(await daysOf('BODYBUILDING')).toEqual([1, 4])
    expect(await sorted('POWERLIFTING')).toEqual(powerlifting)
    expect(await daysOf('POWERLIFTING')).toEqual([2])
  })

  it('anEmptySide_hasNoSnapshot', async () => {
    const curl = (await exerciseNamed(db, 'Curl')).id
    expect(await replace('BODYBUILDING', [item(1, curl)], [1])).toBeNull()
  })

  it('aStaleUndo_afterAnUndo_doesNothing', async () => {
    const { curl } = await oldPlan()
    const undo = (await replace('BODYBUILDING', [item(2, curl)], [2]))!
    expect(await restorePlan(db, 'BODYBUILDING', undo)).toBe('restored')
    const restored = await sorted('BODYBUILDING')
    expect(await restorePlan(db, 'BODYBUILDING', undo)).toBe('stale')
    expect(await sorted('BODYBUILDING')).toEqual(restored)
  })

  it('aStaleUndo_afterAnotherSave_neverPutsTheOlderPlanOverTheNewer', async () => {
    const { curl } = await oldPlan()
    const first = (await replace('BODYBUILDING', [item(2, curl)], [2]))!
    const second = (await replace('BODYBUILDING', [item(3, curl), item(6, curl)], [3, 6]))!
    const newest = await sorted('BODYBUILDING')

    expect(await restorePlan(db, 'BODYBUILDING', first)).toBe('stale')
    expect(await sorted('BODYBUILDING')).toEqual(newest)
    expect(await daysOf('BODYBUILDING')).toEqual([3, 6])
    // The latest one still undoes, back to the plan it replaced.
    expect(await restorePlan(db, 'BODYBUILDING', second)).toBe('restored')
    expect((await sorted('BODYBUILDING')).map((p) => p.weekday)).toEqual([2])
  })

  it('neverAppliesToTheOtherSide', async () => {
    const { curl } = await oldPlan()
    const undo = (await replace('BODYBUILDING', [item(2, curl)], [2]))!
    const powerlifting = await sorted('POWERLIFTING')
    expect(await restorePlan(db, 'POWERLIFTING', undo)).toBe('stale')
    expect(await sorted('POWERLIFTING')).toEqual(powerlifting)
  })

  it('afterASessionOnTheNewPlan_keepsTheSession_itsSets_andNewSkips', async () => {
    const { curl, squat } = await oldPlan()
    const before = await sorted('BODYBUILDING')
    const undo = (await replace('BODYBUILDING', [item(2, curl), item(3, squat)], [2, 3]))!
    const newItem = (await sorted('BODYBUILDING'))[1]!.id
    const sessionId = await startSession(db, TODAY)
    const setId = await logSet(db, sessionId, set(curl, 20))
    const newSkip = (await db.skips.add({ date: TODAY, mode: 'BODYBUILDING', exerciseId: squat, planItemId: newItem, reason: 'TIME' })) as number

    expect(await restorePlan(db, 'BODYBUILDING', undo)).toBe('restored')
    expect(await sorted('BODYBUILDING')).toEqual(before)
    expect(await db.sessions.get(sessionId)).toBeDefined()
    expect(await db.sets.get(setId)).toMatchObject({ sessionId, exerciseId: curl })
    // The skip made on the new plan stays, with no plan item, as after any delete.
    expect(await db.skips.get(newSkip)).toMatchObject({ planItemId: null, reason: 'TIME' })
  })

  it('aFailure_leavesTheNewPlan_andRejects', async () => {
    const { curl, skipId } = await oldPlan()
    const undo = (await replace('BODYBUILDING', [item(2, curl)], [2]))!
    const after = await sorted('BODYBUILDING')
    db.planItems.hook('creating', () => {
      throw new Error('forced failure')
    })

    await expect(restorePlan(db, 'BODYBUILDING', undo)).rejects.toThrow()
    expect(await sorted('BODYBUILDING')).toEqual(after)
    expect(await daysOf('BODYBUILDING')).toEqual([2])
    expect((await db.skips.get(skipId))!.planItemId).toBeNull()
  })
})
