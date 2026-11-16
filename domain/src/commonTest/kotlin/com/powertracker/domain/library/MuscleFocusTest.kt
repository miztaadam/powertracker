package com.powertracker.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MuscleFocusTest {
    private val entries = MuscleFocuses.areas.flatten()

    private fun lines(): List<Pair<String, String>> = entries.flatMap { (name, focus) ->
        when (focus) {
            is MuscleFocus.Lines -> listOf(name to focus.stretch, name to focus.squeeze)
            is MuscleFocus.NoFocus -> emptyList()
        }
    }

    @Test
    fun focus_namesOnlyCatalogueExercises() {
        val catalogue = Library.all().map { it.name }.toSet()
        val unknown = entries.map { it.first }.filter { it !in catalogue }
        assertTrue(unknown.isEmpty(), "not catalogue names (spelled exactly): $unknown")
        val repeated = entries.groupBy { it.first }.filterValues { it.size > 1 }.keys
        assertTrue(repeated.isEmpty(), "written more than once: $repeated")
    }

    @Test
    fun focus_linesHaveTheirShape() {
        for ((name, line) in lines()) {
            assertTrue(line.isNotBlank(), "$name: blank line")
            assertTrue(line.length <= 64, "$name: ${line.length} characters: $line")
            assertTrue(line.first().isUpperCase(), "$name: no capital: $line")
            assertTrue(line.endsWith("."), "$name: no full stop: $line")
        }
        assertEquals("No stretch or squeeze to aim for here: this one trains you to stay steady.", Library.NO_FOCUS_LINE)
    }

    /**
     * F1, with its exception for variations (controller ruling, 2026-11-16): the muscle a line names belongs to the
     * same muscle group as one in the exercise's own list or, for a main-lift variation, in its parent lift's entry.
     * Lines start "Your <muscle> stretch(es) ..." / "Your <muscle> is ..." and "Squeeze your <muscle> as ...".
     */
    @Test
    fun focus_namesAMuscleOfTheExerciseOrItsParent() {
        val stretch = Regex("^Your (.+?) (?:stretch|stretches|is) ")
        val squeeze = Regex("^Squeeze your (.+?) as ")
        for ((name, focus) in entries) {
            if (focus !is MuscleFocus.Lines) continue
            val e = assertNotNull(Library.find(name))
            val parent = e.parentLift?.let { lift -> Library.all().single { it.parentLift == null && it.category.name == lift.name } }
            val groups = (e.muscles + parent?.muscles.orEmpty()).mapNotNull { MuscleGroups.of(it) }.toSet()
            for ((rule, line) in listOf(stretch to focus.stretch, squeeze to focus.squeeze)) {
                val named = assertNotNull(rule.find(line), "$name: line does not name its muscle in the usual way: $line").groupValues[1]
                val group = assertNotNull(MuscleGroups.of(named), "$name: '$named' is not a muscle group")
                assertTrue(group in groups, "$name: '$named' is not in its muscles${if (parent != null) " or its parent's" else ""}")
            }
        }
    }

    @Test
    fun focus_hasSources() {
        for ((name, focus) in entries) {
            assertTrue(focus.sources.isNotEmpty(), "$name: no sources")
            for (id in focus.sources) assertNotNull(Sources.all[id], "$name: unknown source ${id.key}")
        }
    }

    @Test
    fun focus_neverMentionsBelowTheBench() {
        val deep = lines().filter { (_, line) -> "below the bench" in line.lowercase() }
        assertTrue(deep.isEmpty(), "asks for more range than the guide: $deep")
    }

    @Test
    fun focus_mainLiftsAreAllWritten() {
        val missing = Library.all().filter { it.area == BodyArea.MAIN_LIFTS && Library.focus(it.name) == null }.map { it.name }
        assertTrue(missing.isEmpty(), "main lifts with no lines: $missing")
    }

    @Test
    fun focus_chestIsAllWritten() {
        val missing = Library.all().filter { it.area == BodyArea.CHEST && Library.focus(it.name) == null }.map { it.name }
        assertTrue(missing.isEmpty(), "chest exercises with no lines: $missing")
        // The plan's case check, once Chest is written.
        val fly = assertNotNull(Library.focus("DB fly"))
        assertEquals(fly, Library.focus("dB FLY"))
    }

    @Test
    fun focus_backIsAllWritten() {
        val missing = Library.all().filter { it.area == BodyArea.BACK && Library.focus(it.name) == null }.map { it.name }
        assertTrue(missing.isEmpty(), "back exercises with no lines: $missing")
    }

    @Test
    fun focus_shouldersIsAllWritten() {
        val missing = Library.all().filter { it.area == BodyArea.SHOULDERS && Library.focus(it.name) == null }.map { it.name }
        assertTrue(missing.isEmpty(), "shoulder exercises with no lines: $missing")
    }

    @Test
    fun focus_armsIsAllWritten() {
        val missing = Library.all().filter { it.area == BodyArea.ARMS && Library.focus(it.name) == null }.map { it.name }
        assertTrue(missing.isEmpty(), "arm exercises with no lines: $missing")
    }

    @Test
    fun focus_quadsIsAllWritten() {
        val missing = Library.all().filter { it.area == BodyArea.QUADS && Library.focus(it.name) == null }.map { it.name }
        assertTrue(missing.isEmpty(), "quad exercises with no lines: $missing")
    }

    @Test
    fun focus_findsByNameIgnoringCase() {
        val squat = assertNotNull(Library.focus("Squat"))
        assertEquals(squat, Library.focus("SQUAT"))
        assertEquals(squat, Library.focus("back squat"), "an alias finds the same lines, as it finds the same guide")
        assertNull(Library.focus("My own lift"))
    }
}
