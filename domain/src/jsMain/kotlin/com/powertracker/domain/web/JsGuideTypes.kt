@file:OptIn(ExperimentalJsExport::class)

package com.powertracker.domain.web

import com.powertracker.domain.guide.GuideEditor
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideLine
import com.powertracker.domain.guide.GuideProgress
import com.powertracker.domain.guide.GuideScreen
import com.powertracker.domain.guide.GuideTargetStep

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
