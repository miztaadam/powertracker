package com.powertracker.domain.coach

import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.BuilderAnswers
import com.powertracker.domain.plan.PlanBuilder
import com.powertracker.domain.plan.PlanDraft
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.THURSDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY

/** Plans made of catalogue exercises, as the apps save a builder draft, for the lagging-muscle coach's tests. */
object PhysiqueFixtures {
    val THREE_DAYS = listOf(MONDAY, WEDNESDAY, FRIDAY)
    val FOUR_DAYS = listOf(MONDAY, TUESDAY, THURSDAY, FRIDAY)

    /** An own exercise (not in the catalogue). */
    const val OWN_ID = 9000L
    val own = Exercise(OWN_ID, "My sled drag", Category.ACCESSORY, null, listOf("Quads", "Glutes"), isBuiltIn = false)

    /** Every catalogue exercise as a stored row, id 1000 + its catalogue position, plus [own]. */
    val exercises: Map<Long, Exercise> = (
        Library.all().mapIndexed { i, e -> Exercise(1000L + i, e.name, e.category, e.parentLift, e.muscles, isBuiltIn = true) } + own
        ).associateBy { it.id }

    private val byName: Map<String, Long> = exercises.values.associate { Library.normalise(it.name) to it.id }

    fun idOf(name: String): Long = byName[Library.normalise(name)] ?: error("no exercise '$name'")

    fun history(sessions: List<com.powertracker.domain.model.LoggedSession> = emptyList()) = TrainingHistory(sessions, exercises, emptyList())

    /** [draft] saved as bodybuilding plan items, ids from 1 in week order. */
    fun planOf(draft: PlanDraft): List<PlanItem> {
        var id = 1L
        return draft.sessions.flatMap { s ->
            s.exercises.mapIndexed { order, e -> PlanItem(id++, s.day, idOf(e.name), e.sets, e.repsLow, e.rpe, order, TrainingMode.BODYBUILDING, e.repsHigh) }
        }
    }

    fun build(days: List<DayOfWeek>, priorities: List<MuscleGroup> = emptyList(), variant: Int = 0): List<PlanItem> =
        planOf(PlanBuilder.build(BuilderAnswers(days, emptyList(), priorities, variant)))

    val default3: List<PlanItem> by lazy { build(THREE_DAYS) }
    val default4: List<PlanItem> by lazy { build(FOUR_DAYS) }

    fun item(id: Long, day: DayOfWeek, name: String, sets: Int, order: Int) =
        PlanItem(id, day, idOf(name), sets, 8, 8.0, order, TrainingMode.BODYBUILDING, 12)

    /** [change] carried out on [plan] as an app would: catalogue names resolved to their rows, new ids after the last. */
    fun apply(change: PlanChange, plan: List<PlanItem>): List<PlanItem> {
        val removed = change.removed.map { it.id }.toSet()
        val updated = change.updated.associateBy { it.id }
        var next = (plan.maxOfOrNull { it.id } ?: 0) + 1
        return plan.filter { it.id !in removed }.map { updated[it.id] ?: it } +
            change.added.map { (item, name) -> item.copy(id = next++, exerciseId = idOf(name)) }
    }

    private val groupsById: Map<Long, Set<MuscleGroup>> = exercises.mapValues { (_, e) -> e.muscles.mapNotNull { MuscleGroups.of(it) }.toSet() }

    /** Every group's weekly sets in one pass; the same count as [PhysiqueCoach.weeklySets] (checked in its tests). */
    fun weekly(plan: List<PlanItem>): Map<MuscleGroup, Int> {
        val out = MuscleGroup.entries.associateWithTo(LinkedHashMap()) { 0 }
        for (item in plan) if (item.mode == TrainingMode.BODYBUILDING) for (g in groupsById[item.exerciseId].orEmpty()) out[g] = out.getValue(g) + item.sets
        return out
    }

    fun describe(plan: List<PlanItem>): String = plan.groupBy { it.weekday }.entries.sortedBy { it.key }.joinToString("\n") { (d, items) ->
        "$d: " + items.sortedBy { it.order }.joinToString(", ") { "${exercises.getValue(it.exerciseId).name} ${it.sets}" }
    }
}
