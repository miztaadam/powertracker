package com.powertracker.ui.design

import androidx.compose.runtime.Composable
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.ui.progress.ChartRange
import com.powertracker.ui.progress.LiftHistoryContent
import com.powertracker.ui.progress.LiftHistoryViewModel
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressViewModel
import com.powertracker.domain.progress.weekStart
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.abs

// The Progress and lift-history mockups are the real screens fed a sample training history: the
// same lifter as the other mockups, twelve weeks of squat on Monday, bench on Wednesday and Friday
// and deadlift on Friday, ending on Wednesday 11 November. The view models' own `build` turns it
// into screen state, so the figures on the mockups are the app's arithmetic, not typed-in numbers.

private const val SQUAT_ID = 1L
private const val BENCH_ID = 2L
private const val DEADLIFT_ID = 3L
private const val PAUSE_SQUAT_ID = 4L
private const val PRESS_ID = 5L

private val EXERCISES = listOf(
    Exercise(SQUAT_ID, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true),
    Exercise(BENCH_ID, "Bench", Category.BENCH, null, emptyList(), isBuiltIn = true),
    Exercise(DEADLIFT_ID, "Deadlift", Category.DEADLIFT, null, emptyList(), isBuiltIn = true),
    Exercise(PAUSE_SQUAT_ID, "Pause squat", Category.VARIATION, MainLift.SQUAT, emptyList(), isBuiltIn = true),
).associateBy { it.id }

private val PRESS_EXERCISE = Exercise(PRESS_ID, "Strict press", Category.STRICT_PRESS, null, emptyList(), isBuiltIn = true)

/** Strict press e1RM by week, for the mockups with the Progress switch on. */
private val PRESS = listOf(70f, 70f, 71f, 72.5f, 72.5f, 74f, 75f, 75f, 76f, 77.5f, 77.5f, 80f)

private val BODYWEIGHT = listOf(91.8, 92.1, 92.0, 92.4, 92.3, 92.6, 92.5, 92.9, 92.8, 93.0, 93.3, 93.2)

/** The loadable top set (3 to 5 reps at RPE 7 to 9) whose e1RM comes closest to [e1rmKg]. */
private fun topSet(exerciseId: Long, e1rmKg: Float): LoggedSet {
    val options = (3..5).flatMap { reps ->
        listOf(7.0, 7.5, 8.0, 8.5, 9.0).flatMap { rpe ->
            val exact = e1rmKg / E1rm.estimate(1.0, reps, rpe)
            listOf(Math.floor(exact / 2.5) * 2.5, Math.ceil(exact / 2.5) * 2.5).map { kg -> LoggedSet(exerciseId, kg, reps, rpe, false, null, isTopSet = true) }
        }
    }
    return options.minBy { abs(E1rm.estimate(it.weightKg, it.reps, it.rpe) - e1rmKg) }
}

/** A top set followed by [backOffs] lighter sets of five. */
private fun work(exerciseId: Long, e1rmKg: Float, backOffs: Int): List<LoggedSet> {
    val top = topSet(exerciseId, e1rmKg)
    val backOffKg = Math.round(top.weightKg * 0.82 / 2.5) * 2.5
    return listOf(top) + List(backOffs) { LoggedSet(exerciseId, backOffKg, 5, 6.0, false, null, isTopSet = false) }
}

private val SAMPLE_HISTORY: TrainingHistory = run {
    val firstMonday = weekStart(SHOWCASE_DATE).minus(SQUAT.size - 1, DateTimeUnit.WEEK)
    var id = 0L
    val sessions = SQUAT.indices.flatMap { week ->
        val monday = firstMonday.plus(week, DateTimeUnit.WEEK)
        val pauseSquats = if (week % 2 == 1) List(3) { LoggedSet(PAUSE_SQUAT_ID, 130.0 + 2.5 * (week / 2), 3, 7.5, false, null, false) } else emptyList()
        // Week 10: a squat single that did not go, before the back-off work.
        val miss = if (week == 9) listOf(LoggedSet(SQUAT_ID, 170.0, 1, 10.0, true, MissPoint.FLOOR, false)) else emptyList()
        val squatDay = work(SQUAT_ID, SQUAT[week], backOffs = 3 + week % 2)
        listOf(
            LoggedSession(++id, monday, BODYWEIGHT[week], null, squatDay.take(1) + miss + squatDay.drop(1) + pauseSquats),
            LoggedSession(++id, monday.plus(2, DateTimeUnit.DAY), null, null, work(BENCH_ID, BENCH[week], backOffs = 3)),
            LoggedSession(++id, monday.plus(4, DateTimeUnit.DAY), BODYWEIGHT[week] + 0.3, null, work(DEADLIFT_ID, DEADLIFT[week], backOffs = 2 + week % 2) + work(BENCH_ID, BENCH[week] * 0.96f, backOffs = 2).map { it.copy(isTopSet = false) }),
        )
    }
    TrainingHistory(sessions.filter { it.date <= SHOWCASE_DATE }, EXERCISES, emptyList())
}

/** The same lifter, who also presses on Wednesday after the bench. */
internal val SAMPLE_HISTORY_WITH_PRESS: TrainingHistory = run {
    val firstMonday = weekStart(SHOWCASE_DATE).minus(SQUAT.size - 1, DateTimeUnit.WEEK)
    val sessions = SAMPLE_HISTORY.sessions.map { session ->
        val week = (session.date.toEpochDays() - firstMonday.toEpochDays()).toInt() / 7
        if (session.date.dayOfWeek == DayOfWeek.WEDNESDAY) session.copy(sets = session.sets + work(PRESS_ID, PRESS[week], backOffs = 2)) else session
    }
    TrainingHistory(sessions, EXERCISES + (PRESS_ID to PRESS_EXERCISE), emptyList())
}

/** [strictPress] is the Progress switch in Settings: on, strict press is a fourth lift and counts in the total. */
@Composable
fun ProgressShowcase(strictPress: Boolean = false) {
    val history = if (strictPress) SAMPLE_HISTORY_WITH_PRESS else SAMPLE_HISTORY
    ProgressContent(ProgressViewModel.build(history, ChartRange.MONTHS_6, SHOWCASE_DATE, showStrictPress = strictPress), onRange = {}, onOpenLift = {})
}

@Composable
fun LiftHistoryShowcase() {
    LiftHistoryContent(MainLift.SQUAT, LiftHistoryViewModel.build(SAMPLE_HISTORY, MainLift.SQUAT, SHOWCASE_DATE), onBack = {})
}
