package com.powertracker.domain.library.figures

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The equipment kit, at real-world scale in centimetres. Pieces are added only when a batch needs them (spec §2.2).
// Fixed pieces stand in the scene at a given x (the floor's top is y = 0); held pieces follow from the pose: they
// sit in the hands, or across the upper back, and are never placed on their own.
//
// IPF Technical Rules 2026 (https://www.powerlifting.sport/fileadmin/ipf/data/rules/technical-rules/english/2026_IPF_Technical_Rulebook__effective_01_March_2026__v3.pdf):
// bench at least 122 cm long, 29–32 cm wide, 42–45 cm from the floor to the top of the pad; bar rests 75–110 cm;
// the largest discs not more than 45 cm across. Bar shaft 28–29 mm (IPF Technical Rules 2021, same site).
// Other sizes (rack, cable stack, machines) are typical gym dimensions, kept neutral.

/** A kit piece in a [Scene]. */
sealed interface Prop

/** A piece that stands in the scene: the same in both panels unless its own settings change. */
sealed interface FixedProp : Prop

/** A piece held in the hands or across the upper back; its position follows from the pose. */
sealed interface HeldProp : Prop

/** The floor; its top is y = 0. Drawn across the panel on the bottom margin. */
data object Floor : FixedProp

/**
 * A flat bench running from [x] (head end) for [length], its pad's top [height] above the floor. With [nearLimbsBeside]
 * the near limbs pass beside it, not through it (a one-arm row: the far knee and hand on the bench, the near foot on the
 * floor next to it), so only the torso, head and far limbs are checked against it.
 */
data class FlatBench(
    val x: Double = 0.0,
    val length: Double = LENGTH,
    val height: Double = HEIGHT,
    val nearLimbsBeside: Boolean = false,
) : FixedProp {
    companion object {
        const val HEIGHT = 43.0
        const val LENGTH = 122.0
        const val PAD = 6.0
        const val WIDTH = 30.0
        const val LEG_INSET = 15.0
    }
}

/**
 * A power rack's uprights at [x] (the lifter's side of the posts), with J-hooks whose bar rest is [jHookHeight] above
 * the floor and safety arms whose top is [safetyHeight] above it, reaching [armLength] towards the lifter. The lifter
 * is to the right of the posts, or to the left when [lifterLeft] (a lifter who faced the hooks and stepped back).
 * Upright height, post spacing and arm length are typical gym rack sizes, kept neutral.
 */
data class PowerRack(
    val x: Double,
    val jHookHeight: Double,
    val safetyHeight: Double,
    val armLength: Double = 80.0,
    val height: Double = 230.0,
    val lifterLeft: Boolean = false,
) : FixedProp

/**
 * How a barbell is held: in the hands, across the upper back, or in the front rack (resting on the front of the
 * shoulders, on top of the forward-pointing upper arms, as in a front squat), or in the crook of the elbows (a Zercher
 * squat, Task 10: KitQuads.kt [crookOf]), or [ON_TRAPS]: across the upper back like [UPPER_BACK], but seated on top
 * of the shoulders at the base of the neck (Task 14), so in the side view its centre lies inside the back's outline,
 * [ON_TRAPS_BELOW] under the shoulder joint and [ON_TRAPS_BEHIND] behind the torso's axis, not outside the back's
 * edge. An upright lifter's bar on the back read as held in front of the chest otherwise (the calf raises, Calves.kt).
 */
enum class BarHold { HANDS, UPPER_BACK, FRONT_RACK, CROOK, ON_TRAPS }

/** [BarHold.ON_TRAPS]: the bar's centre below the shoulder joint along the torso, and behind its axis, cm (neutral). */
internal const val ON_TRAPS_BELOW = 3.0
internal const val ON_TRAPS_BEHIND = 5.0

/** An Olympic barbell loaded with 45 cm plates. */
data class Barbell(val on: BarHold = BarHold.HANDS) : HeldProp {
    companion object {
        const val PLATE_DIAMETER = 45.0
        const val SHAFT_RADIUS = 1.45
        const val SLEEVE_RADIUS = 2.5
        const val LENGTH = 220.0
        const val INNER_COLLARS = 131.0
    }
}

/**
 * A safety squat bar across the upper back: a padded yoke over the shoulders, two handles that come forward over the
 * shoulders to the hands in front of the chest, and sleeves cambered down and a little forward, so the plates hang
 * [CAMBER] below and [CAMBER_FORWARD] in front of the yoke. Sizes are a typical, manufacturer-neutral bar: handles
 * reaching about 37 cm from the yoke, a 15 cm camber drop, and 45 cm plates. The plates are the load the lifter
 * balances over the mid-foot ([safetyBarLoad]).
 */
data object SafetySquatBar : HeldProp {
    const val CAMBER = 15.0

    /** How far forward of the yoke the cambered sleeves carry the plates (some bars angle them forward; neutral). */
    const val CAMBER_FORWARD = 8.0
    const val PAD_RADIUS = 5.0

    /** The handle end in the torso's frame: [HANDLE_BELOW] under the shoulder joint along the torso, [HANDLE_FRONT] in front of it. */
    const val HANDLE_BELOW = 12.0
    const val HANDLE_FRONT = 24.0
}

/**
 * A trap (hex) bar in both hands. The lifter stands inside the hexagonal frame, holding its two side handles, so the
 * handles and the sleeves' line are beside the legs. Seen from the side, the frame is a level bar [FRAME] long front
 * to back at the handles' height, with the plates on the sleeves level with the handles. A typical,
 * manufacturer-neutral bar: a frame about 65 cm deep inside, low handles at the frame's height, 45 cm plates.
 */
data object TrapBar : HeldProp {
    const val FRAME = 65.0
}

/**
 * A plyo or squat box behind the lifter, centred at [x], its top [height] above the floor, [depth] front to back. A
 * typical sturdy box (about 50 cm deep), kept neutral; the height is set per figure to the target depth. Task 10 also
 * uses it as the step-up box in front of or beside the lifter (in a front view [depth] is its width across).
 */
data class SquatBox(val x: Double, val height: Double, val depth: Double = 50.0) : FixedProp

/**
 * A low platform the lifter stands on for a deficit pull, centred at [x], [height] tall, [length] front to back. The
 * plates stay on the floor beside it. A typical sturdy platform, kept neutral; 5 cm (2 in) is the common deficit.
 */
data class DeficitPlatform(val x: Double, val height: Double = 5.0, val length: Double = 60.0) : FixedProp

/**
 * Pulling blocks under the bar's plates, centred at [x], [height] tall, [length] front to back (about the plates'
 * 45 cm or a little less, so they sit under the plates only). Typical stacked blocks, kept neutral; ExRx's block
 * pull gives 5, 10 or 15 cm.
 */
data class PullingBlocks(val x: Double, val height: Double, val length: Double = 42.0) : FixedProp

/**
 * Bench-press boards held on the chest: [boards] lengths of 2×6 timber (3.8 cm thick each, the common board) stacked
 * on the chest, centred at [x], their underside at [bottom] (world y, the chest's surface), [LENGTH] long along the
 * body. One "2-board" stack is the typical size; the sizes are neutral.
 */
data class BenchBoards(val x: Double, val bottom: Double, val boards: Int = 2) : FixedProp {
    val thickness: Double get() = boards * BOARD

    companion object {
        const val BOARD = 3.8
        const val LENGTH = 30.0
    }
}

/**
 * A dumbbell in one hand. With the usual grip (palms towards the feet, as in a press or a row) its handle points across
 * the body, so the side view sees it end-on, a disc. With a [neutral] grip (palms facing each other, as in a fly) the
 * handle runs along the body, so the side view sees it in profile: the handle with a head at each end, along the
 * torso's line (Task 6). The front view draws the profile either way, unless [endOnFront]: a dumbbell held at the
 * side with the palm facing in points its handle at the camera, so the front view sees it end-on, a disc (Task 7).
 * With [neutral] and [squareToForearm] the side view's profile runs square to the hand instead of along the torso: a
 * palms-in curl's dumbbell (a hammer curl, or a DB curl before the palm turns up), whose handle crosses the fist at a
 * right angle to the forearm as the forearm swings (Task 9).
 */
data class Dumbbell(val hand: Side, val neutral: Boolean = false, val endOnFront: Boolean = false, val squareToForearm: Boolean = false) : HeldProp {
    companion object {
        const val HEAD_RADIUS = 10.0
        const val HANDLE = 14.0
    }
}

/**
 * A cable column at [x] with its pulley [reach] towards the lifter, [pulleyHeight] above the floor (the pulley slides
 * on the column: high for a crossover or pulldown, at the shoulders for a press, low for a low-to-high fly). With
 * [halfSpan] > 0 it is a two-column cable station: in the front view the columns stand at [x] ± [halfSpan], each
 * pulley [reach] in from its column and cabled to the hand on its side. A typical column is 230 cm tall; a real
 * crossover station's columns are 3–4 m apart, so a front view draws them closer than that to keep the lifter large
 * (the cables' angle at the hands is what the picture shows). With [cableInFront] the side view draws the cable in
 * front of the body: a pulley beside the lifter (a bench between low pulleys) whose near cable runs to the near hand
 * in front of the body, where the cable from a column behind would be hidden by it; in a front view the same holds for
 * a single column ([halfSpan] 0: a pulley beside the lifter whose cable crosses in front of the body to the hand,
 * Task 8). With [crossed] a two-column front view cables each pulley to the hand on the other side (a reverse fly's
 * crossed cables, Task 8). With [beyond] the columns stand beyond the lifter in a front view seen from behind (a
 * reverse fly facing the pulleys), so the renderer draws them before every limb, the far arm included (Task 8).
 */
data class CableStack(
    val x: Double,
    val pulleyHeight: Double = 215.0,
    val reach: Double = 45.0,
    val height: Double = 230.0,
    val halfSpan: Double = 0.0,
    val cableInFront: Boolean = false,
    val crossed: Boolean = false,
    val beyond: Boolean = false,
) : FixedProp {
    companion object {
        /** The cable drawn in front of the body ([cableInFront]): thicker than the 0.8 cm one behind, so it shows. */
        const val FRONT_CABLE = 1.4
    }
}

/** A lat bar on the cable, in both hands. */
data object LatBar : HeldProp {
    const val HALF_LENGTH = 60.0
}

/** A lat pulldown seat centred at [x], [seatHeight] to its top, with a roller thigh pad at ([padX], [padHeight]). */
data class LatPulldownSeat(val x: Double, val padX: Double, val seatHeight: Double = 48.0, val padHeight: Double = 66.0) : FixedProp {
    companion object {
        const val PAD_RADIUS = 6.0

        /** Front view (Task 7): the roller pads reach this far either side of the middle, over both thighs. */
        const val FRONT_HALF = 24.0
    }
}

/**
 * A 45° leg press. [x] is the front edge of the seat; the back pad reclines behind it. The foot plate rides the 45°
 * rail, [sled] cm up from its lowest stop. Sizes are typical, kept neutral. The back pad reclines to 25° above level
 * ([BACK_ANGLE] 155°): with the knees about 90° at the bottom and the feet flat, a more upright pad would fold the hips
 * further than the guide allows (see Quads.kt).
 */
data class LegPress45(val x: Double, val sled: Double) : FixedProp {
    companion object {
        const val SEAT_HEIGHT = 45.0
        const val BACK_ANGLE = 155.0
        const val RAIL_ANGLE = 45.0

        /**
         * How far the foot plate's centre stands off the rail on its carriage (a neutral estimate). The plate
         * travels along a line a little above the seated lifter's hips, so the legs push close to along the rail and,
         * with the knees bent about 90° at the bottom, the flat feet need no more ankle bend than the figure allows.
         */
        const val PLATE_RISE = 52.0

        /**
         * The foot plate's tilt back from square to the rail, towards upright (a neutral estimate: plates on these
         * machines are square to the rail or tilted a little). Its face's long axis points along [PLATE_ANGLE].
         */
        const val PLATE_TILT = 10.0
        const val PLATE_ANGLE = RAIL_ANGLE + 90.0 - PLATE_TILT
    }
}

/**
 * A pull-up bar at [x], [height] (its centre) above the floor, 3.2 cm thick. A typical gym bar size, kept neutral; the
 * height is set per figure so the lifter hangs clear of the floor.
 */
data class PullUpBar(val x: Double, val height: Double = 230.0) : FixedProp {
    companion object {
        const val RADIUS = 1.6

        /** The side view draws the bar's end larger than it is (3 cm, not 1.6), so it shows in front of the hand at phone size. */
        const val DRAWN_RADIUS = 3.0
    }
}

/**
 * A bench with a back support: a seat whose front edge is at [x], its top [FlatBench.HEIGHT] up (the IPF bench height,
 * as a typical bench), and a back pad hinged at the seat's rear, [backAngle] from level (90 = upright). Seat
 * [SEAT_LENGTH] and back pad [BACK_LENGTH] are typical adjustable-bench sizes, kept neutral; the back pad is long
 * enough to reach a seated lifter's head. The lifter faces +x.
 */
data class AdjustableBench(val x: Double, val backAngle: Double = 90.0) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0
        const val BACK_LENGTH = 85.0
    }
}

// ---- geometry the renderer and the checks share (internal) ----

/** An oriented box: centre, [length] along [angle], [thickness] across it, its edges rounded by [radius] (a roller is all radius). */
internal data class Slab(
    val cx: Double,
    val cy: Double,
    val length: Double,
    val thickness: Double,
    val angle: Double = 0.0,
    val radius: Double = 0.0,
) {
    /** Signed distance from [p] to the box: negative inside. */
    fun signedDistance(p: Point): Double {
        val d = p - Point(cx, cy)
        val along = dir(angle)
        val across = dir(angle + 90.0)
        val qx = abs(d.x * along.x + d.y * along.y) - length / 2
        val qy = abs(d.x * across.x + d.y * across.y) - thickness / 2
        return hypot(max(qx, 0.0), max(qy, 0.0)) + min(max(qx, qy), 0.0) - radius
    }

    fun corners(): List<Point> {
        val c = Point(cx, cy)
        val a = dir(angle) * (length / 2)
        val b = dir(angle + 90.0) * (thickness / 2)
        return listOf(c - a - b, c + a - b, c + a + b, c - a + b)
    }

    companion object {
        /** The axis-aligned box from (x1, y1) to (x2, y2). */
        fun box(x1: Double, y1: Double, x2: Double, y2: Double) =
            Slab((x1 + x2) / 2, (y1 + y2) / 2, abs(x2 - x1), abs(y2 - y1))
    }
}

/** The smallest signed distance from the segment [a]–[b] to [slab] (sampled finely enough for the checks). */
internal fun segmentDistance(a: Point, b: Point, slab: Slab): Double {
    if (a == b) return slab.signedDistance(a)
    var best = Double.MAX_VALUE
    for (i in 0..SAMPLES) best = min(best, slab.signedDistance(a + (b - a) * (i.toDouble() / SAMPLES)))
    return best
}

private const val SAMPLES = 64

/** A solid piece of kit the body must not pass through. Arms can pass beside narrow pads, so only the floor stops them. */
internal data class Solid(val label: String, val slab: Slab, val stopsArms: Boolean, val stopsNearLimbs: Boolean = true)

/** A shape in world units, before fitting. [fit] = false pieces (tall uprights) are clipped to the panel instead. */
internal data class Placed(val shape: Shape, val fit: Boolean = true, val floor: Boolean = false)

private val FLOOR_SLAB = Slab(0.0, 500.0, 1.0e6, 1000.0)

private fun eq(shape: Shape, fit: Boolean = true) = Placed(shape, fit)

private fun capsule(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun slabShape(role: Role, s: Slab, radius: Double): Shape =
    if (s.angle % 180.0 == 0.0) {
        RoundRect(role, s.cx - s.length / 2, s.cy - s.thickness / 2, s.length, s.thickness, radius)
    } else {
        Polygon(role, s.corners().flatMap { listOf(it.x, it.y) })
    }

/** A ring as one polygon: the outer circle one way and the inner the other, so the non-zero fill leaves a hole. */
internal fun ring(role: Role, cx: Double, cy: Double, outer: Double, inner: Double, steps: Int = 36): Polygon {
    val pts = ArrayList<Double>((steps + 1) * 4)
    for (i in 0..steps) (2 * PI * i / steps).let { pts += cx + outer * cos(it); pts += cy - outer * sin(it) }
    for (i in steps downTo 0) (2 * PI * i / steps).let { pts += cx + inner * cos(it); pts += cy - inner * sin(it) }
    return Polygon(role, pts)
}

private fun legPressParts(p: LegPress45): Triple<Slab, Slab, Slab> {
    val seat = Slab(p.x - 15.0, -LegPress45.SEAT_HEIGHT + 3.0, 30.0, 6.0)
    val corner = Point(p.x - 30.0, -LegPress45.SEAT_HEIGHT)
    val backCentre = corner + dir(LegPress45.BACK_ANGLE) * 35.0 + dir(LegPress45.BACK_ANGLE - 90.0) * 3.0
    val back = Slab(backCentre.x, backCentre.y, 70.0, 6.0, LegPress45.BACK_ANGLE)
    val plateCentre = legPressCarriage(p) + dir(LegPress45.RAIL_ANGLE + 90.0) * LegPress45.PLATE_RISE
    val plate = Slab(plateCentre.x, plateCentre.y, 55.0, 5.0, LegPress45.PLATE_ANGLE)
    return Triple(seat, back, plate)
}

/** The adjustable bench's seat and back pad. The back pad's front face starts at the hinge, the seat's rear top corner. */
private fun adjustableParts(b: AdjustableBench): Pair<Slab, Slab> {
    val pad = FlatBench.PAD
    val seat = Slab.box(b.x - AdjustableBench.SEAT_LENGTH, -FlatBench.HEIGHT, b.x, -FlatBench.HEIGHT + pad)
    val hinge = adjustableHinge(b)
    val along = 180.0 - b.backAngle
    val c = hinge + dir(along) * (AdjustableBench.BACK_LENGTH / 2) + dir(along + 90.0) * (pad / 2)
    return seat to Slab(c.x, c.y, AdjustableBench.BACK_LENGTH, pad, along)
}

/** Where the back pad's front face meets the seat's top. */
internal fun adjustableHinge(b: AdjustableBench) = Point(b.x - AdjustableBench.SEAT_LENGTH, -FlatBench.HEIGHT)

private fun legPressRailStart(p: LegPress45) = Point(p.x + 25.0, -30.0)

/** Where the sled's carriage sits on the rail. */
private fun legPressCarriage(p: LegPress45) = legPressRailStart(p) + dir(LegPress45.RAIL_ANGLE) * (40.0 + p.sled)

/** Where a fixed piece's named surfaces are. */
internal fun FixedProp.surfaces(view: View): Map<Surface, List<Slab>> = when (this) {
    Floor -> mapOf(Surface.FLOOR to listOf(FLOOR_SLAB))
    is FlatBench -> mapOf(Surface.BENCH to listOf(benchPad(view)))
    is PowerRack -> mapOf(Surface.SAFETY_ARM to safetyArms(view))
    is CableStack -> emptyMap()
    is DeclineBench -> surfacesOf()
    is SmithMachine -> mapOf(Surface.SAFETY_ARM to listOf(stopSlab()))
    is PressMachine -> parts().let { (seat, back) -> mapOf(Surface.SEAT to listOf(seat), Surface.BACK_PAD to listOf(back)) }
    is PecDeck -> mapOf(Surface.SEAT to listOf(seatSlab()))
    is Seat -> mapOf(Surface.SEAT to listOf(slab()))
    is DipBars -> mapOf(Surface.DIP_BAR to listOf(barSlab()))
    is AssistedDipMachine -> mapOf(Surface.DIP_BAR to listOf(handleSlab()), Surface.ASSIST_STEP to listOf(stepSlab()))
    is SquatBox -> mapOf(Surface.BOX to listOf(boxSlab()))
    is DeficitPlatform -> mapOf(Surface.PLATFORM to listOf(Slab.box(x - length / 2, -height, x + length / 2, 0.0)))
    is PullingBlocks -> mapOf(Surface.BLOCK to listOf(Slab.box(x - length / 2, -height, x + length / 2, 0.0)))
    is BenchBoards -> mapOf(Surface.BOARD to listOf(boardSlab()))
    is LatPulldownSeat -> mapOf(
        Surface.SEAT to listOf(Slab(x, -seatHeight + 3.5, 44.0, 7.0)),
        Surface.THIGH_PAD to listOf(
            if (view == View.FRONT) Slab(x, -padHeight, 2 * LatPulldownSeat.FRONT_HALF, 0.0, radius = LatPulldownSeat.PAD_RADIUS)
            else Slab(padX, -padHeight, 0.0, 0.0, radius = LatPulldownSeat.PAD_RADIUS),
        ),
    )
    is LegPress45 -> legPressParts(this).let { (seat, back, plate) ->
        mapOf(Surface.SEAT to listOf(seat), Surface.BACK_PAD to listOf(back), Surface.FOOT_PLATE to listOf(plate))
    }
    is AdjustableBench -> adjustableParts(this).let { (seat, back) -> mapOf(Surface.SEAT to listOf(seat), Surface.BACK_PAD to listOf(back)) }
    is RowStation -> mapOf(Surface.SEAT to listOf(seatSlab()), Surface.FOOT_PLATE to listOf(plateSlab()))
    is RowMachine -> mapOf(Surface.SEAT to listOf(seatSlab()), Surface.CHEST_PAD to listOf(padSlab()))
    is PreacherBench -> mapOf(Surface.SEAT to listOf(seat()), Surface.ARM_PAD to listOf(padSlab()))
    is ArmPadMachine -> mapOf(Surface.SEAT to listOf(seat()), Surface.ARM_PAD to listOf(padSlab()))
    is ProneBench -> mapOf(Surface.CHEST_PAD to listOf(padSlab())) + (platformSlab()?.let { mapOf(Surface.FOOT_PLATE to listOf(it)) } ?: emptyMap())
    is TBarLever, is PullUpBand, is DipBelt, is PulldownLever, is ChestPlate, is Spotter -> emptyMap()
    is LateralRaiseMachine -> mapOf(Surface.SEAT to listOf(seatSlab()))
    is RackBar -> mapOf(Surface.PULL_UP_BAR to listOf(barSlab()))
    is AssistPullUpMachine -> mapOf(Surface.PULL_UP_BAR to listOf(barSlab()), Surface.ASSIST_STEP to listOf(padSlab()))
    is ParallelHandles -> mapOf(Surface.PULL_UP_BAR to listOf(handleSlab()))
    is PulloverMachine -> parts().let { (seat, back) -> mapOf(Surface.SEAT to listOf(seat), Surface.BACK_PAD to listOf(back)) }
    is HyperBench -> mapOf(Surface.THIGH_PAD to listOf(padSlab()), Surface.FOOT_PLATE to listOf(plateSlab()), Surface.LEG_BRACE to listOf(rollerSlab()))
    is HackSquatMachine -> mapOf(Surface.BACK_PAD to listOf(padSlab()), Surface.FOOT_PLATE to listOf(plateSlab()))
    is LeverSquatMachine -> emptyMap()
    is LegExtensionMachine -> mapOf(Surface.SEAT to listOf(seatSlab()), Surface.BACK_PAD to listOf(backSlab()), Surface.SHIN_PAD to rollerSlabs())
    is PushSled -> mapOf(Surface.SLED_HANDLE to listOf(postSlab()))
    is MiniBand -> emptyMap()
    is StabilityBall -> mapOf(Surface.BALL to listOf(slab()))
    is NordicStation -> mapOf(Surface.KNEE_PAD to listOf(padSlab()), Surface.LEG_BRACE to listOf(rollerSlab()))
    is ReverseHyperBench -> mapOf(Surface.BENCH to listOf(padSlab()), Surface.HANDLE to listOf(handleSlab()))
    is LyingLegCurlMachine -> mapOf(Surface.BENCH to listOf(padSlab()), Surface.SHIN_PAD to listOf(rollerSlab()), Surface.HANDLE to listOf(handleSlab()))
    is SeatedLegCurlMachine -> mapOf(
        Surface.SEAT to listOf(seatSlab()), Surface.BACK_PAD to listOf(backSlab()),
        Surface.SHIN_PAD to listOf(rollerSlab()), Surface.THIGH_PAD to listOf(lapSlab()), Surface.HANDLE to listOf(handleSlab()),
    )
    is StandingLegCurlMachine -> mapOf(Surface.THIGH_PAD to listOf(thighSlab()), Surface.SHIN_PAD to listOf(rollerSlab()), Surface.HANDLE to listOf(handleSlab()))
    is HipThrustMachine -> mapOf(Surface.BENCH to listOf(benchSlab()), Surface.FOOT_PLATE to listOf(platformSlab()), Surface.HIP_PAD to listOf(padSlab()))
    is HipMachine -> mapOf(Surface.SEAT to listOf(seatSlab())) + (handleSlabs().takeIf { it.isNotEmpty() }?.let { mapOf(Surface.HANDLE to it) } ?: emptyMap())
    // Task 12.
    is StandingCalfMachine -> mapOf(Surface.HANDLE to listOf(handleSlab()))
    is SeatedCalfMachine -> mapOf(Surface.SEAT to listOf(seatSlab()), Surface.THIGH_PAD to listOf(padSlab()))
    is DonkeyCalfMachine -> mapOf(Surface.ARM_PAD to listOf(supportSlab()), Surface.HIP_PAD to listOf(padSlab()))
    is CrunchMachine -> mapOf(Surface.SEAT to listOf(seatSlab()), Surface.HIP_PAD to listOf(hipPadSlab()), Surface.CHEST_PAD to listOf(padSlab()))
    is SupportPost -> mapOf(Surface.HANDLE to listOf(gripSlab()))
    is PullUpBar -> mapOf(
        Surface.PULL_UP_BAR to listOf(if (view == View.SIDE) Slab(x, -height, 2 * PullUpBar.RADIUS, 2 * PullUpBar.RADIUS) else Slab(x, -height, 120.0, 2 * PullUpBar.RADIUS)),
    )
}

private fun SquatBox.boxSlab() = Slab.box(x - depth / 2, -height, x + depth / 2, 0.0)

private fun BenchBoards.boardSlab() = Slab.box(x - BenchBoards.LENGTH / 2, bottom - thickness, x + BenchBoards.LENGTH / 2, bottom)

/** The safety arms' tops as slabs: in the side view one arm from behind the posts to its tip, in the front view one per post. */
private fun PowerRack.safetyArms(view: View): List<Slab> =
    if (view == View.SIDE) {
        val m = if (lifterLeft) -1.0 else 1.0
        val a = x + m * -14.0
        val b = x + m * armLength
        listOf(Slab.box(min(a, b), -safetyHeight, max(a, b), -safetyHeight + 4.0))
    } else {
        listOf(-1.0, 1.0).map { s -> (x + s * 60.0).let { px -> Slab.box(min(px, px - s * 10.0), -safetyHeight, max(px, px - s * 10.0), -safetyHeight + 4.0) } }
    }

private fun FlatBench.benchPad(view: View): Slab =
    if (view == View.SIDE) Slab.box(x, -height, x + length, -height + FlatBench.PAD)
    else Slab.box(x - FlatBench.WIDTH / 2, -height, x + FlatBench.WIDTH / 2, -height + FlatBench.PAD)

private fun FlatBench.legXs(view: View): List<Double> =
    if (view == View.SIDE) listOf(x + FlatBench.LEG_INSET, x + length - FlatBench.LEG_INSET) else listOf(x)

/** The solid parts of a fixed piece, for the no-intersection check. */
internal fun FixedProp.solids(view: View): List<Solid> = when (this) {
    Floor -> listOf(Solid("floor", FLOOR_SLAB, stopsArms = true))
    is FlatBench -> (
        listOf(benchPad(view)) + legXs(view).flatMap { lx ->
            listOf(Slab.box(lx - 2.5, -height + FlatBench.PAD, lx + 2.5, 0.0), Slab.box(lx - 12.0, -3.0, lx + 12.0, 0.0))
        }
        ).map { Solid("bench", it, stopsArms = false, stopsNearLimbs = !nearLimbsBeside) }
    is PowerRack, is CableStack, is PullUpBar, is PullingBlocks, is BenchBoards, is SmithMachine, is DipBars -> emptyList()
    is TBarLever, is RackBar, is ParallelHandles, is PullUpBand, is DipBelt, is PulldownLever, is ChestPlate, is Spotter -> emptyList()
    is RowStation -> listOf(Solid("bench", seatSlab(), stopsArms = false), Solid("foot plate", plateSlab(), stopsArms = false))
    is RowMachine -> listOf(Solid("machine", seatSlab(), stopsArms = false), Solid("chest pad", padSlab(), stopsArms = false))
    is ProneBench -> listOfNotNull(padSlab(), platformSlab()).map { Solid("bench", it, stopsArms = false) }
    // The arm pad is in front of the chest and under the arms, beside the torso in the side view: only the seat stops the body.
    is PreacherBench -> listOf(Solid("bench", seat(), stopsArms = false))
    is ArmPadMachine -> listOf(Solid("machine", seat(), stopsArms = false))
    is AssistPullUpMachine -> listOf(Solid("knee pad", padSlab(), stopsArms = false))
    is PulloverMachine -> parts().toList().map { Solid("machine", it, false) }
    is HyperBench -> listOf(padSlab(), plateSlab(), rollerSlab()).map { Solid("bench", it, stopsArms = false) }
    is DeclineBench -> solidsOf()
    is HackSquatMachine -> listOf(Solid("machine", padSlab(), stopsArms = false), Solid("foot plate", plateSlab(), stopsArms = false))
    is LegExtensionMachine -> listOf(Solid("machine", seatSlab(), stopsArms = false), Solid("machine", backSlab(), stopsArms = false))
    is LeverSquatMachine, is PushSled -> emptyList()
    // Task 11. The band, the standing curl's pads and the front-view hip machine (the seated legs in front of it) stop nothing.
    is MiniBand, is StandingLegCurlMachine, is HipMachine -> emptyList()
    is StabilityBall -> listOf(Solid("ball", slab(), stopsArms = false))
    is NordicStation -> listOf(Solid("pad", padSlab(), stopsArms = false))
    is ReverseHyperBench -> solidsOf()
    is LyingLegCurlMachine -> listOf(Solid("bench", padSlab(), stopsArms = false))
    is SeatedLegCurlMachine -> listOf(Solid("machine", seatSlab(), stopsArms = false), Solid("machine", backSlab(), stopsArms = false))
    is HipThrustMachine -> listOf(Solid("bench", benchSlab(), stopsArms = false), Solid("platform", platformSlab(), stopsArms = false))
    // Task 12. The pads ride on the body and the forearm support is under the forearms: only seats stop it.
    is StandingCalfMachine, is DonkeyCalfMachine, is SupportPost -> emptyList()
    is SeatedCalfMachine -> listOf(Solid("seat", seatSlab(), stopsArms = false))
    is CrunchMachine -> listOf(Solid("seat", seatSlab(), stopsArms = false), Solid("hip pad", hipPadSlab(), stopsArms = false))
    is PressMachine -> parts().toList().map { Solid("machine", it, false) }
    // Front-view seats: the seated legs are in front of them.
    is PecDeck, is Seat, is LateralRaiseMachine -> emptyList()
    // The step stops the feet; the handles are held.
    is AssistedDipMachine -> listOf(Solid("step", stepSlab(), stopsArms = false))
    // The box stops the body (it is behind the legs, under the hips); the platform stops the feet like the floor.
    is SquatBox -> listOf(Solid("box", boxSlab(), stopsArms = false))
    is DeficitPlatform -> surfaces(view).values.flatten().map { Solid("platform", it, stopsArms = true) }
    // Front view: the seated legs are in front of the seat, and the roller lies on the thighs.
    is LatPulldownSeat -> if (view == View.FRONT) emptyList() else surfaces(view).values.flatten().map { Solid("lat pulldown", it, false) }
    is LegPress45 -> surfaces(view).values.flatten().map { Solid("leg press", it, false) }
    is AdjustableBench -> surfaces(view).values.flatten().map { Solid("bench", it, false) } +
        Solid("bench", Slab.box(x - AdjustableBench.SEAT_LENGTH / 2 - 2.5, -FlatBench.HEIGHT + FlatBench.PAD, x - AdjustableBench.SEAT_LENGTH / 2 + 2.5, 0.0), false)
}

/**
 * The fixed piece's shapes in world units. [held] is the centre of a cable attachment in the scene, if any; [s] the
 * scene's skeleton, for machine handles and cables that follow the hands.
 */
internal fun FixedProp.draw(view: View, held: Point?, s: Skeleton? = null): List<Placed> {
    val e = Role.EQUIPMENT
    return when (this) {
        Floor -> listOf(Placed(Capsule(e, 0.0, FLOOR_LINE / 2, 0.0, FLOOR_LINE / 2, FLOOR_LINE), floor = true))
        is FlatBench -> buildList {
            add(eq(slabShape(e, benchPad(view), 2.0)))
            for (lx in legXs(view)) {
                add(eq(Capsule(e, lx, -height + FlatBench.PAD, lx, -1.5, 5.0)))
                add(eq(Capsule(e, lx - 12.0, -1.5, lx + 12.0, -1.5, 3.0)))
            }
        }
        is PowerRack -> if (view == View.SIDE) {
            // Drawn for a lifter to the right of the posts, then mirrored about x when the lifter is to the left.
            val m = if (lifterLeft) -1.0 else 1.0
            fun at(dx: Double) = x + m * dx
            buildList {
                for (dx in listOf(-3.0, -11.0)) add(eq(Capsule(e, at(dx), -1.5, at(dx), -height, 6.0), fit = false))
                add(eq(Capsule(e, at(-28.0), -1.5, at(10.0), -1.5, 3.0)))
                add(eq(Capsule(e, at(0.0), -jHookHeight + 1.75, at(7.0), -jHookHeight + 1.75, 3.5)))
                add(eq(Capsule(e, at(7.0), -jHookHeight + 1.75, at(7.0), -jHookHeight - 3.0, 3.0)))
                add(eq(Capsule(e, at(-14.0), -safetyHeight + 2.0, at(armLength), -safetyHeight + 2.0, 4.0)))
            }
        } else {
            buildList {
                for (s in listOf(-1.0, 1.0)) {
                    val px = x + s * 60.0
                    add(eq(Capsule(e, px, -1.5, px, -height, 6.0), fit = false))
                    add(eq(Capsule(e, px, -jHookHeight + 1.75, px - s * 7.0, -jHookHeight + 1.75, 3.5)))
                    add(eq(RoundRect(e, min(px, px - s * 10.0), -safetyHeight, 10.0, 4.0, 1.0)))
                }
                add(eq(Capsule(e, x - 75.0, -1.5, x + 75.0, -1.5, 3.0)))
            }
        }
        is CableStack -> if (view == View.FRONT && halfSpan > 0.0) drawFrontOf(s?.near?.grip, s?.far?.grip) else buildList {
            val pulley = Point(x + reach, -pulleyHeight)
            add(eq(RoundRect(e, x - 9.0, -height, 18.0, height - 1.5, 3.0), fit = false))
            add(eq(Capsule(e, x, -pulleyHeight, pulley.x, pulley.y, 5.0)))
            add(eq(Circle(e, pulley.x, pulley.y, 4.0)))
            add(eq(RoundRect(e, x - 7.0, -62.0, 14.0, 50.0, 2.0)))
            val end = held ?: (pulley + Point(0.0, 40.0))
            if (!cableInFront || held == null) add(eq(capsule(e, pulley, end, 0.8)))
        }
        is SquatBox -> listOf(eq(RoundRect(e, x - depth / 2, -height, depth, height - 0.75, 2.0)))
        is DeficitPlatform -> listOf(eq(RoundRect(e, x - length / 2, -height, length, height - 0.75, 1.0)))
        is PullingBlocks -> listOf(eq(RoundRect(e, x - length / 2, -height, length, height - 0.75, 1.0)))
        is BenchBoards -> boardSlab().let { b ->
            // One board above the other, set off by a gap line, so the stack reads as boards and not a block.
            buildList {
                for (i in 0 until boards) {
                    val top = bottom - (i + 1) * BenchBoards.BOARD
                    add(eq(RoundRect(Role.GAP, b.cx - b.length / 2 - 0.5, top - 0.5, b.length + 1.0, BenchBoards.BOARD + 1.0, 1.0)))
                    add(eq(RoundRect(e, b.cx - b.length / 2, top, b.length, BenchBoards.BOARD - 0.4, 0.8)))
                }
            }
        }
        is LatPulldownSeat -> if (view == View.FRONT) {
            listOf(
                eq(RoundRect(e, x - 22.0, -seatHeight, 44.0, 7.0, 2.5)),
                eq(Capsule(e, x, -seatHeight + 7.0, x, -1.5, 6.0)),
                eq(Capsule(e, x - 30.0, -1.5, x + 30.0, -1.5, 3.0)),
            )
        } else buildList {
            add(eq(RoundRect(e, x - 22.0, -seatHeight, 44.0, 7.0, 2.5)))
            add(eq(Capsule(e, x, -seatHeight + 7.0, x, -1.5, 6.0)))
            add(eq(Capsule(e, x - 30.0, -1.5, x + 30.0, -1.5, 3.0)))
            add(eq(Capsule(e, padX, -padHeight, padX + 14.0, -seatHeight + 4.0, 3.0)))
            add(eq(Circle(e, padX, -padHeight, LatPulldownSeat.PAD_RADIUS)))
        }
        is LegPress45 -> buildList {
            val (seat, back, plate) = legPressParts(this@draw)
            val railStart = legPressRailStart(this@draw)
            val railEnd = railStart + dir(LegPress45.RAIL_ANGLE) * 160.0
            add(eq(Capsule(e, x - 70.0, -1.5, railEnd.x, -1.5, 3.0)))
            add(eq(Capsule(e, seat.cx, seat.cy, seat.cx, -1.5, 5.0)))
            // A second leg under the back pad's low end, so the seat reads as part of the frame, not a stool.
            add(eq(Capsule(e, x - 30.0, -LegPress45.SEAT_HEIGHT, x - 40.0, -1.5, 5.0)))
            add(eq(Capsule(e, railEnd.x, railEnd.y, railEnd.x, -1.5, 5.0)))
            add(eq(capsule(e, railStart, railEnd, 5.0)))
            add(eq(slabShape(e, seat, 2.0)))
            add(eq(slabShape(e, back, 2.0)))
            // The sled: a solid wedge from its carriage on the rail out to the back of the foot plate.
            val carriage = legPressCarriage(this@draw)
            val rail = dir(LegPress45.RAIL_ANGLE)
            val along = dir(LegPress45.PLATE_ANGLE) * (plate.length / 2)
            val plateBack = Point(plate.cx, plate.cy) + dir(LegPress45.PLATE_ANGLE - 90.0) * (plate.thickness / 2)
            val sled = listOf(plateBack + along, plateBack - along, carriage - rail * 12.0, carriage + rail * 12.0)
            add(eq(Polygon(e, sled.flatMap { listOf(it.x, it.y) })))
            // The plate itself thicker than the sled behind it, set off by a gap line so the feet show on its face.
            add(eq(slabShape(Role.GAP, plate.copy(length = plate.length + 3.0, thickness = plate.thickness + 3.0), 2.5)))
            add(eq(slabShape(e, plate, 1.5)))
        }
        is DeclineBench -> drawOf()
        is RowStation -> drawOf(held)
        is RowMachine -> drawOf(s?.near?.grip)
        is ProneBench -> drawOf()
        is PreacherBench -> drawOf()
        is ArmPadMachine -> drawOf()
        is TBarLever -> drawOf(s?.near?.grip)
        is RackBar -> drawOf()
        is AssistPullUpMachine -> drawOf()
        is ParallelHandles -> drawOf()
        is PullUpBand -> emptyList()
        is DipBelt -> drawOf(s)
        is PulldownLever -> drawOf(s?.near?.grip)
        is PulloverMachine -> drawOf(s)
        is HyperBench -> drawOf()
        is ChestPlate -> drawOf(s)
        is Spotter -> drawOf()
        is SmithMachine -> drawOf()
        is HackSquatMachine -> drawOf()
        is LeverSquatMachine -> drawOf()
        is LegExtensionMachine -> drawOf()
        is PushSled -> drawOf()
        is MiniBand -> emptyList()
        is StabilityBall -> drawOf()
        is NordicStation -> drawOf()
        is ReverseHyperBench -> drawOf()
        is LyingLegCurlMachine -> drawOf()
        is SeatedLegCurlMachine -> drawOf()
        is StandingLegCurlMachine -> drawOf()
        is HipThrustMachine -> drawOf()
        is HipMachine -> drawOf(s)
        is StandingCalfMachine -> drawOf()
        is SeatedCalfMachine -> drawOf()
        is DonkeyCalfMachine -> drawOf()
        is CrunchMachine -> drawOf()
        is SupportPost -> drawOf()
        is PressMachine -> drawOf(s?.near?.grip)
        is PecDeck -> drawOf(s?.near?.grip, s?.far?.grip)
        is LateralRaiseMachine -> drawOf()
        is Seat -> drawOf()
        is DipBars -> drawOf()
        is AssistedDipMachine -> drawOf()
        is AdjustableBench -> buildList {
            val (seat, back) = adjustableParts(this@draw)
            val mid = x - AdjustableBench.SEAT_LENGTH / 2
            add(eq(Capsule(e, mid, -FlatBench.HEIGHT + FlatBench.PAD, mid, -1.5, 5.0)))
            add(eq(Capsule(e, mid - 22.0, -1.5, mid + 22.0, -1.5, 3.0)))
            add(eq(slabShape(e, seat, 2.0)))
            add(eq(slabShape(e, back, 2.0)))
        }
        // Side view: the bar is held out on a level bracket from a wall post behind the lifter (a typical wall-mounted
        // bar, 45 cm out; neutral), so the bracket marks the bar's height right across the picture. The bar itself is
        // drawn in front of the hands (drawFront).
        is PullUpBar -> if (view == View.SIDE) {
            listOf(
                eq(Capsule(e, x - 45.0, -height, x, -height, 2.5)),
                eq(Capsule(e, x - 45.0, -height - 15.0, x - 45.0, -height + 15.0, 5.0)),
            )
        } else {
            listOf(
                eq(Capsule(e, x - 60.0, -height, x - 60.0, -height - 40.0, 4.0), fit = false),
                eq(Capsule(e, x + 60.0, -height, x + 60.0, -height - 40.0, 4.0), fit = false),
                eq(Capsule(e, x - 60.0, -height, x + 60.0, -height, 2 * PullUpBar.RADIUS)),
            )
        }
    }
}

/**
 * The parts of a fixed piece drawn in front of the body: the side view's pull-up bar, which the hands wrap round, would
 * otherwise be hidden under them. Drawn at [PullUpBar.DRAWN_RADIUS] with a gap ring so it shows at phone size. And a
 * side-view cable stack's cable with [CableStack.cableInFront]: from its pulley to [held], the handle in the hand.
 */
internal fun FixedProp.drawFront(view: View, held: Point? = null, s: Skeleton? = null): List<Placed> = when {
    view == View.SIDE && this is RackBar -> barEndOn(x, height)
    view == View.SIDE && this is AssistPullUpMachine -> barEndOn(barX, barHeight)
    view == View.SIDE && this is ParallelHandles -> drawFrontOf()
    this is PullUpBand -> drawFrontOf(s)
    this is DipBelt -> drawFrontOf(s)
    view == View.SIDE && this is PulloverMachine -> drawFrontOf(s)
    view == View.SIDE && this is ArmPadMachine -> drawFrontOf(s)
    view == View.SIDE && this is HackSquatMachine -> drawFrontOf()
    view == View.SIDE && this is LeverSquatMachine -> drawFrontOf()
    view == View.SIDE && this is LegExtensionMachine -> drawFrontOf()
    this is MiniBand -> drawFrontOf(s)
    view == View.SIDE && this is LyingLegCurlMachine -> drawFrontOf()
    view == View.SIDE && this is SeatedLegCurlMachine -> drawFrontOf()
    view == View.SIDE && this is StandingLegCurlMachine -> drawFrontOf()
    view == View.SIDE && this is HipThrustMachine -> drawFrontOf()
    view == View.FRONT && this is HipMachine -> drawFrontOf(s)
    view == View.SIDE && this is ReverseHyperBench -> drawFrontOf()
    view == View.SIDE && this is StandingCalfMachine -> drawFrontOf()
    view == View.SIDE && this is SeatedCalfMachine -> drawFrontOf()
    view == View.SIDE && this is DonkeyCalfMachine -> drawFrontOf()
    view == View.SIDE && this is CrunchMachine -> drawFrontOf()
    view == View.SIDE && this is SupportPost -> drawFrontOf()
    view == View.FRONT && this is LateralRaiseMachine -> drawFrontOf(s)
    view == View.FRONT && this is PecDeck -> drawFrontOf()
    // Front view: the thigh pad's roller lies on top of the thighs, in front of the torso.
    view == View.FRONT && this is LatPulldownSeat -> listOf(
        eq(Capsule(Role.GAP, x - LatPulldownSeat.FRONT_HALF, -padHeight, x + LatPulldownSeat.FRONT_HALF, -padHeight, 2 * LatPulldownSeat.PAD_RADIUS + Body.GAP_EXTRA)),
        eq(Capsule(Role.EQUIPMENT, x - LatPulldownSeat.FRONT_HALF, -padHeight, x + LatPulldownSeat.FRONT_HALF, -padHeight, 2 * LatPulldownSeat.PAD_RADIUS)),
    )
    // Set off by a gap outline, so it shows over the pulley housing and the bench, which share its colour.
    this is CableStack && (view == View.SIDE || halfSpan == 0.0) && cableInFront && held != null -> Point(x + reach, -pulleyHeight).let { p ->
        listOf(
            eq(capsule(Role.GAP, p, held, CableStack.FRONT_CABLE + Body.GAP_EXTRA / 2)),
            eq(capsule(Role.EQUIPMENT, p, held, CableStack.FRONT_CABLE)),
        )
    }
    this is PullUpBar && view == View.SIDE -> listOf(
        eq(Circle(Role.GAP, x, -height, PullUpBar.DRAWN_RADIUS + Body.GAP_EXTRA / 2)),
        eq(Circle(Role.EQUIPMENT, x, -height, PullUpBar.DRAWN_RADIUS)),
    )
    else -> emptyList()
}

private const val FLOOR_LINE = 1.5

/** Where a held piece's centre is: in the hands (between them in the front view) or across the upper back. */
internal fun HeldProp.center(s: Skeleton): Point = when (this) {
    is Barbell -> when (on) {
        BarHold.HANDS -> grips(s)
        BarHold.UPPER_BACK ->
            if (s.view == View.SIDE) s.onTorso(Segment.TORSO.length - 4.0, -(Body.TORSO_RADIUS + Barbell.SHAFT_RADIUS))
            else s.onTorso(Segment.TORSO.length - 4.0, 0.0)
        BarHold.ON_TRAPS ->
            if (s.view == View.SIDE) s.onTorso(Segment.TORSO.length - ON_TRAPS_BELOW, -ON_TRAPS_BEHIND)
            else s.onTorso(Segment.TORSO.length - ON_TRAPS_BELOW, 0.0)
        BarHold.FRONT_RACK ->
            if (s.view == View.SIDE) s.onTorso(Segment.TORSO.length + FRONT_RACK_UP, FRONT_RACK_FRONT)
            else s.onTorso(Segment.TORSO.length + FRONT_RACK_UP, 0.0)
        BarHold.CROOK -> crookOf(s)
    }
    SafetySquatBar ->
        if (s.view == View.SIDE) s.onTorso(Segment.TORSO.length - 2.0, -(Body.TORSO_RADIUS + SafetySquatBar.PAD_RADIUS))
        else s.onTorso(Segment.TORSO.length - 2.0, 0.0)
    TrapBar -> grips(s)
    is Dumbbell -> s.side(hand).grip
    LatBar -> grips(s)
    CableHandles -> grips(s)
    is BackPlates -> centerOn(s)
    is CableAttachment -> grips(s)
    is CableHandle -> s.side(hand).grip
    is CamberedBar -> s.near.grip
    PulloverDumbbell -> PulloverDumbbell.centerOn(s)
    is Kettlebell -> s.side(hand).grip
    is AnkleStrap -> centerOn(s)
    EZBar -> grips(s)
    OverheadDumbbell -> OverheadDumbbell.centerOn(s)
    AbWheel -> grips(s)
    is PinchPlate -> centerOn(s)
}

/**
 * The front rack: the bar on the front of the shoulders, [FRONT_RACK_UP] above the shoulder joint along the torso and
 * [FRONT_RACK_FRONT] in front of it, where it sits on the forward-pointing upper arms (drawn 10 cm thick) in front of
 * the throat. A neutral drawing estimate.
 */
internal const val FRONT_RACK_UP = 6.5
internal const val FRONT_RACK_FRONT = 9.0

/** Where a safety squat bar's plates hang: below and in front of the yoke (see [SafetySquatBar]). */
internal fun safetyBarLoad(s: Skeleton): Point =
    SafetySquatBar.center(s) + Point(SafetySquatBar.CAMBER_FORWARD, SafetySquatBar.CAMBER)

/** Where a safety squat bar's handles end, in the hands: in front of the chest (see [SafetySquatBar]). */
internal fun safetyBarHandle(s: Skeleton): Point =
    s.onTorso(Segment.TORSO.length - SafetySquatBar.HANDLE_BELOW, SafetySquatBar.HANDLE_FRONT)

private fun grips(s: Skeleton): Point = if (s.view == View.SIDE) s.near.grip else (s.near.grip + s.far.grip) * 0.5

/** The held piece's shapes in world units: the chalk accent. */
internal fun HeldProp.draw(s: Skeleton): List<Placed> {
    val c = center(s)
    val a = Role.ACCENT
    val f = Role.ACCENT_FILL
    val side = s.view == View.SIDE
    return when (this) {
        is Barbell -> if (side) {
            val r = Barbell.PLATE_DIAMETER / 2
            listOf(
                Circle(f, c.x, c.y, r),
                ring(a, c.x, c.y, r, r - 1.3),
                ring(a, c.x, c.y, 5.5, 4.6),
                Circle(a, c.x, c.y, Barbell.SLEEVE_RADIUS),
            )
        } else {
            val half = Barbell.LENGTH / 2
            val inner = Barbell.INNER_COLLARS / 2
            buildList {
                add(Capsule(a, c.x - half, c.y, c.x + half, c.y, 2 * Barbell.SHAFT_RADIUS))
                for (sgn in listOf(-1.0, 1.0)) {
                    val px = c.x + sgn * (inner + 4.0)
                    add(RoundRect(f, px - 3.0, c.y - Barbell.PLATE_DIAMETER / 2, 6.0, Barbell.PLATE_DIAMETER, 1.5))
                    add(RoundRect(a, px - 1.0, c.y - Barbell.PLATE_DIAMETER / 2, 2.0, Barbell.PLATE_DIAMETER, 1.0))
                }
            }
        }
        SafetySquatBar -> {
            val r = Barbell.PLATE_DIAMETER / 2
            val plates = safetyBarLoad(s)
            val handle = safetyBarHandle(s)
            listOf(
                Circle(f, plates.x, plates.y, r),
                ring(a, plates.x, plates.y, r, r - 1.3),
                ring(a, plates.x, plates.y, 5.5, 4.6),
                Circle(a, plates.x, plates.y, Barbell.SLEEVE_RADIUS),
                capsule(a, c, plates, 2 * Barbell.SHAFT_RADIUS),
                Circle(Role.EQUIPMENT, c.x, c.y, SafetySquatBar.PAD_RADIUS),
                capsule(a, c, handle, 3.0),
            )
        }
        TrapBar -> {
            val r = Barbell.PLATE_DIAMETER / 2
            listOf(
                Circle(f, c.x, c.y, r),
                ring(a, c.x, c.y, r, r - 1.3),
                capsule(a, Point(c.x - TrapBar.FRAME / 2, c.y), Point(c.x + TrapBar.FRAME / 2, c.y), 2 * Barbell.SHAFT_RADIUS),
                Circle(a, c.x, c.y, Barbell.SLEEVE_RADIUS),
            )
        }
        is Dumbbell -> if (side && neutral) {
            // In profile along the torso's line (or square to the hand): the handle, then a head at each end (5 cm
            // thick, 20 cm across).
            val angle = if (squareToForearm) s.side(hand).let { angleOf(it.hand - it.wrist) + 90.0 } else s.torso
            val along = dir(angle)
            val h = Dumbbell.HANDLE / 2
            listOf(
                Capsule(a, c.x - along.x * h, c.y - along.y * h, c.x + along.x * h, c.y + along.y * h, 3.2),
            ) + listOf(-1.0, 1.0).map { sgn ->
                val m = c + along * (sgn * (h + 2.5))
                Polygon(a, Slab(m.x, m.y, 5.0, 2 * Dumbbell.HEAD_RADIUS, angle, 0.0).corners().flatMap { listOf(it.x, it.y) })
            }
        } else if (side || endOnFront) {
            listOf(
                Circle(f, c.x, c.y, Dumbbell.HEAD_RADIUS),
                ring(a, c.x, c.y, Dumbbell.HEAD_RADIUS, Dumbbell.HEAD_RADIUS - 1.2),
                Circle(a, c.x, c.y, 1.6),
            )
        } else {
            val h = Dumbbell.HANDLE / 2
            listOf(
                Capsule(a, c.x - h, c.y, c.x + h, c.y, 3.2),
                RoundRect(a, c.x - h - 5.0, c.y - Dumbbell.HEAD_RADIUS, 5.0, 2 * Dumbbell.HEAD_RADIUS, 1.5),
                RoundRect(a, c.x + h, c.y - Dumbbell.HEAD_RADIUS, 5.0, 2 * Dumbbell.HEAD_RADIUS, 1.5),
            )
        }
        CableHandles -> CableHandles.drawOn(s)
        is BackPlates -> drawOn(s)
        is CableAttachment -> drawOn(s)
        is CableHandle -> drawOn(s)
        is CamberedBar -> drawOn(s)
        PulloverDumbbell -> PulloverDumbbell.drawOn(s)
        is Kettlebell -> drawOn(s)
        is AnkleStrap -> drawOn(s)
        EZBar -> EZBar.drawOn(s)
        OverheadDumbbell -> OverheadDumbbell.drawOn(s)
        AbWheel -> AbWheel.drawOn(s)
        is PinchPlate -> drawOn(s)
        LatBar -> if (side) {
            listOf(Circle(a, c.x, c.y, 2.0))
        } else {
            val straight = LatBar.HALF_LENGTH - 18.0
            val l = Point(c.x - straight, c.y)
            val r = Point(c.x + straight, c.y)
            listOf(
                capsule(a, l, r, 2.6),
                capsule(a, l, l + dir(200.0) * 18.0, 2.6),
                capsule(a, r, r + dir(-20.0) * 18.0, 2.6),
            )
        }
    }.map { Placed(it) }
}
