package com.powertracker.domain.coach

import com.powertracker.domain.GoldenScenario
import com.powertracker.domain.coach.PhysiqueFixtures.FOUR_DAYS
import com.powertracker.domain.coach.PhysiqueFixtures.default3
import com.powertracker.domain.coach.PhysiqueFixtures.exercises
import com.powertracker.domain.coach.PhysiqueFixtures.idOf
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The coach's answer to a skipped planned exercise (skip-a-planned-exercise plan, task 1). */
class SkipCoachTest {
    private val today = LocalDate(2026, 11, 18)
    private val pulldown = idOf("Lat pulldown")

    /** An own exercise that works no muscle group the catalogue knows: nothing to swap it for. */
    private val oddId = 9100L
    private val odd = Exercise(oddId, "My odd lift", Category.ACCESSORY, null, emptyList(), isBuiltIn = false)
    private val history = TrainingHistory(emptyList(), exercises + (oddId to odd), emptyList())

    /** The four main lifts and a pulldown on the powerlifting side, Monday and Friday. */
    private val powerlifting: List<PlanItem> = listOf("Squat", "Bench", "Deadlift", "Strict press", "Lat pulldown").flatMapIndexed { i, name ->
        listOf(MONDAY, FRIDAY).mapIndexed { d, day -> PlanItem(500L + i * 2 + d, day, idOf(name), 3, 5, 8.0, i, POWERLIFTING) }
    }
    private val bothSides = default3 + powerlifting

    private var nextId = 1L
    private fun skip(daysAgo: Int, reason: SkipReason?, exerciseId: Long = pulldown, mode: TrainingMode = BODYBUILDING) =
        Skip(nextId++, today.minus(daysAgo, DateTimeUnit.DAY), mode, exerciseId, null, reason)

    /** [skips] stored in order; the coach answers the last one. */
    private fun after(vararg skips: Skip, prior: List<Advice> = emptyList(), plan: List<PlanItem> = bothSides): List<Advice> =
        Coaching.afterSkip(skips.last(), skips.toList(), history, plan, today, prior)

    private fun Advice.skipped() = payload as AdvicePayload.Skipped

    private fun firstEquipment(name: String) = Library.find(name)!!.equipment.first()

    private fun card(exerciseId: Long, status: AdviceStatus, daysAgo: Int, severity: Int, mode: TrainingMode = BODYBUILDING) = Advice(
        AdviceType.SKIP, null, today.minus(daysAgo, DateTimeUnit.DAY), "r", severity,
        AdvicePayload.Skipped(99, exerciseId, "x", null, listOf("Machine pulldown")), status, id = 77, mode = mode,
    )

    // ---- busy ----

    @Test
    fun busy_secondInWindow_givesCard_firstDoesNot() {
        val first = skip(10, SkipReason.BUSY)
        assertEquals(emptyList(), after(first))
        val advice = after(first, skip(0, SkipReason.BUSY)).single()
        assertEquals(AdviceType.SKIP, advice.type)
        assertEquals(
            "You've skipped Lat pulldown twice in 4 weeks because you were busy or had no equipment. Swap it for one of these?",
            advice.reason,
        )
        assertEquals(SkipReason.BUSY, advice.skipped().reason)
        assertTrue(advice.skipped().alternatives.isNotEmpty())
        assertEquals(null, advice.lift)
        assertEquals(today, advice.createdAt)
        assertEquals(AdviceStatus.OPEN, advice.status)
    }

    @Test
    fun busy_secondOutside28Days_noCard() {
        assertEquals(emptyList(), after(skip(28, SkipReason.BUSY), skip(0, SkipReason.BUSY)))
        // The 28 days end on the skip's date and include it: 27 days before is still in.
        assertEquals(1, after(skip(27, SkipReason.BUSY), skip(0, SkipReason.BUSY)).size)
    }

    @Test
    fun busy_alternativesHaveDifferentFirstEquipment() {
        val alternatives = after(skip(3, SkipReason.BUSY), skip(0, SkipReason.BUSY)).single().skipped().alternatives
        assertTrue(alternatives.size in 1..3, "$alternatives")
        val own = firstEquipment("Lat pulldown")
        assertTrue(alternatives.none { firstEquipment(it) == own }, "$alternatives")
        // Taken from the swap list as it comes, in its order.
        val all = PhysiqueCoach.swapAlternatives(exercises.getValue(pulldown), default3, exercises).map { it.name }
        assertEquals(all.filter { firstEquipment(it) != own }.take(3), alternatives)
    }

    @Test
    fun busy_mainLift_noCard() {
        for (name in listOf("Squat", "Bench", "Deadlift", "Strict press")) {
            for (mode in TrainingMode.entries) {
                val id = idOf(name)
                assertEquals(emptyList(), after(skip(3, SkipReason.BUSY, id, mode), skip(0, SkipReason.BUSY, id, mode)), "$name $mode")
                // Nor the repeat card.
                assertEquals(
                    emptyList(),
                    after(skip(6, null, id, mode), skip(3, SkipReason.BUSY, id, mode), skip(0, SkipReason.BUSY, id, mode)), "$name $mode",
                )
            }
        }
    }

    @Test
    fun busy_withNoOtherEquipment_fallsThroughToRepeat() {
        // Only dumbbell exercises in the plan: a lateral raise's one alternative is a dumbbell exercise too.
        val dumbbells = listOf("Lateral raise", "DB press", "DB curl").mapIndexed { i, n -> PhysiqueFixtures.item(600L + i, MONDAY, n, 3, i) }
        val raise = idOf("Lateral raise")
        assertEquals(emptyList(), after(skip(5, SkipReason.BUSY, raise), skip(0, SkipReason.BUSY, raise), plan = dumbbells))
        val advice = after(skip(10, null, raise), skip(5, SkipReason.BUSY, raise), skip(0, SkipReason.BUSY, raise), plan = dumbbells).single()
        assertEquals("You've skipped Lateral raise 3 times in 4 weeks. Keep it in your plan, or swap it for one of these?", advice.reason)
        assertEquals(listOf("Seated lateral raise"), advice.skipped().alternatives)
    }

    // ---- pain ----

    private val painWithAlternatives = "You skipped Lat pulldown because of pain. Try one of these instead. " +
        "Its weight won't go up until you do Lat pulldown again. If the pain persists or gets worse, see a physician."

    @Test
    fun pain_first_givesCardWithAlternatives_andSourcedLine() {
        val advice = after(skip(0, SkipReason.PAIN)).single()
        assertEquals(painWithAlternatives, advice.reason)
        val all = PhysiqueCoach.swapAlternatives(exercises.getValue(pulldown), default3, exercises).map { it.name }
        assertEquals(all.take(3), advice.skipped().alternatives)
        assertEquals(SkipReason.PAIN, advice.skipped().reason)
    }

    @Test
    fun pain_mainLift_givesCardWithNoAlternatives() {
        val lifts = mapOf("Squat" to MainLift.SQUAT, "Bench" to MainLift.BENCH, "Deadlift" to MainLift.DEADLIFT, "Strict press" to MainLift.STRICT_PRESS)
        for ((name, lift) in lifts) {
            for (mode in TrainingMode.entries) {
                val advice = after(skip(0, SkipReason.PAIN, idOf(name), mode)).single()
                assertEquals(
                    "You skipped $name because of pain. Its weight won't go up until you do it again. If the pain persists or gets worse, see a physician.",
                    advice.reason, "$name $mode",
                )
                assertEquals(emptyList(), advice.skipped().alternatives, "$name $mode")
                assertEquals(lift, advice.lift, "$name $mode")
                assertEquals(mode, advice.mode)
            }
        }
    }

    @Test
    fun pain_variation_isNotAMainLift() {
        // A variation keeps its parent lift for the coach's other rails, but no lift on a skip card, and it can be swapped.
        val variation = Library.all().first { it.category == Category.VARIATION }
        val id = idOf(variation.name)
        val plan = bothSides + PlanItem(700, WEDNESDAY, id, 3, 5, 8.0, 0, POWERLIFTING)
        val advice = after(skip(0, SkipReason.PAIN, id, POWERLIFTING), plan = plan).single()
        assertEquals(null, advice.lift)
    }

    @Test
    fun pain_noAlternativeLeft_givesHoldCard() {
        val plan = default3 + PlanItem(700, WEDNESDAY, oddId, 3, 8, 8.0, 9, BODYBUILDING, 12)
        val advice = after(skip(0, SkipReason.PAIN, oddId), plan = plan).single()
        assertEquals(
            "You skipped My odd lift because of pain. Its weight won't go up until you do it again. If the pain persists or gets worse, see a physician.",
            advice.reason,
        )
        assertEquals(emptyList(), advice.skipped().alternatives)
        assertEquals(null, advice.lift)
        // Busy or repeated skips of it get no card at all.
        assertEquals(emptyList(), after(skip(9, null, oddId), skip(5, SkipReason.BUSY, oddId), skip(0, SkipReason.BUSY, oddId), plan = plan))
    }

    @Test
    fun painOnThePowerliftingSide_offersAlternativesFromThatSidesPlan() {
        val advice = after(skip(0, SkipReason.PAIN, pulldown, POWERLIFTING)).single()
        assertEquals(painWithAlternatives, advice.reason)
        assertEquals(POWERLIFTING, advice.mode)
        val all = PhysiqueCoach.swapAlternatives(exercises.getValue(pulldown), powerlifting, exercises, POWERLIFTING).map { it.name }
        assertEquals(all.take(3), advice.skipped().alternatives)
        assertTrue(advice.skipped().alternatives.isNotEmpty())
    }

    // ---- repeat and time ----

    @Test
    fun repeat_thirdNonTime_givesCard_secondDoesNot() {
        assertEquals(emptyList(), after(skip(10, null), skip(0, null)))
        val advice = after(skip(20, null), skip(10, null), skip(0, null)).single()
        assertEquals("You've skipped Lat pulldown 3 times in 4 weeks. Keep it in your plan, or swap it for one of these?", advice.reason)
        assertEquals(null, advice.skipped().reason)
        val all = PhysiqueCoach.swapAlternatives(exercises.getValue(pulldown), default3, exercises).map { it.name }
        assertEquals(all.take(3), advice.skipped().alternatives)
        // Any reason but time counts: a busy and a pain skip before it.
        assertEquals(1, after(skip(20, SkipReason.PAIN), skip(10, SkipReason.BUSY), skip(0, null), prior = emptyList()).size)
    }

    @Test
    fun time_neverCard() {
        assertEquals(emptyList(), after(skip(0, SkipReason.TIME)))
        assertEquals(emptyList(), after(skip(20, SkipReason.TIME), skip(10, SkipReason.TIME), skip(5, SkipReason.TIME), skip(0, SkipReason.TIME)))
        assertEquals(emptyList(), after(skip(20, null), skip(10, null), skip(0, SkipReason.TIME)))
    }

    @Test
    fun time_notCountedTowardRepeat() {
        assertEquals(emptyList(), after(skip(20, SkipReason.TIME), skip(10, null), skip(0, null)))
    }

    // ---- what blocks a card ----

    @Test
    fun openSwapCard_blocksSkipCard() {
        val swap = Advice(
            AdviceType.SWAP, null, today.minus(30, DateTimeUnit.DAY), "r", 0,
            AdvicePayload.Swap(pulldown, "Lat pulldown", MuscleGroup.LATS, today, listOf("Machine pulldown")), mode = BODYBUILDING,
        )
        assertEquals(emptyList(), after(skip(0, SkipReason.PAIN), prior = listOf(swap)))
        // Not once it was acted on, and not another exercise's or the other side's.
        assertEquals(1, after(skip(0, SkipReason.PAIN), prior = listOf(swap.copy(status = AdviceStatus.DISMISSED))).size)
        assertEquals(1, after(skip(0, SkipReason.PAIN, mode = POWERLIFTING), prior = listOf(swap)).size)
    }

    @Test
    fun openSkipCard_blocksAnother() {
        val open = card(pulldown, AdviceStatus.OPEN, 20, 1)
        assertEquals(emptyList(), after(skip(0, SkipReason.PAIN), prior = listOf(open)))
        assertEquals(1, after(skip(0, SkipReason.PAIN), prior = listOf(card(idOf("Leg press"), AdviceStatus.OPEN, 20, 1))).size)
        assertEquals(1, after(skip(0, SkipReason.PAIN), prior = listOf(card(pulldown, AdviceStatus.OPEN, 20, 1, POWERLIFTING))).size)
    }

    @Test
    fun cardInLast7Days_blocksAnother() {
        val applied = card(pulldown, AdviceStatus.APPLIED, 6, 1)
        assertEquals(emptyList(), after(skip(0, SkipReason.PAIN), prior = listOf(applied)))
        assertEquals(1, after(skip(0, SkipReason.PAIN), prior = listOf(applied.copy(createdAt = today.minus(7, DateTimeUnit.DAY)))).size)
    }

    @Test
    fun dismissedRepeat_notRecreatedWithin28Days() {
        val repeat = { after(skip(20, null), skip(9, null), skip(0, null)) }
        val severity = repeat().single().severity
        val dismissed = card(pulldown, AdviceStatus.DISMISSED, 27, severity)
        assertEquals(emptyList(), after(skip(20, null), skip(9, null), skip(0, null), prior = listOf(dismissed)))
        assertEquals(1, after(skip(20, null), skip(9, null), skip(0, null), prior = listOf(dismissed.copy(createdAt = today.minus(28, DateTimeUnit.DAY)))).size)
        // A more severe card still comes: a pain skip after a dismissed repeat card.
        assertEquals(1, after(skip(0, SkipReason.PAIN), prior = listOf(dismissed)).size)
        // A dismissed pain card holds back busy and repeat cards too.
        val pain = after(skip(0, SkipReason.PAIN)).single()
        val dismissedPain = pain.copy(createdAt = today.minus(10, DateTimeUnit.DAY), status = AdviceStatus.DISMISSED)
        assertEquals(emptyList(), after(skip(5, SkipReason.BUSY), skip(0, SkipReason.BUSY), prior = listOf(dismissedPain)))
        assertTrue(pain.severity > after(skip(5, SkipReason.BUSY), skip(0, SkipReason.BUSY)).single().severity)
        assertTrue(after(skip(5, SkipReason.BUSY), skip(0, SkipReason.BUSY)).single().severity > severity)
    }

    @Test
    fun aWeeklySkipper_getsTheCardBackOnceTheDismissalIsForgotten() {
        // A skip with no reason every week. The 3rd gives the card, which is dismissed; it is not made again while
        // the dismissal is remembered, but comes back once it is not, though the count is past 3 by then.
        val weekly = (6 downTo 0).map { w -> skip(w * 7, null) }
        val third = weekly[2]
        val card = Coaching.afterSkip(third, weekly.take(3), history, bothSides, third.date, emptyList()).single()
        val dismissed = card.copy(status = AdviceStatus.DISMISSED, id = 50)
        for (i in 3 until weekly.size) {
            val s = weekly[i]
            val made = Coaching.afterSkip(s, weekly.take(i + 1), history, bothSides, s.date, listOf(dismissed))
            val remembered = s.date < third.date.plus(28, DateTimeUnit.DAY)
            assertEquals(if (remembered) 0 else 1, made.size, "skip ${i + 1} on ${s.date}")
        }
        // Busy skips likewise: a 3rd busy skip still gives the busy card, with its real count.
        val busy = after(skip(20, SkipReason.BUSY), skip(10, SkipReason.BUSY), skip(0, SkipReason.BUSY)).single()
        assertTrue(busy.reason.startsWith("You've skipped Lat pulldown 3 times in 4 weeks because"), busy.reason)
    }

    @Test
    fun cardTextsUseTheRealCount() {
        val busy = after(skip(21, SkipReason.BUSY), skip(14, SkipReason.BUSY), skip(7, SkipReason.BUSY), skip(0, SkipReason.BUSY)).single()
        assertEquals(
            "You've skipped Lat pulldown 4 times in 4 weeks because you were busy or had no equipment. Swap it for one of these?",
            busy.reason,
        )
        val repeat = after(skip(21, null), skip(14, SkipReason.TIME), skip(10, SkipReason.PAIN), skip(7, null), skip(0, null)).single()
        assertEquals("You've skipped Lat pulldown 4 times in 4 weeks. Keep it in your plan, or swap it for one of these?", repeat.reason)
    }

    @Test
    fun onlyOneCardPerSkip_painWins() {
        // The second busy skip and the third skip at once: one card, the busy one.
        val busy = after(skip(20, null), skip(10, SkipReason.BUSY), skip(0, SkipReason.BUSY)).single()
        assertEquals(SkipReason.BUSY, busy.skipped().reason)
        assertTrue(busy.reason.startsWith("You've skipped Lat pulldown twice"))
        // A pain skip that is also the third: the pain card.
        val pain = after(skip(20, SkipReason.BUSY), skip(10, null), skip(0, SkipReason.PAIN)).single()
        assertEquals(painWithAlternatives, pain.reason)
    }

    @Test
    fun otherSidesSkips_notCountedTowardThresholds() {
        assertEquals(emptyList(), after(skip(5, SkipReason.BUSY, mode = POWERLIFTING), skip(0, SkipReason.BUSY)))
        assertEquals(emptyList(), after(skip(10, null, mode = POWERLIFTING), skip(5, null, mode = POWERLIFTING), skip(0, null)))
        // Nor another exercise's.
        assertEquals(emptyList(), after(skip(5, SkipReason.BUSY, idOf("Leg press")), skip(0, SkipReason.BUSY)))
    }

    @Test
    fun card_carriesSkipId_andMode() {
        for (mode in TrainingMode.entries) {
            val last = skip(0, SkipReason.PAIN, mode = mode)
            val advice = after(last).single()
            assertEquals(mode, advice.mode)
            assertEquals(last.id, advice.skipped().skipId)
            assertEquals(pulldown, advice.skipped().exerciseId)
            assertEquals("Lat pulldown", advice.skipped().name)
        }
    }

    @Test
    fun anUnknownExerciseGetsNoCard() {
        assertEquals(emptyList(), after(skip(0, SkipReason.PAIN, exerciseId = 123456)))
    }

    @Test
    fun theSkippedPayloadRoundTripsThroughTheAdviceSerializer() {
        for (payload in listOf(
            AdvicePayload.Skipped(3, pulldown, "Lat pulldown", SkipReason.BUSY, listOf("Machine pulldown")),
            AdvicePayload.Skipped(4, idOf("Squat"), "Squat", null, emptyList()),
        )) {
            val json = Json.encodeToString(AdvicePayload.serializer(), payload)
            assertEquals(payload, Json.decodeFromString(AdvicePayload.serializer(), json))
        }
    }

    // ---- the coach's other rails do not see skip cards ----

    @Test
    fun skipCards_inPriorAdvice_leaveEvaluateAndTargetsUnchanged() {
        val start = GoldenScenario.day(0)
        var bit = false
        for (d in 0..100) {
            val day = start.plus(d, DateTimeUnit.DAY)
            val h = GoldenScenario.history.copy(sessions = GoldenScenario.history.sessions.filter { it.date <= day })
            val stored = GoldenScenario.checkpoints.firstOrNull { it.today == day }?.storedAdvice.orEmpty()
            // A card for each main lift made yesterday, in every status, on both sides.
            val cards = MainLift.entries.flatMap { lift ->
                AdviceStatus.entries.flatMap { status ->
                    TrainingMode.entries.map { mode ->
                        Advice(
                            AdviceType.SKIP, lift, day.minus(1, DateTimeUnit.DAY), "r", 3,
                            AdvicePayload.Skipped(1, lift.ordinal + 1L, lift.name, SkipReason.PAIN, emptyList()), status, mode = mode,
                        )
                    }
                }
            }
            for (prior in listOf(emptyList(), stored)) {
                for (mode in TrainingMode.entries) {
                    val without = Coaching.evaluate(mode, h, GoldenScenario.plan, GoldenScenario.settings, day, prior)
                    assertEquals(without, Coaching.evaluate(mode, h, GoldenScenario.plan, GoldenScenario.settings, day, prior + cards), "evaluate $mode on $day")
                    if (without.any { it.type == AdviceType.STALL || it.type == AdviceType.WEAK_POINT }) bit = true
                    for (weekday in DayOfWeek.entries) {
                        assertEquals(
                            Coaching.targetsFor(mode, weekday, h, GoldenScenario.plan, GoldenScenario.settings, day, prior),
                            Coaching.targetsFor(mode, weekday, h, GoldenScenario.plan, GoldenScenario.settings, day, prior + cards),
                            "targets $mode $weekday on $day",
                        )
                    }
                }
            }
        }
        assertTrue(bit, "no day in the scenario gives a stall or a weak point, so the cards were never in the way")
    }

    // ---- applying a skip card's swap ----

    private val pulldownCard = AdvicePayload.Skipped(5, pulldown, "Lat pulldown", SkipReason.BUSY, listOf("Machine pulldown", "Close-grip pulldown"))

    @Test
    fun applySkipSwap_bodybuilding_swapsThatSideOnly() {
        val held = default3.filter { it.exerciseId == pulldown }
        val change = Coaching.applySkipSwap(pulldownCard, "Machine pulldown", BODYBUILDING, bothSides, exercises)
        assertEquals(PlanChange(emptyList(), held, held.map { it.copy(id = 0, exerciseId = 0) to "Machine pulldown" }, "Swapped Lat pulldown for Machine pulldown", true), change)
        // The same as the stuck exercise's swap gives.
        val swap = AdvicePayload.Swap(pulldown, "Lat pulldown", MuscleGroup.LATS, today, pulldownCard.alternatives)
        assertEquals(PhysiqueCoach.applySwap(swap, "Machine pulldown", bothSides, exercises), change)
    }

    @Test
    fun applySkipSwap_powerlifting_swapsThatSideOnly() {
        val held = powerlifting.filter { it.exerciseId == pulldown }
        val change = Coaching.applySkipSwap(pulldownCard, "machine  PULLDOWN", POWERLIFTING, bothSides, exercises)
        assertEquals(PlanChange(emptyList(), held, held.map { it.copy(id = 0, exerciseId = 0) to "Machine pulldown" }, "Swapped Lat pulldown for Machine pulldown", true), change)
        // Nothing to do once the choice is in that side's plan, or the exercise has left it.
        val nothing = PlanChange(emptyList(), emptyList(), emptyList(), PhysiqueCoach.NOTHING_SWAPPED, false)
        val withChoice = bothSides + PlanItem(800, WEDNESDAY, idOf("Machine pulldown"), 3, 5, 8.0, 0, POWERLIFTING)
        assertEquals(nothing, Coaching.applySkipSwap(pulldownCard, "Machine pulldown", POWERLIFTING, withChoice, exercises))
        assertEquals(nothing, Coaching.applySkipSwap(pulldownCard, "Machine pulldown", POWERLIFTING, default3, exercises))
        assertFailsWith<IllegalArgumentException> { Coaching.applySkipSwap(pulldownCard, "Leg press", POWERLIFTING, bothSides, exercises) }
    }

    @Test
    fun applySkipSwap_bodybuildingRefusesToPassTheCeiling_powerliftingHasNone() {
        val plan = PhysiqueFixtures.build(FOUR_DAYS, emptyList(), 3)
        val curl = exercises.getValue(idOf("Cable leg curl"))
        val payload = AdvicePayload.Skipped(6, curl.id, curl.name, null, listOf("DB RDL"))
        assertEquals(
            PlanChange(emptyList(), emptyList(), emptyList(), PhysiqueCoach.PAST_CEILING, false),
            Coaching.applySkipSwap(payload, "DB RDL", BODYBUILDING, plan, exercises),
        )
        val asPowerlifting = plan.map { it.copy(mode = POWERLIFTING) }
        val change = Coaching.applySkipSwap(payload, "DB RDL", POWERLIFTING, asPowerlifting, exercises)
        assertTrue(change.changed)
        assertEquals("Swapped Cable leg curl for DB RDL", change.message)
    }
}
