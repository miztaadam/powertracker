package com.powertracker.domain.guide

import com.powertracker.domain.coach.BodybuildingCoach
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.CoachSources
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.coach.ProgressionRule
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.TrainingMode
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class GuideTest {
    private val modes = TrainingMode.entries

    private fun words(text: String) = text.split(Regex("\\s+")).count { it.isNotBlank() }

    private fun text(screen: GuideScreen) = (listOf(screen.title) + screen.lines.map { it.text }).joinToString(" ")

    @Test
    fun theEightScreens_areInOrder() {
        assertEquals(
            listOf(
                GuideScreenId.WELCOME, GuideScreenId.SIDE, GuideScreenId.PLAN, GuideScreenId.TODAY,
                GuideScreenId.TARGETS, GuideScreenId.HOLDS_BACK, GuideScreenId.MONTHS_LATER, GuideScreenId.YOUR_TURN,
            ),
            Guide.SCREENS,
        )
        for (mode in modes) for (id in Guide.SCREENS) assertEquals(id, Guide.screen(id, mode).id)
    }

    @Test
    fun everyScreenOnBothSides_hasATitleAndUnderFortyWords() {
        for (mode in modes) for (id in Guide.SCREENS) {
            val screen = Guide.screen(id, mode)
            assertTrue(screen.title.isNotBlank(), "$id $mode: no title")
            assertTrue(screen.lines.isNotEmpty(), "$id $mode: no lines")
            assertTrue(screen.lines.all { it.text.isNotBlank() }, "$id $mode: a blank line")
            val count = words(text(screen))
            assertTrue(count <= 40, "$id $mode: $count words, title included")
        }
    }

    /** Every constant a line may name, resolved to the real value in the coach's code. */
    private val constants: Map<String, Double> = mapOf(
        "ProgressionRule.STEP_KG" to ProgressionRule.STEP_KG,
        "ProgressionRule.BIG_STEP_KG" to ProgressionRule.BIG_STEP_KG,
        "ProgressionRule.MAX_INCREASE" to ProgressionRule.MAX_INCREASE,
        "BodybuildingCoach.INCREASE" to BodybuildingCoach.INCREASE,
        "AdvicePayload.Deload.volumeFactor" to AdvicePayload.Deload().volumeFactor,
        "AdvicePayload.Deload.loadFactor" to AdvicePayload.Deload().loadFactor,
        "Coach.GAP_DAYS" to Coach.GAP_DAYS.toDouble(),
        "AdvicePayload.ReEntry.loadFactor" to AdvicePayload.ReEntry().loadFactor,
        "PhysiqueCoach.STUCK_DAYS" to PhysiqueCoach.STUCK_DAYS.toDouble(),
        "PhysiqueCoach.CHECK_IN_DAYS" to PhysiqueCoach.CHECK_IN_DAYS.toDouble(),
    )

    /**
     * The numbers each line shows, written by hand: (screen, side, line index) to the numbers in its text, in order. A
     * line not listed shows no number. Percentages are listed as the whole percent the text prints.
     */
    private val expected: Map<Triple<GuideScreenId, TrainingMode, Int>, List<Double>> = buildMap {
        val pl = TrainingMode.POWERLIFTING
        val bb = TrainingMode.BODYBUILDING
        put(Triple(GuideScreenId.TARGETS, pl, 0), listOf(2.5))
        put(Triple(GuideScreenId.TARGETS, pl, 1), listOf(5.0, 5.0))
        put(Triple(GuideScreenId.TARGETS, bb, 1), listOf(2.0))
        for (side in listOf(pl, bb)) {
            put(Triple(GuideScreenId.HOLDS_BACK, side, 0), listOf(60.0, 90.0))
            put(Triple(GuideScreenId.HOLDS_BACK, side, 1), listOf(14.0, 85.0))
        }
        put(Triple(GuideScreenId.HOLDS_BACK, bb, 3), listOf(21.0))
        put(Triple(GuideScreenId.MONTHS_LATER, bb, 1), listOf(28.0))
    }

    private class Number(val value: Double, val percent: Boolean)

    private fun numbers(text: String): List<Number> =
        Regex("(\\d+(?:\\.\\d+)?)(%?)").findAll(text).map { Number(it.groupValues[1].toDouble(), it.groupValues[2] == "%") }.toList()

    private fun close(a: Double, b: Double) = abs(a - b) < 1e-9

    /** The allowed conversions: a ratio to a whole percent (of itself, or of its rise over 1); anything else as written. */
    private fun matches(number: Number, constant: Double): Boolean =
        if (number.percent) {
            number.value == (constant * 100).roundToLong().toDouble() || number.value == ((constant - 1) * 100).roundToLong().toDouble()
        } else {
            close(number.value, constant)
        }

    @Test
    fun everyNumberInTheCopy_comesFromItsDeclaredConstant() {
        for (mode in modes) for (id in Guide.SCREENS) {
            val screen = Guide.screen(id, mode)
            assertTrue(numbers(screen.title).isEmpty(), "$id $mode: a number in the title")
            screen.lines.forEachIndexed { i, line ->
                val found = numbers(line.text)
                assertEquals(expected[Triple(id, mode, i)].orEmpty(), found.map { it.value }, "$id $mode line $i: \"${line.text}\"")
                val values = line.constants.map { name -> constants[name] ?: fail("$id $mode line $i: unknown constant $name") }
                for (n in found) {
                    assertTrue(values.any { matches(n, it) }, "$id $mode line $i: ${n.value} is not from ${line.constants}")
                }
                for ((name, value) in line.constants.zip(values)) {
                    assertTrue(found.any { matches(it, value) }, "$id $mode line $i: $name is declared but not shown")
                }
            }
        }
    }

    @Test
    fun bodybuildingOnlyRules_areAbsentOnPowerlifting_andTheReverse() {
        val coachScreens = listOf(GuideScreenId.HOLDS_BACK, GuideScreenId.MONTHS_LATER)
        val pl = coachScreens.joinToString(" ") { text(Guide.screen(it, TrainingMode.POWERLIFTING)) }.lowercase()
        val bb = coachScreens.joinToString(" ") { text(Guide.screen(it, TrainingMode.BODYBUILDING)) }.lowercase()
        for (word in listOf("swap", "check-in")) assertFalse(word in pl, "powerlifting mentions $word")
        for (word in listOf("stall", "weak")) assertFalse(word in bb, "bodybuilding mentions $word")
        assertTrue("swap" in bb && "check-in" in bb)
        assertTrue("stall" in pl)
    }

    @Test
    fun screen7_onPowerlifting_endsOnBestsAndTotal_notACheckIn() {
        val screen = Guide.screen(GuideScreenId.MONTHS_LATER, TrainingMode.POWERLIFTING)
        val last = screen.lines.last().text.lowercase()
        assertTrue("bests" in last && "total" in last, last)
        assertFalse("check-in" in text(screen).lowercase())
        assertEquals(GuideStep.MONTHS_ENDING, Guide.steps(GuideScreenId.MONTHS_LATER, TrainingMode.POWERLIFTING).last())
        val progress = GuideSample.progress(TrainingMode.POWERLIFTING)
        assertTrue(progress.totalKg != null && progress.laggingMuscle == null)
    }

    @Test
    fun noLineNamesASource() {
        val sources = listOf(
            CoachSources.BODYBUILDING_INCREASE, CoachSources.BUILDER_WEEKLY_SETS, CoachSources.BUILDER_FREQUENCY,
            CoachSources.BUILDER_ORDER, CoachSources.WEEKLY_CEILING, CoachSources.SKIP_PAIN_PHYSICIAN,
            CoachSources.SKIP_PAIN_TRAIN_AROUND, CoachSources.TEMPLATE_DAYS, CoachSources.TEMPLATE_STRENGTH_LOAD,
            CoachSources.TEMPLATE_STRENGTH_PHASE, CoachSources.TEMPLATE_NSCA_TABLE, CoachSources.TEMPLATE_STRENGTH_FLOOR,
            CoachSources.TEMPLATE_LOAD_CHART, CoachSources.TEMPLATE_ACCESSORY_RANGE, CoachSources.TEMPLATE_SPLIT_ADVANCED,
            CoachSources.TEMPLATE_SPLIT_BODYBUILDERS,
        )
        // "American College of Sports Medicine (ACSM)" -> the full name and "ACSM"; "ExRx.net" -> "ExRx".
        val names = sources.flatMap { s ->
            val p = s.publisher
            listOfNotNull(p, p.substringBefore(" (").trim(), Regex("\\(([^)]*)\\)").find(p)?.groupValues?.get(1), p.substringBefore("."))
        }.filter { it.length >= 3 }.toSet()
        for (mode in modes) for (id in Guide.SCREENS) {
            val all = text(Guide.screen(id, mode))
            for (name in names) assertFalse(all.contains(name, ignoreCase = name.length > 4), "$id $mode names $name")
        }
    }

    @Test
    fun theStep_isAPositiveTime() {
        assertTrue(Guide.STEP_MS > 0)
    }
}
