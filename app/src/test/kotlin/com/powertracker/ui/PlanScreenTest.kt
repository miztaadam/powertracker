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
import androidx.compose.ui.test.performTextReplacement
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
import com.powertracker.ui.library.checkInfoButtonOpensGuide
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

        compose.waitUntil(5_000) { runBlocking { settings.settings.first().restAlert } == RestAlert.SOUND_AND_VIBRATE }
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
        compose.waitUntil(5_000) { !runBlocking { settings.settings.first().vibrateOnTaps } }
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

    /** The add form is open, for [name], titled for Monday. */
    private fun assertAddForm(name: String) {
        waitFor { exists(hasText("Sets") and hasAnyAncestor(isDialog())) }
        compose.onNode(hasText("Add to Monday") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNode(hasText(name) and hasAnyAncestor(isDialog())).assertExists()
        addButton().assertExists()
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).assertDoesNotExist()
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
        waitFor { exists(hasText("Squat  4 × 6 @ 7.5")) }
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
}
