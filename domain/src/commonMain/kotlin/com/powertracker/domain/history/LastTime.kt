package com.powertracker.domain.history

import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.LocalDate

/** An exercise's sets, in logged order, in one earlier session. Each set is in the unit it was logged in. */
data class LastTime(val date: LocalDate, val sets: List<LoggedSet>)

/**
 * What "last time" means for an exercise, to the Today card and to the bodybuilding coach alike: its sets in the latest
 * session on the [mode] side strictly before [before] that holds it; null when there is none. Two sessions on one
 * date: the one later in the list.
 */
fun TrainingHistory.lastTime(exerciseId: Long, mode: TrainingMode, before: LocalDate): LastTime? =
    sessions
        .filter { it.mode == mode && it.date < before }
        .sortedBy { it.date }
        .lastOrNull { s -> s.sets.any { it.exerciseId == exerciseId } }
        ?.let { s -> LastTime(s.date, s.sets.filter { it.exerciseId == exerciseId }) }

/** [lastTime] for every exercise that has one, in one pass over the sessions: what a screen of cards asks at once. */
fun TrainingHistory.lastTimes(mode: TrainingMode, before: LocalDate): Map<Long, LastTime> {
    val out = HashMap<Long, LastTime>()
    for (s in sessions.filter { it.mode == mode && it.date < before }.sortedBy { it.date }.asReversed()) {
        for ((id, sets) in s.sets.groupBy { it.exerciseId }) if (id !in out) out[id] = LastTime(s.date, sets)
    }
    return out
}
