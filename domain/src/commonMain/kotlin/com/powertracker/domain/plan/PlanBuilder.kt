package com.powertracker.domain.plan

import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * The bodybuilding plan builder (stage 2 plan, "The rules"): a pure function from the lifter's answers to a draft
 * week. The same answers always give the same draft. Sources for its numbers are in `coach/CoachSources.kt`
 * (BUILDER_WEEKLY_SETS, BUILDER_FREQUENCY, BUILDER_ORDER).
 */
object PlanBuilder {
    /** At most this many exercises a session. The designer's number, not sourced (CoachSources.BUILDER_ORDER). */
    const val MAX_EXERCISES = 6

    /** Counted sets a week each target group aims at: ACSM 2026, >= 10 sets per muscle group (CoachSources.BUILDER_WEEKLY_SETS). */
    const val WEEKLY_SETS = 10

    /** Days a week each target group aims to be trained on: ACSM 2026, at least twice (CoachSources.BUILDER_FREQUENCY). */
    const val WEEKLY_DAYS = 2

    /** At most this many priority groups. The designer's number, not sourced. */
    const val MAX_PRIORITIES = 3

    /** The owner's 3 sets of 8-12 at RPE 8. */
    const val SETS = 3

    /** Sets for an exercise whose main group is a priority. The designer's number, not sourced. */
    const val PRIORITY_SETS = 4
    const val REPS_LOW = 8
    const val REPS_HIGH = 12
    const val RPE = 8.0

    /** The reason a pick is left out. */
    const val NO_ROOM = "No room: every session it fits is full."

    const val FULL_BODY = "Full body"
    const val UPPER_TITLE = "Upper"
    const val LOWER_TITLE = "Lower"

    /** The groups the builder aims at, in this order. The other six are worked by the same exercises. */
    val TARGET_GROUPS: List<MuscleGroup> = listOf(
        MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
        MuscleGroup.SIDE_DELTS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.CALVES,
        MuscleGroup.ABS, MuscleGroup.REAR_DELTS,
    )

    /** The groups an upper session allows on 4 days. */
    val UPPER: Set<MuscleGroup> = setOf(
        MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.TRAPS, MuscleGroup.FRONT_DELTS,
        MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS,
    )

    /** The groups a lower session allows on 4 days. */
    val LOWER: Set<MuscleGroup> = setOf(
        MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.ADDUCTORS, MuscleGroup.CALVES,
        MuscleGroup.LOWER_BACK, MuscleGroup.ABS, MuscleGroup.OBLIQUES,
    )

    /** The fixed equipment order for catalogue defaults, after the equipment the picks use. */
    private val EQUIPMENT_ORDER = listOf(
        Equipment.BARBELL, Equipment.DUMBBELL, Equipment.MACHINE, Equipment.CABLE, Equipment.BODYWEIGHT,
        Equipment.SMITH, Equipment.KETTLEBELL, Equipment.BAND, Equipment.SPECIALTY_BAR,
    )

    /** How many days before today a logged exercise is still pre-ticked. */
    private const val SUGGEST_DAYS = 28

    /** The group of the first-listed muscle; null when there is none or it belongs to no group. */
    fun mainGroup(muscles: List<String>): MuscleGroup? = muscles.firstOrNull()?.let(MuscleGroups::of)

    // ---- building ----

    /** An exercise while the draft is built. */
    private class Item(
        val name: String,
        val inCatalogue: Boolean,
        val muscles: List<String>,
        val compound: Boolean,
    ) {
        val key: String = Library.normalise(name)
        val mainGroup: MuscleGroup? = mainGroup(muscles)
    }

    private class Slot(val day: DayOfWeek, val title: String, val allows: Set<MuscleGroup>?) {
        val items = mutableListOf<Item>()
        val hasRoom get() = items.size < MAX_EXERCISES
        fun holds(item: Item) = items.any { it.key == item.key }

        /** Whether [item] belongs here. An exercise with no main group belongs to every kind of session. */
        fun allows(item: Item) = allows == null || item.mainGroup == null || item.mainGroup in allows
    }

    /**
     * The draft week for [answers]. Throws IllegalArgumentException unless there are 3 or 4 distinct days and at
     * most [MAX_PRIORITIES] distinct priorities.
     */
    fun build(answers: BuilderAnswers): PlanDraft {
        val days = answers.days
        require(days.size == 3 || days.size == 4) { "days: ${days.size} days; the builder takes 3 or 4" }
        require(days.toSet().size == days.size) { "days: a weekday is repeated" }
        val priorities = answers.priorities
        require(priorities.size <= MAX_PRIORITIES) { "priorities: ${priorities.size}; at most $MAX_PRIORITIES" }
        require(priorities.toSet().size == priorities.size) { "priorities: a group is repeated" }

        val aim = aimOrder(priorities)
        val slots = sessions(days)
        fun setsFor(item: Item) = if (item.mainGroup in priorities) PRIORITY_SETS else SETS

        // The same pick twice (by name) is one pick.
        val picks = answers.picks.distinctBy { Library.normalise(it.name) }
            .map { Item(it.name, it.inCatalogue, it.muscles, it.compound) }
        val leftOut = mutableListOf<LeftOut>()

        // 1. Place the picks: priority main groups first, then the rest; each part in TARGET_GROUPS order, then name.
        val ordered = picks.sortedWith(
            compareBy<Item>({ if (it.mainGroup in priorities) 0 else 1 }, { targetRank(it.mainGroup) }, { it.name }),
        )
        for (pick in ordered) {
            val open = slots.filter { it.allows(pick) && it.hasRoom }
            val target = fewest(open.filter { s -> pick.mainGroup == null || s.items.none { it.mainGroup == pick.mainGroup } })
                ?: fewest(open)
            if (target == null) leftOut += LeftOut(pick.name, NO_ROOM) else target.items += pick
        }

        // 2. Repeat for frequency: a group on fewer than two days gets its first-placed exercise again.
        // Every exercise in the plan, in the order it was first placed ("the first placed").
        val placedOrder = ordered.filter { p -> slots.any { it.holds(p) } }.toMutableList()
        for (group in aim) {
            if (daysTrained(slots, group) >= WEEKLY_DAYS) continue
            val exercise = placedOrder.firstOrNull { it.mainGroup == group } ?: continue
            val target = fewest(slots.filter { it.allows(exercise) && it.hasRoom && !it.holds(exercise) }) ?: continue
            target.items += exercise
        }

        // 3. Fill what is short, largest shortfall first (aim order on a tie).
        val defaults = HashMap<MuscleGroup, List<LibraryExercise>>()
        val equipmentRank = equipmentRank(answers.picks)
        val markedShort = mutableSetOf<MuscleGroup>()
        while (slots.any { it.hasRoom }) {
            val group = aim
                .filter { it !in markedShort }
                .map { it to WEEKLY_SETS - countedSets(slots, it, ::setsFor) }
                .filter { it.second > 0 }
                .maxByOrNull { it.second } // the first of the largest, so aim order breaks a tie
                ?.first ?: break
            val again = placedOrder.firstOrNull { e ->
                e.mainGroup == group && slots.any { it.allows(e) && it.hasRoom && !it.holds(e) }
            }
            val exercise = again ?: defaults.getOrPut(group) { defaultsFor(group, equipmentRank, answers.variant) }
                .firstOrNull { d -> placedOrder.none { it.key == Library.normalise(d.name) } }
                ?.let { Item(it.name, true, it.muscles, it.kind == Kind.COMPOUND) }
            val target = exercise?.let { e -> fewest(slots.filter { it.allows(e) && it.hasRoom && !it.holds(e) }) }
            if (exercise == null || target == null) {
                markedShort += group
                continue
            }
            target.items += exercise
            if (placedOrder.none { it.key == exercise.key }) placedOrder += exercise
        }

        // 4 and 5. Order each session and prescribe.
        val sessions = slots.map { slot ->
            DraftSession(slot.day, slot.title, order(slot.items.map { it.toDraft(setsFor(it)) }, priorities))
        }
        return PlanDraft(sessions, volume(sessions, volumeGroups(priorities)), leftOut)
    }

    /** The priorities in the lifter's order, then the rest of [TARGET_GROUPS]. A priority outside them is aimed at too. */
    private fun aimOrder(priorities: List<MuscleGroup>): List<MuscleGroup> = priorities + TARGET_GROUPS.filter { it !in priorities }

    /** The groups [PlanDraft.volume] reports: the target groups, then any priority that is not one. */
    private fun volumeGroups(priorities: List<MuscleGroup>): List<MuscleGroup> =
        TARGET_GROUPS + priorities.filter { it !in TARGET_GROUPS }

    private fun sessions(days: List<DayOfWeek>): List<Slot> {
        val week = days.sortedBy { it.ordinal }
        return if (week.size == 3) {
            week.map { Slot(it, FULL_BODY, null) }
        } else {
            week.mapIndexed { i, day -> if (i % 2 == 0) Slot(day, UPPER_TITLE, UPPER) else Slot(day, LOWER_TITLE, LOWER) }
        }
    }

    /** The session with the fewest exercises, the earliest on a tie; null for none. */
    private fun fewest(slots: List<Slot>): Slot? = slots.minByOrNull { it.items.size }

    /** A main group's place in [TARGET_GROUPS]; the other groups after them in the enum's order; no group last. */
    private fun targetRank(group: MuscleGroup?): Int = when {
        group == null -> Int.MAX_VALUE
        group in TARGET_GROUPS -> TARGET_GROUPS.indexOf(group)
        else -> TARGET_GROUPS.size + group.ordinal
    }

    private fun daysTrained(slots: List<Slot>, group: MuscleGroup): Int =
        slots.count { s -> s.items.any { MuscleGroups.works(it.muscles, group) } }

    private fun countedSets(slots: List<Slot>, group: MuscleGroup, setsFor: (Item) -> Int): Int =
        slots.sumOf { s -> s.items.filter { MuscleGroups.works(it.muscles, group) }.sumOf(setsFor) }

    private fun Item.toDraft(sets: Int) = DraftExercise(name, inCatalogue, sets, REPS_LOW, REPS_HIGH, RPE, mainGroup, muscles, compound)

    /** Priority main groups first, then the rest; in each part compound first, then aim order of main group, then name. */
    private fun order(exercises: List<DraftExercise>, priorities: List<MuscleGroup>): List<DraftExercise> {
        val aim = aimOrder(priorities)
        fun aimRank(g: MuscleGroup?) = when {
            g == null -> Int.MAX_VALUE
            g in aim -> aim.indexOf(g)
            else -> aim.size + g.ordinal
        }
        return exercises.sortedWith(
            compareBy({ if (it.mainGroup in priorities) 0 else 1 }, { if (it.compound) 0 else 1 }, { aimRank(it.mainGroup) }, { it.name }),
        )
    }

    private fun volume(sessions: List<DraftSession>, groups: List<MuscleGroup>): List<GroupVolume> = groups.map { group ->
        var sets = 0
        var days = 0
        for (s in sessions) {
            val here = s.exercises.filter { MuscleGroups.works(it.muscles, group) }
            sets += here.sumOf { it.sets }
            if (here.isNotEmpty()) days++
        }
        GroupVolume(group, sets, days, sets < WEEKLY_SETS)
    }

    // ---- catalogue defaults ----

    /**
     * Each equipment's place: those the picks use first, by how many picks use them (most first, the fixed order on a
     * tie), then the rest in the fixed order.
     */
    private fun equipmentRank(picks: List<BuilderPick>): Map<Equipment, Int> {
        val uses = EQUIPMENT_ORDER.associateWith { eq -> picks.count { eq in it.equipment } }
        val ranked = EQUIPMENT_ORDER.filter { uses.getValue(it) > 0 }.sortedByDescending { uses.getValue(it) } +
            EQUIPMENT_ORDER.filter { uses.getValue(it) == 0 }
        return ranked.withIndex().associate { (i, eq) -> eq to i }
    }

    /**
     * The catalogue exercises whose main group is [group], ordered: by their primary equipment's rank, compound
     * before accessory, then catalogue order.
     */
    private fun ordered(group: MuscleGroup, equipmentRank: Map<Equipment, Int>): List<LibraryExercise> =
        Library.all().withIndex()
            .filter { (_, e) -> mainGroup(e.muscles) == group }
            .sortedWith(
                compareBy(
                    { (_, e) -> e.equipment.firstOrNull()?.let { equipmentRank.getValue(it) } ?: Int.MAX_VALUE },
                    { (_, e) -> if (e.kind == Kind.COMPOUND) 0 else 1 },
                    { (i, _) -> i },
                ),
            )
            .map { it.value }

    /** [ordered], rotated left by [variant] places. */
    private fun defaultsFor(group: MuscleGroup, equipmentRank: Map<Equipment, Int>, variant: Int): List<LibraryExercise> {
        val list = ordered(group, equipmentRank)
        if (list.isEmpty()) return list
        val shift = ((variant % list.size) + list.size) % list.size
        return list.drop(shift) + list.take(shift)
    }

    // ---- adjusting a draft ----

    /**
     * Catalogue exercises with the same main group as [name] in [draft] that are not in the draft, ordered as the
     * defaults are (by the equipment [picks] use). Empty when [name] is not in the draft or has no main group.
     */
    fun alternatives(name: String, draft: PlanDraft, picks: List<BuilderPick>): List<LibraryExercise> {
        val key = Library.normalise(name)
        val exercises = draft.sessions.flatMap { it.exercises }
        val group = exercises.firstOrNull { Library.normalise(it.name) == key }?.mainGroup ?: return emptyList()
        val inDraft = exercises.map { Library.normalise(it.name) }.toSet()
        return ordered(group, equipmentRank(picks)).filter { Library.normalise(it.name) !in inDraft }
    }

    /**
     * [draft] with [from] on [day] replaced by the catalogue exercise [to], in the same place with the same sets and
     * range; the volume is recounted. Throws IllegalArgumentException when [day] is not in the draft, [from] is not on
     * it, [to] is not in the catalogue, or [to] is already on that day.
     */
    fun swap(draft: PlanDraft, day: DayOfWeek, from: String, to: String): PlanDraft {
        val session = draft.sessions.firstOrNull { it.day == day } ?: throw IllegalArgumentException("day: $day is not in the draft")
        val index = session.exercises.indexOfFirst { Library.normalise(it.name) == Library.normalise(from) }
        require(index >= 0) { "from: '$from' is not on $day" }
        val entry = Library.find(to) ?: throw IllegalArgumentException("to: '$to' is not in the catalogue")
        require(session.exercises.none { Library.normalise(it.name) == Library.normalise(entry.name) }) { "to: '${entry.name}' is already on $day" }
        val old = session.exercises[index]
        val new = old.copy(
            name = entry.name, inCatalogue = true, mainGroup = mainGroup(entry.muscles), muscles = entry.muscles,
            compound = entry.kind == Kind.COMPOUND,
        )
        val sessions = draft.sessions.map { s ->
            if (s.day != day) s else s.copy(exercises = s.exercises.toMutableList().also { it[index] = new })
        }
        return draft.copy(sessions = sessions, volume = volume(sessions, draft.volume.map { it.group }))
    }

    // ---- pre-ticks ----

    /**
     * Catalogue and own exercises to pre-tick: in either side's plan (bodybuilding first, each by weekday and order),
     * then logged in the [SUGGEST_DAYS] days up to and including [today] (newest first); each once, never an archived
     * exercise or one the history does not know.
     */
    fun suggestedPicks(history: TrainingHistory, plans: List<PlanItem>, today: LocalDate): List<Long> {
        val from = today.minus(DatePeriod(days = SUGGEST_DAYS))
        val planned = plans
            .sortedWith(compareBy({ if (it.mode == TrainingMode.BODYBUILDING) 0 else 1 }, { it.weekday }, { it.order }))
            .map { it.exerciseId }
        val logged = history.sessions
            .filter { it.date in from..today }
            .sortedByDescending { it.date }
            .flatMap { s -> s.sets.map { it.exerciseId } }
        return (planned + logged).distinct().filter { id -> history.exercises[id]?.archived == false }
    }
}
