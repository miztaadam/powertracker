package com.powertracker.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.powertracker.data.StoredSet
import com.powertracker.domain.coach.WarmUp
import com.powertracker.domain.coach.WarmUpSet
import com.powertracker.domain.history.LastTime
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.domain.stack.PlateStack
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState
import com.powertracker.ui.today.warmUpHint
import com.powertracker.ui.today.warmUpSpoken
import com.powertracker.ui.today.warmUpText
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The warm-up checklist on the Today card (1.23), from the card's content composable. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class WarmUpBlockTest {
    @get:Rule
    val compose = createComposeRule()

    private val date = LocalDate(2026, 11, 11)
    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)

    /** The squat's card as the view model builds it: [kg] in the field, the warm-up worked out for it. */
    private fun card(kg: Double, sets: List<StoredSet> = emptyList(), exercise: Exercise = squat, unit: LoadUnit = LoadUnit.KG) = TodayItem(
        exercise = exercise,
        planned = PlanItem(1, DayOfWeek.WEDNESDAY, exercise.id, 5, 5, 8.0, 0),
        suggestion = null,
        sets = sets,
        editor = SetEditor(kg, 5, 8.0, unit),
        targetSets = 5,
        perSide = if (unit == LoadUnit.KG) PlateLoader.perSide(kg, false) else null,
        lastWeightKg = kg,
        unit = unit,
        plateCount = exercise.plateCount,
        lastTime = LastTime(LocalDate(2026, 11, 4), listOf(LoggedSet(exercise.id, kg, 5, 7.0, false, null, false, unit))),
        warmUp = if (sets.isEmpty()) WarmUp.sets(exercise, kg, 5, false) else emptyList(),
    )

    /** Shows the card and keeps the ticks as the view model does; returns what was asked of it. */
    private fun show(item: TodayItem, fontScale: Float = 1f, mode: TrainingMode = TrainingMode.POWERLIFTING): MutableList<Triple<Long, WarmUpSet, Boolean>> {
        val asked = mutableListOf<Triple<Long, WarmUpSet, Boolean>>()
        compose.setContent {
            var done by androidx.compose.runtime.remember { mutableStateOf(emptySet<WarmUpSet>()) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = fontScale)) {
                PowertrackerTheme(mode = mode) {
                    TodayContent(
                        TodayState(date, loading = false, sessionId = 1, items = listOf(item.copy(warmUpDone = done)), mode = mode),
                        TodayActions(onTickWarmUp = { id, set, on ->
                            asked += Triple(id, set, on)
                            done = if (on) done + set else done - set
                        }),
                    )
                }
            }
        }
        compose.waitForIdle()
        return asked
    }

    private fun top(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().top
    private val first = "Warm-up set 1: 75 kg × 12, 25 + 2.5 per side"
    private val second = "Warm-up set 2: 112.5 kg × 5, 25 + 20 + 1.25 per side"

    @Test
    fun wording_isTheWebApps() {
        val (half, threeQuarters) = WarmUp.sets(squat, 100.0, 5, false)
        assertEquals(listOf("50 kg × 12", "15 per side", "Warm-up set 1: 50 kg × 12, 15 per side"), listOf(warmUpText(half), warmUpHint(half), warmUpSpoken(1, half)))
        assertEquals(listOf("75 kg × 5", "25 + 2.5 per side"), listOf(warmUpText(threeQuarters), warmUpHint(threeQuarters)))
        assertEquals("empty bar", warmUpHint(WarmUpSet(20.0, 12, LoadUnit.KG, emptyList())))
        assertNull(warmUpHint(WarmUpSet(12.5, 12, LoadUnit.KG)))
        val plate = WarmUpSet(3.5, 12, LoadUnit.PLATES)
        assertEquals("${PlateStack.text(3.5)} × 12", warmUpText(plate))
        assertEquals("Warm-up set 1: ${PlateStack.spoken(3.5)} × 12", warmUpSpoken(1, plate))
    }

    @Test
    fun card_showsTheChecklist_betweenLastTimeAndTheWeightEntry_eachRowATouchTargetToTick() {
        val asked = show(card(150.0))
        compose.onNodeWithTag("warm-up", useUnmergedTree = true).assertExists()
        // The heading says what a tick is not.
        compose.onNodeWithText("WARM-UP · NOT LOGGED", useUnmergedTree = true).assertExists()
        assertTrue(top("LAST TIME · WED 4 NOV") < top("WARM-UP · NOT LOGGED"))
        assertTrue(top("WARM-UP · NOT LOGGED") < compose.onNodeWithContentDescription("Weight, kilograms").getUnclippedBoundsInRoot().top)
        // What is drawn: the set, then its plates.
        for (text in listOf("75 kg × 12", "25 + 2.5 per side", "112.5 kg × 5", "25 + 20 + 1.25 per side")) {
            compose.onNodeWithText(text, useUnmergedTree = true).assertExists()
        }
        // Each row is one control, read as one sentence, a full touch target high.
        compose.onNodeWithContentDescription(first).assertIsOff().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription(second).assertIsOff().assertHeightIsAtLeast(48.dp)

        compose.onNodeWithContentDescription(first).performClick()
        compose.onNodeWithContentDescription(first).assertIsOn()
        compose.onNodeWithContentDescription(second).assertIsOff()
        compose.onNodeWithContentDescription(first).performClick()
        compose.onNodeWithContentDescription(first).assertIsOff()
        val half = WarmUpSet(75.0, 12, LoadUnit.KG, listOf(25.0, 2.5))
        assertEquals(listOf(Triple(squat.id, half, true), Triple(squat.id, half, false)), asked)
    }

    @Test
    fun card_theChecklistFollowsTheTyping_beforeTheFieldIsLeft() {
        show(card(150.0))
        compose.onNodeWithContentDescription("Weight, kilograms").performTextReplacement("100")
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Warm-up set 1: 50 kg × 12, 15 per side").assertExists()
        compose.onNodeWithContentDescription("Warm-up set 2: 75 kg × 5, 25 + 2.5 per side").assertExists()
        compose.onNodeWithContentDescription(first).assertDoesNotExist()
        // The bar itself has nothing lighter to load.
        compose.onNodeWithContentDescription("Weight, kilograms").performTextReplacement("20")
        compose.waitForIdle()
        compose.onNodeWithTag("warm-up", useUnmergedTree = true).assertDoesNotExist()
        // Just above it: the bar alone, once.
        compose.onNodeWithContentDescription("Weight, kilograms").performTextReplacement("25")
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Warm-up set 1: 20 kg × 12, empty bar").assertExists()
        compose.onAllNodesWithText("Warm-up set 2", substring = true, useUnmergedTree = true).fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
    }

    @Test
    fun card_withASetLogged_orNothingToSuggest_hasNoChecklist_andLeavesNoGap() {
        // The item still carries sets to tick (it should not, once a set is logged): the card shows none.
        val logged = listOf(StoredSet(1, 1, LoggedSet(squat.id, 150.0, 5, 8.0, false, null, false)))
        show(card(150.0, logged).copy(warmUp = WarmUp.sets(squat, 150.0, 5, false)))
        compose.onNodeWithTag("warm-up", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun card_aPlateCountedMachine_isInPlateNumbers_withNoPlatesPerSide() {
        val pulldown = Exercise(9, "Lat pulldown", Category.ACCESSORY, null, emptyList(), isBuiltIn = true, plateCount = PlateCount.HALF)
        show(card(7.0, exercise = pulldown, unit = LoadUnit.PLATES), mode = TrainingMode.BODYBUILDING)
        compose.onNodeWithContentDescription("Warm-up set 1: ${PlateStack.spoken(3.5)} × 12").assertIsOff()
        compose.onNodeWithText("${PlateStack.text(3.5)} × 12", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("per side", substring = true, useUnmergedTree = true).fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun card_smallPhoneAndLargeFont_theLongestRowsWrapWithoutClipping_tickedOrNot() {
        // 187.5 kg: 92.5 (25 + 10 + 1.25) and 140 (25 + 25 + 10), then the longest plate list there is at 247.5.
        for (kg in listOf(187.5, 247.5)) {
            val sets = WarmUp.sets(squat, kg, 5, false)
            assertEquals(2, sets.size)
        }
        val kg = 247.5
        val sets = WarmUp.sets(squat, kg, 5, false)
        show(card(kg), fontScale = 1.3f)
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        fun check(state: String) {
            sets.forEachIndexed { i, set ->
                val row = compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set))
                val b = row.getUnclippedBoundsInRoot()
                assertTrue("row ${i + 1} fits across the screen ($state)", b.left >= screen.left && b.right <= screen.right)
                val n = row.fetchSemanticsNode()
                assertEquals("row ${i + 1} is not clipped ($state)", n.size.height.toFloat(), n.boundsInRoot.height, 0.5f)
                assertEquals("row ${i + 1} is not cut at the side ($state)", n.size.width.toFloat(), n.boundsInRoot.width, 0.5f)
                // Every letter of the set and of its plates is on a line that is drawn.
                for (text in listOf(warmUpText(set), warmUpHint(set)!!)) {
                    val node = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
                    val layout = mutableListOf<TextLayoutResult>().also { node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.single()
                    assertEquals("\"$text\" is whole ($state)", text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
                    assertFalse("\"$text\" is cut at the bottom ($state)", layout.didOverflowHeight)
                    assertEquals("\"$text\" is not clipped ($state)", node.size.width.toFloat(), node.boundsInRoot.width, 0.5f)
                }
            }
            compose.assertTextsFit()
        }
        check("unticked")
        sets.forEachIndexed { i, set -> compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set)).performClick() }
        compose.waitForIdle()
        sets.forEachIndexed { i, set -> compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set)).assertIsOn() }
        check("ticked")
    }
}
