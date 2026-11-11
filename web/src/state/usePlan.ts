import { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { getSettings, type AppSettings, type GritDb } from '../db/db'
import { exerciseToJs } from '../db/mapping'
import type { ExerciseRow, PlanItemRow } from '../db/schema'
import { grit } from '../domain'
import type { Lift } from '../router'

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
  /** The squat, bench and deadlift themselves, for "Set best". */
  mainExercises: PlanExercise[]
  defaultRpe: number
}

export type CustomExercise = { name: string; category: 'VARIATION' | 'ACCESSORY'; parentLift: Lift | null; muscles: string[] }

export type PlanActions = {
  /** Adds at the end of the day when `existing` is null, otherwise updates it in place. */
  save: (weekday: number, existing: PlanItem | null, exerciseId: number, sets: number, reps: number, targetRpe: number) => Promise<void>
  remove: (item: PlanItem) => Promise<void>
  /** Swaps the item with its neighbour: -1 up, +1 down. */
  move: (item: PlanItem, direction: -1 | 1) => Promise<void>
  /** Creates a custom exercise; resolves to its id. */
  addCustomExercise: (exercise: CustomExercise) => Promise<number>
  /** Stores a known best for a main lift. Values `parseBaselineKg` would not accept are ignored. */
  setBaseline: (exerciseId: number, text: string) => Promise<void>
}

type Stored = { plan: PlanItem[]; exercises: PlanExercise[]; settings: AppSettings }

async function readStored(db: GritDb): Promise<Stored> {
  const [plan, exercises, settings] = await Promise.all([db.planItems.toArray(), db.exercises.toArray(), getSettings(db)])
  return { plan: plan as PlanItem[], exercises: exercises as PlanExercise[], settings }
}

function build({ plan, exercises, settings }: Stored): Omit<PlanState, 'loading'> {
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
    mainExercises: exercises.filter((e) => grit.isMainLift(exerciseToJs(e))),
    defaultRpe: settings.defaultRpe,
  }
}

const EMPTY: Omit<PlanState, 'loading'> = { sections: [], exercises: [], mainExercises: [], defaultRpe: 8 }

/** The day's items as stored now, in order. */
async function itemsOn(db: GritDb, weekday: number): Promise<PlanItem[]> {
  const items = (await db.planItems.where('weekday').equals(weekday).toArray()) as PlanItem[]
  return items.sort((a, b) => a.order - b.order || a.id - b.id)
}

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

  const save = useCallback<PlanActions['save']>(
    async (weekday, existing, exerciseId, sets, reps, targetRpe) => {
      if (existing != null) {
        await db.planItems.update(existing.id, { exerciseId, sets, reps, targetRpe })
        return
      }
      await db.transaction('rw', db.planItems, async () => {
        const last = (await itemsOn(db, weekday)).at(-1)
        await db.planItems.add({ weekday, exerciseId, sets, reps, targetRpe, order: (last?.order ?? -1) + 1 })
      })
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
        const items = await itemsOn(db, item.weekday)
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

  const addCustomExercise = useCallback<PlanActions['addCustomExercise']>(
    async ({ name, category, parentLift, muscles }) =>
      (await db.exercises.add({ name: name.trim(), category, parentLift: category === 'VARIATION' ? parentLift : null, muscles, isBuiltIn: false, archived: false })) as number,
    [db],
  )

  const setBaseline = useCallback<PlanActions['setBaseline']>(
    async (exerciseId, text) => {
      const e1rmKg = grit.parseBaselineKg(text)
      if (e1rmKg == null) return
      await db.baselines.put({ exerciseId, e1rmKg, date: today() })
    },
    [db, today],
  )

  const state = useMemo(() => (stored ? build(stored) : EMPTY), [stored])
  return { loading: stored == null, ...state, save, remove, move, addCustomExercise, setBaseline }
}
