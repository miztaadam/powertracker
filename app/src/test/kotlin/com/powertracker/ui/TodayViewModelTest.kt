package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.FinishedSession
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.today.TodayAdd
import com.powertracker.ui.today.TodayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TodayViewModelTest {
    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    /** Closes the database after `@After` has stopped the view models, once no query is running. */
    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var squat: Exercise
    private lateinit var curl: Exercise
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()

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
            curl = exercises.first { it.category == com.powertracker.domain.model.Category.ACCESSORY }
        }
    }

    @After
    fun tearDown() {
        collectors.cancel()
        // Nothing of this test may keep reading the database once it is closed.
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    private fun viewModel() = TodayViewModel(training, advice, settings) { today }.also { vm ->
        viewModels += vm
        collectors.launch { vm.state.collect {} }
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

    private fun set(exercise: Exercise, kg: Double) = LoggedSet(exercise.id, kg, 5, 8.0, failed = false, missPoint = null, isTopSet = false)

    @Test
    fun finishSession_insertsCoachAdvice() {
        // Back after a 30-day gap: finishing today's session should store a re-entry note.
        runBlocking {
            training.logSet(training.startSession(today.minus(30, DateTimeUnit.DAY), null, null), set(squat, 150.0))
            training.logSet(training.startSession(today, null, null), set(squat, 140.0))
        }
        val vm = viewModel()
        await("state") { !vm.state.value.loading }

        vm.finishSession()

        await("summary") { vm.state.value.summary != null }
        val stored = runBlocking { advice.all() }
        assertEquals(listOf(AdviceType.RE_ENTRY), stored.map { it.type })
        assertEquals(1, vm.state.value.summary?.newNotes)
        assertEquals(1, vm.state.value.summary?.setCount)
    }

    @Test
    fun finishedSession_staysFinished_whenTheAppIsOpenedAgain() {
        runBlocking { training.logSet(training.startSession(today, null, null), set(squat, 140.0)) }
        val first = viewModel()
        await("state") { !first.state.value.loading }
        first.finishSession()
        await("summary") { first.state.value.summary != null }

        // The app is closed and opened again: a new view model over what is stored.
        val second = viewModel()
        await("second state") { !second.state.value.loading }

        // Still finished, with the numbers Finish showed, from the first moment the screen has its data.
        assertEquals(first.state.value.summary, second.state.value.summary)
        assertEquals(false, second.state.value.inSession)

        // Keep logging opens the session again, and it is still open when the app is next opened.
        second.dismissSummary()
        await("forgotten") { runBlocking { settings.finishedSession.first() } == null }
        val third = viewModel()
        await("third state") { !third.state.value.loading }
        assertNull(third.state.value.summary)
        assertEquals(true, third.state.value.inSession)
    }

    @Test
    fun finishedNote_ofAnotherDayOrAnotherSession_finishesNothing() {
        val sessionId = runBlocking { training.startSession(today, null, null) }
        // Finished yesterday; and a note naming a session that is not the one stored for today (the log was replaced).
        for (note in listOf(
            FinishedSession(sessionId, today.minus(1, DateTimeUnit.DAY), 3, 1, 0, 0),
            FinishedSession(sessionId + 1, today, 3, 1, 0, 0),
        )) {
            runBlocking { settings.setFinishedSession(note) }
            val vm = viewModel()
            await("state") { !vm.state.value.loading }
            assertNull(vm.state.value.summary)
            assertEquals(true, vm.state.value.inSession)
        }
    }

    @Test
    fun targets_areStoredOncePerDay() {
        runBlocking {
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            training.setBaseline(Baseline(squat.id, 190.0, today.minus(30, DateTimeUnit.DAY)))
            training.logSet(training.startSession(today.minus(7, DateTimeUnit.DAY), null, null), set(squat, 150.0).copy(rpe = 7.0))
        }
        val first = viewModel()
        await("suggestion") { first.state.value.items.firstOrNull()?.suggestion != null }
        val suggestion = first.state.value.items.first().suggestion!!

        // Dismissed stays dismissed: reopening the screen must not bring the chip back.
        first.dismissTarget(suggestion.id)
        await("dismissal") { first.state.value.items.first().suggestion == null }
        val second = viewModel()
        // Only after the second view model's own coach pass can a duplicate exist.
        await("second coach pass") { second.coachPasses.value >= 1 }
        await("second state") { !second.state.value.loading }

        assertEquals(1, runBlocking { advice.all() }.count { it.type == AdviceType.TARGET })
        assertEquals(null, second.state.value.items.first().suggestion)
    }

    @Test
    fun addFromCatalogue_unknownName_isNull() {
        val vm = viewModel()
        assertNull(vm.addFromCatalogue("Not an exercise"))
        assertNotNull(vm.addFromCatalogue("Goblet squat"))
    }

    @Test
    fun addedExercise_comesAfterThePlan_andStaysOnceLogged() {
        runBlocking {
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            training.startSession(today, null, null)
        }
        val vm = viewModel()
        await("state") { !vm.state.value.loading }

        vm.addExercise(curl.id)
        await("added") { vm.state.value.items.size == 2 }
        assertEquals(listOf(squat.id, curl.id), vm.state.value.items.map { it.exercise.id })
        assertEquals(null, vm.state.value.items[1].planned)

        vm.logSet(null, curl.id, set(curl, 20.0))
        await("logged") { vm.state.value.items[1].logged.size == 1 }

        // A fresh view model (process restart) still shows it, because a set was logged.
        val restarted = viewModel()
        await("restarted") { restarted.state.value.items.size == 2 }
        assertEquals(curl.id, restarted.state.value.items[1].exercise.id)
    }

    private fun planSquatWithHistory() = runBlocking {
        training.setBaseline(Baseline(squat.id, 190.0, today.minus(30, DateTimeUnit.DAY)))
        training.logSet(training.startSession(today.minus(7, DateTimeUnit.DAY), null, null), set(squat, 150.0).copy(rpe = 7.0))
        training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
    }

    @Test
    fun exercisePlannedTwice_isOneCard_withOneTarget() {
        planSquatWithHistory()
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 2, reps = 8, targetRpe = 7.0, order = 1)) }
        val vm = viewModel()
        await("coach pass") { vm.coachPasses.value >= 1 }
        await("suggestion") { vm.state.value.items.firstOrNull()?.suggestion != null }

        assertEquals(listOf(squat.id), vm.state.value.items.map { it.exercise.id })
        assertEquals(5, vm.state.value.items.single().targetSets)
        assertEquals(1, runBlocking { advice.all() }.count { it.type == AdviceType.TARGET })
    }

    @Test
    fun appliedTarget_withFewerSets_changesTheSetCount() {
        planSquatWithHistory()
        // An open deload makes the coach cut the 5 planned sets to 3.
        runBlocking { advice.insert(listOf(Advice(AdviceType.DELOAD, null, today, "Take a deload week.", 2, AdvicePayload.Deload()))) }
        val vm = viewModel()
        await("suggestion") { vm.state.value.items.firstOrNull()?.suggestion != null }
        val item = vm.state.value.items.single()
        val target = item.suggestion!!.payload as AdvicePayload.Target
        assertEquals(3, target.sets)
        assertEquals("nothing changes before Apply", 5, item.targetSets)
        assertEquals(150.0, item.editor.weightKg)

        vm.applyTarget(item.suggestion!!.id)

        await("applied") { vm.state.value.items.single().suggestion == null }
        await("editor") { vm.state.value.items.single().editor.weightKg == target.weightKg }
        assertEquals(3, vm.state.value.items.single().targetSets)
        assertEquals(AdviceStatus.APPLIED, runBlocking { advice.all() }.single { it.type == AdviceType.TARGET }.status)
    }

    @Test
    fun doubleTap_savesOneSet() {
        val sessionId = runBlocking {
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            training.startSession(today, null, null)
        }
        val vm = viewModel()
        await("state") { !vm.state.value.loading }

        vm.logSet(null, squat.id, set(squat, 100.0))
        vm.logSet(null, squat.id, set(squat, 100.0))

        await("logged") { vm.state.value.items.single().logged.isNotEmpty() }
        repeat(20) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assertEquals(1, runBlocking { training.setsForSession(sessionId).first() }.size)

        // Once the first set is on screen the button works again.
        vm.logSet(null, squat.id, set(squat, 100.0))
        await("second set") { vm.state.value.items.single().logged.size == 2 }
    }

    @Test
    fun doubleTapOnConfirm_savesOneSet() {
        val sessionId = runBlocking {
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            training.startSession(today, null, null)
        }
        val vm = viewModel()
        await("state") { !vm.state.value.loading }
        vm.logSet(null, squat.id, set(squat, 100.0).copy(reps = 13))
        await("held back") { vm.state.value.pending != null }
        val held = vm.state.value.pending!!

        vm.logSet(held.planItemId, held.exerciseId, held.set, confirmed = true)
        vm.logSet(held.planItemId, held.exerciseId, held.set, confirmed = true)

        await("logged") { vm.state.value.items.single().logged.isNotEmpty() }
        repeat(20) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assertEquals(1, runBlocking { training.setsForSession(sessionId).first() }.size)
        assertEquals(null, vm.state.value.pending)
    }
    @Test
    fun planChangeAfterTheDayRollsOver_doesNotInsertTargetsDatedYesterday() {
        val bench = runBlocking { training.exercises().first().first { it.name == "Bench" } }
        planSquatWithHistory()
        runBlocking {
            training.setBaseline(Baseline(bench.id, 120.0, today.minus(30, DateTimeUnit.DAY)))
            training.logSet(training.startSession(today.minus(6, DateTimeUnit.DAY), null, null), set(bench, 100.0).copy(rpe = 7.0))
        }
        var clock = today
        val vm = TodayViewModel(training, advice, settings) { clock }.also { v -> viewModels += v; collectors.launch { v.state.collect {} } }
        await("first coach pass") { vm.coachPasses.value >= 1 }
        assertEquals(listOf(squat.id), runBlocking { advice.all() }.filter { it.type == AdviceType.TARGET }.map { targetExercise(it) })

        // Midnight passes with yesterday's view model still alive; then the plan for its weekday changes.
        clock = today.plus(1, DateTimeUnit.DAY)
        val passes = vm.coachPasses.value
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, bench.id, sets = 3, reps = 5, targetRpe = 8.0, order = 1)) }
        // Give the old view model every chance to react to the plan change.
        val deadline = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assertEquals(passes, vm.coachPasses.value)
        val yesterdays = runBlocking { advice.all() }.filter { it.type == AdviceType.TARGET && it.createdAt == today }
        assertEquals(listOf(squat.id), yesterdays.map { targetExercise(it) })
    }

    @Test
    fun addToOpenSession_pastMidnight_stillAdds() {
        // A session started before midnight and still on screen after it: "Add to today" follows what Today shows.
        runBlocking { training.startSession(today, null, null) }
        var clock = today
        val vm = TodayViewModel(training, advice, settings) { clock }.also { v -> viewModels += v; collectors.launch { v.state.collect {} } }
        await("state") { !vm.state.value.loading }

        clock = today.plus(1, DateTimeUnit.DAY)
        val results = mutableListOf<TodayAdd>()
        vm.addToOpenSession("Hammer curl", null) { results += it }
        await("result") { results.isNotEmpty() }

        assertEquals(listOf(TodayAdd.ADDED), results)
        assertEquals(listOf("Hammer curl"), vm.state.value.items.map { it.exercise.name })
    }

    private fun targetExercise(a: Advice) = (a.payload as AdvicePayload.Target).exerciseId
}
