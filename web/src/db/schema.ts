import type { Transaction } from 'dexie'

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

/** Every schema change appends a numbered version here, with an upgrade test. */
export const SCHEMA_VERSIONS: SchemaVersion[] = [
  {
    version: 1,
    stores: {
      exercises: '++id, name, category, parentLift, muscles, isBuiltIn, archived',
      sessions: '++id, &date',
      sets: '++id, sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet',
      planItems: '++id, weekday, exerciseId, sets, reps, targetRpe, order',
      advice: '++id, type, lift, createdAt, reason, severity, status, payloadJson',
      baselines: 'exerciseId, e1rmKg, date',
      settings: 'key',
      meta: 'key',
    },
  },
]
