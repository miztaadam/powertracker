package com.powertracker.domain.library.figures

import com.powertracker.domain.library.Library
import kotlin.math.abs

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
            add(Triple("torso", Probe("torso", s.pelvis, s.shoulder, Body.TORSO_RADIUS), false))
            add(Triple("head", Probe("head", s.head, s.head, Body.HEAD_RADIUS), false))
            for (side in Side.entries) {
                for (seg in FigureRenderer.limbSegments(s.side(side))) {
                    val isArm = seg.label in ARM_SEGMENTS
                    add(Triple("${side.label} ${seg.label}", Probe(seg.label, seg.a, seg.b, seg.width / 2), isArm))
                }
            }
        }
        for ((label, probe, isArm) in parts) {
            for (solid in solids) {
                if (isArm && !solid.stopsArms) continue
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
            val twoHanded = (held is Barbell && held.on == BarHold.HANDS) || held is LatBar
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
        val bar = scene.props.filterIsInstance<HeldProp>().firstOrNull()
            ?: return "key check $check: no bar in the scene"
        val c = bar.center(s)
        val barRadius = when (bar) {
            is Barbell -> Barbell.SHAFT_RADIUS
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
                if (abs(c.x - mid.x) > LINE_TOLERANCE) "bar ${round1(c.x - mid.x)} cm off the mid-foot" else null
            }
            is HipDepth -> null // handled above: it needs no bar
        }
    }

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
