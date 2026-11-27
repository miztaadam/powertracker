package com.powertracker.domain.coach

import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.domain.stack.PlateStack

/**
 * One suggested warm-up set: [weight] in [unit] (kg, or a plate number) for [reps]. [perSide] is the plates on each
 * side of a 20 kg bar, there only for a barbell lift in kg that the plates can make (empty: the bar alone).
 */
data class WarmUpSet(val weight: Double, val reps: Int, val unit: LoadUnit, val perSide: List<Double>? = null)

/**
 * The warm-up checklist on the Today card (1.23): what to do before the first set of an exercise. It is a suggestion
 * to tick off and nothing more: the sets are never logged, so they are in no history, no chart and no coach rule
 * (the owner's call, 2026-11-27).
 *
 * Sources: [CoachSources.WARM_UP], which also says which of these the owner saw in the design and which were decided
 * while building. From the source: one set at half the working weight for [FIRST_REPS] reps; a second at about three
 * quarters of it on heavy lifts; none for a high-rep exercise. Not from the source: [SECOND_REPS], [HIGH_REPS], which
 * lifts count as heavy (the four main lifts and their variations), and which exercises have none.
 */
object WarmUp {
    /** The first set's share of the working weight. */
    const val FIRST_SHARE = 0.5

    /** The first set's reps: the source's log writes "50% of workout weight · 12 reps" (its text says 12 to 15). */
    const val FIRST_REPS = 12

    /** The second set's share of the working weight, on a main lift or a variation of one. */
    const val SECOND_SHARE = 0.75

    /** The second set's reps. The source gives none; few, so the set readies the lift without tiring it. */
    const val SECOND_REPS = 5

    /** At this many planned reps or more there is no warm-up. The source says "high repetition" without a number. */
    const val HIGH_REPS = 15

    /**
     * The warm-up sets for [exercise] before a first set at [working] (in the exercise's current unit,
     * [PlateStack.unitOf]) for [reps] (the plan's reps, the bottom of a range; the field's for an exercise with no plan
     * item). Lightest first; empty when there is nothing to suggest:
     *
     * - no weight, or none above 0 (a bodyweight set);
     * - [reps] at [HIGH_REPS] or more;
     * - a catalogue exercise done with bodyweight first (the weight is then added weight, and half of it is not half
     *   the effort), or an assisted one (there more weight is easier);
     * - every candidate rounds to the working weight or above, or to nothing.
     *
     * Each weight is rounded to one the lifter can load: on the bar's plate grid and never under the empty bar for a
     * barbell lift; on the working weight's own grid for an accessory or a load under the bar ([PlateLoader.roundFrom]);
     * down to the machine's grid for a plate number, never under plate 1. A set is kept only when it is lighter than
     * the working weight and heavier than the set before it.
     */
    fun sets(exercise: Exercise, working: Double?, reps: Int, changePlates: Boolean): List<WarmUpSet> {
        if (working == null || !working.isFinite() || working <= 0.0) return emptyList()
        if (reps >= HIGH_REPS) return emptyList()
        if (isBodyweightFirst(exercise) || isAssisted(exercise)) return emptyList()

        val unit = PlateStack.unitOf(exercise)
        val barbell = unit == LoadUnit.KG && !ProgressionRule.isFreeLoad(exercise, working)
        fun loadable(share: Double): Double = when {
            unit == LoadUnit.PLATES -> PlateStack.floor(working * share, PlateStack.step(exercise.plateCount))
            barbell -> PlateLoader.round(working * share, changePlates)
            else -> PlateLoader.roundFrom(working, working * share, changePlates)
        }

        val wanted = buildList {
            add(FIRST_SHARE to FIRST_REPS)
            if (exercise.mainLift != null) add(SECOND_SHARE to SECOND_REPS)
        }
        val out = ArrayList<WarmUpSet>(wanted.size)
        for ((share, count) in wanted) {
            val weight = loadable(share)
            if (weight <= 0.0 || weight >= working || weight <= (out.lastOrNull()?.weight ?: 0.0)) continue
            out += WarmUpSet(weight, count, unit, if (barbell) PlateLoader.perSide(weight, changePlates) else null)
        }
        return out
    }

    /** A catalogue exercise whose first piece of equipment is the lifter's own body: a pull-up, a dip, a push-up. */
    private fun isBodyweightFirst(exercise: Exercise): Boolean =
        exercise.isBuiltIn && Library.find(exercise.name)?.equipment?.firstOrNull() == Equipment.BODYWEIGHT

    /** The word "assisted" in the name, as [PlateStack.offered] reads it: on such a machine more weight is easier. */
    private fun isAssisted(exercise: Exercise): Boolean {
        val name = Library.normalise(exercise.name)
        return name == "assisted" || name.startsWith("assisted ") || " assisted" in name
    }
}
