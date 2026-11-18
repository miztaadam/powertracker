package com.powertracker.domain.plan

import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.SATURDAY
import kotlinx.datetime.DayOfWeek.THURSDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlanTemplatesTest {
    private val all = TrainingMode.entries.flatMap { PlanTemplates.forMode(it) }
    private val mainCategories = setOf(Category.SQUAT, Category.BENCH, Category.DEADLIFT, Category.STRICT_PRESS)
    private val competition = setOf(Category.SQUAT, Category.BENCH, Category.DEADLIFT)

    /** Settled rule 3's big groups. */
    private val bigGroups = listOf(
        MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
        MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS, MuscleGroup.TRICEPS,
    )

    /** The first [n] weekdays of the fixed spreads, enough for any template. */
    private fun days(n: Int): List<DayOfWeek> = DayOfWeek.entries.take(n)

    private fun drafts(template: PlanTemplate): List<PlanDraft> =
        listOf(false, true).map { PlanTemplates.draft(template, days(template.dayCount), it) }

    private fun category(name: String): Category = Library.find(name)!!.category

    /** Every exercise that appears more than once in the week has the same sets, reps and RPE each time. */
    private fun assertSameNumbers(draft: PlanDraft, context: String) {
        val byName = draft.sessions.flatMap { it.exercises }.groupBy { Library.normalise(it.name) }
        for ((name, entries) in byName) {
            val numbers = entries.map { listOf(it.sets, it.repsLow, it.repsHigh, it.rpe) }.distinct()
            assertEquals(1, numbers.size, "$context: '$name' has different numbers on different days: $numbers")
        }
    }

    @Test
    fun everyTemplateExerciseIsACatalogueNameWithASlot() {
        for (t in all) for (s in t.sessions) for (e in s.exercises) {
            val entry = assertNotNull(Library.find(e.name), "${t.id}: '${e.name}' is not in the catalogue")
            assertEquals(e.name, entry.name, "${t.id}: '${e.name}' is an alias, not the catalogue name")
            assertNotNull(
                PlanBuilder.kindOf(entry.name, entry.muscles, entry.kind == Kind.COMPOUND, true),
                "${t.id}: '${e.name}' has no slot kind",
            )
        }
        for (t in all) for (d in drafts(t)) {
            assertTrue(d.leftOut.isEmpty(), "${t.id}: ${d.leftOut}")
            for (s in d.sessions) for (e in s.exercises) {
                assertNotNull(e.slot, "${t.id}: '${e.name}' has no slot")
                assertTrue(e.inCatalogue)
                assertEquals(PlanBuilder.mainGroup(Library.find(e.name)!!.muscles), e.mainGroup)
            }
        }
    }

    @Test
    fun forModeGivesTheAgreedTemplatesFewestDaysFirst() {
        val pl = PlanTemplates.forMode(TrainingMode.POWERLIFTING)
        assertEquals(listOf("pl-3", "pl-4", "pl-5"), pl.map { it.id })
        assertEquals(listOf("3-day powerlifting", "4-day powerlifting", "5-day powerlifting"), pl.map { it.name })
        assertEquals(listOf(3, 4, 5), pl.map { it.dayCount })
        assertEquals(
            listOf(
                listOf("Squat & bench", "Deadlift & bench", "Squat & deadlift"),
                listOf("Squat & bench", "Deadlift", "Squat & bench", "Deadlift"),
                listOf("Squat & bench", "Deadlift", "Variations", "Squat & bench", "Deadlift"),
            ),
            pl.map { t -> t.sessions.map { it.title } },
        )
        val bb = PlanTemplates.forMode(TrainingMode.BODYBUILDING)
        assertEquals(listOf("bb-3", "bb-4", "bb-5"), bb.map { it.id })
        assertEquals(listOf("3-day full body", "4-day upper/lower", "5-day body-part split"), bb.map { it.name })
        assertEquals(listOf(3, 4, 5), bb.map { it.dayCount })
        assertEquals(
            listOf(
                listOf("Full body A", "Full body B", "Full body C"),
                listOf("Upper A", "Lower A", "Upper B", "Lower B"),
                listOf("Chest & back A", "Legs A", "Shoulders & arms", "Chest & back B", "Legs B"),
            ),
            bb.map { t -> t.sessions.map { it.title } },
        )
        for (t in all) {
            assertEquals(t, PlanTemplates.byId(t.id))
            assertTrue(t.suits.isNotBlank())
        }
        assertEquals(null, PlanTemplates.byId("nope"))
    }

    @Test
    fun theOwnersCableCrunchEndsBothLegsDaysOfTheFiveDaySplit() {
        val bb5 = PlanTemplates.byId("bb-5")!!
        for (title in listOf("Legs A", "Legs B")) {
            val last = bb5.sessions.first { it.title == title }.exercises.last()
            assertEquals(TemplateExercise("Cable crunch", 3, 8, 12, 8.0), last, title)
        }
    }

    @Test
    fun eachSessionRunsMainLiftsThenVariationsThenCompoundsThenAccessories() {
        fun rank(name: String): Int {
            val e = Library.find(name)!!
            return when {
                e.category in mainCategories -> 0
                e.category == Category.VARIATION -> 1
                e.kind == Kind.COMPOUND -> 2
                else -> 3
            }
        }
        for (t in all) for (s in t.sessions) {
            val ranks = s.exercises.map { rank(it.name) }
            assertEquals(ranks.sorted(), ranks, "${t.id} ${s.title}: ${s.exercises.map { it.name }}")
        }
    }

    @Test
    fun bodybuildingVolumeMeetsSettledRuleThree() {
        for (t in PlanTemplates.forMode(TrainingMode.BODYBUILDING)) {
            val draft = PlanTemplates.draft(t, days(t.dayCount), strictPress = false)
            for (group in bigGroups) {
                val v = draft.volume.first { it.group == group }
                assertTrue(v.weeklySets >= PlanBuilder.WEEKLY_SETS, "${t.id}: $group has ${v.weeklySets} sets")
                assertTrue(v.days >= 2, "${t.id}: $group is on ${v.days} days")
            }
            // The ceiling, counted the way PhysiqueCoach counts it, over every group (front delts are outside volume).
            val exercises = mutableMapOf<Long, Exercise>()
            val items = mutableListOf<PlanItem>()
            for (s in draft.sessions) for ((i, e) in s.exercises.withIndex()) {
                val entry = Library.find(e.name)!!
                val id = exercises.values.firstOrNull { it.name == entry.name }?.id ?: (exercises.size + 1L).also {
                    exercises[it] = Exercise(it, entry.name, entry.category, entry.parentLift, entry.muscles, true)
                }
                items += PlanItem(items.size + 1L, s.day, id, e.sets, e.repsLow, e.rpe, i, TrainingMode.BODYBUILDING, e.repsHigh)
            }
            for (group in MuscleGroup.entries) {
                val sets = PhysiqueCoach.weeklySets(items, exercises, group)
                assertTrue(sets <= PhysiqueCoach.WEEKLY_CEILING, "${t.id}: $group has $sets counted sets")
            }
        }
    }

    @Test
    fun theFiveDaySplitTrainsAtMostThreeBigGroupsADayPlusTheOwnersAbs() {
        val bb5 = PlanTemplates.byId("bb-5")!!
        val draft = PlanTemplates.draft(bb5, days(5), strictPress = false)
        for (s in draft.sessions) {
            val groups = s.exercises.mapNotNull { it.mainGroup }.toSet()
            assertTrue((groups intersect bigGroups.toSet()).size <= 3, "${s.title}: $groups")
            if (s.title.startsWith("Legs")) assertTrue(MuscleGroup.ABS in groups, "${s.title}: $groups")
        }
        assertEquals("Experienced, a year or more of training: 4–6 days a week", bb5.suits)
    }

    @Test
    fun powerliftingMeetsItsSourcedSetsAndSessionsPerLift() {
        val floor = mapOf("pl-3" to 6, "pl-4" to 8, "pl-5" to 10)
        for (t in PlanTemplates.forMode(TrainingMode.POWERLIFTING)) for (draft in drafts(t)) {
            for (cat in competition) {
                val sessions = draft.sessions.filter { s -> s.exercises.any { category(it.name) == cat } }
                val entries = draft.sessions.flatMap { s -> s.exercises.filter { category(it.name) == cat } }
                assertTrue(entries.sumOf { it.sets } >= floor.getValue(t.id), "${t.id}: $cat has ${entries.sumOf { it.sets }} sets")
                assertTrue(sessions.size in 2..3, "${t.id}: $cat on ${sessions.size} sessions")
                for (e in entries) {
                    assertTrue(e.repsLow in 1..6, "${t.id}: ${e.name} ${e.repsLow} reps")
                    assertTrue(e.rpe in 7.5..9.5, "${t.id}: ${e.name} RPE ${e.rpe}")
                }
            }
        }
    }

    @Test
    fun powerliftingRepsAreFixedAndBodybuildingRepsARange() {
        for (t in all) for (draft in drafts(t)) for (s in draft.sessions) for (e in s.exercises) {
            if (t.mode == TrainingMode.POWERLIFTING) {
                assertEquals(e.repsLow, e.repsHigh, "${t.id}: ${e.name}")
            } else {
                assertEquals(8 to 12, e.repsLow to e.repsHigh, "${t.id}: ${e.name}")
            }
        }
        for (t in PlanTemplates.forMode(TrainingMode.POWERLIFTING)) for (s in t.sessions) for (e in s.exercises) {
            assertEquals(null, e.repsHigh, "${t.id}: ${e.name}")
        }
    }

    @Test
    fun powerliftingAccessoriesAreTheDesignersThreeByTenAtEight() {
        for (t in PlanTemplates.forMode(TrainingMode.POWERLIFTING)) for (s in t.sessions) for (e in s.exercises) {
            if (category(e.name) == Category.ACCESSORY) assertEquals(TemplateExercise(e.name, 3, 10, null, 8.0), e, t.id)
        }
    }

    @Test
    fun aLiftOnTwoDaysHasTheSameNumbersOnBoth() {
        for (t in all) for (draft in drafts(t)) assertSameNumbers(draft, t.id)
    }

    @Test
    fun theDraftPutsSessionIOnTheIthDayMondayFirst() {
        val pl3 = PlanTemplates.byId("pl-3")!!
        val draft = PlanTemplates.draft(pl3, listOf(FRIDAY, MONDAY, WEDNESDAY), strictPress = false)
        assertEquals(listOf(MONDAY, WEDNESDAY, FRIDAY), draft.sessions.map { it.day })
        assertEquals(pl3.sessions.map { it.title }, draft.sessions.map { it.title })
        assertEquals(listOf("Squat", "Bench", "Seated cable row", "Cable crunch"), draft.sessions[0].exercises.map { it.name })
        assertEquals(DraftExercise("Squat", true, 3, 5, 5, 8.0, draft.sessions[0].exercises[0].mainGroup,
            Library.find("Squat")!!.muscles, true, PlanBuilder.SlotKind.QUAD_COMPOUND, true), draft.sessions[0].exercises[0])
    }

    @Test
    fun draftRejectsTheWrongNumberOfDaysAndRepeatedDays() {
        val pl4 = PlanTemplates.byId("pl-4")!!
        assertFailsWith<IllegalArgumentException> { PlanTemplates.draft(pl4, listOf(MONDAY, WEDNESDAY, FRIDAY), false) }
        assertFailsWith<IllegalArgumentException> { PlanTemplates.draft(pl4, days(5), false) }
        assertFailsWith<IllegalArgumentException> { PlanTemplates.draft(pl4, listOf(MONDAY, MONDAY, WEDNESDAY, FRIDAY), false) }
    }

    @Test
    fun suggestedDaysKeepsTheCurrentDaysWhenTheCountMatchesElseAFixedSpread() {
        val mwf = setOf(MONDAY, WEDNESDAY, FRIDAY)
        assertEquals(listOf(MONDAY, WEDNESDAY, FRIDAY), PlanTemplates.suggestedDays(3, mwf))
        assertEquals(listOf(MONDAY, TUESDAY, THURSDAY, FRIDAY), PlanTemplates.suggestedDays(4, mwf))
        assertEquals(listOf(MONDAY, TUESDAY, WEDNESDAY, FRIDAY, SATURDAY), PlanTemplates.suggestedDays(5, mwf))
        assertEquals(listOf(MONDAY, WEDNESDAY, FRIDAY), PlanTemplates.suggestedDays(3, emptySet()))
        assertEquals(listOf(TUESDAY, THURSDAY, SATURDAY), PlanTemplates.suggestedDays(3, setOf(SATURDAY, TUESDAY, THURSDAY)))
    }

    @Test
    fun strictPressEntriesAppearOnlyWithStrictPressOn() {
        for (t in all) {
            val (off, on) = drafts(t)
            val offCats = off.sessions.flatMap { s -> s.exercises.map { category(it.name) } }
            assertTrue(Category.STRICT_PRESS !in offCats, t.id)
            val marked = t.sessions.flatMap { s -> s.exercises.filter { it.onlyWithStrictPress } }
            assertTrue(marked.all { category(it.name) == Category.STRICT_PRESS }, t.id)
            val onPress = on.sessions.count { s -> s.exercises.any { category(it.name) == Category.STRICT_PRESS } }
            assertEquals(if (t.mode == TrainingMode.POWERLIFTING) 2 else 0, onPress, t.id)
            assertEquals(
                on.sessions.sumOf { it.exercises.size } - marked.size,
                off.sessions.sumOf { it.exercises.size },
                t.id,
            )
        }
    }

    @Test
    fun aMainLiftHasNoAlternatives() {
        for (t in PlanTemplates.forMode(TrainingMode.POWERLIFTING)) for (draft in drafts(t)) for (s in draft.sessions) {
            for (e in s.exercises.filter { category(it.name) in mainCategories }) {
                assertEquals(emptyList(), PlanTemplates.alternatives(t, draft, s.day, e.name), "${t.id} ${e.name}")
            }
        }
    }

    @Test
    fun aVariationSwapsOnlyToOtherVariationsOfItsLiftNotInTheWeek() {
        val pl5 = PlanTemplates.byId("pl-5")!!
        val draft = PlanTemplates.draft(pl5, days(5), strictPress = false)
        val day = draft.sessions.first { it.title == "Variations" }.day
        val alts = PlanTemplates.alternatives(pl5, draft, day, "Spoto press")
        assertTrue(alts.isNotEmpty())
        assertTrue(alts.all { it.category == Category.VARIATION && it.parentLift == Library.find("Spoto press")!!.parentLift })
        val week = draft.sessions.flatMap { s -> s.exercises.map { it.name } }.toSet()
        assertTrue(alts.none { it.name in week }, "offers a variation already in the week")
        // Close-grip bench and Long-pause bench are bench variations elsewhere in the week, so they are not offered.
        assertTrue(alts.none { it.name == "Close-grip bench" || it.name == "Long-pause bench" })
        val expected = Library.all().filter {
            it.category == Category.VARIATION && it.parentLift == Library.find("Spoto press")!!.parentLift && it.name !in week
        }
        assertEquals(expected, alts)
    }

    @Test
    fun anAccessoryGetsTheBuildersListLessMainLiftsVariationsAndTheRestOfTheWeek() {
        // DB press on bb-3's first day: its builder list holds Bench (a main lift) and Machine chest press (on day 3).
        val bb3 = PlanTemplates.byId("bb-3")!!
        val draft = PlanTemplates.draft(bb3, days(3), strictPress = false)
        val day = draft.sessions[0].day
        val builder = PlanBuilder.alternatives("DB press", draft, emptyList(), day)
        assertTrue(builder.any { it.category in mainCategories }, "the builder's list should hold a main lift here")
        assertTrue(builder.any { it.name == "Machine chest press" })
        val week = draft.sessions.flatMap { s -> s.exercises.map { it.name } }.toSet()
        val expected = builder.filter { it.category == Category.ACCESSORY && it.name !in week }
        val alts = PlanTemplates.alternatives(bb3, draft, day, "DB press")
        assertTrue(alts.isNotEmpty())
        assertEquals(expected, alts)

        // A powerlifting accessory follows the same rule.
        val pl3 = PlanTemplates.byId("pl-3")!!
        val plDraft = PlanTemplates.draft(pl3, days(3), strictPress = true)
        val plDay = plDraft.sessions[0].day
        val plWeek = plDraft.sessions.flatMap { s -> s.exercises.map { it.name } }.toSet()
        val plBuilder = PlanBuilder.alternatives("Seated cable row", plDraft, emptyList(), plDay)
        assertEquals(
            plBuilder.filter { it.category == Category.ACCESSORY && it.name !in plWeek },
            PlanTemplates.alternatives(pl3, plDraft, plDay, "Seated cable row"),
        )
    }

    @Test
    fun anExerciseNotOnThatDayHasNoAlternatives() {
        val pl3 = PlanTemplates.byId("pl-3")!!
        val draft = PlanTemplates.draft(pl3, days(3), strictPress = false)
        assertEquals(emptyList(), PlanTemplates.alternatives(pl3, draft, draft.sessions[0].day, "Lat pulldown"))
    }

    @Test
    fun swapKeepsSetsRepsAndRpe() {
        val pl4 = PlanTemplates.byId("pl-4")!!
        val draft = PlanTemplates.draft(pl4, days(4), strictPress = false)
        val day = draft.sessions[1].day
        val before = draft.sessions[1].exercises.first { it.name == "Close-grip bench" }
        val to = PlanTemplates.alternatives(pl4, draft, day, before.name).first()
        val after = PlanTemplates.swap(draft, day, before.name, to.name).sessions[1].exercises
            .first { it.name == to.name }
        assertEquals(listOf(before.sets, before.repsLow, before.repsHigh, before.rpe), listOf(after.sets, after.repsLow, after.repsHigh, after.rpe))
        assertEquals(draft.sessions[1].exercises.indexOf(before), PlanTemplates.swap(draft, day, before.name, to.name).sessions[1].exercises.indexOf(after))
    }

    @Test
    fun noOfferedSwapIsAMainLiftOrBreaksTheSameNumbersRule() {
        var swaps = 0
        for (t in all) for (draft in drafts(t)) for (s in draft.sessions) for (e in s.exercises) {
            val isVariation = category(e.name) == Category.VARIATION
            for (alt in PlanTemplates.alternatives(t, draft, s.day, e.name)) {
                assertTrue(alt.category !in mainCategories, "${t.id} ${e.name} -> ${alt.name}: a main lift")
                if (!isVariation) assertTrue(alt.category != Category.VARIATION, "${t.id} ${e.name} -> ${alt.name}: a variation")
                val swapped = PlanTemplates.swap(draft, s.day, e.name, alt.name)
                assertSameNumbers(swapped, "${t.id} ${e.name} -> ${alt.name}")
                swaps++
            }
        }
        assertTrue(swaps > 0)
    }
}
