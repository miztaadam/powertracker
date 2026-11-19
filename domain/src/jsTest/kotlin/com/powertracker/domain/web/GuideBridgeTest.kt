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
}
