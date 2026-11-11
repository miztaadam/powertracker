@file:OptIn(ExperimentalJsExport::class)

package com.powertracker.domain.web

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
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

    fun seedExercises(): Array<JsExercise> = SeedLibrary.exercises().map { it.toJs() }.toTypedArray()

    /** Typical strength ratios as `[sdLo, sdHi, bsLo, bsHi]`: squat ÷ deadlift, then bench ÷ squat. */
    fun ratioRanges(): Array<Double> = arrayOf(
        WeakPointRule.SQUAT_TO_DEADLIFT.start, WeakPointRule.SQUAT_TO_DEADLIFT.endInclusive,
        WeakPointRule.BENCH_TO_SQUAT.start, WeakPointRule.BENCH_TO_SQUAT.endInclusive,
    )
}
