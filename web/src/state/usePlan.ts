import { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { addPlanItem, deletePlanItem, planFor, planItemsOn, resolveCatalogue, updatePlanItem } from '../db/catalogue'
import { activeTrainingDays, getSettings, loadHistory, type AppSettings, type GritDb } from '../db/db'
import { exerciseToJs } from '../db/mapping'
import { sideOf, type ExerciseRow, type PlanItemRow, type TrainingMode } from '../db/schema'
import { grit, type JsHistory } from '../domain'
import { toHalfKg } from './useProgress'
import { slideNextChange } from '../ui/viewTransition'
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
  /**
   * The main lifts' own exercises (squat, bench, deadlift and, once it has a row, strict press), for "Set best". With
   * the strict press switch on and no strict press row yet, the catalogue's strict press (`CATALOGUE_STRICT_PRESS`, id
   * 0) comes last: its Set best makes its row.
   */
  mainExercises: PlanExercise[]
  /**
   * The best e1RM of each row's lift to the half kilo (`grit.knownBest`: the higher of the typed best and the best
   * logged), by exercise id. A row with no best has no entry and shows no line.
   */
  bests: Readonly<Record<number, number>>
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

/** The numbers of a plan item not stored yet: what the add form opens with, and what it gives back to `add`. */
export type PlanDraft = { sets: number; reps: number; targetRpe: number; repsHigh?: number }

export type PlanActions = {
  /**
   * The ready-made numbers a pick would be added with on the side the screen shows (`planDraft`). Worked out from
   * what is already read, without waiting, so the add form takes the picker's place in the same render. Stores nothing.
   */
  draft: (entry: PickerEntry) => PlanDraft
  /**
   * Adds what was picked at the end of the day with exactly the numbers chosen in the add form, on the side as
   * stored at the moment of the tap. A catalogue exercise gets its row, if it has none yet, in the same transaction.
   */
  add: (weekday: number, entry: PickerEntry, chosen: PlanDraft) => Promise<void>
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
  /**
   * Stores a known best for a main lift. Values `parseBaselineKg` would not accept are ignored. Id 0 is the catalogue's
   * strict press, stored nowhere yet: its row is found, restored or made as any catalogue pick's is, in the same
   * transaction as the best.
   */
  setBaseline: (exerciseId: number, text: string) => Promise<void>
}

/** The bodybuilding side's new entries: 3 sets of 8-12 at RPE 8 (the owner's numbers, spec §7.3). */
export const BODYBUILDING_DEFAULTS = { sets: 3, reps: 8, repsHigh: 12, targetRpe: 8 } as const

/** The most reps a plan entry with fixed reps (the powerlifting side) can ask for. */
export const MAX_PLAN_REPS = 20

/** The most reps the top of a range (the bodybuilding side) can ask for. */
export const MAX_RANGE_REPS = 30

type Stored = { plan: PlanItem[]; exercises: PlanExercise[]; settings: AppSettings; recentIds: number[]; history: JsHistory }

const STRICT_PRESS = 'STRICT_PRESS'

/** The catalogue's strict press as a Known bests row before it has a row of its own: stored nowhere (id 0). */
export function catalogueStrictPress(): PlanExercise {
  const entry = grit.findInLibrary(grit.liftLabel(STRICT_PRESS))
  if (!entry) throw new Error('Strict press is missing from the catalogue')
  return { id: 0, name: entry.name, category: entry.category, parentLift: entry.parentLift ?? null, muscles: [...entry.muscles], isBuiltIn: true, archived: false }
}

/** The Known bests rows: the stored main lifts, then the catalogue's strict press while the switch is on and it has no row. */
export function knownBestRows(exercises: readonly PlanExercise[], showStrictPress: boolean): PlanExercise[] {
  const stored = exercises.filter((e) => grit.isMainLift(exerciseToJs(e)))
  const hasPress = stored.some((e) => grit.mainLiftOf(exerciseToJs(e)) === STRICT_PRESS)
  return showStrictPress && !hasPress ? [...stored, catalogueStrictPress()] : stored
}

/** Each stored row's best (`grit.knownBest` of its lift, to the half kilo, as Progress rounds), by id. */
export function knownBests(rows: readonly PlanExercise[], history: JsHistory): Record<number, number> {
  const byLift = new Map<string, number | null>()
  const bests: Record<number, number> = {}
  for (const row of rows) {
    const lift = grit.mainLiftOf(exerciseToJs(row))
    if (row.id === 0 || lift == null) continue
    if (!byLift.has(lift)) byLift.set(lift, grit.knownBest(history, lift) ?? null)
    const kg = byLift.get(lift)
    if (kg != null) bests[row.id] = toHalfKg(kg)
  }
  return bests
}

/** The side the app is on, whose plan the Plan screen (and "Add to plan" on the Exercises tab) reads and edits. */
async function activeSide(db: GritDb): Promise<TrainingMode> {
  return (await getSettings(db)).trainingMode
}

async function readStored(db: GritDb): Promise<Stored> {
  const [settings, exercises, sessions, sets, history] = await Promise.all([
    getSettings(db),
    db.exercises.toArray(),
    db.sessions.toArray(),
    db.sets.orderBy('id').toArray(),
    loadHistory(db),
  ])
  const plan = await planFor(db, settings.trainingMode)
  const recentIds = recentExerciseIds(sessions.map((s) => ({ id: s.id as number, date: s.date, sets: sets.filter((set) => set.sessionId === s.id) })))
  return { plan: plan as PlanItem[], exercises: exercises as PlanExercise[], settings, recentIds, history }
}

function build({ plan, exercises, settings, recentIds, history }: Stored): Omit<PlanState, 'loading'> {
  const mainExercises = knownBestRows(exercises, settings.showStrictPress)
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
    mainExercises,
    bests: knownBests(mainExercises, history),
    defaultRpe: settings.defaultRpe,
    trainingDays,
    side: settings.trainingMode,
  }
}

/**
 * The ready-made numbers of a new plan item on `side`: on the powerlifting side 3 × 5 for a compound lift or 3 × 10
 * for an accessory at `defaultRpe` (kept within MIN_RPE-MAX_RPE); on the bodybuilding side 3 × 8-12 at RPE 8.
 */
export function planDraft(side: TrainingMode, defaultRpe: number, entry: PickerEntry): PlanDraft {
  if (side === 'BODYBUILDING') return { ...BODYBUILDING_DEFAULTS }
  return { sets: DEFAULT_SETS, reps: defaultReps(entry), targetRpe: Math.min(MAX_RPE, Math.max(MIN_RPE, defaultRpe)) }
}

/** What was picked as a new plan item for the day on `side`, with `planDraft`'s numbers at the default RPE as stored now. */
async function newItem(db: GritDb, weekday: number, entry: PickerEntry, side: TrainingMode) {
  const defaultRpe = side === 'BODYBUILDING' ? 0 : (await getSettings(db)).defaultRpe
  return { weekday, exerciseId: entry.exerciseId, ...planDraft(side, defaultRpe, entry) }
}

/** The rows of each day in the order they are drawn: what a slide waits for a change to. */
const rowOrder = (stored: Stored | null): string =>
  stored == null
    ? ''
    : [...stored.plan]
        .sort((a, b) => a.weekday - b.weekday || a.order - b.order || a.id - b.id)
        .map((p) => `${p.weekday}:${p.id}`)
        .join()

/** Runs a write of the Plan screen's, asking that the change it makes slides into place; a write that fails asks nothing. */
async function sliding(slide: { arm: () => void; disarm: () => void }, write: () => Promise<unknown>): Promise<void> {
  slide.arm()
  try {
    await write()
  } catch (e) {
    slide.disarm()
    throw e
  }
}

const EMPTY: Omit<PlanState, 'loading'> = { sections: [], exercises: [], recentIds: [], mainExercises: [], bests: {}, defaultRpe: 8, trainingDays: [], side: 'POWERLIFTING' }

/** The weekly plan, read live from the database, and the edits the Plan screen makes to it. */
export function usePlan(db: GritDb, today: () => string): PlanState & PlanActions {
  const [stored, setStored] = useState<Stored | null>(null)
  const [readError, setReadError] = useState<unknown>(null)
  // Stage 3: the read after an add, a move or a delete on the Plan screen slides the rows into place (the rows of each
  // day, in order, are its shape). Every other read lands at once: "Add to plan" on the Exercises tab never slides.
  const [slide] = useState(() => slideNextChange<Stored | null>(setStored, rowOrder))
  useEffect(() => {
    const subscription = liveQuery(() => readStored(db)).subscribe({ next: slide.next, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db, slide])
  // A failed read is for the app's error boundary, not for an empty screen that looks like no plan.
  if (readError != null) throw readError

  // The side and default RPE the screen shows, as last read.
  const draft = useCallback<PlanActions['draft']>((entry) => planDraft(stored?.settings.trainingMode ?? EMPTY.side, stored?.settings.defaultRpe ?? EMPTY.defaultRpe, entry), [stored])

  const add = useCallback<PlanActions['add']>(
    (weekday, entry, chosen) =>
      sliding(slide, async () => {
        // The side as stored at the moment of the tap, which the screen shows.
        const side = await activeSide(db)
        const { sets, reps, targetRpe, repsHigh } = chosen
        // The form's numbers win, even if the side changed while it was open. No `repsHigh` key at all for fixed reps.
        const item = { weekday, exerciseId: entry.exerciseId, sets, reps, targetRpe, ...(repsHigh != null ? { repsHigh } : {}) }
        await addPlanItem(db, item, entry.library?.name ?? null, side)
      }),
    [db, slide],
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
    (item) =>
      sliding(slide, async () => {
        await deletePlanItem(db, item.id)
      }),
    [db, slide],
  )

  const move = useCallback<PlanActions['move']>(
    (item, direction) =>
      sliding(slide, () =>
        db.transaction('rw', db.planItems, async () => {
          const items = await planItemsOn(db, item.weekday, sideOf(item))
          const from = items.findIndex((p) => p.id === item.id)
          const moved = items[from]
          const neighbour = items[from + direction]
          if (moved == null || neighbour == null) return
          items[from + direction] = moved
          items[from] = neighbour
          // Renumbered rather than swapped, so two items that share an order still change places.
          await Promise.all(items.map((p, order) => (p.order === order ? null : db.planItems.update(p.id, { order }))))
        }),
      ),
    [db, slide],
  )

  const addCustomExercise = useCallback<PlanActions['addCustomExercise']>((exercise) => createCustomExercise(db, exercise), [db])

  const setBaseline = useCallback<PlanActions['setBaseline']>(
    async (exerciseId, text) => {
      const e1rmKg = grit.parseBaselineKg(text)
      if (e1rmKg == null) return
      if (exerciseId !== 0) {
        await db.baselines.put({ exerciseId, e1rmKg, date: today() })
        return
      }
      // The catalogue's strict press: its row and its best land together or not at all.
      await db.transaction('rw', db.exercises, db.baselines, async () => {
        const id = await resolveCatalogue(db, catalogueStrictPress().name)
        await db.baselines.put({ exerciseId: id, e1rmKg, date: today() })
      })
    },
    [db, today],
  )

  const state = useMemo(() => (stored ? build(stored) : EMPTY), [stored])
  return { loading: stored == null, ...state, draft, add, addOnce, save, remove, move, addCustomExercise, setBaseline }
}
