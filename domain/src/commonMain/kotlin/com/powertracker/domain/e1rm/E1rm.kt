package com.powertracker.domain.e1rm

object E1rm {
    /** Fraction of 1RM for 1..12 reps taken to RPE 10. */
    private val pct = doubleArrayOf(1.000, .955, .922, .892, .863, .837, .811, .786, .762, .739, .707, .680)

    /**
     * Estimated one-rep max in kg.
     *
     * Effective reps = reps + (10 - rpe). Up to 12 effective reps the RPE-10 percentage table is used,
     * interpolating linearly between whole reps. With no RPE, or beyond 12 effective reps, Epley is used:
     * weight * (1 + effectiveReps / 30), where effectiveReps is [reps] when RPE is null.
     *
     * RPE outside 6.0..10.0 is clamped into that range. Returns 0.0 when [reps] <= 0 or [weightKg] <= 0.
     */
    fun estimate(weightKg: Double, reps: Int, rpe: Double?): Double {
        if (reps <= 0 || weightKg <= 0.0) return 0.0
        if (rpe == null) return epley(weightKg, reps.toDouble())
        val effective = reps + (10.0 - rpe.coerceIn(6.0, 10.0))
        if (effective > pct.size) return epley(weightKg, effective)
        val lo = effective.toInt().coerceAtLeast(1)
        val frac = effective - lo
        val p = if (lo >= pct.size) pct.last() else pct[lo - 1] + (pct[lo] - pct[lo - 1]) * frac
        return weightKg / p
    }

    private fun epley(weightKg: Double, effectiveReps: Double) = weightKg * (1 + effectiveReps / 30.0)
}
