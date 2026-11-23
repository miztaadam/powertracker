package com.powertracker.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FigureRenderer
import com.powertracker.domain.library.figures.Which
import com.powertracker.ui.theme.Space

/** Every panel's shape: the figure view box's 4:3. */
private const val PANEL_ASPECT = 4f / 3f

/** The gap between the two panels. */
private val PANEL_GAP = 8.dp

private val THUMBNAIL_WIDTH = 56.dp
private val THUMBNAIL_HEIGHT = 40.dp

/** What a screen reader says for a figure's picture. */
fun pictureLabel(figure: ExerciseFigure): String = "Picture: ${figure.startCaption}, then ${figure.endCaption}."

/**
 * The picture of the exercise [name]: its start and end pose side by side, sharing the width equally, each with its
 * caption underneath. One element to a screen reader. Nothing at all, taking no room, when [figureFor] finds no
 * figure: the lifter's own exercise (every catalogue exercise has one).
 */
@Composable
fun ExercisePicture(name: String, modifier: Modifier = Modifier, figureFor: (String) -> ExerciseFigure? = Library::figure) {
    val figure = figureFor(name) ?: return
    val colors = figureColors()
    val label = pictureLabel(figure)
    Row(
        modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = label
            role = Role.Image
            testTag = "exercise-picture"
        },
        horizontalArrangement = Arrangement.spacedBy(PANEL_GAP),
    ) {
        for (which in Which.entries) {
            val shapes = remember(figure, which) { FigureRenderer.shapes(figure, which) }
            Column(Modifier.weight(1f)) {
                FigureCanvas(shapes, colors, Modifier.fillMaxWidth().aspectRatio(PANEL_ASPECT).testTag("picture-panel"))
                // Wraps onto as many lines as it needs: a caption is never cut short.
                Text(
                    if (which == Which.START) figure.startCaption else figure.endCaption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth().padding(top = Space.xs).testTag("picture-caption"),
                )
            }
        }
    }
}

/** The muted tile a thumbnail is drawn on. */
fun thumbnailSurface(scheme: ColorScheme): Color = scheme.surfaceContainerHigh

/**
 * A row's small picture: the more telling pose of the exercise [name] on a muted tile. The tile is empty when [name]
 * is null (the lifter's own exercise) or [figureFor] finds no figure, so names stay aligned down the list. Decorative:
 * the row beside it names the exercise, so it carries nothing for a screen reader.
 *
 * Nothing is worked out here: a thumbnail that is ready ([ThumbnailLoader.peek]) is drawn in the first frame, and one
 * that is not is asked of the loader's background worker while the tile shows empty. With the app's own lookup the
 * loader is [LocalThumbnails]; any other [figureFor] (a test's) gets a loader of its own, so it neither reads nor
 * fills the catalogue's cache.
 */
@Composable
fun ExerciseThumbnail(name: String?, modifier: Modifier = Modifier, figureFor: (String) -> ExerciseFigure? = catalogueFigure) {
    val shared = LocalThumbnails.current
    val loader = if (figureFor === catalogueFigure) {
        shared
    } else {
        val own = remember(figureFor, shared) { ThumbnailLoader(figureFor, shared.dispatcher) }
        DisposableEffect(own) { onDispose { own.close() } }
        own
    }
    val thumbnail = if (name == null) {
        null
    } else {
        val ready = remember(name, loader) { mutableStateOf(loader.peek(name)) }
        if (ready.value == null) LaunchedEffect(name, loader) { ready.value = loader.load(name) }
        ready.value
    }
    val tile = thumbnailSurface(MaterialTheme.colorScheme)
    Box(
        modifier.size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT).clip(MaterialTheme.shapes.extraSmall).background(tile).testTag("exercise-thumbnail"),
        contentAlignment = Alignment.Center,
    ) {
        val shapes = thumbnail?.shapes
        // The tag lets a test tell a drawn tile from an empty one; it says nothing to a screen reader.
        if (shapes != null) FigureCanvas(shapes, figureColors(tile), Modifier.fillMaxHeight().aspectRatio(PANEL_ASPECT).testTag("thumbnail-drawing"))
    }
}
