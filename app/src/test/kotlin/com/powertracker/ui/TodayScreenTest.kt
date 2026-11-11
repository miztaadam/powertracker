package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class TodayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate.of(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var squat: Exercise

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            squat = training.exercises().first().first { it.name == "Squat" }
            // Planned today: squat 5 × 5 @ 8. Last week's top set: 150 × 5 @ 7.
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            val lastWeek = training.startSession(today.minusDays(7), null, null)
            training.logSet(lastWeek, LoggedSet(squat.id, 150.0, 5, 7.0, failed = false, missPoint = null, isTopSet = false))
        }
    }

    @After
    fun tearDown() = db.close()

    private fun startSession() = runBlocking { training.startSession(today, null, null) }

    private fun show() = compose.setContent {
        PowertrackerTheme { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { today }) }
    }

    private fun loggedToday(): List<LoggedSet> = runBlocking {
        val session = training.sessionOn(today).first() ?: return@runBlocking emptyList()
        training.setsForSession(session.id).first().map { it.set }
    }

    /**
     * Polls like waitUntil, but also drains the main looper each round: a view-model action with two
     * database steps resumes on the main thread in between, which waitUntil alone does not run here.
     */
    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun waitForText(text: String) =
        waitFor { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForSets(count: Int) = waitFor { loggedToday().size == count }

    @Test
    fun tapSet_logsAsPrescribed() {
        show()
        waitForText("Start session")
        compose.onNodeWithText("Start session").performClick()

        waitForText("Log set 1")
        compose.onNodeWithContentDescription("Weight 150 kg").assertExists()
        compose.onNodeWithText("Log set 1").performClick()

        waitForSets(1)
        // Planned 5 reps @ 8 at last week's top-set weight.
        assertEquals(listOf(LoggedSet(squat.id, 150.0, 5, 8.0, failed = false, missPoint = null, isTopSet = false)), loggedToday())
        waitForText("Log set 2")
        compose.onNodeWithText("Set 1: 150 kg × 5 @ 8, done").assertExists()
        compose.onNodeWithText("Set 2 of 5").assertExists()
    }

    @Test
    fun failedSet_showsMissRow() {
        startSession()
        show()
        waitForText("Log set 1")

        // RPE 8 -> 9: a hard set, but no question yet.
        repeat(2) { compose.onNodeWithContentDescription("Increase RPE").performClick() }
        compose.onNodeWithText("Log set 1").performClick()
        waitForText("Log set 2")
        compose.onNodeWithTag("miss-row-1").assertDoesNotExist()

        // RPE 9.5: the grind is worth asking about.
        compose.onNodeWithContentDescription("Increase RPE").performClick()
        compose.onNodeWithText("Log set 2").performClick()
        waitForText("Log set 3")
        compose.onNodeWithTag("miss-row-2").assertExists()

        // A missed rep always asks, with Floor / Mid / Lockout.
        repeat(3) { compose.onNodeWithContentDescription("Decrease RPE").performClick() }
        compose.onNodeWithText("Missed").performClick()
        waitForText("Log set 4")
        compose.onNodeWithTag("miss-row-3").assertExists()
        for (point in listOf("Floor", "Mid", "Lockout")) {
            compose.onNode(hasText(point) and hasAnyAncestor(hasTestTag("miss-row-3"))).assertExists()
        }
        assertEquals(listOf(false, false, true), loggedToday().map { it.failed })

        compose.onNode(hasText("Lockout") and hasAnyAncestor(hasTestTag("miss-row-3"))).performClick()
        waitFor { loggedToday().last().missPoint == MissPoint.LOCKOUT }
        // Tapping the selected point clears it.
        waitFor { compose.onAllNodes(isSelected() and hasText("Lockout")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Lockout") and hasAnyAncestor(hasTestTag("miss-row-3"))).performClick()
        waitFor { loggedToday().last().missPoint == null }
    }

    @Test
    fun coachChip_changesNothingUntilApplied() {
        // A known best gives the coach enough history to suggest a target: 150 @ 7 -> 155 × 5 @ 8.
        runBlocking { training.setBaseline(Baseline(squat.id, 190.0, today.minusDays(30))) }
        startSession()
        show()

        waitForText("155 × 5 @ 8")
        compose.onNodeWithText("Coach suggests", ignoreCase = true).assertExists()
        // Still the planned numbers: nothing changes until Apply.
        compose.onNodeWithContentDescription("Weight 150 kg").assertExists()

        compose.onNodeWithText("Apply").performClick()

        waitFor { compose.onAllNodes(hasContentDescription("Weight 155 kg")).fetchSemanticsNodes().isNotEmpty() }
        waitFor { compose.onAllNodesWithText("Apply").fetchSemanticsNodes().isEmpty() }
        val target = runBlocking { advice.all() }.single { it.type == AdviceType.TARGET }
        assertEquals(AdviceStatus.APPLIED, target.status)
        assertTrue("applying a suggestion must not log anything", loggedToday().isEmpty())
    }

    @Test
    fun typo_promptsBeforeSaving() {
        startSession()
        show()
        waitForText("Log set 1")

        repeat(8) { compose.onNodeWithContentDescription("Increase Reps").performClick() } // 5 -> 13
        compose.onNodeWithText("Log set 1").performClick()

        waitForText("Did you mean")
        assertTrue("nothing is saved before Confirm", loggedToday().isEmpty())

        // Edit goes back without saving.
        compose.onNodeWithText("Edit").performClick()
        waitFor { compose.onAllNodesWithText("Did you mean", substring = true).fetchSemanticsNodes().isEmpty() }
        assertTrue(loggedToday().isEmpty())

        compose.onNodeWithText("Log set 1").performClick()
        waitForText("Did you mean")
        compose.onNodeWithText("Confirm").performClick()

        waitForSets(1)
        assertEquals(13, loggedToday().single().reps)
    }

    @Test
    fun plateStrip_shows() {
        startSession()
        show()
        waitForText("Log set 1")
        compose.onNodeWithText("25 + 25 + 15").assertExists() // 150 kg

        compose.onNodeWithContentDescription("Weight 150 kg").performClick()
        compose.onNodeWithTag("number-entry").performTextReplacement("182.5")
        compose.onNodeWithText("Set").performClick()

        waitForText("25 + 25 + 25 + 5 + 1.25")
        compose.onNodeWithText("25 + 25 + 25 + 5 + 1.25").assertExists()

        // 181 kg cannot be loaded with 1.25 kg plates: say so instead of drawing a wrong bar.
        compose.onNodeWithContentDescription("Weight 182.5 kg").performClick()
        compose.onNodeWithTag("number-entry").performTextReplacement("181")
        compose.onNodeWithText("Set").performClick()
        waitForText("Can't load 181 kg exactly")
    }

    @Test
    fun appliedDeloadTarget_showsSetOfFewerSets() {
        runBlocking {
            training.setBaseline(Baseline(squat.id, 190.0, today.minusDays(30)))
            advice.insert(listOf(Advice(AdviceType.DELOAD, null, today, "Take a deload week.", 2, AdvicePayload.Deload())))
        }
        startSession()
        show()

        waitForText("· 3 sets")
        compose.onNodeWithText("Set 1 of 5").assertExists()

        compose.onNodeWithText("Apply").performClick()

        waitForText("Set 1 of 3")
    }
}
