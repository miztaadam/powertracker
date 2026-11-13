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

export type ExerciseRow = {
  id?: number
  name: string
  category: string
  parentLift: string | null
  muscles: string[]
  isBuiltIn: boolean
  archived: boolean
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
