package com.powertracker.domain.coach

import com.powertracker.domain.Ids
import com.powertracker.domain.history
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.TrainingHistory
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FatigueRuleTest {
    /** Day 13 of the builder's calendar; the 14-day window is days 0..13. */
    private val today = LocalDate.of(2026, 1, 5).plusDays(13)

    private fun evaluate(h: TrainingHistory) = FatigueRule.evaluate(HistoryStats(h), h, today)
    private fun signals(h: TrainingHistory) = FatigueRule.signals(HistoryStats(h), h, today).map { it.text }

    @Test
    fun twoSignals_deload() {
        val h = history {
            session(7, readiness = 2) { squat(100.0, 5, rpe = 7.0); bench(80.0, 5, rpe = 7.0) }
            session(9, readiness = 2) { squat(100.0, 5, rpe = 8.5); bench(80.0, 5, rpe = 8.5) }
        }
        val advice = assertNotNull(evaluate(h))
        assertEquals(AdviceType.DELOAD, advice.type)
        assertNull(advice.lift)
        assertEquals(today, advice.createdAt)
        assertEquals(2, advice.severity)
        assertEquals(AdvicePayload.Deload(0.6, 0.9), advice.payload)
        assertEquals(
            "Signs of fatigue: RPE rising at the same load on squat and bench; low readiness. " +
                "Take a deload week: about 60% of the volume at about 90% of the load.",
            advice.reason,
        )
    }

    @Test
    fun oneSignal_noDeload() {
        val h = history {
            session(8) {
                bench(100.0, 5)
                set(Ids.BENCH, 100.0, 5, failed = true)
                set(Ids.BENCH, 100.0, 5, failed = true)
            }
        }
        assertEquals(listOf("2 failed reps"), signals(h))
        assertNull(evaluate(h))
    }

    @Test
    fun allFour_severityFour() {
        val h = history {
            session(3, readiness = 2) { squat(100.0, 5, rpe = 7.0); bench(80.0, 5, rpe = 7.0) }
            session(10, readiness = 2) {
                squat(100.0, 5, rpe = 9.0)
                bench(80.0, 5, rpe = 9.0)
                set(Ids.SQUAT, 100.0, 5, failed = true)
                set(Ids.BENCH, 80.0, 5, failed = true)
            }
        }
        val advice = assertNotNull(evaluate(h))
        assertEquals(4, advice.severity)
        assertTrue("e1RM down on squat and bench" in advice.reason)
        assertTrue("2 failed reps" in advice.reason)
    }

    @Test
    fun singleLiftDrop_isNotFatigueSignal() {
        val h = history {
            session(3) { bench(100.0, 5, rpe = 7.0) }
            session(10) { bench(100.0, 5, rpe = 9.0) }
        }
        assertTrue(signals(h).none { it.startsWith("e1RM down") })
    }

    @Test
    fun readinessDrop() {
        val h = history {
            session(1, readiness = 4) { squat(100.0, 5) }
            session(3, readiness = 5) { squat(100.0, 5) }
            session(8, readiness = 3) { squat(100.0, 5) }
            session(10, readiness = 3) { squat(100.0, 5) }
        }
        assertEquals(listOf("low readiness"), signals(h))
    }
}
