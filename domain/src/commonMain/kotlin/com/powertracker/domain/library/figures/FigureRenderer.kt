package com.powertracker.domain.library.figures

import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Turns a figure's scene into an ordered list of colour-role shapes in view-box units. Both panels of a figure share
 * one transform (the union of both scenes fitted with a uniform scale and a 6% margin, the floor on the bottom
 * margin), so the kit stands still between them. Results are cached per figure and panel.
 *
 * Draw order, back to front: far limbs (BODY_FAR), fixed equipment, the body, then for each near limb a GAP outline
 * and the limb, then held equipment (ACCENT / ACCENT_FILL), then any fixed part drawn in front of the hands (the side
 * view's pull-up bar, a cable in front). A cable station marked [CableStack.beyond] is drawn first, behind the far
 * limbs. Front view: legs pointing straight away from the camera are hidden behind
 * the body and not drawn.
 */
object FigureRenderer {
    val viewBox = ViewBox(400.0, 300.0)

    /** The margin on each side, as a fraction of the panel's width and height. */
    const val MARGIN = 0.06

    @Volatile private var shapeCache: Map<Pair<ExerciseFigure, Which>, List<Shape>> = emptyMap()

    @Volatile private var fitCache: Map<ExerciseFigure, Fit> = emptyMap()

    fun shapes(figure: ExerciseFigure, which: Which): List<Shape> {
        val key = figure to which
        shapeCache[key]?.let { return it }
        val fit = fit(figure)
        val out = world(figure.scene(which), figure.view).mapNotNull { fit.place(it) }
        // Copy-on-write: a race only computes twice, and the first stored list wins so callers see one instance.
        val stored = shapeCache[key]
        if (stored != null) return stored
        shapeCache = shapeCache + (key to out)
        return out
    }

    /** Where world point [p] lands in either panel of [figure]. */
    fun toViewBox(figure: ExerciseFigure, p: Point): Point = fit(figure).point(p)

    internal fun fit(figure: ExerciseFigure): Fit {
        fitCache[figure]?.let { return it }
        val placed = Which.entries.flatMap { world(figure.scene(it), figure.view) }
        var content: Bounds? = null
        var floor: Bounds? = null
        for (p in placed) {
            if (!p.fit) continue
            val b = p.shape.bounds()
            if (p.floor) floor = floor?.union(b) ?: b else content = content?.union(b) ?: b
        }
        val c = content ?: Bounds(0.0, 0.0, 1.0, 1.0)
        val all = floor?.let { Bounds(c.minX, min(c.minY, it.minY), c.maxX, max(c.maxY, it.maxY)) } ?: c
        val mx = viewBox.width * MARGIN
        val my = viewBox.height * MARGIN
        val scale = min((viewBox.width - 2 * mx) / max(all.width, 1e-6), (viewBox.height - 2 * my) / max(all.height, 1e-6))
        val ox = (viewBox.width - all.width * scale) / 2 - all.minX * scale
        val oy = if (floor != null) viewBox.height - my - all.maxY * scale else (viewBox.height - all.height * scale) / 2 - all.minY * scale
        return Fit(scale, ox, oy, mx).also { fitCache = fitCache + (figure to it) }
    }

    /** The world-unit shapes of [scene], in draw order. */
    internal fun world(scene: Scene, view: View): List<Placed> {
        val s = scene.pose.skeleton(view)
        val fixed = scene.props.filterIsInstance<FixedProp>()
        val held = scene.props.filterIsInstance<HeldProp>()
        val cableEnd = held.firstOrNull { it is LatBar || it == CableHandles || it is CableAttachment || it is CableHandle || it is AnkleStrap }?.center(s)
        val farRole = if (view == View.SIDE) Role.BODY_FAR else Role.BODY
        return buildList {
            // Front view, legs pointing straight away from the camera (a push-up seen head-on): they lie behind the
            // shoulders and are hidden by them, so they are not drawn.
            val legsHidden = view == View.FRONT && legHidden(scene.pose.nearLeg) && legHidden(scene.pose.farLeg)
            // A backdrop (a cable station beyond a lifter seen from behind) goes behind every limb.
            val (backdrop, rest) = fixed.partition { it is CableStack && it.beyond }
            for (p in backdrop) addAll(p.draw(view, cableEnd, s))
            val far = limbSegments(s.far)
            for (seg in if (legsHidden) far.subList(3, 6) else far) add(Placed(seg.capsule(farRole)))
            for (p in rest) addAll(p.draw(view, cableEnd, s))
            val near = limbSegments(s.near)
            add(Placed(torso(s)))
            add(Placed(Capsule(Role.BODY, s.neckBase.x, s.neckBase.y, s.head.x, s.head.y, Body.NECK_WIDTH)))
            add(Placed(Circle(Role.BODY, s.head.x, s.head.y, Body.HEAD_RADIUS)))
            for (limb in listOfNotNull(near.subList(0, 3).takeUnless { legsHidden }, near.subList(3, 6))) {
                for (seg in limb) add(Placed(seg.capsule(Role.GAP, extra = Body.GAP_EXTRA)))
                for (seg in limb) add(Placed(seg.capsule(Role.BODY)))
            }
            for (p in held) addAll(p.draw(s))
            for (p in fixed) addAll(p.drawFront(view, cableEnd, s))
        }
    }

    /** A front-view leg pointing straight away from the camera, thigh and shin ([Leg.out], [Leg.shinOut] at −90°). */
    internal fun legHidden(leg: Leg): Boolean = leg.out <= -89.9 && leg.shinOut <= -89.9

    /** One limb segment's centre line and drawn width. */
    internal data class LimbSegment(val label: String, val a: Point, val b: Point, val width: Double) {
        fun capsule(role: Role, extra: Double = 0.0) = Capsule(role, a.x, a.y, b.x, b.y, width + extra)
    }

    /** A side's six limb segments: the leg (thigh, shin, foot) then the arm (upper arm, forearm, hand). */
    internal fun limbSegments(l: LimbPoints): List<LimbSegment> = listOf(
        LimbSegment("thigh", l.hip, l.knee, Body.THIGH_WIDTH),
        LimbSegment("shin", l.knee, l.ankle, Body.SHIN_WIDTH),
        LimbSegment("foot", l.heel, l.toe, Body.FOOT_WIDTH),
        LimbSegment("upper arm", l.shoulder, l.elbow, Body.UPPER_ARM_WIDTH),
        LimbSegment("forearm", l.elbow, l.wrist, Body.FOREARM_WIDTH),
        LimbSegment("hand", l.wrist, l.hand, Body.HAND_WIDTH),
    )

    // The torso outline in its own frame: (along the pelvis-to-shoulder axis, towards the front). The side view has a
    // flat back and a chest that is flat where the bar touches; the front view is a shoulders-to-hips taper.
    private val sideOutline: List<Pair<Double, Double>> by lazy {
        val l = Segment.TORSO.length
        val chestTop = l - Body.CHEST_BELOW_SHOULDER + 6.0
        val chestLow = l - Body.CHEST_BELOW_SHOULDER - 6.0
        listOf(
            -4.0 to 7.0, -6.0 to 0.0, -4.0 to -7.0, 0.0 to -10.0, l * 0.5 to -10.0, l - 2.0 to -10.0, l + 3.0 to -7.0,
            l + 5.0 to -2.0, l + 4.0 to 4.0, l - 3.0 to 10.0, chestTop to Body.CHEST_DEPTH, chestLow to Body.CHEST_DEPTH,
            l - 30.0 to 10.5, l * 0.3 to 9.0, 2.0 to 9.0,
        )
    }

    private val frontOutline: List<Pair<Double, Double>> by lazy {
        val l = Segment.TORSO.length
        listOf(
            -5.0 to -16.0, l * 0.45 to -14.0, l - 4.0 to -21.0, l + 3.0 to -19.0, l + 6.0 to -8.0,
            l + 6.0 to 8.0, l + 3.0 to 19.0, l - 4.0 to 21.0, l * 0.45 to 14.0, -5.0 to 16.0,
        )
    }

    private fun torso(s: Skeleton): Polygon {
        val outline = if (s.view == View.SIDE) sideOutline else frontOutline
        val l = Segment.TORSO.length
        return Polygon(
            Role.BODY,
            outline.flatMap { (along, front) ->
                // A shrug lifts the tops of the shoulders with the arms' roots; the base of the neck rises much less,
                // so the shoulders come up beside it (Pose.shrug). Unshrugged outlines are unchanged.
                val lift = when {
                    s.shrug == 0.0 || along < l - 4.5 -> 0.0
                    s.view == View.FRONT && abs(front) < 15.0 -> s.shrug * 0.3
                    s.view == View.FRONT -> s.shrug
                    along > l + 2.5 -> s.shrug * 0.3
                    else -> s.shrug
                }
                s.onTorso(along + lift, front).let { listOf(it.x, it.y) }
            },
        )
    }

    /** The shared transform from world cm to view-box units. */
    internal class Fit(val scale: Double, private val ox: Double, private val oy: Double, private val marginX: Double) {
        fun x(v: Double) = v * scale + ox
        fun y(v: Double) = v * scale + oy
        fun point(p: Point) = Point(r(x(p.x)), r(y(p.y)))

        /** [p] in view-box units, clipped to the panel when it is not part of the fit; null when nothing is left. */
        fun place(p: Placed): Shape? {
            val s = p.shape
            if (p.floor && s is Capsule) {
                val w = s.width * scale
                return Capsule(s.role, r(marginX + w / 2), r(y(s.y1)), r(viewBox.width - marginX - w / 2), r(y(s.y2)), r(w))
            }
            val t: Shape = when (s) {
                is Circle -> Circle(s.role, x(s.cx), y(s.cy), s.r * scale)
                is Capsule -> Capsule(s.role, x(s.x1), y(s.y1), x(s.x2), y(s.y2), s.width * scale)
                is RoundRect -> RoundRect(s.role, x(s.x), y(s.y), s.w * scale, s.h * scale, s.radius * scale)
                is Polygon -> Polygon(s.role, s.points.mapIndexed { i, v -> if (i % 2 == 0) x(v) else y(v) })
            }
            return (if (p.fit) t else clip(t))?.let(::rounded)
        }

        private fun clip(s: Shape): Shape? = when (s) {
            is Capsule -> clipCapsule(s)
            is RoundRect -> {
                val x1 = max(s.x, 0.0)
                val y1 = max(s.y, 0.0)
                val x2 = min(s.x + s.w, viewBox.width)
                val y2 = min(s.y + s.h, viewBox.height)
                if (x2 <= x1 || y2 <= y1) null else s.copy(x = x1, y = y1, w = x2 - x1, h = y2 - y1, radius = min(s.radius, min(x2 - x1, y2 - y1) / 2))
            }
            else -> s
        }

        /** Liang–Barsky: keep the part of the centre line whose round ends stay inside the panel. */
        private fun clipCapsule(c: Capsule): Capsule? {
            val h = c.width / 2
            val minX = h
            val minY = h
            val maxX = viewBox.width - h
            val maxY = viewBox.height - h
            val dx = c.x2 - c.x1
            val dy = c.y2 - c.y1
            var t0 = 0.0
            var t1 = 1.0
            for ((p, q) in listOf(-dx to c.x1 - minX, dx to maxX - c.x1, -dy to c.y1 - minY, dy to maxY - c.y1)) {
                if (p == 0.0) {
                    if (q < 0) return null
                } else {
                    val t = q / p
                    if (p < 0) t0 = max(t0, t) else t1 = min(t1, t)
                }
            }
            if (t0 > t1) return null
            return c.copy(x1 = c.x1 + t0 * dx, y1 = c.y1 + t0 * dy, x2 = c.x1 + t1 * dx, y2 = c.y1 + t1 * dy)
        }
    }

    private fun r(v: Double) = round(v * 100) / 100

    private fun rounded(s: Shape): Shape = when (s) {
        is Circle -> s.copy(cx = r(s.cx), cy = r(s.cy), r = r(s.r))
        is Capsule -> s.copy(x1 = r(s.x1), y1 = r(s.y1), x2 = r(s.x2), y2 = r(s.y2), width = r(s.width))
        is RoundRect -> s.copy(x = r(s.x), y = r(s.y), w = r(s.w), h = r(s.h), radius = r(s.radius))
        is Polygon -> s.copy(points = s.points.map(::r))
    }
}
