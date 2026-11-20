package com.powertracker.ui.guide.pictures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideSideFrame
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.motion.ModeSwitchMotion
import com.powertracker.ui.motion.buildDropArt
import com.powertracker.ui.motion.buildPumpArt
import com.powertracker.ui.motion.drawDrop
import com.powertracker.ui.motion.drawPump
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/**
 * Where the shown screen 2's loop is, for tests (the guide's picture keeps its content out of the accessibility tree):
 * call it for the two motions' times now; null while no screen 2 is composed.
 */
internal var sideLoopNow: (() -> GuideSideFrame)? = null

/**
 * How many times the picture has been composed, for tests: the loop moves on every frame, but only in the draw phase,
 * so this stays put while it plays.
 */
internal var sidePictureCompositions: Int = 0

/**
 * Screen 2 (1.16): the mode switch's hammer (powerlifting) and pump (bodybuilding), cut down and side by side, each in
 * its own box in its side's colours, drawn with the switch's own paint ([drawDrop], [drawPump]) and the domain's loop
 * ([Guide.sideFrame]). Silent: it never reaches the mode switch's controller, so no sound and no vibration.
 *
 * Its one step is settled at once, so the guide's clock has no time for it: the loop runs on the frame clock while
 * motion is on and the guide is on screen (lifecycle STARTED), and is read only when drawn. With motion off it is
 * still, at [Guide.SIDE_STILL].
 */
@Composable
internal fun SidePicture() {
    SideEffect { sidePictureCompositions++ }
    val motion = LocalMotion.current
    // -1 while still; else ms into the loop.
    val loop = remember { mutableLongStateOf(-1L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(motion, lifecycle) {
        if (!motion) {
            loop.longValue = -1L
            return@LaunchedEffect
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Goes on from where it stood.
            val from = loop.longValue.coerceAtLeast(0L)
            val start = withInfiniteAnimationFrameMillis { it } - from
            while (true) withInfiniteAnimationFrameMillis { loop.longValue = it - start }
        }
    }
    val now = remember { { sideFrameOf(loop) } }
    DisposableEffect(now) {
        sideLoopNow = now
        onDispose { if (sideLoopNow === now) sideLoopNow = null }
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    // No phone: the two boxes fill the picture's place, side by side, as large as it allows with their names under them.
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val ratio = (Guide.SIDE_BOX_W / Guide.SIDE_BOX_H).toFloat()
        val boxWidth = min((maxWidth - Space.m) / 2, (maxHeight - LABEL_ROOM) * ratio).coerceAtLeast(0.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            for (side in TrainingMode.entries) {
                PowertrackerTheme(mode = side, darkTheme = dark) {
                    Column(Modifier.width(boxWidth), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        SideBox(side, now)
                        Text(sideName(side), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}

/** Under each box: its side's name and the gap above it. */
private val LABEL_ROOM = 40.dp

private fun sideFrameOf(loop: MutableLongState): GuideSideFrame {
    val t = loop.longValue
    return if (t < 0) Guide.SIDE_STILL else Guide.sideFrame(t)
}

/** One side's motion in its box, in that side's own background; [now] is read only while drawing. */
@Composable
private fun SideBox(side: TrainingMode, now: () -> GuideSideFrame) {
    val w = Guide.SIDE_BOX_W
    val h = Guide.SIDE_BOX_H
    val light = MaterialTheme.colorScheme.background.luminance() > .5f
    val drop = remember { if (side == TrainingMode.POWERLIFTING) buildDropArt(w, h) else null }
    val src = remember { Guide.sidePumpSource() }
    val pump = remember { if (side == TrainingMode.BODYBUILDING) buildPumpArt(w, h, src) else null }
    Canvas(
        Modifier.fillMaxWidth().aspectRatio((w / h).toFloat()).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val s = size.width / w.toFloat()
        val f = now()
        clipRect {
            scale(s, s, pivot = Offset.Zero) {
                if (drop != null) drawDrop(ModeSwitchMotion.dropFrame(f.dropMs, w, h), drop, light)
                if (pump != null) drawPump(ModeSwitchMotion.pumpFrame(f.pumpMs, w, h, src), pump, light)
            }
        }
    }
}
