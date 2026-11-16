package com.powertracker.domain.progress

import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.Test

class ProgressMathTest {
    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)
    private val bench = Exercise(2, "Bench", Category.BENCH, null, emptyList(), isBuiltIn = true)
    private val deadlift = Exercise(3, "Deadlift", Category.DEADLIFT, null, emptyList(), isBuiltIn = true)
    private val pauseSquat = Exercise(4, "Pause squat", Category.VARIATION, MainLift.SQUAT, emptyList(), isBuiltIn = true)
    private val exercises = listOf(squat, bench, deadlift, pauseSquat).associateBy { it.id }

    // Monday 2 November 2026.
    private val monday: LocalDate = LocalDate(2026, 11, 2)

    private fun set(exercise: Exercise, kg: Double, reps: Int = 1, rpe: Double? = 10.0, failed: Boolean = false) =
        LoggedSet(exercise.id, kg, reps, rpe, failed, null, isTopSet = false)

    private fun history(vararg sessions: Pair<LocalDate, List<LoggedSet>>, baselines: List<Baseline> = emptyList()) = TrainingHistory(
        sessions.mapIndexed { i, (date, sets) -> LoggedSession(i + 1L, date, null, null, sets) },
        exercises,
        baselines,
    )

    @Test
    fun prs_onlyRecordsImprovements() {
        // A single at RPE 10 has an e1RM equal to its weight: 100, 105, 103, 110.
        val history = history(
            monday to listOf(set(squat, 100.0)),
            monday.plus(7, DateTimeUnit.DAY) to listOf(set(squat, 105.0)),
            monday.plus(14, DateTimeUnit.DAY) to listOf(set(squat, 103.0)),
            monday.plus(21, DateTimeUnit.DAY) to listOf(set(squat, 110.0)),
        )

        val prs = ProgressMath.prs(history, MainLift.SQUAT)

        assertEquals(listOf(100.0, 105.0, 110.0), prs.map { it.e1rmKg })
        assertEquals(listOf(monday, monday.plus(7, DateTimeUnit.DAY), monday.plus(21, DateTimeUnit.DAY)), prs.map { it.date })
        assertEquals(Pr(monday.plus(21, DateTimeUnit.DAY), 110.0, 110.0, 1, 10.0), prs.last())
    }

    @Test
    fun prs_ignoreFailedSetsVariationsAndOtherLifts() {
        val history = history(
            monday to listOf(set(squat, 100.0)),
            monday.plus(7, DateTimeUnit.DAY) to listOf(set(squat, 120.0, failed = true), set(pauseSquat, 130.0), set(bench, 140.0)),
        )

        assertEquals(listOf(100.0), ProgressMath.prs(history, MainLift.SQUAT).map { it.e1rmKg })
    }

    @Test
    fun prs_withinOneSessionFollowLoggingOrder() {
        val history = history(monday to listOf(set(squat, 100.0), set(squat, 110.0), set(squat, 105.0)))

        assertEquals(listOf(100.0, 110.0), ProgressMath.prs(history, MainLift.SQUAT).map { it.e1rmKg })
    }

    // ---- isNewPr: the buzz for a new best, under the prs rule ----

    private val curl = Exercise(5, "Curl", Category.ACCESSORY, null, emptyList(), isBuiltIn = true)
    private val withCurl = (exercises + (curl.id to curl))
    private val today = monday.plus(7, DateTimeUnit.DAY)
    private val lastWeek = history(monday to listOf(set(squat, 100.0), set(squat, 110.0))).copy(exercises = withCurl)

    @Test
    fun isNewPr_aSetBeatingEveryEarlierE1rm_isOne() {
        assertTrue(ProgressMath.isNewPr(lastWeek, set(squat, 112.5), today, TrainingMode.POWERLIFTING))
    }

    @Test
    fun isNewPr_aLighterSet_isNot() {
        assertFalse(ProgressMath.isNewPr(lastWeek, set(squat, 105.0), today, TrainingMode.POWERLIFTING))
    }

    @Test
    fun isNewPr_anEqualE1rm_isNot() {
        assertFalse(ProgressMath.isNewPr(lastWeek, set(squat, 110.0), today, TrainingMode.POWERLIFTING))
        // 100 × 4 @ 9 has the same e1RM as a set already there.
        val e1rm = com.powertracker.domain.e1rm.E1rm.estimate(100.0, 4, 9.0)
        val same = history(monday to listOf(set(squat, e1rm))).copy(exercises = withCurl)
        assertFalse(ProgressMath.isNewPr(same, set(squat, 100.0, reps = 4, rpe = 9.0), today, TrainingMode.POWERLIFTING))
    }

    @Test
    fun isNewPr_aMissedSet_isNot() {
        assertFalse(ProgressMath.isNewPr(lastWeek, set(squat, 150.0, failed = true), today, TrainingMode.POWERLIFTING))
    }

    @Test
    fun isNewPr_accessoriesAndVariations_neverAre() {
        assertFalse(ProgressMath.isNewPr(lastWeek, set(curl, 50.0), today, TrainingMode.POWERLIFTING))
        assertFalse(ProgressMath.isNewPr(lastWeek, set(pauseSquat, 300.0), today, TrainingMode.POWERLIFTING))
    }

    @Test
    fun isNewPr_theBodybuildingSide_neverIs() {
        assertFalse(ProgressMath.isNewPr(lastWeek, set(squat, 200.0), today, TrainingMode.BODYBUILDING))
        // Nor does a bodybuilding session's heavy squat hold back a powerlifting record.
        val heavyOnTheOtherSide = TrainingHistory(
            listOf(LoggedSession(1, monday, null, null, listOf(set(squat, 150.0)), TrainingMode.BODYBUILDING)) + lastWeek.sessions.map { it.copy(id = 2) },
            withCurl, emptyList(),
        )
        assertTrue(ProgressMath.isNewPr(heavyOnTheOtherSide, set(squat, 120.0), today, TrainingMode.POWERLIFTING))
    }

    @Test
    fun isNewPr_theFirstSetOfALift_isItsFirstRecord_asPrsCountsIt() {
        assertEquals(1, ProgressMath.prs(history(today to listOf(set(bench, 60.0))), MainLift.BENCH).size)
        assertTrue(ProgressMath.isNewPr(lastWeek, set(bench, 60.0), today, TrainingMode.POWERLIFTING))
    }

    @Test
    fun isNewPr_earlierSetsTheSameDay_count_andLaterSessionsDoNot() {
        val todaySoFar = history(monday to listOf(set(squat, 100.0)), today to listOf(set(squat, 115.0))).copy(exercises = withCurl)
        assertFalse(ProgressMath.isNewPr(todaySoFar, set(squat, 112.5), today, TrainingMode.POWERLIFTING))
        assertTrue(ProgressMath.isNewPr(todaySoFar, set(squat, 117.5), today, TrainingMode.POWERLIFTING))
        // A session dated after the set (a clock change) is not "earlier".
        val later = history(monday.plus(30, DateTimeUnit.DAY) to listOf(set(squat, 200.0))).copy(exercises = withCurl)
        assertTrue(ProgressMath.isNewPr(later, set(squat, 120.0), today, TrainingMode.POWERLIFTING))
    }

    @Test
    fun isNewPr_agreesWithPrs_setBySet() {
        val sets = listOf(100.0, 105.0, 103.0, 110.0, 110.0, 111.0).map { set(squat, it) }
        var stored = history().copy(exercises = withCurl)
        val buzzes = sets.map { s ->
            ProgressMath.isNewPr(stored, s, today, TrainingMode.POWERLIFTING).also {
                stored = stored.copy(sessions = listOf(LoggedSession(1, today, null, null, (stored.sessions.firstOrNull()?.sets ?: emptyList()) + s)))
            }
        }
        assertEquals(listOf(true, true, false, true, false, true), buzzes)
        assertEquals(4, ProgressMath.prs(stored, MainLift.SQUAT).size)
    }

    @Test
    fun weeklyVolume_sumsWeightTimesReps() {
        val history = history(
            // Week of 2 Nov: 100 × 5 + 110 × 3 on Monday, 90 × 5 on Friday = 1280.
            monday to listOf(set(squat, 100.0, reps = 5, rpe = 8.0), set(squat, 110.0, reps = 3, rpe = 9.0)),
            monday.plus(4, DateTimeUnit.DAY) to listOf(set(squat, 90.0, reps = 5, rpe = 7.0)),
            // Week of 9 Nov: only the completed main-lift set counts = 500.
            monday.plus(8, DateTimeUnit.DAY) to listOf(
                set(squat, 100.0, reps = 5, rpe = 8.0),
                set(squat, 120.0, reps = 2, failed = true),
                set(pauseSquat, 80.0, reps = 5),
                set(bench, 60.0, reps = 5),
            ),
        )

        assertEquals(
            listOf(monday to 1280.0, monday.plus(7, DateTimeUnit.DAY) to 500.0),
            ProgressMath.weeklyVolume(history, MainLift.SQUAT),
        )
    }

    @Test
    fun total_nullUnlessAllThreeLifts() {
        val until = monday.plus(2, DateTimeUnit.DAY)
        val twoLifts = history(monday to listOf(set(squat, 180.0), set(bench, 120.0)))
        val allThree = history(monday to listOf(set(squat, 180.0), set(bench, 120.0), set(deadlift, 240.0)))

        assertNull(ProgressMath.total(HistoryStats(twoLifts), until, includeStrictPress = false))
        assertEquals(540.0, ProgressMath.total(HistoryStats(allThree), until, includeStrictPress = false)!!, 1e-9)
    }
}
