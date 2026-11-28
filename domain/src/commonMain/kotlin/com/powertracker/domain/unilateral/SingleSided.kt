package com.powertracker.domain.unilateral

import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.SideLog
import com.powertracker.domain.model.SplitSides
import com.powertracker.domain.stack.PlateStack
import kotlin.math.abs
import kotlin.math.round

/**
 * Single-sided ("one side at a time") set tracking: which exercises offer it, how Right and Left combine into one
 * logged set (Coach & Progress read the weaker side so load only advances once both sides hit the target), and how
 * the two halves are formatted for display.
 */
object SingleSided {
    private val UNILATERAL_EQUIPMENT = setOf(
        Equipment.MACHINE,
        Equipment.CABLE,
        Equipment.DUMBBELL,
        Equipment.KETTLEBELL,
    )

    /**
     * Offered on accessory exercises that can be done one side at a time: machines (e.g. Pec deck, Leg press,
     * Lat pulldown), cables, dumbbells, kettlebells, and custom accessories. Main lifts and barbell variations
     * never offer it.
     */
    fun offered(exercise: Exercise): Boolean {
        if (exercise.category != Category.ACCESSORY) return false
        if (!exercise.isBuiltIn) return true
        val equipment = Library.find(exercise.name)?.equipment ?: return false
        return equipment.any { it in UNILATERAL_EQUIPMENT }
    }

    /** True only when [exercise] is [offered] and its single-sided setting is turned on. */
    fun active(exercise: Exercise): Boolean = exercise.singleSided && offered(exercise)

    /**
     * The weaker of [right] and [left], used as the set's primary weight/reps/RPE so the coach and progress charts
     * never bump weight until both sides hit the target:
     * 1. A failed side is weaker than a completed side.
     * 2. Lower weight is weaker.
     * 3. Fewer reps is weaker.
     * 4. Higher RPE (harder effort for the same weight and reps) is weaker.
     */
    fun weaker(right: SideLog, left: SideLog): SideLog {
        if (right.failed != left.failed) return if (right.failed) right else left
        if (right.weightKg != left.weightKg) return if (right.weightKg < left.weightKg) right else left
        if (right.reps != left.reps) return if (right.reps < left.reps) right else left
        val rr = right.rpe
        val lr = left.rpe
        if (rr != lr) {
            if (rr == null) return left
            if (lr == null) return right
            return if (rr > lr) right else left
        }
        return right
    }

    /** Combines [right] and [left] into a single [LoggedSet] whose main fields hold the weaker side. */
    fun combine(
        exerciseId: Long,
        right: SideLog,
        left: SideLog,
        unit: LoadUnit = LoadUnit.KG,
        isTopSet: Boolean = false,
        missPoint: MissPoint? = null,
    ): LoggedSet {
        val w = weaker(right, left)
        return LoggedSet(
            exerciseId = exerciseId,
            weightKg = w.weightKg,
            reps = w.reps,
            rpe = w.rpe,
            failed = w.failed,
            missPoint = missPoint,
            isTopSet = isTopSet,
            unit = unit,
            splitSides = SplitSides(right = right, left = left),
        )
    }

    /**
     * Compact summary of the two halves for a set row:
     * - Same weight: `"R 12 · L 10"`
     * - Different weights: `"R 40×12 · L 37.5×10"` (or plate numbers when [unit] is [LoadUnit.PLATES])
     * Marks a failed side with `" miss"` when only one side missed.
     */
    fun summaryText(split: SplitSides, unit: LoadUnit = LoadUnit.KG): String {
        val sameWeight = abs(split.right.weightKg - split.left.weightKg) < 1e-9
        val oneMissed = split.right.failed != split.left.failed
        fun sidePart(tag: String, side: SideLog): String {
            val base = if (sameWeight) "$tag ${side.reps}" else "$tag ${formatLoad(side.weightKg, unit)}×${side.reps}"
            return if (oneMissed && side.failed) "$base miss" else base
        }
        return "${sidePart("R", split.right)} · ${sidePart("L", split.left)}"
    }

    private fun formatLoad(value: Double, unit: LoadUnit): String {
        if (unit == LoadUnit.PLATES) return PlateStack.text(value)
        val rounded = round(value * 100.0) / 100.0
        val whole = rounded.toLong()
        return if (abs(rounded - whole.toDouble()) < 1e-9) whole.toString() else rounded.toString().trimEnd('0').trimEnd('.')
    }
}
