import type { GritDb } from './db'
import { TRAINING_MODES, type KeyValueRow, type TrainingMode } from './schema'

export type AppSettings = {
  changePlates: boolean
  defaultRpe: number
  trainingDays: number[]
  appLock: boolean
  restMainSec: number
  restAccessorySec: number
  /** `SOUND_AND_VIBRATE` is offered only where the browser can vibrate (Android); elsewhere it acts as `SOUND`. */
  restAlert: 'SOUND' | 'SOUND_AND_VIBRATE' | 'SILENT' | 'OFF'
  keepAwake: boolean
  lastBackupAt: string | null
  /** Strict press on Progress and in the total (strict press spec §4.2). Coaching ignores it. */
  showStrictPress: boolean
  /** The side the app is on (bodybuilding mode spec §6). Off is the powerlifting side. */
  trainingMode: TrainingMode
  /**
   * The bodybuilding side's training days; `trainingDays` stays the powerlifting side's. Copied from `trainingDays`
   * the first time the mode is turned on (see `setTrainingMode`).
   */
  bodybuildingTrainingDays: number[]
  /** ISO `YYYY-MM-DD`: the day each side was last left. Null until it is first left. */
  powerliftingLeftOn: string | null
  bodybuildingLeftOn: string | null
}

export const DEFAULT_SETTINGS: AppSettings = {
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
}

/** The two lists, copied, so a caller changing one never changes the defaults or the stored copy it was read from. */
const withOwnLists = (s: AppSettings): AppSettings => ({ ...s, trainingDays: [...s.trainingDays], bodybuildingTrainingDays: [...s.bodybuildingTrainingDays] })

/** Stored values of the wrong type fall back to the default. `null` defaults accept string or null. */
function sameKind(value: unknown, def: unknown): boolean {
  if (Array.isArray(def)) return Array.isArray(value) && value.every((v) => typeof v === 'number')
  if (def === null) return value === null || typeof value === 'string'
  return typeof value === typeof def
}

/** Builds settings from stored rows; unknown keys and wrong-typed values fall back to defaults. */
export function settingsFromRows(rows: KeyValueRow[]): AppSettings {
  const merged: Record<string, unknown> = withOwnLists(DEFAULT_SETTINGS)
  for (const r of rows) {
    if (r.key in DEFAULT_SETTINGS && sameKind(r.value, DEFAULT_SETTINGS[r.key as keyof AppSettings])) {
      merged[r.key] = r.value
    }
  }
  if (!['SOUND', 'SOUND_AND_VIBRATE', 'SILENT', 'OFF'].includes(merged.restAlert as string)) merged.restAlert = DEFAULT_SETTINGS.restAlert
  if (!(TRAINING_MODES as readonly string[]).includes(merged.trainingMode as string)) merged.trainingMode = DEFAULT_SETTINGS.trainingMode
  return merged as AppSettings
}

export async function getSettings(db: GritDb): Promise<AppSettings> {
  return settingsFromRows(await db.settings.toArray())
}

/** Applies `fn` to the current settings and stores every value that changed. */
export async function updateSettings(
  db: GritDb,
  fn: (current: AppSettings) => AppSettings,
): Promise<AppSettings> {
  return db.transaction('rw', db.settings, async () => {
    const current = await getSettings(db)
    const next = fn(withOwnLists(current))
    const changed = (Object.keys(DEFAULT_SETTINGS) as (keyof AppSettings)[]).filter(
      (key) => JSON.stringify(next[key]) !== JSON.stringify(current[key]),
    )
    await db.settings.bulkPut(changed.map((key) => ({ key, value: next[key] })))
    return next
  })
}

/** The training days of the side the app is on. */
export function activeTrainingDays(settings: AppSettings): number[] {
  return settings.trainingMode === 'BODYBUILDING' ? settings.bodybuildingTrainingDays : settings.trainingDays
}

/** `day` (1 = Monday) added to or taken from the active side's training days; the other side's are left as they are. */
export function withTrainingDayToggled(settings: AppSettings, day: number): AppSettings {
  const key = settings.trainingMode === 'BODYBUILDING' ? 'bodybuildingTrainingDays' : 'trainingDays'
  const days = settings[key]
  return { ...settings, [key]: days.includes(day) ? days.filter((d) => d !== day) : [...days, day].sort((a, b) => a - b) }
}

const LEFT_ON = { POWERLIFTING: 'powerliftingLeftOn', BODYBUILDING: 'bodybuildingLeftOn' } as const

/**
 * Moves the app to `mode`. The side being left is stamped with `today` (ISO `YYYY-MM-DD`). The first time the
 * bodybuilding side is entered (it has never been left, so it has never been on) its training days start as a copy
 * of the powerlifting side's. Switching to the side already on changes nothing.
 */
export function withTrainingMode(current: AppSettings, mode: TrainingMode, today: string): AppSettings {
  if (current.trainingMode === mode) return current
  const next: AppSettings = { ...current, trainingMode: mode, [LEFT_ON[current.trainingMode]]: today }
  if (mode === 'BODYBUILDING' && current.bodybuildingLeftOn == null) next.bodybuildingTrainingDays = [...current.trainingDays]
  return next
}

/** Stores `withTrainingMode`; resolves to the settings as stored. */
export function setTrainingMode(db: GritDb, mode: TrainingMode, today: string): Promise<AppSettings> {
  return updateSettings(db, (s) => withTrainingMode(s, mode, today))
}

/**
 * Forgets the day `mode`'s side was last left, as if it had never been left (the welcome back has been answered).
 * Call it only while that side is on: leaving it stamps the day again. Cleared while the powerlifting side is on,
 * the bodybuilding side would count as never entered and copy the training days again on the next switch.
 */
export function clearLeftOn(db: GritDb, mode: TrainingMode): Promise<AppSettings> {
  return updateSettings(db, (s) => ({ ...s, [LEFT_ON[mode]]: null }))
}
