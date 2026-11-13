package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_IT
import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.*
import kotlin.math.abs
import kotlin.math.sqrt

// Figures for the library's Calves area (library/areas/Calves.kt), keyed by catalogue name.
//
// Camera (Task 12): the side view for every calf raise. The whole movement is the ankle, a bend in the sagittal plane:
// the heels drop below the block's edge, then the lifter rises onto the balls of the feet. START is the bottom (the
// heels below the edge, the stretch), END the top (the heels raised); the guides' steps raise and lower in turn.
//
// Ankle range, both panels, every calf raise: 41° from the bottom to the top with the knees straight, 38° with the
// knees bent. On a step, healthy volunteers' ankle range in the heel drop was about 41° with the knee straight and
// about 38° with it bent (https://pmc.ncbi.nlm.nih.gov/articles/PMC4424477/, seen in the search result). The split
// between below and above level is neutral: the heels 16° below level at the bottom (15° seated), so the drop shows, 25° plantar flexion
// at the top (23° seated), inside the 25–50° a standing heel raise reaches
// (https://pmc.ncbi.nlm.nih.gov/articles/PMC5343533/, seen in the search result) and the figure's joint limits (Body.kt).
// The calf block is the deficit platform at 10 cm (a typical calf block or step; neutral).
// The figure's foot is one straight piece (heel to toe tips, no toe joint). At the bottom the block's edge is under the
// ball of the foot, 5 cm back from the toe's end, the heel hanging below the block's top. At the top the foot pivots on
// the toe tips, which stay where they were on the block, so the heel rises well above the block's top (Quads.kt's
// raised-heel foot: lesson 12).

private fun figure(
    view: View,
    start: Pose,
    end: Pose,
    props: List<Prop>,
    startCaption: String,
    endCaption: String,
    thumbnail: Which,
    guideLines: List<String>,
    contacts: List<Contact>,
    checks: List<KeyCheck>,
    endProps: List<Prop> = props,
) = ExerciseFigure(view, Scene(start, props), Scene(end, endProps), startCaption, endCaption, thumbnail, guideLines, contacts, checks)

private val HANG = Arm(0.0, 0.0)

// Guide lines shared by several calf guides (library/areas/Calves.kt), verbatim.
private const val BRACE_TRUNK = "Brace your trunk before each rep."
private const val BALLS_ON_BLOCK = "Put the balls of your feet on the calf block, with your heels hanging off the edge."
private const val RAISE_HEELS = "Raise your heels by pointing your ankles."
private const val LOWER_HEELS = "Slowly lower your heels below the edge of the block."

private const val HEELS_BELOW = "Heels below the block"
private const val RAISE = "Raise your heels"

/** The calf block's height, cm (a typical calf block or step; neutral). */
internal const val CALF_BLOCK = 10.0

/** The heels' angle below level at the bottom, and the plantar flexion at the top: straight knees, then bent (see the file's header). */
internal const val CALF_DROP = 16.0
internal const val CALF_RISE = -25.0
internal const val SEATED_DROP = 15.0
internal const val SEATED_RISE = -23.0

/** How far back from the toe's end the block's edge sits under the foot at the bottom (the ball of the foot), cm. */
private const val BALL_BACK = 5.0

/** The block: its back edge (where the heels hang off) at x = 0, running 35 cm forward. */
private val calfBlock = DeficitPlatform(x = 17.5, height = CALF_BLOCK, length = 35.0)

/**
 * The foot on an edge: the edge's corner [corner], the surface running forward along [forward]° (0 for a block on the
 * floor) and facing up from it at [forward] + 90°. With the foot at [rel]° to the surface (+: the toes up, the heel down
 * past the edge), returns the ankle joint and the foot's screen angle. [toeAlong]: where the toe's end sits along the
 * surface from the corner (null: the bottom, the corner under the ball of the foot).
 */
internal class FootOnEdge(val ankle: Point, val angle: Double, val toeAlong: Double)

internal fun footOnEdge(corner: Point, forward: Double, rel: Double, toeAlong: Double? = null): FootOnEdge {
    val r = Body.FOOT_WIDTH / 2
    val g = forward + rel
    val fd = unit(g)
    val toe = if (toeAlong == null) {
        // The corner touches the foot's underside, BALL_BACK behind the toe's end.
        corner + unit(g + 90.0) * r + fd * BALL_BACK
    } else {
        // The toe's end rests on the surface.
        corner + unit(forward) * toeAlong + unit(forward + 90.0) * r
    }
    val sole = unit(g - 90.0) * (Body.ANKLE_HEIGHT - r)
    val ankle = toe - sole - fd * (Segment.FOOT.length * 0.8 - r)
    val along = (toe - corner).let { it.x * unit(forward).x + it.y * unit(forward).y }
    return FootOnEdge(ankle, g, along)
}

/** The bottom and top feet on [corner] (a block on the floor unless [forward] says), for the drop and rise given. */
private fun feetOn(corner: Point, drop: Double, rise: Double, forward: Double = 0.0): Pair<FootOnEdge, FootOnEdge> {
    val low = footOnEdge(corner, forward, drop)
    return low to footOnEdge(corner, forward, rise, toeAlong = low.toeAlong)
}

private val blockCorner = Point(0.0, -CALF_BLOCK)

private val LEG = Segment.THIGH.length + Segment.SHIN.length

/**
 * A straight leg (the knee a hair off straight) from the pelvis at [pelvisX], standing on [foot]: the pelvis as high as
 * the leg reaches. The legs lean a degree or two as the ankle moves forward onto the toes.
 */
private fun straightLeg(pelvisX: Double, foot: FootOnEdge, torso: Double): Pair<Point, Leg> {
    val dx = foot.ankle.x - pelvisX
    val pelvis = Point(pelvisX, foot.ankle.y - sqrt((LEG - 1e-3) * (LEG - 1e-3) - dx * dx))
    return pelvis to Reach.leg(pelvis, foot.ankle, torso, foot.angle)
}

/**
 * A standing calf raise's two bodies: the pelvis halfway between the two ankles' x, so the body stays over the feet.
 * [torso] and [neck] as in [Pose]: a lean forward with the neck at minus the lean keeps the head level.
 */
private class Standing(val torso: Double = 90.0, val neck: Double = 0.0) {
    val feet = feetOn(blockCorner, CALF_DROP, CALF_RISE)
    val pelvisX = (feet.first.ankle.x + feet.second.ankle.x) / 2
    val low = straightLeg(pelvisX, feet.first, torso)
    val high = straightLeg(pelvisX, feet.second, torso)

    fun pose(which: Which, near: (Point) -> Arm, far: ((Point) -> Arm)? = null, farLeg: Leg? = null): Pose {
        val (pelvis, leg) = if (which == Which.START) low else high
        val sh = shoulderOf(pelvis, torso)
        val n = near(sh)
        return Pose(pelvis.x, pelvis.y, torso, neck, nearArm = n, nearLeg = leg, farArm = far?.invoke(sh) ?: n, farLeg = farLeg ?: leg)
    }
}

private val ON_BLOCK = listOf(Contact(BodyPoint.FEET, Surface.PLATFORM, BOTH))

/** The point on the perpendicular bisector of [a] and [b] at x = [x]: a pivot whose lever is the same length to both. */
private fun leverPivot(a: Point, b: Point, x: Double): Point {
    val m = (a + b) * 0.5
    val d = b - a
    // (p - m) · d = 0 with p.x = x.
    return if (abs(d.y) < 1e-9) Point(x, m.y - 40.0) else Point(x, m.y - (x - m.x) * d.x / d.y)
}

/**
 * Standing calf raise. On the machine: the shoulder pads on top of the shoulders, the hands on the pads' handles in
 * front of the shoulders, the balls of the feet on the calf block, the body tall with the knees straight. START the
 * heels below the block's edge; END raised (the header's range). The pad rides on the shoulders, its lever turning on
 * the column behind the lifter. ExRx lever standing calf raise
 * (https://exrx.net/WeightExercises/Gastrocnemius/LVStandingCalfRaise, Sources.kt).
 */
private val standingCalfRaise: ExerciseFigure by lazy {
    val st = Standing()
    fun handle(sh: Point) = sh + Point(18.0, 12.0)
    fun pad(p: Pose) = p.skeleton().let { it.onTorso(Segment.TORSO.length + 5.0 + StandingCalfMachine.PAD_RADIUS, -3.0) }
    val start = st.pose(Which.START, { armTo(it, handle(it), 90.0) })
    val end = st.pose(Which.END, { armTo(it, handle(it), 90.0) })
    val columnX = st.pelvisX - 45.0
    val pivot = leverPivot(pad(start), pad(end), columnX)
    fun machine(p: Pose) = StandingCalfMachine(columnX, pivot, pad(p), handle(shoulderOf(Point(p.pelvisX, p.pelvisY), 90.0)))
    figure(
        View.SIDE, start, end, listOf(Floor, calfBlock, machine(start)),
        startCaption = HEELS_BELOW,
        endCaption = RAISE,
        thumbnail = Which.END,
        guideLines = listOf("Step under the pads and put them on your shoulders.", BALLS_ON_BLOCK, RAISE_HEELS, LOWER_HEELS),
        contacts = ON_BLOCK + Contact(BodyPoint.HANDS, Surface.HANDLE, BOTH),
        checks = emptyList(),
        endProps = listOf(Floor, calfBlock, machine(end)),
    )
}

/**
 * The torso with a bar on the back, degrees: 5° forward of upright, the small natural lean of a loaded bar on the back
 * (neutral), the back straight and the head level (the neck at minus the lean), so the chest shows in front and the
 * lifter still stands tall (Task 14).
 */
private const val BACK_BAR_TORSO = 85.0

/**
 * Where the grip is out to the side of its shoulder joint with the bar across the upper back, cm: the hands just outside
 * the shoulders (Body.SHOULDER_HALF_WIDTH 18, so about 38 cm either side of the middle). Neutral, and the least that
 * lets the figure's arm fold to the bar within its elbow (Poses.kt MAX_ELBOW): about 144° here.
 */
private const val BACK_GRIP_OUT = 20.0

/**
 * The arm holding a bar across the upper back ([BarHold.ON_TRAPS]) for an upright lifter standing on [pelvis] with the
 * torso at [torso]° (Task 14, the owner's finding). The grip lands on the bar, seen end-on, [BACK_GRIP_OUT] out to the
 * side of the shoulder; the upper arm runs down and a little back from the shoulder (out of the picture towards the
 * camera, so it is drawn foreshortened, Arm.out) and the forearm comes back up to the bar from below the elbow. So the
 * elbows point down and slightly back, under and just behind the bar, and nothing crosses the chest: the lifter reads
 * as facing his toes. The drawn arm before (Squat's turned-out arm, Poses.kt armTo) put the elbow in front of the
 * chest with the forearm slanting back across it, which read as a bar held in front of an upright lifter facing left.
 */
private fun armToBackBar(pelvis: Point, torso: Double): Arm {
    val bar = Barbell(BarHold.ON_TRAPS).center(Pose(pelvis.x, pelvis.y, torso, nearArm = HANG, nearLeg = Leg(0.0, 0.0)).skeleton())
    // The elbow's side: down the torso, a little towards its back, and out towards the camera with the grip.
    val down = unit(torso) * -1.0
    val back = unit(torso - 90.0) * -1.0
    val hint = v3(down + back * 0.5, 0.5)
    return arm3(v3(shoulderOf(pelvis, torso), 0.0), v3(bar, BACK_GRIP_OUT), hint, torso)
}

/**
 * Barbell calf raise. The bar across the upper back, seated on top of the shoulders ([BarHold.ON_TRAPS]), gripped just
 * outside the shoulders with the elbows down ([armToBackBar]), the balls of the feet on the block, the knees straight. START the heels below the edge, END raised. In a power
 * rack, the guide's safety line: the J-hooks 8 cm under the bar's lowest point (it was lifted out of them by standing
 * up) and the safety arms 16 cm under it. ExRx barbell standing calf raise
 * (https://exrx.net/WeightExercises/Gastrocnemius/BBStandingCalfRaise, Sources.kt).
 */
private val barbellCalfRaise: ExerciseFigure by lazy {
    val st = Standing(torso = BACK_BAR_TORSO, neck = BACK_BAR_TORSO - 90.0)
    val start = st.pose(Which.START, { armToBackBar(st.low.first, st.torso) })
    val end = st.pose(Which.END, { armToBackBar(st.high.first, st.torso) })
    val bar = Barbell(BarHold.ON_TRAPS)
    val low = bar.center(start.skeleton())
    val rack = rackInFront(low.x, jHookHeight = -low.y - 8.0, safetyHeight = -low.y - 16.0, gap = 75.0)
    figure(
        View.SIDE, start, end, listOf(Floor, calfBlock, rack, bar),
        startCaption = HEELS_BELOW,
        endCaption = "Raise heels, pause",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Step under the bar and rest it across your upper back. Grip it just outside your shoulders.",
            "Put the balls of your feet on the block, heels hanging off, feet about hip-width apart and pointing forward.",
            "$RAISE_HEELS Pause at the top.",
            LOWER_HEELS,
        ),
        contacts = ON_BLOCK,
        checks = emptyList(),
    )
}

/**
 * Smith calf raise. The barbell calf raise's body, bar and arms ([armToBackBar]), the Smith bar held at the sides; the
 * pelvis is at the same x in both panels, so the bar runs straight up the rails (BarOnRails). The safety stops 4 cm
 * under the bar's lowest point (guide: just below your lowest position). ExRx Smith standing calf raise
 * (https://exrx.net/WeightExercises/Gastrocnemius/SMStandingCalfRaise, Sources.kt).
 */
private val smithCalfRaise: ExerciseFigure by lazy {
    val st = Standing(torso = BACK_BAR_TORSO, neck = BACK_BAR_TORSO - 90.0)
    val start = st.pose(Which.START, { armToBackBar(st.low.first, st.torso) })
    val end = st.pose(Which.END, { armToBackBar(st.high.first, st.torso) })
    val bar = Barbell(BarHold.ON_TRAPS)
    val low = bar.center(start.skeleton())
    // The stop reaches 20 cm back from the rails, so it shows past the back under the bar (fix round 1).
    val smith = SmithMachine(x = low.x, stopHeight = -low.y - Barbell.SHAFT_RADIUS - 4.0, stopBehind = 20.0)
    figure(
        View.SIDE, start, end, listOf(Floor, calfBlock, smith, bar),
        startCaption = HEELS_BELOW,
        endCaption = RAISE,
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the safety stops just below your lowest position.",
            "Step under the bar and rest it across your upper back. Hold the bar at the sides.",
            BALLS_ON_BLOCK, RAISE_HEELS, LOWER_HEELS,
        ),
        contacts = ON_BLOCK,
        checks = listOf(BarOnRails(Which.START), BarOnRails(Which.END)),
    )
}

/** A dumbbell hanging at the side, palms in: seen in profile from the side, square to the hand (Quads.kt's). */
private fun sideDumbbell(side: Side) = Dumbbell(side, neutral = true, squareToForearm = true)

/** A balance support in front of the block: a post 15 cm past the toes, its grip [height] cm above the floor (neutral). */
private fun supportFor(st: Standing, height: Double = 110.0) =
    SupportPost(x = st.feet.first.ankle.x + Segment.FOOT.length + 15.0, gripHeight = height)

/** The far arm reaching forward to the support's grip (the hand on the grip's middle). */
private fun onSupport(post: SupportPost): (Point) -> Arm = { sh -> armTo(sh, Point(post.x - 6.0, -post.gripHeight), 90.0) }

/**
 * Dumbbell calf raise. Both feet on the block, a dumbbell hanging at the near side in one hand (palms in), the far hand
 * on a support in front for balance. START the heels below the edge, END raised. The support's height and distance
 * are neutral. ExRx dumbbell standing calf raise (https://exrx.net/WeightExercises/Gastrocnemius/DBStandingCalfRaise,
 * Sources.kt).
 */
private val dumbbellCalfRaise: ExerciseFigure by lazy {
    val st = Standing()
    val post = supportFor(st)
    val start = st.pose(Which.START, { HANG }, onSupport(post))
    val end = st.pose(Which.END, { HANG }, onSupport(post))
    figure(
        View.SIDE, start, end, listOf(Floor, calfBlock, post, sideDumbbell(Side.NEAR)),
        startCaption = HEELS_BELOW,
        endCaption = RAISE,
        thumbnail = Which.END,
        guideLines = listOf(
            "Hold a dumbbell at your side in one hand. Hold a support with the other hand for balance.",
            "Put the balls of your feet on a calf block or step, with your heels hanging off the edge.",
            RAISE_HEELS, LOWER_HEELS,
        ),
        contacts = ON_BLOCK + Contact(BodyPoint.FAR_HAND, Surface.HANDLE, BOTH),
        checks = emptyList(),
    )
}

/** The free leg of the single-leg raise: the knee bent 90° with the thigh down, the foot behind, clear of the step. */
private val FREE_LEG = Leg(hip = 5.0, knee = 90.0, ankle = -20.0)

/**
 * Single-leg calf raise. On one foot on the step (the near foot), the other leg bent behind with its foot off the step,
 * the far hand on a support for balance, the near arm at the side (bodyweight: the optional dumbbell is not drawn).
 * START the heel below the step's edge, END raised by pointing the ankle, the knee straight. The free leg's bend is
 * neutral. ACE (https://www.acefitness.org/resources/pros/expert-articles/5771/get-stronger-calves-with-these-5-exercises/)
 * and ExRx dumbbell single-leg calf raise (https://exrx.net/WeightExercises/Gastrocnemius/DBSingleLegCalfRaise),
 * Sources.kt. The bent free leg sets it apart from the two-foot dumbbell calf raise at a glance (lesson 10).
 */
private val singleLegCalfRaise: ExerciseFigure by lazy {
    val st = Standing()
    val post = supportFor(st)
    val start = st.pose(Which.START, { HANG }, onSupport(post), farLeg = FREE_LEG)
    val end = st.pose(Which.END, { HANG }, onSupport(post), farLeg = FREE_LEG)
    figure(
        View.SIDE, start, end, listOf(Floor, calfBlock, post),
        startCaption = "Heel below the step",
        endCaption = "Knee straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand on a step on one foot, with your heel hanging off the edge. Make sure the ball of your foot is firmly on the step.",
            "Slowly lower your heel below the edge of the step.",
            "Raise your heel by pointing your ankle, keeping your knee straight.",
        ),
        contacts = listOf(Contact(BodyPoint.NEAR_FOOT, Surface.PLATFORM, BOTH), Contact(BodyPoint.FAR_HAND, Surface.HANDLE, BOTH)),
        checks = emptyList(),
    )
}

// ---- bent-knee and bent-over raises ----

/**
 * A seated calf raise's legs: the pelvis fixed on the seat, the feet on the block's edge (the seated range), the knees
 * about 90° with the shins upright at the bottom; at the top the ankles rise and the knees with them.
 */
private class SeatedLegs(pelvisY: Double? = null) {
    val low = footOnEdge(blockCorner, 0.0, SEATED_DROP)
    val knee0 = low.ankle + Point(0.0, -Segment.SHIN.length)
    val pelvis = (pelvisY ?: knee0.y).let { y -> Point(knee0.x - sqrt(Segment.THIGH.length * Segment.THIGH.length - (y - knee0.y) * (y - knee0.y)), y) }

    /** The top's foot: turned on the toe tips until the ankle itself is pointed [SEATED_RISE]° (the knee rises, so the shin tips). */
    val high: FootOnEdge by lazy {
        val rel = solveAngle(-60.0, 0.0) { r -> footOnEdge(blockCorner, 0.0, r, low.toeAlong).let { Reach.leg(pelvis, it.ankle, 90.0, it.angle).ankle } - SEATED_RISE }
        footOnEdge(blockCorner, 0.0, rel, low.toeAlong)
    }

    fun foot(which: Which) = if (which == Which.START) low else high

    fun leg(which: Which, torso: Double = 90.0): Leg = foot(which).let { Reach.leg(pelvis, it.ankle, torso, it.angle) }
}

/** A point on the near thigh [along] of the way from the hip to the knee, [up] off its top surface. */
private fun onThigh(s: Skeleton, along: Double, up: Double): Point {
    val h = s.near.hip
    val k = s.near.knee
    val d = (k - h) * (1.0 / (k - h).length())
    // The thigh's top: the normal on the upper side (y down, so the one with negative y).
    val n = Point(-d.y, d.x).let { if (it.y > 0) it * -1.0 else it }
    return h + (k - h) * along + n * (Body.THIGH_WIDTH / 2 + up)
}

/**
 * Seated calf raise. Sitting tall on the machine's seat, the knee pad on the lower thighs, the hands resting on the
 * pad, the balls of the feet on the platform's edge, the knees bent about 90° with the shins upright (neutral). START
 * the heels below the platform's edge; END raised, the knees and the pad rising with them (the seated range in the
 * header). The pad's lever turns on a post in front of the platform. ExRx lever seated calf raise
 * (https://exrx.net/WeightExercises/Soleus/LVSeatedCalfRaise, Sources.kt).
 */
private val seatedCalfRaise: ExerciseFigure by lazy {
    val legs = SeatedLegs()
    fun pose(which: Which): Pose {
        val bare = Pose(legs.pelvis.x, legs.pelvis.y, 90.0, nearArm = HANG, nearLeg = legs.leg(which))
        val pad = onThigh(bare.skeleton(), 0.8, SeatedCalfMachine.PAD_RADIUS)
        val hand = pad + Point(-2.0, -(SeatedCalfMachine.PAD_RADIUS + Body.HAND_WIDTH / 2))
        return bare.copy(nearArm = armTo(shoulderOf(legs.pelvis, 90.0), hand, 90.0, wrist = 0.0)).let { it.copy(farArm = it.nearArm) }
    }
    val start = pose(Which.START)
    val end = pose(Which.END)
    fun pad(p: Pose) = onThigh(p.skeleton(), 0.8, SeatedCalfMachine.PAD_RADIUS)
    val pivotX = legs.low.ankle.x + 45.0
    val pivot = leverPivot(pad(start), pad(end), pivotX)
    fun machine(p: Pose) = SeatedCalfMachine(seatX = legs.pelvis.x - 4.0, seatHeight = -legs.pelvis.y - Body.TORSO_RADIUS, pivot = pivot, pad = pad(p))
    figure(
        View.SIDE, start, end, listOf(Floor, calfBlock, machine(start)),
        startCaption = "Heels below the platform",
        endCaption = RAISE,
        thumbnail = Which.END,
        guideLines = listOf(
            "Put the balls of your feet on the platform, with your heels hanging off the edge.",
            "Set the pads on your lower thighs.",
            RAISE_HEELS,
            "Slowly lower your heels below the edge of the platform.",
        ),
        contacts = listOf(
            Contact(BodyPoint.FEET, Surface.PLATFORM, BOTH), Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.HANDS, Surface.THIGH_PAD, BOTH),
        ),
        checks = emptyList(),
        endProps = listOf(Floor, calfBlock, machine(end)),
    )
}

/**
 * Seated dumbbell calf raise. Sitting on the end of a flat bench (43 cm), both feet on a step, the heels hanging off;
 * one dumbbell across the tops of the thighs by the knees (its handle across the body: end on, a disc), held there with
 * both hands. The thighs slope up a little to the knees, the shins upright at the bottom. START the heels below the
 * step's edge, END raised (the seated range). ACE (https://www.acefitness.org/resources/pros/expert-articles/5771/get-stronger-calves-with-these-5-exercises/,
 * Sources.kt).
 */
private val seatedDumbbellCalfRaise: ExerciseFigure by lazy {
    val legs = SeatedLegs(pelvisY = -FlatBench.HEIGHT - Body.TORSO_RADIUS)
    val pelvis = legs.pelvis
    fun pose(which: Which): Pose {
        val bare = Pose(pelvis.x, pelvis.y, 90.0, nearArm = HANG, nearLeg = legs.leg(which))
        val disc = onThigh(bare.skeleton(), 0.75, Dumbbell.HEAD_RADIUS)
        return bare.copy(nearArm = armTo(shoulderOf(pelvis, 90.0), disc, 90.0)).let { it.copy(farArm = it.nearArm) }
    }
    val bench = FlatBench(x = pelvis.x - 70.0, length = 82.0)
    val props = listOf(Floor, bench, calfBlock, Dumbbell(Side.NEAR))
    figure(
        View.SIDE, pose(Which.START), pose(Which.END), props,
        startCaption = "Heels below the step",
        endCaption = RAISE,
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on a bench with both feet on a step and your heels hanging off the edge.",
            "Put a dumbbell or a plate across the tops of your thighs.",
            "Slowly lower your heels below the edge of the step.",
            RAISE_HEELS,
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.PLATFORM, BOTH), Contact(BodyPoint.HIPS, Surface.BENCH, BOTH)),
        checks = emptyList(),
    )
}

/** The donkey raise's torso at the bottom, degrees above level (bent over at the hips, the back flat; neutral). */
internal const val DONKEY_TORSO = 15.0

/**
 * Donkey calf raise. Bent over at the hips, the back straight and near level (15° above, neutral), the forearms on the
 * machine's support in front, the pad across the lower back and hips, the knees straight, the balls of the feet on
 * the block. START the heels below the edge; END raised: the forearms stay on the support, so the hips rise and the
 * torso pivots about the shoulders (the pad rides up with the hips). ExRx lever donkey calf raise
 * (https://exrx.net/WeightExercises/Gastrocnemius/LVDonkeyCalfRaise, Sources.kt).
 */
private val donkeyCalfRaise: ExerciseFigure by lazy {
    val feet = feetOn(blockCorner, CALF_DROP, CALF_RISE)
    val (p0, _) = straightLeg(feet.first.ankle.x + 1.5, feet.first, DONKEY_TORSO)
    val shoulder = shoulderOf(p0, DONKEY_TORSO)
    // The top: the pelvis where the straight leg reaches from the raised ankle and the torso still reaches the shoulders.
    val a1 = feet.second.ankle
    val t = solveAngle(DONKEY_TORSO - 30.0, DONKEY_TORSO) { tt -> ((shoulder - unit(tt) * Segment.TORSO.length) - a1).length() - (LEG - 1e-3) }
    val p1 = shoulder - unit(t) * Segment.TORSO.length
    // The upper arm 25° forward of vertical, the forearm level on the support (neutral).
    val elbow = shoulder + unit(-65.0) * Segment.UPPER_ARM.length
    fun arm(torso: Double): Arm {
        val u = angleTo(elbow - shoulder)
        return Arm(shoulder = fold180(u - torso - 180.0), elbow = fold180(360.0 - u))
    }
    fun pose(pelvis: Point, torso: Double, foot: FootOnEdge) =
        Pose(pelvis.x, pelvis.y, torso, neck = -DONKEY_TORSO, nearArm = arm(torso), nearLeg = Reach.leg(pelvis, foot.ankle, torso, foot.angle))
    val start = pose(p0, DONKEY_TORSO, feet.first)
    val end = pose(p1, t, feet.second)
    val supportTop = -(elbow.y + Body.FOREARM_WIDTH / 2)
    val columnX = elbow.x + Segment.FOREARM.length + Segment.HAND.length + 6.0
    fun pad(p: Pose) = p.skeleton().onTorso(6.0, -(Body.TORSO_RADIUS + DonkeyCalfMachine.PAD_RADIUS))
    val pivot = leverPivot(pad(start), pad(end), columnX)
    fun machine(p: Pose) = DonkeyCalfMachine(columnX, supportTop, columnX - elbow.x + 4.0, pivot, pad(p))
    figure(
        View.SIDE, start, end, listOf(Floor, calfBlock, machine(start)),
        startCaption = HEELS_BELOW,
        endCaption = RAISE,
        thumbnail = Which.END,
        guideLines = listOf(
            "Get under the machine with the pad on your lower back and hips.",
            "Rest your forearms on the supports.",
            BALLS_ON_BLOCK, RAISE_HEELS, LOWER_HEELS,
        ),
        contacts = ON_BLOCK + Contact(BodyPoint.ELBOWS, Surface.ARM_PAD, BOTH),
        checks = emptyList(),
        endProps = listOf(Floor, calfBlock, machine(end)),
    )
}

// ---- the leg press calf raise ----

/**
 * Leg press calf raise. On the approved Leg press's machine and seat (Quads.kt's geometry): the back and hips on the
 * pads, the hands on the side handles (the arms resting along the body), the legs straight (the knees 3°, the guide's
 * "until your legs are straight"; "or only slightly bent"), the toes and balls of the feet on the platform's lower
 * edge with the heels hanging off. The platform's face is the step: START the heels below its edge, END the platform
 * pushed away by pointing the ankles (the straight-knee range in the header). The sled position follows from the leg:
 * it slides along its rail until the straight leg's foot is on its edge. ExRx sled 45° calf press
 * (https://exrx.net/WeightExercises/Gastrocnemius/SL45CalfPress, Sources.kt).
 */
private val legPressCalfRaise: ExerciseFigure by lazy {
    val back = LegPress45.BACK_ANGLE
    val corner = Point(-30.0, -LegPress45.SEAT_HEIGHT)
    val axisStart = corner + unit(back - 90.0) * (6.0 + Body.TORSO_RADIUS)
    val pelvisY = -(LegPress45.SEAT_HEIGHT + Body.TORSO_RADIUS)
    val pelvis = axisStart + unit(back) * ((pelvisY - axisStart.y) / unit(back).y)
    val knee = 3.0
    val reach = sqrt(
        Segment.THIGH.length * Segment.THIGH.length + Segment.SHIN.length * Segment.SHIN.length +
            2 * Segment.THIGH.length * Segment.SHIN.length * kotlin.math.cos(knee / DEG),
    ) - 1e-6
    fun edgeOf(press: LegPress45): Point {
        val plate = press.surfaces(View.SIDE).getValue(Surface.FOOT_PLATE).single()
        val face = Point(plate.cx, plate.cy) - unit(LegPress45.PLATE_ANGLE - 90.0) * (plate.thickness / 2)
        return face - unit(LegPress45.PLATE_ANGLE) * (plate.length / 2)
    }
    // The face's "up" (towards the lifter) is PLATE_ANGLE + 90; its "forward" along it, up the plate, PLATE_ANGLE.
    val forward = LegPress45.PLATE_ANGLE
    fun scene(rel: Double, toeAlong: Double?): Pair<Scene, FootOnEdge> {
        val sled = solveAngle(0.0, 120.0) { s -> (footOnEdge(edgeOf(LegPress45(0.0, s)), forward, rel, toeAlong).ankle - pelvis).length() - reach }
        val press = LegPress45(0.0, sled)
        val foot = footOnEdge(edgeOf(press), forward, rel, toeAlong)
        val leg = Reach.leg(pelvis, foot.ankle, back, foot.angle)
        return Scene(Pose(pelvis.x, pelvis.y, back, nearArm = Arm(15.0, 25.0), nearLeg = leg), listOf(Floor, press)) to foot
    }
    // The bottom's foot angle is solved so the ankle is dorsiflexed by the header's drop, like the standing raises.
    val rel0 = solveAngle(-20.0, 40.0) { r -> scene(r, null).first.pose.nearLeg.ankle - CALF_DROP }
    val (start, low) = scene(rel0, null)
    val rel1 = solveAngle(-80.0, 0.0) { r -> scene(r, low.toeAlong).first.pose.nearLeg.ankle - CALF_RISE }
    val (end, _) = scene(rel1, low.toeAlong)
    ExerciseFigure(
        View.SIDE, start, end,
        startCaption = "Heels below the edge",
        endCaption = "Point your ankles",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your back against the back pad and hold the side handles.",
            "Put your toes and the balls of your feet on the lower edge of the platform, with your heels and arches hanging off.",
            "Push the platform away by pointing your ankles.",
            "Slowly let your heels come back below the edge of the platform.",
        ),
        contacts = listOf(
            Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.FEET, Surface.FOOT_PLATE, BOTH),
        ),
        checks = emptyList(),
    )
}

internal val calfFigures: Map<String, Lazy<ExerciseFigure>> = mapOf(
    "Standing calf raise" to lazy { standingCalfRaise },
    "Seated calf raise" to lazy { seatedCalfRaise },
    "Leg press calf raise" to lazy { legPressCalfRaise },
    "Donkey calf raise" to lazy { donkeyCalfRaise },
    "Barbell calf raise" to lazy { barbellCalfRaise },
    "Smith calf raise" to lazy { smithCalfRaise },
    "Dumbbell calf raise" to lazy { dumbbellCalfRaise },
    "Single-leg calf raise" to lazy { singleLegCalfRaise },
    "Seated dumbbell calf raise" to lazy { seatedDumbbellCalfRaise },
)
