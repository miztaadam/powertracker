package com.powertracker.ui

import android.content.Context
import android.os.Looper
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
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.today.SkippedToday
import com.powertracker.ui.today.TodayEvent
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Skipping a planned exercise on Today (skip-a-planned-exercise plan, Task 3): the view model's side. */
@RunWith(RobolectricTestRunner::class)
class SkipTodayViewModelTest {
    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var squat: Exercise
    private lateinit var bench: Exercise
    private lateinit var curl: Exercise
    private lateinit var row: Exercise
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()
    private val events = mutableListOf<TodayEvent>()

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
            curl = exercises.first { it.name == "Curl" }
            row = exercises.first { it.name == "Row" }
        }
    }

    @After
    fun tearDown() {
        collectors.cancel()
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    private fun viewModel() = TodayViewModel(training, advice, settings) { today }.also { vm ->
        viewModels += vm
        collectors.launch { vm.state.collect {} }
        collectors.launch { vm.events.collect { events += it } }
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        }
    }

    /** Lets the view model's work run for a while, for checking that something did not happen. */
    private fun settle() {
        val deadline = System.currentTimeMillis() + 300
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }

    private fun plan(exercise: Exercise, order: Int, mode: TrainingMode = TrainingMode.POWERLIFTING, sets: Int = 3, reps: Int = 5, repsHigh: Int? = null): Long =
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, exercise.id, sets, reps, 8.0, order, mode, repsHigh)) }

    private fun set(exercise: Exercise, kg: Double, reps: Int = 5, rpe: Double = 8.0) =
        LoggedSet(exercise.id, kg, reps, rpe, failed = false, missPoint = null, isTopSet = false)

    private fun storedSkips(): List<Skip> = runBlocking { training.skips().first() }

    private fun ids(vm: TodayViewModel) = vm.state.value.items.map { it.exercise.id }

    /** Squat, bench and row planned today on the powerlifting side, in that order. */
    private fun threePlanned(): List<Long> = listOf(plan(squat, 0), plan(bench, 1), plan(row, 2))

    @Test
    fun skip_eachReason_storedAndRowLeaves() {
        for ((i, reason) in listOf(SkipReason.BUSY, SkipReason.PAIN, SkipReason.TIME, null).withIndex()) {
            runBlocking { db.skips().clear(); db.plan().clear() }
            val items = threePlanned()
            val vm = viewModel()
            await("rows $i") { ids(vm) == listOf(squat.id, bench.id, row.id) }

            vm.skip(bench.id, items[1], reason)

            await("bench leaves ($reason)") { ids(vm) == listOf(squat.id, row.id) }
            val stored = storedSkips().single()
            assertEquals(Skip(stored.id, today, TrainingMode.POWERLIFTING, bench.id, items[1], reason), stored)
            assertEquals(listOf(SkippedToday(bench.id, "Bench", stored.id)), vm.state.value.skippedToday)
            await("event") { events.any { it is TodayEvent.Skipped && it.skipId == stored.id } }
            assertEquals("Bench", (events.last { it is TodayEvent.Skipped } as TodayEvent.Skipped).name)
        }
    }

    @Test
    fun skip_withoutSession_works() {
        val items = threePlanned()
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        assertEquals(null, vm.state.value.sessionId)

        vm.skip(squat.id, items[0], SkipReason.BUSY)

        await("squat leaves") { ids(vm) == listOf(bench.id, row.id) }
        assertEquals(null, runBlocking { training.sessionOn(today).first() })
        assertEquals(1, storedSkips().size)
    }

    @Test
    fun skip_refusedForARowWithSets_orNotPlanned() {
        val items = threePlanned()
        runBlocking { training.logSet(training.startSession(today, null, null), set(squat, 100.0)) }
        val vm = viewModel()
        await("rows") { vm.state.value.items.firstOrNull()?.sets?.size == 1 }

        vm.skip(squat.id, items[0], SkipReason.BUSY)
        vm.skip(curl.id, null, SkipReason.BUSY)
        settle()

        assertEquals(emptyList<Skip>(), storedSkips())
        assertEquals(listOf(squat.id, bench.id, row.id), ids(vm))
    }

    @Test
    fun skip_undo_removesSkipAndOpenCard() {
        val items = threePlanned()
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }

        // A pain skip of a main lift always gives its card.
        vm.skip(squat.id, items[0], SkipReason.PAIN)
        await("card") { runBlocking { advice.all() }.any { it.type == AdviceType.SKIP } }
        // The screen has taken the skip in before it is undone. The card is in the database a moment before the squat
        // leaves the list, and "the squat is back", below, is also what the list looks like until it has left: undone
        // in that moment, the wait below passed at once and the skip was read before the undo was stored (the test
        // failed that way on busy machines from 1.17 on; 61 of 700 rounds under load, none of 700 with this wait).
        await("squat leaves") { ids(vm) == listOf(bench.id, row.id) }
        val skipId = storedSkips().single().id
        val card = runBlocking { advice.all() }.single { it.type == AdviceType.SKIP }
        assertEquals(skipId, (card.payload as AdvicePayload.Skipped).skipId)
        assertEquals(TrainingMode.POWERLIFTING, card.mode)

        vm.undoSkip(skipId)

        await("squat back") { ids(vm) == listOf(squat.id, bench.id, row.id) }
        assertEquals(emptyList<Skip>(), storedSkips())
        assertEquals(emptyList<Advice>(), runBlocking { advice.all() }.filter { it.type == AdviceType.SKIP })
        assertEquals(emptyList<SkippedToday>(), vm.state.value.skippedToday)
    }

    @Test
    fun skip_undo_afterTheCardWasDismissed_keepsTheDismissal() {
        val items = threePlanned()
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.skip(squat.id, items[0], SkipReason.PAIN)
        await("card") { runBlocking { advice.all() }.any { it.type == AdviceType.SKIP } }
        val card = runBlocking { advice.all() }.single { it.type == AdviceType.SKIP }
        runBlocking { advice.setStatus(card.id, AdviceStatus.DISMISSED) }
        // As above: without this the wait for the squat's return is true before the undo has run.
        await("squat leaves") { ids(vm) == listOf(bench.id, row.id) }

        vm.undoSkip(storedSkips().single().id)

        await("squat back") { ids(vm).size == 3 }
        assertEquals(emptyList<Skip>(), storedSkips())
        assertEquals(listOf(AdviceStatus.DISMISSED), runBlocking { advice.all() }.filter { it.type == AdviceType.SKIP }.map { it.status })
    }

    @Test
    fun skip_bringBack_returnsRowInPlace() {
        val items = threePlanned()
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.skip(bench.id, items[1], null)
        await("bench leaves") { ids(vm).size == 2 }
        vm.skip(squat.id, items[0], SkipReason.TIME)
        await("both leave") { ids(vm) == listOf(row.id) }
        // The skipped line is in plan order.
        assertEquals(listOf("Squat", "Bench"), vm.state.value.skippedToday.map { it.name })

        vm.bringBack(bench.id)

        await("bench back") { ids(vm) == listOf(bench.id, row.id) }
        assertEquals(listOf("Squat"), vm.state.value.skippedToday.map { it.name })
        assertEquals(listOf(squat.id), storedSkips().map { it.exerciseId })
    }

    @Test
    fun skip_sameExerciseTwiceOnDay_hidesBoth() {
        val first = plan(squat, 0)
        plan(squat, 1, sets = 2, reps = 8)
        plan(bench, 2)
        val vm = viewModel()
        await("rows") { ids(vm) == listOf(squat.id, bench.id) }

        vm.skip(squat.id, first, SkipReason.BUSY)

        await("squat leaves") { ids(vm) == listOf(bench.id) }
        assertEquals(1, storedSkips().size)
        assertEquals(listOf("Squat"), vm.state.value.skippedToday.map { it.name })

        vm.bringBack(squat.id)

        await("squat back") { ids(vm) == listOf(squat.id, bench.id) }
        // The first plan line's sets, as before the skip.
        assertEquals(3, vm.state.value.items.first().targetSets)
    }

    @Test
    fun skip_otherSideUnaffected() {
        val pl = plan(curl, 0)
        plan(curl, 0, mode = TrainingMode.BODYBUILDING, sets = 3, reps = 8, repsHigh = 12)
        val vm = viewModel()
        await("rows") { ids(vm) == listOf(curl.id) }

        vm.skip(curl.id, pl, SkipReason.BUSY)
        await("curl leaves") { ids(vm).isEmpty() }

        // The bodybuilding side's Today still has it, and nothing skipped.
        runBlocking { settings.update { it.copy(trainingMode = TrainingMode.BODYBUILDING) } }
        await("bodybuilding side") { vm.state.value.mode == TrainingMode.BODYBUILDING }
        await("curl there") { ids(vm) == listOf(curl.id) }
        assertEquals(emptyList<SkippedToday>(), vm.state.value.skippedToday)
        assertEquals(TrainingMode.POWERLIFTING, storedSkips().single().mode)
    }

    @Test
    fun skip_onTheBodybuildingSide_isStampedWithIt() {
        val bb = plan(curl, 0, mode = TrainingMode.BODYBUILDING, sets = 3, reps = 8, repsHigh = 12)
        plan(curl, 0)
        runBlocking { settings.update { it.copy(trainingMode = TrainingMode.BODYBUILDING) } }
        val vm = viewModel()
        await("rows") { vm.state.value.mode == TrainingMode.BODYBUILDING && ids(vm) == listOf(curl.id) }

        vm.skip(curl.id, bb, SkipReason.BUSY)

        await("curl leaves") { ids(vm).isEmpty() }
        assertEquals(TrainingMode.BODYBUILDING, storedSkips().single().mode)
        runBlocking { settings.update { it.copy(trainingMode = TrainingMode.POWERLIFTING) } }
        await("powerlifting side keeps it") { vm.state.value.mode == TrainingMode.POWERLIFTING && ids(vm) == listOf(curl.id) }
    }

    @Test
    fun skip_yesterdaysSkip_notHiddenToday() {
        val items = threePlanned()
        runBlocking { training.skip(Skip(0, today.minus(1, DateTimeUnit.DAY), TrainingMode.POWERLIFTING, squat.id, items[0], SkipReason.BUSY)) { _, _ -> emptyList() } }
        val vm = viewModel()
        await("rows") { !vm.state.value.loading }

        assertEquals(listOf(squat.id, bench.id, row.id), ids(vm))
        assertEquals(emptyList<SkippedToday>(), vm.state.value.skippedToday)
    }

    @Test
    fun finish_countsSkipped() {
        val items = threePlanned()
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.skip(bench.id, items[1], SkipReason.BUSY)
        await("bench leaves") { ids(vm).size == 2 }
        vm.skip(row.id, items[2], null)
        await("skipped") { vm.state.value.skippedToday.size == 2 }
        vm.logSet(items[0], squat.id, set(squat, 100.0))
        await("logged") { vm.state.value.items.single().sets.size == 1 }

        vm.finishSession()

        await("summary") { vm.state.value.summary != null }
        assertEquals(2, vm.state.value.summary?.skipped)
        assertEquals(1, vm.state.value.summary?.exerciseCount)
    }

    @Test
    fun finish_withNoSkips_countsNone() {
        val items = threePlanned()
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.logSet(items[0], squat.id, set(squat, 100.0))
        await("logged") { vm.state.value.items.first().sets.size == 1 }
        vm.finishSession()
        await("summary") { vm.state.value.summary != null }
        assertEquals(0, vm.state.value.summary?.skipped)
    }

    /** Squat 150 × 5 @ 7 a week ago, with a known best: without a hold today's target is 155 × 5 @ 8. */
    private fun squatWithHistory(): Long {
        val item = plan(squat, 0, sets = 5)
        runBlocking {
            training.setBaseline(Baseline(squat.id, 190.0, today.minus(30, DateTimeUnit.DAY)))
            training.logSet(training.startSession(today.minus(7, DateTimeUnit.DAY), null, null), set(squat, 150.0, rpe = 7.0))
        }
        return item
    }

    private fun todaysTarget(exercise: Exercise): AdvicePayload.Target? = runBlocking { advice.all() }
        .filter { it.type == AdviceType.TARGET && it.createdAt == today && it.status == AdviceStatus.OPEN }
        .map { it.payload as AdvicePayload.Target }.singleOrNull { it.exerciseId == exercise.id }

    @Test
    fun noSkip_targetGoesUp() {
        squatWithHistory()
        viewModel()
        await("target") { todaysTarget(squat) != null }
        assertEquals(155.0, todaysTarget(squat)?.weightKg)
    }

    @Test
    fun painSkip_nextTodayTarget_isHeld() {
        val item = squatWithHistory()
        // A pain skip since, on either side: the weight is held until squat is logged again.
        runBlocking { training.skip(Skip(0, today.minus(3, DateTimeUnit.DAY), TrainingMode.BODYBUILDING, squat.id, null, SkipReason.PAIN)) { _, _ -> emptyList() } }
        val vm = viewModel()
        await("target") { todaysTarget(squat) != null }

        assertEquals(150.0, todaysTarget(squat)?.weightKg)
        assertEquals(item, vm.state.value.items.single().planned?.id)
    }

    @Test
    fun painSkipToday_thenUndo_targetIsWorkedOutAgainWithoutTheHold() {
        val item = squatWithHistory()
        val vm = viewModel()
        await("target") { todaysTarget(squat)?.weightKg == 155.0 }

        vm.skip(squat.id, item, SkipReason.PAIN)
        // The pain skip deletes today's target; the coach works it out again, held.
        await("held target") { todaysTarget(squat)?.weightKg == 150.0 }

        vm.undoSkip(storedSkips().single().id)
        await("target again") { todaysTarget(squat)?.weightKg == 155.0 }
        await("row back with its chip") { vm.state.value.items.singleOrNull()?.suggestion != null }
    }

    @Test
    fun painSkip_heldRangeTarget_appliesTheHeldRepsAim() {
        // Curl 20 × 12, 12, 12 on the bodybuilding side last week: every set at the top of 8–12.
        val item = plan(curl, 0, mode = TrainingMode.BODYBUILDING, sets = 3, reps = 8, repsHigh = 12)
        runBlocking {
            settings.update { it.copy(trainingMode = TrainingMode.BODYBUILDING) }
            val last = training.startSession(today.minus(7, DateTimeUnit.DAY), null, null, TrainingMode.BODYBUILDING)
            repeat(3) { training.logSet(last, set(curl, 20.0, reps = 12)) }
            training.skip(Skip(0, today.minus(3, DateTimeUnit.DAY), TrainingMode.POWERLIFTING, curl.id, null, SkipReason.PAIN)) { _, _ -> emptyList() }
            training.startSession(today, null, null, TrainingMode.BODYBUILDING)
        }
        val vm = viewModel()
        await("suggestion") { vm.state.value.items.singleOrNull()?.suggestion != null }
        val target = vm.state.value.items.single().suggestion!!.payload as AdvicePayload.Target
        assertEquals(20.0, target.weightKg, 0.0)
        assertEquals(item, vm.state.value.items.single().planned?.id)

        vm.applyTarget(vm.state.value.items.single().suggestion!!.id)

        // Held, the reps aim is last time's top, not the low end an increase would reset to.
        await("editor") { vm.state.value.items.single().editor.weightKg == 20.0 && vm.state.value.items.single().suggestion == null }
        assertEquals(12, vm.state.value.items.single().editor.reps)
    }

    @Test
    fun skipEveryPlannedExercise_listIsEmpty_skippedLineHasThemAll() {
        val items = threePlanned()
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.skip(squat.id, items[0], null)
        await("squat leaves") { ids(vm).size == 2 }
        vm.skip(bench.id, items[1], null)
        await("bench leaves") { ids(vm).size == 1 }
        vm.skip(row.id, items[2], null)
        await("all gone") { ids(vm).isEmpty() && vm.state.value.skippedToday.size == 3 }
        assertEquals(listOf("Squat", "Bench", "Row"), vm.state.value.skippedToday.map { it.name })
    }

    @Test
    fun afterSwapApplied_skippedLine_dropsSwappedName() {
        val items = threePlanned()
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.skip(row.id, items[2], SkipReason.BUSY)
        await("row leaves") { vm.state.value.skippedToday.map { it.exerciseId } == listOf(row.id) }

        // The coach's swap puts another exercise in the row's place: there is nothing left to bring back.
        runBlocking { training.savePlanItem(PlanItem(items[2], DayOfWeek.WEDNESDAY, curl.id, 3, 5, 8.0, 2)) }

        await("curl in its place") { ids(vm) == listOf(squat.id, bench.id, curl.id) }
        assertEquals(emptyList<SkippedToday>(), vm.state.value.skippedToday)
        assertTrue(storedSkips().isNotEmpty())
    }

    @Test
    fun reskip_newReason_replacesCard_notBlocked() {
        // Curl on the bodybuilding side, busy a week ago: today's busy skip is the second, and gives a card.
        val item = plan(curl, 0, mode = TrainingMode.BODYBUILDING, sets = 3, reps = 8, repsHigh = 12)
        runBlocking {
            settings.update { it.copy(trainingMode = TrainingMode.BODYBUILDING) }
            training.skip(Skip(0, today.minus(7, DateTimeUnit.DAY), TrainingMode.BODYBUILDING, curl.id, item, SkipReason.BUSY)) { _, _ -> emptyList() }
        }
        val vm = viewModel()
        await("rows") { vm.state.value.mode == TrainingMode.BODYBUILDING && ids(vm) == listOf(curl.id) }
        vm.skip(curl.id, item, SkipReason.BUSY)
        await("busy card") { skipCards().any { it.reason == SkipReason.BUSY } }
        await("curl leaves") { vm.state.value.skippedToday.isNotEmpty() }

        // Skipping it again today with another reason replaces the skip's card: the old one does not block the new.
        vm.skip(curl.id, item, SkipReason.PAIN)

        await("pain card") { skipCards().any { it.reason == SkipReason.PAIN } }
        assertEquals(listOf(SkipReason.PAIN), skipCards().map { it.reason })
        assertEquals(2, storedSkips().size)
    }

    /**
     * A pain skip of squat [daysAgo] days ago, given the coach's real card, which was then dismissed; the skip itself
     * was then taken back, so only the dismissed card is left and nothing holds squat's weight.
     */
    private fun dismissedPainCard(item: Long, daysAgo: Int) = runBlocking {
        val day = today.minus(daysAgo, DateTimeUnit.DAY)
        val id = training.skip(Skip(0, day, TrainingMode.POWERLIFTING, squat.id, item, SkipReason.PAIN)) { stored, all ->
            val plans = training.plans().first()
            Coaching.afterSkip(stored, all, training.history().first(), plans.powerlifting + plans.bodybuilding, day, advice.all())
        }
        val card = advice.all().single { it.type == AdviceType.SKIP }
        advice.setStatus(card.id, AdviceStatus.DISMISSED)
        training.unskip(id)
    }

    @Test
    fun painSkip_withinTheQuietSpellOfADismissedPainCard_givesNoCard_butTheHoldApplies() {
        val item = squatWithHistory()
        dismissedPainCard(item, 3)
        val vm = viewModel()
        await("target") { todaysTarget(squat)?.weightKg == 155.0 }

        vm.skip(squat.id, item, SkipReason.PAIN)

        await("held") { storedSkips().size == 1 && todaysTarget(squat)?.weightKg == 150.0 }
        settle()
        assertEquals(listOf(AdviceStatus.DISMISSED), runBlocking { advice.all() }.filter { it.type == AdviceType.SKIP }.map { it.status })
    }

    @Test
    fun painSkip_7DaysAfterADismissedPainCard_givesTheCard() {
        val item = squatWithHistory()
        dismissedPainCard(item, 7)
        val vm = viewModel()
        await("target") { todaysTarget(squat) != null }

        vm.skip(squat.id, item, SkipReason.PAIN)

        await("pain card") { skipCards().any { it.reason == SkipReason.PAIN } }
        assertEquals(today, runBlocking { advice.all() }.single { it.type == AdviceType.SKIP && it.status == AdviceStatus.OPEN }.createdAt)
    }

    private fun skipCards(): List<AdvicePayload.Skipped> = runBlocking { advice.all() }
        .filter { it.type == AdviceType.SKIP && it.status == AdviceStatus.OPEN }.map { it.payload as AdvicePayload.Skipped }

    @Test
    fun addSkippedExerciseFromPicker_unskipsIt() {
        val items = threePlanned()
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.skip(bench.id, items[1], SkipReason.BUSY)
        await("bench leaves") { ids(vm) == listOf(squat.id, row.id) }

        // Picked again: it is brought back, its skip taken back, in its planned place.
        vm.addExercise(bench.id)

        await("bench back") { ids(vm) == listOf(squat.id, bench.id, row.id) }
        assertEquals(items[1], vm.state.value.items[1].planned?.id)
        await("skip gone") { storedSkips().isEmpty() }
        assertEquals(emptyList<SkippedToday>(), vm.state.value.skippedToday)
        vm.finishSession()
        await("summary") { vm.state.value.summary != null }
        assertEquals(0, vm.state.value.summary?.skipped)
    }

    @Test
    fun addSkippedExerciseFromTheCatalogue_unskipsIt() {
        val items = threePlanned()
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.skip(row.id, items[2], null)
        await("row leaves") { ids(vm) == listOf(squat.id, bench.id) }

        vm.addFromCatalogue("Row")

        await("row back") { ids(vm) == listOf(squat.id, bench.id, row.id) }
        await("skip gone") { storedSkips().isEmpty() }
        assertEquals(items[2], vm.state.value.items[2].planned?.id)
    }

    @Test
    fun skip_doubleCall_storesOneSkipAndOneEvent() {
        val items = threePlanned()
        val vm = viewModel()
        await("rows") { ids(vm).size == 3 }
        vm.skip(bench.id, items[1], SkipReason.BUSY)
        vm.skip(bench.id, items[1], SkipReason.PAIN)
        await("bench leaves") { ids(vm).size == 2 }
        settle()
        assertEquals(listOf(SkipReason.BUSY), storedSkips().map { it.reason })
        assertEquals(1, events.count { it is TodayEvent.Skipped })
    }
}
