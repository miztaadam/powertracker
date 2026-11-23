package com.powertracker.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.theme.Space

const val WELCOME_BACK_TITLE = "Welcome back to the better side."

/**
 * Back on a side after more than a month on the other (bodybuilding mode spec §7.7). Either answer puts the card
 * away; [onGoalsChanged] also opens the Plan, so the lifter can change it.
 */
@Composable
fun WelcomeBackCard(onResume: () -> Unit, onGoalsChanged: () -> Unit, modifier: Modifier = Modifier) {
    PtCard(modifier.testTag("welcome-back")) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Eyebrow("Coach")
            Text(WELCOME_BACK_TITLE, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(
                "Have your goals changed, or do you want to resume the same plan?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            PrimaryButton("Resume my plan", onResume, Modifier.fillMaxWidth())
            SecondaryButton("My goals have changed", onGoalsChanged, Modifier.fillMaxWidth())
        }
    }
}
