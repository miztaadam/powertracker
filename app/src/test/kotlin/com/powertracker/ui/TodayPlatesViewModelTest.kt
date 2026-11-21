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

    /** Logs [set] and waits until it is on screen (or held as a typo). */
    private fun TodayViewModel.log(id: Long, set: LoggedSet) {
        val before = item(id)?.sets?.size ?: 0
        logSet(item(id)?.planned?.id, id, set)
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
        // The screen builds the set from the editor; the view model gives it the exercise's unit.
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
}
