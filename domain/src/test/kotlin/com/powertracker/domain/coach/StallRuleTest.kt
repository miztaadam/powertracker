package com.powertracker.domain.coach

import com.powertracker.domain.history
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StallRuleTest {
    private val start = LocalDate.of(2026, 1, 5)
    private fun day(n: Int): LocalDate = start.plusDays(n.toLong())
    private val settings = CoachSettings()

    private fun benchFlat(mondays: Int, miss: MissPoint? = null, missFromWeek: Int = 1) =
        history { for (w in 0 until mondays) session(w * 7) { bench(100.0, 5, miss = if (w >= missFromWeek) miss else null) } }

    private fun evaluate(h: com.powertracker.domain.model.TrainingHistory, lift: MainLift, today: LocalDate, current: Set<String> = emptySet()) =
        StallRule.evaluate(lift, HistoryStats(h), settings, today, current)

    @Test
    fun stall_benchFlatFourWeeksLockoutMisses_prescribesTriceps() {
        val advice = assertNotNull(evaluate(benchFlat(5, MissPoint.LOCKOUT), MainLift.BENCH, day(28)))
        assertEquals(AdviceType.STALL, advice.type)
        assertEquals(MainLift.BENCH, advice.lift)
        assertEquals(day(28), advice.createdAt)
        assertEquals(AdviceStatus.OPEN, advice.status)
        val items = (advice.payload as AdvicePayload.Accessories).items
        assertTrue(items.size in 2..3)
        val lockout = AccessoryMap.options(MainLift.BENCH, MissPoint.LOCKOUT).map { it.exerciseName }
        assertTrue(items.all { it.exerciseName in lockout })
        val byName = items.associateBy { it.exerciseName }
        val jm = assertNotNull(byName["JM press"])
        assertEquals(Triple(3, 8, 10), Triple(jm.sets, jm.repsLow, jm.repsHigh))
        val cg = assertNotNull(byName["Close-grip bench"])
        assertEquals(Triple(3, 3, 5), Triple(cg.sets, cg.repsLow, cg.repsHigh))
        assertTrue(advice.reason.contains("Bench e1RM hasn't improved for 4 weeks"))
        assertTrue(advice.reason.contains("at lockout"))
        assertTrue(advice.reason.contains("triceps"))
    }

    @Test
    fun stall_skipsAccessoriesAlreadyDone() {
        val advice = assertNotNull(evaluate(benchFlat(5, MissPoint.LOCKOUT), MainLift.BENCH, day(28), setOf("close-GRIP bench")))
        val names = (advice.payload as AdvicePayload.Accessories).items.map { it.exerciseName }
        assertEquals(listOf("JM press", "Board press"), names)
    }

    @Test
    fun noStall_whenRisingOverOnePercent() {
        val h = history {
            session(0) { bench(100.0, 5) }
            session(7) { bench(100.0, 5) }
            session(14) { bench(100.0, 5) }
            session(21) { bench(102.0, 5) }
        }
        assertNull(evaluate(h, MainLift.BENCH, day(21)))
    }

    @Test
    fun noStall_whenPeakRecent() {
        assertNull(evaluate(benchFlat(3), MainLift.BENCH, day(14)))
    }

    @Test
    fun deadlift_needsFourWeeks() {
        fun dl(mondays: Int) = history { for (w in 0 until mondays) session(w * 7) { deadlift(150.0, 5) } }
        assertNull(evaluate(dl(4), MainLift.DEADLIFT, day(21)))
        val advice = assertNotNull(evaluate(dl(5), MainLift.DEADLIFT, day(28)))
        assertEquals(AdviceType.STALL, advice.type)
        assertEquals(MainLift.DEADLIFT, advice.lift)
    }

    @Test
    fun noMissData_usesFallback() {
        val advice = assertNotNull(evaluate(benchFlat(5), MainLift.BENCH, day(28)))
        val names = (advice.payload as AdvicePayload.Accessories).items.map { it.exerciseName }
        assertEquals(listOf("Spoto press", "Long-pause bench"), names)
        assertEquals(
            "Bench e1RM hasn't improved for 4 weeks. Add one set, change the rep range (5s → 3s or 8s), or swap to a variation.",
            advice.reason,
        )
    }

    @Test
    fun severity_equalsWeeksSincePeak() {
        val advice = assertNotNull(evaluate(benchFlat(6), MainLift.BENCH, day(35)))
        assertEquals(5, advice.severity)
    }

    @Test
    fun dominantMiss_tiesGoToMostRecent() {
        val h = history {
            session(0) { bench(100.0, 5) }
            session(7) { bench(100.0, 5, miss = MissPoint.FLOOR) }
            session(14) { bench(100.0, 5, miss = MissPoint.LOCKOUT) }
            session(21) { bench(100.0, 5) }
            session(28) { bench(100.0, 5) }
        }
        val advice = assertNotNull(evaluate(h, MainLift.BENCH, day(28)))
        assertTrue(advice.reason.contains("at lockout"))
    }

    @Test
    fun missesBeforeThePlateau_areIgnored() {
        val h = history {
            session(0) { bench(100.0, 5, miss = MissPoint.FLOOR) }
            for (w in 1..4) session(w * 7) { bench(100.0, 5, miss = if (w == 4) MissPoint.MID else null) }
        }
        val advice = assertNotNull(evaluate(h, MainLift.BENCH, day(28)))
        assertTrue(advice.reason.contains("mid-range"))
    }

    @Test
    fun noStall_whenLogHasTooManyEmptyWeeks() {
        val h = history {
            session(0) { bench(100.0, 5) }
            session(35) { bench(100.0, 5) }
        }
        assertNull(evaluate(h, MainLift.BENCH, day(35)))
    }

    @Test
    fun noStall_withoutAnyHistory() {
        assertNull(evaluate(history { }, MainLift.SQUAT, day(28)))
    }
}
