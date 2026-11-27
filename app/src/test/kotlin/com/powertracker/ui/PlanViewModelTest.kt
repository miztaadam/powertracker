package com.powertracker.ui

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.Kind
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.plan.SupersetHalf
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.library.PickerEntry
import com.powertracker.ui.library.SAVE_FAILED
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.plan.PlanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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

/**
 * Plan's writes when the database refuses them (a full disk, a broken database): none may escape the view model's
 * coroutine, which on a phone crashes the app. Each says "Couldn't save — try again." and stores nothing.
 */
@RunWith(RobolectricTestRunner::class)
class PlanViewModelTest {
    private val today: LocalDate = LocalDate(2026, 11, 18)

    /** Closes the database after `@After` has stopped the view models, once no query is running. */
    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository
    private val collectors = CoroutineScope(Dispatchers.Main)
    private val messages = mutableListOf<String>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings() } }
    }

    @After
    fun tearDown() {
        collectors.cancel()
    }

    private fun viewModel() = PlanViewModel(training, settings) { today }.also { vm ->
        collectors.launch { vm.state.collect {} }
        collectors.launch { vm.messages.collect { messages += it } }
    }

    /** View-model work resumes on the main looper, which Robolectric only runs when asked. */
    private fun idleUntil(what: () -> String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out; ${what()}" }
        }
    }

    private fun PlanViewModel.await(condition: (PlanUiState) -> Boolean): PlanUiState {
        idleUntil({ "state was ${state.value}" }) { condition(state.value) }
        return state.value
    }

    private fun awaitMessages(count: Int) = idleUntil({ "messages $messages" }) { messages.size >= count }

    /** Makes the database refuse [statement]s on [table], as a full disk would: the throw the real path meets. */
    private fun fail(table: String, statement: String, `when`: String = "1") = db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_${table}_${statement.lowercase()} BEFORE $statement ON $table WHEN ${`when`} BEGIN SELECT RAISE(ABORT, 'disk full'); END",
    )

    private fun stored(): List<PlanItem> = runBlocking { training.plan().first() }.sortedBy { it.id }

    private fun id(name: String): Long = runBlocking { training.exercises().first() }.first { it.name == name }.id

    private fun entry(name: String) = PickerEntry(name, emptyList(), Kind.COMPOUND, library = null, exerciseId = id(name))

    private fun planned(name: String, order: Int): PlanItem = runBlocking {
        val item = PlanItem(0, DayOfWeek.MONDAY, id(name), sets = 3, reps = 5, targetRpe = 8.0, order = order)
        item.copy(id = training.savePlanItem(item))
    }

    @Test
    fun add_whenTheWriteFails_saysSo_storesNothing() {
        fail("plan_item", "INSERT")
        val vm = viewModel()
        vm.await { it.exercises.isNotEmpty() }
        var added = 0

        vm.add(DayOfWeek.MONDAY, entry("Squat"), sets = 5, reps = 3, rpe = 7.5, repsHigh = null, onAdded = { added++ })
        awaitMessages(1)

        assertEquals(listOf(SAVE_FAILED), messages)
        assertEquals(0, added)
        assertTrue(stored().isEmpty())
    }

    /** The add form says a failed Add itself, over the form: Plan's snackbar is not told as well. */
    @Test
    fun add_whenTheWriteFails_withTheFormsOwnSaying_hearsTheFailure_andPlanSaysNothing() {
        fail("plan_item", "INSERT")
        val vm = viewModel()
        vm.await { it.exercises.isNotEmpty() }
        var added = 0
        var failed = 0

        vm.add(DayOfWeek.MONDAY, entry("Squat"), sets = 5, reps = 3, rpe = 7.5, repsHigh = null, onAdded = { added++ }, onFailed = { failed++ })
        idleUntil({ "failed $failed" }) { failed == 1 }

        assertEquals(emptyList<String>(), messages)
        assertEquals(0, added)
        assertTrue(stored().isEmpty())
    }

    @Test
    fun save_whenTheWriteFails_saysSo_rowUnchanged() {
        val item = planned("Squat", 0)
        fail("plan_item", "INSERT")
        val vm = viewModel()
        vm.await { s -> s.sections.any { it.rows.isNotEmpty() } }

        vm.save(item, exercise = null, sets = 5, reps = 3, rpe = 9.0)
        awaitMessages(1)

        assertEquals(listOf(SAVE_FAILED), messages)
        assertEquals(listOf(item), stored())
    }

    @Test
    fun delete_whenTheWriteFails_saysSo_rowKept() {
        val item = planned("Squat", 0)
        fail("plan_item", "DELETE")
        val vm = viewModel()
        vm.await { s -> s.sections.any { it.rows.isNotEmpty() } }

        vm.delete(item)
        awaitMessages(1)

        assertEquals(listOf(SAVE_FAILED), messages)
        assertEquals(listOf(item), stored())
    }

    @Test
    fun move_whenTheSecondWriteFails_saysSo_andNeitherRowMoved() {
        val squat = planned("Squat", 0)
        val bench = planned("Bench", 1)
        // Only the second of move's two writes (the neighbour's new place) is refused: half a move must not be kept.
        fail("plan_item", "INSERT", "NEW.id = ${bench.id}")
        val vm = viewModel()
        vm.await { s -> s.sections.any { it.rows.size == 2 } }

        vm.move(squat, +1)
        awaitMessages(1)

        assertEquals(listOf(SAVE_FAILED), messages)
        assertEquals(listOf(squat, bench), stored())
        assertEquals(listOf(0, 1), stored().map { it.order })
    }

    // --- Supersets (1.24) ---

    private fun rows(vm: PlanViewModel) = vm.state.value.sections.flatMap { it.rows }

    @Test
    fun superset_linksToTheOneAbove_namesEachHalf_andIsTakenOffAgain() {
        val squat = planned("Squat", 0)
        val bench = planned("Bench", 1)
        val row = planned("Row", 2)
        val vm = viewModel()
        vm.await { s -> s.sections.any { it.rows.size == 3 } }
        // The first has nothing above it; the other two can be linked.
        assertEquals(listOf(false, true, true), rows(vm).map { it.canLink })
        assertEquals(listOf(null, null, null), rows(vm).map { it.superset })

        vm.setSuperset(bench, linked = true)
        vm.await { s -> s.sections.any { d -> d.rows.any { it.superset != null } } }

        assertEquals(listOf(SupersetHalf.FIRST, SupersetHalf.SECOND, null), rows(vm).map { it.superset })
        assertEquals(listOf("Bench", "Squat", null), rows(vm).map { it.partnerName })
        // Pairs only: neither half can be linked again, nor the row to the pair above it.
        assertEquals(listOf(false, false, false), rows(vm).map { it.canLink })
        assertEquals(listOf(false, true, false), stored().map { it.supersetWithAbove })

        vm.setSuperset(stored().first { it.id == bench.id }, linked = false)
        vm.await { s -> s.sections.any { d -> d.rows.size == 3 && d.rows.none { it.superset != null } } }
        assertEquals(listOf(false, false, false), stored().map { it.supersetWithAbove })
        assertEquals(listOf(squat.id, bench.id, row.id), stored().map { it.id })
    }

    @Test
    fun superset_aLinkThatCannotBeMade_changesNothing() {
        val squat = planned("Squat", 0)
        planned("Bench", 1)
        val vm = viewModel()
        vm.await { s -> s.sections.any { it.rows.size == 2 } }

        // Nothing above the first.
        vm.setSuperset(squat, linked = true)
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(100)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(false, false), stored().map { it.supersetWithAbove })
        assertEquals(emptyList<String>(), messages)
    }

    @Test
    fun superset_movingAHalfAway_unlinksThePair() {
        val squat = planned("Squat", 0)
        val bench = planned("Bench", 1)
        planned("Row", 2)
        val vm = viewModel()
        vm.await { s -> s.sections.any { it.rows.size == 3 } }
        vm.setSuperset(bench, linked = true)
        vm.await { s -> s.sections.any { d -> d.rows.any { it.superset != null } } }

        // The bench goes below the row: the squat is no longer the one above it.
        vm.move(stored().first { it.id == bench.id }, +1)
        vm.await { s -> s.sections.any { d -> d.rows.map { it.exerciseName } == listOf("Squat", "Row", "Bench") } }

        assertEquals(listOf(null, null, null), rows(vm).map { it.superset })
        assertEquals(listOf(false, false, false), stored().map { it.supersetWithAbove })
        assertEquals(squat.id, stored().first().id)
    }

    @Test
    fun setBaseline_whenTheWriteFails_saysSo_storesNothing() {
        fail("baseline", "INSERT")
        val vm = viewModel()
        vm.await { it.mainExercises.isNotEmpty() }
        val squat = id("Squat")

        vm.setBaseline(squat, 180.0)
        awaitMessages(1)

        assertEquals(listOf(SAVE_FAILED), messages)
        assertTrue(runBlocking { training.history().first() }.baselines.none { it.exerciseId == squat })
    }

    @Test
    fun add_aThrowAfterTheStore_isNotReportedAsAFailedSave() {
        // The throw below escapes the view model, as it should: it is not a failed save. The coroutine test library
        // keeps such a throw for the next test that uses it; this one collects it itself (it starts collecting here).
        runTest {}
        val vm = viewModel()
        vm.await { it.exercises.isNotEmpty() }
        var added = 0
        var failed = 0

        // The item is stored; what runs after it (closing the form, the tick) is not part of the write.
        vm.add(DayOfWeek.MONDAY, entry("Squat"), sets = 5, reps = 3, rpe = 7.5, repsHigh = null, onAdded = {
            added++
            throw IllegalStateException("after the store")
        }, onFailed = { failed++ })
        idleUntil({ "added $added" }) { added == 1 }
        repeat(20) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }

        assertEquals(1, stored().size)
        assertEquals("no \"Couldn't save\" for a stored item", emptyList<String>(), messages)
        assertEquals("the guard is not released into a second add", 0, failed)
        val escaped = runCatching { runTest {} }.exceptionOrNull()
        assertTrue("the throw escaped: $escaped", escaped != null && escaped.suppressed.any { it.message == "after the store" })
    }

    @Test
    fun aWriteThatWorks_saysNothing() {
        val vm = viewModel()
        vm.await { it.exercises.isNotEmpty() }
        var added = 0

        vm.add(DayOfWeek.MONDAY, entry("Squat"), sets = 5, reps = 3, rpe = 7.5, repsHigh = null, onAdded = { added++ })
        idleUntil({ "added $added" }) { added == 1 }

        assertEquals(1, stored().size)
        assertEquals(emptyList<String>(), messages)
    }
}
