package com.powertracker.domain.coach

import com.powertracker.domain.Ids
import com.powertracker.domain.SessionBuilder
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plates.PlateLoader
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reps first, then weight: the bodybuilding side's targets (spec 5.3). */
class BodybuildingCoachTest {
    private val start = LocalDate(2026, 1, 5)

    /** Day 7, a Monday: "last time" is day 0, the Monday before. */
    private val today = start.plus(7, DateTimeUnit.DAY)
    private val settings = CoachSettings()

    private fun item(exerciseId: Long, sets: Int = 3, low: Int = 8, high: Int = 12, id: Long = 1) =
        PlanItem(id, DayOfWeek.MONDAY, exerciseId, sets, low, 8.0, 0, TrainingMode.BODYBUILDING, high)

    /** One bodybuilding session on day 0 holding [block]. */
    private fun lastWeek(block: SessionBuilder.() -> Unit): TrainingHistory =
        history(start) { session(0, mode = TrainingMode.BODYBUILDING) { block() } }

    private fun SessionBuilder.sets(exerciseId: Long, kg: Double, vararg reps: Int, rpe: Double? = 8.0) =
        reps.forEach { set(exerciseId, kg, it, rpe) }

    private fun targets(h: TrainingHistory, plan: List<PlanItem>, prior: List<Advice> = emptyList(), s: CoachSettings = settings) =
        BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, plan, s, today, prior)

    private fun Advice.target() = payload as AdvicePayload.Target

    /** On [anchor] + k × [step] (in hundredths, so float noise does not matter). */
    private fun onGrid(kg: Double, anchor: Double, step: Double): Boolean =
        (((kg - anchor) * 100).roundToLong() % (step * 100).roundToLong()) == 0L

    @Test
    fun repsShortOfTheTopRepeatTheWeight() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) }
        val advice = targets(h, listOf(item(Ids.DB_PRESS))).single()
        assertEquals(AdviceType.TARGET, advice.type)
        assertEquals(TrainingMode.BODYBUILDING, advice.mode)
        assertEquals(AdvicePayload.Target(Ids.DB_PRESS, 26.0, 8, 8.0, null, 12), advice.payload)
        assertEquals("Last time: 26 × 11, 10, 9. Beat it. Coach suggests 26 × 8–12 @ 8.", advice.reason)
    }

    @Test
    fun everySetAtTheTopAtTheTargetRpeAddsWeight() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 12, 12, 12) }
        val advice = targets(h, listOf(item(Ids.DB_PRESS))).single()
        val t = advice.target()
        assertTrue(t.weightKg > 26.0, "${t.weightKg}")
        assertTrue(onGrid(t.weightKg, 26.0, 0.5), "${t.weightKg} is not on the 0.5 kg grid")
        assertEquals(8, t.reps)
        assertEquals(12, t.repsHigh)
        assertEquals(26.5, t.weightKg)
        assertEquals("Last time: 26 × 12, 12, 12. Time to add weight. Coach suggests 26.5 × 8–12 @ 8.", advice.reason)
    }

    @Test
    fun oneSetAboveTheTargetRpeRepeats() {
        val h = lastWeek {
            set(Ids.DB_PRESS, 26.0, 12, 8.0)
            set(Ids.DB_PRESS, 26.0, 12, 8.0)
            set(Ids.DB_PRESS, 26.0, 12, 9.0)
        }
        val advice = targets(h, listOf(item(Ids.DB_PRESS))).single()
        assertEquals(26.0, advice.target().weightKg)
        assertTrue(advice.reason.contains("Beat it."), advice.reason)
    }

    @Test
    fun fewerSetsThanPlannedRepeats() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 12, 12) }
        assertEquals(26.0, targets(h, listOf(item(Ids.DB_PRESS))).single().target().weightKg)
    }

    @Test
    fun aMissedSetRepeats() {
        val h = lastWeek {
            sets(Ids.DB_PRESS, 26.0, 12, 12)
            set(Ids.DB_PRESS, 26.0, 12, 8.0, failed = true)
        }
        assertEquals(26.0, targets(h, listOf(item(Ids.DB_PRESS))).single().target().weightKg)
    }

    @Test
    fun setsWithNoRpeCountAsOnTarget() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 12, 12, 12, rpe = null) }
        assertEquals(26.5, targets(h, listOf(item(Ids.DB_PRESS))).single().target().weightKg)
    }

    @Test
    fun aBarbellLiftAddsAtLeastOnePlateStep() {
        for (changePlates in listOf(false, true)) {
            val h = lastWeek { sets(Ids.BENCH, 60.0, 12, 12, 12) }
            val t = targets(h, listOf(item(Ids.BENCH)), s = CoachSettings(changePlates = changePlates)).single().target()
            val step = PlateLoader.step(changePlates)
            assertTrue(t.weightKg >= 60.0 + step, "$changePlates: ${t.weightKg}")
            assertEquals(PlateLoader.round(t.weightKg, changePlates), t.weightKg, "on the plate grid")
            assertTrue(PlateLoader.perSide(t.weightKg, changePlates) != null, "loadable: ${t.weightKg}")
        }
        // 60 × 1.02 = 61.2: with change plates the nearest loadable weight is 61; without, one 2.5 kg step.
        assertEquals(62.5, targets(lastWeek { sets(Ids.BENCH, 60.0, 12, 12, 12) }, listOf(item(Ids.BENCH))).single().target().weightKg)
        assertEquals(
            61.0,
            targets(lastWeek { sets(Ids.BENCH, 60.0, 12, 12, 12) }, listOf(item(Ids.BENCH)), s = CoachSettings(changePlates = true))
                .single().target().weightKg,
        )
    }

    @Test
    fun theIncreaseIsTheSourcedFraction() {
        assertEquals(0.02, BodybuildingCoach.INCREASE)
        // 100 × 1.02 = 102 on a free load: 102 exactly.
        val h = lastWeek { sets(Ids.DB_PRESS, 100.0, 12, 12, 12) }
        assertEquals(102.0, targets(h, listOf(item(Ids.DB_PRESS))).single().target().weightKg)
    }

    @Test
    fun noHistoryAtAllGivesNoAdvice() {
        val h = history(start) { }
        assertEquals(emptyList(), targets(h, listOf(item(Ids.DB_PRESS))))
        // A powerlifting set older than 28 days is no history for a first-time weight either.
        val old = history(start) { session(0) { bench(100.0, 5) } }
        assertEquals(
            emptyList(),
            BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, old, listOf(item(Ids.BENCH)), settings, start.plus(35, DateTimeUnit.DAY), emptyList()),
        )
    }

    @Test
    fun powerliftingHistoryGivesAFirstTimeWeightForTheMiddleOfTheRange() {
        val h = history(start) { session(0) { bench(100.0, 5, rpe = 8.0) } }
        val advice = targets(h, listOf(item(Ids.BENCH))).single()
        val t = advice.target()
        val expected = ProgressionRule.roundDown(E1rm.loadFor(E1rm.estimate(100.0, 5, 8.0), 10, 8.0), 100.0, free = false, changePlates = false)
        assertEquals(82.5, expected)
        assertEquals(expected, t.weightKg)
        assertTrue(t.weightKg < 100.0)
        assertEquals(8, t.reps)
        assertEquals(12, t.repsHigh)
        assertEquals("First time on this side. Coach suggests 82.5 × 8–12 @ 8.", advice.reason)
    }

    @Test
    fun anOpenDeloadKeepsTheRangeWithLessWeightAndFewerSets() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) }
        val deload = Advice(AdviceType.DELOAD, null, today, "Signs of fatigue", 2, AdvicePayload.Deload(), AdviceStatus.OPEN)
        val advice = targets(h, listOf(item(Ids.DB_PRESS)), listOf(deload)).single()
        val t = advice.target()
        assertTrue(t.weightKg < 26.0, "${t.weightKg}")
        assertEquals(8, t.reps)
        assertEquals(12, t.repsHigh)
        assertEquals(2, t.sets)
        assertEquals(
            "Last time: 26 × 11, 10, 9. Beat it. Coach suggests ${ProgressionRule.fmt(t.weightKg)} × 8–12 @ 8. " +
                "Deload week: lighter load and fewer sets.",
            advice.reason,
        )
    }

    @Test
    fun aDeloadMadeOnThePowerliftingSideCountsHereToo() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) }
        val deload = Advice(AdviceType.DELOAD, null, today, "Signs of fatigue", 2, AdvicePayload.Deload(), mode = TrainingMode.POWERLIFTING)
        assertEquals(2, targets(h, listOf(item(Ids.DB_PRESS)), listOf(deload)).single().target().sets)
    }

    @Test
    fun theFirstDayBackAfterABreakIsEasedIn() {
        val h = history(start) { session(0, mode = TrainingMode.BODYBUILDING) { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) } }
        val back = start.plus(21, DateTimeUnit.DAY)
        val advice = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.DB_PRESS)), settings, back, emptyList()).single()
        assertTrue(advice.target().weightKg < 26.0)
        assertTrue(advice.reason.endsWith(" Easing back in: lighter load."), advice.reason)
        assertNull(advice.target().sets)
    }

    @Test
    fun onlyTodaysBodybuildingItemsInPlanOrder() {
        val h = lastWeek {
            sets(Ids.DB_PRESS, 26.0, 11, 10, 9)
            sets(Ids.CURL, 12.0, 12, 12, 12)
        }
        val plan = listOf(
            item(Ids.CURL, id = 2).copy(order = 1),
            item(Ids.DB_PRESS, id = 1).copy(order = 0),
            item(Ids.DB_PRESS, id = 3).copy(weekday = DayOfWeek.TUESDAY),
            PlanItem(4, DayOfWeek.MONDAY, Ids.BENCH, 5, 5, 8.0, 2),
        )
        assertEquals(listOf(Ids.DB_PRESS, Ids.CURL), targets(h, plan).map { it.target().exerciseId })
    }

    @Test
    fun lastTimeIsTheLatestBodybuildingSessionNotAPowerliftingOne() {
        val h = history(start) {
            session(0, mode = TrainingMode.BODYBUILDING) { sets(Ids.BENCH, 60.0, 11, 10, 9) }
            session(3) { bench(100.0, 5) }
        }
        val advice = targets(h, listOf(item(Ids.BENCH))).single()
        assertEquals(60.0, advice.target().weightKg)
        assertEquals("Last time: 60 × 11, 10, 9. Beat it. Coach suggests 60 × 8–12 @ 8.", advice.reason)
    }

    @Test
    fun theReasonKeepsTheTodayCardsSuggestionStripping() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) }
        val reason = targets(h, listOf(item(Ids.DB_PRESS))).single().reason
        // The apps' SUGGESTION_SENTENCE (ExerciseCard.kt / ExerciseCard.tsx).
        val stripped = reason.replace(Regex("""\s*Coach suggests .*? @ \d+(\.\d+)?\."""), "").trim()
        assertEquals("Last time: 26 × 11, 10, 9. Beat it.", stripped)
    }

    @Test
    fun repsToBeatAreLastTimesPlusOneCappedAtTheTop() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) }
        val item = item(Ids.DB_PRESS)
        assertEquals(listOf(12, 11, 10), (1..3).map { BodybuildingCoach.repsToBeat(item, h, it, today) })
        val capped = lastWeek { sets(Ids.DB_PRESS, 26.0, 12, 11, 12) }
        // Not every set reached 12, so no increase: 12 stays 12, 11 becomes 12.
        assertEquals(listOf(12, 12, 12), (1..3).map { BodybuildingCoach.repsToBeat(item, capped, it, today) })
    }

    @Test
    fun repsToBeatAreTheLowEndAfterAnIncreaseAndTheFirstTime() {
        val item = item(Ids.DB_PRESS)
        val increased = lastWeek { sets(Ids.DB_PRESS, 26.0, 12, 12, 12) }
        assertEquals(listOf(8, 8, 8), (1..3).map { BodybuildingCoach.repsToBeat(item, increased, it, today) })
        val none = history(start) { }
        assertEquals(8, BodybuildingCoach.repsToBeat(item, none, 1, today))
        val powerOnly = history(start) { session(0) { set(Ids.DB_PRESS, 30.0, 5) } }
        assertEquals(8, BodybuildingCoach.repsToBeat(item, powerOnly, 1, today))
    }

    @Test
    fun repsToBeatForASetNotDoneLastTimeIsTheLowEnd() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 11, 10) }
        assertEquals(8, BodybuildingCoach.repsToBeat(item(Ids.DB_PRESS), h, 3, today))
    }

    @Test
    fun anExerciseUntrainedForSixWeeksIsEasedInNeverLastWeightPlusAnIncrease() {
        // Dumbbell press last done on the bodybuilding side on day 0, earning an increase; then six weeks of
        // powerlifting bench only, so no 14-day gap. Day 49: it is eased in from 26, not 26.5.
        val h = history(start) {
            session(0, mode = TrainingMode.BODYBUILDING) { sets(Ids.DB_PRESS, 26.0, 12, 12, 12) }
            for (d in 7..42 step 7) session(d) { bench(100.0, 5) }
        }
        val back = start.plus(49, DateTimeUnit.DAY)
        val item = item(Ids.DB_PRESS)
        val advice = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item), settings, back, emptyList()).single()
        assertEquals(AdvicePayload.Target(Ids.DB_PRESS, 21.0, 8, 8.0, null, 12), advice.payload)
        assertEquals(
            "Last time: 26 × 12, 12, 12. Time to add weight. Coach suggests 21 × 8–12 @ 8. Easing back in: lighter load.",
            advice.reason,
        )
        // With a deload open as well: both notes, each once, and fewer sets.
        val deload = Advice(AdviceType.DELOAD, null, back, "Signs of fatigue", 2, AdvicePayload.Deload())
        val both = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item), settings, back, listOf(deload)).single()
        assertEquals(2, both.target().sets)
        assertTrue(both.target().weightKg <= 21.0)
        assertTrue(
            both.reason.endsWith("@ 8. Deload week: lighter load and fewer sets. Easing back in: lighter load."),
            both.reason,
        )
    }
}
