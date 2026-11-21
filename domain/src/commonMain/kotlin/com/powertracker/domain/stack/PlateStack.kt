package com.powertracker.domain.stack

import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingHistory
import kotlin.math.ceil
import kotlin.math.floor as floorOf
import kotlin.math.roundToLong

/** What a typed plate number is: on the grid and in range, off the setting's grid, or outside plate 1 to [PlateStack.MAX_PLATE]. */
enum class PlateCheck { OK, STEP, RANGE }

/**
 * Counting plates on a stack machine (1.17): which exercises may, the step grid, the lighter-week rule and the
 * wording. The grid is plate 1, 2, 3 … with whole plates, or 1, 1½, 2 … with half steps; nothing goes under plate 1.
 * Every comparison is made in whole half-plates, so a factor never lands a step lower through rounding.
 */
object PlateStack {
    const val MIN_PLATE = 1.0

    /** The highest plate the apps accept when typed (the design leaves the bound to the builder). */
    const val MAX_PLATE = 50.0

    private const val MIN_HALVES = 2L
    private const val MAX_HALVES = 100L

    /** Absorbs the last-bit error of a product such as 10 × 0.9, so it floors to plate 9 and never to 8. */
    private const val EPSILON = 1e-9

    /**
     * Whether [exercise] may count plates at all: an accessory whose name does not hold the word "assisted" (on an
     * assisted machine more weight is easier), and either a catalogue machine or cable exercise, or a custom one.
     * Main lifts, variations (built-in or custom) and SMITH or free-weight catalogue entries never count plates.
     */
    fun offered(exercise: Exercise): Boolean {
        if (exercise.category != Category.ACCESSORY) return false
        val name = Library.normalise(exercise.name)
        if (name == "assisted" || name.startsWith("assisted ") || " assisted" in name) return false
        if (!exercise.isBuiltIn) return true
        val equipment = Library.find(exercise.name)?.equipment ?: return false
        return Equipment.MACHINE in equipment || Equipment.CABLE in equipment
    }

    /** [LoadUnit.PLATES] only when [exercise] is [offered] and its setting is on; a stored setting elsewhere counts as off. */
    fun unitOf(exercise: Exercise): LoadUnit =
        if (exercise.plateCount != PlateCount.OFF && offered(exercise)) LoadUnit.PLATES else LoadUnit.KG

    /** One step on [count]'s grid: ½ with half steps, else 1. */
    fun step(count: PlateCount): Double = when (count) {
        PlateCount.HALF -> 0.5
        PlateCount.WHOLE, PlateCount.OFF -> 1.0
    }

    /** The lowest grid plate above [plate], at most [MAX_PLATE]; plate 1 from an empty field. */
    fun up(plate: Double?, step: Double): Double {
        if (plate == null) return MIN_PLATE
        val s = halves(step)
        val next = (floorOf(plate * 2 + EPSILON).toLong().floorDiv(s) + 1) * s
        return fromHalves(next.coerceIn(MIN_HALVES, MAX_HALVES))
    }

    /** The highest grid plate below [plate], never under plate 1; plate 1 from an empty field. */
    fun down(plate: Double?, step: Double): Double {
        if (plate == null) return MIN_PLATE
        val s = halves(step)
        val below = (ceil(plate * 2 - EPSILON).toLong() - 1).floorDiv(s) * s
        return fromHalves(below.coerceAtLeast(MIN_HALVES))
    }

    /** The highest grid plate not above [plate], never under plate 1. */
    fun floor(plate: Double, step: Double): Double {
        val s = halves(step)
        return fromHalves((floorOf(plate * 2 + EPSILON).toLong().floorDiv(s) * s).coerceAtLeast(MIN_HALVES))
    }

    /**
     * The lighter-week and step-down rule: [factor] of [reference], rounded down to the grid, at least one step
     * below [reference], never under plate 1.
     */
    fun eased(reference: Double, factor: Double, step: Double): Double =
        minOf(floor(reference * factor, step), down(reference, step))

    /** A typed plate: [PlateCheck.STEP] off [count]'s grid (OFF checks as whole plates), else [PlateCheck.RANGE] outside 1 to [MAX_PLATE]. */
    fun check(plate: Double, count: PlateCount): PlateCheck {
        val h = plate * 2
        if (!h.isFinite() || h != floorOf(h) || h.toLong() % halves(step(count)) != 0L) return PlateCheck.STEP
        return if (plate < MIN_PLATE || plate > MAX_PLATE) PlateCheck.RANGE else PlateCheck.OK
    }

    /** "plate 7", "plate 7½". */
    fun text(plate: Double): String {
        val h = (plate * 2).roundToLong()
        return "plate ${h / 2}" + if (h % 2 != 0L) "½" else ""
    }

    /** What a screen reader says: "plate 7", "plate 7 and a half". */
    fun spoken(plate: Double): String {
        val h = (plate * 2).roundToLong()
        return "plate ${h / 2}" + if (h % 2 != 0L) " and a half" else ""
    }

    private fun halves(step: Double): Long = (step * 2).roundToLong().coerceAtLeast(1L)

    private fun fromHalves(h: Long): Double = h / 2.0
}

/**
 * The same sessions, each holding only the sets logged in their exercise's current unit ([PlateStack.unitOf]); a set
 * whose exercise has no row counts as kg. No session is dropped, even when emptied: the 14-day gap, easing back in and
 * "trained before" count sessions. With no plate set and no setting on, this history itself is returned.
 */
fun TrainingHistory.inCurrentUnits(): TrainingHistory {
    val anyPlates = sessions.any { s -> s.sets.any { it.unit != LoadUnit.KG } } ||
        exercises.values.any { it.plateCount != PlateCount.OFF }
    if (!anyPlates) return this
    val units = HashMap<Long, LoadUnit>()
    fun unitOf(exerciseId: Long): LoadUnit =
        units.getOrPut(exerciseId) { exercises[exerciseId]?.let(PlateStack::unitOf) ?: LoadUnit.KG }
    var changed = false
    val kept = sessions.map { session ->
        val sets = session.sets.filter { it.unit == unitOf(it.exerciseId) }
        if (sets.size == session.sets.size) session else session.copy(sets = sets).also { changed = true }
    }
    return if (changed) copy(sessions = kept) else this
}
