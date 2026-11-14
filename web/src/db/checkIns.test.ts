import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { checkInToJs, inGroupOrder, loadCheckIns, putCheckIn } from './checkIns'
import { installedVersion, openGritDb, type GritDb } from './db'
import { SCHEMA_VERSIONS, type SchemaVersion } from './schema'

/** Bodybuilding mode stage 3: check-ins in the web app's storage (schema version 4). */

let n = 0
let name = ''
const open: GritDb[] = []
beforeEach(() => {
  name = `grit-checkins-test-${++n}`
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

const V3 = SCHEMA_VERSIONS.filter((v) => v.version <= 3)

describe('schema version 4', () => {
  it('upgrade3to4_keepsEveryRow_andAddsAnEmptyCheckInStore', async () => {
    // A real version 3 database, written by the previous app, on both sides.
    const v3 = await openDb(V3)
    await v3.exercises.bulkAdd([
      { id: 1, name: 'Squat', category: 'SQUAT', parentLift: null, muscles: ['Quads'], isBuiltIn: true, archived: false },
      { id: 2, name: 'Curl', category: 'ACCESSORY', parentLift: null, muscles: ['Biceps'], isBuiltIn: true, archived: false },
    ])
    await v3.sessions.bulkAdd([
      { id: 1, date: '2026-10-27', bodyweightKg: 84.5, readiness: 4, mode: 'POWERLIFTING' },
      { id: 2, date: '2026-10-29', bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' },
    ])
    await v3.sets.bulkAdd([
      { id: 1, sessionId: 1, exerciseId: 1, weightKg: 140, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: true },
      { id: 2, sessionId: 2, exerciseId: 2, weightKg: 20, reps: 10, rpe: null, failed: false, missPoint: null, isTopSet: false },
    ])
    await v3.planItems.bulkAdd([
      { id: 1, weekday: 1, exerciseId: 1, sets: 4, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' },
      { id: 2, weekday: 3, exerciseId: 2, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' },
    ])
    await v3.advice.add({ id: 1, type: 'DELOAD', lift: null, createdAt: '2026-10-30', reason: 'd', severity: 3, status: 'DISMISSED', payloadJson: '{}', mode: 'POWERLIFTING' })
    await v3.baselines.put({ exerciseId: 1, e1rmKg: 172.5, date: '2026-09-01' })
    await v3.settings.put({ key: 'trainingMode', value: 'BODYBUILDING' })
    const tables = ['exercises', 'sessions', 'sets', 'planItems', 'advice', 'baselines', 'settings'] as const
    const before = Object.fromEntries(await Promise.all(tables.map(async (t) => [t, await v3.table(t).toArray()])))
    v3.close()

    const db = await openDb()

    expect(await installedVersion(name)).toBe(4)
    for (const t of tables) expect(await db.table(t).toArray(), t).toEqual(before[t])
    expect(await db.checkIns.toArray()).toEqual([])
    // The upgrade wrote a safety copy of the version 3 data first.
    expect((await db.meta.get('safetyCopy'))?.value).toEqual(expect.any(String))
  })

  it('checkInDate_isUnique', async () => {
    const db = await openDb()
    await db.checkIns.add({ date: '2026-11-11', lagging: ['CHEST'], growingWell: [] })
    await expect(db.checkIns.add({ date: '2026-11-11', lagging: [], growingWell: [] })).rejects.toMatchObject({ name: 'ConstraintError' })
  })
})

describe('check-ins', () => {
  it('save_replaceOnTheSameDate_andReadOldestFirst', async () => {
    const db = await openDb()
    await putCheckIn(db, '2026-11-11', ['QUADS', 'CHEST'], ['CALVES'])
    await putCheckIn(db, '2026-10-14', [], [])
    // The same date again replaces it.
    await putCheckIn(db, '2026-11-11', ['SIDE_DELTS'], ['QUADS', 'BICEPS'])

    const all = await loadCheckIns(db)
    expect(all.map(({ id: _id, ...rest }) => rest)).toEqual([
      { date: '2026-10-14', lagging: [], growingWell: [] },
      { date: '2026-11-11', lagging: ['SIDE_DELTS'], growingWell: ['BICEPS', 'QUADS'] },
    ])
    const js = checkInToJs(all[1]!)
    expect([js.date, [...js.lagging], [...js.growingWell], js.id]).toEqual(['2026-11-11', ['SIDE_DELTS'], ['BICEPS', 'QUADS'], all[1]!.id])
  })

  it('groups_areStoredInTheDomainsOrder_once', () => {
    expect(inGroupOrder(['OBLIQUES', 'CHEST', 'CHEST', 'QUADS'])).toEqual(['CHEST', 'QUADS', 'OBLIQUES'])
  })

  it('aFailedWriteInTheSameTransaction_leavesTheOldCheckIn', async () => {
    const db = await openDb()
    await putCheckIn(db, '2026-11-11', ['CHEST'], [])
    await expect(
      db.transaction('rw', db.checkIns, db.advice, async () => {
        await putCheckIn(db, '2026-11-11', ['QUADS'], [])
        throw new Error('boom')
      }),
    ).rejects.toThrow('boom')
    expect((await loadCheckIns(db)).map((c) => c.lagging)).toEqual([['CHEST']])
  })
})
