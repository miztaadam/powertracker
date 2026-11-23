package com.powertracker.ui.design

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.powertracker.ui.components.AdviceCard
import com.powertracker.ui.components.PlateStrip
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SetRow
import com.powertracker.ui.components.SetState
import com.powertracker.ui.components.Stepper
import com.powertracker.ui.components.StepperEntry
import com.powertracker.ui.components.StepperField
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.platesLabel
import com.powertracker.ui.theme.DarkPtColors
import com.powertracker.ui.theme.LightPtColors
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class ComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theme_alwaysUsesBrandPalette_notDynamicColour() {
        var dark = Color.Unspecified
        var light = Color.Unspecified
        compose.setContent {
            PowertrackerTheme(darkTheme = true) { dark = MaterialTheme.colorScheme.primary }
            PowertrackerTheme(darkTheme = false) { light = PowertrackerTheme.colors.accent }
        }
        compose.waitForIdle()
        assertEquals(DarkPtColors.accent, dark)
        assertEquals(LightPtColors.accent, light)
    }

    @Test
    fun stepper_keepsAccessibleButtons_andRespectsLimits() {
        var value = 1
        compose.setContent {
            PowertrackerTheme { Stepper("Sets", value.toString(), canDecrement = false, canIncrement = true, onDecrement = { value-- }, onIncrement = { value++ }) }
        }
        compose.onNodeWithContentDescription("Decrease Sets").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Increase Sets").assertIsEnabled().performClick()
        assertEquals(2, value)
    }

    @Test
    fun compactButtons_keepA48dpTouchTarget() {
        compose.setContent {
            PowertrackerTheme {
                Column {
                    PrimaryButton("Apply", {}, compact = true)
                    SecondaryButton("+30 s", {}, compact = true)
                    QuietButton("Skip", {})
                }
            }
        }
        for (label in listOf("Apply", "+30 s", "Skip")) {
            compose.onNodeWithText(label).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
    }

    @Test
    fun setRow_readsAsOneLine_andNamesItsState() {
        compose.setContent {
            PowertrackerTheme { SetRow(index = 3, weightKg = 152.5, reps = 3, rpe = 9.5, state = SetState.FAILED) }
        }
        compose.onNodeWithText("Set 3: 152.5 kg × 3 @ 9.5, failed").assertExists()
    }

    @Test
    fun plateStrip_labelsEachSide() {
        assertEquals("25 + 25 + 15 + 2.5", platesLabel(listOf(25.0, 25.0, 15.0, 2.5)))
        assertEquals("Empty bar", platesLabel(emptyList()))
        assertEquals("152.5", formatKg(152.5))
        assertEquals("155", formatKg(155.0))
        compose.setContent { PowertrackerTheme { PlateStrip(listOf(25.0, 20.0, 1.25)) } }
        compose.onNodeWithText("25 + 20 + 1.25").assertExists()
    }

    @Test
    fun adviceCard_showsTextAndFiresActions() {
        var applied = 0
        var dismissed = 0
        compose.setContent {
            PowertrackerTheme {
                AdviceCard(
                    tone = Tone.WARNING, icon = Icons.Filled.Insights, tag = "Stall · Bench", title = "Bench has stalled",
                    body = "Flat for 3 weeks.", primaryAction = "Add to plan", onPrimary = { applied++ },
                    secondaryAction = "Dismiss", onSecondary = { dismissed++ },
                )
            }
        }
        compose.onNodeWithText("Bench has stalled").assertExists()
        compose.onNodeWithText("Add to plan").performClick()
        compose.onNodeWithText("Dismiss").performClick()
        assertEquals(1 to 1, applied to dismissed)
    }

    @Test
    fun stepperField_valueReadsAsOnePhrase() {
        compose.setContent {
            PowertrackerTheme { StepperField("Weight", "182.5", true, true, {}, {}, unit = "kg") }
        }
        compose.onNodeWithContentDescription("Weight 182.5 kg").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun stepperField_withEntry_isTypedInto() {
        var text by mutableStateOf("182.5")
        var error by mutableStateOf<String?>(null)
        val commits = mutableListOf<String>()
        var steps = 0
        compose.setContent {
            PowertrackerTheme {
                StepperField(
                    "Weight", text, true, true, { steps-- }, { steps++ }, unit = "kg",
                    entry = StepperEntry(KeyboardType.Decimal, "kg", error, onDraft = { text = it }, onCommit = { commits += it }, label = "Weight, kilograms"),
                )
            }
        }
        val field = compose.onNodeWithContentDescription("Weight, kilograms")
        fun typed() = field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        // The figure is the field: no second node reads the value out.
        compose.onNodeWithContentDescription("Weight 182.5 kg").assertDoesNotExist()
        field.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        assertEquals("182.5", typed())
        // A tap selects the value, so typing replaces it.
        field.performClick()
        field.assertIsFocused()
        compose.waitUntil { field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange] == TextRange(0, 5) }
        field.performTextInput("90")
        assertEquals("90", text)
        assertEquals("90", typed())
        assertTrue("typing alone commits nothing", commits.isEmpty())

        // The buttons leave the cursor where it is.
        compose.onNodeWithContentDescription("Increase Weight").performClick()
        assertEquals(1, steps)
        field.assertIsFocused()
        assertTrue(commits.isEmpty())

        // Done leaves the field, and leaving it commits once.
        field.performImeAction()
        field.assertIsNotFocused()
        assertEquals(listOf("90"), commits)

        // A value set from outside shows in the field.
        text = "155"
        compose.waitForIdle()
        assertEquals("155", typed())

        // A refused entry is read with the field and shown under it.
        error = "Numbers only"
        compose.waitForIdle()
        assertEquals("Numbers only", field.fetchSemanticsNode().config[SemanticsProperties.Error])
        compose.onNodeWithText("Numbers only").assertExists()

        // Empty: the placeholder stands in and the unit steps aside.
        text = ""
        compose.waitForIdle()
        assertEquals(listOf("kg"), field.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text })
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w360dp-h400dp-xhdpi")
    fun stepperField_withEntry_drawsItsFigures_centred() {
        compose.setContent {
            PowertrackerTheme(darkTheme = false) {
                // The Today card on a small phone: 296 dp for three across.
                Row(Modifier.width(296.dp).background(Color.White), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    for ((label, value, share) in listOf(Triple("Weight", "182.5", 1.25f), Triple("Reps", "5", 1f), Triple("RPE", "7.5", 1f))) {
                        StepperField(label, value, true, true, {}, {}, Modifier.weight(share), unit = "kg".takeIf { label == "Weight" }, entry = StepperEntry(KeyboardType.Decimal, null, null, {}, {}))
                    }
                }
            }
        }
        compose.waitForIdle()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        for (label in listOf("Weight", "Reps", "RPE")) {
            val field = compose.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot
            // Columns of the field with ink in them, leaving out the rule along its bottom edge.
            val inked = (field.left.toInt() until field.right.toInt()).filter { x ->
                (field.top.toInt() until field.bottom.toInt() - 8).any { y -> Color(bitmap.getPixel(x, y)).luminance() < 0.3f }
            }
            assertTrue("$label: the figures are drawn", inked.size > 12)
            val ink = (inked.first() + inked.last() + 1) / 2f
            assertEquals("$label: the figures sit in the middle of the field", field.center.x, ink, 6f)
        }
    }
}
