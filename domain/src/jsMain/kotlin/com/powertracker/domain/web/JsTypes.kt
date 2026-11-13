@file:OptIn(ExperimentalJsExport::class)

package com.powertracker.domain.web

import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.coach.AccessoryOption
import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.figures.Capsule
import com.powertracker.domain.library.figures.Circle
import com.powertracker.domain.library.figures.Polygon
import com.powertracker.domain.library.figures.RoundRect
import com.powertracker.domain.library.figures.Shape
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.domain.progress.Pr
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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

@JsExport
data class JsHistory(
    val sessions: Array<JsSession>,
    val exercises: Array<JsExercise>,
    val baselines: Array<JsBaseline>,
)

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

/**
 * A decoded `AdvicePayload`. [kind] is "Target", "Accessories", "Deload", "WeakPoint" or "ReEntry"; only the
 * fields of that kind are set, with Android's defaults where the JSON leaves one out.
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
)

internal fun Exercise.toJs() = JsExercise(
    id.toFacadeId("exercise.id"), name, category.name, parentLift?.name, muscles.toTypedArray(), isBuiltIn, archived,
)

internal fun JsSet.toDomain() = LoggedSet(
    exerciseId.toLong(), weightKg, reps, rpe, failed,
    missPoint?.let { parseEnum<MissPoint>("set.missPoint", it) }, isTopSet,
)

internal fun JsSession.toDomain() = LoggedSession(
    id.toLong(), parseDate("session.date", date), bodyweightKg, readiness, sets.map { it.toDomain() },
    parseEnum<TrainingMode>("session.mode", mode),
)

internal fun JsPlanItem.toDomain() = PlanItem(
    id.toLong(), parseWeekday("plan.weekday", weekday), exerciseId.toLong(), sets, reps, targetRpe, order,
    parseEnum<TrainingMode>("plan.mode", mode), repsHigh,
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

internal fun Pr.toJs() = JsPr(date.toString(), e1rmKg, weightKg, reps, rpe)

internal fun AccessoryOption.toJs() = JsAccessoryOption(exerciseName, heavy)

internal fun LoggedSet.toJs() = JsSet(exerciseId.toFacadeId("set.exerciseId"), weightKg, reps, rpe, failed, missPoint?.name, isTopSet)

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
            reps = payload.reps, rpe = payload.rpe, sets = payload.sets, repsHigh = payload.repsHigh,
        )
        is AdvicePayload.Accessories -> JsAdvicePayload(
            "Accessories",
            items = payload.items.map { JsPrescription(it.exerciseName, it.sets, it.repsLow, it.repsHigh) }.toTypedArray(),
        )
        is AdvicePayload.Deload -> JsAdvicePayload("Deload", volumeFactor = payload.volumeFactor, loadFactor = payload.loadFactor)
        is AdvicePayload.WeakPoint -> JsAdvicePayload("WeakPoint", lift = payload.lift.name, missPoint = payload.missPoint?.name)
        is AdvicePayload.ReEntry -> JsAdvicePayload("ReEntry", loadFactor = payload.loadFactor)
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
