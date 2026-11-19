package com.powertracker.ui.plan

import android.content.Context
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.PlanSnapshot
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.plan.builder.PlanBuilderViewModel
import com.powertracker.ui.plan.templates.TemplatesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Undo after a whole-plan replace (1.14.1): a template's or the builder's save, then Plan's snackbar Undo. */
@RunWith(RobolectricTestRunner::class)
class PlanUndoTest {
    private val today = LocalDate(2026, 11, 18) // a Wednesday

    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository
    private val viewModels = mutableListOf<ViewModel>()
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val messages = mutableListOf<String>()

    @Before
    fun setUp() {
        training = TrainingRepository(db)
        settings = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())
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

    private fun plan(mode: TrainingMode) = runBlocking { training.plan(mode).first() }
    private fun stored() = runBlocking { settings.settings.first() }
    private fun skips() = runBlocking { training.skips().first() }
    private fun rowId(name: String) = runBlocking { training.exercises().first() }.first { it.name == name }.id

    private fun planVm(undoReplace: (suspend (PlanSnapshot) -> Boolean)? = null): PlanViewModel {
        val vm = if (undoReplace == null) PlanViewModel(training, settings) { today }
        else PlanViewModel(training, settings, undoReplace = undoReplace) { today }
        viewModels += vm
        collectors.launch { vm.messages.collect { messages += it } }
        return vm
    }

    /** Uses [id]'s template on [mode]'s side with its suggested days, replacing any plan, and returns the save's Undo. */
    private fun useTemplate(mode: TrainingMode, id: String): PlanSnapshot? {
        val vm = TemplatesViewModel(mode, training, settings)
        viewModels += vm
        await("loaded") { !vm.state.value.loading }
        vm.choose(id)
        vm.use()
        if (vm.state.value.confirming) vm.confirmReplace()
        await("saved") { vm.state.value.saved }
        return vm.state.value.undo
    }

    private data class Old(val plan: List<PlanItem>, val skip: Skip, val days: AppSettings)

    /** A powerlifting plan of three items, numbers of their own, and a skip of the second. */
    private fun oldPowerliftingPlan(): Old {
        val squat = rowId("Squat")
        val bench = rowId("Bench")
        runBlocking {
            training.savePlanItem(PlanItem(0, MONDAY, squat, 4, 3, 7.5, 0))
            val skipped = training.savePlanItem(PlanItem(0, MONDAY, bench, 5, 5, 8.0, 1))
            training.savePlanItem(PlanItem(0, FRIDAY, squat, 2, 2, 9.0, 0))
            training.skip(Skip(0, today.minus(2, DateTimeUnit.DAY), POWERLIFTING, bench, skipped, SkipReason.BUSY)) { _, _ -> emptyList() }
        }
        return Old(plan(POWERLIFTING), skips().single(), stored())
    }

    private fun undo(vm: PlanViewModel, snapshot: PlanSnapshot) {
        vm.undo(snapshot)
        settle()
    }

    @Test
    fun undo_putsBackTheIds_order_numbers_skipLinks_andDays() {
        val old = oldPowerliftingPlan()
        val bodybuilding = runBlocking {
            training.savePlanItem(PlanItem(0, TUESDAY, rowId("Curl"), 3, 8, 8.0, 0, BODYBUILDING, 12))
            plan(BODYBUILDING)
        }

        val snapshot = useTemplate(POWERLIFTING, "pl-4")!!
        assertNotEquals(old.plan, plan(POWERLIFTING))
        assertNull("the replace cleared the skip's link", skips().single().planItemId)
        assertEquals(4, stored().trainingDays.size)

        undo(planVm(), snapshot)

        assertEquals("the old items, ids and all", old.plan, plan(POWERLIFTING))
        assertEquals("the skip points at its item again", old.skip, skips().single())
        assertEquals("both sides' days as before", old.days, stored())
        assertEquals("the other side is untouched", bodybuilding, plan(BODYBUILDING))
        assertTrue(messages.isEmpty())
    }

    @Test
    fun noUndo_whenTheSideHadNoPlan() {
        assertNull(useTemplate(BODYBUILDING, "bb-3"))
        assertTrue(plan(BODYBUILDING).isNotEmpty())
    }

    @Test
    fun aStaleUndo_doesNothing() {
        val old = oldPowerliftingPlan()
        val first = useTemplate(POWERLIFTING, "pl-3")!!
        val firstPlan = plan(POWERLIFTING)
        // The same template again: the same number of items, on new ids.
        val second = useTemplate(POWERLIFTING, "pl-3")!!
        val secondPlan = plan(POWERLIFTING)
        assertEquals(firstPlan.size, secondPlan.size)
        val vm = planVm()

        undo(vm, first)
        assertEquals("the older Undo can't put the older plan over the newer one", secondPlan, plan(POWERLIFTING))

        undo(vm, second)
        assertEquals("the latest Undo works", firstPlan, plan(POWERLIFTING))

        undo(vm, second)
        undo(vm, first)
        assertEquals("Undo after Undo does nothing", firstPlan, plan(POWERLIFTING))
        assertNotEquals(old.plan, plan(POWERLIFTING))
        assertTrue(messages.isEmpty())
    }

    @Test
    fun anUndo_neverAppliesToTheOtherSide() {
        oldPowerliftingPlan()
        val snapshot = useTemplate(POWERLIFTING, "pl-3")!!
        val powerlifting = plan(POWERLIFTING)
        runBlocking { assertFalse(training.restorePlan(BODYBUILDING, snapshot)) }
        assertTrue(plan(BODYBUILDING).isEmpty())
        assertEquals(powerlifting, plan(POWERLIFTING))
    }

    @Test
    fun undo_afterASessionWasLogged_keepsTheSessionItsSets_andTheNewSkip() {
        val old = oldPowerliftingPlan()
        val snapshot = useTemplate(POWERLIFTING, "pl-3")!!
        val newItem = plan(POWERLIFTING).first()
        val (sessionId, newSkip) = runBlocking {
            val sessionId = training.startSession(today, 80.0, 4, POWERLIFTING)
            training.logSet(sessionId, LoggedSet(newItem.exerciseId, 100.0, 5, 8.0, false, null, true))
            val other = plan(POWERLIFTING).last()
            training.skip(Skip(0, today, POWERLIFTING, other.exerciseId, other.id, null)) { _, _ -> emptyList() }
            sessionId to other
        }
        val history = runBlocking { training.history().first() }

        undo(planVm(), snapshot)

        assertEquals(old.plan, plan(POWERLIFTING))
        assertEquals("the session and its sets stay", history.sessions, runBlocking { training.history().first() }.sessions)
        assertEquals(1, runBlocking { training.history().first() }.sessions.single { it.id == sessionId }.sets.size)
        val after = skips()
        assertEquals(old.skip, after.single { it.id == old.skip.id })
        val kept = after.single { it.id != old.skip.id }
        assertEquals("the skip logged after the save stays", newSkip.exerciseId, kept.exerciseId)
        assertEquals(today, kept.date)
    }

    @Test
    fun aFailedUndo_keepsTheNewPlanAndDays_andSaysSo() {
        oldPowerliftingPlan()
        val snapshot = useTemplate(POWERLIFTING, "pl-4")!!
        val newPlan = plan(POWERLIFTING)
        val newSettings = stored()

        // The days write fails, last inside the transaction: the restored items roll back with it.
        val vm = planVm { s -> undoPlanReplace(training, settings, s, setDays = { _, _ -> error("disk full") }) }
        undo(vm, snapshot)
        await("message") { messages.isNotEmpty() }
        assertEquals(listOf(UNDO_FAILED), messages)
        assertEquals(newPlan, plan(POWERLIFTING))
        assertEquals(newSettings, stored())

        // The days are stored, then the transaction fails: they are put back.
        messages.clear()
        val late = planVm { s ->
            undoPlanReplace(training, settings, s, restorePlan = { mode, snap ->
                training.restorePlan(mode, snap)
            }, setDays = { side, days ->
                com.powertracker.ui.plan.builder.setSideTrainingDays(settings, side, days)
                error("commit failed")
            })
        }
        undo(late, snapshot)
        await("message") { messages.isNotEmpty() }
        assertEquals(listOf(UNDO_FAILED), messages)
        assertEquals(newPlan, plan(POWERLIFTING))
        assertEquals(newSettings, stored())

        // And the Undo still works afterwards.
        undo(planVm(), snapshot)
        assertNotEquals(newPlan, plan(POWERLIFTING))
    }

    // The builder's save

    private fun build(): PlanSnapshot? {
        runBlocking { settings.update { it.copy(trainingMode = BODYBUILDING) } }
        val vm = PlanBuilderViewModel(training, settings) { today }
        viewModels += vm
        await("loaded") { !vm.state.value.loading }
        vm.next(); vm.next(); vm.next()
        assertNotNull(vm.state.value.draft)
        vm.use()
        if (vm.state.value.confirming) vm.confirmReplace()
        await("saved") { vm.state.value.saved }
        return vm.state.value.undo
    }

    @Test
    fun builder_offersUndoOnlyOverAPlan_andItPutsThePlanBack() {
        assertNull("an empty side gets no Undo", build())
        val built = plan(BODYBUILDING)

        val snapshot = build()!!
        assertNotEquals(built, plan(BODYBUILDING))
        undo(planVm(), snapshot)
        assertEquals(built, plan(BODYBUILDING))
        assertEquals(setOf(TUESDAY, THURSDAY, SATURDAY), stored().bodybuildingTrainingDays)
    }
}
