package com.powertracker.domain.web

import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.coach.PhysiqueFixtures
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.progress.MuscleProgress
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PhysiqueBridgeTest {
    private val today = LocalDate(2026, 11, 14)
    private val plan = PhysiqueFixtures.default3
    private val jsPlan = plan.map { it.toJs() }.toTypedArray()
    private val jsExercises = PhysiqueFixtures.exercises.values.map { it.toJs() }.toTypedArray()

    private fun session(daysAgo: Int) = LoggedSession(
        100L - daysAgo, today.minus(daysAgo, DateTimeUnit.DAY), null, null,
        List(3) { LoggedSet(PhysiqueFixtures.idOf("Leg extension"), 40.0, 10, 8.0, false, null, false) }, TrainingMode.BODYBUILDING,
    )

    private val history = PhysiqueFixtures.history(listOf(session(28), session(14), session(7)))
    private val jsHistory = JsHistory(history.sessions.map { s ->
        JsSession(s.id.toInt(), s.date.toString(), null, null, s.sets.map { it.toJs() }.toTypedArray(), s.mode.name)
    }.toTypedArray(), jsExercises, emptyArray())

    private val checkIn = JsCheckIn(today.toString(), arrayOf("CHEST", "TRAPS"), arrayOf("QUADS"))
    private val domainCheckIn = CheckIn(today, setOf(MuscleGroup.CHEST, MuscleGroup.TRAPS), setOf(MuscleGroup.QUADS))

    @Test
    fun aCheckInCrossesTheBoundaryAsTheDomainMakesIt() {
        val out = GritDomain.physiqueAfterCheckIn(checkIn, jsHistory, jsPlan, today.toString(), emptyArray())
        val domain = Coaching.afterCheckIn(domainCheckIn, history, plan, today)
        assertEquals(domain.advice, out.advice.map { it.toDomain() })
        assertEquals(listOf("MORE_WORK", "MORE_WORK"), out.advice.map { it.type })
        assertEquals(listOf("BODYBUILDING", "BODYBUILDING"), out.advice.map { it.mode })
        assertEquals(0, out.notes.size)
        // The stored payload decodes for the screens.
        // Chest is a target group, so it comes before traps.
        val traps = GritDomain.advicePayload(out.advice[1].payloadJson)!!
        assertEquals("MoreWork", traps.kind)
        assertEquals("TRAPS", traps.group)
        assertEquals(3, traps.newExercise!!.weekday)
        assertEquals("Barbell shrug", traps.newExercise!!.name)
        assertEquals(0, traps.weeklyBefore)
        val chest = GritDomain.advicePayload(out.advice[0].payloadJson)!!
        assertEquals("CHEST", chest.group)
        assertEquals(listOf(1, 3), chest.addSets.map { it.weekday })
        // Saving again dismisses the open ones.
        val again = GritDomain.physiqueAfterCheckIn(checkIn, jsHistory, jsPlan, today.toString(), out.advice)
        assertEquals(listOf("DISMISSED", "DISMISSED"), again.dismissed.map { it.status })
        assertFailsWith<IllegalArgumentException> { GritDomain.physiqueAfterCheckIn(JsCheckIn("2026-11-14", arrayOf("CHEST"), arrayOf("CHEST")), jsHistory, jsPlan, "2026-11-14", emptyArray()) }
        assertFailsWith<IllegalArgumentException> { GritDomain.physiqueAfterCheckIn(JsCheckIn("2026-11-14", arrayOf("CHEEST"), emptyArray()), jsHistory, jsPlan, "2026-11-14", emptyArray()) }
    }

    @Test
    fun applyingCrossesTheBoundary() {
        val out = GritDomain.physiqueAfterCheckIn(checkIn, jsHistory, jsPlan, today.toString(), emptyArray())
        val traps = GritDomain.physiqueApplyMoreWork(out.advice[1].payloadJson, jsPlan, jsExercises)
        assertEquals("Added Barbell shrug to Wednesday", traps.message)
        assertTrue(traps.changed)
        assertEquals("Barbell shrug", traps.added.single().name)
        assertEquals(3, traps.added.single().item.weekday)
        assertEquals("BODYBUILDING", traps.added.single().item.mode)
        assertEquals(12, traps.added.single().item.repsHigh)
        val chest = GritDomain.physiqueApplyMoreWork(out.advice[0].payloadJson, jsPlan, jsExercises)
        assertEquals("Added a set to 2 exercises", chest.message)
        assertContentEquals(intArrayOf(4, 4), chest.updated.map { it.sets }.toIntArray())
        // A swap payload is not more work.
        val swap = GritDomain.physiqueStuck(jsHistory, jsPlan, emptyArray(), emptyArray(), today.toString()).single()
        assertFailsWith<IllegalArgumentException> { GritDomain.physiqueApplyMoreWork(swap.payloadJson, jsPlan, jsExercises) }
        val swapped = GritDomain.physiqueApplySwap(swap.payloadJson, "Split squat", jsPlan, jsExercises)
        assertEquals("Swapped Leg extension for Split squat", swapped.message)
        assertEquals(1, swapped.removed.size)
        assertEquals(swapped.removed.single().weekday, swapped.added.single().item.weekday)
        val decoded = GritDomain.advicePayload(swap.payloadJson)!!
        assertEquals("Swap", decoded.kind)
        assertEquals("Leg extension", decoded.name)
        assertEquals(today.minus(28, DateTimeUnit.DAY).toString(), decoded.since)
        assertEquals(listOf("Bulgarian split squat", "Split squat", "Lunge"), decoded.alternatives.toList())
    }

    @Test
    fun evaluateDueRowsAndWeeklySetsCrossTheBoundary() {
        val settings = JsCoachSettings(8.0, false)
        val withCheckIns = GritDomain.coachingEvaluateWithCheckIns("BODYBUILDING", jsHistory, jsPlan, settings, today.toString(), emptyArray(), emptyArray())
        assertEquals(
            Coaching.evaluate(TrainingMode.BODYBUILDING, history, plan, CoachSettings(8.0, false), today, emptyList()),
            withCheckIns.map { it.toDomain() },
        )
        assertEquals(listOf("SWAP"), withCheckIns.map { it.type })
        assertEquals(0, GritDomain.coachingEvaluateWithCheckIns("BODYBUILDING", jsHistory, jsPlan, settings, today.toString(), emptyArray(), arrayOf(checkIn)).size)
        assertTrue(GritDomain.coachingCheckInDue(jsHistory, jsPlan, emptyArray(), today.toString()))
        assertEquals(false, GritDomain.coachingCheckInDue(jsHistory, jsPlan, arrayOf(checkIn), today.toString()))
        val rows = GritDomain.muscleProgressRows(jsHistory, jsPlan, arrayOf(checkIn), today.toString())
        assertEquals(MuscleProgress.rows(history, plan, listOf(domainCheckIn), today).map { it.group.name }, rows.map { it.group })
        val chest = rows.first { it.group == "CHEST" }
        assertEquals("LAGGING", chest.ratings.single().rating)
        assertEquals(10, chest.setsPlanned)
        assertEquals(10, GritDomain.physiqueWeeklySets(jsPlan, jsExercises, "CHEST"))
        assertEquals(PhysiqueCoach.weeklySets(plan, PhysiqueFixtures.exercises, MuscleGroup.LATS), GritDomain.physiqueWeeklySets(jsPlan, jsExercises, "LATS"))
        // The latest session was 7 days ago, outside the 7 days ending today.
        assertEquals(0, rows.first { it.group == "QUADS" }.setsDone)
    }
}
