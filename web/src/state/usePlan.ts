import { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { addPlanItem, planFor, planItemsOn, updatePlanItem } from '../db/catalogue'
import { activeTrainingDays, getSettings, type AppSettings, type GritDb } from '../db/db'
import { exerciseToJs } from '../db/mapping'
import { sideOf, type ExerciseRow, type PlanItemRow, type TrainingMode } from '../db/schema'
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
  /** The days the lifter trains on this side (1 = Monday), from Settings; a day can have plan items without being one. */
  trainingDays: number[]
  /** The side whose plan this is: the side the app is on. */
  side: TrainingMode
}

/** How adding an exercise from the Exercises tab went: added, or already on that day and left alone. */
export type PlanAdd = 'ADDED' | 'ALREADY_THERE'

/** The days "Add to plan" offers: the lifter's training days in weekday order, or the whole week when none are set. */
export function planDayChoices(trainingDays: readonly number[]): number[] {
  const days = [...new Set(trainingDays)].sort((a, b) => a - b)
  return days.length > 0 ? days : [1, 2, 3, 4, 5, 6, 7]
}

export const MIN_RPE = 6
export const MAX_RPE = 10

export type PlanActions = {
  /**
   * Adds what was picked at the end of the day: 3 × 5 for a compound lift, 3 × 10 for an accessory,
   * at the default RPE. A catalogue exercise gets its row, if it has none yet, in the same transaction.
   */
  add: (weekday: number, entry: PickerEntry) => Promise<void>
  /**
   * `add` for the Exercises tab, where the day is chosen after the exercise: the same defaults and the
   * same single transaction, but an exercise already on that day is left alone. It reads what is
   * stored at the moment of the call. Rejects when the write fails; nothing is changed then.
   */
  addOnce: (weekday: number, entry: PickerEntry) => Promise<PlanAdd>
  /**
   * Updates the item in place. `exercise` is set when the exercise itself was changed. `repsHigh` is the top of
   * a range (`reps` its bottom), for an item on the bodybuilding side; left out, the item keeps what it has.
   */
  save: (existing: PlanItem, exercise: PickerEntry | null, sets: number, reps: number, targetRpe: number, repsHigh?: number) => Promise<void>
  remove: (item: PlanItem) => Promise<void>
  /** Swaps the item with its neighbour: -1 up, +1 down. */
  move: (item: PlanItem, direction: -1 | 1) => Promise<void>
  /** Creates a custom exercise and resolves to it as a picker entry, ready to `add` or `save`. A catalogue name gives the catalogue's entry. */
  addCustomExercise: (exercise: CustomExercise) => Promise<PickerEntry>
  /** Stores a known best for a main lift. Values `parseBaselineKg` would not accept are ignored. */
  setBaseline: (exerciseId: number, text: string) => Promise<void>
}

/** The bodybuilding side's new entries: 3 sets of 8-12 at RPE 8 (the owner's numbers, spec §7.3). */
export const BODYBUILDING_DEFAULTS = { sets: 3, reps: 8, repsHigh: 12, targetRpe: 8 } as const

/** The most reps a plan entry can ask for, the top of a range included. */
export const MAX_PLAN_REPS = 20

type Stored = { plan: PlanItem[]; exercises: PlanExercise[]; settings: AppSettings; recentIds: number[] }

/** The side the app is on, whose plan the Plan screen (and "Add to plan" on the Exercises tab) reads and edits. */
async function activeSide(db: GritDb): Promise<TrainingMode> {
  return (await getSettings(db)).trainingMode
}

async function readStored(db: GritDb): Promise<Stored> {
  const [settings, exercises, sessions, sets] = await Promise.all([getSettings(db), db.exercises.toArray(), db.sessions.toArray(), db.sets.orderBy('id').toArray()])
  const plan = await planFor(db, settings.trainingMode)
  const recentIds = recentExerciseIds(sessions.map((s) => ({ id: s.id as number, date: s.date, sets: sets.filter((set) => set.sessionId === s.id) })))
  return { plan: plan as PlanItem[], exercises: exercises as PlanExercise[], settings, recentIds }
}

function build({ plan, exercises, settings, recentIds }: Stored): Omit<PlanState, 'loading'> {
  const names = new Map(exercises.map((e) => [e.id, e.name]))
  const trainingDays = activeTrainingDays(settings)
  const weekdays = [...new Set([...trainingDays, ...plan.map((p) => p.weekday)])].sort((a, b) => a - b)
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
    trainingDays,
    side: settings.trainingMode,
  }
}

/**
 * What was picked as a new plan item for the day on `side`: on the powerlifting side 3 × 5 or 3 × 10 at the default
 * RPE as stored now; on the bodybuilding side 3 × 8-12 at RPE 8.
 */
async function newItem(db: GritDb, weekday: number, entry: PickerEntry, side: TrainingMode) {
  if (side === 'BODYBUILDING') return { weekday, exerciseId: entry.exerciseId, ...BODYBUILDING_DEFAULTS }
  const { defaultRpe } = await getSettings(db)
  return { weekday, exerciseId: entry.exerciseId, sets: DEFAULT_SETS, reps: defaultReps(entry), targetRpe: Math.min(MAX_RPE, Math.max(MIN_RPE, defaultRpe)) }
}

const EMPTY: Omit<PlanState, 'loading'> = { sections: [], exercises: [], recentIds: [], mainExercises: [], defaultRpe: 8, trainingDays: [], side: 'POWERLIFTING' }

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
      // The side as stored at the moment of the tap, which the screen shows.
      const side = await activeSide(db)
      await addPlanItem(db, await newItem(db, weekday, entry, side), entry.library?.name ?? null, side)
    },
    [db],
  )

  const addOnce = useCallback<PlanActions['addOnce']>(
    (weekday, entry) =>
      // One transaction: the check, the exercise's row and the plan item stand or fall together.
      db.transaction('rw', db.exercises, db.planItems, db.settings, async () => {
        const name = entry.library?.name ?? null
        const side = await activeSide(db)
        // The row a catalogue name would be saved to (the oldest live one, as resolveCatalogue picks it), if it has one yet.
        const rowId = name == null ? entry.exerciseId : ((await db.exercises.toArray()).find((e) => !e.archived && e.name.toLowerCase() === name.toLowerCase())?.id ?? null)
        if (rowId != null && (await planItemsOn(db, weekday, side)).some((p) => p.exerciseId === rowId)) return 'ALREADY_THERE'
        await addPlanItem(db, await newItem(db, weekday, entry, side), name, side)
        return 'ADDED'
      }),
    [db],
  )

  const save = useCallback<PlanActions['save']>(
    async (existing, exercise, sets, reps, targetRpe, repsHigh) => {
      await updatePlanItem(db, existing.id, { exerciseId: exercise ? exercise.exerciseId : existing.exerciseId, sets, reps, targetRpe, ...(repsHigh != null ? { repsHigh } : {}) }, exercise?.library?.name ?? null)
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
        const items = await planItemsOn(db, item.weekday, sideOf(item))
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
  return { loading: stored == null, ...state, add, addOnce, save, remove, move, addCustomExercise, setBaseline }
}
