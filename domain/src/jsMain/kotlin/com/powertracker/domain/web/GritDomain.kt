@file:OptIn(ExperimentalJsExport::class)

package com.powertracker.domain.web

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.domain.progress.ProgressMath
import com.powertracker.domain.seed.SeedLibrary
import com.powertracker.domain.validation.InputValidator
import com.powertracker.domain.validation.InputWarning

/**
 * The coach and its helpers for the web app. Everything crossing this boundary is plain data (see JsTypes.kt);
 * bad input (an unparseable date, an unknown enum name, a weekday outside 1-7) throws IllegalArgumentException
 * naming the offending field.
 */
@JsExport
object GritDomain {
    fun estimateE1rm(weightKg: Double, reps: Int, rpe: Double?): Double = E1rm.estimate(weightKg, reps, rpe)

    fun platesPerSide(totalKg: Double, changePlates: Boolean): Array<Double>? =
        PlateLoader.perSide(totalKg, changePlates)?.toTypedArray()

    fun roundLoad(kg: Double, changePlates: Boolean): Double = PlateLoader.round(kg, changePlates)

    fun loadStep(changePlates: Boolean): Double = PlateLoader.step(changePlates)

    /** "HIGH_REPS" and/or "BIG_JUMP". */
    fun checkInput(weightKg: Double, reps: Int, lastWeightKg: Double?): Array<String> =
        InputValidator.check(weightKg, reps, lastWeightKg).map {
            when (it) {
                InputWarning.HighReps -> "HIGH_REPS"
                is InputWarning.BigJump -> "BIG_JUMP"
            }
        }.toTypedArray()

    fun parseBaselineKg(text: String): Double? = InputValidator.baselineKg(text)

    fun targetsFor(
        weekday: Int,
        history: JsHistory,
        plan: Array<JsPlanItem>,
        settings: JsCoachSettings,
        today: String,
        prior: Array<JsAdvice>,
    ): Array<JsAdvice> = Coach.targetsFor(
        parseWeekday("weekday", weekday),
        history.toDomain(),
        plan.map { it.toDomain() },
        settings.toDomain(),
        parseDate("today", today),
        prior.map { it.toDomain() },
    ).map { it.toJs() }.toTypedArray()

    fun evaluate(
        history: JsHistory,
        plan: Array<JsPlanItem>,
        settings: JsCoachSettings,
        today: String,
        prior: Array<JsAdvice>,
    ): Array<JsAdvice> = Coach.evaluate(
        history.toDomain(),
        plan.map { it.toDomain() },
        settings.toDomain(),
        parseDate("today", today),
        prior.map { it.toDomain() },
    ).map { it.toJs() }.toTypedArray()

    fun weeklyTrend(history: JsHistory, lift: String, until: String): Array<JsWeeklyPoint> =
        HistoryStats(history.toDomain()).weeklyTrend(parseLift("lift", lift), parseDate("until", until))
            .map { JsWeeklyPoint(it.weekStart.toString(), it.e1rmKg) }.toTypedArray()

    fun currentE1rm(history: JsHistory, lift: String, until: String, days: Int): Double? =
        HistoryStats(history.toDomain()).currentE1rm(parseLift("lift", lift), parseDate("until", until), days.toLong())

    fun prs(history: JsHistory, lift: String): Array<JsPr> =
        ProgressMath.prs(history.toDomain(), parseLift("lift", lift)).map { it.toJs() }.toTypedArray()

    fun weeklyVolume(history: JsHistory, lift: String): Array<JsWeeklyPoint> =
        ProgressMath.weeklyVolume(history.toDomain(), parseLift("lift", lift))
            .map { (week, volume) -> JsWeeklyPoint(week.toString(), volume) }.toTypedArray()

    fun accessoryOptions(lift: String, miss: String?): Array<JsAccessoryOption> =
        AccessoryMap.options(parseLift("lift", lift), miss?.let { parseEnum<MissPoint>("miss", it) })
            .map { it.toJs() }.toTypedArray()

    /** The muscles behind a sticking point, e.g. `["Quads", "Glutes"]`; empty when no miss point is given. */
    fun accessoryMuscles(lift: String, miss: String?): Array<String> =
        AccessoryMap.muscles(parseLift("lift", lift), miss?.let { parseEnum<MissPoint>("miss", it) }).toTypedArray()

    fun seedExercises(): Array<JsExercise> = SeedLibrary.exercises().map { it.toJs() }.toTypedArray()

    /** Squat + bench + deadlift current e1RM as of [until]; null until all three have one. */
    fun total(history: JsHistory, until: String): Double? =
        ProgressMath.total(HistoryStats(history.toDomain()), parseDate("until", until))

    /** The main lift an exercise trains: itself for squat, bench and deadlift, its parent for a variation, null for an accessory. */
    fun mainLiftOf(exercise: JsExercise): String? = exercise.toDomain().mainLift?.name

    /** True for the squat, bench or deadlift itself; false for variations and accessories. */
    fun isMainLift(exercise: JsExercise): Boolean = exercise.toDomain().isMainLift

    /**
     * The top set of the latest session strictly before [before] that has the exercise: the flagged set, else the
     * heaviest good set, else the heaviest. Null when it was never logged before then.
     */
    fun lastTopSet(history: JsHistory, exerciseId: Int, before: String): JsSet? =
        HistoryStats(history.toDomain()).lastTopSet(exerciseId.toLong(), parseDate("before", before))?.second?.toJs()

    /** True once open advice of [type] created on [createdAt] no longer counts on [today] (a deload or re-entry after 14 days). */
    fun openAdviceExpired(type: String, createdAt: String, today: String): Boolean =
        Coach.openExpired(parseEnum<AdviceType>("type", type), parseDate("createdAt", createdAt), parseDate("today", today))

    /** Decodes a stored advice payload, filling in Android's defaults; null when it is not an `AdvicePayload`. */
    fun advicePayload(payloadJson: String): JsAdvicePayload? = decodePayload(payloadJson)

    /** The whole exercise catalogue, area by area. */
    fun library(): Array<JsLibraryExercise> = Library.all().map { it.toJs() }.toTypedArray()

    /** The catalogue entry whose name or alias matches [name] ignoring case, hyphens and spacing; null when there is none. */
    fun findInLibrary(name: String): JsLibraryExercise? = Library.find(name)?.toJs()

    /**
     * Catalogue search: [areas] are `BodyArea` names and [equipment] `Equipment` names; an empty array means no filter.
     * Ranked names-start, names-contain, aliases, muscles; alphabetical within a rank.
     */
    fun searchLibrary(query: String, areas: Array<String>, equipment: Array<String>): Array<JsLibraryExercise> =
        Library.search(
            query,
            areas.map { parseEnum<BodyArea>("areas", it) }.toSet(),
            equipment.map { parseEnum<Equipment>("equipment", it) }.toSet(),
        ).map { it.toJs() }.toTypedArray()

    /** The `MuscleGroup` names with at least one catalogue exercise, in the order the picker's grid shows them. */
    fun muscleGroups(): Array<String> = Library.muscleGroups().map { it.name }.toTypedArray()

    /** What the picker calls a `MuscleGroup`, e.g. "Upper back" for `UPPER_BACK`. */
    fun muscleGroupLabel(group: String): String = parseEnum<MuscleGroup>("group", group).label

    /** The `MuscleGroup` name a muscle string belongs to ("Pecs" is `CHEST`); null when it belongs to none. */
    fun muscleGroupOf(muscle: String): String? = MuscleGroups.of(muscle)?.name

    /**
     * Every catalogue exercise that works [group] (a `MuscleGroup` name) with any of [equipment] (`Equipment` names;
     * an empty array means no filter), alphabetically.
     */
    fun exercisesByMuscle(group: String, equipment: Array<String>): Array<JsLibraryExercise> =
        Library.byMuscle(
            parseEnum<MuscleGroup>("group", group),
            equipment.map { parseEnum<Equipment>("equipment", it) }.toSet(),
        ).map { it.toJs() }.toTypedArray()

    /** The form guide of the catalogue entry [name] resolves to; null for anything not in the catalogue. */
    fun formGuide(name: String): JsFormGuide? = Library.find(name)?.guide?.toJs()

    /** The fixed last line of every guide sheet, which the UI appends after the guide. */
    fun safetyLine(): String = Library.SAFETY_LINE

    /** Typical strength ratios: squat ÷ deadlift, then bench ÷ squat. */
    fun ratioRanges(): Array<JsRatioRange> = arrayOf(
        ratioRange(MainLift.SQUAT, MainLift.DEADLIFT, WeakPointRule.SQUAT_TO_DEADLIFT),
        ratioRange(MainLift.BENCH, MainLift.SQUAT, WeakPointRule.BENCH_TO_SQUAT),
    )

    private fun ratioRange(numerator: MainLift, denominator: MainLift, typical: ClosedFloatingPointRange<Double>) =
        JsRatioRange(numerator.name, denominator.name, typical.start, typical.endInclusive)
}
