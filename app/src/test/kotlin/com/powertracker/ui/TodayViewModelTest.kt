package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.today.TodayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TodayViewModelTest {
    private val today: LocalDate = LocalDate.of(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var squat: Exercise
    private lateinit var curl: Exercise
    private val collectors = CoroutineScope(Dispatchers.Main)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
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
        db.close()
    }

    private fun viewModel() = TodayViewModel(training, advice, settings) { today }.also { vm ->
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
            training.logSet(training.startSession(today.minusDays(30), null, null), set(squat, 150.0))
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
    fun targets_areStoredOncePerDay() {
        runBlocking {
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            training.setBaseline(Baseline(squat.id, 190.0, today.minusDays(30)))
            training.logSet(training.startSession(today.minusDays(7), null, null), set(squat, 150.0).copy(rpe = 7.0))
        }
        val first = viewModel()
        await("suggestion") { first.state.value.items.firstOrNull()?.suggestion != null }
        val suggestion = first.state.value.items.first().suggestion!!

        // Dismissed stays dismissed: reopening the screen must not bring the chip back.
        first.dismissTarget(suggestion.id)
        await("dismissal") { first.state.value.items.first().suggestion == null }
        val second = viewModel()
        await("second state") { !second.state.value.loading }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, runBlocking { advice.all() }.count { it.type == AdviceType.TARGET })
        assertEquals(null, second.state.value.items.first().suggestion)
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
}
