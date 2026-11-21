import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { planFor, resolveCatalogue, setPlateCount } from '../db/catalogue'
import { activeTrainingDays, ensureSeeded, getSettings, loadHistory, openGritDb, setTrainingMode, updateSettings, type GritDb } from '../db/db'
import { exerciseToJs } from '../db/mapping'
import { plateCountOf, setUnit, type ExerciseRow } from '../db/schema'
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
/** What the check-in apps wrote (version 4): frozen since version 5, and must keep restoring. */
const WEB_V4_FIXTURE = resolve(FIXTURES, 'web-v4.json')
/** What the skips apps wrote (version 5): frozen since version 6, and must keep restoring. */
const WEB_V5_FIXTURE = resolve(FIXTURES, 'web-v5.json')
/** What this app writes now (version 6, plates). Regenerate with UPDATE_FIXTURES=1. */
const WEB_FIXTURE = resolve(FIXTURES, 'web-v6.json')
/** What the Android app wrote in 1.12 (version 4, stage 3 Task 2): frozen. */
const ANDROID_V4_FIXTURE = resolve(FIXTURES, 'android-v4.json')
/** What the Android app writes now (version 5, 1.13 Task 2). */
const ANDROID_V5_FIXTURE = resolve(FIXTURES, 'android-v5.json')
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
async function buildSampleDb(db: GritDb, { strictPress = false, bodybuilding = false, checkIns = false, skips = false, plates = false } = {}) {
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
  if (skips) {
    // Version 5: three skips on both sides (pain, no reason, busy with its plan item since deleted) and the pain
    // skip's OPEN card, its payload exactly as the domain writes it.
    const curl = await resolveCatalogue(db, 'Curl')
    const curlItem = (await db.planItems.where('exerciseId').equals(curl).first())?.id ?? null
    const squatItem = (await db.planItems.where('exerciseId').equals(squat).first())!.id as number
    const pain = (await db.skips.add({ date: '2026-10-30', mode: 'BODYBUILDING', exerciseId: curl, planItemId: curlItem, reason: 'PAIN' })) as number
    await db.skips.add({ date: '2026-10-30', mode: 'POWERLIFTING', exerciseId: squat, planItemId: squatItem, reason: null })
    await db.skips.add({ date: '2026-10-23', mode: 'POWERLIFTING', exerciseId: bench, planItemId: null, reason: 'BUSY' })
    await db.advice.add({
      ...adv('SKIP', null, "You skipped Curl because of pain. Try one of these instead. Its weight won't go up until you do Curl again. If the pain persists or gets worse, see a physician.", 3,
        `{"type":"${P}Skipped","skipId":${pain},"exerciseId":${curl},"name":"Curl","reason":"PAIN","alternatives":["Cable curl"]}`, 'OPEN'),
      mode: 'BODYBUILDING',
    })
  }
  if (plates) {
    // Version 6: a WHOLE and a HALF exercise, plate sets with a half among them, one session holding a kg and a plate
    // set of the same exercise (60 kg, then plates turned on), and a PLATES target, its payload as the domain writes it.
    const lat = await resolveCatalogue(db, 'Lat pulldown')
    const press = await resolveCatalogue(db, 'Leg press')
    await setPlateCount(db, lat, null, 'WHOLE')
    await setPlateCount(db, press, null, 'HALF')
    const plate = (sessionId: number, exerciseId: number, weightKg: number, reps: number, rpe: number | null, failed: boolean, isTopSet: boolean) =>
      db.sets.add({ sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint: null, isTopSet, unit: 'PLATES' })
    const s4 = (await db.sessions.add({ date: '2026-11-03', bodyweightKg: null, readiness: 3, mode: 'POWERLIFTING' })) as number
    await set(s4, lat, 60, 10, 8, false, null, false)
    await plate(s4, lat, 7, 10, 8, false, true)
    await plate(s4, lat, 8, 6, 10, true, false)
    const s5 = (await db.sessions.add({ date: '2026-11-05', bodyweightKg: 83.5, readiness: null, mode: 'BODYBUILDING' })) as number
    await plate(s5, press, 7.5, 12, 8, false, false)
    await plate(s5, press, 7.5, 11, 8.5, false, false)
    await plate(s5, press, 8, 9, 9, false, false)
    await db.advice.add({
      ...adv('TARGET', null, 'Last time: plate 7½ × 12, plate 7½ × 11, plate 8 × 9. Beat it. Coach suggests plate 8 × 8–12 @ 8.', 1,
        `{"type":"${P}Target","exerciseId":${press},"weightKg":8.0,"reps":8,"rpe":8.0,"sets":3,"repsHigh":12,"unit":"PLATES"}`, 'OPEN'),
      createdAt: '2026-11-05',
      mode: 'BODYBUILDING',
    })
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
    // Version 5: no skips.
    skips: [],
  }
}

/** A version 3 file as it exports here once restored: the current version, with no check-ins and no skips. */
function withV4Defaults(file: Record<string, unknown>): Record<string, unknown> {
  return { ...file, version: BACKUP_VERSION, checkIns: [], skips: [] }
}

/** A version 4 file as it exports here once restored: the current version, with no skips. */
function withV5Defaults(file: Record<string, unknown>): Record<string, unknown> {
  return { ...file, version: BACKUP_VERSION, skips: [] }
}

/** A version 5 file as it exports here once restored: the current version, every row as it was (no plate keys). */
function asUpgradedV5(file: Record<string, unknown>): Record<string, unknown> {
  expect(file.version).toBe(5)
  return { ...file, version: BACKUP_VERSION }
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
    await buildSampleDb(db, { strictPress: true, bodybuilding: true, checkIns: true, skips: true, plates: true })
    const text = encodeBackup(await exportBackup(db, EXPORTED_AT))
    if (process.env.UPDATE_FIXTURES === '1') {
      writeFileSync(WEB_FIXTURE, JSON.stringify(JSON.parse(text), null, 4) + '\n')
    }
    expect(existsSync(WEB_FIXTURE), `Missing ${WEB_FIXTURE}; run with UPDATE_FIXTURES=1`).toBe(true)
    expect(withoutExportedAt(readJson(WEB_FIXTURE))).toEqual(withoutExportedAt(JSON.parse(text)))
    const file = decodeOk(readFileSync(WEB_FIXTURE, 'utf8'))
    expect(file.settings.restAlert).toBe('VIBRATE')
    expect(file.settings.webRestAlert).toBe('SILENT')
    expect(file.version).toBe(6)
    expect(file.checkIns).toHaveLength(2)
    expect(file.skips).toHaveLength(3)
    expect(file.advice.filter((a) => a.type === 'SKIP')).toHaveLength(1)
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

    expect(json.version).toBe(BACKUP_VERSION)
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

  it('v4File_onVersion3Decoder_isTooNew', () => {
    const text = readFileSync(WEB_V4_FIXTURE, 'utf8')
    expect(decodeBackup(text, 3)).toEqual({ ok: false, kind: 'TOO_NEW', version: 4 })
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

/** 1.13: backup version 5 carries the skipped planned exercises and the SKIP advice, as Android's `SkipDto`. */
describe('backup version 5', () => {
  it('everyBackup_isVersion5_withOrWithoutSkips', async () => {
    const db = await openDb()
    expect(BACKUP_VERSION).toBe(6)
    const empty = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>
    expect(empty.version).toBe(6)
    expect(empty.skips).toEqual([])
    await buildSampleDb(db, { bodybuilding: true, skips: true })
    const full = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>
    expect(full.version).toBe(6)
    expect(full.skips).toHaveLength(3)
  })

  it('v5RoundTrip_keepsSkips', async () => {
    const db = await openDb()
    await buildSampleDb(db, { strictPress: true, bodybuilding: true, checkIns: true, skips: true })
    const exported = await exportBackup(db, EXPORTED_AT)
    const text = encodeBackup(exported)
    const json = JSON.parse(text) as BackupFile
    const byName = new Map((await db.exercises.toArray()).map((e) => [e.name, e.id as number]))
    const curlItem = (await db.planItems.where('exerciseId').equals(byName.get('Curl')!).first())!.id
    const squatItem = (await db.planItems.where('exerciseId').equals(byName.get('Squat')!).first())!.id
    // Android's SkipDto, key for key: dates are epochDays and both nulls are written out.
    expect(json.skips).toEqual([
      { id: 1, date: isoToEpochDay('2026-10-30'), mode: 'BODYBUILDING', exerciseId: byName.get('Curl'), planItemId: curlItem, reason: 'PAIN' },
      { id: 2, date: isoToEpochDay('2026-10-30'), mode: 'POWERLIFTING', exerciseId: byName.get('Squat'), planItemId: squatItem, reason: null },
      { id: 3, date: isoToEpochDay('2026-10-23'), mode: 'POWERLIFTING', exerciseId: byName.get('Bench'), planItemId: null, reason: 'BUSY' },
    ])
    expect(json.advice.at(-1)).toMatchObject({ type: 'SKIP', status: 'OPEN', mode: 'BODYBUILDING', lift: null })

    // Everything changed here comes back from the file.
    await db.skips.clear()
    await db.skips.add({ date: '2026-11-12', mode: 'BODYBUILDING', exerciseId: byName.get('Curl')!, planItemId: null, reason: 'TIME' })
    await db.advice.where('type').equals('SKIP').delete()
    await restoreBackup(db, decodeOk(text))

    expect(await exportBackup(db, EXPORTED_AT)).toEqual(exported)
    expect((await db.skips.orderBy('id').toArray()).map((s) => [s.id, s.date, s.mode, s.reason, s.planItemId])).toEqual([
      [1, '2026-10-30', 'BODYBUILDING', 'PAIN', curlItem],
      [2, '2026-10-30', 'POWERLIFTING', null, squatItem],
      [3, '2026-10-23', 'POWERLIFTING', 'BUSY', null],
    ])
    // The card still finds its skip, and the domain still reads it.
    const card = (await db.advice.where('type').equals('SKIP').first())!
    expect(grit.advicePayload(card.payloadJson)?.skipId).toBe(1)
  })

  it('webV4Fixture_restoresWithNoSkips', async () => {
    const db = await openDb()
    await ensureSeeded(db)
    await db.skips.add({ date: '2026-11-01', mode: 'POWERLIFTING', exerciseId: 1, planItemId: null, reason: null }) // must be wiped
    const v4 = readJson(WEB_V4_FIXTURE)
    expect(v4.version).toBe(4)
    expect('skips' in v4).toBe(false)
    await restoreBackup(db, decodeOk(readFileSync(WEB_V4_FIXTURE, 'utf8')))

    expect(await db.skips.count()).toBe(0)
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>
    const expected = withoutExportedAt(withV5Defaults(v4))
    ;(expected.settings as Record<string, unknown>).appLock = false // this device's own
    expect(withoutExportedAt(out)).toEqual(expected)
  })

  it('v5File_onVersion4Decoder_isTooNew_andVersion6IsTooNewHere', () => {
    const text = readFileSync(WEB_V5_FIXTURE, 'utf8')
    expect(decodeBackup(text, 4)).toEqual({ ok: false, kind: 'TOO_NEW', version: 5 })
    expect(decodeBackup(readFileSync(ANDROID_V5_FIXTURE, 'utf8'), 4)).toEqual({ ok: false, kind: 'TOO_NEW', version: 5 })
    const v6 = JSON.stringify({ ...readJson(WEB_FIXTURE), version: 7 })
    expect(decodeBackup(v6)).toEqual({ ok: false, kind: 'TOO_NEW', version: 7 })
  })

  it('decode_skips_areOptionalButTyped', () => {
    type Loose = Record<string, unknown> & { skips: Record<string, unknown>[]; advice: Record<string, unknown>[] }
    const valid = readJson(WEB_FIXTURE) as Loose
    const mut = (f: (o: Loose) => void) => {
      const o = structuredClone(valid)
      f(o)
      return JSON.stringify(o)
    }
    const corrupt = (text: string) => {
      const r = decodeBackup(text)
      expect(r.ok === false && r.kind, text.slice(0, 120)).toBe('CORRUPT')
    }
    expect(valid.skips.length).toBe(3)
    corrupt(mut((o) => (o.skips = null as never)))
    corrupt(mut((o) => (o.skips[0]!.reason = 'TIRED')))
    corrupt(mut((o) => (o.skips[0]!.mode = 'CROSSFIT')))
    corrupt(mut((o) => delete o.skips[0]!.mode)) // Android's SkipDto has no default for it
    corrupt(mut((o) => delete o.skips[0]!.reason)) // nor for the nullable ones
    corrupt(mut((o) => delete o.skips[2]!.planItemId))
    corrupt(mut((o) => (o.skips[0]!.date = '2026-10-30')))
    corrupt(mut((o) => (o.skips[0]!.exerciseId = 1.5)))
    corrupt(mut((o) => (o.skips[0]!.extra = 1)))
    // Ids unique, one per exercise, date and side, and what they point at exists (Android's foreign keys).
    corrupt(mut((o) => (o.skips[1]!.id = o.skips[0]!.id)))
    corrupt(mut((o) => (o.skips[1] = { ...o.skips[0]!, id: 99 })))
    corrupt(mut((o) => (o.skips[0]!.exerciseId = 9999)))
    corrupt(mut((o) => (o.skips[0]!.planItemId = 9999)))
    corrupt(mut((o) => (o.advice.at(-1)!.type = 'SKIPPED')))

    // Absent means none, as in an older file; another side or date is another skip.
    expect(decodeOk(mut((o) => delete (o as Partial<Loose>).skips)).skips).toEqual([])
    expect(decodeOk(mut((o) => (o.skips[1] = { ...o.skips[0]!, id: 99, mode: 'POWERLIFTING' }))).skips).toHaveLength(3)
  })

  it('restore_clearsTheSkips_whenAFileHasNone', async () => {
    const db = await openDb()
    await buildSampleDb(db, { bodybuilding: true, skips: true })
    const file = await exportBackup(db, EXPORTED_AT)
    await restoreBackup(db, { ...file, skips: [] })
    expect(await db.skips.count()).toBe(0)
  })

  it('safetyCopy_ofAVersion4Database_hasNoSkips', async () => {
    const { SCHEMA_VERSIONS } = await import('../db/schema')
    const v4 = await openGritDb(name, { schemaVersions: SCHEMA_VERSIONS.filter((v) => v.version <= 4) })
    await ensureSeeded(v4)
    v4.close()
    // Opening on the current schema writes the copy from the version 4 data first, through exportBackup.
    const db = await openDb()
    const copy = (await readSafetyCopy(db))!
    expect(copy.kind).toBe('backup')
    const file = decodeOk(copy.json)
    expect(file.version).toBe(BACKUP_VERSION)
    expect(file.skips).toEqual([])
  })
})

describe('cross-platform restore, version 5', () => {
  /** Android's version-5 backup (1.13 Task 2): three skips on both sides and a pain skip's OPEN card. */
  it('androidV5Fixture_restoresOnWeb', async () => {
    const android = readJson(ANDROID_V5_FIXTURE) as unknown as BackupFile
    expect(android.version).toBe(5)
    const db = await openDb()
    await ensureSeeded(db)
    await restoreBackup(db, decodeOk(readFileSync(ANDROID_V5_FIXTURE, 'utf8')))

    const stored = await db.skips.orderBy('id').toArray()
    expect(stored.map((s) => [s.id, isoToEpochDay(s.date), s.mode, s.exerciseId, s.planItemId, s.reason])).toEqual(
      android.skips.map((s) => [s.id, s.date, s.mode, s.exerciseId, s.planItemId, s.reason]),
    )
    expect(stored.map((s) => s.reason)).toEqual(expect.arrayContaining(['PAIN', null, 'BUSY']))
    const card = (await db.advice.where('type').equals('SKIP').toArray())[0]!
    const androidCard = android.advice.find((a) => a.type === 'SKIP')!
    expect([card.id, card.status, card.mode, card.payloadJson]).toEqual([androidCard.id, androidCard.status, androidCard.mode, androidCard.payload])
    const payload = grit.advicePayload(card.payloadJson)!
    expect([payload.kind, payload.skipId, payload.skipReason]).toEqual(['Skipped', stored[0]!.id, 'PAIN'])

    // And it exports again as Android wrote it: the same skips and card, under the same top-level keys.
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Record<string, unknown>
    expect(out.skips).toEqual(android.skips)
    expect((out.advice as unknown[]).find((a) => (a as { type: string }).type === 'SKIP')).toEqual(androidCard)
    expect(Object.keys(out).sort()).toEqual(Object.keys(android).sort())
  })

  /** What Android's `SkipDto` decodes strictly (no defaults, no unknown keys): every key, in its order, nulls written out. */
  it('aWebSkip_hasExactlyAndroidsKeys', async () => {
    const android = readJson(ANDROID_V5_FIXTURE) as { skips: Record<string, unknown>[] }
    const db = await openDb()
    await buildSampleDb(db, { bodybuilding: true, skips: true })
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as { skips: Record<string, unknown>[] }
    for (const s of out.skips) expect(Object.keys(s)).toEqual(Object.keys(android.skips[0]!))
    expect(Object.keys(android.skips[0]!)).toEqual(['id', 'date', 'mode', 'exerciseId', 'planItemId', 'reason'])
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

/** 1.17: backup version 6 carries a set's unit and an exercise's plate setting, each only when it is not the default. */
describe('backup version 6', () => {
  /** The keys every set and exercise had in version 5, in their order. */
  const V5_SET_KEYS = ['id', 'sessionId', 'exerciseId', 'weightKg', 'reps', 'rpe', 'failed', 'missPoint', 'isTopSet']
  const V5_EXERCISE_KEYS = ['id', 'name', 'category', 'parentLift', 'muscles', 'isBuiltIn', 'archived']
  type Json = Record<string, unknown>
  const FROZEN = [ANDROID_FIXTURE, ANDROID_V2_FIXTURE, resolve(FIXTURES, 'android-v3.json'), ANDROID_V4_FIXTURE, ANDROID_V5_FIXTURE, WEB_V1_FIXTURE, WEB_V2_FIXTURE, WEB_V3_FIXTURE, WEB_V4_FIXTURE, WEB_V5_FIXTURE]

  async function sampleWithPlates(db: GritDb) {
    await buildSampleDb(db, { strictPress: true, bodybuilding: true, checkIns: true, skips: true, plates: true })
  }

  it('version 6 round-trips unit and plateCount', async () => {
    const db = await openDb()
    await sampleWithPlates(db)
    const exported = await exportBackup(db, EXPORTED_AT)
    const text = encodeBackup(exported)
    const json = JSON.parse(text) as { sets: Json[]; exercises: Json[] }
    expect(json.exercises.filter((e) => 'plateCount' in e).map((e) => [e.name, e.plateCount])).toEqual([['Lat pulldown', 'WHOLE'], ['Leg press', 'HALF']])
    expect(json.sets.filter((s) => 'unit' in s).map((s) => [s.weightKg, s.unit])).toEqual([[7, 'PLATES'], [8, 'PLATES'], [7.5, 'PLATES'], [7.5, 'PLATES'], [8, 'PLATES']])

    // Everything changed here comes back from the file.
    const byName = new Map((await db.exercises.toArray()).map((e) => [e.name, e.id as number]))
    await setPlateCount(db, byName.get('Lat pulldown')!, null, 'OFF')
    await setPlateCount(db, byName.get('Leg press')!, null, 'WHOLE')
    await db.sets.filter((s) => s.unit === 'PLATES').delete()
    await restoreBackup(db, decodeOk(text))

    expect(await exportBackup(db, EXPORTED_AT)).toEqual(exported)
    const stored = await db.sets.toArray()
    expect(stored.filter((s) => s.unit === 'PLATES')).toHaveLength(5)
    // A restored kg set and off exercise are stored as the app writes them: no key, not even undefined.
    expect(stored.filter((s) => s.unit !== 'PLATES').every((s) => !('unit' in s))).toBe(true)
    const exercises = await db.exercises.toArray()
    expect(exercises.filter((e) => 'plateCount' in e).map((e) => [e.name, e.plateCount])).toEqual([['Lat pulldown', 'WHOLE'], ['Leg press', 'HALF']])
    // And the domain reads them in their units.
    const history = await loadHistory(db)
    expect(history.exercises.filter((e) => e.plateCount !== 'OFF').map((e) => e.plateCount)).toEqual(['WHOLE', 'HALF'])
    expect(history.sessions.flatMap((s) => s.sets).filter((s) => s.unit === 'PLATES')).toHaveLength(5)
  })

  it('a kg set and an off exercise export with no new key', async () => {
    const db = await openDb()
    await sampleWithPlates(db)
    const json = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as { sets: Json[]; exercises: Json[] }
    for (const s of json.sets) expect(Object.keys(s)).toEqual(s.unit === undefined ? V5_SET_KEYS : [...V5_SET_KEYS, 'unit'])
    for (const e of json.exercises) expect(Object.keys(e)).toEqual(e.plateCount === undefined ? V5_EXERCISE_KEYS : [...V5_EXERCISE_KEYS, 'plateCount'])
    const plate = json.sets.find((s) => s.unit !== undefined)!
    expect(JSON.stringify(plate).endsWith('"unit":"PLATES"}')).toBe(true)
    const half = json.exercises.find((e) => e.plateCount === 'HALF')!
    expect(JSON.stringify(half).endsWith('"plateCount":"HALF"}')).toBe(true)
    // A log with no plates exports exactly as version 5 did but for the version number.
    const plain = await openGritDb(`${name}-plain`)
    try {
      await buildSampleDb(plain, { strictPress: true, bodybuilding: true, checkIns: true, skips: true })
      const out = JSON.parse(encodeBackup(await exportBackup(plain, EXPORTED_AT))) as Json
      expect(withoutExportedAt(out)).toEqual(withoutExportedAt(asUpgradedV5(readJson(WEB_V5_FIXTURE))))
      expect(JSON.stringify(out)).not.toMatch(/"unit"|"plateCount"/)
    } finally {
      plain.close()
      await Dexie.delete(`${name}-plain`)
    }
  })

  it('versions 1 to 5 restore with every set kg and every exercise off', async () => {
    for (const path of FROZEN) {
      const db = await openGritDb(`${name}-frozen`)
      try {
        const fixture = readJson(path)
        expect(fixture.version, path).toBeLessThanOrEqual(5)
        expect(JSON.stringify(fixture), path).not.toMatch(/"unit"|"plateCount"/)
        await restoreBackup(db, decodeOk(readFileSync(path, 'utf8')))
        const sets = await db.sets.toArray()
        const exercises = await db.exercises.toArray()
        expect(sets.length, path).toBeGreaterThan(0)
        expect(sets.every((s) => !('unit' in s) && setUnit(s) === 'KG'), path).toBe(true)
        expect(exercises.every((e) => !('plateCount' in e) && plateCountOf(e) === 'OFF'), path).toBe(true)
        // And it exports again with no new key on any row.
        expect(encodeBackup(await exportBackup(db, EXPORTED_AT)), path).not.toMatch(/"unit"|"plateCount"/)
      } finally {
        db.close()
        await Dexie.delete(`${name}-frozen`)
      }
    }
  })

  /** The previous web app's backup (version 5, frozen since version 6) restores and exports as itself, raised to version 6. */
  it('webV5Fixture_restoresAsItWasWritten', async () => {
    const db = await openDb()
    const v5 = readJson(WEB_V5_FIXTURE)
    expect(v5.version).toBe(5)
    await restoreBackup(db, decodeOk(readFileSync(WEB_V5_FIXTURE, 'utf8')))
    const out = JSON.parse(encodeBackup(await exportBackup(db, EXPORTED_AT))) as Json
    const expected = withoutExportedAt(asUpgradedV5(v5))
    ;(expected.settings as Json).appLock = false // this device's own
    expect(withoutExportedAt(out)).toEqual(expected)
  })

  it('a version 6 file is too new for a version 5 reader', async () => {
    const db = await openDb()
    await sampleWithPlates(db)
    const text = encodeBackup(await exportBackup(db, EXPORTED_AT))
    expect(decodeBackup(text, 5)).toEqual({ ok: false, kind: 'TOO_NEW', version: 6 })
    expect(decodeBackup(readFileSync(WEB_FIXTURE, 'utf8'), 5)).toEqual({ ok: false, kind: 'TOO_NEW', version: 6 })
    expect(decodeBackup(text).ok).toBe(true)
  })

  it('decode fills in kg and off where a row has neither', () => {
    const file = decodeOk(readFileSync(WEB_FIXTURE, 'utf8'))
    expect(new Set(file.sets.map((s) => s.unit))).toEqual(new Set(['KG', 'PLATES']))
    expect(new Set(file.exercises.map((e) => e.plateCount))).toEqual(new Set(['OFF', 'WHOLE', 'HALF']))
    const old = decodeOk(readFileSync(WEB_V5_FIXTURE, 'utf8'))
    expect(old.sets.every((s) => s.unit === 'KG')).toBe(true)
    expect(old.exercises.every((e) => e.plateCount === 'OFF')).toBe(true)
  })

  type Loose = Json & { sets: Json[]; exercises: Json[] }
  const mutFixture = (f: (o: Loose) => void) => {
    const o = structuredClone(readJson(WEB_FIXTURE)) as Loose
    f(o)
    return JSON.stringify(o)
  }
  const plateSetIndex = (o: Loose) => o.sets.findIndex((s) => s.unit === 'PLATES')
  const kgSetIndex = (o: Loose) => o.sets.findIndex((s) => s.unit === undefined)
  const plateExerciseIndex = (o: Loose) => o.exercises.findIndex((e) => e.plateCount !== undefined)

  it('an unknown unit or plateCount name is corrupt', () => {
    const corrupt = (text: string) => expect(decodeBackup(text), text.slice(0, 80)).toMatchObject({ ok: false, kind: 'CORRUPT' })
    corrupt(mutFixture((o) => (o.sets[plateSetIndex(o)]!.unit = 'STONES')))
    corrupt(mutFixture((o) => (o.sets[plateSetIndex(o)]!.unit = 'plates')))
    corrupt(mutFixture((o) => (o.sets[kgSetIndex(o)]!.unit = 'LB')))
    corrupt(mutFixture((o) => (o.sets[plateSetIndex(o)]!.unit = null)))
    corrupt(mutFixture((o) => (o.sets[plateSetIndex(o)]!.unit = 1)))
    corrupt(mutFixture((o) => (o.exercises[plateExerciseIndex(o)]!.plateCount = 'QUARTER')))
    corrupt(mutFixture((o) => (o.exercises[0]!.plateCount = 'whole')))
    corrupt(mutFixture((o) => (o.exercises[plateExerciseIndex(o)]!.plateCount = null)))
    corrupt(mutFixture((o) => (o.exercises[plateExerciseIndex(o)]!.plateCount = true)))
    // The defaults written out are read as what they mean, as Android reads them.
    const spelled = decodeOk(mutFixture((o) => {
      o.sets[kgSetIndex(o)]!.unit = 'KG'
      o.exercises[0]!.plateCount = 'OFF'
    }))
    expect(spelled.sets.every((s) => s.unit === 'KG' || s.unit === 'PLATES')).toBe(true)
  })

  it('a plate set below plate 1 is corrupt', async () => {
    for (const weightKg of [0, 0.5, -1]) {
      expect(decodeBackup(mutFixture((o) => (o.sets[plateSetIndex(o)]!.weightKg = weightKg)))).toEqual({ ok: false, kind: 'CORRUPT', message: 'A plate set below plate 1' })
    }
    // A kg set at 0 kg is bodyweight, as before.
    expect(decodeBackup(mutFixture((o) => (o.sets[kgSetIndex(o)]!.weightKg = 0))).ok).toBe(true)
    expect(decodeBackup(mutFixture((o) => (o.sets[plateSetIndex(o)]!.weightKg = 1))).ok).toBe(true)

    // Restored from a file as given, it is refused with nothing changed.
    const db = await openDb()
    await buildSampleDb(db)
    const before = await exportBackup(db, EXPORTED_AT)
    const bad = decodeOk(readFileSync(WEB_FIXTURE, 'utf8'))
    bad.sets.find((s) => s.unit === 'PLATES')!.weightKg = 0
    await expect(restoreBackup(db, bad)).rejects.toThrow('A plate set below plate 1')
    expect(await exportBackup(db, EXPORTED_AT)).toEqual(before)
  })

  it('a plate set on a main lift, or a setting on an exercise that is never offered, still restores', async () => {
    const text = mutFixture((o) => {
      const squat = o.exercises.find((e) => e.name === 'Squat')!
      squat.plateCount = 'WHOLE'
      o.sets.find((s) => s.exerciseId === squat.id)!.unit = 'PLATES'
    })
    const db = await openDb()
    await restoreBackup(db, decodeOk(text))
    const squat = (await db.exercises.toArray()).find((e) => e.name === 'Squat')!
    expect(squat.plateCount).toBe('WHOLE')
    // The domain still never counts plates on it.
    expect(grit.loadUnitOf(exerciseToJs(squat as typeof squat & { id: number }))).toBe('KG')
  })

  it('a plate target\'s payload comes back unchanged', async () => {
    const db = await openDb()
    await sampleWithPlates(db)
    const stored = (await db.advice.toArray()).find((a) => a.payloadJson.includes('"unit":"PLATES"'))!
    expect(grit.advicePayload(stored.payloadJson)?.unit).toBe('PLATES')
    const text = encodeBackup(await exportBackup(db, EXPORTED_AT))
    expect((JSON.parse(text) as { advice: { payload: string }[] }).advice.map((a) => a.payload)).toContain(stored.payloadJson)
    await db.advice.clear()
    await restoreBackup(db, decodeOk(text))
    expect((await db.advice.get(stored.id!))!.payloadJson).toBe(stored.payloadJson)
  })

  it('webFixture_holdsPlates', () => {
    const v6 = readJson(WEB_FIXTURE) as Json & Record<'exercises' | 'sessions' | 'sets' | 'advice', Json[]>
    const v5 = readJson(WEB_V5_FIXTURE) as Json & Record<'exercises' | 'sessions' | 'sets' | 'advice', Json[]>
    expect(v6.version).toBe(6)
    // The plate rows are there: a WHOLE and a HALF exercise, plate sets with a half among them, a session holding a kg
    // and a plate set of one exercise, and a PLATES target.
    const plateExercises = v6.exercises.filter((e) => 'plateCount' in e)
    expect(plateExercises.map((e) => e.plateCount)).toEqual(['WHOLE', 'HALF'])
    for (const e of plateExercises) expect(grit.plateCountOffered(exerciseToJs(e as unknown as ExerciseRow & { id: number }))).toBe(true)
    const plateSets = v6.sets.filter((s) => 'unit' in s)
    expect(plateSets.every((s) => s.unit === 'PLATES')).toBe(true)
    expect(plateSets.map((s) => s.weightKg)).toContain(7.5)
    expect(plateSets.every((s) => plateExercises.some((e) => e.id === s.exerciseId))).toBe(true)
    const mixed = v6.sessions.filter((session) => {
      const sets = v6.sets.filter((s) => s.sessionId === session.id)
      return sets.some((s) => s.unit === 'PLATES') && sets.some((s) => !('unit' in s) && s.exerciseId === sets.find((p) => p.unit === 'PLATES')!.exerciseId)
    })
    expect(mixed).toHaveLength(1)
    const targets = v6.advice.filter((a) => a.type === 'TARGET' && grit.advicePayload(a.payload as string)?.unit === 'PLATES')
    expect(targets).toHaveLength(1)
    // Everything else is the version 5 file as it was, but for the version: no key on any other row.
    const only = (key: 'exercises' | 'sessions' | 'sets' | 'advice') => {
      const ids = new Set(v5[key].map((r) => r.id))
      return v6[key].filter((r) => ids.has(r.id))
    }
    const kept = { ...v6, exercises: only('exercises'), sessions: only('sessions'), sets: only('sets'), advice: only('advice') }
    expect(withoutExportedAt(kept)).toEqual(withoutExportedAt(asUpgradedV5(v5)))
  })
})
