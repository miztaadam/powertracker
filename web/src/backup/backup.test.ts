import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { ensureSeeded, getSettings, openGritDb, updateSettings, type GritDb } from '../db/db'
import { readSafetyCopy } from '../db/safety'
import {
  androidRestAlertOf,
  backupFileName,
  decodeBackup,
  encodeBackup,
  exportBackup,
  markBackedUp,
  restoreBackup,
  webRestAlertOf,
  isoToEpochDay,
  epochDayToIso,
} from './backup'
import type { BackupFile } from './format'

const FIXTURES = resolve(__dirname, '__fixtures__')
const ANDROID_FIXTURE = resolve(FIXTURES, 'android-v1.json')
const WEB_FIXTURE = resolve(FIXTURES, 'web-v1.json')
const EXPORTED_AT = 1_793_000_000_000

let n = 0
let name = ''
const open: GritDb[] = []
beforeEach(() => {
  name = `grit-backup-test-${++n}`
})
afterEach(async () => {
  for (const d of open.splice(0)) d.close()
  await Dexie.delete(name)
})
async function openDb() {
  const db = await openGritDb(name)
  open.push(db)
  return db
}

const readJson = (path: string) => JSON.parse(readFileSync(path, 'utf8')) as Record<string, unknown>
const withoutExportedAt = (o: Record<string, unknown>) => {
  const { exportedAt: _ignored, ...rest } = o
  return rest
}
const decodeOk = (text: string): BackupFile => {
  const r = decodeBackup(text)
  if (!r.ok) throw new Error(`decode failed: ${JSON.stringify(r)}`)
  return r.file
}

/** Mirrors the Android fixture's breadth, deterministically. */
async function buildSampleDb(db: GritDb) {
  await ensureSeeded(db)
  const byName = new Map((await db.exercises.toArray()).map((e) => [e.name, e.id as number]))
  const squat = byName.get('Squat')!
  const bench = byName.get('Bench')!
  const pause = byName.get('Pause squat')!
  const custom = (await db.exercises.add({
    name: 'Zercher squat',
    category: 'VARIATION',
    parentLift: 'SQUAT',
    muscles: ['Quads', 'Core'],
    isBuiltIn: false,
    archived: false,
  })) as number
  await updateSettings(db, (s) => ({
    ...s,
    changePlates: true,
    defaultRpe: 7.5,
    trainingDays: [6, 2, 4],
    appLock: true,
    restMainSec: 210,
    restAccessorySec: 75,
    restAlert: 'SILENT',
    keepAwake: false,
  }))
  const s1 = (await db.sessions.add({ date: '2026-10-27', bodyweightKg: 84.5, readiness: 4 })) as number
  const s2 = (await db.sessions.add({ date: '2026-10-29', bodyweightKg: null, readiness: null })) as number
  const set = (sessionId: number, exerciseId: number, weightKg: number, reps: number, rpe: number | null,
    failed: boolean, missPoint: string | null, isTopSet: boolean) =>
    db.sets.add({ sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet })
  await set(s1, squat, 140, 5, 8, false, null, true)
  await set(s1, squat, 147.5, 3, null, true, 'MID', false)
  await set(s1, custom, 100, 6, 7.5, false, null, false)
  await set(s2, bench, 102.5, 4, 9, false, null, true)
  await set(s2, bench, 105, 1, 10, true, 'LOCKOUT', false)
  await db.planItems.bulkAdd([
    { weekday: 2, exerciseId: squat, sets: 4, reps: 5, targetRpe: 8, order: 0 },
    { weekday: 2, exerciseId: pause, sets: 3, reps: 4, targetRpe: 7.5, order: 1 },
    { weekday: 6, exerciseId: bench, sets: 5, reps: 3, targetRpe: 8.5, order: 0 },
  ])
  const P = 'com.powertracker.domain.model.AdvicePayload.'
  const adv = (type: string, lift: string | null, reason: string, severity: number, payloadJson: string, status: string) =>
    ({ type, lift, createdAt: '2026-10-30', reason, severity, status, payloadJson })
  await db.advice.bulkAdd([
    adv('TARGET', 'SQUAT', 'Last top set was clean at RPE 8', 1,
      `{"type":"${P}Target","exerciseId":${squat},"weightKg":142.5,"reps":5,"rpe":8.0,"sets":4}`, 'OPEN'),
    adv('STALL', 'BENCH', 'No bench PR in four weeks', 2,
      `{"type":"${P}Accessories","items":[{"exerciseName":"Pause bench","sets":3,"repsLow":5,"repsHigh":8}]}`, 'APPLIED'),
    adv('DELOAD', null, 'Fatigue is building', 3, `{"type":"${P}Deload"}`, 'DISMISSED'),
    adv('WEAK_POINT', 'SQUAT', 'Misses cluster mid-lift', 2, `{"type":"${P}WeakPoint","lift":"SQUAT","missPoint":"MID"}`, 'OPEN'),
    adv('RE_ENTRY', null, 'Back after a break', 1, `{"type":"${P}ReEntry"}`, 'OPEN'),
  ])
  await db.baselines.put({ exerciseId: squat, e1rmKg: 172.5, date: '2026-09-01' })
}

describe('backup', () => {
  it('androidFixture_restoresExactly', async () => {
    const db = await openDb()
    await ensureSeeded(db)
    await db.sessions.add({ date: '2000-01-01', bodyweightKg: null, readiness: null }) // must be wiped
    const fixtureText = readFileSync(ANDROID_FIXTURE, 'utf8')
    await restoreBackup(db, decodeOk(fixtureText))
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>

    const expected = withoutExportedAt(readJson(ANDROID_FIXTURE))
    // The documented differences: web fills webRestAlert, and restAlert round-trips through web SOUND.
    expect((expected.settings as Record<string, unknown>).webRestAlert).toBeNull()
    expect((expected.settings as Record<string, unknown>).restAlert).toBe('SOUND_AND_VIBRATE')
    ;(expected.settings as Record<string, unknown>).webRestAlert = 'SOUND'
    expect(withoutExportedAt(out)).toEqual(expected)
  })

  it('restore_forgetsTheTodayScreensNotesAboutTheReplacedData', async () => {
    const db = await openDb()
    // What the Today screen keeps for a reload: the session being logged and the unlogged numbers.
    await db.meta.bulkPut([
      { key: 'today:active', value: { date: '2026-11-11', at: Date.now() } },
      { key: 'editor:2026-11-11:1', value: { weightKg: 152.5, reps: 3, rpe: 9 } },
      { key: 'unrelated', value: 'kept' },
    ])
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_FIXTURE, 'utf8')))
    expect(await db.meta.toCollection().primaryKeys()).toEqual(['unrelated'])
  })

  it('restore_keepsWebOnlySettingsAndNeverTouchesLastBackupAt', async () => {
    const db = await openDb()
    await updateSettings(db, (s) => ({ ...s, keepAwake: false, lastBackupAt: '2026-01-01T00:00:00.000Z' }))
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_FIXTURE, 'utf8')))
    const s = await getSettings(db)
    expect(s.keepAwake).toBe(false)
    expect(s.lastBackupAt).toBe('2026-01-01T00:00:00.000Z')
    expect(s.restAlert).toBe('SOUND')
    expect(s.trainingDays).toEqual([2, 4, 6])
  })

  it('export_isPureAndMarkBackedUpSetsLastBackupAt', async () => {
    const db = await openDb()
    await exportBackup(db, EXPORTED_AT)
    expect((await getSettings(db)).lastBackupAt).toBeNull()
    await markBackedUp(db, EXPORTED_AT)
    expect((await getSettings(db)).lastBackupAt).toBe(new Date(EXPORTED_AT).toISOString())
  })

  it('export_roundTripsThroughEncodeDecodeRestore', async () => {
    const db = await openDb()
    await buildSampleDb(db)
    const first = await exportBackup(db, EXPORTED_AT)
    await db.sets.clear()
    await restoreBackup(db, decodeOk(encodeBackup(first)))
    expect(await exportBackup(db, EXPORTED_AT)).toEqual(first)
  })

  it('duplicateSessionDates_areMerged', async () => {
    const o = readJson(ANDROID_FIXTURE) as unknown as BackupFile
    o.sessions.push({ id: 3, date: o.sessions[0]!.date, bodyweightKg: 90, readiness: 2 })
    o.sessions[0]!.bodyweightKg = null // first non-null (by id) wins per field
    o.sets.push({ id: 6, sessionId: 3, exerciseId: 2, weightKg: 60, reps: 8, rpe: null, failed: false, missPoint: null, isTopSet: false })
    const db = await openDb()
    await restoreBackup(db, decodeOk(JSON.stringify(o)))
    const sessions = await db.sessions.toArray()
    expect(sessions.map((s) => s.id)).toEqual([1, 2])
    expect(sessions[0]).toMatchObject({ bodyweightKg: 90, readiness: 4 })
    expect((await db.sets.where('sessionId').equals(1).toArray()).map((s) => s.id).sort()).toEqual([1, 2, 3, 6])
    const out = await exportBackup(db, EXPORTED_AT)
    expect(out.sessions.map((s) => s.id)).toEqual([1, 2])
    expect(out.sets.map((s) => s.id)).toEqual([1, 2, 3, 4, 5, 6])
    expect(decodeBackup(encodeBackup(out)).ok).toBe(true)
  })

  it('isoToEpochDay_validatesCalendar', () => {
    expect(() => isoToEpochDay('2026-02-31')).toThrow()
    expect(() => isoToEpochDay('2026-13-01')).toThrow()
    expect(epochDayToIso(isoToEpochDay('0050-03-04'))).toBe('0050-03-04')
    expect(isoToEpochDay('2026-10-27')).toBe(20753)
  })

  it('restore_unknownWebRestAlert_fallsBackToAndroidValue', () => {
    expect(webRestAlertOf('OFF', 'LOUD')).toBe('OFF')
    expect(webRestAlertOf('VIBRATE', 'LOUD')).toBe('SOUND')
  })

  it('backupFileName_format', () => {
    expect(backupFileName('2026-11-11')).toBe('grit-backup-2026-11-11.json')
  })

  it('restAlert_mapping', () => {
    expect(androidRestAlertOf('SOUND')).toBe('SOUND_AND_VIBRATE')
    expect(androidRestAlertOf('SILENT')).toBe('VIBRATE')
    expect(androidRestAlertOf('OFF')).toBe('OFF')
    // webRestAlert wins when present
    expect(webRestAlertOf('VIBRATE', 'SOUND')).toBe('SOUND')
    expect(webRestAlertOf('SOUND_AND_VIBRATE', 'SILENT')).toBe('SILENT')
    expect(webRestAlertOf('OFF', 'OFF')).toBe('OFF')
    // otherwise the Android value maps
    expect(webRestAlertOf('VIBRATE', null)).toBe('SOUND')
    expect(webRestAlertOf('SOUND_AND_VIBRATE', null)).toBe('SOUND')
    expect(webRestAlertOf('OFF', null)).toBe('OFF')
  })

  it('restAlert_webValuesRoundTripThroughRestore', async () => {
    const db = await openDb()
    for (const v of ['SOUND', 'SILENT', 'OFF'] as const) {
      await updateSettings(db, (s) => ({ ...s, restAlert: v }))
      const file = await exportBackup(db, EXPORTED_AT)
      expect(file.settings.restAlert).toBe(androidRestAlertOf(v))
      expect(file.settings.webRestAlert).toBe(v)
      await updateSettings(db, (s) => ({ ...s, restAlert: v === 'OFF' ? 'SOUND' : 'OFF' }))
      await restoreBackup(db, file)
      expect((await getSettings(db)).restAlert).toBe(v)
    }
  })

  it('decode_rejects', () => {
    const valid = readJson(ANDROID_FIXTURE)
    type Loose = BackupFile & { extra?: number; settings: { extra?: number } }
    const mut = (f: (o: Loose) => void) => {
      const o = structuredClone(valid) as unknown as Loose
      f(o)
      return JSON.stringify(o)
    }
    const corrupt = (text: string) => {
      const r = decodeBackup(text)
      expect(r.ok, text.slice(0, 80)).toBe(false)
      expect(r.ok === false && r.kind).toBe('CORRUPT')
    }
    for (const t of ['not json', '[1]', '{}', '{"version":"x"}', '{"version":0}', '{"version":-1}', '{"version":"1"}', 'null']) corrupt(t)
    expect(decodeBackup('{"version":2}')).toEqual({ ok: false, kind: 'TOO_NEW', version: 2 })
    corrupt(mut((o) => (o.exercises[0]!.category = 'NOPE')))
    corrupt(mut((o) => (o.exercises[3]!.parentLift = 'ROW')))
    corrupt(mut((o) => (o.sets[1]!.missPoint = 'TOP')))
    corrupt(mut((o) => (o.advice[0]!.type = 'X')))
    corrupt(mut((o) => (o.advice[0]!.status = 'DONE')))
    corrupt(mut((o) => (o.settings.restAlert = 'LOUD')))
    corrupt(mut((o) => (o.planItems[0]!.weekday = 8)))
    corrupt(mut((o) => (o.settings.trainingDays = [0])))
    corrupt(mut((o) => (o.extra = 1)))
    corrupt(mut((o) => (o.settings.extra = 1)))
    corrupt(mut((o) => ((o.sets[0] as Record<string, unknown>).extra = 1)))
    corrupt(mut((o) => delete (o as Partial<Loose>).baselines))
    corrupt(mut((o) => delete (o.sets[0] as Partial<Loose['sets'][number]>).rpe))
    corrupt(mut((o) => (o.sets[0]!.reps = 1.5)))
    corrupt(mut((o) => ((o.sets[0] as unknown as Record<string, unknown>).weightKg = '100')))
    corrupt(mut((o) => (o.sets[0]!.sessionId = 99)))
    corrupt(mut((o) => (o.sessions[1]!.id = o.sessions[0]!.id)))
    corrupt(mut((o) => (o.exercises[1]!.id = o.exercises[0]!.id)))
    corrupt(mut((o) => (o.sets[1]!.id = o.sets[0]!.id)))
    corrupt(mut((o) => o.baselines.push({ ...o.baselines[0]! })))
    corrupt(mut((o) => (o.planItems[0]!.exerciseId = 9999)))
    corrupt(mut((o) => (o.baselines[0]!.exerciseId = 9999)))
    corrupt(mut((o) => (o.sessions[0]!.date = 99_999_999)))
    expect(decodeBackup(JSON.stringify(valid)).ok).toBe(true)
  })

  it('decode_acceptsMissingRestAlertAsVibrate', () => {
    const o = readJson(ANDROID_FIXTURE) as { settings: Record<string, unknown> }
    delete o.settings.restAlert
    delete o.settings.webRestAlert
    const f = decodeOk(JSON.stringify(o))
    expect(f.settings.restAlert).toBe('VIBRATE')
    expect(f.settings.webRestAlert).toBeNull()
  })

  it('restore_invalidContent_leavesDataUntouched', async () => {
    const db = await openDb()
    await buildSampleDb(db)
    const before = await exportBackup(db, EXPORTED_AT)
    const bad = structuredClone(decodeOk(readFileSync(ANDROID_FIXTURE, 'utf8')))
    bad.sets[0]!.exerciseId = 9999
    await expect(restoreBackup(db, bad)).rejects.toThrow()
    const bad2 = structuredClone(bad)
    bad2.sets[0]!.exerciseId = 1
    bad2.exercises[0]!.category = 'NOPE'
    await expect(restoreBackup(db, bad2)).rejects.toThrow()
    expect(await exportBackup(db, EXPORTED_AT)).toEqual(before)
  })

  it('restore_failureInsideTransaction_rollsBack', async () => {
    const db = await openDb()
    await buildSampleDb(db)
    const before = await exportBackup(db, EXPORTED_AT)
    const file = structuredClone(decodeOk(readFileSync(ANDROID_FIXTURE, 'utf8')))
    // Passes validation, then makes the insert throw part-way (exercise rows are inserted first).
    const boom = () => {
      throw new Error('disk full')
    }
    db.sets.hook('creating', boom)
    await expect(restoreBackup(db, file)).rejects.toThrow('disk full')
    db.sets.hook('creating').unsubscribe(boom)
    expect(await exportBackup(db, EXPORTED_AT)).toEqual(before)
  })

  it('safetyCopy_isARealBackup', async () => {
    const db = await openDb()
    await buildSampleDb(db)
    const { writeSafetyCopy } = await import('../db/safety')
    await writeSafetyCopy(db)
    const copy = decodeOk((await readSafetyCopy(db))!.json)
    expect(copy.sessions).toHaveLength(2)
    expect(copy.version).toBe(1)
  })

  it('webFixture_isUpToDate', async () => {
    const db = await openDb()
    await buildSampleDb(db)
    const text = encodeBackup(await exportBackup(db, EXPORTED_AT))
    if (process.env.UPDATE_FIXTURES === '1') {
      writeFileSync(WEB_FIXTURE, JSON.stringify(JSON.parse(text), null, 4) + '\n')
    }
    expect(existsSync(WEB_FIXTURE), `Missing ${WEB_FIXTURE}; run with UPDATE_FIXTURES=1`).toBe(true)
    expect(withoutExportedAt(readJson(WEB_FIXTURE))).toEqual(withoutExportedAt(JSON.parse(text)))
    const file = decodeOk(readFileSync(WEB_FIXTURE, 'utf8'))
    expect(file.settings.restAlert).toBe('VIBRATE')
    expect(file.settings.webRestAlert).toBe('SILENT')
  })
})
