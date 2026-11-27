import type { LoadUnit } from '../db/schema'
import { grit, type JsExercise } from '../domain'

/**
 * One suggested warm-up set (1.23): `weight` in `unit` (kg, or a plate number) for `reps`. `perSide` is the plates on
 * each side of the bar, there only for a barbell lift the plates can make (empty: the bar alone).
 */
export type WarmUpSet = { weight: number; reps: number; unit: LoadUnit; perSide: number[] | null }

/**
 * The warm-up sets before a first set of `exercise` at `working` for `reps` (the plan's, or the field's for an exercise
 * with no plan item), lightest first. Empty when there is nothing to suggest. The rule is the domain's `WarmUp.sets`.
 */
export function warmUpSets(exercise: JsExercise, working: number | null, reps: number, changePlates: boolean): WarmUpSet[] {
  if (working == null) return []
  return grit.warmUp(exercise, working, reps, changePlates).map((set) => ({
    weight: set.weight,
    reps: set.reps,
    unit: set.unit as LoadUnit,
    perSide: set.perSide != null ? [...set.perSide] : null,
  }))
}

/** What a tick is kept under: the set's own numbers, so a tick does not pass to a set with other numbers. */
export const warmUpKey = (set: WarmUpSet): string => `${set.unit}:${set.weight}x${set.reps}`
