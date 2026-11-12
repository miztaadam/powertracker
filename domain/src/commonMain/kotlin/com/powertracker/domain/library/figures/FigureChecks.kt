package com.powertracker.domain.library.figures

import com.powertracker.domain.library.Library
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** One thing wrong with a figure; [which] is the panel, or null for the entry as a whole. */
data class FigureProblem(val name: String, val which: Which?, val message: String)

/**
 * The automatic checks (spec §4.2), run on the JVM and JS: catalogue name, captions, guide lines, joint limits,
 * contacts, no intersection, held equipment in the hands, the entry's key positions, and fit in the view box.
 */
object FigureChecks {
    /** How far a contact point may sit from its surface, in cm. */
    const val CONTACT_TOLERANCE = 1.5

    /** How deep a body capsule may sink into a solid before it counts as passing through, in cm. */
    const val INTERSECTION_SLACK = 0.5

    /** How far a lockout or mid-foot bar may sit off the vertical line it should be on, in cm. */
    const val LINE_TOLERANCE = 5.0

    /** A locked elbow is within this many degrees of straight. */
    const val LOCKED_ELBOW = 5.0

    const val MAX_CAPTION = 24

    fun check(name: String, figure: ExerciseFigure): List<FigureProblem> = buildList {
        fun report(which: Which?, message: String) = add(FigureProblem(name, which, message))

        val entry = Library.find(name)?.takeIf { it.name == name }
        if (entry == null) {
            report(null, "'$name' is not a catalogue name")
        } else {
            val lines = (entry.guide.setup + entry.guide.steps).toSet()
            for (line in figure.guideLines) if (line !in lines) report(null, "guide line not in the $name guide: '$line'")
        }
        for ((label, caption) in listOf("start" to figure.startCaption, "end" to figure.endCaption)) {
            if (caption.isBlank()) report(null, "$label caption is blank")
            if (caption.length > MAX_CAPTION) report(null, "$label caption is over $MAX_CAPTION characters: '$caption'")
        }

        for (which in Which.entries) {
            val scene = figure.scene(which)
            val s = scene.pose.skeleton(figure.view)
            for (p in JointLimits.problems(scene.pose, figure.view)) report(which, "joint: $p")
            contacts(figure, which, s).forEach { report(which, it) }
            intersections(scene, figure.view, s).forEach { report(which, it) }
            grips(scene, figure.view, s).forEach { report(which, it) }
            for (c in figure.checks.filter { it.at == which }) keyCheck(c, scene, figure.view, s)?.let { report(which, it) }
            outsideViewBox(FigureRenderer.shapes(figure, which)).forEach { report(which, it) }
        }
    }

    /** One line per shape that leaves the view box (0.01 slack for rounding). */
    internal fun outsideViewBox(shapes: List<Shape>): List<String> {
        val box = FigureRenderer.viewBox
        return shapes.filter { shape ->
            val b = shape.bounds()
            b.minX < -0.01 || b.minY < -0.01 || b.maxX > box.width + 0.01 || b.maxY > box.height + 0.01
        }.map { "shape outside the view box: $it" }
    }

    private fun contacts(figure: ExerciseFigure, which: Which, s: Skeleton): List<String> = buildList {
        val scene = figure.scene(which)
        for (c in figure.contacts.filter { which in it.at }) {
            val slabs = scene.props.filterIsInstance<FixedProp>().flatMap { it.surfaces(figure.view)[c.surface].orEmpty() }
            if (slabs.isEmpty()) {
                add("contact ${c.point} on ${c.surface}: no ${c.surface} in the scene")
                continue
            }
            for (probe in s.probes(c.point)) {
                val gap = if (c.surface.grip && probe.handGrip != null) {
                    // A held surface: the grip point is in it or within the tolerance of it.
                    slabs.minOf { it.signedDistance(probe.handGrip) }.coerceAtLeast(0.0)
                } else {
                    slabs.minOf { segmentDistance(probe.a, probe.b, it) } - probe.radius
                }
                if (abs(gap) > CONTACT_TOLERANCE) {
                    add("contact ${c.point} on ${c.surface}: ${probe.label} is ${round1(gap)} cm ${if (gap > 0) "off" else "into"} it")
                }
            }
        }
    }

    private fun intersections(scene: Scene, view: View, s: Skeleton): List<String> = buildList {
        val solids = scene.props.filterIsInstance<FixedProp>().flatMap { it.solids(view) }
        val parts = buildList {
            add(Part("torso", Probe("torso", s.pelvis, s.shoulder, Body.TORSO_RADIUS), isArm = false, near = false))
            add(Part("head", Probe("head", s.head, s.head, Body.HEAD_RADIUS), isArm = false, near = false))
            for (side in Side.entries) {
                for (seg in FigureRenderer.limbSegments(s.side(side))) {
                    val isArm = seg.label in ARM_SEGMENTS
                    add(Part("${side.label} ${seg.label}", Probe(seg.label, seg.a, seg.b, seg.width / 2), isArm, side == Side.NEAR))
                }
            }
        }
        for ((label, probe, isArm, near) in parts) {
            for (solid in solids) {
                if (isArm && !solid.stopsArms) continue
                if (near && !solid.stopsNearLimbs) continue
                val depth = probe.radius - segmentDistance(probe.a, probe.b, solid.slab)
                if (depth > INTERSECTION_SLACK) {
                    add("$label goes through the ${solid.label} (${round1(depth)} cm)")
                    break
                }
            }
        }
    }

    private fun grips(scene: Scene, view: View, s: Skeleton): List<String> = buildList {
        for (held in scene.props.filterIsInstance<HeldProp>()) {
            if (held == SafetySquatBar) {
                // Both hands on the handle ends (the far side's handle is the near one's mirror image).
                for (side in Side.entries) {
                    val off = (s.side(side).grip - safetyBarHandle(s)).length()
                    if (off > CONTACT_TOLERANCE) add("held: the ${side.label} hand is ${round1(off)} cm off the safety bar's handle")
                }
                continue
            }
            val twoHanded = (held is Barbell && held.on == BarHold.HANDS) || held is LatBar || held == TrapBar
            if (!twoHanded) continue
            val off = if (view == View.SIDE) (s.near.grip - s.far.grip).length() else abs(s.near.grip.y - s.far.grip.y)
            if (off > CONTACT_TOLERANCE) add("held: the hands are ${round1(off)} cm apart on the bar")
        }
    }

    private fun keyCheck(check: KeyCheck, scene: Scene, view: View, s: Skeleton): String? {
        if (check is HipDepth) {
            val below = s.near.hip.y - s.near.knee.y
            return if (below < check.belowKnee) "hip depth: hip ${round1(below)} cm below the knee, needs ${check.belowKnee}" else null
        }
        if (check is HandsInsideKnees) {
            if (view != View.FRONT) return "hands inside knees: a front-view check"
            val mid = (s.near.hip.x + s.far.hip.x) / 2
            val clear = Body.HAND_WIDTH / 2 + Body.SHIN_WIDTH / 2
            for (side in Side.entries) {
                val l = s.side(side)
                val room = abs(l.knee.x - mid) - abs(l.grip.x - mid)
                if (room < clear) return "hands inside knees: the ${side.label} hand is ${round1(room)} cm inside the knee, needs $clear"
            }
            return null
        }
        if (check is ChinOverBar) {
            val bar = scene.props.filterIsInstance<PullUpBar>().firstOrNull() ?: return "chin over bar: no pull-up bar in the scene"
            val barTop = -bar.height - PullUpBar.RADIUS
            val above = barTop - s.chin().y
            return if (above < check.margin) "chin over bar: chin ${round1(above)} cm above the top of the bar, needs ${check.margin}" else null
        }
        val bar = scene.props.filterIsInstance<HeldProp>().firstOrNull()
            ?: return "key check $check: no bar in the scene"
        val c = bar.center(s)
        val barRadius = when (bar) {
            is Barbell, SafetySquatBar, TrapBar -> Barbell.SHAFT_RADIUS
            is Dumbbell -> Dumbbell.HEAD_RADIUS
            LatBar -> 1.3
        }
        return when (check) {
            is BarTouches -> {
                val gap = s.probes(check.point).minOf { distance(c, it.a, it.b) - it.radius } - barRadius
                if (abs(gap) > CONTACT_TOLERANCE) "bar is ${round1(gap)} cm off ${check.point}, not touching it" else null
            }
            is LockoutOverShoulders -> {
                val elbows = listOf(scene.pose.nearArm.elbow, scene.pose.farArm.elbow)
                val shoulderX = if (view == View.SIDE) s.shoulder.x else (s.near.shoulder.x + s.far.shoulder.x) / 2
                when {
                    elbows.any { it > LOCKED_ELBOW } -> "lockout: elbows bent ${elbows.max()}°"
                    abs(c.x - shoulderX) > LINE_TOLERANCE -> "lockout: bar ${round1(c.x - shoulderX)} cm off the shoulders"
                    c.y >= s.shoulder.y -> "lockout: bar not above the shoulders"
                    else -> null
                }
            }
            is BarOverMidfoot -> {
                val mid = (s.near.heel + s.near.toe) * 0.5
                // A safety squat bar's load is its cambered plates, not the yoke on the back.
                val load = if (bar == SafetySquatBar) safetyBarLoad(s) else c
                if (abs(load.x - mid.x) > LINE_TOLERANCE) "bar ${round1(load.x - mid.x)} cm off the mid-foot" else null
            }
            is BarNearLegs -> {
                val gap = barToLegs(c, s)
                when {
                    gap < -INTERSECTION_SLACK -> "bar ${round1(-gap)} cm into the legs"
                    gap > check.within -> "bar ${round1(gap)} cm from the legs, more than ${check.within}"
                    else -> null
                }
            }
            is BarAbove -> {
                val gap = s.probes(check.point).minOf { distance(c, it.a, it.b) - it.radius } - barRadius
                if (gap < check.min || gap > check.max) "bar is ${round1(gap)} cm above ${check.point}, needs ${check.min}..${check.max}" else null
            }
            is BarRestsOn -> {
                val slabs = scene.props.filterIsInstance<FixedProp>().flatMap { it.surfaces(view)[check.surface].orEmpty() }
                if (slabs.isEmpty()) return "bar rests on ${check.surface}: no ${check.surface} in the scene"
                // The plates rest on blocks; the shaft rests on pins or boards.
                val r = if (check.surface == Surface.BLOCK) Barbell.PLATE_DIAMETER / 2 else barRadius
                val gap = slabs.minOf { it.signedDistance(Point(c.x, c.y + r)) }
                if (abs(gap) > CONTACT_TOLERANCE) "bar is ${round1(gap)} cm ${if (gap > 0) "off" else "into"} the ${check.surface}" else null
            }
            is HandlesBesideLegs -> {
                val off = legLineOffset(c, s)
                when {
                    off == null -> "handles: the grip is not beside the legs"
                    abs(off) > check.within -> "handles ${round1(off)} cm off the legs' line, more than ${check.within}"
                    else -> null
                }
            }
            is HipDepth, is ChinOverBar, is HandsInsideKnees -> null // handled above: they need no held bar
        }
    }

    /** The gap from a bar shaft centred at [bar] to the surface of the near thigh or shin, whichever is closer, in cm. */
    internal fun barToLegs(bar: Point, s: Skeleton): Double {
        val n = s.near
        return minOf(
            distance(bar, n.hip, n.knee) - Body.THIGH_WIDTH / 2,
            distance(bar, n.knee, n.ankle) - Body.SHIN_WIDTH / 2,
        ) - Barbell.SHAFT_RADIUS
    }

    /**
     * How far [grip] is in front of (+) or behind (-) the near leg's centre line at the grip's height, along the thigh
     * or shin that spans that height; null when neither does.
     */
    internal fun legLineOffset(grip: Point, s: Skeleton): Double? {
        val n = s.near
        for ((a, b) in listOf(n.hip to n.knee, n.knee to n.ankle)) {
            val lo = min(a.y, b.y)
            val hi = max(a.y, b.y)
            if (grip.y < lo || grip.y > hi || hi - lo < 1e-9) continue
            val t = (grip.y - a.y) / (b.y - a.y)
            return grip.x - (a.x + (b.x - a.x) * t)
        }
        return null
    }

    private data class Part(val label: String, val probe: Probe, val isArm: Boolean, val near: Boolean)

    private val ARM_SEGMENTS = setOf("upper arm", "forearm", "hand")

    /** Distance from [p] to the segment [a]–[b]. */
    private fun distance(p: Point, a: Point, b: Point): Double {
        val ab = b - a
        val len2 = ab.x * ab.x + ab.y * ab.y
        if (len2 == 0.0) return (p - a).length()
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0.0, 1.0)
        return (p - (a + ab * t)).length()
    }
}
