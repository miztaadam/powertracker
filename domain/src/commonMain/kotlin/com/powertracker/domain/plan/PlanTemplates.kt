package com.powertracker.domain.plan

import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DayOfWeek

/**
 * One exercise of a template day: [repsHigh] is null for fixed reps (every powerlifting entry) and the top of the range
 * on bodybuilding. [onlyWithStrictPress] entries appear only when the lifter has strict press on (`showStrictPress`).
 */
data class TemplateExercise(
    val name: String,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int?,
    val rpe: Double,
    val onlyWithStrictPress: Boolean = false,
)

/** One day of a template: its [title] and its [exercises] in the order to do them. */
data class TemplateSession(val title: String, val exercises: List<TemplateExercise>)

/** A ready-made week for one side: [suits] says who it is for; [sessions] are its days in order. */
data class PlanTemplate(
    val id: String,
    val mode: TrainingMode,
    val name: String,
    val suits: String,
    val sessions: List<TemplateSession>,
) {
    val dayCount: Int get() = sessions.size
}

/**
 * The ready-made weekly plans (1.14), three on each side, set by days a week. A template becomes the plan builder's
 * [PlanDraft], so its Swap and volume count are the builder's.
 *
 * Sources for the numbers are in `coach/CoachSources.kt`: days a week and the "who it suits" lines
 * (TEMPLATE_DAYS, TEMPLATE_SPLIT_ADVANCED, TEMPLATE_SPLIT_BODYBUILDERS); the main-lift sets, reps and RPE
 * (TEMPLATE_STRENGTH_LOAD, TEMPLATE_STRENGTH_PHASE, TEMPLATE_NSCA_TABLE, TEMPLATE_STRENGTH_FLOOR, with
 * TEMPLATE_LOAD_CHART for the notes' %1RM); the ranges around the accessories' count (TEMPLATE_ACCESSORY_RANGE,
 * TEMPLATE_NSCA_TABLE). Bodybuilding's 3 x 8-12 at RPE 8 is the owner's default; its 4-set entries are the
 * designer's, as the builder's LEAD_SETS is.
 *
 * The designer's, not sourced: the template names, ids and day titles; which exercise goes on which day; the
 * powerlifting accessories' fixed 3 x 10 at RPE 8 (settled by the owner on 2026-11-18, inside the sourced 8-10 and
 * 6-12 ranges); the exact reps and RPE 8 picked inside the sourced main-lift ranges; 2 sessions for every competition
 * lift; the fixed weekday spreads of [suggestedDays]. The Cable crunch at the end of both 5-day Legs days is the
 * owner's addition (2026-11-18).
 *
 * Every lift that appears on more than one day has the same sets x reps @ RPE on each (settled rule 1), and every
 * variation shares 3 x 5 at RPE 8.
 */
object PlanTemplates {
    // ---- the numbers ----

    /** Every powerlifting main lift and variation, and every bodybuilding entry: RPE 8 (inside PMC8435792's 7.5-9.5). */
    private const val RPE = 8.0

    /** Powerlifting accessories: 3 x 10 at RPE 8, the designer's fixed count, settled by the owner on 2026-11-18. */
    const val ACCESSORY_SETS = 3
    const val ACCESSORY_REPS = 10

    /** Variations on every powerlifting template: 3 x 5 at RPE 8, inside the 3-day main lifts' sourced ranges. */
    private const val VARIATION_SETS = 3
    private const val VARIATION_REPS = 5

    private fun lift(name: String, sets: Int, reps: Int, strictPressOnly: Boolean = false) =
        TemplateExercise(name, sets, reps, null, RPE, strictPressOnly)

    private fun variation(name: String) = TemplateExercise(name, VARIATION_SETS, VARIATION_REPS, null, RPE)

    private fun acc(name: String) = TemplateExercise(name, ACCESSORY_SETS, ACCESSORY_REPS, null, RPE)

    /** A bodybuilding entry: [sets] x 8-12 at RPE 8. */
    private fun bb(name: String, sets: Int = PlanBuilder.SETS) =
        TemplateExercise(name, sets, PlanBuilder.REPS_LOW, PlanBuilder.REPS_HIGH, PlanBuilder.RPE)

    private fun day(title: String, vararg exercises: TemplateExercise) = TemplateSession(title, exercises.toList())

    // ---- powerlifting ----

    /** Main lifts 3 x 5 at RPE 8, twice a week each: 6 sets a week per lift. */
    private val PL_3: PlanTemplate = run {
        fun main(name: String, sp: Boolean = false) = lift(name, 3, 5, sp)
        PlanTemplate(
            "pl-3", TrainingMode.POWERLIFTING, "3-day powerlifting", "New to powerlifting: 2–3 days a week",
            listOf(
                day("Squat & bench", main("Squat"), main("Bench"), acc("Seated cable row"), acc("Cable crunch")),
                day("Deadlift & bench", main("Deadlift"), main("Bench"), main("Strict press", true), acc("Lat pulldown"), acc("Triceps pushdown")),
                day("Squat & deadlift", main("Squat"), main("Deadlift"), main("Strict press", true), acc("Lying leg curl"), acc("Face pull")),
            ),
        )
    }

    /** Main lifts 4 x 4 at RPE 8, twice a week each: 8 sets a week per lift. */
    private val PL_4: PlanTemplate = run {
        fun main(name: String, sp: Boolean = false) = lift(name, 4, 4, sp)
        PlanTemplate(
            "pl-4", TrainingMode.POWERLIFTING, "4-day powerlifting", "Some training behind you: 3–4 days a week",
            listOf(
                day("Squat & bench", main("Squat"), main("Bench"), acc("Seated cable row"), acc("Cable crunch")),
                day("Deadlift", main("Deadlift"), main("Strict press", true), variation("Close-grip bench"), acc("Lat pulldown"), acc("Lying leg curl")),
                day("Squat & bench", main("Squat"), main("Bench"), acc("One-arm DB row"), acc("Face pull")),
                day("Deadlift", main("Deadlift"), main("Strict press", true), variation("Pause squat"), acc("Back extension"), acc("Triceps pushdown")),
            ),
        )
    }

    /** Main lifts 5 x 3 at RPE 8, twice a week each: 10 sets a week per lift. */
    private val PL_5: PlanTemplate = run {
        fun main(name: String, sp: Boolean = false) = lift(name, 5, 3, sp)
        PlanTemplate(
            "pl-5", TrainingMode.POWERLIFTING, "5-day powerlifting", "Experienced, a year or more of training: 4–6 days a week",
            listOf(
                day("Squat & bench", main("Squat"), main("Bench"), acc("Seated cable row"), acc("Cable crunch")),
                day("Deadlift", main("Deadlift"), main("Strict press", true), variation("Close-grip bench"), acc("Lat pulldown"), acc("Lying leg curl")),
                day("Variations", variation("Pause squat"), variation("Spoto press"), acc("Chest-supported row"), acc("Face pull"), acc("Hammer curl")),
                day("Squat & bench", main("Squat"), main("Bench"), acc("One-arm DB row"), acc("Triceps pushdown")),
                day("Deadlift", main("Deadlift"), main("Strict press", true), variation("Long-pause bench"), acc("Back extension"), acc("Cable crunch")),
            ),
        )
    }

    // ---- bodybuilding (every entry 8-12 at RPE 8) ----

    private val BB_3 = PlanTemplate(
        "bb-3", TrainingMode.BODYBUILDING, "3-day full body", "New to lifting: 2–3 days a week, the whole body each day",
        listOf(
            day(
                "Full body A", bb("Leg press"), bb("DB press", 4), bb("Lat pulldown"), bb("Seated leg curl"), bb("Lateral raise"),
                bb("DB curl"), bb("Rope pushdown"),
            ),
            day(
                "Full body B", bb("DB RDL"), bb("Incline DB press"), bb("Seated cable row", 4), bb("Leg extension"), bb("Face pull"),
                bb("Standing calf raise"), bb("Cable crunch"),
            ),
            day(
                "Full body C", bb("Hack squat", 4), bb("Machine chest press"), bb("Chin-up"), bb("Lying leg curl", 4), bb("Face pull"),
                bb("Triceps pushdown"), bb("Cable curl"),
            ),
        ),
    )

    private val BB_4 = PlanTemplate(
        "bb-4", TrainingMode.BODYBUILDING, "4-day upper/lower", "Some training behind you: 3–4 days a week",
        listOf(
            day(
                "Upper A", bb("DB press", 4), bb("Seated cable row", 4), bb("DB shoulder press"), bb("Lat pulldown"), bb("Lateral raise"),
                bb("Face pull"), bb("DB curl"), bb("Rope pushdown"),
            ),
            day("Lower A", bb("Leg press", 4), bb("RDL"), bb("Leg extension"), bb("Seated leg curl"), bb("Standing calf raise"), bb("Cable crunch")),
            day(
                "Upper B", bb("Incline DB press", 4), bb("Machine row"), bb("Chin-up", 4), bb("Cable crossover"), bb("Lateral raise"),
                bb("Face pull"), bb("Cable curl"), bb("Triceps pushdown"),
            ),
            day(
                "Lower B", bb("Hack squat", 4), bb("Hip thrust"), bb("Lying leg curl", 4), bb("Bulgarian split squat"),
                bb("Standing calf raise"), bb("Cable crunch"),
            ),
        ),
    )

    private val BB_5 = PlanTemplate(
        "bb-5", TrainingMode.BODYBUILDING, "5-day body-part split", "Experienced, a year or more of training: 4–6 days a week",
        listOf(
            day("Chest & back A", bb("Machine chest press"), bb("Wide-grip cable row"), bb("Lat pulldown"), bb("Cable crossover")),
            // The Cable crunch is the owner's addition (2026-11-18): abs on top of the day's big groups.
            day(
                "Legs A", bb("Leg press", 4), bb("RDL"), bb("Leg extension"), bb("Seated leg curl"), bb("Standing calf raise"),
                bb("Cable crunch"),
            ),
            day(
                "Shoulders & arms", bb("Lateral raise", 4), bb("Cable lateral raise"), bb("DB curl"), bb("Rope pushdown"), bb("Cable curl"),
                bb("Triceps pushdown"),
            ),
            day("Chest & back B", bb("Incline DB press"), bb("T-bar row"), bb("Neutral-grip pull-up"), bb("Pec deck")),
            // As on Legs A, the Cable crunch is the owner's addition.
            day("Legs B", bb("Hack squat", 4), bb("Hip thrust"), bb("Lying leg curl", 4), bb("Bulgarian split squat"), bb("Cable crunch")),
        ),
    )

    private val ALL: List<PlanTemplate> = listOf(PL_3, PL_4, PL_5, BB_3, BB_4, BB_5)

    /** The reason a template exercise missing from the catalogue is left out (it cannot happen while entries are never deleted). */
    const val NOT_IN_CATALOGUE = "Not in the catalogue."

    /** [mode]'s templates, fewest days first. */
    fun forMode(mode: TrainingMode): List<PlanTemplate> = ALL.filter { it.mode == mode }.sortedBy { it.dayCount }

    fun byId(id: String): PlanTemplate? = ALL.firstOrNull { it.id == id }

    /** The designer's spreads of n training days, Monday first. */
    private val SPREADS: Map<Int, List<DayOfWeek>> = mapOf(
        1 to listOf(DayOfWeek.MONDAY),
        2 to listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        3 to listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        4 to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        5 to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
        6 to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
        7 to DayOfWeek.entries.toList(),
    )

    /** The weekdays to tick first: [current] (Monday first) when it has [count] days, otherwise a fixed spread. */
    fun suggestedDays(count: Int, current: Set<DayOfWeek>): List<DayOfWeek> {
        require(count in 1..7) { "count: $count; 1 to 7 days" }
        return if (current.size == count) current.sortedBy { it.ordinal } else SPREADS.getValue(count)
    }

    /**
     * [template] as a draft week: [days] sorted Monday first, session i on day i. Each exercise carries its catalogue
     * muscles, compound flag, main group and slot, so the volume is counted by the builder's rules; powerlifting's fixed
     * reps give repsHigh = repsLow; strict-press-only entries are dropped unless [strictPress]. Throws
     * IllegalArgumentException unless [days] are [PlanTemplate.dayCount] distinct weekdays.
     */
    fun draft(template: PlanTemplate, days: List<DayOfWeek>, strictPress: Boolean): PlanDraft {
        require(days.size == template.dayCount) { "days: ${days.size} days; ${template.name} has ${template.dayCount}" }
        require(days.toSet().size == days.size) { "days: a weekday is repeated" }
        val week = days.sortedBy { it.ordinal }
        val leftOut = mutableListOf<LeftOut>()
        val sessions = template.sessions.mapIndexed { i, session ->
            val exercises = session.exercises.filter { strictPress || !it.onlyWithStrictPress }.mapNotNull { e ->
                val entry = Library.find(e.name)
                if (entry == null) leftOut += LeftOut(e.name, NOT_IN_CATALOGUE)
                entry?.let { e to it }
            }.mapIndexed { j, (e, entry) ->
                val compound = entry.kind == Kind.COMPOUND
                DraftExercise(
                    entry.name, true, e.sets, e.repsLow, e.repsHigh ?: e.repsLow, e.rpe, PlanBuilder.mainGroup(entry.muscles),
                    entry.muscles, compound, PlanBuilder.kindOf(entry.name, entry.muscles, compound, true), j == 0,
                )
            }
            DraftSession(week[i], session.title, exercises)
        }
        return PlanDraft(sessions, PlanBuilder.volume(sessions), leftOut)
    }

    private val MAIN_LIFTS = setOf(Category.SQUAT, Category.BENCH, Category.DEADLIFT, Category.STRICT_PRESS)

    /**
     * What [name] on [day] of [draft] (made from [template]) can be swapped for. None for a main lift. For a variation,
     * the other catalogue variations of the same lift. Otherwise the builder's list (no picks) without main lifts and
     * variations. Never an exercise already anywhere in the week, so a swap cannot put one lift on two days with
     * different numbers. Empty when [name] is not on [day].
     */
    fun alternatives(template: PlanTemplate, draft: PlanDraft, day: DayOfWeek, name: String): List<LibraryExercise> {
        require(draft.sessions.size == template.dayCount) { "draft: ${draft.sessions.size} days; ${template.name} has ${template.dayCount}" }
        val key = Library.normalise(name)
        val session = draft.sessions.firstOrNull { it.day == day } ?: return emptyList()
        if (session.exercises.none { Library.normalise(it.name) == key }) return emptyList()
        val entry = Library.find(name) ?: return emptyList()
        val week = draft.sessions.flatMap { s -> s.exercises.map { Library.normalise(it.name) } }.toSet()
        fun free(e: LibraryExercise) = Library.normalise(e.name) !in week
        return when (entry.category) {
            in MAIN_LIFTS -> emptyList()
            Category.VARIATION -> Library.all().filter { it.category == Category.VARIATION && it.parentLift == entry.parentLift && free(it) }
            else -> PlanBuilder.alternatives(name, draft, emptyList(), day).filter { it.category == Category.ACCESSORY && free(it) }
        }
    }

    /** [PlanBuilder.swap]: [from] on [day] becomes [to], with the same sets, reps and RPE; the volume is recounted. */
    fun swap(draft: PlanDraft, day: DayOfWeek, from: String, to: String): PlanDraft = PlanBuilder.swap(draft, day, from, to)
}
