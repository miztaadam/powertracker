package com.powertracker.domain.web

import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.PlanTemplates
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlanTemplatesBridgeTest {
    @Test
    fun templatesCrossTheBoundaryFewestDaysFirst() {
        val pl = GritDomain.planTemplatesFor("POWERLIFTING")
        assertEquals(listOf("pl-3", "pl-4", "pl-5"), pl.map { it.id })
        assertEquals(listOf(3, 4, 5), pl.map { it.dayCount })
        assertTrue(pl.all { it.mode == "POWERLIFTING" })
        val domain = PlanTemplates.forMode(TrainingMode.POWERLIFTING)[0]
        assertEquals(domain.sessions.map { it.title }, pl[0].sessions.map { it.title })
        val squat = pl[0].sessions[0].exercises[0]
        assertEquals(JsTemplateExercise("Squat", 3, 5, null, 8.0, false), squat)
        assertTrue(pl[0].sessions[1].exercises.any { it.name == "Strict press" && it.onlyWithStrictPress })
        assertEquals(listOf("bb-3", "bb-4", "bb-5"), GritDomain.planTemplatesFor("BODYBUILDING").map { it.id })
        assertEquals(12, GritDomain.planTemplatesFor("BODYBUILDING")[0].sessions[0].exercises[0].repsHigh)
        assertFailsWith<IllegalArgumentException> { GritDomain.planTemplatesFor("CARDIO") }
    }

    @Test
    fun suggestedDaysAreWeekdayNumbers() {
        assertContentEquals(arrayOf(1, 3, 5), GritDomain.planTemplateSuggestedDays(3, arrayOf(5, 1, 3)))
        assertContentEquals(arrayOf(1, 2, 4, 5), GritDomain.planTemplateSuggestedDays(4, arrayOf(1, 3, 5)))
    }

    @Test
    fun draftAlternativesAndSwapMatchTheDomain() {
        val draft = GritDomain.planTemplateDraft("pl-4", arrayOf(5, 1, 2, 4), true)
        val domain = PlanTemplates.draft(
            PlanTemplates.byId("pl-4")!!, listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), true,
        )
        // Arrays inside the JS data classes compare by identity, so compare through the domain.
        assertEquals(domain, draft.toDomain())
        assertEquals(listOf(1, 2, 4, 5), draft.sessions.map { it.day })
        assertEquals(4, draft.sessions[0].exercises[0].repsHigh)
        assertTrue(draft.sessions.flatMap { it.exercises.toList() }.all { it.slot != null })

        assertEquals(0, GritDomain.planTemplateAlternatives("pl-4", draft, 1, "Squat").size)
        val alts = GritDomain.planTemplateAlternatives("pl-4", draft, 2, "Close-grip bench")
        assertEquals(
            PlanTemplates.alternatives(PlanTemplates.byId("pl-4")!!, domain, DayOfWeek.TUESDAY, "Close-grip bench").map { it.name },
            alts.map { it.name },
        )
        val swapped = GritDomain.planTemplateSwap(draft, 2, "Close-grip bench", alts[0].name)
        val after = swapped.sessions[1].exercises.first { it.name == alts[0].name }
        assertEquals(listOf(3, 5, 5), listOf(after.sets, after.repsLow, after.repsHigh))

        assertFailsWith<IllegalArgumentException> { GritDomain.planTemplateDraft("nope", arrayOf(1, 3, 5), false) }
        assertFailsWith<IllegalArgumentException> { GritDomain.planTemplateDraft("pl-3", arrayOf(1, 3), false) }
        assertFailsWith<IllegalArgumentException> { GritDomain.planTemplateDraft("pl-3", arrayOf(1, 1, 3), false) }
    }
}
