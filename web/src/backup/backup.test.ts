import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { planFor, resolveCatalogue } from '../db/catalogue'
import { activeTrainingDays, ensureSeeded, getSettings, openGritDb, setTrainingMode, updateSettings, type GritDb } from '../db/db'
import { readSafetyCopy } from '../db/safety'
import {
  androidRestAlertOf,
  backupFileName,
  decodeBackup,
  encodeBackup,
  exportBackup,
  markBackedUp,
  onDataReplaced,
  restoreBackup,
  webRestAlertOf,
  isoToEpochDay,
  epochDayToIso,
} from './backup'
import { BACKUP_VERSION, type BackupFile } from './format'
import { grit } from '../domain'

const FIXTURES = resolve(__dirname, '__fixtures__')
/** What the previous apps wrote (version 1): frozen, and must keep restoring. */
const ANDROID_FIXTURE = resolve(FIXTURES, 'android-v1.json')
const WEB_V1_FIXTURE = resolve(FIXTURES, 'web-v1.json')
/** What the strict press apps wrote (version 2): frozen since version 3, and must keep restoring. */
const ANDROID_V2_FIXTURE = resolve(FIXTURES, 'android-v2.json')
const WEB_V2_FIXTURE = resolve(FIXTURES, 'web-v2.json')
/** What the bodybuilding mode apps wrote (version 3): frozen since version 4, and must keep restoring. */
const WEB_V3_FIXTURE = resolve(FIXTURES, 'web-v3.json')
/** What this app writes now (version 4, check-ins). Regenerate with UPDATE_FIXTURES=1. */
const WEB_FIXTURE = resolve(FIXTURES, 'web-v4.json')
/** What the Android app writes now (version 4, stage 3 Task 2). */
const ANDROID_V4_FIXTURE = resolve(FIXTURES, 'android-v4.json')
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

/**
 * Mirrors the Android fixture's breadth, deterministically. `strictPress` adds what version 2 brought, as
 * Android's fixture has it: the switch on and a catalogue "Strict press" row with an "off the shoulders" miss.
 */
async function buildSampleDb(db: GritDb, { strictPress = false, bodybuilding = false, checkIns = false } = {}) {
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
  if (strictPress) {
    await updateSettings(db, (s) => ({ ...s, showStrictPress: true }))
    const press = await resolveCatalogue(db, 'Strict press')
    await set(s2, press, 60, 5, 8.5, false, null, true)
    await set(s2, press, 65, 2, 10, true, 'FLOOR', false)
  }
  if (bodybuilding) {
    // Version 3: on, off and on again, so both sides have been left once; the bodybuilding side has its own days,
    // its own plan, one session and one piece of advice.
    await setTrainingMode(db, 'BODYBUILDING', '2026-10-20')
    await setTrainingMode(db, 'POWERLIFTING', '2026-10-24')
    await setTrainingMode(db, 'BODYBUILDING', '2026-10-31')
    await updateSettings(db, (s) => ({ ...s, bodybuildingTrainingDays: [1, 3, 5, 6] }))
    const curl = await resolveCatalogue(db, 'Curl')
    await db.planItems.bulkAdd([
      { weekday: 1, exerciseId: curl, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' },
      { weekday: 1, exerciseId: squat, sets: 3, reps: 10, repsHigh: 15, targetRpe: 8, order: 1, mode: 'BODYBUILDING' },
    ])
    const s3 = (await db.sessions.add({ date: '2026-10-31', bodyweightKg: 84, readiness: 3, mode: 'BODYBUILDING' })) as number
    await set(s3, curl, 14, 11, 8, false, null, false)
    await set(s3, curl, 14, 10, 8, false, null, false)
    await set(s3, curl, 14, 9, 8.5, false, null, false)
    await db.advice.add({
      ...adv('TARGET', null, 'Beat it.', 1,
        `{"type":"${P}Target","exerciseId":${curl},"weightKg":14.0,"reps":8,"rpe":8.0,"sets":3,"repsHigh":12}`, 'OPEN'),
      mode: 'BODYBUILDING',
    })
  }
  if (checkIns) {
    // Version 4: two check-ins, a MORE_WORK and a SWAP advice, with their payloads exactly as the domain writes them.
    await db.checkIns.bulkAdd([
      { date: '2026-10-03', lagging: [], growingWell: ['BICEPS'] },
      { date: '2026-10-31', lagging: ['CHEST', 'SIDE_DELTS'], growingWell: ['QUADS'] },
    ])
    const curl = await resolveCatalogue(db, 'Curl')
    await db.advice.bulkAdd([
      {
        ...adv('MORE_WORK', null, 'You marked chest as lagging. Add Pec deck on Monday, 3 × 8–12 @ 8. That takes it from 0 to 3 sets a week.', 0,
          `{"type":"${P}MoreWork","group":"CHEST","addSets":[],"newExercise":{"weekday":"MONDAY","name":"Pec deck","sets":3,"repsLow":8,"repsHigh":12,"rpe":8},"weeklyBefore":0,"weeklyAfter":3}`, 'OPEN'),
        mode: 'BODYBUILDING',
      },
      {
        ...adv('SWAP', null, "Curl hasn't moved in 3 weeks. Same weight and no more reps since 3 Oct.", 0,
          `{"type":"${P}Swap","exerciseId":${curl},"name":"Curl","group":"BICEPS","since":"2026-10-03","alternatives":["Cable curl","EZ-bar curl","Incline DB curl"]}`, 'DISMISSED'),
        mode: 'BODYBUILDING',
      },
    ])
  }
}

/** Version 3's fields as an older file means them: everything on the powerlifting side, neither side ever left. */
function withV3Defaults(file: Record<string, unknown>): Record<string, unknown> {
  const rows = (key: string, extra: Record<string, unknown>) =>
    (file[key] as Record<string, unknown>[]).map((r) => ({ ...r, ...extra }))
  return {
    ...file,
    version: BACKUP_VERSION,
    settings: {
      ...(file.settings as object),
      trainingMode: 'POWERLIFTING',
      bodybuildingTrainingDays: [1, 3, 5],
      powerliftingLeftOn: null,
      bodybuildingLeftOn: null,
    },
    sessions: rows('sessions', { mode: 'POWERLIFTING' }),
    planItems: rows('planItems', { mode: 'POWERLIFTING', repsHigh: null }),
    advice: rows('advice', { mode: 'POWERLIFTING' }),
    // Version 4: no check-ins.
    checkIns: [],
  }
}

/** A version 3 file as it exports here once restored: the current version, with no check-ins. */
function withV4Defaults(file: Record<string, unknown>): Record<string, unknown> {
  return { ...file, version: BACKUP_VERSION, checkIns: [] }
}

/** What a version-1 file with no strict press row exports as once restored here: the current version, switch off, all powerlifting. */
function asUpgradedV1(v1: Record<string, unknown>): Record<string, unknown> {
  expect(v1.version).toBe(1)
  return withoutExportedAt(withV3Defaults({ ...v1, settings: { ...(v1.settings as object), showStrictPress: false } }))
}

/** `text` as the previous app wrote it: version 1, and no setting it did not have. */
function asVersion1(text: string): string {
  const root = JSON.parse(text) as Record<string, unknown>
  const { showStrictPress: _dropped, ...settings } = root.settings as Record<string, unknown>
  return JSON.stringify({ ...root, version: 1, settings })
}

describe('backup', () => {
  it('androidFixture_restoresExactly', async () => {
    const db = await openDb()
    await ensureSeeded(db)
    await db.sessions.add({ date: '2000-01-01', bodyweightKg: null, readiness: null }) // must be wiped
    const fixtureText = readFileSync(ANDROID_FIXTURE, 'utf8')
    await restoreBackup(db, decodeOk(fixtureText))
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>

    // A version-1 file exports again as version 2 with the strict press switch off.
    const expected = asUpgradedV1(readJson(ANDROID_FIXTURE))
    // The documented differences: web fills webRestAlert, and restAlert round-trips through web SOUND.
    expect((expected.settings as Record<string, unknown>).webRestAlert).toBeNull()
    expect((expected.settings as Record<string, unknown>).restAlert).toBe('SOUND_AND_VIBRATE')
    ;(expected.settings as Record<string, unknown>).webRestAlert = 'SOUND'
    // And App lock is this device's own: the backup's phone was locked, this one is not and stays so.
    expect((expected.settings as Record<string, unknown>).appLock).toBe(true)
    ;(expected.settings as Record<string, unknown>).appLock = false
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

  it('restore_clearsTheRestTimer_keepsWhatBelongsToThisDevice_andTellsListeners', async () => {
    const db = await openDb()
    await db.meta.bulkPut([
      { key: 'restTimer', value: { endsAtMs: Date.now() + 60_000, totalSec: 180 } },
      { key: 'lock:credentialId', value: 'AQID' },
      { key: 'install:dismissed', value: true },
      { key: 'backupReminder:snoozedOn', value: '2026-11-11' },
      { key: 'safetyCopy', value: '{}' },
    ])
    let told = 0
    const stop = onDataReplaced(() => told++)
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_FIXTURE, 'utf8')))
    stop()
    expect(await db.meta.toCollection().primaryKeys()).toEqual(['backupReminder:snoozedOn', 'install:dismissed', 'lock:credentialId', 'safetyCopy'])
    expect(told).toBe(1)

    // A restore that fails replaced nothing, and says nothing.
    const bad = structuredClone(decodeOk(readFileSync(ANDROID_FIXTURE, 'utf8')))
    bad.sets[0]!.exerciseId = 9999
    const stopBad = onDataReplaced(() => told++)
    await expect(restoreBackup(db, bad)).rejects.toThrow()
    stopBad()
    expect(told).toBe(1)
  })

  it('restore_neverChangesThisDevicesAppLock', async () => {
    const db = await openDb()
    const fixture = decodeOk(readFileSync(ANDROID_FIXTURE, 'utf8'))
    // Locked here, and the backup comes from a phone without a lock: still locked.
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    await restoreBackup(db, { ...fixture, settings: { ...fixture.settings, appLock: false } })
    expect((await getSettings(db)).appLock).toBe(true)
    // Not locked here, and the backup's phone was: this one has no passkey, and stays open.
    await updateSettings(db, (s) => ({ ...s, appLock: false }))
    await restoreBackup(db, { ...fixture, settings: { ...fixture.settings, appLock: true } })
    expect((await getSettings(db)).appLock).toBe(false)
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
    expect(decodeBackup(`{"version":${BACKUP_VERSION + 1}}`)).toEqual({ ok: false, kind: 'TOO_NEW', version: BACKUP_VERSION + 1 })
    corrupt(mut((o) => (o.exercises[0]!.category = 'NOPE')))
    corrupt(mut((o) => (o.exercises[3]!.parentLift = 'ROW')))
    corrupt(mut((o) => (o.sets[1]!.missPoint = 'TOP')))
    corrupt(mut((o) => (o.advice[0]!.type = 'X')))
    corrupt(mut((o) => (o.advice[0]!.status = 'DONE')))
    corrupt(mut((o) => (o.settings.restAlert = 'LOUD')))
    corrupt(mut((o) => (o.planItems[0]!.weekday = 8)))
    corrupt(mut((o) => (o.settings.trainingDays = [0])))
    corrupt(mut((o) => (o.extra = 1)))
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
    expect(copy.version).toBe(BACKUP_VERSION)
  })

  it('webFixture_isUpToDate', async () => {
    const db = await openDb()
    await buildSampleDb(db, { strictPress: true, bodybuilding: true, checkIns: true })
    const text = encodeBackup(await exportBackup(db, EXPORTED_AT))
    if (process.env.UPDATE_FIXTURES === '1') {
      writeFileSync(WEB_FIXTURE, JSON.stringify(JSON.parse(text), null, 4) + '\n')
    }
    expect(existsSync(WEB_FIXTURE), `Missing ${WEB_FIXTURE}; run with UPDATE_FIXTURES=1`).toBe(true)
    expect(withoutExportedAt(readJson(WEB_FIXTURE))).toEqual(withoutExportedAt(JSON.parse(text)))
    const file = decodeOk(readFileSync(WEB_FIXTURE, 'utf8'))
    expect(file.settings.restAlert).toBe('VIBRATE')
    expect(file.settings.webRestAlert).toBe('SILENT')
    expect(file.version).toBe(4)
    expect(file.checkIns).toHaveLength(2)
    expect(file.settings.showStrictPress).toBe(true)
    expect(file.exercises.filter((e) => e.category === 'STRICT_PRESS').map((e) => e.name)).toEqual(['Strict press'])
  })

  /** The previous web app's backup restores, and exports again as version 2 with the strict press switch off. */
  it('webV1Fixture_restoresOnWeb', async () => {
    const db = await openDb()
    await restoreBackup(db, decodeOk(readFileSync(WEB_V1_FIXTURE, 'utf8')))
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>
    const expected = asUpgradedV1(readJson(WEB_V1_FIXTURE))
    // App lock is this device's own (see androidFixture_restoresExactly).
    ;(expected.settings as Record<string, unknown>).appLock = false
    expect(withoutExportedAt(out)).toEqual(expected)
  })

  /** Android's version-2 backup restores exactly: its strict press row, its sets and the switch. */
  it('androidV2Fixture_restoresOnWeb', async () => {
    const db = await openDb()
    await ensureSeeded(db)
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_V2_FIXTURE, 'utf8')))
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>

    expect(readJson(ANDROID_V2_FIXTURE).version).toBe(2)
    // It exports again as the current version, everything on the powerlifting side.
    const expected = withoutExportedAt(withV3Defaults(readJson(ANDROID_V2_FIXTURE)))
    const settings = expected.settings as Record<string, unknown>
    expect(settings.showStrictPress).toBe(true)
    // The same documented differences as for version 1: webRestAlert is filled, App lock stays this device's.
    expect(settings.webRestAlert).toBeNull()
    settings.webRestAlert = 'SOUND'
    expect(settings.appLock).toBe(true)
    settings.appLock = false
    expect(withoutExportedAt(out)).toEqual(expected)

    expect((await getSettings(db)).showStrictPress).toBe(true)
    const press = (await db.exercises.toArray()).filter((e) => e.category === 'STRICT_PRESS')
    expect(press.map((e) => [e.name, e.isBuiltIn])).toEqual([['Strict press', true]])
    expect((await db.sets.toArray()).some((s) => s.exerciseId === press[0]!.id && s.missPoint === 'FLOOR')).toBe(true)
  })

  /** Strict press spec §4.3: a version-1 file's "Overhead press" row is converted on restore by the §4.1 rule. */
  it('v1Restore_convertsOverheadPress', async () => {
    const db = await openDb()
    await buildSampleDb(db)
    const add = (name: string) =>
      db.exercises.add({ name, category: 'ACCESSORY', parentLift: null, muscles: ['Delts'], isBuiltIn: false, archived: false }) as Promise<number>
    const ohp = await add('Overhead press')
    const mil = await add('Military press')
    const archived = (await db.exercises.add({ name: 'OHP', category: 'ACCESSORY', parentLift: null, muscles: ['Delts'], isBuiltIn: false, archived: true })) as number
    const session = (await db.sessions.toCollection().first())!.id!
    await db.sets.bulkAdd([
      { sessionId: session, exerciseId: ohp, weightKg: 60, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: true },
      { sessionId: session, exerciseId: mil, weightKg: 55, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: false },
    ])
    const before = await exportBackup(db, EXPORTED_AT)
    const v1 = asVersion1(encodeBackup(before))

    const decoded = decodeOk(v1)
    expect(decoded.version).toBe(1)
    expect(decoded.settings.showStrictPress).toBe(false)
    await updateSettings(db, (s) => ({ ...s, showStrictPress: true }))
    await restoreBackup(db, decoded)

    const after = await exportBackup(db, EXPORTED_AT)
    expect(after.version).toBe(BACKUP_VERSION)
    expect(after.exercises.find((e) => e.id === ohp)).toEqual({ id: ohp, name: 'Strict press', category: 'STRICT_PRESS', parentLift: null, muscles: ['Delts'], isBuiltIn: true, archived: false })
    expect(after.exercises.find((e) => e.id === mil)).toEqual({ id: mil, name: 'Military press', category: 'STRICT_PRESS', parentLift: null, muscles: ['Delts'], isBuiltIn: false, archived: false })
    // Nothing else changes: the other rows (the archived one included), every set and the settings (a v1 file means off).
    const others = (f: BackupFile) => f.exercises.filter((e) => e.id !== ohp && e.id !== mil)
    expect(others(after)).toEqual(others(before))
    expect(after.exercises.find((e) => e.id === archived)?.category).toBe('ACCESSORY')
    expect(after.sets).toEqual(before.sets)
    expect(after.settings).toEqual({ ...before.settings, showStrictPress: false })
    expect((await getSettings(db)).showStrictPress).toBe(false)
  })

  it('v2RoundTrip_keepsShowStrictPress', async () => {
    const db = await openDb()
    await buildSampleDb(db, { strictPress: true })
    // A version-2 file is already upgraded: a row named like the strict press is left as it is.
    await db.exercises.add({ name: 'OHP', category: 'ACCESSORY', parentLift: null, muscles: ['Delts'], isBuiltIn: false, archived: false })
    const exported = await exportBackup(db, EXPORTED_AT)
    const text = encodeBackup(exported)
    const json = JSON.parse(text) as { version: number; settings: Record<string, unknown> }
    expect(json.version).toBe(BACKUP_VERSION)
    expect(json.settings.showStrictPress).toBe(true)
    await updateSettings(db, (s) => ({ ...s, showStrictPress: false }))

    await restoreBackup(db, decodeOk(text))

    expect((await getSettings(db)).showStrictPress).toBe(true)
    expect(await exportBackup(db, EXPORTED_AT)).toEqual(exported)
  })

  /** An app that only reads version 1 turns a version-2 file away with the existing "too new" result. */
  it('v2File_onVersion1Decoder_isTooNew', async () => {
    const db = await openDb()
    await buildSampleDb(db, { strictPress: true })
    const text = encodeBackup(await exportBackup(db, EXPORTED_AT))

    expect(decodeBackup(text, 1)).toEqual({ ok: false, kind: 'TOO_NEW', version: BACKUP_VERSION })
    expect(decodeBackup(text).ok).toBe(true)
  })

  it('decode_showStrictPress_isOptionalButTyped', () => {
    const o = readJson(WEB_FIXTURE) as { settings: Record<string, unknown> }
    delete o.settings.showStrictPress
    expect(decodeOk(JSON.stringify(o)).settings.showStrictPress).toBe(false)
    o.settings.showStrictPress = 'yes'
    const r = decodeBackup(JSON.stringify(o))
    expect(r.ok === false && r.kind).toBe('CORRUPT')
  })
})

/** Bodybuilding mode spec §6: backup version 3 carries the training side. */
describe('backup version 3', () => {
  it('v3RoundTrip_keepsBothSides', async () => {
    const db = await openDb()
    await buildSampleDb(db, { strictPress: true, bodybuilding: true })
    const exported = await exportBackup(db, EXPORTED_AT)
    const text = encodeBackup(exported)
    const json = JSON.parse(text) as BackupFile

    expect(json.version).toBe(BACKUP_VERSION)
    expect(json.settings).toMatchObject({
      trainingMode: 'BODYBUILDING',
      trainingDays: [2, 4, 6],
      bodybuildingTrainingDays: [1, 3, 5, 6],
      powerliftingLeftOn: isoToEpochDay('2026-10-31'),
      bodybuildingLeftOn: isoToEpochDay('2026-10-24'),
    })
    expect(json.planItems.map((p) => [p.mode, p.reps, p.repsHigh])).toEqual([
      ['POWERLIFTING', 5, null],
      ['POWERLIFTING', 4, null],
      ['POWERLIFTING', 3, null],
      ['BODYBUILDING', 8, 12],
      ['BODYBUILDING', 10, 15],
    ])
    expect(json.sessions.map((s) => s.mode)).toEqual(['POWERLIFTING', 'POWERLIFTING', 'BODYBUILDING'])
    expect(json.advice.map((a) => a.mode)).toEqual(['POWERLIFTING', 'POWERLIFTING', 'POWERLIFTING', 'POWERLIFTING', 'POWERLIFTING', 'BODYBUILDING'])

    // Everything changed here comes back from the file.
    await setTrainingMode(db, 'POWERLIFTING', '2026-11-13')
    await updateSettings(db, (s) => ({ ...s, bodybuildingTrainingDays: [7], bodybuildingLeftOn: null }))
    await db.planItems.where('mode').equals('BODYBUILDING').delete()
    await restoreBackup(db, decodeOk(text))

    expect(await exportBackup(db, EXPORTED_AT)).toEqual(exported)
    const s = await getSettings(db)
    expect(s).toMatchObject({ trainingMode: 'BODYBUILDING', bodybuildingTrainingDays: [1, 3, 5, 6], powerliftingLeftOn: '2026-10-31', bodybuildingLeftOn: '2026-10-24' })
    expect((await db.planItems.where('mode').equals('BODYBUILDING').toArray()).map((p) => p.repsHigh)).toEqual([12, 15])
    expect((await db.sessions.toArray()).find((x) => x.date === '2026-10-31')?.mode).toBe('BODYBUILDING')
  })

  it('webV2Fixture_restoresAsAllPowerlifting', async () => {
    const db = await openDb()
    const v2 = readJson(WEB_V2_FIXTURE)
    expect(v2.version).toBe(2)
    await restoreBackup(db, decodeOk(readFileSync(WEB_V2_FIXTURE, 'utf8')))

    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>
    const expected = withoutExportedAt(withV3Defaults(v2))
    ;(expected.settings as Record<string, unknown>).appLock = false // this device's own
    expect(withoutExportedAt(out)).toEqual(expected)
    expect((await db.planItems.toArray()).every((p) => p.mode === 'POWERLIFTING' && p.repsHigh === undefined)).toBe(true)
    expect((await db.sessions.toArray()).every((p) => p.mode === 'POWERLIFTING')).toBe(true)
    expect((await db.advice.toArray()).every((p) => p.mode === 'POWERLIFTING')).toBe(true)
    expect((await getSettings(db)).trainingMode).toBe('POWERLIFTING')
  })

  it('v3File_onVersion2Decoder_isTooNew', async () => {
    const text = readFileSync(WEB_V3_FIXTURE, 'utf8')
    expect(decodeBackup(text, 2)).toEqual({ ok: false, kind: 'TOO_NEW', version: 3 })
  })

  it('decode_newKeys_areOptionalButTyped', () => {
    type Loose = Record<string, unknown> & { settings: Record<string, unknown>; sessions: Record<string, unknown>[]; planItems: Record<string, unknown>[]; advice: Record<string, unknown>[] }
    const valid = readJson(WEB_FIXTURE) as Loose
    const mut = (f: (o: Loose) => void) => {
      const o = structuredClone(valid)
      f(o)
      return JSON.stringify(o)
    }
    const corrupt = (text: string) => {
      const r = decodeBackup(text)
      expect(r.ok === false && r.kind, text.slice(0, 80)).toBe('CORRUPT')
    }
    corrupt(mut((o) => (o.settings.trainingMode = 'CROSSFIT')))
    corrupt(mut((o) => (o.settings.trainingMode = null)))
    corrupt(mut((o) => (o.settings.bodybuildingTrainingDays = [8])))
    corrupt(mut((o) => (o.settings.bodybuildingTrainingDays = null)))
    corrupt(mut((o) => (o.settings.powerliftingLeftOn = 'yesterday')))
    corrupt(mut((o) => (o.settings.bodybuildingLeftOn = 99_999_999)))
    corrupt(mut((o) => (o.sessions[0]!.mode = 'X')))
    corrupt(mut((o) => (o.planItems[0]!.mode = 'bodybuilding')))
    corrupt(mut((o) => (o.planItems[0]!.repsHigh = 'twelve')))
    corrupt(mut((o) => (o.planItems[0]!.repsHigh = 12.5)))
    corrupt(mut((o) => (o.advice[0]!.mode = 7)))

    // Absent keys mean what an older file means: the powerlifting side.
    const old = decodeOk(
      mut((o) => {
        for (const k of ['trainingMode', 'bodybuildingTrainingDays', 'powerliftingLeftOn', 'bodybuildingLeftOn']) delete o.settings[k]
        for (const r of [...o.sessions, ...o.planItems, ...o.advice]) {
          delete r.mode
          delete r.repsHigh
        }
      }),
    )
    expect(old.settings).toMatchObject({ trainingMode: 'POWERLIFTING', bodybuildingTrainingDays: [1, 3, 5], powerliftingLeftOn: null, bodybuildingLeftOn: null })
    expect(old.sessions.every((s) => s.mode === 'POWERLIFTING')).toBe(true)
    expect(old.planItems.every((p) => p.mode === 'POWERLIFTING' && p.repsHigh === null)).toBe(true)
    expect(old.advice.every((a) => a.mode === 'POWERLIFTING')).toBe(true)
  })
})

describe('cross-platform restore, version 3', () => {
  /** Android's version-3 backup (Task 4's fixture): both sides' plans, each session's side and the side settings come back. */
  it('androidV3Fixture_restoresOnWeb', async () => {
    const ANDROID_V3_FIXTURE = resolve(FIXTURES, 'android-v3.json')
    expect(readJson(ANDROID_V3_FIXTURE).version).toBe(3)
    const db = await openDb()
    await ensureSeeded(db)
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_V3_FIXTURE, 'utf8')))

    const names = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
    const side = async (mode: 'POWERLIFTING' | 'BODYBUILDING') =>
      (await planFor(db, mode)).map((p) => [p.weekday, names.get(p.exerciseId), p.sets, p.reps, p.repsHigh ?? null, p.targetRpe])
    expect(await side('POWERLIFTING')).toEqual([
      [2, 'Squat', 4, 5, null, 8],
      [2, 'Pause squat', 3, 4, null, 7.5],
      [6, 'Bench', 5, 3, null, 8.5],
    ])
    expect(await side('BODYBUILDING')).toEqual([
      [1, 'Curl', 3, 8, 12, 8],
      [1, 'Squat', 4, 10, 15, 8],
    ])

    const sessions = (await db.sessions.orderBy('date').toArray()).map((s) => [s.date, s.mode])
    expect(sessions).toEqual([
      [epochDayToIso(20753), 'POWERLIFTING'],
      [epochDayToIso(20755), 'POWERLIFTING'],
      [epochDayToIso(20759), 'BODYBUILDING'],
    ])
    expect((await db.advice.orderBy('id').toArray()).map((a) => [a.type, a.mode])).toEqual([
      ['TARGET', 'POWERLIFTING'],
      ['STALL', 'POWERLIFTING'],
      ['DELOAD', 'POWERLIFTING'],
      ['WEAK_POINT', 'POWERLIFTING'],
      ['RE_ENTRY', 'POWERLIFTING'],
      ['TARGET', 'BODYBUILDING'],
    ])

    const s = await getSettings(db)
    expect(s).toMatchObject({
      trainingMode: 'BODYBUILDING',
      trainingDays: [2, 4, 6],
      bodybuildingTrainingDays: [1, 3, 5, 6],
      powerliftingLeftOn: epochDayToIso(20757),
      bodybuildingLeftOn: epochDayToIso(20750),
      changePlates: true,
      defaultRpe: 7.5,
      showStrictPress: true,
    })
    expect(activeTrainingDays(s)).toEqual([1, 3, 5, 6])
  })
})

/** Bodybuilding mode stage 3: backup version 4 carries the check-ins and the two new advice types. */
describe('backup version 4', () => {
  it('v4RoundTrip_keepsCheckInsAndTheNewAdvice', async () => {
    const db = await openDb()
    await buildSampleDb(db, { strictPress: true, bodybuilding: true, checkIns: true })
    const exported = await exportBackup(db, EXPORTED_AT)
    const text = encodeBackup(exported)
    const json = JSON.parse(text) as BackupFile

    expect(json.version).toBe(4)
    expect(json.checkIns).toEqual([
      { id: 1, date: isoToEpochDay('2026-10-03'), lagging: [], growingWell: ['BICEPS'] },
      { id: 2, date: isoToEpochDay('2026-10-31'), lagging: ['CHEST', 'SIDE_DELTS'], growingWell: ['QUADS'] },
    ])
    expect(json.advice.slice(-2).map((a) => [a.type, a.status, a.mode, a.createdAt])).toEqual([
      ['MORE_WORK', 'OPEN', 'BODYBUILDING', isoToEpochDay('2026-10-30')],
      ['SWAP', 'DISMISSED', 'BODYBUILDING', isoToEpochDay('2026-10-30')],
    ])
    expect(json.advice.at(-2)!.payload).toContain('"newExercise":{"weekday":"MONDAY","name":"Pec deck"')

    // Everything changed here comes back from the file.
    await db.checkIns.clear()
    await db.checkIns.add({ date: '2026-11-12', lagging: ['CALVES'], growingWell: [] })
    await db.advice.where('type').anyOf('MORE_WORK', 'SWAP').delete()
    await restoreBackup(db, decodeOk(text))

    expect(await exportBackup(db, EXPORTED_AT)).toEqual(exported)
    expect((await db.checkIns.orderBy('date').toArray()).map((c) => [c.id, c.date, c.lagging, c.growingWell])).toEqual([
      [1, '2026-10-03', [], ['BICEPS']],
      [2, '2026-10-31', ['CHEST', 'SIDE_DELTS'], ['QUADS']],
    ])
    // A payload is kept exactly as written, and the domain still reads it.
    const swap = (await db.advice.where('type').equals('SWAP').first())!
    expect(swap.payloadJson).toBe(exported.advice.at(-1)!.payload)
    expect(grit.advicePayload(swap.payloadJson)?.alternatives).toEqual(['Cable curl', 'EZ-bar curl', 'Incline DB curl'])
  })

  it('webV3Fixture_restoresWithNoCheckIns', async () => {
    const db = await openDb()
    await db.checkIns.add({ date: '2026-11-01', lagging: ['CHEST'], growingWell: [] }) // must be wiped
    const v3 = readJson(WEB_V3_FIXTURE)
    expect(v3.version).toBe(3)
    expect('checkIns' in v3).toBe(false)
    await restoreBackup(db, decodeOk(readFileSync(WEB_V3_FIXTURE, 'utf8')))

    expect(await db.checkIns.count()).toBe(0)
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>
    const expected = withoutExportedAt(withV4Defaults(v3))
    ;(expected.settings as Record<string, unknown>).appLock = false // this device's own
    expect(withoutExportedAt(out)).toEqual(expected)
  })

  it('v4File_onVersion3Decoder_isTooNew_andVersion5IsTooNewHere', () => {
    const text = readFileSync(WEB_FIXTURE, 'utf8')
    expect(decodeBackup(text, 3)).toEqual({ ok: false, kind: 'TOO_NEW', version: 4 })
    const v5 = JSON.stringify({ ...readJson(WEB_FIXTURE), version: 5 })
    expect(decodeBackup(v5)).toEqual({ ok: false, kind: 'TOO_NEW', version: 5 })
    expect(BACKUP_VERSION).toBe(4)
  })

  it('decode_checkIns_areOptionalButTyped', () => {
    type Loose = Record<string, unknown> & { checkIns: Record<string, unknown>[]; advice: Record<string, unknown>[] }
    const valid = readJson(WEB_FIXTURE) as Loose
    const mut = (f: (o: Loose) => void) => {
      const o = structuredClone(valid)
      f(o)
      return JSON.stringify(o)
    }
    const corrupt = (text: string) => {
      const r = decodeBackup(text)
      expect(r.ok === false && r.kind, text.slice(0, 80)).toBe('CORRUPT')
    }
    corrupt(mut((o) => (o.checkIns = null as never)))
    corrupt(mut((o) => (o.checkIns[0]!.lagging = ['PECS'])))
    corrupt(mut((o) => (o.checkIns[0]!.growingWell = 'BICEPS')))
    corrupt(mut((o) => (o.checkIns[0]!.date = '2026-10-03')))
    corrupt(mut((o) => delete o.checkIns[0]!.growingWell))
    corrupt(mut((o) => (o.checkIns[0]!.extra = 1)))
    // One per date, ids unique, and a group never both lagging and growing well.
    corrupt(mut((o) => (o.checkIns[1]!.date = o.checkIns[0]!.date)))
    corrupt(mut((o) => (o.checkIns[1]!.id = o.checkIns[0]!.id)))
    corrupt(mut((o) => (o.checkIns[1]!.growingWell = ['CHEST'])))
    corrupt(mut((o) => (o.advice.at(-1)!.type = 'SWAPS')))

    // Absent means none, as in an older file.
    const old = decodeOk(mut((o) => delete (o as Partial<Loose>).checkIns))
    expect(old.checkIns).toEqual([])
    // Groups come back in the domain's order, as the app stores them.
    const reordered = decodeOk(mut((o) => (o.checkIns[1]!.lagging = ['SIDE_DELTS', 'CHEST'])))
    expect(reordered.checkIns[1]!.lagging).toEqual(['SIDE_DELTS', 'CHEST'])
  })

  it('restore_storesGroupsInTheDomainsOrder', async () => {
    const db = await openDb()
    const file = decodeOk(readFileSync(WEB_FIXTURE, 'utf8'))
    file.checkIns[1]!.lagging = ['SIDE_DELTS', 'CHEST']
    await restoreBackup(db, file)
    expect((await db.checkIns.get(2))!.lagging).toEqual(['CHEST', 'SIDE_DELTS'])
  })
})

describe('cross-platform restore, version 4', () => {
  /**
   * Android's version-4 backup: two check-ins, a MORE_WORK and a SWAP advice, both sides' plans and a few sessions.
   * Written by the Android app (stage 3 Task 2).
   */
  const androidV4Fixture_restoresOnWeb = async () => {
    const android = readJson(ANDROID_V4_FIXTURE) as unknown as BackupFile
    expect(android.version).toBe(4)
    const db = await openDb()
    await ensureSeeded(db)
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_V4_FIXTURE, 'utf8')))

    // Every check-in, every advice (the two new types among them) and both sides' plans come back.
    const checkIns = await db.checkIns.orderBy('id').toArray()
    expect(checkIns.map((c) => [c.id, isoToEpochDay(c.date), [...c.lagging].sort(), [...c.growingWell].sort()])).toEqual(
      android.checkIns.map((c) => [c.id, c.date, [...c.lagging].sort(), [...c.growingWell].sort()]),
    )
    expect(checkIns.length).toBeGreaterThanOrEqual(2)
    const advice = await db.advice.orderBy('id').toArray()
    expect(advice.map((a) => [a.id, a.type, a.status, a.mode, a.payloadJson])).toEqual(android.advice.map((a) => [a.id, a.type, a.status, a.mode, a.payload]))
    expect(advice.map((a) => a.type)).toEqual(expect.arrayContaining(['MORE_WORK', 'SWAP']))
    for (const a of advice.filter((x) => x.type === 'MORE_WORK' || x.type === 'SWAP')) expect(grit.advicePayload(a.payloadJson)?.kind).toBe(a.type === 'SWAP' ? 'Swap' : 'MoreWork')
    for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
      expect((await planFor(db, side)).length).toBe(android.planItems.filter((p) => (p.mode ?? 'POWERLIFTING') === side).length)
    }
    // And it exports again with the same check-ins.
    const out = await exportBackup(db, EXPORTED_AT)
    expect(out.checkIns.map((c) => c.id)).toEqual(android.checkIns.map((c) => c.id))
  }
  it('androidV4Fixture_restoresOnWeb', androidV4Fixture_restoresOnWeb)
})

describe('settings this app does not know (stage 2, owner option A)', () => {
  const withSettings = (extra: Record<string, unknown>) => {
    const o = readJson(ANDROID_V4_FIXTURE)
    return JSON.stringify({ ...o, settings: { ...(o.settings as Record<string, unknown>), ...extra } })
  }

  it('androidV4Fixture_withAnUnknownSetting_restores_asTheFixtureDoes', async () => {
    const text = withSettings({ vibrateOnTaps: false, somethingLater: { a: 1 } })
    const db = await openDb()
    await ensureSeeded(db)
    await restoreBackup(db, decodeOk(text))
    const fromNew = withoutExportedAt((await exportBackup(db, EXPORTED_AT)) as unknown as Record<string, unknown>)
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_V4_FIXTURE, 'utf8')))
    const fromFixture = withoutExportedAt((await exportBackup(db, EXPORTED_AT)) as unknown as Record<string, unknown>)
    expect(fromNew).toEqual(fromFixture)
    expect(decodeOk(text).settings).toEqual(decodeOk(readFileSync(ANDROID_V4_FIXTURE, 'utf8')).settings)
  })

  it('unknownKeys_outsideSettings_areStillCorrupt', () => {
    const o = readJson(ANDROID_V4_FIXTURE)
    const corrupt = (v: unknown) => expect(decodeBackup(JSON.stringify(v))).toMatchObject({ ok: false, kind: 'CORRUPT' })
    corrupt({ ...o, somethingLater: 1 })
    const sets = o.sets as Record<string, unknown>[]
    corrupt({ ...o, sets: [{ ...sets[0], extra: 1 }, ...sets.slice(1)] })
    corrupt({ ...o, settings: { ...(o.settings as Record<string, unknown>), restAlert: 'LOUD' } })
    corrupt({ ...o, settings: { ...(o.settings as Record<string, unknown>), showStrictPress: 'yes' } })
  })

  it('vibrateOnTaps_isNotInTheExport_andIsOnAfterARestore', async () => {
    const db = await openDb()
    await ensureSeeded(db)
    await updateSettings(db, (s) => ({ ...s, vibrateOnTaps: false }))
    const out = encodeBackup(await exportBackup(db, EXPORTED_AT))
    expect(out).not.toContain('vibrateOnTaps')
    await restoreBackup(db, decodeOk(out))
    expect((await getSettings(db)).vibrateOnTaps).toBe(true)
  })
})

describe('rest alert and vibration', () => {
  afterEach(() => {
    delete (navigator as unknown as Record<string, unknown>).vibrate
  })

  it('androidsVibratingAlerts_keepTheVibration_whereTheBrowserCanVibrate', () => {
    expect(webRestAlertOf('SOUND_AND_VIBRATE', null, true)).toBe('SOUND_AND_VIBRATE')
    // The web has no vibrate-only alert: the vibration is kept, with the sound.
    expect(webRestAlertOf('VIBRATE', null, true)).toBe('SOUND_AND_VIBRATE')
    expect(webRestAlertOf('OFF', null, true)).toBe('OFF')
  })

  it('androidsVibratingAlerts_becomeSound_whereItCannot', () => {
    expect(webRestAlertOf('SOUND_AND_VIBRATE', null, false)).toBe('SOUND')
    expect(webRestAlertOf('VIBRATE', null, false)).toBe('SOUND')
    expect(webRestAlertOf('OFF', null, false)).toBe('OFF')
  })

  it('theWebsSoundAndVibrate_isKeptWhereItCanVibrate_andIsSoundWhereItCannot', () => {
    expect(webRestAlertOf('SOUND_AND_VIBRATE', 'SOUND_AND_VIBRATE', true)).toBe('SOUND_AND_VIBRATE')
    expect(webRestAlertOf('SOUND_AND_VIBRATE', 'SOUND_AND_VIBRATE', false)).toBe('SOUND')
    // The other web values win as before, on either kind of phone.
    for (const vibrates of [true, false]) {
      expect(webRestAlertOf('VIBRATE', 'SILENT', vibrates)).toBe('SILENT')
      expect(webRestAlertOf('SOUND_AND_VIBRATE', 'SOUND', vibrates)).toBe('SOUND')
      expect(webRestAlertOf('OFF', 'OFF', vibrates)).toBe('OFF')
    }
  })

  it('soundAndVibrate_exportsAsAndroidsSoundAndVibrate', () => {
    expect(androidRestAlertOf('SOUND_AND_VIBRATE')).toBe('SOUND_AND_VIBRATE')
  })

  it('aBackupFromAnAndroidPhone_restoresWithVibration_onAnAndroidBrowser', async () => {
    Object.defineProperty(navigator, 'vibrate', { value: () => true, configurable: true })
    const db = await openDb()
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_V4_FIXTURE, 'utf8')))
    expect((await getSettings(db)).restAlert).toBe('SOUND_AND_VIBRATE')
  })

  it('aBackupFromAnAndroidPhone_restoresWithSound_onAnIPhone', async () => {
    const db = await openDb()
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_V4_FIXTURE, 'utf8')))
    expect((await getSettings(db)).restAlert).toBe('SOUND')
  })

  it('soundAndVibrate_roundTripsThroughABackup_andTheFormatStaysTheSame', async () => {
    Object.defineProperty(navigator, 'vibrate', { value: () => true, configurable: true })
    const db = await openDb()
    await updateSettings(db, (s) => ({ ...s, restAlert: 'SOUND_AND_VIBRATE' }))
    const file = await exportBackup(db, EXPORTED_AT)
    expect(file.version).toBe(BACKUP_VERSION)
    expect(file.settings.restAlert).toBe('SOUND_AND_VIBRATE')
    expect(file.settings.webRestAlert).toBe('SOUND_AND_VIBRATE')
    // It decodes as any other backup does.
    const decoded = decodeOk(encodeBackup(file))
    await updateSettings(db, (s) => ({ ...s, restAlert: 'OFF' }))
    await restoreBackup(db, decoded)
    expect((await getSettings(db)).restAlert).toBe('SOUND_AND_VIBRATE')

    // The same file on an iPhone: the sound alone.
    delete (navigator as unknown as Record<string, unknown>).vibrate
    await restoreBackup(db, decoded)
    expect((await getSettings(db)).restAlert).toBe('SOUND')
  })
})
