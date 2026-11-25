import { exerciseFromJs, exerciseToJs } from '../../db/mapping'
import type { TrainingMode } from '../../db/schema'
import { DEFAULT_SETTINGS } from '../../db/settings'
import { grit, type JsAdvice, type JsCheckIn, type JsExercise, type JsPlanItem } from '../../domain'
import { LIFTS, type Lift } from '../../router'
import type { CardDetail, CoachActions, CoachCard, CoachState } from '../../state/useCoach'
import type { PlanActions, PlanItem, PlanState } from '../../state/usePlan'
import { DEFAULT_RANGE, buildBodybuildingProgress, buildProgress, type BodybuildingProgressState, type ProgressState } from '../../state/useProgress'
import type { TemplatesActions, TemplatesState } from '../../state/useTemplates'
import { weekdayOf, type PlannedItem, type SetEditor, type StoredSet, type TodayActions, type TodayItem, type TodayState } from '../../state/useToday'

/*
 * The domain's sample lifter as the real screens' state: what each picture's content component is fed at each step,
 * as Android's GuideSampleState. Nothing here is stored; the ids are the sample's own (guideSampleExercises,
 * guideSamplePlan, guideSampleTodayItems).
 */

/** The sample session's id: any id, as nothing is stored. */
const SESSION_ID = 1

const planned = (p: JsPlanItem): PlannedItem => ({
  id: p.id,
  weekday: p.weekday,
  exerciseId: p.exerciseId,
  sets: p.sets,
  reps: p.reps,
  targetRpe: p.targetRpe,
  order: p.order,
  mode: p.mode as TrainingMode,
  ...(p.repsHigh != null ? { repsHigh: p.repsHigh } : {}),
})

/**
 * Today on the sample's first day, in session, at `step` of screen 4: the sample lift's card with nothing in it, then
 * the weight, the reps and the RPE put in one by one, then the set logged (ticked), then the day's last planned
 * exercise skipped. The day is the domain's for screen 4 (`guideSampleTodayItems`, its first few items). Each item is built as `useToday` builds it for a lifter with no history: no weight yet, the plan's
 * reps and target RPE, the plan's sets, the plates for the weight. The lift's editor shows `shown`: by default the step's
 * end state (`guideSampleTodayEditor` at 1), part-way through a move the domain's in-between values.
 */
export function guideTodayState(mode: TrainingMode, step: string, shown?: SetEditor): TodayState {
  const steps = grit.guideSteps('TODAY', mode)
  const at = steps.indexOf(step)
  if (at < 0) throw new Error(`${step} is not a step of the Today picture`)
  const reached = (s: string) => at >= steps.indexOf(s)

  const sample = grit.guideSampleTodaySet(mode)
  const exercises = grit.guideSampleExercises(mode)
  const byId = new Map(exercises.map((e) => [e.id, e]))
  const plan = grit.guideSampleTodayItems(mode).map(planned)
  const liftId = exercises.find((e) => e.name === sample.exerciseId)!.id

  function item(p: PlannedItem): TodayItem {
    const lift = p.exerciseId === liftId
    const end = (): SetEditor => {
      const e = grit.guideSampleTodayEditor(mode, step, 1)
      return { weightKg: e.weightKg ?? null, reps: e.reps, rpe: e.rpe }
    }
    const editor: SetEditor = lift ? (shown ?? end()) : { weightKg: null, reps: p.reps, rpe: p.targetRpe }
    const sets: StoredSet[] =
      lift && reached('TODAY_TICKED')
        ? [{ id: 1, sessionId: SESSION_ID, exerciseId: liftId, weightKg: sample.doneWeightKg, reps: sample.doneReps, rpe: sample.doneRpe, failed: false, missPoint: null, isTopSet: false }]
        : []
    return {
      exercise: byId.get(p.exerciseId)!,
      planned: p,
      suggestion: null,
      sets,
      editor,
      targetSets: p.sets,
      isComplete: sets.length >= p.sets,
      perSide: editor.weightKg != null ? (grit.platesPerSide(editor.weightKg, false) ?? null) : null,
      lastWeightKg: sets[sets.length - 1]?.weightKg ?? null,
      lastTime: null,
    }
  }

  // Skip today: the day's last planned exercise, which has nothing logged, leaves the list for the "Skipped today" line.
  const skipped = reached('TODAY_SKIP') ? [...plan].reverse().find((p) => p.exerciseId !== liftId) : undefined
  return {
    date: sample.date,
    loading: false,
    sessionId: SESSION_ID,
    items: plan.filter((p) => p !== skipped).map(item),
    pending: null,
    summary: null,
    exercises,
    recentIds: [],
    resolved: {},
    weightStepKg: grit.loadStep(false),
    changePlates: false,
    coachPasses: 0,
    side: mode,
    activeSide: mode,
    skippedToday: skipped ? [{ exerciseId: skipped.exerciseId, name: byId.get(skipped.exerciseId)!.name, skipId: 1 }] : [],
  }
}

const nothing = () => Promise.resolve()

/** Actions that do nothing: the pictures are never tapped (they are inert), and nothing is ever stored from them. */
export const NO_TODAY_ACTIONS: TodayActions = {
  startSession: nothing,
  edit: () => {},
  logSet: nothing,
  confirmPending: nothing,
  cancelPending: () => {},
  setMissPoint: nothing,
  deleteSet: nothing,
  applyTarget: nothing,
  dismissTarget: nothing,
  addExercise: () => {},
  addFromCatalogue: () => null,
  createExercise: () => Promise.resolve(null),
  addToOpenSession: () => Promise.resolve(null),
  removeExercise: () => {},
  skip: () => Promise.resolve(null),
  undoSkip: nothing,
  bringBack: nothing,
  finishSession: nothing,
  dismissSummary: () => {},
  refreshDay: () => {},
}

// ---- screens 3 and 8: Plan and the templates ----

const planRow = (p: PlannedItem): PlanItem => p as PlanItem

/**
 * Plan for a new lifter with `items` on `mode`'s side (none: the empty plan): its day cards on the side's default days
 * and any day with items, as `usePlan` builds them; the exercises are `exercises` (the sample's).
 */
export function guidePlanState(mode: TrainingMode, items: readonly JsPlanItem[], exercises: readonly JsExercise[]): PlanState {
  const rows = exercises.map(exerciseFromJs)
  const names = new Map(rows.map((e) => [e.id, e.name]))
  const plan = items.map(planned).map(planRow)
  const trainingDays = [...(mode === 'BODYBUILDING' ? DEFAULT_SETTINGS.bodybuildingTrainingDays : DEFAULT_SETTINGS.trainingDays)]
  const weekdays = [...new Set([...trainingDays, ...plan.map((p) => p.weekday)])].sort((a, b) => a - b)
  return {
    loading: false,
    sections: weekdays.map((weekday) => ({
      weekday,
      rows: plan
        .filter((p) => p.weekday === weekday)
        .sort((a, b) => a.order - b.order || a.id - b.id)
        .map((item) => ({ item, exerciseName: names.get(item.exerciseId) ?? '' })),
    })),
    exercises: rows.filter((e) => !e.archived),
    recentIds: [],
    // A new lifter: the strict press switch off and no bests yet, so no "Best e1RM" lines.
    mainExercises: rows.filter((e) => grit.isMainLift(exerciseToJs(e))),
    bests: {},
    defaultRpe: DEFAULT_SETTINGS.defaultRpe,
    trainingDays,
    side: mode,
  }
}

/** Plan's actions, doing nothing: nothing is stored from a picture. */
export const NO_PLAN_ACTIONS: PlanActions = {
  draft: () => ({ sets: 0, reps: 0, targetRpe: 0 }),
  add: nothing,
  addOnce: () => Promise.resolve('ALREADY_THERE'),
  save: nothing,
  remove: nothing,
  move: nothing,
  addCustomExercise: () => Promise.reject(new Error('the guide stores nothing')),
  setBaseline: nothing,
}

/**
 * The templates at one of screen 3's steps on them: the side's list (`PLAN_TEMPLATES`), then the sample template's
 * draft on its days (`PLAN_DRAFT`), then that draft with the sample's one swap made (`PLAN_SWAP`). Nothing is stored.
 */
export function guideTemplatesState(mode: TrainingMode, step: string): TemplatesState {
  const templates = grit.planTemplatesFor(mode)
  const chosenId = grit.guideSampleTemplate(mode).id
  const drafting = step !== 'PLAN_TEMPLATES'
  const draft = drafting ? grit.guideSampleDraft(mode, step === 'PLAN_SWAP') : null
  const chosen = drafting ? (templates.find((t) => t.id === chosenId) ?? null) : null
  return {
    mode,
    loading: false,
    step: drafting ? 'draft' : 'list',
    templates,
    chosen,
    days: draft == null ? [] : draft.sessions.map((s) => s.day),
    draft,
    sideDays: [],
    strictPress: false,
    hasPlan: false,
    confirming: false,
    saving: false,
    saved: false,
    undo: null,
    failed: false,
    daysMatch: drafting,
    canUse: drafting,
    fixedReps: mode === 'POWERLIFTING',
  }
}

export const NO_TEMPLATES_ACTIONS: TemplatesActions = {
  choose: () => {},
  toggleDay: () => {},
  alternatives: () => [],
  swap: () => {},
  use: () => {},
  confirmReplace: () => {},
  cancelReplace: () => {},
  back: () => false,
}

// ---- screen 5: the coach's target, session after session ----

/**
 * Today on the day of screen 5's session `index` (the date its target is for), showing the sample lift only: its card
 * open, the coach's OPEN target (`guideSampleTargetAdvice`) with its reason, and the lift's fields holding `editor`
 * (what following it puts in: `guideSampleTargetEditor`). Nothing is logged yet on that day.
 */
export function guideTargetsState(mode: TrainingMode, index: number, editor: SetEditor): TodayState {
  const advice = grit.guideSampleTargetAdvice(mode)[index]
  if (advice == null) throw new Error(`${index} is not a step of the targets picture`)
  const p = grit.advicePayload(advice.payloadJson)
  if (p?.kind !== 'Target' || p.exerciseId == null || p.weightKg == null || p.reps == null || p.rpe == null) throw new Error('not a target')
  const exercises = grit.guideSampleExercises(mode)
  const exercise = exercises.find((e) => e.id === p.exerciseId)!
  const weekday = weekdayOf(advice.createdAt)
  const item = grit.guideSamplePlan(mode).map(planned).find((q) => q.exerciseId === exercise.id && q.weekday === weekday)!
  const history = grit.guideSampleTargetHistory(mode)
  // The last top set before the day, as Today judges a typo against.
  const last = history.sessions
    .filter((s) => s.date < advice.createdAt)
    .flatMap((s) => s.sets.filter((set) => set.exerciseId === exercise.id && !set.failed))
    .reduce<number | null>((kg, set) => (kg == null || set.weightKg > kg ? set.weightKg : kg), null)
  const target = { exerciseId: exercise.id, weightKg: p.weightKg, reps: p.reps, rpe: p.rpe, sets: p.sets ?? null, repsHigh: p.repsHigh ?? null }
  return {
    date: advice.createdAt,
    loading: false,
    sessionId: SESSION_ID,
    items: [
      {
        exercise,
        planned: item,
        suggestion: { id: advice.id, reason: advice.reason, target, applyReps: editor.reps },
        sets: [],
        editor,
        targetSets: item.sets,
        isComplete: false,
        perSide: editor.weightKg != null ? (grit.platesPerSide(editor.weightKg, false) ?? null) : null,
        lastWeightKg: last,
        // The guide's pictures keep to the coach's box: no "Last time" rows.
        lastTime: null,
      },
    ],
    pending: null,
    summary: null,
    exercises,
    recentIds: [],
    resolved: {},
    weightStepKg: grit.loadStep(false),
    changePlates: false,
    coachPasses: 0,
    side: mode,
    activeSide: mode,
    skippedToday: [],
  }
}

// ---- screen 6: the Coach screen's cards for when it holds back ----

/** As the Coach screen shows a stall's trend: its last six weeks. */
const TREND_WEEKS = 6

/** Whole weeks from `since` to `until`, as the Coach screen counts a swap's. */
const weeksBetween = (since: string, until: string) => Math.max(0, Math.floor((Date.parse(`${until}T00:00:00Z`) - Date.parse(`${since}T00:00:00Z`)) / (7 * 86_400_000)))

/** One of screen 6's cards (`guideSampleAdvice`) as the Coach screen draws it, with the figures the domain worked out for it. */
function coachCard(mode: TrainingMode, advice: JsAdvice): CoachCard {
  const p = grit.advicePayload(advice.payloadJson)
  const lift = (LIFTS as readonly string[]).includes(advice.lift ?? '') ? (advice.lift as Lift) : null
  const card = (detail: CardDetail): CoachCard => ({ advice: { id: advice.id, type: advice.type, lift, createdAt: advice.createdAt, reason: advice.reason }, detail })
  switch (p?.kind) {
    case 'Deload':
      return card({ kind: 'deload', volumePct: Math.round(p.volumeFactor! * 100), loadPct: Math.round(p.loadFactor! * 100) })
    case 'ReEntry':
      return card({ kind: 'reEntry', loadPct: Math.round(p.loadFactor! * 100) })
    case 'Skipped':
      return card({ kind: 'skip', exerciseId: p.exerciseId!, name: p.name!, alternatives: [...p.alternatives] })
    case 'Swap':
      return card({ kind: 'swap', exerciseId: p.exerciseId!, name: p.name!, group: p.group!, since: p.since!, weeks: weeksBetween(p.since!, advice.createdAt), alternatives: [...p.alternatives] })
    case 'Accessories': {
      const trend = grit.guideSampleStallTrend(mode).slice(-TREND_WEEKS).map((t) => t.value)
      return card({
        kind: 'stall',
        trend,
        e1rmKg: grit.guideSampleStallE1rm(mode) ?? trend[trend.length - 1] ?? null,
        prescriptions: p.items.map((i) => ({ exerciseName: i.exerciseName, sets: i.sets, repsLow: i.repsLow, repsHigh: i.repsHigh })),
      })
    }
    default:
      throw new Error(`screen 6 has no ${p?.kind ?? 'unreadable'} card`)
  }
}

/** The Coach screen at screen 6's step `index`: its feed holds that one card (`guideSampleAdvice(mode)[index]`). */
export function guideHoldsBackState(mode: TrainingMode, index: number): CoachState {
  const advice = grit.guideSampleAdvice(mode)[index]
  if (advice == null) throw new Error(`${index} is not a step of the holds-back picture`)
  return { loading: false, cards: [coachCard(mode, advice)], message: null, side: mode, checkInDue: false }
}

export const NO_COACH_ACTIONS: CoachActions = { apply: nothing, dismiss: nothing, clearMessage: () => {} }

// ---- screen 7: Progress, months on ----

/** Powerlifting's Progress screen on `date` of the months run: what `useProgress` builds from the run up to then. */
export function guideMonthsState(mode: TrainingMode, date: string): ProgressState {
  return buildProgress(grit.guideSampleMonthsHistory(mode, date), DEFAULT_RANGE, date)
}

/** Bodybuilding's Progress screen on `date`, with the run's check-in once it is there (`guideSampleMonthsCheckIn`). */
export function guideMonthsBodybuildingState(mode: TrainingMode, date: string, checkIn: JsCheckIn | null): BodybuildingProgressState {
  const plan = grit.guideSampleMonthsPlan(mode).map(planned).map(planRow)
  const checkIns = checkIn == null ? [] : [{ id: checkIn.id, date: checkIn.date, lagging: [...checkIn.lagging], growingWell: [...checkIn.growingWell] }]
  return buildBodybuildingProgress(grit.guideSampleMonthsHistory(mode, date), plan, date, checkIns)
}
