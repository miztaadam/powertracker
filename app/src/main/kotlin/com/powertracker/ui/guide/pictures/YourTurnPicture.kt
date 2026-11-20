package com.powertracker.ui.guide.pictures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.guide.PHONE_STATUS_BAR

/**
 * Screen 8 (1.16): the Plan tab as it opens on the lifter's side with no plan yet, in that side's colours: what the
 * screen's own buttons (the guide's, below the picture) lead to. One step, no motion, nothing ringed.
 */
@Composable
internal fun YourTurnPicture(mode: TrainingMode) {
    Box(Modifier.fillMaxSize().padding(top = PHONE_STATUS_BAR)) { EmptyPlan(mode) }
}
