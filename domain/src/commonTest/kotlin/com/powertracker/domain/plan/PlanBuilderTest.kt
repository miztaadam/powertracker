package com.powertracker.domain.plan

import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.SATURDAY
import kotlinx.datetime.DayOfWeek.THURSDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.DatePeriod
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PlanBuilderTest {
    private val three = listOf(MONDAY, WEDNESDAY, FRIDAY)
    private val four = listOf(MONDAY, TUESDAY, THURSDAY, FRIDAY)

    private fun pick(e: LibraryExercise) = BuilderPick(e.name, e.muscles, e.kind == Kind.COMPOUND, e.equipment, true)

    private fun picks(count: Int, seed: Int): List<BuilderPick> =
        Library.all().shuffled(Random(seed)).take(count).map(::pick)

    private fun build(days: List<DayOfWeek>, picks: List<BuilderPick> = emptyList(), priorities: List<MuscleGroup> = emptyList(), variant: Int = 0) =
        PlanBuilder.build(BuilderAnswers(days, picks, priorities, variant))

    private fun key(name: String) = Library.normalise(name)

    /** Every answer the property checks run over. */
    private val answers: List<BuilderAnswers> by lazy {
        val prioritySets = listOf(
            emptyList(),
            listOf(MuscleGroup.SIDE_DELTS),
            listOf(MuscleGroup.CALVES, MuscleGroup.CHEST),
            listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.REAR_DELTS, MuscleGroup.TRAPS),
        )
        buildList {
            for (days in listOf(three, four)) for ((i, count) in listOf(0, 5, 12, 40).withIndex()) {
                for ((p, priorities) in prioritySets.withIndex()) for (variant in 0..3) {
                    add(BuilderAnswers(days, picks(count, seed = 7 + i * 31 + p), priorities, variant))
                }
            }
        }
    }

    private fun recount(sessions: List<DraftSession>, group: MuscleGroup): Pair<Int, Int> {
        var sets = 0
        var days = 0
        for (s in sessions) {
            val here = s.exercises.filter { MuscleGroups.works(it.muscles, group) }
            sets += here.sumOf { it.sets }
            if (here.isNotEmpty()) days++
        }
        return sets to days
    }

    // ---- properties over many answers ----

    @Test
    fun theSameAnswersGiveAnEqualDraft() {
        for (a in answers) assertEquals(PlanBuilder.build(a), PlanBuilder.build(a.copy()), "$a")
    }

    @Test
    fun everySessionHoldsOneToSixExercisesWithNoRepeat() {
        for (a in answers) {
            val draft = PlanBuilder.build(a)
            assertEquals(a.days.size, draft.sessions.size)
            assertEquals(a.days.sortedBy { it.ordinal }, draft.sessions.map { it.day })
            for (s in draft.sessions) {
                assertTrue(s.exercises.size in 1..PlanBuilder.MAX_EXERCISES, "${s.day} has ${s.exercises.size}: $a")
                assertEquals(s.exercises.size, s.exercises.map { key(it.name) }.toSet().size, "repeat on ${s.day}: $a")
            }
        }
    }

    @Test
    fun everyExerciseIsAPickOrInTheCatalogue() {
        for (a in answers) {
            val pickNames = a.picks.map { key(it.name) }.toSet()
            for (e in PlanBuilder.build(a).sessions.flatMap { it.exercises }) {
                assertTrue(key(e.name) in pickNames || (e.inCatalogue && Library.find(e.name) != null), "${e.name}: $a")
                if (e.inCatalogue) assertEquals(Library.find(e.name)!!.muscles, e.muscles)
            }
        }
    }

    @Test
    fun everyPickIsPlacedOrLeftOutNeverBothNeverNeither() {
        for (a in answers) {
            val draft = PlanBuilder.build(a)
            val placed = draft.sessions.flatMap { s -> s.exercises.map { key(it.name) } }.toSet()
            val left = draft.leftOut.map { key(it.name) }
            assertEquals(left.size, left.toSet().size)
            for (p in a.picks) {
                val inDraft = key(p.name) in placed
                val out = key(p.name) in left
                assertTrue(inDraft != out, "${p.name} placed=$inDraft leftOut=$out: $a")
            }
            assertTrue(draft.leftOut.all { it.reason == PlanBuilder.NO_ROOM })
        }
    }

    @Test
    fun onFourDaysEveryExerciseSitsInASessionThatAllowsItsMainGroup() {
        for (a in answers.filter { it.days.size == 4 }) {
            val draft = PlanBuilder.build(a)
            assertEquals(listOf("Upper", "Lower", "Upper", "Lower"), draft.sessions.map { it.title })
            for (s in draft.sessions) for (e in s.exercises) {
                val g = e.mainGroup ?: continue
                val allowed = if (s.title == "Upper") PlanBuilder.UPPER else PlanBuilder.LOWER
                assertTrue(g in allowed, "${e.name} ($g) on ${s.title}: $a")
            }
        }
        for (a in answers.filter { it.days.size == 3 }) {
            assertTrue(PlanBuilder.build(a).sessions.all { it.title == "Full body" })
        }
    }

    @Test
    fun priorityPartFirstThenCompoundsBeforeAccessoriesInEachPart() {
        for (a in answers) for (s in PlanBuilder.build(a).sessions) {
            val isPriority = s.exercises.map { it.mainGroup in a.priorities }
            assertEquals(isPriority.sortedDescending(), isPriority, "priority part not first on ${s.day}: $a")
            for (part in s.exercises.partition { it.mainGroup in a.priorities }.toList()) {
                val compound = part.map { it.compound }
                assertEquals(compound.sortedDescending(), compound, "accessory before compound on ${s.day}: $a")
            }
        }
    }

    @Test
    fun everyPrescriptionIsEightToTwelveAtEightWithFourSetsExactlyForAPriority() {
        for (a in answers) for (e in PlanBuilder.build(a).sessions.flatMap { it.exercises }) {
            assertEquals(8, e.repsLow)
            assertEquals(12, e.repsHigh)
            assertEquals(8.0, e.rpe)
            assertEquals(if (e.mainGroup in a.priorities) 4 else 3, e.sets, "${e.name}: $a")
        }
    }

    @Test
    fun volumeMatchesARecountAndShortMeansUnderTen() {
        for (a in answers) {
            val draft = PlanBuilder.build(a)
            val expectedGroups = PlanBuilder.TARGET_GROUPS + a.priorities.filter { it !in PlanBuilder.TARGET_GROUPS }
            assertEquals(expectedGroups, draft.volume.map { it.group })
            for (v in draft.volume) {
                assertEquals(recount(draft.sessions, v.group), v.weeklySets to v.days, "${v.group}: $a")
                assertEquals(v.weeklySets < PlanBuilder.WEEKLY_SETS, v.short)
            }
        }
    }

    // ---- fixed cases ----

    /** Weekly sets and days per target group with no picks, as the rules give them (see the stage 2 task 1 report). */
    private fun table(draft: PlanDraft) = draft.volume.associate { it.group to (it.weeklySets to it.days) }

    @Test
    fun noPicksOnThreeDaysFillsThreeSessionsOfSix() {
        val draft = build(three)
        assertEquals(listOf(6, 6, 6), draft.sessions.map { it.exercises.size })
        assertTrue(draft.leftOut.isEmpty())
        assertEquals(NO_PICKS_THREE, table(draft))
        // The frequency aim is reached; the sets aim is not: 18 places of 3 sets cannot give 12 groups 10 each.
        assertTrue(draft.volume.all { it.days >= 2 })
    }

    @Test
    fun noPicksOnFourDaysFillsFourSessionsOfSix() {
        val draft = build(four)
        assertEquals(listOf(6, 6, 6, 6), draft.sessions.map { it.exercises.size })
        assertEquals(NO_PICKS_FOUR, table(draft))
        assertTrue(draft.volume.all { it.days >= 2 })
    }

    @Test
    fun aSideDeltPriorityComesFirstWithFourSets() {
        for (days in listOf(three, four)) {
            val without = build(days)
            val with = build(days, priorities = listOf(MuscleGroup.SIDE_DELTS))
            val sessionsWithSideDelts = with.sessions.filter { s -> s.exercises.any { it.mainGroup == MuscleGroup.SIDE_DELTS } }
            assertTrue(sessionsWithSideDelts.isNotEmpty())
            for (s in sessionsWithSideDelts) {
                assertEquals(MuscleGroup.SIDE_DELTS, s.exercises.first().mainGroup, "${s.day}")
                assertEquals(4, s.exercises.first().sets)
            }
        }
    }

    /**
     * What the rules give a side-delt priority with no picks. On 3 days it gains sets (8 against 6). On 4 days it does
     * not (8 against 9): its 4-set exercise placed twice leaves a shortfall of 2, smaller than the others', so step 3
     * never adds a third; without the priority, 3 sets twice leaves 4 and a third is added. Reported for the owner.
     */
    @Test
    fun aSideDeltPriorityWeeklySetsAsTheRulesGiveThem() {
        fun sideDelts(d: PlanDraft) = d.volume.single { it.group == MuscleGroup.SIDE_DELTS }.weeklySets
        val side = listOf(MuscleGroup.SIDE_DELTS)
        assertEquals(6 to 8, sideDelts(build(three)) to sideDelts(build(three, priorities = side)))
        assertEquals(9 to 8, sideDelts(build(four)) to sideDelts(build(four, priorities = side)))
    }

    @Test
    fun fortyPicksOnThreeDaysFillEighteenPlacesAndNameTwentyTwoLeftOut() {
        val forty = picks(40, seed = 11)
        val draft = build(three, forty)
        assertEquals(18, draft.sessions.sumOf { it.exercises.size })
        assertEquals(22, draft.leftOut.size)
        assertTrue(draft.leftOut.all { it.reason == "No room: every session it fits is full." })
        val placed = draft.sessions.flatMap { s -> s.exercises.map { it.name } }
        assertTrue(placed.all { n -> forty.any { it.name == n } }, "only picks fill a full week")
    }

    @Test
    fun threePrioritiesOnThreeFullDaysStillComeFirstWithExtraSets() {
        val priorities = listOf(MuscleGroup.BICEPS, MuscleGroup.CALVES, MuscleGroup.REAR_DELTS)
        val draft = build(three, picks(40, seed = 3), priorities)
        val placed = draft.sessions.flatMap { it.exercises }
        val forty = picks(40, seed = 3)
        // Every priority pick is placed before any other pick could take its room.
        val priorityPicks = forty.filter { PlanBuilder.mainGroup(it.muscles) in priorities }
        assertTrue(priorityPicks.isNotEmpty())
        assertTrue(priorityPicks.size <= 18)
        assertTrue(priorityPicks.all { p -> placed.any { it.name == p.name } }, "every priority pick is placed first")
        for (e in placed) assertEquals(if (e.mainGroup in priorities) 4 else 3, e.sets)
        assertTrue(draft.volume.any { it.short }, "the draft says what fell short")
    }

    @Test
    fun anOwnExerciseWithNoMusclesIsPlacedLastAndCountsForNothing() {
        val own = BuilderPick("Sled push my way", emptyList(), compound = false, equipment = emptyList(), inCatalogue = false)
        for (days in listOf(three, four)) {
            val draft = build(days, picks(5, seed = 1) + own)
            val session = draft.sessions.single { s -> s.exercises.any { it.name == own.name } }
            val e = session.exercises.last()
            assertEquals(own.name, e.name)
            assertEquals(null, e.mainGroup)
            assertEquals(false, e.inCatalogue)
            assertEquals(3, e.sets)
            val without = draft.sessions.map { s -> s.copy(exercises = s.exercises.filter { it.name != own.name }) }
            for (v in draft.volume) assertEquals(recount(without, v.group).first, v.weeklySets)
        }
    }

    @Test
    fun anotherDraftChangesOnlyTheCatalogueDefaults() {
        for (days in listOf(three, four)) for (chosen in listOf(emptyList(), picks(5, seed = 9))) {
            val v0 = build(days, chosen, variant = 0)
            val v1 = build(days, chosen, variant = 1)
            assertNotEquals(v0, v1)
            assertEquals(v0.leftOut, v1.leftOut)
            val pickNames = chosen.map { key(it.name) }.toSet()
            fun placedPicks(d: PlanDraft) = d.sessions.flatMap { s -> s.exercises.map { key(it.name) } }.filter { it in pickNames }.toSet()
            assertEquals(placedPicks(v0), placedPicks(v1))
        }
    }

    @Test
    fun alternativesShareTheMainGroupAndAreNeverInTheDraft() {
        val chosen = picks(5, seed = 4)
        val draft = build(four, chosen)
        val inDraft = draft.sessions.flatMap { s -> s.exercises.map { key(it.name) } }.toSet()
        for (e in draft.sessions.flatMap { it.exercises }.filter { it.mainGroup != null }) {
            val alts = PlanBuilder.alternatives(e.name, draft, chosen)
            assertTrue(alts.isNotEmpty(), e.name)
            for (alt in alts) {
                assertEquals(e.mainGroup, PlanBuilder.mainGroup(alt.muscles))
                assertTrue(key(alt.name) !in inDraft)
            }
        }
    }

    @Test
    fun swapKeepsThePlaceAndSetsAndRecountsTheVolume() {
        val draft = build(three, priorities = listOf(MuscleGroup.CHEST))
        val session = draft.sessions[1]
        val index = 2
        val from = session.exercises[index]
        val to = PlanBuilder.alternatives(from.name, draft, emptyList()).first()
        val swapped = PlanBuilder.swap(draft, session.day, from.name, to.name)
        val after = swapped.sessions[1].exercises[index]
        assertEquals(to.name, after.name)
        assertEquals(from.sets, after.sets)
        assertEquals(from.repsLow to from.repsHigh, after.repsLow to after.repsHigh)
        assertEquals(from.rpe, after.rpe)
        assertEquals(draft.sessions[0], swapped.sessions[0])
        assertEquals(draft.sessions[2], swapped.sessions[2])
        for (v in swapped.volume) assertEquals(recount(swapped.sessions, v.group), v.weeklySets to v.days)
        assertFailsWith<IllegalArgumentException> { PlanBuilder.swap(draft, SATURDAY, from.name, to.name) }
    }

    @Test
    fun buildRejectsTwoAndFiveDaysAndRepeatedDays() {
        assertFailsWith<IllegalArgumentException> { build(listOf(MONDAY, THURSDAY)) }
        assertFailsWith<IllegalArgumentException> { build(listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) }
        assertFailsWith<IllegalArgumentException> { build(listOf(MONDAY, MONDAY, FRIDAY)) }
        assertFailsWith<IllegalArgumentException> {
            build(three, priorities = listOf(MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.QUADS, MuscleGroup.ABS))
        }
    }

    @Test
    fun mainGroupIsTheGroupOfTheFirstListedMuscle() {
        assertEquals(MuscleGroup.CHEST, PlanBuilder.mainGroup(listOf("Pecs", "Triceps")))
        assertEquals(null, PlanBuilder.mainGroup(emptyList()))
        assertEquals(null, PlanBuilder.mainGroup(listOf("Something of mine", "Biceps")))
    }

    // ---- suggestedPicks ----

    @Test
    fun suggestedPicksAreBothPlansAndTheLastTwentyEightDaysEachOnceNoArchived() {
        val today = LocalDate(2026, 11, 13)
        fun ex(id: Long, archived: Boolean = false) = Exercise(id, "E$id", Category.ACCESSORY, null, emptyList(), false, archived)
        val exercises = (1L..9L).associateWith { ex(it, archived = it == 9L) }
        fun session(id: Long, daysAgo: Int, vararg ids: Long, mode: TrainingMode = TrainingMode.BODYBUILDING) = LoggedSession(
            id, today.minus(DatePeriod(days = daysAgo)), null, null,
            ids.map { LoggedSet(it, 20.0, 10, 8.0, false, null, false) }, mode,
        )
        val history = TrainingHistory(
            sessions = listOf(
                session(1, daysAgo = 3, 4, 5),
                session(2, daysAgo = 28, 6, 1, mode = TrainingMode.POWERLIFTING),
                session(3, daysAgo = 29, 7),
                session(4, daysAgo = 1, 9),
            ),
            exercises = exercises,
            baselines = emptyList(),
        )
        val plans = listOf(
            PlanItem(1, MONDAY, 1, 3, 5, 8.0, 0, TrainingMode.POWERLIFTING),
            PlanItem(2, FRIDAY, 2, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12),
            PlanItem(3, MONDAY, 3, 3, 8, 8.0, 1, TrainingMode.BODYBUILDING, 12),
            PlanItem(4, TUESDAY, 2, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12),
            PlanItem(5, TUESDAY, 9, 3, 8, 8.0, 1, TrainingMode.BODYBUILDING, 12),
        )
        val ids = PlanBuilder.suggestedPicks(history, plans, today)
        assertEquals(listOf(3L, 2L, 1L, 4L, 5L, 6L), ids)
    }

    companion object {
        private fun t(vararg rows: Pair<MuscleGroup, Pair<Int, Int>>) = rows.toMap()

        /** No picks, 3 days, no priorities: weekly sets to days, as the rules give them (stage 2 task 1 report). */
        val NO_PICKS_THREE = t(
            MuscleGroup.CHEST to (9 to 3), MuscleGroup.LATS to (9 to 3), MuscleGroup.QUADS to (6 to 2),
            MuscleGroup.HAMSTRINGS to (6 to 2), MuscleGroup.GLUTES to (12 to 2), MuscleGroup.SIDE_DELTS to (6 to 2),
            MuscleGroup.UPPER_BACK to (9 to 2), MuscleGroup.BICEPS to (6 to 2), MuscleGroup.TRICEPS to (9 to 3),
            MuscleGroup.CALVES to (6 to 2), MuscleGroup.ABS to (6 to 2), MuscleGroup.REAR_DELTS to (6 to 2),
        )

        /** No picks, 4 days, no priorities. */
        val NO_PICKS_FOUR = t(
            MuscleGroup.CHEST to (9 to 2), MuscleGroup.LATS to (12 to 3), MuscleGroup.QUADS to (9 to 2),
            MuscleGroup.HAMSTRINGS to (9 to 2), MuscleGroup.GLUTES to (15 to 2), MuscleGroup.SIDE_DELTS to (9 to 2),
            MuscleGroup.UPPER_BACK to (9 to 2), MuscleGroup.BICEPS to (9 to 2), MuscleGroup.TRICEPS to (6 to 2),
            MuscleGroup.CALVES to (9 to 2), MuscleGroup.ABS to (9 to 2), MuscleGroup.REAR_DELTS to (6 to 2),
        )
    }
}
