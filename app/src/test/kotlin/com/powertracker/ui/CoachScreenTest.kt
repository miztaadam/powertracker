package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.coach.CoachScreen
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class CoachScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var exercises: List<Exercise>

    private val stallReason = "Bench e1RM hasn't improved for 3 weeks. You've mostly missed at lockout — this works the triceps."

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            exercises = training.exercises().first()
        }
    }

    @After
    fun tearDown() = db.close()

    private fun id(name: String) = exercises.first { it.name == name }.id

    private fun insertStall() = runBlocking {
        // Bench is trained on Wednesday (with a row already there) and again on Friday.
        training.savePlanItem(PlanItem(0, DayOfWeek.FRIDAY, id("Bench"), sets = 3, reps = 5, targetRpe = 8.0, order = 0))
        training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, id("Bench"), sets = 4, reps = 6, targetRpe = 8.0, order = 0))
        training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, id("Squat"), sets = 5, reps = 5, targetRpe = 8.0, order = 1))
        val session = training.startSession(today.minus(2, DateTimeUnit.DAY), null, null)
        training.logSet(session, LoggedSet(id("Bench"), 105.0, 5, 9.0, failed = false, missPoint = null, isTopSet = false))
        val payload = AdvicePayload.Accessories(
            listOf(AccessoryPrescription("Close-grip bench", 3, 3, 5), AccessoryPrescription("JM press", 3, 8, 10)),
        )
        advice.insert(listOf(Advice(AdviceType.STALL, MainLift.BENCH, today, stallReason, 3, payload)))
    }

    private fun show() = compose.setContent {
        PowertrackerTheme { CoachScreen(training, advice, settings, today = { today }) }
    }

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun stallCard_applyAddsAccessoriesToPlan() {
        insertStall()
        show()
        waitFor { shows("Bench has stalled") }
        compose.onNodeWithText("STALL · BENCH").assertExists()
        compose.onNodeWithText(stallReason).assertExists()
        compose.onNodeWithText("Close-grip bench").assertExists()
        compose.onNodeWithText("3 × 3–5").assertExists()
        compose.onNodeWithText("3 × 8–10").assertExists()

        compose.onNodeWithText("Add to plan").performClick()

        waitFor { runBlocking { advice.all() }.single().status == AdviceStatus.APPLIED }
        val added = runBlocking { training.plan().first() }.filter { it.exerciseId == id("Close-grip bench") || it.exerciseId == id("JM press") }
        assertEquals(
            listOf(
                PlanItem(0, DayOfWeek.WEDNESDAY, id("Close-grip bench"), sets = 3, reps = 5, targetRpe = 8.0, order = 2),
                PlanItem(0, DayOfWeek.WEDNESDAY, id("JM press"), sets = 3, reps = 10, targetRpe = 8.0, order = 3),
            ),
            added.sortedBy { it.order }.map { it.copy(id = 0) },
        )
        waitFor { shows("Added 2 exercises to Wednesday") }
        waitFor { !shows("Bench has stalled") }
    }

    @Test
    fun dismiss_removesCard() {
        insertStall()
        show()
        waitFor { shows("Bench has stalled") }

        compose.onNodeWithText("Dismiss").performClick()

        waitFor { !shows("Bench has stalled") }
        waitFor { runBlocking { advice.all() }.single().status == AdviceStatus.DISMISSED }
        assertEquals(3, runBlocking { training.plan().first() }.size)
        waitFor { shows("No advice right now. Keep logging.") }
    }

    @Test
    fun emptyState_showsMessage() {
        // A TARGET belongs to Today, not to the feed.
        runBlocking {
            advice.insert(listOf(Advice(AdviceType.TARGET, MainLift.SQUAT, today, "Coach suggests 155 × 5 @ 8.", 0, AdvicePayload.Target(id("Squat"), 155.0, 5, 8.0))))
        }
        show()
        waitFor { shows("No advice right now. Keep logging.") }
        compose.onNodeWithText("Coach suggests", substring = true).assertDoesNotExist()
    }
}
