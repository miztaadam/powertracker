package com.powertracker.domain.library

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * docs/library/guides.html is one page with every form guide, for the owner to read through, with each exercise's
 * stretch and squeeze lines where they are written. It is generated from [Library.all] and [Library.focus]; this test
 * fails when it is stale and rewrites it with
 * `./gradlew :domain:jvmTest --tests '*GuidesDocTest*' -Pguides.update=true`.
 */
class GuidesDocTest {
    @Test
    fun guidesDoc_isCurrent() {
        val file = File(checkNotNull(System.getProperty("guides.file")) { "guides.file is set by domain/build.gradle.kts" })
        val expected = render()
        if (System.getProperty("guides.update") == "true") {
            file.parentFile.mkdirs()
            file.writeText(expected)
        }
        assertEquals(expected, if (file.exists()) file.readText() else "", "${file.path} is stale: rerun with -Pguides.update=true")
    }

    private fun esc(s: String) = buildString {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }

    private fun render(): String = buildString {
        val areas = BodyArea.entries.map { it to Library.all().filter { e -> e.area == it }.sortedBy { e -> Library.normalise(e.name) } }
            .filter { it.second.isNotEmpty() }
        appendLine("<!doctype html>")
        appendLine("<html lang=\"en\">")
        appendLine("<head>")
        appendLine("<meta charset=\"utf-8\">")
        appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        appendLine("<meta name=\"color-scheme\" content=\"light dark\">")
        appendLine("<title>Grit exercise guides</title>")
        appendLine("<style>")
        appendLine(CSS.trimIndent())
        appendLine("</style>")
        appendLine("</head>")
        appendLine("<body>")
        appendLine("<main>")
        appendLine("<h1>Grit exercise guides</h1>")
        appendLine("<p>Generated from the catalogue by <code>GuidesDocTest</code>. Do not edit by hand. ${Library.all().size} exercises. Read through and flag anything that reads oddly. Sources are in <a href=\"sources.md\">sources.md (in the repo)</a>.</p>")
        appendLine("<input id=\"q\" type=\"search\" placeholder=\"Search by name or alias\" aria-label=\"Search exercises\" autocomplete=\"off\">")
        appendLine("<p id=\"none\" hidden>No exercises match.</p>")
        appendLine("<nav aria-label=\"Contents\"><ul>")
        for ((area, list) in areas) appendLine("<li><a href=\"#${anchor(area)}\">${esc(title(area))}</a> (${list.size})</li>")
        appendLine("</ul></nav>")
        for ((area, list) in areas) {
            appendLine("<section class=\"area\" id=\"${anchor(area)}\">")
            appendLine("<h2>${esc(title(area))}</h2>")
            for (e in list) {
                val search = (listOf(e.name) + e.aliases).joinToString(" ") { Library.normalise(it) }
                appendLine("<article class=\"ex\" data-s=\"${esc(search)}\">")
                appendLine("<h3>${esc(e.name)}</h3>")
                appendLine("<p class=\"meta\">")
                if (e.aliases.isNotEmpty()) appendLine("<span><b>Also called:</b> ${esc(e.aliases.joinToString(", "))}</span>")
                appendLine("<span><b>Equipment:</b> ${esc(e.equipment.joinToString(", ") { equipment(it) })}</span>")
                appendLine("<span><b>Muscles:</b> ${esc(e.muscles.joinToString(", "))}</span>")
                appendLine("</p>")
                appendLine("<p>${esc(e.guide.purpose)}</p>")
                section("Setup", e.guide.setup, ordered = false)
                section("How to do it", e.guide.steps, ordered = true)
                section("Cues", e.guide.cues, ordered = false)
                when (val focus = Library.focus(e.name)) {
                    is MuscleFocus.Lines -> section(
                        "Stretch and squeeze (bodybuilding side)", listOf("Stretch: ${focus.stretch}", "Squeeze: ${focus.squeeze}"), ordered = false,
                    )
                    is MuscleFocus.NoFocus -> section("Stretch and squeeze (bodybuilding side)", listOf(Library.NO_FOCUS_LINE), ordered = false)
                    null -> Unit
                }
                section("Common mistakes", e.guide.mistakes, ordered = false)
                appendLine("<p class=\"safety\">${esc(Library.SAFETY_LINE)}</p>")
                appendLine("</article>")
            }
            appendLine("</section>")
        }
        appendLine("</main>")
        appendLine("<script>")
        appendLine(SCRIPT.trimIndent())
        appendLine("</script>")
        appendLine("</body>")
        appendLine("</html>")
    }

    private fun StringBuilder.section(heading: String, items: List<String>, ordered: Boolean) {
        val tag = if (ordered) "ol" else "ul"
        appendLine("<h4>$heading</h4>")
        appendLine("<$tag>")
        for (i in items) appendLine("<li>${esc(i)}</li>")
        appendLine("</$tag>")
    }

    private fun anchor(area: BodyArea) = "area-" + area.name.lowercase().replace('_', '-')

    private fun equipment(e: Equipment) = when (e) {
        Equipment.BARBELL -> "Barbell"
        Equipment.DUMBBELL -> "Dumbbell"
        Equipment.KETTLEBELL -> "Kettlebell"
        Equipment.CABLE -> "Cable"
        Equipment.MACHINE -> "Machine"
        Equipment.BODYWEIGHT -> "Bodyweight"
        Equipment.BAND -> "Band"
        Equipment.SPECIALTY_BAR -> "Specialty bar"
        Equipment.SMITH -> "Smith machine"
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

    private companion object {
        const val CSS = """
            :root { color-scheme: light dark; --bg: #fbfaf8; --fg: #1f1d1a; --muted: #5d5850; --line: #dcd8d0; --card: #ffffff; --accent: #46423B; }
            @media (prefers-color-scheme: dark) {
              :root { --bg: #14130f; --fg: #ece8e0; --muted: #a8a296; --line: #3a372f; --card: #1d1b16; --accent: #DCD6C8; }
            }
            * { box-sizing: border-box; }
            html { -webkit-text-size-adjust: 100%; }
            body { margin: 0; background: var(--bg); color: var(--fg); font: 16px/1.5 -apple-system, system-ui, "Segoe UI", Roboto, sans-serif; }
            main { max-width: 46rem; margin: 0 auto; padding: 16px; }
            h1, h2, h3, h4 { line-height: 1.25; overflow-wrap: anywhere; }
            h1, h2, h3 { color: var(--accent); }
            h1 { font-size: 1.6rem; }
            h2 { font-size: 1.35rem; margin-top: 2rem; border-bottom: 1px solid var(--line); padding-bottom: .25rem; }
            h3 { font-size: 1.15rem; margin: 0 0 .25rem; }
            h4 { font-size: 1rem; margin: .9rem 0 .2rem; }
            a { color: var(--accent); }
            code { font-size: .9em; }
            input { width: 100%; font: inherit; padding: 10px 12px; border: 1px solid var(--line); border-radius: 8px; background: var(--card); color: var(--fg); }
            nav ul { list-style: none; padding: 0; display: flex; flex-wrap: wrap; gap: 6px 16px; }
            .ex { background: var(--card); border: 1px solid var(--line); border-radius: 10px; padding: 12px 14px; margin: 12px 0; overflow-wrap: anywhere; }
            .meta { display: flex; flex-direction: column; gap: 2px; color: var(--muted); margin: 0 0 .5rem; }
            ul, ol { margin: 0; padding-left: 1.3rem; }
            .safety { color: var(--muted); font-style: italic; margin-bottom: 0; }
            [hidden] { display: none !important; }
        """
        const val SCRIPT = """
            (function () {
              var q = document.getElementById('q');
              var none = document.getElementById('none');
              var items = Array.prototype.slice.call(document.querySelectorAll('.ex'));
              var areas = Array.prototype.slice.call(document.querySelectorAll('.area'));
              function fold(s) { return s.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim(); }
              q.addEventListener('input', function () {
                var t = fold(q.value);
                var shown = 0;
                items.forEach(function (el) {
                  var hit = !t || (' ' + el.getAttribute('data-s') + ' ').indexOf(t) !== -1;
                  el.hidden = !hit;
                  if (hit) shown++;
                });
                areas.forEach(function (a) { a.hidden = !a.querySelector('.ex:not([hidden])'); });
                none.hidden = shown !== 0;
              });
            })();
        """
    }
}
