package com.powertracker.domain.coach

import com.powertracker.domain.coach.PhysiqueFixtures.FOUR_DAYS
import com.powertracker.domain.coach.PhysiqueFixtures.OWN_ID
import com.powertracker.domain.coach.PhysiqueFixtures.THREE_DAYS
import com.powertracker.domain.coach.PhysiqueFixtures.apply
import com.powertracker.domain.coach.PhysiqueFixtures.default3
import com.powertracker.domain.coach.PhysiqueFixtures.default4
import com.powertracker.domain.coach.PhysiqueFixtures.exercises
import com.powertracker.domain.coach.PhysiqueFixtures.idOf
import com.powertracker.domain.coach.PhysiqueFixtures.item
import com.powertracker.domain.coach.PhysiqueFixtures.weekly
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.NewExercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.SetAddition
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.PlanBuilder
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The lagging-muscle coach (stage 3 plan, "The rules"). */
class PhysiqueCoachTest {
    private val today = LocalDate(2026, 11, 14)
    private val noHistory = PhysiqueFixtures.history()

    private fun checkIn(lagging: Set<MuscleGroup>, growing: Set<MuscleGroup> = emptySet()) = CheckIn(today, lagging, growing)

    private fun outcome(plan: List<PlanItem>, lagging: Set<MuscleGroup>, growing: Set<MuscleGroup> = emptySet(), prior: List<Advice> = emptyList()) =
        PhysiqueCoach.afterCheckIn(checkIn(lagging, growing), noHistory, plan, today, prior)

    private fun moreWork(a: Advice) = a.payload as AdvicePayload.MoreWork

    private fun applyAll(plan: List<PlanItem>, advice: List<Advice>): List<PlanItem> =
        advice.fold(plan) { p, a -> apply(PhysiqueCoach.applyMoreWork(moreWork(a), p, exercises), p) }

    // ---- the plans and check-ins the properties run over ----

    /** The stage 2 builder's drafts (3 and 4 days, variants 0-3, with and without priorities) and hand-made plans. */
    private val plans: List<Pair<String, List<PlanItem>>> by lazy {
        val priorities = listOf(
            emptyList(), listOf(MuscleGroup.CHEST), listOf(MuscleGroup.CALVES, MuscleGroup.ABS, MuscleGroup.REAR_DELTS),
            listOf(MuscleGroup.GLUTES, MuscleGroup.BICEPS),
        )
        val drafts = listOf(THREE_DAYS, FOUR_DAYS).flatMap { days ->
            (0..3).flatMap { v -> priorities.map { p -> "${days.size} days v$v $p" to PhysiqueFixtures.build(days, p, v) } }
        }
        drafts + listOf(
            "own exercise" to withOwn, "a 5-set exercise" to withFiveSets, "a full 10-exercise day" to withFullDay,
            "only an own exercise" to listOf(PlanItem(1, MONDAY, OWN_ID, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)),
            "every day full" to fullWeek,
        )
    }

    private val withOwn: List<PlanItem> by lazy {
        default3 + PlanItem(100, MONDAY, OWN_ID, 3, 8, 8.0, 8, TrainingMode.BODYBUILDING, 12)
    }
    private val withFiveSets: List<PlanItem> by lazy {
        default3.map { if (it.weekday == MONDAY && it.order == 0) it.copy(sets = 5) else it }
    }

    /** Wednesday holds ten exercises. */
    private val withFullDay: List<PlanItem> by lazy { default3 + item(100, WEDNESDAY, "Cable crossover", 3, 9) }

    /** Monday and Friday with ten exercises each, every one at 4 sets. */
    private val fullWeek: List<PlanItem> by lazy {
        val names = listOf("DB press", "Lat pulldown", "Leg press", "Seated leg curl", "Lateral raise", "Face pull", "DB curl", "Rope pushdown", "Standing calf raise", "Cable crunch")
        listOf(MONDAY, FRIDAY).flatMapIndexed { d, day -> names.mapIndexed { i, n -> item(d * 100L + i + 1, day, n, 4, i) } }
    }

    /** Each single group lagging, then random lagging and growing-well sets (fixed seed). */
    private fun checkIns(seed: Int, randoms: Int): List<Pair<Set<MuscleGroup>, Set<MuscleGroup>>> {
        val random = Random(seed)
        val singles = MuscleGroup.entries.map { setOf(it) to emptySet<MuscleGroup>() }
        val mixed = List(randoms) {
            val shuffled = MuscleGroup.entries.shuffled(random)
            val lagging = shuffled.take(1 + random.nextInt(8)).toSet()
            val growing = shuffled.drop(lagging.size).take(random.nextInt(5)).toSet()
            lagging to growing
        }
        return singles + mixed
    }

    /** Every group at the ceiling or under, or no higher than it was in [before]. */
    private fun assertCeiling(before: List<PlanItem>, after: List<PlanItem>, label: String) {
        val b = weekly(before)
        for ((g, sets) in weekly(after)) {
            assertTrue(sets <= PhysiqueCoach.WEEKLY_CEILING || sets <= b.getValue(g), "$label: $g $sets sets (was ${b[g]})\n${PhysiqueFixtures.describe(after)}")
        }
        val setsBefore = before.associate { it.id to it.sets }
        for (item in after) {
            val was = setsBefore[item.id]
            assertTrue(item.sets <= PhysiqueCoach.MAX_SETS || (was != null && item.sets <= was), "$label: item ${item.id} at ${item.sets} sets")
        }
        val daysBefore = before.groupingBy { it.weekday }.eachCount()
        for ((day, count) in after.groupingBy { it.weekday }.eachCount()) {
            assertTrue(count <= PhysiqueCoach.MAX_EXERCISES || count <= (daysBefore[day] ?: 0), "$label: $day has $count exercises")
        }
    }

    @Test
    fun everyCheckInKeepsItsShapeAndEverySuggestionKeepsTheCeiling() {
        val seen = mutableSetOf<String>()
        for ((name, plan) in plans) for ((lagging, growing) in checkIns(seed = name.hashCode(), randoms = 12)) {
            val label = "$name, lagging $lagging, growing $growing"
            val out = outcome(plan, lagging, growing)
            out.notes.forEach { seen += it.reason.name }
            out.advice.forEach { seen += if (moreWork(it).newExercise != null) "new" else "sets" }
            assertTrue(out.advice.size <= PhysiqueCoach.MAX_GROUPS, label)
            assertTrue(out.advice.all { it.type == AdviceType.MORE_WORK && it.mode == TrainingMode.BODYBUILDING && it.createdAt == today }, label)
            val advised = out.advice.map { moreWork(it).group }
            assertEquals(advised.distinct(), advised, label)
            assertTrue(advised.all { it in lagging }, label)
            assertEquals(lagging - advised.toSet(), out.notes.map { it.group }.toSet(), label)
            assertEquals(out.notes.size, out.notes.map { it.group }.distinct().size, label)
            assertTrue(out.notes.none { it.group in advised }, label)
            val before = weekly(plan)
            for (n in out.notes) assertEquals(before.getValue(n.group), n.weeklySets, label)
            // A LATER note only once three groups have advice.
            if (out.notes.any { it.reason == NoteReason.LATER }) assertEquals(PhysiqueCoach.MAX_GROUPS, out.advice.size, label)
            for (a in out.advice) {
                val p = moreWork(a)
                assertTrue((p.addSets.isNotEmpty()) != (p.newExercise != null), "$label: sets or a new exercise, never both: $p")
                assertTrue(p.addSets.size <= 2, label)
                assertTrue(p.addSets.all { it.toSets == it.fromSets + 1 && it.toSets <= PhysiqueCoach.MAX_SETS }, label)
                p.newExercise?.let { assertEquals(NewExercise(it.weekday, it.name, 3, 8, 12, 8.0), it, label) }
                assertEquals(before.getValue(p.group), p.weeklyBefore, label)
                val alone = apply(PhysiqueCoach.applyMoreWork(p, plan, exercises), plan)
                assertEquals(weekly(alone).getValue(p.group), p.weeklyAfter, "$label: weeklyAfter is a recount")
                assertTrue(p.weeklyAfter > p.weeklyBefore, label)
                assertCeiling(plan, alone, "$label, ${p.group} alone")
            }
            assertCeiling(plan, applyAll(plan, out.advice), "$label, all applied")
        }
        // The properties are not empty: every kind of outcome happens over these plans.
        assertEquals(setOf("new", "sets", "AT_CEILING", "NO_EXERCISE", "FULL", "LATER"), seen)
    }

    @Test
    fun tenCheckInsInARowWithTheSameLaggingGroupsNeverPassTheCeiling() {
        var reached = 0
        for ((name, plan) in plans) {
            val sets = if (name.startsWith("3 days v0 []") || name.startsWith("4 days v0 []")) checkIns(name.hashCode(), 6) else checkIns(name.hashCode(), 3).takeLast(3)
            for ((lagging, growing) in sets) {
                var current = plan
                var open = emptyList<Advice>()
                repeat(10) { round ->
                    val out = PhysiqueCoach.afterCheckIn(checkIn(lagging, growing), noHistory, current, today, open)
                    assertEquals(open.map { it.copy(status = AdviceStatus.DISMISSED) }, out.dismissed)
                    current = applyAll(current, out.advice)
                    open = out.advice
                    assertCeiling(plan, current, "$name, lagging $lagging, round ${round + 1}")
                }
                if (weekly(current).values.any { it == PhysiqueCoach.WEEKLY_CEILING }) reached++
            }
        }
        // Ten rounds take groups up to the ceiling itself, so the ceiling is what stops them.
        assertTrue(reached > 0)
    }

    @Test
    fun theSameInputsGiveEqualOutputs() {
        for ((name, plan) in plans.take(6)) for ((lagging, growing) in checkIns(7, 4)) {
            assertEquals(outcome(plan, lagging, growing), outcome(plan.reversed(), lagging, growing), name)
            assertEquals(outcome(plan, lagging, growing), outcome(plan, lagging, growing), name)
        }
    }

    @Test
    fun theFixturesOnePassCountIsWeeklySets() {
        for ((name, plan) in plans) assertEquals(MuscleGroup.entries.associateWith { PhysiqueCoach.weeklySets(plan, exercises, it) }, weekly(plan), name)
    }

    @Test
    fun growingWellAndOnTrackGroupsGetNothing() {
        val out = outcome(default3, setOf(MuscleGroup.CALVES), growing = setOf(MuscleGroup.CHEST, MuscleGroup.ABS))
        assertEquals(listOf(MuscleGroup.CALVES), out.advice.map { moreWork(it).group })
        assertEquals(emptyList(), out.notes)
        assertEquals(CheckInOutcome(emptyList(), emptyList()), outcome(default3, emptySet(), growing = MuscleGroup.entries.toSet()))
    }

    // ---- fixed cases ----

    @Test
    fun aLaggingGroupGetsOneMoreSetOnAtMostTwoDirectExercisesFewestSetsFirst() {
        // Chest on the default 3 days: DB press 3 (Mon), Incline DB press 3 (Wed), DB press 4 (Fri).
        val p = moreWork(outcome(default3, setOf(MuscleGroup.CHEST)).advice.single())
        assertEquals(
            listOf(
                SetAddition(MONDAY, idOf("DB press"), "DB press", 3, 4),
                SetAddition(WEDNESDAY, idOf("Incline DB press"), "Incline DB press", 3, 4),
            ),
            p.addSets,
        )
        assertNull(p.newExercise)
        assertEquals(10, p.weeklyBefore)
        assertEquals(12, p.weeklyAfter)
    }

    @Test
    fun aGroupWhoseDirectExercisesAreAllAtFourGetsANewExerciseThatWorksItDifferently() {
        val plan = default3.map { if (PlanBuilder.mainGroup(exercises.getValue(it.exerciseId).muscles) == MuscleGroup.CHEST) it.copy(sets = 4) else it }
        val p = moreWork(outcome(plan, setOf(MuscleGroup.CHEST)).advice.single())
        assertEquals(emptyList(), p.addSets)
        // DB press and Incline DB press are presses; a fly works the chest differently. Each day has 4 chest sets: Monday.
        assertEquals(NewExercise(MONDAY, "Cable crossover", 3, 8, 12, 8.0), p.newExercise)
        val entry = Library.find("Cable crossover")!!
        assertEquals(PlanBuilder.SlotKind.CHEST_FLY, PlanBuilder.kindOf(entry.name, entry.muscles, entry.kind == Kind.COMPOUND, true))
        assertEquals(12, p.weeklyBefore)
        assertEquals(15, p.weeklyAfter)
    }

    @Test
    fun aNewExerciseGoesToTheDayWithTheFewestSetsOfTheGroupAmongThoseThatTrainIt() {
        // Every chest exercise at 4, with a second press on Monday: chest sets Monday 8, Wednesday 4, Friday 4.
        // Wednesday and Friday tie on the fewest; the earlier weekday wins.
        val plan = default3.map {
            if (PlanBuilder.mainGroup(exercises.getValue(it.exerciseId).muscles) == MuscleGroup.CHEST) it.copy(sets = 4) else it
        } + item(101, MONDAY, "Decline DB press", 4, 8)
        val p = moreWork(outcome(plan, setOf(MuscleGroup.CHEST)).advice.single())
        assertEquals(WEDNESDAY, p.newExercise!!.weekday)
        // Front delts are at 19 here, so the crossover (pecs, front delts) would take them past the ceiling; the next
        // fly-kind candidate that keeps every group it works at 20 or under is taken.
        assertEquals(19, PhysiqueCoach.weeklySets(plan, exercises, MuscleGroup.FRONT_DELTS))
        assertEquals("DB pullover", p.newExercise!!.name)
    }

    @Test
    fun aGroupTrainedOnNoDayGoesToTheDayThatWorksItsHalf() {
        // Traps: no day trains them. Upper-half items: Monday 6, Wednesday 7 (RDL lists lats and upper back), Friday 6.
        val p = moreWork(outcome(default3, setOf(MuscleGroup.TRAPS)).advice.single())
        assertEquals(NewExercise(WEDNESDAY, "Barbell shrug", 3, 8, 12, 8.0), p.newExercise)
        assertEquals(0, p.weeklyBefore)
        assertEquals(3, p.weeklyAfter)
    }

    @Test
    fun adductorsLaggingOnFourDaysGoToALowerDay() {
        val p = moreWork(outcome(default4, setOf(MuscleGroup.ADDUCTORS)).advice.single())
        assertEquals(NewExercise(TUESDAY, "Machine hip adduction", 3, 8, 12, 8.0), p.newExercise)
    }

    @Test
    fun aGroupAtTwentyIsAtTheCeiling() {
        val plan = listOf(
            item(1, MONDAY, "DB press", 4, 0), item(2, MONDAY, "Incline DB press", 4, 1), item(3, MONDAY, "Cable crossover", 4, 2),
            item(4, MONDAY, "Pec deck", 4, 3), item(5, WEDNESDAY, "Machine chest press", 3, 0), item(6, WEDNESDAY, "Leg press", 3, 1),
        )
        assertEquals(19, PhysiqueCoach.weeklySets(plan, exercises, MuscleGroup.CHEST))
        // One set short of the ceiling: Machine chest press gets the one set that fits.
        val p = moreWork(outcome(plan, setOf(MuscleGroup.CHEST)).advice.single())
        assertEquals(listOf(SetAddition(WEDNESDAY, idOf("Machine chest press"), "Machine chest press", 3, 4)), p.addSets)
        val at20 = plan.map { if (it.id == 5L) it.copy(sets = 4) else it }
        val out = outcome(at20, setOf(MuscleGroup.CHEST))
        assertEquals(emptyList(), out.advice)
        assertEquals(listOf(GroupNote(MuscleGroup.CHEST, NoteReason.AT_CEILING, 20)), out.notes)
    }

    @Test
    fun aDirectExerciseThatWouldTakeAnotherGroupPastTheCeilingIsPassedOver() {
        // Triceps at 20: Incline DB press (pecs, front delts, triceps) cannot take a set; the crossover can.
        val plan = listOf(
            item(1, MONDAY, "Incline DB press", 3, 0), item(2, MONDAY, "Cable crossover", 3, 1),
            item(3, MONDAY, "Rope pushdown", 5, 2), item(4, MONDAY, "Skull crusher", 4, 3), item(5, WEDNESDAY, "Triceps pushdown", 4, 0),
            item(6, WEDNESDAY, "Triceps kickback", 4, 1),
        )
        assertEquals(20, PhysiqueCoach.weeklySets(plan, exercises, MuscleGroup.TRICEPS))
        val p = moreWork(outcome(plan, setOf(MuscleGroup.CHEST)).advice.single())
        assertEquals(listOf(SetAddition(MONDAY, idOf("Cable crossover"), "Cable crossover", 3, 4)), p.addSets)
    }

    @Test
    fun aPlanWhoseEquipmentRulesOutEveryCandidateGetsNoExercise() {
        // Only an own exercise: the plan's equipment is bodyweight, and the one adductor exercise needs a machine.
        val plan = listOf(PlanItem(1, MONDAY, OWN_ID, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12))
        assertEquals(listOf(GroupNote(MuscleGroup.ADDUCTORS, NoteReason.NO_EXERCISE, 0)), outcome(plan, setOf(MuscleGroup.ADDUCTORS)).notes)
    }

    @Test
    fun aWeekWithNoDayWithRoomIsFull() {
        assertEquals(listOf(GroupNote(MuscleGroup.TRAPS, NoteReason.FULL, 0)), outcome(fullWeek, setOf(MuscleGroup.TRAPS)).notes)
    }

    @Test
    fun fourLaggingGroupsGiveThreeAdviceAndLaterForTheGroupWithTheMostWeeklySets() {
        // Rear delts, calves and abs have 6 weekly sets each, chest 10.
        val out = outcome(default3, setOf(MuscleGroup.CHEST, MuscleGroup.CALVES, MuscleGroup.ABS, MuscleGroup.REAR_DELTS))
        assertEquals(listOf(MuscleGroup.REAR_DELTS, MuscleGroup.CALVES, MuscleGroup.ABS), out.advice.map { moreWork(it).group })
        assertEquals(listOf(GroupNote(MuscleGroup.CHEST, NoteReason.LATER, 10)), out.notes)
    }

    @Test
    fun savingACheckInDismissesTheOpenMoreWorkAdvice() {
        val old = outcome(default3, setOf(MuscleGroup.CALVES)).advice.single().copy(id = 7, createdAt = today.minus(28, DateTimeUnit.DAY))
        val applied = old.copy(id = 8, status = AdviceStatus.APPLIED)
        val swap = Advice(
            AdviceType.SWAP, null, today, "r", 0,
            AdvicePayload.Swap(idOf("Leg extension"), "Leg extension", MuscleGroup.QUADS, today, listOf("Lunge")), mode = TrainingMode.BODYBUILDING,
        )
        val out = outcome(default3, setOf(MuscleGroup.CALVES), prior = listOf(old, applied, swap))
        assertEquals(listOf(old.copy(status = AdviceStatus.DISMISSED)), out.dismissed)
    }

    @Test
    fun theNewPayloadsRoundTripThroughTheAdviceSerializer() {
        val payloads = listOf<AdvicePayload>(
            moreWork(outcome(default3, setOf(MuscleGroup.CHEST)).advice.single()),
            moreWork(outcome(default3, setOf(MuscleGroup.TRAPS)).advice.single()),
            AdvicePayload.Swap(5, "Leg extension", MuscleGroup.QUADS, today, listOf("Bulgarian split squat", "Split squat")),
        )
        for (p in payloads) assertEquals(p, Json.decodeFromString(AdvicePayload.serializer(), Json.encodeToString(AdvicePayload.serializer(), p)))
    }

    // ---- applying more work ----

    private val chest by lazy { moreWork(outcome(default3, setOf(MuscleGroup.CHEST)).advice.single()) }
    private val traps by lazy { moreWork(outcome(default3, setOf(MuscleGroup.TRAPS)).advice.single()) }

    private fun at(plan: List<PlanItem>, day: DayOfWeek, name: String) = plan.single { it.weekday == day && it.exerciseId == idOf(name) }

    @Test
    fun stillMatchingSetAdditionsApply() {
        val change = PhysiqueCoach.applyMoreWork(chest, default3, exercises)
        assertTrue(change.changed)
        assertEquals("Added a set to 2 exercises", change.message)
        assertEquals(listOf(at(default3, MONDAY, "DB press").copy(sets = 4), at(default3, WEDNESDAY, "Incline DB press").copy(sets = 4)), change.updated)
        assertEquals(emptyList(), change.removed)
        assertEquals(emptyList(), change.added)
    }

    @Test
    fun anItemEditedOrRemovedSinceIsSkippedAndTheMessageSaysSo() {
        val edited = default3.map { if (it == at(default3, MONDAY, "DB press")) it.copy(sets = 4) else it }
        val change = PhysiqueCoach.applyMoreWork(chest, edited, exercises)
        assertEquals(listOf(at(default3, WEDNESDAY, "Incline DB press").copy(sets = 4)), change.updated)
        assertEquals("Added a set to 1 exercise; 1 exercise had changed since and was left as it is", change.message)
        assertTrue(change.changed)

        val removed = default3 - at(default3, WEDNESDAY, "Incline DB press")
        assertEquals(listOf(at(default3, MONDAY, "DB press").copy(sets = 4)), PhysiqueCoach.applyMoreWork(chest, removed, exercises).updated)

        val both = edited - at(default3, WEDNESDAY, "Incline DB press")
        val none = PhysiqueCoach.applyMoreWork(chest, both, exercises)
        assertEquals(PlanChange(emptyList(), emptyList(), emptyList(), "Your plan has changed since; nothing was added", false), none)
    }

    @Test
    fun aNewExerciseIsAddedAtTheEndOfItsDay() {
        val change = PhysiqueCoach.applyMoreWork(traps, default3, exercises)
        assertEquals("Added Barbell shrug to Wednesday", change.message)
        assertEquals(listOf(PlanItem(0, WEDNESDAY, 0, 3, 8, 8.0, 9, TrainingMode.BODYBUILDING, 12) to "Barbell shrug"), change.added)
        assertEquals(emptyList(), change.updated)
    }

    @Test
    fun aNewExerciseAlreadyInThePlanOrADayFullSinceIsSkipped() {
        val added = default3 + item(100, FRIDAY, "Barbell shrug", 3, 8)
        assertFalse(PhysiqueCoach.applyMoreWork(traps, added, exercises).changed)
        val full = default3 + item(100, WEDNESDAY, "Cable crossover", 3, 9)
        val change = PhysiqueCoach.applyMoreWork(traps, full, exercises)
        assertEquals(PlanChange(emptyList(), emptyList(), emptyList(), "Your plan has changed since; nothing was added", false), change)
    }

    @Test
    fun applyingTwiceChangesNothingTheSecondTime() {
        for (p in listOf(chest, traps)) {
            val once = apply(PhysiqueCoach.applyMoreWork(p, default3, exercises), default3)
            val again = PhysiqueCoach.applyMoreWork(p, once, exercises)
            assertFalse(again.changed, "$p")
            assertEquals("Your plan has changed since; nothing was added", again.message)
            assertEquals(once, apply(again, once))
        }
    }

    @Test
    fun applyingReadsOnlyTheBodybuildingSide() {
        val powerlifting = at(default3, MONDAY, "DB press").copy(id = 500, mode = TrainingMode.POWERLIFTING)
        val change = PhysiqueCoach.applyMoreWork(chest, listOf(powerlifting) + default3, exercises)
        assertTrue(change.updated.none { it.mode == TrainingMode.POWERLIFTING })
        assertEquals(2, change.updated.size)
    }

    // ---- stuck ----

    private val legExtension = idOf("Leg extension")

    private fun session(daysAgo: Int, vararg sets: Pair<Double, Int>, id: Long = legExtension, failed: Boolean = false, mode: TrainingMode = TrainingMode.BODYBUILDING) =
        LoggedSession(daysAgo.toLong() + 1, today.minus(daysAgo, DateTimeUnit.DAY), null, null, sets.map { (kg, reps) -> LoggedSet(id, kg, reps, 8.0, failed, null, false) }, mode)

    private fun flat(): TrainingHistory = PhysiqueFixtures.history(
        listOf(session(28, 40.0 to 10, 40.0 to 10, 40.0 to 10), session(14, 40.0 to 10, 40.0 to 10, 40.0 to 10), session(7, 40.0 to 10, 40.0 to 10, 40.0 to 9)),
    )

    private fun stuck(h: TrainingHistory = flat(), checkIns: List<CheckIn> = emptyList(), prior: List<Advice> = emptyList(), plan: List<PlanItem> = default3) =
        PhysiqueCoach.stuck(h, plan, checkIns, prior, today)

    @Test
    fun anExerciseFlatForThreeWeeksOverThreeSessionsGetsOneSwap() {
        val advice = stuck().single()
        assertEquals(AdviceType.SWAP, advice.type)
        assertEquals(TrainingMode.BODYBUILDING, advice.mode)
        assertEquals(today, advice.createdAt)
        // Same kind (quad isolation) first: the leg extension's own defaults, in order.
        assertEquals(
            AdvicePayload.Swap(legExtension, "Leg extension", MuscleGroup.QUADS, today.minus(28, DateTimeUnit.DAY), listOf("Bulgarian split squat", "Split squat", "Lunge")),
            advice.payload,
        )
    }

    @Test
    fun theBaselineIsTheLatestSessionThreeWeeksOrMoreAgo() {
        // A session exactly 21 days ago is the baseline; the two after it are the window.
        val h = PhysiqueFixtures.history(listOf(session(30, 30.0 to 10), session(21, 40.0 to 10), session(10, 40.0 to 10), session(2, 40.0 to 8)))
        assertEquals(today.minus(21, DateTimeUnit.DAY), (stuck(h).single().payload as AdvicePayload.Swap).since)
        // No session that old: no baseline, never stuck.
        assertEquals(emptyList(), stuck(PhysiqueFixtures.history(listOf(session(20, 40.0 to 10), session(10, 40.0 to 10), session(2, 40.0 to 10)))))
    }

    @Test
    fun anExerciseThatWentUpOnceInTheWindowIsNotStuck() {
        val heavier = PhysiqueFixtures.history(listOf(session(28, 40.0 to 10), session(14, 42.5 to 8), session(7, 40.0 to 10)))
        assertEquals(emptyList(), stuck(heavier))
        val moreReps = PhysiqueFixtures.history(listOf(session(28, 40.0 to 10, 40.0 to 10), session(14, 40.0 to 10, 40.0 to 11), session(7, 40.0 to 10)))
        assertEquals(emptyList(), stuck(moreReps))
    }

    @Test
    fun missedSetsAndOtherSidesSessionsDoNotCount() {
        // A heavier set that was missed is not a gain; a powerlifting session is not in the window.
        val missed = PhysiqueFixtures.history(flat().sessions + session(3, 50.0 to 10, failed = true).copy(id = 90))
        assertEquals(1, stuck(missed).size)
        val oneBodybuilding = PhysiqueFixtures.history(
            listOf(session(28, 40.0 to 10), session(14, 40.0 to 10, mode = TrainingMode.POWERLIFTING), session(7, 40.0 to 10)),
        )
        assertEquals(emptyList(), stuck(oneBodybuilding))
    }

    @Test
    fun anExerciseDoneOnceInTheWindowIsNotStuck() {
        assertEquals(emptyList(), stuck(PhysiqueFixtures.history(listOf(session(28, 40.0 to 10), session(7, 40.0 to 10)))))
    }

    @Test
    fun anExerciseWhoseGroupIsGrowingWellIsNotStuck() {
        val growing = CheckIn(today.minus(3, DateTimeUnit.DAY), emptySet(), setOf(MuscleGroup.QUADS))
        assertEquals(emptyList(), stuck(checkIns = listOf(growing)))
        // Only the latest check-in counts.
        val older = growing.copy(date = today.minus(40, DateTimeUnit.DAY))
        val latest = CheckIn(today.minus(3, DateTimeUnit.DAY), setOf(MuscleGroup.QUADS), emptySet())
        assertEquals(1, stuck(checkIns = listOf(older, latest)).size)
    }

    private fun lifter(type: AdviceType, daysAgo: Int, status: AdviceStatus) = Advice(
        type, null, today.minus(daysAgo, DateTimeUnit.DAY), "r", 1,
        if (type == AdviceType.DELOAD) AdvicePayload.Deload() else AdvicePayload.ReEntry(), status,
    )

    @Test
    fun noSwapWhileADeloadOrEasingBackInFromTheLastThreeWeeksStands() {
        for (type in listOf(AdviceType.DELOAD, AdviceType.RE_ENTRY)) {
            for (status in listOf(AdviceStatus.OPEN, AdviceStatus.APPLIED)) {
                assertEquals(emptyList(), stuck(prior = listOf(lifter(type, 20, status))), "$type $status")
                assertEquals(1, stuck(prior = listOf(lifter(type, 21, status))).size, "$type $status 21 days ago")
            }
            assertEquals(1, stuck(prior = listOf(lifter(type, 5, AdviceStatus.DISMISSED))).size, "$type dismissed")
        }
    }

    @Test
    fun noSwapAgainWithinFourWeeksOfAnEarlierOneForThatExercise() {
        val earlier = stuck().single()
        for (status in AdviceStatus.entries) {
            assertEquals(emptyList(), stuck(prior = listOf(earlier.copy(createdAt = today.minus(27, DateTimeUnit.DAY), status = status))), "$status")
        }
        assertEquals(1, stuck(prior = listOf(earlier.copy(createdAt = today.minus(28, DateTimeUnit.DAY), status = AdviceStatus.DISMISSED))).size)
        assertEquals(1, stuck(prior = listOf(earlier.copy(createdAt = today.minus(28, DateTimeUnit.DAY), status = AdviceStatus.APPLIED))).size)
        assertEquals(emptyList(), stuck(prior = listOf(earlier.copy(createdAt = today.minus(60, DateTimeUnit.DAY)))), "still open")
        // A swap for another exercise does not count.
        val other = earlier.copy(payload = (earlier.payload as AdvicePayload.Swap).copy(exerciseId = idOf("Leg press")))
        assertEquals(1, stuck(prior = listOf(other)).size)
    }

    @Test
    fun stuckExercisesComeInPlanOrderAndUpToThreeAlternatives() {
        val curl = idOf("DB curl")
        val h = PhysiqueFixtures.history(
            flat().sessions.map { s -> s.copy(sets = s.sets + s.sets.map { it.copy(exerciseId = curl, weightKg = 12.0) }) },
        )
        val advice = stuck(h)
        // Monday's DB curl comes before Wednesday's leg extension.
        assertEquals(listOf(curl, legExtension), advice.map { (it.payload as AdvicePayload.Swap).exerciseId })
        assertTrue(advice.all { (it.payload as AdvicePayload.Swap).alternatives.size <= PhysiqueCoach.ALTERNATIVES })
    }

    // ---- applying a swap ----

    private val pulldownSwap = AdvicePayload.Swap(idOf("Lat pulldown"), "Lat pulldown", MuscleGroup.LATS, today, listOf("Pull-up", "Machine pulldown", "Chin-up"))

    @Test
    fun aSwapReplacesTheExerciseOnEveryDayKeepingThePrescription() {
        val held = default3.filter { it.exerciseId == idOf("Lat pulldown") }
        assertEquals(listOf(MONDAY, FRIDAY), held.map { it.weekday })
        val change = PhysiqueCoach.applySwap(pulldownSwap, "Machine pulldown", default3, exercises)
        assertTrue(change.changed)
        assertEquals("Swapped Lat pulldown for Machine pulldown", change.message)
        assertEquals(held, change.removed)
        assertEquals(held.map { it.copy(id = 0, exerciseId = 0) to "Machine pulldown" }, change.added)
        val after = apply(change, default3)
        assertEquals(PhysiqueCoach.weeklySets(default3, exercises, MuscleGroup.LATS), PhysiqueCoach.weeklySets(after, exercises, MuscleGroup.LATS))
        assertEquals(default3.size, after.size)
    }

    @Test
    fun aSwapDoesNothingWhenTheChoiceIsInThePlanByThenOrTheExerciseIsGone() {
        val nothing = PlanChange(emptyList(), emptyList(), emptyList(), "Your plan has changed since; nothing was swapped", false)
        assertEquals(nothing, PhysiqueCoach.applySwap(pulldownSwap, "Pull-up", default3 + item(100, WEDNESDAY, "Pull-up", 3, 9), exercises))
        assertEquals(nothing, PhysiqueCoach.applySwap(pulldownSwap, "Pull-up", default3.filter { it.exerciseId != idOf("Lat pulldown") }, exercises))
        assertFailsWith<IllegalArgumentException> { PhysiqueCoach.applySwap(pulldownSwap, "Leg press", default3, exercises) }
    }

    // ---- candidates ----

    @Test
    fun candidatesAreAccessoriesOfTheGroupOnPlanEquipmentNotInThePlanNorHeldOrCarried() {
        val words = listOf("plank", "carry", "walk", "hold", "clamshell", "quadruped", "rotation", "pallof")
        val planned = default3.map { Library.normalise(exercises.getValue(it.exerciseId).name) }.toSet()
        val equipment = default3.flatMap { Library.find(exercises.getValue(it.exerciseId).name)!!.equipment }.toSet() + com.powertracker.domain.library.Equipment.BODYWEIGHT
        for (g in MuscleGroup.entries) for (c in PhysiqueCoach.newExerciseCandidates(g, default3, exercises)) {
            assertEquals(com.powertracker.domain.model.Category.ACCESSORY, c.category, c.name)
            assertEquals(g, PlanBuilder.mainGroup(c.muscles), c.name)
            assertTrue(Library.normalise(c.name) !in planned, c.name)
            assertTrue(c.equipment.all { it in equipment }, c.name)
            assertTrue(words.none { c.name.contains(it, ignoreCase = true) }, c.name)
        }
    }

    // ---- revised rules: lagging order, candidate order, swap ceiling ----

    private fun light(e: com.powertracker.domain.library.LibraryExercise) =
        e.equipment.first() == com.powertracker.domain.library.Equipment.BODYWEIGHT || e.equipment.first() == com.powertracker.domain.library.Equipment.BAND

    private fun kindOf(e: com.powertracker.domain.library.LibraryExercise) = PlanBuilder.kindOf(e.name, e.muscles, e.kind == Kind.COMPOUND, true)

    @Test
    fun targetGroupsComeBeforeTheOtherSixWhenEverythingLags() {
        val out = outcome(default3, MuscleGroup.entries.toSet())
        // Rear delts, calves and abs are the target groups with the fewest weekly sets (6 each).
        assertEquals(listOf(MuscleGroup.REAR_DELTS, MuscleGroup.CALVES, MuscleGroup.ABS), out.advice.map { moreWork(it).group })
        assertTrue(out.notes.all { it.reason == NoteReason.LATER })
        val four = outcome(default4, MuscleGroup.entries.toSet())
        assertTrue(four.advice.all { moreWork(it).group in PlanBuilder.TARGET_GROUPS })
        // A lone non-target group still gets its round.
        assertEquals(listOf(MuscleGroup.TRAPS), outcome(default3, setOf(MuscleGroup.TRAPS, MuscleGroup.LATS)).advice.map { moreWork(it).group }.filter { it == MuscleGroup.TRAPS })
        assertEquals(listOf(MuscleGroup.LATS, MuscleGroup.TRAPS), outcome(default3, setOf(MuscleGroup.TRAPS, MuscleGroup.LATS)).advice.map { moreWork(it).group })
    }

    @Test
    fun candidatesPutLoadedExercisesBeforeBodyweightAndBandOnesExceptForCore() {
        for ((name, plan) in plans.take(32)) for (g in MuscleGroup.entries) {
            val kinds = plan.filter { PlanBuilder.mainGroup(exercises.getValue(it.exerciseId).muscles) == g }
                .map { exercises.getValue(it.exerciseId) }.map { e -> Library.find(e.name)!!.let(::kindOf) }.toSet()
            val keys = PhysiqueCoach.newExerciseCandidates(g, plan, exercises).map { c ->
                val differs = if (kindOf(c) !in kinds) 0 else 1
                val heavy = if (g != MuscleGroup.ABS && g != MuscleGroup.OBLIQUES && light(c)) 1 else 0
                differs * 2 + heavy
            }
            assertEquals(keys.sorted(), keys, "$name $g")
        }
    }

    @Test
    fun glutesPreferTheBuildersGlutesPriorityDefaults() {
        assertEquals(listOf("Hip thrust", "Machine hip thrust"), PhysiqueCoach.newExerciseCandidates(MuscleGroup.GLUTES, default3, exercises).take(2).map { it.name })
        assertTrue(PhysiqueCoach.newExerciseCandidates(MuscleGroup.OBLIQUES, default3, exercises).none { it.name.contains("Pallof") })
    }

    @Test
    fun aLoadedExerciseIsNeverSwappedForABodyweightOrBandOne() {
        for ((name, plan) in plans.take(32)) for (id in plan.map { it.exerciseId }.distinct()) {
            val e = exercises.getValue(id)
            if (light(Library.find(e.name)!!)) continue
            assertTrue(PhysiqueCoach.swapAlternatives(e, plan, exercises).none(::light), "$name ${e.name}")
        }
        assertTrue("Bodyweight squat" !in PhysiqueCoach.swapAlternatives(exercises.getValue(idOf("Leg press")), default3, exercises).map { it.name })
    }

    /** Every group at the ceiling or under, or not raised. */
    private fun keepsCeiling(before: List<PlanItem>, after: List<PlanItem>): Boolean {
        val b = weekly(before)
        return weekly(after).all { (g, v) -> v <= PhysiqueCoach.WEEKLY_CEILING || v <= b.getValue(g) }
    }

    @Test
    fun noSwapOfferedOrAppliedBreaksTheCeiling() {
        var offered = 0
        var refused = 0
        for ((name, plan) in plans.take(32)) for (id in plan.map { it.exerciseId }.distinct()) {
            val e = exercises.getValue(id)
            val group = PlanBuilder.mainGroup(e.muscles) ?: continue
            val alternatives = PhysiqueCoach.swapAlternatives(e, plan, exercises).map { it.name }
            for (alt in alternatives) {
                val change = PhysiqueCoach.applySwap(AdvicePayload.Swap(id, e.name, group, today, listOf(alt)), alt, plan, exercises)
                assertTrue(change.changed, "$name ${e.name} -> $alt offered but not applied")
                assertTrue(keepsCeiling(plan, apply(change, plan)), "$name ${e.name} -> $alt")
                offered++
            }
            // Any other candidate of its group, applied as if offered: it applies only when it keeps the ceiling.
            for (c in PhysiqueCoach.newExerciseCandidates(group, plan, exercises).filter { it.name !in alternatives }) {
                val change = PhysiqueCoach.applySwap(AdvicePayload.Swap(id, e.name, group, today, listOf(c.name)), c.name, plan, exercises)
                if (change.changed) assertTrue(keepsCeiling(plan, apply(change, plan)), "$name ${e.name} -> ${c.name}") else refused++
            }
        }
        assertTrue(offered > 0 && refused > 0)
    }

    @Test
    fun aCableLegCurlOnFourDaysVariantThreeIsNotSwappedForARdl() {
        val plan = PhysiqueFixtures.build(FOUR_DAYS, emptyList(), 3)
        assertEquals(20, PhysiqueCoach.weeklySets(plan, exercises, MuscleGroup.GLUTES))
        val curl = exercises.getValue(idOf("Cable leg curl"))
        val alternatives = PhysiqueCoach.swapAlternatives(curl, plan, exercises).map { it.name }
        assertTrue("DB RDL" !in alternatives && "Stiff-leg deadlift" !in alternatives, "$alternatives")
        // Offered earlier (say, before the plan changed), it is refused when applied.
        val change = PhysiqueCoach.applySwap(AdvicePayload.Swap(curl.id, curl.name, MuscleGroup.HAMSTRINGS, today, listOf("DB RDL")), "DB RDL", plan, exercises)
        assertEquals(PlanChange(emptyList(), emptyList(), emptyList(), "Your plan has changed since; that swap would take a body part past 20 sets a week, so nothing was swapped", false), change)
    }

    @Test
    fun theSwapReasonReadsLikeItsCard() {
        // The baseline was 28 days before 14 Nov.
        assertEquals("Leg extension hasn't moved in 4 weeks. Same weight and no more reps since 17 Oct.", stuck().single().reason)

    }
}
