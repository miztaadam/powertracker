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
    )

    /** Muscles worked for the miss point; empty for the fallback (null). */
    fun muscles(lift: MainLift, miss: MissPoint?): List<String> =
        miss?.let { table.getValue(lift to it).muscles }.orEmpty()

    /** Options in table order; with no miss point, the lift's first two heavy variations. */
    fun options(lift: MainLift, miss: MissPoint?): List<AccessoryOption> =
        if (miss != null) {
            table.getValue(lift to miss).options
        } else {
            MissPoint.entries.flatMap { table.getValue(lift to it).options }
                .filter { it.heavy }.distinct().take(2)
        }

    val allExerciseNames: Set<String> =
        table.values.flatMap { row -> row.options.map { it.exerciseName } }.toSet()
}
