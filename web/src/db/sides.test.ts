import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { JsAdvice, JsPlanItem, JsSession } from '../domain'
import { addPlanItem, planFor, planItemsOn, updatePlanItem } from './catalogue'
import {
  activeTrainingDays,
  clearLeftOn,
  DEFAULT_SETTINGS,
  getSettings,
  installedVersion,
  openGritDb,
  readSafetyCopy,
  setTrainingMode,
  updateSettings,
  type GritDb,
} from './db'
import { adviceFromJs, adviceToJs, planItemFromJs, planItemToJs, sessionFromJs, sessionToJs } from './mapping'
import { SCHEMA_VERSIONS, sideOf, type SchemaVersion } from './schema'

/** Bodybuilding mode spec §6: the training side in the web app's storage. */

let n = 0
let name = ''
const open: GritDb[] = []
beforeEach(() => {
  name = `grit-sides-test-${++n}`
})
afterEach(async () => {
  for (const d of open.splice(0)) d.close()
  await Dexie.delete(name)
})
async function openDb(versions?: SchemaVersion[]) {
  const db = await openGritDb(name, versions ? { schemaVersions: versions } : {})
  open.push(db)
  return db
}

const V2 = SCHEMA_VERSIONS.filter((v) => v.version <= 2)
const P = 'com.powertracker.domain.model.AdvicePayload.'

describe('schema version 3', () => {
  it('upgrade2to3_keepsEveryRow_andStampsPowerlifting', async () => {
    // A real version 2 database, written by the previous app: rows have no mode and no repsHigh.
    const v2 = await openDb(V2)
    await v2.exercises.bulkAdd([
      { id: 1, name: 'Squat', category: 'SQUAT', parentLift: null, muscles: ['Quads'], isBuiltIn: true, archived: false },
      { id: 2, name: 'Curl', category: 'ACCESSORY', parentLift: null, muscles: ['Biceps'], isBuiltIn: true, archived: false },
    ])
    await v2.sessions.bulkAdd([
      { id: 1, date: '2026-10-27', bodyweightKg: 84.5, readiness: 4 },
      { id: 2, date: '2026-10-29', bodyweightKg: null, readiness: null },
    ])
    await v2.sets.bulkAdd([
      { id: 1, sessionId: 1, exerciseId: 1, weightKg: 140, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: true },
      { id: 2, sessionId: 2, exerciseId: 2, weightKg: 20, reps: 10, rpe: null, failed: false, missPoint: null, isTopSet: false },
    ])
    await v2.planItems.bulkAdd([
      { id: 1, weekday: 1, exerciseId: 1, sets: 4, reps: 5, targetRpe: 8, order: 0 },
      { id: 2, weekday: 3, exerciseId: 2, sets: 3, reps: 10, targetRpe: 8, order: 0 },
    ])
    await v2.advice.bulkAdd([
      { id: 1, type: 'TARGET', lift: 'SQUAT', createdAt: '2026-10-30', reason: 'r', severity: 1, status: 'OPEN', payloadJson: `{"type":"${P}Target","exerciseId":1,"weightKg":142.5,"reps":5,"rpe":8.0,"sets":4}` },
      { id: 2, type: 'DELOAD', lift: null, createdAt: '2026-10-30', reason: 'd', severity: 3, status: 'DISMISSED', payloadJson: `{"type":"${P}Deload"}` },
    ])
    await v2.baselines.put({ exerciseId: 1, e1rmKg: 172.5, date: '2026-09-01' })
    await v2.settings.put({ key: 'trainingDays', value: [2, 4] })
    const before = {
      sessions: await v2.sessions.toArray(),
      sets: await v2.sets.toArray(),
      planItems: await v2.planItems.toArray(),
      advice: await v2.advice.toArray(),
      exercises: await v2.exercises.toArray(),
      baselines: await v2.baselines.toArray(),
    }
    v2.close()

    // The upgrade to version 3 itself (version 4 came later and only adds a store).
    const db = await openDb(SCHEMA_VERSIONS.filter((v) => v.version <= 3))

    expect(await installedVersion(name)).toBe(3)
    const stamped = <T extends object>(rows: T[]) => rows.map((r) => ({ ...r, mode: 'POWERLIFTING' }))
    expect(await db.sessions.toArray()).toEqual(stamped(before.sessions))
    expect(await db.planItems.toArray()).toEqual(stamped(before.planItems))
    expect(await db.advice.toArray()).toEqual(stamped(before.advice))
    expect((await db.planItems.toArray()).every((p) => !('repsHigh' in p))).toBe(true)
    // Sets, exercises and baselines are not touched: one log, whatever the side.
    expect(await db.sets.toArray()).toEqual(before.sets)
    expect(await db.exercises.toArray()).toEqual(before.exercises)
    expect(await db.baselines.toArray()).toEqual(before.baselines)
    // Settings keep their values; the new ones read as their defaults.
    const s = await getSettings(db)
    expect(s.trainingDays).toEqual([2, 4])
    expect(s.trainingMode).toBe('POWERLIFTING')
    expect(s.powerliftingLeftOn).toBeNull()
    expect(s.bodybuildingLeftOn).toBeNull()
    // planItems is indexed by mode, and the index finds the stamped rows.
    expect(db.planItems.schema.indexes.map((i) => i.name)).toContain('mode')
    expect(await db.planItems.where('mode').equals('POWERLIFTING').count()).toBe(2)
    // The safety copy was written first, from the version 2 data.
    const copy = JSON.parse((await readSafetyCopy(db))!.json)
    expect(copy.sessions.map((x: { id: number }) => x.id)).toEqual([1, 2])
    expect(copy.planItems).toHaveLength(2)
  })

  it('upgrade_keepsAModeAlreadyThere', async () => {
    // A row that already says bodybuilding (a restore that ran before the upgrade cannot write one, but be safe).
    const v2 = await openDb(V2)
    await v2.planItems.add({ id: 1, weekday: 1, exerciseId: 1, sets: 3, reps: 8, targetRpe: 8, order: 0, mode: 'BODYBUILDING', repsHigh: 12 })
    v2.close()
    const db = await openDb()
    expect(await db.planItems.get(1)).toMatchObject({ mode: 'BODYBUILDING', repsHigh: 12 })
  })

  it('sideOf_readsAMissingModeAsPowerlifting', () => {
    expect(sideOf({})).toBe('POWERLIFTING')
    expect(sideOf({ mode: 'BODYBUILDING' })).toBe('BODYBUILDING')
    expect(sideOf({ mode: 'POWERLIFTING' })).toBe('POWERLIFTING')
  })
})

describe('mapping carries the side', () => {
  it('planItem_modeAndRepsHigh_roundTrip', async () => {
    const db = await openDb()
    const bb = new JsPlanItem(1, 3, 2, 3, 8, 8, 0, 'BODYBUILDING', 12)
    await db.planItems.put(planItemFromJs(bb))
    const row = (await db.planItems.get(1))!
    expect(row).toMatchObject({ mode: 'BODYBUILDING', repsHigh: 12 })
    expect(String(planItemToJs(row as never))).toBe(String(bb))

    const pl = new JsPlanItem(2, 3, 2, 5, 5, 8, 1)
    const plRow = planItemFromJs(pl)
    expect(plRow.mode).toBe('POWERLIFTING')
    // Fixed reps store no repsHigh at all, as every row before this version.
    expect('repsHigh' in plRow).toBe(false)
    expect(planItemToJs({ id: 9, weekday: 1, exerciseId: 1, sets: 3, reps: 5, targetRpe: 8, order: 0 }).mode).toBe('POWERLIFTING')
    expect(planItemToJs({ id: 9, weekday: 1, exerciseId: 1, sets: 3, reps: 5, targetRpe: 8, order: 0 }).repsHigh).toBeNull()
  })

  it('session_andAdvice_mode_roundTrip', () => {
    const s = new JsSession(3, '2026-11-02', null, null, [], 'BODYBUILDING')
    expect(sessionFromJs(s)).toEqual({ id: 3, date: '2026-11-02', bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' })
    expect(sessionToJs({ id: 3, date: '2026-11-02', bodyweightKg: null, readiness: null }, []).mode).toBe('POWERLIFTING')
    expect(sessionToJs({ id: 3, date: '2026-11-02', bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' }, []).mode).toBe('BODYBUILDING')

    const a = new JsAdvice(1, 'TARGET', null, '2026-11-02', 'why', 1, 'OPEN', '{}', 'BODYBUILDING')
    expect(adviceFromJs(a).mode).toBe('BODYBUILDING')
    expect(String(adviceToJs(adviceFromJs(a)))).toBe(String(a))
    expect(adviceToJs({ id: 1, type: 'DELOAD', lift: null, createdAt: '2026-11-02', reason: 'x', severity: 1, status: 'OPEN', payloadJson: '{}' }).mode).toBe('POWERLIFTING')
  })
})

describe('plan reads and writes take the side', () => {
  it('bodybuildingPlan_besidePowerlifting_leavesItUntouched', async () => {
    const db = await openDb()
    const pl1 = await addPlanItem(db, { weekday: 1, exerciseId: 1, sets: 4, reps: 5, targetRpe: 8 }, null, 'POWERLIFTING')
    const pl2 = await addPlanItem(db, { weekday: 1, exerciseId: 2, sets: 3, reps: 8, targetRpe: 8 }, null, 'POWERLIFTING')
    const powerliftingBefore = await planFor(db, 'POWERLIFTING')

    // The bodybuilding side's Monday is numbered on its own: its first item is first, not third.
    const bb1 = await addPlanItem(db, { weekday: 1, exerciseId: 2, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8 }, null, 'BODYBUILDING')
    const bb2 = await addPlanItem(db, { weekday: 1, exerciseId: 1, sets: 3, reps: 10, repsHigh: 15, targetRpe: 8 }, null, 'BODYBUILDING')
    await updatePlanItem(db, bb1, { exerciseId: 2, sets: 4, reps: 8, targetRpe: 8 }, null)

    expect(await planFor(db, 'POWERLIFTING')).toEqual(powerliftingBefore)
    expect((await planFor(db, 'POWERLIFTING')).map((p) => p.id)).toEqual([pl1, pl2])
    const bb = await planFor(db, 'BODYBUILDING')
    expect(bb.map((p) => [p.id, p.order, p.mode, p.repsHigh, p.sets])).toEqual([
      [bb1, 0, 'BODYBUILDING', 12, 4],
      [bb2, 1, 'BODYBUILDING', 15, 3],
    ])
    expect((await planItemsOn(db, 1, 'POWERLIFTING')).map((p) => p.id)).toEqual([pl1, pl2])
    expect((await planItemsOn(db, 1, 'BODYBUILDING')).map((p) => p.id)).toEqual([bb1, bb2])
    expect(await planItemsOn(db, 3, 'BODYBUILDING')).toEqual([])
  })

  it('aRowWithoutMode_isOnThePowerliftingSide', async () => {
    const db = await openDb()
    await db.planItems.add({ weekday: 2, exerciseId: 1, sets: 3, reps: 5, targetRpe: 8, order: 0 })
    expect(await planFor(db, 'POWERLIFTING')).toHaveLength(1)
    expect(await planFor(db, 'BODYBUILDING')).toEqual([])
    expect(await planItemsOn(db, 2, 'POWERLIFTING')).toHaveLength(1)
  })
})

describe('settings for the side', () => {
  it('newSettings_defaults', async () => {
    const db = await openDb()
    const s = await getSettings(db)
    expect(s.trainingMode).toBe('POWERLIFTING')
    expect(s.bodybuildingTrainingDays).toEqual([1, 3, 5])
    expect(s.powerliftingLeftOn).toBeNull()
    expect(s.bodybuildingLeftOn).toBeNull()
    expect(DEFAULT_SETTINGS.trainingMode).toBe('POWERLIFTING')
    expect(activeTrainingDays(s)).toEqual(s.trainingDays)
    ;(await getSettings(db)).bodybuildingTrainingDays.push(7)
    expect((await getSettings(db)).bodybuildingTrainingDays).toEqual([1, 3, 5])
  })

  it('setTrainingMode_stampsTheSideLeft_andCopiesDaysOnce', async () => {
    const db = await openDb()
    await updateSettings(db, (s) => ({ ...s, trainingDays: [2, 4, 6] }))

    // First time on: the side left is stamped, the training days are copied.
    let s = await setTrainingMode(db, 'BODYBUILDING', '2026-11-13')
    expect(s.trainingMode).toBe('BODYBUILDING')
    expect(s.powerliftingLeftOn).toBe('2026-11-13')
    expect(s.bodybuildingLeftOn).toBeNull()
    expect(s.bodybuildingTrainingDays).toEqual([2, 4, 6])
    expect(activeTrainingDays(s)).toEqual([2, 4, 6])
    expect(await getSettings(db)).toEqual(s)

    // The bodybuilding side's own days change; the powerlifting side's stay.
    await updateSettings(db, (x) => ({ ...x, bodybuildingTrainingDays: [1, 4] }))
    s = await getSettings(db)
    expect(activeTrainingDays(s)).toEqual([1, 4])
    expect(s.trainingDays).toEqual([2, 4, 6])

    // Off again: bodybuilding is stamped, powerlifting keeps the day it was left.
    s = await setTrainingMode(db, 'POWERLIFTING', '2026-11-20')
    expect(s.trainingMode).toBe('POWERLIFTING')
    expect(s.bodybuildingLeftOn).toBe('2026-11-20')
    expect(s.powerliftingLeftOn).toBe('2026-11-13')
    expect(activeTrainingDays(s)).toEqual([2, 4, 6])

    // On a second time: the days are not copied again.
    await updateSettings(db, (x) => ({ ...x, trainingDays: [3] }))
    s = await setTrainingMode(db, 'BODYBUILDING', '2026-12-25')
    expect(s.bodybuildingTrainingDays).toEqual([1, 4])
    expect(s.powerliftingLeftOn).toBe('2026-12-25')
    expect(s.bodybuildingLeftOn).toBe('2026-11-20')
  })

  it('setTrainingMode_toTheSideAlreadyOn_changesNothing', async () => {
    const db = await openDb()
    const before = await getSettings(db)
    expect(await setTrainingMode(db, 'POWERLIFTING', '2026-11-13')).toEqual(before)
    expect(await db.settings.count()).toBe(0)
  })

  it('clearLeftOn_clearsOnlyThatSide', async () => {
    const db = await openDb()
    await setTrainingMode(db, 'BODYBUILDING', '2026-11-13')
    await setTrainingMode(db, 'POWERLIFTING', '2026-11-14')
    let s = await clearLeftOn(db, 'POWERLIFTING')
    expect(s.powerliftingLeftOn).toBeNull()
    expect(s.bodybuildingLeftOn).toBe('2026-11-14')
    s = await clearLeftOn(db, 'BODYBUILDING')
    expect(s.bodybuildingLeftOn).toBeNull()
    expect(await getSettings(db)).toEqual(s)
  })

  it('wrongTypedStoredValues_fallBackToDefaults', async () => {
    const db = await openDb()
    await db.settings.bulkPut([
      { key: 'trainingMode', value: 'CROSSFIT' },
      { key: 'bodybuildingTrainingDays', value: 'mon' },
      { key: 'powerliftingLeftOn', value: 5 },
      { key: 'bodybuildingLeftOn', value: '2026-11-01' },
    ])
    const s = await getSettings(db)
    expect(s.trainingMode).toBe('POWERLIFTING')
    expect(s.bodybuildingTrainingDays).toEqual([1, 3, 5])
    expect(s.powerliftingLeftOn).toBeNull()
    expect(s.bodybuildingLeftOn).toBe('2026-11-01')
    await db.settings.put({ key: 'trainingMode', value: 'BODYBUILDING' })
    expect((await getSettings(db)).trainingMode).toBe('BODYBUILDING')
  })
})
