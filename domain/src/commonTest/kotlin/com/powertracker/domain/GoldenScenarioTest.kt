package com.powertracker.domain

import com.powertracker.domain.coach.Coach
import com.powertracker.domain.model.AdvicePayload
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
