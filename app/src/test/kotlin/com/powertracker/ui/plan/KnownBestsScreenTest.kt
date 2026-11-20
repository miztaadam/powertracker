package com.powertracker.ui.plan

import android.content.Context
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.testing.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Plan's Known bests on screen (1.16.1): each row's "Best e1RM" line, and strict press with the Progress switch on. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class KnownBestsScreenTest {
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings() } }
    }

    private fun showPlan() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("baseline-Squat")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun setBest(lift: String, text: String) {
        compose.onNodeWithTag("baseline-$lift").performClick()
        compose.onNodeWithText("Estimated 1RM (kg)").performTextReplacement(text)
        compose.onNodeWithText("Save").performClick()
    }

    @Test
    fun noBest_noLine_thenSetBest_showsIt() {
        showPlan()
        assertTrue(compose.onAllNodes(hasText("Best e1RM", substring = true)).fetchSemanticsNodes().isEmpty())

        setBest("Squat", "182,5")

        waitForText("Best e1RM 182.5 kg")
        // Only the squat has one.
        assertEquals(1, compose.onAllNodes(hasText("Best e1RM", substring = true)).fetchSemanticsNodes().size)
    }

    @Test
    fun pressOn_withNoRow_rowShows_andItsSetBestWorks() {
        runBlocking { settings.update { it.copy(showStrictPress = true) } }
        showPlan()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("baseline-Strict press")).fetchSemanticsNodes().isNotEmpty() }

        setBest("Strict press", "70")

        waitForText("Best e1RM 70 kg")
        val press = runBlocking { training.exercises().first() }.single { it.category == Category.STRICT_PRESS }
        assertEquals("Strict press", press.name)
        assertEquals(listOf(press.id to 70.0), runBlocking { training.history().first() }.baselines.map { it.exerciseId to it.e1rmKg })
    }

    @Test
    fun pressOff_withNoRow_noPressRow_asBefore() {
        showPlan()
        compose.onNodeWithTag("baseline-Strict press").assertDoesNotExist()
    }

    /** At 320 dp a name over two lines keeps its line and the button beside it, never under or over it. */
    @Test
    @Config(qualifiers = "w320dp-h2400dp-xxhdpi")
    // Real text measuring: without it Robolectric's text has next to no width and never wraps.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun narrowPhone_twoLineName_buttonBesideIt() {
        val name = "Seated strict press from the safety pins"
        runBlocking {
            val id = training.addExercise(Exercise(0, name, Category.STRICT_PRESS, null, emptyList(), isBuiltIn = false))
            training.setBaseline(Baseline(id, 72.5, LocalDate(2026, 11, 18)))
        }
        showPlan()
        waitForText("Best e1RM 72.5 kg")

        val title = compose.onNodeWithText(name).getBoundsInRoot()
        val line = compose.onNodeWithText("Best e1RM 72.5 kg").getBoundsInRoot()
        val button = compose.onNodeWithTag("baseline-$name").getBoundsInRoot()
        val oneLine = compose.onNodeWithText("Squat").getBoundsInRoot()
        val root = compose.onRoot().getBoundsInRoot()

        // Two lines of name, then the best under it.
        assertTrue("the name should take two lines: $title", title.height >= (oneLine.height * 2) - 1.dp)
        assertTrue("the line should be under the name", line.top >= title.bottom - 0.5.dp)
        // The button is beside the text, inside the screen.
        assertTrue("the name runs under the button: $title, $button", title.right <= button.left)
        assertTrue("the line runs under the button: $line, $button", line.right <= button.left)
        assertTrue("the button leaves the screen: $button", button.right <= root.right)
    }
}
