package com.powertracker.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The retired green accent must not come back: neither its hex values nor its name, in the app's
 * sources and resources or in the design system document. The web app has its own check
 * (`web/src/ui/noVolt.test.ts`); `docs/design/chalk-options/` documents the old baseline and is
 * not scanned.
 */
class NoVoltTest {
    private val retired = listOf("C9F73A", "121700", "25300A", "DDFB86", "3F6100", "E6F9A8", "2A3F00")
    private val hexForms = retired.flatMap { listOf("#$it", "0xFF$it") }.map { it.lowercase() }
    private val word = Regex("\\bvolt\\b", RegexOption.IGNORE_CASE)

    // Unit tests run with the module directory (app/) as the working directory.
    private val main = File("src/main")
    private val designDoc = File("../docs/design/DESIGN.md")

    private val sources = main.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml") }.toList()

    private fun hits(files: List<File>): List<String> = files.flatMap { f ->
        val text = f.readText()
        val lower = text.lowercase()
        hexForms.filter { it in lower }.map { "${f.path}: $it" } +
            word.findAll(text).map { "${f.path}: \"${it.value}\"" }
    }

    @Test
    fun scansTheAppSourcesAndTheDesignDoc() {
        assertTrue("only ${sources.size} files under ${main.absolutePath}", sources.size > 50)
        assertTrue(sources.any { it.path.endsWith("ui/theme/Color.kt") })
        assertTrue(sources.any { it.path.endsWith("res/values-night/colors.xml") })
        assertTrue("${designDoc.absolutePath} is missing", designDoc.isFile)
    }

    @Test
    fun appSourcesAndResources_haveNoVolt() = assertEquals(emptyList<String>(), hits(sources))

    @Test
    fun designDoc_hasNoVolt() = assertEquals(emptyList<String>(), hits(listOf(designDoc)))
}
