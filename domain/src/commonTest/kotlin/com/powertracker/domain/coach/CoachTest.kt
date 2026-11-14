package com.powertracker.domain.coach

import com.powertracker.domain.Ids
import com.powertracker.domain.history
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class CoachTest {
    private val start = LocalDate(2026, 1, 5) // Monday, day 0 of the history builder
    private fun day(n: Int): LocalDate = start.plus(n, DateTimeUnit.DAY)
    private val settings = CoachSettings()

    private fun item(exerciseId: Long, order: Int = 0, weekday: DayOfWeek = DayOfWeek.MONDAY, sets: Int = 5) =
        PlanItem(exerciseId * 100 + order, weekday, exerciseId, sets, 5, 8.0, order)

    private fun prior(
        type: AdviceType,
        lift: MainLift?,
        createdAt: LocalDate,
        status: AdviceStatus,
        severity: Int = 1,
    ): Advice {
        val payload = when (type) {
            AdviceType.TARGET -> AdvicePayload.Target(Ids.BENCH, 100.0, 5, 8.0)
            AdviceType.STALL -> AdvicePayload.Accessories(emptyList())
            AdviceType.DELOAD -> AdvicePayload.Deload()
            AdviceType.WEAK_POINT -> AdvicePayload.WeakPoint(lift!!, null)
            AdviceType.RE_ENTRY -> AdvicePayload.ReEntry()
            AdviceType.MORE_WORK, AdviceType.SWAP -> error("not a powerlifting advice type")
        }
        return Advice(type, lift, createdAt, "prior", severity, payload, status)
    }

    private fun evaluate(h: TrainingHistory, today: LocalDate, prior: List<Advice> = emptyList(), plan: List<PlanItem> = emptyList()) =
        Coach.evaluate(h, plan, settings, today, prior)

    private fun targets(h: TrainingHistory, plan: List<PlanItem>, today: LocalDate, prior: List<Advice> = emptyList()) =
        Coach.targetsFor(today.dayOfWeek, h, plan, settings, today, prior)

    private fun Advice.target() = payload as AdvicePayload.Target

    /** Flat bench 100 x 5 every Monday; [miss] is recorded from week 1 on. */
    private fun benchFlat(mondays: Int, miss: MissPoint? = null) =
        history { for (w in 0 until mondays) session(w * 7) { bench(100.0, 5, miss = if (w >= 1) miss else null) } }

    // --- safety rails: deload / re-entry clamps (R3) ---

    /** Low readiness + 2 failed reps over the last 14 days; last squat top set 100 x 5 @ 7 (would progress to 105). */
    private val fatigued = history {
        session(0, readiness = 2) { squat(100.0, 5, rpe = 7.0) }
        session(4, readiness = 2) { squat(100.0, 5, rpe = 7.0) }
        session(8, readiness = 2) {
            squat(100.0, 5, rpe = 7.0)
            set(Ids.SQUAT, 100.0, 5, failed = true)
            set(Ids.SQUAT, 100.0, 5, failed = true)
        }
    }

    @Test
    fun deloadSuppressesIncreases() {
        val today = day(10)
        val plan = listOf(item(Ids.SQUAT, weekday = today.dayOfWeek))
        val deload = evaluate(fatigued, today).single { it.type == AdviceType.DELOAD }

        val free = targets(fatigued, plan, today).single()
        assertEquals(105.0, free.target().weightKg) // without the deload it would progress

        val clamped = targets(fatigued, plan, today, listOf(deload)).single()
        assertTrue(clamped.target().weightKg <= 100.0)
        assertEquals(90.0, clamped.target().weightKg)
        assertEquals(3, clamped.target().sets)
        assertEquals("Last time: 100 × 5 @ 7. Coach suggests 90 × 5 @ 8. Deload week: lighter load and fewer sets.", clamped.reason)
    }

    @Test
    fun deloadClamp_activeWhenOpenOrAppliedWithinSevenDays() {
        val today = day(10)
        val plan = listOf(item(Ids.SQUAT, weekday = today.dayOfWeek))
        fun weight(vararg prior: Advice) = targets(fatigued, plan, today, prior.toList()).single().target().weightKg

        assertEquals(90.0, weight(prior(AdviceType.DELOAD, null, today.minus(13, DateTimeUnit.DAY), AdviceStatus.OPEN)))
        assertEquals(90.0, weight(prior(AdviceType.DELOAD, null, today.minus(6, DateTimeUnit.DAY), AdviceStatus.APPLIED)))
        assertEquals(105.0, weight(prior(AdviceType.DELOAD, null, today.minus(7, DateTimeUnit.DAY), AdviceStatus.APPLIED)))
        assertEquals(105.0, weight(prior(AdviceType.DELOAD, null, today, AdviceStatus.DISMISSED)))
        assertNull(targets(fatigued, plan, today).single().target().sets)
    }

    @Test
    fun deloadSets_roundedWithMinimumOne() {
        val today = day(10)
        val deload = listOf(prior(AdviceType.DELOAD, null, today, AdviceStatus.OPEN))
        fun sets(planned: Int) =
            targets(fatigued, listOf(item(Ids.SQUAT, weekday = today.dayOfWeek, sets = planned)), today, deload).single().target().sets
        assertEquals(1, sets(1))
        assertEquals(2, sets(4))
        assertEquals(3, sets(5))
    }

    @Test
    fun reEntryClamp_roundsDownToEightyFivePercent() {
        val h = history {
            for (d in listOf(0, 3, 7)) session(d) { squat(102.5, 5, rpe = 7.0) }
        }
        val today = day(9)
        val plan = listOf(item(Ids.SQUAT, weekday = today.dayOfWeek))
        // 102.5 x 0.85 = 87.125 rounds to 87.5, which is above the clamp, so one step lighter.
        val a = targets(h, plan, today, listOf(prior(AdviceType.RE_ENTRY, null, today.minus(2, DateTimeUnit.DAY), AdviceStatus.APPLIED))).single()
        assertEquals(85.0, a.target().weightKg)
        assertNull(a.target().sets)
        assertTrue(a.reason.endsWith("Coach suggests 85 × 5 @ 8. Easing back in: lighter load."), a.reason)
    }

    // --- minimum history ---

    /** Two main-lift bench sessions (peak), then three flat pause-bench weeks with lockout misses. */
    private fun twoBenchSessions(withBaseline: Boolean) = history {
        session(0) { bench(100.0, 5) }
        session(7) { bench(100.0, 5) }
        for (w in 2..4) session(w * 7) { set(Ids.PAUSE_BENCH, 100.0, 5, miss = MissPoint.LOCKOUT, isTopSet = true) }
        if (withBaseline) baseline(Ids.BENCH, 120.0)
    }

    @Test
    fun minimumHistory() {
        val today = day(28)
        val plan = listOf(item(Ids.BENCH, order = 0), item(Ids.PAUSE_BENCH, order = 1))

        val without = twoBenchSessions(withBaseline = false)
        assertTrue(evaluate(without, today, plan = plan).none { it.lift == MainLift.BENCH })
        assertTrue(targets(without, plan, today).isEmpty())

        val with = twoBenchSessions(withBaseline = true)
        val advice = evaluate(with, today, plan = plan).filter { it.lift == MainLift.BENCH }
        assertEquals(listOf(AdviceType.STALL), advice.map { it.type }) // WEAK_POINT loses to STALL
        assertEquals(listOf(Ids.BENCH, Ids.PAUSE_BENCH), targets(with, plan, today).map { it.target().exerciseId })
    }

    // --- one new card per lift per week ---

    @Test
    fun onePerLiftPerWeek() {
        val h = benchFlat(5)
        val today = day(28)
        fun bench(vararg prior: Advice) = evaluate(h, today, prior.toList()).filter { it.lift == MainLift.BENCH }

        assertEquals(listOf(AdviceType.STALL), bench().map { it.type })

        for (status in AdviceStatus.entries) {
            assertTrue(bench(prior(AdviceType.STALL, MainLift.BENCH, today.minus(3, DateTimeUnit.DAY), status)).isEmpty(), "$status 3 days ago")
        }
        assertTrue(bench(prior(AdviceType.WEAK_POINT, MainLift.BENCH, today.minus(6, DateTimeUnit.DAY), AdviceStatus.APPLIED)).isEmpty())

        // The weekly cap ends after 7 days (an APPLIED stall has its own 28-day cooldown, tested below).
        for (status in listOf(AdviceStatus.APPLIED, AdviceStatus.DISMISSED)) {
            assertEquals(1, bench(prior(AdviceType.WEAK_POINT, MainLift.BENCH, today.minus(8, DateTimeUnit.DAY), status)).size, "$status 8 days ago")
        }
        assertEquals(1, bench(prior(AdviceType.WEAK_POINT, MainLift.BENCH, today.minus(7, DateTimeUnit.DAY), AdviceStatus.APPLIED)).size)
        assertEquals(1, bench(prior(AdviceType.STALL, MainLift.BENCH, today.minus(8, DateTimeUnit.DAY), AdviceStatus.DISMISSED)).size)
        // An OPEN card blocks a duplicate whatever its age.
        assertTrue(bench(prior(AdviceType.STALL, MainLift.BENCH, today.minus(8, DateTimeUnit.DAY), AdviceStatus.OPEN)).isEmpty())
        // Other lifts are unaffected.
        assertEquals(1, bench(prior(AdviceType.STALL, MainLift.SQUAT, today.minus(1, DateTimeUnit.DAY), AdviceStatus.APPLIED)).size)

        // TARGET advice is exempt: it neither counts towards nor is blocked by the weekly cap.
        assertEquals(1, bench(prior(AdviceType.TARGET, MainLift.BENCH, today.minus(1, DateTimeUnit.DAY), AdviceStatus.APPLIED)).size)
        val recentStall = listOf(prior(AdviceType.STALL, MainLift.BENCH, today.minus(3, DateTimeUnit.DAY), AdviceStatus.APPLIED))
        assertEquals(1, targets(h, listOf(item(Ids.BENCH)), today, recentStall).size)
    }

    @Test
    fun onePerLiftPerEvaluate_prefersStallOverWeakPoint() {
        val result = evaluate(benchFlat(5, MissPoint.LOCKOUT), day(28))
        assertEquals(listOf(AdviceType.STALL), result.filter { it.lift == MainLift.BENCH }.map { it.type })
        // Without the stall (dismissed at a higher severity) the weak point is shown instead.
        val dismissed = prior(AdviceType.STALL, MainLift.BENCH, day(14), AdviceStatus.DISMISSED, severity = 9)
        val fallback = evaluate(benchFlat(5, MissPoint.LOCKOUT), day(28), listOf(dismissed))
        assertEquals(listOf(AdviceType.WEAK_POINT), fallback.filter { it.lift == MainLift.BENCH }.map { it.type })
    }

    // --- dismissed / open memory ---

    @Test
    fun dismissedNotRepeatedUnlessWorse() {
        val dismissed = listOf(prior(AdviceType.STALL, MainLift.BENCH, day(14), AdviceStatus.DISMISSED, severity = 4))

        assertTrue(evaluate(benchFlat(5), day(28), dismissed).none { it.lift == MainLift.BENCH }) // severity 4
        val worse = evaluate(benchFlat(6), day(35), dismissed).single { it.lift == MainLift.BENCH }
        assertEquals(AdviceType.STALL, worse.type)
        assertEquals(5, worse.severity)
    }

    @Test
    fun dismissed_comparesWithMostRecentDismissal() {
        val prior = listOf(
            prior(AdviceType.STALL, MainLift.BENCH, day(7), AdviceStatus.DISMISSED, severity = 9),
            prior(AdviceType.STALL, MainLift.BENCH, day(14), AdviceStatus.DISMISSED, severity = 3),
        )
        assertEquals(1, evaluate(benchFlat(5), day(28), prior).count { it.lift == MainLift.BENCH })
    }

    @Test
    fun openNotDuplicated() {
        for (severity in listOf(1, 4, 99)) {
            val open = listOf(prior(AdviceType.STALL, MainLift.BENCH, day(0), AdviceStatus.OPEN, severity))
            assertTrue(evaluate(benchFlat(5), day(28), open).none { it.lift == MainLift.BENCH }, "severity $severity")
        }
    }

    @Test
    fun openDeloadNotDuplicated() {
        val today = day(10)
        assertEquals(1, evaluate(fatigued, today).count { it.type == AdviceType.DELOAD })
        val open = listOf(prior(AdviceType.DELOAD, null, day(0), AdviceStatus.OPEN, severity = 1))
        assertTrue(evaluate(fatigued, today, open).none { it.type == AdviceType.DELOAD })
    }

    // --- gaps ---

    @Test
    fun gapReturnsReEntry() {
        val h = history {
            session(0) { squat(100.0, 5); bench(80.0, 5); deadlift(140.0, 5) }
            session(20) { squat(100.0, 5); bench(80.0, 5); deadlift(140.0, 5) }
        }
        val reEntry = evaluate(h, day(21)).single { it.type == AdviceType.RE_ENTRY }
        assertNull(reEntry.lift)
        assertEquals(day(21), reEntry.createdAt)
        assertEquals(20, reEntry.severity)
        assertEquals(AdvicePayload.ReEntry(0.85), reEntry.payload)
        assertEquals(
            "Welcome back — it's been 20 days since your previous session. Take a lighter week: about 85% of your usual loads.",
            reEntry.reason,
        )
        for (d in 21 until 34) assertTrue(evaluate(h, day(d)).none { it.type == AdviceType.STALL }, "day $d")
    }

    @Test
    fun gapPausesStallDetectionForTwoWeeks() {
        // Flat bench weeks 0-4, a 21-day gap, then flat again from day 49 (the return date).
        // Stall weeks count from the return week, so the first stall comes 3 weeks after it.
        val h = history { for (d in listOf(0, 7, 14, 21, 28, 49, 56, 63, 70)) session(d) { bench(100.0, 5) } }
        for (d in 49 until 70) assertTrue(evaluate(h, day(d)).none { it.type == AdviceType.STALL }, "day $d")
        assertEquals(1, evaluate(h, day(70)).count { it.type == AdviceType.STALL })
    }

    @Test
    fun reEntry_onlyOncePerReturnAndOnlyInFirstWeek() {
        val h = history {
            session(0) { squat(100.0, 5) }
            session(20) { squat(100.0, 5) }
        }
        assertEquals(1, evaluate(h, day(27)).count { it.type == AdviceType.RE_ENTRY })
        assertTrue(evaluate(h, day(28)).none { it.type == AdviceType.RE_ENTRY })
        val shown = listOf(prior(AdviceType.RE_ENTRY, null, day(21), AdviceStatus.APPLIED, severity = 1))
        assertTrue(evaluate(h, day(23), shown).none { it.type == AdviceType.RE_ENTRY })
        // A RE_ENTRY from an earlier gap does not count.
        val older = listOf(prior(AdviceType.RE_ENTRY, null, day(19), AdviceStatus.APPLIED, severity = 1))
        assertEquals(1, evaluate(h, day(23), older).count { it.type == AdviceType.RE_ENTRY })
    }

    // --- targetsFor plan handling ---

    @Test
    fun targetsFor_todaysItemsInOrder_skippingMissingAndArchived() {
        val base = history {
            for (d in listOf(0, 7, 14)) session(d) { squat(100.0, 5); bench(80.0, 5); deadlift(140.0, 5); set(Ids.CURL, 20.0, 10) }
        }
        val curl = base.exercises.getValue(Ids.CURL)
        val h = base.copy(exercises = base.exercises + (Ids.CURL to curl.copy(archived = true)))
        val plan = listOf(
            item(Ids.BENCH, order = 2),
            item(Ids.SQUAT, order = 1),
            item(Ids.CURL, order = 3),
            item(999L, order = 4),
            item(Ids.DEADLIFT, order = 0, weekday = DayOfWeek.TUESDAY),
        )
        val result = targets(h, plan, day(21))
        assertEquals(listOf(Ids.SQUAT, Ids.BENCH), result.map { it.target().exerciseId })
        assertTrue(result.all { it.type == AdviceType.TARGET && it.createdAt == day(21) })
    }

    // --- end to end ---

    @Test
    fun integration_benchStallWithLockout_endToEnd() {
        val closeGripId = 60L
        val base = history {
            for (w in 0 until 6) session(w * 7) {
                squat(150.0 + 2.5 * w, 5)
                deadlift(175.0 + 2.5 * w, 5)
                if (w < 2) bench(100.0, 5) else bench(100.0, 5, rpe = 9.5, miss = MissPoint.LOCKOUT)
            }
        }
        val closeGrip = Exercise(closeGripId, "Close-grip bench", Category.VARIATION, MainLift.BENCH, emptyList(), true)
        val h = base.copy(exercises = base.exercises + (closeGripId to closeGrip))
        val plan = listOf(
            item(Ids.SQUAT, order = 0),
            item(Ids.BENCH, order = 1),
            item(Ids.DEADLIFT, order = 2),
            item(closeGripId, order = 3, weekday = DayOfWeek.THURSDAY),
            item(Ids.CURL, order = 4, weekday = DayOfWeek.THURSDAY),
        )

        val result = evaluate(h, day(35), plan = plan)
        val stall = result.single()
        assertEquals(AdviceType.STALL, stall.type)
        assertEquals(MainLift.BENCH, stall.lift)
        assertEquals(AdviceStatus.OPEN, stall.status)
        assertTrue(stall.reason.contains("at lockout") && stall.reason.contains("triceps"), stall.reason)
        val names = (stall.payload as AdvicePayload.Accessories).items.map { it.exerciseName }
        assertEquals(listOf("JM press", "Board press"), names) // close-grip bench is already in the plan
    }
    // --- final review regressions ---

    /** Bench 100 x 5 on days 0, 3 and 7, then whatever [logged] adds (day to weight, 5 reps @ 8). */
    private fun benchThen(logged: List<Pair<Int, Double>>) = history {
        for (d in listOf(0, 3, 7)) session(d) { bench(100.0, 5) }
        for ((d, kg) in logged) session(d) { bench(kg, 5) }
    }

    /** Walks [days], taking today's bench target and logging it as done; returns the targets. */
    private fun followTargets(days: List<Int>, prior: List<Advice>): List<Double> {
        val logged = mutableListOf<Pair<Int, Double>>()
        for (d in days) {
            val today = day(d)
            val target = targets(benchThen(logged), listOf(item(Ids.BENCH, weekday = today.dayOfWeek)), today, prior).single()
            logged += d to target.target().weightKg
        }
        return logged.map { it.second }
    }

    @Test
    fun appliedDeload_twiceAWeek_everyTargetIsNinetyPercentOfThePreDeloadTopSet() {
        val deload = listOf(prior(AdviceType.DELOAD, null, day(8), AdviceStatus.APPLIED))
        assertEquals(listOf(90.0, 90.0, 90.0), followTargets(listOf(9, 12, 14), deload))
    }

    @Test
    fun appliedReEntry_everyTargetIsEightyFivePercentOfThePreGapTopSet() {
        val reEntry = listOf(prior(AdviceType.RE_ENTRY, null, day(28), AdviceStatus.APPLIED))
        assertEquals(listOf(85.0, 85.0, 85.0, 85.0), followTargets(listOf(28, 30, 32, 34), reEntry))
    }

    @Test
    fun reEntryCreatedAfterTheReturnDay_stillMeasuresFromThePreGapTopSet() {
        // Back on day 28 after three weeks off; the lifter trains at the eased 85 but skips Finish,
        // so the RE_ENTRY advice is only created at the next session, on day 30.
        val reEntry = listOf(prior(AdviceType.RE_ENTRY, null, day(30), AdviceStatus.OPEN))
        val h = benchThen(listOf(28 to 85.0, 30 to 85.0))
        val target = targets(h, listOf(item(Ids.BENCH, weekday = day(32).dayOfWeek)), day(32), reEntry).single()
        // 85% of the pre-gap 100, not 85% of the already-eased 85 (about 72).
        assertEquals(85.0, target.target().weightKg)
    }

    @Test
    fun deloadClamp_usesPlannedRepsNotProgressionsExtraRep() {
        // Curl 20 x 10: the 5% cap blocks a load jump, so progression alone would ask for 11 reps.
        val h = history { for (d in listOf(0, 3, 7)) session(d) { set(Ids.CURL, 20.0, 10, isTopSet = true) } }
        val today = day(9)
        val plan = listOf(PlanItem(1, today.dayOfWeek, Ids.CURL, 3, 10, 8.0, 0))
        assertEquals(11, targets(h, plan, today).single().target().reps)
        val clamped = targets(h, plan, today, listOf(prior(AdviceType.DELOAD, null, day(8), AdviceStatus.OPEN))).single().target()
        assertEquals(10, clamped.reps)
        assertEquals(17.5, clamped.weightKg) // 90% of 20 = 18, rounded down without the bar floor
    }

    @Test
    fun deloadWeek_doesNotCreateASecondDeload() {
        val applied = listOf(prior(AdviceType.DELOAD, null, day(10), AdviceStatus.APPLIED, severity = 2))
        // The fatigue signals from days 0-8 are still inside the 14-day window during the deload week.
        for (d in 11..13) assertTrue(evaluate(fatigued, day(d), applied).none { it.type == AdviceType.DELOAD }, "day $d")

        // Fatigue again three weeks later: suppressed until 21 days after the applied deload, then allowed.
        val later = history {
            session(0, readiness = 2) { squat(100.0, 5, rpe = 7.0) }
            session(4, readiness = 2) { squat(100.0, 5, rpe = 7.0) }
            session(8, readiness = 2) { squat(100.0, 5, rpe = 7.0) }
            for (d in listOf(24, 26, 28)) session(d, readiness = 2) {
                squat(90.0, 5, rpe = 7.0)
                set(Ids.SQUAT, 90.0, 5, failed = true)
            }
        }
        assertTrue(evaluate(later, day(30), applied).none { it.type == AdviceType.DELOAD })
        assertEquals(1, evaluate(later, day(31), applied).count { it.type == AdviceType.DELOAD })
    }

    @Test
    fun openDeloadOrReEntry_expiresAfterFourteenDays() {
        val today = day(10)
        val plan = listOf(item(Ids.SQUAT, weekday = today.dayOfWeek))
        fun weight(vararg prior: Advice) = targets(fatigued, plan, today, prior.toList()).single().target().weightKg
        assertEquals(90.0, weight(prior(AdviceType.DELOAD, null, today.minus(13, DateTimeUnit.DAY), AdviceStatus.OPEN)))
        assertEquals(105.0, weight(prior(AdviceType.DELOAD, null, today.minus(15, DateTimeUnit.DAY), AdviceStatus.OPEN)))
        assertEquals(105.0, weight(prior(AdviceType.RE_ENTRY, null, today.minus(15, DateTimeUnit.DAY), AdviceStatus.OPEN)))
        // A stale OPEN deload no longer blocks a new one.
        val stale = listOf(prior(AdviceType.DELOAD, null, today.minus(15, DateTimeUnit.DAY), AdviceStatus.OPEN))
        assertEquals(1, evaluate(fatigued, today, stale).count { it.type == AdviceType.DELOAD })
    }

    @Test
    fun dismissedStall_onlySuppressesForTwentyEightDays() {
        val today = day(28) // benchFlat(5) stalls with severity 4
        val old = listOf(prior(AdviceType.STALL, MainLift.BENCH, today.minus(30, DateTimeUnit.DAY), AdviceStatus.DISMISSED, severity = 4))
        assertEquals(1, evaluate(benchFlat(5), today, old).count { it.type == AdviceType.STALL })
        val recent = listOf(prior(AdviceType.STALL, MainLift.BENCH, today.minus(27, DateTimeUnit.DAY), AdviceStatus.DISMISSED, severity = 4))
        assertTrue(evaluate(benchFlat(5), today, recent).none { it.type == AdviceType.STALL })
    }

    @Test
    fun returnDayAfterHoliday_targetIsAtMostEightyFivePercentOfThePreGapLoad() {
        val h = benchThen(emptyList())
        val today = day(28) // 21 days after the last session; no RE_ENTRY advice exists yet
        val target = targets(h, listOf(item(Ids.BENCH, weekday = today.dayOfWeek)), today).single()
        assertTrue(target.target().weightKg <= 85.0, "got ${target.target().weightKg}")
        assertEquals(85.0, target.target().weightKg)
        assertTrue(target.reason.endsWith("Easing back in: lighter load."), target.reason)
        // Two days later, with no advice and no gap, the ordinary target is back.
        assertEquals(102.5, targets(h, listOf(item(Ids.BENCH, weekday = day(9).dayOfWeek)), day(9)).single().target().weightKg)
    }

    @Test
    fun appliedStall_suppressesStallForThatLiftForTwentyEightDays() {
        val h = benchFlat(9)
        val today = day(56)
        fun stalls(appliedDaysAgo: Long) =
            evaluate(h, today, listOf(prior(AdviceType.STALL, MainLift.BENCH, today.minus(appliedDaysAgo, DateTimeUnit.DAY), AdviceStatus.APPLIED)))
                .count { it.type == AdviceType.STALL && it.lift == MainLift.BENCH }
        assertEquals(0, stalls(10))
        assertEquals(0, stalls(27))
        assertEquals(1, stalls(28))
        // Another lift's applied stall does not count.
        assertEquals(1, evaluate(h, today, listOf(prior(AdviceType.STALL, MainLift.SQUAT, today.minus(10, DateTimeUnit.DAY), AdviceStatus.APPLIED))).count { it.type == AdviceType.STALL })
    }

    @Test
    fun stallAfterHoliday_onlyAfterNRealWeeksSinceReturn() {
        val h = history { for (d in listOf(0, 7, 14, 35, 42, 49, 56)) session(d) { bench(100.0, 5) } }
        assertTrue(evaluate(h, day(49)).none { it.type == AdviceType.STALL })
        val stall = evaluate(h, day(56)).single { it.type == AdviceType.STALL }
        assertEquals(3, stall.severity)
        assertTrue(stall.reason.contains("3 weeks"), stall.reason)
    }
}
