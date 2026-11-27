@file:OptIn(ExperimentalJsExport::class)

package com.powertracker.domain.web

import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.coach.AccessoryOption
import com.powertracker.domain.coach.CheckInOutcome
import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.coach.PlanChange
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.progress.MuscleRow
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.figures.Capsule
import com.powertracker.domain.library.figures.Circle
import com.powertracker.domain.library.figures.Polygon
import com.powertracker.domain.library.figures.RoundRect
import com.powertracker.domain.library.figures.Shape
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.plan.BuilderPick
import com.powertracker.domain.plan.DraftExercise
import com.powertracker.domain.plan.DraftSession
import com.powertracker.domain.plan.GroupVolume
import com.powertracker.domain.plan.LeftOut
import com.powertracker.domain.plan.PlanBuilder
import com.powertracker.domain.plan.PlanDraft
import com.powertracker.domain.plan.PlanTemplate
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.domain.progress.Pr
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// Plain data only crosses the JS boundary: Int ids, Double kilograms, ISO date strings, weekdays 1-7
// (Monday = 1), enums as their Kotlin names, and advice payloads as the Android AdvicePayload JSON. A `mode` is a
// `TrainingMode` name ("POWERLIFTING" or "BODYBUILDING"); it may be left out, and then means POWERLIFTING.

@JsExport
data class JsExercise(
    val id: Int,
    val name: String,
    val category: String,
    val parentLift: String?,
    val muscles: Array<String>,
    val isBuiltIn: Boolean,
    val archived: Boolean,
    /** A `PlateCount` name: "OFF", "WHOLE" or "HALF"; left out, it is "OFF". */
    val plateCount: String = "OFF",
)

@JsExport
data class JsSet(
    val exerciseId: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val failed: Boolean,
    val missPoint: String?,
    val isTopSet: Boolean,
    /** A `LoadUnit` name: "KG", or "PLATES" when [weightKg] is a plate number; left out, it is "KG". */
    val unit: String = "KG",
)

@JsExport
data class JsSession(
    val id: Int,
    val date: String,
    val bodyweightKg: Double?,
    val readiness: Int?,
    val sets: Array<JsSet>,
    val mode: String = "POWERLIFTING",
)

@JsExport
data class JsPlanItem(
    val id: Int,
    val weekday: Int,
    val exerciseId: Int,
    val sets: Int,
    val reps: Int,
    val targetRpe: Double,
    val order: Int,
    val mode: String = "POWERLIFTING",
    /** The top of a rep range, with [reps] its low end; null means fixed reps. */
    val repsHigh: Int? = null,
)

@JsExport
data class JsBaseline(val exerciseId: Int, val e1rmKg: Double, val date: String)

@JsExport
data class JsAdvice(
    val id: Int,
    val type: String,
    val lift: String?,
    val createdAt: String,
    val reason: String,
    val severity: Int,
    val status: String,
    val payloadJson: String,
    val mode: String = "POWERLIFTING",
)

/**
 * A skipped planned exercise: [date] ISO, [mode] a `TrainingMode` name, [planItemId] null once its plan item is
 * deleted, [reason] a `SkipReason` name ("BUSY", "PAIN", "TIME") or null for no reason.
 */
@JsExport
data class JsSkip(
    val id: Int,
    val date: String,
    val mode: String,
    val exerciseId: Int,
    val planItemId: Int?,
    val reason: String?,
)

@JsExport
data class JsHistory(
    val sessions: Array<JsSession>,
    val exercises: Array<JsExercise>,
    val baselines: Array<JsBaseline>,
)

/** An exercise's sets, in logged order, in its last session before a day: the Today card's "Last time". [date] is ISO. */
@JsExport
data class JsLastTime(val exerciseId: Int, val date: String, val sets: Array<JsSet>)

/**
 * One warm-up set to tick off: [weight] in [unit] ("KG", or "PLATES" for a plate number) for [reps]. [perSide] is the
 * plates on each side of the bar, there only for a barbell lift the plates can make (empty: the bar alone).
 */
@JsExport
data class JsWarmUpSet(val weight: Double, val reps: Int, val unit: String, val perSide: Array<Double>?)

/** One session's best set on the bodybuilding Progress screen. */
@JsExport
data class JsBestSet(val date: String, val set: JsSet)

/** A bodybuilding Progress row; [goingUp] is null with fewer than two sessions. */
@JsExport
data class JsExerciseTrend(val exerciseId: Int, val name: String, val bestSets: Array<JsBestSet>, val goingUp: Boolean?)

@JsExport
data class JsCoachSettings(val defaultRpe: Double, val changePlates: Boolean)

@JsExport
data class JsWeeklyPoint(val weekStart: String, val value: Double)

@JsExport
data class JsPr(val date: String, val e1rmKg: Double, val weightKg: Double, val reps: Int, val rpe: Double?)

@JsExport
data class JsAccessoryOption(val name: String, val heavy: Boolean)

/** The typical range of one lift as a share of another: [low]..[high] are factors, e.g. 0.8..0.9. */
@JsExport
data class JsRatioRange(val numerator: String, val denominator: String, val low: Double, val high: Double)

/** An accessory and its dose, as the coach prescribes it. */
@JsExport
data class JsPrescription(val exerciseName: String, val sets: Int, val repsLow: Int, val repsHigh: Int)

/** One more set for the plan item on [weekday] (1-7) holding [exerciseId]: [fromSets] to [toSets]. */
@JsExport
data class JsSetAddition(val weekday: Int, val exerciseId: Int, val name: String, val fromSets: Int, val toSets: Int)

/** A catalogue exercise ([name]) to add at the end of [weekday] (1-7), [sets] x [repsLow]-[repsHigh] at [rpe]. */
@JsExport
data class JsNewExercise(val weekday: Int, val name: String, val sets: Int, val repsLow: Int, val repsHigh: Int, val rpe: Double)

/**
 * A decoded `AdvicePayload`. [kind] is "Target", "Accessories", "Deload", "WeakPoint", "ReEntry", "MoreWork",
 * "Swap" or "Skipped"; only the fields of that kind are set, with Android's defaults where the JSON leaves one out.
 * MoreWork: [group], [addSets] or [newExercise], [weeklyBefore], [weeklyAfter]. Swap: [exerciseId], [name], [group],
 * [since] (ISO date), [alternatives]. Skipped: [skipId], [exerciseId], [name], [skipReason] (a `SkipReason` name, or
 * null for no reason), [alternatives] (empty for a main lift, or when only the hold is said).
 */
@JsExport
data class JsAdvicePayload(
    val kind: String,
    val exerciseId: Int? = null,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rpe: Double? = null,
    /** Target: the coach's set count, or null to keep the plan's. */
    val sets: Int? = null,
    val items: Array<JsPrescription> = emptyArray(),
    val volumeFactor: Double? = null,
    val loadFactor: Double? = null,
    val lift: String? = null,
    val missPoint: String? = null,
    /** Target: the top of the rep range, or null for fixed reps. */
    val repsHigh: Int? = null,
    /** MoreWork and Swap: a `MuscleGroup` name. */
    val group: String? = null,
    val addSets: Array<JsSetAddition> = emptyArray(),
    val newExercise: JsNewExercise? = null,
    val weeklyBefore: Int? = null,
    val weeklyAfter: Int? = null,
    /** Swap: the stuck exercise's name. */
    val name: String? = null,
    val since: String? = null,
    val alternatives: Array<String> = emptyArray(),
    /** Skipped: the skip the card answers. */
    val skipId: Int? = null,
    /** Skipped: a `SkipReason` name, or null when the skip gave no reason. */
    val skipReason: String? = null,
    /** Target: what [weightKg] means, a `LoadUnit` name ("KG", also for every payload stored before 1.17, or "PLATES"). */
    val unit: String? = null,
)

/**
 * A check-in: [date] ISO, [lagging] and [growingWell] disjoint `MuscleGroup` names (every other group is on track);
 * [id] 0 for one not yet stored.
 */
@JsExport
data class JsCheckIn(val date: String, val lagging: Array<String>, val growingWell: Array<String>, val id: Int = 0)

/** A lagging [group] that got no advice: [reason] a `NoteReason` name (AT_CEILING, NO_EXERCISE, FULL, LATER, WOULD_PASS_CEILING). */
@JsExport
data class JsGroupNote(val group: String, val reason: String, val weeklySets: Int)

/** What saving a check-in gives: new MORE_WORK [advice], [notes], and the OPEN MORE_WORK advice now DISMISSED. */
@JsExport
data class JsCheckInOutcome(val advice: Array<JsAdvice>, val notes: Array<JsGroupNote>, val dismissed: Array<JsAdvice>)

/** A plan item to insert (id 0, exerciseId 0) and the catalogue [name] to resolve for it. */
@JsExport
data class JsAddedPlanItem(val item: JsPlanItem, val name: String)

/**
 * A change to the bodybuilding plan, for one transaction: save [updated], delete [removed], insert [added] after
 * resolving each catalogue name. [changed] is false when nothing applied; [message] says what happened.
 */
@JsExport
data class JsPlanChange(
    val updated: Array<JsPlanItem>,
    val removed: Array<JsPlanItem>,
    val added: Array<JsAddedPlanItem>,
    val message: String,
    val changed: Boolean,
)

/** A body part's rating at one check-in: [rating] a `MuscleRating` name. */
@JsExport
data class JsRating(val date: String, val rating: String)

/** One body part on the bodybuilding Progress screen; [group] a `MuscleGroup` name, [ratings] oldest first. */
@JsExport
data class JsMuscleRow(
    val group: String,
    val ratings: Array<JsRating>,
    val setsDone: Int,
    val setsPlanned: Int,
    val exercisesUp: Int,
    val exercisesTracked: Int,
)

/** A form guide: [purpose] is one or two sentences; the UI appends `GritDomain.safetyLine()` after [mistakes]. */
@JsExport
data class JsFormGuide(
    val purpose: String,
    val setup: Array<String>,
    val steps: Array<String>,
    val cues: Array<String>,
    val mistakes: Array<String>,
)

/**
 * An exercise's stretch and squeeze: exactly one of [none] (the fixed line for a hold or a carry) or the pair
 * [stretch] and [squeeze] is set.
 */
@JsExport
data class JsMuscleFocus(val stretch: String?, val squeeze: String?, val none: String?)

/** The RPE sheet's heading and its lines, in order. Unlike a form guide it takes no safety line. */
@JsExport
data class JsRpeGuide(val title: String, val lines: Array<String>)

/**
 * A catalogue exercise. [area] is a `BodyArea` name, [equipment] `Equipment` names (first = primary), [kind]
 * "COMPOUND" or "ACCESSORY", [category] and [parentLift] as on [JsExercise].
 */
@JsExport
data class JsLibraryExercise(
    val name: String,
    val aliases: Array<String>,
    val area: String,
    val equipment: Array<String>,
    val muscles: Array<String>,
    val kind: String,
    val category: String,
    val parentLift: String?,
    val guide: JsFormGuide,
)

/**
 * One exercise the lifter ticked in the plan builder: [muscles] as on the exercise, [compound] from the catalogue's
 * kind (or an own exercise's category), [equipment] `Equipment` names (empty for an own exercise).
 */
@JsExport
data class JsBuilderPick(
    val name: String,
    val muscles: Array<String>,
    val compound: Boolean,
    val equipment: Array<String>,
    val inCatalogue: Boolean,
)

/**
 * One exercise of a draft session: [mainGroup] a `MuscleGroup` name or null; [muscles] and [compound] for recounting;
 * [slot] the `PlanBuilder.SlotKind` name of the template slot it fills, null for a held-over pick; [lead] true for the
 * session's first template slot.
 */
@JsExport
data class JsDraftExercise(
    val name: String,
    val inCatalogue: Boolean,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val rpe: Double,
    val mainGroup: String?,
    val muscles: Array<String>,
    val compound: Boolean,
    val slot: String?,
    val lead: Boolean,
)

/** One day of a draft: [day] a weekday 1-7 (Monday = 1), [title] "Full body", "Upper" or "Lower". */
@JsExport
data class JsDraftSession(val day: Int, val title: String, val exercises: Array<JsDraftExercise>)

/** A group's week in a draft (the 12 target groups): [group] a `MuscleGroup` name; [short] when [weeklySets] is under 10. */
@JsExport
data class JsGroupVolume(val group: String, val weeklySets: Int, val days: Int, val short: Boolean)

/** A pick the draft left out, and why. */
@JsExport
data class JsLeftOut(val name: String, val reason: String)

/** A plan builder draft. Hand it back unchanged to `planBuilderAlternatives` and `planBuilderSwap`. */
@JsExport
data class JsPlanDraft(val sessions: Array<JsDraftSession>, val volume: Array<JsGroupVolume>, val leftOut: Array<JsLeftOut>)

/**
 * One exercise of a plan template day: [repsHigh] null for fixed reps (every powerlifting entry); [onlyWithStrictPress]
 * true for an entry that appears only when strict press is on.
 */
@JsExport
data class JsTemplateExercise(
    val name: String,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int?,
    val rpe: Double,
    val onlyWithStrictPress: Boolean,
)

/** One day of a plan template: its [title] and its exercises in order. */
@JsExport
data class JsTemplateSession(val title: String, val exercises: Array<JsTemplateExercise>)

/** A ready-made week: [mode] a `TrainingMode` name; [dayCount] how many weekdays it takes; [suits] who it is for. */
@JsExport
data class JsPlanTemplate(
    val id: String,
    val mode: String,
    val name: String,
    val suits: String,
    val dayCount: Int,
    val sessions: Array<JsTemplateSession>,
)

// ---- converters (internal: not part of the JS API) ----

private val androidJson = Json

internal fun Long.toFacadeId(field: String): Int {
    require(this in Int.MIN_VALUE..Int.MAX_VALUE) { "$field: id $this does not fit in an Int" }
    return toInt()
}

internal fun parseDate(field: String, text: String): LocalDate =
    try {
        LocalDate.parse(text)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("$field: '$text' is not an ISO date (YYYY-MM-DD)", e)
    }

internal fun parseWeekday(field: String, number: Int): DayOfWeek {
    require(number in 1..7) { "$field: weekday $number is not between 1 (Monday) and 7 (Sunday)" }
    return DayOfWeek(number)
}

internal inline fun <reified E : Enum<E>> parseEnum(field: String, text: String): E =
    enumValues<E>().firstOrNull { it.name == text }
        ?: throw IllegalArgumentException("$field: '$text' is not one of ${enumValues<E>().joinToString { it.name }}")

internal fun parseLift(field: String, text: String): MainLift = parseEnum<MainLift>(field, text)

internal fun JsExercise.toDomain() = Exercise(
    id.toLong(), name, parseEnum<Category>("exercise.category", category),
    parentLift?.let { parseLift("exercise.parentLift", it) }, muscles.toList(), isBuiltIn, archived,
    parseEnum<PlateCount>("exercise.plateCount", plateCount),
)

internal fun Exercise.toJs() = JsExercise(
    id.toFacadeId("exercise.id"), name, category.name, parentLift?.name, muscles.toTypedArray(), isBuiltIn, archived,
    plateCount.name,
)

internal fun JsSet.toDomain() = LoggedSet(
    exerciseId.toLong(), weightKg, reps, rpe, failed,
    missPoint?.let { parseEnum<MissPoint>("set.missPoint", it) }, isTopSet, parseEnum<LoadUnit>("set.unit", unit),
)

internal fun JsSession.toDomain() = LoggedSession(
    id.toLong(), parseDate("session.date", date), bodyweightKg, readiness, sets.map { it.toDomain() },
    parseEnum<TrainingMode>("session.mode", mode),
)

internal fun JsPlanItem.toDomain() = PlanItem(
    id.toLong(), parseWeekday("plan.weekday", weekday), exerciseId.toLong(), sets, reps, targetRpe, order,
    parseEnum<TrainingMode>("plan.mode", mode), repsHigh,
)

internal fun PlanItem.toJs() = JsPlanItem(
    id.toFacadeId("plan.id"), weekday.isoDayNumber, exerciseId.toFacadeId("plan.exerciseId"), sets, reps, targetRpe, order,
    mode.name, repsHigh,
)

internal fun JsCheckIn.toDomain(): CheckIn {
    val lagging = lagging.map { parseEnum<MuscleGroup>("checkIn.lagging", it) }.toSet()
    val growing = growingWell.map { parseEnum<MuscleGroup>("checkIn.growingWell", it) }.toSet()
    require(lagging.none { it in growing }) { "checkIn: ${lagging.filter { it in growing }} both lagging and growing well" }
    return CheckIn(parseDate("checkIn.date", date), lagging, growing, id.toLong())
}

internal fun CheckInOutcome.toJs() = JsCheckInOutcome(
    advice.map { it.toJs() }.toTypedArray(),
    notes.map { JsGroupNote(it.group.name, it.reason.name, it.weeklySets) }.toTypedArray(),
    dismissed.map { it.toJs() }.toTypedArray(),
)

internal fun PlanChange.toJs() = JsPlanChange(
    updated.map { it.toJs() }.toTypedArray(),
    removed.map { it.toJs() }.toTypedArray(),
    added.map { (item, name) -> JsAddedPlanItem(item.toJs(), name) }.toTypedArray(),
    message,
    changed,
)

internal fun MuscleRow.toJs() = JsMuscleRow(
    group.name, ratings.map { (date, rating) -> JsRating(date.toString(), rating.name) }.toTypedArray(),
    setsDone, setsPlanned, exercisesUp, exercisesTracked,
)

/** The stored payload [payloadJson], which must be an `AdvicePayload` of type [T]. */
internal inline fun <reified T : AdvicePayload> payloadOf(field: String, payloadJson: String): T {
    val payload = try {
        androidJson.decodeFromString(AdvicePayload.serializer(), payloadJson)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("$field: not an AdvicePayload (${e.message})", e)
    }
    return payload as? T ?: throw IllegalArgumentException("$field: a ${payload::class.simpleName}, not a ${T::class.simpleName}")
}

internal fun JsSkip.toDomain() = Skip(
    id.toLong(), parseDate("skip.date", date), parseEnum<TrainingMode>("skip.mode", mode), exerciseId.toLong(),
    planItemId?.toLong(), reason?.let { parseEnum<SkipReason>("skip.reason", it) },
)

internal fun JsBaseline.toDomain() = Baseline(exerciseId.toLong(), e1rmKg, parseDate("baseline.date", date))

internal fun JsHistory.toDomain() = TrainingHistory(
    sessions.map { it.toDomain() },
    exercises.map { it.toDomain() }.associateBy { it.id },
    baselines.map { it.toDomain() },
)

internal fun JsCoachSettings.toDomain() = CoachSettings(defaultRpe = defaultRpe, changePlates = changePlates)

internal fun JsAdvice.toDomain(): Advice {
    val payload = try {
        androidJson.decodeFromString(AdvicePayload.serializer(), payloadJson)
    } catch (e: SerializationException) {
        throw IllegalArgumentException("advice.payloadJson: not an AdvicePayload (${e.message})", e)
    }
    return Advice(
        parseEnum<AdviceType>("advice.type", type),
        lift?.let { parseLift("advice.lift", it) },
        parseDate("advice.createdAt", createdAt),
        reason,
        severity,
        payload,
        parseEnum<AdviceStatus>("advice.status", status),
        id.toLong(),
        parseEnum<TrainingMode>("advice.mode", mode),
    )
}

internal fun Advice.toJs() = JsAdvice(
    id.toFacadeId("advice.id"), type.name, lift?.name, createdAt.toString(), reason, severity, status.name,
    androidJson.encodeToString(AdvicePayload.serializer(), payload), mode.name,
)

internal fun ExerciseTrend.toJs() = JsExerciseTrend(
    exerciseId.toFacadeId("trend.exerciseId"),
    name,
    bestSets.map { (date, set) -> JsBestSet(date.toString(), set.toJs()) }.toTypedArray(),
    goingUp,
)

internal fun JsBuilderPick.toDomain() = BuilderPick(
    name, muscles.toList(), compound, equipment.map { parseEnum<Equipment>("pick.equipment", it) }, inCatalogue,
)

internal fun PlanDraft.toJs() = JsPlanDraft(
    sessions.map { it.toJs() }.toTypedArray(),
    volume.map { JsGroupVolume(it.group.name, it.weeklySets, it.days, it.short) }.toTypedArray(),
    leftOut.map { JsLeftOut(it.name, it.reason) }.toTypedArray(),
)

internal fun DraftSession.toJs() = JsDraftSession(day.isoDayNumber, title, exercises.map { it.toJs() }.toTypedArray())

internal fun DraftExercise.toJs() = JsDraftExercise(
    name, inCatalogue, sets, repsLow, repsHigh, rpe, mainGroup?.name, muscles.toTypedArray(), compound, slot?.name, lead,
)

internal fun JsPlanDraft.toDomain() = PlanDraft(
    sessions.map { s ->
        DraftSession(
            parseWeekday("draft.day", s.day), s.title,
            s.exercises.map { e ->
                DraftExercise(
                    e.name, e.inCatalogue, e.sets, e.repsLow, e.repsHigh, e.rpe,
                    e.mainGroup?.let { parseEnum<MuscleGroup>("draft.mainGroup", it) }, e.muscles.toList(), e.compound,
                    e.slot?.let { parseEnum<PlanBuilder.SlotKind>("draft.slot", it) }, e.lead,
                )
            },
        )
    },
    volume.map { GroupVolume(parseEnum<MuscleGroup>("draft.volume.group", it.group), it.weeklySets, it.days, it.short) },
    leftOut.map { LeftOut(it.name, it.reason) },
)

internal fun PlanTemplate.toJs() = JsPlanTemplate(
    id, mode.name, name, suits, dayCount,
    sessions.map { s ->
        JsTemplateSession(
            s.title,
            s.exercises.map { JsTemplateExercise(it.name, it.sets, it.repsLow, it.repsHigh, it.rpe, it.onlyWithStrictPress) }.toTypedArray(),
        )
    }.toTypedArray(),
)

internal fun Pr.toJs() = JsPr(date.toString(), e1rmKg, weightKg, reps, rpe)

internal fun AccessoryOption.toJs() = JsAccessoryOption(exerciseName, heavy)

internal fun LoggedSet.toJs() = JsSet(exerciseId.toFacadeId("set.exerciseId"), weightKg, reps, rpe, failed, missPoint?.name, isTopSet, unit.name)

/** Null when [payloadJson] is not an `AdvicePayload` as Android stores it. */
internal fun decodePayload(payloadJson: String): JsAdvicePayload? {
    val payload = try {
        androidJson.decodeFromString(AdvicePayload.serializer(), payloadJson)
    } catch (e: IllegalArgumentException) {
        // SerializationException is an IllegalArgumentException.
        return null
    }
    return when (payload) {
        is AdvicePayload.Target -> JsAdvicePayload(
            "Target", exerciseId = payload.exerciseId.toFacadeId("payload.exerciseId"), weightKg = payload.weightKg,
            reps = payload.reps, rpe = payload.rpe, sets = payload.sets, repsHigh = payload.repsHigh, unit = payload.unit.name,
        )
        is AdvicePayload.Accessories -> JsAdvicePayload(
            "Accessories",
            items = payload.items.map { JsPrescription(it.exerciseName, it.sets, it.repsLow, it.repsHigh) }.toTypedArray(),
        )
        is AdvicePayload.Deload -> JsAdvicePayload("Deload", volumeFactor = payload.volumeFactor, loadFactor = payload.loadFactor)
        is AdvicePayload.WeakPoint -> JsAdvicePayload("WeakPoint", lift = payload.lift.name, missPoint = payload.missPoint?.name)
        is AdvicePayload.ReEntry -> JsAdvicePayload("ReEntry", loadFactor = payload.loadFactor)
        is AdvicePayload.MoreWork -> JsAdvicePayload(
            "MoreWork", group = payload.group.name,
            addSets = payload.addSets.map {
                JsSetAddition(it.weekday.isoDayNumber, it.exerciseId.toFacadeId("payload.addSets.exerciseId"), it.name, it.fromSets, it.toSets)
            }.toTypedArray(),
            newExercise = payload.newExercise?.let { JsNewExercise(it.weekday.isoDayNumber, it.name, it.sets, it.repsLow, it.repsHigh, it.rpe) },
            weeklyBefore = payload.weeklyBefore, weeklyAfter = payload.weeklyAfter,
        )
        is AdvicePayload.Swap -> JsAdvicePayload(
            "Swap", exerciseId = payload.exerciseId.toFacadeId("payload.exerciseId"), group = payload.group.name,
            name = payload.name, since = payload.since.toString(), alternatives = payload.alternatives.toTypedArray(),
        )
        is AdvicePayload.Skipped -> JsAdvicePayload(
            "Skipped", exerciseId = payload.exerciseId.toFacadeId("payload.exerciseId"), name = payload.name,
            alternatives = payload.alternatives.toTypedArray(), skipId = payload.skipId.toFacadeId("payload.skipId"),
            skipReason = payload.reason?.name,
        )
    }
}

internal fun FormGuide.toJs() = JsFormGuide(
    purpose, setup.toTypedArray(), steps.toTypedArray(), cues.toTypedArray(), mistakes.toTypedArray(),
)

internal fun LibraryExercise.toJs() = JsLibraryExercise(
    name, aliases.toTypedArray(), area.name, equipment.map { it.name }.toTypedArray(), muscles.toTypedArray(),
    kind.name, category.name, parentLift?.name, guide.toJs(),
)

/** A figure's captions, which pose the thumbnail shows ("start" or "end"), and its view box [width, height]. */
@JsExport
data class JsFigureInfo(val startCaption: String, val endCaption: String, val thumbnail: String, val viewBox: Array<Double>)

/**
 * One drawing primitive in view-box units (y down). [kind] is "circle", "capsule", "rect" or "polygon"; [role] a
 * `Role` name. The number fields use the domain's property names.
 */
@JsExport
sealed class JsFigureShape(val kind: String, val role: String)

@JsExport
class JsFigureCircle(role: String, val cx: Double, val cy: Double, val r: Double) : JsFigureShape("circle", role)

@JsExport
class JsFigureCapsule(role: String, val x1: Double, val y1: Double, val x2: Double, val y2: Double, val width: Double) :
    JsFigureShape("capsule", role)

@JsExport
class JsFigureRect(role: String, val x: Double, val y: Double, val w: Double, val h: Double, val radius: Double) :
    JsFigureShape("rect", role)

/** [points] are x,y pairs, filled with the non-zero rule. */
@JsExport
class JsFigurePolygon(role: String, val points: Array<Double>) : JsFigureShape("polygon", role)

internal fun Shape.toJs(): JsFigureShape = when (this) {
    is Circle -> JsFigureCircle(role.name, cx, cy, r)
    is Capsule -> JsFigureCapsule(role.name, x1, y1, x2, y2, width)
    is RoundRect -> JsFigureRect(role.name, x, y, w, h, radius)
    is Polygon -> JsFigurePolygon(role.name, points.toTypedArray())
}
