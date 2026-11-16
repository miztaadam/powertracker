package com.powertracker.domain.library

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * docs/library/sources.md lists every catalogue exercise's sources (and its stretch and squeeze lines, with theirs), by
 * area, then the RPE sheet's. It is generated from [Library.all], [Library.focus], [RpeGuide] and [Sources.all]; this
 * test fails when it is stale and rewrites it with
 * `./gradlew :domain:jvmTest --tests '*SourcesDocTest*' -Psources.update=true`.
 */
class SourcesDocTest {
    @Test
    fun sourcesDoc_isCurrent() {
        val file = File(checkNotNull(System.getProperty("sources.file")) { "sources.file is set by domain/build.gradle.kts" })
        val expected = render()
        if (System.getProperty("sources.update") == "true") {
            file.parentFile.mkdirs()
            file.writeText(expected)
        }
        assertEquals(expected, if (file.exists()) file.readText() else "", "${file.path} is stale: rerun with -Psources.update=true")
    }

    private fun render(): String = buildString {
        appendLine("# Exercise library sources")
        appendLine()
        appendLine("Generated from `Library.all()`, `Library.focus`, `RpeGuide` and `Sources.all` by `SourcesDocTest`. Do not edit by hand. Regenerate with")
        appendLine("`./gradlew :domain:jvmTest --tests '*SourcesDocTest*' -Psources.update=true`.")
        appendLine()
        appendLine("Each exercise lists the sources its form guide was checked against. Sources are not shown in the app.")
        appendLine("Where an exercise has its stretch and squeeze lines, they follow, with their own sources. The lines are worked out")
        appendLine("from the exercise's own guide, never asking for more range than its steps; no source is claimed for squeezing.")
        for (area in BodyArea.entries) {
            appendLine()
            appendLine("## ${title(area)}")
            val exercises = Library.all().filter { it.area == area }.sortedBy { Library.normalise(it.name) }
            if (exercises.isEmpty()) {
                appendLine()
                appendLine("No exercises yet.")
            }
            for (e in exercises) {
                appendLine()
                appendLine("### ${e.name}")
                appendLine()
                for (id in e.sources) {
                    appendLine(line(id))
                }
                val focus = Library.focus(e.name) ?: continue
                appendLine()
                when (focus) {
                    is MuscleFocus.Lines -> {
                        appendLine("Stretch and squeeze (bodybuilding side):")
                        appendLine()
                        appendLine("- Stretch: ${focus.stretch}")
                        appendLine("- Squeeze: ${focus.squeeze}")
                    }
                    is MuscleFocus.NoFocus -> {
                        appendLine("Stretch and squeeze (bodybuilding side):")
                        appendLine()
                        appendLine("- ${Library.NO_FOCUS_LINE}")
                    }
                }
                appendLine()
                appendLine("Sources of the stretch and squeeze:")
                appendLine()
                for (id in focus.sources) appendLine(line(id))
            }
        }
        appendLine()
        appendLine("## RPE scale")
        appendLine()
        appendLine("The sources of the \"What RPE means\" sheet.")
        appendLine()
        for (id in RpeGuide.sources) appendLine(line(id))
    }

    private fun line(id: SourceId): String {
        val s = Sources.all.getValue(id)
        return "- [${s.title}](${s.url}). ${s.publisher}, accessed ${s.accessed}."
    }

    private fun title(area: BodyArea) = when (area) {
        BodyArea.MAIN_LIFTS -> "Main lifts"
        BodyArea.CHEST -> "Chest"
        BodyArea.BACK -> "Back"
        BodyArea.SHOULDERS -> "Shoulders"
        BodyArea.ARMS -> "Arms"
        BodyArea.QUADS -> "Quads"
        BodyArea.HAMSTRINGS_GLUTES -> "Hamstrings and glutes"
        BodyArea.CALVES -> "Calves"
        BodyArea.CORE -> "Core"
    }
}
