package com.powertracker.ui.plan

import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.domain.plan.PlanTemplates
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.coach.CoachViewModel
import com.powertracker.ui.plan.templates.TemplateStep
import com.powertracker.ui.plan.templates.TemplatesViewModel
import com.powertracker.ui.today.TodayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.SATURDAY
import kotlinx.datetime.DayOfWeek.THURSDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Choosing a plan template (1.14, Task 3): the list, the draft's weekdays and Swap, and saving over each side. */
@RunWith(RobolectricTestRunner::class)
class TemplatesViewModelTest {
    private val today = LocalDate(2026, 11, 11) // a Wednesday

    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val coachMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update {
                AppSettings(trainingDays = setOf(MONDAY, WEDNESDAY, FRIDAY), bodybuildingTrainingDays = setOf(TUESDAY, THURSDAY, SATURDAY))
            }
        }
    }

    @After
    fun tearDown() {
        collectors.cancel()
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

    private fun settle() {
        val deadline = System.currentTimeMillis() + 300
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }

    private fun vm(mode: TrainingMode): TemplatesViewModel {
        val vm = TemplatesViewModel(mode, training, settings)
        viewModels += vm
        await("loaded") { !vm.state.value.loading }
        return vm
    }

    /** [name]'s exercise row: a catalogue exercise not seeded gets its row, as saving it to a plan would. */
    private fun row(name: String): Exercise = runBlocking {
        training.exercises().first().firstOrNull { it.name == name } ?: training.inTransaction { training.resolveCatalogue(name) }.let { id ->
            training.exercises().first().first { it.id == id }
        }
    }
    private fun plan(mode: TrainingMode) = runBlocking { training.plan(mode).first() }
    private fun stored(): AppSettings = runBlocking { settings.settings.first() }
    private fun names(): Map<Long, String> = runBlocking { training.exercises().first() }.associate { it.id to it.name }

    /** [mode]'s plan as "MONDAY 0 Squat 3x5-null @ 8.0", in week and plan order. */
    private fun described(mode: TrainingMode): List<String> {
        val byId = names()
        return plan(mode).sortedWith(compareBy({ it.weekday }, { it.order }))
            .map { "${it.weekday} ${it.order} ${byId[it.exerciseId]} ${it.sets}x${it.reps}-${it.repsHigh} @ ${it.targetRpe}" }
    }

    private fun save(vm: TemplatesViewModel) {
        vm.use()
        if (vm.state.value.confirming) vm.confirmReplace()
        await("saved") { vm.state.value.saved }
    }

    // The list

    @Test
    fun list_isTheSidesTemplates_fewestDaysFirst() {
        assertEquals(listOf("pl-3", "pl-4", "pl-5"), vm(POWERLIFTING).state.value.templates.map { it.id })
        assertEquals(listOf("bb-3", "bb-4", "bb-5"), vm(BODYBUILDING).state.value.templates.map { it.id })
        assertEquals(TemplateStep.LIST, vm(POWERLIFTING).state.value.step)
    }

    // The draft

    @Test
    fun draft_startsWithThatSidesDays_whenTheCountMatches_elseTheSpread() {
        val pl = vm(POWERLIFTING)
        pl.choose("pl-3")
        assertEquals(TemplateStep.DRAFT, pl.state.value.step)
        assertEquals(setOf(MONDAY, WEDNESDAY, FRIDAY), pl.state.value.days)
        assertTrue(pl.state.value.canUse)
        assertTrue(pl.back())
        assertEquals(TemplateStep.LIST, pl.state.value.step)
        pl.choose("pl-4")
        assertEquals(PlanTemplates.suggestedDays(4, setOf(MONDAY, WEDNESDAY, FRIDAY)).toSet(), pl.state.value.days)
        assertFalse("back from the list leaves", run { pl.back(); pl.back() })

        // The bodybuilding side starts from its own days, not the powerlifting side's.
        val bb = vm(BODYBUILDING)
        bb.choose("bb-3")
        assertEquals(setOf(TUESDAY, THURSDAY, SATURDAY), bb.state.value.days)
        assertEquals(listOf(TUESDAY, THURSDAY, SATURDAY), bb.state.value.draft!!.sessions.map { it.day })
    }

    @Test
    fun aTemplateOfTheOtherSide_isNotChosen() {
        val pl = vm(POWERLIFTING)
        pl.choose("bb-3")
        assertEquals(TemplateStep.LIST, pl.state.value.step)
        assertNull(pl.state.value.chosen)
    }

    @Test
    fun useThisPlan_needsExactlyTheTemplatesDayCount() {
        val vm = vm(POWERLIFTING)
        vm.choose("pl-3")
        vm.toggleDay(FRIDAY)
        assertFalse(vm.state.value.canUse)
        vm.use()
        settle()
        assertFalse(vm.state.value.confirming)
        assertTrue("nothing stored with 2 of 3 days", plan(POWERLIFTING).isEmpty())
        vm.toggleDay(SATURDAY)
        vm.toggleDay(SUNDAY_)
        assertFalse("4 of 3 days", vm.state.value.canUse)
        vm.use()
        settle()
        assertTrue(plan(POWERLIFTING).isEmpty())
        vm.toggleDay(SUNDAY_)
        assertTrue(vm.state.value.canUse)
        assertEquals(listOf(MONDAY, WEDNESDAY, SATURDAY), vm.state.value.draft!!.sessions.map { it.day })
    }

    @Test
    fun swap_noneOnAMainLift_theTemplatesListOnAnAccessory_andItSurvivesAChangeOfDays() {
        val vm = vm(POWERLIFTING)
        vm.choose("pl-3")
        assertFalse(TemplatesViewModel.canSwap("Squat"))
        assertFalse(TemplatesViewModel.canSwap("Bench"))
        assertTrue(TemplatesViewModel.canSwap("Seated cable row"))
        assertTrue(vm.alternatives(MONDAY, "Squat").isEmpty())
        val options = vm.alternatives(MONDAY, "Seated cable row")
        val template = PlanTemplates.byId("pl-3")!!
        assertEquals(PlanTemplates.alternatives(template, vm.state.value.draft!!, MONDAY, "Seated cable row").map { it.name }, options.map { it.name })
        assertTrue(options.isNotEmpty())

        // A main lift is never swapped, even if asked.
        val before = vm.state.value.draft
        vm.swap(MONDAY, "Squat", "Leg press")
        assertEquals(before, vm.state.value.draft)

        val to = options.first().name
        vm.swap(MONDAY, "Seated cable row", to)
        val monday = vm.state.value.draft!!.sessions.first { it.day == MONDAY }
        val swapped = monday.exercises.first { it.name == to }
        assertEquals(listOf(3, 10, 8.0), listOf(swapped.sets, swapped.repsLow, swapped.rpe))

        // Moving Monday to Tuesday keeps the swap.
        vm.toggleDay(MONDAY)
        vm.toggleDay(TUESDAY)
        val tuesday = vm.state.value.draft!!.sessions.first()
        assertEquals(TUESDAY, tuesday.day)
        assertTrue(tuesday.exercises.any { it.name == to })
        assertTrue(tuesday.exercises.none { it.name == "Seated cable row" })
    }

    @Test
    fun strictPress_onlyWhenTheSettingIsOn() {
        val off = vm(POWERLIFTING)
        off.choose("pl-3")
        assertTrue(off.state.value.draft!!.sessions.flatMap { it.exercises }.none { it.name == "Strict press" })
        runBlocking { settings.setShowStrictPress(true) }
        val on = vm(POWERLIFTING)
        on.choose("pl-3")
        assertEquals(2, on.state.value.draft!!.sessions.flatMap { it.exercises }.count { it.name == "Strict press" })
    }

    // Saving

    @Test
    fun aSaveOnAnEmptyPowerliftingSide_storesTheTemplateOnTheChosenDays_withFixedReps_andSetsItsDays() {
        val bbBefore = stored().bodybuildingTrainingDays
        val vm = vm(POWERLIFTING)
        vm.choose("pl-3")
        vm.toggleDay(FRIDAY)
        vm.toggleDay(SATURDAY)
        val draft = vm.state.value.draft!!
        vm.use()
        assertFalse("an empty side asks nothing", vm.state.value.confirming)
        await("saved") { vm.state.value.saved }

        assertEquals(
            draft.sessions.flatMap { s -> s.exercises.mapIndexed { i, e -> "${s.day} $i ${e.name} ${e.sets}x${e.repsLow}-null @ ${e.rpe}" } },
            described(POWERLIFTING),
        )
        assertEquals(setOf(MONDAY, WEDNESDAY, SATURDAY), plan(POWERLIFTING).map { it.weekday }.toSet())
        assertTrue("powerlifting items have fixed reps", plan(POWERLIFTING).all { it.repsHigh == null })
        assertEquals(setOf(MONDAY, WEDNESDAY, SATURDAY), stored().trainingDays)
        assertEquals("the bodybuilding days are untouched", bbBefore, stored().bodybuildingTrainingDays)
        assertTrue(plan(BODYBUILDING).isEmpty())
    }

    @Test
    fun aSaveOnAnEmptyBodybuildingSide_keepsTheRanges_andSetsOnlyItsDays() {
        val plBefore = stored().trainingDays
        val vm = vm(BODYBUILDING)
        vm.choose("bb-4")
        assertEquals(PlanTemplates.suggestedDays(4, setOf(TUESDAY, THURSDAY, SATURDAY)).toSet(), vm.state.value.days)
        val draft = vm.state.value.draft!!
        save(vm)
        assertEquals(
            draft.sessions.flatMap { s -> s.exercises.mapIndexed { i, e -> "${s.day} $i ${e.name} ${e.sets}x${e.repsLow}-${e.repsHigh} @ ${e.rpe}" } },
            described(BODYBUILDING),
        )
        assertTrue(plan(BODYBUILDING).all { it.repsHigh == 12 && it.reps == 8 })
        assertEquals(vm.state.value.days, stored().bodybuildingTrainingDays)
        assertEquals("the powerlifting days are untouched", plBefore, stored().trainingDays)
        assertTrue(plan(POWERLIFTING).isEmpty())
    }

    @Test
    fun replacing_asksFirst_cancelStoresNothing_andReplaceTouchesOnlyThatSide() {
        val squat = row("Squat")
        val curl = row("Curl")
        var skipId = 0L
        runBlocking {
            val old = training.savePlanItem(PlanItem(0, TUESDAY, curl.id, 4, 6, 7.0, 0))
            training.savePlanItem(PlanItem(0, MONDAY, curl.id, 3, 8, 8.0, 0, BODYBUILDING, 12))
            training.setBaseline(Baseline(squat.id, 180.0, today.minus(20, DateTimeUnit.DAY)))
            val s = training.startSession(today.minus(6, DateTimeUnit.DAY), 90.0, 4)
            training.logSet(s, LoggedSet(squat.id, 140.0, 5, 8.0, false, null, true))
            training.saveCheckIn(CheckIn(today.minus(3, DateTimeUnit.DAY), setOf(MuscleGroup.CHEST), setOf(MuscleGroup.QUADS)))
            skipId = training.skip(Skip(0, today.minus(1, DateTimeUnit.DAY), POWERLIFTING, curl.id, old, SkipReason.BUSY)) { _, _ -> emptyList() }
        }
        val pl = described(POWERLIFTING)
        val bb = plan(BODYBUILDING)
        val days = stored()
        val history = runBlocking { training.history().first() }
        val checkIns = runBlocking { training.checkIns().first() }
        val adviceBefore = runBlocking { advice.all() }

        val vm = vm(POWERLIFTING)
        assertTrue(vm.state.value.hasPlan)
        vm.choose("pl-4")
        vm.use()
        assertTrue(vm.state.value.confirming)
        vm.cancelReplace()
        assertFalse(vm.state.value.confirming)
        settle()
        assertEquals("Cancel stores nothing", pl, described(POWERLIFTING))
        assertEquals(days, stored())

        vm.use()
        vm.confirmReplace()
        await("saved") { vm.state.value.saved }
        assertEquals(PlanTemplates.byId("pl-4")!!.sessions.sumOf { it.exercises.count { e -> !e.onlyWithStrictPress } }, plan(POWERLIFTING).size)
        assertTrue(plan(POWERLIFTING).none { it.exerciseId == curl.id })
        assertEquals(vm.state.value.days, stored().trainingDays)
        assertEquals(days.bodybuildingTrainingDays, stored().bodybuildingTrainingDays)
        assertEquals("the other side is untouched", bb, plan(BODYBUILDING))
        assertEquals("history and bests are kept", history.sessions, runBlocking { training.history().first() }.sessions)
        assertEquals(history.baselines, runBlocking { training.history().first() }.baselines)
        assertEquals(checkIns, runBlocking { training.checkIns().first() })
        assertEquals("advice is left as it is", adviceBefore, runBlocking { advice.all() })
        val skip = runBlocking { training.skips().first() }.single()
        assertEquals(skipId, skip.id)
        assertEquals(curl.id, skip.exerciseId)
        assertNull("the skip keeps its record, its plan item gone", skip.planItemId)
    }

    @Test
    fun whenSettingTheDaysFails_itSaysSo_andTryingAgainSaves() {
        var failNext = true
        val vm = TemplatesViewModel(POWERLIFTING, training, settings, setDays = { mode, days ->
            if (failNext) {
                failNext = false
                throw java.io.IOException("settings store unavailable")
            }
            com.powertracker.ui.plan.builder.setSideTrainingDays(settings, mode, days)
        })
        viewModels += vm
        await("loaded") { !vm.state.value.loading }
        vm.choose("pl-4")
        vm.use()
        await("failed") { vm.state.value.failed }
        assertFalse(vm.state.value.saved)
        save(vm)
        assertFalse(vm.state.value.failed)
        assertEquals(vm.state.value.days, stored().trainingDays)
    }

    // The coach on a template's exercises

    private fun todaysTarget(exercise: Exercise): AdvicePayload.Target? = runBlocking { advice.all() }
        .filter { it.type == AdviceType.TARGET && it.createdAt == today && it.status == AdviceStatus.OPEN }
        .map { it.payload as AdvicePayload.Target }.singleOrNull { it.exerciseId == exercise.id }

    private fun todayVm(): TodayViewModel = TodayViewModel(training, advice, settings) { today }.also { vm ->
        viewModels += vm
        collectors.launch { vm.state.collect {} }
    }

    @Test
    fun aNewLifter_getsNoTargetOnATemplatesLifts() {
        save(vm(POWERLIFTING).also { it.choose("pl-3") })
        val today = todayVm()
        await("today's rows") { today.state.value.items.map { it.exercise.name } == WEDNESDAY_PL_3 }
        settle()
        assertNull(todaysTarget(row("Deadlift")))
        assertNull(todaysTarget(row("Bench")))
    }

    /** Bench is on Monday and Wednesday in the 3-day template, 3 × 5 @ 8 on both: Wednesday's target is a plain 3 × 5 bench's. */
    @Test
    fun withHistory_aLiftOnTwoDays_getsTheSameTargetAsAPlainPlan() {
        val bench = row("Bench")
        runBlocking {
            training.setBaseline(Baseline(bench.id, 125.0, today.minus(30, DateTimeUnit.DAY)))
            training.logSet(training.startSession(today.minus(7, DateTimeUnit.DAY), null, null), LoggedSet(bench.id, 100.0, 5, 7.0, false, null, true))
            training.savePlanItem(PlanItem(0, WEDNESDAY, bench.id, 3, 5, 8.0, 0))
        }
        val control = todayVm()
        await("control target") { todaysTarget(bench) != null }
        val expected = todaysTarget(bench)!!
        control.viewModelScope.cancel()
        runBlocking { db.advice().clear(); db.plan().clear() }

        save(vm(POWERLIFTING).also { it.choose("pl-3") })
        assertEquals(2, plan(POWERLIFTING).count { it.exerciseId == bench.id })
        todayVm()
        await("target") { todaysTarget(bench) != null }
        val target = todaysTarget(bench)!!
        assertEquals(expected.weightKg, target.weightKg, 0.0)
        assertEquals(expected.reps, target.reps)
        assertTrue("the weight goes up from 100 kg", target.weightKg > 100.0)
    }

    // Replacing with state tied to the old plan

    private fun coach(): CoachViewModel = CoachViewModel(training, advice, settings) { today }.also { vm ->
        viewModels += vm
        collectors.launch { vm.state.collect {} }
        collectors.launch { vm.messages.collect { coachMessages += it } }
    }

    private fun card(type: AdviceType, mode: TrainingMode, payload: AdvicePayload): Long = runBlocking {
        advice.insert(listOf(Advice(type, null, today.minus(1, DateTimeUnit.DAY), "card", 1, payload, mode = mode)))
        advice.all().last().id
    }

    private fun status(id: Long) = runBlocking { advice.all() }.first { it.id == id }.status

    /** An open SKIP card for an old powerlifting item, Apply after the side is replaced: absent and present in the template. */
    @Test
    fun anOpenSkipCard_forAnOldItem_thenApply_afterAPowerliftingReplace() {
        val curl = row("Curl")
        val pulldown = row("Lat pulldown")
        val (absent, present) = runBlocking {
            val curlItem = training.savePlanItem(PlanItem(0, MONDAY, curl.id, 3, 10, 8.0, 0))
            val pulldownItem = training.savePlanItem(PlanItem(0, WEDNESDAY, pulldown.id, 3, 10, 8.0, 0))
            val curlSkip = training.skip(Skip(0, today.minus(1, DateTimeUnit.DAY), POWERLIFTING, curl.id, curlItem, SkipReason.BUSY)) { _, _ -> emptyList() }
            val pulldownSkip = training.skip(Skip(0, today.minus(2, DateTimeUnit.DAY), POWERLIFTING, pulldown.id, pulldownItem, SkipReason.BUSY)) { _, _ -> emptyList() }
            card(AdviceType.SKIP, POWERLIFTING, AdvicePayload.Skipped(curlSkip, curl.id, "Curl", SkipReason.BUSY, listOf("Hammer curl"))) to
                card(AdviceType.SKIP, POWERLIFTING, AdvicePayload.Skipped(pulldownSkip, pulldown.id, "Lat pulldown", SkipReason.BUSY, listOf("Chin-up")))
        }
        val coach = coach()
        await("cards") { coach.state.value.cards.size == 2 }
        save(vm(POWERLIFTING).also { it.choose("pl-3") })
        val replaced = described(POWERLIFTING)

        // Curl is not in the template: nothing is swapped, the card stays, as after Rebuild my plan.
        coach.swap(absent, "Hammer curl")
        await("message") { coachMessages.isNotEmpty() }
        assertEquals(com.powertracker.domain.coach.PhysiqueCoach.NOTHING_SWAPPED, coachMessages.last())
        assertEquals(AdviceStatus.OPEN, status(absent))
        assertEquals(replaced, described(POWERLIFTING))

        // Lat pulldown is in the template on Wednesday: the swap lands on the new item.
        coach.swap(present, "Chin-up")
        await("applied") { status(present) == AdviceStatus.APPLIED }
        val byId = names()
        val wednesday = plan(POWERLIFTING).filter { it.weekday == WEDNESDAY }.map { byId[it.exerciseId] }
        assertTrue(wednesday.contains("Chin-up"))
        assertFalse(wednesday.contains("Lat pulldown"))
        assertEquals(replaced.size, plan(POWERLIFTING).size)
        assertTrue(plan(POWERLIFTING).all { it.repsHigh == null })
    }

    /** An open SWAP card (bodybuilding) for an old item, Apply after the side is replaced: absent and present in the template. */
    @Test
    fun anOpenSwapCard_forAnOldItem_thenApply_afterABodybuildingReplace() {
        runBlocking { settings.update { it.copy(trainingMode = BODYBUILDING) } }
        val pecDeck = row("Pec deck")
        val pulldown = row("Lat pulldown")
        runBlocking {
            training.savePlanItem(PlanItem(0, TUESDAY, pecDeck.id, 3, 8, 8.0, 0, BODYBUILDING, 12))
            training.savePlanItem(PlanItem(0, THURSDAY, pulldown.id, 3, 8, 8.0, 0, BODYBUILDING, 12))
        }
        val absent = card(AdviceType.SWAP, BODYBUILDING, AdvicePayload.Swap(pecDeck.id, "Pec deck", MuscleGroup.CHEST, today.minus(30, DateTimeUnit.DAY), listOf("Cable crossover")))
        val present = card(AdviceType.SWAP, BODYBUILDING, AdvicePayload.Swap(pulldown.id, "Lat pulldown", MuscleGroup.LATS, today.minus(30, DateTimeUnit.DAY), listOf("Neutral-grip pull-up")))
        val coach = coach()
        await("cards") { coach.state.value.cards.size == 2 }
        save(vm(BODYBUILDING).also { it.choose("bb-3") })
        val replaced = described(BODYBUILDING)

        coach.swap(absent, "Cable crossover")
        await("message") { coachMessages.isNotEmpty() }
        assertEquals(com.powertracker.domain.coach.PhysiqueCoach.NOTHING_SWAPPED, coachMessages.last())
        assertEquals(AdviceStatus.OPEN, status(absent))
        assertEquals(replaced, described(BODYBUILDING))

        coach.swap(present, "Neutral-grip pull-up")
        await("applied") { status(present) == AdviceStatus.APPLIED }
        val byId = names()
        val names = plan(BODYBUILDING).map { byId[it.exerciseId] }
        assertTrue(names.contains("Neutral-grip pull-up"))
        assertFalse(names.contains("Lat pulldown"))
        assertEquals(replaced.size, plan(BODYBUILDING).size)
    }

    /** A skip made today keeps its record through a replace, and Today still lists it as skipped when the template plans it. */
    @Test
    fun aSkipMadeToday_keepsItsRecord_throughAReplace() {
        val bench = row("Bench")
        val curl = row("Curl")
        val items = runBlocking {
            listOf(training.savePlanItem(PlanItem(0, WEDNESDAY, bench.id, 3, 5, 8.0, 0)), training.savePlanItem(PlanItem(0, WEDNESDAY, curl.id, 3, 10, 8.0, 1)))
        }
        val today = todayVm()
        await("rows") { today.state.value.items.size == 2 }
        today.skip(bench.id, items[0], SkipReason.BUSY)
        await("bench skipped") { runBlocking { training.skips().first() }.size == 1 }
        // Curl, which the template does not plan, skipped today too (stored as Today stores it, with no card for "Out of time").
        runBlocking { training.skip(Skip(0, this@TemplatesViewModelTest.today, POWERLIFTING, curl.id, items[1], SkipReason.TIME)) { _, _ -> emptyList() } }
        await("curl skipped") { runBlocking { training.skips().first() }.size == 2 }
        val skips = runBlocking { training.skips().first() }

        save(vm(POWERLIFTING).also { it.choose("pl-3") })

        val after = runBlocking { training.skips().first() }
        assertEquals(skips.map { it.copy(planItemId = null) }, after)
        // Wednesday is the template's now: bench is still skipped today, the rest are on the list.
        await("today follows the new plan") { today.state.value.items.map { it.exercise.name } == WEDNESDAY_PL_3 - "Bench" }
        assertEquals(listOf("Bench"), today.state.value.skippedToday.map { it.name })
    }

    /** A session in progress keeps its logged sets through a replace, and logging goes on against the new plan. */
    @Test
    fun aSessionInProgress_keepsItsLoggedSets_throughAReplace() {
        val bench = row("Bench")
        val item = runBlocking { training.savePlanItem(PlanItem(0, WEDNESDAY, bench.id, 3, 5, 8.0, 0)) }
        val today = todayVm()
        await("row") { today.state.value.items.size == 1 }
        today.startSession(85.0, 4)
        await("session") { today.state.value.sessionId != null }
        val set = LoggedSet(bench.id, 100.0, 5, 8.0, false, null, false)
        today.logSet(item, bench.id, set, confirmed = true)
        await("logged") { today.state.value.items.single().sets.size == 1 }

        save(vm(POWERLIFTING).also { it.choose("pl-3") })
        await("today follows the new plan") { today.state.value.items.map { it.exercise.name }.toSet() == WEDNESDAY_PL_3.toSet() }
        val benchRow = today.state.value.items.first { it.exercise.id == bench.id }
        assertEquals("the logged set is kept", 1, benchRow.sets.size)
        assertNotNull(benchRow.planned)

        // A set logged with the old item's id (what a screen open across the replace holds) is stored, not lost.
        today.logSet(item, bench.id, set.copy(weightKg = 102.5), confirmed = true)
        await("second set") { today.state.value.items.first { it.exercise.id == bench.id }.sets.size == 2 }
        val session = today.state.value.sessionId!!
        assertEquals(listOf(100.0, 102.5), runBlocking { training.setsForSession(session).first() }.map { it.set.weightKg })
    }

    private companion object {
        val SUNDAY_ = DayOfWeek.SUNDAY

        /** The 3-day powerlifting template's Wednesday (its second day, on Monday, Wednesday, Friday), strict press off. */
        val WEDNESDAY_PL_3 = listOf("Deadlift", "Bench", "Lat pulldown", "Triceps pushdown")
    }
}
