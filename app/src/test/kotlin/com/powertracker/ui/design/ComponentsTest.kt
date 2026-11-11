package com.powertracker.ui.design

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.powertracker.ui.components.AdviceCard
import com.powertracker.ui.components.PlateStrip
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SetRow
import com.powertracker.ui.components.SetState
import com.powertracker.ui.components.Stepper
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.platesLabel
import com.powertracker.ui.theme.DarkPtColors
import com.powertracker.ui.theme.LightPtColors
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Tone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
}
