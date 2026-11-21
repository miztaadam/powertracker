package com.powertracker.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.StoredSet
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.stack.PlateStack
import com.powertracker.domain.validation.InputWarning
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.components.SetRow
import com.powertracker.ui.components.SetState
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.design.assertTextsFitInEveryWindow
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.COUNT_PLATES
import com.powertracker.ui.today.PendingLog
import com.powertracker.ui.today.PlateCountSheet
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.StartSessionCard
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayScreen
import com.powertracker.ui.today.TodayState
import com.powertracker.ui.today.doseText
import com.powertracker.ui.today.plateNote
import com.powertracker.ui.today.plateSheetLabel
import com.powertracker.ui.today.targetHeadline
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val DAY: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

private fun exercise(id: Long, name: String, category: Category = Category.ACCESSORY, plateCount: PlateCount = PlateCount.OFF) =
    Exercise(id, name, category, null, emptyList(), isBuiltIn = true, plateCount = plateCount)

private fun logged(id: Long, exerciseId: Long, weight: Double, reps: Int, unit: LoadUnit = LoadUnit.KG, failed: Boolean = false) =
    StoredSet(id, 1, LoggedSet(exerciseId, weight, reps, 8.0, failed = failed, missPoint = null, isTopSet = false, unit = unit))

/** A Today item as the view model builds one: in plates when [count] is on (Lat pulldown is offered). */
private fun plateItem(
    count: PlateCount,
    weight: Double?,
    sets: List<StoredSet> = emptyList(),
    suggestion: Advice? = null,
    name: String = "Lat pulldown",
    id: Long = 1,
    offered: Boolean = true,
): TodayItem {
    val ex = exercise(id, name, plateCount = count)
    val unit = PlateStack.unitOf(ex)
    val plan = PlanItem(id, DayOfWeek.WEDNESDAY, id, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)
    return TodayItem(ex, plan, suggestion, sets, SetEditor(weight, 8, 8.0, unit), 3, null, weight, unit = unit, plateCount = count, plateOffered = offered)
}

/** The real screen and its menu, the sheet, the stepper and the plate wording (1.17, Task 7). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class PlateCountScreenTest {
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings() } }
    }

    private fun row(name: String): Long = runBlocking { training.inTransaction { training.resolveCatalogue(name) } }

    private fun plan(exerciseId: Long, order: Int) = runBlocking {
        training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, exerciseId, sets = 3, reps = 8, targetRpe = 8.0, order = order))
    }

    private fun showScreen() = compose.setContent {
        PowertrackerTheme { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { DAY }) }
    }

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun exists(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun menuExists(name: String) = compose.onAllNodes(hasContentDescription("Options for $name")).fetchSemanticsNodes().isNotEmpty()

    private fun plateItemShown() = compose.onAllNodes(hasTestTag("exercise-menu-plates")).fetchSemanticsNodes().isNotEmpty()

    /** Opens [name]'s ⋮ and says whether Count plates is in it; the menu is closed again. */
    private fun menuOffersPlates(name: String): Boolean {
        waitFor { menuExists(name) }
        compose.onNodeWithContentDescription("Options for $name").performScrollTo().performClick()
        compose.waitForIdle()
        val offered = plateItemShown()
        if (offered) compose.onNodeWithText(COUNT_PLATES).assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
        return offered
    }

    private fun stored(id: Long) = runBlocking { training.exercises().first() }.first { it.id == id }

    @Test
    fun theStartSessionCard_offersCountPlates_beforeASession_andTheChoiceIsStored() {
        val lat = row("Lat pulldown")
        plan(lat, 0)
        showScreen()
        waitFor { exists("Ready to train?") && menuExists("Lat pulldown") }

        compose.onNodeWithContentDescription("Options for Lat pulldown").performScrollTo().performClick()
        waitFor { plateItemShown() }
        compose.onNodeWithTag("exercise-menu-plates").performClick()
        waitFor { compose.onAllNodes(hasTestTag("plate-sheet")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("plate-option-WHOLE").performClick()

        waitFor { stored(lat).plateCount == PlateCount.WHOLE }
        assertNull("no session was needed", runBlocking { training.sessionOn(DAY).first() })
    }

    @Test
    fun theStartSessionCard_withDefaultActions_hasNoPlateItem() {
        val item = plateItem(PlateCount.OFF, null)
        compose.setContent {
            PowertrackerTheme {
                androidx.compose.foundation.layout.Column {
                    TodayContent(TodayState(DAY, loading = false, items = listOf(item)), TodayActions())
                }
            }
        }
        // The start card's own Skip today is there; Count plates is not, with no callback for it.
        compose.onNodeWithContentDescription("Options for Lat pulldown").performClick()
        compose.waitForIdle()
        assertTrue(exists("Skip today"))
        assertTrue(!plateItemShown())
    }

    @Test
    fun kgTypedAndNotYetCommitted_neverReachesThePlateField() {
        val lat = row("Lat pulldown")
        plan(lat, 0)
        runBlocking { training.startSession(DAY, null, null) }
        showScreen()
        waitFor { compose.onAllNodes(hasContentDescription("Weight, kilograms")).fetchSemanticsNodes().isNotEmpty() }
        // Typed, with the cursor still in the field: the 40 is a draft, not yet the editor's.
        compose.onNode(hasContentDescription("Weight, kilograms")).performClick().performTextReplacement("40")
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Options for Lat pulldown").performClick()
        waitFor { plateItemShown() }
        compose.onNodeWithTag("exercise-menu-plates").performClick()
        waitFor { compose.onAllNodes(hasTestTag("plate-sheet")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("plate-option-WHOLE").performClick()
        waitFor { stored(lat).plateCount == PlateCount.WHOLE }
        compose.onNodeWithTag("plate-done").performClick()
        waitFor { compose.onAllNodes(hasContentDescription("Weight, plates")).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()

        assertEquals("", field().fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("Log set 1").assertIsNotEnabled()
        compose.onNodeWithText("Missed").assertIsNotEnabled()
        assertTrue("nothing stored", runBlocking { training.setsForSession(training.sessionOn(DAY).first()!!.id).first() }.isEmpty())
    }

    @Test
    fun theMenuItem_showsOnAnOfferedExercise_alsoAfterItsFirstSet_andOnAnAddedOne() {
        val lat = row("Lat pulldown")
        val legPress = row("Leg press")
        plan(lat, 0)
        runBlocking {
            val session = training.startSession(DAY, null, null)
            training.logSet(session, LoggedSet(lat, 60.0, 8, 8.0, failed = false, missPoint = null, isTopSet = false))
            // Logged today without a plan line: an added exercise.
            training.logSet(session, LoggedSet(legPress, 120.0, 8, 8.0, failed = false, missPoint = null, isTopSet = false))
        }
        showScreen()
        // Lat pulldown is the open card (sets left), with a set: no Skip today, but the plate item.
        assertTrue(menuOffersPlates("Lat pulldown"))
        // Leg press is a folded row, added.
        assertTrue(menuOffersPlates("Leg press"))
    }

    @Test
    fun noMenuItem_onBench_onACustomVariation_onAssistedDip() {
        val bench = runBlocking { training.exercises().first() }.first { it.name == "Bench" }.id
        val variation = runBlocking { training.addExercise(Exercise(0, "My board press", Category.VARIATION, MainLift.BENCH, emptyList(), isBuiltIn = false)) }
        val dip = row("Assisted dip")
        plan(bench, 0)
        plan(variation, 1)
        plan(dip, 2)
        runBlocking { training.startSession(DAY, null, null) }
        showScreen()
        for (name in listOf("Bench", "My board press", "Assisted dip")) {
            assertTrue("$name has no plate item", !menuOffersPlates(name))
        }
    }

    @Test
    fun benchWithASet_hasNoMenuAtAll() {
        val bench = runBlocking { training.exercises().first() }.first { it.name == "Bench" }.id
        plan(bench, 0)
        runBlocking {
            val session = training.startSession(DAY, null, null)
            training.logSet(session, LoggedSet(bench, 100.0, 5, 8.0, failed = false, missPoint = null, isTopSet = false))
        }
        showScreen()
        waitFor { exists("Bench") }
        assertTrue(!menuExists("Bench"))
    }

    @Test
    fun theGuidesToday_hasNoPlateItem() {
        val item = plateItem(PlateCount.OFF, 60.0, sets = listOf(logged(1, 1, 60.0, 10)))
        compose.setContent { PowertrackerTheme { TodayContent(TodayState(DAY, loading = false, sessionId = 1, items = listOf(item)), TodayActions()) } }
        compose.waitForIdle()
        assertTrue(exists("Lat pulldown"))
        assertTrue("no ⋮ at all", !menuExists("Lat pulldown"))
    }

    @Test
    fun theSheet_marksTheCurrentChoice_andAChoiceCallsOnCountPlates() {
        val chosen = mutableListOf<PlateCount>()
        var dismissed = 0
        compose.setContent { PowertrackerTheme { PlateCountSheet("Lat pulldown", PlateCount.WHOLE, null, { chosen += it }, { dismissed++ }) } }
        waitFor { compose.onAllNodes(hasTestTag("plate-done")).fetchSemanticsNodes().isNotEmpty() }

        assertTrue(exists("Lat pulldown"))
        assertTrue(exists("How does this machine show its weight?"))
        for (text in listOf("In kg", "Whole plates", "1, 2, 3 …", "Half steps too", "1, 1½, 2 …", "Done")) assertTrue(text, compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty())
        val sheet = compose.onNodeWithTag("plate-sheet").fetchSemanticsNode()
        assertEquals(plateSheetLabel("Lat pulldown"), sheet.config.getOrNull(SemanticsProperties.PaneTitle))
        assertEquals("Count plates on Lat pulldown", plateSheetLabel("Lat pulldown"))
        // One selectable group of three radio buttons.
        assertTrue(compose.onNodeWithTag("plate-options").fetchSemanticsNode().config.contains(SemanticsProperties.SelectableGroup))
        compose.onNodeWithTag("plate-option-WHOLE").assertIsSelected()
        compose.onNodeWithTag("plate-option-OFF").assertIsNotSelected()
        compose.onNodeWithTag("plate-option-HALF").assertIsNotSelected()
        assertEquals(androidx.compose.ui.semantics.Role.RadioButton, compose.onNodeWithTag("plate-option-HALF").fetchSemanticsNode().config[SemanticsProperties.Role])
        assertTrue(compose.onAllNodes(hasTestTag("plate-note")).fetchSemanticsNodes().isEmpty())

        compose.onNodeWithTag("plate-option-HALF").performClick()
        waitFor { chosen == listOf(PlateCount.HALF) }
        compose.onNodeWithTag("plate-option-HALF").assertIsSelected()
        assertEquals(0, dismissed)
        compose.onNodeWithTag("plate-done").performClick()
        waitFor { dismissed == 1 }
        assertEquals(listOf(PlateCount.HALF), chosen)
    }

    @Test
    fun theNote_showsOnlyForANewUnitWithSetsInTheOldOne() {
        val toPlates = "The coach starts this exercise fresh in plates. Your kg sets stay as they are."
        val toKg = "The coach goes back to kg. Your plate sets stay as they are."
        assertEquals(toPlates, plateNote(LoadUnit.KG, LoadUnit.PLATES, hasSetsInOpenedUnit = true))
        assertNull(plateNote(LoadUnit.PLATES, LoadUnit.PLATES, hasSetsInOpenedUnit = true))
        assertNull(plateNote(LoadUnit.KG, LoadUnit.PLATES, hasSetsInOpenedUnit = false))
        assertEquals(toKg, plateNote(LoadUnit.PLATES, LoadUnit.KG, hasSetsInOpenedUnit = true))
        assertNull(plateNote(LoadUnit.KG, LoadUnit.KG, hasSetsInOpenedUnit = true))

        // On Today: opened in kg with a kg set today; each choice applies at once, and the note follows it.
        var state by mutableStateOf(TodayState(DAY, loading = false, sessionId = 1, items = listOf(plateItem(PlateCount.OFF, 60.0, listOf(logged(1, 1, 60.0, 10))))))
        val actions = TodayActions(onCountPlates = { _, value ->
            val item = state.items.single()
            state = state.copy(items = listOf(plateItem(value, null, item.sets)))
        })
        compose.setContent { PowertrackerTheme { TodayContent(state, actions) } }
        compose.onNodeWithContentDescription("Options for Lat pulldown").performClick()
        compose.onNodeWithTag("exercise-menu-plates").performClick()
        waitFor { compose.onAllNodes(hasTestTag("plate-sheet")).fetchSemanticsNodes().isNotEmpty() }
        fun note() = compose.onAllNodes(hasTestTag("plate-note")).fetchSemanticsNodes().singleOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString()
        assertNull(note())
        compose.onNodeWithTag("plate-option-WHOLE").performClick()
        waitFor { note() == toPlates }
        // Whole ↔ half is not a change of unit: the note is the one for the unit the sheet opened in.
        compose.onNodeWithTag("plate-option-HALF").performClick()
        waitFor { state.items.single().plateCount == PlateCount.HALF }
        assertEquals(toPlates, note())
        compose.onNodeWithTag("plate-option-OFF").performClick()
        waitFor { note() == null }
        compose.onNodeWithTag("plate-done").performClick()

        // Opened in plates with plate sets: back to kg says the kg sentence. Opened in kg with no sets: nothing.
        state = TodayState(DAY, loading = false, sessionId = 1, items = listOf(plateItem(PlateCount.WHOLE, 7.0, listOf(logged(2, 1, 7.0, 10, LoadUnit.PLATES)))))
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Options for Lat pulldown").performClick()
        compose.onNodeWithTag("exercise-menu-plates").performClick()
        waitFor { compose.onAllNodes(hasTestTag("plate-sheet")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("plate-option-HALF").performClick()
        waitFor { state.items.single().plateCount == PlateCount.HALF }
        assertNull(note())
        compose.onNodeWithTag("plate-option-OFF").performClick()
        waitFor { note() == toKg }
        compose.onNodeWithTag("plate-done").performClick()

        state = TodayState(DAY, loading = false, sessionId = 1, items = listOf(plateItem(PlateCount.OFF, null)))
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Options for Lat pulldown").performClick()
        compose.onNodeWithTag("exercise-menu-plates").performClick()
        waitFor { compose.onAllNodes(hasTestTag("plate-sheet")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("plate-option-WHOLE").performClick()
        waitFor { state.items.single().plateCount == PlateCount.WHOLE }
        assertNull("no sets in kg: nothing to say", note())
    }

    private var stepState: androidx.compose.runtime.MutableState<TodayState>? = null

    /** Today with one plate item whose editor follows [onEdit], as the view model's does. Called again, it shows another item. */
    private fun stepper(count: PlateCount, weight: Double?): () -> SetEditor {
        val fresh = TodayState(DAY, loading = false, sessionId = 1, items = listOf(plateItem(count, weight)))
        val held = stepState
        if (held != null) {
            held.value = fresh
        } else {
            val state = mutableStateOf(fresh)
            stepState = state
            val actions = TodayActions(onEdit = { _, editor -> state.value = state.value.copy(items = listOf(state.value.items.single().copy(editor = editor))) })
            compose.setContent { PowertrackerTheme { TodayContent(state.value, actions) } }
        }
        compose.waitForIdle()
        val state = stepState!!
        return { state.value.items.single().editor }
    }

    private fun field() = compose.onNode(hasContentDescription("Weight, plates"))

    private fun fieldText() = field().fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    @Test
    fun theStepper_inWholePlates() {
        val editor = stepper(PlateCount.WHOLE, null)
        assertEquals("", fieldText())
        compose.onNodeWithContentDescription("Decrease Plate").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Increase Plate").performClick()
        compose.waitForIdle()
        assertEquals(1.0, editor().weightKg!!, 0.0)
        assertEquals(LoadUnit.PLATES, editor().unit)
        assertEquals("1", fieldText())
        compose.onNodeWithContentDescription("Decrease Plate").assertIsNotEnabled()

        val seven = stepper(PlateCount.WHOLE, 7.0)
        compose.onNodeWithContentDescription("Increase Plate").performClick()
        compose.waitForIdle()
        assertEquals(8.0, seven().weightKg!!, 0.0)
        assertEquals("8", fieldText())
    }

    @Test
    fun theStepper_inHalfSteps() {
        val editor = stepper(PlateCount.HALF, 7.0)
        compose.onNodeWithContentDescription("Increase Plate").performClick()
        compose.waitForIdle()
        assertEquals(7.5, editor().weightKg!!, 0.0)
        assertEquals("7.5", fieldText())
        compose.onNodeWithContentDescription("Decrease Plate").performClick()
        compose.onNodeWithContentDescription("Decrease Plate").performClick()
        compose.waitForIdle()
        assertEquals(6.5, editor().weightKg!!, 0.0)
    }

    @Test
    fun afterHalfStepsAreTurnedOff_7AndAHalfGoesTo7And8() {
        val up = stepper(PlateCount.WHOLE, 7.5)
        assertEquals("7.5", fieldText())
        compose.onNodeWithContentDescription("Increase Plate").performClick()
        compose.waitForIdle()
        assertEquals(8.0, up().weightKg!!, 0.0)
        val down = stepper(PlateCount.WHOLE, 7.5)
        compose.onNodeWithContentDescription("Decrease Plate").performClick()
        compose.waitForIdle()
        assertEquals(7.0, down().weightKg!!, 0.0)
    }

    @Test
    fun noPlatePicture_forAPlateExercise() {
        stepper(PlateCount.WHOLE, null)
        assertTrue("no \"No weight yet\"", !exists("No weight yet"))
        stepper(PlateCount.WHOLE, 7.0)
        assertTrue("no bar", compose.onAllNodesWithText("per side", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText("Can't load", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText("Lighter than the bar").fetchSemanticsNodes().isEmpty())
        // The same item in kg draws its note: the check above would see one.
        stepper(PlateCount.OFF, null)
        assertTrue(exists("No weight yet"))
    }

    @Test
    fun theFieldsAccessibleName_isWeightPlates() {
        stepper(PlateCount.WHOLE, null)
        field().assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasContentDescription("Weight, kilograms")).fetchSemanticsNodes().isEmpty())
        assertTrue("the placeholder", exists("plate"))
        assertTrue("no kg after the number", compose.onAllNodesWithText("kg").fetchSemanticsNodes().isEmpty())
        // With a number in it, still no unit after it.
        stepper(PlateCount.HALF, 12.5)
        assertEquals("12.5", fieldText())
        assertTrue(compose.onAllNodesWithText("kg").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aPlateSetRow_readsPlate7AndAHalf_andSpeaksIt() {
        compose.setContent {
            PowertrackerTheme {
                androidx.compose.foundation.layout.Column {
                    SetRow(1, 7.5, 12, 8.0, SetState.DONE, unit = LoadUnit.PLATES)
                    SetRow(2, 60.0, 10, 8.0, SetState.DONE)
                }
            }
        }
        compose.waitForIdle()
        assertTrue(compose.onAllNodes(hasText("Set 1: plate 7 and a half × 12 at RPE 8, done")).fetchSemanticsNodes().isNotEmpty())
        assertTrue("kg is as before", compose.onAllNodes(hasText("Set 2: 60 kg × 10 @ 8, done")).fetchSemanticsNodes().isNotEmpty())
        assertEquals("plate 7½ × 12 @ 8", doseText(7.5, 12, 8.0, LoadUnit.PLATES))
        assertEquals("60 × 10 @ 8", doseText(60.0, 10, 8.0))
    }

    @Test
    fun theChip_readsPlate7Times8To12() {
        val target = AdvicePayload.Target(1, 7.0, 8, 8.0, repsHigh = 12, unit = LoadUnit.PLATES)
        assertEquals("plate 7 × 8–12 @ 8", targetHeadline(target))
        assertEquals("plate 7 × 9 @ 8", targetHeadline(AdvicePayload.Target(1, 7.0, 9, 8.0, unit = LoadUnit.PLATES)))
        assertEquals("kg is as before", "26 kg × 8–12 @ 8", targetHeadline(AdvicePayload.Target(1, 26.0, 8, 8.0, repsHigh = 12)))
        val advice = Advice(
            AdviceType.TARGET, null, DAY, "Last time: plate 7 × 12, 12, 11. Beat it. Coach suggests plate 7 × 8–12 @ 8.", 0, target,
            id = 5, mode = TrainingMode.BODYBUILDING,
        )
        compose.setContent {
            PowertrackerTheme {
                TodayContent(TodayState(DAY, loading = false, sessionId = 1, items = listOf(plateItem(PlateCount.WHOLE, null, suggestion = advice))), TodayActions())
            }
        }
        compose.waitForIdle()
        assertTrue(exists("plate 7 × 8–12 @ 8"))
        assertTrue(exists("Last time: plate 7 × 12, 12, 11. Beat it."))
    }

    @Test
    fun theTypoDialog_namesPlates() {
        val set = LoggedSet(1, 12.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false, unit = LoadUnit.PLATES)
        val pending = PendingLog(null, 1, set, listOf(InputWarning.BigJump(3.0, LoadUnit.PLATES)))
        compose.setContent {
            PowertrackerTheme { TodayContent(TodayState(DAY, loading = false, sessionId = 1, items = listOf(plateItem(PlateCount.WHOLE, 12.0)), pending = pending), TodayActions()) }
        }
        compose.waitForIdle()
        assertTrue(exists("Did you mean plate 12 × 10?"))
        assertTrue(exists("That is a big jump from plate 3."))
    }

    @Test
    fun aSetLoggedBeforeTheSwitch_keepsItsUnit_whenItsMissPointIsSetOrItIsDeleted() {
        val lat = row("Lat pulldown")
        plan(lat, 0)
        var kgId = 0L
        var plateId = 0L
        runBlocking {
            val session = training.startSession(DAY, null, null)
            kgId = training.logSet(session, LoggedSet(lat, 60.0, 8, 8.0, failed = true, missPoint = null, isTopSet = false))
            training.setPlateCount(lat, null, PlateCount.WHOLE)
            plateId = training.logSet(session, LoggedSet(lat, 7.0, 8, 8.0, failed = false, missPoint = null, isTopSet = false, unit = LoadUnit.PLATES))
        }
        fun sets() = runBlocking { training.setsForSession(training.sessionOn(DAY).first()!!.id).first() }
        showScreen()
        waitFor { compose.onAllNodes(hasTestTag("miss-row-1")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodes(hasText("Set 1: 60 kg × 8 @ 8, failed")).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText("Set 2: plate 7 × 8 at RPE 8, done")).fetchSemanticsNodes().isNotEmpty())

        // The miss point is stored on the kg set, which stays a 60 kg set.
        compose.onNode(hasText("Floor") and androidx.compose.ui.test.hasAnyAncestor(hasTestTag("miss-row-1"))).performClick()
        waitFor { sets().first { it.id == kgId }.set.missPoint == MissPoint.FLOOR }
        val kg = sets().first { it.id == kgId }.set
        assertEquals(LoadUnit.KG, kg.unit)
        assertEquals(60.0, kg.weightKg, 0.0)
        waitFor { compose.onAllNodes(hasText("Set 1: 60 kg × 8 @ 8, failed")).fetchSemanticsNodes().isNotEmpty() }

        // The selected set's line is in its own unit.
        compose.onNode(hasText("Set 2: plate 7 × 8 at RPE 8, done")).performClick()
        waitFor { exists("plate 7 × 8 @ 8") }
        compose.onNodeWithText("Delete set").performClick()
        waitFor { sets().none { it.id == plateId } }
        assertEquals(listOf(kg), sets().map { it.set })

        compose.onNode(hasText("Set 1: 60 kg × 8 @ 8, failed")).performClick()
        waitFor { exists("60 × 8 @ 8 · failed") }
    }
}

/** Review Focus 5: a 320 dp phone with twice the font size. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class PlateCountNarrowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        RuntimeEnvironment.setFontScale(1f)
    }

    private fun ComposeTestRule.fieldFits() {
        val node = onNode(hasContentDescription("Weight, plates")).performScrollTo().assertIsDisplayed().fetchSemanticsNode()
        assertEquals("12.5", node.config[SemanticsProperties.EditableText].text)
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val screen = onRoot().fetchSemanticsNode().boundsInRoot
        for (layout in layouts) {
            val widest = (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
            assertTrue("12.5 is wider than its field ($widest in ${node.size.width})", widest <= node.size.width + 1f)
        }
        assertTrue("the field is on the screen", node.boundsInRoot.left >= 0f && node.boundsInRoot.right <= screen.right + 0.5f)
        assertEquals("the field is not cut", node.size.width.toFloat(), node.boundsInRoot.width, 1f)
    }

    @Test
    fun at320dpAndTwiceTheFontSize_nothingIsCut() {
        RuntimeEnvironment.setFontScale(2f)
        val target = AdvicePayload.Target(1, 12.5, 8, 8.0, repsHigh = 12, unit = LoadUnit.PLATES)
        val advice = Advice(AdviceType.TARGET, null, DAY, "Last time: plate 12 × 12, 12, 12. Time to add weight. Coach suggests plate 12½ × 8–12 @ 8.", 0, target, id = 5, mode = TrainingMode.BODYBUILDING)
        var state by mutableStateOf(
            TodayState(
                DAY, loading = false, sessionId = 1, mode = TrainingMode.BODYBUILDING,
                items = listOf(plateItem(PlateCount.HALF, 12.5, listOf(logged(1, 1, 12.0, 12, LoadUnit.PLATES)), advice)),
            ),
        )
        val actions = TodayActions(onCountPlates = { _, value -> state = state.copy(items = listOf(state.items.single().copy(plateCount = value))) })
        scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            assertEquals(2f, activity.resources.configuration.fontScale, 0.001f)
            activity.setContent { PowertrackerTheme { TodayContent(state, actions) } }
        }
        compose.waitForIdle()

        compose.onNodeWithText("plate 12½ × 8–12 @ 8").performScrollTo().assertIsDisplayed()
        compose.assertTextsFit()
        compose.fieldFits()
        compose.assertTextsFit()

        compose.onNodeWithContentDescription("Options for Lat pulldown").performScrollTo().performClick()
        compose.onNodeWithTag("exercise-menu-plates").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("plate-done")).fetchSemanticsNodes().isNotEmpty() }
        // Every text in the sheet, scrolled to: laid out whole, inside the sheet's width, and not cut by a parent.
        val inSheet = androidx.compose.ui.test.hasAnyAncestor(hasTestTag("plate-sheet"))
        val sheetWidth = compose.onNodeWithTag("plate-sheet").fetchSemanticsNode().boundsInRoot.right
        val texts = compose.onAllNodes(inSheet and androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes().map { it.config[SemanticsProperties.Text].joinToString() }
        for (text in listOf("Lat pulldown", "How does this machine show its weight?", "In kg", "Whole plates", "1, 2, 3 …", "Half steps too", "1, 1½, 2 …", "Done")) {
            assertTrue("\"$text\" is in the sheet", text in texts)
        }
        for (text in texts) {
            val node = compose.onNode(inSheet and hasText(text), useUnmergedTree = true).performScrollTo().assertIsDisplayed().fetchSemanticsNode()
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            for (layout in layouts) {
                val widest = (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
                assertTrue("\"$text\" is wider than its box", widest <= layout.size.width + 1f)
                assertTrue("\"$text\" is ellipsised", (0 until layout.lineCount).none { layout.isLineEllipsized(it) } && !layout.multiParagraph.didExceedMaxLines)
            }
            assertTrue("\"$text\" runs out of the sheet", node.positionInRoot.x >= -0.5f && node.positionInRoot.x + node.size.width <= sheetWidth + 0.5f)
            assertTrue("\"$text\" is cut by a parent", node.boundsInRoot.width >= node.size.width - 1f && node.boundsInRoot.height >= node.size.height - 1f)
        }
        compose.onNodeWithTag("plate-done").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("plate-done").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("plate-sheet")).fetchSemanticsNodes().isEmpty() }
    }
}
