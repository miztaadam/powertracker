import type { TrainingMode } from '../../db/schema'
import { grit, type JsPlanItem } from '../../domain'
import type { PlannedItem, SetEditor, StoredSet, TodayActions, TodayItem, TodayState } from '../../state/useToday'

/*
 * The domain's sample lifter as the real screens' state: what each picture's content component is fed at each step,
 * as Android's GuideSampleState. Nothing here is stored; the ids are the sample's own (guideSampleExercises,
 * guideSamplePlan).
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

/** The ISO weekday (1 = Monday) of a `YYYY-MM-DD` date. */
const weekdayOf = (iso: string) => {
  const day = new Date(`${iso}T00:00:00Z`).getUTCDay()
  return day === 0 ? 7 : day
}

/**
 * Today on the sample's first day, in session, at `step` of screen 4: the sample lift's card with nothing in it, then
 * the weight, the reps and the RPE put in one by one, then the set logged (ticked), then the day's last planned
 * exercise skipped. Each item is built as `useToday` builds it for a lifter with no history: no weight yet, the plan's
 * reps and target RPE, the plan's sets, the plates for the weight.
 */
export function guideTodayState(mode: TrainingMode, step: string): TodayState {
  const steps = grit.guideSteps('TODAY', mode)
  const at = steps.indexOf(step)
  if (at < 0) throw new Error(`${step} is not a step of the Today picture`)
  const reached = (s: string) => at >= steps.indexOf(s)

  const sample = grit.guideSampleTodaySet(mode)
  const exercises = grit.guideSampleExercises(mode)
  const byId = new Map(exercises.map((e) => [e.id, e]))
  const day = weekdayOf(sample.date)
  const plan = grit
    .guideSamplePlan(mode)
    .filter((p) => p.weekday === day)
    .sort((a, b) => a.order - b.order)
    .map(planned)
  const liftId = exercises.find((e) => e.name === sample.exerciseId)!.id

  function item(p: PlannedItem): TodayItem {
    const lift = p.exerciseId === liftId
    const editor: SetEditor = lift
      ? {
          weightKg: reached('TODAY_WEIGHT') ? sample.doneWeightKg : null,
          reps: reached('TODAY_REPS') ? sample.doneReps : p.reps,
          rpe: reached('TODAY_RPE') ? sample.doneRpe : p.targetRpe,
        }
      : { weightKg: null, reps: p.reps, rpe: p.targetRpe }
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
