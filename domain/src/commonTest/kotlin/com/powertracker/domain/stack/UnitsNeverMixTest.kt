package com.powertracker.domain.stack

import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.coach.FatigueRule
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.coach.PhysiqueFixtures
import com.powertracker.domain.coach.SkipCoach
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.progress.BodybuildingProgress
import com.powertracker.domain.progress.ProgressMath
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 1.17: every coach rule reads one unit, and no kg figure ever reads a plate set. */
class UnitsNeverMixTest {
    private val today = LocalDate(2026, 11, 20)
    private val pulldown = PhysiqueFixtures.idOf("Lat pulldown")

    private val squat = 1L
    private val bench = 2L
    private val deadlift = 3L
    private val mainLifts = listOf(
        Exercise(squat, "Squat", Category.SQUAT, null, emptyList(), true),
        Exercise(bench, "Bench", Category.BENCH, null, emptyList(), true),
        Exercise(deadlift, "Deadlift", Category.DEADLIFT, null, emptyList(), true),
    ).associateBy { it.id }

    /** Every catalogue row, the three main lifts, and Lat pulldown with [count]. */
    private fun exercises(count: PlateCount): Map<Long, Exercise> =
        PhysiqueFixtures.exercises + mainLifts + (pulldown to PhysiqueFixtures.exercises.getValue(pulldown).copy(plateCount = count))

    private fun history(count: PlateCount, vararg sessions: LoggedSession) = TrainingHistory(sessions.toList(), exercises(count), emptyList())

    private fun kg(weight: Double, reps: Int, rpe: Double? = 8.0, id: Long = pulldown, top: Boolean = false, failed: Boolean = false) =
        LoggedSet(id, weight, reps, rpe, failed, null, top)

    private fun plate(plate: Double, reps: Int, rpe: Double? = 8.0, id: Long = pulldown, top: Boolean = false, failed: Boolean = false) =
        LoggedSet(id, plate, reps, rpe, failed, null, top, LoadUnit.PLATES)

    private fun day(daysAgo: Int) = today.minus(daysAgo, DateTimeUnit.DAY)

    private fun session(daysAgo: Int, vararg sets: LoggedSet, mode: TrainingMode = TrainingMode.POWERLIFTING, readiness: Int? = null) =
        LoggedSession(daysAgo.toLong() + 1 + (if (mode == TrainingMode.BODYBUILDING) 1000 else 0), day(daysAgo), null, readiness, sets.toList(), mode)

    private fun bb(daysAgo: Int, vararg sets: LoggedSet) = session(daysAgo, *sets, mode = TrainingMode.BODYBUILDING)

    /** Three sets of the same load and reps. */
    private fun three(set: LoggedSet) = arrayOf(set, set, set)

    private val plItem = PlanItem(1, today.dayOfWeek, pulldown, 3, 8, 8.0, 0)
    private val bbItem = PlanItem(2, today.dayOfWeek, pulldown, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)

    private fun powerliftingTargets(h: TrainingHistory) =
        Coaching.targetsFor(TrainingMode.POWERLIFTING, today.dayOfWeek, h, listOf(plItem), CoachSettings(), today, emptyList())

    private fun targetFor(h: TrainingHistory) =
        powerliftingTargets(h).singleOrNull { (it.payload as AdvicePayload.Target).exerciseId == pulldown }

    private fun stuck(h: TrainingHistory) =
        PhysiqueCoach.stuck(h, listOf(bbItem), emptyList(), emptyList(), today)

    // ---- the filter ----

    @Test
    fun inCurrentUnits_keepsEverySession_andOnlyCurrentUnitSets() {
        val h = history(
            PlateCount.WHOLE,
            session(7, kg(60.0, 10), plate(7.0, 10), kg(100.0, 5, id = squat)),
            session(3, kg(60.0, 10)),
        )
        val current = h.inCurrentUnits()
        assertEquals(2, current.sessions.size)
        assertEquals(listOf(plate(7.0, 10), kg(100.0, 5, id = squat)), current.sessions[0].sets)
        // Emptied, but still there with its id, date and side: gaps and easing back in count sessions.
        assertEquals(h.sessions[1].copy(sets = emptyList()), current.sessions[1])
        assertEquals(day(3), current.sessions[1].date)
        assertEquals(h.exercises, current.exercises)

        // Back to kg: the kg sets come back, the plate set goes.
        val off = h.copy(exercises = exercises(PlateCount.OFF)).inCurrentUnits()
        assertEquals(listOf(kg(60.0, 10), kg(100.0, 5, id = squat)), off.sessions[0].sets)
        assertEquals(listOf(kg(60.0, 10)), off.sessions[1].sets)
    }

    @Test
    fun inCurrentUnits_withNoPlates_changesNothing() {
        val h = history(PlateCount.OFF, session(7, kg(60.0, 10), kg(100.0, 5, id = squat)), session(3, kg(62.5, 8)))
        assertEquals(h, h.inCurrentUnits())
        // A set of an exercise with no row reads as kg.
        val orphan = TrainingHistory(listOf(session(2, kg(40.0, 8, id = 99_999))), exercises(PlateCount.WHOLE), emptyList())
        assertEquals(orphan, orphan.inCurrentUnits())
    }

    // ---- the powerlifting coach ----

    @Test
    fun aKgHistoryThenPlates_givesNoPowerliftingTarget() {
        val sessions = arrayOf(session(14, kg(60.0, 8, top = true)), session(10, kg(60.0, 8, top = true)), session(7, kg(60.0, 8, top = true)))
        assertNotNull(targetFor(history(PlateCount.OFF, *sessions)))
        assertNull(targetFor(history(PlateCount.WHOLE, *sessions)))
        assertNull(targetFor(history(PlateCount.HALF, *sessions)))
    }

    @Test
    fun switchingBackToKg_picksTheOldKgSetsUp() {
        val h = history(
            PlateCount.OFF,
            session(20, kg(57.5, 8, top = true)),
            session(17, kg(60.0, 8, top = true)),
            session(10, plate(7.0, 8, top = true)),
            session(7, plate(7.0, 9, top = true)),
            session(3, plate(7.0, 10, top = true)),
        )
        val target = assertNotNull(targetFor(h))
        val payload = target.payload as AdvicePayload.Target
        assertEquals(LoadUnit.KG, payload.unit)
        assertTrue(target.reason.startsWith("Last time: 60 × 8 @ 8."), target.reason)
        // 60 kg on target, a free load: 2.5 kg more, under the 5% cap (63).
        assertEquals(62.5, payload.weightKg)
    }

    // ---- kg figures ----

    @Test
    fun aPlateSetOnAMainLift_feedsNoKgFigure() {
        // A hand-edited backup: plate 9 on a squat, in its own earlier week, and a heavier plate "top set" last.
        val plateSquat = kg(9.0, 5, id = squat, top = true).copy(unit = LoadUnit.PLATES)
        val kgSessions = listOf(
            session(6, kg(100.0, 5, 7.0, id = squat, top = true), kg(80.0, 5, 7.0, id = bench, top = true), kg(120.0, 5, 7.0, id = deadlift, top = true), readiness = 2),
            session(4, kg(100.0, 5, 8.5, id = squat, top = true), kg(80.0, 5, 8.5, id = bench, top = true), kg(120.0, 5, 7.0, id = deadlift, top = true), readiness = 2),
        )
        val without = TrainingHistory(kgSessions, exercises(PlateCount.OFF), emptyList())
        val heavyPlateTop = kg(150.0, 5, 8.5, id = squat, top = true).copy(unit = LoadUnit.PLATES)
        val with = without.copy(
            sessions = listOf(session(20, plateSquat)) + kgSessions.dropLast(1) +
                kgSessions.last().let { it.copy(sets = it.sets + heavyPlateTop) },
        )
        val a = HistoryStats(with)
        val b = HistoryStats(without)
        for (lift in listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT)) {
            assertEquals(b.weeklyTrend(lift, today), a.weeklyTrend(lift, today))
            assertEquals(b.currentE1rm(lift, today), a.currentE1rm(lift, today))
            assertEquals(b.sessionCount(lift), a.sessionCount(lift))
            assertEquals(ProgressMath.prs(without, lift), ProgressMath.prs(with, lift))
            assertEquals(ProgressMath.weeklyVolume(without, lift), ProgressMath.weeklyVolume(with, lift))
            assertEquals(ProgressMath.knownBest(without, lift), ProgressMath.knownBest(with, lift))
        }
        assertEquals(ProgressMath.total(b, today, false), ProgressMath.total(a, today, false))
        assertFalse(ProgressMath.isNewPr(without, plateSquat, today, TrainingMode.POWERLIFTING))
        // The kg history raises the RPE-rising deload (squat and bench; deadlift held its RPE); the plate "top set"
        // must not take the squat's place in it.
        val deload = assertNotNull(FatigueRule.evaluate(b, without, today))
        assertEquals(AdviceType.DELOAD, deload.type)
        assertEquals(deload, FatigueRule.evaluate(a, with, today))

        // With only that set (and kg bench and deadlift), the squat has no figure at all.
        val only = TrainingHistory(
            listOf(session(20, plateSquat), session(4, kg(80.0, 5, id = bench, top = true), kg(120.0, 5, id = deadlift, top = true))),
            exercises(PlateCount.OFF), emptyList(),
        )
        val s = HistoryStats(only)
        assertEquals(emptyList(), s.weeklyTrend(MainLift.SQUAT, today))
        assertNull(s.currentE1rm(MainLift.SQUAT, today))
        assertEquals(0, s.sessionCount(MainLift.SQUAT))
        assertEquals(emptyList(), ProgressMath.prs(only, MainLift.SQUAT))
        assertEquals(emptyList(), ProgressMath.weeklyVolume(only, MainLift.SQUAT))
        assertNull(ProgressMath.knownBest(only, MainLift.SQUAT))
        assertNull(ProgressMath.total(s, today, false))
        val noSquat = only.copy(sessions = only.sessions.drop(1))
        assertFalse(ProgressMath.isNewPr(noSquat, plateSquat, today, TrainingMode.POWERLIFTING))
        // A kg squat there would be the first record: only the unit stops this one.
        assertTrue(ProgressMath.isNewPr(noSquat, plateSquat.copy(unit = LoadUnit.KG), today, TrainingMode.POWERLIFTING))
    }

    // ---- reads the whole history ----

    @Test
    fun aPainHold_isReleasedByASetInEitherUnit() {
        val skips = listOf(Skip(1, day(10), TrainingMode.POWERLIFTING, pulldown, 1, SkipReason.PAIN))
        assertTrue(SkipCoach.held(pulldown, skips, history(PlateCount.WHOLE, session(14, plate(7.0, 8))), today))
        assertFalse(SkipCoach.held(pulldown, skips, history(PlateCount.WHOLE, session(5, kg(60.0, 8))), today))
        assertFalse(SkipCoach.held(pulldown, skips, history(PlateCount.OFF, session(5, plate(7.0, 8))), today))
        assertFalse(SkipCoach.held(pulldown, skips, history(PlateCount.WHOLE, session(5, plate(7.0, 8))), today))
    }

    // ---- stuck ----

    @Test
    fun stuck_comparesOnlyTheCurrentUnit() {
        val sessions = arrayOf(bb(28, *three(kg(40.0, 10))), bb(14, *three(kg(40.0, 10))), bb(7, *three(kg(40.0, 10))))
        assertEquals(1, stuck(history(PlateCount.OFF, *sessions)).size)
        assertEquals(emptyList(), stuck(history(PlateCount.WHOLE, *sessions)))
    }

    @Test
    fun stuck_aPlateExerciseAddingRepsThenAPlate_isNotStuck() {
        val adding = history(
            PlateCount.WHOLE,
            bb(28, *three(plate(7.0, 10))), bb(21, *three(plate(7.0, 11))), bb(14, *three(plate(7.0, 12))), bb(7, *three(plate(8.0, 8))),
        )
        assertEquals(emptyList(), stuck(adding))
        // A higher plate beats the baseline although the reps fell from 36 to 24.
        val heavier = history(PlateCount.WHOLE, bb(21, *three(plate(7.0, 12))), bb(14, *three(plate(8.0, 8))), bb(7, *three(plate(8.0, 8))))
        assertEquals(emptyList(), stuck(heavier))
    }

    @Test
    fun stuck_theSamePlateAndRepsFor21Days_givesASwap() {
        val h = history(PlateCount.WHOLE, bb(28, *three(plate(7.0, 10))), bb(14, *three(plate(7.0, 10))), bb(7, *three(plate(7.0, 10))))
        val advice = stuck(h).single()
        assertEquals(AdviceType.SWAP, advice.type)
        assertEquals(pulldown, (advice.payload as AdvicePayload.Swap).exerciseId)
        assertTrue(advice.reason.contains("Lat pulldown"), advice.reason)
        assertFalse(advice.reason.contains("plate"), advice.reason)
        assertFalse(advice.reason.contains("kg"), advice.reason)
        assertFalse(advice.reason.contains("7"), advice.reason)
    }

    @Test
    fun stuck_anExerciseSwitchedToPlatesLessThan21DaysAgo_isNotStuck() {
        val kgMonths = (63 downTo 21 step 7).map { bb(it, *three(kg(40.0, 10))) }.toTypedArray()
        val plates = arrayOf(bb(14, *three(plate(7.0, 10))), bb(7, *three(plate(7.0, 10))), bb(1, *three(plate(7.0, 10))))
        assertEquals(1, stuck(history(PlateCount.OFF, *kgMonths, bb(14, *three(kg(40.0, 10))), bb(7, *three(kg(40.0, 10))))).size)
        assertEquals(emptyList(), stuck(history(PlateCount.WHOLE, *kgMonths, *plates)))
    }

    // ---- bodybuilding Progress ----

    @Test
    fun bodybuildingProgress_rowsHoldOnlyCurrentUnitSets() {
        val kgSessions = arrayOf(bb(30, *three(kg(40.0, 10))), bb(25, *three(kg(42.5, 10))), bb(20, *three(kg(45.0, 10))))
        val h = history(PlateCount.WHOLE, *kgSessions, bb(10, *three(plate(8.0, 10))), bb(5, *three(plate(9.0, 10))))
        val row = BodybuildingProgress.rows(h, listOf(bbItem), today).single()
        assertEquals(listOf(day(10), day(5)), row.bestSets.map { it.first })
        assertTrue(row.bestSets.all { it.second.unit == LoadUnit.PLATES })
        assertEquals(listOf(8.0, 9.0), row.bestSets.map { it.second.weightKg })
        assertEquals(true, row.goingUp)

        val onePlate = BodybuildingProgress.rows(history(PlateCount.WHOLE, *kgSessions, bb(5, *three(plate(9.0, 10)))), listOf(bbItem), today).single()
        assertEquals(listOf(plate(9.0, 10)), onePlate.bestSets.map { it.second })
        assertNull(onePlate.goingUp)
    }
}
