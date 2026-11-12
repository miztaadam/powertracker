package com.powertracker.domain.library

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RpeGuideTest {
    private val lines = RpeGuide.lines

    @Test
    fun title_isNotBlank() {
        assertTrue(RpeGuide.title.isNotBlank())
    }

    @Test
    fun lines_areShortAndNotBlank() {
        assertTrue(lines.size in 6..9, "about 6-9 lines, got ${lines.size}")
        assertTrue(lines.none { it.isBlank() }, "blank line")
    }

    @Test
    fun explainsEachValueFrom6To10_inRepsInReserve() {
        fun line(v: String) = assertNotNull(lines.firstOrNull { it.startsWith("$v:") }, "no line for $v").lowercase()
        assertTrue("no more reps" in line("10"), "10")
        assertTrue("one more" in line("9"), "9")
        assertTrue("two more" in line("8"), "8")
        assertTrue("three more" in line("7"), "7")
        assertTrue("four to six more" in line("6"), "6")
        assertTrue(lines.any { "reps in reserve" in it.lowercase() }, "names reps in reserve")
    }

    @Test
    fun explainsHalfPoints_withoutAGeneralRule() {
        assertTrue("maybe two" in assertNotNull(lines.firstOrNull { it.startsWith("8.5:") }, "8.5"), "8.5")
        val nineAndHalf = assertNotNull(lines.firstOrNull { it.startsWith("9.5:") }, "9.5")
        assertTrue("no more reps" in nineAndHalf && "load" in nineAndHalf, "9.5")
        // 9.5 is no more reps, so no line may claim every half point means one more.
        assertTrue(lines.none { "half point" in it.lowercase() }, "general half-point rule")
    }

    @Test
    fun statesTheAppRange() {
        assertTrue(lines.any { "6 to 10" in it && "0.5" in it }, "app range 6-10 in 0.5 steps")
    }

    @Test
    fun sources_areKnownAndAllowed() {
        val allowed = listOf("acefitness.org", "exrx.net", "nsca.com", "acsm.org", "pmc.ncbi.nlm.nih.gov", "powerlifting.sport")
        assertTrue(RpeGuide.sources.isNotEmpty(), "no sources")
        for (id in RpeGuide.sources) {
            val s = assertNotNull(Sources.all[id], "unknown source ${id.key}")
            val host = s.url.removePrefix("https://").substringBefore('/')
            assertTrue(allowed.any { host == it || host == "www.$it" }, "${id.key}: $host is not allowed")
            assertTrue("/blog/" !in s.url, "${id.key}: blog post")
        }
    }

    @Test
    fun noSensationOrPainWording() {
        val banned = listOf("pain", "burn", "hurt", "feel", "sensation", "sore", "ache")
        for (line in lines + RpeGuide.title) {
            for (word in banned) assertTrue(word !in line.lowercase(), "\"$word\" in: $line")
        }
        assertTrue(lines.none { Library.SAFETY_LINE in it }, "no safety line")
    }
}
