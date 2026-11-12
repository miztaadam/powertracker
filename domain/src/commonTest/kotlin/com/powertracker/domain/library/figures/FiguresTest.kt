package com.powertracker.domain.library.figures

import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
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
    @Ignore // switched on in the final task
    fun everyCatalogueExercise_hasFigure() {
        val missing = Library.all().map { it.name }.filter { Library.figure(it) == null }
        assertEquals(emptyList(), missing)
    }

    private companion object {
        const val GOLDEN = "10:d2bea818efe378a3"
    }
}
