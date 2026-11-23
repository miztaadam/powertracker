@file:OptIn(ExperimentalJsExport::class)

package com.powertracker.domain.web

import com.powertracker.domain.guide.GuideEditor
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideLine
import com.powertracker.domain.guide.GuideProgress
import com.powertracker.domain.guide.GuideScreen
import com.powertracker.domain.guide.GuideSideFrame
import com.powertracker.domain.guide.GuideSwap
import com.powertracker.domain.guide.GuideTargetStep
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.TrainingHistory
import kotlinx.datetime.isoDayNumber

/*
 * The guide (guide/Guide.kt, guide/GuideSample.kt) for the web: plain data, the same fields as the domain's types.
 * Dates are ISO strings; screen ids, steps and muscle groups are their Kotlin enum names; an exercise id in the
 * sample's steps and bests is the exercise's catalogue name.
 */

/** One sentence of a screen and the names of the code constants its numbers come from. */
@JsExport
data class JsGuideLine(val text: String, val constants: Array<String>)

/** One screen's copy for one side; [example] is true when its picture shows sample numbers. */
@JsExport
data class JsGuideScreen(val id: String, val title: String, val lines: Array<JsGuideLine>, val example: Boolean)

/** One session of the sample lift and the coach's next target ([GuideTargetStep]). */
@JsExport
data class JsGuideTargetStep(
    val date: String,
    val exerciseId: String,
    val doneWeightKg: Double,
    val doneReps: Int,
    val doneRpe: Double,
    val nextWeightKg: Double,
    val nextRepsLow: Int,
    val nextRepsHigh: Int,
    val note: String?,
)

/** One point of the months-later chart. */
@JsExport
data class JsGuidePoint(val date: String, val value: Double)

/** One exercise's best, kg. */
@JsExport
data class JsGuideBest(val exerciseId: String, val kg: Double)

/** The sample lifter months on ([GuideProgress]). */
@JsExport
data class JsGuideProgress(val points: Array<JsGuidePoint>, val bests: Array<JsGuideBest>, val totalKg: Double?, val laggingMuscle: String?)

/** Where a picture is at one moment ([GuideFrame]); [step] is the `GuideStep` name, [tap] the `GuideTap` name or null. */
@JsExport
data class JsGuideFrame(val stepIndex: Int, val step: String, val progress: Double, val done: Boolean, val tap: String?, val tapProgress: Double)

/** What screen 4's lift editor shows ([GuideEditor]); [weightKg] null while no weight is in. */
@JsExport
data class JsGuideEditor(val weightKg: Double?, val reps: Int, val rpe: Double)

internal fun GuideFrame.toJs() = JsGuideFrame(stepIndex, step.name, progress, done, tap?.name, tapProgress)

internal fun GuideEditor.toJs() = JsGuideEditor(weightKg, reps, rpe)

internal fun GuideLine.toJs() = JsGuideLine(text, constants.toTypedArray())

internal fun GuideScreen.toJs() = JsGuideScreen(id.name, title, lines.map { it.toJs() }.toTypedArray(), example)

internal fun GuideTargetStep.toJs() = JsGuideTargetStep(
    date.toString(), exerciseId, doneWeightKg, doneReps, doneRpe, nextWeightKg, nextRepsLow, nextRepsHigh, note,
)

internal fun GuideProgress.toJs() = JsGuideProgress(
    points.map { (date, value) -> JsGuidePoint(date.toString(), value) }.toTypedArray(),
    bests.map { (id, kg) -> JsGuideBest(id, kg) }.toTypedArray(),
    totalKg,
    laggingMuscle,
)

/** Screen 2's two motions at one moment of their loop ([GuideSideFrame]), in the mode switch's own time, ms. */
@JsExport
data class JsGuideSideFrame(val dropMs: Double, val pumpMs: Double)

/** Each of screen 2's motion boxes, in the mode switch's units, and where the pump starts in it. */
@JsExport
data class JsGuideSideBox(val w: Double, val h: Double, val srcX: Double, val srcY: Double)

/** Screen 3's one swap ([GuideSwap]): on [day] (1-7, Monday = 1) the draft's [from] becomes [to]. */
@JsExport
data class JsGuideSwap(val day: Int, val from: String, val to: String)

internal fun GuideSideFrame.toJs() = JsGuideSideFrame(dropMs, pumpMs)

internal fun GuideSwap.toJs() = JsGuideSwap(day.isoDayNumber, from, to)

internal fun Baseline.toJs() = JsBaseline(exerciseId.toFacadeId("baseline.exerciseId"), e1rmKg, date.toString())

internal fun LoggedSession.toJs() = JsSession(
    id.toFacadeId("session.id"), date.toString(), bodyweightKg, readiness, sets.map { it.toJs() }.toTypedArray(), mode.name,
)

/** A guide sample history for the web, in the shape the web's stores give the real screens' builders. */
internal fun TrainingHistory.toJs() = JsHistory(
    sessions.map { it.toJs() }.toTypedArray(),
    exercises.values.sortedBy { it.id }.map { it.toJs() }.toTypedArray(),
    baselines.map { it.toJs() }.toTypedArray(),
)

internal fun CheckIn.toJs() = JsCheckIn(
    date.toString(),
    lagging.map { it.name }.sorted().toTypedArray(),
    growingWell.map { it.name }.sorted().toTypedArray(),
    id.toFacadeId("checkIn.id"),
)
