package com.powertracker.ui.guide.pictures

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.min
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.powertracker.R
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** The mark's share of the picture's width and of its height. */
private const val MARK_OF_WIDTH = 0.5f
private const val MARK_OF_HEIGHT = 0.4f

/** How far the two sides rise as they come in, in dp. */
private const val SIDES_RISE = 24f

/** The words for each side: the app's own mode names. */
internal fun sideName(mode: TrainingMode): String = if (mode == TrainingMode.BODYBUILDING) "Bodybuilding" else "Powerlifting"

/**
 * Screen 1 (1.16): the app's mark (its launcher icon, background and foreground) at [GuideStep.WELCOME_MARK]; then at
 * [GuideStep.WELCOME_SIDES] the two sides' colours come in under it, rising and fading in with the move ([frame]'s
 * progress). No phone: the art fills the picture's place. Each side is a plain colour swatch, its name under it as a
 * caption: nothing about it is a button (no button shape, no press, no click, no focus). No numbers.
 */
@Composable
internal fun WelcomePicture(frame: GuideFrame) {
    val sides = frame.step == GuideStep.WELCOME_SIDES
    val shown = if (sides) frame.progress.toFloat() else 0f
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val rise = with(LocalDensity.current) { SIDES_RISE.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val mark = min(maxWidth * MARK_OF_WIDTH, maxHeight * MARK_OF_HEIGHT)
        // Each swatch a square, as wide as half the row, never taller than what the mark leaves.
        val swatch = min((maxWidth - Space.m) / 2, (maxHeight - mark) * 0.5f).coerceAtLeast(0.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xl, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(mark).clip(RoundedCornerShape(mark / 4))) {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
            }
            Row(
                Modifier.testTag("guide-welcome-sides").graphicsLayer {
                    alpha = shown
                    translationY = rise * (1f - shown)
                },
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                for (side in TrainingMode.entries) {
                    // Each side in its own colours, in the scheme the guide is in: its background, its accent in it.
                    PowertrackerTheme(mode = side, darkTheme = dark) {
                        val pt = PowertrackerTheme.colors
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.s)) {
                            Box(
                                Modifier.testTag("guide-side-panel").size(swatch).background(MaterialTheme.colorScheme.background).border(1.dp, pt.hairline),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.fillMaxSize(0.5f).background(pt.accent, CircleShape))
                            }
                            Text(sideName(side), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    }
}
