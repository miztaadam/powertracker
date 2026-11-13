package com.powertracker.domain.plan

import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * The bodybuilding plan builder (stage 2 plan, "The rules", as revised to session templates): a pure function from the
 * lifter's answers to a draft week. The same answers always give the same draft. Sources for its numbers are in
 * `coach/CoachSources.kt` (BUILDER_WEEKLY_SETS, BUILDER_FREQUENCY, BUILDER_ORDER); the templates, their sizes, the
 * held-over place, the 4 sets and the default exercises are the designer's, not sourced.
 */
object PlanBuilder {
    /** Counted sets a week each target group aims at: ACSM 2026, >= 10 sets per muscle group (CoachSources.BUILDER_WEEKLY_SETS). */
    const val WEEKLY_SETS = 10

    /** At most this many priority groups, each a target group. The designer's number, not sourced. */
    const val MAX_PRIORITIES = 3

    /** Picks a session takes beyond its slots. The designer's number, not sourced. */
    const val HELD_OVER_PER_SESSION = 1

    /** The owner's 3 sets of 8-12 at RPE 8. */
    const val SETS = 3

    /** Sets for the exercise in a session's first template slot. The designer's number, not sourced. */
    const val LEAD_SETS = 4

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

    /**
     * A kind of template slot: the main groups it draws from ([kindOf] decides which kind an exercise is), whether it
     * belongs to an upper session on 4 days, and its catalogue defaults by exact name, in order (hand-picked by the
     * designer, not sourced). A slot takes only exercises of its own kind.
     */
    enum class SlotKind(val accepts: Set<MuscleGroup>, val upper: Boolean, val defaults: List<String>) {
        CHEST_PRESS(setOf(MuscleGroup.CHEST), true, listOf("DB press", "Machine chest press", "Bench", "Smith bench press")),
        CHEST_INCLINE(setOf(MuscleGroup.CHEST), true, listOf("Incline DB press", "Incline bench press", "Incline machine press", "Smith incline press")),
        CHEST_FLY(setOf(MuscleGroup.CHEST), true, listOf("Cable crossover", "Pec deck", "DB fly", "Seated cable fly")),
        PULL_DOWN(setOf(MuscleGroup.LATS), true, listOf("Lat pulldown", "Pull-up", "Machine pulldown", "Chin-up", "Close-grip pulldown")),
        ROW(
            setOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS), true,
            listOf("Seated cable row", "One-arm DB row", "Machine row", "Chest-supported row", "Row", "T-bar row"),
        ),
        QUAD_COMPOUND(setOf(MuscleGroup.QUADS, MuscleGroup.ADDUCTORS), false, listOf("Leg press", "Hack squat", "Squat", "Goblet squat", "Smith squat")),
        QUAD_ISOLATION(setOf(MuscleGroup.QUADS, MuscleGroup.ADDUCTORS), false, listOf("Leg extension", "Bulgarian split squat", "Split squat", "Lunge")),
        HINGE(
            setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK), false,
            listOf("RDL", "DB RDL", "Stiff-leg deadlift"),
        ),
        LEG_CURL(setOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES), false, listOf("Seated leg curl", "Lying leg curl", "Standing leg curl", "Cable leg curl")),
        SHOULDER_PRESS(setOf(MuscleGroup.FRONT_DELTS), true, listOf("DB shoulder press", "Machine shoulder press", "Seated barbell press", "Arnold press")),
        SIDE_DELT(setOf(MuscleGroup.SIDE_DELTS), true, listOf("Lateral raise", "Cable lateral raise", "Machine lateral raise", "Seated lateral raise")),
        REAR_DELT(setOf(MuscleGroup.REAR_DELTS, MuscleGroup.TRAPS), true, listOf("Face pull", "Rear delt raise", "Machine reverse fly", "Cable reverse fly")),
        BICEPS(setOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS), true, listOf("DB curl", "Cable curl", "EZ-bar curl", "Incline DB curl", "Machine curl")),
        TRICEPS(setOf(MuscleGroup.TRICEPS), true, listOf("Rope pushdown", "Triceps pushdown", "DB overhead triceps extension", "Skull crusher")),
        CALVES(setOf(MuscleGroup.CALVES), false, listOf("Standing calf raise", "Seated calf raise", "Leg press calf raise", "Dumbbell calf raise")),
        ABS(setOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES), false, listOf("Cable crunch", "Machine crunch", "Reverse crunch", "Ab wheel rollout")),
    }

    /** A GLUTES priority's added slot draws from these, in this order, rotated by the variant (not its kind's defaults). */
    internal val GLUTE_PRIORITY_DEFAULTS: List<String> = listOf("Hip thrust", "Machine hip thrust", "DB hip thrust")

    /** The slot each priority adds. */
    private val PRIORITY_SLOT: Map<MuscleGroup, SlotKind> = mapOf(
        MuscleGroup.CHEST to SlotKind.CHEST_FLY, MuscleGroup.LATS to SlotKind.PULL_DOWN, MuscleGroup.UPPER_BACK to SlotKind.ROW,
        MuscleGroup.QUADS to SlotKind.QUAD_ISOLATION, MuscleGroup.HAMSTRINGS to SlotKind.LEG_CURL, MuscleGroup.GLUTES to SlotKind.HINGE,
        MuscleGroup.SIDE_DELTS to SlotKind.SIDE_DELT, MuscleGroup.REAR_DELTS to SlotKind.REAR_DELT, MuscleGroup.BICEPS to SlotKind.BICEPS,
        MuscleGroup.TRICEPS to SlotKind.TRICEPS, MuscleGroup.CALVES to SlotKind.CALVES, MuscleGroup.ABS to SlotKind.ABS,
    )

    private val FULL_BODY_TEMPLATES: List<List<SlotKind>> = listOf(
        listOf(SlotKind.QUAD_COMPOUND, SlotKind.CHEST_PRESS, SlotKind.PULL_DOWN, SlotKind.LEG_CURL, SlotKind.SIDE_DELT, SlotKind.REAR_DELT, SlotKind.BICEPS, SlotKind.TRICEPS),
        listOf(SlotKind.HINGE, SlotKind.CHEST_INCLINE, SlotKind.ROW, SlotKind.QUAD_ISOLATION, SlotKind.SHOULDER_PRESS, SlotKind.CALVES, SlotKind.ABS, SlotKind.BICEPS, SlotKind.TRICEPS),
        listOf(SlotKind.CHEST_PRESS, SlotKind.QUAD_COMPOUND, SlotKind.PULL_DOWN, SlotKind.HINGE, SlotKind.SIDE_DELT, SlotKind.REAR_DELT, SlotKind.CALVES, SlotKind.ABS),
    )

    /** Upper, lower, upper, lower. */
    private val UPPER_LOWER_TEMPLATES: List<List<SlotKind>> = listOf(
        listOf(SlotKind.CHEST_PRESS, SlotKind.ROW, SlotKind.SHOULDER_PRESS, SlotKind.PULL_DOWN, SlotKind.SIDE_DELT, SlotKind.REAR_DELT, SlotKind.BICEPS, SlotKind.TRICEPS),
        listOf(SlotKind.QUAD_COMPOUND, SlotKind.HINGE, SlotKind.QUAD_ISOLATION, SlotKind.LEG_CURL, SlotKind.CALVES, SlotKind.ABS),
        listOf(SlotKind.CHEST_INCLINE, SlotKind.PULL_DOWN, SlotKind.CHEST_FLY, SlotKind.ROW, SlotKind.SIDE_DELT, SlotKind.REAR_DELT, SlotKind.TRICEPS, SlotKind.BICEPS),
        listOf(SlotKind.HINGE, SlotKind.QUAD_COMPOUND, SlotKind.LEG_CURL, SlotKind.QUAD_ISOLATION, SlotKind.CALVES, SlotKind.ABS),
    )

    /** How many days before today a logged exercise is still pre-ticked. */
    private const val SUGGEST_DAYS = 28

    /** The group of the first-listed muscle; null when there is none or it belongs to no group. */
    fun mainGroup(muscles: List<String>): MuscleGroup? = muscles.firstOrNull()?.let(MuscleGroups::of)

    /**
     * An exercise's one kind. A catalogue main lift or variation ([inCatalogue], looked up by name) takes its lift's
     * kind whatever its first muscle. Otherwise its main group decides and, where a group has two kinds, its name (any
     * letter case) or whether it is a compound. Null for no kind: such a pick can only be held over.
     */
    fun kindOf(name: String, muscles: List<String>, compound: Boolean, inCatalogue: Boolean): SlotKind? {
        if (inCatalogue) {
            val entry = Library.find(name)
            val lift = when (entry?.category) {
                Category.SQUAT -> MainLift.SQUAT
                Category.BENCH -> MainLift.BENCH
                Category.DEADLIFT -> MainLift.DEADLIFT
                Category.STRICT_PRESS -> MainLift.STRICT_PRESS
                Category.VARIATION -> entry.parentLift
                Category.ACCESSORY, null -> null
            }
            when (lift) {
                MainLift.SQUAT -> return SlotKind.QUAD_COMPOUND
                MainLift.BENCH -> return SlotKind.CHEST_PRESS
                MainLift.DEADLIFT -> return SlotKind.HINGE
                MainLift.STRICT_PRESS -> return SlotKind.SHOULDER_PRESS
                null -> Unit
            }
        }
        fun has(vararg words: String) = words.any { name.contains(it, ignoreCase = true) }
        return when (mainGroup(muscles)) {
            MuscleGroup.CHEST -> when {
                !compound -> SlotKind.CHEST_FLY
                has("incline") -> SlotKind.CHEST_INCLINE
                else -> SlotKind.CHEST_PRESS
            }
            MuscleGroup.LATS -> if (has("row")) SlotKind.ROW else SlotKind.PULL_DOWN
            MuscleGroup.UPPER_BACK -> SlotKind.ROW
            MuscleGroup.QUADS, MuscleGroup.ADDUCTORS ->
                if (has("extension", "adduction", "lunge", "split squat", "step-up")) SlotKind.QUAD_ISOLATION else SlotKind.QUAD_COMPOUND
            MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK -> when {
                has("curl", "glute-ham") -> SlotKind.LEG_CURL
                has("abduction", "clamshell", "band walk", "quadruped") -> null
                has("curtsy lunge") -> SlotKind.QUAD_ISOLATION
                else -> SlotKind.HINGE
            }
            MuscleGroup.FRONT_DELTS -> if (has("raise")) null else SlotKind.SHOULDER_PRESS
            MuscleGroup.SIDE_DELTS -> SlotKind.SIDE_DELT
            MuscleGroup.REAR_DELTS -> if (has("rotation")) null else SlotKind.REAR_DELT
            MuscleGroup.BICEPS -> SlotKind.BICEPS
            MuscleGroup.FOREARMS -> if (has("curl") && !has("wrist")) SlotKind.BICEPS else null
            MuscleGroup.TRICEPS -> SlotKind.TRICEPS
            MuscleGroup.CALVES -> SlotKind.CALVES
            MuscleGroup.ABS, MuscleGroup.OBLIQUES -> if (has("plank", "carry", "walk")) null else SlotKind.ABS
            MuscleGroup.TRAPS, null -> null
        }
    }

    // ---- building ----

    /** An exercise while the draft is built. */
    private class Item(val name: String, val inCatalogue: Boolean, val muscles: List<String>, val compound: Boolean) {
        val key: String = Library.normalise(name)
        val mainGroup: MuscleGroup? = mainGroup(muscles)
        val kind: SlotKind? = kindOf(name, muscles, compound, inCatalogue)
    }

    /** A template slot, or one a priority added ([forGroup]): an added slot takes only an exercise that works its group. */
    private class Slot(val kind: SlotKind, val lead: Boolean, val forGroup: MuscleGroup? = null) {
        var item: Item? = null
        fun takes(item: Item) = item.kind == kind && (forGroup == null || MuscleGroups.works(item.muscles, forGroup))
    }

    /** A session being built; [upper] is null on a full-body day. */
    private class Session(val day: DayOfWeek, val title: String, val upper: Boolean?, kinds: List<SlotKind>) {
        val slots: MutableList<Slot> = kinds.mapIndexed { i, kind -> Slot(kind, lead = i == 0) }.toMutableList()
        var heldOver: Item? = null
        fun holds(key: String) = slots.any { it.item?.key == key } || heldOver?.key == key
        fun firstEmpty(pick: Item, added: Boolean): Slot? =
            slots.firstOrNull { it.item == null && (it.forGroup != null) == added && it.takes(pick) }

        /** On 4 days, whether a slot or a pick of [kind] belongs here; a pick with no kind belongs anywhere. */
        fun mayTake(kind: SlotKind?) = upper == null || kind == null || upper == kind.upper
    }

    /**
     * The draft week for [answers]. Throws IllegalArgumentException unless there are 3 or 4 distinct days, at most
     * [MAX_PRIORITIES] distinct priorities, each one of [TARGET_GROUPS], and every pick marked [BuilderPick.inCatalogue]
     * is a catalogue name.
     */
    fun build(answers: BuilderAnswers): PlanDraft {
        val days = answers.days
        require(days.size == 3 || days.size == 4) { "days: ${days.size} days; the builder takes 3 or 4" }
        require(days.toSet().size == days.size) { "days: a weekday is repeated" }
        val priorities = answers.priorities
        require(priorities.size <= MAX_PRIORITIES) { "priorities: ${priorities.size}; at most $MAX_PRIORITIES" }
        require(priorities.toSet().size == priorities.size) { "priorities: a group is repeated" }
        require(priorities.all { it in TARGET_GROUPS }) { "priorities: ${priorities.filter { it !in TARGET_GROUPS }} not target groups" }
        val unknown = answers.picks.filter { it.inCatalogue && Library.find(it.name) == null }.map { it.name }
        require(unknown.isEmpty()) { "picks: $unknown marked as catalogue exercises but not in the catalogue" }

        val sessions = sessions(days)

        // 1. Each priority adds a slot of its kind: fewest slots of that kind, then fewest slots, then earliest.
        for (group in priorities) {
            val kind = PRIORITY_SLOT.getValue(group)
            sessions.filter { it.mayTake(kind) }
                .minWith(compareBy<Session>({ s -> s.slots.count { it.kind == kind } }, { it.slots.size }))
                .slots += Slot(kind, lead = false, forGroup = group)
        }

        // The same pick twice (by name) is one pick. Kinds-table order (no kind last), then name; priorities do not
        // change it, so a priority cannot move anyone else's pick.
        val picks = answers.picks.distinctBy { Library.normalise(it.name) }
            .map { Item(it.name, it.inCatalogue, it.muscles, it.compound) }
        val ordered = picks.sortedWith(compareBy<Item>({ it.kind?.ordinal ?: Int.MAX_VALUE }, { it.name }))

        // A pick looks at every template slot (week order) before any added slot, so a priority only adds.
        fun slotFor(pick: Item, among: List<Session>): Slot? =
            among.firstNotNullOfOrNull { it.firstEmpty(pick, added = false) } ?: among.firstNotNullOfOrNull { it.firstEmpty(pick, added = true) }

        // 2. Place the picks once: the first empty slot of its kind, sessions in week order, slots in session order.
        val placed = mutableListOf<Item>()
        val heldOver = mutableListOf<Item>()
        for (pick in ordered) {
            val slot = if (pick.kind == null) null else slotFor(pick, sessions)
            if (slot == null) {
                heldOver += pick
            } else {
                slot.item = pick
                placed += pick
            }
        }

        // 3. Place them a second time, in a session that does not already hold them.
        for (pick in placed) slotFor(pick, sessions.filter { !it.holds(pick.key) })?.item = pick

        // 4. Held-over picks: one a session, the session with the fewest exercises (its slots) that may take it.
        val leftOut = mutableListOf<LeftOut>()
        for (pick in heldOver) {
            val target = sessions.filter { it.heldOver == null && it.mayTake(pick.kind) }.minByOrNull { it.slots.size }
            if (target == null) leftOut += LeftOut(pick.name, NO_ROOM) else target.heldOver = pick
        }

        // 5. Fill the empty slots with each kind's first default the session does not hold: every template slot first,
        // then the added slots, which take only a default that works their group (a GLUTES slot draws from its own list). A pick never goes in a third time
        // through a default.
        val equipmentUse = equipmentUse(answers.picks)
        val pickKeys = picks.map { it.key }.toSet()
        fun weekCount(key: String) = sessions.sumOf { s -> s.slots.count { it.item?.key == key } + (if (s.heldOver?.key == key) 1 else 0) }
        for (added in listOf(false, true)) for (s in sessions) {
            for (slot in s.slots.filter { it.item == null && (it.forGroup != null) == added }) {
                val candidates = if (slot.forGroup == MuscleGroup.GLUTES) rotate(GLUTE_PRIORITY_DEFAULTS.map(::entry), answers.variant)
                else defaultsFor(slot.kind, equipmentUse, answers.variant)
                val default = candidates.firstOrNull { d ->
                    val k = Library.normalise(d.name)
                    !s.holds(k) && (k !in pickKeys || weekCount(k) < 2) && (slot.forGroup == null || MuscleGroups.works(d.muscles, slot.forGroup))
                }
                slot.item = default?.let { Item(it.name, true, it.muscles, it.kind == Kind.COMPOUND) }
            }
        }
        for (s in sessions) s.slots.removeAll { it.item == null } // only when no default fits

        // 6 and 7. Order each session and prescribe.
        val draftSessions = sessions.map { s ->
            fun draft(item: Item, slot: Slot?): DraftExercise {
                val sets = if (item.mainGroup in priorities) PRIORITY_SETS else if (slot?.lead == true) LEAD_SETS else SETS
                return DraftExercise(
                    item.name, item.inCatalogue, sets, REPS_LOW, REPS_HIGH, RPE, item.mainGroup, item.muscles, item.compound,
                    slot?.kind, slot?.lead == true,
                )
            }
            val inSlots = s.slots.map { draft(it.item!!, it) }
            val held = listOfNotNull(s.heldOver?.let { draft(it, null) })
            fun priority(e: DraftExercise) = e.mainGroup in priorities
            val exercises = inSlots.filter(::priority) + held.filter(::priority) + inSlots.filterNot(::priority) + held.filterNot(::priority)
            DraftSession(s.day, s.title, exercises)
        }
        return PlanDraft(draftSessions, volume(draftSessions), leftOut)
    }

    private fun sessions(days: List<DayOfWeek>): List<Session> {
        val week = days.sortedBy { it.ordinal }
        return if (week.size == 3) {
            week.mapIndexed { i, day -> Session(day, FULL_BODY, null, FULL_BODY_TEMPLATES[i]) }
        } else {
            week.mapIndexed { i, day ->
                if (i % 2 == 0) Session(day, UPPER_TITLE, true, UPPER_LOWER_TEMPLATES[i]) else Session(day, LOWER_TITLE, false, UPPER_LOWER_TEMPLATES[i])
            }
        }
    }

    private fun volume(sessions: List<DraftSession>): List<GroupVolume> = TARGET_GROUPS.map { group ->
        var sets = 0
        var days = 0
        for (s in sessions) {
            val here = s.exercises.filter { MuscleGroups.works(it.muscles, group) }
            sets += here.sumOf { it.sets }
            if (here.isNotEmpty()) days++
        }
        GroupVolume(group, sets, days, sets < WEEKLY_SETS)
    }

    // ---- defaults ----

    /** How many picks list each piece of equipment; the same pick given twice (by name) counts once. */
    private fun equipmentUse(picks: List<BuilderPick>): Map<Equipment, Int> {
        val once = picks.distinctBy { Library.normalise(it.name) }
        return Equipment.entries.associateWith { eq -> once.count { eq in it.equipment } }
    }

    private fun entry(name: String): LibraryExercise =
        Library.all().firstOrNull { it.name == name } ?: error("default '$name' is not a catalogue name")

    /** [kind]'s defaults, the most-used primary equipment first (the table's order otherwise). */
    private fun orderedDefaults(kind: SlotKind, equipmentUse: Map<Equipment, Int>): List<LibraryExercise> =
        kind.defaults.map(::entry).sortedByDescending { d -> d.equipment.firstOrNull()?.let { equipmentUse.getValue(it) } ?: 0 }

    /** [orderedDefaults], rotated left by [variant] places. */
    private fun defaultsFor(kind: SlotKind, equipmentUse: Map<Equipment, Int>, variant: Int): List<LibraryExercise> =
        rotate(orderedDefaults(kind, equipmentUse), variant)

    /** [list] rotated left by [variant] places. */
    private fun rotate(list: List<LibraryExercise>, variant: Int): List<LibraryExercise> {
        val shift = ((variant % list.size) + list.size) % list.size
        return list.drop(shift) + list.take(shift)
    }

    // ---- adjusting a draft ----

    /**
     * What [name] on [day] (null: the first session that holds it) can be swapped for, none of them already in that
     * session. For a slot exercise: the catalogue exercises of that slot's kind, the kind's defaults first (ordered by
     * the equipment [picks] use), then the rest in catalogue order. For a held-over pick: the catalogue
     * exercises with the same main group. Empty when [name] is not in the draft or a held-over pick has no main group.
     */
    fun alternatives(name: String, draft: PlanDraft, picks: List<BuilderPick>, day: DayOfWeek? = null): List<LibraryExercise> {
        val key = Library.normalise(name)
        val session = draft.sessions.firstOrNull { s -> (day == null || s.day == day) && s.exercises.any { Library.normalise(it.name) == key } }
            ?: return emptyList()
        val exercise = session.exercises.first { Library.normalise(it.name) == key }
        val inSession = session.exercises.map { Library.normalise(it.name) }.toSet()
        fun free(e: LibraryExercise) = Library.normalise(e.name) !in inSession
        val kind = exercise.slot
        if (kind == null) {
            val group = exercise.mainGroup ?: return emptyList()
            return Library.all().filter { mainGroup(it.muscles) == group && free(it) }
        }
        val defaults = orderedDefaults(kind, equipmentUse(picks))
        val defaultNames = defaults.map { it.name }.toSet()
        return defaults.filter(::free) +
            Library.all().filter { it.name !in defaultNames && kindOf(it.name, it.muscles, it.kind == Kind.COMPOUND, true) == kind && free(it) }
    }

    /**
     * [draft] with [from] on [day] replaced by the catalogue exercise [to], in the same place with the same sets,
     * range and slot; the volume is recounted. Throws IllegalArgumentException when [day] is not in the draft, [from]
     * is not on it, [to] is not in the catalogue, or [to] is already on that day.
     */
    fun swap(draft: PlanDraft, day: DayOfWeek, from: String, to: String): PlanDraft {
        val session = draft.sessions.firstOrNull { it.day == day } ?: throw IllegalArgumentException("day: $day is not in the draft")
        val index = session.exercises.indexOfFirst { Library.normalise(it.name) == Library.normalise(from) }
        require(index >= 0) { "from: '$from' is not on $day" }
        val entry = Library.find(to) ?: throw IllegalArgumentException("to: '$to' is not in the catalogue")
        require(session.exercises.none { Library.normalise(it.name) == Library.normalise(entry.name) }) { "to: '${entry.name}' is already on $day" }
        val new = session.exercises[index].copy(
            name = entry.name, inCatalogue = true, mainGroup = mainGroup(entry.muscles), muscles = entry.muscles,
            compound = entry.kind == Kind.COMPOUND,
        )
        val sessions = draft.sessions.map { s ->
            if (s.day != day) s else s.copy(exercises = s.exercises.toMutableList().also { it[index] = new })
        }
        return draft.copy(sessions = sessions, volume = volume(sessions))
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
