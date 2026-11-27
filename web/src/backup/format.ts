/**
 * Android backup format, version 7: mirrors `BackupFile` and its DTOs in
 * app/src/main/kotlin/com/powertracker/data/Backup.kt field for field.
 * Dates are epochDay, weekdays 1–7 (Monday = 1), enums are their Kotlin names.
 */
/**
 * 2: the strict press release (category STRICT_PRESS, `settings.showStrictPress`).
 * 3: bodybuilding mode (the training side: `mode` on sessions, plan items and advice, `repsHigh` on plan items, and
 * four settings). Version 1 and 2 files still restore, everything in them on the powerlifting side.
 * 4: check-ins (a top-level `checkIns`) and the advice types MORE_WORK and SWAP. A version 1 to 3 file restores with
 * no check-ins.
 * 5: 1.13's skipped planned exercises (a top-level `skips`) and the advice type SKIP. A version 1 to 4 file restores
 * with no skips; an app that reads only version 4 turns a version 5 file away as too new.
 * 6: 1.17's counting plates (a set's `unit`, an exercise's `plateCount`, and a unit in a TARGET payload). Each is
 * written only when it is not the default, so a row without it is in kg and off; a version 1 to 5 file restores with
 * every set in kg and every exercise off. An app that reads only version 5 turns a version 6 file away as too new.
 * 7: 1.24's supersets (a plan item's `supersetWithAbove`). Written only when true, as the item's last key, so an item
 * without it is not linked; a version 1 to 6 file restores with no supersets. A flag that cannot count (the first item
 * of a day, the third in a row, the same exercise twice) is cleared on restore, in both apps, by the domain's
 * `Supersets.stale`. An app that reads only version 6 turns a version 7 file away as too new.
 */
export const BACKUP_VERSION = 7

/** The domain's `TrainingMode` names. */
export { TRAINING_MODES } from '../db/schema'
/** Version 6: the domain's `LoadUnit` and `PlateCount` names. */
export { LOAD_UNITS, PLATE_COUNTS } from '../db/schema'

export const CATEGORIES = ['SQUAT', 'BENCH', 'DEADLIFT', 'STRICT_PRESS', 'VARIATION', 'ACCESSORY'] as const
export const MAIN_LIFTS = ['SQUAT', 'BENCH', 'DEADLIFT', 'STRICT_PRESS'] as const
export const MISS_POINTS = ['FLOOR', 'MID', 'LOCKOUT'] as const
export const ADVICE_TYPES = ['TARGET', 'STALL', 'DELOAD', 'WEAK_POINT', 'RE_ENTRY', 'MORE_WORK', 'SWAP', 'SKIP'] as const
/** The domain's `SkipReason` names; a skip with none is "Just skip". */
export { SKIP_REASONS } from '../db/schema'
export const ADVICE_STATUSES = ['OPEN', 'APPLIED', 'DISMISSED'] as const
/** Android's `RestAlert` enum. */
export const ANDROID_REST_ALERTS = ['VIBRATE', 'SOUND_AND_VIBRATE', 'OFF'] as const
/** The web app's rest alert, carried in `webRestAlert` (Android keeps that field as it is, unread). */
export const WEB_REST_ALERTS = ['SOUND', 'SOUND_AND_VIBRATE', 'SILENT', 'OFF'] as const

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
  // Version 3. An older file has none of these: the powerlifting side, bodybuilding days Monday, Wednesday and
  // Friday (the default), neither side ever left. The parser fills them in.
  trainingMode?: string
  bodybuildingTrainingDays?: number[]
  /** epochDay, or null until the side is first left. */
  powerliftingLeftOn?: number | null
  bodybuildingLeftOn?: number | null
}

export type ExerciseDto = {
  id: number
  name: string
  category: string
  parentLift: string | null
  muscles: string[]
  isBuiltIn: boolean
  archived: boolean
  /** Version 6, the row's last key: written only as WHOLE or HALF; absent means OFF. The parser fills it in. */
  plateCount?: string
}

/** `mode` (version 3) is absent from older files, which means POWERLIFTING; the parser fills it in, as on plan items and advice. */
export type SessionDto = { id: number; date: number; bodyweightKg: number | null; readiness: number | null; mode?: string }

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
  /** Version 6, the row's last key: written only as PLATES; absent means KG. The parser fills it in. */
  unit?: string
}

export type PlanItemDto = {
  id: number
  weekday: number
  exerciseId: number
  sets: number
  reps: number
  targetRpe: number
  sortOrder: number
  mode?: string
  /** Version 3: the top of a rep range; null for fixed reps. */
  repsHigh?: number | null
  /** Version 7: done as a superset with the item above it. Only when true. */
  supersetWithAbove?: boolean
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
  mode?: string
}

export type BaselineDto = { exerciseId: number; e1rmKg: number; date: number }

/** Version 4: one check-in. `lagging` and `growingWell` are `MuscleGroup` names; every other group is on track. */
export type CheckInDto = { id: number; date: number; lagging: string[]; growingWell: string[] }

/**
 * Version 5: one skipped planned exercise, as Android's `SkipDto`, in its key order. `mode` and `reason` are names,
 * `reason` null for "Just skip"; `planItemId` is null once that plan item was deleted. Android has no defaults here:
 * every key is always written.
 */
export type SkipDto = { id: number; date: number; mode: string; exerciseId: number; planItemId: number | null; reason: string | null }

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
  /** Version 4; absent from an older file, which means none. The parser fills it in. */
  checkIns: CheckInDto[]
  /** Version 5; absent from an older file, which means none. The parser fills it in. */
  skips: SkipDto[]
}
