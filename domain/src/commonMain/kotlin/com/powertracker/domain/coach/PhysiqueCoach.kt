package com.powertracker.domain.coach

import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.NewExercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.SetAddition
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
import com.powertracker.domain.plan.PlanBuilder
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

/** Why a lagging group got no more work from a check-in. */
/**
 * Why a lagging group got no more work: already at the ceiling; no candidate at all; no day with room; held for a
 * later check-in; or every direct exercise and candidate there was would take some group past the ceiling.
 */
enum class NoteReason { AT_CEILING, NO_EXERCISE, FULL, LATER, WOULD_PASS_CEILING }

/** A lagging [group] that got no advice, why, and its [weeklySets] in the plan. */
data class GroupNote(val group: MuscleGroup, val reason: NoteReason, val weeklySets: Int)

/**
 * What saving a check-in gives: new MORE_WORK [advice] and a [notes] line for every other lagging group, plus the
 * MORE_WORK advice that was OPEN before, now DISMISSED ([dismissed], to store with the check-in).
 */
data class CheckInOutcome(val advice: List<Advice>, val notes: List<GroupNote>, val dismissed: List<Advice> = emptyList())

/**
 * A change to the bodybuilding plan, to carry out in one transaction: save [updated], delete [removed], and insert
 * each of [added] (exerciseId 0) after resolving its catalogue name. [changed] is false when nothing applied.
 */
data class PlanChange(
    val updated: List<PlanItem>,
    val removed: List<PlanItem>,
    val added: List<Pair<PlanItem, String>>,
    val message: String,
    val changed: Boolean,
)

/**
 * The lagging-muscle coach of the bodybuilding side (stage 3 plan, "The rules"): more work for a lagging body part,
 * swaps for a stuck exercise, and applying either to the plan. Pure functions; the same inputs give the same output.
 * Every function reads only the bodybuilding side's plan items of the plan it is given.
 *
 * The weekly ceiling is sourced (CoachSources.WEEKLY_CEILING); every other number here is the designer's, not sourced.
 */
object PhysiqueCoach {
    /** At most this many counted sets a week for one group (Baz-Valle et al. 2022, CoachSources.WEEKLY_CEILING). */
    const val WEEKLY_CEILING = 20

    /** No exercise is taken above this many sets. The designer's number, not sourced. */
    const val MAX_SETS = 4

    /** At most this many lagging groups get more work from one check-in. The designer's number, not sourced. */
    const val MAX_GROUPS = 3

    /** No session grows above this many exercises. The designer's number, not sourced. */
    const val MAX_EXERCISES = 10

    /** A check-in falls due this many days after the last. The designer's number, not sourced. */
    const val CHECK_IN_DAYS = 28

    /** An exercise is stuck when it has not moved for this many days. The designer's number, not sourced. */
    const val STUCK_DAYS = 21

    /** No second swap for the same exercise within this many days. The designer's number, not sourced. */
    const val SWAP_COOLDOWN_DAYS = 28

    /** Alternatives offered with a swap. The designer's number, not sourced. */
    const val ALTERNATIVES = 3

    /** Sets, reps and RPE of a new exercise: the owner's 3 x 8-12 at RPE 8. */
    private const val NEW_SETS = PlanBuilder.SETS
    private const val NEW_REPS_LOW = PlanBuilder.REPS_LOW
    private const val NEW_REPS_HIGH = PlanBuilder.REPS_HIGH
    private const val NEW_RPE = PlanBuilder.RPE

    /** Held, carried or drill work, not done for 8-12 reps: never a new exercise or an alternative. */
    private val NOT_FOR_REPS = listOf("plank", "carry", "walk", "hold", "clamshell", "quadruped", "rotation", "pallof")

    private val UPPER: Set<MuscleGroup> = setOf(
        MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.TRAPS, MuscleGroup.FRONT_DELTS,
        MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS,
    )
    private val LOWER: Set<MuscleGroup> = setOf(
        MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.ADDUCTORS, MuscleGroup.CALVES,
        MuscleGroup.LOWER_BACK,
    )
    private val BOTH: Set<MuscleGroup> = setOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES)

    /** [group]'s half of the body: upper or lower, each with abs and obliques; abs and obliques belong to both. */
    private fun half(group: MuscleGroup): Set<MuscleGroup> = when (group) {
        in UPPER -> UPPER + BOTH
        in LOWER -> LOWER + BOTH
        else -> UPPER + LOWER + BOTH
    }

    // ---- counting ----

    private fun muscles(item: PlanItem, exercises: Map<Long, Exercise>): List<String> = exercises[item.exerciseId]?.muscles.orEmpty()

    private fun works(item: PlanItem, exercises: Map<Long, Exercise>, group: MuscleGroup) = MuscleGroups.works(muscles(item, exercises), group)

    /** The groups [muscles] work, each once. */
    private fun groupsOf(muscles: List<String>): Set<MuscleGroup> = muscles.mapNotNullTo(LinkedHashSet()) { MuscleGroups.of(it) }

    /** [group]'s weekly sets: the counted sets of the bodybuilding plan's items for it. */
    fun weeklySets(plan: List<PlanItem>, exercises: Map<Long, Exercise>, group: MuscleGroup): Int =
        plan.on(TrainingMode.BODYBUILDING).filter { works(it, exercises, group) }.sumOf { it.sets }

    private fun mainGroupOf(item: PlanItem, exercises: Map<Long, Exercise>): MuscleGroup? =
        exercises[item.exerciseId]?.let { PlanBuilder.mainGroup(it.muscles) }

    /** The kind of a stored exercise: a catalogue one (found by name) by its entry, an own one by its category. */
    private fun kindOf(exercise: Exercise): PlanBuilder.SlotKind? {
        val entry = Library.find(exercise.name)
        val compound = entry?.let { it.kind == Kind.COMPOUND } ?: (exercise.category != Category.ACCESSORY)
        return PlanBuilder.kindOf(exercise.name, exercise.muscles, compound, inCatalogue = entry != null)
    }

    /** Each catalogue exercise's kind and its place in that kind's defaults, worked out once. */
    private class Entry(val kind: PlanBuilder.SlotKind?, val defaultsRank: Int, val index: Int)

    private val catalogue: Map<String, Entry> by lazy {
        Library.all().withIndex().associate { (i, e) ->
            val kind = PlanBuilder.kindOf(e.name, e.muscles, e.kind == Kind.COMPOUND, inCatalogue = true)
            val rank = kind?.defaults?.indexOf(e.name)?.takeIf { it >= 0 } ?: Int.MAX_VALUE
            e.name to Entry(kind, rank, i)
        }
    }

    private fun kindOf(entry: LibraryExercise): PlanBuilder.SlotKind? = catalogue.getValue(entry.name).kind

    /** Whether the catalogue exercise [name] is in [plan]: an item's exercise has the same normalised name. */
    private fun inPlan(name: String, plan: List<PlanItem>, exercises: Map<Long, Exercise>): Boolean {
        val key = Library.normalise(name)
        return plan.any { p -> exercises[p.exerciseId]?.let { Library.normalise(it.name) == key } == true }
    }

    /** The equipment of the plan's catalogue exercises (found by name), plus BODYWEIGHT. */
    private fun planEquipment(plan: List<PlanItem>, exercises: Map<Long, Exercise>): Set<Equipment> =
        plan.mapNotNull { exercises[it.exerciseId]?.let { e -> Library.find(e.name) } }.flatMapTo(mutableSetOf(Equipment.BODYWEIGHT)) { it.equipment }

    private fun inWeekOrder(plan: List<PlanItem>) = plan.sortedWith(compareBy({ it.weekday }, { it.order }, { it.id }))

    /**
     * The catalogue exercises that may join [plan] for [group]: category ACCESSORY, main group [group], not in the
     * plan, every piece of equipment in the plan's equipment, nothing held or carried. In catalogue order.
     */
    private fun candidates(group: MuscleGroup, plan: List<PlanItem>, exercises: Map<Long, Exercise>): List<LibraryExercise> {
        val equipment = planEquipment(plan, exercises)
        val planned = plan.mapNotNullTo(HashSet()) { p -> exercises[p.exerciseId]?.let { Library.normalise(it.name) } }
        return forReps[group].orEmpty().filter { e -> e.equipment.all { it in equipment } && Library.normalise(e.name) !in planned }
    }

    /** The catalogue's ACCESSORY exercises done for reps (nothing held or carried), by main group, in catalogue order. */
    private val forReps: Map<MuscleGroup?, List<LibraryExercise>> by lazy {
        Library.all()
            .filter { e -> e.category == Category.ACCESSORY && NOT_FOR_REPS.none { e.name.contains(it, ignoreCase = true) } }
            .groupBy { PlanBuilder.mainGroup(it.muscles) }
    }

    /**
     * New-exercise candidates for [group], best first: those whose kind differs from the kind of every plan item whose
     * main group is [group] (they work it differently), then the rest of [order]'s keys.
     */
    internal fun newExerciseCandidates(group: MuscleGroup, plan: List<PlanItem>, exercises: Map<Long, Exercise>): List<LibraryExercise> {
        val items = plan.on(TrainingMode.BODYBUILDING)
        val kinds = items.filter { mainGroupOf(it, exercises) == group }.mapNotNull { exercises[it.exerciseId] }.map(::kindOf).toSet()
        return order(group, candidates(group, items, exercises)) { kindOf(it) !in kinds }
    }

    /**
     * Every swap alternative for the stuck [exercise] the rules allow, best first: the same kind first, then the rest of
     * [order]'s keys. When the exercise's first equipment (catalogue exercises only) is not BODYWEIGHT or BAND, no
     * alternative whose first equipment is; and only alternatives that keep the ceiling ([swapKeepsCeiling]).
     */
    internal fun swapAlternatives(exercise: Exercise, plan: List<PlanItem>, exercises: Map<Long, Exercise>): List<LibraryExercise> {
        val group = PlanBuilder.mainGroup(exercise.muscles) ?: return emptyList()
        val kind = kindOf(exercise)
        val items = plan.on(TrainingMode.BODYBUILDING)
        // Abs and obliques are trained with bodyweight and bands as much as with loads: no filter for them.
        val core = group == MuscleGroup.ABS || group == MuscleGroup.OBLIQUES
        val loaded = !core && Library.find(exercise.name)?.equipment?.firstOrNull()?.let { !light(it) } ?: false
        return order(group, candidates(group, items, exercises)) { kindOf(it) == kind }
            .filter { !loaded || !lightFirst(it) }
            .filter { swapKeepsCeiling(exercise.id, it.muscles, items, exercises) }
    }

    private fun light(e: Equipment) = e == Equipment.BODYWEIGHT || e == Equipment.BAND

    private fun lightFirst(e: LibraryExercise) = e.equipment.firstOrNull()?.let(::light) ?: false

    /** The groups whose preferred list comes before their kind's defaults: glutes take the builder's glutes-priority list. */
    private val PREFERRED: Map<MuscleGroup, List<String>> = mapOf(MuscleGroup.GLUTES to PlanBuilder.GLUTE_PRIORITY_DEFAULTS)

    /**
     * [first] ones first; then, except for abs and obliques, those whose first equipment is not BODYWEIGHT or BAND;
     * then [group]'s preferred list, in order; then their kind's defaults, in order; then catalogue order.
     */
    private fun order(group: MuscleGroup, list: List<LibraryExercise>, first: (LibraryExercise) -> Boolean): List<LibraryExercise> {
        val preferred = PREFERRED[group].orEmpty()
        val core = group == MuscleGroup.ABS || group == MuscleGroup.OBLIQUES
        return list.sortedWith(
            compareBy(
                { if (first(it)) 0 else 1 },
                { if (!core && lightFirst(it)) 1 else 0 },
                { preferred.indexOf(it.name).let { i -> if (i >= 0) i else preferred.size } },
                { catalogue.getValue(it.name).defaultsRank },
                { catalogue.getValue(it.name).index },
            ),
        )
    }

    /**
     * Whether putting [muscles]' exercise in the place of [exerciseId] on every day it is planned, with the same sets,
     * leaves every group at the ceiling or under, or, for a group already above it, does not raise it.
     */
    private fun swapKeepsCeiling(exerciseId: Long, muscles: List<String>, plan: List<PlanItem>, exercises: Map<Long, Exercise>): Boolean {
        val held = plan.filter { it.exerciseId == exerciseId }.sumOf { it.sets }
        val old = groupsOf(exercises[exerciseId]?.muscles.orEmpty())
        val new = groupsOf(muscles)
        return (old + new).all { g ->
            val before = weeklySets(plan, exercises, g)
            val after = before - (if (g in old) held else 0) + (if (g in new) held else 0)
            after <= WEEKLY_CEILING || after <= before
        }
    }

    // ---- check-in: more work ----

    /** A working copy of the plan while one check-in's round of suggestions is chosen. */
    private class Working(plan: List<PlanItem>, val exercises: MutableMap<Long, Exercise>) {
        val items: MutableList<PlanItem> = plan.toMutableList()
        private var nextId = -1L
        private val groups = HashMap<Long, Set<MuscleGroup>>()

        /** The groups the item's exercise works, worked out once per exercise. */
        fun groupsOf(item: PlanItem): Set<MuscleGroup> = groups.getOrPut(item.exerciseId) { groupsOf(exercises[item.exerciseId]?.muscles.orEmpty()) }

        fun weekly(group: MuscleGroup) = items.sumOf { if (group in groupsOf(it)) it.sets else 0 }

        /** Every group [muscles] works stays at the ceiling or under with [extra] more sets. */
        fun fits(muscles: List<String>, extra: Int) = groupsOf(muscles).all { weekly(it) + extra <= WEEKLY_CEILING }

        fun addSet(item: PlanItem) {
            val i = items.indexOf(item)
            items[i] = item.copy(sets = item.sets + 1)
        }

        fun addNew(entry: LibraryExercise, day: DayOfWeek) {
            val id = nextId--
            exercises[id] = Exercise(id, entry.name, entry.category, entry.parentLift, entry.muscles, isBuiltIn = true)
            val order = (items.filter { it.weekday == day }.maxOfOrNull { it.order } ?: -1) + 1
            items += PlanItem(id, day, id, NEW_SETS, NEW_REPS_LOW, NEW_RPE, order, TrainingMode.BODYBUILDING, NEW_REPS_HIGH)
        }
    }

    private sealed interface Choice {
        data class Sets(val items: List<PlanItem>) : Choice
        data class New(val entry: LibraryExercise, val day: DayOfWeek) : Choice
        data class None(val reason: NoteReason) : Choice
    }

    /** Steps 2 and 3 of the rule for one lagging [group], on the working plan. */
    private fun choose(group: MuscleGroup, w: Working): Choice {
        val ex = w.exercises
        // 2. More sets: direct exercises under MAX_SETS, fewest sets first, then by weekday and order; at most two.
        val direct = w.items.filter { mainGroupOf(it, ex) == group && it.sets < MAX_SETS }
            .sortedWith(compareBy({ it.sets }, { it.weekday }, { it.order }, { it.id }))
        val chosen = mutableListOf<PlanItem>()
        val extra = mutableMapOf<MuscleGroup, Int>()
        for (item in direct) {
            if (chosen.size == 2) break
            val groups = w.groupsOf(item)
            if (groups.all { w.weekly(it) + (extra[it] ?: 0) + 1 <= WEEKLY_CEILING }) {
                chosen += item
                for (g in groups) extra[g] = (extra[g] ?: 0) + 1
            }
        }
        if (chosen.isNotEmpty()) return Choice.Sets(chosen)

        // 3. A new exercise.
        val days = w.items.map { it.weekday }.distinct().sorted()
        val open = days.filter { d -> w.items.count { it.weekday == d } < MAX_EXERCISES }
        if (open.isEmpty()) return Choice.None(NoteReason.FULL)
        fun setsOn(d: DayOfWeek) = w.items.filter { it.weekday == d && group in w.groupsOf(it) }.sumOf { it.sets }
        val training = open.filter { setsOn(it) > 0 }
        val day = if (training.isNotEmpty()) {
            training.minWith(compareBy({ setsOn(it) }, { it }))
        } else {
            val halfGroups = half(group)
            fun inHalf(d: DayOfWeek) = w.items.count { it.weekday == d && w.groupsOf(it).any { g -> g in halfGroups } }
            fun items(d: DayOfWeek) = w.items.count { it.weekday == d }
            open.minWith(compareBy({ -inHalf(it) }, { items(it) }, { it }))
        }
        val candidates = newExerciseCandidates(group, w.items, ex)
        val entry = candidates.firstOrNull { w.fits(it.muscles, NEW_SETS) }
            ?: return Choice.None(if (candidates.isEmpty() && direct.isEmpty()) NoteReason.NO_EXERCISE else NoteReason.WOULD_PASS_CEILING)
        return Choice.New(entry, day)
    }

    /**
     * What saving [checkIn] gives (the rule "More work"), for the bodybuilding side's items of [plan]. Every OPEN
     * MORE_WORK advice in [priorAdvice] comes back DISMISSED in [CheckInOutcome.dismissed]. The lagging groups are
     * taken the builder's 12 target groups first, then the rest, each fewest weekly sets first (ties in [MuscleGroup]
     * order); the first [MAX_GROUPS] that can be given more work
     * each get one MORE_WORK advice and every other lagging group a note. The suggestions of one check-in are chosen
     * one after another on a working copy of the plan, so applying all of them together keeps every group at the
     * ceiling or under and no new exercise takes a day's last place twice.
     */
    fun afterCheckIn(
        checkIn: CheckIn,
        history: TrainingHistory,
        plan: List<PlanItem>,
        today: LocalDate,
        priorAdvice: List<Advice> = emptyList(),
    ): CheckInOutcome {
        val items = plan.on(TrainingMode.BODYBUILDING)
        val exercises = history.exercises
        val dismissed = priorAdvice
            .filter { it.type == AdviceType.MORE_WORK && it.status == AdviceStatus.OPEN }
            .map { it.copy(status = AdviceStatus.DISMISSED) }
        // The builder's 12 target groups first, then the other six; within each, fewest weekly sets first.
        val lagging = checkIn.lagging.sortedWith(
            compareBy({ if (it in PlanBuilder.TARGET_GROUPS) 0 else 1 }, { weeklySets(items, exercises, it) }, { it.ordinal }),
        )
        val w = Working(items, exercises.toMutableMap())
        val advice = mutableListOf<Advice>()
        val notes = mutableListOf<GroupNote>()
        for (group in lagging) {
            val before = weeklySets(items, exercises, group)
            // 1. Already at the ceiling.
            if (before >= WEEKLY_CEILING) {
                notes += GroupNote(group, NoteReason.AT_CEILING, before)
                continue
            }
            val choice = choose(group, w)
            if (choice !is Choice.None && advice.size >= MAX_GROUPS) {
                // 4. It could be given more work, but the first MAX_GROUPS have it.
                notes += GroupNote(group, NoteReason.LATER, before)
                continue
            }
            when (choice) {
                is Choice.None -> notes += GroupNote(group, choice.reason, before)
                is Choice.Sets -> {
                    val adds = choice.items.map { SetAddition(it.weekday, it.exerciseId, exercises.getValue(it.exerciseId).name, it.sets, it.sets + 1) }
                    val after = before + choice.items.count { works(it, exercises, group) }
                    advice += moreWork(group, adds, null, before, after, today)
                    choice.items.forEach(w::addSet)
                }
                is Choice.New -> {
                    val new = NewExercise(choice.day, choice.entry.name, NEW_SETS, NEW_REPS_LOW, NEW_REPS_HIGH, NEW_RPE)
                    advice += moreWork(group, emptyList(), new, before, before + NEW_SETS, today)
                    w.addNew(choice.entry, choice.day)
                }
            }
        }
        return CheckInOutcome(advice, notes, dismissed)
    }

    private fun moreWork(
        group: MuscleGroup,
        adds: List<SetAddition>,
        new: NewExercise?,
        before: Int,
        after: Int,
        today: LocalDate,
    ): Advice {
        val what = if (new != null) {
            "Add ${new.name} on ${dayName(new.weekday)}, ${new.sets} × ${new.repsLow}–${new.repsHigh} @ ${rpeText(new.rpe)}."
        } else {
            "Add a set to " + adds.joinToString(" and ") { "${it.name} on ${dayName(it.weekday)}" } + "."
        }
        val reason = "You marked ${group.label.lowercase()} as lagging. $what That takes it from $before to $after sets a week."
        return Advice(
            AdviceType.MORE_WORK, null, today, reason, 0,
            AdvicePayload.MoreWork(group, adds, new, before, after), mode = TrainingMode.BODYBUILDING,
        )
    }

    private fun rpeText(rpe: Double): String = if (rpe == rpe.toLong().toDouble()) "${rpe.toLong()}" else "$rpe"

    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** "3 Nov". */
    internal fun shortDate(date: LocalDate): String = "${date.day} ${MONTHS[date.month.ordinal]}"

    /** "Monday" ... "Sunday". */
    internal fun dayName(day: DayOfWeek): String = day.name.lowercase().replaceFirstChar { it.uppercase() }

    // ---- applying ----

    /** Nothing applied: the plan changed since the advice was made. */
    const val NOTHING_ADDED = "Your plan has changed since; nothing was added"
    const val NOTHING_SWAPPED = "Your plan has changed since; nothing was swapped"

    /** More work that would now take a body part past the weekly ceiling. */
    const val NOTHING_ADDED_PAST_CEILING = "Your plan has changed since; that would take a body part past 20 sets a week, so nothing was added"

    /** A swap that would now take a body part past the weekly ceiling. */
    const val PAST_CEILING = "Your plan has changed since; that swap would take a body part past 20 sets a week, so nothing was swapped"

    /**
     * [payload] applied to the bodybuilding side's items of [plan]. A set addition applies only while the plan still
     * has that item (weekday and exercise) at its `fromSets`; a new exercise only while it is still not in the plan
     * and its day has fewer than [MAX_EXERCISES] items. Each change is taken only if it keeps every group it works at the
     * ceiling or under on the current plan, counting the changes already taken (a group above it is never raised).
     * What did not apply is left alone, and the message says so.
     */
    fun applyMoreWork(payload: AdvicePayload.MoreWork, plan: List<PlanItem>, exercises: Map<Long, Exercise>): PlanChange {
        val items = inWeekOrder(plan.on(TrainingMode.BODYBUILDING))
        // Weekly sets on the current plan, raised as each change below is taken, so the ones already taken count.
        val weekly = MuscleGroup.entries.associateWithTo(HashMap()) { g -> weeklySets(items, exercises, g) }
        /** Every group [muscles] work stays at the ceiling or under with [sets] more (a group above it is never raised). */
        fun take(muscles: List<String>, sets: Int): Boolean {
            val groups = groupsOf(muscles)
            if (groups.any { weekly.getValue(it) + sets > WEEKLY_CEILING }) return false
            for (g in groups) weekly[g] = weekly.getValue(g) + sets
            return true
        }
        val updates = mutableListOf<PlanItem>()
        var changedSince = 0
        var pastCeiling = 0
        for (add in payload.addSets) {
            val item = items.firstOrNull { it.weekday == add.weekday && it.exerciseId == add.exerciseId && it.sets == add.fromSets && updates.none { u -> u.id == it.id } }
            when {
                item == null -> changedSince++
                !take(exercises[item.exerciseId]?.muscles.orEmpty(), add.toSets - add.fromSets) -> pastCeiling++
                else -> updates += item.copy(sets = add.toSets)
            }
        }
        val added = mutableListOf<Pair<PlanItem, String>>()
        val new = payload.newExercise
        if (new != null) {
            when {
                inPlan(new.name, items, exercises) || items.count { it.weekday == new.weekday } >= MAX_EXERCISES -> changedSince++
                !take(Library.find(new.name)?.muscles.orEmpty(), new.sets) -> pastCeiling++
                else -> {
                    val order = (items.filter { it.weekday == new.weekday }.maxOfOrNull { it.order } ?: -1) + 1
                    added += PlanItem(0, new.weekday, 0, new.sets, new.repsLow, new.rpe, order, TrainingMode.BODYBUILDING, new.repsHigh) to new.name
                }
            }
        }
        val message = when {
            added.isNotEmpty() -> "Added ${new!!.name} to ${dayName(new.weekday)}"
            updates.isEmpty() -> if (pastCeiling > 0) NOTHING_ADDED_PAST_CEILING else NOTHING_ADDED
            else -> buildString {
                append("Added a set to ${exercisesText(updates.size)}")
                if (changedSince > 0) append("; ${exercisesText(changedSince)} had changed since and was left as it is")
                if (pastCeiling > 0) append("; ${exercisesText(pastCeiling)} would take a body part past 20 sets a week and was left as it is")
            }
        }
        return PlanChange(updates, emptyList(), added, message, updates.isNotEmpty() || added.isNotEmpty())
    }

    private fun exercisesText(n: Int) = if (n == 1) "1 exercise" else "$n exercises"

    /**
     * [payload]'s stuck exercise swapped for [choice] (one of its alternatives) in every bodybuilding plan item that
     * holds it, keeping weekday, order, sets, reps and RPE: the old items in `removed`, their replacements (exerciseId 0)
     * in `added`. Nothing changes when the stuck exercise is no longer in the plan, [choice] is in it by now, or the
     * swap would now take a group past the weekly ceiling (or raise one already above it).
     */
    fun applySwap(payload: AdvicePayload.Swap, choice: String, plan: List<PlanItem>, exercises: Map<Long, Exercise>): PlanChange {
        require(payload.alternatives.any { Library.normalise(it) == Library.normalise(choice) }) { "choice: '$choice' is not one of ${payload.alternatives}" }
        val name = payload.alternatives.first { Library.normalise(it) == Library.normalise(choice) }
        val items = inWeekOrder(plan.on(TrainingMode.BODYBUILDING))
        val holding = items.filter { it.exerciseId == payload.exerciseId }
        if (holding.isEmpty() || inPlan(name, items, exercises)) return PlanChange(emptyList(), emptyList(), emptyList(), NOTHING_SWAPPED, false)
        val entry = Library.find(name) ?: throw IllegalArgumentException("choice: '$name' is not in the catalogue")
        if (!swapKeepsCeiling(payload.exerciseId, entry.muscles, items, exercises)) {
            return PlanChange(emptyList(), emptyList(), emptyList(), PAST_CEILING, false)
        }
        return PlanChange(
            emptyList(), holding, holding.map { it.copy(id = 0, exerciseId = 0) to name },
            "Swapped ${payload.name} for $name", true,
        )
    }

    // ---- stuck exercises ----

    /** [date] is one of the [days] days ending [today]. */
    private fun within(date: LocalDate, today: LocalDate, days: Int) =
        date <= today && date >= today.minus(days - 1, DateTimeUnit.DAY)

    /** [a] beats [b]: a heavier heaviest weight, or the same with more reps in total. */
    private fun beats(a: List<LoggedSet>, b: List<LoggedSet>): Boolean {
        val wa = a.maxOf { it.weightKg }
        val wb = b.maxOf { it.weightKg }
        return wa > wb || (wa == wb && a.sumOf { it.reps } > b.sumOf { it.reps })
    }

    /**
     * SWAP advice for the bodybuilding plan's stuck exercises (the rule "Stuck"), in plan order. Skips an exercise whose
     * main group is growing well at the latest of [checkIns], one with a SWAP from the last [SWAP_COOLDOWN_DAYS] days
     * (any status) or an OPEN one, and every exercise while a DELOAD or RE_ENTRY advice, OPEN or APPLIED, was created in
     * the last [STUCK_DAYS] days.
     */
    fun stuck(
        history: TrainingHistory,
        plan: List<PlanItem>,
        checkIns: List<CheckIn>,
        priorAdvice: List<Advice>,
        today: LocalDate,
    ): List<Advice> {
        if (priorAdvice.any {
                (it.type == AdviceType.DELOAD || it.type == AdviceType.RE_ENTRY) &&
                    (it.status == AdviceStatus.OPEN || it.status == AdviceStatus.APPLIED) && within(it.createdAt, today, STUCK_DAYS)
            }
        ) {
            return emptyList()
        }
        val items = inWeekOrder(plan.on(TrainingMode.BODYBUILDING))
        val latest = checkIns.filter { it.date <= today }.maxByOrNull { it.date }
        val cutoff = today.minus(STUCK_DAYS, DateTimeUnit.DAY)
        val sessions = history.sessions.filter { it.mode == TrainingMode.BODYBUILDING && it.date <= today }.sortedBy { it.date }
        val out = mutableListOf<Advice>()
        for (id in items.map { it.exerciseId }.distinct()) {
            val exercise = history.exercises[id] ?: continue
            val group = PlanBuilder.mainGroup(exercise.muscles) ?: continue
            if (latest != null && group in latest.growingWell) continue
            val swaps = priorAdvice.filter { it.type == AdviceType.SWAP && (it.payload as? AdvicePayload.Swap)?.exerciseId == id }
            if (swaps.any { it.status == AdviceStatus.OPEN || within(it.createdAt, today, SWAP_COOLDOWN_DAYS) }) continue
            val done = sessions.mapNotNull { s -> s.sets.filter { it.exerciseId == id && !it.failed }.takeIf { it.isNotEmpty() }?.let { s.date to it } }
            val baseline = done.lastOrNull { it.first <= cutoff } ?: continue
            val window = done.filter { it.first > cutoff }
            if (window.size < 2 || window.any { beats(it.second, baseline.second) }) continue
            val alternatives = swapAlternatives(exercise, items, history.exercises).take(ALTERNATIVES).map { it.name }
            if (alternatives.isEmpty()) continue
            val weeks = baseline.first.daysUntil(today) / 7
            val reason = "${exercise.name} hasn't moved in $weeks ${if (weeks == 1) "week" else "weeks"}. " +
                "Same weight and no more reps since ${shortDate(baseline.first)}."
            out += Advice(
                AdviceType.SWAP, null, today, reason, 0,
                AdvicePayload.Swap(id, exercise.name, group, baseline.first, alternatives), mode = TrainingMode.BODYBUILDING,
            )
        }
        return out
    }

    // ---- due ----

    /**
     * Whether a check-in is due: the bodybuilding plan is not empty, at least one bodybuilding session is logged, and
     * the later of the latest check-in and the first bodybuilding session is [CHECK_IN_DAYS] or more days before [today].
     */
    fun checkInDue(history: TrainingHistory, plan: List<PlanItem>, checkIns: List<CheckIn>, today: LocalDate): Boolean {
        if (plan.on(TrainingMode.BODYBUILDING).isEmpty()) return false
        val first = history.sessions.filter { it.mode == TrainingMode.BODYBUILDING && it.date <= today }.minOfOrNull { it.date } ?: return false
        val latest = checkIns.filter { it.date <= today }.maxOfOrNull { it.date }
        val from = if (latest != null && latest > first) latest else first
        return from.daysUntil(today) >= CHECK_IN_DAYS
    }
}
