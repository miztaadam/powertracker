import type { GritDb } from './db'

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
}

export async function getSettings(db: GritDb): Promise<AppSettings> {
  const rows = await db.settings.toArray()
  const merged: Record<string, unknown> = { ...DEFAULT_SETTINGS }
  for (const r of rows) if (r.key in DEFAULT_SETTINGS) merged[r.key] = r.value
  return merged as AppSettings
}

/** Applies `fn` to the current settings and stores every value that changed. */
export async function updateSettings(
  db: GritDb,
  fn: (current: AppSettings) => AppSettings,
): Promise<AppSettings> {
  return db.transaction('rw', db.settings, async () => {
    const current = await getSettings(db)
    const next = fn({ ...current, trainingDays: [...current.trainingDays] })
    await db.settings.bulkPut(
      (Object.keys(DEFAULT_SETTINGS) as (keyof AppSettings)[]).map((key) => ({ key, value: next[key] })),
    )
    return next
  })
}
