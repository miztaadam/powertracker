import type { GritDb } from '../db/db'
import type { AdviceRow, TrainingMode } from '../db/schema'
import { TODAY, addDays } from './todayFixture'

const PAYLOAD = 'com.powertracker.domain.model.AdvicePayload'

type Prescription = { exerciseName: string; sets: number; repsLow: number; repsHigh: number }

/** Payloads exactly as Android stores them: defaults (a deload's factors) are left out. */
export const payload = {
  accessories: (items: Prescription[]) => JSON.stringify({ type: `${PAYLOAD}.Accessories`, items }),
  deload: () => JSON.stringify({ type: `${PAYLOAD}.Deload` }),
  reEntry: () => JSON.stringify({ type: `${PAYLOAD}.ReEntry` }),
  weakPoint: (lift: string, missPoint: string | null) => JSON.stringify({ type: `${PAYLOAD}.WeakPoint`, lift, missPoint }),
  target: (exerciseId: number) => JSON.stringify({ type: `${PAYLOAD}.Target`, exerciseId, weightKg: 155, reps: 5, rpe: 8 }),
}

export const rx = (exerciseName: string, sets: number, repsLow: number, repsHigh: number): Prescription => ({ exerciseName, sets, repsLow, repsHigh })

/** An OPEN piece of advice created today unless `over` says otherwise. */
export function advice(type: string, lift: string | null, reason: string, payloadJson: string, over: Partial<AdviceRow> = {}): AdviceRow {
  return { type, lift, createdAt: TODAY, reason, severity: 3, status: 'OPEN', payloadJson, ...over }
}

/**
 * A session on each of `sides` well before today (40, 41, ... days ago): a side trained before any break, so an
 * easing back in shows on it (spec point 10).
 */
export async function trainedSides(db: GritDb, ...sides: TrainingMode[]): Promise<void> {
  for (const [i, mode] of sides.entries()) await db.sessions.add({ date: addDays(TODAY, -40 - i), bodyweightKg: null, readiness: null, mode })
}

export async function insertAdvice(db: GritDb, ...rows: AdviceRow[]): Promise<void> {
  await db.advice.bulkAdd(rows)
}

/** The stored plan without ids, in weekday then order. */
export async function planOf(db: GritDb) {
  const rows = await db.planItems.toArray()
  return rows
    .map(({ id: _id, ...rest }) => rest)
    .sort((a, b) => a.weekday - b.weekday || a.order - b.order)
}

export async function statuses(db: GritDb): Promise<string[]> {
  return (await db.advice.orderBy('id').toArray()).map((a) => a.status)
}
