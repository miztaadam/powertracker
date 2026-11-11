import type Dexie from 'dexie'
import type { Table } from 'dexie'
import type {
  AdviceRow,
  BaselineRow,
  ExerciseRow,
  KeyValueRow,
  PlanItemRow,
  SessionRow,
  SetRow,
} from '../db/schema'
import { DEFAULT_SETTINGS, settingsFromRows, type AppSettings } from '../db/settings'
import {
  ADVICE_STATUSES,
  ADVICE_TYPES,
  ANDROID_REST_ALERTS,
  BACKUP_VERSION,
  CATEGORIES,
  MAIN_LIFTS,
  MISS_POINTS,
  WEB_REST_ALERTS,
  type AndroidRestAlert,
  type BackupFile,
  type WebRestAlert,
} from './format'

export type DecodeResult =
  | { ok: true; file: BackupFile }
  | { ok: false; kind: 'TOO_NEW'; version: number }
  | { ok: false; kind: 'CORRUPT'; message: string }

class InvalidBackup extends Error {}
const bad = (message: string): never => {
  throw new InvalidBackup(message)
}

export function backupFileName(date: string): string {
  return `grit-backup-${date}.json`
}

// ---- rest alert mapping (spec §5) -------------------------------------------------------------

export function androidRestAlertOf(web: WebRestAlert): AndroidRestAlert {
  return web === 'SOUND' ? 'SOUND_AND_VIBRATE' : web === 'SILENT' ? 'VIBRATE' : 'OFF'
}

/** `webRestAlert` wins when it is a known web value; otherwise Android's value maps (vibrate -> sound). */
export function webRestAlertOf(restAlert: string, webRestAlert: string | null): WebRestAlert {
  if ((WEB_REST_ALERTS as readonly string[]).includes(webRestAlert ?? '')) return webRestAlert as WebRestAlert
  return restAlert === 'OFF' ? 'OFF' : 'SOUND'
}

// ---- epochDay <-> ISO date ---------------------------------------------------------------------

const DAY_MS = 86_400_000
const MIN_EPOCH_DAY = -719_528 // 0000-01-01
const MAX_EPOCH_DAY = 2_932_896 // 9999-12-31

export function isoToEpochDay(iso: string): number {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso)
  if (!m) return bad(`Invalid date ${iso}`)
  const [y, mo, d] = [Number(m[1]), Number(m[2]), Number(m[3])]
  const t = new Date(0)
  t.setUTCFullYear(y, mo - 1, d) // Date.UTC would read years 0-99 as 1900-1999
  if (t.getUTCFullYear() !== y || t.getUTCMonth() !== mo - 1 || t.getUTCDate() !== d) return bad(`Invalid date ${iso}`)
  return t.getTime() / DAY_MS
}

export function epochDayToIso(day: number): string {
  if (!Number.isInteger(day) || day < MIN_EPOCH_DAY || day > MAX_EPOCH_DAY) return bad(`Invalid date ${day}`)
  return new Date(day * DAY_MS).toISOString().slice(0, 10)
}

// ---- export ------------------------------------------------------------------------------------

type Rows = {
  exercises: (ExerciseRow & { id: number })[]
  sessions: (SessionRow & { id: number })[]
  sets: (SetRow & { id: number })[]
  planItems: (PlanItemRow & { id: number })[]
  advice: (AdviceRow & { id: number })[]
  baselines: BaselineRow[]
}

const byId = <T extends { id: number }>(a: T, b: T) => a.id - b.id

/**
 * Reads every table in one transaction. Pure: `lastBackupAt` is not touched (see `markBackedUp`).
 * `keepAwake` and `lastBackupAt` are web-only settings and are not part of the backup.
 * Takes any Dexie so the safety copy can run against a database still on an older schema.
 */
export async function exportBackup(db: Dexie, nowMs: number): Promise<BackupFile> {
  const names = ['exercises', 'sessions', 'sets', 'planItems', 'advice', 'baselines', 'settings']
  return db.transaction('r', names, async () => {
    const all = <T>(name: string) => (db.table(name) as Table<T>).toArray()
    const settings = settingsFromRows(await all<KeyValueRow>('settings'))
    const exercises = (await all<ExerciseRow & { id: number }>('exercises')).sort(byId)
    const sessions = (await all<SessionRow & { id: number }>('sessions')).sort(byId)
    const sets = (await all<SetRow & { id: number }>('sets')).sort(byId)
    const planItems = (await all<PlanItemRow & { id: number }>('planItems')).sort(byId)
    const advice = (await all<AdviceRow & { id: number }>('advice')).sort(byId)
    const baselines = (await all<BaselineRow>('baselines')).sort((a, b) => a.exerciseId - b.exerciseId)
    return {
      version: BACKUP_VERSION,
      exportedAt: nowMs,
      settings: {
        changePlates: settings.changePlates,
        defaultRpe: settings.defaultRpe,
        trainingDays: [...settings.trainingDays].sort((a, b) => a - b),
        appLock: settings.appLock,
        restMainSec: settings.restMainSec,
        restAccessorySec: settings.restAccessorySec,
        restAlert: androidRestAlertOf(settings.restAlert),
        webRestAlert: settings.restAlert,
      },
      exercises: exercises.map((e) => ({
        id: e.id,
        name: e.name,
        category: e.category,
        parentLift: e.parentLift,
        muscles: e.muscles,
        isBuiltIn: e.isBuiltIn,
        archived: e.archived,
      })),
      sessions: sessions.map((s) => ({
        id: s.id,
        date: isoToEpochDay(s.date),
        bodyweightKg: s.bodyweightKg,
        readiness: s.readiness,
      })),
      sets: sets.map((s) => ({
        id: s.id,
        sessionId: s.sessionId,
        exerciseId: s.exerciseId,
        weightKg: s.weightKg,
        reps: s.reps,
        rpe: s.rpe,
        failed: s.failed,
        missPoint: s.missPoint,
        isTopSet: s.isTopSet,
      })),
      planItems: planItems.map((p) => ({
        id: p.id,
        weekday: p.weekday,
        exerciseId: p.exerciseId,
        sets: p.sets,
        reps: p.reps,
        targetRpe: p.targetRpe,
        sortOrder: p.order,
      })),
      advice: advice.map((a) => ({
        id: a.id,
        type: a.type,
        lift: a.lift,
        createdAt: isoToEpochDay(a.createdAt),
        reason: a.reason,
        severity: a.severity,
        payload: a.payloadJson,
        status: a.status,
      })),
      baselines: baselines.map((b) => ({ exerciseId: b.exerciseId, e1rmKg: b.e1rmKg, date: isoToEpochDay(b.date) })),
    }
  })
}

/** Records a user-initiated export. Separate from `exportBackup` so safety copies don't count. */
export async function markBackedUp(db: Dexie, nowMs: number): Promise<void> {
  await db.table('settings').put({ key: 'lastBackupAt', value: new Date(nowMs).toISOString() })
}

export function encodeBackup(file: BackupFile): string {
  return JSON.stringify(file)
}

// ---- decode (strict, like Android's ignoreUnknownKeys = false) -----------------------------------

type Obj = Record<string, unknown>

function obj(v: unknown, path: string, keys: readonly string[], optional: readonly string[] = []): Obj {
  if (typeof v !== 'object' || v === null || Array.isArray(v)) return bad(`${path}: expected object`)
  const o = v as Obj
  for (const k of Object.keys(o)) if (!keys.includes(k) && !optional.includes(k)) bad(`${path}: unknown key ${k}`)
  for (const k of keys) if (!(k in o)) bad(`${path}: missing ${k}`)
  return o
}
const bool = (o: Obj, k: string): boolean => (typeof o[k] === 'boolean' ? (o[k] as boolean) : bad(`${k}: expected boolean`))
const num = (o: Obj, k: string): number =>
  typeof o[k] === 'number' && Number.isFinite(o[k]) ? (o[k] as number) : bad(`${k}: expected number`)
const int = (o: Obj, k: string): number => (Number.isSafeInteger(o[k]) ? (o[k] as number) : bad(`${k}: expected integer`))
const str = (o: Obj, k: string): string => (typeof o[k] === 'string' ? (o[k] as string) : bad(`${k}: expected string`))
const nullable = <T>(o: Obj, k: string, f: (o: Obj, k: string) => T): T | null => (o[k] === null ? null : f(o, k))
const list = <T>(v: unknown, path: string, f: (item: unknown, path: string) => T): T[] =>
  Array.isArray(v) ? v.map((x, i) => f(x, `${path}[${i}]`)) : bad(`${path}: expected array`)

function parseBackup(root: Obj): BackupFile {
  const top = ['version', 'exportedAt', 'settings', 'exercises', 'sessions', 'sets', 'planItems', 'advice', 'baselines']
  const o = obj(root, 'backup', top)
  const s = obj(
    o.settings,
    'settings',
    ['changePlates', 'defaultRpe', 'trainingDays', 'appLock', 'restMainSec', 'restAccessorySec'],
    ['restAlert', 'webRestAlert'],
  )
  return {
    version: int(o, 'version'),
    exportedAt: int(o, 'exportedAt'),
    settings: {
      changePlates: bool(s, 'changePlates'),
      defaultRpe: num(s, 'defaultRpe'),
      trainingDays: list(s.trainingDays, 'trainingDays', (x) => (Number.isSafeInteger(x) ? (x as number) : bad('trainingDays: expected integer'))),
      appLock: bool(s, 'appLock'),
      restMainSec: int(s, 'restMainSec'),
      restAccessorySec: int(s, 'restAccessorySec'),
      restAlert: 'restAlert' in s ? str(s, 'restAlert') : 'VIBRATE',
      webRestAlert: 'webRestAlert' in s ? nullable(s, 'webRestAlert', str) : null,
    },
    exercises: list(o.exercises, 'exercises', (v, p) => {
      const e = obj(v, p, ['id', 'name', 'category', 'parentLift', 'muscles', 'isBuiltIn', 'archived'])
      return {
        id: int(e, 'id'),
        name: str(e, 'name'),
        category: str(e, 'category'),
        parentLift: nullable(e, 'parentLift', str),
        muscles: list(e.muscles, `${p}.muscles`, (m) => (typeof m === 'string' ? m : bad('muscles: expected string'))),
        isBuiltIn: bool(e, 'isBuiltIn'),
        archived: bool(e, 'archived'),
      }
    }),
    sessions: list(o.sessions, 'sessions', (v, p) => {
      const x = obj(v, p, ['id', 'date', 'bodyweightKg', 'readiness'])
      return { id: int(x, 'id'), date: int(x, 'date'), bodyweightKg: nullable(x, 'bodyweightKg', num), readiness: nullable(x, 'readiness', int) }
    }),
    sets: list(o.sets, 'sets', (v, p) => {
      const x = obj(v, p, ['id', 'sessionId', 'exerciseId', 'weightKg', 'reps', 'rpe', 'failed', 'missPoint', 'isTopSet'])
      return {
        id: int(x, 'id'),
        sessionId: int(x, 'sessionId'),
        exerciseId: int(x, 'exerciseId'),
        weightKg: num(x, 'weightKg'),
        reps: int(x, 'reps'),
        rpe: nullable(x, 'rpe', num),
        failed: bool(x, 'failed'),
        missPoint: nullable(x, 'missPoint', str),
        isTopSet: bool(x, 'isTopSet'),
      }
    }),
    planItems: list(o.planItems, 'planItems', (v, p) => {
      const x = obj(v, p, ['id', 'weekday', 'exerciseId', 'sets', 'reps', 'targetRpe', 'sortOrder'])
      return {
        id: int(x, 'id'),
        weekday: int(x, 'weekday'),
        exerciseId: int(x, 'exerciseId'),
        sets: int(x, 'sets'),
        reps: int(x, 'reps'),
        targetRpe: num(x, 'targetRpe'),
        sortOrder: int(x, 'sortOrder'),
      }
    }),
    advice: list(o.advice, 'advice', (v, p) => {
      const x = obj(v, p, ['id', 'type', 'lift', 'createdAt', 'reason', 'severity', 'payload', 'status'])
      return {
        id: int(x, 'id'),
        type: str(x, 'type'),
        lift: nullable(x, 'lift', str),
        createdAt: int(x, 'createdAt'),
        reason: str(x, 'reason'),
        severity: int(x, 'severity'),
        payload: str(x, 'payload'),
        status: str(x, 'status'),
      }
    }),
    baselines: list(o.baselines, 'baselines', (v, p) => {
      const x = obj(v, p, ['exerciseId', 'e1rmKg', 'date'])
      return { exerciseId: int(x, 'exerciseId'), e1rmKg: num(x, 'e1rmKg'), date: int(x, 'date') }
    }),
  }
}

// ---- validation + mapping to web rows ------------------------------------------------------------

type Mapped = { rows: Rows; settings: Omit<AppSettings, 'keepAwake' | 'lastBackupAt'> }

const oneOf = (v: string, allowed: readonly string[], what: string): string =>
  allowed.includes(v) ? v : bad(`Unknown ${what} ${v}`)
const weekday = (d: number) => (Number.isInteger(d) && d >= 1 && d <= 7 ? d : bad(`Invalid weekday ${d}`))

function unique(ids: (number | string)[], what: string) {
  if (new Set(ids).size !== ids.length) bad(`Duplicate ${what}`)
}

/** Validates every row (enums, weekdays, dates, references, uniqueness) and maps it. Throws before any write. */
function mapBackup(file: BackupFile): Mapped {
  const exercises = file.exercises.map((e) => ({
    id: e.id,
    name: e.name,
    category: oneOf(e.category, CATEGORIES, 'category'),
    parentLift: e.parentLift === null ? null : oneOf(e.parentLift, MAIN_LIFTS, 'lift'),
    muscles: [...e.muscles],
    isBuiltIn: e.isBuiltIn,
    archived: e.archived,
  }))
  unique(file.sessions.map((s) => s.id), 'session id')
  // Android's date index is not unique, so a real export can hold several sessions on one date.
  // Web keeps one per date: merge into the lowest id; the other sessions' sets move to it, and the
  // first non-null bodyweight / readiness (by id) wins.
  const sessions: { id: number; date: string; bodyweightKg: number | null; readiness: number | null }[] = []
  const mergedInto = new Map<number, number>()
  const byDate = new Map<string, (typeof sessions)[number]>()
  for (const s of [...file.sessions].sort((a, b) => a.id - b.id)) {
    const date = epochDayToIso(s.date)
    const keep = byDate.get(date)
    if (keep) {
      mergedInto.set(s.id, keep.id)
      keep.bodyweightKg ??= s.bodyweightKg
      keep.readiness ??= s.readiness
    } else {
      const row = { id: s.id, date, bodyweightKg: s.bodyweightKg, readiness: s.readiness }
      byDate.set(date, row)
      sessions.push(row)
    }
  }
  const sets = file.sets.map((s) => ({
    id: s.id,
    sessionId: mergedInto.get(s.sessionId) ?? s.sessionId,
    exerciseId: s.exerciseId,
    weightKg: s.weightKg,
    reps: s.reps,
    rpe: s.rpe,
    failed: s.failed,
    missPoint: s.missPoint === null ? null : oneOf(s.missPoint, MISS_POINTS, 'miss point'),
    isTopSet: s.isTopSet,
  }))
  const planItems = file.planItems.map((p) => ({
    id: p.id,
    weekday: weekday(p.weekday),
    exerciseId: p.exerciseId,
    sets: p.sets,
    reps: p.reps,
    targetRpe: p.targetRpe,
    order: p.sortOrder,
  }))
  const advice = file.advice.map((a) => ({
    id: a.id,
    type: oneOf(a.type, ADVICE_TYPES, 'advice type'),
    lift: a.lift === null ? null : oneOf(a.lift, MAIN_LIFTS, 'lift'),
    createdAt: epochDayToIso(a.createdAt),
    reason: a.reason,
    severity: a.severity,
    status: oneOf(a.status, ADVICE_STATUSES, 'advice status'),
    payloadJson: a.payload,
  }))
  const baselines = file.baselines.map((b) => ({ exerciseId: b.exerciseId, e1rmKg: b.e1rmKg, date: epochDayToIso(b.date) }))

  unique(exercises.map((e) => e.id), 'exercise id')
  unique(sets.map((s) => s.id), 'set id')
  unique(planItems.map((p) => p.id), 'plan item id')
  unique(advice.map((a) => a.id), 'advice id')
  unique(baselines.map((b) => b.exerciseId), 'baseline')
  const exerciseIds = new Set(exercises.map((e) => e.id))
  const sessionIds = new Set(sessions.map((s) => s.id))
  for (const s of sets) {
    if (!sessionIds.has(s.sessionId) || !exerciseIds.has(s.exerciseId)) bad('A set refers to a missing session or exercise')
  }
  for (const p of planItems) if (!exerciseIds.has(p.exerciseId)) bad('A plan item refers to a missing exercise')
  for (const b of baselines) if (!exerciseIds.has(b.exerciseId)) bad('A baseline refers to a missing exercise')

  const st = file.settings
  oneOf(st.restAlert, ANDROID_REST_ALERTS, 'rest alert')
  const trainingDays = st.trainingDays.map(weekday)
  return {
    rows: { exercises, sessions, sets, planItems, advice, baselines },
    settings: {
      changePlates: st.changePlates,
      defaultRpe: st.defaultRpe,
      trainingDays: [...new Set(trainingDays)].sort((a, b) => a - b),
      appLock: st.appLock,
      restMainSec: st.restMainSec,
      restAccessorySec: st.restAccessorySec,
      restAlert: webRestAlertOf(st.restAlert, st.webRestAlert),
    },
  }
}

export function decodeBackup(text: string): DecodeResult {
  const corrupt = (message: string): DecodeResult => ({ ok: false, kind: 'CORRUPT', message })
  let root: unknown
  try {
    root = JSON.parse(text)
  } catch (e) {
    return corrupt(e instanceof Error ? e.message : 'Unreadable backup')
  }
  if (typeof root !== 'object' || root === null || Array.isArray(root)) return corrupt('Not a backup file')
  const version = (root as Obj).version
  if (typeof version !== 'number' || !Number.isInteger(version) || version <= 0) {
    return corrupt('Missing or invalid version')
  }
  if (version > BACKUP_VERSION) return { ok: false, kind: 'TOO_NEW', version }
  try {
    const file = parseBackup(root as Obj)
    mapBackup(file)
    return { ok: true, file }
  } catch (e) {
    if (e instanceof InvalidBackup) return corrupt(e.message)
    throw e
  }
}

// ---- restore -------------------------------------------------------------------------------------

/**
 * Replaces all data. Everything is validated and mapped first, so bad content fails before a
 * single row is deleted; then one transaction clears, inserts and applies the settings.
 * `keepAwake` and `lastBackupAt` stay as they are.
 */
export async function restoreBackup(db: Dexie, file: BackupFile): Promise<void> {
  const { rows, settings } = mapBackup(file)
  const t = <T>(name: string) => db.table(name) as Table<T>
  const stores = ['exercises', 'sessions', 'sets', 'planItems', 'advice', 'baselines', 'settings']
  await db.transaction('rw', stores, async () => {
    for (const name of ['sets', 'advice', 'planItems', 'baselines', 'sessions', 'exercises']) await t(name).clear()
    await t<ExerciseRow>('exercises').bulkAdd(rows.exercises)
    await t<SessionRow>('sessions').bulkAdd(rows.sessions)
    await t<SetRow>('sets').bulkAdd(rows.sets)
    await t<PlanItemRow>('planItems').bulkAdd(rows.planItems)
    await t<AdviceRow>('advice').bulkAdd(rows.advice)
    await t<BaselineRow>('baselines').bulkAdd(rows.baselines)
    const kept: (keyof AppSettings)[] = ['keepAwake', 'lastBackupAt']
    const entries = (Object.keys(DEFAULT_SETTINGS) as (keyof AppSettings)[])
      .filter((k) => !kept.includes(k))
      .map((key) => ({ key, value: settings[key as keyof typeof settings] }))
    await t<KeyValueRow>('settings').bulkPut(entries)
  })
}
