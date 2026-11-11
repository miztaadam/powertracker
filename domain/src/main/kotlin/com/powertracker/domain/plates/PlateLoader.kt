package com.powertracker.domain.plates

import kotlin.math.max
import kotlin.math.roundToLong

object PlateLoader {
    val DEFAULT_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val CHANGE_PLATES = listOf(0.5, 0.25)
    const val BAR_KG = 20.0

    // All arithmetic is done in integer hundredths of a kg to avoid floating-point drift.
    private const val SCALE = 100.0
    private val BAR_HUNDREDTHS = toHundredths(BAR_KG)

    private fun toHundredths(kg: Double): Long = (kg * SCALE).roundToLong()
    private fun fromHundredths(h: Long): Double = h / SCALE

    fun plates(changePlates: Boolean): List<Double> =
        if (changePlates) (DEFAULT_PLATES + CHANGE_PLATES).sortedDescending() else DEFAULT_PLATES

    fun step(changePlates: Boolean): Double = 2 * plates(changePlates).min()

    fun round(kg: Double, changePlates: Boolean): Double {
        val step = toHundredths(step(changePlates))
        val h = toHundredths(kg)
        val whole = Math.floorDiv(h, step)
        val remainder = h - whole * step
        val units = if (remainder * 2 > step) whole + 1 else whole // exact tie goes lighter
        return fromHundredths(max(units * step, BAR_HUNDREDTHS))
    }

    /**
     * For loads that are not on a barbell (accessories, sub-bar loads): the weight [anchor] + k × step
     * nearest to [kg] (exact tie goes lighter), with no bar floor and never below 0, so a bodyweight 0
     * stays 0 and a 12 kg dumbbell stays on its own 12, 14.5, 17 … grid.
     */
    fun roundFrom(anchor: Double, kg: Double, changePlates: Boolean): Double {
        val step = toHundredths(step(changePlates))
        val base = toHundredths(anchor)
        val h = toHundredths(kg) - base
        val whole = Math.floorDiv(h, step)
        val remainder = h - whole * step
        val units = if (remainder * 2 > step) whole + 1 else whole // exact tie goes lighter
        return fromHundredths(max(base + units * step, 0L))
    }

    fun perSide(totalKg: Double, changePlates: Boolean): List<Double>? {
        val beyondBar = toHundredths(totalKg) - BAR_HUNDREDTHS
        if (beyondBar < 0 || beyondBar % 2L != 0L) return null
        var remaining = beyondBar / 2
        val result = mutableListOf<Double>()
        for (plate in plates(changePlates)) {
            val p = toHundredths(plate)
            while (remaining >= p) {
                result += plate
                remaining -= p
            }
        }
        return if (remaining == 0L) result else null
    }
}
