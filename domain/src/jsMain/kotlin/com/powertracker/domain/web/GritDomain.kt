@file:OptIn(ExperimentalJsExport::class)

package com.powertracker.domain.web

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.coach.BodybuildingCoach
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.forSide
import com.powertracker.domain.progress.MuscleProgress
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.hasFigure
import com.powertracker.domain.library.figures.FigureRenderer
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.library.RpeGuide
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.motion.ModeSwitchMotion
import com.powertracker.domain.motion.Point
import com.powertracker.domain.model.liftLabel as domainLiftLabel
import com.powertracker.domain.model.missPointLabel as domainMissPointLabel
import com.powertracker.domain.model.repsText as domainRepsText
import com.powertracker.domain.plan.BuilderAnswers
import com.powertracker.domain.plan.PlanBuilder
import com.powertracker.domain.plan.PlanTemplate
import com.powertracker.domain.plan.PlanTemplates
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.domain.progress.BodybuildingProgress
import com.powertracker.domain.progress.ProgressMath
import com.powertracker.domain.seed.SeedLibrary
import com.powertracker.domain.validation.InputValidator
import com.powertracker.domain.validation.InputWarning
import kotlinx.datetime.isoDayNumber

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

    /**
     * Today's targets for [mode]'s side (a `TrainingMode` name), from the whole log, the whole plan and every stored
     * advice: the app's one way in to both coaches. On the powerlifting side it is exactly [targetsFor].
     */
    fun coachingTargetsFor(
        mode: String,
        weekday: Int,
        history: JsHistory,
        plan: Array<JsPlanItem>,
        settings: JsCoachSettings,
        today: String,
        prior: Array<JsAdvice>,
    ): Array<JsAdvice> = Coaching.targetsFor(
        parseEnum<TrainingMode>("mode", mode),
        parseWeekday("weekday", weekday),
        history.toDomain(),
        plan.map { it.toDomain() },
        settings.toDomain(),
        parseDate("today", today),
        prior.map { it.toDomain() },
    ).map { it.toJs() }.toTypedArray()

    /**
     * As [coachingTargetsFor], with every stored skip: an exercise held by a pain skip (on either side, until a session
     * of it is logged after the skip) repeats its latest session's weight instead of going up.
     */
    fun coachingTargetsForWithSkips(
        mode: String,
        weekday: Int,
        history: JsHistory,
        plan: Array<JsPlanItem>,
        settings: JsCoachSettings,
        today: String,
        prior: Array<JsAdvice>,
        skips: Array<JsSkip>,
    ): Array<JsAdvice> = Coaching.targetsFor(
        parseEnum<TrainingMode>("mode", mode),
        parseWeekday("weekday", weekday),
        history.toDomain(),
        plan.map { it.toDomain() },
        settings.toDomain(),
        parseDate("today", today),
        prior.map { it.toDomain() },
        skips.map { it.toDomain() },
    ).map { it.toJs() }.toTypedArray()

    /**
     * The coach's answer to [skip], just stored: zero or one SKIP advice to store, stamped with the skip's side.
     * [skips] is every stored skip, [skip] among them; [plan] the whole plan; [prior] every stored advice. A pain skip
     * gives its card unless an OPEN pain SKIP card for the exercise stands on that side, or one was dismissed within 7
     * days (made 0 to 6 days ago); the caller deletes that side's OPEN busy or repeat SKIP card for the exercise first
     * ([com.powertracker.domain.coach.SkipCoach]).
     */
    fun coachingAfterSkip(
        skip: JsSkip,
        skips: Array<JsSkip>,
        history: JsHistory,
        plan: Array<JsPlanItem>,
        today: String,
        prior: Array<JsAdvice>,
    ): Array<JsAdvice> = Coaching.afterSkip(
        skip.toDomain(),
        skips.map { it.toDomain() },
        history.toDomain(),
        plan.map { it.toDomain() },
        parseDate("today", today),
        prior.map { it.toDomain() },
    ).map { it.toJs() }.toTypedArray()

    /**
     * A SKIP advice's stored payload ([payloadJson]) applied with [choice], one of its alternatives, to [mode]'s side
     * (a `TrainingMode` name) of [plan]. The bodybuilding side keeps its weekly ceiling; the powerlifting side has none.
     */
    fun coachingApplySkipSwap(payloadJson: String, choice: String, mode: String, plan: Array<JsPlanItem>, exercises: Array<JsExercise>): JsPlanChange =
        Coaching.applySkipSwap(
            payloadOf<AdvicePayload.Skipped>("payloadJson", payloadJson), choice, parseEnum<TrainingMode>("mode", mode),
            plan.map { it.toDomain() }, exercises.map { it.toDomain() }.associateBy { it.id },
        ).toJs()

    /** New advice to store for [mode]'s side (a `TrainingMode` name). On the powerlifting side it is exactly [evaluate]. */
    fun coachingEvaluate(
        mode: String,
        history: JsHistory,
        plan: Array<JsPlanItem>,
        settings: JsCoachSettings,
        today: String,
        prior: Array<JsAdvice>,
    ): Array<JsAdvice> = Coaching.evaluate(
        parseEnum<TrainingMode>("mode", mode),
        history.toDomain(),
        plan.map { it.toDomain() },
        settings.toDomain(),
        parseDate("today", today),
        prior.map { it.toDomain() },
    ).map { it.toJs() }.toTypedArray()

    /**
     * As [coachingEvaluate], with the bodybuilding side's [checkIns]: on that side it adds the SWAP advice for stuck
     * exercises (a growing-well group's exercises are left alone). The powerlifting side ignores [checkIns].
     */
    fun coachingEvaluateWithCheckIns(
        mode: String,
        history: JsHistory,
        plan: Array<JsPlanItem>,
        settings: JsCoachSettings,
        today: String,
        prior: Array<JsAdvice>,
        checkIns: Array<JsCheckIn>,
    ): Array<JsAdvice> = Coaching.evaluate(
        parseEnum<TrainingMode>("mode", mode),
        history.toDomain(),
        plan.map { it.toDomain() },
        settings.toDomain(),
        parseDate("today", today),
        prior.map { it.toDomain() },
        checkIns.map { it.toDomain() },
    ).map { it.toJs() }.toTypedArray()

    /** Whether a check-in is due on the bodybuilding side: a plan, a session, and 28 days since the later of the last check-in and the first session. */
    fun coachingCheckInDue(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, today: String): Boolean =
        Coaching.checkInDue(history.toDomain(), plan.map { it.toDomain() }, checkIns.map { it.toDomain() }, parseDate("today", today))

    /**
     * What saving [checkIn] gives, from the whole plan (only the bodybuilding side's items are read): new MORE_WORK
     * advice stamped BODYBUILDING, a note for every other lagging group, and the OPEN MORE_WORK advice of [prior] now
     * DISMISSED (store those with the check-in).
     */
    fun physiqueAfterCheckIn(checkIn: JsCheckIn, history: JsHistory, plan: Array<JsPlanItem>, today: String, prior: Array<JsAdvice>): JsCheckInOutcome =
        Coaching.afterCheckIn(
            checkIn.toDomain(), history.toDomain(), plan.map { it.toDomain() }, parseDate("today", today), prior.map { it.toDomain() },
        ).toJs()

    /** The SWAP advice for the bodybuilding plan's stuck exercises (also part of [coachingEvaluateWithCheckIns]). */
    fun physiqueStuck(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, prior: Array<JsAdvice>, today: String): Array<JsAdvice> =
        PhysiqueCoach.stuck(
            history.toDomain(), plan.map { it.toDomain() }, checkIns.map { it.toDomain() },
            prior.map { it.toDomain() }.forSide(TrainingMode.BODYBUILDING), parseDate("today", today),
        ).map { it.toJs() }.toTypedArray()

    /** A MORE_WORK advice's stored payload ([payloadJson]) applied to the bodybuilding side of [plan]. */
    fun physiqueApplyMoreWork(payloadJson: String, plan: Array<JsPlanItem>, exercises: Array<JsExercise>): JsPlanChange =
        PhysiqueCoach.applyMoreWork(
            payloadOf<AdvicePayload.MoreWork>("payloadJson", payloadJson), plan.map { it.toDomain() },
            exercises.map { it.toDomain() }.associateBy { it.id },
        ).toJs()

    /** A SWAP advice's stored payload ([payloadJson]) applied with [choice], one of its alternatives. */
    fun physiqueApplySwap(payloadJson: String, choice: String, plan: Array<JsPlanItem>, exercises: Array<JsExercise>): JsPlanChange =
        PhysiqueCoach.applySwap(
            payloadOf<AdvicePayload.Swap>("payloadJson", payloadJson), choice, plan.map { it.toDomain() },
            exercises.map { it.toDomain() }.associateBy { it.id },
        ).toJs()

    /** [group]'s (a `MuscleGroup` name) weekly counted sets in the bodybuilding side of [plan]. */
    fun physiqueWeeklySets(plan: Array<JsPlanItem>, exercises: Array<JsExercise>, group: String): Int =
        PhysiqueCoach.weeklySets(plan.map { it.toDomain() }, exercises.map { it.toDomain() }.associateBy { it.id }, parseEnum<MuscleGroup>("group", group))

    /** The bodybuilding side's body-part Progress rows, in `MuscleGroup` order. */
    fun muscleProgressRows(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, today: String): Array<JsMuscleRow> =
        MuscleProgress.rows(history.toDomain(), plan.map { it.toDomain() }, checkIns.map { it.toDomain() }, parseDate("today", today))
            .map { it.toJs() }.toTypedArray()

    /**
     * Whether to show the welcome-back card ([Coaching.welcomeBack]): the side has a plan, was left ([leftOn], ISO date
     * or null) more than 30 days before [today], and its earliest session ([firstSessionOn], ISO date, or null when it
     * has none) is on or before [leftOn].
     */
    fun welcomeBack(leftOn: String?, today: String, hasPlan: Boolean, firstSessionOn: String?): Boolean =
        Coaching.welcomeBack(
            leftOn?.let { parseDate("leftOn", it) }, parseDate("today", today), hasPlan,
            firstSessionOn?.let { parseDate("firstSessionOn", it) },
        )

    /**
     * Reps to put in the reps field for set [setNumber] (1-based) of a bodybuilding [item]: last time's reps for that
     * set + 1, capped at the top of the range; the low end the first time and after an increase.
     */
    fun repsToBeat(item: JsPlanItem, history: JsHistory, setNumber: Int, today: String): Int =
        BodybuildingCoach.repsToBeat(item.toDomain(), history.toDomain(), setNumber, parseDate("today", today))

    /**
     * As [repsToBeat], with every stored skip: while a pain skip holds the exercise there is no increase, so the reps
     * carry on from last time instead of starting again at the low end.
     */
    fun repsToBeatWithSkips(item: JsPlanItem, history: JsHistory, setNumber: Int, today: String, skips: Array<JsSkip>): Int =
        BodybuildingCoach.repsToBeat(item.toDomain(), history.toDomain(), setNumber, parseDate("today", today), skips.map { it.toDomain() })

    /** The bodybuilding side's Progress rows: one per exercise in the bodybuilding plan, in plan order. */
    fun bodybuildingProgress(history: JsHistory, plan: Array<JsPlanItem>, today: String): Array<JsExerciseTrend> =
        BodybuildingProgress.rows(history.toDomain(), plan.map { it.toDomain() }, parseDate("today", today))
            .map { it.toJs() }.toTypedArray()

    /**
     * The bodybuilding plan builder's draft week. [days] are 3 or 4 distinct weekdays 1-7; [picks] the ticked
     * exercises (empty: choose for me); [priorities] up to 3 distinct target-group `MuscleGroup` names in the lifter's order;
     * [variant] 0 for the first draft, then 1, 2, ... for "Another draft". Throws IllegalArgumentException otherwise.
     */
    fun planBuilderBuild(days: Array<Int>, picks: Array<JsBuilderPick>, priorities: Array<String>, variant: Int): JsPlanDraft =
        PlanBuilder.build(
            BuilderAnswers(
                days.map { parseWeekday("days", it) },
                picks.map { it.toDomain() },
                priorities.map { parseEnum<MuscleGroup>("priorities", it) },
                variant,
            ),
        ).toJs()

    /**
     * Catalogue exercises to swap [name] on [day] (1-7; null: the first day that holds it) for, none already on that
     * day: for a slot exercise its kind's defaults first (by the equipment [picks] use), then the rest its kind
     * accepts; for a held-over pick the exercises with the same main group.
     */
    fun planBuilderAlternatives(name: String, draft: JsPlanDraft, picks: Array<JsBuilderPick>, day: Int?): Array<JsLibraryExercise> =
        PlanBuilder.alternatives(name, draft.toDomain(), picks.map { it.toDomain() }, day?.let { parseWeekday("day", it) })
            .map { it.toJs() }.toTypedArray()

    /** [draft] with [from] on [day] (1-7) replaced by the catalogue exercise [to], same place, sets and range; volume recounted. */
    fun planBuilderSwap(draft: JsPlanDraft, day: Int, from: String, to: String): JsPlanDraft =
        PlanBuilder.swap(draft.toDomain(), parseWeekday("day", day), from, to).toJs()

    /**
     * Exercise ids to pre-tick in the builder: in either side's [plans], then logged in the 28 days up to [today]; each
     * once, no archived ones.
     */
    fun planBuilderSuggestedPicks(history: JsHistory, plans: Array<JsPlanItem>, today: String): Array<Int> =
        PlanBuilder.suggestedPicks(history.toDomain(), plans.map { it.toDomain() }, parseDate("today", today))
            .map { it.toFacadeId("suggestedPicks") }.toTypedArray()

    /** The most body parts the lifter may choose to improve (3). */
    fun planBuilderMaxPriorities(): Int = PlanBuilder.MAX_PRIORITIES

    /** The `MuscleGroup` names a priority may be, in the builder's order: the 12 target groups. */
    fun planBuilderTargetGroups(): Array<String> = PlanBuilder.TARGET_GROUPS.map { it.name }.toTypedArray()

    /** The ready-made plans for [mode] (a `TrainingMode` name), fewest days first. */
    fun planTemplatesFor(mode: String): Array<JsPlanTemplate> =
        PlanTemplates.forMode(parseEnum<TrainingMode>("mode", mode)).map { it.toJs() }.toTypedArray()

    /** The weekdays (1-7, Monday = 1, in week order) to tick first for a [count]-day template: [current] when it has [count] days, else a fixed spread. */
    fun planTemplateSuggestedDays(count: Int, current: Array<Int>): Array<Int> =
        PlanTemplates.suggestedDays(count, current.map { parseWeekday("current", it) }.toSet()).map { it.isoDayNumber }.toTypedArray()

    /**
     * Template [id] as a draft week on [days] (1-7, as many as the template has, distinct; sorted Monday first), with
     * strict-press entries only when [strictPress]. Throws IllegalArgumentException for an unknown id or wrong days.
     */
    fun planTemplateDraft(id: String, days: Array<Int>, strictPress: Boolean): JsPlanDraft =
        PlanTemplates.draft(templateById(id), days.map { parseWeekday("days", it) }, strictPress).toJs()

    /**
     * Catalogue exercises to swap [name] on [day] (1-7) of template [id]'s [draft] for: none for a main lift, other
     * variations of the same lift for a variation, otherwise the builder's list without main lifts and variations;
     * never one already in the week.
     */
    fun planTemplateAlternatives(id: String, draft: JsPlanDraft, day: Int, name: String): Array<JsLibraryExercise> =
        PlanTemplates.alternatives(templateById(id), draft.toDomain(), parseWeekday("day", day), name).map { it.toJs() }.toTypedArray()

    /** [draft] with [from] on [day] (1-7) replaced by the catalogue exercise [to], same place, sets, reps and RPE; volume recounted. */
    fun planTemplateSwap(draft: JsPlanDraft, day: Int, from: String, to: String): JsPlanDraft =
        PlanTemplates.swap(draft.toDomain(), parseWeekday("day", day), from, to).toJs()

    private fun templateById(id: String): PlanTemplate =
        PlanTemplates.byId(id) ?: throw IllegalArgumentException("id: no plan template '$id'")

    fun weeklyTrend(history: JsHistory, lift: String, until: String): Array<JsWeeklyPoint> =
        HistoryStats(history.toDomain()).weeklyTrend(parseLift("lift", lift), parseDate("until", until))
            .map { JsWeeklyPoint(it.weekStart.toString(), it.e1rmKg) }.toTypedArray()

    fun currentE1rm(history: JsHistory, lift: String, until: String, days: Int): Double? =
        HistoryStats(history.toDomain()).currentE1rm(parseLift("lift", lift), parseDate("until", until), days.toLong())

    fun prs(history: JsHistory, lift: String): Array<JsPr> =
        ProgressMath.prs(history.toDomain(), parseLift("lift", lift)).map { it.toJs() }.toTypedArray()

    /** Whether [set], about to be logged on [date] in a [mode] session, is a new best (ProgressMath.isNewPr). */
    fun isNewPr(history: JsHistory, set: JsSet, date: String, mode: String): Boolean =
        ProgressMath.isNewPr(history.toDomain(), set.toDomain(), parseDate("date", date), parseEnum<TrainingMode>("mode", mode))

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

    /**
     * The stretch and squeeze of the catalogue entry [name] resolves to (names, aliases and former names, ignoring
     * case, as [formGuide]): either [JsMuscleFocus.stretch] and [JsMuscleFocus.squeeze], or [JsMuscleFocus.none] (the
     * fixed line); null only for a name outside the catalogue (the lifter's own exercise). Asked for one exercise at a time, so
     * [library] carries no lines.
     */
    fun muscleFocus(name: String): JsMuscleFocus? = when (val focus = Library.focus(name)) {
        is MuscleFocus.Lines -> JsMuscleFocus(focus.stretch, focus.squeeze, null)
        is MuscleFocus.NoFocus -> JsMuscleFocus(null, null, Library.NO_FOCUS_LINE)
        null -> null
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

    // ---- the mode switch animations (ModeSwitchMotion): ms from the tap, CSS px on a w x h screen ----

    /** The hammer drop's timeline. */
    fun modeSwitchDropTimeline(): JsDropTimeline = ModeSwitchMotion.DROP.toJs()

    /** The pump's timeline. */
    fun modeSwitchPumpTimeline(): JsPumpTimeline = ModeSwitchMotion.PUMP.toJs()

    /** The vibration patterns for `navigator.vibrate`, with Android's amplitudes beside them. */
    fun modeSwitchVibration(): JsModeSwitchVibration = JsModeSwitchVibration(
        ModeSwitchMotion.DROP_VIBE.map { it.toInt() }.toTypedArray(), ModeSwitchMotion.DROP_AMPLITUDES.toTypedArray(),
        ModeSwitchMotion.PUMP_VIBE.map { it.toInt() }.toTypedArray(), ModeSwitchMotion.PUMP_AMPLITUDES.toTypedArray(),
    )

    /** The effects' colours. */
    fun modeSwitchColors(): JsModeSwitchColors = modeSwitchColorsJs()

    /** The light scheme's effects, the same set Android draws. */
    fun modeSwitchLightColors(): JsModeSwitchLightColors = modeSwitchLightColorsJs()

    /** The light scheme's edge under a core vein [coreWidth] wide, at the frame's `veinWidthScale`. */
    fun modeSwitchVeinEdgeWidth(coreWidth: Double, widthScale: Double): Double = ModeSwitchMotion.veinEdgeWidth(coreWidth, widthScale)

    /** The sparks' stroke width. */
    fun modeSwitchSparkWidth(): Double = ModeSwitchMotion.SPARK_WIDTH

    /** The hammer's shapes, back to front, the striking boss at (0, 0): place them with a frame's pose. */
    fun modeSwitchHammerPaths(): Array<JsMotionPath> = ModeSwitchMotion.HAMMER_PATHS.map { it.toJs() }.toTypedArray()

    /** The rune's glow, over the hammer at the frame's `runeOpacity`. */
    fun modeSwitchHammerRune(): JsMotionPath = ModeSwitchMotion.HAMMER_RUNE.toJs()

    /** The gradients the shapes name with `url(#id)`. */
    fun modeSwitchGradients(): Array<JsMotionGradient> = ModeSwitchMotion.GRADIENTS.map { it.toJs() }.toTypedArray()

    /** Where the hammer lands: the bottom edge, centred. */
    fun modeSwitchHitPoint(w: Double, h: Double): JsMotionPoint = ModeSwitchMotion.hitPoint(w, h).toJs()

    /** The hammer drop at [tMs]; at or after the end everything is at rest. */
    fun modeSwitchDropFrame(tMs: Double, w: Double, h: Double): JsDropFrame = ModeSwitchMotion.dropFrame(tMs, w, h).toJs()

    /** The hop (translateY) of an element whose centre is at [elementCenterY], as the shock reaches it. */
    fun modeSwitchHop(tMs: Double, elementCenterY: Double, hitY: Double): Double = ModeSwitchMotion.hop(tMs, elementCenterY, hitY)

    /** The pump at [tMs] from the switch at ([srcX], [srcY]); at or after the end it is at rest. */
    fun modeSwitchPumpFrame(tMs: Double, w: Double, h: Double, srcX: Double, srcY: Double): JsPumpFrame =
        ModeSwitchMotion.pumpFrame(tMs, w, h, Point(srcX, srcY)).toJs()

    /** The swell (scale round its own centre) of an element whose centre is at [elementCenterY]. */
    fun modeSwitchPumpElementScale(tMs: Double, w: Double, elementCenterY: Double, srcX: Double, srcY: Double): Double =
        ModeSwitchMotion.pumpElementScale(tMs, w, elementCenterY, Point(srcX, srcY))

    /** The cracks round the hit, seeded. */
    fun modeSwitchCracks(w: Double, h: Double, hitX: Double, hitY: Double): Array<JsMotionPath> =
        ModeSwitchMotion.cracks(w, h, Point(hitX, hitY)).map { it.toJs() }.toTypedArray()

    /** The chalk dust, seeded; the frame's `dust` follows it index for index. */
    fun modeSwitchDust(hitX: Double, hitY: Double): Array<JsMotionParticle> =
        ModeSwitchMotion.dust(Point(hitX, hitY)).map { it.toJs() }.toTypedArray()

    /** The sparks, seeded; the frame's `sparks` follows it index for index. */
    fun modeSwitchSparks(hitX: Double, hitY: Double): Array<JsMotionParticle> =
        ModeSwitchMotion.sparks(Point(hitX, hitY)).map { it.toJs() }.toTypedArray()

    /** The starburst's points round the hit, before the frame's `burstScale`. */
    fun modeSwitchBurst(hitX: Double, hitY: Double): Array<JsMotionPoint> =
        ModeSwitchMotion.burst(Point(hitX, hitY)).map { it.toJs() }.toTypedArray()

    /** The veins from the switch, aimed at the screen's centre and reaching its edges, in drawing order. */
    fun modeSwitchVeins(w: Double, h: Double, srcX: Double, srcY: Double): Array<JsVeinPath> =
        ModeSwitchMotion.veins(w, h, Point(srcX, srcY)).map { it.toJs() }.toTypedArray()

    /** The veins' branch nodes, drawn between the hot veins and the pulses. */
    fun modeSwitchVeinNodes(w: Double, h: Double, srcX: Double, srcY: Double): Array<JsVeinNode> =
        ModeSwitchMotion.veinNodes(w, h, Point(srcX, srcY)).map { it.toJs() }.toTypedArray()

    /** Whether a node that starts at [d0] shows at the frame's `veinFront`. */
    fun modeSwitchNodeShown(front: Double, d0: Double): Boolean = ModeSwitchMotion.nodeShown(front, d0)

    /** How much of a vein starting at [d0], [len] long, is drawn at the frame's `veinFront`: 0..1, hidden at 0. */
    fun modeSwitchVeinProgress(front: Double, d0: Double, len: Double): Double =
        ModeSwitchMotion.veinProgress(front, d0, len)

    /** A pulse's opacity: the frame's `pulseOpacity`, or 0 once the front has run past the end of a [len] long vein. */
    fun modeSwitchPulseOpacity(pulseOpacity: Double, front: Double, len: Double): Double =
        ModeSwitchMotion.pulseOpacity(pulseOpacity, front, len)

    /** Where a pulse's bead starts along a [len] long vein, 0..1 of its length; the bead is the next 6%. */
    fun modeSwitchPulseStart(front: Double, len: Double): Double = ModeSwitchMotion.pulseStart(front, len)

    // ---- the guide ----

    /** The guide's 8 screens (`GuideScreenId` names), in order. */
    fun guideScreens(): Array<String> = Guide.SCREENS.map { it.name }.toTypedArray()

    /** Screen [id] (a `GuideScreenId` name) for [mode]'s side: its title, lines and whether its picture is an example. */
    fun guideScreen(id: String, mode: String): JsGuideScreen =
        Guide.screen(parseEnum<GuideScreenId>("id", id), parseEnum<TrainingMode>("mode", mode)).toJs()

    /** What screen [id]'s picture shows, step by step (`GuideStep` names), on [mode]'s side. */
    fun guideSteps(id: String, mode: String): Array<String> =
        Guide.steps(parseEnum<GuideScreenId>("id", id), parseEnum<TrainingMode>("mode", mode)).map { it.name }.toTypedArray()

    /** How long a picture step holds once its move has ended, in ms. */
    fun guideStepMs(): Int = Guide.STEP_MS

    /** A step's move, in ms: the web's layout moves take this long. */
    fun guideMoveMs(): Int = Guide.MOVE_MS

    /** A row new in a step fades in (one gone fades out) this long, in ms. */
    fun guideFadeMs(): Int = Guide.FADE_MS

    /** When step [index] of a picture starts, in ms from the picture's start. */
    fun guideStepStartMs(index: Int): Int = Guide.stepStartMs(index)

    /** When step [index] of a picture has settled (its move over), in ms from the picture's start. */
    fun guideSettledMs(index: Int): Int = Guide.settledMs(index)

    /** When a picture of [count] steps is done (its last step settled), in ms from its start. */
    fun guideDoneMs(count: Int): Int = Guide.doneMs(count)

    /** The move's curve at [x] (0 to 1 of its time): how far it has gone, 0 to 1. */
    fun guideEase(x: Double): Double = Guide.ease(x)

    /** The tap ring's time, in ms: the last of the hold before a step that stands for a tap. */
    fun guideTapMs(): Int = Guide.TAP_MS

    /** The control (a `GuideTap` name) that [step] (a `GuideStep` name) stands for pressing, or null. */
    fun guideTap(step: String): String? = Guide.tap(parseEnum<GuideStep>("step", step))?.name

    /**
     * When the motion into step [index] of [steps] (`GuideStep` names, as [guideSteps] gives them) starts, in ms from
     * the picture's start: its tap ring for a step that stands for a tap, else its move.
     */
    fun guideMotionStartMs(steps: Array<String>, index: Int): Int =
        Guide.motionStartMs(steps.map { parseEnum<GuideStep>("step", it) }, index)

    /** Where screen [id]'s picture on [mode]'s side is [tMs] after it started. */
    fun guideFrame(id: String, mode: String, tMs: Int): JsGuideFrame =
        Guide.frame(parseEnum<GuideScreenId>("id", id), parseEnum<TrainingMode>("mode", mode), tMs).toJs()

    /** The sample lifter's fixed today (ISO). */
    fun guideSampleToday(): String = GuideSample.TODAY.toString()

    /** The 3-day template screen 3 picks for [mode]. */
    fun guideSampleTemplate(mode: String): JsPlanTemplate = GuideSample.template(parseEnum<TrainingMode>("mode", mode)).toJs()

    /** That template's draft, saved: the sample's plan items, whose exercise ids are [guideSampleExercises]'. */
    fun guideSamplePlan(mode: String): Array<JsPlanItem> =
        GuideSample.plan(parseEnum<TrainingMode>("mode", mode)).map { it.toJs() }.toTypedArray()

    /** Screen 4's day, for the guide only: the first of the plan's items on the sample's today, in order. */
    fun guideSampleTodayItems(mode: String): Array<JsPlanItem> =
        GuideSample.todayItems(parseEnum<TrainingMode>("mode", mode)).map { it.toJs() }.toTypedArray()

    /** The sample's exercises: the ids its plan and advice use. */
    fun guideSampleExercises(mode: String): Array<JsExercise> =
        GuideSample.exercises(parseEnum<TrainingMode>("mode", mode)).values.sortedBy { it.id }.map { it.toJs() }.toTypedArray()

    /** What screen 4's lift editor shows at [progress] (0 to 1) through the move into [step] (a `GuideStep` name). */
    fun guideSampleTodayEditor(mode: String, step: String, progress: Double): JsGuideEditor =
        GuideSample.todayEditor(parseEnum<TrainingMode>("mode", mode), parseEnum<GuideStep>("step", step), progress).toJs()

    /** The set screen 4 logs: the first of [guideSampleTargets]. */
    fun guideSampleTodaySet(mode: String): JsGuideTargetStep = GuideSample.today(parseEnum<TrainingMode>("mode", mode)).toJs()

    /** One lift, 5 sessions: what was done and the coach's next target. */
    fun guideSampleTargets(mode: String): Array<JsGuideTargetStep> =
        GuideSample.targets(parseEnum<TrainingMode>("mode", mode)).map { it.toJs() }.toTypedArray()

    /** Screen 6's cards, from the real rules, in the rule table's order. */
    fun guideSampleAdvice(mode: String): Array<JsAdvice> =
        GuideSample.advice(parseEnum<TrainingMode>("mode", mode)).map { it.toJs() }.toTypedArray()

    /** The sample lifter about six months on. */
    fun guideSampleProgress(mode: String): JsGuideProgress = GuideSample.progress(parseEnum<TrainingMode>("mode", mode)).toJs()

    private fun ratioRange(numerator: MainLift, denominator: MainLift, typical: ClosedFloatingPointRange<Double>) =
        JsRatioRange(numerator.name, denominator.name, typical.start, typical.endInclusive)
}
