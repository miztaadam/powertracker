package com.powertracker.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.powertracker.domain.library.figures.Capsule
import com.powertracker.domain.library.figures.Circle
import com.powertracker.domain.library.figures.FigureRenderer
import com.powertracker.domain.library.figures.Polygon
import com.powertracker.domain.library.figures.Role
import com.powertracker.domain.library.figures.RoundRect
import com.powertracker.domain.library.figures.Shape
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.PtColors

/** How far the far-side limbs are mixed from the text colour toward the surface. */
private const val FAR_TOWARD_SURFACE = 0.45f

/** The plate face: the accent, this opaque. */
private const val ACCENT_FILL_ALPHA = 0.10f

/** The colour of each figure [Role], from the theme's tokens, for a picture on one surface. */
@Immutable
data class FigureColors(
    val body: Color,
    val bodyFar: Color,
    val equipment: Color,
    val accent: Color,
    val accentFill: Color,
    val gap: Color,
) {
    fun of(role: Role): Color = when (role) {
        Role.BODY -> body
        Role.BODY_FAR -> bodyFar
        Role.EQUIPMENT -> equipment
        Role.ACCENT -> accent
        Role.ACCENT_FILL -> accentFill
        Role.GAP -> gap
    }
}

/**
 * The role colours for a picture drawn on [surface]: the body in the main text colour, far limbs mixed toward the
 * surface, equipment muted, the bar and plates in the chalk accent, and gaps in the surface itself.
 */
fun figureColors(scheme: ColorScheme, pt: PtColors, surface: Color): FigureColors = FigureColors(
    body = scheme.onSurface,
    bodyFar = lerp(scheme.onSurface, surface, FAR_TOWARD_SURFACE),
    equipment = pt.muted,
    accent = pt.accent,
    accentFill = pt.accent.copy(alpha = ACCENT_FILL_ALPHA),
    gap = surface,
)

/** The role colours of the current theme. Read where the picture is composed, so a theme change redraws it. */
@Composable
fun figureColors(surface: Color = MaterialTheme.colorScheme.surface): FigureColors =
    figureColors(MaterialTheme.colorScheme, PowertrackerTheme.colors, surface)

/**
 * Draws a figure's [shapes] in the order given (the renderer's back-to-front order), fitted whole and centred in
 * whatever size the canvas has, with the view box's 4:3 kept.
 */
@Composable
fun FigureCanvas(shapes: List<Shape>, colors: FigureColors, modifier: Modifier = Modifier) {
    val viewBox = FigureRenderer.viewBox
    // Polygon outlines in view-box units, so they are built once per shape list and not per size or frame.
    // Non-zero winding: a plate ring is one polygon, its outer edge one way round and its inner edge the other.
    val polygons = remember(shapes) {
        shapes.filterIsInstance<Polygon>().associateWith { polygon ->
            Path().apply {
                fillType = PathFillType.NonZero
                val p = polygon.points
                for (i in 0 until p.size / 2) {
                    val x = p[2 * i].toFloat()
                    val y = p[2 * i + 1].toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }
    }
    Canvas(modifier) {
        val fit = minOf(size.width / viewBox.width.toFloat(), size.height / viewBox.height.toFloat())
        val left = (size.width - viewBox.width.toFloat() * fit) / 2
        val top = (size.height - viewBox.height.toFloat() * fit) / 2
        translate(left, top) {
            scale(fit, fit, pivot = Offset.Zero) {
                for (shape in shapes) {
                    val color = colors.of(shape.role)
                    when (shape) {
                        is Circle -> drawCircle(color, shape.r.toFloat(), Offset(shape.cx.toFloat(), shape.cy.toFloat()))
                        is Capsule -> drawLine(
                            color,
                            Offset(shape.x1.toFloat(), shape.y1.toFloat()),
                            Offset(shape.x2.toFloat(), shape.y2.toFloat()),
                            strokeWidth = shape.width.toFloat(),
                            cap = StrokeCap.Round,
                        )
                        is RoundRect -> drawRoundRect(
                            color,
                            topLeft = Offset(shape.x.toFloat(), shape.y.toFloat()),
                            size = Size(shape.w.toFloat(), shape.h.toFloat()),
                            cornerRadius = CornerRadius(shape.radius.toFloat()),
                        )
                        is Polygon -> drawPath(polygons.getValue(shape), color)
                    }
                }
            }
        }
    }
}
