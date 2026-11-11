package com.powertracker.domain.coach

import com.powertracker.domain.history
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.TrainingHistory
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeakPointRuleTest {
    private val start = LocalDate.of(2026, 1, 5)
    private fun day(n: Int): LocalDate = start.plusDays(n.toLong())

    private fun weekly(weeks: Int, squat: Double, bench: Double, deadlift: Double) = history {
        for (w in 0 until weeks) session(w * 7) {
            squat(squat, 5)
            bench(bench, 5)
            deadlift(deadlift, 5)
        }
    }

    private fun evaluate(h: TrainingHistory, today: LocalDate) = WeakPointRule.evaluate(HistoryStats(h), today)

    @Test
    fun lowBenchRatioFourWeeks_flagsBench() {
        val result = evaluate(weekly(4, 200.0, 110.0, 240.0), day(21))
        assertEquals(1, result.size)
        val advice = result.single()
        assertEquals(AdviceType.WEAK_POINT, advice.type)
        assertEquals(MainLift.BENCH, advice.lift)
        assertEquals(day(21), advice.createdAt)
        assertEquals(4, advice.severity)
        assertEquals(AdvicePayload.WeakPoint(MainLift.BENCH, null), advice.payload)
        assertTrue(advice.reason.startsWith("Your bench is low relative to your squat (55% vs a typical 60–70%)."), advice.reason)
        assertTrue(advice.reason.contains("Prioritise bench."))
    }

    @Test
    fun lowRatioOnlyThreeWeeks_noFlag() {
        assertTrue(evaluate(weekly(3, 200.0, 110.0, 240.0), day(14)).isEmpty())
    }

    @Test
    fun staleRatioData_noFlag() {
        assertTrue(evaluate(weekly(4, 200.0, 110.0, 240.0), day(21 + 14)).isEmpty())
    }

    @Test
    fun squatToDeadliftLow_flagsSquat() {
        val result = evaluate(weekly(4, 180.0, 115.0, 240.0), day(21))
        assertEquals(listOf(MainLift.SQUAT), result.map { it.lift })
        assertEquals(AdvicePayload.WeakPoint(MainLift.SQUAT, null), result.single().payload)
        assertTrue(result.single().reason.contains("Your squat is low relative to your deadlift (75% vs a typical 80–90%)."))
    }

    @Test
    fun squatToDeadliftHigh_flagsDeadlift() {
        val result = evaluate(weekly(4, 230.0, 150.0, 240.0), day(21))
        assertEquals(listOf(MainLift.DEADLIFT), result.map { it.lift })
        assertTrue(result.single().reason.contains("Your deadlift is low relative to your squat (squat is 96% of deadlift, typical 80–90%). Prioritise deadlift."))
    }

    @Test
    fun normalRatios_noFlag() {
        assertTrue(evaluate(weekly(4, 200.0, 130.0, 240.0), day(21)).isEmpty())
    }

    @Test
    fun consistentMissPoint_flags() {
        val h = history {
            for (w in 0 until 4) session(w * 7) {
                squat(200.0, 5)
                bench(130.0, 5)
                deadlift(240.0, 5, failed = w < 3, miss = if (w < 3) MissPoint.FLOOR else null)
            }
        }
        val advice = evaluate(h, day(21)).single()
        assertEquals(MainLift.DEADLIFT, advice.lift)
        assertEquals(AdvicePayload.WeakPoint(MainLift.DEADLIFT, MissPoint.FLOOR), advice.payload)
        assertEquals(3, advice.severity)
        assertEquals("You've missed deadlift off the floor 3 times in the last 4 weeks.", advice.reason)
    }

    @Test
    fun missesBelowSixtyPercentShare_noFlag() {
        val h = history {
            for (w in 0 until 4) session(w * 7) {
                squat(200.0, 5)
                bench(130.0, 5)
                deadlift(240.0, 5, failed = true, miss = if (w < 3) MissPoint.FLOOR else MissPoint.LOCKOUT)
                deadlift(240.0, 5, failed = true, miss = MissPoint.MID)
            }
        }
        assertTrue(evaluate(h, day(21)).isEmpty())
    }

    @Test
    fun ratioAndMissOnSameLift_singleCombinedAdvice() {
        val h = history {
            for (w in 0 until 4) session(w * 7) {
                squat(200.0, 5)
                bench(110.0, 5, miss = if (w < 3) MissPoint.MID else null)
                deadlift(240.0, 5)
            }
        }
        val advice = evaluate(h, day(21)).single()
        assertEquals(MainLift.BENCH, advice.lift)
        assertEquals(AdvicePayload.WeakPoint(MainLift.BENCH, MissPoint.MID), advice.payload)
        assertEquals(4, advice.severity)
        assertTrue(advice.reason.contains("Prioritise bench."))
        assertTrue(advice.reason.contains("You've missed bench mid-range 3 times in the last 4 weeks."))
    }

    @Test
    fun missesOutsideFourWeekWindow_ignored() {
        val h = history {
            for (w in 0 until 4) session(w * 7) {
                squat(200.0, 5)
                bench(130.0, 5)
                deadlift(240.0, 5, failed = w < 3, miss = if (w < 3) MissPoint.FLOOR else null)
            }
        }
        // today = day 35: window is day 8..35, only the week-2 miss remains; ratio data is 2 weeks stale
        assertTrue(evaluate(h, day(35)).isEmpty())
    }
}
