package com.powertracker.domain.library.figures

import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.hasFigure
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FiguresTest {
    @Test
    fun everyFigureNameIsInCatalogue() {
        val notInCatalogue = Figures.all.keys.filter { Library.find(it)?.name != it }
        assertEquals(emptyList(), notInCatalogue)
        assertTrue("Bench" in Figures.all)
    }

    @Test
    fun everyFigurePassesChecks() {
        val problems = Figures.all.flatMap { (name, figure) -> FigureChecks.check(name, figure) }
        assertEquals(emptyList(), problems)
    }

    @Test
    fun fingerprint_matchesGolden() {
        // The parity check: the same on the JVM and JS builds. Update it only when a figure or the renderer changes on purpose.
        assertEquals(GOLDEN, Figures.fingerprint())
    }

    @Test
    fun figure_resolvesAliasesAndFormerNames() {
        val fixture = FigureFixtures.benchDraft()
        // Whatever "Strict press" resolves to without the test registration (none now, a real figure later).
        val before = Library.figure("Overhead press")
        Figures.registerForTest("Strict press", fixture)
        try {
            assertSame(fixture, Library.figure("Strict press"))
            assertSame(fixture, Library.figure("Overhead press"))
            assertSame(fixture, Library.figure("OHP"))
            assertSame(fixture, Library.figure("  overhead-press "))
        } finally {
            Figures.unregisterForTest("Strict press")
        }
        assertSame(before, Library.figure("Overhead press"))
        assertSame(before, Library.figure("OHP"))
        // A real entry resolves by its former names and aliases the same way.
        val bench = assertNotNull(Library.figure("Bench"))
        assertSame(bench, Library.figure("Barbell bench press"))
        assertSame(bench, Library.figure("bench press"))
        assertNull(Library.figure("My own exercise"))
    }

    @Test
    fun hasFigure_resolvesNamesLikeFigure() {
        // Names, aliases and former names, the same as Library.figure; nothing for the lifter's own exercise.
        for (name in listOf("Bench", "bench press", "Barbell bench press", "Overhead press", "OHP", "  overhead-press ")) {
            assertTrue(Library.hasFigure(name), name)
        }
        assertFalse(Library.hasFigure("My own exercise"))
        assertFalse(Library.hasFigure(""))
        // A catalogue exercise not drawn yet has none, until a test registers one under its name. Since Task 12 every
        // catalogue exercise is drawn, so there is none left to try this on.
        val undrawn = Library.all().firstOrNull { it.name !in Figures.all }?.name
        if (undrawn != null) {
            assertFalse(Library.hasFigure(undrawn))
            Figures.registerForTest(undrawn, FigureFixtures.benchDraft())
            try {
                assertTrue(Library.hasFigure(undrawn))
            } finally {
                Figures.unregisterForTest(undrawn)
            }
            assertFalse(Library.hasFigure(undrawn))
        }
        // The same answer as looking the figure up, for every catalogue name.
        for (entry in Library.all()) assertEquals(Library.figure(entry.name) != null, Library.hasFigure(entry.name), entry.name)
    }

    @Test
    fun figures_areBuiltOnFirstUse() {
        // Two areas whose figures count how often they are built.
        val built = mutableListOf<String>()
        val fixture = FigureFixtures.benchDraft()
        fun counted(name: String) = lazy { built += name; fixture }
        val figures = lazyFigures(listOf(mapOf("A" to counted("A")), mapOf("B" to counted("B"), "C" to counted("C"))))
        // Listing the names, counting them and asking which there are builds none.
        assertEquals(listOf("A", "B", "C"), figures.keys.toList())
        assertEquals(3, figures.size)
        assertTrue("B" in figures && "Z" !in figures)
        assertNull(figures["Z"])
        assertEquals(emptyList(), built)
        // Looking one up builds that one only, once.
        assertSame(fixture, figures["B"])
        assertSame(fixture, figures["B"])
        assertEquals(listOf("B"), built)
        // Walking them all builds the rest, in order.
        assertEquals(3, figures.entries.count { it.value === fixture })
        assertEquals(listOf("B", "A", "C"), built)
        // Two figures under one name are still refused, without building either.
        assertFailsWith<IllegalArgumentException> { lazyFigures(listOf(mapOf("A" to counted("A2")), mapOf("A" to counted("A3")))) }
        assertEquals(listOf("B", "A", "C"), built)
    }

    @Test
    @Ignore // switched on in the final task
    fun everyCatalogueExercise_hasFigure() {
        val missing = Library.all().map { it.name }.filter { Library.figure(it) == null }
        assertEquals(emptyList(), missing)
    }

    private companion object {
        const val GOLDEN = "225:f0c60837400511bf"
    }
}
