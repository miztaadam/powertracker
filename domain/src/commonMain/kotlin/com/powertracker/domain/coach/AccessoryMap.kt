package com.powertracker.domain.coach

import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint

/** heavy variations are prescribed 3x3-5, hypertrophy accessories 3x8-10. */
data class AccessoryOption(val exerciseName: String, val heavy: Boolean)

/** Static table from spec 5.2: which muscles and accessories address a miss point on each lift. */
object AccessoryMap {
    private class Row(val muscles: List<String>, val options: List<AccessoryOption>)

    private fun heavy(name: String) = AccessoryOption(name, heavy = true)
    private fun light(name: String) = AccessoryOption(name, heavy = false)

    private val table: Map<Pair<MainLift, MissPoint>, Row> = mapOf(
        (MainLift.SQUAT to MissPoint.FLOOR) to Row(
            listOf("Quads", "Glutes"),
            listOf(heavy("Pause squat"), heavy("Front squat"), light("Bulgarian split squat")),
        ),
        (MainLift.SQUAT to MissPoint.MID) to Row(
            listOf("Upper back", "Trunk"),
            listOf(heavy("Pin squat"), heavy("Safety-bar squat"), heavy("Front squat")),
        ),
        (MainLift.SQUAT to MissPoint.LOCKOUT) to Row(
            listOf("Glutes", "Hips"),
            listOf(light("Hip thrust"), heavy("Box squat")),
        ),
        (MainLift.BENCH to MissPoint.FLOOR) to Row(
            listOf("Pecs", "Front delts"),
            listOf(heavy("Spoto press"), heavy("Long-pause bench"), light("DB press")),
        ),
        (MainLift.BENCH to MissPoint.MID) to Row(
            listOf("Pecs", "Triceps"),
            listOf(heavy("Pin press"), heavy("Larsen press")),
        ),
        (MainLift.BENCH to MissPoint.LOCKOUT) to Row(
            listOf("Triceps"),
            listOf(heavy("Close-grip bench"), light("JM press"), heavy("Board press")),
        ),
        (MainLift.DEADLIFT to MissPoint.FLOOR) to Row(
            listOf("Quads", "Lats", "Position"),
            listOf(heavy("Deficit deadlift"), heavy("Paused deadlift")),
        ),
        (MainLift.DEADLIFT to MissPoint.MID) to Row(
            listOf("Hamstrings", "Lats"),
            listOf(light("RDL"), heavy("Paused deadlift at the knee")),
        ),
        (MainLift.DEADLIFT to MissPoint.LOCKOUT) to Row(
            listOf("Glutes", "Upper back"),
            listOf(heavy("Block pull"), light("Hip thrust"), light("RDL")),
        ),
        // Strict press (strict press spec §3.2). "Delts" and "Upper chest" are mapped in MuscleGroups.
        (MainLift.STRICT_PRESS to MissPoint.FLOOR) to Row(
            listOf("Front delts", "Upper chest"),
            listOf(light("Seated barbell press"), light("Incline bench press"), light("Landmine press")),
        ),
        (MainLift.STRICT_PRESS to MissPoint.MID) to Row(
            listOf("Delts", "Triceps"),
            listOf(light("DB shoulder press"), light("Arnold press"), light("One-arm DB press")),
        ),
        (MainLift.STRICT_PRESS to MissPoint.LOCKOUT) to Row(
            listOf("Triceps", "Upper back"),
            // Close-grip bench stays heavy: it is a bench variation.
            listOf(heavy("Close-grip bench"), light("JM press"), light("DB overhead triceps extension")),
        ),
    )

    /** Muscles worked for the miss point; empty for the fallback (null). */
    fun muscles(lift: MainLift, miss: MissPoint?): List<String> =
        miss?.let { table.getValue(lift to it).muscles }.orEmpty()

    /**
     * Options in table order; with no miss point, the lift's first two heavy variations. A lift with fewer than two
     * (strict press has only Close-grip bench) is filled up from its other options in table order.
     */
    fun options(lift: MainLift, miss: MissPoint?): List<AccessoryOption> =
        if (miss != null) {
            table.getValue(lift to miss).options
        } else {
            val all = MissPoint.entries.flatMap { table.getValue(lift to it).options }.distinct()
            (all.filter { it.heavy } + all.filterNot { it.heavy }).take(2)
        }

    /** Every option for [lift], miss points in table order, each exercise once. */
    fun allOptions(lift: MainLift): List<AccessoryOption> =
        MissPoint.entries.flatMap { table.getValue(lift to it).options }.distinctBy { it.exerciseName }

    val allExerciseNames: Set<String> =
        table.values.flatMap { row -> row.options.map { it.exerciseName } }.toSet()
}
