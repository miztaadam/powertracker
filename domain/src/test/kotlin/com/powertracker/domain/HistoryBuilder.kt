package com.powertracker.domain

import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.TrainingHistory
import java.time.LocalDate

object Ids {
    const val SQUAT = 1L
    const val BENCH = 2L
    const val DEADLIFT = 3L
    const val PAUSE_BENCH = 10L
    const val CURL = 50L
}

/** Builds a [TrainingHistory]. [start] (a Monday by default) is day 0; `dayOffset` counts days from it. */
fun history(start: LocalDate = LocalDate.of(2026, 1, 5), block: HistoryBuilder.() -> Unit): TrainingHistory =
    HistoryBuilder(start).apply(block).build()

class HistoryBuilder(private val start: LocalDate) {
    private val sessions = mutableListOf<LoggedSession>()
    private val baselines = mutableListOf<Baseline>()

    /** Adds a session [dayOffset] days after the start. Session ids auto-increment from 1. */
    fun session(dayOffset: Int, readiness: Int? = null, block: SessionBuilder.() -> Unit) {
        val sets = SessionBuilder().apply(block).sets
        sessions += LoggedSession(sessions.size + 1L, start.plusDays(dayOffset.toLong()), null, readiness, sets)
    }

    /** Adds a baseline e1RM for [exerciseId], dated at the start. */
    fun baseline(exerciseId: Long, e1rm: Double) {
        baselines += Baseline(exerciseId, e1rm, start)
    }

    internal fun build() = TrainingHistory(sessions.toList(), exercises, baselines.toList())

    private companion object {
        val exercises: Map<Long, Exercise> = listOf(
            Exercise(Ids.SQUAT, "Squat", Category.SQUAT, null, emptyList(), true),
            Exercise(Ids.BENCH, "Bench", Category.BENCH, null, emptyList(), true),
            Exercise(Ids.DEADLIFT, "Deadlift", Category.DEADLIFT, null, emptyList(), true),
            Exercise(Ids.PAUSE_BENCH, "Pause Bench", Category.VARIATION, MainLift.BENCH, emptyList(), true),
            Exercise(Ids.CURL, "Curl", Category.ACCESSORY, null, emptyList(), true),
        ).associateBy { it.id }
    }
}

class SessionBuilder {
    internal val sets = mutableListOf<LoggedSet>()

    /** Named lift calls log one set flagged as the top set. */
    fun squat(kg: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false, miss: MissPoint? = null) =
        set(Ids.SQUAT, kg, reps, rpe, failed, miss, isTopSet = true)

    fun bench(kg: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false, miss: MissPoint? = null) =
        set(Ids.BENCH, kg, reps, rpe, failed, miss, isTopSet = true)

    fun deadlift(kg: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false, miss: MissPoint? = null) =
        set(Ids.DEADLIFT, kg, reps, rpe, failed, miss, isTopSet = true)

    fun set(
        exerciseId: Long,
        kg: Double,
        reps: Int,
        rpe: Double? = 8.0,
        failed: Boolean = false,
        miss: MissPoint? = null,
        isTopSet: Boolean = false,
    ) {
        sets += LoggedSet(exerciseId, kg, reps, rpe, failed, miss, isTopSet)
    }
}
