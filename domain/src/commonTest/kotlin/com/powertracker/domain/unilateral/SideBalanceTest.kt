package com.powertracker.domain.unilateral

import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.Side
import com.powertracker.domain.model.SideLog
import com.powertracker.domain.model.SplitSides
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.progress.SessionRecapMath
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SideBalanceTest {
    private val pecDeck = Exercise(10, "Pec deck", Category.ACCESSORY, null, listOf("Chest"), isBuiltIn = true, singleSided = true)
    private val bench = Exercise(2, "Bench", Category.BENCH, null, listOf("Chest"), isBuiltIn = true)

    @Test
    fun detectsLeftTrailingRightAcrossRecentSessions() {
        val s1 = LoggedSession(
            id = 1,
            date = LocalDate(2026, 11, 20),
            bodyweightKg = null,
            readiness = null,
            sets = listOf(
                SingleSided.combine(10, SideLog(40.0, 12, 8.0, false), SideLog(40.0, 10, 8.5, false)),
                SingleSided.combine(10, SideLog(40.0, 12, 8.5, false), SideLog(40.0, 10, 9.0, false)),
            ),
            mode = TrainingMode.BODYBUILDING,
        )
        val history = TrainingHistory(sessions = listOf(s1), exercises = mapOf(10L to pecDeck), baselines = emptyList())
        val rows = SideBalance.rows(history)
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals(Side.LEFT, row.weakerSide)
        assertEquals(2.0, row.avgRepGap)
        assertEquals("Left trailing Right by ~2 reps (last session)", row.summary)
    }

    @Test
    fun reportsBalancedWhenGapBelowThreshold() {
        val s1 = LoggedSession(
            id = 1,
            date = LocalDate(2026, 11, 20),
            bodyweightKg = null,
            readiness = null,
            sets = listOf(
                SingleSided.combine(10, SideLog(40.0, 12, 8.0, false), SideLog(40.0, 12, 8.0, false)),
            ),
            mode = TrainingMode.BODYBUILDING,
        )
        val history = TrainingHistory(sessions = listOf(s1), exercises = mapOf(10L to pecDeck), baselines = emptyList())
        val row = SideBalance.rows(history).single()
        assertNull(row.weakerSide)
        assertEquals(0.0, row.avgRepGap)
        assertEquals("Balanced across last session", row.summary)
    }

    @Test
    fun sessionRecapComputesTonnageAndPrCount() {
        val prior = TrainingHistory(
            sessions = listOf(
                LoggedSession(
                    id = 1,
                    date = LocalDate(2026, 11, 20),
                    bodyweightKg = null,
                    readiness = null,
                    sets = listOf(LoggedSet(2, 100.0, 5, 8.0, false, null, true)),
                    mode = TrainingMode.POWERLIFTING,
                ),
            ),
            exercises = mapOf(2L to bench, 10L to pecDeck),
            baselines = emptyList(),
        )
        val todaySets = listOf(
            LoggedSet(2, 105.0, 5, 8.0, false, null, true), // PR! 525 kg
            LoggedSet(10, 40.0, 10, 8.0, false, null, false), // 400 kg
            LoggedSet(10, 40.0, 10, 10.0, true, null, false), // failed -> excluded from tonnage
            LoggedSet(10, 8.0, 12, 8.0, false, null, false, unit = LoadUnit.PLATES), // plates -> excluded from kg tonnage
        )
        val recap = SessionRecapMath.of(prior, todaySets, LocalDate(2026, 11, 28), TrainingMode.POWERLIFTING)
        assertEquals(925.0, recap.tonnageKg)
        assertEquals(1, recap.prCount)
    }
}
