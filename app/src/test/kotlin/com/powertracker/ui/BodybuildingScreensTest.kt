package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.Backup
import com.powertracker.data.BackupResult
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.Kind
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.coach.CoachViewModel
import com.powertracker.ui.library.PickerEntry
import com.powertracker.ui.plan.PlanViewModel
import com.powertracker.ui.plan.planItemText
import com.powertracker.ui.progress.LiftHistoryViewModel
import com.powertracker.ui.progress.ProgressViewModel
import com.powertracker.ui.today.TodayEvent
import com.powertracker.ui.today.TodayViewModel
import com.powertracker.ui.today.planText
import com.powertracker.ui.today.targetHeadline
import com.powertracker.ui.today.targetReason
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/** Plan, Today, Coach and Progress on the bodybuilding side (bodybuilding mode spec §5.1, §5.3, §7.3 to §7.7). */
@RunWith(RobolectricTestRunner::class)
class BodybuildingScreensTest {
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
            bench = exercises.first { it.name == "Bench" }
            curl = exercises.first { it.name == "Curl" }
        }
    }

    @After
    fun tearDown() {
        collectors.cancel()
        // Nothing of this test may keep reading the database once it is closed.
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        }
    }

    private fun onSide(mode: TrainingMode, leftOn: LocalDate? = null) = runBlocking {
        settings.update { it.copy(trainingMode = mode, bodybuildingLeftOn = if (mode == BODYBUILDING) leftOn else it.bodybuildingLeftOn, powerliftingLeftOn = if (mode == POWERLIFTING) leftOn else it.powerliftingLeftOn) }
    }

    private fun bbItem(exercise: Exercise, day: DayOfWeek = DayOfWeek.WEDNESDAY, order: Int = 0) =
        PlanItem(0, day, exercise.id, sets = 3, reps = 8, targetRpe = 8.0, order = order, mode = BODYBUILDING, repsHigh = 12)

    private fun plItem(exercise: Exercise, day: DayOfWeek = DayOfWeek.WEDNESDAY, order: Int = 0) =
        PlanItem(0, day, exercise.id, sets = 3, reps = 5, targetRpe = 8.0, order = order)

    private fun save(vararg items: PlanItem) = runBlocking { items.forEach { training.savePlanItem(it) } }

    private fun set(exercise: Exercise, kg: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false) =
        LoggedSet(exercise.id, kg, reps, rpe, failed = failed, missPoint = null, isTopSet = false)

    private fun session(date: LocalDate, mode: TrainingMode, vararg sets: LoggedSet) = runBlocking {
        val id = training.startSession(date, null, null, mode)
        sets.forEach { training.logSet(id, it) }
        id
    }

    private fun todayVm() = TodayViewModel(training, advice, settings) { today }.also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }

    private fun planVm() = PlanViewModel(training, settings) { today }.also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }

    private fun coachVm() = CoachViewModel(training, advice, settings) { today }.also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }

    private fun CoachViewModel.await(condition: (CoachState) -> Boolean): CoachState {
        await("coach state") { !state.value.loading && condition(state.value) }
        return state.value
    }

    // Plan

    @Test
    fun plan_showsTheActiveSidesPlanAndDays_andSwitchingSwapsThem() {
        save(plItem(squat, DayOfWeek.MONDAY), bbItem(curl, DayOfWeek.TUESDAY))
        runBlocking {
            settings.update {
                it.copy(trainingDays = setOf(DayOfWeek.MONDAY), bodybuildingTrainingDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), bodybuildingLeftOn = today)
            }
        }
        val vm = planVm()
        await("powerlifting plan") { vm.state.value.sections.flatMap { it.rows }.isNotEmpty() }
        assertEquals(listOf("Squat"), vm.state.value.sections.flatMap { it.rows }.map { it.exerciseName })
        assertEquals(setOf(DayOfWeek.MONDAY), vm.state.value.trainingDays)

        runBlocking { settings.setTrainingMode(BODYBUILDING, today) }
        await("bodybuilding plan") { vm.state.value.mode == BODYBUILDING && vm.state.value.sections.flatMap { it.rows }.isNotEmpty() }
        assertEquals(listOf("Curl"), vm.state.value.sections.flatMap { it.rows }.map { it.exerciseName })
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), vm.state.value.trainingDays)
        assertEquals(listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), vm.state.value.sections.map { it.day })
    }

    @Test
    fun plan_newBodybuildingEntry_is3x8to12at8_onItsOwnSide() {
        onSide(BODYBUILDING)
        save(plItem(squat, DayOfWeek.MONDAY))
        val vm = planVm()
        await("state") { vm.state.value.mode == BODYBUILDING }
        vm.add(DayOfWeek.MONDAY, PickerEntry(bench.name, bench.muscles, Kind.COMPOUND, null, bench.id))

        await("saved") { runBlocking { training.plan(BODYBUILDING).first() }.isNotEmpty() }
        val item = runBlocking { training.plan(BODYBUILDING).first() }.single()
        assertEquals(listOf(3, 8, 12), listOf(item.sets, item.reps, item.repsHigh))
        assertEquals(8.0, item.targetRpe, 0.0)
        assertEquals("Bench  3 × 8–12 @ 8", planItemText("Bench", item))
        assertEquals("3 × 8–12 @ 8", planText(item))
        // The powerlifting plan is untouched.
        assertEquals(listOf(squat.id), runBlocking { training.plan(POWERLIFTING).first() }.map { it.exerciseId })
    }

    @Test
    fun plan_saveKeepsTheRange_andAddOnceUsesTheActiveSide() {
        onSide(BODYBUILDING)
        save(bbItem(curl, DayOfWeek.MONDAY), plItem(bench, DayOfWeek.MONDAY))
        val vm = planVm()
        await("state") { vm.state.value.sections.flatMap { it.rows }.isNotEmpty() }
        val item = vm.state.value.sections.flatMap { it.rows }.single().item
        vm.save(item, null, sets = 4, reps = 10, rpe = 8.5, repsHigh = 15)
        await("saved") { runBlocking { training.plan(BODYBUILDING).first() }.single().repsHigh == 15 }
        assertEquals(10, runBlocking { training.plan(BODYBUILDING).first() }.single().reps)

        // Bench is on Monday on the powerlifting side only: the bodybuilding side can still take it.
        var result: com.powertracker.ui.plan.PlanAdd? = null
        vm.addOnce(DayOfWeek.MONDAY, PickerEntry(bench.name, bench.muscles, Kind.COMPOUND, null, bench.id)) { result = it }
        await("added") { result != null }
        assertEquals(com.powertracker.ui.plan.PlanAdd.ADDED, result)
        val bb = runBlocking { training.plan(BODYBUILDING).first() }
        assertEquals(listOf(curl.id, bench.id), bb.sortedBy { it.order }.map { it.exerciseId })
        assertEquals(12, bb.first { it.exerciseId == bench.id }.repsHigh)
        assertEquals(1, runBlocking { training.plan(POWERLIFTING).first() }.size)
    }

    @Test
    fun plan_emptyBodybuildingPlan_isFlagged_butAnEmptyPowerliftingPlanIsNot() {
        val vm = planVm()
        await("powerlifting state") { vm.state.value.sections.isNotEmpty() }
        assertFalse(vm.state.value.emptySide)
        onSide(BODYBUILDING)
        await("bodybuilding state") { vm.state.value.mode == BODYBUILDING }
        assertTrue(vm.state.value.emptySide)
    }

    // Today

    @Test
    fun today_bodybuildingTarget_showsTheRangeAndLastTime_andApplyFillsRepsToBeat() {
        onSide(BODYBUILDING)
        save(bbItem(curl))
        session(today.minus(7, DateTimeUnit.DAY), BODYBUILDING, set(curl, 26.0, 11), set(curl, 26.0, 10), set(curl, 26.0, 9))
        val vm = todayVm()
        await("suggestion") { vm.state.value.items.firstOrNull()?.suggestion != null }
        val item = vm.state.value.items.single()
        val target = item.suggestion!!.payload as AdvicePayload.Target
        assertEquals("26 kg × 8–12 @ 8", targetHeadline(target))
        assertEquals("Last time: 26 × 11, 10, 9. Beat it.", targetReason(item.suggestion!!))
        assertEquals(BODYBUILDING, item.suggestion!!.mode)
        assertEquals("3 × 8–12 @ 8", planText(item.planned!!))

        vm.startSession(null, null)
        await("session") { vm.state.value.sessionId != null }
        assertEquals(BODYBUILDING, runBlocking { training.sessionOn(today).first() }!!.mode)

        vm.applyTarget(item.suggestion!!.id)
        // Before Apply the reps field holds the plan's low end; Apply takes the chip away.
        await("applied") { vm.state.value.items.single().suggestion == null }
        assertEquals(26.0, vm.state.value.items.single().editor.weightKg!!, 0.0)
        // Set 1: last time 11, so 12 to beat it.
        assertEquals(12, vm.state.value.items.single().editor.reps)
    }

    @Test
    fun today_aPowerliftingTargetForTheSameExercise_doesNotHideTheBodybuildingOne() {
        onSide(BODYBUILDING)
        save(bbItem(curl))
        session(today.minus(7, DateTimeUnit.DAY), BODYBUILDING, set(curl, 26.0, 11), set(curl, 26.0, 10), set(curl, 26.0, 9))
        runBlocking { advice.insert(listOf(Advice(AdviceType.TARGET, null, today, "Coach suggests 30 × 5 @ 8.", 0, AdvicePayload.Target(curl.id, 30.0, 5, 8.0)))) }
        val vm = todayVm()
        await("suggestion") { vm.state.value.items.firstOrNull()?.suggestion != null }
        assertEquals(26.0, (vm.state.value.items.single().suggestion!!.payload as AdvicePayload.Target).weightKg, 0.0)
    }

    @Test
    fun today_switchingOnADayAlreadyTrained_staysOnThatSession_withTheNote() {
        save(plItem(squat), bbItem(curl))
        session(today, POWERLIFTING, set(squat, 140.0, 5))
        runBlocking { settings.setTrainingMode(BODYBUILDING, today) }
        val vm = todayVm()
        await("state") { !vm.state.value.loading && vm.state.value.items.isNotEmpty() }
        assertEquals(POWERLIFTING, vm.state.value.mode)
        assertEquals(listOf("Squat"), vm.state.value.items.map { it.exercise.name })
        assertEquals("Today's session is on the powerlifting side. Bodybuilding starts with your next session.", vm.state.value.sideNote)

        vm.startSession(null, null)
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(50)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, runBlocking { training.history().first() }.sessions.count { it.date == today })
    }

    @Test
    fun today_theReverseNote_andNoNoteWhenTheSidesAgree() {
        onSide(BODYBUILDING)
        save(plItem(squat), bbItem(curl))
        session(today, BODYBUILDING)
        val vm = todayVm()
        await("state") { !vm.state.value.loading && vm.state.value.items.isNotEmpty() }
        assertNull(vm.state.value.sideNote)
        assertEquals(listOf("Curl"), vm.state.value.items.map { it.exercise.name })

        runBlocking { settings.setTrainingMode(POWERLIFTING, today) }
        await("note") { vm.state.value.sideNote != null }
        assertEquals("Today's session is on the bodybuilding side. Powerlifting starts with your next session.", vm.state.value.sideNote)
        assertEquals(BODYBUILDING, vm.state.value.mode)
    }

    @Test
    fun today_anExerciseWithNoHistory_hasNoCoachCard_andCanBeLogged_withTheAccessoryRest() {
        onSide(BODYBUILDING)
        save(bbItem(squat))
        val vm = todayVm()
        val events = mutableListOf<TodayEvent>()
        collectors.launch { vm.events.collect { events += it } }
        await("coach pass") { vm.coachPasses.value >= 1 && !vm.state.value.loading }
        assertNull(vm.state.value.items.single().suggestion)
        vm.startSession(null, null)
        await("session") { vm.state.value.sessionId != null }
        vm.logSet(vm.state.value.items.single().planned!!.id, squat.id, set(squat, 60.0, 10))
        await("set") { vm.state.value.items.single().sets.size == 1 }
        await("event") { events.isNotEmpty() }
        // Squat would get the main-lift rest on the powerlifting side. (The set's buzz, SetBuzz, comes after it, and a
        // bodybuilding set is never a best.)
        assertEquals(TodayEvent.SetLogged(isMainOrVariation = false), events.first())
        assertEquals(1, events.count { it is TodayEvent.SetLogged })
        await("buzz") { events.any { it is TodayEvent.SetBuzz } }
        assertEquals(listOf(TodayEvent.SetBuzz(isNewBest = false)), events.filterIsInstance<TodayEvent.SetBuzz>())
    }

    @Test
    fun today_welcomeBack_after31DaysAway_untilAnswered_notOnFirstEntry() {
        save(bbItem(curl))
        onSide(BODYBUILDING, leftOn = today.minus(31, DateTimeUnit.DAY))
        val vm = todayVm()
        await("state") { !vm.state.value.loading }
        await("welcome") { vm.state.value.welcomeBack }
        vm.answerWelcomeBack()
        await("answered") { !vm.state.value.welcomeBack }
        assertNull(runBlocking { settings.settings.first() }.bodybuildingLeftOn)

        // 30 days is not more than a month.
        onSide(BODYBUILDING, leftOn = today.minus(30, DateTimeUnit.DAY))
        val second = todayVm()
        await("second") { !second.state.value.loading }
        Thread.sleep(50)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(second.state.value.welcomeBack)

        // First entry: never left, so no card.
        runBlocking { settings.update { AppSettings() } }
        runBlocking { settings.setTrainingMode(BODYBUILDING, today) }
        val third = todayVm()
        await("third") { !third.state.value.loading }
        Thread.sleep(50)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(third.state.value.welcomeBack)
    }

    // Coach

    @Test
    fun coach_showsTheActiveSidesFeed_withDeloadsWhateverTheirSide() {
        runBlocking {
            advice.insert(
                listOf(
                    Advice(AdviceType.STALL, MainLift.BENCH, today, "stalled", 3, AdvicePayload.Accessories(emptyList())),
                    Advice(AdviceType.DELOAD, null, today, "tired", 2, AdvicePayload.Deload()),
                    Advice(AdviceType.RE_ENTRY, null, today, "back", 20, AdvicePayload.ReEntry(), mode = BODYBUILDING),
                ),
            )
        }
        val vm = coachVm()
        assertEquals(listOf("tired", "back", "stalled"), vm.await { it.cards.size == 3 }.cards.map { it.advice.reason })
        onSide(BODYBUILDING)
        assertEquals(listOf("tired", "back"), vm.await { it.cards.size == 2 }.cards.map { it.advice.reason })
    }

    @Test
    fun coach_welcomeBack_whenTheSideHasAPlanAndWasLeftMoreThan30DaysAgo() {
        onSide(BODYBUILDING, leftOn = today.minus(31, DateTimeUnit.DAY))
        val vm = coachVm()
        assertFalse("no plan, no card", vm.await { true }.welcomeBack)
        save(bbItem(curl))
        vm.await { it.welcomeBack }
        vm.answerWelcomeBack()
        vm.await { !it.welcomeBack }
        assertNull(runBlocking { settings.settings.first() }.bodybuildingLeftOn)
    }

    @Test
    fun coach_powerliftingSideWelcomeBack_too() {
        save(plItem(squat))
        onSide(POWERLIFTING, leftOn = today.minus(40, DateTimeUnit.DAY))
        coachVm().await { it.welcomeBack }
    }

    // Progress

    @Test
    fun progress_bodybuildingSide_listsThePlansExercises_withBestSetsAndTrend() {
        onSide(BODYBUILDING)
        save(bbItem(curl), bbItem(squat, order = 1))
        session(today.minus(14, DateTimeUnit.DAY), BODYBUILDING, set(curl, 26.0, 10), set(curl, 26.0, 9))
        session(today.minus(7, DateTimeUnit.DAY), BODYBUILDING, set(curl, 26.0, 11), set(curl, 26.0, 10))
        session(today.minus(3, DateTimeUnit.DAY), POWERLIFTING, set(squat, 150.0, 5))
        val vm = ProgressViewModel(training, settings.showStrictPress, settings.settings.map { it.trainingMode }) { today }.also { viewModels += it }
        collectors.launch { vm.state.collect {} }
        await("rows") { vm.state.value.mode == BODYBUILDING && vm.state.value.trends.isNotEmpty() }
        val rows = vm.state.value.trends
        assertEquals(listOf("Curl", "Squat"), rows.map { it.name })
        assertEquals(listOf(10, 11), rows[0].bestSets.map { it.second.reps })
        assertEquals(true, rows[0].goingUp)
        assertEquals(emptyList<Any>(), rows[1].bestSets)
        assertNull(rows[1].goingUp)
    }

    @Test
    fun anExercisesHistory_showsItsSetsFromBothSides_newestFirst() {
        session(today.minus(9, DateTimeUnit.DAY), POWERLIFTING, set(curl, 20.0, 10))
        session(today.minus(2, DateTimeUnit.DAY), BODYBUILDING, set(curl, 26.0, 11), set(squat, 100.0, 5), set(curl, 26.0, 10))
        val state = com.powertracker.ui.progress.ExerciseHistoryViewModel.build(runBlocking { training.history().first() }, curl.id)
        assertEquals("Curl", state.name)
        assertEquals(listOf(listOf(26.0 to 11, 26.0 to 10), listOf(20.0 to 10)), state.sessions.map { s -> s.groups.single().sets.map { it.set.weightKg to it.set.reps } })
    }

    @Test
    fun aSessionLoggedOnTheBodybuildingSide_isInThatExercisesHistory_seenFromThePowerliftingSide_notInTheLiftHistory() {
        session(today.minus(2, DateTimeUnit.DAY), BODYBUILDING, set(bench, 60.0, 12))
        onSide(POWERLIFTING)
        val history = runBlocking { training.history().first() }
        val exercise = com.powertracker.ui.progress.ExerciseHistoryViewModel.build(history, bench.id)
        assertEquals(listOf(60.0), exercise.sessions.flatMap { s -> s.groups.flatMap { g -> g.sets.map { it.set.weightKg } } })
        // The lift history holds powerlifting sessions only (fix-round ruling A).
        assertEquals(emptyList<Any>(), LiftHistoryViewModel.build(history, MainLift.BENCH, today).sessions)
    }

    // Backup from the web app

    @Test
    fun webV3Backup_restoresBothSidesPlans_andThePlanScreenShowsEachSide() {
        val fixture = File("../web/src/backup/__fixtures__/web-v3.json")
        assertTrue(fixture.exists())
        val result = Backup.decode(fixture.readText())
        assertTrue(result.toString(), result is BackupResult.Ok)
        runBlocking { BackupService(db, settings).restore((result as BackupResult.Ok).file) }

        val vm = planVm()
        await("bodybuilding plan") { vm.state.value.mode == BODYBUILDING && vm.state.value.sections.any { it.rows.isNotEmpty() } }
        val bodybuilding = vm.state.value.sections.flatMap { it.rows }.map { it.item }
        assertEquals(2, bodybuilding.size)
        assertTrue(bodybuilding.all { it.mode == BODYBUILDING && it.repsHigh != null })
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY), vm.state.value.trainingDays)

        runBlocking { settings.setTrainingMode(POWERLIFTING, today) }
        await("powerlifting plan") { vm.state.value.mode == POWERLIFTING && vm.state.value.sections.any { it.rows.isNotEmpty() } }
        val powerlifting = vm.state.value.sections.flatMap { it.rows }.map { it.item }
        assertEquals(3, powerlifting.size)
        assertTrue(powerlifting.all { it.mode == POWERLIFTING && it.repsHigh == null })
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), vm.state.value.trainingDays)
    }
}
