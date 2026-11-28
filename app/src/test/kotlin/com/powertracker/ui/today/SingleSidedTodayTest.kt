package com.powertracker.ui.today

import android.content.Context
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.Backup
import com.powertracker.data.BackupResult
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.SideLog
import com.powertracker.domain.model.SplitSides
import com.powertracker.domain.unilateral.SingleSided
import com.powertracker.testing.TestDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
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

@RunWith(RobolectricTestRunner::class)
class SingleSidedTodayTest {
    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private val collectors = CoroutineScope(Dispatchers.Main)
    private var vm: TodayViewModel? = null
    private val day = LocalDate(2026, 11, 28)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        collectors.cancel()
        vm?.viewModelScope?.cancel()
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        }
    }

    @Test
    fun singleSided_rightThenLeft_savesOneSetWithWeakerSide_startsRestOnlyAfterLeft_andBackupsRoundTrip() {
        val model = TodayViewModel(training, advice, settings, today = { day }).also { vm = it }
        val events = mutableListOf<TodayEvent>()
        collectors.launch { model.events.collect { events.add(it) } }
        collectors.launch { model.state.collect {} }

        await("loaded") { !model.state.value.loading }
        model.startSession(null, null)
        await("session") { model.state.value.sessionId != null }
        val pecDeckId = model.addFromCatalogue("Pec deck")!!
        await("pec deck added") { model.state.value.items.any { it.exercise.id == pecDeckId } }

        val initialItem = model.state.value.items.first { it.exercise.id == pecDeckId }
        assertTrue(initialItem.singleSidedOffered)
        assertFalse(initialItem.singleSided)

        // Toggle "One side at a time" on
        model.setSingleSided(pecDeckId, true)
        await("singleSided enabled") { model.state.value.items.any { it.exercise.name == "Pec deck" && it.singleSided } }
        val resolvedId = model.state.value.items.first { it.exercise.name == "Pec deck" }.exercise.id

        // Log Right side: no set saved yet, NO rest timer event, pendingRight populated
        val right = SideLog(weightKg = 40.0, reps = 12, rpe = 8.0, failed = false)
        model.logSide(null, resolvedId, right, LoadUnit.KG)
        await("pendingRight") { model.state.value.items.first { it.exercise.id == resolvedId }.pendingRight != null }
        assertEquals(0, model.state.value.items.first { it.exercise.id == resolvedId }.sets.size)
        assertEquals(right, model.state.value.items.first { it.exercise.id == resolvedId }.pendingRight)
        assertTrue("Rest timer must NOT start after Right side alone", events.none { it is TodayEvent.SetLogged })

        // Log Left side (weaker: 10 reps @ 8.5): saves 1 set with weaker side, clears pendingRight, starts rest timer
        val left = SideLog(weightKg = 40.0, reps = 10, rpe = 8.5, failed = false)
        model.logSide(null, resolvedId, left, LoadUnit.KG)
        await("set saved") { model.state.value.items.first { it.exercise.id == resolvedId }.sets.size == 1 }
        await("rest timer event") { events.any { it is TodayEvent.SetLogged } }
        val itemAfter = model.state.value.items.first { it.exercise.id == resolvedId }
        assertNull(itemAfter.pendingRight)
        assertEquals(1, itemAfter.sets.size)
        val saved = itemAfter.sets.single().set
        assertEquals(40.0, saved.weightKg, 0.0)
        assertEquals(10, saved.reps)
        assertEquals(8.5, saved.rpe!!, 0.0)
        assertEquals(SplitSides(right, left), saved.splitSides)
        assertEquals("R 12 · L 10", SingleSided.summaryText(saved.splitSides!!, saved.unit))

        // Backup v8 round-trip preserves both Exercise.singleSided and Set.splitSides
        runBlocking {
            val backup = BackupService(db, settings)
            val exported = Backup.encode(backup.export(1_793_000_000_000L))
            val decoded = Backup.decode(exported)
            assertTrue(decoded is BackupResult.Ok)
            assertEquals(BackupResult.TooNew(8), Backup.decode(exported, supportedVersion = 7))
            backup.restore((decoded as BackupResult.Ok).file)
            val restoredExercise = db.exercises().all().first { it.id == resolvedId }
            assertTrue(restoredExercise.singleSided)
            val restoredSet = db.sets().all().single()
            assertNotNull(restoredSet.splitSides)
            assertEquals(SplitSides(right, left), restoredSet.splitSides)
        }
    }
}
