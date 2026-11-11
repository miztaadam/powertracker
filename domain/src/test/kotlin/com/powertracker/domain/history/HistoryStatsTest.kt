package com.powertracker.domain.history

import com.powertracker.domain.Ids
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryStatsTest {
    private val start = LocalDate.of(2026, 1, 5)
    private fun day(n: Int) = start.plusDays(n.toLong())

    @Test
    fun weeklyTrend_takesBestSetOfWeek() {
        val h = history {
            session(0) { squat(150.0, 5) }
            session(3) { squat(120.0, 5) }
        }
        val trend = HistoryStats(h).weeklyTrend(MainLift.SQUAT, day(10))
        assertEquals(listOf(WeeklyPoint(start, E1rm.estimate(150.0, 5, 8.0))), trend)
    }

    @Test
    fun weeklyTrend_variationCountsAtNinetyPercent() {
        val h = history { session(0) { set(Ids.PAUSE_BENCH, 100.0, 5) } }
        val stats = HistoryStats(h)
        val trend = stats.weeklyTrend(MainLift.BENCH, day(6))
        assertEquals(1, trend.size)
        assertEquals(0.9 * E1rm.estimate(100.0, 5, 8.0), trend[0].e1rmKg, 1e-9)
        assertNull(stats.currentE1rm(MainLift.BENCH, day(6)))
    }

    @Test
    fun weeklyTrend_excludesFailedSets() {
        val h = history {
            session(0) { squat(200.0, 3, failed = true) }
            session(7) { squat(150.0, 5); squat(210.0, 1, failed = true) }
        }
        val trend = HistoryStats(h).weeklyTrend(MainLift.SQUAT, day(20))
        assertEquals(listOf(WeeklyPoint(day(7), E1rm.estimate(150.0, 5, 8.0))), trend)
    }

    @Test
    fun weeklyTrend_sortedAndRespectsUntil() {
        val h = history {
            session(14) { squat(100.0, 5) }
            session(0) { squat(90.0, 5) }
            session(21) { squat(110.0, 5) }
        }
        val trend = HistoryStats(h).weeklyTrend(MainLift.SQUAT, day(14))
        assertEquals(listOf(day(0), day(14)), trend.map { it.weekStart })
    }

    @Test
    fun currentE1rm_usesBaselineWhenNoSets() {
        val h = history { baseline(Ids.BENCH, 120.0) }
        assertEquals(120.0, HistoryStats(h).currentE1rm(MainLift.BENCH, day(5)))
    }

    @Test
    fun currentE1rm_windowIsHalfOpen() {
        val h = history {
            session(0) { bench(100.0, 5) }
            session(28) { bench(90.0, 5) }
        }
        val stats = HistoryStats(h)
        // day 0 is exactly 28 days before day 28: outside (until - 28, until]
        assertEquals(E1rm.estimate(90.0, 5, 8.0), stats.currentE1rm(MainLift.BENCH, day(28))!!, 1e-9)
        assertEquals(E1rm.estimate(100.0, 5, 8.0), stats.currentE1rm(MainLift.BENCH, day(27))!!, 1e-9)
    }

    @Test
    fun sessionCount_countsMainLiftSessionsOnly() {
        val h = history {
            session(0) { bench(100.0, 5) }
            session(2) { set(Ids.PAUSE_BENCH, 80.0, 5) }
            session(4) { bench(100.0, 5); bench(90.0, 5) }
        }
        assertEquals(2, HistoryStats(h).sessionCount(MainLift.BENCH))
    }

    @Test
    fun lastAndPreviousTopSet() {
        val h = history {
            session(0) { squat(100.0, 5) }
            session(3) { set(Ids.SQUAT, 140.0, 3); set(Ids.SQUAT, 120.0, 5) }
            session(7) { squat(110.0, 5) }
        }
        val stats = HistoryStats(h)
        assertEquals(day(3) to 140.0, stats.lastTopSet(Ids.SQUAT, day(7))!!.let { it.first to it.second.weightKg })
        assertEquals(day(0), stats.previousTopSet(Ids.SQUAT, day(7))!!.first)
        assertEquals(day(7), stats.lastTopSet(Ids.SQUAT, day(8))!!.first)
        assertNull(stats.previousTopSet(Ids.SQUAT, day(3)))
    }

    @Test
    fun topSet_prefersFlagThenHeaviestNonFailed() {
        val h = history {
            session(0) {
                set(Ids.BENCH, 120.0, 1, failed = true)
                set(Ids.BENCH, 100.0, 5)
            }
            session(2) { set(Ids.BENCH, 90.0, 3, failed = true) }
        }
        val stats = HistoryStats(h)
        assertEquals(100.0, stats.lastTopSet(Ids.BENCH, day(1))!!.second.weightKg)
        assertEquals(90.0, stats.lastTopSet(Ids.BENCH, day(3))!!.second.weightKg)
    }

    @Test
    fun returnDateAfterGap() {
        val gap = history {
            session(0) { squat(100.0, 5) }
            session(3) { squat(100.0, 5) }
            session(20) { squat(100.0, 5) }
            session(23) { squat(100.0, 5) }
        }
        assertEquals(day(20), HistoryStats(gap).returnDateAfterGap(day(30)))
        assertNull(HistoryStats(gap).returnDateAfterGap(day(10)))
        val noGap = history {
            session(0) { squat(100.0, 5) }
            session(3) { squat(100.0, 5) }
            session(7) { squat(100.0, 5) }
        }
        assertNull(HistoryStats(noGap).returnDateAfterGap(day(30)))
    }

    @Test
    fun lastSessionDate_isStrictlyBefore() {
        val h = history { session(0) { squat(100.0, 5) }; session(3) { squat(100.0, 5) } }
        val stats = HistoryStats(h)
        assertEquals(day(0), stats.lastSessionDate(day(3)))
        assertNull(stats.lastSessionDate(day(0)))
    }

    @Test
    fun missPoints_includesVariations() {
        val h = history {
            session(0) {
                bench(100.0, 3, failed = true, miss = MissPoint.MID)
                set(Ids.PAUSE_BENCH, 80.0, 5, failed = true, miss = MissPoint.FLOOR)
                squat(150.0, 3, failed = true, miss = MissPoint.LOCKOUT)
            }
            session(30) { bench(100.0, 3, failed = true, miss = MissPoint.LOCKOUT) }
        }
        val points = HistoryStats(h).missPoints(MainLift.BENCH, day(0), day(10))
        assertEquals(listOf(MissPoint.MID, MissPoint.FLOOR), points)
    }
}
