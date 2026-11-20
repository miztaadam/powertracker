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
        assertEquals(emptyList(), targets(h, listOf(item(Ids.BENCH))))
        assertEquals(emptyList(), targets(h, listOf(item(Ids.DB_PRESS))))
        // Only missed sets: nothing to go on either.
        val missed = history(start) { session(0) { bench(100.0, 5, failed = true) } }
        assertEquals(emptyList(), targets(missed, listOf(item(Ids.BENCH))))
    }

    @Test
    fun powerliftingHistoryOlderThanFourWeeksGivesALighterFirstTimeWeight() {
        // Bench 100 × 5 @ 8, 52 days before day 56 (a Monday); and the same sets four days before it. Dumbbell
        // press on the bodybuilding side last week, so neither log is a break from training.
        val day = start.plus(56, DateTimeUnit.DAY)
        val old = history(start) {
            session(4) { bench(100.0, 5, rpe = 8.0) }
            session(49, mode = TrainingMode.BODYBUILDING) { sets(Ids.DB_PRESS, 26.0, 10, 10, 10) }
        }
        val recent = history(start) {
            session(49, mode = TrainingMode.BODYBUILDING) { sets(Ids.DB_PRESS, 26.0, 10, 10, 10) }
            session(52) { bench(100.0, 5, rpe = 8.0) }
        }
        fun first(h: TrainingHistory) =
            BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.BENCH)), settings, day, emptyList()).single()
        val fresh = first(recent)
        assertEquals(82.5, fresh.target().weightKg)
        val eased = first(old)
        val factor = AdvicePayload.ReEntry().loadFactor
        val expected = ProgressionRule.roundDown(
            E1rm.loadFor(E1rm.estimate(100.0, 5, 8.0), 10, 8.0) * factor, 100.0, free = false, changePlates = false,
        )
        assertEquals(70.0, expected)
        assertEquals(expected, eased.target().weightKg)
        assertTrue(eased.target().weightKg < fresh.target().weightKg)
        assertEquals("First time on this side. Coach suggests 70 × 8–12 @ 8.", eased.reason)
    }

    @Test
    fun oldHistoryIsReadFromTheFourWeeksEndingAtItsLatestSession() {
        // Bench 140 × 5 on day -10, 120 × 5 on day 0, 100 × 5 on day 20; today is day 70. The 28 days ending on
        // day 20 hold the 120 and the 100, so the estimate is the 120's; the 140, 30 days before day 20, is out.
        val day = start.plus(70, DateTimeUnit.DAY)
        val h = history(start.plus(-10, DateTimeUnit.DAY)) {
            session(0) { bench(140.0, 5, rpe = 8.0) }
            session(10) { bench(120.0, 5, rpe = 8.0) }
            session(30) { bench(100.0, 5, rpe = 8.0) }
            session(75, mode = TrainingMode.BODYBUILDING) { sets(Ids.DB_PRESS, 26.0, 10, 10, 10) }
        }
        val t = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.BENCH)), settings, day, emptyList()).single().target()
        val expected = ProgressionRule.roundDown(
            E1rm.loadFor(E1rm.estimate(120.0, 5, 8.0), 10, 8.0) * AdvicePayload.ReEntry().loadFactor, 120.0,
            free = false, changePlates = false,
        )
        assertEquals(expected, t.weightKg)
    }

    @Test
    fun aBaselineAloneGivesAFirstTimeWeight() {
        // The builder dates a baseline at day 0, seven days before today: recent, so no easing.
        val h = history(start) { baseline(Ids.BENCH, 120.0) }
        val advice = targets(h, listOf(item(Ids.BENCH))).single()
        val expected = ProgressionRule.roundDown(E1rm.loadFor(120.0, 10, 8.0), 0.0, free = false, changePlates = false)
        assertEquals(expected, advice.target().weightKg)
        assertEquals("First time on this side. Coach suggests ${ProgressionRule.fmt(expected)} × 8–12 @ 8.", advice.reason)
        // The same baseline 52 days later is eased by the re-entry factor.
        val later = start.plus(52, DateTimeUnit.DAY)
        val eased = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.BENCH)), settings, later, emptyList()).single()
        assertEquals(
            ProgressionRule.roundDown(E1rm.loadFor(120.0, 10, 8.0) * AdvicePayload.ReEntry().loadFactor, 0.0, free = false, changePlates = false),
            eased.target().weightKg,
        )
        assertTrue(eased.target().weightKg < expected)
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
        assertEquals(AdvicePayload.Target(Ids.DB_PRESS, 22.0, 8, 8.0, null, 12), advice.payload)
        assertEquals(
            "Last time: 26 × 12, 12, 12. Coach suggests 22 × 8–12 @ 8. Easing back in: lighter load.",
            advice.reason,
        )
        // With a deload open as well: both notes, each once, and fewer sets.
        val deload = Advice(AdviceType.DELOAD, null, back, "Signs of fatigue", 2, AdvicePayload.Deload())
        val both = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item), settings, back, listOf(deload)).single()
        assertEquals(2, both.target().sets)
        assertTrue(both.target().weightKg <= 22.0)
        assertTrue(
            both.reason.endsWith("@ 8. Deload week: lighter load and fewer sets. Easing back in: lighter load."),
            both.reason,
        )
    }

    @Test
    fun aDumbbellDeloadIsRoundedDownOnTheHalfKiloGrid() {
        // 26 × 0.9 = 23.4: 23, not 21 on the 2.5 kg grid.
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) }
        val deload = Advice(AdviceType.DELOAD, null, today, "Signs of fatigue", 2, AdvicePayload.Deload())
        for (changePlates in listOf(false, true)) {
            val t = targets(h, listOf(item(Ids.DB_PRESS)), listOf(deload), CoachSettings(changePlates = changePlates)).single()
            assertEquals(23.0, t.target().weightKg, "change plates $changePlates")
            assertTrue(t.reason.contains("Coach suggests 23 × 8–12 @ 8."), t.reason)
        }
    }

    @Test
    fun aDumbbellEasedInAfterABreakIsRoundedDownOnTheHalfKiloGrid() {
        // 26 × 0.85 = 22.1: 22.
        val h = history(start) { session(0, mode = TrainingMode.BODYBUILDING) { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) } }
        val back = start.plus(21, DateTimeUnit.DAY)
        val advice = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.DB_PRESS)), settings, back, emptyList()).single()
        assertEquals(22.0, advice.target().weightKg)
    }

    @Test
    fun aDumbbellCappedByTheOtherSideIsRoundedDownOnTheHalfKiloGrid() {
        // Dumbbell press 26 × 11, 10, 9 on the bodybuilding side on day 0; then 20 × 5 @ 8 on the powerlifting side
        // only. Day 35: capped at what 20 × 5 supports for 8 reps @ 8, rounded down on the 0.5 kg grid from 26.
        val h = history(start) {
            session(0, mode = TrainingMode.BODYBUILDING) { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) }
            for (d in listOf(14, 21, 28)) session(d) { set(Ids.DB_PRESS, 20.0, 5, 8.0) }
        }
        val day = start.plus(35, DateTimeUnit.DAY)
        val t = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.DB_PRESS)), settings, day, emptyList()).single().target()
        val supported = E1rm.loadFor(E1rm.estimate(20.0, 5, 8.0), 8, 8.0)
        assertTrue(supported < 26.0, "$supported")
        assertTrue(t.weightKg <= supported && t.weightKg > supported - 0.5, "${t.weightKg} from $supported")
        assertTrue(onGrid(t.weightKg, 26.0, 0.5), "${t.weightKg}")
    }

    @Test
    fun aDumbbellFirstTimeWeightIsRoundedDownOnTheHalfKiloGrid() {
        // 32 × 5 @ 8 supports about 26.8 for 10 @ 8: 26.5, not 24.5 on the 2.5 kg grid.
        val h = history(start) { session(0) { set(Ids.DB_PRESS, 32.0, 5, 8.0) } }
        val t = targets(h, listOf(item(Ids.DB_PRESS))).single().target()
        val wanted = E1rm.loadFor(E1rm.estimate(32.0, 5, 8.0), 10, 8.0)
        assertTrue(wanted > 26.5 && wanted < 27.0, "$wanted")
        assertEquals(26.5, t.weightKg)
    }

    @Test
    fun aBarbellDeloadStaysOnThePlateGrid() {
        // 60 × 0.9 = 54: 52.5 without change plates, 54 with them.
        val h = lastWeek { sets(Ids.BENCH, 60.0, 11, 10, 9) }
        val deload = Advice(AdviceType.DELOAD, null, today, "Signs of fatigue", 2, AdvicePayload.Deload())
        assertEquals(52.5, targets(h, listOf(item(Ids.BENCH)), listOf(deload)).single().target().weightKg)
        assertEquals(54.0, targets(h, listOf(item(Ids.BENCH)), listOf(deload), CoachSettings(changePlates = true)).single().target().weightKg)
    }

    @Test
    fun theSameDumbbellDeloadOnThePowerliftingSideKeepsTodaysNumber() {
        val h = history(start) { session(0) { set(Ids.DB_PRESS, 26.0, 10, 8.0, isTopSet = true) } }
        val deload = Advice(AdviceType.DELOAD, null, today, "Signs of fatigue", 2, AdvicePayload.Deload())
        val plan = listOf(PlanItem(1, DayOfWeek.MONDAY, Ids.DB_PRESS, 3, 10, 8.0, 0))
        val t = Coach.targetsFor(DayOfWeek.MONDAY, h, plan, settings, today, listOf(deload)).single().payload as AdvicePayload.Target
        assertEquals(21.0, t.weightKg)
    }

    @Test
    fun aBodyweightExerciseAtZeroIsNeverToldToAddWeight() {
        val h = lastWeek { sets(Ids.PULL_UP, 0.0, 12, 12, 12) }
        val item = item(Ids.PULL_UP)
        val advice = targets(h, listOf(item)).single()
        assertEquals(AdvicePayload.Target(Ids.PULL_UP, 0.0, 8, 8.0, null, 12), advice.payload)
        assertEquals("Last time: 0 × 12, 12, 12. Beat it. Coach suggests 0 × 8–12 @ 8.", advice.reason)
        // With no weight to add, the reps to beat stay at the top of the range.
        assertEquals(listOf(12, 12, 12), (1..3).map { BodybuildingCoach.repsToBeat(item, h, it, today) })
    }

    @Test
    fun aBreakWithOldPowerliftingHistoryIsEasedOnce() {
        // Bench 100 × 5 @ 8 on day 4, nothing since; day 56. The first-time weight from history that old is eased
        // once: 82.5 × 0.85 rounded down, not 82.5 × 0.85 × 0.85 (about 72%). The side was never trained, so the
        // break's re-entry clamp does not apply (1.16) and the reason does not say easing back in.
        val h = history(start) { session(4) { bench(100.0, 5, rpe = 8.0) } }
        val day = start.plus(56, DateTimeUnit.DAY)
        val advice = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.BENCH)), settings, day, emptyList()).single()
        val once = ProgressionRule.roundDown(82.5 * AdvicePayload.ReEntry().loadFactor, 82.5, free = false, changePlates = false)
        assertEquals(70.0, once)
        assertEquals(once, advice.target().weightKg)
        assertEquals("First time on this side. Coach suggests 70 × 8–12 @ 8.", advice.reason)
        // A deload on top still applies: fewer sets, and never heavier.
        val deload = Advice(AdviceType.DELOAD, null, day, "Signs of fatigue", 2, AdvicePayload.Deload())
        val both = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.BENCH)), settings, day, listOf(deload)).single()
        assertEquals(2, both.target().sets)
        assertTrue(both.target().weightKg <= once)
    }

    @Test
    fun aBreakDoesNotEaseASideNeverTrained() {
        // Bench on the powerlifting side on days 0, 7 and 14, nothing since; the bodybuilding side has no session.
        // Day 30 is the lifter's first day back after 16 days away, but this side has nothing to come back to: its
        // first-time weight is the one a day without a break gives (day 20, same 28-day window of bench), unclamped.
        val h = history(start) { for (d in listOf(0, 7, 14)) session(d) { bench(100.0, 5, rpe = 8.0) } }
        fun on(d: Int, prior: List<Advice> = emptyList()) =
            BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.BENCH)), settings, start.plus(d, DateTimeUnit.DAY), prior).single()
        val noBreak = on(20)
        val back = on(30)
        assertEquals(noBreak.target(), back.target())
        assertTrue(!back.reason.contains("Easing back in"), back.reason)
        // Nor does a stored easing back in, which belongs to the lifter.
        val reEntry = Advice(AdviceType.RE_ENTRY, null, start.plus(30, DateTimeUnit.DAY), "Welcome back", 16, AdvicePayload.ReEntry())
        assertEquals(noBreak.target(), on(30, listOf(reEntry)).target())
        // A deload still applies: fewer sets.
        val deload = Advice(AdviceType.DELOAD, null, start.plus(30, DateTimeUnit.DAY), "Signs of fatigue", 2, AdvicePayload.Deload())
        assertEquals(2, on(30, listOf(deload)).target().sets)
    }

    @Test
    fun aDeloadAfterEarningAnIncreaseDoesNotSayAddWeight() {
        val h = lastWeek { sets(Ids.DB_PRESS, 26.0, 12, 12, 12) }
        val deload = Advice(AdviceType.DELOAD, null, today, "Signs of fatigue", 2, AdvicePayload.Deload())
        val advice = targets(h, listOf(item(Ids.DB_PRESS)), listOf(deload)).single()
        assertEquals(23.0, advice.target().weightKg)
        assertEquals(
            "Last time: 26 × 12, 12, 12. Coach suggests 23 × 8–12 @ 8. Deload week: lighter load and fewer sets.",
            advice.reason,
        )
    }

    @Test
    fun theReturnRuleAndABreakTogetherEaseOnce() {
        // Dumbbell press on day 0, powerlifting bench until day 35, nothing since; day 56 is both a return to this
        // side (return rule branch b) and a 21-day break: 26 × 0.85 = 22.1, 22, once.
        val h = history(start) {
            session(0, mode = TrainingMode.BODYBUILDING) { sets(Ids.DB_PRESS, 26.0, 11, 10, 9) }
            for (d in 7..35 step 7) session(d) { bench(100.0, 5) }
        }
        val day = start.plus(56, DateTimeUnit.DAY)
        val advice = BodybuildingCoach.targetsFor(DayOfWeek.MONDAY, h, listOf(item(Ids.DB_PRESS)), settings, day, emptyList()).single()
        assertEquals(22.0, advice.target().weightKg)
        assertEquals("Last time: 26 × 11, 10, 9. Beat it. Coach suggests 22 × 8–12 @ 8. Easing back in: lighter load.", advice.reason)
    }
}
