import { liveQuery } from 'dexie'
import { useEffect, useMemo, useState } from 'react'
import { loadHistory, type GritDb } from '../db/db'
import type { LoadUnit, TrainingMode } from '../db/schema'
import type { JsHistory } from '../domain'
import type { PastSet } from '../ui/components/PastSets'

/** One exercise of a workout with every set of it, in the order logged. */
export type WorkoutExercise = { exerciseId: number; name: string; sets: PastSet[] }

/** One past workout (a session with at least one set). `date` is `YYYY-MM-DD`; `setCount` is over all its exercises. */
export type Workout = { sessionId: number; date: string; side: TrainingMode; bodyweightKg: number | null; exercises: WorkoutExercise[]; setCount: number }

export type WorkoutHistoryState = { loading: boolean; workouts: Workout[] }

/** A set whose exercise has no row any more (never the case for a stored log: an exercise with sets cannot be deleted). */
const UNKNOWN_EXERCISE = 'Exercise'

/**
 * Every workout of the log, newest first, from both sides (1.19): each session that has a set, with its exercises in
 * the order each was first logged and every set of an exercise together, in logged order (an exercise come back to
 * later in the session is still one group). A session with no sets is no workout: it is left out. An archived
 * exercise keeps its name.
 */
export function buildWorkoutHistory(history: JsHistory): WorkoutHistoryState {
  const names = new Map(history.exercises.map((e) => [e.id, e.name]))
  const workouts = history.sessions
    .filter((session) => session.sets.length > 0)
    // Newest first; two sessions on one date (never stored, the date is unique) keep the later one first.
    .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id)
    .map((session): Workout => {
      const groups = new Map<number, PastSet[]>()
      for (const set of session.sets) {
        const sets = groups.get(set.exerciseId) ?? []
        groups.set(set.exerciseId, sets)
        sets.push({ weightKg: set.weightKg, reps: set.reps, rpe: set.rpe ?? null, failed: set.failed, ...(set.unit === 'PLATES' ? { unit: 'PLATES' as LoadUnit } : {}) })
      }
      return {
        sessionId: session.id,
        date: session.date,
        side: session.mode === 'BODYBUILDING' ? 'BODYBUILDING' : 'POWERLIFTING',
        bodyweightKg: session.bodyweightKg ?? null,
        exercises: [...groups].map(([exerciseId, sets]) => ({ exerciseId, name: names.get(exerciseId) ?? UNKNOWN_EXERCISE, sets })),
        setCount: session.sets.length,
      }
    })
  return { loading: false, workouts }
}

const LOADING: WorkoutHistoryState = { loading: true, workouts: [] }

/** Every workout, read live from the database. */
export function useWorkoutHistory(db: GritDb): WorkoutHistoryState {
  const [history, setHistory] = useState<JsHistory>()
  const [readError, setReadError] = useState<unknown>(null)
  useEffect(() => {
    const subscription = liveQuery(() => loadHistory(db)).subscribe({ next: setHistory, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db])
  // A failed read is for the app's error boundary, not for a history that looks empty.
  if (readError != null) throw readError
  return useMemo(() => (history ? buildWorkoutHistory(history) : LOADING), [history])
}
