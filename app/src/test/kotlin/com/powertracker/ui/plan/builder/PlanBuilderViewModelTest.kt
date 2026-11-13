package com.powertracker.ui.plan.builder

import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.domain.plan.BuilderAnswers
import com.powertracker.domain.plan.BuilderPick
import com.powertracker.domain.plan.PlanBuilder
import com.powertracker.ui.library.PickerEntry
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.THURSDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The plan builder's steps, rules and saving (stage 2 plan, Task 2), on a real database and settings store. */
@RunWith(RobolectricTestRunner::class)
class PlanBuilderViewModelTest {
    private val today = LocalDate(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository
    private val viewModels = mutableListOf<PlanBuilderViewModel>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings(trainingMode = BODYBUILDING, bodybuildingTrainingDays = setOf(MONDAY, WEDNESDAY, FRIDAY)) } }
    }

    @After
    fun tearDown() {
        for (vm in viewModels) vm.viewModelScope.cancel()
        db.close()
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        }
    }

    private fun vm(): PlanBuilderViewModel {
        val vm = PlanBuilderViewModel(training, settings) { today }
        viewModels += vm
        await("loaded") { !vm.state.value.loading }
        return vm
    }

    private fun row(name: String): Exercise = runBlocking { training.exercises().first() }.first { it.name == name }

    private fun entry(name: String): PickerEntry {
        val lib = Library.find(name)!!
        return PickerEntry(lib.name, lib.muscles, lib.kind, lib, null)
    }

    private fun bbPlan() = runBlocking { training.plan(BODYBUILDING).first() }
    private fun plPlan() = runBlocking { training.plan(POWERLIFTING).first() }
    private fun bbDays() = runBlocking { settings.settings.first().bodybuildingTrainingDays }

    private fun pick(name: String): BuilderPick {
        val lib = Library.find(name)!!
        return BuilderPick(lib.name, lib.muscles, lib.kind == com.powertracker.domain.library.Kind.COMPOUND, lib.equipment, inCatalogue = true)
    }

    private fun PlanBuilderViewModel.untickAll() {
        for (key in state.value.ticked.keys.toList()) untick(key)
    }

    // Step 1: days

    @Test
    fun days_startFromTheBodybuildingDays_andNextNeedsExactlyThatMany() {
        val vm = vm()
        val s = vm.state.value
        assertEquals(BuilderStep.DAYS, s.step)
        assertEquals(3, s.dayCount)
        assertEquals(setOf(MONDAY, WEDNESDAY, FRIDAY), s.days)
        assertTrue(s.canNext)

        vm.chooseDayCount(4)
        assertFalse(vm.state.value.canNext)
        vm.toggleDay(THURSDAY)
        assertTrue(vm.state.value.canNext)
        vm.toggleDay(TUESDAY)
        assertFalse("five days chosen for four", vm.state.value.canNext)
        vm.next()
        assertEquals(BuilderStep.DAYS, vm.state.value.step)
    }

    @Test
    fun days_withSomethingOtherThan3or4_startAt3WithNoneChosen() {
        runBlocking { settings.update { it.copy(bodybuildingTrainingDays = setOf(MONDAY, TUESDAY)) } }
        val vm = vm()
        assertEquals(3, vm.state.value.dayCount)
        assertEquals(emptySet<DayOfWeek>(), vm.state.value.days)
        assertFalse(vm.state.value.canNext)

        runBlocking { settings.update { it.copy(bodybuildingTrainingDays = setOf(MONDAY, TUESDAY, THURSDAY, FRIDAY)) } }
        val four = vm()
        assertEquals(4, four.state.value.dayCount)
        assertEquals(setOf(MONDAY, TUESDAY, THURSDAY, FRIDAY), four.state.value.days)
    }

    // Step 2: exercises

    @Test
    fun exercises_arePreTickedFromSuggestedPicks() {
        val squat = row("Squat")
        val curl = row("Curl")
        val bench = row("Bench")
        runBlocking {
            training.savePlanItem(PlanItem(0, MONDAY, squat.id, 3, 5, 8.0, 0))
            training.savePlanItem(PlanItem(0, TUESDAY, 0, 3, 8, 8.0, 0, BODYBUILDING, 12), "Lat pulldown")
            val own = training.addExercise(Exercise(0, "Tyre flip", Category.ACCESSORY, null, emptyList(), isBuiltIn = false))
            val recent = training.startSession(today.minus(10, DateTimeUnit.DAY), null, null)
            training.logSet(recent, LoggedSet(curl.id, 20.0, 10, 8.0, false, null, false))
            training.logSet(recent, LoggedSet(own, 0.0, 10, 8.0, false, null, false))
            val old = training.startSession(today.minus(29, DateTimeUnit.DAY), null, null)
            training.logSet(old, LoggedSet(bench.id, 100.0, 5, 8.0, false, null, false))
        }
        val vm = vm()
        val expected = PlanBuilder.suggestedPicks(runBlocking { training.history().first() }, plPlan() + bbPlan(), today)
        val ticked = vm.state.value.ticked.values.toList()
        assertEquals(setOf("Lat pulldown", "Squat", "Curl", "Tyre flip"), ticked.map { it.pick.name }.toSet())
        assertEquals(expected.size, ticked.size)
        val own = ticked.single { it.pick.name == "Tyre flip" }
        assertFalse(own.pick.inCatalogue)
        assertTrue(own.exerciseId != null)
        assertTrue(ticked.filter { it.pick.inCatalogue }.all { Library.find(it.pick.name) != null })
        assertEquals(4, vm.state.value.chosenCount)
    }

    @Test
    fun nothingTicked_asksWhatTheyWouldLike_andChooseForMeGivesAFullPlan() {
        val vm = vm()
        vm.next()
        assertEquals(BuilderStep.EXERCISES, vm.state.value.step)
        vm.untickAll()
        assertTrue(vm.state.value.nothingTicked)
        vm.next()
        assertEquals(BuilderStep.BODY_PARTS, vm.state.value.step)
        vm.next()
        val draft = vm.state.value.draft!!
        assertEquals(BuilderStep.DRAFT, vm.state.value.step)
        assertEquals(PlanBuilder.build(BuilderAnswers(listOf(MONDAY, WEDNESDAY, FRIDAY), emptyList(), emptyList())), draft)
        assertEquals(listOf(8, 9, 8), draft.sessions.map { it.exercises.size })
    }

    @Test
    fun ticking_addsAndRemoves() {
        val vm = vm()
        vm.untickAll()
        vm.toggle(entry("Leg press"))
        assertTrue(vm.isTicked(entry("Leg press")))
        assertEquals(1, vm.state.value.chosenCount)
        vm.toggle(entry("Leg press"))
        assertFalse(vm.isTicked(entry("Leg press")))
    }

    // Step 3: body parts

    @Test
    fun bodyParts_atMostThree_aFourthIsRefused() {
        val vm = vm()
        vm.next(); vm.next()
        assertEquals(BuilderStep.BODY_PARTS, vm.state.value.step)
        assertEquals(PlanBuilder.TARGET_GROUPS, vm.state.value.targetGroups)
        vm.togglePriority(MuscleGroup.SIDE_DELTS)
        vm.togglePriority(MuscleGroup.BICEPS)
        vm.togglePriority(MuscleGroup.CALVES)
        assertFalse(vm.state.value.priorityRefused)
        vm.togglePriority(MuscleGroup.CHEST)
        assertEquals(listOf(MuscleGroup.SIDE_DELTS, MuscleGroup.BICEPS, MuscleGroup.CALVES), vm.state.value.priorities)
        assertTrue(vm.state.value.priorityRefused)
        vm.togglePriority(MuscleGroup.BICEPS)
        assertFalse(vm.state.value.priorityRefused)
        assertEquals(listOf(MuscleGroup.SIDE_DELTS, MuscleGroup.CALVES), vm.state.value.priorities)
    }

    // Step 4: the draft

    @Test
    fun draft_isTheBuildersDraftOfTheAnswers_withLeftOutPicks() {
        val vm = vm()
        vm.untickAll()
        val names = Library.all().take(40).map { it.name }
        for (name in names) vm.toggle(entry(name))
        vm.next(); vm.next()
        vm.togglePriority(MuscleGroup.SIDE_DELTS)
        vm.next()
        val draft = vm.state.value.draft!!
        val expected = PlanBuilder.build(BuilderAnswers(listOf(MONDAY, WEDNESDAY, FRIDAY), names.map(::pick), listOf(MuscleGroup.SIDE_DELTS)))
        assertEquals(expected.sessions.map { s -> s.exercises.map { it.name } }, draft.sessions.map { s -> s.exercises.map { it.name } })
        assertEquals(expected.volume, draft.volume)
        assertEquals(expected.leftOut, draft.leftOut)
        assertTrue(draft.leftOut.isNotEmpty())
        assertTrue(draft.volume.any { it.short })
    }

    @Test
    fun swap_replacesInPlace_keepingTheSets() {
        val vm = vm()
        vm.untickAll()
        vm.next(); vm.next(); vm.next()
        val first = vm.state.value.draft!!.sessions.first()
        val old = first.exercises[1]
        val options = vm.alternatives(first.day, old.name)
        assertTrue(options.isNotEmpty())
        assertTrue(options.none { o -> first.exercises.any { it.name == o.name } })
        vm.swap(first.day, old.name, options.first().name)
        val after = vm.state.value.draft!!.sessions.first().exercises
        assertEquals(options.first().name, after[1].name)
        assertEquals(old.sets, after[1].sets)
        assertEquals(first.exercises.size, after.size)
        assertEquals(first.exercises.map { it.name }.filterIndexed { i, _ -> i != 1 }, after.map { it.name }.filterIndexed { i, _ -> i != 1 })
    }

    @Test
    fun anotherDraft_changesOnlyDefaults() {
        val vm = vm()
        vm.untickAll()
        vm.toggle(entry("Leg press"))
        vm.next(); vm.next(); vm.next()
        val first = vm.state.value.draft!!
        vm.anotherDraft()
        val second = vm.state.value.draft!!
        assertEquals(1, vm.state.value.variant)
        assertNotEquals(first, second)
        assertEquals(PlanBuilder.build(BuilderAnswers(listOf(MONDAY, WEDNESDAY, FRIDAY), listOf(pick("Leg press")), emptyList(), variant = 1)), second)
        // The pick is where it was.
        fun legPress(d: com.powertracker.domain.plan.PlanDraft) = d.sessions.map { s -> s.exercises.indexOfFirst { it.name == "Leg press" } }
        assertEquals(legPress(first), legPress(second))
    }

    @Test
    fun goingBackToTheAnswers_andOnAgain_startsFromVariant0() {
        val vm = vm()
        vm.untickAll()
        vm.next(); vm.next(); vm.next()
        vm.anotherDraft()
        assertTrue(vm.back())
        assertEquals(BuilderStep.BODY_PARTS, vm.state.value.step)
        vm.next()
        assertEquals(0, vm.state.value.variant)
    }

    // Saving

    @Test
    fun useThisPlan_withNoPlanYet_savesTheDraftAndTheDays() {
        val squat = row("Squat")
        runBlocking { training.savePlanItem(PlanItem(0, MONDAY, squat.id, 3, 5, 8.0, 0)) }
        val powerlifting = plPlan()
        val vm = vm()
        vm.chooseDayCount(4)
        vm.toggleDay(WEDNESDAY); vm.toggleDay(TUESDAY); vm.toggleDay(THURSDAY)
        assertEquals(setOf(MONDAY, TUESDAY, THURSDAY, FRIDAY), vm.state.value.days)
        vm.next()
        vm.untickAll()
        runBlocking { training.addExercise(Exercise(0, "Tyre flip", Category.ACCESSORY, null, emptyList(), isBuiltIn = false)) }
        await("own exercise listed") { vm.state.value.exercises.any { it.name == "Tyre flip" } }
        val own = vm.state.value.exercises.first { it.name == "Tyre flip" }
        vm.toggle(PickerEntry(own.name, own.muscles, com.powertracker.domain.library.Kind.ACCESSORY, null, own.id))
        vm.next(); vm.next()
        val draft = vm.state.value.draft!!
        vm.use()
        assertFalse(vm.state.value.confirming)
        await("saved") { vm.state.value.saved }

        val byId = runBlocking { training.exercises().first() }.associate { it.id to it.name }
        val stored = bbPlan().sortedWith(compareBy({ it.weekday }, { it.order }))
        assertEquals(
            draft.sessions.flatMap { s -> s.exercises.mapIndexed { i, e -> "${s.day} $i ${e.name} ${e.sets} ${e.repsLow}-${e.repsHigh} @ ${e.rpe}" } },
            stored.map { "${it.weekday} ${it.order} ${byId[it.exerciseId]} ${it.sets} ${it.reps}-${it.repsHigh} @ ${it.targetRpe}" },
        )
        assertTrue(stored.any { it.exerciseId == own.id })
        assertEquals(setOf(MONDAY, TUESDAY, THURSDAY, FRIDAY), bbDays())
        assertEquals(powerlifting, plPlan())
    }

    @Test
    fun useThisPlan_overAnExistingPlan_confirmsFirst() {
        val curl = row("Curl")
        runBlocking { training.savePlanItem(PlanItem(0, TUESDAY, curl.id, 3, 8, 8.0, 0, BODYBUILDING, 12)) }
        val vm = vm()
        assertTrue(vm.state.value.hasPlan)
        vm.untickAll()
        vm.next(); vm.next(); vm.next()
        vm.use()
        assertTrue(vm.state.value.confirming)
        vm.cancelReplace()
        assertFalse(vm.state.value.confirming)
        assertEquals(1, bbPlan().size)

        vm.use()
        vm.confirmReplace()
        await("saved") { vm.state.value.saved }
        assertEquals(vm.state.value.draft!!.sessions.sumOf { it.exercises.size }, bbPlan().size)
        assertTrue(bbPlan().none { it.weekday == TUESDAY })
    }

    // Final review, minor 2: "Rebuild my plan" replaces the plan only; every logged session and set is kept.
    @Test
    fun rebuildingOverABodybuildingPlanWithLoggedSessions_keepsEverySessionAndSet_andThePowerliftingPlan() {
        val curl = row("Curl")
        val squat = row("Squat")
        runBlocking {
            training.savePlanItem(PlanItem(0, MONDAY, curl.id, 3, 8, 8.0, 0, BODYBUILDING, 12))
            training.savePlanItem(PlanItem(0, MONDAY, squat.id, 3, 5, 8.0, 0))
            val first = training.startSession(today.minus(9, DateTimeUnit.DAY), 80.0, 4, BODYBUILDING)
            training.logSet(first, LoggedSet(curl.id, 20.0, 12, 8.0, false, null, false))
            training.logSet(first, LoggedSet(curl.id, 20.0, 10, 9.0, false, null, false))
            val second = training.startSession(today.minus(2, DateTimeUnit.DAY), null, null, BODYBUILDING)
            training.logSet(second, LoggedSet(curl.id, 22.5, 9, 9.5, true, null, false))
            val heavy = training.startSession(today.minus(5, DateTimeUnit.DAY), null, null, POWERLIFTING)
            training.logSet(heavy, LoggedSet(squat.id, 140.0, 5, 8.0, false, null, true))
        }
        val log = runBlocking { training.history().first() }.sessions
        assertEquals(listOf(2, 1, 1), log.sortedBy { it.date }.map { it.sets.size })
        val sets = runBlocking { log.flatMap { training.setsForSession(it.id).first() } }
        val pl = plPlan()
        val oldBb = bbPlan()

        val vm = vm()
        vm.next(); vm.next(); vm.next()
        vm.use()
        assertTrue(vm.state.value.confirming)
        vm.confirmReplace()
        await("saved") { vm.state.value.saved }

        assertNotEquals("the plan was rebuilt", oldBb, bbPlan())
        assertEquals(log, runBlocking { training.history().first() }.sessions)
        assertEquals(sets, runBlocking { log.flatMap { training.setsForSession(it.id).first() } })
        assertEquals(pl, plPlan())
    }

    @Test
    fun backingOutAtEachStep_changesNothing() {
        val curl = row("Curl")
        val squat = row("Squat")
        runBlocking {
            training.savePlanItem(PlanItem(0, TUESDAY, curl.id, 3, 8, 8.0, 0, BODYBUILDING, 12))
            training.savePlanItem(PlanItem(0, MONDAY, squat.id, 3, 5, 8.0, 0))
        }
        val bb = bbPlan()
        val pl = plPlan()
        val days = bbDays()
        for (steps in 0..3) {
            val vm = vm()
            vm.chooseDayCount(4)
            vm.toggleDay(TUESDAY)
            repeat(steps) { vm.next() }
            assertEquals(BuilderStep.entries[steps], vm.state.value.step)
            if (steps == 3) vm.anotherDraft()
            repeat(steps) { assertTrue(vm.back()) }
            assertFalse("back from the first step leaves the builder", vm.back())
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(bb, bbPlan())
        assertEquals(pl, plPlan())
        assertEquals(days, bbDays())
    }

    /** Ruling P7: the plan is replaced first, then the days set; if setting the days fails, the builder stays open and can try again. */
    @Test
    fun whenSettingTheDaysFails_theBuilderStaysOpenWithTheError_andTryingAgainSaves() {
        var failNext = true
        val vm = PlanBuilderViewModel(training, settings, today = { today }, setDays = { days ->
            if (failNext) {
                failNext = false
                throw java.io.IOException("settings store unavailable")
            }
            settings.update { it.copy(bodybuildingTrainingDays = days) }
        })
        viewModels += vm
        await("loaded") { !vm.state.value.loading }
        vm.chooseDayCount(4)
        vm.toggleDay(THURSDAY)
        vm.next(); vm.untickAll(); vm.next(); vm.next()
        val draft = vm.state.value.draft!!
        vm.use()
        await("failed") { vm.state.value.failed }
        assertFalse(vm.state.value.saved)
        assertFalse(vm.state.value.saving)
        assertEquals(BuilderStep.DRAFT, vm.state.value.step)
        assertEquals("the plan was replaced first", draft.sessions.sumOf { it.exercises.size }, bbPlan().size)
        assertEquals(setOf(MONDAY, WEDNESDAY, FRIDAY), bbDays())

        // Trying again: the plan now exists, so it asks first; the plan save repeats and the days are set.
        vm.use()
        if (vm.state.value.confirming) vm.confirmReplace()
        await("saved") { vm.state.value.saved }
        assertFalse(vm.state.value.failed)
        assertEquals(draft.sessions.sumOf { it.exercises.size }, bbPlan().size)
        assertEquals(setOf(MONDAY, WEDNESDAY, THURSDAY, FRIDAY), bbDays())
    }

    /** Ruling P6: every ticked exercise, in the order ticked (pre-ticks first), for the "Chosen" section; no cap. */
    @Test
    fun chosen_isEveryTickedExerciseInTickOrder_andTheListIsTheUsualOne() {
        // Planned but never logged: pre-ticked, so under "Chosen", and no longer pushed into "Your exercises".
        runBlocking { training.savePlanItem(PlanItem(0, MONDAY, row("Squat").id, 3, 5, 8.0, 0)) }
        val vm = vm()
        await("lists") { vm.state.value.exercises.isNotEmpty() }
        assertEquals(listOf("Squat"), vm.state.value.chosen.map { it.name })
        vm.untickAll()
        val names = listOf("Leg press", "Cable curl", "T-bar row", "Lat pulldown", "Pec deck", "Lateral raise", "Seated leg curl", "Face pull")
        for (n in names) vm.toggle(entry(n))
        assertEquals(names, vm.state.value.chosen.map { it.name })
        vm.toggle(entry("Cable curl"))
        assertEquals(names - "Cable curl", vm.state.value.chosen.map { it.name })
        assertEquals(emptyList<Long>(), vm.state.value.recentIds)
    }

    @Test
    fun onThePowerliftingSide_theBuilderIsNotOffered() {
        runBlocking { settings.update { it.copy(trainingMode = POWERLIFTING) } }
        val vm = vm()
        assertFalse(vm.state.value.available)
        vm.next(); vm.next(); vm.next()
        vm.use()
        vm.confirmReplace()
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(vm.state.value.draft)
        assertTrue(bbPlan().isEmpty())
    }
}
