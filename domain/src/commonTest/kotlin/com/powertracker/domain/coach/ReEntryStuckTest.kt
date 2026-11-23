package com.powertracker.domain.coach

import com.powertracker.domain.coach.PhysiqueFixtures.default3
import com.powertracker.domain.coach.PhysiqueFixtures.idOf
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Review, 1.16: an easing-back-in card that does not count on the bodybuilding side (spec point 10) does not hold that
 * side's stuck-exercise SWAP back either; one that counts still does, for its 21 days as before.
 */
class ReEntryStuckTest {
    private val start = LocalDate(2026, 1, 5)
    private val settings = CoachSettings()
    private fun day(d: Int) = start.plus(d, DateTimeUnit.DAY)

    private val legExtension = idOf("Leg extension")
    private val squat = idOf("Squat")

    private fun session(d: Int, mode: TrainingMode, id: Long, kg: Double, reps: Int) =
        LoggedSession(d.toLong() + 1, day(d), null, null, List(3) { LoggedSet(id, kg, reps, 8.0, false, null, it == 0) }, mode)

    private fun powerlifting(d: Int) = session(d, TrainingMode.POWERLIFTING, squat, 100.0, 5)
    private fun legExtension(d: Int) = session(d, TrainingMode.BODYBUILDING, legExtension, 40.0, 10)

    private fun history(sessions: List<LoggedSession>) = PhysiqueFixtures.history(sessions.sortedBy { it.date })

    /** Evaluates [mode] on [d] with the sessions up to that day, storing what comes back, as the apps do. */
    private fun evaluate(mode: TrainingMode, all: TrainingHistory, d: Int, prior: List<Advice>): List<Advice> {
        val h = all.copy(sessions = all.sessions.filter { it.date <= day(d) })
        return prior + Coaching.evaluate(mode, h, default3, settings, day(d), prior)
    }

    private fun List<Advice>.swaps() = filter { it.type == AdviceType.SWAP && (it.payload as AdvicePayload.Swap).exerciseId == legExtension }

    // Powerlifting on days 0, 7 and 14; the first bodybuilding sessions on days 30 to 51, the leg extension flat.
    private val bodybuildingDays = listOf(30, 37, 44, 51)
    private val base = listOf(0, 7, 14).map(::powerlifting) + bodybuildingDays.map(::legExtension)

    /** Every session evaluated on its own side, in date order, as the lifter logs them; the last day is not run. */
    private fun storedBefore(all: TrainingHistory, lastDay: Int): List<Advice> =
        all.sessions.filter { it.date < day(lastDay) }.fold(emptyList()) { prior, s -> evaluate(s.mode, all, start.daysUntil(s.date), prior) }

    @Test
    fun aCardThatDoesNotCountOnBodybuildingDoesNotHoldItsSwapBack() {
        // A powerlifting session on day 32 ends that side's break (day 14 to 30, measured over both sides) and is
        // eased: the card is made then, on the powerlifting side.
        val all = history(base + powerlifting(32))
        val stored = storedBefore(all, 51)
        val cards = stored.filter { it.type == AdviceType.RE_ENTRY }
        assertEquals(listOf(day(32)), cards.map { it.createdAt }, "the day-32 card is made")
        assertEquals(TrainingMode.POWERLIFTING, cards.single().mode)
        // Bodybuilding was not trained before the break, so the card does not count there ...
        assertEquals(false, Coaching.reEntryShows(TrainingMode.BODYBUILDING, day(32), all))
        // ... and on day 51 the leg extension, flat since day 30, gets its SWAP, as it does without that session.
        val withCard = evaluate(TrainingMode.BODYBUILDING, all, 51, stored).swaps()
        val withoutSession = history(base).let { h -> evaluate(TrainingMode.BODYBUILDING, h, 51, storedBefore(h, 51)).swaps() }
        assertEquals(1, withoutSession.size, "without the day-32 session")
        assertEquals(1, withCard.size, "with the day-32 card")
        assertEquals(withoutSession.single().payload, withCard.single().payload)
    }

    @Test
    fun aGenuineReturnersCardStillHoldsTheSwapBackForItsDays() {
        // The leg extension trained before the break (days 0, 7, 14) and after it (days 30 to 50), flat throughout.
        val all = history(listOf(0, 7, 14, 30, 37, 44, 50).map(::legExtension))
        val stored = storedBefore(all, 50)
        val card = stored.single { it.type == AdviceType.RE_ENTRY }
        assertEquals(day(30), card.createdAt)
        assertTrue(Coaching.reEntryShows(TrainingMode.BODYBUILDING, card.createdAt, all))
        // Day 50 is the card's 21st day: no SWAP while it holds ...
        assertEquals(emptyList(), evaluate(TrainingMode.BODYBUILDING, all, 50, stored).swaps())
        // ... and it is the card that holds it: the same history without it gives the SWAP.
        assertEquals(1, evaluate(TrainingMode.BODYBUILDING, all, 50, stored - card).swaps().size)
    }
}
