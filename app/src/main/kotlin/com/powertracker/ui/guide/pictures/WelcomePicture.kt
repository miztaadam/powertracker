package com.powertracker.ui.guide.pictures

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.powertracker.R
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** The mark's size on the drawn phone: a launcher icon, large. */
private val MARK = 168.dp

/** How far the two sides rise as they come in, in dp. */
private const val SIDES_RISE = 24f

/** The words for each side: the app's own mode names. */
internal fun sideName(mode: TrainingMode): String = if (mode == TrainingMode.BODYBUILDING) "Bodybuilding" else "Powerlifting"

/**
 * Screen 1 (1.16): the app's mark (its launcher icon, background and foreground) at [GuideStep.WELCOME_MARK]; then at
 * [GuideStep.WELCOME_SIDES] the two sides' colours come in under it, rising and fading in with the move ([frame]'s
 * progress). No numbers, nothing from the sample.
 */
@Composable
internal fun WelcomePicture(frame: GuideFrame) {
    val sides = frame.step == GuideStep.WELCOME_SIDES
    val shown = if (sides) frame.progress.toFloat() else 0f
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val rise = with(LocalDensity.current) { SIDES_RISE.dp.toPx() }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = Space.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xl, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(MARK).clip(RoundedCornerShape(MARK / 4))) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
        }
        Row(
            Modifier.fillMaxWidth().graphicsLayer {
                alpha = shown
                translationY = rise * (1f - shown)
            },
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            for (side in TrainingMode.entries) {
                // Each side in its own colours, in the scheme the guide is in.
                PowertrackerTheme(mode = side, darkTheme = dark) {
                    val pt = PowertrackerTheme.colors
                    Box(
                        Modifier.weight(1f).height(120.dp).clip(MaterialTheme.shapes.large).background(pt.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(sideName(side), style = MaterialTheme.typography.titleMedium, color = pt.onAccent)
                    }
                }
            }
        }
    }
}
