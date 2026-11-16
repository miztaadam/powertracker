import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { grit, JsAdvice, JsBaseline, JsPlanItem, JsSession, JsSet } from '../domain'
import {
  DEFAULT_SETTINGS,
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
import { decodeBackup, restoreBackup } from '../backup/backup'
import { SCHEMA_VERSIONS, type ExerciseRow, type SchemaVersion } from './schema'
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
      id: 3, date: '2026-11-02', bodyweightKg: null, readiness: null, mode: 'POWERLIFTING',
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
      showStrictPress: false,
      trainingMode: 'POWERLIFTING',
      bodybuildingTrainingDays: [1, 3, 5],
      powerliftingLeftOn: null,
      bodybuildingLeftOn: null,
      vibrateOnTaps: true,
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

  /** Strict press spec §4.2: off by default, and stored once switched. */
  /** Stage 2: "Vibrate on taps" is on by default, stored once switched, and apart from the rest alert. */
  it('vibrateOnTaps_defaultsOn_andPersists', async () => {
    const db = await openDb()
    expect(DEFAULT_SETTINGS.vibrateOnTaps).toBe(true)
    expect((await getSettings(db)).vibrateOnTaps).toBe(true)
    await updateSettings(db, (s) => ({ ...s, vibrateOnTaps: false }))
    expect((await getSettings(db)).vibrateOnTaps).toBe(false)
    expect(await db.settings.get('vibrateOnTaps')).toEqual({ key: 'vibrateOnTaps', value: false })
    expect((await getSettings(db)).restAlert).toBe('SOUND')
    await db.settings.put({ key: 'vibrateOnTaps', value: 'yes' })
    expect((await getSettings(db)).vibrateOnTaps).toBe(true)
  })

  it('showStrictPress_defaultsOff_andPersists', async () => {
    const db = await openDb()
    expect(DEFAULT_SETTINGS.showStrictPress).toBe(false)
    expect((await getSettings(db)).showStrictPress).toBe(false)
    await updateSettings(db, (s) => ({ ...s, showStrictPress: true }))
    expect((await getSettings(db)).showStrictPress).toBe(true)
    expect(await db.settings.get('showStrictPress')).toEqual({ key: 'showStrictPress', value: true })
    await updateSettings(db, (s) => ({ ...s, showStrictPress: false }))
    expect((await getSettings(db)).showStrictPress).toBe(false)
    // A wrong-typed stored value falls back to off.
    await db.settings.put({ key: 'showStrictPress', value: 'yes' })
    expect((await getSettings(db)).showStrictPress).toBe(false)
  })

  it('defaults_areNotShared', async () => {
    const db = await openDb()
    ;(await getSettings(db)).trainingDays.push(7)
    expect((await getSettings(db)).trainingDays).toEqual([1, 3, 5])
  })
})

describe('upgrades', () => {
  // The installed version and the next one a test injects on top of it.
  const LATEST = Math.max(...SCHEMA_VERSIONS.map((v) => v.version))
  const NEXT = LATEST + 1
  const v2 = (upgrade: SchemaVersion['upgrade']): SchemaVersion[] => [
    ...SCHEMA_VERSIONS,
    { version: NEXT, stores: {}, upgrade },
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
    const copy = JSON.parse((await readSafetyCopy(db))!.json)
    expect(copy.sessions).toEqual([{ id: 1, date: 20759, bodyweightKg: 80, readiness: null, mode: 'POWERLIFTING' }])
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
        version: NEXT,
        stores: { extra: '++id' },
        upgrade: async (tx) => {
          await tx.table('sessions').toCollection().modify({ bodyweightKg: 999 })
          await tx.table('extra').add({ x: 1 })
          throw new Error('boom')
        },
      },
    ]
    await expect(openGritDb(name, { schemaVersions: failing })).rejects.toBeInstanceOf(UpgradeFailedError)

    expect(await installedVersion(name)).toBe(LATEST)
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
    const copy = JSON.parse((await readSafetyCopy(again))!.json)
    expect(copy.sessions).toEqual([{ id: 1, date: 20759, bodyweightKg: 80, readiness: null, mode: 'POWERLIFTING' }])
  })

  it('upgrade_malformedStoredDate_fallsBackToRawCopyAndProceeds', async () => {
    const v1 = await openDb()
    await v1.sessions.add({ date: 'garbage', bodyweightKg: 80, readiness: null })
    v1.close()
    const db = await openDb(v2(undefined))
    const copy = (await readSafetyCopy(db))!
    expect(copy.kind).toBe('raw')
    const parsed = JSON.parse(copy.json)
    expect(parsed.kind).toBe('raw')
    expect(parsed.tables.sessions[0].date).toBe('garbage')
    expect(await installedVersion(name)).toBe(NEXT)
  })

  it('upgrade_copyWriteFails_throwsUpgradeFailedError', async () => {
    const v1 = await openDb()
    await v1.sessions.add({ date: '2026-11-02', bodyweightKg: 80, readiness: null })
    v1.close()
    const failMetaWrites = (d: Dexie) => {
      if (d.name !== name) return
      d.use({
        stack: 'dbcore',
        name: 'failMeta',
        create: (down) => ({
          ...down,
          table: (n) => {
            const t = down.table(n)
            return n === 'meta' ? { ...t, mutate: () => Promise.reject(new Error('quota')) } : t
          },
        }),
      })
    }
    Dexie.addons.push(failMetaWrites)
    try {
      await expect(openGritDb(name, { schemaVersions: v2(undefined) })).rejects.toBeInstanceOf(UpgradeFailedError)
    } finally {
      Dexie.addons.splice(Dexie.addons.indexOf(failMetaWrites), 1)
    }
    expect(await installedVersion(name)).toBe(LATEST)
  })

  it('freshInstall_writesNoSafetyCopy', async () => {
    const db = await openDb()
    expect(await readSafetyCopy(db)).toBeNull()
  })
})

/** Strict press spec §4.1: the one-time upgrade that turns the catalogue's former "Overhead press" into the fourth main lift. */
describe('strict press upgrade', () => {
  const V1 = SCHEMA_VERSIONS.filter((v) => v.version === 1)
  type Row = Pick<ExerciseRow, 'name' | 'category' | 'isBuiltIn' | 'archived'> & { id: number }

  /** A version-1 database holding the three main lifts and `extra`, with `write` run on it before it closes. */
  async function version1(extra: Row[], write: (db: GritDb) => Promise<unknown> = async () => {}) {
    const db = await openDb(V1)
    const exercise = (r: Row): ExerciseRow => ({ parentLift: null, muscles: ['Delts'], ...r })
    await db.exercises.bulkAdd([
      exercise({ id: 1, name: 'Squat', category: 'SQUAT', isBuiltIn: true, archived: false }),
      exercise({ id: 2, name: 'Bench', category: 'BENCH', isBuiltIn: true, archived: false }),
      exercise({ id: 3, name: 'Deadlift', category: 'DEADLIFT', isBuiltIn: true, archived: false }),
      ...extra.map(exercise),
    ])
    await write(db)
    db.close()
  }
  const session = (db: GritDb, id: number, date: string) => db.sessions.add({ id, date, bodyweightKg: null, readiness: null })
  const set = (db: GritDb, id: number, sessionId: number, exerciseId: number, weightKg: number) =>
    db.sets.add({ id, sessionId, exerciseId, weightKg, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: true })
  const planItem = (db: GritDb, id: number, exerciseId: number) =>
    db.planItems.add({ id, weekday: 1, exerciseId, sets: 3, reps: 5, targetRpe: 8, order: 0 })
  const rows = async (db: GritDb): Promise<Row[]> =>
    (await db.exercises.orderBy('id').toArray()).map((e) => ({ id: e.id!, name: e.name, category: e.category, isBuiltIn: e.isBuiltIn, archived: e.archived }))
  const pairs = async (db: GritDb, table: 'sets' | 'planItems') => (await db[table].orderBy('id').toArray()).map((r) => [r.id, r.exerciseId])
  const MAIN: Row[] = [
    { id: 1, name: 'Squat', category: 'SQUAT', isBuiltIn: true, archived: false },
    { id: 2, name: 'Bench', category: 'BENCH', isBuiltIn: true, archived: false },
    { id: 3, name: 'Deadlift', category: 'DEADLIFT', isBuiltIn: true, archived: false },
  ]

  it('upgrade_overheadPressRow_becomesStrictPress_keepsSets', async () => {
    await version1([{ id: 4, name: 'Overhead press', category: 'ACCESSORY', isBuiltIn: false, archived: false }], async (db) => {
      await session(db, 1, '2025-11-08')
      await set(db, 1, 1, 4, 60)
      await set(db, 2, 1, 4, 62.5)
      await set(db, 3, 1, 1, 140)
      await planItem(db, 1, 4)
    })

    const db = await openDb()

    expect(await installedVersion(name)).toBe(Math.max(...SCHEMA_VERSIONS.map((v) => v.version)))
    expect(await rows(db)).toEqual([...MAIN, { id: 4, name: 'Strict press', category: 'STRICT_PRESS', isBuiltIn: true, archived: false }])
    expect(await pairs(db, 'sets')).toEqual([[1, 4], [2, 4], [3, 1]])
    expect(await pairs(db, 'planItems')).toEqual([[1, 4]])
    // The safety copy was written first, from the data as it was.
    const copy = JSON.parse((await readSafetyCopy(db))!.json)
    expect(copy.exercises.find((e: { id: number }) => e.id === 4)).toMatchObject({ name: 'Overhead press', category: 'ACCESSORY' })
    // It is labelled as the version-1 data it is, so restoring it applies the upgrade again.
    expect(copy.version).toBe(1)
    const decoded = decodeBackup((await readSafetyCopy(db))!.json)
    if (!decoded.ok) throw new Error('the safety copy must decode')
    await restoreBackup(db, decoded.file)
    expect((await rows(db)).at(-1)).toEqual({ id: 4, name: 'Strict press', category: 'STRICT_PRESS', isBuiltIn: true, archived: false })
  })

  /** Review focus 3: the lifter's own "Strict press" and the catalogue's "Overhead press" both exist. */
  it('upgrade_twoMatchingRows_keepsAllHistory', async () => {
    await version1(
      [
        { id: 4, name: 'Overhead press', category: 'ACCESSORY', isBuiltIn: true, archived: false },
        { id: 5, name: 'Strict press', category: 'ACCESSORY', isBuiltIn: false, archived: false },
        { id: 6, name: 'OHP', category: 'VARIATION', isBuiltIn: false, archived: true },
        { id: 7, name: 'military PRESS', category: 'ACCESSORY', isBuiltIn: false, archived: false },
      ],
      async (db) => {
        await session(db, 1, '2025-11-08')
        await session(db, 2, '2025-11-15')
        await set(db, 1, 1, 4, 60)
        await set(db, 2, 1, 5, 57.5)
        await set(db, 3, 2, 6, 55)
        await set(db, 4, 2, 7, 50)
        await planItem(db, 1, 4)
        await planItem(db, 2, 5)
      },
    )

    const db = await openDb()

    expect(await rows(db)).toEqual([
      ...MAIN,
      { id: 4, name: 'Strict press', category: 'STRICT_PRESS', isBuiltIn: true, archived: false },
      { id: 5, name: 'Strict press', category: 'STRICT_PRESS', isBuiltIn: false, archived: false },
      // Archived rows are left alone.
      { id: 6, name: 'OHP', category: 'VARIATION', isBuiltIn: false, archived: true },
      { id: 7, name: 'military PRESS', category: 'STRICT_PRESS', isBuiltIn: false, archived: false },
    ])
    expect(await pairs(db, 'sets')).toEqual([[1, 4], [2, 5], [3, 6], [4, 7]])
    expect(await pairs(db, 'planItems')).toEqual([[1, 4], [2, 5]])
  })

  it('upgrade_noMatch_noChange', async () => {
    const extra: Row[] = [
      { id: 4, name: 'Push press', category: 'ACCESSORY', isBuiltIn: false, archived: false },
      { id: 5, name: 'DB shoulder press', category: 'ACCESSORY', isBuiltIn: true, archived: false },
      { id: 6, name: 'Overhead press machine', category: 'ACCESSORY', isBuiltIn: false, archived: false },
    ]
    await version1(extra, async (db) => {
      await session(db, 1, '2025-11-08')
      await set(db, 1, 1, 4, 70)
    })
    const before = await (async () => {
      const v1 = await openDb(V1)
      const all = await v1.exercises.orderBy('id').toArray()
      v1.close()
      return all
    })()

    const db = await openDb()

    expect(await installedVersion(name)).toBe(Math.max(...SCHEMA_VERSIONS.map((v) => v.version)))
    expect(await db.exercises.orderBy('id').toArray()).toEqual(before)
    expect(await pairs(db, 'sets')).toEqual([[1, 4]])
  })

  it('upgrade_twice_isIdempotent', async () => {
    await version1(
      [
        { id: 4, name: 'Strict press', category: 'ACCESSORY', isBuiltIn: false, archived: false },
        { id: 5, name: 'Overhead press', category: 'ACCESSORY', isBuiltIn: true, archived: false },
      ],
      async (db) => {
        await session(db, 1, '2025-11-08')
        await set(db, 1, 1, 4, 60)
        await set(db, 2, 1, 5, 60)
      },
    )
    const db = await openDb()
    const once = await db.exercises.orderBy('id').toArray()
    db.close()

    // The same upgrade again, as a version on top.
    const upgrade = SCHEMA_VERSIONS.find((v) => v.version === 2)!.upgrade
    const again = await openDb([...SCHEMA_VERSIONS, { version: 5, stores: {}, upgrade }])

    expect(await installedVersion(name)).toBe(5)
    expect(await again.exercises.orderBy('id').toArray()).toEqual(once)
    expect((await rows(again)).slice(3)).toEqual([
      { id: 4, name: 'Strict press', category: 'STRICT_PRESS', isBuiltIn: true, archived: false },
      { id: 5, name: 'Overhead press', category: 'STRICT_PRESS', isBuiltIn: true, archived: false },
    ])
    expect(await pairs(again, 'sets')).toEqual([[1, 4], [2, 5]])
  })

  it('upgrade_changesNoTableDefinition', async () => {
    expect(SCHEMA_VERSIONS.find((v) => v.version === 2)?.stores).toEqual({})
    const tables = (db: GritDb) =>
      db.tables.map((t) => [t.name, t.schema.primKey.src, t.schema.indexes.map((i) => i.src).sort()]).sort()
    await version1([])
    const v1 = await openDb(V1)
    const before = tables(v1)
    v1.close()
    expect(tables(await openDb(SCHEMA_VERSIONS.filter((v) => v.version <= 2)))).toEqual(before)
  })
})
