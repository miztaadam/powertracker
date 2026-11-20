package com.powertracker.domain.coach

import com.powertracker.domain.HistoryBuilder
import com.powertracker.domain.Ids
import com.powertracker.domain.history
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Spec point 10 (1.16): the break's easing back in is stored, and clamps targets, only on a side trained before the
 * session that ended the break. The break itself is still measured over both sides' sessions.
 */
class ReEntryGateTest {
    private val start = LocalDate(2026, 1, 5)
    private val settings = CoachSettings()
    private fun day(d: Int) = start.plus(d, DateTimeUnit.DAY)

    private val bbBench = PlanItem(1, DayOfWeek.MONDAY, Ids.BENCH, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)
    private val plBench = PlanItem(2, DayOfWeek.MONDAY, Ids.BENCH, 3, 5, 8.0, 0)
    private val plCurl = PlanItem(3, DayOfWeek.MONDAY, Ids.CURL, 3, 10, 8.0, 1)
    private val plan = listOf(bbBench, plBench, plCurl)

    private fun HistoryBuilder.plBench(d: Int) = session(d) { bench(100.0, 5) }
    private fun HistoryBuilder.bbBench(d: Int, reps: Int = 10) =
        session(d, mode = TrainingMode.BODYBUILDING) { repeat(3) { set(Ids.BENCH, 70.0, reps) } }
    private fun HistoryBuilder.bbPress(d: Int) =
        session(d, mode = TrainingMode.BODYBUILDING) { repeat(3) { set(Ids.DB_PRESS, 26.0, 10) } }
    private fun HistoryBuilder.plDay(d: Int) = session(d) {
        bench(100.0, 5)
        repeat(3) { set(Ids.CURL, 20.0, 10, isTopSet = it == 0) }
    }

    /** Evaluates [mode] on each day in turn, storing what comes back, as the apps do; returns everything stored. */
    private fun evaluateOn(mode: TrainingMode, history: TrainingHistory, days: List<Int>, stored: List<Advice> = emptyList()): List<Advice> =
        days.fold(stored) { prior, d ->
            val today = day(d)
            val h = history.copy(sessions = history.sessions.filter { it.date <= today })
            prior + Coaching.evaluate(mode, h, plan, settings, today, prior)
        }

    private fun targets(mode: TrainingMode, history: TrainingHistory, d: Int, prior: List<Advice>): List<Advice> {
        val today = day(d)
        val h = history.copy(sessions = history.sessions.filter { it.date < today })
        return Coaching.targetsFor(mode, DayOfWeek.MONDAY, h, plan, settings, today, prior)
    }

    private fun Advice.target() = payload as AdvicePayload.Target
    private fun List<Advice>.reEntries() = filter { it.type == AdviceType.RE_ENTRY }

    private fun legacyCards(createdOn: Int) = TrainingMode.entries.map {
        Advice(
            AdviceType.RE_ENTRY, null, day(createdOn),
            "Welcome back — it's been 16 days since your previous session. Take a lighter week: about 85% of your usual loads.",
            16, AdvicePayload.ReEntry(), mode = it,
        )
    }

    // ---- 1. a first bodybuilding session after a break on the powerlifting side ----

    private val firstBodybuilding = history(start) {
        for (d in listOf(0, 7, 14)) plBench(d)
        bbBench(30)
        bbBench(33, reps = 11)
    }

    @Test
    fun aFirstBodybuildingSessionAfterABreakStoresNoEasingBackIn() {
        val stored = evaluateOn(TrainingMode.BODYBUILDING, firstBodybuilding, listOf(30, 31))
        assertEquals(emptyList(), stored.reEntries(), "day 30 and day 31")

        val next = targets(TrainingMode.BODYBUILDING, firstBodybuilding, 31, stored).single()
        assertEquals(70.0, next.target().weightKg)
        assertTrue(!next.reason.contains("Easing back in"), next.reason)

        val afterSecond = evaluateOn(TrainingMode.BODYBUILDING, firstBodybuilding, listOf(33), stored)
        assertEquals(emptyList(), afterSecond.reEntries(), "after the second session")
    }

    @Test
    fun aCardOnTheTrainedSideDoesNotEaseTheSideFirstTrainedOnTheReturn() {
        // Back on day 30 on the bodybuilding side (its first session), then powerlifting on day 32: the powerlifting
        // side was trained before the return, so it gets the card; the bodybuilding side, which is handed that card too,
        // is not eased by it.
        val h = history(start) {
            for (d in listOf(0, 7, 14)) plBench(d)
            bbBench(30)
            plBench(32)
        }
        val stored = evaluateOn(TrainingMode.BODYBUILDING, h, listOf(30)).let { evaluateOn(TrainingMode.POWERLIFTING, h, listOf(32), it) }
        val card = stored.reEntries().single()
        assertEquals(TrainingMode.POWERLIFTING, card.mode)
        assertEquals(16, card.severity)

        val bb = targets(TrainingMode.BODYBUILDING, h, 33, stored).single()
        assertEquals(70.0, bb.target().weightKg)
        assertTrue(!bb.reason.contains("Easing back in"), bb.reason)
        val pl = targets(TrainingMode.POWERLIFTING, h, 33, stored).single { it.target().exerciseId == Ids.BENCH }
        assertTrue(pl.reason.endsWith(Coach.RE_ENTRY_NOTE), pl.reason)
        assertTrue(pl.target().weightKg < targets(TrainingMode.POWERLIFTING, h, 33, emptyList()).single { it.target().exerciseId == Ids.BENCH }.target().weightKg)
    }

    // ---- 2. the mirror: a first powerlifting session after a break on the bodybuilding side ----

    private val firstPowerlifting = history(start) {
        baseline(Ids.BENCH, 120.0)
        for (d in listOf(0, 7, 14)) bbPress(d)
        plDay(30)
        plDay(33)
    }

    /** The same powerlifting log with no bodybuilding sessions: no break at all. */
    private val noBreak = firstPowerlifting.copy(sessions = firstPowerlifting.sessions.filter { it.mode == TrainingMode.POWERLIFTING })

    @Test
    fun aFirstPowerliftingSessionAfterABreakStoresNoEasingBackIn() {
        val stored = evaluateOn(TrainingMode.POWERLIFTING, firstPowerlifting, listOf(30, 31))
        assertEquals(emptyList(), stored.reEntries(), "day 30 and day 31")

        val next = targets(TrainingMode.POWERLIFTING, firstPowerlifting, 31, stored)
        val unbroken = targets(TrainingMode.POWERLIFTING, noBreak, 31, emptyList())
        assertEquals(listOf(Ids.BENCH, Ids.CURL), next.map { it.target().exerciseId })
        assertEquals(unbroken, next)
        val curl = next.single { it.target().exerciseId == Ids.CURL }
        assertEquals(unbroken.single { it.target().exerciseId == Ids.CURL }.target().weightKg, curl.target().weightKg)
        assertTrue(curl.target().weightKg >= 20.0, curl.reason)
        assertTrue(next.none { it.reason.contains("Easing back in") }, "$next")

        val afterSecond = evaluateOn(TrainingMode.POWERLIFTING, firstPowerlifting, listOf(33), stored)
        assertEquals(emptyList(), afterSecond.reEntries(), "after the second session")
    }

    // ---- 3. a real returner keeps the card and the easing ----

    @Test
    fun aSideTrainedBeforeTheBreakIsWelcomedBackAndEased() {
        val sides = mapOf(
            TrainingMode.POWERLIFTING to history(start) { for (d in listOf(0, 7, 14, 30)) plDay(d) },
            TrainingMode.BODYBUILDING to history(start) { for (d in listOf(0, 7, 14, 30)) bbBench(d) },
        )
        for ((mode, h) in sides) {
            val stored = evaluateOn(mode, h, listOf(30, 31))
            val card = stored.reEntries().single()
            assertEquals(day(30), card.createdAt, "$mode")
            assertEquals(mode, card.mode)
            assertEquals(16, card.severity)
            assertTrue(card.reason.startsWith("Welcome back — it's been 16 days"), card.reason)

            val eased = targets(mode, h, 31, stored)
            val free = targets(mode, h, 31, emptyList())
            assertTrue(eased.isNotEmpty(), "$mode")
            for ((e, f) in eased.zip(free)) {
                assertTrue(e.reason.endsWith(Coach.RE_ENTRY_NOTE), "$mode: ${e.reason}")
                assertTrue(e.target().weightKg < f.target().weightKg, "$mode: ${e.reason} vs ${f.reason}")
            }
        }
    }

    // ---- 4. a card an older version stored on a side never trained eases nothing ----

    @Test
    fun aStoredCardOnASideNeverTrainedDoesNotClamp() {
        for (createdOn in listOf(30, 31)) {
            val prior = legacyCards(createdOn)
            for (d in listOf(31, 32)) {
                if (d < createdOn) continue
                for (card in prior) {
                    val bb = targets(TrainingMode.BODYBUILDING, firstBodybuilding, d, listOf(card)).single()
                    assertEquals(70.0, bb.target().weightKg, "made on ${card.mode} day $createdOn, day $d")
                    assertTrue(!bb.reason.contains("Easing back in"), bb.reason)

                    assertEquals(
                        targets(TrainingMode.POWERLIFTING, noBreak, d, emptyList()),
                        targets(TrainingMode.POWERLIFTING, firstPowerlifting, d, listOf(card)),
                        "made on ${card.mode} day $createdOn, day $d",
                    )
                }
            }
        }
    }
}
