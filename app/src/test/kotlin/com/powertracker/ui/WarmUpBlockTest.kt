package com.powertracker.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import com.powertracker.ui.today.warmUpPlates
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
        warmUpOpen = sets.isEmpty(),
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
        assertEquals(listOf("75 kg × 5", "25 + 2.5 per side", "25 + 2.5"), listOf(warmUpText(threeQuarters), warmUpHint(threeQuarters), warmUpPlates(threeQuarters)))
        assertEquals("empty bar", warmUpPlates(WarmUpSet(20.0, 12, LoadUnit.KG, emptyList())))
        assertNull(warmUpPlates(WarmUpSet(12.5, 12, LoadUnit.KG)))
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
        for (text in listOf("75 kg × 12", "25 + 2.5", "112.5 kg × 5", "25 + 20 + 1.25")) {
            compose.onNodeWithText(text, useUnmergedTree = true).assertExists()
        }
        // "per side" is said once, in the heading (and to TalkBack with each set).
        compose.onAllNodes(hasText("per side", substring = true, ignoreCase = true) and hasAnyAncestor(hasTestTag("warm-up")), useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("PLATES PER SIDE", useUnmergedTree = true).assertExists()
        // Side by side on a phone this wide, so the card stays short: both start on one line, the second to the right,
        // inside the screen, and as tall as each other.
        val a = compose.onNodeWithContentDescription(first).getUnclippedBoundsInRoot()
        val b = compose.onNodeWithContentDescription(second).getUnclippedBoundsInRoot()
        assertEquals(a.top, b.top)
        assertEquals(a.bottom, b.bottom)
        assertTrue(b.left >= a.right && b.right <= compose.onRoot().getUnclippedBoundsInRoot().right)
        assertEquals("the two sets' text starts on one line", top("75 kg × 12"), top("112.5 kg × 5"))
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
    fun card_theWeightEntryKeepsStill_whateverThePlates() {
        // From one plate a side to five: the checklist is one height, so the − and + under it do not move.
        show(card(100.0))
        fun weightTop() = compose.onNodeWithContentDescription("Weight, kilograms").getUnclippedBoundsInRoot().top
        val top = weightTop()
        for (kg in listOf("102.5", "105", "117.5", "157.5", "187.5", "247.5")) {
            compose.onNodeWithContentDescription("Weight, kilograms").performTextReplacement(kg)
            compose.waitForIdle()
            compose.onNodeWithTag("warm-up", useUnmergedTree = true).assertExists()
            assertEquals("the weight field's top at $kg kg", top, weightTop())
        }
    }

    @Test
    fun card_whoseWarmUpIsOver_hasNoChecklist_evenWithNoSetLeft_andTypingDoesNotBringItBack() {
        // A set was logged and deleted again: no set on the card, but the item says the warm-up is over.
        show(card(150.0).copy(warmUpOpen = false, warmUp = emptyList()))
        compose.onNodeWithTag("warm-up", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Weight, kilograms").performTextReplacement("100")
        compose.waitForIdle()
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
        // 247.5 kg: 122.5 kg × 12 (25 + 25 + 1.25) and 185 kg × 5 (25 + 25 + 25 + 5 + 2.5), long rows both.
        val kg = 247.5
        val sets = WarmUp.sets(squat, kg, 5, false)
        assertEquals(listOf(122.5, 185.0), sets.map { it.weight })
        show(card(kg), fontScale = 1.3f)
        // Too narrow for two side by side at this font: one under the other.
        val rows = sets.mapIndexed { i, set -> compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set)).getUnclippedBoundsInRoot() }
        assertTrue("stacked", rows[1].top >= rows[0].bottom)
        fun check(state: String) = assertRowsWhole(sets, state)
        check("unticked")
        sets.forEachIndexed { i, set -> compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set)).performClick() }
        compose.waitForIdle()
        sets.forEachIndexed { i, set -> compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set)).assertIsOn() }
        check("ticked")
    }

    /** Every row inside the screen and unclipped, and every letter of its set and its plates on a line that is drawn. */
    private fun assertRowsWhole(sets: List<WarmUpSet>, state: String) {
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        sets.forEachIndexed { i, set ->
            val row = compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set))
            val b = row.getUnclippedBoundsInRoot()
            assertTrue("row ${i + 1} fits across the screen ($state)", b.left >= screen.left && b.right <= screen.right)
            row.assertHeightIsAtLeast(48.dp)
            val n = row.fetchSemanticsNode()
            assertEquals("row ${i + 1} is not clipped ($state)", n.size.height.toFloat(), n.boundsInRoot.height, 0.5f)
            assertEquals("row ${i + 1} is not cut at the side ($state)", n.size.width.toFloat(), n.boundsInRoot.width, 0.5f)
            for (text in listOf(warmUpText(set), warmUpPlates(set)!!)) {
                val node = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
                val layout = mutableListOf<TextLayoutResult>().also { node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.single()
                assertEquals("\"$text\" is whole ($state)", text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
                assertFalse("\"$text\" is cut at the bottom ($state)", layout.didOverflowHeight)
                assertEquals("\"$text\" is not clipped ($state)", node.size.width.toFloat(), node.boundsInRoot.width, 0.5f)
            }
        }
        compose.assertTextsFit()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w360dp-h2400dp-xhdpi")
    fun card_at360dp_theLongestRowsSitSideBySide_whole_tickedOrNot() {
        val sets = WarmUp.sets(squat, 247.5, 5, false)
        show(card(247.5))
        val rows = sets.mapIndexed { i, set -> compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set)).getUnclippedBoundsInRoot() }
        assertEquals("side by side", rows[0].top, rows[1].top)
        assertTrue(rows[1].left >= rows[0].right)
        assertRowsWhole(sets, "unticked")
        sets.forEachIndexed { i, set -> compose.onNodeWithContentDescription(warmUpSpoken(i + 1, set)).performClick() }
        compose.waitForIdle()
        assertRowsWhole(sets, "ticked")
    }
}
