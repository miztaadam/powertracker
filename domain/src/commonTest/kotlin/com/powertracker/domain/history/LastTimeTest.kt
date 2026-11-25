package com.powertracker.domain.history

import com.powertracker.domain.Ids
import com.powertracker.domain.history
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class LastTimeTest {
    private val start = LocalDate(2026, 1, 5)
    private fun day(n: Int) = start.plus(n, DateTimeUnit.DAY)

    @Test
    fun lastTime_neverDone_isNull() {
        val h = history { session(0) { squat(150.0, 5) } }
        assertNull(h.lastTime(Ids.CURL, POWERLIFTING, day(7)))
        assertEquals(emptyMap(), history { }.lastTimes(POWERLIFTING, day(7)))
    }

    @Test
    fun lastTime_isTheLatestSessionWithTheExercise_everySetInLoggedOrder() {
        val h = history {
            session(7) { set(Ids.CURL, 20.0, 12); squat(150.0, 5); set(Ids.CURL, 20.0, 11); set(Ids.CURL, 22.5, 9, rpe = null, failed = true) }
            session(0) { set(Ids.CURL, 18.0, 12) }
            // A later session without the exercise is passed over.
            session(9) { squat(155.0, 5) }
        }
        val last = h.lastTime(Ids.CURL, POWERLIFTING, day(14))!!
        assertEquals(day(7), last.date)
        assertEquals(listOf(20.0 to 12, 20.0 to 11, 22.5 to 9), last.sets.map { it.weightKg to it.reps })
        assertEquals(listOf(false, false, true), last.sets.map { it.failed })
        assertNull(last.sets.last().rpe)
    }

    @Test
    fun lastTime_isStrictlyBeforeTheDay() {
        val h = history {
            session(0) { set(Ids.CURL, 18.0, 12) }
            session(7) { set(Ids.CURL, 20.0, 12) }
            session(14) { set(Ids.CURL, 22.0, 12) }
        }
        // Day 7's own sets and anything later are not "last time" on day 7.
        assertEquals(day(0), h.lastTime(Ids.CURL, POWERLIFTING, day(7))!!.date)
        assertNull(h.lastTime(Ids.CURL, POWERLIFTING, day(0)))
    }

    @Test
    fun lastTime_readsItsOwnSideOnly_evenWhenTheOtherSideIsLater() {
        val h = history {
            session(0, mode = BODYBUILDING) { set(Ids.DB_PRESS, 24.0, 12) }
            session(7, mode = POWERLIFTING) { set(Ids.DB_PRESS, 30.0, 5) }
        }
        assertEquals(day(0), h.lastTime(Ids.DB_PRESS, BODYBUILDING, day(14))!!.date)
        assertEquals(day(7), h.lastTime(Ids.DB_PRESS, POWERLIFTING, day(14))!!.date)
        assertNull(h.lastTime(Ids.DB_PRESS, POWERLIFTING, day(7)))
    }

    @Test
    fun lastTime_keepsEachSetInTheUnitItWasLoggedIn() {
        val kg = LoggedSet(Ids.PULL_UP, 50.0, 12, 8.0, failed = false, missPoint = null, isTopSet = false)
        val plates = kg.copy(weightKg = 7.5, reps = 10, unit = LoadUnit.PLATES)
        val h = history { }.copy(sessions = listOf(LoggedSession(1, day(0), null, null, listOf(kg, plates), BODYBUILDING)))
        assertEquals(listOf(kg, plates), h.lastTime(Ids.PULL_UP, BODYBUILDING, day(7))!!.sets)
    }

    @Test
    fun lastTimes_agreesWithLastTime_forEveryExercise() {
        val h = history {
            session(0) { squat(140.0, 5); set(Ids.CURL, 18.0, 12) }
            session(0, mode = BODYBUILDING) { set(Ids.CURL, 16.0, 15) }
            // Two sessions on one day and side: the one later in the list is "last time".
            session(7) { squat(145.0, 5); set(Ids.PULL_UP, 0.0, 8) }
            session(7) { squat(150.0, 3); bench(100.0, 5) }
            session(3) { set(Ids.DB_PRESS, 24.0, 10); set(Ids.CURL, 20.0, 10) }
            session(14) { deadlift(200.0, 3) }
        }
        for (mode in listOf(POWERLIFTING, BODYBUILDING)) for (before in listOf(day(0), day(3), day(7), day(8), day(30))) {
            val ids = h.sessions.flatMap { s -> s.sets.map { it.exerciseId } }.toSet() + Ids.STRICT_PRESS
            val each = ids.mapNotNull { id -> h.lastTime(id, mode, before)?.let { id to it } }.toMap()
            assertEquals(each, h.lastTimes(mode, before), "$mode before $before")
        }
        assertEquals(listOf(150.0), h.lastTime(Ids.SQUAT, POWERLIFTING, day(8))!!.sets.map { it.weightKg })
    }
}
