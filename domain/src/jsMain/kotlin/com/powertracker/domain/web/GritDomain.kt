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
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.hasFigure
import com.powertracker.domain.library.figures.FigureRenderer
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.library.RpeGuide
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.liftLabel as domainLiftLabel
import com.powertracker.domain.model.missPointLabel as domainMissPointLabel
import com.powertracker.domain.model.repsText as domainRepsText
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

    /**
     * Squat + bench + deadlift current e1RM as of [until], plus strict press when [includeStrictPress]; null until every
     * included lift has one.
     */
    fun total(history: JsHistory, until: String, includeStrictPress: Boolean): Double? =
        ProgressMath.total(HistoryStats(history.toDomain()), parseDate("until", until), includeStrictPress)

    /** Every `MainLift` name in order: SQUAT, BENCH, DEADLIFT, STRICT_PRESS. */
    fun mainLifts(): Array<String> = MainLift.entries.map { it.name }.toTypedArray()

    /** What the apps call [lift] (a `MainLift` name), e.g. "Strict press". */
    fun liftLabel(lift: String): String = domainLiftLabel(parseLift("lift", lift))

    /** The label of the miss-point choice [point] (a `MissPoint` name) when logging [lift]: "Off the shoulders" for a strict press floor miss. */
    fun missPointLabel(lift: String, point: String): String =
        domainMissPointLabel(parseLift("lift", lift), parseEnum<MissPoint>("point", point))

    /** Reps as the apps print them: "5" for fixed reps ([repsHigh] null), "8–12" (en dash) for a range. */
    fun repsText(reps: Int, repsHigh: Int?): String = domainRepsText(reps, repsHigh)

    /** The main lift an exercise trains: itself for a main lift, its parent for a variation, null for an accessory. */
    fun mainLiftOf(exercise: JsExercise): String? = exercise.toDomain().mainLift?.name

    /** True for a main lift itself (squat, bench, deadlift or strict press); false for variations and accessories. */
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

    /** Whether the one-time strict press upgrade matches a row named [name]: its frozen name list, normalised as the catalogue matches. */
    fun isStrictPressUpgradeName(name: String): Boolean = Library.isStrictPressUpgradeName(name)

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

    /**
     * Every catalogue exercise that works any of [groups] (`MuscleGroup` names) with any of [equipment] (`Equipment`
     * names; an empty array means no filter), each once, alphabetically. An empty [groups] matches nothing.
     */
    fun exercisesByMuscles(groups: Array<String>, equipment: Array<String>): Array<JsLibraryExercise> =
        Library.byMuscles(
            groups.map { parseEnum<MuscleGroup>("groups", it) }.toSet(),
            equipment.map { parseEnum<Equipment>("equipment", it) }.toSet(),
        ).map { it.toJs() }.toTypedArray()

    /** The form guide of the catalogue entry [name] resolves to; null for anything not in the catalogue. */
    fun formGuide(name: String): JsFormGuide? = Library.find(name)?.guide?.toJs()

    /**
     * The picture of the catalogue entry [name] resolves to (names, aliases and former names, as [findInLibrary]):
     * its captions, the thumbnail's pose ("start" or "end") and the view box [400, 300]; null when it has none.
     */
    fun figureInfo(name: String): JsFigureInfo? = Library.figure(name)?.let {
        JsFigureInfo(
            it.startCaption, it.endCaption, it.thumbnail.name.lowercase(),
            arrayOf(FigureRenderer.viewBox.width, FigureRenderer.viewBox.height),
        )
    }

    /**
     * Whether the catalogue entry [name] resolves to has a picture (the same names as [figureInfo]), without building
     * it: cheap enough to ask of every row of a list.
     */
    fun hasFigure(name: String): Boolean = Library.hasFigure(name)

    /** The shapes of one panel ([which] "start" or "end"), in draw order; null when [name] has no picture. */
    fun figureShapes(name: String, which: String): Array<JsFigureShape>? {
        val panel = when (which) {
            "start" -> Which.START
            "end" -> Which.END
            else -> throw IllegalArgumentException("which: '$which' is not one of start, end")
        }
        return Library.figure(name)?.let { f -> FigureRenderer.shapes(f, panel).map { it.toJs() }.toTypedArray() }
    }

    /** The fixed last line of every guide sheet, which the UI appends after the guide. */
    fun safetyLine(): String = Library.SAFETY_LINE

    /** The "What RPE means" sheet: RPE as reps in reserve. It has no safety line; its sources are not shown. */
    fun rpeGuide(): JsRpeGuide = JsRpeGuide(RpeGuide.title, RpeGuide.lines.toTypedArray())

    /** Typical strength ratios: squat ÷ deadlift, then bench ÷ squat. Strict press is never in one. */
    fun ratioRanges(): Array<JsRatioRange> = arrayOf(
        ratioRange(MainLift.SQUAT, MainLift.DEADLIFT, WeakPointRule.SQUAT_TO_DEADLIFT),
        ratioRange(MainLift.BENCH, MainLift.SQUAT, WeakPointRule.BENCH_TO_SQUAT),
    )

    private fun ratioRange(numerator: MainLift, denominator: MainLift, typical: ClosedFloatingPointRange<Double>) =
        JsRatioRange(numerator.name, denominator.name, typical.start, typical.endInclusive)
}
