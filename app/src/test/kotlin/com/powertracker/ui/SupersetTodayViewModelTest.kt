package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.plan.SupersetHalf
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.today.GRIND_RPE
import com.powertracker.ui.today.OpenRequest
import com.powertracker.ui.today.SUPERSET_SWITCH_GUARD_MS
import com.powertracker.ui.today.TodayEvent
import com.powertracker.ui.today.TodaySuperset
import com.powertracker.ui.today.TodayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Supersets on Today (1.24), in the view model: Wednesday's squat, bench and row, the bench linked to the squat above
 * it. The same story as the web's `Supersets.test.tsx`.
 */
@RunWith(RobolectricTestRunner::class)
class SupersetTodayViewModelTest {
    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    /** Closes the database after `@After` has stopped the view models, once no query is running. */
    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var squat: Exercise
    private lateinit var bench: Exercise
    private lateinit var row: Exercise
    private var sessionId = 0L
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()

    /** The rests asked for: one entry per [TodayEvent.SetLogged]. */
    private val rests = mutableListOf<Boolean>()

    /** The view model's running clock, moved by the test: never slept on. */
    private var clock = 10_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            val exercises = training.exercises().first()
            squat = exercises.first { it.name == "Squat" }
            bench = exercises.first { it.name == "Bench" }
            row = exercises.first { it.name == "Row" }
            sessionId = training.startSession(today, null, null)
        }
    }

    @After
    fun tearDown() {
        collectors.cancel()
        // Nothing of this test may keep reading the database once it is closed.
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    /** Wednesday: squat, bench (linked to the squat when [linked]) and row, with [sets] sets each (squat, bench, row). */
    private fun plan(linked: Boolean = true, sets: List<Int> = listOf(3, 3, 3)) = runBlocking {
        listOf(squat, bench, row).forEachIndexed { i, exercise ->
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, exercise.id, sets = sets[i], reps = 5, targetRpe = 8.0, order = i, supersetWithAbove = linked && exercise == bench))
        }
    }

    private fun viewModel() = TodayViewModel(training, advice, settings, elapsedMs = { clock }) { today }.also { vm ->
        viewModels += vm
        collectors.launch { vm.state.collect {} }
        collectors.launch { vm.events.collect { if (it is TodayEvent.SetLogged) rests += it.isMainOrVariation } }
    }

    /** View-model work resumes on the main looper, which Robolectric only runs when asked. */
    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        }
    }

    /** Lets a tap that must save nothing run to its end. */
    private fun settle() = repeat(20) {
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(5)
    }

    private fun opened(): TodayViewModel {
        val vm = viewModel()
        await("the three cards") { !vm.state.value.loading && vm.state.value.items.size == 3 }
        return vm
    }

    private fun item(vm: TodayViewModel, exercise: Exercise) = vm.state.value.items.first { it.exercise.id == exercise.id }
    private fun done(vm: TodayViewModel) = listOf(squat, bench, row).map { item(vm, it).sets.size }
    private fun stored() = runBlocking { training.setsForSession(sessionId).first() }.size

    private fun tap(vm: TodayViewModel, exercise: Exercise, failed: Boolean = false, rpe: Double = 8.0) {
        val it = item(vm, exercise)
        vm.logSet(it.planned?.id, exercise.id, LoggedSet(exercise.id, 100.0, 5, rpe, failed = failed, missPoint = null, isTopSet = false))
    }

    /** Logs a set of [exercise] and waits until its card shows it and the tap's work is over (the card asked for, if any). */
    private fun log(vm: TodayViewModel, exercise: Exercise, failed: Boolean = false, rpe: Double = 8.0, opens: Exercise? = null) {
        val before = item(vm, exercise).sets.size
        val asked = vm.state.value.openRequest?.n ?: 0
        tap(vm, exercise, failed, rpe)
        await("${exercise.name}'s set ${before + 1}") { item(vm, exercise).sets.size == before + 1 }
        if (opens != null) {
            await("${opens.name}'s card being asked for") { vm.state.value.openRequest == OpenRequest(opens.id, asked + 1) }
        } else {
            // Nothing more is to come: the tap has let go of its guard and asked for no card.
            settle()
            assertEquals(asked, vm.state.value.openRequest?.n ?: 0)
        }
    }

    private fun pastTheGuard() {
        clock += SUPERSET_SWITCH_GUARD_MS
    }

    @Test
    fun theFirstLeads_bothAreTagged_andLoggingItAsksForTheSecondWithNoRest() {
        plan()
        val vm = opened()
        assertEquals(TodaySuperset(SupersetHalf.FIRST, bench.id, "Bench", due = SupersetHalf.FIRST), item(vm, squat).superset)
        assertEquals(TodaySuperset(SupersetHalf.SECOND, squat.id, "Squat", due = SupersetHalf.FIRST), item(vm, bench).superset)
        assertNull(item(vm, row).superset)
        assertNull(vm.state.value.openRequest)

        log(vm, squat, opens = bench)

        assertEquals(emptyList<Boolean>(), rests)
        assertEquals(SupersetHalf.SECOND, item(vm, squat).superset?.due)
        assertEquals(listOf(1, 0, 0), done(vm))
    }

    @Test
    fun loggingTheSecond_startsTheRest_andAsksForTheFirstAgain_roundAfterRound() {
        plan()
        val vm = opened()
        for (round in 1..3) {
            log(vm, squat, opens = bench)
            assertEquals("no rest after the squat, round $round", round - 1, rests.size)
            pastTheGuard()
            // After the last round both are done: the bench's card stays, with its way on.
            log(vm, bench, opens = squat.takeIf { round < 3 })
            assertEquals("a rest after the bench, round $round", round, rests.size)
            pastTheGuard()
        }
        // Both are heavy lifts on the powerlifting side: the long rest.
        assertEquals(listOf(true, true, true), rests)
        assertEquals(listOf(3, 3, 0), done(vm))
        assertNull(item(vm, squat).superset?.due)
    }

    @Test
    fun aSecondQuickTap_afterTheHandOver_savesNothingOnTheOtherCard() {
        plan()
        val vm = opened()
        log(vm, squat, opens = bench)

        // The second tap of a double tap lands on the bench's Log set, just opened.
        tap(vm, bench)
        settle()
        assertEquals(listOf(1, 0, 0), done(vm))
        assertEquals(1, stored())
        // Just short of the guard's end it still saves nothing.
        clock += SUPERSET_SWITCH_GUARD_MS - 1
        tap(vm, bench)
        settle()
        assertEquals(1, stored())

        clock += 1
        log(vm, bench, opens = squat)
        assertEquals(listOf(1, 1, 0), done(vm))
        // The guard is the bench's alone: another exercise is logged at once.
        log(vm, row)
        assertEquals(listOf(1, 1, 1), done(vm))
    }

    @Test
    fun openedAgainMidPair_theTurnIsReadFromTheSetsLogged_andNoCardIsAskedFor() {
        plan()
        runBlocking { training.logSet(sessionId, LoggedSet(squat.id, 100.0, 5, 8.0, failed = false, missPoint = null, isTopSet = false)) }

        val vm = opened()

        assertEquals(SupersetHalf.SECOND, item(vm, squat).superset?.due)
        assertEquals(SupersetHalf.SECOND, item(vm, bench).superset?.due)
        assertNull(vm.state.value.openRequest)
    }

    @Test
    fun unevenSets_theOneWithSetsLeftCarriesOnAlone_withARestAfterEachSet() {
        plan(sets = listOf(1, 3, 3))
        val vm = opened()
        log(vm, squat, opens = bench)
        pastTheGuard()

        repeat(3) { log(vm, bench) }

        assertEquals(3, rests.size)
        assertEquals(listOf(1, 3, 0), done(vm))
        assertEquals(1, vm.state.value.openRequest?.n)
    }

    @Test
    fun aMissOrAGrindOnTheFirst_keepsItsCardToAskWhereItStalled_andStillStartsNoRest() {
        plan()
        val vm = opened()

        log(vm, squat, failed = true)
        assertNull(vm.state.value.openRequest)
        assertEquals(emptyList<Boolean>(), rests)
        // The turn is still the bench's: the screen offers it as the way on.
        assertEquals(SupersetHalf.SECOND, item(vm, squat).superset?.due)

        log(vm, bench, opens = squat)
        assertEquals(1, rests.size)
        pastTheGuard()
        log(vm, squat, rpe = GRIND_RPE)
        assertEquals(1, vm.state.value.openRequest?.n)
        assertEquals(1, rests.size)
    }

    @Test
    fun withOneOfThePairSkippedToday_theOtherIsAnExerciseOnItsOwn() {
        plan()
        val vm = opened()
        vm.skip(bench.id, item(vm, bench).planned?.id, SkipReason.BUSY)
        await("the bench leaving") { vm.state.value.items.map { it.exercise.id } == listOf(squat.id, row.id) }
        assertNull(item(vm, squat).superset)

        val before = item(vm, squat).sets.size
        tap(vm, squat)
        await("the squat's set") { item(vm, squat).sets.size == before + 1 }
        await("its rest") { rests.size == 1 }
        settle()
        assertNull(vm.state.value.openRequest)
    }

    @Test
    fun withoutALink_nothingChanges() {
        plan(linked = false)
        val vm = opened()
        assertEquals(listOf(null, null, null), listOf(squat, bench, row).map { item(vm, it).superset })

        val before = item(vm, squat).sets.size
        tap(vm, squat)
        await("the squat's set") { item(vm, squat).sets.size == before + 1 }
        await("its rest") { rests.size == 1 }
        // No guard on the next card either.
        tap(vm, bench)
        await("the bench's set") { item(vm, bench).sets.size == 1 }
        assertNull(vm.state.value.openRequest)
    }
}
