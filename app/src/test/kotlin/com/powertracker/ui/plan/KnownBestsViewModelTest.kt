package com.powertracker.ui.plan

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Known bests (1.16.1): each row's best e1RM, the higher of the typed best and the best logged ([ProgressMath.knownBest]),
 * and the strict press row, which the Progress switch being on always brings.
 */
@RunWith(RobolectricTestRunner::class)
class KnownBestsViewModelTest {
    private val today: LocalDate = LocalDate(2026, 11, 18)

    @get:Rule
    val database = TestDatabase()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository
    private val collectors = CoroutineScope(Dispatchers.Main)

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
    }

    private fun PlanViewModel.await(condition: (PlanUiState) -> Boolean): PlanUiState {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition(state.value)) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Timed out; state was ${state.value}" }
        }
        return state.value
    }

    private fun id(name: String): Long = runBlocking { training.exercises().first() }.first { it.name == name }.id

    private fun log(name: String, kg: Double, mode: TrainingMode = TrainingMode.POWERLIFTING) = runBlocking {
        val session = training.startSession(today, null, null, mode)
        training.logSet(session, LoggedSet(id(name), kg, 1, 10.0, false, null, isTopSet = false))
    }

    private fun pressRows(state: PlanUiState) = state.mainExercises.filter { it.category == Category.STRICT_PRESS }

    @Test
    fun noBest_noFigure() {
        val state = viewModel().await { it.mainExercises.isNotEmpty() }
        assertEquals(listOf("Squat", "Bench", "Deadlift"), state.mainExercises.map { it.name })
        assertTrue(state.bests.isEmpty())
    }

    @Test
    fun setBest_givesItsFigure() {
        val vm = viewModel()
        vm.await { it.mainExercises.isNotEmpty() }
        val squat = id("Squat")

        vm.setBaseline(squat, 182.5)

        assertEquals(mapOf(squat to 182.5), vm.await { it.bests.isNotEmpty() }.bests)
    }

    @Test
    fun aHeavierLoggedSet_raisesIt() {
        val vm = viewModel()
        vm.await { it.mainExercises.isNotEmpty() }
        val squat = id("Squat")
        vm.setBaseline(squat, 180.0)
        vm.await { it.bests[squat] == 180.0 }

        log("Squat", 190.0)

        assertEquals(190.0, vm.await { it.bests[squat] == 190.0 }.bests[squat])
    }

    @Test
    fun aLighterLoggedSet_leavesTheTypedBest_andTheBodybuildingSideCountsForNothing() {
        log("Squat", 150.0)
        log("Squat", 230.0, TrainingMode.BODYBUILDING)
        val vm = viewModel()
        vm.await { it.mainExercises.isNotEmpty() }
        val squat = id("Squat")
        assertEquals(150.0, vm.await { it.bests[squat] != null }.bests[squat])

        vm.setBaseline(squat, 200.0)

        assertEquals(200.0, vm.await { it.bests[squat] == 200.0 }.bests[squat])
    }

    @Test
    fun bestsAreToTheHalfKilo_asOnProgress() {
        val vm = viewModel()
        vm.await { it.mainExercises.isNotEmpty() }
        val bench = id("Bench")
        runBlocking {
            val session = training.startSession(today, null, null)
            // 100 × 3 @ 8: an e1RM that is no round half kilo.
            training.logSet(session, LoggedSet(bench, 100.0, 3, 8.0, false, null, isTopSet = false))
        }
        val best = vm.await { it.bests[bench] != null }.bests.getValue(bench)
        assertEquals(best, (best * 2).toLong() / 2.0, 0.0)
    }

    // ---- The strict press row ----

    @Test
    fun pressOn_withNoRowYet_showsTheCataloguesStrictPress_storedNowhere() {
        runBlocking { settings.update { it.copy(showStrictPress = true) } }
        val state = viewModel().await { it.mainExercises.isNotEmpty() }

        assertEquals(listOf("Squat", "Bench", "Deadlift", "Strict press"), state.mainExercises.map { it.name })
        assertEquals(0L, pressRows(state).single().id)
        assertTrue(runBlocking { training.exercises().first() }.none { it.category == Category.STRICT_PRESS })
    }

    @Test
    fun pressOff_withNoRow_hasNoPressRow_asBefore() {
        val state = viewModel().await { it.mainExercises.isNotEmpty() }
        assertTrue(pressRows(state).isEmpty())
    }

    @Test
    fun setBest_onTheCataloguesStrictPress_makesItsRow_andStoresTheBest() {
        runBlocking { settings.update { it.copy(showStrictPress = true) } }
        val vm = viewModel()
        vm.await { it.mainExercises.isNotEmpty() }

        vm.setBaseline(0L, 70.0)

        val state = vm.await { s -> pressRows(s).singleOrNull()?.id?.let { it != 0L } == true && s.bests.isNotEmpty() }
        val press = pressRows(state).single()
        assertEquals("Strict press", press.name)
        assertEquals(mapOf(press.id to 70.0), state.bests)
        assertEquals(listOf(press.id to 70.0), runBlocking { training.history().first() }.baselines.map { it.exerciseId to it.e1rmKg })
    }

    @Test
    fun pressOff_keepsAnExistingRow_andItsBest() {
        runBlocking { settings.update { it.copy(showStrictPress = true) } }
        val vm = viewModel()
        vm.await { it.mainExercises.isNotEmpty() }
        vm.setBaseline(0L, 70.0)
        val press = pressRows(vm.await { s -> s.bests.isNotEmpty() }).single()

        runBlocking { settings.update { it.copy(showStrictPress = false) } }

        // Today's rule stays: a strict press that exists keeps its row whatever the switch says (the coach uses its best).
        val state = vm.await { !runBlocking { settings.settings.first() }.showStrictPress && it.bests.isNotEmpty() }
        assertEquals(listOf(press), pressRows(state))
        assertEquals(70.0, state.bests[press.id])
    }

    @Test
    fun pressOn_withAnArchivedOrRenamedPressRow_addsNoSecondRow() {
        runBlocking {
            val ohp = training.addExercise(Exercise(0, "OHP", Category.STRICT_PRESS, null, emptyList(), isBuiltIn = false))
            training.archiveExercise(ohp)
            settings.update { it.copy(showStrictPress = true) }
        }
        val state = viewModel().await { it.mainExercises.isNotEmpty() }
        assertEquals(listOf("OHP"), pressRows(state).map { it.name })
        assertNull(pressRows(state).firstOrNull { it.id == 0L })
    }
}
