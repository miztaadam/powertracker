package com.powertracker.domain.unilateral

import com.powertracker.domain.model.Side
import com.powertracker.domain.model.TrainingHistory
import kotlin.math.abs
import kotlin.math.round

/**
 * Left vs. Right balance summary for one single-sided exercise across its recent sessions.
 * [avgRepGap] is `avg(right.reps - left.reps)` (positive = Left trailing Right; negative = Right trailing Left).
 * [weakerSide] is null when the two sides are balanced (`|avgRepGap| < 0.5`).
 */
data class SideBalanceRow(
    val exerciseId: Long,
    val name: String,
    val weakerSide: Side?,
    val avgRepGap: Double,
    val sessionCount: Int,
    val summary: String,
)

/** Computes Left vs. Right balance across recent sessions for exercises logged one side at a time. */
object SideBalance {
    private const val RECENT_SESSIONS = 3
    private const val BALANCE_THRESHOLD = 0.5

    fun rows(history: TrainingHistory): List<SideBalanceRow> {
        // Group split-side sets by (exerciseId, sessionDate), newest sessions last.
        val byExercise = LinkedHashMap<Long, MutableList<List<com.powertracker.domain.model.SplitSides>>>()
        val orderedSessions = history.sessions.sortedBy { it.date }
        for (session in orderedSessions) {
            val splitByEx = session.sets
                .filter { it.splitSides != null }
                .groupBy { it.exerciseId }
            for ((exId, sets) in splitByEx) {
                val splits = sets.mapNotNull { it.splitSides }
                if (splits.isNotEmpty()) {
                    byExercise.getOrPut(exId) { mutableListOf() }.add(splits)
                }
            }
        }
        return byExercise.entries.mapNotNull { (exId, sessionSplits) ->
            val exercise = history.exercises[exId] ?: return@mapNotNull null
            val recent = sessionSplits.takeLast(RECENT_SESSIONS)
            val allSplits = recent.flatten()
            if (allSplits.isEmpty()) return@mapNotNull null
            // Positive = right did more reps (left is weaker); negative = left did more reps (right is weaker).
            val rawGap = allSplits.map { (it.right.reps - it.left.reps).toDouble() }.average()
            val roundedGap = round(rawGap * 10.0) / 10.0
            val sessionsLabel = if (recent.size == 1) "last session" else "last ${recent.size} sessions"
            val weaker = when {
                roundedGap >= BALANCE_THRESHOLD -> Side.LEFT
                roundedGap <= -BALANCE_THRESHOLD -> Side.RIGHT
                else -> null
            }
            val summary = when (weaker) {
                null -> "Balanced across $sessionsLabel"
                Side.LEFT -> "Left trailing Right by ~${formatGap(abs(roundedGap))} reps ($sessionsLabel)"
                Side.RIGHT -> "Right trailing Left by ~${formatGap(abs(roundedGap))} reps ($sessionsLabel)"
            }
            SideBalanceRow(
                exerciseId = exId,
                name = exercise.name,
                weakerSide = weaker,
                avgRepGap = roundedGap,
                sessionCount = recent.size,
                summary = summary,
            )
        }.sortedWith(compareByDescending<SideBalanceRow> { abs(it.avgRepGap) }.thenBy { it.name })
    }

    private fun formatGap(value: Double): String {
        val whole = value.toLong()
        return if (abs(value - whole.toDouble()) < 1e-9) whole.toString() else value.toString()
    }
}
