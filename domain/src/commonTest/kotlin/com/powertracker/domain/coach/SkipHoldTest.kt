package com.powertracker.domain.coach

import com.powertracker.domain.GoldenScenario
import com.powertracker.domain.HistoryBuilder
import com.powertracker.domain.Ids
import com.powertracker.domain.history
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The pain hold: after a pain skip, on either side, an exercise's weight does not go up until a session holding a set
 * of it is logged after the skip, on either side. Held, the target repeats the latest session on the side targeted.
 */
class SkipHoldTest {
    private val start = LocalDate(2026, 1, 5) // a Monday, day 0
    private fun day(n: Int): LocalDate = start.plus(n, DateTimeUnit.DAY)
    private val today = day(7) // the Monday after
    private val settings = CoachSettings()

    private fun skip(dayOffset: Int, reason: SkipReason?, exerciseId: Long, mode: TrainingMode) =
        Skip(dayOffset.toLong() + 1, day(dayOffset), mode, exerciseId, null, reason)

    private fun Advice.target() = payload as AdvicePayload.Target

    // ---- powerlifting ----

    private val squatItem = PlanItem(1, DayOfWeek.MONDAY, Ids.SQUAT, 5, 5, 8.0, 0)

    /** Squat 100 x 5 @ 7 on days 0, 2 and 4: without a hold the next target is 105 x 5. */
    private fun squats(more: HistoryBuilder.() -> Unit = {}): TrainingHistory = history(start) {
        for (d in listOf(0, 2, 4)) session(d) { squat(100.0, 5, rpe = 7.0) }
        more()
    }

    private fun pl(h: TrainingHistory, skips: List<Skip>) =
        Coaching.targetsFor(POWERLIFTING, DayOfWeek.MONDAY, h, listOf(squatItem), settings, today, emptyList(), skips).single()

    @Test
    fun painSkip_holdsTargetWeight() {
        val h = squats()
        assertEquals(105.0, pl(h, emptyList()).target().weightKg)
        val held = pl(h, listOf(skip(5, SkipReason.PAIN, Ids.SQUAT, POWERLIFTING)))
        assertEquals(AdvicePayload.Target(Ids.SQUAT, 100.0, 5, 8.0), held.payload)
        assertEquals("Last time: 100 × 5 @ 7. Coach suggests 100 × 5 @ 8.", held.reason)
    }

    @Test
    fun painSkip_afterAMissedTopSet_repeatsTheWeight() {
        // Progression would take 5% off a missed top set; held, no decision is made and the weight repeats.
        val h = history(start) {
            session(0) { squat(100.0, 5, rpe = 7.0) }
            session(2) { squat(100.0, 5, rpe = 7.0) }
            session(4) { squat(100.0, 5, failed = true) }
        }
        assertEquals(95.0, pl(h, emptyList()).target().weightKg)
        assertEquals(100.0, pl(h, listOf(skip(5, SkipReason.PAIN, Ids.SQUAT, POWERLIFTING))).target().weightKg)
    }

    @Test
    fun painSkip_keepsTheLastRepsWhenTheyWereAbovePlan() {
        // A curl progressed by reps to 10 on a plan of 8: held, the aim stays at 10, not back to 8.
        val curl = PlanItem(3, DayOfWeek.MONDAY, Ids.CURL, 3, 8, 8.0, 0)
        val h = history(start) { session(4) { set(Ids.CURL, 12.0, 10, isTopSet = true) } }
        val held = Coaching.targetsFor(POWERLIFTING, DayOfWeek.MONDAY, h, listOf(curl), settings, today, emptyList(), listOf(skip(5, SkipReason.PAIN, Ids.CURL, POWERLIFTING))).single()
        assertEquals(AdvicePayload.Target(Ids.CURL, 12.0, 10, 8.0), held.payload)
    }

    @Test
    fun painSkip_onOtherSide_holdsToo() {
        assertEquals(100.0, pl(squats(), listOf(skip(5, SkipReason.PAIN, Ids.SQUAT, BODYBUILDING))).target().weightKg)
    }

    @Test
    fun loggedSessionAfterSkip_releasesHold() {
        // Logged on the powerlifting side after the skip.
        assertEquals(105.0, pl(squats(), listOf(skip(3, SkipReason.PAIN, Ids.SQUAT, POWERLIFTING))).target().weightKg)
        // Logged on the bodybuilding side after the skip: released, and the target is what it would be with no skip.
        val h = squats { session(6, mode = BODYBUILDING) { set(Ids.SQUAT, 80.0, 10) } }
        assertEquals(pl(h, emptyList()), pl(h, listOf(skip(5, SkipReason.PAIN, Ids.SQUAT, POWERLIFTING))))
        // A session on the skip's own date does not release it.
        val sameDay = squats { session(5, mode = BODYBUILDING) { set(Ids.SQUAT, 80.0, 10) } }
        assertEquals(100.0, pl(sameDay, listOf(skip(5, SkipReason.PAIN, Ids.SQUAT, POWERLIFTING))).target().weightKg)
    }

    @Test
    fun busySkip_doesNotHold() {
        val h = squats()
        for (reason in listOf(SkipReason.BUSY, SkipReason.TIME, null)) {
            assertEquals(pl(h, emptyList()), pl(h, listOf(skip(5, reason, Ids.SQUAT, POWERLIFTING))), "$reason")
        }
        // Nor does another exercise's pain skip.
        assertEquals(pl(h, emptyList()), pl(h, listOf(skip(5, SkipReason.PAIN, Ids.BENCH, POWERLIFTING))))
    }

    @Test
    fun noSkips_targetsUnchanged() {
        // The golden scenario, every day for 100 days: no skips, or skips that hold nothing, change no target.
        val plan = GoldenScenario.plan
        for (d in 0..100) {
            val now = GoldenScenario.day(d)
            val h = GoldenScenario.history.copy(sessions = GoldenScenario.history.sessions.filter { it.date <= now })
            val prior = GoldenScenario.checkpoints.firstOrNull { it.today == now }?.storedAdvice.orEmpty()
            val harmless = plan.map { it.exerciseId }.distinct().flatMap { id ->
                listOf(SkipReason.BUSY, SkipReason.TIME, null).map { r -> Skip(id, now, POWERLIFTING, id, null, r) }
            }
            for (weekday in DayOfWeek.entries) {
                val coach = Coach.targetsFor(weekday, h, plan, GoldenScenario.settings, now, prior)
                assertEquals(coach, Coaching.targetsFor(POWERLIFTING, weekday, h, plan, GoldenScenario.settings, now, prior, emptyList()))
                assertEquals(coach, Coaching.targetsFor(POWERLIFTING, weekday, h, plan, GoldenScenario.settings, now, prior, harmless))
            }
        }
    }

    // ---- bodybuilding ----

    private val pressItem = PlanItem(2, DayOfWeek.MONDAY, Ids.DB_PRESS, 3, 8, 8.0, 0, BODYBUILDING, 12)

    /** DB press 26 x 12, 12, 12 on day 0: every set at the top, so without a hold the weight goes up. */
    private val topOfRange = history(start) {
        session(0, mode = BODYBUILDING) { repeat(3) { set(Ids.DB_PRESS, 26.0, 12) } }
    }

    private fun bb(h: TrainingHistory, skips: List<Skip>) =
        Coaching.targetsFor(BODYBUILDING, DayOfWeek.MONDAY, h, listOf(pressItem), settings, today, emptyList(), skips).single()

    @Test
    fun bodybuilding_painSkip_holdsTargetWeight() {
        assertEquals(26.5, bb(topOfRange, emptyList()).target().weightKg)
        val held = bb(topOfRange, listOf(skip(3, SkipReason.PAIN, Ids.DB_PRESS, BODYBUILDING)))
        assertEquals(AdvicePayload.Target(Ids.DB_PRESS, 26.0, 8, 8.0, null, 12), held.payload)
        assertEquals("Last time: 26 × 12, 12, 12. Beat it. Coach suggests 26 × 8–12 @ 8.", held.reason)
    }

    @Test
    fun painSkip_keepsRepsAim_noResetToLowEnd() {
        // After an earned increase the reps field starts again at the low end; held, the weight stays and so does the aim.
        assertEquals(8, BodybuildingCoach.repsToBeat(pressItem, topOfRange, 1, today))
        val skips = listOf(skip(3, SkipReason.PAIN, Ids.DB_PRESS, POWERLIFTING))
        for (set in 1..3) assertEquals(12, BodybuildingCoach.repsToBeat(pressItem, topOfRange, set, today, skips), "set $set")
        // A set not done last time still starts at the low end.
        assertEquals(8, BodybuildingCoach.repsToBeat(pressItem, topOfRange, 4, today, skips))
        // Below the top it is last time's reps + 1, as without a hold.
        val short = history(start) { session(0, mode = BODYBUILDING) { set(Ids.DB_PRESS, 26.0, 11); set(Ids.DB_PRESS, 26.0, 10) } }
        assertEquals(12, BodybuildingCoach.repsToBeat(pressItem, short, 1, today, skips))
        assertEquals(11, BodybuildingCoach.repsToBeat(pressItem, short, 2, today, skips))
    }

    @Test
    fun todaysOwnSession_doesNotReleaseTheHoldMidSession() {
        // Today's first set is logged: the target stored this morning was held, and set 2 keeps its aim.
        val skips = listOf(skip(3, SkipReason.PAIN, Ids.DB_PRESS, BODYBUILDING))
        val midSession = history(start) {
            session(0, mode = BODYBUILDING) { repeat(3) { set(Ids.DB_PRESS, 26.0, 12) } }
            session(7, mode = BODYBUILDING) { set(Ids.DB_PRESS, 26.0, 12) }
        }
        assertEquals(12, BodybuildingCoach.repsToBeat(pressItem, midSession, 2, today, skips))
        assertEquals(26.0, bb(midSession, skips).target().weightKg)
        // The day after, that session releases it.
        assertEquals(true, SkipCoach.held(Ids.DB_PRESS, skips, midSession, today))
        assertEquals(false, SkipCoach.held(Ids.DB_PRESS, skips, midSession, today.plus(1, DateTimeUnit.DAY)))
    }

    @Test
    fun bodybuilding_painSkip_onOtherSide_holdsToo() {
        assertEquals(26.0, bb(topOfRange, listOf(skip(3, SkipReason.PAIN, Ids.DB_PRESS, POWERLIFTING))).target().weightKg)
    }

    @Test
    fun bodybuilding_loggedSessionAfterSkip_releasesHold() {
        val h = history(start) {
            session(0, mode = BODYBUILDING) { repeat(3) { set(Ids.DB_PRESS, 26.0, 12) } }
            session(4) { set(Ids.DB_PRESS, 30.0, 5) }
        }
        val skips = listOf(skip(3, SkipReason.PAIN, Ids.DB_PRESS, BODYBUILDING))
        assertEquals(bb(h, emptyList()), bb(h, skips))
        assertEquals(26.5, bb(h, skips).target().weightKg)
        assertEquals(BodybuildingCoach.repsToBeat(pressItem, h, 1, today), BodybuildingCoach.repsToBeat(pressItem, h, 1, today, skips))
    }

    @Test
    fun bodybuilding_busySkip_doesNotHold() {
        for (reason in listOf(SkipReason.BUSY, SkipReason.TIME, null)) {
            val skips = listOf(skip(3, reason, Ids.DB_PRESS, BODYBUILDING))
            assertEquals(bb(topOfRange, emptyList()), bb(topOfRange, skips), "$reason")
            assertEquals(8, BodybuildingCoach.repsToBeat(pressItem, topOfRange, 1, today, skips), "$reason")
        }
    }
}
