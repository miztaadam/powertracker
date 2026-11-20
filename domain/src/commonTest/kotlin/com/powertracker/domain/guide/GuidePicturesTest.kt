package com.powertracker.domain.guide

import com.powertracker.domain.coach.BodybuildingCoach
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.motion.ModeSwitchMotion
import com.powertracker.domain.plan.PlanTemplates
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.domain.progress.MuscleProgress
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the seven other pictures (1.16, Task 4) draw at each step and part-way through each move: only values the app
 * allows, worked out by the real code the apps call, and the same for both apps.
 */
class GuidePicturesTest {
    private val modes = TrainingMode.entries
    private val progresses = (0..20).map { it / 20.0 }

    private fun onGrid(kg: Double): Boolean {
        val grid = PlateLoader.step(changePlates = false)
        return abs(kg / grid - round(kg / grid)) < 1e-9
    }

    private fun halves(rpe: Double) = abs(rpe * 2 - round(rpe * 2)) < 1e-9

    // ---- taps ----

    @Test
    fun screen3_tapsChooseTemplateTheTemplateSwapAndUseThisPlan() {
        for (mode in modes) {
            val taps = Guide.steps(GuideScreenId.PLAN, mode).associateWith { Guide.tap(it) }
            assertEquals(
                mapOf(
                    GuideStep.PLAN_EMPTY to null,
                    GuideStep.PLAN_TEMPLATES to GuideTap.CHOOSE_TEMPLATE,
                    GuideStep.PLAN_DRAFT to GuideTap.TEMPLATE,
                    GuideStep.PLAN_SWAP to GuideTap.SWAP,
                    GuideStep.PLAN_FILLED to GuideTap.USE_THIS_PLAN,
                ),
                taps,
                "$mode",
            )
        }
    }

    @Test
    fun theOtherPictures_standForNoTap() {
        val none = listOf(GuideScreenId.WELCOME, GuideScreenId.SIDE, GuideScreenId.TARGETS, GuideScreenId.HOLDS_BACK, GuideScreenId.MONTHS_LATER, GuideScreenId.YOUR_TURN)
        for (mode in modes) for (id in none) {
            assertTrue(Guide.steps(id, mode).all { Guide.tap(it) == null }, "$id on $mode")
        }
    }

    @Test
    fun frame_ringsBeforeEachPlanTap() {
        for (mode in modes) {
            val steps = Guide.steps(GuideScreenId.PLAN, mode)
            for (i in 1 until steps.size) {
                val tap = Guide.tap(steps[i]) ?: continue
                val ring = Guide.frame(steps, Guide.stepStartMs(i) - Guide.TAP_MS / 2)
                assertEquals(i - 1, ring.stepIndex)
                assertEquals(tap, ring.tap)
                assertEquals(0.5, ring.tapProgress, 1e-9)
                assertNull(Guide.frame(steps, Guide.stepStartMs(i) - Guide.TAP_MS - 1).tap)
            }
        }
    }

    // ---- screen 2: the hammer and the pump, looping ----

    @Test
    fun sideLoop_cutsEachMotionDown_andRepeats() {
        assertEquals(ModeSwitchMotion.DROP.lift, Guide.SIDE_DROP_CUT_MS)
        val pump = ModeSwitchMotion.PUMP
        assertEquals(pump.beats[2] + pump.rise[2] + pump.fall[2], Guide.SIDE_PUMP_CUT_MS)
        assertTrue(Guide.SIDE_LOOP_MS > maxOf(Guide.SIDE_DROP_CUT_MS, Guide.SIDE_PUMP_CUT_MS))
        for (t in listOf(0L, 100L, 300L, 700L, 1200L, 2000L)) {
            val f = Guide.sideFrame(t)
            assertEquals(f, Guide.sideFrame(t + Guide.SIDE_LOOP_MS))
            assertTrue(f.dropMs in 0.0..Guide.SIDE_DROP_CUT_MS && f.pumpMs in 0.0..Guide.SIDE_PUMP_CUT_MS, "$t: $f")
        }
        assertEquals(GuideSideFrame(0.0, 0.0), Guide.sideFrame(0))
        // Each runs at its own speed until its cut, then holds there until the loop comes round.
        assertEquals(GuideSideFrame(300.0, 300.0), Guide.sideFrame(300))
        assertEquals(GuideSideFrame(Guide.SIDE_DROP_CUT_MS, 1000.0), Guide.sideFrame(1000))
        val end = Guide.SIDE_LOOP_MS.toLong() - 1
        assertEquals(GuideSideFrame(Guide.SIDE_DROP_CUT_MS, Guide.SIDE_PUMP_CUT_MS), Guide.sideFrame(end))
    }

    @Test
    fun sideStill_showsTheHammerDown_andTheLastBeatAtItsHeight() {
        val still = Guide.SIDE_STILL
        val w = Guide.SIDE_BOX_W
        val h = Guide.SIDE_BOX_H
        // The hammer at rest on the glass, its cracks drawn: not the end of the drop, which draws nothing.
        val hammer = ModeSwitchMotion.hammerPose(still.dropMs, ModeSwitchMotion.hitPoint(w, h))
        assertNotNull(hammer)
        assertTrue(ModeSwitchMotion.dropFrame(still.dropMs, w, h).crackDrawn > 0)
        val pump = ModeSwitchMotion.PUMP
        assertEquals(pump.beats[2] + pump.rise[2], still.pumpMs)
        assertTrue(ModeSwitchMotion.pumpFrame(still.pumpMs, w, h, Guide.sidePumpSource()).veinsOpacity > 0)
        // The pump starts where the switch would be on a phone: the box's lower right, as on the prototype's screen.
        val src = Guide.sidePumpSource()
        assertTrue(src.x in w / 2..w && src.y in h / 2..h)
    }

    // ---- screen 3: the plan ----

    @Test
    fun swap_isARealAlternative_ofAnExerciseShownNowhereElse() {
        for (mode in modes) {
            val swap = GuideSample.swap(mode)
            val template = GuideSample.template(mode)
            val draft = GuideSample.draft(mode, swapped = false)
            assertEquals(PlanTemplates.draft(template, PlanTemplates.suggestedDays(3, emptySet()), strictPress = false), draft)
            // On the first day, so it is in view on the draft; once in the week, so its Swap names one row.
            assertEquals(draft.sessions.first().day, swap.day)
            assertEquals(1, draft.sessions.sumOf { s -> s.exercises.count { it.name == swap.from } }, "$mode: ${swap.from} once in the week")
            assertEquals(PlanTemplates.alternatives(template, draft, swap.day, swap.from).first().name, swap.to)
            assertEquals(PlanTemplates.swap(draft, swap.day, swap.from, swap.to), GuideSample.draft(mode, swapped = true))
            // Screens 4 to 7 draw the plan as the template gives it: the swapped exercise is on none of them.
            val elsewhere = GuideSample.todayItems(mode).map { GuideSample.exercises(mode).getValue(it.exerciseId).name } +
                GuideSample.targets(mode).map { it.exerciseId } +
                GuideSample.progressPlan(mode).map { GuideSample.exercises(mode).getValue(it.exerciseId).name } +
                GuideSample.progress(mode).bests.map { it.first }
            assertTrue(swap.from !in elsewhere, "$mode: ${swap.from} is drawn on another screen")
            val skippedOrHeld = GuideSample.advice(mode).mapNotNull { a ->
                when (val p = a.payload) {
                    is AdvicePayload.Skipped -> p.name
                    is AdvicePayload.Swap -> p.name
                    else -> null
                }
            }
            assertTrue(swap.from !in skippedOrHeld)
        }
    }

    @Test
    fun filledPlan_isTheSwappedDraft_asUseThisPlanSavesIt() {
        for (mode in modes) {
            val swap = GuideSample.swap(mode)
            val plan = GuideSample.plan(mode)
            val filled = GuideSample.filledPlan(mode)
            val names = GuideSample.filledExercises(mode)
            val draft = GuideSample.draft(mode, swapped = true)
            val expected = draft.sessions.flatMap { s -> s.exercises.mapIndexed { order, e -> Triple(s.day, order, e) } }
            assertEquals(expected.size, filled.size)
            for ((item, want) in filled.zip(expected)) {
                val (day, order, e) = want
                assertEquals(e.name, names.getValue(item.exerciseId).name)
                assertEquals(day, item.weekday)
                assertEquals(order, item.order)
                assertEquals(e.sets, item.sets)
                assertEquals(e.repsLow, item.reps)
                assertEquals(e.rpe, item.targetRpe)
                assertEquals(if (mode == TrainingMode.POWERLIFTING) null else e.repsHigh, item.repsHigh)
            }
            // The same items and ids as the plan the other screens use, but the one exercise swapped.
            assertEquals(plan.map { it.id }, filled.map { it.id })
            val differ = plan.zip(filled).filter { (a, b) -> GuideSample.exercises(mode).getValue(a.exerciseId).name != names.getValue(b.exerciseId).name }
            assertEquals(1, differ.size)
            assertEquals(swap.from, GuideSample.exercises(mode).getValue(differ.single().first.exerciseId).name)
            assertEquals(swap.to, Library.find(names.getValue(differ.single().second.exerciseId).name)?.name)
        }
    }

    // ---- screen 5: targets ----

    @Test
    fun targetAdvice_isTheRealCoachsTarget_forEachNextSession() {
        for (mode in modes) {
            val steps = GuideSample.targets(mode)
            val advice = GuideSample.targetAdvice(mode)
            assertEquals(steps.size, advice.size)
            for ((i, a) in advice.withIndex()) {
                val p = a.payload as AdvicePayload.Target
                assertEquals(AdviceType.TARGET, a.type)
                assertEquals(steps[i].nextWeightKg, p.weightKg)
                assertEquals(steps[i].nextRepsLow, p.reps)
                assertEquals(steps[i].note, a.reason)
                assertTrue(a.createdAt > steps[i].date, "the target is for the next session")
                if (i + 1 < steps.size) assertEquals(steps[i + 1].date, a.createdAt)
            }
            assertEquals(advice.size, advice.map { it.id }.toSet().size, "$mode: distinct ids")
        }
    }

    @Test
    fun targetEditor_atProgressOne_isWhatFollowingTheTargetPutsIn() {
        for (mode in modes) {
            val advice = GuideSample.targetAdvice(mode)
            val history = GuideSample.targetHistory(mode)
            val plan = GuideSample.plan(mode)
            for ((i, a) in advice.withIndex()) {
                val p = a.payload as AdvicePayload.Target
                val e = GuideSample.targetEditor(mode, i, 1.0)
                assertEquals(p.weightKg, e.weightKg)
                assertEquals(p.rpe, e.rpe)
                val reps = if (mode == TrainingMode.POWERLIFTING) {
                    p.reps
                } else {
                    val item = plan.first { it.exerciseId == p.exerciseId && it.weekday == a.createdAt.dayOfWeek }
                    val before = TrainingHistory(history.sessions.filter { it.date < a.createdAt }, history.exercises, history.baselines)
                    BodybuildingCoach.repsToBeat(item, before, 1, a.createdAt)
                }
                assertEquals(reps, e.reps, "$mode session ${i + 1}")
            }
        }
    }

    @Test
    fun bodybuildingTargets_climbInReps_thenInWeight_backToTheBottom() {
        val mode = TrainingMode.BODYBUILDING
        val editors = GuideSample.targetAdvice(mode).indices.map { GuideSample.targetEditor(mode, it, 1.0) }
        val firstRaise = editors.zipWithNext().indexOfFirst { (a, b) -> b.weightKg!! > a.weightKg!! }
        assertTrue(firstRaise > 0, "the weight goes up only after the reps climb: $editors")
        for (i in 0 until firstRaise) assertTrue(editors[i + 1].reps > editors[i].reps, "reps climb first: $editors")
        assertEquals(GuideSample.plan(mode).first().reps, editors[firstRaise + 1].reps, "then the reps start at the bottom")
    }

    @Test
    fun targetEditor_onlyShowsValuesTheAppAllows_andMovesOneWay() {
        for (mode in modes) {
            val count = GuideSample.targetAdvice(mode).size
            for (i in 0 until count) {
                val end = GuideSample.targetEditor(mode, i, 1.0)
                val from = if (i == 0) end else GuideSample.targetEditor(mode, i - 1, 1.0)
                val values = progresses.map { GuideSample.targetEditor(mode, i, it) }
                for (v in values) {
                    assertTrue(onGrid(v.weightKg!!) || v.weightKg == from.weightKg || v.weightKg == end.weightKg, "$mode $i: ${v.weightKg}")
                    assertTrue(halves(v.rpe))
                }
                fun monotonic(xs: List<Double>) = xs.zipWithNext().all { (a, b) -> a <= b } || xs.zipWithNext().all { (a, b) -> a >= b }
                assertTrue(monotonic(values.map { it.weightKg!! }) && monotonic(values.map { it.reps.toDouble() }), "$mode $i: $values")
                assertEquals(end, values.last())
                if (i == 0) assertTrue(values.all { it == end }, "the first step has no move")
            }
        }
    }

    @Test
    fun targetEditor_midMove_showsAValueBetween_whenTheWeightChangesByMoreThanAStep() {
        val mode = TrainingMode.POWERLIFTING
        val advice = GuideSample.targetAdvice(mode)
        val i = advice.indices.drop(1).first { (advice[it].payload as AdvicePayload.Target).weightKg - (advice[it - 1].payload as AdvicePayload.Target).weightKg > PlateLoader.step(false) }
        val from = GuideSample.targetEditor(mode, i - 1, 1.0).weightKg!!
        val to = GuideSample.targetEditor(mode, i, 1.0).weightKg!!
        assertTrue(progresses.map { GuideSample.targetEditor(mode, i, it).weightKg!! }.any { it > from && it < to })
    }

    // ---- screen 6: the stall card's figures ----

    @Test
    fun stallFigures_areTheRealStatsOfTheStallCardsHistory() {
        val pl = TrainingMode.POWERLIFTING
        val stall = GuideSample.advice(pl).single { it.type == AdviceType.STALL }
        val history = GuideSample.stallHistory(pl)!!
        assertEquals(stall, Coaching.evaluate(pl, history, GuideSample.plan(pl), CoachSettings(), GuideSample.TODAY, emptyList()).single { it.type == AdviceType.STALL }.copy(id = stall.id))
        val stats = HistoryStats(history)
        assertEquals(stats.weeklyTrend(MainLift.SQUAT, GuideSample.TODAY).map { it.weekStart to it.e1rmKg }, GuideSample.stallTrend(pl))
        assertEquals(stats.currentE1rm(MainLift.SQUAT, GuideSample.TODAY), GuideSample.stallE1rm(pl))
        assertTrue(GuideSample.stallTrend(pl).size >= 2)
        // Bodybuilding has no stall card.
        assertNull(GuideSample.stallHistory(TrainingMode.BODYBUILDING))
        assertEquals(emptyList(), GuideSample.stallTrend(TrainingMode.BODYBUILDING))
        assertNull(GuideSample.stallE1rm(TrainingMode.BODYBUILDING))
    }

    // ---- screen 7: months later ----

    @Test
    fun monthsDate_movesInWholeWeeks_fromTheChartToTheEnd() {
        for (mode in modes) {
            val steps = Guide.steps(GuideScreenId.MONTHS_LATER, mode)
            var last = GuideSample.monthsDate(mode, steps.first(), 0.0)
            assertEquals(last, GuideSample.monthsDate(mode, steps.first(), 1.0), "the first step has no move")
            for (step in steps.drop(1)) {
                for (p in progresses) {
                    val d = GuideSample.monthsDate(mode, step, p)
                    assertTrue(d >= last, "$mode $step $p")
                    assertEquals(0, GuideSample.monthsDate(mode, steps.first(), 0.0).daysUntil(d) % 7, "whole weeks")
                    last = d
                }
                assertTrue(GuideSample.monthsDate(mode, step, 1.0) > GuideSample.monthsDate(mode, steps[steps.indexOf(step) - 1], 1.0), "$step moves on")
            }
            // It ends in the week of the run's last session, which it counts.
            val end = GuideSample.monthsDate(mode, steps.last(), 1.0)
            val lastSession = GuideSample.monthsHistory(mode, end).sessions.last().date
            assertTrue(lastSession <= end && lastSession.plus(7, DateTimeUnit.DAY) > end)
            assertEquals(GuideSample.progressHistory(mode).sessions, GuideSample.monthsHistory(mode, end).sessions)
        }
    }

    @Test
    fun monthsHistory_isTheRunUpToThatDay() {
        for (mode in modes) {
            val all = GuideSample.progressHistory(mode)
            val steps = Guide.steps(GuideScreenId.MONTHS_LATER, mode)
            val first = GuideSample.monthsDate(mode, steps.first(), 1.0)
            val h = GuideSample.monthsHistory(mode, first)
            assertEquals(all.sessions.filter { it.date <= first }, h.sessions)
            assertTrue(h.sessions.isNotEmpty() && h.sessions.size < all.sessions.size)
            assertEquals(all.exercises, h.exercises)
            assertEquals(all.baselines, h.baselines)
            assertEquals(GuideSample.progressPlan(mode), GuideSample.monthsPlan(mode))
        }
    }

    @Test
    fun monthsCheckIn_isBodybuildingOnly_atTheEnd_markingTheLaggingMuscle() {
        val pl = TrainingMode.POWERLIFTING
        for (step in Guide.steps(GuideScreenId.MONTHS_LATER, pl)) for (p in progresses) assertNull(GuideSample.monthsCheckIn(pl, step, p))
        val bb = TrainingMode.BODYBUILDING
        val steps = Guide.steps(GuideScreenId.MONTHS_LATER, bb)
        for (step in steps.dropLast(1)) for (p in progresses) assertNull(GuideSample.monthsCheckIn(bb, step, p))
        assertNull(GuideSample.monthsCheckIn(bb, steps.last(), 0.5))
        val checkIn = assertNotNull(GuideSample.monthsCheckIn(bb, steps.last(), 1.0))
        val end = GuideSample.monthsDate(bb, steps.last(), 1.0)
        assertEquals(end, checkIn.date)
        assertEquals(setOf(MuscleGroup.valueOf(GuideSample.progress(bb).laggingMuscle!!)), checkIn.lagging)
        // The Progress screen's body parts then rate it lagging.
        val rows = MuscleProgress.rows(GuideSample.monthsHistory(bb, end), GuideSample.monthsPlan(bb), listOf(checkIn), end)
        assertEquals(com.powertracker.domain.model.MuscleRating.LAGGING, rows.single { it.group == MuscleGroup.CALVES }.ratings.last().second)
    }

    @Test
    fun monthsBests_climbAsTheWeeksGoBy() {
        val mode = TrainingMode.POWERLIFTING
        val steps = Guide.steps(GuideScreenId.MONTHS_LATER, mode)
        val dates = steps.flatMap { s -> progresses.map { GuideSample.monthsDate(mode, s, it) } }.distinct()
        val bests = dates.map { d -> HistoryStats(GuideSample.monthsHistory(mode, d)).currentE1rm(MainLift.SQUAT, d) ?: 0.0 }
        assertTrue(bests.zipWithNext().all { (a, b) -> b >= a - 1e-9 }, "$bests")
        assertNotEquals(bests.first(), bests.last())
    }

    // ---- nothing is worked out until asked for ----

    @Test
    fun theTimeFunctions_readNoSample() {
        val before = GuideSample.reads
        Guide.sideFrame(1234)
        Guide.SIDE_STILL
        for (mode in modes) for (id in Guide.SCREENS) Guide.steps(id, mode)
        assertEquals(before, GuideSample.reads)
    }

    @Test
    fun aBodybuildingSession_isNeverLoggedOnThePowerliftingRun_andTheReverse() {
        for (mode in modes) {
            val other = modes.single { it != mode }
            val dates = Guide.steps(GuideScreenId.MONTHS_LATER, mode).map { GuideSample.monthsDate(mode, it, 1.0) }
            for (d in dates) assertTrue(GuideSample.monthsHistory(mode, d).sessions.none { s: LoggedSession -> s.mode == other })
        }
    }
}
