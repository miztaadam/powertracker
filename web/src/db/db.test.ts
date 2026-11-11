import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { grit, JsBaseline, JsSession, JsSet } from '../domain'
import {
  ensureSeeded,
  getSettings,
  loadHistory,
  openGritDb,
  readSafetyCopy,
  updateSettings,
  UpgradeFailedError,
  type GritDb,
} from './db'
import { SCHEMA_VERSIONS, type SchemaVersion } from './schema'
import { sessionFromJs, setFromJs, baselineFromJs } from './mapping'

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

    await expect(
      openGritDb(name, {
        schemaVersions: v2(() => {
          throw new Error('boom')
        }),
      }),
    ).rejects.toBeInstanceOf(UpgradeFailedError)

    const again = await openDb()
    expect(await again.sessions.toArray()).toEqual([
      { id: 1, date: '2026-11-02', bodyweightKg: 80, readiness: null },
    ])
    const copy = JSON.parse((await readSafetyCopy(again))!)
    expect(copy.tables.sessions).toHaveLength(1)
  })

  it('freshInstall_writesNoSafetyCopy', async () => {
    const db = await openDb()
    expect(await readSafetyCopy(db)).toBeNull()
  })
})
