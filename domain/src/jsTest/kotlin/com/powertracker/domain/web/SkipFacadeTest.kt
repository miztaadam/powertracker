package com.powertracker.domain.web

import com.powertracker.domain.library.Library
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The skip coach through the web facade: the same rules as on Android, as plain data. */
class SkipFacadeTest {
    private val settings = JsCoachSettings(8.0, false)

    private fun exercise(id: Int, name: String) = Library.find(name)!!.let {
        JsExercise(id, it.name, it.category.name, it.parentLift?.name, it.muscles.toTypedArray(), true, false)
    }

    private val pulldown = exercise(1, "Lat pulldown")
    private val legPress = exercise(2, "Leg press")
    private val squat = exercise(3, "Squat")
    private val plan = arrayOf(
        JsPlanItem(10, 1, 1, 3, 8, 8.0, 0, "BODYBUILDING", 12),
        JsPlanItem(11, 1, 2, 3, 8, 8.0, 1, "BODYBUILDING", 12),
        JsPlanItem(12, 3, 1, 3, 8, 8.0, 0, "POWERLIFTING"),
        JsPlanItem(13, 3, 3, 3, 5, 8.0, 1, "POWERLIFTING"),
    )
    private val history = JsHistory(emptyArray(), arrayOf(pulldown, legPress, squat), emptyArray())

    @Test
    fun afterSkipGivesTheCardAsPlainData() {
        val first = JsSkip(1, "2026-11-10", "BODYBUILDING", 1, 10, "BUSY")
        val second = JsSkip(2, "2026-11-18", "BODYBUILDING", 1, 10, "BUSY")
        assertEquals(0, GritDomain.coachingAfterSkip(first, arrayOf(first), history, plan, "2026-11-10", emptyArray()).size)
        val card = GritDomain.coachingAfterSkip(second, arrayOf(first, second), history, plan, "2026-11-18", emptyArray()).single()
        assertEquals(listOf("SKIP", "BODYBUILDING", "2026-11-18", "OPEN"), listOf(card.type, card.mode, card.createdAt, card.status))
        assertNull(card.lift)
        val payload = GritDomain.advicePayload(card.payloadJson)!!
        assertEquals(listOf<Any?>("Skipped", 2, 1, "Lat pulldown", "BUSY"), listOf(payload.kind, payload.skipId, payload.exerciseId, payload.name, payload.skipReason))
        assertTrue(payload.alternatives.isNotEmpty())

        // The card stored and handed back blocks a second one.
        val stored = card.copy(id = 40)
        val third = JsSkip(3, "2026-11-18", "BODYBUILDING", 1, 10, "PAIN")
        assertEquals(0, GritDomain.coachingAfterSkip(third, arrayOf(first, third), history, plan, "2026-11-18", arrayOf(stored)).size)

        // A main lift's pain card: no alternatives, the lift set, no reason left out of the payload.
        val pain = JsSkip(4, "2026-11-18", "POWERLIFTING", 3, 13, "PAIN")
        val held = GritDomain.coachingAfterSkip(pain, arrayOf(pain), history, plan, "2026-11-18", emptyArray()).single()
        assertEquals("SQUAT", held.lift)
        assertEquals(0, GritDomain.advicePayload(held.payloadJson)!!.alternatives.size)

        // No reason at all.
        val none = JsSkip(5, "2026-11-18", "BODYBUILDING", 2, 11, null)
        assertEquals(0, GritDomain.coachingAfterSkip(none, arrayOf(none), history, plan, "2026-11-18", emptyArray()).size)
        assertFailsWith<IllegalArgumentException> {
            GritDomain.coachingAfterSkip(none.copy(reason = "busy"), arrayOf(none), history, plan, "2026-11-18", emptyArray())
        }
    }

    @Test
    fun applySkipSwapSwapsTheNamedSide() {
        val payload = """{"type":"com.powertracker.domain.model.AdvicePayload.Skipped","skipId":2,"exerciseId":1,"name":"Lat pulldown","reason":"BUSY","alternatives":["Machine pulldown"]}"""
        val bb = GritDomain.coachingApplySkipSwap(payload, "Machine pulldown", "BODYBUILDING", plan, arrayOf(pulldown, legPress, squat))
        assertTrue(bb.changed)
        assertContentEquals(arrayOf(10), bb.removed.map { it.id }.toTypedArray())
        val pl = GritDomain.coachingApplySkipSwap(payload, "Machine pulldown", "POWERLIFTING", plan, arrayOf(pulldown, legPress, squat))
        assertContentEquals(arrayOf(12), pl.removed.map { it.id }.toTypedArray())
        assertEquals("Machine pulldown", pl.added.single().name)
        assertEquals("Swapped Lat pulldown for Machine pulldown", pl.message)
    }

    @Test
    fun aPainSkipHoldsTargetsAndRepsToBeat() {
        // A press done 12, 12, 12 at 26 kg the Monday before: the weight would go up, and the reps start again at 8.
        val press = JsExercise(52, "DB press", "ACCESSORY", null, emptyArray(), true, false)
        val sets = Array(3) { JsSet(52, 26.0, 12, 8.0, false, null, false) }
        val h = JsHistory(arrayOf(JsSession(1, "2026-11-02", null, null, sets, "BODYBUILDING")), arrayOf(press), emptyArray())
        val item = JsPlanItem(1, 1, 52, 3, 8, 8.0, 0, "BODYBUILDING", 12)
        val free = GritDomain.coachingTargetsFor("BODYBUILDING", 1, h, arrayOf(item), settings, "2026-11-09", emptyArray()).single()
        assertEquals(26.5, GritDomain.advicePayload(free.payloadJson)!!.weightKg)
        // With no skips the new functions give the same as the old ones.
        assertContentEquals(
            GritDomain.coachingTargetsFor("BODYBUILDING", 1, h, arrayOf(item), settings, "2026-11-09", emptyArray()),
            GritDomain.coachingTargetsForWithSkips("BODYBUILDING", 1, h, arrayOf(item), settings, "2026-11-09", emptyArray(), emptyArray()),
        )
        assertEquals(GritDomain.repsToBeat(item, h, 1, "2026-11-09"), GritDomain.repsToBeatWithSkips(item, h, 1, "2026-11-09", emptyArray()))

        // Skipped for pain on the other side on the Friday: held.
        val skips = arrayOf(JsSkip(1, "2026-11-06", "POWERLIFTING", 52, null, "PAIN"))
        val held = GritDomain.coachingTargetsForWithSkips("BODYBUILDING", 1, h, arrayOf(item), settings, "2026-11-09", emptyArray(), skips).single()
        assertEquals(26.0, GritDomain.advicePayload(held.payloadJson)!!.weightKg)
        assertEquals(8, GritDomain.repsToBeat(item, h, 1, "2026-11-09"))
        assertEquals(12, GritDomain.repsToBeatWithSkips(item, h, 1, "2026-11-09", skips))
    }
}
