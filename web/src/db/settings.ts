import type { GritDb } from './db'
import type { KeyValueRow } from './schema'

export type AppSettings = {
  changePlates: boolean
  defaultRpe: number
  trainingDays: number[]
  appLock: boolean
  restMainSec: number
  restAccessorySec: number
  restAlert: 'SOUND' | 'SILENT' | 'OFF'
  keepAwake: boolean
  lastBackupAt: string | null
  /** Strict press on Progress and in the total (strict press spec §4.2). Coaching ignores it. */
  showStrictPress: boolean
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
}

/** Stored values of the wrong type fall back to the default. `null` defaults accept string or null. */
function sameKind(value: unknown, def: unknown): boolean {
  if (Array.isArray(def)) return Array.isArray(value) && value.every((v) => typeof v === 'number')
  if (def === null) return value === null || typeof value === 'string'
  return typeof value === typeof def
}

/** Builds settings from stored rows; unknown keys and wrong-typed values fall back to defaults. */
export function settingsFromRows(rows: KeyValueRow[]): AppSettings {
  const merged: Record<string, unknown> = { ...DEFAULT_SETTINGS, trainingDays: [...DEFAULT_SETTINGS.trainingDays] }
  for (const r of rows) {
    if (r.key in DEFAULT_SETTINGS && sameKind(r.value, DEFAULT_SETTINGS[r.key as keyof AppSettings])) {
      merged[r.key] = r.value
    }
  }
  if (!['SOUND', 'SILENT', 'OFF'].includes(merged.restAlert as string)) merged.restAlert = DEFAULT_SETTINGS.restAlert
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
    const next = fn({ ...current, trainingDays: [...current.trainingDays] })
    const changed = (Object.keys(DEFAULT_SETTINGS) as (keyof AppSettings)[]).filter(
      (key) => JSON.stringify(next[key]) !== JSON.stringify(current[key]),
    )
    await db.settings.bulkPut(changed.map((key) => ({ key, value: next[key] })))
    return next
  })
}
