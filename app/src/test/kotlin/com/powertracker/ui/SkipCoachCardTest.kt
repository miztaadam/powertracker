package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.coach.CoachViewModel
import com.powertracker.ui.theme.PowertrackerTheme
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The SKIP card on Coach (skip-a-planned-exercise plan, Task 3): a swap chooser, or Dismiss only. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class SkipCoachCardTest {
    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var exercises: List<Exercise>
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()
    private val messages = mutableListOf<String>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            exercises = training.exercises().first()
        }
    }

    @After
    fun tearDown() {
        collectors.cancel()
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    private fun ex(name: String) = exercises.first { it.name == name }

    private fun viewModel() = CoachViewModel(training, advice, settings) { today }.also { vm ->
        viewModels += vm
        collectors.launch { vm.state.collect {} }
        collectors.launch { vm.messages.collect { messages += it } }
    }

    private fun CoachViewModel.await(condition: (CoachState) -> Boolean): CoachState {
        val deadline = System.currentTimeMillis() + 5_000
        while (state.value.loading || !condition(state.value)) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out; state was ${state.value}" }
        }
        return state.value
    }

    private fun awaitTrue(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        }
    }

    private fun onSide(mode: TrainingMode) = runBlocking { settings.update { it.copy(trainingMode = mode) } }

    /**
     * [exercise] planned on Wednesday on [mode]'s side, with Squat and Bench beside it; then a pain skip of it today,
     * stored with the coach's real card ([Coaching.afterSkip]). Returns the card as stored.
     */
    private fun painSkip(exercise: Exercise, mode: TrainingMode): Advice = runBlocking {
        val repsHigh = if (mode == TrainingMode.BODYBUILDING) 12 else null
        val item = training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, exercise.id, 3, 8, 8.0, 2, mode, repsHigh))
        training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, ex("Squat").id, 3, 5, 8.0, 0, mode, repsHigh?.let { 8 }))
        training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, ex("Bench").id, 3, 5, 8.0, 1, mode, repsHigh?.let { 8 }))
        val history = training.history().first()
        val plans = training.plans().first()
        training.skip(Skip(0, today, mode, exercise.id, item, SkipReason.PAIN)) { stored, all ->
            Coaching.afterSkip(stored, all, history, plans.powerlifting + plans.bodybuilding, today, advice.all())
        }
        advice.all().single { it.type == AdviceType.SKIP }
    }

    private fun planned(mode: TrainingMode) = runBlocking { training.plan(mode).first() }

    private fun swapApplies(mode: TrainingMode) {
        onSide(mode)
        val card = painSkip(ex("Curl"), mode)
        val payload = card.payload as AdvicePayload.Skipped
        assertTrue("the card offers alternatives on $mode", payload.alternatives.isNotEmpty())
        assertEquals(mode, card.mode)
        val vm = viewModel()
        val state = vm.await { it.cards.isNotEmpty() }
        assertEquals(CardDetail.Skip(payload), state.cards.single().detail)

        val choice = payload.alternatives.first()
        vm.swap(card.id, choice)

        awaitTrue("applied") { runBlocking { advice.all() }.single { it.id == card.id }.status == AdviceStatus.APPLIED }
        awaitTrue("message") { messages.isNotEmpty() }
        assertEquals(listOf("Swapped Curl for $choice"), messages)
        val names = runBlocking { db.exercises().all() }.associate { it.id to it.name }
        val side = runBlocking { training.plan(mode).first() }.map { names.getValue(it.exerciseId) }
        assertTrue(side.toString(), choice in side && "Curl" !in side)
        vm.await { it.cards.isEmpty() }
    }

    @Test
    fun coach_skipCard_swapApplies_bodybuilding() = swapApplies(TrainingMode.BODYBUILDING)

    @Test
    fun coach_skipCard_swapApplies_powerlifting() = swapApplies(TrainingMode.POWERLIFTING)

    @Test
    fun coach_skipCard_swapLeavesTheOtherSide() {
        onSide(TrainingMode.POWERLIFTING)
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.FRIDAY, ex("Curl").id, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)) }
        val card = painSkip(ex("Curl"), TrainingMode.POWERLIFTING)
        val vm = viewModel()
        vm.await { it.cards.isNotEmpty() }
        vm.swap(card.id, (card.payload as AdvicePayload.Skipped).alternatives.first())
        awaitTrue("applied") { runBlocking { advice.all() }.single { it.id == card.id }.status == AdviceStatus.APPLIED }
        assertEquals(listOf(ex("Curl").id), runBlocking { training.plan(TrainingMode.BODYBUILDING).first() }.map { it.exerciseId })
    }

    @Test
    fun coach_mainLiftPainCard_dismissOnly() {
        val card = painSkip(ex("Squat"), TrainingMode.POWERLIFTING)
        val payload = card.payload as AdvicePayload.Skipped
        assertEquals(emptyList<String>(), payload.alternatives)
        assertEquals(MainLift.SQUAT, card.lift)
        val vm = viewModel()
        val state = vm.await { it.cards.isNotEmpty() }
        assertEquals(CardDetail.Skip(payload), state.cards.single().detail)

        compose.setContent { PowertrackerTheme { CoachContent(state, onApply = {}, onDismiss = { vm.dismiss(it) }, onSwap = vm::swap) } }
        compose.onNodeWithText(card.reason).assertExists()
        compose.onNodeWithText("Squat skipped").assertExists()
        assertEquals(0, compose.onAllNodesWithText("Swap for", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Dismiss").performClick()

        awaitTrue("dismissed") { runBlocking { advice.all() }.single { it.id == card.id }.status == AdviceStatus.DISMISSED }
    }

    @Test
    fun coach_strictPressPainCard_dismissOnly() {
        val pressId = runBlocking { training.resolveCatalogue("Strict press") }
        exercises = runBlocking { training.exercises().first() }
        val press = exercises.first { it.id == pressId }
        val card = painSkip(press, TrainingMode.POWERLIFTING)
        val payload = card.payload as AdvicePayload.Skipped
        assertEquals(emptyList<String>(), payload.alternatives)
        assertEquals(MainLift.STRICT_PRESS, card.lift)
        val state = viewModel().await { it.cards.isNotEmpty() }
        assertEquals(CardDetail.Skip(payload), state.cards.single().detail)
        compose.setContent { PowertrackerTheme { CoachContent(state, onApply = {}, onDismiss = {}) } }
        compose.onNodeWithText("${press.name} skipped").assertExists()
        compose.onNodeWithText("Dismiss").assertExists()
        assertEquals(0, compose.onAllNodesWithText("Swap for", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun undoAfterTheSwapWasApplied_keepsTheSwap_andThePlan() {
        onSide(TrainingMode.BODYBUILDING)
        val card = painSkip(ex("Curl"), TrainingMode.BODYBUILDING)
        val payload = card.payload as AdvicePayload.Skipped
        val vm = viewModel()
        vm.await { it.cards.isNotEmpty() }
        vm.swap(card.id, payload.alternatives.first())
        awaitTrue("applied") { runBlocking { advice.all() }.single { it.id == card.id }.status == AdviceStatus.APPLIED }
        val planAfterSwap = planned(TrainingMode.BODYBUILDING)

        // Undo on Today, after the swap: the skip goes; the applied card and the swapped plan stay.
        val today = com.powertracker.ui.today.TodayViewModel(training, advice, settings) { this.today }.also { t ->
            viewModels += t
            collectors.launch { t.state.collect {} }
        }
        today.undoSkip(payload.skipId)

        awaitTrue("skip gone") { runBlocking { training.skips().first() }.isEmpty() }
        assertEquals(AdviceStatus.APPLIED, runBlocking { advice.all() }.single { it.id == card.id }.status)
        assertEquals(planAfterSwap, planned(TrainingMode.BODYBUILDING))
        assertTrue(planAfterSwap.none { it.exerciseId == ex("Curl").id })
    }

    @Test
    fun coach_skipCardWithAlternatives_showsASwapButtonForEach() {
        onSide(TrainingMode.BODYBUILDING)
        val card = painSkip(ex("Curl"), TrainingMode.BODYBUILDING)
        val payload = card.payload as AdvicePayload.Skipped
        val state = viewModel().await { it.cards.isNotEmpty() }
        val swapped = mutableListOf<String>()
        compose.setContent { PowertrackerTheme { CoachContent(state, onApply = {}, onDismiss = {}, onSwap = { _, c -> swapped += c }) } }
        for (name in payload.alternatives) compose.onNodeWithText("Swap for $name").assertExists()
        compose.onNodeWithText("Dismiss").assertExists()
        compose.onNodeWithText("Swap for ${payload.alternatives.first()}").performClick()
        assertEquals(listOf(payload.alternatives.first()), swapped)
    }

    @Test
    fun coach_skipCard_hiddenOnceItsExerciseLeftThatSidesPlan() {
        val card = painSkip(ex("Squat"), TrainingMode.POWERLIFTING)
        val vm = viewModel()
        vm.await { it.cards.isNotEmpty() }
        val squatItems = runBlocking { training.plan(TrainingMode.POWERLIFTING).first() }.filter { it.exerciseId == ex("Squat").id }
        runBlocking { for (p in squatItems) training.deletePlanItem(p.id) }
        vm.await { it.cards.isEmpty() }
        assertEquals(AdviceStatus.OPEN, runBlocking { advice.all() }.single { it.id == card.id }.status)
        assertTrue(planned(TrainingMode.POWERLIFTING).isNotEmpty())
    }

    @Test
    fun coach_skipCard_onlyOnItsOwnSide() {
        painSkip(ex("Squat"), TrainingMode.POWERLIFTING)
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, ex("Squat").id, 3, 5, 8.0, 0, TrainingMode.BODYBUILDING, 8)) }
        onSide(TrainingMode.BODYBUILDING)
        val vm = viewModel()
        vm.await { true }
        assertEquals(emptyList<Any>(), vm.state.value.cards)
    }
}
