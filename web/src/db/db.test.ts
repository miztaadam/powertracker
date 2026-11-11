import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { grit, JsAdvice, JsBaseline, JsPlanItem, JsSession, JsSet } from '../domain'
import {
  ensureSeeded,
  getSettings,
  loadHistory,
  installedVersion,
  openGritDb,
  readSafetyCopy,
  updateSettings,
  UpgradeFailedError,
  type GritDb,
} from './db'
import { SCHEMA_VERSIONS, type SchemaVersion } from './schema'
import {
  adviceFromJs,
  adviceToJs,
  baselineFromJs,
  planItemFromJs,
  planItemToJs,
  sessionFromJs,
  setFromJs,
} from './mapping'

let n = 0
let name = ''
const open: GritDb[] = []
beforeEach(() => {
  name = `grit-test-${++n}`
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

describe('seeding', () => {
  it('seed_idempotent_matchesDomain', async () => {
    const db = await openDb()
    await ensureSeeded(db)
    await ensureSeeded(db)
    const rows = await db.exercises.orderBy('id').toArray()
    const seed = grit.seedExercises()
    expect(rows).toHaveLength(seed.length)
    expect(rows.map((r) => r.id)).toEqual(seed.map((e) => e.id).sort((a, b) => a - b))
    const hist = await loadHistory(db)
    expect(hist.exercises.map(String).sort()).toEqual(seed.map(String).sort())
  })
})

describe('history', () => {
  it('historyRoundTrip', async () => {
    const db = await openDb()
    await ensureSeeded(db)
    const sessions = [
      new JsSession(1, '2026-11-02', 82.5, 4, [
        new JsSet(1, 100, 5, 8.5, false, null, true),
        new JsSet(1, 90, 5, null, false, null, false),
      ]),
      new JsSession(2, '2026-11-04', null, null, [new JsSet(1, 105, 3, 9.5, true, 'MIDPOINT', true)]),
    ]
    for (const s of sessions) {
      await db.sessions.add(sessionFromJs(s))
      await db.sets.bulkAdd(s.sets.map((x) => setFromJs(x, s.id)))
    }
    const baseline = new JsBaseline(1, 140, '2026-10-01')
    await db.baselines.put(baselineFromJs(baseline))

    const h = await loadHistory(db)
    expect(h.sessions).toHaveLength(2)
    expect(h.sessions.map(String)).toEqual(sessions.map(String))
    expect(h.baselines).toHaveLength(1)
    expect(String(h.baselines[0])).toBe(String(baseline))
    expect(h.exercises).toHaveLength(grit.seedExercises().length)
  })
})

describe('mapping', () => {
  it('rawRows_structure_withExplicitNulls', () => {
    expect(setFromJs(new JsSet(1, 100, 5, null, true, 'MIDPOINT', false), 7)).toEqual({
      sessionId: 7, exerciseId: 1, weightKg: 100, reps: 5, rpe: null, failed: true, missPoint: 'MIDPOINT', isTopSet: false,
    })
    expect(setFromJs(new JsSet(1, 100, 5, 8, false, null, true), 7).missPoint).toBeNull()
    expect(sessionFromJs(new JsSession(3, '2026-11-02', null, null, []))).toEqual({
      id: 3, date: '2026-11-02', bodyweightKg: null, readiness: null,
    })
    expect(baselineFromJs(new JsBaseline(1, 140, '2026-10-01'))).toEqual({ exerciseId: 1, e1rmKg: 140, date: '2026-10-01' })
  })

  it('planItem_andAdvice_roundTrip', async () => {
    const db = await openDb()
    const p = new JsPlanItem(1, 3, 2, 4, 6, 8.5, 0)
    await db.planItems.put(planItemFromJs(p))
    expect(String(planItemToJs((await db.planItems.get(1))! as never))).toBe(String(p))
    const payload = '{"kind":"x","n":[1,2]}'
    const a = new JsAdvice(1, 'DELOAD', null, '2026-11-02', 'why', 2, 'OPEN', payload)
    await db.advice.put(adviceFromJs(a))
    const row = (await db.advice.get(1))!
    expect(row.payloadJson).toBe(payload)
    expect(row.lift).toBeNull()
    expect(String(adviceToJs(row as never))).toBe(String(a))
  })

  it('sessionDate_isUnique', async () => {
    const db = await openDb()
    await db.sessions.add({ date: '2026-11-02', bodyweightKg: null, readiness: null })
    await expect(
      db.sessions.add({ date: '2026-11-02', bodyweightKg: null, readiness: null }),
    ).rejects.toMatchObject({ name: 'ConstraintError' })
  })
})

describe('settings', () => {
  it('settings_defaults_andUpdate', async () => {
    const db = await openDb()
    expect(await getSettings(db)).toEqual({
      changePlates: false,
      defaultRpe: 8.0,
      trainingDays: [1, 3, 5],
      appLock: false,
      restMainSec: 180,
      restAccessorySec: 90,
      restAlert: 'SOUND',
      keepAwake: true,
      lastBackupAt: null,
    })
    await updateSettings(db, (s) => ({ ...s, changePlates: true, trainingDays: [2, 4], lastBackupAt: '2026-11-11' }))
    const s = await getSettings(db)
    expect(s.changePlates).toBe(true)
    expect(s.trainingDays).toEqual([2, 4])
    expect(s.lastBackupAt).toBe('2026-11-11')
    expect(s.restMainSec).toBe(180)
    expect((await db.settings.toArray()).map((r) => r.key).sort()).toEqual(['changePlates', 'lastBackupAt', 'trainingDays'])
    await db.settings.put({ key: 'defaultRpe', value: 'oops' })
    expect((await getSettings(db)).defaultRpe).toBe(8.0)
  })

  it('defaults_areNotShared', async () => {
    const db = await openDb()
    ;(await getSettings(db)).trainingDays.push(7)
    expect((await getSettings(db)).trainingDays).toEqual([1, 3, 5])
  })
})

describe('upgrades', () => {
  const v2 = (upgrade: SchemaVersion['upgrade']): SchemaVersion[] => [
    ...SCHEMA_VERSIONS,
    { version: 2, stores: {}, upgrade },
  ]

  it('upgrade_writesSafetyCopyFirst', async () => {
    const v1 = await openDb()
    await v1.sessions.add({ date: '2026-11-02', bodyweightKg: 80, readiness: null })
    v1.close()

    const db = await openDb(
      v2(async (tx) => {
        await tx.table('sessions').toCollection().modify((s: Record<string, unknown>) => {
          s.bodyweight = s.bodyweightKg
          delete s.bodyweightKg
        })
      }),
    )
    const copy = JSON.parse((await readSafetyCopy(db))!)
    expect(copy.tables.sessions).toEqual([{ id: 1, date: '2026-11-02', bodyweightKg: 80, readiness: null }])
    const row = (await db.sessions.toArray())[0] as unknown as Record<string, unknown>
    expect(row.bodyweight).toBe(80)
    expect(row.bodyweightKg).toBeUndefined()
  })

  it('upgrade_throws_leavesDataAndOffersRecovery', async () => {
    const v1 = await openDb()
    await v1.sessions.add({ date: '2026-11-02', bodyweightKg: 80, readiness: null })
    v1.close()

    // The upgrade mutates a row and adds a store, THEN throws: only a real abort leaves v1 intact.
    const failing: SchemaVersion[] = [
      ...SCHEMA_VERSIONS,
      {
        version: 2,
        stores: { extra: '++id' },
        upgrade: async (tx) => {
          await tx.table('sessions').toCollection().modify({ bodyweightKg: 999 })
          await tx.table('extra').add({ x: 1 })
          throw new Error('boom')
        },
      },
    ]
    await expect(openGritDb(name, { schemaVersions: failing })).rejects.toBeInstanceOf(UpgradeFailedError)

    expect(await installedVersion(name)).toBe(1)
    const raw = await new Promise<IDBDatabase>((res, rej) => {
      const r = indexedDB.open(name)
      r.onsuccess = () => res(r.result)
      r.onerror = () => rej(r.error)
    })
    expect(Array.from(raw.objectStoreNames)).not.toContain('extra')
    raw.close()

    const again = await openDb()
    expect(await again.sessions.toArray()).toEqual([
      { id: 1, date: '2026-11-02', bodyweightKg: 80, readiness: null },
    ])
    const copy = JSON.parse((await readSafetyCopy(again))!)
    expect(copy.tables.sessions).toEqual([{ id: 1, date: '2026-11-02', bodyweightKg: 80, readiness: null }])
  })

  it('freshInstall_writesNoSafetyCopy', async () => {
    const db = await openDb()
    expect(await readSafetyCopy(db)).toBeNull()
  })
})
