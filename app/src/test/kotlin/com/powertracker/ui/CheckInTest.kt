package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MuscleRating
import com.powertracker.domain.model.NewExercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.SetAddition
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.ui.checkin.CheckInStep
import com.powertracker.ui.checkin.CheckInViewModel
import com.powertracker.ui.checkin.noteText
import com.powertracker.domain.coach.Coaching
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.coach.CoachViewModel
import com.powertracker.ui.progress.ProgressState
import com.powertracker.ui.progress.ProgressViewModel
import com.powertracker.ui.today.TodayViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Stage 3 on Android, behind the screens: the check-in falls due, is saved with the coach's suggestions in one go, the
 * suggestions apply to the plan with one tap, and Progress shows the body parts. None of it on the powerlifting side.
 */
@RunWith(RobolectricTestRunner::class)
class CheckInTest {
    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings(trainingMode = BODYBUILDING) } }
    }

    @After
    fun tearDown() {
        collectors.cancel()
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

    private fun onSide(mode: TrainingMode) = runBlocking { settings.update { it.copy(trainingMode = mode) } }

    private fun ago(days: Int) = today.minus(days, DateTimeUnit.DAY)

    private fun id(name: String) = runBlocking { training.exercises().first().first { it.name.equals(name, ignoreCase = true) }.id }

    /** A bodybuilding item for the catalogue exercise [name]. */
    private fun plan(name: String, day: DayOfWeek, order: Int, sets: Int = 3) = runBlocking {
        training.savePlanItem(PlanItem(0, day, 0, sets, 8, 8.0, order, BODYBUILDING, 12), name)
    }

    /** Monday: DB press, Lat pulldown, Leg press; Wednesday: DB press, Leg extension. */
    private fun standardPlan() {
        plan("DB press", DayOfWeek.MONDAY, 0)
        plan("Lat pulldown", DayOfWeek.MONDAY, 1)
        plan("Leg press", DayOfWeek.MONDAY, 2)
        plan("DB press", DayOfWeek.WEDNESDAY, 0)
        plan("Leg extension", DayOfWeek.WEDNESDAY, 1)
    }

    private fun session(date: LocalDate, mode: TrainingMode, vararg sets: Pair<String, Pair<Double, Int>>) = runBlocking {
        val sid = training.startSession(date, null, null, mode)
        for ((name, set) in sets) training.logSet(sid, LoggedSet(id(name), set.first, set.second, 8.0, failed = false, missPoint = null, isTopSet = false))
    }

    private fun names(): Map<Long, String> = runBlocking { training.exercises().first().associate { it.id to it.name } }

    private fun bbPlan(): List<Pair<DayOfWeek, Pair<String, Int>>> = runBlocking {
        val n = names()
        training.plan(BODYBUILDING).first().map { it.weekday to (n.getValue(it.exerciseId) to it.sets) }
    }

    private fun coachVm(repository: AdviceRepository = advice) = CoachViewModel(training, repository, settings) { today }.also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }

    private fun CoachViewModel.await(condition: (CoachState) -> Boolean): CoachState {
        await("coach state") { !state.value.loading && condition(state.value) }
        return state.value
    }

    private fun checkInVm(repository: AdviceRepository = advice) = CheckInViewModel(training, repository, settings) { today }.also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }

    private fun progressVm() = ProgressViewModel(training, settings.showStrictPress, settings.settings.map { it.trainingMode }) { today }
        .also { vm -> viewModels += vm; collectors.launch { vm.state.collect {} } }

    private fun ProgressViewModel.await(condition: (ProgressState) -> Boolean): ProgressState {
        await("progress state") { !state.value.loading && condition(state.value) }
        return state.value
    }

    private fun open(): List<Advice> = runBlocking { advice.open().first() }

    // ---- due ----

    @Test
    fun theDueCard_showsAt28Days_notAt27_andNeverOnThePowerliftingSide() {
        standardPlan()
        session(ago(27), BODYBUILDING, "DB press" to (26.0 to 10))
        val vm = coachVm()
        vm.await { true }
        assertFalse("27 days", vm.state.value.checkInDue)

        session(ago(28), BODYBUILDING, "DB press" to (26.0 to 9))
        vm.await { it.checkInDue }

        onSide(POWERLIFTING)
        vm.await { !it.checkInDue }
    }

    @Test
    fun aCheckIn_putsTheDueCardAwayForFourWeeks() {
        standardPlan()
        session(ago(40), BODYBUILDING, "DB press" to (26.0 to 10))
        val vm = coachVm()
        vm.await { it.checkInDue }
        runBlocking { training.saveCheckIn(CheckIn(ago(3), emptySet(), emptySet())) }
        vm.await { !it.checkInDue }
    }

    // ---- the check-in ----

    @Test
    fun aGroupTickedLagging_cannotBeTickedGrowingWell_andLeavesGrowingWellIfTickedLater() {
        val vm = checkInVm()
        await("loaded") { !vm.state.value.loading }
        vm.toggleLagging(MuscleGroup.CHEST)
        vm.next()
        await("step two") { vm.state.value.step == CheckInStep.GROWING }
        vm.toggleGrowing(MuscleGroup.CHEST)
        vm.toggleGrowing(MuscleGroup.QUADS)
        await("quads") { vm.state.value.growingWell == setOf(MuscleGroup.QUADS) }
        assertTrue(vm.back())
        vm.toggleLagging(MuscleGroup.QUADS)
        await("quads lagging") { vm.state.value.lagging == setOf(MuscleGroup.CHEST, MuscleGroup.QUADS) }
        assertEquals(emptySet<MuscleGroup>(), vm.state.value.growingWell)
        assertFalse("Back from the first step leaves", vm.back())
    }

    @Test
    fun leavingBeforeSave_storesNothing() {
        standardPlan()
        val vm = checkInVm()
        await("loaded") { !vm.state.value.loading }
        vm.toggleLagging(MuscleGroup.CHEST)
        vm.next()
        vm.toggleGrowing(MuscleGroup.QUADS)
        vm.viewModelScope.cancel()
        assertEquals(emptyList<CheckIn>(), runBlocking { training.checkIns().first() })
        assertEquals(emptyList<Advice>(), runBlocking { advice.all() })
    }

    @Test
    fun save_storesOneCheckIn_dismissesTheOldMoreWork_storesTheNewAdvice_andSaysWhatEachLaggingGroupGot() {
        standardPlan()
        session(ago(30), BODYBUILDING, "DB press" to (26.0 to 10))
        val old = Advice(
            AdviceType.MORE_WORK, null, ago(28), "Old", 0,
            AdvicePayload.MoreWork(MuscleGroup.BICEPS, emptyList(), NewExercise(DayOfWeek.MONDAY, "Cable curl", 3, 8, 12, 8.0), 0, 3),
            mode = BODYBUILDING,
        )
        runBlocking { advice.insert(listOf(old)) }

        val lagging = listOf(MuscleGroup.CHEST, MuscleGroup.TRAPS, MuscleGroup.LOWER_BACK, MuscleGroup.FOREARMS, MuscleGroup.CALVES)
        // What the coach makes of it, worked out from the same data before saving.
        val expected = runBlocking {
            Coaching.afterCheckIn(
                CheckIn(today, lagging.toSet(), setOf(MuscleGroup.QUADS)), training.history().first(), training.plan(BODYBUILDING).first(), today, advice.all(),
            )
        }
        assertEquals("the case has advice and notes", true, expected.advice.isNotEmpty() && expected.notes.isNotEmpty())

        val vm = checkInVm()
        await("loaded") { !vm.state.value.loading }
        for (g in lagging) vm.toggleLagging(g)
        vm.next()
        vm.toggleGrowing(MuscleGroup.QUADS)
        vm.save()
        await("saved") { vm.state.value.step == CheckInStep.SAVED }

        val stored = runBlocking { training.checkIns().first() }.single()
        assertEquals(today, stored.date)
        assertEquals(lagging.toSet(), stored.lagging)
        assertEquals(setOf(MuscleGroup.QUADS), stored.growingWell)
        val all = runBlocking { advice.all() }
        assertEquals(AdviceStatus.DISMISSED, all.first { it.reason == "Old" }.status)
        val fresh = all.filter { it.type == AdviceType.MORE_WORK && it.status == AdviceStatus.OPEN }
        assertEquals(expected.advice.map { it.payload }, fresh.map { it.payload })
        assertTrue(fresh.all { it.mode == BODYBUILDING })

        // One line per lagging group, in the grid's order.
        assertEquals(lagging.sortedBy { it.ordinal }, vm.state.value.lines.map { it.group })
        val lines = vm.state.value.lines.associate { it.group to it.text }
        val suggested = fresh.map { (it.payload as AdvicePayload.MoreWork).group }.toSet()
        for (g in suggested) assertEquals("More work suggested on Coach", lines.getValue(g))
        for (note in expected.notes) assertEquals(noteText(note), lines.getValue(note.group))
        assertTrue(vm.state.value.hasAdvice)
    }

    @Test
    fun aSecondCheckInTheSameDay_replacesTheFirst() {
        standardPlan()
        session(ago(30), BODYBUILDING, "DB press" to (26.0 to 10))
        for (lagging in listOf(MuscleGroup.CHEST, MuscleGroup.BICEPS)) {
            val vm = checkInVm()
            await("loaded") { !vm.state.value.loading }
            vm.toggleLagging(lagging)
            vm.next()
            vm.save()
            await("saved") { vm.state.value.step == CheckInStep.SAVED }
        }
        assertEquals(setOf(MuscleGroup.BICEPS), runBlocking { training.checkIns().first() }.single().lagging)
    }

    @Test
    fun aCheckInWithNothingMarked_isSavedWithNoLines() {
        standardPlan()
        val vm = checkInVm()
        await("loaded") { !vm.state.value.loading }
        vm.next()
        vm.save()
        await("saved") { vm.state.value.step == CheckInStep.SAVED }
        assertEquals(1, runBlocking { training.checkIns().first() }.size)
        assertEquals(emptyList<Any>(), vm.state.value.lines)
        assertFalse(vm.state.value.hasAdvice)
    }

    @Test
    fun theCheckInIsNotOfferedOnThePowerliftingSide() {
        onSide(POWERLIFTING)
        val vm = checkInVm()
        await("loaded") { !vm.state.value.loading }
        assertFalse(vm.state.value.available)
    }

    // ---- the Coach cards ----

    private fun moreWork(group: MuscleGroup, adds: List<SetAddition>, new: NewExercise?, before: Int, after: Int) = Advice(
        AdviceType.MORE_WORK, null, today, "More", 0, AdvicePayload.MoreWork(group, adds, new, before, after), mode = BODYBUILDING,
    )

    @Test
    fun moreWork_addToPlan_addsTheSetsTheAdviceSays_andGoesFromTheFeed() {
        standardPlan()
        val press = id("DB press")
        runBlocking {
            advice.insert(listOf(moreWork(MuscleGroup.CHEST,
                listOf(SetAddition(DayOfWeek.MONDAY, press, "DB press", 3, 4), SetAddition(DayOfWeek.WEDNESDAY, press, "DB press", 3, 4)), null, 6, 8)))
        }
        val vm = coachVm()
        val card = vm.await { s -> s.cards.any { it.detail is CardDetail.MoreWork } }.cards.single()
        val messages = mutableListOf<String>()
        collectors.launch { vm.messages.collect { messages += it } }
        vm.apply(card.advice.id)
        await("message") { messages.isNotEmpty() }

        assertEquals("Added a set to 2 exercises", messages.single())
        assertEquals(listOf(4, 4), bbPlan().filter { it.second.first == "DB press" }.map { it.second.second })
        assertEquals(AdviceStatus.APPLIED, runBlocking { advice.all() }.single().status)
        vm.await { it.cards.isEmpty() }
    }

    @Test
    fun moreWork_aNewExercise_isAddedAtTheEndOfItsDay() {
        standardPlan()
        runBlocking { advice.insert(listOf(moreWork(MuscleGroup.CHEST, emptyList(), NewExercise(DayOfWeek.WEDNESDAY, "Pec deck", 3, 8, 12, 8.0), 6, 9))) }
        val vm = coachVm()
        val card = vm.await { it.cards.isNotEmpty() }.cards.single()
        val messages = mutableListOf<String>()
        collectors.launch { vm.messages.collect { messages += it } }
        vm.apply(card.advice.id)
        await("message") { messages.isNotEmpty() }

        assertEquals("Added Pec deck to Wednesday", messages.single())
        assertEquals(DayOfWeek.WEDNESDAY to ("Pec deck" to 3), bbPlan().last())
        val item = runBlocking { training.plan(BODYBUILDING).first() }.last()
        assertEquals(8 to 12, item.reps to item.repsHigh)
        assertEquals(2, item.order)
    }

    @Test
    fun moreWork_afterThePlanChanged_saysSo_andTheCardGoes() {
        standardPlan()
        val press = id("DB press")
        runBlocking { advice.insert(listOf(moreWork(MuscleGroup.CHEST, listOf(SetAddition(DayOfWeek.MONDAY, press, "DB press", 2, 3)), null, 6, 7))) }
        val vm = coachVm()
        val card = vm.await { it.cards.isNotEmpty() }.cards.single()
        val before = bbPlan()
        val messages = mutableListOf<String>()
        collectors.launch { vm.messages.collect { messages += it } }
        vm.apply(card.advice.id)
        await("message") { messages.isNotEmpty() }

        assertEquals("Your plan has changed since; nothing was added", messages.single())
        assertEquals(before, bbPlan())
        vm.await { it.cards.isEmpty() }
    }

    private fun swapAdvice(exercise: String, alternatives: List<String>) = Advice(
        AdviceType.SWAP, null, today, "Stuck", 0,
        AdvicePayload.Swap(id(exercise), exercise, MuscleGroup.QUADS, ago(25), alternatives), mode = BODYBUILDING,
    )

    @Test
    fun swap_replacesTheExerciseOnEveryPlannedDay_keepingThePrescription() {
        plan("Leg extension", DayOfWeek.MONDAY, 0, sets = 4)
        plan("DB press", DayOfWeek.MONDAY, 1)
        plan("Leg extension", DayOfWeek.WEDNESDAY, 0)
        runBlocking { advice.insert(listOf(swapAdvice("Leg extension", listOf("Bulgarian split squat", "Split squat")))) }
        val vm = coachVm()
        val card = vm.await { s -> s.cards.any { it.detail is CardDetail.Swap } }.cards.single()
        val messages = mutableListOf<String>()
        collectors.launch { vm.messages.collect { messages += it } }
        vm.swap(card.advice.id, "Split squat")
        await("message") { messages.isNotEmpty() }

        assertEquals("Swapped Leg extension for Split squat", messages.single())
        assertEquals(
            listOf(DayOfWeek.MONDAY to ("Split squat" to 4), DayOfWeek.MONDAY to ("DB press" to 3), DayOfWeek.WEDNESDAY to ("Split squat" to 3)),
            bbPlan(),
        )
        assertEquals(AdviceStatus.APPLIED, runBlocking { advice.all() }.single().status)
    }

    @Test
    fun aSwapCard_isNotShownOnceItsExerciseLeftThePlan() {
        plan("DB press", DayOfWeek.MONDAY, 0)
        val gone = plan("Leg extension", DayOfWeek.MONDAY, 1)
        runBlocking { advice.insert(listOf(swapAdvice("Leg extension", listOf("Split squat")))) }
        runBlocking { training.deletePlanItem(gone) }
        val vm = coachVm()
        assertEquals(emptyList<Any>(), vm.await { true }.cards)
    }

    @Test
    fun dismiss_putsEitherCardAway() {
        standardPlan()
        runBlocking {
            advice.insert(listOf(
                moreWork(MuscleGroup.CHEST, emptyList(), NewExercise(DayOfWeek.WEDNESDAY, "Pec deck", 3, 8, 12, 8.0), 6, 9),
                swapAdvice("Leg extension", listOf("Split squat")),
            ))
        }
        val vm = coachVm()
        val cards = vm.await { it.cards.size == 2 }.cards
        val before = bbPlan()
        cards.forEach { vm.dismiss(it.advice.id) }
        vm.await { it.cards.isEmpty() }
        assertTrue(runBlocking { advice.all() }.all { it.status == AdviceStatus.DISMISSED })
        assertEquals(before, bbPlan())
    }

    @Test
    fun theNewCards_comeAfterADeload_andNeverShowOnThePowerliftingSide() {
        standardPlan()
        runBlocking {
            advice.insert(listOf(
                swapAdvice("Leg extension", listOf("Split squat")),
                moreWork(MuscleGroup.CHEST, emptyList(), NewExercise(DayOfWeek.WEDNESDAY, "Pec deck", 3, 8, 12, 8.0), 6, 9),
                Advice(AdviceType.DELOAD, null, today, "Tired", 1, AdvicePayload.Deload(), mode = BODYBUILDING),
            ))
        }
        val vm = coachVm()
        val types = vm.await { it.cards.size == 3 }.cards.map { it.advice.type }
        assertEquals(listOf(AdviceType.DELOAD, AdviceType.MORE_WORK, AdviceType.SWAP), types)
        onSide(POWERLIFTING)
        assertEquals(listOf(AdviceType.DELOAD), vm.await { it.cards.size == 1 }.cards.map { it.advice.type })
    }

    // ---- Today hands the check-ins to the coach ----

    @Test
    fun finishingASession_givesNoSwapForAGroupMarkedGrowingWell() {
        plan("Leg extension", DayOfWeek.WEDNESDAY, 0)
        session(ago(28), BODYBUILDING, "Leg extension" to (50.0 to 12))
        session(ago(14), BODYBUILDING, "Leg extension" to (50.0 to 10))
        session(ago(7), BODYBUILDING, "Leg extension" to (50.0 to 11))
        runBlocking { training.saveCheckIn(CheckIn(ago(2), emptySet(), setOf(MuscleGroup.QUADS))) }
        session(today, BODYBUILDING, "Leg extension" to (50.0 to 11))

        val vm = TodayViewModel(training, advice, settings) { today }.also { v -> viewModels += v; collectors.launch { v.state.collect {} } }
        await("today") { !vm.state.value.loading && vm.state.value.sessionId != null }
        vm.finishSession()
        await("summary") { vm.state.value.summary != null }
        assertTrue(runBlocking { advice.all() }.none { it.type == AdviceType.SWAP })

        // The same history without the check-in: the leg extension is stuck.
        runBlocking { db.checkIns().clear() }
        vm.finishSession()
        await("swap") { runBlocking { advice.all() }.any { it.type == AdviceType.SWAP } }
    }

    // ---- Progress ----

    @Test
    fun progress_showsTheBodyParts_withTheirRatings_onTheBodybuildingSideOnly() {
        standardPlan()
        session(ago(3), BODYBUILDING, "DB press" to (26.0 to 10), "DB press" to (26.0 to 9))
        val vm = progressVm()
        val before = vm.await { it.muscles.isNotEmpty() }
        assertFalse(before.checkedIn)
        val chest = before.muscles.first { it.group == MuscleGroup.CHEST }
        assertEquals(2, chest.setsDone)
        assertEquals(6, chest.setsPlanned)
        assertEquals(emptyList<Any>(), chest.ratings)

        runBlocking {
            training.saveCheckIn(CheckIn(ago(30), setOf(MuscleGroup.CHEST), emptySet()))
            training.saveCheckIn(CheckIn(ago(1), emptySet(), setOf(MuscleGroup.CHEST)))
        }
        val after = vm.await { it.checkedIn }
        assertEquals(listOf(MuscleRating.LAGGING, MuscleRating.GROWING_WELL), after.muscles.first { it.group == MuscleGroup.CHEST }.ratings.map { it.second })

        onSide(POWERLIFTING)
        val powerlifting = vm.await { it.mode == POWERLIFTING }
        assertEquals(emptyList<Any>(), powerlifting.muscles)
    }

    // ---- failed writes (fix round) ----

    /** Stores like the real repository, but the advice's status write fails: it comes after the plan change in one transaction. */
    private val failingStatus by lazy {
        object : AdviceRepository(db) {
            override suspend fun setStatus(id: Long, status: AdviceStatus) = throw IllegalStateException("disk full")
        }
    }

    private fun CoachViewModel.messagesInto(list: MutableList<String>) = collectors.launch { messages.collect { list += it } }

    @Test
    fun moreWork_whenStoringFails_bringsTheCardBack_andThePlanIsAsItWas() {
        standardPlan()
        val press = id("DB press")
        runBlocking { advice.insert(listOf(moreWork(MuscleGroup.CHEST, listOf(SetAddition(DayOfWeek.MONDAY, press, "DB press", 3, 4)), null, 6, 7))) }
        val before = bbPlan()
        val vm = coachVm(failingStatus)
        val id = vm.await { it.cards.size == 1 }.cards.single().advice.id
        val messages = mutableListOf<String>()
        vm.messagesInto(messages)

        vm.apply(id)
        await("message") { messages.isNotEmpty() }

        assertEquals(listOf("Couldn't update — try again"), messages)
        assertEquals(listOf(id), vm.await { it.cards.size == 1 }.cards.map { it.advice.id })
        assertEquals(before, bbPlan())
        assertEquals(AdviceStatus.OPEN, runBlocking { advice.all() }.single().status)
    }

    @Test
    fun swap_whenStoringFails_bringsTheCardBack_andThePlanIsAsItWas() {
        plan("Leg extension", DayOfWeek.MONDAY, 0)
        plan("Leg extension", DayOfWeek.WEDNESDAY, 0)
        runBlocking { advice.insert(listOf(swapAdvice("Leg extension", listOf("Split squat")))) }
        val before = bbPlan()
        val vm = coachVm(failingStatus)
        val id = vm.await { it.cards.size == 1 }.cards.single().advice.id
        val messages = mutableListOf<String>()
        vm.messagesInto(messages)

        vm.swap(id, "Split squat")
        await("message") { messages.isNotEmpty() }

        assertEquals(listOf("Couldn't update — try again"), messages)
        assertEquals(listOf(id), vm.await { it.cards.size == 1 }.cards.map { it.advice.id })
        assertEquals(before, bbPlan())
        assertTrue(runBlocking { training.exercises().first() }.none { it.name == "Split squat" })
    }

    @Test
    fun save_whenStoringFails_saysSo_andStoresNothing() {
        standardPlan()
        session(ago(30), BODYBUILDING, "DB press" to (26.0 to 10))
        val old = moreWork(MuscleGroup.BICEPS, emptyList(), NewExercise(DayOfWeek.MONDAY, "Cable curl", 3, 8, 12, 8.0), 0, 3)
        runBlocking { advice.insert(listOf(old)) }
        // The last write of the three fails: the check-in and the dismissal before it must go back with it.
        val failingInsert = object : AdviceRepository(db) {
            override suspend fun insert(advice: List<Advice>) = throw IllegalStateException("disk full")
        }
        val vm = checkInVm(failingInsert)
        await("loaded") { !vm.state.value.loading }
        vm.toggleLagging(MuscleGroup.CHEST)
        vm.next()
        vm.save()
        await("failed") { vm.state.value.failed }

        assertEquals(CheckInStep.GROWING, vm.state.value.step)
        assertEquals(emptyList<CheckIn>(), runBlocking { training.checkIns().first() })
        val all = runBlocking { advice.all() }
        assertEquals(1, all.size)
        assertEquals(AdviceStatus.OPEN, all.single().status)
    }

    // ---- an empty plan (fix round) ----

    @Test
    fun withAnEmptyPlan_noCheckInIsOffered() {
        session(ago(40), BODYBUILDING, "Curl" to (16.0 to 10))
        assertFalse(coachVm().await { true }.checkInDue)
        assertEquals(emptyList<Any>(), progressVm().await { true }.trends)
    }
}
