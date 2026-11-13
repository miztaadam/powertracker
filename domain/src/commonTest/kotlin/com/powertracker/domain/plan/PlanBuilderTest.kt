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
import com.powertracker.domain.plan.PlanBuilder.SlotKind
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.SATURDAY
import kotlinx.datetime.DayOfWeek.THURSDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanBuilderTest {
    private val three = listOf(MONDAY, WEDNESDAY, FRIDAY)
    private val four = listOf(MONDAY, TUESDAY, THURSDAY, FRIDAY)

    private fun pick(e: LibraryExercise) = BuilderPick(e.name, e.muscles, e.kind == Kind.COMPOUND, e.equipment, true)
    private fun pick(name: String) = pick(Library.all().single { it.name == name })

    private fun picks(count: Int, seed: Int): List<BuilderPick> =
        Library.all().shuffled(Random(seed)).take(count).map(::pick)

    private val ownNoMuscles = BuilderPick("Sled push my way", emptyList(), compound = false, equipment = emptyList(), inCatalogue = false)
    private val ownWithMuscles = BuilderPick("My cable thing", listOf("Pecs", "Triceps"), compound = false, equipment = emptyList(), inCatalogue = false)

    private fun build(days: List<DayOfWeek>, picks: List<BuilderPick> = emptyList(), priorities: List<MuscleGroup> = emptyList(), variant: Int = 0) =
        PlanBuilder.build(BuilderAnswers(days, picks, priorities, variant))

    private fun key(name: String) = Library.normalise(name)

    private val defaultNames: Set<String> = SlotKind.entries.flatMap { it.defaults }.map(::key).toSet()

    /** Every answer the property checks run over. */
    private val answers: List<BuilderAnswers> by lazy {
        val prioritySets = listOf(
            emptyList(),
            listOf(MuscleGroup.SIDE_DELTS),
            listOf(MuscleGroup.CALVES, MuscleGroup.CHEST),
            listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.REAR_DELTS, MuscleGroup.BICEPS),
        )
        buildList {
            for (days in listOf(three, four)) for ((i, count) in listOf(0, 5, 12, 40).withIndex()) {
                for ((p, priorities) in prioritySets.withIndex()) for (variant in 0..3) {
                    val drawn = picks(count, seed = 7 + i * 31 + p)
                    val own = when ((i + p + variant) % 3) { 0 -> emptyList(); 1 -> listOf(ownNoMuscles); else -> listOf(ownWithMuscles, ownNoMuscles) }
                    add(BuilderAnswers(days, drawn + own, priorities, variant))
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

    private fun templateSizes(days: Int): List<Int> = if (days == 3) listOf(8, 8, 8) else listOf(8, 6, 8, 6)

    // ---- properties over many answers ----

    @Test
    fun theSameAnswersGiveAnEqualDraft() {
        for (a in answers) assertEquals(PlanBuilder.build(a), PlanBuilder.build(a.copy()), "$a")
    }

    @Test
    fun noSessionHoldsAnExerciseTwiceOrRunsLongerThanItsSlotsPlusOneHeldOver() {
        for (a in answers) {
            val draft = PlanBuilder.build(a)
            assertEquals(a.days.sortedBy { it.ordinal }, draft.sessions.map { it.day })
            val longest = templateSizes(a.days.size).map { it + a.priorities.size + PlanBuilder.HELD_OVER_PER_SESSION }
            for ((i, s) in draft.sessions.withIndex()) {
                assertEquals(s.exercises.size, s.exercises.map { key(it.name) }.toSet().size, "repeat on ${s.day}: $a")
                assertTrue(s.exercises.size <= longest[i], "${s.day} has ${s.exercises.size}: $a")
                assertTrue(s.exercises.count { it.slot == null } <= PlanBuilder.HELD_OVER_PER_SESSION)
            }
        }
    }

    @Test
    fun everyExerciseIsAPickOrACatalogueDefault() {
        for (a in answers) {
            val pickNames = a.picks.map { key(it.name) }.toSet()
            for (e in PlanBuilder.build(a).sessions.flatMap { it.exercises }) {
                assertTrue(key(e.name) in pickNames || key(e.name) in defaultNames, "${e.name}: $a")
                if (e.inCatalogue) assertEquals(Library.find(e.name)!!.muscles, e.muscles)
                if (e.slot != null && e.mainGroup != null) assertTrue(e.mainGroup in e.slot!!.accepts, "${e.name} in ${e.slot}")
            }
        }
    }

    @Test
    fun everyPickIsPlacedOrLeftOutNeverBothNeverNeitherAtMostTwice() {
        for (a in answers) {
            val draft = PlanBuilder.build(a)
            val placed = draft.sessions.flatMap { s -> s.exercises.map { key(it.name) } }
            val left = draft.leftOut.map { key(it.name) }
            assertEquals(left.size, left.toSet().size)
            for (p in a.picks) {
                val times = placed.count { it == key(p.name) }
                val out = key(p.name) in left
                assertTrue((times > 0) != out, "${p.name} placed $times leftOut=$out: $a")
                assertTrue(times <= 2, "${p.name} placed $times times: $a")
            }
            assertTrue(draft.leftOut.all { it.reason == "No room: every session it fits is full." })
        }
    }

    @Test
    fun onFourDaysUpperAndLowerGroupsStayOnTheirOwnDays() {
        val lowerOnly = SlotKind.entries.filter { !it.upper }.flatMap { it.accepts }.toSet()
        val upperOnly = SlotKind.entries.filter { it.upper }.flatMap { it.accepts }.toSet()
        for (a in answers.filter { it.days.size == 4 }) {
            val draft = PlanBuilder.build(a)
            assertEquals(listOf("Upper", "Lower", "Upper", "Lower"), draft.sessions.map { it.title })
            for (s in draft.sessions) for (e in s.exercises) {
                val g = e.mainGroup ?: continue
                if (s.title == "Upper") assertTrue(g !in lowerOnly, "${e.name} ($g) on Upper: $a")
                else assertTrue(g !in upperOnly, "${e.name} ($g) on Lower: $a")
            }
        }
        for (a in answers.filter { it.days.size == 3 }) assertTrue(PlanBuilder.build(a).sessions.all { it.title == "Full body" })
    }

    @Test
    fun prescriptionsAndPriorityOrder() {
        for (a in answers) for (s in PlanBuilder.build(a).sessions) {
            for (e in s.exercises) {
                assertEquals(8 to 12, e.repsLow to e.repsHigh)
                assertEquals(8.0, e.rpe)
                val four = e.mainGroup in a.priorities || e.lead
                assertEquals(if (four) 4 else 3, e.sets, "${e.name} on ${s.day}: $a")
            }
            assertEquals(1, s.exercises.count { it.lead }, "one lead exercise on ${s.day}: $a")
            val isPriority = s.exercises.map { it.mainGroup in a.priorities }
            assertEquals(isPriority.sortedDescending(), isPriority, "priority exercises not first on ${s.day}: $a")
        }
    }

    @Test
    fun volumeMatchesARecountAndShortMeansUnderTen() {
        for (a in answers) {
            val draft = PlanBuilder.build(a)
            assertEquals(PlanBuilder.TARGET_GROUPS, draft.volume.map { it.group })
            for (v in draft.volume) {
                assertEquals(recount(draft.sessions, v.group), v.weeklySets to v.days, "${v.group}: $a")
                assertEquals(v.weeklySets < PlanBuilder.WEEKLY_SETS, v.short)
            }
        }
    }

    /** With no picks, each single priority raises its own group and lowers none. */
    @Test
    fun withNoPicksAPriorityRaisesItsGroupAndLowersNone() {
        for (days in listOf(three, four)) {
            val without = build(days).volume.associate { it.group to it.weeklySets }
            for (g in PlanBuilder.TARGET_GROUPS) {
                val with = build(days, priorities = listOf(g)).volume.associate { it.group to it.weeklySets }
                for (other in PlanBuilder.TARGET_GROUPS) assertTrue(with.getValue(other) >= without.getValue(other), "$g lowers $other on $days")
                assertTrue(with.getValue(g) > without.getValue(g), "$g on $days")
                // Every priority reaches 10, except rear delts on 3 days: one REAR_DELT slot plus the added one is 4 + 4.
                if (days == three && g == MuscleGroup.REAR_DELTS) assertEquals(3 to 8, without.getValue(g) to with.getValue(g))
                else assertTrue(with.getValue(g) >= PlanBuilder.WEEKLY_SETS, "$g reaches ${with[g]} on $days")
            }
        }
    }

    /**
     * With picks the rules do not keep either promise; these record what they give (stage 2 task 1 report). A
     * priority re-orders the picks, so a pick's second placement can land in another slot: here Pull-up takes the ROW
     * slot that held Seated cable row, and upper back falls from 6 to 3.
     */
    @Test
    fun withPicksAPriorityCanLowerAnotherGroup() {
        val chosen = listOf("Pull-up", "Incline machine press", "Machine hip adduction", "Ab wheel rollout", "Clap push-up").map(::pick)
        fun upperBack(d: PlanDraft) = d.volume.single { it.group == MuscleGroup.UPPER_BACK }.weeklySets
        val priorities = listOf(MuscleGroup.CALVES, MuscleGroup.CHEST)
        assertEquals(6 to 3, upperBack(build(three, chosen)) to upperBack(build(three, chosen, priorities)))
    }

    /** REAR_DELT slots also take trap picks, so shrugs fill the added slot and rear delts stay at 0. */
    @Test
    fun withPicksAPriorityCanLeaveItsOwnGroupUnraised() {
        val shrugs = listOf("Barbell shrug", "DB shrug").map(::pick)
        fun rearDelts(d: PlanDraft) = d.volume.single { it.group == MuscleGroup.REAR_DELTS }.weeklySets
        assertEquals(0 to 0, rearDelts(build(three, shrugs)) to rearDelts(build(three, shrugs, listOf(MuscleGroup.REAR_DELTS))))
    }

    // ---- fixed cases ----

    @Test
    fun everyDefaultIsInTheCatalogueUnderAMainGroupItsKindAccepts() {
        assertEquals(16, SlotKind.entries.size)
        for (kind in SlotKind.entries) {
            assertTrue(kind.defaults.size >= 4, "$kind")
            for (name in kind.defaults) {
                val e = Library.all().singleOrNull { it.name == name }
                assertTrue(e != null, "$kind: '$name' is not a catalogue name")
                assertTrue(PlanBuilder.mainGroup(e.muscles) in kind.accepts, "$kind: $name works ${PlanBuilder.mainGroup(e.muscles)} first")
            }
        }
    }

    private fun names(d: PlanDraft) = d.sessions.map { s -> s.exercises.map { it.name } }

    private fun table(d: PlanDraft) = d.volume.associate { it.group to (it.weeklySets to it.days) }

    @Test
    fun noPicksOnThreeDaysIsTheTemplateWithItsFirstDefaults() {
        val draft = build(three)
        assertEquals(
            listOf(
                listOf("Leg press", "DB press", "Lat pulldown", "Seated leg curl", "Lateral raise", "DB curl", "Rope pushdown", "Cable crunch"),
                listOf("RDL", "Incline DB press", "Seated cable row", "Leg extension", "DB shoulder press", "Face pull", "Standing calf raise", "DB curl"),
                listOf("Leg press", "Cable crossover", "Lat pulldown", "RDL", "Lateral raise", "Rope pushdown", "Standing calf raise", "Cable crunch"),
            ),
            names(draft),
        )
        assertTrue(draft.leftOut.isEmpty())
        assertEquals(NO_PICKS_THREE, table(draft))
    }

    @Test
    fun noPicksOnFourDaysIsTheTemplateWithItsFirstDefaults() {
        val draft = build(four)
        assertEquals(
            listOf(
                listOf("DB press", "Seated cable row", "DB shoulder press", "Lat pulldown", "Lateral raise", "Face pull", "DB curl", "Rope pushdown"),
                listOf("Leg press", "RDL", "Leg extension", "Seated leg curl", "Standing calf raise", "Cable crunch"),
                listOf("Incline DB press", "Lat pulldown", "Cable crossover", "Seated cable row", "Lateral raise", "Face pull", "Rope pushdown", "DB curl"),
                listOf("RDL", "Leg press", "Seated leg curl", "Leg extension", "Standing calf raise", "Cable crunch"),
            ),
            names(draft),
        )
        assertEquals(NO_PICKS_FOUR, table(draft))
    }

    @Test
    fun aSideDeltPriorityAddsASlotComesFirstWithFourSetsAndReachesTen() {
        for (days in listOf(three, four)) {
            val without = build(days)
            val with = build(days, priorities = listOf(MuscleGroup.SIDE_DELTS))
            fun count(d: PlanDraft) = d.sessions.sumOf { s -> s.exercises.count { it.mainGroup == MuscleGroup.SIDE_DELTS } }
            assertEquals(count(without) + 1, count(with))
            for (s in with.sessions.filter { s -> s.exercises.any { it.mainGroup == MuscleGroup.SIDE_DELTS } }) {
                assertEquals(MuscleGroup.SIDE_DELTS, s.exercises.first().mainGroup, "${s.day}")
                assertTrue(s.exercises.filter { it.mainGroup == MuscleGroup.SIDE_DELTS }.all { it.sets == 4 })
            }
            fun sideDelts(d: PlanDraft) = d.volume.single { it.group == MuscleGroup.SIDE_DELTS }.weeklySets
            assertTrue(sideDelts(with) > sideDelts(without), "${sideDelts(with)} vs ${sideDelts(without)}")
            assertTrue(sideDelts(with) >= 10, "${sideDelts(with)}")
        }
    }

    @Test
    fun fivePicksLandInSlotsThatAcceptThemAndAppearTwiceWhereASecondSlotExists() {
        val five = listOf("Bench", "Row", "Hack squat", "Hip thrust", "Cable curl").map(::pick)
        for (days in listOf(three, four)) {
            val draft = build(days, five)
            assertTrue(draft.leftOut.isEmpty())
            for (p in five) {
                val placed = draft.sessions.flatMap { it.exercises }.filter { it.name == p.name }
                val group = PlanBuilder.mainGroup(p.muscles)!!
                assertTrue(placed.all { it.slot != null && group in it.slot!!.accepts }, "${p.name}: $placed")
                // Each of these has an accepting slot on at least two days of both templates.
                assertEquals(2, placed.size, "${p.name} on $days")
            }
        }
    }

    @Test
    fun fortyPicksOnThreeDaysLeaveOutOnlyWhatHasNoSlotAndNoHeldOverPlace() {
        val forty = picks(40, seed = 11)
        val draft = build(three, forty)
        val placed = draft.sessions.flatMap { s -> s.exercises.map { it.name } }.toSet()
        assertEquals(forty.map { it.name }.filter { it !in placed }.toSet(), draft.leftOut.map { it.name }.toSet())
        assertTrue(draft.leftOut.isNotEmpty())
        // With picks left out, every session has taken its one held-over pick.
        assertTrue(draft.sessions.all { s -> s.exercises.count { it.slot == null } == 1 })
        assertTrue(draft.leftOut.all { it.reason == PlanBuilder.NO_ROOM })
    }

    @Test
    fun anOwnExerciseWithNoMusclesIsHeldOverLastAndCountsForNothing() {
        for (days in listOf(three, four)) {
            val draft = build(days, picks(5, seed = 1) + ownNoMuscles)
            val session = draft.sessions.single { s -> s.exercises.any { it.name == ownNoMuscles.name } }
            val e = session.exercises.last()
            assertEquals(ownNoMuscles.name, e.name)
            assertNull(e.mainGroup)
            assertNull(e.slot)
            assertEquals(false, e.inCatalogue)
            assertEquals(3, e.sets)
            val without = draft.sessions.map { s -> s.copy(exercises = s.exercises.filter { it.name != ownNoMuscles.name }) }
            for (v in draft.volume) assertEquals(recount(without, v.group).first, v.weeklySets)
        }
    }

    @Test
    fun anotherDraftChangesOnlyTheDefaults() {
        for (days in listOf(three, four)) for (chosen in listOf(emptyList(), picks(5, seed = 9))) {
            val v0 = build(days, chosen, variant = 0)
            val v1 = build(days, chosen, variant = 1)
            assertNotEquals(v0, v1)
            assertEquals(v0.leftOut, v1.leftOut)
            val pickNames = chosen.map { key(it.name) }.toSet()
            fun pickPlaces(d: PlanDraft) = d.sessions.map { s -> s.exercises.filter { key(it.name) in pickNames }.map { it.name to it.slot }.toSet() }
            assertEquals(pickPlaces(v0), pickPlaces(v1))
            fun slots(d: PlanDraft) = d.sessions.map { s -> s.exercises.map { it.slot }.toSet() }
            assertEquals(slots(v0), slots(v1))
        }
    }

    @Test
    fun alternativesStartWithTheKindsDefaultsAndAreNeverInThatSession() {
        val chosen = picks(5, seed = 4) + ownWithMuscles
        for (days in listOf(three, four)) {
            val draft = build(days, chosen)
            for (s in draft.sessions) for (e in s.exercises) {
                val alts = PlanBuilder.alternatives(e.name, draft, chosen, s.day)
                val inSession = s.exercises.map { key(it.name) }.toSet()
                assertTrue(alts.none { key(it.name) in inSession }, "${e.name} on ${s.day}")
                val kind = e.slot
                if (kind != null) {
                    assertTrue(alts.all { PlanBuilder.mainGroup(it.muscles) in kind.accepts })
                    val defaultsLeft = kind.defaults.filter { key(it) !in inSession }.map(::key).toSet()
                    assertEquals(defaultsLeft, alts.take(defaultsLeft.size).map { key(it.name) }.toSet(), "${e.name} ($kind)")
                } else if (e.mainGroup != null) {
                    assertTrue(alts.isNotEmpty())
                    assertTrue(alts.all { PlanBuilder.mainGroup(it.muscles) == e.mainGroup })
                }
            }
        }
    }

    @Test
    fun swapKeepsThePlaceAndSetsAndRecountsTheVolume() {
        val draft = build(three, priorities = listOf(MuscleGroup.CHEST))
        val session = draft.sessions[1]
        for (index in listOf(0, 3)) {
            val from = session.exercises[index]
            val to = PlanBuilder.alternatives(from.name, draft, emptyList(), session.day).first()
            val swapped = PlanBuilder.swap(draft, session.day, from.name, to.name)
            val after = swapped.sessions[1].exercises[index]
            assertEquals(to.name, after.name)
            assertEquals(from.sets, after.sets)
            assertEquals(from.slot, after.slot)
            assertEquals(from.lead, after.lead)
            assertEquals(from.repsLow to from.repsHigh, after.repsLow to after.repsHigh)
            assertEquals(draft.sessions[0], swapped.sessions[0])
            assertEquals(draft.sessions[2], swapped.sessions[2])
            for (v in swapped.volume) assertEquals(recount(swapped.sessions, v.group), v.weeklySets to v.days)
            assertFailsWith<IllegalArgumentException> { PlanBuilder.swap(draft, SATURDAY, from.name, to.name) }
        }
    }

    @Test
    fun buildRejectsBadAnswers() {
        assertFailsWith<IllegalArgumentException> { build(listOf(MONDAY, THURSDAY)) }
        assertFailsWith<IllegalArgumentException> { build(listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) }
        assertFailsWith<IllegalArgumentException> { build(listOf(MONDAY, MONDAY, FRIDAY)) }
        assertFailsWith<IllegalArgumentException> {
            build(three, priorities = listOf(MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.QUADS, MuscleGroup.ABS))
        }
        assertFailsWith<IllegalArgumentException> { build(three, priorities = listOf(MuscleGroup.TRAPS)) }
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
        assertEquals(listOf(3L, 2L, 1L, 4L, 5L, 6L), PlanBuilder.suggestedPicks(history, plans, today))
    }

    companion object {
        private fun t(vararg rows: Pair<MuscleGroup, Pair<Int, Int>>) = rows.toMap()

        /** No picks, 3 days, no priorities: weekly sets to days, as the rules give them (stage 2 task 1 report). */
        val NO_PICKS_THREE = t(
            MuscleGroup.CHEST to (9 to 3), MuscleGroup.LATS to (16 to 3), MuscleGroup.QUADS to (11 to 3),
            MuscleGroup.HAMSTRINGS to (10 to 3), MuscleGroup.GLUTES to (15 to 3), MuscleGroup.SIDE_DELTS to (9 to 3),
            MuscleGroup.UPPER_BACK to (13 to 2), MuscleGroup.BICEPS to (12 to 3), MuscleGroup.TRICEPS to (12 to 3),
            MuscleGroup.CALVES to (6 to 2), MuscleGroup.ABS to (6 to 2), MuscleGroup.REAR_DELTS to (3 to 1),
        )

        /** No picks, 4 days, no priorities. */
        val NO_PICKS_FOUR = t(
            MuscleGroup.CHEST to (11 to 2), MuscleGroup.LATS to (19 to 4), MuscleGroup.QUADS to (13 to 2),
            MuscleGroup.HAMSTRINGS to (13 to 2), MuscleGroup.GLUTES to (14 to 2), MuscleGroup.SIDE_DELTS to (9 to 2),
            MuscleGroup.UPPER_BACK to (19 to 4), MuscleGroup.BICEPS to (12 to 2), MuscleGroup.TRICEPS to (13 to 2),
            MuscleGroup.CALVES to (6 to 2), MuscleGroup.ABS to (6 to 2), MuscleGroup.REAR_DELTS to (6 to 2),
        )
    }
}
