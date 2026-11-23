package com.powertracker.domain.library.figures

import kotlin.math.max
import kotlin.math.min

/** What a shape is drawn with. Each app maps a role to a theme colour at draw time (spec §3.3), never a fixed colour. */
enum class Role {
    /** The body: the main text colour. */
    BODY,

    /** Far-side limbs: the text colour mixed toward the surface. */
    BODY_FAR,

    /** Benches, racks, machines, the floor: muted. */
    EQUIPMENT,

    /** The bar, plates and handles: the chalk accent. */
    ACCENT,

    /** Plate faces: the chalk accent at low opacity. */
    ACCENT_FILL,

    /** An outline in the picture's surface colour, under each near limb, so overlapping limbs stay readable. */
    GAP,
}

/** A drawing primitive in view-box units (y down). Filled with its role's colour; capsules are thick round-ended lines. */
sealed interface Shape {
    val role: Role
}

data class Circle(override val role: Role, val cx: Double, val cy: Double, val r: Double) : Shape

data class Capsule(
    override val role: Role,
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val width: Double,
) : Shape

data class RoundRect(override val role: Role, val x: Double, val y: Double, val w: Double, val h: Double, val radius: Double) : Shape

/** A filled polygon; [points] are x,y pairs. Filled with the non-zero rule, so a ring is outer one way, inner the other. */
data class Polygon(override val role: Role, val points: List<Double>) : Shape

/** Every panel's frame: 400 × 300 (4:3), so the layout never shifts between exercises. */
data class ViewBox(val width: Double, val height: Double)

/** An axis-aligned box. */
data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double) {
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY

    fun union(o: Bounds) = Bounds(min(minX, o.minX), min(minY, o.minY), max(maxX, o.maxX), max(maxY, o.maxY))
}

/** The box a shape covers, including stroke width and radius. */
fun Shape.bounds(): Bounds = when (this) {
    is Circle -> Bounds(cx - r, cy - r, cx + r, cy + r)
    is Capsule -> (width / 2).let { h -> Bounds(min(x1, x2) - h, min(y1, y2) - h, max(x1, x2) + h, max(y1, y2) + h) }
    is RoundRect -> Bounds(x, y, x + w, y + h)
    is Polygon -> {
        val xs = points.filterIndexed { i, _ -> i % 2 == 0 }
        val ys = points.filterIndexed { i, _ -> i % 2 == 1 }
        Bounds(xs.min(), ys.min(), xs.max(), ys.max())
    }
}
