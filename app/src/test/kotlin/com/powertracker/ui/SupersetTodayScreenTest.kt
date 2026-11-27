package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.SUPERSET_SWITCH_GUARD_MS
import com.powertracker.ui.today.TodayScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * Supersets on the Today screen (1.24): Wednesday's squat, bench and row, each done last week, the bench linked to the
 * squat above it. The view model's rules are in [SupersetTodayViewModelTest]; here, what the screen does with them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class SupersetTodayScreenTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var squat: Exercise
    private lateinit var bench: Exercise
    private lateinit var row: Exercise
    private var benchPlanId = 0L
    private var sessionId = 0L
    private var rests = 0

    private fun set(exercise: Exercise, kg: Double) = LoggedSet(exercise.id, kg, 5, 8.0, failed = false, missPoint = null, isTopSet = false)

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
            row = exercises.first { it.name == "Row" }
            val lastWeek = training.startSession(today.minus(7, DateTimeUnit.DAY), null, null)
            for ((exercise, kg) in listOf(squat to 150.0, bench to 100.0, row to 60.0)) training.logSet(lastWeek, set(exercise, kg))
            listOf(squat, bench, row).forEachIndexed { i, exercise ->
                val id = training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, exercise.id, sets = 3, reps = 5, targetRpe = 8.0, order = i))
                if (exercise == bench) benchPlanId = id
            }
            sessionId = training.startSession(today, null, null)
        }
    }

    private fun link() = runBlocking { assertEquals(true, training.setSuperset(benchPlanId, true)) }

    private fun show() = compose.setContent {
        PowertrackerTheme { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { today }, onSetLogged = { rests++ }) }
    }

    private fun waitFor(what: String, condition: () -> Boolean) = try {
        compose.waitUntil(5_000) {
            compose.waitForIdle()
            condition()
        }
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
        throw AssertionError("Timed out waiting for $what", e)
    }

    private fun spoken(description: String) = compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
    private fun shown(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun logged() = runBlocking { training.setsForSession(sessionId).first() }.map { it.set.exerciseId }

    /** The open card: its exercise's "Form guide" button is beside the eyebrow spoken as [eyebrow]. */
    private fun waitForCard(eyebrow: String) = waitFor("the card \"$eyebrow\"") { spoken(eyebrow) }

    @Test
    fun theFirstOpens_loggingItOpensTheSecondWithNoRest_andTheSecondStartsTheRestAndOpensTheFirst() {
        link()
        show()
        waitForCard("Exercise 1 of 3 · Superset A")
        // On screen the tag alone, in the count's place; the folded bench carries its own.
        compose.onNodeWithText("SUPERSET A", useUnmergedTree = true).assertExists()
        assertEquals(false, shown("EXERCISE 1 OF 3"))
        compose.onNodeWithText("Superset B", useUnmergedTree = true).assertExists()

        compose.onNodeWithText("Log set 1").performClick()
        waitForCard("Exercise 2 of 3 · Superset B")
        assertEquals(listOf(squat.id), logged())
        assertEquals(0, rests)
        // The squat is folded, with its tag and its count.
        compose.onNodeWithText("Superset A", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("1 of 3", useUnmergedTree = true).assertExists()

        ShadowSystemClock.advanceBy(Duration.ofMillis(SUPERSET_SWITCH_GUARD_MS))
        compose.onNodeWithText("Log set 1").performClick()
        waitForCard("Exercise 1 of 3 · Superset A")
        assertEquals(listOf(squat.id, bench.id), logged())
        assertEquals(1, rests)
        compose.onNodeWithText("Log set 2").assertExists()
    }

    @Test
    fun openedMidPair_theCardIsTheOneWhoseTurnItIs() {
        link()
        runBlocking { training.logSet(sessionId, set(squat, 150.0)) }

        show()

        waitForCard("Exercise 2 of 3 · Superset B")
        compose.onNodeWithText("Log set 1").assertExists()
    }

    @Test
    fun theTagAddsNoHeight_theLogButtonStaysWhereItWas() {
        show()
        waitFor("the squat's card") { shown("EXERCISE 1 OF 3") && shown("Log set 1") }
        val plain = compose.onNodeWithText("Log set 1").getUnclippedBoundsInRoot()

        link()
        waitForCard("Exercise 1 of 3 · Superset A")
        compose.waitForIdle()

        assertEquals(plain.top, compose.onNodeWithText("Log set 1").getUnclippedBoundsInRoot().top)
    }

    @Test
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun onASmallPhone_theTagsAreWhole() {
        link()
        show()
        waitForCard("Exercise 1 of 3 · Superset A")
        compose.onNodeWithText("SUPERSET A", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Superset B", useUnmergedTree = true).assertExists()
        compose.assertTextsFit()

        compose.onNodeWithText("Log set 1").performClick()
        waitForCard("Exercise 2 of 3 · Superset B")
        compose.onNodeWithText("SUPERSET B", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Superset A", useUnmergedTree = true).assertExists()
        compose.assertTextsFit()
    }
}
