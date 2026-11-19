package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.Backup
import com.powertracker.data.BackupService
import com.powertracker.data.RestAlert
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.library.OWN_NAME
import com.powertracker.ui.library.SAVE_FAILED
import com.powertracker.ui.library.assertCustomExerciseEmpty
import com.powertracker.ui.library.assertCustomExerciseKept
import com.powertracker.ui.library.assertSaveFailedOverTheForm
import com.powertracker.ui.library.createFormOpen
import com.powertracker.ui.library.fillCustomExercise
import com.powertracker.ui.library.checkInfoButtonOpensGuide
import com.powertracker.ui.plan.ADD_FORM_TAP_WINDOW_MS
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.RestoreCheck
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.plan.backupFileName
import com.powertracker.ui.plan.checkBackupText
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.powertracker.ui.components.FakeHapticFeedback
import com.powertracker.ui.components.WithHaptics

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class PlanScreenTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
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
        runBlocking { settings.update { com.powertracker.data.AppSettings() } }
    }

    @Test
    fun addPlanItem_showsUnderWeekday() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }

        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("add-MONDAY")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("add-MONDAY").performClick()
        compose.onNodeWithTag("exercise-search").performTextReplacement("Squat")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("exercise-Squat")).fetchSemanticsNodes().isNotEmpty() }
        // Picking opens the form with the defaults for a compound lift, 3 sets x 5 reps @ RPE 8; Add stores them.
        compose.onNodeWithTag("exercise-Squat").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Add") and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty() }
        // Past the moment in which the new form ignores taps (the second tap of a double tap on the row).
        compose.mainClock.advanceTimeBy(ADD_FORM_TAP_WINDOW_MS)
        compose.onNode(hasText("Add") and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Squat  3 × 5 @ 8").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Squat  3 × 5 @ 8").assertExists()
    }

    @Test
    fun moreMenu_editOpensDialogWithItemValues_andDeleteRemovesRow() {
        runBlocking {
            val bench = training.exercises().first().first { it.name == "Bench" }
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, bench.id, sets = 4, reps = 6, targetRpe = 7.5, order = 0))
        }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        val row = "Bench  4 × 6 @ 7.5"
        compose.waitUntil(5_000) { compose.onAllNodesWithText(row).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Edit Monday").assertExists()
        compose.onNode(hasText("Bench") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("4").assertExists()
        compose.onNodeWithText("6").assertExists()
        compose.onNodeWithText("7.5").assertExists()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(row).fetchSemanticsNodes().isEmpty() }
        assertTrue(runBlocking { training.plan().first().isEmpty() })
    }

    @Test
    fun infoButton_opensGuide_fromEveryPlace() {
        runBlocking {
            val exercises = training.exercises().first()
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exercises.first { it.name == "Bench" }.id, sets = 4, reps = 6, targetRpe = 7.5, order = 0))
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exercises.first { it.name == "Curl" }.id, sets = 3, reps = 10, targetRpe = 8.0, order = 1))
            // The lifter's own exercise has the button too, and a sheet that says there is no guide.
            val own = training.addExercise(Exercise(0, "Tyre flip", Category.ACCESSORY, null, emptyList(), isBuiltIn = false))
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, own, sets = 3, reps = 10, targetRpe = 8.0, order = 2))
        }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tyre flip  3 × 10 @ 8").fetchSemanticsNodes().isNotEmpty() }

        compose.checkInfoButtonOpensGuide("Bench")
        compose.checkInfoButtonOpensGuide("Curl")
        compose.checkInfoButtonOpensGuide("Tyre flip")

        assertEquals(3, runBlocking { training.plan().first() }.size)
    }

    @Test
    fun edit_changeExercise_swapsItThroughThePicker_onSave() {
        runBlocking {
            val bench = training.exercises().first().first { it.name == "Bench" }
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, bench.id, sets = 4, reps = 6, targetRpe = 7.5, order = 0))
            // The exercise to change to has no row yet.
            db.exercises().all().filter { it.name == "Curl" }.forEach { db.exercises().delete(it.id) }
        }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Bench  4 × 6 @ 7.5").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithContentDescription("Change exercise").performClick()
        compose.onNodeWithTag("exercise-search").performTextReplacement("curl")
        compose.onNodeWithTag("exercise-Curl").performClick()
        // Chosen, not saved: Cancel would still leave bench, and no row for the curl.
        compose.onNodeWithText("Edit Monday").assertExists()
        assertTrue(runBlocking { db.exercises().all() }.none { it.name == "Curl" })
        compose.onNodeWithText("Save").performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Curl  4 × 6 @ 7.5").fetchSemanticsNodes().isNotEmpty() }
        val curl = runBlocking { db.exercises().all() }.single { it.name == "Curl" }
        assertEquals(listOf(curl.id), runBlocking { training.plan().first() }.map { it.exerciseId })
    }

    // Native graphics, so text is measured at its real width.
    @Test
    @org.robolectric.annotation.Config(qualifiers = "w360dp-h2400dp-xxhdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun planRows_fitASmallPhone_withFullSizeTargets() {
        runBlocking {
            val exercises = training.exercises().first()
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exercises.first { it.name == "Bulgarian split squat" }.id, sets = 3, reps = 10, targetRpe = 7.5, order = 0))
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exercises.first { it.name == "Squat" }.id, sets = 3, reps = 5, targetRpe = 8.0, order = 1))
        }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Squat  3 × 5 @ 8").fetchSemanticsNodes().isNotEmpty() }
        val width = compose.onRoot().getUnclippedBoundsInRoot().width
        assertEquals(360.dp, width)

        for (name in listOf("Bulgarian split squat", "Squat")) {
            compose.onNodeWithContentDescription("Form guide for $name").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
        // The info button and the row's own controls are all on screen, side by side.
        val controls = listOf("Form guide for Bulgarian split squat", "Form guide for Squat").map { compose.onNodeWithContentDescription(it) } +
            listOf("Move up", "Move down", "More").flatMap { d -> List(2) { compose.onAllNodesWithContentDescription(d)[it] } }
        for (control in controls) {
            val b = control.getUnclippedBoundsInRoot()
            assertTrue(b.left >= 0.dp && b.right <= width)
        }
        val firstRow = listOf("Form guide for Bulgarian split squat", "Move up", "Move down", "More").map { compose.onAllNodesWithContentDescription(it)[0].getUnclippedBoundsInRoot() }
        assertTrue(firstRow.zipWithNext().all { (a, b) -> a.right <= b.left })
        // The text keeps a readable share of the row: it wraps, it is not squeezed out.
        for (text in listOf("Bulgarian split squat  3 × 10 @ 7.5", "Squat  3 × 5 @ 8")) {
            val b = compose.onNodeWithText(text).getUnclippedBoundsInRoot()
            assertTrue("$text is ${b.width} wide", b.width >= 80.dp && b.left >= 0.dp)
        }
    }

    @Test
    fun settings_toggleChangePlates_persists() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Change plates").performClick()

        compose.waitUntil(5_000) { runBlocking { settings.settings.first().changePlates } }
        assertTrue(runBlocking { settings.settings.first().changePlates })
    }

    @Test
    fun settings_restAlert_showsThreeChoicesAndPersists() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Rest alert").assertExists()
        compose.onNodeWithText("Vibrate only").assertIsSelected()
        compose.onNodeWithText("Off").assertExists()
        compose.onNodeWithText("Sound + vibrate").performClick()

        // Stored, and shown: the screen hears of the stored value on its own, a moment later, so both are waited for.
        val stored = { runBlocking { settings.settings.first().restAlert } }
        val selected = { listOf("Vibrate only", "Sound + vibrate", "Off").filter { exists(hasText(it) and androidx.compose.ui.test.isSelected()) } }
        waitOrSay({ "stored ${stored()}, selected on screen ${selected()}" }) {
            stored() == RestAlert.SOUND_AND_VIBRATE && exists(hasText("Sound + vibrate") and androidx.compose.ui.test.isSelected())
        }
        compose.onNodeWithText("Sound + vibrate").assertIsSelected()
    }

    /** Stage 2: always shown on Android, under the rest alert, on by default; the switch is stored. */
    @Test
    fun settings_vibrateOnTaps_isUnderRestAlert_onByDefault_andPersists() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        val row = compose.onNodeWithText("Vibrate on taps")
        row.assertIsOn()
        val alertBottom = compose.onNodeWithText("Vibrate only").getUnclippedBoundsInRoot().bottom
        assertTrue("under the rest alert", row.getUnclippedBoundsInRoot().top >= alertBottom)
        assertTrue("above app lock", row.getUnclippedBoundsInRoot().bottom <= compose.onNodeWithText("App lock").getUnclippedBoundsInRoot().top)

        row.performClick()
        compose.waitUntil(5_000) { !runBlocking { settings.settings.first().vibrateOnTaps } && exists(hasText("Vibrate on taps") and androidx.compose.ui.test.isOff()) }
        row.assertIsOff()
        assertEquals(RestAlert.VIBRATE, runBlocking { settings.settings.first().restAlert })

        row.performClick()
        compose.waitUntil(5_000) { runBlocking { settings.settings.first().vibrateOnTaps } }
    }

    @Test
    fun settings_appLockRow_saysWhatItDoes() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Ask for your fingerprint, face or screen lock when Grit opens, and after a minute away").assertExists()
    }

    @Test
    fun settings_copy_saysGrit() {
        assertEquals(RestoreCheck.Rejected("This file isn't a valid Grit backup."), checkBackupText("not json"))
        assertEquals(
            RestoreCheck.Rejected("This backup is from a newer version of Grit."),
            checkBackupText("""{"version":${Backup.VERSION + 1}}"""),
        )
        assertEquals("grit-backup-2026-11-11.json", backupFileName(LocalDate(2026, 11, 11)))
    }

    @Test
    fun baselineDialog_rejectsInfinityAndOverFiveHundred_andBackupStillEncodes() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("baseline-Squat")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("baseline-Squat").performClick()

        val field = compose.onNodeWithText("Estimated 1RM (kg)")
        for (text in listOf("Infinity", "1e400", "NaN", "501")) {
            field.performTextReplacement(text)
            compose.onNodeWithText("Save").assertIsNotEnabled()
        }
        field.performTextReplacement("182,5")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()

        compose.waitUntil(5_000) { runBlocking { training.history().first().baselines.isNotEmpty() } }
        assertEquals(listOf(182.5), runBlocking { training.history().first().baselines.map { it.e1rmKg } })
        // Every stored double is finite, so the strict JSON writer cannot fail on export.
        val text = runBlocking { Backup.encode(BackupService(db, settings).export(0L)) }
        assertTrue(text.contains("182.5"))
    }

    // ---- Strict press: the Progress switch and the known bests ----

    @Test
    fun settings_strictPressSwitch_saysWhatItDoes_andPersists() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Show it on Progress and add it to your total.").assertExists()
        compose.onNodeWithText("Strict press").performClick()
        compose.waitUntil(5_000) { runBlocking { settings.showStrictPress.first() } }

        compose.onNodeWithText("Strict press").performClick()
        compose.waitUntil(5_000) { !runBlocking { settings.showStrictPress.first() } }
    }

    @Test
    fun knownBests_includeStrictPress_onceItHasARow() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("baseline-Squat")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("baseline-Strict press").assertDoesNotExist()

        runBlocking { training.addExercise(Exercise(0, "Strict press", Category.STRICT_PRESS, null, emptyList(), isBuiltIn = true)) }

        // It is a main lift like the other three: it can be given a known best.
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("baseline-Strict press")).fetchSemanticsNodes().isNotEmpty() }
        for (lift in listOf("Squat", "Bench", "Deadlift")) compose.onNodeWithTag("baseline-$lift").assertExists()
    }

    // ---- The buzz (stage 2): adding to a day from the picker ticks once; changing an exercise does not ----

    @Test
    fun addToADay_ticksOnce_changeExercise_doesNotTick() {
        val haptics = FakeHapticFeedback()
        compose.setContent { WithHaptics(haptics) { PlanScreen(training, settings, onOpenSettings = {}) } }

        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("add-MONDAY")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("add-MONDAY").performClick()
        compose.onNodeWithTag("exercise-search").performTextReplacement("Squat")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("exercise-Squat")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, haptics.calls.size)
        compose.onNodeWithTag("exercise-Squat").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Add") and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty() }
        // Past the moment in which the new form ignores taps (the second tap of a double tap on the row).
        compose.mainClock.advanceTimeBy(ADD_FORM_TAP_WINDOW_MS)
        compose.onNode(hasText("Add") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Squat  3 × 5 @ 8").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf(HapticFeedbackType.ContextClick), haptics.calls.toList())

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithContentDescription("Change exercise").performClick()
        compose.onNodeWithTag("exercise-search").performTextReplacement("curl")
        compose.onNodeWithTag("exercise-Curl").performClick()
        compose.onNodeWithText("Edit Monday").assertExists()
        compose.waitForIdle()
        assertEquals(1, haptics.calls.size)
    }

    // ---- Choosing sets and reps when adding (1.12): picking opens the form; only Add stores ----

    /** Idles between checks, as ExercisePickerTest does: what is stored is read straight from the database. */
    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    /** [compose]'s 5 s wait for [condition]; should it time out, [state] is printed first, to say which half was missing. */
    private fun waitOrSay(state: () -> String, condition: () -> Boolean) {
        try {
            compose.waitUntil(5_000, condition)
        } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
            System.err.println("WAIT TIMED OUT: ${state()}")
            throw timeout
        }
    }

    private fun exists(matcher: androidx.compose.ui.test.SemanticsMatcher) = compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun plan(mode: TrainingMode = TrainingMode.POWERLIFTING) = runBlocking { training.plan(mode).first() }

    /** Opens Add on Monday and picks [name] from the list. */
    private fun pickOnMonday(name: String) {
        waitFor { exists(hasTestTag("add-MONDAY")) }
        compose.onNodeWithTag("add-MONDAY").performClick()
        compose.onNodeWithTag("exercise-search").performTextReplacement(name)
        waitFor { exists(hasTestTag("exercise-$name")) }
        compose.onNodeWithTag("exercise-$name").performClick()
    }

    /**
     * The add form is open, for [name], titled for Monday. Then, unless [stayInTheTapWindow], the clock moves past the
     * window in which the form ignores taps on Add, Cancel and outside (the second tap of a double tap on a row).
     */
    private fun assertAddForm(name: String, stayInTheTapWindow: Boolean = false) {
        waitFor { exists(hasText("Sets") and hasAnyAncestor(isDialog())) }
        compose.onNode(hasText("Add to Monday") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNode(hasText(name) and hasAnyAncestor(isDialog())).assertExists()
        addButton().assertExists()
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).assertDoesNotExist()
        if (!stayInTheTapWindow) pastTheTapWindow()
    }

    /** Moves the clock past the add form's tap window. */
    private fun pastTheTapWindow() {
        compose.mainClock.advanceTimeBy(ADD_FORM_TAP_WINDOW_MS)
        compose.waitForIdle()
    }

    private fun formOpen() = exists(hasText("Sets") and hasAnyAncestor(isDialog()))

    /**
     * A tap outside the form, above it. The dialog's window wraps the form, and being modal it is sent touches outside
     * its bounds too, as here: what a tap on the scrim does on a phone.
     */
    private fun tapOutside() {
        val view = compose.onNode(isDialog()).fetchSemanticsNode().root as android.view.View
        compose.runOnUiThread {
            val root = view.rootView
            val t = android.os.SystemClock.uptimeMillis()
            val x = root.width / 2f
            root.dispatchTouchEvent(android.view.MotionEvent.obtain(t, t, android.view.MotionEvent.ACTION_DOWN, x, -20f, 0))
            root.dispatchTouchEvent(android.view.MotionEvent.obtain(t, t + 10, android.view.MotionEvent.ACTION_UP, x, -20f, 0))
        }
        compose.waitForIdle()
    }

    /** The system back, sent to the dialog's window. */
    private fun pressBack() {
        val view = compose.onNode(isDialog()).fetchSemanticsNode().root as android.view.View
        compose.runOnUiThread {
            val root = view.rootView
            root.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK))
            root.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK))
        }
        compose.waitForIdle()
    }

    /** What the form's [label] stepper shows. */
    private fun assertField(label: String, value: String) {
        compose.onNode(hasText(value) and hasAnyAncestor(hasTestTag("plan-field-$label"))).assertExists()
    }

    private fun addButton() = compose.onNode(hasText("Add") and hasAnyAncestor(isDialog()))

    private fun tap(description: String) = compose.onNodeWithContentDescription(description).performClick()

    /** Runs [node]'s click twice in one go on the UI thread, as two taps landing in the same frame would. */
    private fun doubleTap(node: androidx.compose.ui.test.SemanticsNodeInteraction) {
        val click = node.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!
        compose.runOnUiThread {
            click()
            click()
        }
    }

    private fun squatId() = runBlocking { training.exercises().first() }.first { it.name == "Squat" }.id

    @Test
    fun planAdd_pick_opensFormWithDefaults() {
        runBlocking { settings.update { it.copy(defaultRpe = 7.5) } }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")

        assertAddForm("Squat")
        assertField("Sets", "3")
        assertField("Reps", "5")
        assertField("RPE", "7.5")
        compose.onNodeWithText("Reps from").assertDoesNotExist()
        compose.waitForIdle()
        assertTrue(plan().isEmpty())
    }

    @Test
    fun planAdd_changeNumbers_addStoresThem() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        tap("Increase Sets")
        tap("Increase Reps")
        tap("Decrease RPE")
        assertField("Sets", "4")
        assertField("Reps", "6")
        assertField("RPE", "7.5")
        compose.waitForIdle()
        assertTrue("nothing is stored before Add", plan().isEmpty())

        addButton().performClick()
        waitFor { plan().isNotEmpty() }
        assertEquals(listOf(PlanItem(0, DayOfWeek.MONDAY, squatId(), sets = 4, reps = 6, targetRpe = 7.5, order = 0)), plan().map { it.copy(id = 0) })
        // The row comes from the plan read again after the write, the closing from the write's own answer: two
        // moments, so both are waited for.
        waitFor { exists(hasText("Squat  4 × 6 @ 7.5")) && !formOpen() }
        compose.onNode(hasText("Sets") and hasAnyAncestor(isDialog())).assertDoesNotExist()
    }

    @Test
    fun planAdd_cancel_storesNothing() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        tap("Increase Sets")
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Sets") and hasAnyAncestor(isDialog())).assertDoesNotExist()
        assertTrue(plan().isEmpty())

        // Change, then Cancel: nothing either.
        pickOnMonday("Squat")
        assertAddForm("Squat")
        tap("Change exercise")
        compose.onNodeWithTag("exercise-search").performTextReplacement("curl")
        waitFor { exists(hasTestTag("exercise-Curl")) }
        compose.onNodeWithTag("exercise-Curl").performClick()
        waitFor { exists(hasText("Curl") and hasAnyAncestor(isDialog())) }
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertTrue(plan().isEmpty())
    }

    @Test
    fun planAdd_change_keepsTheNumbers_andAddStoresTheNewExercise() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        tap("Increase Sets")
        tap("Change exercise")
        compose.onNodeWithTag("exercise-search").performTextReplacement("curl")
        waitFor { exists(hasTestTag("exercise-Curl")) }
        compose.onNodeWithTag("exercise-Curl").performClick()
        waitFor { exists(hasText("Curl") and hasAnyAncestor(isDialog())) }
        // What was typed is not overwritten by the new exercise's ready-made numbers.
        assertField("Sets", "4")
        assertField("Reps", "5")

        addButton().performClick()
        waitFor { plan().isNotEmpty() }
        val curl = runBlocking { training.exercises().first() }.single { it.name == "Curl" }
        assertEquals(listOf(Triple(curl.id, 4, 5)), plan().map { Triple(it.exerciseId, it.sets, it.reps) })
    }

    @Test
    fun planAdd_bodybuilding_storesRange() {
        runBlocking { settings.update { com.powertracker.data.AppSettings(trainingMode = TrainingMode.BODYBUILDING) } }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Curl")

        assertAddForm("Curl")
        assertField("Sets", "3")
        assertField("Reps from", "8")
        assertField("Reps to", "12")
        assertField("RPE", "8")
        tap("Decrease Reps from")
        tap("Increase Reps to")
        tap("Increase Reps to")
        compose.waitForIdle()
        assertTrue(plan(TrainingMode.BODYBUILDING).isEmpty())

        addButton().performClick()
        waitFor { plan(TrainingMode.BODYBUILDING).isNotEmpty() }
        val item = plan(TrainingMode.BODYBUILDING).single()
        assertEquals(listOf(3, 7, 14), listOf(item.sets, item.reps, item.repsHigh))
        assertEquals(8.0, item.targetRpe, 0.0)
        assertEquals(DayOfWeek.MONDAY, item.weekday)
        assertTrue(plan(TrainingMode.POWERLIFTING).isEmpty())
        waitFor { exists(hasText("Curl  3 × 7–14 @ 8")) }
    }

    @Test
    fun planAdd_ownExercise_opensForm() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        fun create(name: String) {
            waitFor { exists(hasTestTag("add-MONDAY")) }
            compose.onNodeWithTag("add-MONDAY").performClick()
            compose.onNodeWithTag("exercise-search").performTextReplacement("zzz")
            compose.onNodeWithText("Create your own").performClick()
            compose.onNodeWithText("Name").performTextReplacement(name)
            compose.onNodeWithText("Create").performClick()
        }
        fun rows(name: String) = runBlocking { db.exercises().all() }.filter { it.name == name }

        // Created, then Cancel: it stays in the library, and nothing is added.
        create("Tyre flip")
        assertAddForm("Tyre flip")
        assertEquals(1, rows("Tyre flip").size)
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertEquals(1, rows("Tyre flip").size)
        assertTrue(plan().isEmpty())

        // Created, then Add: stored with the chosen numbers.
        create("Sled push")
        assertAddForm("Sled push")
        tap("Increase Sets")
        addButton().performClick()
        waitFor { plan().isNotEmpty() }
        val item = plan().single()
        assertEquals(rows("Sled push").single().id, item.exerciseId)
        assertEquals(4, item.sets)
    }

    @Test
    fun planAdd_doubleTapAdd_storesOne() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")

        doubleTap(addButton())
        waitFor { plan().isNotEmpty() }
        compose.waitForIdle()
        assertEquals(1, plan().size)
    }

    @Test
    fun planAdd_survivesRecreation() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        tap("Increase Sets")
        tap("Increase Sets")
        assertField("Sets", "5")

        restoration.emulateSavedInstanceStateRestore()

        assertAddForm("Squat")
        assertField("Sets", "5")
        assertField("Reps", "5")
        compose.waitForIdle()
        assertTrue(plan().isEmpty())
        addButton().performClick()
        waitFor { plan().isNotEmpty() }
        assertEquals(listOf(5 to 5), plan().map { it.sets to it.reps })
        assertEquals(squatId(), plan().single().exerciseId)
    }

    @Test
    fun planAdd_ticksOnceWhenStored() {
        val haptics = FakeHapticFeedback()
        compose.setContent { WithHaptics(haptics) { PlanScreen(training, settings, onOpenSettings = {}) } }

        // Picking, stepping and Cancel are quiet.
        pickOnMonday("Squat")
        assertAddForm("Squat")
        tap("Increase Sets")
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertEquals(0, haptics.calls.size)

        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.waitForIdle()
        assertEquals(0, haptics.calls.size)
        addButton().performClick()
        waitFor { plan().isNotEmpty() }
        waitFor { haptics.calls.isNotEmpty() }
        compose.waitForIdle()
        assertEquals(listOf(HapticFeedbackType.ContextClick), haptics.calls.toList())
    }

    // ---- The second tap of a double tap on a picker row does not reach the new form ----

    @Test
    fun planAdd_secondTapOfADoubleTap_isIgnored() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        val opened = compose.mainClock.currentTime
        assertAddForm("Squat", stayInTheTapWindow = true)

        // A tap on Add, on Cancel, or outside the form, landing just after it opened: nothing happens.
        addButton().performClick()
        compose.waitForIdle()
        assertTrue("Add is ignored", formOpen())
        assertTrue(plan().isEmpty())
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertTrue("Cancel is ignored", formOpen())
        tapOutside()
        assertTrue("a tap outside is ignored", formOpen())

        // The steppers stay live; Add by its accessibility action (not a tap) and back are not held either.
        tap("Increase Sets")
        assertField("Sets", "4")
        pressBack()
        assertTrue("back closes the form", !formOpen())
        assertTrue("all of this inside the window", compose.mainClock.currentTime - opened < ADD_FORM_TAP_WINDOW_MS)
        assertTrue(plan().isEmpty())

        pickOnMonday("Squat")
        assertAddForm("Squat", stayInTheTapWindow = true)
        addButton().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        waitFor { plan().isNotEmpty() }
        assertEquals(1, plan().size)
    }

    @Test
    fun planAdd_tapAfterTheWindow_works() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat", stayInTheTapWindow = true)
        addButton().performClick()
        compose.waitForIdle()
        assertTrue(formOpen())

        // Once the moment has passed, Add stores.
        pastTheTapWindow()
        addButton().performClick()
        // Stored, and closed: the form closes when the screen hears back from the write, a moment after the row is
        // in the database, so both are waited for.
        waitFor { plan().isNotEmpty() && !formOpen() }
        assertEquals(listOf(3 to 5), plan().map { it.sets to it.reps })
        assertTrue(!formOpen())

        // And a tap outside closes it, storing nothing more.
        pickOnMonday("Squat")
        assertAddForm("Squat", stayInTheTapWindow = true)
        tapOutside()
        assertTrue(formOpen())
        pastTheTapWindow()
        tapOutside()
        waitFor { !formOpen() }
        assertEquals(1, plan().size)
    }

    // ---- Review gaps ----

    /** Makes every insert into the exercise table fail, as a full disk or a broken database would: the throw the real path meets. */
    private fun failExerciseInserts() = db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_exercise_insert BEFORE INSERT ON exercise BEGIN SELECT RAISE(ABORT, 'disk full'); END",
    )

    /** In the open picker: searches for nothing, then creates "Tyre flip" with "Create your own". */
    private fun createTyreFlip() {
        compose.onNodeWithTag("exercise-search").performTextReplacement("zzz")
        compose.onNodeWithText("Create your own").performClick()
        compose.onNodeWithText("Name").performTextReplacement("Tyre flip")
        compose.onNodeWithText("Create").performClick()
    }

    /** The failed save is said; that is also the moment the failure has been handled. */
    private fun waitForSaveFailed() = waitFor { exists(hasText(SAVE_FAILED)) }

    private fun tyreFlipStored() = runBlocking { db.exercises().all() }.any { it.name == "Tyre flip" }

    private fun allowExerciseInserts() = db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_exercise_insert")

    /** In the open picker: searches for nothing and opens "Create your own", without creating. */
    private fun openCreateForm() {
        compose.onNodeWithTag("exercise-search").performTextReplacement("zzz")
        compose.onNodeWithText("Create your own").performClick()
        waitFor { compose.createFormOpen() }
    }

    private fun tapCreate() = compose.onNode(hasText("Create") and hasAnyAncestor(hasTestTag("custom-exercise-form"))).performClick()

    private fun closeCreateFormAndPicker() {
        compose.onNode(hasText("Cancel") and hasAnyAncestor(hasTestTag("custom-exercise-form"))).performClick()
        waitFor { !compose.createFormOpen() }
        compose.onNodeWithContentDescription("Close").performClick()
        waitFor { !exists(hasTestTag("exercise-search")) }
    }

    /** The exercise [fillCustomExercise] describes was stored, with everything typed. */
    private fun assertOwnStored() {
        val own = runBlocking { db.exercises().all() }.single { it.name == OWN_NAME }
        assertEquals(Category.VARIATION, own.category)
        assertEquals(com.powertracker.domain.model.MainLift.BENCH, own.parentLift)
        assertEquals(listOf("Lats", "Grip"), own.muscles)
    }

    @Test
    fun planAdd_ownExerciseFails_createFormKeepsEverything_saysSoOverIt_andASecondCreateWorks() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        waitFor { exists(hasTestTag("add-MONDAY")) }
        compose.onNodeWithTag("add-MONDAY").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        tapCreate()
        waitForSaveFailed()

        // Back in the create form with everything typed, the message over it, and nothing stored.
        compose.assertCustomExerciseKept()
        compose.assertSaveFailedOverTheForm()
        assertTrue(!formOpen())
        assertTrue(!tyreFlipStored())

        // Once the disk has room, Create works again: the exercise is stored as typed and the add form opens on it.
        allowExerciseInserts()
        tapCreate()
        assertAddForm(OWN_NAME)
        assertTrue(!compose.createFormOpen())
        assertOwnStored()
    }

    @Test
    fun planAdd_ownExerciseCreated_theNextCreateFormStartsEmpty() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        waitFor { exists(hasTestTag("add-MONDAY")) }
        compose.onNodeWithTag("add-MONDAY").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        tapCreate()
        assertAddForm(OWN_NAME)
        pastTheTapWindow()
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        waitFor { !formOpen() }

        compose.onNodeWithTag("add-MONDAY").performClick()
        openCreateForm()
        compose.assertCustomExerciseEmpty("zzz")
    }

    @Test
    fun planAdd_changeToOwnExerciseFails_createFormKeepsEverything_evenAfterRecreation() {
        failExerciseInserts()
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        tapCreate()
        waitForSaveFailed()
        compose.assertCustomExerciseKept()
        compose.assertSaveFailedOverTheForm()

        // The add form survives a rotation, and so do its picker and the create form, with what was typed.
        restoration.emulateSavedInstanceStateRestore()
        waitFor { compose.createFormOpen() }
        compose.assertCustomExerciseKept()

        allowExerciseInserts()
        tapCreate()
        waitFor { !compose.createFormOpen() && !exists(hasTestTag("exercise-search")) }
        assertAddForm(OWN_NAME)
        assertOwnStored()
        assertTrue(plan().isEmpty())
    }

    @Test
    fun planAdd_ownExerciseFails_saysSo_opensNoForm() {
        // A failed insert must not escape the view model's coroutine (on a phone that crashes the app): it is said.
        failExerciseInserts()
        val haptics = FakeHapticFeedback()
        compose.setContent { WithHaptics(haptics) { PlanScreen(training, settings, onOpenSettings = {}) } }
        waitFor { exists(hasTestTag("add-MONDAY")) }
        compose.onNodeWithTag("add-MONDAY").performClick()
        createTyreFlip()
        waitForSaveFailed()

        // No form opened, nothing was stored, and nothing buzzed.
        assertTrue(!formOpen())
        addButton().assertDoesNotExist()
        assertTrue(plan().isEmpty())
        assertTrue(!tyreFlipStored())
        assertEquals(0, haptics.calls.size)
    }

    @Test
    fun planAdd_changeToOwnExerciseFails_saysSo_formKeepsItsExercise() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        createTyreFlip()
        waitForSaveFailed()

        // The add form is still on Squat, nothing was stored, and its Add still stores Squat.
        assertAddForm("Squat")
        assertTrue(plan().isEmpty())
        assertTrue(!tyreFlipStored())
        // Since 1.12.2 the create form and the picker stay open after the failure: close both to reach the add form.
        closeCreateFormAndPicker()
        compose.onNode(hasText("Tyre flip") and hasAnyAncestor(isDialog())).assertDoesNotExist()
        addButton().performClick()
        waitFor { plan().isNotEmpty() }
        assertEquals(squatId(), plan().single().exerciseId)
    }

    @Test
    fun planEdit_changeToOwnExerciseFails_saysSo_rowKeepsItsExercise() {
        val squat = squatId()
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, squat, sets = 3, reps = 5, targetRpe = 8.0, order = 0)) }
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        waitFor { exists(hasText("Squat  3 × 5 @ 8")) }
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithContentDescription("Change exercise").performClick()
        createTyreFlip()
        waitForSaveFailed()

        // The edit form is still on Squat, and the row is as it was.
        compose.onNodeWithText("Edit Monday").assertExists()
        compose.onNode(hasText("Squat") and hasAnyAncestor(isDialog())).assertExists()
        assertTrue(!tyreFlipStored())
        assertEquals(listOf(squat), plan().map { it.exerciseId })
    }

    @Test
    fun planAdd_addFails_saysSo_formKeepsItsNumbers_andASecondAddWorks() {
        // Every plan item write is refused, as a full disk would refuse it.
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_plan_insert BEFORE INSERT ON plan_item BEGIN SELECT RAISE(ABORT, 'disk full'); END",
        )
        val haptics = FakeHapticFeedback()
        compose.setContent { WithHaptics(haptics) { PlanScreen(training, settings, onOpenSettings = {}) } }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        tap("Increase Sets")
        tap("Increase Reps")
        assertField("Sets", "4")
        assertField("Reps", "6")
        addButton().performClick()
        waitForSaveFailed()

        // The form is still open on Squat with the numbers set, nothing was stored, and nothing buzzed.
        assertAddForm("Squat")
        assertField("Sets", "4")
        assertField("Reps", "6")
        assertTrue(plan().isEmpty())
        assertEquals(0, haptics.calls.size)

        // Once the disk has room, a second Add stores those numbers, once, closes the form and ticks.
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_plan_insert")
        addButton().performClick()
        waitFor { plan().isNotEmpty() && !formOpen() }
        val item = plan().single()
        assertEquals(squatId(), item.exerciseId)
        assertEquals(4, item.sets)
        assertEquals(6, item.reps)
        assertEquals(listOf(HapticFeedbackType.ContextClick), haptics.calls.toList())
    }

    // ---- 1.12.2 review: a failure is said where the lifter is looking ----

    /** The click of the node [matcher] finds, to run later (several in one go, as taps landing in one frame). */
    private fun click(matcher: androidx.compose.ui.test.SemanticsMatcher): () -> Boolean =
        compose.onNode(matcher).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!

    private val createInForm = hasText("Create") and hasAnyAncestor(hasTestTag("custom-exercise-form"))
    private val cancelInForm = hasText("Cancel") and hasAnyAncestor(hasTestTag("custom-exercise-form"))
    private val closePicker = androidx.compose.ui.test.hasContentDescription("Close") and hasAnyAncestor(isDialog())

    /** Taps Create in the filled create form and, before the save can answer, each of [then]. */
    private fun createThen(vararg then: androidx.compose.ui.test.SemanticsMatcher) {
        val clicks = (listOf(createInForm) + then).map(::click)
        compose.runOnUiThread { clicks.forEach { it() } }
    }

    /**
     * The message, in a window of its own over whatever dialog is open (not behind it on the screen). Waited for at
     * once, not after Plan's own: it is on screen only for the snackbar's few seconds.
     */
    private fun saidOverTheDialog() = exists(hasText(SAVE_FAILED) and hasAnyAncestor(androidx.compose.ui.test.isPopup()))

    private fun pickerOpen() = exists(hasTestTag("exercise-search"))

    /** Where [SAVE_FAILED] is now, and which dialogs are open: over a dialog in a popup, Plan's snackbar, or nowhere. */
    private fun whereTheMessageIs(): String {
        val inPopup = compose.onAllNodes(hasText(SAVE_FAILED) and hasAnyAncestor(androidx.compose.ui.test.isPopup())).fetchSemanticsNodes().size
        val inDialog = compose.onAllNodes(hasText(SAVE_FAILED) and hasAnyAncestor(isDialog())).fetchSemanticsNodes().size
        val anywhere = compose.onAllNodes(hasText(SAVE_FAILED)).fetchSemanticsNodes().size
        val message = when {
            anywhere == 0 -> "nowhere"
            inPopup > 0 -> "in a popup (x$inPopup)" + if (anywhere > inPopup) ", and ${anywhere - inPopup} more elsewhere" else ""
            inDialog > 0 -> "inside a dialog (x$inDialog), not in a popup"
            else -> "in Plan's snackbar only (x$anywhere)"
        }
        val dialogs = buildList {
            if (pickerOpen()) add("picker")
            if (formOpen()) add("add form")
            if (compose.createFormOpen()) add("create form")
            val count = compose.onAllNodes(isDialog()).fetchSemanticsNodes().size
            if (count > size) add("${count - size} other")
        }
        return "message $message; dialogs open: ${dialogs.ifEmpty { listOf("none") }.joinToString()}"
    }

    /**
     * [waitFor], and should it time out, prints before rethrowing: each change during the wait (wall and test clock
     * times), [whereTheMessageIs], the longest gap between two checks, the database work still running and its
     * threads, and the semantics tree. So a failure says where the message went, and whether the test stalled (few
     * checks, a long gap), the test clock leapt (far more than 16 ms a check), or the failure never came back.
     */
    private fun waitRecording(condition: () -> Boolean) {
        val wallStart = System.nanoTime()
        val clockStart = compose.mainClock.currentTime
        val timeline = mutableListOf<String>()
        var last = ""
        var polls = 0
        var lastPoll = wallStart
        var longestGap = 0L
        try {
            waitFor {
                val now = System.nanoTime()
                longestGap = maxOf(longestGap, now - lastPoll)
                lastPoll = now
                polls++
                condition().also { met ->
                    // Cheap, so the wait idles as the plain one does: the full picture is taken at the timeout.
                    val where = "condition $met, message anywhere ${exists(hasText(SAVE_FAILED))}, database work in flight ${database.inFlight}"
                    if (where != last) {
                        timeline += "  +${(now - wallStart) / 1_000_000} ms wall, +${compose.mainClock.currentTime - clockStart} ms test clock, check $polls: $where"
                        last = where
                    }
                }
            }
        } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
            val report = buildString {
                appendLine("WAIT TIMED OUT after $polls checks, ${(System.nanoTime() - wallStart) / 1_000_000} ms wall, ${compose.mainClock.currentTime - clockStart} ms test clock; longest gap between checks ${longestGap / 1_000_000} ms")
                appendLine("now: ${whereTheMessageIs()}; Tyre flip stored: ${tyreFlipStored()}; database work in flight: ${database.inFlight}")
                timeline.forEach(::appendLine)
                Thread.getAllStackTraces().filter { it.key.name == "test-database" }.forEach { (thread, stack) ->
                    appendLine("thread ${thread.name} ${thread.state}:")
                    stack.take(25).forEach { appendLine("    at $it") }
                }
            }
            System.err.println(report)
            val roots = compose.onAllNodes(androidx.compose.ui.test.isRoot())
            roots.fetchSemanticsNodes().indices.forEach { System.err.println(roots[it].printToString(maxDepth = Int.MAX_VALUE)) }
            throw timeout
        }
    }

    @Test
    fun planAdd_addFails_saysSoOverTheAddForm_whileItStaysOpen() {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_plan_insert BEFORE INSERT ON plan_item BEGIN SELECT RAISE(ABORT, 'disk full'); END",
        )
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        addButton().performClick()

        // Plan's own snackbar is under the form's scrim: the form says it too, from its own window.
        waitFor { saidOverTheDialog() }
        assertAddForm("Squat")
        assertTrue(plan().isEmpty())
    }

    /**
     * A failed Add is said once, over the add form, and not by Plan's own snackbar under it as well: every copy anywhere
     * is counted, frame by frame, while the message runs its course (1.15, as Change already does).
     */
    @Test
    fun planAdd_addFails_saidOnceOverTheForm_notByPlanToo() {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_plan_insert BEFORE INSERT ON plan_item BEGIN SELECT RAISE(ABORT, 'disk full'); END",
        )
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        addButton().performClick()
        waitFor { copies() > 0 }
        val first = copyNodes()
        assertEquals("one copy (${whereTheMessageIs()})", 1, first.size)
        assertTrue("over the add form", inAPopup(first.single()))
        compose.mainClock.autoAdvance = false
        try {
            var appearances = 1
            var most = 1
            var lastIds = first.map { it.id }.toSet()
            repeat(6_000 / 16) {
                compose.mainClock.advanceTimeByFrame()
                val nodes = copyNodes()
                most = maxOf(most, nodes.size)
                val ids = nodes.map { it.id }.toSet()
                appearances += (ids - lastIds).size
                lastIds = ids
            }
            assertEquals("never two at once", 1, most)
            assertEquals("said once", 1, appearances)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        assertAddForm("Squat")
        assertTrue(plan().isEmpty())
    }

    /** An Add that fails once the form has gone (Cancel while it was saving): nobody else can say it, so Plan does, once. */
    @Test
    fun planAdd_addFails_afterTheFormWasCancelled_planSaysSoOnce() {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_plan_insert BEFORE INSERT ON plan_item BEGIN SELECT RAISE(ABORT, 'disk full'); END",
        )
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat", stayInTheTapWindow = false)
        val release = database.holdWork()
        try {
            val add = click(hasText("Add") and hasAnyAncestor(isDialog()))
            val cancel = click(hasText("Cancel") and hasAnyAncestor(isDialog()))
            compose.runOnUiThread {
                add()
                cancel()
            }
            waitFor { !exists(isDialog()) }
        } finally {
            release()
        }
        waitFor { copies() == 1 }
        val node = copyNodes().single()
        assertTrue("in Plan's own snackbar", !inAPopup(node))
        compose.mainClock.autoAdvance = false
        try {
            var appearances = 1
            var lastIds = setOf(node.id)
            repeat(6_000 / 16) {
                compose.mainClock.advanceTimeByFrame()
                val ids = copyNodes().map { it.id }.toSet()
                appearances += (ids - lastIds).size
                lastIds = ids
            }
            assertEquals("Plan says it once", 1, appearances)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        assertTrue(plan().isEmpty())
    }

    @Test
    fun planEdit_saveFails_formClosed_planSaysSo() {
        val squat = squatId()
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, squat, sets = 3, reps = 5, targetRpe = 8.0, order = 0)) }
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_plan_insert BEFORE INSERT ON plan_item BEGIN SELECT RAISE(ABORT, 'disk full'); END",
        )
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        waitFor { exists(hasText("Squat  3 × 5 @ 8")) }
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        tap("Increase Sets")
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()
        waitForSaveFailed()

        // Save closes the form at once, so Plan's own snackbar is in plain view; the row is as it was.
        assertTrue(!exists(isDialog()))
        assertEquals(listOf(3), plan().map { it.sets })
    }

    @Test
    fun planAdd_ownExerciseFails_afterTheFormWasCancelled_thePickerSaysSo() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        waitFor { exists(hasTestTag("add-MONDAY")) }
        compose.onNodeWithTag("add-MONDAY").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        createThen(cancelInForm)

        // The full-screen picker hides Plan's snackbar: the picker says it over its list.
        waitFor { saidOverTheDialog() }
        assertTrue(pickerOpen())
        assertTrue(!compose.createFormOpen())
        assertTrue(!tyreFlipStored())
    }

    @Test
    fun planAdd_ownExerciseFails_afterThePickerWasClosed_planSaysSo() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        waitFor { exists(hasTestTag("add-MONDAY")) }
        compose.onNodeWithTag("add-MONDAY").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        createThen(cancelInForm, closePicker)
        waitForSaveFailed()

        // Nothing is open over Plan: its own snackbar says it.
        assertTrue(!exists(isDialog()))
        assertTrue(!tyreFlipStored())
    }

    @Test
    fun planAdd_changeToOwnExerciseFails_afterThePickerWasClosed_theAddFormSaysSo() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        createThen(cancelInForm, closePicker)

        // Back on the add form, whose scrim hides Plan's snackbar: the add form says it.
        waitRecording { saidOverTheDialog() }
        assertTrue(!pickerOpen())
        assertAddForm("Squat")
        assertTrue(!tyreFlipStored())
    }

    /**
     * The create form cancelled and gone, the failure lands and Close is tapped before the next frame: the picker counted
     * it but is never drawn again to say it, so the add form must.
     */
    @Test
    fun planAdd_changeToOwnExerciseFails_inTheFrameThePickerCloses_theAddFormSaysSo() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        val release = database.holdWork()
        try {
            compose.onNode(createInForm).performClick()
            compose.onNode(cancelInForm).performClick()
            waitFor { !compose.createFormOpen() && pickerOpen() }
            val close = click(closePicker)
            compose.runOnUiThread {
                release()
                // The failed insert hands its answer to the main thread; it is handled there, then Close, with no frame between.
                while (database.inFlight > 0) Thread.sleep(1)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                close()
            }
        } finally {
            release()
        }

        waitRecording { saidOverTheDialog() }
        assertTrue(!pickerOpen())
        assertAddForm("Squat")
        assertTrue(!tyreFlipStored())
    }

    /**
     * The insert answers before the Create tap has returned (the database's thread was quicker than the main one), so
     * the failure is handled while the create form is still open; then Cancel and Close, before any frame. The forms that
     * were open at the failure are never drawn again: the add form, the one left, says it. The order the 2026-11-18 and
     * 2026-11-19 failures of the test above were caught in.
     */
    @Test
    fun planAdd_changeToOwnExerciseFails_answeredWithinTheCreateTap_thenThePickerClosed_theAddFormSaysSo() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        database.answerAtOnce = true
        try {
            createThen(cancelInForm, closePicker)
        } finally {
            database.answerAtOnce = false
        }

        waitRecording { saidOverTheDialog() }
        assertTrue(!pickerOpen())
        assertAddForm("Squat")
        assertTrue(!tyreFlipStored())
    }

    @Test
    fun planAdd_changeToOwnExerciseFails_afterTheFormWasCancelled_thePickerSaysSo() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        createThen(cancelInForm)

        // Change's picker is still open over the add form: the picker says it over its list.
        waitFor { saidOverTheDialog() }
        assertTrue(pickerOpen())
        assertTrue(!compose.createFormOpen())
        assertTrue(!tyreFlipStored())
    }

    // ---- A failed own exercise from Change is held by the add form until it has been said (1.15) ----

    /** Every copy of [SAVE_FAILED] on the screen now: in a popup over a dialog, or Plan's own snackbar under them. */
    private fun copyNodes() = compose.onAllNodes(hasText(SAVE_FAILED)).fetchSemanticsNodes()

    private fun copies() = copyNodes().size

    private fun inAPopup(node: androidx.compose.ui.semantics.SemanticsNode): Boolean {
        var at: androidx.compose.ui.semantics.SemanticsNode? = node
        while (at != null) {
            if (at.config.contains(androidx.compose.ui.semantics.SemanticsProperties.IsPopup)) return true
            at = at.parent
        }
        return false
    }

    /**
     * What [sayChangeFailure] saw: where the message was just before Close and whether the picker was saying it then, the
     * copies in each frame after it (anywhere, Plan's snackbar included), and how many snackbars came on screen after it
     * (a message said again is a new one, even straight after the last).
     */
    private class Sayings(
        val beforeClose: String,
        val pickerSaidItBeforeClose: Boolean,
        val frames: List<Int>,
        val wrongPlace: List<String>,
        val appearances: Int,
    )

    /**
     * Change's picker, its create form cancelled, makes the own exercise; the failure is handled on the main thread
     * with no frame drawn, then [frames] frames pass and Close is tapped, then the clock steps frame by frame for 6 s
     * (past a snackbar's 4 s). Each frame with the message on screen must have it over the add form, the picker gone.
     */
    private fun sayChangeFailure(frames: Int): Sayings {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        val release = database.holdWork()
        val close = try {
            compose.onNode(createInForm).performClick()
            compose.onNode(cancelInForm).performClick()
            waitFor { !compose.createFormOpen() && pickerOpen() }
            click(closePicker)
        } catch (e: Throwable) {
            release()
            throw e
        }
        // Close, then the main looper runs what Close posted (the app's own apply of its writes), as on a phone before the
        // next frame. With the clock stepped by hand that work would otherwise wait, so the next frame would still draw
        // the picker: a step no phone takes.
        val closeNow = {
            close()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnUiThread {
                release()
                while (database.inFlight > 0) Thread.sleep(1)
                // The failure is handled here, on the main thread, with no frame drawn.
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (frames == 0) closeNow()
            }
            repeat(frames) { compose.mainClock.advanceTimeByFrame() }
            val beforeClose = whereTheMessageIs()
            val pickerSaidIt = pickerOpen() && copyNodes().let { nodes -> nodes.size == 1 && inAPopup(nodes.single()) }
            if (frames > 0) compose.runOnUiThread { closeNow() }
            val seen = mutableListOf<Int>()
            val wrongPlace = mutableListOf<String>()
            // A copy is a new appearance when its node was not on screen the frame before. Ids can be handed out again
            // later, so they are only compared with the last frame, never with every frame so far.
            var appearances = 0
            var lastIds = emptySet<Int>()
            repeat(6_000 / 16) { frame ->
                compose.mainClock.advanceTimeByFrame()
                val nodes = copyNodes()
                val copies = nodes.size
                seen += copies
                val ids = nodes.map { it.id }.toSet()
                appearances += (ids - lastIds).size
                lastIds = ids
                if (copies > 0 && (pickerOpen() || !formOpen() || nodes.any { !inAPopup(it) })) wrongPlace += "frame $frame: ${whereTheMessageIs()}"
            }
            println("Close $frames frame(s) after the failure; before Close: $beforeClose; copies per frame after: ${seen.joinToString("")}")
            return Sayings(beforeClose, pickerSaidIt, seen, wrongPlace, appearances)
        } finally {
            release()
            compose.mainClock.autoAdvance = true
        }
    }

    private fun assertSaidOnceOverTheForm(said: Sayings) {
        assertEquals("over a dialog only once the picker has gone, over the add form: ${said.wrongPlace}", emptyList<String>(), said.wrongPlace)
        assertEquals("one copy at a time (before Close: ${said.beforeClose})", 1, said.frames.max())
        assertEquals("said once over the add form, never twice (before Close: ${said.beforeClose})", 1, said.appearances)
        assertTrue(!pickerOpen())
        assertAddForm("Squat")
        assertTrue(!tyreFlipStored())
    }

    @Test
    fun planAdd_changeToOwnExerciseFails_closedInTheSameFrame_theAddFormSaysItOnce() = assertSaidOnceOverTheForm(sayChangeFailure(frames = 0))

    @Test
    fun planAdd_changeToOwnExerciseFails_closedOneFrameLater_theAddFormSaysItOnce() = assertSaidOnceOverTheForm(sayChangeFailure(frames = 1))

    @Test
    fun planAdd_changeToOwnExerciseFails_closedTwoFramesLater_theAddFormSaysItOnce() = assertSaidOnceOverTheForm(sayChangeFailure(frames = 2))

    /** The picker was saying it and is closed mid-message: the add form says it, once, as it does for the orders above. */
    @Test
    fun planAdd_changeToOwnExerciseFails_closedWhileThePickerSaysIt_theAddFormSaysItOnce() {
        val said = sayChangeFailure(frames = 60)
        assertTrue("the picker was saying it when Close came (before Close: ${said.beforeClose})", said.pickerSaidItBeforeClose)
        assertSaidOnceOverTheForm(said)
    }

    /** The picker stays open and says it through; closed after that, the add form has nothing left to say. */
    @Test
    fun planAdd_changeToOwnExerciseFails_thePickerSaidItThrough_theAddFormStaysSilent() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        createThen(cancelInForm)
        waitFor { saidOverTheDialog() }
        assertTrue(pickerOpen())
        assertEquals(1, copies())
        // The picker's message runs its course (4 s) and goes.
        compose.mainClock.advanceTimeBy(5_000)
        waitFor { copies() == 0 }

        compose.onNode(closePicker).performClick()
        waitFor { !pickerOpen() }
        compose.mainClock.autoAdvance = false
        try {
            repeat(6_000 / 16) {
                compose.mainClock.advanceTimeByFrame()
                assertEquals("the add form says nothing more", 0, copies())
            }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        assertAddForm("Squat")
    }

    /** For 6 s (past a snackbar's 4 s), stepped frame by frame: no copy of the message anywhere, Plan's snackbar included. */
    private fun assertNothingSaidFor6s(why: String) {
        compose.mainClock.autoAdvance = false
        try {
            repeat(6_000 / 16) { frame ->
                compose.mainClock.advanceTimeByFrame()
                assertEquals("$why (frame $frame: ${whereTheMessageIs()})", 0, copies())
            }
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    /** Change's create form fails and says so; Create again works within the message's 4 s. Nothing failed is said after. */
    @Test
    fun planAdd_changeToOwnExerciseFails_thenASecondCreateWorks_theAddFormSaysNothing() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        tapCreate()
        waitFor { saidOverTheDialog() }
        val failedAt = compose.mainClock.currentTime
        assertTrue(compose.createFormOpen())

        allowExerciseInserts()
        tapCreate()
        waitFor { !pickerOpen() && exists(hasText(OWN_NAME) and hasAnyAncestor(isDialog())) }
        assertTrue("the create worked within the failure's 4 s", compose.mainClock.currentTime - failedAt < 4_000)
        assertNothingSaidFor6s("the add form, on the exercise just made, says no failure")
        assertTrue(formOpen())
        assertTrue(exists(hasText(OWN_NAME) and hasAnyAncestor(isDialog())))
    }

    /** Change's create form fails and says so; Cancel, then a row of the list is picked within the 4 s. Nothing is said after. */
    @Test
    fun planAdd_changeToOwnExerciseFails_thenARowIsPicked_theAddFormSaysNothing() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        tapCreate()
        waitFor { saidOverTheDialog() }
        val failedAt = compose.mainClock.currentTime
        compose.onNode(cancelInForm).performClick()
        waitFor { !compose.createFormOpen() }

        compose.onNodeWithTag("exercise-search").performTextReplacement("curl")
        waitFor { exists(hasTestTag("exercise-Curl")) }
        compose.onNodeWithTag("exercise-Curl").performClick()
        waitFor { !pickerOpen() && exists(hasText("Curl") and hasAnyAncestor(isDialog())) }
        assertTrue("the pick came within the failure's 4 s", compose.mainClock.currentTime - failedAt < 4_000)
        assertNothingSaidFor6s("the add form, on the exercise just picked, says no failure")
        assertTrue(formOpen())
        assertTrue(!tyreFlipStored())
    }

    /** The failure lands, then Close and the add form's Cancel, with no frame between: Plan's own snackbar says it, once. */
    @Test
    fun planAdd_changeToOwnExerciseFails_theAddFormClosedFirst_planSaysSo() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        val release = database.holdWork()
        try {
            compose.onNode(createInForm).performClick()
            compose.onNode(cancelInForm).performClick()
            waitFor { !compose.createFormOpen() && pickerOpen() }
            val close = click(closePicker)
            val cancel = click(hasText("Cancel") and hasAnyAncestor(isDialog()))
            compose.runOnUiThread {
                release()
                while (database.inFlight > 0) Thread.sleep(1)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                close()
                cancel()
            }
        } finally {
            release()
        }

        waitFor { !exists(isDialog()) && copies() == 1 }
        val node = copyNodes().single()
        assertTrue("in Plan's own snackbar", !inAPopup(node))
        compose.mainClock.autoAdvance = false
        try {
            var appearances = 1
            var lastIds = setOf(node.id)
            repeat(6_000 / 16) {
                compose.mainClock.advanceTimeByFrame()
                val ids = copyNodes().map { it.id }.toSet()
                appearances += (ids - lastIds).size
                lastIds = ids
            }
            assertEquals("Plan says it once", 1, appearances)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        assertTrue(plan().isEmpty())
    }

    /** Plan's Add: the create form says it, then Cancel and Close inside its 4 s. Plan's own snackbar does not say it again. */
    @Test
    fun planAdd_ownExerciseFails_saidByTheCreateForm_thenClosed_planDoesNotRepeatIt() {
        failExerciseInserts()
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        waitFor { exists(hasTestTag("add-MONDAY")) }
        compose.onNodeWithTag("add-MONDAY").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        tapCreate()
        waitFor { saidOverTheDialog() }
        assertEquals("over the create form only", 1, copies())
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNode(cancelInForm).performClick()
        waitFor { !compose.createFormOpen() }
        assertEquals(1, copies())
        compose.onNode(closePicker).performClick()
        waitFor { !exists(isDialog()) }
        assertNothingSaidFor6s("Plan does not repeat what the create form said")
        assertTrue(!tyreFlipStored())
    }

    /** The add form is saying a failed own exercise from Change when the screen is recreated: the restored form says it. */
    @Test
    fun planAdd_changeToOwnExerciseFails_heldAcrossRecreation_theRestoredFormSaysIt() {
        failExerciseInserts()
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        compose.onNodeWithContentDescription("Change exercise").performClick()
        openCreateForm()
        compose.fillCustomExercise()
        createThen(cancelInForm, closePicker)
        waitFor { saidOverTheDialog() && !pickerOpen() }

        restoration.emulateSavedInstanceStateRestore()
        waitFor { formOpen() && saidOverTheDialog() }
        assertEquals("over the restored form, not handed to Plan as well", 1, copies())
        assertAddForm("Squat")
        assertTrue(!tyreFlipStored())
    }

    @Test
    fun planAdd_rotatedWhileAdding_theFormIsNotRestoredOverTheStoredItem() {
        // The write is held until the test lets it go, as a slow disk would hold it.
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        val slow = object : TrainingRepository(db) {
            override suspend fun savePlanItem(item: PlanItem, catalogueName: String?): Long {
                release.await()
                return super.savePlanItem(item, catalogueName)
            }
        }
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { PlanScreen(slow, settings, onOpenSettings = {}) }
        pickOnMonday("Squat")
        assertAddForm("Squat")
        addButton().performClick()
        compose.waitForIdle()
        assertTrue(plan().isEmpty())

        // A rotation while Add is saving: the form is not brought back, so a second Add cannot store it again.
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertTrue("the add form is not restored mid-add", !formOpen())
        release.complete(Unit)
        waitFor { plan().isNotEmpty() }
        compose.waitForIdle()
        assertEquals(1, plan().size)
        assertTrue(!formOpen())
    }

    @Test
    fun planAdd_survivesRecreation_bodybuilding() {
        runBlocking { settings.update { com.powertracker.data.AppSettings(trainingMode = TrainingMode.BODYBUILDING) } }
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        pickOnMonday("Curl")
        assertAddForm("Curl")
        tap("Decrease Reps from")
        tap("Increase Reps to")
        tap("Increase Reps to")
        assertField("Reps from", "7")
        assertField("Reps to", "14")

        restoration.emulateSavedInstanceStateRestore()

        assertAddForm("Curl")
        assertField("Sets", "3")
        assertField("Reps from", "7")
        assertField("Reps to", "14")
        assertField("RPE", "8")
        compose.waitForIdle()
        assertTrue(plan(TrainingMode.BODYBUILDING).isEmpty())
        addButton().performClick()
        waitFor { plan(TrainingMode.BODYBUILDING).isNotEmpty() }
        val item = plan(TrainingMode.BODYBUILDING).single()
        assertEquals(listOf(3, 7, 14), listOf(item.sets, item.reps, item.repsHigh))
        assertEquals(TrainingMode.BODYBUILDING, item.mode)
    }
}
