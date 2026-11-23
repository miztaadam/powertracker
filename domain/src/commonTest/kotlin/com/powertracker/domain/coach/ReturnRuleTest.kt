package com.powertracker.domain.coach

import com.powertracker.domain.Ids
import com.powertracker.domain.SessionBuilder
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Coming back to a side after time on the other: the first target is held to what recent training supports, and a
 * lift not trained at all for 28 days is eased in, never its old top set plus an increase.
 */
class ReturnRuleTest {
    private val start = LocalDate(2026, 1, 5)

    /** Day 56, a Monday: deadlift was last pulled on day 14, 42 days before. */
    private val today = start.plus(56, DateTimeUnit.DAY)
    private val settings = CoachSettings()
    private val deadliftItem = PlanItem(1, DayOfWeek.MONDAY, Ids.DEADLIFT, 3, 5, 8.0, 0)

    private val rdl = Exercise(RDL, "Romanian deadlift", Category.VARIATION, MainLift.DEADLIFT, emptyList(), true)

    /**
     * Deadlift 180 × 5 @ 8 on the powerlifting side on days 0, 7 and 14, then a session every week on days 21 to 49
     * on [otherWeeks]' side, each holding [weekly]; nothing in the whole log is 14 days apart.
     */
    private fun returning(otherWeeks: TrainingMode, weekly: SessionBuilder.(day: Int) -> Unit): TrainingHistory {
        val h = history(start) {
            for (d in listOf(0, 7, 14)) session(d) { deadlift(180.0, 5) }
            for (d in 21..49 step 7) session(d, mode = otherWeeks) { weekly(d) }
        }
        return h.copy(exercises = h.exercises + (RDL to rdl))
    }

    private fun deadliftTarget(h: TrainingHistory) =
        Coach.targetsFor(DayOfWeek.MONDAY, h, listOf(deadliftItem), settings, today, emptyList()).single()

    /** What the powerlifting coach would prescribe from its own side's log alone, before the return rule. */
    private fun progression(h: TrainingHistory) = ProgressionRule.target(
        deadliftItem, h.exercises.getValue(Ids.DEADLIFT), HistoryStats(h.on(TrainingMode.POWERLIFTING)), settings, today,
    )!!.payload as AdvicePayload.Target

    @Test
    fun aVariationOnTheOtherSideCapsTheFirstTargetBack() {
        val h = returning(TrainingMode.BODYBUILDING) { d ->
            set(Ids.DB_PRESS, 26.0, 10)
            if (d == 49) repeat(3) { set(RDL, 140.0, 10, rpe = 8.0) }
        }
        val incoming = progression(h)
        assertEquals(182.5, incoming.weightKg)

        val supported = E1rm.loadFor(E1rm.estimate(140.0, 10, 8.0), 5, 8.0)
        val target = deadliftTarget(h).payload as AdvicePayload.Target
        assertTrue(target.weightKg <= supported, "${target.weightKg} > $supported")
        assertTrue(target.weightKg <= incoming.weightKg)
        assertEquals(ProgressionRule.roundDown(supported, incoming.weightKg, free = false, changePlates = false), target.weightKg)
        assertEquals(165.0, target.weightKg)
        assertEquals(5, target.reps)
        assertEquals(
            "Last time: 180 × 5 @ 8. Coach suggests 165 × 5 @ 8. Easing back in: lighter load.",
            deadliftTarget(h).reason,
        )
    }

    @Test
    fun aCapThatLowersTheWeightSaysWhyItIsLighter() {
        val h = returning(TrainingMode.BODYBUILDING) { d ->
            set(Ids.DB_PRESS, 26.0, 10)
            if (d == 49) repeat(3) { set(RDL, 140.0, 10, rpe = 8.0) }
        }
        val advice = deadliftTarget(h)
        assertTrue((advice.payload as AdvicePayload.Target).weightKg < progression(h).weightKg)
        assertTrue(advice.reason.endsWith(Coach.RE_ENTRY_NOTE), advice.reason)
        // The note stays outside the "Coach suggests ..." sentence the Today card strips.
        assertEquals("Last time: 180 × 5 @ 8. Easing back in: lighter load.", stripSuggestion(advice.reason))
    }

    @Test
    fun aCapThatDoesNotLowerTheWeightLeavesTheReasonAsItCame() {
        val h = returning(TrainingMode.BODYBUILDING) { d ->
            if (d == 49) set(RDL, 200.0, 8, rpe = 8.0) else set(Ids.DB_PRESS, 26.0, 10)
        }
        val advice = deadliftTarget(h)
        assertEquals(progression(h).weightKg, (advice.payload as AdvicePayload.Target).weightKg)
        assertTrue(!advice.reason.contains("Easing back in"), advice.reason)
        assertEquals(ProgressionRule.target(
            deadliftItem, h.exercises.getValue(Ids.DEADLIFT), HistoryStats(h.on(TrainingMode.POWERLIFTING)), settings, today,
        )!!.reason, advice.reason)
    }

    /** The apps' SUGGESTION_SENTENCE (ExerciseCard.kt / ExerciseCard.tsx), applied as the Today card applies it. */
    private fun stripSuggestion(reason: String) =
        reason.replace(Regex("""\s*Coach suggests .*? @ \d+(\.\d+)?\."""), "").trim()

    @Test
    fun recentSetsThatSupportMoreLeaveTheTargetAsItCame() {
        val h = returning(TrainingMode.BODYBUILDING) { d ->
            if (d == 49) set(RDL, 200.0, 8, rpe = 8.0) else set(Ids.DB_PRESS, 26.0, 10)
        }
        assertEquals(progression(h).weightKg, (deadliftTarget(h).payload as AdvicePayload.Target).weightKg)
        assertEquals("Last time: 180 × 5 @ 8. Coach suggests 182.5 × 5 @ 8.", deadliftTarget(h).reason)
    }

    @Test
    fun aLiftUntrainedOnEitherSideIsEasedInNeverItsOldTopSetPlusAnIncrease() {
        val h = returning(TrainingMode.BODYBUILDING) { set(Ids.DB_PRESS, 26.0, 10) }
        val advice = deadliftTarget(h)
        val target = advice.payload as AdvicePayload.Target
        val expected = ProgressionRule.roundDown(180.0 * AdvicePayload.ReEntry().loadFactor, 180.0, free = false, changePlates = false)
        assertEquals(152.5, expected)
        assertEquals(expected, target.weightKg)
        assertNotEquals(182.5, target.weightKg)
        assertEquals(5, target.reps)
        assertEquals("Last time: 180 × 5 @ 8. Coach suggests 152.5 × 5 @ 8. Easing back in: lighter load.", advice.reason)
    }

    @Test
    fun aLiftTrainedOnThisSideWithinTheWindowIsUntouched() {
        val h = returning(TrainingMode.BODYBUILDING) { set(Ids.DB_PRESS, 26.0, 10) }.let { h ->
            // Deadlift again on the powerlifting side on day 36, 20 days ago.
            val extra = history(start) { session(36) { deadlift(180.0, 5) } }.sessions.single().copy(id = 99)
            h.copy(sessions = (h.sessions + extra).sortedBy { it.date })
        }
        assertEquals(progression(h), deadliftTarget(h).payload)
    }

    @Test
    fun withNoRecentTimeOnTheOtherSideThePowerliftingCoachIsAsBefore() {
        // Every session is on the powerlifting side: the return rule does not apply, so a powerlifting-only log
        // gets exactly what it got before the bodybuilding side existed.
        val h = returning(TrainingMode.POWERLIFTING) { squat(140.0, 5) }
        assertEquals(progression(h), deadliftTarget(h).payload)
        assertEquals(182.5, progression(h).weightKg)
    }

    @Test
    fun easeOnTheBodybuildingSideCountsTheExerciseItselfNotItsFamily() {
        // Deadlift on the powerlifting side last week, Romanian deadlift last done on the bodybuilding side 42 days
        // ago: for the bodybuilding side the Romanian deadlift is its own family, untrained in 28 days on either side.
        val h = history(start) {
            for (d in listOf(0, 7, 14)) session(d, mode = TrainingMode.BODYBUILDING) { repeat(3) { set(RDL, 100.0, 10, isTopSet = it == 0) } }
            for (d in 21..49 step 7) session(d) { deadlift(180.0, 5) }
        }.let { it.copy(exercises = it.exercises + (RDL to rdl)) }
        val item = PlanItem(9, DayOfWeek.MONDAY, RDL, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)
        val incoming = Advice(
            AdviceType.TARGET, MainLift.DEADLIFT, today,
            "Last time: 100 × 10, 10, 10. Time to add weight. Coach suggests 102.5 × 8–12 @ 8.", 0,
            AdvicePayload.Target(RDL, 102.5, 8, 8.0, repsHigh = 12),
        )
        val eased = ReturnRule.ease(incoming, item, rdl, h, TrainingMode.BODYBUILDING, settings, today)
        val payload = eased.payload as AdvicePayload.Target
        assertEquals(85.0, payload.weightKg)
        assertEquals(8, payload.reps)
        assertEquals(12, payload.repsHigh)
        assertEquals(
            "Last time: 100 × 10, 10, 10. Time to add weight. Coach suggests 85 × 8–12 @ 8. Easing back in: lighter load.",
            eased.reason,
        )
    }

    @Test
    fun aPowerliftingSideNeverTrainedIsNotEasedBack() {
        // Left in Settings before any powerlifting session: only a baseline on that side, and Romanian deadlifts on
        // the bodybuilding side every week since. Nothing to come back to, so a target is left as it came.
        val h = history(start) {
            baseline(Ids.DEADLIFT, 230.0)
            for (d in 21..49 step 7) session(d, mode = TrainingMode.BODYBUILDING) { repeat(3) { set(RDL, 140.0, 10, rpe = 8.0) } }
        }.let { it.copy(exercises = it.exercises + (RDL to rdl)) }
        val incoming = Advice(
            AdviceType.TARGET, MainLift.DEADLIFT, today, "Coach suggests 190 × 5 @ 8.", 0,
            AdvicePayload.Target(Ids.DEADLIFT, 190.0, 5, 8.0),
        )
        val supported = E1rm.loadFor(E1rm.estimate(140.0, 10, 8.0), 5, 8.0)
        assertTrue(190.0 > supported, "the case needs a cap that would lower it: $supported")
        assertEquals(incoming, ReturnRule.ease(incoming, deadliftItem, h.exercises.getValue(Ids.DEADLIFT), h, TrainingMode.POWERLIFTING, settings, today))
        // Through the coach the side has no top set to progress from, so no target at all, eased or not.
        assertTrue(Coach.targetsFor(DayOfWeek.MONDAY, h, listOf(deadliftItem), settings, today, emptyList()).isEmpty())
    }

    @Test
    fun aBodybuildingSideNeverTrainedIsNotEasedBack() {
        // Powerlifting every week, DB press among it; the bodybuilding side has no session at all.
        val h = history(start) {
            for (d in 21..49 step 7) session(d) { deadlift(180.0, 5); repeat(3) { set(Ids.DB_PRESS, 26.0, 10, rpe = 8.0) } }
        }
        val item = PlanItem(9, DayOfWeek.MONDAY, Ids.DB_PRESS, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)
        val incoming = Advice(
            AdviceType.TARGET, null, today, "First time on this side. Coach suggests 40 × 8–12 @ 8.", 0,
            AdvicePayload.Target(Ids.DB_PRESS, 40.0, 8, 8.0, repsHigh = 12),
        )
        assertEquals(incoming, ReturnRule.ease(incoming, item, h.exercises.getValue(Ids.DB_PRESS), h, TrainingMode.BODYBUILDING, settings, today))
    }

    @Test
    fun aSideWithASessionIsStillEasedBack() {
        // The same baseline and week-by-week bodybuilding as the never-trained case, after one powerlifting session.
        val h = history(start) {
            baseline(Ids.DEADLIFT, 230.0)
            session(0) { deadlift(180.0, 5) }
            for (d in 21..49 step 7) session(d, mode = TrainingMode.BODYBUILDING) { repeat(3) { set(RDL, 140.0, 10, rpe = 8.0) } }
        }.let { it.copy(exercises = it.exercises + (RDL to rdl)) }
        val advice = deadliftTarget(h)
        assertTrue((advice.payload as AdvicePayload.Target).weightKg < progression(h).weightKg)
        assertTrue(advice.reason.endsWith(Coach.RE_ENTRY_NOTE), advice.reason)
    }

    private companion object {
        const val RDL = 60L
    }
}
