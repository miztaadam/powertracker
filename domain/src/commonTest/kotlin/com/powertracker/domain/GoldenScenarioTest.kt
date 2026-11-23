package com.powertracker.domain

import com.powertracker.domain.coach.Coach
import com.powertracker.domain.model.AdvicePayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class GoldenScenarioTest {
    @Test
    fun goldenScenario_coachOutput() {
        for (cp in GoldenScenario.checkpoints) {
            val history = GoldenScenario.historyUntil(cp.today)
            val advice = Coach.evaluate(history, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList())
            assertEquals(cp.expectedAdvice, advice.map { ExpectedAdvice(it.type, it.lift, it.severity) }, "advice at ${cp.label}")

            val targets = Coach.targetsFor(cp.weekday, history, GoldenScenario.plan, GoldenScenario.settings, cp.today, cp.storedAdvice)
            val actual = targets.map { (it.payload as AdvicePayload.Target).let { t -> ExpectedTarget(t.exerciseId, t.weightKg, t.reps, t.sets) } }
            assertEquals(cp.expectedTargets, actual, "targets at ${cp.label}")
        }
    }

    @Test
    fun goldenScenario_stallPrescribesTricepsAccessories() {
        val cp = GoldenScenario.checkpoints.first()
        val stall = Coach.evaluate(GoldenScenario.historyUntil(cp.today), GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList()).single()
        val names = (stall.payload as AdvicePayload.Accessories).items.map { it.exerciseName }
        assertEquals(GoldenScenario.stallAccessories, names)
    }

    @Test
    fun goldenScenario_returnLoadsAreAtMost85PercentOfPreGap() {
        val lastTop = mapOf(Ids.SQUAT to 162.5, Ids.BENCH to 95.0, Ids.PAUSE_BENCH to 80.0, Ids.CURL to 20.0)
        for (t in GoldenScenario.checkpoints.last().expectedTargets) {
            assertEquals(true, t.weightKg <= lastTop.getValue(t.exerciseId) * 0.85, "exercise ${t.exerciseId}")
        }
    }

    /**
     * A lifter who never presses gets exactly what they got before strict press existed. Every coach and Progress
     * figure for the golden scenario (days 0-100) and a second mixed history (days 0-63), rendered by [CoachSnapshot],
     * matches what was captured at BASE (ff633da) before any strict press change: the full text on the JVM (from
     * jvmTest/resources/snapshots, naming the first line that differs) and its hash on every platform.
     */
    @Test
    fun noStrictPressHistory_coachOutputUnchanged() {
        assertSnapshot("golden", CoachSnapshot.golden(), "ae05c47f678286fc", 196645)
        assertSnapshot("mixed", CoachSnapshot.mixed(), "c871a8e131d334d1", 203975)
    }

    private fun assertSnapshot(name: String, actual: String, hash: String, length: Int) {
        snapshotBaseline(name)?.let { base ->
            val expectedLines = base.lines()
            val actualLines = actual.lines()
            val i = (0 until maxOf(expectedLines.size, actualLines.size)).firstOrNull { expectedLines.getOrNull(it) != actualLines.getOrNull(it) }
            if (i != null) {
                // The date header the differing line falls under, so the day is easy to find.
                val day = actualLines.take(i + 1).lastOrNull { it.startsWith("== ") }
                fail(
                    "$name output changed at line ${i + 1} ($day):\n" +
                        "  expected: ${expectedLines.getOrNull(i)}\n  actual:   ${actualLines.getOrNull(i)}",
                )
            }
        }
        assertEquals(hash to length, CoachSnapshot.hash(actual) to actual.length, "$name output changed")
    }
}
