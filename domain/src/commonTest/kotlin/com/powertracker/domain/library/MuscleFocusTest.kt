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
    fun focus_findsByNameIgnoringCase() {
        val squat = assertNotNull(Library.focus("Squat"))
        assertEquals(squat, Library.focus("SQUAT"))
        assertEquals(squat, Library.focus("back squat"), "an alias finds the same lines, as it finds the same guide")
        assertNull(Library.focus("My own lift"))
    }
}
