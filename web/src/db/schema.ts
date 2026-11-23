import type { Transaction } from 'dexie'
import { runStrictPressUpgrade } from './strictPressUpgrade'

/** The two sides of the app (bodybuilding mode spec §5.1), as the domain's `TrainingMode` names them. */
export const TRAINING_MODES = ['POWERLIFTING', 'BODYBUILDING'] as const
export type TrainingMode = (typeof TRAINING_MODES)[number]

/**
 * The side a session, plan item or advice row belongs to. Schema version 3 stamps every older row and the app
 * stamps every row it writes; a row with no mode (written outside the app, as tests and older code do) is on the
 * powerlifting side, as every row was before bodybuilding mode.
 */
export function sideOf(row: { mode?: TrainingMode }): TrainingMode {
  return row.mode ?? 'POWERLIFTING'
}

/** A set's unit (1.17), as the domain's `LoadUnit` names it. `weightKg` holds a plate number when the unit is PLATES. */
export const LOAD_UNITS = ['KG', 'PLATES'] as const
export type LoadUnit = (typeof LOAD_UNITS)[number]

/** An exercise's plate setting (1.17), as the domain's `PlateCount` names it: off (kg), whole plates, or half steps. */
export const PLATE_COUNTS = ['OFF', 'WHOLE', 'HALF'] as const
export type PlateCount = (typeof PLATE_COUNTS)[number]

/** A set's unit. Only a plate set stores the key; a row without it (every row before 1.17) is in kg. */
export function setUnit(row: { unit?: LoadUnit }): LoadUnit {
  return row.unit ?? 'KG'
}

/** An exercise's plate setting. Only a plate-counted exercise stores the key; a row without it is off. */
export function plateCountOf(row: { plateCount?: PlateCount }): PlateCount {
  return row.plateCount ?? 'OFF'
}

export type ExerciseRow = {
  id?: number
  name: string
  category: string
  parentLift: string | null
  muscles: string[]
  isBuiltIn: boolean
  archived: boolean
  /** 1.17: written only as WHOLE or HALF, never as OFF (see `plateCountOf`). */
  plateCount?: PlateCount
}

export type SessionRow = {
  id?: number
  /** ISO `YYYY-MM-DD`, unique. */
  date: string
  bodyweightKg: number | null
  readiness: number | null
  /** The side active when the session was started (see `sideOf`). */
  mode?: TrainingMode
}

export type SetRow = {
  id?: number
  sessionId: number
  exerciseId: number
  weightKg: number
  reps: number
  rpe: number | null
  failed: boolean
  missPoint: string | null
  isTopSet: boolean
  /** 1.17: written only as PLATES, never as KG (see `setUnit`). */
  unit?: LoadUnit
}

export type PlanItemRow = {
  id?: number
  /** 1 = Monday .. 7 = Sunday. */
  weekday: number
  exerciseId: number
  sets: number
  reps: number
  targetRpe: number
  order: number
  /** The side whose plan this is (see `sideOf`). */
  mode?: TrainingMode
  /** The top of a rep range, `reps` being its bottom. Absent: fixed reps, as on every powerlifting item. */
  repsHigh?: number
}

export type AdviceRow = {
  id?: number
  type: string
  lift: string | null
  createdAt: string
  reason: string
  severity: number
  status: string
  /** The Android `AdvicePayload` JSON, exactly as the domain facade returns it. */
  payloadJson: string
  /** The side the advice is for (see `sideOf`); deloads and easing back in count on both whatever it says. */
  mode?: TrainingMode
}

export type BaselineRow = {
  exerciseId: number
  e1rmKg: number
  date: string
}

/**
 * One check-in on the bodybuilding side (stage 3): the body parts the lifter marked lagging and growing well that
 * day, as `MuscleGroup` names in the domain's order; every other group is on track. One per date.
 */
export type CheckInRow = {
  id?: number
  /** ISO `YYYY-MM-DD`, unique. */
  date: string
  lagging: string[]
  growingWell: string[]
}

/** Why a planned exercise was skipped (1.13), as the domain's `SkipReason` names it; null is "Just skip". */
export const SKIP_REASONS = ['BUSY', 'PAIN', 'TIME'] as const
export type SkipReason = (typeof SKIP_REASONS)[number]

/**
 * A planned exercise skipped for a day (1.13), as Android's `skips` table holds it. One per date, side and exercise.
 * `mode` is always written: a row without it would drop out of the unique `[date+mode+exerciseId]` index.
 */
export type SkipRow = {
  id?: number
  /** ISO `YYYY-MM-DD`, the local date, as on sessions. */
  date: string
  mode: TrainingMode
  exerciseId: number
  /** The plan item skipped; null once that plan item was deleted. */
  planItemId: number | null
  reason: SkipReason | null
}

export type KeyValueRow = { key: string; value: unknown }

/** One numbered Dexie version. `stores` uses Dexie's schema-string syntax. */
export type SchemaVersion = {
  version: number
  stores: Record<string, string | null>
  upgrade?: (tx: Transaction) => void | Promise<void>
}

export const TABLE_NAMES = [
  'exercises',
  'sessions',
  'sets',
  'planItems',
  'advice',
  'baselines',
  'settings',
  'meta',
  'checkIns',
  'skips',
] as const

/** Only fields that are valid IndexedDB keys and get queried are indexed (booleans are not keys).
 * Every schema change appends a numbered version here, with an upgrade test. */
export const SCHEMA_VERSIONS: SchemaVersion[] = [
  {
    version: 1,
    stores: {
      exercises: '++id, name, category, parentLift',
      sessions: '++id, &date',
      sets: '++id, sessionId, exerciseId',
      planItems: '++id, weekday, exerciseId',
      advice: '++id, type, lift, createdAt, status',
      baselines: 'exerciseId',
      settings: 'key',
      meta: 'key',
    },
  },
  // The strict press release: no table changes, only data. The safety copy is written before it runs
  // (see openGritDb), and a failure aborts it, leaving version 1 as it was.
  { version: 2, stores: {}, upgrade: runStrictPressUpgrade },
  // Bodybuilding mode: each side keeps its own plan, so plan items are read by mode. Everything already stored was
  // made on the powerlifting side.
  { version: 3, stores: { planItems: '++id, weekday, exerciseId, mode' }, upgrade: stampPowerlifting },
  // Check-ins (bodybuilding mode stage 3): a new, empty store; nothing already stored changes.
  { version: 4, stores: { checkIns: '++id, &date' } },
  // Skipped planned exercises (1.13): a new, empty store; nothing already stored changes. A skip whose plan item is
  // deleted keeps a null `planItemId`, which leaves the planItemId index (null is not a key).
  { version: 5, stores: { skips: '++id, &[date+mode+exerciseId], exerciseId, planItemId' } },
  // Counting plates (1.17): a set's `unit` and an exercise's `plateCount`. No new index and no upgrade step: a row
  // without them reads as kg and off, so nothing stored is stamped. Declared for the version number (from 1.17 on, a
  // copy older than the stored version is turned away by openGritDb) and so openGritDb writes the safety copy before
  // it is installed. It does NOT make an older app refuse this database: Dexie opens a newer database whose stores
  // match. What protects 1.16.1 and older is the service worker reloading every window when a new version takes over
  // (public/sw-reload-clients.js); from 1.17 on, also OlderCopyError and its screen.
  { version: 6, stores: {} },
]

/** Schema version 3's upgrade: every plan item, session and advice row without a side is on the powerlifting side. */
async function stampPowerlifting(tx: Transaction): Promise<void> {
  for (const name of ['planItems', 'sessions', 'advice']) {
    await tx
      .table(name)
      .toCollection()
      .modify((row: { mode?: TrainingMode }) => {
        if (row.mode == null) row.mode = 'POWERLIFTING'
      })
  }
}
