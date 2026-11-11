package com.powertracker.ui

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.library.checkInfoButtonOpensGuide
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class TodayScreenTest {
    /** Outermost: the database closes only after the compose rule has disposed the composition and cleared the activity's view models. */
    @get:Rule(order = 0)
    val closeDatabase = object : ExternalResource() {
        override fun after() {
            if (::db.isInitialized) db.close()
        }
    }

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var squat: Exercise
    private var squatPlanId = 0L

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
            squatPlanId = training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            val lastWeek = training.startSession(today.minus(7, DateTimeUnit.DAY), null, null)
            training.logSet(lastWeek, LoggedSet(squat.id, 150.0, 5, 7.0, failed = false, missPoint = null, isTopSet = false))
        }
    }

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

    private val weight get() = compose.onNodeWithContentDescription("Weight, kilograms")
    private val reps get() = compose.onNodeWithContentDescription("Reps")
    private val rpe get() = compose.onNodeWithContentDescription("RPE")

    /** What the field holds: the value, or the text being typed. */
    private fun SemanticsNodeInteraction.typed(): String = fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    /** The message of a refused entry, read with the field. */
    private fun SemanticsNodeInteraction.message(): String? = fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error)

    /** Types over what the field holds and stays in it: nothing is committed until Done, another field or a button. */
    private fun SemanticsNodeInteraction.type(text: String) = performTextReplacement(text)

    private fun waitForField(field: () -> SemanticsNodeInteraction, text: String) = waitFor { field().typed() == text }

    /** Long enough for a set to have been written, had one been logged. */
    private fun settled() {
        val deadline = System.currentTimeMillis() + 300
        while (System.currentTimeMillis() < deadline) { compose.waitForIdle(); Thread.sleep(10) }
    }

    /** The session is open on the squat card: 150 × 5 @ 8. */
    private fun showSession() {
        startSession()
        show()
        waitForText("Log set 1")
    }

    /** Squat is replaced by a bench day: no history, so there is no weight yet. */
    private fun benchWithNoWeight() = runBlocking {
        training.deletePlanItem(squatPlanId)
        val bench = training.exercises().first().first { it.name == "Bench" }
        training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, bench.id, sets = 3, reps = 5, targetRpe = 8.0, order = 0))
    }

    /** A known best gives the coach enough history to suggest a target: 150 @ 7 -> 155 × 5 @ 8. */
    private fun showSessionWithSuggestion() {
        runBlocking { training.setBaseline(Baseline(squat.id, 190.0, today.minus(30, DateTimeUnit.DAY))) }
        startSession()
        show()
        waitForText("155 × 5 @ 8")
    }

    // ---- The exercise library: the picker and the info button ----

    private fun exerciseRows(name: String) = runBlocking { db.exercises().all() }.filter { it.name.equals(name, ignoreCase = true) }

    /** Opens the picker from the session and searches for [name] in it. */
    private fun pickFromLibrary(name: String) {
        compose.onNodeWithText("Add exercise").performScrollTo().performClick()
        waitFor { compose.onAllNodes(hasTestTag("exercise-search")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise-search").performTextReplacement(name)
        compose.waitForIdle()
    }

    @Test
    fun infoButton_opensGuide_fromEveryPlace() {
        // Squat is the open card; bench is a second planned exercise, folded to a row.
        runBlocking {
            val bench = training.exercises().first().first { it.name == "Bench" }
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, bench.id, sets = 3, reps = 5, targetRpe = 8.0, order = 1))
        }
        showSession()
        compose.onNodeWithText("EXERCISE 1 OF 2").assertExists()

        compose.checkInfoButtonOpensGuide("Squat")
        compose.checkInfoButtonOpensGuide("Bench")

        // Reading the row's guide did not open its card, and nothing was logged.
        compose.onNodeWithText("EXERCISE 1 OF 2").assertExists()
        compose.onNodeWithText("Log set 1").assertExists()
        assertTrue(loggedToday().isEmpty())

        // In the picker too, without adding the exercise to the session.
        pickFromLibrary("Curl")
        compose.checkInfoButtonOpensGuide("Curl")
        compose.onNodeWithTag("exercise-search").assertExists()
        compose.onNodeWithContentDescription("Close").performClick()
        waitForText("EXERCISE 1 OF 2")
        compose.onNodeWithText("Curl").assertDoesNotExist()
    }

    @Test
    fun pickedLibraryExercise_getsItsRowWithItsFirstSet() {
        // A library exercise that has never been used has no row.
        runBlocking { exerciseRows("Curl").forEach { db.exercises().delete(it.id) } }
        val before = runBlocking { db.exercises().all() }
        showSession()

        pickFromLibrary("Curl")
        compose.onNodeWithTag("exercise-Curl").performClick()
        waitForText("EXERCISE 2 OF 2")
        compose.onNodeWithText("Curl").assertExists()
        // On the card, not in the data: taking it back leaves nothing behind.
        assertEquals(before, runBlocking { db.exercises().all() })
        compose.onNodeWithText("Remove").performClick()
        waitForText("EXERCISE 1 OF 1")
        assertEquals(before, runBlocking { db.exercises().all() })

        pickFromLibrary("Curl")
        compose.onNodeWithTag("exercise-Curl").performClick()
        waitForText("EXERCISE 2 OF 2")
        weight.type("20")
        waitFor { compose.onAllNodesWithText("Log set 1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Log set 1").assertIsEnabled().performClick()
        waitForSets(1)

        // One built-in row, saved with the set that needed it; the card stays open on it for set 2.
        val curl = exerciseRows("Curl").single()
        assertTrue(curl.isBuiltIn)
        assertEquals(before.size + 1, runBlocking { db.exercises().all() }.size)
        assertEquals(listOf(LoggedSet(curl.id, 20.0, 5, 8.0, failed = false, missPoint = null, isTopSet = false)), loggedToday())
        waitForText("Log set 2")
        compose.onNodeWithText("EXERCISE 2 OF 2").assertExists()
        assertEquals("20", weight.typed())

        weight.type("22.5")
        compose.onNodeWithText("Log set 2").performClick()
        waitForSets(2)
        assertEquals(listOf(curl.id, curl.id), loggedToday().map { it.exerciseId })
        assertEquals(1, exerciseRows("Curl").size)
    }

    @Test
    fun pickedLibraryExercise_thatIsArchived_isRestoredWithItsFirstSet() {
        val curl = exerciseRows("Curl").single()
        runBlocking { training.archiveExercise(curl.id) }
        showSession()

        pickFromLibrary("Curl")
        compose.onNodeWithTag("exercise-Curl").performClick()
        waitForText("EXERCISE 2 OF 2")
        assertTrue(exerciseRows("Curl").single().archived)

        weight.type("20")
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)

        assertEquals(listOf(curl.copy(archived = false)), exerciseRows("Curl"))
        assertEquals(curl.id, loggedToday().single().exerciseId)
    }

    @Test
    fun createCustom_withCatalogueName_usesTheCatalogueEntry() {
        runBlocking { exerciseRows("Goblet squat").forEach { db.exercises().delete(it.id) } }
        val before = runBlocking { db.exercises().all() }
        showSession()
        pickFromLibrary("zzz")

        // An alias typed as a new exercise opens the catalogue's card, with its guide, and stores nothing yet.
        compose.onNodeWithText("Create your own").performClick()
        compose.onNodeWithText("Name").performTextReplacement("KB goblet squat")
        compose.onNodeWithText("Create").performClick()
        waitForText("EXERCISE 2 OF 2")
        assertTrue(compose.onAllNodesWithText("Goblet squat").fetchSemanticsNodes().isNotEmpty())
        assertEquals(before, runBlocking { db.exercises().all() })
        compose.checkInfoButtonOpensGuide("Goblet squat")
    }

    @Test
    fun createYourOwn_fromTodaysPicker_addsItToTheSession_withNoGuide() {
        showSession()
        pickFromLibrary("Tyre flip")
        compose.onNodeWithText("No exercises match. Try fewer filters, or create your own.").assertExists()

        compose.onNodeWithText("Create your own").performClick()
        compose.onNodeWithText("Create").performClick()
        waitForText("EXERCISE 2 OF 2")

        val sled = exerciseRows("Tyre flip").single()
        assertEquals(false, sled.isBuiltIn)
        compose.checkInfoButtonOpensGuide("Tyre flip")
    }

    @Test
    fun tapSet_logsAsPrescribed() {
        show()
        waitForText("Start session")
        compose.onNodeWithText("Start session").performClick()

        waitForText("Log set 1")
        assertEquals(listOf("150", "5", "8"), listOf(weight.typed(), reps.typed(), rpe.typed()))
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
        runBlocking { training.setBaseline(Baseline(squat.id, 190.0, today.minus(30, DateTimeUnit.DAY))) }
        startSession()
        show()

        waitForText("155 × 5 @ 8")
        compose.onNodeWithText("Coach suggests", ignoreCase = true).assertExists()
        // Still the planned numbers: nothing changes until Apply.
        assertEquals("150", weight.typed())

        compose.onNodeWithText("Apply").performClick()

        waitForField({ weight }, "155")
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

        // The plates follow the typing, before anything is committed.
        weight.type("182.5")
        waitForText("25 + 25 + 25 + 5 + 1.25")
        // Text that is not a number is not a weight: the plates are the last good value's.
        weight.type("182.5x")
        waitForText("25 + 25 + 15")

        // 181 kg cannot be loaded with 1.25 kg plates: say so instead of drawing a wrong bar.
        weight.type("181")
        weight.performImeAction()
        waitForText("Can't load 181 kg exactly")

        // Lighter than the bar.
        weight.type("15")
        waitForText("Lighter than the bar")
    }

    // ---- Typing weight, reps and RPE straight into the exercise card ----

    @Test
    fun typeWeight_thenLog_logs140() {
        benchWithNoWeight()
        showSession()
        assertEquals("", weight.typed())
        // A faint "kg" stands where the number will be; the old "tap the number" note is gone.
        assertEquals(listOf("kg"), weight.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text })
        compose.onNodeWithText("Tap the number", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Type the weight to see the plates.").assertExists()
        compose.onNodeWithText("Log set 1").assertIsNotEnabled()
        compose.onNodeWithText("Missed").assertIsNotEnabled()

        weight.type("140")
        // The plates follow the typing.
        waitForText("25 + 25 + 10")
        compose.onNodeWithText("Log set 1").assertIsEnabled().performClick()

        waitForSets(1)
        val bench = runBlocking { training.exercises().first().first { it.name == "Bench" } }
        assertEquals(listOf(LoggedSet(bench.id, 140.0, 5, 8.0, failed = false, missPoint = null, isTopSet = false)), loggedToday())
        waitForText("Log set 2")
        assertEquals("140", weight.typed())
    }

    @Test
    fun tapPrefilled_selectsAndReplaces() {
        showSession()
        assertEquals("150", weight.typed())

        // No dialog: the tap puts the cursor in the field with the value selected.
        weight.performClick()
        compose.onNodeWithTag("number-entry").assertDoesNotExist()
        weight.assertIsFocused()
        waitFor { weight.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange] == TextRange(0, 3) }
        weight.performTextInput("160")
        assertEquals("160", weight.typed())
        weight.performImeAction()

        weight.assertIsNotFocused()
        waitForText("25 + 25 + 20")
        assertEquals("160", weight.typed())
        // Committed to the editor: the step starts from it.
        compose.onNodeWithContentDescription("Increase Weight").performClick()
        waitForField({ weight }, "162.5")
    }

    @Test
    fun movingToAnotherField_commitsTheOneLeft() {
        showSession()
        weight.type("160,0")
        reps.performClick()
        reps.assertIsFocused()
        // Leaving the field commits it: the entry is replaced by the value as the editor holds it.
        waitForField({ weight }, "160")
        waitFor { reps.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange] == TextRange(0, 1) }
        reps.performTextInput("3")
        rpe.performClick()
        waitForField({ reps }, "3")
        rpe.performTextInput("9")
        rpe.performImeAction()
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(listOf(160.0 to 3), loggedToday().map { it.weightKg to it.reps })
        assertEquals(9.0, loggedToday().single().rpe)
    }

    @Test
    fun usesTheDecimalAndNumericKeypads_withDone() {
        showSession()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        fun inputOf(field: SemanticsNodeInteraction): EditorInfo {
            field.performClick()
            compose.waitForIdle()
            return EditorInfo().also { view.onCreateInputConnection(it) }
        }
        val decimal = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        for ((field, type) in listOf(weight to decimal, reps to InputType.TYPE_CLASS_NUMBER, rpe to decimal)) {
            val info = inputOf(field)
            assertEquals(type, info.inputType and (InputType.TYPE_MASK_CLASS or InputType.TYPE_MASK_FLAGS))
            assertEquals(EditorInfo.IME_ACTION_DONE, info.imeOptions and EditorInfo.IME_MASK_ACTION)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp-xxhdpi")
    fun threeFieldsFitAcrossASmallPhone_withFullSizeTargets() {
        showSession()
        // The longest weight there is, with the cursor in it.
        weight.type("1000.25")
        compose.waitForIdle()
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        val fields = listOf(weight, reps, rpe).map { it.getUnclippedBoundsInRoot() }
        for (field in listOf(weight, reps, rpe)) field.assertHeightIsAtLeast(48.dp)
        fields.forEach { assertTrue("a field is at least 48 dp wide", it.right - it.left >= 48.dp) }
        assertTrue("fields stay on the screen", fields.first().left >= 0.dp && fields.last().right <= screen.right)
        fields.zipWithNext { a, b -> assertTrue("fields do not run into each other", a.right <= b.left) }
        // The entry scrolls inside its own column: it never reaches under the reps buttons.
        assertTrue(fields[0].right <= compose.onNodeWithContentDescription("Decrease Reps").getUnclippedBoundsInRoot().left)
        for (name in listOf("Decrease Weight", "Increase Weight", "Decrease Reps", "Increase Reps", "Decrease RPE", "Increase RPE")) {
            val button = compose.onNodeWithContentDescription(name)
            button.assertHeightIsAtLeast(48.dp)
            val bounds = button.getUnclippedBoundsInRoot()
            assertTrue("$name stays on the screen", bounds.left >= 0.dp && bounds.right <= screen.right)
        }
    }

    @Test
    fun invalid_showsMessage_andDoesNotLog() {
        showSession()

        weight.type("abc")
        // Nothing is said while the entry is still being typed.
        assertNull(weight.message())
        weight.performImeAction()
        assertEquals("Numbers only", weight.message())
        compose.onNodeWithText("Numbers only").assertExists()
        assertEquals("abc", weight.typed())

        weight.type("1200")
        // The message goes while the entry is being corrected, and comes back when it is still wrong.
        assertNull(weight.message())
        weight.performImeAction()
        assertEquals("0 to 1000 kg", weight.message())

        rpe.type("11")
        rpe.performImeAction()
        assertEquals("6 to 10", rpe.message())

        reps.type("0")
        reps.performImeAction()
        assertEquals("Whole, 1 to 99", reps.message())
        reps.type("5.5")
        reps.performImeAction()
        assertEquals("Whole, 1 to 99", reps.message())

        // The editor keeps its last good numbers.
        compose.onNodeWithText("25 + 25 + 15").assertExists() // still 150 kg

        // Log set does not fall back to those numbers while a refused entry is on the card: it points at the first one.
        compose.onNodeWithText("Log set 1").performClick()
        weight.assertIsFocused()
        settled()
        assertTrue(loggedToday().isEmpty())

        // Corrected, the set is logged.
        weight.type("152,5")
        reps.type("4")
        rpe.type("9")
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(listOf(LoggedSet(squat.id, 152.5, 4, 9.0, failed = false, missPoint = null, isTopSet = false)), loggedToday())
        for (field in listOf(weight, reps, rpe)) assertNull(field.message())
    }

    @Test
    fun logSet_withInvalidTypedValue_refusesAndFocusesField() {
        showSession()
        weight.type("1200")
        compose.onNodeWithText("Log set 1").performClick()

        weight.assertIsFocused()
        assertEquals("0 to 1000 kg", weight.message())
        assertEquals("1200", weight.typed())
        settled()
        assertTrue(loggedToday().isEmpty())
        compose.onNodeWithText("Did you mean", substring = true).assertDoesNotExist()

        // With the cursor in another field: the good entry is kept, the first bad one gets the cursor.
        reps.type("3")
        rpe.type("11")
        compose.onNodeWithText("Log set 1").performClick()
        weight.assertIsFocused()
        assertEquals("6 to 10", rpe.message())
        waitForField({ reps }, "3")
        assertNull(reps.message())
        settled()
        assertTrue(loggedToday().isEmpty())

        // Weight put right: the next refused field is RPE, and it gets the cursor.
        weight.type("160")
        compose.onNodeWithText("Log set 1").performClick()
        rpe.assertIsFocused()
        waitForField({ weight }, "160")
        settled()
        assertTrue(loggedToday().isEmpty())

        // An emptied field is no error: it falls back to the last good RPE.
        rpe.type("")
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(listOf(LoggedSet(squat.id, 160.0, 3, 8.0, failed = false, missPoint = null, isTopSet = false)), loggedToday())
    }

    @Test
    fun missed_withInvalidTypedValue_refusesAndFocusesField() {
        showSession()
        rpe.type("abc")
        compose.onNodeWithText("Missed").performClick()

        rpe.assertIsFocused()
        assertEquals("Numbers only", rpe.message())
        settled()
        assertTrue(loggedToday().isEmpty())

        // The same after the field was left with the refused entry in it.
        rpe.performImeAction()
        rpe.assertIsNotFocused()
        compose.onNodeWithText("Missed").performClick()
        rpe.assertIsFocused()
        settled()
        assertTrue(loggedToday().isEmpty())

        // Reps is checked before RPE.
        reps.type("0")
        compose.onNodeWithText("Missed").performClick()
        reps.assertIsFocused()
        settled()
        assertTrue(loggedToday().isEmpty())
    }

    @Test
    fun invalid_withNoWeightYet_keepsLogDisabled() {
        benchWithNoWeight()
        showSession()
        for (text in listOf("abc", "1200", "-5", "1,000")) {
            weight.type(text)
            weight.performImeAction()
            assertTrue("$text is refused", weight.message() != null)
            compose.onNodeWithText("Log set 1").assertIsNotEnabled()
            compose.onNodeWithText("Missed").assertIsNotEnabled()
        }
        assertTrue(loggedToday().isEmpty())
    }

    @Test
    fun commaDecimal_isSavedAsADecimalPoint() {
        showSession()
        weight.type("102,5")
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(102.5, loggedToday().single().weightKg, 0.0)
        waitForField({ weight }, "102.5")
    }

    @Test
    fun typeThenPlus_stepsFromTyped() {
        showSession()
        // The buttons take no focus: the cursor is still in the field when + is tapped.
        weight.type("140")
        compose.onNodeWithContentDescription("Increase Weight").performClick()
        weight.assertIsFocused()
        waitForField({ weight }, "142.5")
        reps.type("8")
        compose.onNodeWithContentDescription("Decrease Reps").performClick()
        waitForField({ reps }, "7")
        rpe.type("9")
        compose.onNodeWithContentDescription("Increase RPE").performClick()
        waitForField({ rpe }, "9.5")

        // A step in one field keeps what was typed in another.
        weight.type("100")
        compose.onNodeWithContentDescription("Increase Reps").performClick()
        waitForField({ reps }, "8")
        assertEquals("100", weight.typed())

        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(listOf(LoggedSet(squat.id, 100.0, 8, 9.5, failed = false, missPoint = null, isTopSet = false)), loggedToday())
    }

    @Test
    fun typeThenPlus_withChangePlates_usesTheSmallerStep() {
        runBlocking { settings.update { it.copy(changePlates = true) } }
        showSession()
        weight.type("140")
        compose.onNodeWithContentDescription("Increase Weight").performClick()
        // The step is the smallest change the plates allow: 2 × 0.25 kg with change plates.
        waitForField({ weight }, formatKg(140 + PlateLoader.step(changePlates = true)))
        assertEquals("140.5", weight.typed())
    }

    @Test
    fun bigJump_stillAsksDidYouMean() {
        showSession()
        weight.type("250")
        compose.onNodeWithText("Log set 1").performClick()
        waitForText("Did you mean 250 kg × 5?")
        compose.onNodeWithText("That is a big jump from 150 kg.").assertExists()
        assertTrue("nothing is saved before Confirm", loggedToday().isEmpty())

        // Typed reps are checked too.
        compose.onNodeWithText("Edit").performClick()
        waitFor { compose.onAllNodesWithText("Did you mean", substring = true).fetchSemanticsNodes().isEmpty() }
        weight.type("150")
        reps.type("13")
        compose.onNodeWithText("Log set 1").performClick()
        waitForText("Did you mean 150 kg × 13?")
    }

    @Test
    fun configurationChange_keepsTypedValue() {
        startSession()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            PowertrackerTheme { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { today }) }
        }
        waitForText("Log set 1")
        // Reps and RPE are committed; the weight is still being typed when the screen is rebuilt.
        reps.type("3")
        reps.performImeAction()
        rpe.type("9")
        rpe.performImeAction()
        weight.type("162,5")

        restoration.emulateSavedInstanceStateRestore()

        waitForText("Log set 1")
        // The rebuild took the cursor out of the weight field, which committed it: the field shows the value, as after any blur.
        assertEquals(listOf("162.5", "3", "9"), listOf(weight.typed(), reps.typed(), rpe.typed()))
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(listOf(LoggedSet(squat.id, 162.5, 3, 9.0, failed = false, missPoint = null, isTopSet = false)), loggedToday())
    }

    @Test
    fun configurationChange_keepsARefusedEntryAndItsMessage() {
        startSession()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            PowertrackerTheme { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { today }) }
        }
        waitForText("Log set 1")
        weight.type("1200")
        weight.performImeAction()

        restoration.emulateSavedInstanceStateRestore()

        waitForText("Log set 1")
        assertEquals("1200", weight.typed())
        assertEquals("0 to 1000 kg", weight.message())
    }

    @Test
    fun configurationChange_thenApply_showsTheAppliedValue() {
        runBlocking { training.setBaseline(Baseline(squat.id, 190.0, today.minus(30, DateTimeUnit.DAY))) }
        startSession()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            PowertrackerTheme { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { today }) }
        }
        waitForText("155 × 5 @ 8")
        // The weight is still being typed when the screen is rebuilt: the number comes back, the cursor does not.
        weight.type("14")

        restoration.emulateSavedInstanceStateRestore()

        waitForText("Apply")
        weight.assertIsNotFocused()
        assertEquals("14", weight.typed())

        // Nothing leaves a field without the cursor, so the applied value has to show at once.
        compose.onNodeWithText("Apply").performClick()
        waitForText("25 + 25 + 15 + 2.5")
        waitForField({ weight }, "155")
        assertNull(weight.message())

        // The field and the logged set agree.
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(155.0, loggedToday().single().weightKg, 0.0)
        assertEquals("155", weight.typed())
    }

    @Test
    fun processDeath_showsTheEditorValue_notTheStaleTypedOne() {
        startSession()
        // A fresh store stands in for the process that was killed: the view model, and its editor, start over.
        val stores = mutableListOf<ViewModelStore>()
        fun freshStore() = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore().also { stores += it }
        }
        // These stores are not an activity's, so nothing else clears them. Clear them while the database is still open.
        try {
            var models: ViewModelStoreOwner = freshStore()
            val restoration = StateRestorationTester(compose)
            restoration.setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides models) {
                    PowertrackerTheme { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { today }) }
                }
            }
            waitForText("Log set 1")
            // + takes the weight to 152.5; "160" is typed over that and never committed.
            compose.onNodeWithContentDescription("Increase Weight").performClick()
            waitForField({ weight }, "152.5")
            weight.type("160")

            models = freshStore()
            restoration.emulateSavedInstanceStateRestore()

            // The editor is rebuilt from history at 150. What was typed over 152.5 is stale: it is not shown.
            waitForText("Log set 1")
            waitForField({ weight }, "150")
            assertNull(weight.message())
            compose.onNodeWithText("25 + 25 + 15").assertExists()

            // The field and the logged set agree.
            compose.onNodeWithText("Log set 1").performClick()
            waitForSets(1)
            assertEquals(150.0, loggedToday().single().weightKg, 0.0)
            assertEquals("150", weight.typed())
        } finally {
            stores.forEach { it.clear() }
        }
    }

    @Test
    fun logSet_usesTheTypedValue_withoutBlur() {
        showSession()
        reps.type("3")
        rpe.type("8.7")
        weight.type("140")
        // The tap lands while the weight field still has the cursor.
        compose.onNodeWithText("Log set 1").performClick()
        weight.assertIsFocused()

        waitForSets(1)
        assertEquals(listOf(LoggedSet(squat.id, 140.0, 3, 8.5, failed = false, missPoint = null, isTopSet = false)), loggedToday())
        waitForField({ weight }, "140")
        waitForField({ rpe }, "8.5")
    }

    @Test
    fun missed_usesTheTypedValue_withoutBlur() {
        showSession()
        weight.type("155")
        compose.onNodeWithText("Missed").performClick()
        waitForSets(1)
        assertEquals(listOf(155.0 to true), loggedToday().map { it.weightKg to it.failed })
    }

    @Test
    fun stepAfterInvalidText_stepsFromLastGoodValue() {
        showSession()
        weight.type("abc")
        weight.performImeAction()
        assertEquals("Numbers only", weight.message())

        compose.onNodeWithContentDescription("Increase Weight").performClick()
        waitForField({ weight }, "152.5")
        assertNull(weight.message())
        compose.onNodeWithText("Numbers only").assertDoesNotExist()

        // The same when + is tapped straight from the field.
        weight.type("abc")
        compose.onNodeWithContentDescription("Increase Weight").performClick()
        waitForField({ weight }, "155")
        assertNull(weight.message())

        // A step in another field leaves the refused entry and its message alone.
        weight.type("abc")
        compose.onNodeWithContentDescription("Increase Reps").performClick()
        waitForField({ reps }, "6")
        assertEquals("abc", weight.typed())
        assertEquals("Numbers only", weight.message())
    }

    @Test
    fun applyWhileTyping_doesNotClobberKeystrokes() {
        showSessionWithSuggestion()
        // The editor is off the plan in every field: 150 × 6 @ 8.5.
        compose.onNodeWithContentDescription("Increase Reps").performClick()
        compose.onNodeWithContentDescription("Increase RPE").performClick()
        waitForField({ reps }, "6")
        waitForField({ rpe }, "8.5")

        // Half-way through typing "140" the lifter taps Apply; the field keeps the cursor.
        weight.type("14")
        compose.onNodeWithText("Apply").performClick()
        waitFor { compose.onAllNodesWithText("Apply").fetchSemanticsNodes().isEmpty() }
        // The plates already show what was applied.
        waitForText("25 + 25 + 15 + 2.5")

        // Focus stays and the keystrokes are still there.
        weight.assertIsFocused()
        assertEquals("14", weight.typed())
        // The fields without the cursor show the applied numbers: 155 × 5 @ 8.
        waitForField({ reps }, "5")
        waitForField({ rpe }, "8")
        assertEquals("14", weight.typed())

        // Leaving the field shows the applied value: the stale "14" is not saved over it.
        weight.performImeAction()
        waitForField({ weight }, "155")
        assertNull(weight.message())
        compose.onNodeWithText("25 + 25 + 15 + 2.5").assertExists()
        assertEquals(listOf(AdviceStatus.APPLIED), runBlocking { advice.all() }.filter { it.type == AdviceType.TARGET }.map { it.status })

        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(155.0, loggedToday().single().weightKg, 0.0)
    }

    @Test
    fun applyWhileTyping_thenLogWithoutLeaving_logsTheAppliedValue() {
        showSessionWithSuggestion()
        weight.type("14")
        compose.onNodeWithText("Apply").performClick()
        waitFor { compose.onAllNodesWithText("Apply").fetchSemanticsNodes().isEmpty() }
        waitForText("25 + 25 + 15 + 2.5")

        // What was typed before Apply is stale: the set is logged at the weight the plates show.
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(155.0, loggedToday().single().weightKg, 0.0)
        waitForField({ weight }, "155")
    }

    @Test
    fun applyWhileTyping_thenTypingOn_keepsWhatWasTyped() {
        showSessionWithSuggestion()
        weight.type("14")
        compose.onNodeWithText("Apply").performClick()
        waitFor { compose.onAllNodesWithText("Apply").fetchSemanticsNodes().isEmpty() }
        waitForText("25 + 25 + 15 + 2.5")
        assertEquals("14", weight.typed())

        // The lifter finishes the number after Apply: that is the newer decision.
        weight.performTextInput("0")
        assertEquals("140", weight.typed())
        weight.performImeAction()
        waitForText("25 + 25 + 10")
        assertEquals("140", weight.typed())
        compose.onNodeWithText("Log set 1").performClick()
        waitForSets(1)
        assertEquals(140.0, loggedToday().single().weightKg, 0.0)
    }

    @Test
    fun applyWithARefusedEntryOnTheCard_showsTheAppliedValue() {
        showSessionWithSuggestion()
        weight.type("abc")
        weight.performImeAction()
        assertEquals("Numbers only", weight.message())

        // A refused entry gives way to the newer value.
        compose.onNodeWithText("Apply").performClick()
        waitForField({ weight }, "155")
        assertNull(weight.message())
    }

    @Test
    fun clearedField_keepsLastGoodValue() {
        showSession()
        weight.type("")
        assertEquals("", weight.typed())
        weight.performImeAction()

        // Not a value and not an error: the last good number is back.
        waitForField({ weight }, "150")
        assertNull(weight.message())
        compose.onNodeWithText("25 + 25 + 15").assertExists()

        reps.type("")
        rpe.type("")
        compose.onNodeWithText("Log set 1").assertIsEnabled().performClick()
        waitForSets(1)
        assertEquals(listOf(LoggedSet(squat.id, 150.0, 5, 8.0, failed = false, missPoint = null, isTopSet = false)), loggedToday())
    }

    @Test
    fun clearedField_withNoWeightYet_keepsLogDisabled() {
        benchWithNoWeight()
        showSession()
        weight.type("60")
        compose.onNodeWithText("Log set 1").assertIsEnabled()
        weight.type("")
        weight.performImeAction()
        assertEquals("", weight.typed())
        assertNull(weight.message())
        compose.onNodeWithText("Log set 1").assertIsNotEnabled()
        compose.onNodeWithText("Missed").assertIsNotEnabled()
        // + still starts an empty weight at the bar.
        compose.onNodeWithContentDescription("Increase Weight").performClick()
        waitForField({ weight }, "20")
        assertTrue(loggedToday().isEmpty())
    }

    @Test
    fun appliedDeloadTarget_showsSetOfFewerSets() {
        runBlocking {
            training.setBaseline(Baseline(squat.id, 190.0, today.minus(30, DateTimeUnit.DAY)))
            advice.insert(listOf(Advice(AdviceType.DELOAD, null, today, "Take a deload week.", 2, AdvicePayload.Deload())))
        }
        startSession()
        show()

        waitForText("· 3 sets")
        compose.onNodeWithText("Set 1 of 5").assertExists()

        compose.onNodeWithText("Apply").performClick()

        waitForText("Set 1 of 3")
    }

    @Test
    fun appLeftOpenPastMidnight_planEditDoesNotStoreTargetsUnderYesterday() {
        runBlocking { training.setBaseline(Baseline(squat.id, 170.0, today.minus(30, DateTimeUnit.DAY))) }
        var clock = today
        compose.setContent {
            PowertrackerTheme { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { clock }) }
        }
        // Today's coach pass stores one squat suggestion dated today.
        waitFor { runBlocking { advice.all() }.any { it.type == AdviceType.TARGET } }
        val before = runBlocking { advice.all() }.filter { it.type == AdviceType.TARGET }
        assertEquals(listOf(today), before.map { it.createdAt })

        // Midnight passes without the screen resuming; then the plan for yesterday's weekday changes.
        clock = today.plus(1, DateTimeUnit.DAY)
        val bench = runBlocking { training.exercises().first().first { it.name == "Bench" } }
        runBlocking {
            training.setBaseline(Baseline(bench.id, 120.0, today.minus(30, DateTimeUnit.DAY)))
            training.logSet(training.startSession(today.minus(6, DateTimeUnit.DAY), null, null),
                LoggedSet(bench.id, 100.0, 5, 7.0, failed = false, missPoint = null, isTopSet = false))
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, bench.id, sets = 3, reps = 5, targetRpe = 8.0, order = 1))
        }
        val deadline = System.currentTimeMillis() + 1_000
        while (System.currentTimeMillis() < deadline) { compose.waitForIdle(); Thread.sleep(10) }

        val datedYesterday = runBlocking { advice.all() }.filter { it.type == AdviceType.TARGET && it.createdAt == today }
        assertEquals(before.map { it.id }, datedYesterday.map { it.id })
    }
}
