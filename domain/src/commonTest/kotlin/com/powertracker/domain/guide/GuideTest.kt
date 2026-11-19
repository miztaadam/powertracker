package com.powertracker.domain.guide

import com.powertracker.domain.coach.BodybuildingCoach
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.CoachSources
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.coach.ProgressionRule
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
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
            // "Under about 40": 38 or fewer, so no screen sits at the limit.
            assertTrue(count <= 38, "$id $mode: $count words, title included")
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
        // Stall and weak point share one line and show no number: the stall window is a setting (CoachSettings.stallWeeks).
        put(Triple(GuideScreenId.HOLDS_BACK, pl, 3), emptyList())
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
        // Stall and weak point (a lift lagging the others) share powerlifting's last screen-6 line.
        assertTrue("stall" in pl && "lagging" in pl)
    }

    @Test
    fun stallAndWeakPoint_areSuggestions_notChangesTheCoachMakes() {
        // Both cards' exercises reach the plan only when the lifter applies the card.
        val line = Guide.screen(GuideScreenId.HOLDS_BACK, TrainingMode.POWERLIFTING).lines.last().text
        assertTrue("stalled" in line.lowercase() && "lagging" in line.lowercase() && "suggest" in line, line)
    }

    @Test
    fun theDeloadLine_saysAbout_asTheCardDoes() {
        val deload = AdvicePayload.Deload()
        val volume = (deload.volumeFactor * 100).roundToLong()
        val load = (deload.loadFactor * 100).roundToLong()
        for (mode in modes) {
            val line = Guide.screen(GuideScreenId.HOLDS_BACK, mode).lines.first().text
            assertTrue("about $volume% of sets at about $load% of the weight" in line, "$mode: $line")
        }
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

    @Test
    fun openingTheGuide_readsNoSample() {
        for (mode in modes) {
            val before = GuideSample.reads
            Guide.screen(GuideScreenId.WELCOME, mode)
            Guide.steps(GuideScreenId.WELCOME, mode)
            assertEquals(before, GuideSample.reads, "$mode: screen 1 read the sample")
        }
    }

    @Test
    fun countingSteps_readsNoSample() {
        for (mode in modes) {
            val before = GuideSample.reads
            for (id in Guide.SCREENS) {
                Guide.screen(id, mode)
                Guide.steps(id, mode)
            }
            assertEquals(before, GuideSample.reads, "$mode: a screen's copy or steps read the sample")
        }
    }

    @Test
    fun theEasingBackDays_areTheGapTheCardFiresAfter() {
        // The copy's number is Coach.GAP_DAYS; the RE_ENTRY card fires from HistoryStats.returnDateAfterGap.
        val start = GuideSample.TODAY
        fun returnAfter(days: Long): Boolean {
            val back = start.plus(days, DateTimeUnit.DAY)
            val sessions = listOf(start, back).mapIndexed { i, d -> LoggedSession(i + 1L, d, null, null, emptyList(), TrainingMode.POWERLIFTING) }
            return HistoryStats(TrainingHistory(sessions, emptyMap(), emptyList())).returnDateAfterGap(back) == back
        }
        assertTrue(returnAfter(Coach.GAP_DAYS), "a gap of Coach.GAP_DAYS is a return")
        assertFalse(returnAfter(Coach.GAP_DAYS - 1), "a day short of Coach.GAP_DAYS is not")
    }

    // ---- time: one definition for both apps (Task 2b) ----

    @Test
    fun frame_stepsAndProgress_overTime() {
        val id = GuideScreenId.TODAY
        for (mode in modes) {
            val steps = Guide.steps(id, mode)
            val span = Guide.MOVE_MS + Guide.STEP_MS
            assertTrue(Guide.MOVE_MS in 280..320, "a move as long as the apps' own: ${Guide.MOVE_MS}")
            // The first step is simply there: nothing moves into it, and it holds for a step's span.
            assertEquals(GuideFrame(0, steps[0], 1.0, false), Guide.frame(id, mode, 0))
            assertEquals(GuideFrame(0, steps[0], 1.0, false), Guide.frame(id, mode, -50))
            assertEquals(GuideFrame(0, steps[0], 1.0, false), Guide.frame(id, mode, span - 1))
            for (i in 1 until steps.size) {
                val start = i * span
                assertEquals(start, Guide.stepStartMs(i))
                assertEquals(start + Guide.MOVE_MS, Guide.settledMs(i))
                // Its move: progress from 0, rising, to 1 at the move's end; then it holds at 1 until the next step.
                assertEquals(GuideFrame(i, steps[i], 0.0, false), Guide.frame(id, mode, start))
                val mid = Guide.frame(id, mode, start + Guide.MOVE_MS / 2)
                assertEquals(i, mid.stepIndex)
                assertTrue(mid.progress > 0.0 && mid.progress < 1.0, "step $i mid-move: ${mid.progress}")
                val progress = (0..Guide.MOVE_MS step 10).map { Guide.frame(id, mode, start + it).progress }
                assertEquals(progress, progress.sorted(), "step $i: progress never goes back")
                val last = i == steps.lastIndex
                assertEquals(GuideFrame(i, steps[i], 1.0, last), Guide.frame(id, mode, start + Guide.MOVE_MS))
                if (!last) assertEquals(GuideFrame(i, steps[i], 1.0, false), Guide.frame(id, mode, start + span - 1))
            }
            // The last step holds for good, done.
            val end = Guide.settledMs(steps.lastIndex)
            assertEquals(end, Guide.doneMs(steps.size))
            assertFalse(Guide.frame(id, mode, end - 1).done)
            for (t in listOf(end, end + 1, end + 60_000)) assertEquals(GuideFrame(steps.lastIndex, steps.last(), 1.0, true), Guide.frame(id, mode, t))
        }
        // A picture of one step is done at once.
        assertEquals(GuideFrame(0, GuideStep.SIDE_LOOP, 1.0, true), Guide.frame(GuideScreenId.SIDE, TrainingMode.POWERLIFTING, 0))
        assertEquals(0, Guide.doneMs(1))
    }

    @Test
    fun ease_isTheWebsMoveCurve_fromZeroToOne() {
        assertEquals(0.0, Guide.ease(0.0))
        assertEquals(1.0, Guide.ease(1.0))
        assertEquals(0.0, Guide.ease(-1.0))
        assertEquals(1.0, Guide.ease(2.0))
        val curve = (0..100).map { Guide.ease(it / 100.0) }
        assertEquals(curve, curve.sorted())
        // cubic-bezier(0.2, 0.8, 0.2, 1): fast out, slow in. At a quarter of the time it is well over half way.
        assertTrue(abs(Guide.ease(0.25) - 0.767) < 0.002, "${Guide.ease(0.25)}")
        assertTrue(abs(Guide.ease(0.5) - 0.946) < 0.002, "${Guide.ease(0.5)}")
    }
}
