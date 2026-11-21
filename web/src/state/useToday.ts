import Dexie, { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { addSet, planFor, setPlateCount as storePlateCount } from '../db/catalogue'
import { checkInToJs, loadCheckIns } from '../db/checkIns'
import { getSettings, loadHistory, type AppSettings, type GritDb } from '../db/db'
import { adviceFromJs, adviceToJs, planItemToJs } from '../db/mapping'
import { LOAD_UNITS, setUnit, sideOf, type AdviceRow, type LoadUnit, type PlanItemRow, type PlateCount, type SetRow, type SkipReason, type TrainingMode } from '../db/schema'
import { skip as storeSkip, skips as readSkips, skipToJs, unskip, type StoredSkip } from '../db/skips'
import { haptic, hapticsPossible } from '../platform/haptics'
import { slideNextChange, withTransition } from '../ui/viewTransition'
import { grit, JsCoachSettings, JsExercise, JsSet, type JsHistory, type JsLibraryExercise, type JsSkip } from '../domain'
import { createCustomExercise, library, recentExerciseIds, type CustomExercise } from './exercisePicker'

/**
 * The numbers in the steppers for the next set. `weightKg` is null until the lifter enters one; it is a plate number when
 * `unit` is PLATES (1.17). `unit` is there only for plates, so a kg editor is exactly as it was. An editor kept in
 * another unit than its exercise's is never shown.
 */
export type SetEditor = { weightKg: number | null; reps: number; rpe: number; unit?: LoadUnit }

export type StoredSet = SetRow & { id: number }
export type PlannedItem = PlanItemRow & { id: number }

export type MissPoint = 'FLOOR' | 'MID' | 'LOCKOUT'

/**
 * The coach's numbers for one exercise today; `sets` is set only when the coach changes the set count. `repsHigh`
 * is the top of a rep range (a bodybuilding target, `reps` its bottom); null for fixed reps. `unit` is there only for a
 * plate target (1.17): `weightKg` is then a plate number.
 */
export type Target = { exerciseId: number; weightKg: number; reps: number; rpe: number; sets: number | null; repsHigh?: number | null; unit?: LoadUnit }

/**
 * An OPEN target for today. Changes nothing until applied. `applyReps` is what Apply puts in the reps field: the
 * target's reps, or on the bodybuilding side the reps to beat for the set about to be logged (spec §5.3).
 */
export type Suggestion = { id: number; reason: string; target: Target; applyReps?: number }

export type TodayItem = {
  exercise: JsExercise
  /** Today's plan item, or null for an exercise added during the session. */
  planned: PlannedItem | null
  suggestion: Suggestion | null
  sets: StoredSet[]
  editor: SetEditor
  /** Sets to do: the plan's, or an applied coach target's. Null for an added exercise. */
  targetSets: number | null
  isComplete: boolean
  /** Plates for the editor weight; null when there is no weight or it cannot be loaded exactly. */
  perSide: number[] | null
  /** The weight a typo is judged against: the last set today, else the last top set before today, both in `unit`. */
  lastWeightKg: number | null
  /**
   * 1.17, each there only when it is not the default: the exercise's current unit (PLATES; absent is kg), which the
   * editor, the targets and the typo check are in; its stored plate setting (WHOLE or HALF; absent is OFF), which the
   * plate sheet marks; and whether it may count plates at all (true; absent is false), which puts the item in its menu.
   */
  unit?: LoadUnit
  plateCount?: PlateCount
  plateOffered?: boolean
}

/**
 * What the lifter did, as the editor has it. Never a top set: that is worked out from the session afterwards. `unit`
 * (1.17) is the field's it was typed in, there only for plates.
 */
export type SetInput = { weightKg: number; reps: number; rpe: number | null; failed: boolean; unit?: LoadUnit }

export type InputWarning = 'HIGH_REPS' | 'BIG_JUMP'

/** A set held back because it looks like a typo; saved only after the lifter confirms. */
export type PendingLog = { exerciseId: number; set: SetInput; warnings: InputWarning[]; lastWeightKg: number | null }

/** How adding an exercise to today's session from outside the Today screen went. */
export type TodayAdd =
  | 'ADDED'
  /** The exercise is already in today's session; nothing changed. */
  | 'ALREADY_THERE'
  /** There is no open, unfinished session to add to (or no such exercise); nothing changed. */
  | 'REFUSED'

/** `skipped` (1.13) is how many exercises were skipped that day on the session's side; there only when 1 or more. */
export type FinishSummary = { setCount: number; exerciseCount: number; newNotes: number; skipped?: number }

/** A planned exercise skipped today on the side shown (1.13), for the "Skipped today" line and its Bring back. */
export type SkippedToday = { exerciseId: number; name: string; skipId: number }

/** A skip just stored: its id, for Undo, and the exercise's name, for the snackbar. */
export type SkipDone = { skipId: number; name: string }

export type TodayState = {
  /** The day this screen logs to, `YYYY-MM-DD`. */
  date: string
  loading: boolean
  sessionId: number | null
  items: TodayItem[]
  pending: PendingLog | null
  summary: FinishSummary | null
  /** The lifter's exercises that are not archived, for the picker. */
  exercises: JsExercise[]
  /** Exercise ids, most recently logged first, for the picker's "Your exercises". */
  recentIds: number[]
  /**
   * A catalogue exercise added before it has a row is shown under a stand-in id (a negative number,
   * never stored). This maps each stand-in to the row's id once the row exists, so the screen can
   * follow the card across.
   */
  resolved: Record<number, number>
  weightStepKg: number
  changePlates: boolean
  /** How many times today's targets have been worked out and stored (on open, again when the plan changes). */
  coachPasses: number
  /**
   * The side this screen shows: the side of the day's session when there is one (it keeps its own plan and
   * targets after a switch), otherwise the side the app is on (spec §5.1).
   */
  side: TrainingMode
  /** The side the app is on now, from Settings. */
  activeSide: TrainingMode
  /** Today's planned exercises skipped on the side shown, in plan order (1.13); they are not in `items`. */
  skippedToday: SkippedToday[]
}

export type TodayActions = {
  /** Starts the session for the clock's day; if that is no longer the day on screen, the screen moves to it. */
  startSession: (bodyweightKg: number | null, readiness: number | null) => Promise<void>
  /** Changes the numbers for the next set of an exercise. No set is saved. */
  edit: (exerciseId: number, editor: SetEditor) => void
  /**
   * Saves `set` for the item with this key (its exercise id). Unless `confirmed`, a set that looks
   * like a typo is held in `pending` instead. Rejects when the write fails or there is no session.
   * The set goes into the open session also after midnight: a session belongs to the day it was started.
   */
  logSet: (itemKey: number, set: SetInput, confirmed?: boolean) => Promise<void>
  /** Saves the held set as it is. */
  confirmPending: () => Promise<void>
  /** Back to the editor without saving the held set. */
  cancelPending: () => void
  /** Records where the rep was hardest or missed; null clears it. */
  setMissPoint: (setId: number, point: MissPoint | null) => Promise<void>
  deleteSet: (setId: number) => Promise<void>
  /** Puts the coach's numbers into the editor and marks the advice applied. */
  applyTarget: (adviceId: number) => Promise<void>
  dismissTarget: (adviceId: number) => Promise<void>
  /** Adds an exercise for this session only; it is kept once a set is logged for it. */
  addExercise: (exerciseId: number) => void
  /**
   * Adds a catalogue exercise for this session and returns the id its card is shown under: its row's
   * id when it has one, otherwise a stand-in. Nothing is stored until a set is logged for it: the
   * row is saved with that set. Null for a name the catalogue does not know.
   */
  addFromCatalogue: (name: string) => number | null
  /**
   * Makes an exercise of the lifter's own, adds it to this session and resolves to the id its card
   * is shown under. A catalogue name is added from the catalogue instead of being made again.
   * `onAdded`, when given, runs with that id in the same step as the add, which slides in (stage 3): once the new
   * exercise is on screen, so its card can open as it arrives. `onMade`, when given, runs as soon as the exercise is
   * made (or found in the catalogue), before that step; if making it fails, neither runs and the promise rejects.
   */
  createExercise: (exercise: CustomExercise, onAdded?: (id: number | null) => void, onMade?: () => void) => Promise<number | null>
  /**
   * Adds an exercise from another screen (the Exercises tab) the way "Add exercise" here does: a
   * catalogue exercise by `catalogueName`, with no row until its first set; the lifter's own by
   * `exerciseId`. The session is looked at when this is called, not when the button was drawn: with
   * no session, or one finished meanwhile, nothing is added. Resolves to null for a call made while
   * another is still at work (a double tap), which does nothing. Rejects when the session cannot be read.
   */
  addToOpenSession: (catalogueName: string | null, exerciseId: number | null) => Promise<TodayAdd | null>
  /** Takes back an added exercise that has no sets yet. */
  removeExercise: (exerciseId: number) => void
  /**
   * Stores the plate setting of the item with this key (1.17). A stand-in key is a catalogue exercise with no row yet: its
   * row is made in the same write, as its first set would make it. Needs no session. The numbers kept for its next set,
   * under its id and its stand-in's, are dropped, so the field starts afresh; no stored set or target changes. The coach
   * then works out today's targets again. Rejects when the write fails. Optional: the guide's Today has none, so it
   * offers no plate item.
   */
  setPlateCount?: (itemKey: number, value: PlateCount) => Promise<void>
  /**
   * Skips `exerciseId`, planned today with no sets logged, for today on the side shown, with an optional reason (null is
   * "Just skip"). The skip and the coach's card for it, if any, are stored together, and it ticks once. Skipping needs no
   * session; skipping one already skipped today replaces its reason and its card. Resolves to null when nothing was
   * stored (sets logged, not planned today, or another skip still being stored: a double tap). Rejects when the write fails.
   */
  skip: (exerciseId: number, planItemId: number | null, reason: SkipReason | null) => Promise<SkipDone | null>
  /** Undo on the snackbar: takes back the skip `skipId` and its still-open card. An answered card stays. */
  undoSkip: (skipId: number) => Promise<void>
  /** Bring back on the "Skipped today" line: takes back today's skip of `exerciseId` on the side shown. */
  bringBack: (exerciseId: number) => Promise<void>
  /** Runs the coach over the history including today and stores what is new. */
  finishSession: () => Promise<void>
  /** Back to logging after the summary; if midnight has passed meanwhile, on to the new day. */
  dismissSummary: () => void
  /**
   * Reads the clock again, as coming back to the front does: on to the clock's day, unless a finish
   * summary is up or an unfinished session keeps the screen.
   */
  refreshDay: () => void
}

const DEFAULT_REPS = 5
const SHOWN_TIMEOUT_MS = 2000

type Stored = {
  date: string
  sessionId: number | null
  sets: StoredSet[]
  history: JsHistory
  plan: PlannedItem[]
  settings: AppSettings
  targets: (Omit<Suggestion, 'applyReps'> & { status: string })[]
  side: TrainingMode
  /** Every stored skip, both sides': the pain hold crosses sides. */
  skips: StoredSkip[]
}

/** What only this screen knows: edited numbers, exercises added but not yet logged, open prompts. */
type Local = {
  date: string
  /** False until the editor values saved for this day have been read back. */
  restored: boolean
  editors: Record<number, SetEditor>
  added: number[]
  pending: PendingLog | null
  summary: FinishSummary | null
}

const emptyLocal = (date: string): Local => ({ date, restored: false, editors: {}, added: [], pending: null, summary: null })

const editorKey = (date: string, exerciseId: number) => `editor:${date}:${exerciseId}`

/** 1 = Monday .. 7 = Sunday for an ISO date. */
export function weekdayOf(isoDate: string): number {
  return new Date(`${isoDate}T00:00:00Z`).getUTCDay() || 7
}

function parseTarget(payloadJson: string): Target | null {
  const p = grit.advicePayload(payloadJson)
  if (p?.kind !== 'Target' || p.exerciseId == null || p.weightKg == null || p.reps == null || p.rpe == null) return null
  return { exerciseId: p.exerciseId, weightKg: p.weightKg, reps: p.reps, rpe: p.rpe, sets: p.sets ?? null, repsHigh: p.repsHigh ?? null, ...(p.unit === 'PLATES' ? { unit: 'PLATES' as const } : {}) }
}

/** `{ unit: 'PLATES' }` for plates, else nothing: a kg editor, target, set input or row never carries the key (1.17). */
const unitKey = (unit: LoadUnit): { unit?: LoadUnit } => (unit === 'PLATES' ? { unit } : {})

/** Today holds at most one target for each exercise and unit (1.17): a kg target does not stand in for the plate one. */
const targetKey = (t: Target) => `${t.exerciseId}:${setUnit(t)}`

/** The TARGET advice stored for `date` on `side`, in every status, oldest first. Each side has its own targets. */
function todaysTargets(advice: AdviceRow[], date: string, side: TrainingMode): Stored['targets'] {
  return advice.flatMap((a) => {
    if (a.type !== 'TARGET' || a.createdAt !== date || a.id == null || sideOf(a) !== side) return []
    const target = parseTarget(a.payloadJson)
    return target ? [{ id: a.id, reason: a.reason, status: a.status, target }] : []
  })
}

function isEditor(value: unknown): value is SetEditor {
  if (typeof value !== 'object' || value === null) return false
  const v = value as Record<string, unknown>
  return (
    (v.weightKg === null || typeof v.weightKg === 'number') &&
    typeof v.reps === 'number' &&
    typeof v.rpe === 'number' &&
    (v.unit === undefined || (LOAD_UNITS as readonly unknown[]).includes(v.unit))
  )
}

/** The side Today follows for `date`: that day's session's side when it has one, else the side the app is on. */
async function sideFor(db: GritDb, date: string, settings: AppSettings): Promise<TrainingMode> {
  const session = await db.sessions.where('date').equals(date).first()
  return session ? sideOf(session) : settings.trainingMode
}

async function readStored(db: GritDb, date: string): Promise<Stored> {
  const [session, history, settings, advice, skips] = await Promise.all([db.sessions.where('date').equals(date).first(), loadHistory(db), getSettings(db), db.advice.toArray(), readSkips(db)])
  const side = session ? sideOf(session) : settings.trainingMode
  const plan = await planFor(db, side)
  const sets = session?.id != null ? await db.sets.where('sessionId').equals(session.id).sortBy('id') : []
  return {
    date,
    sessionId: session?.id ?? null,
    sets: sets as StoredSet[],
    history,
    plan: plan as PlannedItem[],
    settings,
    targets: todaysTargets(advice, date, side),
    side,
    skips,
  }
}

async function readEditors(db: GritDb, date: string): Promise<Record<number, SetEditor>> {
  const prefix = editorKey(date, 0).slice(0, -1)
  const rows = await db.meta.where('key').startsWith(prefix).toArray()
  const editors: Record<number, SetEditor> = {}
  for (const row of rows) {
    const exerciseId = Number(row.key.slice(prefix.length))
    if (Number.isInteger(exerciseId) && isEditor(row.value)) editors[exerciseId] = row.value
  }
  return editors
}

const coachSettings = (s: AppSettings) => new JsCoachSettings(s.defaultRpe, s.changePlates)

const withoutId = (row: AdviceRow): AdviceRow => {
  const { id: _id, ...rest } = row
  return rest
}

/** Thrown by `coachPass` when storing its results failed. The reads worked: the log itself is fine. */
class CoachWriteError extends Error {
  constructor(cause: unknown) {
    super("The coach could not store today's targets.", { cause })
    this.name = 'CoachWriteError'
  }
}

/**
 * Works out today's targets and stores the ones not stored yet, one per exercise. Returns false and
 * writes nothing when `date` is no longer today: a screen left open past midnight must not store
 * targets dated the day before. Rejects with `CoachWriteError` when the reads worked but storing
 * failed (a full phone); nothing is half-written then.
 */
async function coachPass(db: GritDb, date: string, today: () => string): Promise<boolean> {
  if (today() !== date) return false
  let writing = false
  try {
    // One transaction, so two passes (two tabs, a remount) cannot both find the target missing.
    await db.transaction('rw', [db.sessions, db.sets, db.exercises, db.baselines, db.planItems, db.settings, db.advice, db.meta, db.skips], async () => {
      const [history, settings, advice, skips] = await Promise.all([loadHistory(db), getSettings(db), db.advice.toArray(), readSkips(db)])
      const side = await sideFor(db, date, settings)
      const plan = await planFor(db, side)
      // Each unit has its own target (1.17): one stored in kg does not stand in for the plate one, nor the other way.
      const known = new Set(todaysTargets(advice, date, side).map((t) => targetKey(t.target)))
      // The whole advice list: the domain counts each side's own, and every deload and easing back in.
      const prior = advice.map((a) => adviceToJs(a as AdviceRow & { id: number }))
      const fresh: AdviceRow[] = []
      // Every skip, both sides': a pain skip on either holds the exercise's weight (1.13).
      const held = skips.map(skipToJs)
      for (const a of grit.coachingTargetsForWithSkips(side, weekdayOf(date), history, (plan as PlannedItem[]).map(planItemToJs), coachSettings(settings), date, prior, held)) {
        const target = parseTarget(a.payloadJson)
        if (target == null || known.has(targetKey(target))) continue
        known.add(targetKey(target))
        fresh.push(withoutId(adviceFromJs(a)))
      }
      writing = true
      if (fresh.length > 0) await db.advice.bulkAdd(fresh)
      // Editor values are kept for a reload during the session; earlier days' are of no use any more.
      await db.meta
        .where('key')
        .startsWith('editor:')
        .filter((row) => !row.key.startsWith(`editor:${date}:`))
        .delete()
    })
  } catch (e) {
    throw writing ? new CoachWriteError(e) : e
  }
  return true
}

/** Each plate-counting exercise's [id, setting], for the coach pass's key (1.17). */
const plateSettings = (history: JsHistory) => history.exercises.filter((e) => e.plateCount !== 'OFF').map((e) => [e.id, e.plateCount])

// The domain reads the whole history for each answer, and the screen rebuilds on every stepper tap:
// answers are kept for as long as the history object they were worked out from.
const topSets = new WeakMap<JsHistory, Map<string, JsSet | null>>()

/**
 * The buzz for the set just saved as [setId]: `best` when it is a new record of its lift, asked of the history without
 * it, else `tick`, and `tick` too when the history cannot be read.
 */
export async function buzzForSet(db: GritDb, setId: number, row: Parameters<typeof isNewBest>[1], date: string, side: TrainingMode): Promise<void> {
  let best = false
  try {
    best = row.exerciseId != null && isNewBest(await loadHistory(db, setId), row, date, side)
  } catch {
    // Fall back to the tick: the set is saved either way.
  }
  haptic(best ? 'best' : 'tick')
}

/** Whether [row], about to be saved on [date] in a [side] session, is a new record of its lift (ProgressMath.isNewPr). */
export function isNewBest(
  history: JsHistory,
  row: { exerciseId: number | null; weightKg: number; reps: number; rpe: number | null; failed: boolean; unit?: LoadUnit },
  date: string,
  side: TrainingMode,
): boolean {
  // A record is only ever a kg set: a plate number is never judged a best (1.17).
  if (row.exerciseId == null || setUnit(row) !== 'KG') return false
  return grit.isNewPr(history, new JsSet(row.exerciseId, row.weightKg, row.reps, row.rpe, row.failed, null, false, 'KG'), date, side)
}

/** The top set of the latest session before `before` that has this exercise, as the coach picks it. */
function lastTopSet(history: JsHistory, exerciseId: number, before: string): JsSet | null {
  let known = topSets.get(history)
  if (!known) topSets.set(history, (known = new Map()))
  const key = `${exerciseId}:${before}`
  let top = known.get(key)
  if (top === undefined) known.set(key, (top = grit.lastTopSet(history, exerciseId, before) ?? null))
  return top
}

/** The id a catalogue exercise is shown under while it has no row: negative, so it can never be a row's. */
const standInId = (entry: JsLibraryExercise) => -(library().findIndex((e) => e.name === entry.name) + 1)

/** The catalogue entry behind a stand-in id; null for a row's id. */
const standInEntry = (id: number): JsLibraryExercise | null => (id < 0 ? (library()[-id - 1] ?? null) : null)

const sameName = (a: string, b: string) => a.toLowerCase() === b.toLowerCase()

/** The id a catalogue exercise's card is shown under: its row's when it has one, otherwise a stand-in. Null for a name the catalogue does not know. */
function catalogueId(name: string, exercises: readonly JsExercise[]): number | null {
  const entry = grit.findInLibrary(name)
  if (!entry) return null
  return exercises.find((e) => sameName(e.name, entry.name))?.id ?? standInId(entry)
}

function build(d: Stored, l: Local): Pick<TodayState, 'sessionId' | 'items' | 'exercises' | 'recentIds' | 'resolved' | 'weightStepKg' | 'changePlates' | 'side' | 'activeSide' | 'skippedToday'> {
  const exercises = new Map(d.history.exercises.map((e) => [e.id, e]))
  // Oldest first, like resolveCatalogue: with two rows of one name, the one shown is the one saved to.
  const live = d.history.exercises.filter((e) => !e.archived).sort((a, b) => a.id - b.id)
  // A catalogue exercise added without a row gets one with its first set (or from the Plan tab meanwhile).
  const resolved: Record<number, number> = {}
  const standInOf = new Map<number, number>()
  for (const standIn of l.added) {
    const name = standInEntry(standIn)?.name
    const row = name != null ? live.find((e) => sameName(e.name, name)) : undefined
    if (row) {
      resolved[standIn] = row.id
      standInOf.set(row.id, standIn)
    }
  }
  const added = l.added.map((id) => resolved[id] ?? id)
  // One card per exercise: sets are stored per exercise, so a second plan line for the same one merges into the first.
  const allPlanned: PlannedItem[] = []
  for (const p of d.plan.filter((p) => p.weekday === weekdayOf(d.date)).sort((a, b) => a.order - b.order)) {
    if (!allPlanned.some((q) => q.exerciseId === p.exerciseId)) allPlanned.push(p)
  }
  const loggedIds = d.sets.map((s) => s.exerciseId)
  // A planned exercise skipped today on this side leaves the list (1.13), unless sets were logged for it after all.
  const skippedIds = new Map(d.skips.filter((s) => s.date === d.date && s.mode === d.side).map((s) => [s.exerciseId, s]))
  const planned = allPlanned.filter((p) => !skippedIds.has(p.exerciseId) || loggedIds.includes(p.exerciseId))
  const plannedIds = new Set(planned.map((p) => p.exerciseId))
  const jsSkips: JsSkip[] | null = d.side === 'BODYBUILDING' ? d.skips.map(skipToJs) : null
  const addedIds = [...new Set([...loggedIds.filter((id) => !added.includes(id)), ...added])].filter((id) => !plannedIds.has(id))

  function item(exercise: JsExercise, plan: PlannedItem | null): TodayItem {
    // Each exercise is read in its current unit only (1.17): a kg number never fills the PLATE field, nor the other way.
    const unit = grit.loadUnitOf(exercise) as LoadUnit
    const offered = grit.plateCountOffered(exercise)
    const plateCount = exercise.plateCount as PlateCount
    const sets = d.sets.filter((s) => s.exerciseId === exercise.id)
    // A stored target in the other unit is neither shown nor applied.
    const mine = d.targets.filter((t) => t.target.exerciseId === exercise.id && setUnit(t.target) === unit)
    const applied = mine.filter((t) => t.status === 'APPLIED').pop()?.target
    // The domain's last top set is read in the exercise's current unit (`inCurrentUnits`).
    const top = lastTopSet(d.history, exercise.id, d.date)
    const lastToday = sets.filter((s) => setUnit(s) === unit).at(-1)
    const rpe = plan?.targetRpe ?? d.settings.defaultRpe
    // A kept editor in another unit (from before the switch, another tab, or kept across a reload) is never shown.
    const kept = [l.editors[exercise.id], l.editors[standInOf.get(exercise.id) ?? exercise.id]].find((e) => e != null && setUnit(e) === unit)
    const editor: SetEditor =
      kept ??
      (lastToday != null
        ? { weightKg: lastToday.weightKg, reps: lastToday.reps, rpe: lastToday.rpe ?? rpe, ...unitKey(unit) }
        : applied != null
          ? { weightKg: applied.weightKg, reps: applied.reps, rpe: applied.rpe, ...unitKey(unit) }
          : { weightKg: top?.weightKg ?? null, reps: plan?.reps ?? top?.reps ?? DEFAULT_REPS, rpe, ...unitKey(unit) })
    const targetSets = plan ? (applied?.sets ?? plan.sets) : null
    const open = mine.find((t) => t.status === 'OPEN')
    // On the bodybuilding side Apply fills in the reps to beat for the set about to be logged. Under a pain hold the aim is
    // not reset to the low end of the range, so the skips go too.
    const applyReps = open && plan && jsSkips ? grit.repsToBeatWithSkips(planItemToJs(plan), d.history, sets.length + 1, d.date, jsSkips) : (open?.target.reps ?? 0)
    return {
      exercise,
      planned: plan,
      suggestion: open ? { id: open.id, reason: open.reason, target: open.target, applyReps } : null,
      sets,
      editor,
      targetSets,
      isComplete: targetSets != null && sets.length >= targetSets,
      // A plate number has no bar and no kg plates.
      perSide: editor.weightKg != null && unit === 'KG' ? (grit.platesPerSide(editor.weightKg, d.settings.changePlates) ?? null) : null,
      lastWeightKg: lastToday?.weightKg ?? top?.weightKg ?? null,
      ...unitKey(unit),
      ...(offered && plateCount !== 'OFF' ? { plateCount } : {}),
      ...(offered ? { plateOffered: true } : {}),
    }
  }

  const items: TodayItem[] = []
  for (const p of planned) {
    const exercise = exercises.get(p.exerciseId)
    if (exercise) items.push(item(exercise, p))
  }
  for (const id of addedIds) {
    const entry = standInEntry(id)
    const exercise = exercises.get(id) ?? (entry ? new JsExercise(id, entry.name, entry.category, entry.parentLift, [...entry.muscles], true, false) : undefined)
    if (exercise) items.push(item(exercise, null))
  }
  // Only what is still planned today and not on the list: after a swap there is nothing left to bring back.
  const shownIds = new Set(items.map((i) => i.exercise.id))
  const skippedToday = allPlanned.flatMap((p): SkippedToday[] => {
    const skip = skippedIds.get(p.exerciseId)
    const exercise = exercises.get(p.exerciseId)
    return skip && exercise && !shownIds.has(p.exerciseId) ? [{ exerciseId: p.exerciseId, name: exercise.name, skipId: skip.id }] : []
  })
  return {
    sessionId: d.sessionId,
    items,
    exercises: live,
    recentIds: recentExerciseIds(d.history.sessions),
    resolved,
    weightStepKg: grit.loadStep(d.settings.changePlates),
    changePlates: d.settings.changePlates,
    side: d.side,
    activeSide: d.settings.trainingMode,
    skippedToday,
  }
}

/**
 * The calendar day on screen: the clock's day when the screen opens, read again whenever the app
 * comes back to the front, unless `holds` says an open session keeps the screen (a session belongs
 * to the day it was started, and a phone locks between sets). `refreshDay()` does what coming back
 * to the front does. `syncDay()` reads the clock now, moves the screen to that day and returns it;
 * `showDay` puts a given day on screen.
 */
function useDay(
  today: () => string,
  holds: { current: () => boolean },
): [date: string, syncDay: () => string, showDay: (date: string) => void, refreshDay: () => void] {
  const [date, setDate] = useState(today)
  const clock = useRef(today)
  clock.current = today
  const refreshDay = useCallback(() => {
    if (!holds.current()) setDate(clock.current())
  }, [holds])
  useEffect(() => {
    const refresh = () => {
      if (document.visibilityState === 'visible') refreshDay()
    }
    document.addEventListener('visibilitychange', refresh)
    window.addEventListener('pageshow', refresh)
    return () => {
      document.removeEventListener('visibilitychange', refresh)
      window.removeEventListener('pageshow', refresh)
    }
  }, [refreshDay])
  const syncDay = useCallback(() => {
    const now = clock.current()
    setDate(now)
    return now
  }, [])
  return [date, syncDay, setDate, refreshDay]
}

/** When the session of `date` was last started or logged to; kept in `meta` so a reload knows too. */
type Activity = { date: string; at: number }

export const ACTIVITY_KEY = 'today:active'

/** How long after its last set an unfinished session still keeps the screen once its day is over. */
const HOLD_MS = 6 * 60 * 60 * 1000

function recentActivity(value: unknown): Activity | null {
  if (typeof value !== 'object' || value === null) return null
  const v = value as Record<string, unknown>
  if (typeof v.date !== 'string' || typeof v.at !== 'number') return null
  return Date.now() - v.at < HOLD_MS ? { date: v.date, at: v.at } : null
}

/** Whether the stored `today:active` note says a session is being logged: started or logged to recently, and not finished. */
export function sessionInProgress(note: unknown): boolean {
  return recentActivity(note) != null
}

/**
 * Today's session: the plan for the day, the sets logged so far and the numbers for the next one.
 * Reads follow the database live. `today` is the real clock; the screen keeps the day it opened on
 * until the app comes back to the front, and an unfinished session keeps it past midnight. `onSetLogged` fires after each saved set (true for a main
 * lift or variation), so a rest timer can start.
 */
export function useToday(db: GritDb, today: () => string, onSetLogged?: (isMainOrVariation: boolean) => void): TodayState & TodayActions {
  const clock = useRef(today)
  clock.current = today
  const holds = useRef<() => boolean>(() => false)
  const [date, syncDay, showDay, refreshDay] = useDay(today, holds)

  // After a reload past midnight, come back to the session that was being logged, not to the new day's
  // start card. Nothing is read or written for the clock's day until that is settled.
  const activity = useRef<Activity | null>(null)
  const [resumed, setResumed] = useState(false)
  useEffect(() => {
    let stale = false
    void (async () => {
      try {
        const recent = recentActivity((await db.meta.get(ACTIVITY_KEY))?.value)
        const open = recent != null && recent.date < clock.current() && (await db.sessions.where('date').equals(recent.date).count()) > 0
        if (stale) return
        activity.current = recent
        if (recent && open) showDay(recent.date)
      } catch {
        // Without the note the screen simply opens on the clock's day.
      }
      if (!stale) setResumed(true)
    })()
    return () => {
      stale = true
    }
  }, [db, showDay])

  const [stored, setStored] = useState<Stored>()
  const [readError, setReadError] = useState<unknown>(null)
  // Stage 3: the read after Log set or Delete set slides the set in or out (the sets on screen are its shape). Every
  // other read lands at once.
  const [slide] = useState(() => slideNextChange<Stored>(setStored, (s) => `${s.date}:${s.sessionId}:${s.sets.map((set) => set.id).join()}`))
  useEffect(() => {
    if (!resumed) return
    const subscription = liveQuery(() => readStored(db, date)).subscribe({ next: slide.next, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db, date, resumed, slide])
  // A failed read is for the app's error boundary, not for an empty screen that looks like no data.
  if (readError != null) throw readError
  const [localState, setLocal] = useState<Local>(() => emptyLocal(date))
  const [coachPasses, setCoachPasses] = useState(0)

  // Local state belongs to one day; on a new day it starts empty.
  const local = localState.date === date ? localState : emptyLocal(date)
  const update = useCallback((fn: (l: Local) => Local) => setLocal((prev) => fn(prev.date === date ? prev : emptyLocal(date))), [date])

  // Bring back the editor values saved before a reload.
  useEffect(() => {
    if (!resumed) return
    let stale = false
    readEditors(db, date)
      .then((saved) => {
        if (!stale) update((l) => ({ ...l, restored: true, editors: { ...saved, ...l.editors } }))
      })
      // Once the screen has moved on (or the database closed under it) the failure concerns nobody.
      .catch((e: unknown) => {
        if (!stale) setReadError(e)
      })
    return () => {
      stale = true
    }
  }, [db, date, update, resumed])

  const current = stored?.date === date ? stored : undefined
  const built = useMemo(() => (current ? build(current, local) : null), [current, local])
  const state: TodayState = {
    date,
    loading: !resumed || built == null || !local.restored,
    sessionId: built?.sessionId ?? null,
    items: built?.items ?? [],
    pending: local.pending,
    summary: local.summary,
    exercises: built?.exercises ?? [],
    recentIds: built?.recentIds ?? [],
    resolved: built?.resolved ?? {},
    weightStepKg: built?.weightStepKg ?? grit.loadStep(false),
    changePlates: built?.changePlates ?? false,
    coachPasses,
    side: built?.side ?? 'POWERLIFTING',
    activeSide: built?.activeSide ?? 'POWERLIFTING',
    skippedToday: built?.skippedToday ?? [],
  }

  // Actions read the newest state and callbacks through refs, so a tap never acts on a stale screen.
  const latest = useRef(state)
  latest.current = state
  const setLogged = useRef(onSetLogged)
  setLogged.current = onSetLogged

  // An unfinished session that was started or logged to in the last few hours keeps the screen when
  // its day ends; one left open for longer does not stand in the way of the next day. A finish summary
  // keeps it until it is dismissed, so a phone that locks meanwhile does not throw it away unseen.
  holds.current = () => {
    if (state.summary != null) return true
    const recent = activity.current
    return state.sessionId != null && recent != null && recent.date === date && Date.now() - recent.at < HOLD_MS
  }
  const touch = useCallback(
    (day: string) => {
      activity.current = { date: day, at: Date.now() }
      // Only a reload after midnight needs the stored copy.
      db.meta.put({ key: ACTIVITY_KEY, value: activity.current }).catch(() => {})
    },
    [db],
  )

  // Ask the coach for today's targets when the screen opens and again if the plan changes.
  // And again when a skip is stored or taken back (1.13): a pain skip, and taking any skip back, delete that day's target,
  // which is then worked out again, with or without the hold.
  const planKey = current
    ? JSON.stringify([
        current.side,
        current.plan,
        current.settings.defaultRpe,
        current.settings.changePlates,
        current.skips.map((s) => [s.id, s.date, s.reason]),
        // 1.17: a change of an exercise's plate setting asks again (the coach then reads it in its new unit). Only the
        // exercises counting plates are in the key, so a kg-only lifter's passes are as they were.
        ...plateSettings(current.history),
      ])
    : null
  useEffect(() => {
    if (planKey == null) return
    let stale = false
    coachPass(db, date, () => clock.current())
      .then((ran) => {
        if (ran && !stale) setCoachPasses((n) => n + 1)
      })
      .catch((e: unknown) => {
        // A failed write (a full phone) costs today's targets, nothing else: the screen stays, and
        // the storage-full dialog has already been raised by the database. Only a failed read means
        // the log cannot be shown.
        if (!stale && !(e instanceof CoachWriteError)) setReadError(e)
      })
    return () => {
      stale = true
    }
  }, [db, date, planKey])

  // True while a set is being saved; further log taps are ignored until the screen shows it.
  const saving = useRef(false)
  const starting = useRef(false)
  const waiting = useRef<{ setId: number; done: () => void }[]>([])
  const loadWaiting = useRef<(() => void)[]>([])
  const closed = useRef(false)
  const knownWaiting = useRef<{ id: number; done: () => void }[]>([])
  useEffect(() => {
    const ids = new Set(state.exercises.map((e) => e.id))
    knownWaiting.current = knownWaiting.current.filter((w) => {
      if (ids.has(w.id)) w.done()
      return !ids.has(w.id)
    })
  })
  useEffect(() => {
    if (!state.loading) loadWaiting.current.splice(0).forEach((done) => done())
    const onScreen = new Set(state.items.flatMap((i) => i.sets.map((s) => s.id)))
    waiting.current = waiting.current.filter((w) => {
      if (onScreen.has(w.setId)) w.done()
      return !onScreen.has(w.setId)
    })
  })
  useEffect(() => {
    closed.current = false
    return () => {
      closed.current = true
      waiting.current.splice(0).forEach((w) => w.done())
      loadWaiting.current.splice(0).forEach((done) => done())
    }
  }, [])

  /** Waits until the screen has its data, however often it starts loading again meanwhile. False only when the screen was closed. */
  const loaded = useCallback(async () => {
    while (latest.current.loading && !closed.current) await new Promise<void>((resolve) => loadWaiting.current.push(resolve))
    return !closed.current
  }, [])

  /** Waits until the exercise row `id` is in the screen's data (or a moment has passed). */
  const known = useCallback(
    (id: number) =>
      new Promise<void>((resolve) => {
        if (latest.current.exercises.some((e) => e.id === id)) return resolve()
        const timer = setTimeout(done, SHOWN_TIMEOUT_MS)
        function done() {
          clearTimeout(timer)
          resolve()
        }
        knownWaiting.current.push({ id, done })
      }),
    [],
  )

  /** Waits until the screen's state passes `test` (or a moment has passed). */
  const stateWaiting = useRef<{ test: (s: TodayState) => boolean; done: () => void }[]>([])
  useEffect(() => {
    stateWaiting.current = stateWaiting.current.filter((w) => {
      if (w.test(latest.current)) w.done()
      return !w.test(latest.current)
    })
  })
  const until = useCallback(
    (test: (s: TodayState) => boolean) =>
      new Promise<void>((resolve) => {
        if (test(latest.current) || closed.current) return resolve()
        const timer = setTimeout(done, SHOWN_TIMEOUT_MS)
        function done() {
          clearTimeout(timer)
          resolve()
        }
        stateWaiting.current.push({ test, done })
      }),
    [],
  )

  const shown = useCallback(
    (setId: number) =>
      new Promise<void>((resolve) => {
        const timer = setTimeout(done, SHOWN_TIMEOUT_MS)
        function done() {
          clearTimeout(timer)
          resolve()
        }
        waiting.current.push({ setId, done })
      }),
    [],
  )

  const edit = useCallback(
    (exerciseId: number, editor: SetEditor) => {
      update((l) => ({ ...l, editors: { ...l.editors, [exerciseId]: editor } }))
      // A stand-in id is never stored, and its card would not survive a reload anyway.
      if (exerciseId < 0) return
      // The numbers are on screen either way; a failed write only costs them on a reload.
      db.meta.put({ key: editorKey(date, exerciseId), value: editor }).catch(() => {})
    },
    [db, date, update],
  )

  const startSession = useCallback(
    async (bodyweightKg: number | null, readiness: number | null) => {
      if (starting.current) return
      starting.current = true
      try {
        if (!(await loaded())) return
        // A session belongs to the day it is started. Started after midnight on a card opened the day
        // before, it is dated today and the screen moves to today with it.
        const day = syncDay()
        await db.transaction('rw', db.sessions, db.settings, async () => {
          const existing = await db.sessions.where('date').equals(day).first()
          // Started on the side the app is on at the moment of the tap.
          if (!existing) await db.sessions.add({ date: day, bodyweightKg, readiness, mode: (await getSettings(db)).trainingMode })
        })
        touch(day)
      } finally {
        starting.current = false
      }
    },
    [db, syncDay, loaded, touch],
  )

  const logSet = useCallback(
    async (itemKey: number, set: SetInput, confirmed = false) => {
      // A fast double tap must not save the set twice.
      if (saving.current) return
      saving.current = true
      try {
        // A set goes into the open session, also after midnight: the session belongs to the day it was started.
        // A tap is never dropped: it waits for the screen's data, and without a session it fails out loud.
        if (!(await loaded())) return
        const now = latest.current
        if (now.sessionId == null) throw new Error('There is no session to log to.')
        const item = now.items.find((i) => i.exercise.id === (now.resolved[itemKey] ?? itemKey))
        const lastWeightKg = item?.lastWeightKg ?? null
        if (!confirmed) {
          // A number typed in one unit is never stored in the other (1.17): if the exercise's setting changed between
          // the screen being drawn and the tap (another tab, a slow tap), the set is refused, not relabelled, and the
          // lifter types it again in the new field. A set held for the typo dialog keeps its own unit when confirmed.
          if (item != null && setUnit(set) !== setUnit(item)) return
          // Judged against the last weight in the same unit only, with the plate step for a plate number.
          const warnings = grit.checkLoadInput(set.weightKg, set.reps, lastWeightKg, setUnit(set), item?.plateCount ?? 'OFF') as InputWarning[]
          if (warnings.length > 0) {
            update((l) => ({ ...l, pending: { exerciseId: itemKey, set, warnings, lastWeightKg } }))
            return
          }
        }
        // The top set is worked out from the session afterwards (heaviest good set), never flagged here.
        // A stand-in key means a catalogue exercise with no row yet: the row is saved with this set, in one transaction.
        const entry = standInEntry(itemKey)
        // `unit` goes last and only for plates: a kg row is exactly as it was.
        const row = { sessionId: now.sessionId, exerciseId: entry ? null : itemKey, weightKg: set.weightKg, reps: set.reps, rpe: set.rpe, failed: set.failed, missPoint: null, isTopSet: false, ...unitKey(setUnit(set)) }
        slide.arm()
        let setId: number
        try {
          setId = await addSet(db, row, entry?.name ?? null)
        } catch (e) {
          slide.disarm()
          throw e
        }
        // One buzz per set saved, Log set or Missed; a failed write does not buzz. A new best gets the stronger buzz,
        // worked out after the save and beside it: it never holds the set up, and if it fails the set still ticks.
        if (hapticsPossible()) void buzzForSet(db, setId, row, now.date, now.side)
        update((l) => ({ ...l, pending: null }))
        touch(now.date)
        // Every exercise on the bodybuilding side takes the accessory rest (spec §5.3).
        setLogged.current?.(now.side === 'POWERLIFTING' && item?.exercise.category !== 'ACCESSORY')
        // Hold the guard until the new set is on screen, so the button the lifter sees is never stale.
        await shown(setId)
      } finally {
        saving.current = false
      }
    },
    [db, update, shown, loaded, touch, slide],
  )

  const confirmPending = useCallback(async () => {
    const held = latest.current.pending
    if (held) await logSet(held.exerciseId, held.set, true)
  }, [logSet])

  const cancelPending = useCallback(() => update((l) => ({ ...l, pending: null })), [update])

  const setMissPoint = useCallback(
    async (setId: number, point: MissPoint | null) => {
      await db.sets.update(setId, { missPoint: point })
    },
    [db],
  )

  const deleteSet = useCallback(
    async (setId: number) => {
      slide.arm()
      try {
        await db.sets.delete(setId)
      } catch (e) {
        slide.disarm()
        throw e
      }
    },
    [db, slide],
  )

  // Coach targets whose Apply is under way or done: a second tap landing before the first has applied (a double tap)
  // applies nothing more and buzzes nothing more. Kept once applied, as the advice is closed for good.
  const applyingTargets = useRef(new Set<number>())
  const applyTarget = useCallback(
    async (adviceId: number) => {
      if (applyingTargets.current.has(adviceId)) return
      applyingTargets.current.add(adviceId)
      let applied = false
      try {
        const suggestion = latest.current.items.map((i) => i.suggestion).find((s) => s?.id === adviceId)
        if (!suggestion) return
        // Status first: if storing it fails, the editor keeps its numbers and the chip stays.
        await db.advice.update(adviceId, { status: 'APPLIED' })
        const { exerciseId, weightKg, reps, rpe } = suggestion.target
        // In the target's unit, which is the exercise's: otherwise the screen would not show it.
        edit(exerciseId, { weightKg, reps: suggestion.applyReps ?? reps, rpe, ...unitKey(setUnit(suggestion.target)) })
        applied = true
        // One tick once the target is in the editor.
        haptic('tick')
      } finally {
        if (!applied) applyingTargets.current.delete(adviceId)
      }
    },
    [db, edit],
  )

  const dismissTarget = useCallback(
    async (adviceId: number) => {
      await db.advice.update(adviceId, { status: 'DISMISSED' })
    },
    [db],
  )

  const addLocally = useCallback((exerciseId: number) => update((l) => (l.added.includes(exerciseId) ? l : { ...l, added: [...l.added, exerciseId] })), [update])
  // A planned exercise skipped today and added again (the picker, the Exercises tab) is taken back as Bring back does
  // (1.13): it is its planned row again, not an added one. If that write fails, it is added for the session instead.
  const addExercise = useCallback(
    (exerciseId: number) => {
      const skipped = latest.current.skippedToday.find((s) => s.exerciseId === exerciseId)
      if (skipped == null) return addLocally(exerciseId)
      unskip(db, skipped.skipId).catch(() => addLocally(exerciseId))
    },
    [db, addLocally],
  )

  const addFromCatalogue = useCallback<TodayActions['addFromCatalogue']>(
    (name) => {
      const id = catalogueId(name, latest.current.exercises)
      if (id != null) addExercise(id)
      return id
    },
    [addExercise],
  )

  // True while the coach runs for Finish; a second tap must not run it again and replace the summary.
  const finishing = useRef(false)
  // True while `addToOpenSession` is at work; further calls are ignored until it has answered.
  const addingToSession = useRef(false)
  const addToOpenSession = useCallback<TodayActions['addToOpenSession']>(
    async (catalogueName, exerciseId) => {
      if (addingToSession.current) return null
      addingToSession.current = true
      try {
        if (!(await loaded())) return 'REFUSED'
        const sessionId = latest.current.sessionId
        // The session as it is stored now, then as this screen has it: a Finish a moment ago counts,
        // also one still running the coach.
        if (sessionId == null || (await db.sessions.get(sessionId)) == null) return 'REFUSED'
        const now = latest.current
        if (now.sessionId !== sessionId || now.summary != null || finishing.current) return 'REFUSED'
        const id = catalogueName != null ? catalogueId(catalogueName, now.exercises) : now.exercises.find((e) => e.id === exerciseId)?.id
        if (id == null) return 'REFUSED'
        if (now.items.some((i) => i.exercise.id === id)) return 'ALREADY_THERE'
        addExercise(id)
        return 'ADDED'
      } finally {
        addingToSession.current = false
      }
    },
    [db, loaded, addExercise],
  )

  const createExercise = useCallback<TodayActions['createExercise']>(
    async (exercise, onAdded, onMade) => {
      const entry = await createCustomExercise(db, exercise)
      onMade?.()
      const newRow = entry.library == null ? entry.exerciseId : null
      // A new row of the lifter's own reaches the screen with the next read: wait for it, so it slides in whole.
      if (newRow != null) await known(newRow)
      let id: number | null = null
      withTransition(() => {
        if (entry.library) id = addFromCatalogue(entry.library.name)
        else if (entry.exerciseId != null) {
          addExercise(entry.exerciseId)
          id = entry.exerciseId
        }
        onAdded?.(id)
      })
      return entry.library ? catalogueId(entry.library.name, latest.current.exercises) : entry.exerciseId
    },
    [db, addExercise, addFromCatalogue, known],
  )

  const removeExercise = useCallback(
    (exerciseId: number) => {
      // The card may be shown under its row's id by now and still be listed under its stand-in.
      const standIn = Object.entries(latest.current.resolved).find(([, id]) => id === exerciseId)?.[0]
      const gone = new Set([exerciseId, ...(standIn != null ? [Number(standIn)] : [])])
      update((l) => ({ ...l, added: l.added.filter((id) => !gone.has(id)), editors: Object.fromEntries(Object.entries(l.editors).filter(([id]) => !gone.has(Number(id)))) }))
      if (exerciseId >= 0) db.meta.delete(editorKey(date, exerciseId)).catch(() => {})
    },
    [db, date, update],
  )

  const setPlateCount = useCallback<NonNullable<TodayActions['setPlateCount']>>(
    async (itemKey, value) => {
      const entry = standInEntry(itemKey)
      const id = await storePlateCount(db, entry ? null : itemKey, entry?.name ?? null, value)
      // The numbers kept for the next set go, under the row's id, the key it was chosen under and any stand-in shown as
      // it: the field starts afresh in the new unit (whole ↔ half too).
      const standIns = Object.entries(latest.current.resolved).flatMap(([standIn, row]) => (row === id || row === itemKey ? [Number(standIn)] : []))
      const dropped = new Set([itemKey, id, ...standIns])
      update((l) => ({ ...l, editors: Object.fromEntries(Object.entries(l.editors).filter(([key]) => !dropped.has(Number(key)))) }))
      await db.meta.bulkDelete([...dropped].filter((key) => key >= 0).map((key) => editorKey(date, key)))
      // The coach reads the exercise in its new unit; what is stored already stays as it is.
      try {
        if (await coachPass(db, date, () => clock.current())) setCoachPasses((n) => n + 1)
      } catch (e) {
        // The setting is stored: a failed pass costs today's targets only, as on opening the screen.
        if (!(e instanceof CoachWriteError)) throw e
      }
    },
    [db, date, update],
  )

  const finishSession = useCallback(async () => {
    const now = latest.current
    if (finishing.current || now.summary != null || now.loading || now.sessionId == null) return
    finishing.current = true
    try {
      // Advice is dated by the real clock: a session finished after midnight stores nothing under the day before.
      const clockDay = clock.current()
      const [newNotes, skipped] = await db.transaction('rw', [db.sessions, db.sets, db.exercises, db.baselines, db.planItems, db.settings, db.advice, db.checkIns, db.skips], async () => {
        // The coach of the session's own side looks at it, also after a switch since it was started.
        const side = now.side
        const [history, plan, settings, advice, checkIns] = await Promise.all([loadHistory(db), planFor(db, side), getSettings(db), db.advice.toArray(), loadCheckIns(db)])
        const prior = advice.map((a) => adviceToJs(a as AdviceRow & { id: number }))
        // The check-ins tell the bodybuilding coach which body parts are growing well (their exercises are left alone);
        // the powerlifting coach does not read them.
        const fresh = grit.coachingEvaluateWithCheckIns(side, history, (plan as PlannedItem[]).map(planItemToJs), coachSettings(settings), clockDay, prior, checkIns.map(checkInToJs))
        if (fresh.length > 0) await db.advice.bulkAdd(fresh.map((a) => withoutId(adviceFromJs(a))))
        // The exercises skipped that day on the session's side (1.13).
        const skipped = await db.skips.where('[date+mode+exerciseId]').between([now.date, side, Dexie.minKey], [now.date, side, Dexie.maxKey]).count()
        return [fresh.length, skipped] as const
      })
      // Finished: it no longer keeps the screen, here or after a reload.
      activity.current = null
      db.meta.delete(ACTIVITY_KEY).catch(() => {})
      const done = now.items.filter((i) => i.sets.length > 0)
      // A summary already up is kept: it is the one that counted the new notes.
      const summary: FinishSummary = { setCount: done.reduce((n, i) => n + i.sets.length, 0), exerciseCount: done.length, newNotes, ...(skipped > 0 ? { skipped } : {}) }
      update((l) => (l.summary != null ? l : { ...l, summary }))
    } finally {
      finishing.current = false
    }
  }, [db, update])

  // True while a skip is being stored; a second Skip today meanwhile (a double tap) is ignored.
  const skipping = useRef(false)
  const skip = useCallback<TodayActions['skip']>(
    async (exerciseId, planItemId, reason) => {
      if (skipping.current) return null
      skipping.current = true
      // Released once the row has left the screen (below), or straight away when nothing is stored.
      let held = false
      try {
        if (!(await loaded())) return null
        const now = latest.current
        const item = now.items.find((i) => i.exercise.id === exerciseId)
        // A row on the list, planned and with nothing logged; or one skipped already today, whose reason this replaces.
        const again = now.skippedToday.find((s) => s.exerciseId === exerciseId)
        const name = item != null ? (item.planned == null || item.sets.length > 0 ? null : item.exercise.name) : (again?.name ?? null)
        if (name == null) return null
        const day = now.date
        const skipId = await storeSkip(db, { date: day, mode: now.side, exerciseId, planItemId: planItemId ?? item?.planned?.id ?? null, reason }, async (stored, all) => {
          // Read here, inside the transaction: a re-skip's old card is already gone, so it does not hold back the new
          // reason's card. The whole plan, both sides', and every piece of advice: the coach picks out the skip's side.
          const [history, plan, advice] = await Promise.all([loadHistory(db), db.planItems.toArray(), db.advice.toArray()])
          return grit.coachingAfterSkip(stored, all, history, (plan as PlannedItem[]).map(planItemToJs), day, advice.map((a) => adviceToJs(a as AdviceRow & { id: number })))
        })
        // One tick once it is stored.
        haptic('tick')
        // Hold the guard until the row has left, so a second tap on a row still drawn cannot skip it again.
        held = true
        void until((s) => !s.items.some((i) => i.exercise.id === exerciseId)).then(() => {
          skipping.current = false
        })
        return { skipId, name }
      } finally {
        if (!held) skipping.current = false
      }
    },
    [db, loaded, until],
  )

  const undoSkip = useCallback<TodayActions['undoSkip']>((skipId) => unskip(db, skipId), [db])

  const bringBack = useCallback<TodayActions['bringBack']>(
    async (exerciseId) => {
      const skipId = latest.current.skippedToday.find((s) => s.exerciseId === exerciseId)?.skipId
      if (skipId != null) await unskip(db, skipId)
    },
    [db],
  )

  const dismissSummary = useCallback(() => {
    update((l) => ({ ...l, summary: null }))
    // A session finished after midnight is behind us now: on to the new day.
    syncDay()
  }, [update, syncDay])

  return {
    ...state,
    startSession,
    edit,
    logSet,
    confirmPending,
    cancelPending,
    setMissPoint,
    deleteSet,
    applyTarget,
    dismissTarget,
    addExercise,
    addFromCatalogue,
    addToOpenSession,
    createExercise,
    removeExercise,
    setPlateCount,
    skip,
    undoSkip,
    bringBack,
    finishSession,
    dismissSummary,
    refreshDay,
  }
}
