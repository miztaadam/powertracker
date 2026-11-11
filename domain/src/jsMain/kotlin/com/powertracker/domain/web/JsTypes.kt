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
import com.powertracker.domain.coach.AccessoryOption
import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.progress.Pr
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// Plain data only crosses the JS boundary: Int ids, Double kilograms, ISO date strings, weekdays 1-7
// (Monday = 1), enums as their Kotlin names, and advice payloads as the Android AdvicePayload JSON.

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
)

@JsExport
data class JsHistory(
    val sessions: Array<JsSession>,
    val exercises: Array<JsExercise>,
    val baselines: Array<JsBaseline>,
)

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
)

internal fun JsPlanItem.toDomain() = PlanItem(
    id.toLong(), parseWeekday("plan.weekday", weekday), exerciseId.toLong(), sets, reps, targetRpe, order,
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
    )
}

internal fun Advice.toJs() = JsAdvice(
    id.toFacadeId("advice.id"), type.name, lift?.name, createdAt.toString(), reason, severity, status.name,
    androidJson.encodeToString(AdvicePayload.serializer(), payload),
)

internal fun Pr.toJs() = JsPr(date.toString(), e1rmKg, weightKg, reps, rpe)

internal fun AccessoryOption.toJs() = JsAccessoryOption(exerciseName, heavy)
