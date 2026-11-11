import { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { addPlanItem, planItemsOn, updatePlanItem } from '../db/catalogue'
import { getSettings, type AppSettings, type GritDb } from '../db/db'
import { exerciseToJs } from '../db/mapping'
import type { ExerciseRow, PlanItemRow } from '../db/schema'
import { grit } from '../domain'
import { DEFAULT_SETS, createCustomExercise, defaultReps, recentExerciseIds, type CustomExercise, type PickerEntry } from './exercisePicker'

export type PlanItem = PlanItemRow & { id: number }
export type PlanExercise = ExerciseRow & { id: number }

export type PlanRow = { item: PlanItem; exerciseName: string }

/** One weekday (1 = Monday) with its plan items in order. */
export type DaySection = { weekday: number; rows: PlanRow[] }

export type PlanState = {
  loading: boolean
  /** The training days plus any other day that has plan items, Monday first. */
  sections: DaySection[]
  /** Exercises that are not archived, for the picker. */
  exercises: PlanExercise[]
  /** Exercise ids, most recently logged first, for the picker's "Your exercises". */
  recentIds: number[]
  /** The squat, bench and deadlift themselves, for "Set best". */
  mainExercises: PlanExercise[]
  defaultRpe: number
}

export const MIN_RPE = 6
export const MAX_RPE = 10

export type PlanActions = {
  /**
   * Adds what was picked at the end of the day: 3 × 5 for a compound lift, 3 × 10 for an accessory,
   * at the default RPE. A catalogue exercise gets its row, if it has none yet, in the same transaction.
   */
  add: (weekday: number, entry: PickerEntry) => Promise<void>
  /** Updates the item in place. `exercise` is set when the exercise itself was changed. */
  save: (existing: PlanItem, exercise: PickerEntry | null, sets: number, reps: number, targetRpe: number) => Promise<void>
  remove: (item: PlanItem) => Promise<void>
  /** Swaps the item with its neighbour: -1 up, +1 down. */
  move: (item: PlanItem, direction: -1 | 1) => Promise<void>
  /** Creates a custom exercise and resolves to it as a picker entry, ready to `add` or `save`. A catalogue name gives the catalogue's entry. */
  addCustomExercise: (exercise: CustomExercise) => Promise<PickerEntry>
  /** Stores a known best for a main lift. Values `parseBaselineKg` would not accept are ignored. */
  setBaseline: (exerciseId: number, text: string) => Promise<void>
}

type Stored = { plan: PlanItem[]; exercises: PlanExercise[]; settings: AppSettings; recentIds: number[] }

async function readStored(db: GritDb): Promise<Stored> {
  const [plan, exercises, settings, sessions, sets] = await Promise.all([db.planItems.toArray(), db.exercises.toArray(), getSettings(db), db.sessions.toArray(), db.sets.orderBy('id').toArray()])
  const recentIds = recentExerciseIds(sessions.map((s) => ({ id: s.id as number, date: s.date, sets: sets.filter((set) => set.sessionId === s.id) })))
  return { plan: plan as PlanItem[], exercises: exercises as PlanExercise[], settings, recentIds }
}

function build({ plan, exercises, settings, recentIds }: Stored): Omit<PlanState, 'loading'> {
  const names = new Map(exercises.map((e) => [e.id, e.name]))
  const weekdays = [...new Set([...settings.trainingDays, ...plan.map((p) => p.weekday)])].sort((a, b) => a - b)
  return {
    sections: weekdays.map((weekday) => ({
      weekday,
      rows: plan
        .filter((p) => p.weekday === weekday)
        .sort((a, b) => a.order - b.order || a.id - b.id)
        .map((item) => ({ item, exerciseName: names.get(item.exerciseId) ?? 'Unknown exercise' })),
    })),
    exercises: exercises.filter((e) => !e.archived),
    recentIds,
    mainExercises: exercises.filter((e) => grit.isMainLift(exerciseToJs(e))),
    defaultRpe: settings.defaultRpe,
  }
}

const EMPTY: Omit<PlanState, 'loading'> = { sections: [], exercises: [], recentIds: [], mainExercises: [], defaultRpe: 8 }

/** The weekly plan, read live from the database, and the edits the Plan screen makes to it. */
export function usePlan(db: GritDb, today: () => string): PlanState & PlanActions {
  const [stored, setStored] = useState<Stored | null>(null)
  const [readError, setReadError] = useState<unknown>(null)
  useEffect(() => {
    const subscription = liveQuery(() => readStored(db)).subscribe({ next: setStored, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db])
  // A failed read is for the app's error boundary, not for an empty screen that looks like no plan.
  if (readError != null) throw readError

  const add = useCallback<PlanActions['add']>(
    async (weekday, entry) => {
      const { defaultRpe } = await getSettings(db)
      const targetRpe = Math.min(MAX_RPE, Math.max(MIN_RPE, defaultRpe))
      await addPlanItem(db, { weekday, exerciseId: entry.exerciseId, sets: DEFAULT_SETS, reps: defaultReps(entry), targetRpe }, entry.library?.name ?? null)
    },
    [db],
  )

  const save = useCallback<PlanActions['save']>(
    async (existing, exercise, sets, reps, targetRpe) => {
      await updatePlanItem(db, existing.id, { exerciseId: exercise ? exercise.exerciseId : existing.exerciseId, sets, reps, targetRpe }, exercise?.library?.name ?? null)
    },
    [db],
  )

  const remove = useCallback<PlanActions['remove']>(
    async (item) => {
      await db.planItems.delete(item.id)
    },
    [db],
  )

  const move = useCallback<PlanActions['move']>(
    async (item, direction) => {
      await db.transaction('rw', db.planItems, async () => {
        const items = await planItemsOn(db, item.weekday)
        const from = items.findIndex((p) => p.id === item.id)
        const moved = items[from]
        const neighbour = items[from + direction]
        if (moved == null || neighbour == null) return
        items[from + direction] = moved
        items[from] = neighbour
        // Renumbered rather than swapped, so two items that share an order still change places.
        await Promise.all(items.map((p, order) => (p.order === order ? null : db.planItems.update(p.id, { order }))))
      })
    },
    [db],
  )

  const addCustomExercise = useCallback<PlanActions['addCustomExercise']>((exercise) => createCustomExercise(db, exercise), [db])

  const setBaseline = useCallback<PlanActions['setBaseline']>(
    async (exerciseId, text) => {
      const e1rmKg = grit.parseBaselineKg(text)
      if (e1rmKg == null) return
      await db.baselines.put({ exerciseId, e1rmKg, date: today() })
    },
    [db, today],
  )

  const state = useMemo(() => (stored ? build(stored) : EMPTY), [stored])
  return { loading: stored == null, ...state, add, save, remove, move, addCustomExercise, setBaseline }
}
