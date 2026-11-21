package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.validation.InputWarning
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.TodayItem
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

/** Counting plates on Today (1.17, Task 7): the setting, the field, the targets, the typo check and the top set. */
@RunWith(RobolectricTestRunner::class)
class TodayPlatesViewModelTest {
    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings() } }
    }

    @After
    fun tearDown() {
        collectors.cancel()
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    private fun viewModel() = TodayViewModel(training, advice, settings) { today }.also { vm ->
        viewModels += vm
        collectors.launch { vm.state.collect {} }
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        }
    }

    /** The row of the catalogue exercise [name], made as Plan or a first set would make it. */
    private fun row(name: String): Long = runBlocking { training.inTransaction { training.resolveCatalogue(name) } }

    private fun plan(exerciseId: Long, order: Int = 0, reps: Int = 8) = runBlocking {
        training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, exerciseId, sets = 3, reps = reps, targetRpe = 8.0, order = order))
    }

    private fun set(exerciseId: Long, weight: Double, reps: Int = 8, rpe: Double? = 8.0, unit: LoadUnit = LoadUnit.KG, failed: Boolean = false) =
        LoggedSet(exerciseId, weight, reps, rpe, failed = failed, missPoint = null, isTopSet = false, unit = unit)

    /** A past session [daysAgo] before today holding [sets]. */
    private fun session(daysAgo: Int, vararg sets: LoggedSet) = runBlocking {
        val id = training.startSession(today.minus(daysAgo, DateTimeUnit.DAY), null, null)
        for (s in sets) training.logSet(id, s)
    }

    private fun setting(exerciseId: Long, value: PlateCount) = runBlocking { training.setPlateCount(exerciseId, null, value) }

    private fun TodayViewModel.item(id: Long): TodayItem? = state.value.items.firstOrNull { it.exercise.id == id }

    private fun todaysSets() = runBlocking {
        val sessionId = training.sessionOn(today).first()!!.id
        training.setsForSession(sessionId).first().map { it.set }
    }

    /** Logs [set] in the exercise's current unit, as the screen builds it from the field, and waits until it is on screen (or held as a typo). */
    private fun TodayViewModel.log(id: Long, set: LoggedSet) {
        val before = item(id)?.sets?.size ?: 0
        logSet(item(id)?.planned?.id, id, set.copy(unit = item(id)!!.unit))
        await("set saved or held") { state.value.pending != null || (item(id)?.sets?.size ?: 0) > before }
    }

    private fun TodayViewModel.turn(id: Long, value: PlateCount) {
        setPlateCount(id, value)
        val unit = if (value == PlateCount.OFF) LoadUnit.KG else LoadUnit.PLATES
        await("the setting on screen") { item(id)?.plateCount == value && item(id)?.unit == unit }
    }

    @Test
    fun turningPlatesOn_emptiesTheField_whateverWasInIt() {
        val typed = row("Lat pulldown")
        val applied = row("Leg press")
        val logged = row("Seated cable row")
        plan(typed, 0)
        plan(applied, 1)
        plan(logged, 2)
        runBlocking {
            training.startSession(today, null, null)
            advice.insert(listOf(Advice(AdviceType.TARGET, null, today, "Coach suggests 120 × 8 @ 8.", 0, AdvicePayload.Target(applied, 120.0, 8, 8.0))))
        }
        val vm = viewModel()
        await("items") { vm.state.value.items.size == 3 && vm.item(applied)?.suggestion != null }

        // 40 typed and kept in the editor.
        vm.edit(typed, vm.item(typed)!!.editor.copy(weightKg = 40.0))
        await("typed") { vm.item(typed)?.editor?.weightKg == 40.0 }
        // A kg target applied.
        vm.applyTarget(vm.item(applied)!!.suggestion!!.id)
        await("applied") { vm.item(applied)?.editor?.weightKg == 120.0 }
        // A kg set logged today.
        vm.log(logged, set(logged, 60.0))
        await("logged") { vm.item(logged)?.editor?.weightKg == 60.0 }

        for (id in listOf(typed, applied, logged)) {
            vm.turn(id, PlateCount.WHOLE)
            val editor = vm.item(id)!!.editor
            assertNull("the PLATE field is empty, never a kg number ($id)", editor.weightKg)
            assertEquals(LoadUnit.PLATES, editor.unit)
        }
    }

    @Test
    fun theNextSetIsLoggedInPlates_andEarlierSetsKeepTheirUnit() {
        val id = row("Lat pulldown")
        plan(id)
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("item") { vm.item(id) != null }

        vm.log(id, set(id, 60.0))
        vm.log(id, set(id, 62.5))
        vm.turn(id, PlateCount.WHOLE)
        // The screen builds the set from the field, in the exercise's unit.
        vm.log(id, set(id, 7.0))

        assertNull(vm.state.value.pending)
        assertEquals(listOf(LoadUnit.KG, LoadUnit.KG, LoadUnit.PLATES), todaysSets().map { it.unit })
        assertEquals(listOf(60.0, 62.5, 7.0), todaysSets().map { it.weightKg })
        assertEquals(listOf(LoadUnit.KG, LoadUnit.KG, LoadUnit.PLATES), vm.item(id)!!.sets.map { it.set.unit })
    }

    /** Lat pulldown planned 3 × 8 @ 8, with three kg sessions behind it: the coach stores a kg target today. */
    private fun latPulldownWithKgHistory(): Long {
        val id = row("Lat pulldown")
        plan(id)
        session(21, set(id, 55.0))
        session(14, set(id, 57.5))
        session(7, set(id, 60.0))
        return id
    }

    private fun targets(id: Long) = runBlocking { advice.all() }.filter { it.type == AdviceType.TARGET && (it.payload as AdvicePayload.Target).exerciseId == id }

    private fun Advice.unit() = (payload as AdvicePayload.Target).unit

    @Test
    fun aKgTargetIsNotShownOrAppliedForAPlateExercise_andNoStoredAdviceChanges() {
        val id = latPulldownWithKgHistory()
        val vm = viewModel()
        await("kg target") { vm.item(id)?.suggestion != null }
        val kg = vm.item(id)!!.suggestion!!
        assertEquals(LoadUnit.KG, kg.unit())
        val stored = runBlocking { advice.all() }
        val passes = vm.coachPasses.value

        vm.turn(id, PlateCount.WHOLE)
        await("the coach asked again") { vm.coachPasses.value > passes }

        val item = vm.item(id)!!
        assertNull("a kg target never stands for a plate exercise", item.suggestion)
        assertNull(item.editor.weightKg)
        // Applying it (a stale tap) puts nothing in the field and changes nothing stored.
        vm.applyTarget(kg.id)
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(100)
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(vm.item(id)!!.editor.weightKg)
        assertEquals("kg history then plates: no new target, and nothing stored changes", stored, runBlocking { advice.all() })
    }

    @Test
    fun switchingBackToKgTheSameDay_showsTheKgTargetAgain() {
        val id = latPulldownWithKgHistory()
        val vm = viewModel()
        await("kg target") { vm.item(id)?.suggestion != null }
        val kg = vm.item(id)!!.suggestion!!

        vm.turn(id, PlateCount.WHOLE)
        await("hidden") { vm.item(id)?.suggestion == null }
        val passes = vm.coachPasses.value
        vm.turn(id, PlateCount.OFF)
        await("the coach asked again") { vm.coachPasses.value > passes }
        await("kg target back") { vm.item(id)?.suggestion != null }

        assertEquals(kg.id, vm.item(id)!!.suggestion!!.id)
        assertEquals("no second kg target is stored", 1, targets(id).size)
    }

    @Test
    fun turningPlatesBackOnWithOlderPlateHistory_storesAPlateTargetThatDay_besideTheKgOne() {
        val id = row("Lat pulldown")
        plan(id)
        session(28, set(id, 7.0, unit = LoadUnit.PLATES))
        session(21, set(id, 7.0, unit = LoadUnit.PLATES))
        session(14, set(id, 55.0))
        session(7, set(id, 57.5))
        val vm = viewModel()
        await("kg target") { vm.item(id)?.suggestion != null }
        assertEquals(LoadUnit.KG, vm.item(id)!!.suggestion!!.unit())

        vm.turn(id, PlateCount.WHOLE)
        await("plate target") { vm.item(id)?.suggestion?.unit() == LoadUnit.PLATES }

        val stored = targets(id)
        assertEquals(listOf(LoadUnit.KG, LoadUnit.PLATES), stored.map { it.unit() })
        assertTrue(stored.all { it.createdAt == today })
        assertEquals(7.0, (vm.item(id)!!.suggestion!!.payload as AdvicePayload.Target).weightKg, 0.0)
    }

    @Test
    fun theFieldFills_fromThisUnitOnly() {
        val id = row("Lat pulldown")
        plan(id)
        session(14, set(id, 7.0, unit = LoadUnit.PLATES))
        session(7, set(id, 60.0))
        setting(id, PlateCount.WHOLE)
        val vm = viewModel()
        await("item") { vm.item(id) != null }

        val item = vm.item(id)!!
        assertEquals(LoadUnit.PLATES, item.unit)
        assertEquals(7.0, item.editor.weightKg!!, 0.0)
        assertEquals(LoadUnit.PLATES, item.editor.unit)
        assertEquals(7.0, item.lastWeightKg!!, 0.0)
        assertNull("no plates for a plate number", item.perSide)
    }

    @Test
    fun theFirstPlateSetAfterKg_raisesNoBigJump_andPlate3To5Does() {
        val id = row("Lat pulldown")
        plan(id)
        session(7, set(id, 60.0))
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("item") { vm.item(id) != null }
        vm.log(id, set(id, 60.0))
        vm.turn(id, PlateCount.WHOLE)

        vm.log(id, set(id, 7.0))
        assertNull("plate 7 after 60 kg is no jump", vm.state.value.pending)
        vm.log(id, set(id, 3.0))
        assertNull(vm.state.value.pending)
        vm.log(id, set(id, 5.0))
        assertEquals(listOf<InputWarning>(InputWarning.BigJump(3.0, LoadUnit.PLATES)), vm.state.value.pending?.warnings)
        vm.dismissWarning()

        // A kg number under the plate's: plate 7 after 5 kg (today and before) would be a "jump" if kg were read.
        val light = row("Seated cable row")
        plan(light, 1)
        session(14, set(light, 5.0))
        vm.addExercise(light)
        await("second item") { vm.item(light) != null }
        vm.log(light, set(light, 5.0))
        vm.turn(light, PlateCount.WHOLE)
        assertNull(vm.item(light)!!.lastWeightKg)
        vm.log(light, set(light, 7.0))
        assertNull("plate 7 after 5 kg is no jump", vm.state.value.pending)
        assertEquals(LoadUnit.PLATES, vm.item(light)!!.sets.last().set.unit)
    }

    @Test
    fun theFirstKgSetAfterPlates_raisesNoBigJump() {
        val id = row("Leg press")
        plan(id)
        session(7, set(id, 7.0, unit = LoadUnit.PLATES))
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("item") { vm.item(id) != null }
        assertEquals(LoadUnit.KG, vm.item(id)!!.unit)
        assertNull("no kg number to judge by", vm.item(id)!!.lastWeightKg)
        assertNull("the field is empty, never plate 7 read as kg", vm.item(id)!!.editor.weightKg)

        vm.log(id, set(id, 120.0))
        assertNull("120 kg after plate 7 is no jump", vm.state.value.pending)
    }

    @Test
    fun aStandIn_getsItsRowWhenPlatesAreChosen_andItsCardStays() {
        runBlocking { training.startSession(today, null, null) }
        assertTrue("Lat pulldown has no row yet", runBlocking { training.exercises().first() }.none { it.name == "Lat pulldown" })
        val vm = viewModel()
        await("loaded") { !vm.state.value.loading }
        val standIn = vm.addFromCatalogue("Lat pulldown")!!
        assertTrue("a stand-in id", standIn < 0)
        await("card") { vm.item(standIn) != null }
        assertTrue(vm.item(standIn)!!.plateOffered)

        vm.setPlateCount(standIn, PlateCount.HALF)
        await("row") { vm.state.value.resolved[standIn] != null }
        val rowId = vm.state.value.resolved.getValue(standIn)
        await("its card in plates") { vm.item(rowId)?.unit == LoadUnit.PLATES }

        val rows = runBlocking { training.exercises().first() }.filter { it.name == "Lat pulldown" }
        assertEquals(1, rows.size)
        assertEquals(PlateCount.HALF, rows.single().plateCount)
        assertEquals("the card stays, once", listOf(rowId), vm.state.value.items.map { it.exercise.id })
        assertEquals(PlateCount.HALF, vm.item(rowId)!!.plateCount)
        assertNull(vm.item(rowId)!!.editor.weightKg)
    }

    @Test
    fun plateOffered_isFalseForMainLifts_variations_andAssisted() {
        val exercises = runBlocking { training.exercises().first() }
        val bench = exercises.first { it.name == "Bench" }.id
        val variation = exercises.first { it.category == Category.VARIATION }.id
        val custom = runBlocking { training.addExercise(Exercise(0, "My board press", Category.VARIATION, MainLift.BENCH, emptyList(), isBuiltIn = false)) }
        val assisted = row("Assisted dip")
        val lat = row("Lat pulldown")
        listOf(bench, variation, custom, assisted, lat).forEachIndexed { i, id -> plan(id, i) }
        // A stored setting on one that is not offered counts as off.
        setting(bench, PlateCount.WHOLE)
        val vm = viewModel()
        await("items") { vm.state.value.items.size == 5 }

        for (id in listOf(bench, variation, custom, assisted)) {
            val item = vm.item(id)!!
            assertFalse("${item.exercise.name} is never offered", item.plateOffered)
            assertEquals(LoadUnit.KG, item.unit)
        }
        assertTrue(vm.item(lat)!!.plateOffered)
    }

    @Test
    fun topSet_isMarkedAmongThisUnitsSetsOnly() {
        val id = row("Lat pulldown")
        plan(id)
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("item") { vm.item(id) != null }
        vm.log(id, set(id, 60.0))
        vm.log(id, set(id, 62.5))
        vm.turn(id, PlateCount.WHOLE)
        vm.log(id, set(id, 7.0))
        vm.log(id, set(id, 8.0))
        assertNull(vm.state.value.pending)

        val item = vm.item(id)!!
        assertEquals(LoadUnit.PLATES, item.unit)
        // The plate 8 is the top set: 62.5 kg is never "heavier" than plate 8, nor plate 8 than 60 kg.
        assertEquals(3, com.powertracker.ui.today.topSetIndex(item.logged, item.unit))
        assertEquals(1, com.powertracker.ui.today.topSetIndex(item.logged, LoadUnit.KG))
        assertNotNull(item.lastWeightKg)
        assertEquals(8.0, item.lastWeightKg!!, 0.0)
        assertEquals(SetEditor(8.0, 8, 8.0, LoadUnit.PLATES), item.editor)
    }

    /** Lets the view model work for a moment: long enough for a save to land, had one been made. */
    private fun settled() {
        val deadline = System.currentTimeMillis() + 300
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    @Test
    fun aSetMadeInTheOtherUnit_isNotSavedOrRelabelled() {
        val id = row("Lat pulldown")
        plan(id)
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("item") { vm.item(id) != null }
        // The number was entered in the kg field; the setting changed before Log set landed.
        val typedInKg = set(id, 40.0)
        vm.turn(id, PlateCount.WHOLE)

        vm.logSet(vm.item(id)!!.planned?.id, id, typedInKg)
        settled()
        assertTrue("nothing stored", todaysSets().isEmpty())
        assertNull(vm.state.value.pending)
        assertNull("the PLATE field stays empty", vm.item(id)!!.editor.weightKg)

        // The other way: a plate number made before plates were turned off.
        vm.log(id, set(id, 7.0))
        assertEquals(1, todaysSets().size)
        vm.turn(id, PlateCount.OFF)
        vm.logSet(vm.item(id)!!.planned?.id, id, set(id, 8.0, unit = LoadUnit.PLATES))
        settled()
        assertEquals("only the plate 7 is stored", listOf(7.0 to LoadUnit.PLATES), todaysSets().map { it.weightKg to it.unit })
    }

    @Test
    fun aConfirmedSet_keepsTheUnitItWasCheckedIn() {
        val id = row("Lat pulldown")
        plan(id)
        session(7, set(id, 3.0, unit = LoadUnit.PLATES))
        setting(id, PlateCount.WHOLE)
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("item") { vm.item(id) != null }
        vm.log(id, set(id, 5.0))
        val pending = vm.state.value.pending!!
        assertEquals(LoadUnit.PLATES, pending.set.unit)

        vm.logSet(pending.planItemId, pending.exerciseId, pending.set, confirmed = true)
        await("saved") { todaysSets().isNotEmpty() }
        assertEquals(listOf(5.0 to LoadUnit.PLATES), todaysSets().map { it.weightKg to it.unit })
    }

    @Test
    fun turningPlatesOffOnTheLastPlateExercise_storesAKgTargetThatDay_besideThePlateOne() {
        val id = row("Lat pulldown")
        plan(id)
        session(28, set(id, 55.0))
        session(21, set(id, 57.5))
        session(14, set(id, 7.0, unit = LoadUnit.PLATES))
        session(7, set(id, 7.0, unit = LoadUnit.PLATES))
        setting(id, PlateCount.WHOLE)
        val vm = viewModel()
        await("plate target") { vm.item(id)?.suggestion?.unit() == LoadUnit.PLATES }

        val passes = vm.coachPasses.value
        vm.turn(id, PlateCount.OFF)
        await("the coach asked again") { vm.coachPasses.value > passes }
        await("kg target") { vm.item(id)?.suggestion?.unit() == LoadUnit.KG }

        val stored = targets(id)
        assertEquals(listOf(LoadUnit.PLATES, LoadUnit.KG), stored.map { it.unit() })
        assertTrue(stored.all { it.createdAt == today })
        assertEquals(57.5, (vm.item(id)!!.suggestion!!.payload as AdvicePayload.Target).weightKg, 2.5)
    }

    @Test
    fun anEditorInTheOtherUnit_neverShows() {
        val lat = row("Lat pulldown")
        val legPress = row("Leg press")
        plan(lat, 0)
        plan(legPress, 1)
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("items") { vm.state.value.items.size == 2 }

        // A kg edit that lands after the switch to plates (a field committing as it leaves the screen).
        vm.turn(lat, PlateCount.WHOLE)
        vm.edit(lat, SetEditor(40.0, 8, 8.0, LoadUnit.KG))
        settled()
        assertNull("never in the PLATE field", vm.item(lat)!!.editor.weightKg)
        assertEquals(LoadUnit.PLATES, vm.item(lat)!!.editor.unit)
        // A plate editor under a kg exercise.
        vm.edit(legPress, SetEditor(7.0, 8, 8.0, LoadUnit.PLATES))
        settled()
        assertNull("never in the kg field", vm.item(legPress)!!.editor.weightKg)
        assertEquals(LoadUnit.KG, vm.item(legPress)!!.editor.unit)
        // The same unit shows, as before.
        vm.edit(lat, SetEditor(7.0, 8, 8.0, LoadUnit.PLATES))
        await("plate editor") { vm.item(lat)?.editor?.weightKg == 7.0 }
    }

    @Test
    fun plateSaving_isTrueUntilTheSettingShows_andAFailedSaveSaysSo() {
        val id = row("Lat pulldown")
        plan(id)
        val vm = viewModel()
        await("item") { vm.item(id) != null }
        val failures = mutableListOf<com.powertracker.ui.today.TodayEvent>()
        collectors.launch { vm.events.collect { if (it == com.powertracker.ui.today.TodayEvent.SaveFailed) failures += it } }

        vm.setPlateCount(id, PlateCount.WHOLE)
        await("stored") { vm.item(id)?.plateCount == PlateCount.WHOLE }
        await("settled") { !vm.state.value.plateSaving }
        assertEquals(PlateCount.WHOLE, vm.item(id)!!.plateCount)

        // A save that fails (an id with no row and no catalogue entry): said, settled, nothing changed.
        vm.setPlateCount(-100_000, PlateCount.HALF)
        await("failure said") { failures.isNotEmpty() }
        await("settled again") { !vm.state.value.plateSaving }
        assertEquals(PlateCount.WHOLE, vm.item(id)!!.plateCount)

        // Two taps before the first is stored: the last one wins.
        vm.setPlateCount(id, PlateCount.HALF)
        vm.setPlateCount(id, PlateCount.OFF)
        await("settled once more") { !vm.state.value.plateSaving }
        settled()
        assertEquals(PlateCount.OFF, vm.item(id)!!.plateCount)
        assertEquals(PlateCount.OFF, runBlocking { training.exercises().first() }.first { it.id == id }.plateCount)
    }

    private fun Advice.plate() = (payload as AdvicePayload.Target).weightKg

    @Test
    fun aPlateTargetOffTheCurrentGrid_isNotUsed_andAFreshOneIsStored() {
        val id = row("Lat pulldown")
        plan(id)
        session(14, set(id, 7.0, reps = 11, unit = LoadUnit.PLATES))
        session(7, set(id, 7.0, reps = 12, unit = LoadUnit.PLATES))
        setting(id, PlateCount.HALF)
        val vm = viewModel()
        await("half-step target") { vm.item(id)?.suggestion != null }
        val half = vm.item(id)!!.suggestion!!
        assertEquals("12 reps at plate 7 earns half a step", 7.5, half.plate(), 0.0)

        // Whole plates the same day: plate 7½ is off the grid, so it is neither shown nor applied, and the coach stores
        // a whole-plate one beside it (one step up from plate 7 at 12 reps: plate 8).
        val passes = vm.coachPasses.value
        vm.turn(id, PlateCount.WHOLE)
        await("the coach asked again") { vm.coachPasses.value > passes }
        await("whole-plate target") { vm.item(id)?.suggestion?.let { it.id != half.id } == true }
        val whole = vm.item(id)!!.suggestion!!
        assertEquals(8.0, whole.plate(), 0.0)
        assertEquals(LoadUnit.PLATES, whole.unit())
        assertEquals("the half-step target is kept as it was", listOf(half, whole).map { it.id to it.plate() }, targets(id).map { it.id to it.plate() })
        assertEquals(AdviceStatus.OPEN, targets(id).first { it.id == half.id }.status)

        assertEquals("before Apply, the last plate top set", 7.0, vm.item(id)!!.editor.weightKg!!, 0.0)
        vm.applyTarget(whole.id)
        await("applied") { runBlocking { advice.all() }.first { it.id == whole.id }.status == AdviceStatus.APPLIED }
        await("in the field") { vm.item(id)?.editor?.weightKg == 8.0 }
        assertEquals(LoadUnit.PLATES, vm.item(id)!!.editor.unit)
    }

    @Test
    fun aWholePlateTarget_underHalfSteps_stays_andNoDuplicateIsStored() {
        val id = row("Lat pulldown")
        plan(id)
        session(7, set(id, 7.0, reps = 9, unit = LoadUnit.PLATES))
        setting(id, PlateCount.WHOLE)
        val vm = viewModel()
        await("whole-plate target") { vm.item(id)?.suggestion != null }
        val target = vm.item(id)!!.suggestion!!
        assertEquals(7.0, target.plate(), 0.0)

        val passes = vm.coachPasses.value
        vm.turn(id, PlateCount.HALF)
        await("the coach asked again") { vm.coachPasses.value > passes }
        settled()

        assertEquals("plate 7 is on the half-step grid: it stays", target.id, vm.item(id)!!.suggestion?.id)
        assertEquals(listOf(target.id), targets(id).map { it.id })
    }

    @Test
    fun aPlateSetOutOfRange_isNeverStored() {
        val id = row("Lat pulldown")
        plan(id)
        setting(id, PlateCount.WHOLE)
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("item") { vm.item(id)?.unit == LoadUnit.PLATES }
        val planItem = vm.item(id)!!.planned?.id

        for (plate in listOf(0.0, 0.5, 51.0)) {
            vm.logSet(planItem, id, set(id, plate, unit = LoadUnit.PLATES))
            settled()
            vm.logSet(planItem, id, set(id, plate, unit = LoadUnit.PLATES), confirmed = true)
            settled()
        }
        assertTrue("nothing under plate 1 or over plate 50", todaysSets().isEmpty())
        assertNull(vm.state.value.pending)

        // Off the grid is not refused here: a logged half stands with whole plates on.
        vm.log(id, set(id, 7.5))
        assertEquals(listOf(7.5 to LoadUnit.PLATES), todaysSets().map { it.weightKg to it.unit })
    }

    /**
     * 1.17.1: half steps on, a plate 7½ target is stored (open); then whole plates, and a plate 8 target is stored beside
     * it. Returns the view model, the exercise and the two targets, with the card on whole plates showing the 8.
     */
    private fun halfThenWhole(): Triple<TodayViewModel, Long, Pair<Advice, Advice>> {
        val id = row("Lat pulldown")
        plan(id)
        session(14, set(id, 7.0, reps = 11, unit = LoadUnit.PLATES))
        session(7, set(id, 7.0, reps = 12, unit = LoadUnit.PLATES))
        setting(id, PlateCount.HALF)
        val vm = viewModel()
        await("half-step target") { vm.item(id)?.suggestion != null }
        val half = vm.item(id)!!.suggestion!!
        assertEquals(7.5, half.plate(), 0.0)
        val passes = vm.coachPasses.value
        vm.turn(id, PlateCount.WHOLE)
        await("the coach asked again") { vm.coachPasses.value > passes }
        await("whole-plate target") { vm.item(id)?.suggestion?.let { it.id != half.id } == true }
        val whole = vm.item(id)!!.suggestion!!
        assertEquals(8.0, whole.plate(), 0.0)
        assertTrue("the newer target has the higher id", whole.id > half.id)
        return Triple(vm, id, half to whole)
    }

    /** Back to half steps: both targets are on the half grid again. Waits for the coach's pass and for the view to settle. */
    private fun TodayViewModel.backToHalf(id: Long) {
        val passes = coachPasses.value
        turn(id, PlateCount.HALF)
        await("the coach asked again") { coachPasses.value > passes }
        settled()
    }

    @Test
    fun theNewestTargetDecides_anEarlierOpenOneDoesNotComeBack() {
        val (vm, id, targets) = halfThenWhole()
        val (half, whole) = targets
        vm.applyTarget(whole.id)
        await("applied") { runBlocking { advice.all() }.first { it.id == whole.id }.status == AdviceStatus.APPLIED }
        await("in the field") { vm.item(id)?.editor?.weightKg == 8.0 }

        vm.backToHalf(id)
        val item = vm.item(id)!!
        assertNull("the older open plate 7½ does not come back as the chip", item.suggestion)
        // As for any applied target: its numbers fill the field and its sets are the sets to do.
        assertEquals(8.0, item.editor.weightKg!!, 0.0)
        assertEquals(LoadUnit.PLATES, item.editor.unit)
        assertEquals("the target's sets, else the plan's", (whole.payload as AdvicePayload.Target).sets ?: 3, item.targetSets)
        assertEquals("nothing stored changes", listOf(half.id to AdviceStatus.OPEN, whole.id to AdviceStatus.APPLIED), targets(id).map { it.id to it.status })
    }

    @Test
    fun theNewestTargetDecides_whenItWasDismissed() {
        val (vm, id, targets) = halfThenWhole()
        val (half, whole) = targets
        vm.dismissTarget(whole.id)
        await("dismissed") { vm.item(id)?.suggestion == null }

        vm.backToHalf(id)
        val item = vm.item(id)!!
        assertNull("the older open plate 7½ does not come back as the chip", item.suggestion)
        // As for any dismissed target: no chip, and the field and the sets to do are as with no target at all.
        assertEquals("the last plate top set", 7.0, item.editor.weightKg!!, 0.0)
        assertEquals(3, item.targetSets)
        assertEquals(listOf(half.id to AdviceStatus.OPEN, whole.id to AdviceStatus.DISMISSED), targets(id).map { it.id to it.status })
    }

    @Test
    fun theNewestOpenTargetIsTheChip() {
        val (vm, id, targets) = halfThenWhole()
        val (half, whole) = targets
        assertEquals("on whole plates, the 8 is the chip, as before", whole.id, vm.item(id)!!.suggestion!!.id)

        // Back to half steps with nothing applied: both are open and on the grid, and the newer one is the chip.
        vm.backToHalf(id)
        assertEquals(whole.id, vm.item(id)!!.suggestion?.id)
        assertEquals(listOf(half.id, whole.id), targets(id).filter { it.status == AdviceStatus.OPEN }.map { it.id })
    }

    @Test
    fun theNewestAppliedTargetDecides_anOlderAppliedOneDoesNotFillTheField() {
        // Half steps: plate 7½ applied. Whole plates: the 7½ is off the grid, and the plate 8 stored beside it is open.
        val id = row("Lat pulldown")
        plan(id)
        session(14, set(id, 7.0, reps = 11, unit = LoadUnit.PLATES))
        session(7, set(id, 7.0, reps = 12, unit = LoadUnit.PLATES))
        setting(id, PlateCount.HALF)
        val vm = viewModel()
        await("half-step target") { vm.item(id)?.suggestion != null }
        val half = vm.item(id)!!.suggestion!!
        vm.applyTarget(half.id)
        await("applied") { vm.item(id)?.editor?.weightKg == 7.5 }
        val passes = vm.coachPasses.value
        vm.turn(id, PlateCount.WHOLE)
        await("the coach asked again") { vm.coachPasses.value > passes }
        await("whole-plate target") { vm.item(id)?.suggestion?.let { it.id != half.id } == true }
        val whole = vm.item(id)!!.suggestion!!

        // Back to half steps: the open 8 is the newest, so it is the chip, and the older applied 7½ fills nothing.
        vm.backToHalf(id)
        val item = vm.item(id)!!
        assertEquals(whole.id, item.suggestion?.id)
        assertEquals("the last plate top set, not the older applied 7½", 7.0, item.editor.weightKg!!, 0.0)
        assertEquals(3, item.targetSets)
    }

    @Test
    fun aKgExercise_newestTargetRule_changesNothing() {
        val id = latPulldownWithKgHistory()
        val vm = viewModel()
        await("kg target") { vm.item(id)?.suggestion != null }
        val kg = vm.item(id)!!.suggestion!!
        assertEquals(LoadUnit.KG, kg.unit())
        assertEquals(listOf(kg.id), targets(id).map { it.id })

        vm.applyTarget(kg.id)
        await("applied") { vm.item(id)?.suggestion == null }
        val item = vm.item(id)!!
        assertEquals((kg.payload as AdvicePayload.Target).weightKg, item.editor.weightKg!!, 0.0)
        assertEquals(LoadUnit.KG, item.editor.unit)
        assertEquals("the target's sets, else the plan's", (kg.payload as AdvicePayload.Target).sets ?: 3, item.targetSets)
    }

    @Test
    fun aPlateSetOffTheHalfPlateGrid_isNeverStored() {
        val id = row("Lat pulldown")
        plan(id)
        setting(id, PlateCount.WHOLE)
        runBlocking { training.startSession(today, null, null) }
        val vm = viewModel()
        await("item") { vm.item(id)?.unit == LoadUnit.PLATES }
        val planItem = vm.item(id)!!.planned?.id

        for (plate in listOf(7.3, 7.25)) {
            vm.logSet(planItem, id, set(id, plate, unit = LoadUnit.PLATES))
            settled()
            vm.logSet(planItem, id, set(id, plate, unit = LoadUnit.PLATES), confirmed = true)
            settled()
        }
        assertTrue("nothing off the half-plate grid", todaysSets().isEmpty())
        assertNull(vm.state.value.pending)

        // A half with whole plates on is on the half-plate grid: it is stored, as a logged half stands.
        vm.log(id, set(id, 7.5))
        assertEquals(listOf(7.5 to LoadUnit.PLATES), todaysSets().map { it.weightKg to it.unit })
    }
}
