import type { Transaction } from 'dexie'
import { runStrictPressUpgrade } from './strictPressUpgrade'

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
]
