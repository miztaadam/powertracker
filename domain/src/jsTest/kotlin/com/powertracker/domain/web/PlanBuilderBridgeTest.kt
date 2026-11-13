package com.powertracker.domain.web

import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.plan.BuilderAnswers
import com.powertracker.domain.plan.BuilderPick
import com.powertracker.domain.plan.PlanBuilder
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlanBuilderBridgeTest {
    private val names = listOf("DB press", "Lat pulldown", "Leg press", "Lateral raise")
    private val picks = names.map { Library.find(it)!! }
    private val jsPicks = picks.map {
        JsBuilderPick(it.name, it.muscles.toTypedArray(), it.kind == Kind.COMPOUND, it.equipment.map { e -> e.name }.toTypedArray(), true)
    }.toTypedArray()
    private val own = JsBuilderPick("My sled push", emptyArray(), false, emptyArray(), false)

    @Test
    fun buildCrossesTheBoundaryAsTheDomainBuildsIt() {
        val draft = GritDomain.planBuilderBuild(arrayOf(1, 2, 4, 5), jsPicks + own, arrayOf("SIDE_DELTS"), 1)
        val domain = PlanBuilder.build(
            BuilderAnswers(
                listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                picks.map { BuilderPick(it.name, it.muscles, it.kind == Kind.COMPOUND, it.equipment, true) } +
                    BuilderPick("My sled push", emptyList(), false, emptyList(), false),
                listOf(com.powertracker.domain.library.MuscleGroup.SIDE_DELTS),
                1,
            ),
        )
        // Arrays inside the JS data classes compare by identity, so compare through the domain.
        assertEquals(domain, draft.toDomain())
        assertEquals(listOf(1, 2, 4, 5), draft.sessions.map { it.day })
        assertEquals(listOf("Upper", "Lower", "Upper", "Lower"), draft.sessions.map { it.title })
        assertEquals("SIDE_DELTS", draft.sessions[0].exercises[0].mainGroup)
        assertEquals(4, draft.sessions[0].exercises[0].sets)
        assertEquals(12, draft.volume.size)
        assertEquals("CHEST", draft.volume[0].group)
        val held = draft.sessions.flatMap { it.exercises.toList() }.single { it.name == "My sled push" }
        assertEquals(null, held.mainGroup)
        assertEquals(null, held.slot)
        assertEquals("CHEST_PRESS", draft.sessions[0].exercises.single { it.lead }.slot)
    }

    @Test
    fun alternativesAndSwapCrossTheBoundary() {
        val draft = GritDomain.planBuilderBuild(arrayOf(1, 3, 5), jsPicks, emptyArray(), 0)
        val from = draft.sessions[0].exercises[0]
        val alts = GritDomain.planBuilderAlternatives(from.name, draft, jsPicks, 1)
        assertTrue(alts.isNotEmpty())
        val swapped = GritDomain.planBuilderSwap(draft, 1, from.name, alts[0].name)
        assertEquals(alts[0].name, swapped.sessions[0].exercises[0].name)
        assertEquals(from.sets, swapped.sessions[0].exercises[0].sets)
        assertEquals(draft.toDomain().sessions[1], swapped.toDomain().sessions[1])
    }

    @Test
    fun badInputNamesTheField() {
        assertFailsWith<IllegalArgumentException> { GritDomain.planBuilderBuild(arrayOf(1, 3), jsPicks, emptyArray(), 0) }
        assertFailsWith<IllegalArgumentException> { GritDomain.planBuilderBuild(arrayOf(1, 3, 8), jsPicks, emptyArray(), 0) }
        assertFailsWith<IllegalArgumentException> { GritDomain.planBuilderBuild(arrayOf(1, 3, 5), jsPicks, arrayOf("Chest"), 0) }
        assertFailsWith<IllegalArgumentException> { GritDomain.planBuilderBuild(arrayOf(1, 3, 5), jsPicks, arrayOf("TRAPS"), 0) }
        assertEquals(3, GritDomain.planBuilderMaxPriorities())
        assertEquals(12, GritDomain.planBuilderTargetGroups().size)
        assertEquals("CHEST", GritDomain.planBuilderTargetGroups()[0])
    }

    @Test
    fun suggestedPicksCrossTheBoundary() {
        val exercises = arrayOf(
            JsExercise(1, "A", "ACCESSORY", null, emptyArray(), false, false),
            JsExercise(2, "B", "ACCESSORY", null, emptyArray(), false, false),
            JsExercise(3, "C", "ACCESSORY", null, emptyArray(), false, true),
        )
        val set = { id: Int -> JsSet(id, 20.0, 10, 8.0, false, null, false) }
        val history = JsHistory(
            arrayOf(
                JsSession(1, "2026-11-10", null, null, arrayOf(set(2), set(3)), "BODYBUILDING"),
                JsSession(2, "2026-10-15", null, null, arrayOf(set(1)), "POWERLIFTING"),
            ),
            exercises,
            emptyArray(),
        )
        val plans = arrayOf(JsPlanItem(1, 1, 1, 3, 5, 8.0, 0))
        assertContentEquals(arrayOf(1, 2), GritDomain.planBuilderSuggestedPicks(history, plans, "2026-11-13"))
    }
}
