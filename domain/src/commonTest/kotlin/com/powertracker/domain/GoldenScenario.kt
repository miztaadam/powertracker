package com.powertracker.domain

import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** What the coach is expected to say about one lift (or none) at a checkpoint. */
data class ExpectedAdvice(val type: AdviceType, val lift: MainLift?, val severity: Int)

/** The load the coach is expected to prescribe for one plan item. */
data class ExpectedTarget(val exerciseId: Long, val weightKg: Double, val reps: Int, val sets: Int?)

class Checkpoint(
    val label: String,
    val today: LocalDate,
    val weekday: DayOfWeek,
    /** Advice the app has already stored when it asks for today's targets (evaluate always starts from none). */
    val storedAdvice: List<Advice>,
    val expectedAdvice: List<ExpectedAdvice>,
    val expectedTargets: List<ExpectedTarget>,
)

/**
 * A shared 10-week training history plus the coach output pinned at three dates. Both the common test
 * (JVM and JS) and the JS facade test read it, so the facade is held to the same answers as the domain.
 *
 * Week 0 starts on Monday 2026-01-05; sessions are Monday (squat, bench, pause bench, curl) and Thursday
 * (deadlift). Squat and deadlift keep progressing; bench stalls from week 2 with failed lockout reps; the
 * last two weeks have low readiness; then a 21-day holiday ends with a light deadlift on day 87.
 */
object GoldenScenario {
    private val start = LocalDate(2026, 1, 5)
    fun day(n: Int): LocalDate = start.plus(n, DateTimeUnit.DAY)

    val settings = CoachSettings()

    val plan: List<PlanItem> = listOf(
        PlanItem(1, DayOfWeek.MONDAY, Ids.SQUAT, 5, 5, 8.0, 0),
        PlanItem(2, DayOfWeek.MONDAY, Ids.BENCH, 5, 5, 8.0, 1),
        PlanItem(3, DayOfWeek.MONDAY, Ids.PAUSE_BENCH, 3, 5, 8.0, 2),
        PlanItem(4, DayOfWeek.MONDAY, Ids.CURL, 3, 10, 8.0, 3),
        PlanItem(5, DayOfWeek.THURSDAY, Ids.DEADLIFT, 3, 5, 8.0, 0),
    )

    val history: TrainingHistory = history(start) {
        for (w in 0..9) {
            val tired = if (w >= 8) 1 else null
            session(w * 7, readiness = tired) {
                squat(140.0 + 2.5 * w, 5)
                val bench = if (w < 2) 90.0 + 2.5 * w else 95.0
                bench(bench, 5)
                if (w >= 2) {
                    repeat(2) { set(Ids.BENCH, 100.0, 2, rpe = 10.0, failed = true, miss = MissPoint.LOCKOUT) }
                }
                set(Ids.PAUSE_BENCH, 80.0, 5, isTopSet = true)
                set(Ids.CURL, 20.0, 10, isTopSet = true)
            }
            session(w * 7 + 3, readiness = tired) { deadlift(165.0 + 2.5 * w, 5) }
        }
        session(87) { deadlift(120.0, 5) }
    }

    fun historyUntil(date: LocalDate): TrainingHistory =
        history.copy(sessions = history.sessions.filter { it.date <= date })

    private val reEntry = Advice(AdviceType.RE_ENTRY, null, day(91), "Welcome back", 21, AdvicePayload.ReEntry())

    private val deload = Advice(AdviceType.DELOAD, null, day(70), "Signs of fatigue", 2, AdvicePayload.Deload())

    private fun stall(severity: Int) = ExpectedAdvice(AdviceType.STALL, MainLift.BENCH, severity)

    /**
     * Pinned from the real coach and checked by hand against the rules:
     * - day 35: bench peaked in week 2 (95 vs 92.5 is over the 1% margin) and has been flat for 3 weeks with
     *   lockout misses, so STALL (severity 3 = weeks since the peak) with triceps accessories; squat and
     *   deadlift still climb, so nothing else.
     * - day 70: the last 14 days hold 2+ failed reps and readiness 1, two fatigue signals, so DELOAD;
     *   bench is still flat (STALL, 8 weeks). The stored OPEN deload caps targets at 90% of the pre-advice top
     *   set rounded down to the plate grid (162.5 -> 145, 95 -> 85, 80 -> 70, curl 20 -> 17.5) and sets at
     *   60% (5 -> 3, 3 -> 2).
     * - day 91: 21 days since the previous session (day 66 -> 87), so RE_ENTRY (severity = gap days) and the
     *   bench stall is paused. Targets are at most 85% of the pre-gap top sets, rounded down
     *   (162.5 -> 137.5, 95 -> 80, 80 -> 67.5, curl 20 -> 15 on its own 2.5 kg grid).
     */
    val checkpoints: List<Checkpoint> = listOf(
        Checkpoint(
            "stall", day(35), DayOfWeek.MONDAY, emptyList(),
            listOf(stall(3)),
            listOf(
                ExpectedTarget(Ids.SQUAT, 152.5, 5, null),
                ExpectedTarget(Ids.BENCH, 97.5, 5, null),
                ExpectedTarget(Ids.PAUSE_BENCH, 82.5, 5, null),
                ExpectedTarget(Ids.CURL, 20.0, 11, null),
            ),
        ),
        Checkpoint(
            "fatigue", day(70), DayOfWeek.MONDAY, listOf(deload),
            listOf(ExpectedAdvice(AdviceType.DELOAD, null, 2), stall(8)),
            listOf(
                ExpectedTarget(Ids.SQUAT, 145.0, 5, 3),
                ExpectedTarget(Ids.BENCH, 85.0, 5, 3),
                ExpectedTarget(Ids.PAUSE_BENCH, 70.0, 5, 2),
                ExpectedTarget(Ids.CURL, 17.5, 10, 2),
            ),
        ),
        Checkpoint(
            "return", day(91), DayOfWeek.MONDAY, listOf(reEntry),
            listOf(ExpectedAdvice(AdviceType.RE_ENTRY, null, 21)),
            listOf(
                ExpectedTarget(Ids.SQUAT, 137.5, 5, null),
                ExpectedTarget(Ids.BENCH, 80.0, 5, null),
                ExpectedTarget(Ids.PAUSE_BENCH, 67.5, 5, null),
                ExpectedTarget(Ids.CURL, 15.0, 10, null),
            ),
        ),
    )

    /** Stall accessories at day 35: lockout misses on bench, none of them already in the plan. */
    val stallAccessories = listOf("Close-grip bench", "JM press", "Board press")
}
