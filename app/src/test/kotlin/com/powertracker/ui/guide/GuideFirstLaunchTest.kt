package com.powertracker.ui.guide

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertTextContains
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.powertracker.data.AdviceRepository
import com.powertracker.data.Backup
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.PlanItemEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.AppShell
import com.powertracker.ui.ModeTheme
import com.powertracker.ui.motion.ModeSwitchController
import com.powertracker.ui.motion.ModeSwitchFeedback
import com.powertracker.ui.motion.ModeSwitchKind
import com.powertracker.ui.plan.GUIDE_ROW
import com.powertracker.ui.plan.GUIDE_ROW_SUPPORTING
import com.powertracker.ui.plan.builder.BUILDER_TITLE
import com.powertracker.domain.coach.Coaching
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The guide opened by the first launch and from Settings (1.16, Task 3), through the real shell. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class GuideFirstLaunchTest {
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var settings: SettingsRepository

    private class RecordingFeedback : ModeSwitchFeedback {
        val starts = mutableListOf<ModeSwitchKind>()
        var stops = 0
        override fun start(kind: ModeSwitchKind) {
            starts += kind
        }
        override fun stop() {
            stops++
        }
        override fun skip() = stop()
        override fun prefetch(next: ModeSwitchKind) = Unit
        override fun release() = Unit
    }

    private val feedback = RecordingFeedback()
    private val controller = ModeSwitchController(feedback)

    /** The training repository, counting its new-user checks. */
    private inner class Counting : TrainingRepository(database.db) {
        var newUserChecks = 0
        override suspend fun isNewUser(): Boolean {
            newUserChecks++
            return super.isNewUser()
        }
    }

    private lateinit var training: Counting
    private var starts by mutableIntStateOf(0)
    private val day = LocalDate(2026, 11, 19)

    @Before
    fun setUp() {
        settings = SettingsRepository(context)
        // The settings store is one per process: each test starts as a new device.
        runBlocking {
            settings.update { com.powertracker.data.AppSettings() }
            settings.forgetGuideSeen()
        }
        training = Counting()
    }

    /** The app started, as MainActivity starts it: the gate on. [restart] starts it again, as after the process was killed. */
    private fun start(gate: Boolean = true) {
        val db = database.db
        compose.setContent {
            key(starts) {
                ModeTheme(settings.settings, darkTheme = true, motion = false, modeSwitch = controller) {
                    AppShell(training, AdviceRepository(db), settings, BackupService(db, settings), guideOnFirstLaunch = gate)
                }
            }
        }
        settle()
    }

    private fun restart() {
        compose.runOnUiThread { starts++ }
        settle()
    }

    private fun has(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun shows(text: String): Boolean = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    /** Until the gate has decided and the shell shows the guide or a tab. */
    private fun settle() {
        compose.waitUntil(10_000) { !has("guide-gate") && (has("guide") || has("tab-today")) }
        compose.waitForIdle()
    }

    private fun guideUp() = has("guide")

    private fun title() = compose.onNodeWithTag("guide-title").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()

    private fun nextTo(id: GuideScreenId) {
        while (title() != Guide.screen(id, stored()).title) {
            compose.onNodeWithTag("guide-next").performClick()
            compose.waitForIdle()
        }
    }

    private fun stored() = runBlocking { settings.settings.first().trainingMode }

    private fun seen() = runBlocking { settings.guideSeen.first() }

    private val store = File(context.filesDir, "datastore/settings.preferences_pb")

    /** The whole settings store as written, byte for byte (empty if never written). */
    private fun storeBytes(): ByteArray = runBlocking { settings.settings.first() }.let { if (store.exists()) store.readBytes() else ByteArray(0) }

    /** Every Room table, every row. */
    private fun tables(): String {
        val sql = database.db.openHelper.readableDatabase
        val names = sql.query("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name").use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
        return names.joinToString("\n") { name ->
            sql.query("SELECT * FROM `$name`").use { c ->
                buildString {
                    append(name).append(':')
                    while (c.moveToNext()) append((0 until c.columnCount).joinToString(",", "[", "]") { c.getString(it) ?: "null" })
                }
            }
        }
    }

    private fun exercise(): Long = runBlocking { database.db.exercises().insert(ExerciseEntity(name = "Squat", category = Category.SQUAT, parentLift = null, muscles = emptyList(), isBuiltIn = true)) }

    private fun planItem(mode: TrainingMode) = runBlocking {
        database.db.plan().insertAll(listOf(PlanItemEntity(weekday = DayOfWeek.MONDAY, exerciseId = exercise(), sets = 3, reps = 5, targetRpe = 8.0, sortOrder = 0, mode = mode)))
    }

    @Test
    fun firstLaunch_withNoData_opensTheGuideOnce() {
        start()
        assertTrue(guideUp())
        assertEquals(Guide.screen(GuideScreenId.WELCOME, TrainingMode.POWERLIFTING).title, title())
        assertFalse("not seen while it is open", seen())
        compose.onNodeWithTag("guide-skip").performClick()
        compose.waitUntil(10_000) { has("tab-today") }
        assertFalse(guideUp())
        compose.waitUntil(10_000) { seen() }
        restart()
        assertFalse("never twice", guideUp())
        assertTrue(has("tab-today"))
    }

    @Test
    fun firstLaunch_withASessionOrAPlanItemOnEitherSide_neverOpensIt() {
        val cases = listOf<Pair<String, () -> Unit>>(
            "a powerlifting session" to { runBlocking { database.db.sessions().insert(SessionEntity(date = day, bodyweightKg = null, readiness = null, mode = TrainingMode.POWERLIFTING)) } },
            "a bodybuilding session" to { runBlocking { database.db.sessions().insert(SessionEntity(date = day, bodyweightKg = null, readiness = null, mode = TrainingMode.BODYBUILDING)) } },
            "a powerlifting plan item" to { planItem(TrainingMode.POWERLIFTING) },
            "a bodybuilding plan item" to { planItem(TrainingMode.BODYBUILDING) },
        )
        for ((i, case) in cases.withIndex()) {
            val (name, add) = case
            runBlocking {
                database.db.sessions().clear()
                database.db.plan().clear()
            }
            add()
            if (i == 0) start() else restart()
            assertFalse(name, guideUp())
            assertTrue(name, has("tab-today"))
            assertFalse(name, seen())
        }
    }

    @Test
    fun seenIsReadFirst_soASeenDeviceRunsNoDataQuery() {
        runBlocking { settings.markGuideSeen() }
        start()
        assertFalse(guideUp())
        assertEquals(0, training.newUserChecks)
    }

    @Test
    fun notSeen_asksOnceWhetherThisIsANewUser() {
        start()
        assertTrue(guideUp())
        assertEquals(1, training.newUserChecks)
    }

    @Test
    fun theGate_isOffUnlessAskedFor() {
        start(gate = false)
        assertFalse(guideUp())
        assertEquals(0, training.newUserChecks)
    }

    @Test
    fun screen2_onFirstLaunch_setsTheRealSide_withNoAnimationSoundOrVibration() {
        start()
        nextTo(GuideScreenId.SIDE)
        assertTrue(has("guide-side-powerlifting") && has("guide-side-bodybuilding"))
        compose.onNodeWithTag("guide-side-bodybuilding").performClick()
        compose.waitUntil(10_000) { stored() == TrainingMode.BODYBUILDING }
        // Unlike the Settings switch, no side is dated as left: a new lifter has left nothing.
        val picked = runBlocking { settings.settings.first() }
        assertNull(picked.powerliftingLeftOn)
        assertNull(picked.bodybuildingLeftOn)
        compose.waitForIdle()
        assertEquals("no animation, no sound, no vibration", emptyList<ModeSwitchKind>(), feedback.starts)
        assertEquals(0, feedback.stops)
        assertNull(controller.shownMode)
        // The guide now speaks of the side picked, and is still open on screen 2.
        assertEquals(Guide.screen(GuideScreenId.SIDE, TrainingMode.BODYBUILDING).title, title())
        assertFalse(seen())
    }

    @Test
    fun screen2_pickingTheSideAlreadyOn_writesNothing() {
        start()
        nextTo(GuideScreenId.SIDE)
        val before = storeBytes()
        val rows = tables()
        compose.onNodeWithTag("guide-side-powerlifting").performClick()
        compose.waitForIdle()
        assertArrayEquals(before, storeBytes())
        assertEquals(rows, tables())
        assertEquals(TrainingMode.POWERLIFTING, stored())
    }

    private val collectors = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    private val viewModels = mutableListOf<com.powertracker.ui.today.TodayViewModel>()

    @org.junit.After
    fun stopViewModels() {
        collectors.cancel()
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    /** Today's state on [date], from the real view model, once loaded. */
    private fun todayOn(date: LocalDate): com.powertracker.ui.today.TodayState {
        val vm = com.powertracker.ui.today.TodayViewModel(training, AdviceRepository(database.db), settings) { date }
        viewModels += vm
        collectors.launch { vm.state.collect {} }
        val deadline = System.currentTimeMillis() + 5_000
        while (vm.state.value.loading) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(5)
            check(System.currentTimeMillis() < deadline) { "Today did not load" }
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        return vm.state.value
    }

    /**
     * The plan's check (Task 3), with the owner's call (b): the first-launch pick dates no side as left. Pick
     * bodybuilding on the first launch; 40 days later switch to powerlifting in Settings and give it a plan. Today
     * there: no welcome back (powerlifting was never left), and no easing back (there are no sessions).
     */
    @Test
    fun pickingBodybuildingOnFirstLaunch_thenFirstVisitingPowerlifting_showsNoWelcomeBackOrEasing() {
        start()
        nextTo(GuideScreenId.SIDE)
        compose.onNodeWithTag("guide-side-bodybuilding").performClick()
        compose.waitUntil(10_000) { stored() == TrainingMode.BODYBUILDING }
        val later = day.plus(DatePeriod(days = 40))
        // The Settings switch, as it is: it dates bodybuilding as left, and powerlifting has no date.
        runBlocking { settings.setTrainingMode(TrainingMode.POWERLIFTING, later) }
        val s = runBlocking { settings.settings.first() }
        assertNull(s.powerliftingLeftOn)
        assertEquals(later, s.bodybuildingLeftOn)
        assertFalse("first visit, no plan: no welcome back", todayOn(later).welcomeBack)
        planItem(TrainingMode.POWERLIFTING)
        val withPlan = todayOn(later)
        assertFalse("with a plan there: no welcome back", withPlan.welcomeBack)
        assertTrue("no easing back either", runBlocking { database.db.advice().all() }.none { it.type.name.contains("RE_ENTRY") || it.type.name.contains("REENTRY") })
    }

    /** The default side picked on the first launch, then the guide left: only "seen" is written. */
    @Test
    fun pickingTheDefaultSide_writesNothingBeyondSeen() {
        start()
        val before = runBlocking { settings.settings.first() }
        val rows = tables()
        nextTo(GuideScreenId.SIDE)
        compose.onNodeWithTag("guide-side-powerlifting").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("guide-skip").performClick()
        compose.waitUntil(10_000) { seen() }
        assertEquals(before, runBlocking { settings.settings.first() })
        assertEquals(rows, tables())
    }

    /**
     * Not the guide's: a new lifter who switches to bodybuilding in Settings, then back 40 days later and makes a
     * powerlifting plan, is not greeted as returning, since powerlifting was never trained before it was left (spec point 10).
     */
    @Test
    fun theSettingsSwitch_alone_showsNoWelcomeBack() {
        runBlocking { settings.setTrainingMode(TrainingMode.BODYBUILDING, day) }
        val later = day.plus(DatePeriod(days = 40))
        runBlocking { settings.setTrainingMode(TrainingMode.POWERLIFTING, later) }
        planItem(TrainingMode.POWERLIFTING)
        assertFalse(todayOn(later).welcomeBack)
    }

    @Test
    fun killedAfterTheSidePick_reopensOnScreen1_withTheSideKept() {
        start()
        val rows = tables()
        nextTo(GuideScreenId.SIDE)
        compose.onNodeWithTag("guide-side-bodybuilding").performClick()
        compose.waitUntil(10_000) { stored() == TrainingMode.BODYBUILDING }
        restart()
        assertTrue(guideUp())
        assertEquals(Guide.screen(GuideScreenId.WELCOME, TrainingMode.BODYBUILDING).title, title())
        assertEquals(TrainingMode.BODYBUILDING, stored())
        assertFalse(seen())
        assertEquals("nothing else written", rows, tables())
    }

    @Test
    fun screen8_withNoPlan_opensThatSidesTemplateList() {
        start()
        nextTo(GuideScreenId.YOUR_TURN)
        compose.onNodeWithTag("guide-next").assertTextContains(com.powertracker.ui.plan.templates.CHOOSE_A_TEMPLATE)
        assertFalse("powerlifting: no Build my plan", has("guide-last-secondary"))
        compose.onNodeWithTag("guide-next").performClick()
        compose.waitUntil(10_000) { has("template-list") }
        assertFalse(guideUp())
        compose.waitUntil(10_000) { seen() }
    }

    @Test
    fun buildMyPlan_opensTheBuilder() {
        start()
        nextTo(GuideScreenId.SIDE)
        compose.onNodeWithTag("guide-side-bodybuilding").performClick()
        compose.waitUntil(10_000) { stored() == TrainingMode.BODYBUILDING }
        compose.waitForIdle()
        nextTo(GuideScreenId.YOUR_TURN)
        compose.onNodeWithTag("guide-last-secondary").assertTextContains(com.powertracker.ui.plan.builder.BUILD_MY_PLAN)
        compose.onNodeWithTag("guide-last-secondary").performClick()
        compose.waitUntil(10_000) { shows(BUILDER_TITLE) && !guideUp() }
        compose.waitUntil(10_000) { seen() }
    }

    @Test
    fun backFromTheTemplateList_landsOnPlan_andTheGuideStaysClosed() {
        start()
        nextTo(GuideScreenId.YOUR_TURN)
        compose.onNodeWithTag("guide-next").performClick()
        compose.waitUntil(10_000) { has("template-list") }
        Espresso.pressBack()
        compose.waitUntil(10_000) { !has("template-list") }
        compose.waitForIdle()
        assertFalse(guideUp())
        assertTrue("on Plan", compose.onNodeWithTag("tab-plan").fetchSemanticsNode().config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Selected) { false })
        compose.waitUntil(10_000) { seen() }
        restart()
        assertFalse(guideUp())
    }

    private fun openSettingsGuide() {
        compose.onNodeWithTag("tab-plan").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("Settings")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.waitUntil(10_000) { has("settings-guide") }
        compose.onNodeWithTag("settings-guide").performScrollTo().performClick()
        compose.waitUntil(10_000) { guideUp() }
    }

    @Test
    fun settings_hasAGuideRow_thatOpensTheGuideAsAReplay() {
        start(gate = false)
        compose.onNodeWithTag("tab-plan").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("Settings")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.waitUntil(10_000) { has("settings-guide") }
        compose.onNodeWithText(GUIDE_ROW).assertExists()
        compose.onNodeWithText(GUIDE_ROW_SUPPORTING).assertExists()
        compose.onNodeWithTag("settings-guide").performScrollTo().performClick()
        compose.waitUntil(10_000) { guideUp() }
        assertEquals(Guide.screen(GuideScreenId.WELCOME, TrainingMode.POWERLIFTING).title, title())
        // Back where it was opened from.
        compose.onNodeWithTag("guide-skip").performClick()
        compose.waitUntil(10_000) { has("settings-guide") }
        assertFalse("a replay writes no seen", seen())
    }

    @Test
    fun screen2_onAReplay_showsNoButtons() {
        start(gate = false)
        openSettingsGuide()
        nextTo(GuideScreenId.SIDE)
        assertFalse(has("guide-side-powerlifting"))
        assertFalse(has("guide-side-bodybuilding"))
    }

    @Test
    fun screen8_withAPlan_offersDoneAndSeeMyPlan_andReplacesNothing() {
        planItem(TrainingMode.POWERLIFTING)
        start()
        assertFalse(guideUp())
        val rows = tables()
        openSettingsGuide()
        nextTo(GuideScreenId.YOUR_TURN)
        compose.onNodeWithTag("guide-next").assertTextContains(DONE)
        compose.onNodeWithTag("guide-last-secondary").assertTextContains(SEE_MY_PLAN)
        compose.onNodeWithTag("guide-last-secondary").performClick()
        compose.waitUntil(10_000) { !guideUp() }
        compose.waitForIdle()
        assertTrue("on Plan", compose.onNodeWithTag("tab-plan").fetchSemanticsNode().config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Selected) { false })
        assertEquals(rows, tables())
    }

    @Test
    fun replay_writesNothing() {
        planItem(TrainingMode.BODYBUILDING)
        runBlocking { database.db.sessions().insert(SessionEntity(date = day, bodyweightKg = null, readiness = null)) }
        start()
        assertFalse(guideUp())
        val rows = tables()
        val store = storeBytes()
        openSettingsGuide()
        nextTo(GuideScreenId.YOUR_TURN)
        compose.onNodeWithTag("guide-next").performClick()
        compose.waitUntil(10_000) { !guideUp() }
        compose.waitForIdle()
        assertEquals(rows, tables())
        assertArrayEquals(store, storeBytes())
    }

    @Test
    fun aBackupMadeAfterTheGuide_hasNoGuideKey() {
        runBlocking { settings.markGuideSeen() }
        val text = Backup.encode(runBlocking { BackupService(database.db, settings).export(0) })
        assertFalse(text, text.contains("guide", ignoreCase = true))
    }

    @Test
    fun restore_keepsSeen() {
        val backup = BackupService(database.db, settings)
        val file = runBlocking { backup.export(0) }
        runBlocking { settings.markGuideSeen() }
        runBlocking { backup.restore(file) }
        assertTrue(seen())
    }
}

