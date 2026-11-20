package com.powertracker.domain.web

import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.model.TrainingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GuideBridgeTest {
    @Test
    fun screensAndStepsCrossTheBoundary() {
        assertEquals(
            listOf("WELCOME", "SIDE", "PLAN", "TODAY", "TARGETS", "HOLDS_BACK", "MONTHS_LATER", "YOUR_TURN"),
            GritDomain.guideScreens().toList(),
        )
        val screen = GritDomain.guideScreen("TARGETS", "POWERLIFTING")
        val domain = Guide.screen(Guide.SCREENS[4], TrainingMode.POWERLIFTING)
        assertEquals(domain.title, screen.title)
        assertEquals(domain.lines.map { it.text }, screen.lines.map { it.text })
        assertEquals(domain.lines.map { it.constants }, screen.lines.map { it.constants.toList() })
        assertTrue(screen.example)
        assertEquals(List(GuideSample.targets(TrainingMode.BODYBUILDING).size) { "TARGETS_SESSION" }, GritDomain.guideSteps("TARGETS", "BODYBUILDING").toList())
        assertEquals(Guide.STEP_MS, GritDomain.guideStepMs())
        assertFailsWith<IllegalArgumentException> { GritDomain.guideScreen("NOPE", "POWERLIFTING") }
        assertFailsWith<IllegalArgumentException> { GritDomain.guideSteps("TODAY", "CARDIO") }
    }

    @Test
    fun theSampleCrossesTheBoundary() {
        assertEquals(GuideSample.TODAY.toString(), GritDomain.guideSampleToday())
        assertEquals("bb-3", GritDomain.guideSampleTemplate("BODYBUILDING").id)
        val plan = GritDomain.guideSamplePlan("POWERLIFTING")
        val exercises = GritDomain.guideSampleExercises("POWERLIFTING").associateBy { it.id }
        assertTrue(plan.all { it.exerciseId in exercises })
        for (mode in TrainingMode.entries) {
            assertEquals(GuideSample.todayItems(mode).map { it.id }, GritDomain.guideSampleTodayItems(mode.name).map { it.id.toLong() })
        }
        val targets = GritDomain.guideSampleTargets("POWERLIFTING")
        assertEquals(GritDomain.guideSampleTodaySet("POWERLIFTING"), targets.first())
        assertEquals(GuideSample.TODAY.toString(), targets.first().date)
        assertEquals(GuideSample.targets(TrainingMode.POWERLIFTING).map { it.nextWeightKg }, targets.map { it.nextWeightKg })
        assertEquals(listOf("DELOAD", "RE_ENTRY", "SKIP", "SWAP"), GritDomain.guideSampleAdvice("BODYBUILDING").map { it.type })
        assertTrue(GritDomain.guideSampleAdvice("BODYBUILDING").all { it.mode == "BODYBUILDING" })
        val pl = GritDomain.guideSampleProgress("POWERLIFTING")
        assertTrue(pl.totalKg != null && pl.laggingMuscle == null && pl.points.isNotEmpty())
        assertEquals("CALVES", GritDomain.guideSampleProgress("BODYBUILDING").laggingMuscle)
    }

    @Test
    fun theOtherPicturesCrossTheBoundary() {
        // Screen 2.
        assertEquals(Guide.sideFrame(700).dropMs, GritDomain.guideSideFrame(700.0).dropMs)
        assertEquals(Guide.SIDE_STILL.pumpMs, GritDomain.guideSideStill().pumpMs)
        assertEquals(Guide.SIDE_LOOP_MS, GritDomain.guideSideLoopMs())
        assertEquals(Guide.sidePumpSource().x, GritDomain.guideSideBox().srcX)
        // Screen 3's taps and swap.
        assertEquals("CHOOSE_TEMPLATE", GritDomain.guideTap("PLAN_TEMPLATES"))
        assertEquals("USE_THIS_PLAN", GritDomain.guideTap("PLAN_FILLED"))
        for (mode in TrainingMode.entries) {
            val swap = GritDomain.guideSampleSwap(mode.name)
            assertEquals(GuideSample.swap(mode).from, swap.from)
            assertEquals(1, swap.day)
            val swapped = GritDomain.guideSampleDraft(mode.name, true)
            assertTrue(swapped.sessions.first().exercises.any { it.name == swap.to })
            assertTrue(GritDomain.guideSampleDraft(mode.name, false).sessions.first().exercises.any { it.name == swap.from })
            val names = GritDomain.guideSampleFilledExercises(mode.name).associateBy { it.id }
            assertTrue(GritDomain.guideSampleFilledPlan(mode.name).all { it.exerciseId in names })
            // Screen 5.
            val advice = GritDomain.guideSampleTargetAdvice(mode.name)
            assertEquals(GuideSample.targetAdvice(mode).map { it.reason }, advice.map { it.reason })
            assertTrue(advice.all { it.type == "TARGET" && it.status == "OPEN" })
            assertEquals(GuideSample.targetEditor(mode, 2, 0.5).weightKg, GritDomain.guideSampleTargetEditor(mode.name, 2, 0.5).weightKg)
            assertEquals(GuideSample.targetHistory(mode).sessions.size, GritDomain.guideSampleTargetHistory(mode.name).sessions.size)
            // Screen 7.
            val end = GritDomain.guideSampleMonthsDate(mode.name, "MONTHS_ENDING", 1.0)
            val history = GritDomain.guideSampleMonthsHistory(mode.name, end)
            assertEquals(GuideSample.progressHistory(mode).sessions.size, history.sessions.size)
            assertEquals(mode.name, history.sessions.first().mode)
            assertEquals(GuideSample.monthsPlan(mode).size, GritDomain.guideSampleMonthsPlan(mode.name).size)
        }
        // Screen 6's stall figures.
        assertEquals(GuideSample.stallTrend(TrainingMode.POWERLIFTING).size, GritDomain.guideSampleStallTrend("POWERLIFTING").size)
        assertEquals(GuideSample.stallE1rm(TrainingMode.POWERLIFTING), GritDomain.guideSampleStallE1rm("POWERLIFTING"))
        assertEquals(0, GritDomain.guideSampleStallTrend("BODYBUILDING").size)
        // Screen 7's check-in.
        val checkIn = GritDomain.guideSampleMonthsCheckIn("BODYBUILDING", "MONTHS_ENDING", 1.0)!!
        assertEquals(listOf("CALVES"), checkIn.lagging.toList())
        assertEquals(null, GritDomain.guideSampleMonthsCheckIn("POWERLIFTING", "MONTHS_ENDING", 1.0))
    }
}
