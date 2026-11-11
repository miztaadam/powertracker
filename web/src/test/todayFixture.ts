import Dexie from 'dexie'
import { ensureSeeded, openGritDb, type GritDb } from '../db/db'
import type { ExerciseRow, SetRow } from '../db/schema'

/** A Wednesday, the same day the Android Today tests use. */
export const TODAY = '2026-11-11'

export function addDays(iso: string, days: number): string {
  const d = new Date(`${iso}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}

let n = 0

/** A fresh seeded database per test, with helpers that write what the Android repositories would. */
export function todayFixture() {
  const name = `grit-today-${Date.now()}-${++n}`
  const opened: GritDb[] = []

  async function open(): Promise<GritDb> {
    const db = await openGritDb(name)
    await ensureSeeded(db)
    opened.push(db)
    return db
  }

  return {
    open,
    async dispose() {
      for (const db of opened.splice(0)) db.close()
      await Dexie.delete(name)
    },
  }
}

export async function exerciseNamed(db: GritDb, name: string): Promise<ExerciseRow & { id: number }> {
  const row = await db.exercises.where('name').equals(name).first()
  if (!row) throw new Error(`no exercise named ${name}`)
  return row as ExerciseRow & { id: number }
}

export function set(exerciseId: number, weightKg: number, over: Partial<SetRow> = {}): Omit<SetRow, 'sessionId'> {
  return { exerciseId, weightKg, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: false, ...over }
}

export async function startSession(db: GritDb, date: string): Promise<number> {
  return (await db.sessions.add({ date, bodyweightKg: null, readiness: null })) as number
}

export async function logSet(db: GritDb, sessionId: number, s: Omit<SetRow, 'sessionId'>): Promise<number> {
  return (await db.sets.add({ ...s, sessionId })) as number
}

export async function plan(db: GritDb, exerciseId: number, over: { sets?: number; reps?: number; targetRpe?: number; order?: number; weekday?: number } = {}) {
  return db.planItems.add({ weekday: 3, exerciseId, sets: 5, reps: 5, targetRpe: 8, order: 0, ...over })
}

export async function baseline(db: GritDb, exerciseId: number, e1rmKg: number, date: string) {
  await db.baselines.put({ exerciseId, e1rmKg, date })
}

export async function setsOn(db: GritDb, date: string): Promise<SetRow[]> {
  const session = await db.sessions.where('date').equals(date).first()
  return session?.id != null ? db.sets.where('sessionId').equals(session.id).sortBy('id') : []
}

export async function targets(db: GritDb) {
  return (await db.advice.toArray()).filter((a) => a.type === 'TARGET')
}

export const DELOAD_ADVICE = {
  type: 'DELOAD',
  lift: null,
  createdAt: TODAY,
  reason: 'Take a deload week.',
  severity: 2,
  status: 'OPEN',
  payloadJson: '{"type":"com.powertracker.domain.model.AdvicePayload.Deload"}',
}
