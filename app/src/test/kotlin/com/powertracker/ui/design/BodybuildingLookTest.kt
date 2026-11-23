package com.powertracker.ui.design

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.ModeTheme
import com.powertracker.ui.Tab
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The bodybuilding side's look (bodybuilding mode spec §7.1, §7.2): the blue palette, the label and the Settings switch. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class BodybuildingLookTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val db get() = database.db
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings() } }
    }

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "Today" to { TodayShowcase() },
        "Coach" to { CoachShowcase() },
        "Progress" to { ProgressShowcase() },
        "Exercises" to { ExercisesShowcase() },
        "Plan" to { PlanShowcase() },
        "Settings" to { SettingsShowcase() },
    )

    private fun labelCount() = compose.onAllNodes(hasTestTag("mode-label"), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun bodybuildingSide_everyTopLevelScreenShowsTheLabel() {
        var current by mutableStateOf(0)
        compose.setContent { ShowcaseFrame(dark = true, tab = Tab.TODAY, mode = TrainingMode.BODYBUILDING) { screens[current].second() } }
        for (i in screens.indices) {
            current = i
            compose.waitForIdle()
            assertEquals("${screens[i].first} shows one label", 1, labelCount())
            compose.onNodeWithTag("mode-label", useUnmergedTree = true).assertExistsWithText("BODYBUILDING")
        }
    }

    @Test
    fun powerliftingSide_noScreenShowsTheLabel() {
        var current by mutableStateOf(0)
        compose.setContent { ShowcaseFrame(dark = true, tab = Tab.TODAY) { screens[current].second() } }
        for (i in screens.indices) {
            current = i
            compose.waitForIdle()
            assertEquals("${screens[i].first} shows no label", 0, labelCount())
        }
    }

    @Test
    fun theAccentIsBlue_inDarkAndLight() {
        val seen = mutableMapOf<String, Color>()
        compose.setContent {
            PowertrackerTheme(mode = TrainingMode.BODYBUILDING, darkTheme = true) {
                val p = MaterialTheme.colorScheme.primary
                val a = PowertrackerTheme.colors.accent
                SideEffect { seen["dark primary"] = p; seen["dark accent"] = a }
            }
            PowertrackerTheme(mode = TrainingMode.BODYBUILDING, darkTheme = false) {
                val p = MaterialTheme.colorScheme.primary
                val a = PowertrackerTheme.colors.accent
                SideEffect { seen["light primary"] = p; seen["light accent"] = a }
            }
            PowertrackerTheme(darkTheme = true) {
                val a = PowertrackerTheme.colors.accent
                SideEffect { seen["powerlifting dark accent"] = a }
            }
        }
        compose.waitForIdle()
        assertEquals(Color(0xFF8DB8FF), seen["dark primary"])
        assertEquals(Color(0xFF8DB8FF), seen["dark accent"])
        assertEquals(Color(0xFF1F4FA8), seen["light primary"])
        assertEquals(Color(0xFF1F4FA8), seen["light accent"])
        assertEquals(Color(0xFF93979D), seen["powerlifting dark accent"])
    }

    @Test
    fun theSwitch_storesTheMode_andTheLookChangesAtOnce() {
        runBlocking { settings.update { it.copy(trainingDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)) } }
        var accent = Color.Unspecified
        compose.setContent {
            ModeTheme(settings.settings, darkTheme = true, motion = false) {
                val a = PowertrackerTheme.colors.accent
                SideEffect { accent = a }
                SettingsScreen(settings, BackupService(db, settings), onBack = {}, today = { LocalDate(2026, 11, 13) })
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("bodybuilding-mode")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(Color(0xFF93979D), accent)
        assertEquals(0, labelCount())

        compose.onNodeWithTag("bodybuilding-mode").performClick()
        compose.waitUntil(5_000) { runBlocking { settings.settings.first().trainingMode } == TrainingMode.BODYBUILDING }
        compose.waitUntil(5_000) { accent == Color(0xFF8DB8FF) }
        compose.waitUntil(5_000) { labelCount() == 1 }
        val stored = runBlocking { settings.settings.first() }
        // The first switch-on copies the training days and dates the side left.
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), stored.bodybuildingTrainingDays)
        assertEquals(LocalDate(2026, 11, 13), stored.powerliftingLeftOn)

        compose.onNodeWithTag("bodybuilding-mode").performClick()
        compose.waitUntil(5_000) { runBlocking { settings.settings.first().trainingMode } == TrainingMode.POWERLIFTING }
        compose.waitUntil(5_000) { accent == Color(0xFF93979D) }
        compose.waitUntil(5_000) { labelCount() == 0 }
    }

    @Test
    fun trainingStyle_isTheFirstGroupInSettings() {
        compose.setContent { PowertrackerTheme { SettingsShowcase() } }
        compose.onNodeWithText("Bodybuilding mode").assertExists()
        compose.onNodeWithText("Rep ranges, a physique coach and a blue look. Your powerlifting plan is kept.").assertExists()
        val style = compose.onNodeWithText("TRAINING STYLE").fetchSemanticsNode().positionInRoot.y
        val loading = compose.onNodeWithText("LOADING AND TARGETS").fetchSemanticsNode().positionInRoot.y
        val switch = compose.onNodeWithTag("bodybuilding-mode").fetchSemanticsNode().positionInRoot.y
        assertTrue("Training style comes before Loading and targets", style < switch && switch < loading)
        // Nothing but the header sits above it.
        val headers = compose.onAllNodes(hasText("Settings")).fetchSemanticsNodes()
        assertTrue(headers.all { it.positionInRoot.y < style })
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsWithText(text: String) {
        val node = fetchSemanticsNode()
        fun texts(n: androidx.compose.ui.semantics.SemanticsNode): List<String> =
            (n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.map { it.text } ?: emptyList()) + n.children.flatMap(::texts)
        assertTrue("label reads $text", text in texts(node))
    }
}
