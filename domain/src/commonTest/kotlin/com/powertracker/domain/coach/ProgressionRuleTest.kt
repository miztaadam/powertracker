package com.powertracker.domain.coach

import com.powertracker.domain.Ids
import com.powertracker.domain.history
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class ProgressionRuleTest {
    private val today = LocalDate(2026, 2, 2)
    private val settings = CoachSettings()

    private fun item(exerciseId: Long, reps: Int = 5, rpe: Double = 8.0) =
        PlanItem(1, DayOfWeek.MONDAY, exerciseId, 3, reps, rpe, 0)

    private fun run(h: TrainingHistory, id: Long, reps: Int = 5) =
        ProgressionRule.target(item(id, reps), h.exercises.getValue(id), HistoryStats(h), settings, today)

    private fun target(advice: com.powertracker.domain.model.Advice?) = advice!!.payload as AdvicePayload.Target

    @Test
    fun squatBelowPlan_addsFiveKg() {
        val h = history { session(0) { squat(150.0, 5, rpe = 7.0) } }
        val a = run(h, Ids.SQUAT)!!
        assertEquals(155.0, target(a).weightKg)
        assertEquals(5, target(a).reps)
        assertEquals(8.0, target(a).rpe)
        assertEquals(AdviceType.TARGET, a.type)
        assertEquals(MainLift.SQUAT, a.lift)
        assertEquals(0, a.severity)
        assertEquals(today, a.createdAt)
        assertEquals("Last time: 150 × 5 @ 7. Coach suggests 155 × 5 @ 8.", a.reason)
    }

    @Test
    fun benchBelowPlan_addsTwoPointFive() {
        val h = history { session(0) { bench(100.0, 5, rpe = 7.0) } }
        assertEquals(102.5, target(run(h, Ids.BENCH)).weightKg)
    }

    @Test
    fun onTarget_addsTwoPointFive() {
        val h = history { session(0) { squat(150.0, 5, rpe = 8.0) } }
        assertEquals(152.5, target(run(h, Ids.SQUAT)).weightKg)
    }

    @Test
    fun recentJumpWithRisingRpe_repeats() {
        val h = history {
            session(0) { squat(147.5, 5, rpe = 7.5) }
            session(10) { squat(150.0, 5, rpe = 8.0) }
        }
        assertEquals(150.0, target(run(h, Ids.SQUAT)).weightKg)
    }

    @Test
    fun slightlyOverPlan_repeats() {
        val h = history { session(0) { squat(150.0, 5, rpe = 9.0) } }
        assertEquals(150.0, target(run(h, Ids.SQUAT)).weightKg)
    }

    @Test
    fun farOverPlan_dropsTwoPointFivePercent() {
        val h = history { session(0) { squat(150.0, 5, rpe = 9.5) } }
        assertEquals(145.0, target(run(h, Ids.SQUAT)).weightKg)
    }

    @Test
    fun failedSet_dropsFivePercent() {
        val h = history { session(0) { squat(150.0, 5, rpe = null, failed = true) } }
        val a = run(h, Ids.SQUAT)
        assertEquals(142.5, target(a).weightKg)
        assertEquals("Last time: 150 × 5 (missed). Coach suggests 142.5 × 5 @ 8.", a!!.reason)
    }

    @Test
    fun nullRpe_treatedAsOnTarget() {
        val h = history { session(0) { squat(150.0, 5, rpe = null) } }
        val a = run(h, Ids.SQUAT)
        assertEquals(152.5, target(a).weightKg)
        assertEquals("Last time: 150 × 5. Coach suggests 152.5 × 5 @ 8.", a!!.reason)
    }

    @Test
    fun capBlocksLoadJump_addsRep() {
        val h = history { session(0) { set(Ids.CURL, 20.0, 10, rpe = 6.0, isTopSet = true) } }
        val t = target(run(h, Ids.CURL, reps = 10))
        assertEquals(20.0, t.weightKg)
        assertEquals(11, t.reps)
    }

    @Test
    fun repCeilingIsTwelve() {
        val h = history { session(0) { set(Ids.CURL, 20.0, 12, rpe = 6.0, isTopSet = true) } }
        val t = target(run(h, Ids.CURL, reps = 12))
        assertEquals(20.0, t.weightKg)
        assertEquals(12, t.reps)
    }

    @Test
    fun neverMoreThanFivePercentAboveLast() {
        for (last in listOf(20.0, 22.5, 47.5, 100.0, 150.0, 182.5)) {
            for (rpe in listOf(5.0, 7.0, 8.0, 9.0, 10.0)) {
                val h = history { session(0) { set(Ids.CURL, last, 8, rpe = rpe, isTopSet = true) } }
                val w = target(run(h, Ids.CURL, reps = 8)).weightKg
                assertTrue(w <= last * 1.05 + 1e-9, "last=$last rpe=$rpe got $w")
            }
        }
    }

    @Test
    fun noPriorSet_returnsNull() {
        val h = history { session(0) { bench(100.0, 5) } }
        assertNull(run(h, Ids.SQUAT))
    }
    // --- final review: sub-bar and bodyweight loads are not floored at the empty bar ---

    @Test
    fun subBarAccessory_curlTwelveKgStaysTwelve_addsRep() {
        val h = history { session(0) { set(Ids.CURL, 12.0, 10, rpe = 8.0, isTopSet = true) } }
        val t = target(run(h, Ids.CURL, reps = 10))
        assertEquals(12.0, t.weightKg)
        assertEquals(11, t.reps)
    }

    @Test
    fun bodyweightPullUp_zeroStaysZero() {
        val h = history { session(0) { set(Ids.PULL_UP, 0.0, 8, rpe = 8.0, isTopSet = true) } }
        val t = target(run(h, Ids.PULL_UP, reps = 8))
        assertEquals(0.0, t.weightKg)
        assertEquals(9, t.reps)
        val failed = history { session(0) { set(Ids.PULL_UP, 0.0, 8, rpe = null, failed = true, isTopSet = true) } }
        assertEquals(0.0, target(run(failed, Ids.PULL_UP, reps = 8)).weightKg)
    }

    @Test
    fun dbPressFifteenAtRpeSix_neverAboveFivePercent_staysFifteenPlusRep() {
        val h = history { session(0) { set(Ids.DB_PRESS, 15.0, 10, rpe = 6.0, isTopSet = true) } }
        val t = target(run(h, Ids.DB_PRESS, reps = 10))
        assertTrue(t.weightKg <= 15.75, "got ${t.weightKg}")
        assertEquals(15.0, t.weightKg)
        assertEquals(11, t.reps)
    }

    @Test
    fun subBarAccessory_neverAboveFivePercent_anyRpe() {
        for (last in listOf(0.0, 5.0, 8.0, 12.0, 14.0, 15.0, 17.5, 19.0)) {
            for (rpe in listOf(5.0, 7.0, 8.0, 9.0, 10.0)) {
                val h = history { session(0) { set(Ids.CURL, last, 8, rpe = rpe, isTopSet = true) } }
                val w = target(run(h, Ids.CURL, reps = 8)).weightKg
                assertTrue(w <= last * 1.05 + 1e-9, "last=$last rpe=$rpe got $w")
            }
        }
    }

    @Test
    fun mainLift_keepsBarFloor() {
        val h = history { session(0) { bench(20.0, 5, rpe = null, failed = true) } }
        assertEquals(20.0, target(run(h, Ids.BENCH)).weightKg)
    }
}
