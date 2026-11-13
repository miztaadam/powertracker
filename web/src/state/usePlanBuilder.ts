import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { replacePlan, planFor, type NewPlanItem } from '../db/catalogue'
import { getSettings, loadHistory, updateSettings, type GritDb } from '../db/db'
import { planItemToJs } from '../db/mapping'
import type { ExerciseRow, TrainingMode } from '../db/schema'
import { grit, JsBuilderPick, type JsLibraryExercise, type JsPlanDraft } from '../domain'
import { catalogueEntry, library, ownEntry, recentExerciseIds, type PickerEntry } from './exercisePicker'

/** The builder's four steps, in order (bodybuilding mode stage 2). */
export const BUILDER_STEPS = ['days', 'exercises', 'bodyParts', 'draft'] as const
export type BuilderStep = (typeof BUILDER_STEPS)[number]

/** How many days a week the builder offers: 3 (full body) or 4 (upper and lower twice). */
export const DAY_COUNTS = [3, 4] as const
export type DayCount = (typeof DAY_COUNTS)[number]

type StoredExercise = ExerciseRow & { id: number }

/** What the builder reads once, when it opens. Nothing is written until "Use this plan". */
type Loaded = {
  side: TrainingMode
  exercises: StoredExercise[]
  recentIds: number[]
  /** The exercises to pre-tick: in either side's plan, or logged in the last 28 days. */
  suggested: PickerEntry[]
  /** The bodybuilding side's training days as stored. */
  trainingDays: number[]
}

/** The key a ticked exercise is kept by: its catalogue name, or its row for one of the lifter's own. */
export const pickKey = (entry: PickerEntry): string => entry.library?.name ?? `own-${entry.exerciseId}`

/** A ticked exercise as the builder takes it. The lifter's own has no equipment, and is compound unless it is an accessory. */
export function builderPick(entry: PickerEntry): JsBuilderPick {
  return new JsBuilderPick(entry.name, [...entry.muscles], entry.kind === 'COMPOUND', [...(entry.library?.equipment ?? [])], entry.library != null)
}

/** The picker entries for stored exercise ids, as the picker would list them: by catalogue name where it has one. */
function entriesFor(ids: readonly number[], exercises: readonly StoredExercise[]): PickerEntry[] {
  const byId = new Map(exercises.map((e) => [e.id, e]))
  const byName = new Map(library().map((e) => [e.name.toLowerCase(), e]))
  const seen = new Set<string>()
  return ids.flatMap((id) => {
    const row = byId.get(id)
    if (row == null || row.archived) return []
    const known = byName.get(row.name.toLowerCase())
    const entry = known ? catalogueEntry(known, row.id) : ownEntry(row)
    if (seen.has(pickKey(entry))) return []
    seen.add(pickKey(entry))
    return [entry]
  })
}

async function load(db: GritDb, today: string): Promise<Loaded> {
  const [settings, history, exercises, sessions, sets, powerlifting, bodybuilding] = await Promise.all([
    getSettings(db),
    loadHistory(db),
    db.exercises.toArray() as Promise<StoredExercise[]>,
    db.sessions.toArray(),
    db.sets.orderBy('id').toArray(),
    planFor(db, 'POWERLIFTING'),
    planFor(db, 'BODYBUILDING'),
  ])
  const ids = grit.planBuilderSuggestedPicks(history, [...powerlifting, ...bodybuilding].map(planItemToJs), today)
  const recentIds = recentExerciseIds(sessions.map((s) => ({ id: s.id as number, date: s.date, sets: sets.filter((set) => set.sessionId === s.id) })))
  return { side: settings.trainingMode, exercises, recentIds, suggested: entriesFor(ids, exercises), trainingDays: settings.bodybuildingTrainingDays }
}

export type PlanBuilderState = {
  loading: boolean
  /** The side the app is on. The builder is only for the bodybuilding side. */
  side: TrainingMode
  step: BuilderStep
  /** 3 unless the side's stored days are 4. */
  dayCount: DayCount
  /** The chosen weekdays, 1 = Monday, in week order. */
  days: number[]
  /** The stored exercises not archived, for the browser. */
  exercises: StoredExercise[]
  recentIds: number[]
  /** The ticked exercises, by `pickKey`, in the order they were ticked (pre-ticks first). */
  ticked: ReadonlyMap<string, PickerEntry>
  /** The body parts to improve (target-group `MuscleGroup` names), in the order chosen. */
  priorities: string[]
  /** True after a tick past the most body parts was refused, until the next change. */
  prioritiesFull: boolean
  /** The draft, on the last step. */
  draft: JsPlanDraft | null
  /** Whether Next (or Use this plan) can be tapped on this step. */
  canGoOn: boolean
}

export type PlanBuilderActions = {
  chooseDayCount: (count: DayCount) => void
  toggleDay: (weekday: number) => void
  toggle: (entry: PickerEntry) => void
  togglePriority: (group: string) => void
  /** The next step; from the body parts, the first draft. */
  next: () => void
  /** The step before. False on the first step, which has none (and no Back). */
  back: () => boolean
  /** The next draft for the same answers: only the catalogue's defaults change. */
  anotherDraft: () => void
  /** What `name` on `day` could be swapped for, none already on that day. */
  alternatives: (day: number, name: string) => JsLibraryExercise[]
  swap: (day: number, from: string, to: string) => void
  /** Whether the bodybuilding side has a plan now, as stored: "Use this plan" asks first when it has. */
  hasPlan: () => Promise<boolean>
  /** Saves the draft as the bodybuilding side's plan and its days as that side's training days, in one transaction. */
  save: () => Promise<void>
}

/** The items a draft saves as: in session order, each exercise in its place; the lifter's own by their rows. */
export function draftItems(draft: JsPlanDraft, picks: Iterable<PickerEntry>): NewPlanItem[] {
  const own = new Map<string, number>()
  for (const p of picks) if (p.library == null && p.exerciseId != null) own.set(p.name.toLowerCase(), p.exerciseId)
  return draft.sessions.flatMap((session) =>
    session.exercises.map((e) => {
      const item = { weekday: session.day, sets: e.sets, reps: e.repsLow, repsHigh: e.repsHigh, targetRpe: e.rpe }
      if (e.inCatalogue) return { item: { ...item, exerciseId: null }, catalogueName: e.name }
      const exerciseId = own.get(e.name.toLowerCase())
      if (exerciseId == null) throw new Error(`${e.name} is not one of the ticked exercises`)
      return { item: { ...item, exerciseId }, catalogueName: null }
    }),
  )
}

/**
 * The plan builder's answers and draft, kept in memory while the builder is open. It reads the database once when it
 * opens and writes only on `save`: leaving at any step changes nothing. `today` is the real clock.
 */
export function usePlanBuilder(db: GritDb, today: () => string): PlanBuilderState & PlanBuilderActions {
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [readError, setReadError] = useState<unknown>(null)
  const [step, setStep] = useState<BuilderStep>('days')
  const [dayCount, setDayCount] = useState<DayCount>(3)
  const [days, setDays] = useState<number[]>([])
  const [ticked, setTicked] = useState<ReadonlyMap<string, PickerEntry>>(new Map())
  const [priorities, setPriorities] = useState<string[]>([])
  const [prioritiesFull, setPrioritiesFull] = useState(false)
  const [variant, setVariant] = useState(0)
  const [draft, setDraft] = useState<JsPlanDraft | null>(null)
  const clock = useRef(today)
  clock.current = today

  useEffect(() => {
    let stale = false
    load(db, clock.current())
      .then((data) => {
        if (stale) return
        setLoaded(data)
        // The answers start from what is stored: the side's days when there are 3 or 4 (otherwise 3 days, none
        // ticked), the suggested exercises ticked.
        const stored = [...new Set(data.trainingDays)].sort((a, b) => a - b)
        if (stored.length === 3 || stored.length === 4) {
          setDayCount(stored.length)
          setDays(stored)
        }
        setTicked(new Map(data.suggested.map((e) => [pickKey(e), e])))
      })
      .catch((error: unknown) => {
        if (!stale) setReadError(error)
      })
    return () => {
      stale = true
    }
  }, [db])
  const picks = useMemo(() => [...ticked.values()].map(builderPick), [ticked])

  const build = useCallback((v: number) => setDraft(grit.planBuilderBuild(days, picks, priorities, v)), [days, picks, priorities])

  const chooseDayCount = useCallback((count: DayCount) => setDayCount(count), [])
  const toggleDay = useCallback((weekday: number) => setDays((ds) => (ds.includes(weekday) ? ds.filter((d) => d !== weekday) : [...ds, weekday].sort((a, b) => a - b))), [])
  const toggle = useCallback(
    (entry: PickerEntry) =>
      setTicked((current) => {
        const next = new Map(current)
        const key = pickKey(entry)
        if (next.has(key)) next.delete(key)
        else next.set(key, entry)
        return next
      }),
    [],
  )
  const togglePriority = useCallback(
    (group: string) => {
      if (priorities.includes(group)) {
        setPriorities(priorities.filter((g) => g !== group))
        setPrioritiesFull(false)
      } else if (priorities.length >= grit.planBuilderMaxPriorities()) {
        setPrioritiesFull(true)
      } else {
        setPriorities([...priorities, group])
        setPrioritiesFull(false)
      }
    },
    [priorities],
  )

  const canGoOn = step === 'days' ? days.length === dayCount : true

  const next = useCallback(() => {
    if (!canGoOn) return
    if (step === 'days') setStep('exercises')
    else if (step === 'exercises') setStep('bodyParts')
    else if (step === 'bodyParts') {
      setVariant(0)
      build(0)
      setStep('draft')
    }
  }, [canGoOn, step, build])

  const back = useCallback(() => {
    const i = BUILDER_STEPS.indexOf(step)
    if (i === 0) return false
    setPrioritiesFull(false)
    setStep(BUILDER_STEPS[i - 1]!)
    return true
  }, [step])

  const anotherDraft = useCallback(() => {
    setVariant(variant + 1)
    build(variant + 1)
  }, [variant, build])

  const alternatives = useCallback((day: number, name: string) => (draft ? grit.planBuilderAlternatives(name, draft, picks, day) : []), [draft, picks])
  const swap = useCallback((day: number, from: string, to: string) => setDraft((d) => (d ? grit.planBuilderSwap(d, day, from, to) : d)), [])

  const hasPlan = useCallback(async () => (await planFor(db, 'BODYBUILDING')).length > 0, [db])

  const save = useCallback(async () => {
    if (draft == null) return
    const items = draftItems(draft, ticked.values())
    const chosen = draft.sessions.map((s) => s.day)
    // One transaction: the plan and its days stand or fall together.
    await db.transaction('rw', db.exercises, db.planItems, db.settings, async () => {
      await replacePlan(db, 'BODYBUILDING', items)
      await updateSettings(db, (s) => ({ ...s, bodybuildingTrainingDays: [...chosen].sort((a, b) => a - b) }))
    })
  }, [db, draft, ticked])

  const exercises = useMemo(() => (loaded?.exercises ?? []).filter((e) => !e.archived), [loaded])
  // A failed read is for the app's error boundary, not for a builder that looks as if nothing was stored.
  if (readError != null) throw readError

  return {
    loading: loaded == null,
    side: loaded?.side ?? 'POWERLIFTING',
    step,
    dayCount,
    days,
    exercises,
    recentIds: loaded?.recentIds ?? [],
    ticked,
    priorities,
    prioritiesFull,
    draft,
    canGoOn,
    chooseDayCount,
    toggleDay,
    toggle,
    togglePriority,
    next,
    back,
    anotherDraft,
    alternatives,
    swap,
    hasPlan,
    save,
  }
}

