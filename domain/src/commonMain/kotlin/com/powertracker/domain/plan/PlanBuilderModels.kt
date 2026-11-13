package com.powertracker.domain.plan

import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.MuscleGroup
import kotlinx.datetime.DayOfWeek

/**
 * One exercise the lifter does now (or would like to do), as the builder's exercise step hands it over. [muscles] are
 * display strings as on `Exercise.muscles` (a lifter's own exercise may have none); [compound] is the catalogue's kind,
 * or for an own exercise what its stored category says; [equipment] is empty for an own exercise.
 */
data class BuilderPick(
    val name: String,
    val muscles: List<String>,
    val compound: Boolean,
    val equipment: List<Equipment>,
    val inCatalogue: Boolean,
)

/**
 * The lifter's answers: 3 or 4 distinct [days]; the [picks] (empty means "choose for me"); up to
 * [PlanBuilder.MAX_PRIORITIES] distinct [priorities], each one of [PlanBuilder.TARGET_GROUPS], in the order chosen.
 * [variant] is "another draft": it rotates the catalogue defaults and never changes the picks.
 */
data class BuilderAnswers(
    val days: List<DayOfWeek>,
    val picks: List<BuilderPick>,
    val priorities: List<MuscleGroup>,
    val variant: Int = 0,
)

/**
 * One exercise of a draft session and its prescription. [mainGroup] is the group of its first-listed muscle, null when
 * that muscle belongs to none. [muscles] and [compound] are carried so the draft can be recounted after a swap.
 * [slot] is the template slot it fills, null for a held-over pick (what Swap offers depends on it); [lead] is true for
 * the exercise in the session's first template slot (it gets [PlanBuilder.LEAD_SETS]).
 */
data class DraftExercise(
    val name: String,
    val inCatalogue: Boolean,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val rpe: Double,
    val mainGroup: MuscleGroup?,
    val muscles: List<String>,
    val compound: Boolean,
    val slot: PlanBuilder.SlotKind?,
    val lead: Boolean,
)

/** One training day of the draft: [title] is "Full body", "Upper" or "Lower"; [exercises] in the order to do them. */
data class DraftSession(val day: DayOfWeek, val title: String, val exercises: List<DraftExercise>)

/**
 * A group's week in the draft: sets counted for every group an exercise works, the number of days it is trained on,
 * and [short] when [weeklySets] is under [PlanBuilder.WEEKLY_SETS].
 */
data class GroupVolume(val group: MuscleGroup, val weeklySets: Int, val days: Int, val short: Boolean)

/** A pick the draft could not place, and why. */
data class LeftOut(val name: String, val reason: String)

/**
 * What the builder made: the [sessions] in week order; [volume] for each target group in [PlanBuilder.TARGET_GROUPS]
 * order; the picks [leftOut].
 */
data class PlanDraft(val sessions: List<DraftSession>, val volume: List<GroupVolume>, val leftOut: List<LeftOut>)
