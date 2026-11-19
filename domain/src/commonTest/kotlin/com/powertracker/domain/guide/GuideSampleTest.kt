package com.powertracker.domain.guide

import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.coach.FatigueRule
import com.powertracker.domain.coach.ProgressionRule
import com.powertracker.domain.coach.StallRule
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.PlanTemplates
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuideSampleTest {
    private val modes = TrainingMode.entries

    private fun idOf(mode: TrainingMode, name: String): Long =
        GuideSample.exercises(mode).values.single { it.name == name }.id

    /** The next date after [date] whose weekday holds a plan item for [exerciseId]. */
    private fun nextDay(plan: List<PlanItem>, exerciseId: Long, date: LocalDate): LocalDate {
        val days = plan.filter { it.exerciseId == exerciseId }.map { it.weekday }.toSet()
        var d = date.plus(1, DateTimeUnit.DAY)
        while (d.dayOfWeek !in days) d = d.plus(1, DateTimeUnit.DAY)
        return d
    }

    @Test
    fun targets_areWhatTheRealCoachGives() {
        for (mode in modes) {
            val steps = GuideSample.targets(mode)
            assertEquals(5, steps.size, "$mode: five sessions")
            assertEquals(1, steps.map { it.exerciseId }.distinct().size, "$mode: one lift")
            val plan = GuideSample.plan(mode)
            val exercises = GuideSample.exercises(mode)
            val id = idOf(mode, steps.first().exerciseId)
            val first = steps.first()
            // The powerlifting coach needs 3 sessions or a baseline for a main lift: one from the first set itself.
            val baselines = if (mode == TrainingMode.POWERLIFTING) {
                listOf(Baseline(id, E1rm.estimate(first.doneWeightKg, first.doneReps, first.doneRpe), first.date))
            } else {
                emptyList()
            }
            val sessions = mutableListOf<LoggedSession>()
            steps.forEachIndexed { i, step ->
                if (i > 0) assertEquals(nextDay(plan, id, steps[i - 1].date), step.date, "$mode step $i: the next planned day")
                val item = plan.first { it.exerciseId == id && it.weekday == step.date.dayOfWeek }
                val sets = (1..item.sets).map { n -> LoggedSet(id, step.doneWeightKg, step.doneReps, step.doneRpe, false, null, n == item.sets) }
                sessions += LoggedSession(i + 1L, step.date, null, null, sets, mode)
                val next = nextDay(plan, id, step.date)
                val history = TrainingHistory(sessions.toList(), exercises, baselines)
                val target = Coaching.targetsFor(mode, next.dayOfWeek, history, plan, CoachSettings(), next, emptyList())
                    .single { (it.payload as AdvicePayload.Target).exerciseId == id }
                val payload = target.payload as AdvicePayload.Target
                assertEquals(payload.weightKg, step.nextWeightKg, "$mode step $i weight")
                assertEquals(payload.reps, step.nextRepsLow, "$mode step $i reps")
                assertEquals(payload.repsHigh ?: payload.reps, step.nextRepsHigh, "$mode step $i reps high")
                assertEquals(target.reason, step.note, "$mode step $i note")
                if (i + 1 < steps.size) {
                    assertEquals(step.nextWeightKg, steps[i + 1].doneWeightKg, "$mode step ${i + 1}: the lifter does the target's weight")
                }
            }
        }
    }

    @Test
    fun targets_showTheSidesOwnProgression() {
        val pl = GuideSample.targets(TrainingMode.POWERLIFTING)
        // Up, up, held, up, and a big step (capped) after an easy session.
        assertTrue(pl.zipWithNext().all { (a, b) -> b.doneWeightKg >= a.doneWeightKg })
        assertTrue(pl.any { it.nextWeightKg == it.doneWeightKg }, "powerlifting: a repeat")
        assertTrue(pl.any { it.nextWeightKg - it.doneWeightKg == ProgressionRule.BIG_STEP_KG }, "powerlifting: a big step")
        val bb = GuideSample.targets(TrainingMode.BODYBUILDING)
        // Reps climb at one weight, then the weight goes up.
        val up = bb.indexOfFirst { it.nextWeightKg > it.doneWeightKg }
        assertTrue(up >= 2, "bodybuilding: reps first")
        assertTrue(bb.take(up + 1).zipWithNext().all { (a, b) -> b.doneReps > a.doneReps && b.doneWeightKg == a.doneWeightKg })
    }

    @Test
    fun plan_isAPlanTemplatesDraft() {
        for (mode in modes) {
            val template = GuideSample.template(mode)
            assertEquals(PlanTemplates.forMode(mode).first { it.dayCount == 3 }, template)
            val draft = PlanTemplates.draft(template, PlanTemplates.suggestedDays(3, emptySet()), strictPress = false)
            val exercises = GuideSample.exercises(mode)
            val plan = GuideSample.plan(mode)
            val expected = draft.sessions.flatMap { s -> s.exercises.mapIndexed { order, e -> Triple(s.day, order, e) } }
            assertEquals(expected.size, plan.size, "$mode: one item per draft exercise")
            for ((item, want) in plan.zip(expected)) {
                val (day, order, e) = want
                assertEquals(e.name, exercises.getValue(item.exerciseId).name)
                assertEquals(day, item.weekday)
                assertEquals(order, item.order)
                assertEquals(e.sets, item.sets)
                assertEquals(e.repsLow, item.reps)
                assertEquals(e.rpe, item.targetRpe)
                assertEquals(mode, item.mode)
                assertEquals(if (mode == TrainingMode.POWERLIFTING) null else e.repsHigh, item.repsHigh)
            }
            assertEquals(plan.size, plan.map { it.id }.toSet().size)
        }
    }

    @Test
    fun today_isTheFirstTargetStep() {
        for (mode in modes) {
            assertEquals(GuideSample.targets(mode).first(), GuideSample.today(mode))
            assertEquals(GuideSample.TODAY, GuideSample.today(mode).date)
        }
    }

    @Test
    fun advice_isWhatTheRealRulesGive() {
        val pl = GuideSample.advice(TrainingMode.POWERLIFTING)
        assertEquals(listOf(AdviceType.DELOAD, AdviceType.RE_ENTRY, AdviceType.SKIP, AdviceType.STALL), pl.map { it.type })
        val bb = GuideSample.advice(TrainingMode.BODYBUILDING)
        assertEquals(listOf(AdviceType.DELOAD, AdviceType.RE_ENTRY, AdviceType.SKIP, AdviceType.SWAP), bb.map { it.type })
        for ((mode, cards) in listOf(TrainingMode.POWERLIFTING to pl, TrainingMode.BODYBUILDING to bb)) {
            assertTrue(cards.all { it.mode == mode && it.createdAt == GuideSample.TODAY }, "$mode: stamped with its side")
            assertEquals(AdvicePayload.Deload(), cards[0].payload)
            assertTrue(cards[0].reason.startsWith("Signs of fatigue"), cards[0].reason)
            assertEquals(AdvicePayload.ReEntry(), cards[1].payload)
            assertTrue(cards[1].reason.startsWith("Welcome back"), cards[1].reason)
            val skip = cards[2].payload as AdvicePayload.Skipped
            assertEquals(SkipReason.PAIN, skip.reason)
            assertTrue("won't go up" in cards[2].reason, cards[2].reason)
            // Each card names an exercise of the sample's plan.
            assertTrue(skip.exerciseId in GuideSample.plan(mode).map { it.exerciseId })
        }
        // Powerlifting's pain card is for a main lift, so it offers no swap: that side's coach shows none.
        assertTrue((pl[2].payload as AdvicePayload.Skipped).alternatives.isEmpty())
        assertTrue(pl[3].reason.contains("hasn't improved"), pl[3].reason)
        val swap = bb[3].payload as AdvicePayload.Swap
        assertTrue(swap.alternatives.isNotEmpty())
        assertTrue(swap.exerciseId in GuideSample.plan(TrainingMode.BODYBUILDING).map { it.exerciseId })
        assertTrue(pl.none { it.reason.contains("swap", ignoreCase = true) }, "powerlifting: a card offers a swap")
        assertTrue(pl.none { it.type == AdviceType.SWAP || it.type == AdviceType.MORE_WORK })
        assertTrue(bb.none { it.type == AdviceType.STALL || it.type == AdviceType.WEAK_POINT })
    }

    @Test
    fun steps_coverEveryScreen_andRepeatOncePerSampleEntry() {
        for (mode in modes) {
            val all = Guide.SCREENS.flatMap { Guide.steps(it, mode) }
            assertTrue(Guide.SCREENS.all { Guide.steps(it, mode).isNotEmpty() }, "$mode: a screen with no steps")
            assertEquals(GuideSample.targets(mode).size, Guide.steps(GuideScreenId.TARGETS, mode).count { it == GuideStep.TARGETS_SESSION })
            assertEquals(GuideSample.advice(mode).size, Guide.steps(GuideScreenId.HOLDS_BACK, mode).count { it == GuideStep.HOLDS_CARD })
            assertEquals(GuideSample.advice(mode).size, Guide.screen(GuideScreenId.HOLDS_BACK, mode).lines.size, "$mode: a line per card")
            // Every step but the repeated ones appears once, on one screen, in the enum's order.
            val once = all.filter { it != GuideStep.TARGETS_SESSION && it != GuideStep.HOLDS_CARD }
            assertEquals(once, once.distinct())
            assertEquals(all.distinct(), all.distinct().sortedBy { it.ordinal })
            assertEquals(GuideStep.entries.toSet(), all.toSet())
        }
    }

    @Test
    fun progress_bestsNeverFall_andStayWithinTheProgressionCap() {
        for (mode in modes) {
            val progress = GuideSample.progress(mode)
            val points = progress.points
            assertTrue(points.isNotEmpty())
            val days = points.first().first.daysUntil(points.last().first)
            assertTrue(days in 150..200, "$mode: about six months, $days days")
            assertEquals(points.map { it.first }.sorted(), points.map { it.first }, "$mode: oldest first")
            for ((a, b) in points.zipWithNext()) {
                assertTrue(b.second >= a.second, "$mode: ${b.first} falls from ${a.second} to ${b.second}")
                assertTrue(b.second <= a.second * ProgressionRule.MAX_INCREASE, "$mode: ${b.first} jumps from ${a.second} to ${b.second}")
            }
            assertTrue(points.last().second > points.first().second, "$mode: it climbs")
            assertTrue(progress.bests.isNotEmpty())
            val names = GuideSample.exercises(mode).values.map { it.name }.toSet()
            assertTrue(progress.bests.all { it.first in names && it.second > 0.0 })
        }
        val pl = GuideSample.progress(TrainingMode.POWERLIFTING)
        assertNotNull(pl.totalKg)
        assertNull(pl.laggingMuscle)
        val bb = GuideSample.progress(TrainingMode.BODYBUILDING)
        assertNull(bb.totalKg)
        val lagging = MuscleGroup.valueOf(assertNotNull(bb.laggingMuscle))
        // The check-in naming it gets more work from the real coach.
        val outcome = Coaching.afterCheckIn(
            com.powertracker.domain.model.CheckIn(GuideSample.TODAY, setOf(lagging), emptySet()),
            TrainingHistory(emptyList(), GuideSample.exercises(TrainingMode.BODYBUILDING), emptyList()),
            GuideSample.plan(TrainingMode.BODYBUILDING), GuideSample.TODAY,
        )
        assertEquals(listOf(lagging), outcome.advice.map { (it.payload as AdvicePayload.MoreWork).group })
    }

    /**
     * Every day of the powerlifting progress run, the history up to and including it: the coach reads a day's history
     * whether or not there is a session on it (a stall counts from the Monday, before that week's session).
     */
    private fun everyDayOfThePowerliftingRun(): List<Pair<LocalDate, TrainingHistory>> {
        val history = GuideSample.progressHistory(TrainingMode.POWERLIFTING)
        val days = mutableListOf<Pair<LocalDate, TrainingHistory>>()
        var day = GuideSample.TODAY
        while (day <= GuideSample.PROGRESS_END) {
            val until = day
            days += day to history.copy(sessions = history.sessions.filter { it.date <= until })
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return days
    }

    @Test
    fun samplePowerlifter_isNeverFlaggedByTheWeakPointRule() {
        for ((day, history) in everyDayOfThePowerliftingRun()) {
            val flagged = WeakPointRule.evaluate(HistoryStats(history), day)
            assertTrue(flagged.isEmpty(), "$day: ${flagged.map { it.reason }}")
        }
    }

    @Test
    fun samplePowerlifter_isNeverFlaggedByTheStallRule() {
        val settings = CoachSettings()
        for ((day, history) in everyDayOfThePowerliftingRun()) {
            val stats = HistoryStats(history)
            // No accessory counted as planned, so "already in the plan" can't hide a plateau.
            for (lift in MainLift.entries) {
                val stall = StallRule.evaluate(lift, stats, settings, day, emptySet())
                assertNull(stall, "$day $lift: ${stall?.reason}")
            }
        }
    }

    @Test
    fun samplePowerlifter_isNeverFlaggedByTheFatigueRule() {
        for ((day, history) in everyDayOfThePowerliftingRun()) {
            val deload = FatigueRule.evaluate(HistoryStats(history), history, day)
            assertNull(deload, "$day: ${deload?.reason}")
        }
    }

    @Test
    fun theStallCard_readsTheWeeksTheRuleFirstFiresAt() {
        val weeks = CoachSettings().stallWeeks
        val stall = GuideSample.advice(TrainingMode.POWERLIFTING).single { it.type == AdviceType.STALL }
        assertTrue("hasn't improved for $weeks weeks." in stall.reason, stall.reason)
        assertEquals(weeks, stall.severity)
    }

    @Test
    fun theSampleCards_haveDistinctIds() {
        for (mode in modes) {
            assertEquals(listOf(1L, 2L, 3L, 4L), GuideSample.advice(mode).map { it.id }, "$mode: ids for list keys")
        }
    }
}
