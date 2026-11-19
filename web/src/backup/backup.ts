import type Dexie from 'dexie'
import type { Table } from 'dexie'
import type {
  AdviceRow,
  BaselineRow,
  CheckInRow,
  ExerciseRow,
  KeyValueRow,
  PlanItemRow,
  SessionRow,
  SetRow,
  SkipReason,
  SkipRow,
  TrainingMode,
} from '../db/schema'
import { sideOf } from '../db/schema'
import { DEFAULT_SETTINGS, settingsFromRows, type AppSettings } from '../db/settings'
import { applyStrictPressUpgrade } from '../db/strictPressUpgrade'
import { inGroupOrder } from '../db/checkIns'
import { grit } from '../domain'
import { canVibrate } from '../platform/device'
import {
  ADVICE_STATUSES,
  ADVICE_TYPES,
  ANDROID_REST_ALERTS,
  BACKUP_VERSION,
  CATEGORIES,
  MAIN_LIFTS,
  MISS_POINTS,
  SKIP_REASONS,
  TRAINING_MODES,
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
  return web === 'SOUND' || web === 'SOUND_AND_VIBRATE' ? 'SOUND_AND_VIBRATE' : web === 'SILENT' ? 'VIBRATE' : 'OFF'
}

/**
 * `webRestAlert` wins when it is a known web value; otherwise Android's value maps. Where this browser can
 * vibrate (Android), Android's vibrating alerts keep the vibration as Sound + vibrate (the web has no
 * vibrate-only alert); elsewhere they become Sound, the alert a phone that cannot vibrate can give.
 */
export function webRestAlertOf(restAlert: string, webRestAlert: string | null, vibrates: boolean = canVibrate()): WebRestAlert {
  const web: WebRestAlert = (WEB_REST_ALERTS as readonly string[]).includes(webRestAlert ?? '')
    ? (webRestAlert as WebRestAlert)
    : restAlert === 'OFF'
      ? 'OFF'
      : 'SOUND_AND_VIBRATE'
  return web === 'SOUND_AND_VIBRATE' && !vibrates ? 'SOUND' : web
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
  checkIns: (CheckInRow & { id: number })[]
  skips: (SkipRow & { id: number })[]
}

const byId = <T extends { id: number }>(a: T, b: T) => a.id - b.id

/**
 * Reads every table in one transaction. Pure: `lastBackupAt` is not touched (see `markBackedUp`).
 * `keepAwake` and `lastBackupAt` are web-only settings and are not part of the backup.
 * Takes any Dexie so the safety copy can run against a database still on an older schema.
 */
export async function exportBackup(db: Dexie, nowMs: number): Promise<BackupFile> {
  // A database still on a schema before check-ins (a safety copy before upgrading) has none to export.
  const hasCheckIns = db.tables.some((t) => t.name === 'checkIns')
  // Nor, before version 5, skips.
  const hasSkips = db.tables.some((t) => t.name === 'skips')
  const names = ['exercises', 'sessions', 'sets', 'planItems', 'advice', 'baselines', 'settings', ...(hasCheckIns ? ['checkIns'] : []), ...(hasSkips ? ['skips'] : [])]
  return db.transaction('r', names, async () => {
    const all = <T>(name: string) => (db.table(name) as Table<T>).toArray()
    const settings = settingsFromRows(await all<KeyValueRow>('settings'))
    const exercises = (await all<ExerciseRow & { id: number }>('exercises')).sort(byId)
    const sessions = (await all<SessionRow & { id: number }>('sessions')).sort(byId)
    const sets = (await all<SetRow & { id: number }>('sets')).sort(byId)
    const planItems = (await all<PlanItemRow & { id: number }>('planItems')).sort(byId)
    const advice = (await all<AdviceRow & { id: number }>('advice')).sort(byId)
    const baselines = (await all<BaselineRow>('baselines')).sort((a, b) => a.exerciseId - b.exerciseId)
    const checkIns = hasCheckIns ? (await all<CheckInRow & { id: number }>('checkIns')).sort(byId) : []
    const skips = hasSkips ? (await all<SkipRow & { id: number }>('skips')).sort(byId) : []
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
        showStrictPress: settings.showStrictPress,
        trainingMode: settings.trainingMode,
        bodybuildingTrainingDays: [...settings.bodybuildingTrainingDays].sort((a, b) => a - b),
        powerliftingLeftOn: settings.powerliftingLeftOn == null ? null : isoToEpochDay(settings.powerliftingLeftOn),
        bodybuildingLeftOn: settings.bodybuildingLeftOn == null ? null : isoToEpochDay(settings.bodybuildingLeftOn),
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
        mode: sideOf(s),
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
        mode: sideOf(p),
        repsHigh: p.repsHigh ?? null,
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
        mode: sideOf(a),
      })),
      baselines: baselines.map((b) => ({ exerciseId: b.exerciseId, e1rmKg: b.e1rmKg, date: isoToEpochDay(b.date) })),
      checkIns: checkIns.map((c) => ({ id: c.id, date: isoToEpochDay(c.date), lagging: [...c.lagging], growingWell: [...c.growingWell] })),
      // Android's SkipDto key order, every key written (it has no defaults).
      skips: skips.map((k) => ({ id: k.id, date: isoToEpochDay(k.date), mode: k.mode, exerciseId: k.exerciseId, planItemId: k.planItemId ?? null, reason: k.reason ?? null })),
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

// ---- decode (strict, like Android's ignoreUnknownKeys = false; since 1.7, except unknown keys inside `settings`) ----

type Obj = Record<string, unknown>

function obj(v: unknown, path: string, keys: readonly string[], optional: readonly string[] = [], unknownKeys: 'reject' | 'ignore' = 'reject'): Obj {
  if (typeof v !== 'object' || v === null || Array.isArray(v)) return bad(`${path}: expected object`)
  const o = v as Obj
  if (unknownKeys === 'reject') for (const k of Object.keys(o)) if (!keys.includes(k) && !optional.includes(k)) bad(`${path}: unknown key ${k}`)
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

/** A row's or the settings' training side (version 3); absent in older files, which means POWERLIFTING. */
const modeOf = (o: Obj, key = 'mode'): string => (key in o ? str(o, key) : 'POWERLIFTING')

function parseBackup(root: Obj): BackupFile {
  const top = ['version', 'exportedAt', 'settings', 'exercises', 'sessions', 'sets', 'planItems', 'advice', 'baselines']
  const o = obj(root, 'backup', top, ['checkIns', 'skips'])
  const s = obj(
    o.settings,
    'settings',
    ['changePlates', 'defaultRpe', 'trainingDays', 'appLock', 'restMainSec', 'restAccessorySec'],
    ['restAlert', 'webRestAlert', 'showStrictPress', 'trainingMode', 'bodybuildingTrainingDays', 'powerliftingLeftOn', 'bodybuildingLeftOn'],
    // A setting this app does not know is ignored, so a later app can add one without older apps turning its backups
    // away (1.6 rejected them). Known settings are still checked; unknown keys anywhere else are still corrupt.
    'ignore',
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
      showStrictPress: 'showStrictPress' in s ? bool(s, 'showStrictPress') : false,
      trainingMode: modeOf(s, 'trainingMode'),
      bodybuildingTrainingDays:
        'bodybuildingTrainingDays' in s
          ? list(s.bodybuildingTrainingDays, 'bodybuildingTrainingDays', (x) => (Number.isSafeInteger(x) ? (x as number) : bad('bodybuildingTrainingDays: expected integer')))
          : [...DEFAULT_SETTINGS.bodybuildingTrainingDays],
      powerliftingLeftOn: 'powerliftingLeftOn' in s ? nullable(s, 'powerliftingLeftOn', int) : null,
      bodybuildingLeftOn: 'bodybuildingLeftOn' in s ? nullable(s, 'bodybuildingLeftOn', int) : null,
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
      const x = obj(v, p, ['id', 'date', 'bodyweightKg', 'readiness'], ['mode'])
      return { id: int(x, 'id'), date: int(x, 'date'), bodyweightKg: nullable(x, 'bodyweightKg', num), readiness: nullable(x, 'readiness', int), mode: modeOf(x) }
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
      const x = obj(v, p, ['id', 'weekday', 'exerciseId', 'sets', 'reps', 'targetRpe', 'sortOrder'], ['mode', 'repsHigh'])
      return {
        id: int(x, 'id'),
        weekday: int(x, 'weekday'),
        exerciseId: int(x, 'exerciseId'),
        sets: int(x, 'sets'),
        reps: int(x, 'reps'),
        targetRpe: num(x, 'targetRpe'),
        sortOrder: int(x, 'sortOrder'),
        mode: modeOf(x),
        repsHigh: 'repsHigh' in x ? nullable(x, 'repsHigh', int) : null,
      }
    }),
    advice: list(o.advice, 'advice', (v, p) => {
      const x = obj(v, p, ['id', 'type', 'lift', 'createdAt', 'reason', 'severity', 'payload', 'status'], ['mode'])
      return {
        id: int(x, 'id'),
        type: str(x, 'type'),
        lift: nullable(x, 'lift', str),
        createdAt: int(x, 'createdAt'),
        reason: str(x, 'reason'),
        severity: int(x, 'severity'),
        payload: str(x, 'payload'),
        status: str(x, 'status'),
        mode: modeOf(x),
      }
    }),
    baselines: list(o.baselines, 'baselines', (v, p) => {
      const x = obj(v, p, ['exerciseId', 'e1rmKg', 'date'])
      return { exerciseId: int(x, 'exerciseId'), e1rmKg: num(x, 'e1rmKg'), date: int(x, 'date') }
    }),
    // Version 4. An older file has none.
    checkIns:
      'checkIns' in o
        ? list(o.checkIns, 'checkIns', (v, p) => {
            const x = obj(v, p, ['id', 'date', 'lagging', 'growingWell'])
            const groups = (k: string) => list(x[k], `${p}.${k}`, (g) => (typeof g === 'string' ? g : bad(`${k}: expected string`)))
            return { id: int(x, 'id'), date: int(x, 'date'), lagging: groups('lagging'), growingWell: groups('growingWell') }
          })
        : [],
    // Version 5. An older file has none. Every key is required, as Android's SkipDto has no defaults.
    skips:
      'skips' in o
        ? list(o.skips, 'skips', (v, p) => {
            const x = obj(v, p, ['id', 'date', 'mode', 'exerciseId', 'planItemId', 'reason'])
            return {
              id: int(x, 'id'),
              date: int(x, 'date'),
              mode: str(x, 'mode'),
              exerciseId: int(x, 'exerciseId'),
              planItemId: nullable(x, 'planItemId', int),
              reason: nullable(x, 'reason', str),
            }
          })
        : [],
  }
}

// ---- validation + mapping to web rows ------------------------------------------------------------

type Mapped = { rows: Rows; settings: Omit<AppSettings, 'keepAwake' | 'lastBackupAt'> }

const oneOf = (v: string, allowed: readonly string[], what: string): string =>
  allowed.includes(v) ? v : bad(`Unknown ${what} ${v}`)
const side = (v: string | undefined): TrainingMode => oneOf(v ?? 'POWERLIFTING', TRAINING_MODES, 'training mode') as TrainingMode
const weekday = (d: number) => (Number.isInteger(d) && d >= 1 && d <= 7 ? d : bad(`Invalid weekday ${d}`))

function unique(ids: (number | string)[], what: string) {
  if (new Set(ids).size !== ids.length) bad(`Duplicate ${what}`)
}

/**
 * Validates every row (enums, weekdays, dates, references, uniqueness) and maps it. Throws before any write.
 * A version-1 file gets the strict press upgrade on the way in, as the database got schema version 2.
 */
function mapBackup(file: BackupFile): Mapped {
  const mappedExercises = file.exercises.map((e) => ({
    id: e.id,
    name: e.name,
    category: oneOf(e.category, CATEGORIES, 'category'),
    parentLift: e.parentLift === null ? null : oneOf(e.parentLift, MAIN_LIFTS, 'lift'),
    muscles: [...e.muscles],
    isBuiltIn: e.isBuiltIn,
    archived: e.archived,
  }))
  const exercises = file.version < 2 ? applyStrictPressUpgrade(mappedExercises) : mappedExercises
  unique(file.sessions.map((s) => s.id), 'session id')
  // Android's date index is not unique, so a real export can hold several sessions on one date.
  // Web keeps one per date: merge into the lowest id; the other sessions' sets move to it, and the
  // first non-null bodyweight / readiness (by id) wins.
  const sessions: { id: number; date: string; bodyweightKg: number | null; readiness: number | null; mode: TrainingMode }[] = []
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
      const row = { id: s.id, date, bodyweightKg: s.bodyweightKg, readiness: s.readiness, mode: side(s.mode) }
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
    mode: side(p.mode),
    // Fixed reps store no repsHigh, as on every row the app writes.
    ...(p.repsHigh != null ? { repsHigh: p.repsHigh } : {}),
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
    mode: side(a.mode),
  }))
  const baselines = file.baselines.map((b) => ({ exerciseId: b.exerciseId, e1rmKg: b.e1rmKg, date: epochDayToIso(b.date) }))
  const groups = grit.muscleGroups()
  const checkIns = file.checkIns.map((c) => {
    for (const g of [...c.lagging, ...c.growingWell]) oneOf(g, groups, 'muscle group')
    unique([...c.lagging, ...c.growingWell], 'muscle group in a check-in')
    // Stored in the domain's order, as the app writes them.
    return { id: c.id, date: epochDayToIso(c.date), lagging: inGroupOrder(c.lagging), growingWell: inGroupOrder(c.growingWell) }
  })
  const skips = file.skips.map((k) => ({
    id: k.id,
    date: epochDayToIso(k.date),
    mode: side(k.mode),
    exerciseId: k.exerciseId,
    planItemId: k.planItemId,
    reason: k.reason === null ? null : (oneOf(k.reason, SKIP_REASONS, 'skip reason') as SkipReason),
  }))

  unique(exercises.map((e) => e.id), 'exercise id')
  unique(sets.map((s) => s.id), 'set id')
  unique(planItems.map((p) => p.id), 'plan item id')
  unique(advice.map((a) => a.id), 'advice id')
  unique(baselines.map((b) => b.exerciseId), 'baseline')
  unique(checkIns.map((c) => c.id), 'check-in id')
  unique(checkIns.map((c) => c.date), 'check-in date')
  unique(skips.map((k) => k.id), 'skip id')
  // One skip per exercise, date and side, as both apps keep them.
  unique(skips.map((k) => `${k.date}|${k.mode}|${k.exerciseId}`), 'skip of one exercise on one date and side')
  const exerciseIds = new Set(exercises.map((e) => e.id))
  const sessionIds = new Set(sessions.map((s) => s.id))
  for (const s of sets) {
    if (!sessionIds.has(s.sessionId) || !exerciseIds.has(s.exerciseId)) bad('A set refers to a missing session or exercise')
  }
  for (const p of planItems) if (!exerciseIds.has(p.exerciseId)) bad('A plan item refers to a missing exercise')
  for (const b of baselines) if (!exerciseIds.has(b.exerciseId)) bad('A baseline refers to a missing exercise')
  // Android's foreign keys: a skip's exercise exists, and so does its plan item unless it has none.
  const planItemIds = new Set(planItems.map((p) => p.id))
  for (const k of skips) {
    if (!exerciseIds.has(k.exerciseId)) bad('A skip refers to a missing exercise')
    if (k.planItemId !== null && !planItemIds.has(k.planItemId)) bad('A skip refers to a missing plan item')
  }

  const st = file.settings
  oneOf(st.restAlert, ANDROID_REST_ALERTS, 'rest alert')
  const trainingDays = st.trainingDays.map(weekday)
  const bodybuildingTrainingDays = (st.bodybuildingTrainingDays ?? DEFAULT_SETTINGS.bodybuildingTrainingDays).map(weekday)
  const leftOn = (day: number | null | undefined) => (day == null ? null : epochDayToIso(day))
  return {
    rows: { exercises, sessions, sets, planItems, advice, baselines, checkIns, skips },
    settings: {
      changePlates: st.changePlates,
      defaultRpe: st.defaultRpe,
      trainingDays: [...new Set(trainingDays)].sort((a, b) => a - b),
      appLock: st.appLock,
      restMainSec: st.restMainSec,
      restAccessorySec: st.restAccessorySec,
      restAlert: webRestAlertOf(st.restAlert, st.webRestAlert),
      showStrictPress: st.showStrictPress,
      trainingMode: side(st.trainingMode),
      bodybuildingTrainingDays: [...new Set(bodybuildingTrainingDays)].sort((a, b) => a - b),
      powerliftingLeftOn: leftOn(st.powerliftingLeftOn),
      bodybuildingLeftOn: leftOn(st.bodybuildingLeftOn),
      // Not in the backup (owner, option A): a restore puts it back on, its default, as on Android.
      vibrateOnTaps: DEFAULT_SETTINGS.vibrateOnTaps,
    },
  }
}

/** `supportedVersion` is the newest version this app reads; tests lower it to stand in for an older app. */
export function decodeBackup(text: string, supportedVersion: number = BACKUP_VERSION): DecodeResult {
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
  if (version > supportedVersion) return { ok: false, kind: 'TOO_NEW', version }
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
 * The sides whose plan restoring `file` would remove (1.14.1): those with plan items now and none in the backup,
 * powerlifting first. A backup item with no mode is on the powerlifting side, as the restore maps it.
 */
export async function plansLostBy(db: Dexie, file: BackupFile): Promise<TrainingMode[]> {
  const now = new Set(((await db.table('planItems').toArray()) as PlanItemRow[]).map(sideOf))
  const kept = new Set(file.planItems.map((p) => p.mode ?? 'POWERLIFTING'))
  return TRAINING_MODES.filter((mode) => now.has(mode) && !kept.has(mode))
}

const replacedListeners = new Set<() => void>()

/** Calls `cb` each time a restore has replaced the data, so what runs on the old data (a rest) can stop. Returns an unsubscribe. */
export function onDataReplaced(cb: () => void): () => void {
  replacedListeners.add(cb)
  return () => replacedListeners.delete(cb)
}

/** `meta` rows about the replaced data: the session being logged, unlogged numbers, the running rest. */
const aboutReplacedData = (key: string) => key === 'today:active' || key === 'restTimer' || key.startsWith('editor:')

/**
 * Replaces all data. Everything is validated and mapped first, so bad content fails before a
 * single row is deleted; then one transaction clears, inserts and applies the settings.
 *
 * What belongs to this device stays as it is, whatever the backup says: `keepAwake`, `lastBackupAt`
 * and `appLock` (its passkey exists only on this phone), and in `meta` the passkey id, the safety
 * copy, the dismissed install card and the snoozed backup reminder.
 */
export async function restoreBackup(db: Dexie, file: BackupFile): Promise<void> {
  const { rows, settings } = mapBackup(file)
  const t = <T>(name: string) => db.table(name) as Table<T>
  const stores = ['exercises', 'sessions', 'sets', 'planItems', 'advice', 'baselines', 'checkIns', 'skips', 'settings', 'meta']
  await db.transaction('rw', stores, async () => {
    for (const name of ['skips', 'sets', 'advice', 'planItems', 'baselines', 'checkIns', 'sessions', 'exercises']) await t(name).clear()
    await t<KeyValueRow>('meta')
      .filter((row) => aboutReplacedData(row.key))
      .delete()
    await t<ExerciseRow>('exercises').bulkAdd(rows.exercises)
    await t<SessionRow>('sessions').bulkAdd(rows.sessions)
    await t<SetRow>('sets').bulkAdd(rows.sets)
    await t<PlanItemRow>('planItems').bulkAdd(rows.planItems)
    await t<AdviceRow>('advice').bulkAdd(rows.advice)
    await t<BaselineRow>('baselines').bulkAdd(rows.baselines)
    await t<CheckInRow>('checkIns').bulkAdd(rows.checkIns)
    await t<SkipRow>('skips').bulkAdd(rows.skips)
    const kept: (keyof AppSettings)[] = ['keepAwake', 'lastBackupAt', 'appLock']
    const entries = (Object.keys(DEFAULT_SETTINGS) as (keyof AppSettings)[])
      .filter((k) => !kept.includes(k))
      .map((key) => ({ key, value: settings[key as keyof typeof settings] }))
    await t<KeyValueRow>('settings').bulkPut(entries)
  })
  replacedListeners.forEach((cb) => cb())
}
