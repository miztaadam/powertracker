/**
 * Android backup format, version 2: mirrors `BackupFile` and its DTOs in
 * app/src/main/kotlin/com/powertracker/data/Backup.kt field for field.
 * Dates are epochDay, weekdays 1–7 (Monday = 1), enums are their Kotlin names.
 */
/** 2: the strict press release (category STRICT_PRESS, `settings.showStrictPress`). Version 1 files still restore. */
export const BACKUP_VERSION = 2

export const CATEGORIES = ['SQUAT', 'BENCH', 'DEADLIFT', 'STRICT_PRESS', 'VARIATION', 'ACCESSORY'] as const
export const MAIN_LIFTS = ['SQUAT', 'BENCH', 'DEADLIFT', 'STRICT_PRESS'] as const
export const MISS_POINTS = ['FLOOR', 'MID', 'LOCKOUT'] as const
export const ADVICE_TYPES = ['TARGET', 'STALL', 'DELOAD', 'WEAK_POINT', 'RE_ENTRY'] as const
export const ADVICE_STATUSES = ['OPEN', 'APPLIED', 'DISMISSED'] as const
/** Android's `RestAlert` enum. */
export const ANDROID_REST_ALERTS = ['VIBRATE', 'SOUND_AND_VIBRATE', 'OFF'] as const
/** The web app's rest alert, carried in `webRestAlert`. */
export const WEB_REST_ALERTS = ['SOUND', 'SILENT', 'OFF'] as const

export type AndroidRestAlert = (typeof ANDROID_REST_ALERTS)[number]
export type WebRestAlert = (typeof WEB_REST_ALERTS)[number]

export type BackupSettings = {
  changePlates: boolean
  defaultRpe: number
  trainingDays: number[]
  appLock: boolean
  restMainSec: number
  restAccessorySec: number
  restAlert: string
  webRestAlert: string | null
  /** Version 2. A version-1 file has none, which means off. */
  showStrictPress: boolean
}

export type ExerciseDto = {
  id: number
  name: string
  category: string
  parentLift: string | null
  muscles: string[]
  isBuiltIn: boolean
  archived: boolean
}

export type SessionDto = { id: number; date: number; bodyweightKg: number | null; readiness: number | null }

export type SetDto = {
  id: number
  sessionId: number
  exerciseId: number
  weightKg: number
  reps: number
  rpe: number | null
  failed: boolean
  missPoint: string | null
  isTopSet: boolean
}

export type PlanItemDto = {
  id: number
  weekday: number
  exerciseId: number
  sets: number
  reps: number
  targetRpe: number
  sortOrder: number
}

export type AdviceDto = {
  id: number
  type: string
  lift: string | null
  createdAt: number
  reason: string
  severity: number
  payload: string
  status: string
}

export type BaselineDto = { exerciseId: number; e1rmKg: number; date: number }

export type BackupFile = {
  version: number
  exportedAt: number
  settings: BackupSettings
  exercises: ExerciseDto[]
  sessions: SessionDto[]
  sets: SetDto[]
  planItems: PlanItemDto[]
  advice: AdviceDto[]
  baselines: BaselineDto[]
}
