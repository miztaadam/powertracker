package com.powertracker.domain.guide

import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.PlanTemplate
import com.powertracker.domain.plan.PlanTemplates
import com.powertracker.domain.progress.ProgressMath
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * One session of the sample lift: what was done on [date] (every planned set at [doneWeightKg] × [doneReps] @
 * [doneRpe]) and the coach's next target for it. [exerciseId] is the exercise's catalogue name (the key of
 * [GuideSample.exercises]'s names); [nextRepsHigh] equals [nextRepsLow] for fixed reps; [note] is the coach's reason.
 */
data class GuideTargetStep(
    val date: LocalDate,
    val exerciseId: String,
    val doneWeightKg: Double,
    val doneReps: Int,
    val doneRpe: Double,
    val nextWeightKg: Double,
    val nextRepsLow: Int,
    val nextRepsHigh: Int,
    val note: String?,
)

/**
 * The sample lifter months on: [points] for the chart, oldest first (powerlifting: the sample lift's weekly e1RM;
 * bodybuilding: its heaviest set each session); [bests] by exercise (catalogue name) to the best, kg (powerlifting:
 * the best weekly e1RM; bodybuilding: the heaviest set); [totalKg] powerlifting only; [laggingMuscle] bodybuilding
 * only, the `MuscleGroup` name a check-in marks as lagging.
 */
data class GuideProgress(
    val points: List<Pair<LocalDate, Double>>,
    val bests: List<Pair<String, Double>>,
    val totalKg: Double?,
    val laggingMuscle: String?,
)

/**
 * The guide's sample lifter. Every number but the starting weights and the sample's own results (reps, RPE,
 * readiness, the lagging muscle) is worked out by the real coach, through [Coaching] as the apps call it. Nothing
 * is computed until it is first asked for.
 */
object GuideSample {
    /** A fixed Monday, so every run draws the same. */
    val TODAY: LocalDate = LocalDate(2026, 3, 2)

    private val SETTINGS = CoachSettings()

    /**
     * How many times the worked-out sample (plan, targets, cards, progress) has been read, for tests: opening the
     * guide and counting a picture's steps must not read it.
     */
    internal var reads: Int = 0
        private set

    /** The days a 3-day template is put on: the templates' own spread. */
    private const val DAYS = 3

    // ---- the sample's literals: starting weights and results ----

    /** The lift the targets follow, and its starting weight. */
    private const val PL_LIFT = "Squat"
    private const val PL_START_KG = 100.0
    private const val BB_LIFT = "Leg press"
    private const val BB_START_KG = 100.0

    /** Screen 5's sessions: powerlifting reps are the plan's, at these RPEs; bodybuilding's every set at these reps. */
    private val PL_TARGET_RPES = listOf(8.0, 8.0, 8.5, 8.0, 7.0)
    private val BB_TARGET_REPS = listOf(10, 11, 12, 9, 10)
    private const val BB_RPE = 8.0

    /** Six months of the main lifts (powerlifting) or the first day's first three exercises (bodybuilding). */
    private const val PROGRESS_WEEKS = 26
    private val PL_PROGRESS_START = mapOf("Squat" to 100.0, "Bench" to 60.0, "Deadlift" to 120.0)
    private val BB_PROGRESS_START = mapOf("Leg press" to 100.0, "DB press" to 26.0, "Lat pulldown" to 50.0)

    /** Powerlifting RPEs, by an exercise's session count: one easy session, then three hard ones at the same weight. */
    private val PL_PROGRESS_RPES = listOf(8.5, 9.0, 9.0, 9.0)

    /** Bodybuilding reps per set, by an exercise's session count since its last increase. */
    private val BB_PROGRESS_REPS = listOf(9, 10, 11, 12)

    /** Low readiness, as a tired lifter rates it. */
    private const val TIRED = 2

    private val BB_LAGGING = MuscleGroup.CALVES

    /** How many cards [advice] has on each side (deload, easing back, pain hold, then stall or swap): one per line. */
    internal const val ADVICE_CARDS = 4

    // ---- the plan ----

    /** The 3-day template screen 3 picks. */
    fun template(mode: TrainingMode): PlanTemplate = PlanTemplates.forMode(mode).first { it.dayCount == DAYS }

    private class Side(val exercises: Map<Long, Exercise>, val plan: List<PlanItem>)

    private fun side(mode: TrainingMode): Side {
        val draft = PlanTemplates.draft(template(mode), PlanTemplates.suggestedDays(DAYS, emptySet()), strictPress = false)
        val names = draft.sessions.flatMap { s -> s.exercises.map { it.name } }.distinct()
        val exercises = names.mapIndexed { i, name ->
            val entry = Library.find(name) ?: error("$name is not in the catalogue")
            Exercise(i + 1L, entry.name, entry.category, entry.parentLift, entry.muscles, isBuiltIn = true)
        }.associateBy { it.id }
        val idOf = exercises.values.associate { it.name to it.id }
        var nextId = 1L
        // As Templates' Use this plan saves it: powerlifting's fixed reps store repsHigh null.
        val plan = draft.sessions.flatMap { s ->
            s.exercises.mapIndexed { order, e ->
                PlanItem(
                    nextId++, s.day, idOf.getValue(e.name), e.sets, e.repsLow, e.rpe, order, mode,
                    if (mode == TrainingMode.POWERLIFTING) null else e.repsHigh,
                )
            }
        }
        return Side(exercises, plan)
    }

    private val sides: Map<TrainingMode, Lazy<Side>> = TrainingMode.entries.associateWith { lazy { side(it) } }

    private fun sideOf(mode: TrainingMode): Side {
        reads++
        return sides.getValue(mode).value
    }

    /** The sample's exercises, by id: the ids [plan], [advice] and the histories use. */
    fun exercises(mode: TrainingMode): Map<Long, Exercise> = sideOf(mode).exercises

    /** That template's draft, saved, on the templates' 3-day spread (Monday, Wednesday, Friday). */
    fun plan(mode: TrainingMode): List<PlanItem> = sideOf(mode).plan

    private fun idOf(mode: TrainingMode, name: String): Long = exercises(mode).values.first { it.name == name }.id

    // ---- logging and targets ----

    /** Every planned set of [item] at one weight, reps and RPE; the last is the top set. */
    private fun sets(item: PlanItem, kg: Double, reps: Int, rpe: Double?, failed: Boolean = false): List<LoggedSet> =
        (1..item.sets).map { LoggedSet(item.exerciseId, kg, reps, rpe, failed, null, it == item.sets) }

    /** The next date after [date] whose weekday holds a plan item for [exerciseId]. */
    private fun nextDay(plan: List<PlanItem>, exerciseId: Long, date: LocalDate): LocalDate {
        val days = plan.filter { it.exerciseId == exerciseId }.map { it.weekday }.toSet()
        var d = date.plus(1, DateTimeUnit.DAY)
        while (d.dayOfWeek !in days) d = d.plus(1, DateTimeUnit.DAY)
        return d
    }

    /** The powerlifting coach needs 3 sessions or a baseline for a main lift: a baseline from the first set itself. */
    private fun baselines(mode: TrainingMode, starts: Map<Long, Triple<Double, Int, Double>>, date: LocalDate): List<Baseline> =
        if (mode != TrainingMode.POWERLIFTING) {
            emptyList()
        } else {
            starts.map { (id, set) -> Baseline(id, E1rm.estimate(set.first, set.second, set.third), date) }
        }

    private fun buildTargets(mode: TrainingMode): List<GuideTargetStep> {
        val pl = mode == TrainingMode.POWERLIFTING
        val name = if (pl) PL_LIFT else BB_LIFT
        val id = idOf(mode, name)
        val plan = plan(mode)
        val count = if (pl) PL_TARGET_RPES.size else BB_TARGET_REPS.size
        var date = TODAY
        var kg = if (pl) PL_START_KG else BB_START_KG
        val sessions = mutableListOf<LoggedSession>()
        var baselines = emptyList<Baseline>()
        return (0 until count).map { i ->
            val item = plan.first { it.exerciseId == id && it.weekday == date.dayOfWeek }
            val reps = if (pl) item.reps else BB_TARGET_REPS[i]
            val rpe = if (pl) PL_TARGET_RPES[i] else BB_RPE
            if (i == 0) baselines = baselines(mode, mapOf(id to Triple(kg, reps, rpe)), date)
            sessions += LoggedSession(i + 1L, date, null, null, sets(item, kg, reps, rpe), mode)
            val next = nextDay(plan, id, date)
            val history = TrainingHistory(sessions.toList(), exercises(mode), baselines)
            val target = Coaching.targetsFor(mode, next.dayOfWeek, history, plan, SETTINGS, next, emptyList())
                .first { (it.payload as AdvicePayload.Target).exerciseId == id }
            val payload = target.payload as AdvicePayload.Target
            GuideTargetStep(date, name, kg, reps, rpe, payload.weightKg, payload.reps, payload.repsHigh ?: payload.reps, target.reason)
                .also {
                    date = next
                    kg = payload.weightKg
                }
        }
    }

    private val targetSteps: Map<TrainingMode, Lazy<List<GuideTargetStep>>> = TrainingMode.entries.associateWith { lazy { buildTargets(it) } }

    /** One lift, 5 sessions from [TODAY]: what was done, and the coach's next target. */
    fun targets(mode: TrainingMode): List<GuideTargetStep> {
        reads++
        return targetSteps.getValue(mode).value
    }

    /** How many sessions [targets] has, without working them out: one per session result. */
    internal fun targetCount(mode: TrainingMode): Int =
        if (mode == TrainingMode.POWERLIFTING) PL_TARGET_RPES.size else BB_TARGET_REPS.size

    /** The set screen 4 logs: the first target step. */
    fun today(mode: TrainingMode): GuideTargetStep = targets(mode).first()

    // ---- screen 6: when it holds back ----

    private fun daysBefore(n: Long): LocalDate = TODAY.minus(n, DateTimeUnit.DAY)

    private fun history(mode: TrainingMode, vararg sessions: Pair<LocalDate, List<LoggedSet>>, readiness: Int? = null) = TrainingHistory(
        sessions.mapIndexed { i, (date, sets) -> LoggedSession(i + 1L, date, null, readiness, sets, mode) },
        exercises(mode),
        emptyList(),
    )

    private fun evaluated(mode: TrainingMode, history: TrainingHistory, type: AdviceType): Advice =
        Coaching.evaluate(mode, history, plan(mode), SETTINGS, TODAY, emptyList()).single { it.type == type }

    private fun buildAdvice(mode: TrainingMode): List<Advice> {
        val pl = mode == TrainingMode.POWERLIFTING
        val id = idOf(mode, if (pl) PL_LIFT else BB_LIFT)
        val item = plan(mode).first { it.exerciseId == id }
        val kg = if (pl) PL_START_KG else BB_START_KG
        val reps = item.reps
        fun done(failed: Boolean = false) = sets(item, kg, reps, if (pl) PL_TARGET_RPES.first() else BB_RPE, failed)

        // Deload: tired, and on powerlifting also missed sets (the powerlifting side needs two signs; bodybuilding's
        // low readiness is enough by itself).
        val deload = evaluated(
            mode,
            history(mode, daysBefore(7) to done(failed = pl), daysBefore(4) to done(failed = pl), daysBefore(1) to done(), readiness = TIRED),
            AdviceType.DELOAD,
        )
        // Easing back in: back today after a break of the gap's length.
        val reEntry = evaluated(mode, history(mode, daysBefore(Coach.GAP_DAYS) to done(), TODAY to done()), AdviceType.RE_ENTRY)
        // The pain hold's card: the lift skipped for pain today.
        val skip = Skip(1, TODAY, mode, id, item.id, SkipReason.PAIN)
        val pain = Coaching.afterSkip(skip, listOf(skip), history(mode), plan(mode), TODAY, emptyList()).single()
        // Stall (powerlifting): the same top set every week since the stall window's first week, so the card reads
        // what a lifter first sees (the rule fires at that many weeks), and after the first an extra set missed out of
        // the hole. Stuck (bodybuilding): the same sets for the stuck window and two sessions past it.
        val last = if (pl) {
            val missed = LoggedSet(id, kg, reps, null, failed = true, missPoint = MissPoint.FLOOR, isTopSet = false)
            val first = SETTINGS.stallWeeks
            val weeks = (first downTo 1).map { daysBefore(7L * it) to (done() + if (it < first) listOf(missed) else emptyList()) }.toTypedArray()
            evaluated(mode, history(mode, *weeks), AdviceType.STALL)
        } else {
            val week = 7L
            val from = PhysiqueCoach.STUCK_DAYS.toLong()
            val sessions = listOf(from + week, from, from - week, from - 2 * week).map { daysBefore(it) to done() }.toTypedArray()
            evaluated(mode, history(mode, *sessions), AdviceType.SWAP)
        }
        // Distinct ids, as stored cards have, so the apps can key a list by them.
        return listOf(deload, reEntry, pain, last).mapIndexed { i, card -> card.copy(id = i + 1L) }
    }

    private val cards: Map<TrainingMode, Lazy<List<Advice>>> = TrainingMode.entries.associateWith { lazy { buildAdvice(it) } }

    /** Screen 6's cards, in the rule table's order, each from the real rule run on a small history built for it. */
    fun advice(mode: TrainingMode): List<Advice> {
        reads++
        return cards.getValue(mode).value
    }

    // ---- screen 7: months later ----

    private fun progressStarts(mode: TrainingMode): Map<Long, Double> =
        (if (mode == TrainingMode.POWERLIFTING) PL_PROGRESS_START else BB_PROGRESS_START).mapKeys { idOf(mode, it.key) }

    /** The last day of the progress run (exclusive): [PROGRESS_WEEKS] from [TODAY]. */
    internal val PROGRESS_END: LocalDate = TODAY.plus(PROGRESS_WEEKS, DateTimeUnit.WEEK)

    /** The progress run's plan: the plan's items for the run's exercises. */
    internal fun progressPlan(mode: TrainingMode): List<PlanItem> {
        val starts = progressStarts(mode)
        return plan(mode).filter { it.exerciseId in starts }
    }

    private fun buildProgressHistory(mode: TrainingMode): TrainingHistory {
        val pl = mode == TrainingMode.POWERLIFTING
        val starts = progressStarts(mode)
        val plan = progressPlan(mode)
        val end = PROGRESS_END
        val baselines = baselines(mode, starts.mapValues { (id, kg) -> Triple(kg, plan.first { it.exerciseId == id }.reps, PL_PROGRESS_RPES.first()) }, TODAY)
        val sessions = mutableListOf<LoggedSession>()
        val done = mutableMapOf<Long, Int>()
        val sinceIncrease = mutableMapOf<Long, Int>()
        var date = TODAY
        while (date < end) {
            val items = plan.filter { it.weekday == date.dayOfWeek }.sortedBy { it.order }
            if (items.isNotEmpty()) {
                val history = TrainingHistory(sessions.toList(), exercises(mode), baselines)
                val targets = Coaching.targetsFor(mode, date.dayOfWeek, history, plan, SETTINGS, date, emptyList())
                    .associate { (it.payload as AdvicePayload.Target).let { p -> p.exerciseId to p } }
                val sets = items.flatMap { item ->
                    val id = item.exerciseId
                    val n = done.getOrElse(id) { 0 }
                    val kg = targets[id]?.weightKg ?: starts.getValue(id)
                    val previous = sessions.lastOrNull { s -> s.sets.any { it.exerciseId == id } }?.sets?.first { it.exerciseId == id }?.weightKg
                    if (previous != null && kg > previous) sinceIncrease[id] = 0
                    val k = sinceIncrease.getOrElse(id) { 0 }
                    done[id] = n + 1
                    sinceIncrease[id] = k + 1
                    if (pl) {
                        sets(item, kg, item.reps, PL_PROGRESS_RPES[n % PL_PROGRESS_RPES.size])
                    } else {
                        sets(item, kg, BB_PROGRESS_REPS[minOf(k, BB_PROGRESS_REPS.size - 1)], BB_RPE)
                    }
                }
                sessions += LoggedSession(sessions.size + 1L, date, null, null, sets, mode)
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return TrainingHistory(sessions.toList(), exercises(mode), baselines)
    }

    private val progressHistories: Map<TrainingMode, Lazy<TrainingHistory>> =
        TrainingMode.entries.associateWith { lazy { buildProgressHistory(it) } }

    /** The six months [progress] is read from, as the real coach saw them session by session. */
    internal fun progressHistory(mode: TrainingMode): TrainingHistory {
        reads++
        return progressHistories.getValue(mode).value
    }

    private fun buildProgress(mode: TrainingMode): GuideProgress {
        val pl = mode == TrainingMode.POWERLIFTING
        val starts = progressStarts(mode)
        val end = PROGRESS_END
        val history = progressHistory(mode)
        val sessions = history.sessions
        val lead = starts.keys.first()
        val name = { id: Long -> exercises(mode).getValue(id).name }
        return if (pl) {
            val stats = HistoryStats(history)
            val lift = exercises(mode).getValue(lead).mainLift ?: MainLift.SQUAT
            GuideProgress(
                points = stats.weeklyTrend(lift, end).map { it.weekStart to it.e1rmKg },
                bests = starts.keys.map { id ->
                    val l = exercises(mode).getValue(id).mainLift!!
                    name(id) to stats.weeklyTrend(l, end).maxOf { it.e1rmKg }
                },
                totalKg = ProgressMath.total(stats, end, includeStrictPress = false),
                laggingMuscle = null,
            )
        } else {
            fun heaviest(id: Long) = sessions.mapNotNull { s -> s.sets.filter { it.exerciseId == id }.maxOfOrNull { it.weightKg }?.let { s.date to it } }
            GuideProgress(
                points = heaviest(lead),
                bests = starts.keys.map { id -> name(id) to heaviest(id).maxOf { it.second } },
                totalKg = null,
                laggingMuscle = BB_LAGGING.name,
            )
        }
    }

    private val months: Map<TrainingMode, Lazy<GuideProgress>> = TrainingMode.entries.associateWith { lazy { buildProgress(it) } }

    /** About six months from [TODAY], worked out week by week with the real coach. */
    fun progress(mode: TrainingMode): GuideProgress {
        reads++
        return months.getValue(mode).value
    }
}
