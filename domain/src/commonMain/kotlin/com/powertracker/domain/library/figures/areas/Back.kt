package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.OVER_FACE_SPOT
import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.AssistPullUpMachine
import com.powertracker.domain.library.figures.Attachment
import com.powertracker.domain.library.figures.BarTouches
import com.powertracker.domain.library.figures.Barbell
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.CableAttachment
import com.powertracker.domain.library.figures.CableHandle
import com.powertracker.domain.library.figures.CableStack
import com.powertracker.domain.library.figures.CamberedBar
import com.powertracker.domain.library.figures.ChestPlate
import com.powertracker.domain.library.figures.ChinOverBar
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.DipBelt
import com.powertracker.domain.library.figures.Dumbbell
import com.powertracker.domain.library.figures.ElbowBend
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FlatBench
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.HandlesBesideLegs
import com.powertracker.domain.library.figures.HandsLevelWith
import com.powertracker.domain.library.figures.HeldBeside
import com.powertracker.domain.library.figures.HeldOver
import com.powertracker.domain.library.figures.HyperBench
import com.powertracker.domain.library.figures.KeyCheck
import com.powertracker.domain.library.figures.LatBar
import com.powertracker.domain.library.figures.LatPulldownSeat
import com.powertracker.domain.library.figures.Leg
import com.powertracker.domain.library.figures.ParallelHandles
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.ProneBench
import com.powertracker.domain.library.figures.Prop
import com.powertracker.domain.library.figures.PullUpBand
import com.powertracker.domain.library.figures.PullUpBar
import com.powertracker.domain.library.figures.PulldownLever
import com.powertracker.domain.library.figures.PulloverDumbbell
import com.powertracker.domain.library.figures.PulloverMachine
import com.powertracker.domain.library.figures.RackBar
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.RowMachine
import com.powertracker.domain.library.figures.RowStation
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Segment
import com.powertracker.domain.library.figures.Side
import com.powertracker.domain.library.figures.Skeleton
import com.powertracker.domain.library.figures.Spotter
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.TBarLever
import com.powertracker.domain.library.figures.TrapBar
import com.powertracker.domain.library.figures.UpperArmDown
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.figures.point
import com.powertracker.domain.library.figures.skeleton
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Figures for the library's Back area (library/areas/Back.kt), keyed by catalogue name.

/**
 * Pull-up. START hanging with straight arms; END pulled up until the chin clears the bar. Body upright and still, one
 * leg crossed over the other (knees bent) in both panels. No floor: the lifter hangs clear of it.
 * - View: side, so the chin over the bar shows; the wide grip is the guide's setup, not something this view can show.
 * - END: the bar about 19 cm in front of the shoulder joints, in front of the throat, with the elbows bent down and
 *   forward; the chin about 5.5 cm over the bar's top, plainly clear (ExRx pull-up,
 *   https://exrx.net/WeightExercises/LatissimusDorsi/BWPullup: pull until the chin is over the bar; ACE pull-ups,
 *   https://www.acefitness.org/resources/everyone/exercise-library/191/pull-ups/). The torso stays upright (90°) for
 *   the guide's "upright, without swinging".
 * - Legs crossed with the knees bent well back (neutral), which keeps the hanging figure short enough to read.
 * - The bar on a wall bracket (Kit.kt PullUpBar), so its height reads across the picture.
 */
private val pullUp: ExerciseFigure by lazy {
    val bar = Point(0.0, -200.0)
    val hangArm = Arm(180.0, 0.0)
    val near = Leg(hip = 25.0, knee = 110.0, ankle = -20.0)
    val far = Leg(hip = 20.0, knee = 120.0, ankle = -25.0)
    val hangShoulder = bar + Point(0.0, ARM_REACH)
    val hangPelvis = hangShoulder + Point(0.0, Segment.TORSO.length)
    val start = Pose(hangPelvis.x, hangPelvis.y, 90.0, nearArm = hangArm, nearLeg = near, farLeg = far)
    // The chin plainly over the bar: the shoulder joint 2 cm below the bar's centre puts the chin about 5.5 cm over its top.
    val topShoulder = bar + Point(-19.0, 2.0)
    val pull = Reach.arm(topShoulder, bar, 90.0)
    val topPelvis = topShoulder + Point(0.0, Segment.TORSO.length)
    val end = Pose(topPelvis.x, topPelvis.y, 90.0, nearArm = pull, nearLeg = near, farLeg = far)
    val props = listOf(PullUpBar(x = bar.x, height = -bar.y))
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Hang with straight arms",
        endCaption = "Chin clears the bar",
        thumbnail = Which.END,
        guideLines = listOf(
            "Hang with straight arms.",
            "Cross one leg over the other, and brace your abs.",
            "Pull yourself up until your chin clears the bar.",
            "Keep your body upright, without swinging.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH)),
        checks = listOf(ChinOverBar(at = Which.END)),
    )
}

/**
 * Lat pulldown. Seated with the thighs under the roller pad, feet flat. START the arms fully stretched up to the bar;
 * END the bar pulled down to the upper chest. The trunk leans back 10° in both panels: the guide's "only slightly"
 * (ExRx cable pulldown, https://exrx.net/WeightExercises/LatissimusDorsi/CBFrontPulldown; ACE seated lat pulldown,
 * https://www.acefitness.org/resources/everyone/exercise-library/158/seated-lat-pulldown/); 10° is a neutral "slightly".
 * - View: side, like the guide's sources; the wide grip is the guide's setup, not something this view can show.
 * - Seat 48 cm, roller on the thighs near the knees, the pulley over the bar's start: typical machine sizes, neutral.
 */
private val latPulldown: ExerciseFigure by lazy {
    val torso = 100.0
    val seatHeight = 48.0
    val pelvis = Point(0.0, -(seatHeight + Body.TORSO_RADIUS))
    val legs = Reach.leg(pelvis, Point(42.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val shoulder = shoulderOf(pelvis, torso)
    val up = straightArm(shoulder, shoulder + Point(0.0, -10.0), torso)
    val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = up, nearLeg = legs).skeleton()
    val chest = bare.point(BodyPoint.UPPER_CHEST) + unit(torso - 90.0) * 1.3
    val down = armTo(shoulder, chest, torso)
    // The roller sits on the thighs, 80% of the way to the knee.
    val thigh = bare.near.knee - bare.near.hip
    val pad = bare.near.hip + thigh * 0.8 + unit(angleTo(thigh) + 90.0) * (Body.THIGH_WIDTH / 2 + LatPulldownSeat.PAD_RADIUS)
    val seat = LatPulldownSeat(x = -6.0, padX = pad.x, seatHeight = seatHeight, padHeight = -pad.y)
    val grip = bare.near.grip
    val stack = CableStack(x = -60.0, reach = grip.x + 60.0)
    val props = listOf(Floor, stack, seat, LatBar)
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(Pose(pelvis.x, pelvis.y, torso, nearArm = up, nearLeg = legs), props),
        end = Scene(Pose(pelvis.x, pelvis.y, torso, nearArm = down, nearLeg = legs), props),
        startCaption = "Arms fully stretched",
        endCaption = "Bar to your upper chest",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your thighs under the pads.",
            "Lean back only slightly, using your legs to support your body.",
            "Pull the bar down to your upper chest.",
            "Return under control until your arms and shoulders are fully stretched.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.THIGHS, Surface.THIGH_PAD, BOTH),
            Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
        ),
        checks = listOf(BarTouches(BodyPoint.UPPER_CHEST, Which.END)),
    )
}

/**
 * One-arm DB row. The far knee and far hand on a flat bench, the near foot on the floor beside it and slightly back;
 * the near hand rows the dumbbell. START the arm hanging straight; END the dumbbell pulled up to the lower ribs: the
 * upper arm 25° behind the torso line, the elbow up past the back, the forearm hanging under it (the guide's "until it
 * touches your ribs").
 * - Torso 12° above level: the guide's "close to level". The bench hand is flat under the shoulder with the elbow
 *   slightly bent; with the thigh upright on the bench the figure's fixed lengths need that much rise.
 * - ExRx dumbbell bent-over row (https://exrx.net/WeightExercises/BackGeneral/DBBentOverRow) and ACE single-arm row
 *   (https://www.acefitness.org/resources/everyone/exercise-library/126/single-arm-row/): the same kneeling set-up.
 */
private val oneArmRow: ExerciseFigure by lazy {
    val torso = 12.0
    val bench = FlatBench(x = 0.0, nearLimbsBeside = true)
    // The knee 6.6 cm over the pad: the thigh's thicker round end (7 cm) rests on it, the knee within the contact tolerance.
    val knee = Point(36.0, -(FlatBench.HEIGHT + 6.6))
    val pelvis = knee + Point(0.0, -Segment.THIGH.length)
    val shoulder = shoulderOf(pelvis, torso)
    // Far knee on the bench: the thigh straight down, the shin back along the pad, the foot off the end.
    val kneel = Leg(hip = fold180(270.0 - torso - 180.0), knee = 90.0, ankle = -40.0)
    // The near foot slightly back of the hip: with the thigh upright on the bench the standing leg is close to straight.
    val standing = Reach.leg(pelvis, Point(pelvis.x - 4.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    // Far hand flat on the bench under the shoulder: the wrist bent back, the hand's underside on the pad.
    val wrist = -65.0
    val handArm = (0..200).map { i -> -FlatBench.HEIGHT - 2.0 - i * 0.05 }.map { y ->
        Reach.arm(shoulder, Point(shoulder.x + 8.0, y), torso, wrist)
    }.minBy { arm ->
        val s = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = standing, farArm = arm, farLeg = kneel).skeleton()
        val lowest = maxOf(s.far.wrist.y, s.far.hand.y) + Body.HAND_WIDTH / 2
        abs(lowest + FlatBench.HEIGHT)
    }
    val hang = Arm(shoulder = fold180(270.0 - torso - 180.0), elbow = 0.0)
    // The elbow drawn up and back past the torso, the forearm hanging straight down: the dumbbell at the lower ribs.
    val rowShoulder = -25.0
    val rowed = Arm(shoulder = rowShoulder, elbow = fold180(270.0 - (torso + 180.0 + rowShoulder)))
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = standing, farArm = handArm, farLeg = kneel)
    val props = listOf(Floor, bench, Dumbbell(Side.NEAR))
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(pose(hang), props),
        end = Scene(pose(rowed), props),
        startCaption = "Arm straight",
        endCaption = "Pull to your ribs",
        thumbnail = Which.END,
        guideLines = listOf(
            "Kneel with one knee on a bench, and put the hand on the same side on the bench, under your shoulder.",
            "Set your other foot on the floor, slightly back and out to the side.",
            "Keep your torso close to level with the floor. Move the knee or hand on the bench forward or back to level it.",
            "Pull the dumbbell up to your side, until it touches your ribs or your upper arm is just past level with your torso.",
            "Lower it under control until your arm is straight and your shoulder is stretched down.",
        ),
        contacts = listOf(
            Contact(BodyPoint.FAR_HAND, Surface.BENCH, BOTH),
            Contact(BodyPoint.FAR_KNEE, Surface.BENCH, BOTH),
            Contact(BodyPoint.NEAR_FOOT, Surface.FLOOR, BOTH),
        ),
        checks = emptyList(),
    )
}

// ==== Task 7: the rest of the Back area ====
//
// Camera: the side view for the rows, the pulldowns on a cable or lever, the pull-ups on a bar or machine, the
// pullovers, the trap-bar shrug and the back extensions, whose key positions (torso angle, where the handle ends, the
// arc's end points, the body line) are in the sagittal plane. The front view where the guide's point is the grip's
// width and the elbows at the sides (the chin-ups and the underhand and one-arm pulldowns) and for the barbell and
// dumbbell shrugs, whose shoulders rising read best from in front (the slope of the shoulders).
// Every position a guide does not fix is sourced in its comment, or kept neutral and marked so.

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

/** The arm along the torso, towards the hips: hanging straight down when the torso is upright. */
private val HANG = Arm(0.0, 0.0)

/** Bisection for the root of [f] between [lo] and [hi] (f changes sign between them). */
private fun solve(lo: Double, hi: Double, f: (Double) -> Double): Double {
    var a = lo
    var b = hi
    val fa = f(a)
    repeat(60) {
        val m = (a + b) / 2
        if ((f(m) > 0) == (fa > 0)) a = m else b = m
    }
    return (a + b) / 2
}

/** The point on the circle about [c] with radius [r] at [angle]°. */
private fun onCircle(c: Point, r: Double, angle: Double) = c + unit(angle) * r

/** Along the arc about [c] (radius [r]) from [from]° in steps of [step]° until [done] (a lever's handle on its way up). */
private fun alongArc(c: Point, r: Double, from: Double, step: Double, done: (Point) -> Boolean): Point {
    var a = from
    repeat(1200) {
        val p = onCircle(c, r, a)
        if (done(p)) return p
        a += step
    }
    error("arc never got there")
}

private fun distanceToSegment(p: Point, a: Point, b: Point): Double {
    val ab = b - a
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / (ab.x * ab.x + ab.y * ab.y)).coerceIn(0.0, 1.0)
    return (p - (a + ab * t)).length()
}

/** The pivot on the floor-level line y = [y] that is the same distance from [a] and [b] (a lever's two positions). */
private fun pivotOn(a: Point, b: Point, y: Double): Point {
    val m = (a + b) * 0.5
    val d = b - a
    // Points p with (p - m) · d = 0 and p.y = y.
    return Point(m.x - (y - m.y) * d.y / d.x, y)
}

/** The pivot at x = [x] the same distance from [a] and [b]. */
private fun pivotAtX(a: Point, b: Point, x: Double): Point {
    val m = (a + b) * 0.5
    val d = b - a
    return Point(x, m.y - (x - m.x) * d.x / d.y)
}

// ---- rows: bent over ----

/**
 * The bent-over row's set-up, shared with the T-bar row: the knees bent [ROW_KNEE]°, the shins upright, the torso
 * [ROW_TORSO]° above level. Neutral: the guide's "bend your knees slightly", and "close to level" (its strict option is
 * level; ExRx barbell bent-over row, https://exrx.net/WeightExercises/BackGeneral/BBBentOverRow, has the torso bent
 * over close to horizontal). With the body's fixed lengths a level torso would need a squat's knee bend to keep the
 * bar over the feet, so 20° is drawn, the arms hanging just back to the bar in front of the shins.
 */
private const val ROW_TORSO = 20.0
private const val ROW_KNEE = 25.0
private val rowLeg by lazy { standingLeg(0.0, shinLean = 0.0, knee = ROW_KNEE, torso = ROW_TORSO) }
private val rowShoulder by lazy { shoulderOf(rowLeg.hip, ROW_TORSO) }

/**
 * Row. START the arms straight, the bar hanging in front of the shins over the mid-foot; END pulled to the upper waist
 * (the upper abdomen), the bar's path straight up, the elbows drawn up past the back. Torso still and close to level.
 */
private val row: ExerciseFigure by lazy {
    val clear = Body.SHIN_WIDTH / 2 + Barbell.SHAFT_RADIUS + 1.5
    val start = standOn(rowLeg, ROW_TORSO, hangingArmAt(rowShoulder, rowLeg.knee.x + clear, ROW_TORSO))
    val bare = standOn(rowLeg, ROW_TORSO, HANG).skeleton()
    val waist = bare.point(BodyPoint.UPPER_ABDOMEN) + unit(ROW_TORSO - 90.0) * Barbell.SHAFT_RADIUS
    val end = standOn(rowLeg, ROW_TORSO, Reach.arm(rowShoulder, waist, ROW_TORSO))
    figure(
        View.SIDE, start, end, listOf(Floor, Barbell()),
        startCaption = "Arms straight",
        endCaption = "Pull to your upper waist",
        thumbnail = Which.END,
        guideLines = listOf(
            "Bend your knees slightly and bend over the bar with your back straight.",
            "Pull the bar up to your upper waist.",
            "Lower it until your arms are straight and your shoulders are stretched down.",
            "Keep your torso still. For strict reps, keep it level with the floor.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(BarTouches(BodyPoint.UPPER_ABDOMEN, Which.END), ElbowBend(0.0, 1.0, Which.START, Side.NEAR)),
    )
}

/**
 * T-bar row. The bent-over row's legs, the torso a little higher (25°, neutral: "torso stays down"); the lever pivots on
 * the floor behind the heels, between the legs, its plates past the handle. START the arms hanging straight; END the
 * handle pulled up on its arc until it touches the torso under the chest. ExRx lever T-bar row
 * (https://exrx.net/WeightExercises/BackGeneral/LVTBarRow).
 */
private val tBarRow: ExerciseFigure by lazy {
    val torso = 25.0
    val leg = standingLeg(0.0, shinLean = 0.0, knee = ROW_KNEE, torso = torso)
    val shoulder = shoulderOf(leg.hip, torso)
    val handle = shoulder + Point(0.0, ARM_REACH)
    val pivot = Point(handle.x - 150.0, -4.0)
    val r = (handle - pivot).length()
    val bare = standOn(leg, torso, HANG).skeleton()
    // Up the arc until the handle's crossbar (2.4 cm) touches the front of the torso.
    val top = alongArc(pivot, r, angleTo(handle - pivot), 0.1) { p ->
        distanceToSegment(p, bare.pelvis, bare.shoulder) <= Body.CHEST_DEPTH - 1.5 + 2.4
    }
    val start = standOn(leg, torso, straightArm(shoulder, handle, torso))
    val end = standOn(leg, torso, Reach.arm(shoulder, top, torso))
    val lever = TBarLever(pivot, plateAlong = r + 20.0)
    figure(
        View.SIDE, start, end, listOf(Floor, lever),
        startCaption = "Arms straight",
        endCaption = "Handles to your torso",
        thumbnail = Which.END,
        guideLines = listOf(
            "Bend your knees slightly and bend over the lever handles with your back straight.",
            "Pull the handles up to your torso.",
            "Keep your torso still and your back flat for every rep.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START, Side.NEAR)),
    )
}

// ---- rows: seated on a cable ----

/**
 * The seated cable row's body: on a 42 cm bench, the torso upright, the feet on the plates with the knees slightly
 * bent (about 25°; the guide's "slide your hips back until your knees are slightly bent"; neutral). The plates are
 * tilted 15° and the pulley a little above them (Kit.kt RowStation). ExRx cable seated row
 * (https://exrx.net/WeightExercises/BackGeneral/CBSeatedRow) and straight-back seated row
 * (https://exrx.net/WeightExercises/BackGeneral/CBStraightBackSeatedRow).
 */
private const val ROW_SEAT = 42.0
private val rowPelvis = Point(0.0, -(ROW_SEAT + Body.TORSO_RADIUS))
private val rowFootAngle = 75.0
private val rowLegs by lazy { Reach.leg(rowPelvis, rowPelvis + Point(80.0, 16.0), 90.0, foot = rowFootAngle) }
private val rowSeatShoulder = shoulderOf(rowPelvis, 90.0)

private fun rowPose(arm: Arm, far: Arm = arm, torso: Double = 90.0) =
    Pose(rowPelvis.x, rowPelvis.y, torso, nearArm = arm, nearLeg = rowLegs, farArm = far)

private val rowStation by lazy {
    val n = rowPose(HANG).skeleton().near
    // The plate's face under the sole: from the ankle, the sole's offset, and halfway along the foot.
    val sole = unit(rowFootAngle - 90.0) * Body.ANKLE_HEIGHT
    val face = n.ankle + sole + unit(rowFootAngle) * 7.0
    RowStation(seatX = rowPelvis.x + 14.0, seatHeight = ROW_SEAT, plate = face, pulley = Point(face.x + 22.0, face.y - 4.0))
}

/** The arms straight forward and a little down, towards the pulley: the stretch at the start of a seated row. */
private fun rowReach(z: Double = 0.0): Arm {
    val sh = V3(rowSeatShoulder.x, rowSeatShoulder.y, Body.SHOULDER_HALF_WIDTH)
    val dz = z - Body.SHOULDER_HALF_WIDTH
    val flat = sqrt(ARM_REACH * ARM_REACH - dz * dz)
    val grip = V3(sh.x + flat * cos(15.0 / DEG), sh.y + flat * sin(15.0 / DEG), z)
    return if (dz == 0.0) Arm(shoulder = fold180(-15.0 - 90.0 - 180.0), elbow = 0.0) else arm3(sh, grip, V3(0.0, 1.0, 0.0), 90.0)
}

private val SEATED_ROW_CONTACTS = listOf(Contact(BodyPoint.HIPS, Surface.SEAT, BOTH), Contact(BodyPoint.FEET, Surface.FOOT_PLATE, BOTH))

/**
 * Seated cable row. START arms straight towards the pulley, torso upright; END the V-handle pulled to the waist, the
 * elbows close to the ribs, the shoulders back. The V-handle's parallel grips are drawn in profile.
 */
private val seatedCableRow: ExerciseFigure by lazy {
    val bare = rowPose(HANG).skeleton()
    val waist = bare.point(BodyPoint.WAIST)
    val end = Reach.arm(rowSeatShoulder, waist + Point(1.0, 0.0), 90.0)
    figure(
        View.SIDE, rowPose(rowReach()), rowPose(end), listOf(Floor, rowStation, CableAttachment(Attachment.V_HANDLE)),
        startCaption = "Arms straight",
        endCaption = "Handle to your waist",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit slightly forward on the bench with your feet on the footplates. Grip the close-grip handle.",
            "Sit up straight, then slide your hips back until your knees are slightly bent.",
            "Pull the handle to your waist, keeping your elbows close to your ribs.",
            "Hold briefly, then return until your arms are straight. Keep your torso upright.",
        ),
        contacts = SEATED_ROW_CONTACTS,
        checks = listOf(HeldBeside(BodyPoint.WAIST, 2.0, Which.END), UpperArmDown(50.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Wide-grip cable row. The seated row's body with a straight bar gripped wide (the hands 36 cm out from the middle,
 * neutral "wider than your shoulders"); START arms straight; END the bar to the waist, the elbows out to the sides and
 * back (the arms drawn foreshortened, Arm.out). ExRx cable wide-grip seated row
 * (https://exrx.net/WeightExercises/BackGeneral/CBWideGripSeatedRow).
 */
private val wideGripCableRow: ExerciseFigure by lazy {
    val bare = rowPose(HANG).skeleton()
    val waist = bare.point(BodyPoint.WAIST)
    val sh = V3(rowSeatShoulder.x, rowSeatShoulder.y, Body.SHOULDER_HALF_WIDTH)
    val end = arm3(sh, V3(waist.x + 2.3, waist.y, 36.0), V3(-0.6, 0.2, 1.0), 90.0)
    figure(
        View.SIDE, rowPose(rowReach(36.0)), rowPose(end), listOf(Floor, rowStation, CableAttachment(Attachment.STRAIGHT_BAR)),
        startCaption = "Arms straight",
        endCaption = "Bar to your waist",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit slightly forward and grip the bar overhand, wider than your shoulders.",
            "Pull the bar to your waist.",
            "Return slowly until your arms are straight, with your back straight.",
        ),
        contacts = SEATED_ROW_CONTACTS,
        checks = listOf(BarTouches(BodyPoint.WAIST, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * One-arm cable row. The seated row's body, a single handle in the near hand; the far arm hangs at the side.
 * START the arm reaching forward, straight; END the handle pulled to the side of the waist, outside the torso (24 cm
 * out, neutral), the elbow back past the torso. ExRx cable one-arm seated row
 * (https://exrx.net/WeightExercises/BackGeneral/CBOneArmRow).
 */
private val oneArmCableRow: ExerciseFigure by lazy {
    val bare = rowPose(HANG).skeleton()
    val waist = bare.point(BodyPoint.WAIST)
    val sh = V3(rowSeatShoulder.x, rowSeatShoulder.y, Body.SHOULDER_HALF_WIDTH)
    val end = arm3(sh, V3(waist.x - 4.0, waist.y, 24.0), V3(-1.0, 0.3, 0.3), 90.0)
    val rest = HANG
    figure(
        View.SIDE, rowPose(rowReach(), rest), rowPose(end, rest), listOf(Floor, rowStation, CableHandle(Side.NEAR)),
        startCaption = "Let your arm reach",
        endCaption = "Handle to your side",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit slightly forward on the bench and grip a single handle with one hand.",
            "Slide your hips back, with your knees slightly bent. Let your arm reach forward.",
            "Pull the handle to your side.",
        ),
        contacts = SEATED_ROW_CONTACTS,
        checks = listOf(ElbowBend(0.0, 1.0, Which.START, Side.NEAR)),
    )
}

/**
 * Machine row. Seated upright on a 45 cm seat, the feet planted, the chest lightly on the upright pad; START the arms
 * straight forward to the handles about level with the shoulders; END pulled back until the elbows pass the torso.
 * The handles are 34 cm out from the middle (neutral; wide enough for the elbows to pass the torso) on a lever from a
 * pivot below, the same length in both panels.
 * ExRx lever seated row (https://exrx.net/WeightExercises/BackGeneral/LVSeatedRow).
 */
private val machineRow: ExerciseFigure by lazy {
    val seat = 45.0
    val pelvis = Point(0.0, -(seat + Body.TORSO_RADIUS))
    val legs = Reach.leg(pelvis, Point(pelvis.x + 40.0, -Body.ANKLE_HEIGHT), 90.0, foot = 0.0)
    val shoulder = shoulderOf(pelvis, 90.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, 90.0, nearArm = arm, nearLeg = legs)
    val bare = pose(HANG).skeleton()
    val chest = bare.point(BodyPoint.CHEST)
    val sh = V3(shoulder.x, shoulder.y, Body.SHOULDER_HALF_WIDTH)
    val dz = 34.0 - Body.SHOULDER_HALF_WIDTH
    val ahead = sqrt(ARM_REACH * ARM_REACH - dz * dz - 9.0)
    val gripStart = V3(sh.x + ahead, sh.y + 3.0, 34.0)
    val start = arm3(sh, gripStart, V3(0.0, 1.0, 0.0), 90.0)
    val gripEnd = V3(shoulder.x + 8.0, sh.y + 6.0, 34.0)
    val end = arm3(sh, gripEnd, V3(-1.0, 0.15, 0.2), 90.0)
    // The lever's pivot: on the two handle positions' bisector, low on the frame under the handles.
    val pivot = pivotOn(gripStart.flat, gripEnd.flat, -40.0)
    val machine = RowMachine(seatX = pelvis.x + 14.0, seatHeight = seat, padFace = chest.x, padHeight = -chest.y, pivot = pivot)
    figure(
        View.SIDE, pose(start), pose(end), listOf(Floor, machine),
        startCaption = "Arms straight",
        endCaption = "Elbows pass your torso",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so the handles are about level with your shoulders.",
            "Sit with your feet planted and your chest lightly touching the pad. Grip the handles.",
            "Pull the handles back until your elbows pass your torso.",
            "Return slowly until your arms are straight. Keep your torso upright.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
            Contact(BodyPoint.CHEST, Surface.CHEST_PAD, BOTH),
        ),
        checks = listOf(HandsLevelWith(BodyPoint.SHOULDERS, 4.0, Which.START), ElbowBend(0.0, 1.0, Which.START)),
    )
}

// ---- rows: lying face down ----

/**
 * The toe of a foot standing on the floor: the ankle's height that puts the foot capsule's toe end on the floor for a
 * foot at screen angle [foot] (pointing down and forward, on the toes).
 */
private fun ankleOnToes(ankleX: Double, foot: Double): Point {
    val r = Body.FOOT_WIDTH / 2
    val sole = unit(foot - 90.0) * (Body.ANKLE_HEIGHT - r)
    val toe = sole + unit(foot) * (Segment.FOOT.length * 0.8 - r)
    return Point(ankleX, -r - toe.y)
}

/** Hip to ankle for legs trailing behind with the knees bent 15° (a neutral "nearly straight"). */
private val TRAIL = sqrt(
    Segment.THIGH.length * Segment.THIGH.length + Segment.SHIN.length * Segment.SHIN.length +
        2 * Segment.THIGH.length * Segment.SHIN.length * cos(15.0 / DEG),
)

/**
 * Chest-supported row. Face down on an incline bench at 30° (neutral "slight incline"; ExRx dumbbell incline row,
 * https://exrx.net/WeightExercises/BackGeneral/DBInclineRow: a gently sloped bench), the chest on the pad, the feet
 * behind on the toes; START the arms hanging straight, the dumbbells clear of the floor; END pulled up to the sides,
 * the elbows up past the back (the upper arm 30° behind the torso's line), the forearms hanging under them. The
 * guide gives no grip, so the dumbbells keep the usual end-on drawing.
 */
private val chestSupportedRow: ExerciseFigure by lazy {
    val angle = 30.0
    val low = Point(0.0, -62.0)
    val bench = ProneBench(low = low, angle = angle, length = 65.0)
    // The torso along the pad, the chest touching its top 36 cm up the pad (the hips at its low end).
    val touch = low + unit(angle) * 36.0
    val chestAlong = Segment.TORSO.length - Body.CHEST_BELOW_SHOULDER
    val pelvis = touch - unit(angle) * chestAlong + unit(angle + 90.0) * Body.CHEST_DEPTH
    val shoulder = shoulderOf(pelvis, angle)
    // The legs trail nearly straight behind (the knees bent 15°, neutral), on the toes.
    val ankle = ankleOnToes(0.0, foot = -55.0).let { a ->
        val dy = a.y - pelvis.y
        Point(pelvis.x - sqrt(TRAIL * TRAIL - dy * dy), a.y)
    }
    val legs = Reach.leg(pelvis, ankle, angle, foot = -55.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, angle, nearArm = arm, nearLeg = legs)
    val down = straightArm(shoulder, shoulder + Point(0.0, 10.0), angle)
    val rowShoulder = -30.0
    val up = Arm(shoulder = rowShoulder, elbow = fold180(270.0 - (angle + 180.0 + rowShoulder)))
    figure(
        View.SIDE, pose(down), pose(up), listOf(Floor, bench, Dumbbell(Side.FAR), Dumbbell(Side.NEAR)),
        startCaption = "Arms hanging down",
        endCaption = "Pull to your sides",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the bench to a slight incline. A flatter bench gives a better line of pull, if it is high enough.",
            "Lie face down on the bench with a dumbbell in each hand, arms hanging down.",
            "Pull the dumbbells up to your sides.",
            "Lower until your arms are straight and your shoulders are stretched down.",
        ),
        contacts = listOf(Contact(BodyPoint.CHEST, Surface.CHEST_PAD, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Seal row. Chest down on a high flat bench (95 cm, neutral: high enough that the plates on the cambered bar clear the
 * floor at full stretch), the legs along it; START the arms hanging straight, the bar's plates hanging 12 cm lower on
 * the camber; END the hands pulled up beside the bench to the upper waist. ExRx cambered bar lying row
 * (https://exrx.net/WeightExercises/BackGeneral/CamberedBarLyingRow).
 */
private val sealRow: ExerciseFigure by lazy {
    val height = 95.0
    val bench = FlatBench(x = -95.0, height = height, nearLimbsBeside = true)
    val pelvis = Point(-12.0, -(height + Body.CHEST_DEPTH))
    val shoulder = shoulderOf(pelvis, 0.0)
    val legs = Leg(hip = 0.0, knee = 0.0, ankle = -45.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, 0.0, nearArm = arm, nearLeg = legs)
    val bare = pose(HANG).skeleton()
    val waist = bare.point(BodyPoint.UPPER_ABDOMEN)
    val down = straightArm(shoulder, shoulder + Point(0.0, 10.0), 0.0)
    val up = Reach.arm(shoulder, Point(waist.x, -height + 2.0), 0.0)
    figure(
        View.SIDE, pose(down), pose(up), listOf(Floor, bench, CamberedBar()),
        startCaption = "Full stretch",
        endCaption = "Pull to your upper waist",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie chest down on a high, flat bench.",
            "Grip a cambered bar below the bench with an overhand grip, or hold a dumbbell in each hand.",
            "Pull the bar to your upper waist or lower chest. With dumbbells, pull them up to your sides.",
            "Lower until your arms are straight and your shoulders are stretched down.",
        ),
        contacts = listOf(Contact(BodyPoint.CHEST, Surface.BENCH, BOTH)),
        checks = listOf(HeldOver(BodyPoint.UPPER_ABDOMEN, 3.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Chest-supported T-bar row. Chest down on the machine's pad at 40° (neutral, a typical plate-loaded incline row), the
 * feet on its platform; a T-bar lever under the pad, pivoting on the floor behind the platform, its plates past the
 * handles under the head. START
 * the arms hanging straight; END the handles pulled up on the lever's arc as high as the arms go, the elbows past the
 * back. ExRx lever incline row (https://exrx.net/WeightExercises/BackGeneral/LVInclineRow).
 */
private val chestSupportedTBarRow: ExerciseFigure by lazy {
    val angle = 40.0
    val low = Point(0.0, -70.0)
    val touch = low + unit(angle) * 33.0
    val chestAlong = Segment.TORSO.length - Body.CHEST_BELOW_SHOULDER
    val pelvis = touch - unit(angle) * chestAlong + unit(angle + 90.0) * Body.CHEST_DEPTH
    val shoulder = shoulderOf(pelvis, angle)
    // The feet on the platform, on the balls of the feet: the toes' ends on its top.
    // The legs trail nearly straight behind (the knees bent 15°, neutral), down to the platform on the balls of the feet.
    val ankle = pelvis + unit(225.0) * TRAIL
    val legs = Reach.leg(pelvis, ankle, angle, foot = -40.0)
    val toe = Pose(pelvis.x, pelvis.y, angle, nearArm = HANG, nearLeg = legs).skeleton().near.toe
    val platform = Point(toe.x - 8.0, toe.y + Body.FOOT_WIDTH / 2)
    val bench = ProneBench(low = low, angle = angle, length = 65.0, footPlate = platform)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, angle, nearArm = arm, nearLeg = legs)
    val handle = shoulder + Point(0.0, ARM_REACH)
    val pivot = Point(handle.x - 150.0, -4.0)
    val r = (handle - pivot).length()
    // Up the arc until the handle is 32 cm from the shoulder (the elbow bent about 100°).
    val top = alongArc(pivot, r, angleTo(handle - pivot), 0.1) { p -> (p - shoulder).length() <= 32.0 }
    val down = straightArm(shoulder, handle, angle)
    val up = Reach.arm(shoulder, top, angle)
    figure(
        View.SIDE, pose(down), pose(up), listOf(Floor, bench, TBarLever(pivot, plateAlong = r + 18.0)),
        startCaption = "Arms straight",
        endCaption = "Handles up high",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie with your chest on the incline pad and grip the handles overhand.",
            "Pull the handles up as high as you can.",
            "Lower until your arms are straight and your shoulders are stretched down.",
        ),
        contacts = listOf(Contact(BodyPoint.CHEST, Surface.CHEST_PAD, BOTH), Contact(BodyPoint.FEET, Surface.FOOT_PLATE, BOTH)),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Inverted row. Lying under a bar in a rack, the heels on the floor, the body straight and rigid; START hanging from
 * straight arms, the bar set just high enough for that (the back just off the floor); END the body pulled up,
 * pivoting on the heels, until the chest (its upper part) touches the bar. ExRx inverted (supine) row
 * (https://exrx.net/WeightExercises/BackGeneral/BWSupineRow).
 * - With the hands and heels fixed and the body rigid, the shoulders swing towards the head as the body lowers, so at
 *   the full hang the arms lean towards the feet. The end angle (neutral) is the one, of those that leave the full hang
 *   at least 3° above level, that keeps that lean smallest: about 28° from vertical. Below about 25° no rigid body on
 *   fixed heels can both hang at full stretch and bring its chest to the same bar.
 */
private val invertedRow: ExerciseFigure by lazy {
    val legs = Leg(hip = 0.0, knee = 0.0, ankle = 0.0)
    val heelX = 90.0
    // The body straight at [up]° above level, head to the left, the heels on the floor at heelX.
    fun body(up: Double, arm: Arm = HANG): Pose {
        val torso = 180.0 - up
        val probe = Pose(0.0, 0.0, torso, nearArm = arm, nearLeg = legs).skeleton().near
        val r = Body.FOOT_WIDTH / 2
        val lowest = if (probe.heel.y > probe.toe.y) probe.heel else probe.toe
        return Pose(heelX - lowest.x, -r - lowest.y, torso, nearArm = arm, nearLeg = legs)
    }
    fun barAt(up: Double) = body(up).skeleton().let { it.point(BodyPoint.UPPER_CHEST) + unit(it.torso - 90.0) * PullUpBar.RADIUS }
    fun reachGap(up: Double, bar: Point) = (body(up).skeleton().shoulder - bar).length() - ARM_REACH
    // The full hang keeps the back off the floor: at least 3° above level.
    val lowest = 3.0
    val (topUp, startUp) = (40..90).map { it * 0.5 }.mapNotNull { top ->
        val bar = barAt(top)
        if (reachGap(lowest, bar) < 0.0) null else top to solve(lowest, top) { up -> reachGap(up, bar) }
    }.minBy { (top, up) ->
        val sh = body(up).skeleton().shoulder
        abs(barAt(top).x - sh.x)
    }
    val bar = barAt(topUp)
    val topBare = body(topUp).skeleton()
    val sb = body(startUp).skeleton()
    val start = body(startUp, straightArm(sb.shoulder, bar, sb.torso))
    val end = body(topUp, armTo(topBare.shoulder, bar, topBare.torso))
    figure(
        View.SIDE, start, end, listOf(Floor, RackBar(bar.x, -bar.y)),
        startCaption = "Arms straight",
        endCaption = "Chest to the bar",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a fixed bar just high enough that your arms can fully straighten when you hang under it.",
            "Brace your trunk so your body stays rigid and straight, with your heels on the floor.",
            "Pull your chest up to the bar.",
            "Lower until your arms are straight and your shoulders are stretched forward.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START)),
    )
}

// ---- pulldowns ----

/** The approved Lat pulldown's set-up (Back.kt), rebuilt: the same seat, pad, legs and stretched arms. */
private class PulldownBase {
    val torso = 100.0
    val seatHeight = 48.0
    val pelvis = Point(0.0, -(seatHeight + Body.TORSO_RADIUS))
    val legs = Reach.leg(pelvis, Point(42.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val shoulder = shoulderOf(pelvis, torso)
    val up = straightArm(shoulder, shoulder + Point(0.0, -10.0), torso)
    val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = up, nearLeg = legs).skeleton()
    val seat: LatPulldownSeat = run {
        val thigh = bare.near.knee - bare.near.hip
        val pad = bare.near.hip + thigh * 0.8 + unit(angleTo(thigh) + 90.0) * (Body.THIGH_WIDTH / 2 + LatPulldownSeat.PAD_RADIUS)
        LatPulldownSeat(x = -6.0, padX = pad.x, seatHeight = seatHeight, padHeight = -pad.y)
    }
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = legs)
}

private val pulldownBase by lazy { PulldownBase() }

private val PULLDOWN_CONTACTS = listOf(
    Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
    Contact(BodyPoint.THIGHS, Surface.THIGH_PAD, BOTH),
    Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
)

/**
 * Close-grip pulldown. The lat pulldown's seat and lean, a V-handle (parallel grips, drawn in profile); START the arms
 * fully stretched up; END the handle down at the upper chest, its grips just in front of it. ExRx cable close-grip
 * pulldown (https://exrx.net/WeightExercises/LatissimusDorsi/CBCloseGripPulldown).
 */
private val closeGripPulldown: ExerciseFigure by lazy {
    val b = pulldownBase
    val chest = b.bare.point(BodyPoint.UPPER_CHEST) + unit(b.torso - 90.0) * (CableAttachment.HANDLE / 2 + 1.0)
    val down = armTo(b.shoulder, chest, b.torso)
    val grip = b.bare.near.grip
    figure(
        View.SIDE, b.pose(b.up), b.pose(down), listOf(Floor, CableStack(x = -60.0, reach = grip.x + 60.0), b.seat, CableAttachment(Attachment.V_HANDLE)),
        startCaption = "Arms fully stretched",
        endCaption = "Handle to upper chest",
        thumbnail = Which.END,
        guideLines = listOf(
            "Grip the parallel handle, palms facing each other.",
            "Sit with your thighs under the pads.",
            "Pull the handle down to your upper chest.",
            "Return under control until your arms and shoulders are fully stretched.",
        ),
        contacts = PULLDOWN_CONTACTS,
        checks = listOf(HeldBeside(BodyPoint.UPPER_CHEST, 8.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Machine pulldown. The lat pulldown's seat and lean; the handle on a plate-loaded lever pivoting on a frame behind
 * the seat (Kit.kt PulldownLever), the same length in both panels; START the arms fully stretched up; END the handle
 * down level with the upper chest. ExRx lever front pulldown
 * (https://exrx.net/WeightExercises/LatissimusDorsi/LVFrontPulldown).
 */
private val machinePulldown: ExerciseFigure by lazy {
    val b = pulldownBase
    val chest = b.bare.point(BodyPoint.UPPER_CHEST) + unit(b.torso - 90.0) * 4.0
    val down = armTo(b.shoulder, chest, b.torso)
    val top = b.bare.near.grip
    val low = b.pose(down).skeleton().near.grip
    val pivot = pivotAtX(top, low, b.pelvis.x - 40.0)
    val props = listOf(Floor, b.seat, PulldownLever(pivot))
    figure(
        View.SIDE, b.pose(b.up), b.pose(down), props,
        startCaption = "Arms fully stretched",
        endCaption = "Lever to upper chest",
        thumbnail = Which.END,
        guideLines = listOf(
            "Grip the lever handles.",
            "Sit with your thighs under the pads.",
            "Pull the lever down to your upper chest.",
            "Return under control until your arms and shoulders are fully stretched.",
        ),
        contacts = PULLDOWN_CONTACTS,
        checks = listOf(HandsLevelWith(BodyPoint.UPPER_CHEST, 3.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Straight-arm pulldown. Seated on a bench facing away from a high pulley behind, a straight bar in the hands; the
 * elbows' slight bend (15°, neutral) fixed in both panels; START the bar overhead (the arms 160° up from hanging,
 * neutral); END pulled forward and down until the bar is just above the thighs (as far as it goes). ExRx cable seated
 * straight-arm pullover (https://exrx.net/WeightExercises/LatissimusDorsi/CBSeatedStraightArmPullover).
 */
private val straightArmPulldown: ExerciseFigure by lazy {
    val seat = FlatBench.HEIGHT
    val pelvis = Point(0.0, -(seat + Body.TORSO_RADIUS))
    val legs = Reach.leg(pelvis, Point(pelvis.x + 44.0, -Body.ANKLE_HEIGHT), 90.0, foot = 0.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, 90.0, nearArm = arm, nearLeg = legs)
    val bend = 15.0
    val overhead = Arm(shoulder = 160.0, elbow = bend)
    val thighTop = pose(HANG).skeleton().near.let { (it.hip + it.knee) * 0.5 }.y - Body.THIGH_WIDTH / 2
    val downAngle = solve(20.0, 90.0) { a -> -(pose(Arm(a, bend)).skeleton().near.grip.y - (thighTop - 6.0)) }
    val down = Arm(shoulder = downAngle, elbow = bend)
    val stack = CableStack(x = pelvis.x - 85.0, pulleyHeight = 205.0, reach = 35.0)
    figure(
        View.SIDE, pose(overhead), pose(down), listOf(Floor, stack, FlatBench(x = pelvis.x - 60.0, length = 70.0), CableAttachment(Attachment.STRAIGHT_BAR)),
        startCaption = "Grip it overhead",
        endCaption = "Forward and down",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on the bench facing away from the high pulley.",
            "Keep a slight bend in your elbows, and keep it fixed.",
            "Pull the bar forward and down as far as you can.",
        ),
        contacts = listOf(Contact(BodyPoint.HIPS, Surface.BENCH, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(10.0, 20.0, Which.START), ElbowBend(10.0, 20.0, Which.END)),
    )
}

// ---- pulldowns from the front ----

/** Seated, front view: the thighs pointing at the camera (80° out of the plane), the shins upright, feet flat. */
private val FRONT_SEATED_LEG = Leg(hip = 10.0, knee = 10.0, out = 80.0)
private val FRONT_SEATED_Y by lazy { -(Body.ANKLE_HEIGHT + Segment.THIGH.length * cos(80.0 / DEG) * cos(10.0 / DEG) + Segment.SHIN.length) }
private val frontShoulderY by lazy { FRONT_SEATED_Y - Segment.TORSO.length }

/** The front pulldown station: the seat under the hips, the roller across the tops of the thighs, the column behind. */
private val frontPulldownSeat by lazy {
    val seatTop = -(FRONT_SEATED_Y + Body.TORSO_RADIUS)
    val thighTop = -FRONT_SEATED_Y + Body.THIGH_WIDTH / 2
    LatPulldownSeat(x = 0.0, padX = 0.0, seatHeight = seatTop, padHeight = thighTop + LatPulldownSeat.PAD_RADIUS)
}

private fun frontSeated(near: Arm, far: Arm = near) = Pose(0.0, FRONT_SEATED_Y, 90.0, nearArm = near, nearLeg = FRONT_SEATED_LEG, farArm = far)

/** The front view's near shoulder joint (3D), for a lifter centred at x = 0 with the shoulder joints at height [y]. */
private fun frontShoulder(y: Double) = V3(Body.SHOULDER_HALF_WIDTH, y, 0.0)

/**
 * A straight arm overhead in the front view: the grip [half] cm right of the middle, [forward] cm in front of the
 * shoulder, at arm's length (the hands on a bar or a cable overhead).
 */
private fun overhead(shoulderY: Double, half: Double, forward: Double): Pair<Arm, V3> {
    val sh = frontShoulder(shoulderY)
    val dx = half - sh.x
    val up = sqrt(ARM_REACH * ARM_REACH - dx * dx - forward * forward)
    val grip = V3(half, shoulderY - up, forward)
    return arm3(sh, grip, V3(1.0, 0.0, 0.0), 90.0) to grip
}

private val FRONT_PULLDOWN_CONTACTS = listOf(
    Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
    Contact(BodyPoint.THIGHS, Surface.THIGH_PAD, BOTH),
    Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
)

/** Underhand pulldown and chin-up grip: about shoulder width, the hands 25 cm out from the middle (neutral). */
private const val SHOULDER_GRIP = 25.0

/**
 * Underhand pulldown. Front view, so the shoulder-width underhand grip and the elbows' path show: seated with the
 * thighs under the roller; START the arms fully stretched up to the bar; END the bar down at the upper chest, in front
 * of it, the elbows down at the sides. ExRx cable underhand pulldown
 * (https://exrx.net/WeightExercises/LatissimusDorsi/CBUnderhandPulldown).
 */
private val underhandPulldown: ExerciseFigure by lazy {
    val (up, _) = overhead(frontShoulderY, SHOULDER_GRIP, 8.0)
    val upperChest = frontShoulderY + Body.UPPER_CHEST_BELOW_SHOULDER
    val down = arm3(frontShoulder(frontShoulderY), V3(SHOULDER_GRIP, upperChest, Body.UPPER_CHEST_DEPTH + 3.0), V3(0.3, 1.0, 0.2), 90.0)
    val props = listOf(Floor, CableStack(x = 0.0, pulleyHeight = 215.0, reach = 0.0), frontPulldownSeat, LatBar)
    figure(
        View.FRONT, frontSeated(up), frontSeated(down), props,
        startCaption = "Grip the bar underhand",
        endCaption = "Elbows to your sides",
        thumbnail = Which.END,
        guideLines = listOf(
            "Grip the bar underhand.",
            "Sit with your thighs under the pads.",
            "Pull the bar down to your upper chest, until your elbows are at your sides.",
            "Return under control until your arms and shoulders are fully stretched.",
        ),
        contacts = FRONT_PULLDOWN_CONTACTS,
        checks = listOf(HandsLevelWith(BodyPoint.UPPER_CHEST, 2.0, Which.END), UpperArmDown(25.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * One-arm pulldown. Front view: one handle on a cable from the pulley above the near shoulder; the far hand rests on
 * the roller. START the near arm fully straight up; END the handle down at the side of the chest, the elbow at the
 * side. ExRx cable one-arm pulldown (https://exrx.net/WeightExercises/LatissimusDorsi/CBOneArmPulldown).
 */
private val oneArmPulldown: ExerciseFigure by lazy {
    val (up, grip) = overhead(frontShoulderY, 24.0, 8.0)
    val chestY = frontShoulderY + Body.CHEST_BELOW_SHOULDER
    val down = arm3(frontShoulder(frontShoulderY), V3(24.0, chestY, 12.0), V3(0.3, 1.0, 0.1), 90.0)
    // The far hand on the roller: the arm hanging forward and down to it.
    val padTop = -frontPulldownSeat.padHeight - LatPulldownSeat.PAD_RADIUS - Body.HAND_WIDTH / 2
    val rest = arm3(frontShoulder(frontShoulderY), V3(16.0, padTop, 22.0), V3(0.5, 0.0, -1.0), 90.0)
    val props = listOf(Floor, CableStack(x = 0.0, pulleyHeight = 215.0, reach = grip.x), frontPulldownSeat, CableHandle(Side.NEAR))
    figure(
        View.FRONT, frontSeated(up, rest), frontSeated(down, rest), props,
        startCaption = "Arm fully straight",
        endCaption = "Side of your chest",
        thumbnail = Which.START,
        guideLines = listOf(
            "Grip a single handle in one hand.",
            "Sit with your thighs under the pads.",
            "Pull the handle down to the side of your chest.",
            "Return until your arm is fully straight, letting your shoulder rise.",
        ),
        contacts = FRONT_PULLDOWN_CONTACTS,
        checks = listOf(HandsLevelWith(BodyPoint.CHEST, 2.0, Which.END), UpperArmDown(25.0, Which.END), ElbowBend(0.0, 1.0, Which.START, Side.NEAR)),
    )
}

// ---- pull-ups and chin-ups ----

/** The approved Pull-up's poses (Back.kt), rebuilt: the bar, the crossed legs, the hang and the chin over the bar. */
private val PULL_BAR = Point(0.0, -200.0)
private val PULL_NEAR_LEG = Leg(hip = 25.0, knee = 110.0, ankle = -20.0)
private val PULL_FAR_LEG = Leg(hip = 20.0, knee = 120.0, ankle = -25.0)

/** A hanging pose: the shoulder joint at [shoulder], the torso upright, the near arm [arm]. */
private fun hangAt(shoulder: Point, arm: Arm, near: Leg = PULL_NEAR_LEG, far: Leg = PULL_FAR_LEG): Pose {
    val pelvis = shoulder + Point(0.0, Segment.TORSO.length)
    return Pose(pelvis.x, pelvis.y, 90.0, nearArm = arm, nearLeg = near, farLeg = far)
}

private val pullHang by lazy { hangAt(PULL_BAR + Point(0.0, ARM_REACH), Arm(180.0, 0.0)) }
private val pullTop by lazy { (PULL_BAR + Point(-19.0, 2.0)).let { hangAt(it, Reach.arm(it, PULL_BAR, 90.0)) } }

/**
 * Weighted pull-up. The approved Pull-up's poses, with a dip belt and a plate hanging on its chain in front of the
 * thighs (Kit.kt DipBelt); START hanging with straight arms; END the chin clear of the bar. ExRx weighted pull-up
 * (https://exrx.net/WeightExercises/LatissimusDorsi/WtPullup).
 */
private val weightedPullUp: ExerciseFigure by lazy {
    val props = listOf(PullUpBar(x = PULL_BAR.x, height = -PULL_BAR.y), DipBelt(drop = 60.0))
    figure(
        View.SIDE, pullHang, pullTop, props,
        startCaption = "Hang with straight arms",
        endCaption = "Chin clears the bar",
        thumbnail = Which.END,
        guideLines = listOf(
            "Put on a dip belt with the weight hanging from it.",
            "Hang with straight arms.",
            "Pull yourself up until your chin clears the bar.",
            "Keep your body upright, without swinging.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH)),
        checks = listOf(ChinOverBar(at = Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Neutral-grip pull-up. Parallel handles seen in profile (Kit.kt ParallelHandles), the Pull-up's crossed legs; START
 * hanging with straight arms; END pulled up until the neck is at the hands' height (the handles 17 cm in front of the
 * shoulder joints, so the elbow bends under its limit). ExRx neutral-grip pull-up
 * (https://exrx.net/WeightExercises/LatissimusDorsi/BWPullupN).
 */
private val neutralGripPullUp: ExerciseFigure by lazy {
    val handle = Point(0.0, -200.0)
    val start = hangAt(handle + Point(0.0, ARM_REACH), Arm(180.0, 0.0))
    val neckAbove = (Segment.NECK.length / 2) * 1.0
    val topShoulder = handle + Point(-17.0, neckAbove)
    val end = hangAt(topShoulder, armTo(topShoulder, handle, 90.0))
    figure(
        View.SIDE, start, end, listOf(ParallelHandles(handle.x, -handle.y)),
        startCaption = "Palms face each other",
        endCaption = "Neck at hand height",
        thumbnail = Which.END,
        guideLines = listOf(
            "Step up and grip the handles with your palms facing each other.",
            "Hang with straight arms.",
            "Pull yourself up until your neck reaches the height of your hands.",
            "Keep your body upright, without swinging.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH)),
        checks = listOf(HandsLevelWith(BodyPoint.NECK, 2.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Band-assisted pull-up. A loop band over the bar, under the near foot (drawn in front of the body, Kit.kt
 * PullUpBand); the banded leg straight, the free foot resting on top of the banded foot. START hanging with straight
 * arms; END the chin above the bar (the approved Pull-up's top). ExRx band-assisted pull-up
 * (https://exrx.net/WeightExercises/LatissimusDorsi/ASPullupBand).
 */
private val bandAssistedPullUp: ExerciseFigure by lazy {
    val banded = Leg(hip = 0.0, knee = 0.0, ankle = -30.0)
    fun pose(shoulder: Point, arm: Arm): Pose {
        val base = hangAt(shoulder, arm, banded, banded)
        val n = base.skeleton().near
        // The free foot's sole on top of the banded foot: its ankle just above and a little behind the banded one.
        val ankle = n.ankle + Point(-3.0, -(Body.FOOT_WIDTH + 1.0))
        val free = Reach.leg(Point(base.pelvisX, base.pelvisY), ankle, 90.0, foot = angleTo(n.toe - n.heel))
        return base.copy(farLeg = free)
    }
    val hang = PULL_BAR + Point(0.0, ARM_REACH)
    val top = PULL_BAR + Point(-19.0, 2.0)
    val props = listOf(PullUpBar(x = PULL_BAR.x, height = -PULL_BAR.y), PullUpBand(PULL_BAR))
    figure(
        View.SIDE, pose(hang, Arm(180.0, 0.0)), pose(top, Reach.arm(top, PULL_BAR, 90.0)), props,
        startCaption = "Banded leg straight",
        endCaption = "Chin above the bar",
        thumbnail = Which.END,
        guideLines = listOf(
            "Straighten that leg to stretch the band further.",
            "Put your free foot on top of the banded foot, so the band can't slip off.",
            "Pull yourself up until your chin is above the bar.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH)),
        checks = listOf(ChinOverBar(at = Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/** Kneeling on the assist pad: the thighs straight down, the shins back along the pad, the feet off its end. */
private val KNEEL = Leg(hip = 0.0, knee = 90.0, ankle = -40.0)

/**
 * An assisted pull-up machine's two poses: the bar at [bar]; START the arms straight; END [top]'s shoulder and arm.
 * The knee pad rides up with the knees, its lever's pivot on the column keeping it the same length.
 */
private fun assisted(top: Point, topArm: Arm): Triple<Pose, Pose, Pair<AssistPullUpMachine, AssistPullUpMachine>> {
    val bar = Point(0.0, -215.0)
    val start = hangAt(bar + Point(0.0, ARM_REACH), Arm(180.0, 0.0), KNEEL, KNEEL)
    val end = hangAt(top, topArm, KNEEL, KNEEL)
    fun pad(p: Pose) = p.skeleton().near.let { Point(it.knee.x - 12.0, it.knee.y + Body.THIGH_WIDTH / 2 - 0.4) }
    val p0 = pad(start)
    val p1 = pad(end)
    val column = bar.x + 50.0
    val pivot = pivotAtX(p0, p1, column - 9.0)
    fun machine(p: Point) = AssistPullUpMachine(x = column, barX = bar.x, barHeight = -bar.y, padX = p.x, padHeight = -p.y, pivot = pivot)
    return Triple(start, end, machine(p0) to machine(p1))
}

private val ASSISTED_CONTACTS = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH), Contact(BodyPoint.SHINS, Surface.ASSIST_STEP, BOTH))

/**
 * Assisted pull-up. Kneeling on the machine's pad (ExRx machine-assisted pull-up, kneeling,
 * https://exrx.net/WeightExercises/LatissimusDorsi/AsPullupKneeling); START hanging with straight arms; END pulled up
 * until the neck is at the hands' height, the bar 17 cm in front of the shoulder joints. Body upright.
 */
private val assistedPullUp: ExerciseFigure by lazy {
    val bar = Point(0.0, -215.0)
    val topShoulder = bar + Point(-17.0, Segment.NECK.length / 2)
    val (start, end, machines) = assisted(topShoulder, armTo(topShoulder, bar, 90.0))
    figure(
        View.SIDE, start, end, listOf(Floor, machines.first), endProps = listOf(Floor, machines.second),
        startCaption = "Arms straight",
        endCaption = "Neck at hand height",
        thumbnail = Which.END,
        guideLines = listOf(
            "Kneel on the pad and lower yourself until your arms are straight. Some machines have you stand on the platform instead.",
            "Pull yourself up until your neck reaches the height of your hands.",
            "Keep your body upright.",
        ),
        contacts = ASSISTED_CONTACTS,
        checks = listOf(HandsLevelWith(BodyPoint.NECK, 2.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Assisted chin-up. The assisted pull-up's machine and kneel; END pulled up until the elbows are at the sides: the
 * upper arm hanging down the torso's line (8° forward, neutral), the elbow folded 145°, so the bar is at the upper
 * chest, in front of it. ExRx machine-assisted chin-up (https://exrx.net/WeightExercises/LatissimusDorsi/AsUnderhandChinup).
 */
private val assistedChinUp: ExerciseFigure by lazy {
    val bar = Point(0.0, -215.0)
    val arm = Arm(shoulder = 8.0, elbow = 145.0)
    val offset = Pose(0.0, Segment.TORSO.length, 90.0, nearArm = arm, nearLeg = KNEEL).skeleton().let { it.near.grip - it.shoulder }
    val topShoulder = bar - offset
    val (start, end, machines) = assisted(topShoulder, arm)
    figure(
        View.SIDE, start, end, listOf(Floor, machines.first), endProps = listOf(Floor, machines.second),
        startCaption = "Grip shoulder-width",
        endCaption = "Elbows to your sides",
        thumbnail = Which.END,
        guideLines = listOf(
            "Step up and grip the bar underhand, shoulder-width apart.",
            "Kneel on the pad, or stand on it if your machine is built that way.",
            "Pull yourself up until your elbows are at your sides.",
            "Keep your body upright.",
        ),
        contacts = ASSISTED_CONTACTS,
        checks = listOf(UpperArmDown(12.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

// ---- chin-ups from the front ----

/**
 * The front view's hang under a bar: the knees bent back (the shins pointing 60° away from the camera, drawn short)
 * and the legs drawn together, crossed at the ankles (the guide's "cross one leg over the other"; seen from in front
 * the crossing is at the feet).
 */
private val FRONT_HANG_LEG = Leg(hip = -4.0, knee = 0.0, shinOut = -60.0)

/** The bar for the front-view chin-ups. */
private val CHIN_BAR = Point(0.0, -210.0)

/**
 * The chin-up's two front-view poses: START the arms straight up to the bar, the hands [SHOULDER_GRIP] out, the body
 * hanging 10 cm behind the bar; END the elbows down at the sides, the bar 10 cm above the shoulder joints (about chin
 * height) and 14 cm in front of them.
 */
private fun chinUpPoses(leg: Leg): Pair<Pose, Pose> {
    val forward = 10.0
    val dx = SHOULDER_GRIP - Body.SHOULDER_HALF_WIDTH
    val up = sqrt(ARM_REACH * ARM_REACH - dx * dx - forward * forward)
    val hangY = CHIN_BAR.y + up
    val hangArm = arm3(frontShoulder(hangY), V3(SHOULDER_GRIP, CHIN_BAR.y, forward), V3(1.0, 0.0, 0.0), 90.0)
    val topY = CHIN_BAR.y + 10.0
    val topArm = arm3(frontShoulder(topY), V3(SHOULDER_GRIP, CHIN_BAR.y, 14.0), V3(0.0, 1.0, 0.6), 90.0)
    fun pose(shoulderY: Double, arm: Arm) = Pose(0.0, shoulderY + Segment.TORSO.length, 90.0, nearArm = arm, nearLeg = leg)
    return pose(hangY, hangArm) to pose(topY, topArm)
}

/**
 * Chin-up. Front view, so the shoulder-width underhand grip and the elbows coming down to the sides show (ExRx chin-up,
 * https://exrx.net/WeightExercises/LatissimusDorsi/BWUnderhandChinup); START hanging with straight arms; END the
 * elbows at the sides, the chin about at the bar.
 */
private val chinUp: ExerciseFigure by lazy {
    val (start, end) = chinUpPoses(FRONT_HANG_LEG)
    figure(
        View.FRONT, start, end, listOf(PullUpBar(x = CHIN_BAR.x, height = -CHIN_BAR.y)),
        startCaption = "Hang with straight arms",
        endCaption = "Elbows to your sides",
        thumbnail = Which.END,
        guideLines = listOf(
            "Grip the bar underhand, about shoulder-width apart, thumbs wrapped around the bar.",
            "Hang with straight arms.",
            "Cross one leg over the other, and brace your abs.",
            "Pull yourself up until your elbows are at your sides.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH)),
        checks = listOf(UpperArmDown(25.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/**
 * Weighted chin-up. Side view, so the plate on the dip belt shows hanging below the crossed legs (as the Weighted
 * pull-up's); START hanging with straight arms; END the elbows at the sides, as the Assisted chin-up's top (the upper
 * arm 8° forward of the torso's line, the elbow folded 145°), the bar at the upper chest. ExRx weighted chin-up
 * (https://exrx.net/WeightExercises/LatissimusDorsi/WtUnderhandChinup).
 */
private val weightedChinUp: ExerciseFigure by lazy {
    val arm = Arm(shoulder = 8.0, elbow = 145.0)
    val offset = hangAt(Point(0.0, 0.0), arm).skeleton().let { it.near.grip - it.shoulder }
    val props = listOf(PullUpBar(x = PULL_BAR.x, height = -PULL_BAR.y), DipBelt(drop = 60.0))
    figure(
        View.SIDE, pullHang, hangAt(PULL_BAR - offset, arm), props,
        startCaption = "Hang with straight arms",
        endCaption = "Elbows to your sides",
        thumbnail = Which.END,
        guideLines = listOf(
            "Put on a dip belt with the weight hanging from it.",
            "Grip the bar underhand, about shoulder-width apart.",
            "Hang with straight arms.",
            "Pull yourself up until your elbows are at your sides.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH)),
        checks = listOf(UpperArmDown(12.0, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

// ---- pullovers ----

/**
 * DB pullover. Lying with the upper back across a flat bench (seen end-on), the body square to it, the hips let down
 * below it (the torso 12° above level towards the head, neutral) and the feet flat; one dumbbell in both hands under
 * its inner plate, in profile along the forearms. The elbows' slight bend (15°, neutral) fixed. START over the chest;
 * END lowered back over the head until the upper arms are about level with the torso (10° short of in line, the
 * conservative side). ExRx dumbbell pullover (https://exrx.net/WeightExercises/PectoralSternal/DBPullover).
 * The guide's safety line ("Use a spotter."): a spotter half-kneels at the head, the hands near the dumbbell without
 * holding it (ACE's chest press and lying chest fly pages: a split stance or half-kneeling, the hands near the wrists,
 * https://www.acefitness.org/resources/everyone/exercise-library/19/chest-press/ and
 * https://www.acefitness.org/resources/everyone/exercise-library/21/lying-chest-fly/).
 */
private val dbPullover: ExerciseFigure by lazy {
    val torso = 168.0
    // The bench's edge on the hips' side 2 cm past the upper back (8 cm below the shoulder joint along the torso), the
    // pad under the shoulders: the hips drop off that edge.
    val bench = FlatBench(x = 2.0 - FlatBench.WIDTH, length = FlatBench.WIDTH)
    val back = Point(0.0, -FlatBench.HEIGHT)
    val shoulder = back + unit(torso) * 8.0 + unit(torso - 90.0) * Body.TORSO_RADIUS
    val pelvis = shoulder - unit(torso) * Segment.TORSO.length
    val legs = Reach.leg(pelvis, Point(pelvis.x + 40.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = legs)
    val bend = 15.0
    // Over the chest: the dumbbell's centre straight above the chest touch point.
    val chest = pose(HANG).skeleton().point(BodyPoint.CHEST)
    val overA = solve(60.0, 120.0) { a -> dbCentre(pose(Arm(a, bend)).skeleton()).x - chest.x }
    val over = Arm(overA, bend)
    val behind = Arm(170.0, bend)
    fun spotter(p: Pose): Spotter {
        val s = p.skeleton()
        val wrist = s.near.wrist
        // Half-kneeling: the back knee on the floor (its thigh upright, the shin back along the floor), the front foot
        // flat with the shin upright; the torso leaning 10° towards the bench.
        val kneel = Leg(hip = 10.0, knee = 90.0, ankle = -45.0)
        val front = Leg(hip = 100.0, knee = 90.0, ankle = 0.0)
        val pelvisY = -(Segment.THIGH.length * cos(0.0) + Body.SHIN_WIDTH / 2)
        val sp = Pose(s.head.x - 45.0, pelvisY, 80.0, nearArm = HANG, nearLeg = front, farLeg = kneel)
        val tsh = sp.skeleton().shoulder
        val target = wrist + Point(-8.0, 6.0)
        val reach = if ((target - tsh).length() < ARM_REACH - 1.0) armTo(tsh, target, 80.0) else straightArm(tsh, target, 80.0)
        return Spotter(sp.copy(nearArm = reach, farArm = reach))
    }
    val start = pose(over)
    val end = pose(behind)
    figure(
        View.SIDE, start, end, listOf(Floor, spotter(start), bench, PulloverDumbbell),
        endProps = listOf(Floor, spotter(end), bench, PulloverDumbbell),
        startCaption = "Over your chest",
        endCaption = "Upper arms level",
        thumbnail = Which.END,
        guideLines = listOf(
            OVER_FACE_SPOT,
            "Lie with your upper back across a flat bench, your body at a right angle to it.",
            "Let your hips and legs extend off the bench. They act as a counterweight.",
            "Hold one dumbbell with both hands under its inner plate, over your chest, with your elbows slightly bent.",
            "Keeping the same slight bend in your elbows, lower the dumbbell back over your head until your upper arms are about level with your torso.",
        ),
        contacts = listOf(Contact(BodyPoint.UPPER_BACK, Surface.BENCH, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(HeldOver(BodyPoint.CHEST, 2.0, Which.START), ElbowBend(10.0, 20.0, Which.START), ElbowBend(10.0, 20.0, Which.END)),
    )
}

private fun dbCentre(s: Skeleton): Point {
    val f = (s.near.wrist - s.near.elbow).let { it * (1.0 / it.length()) }
    return s.near.grip - f * (Dumbbell.HANDLE / 2 + 2.5)
}

/**
 * Machine pullover. Seated upright against the back pad, feet flat; the machine's pivot at the shoulder joint, the
 * elbows on its pads, the hands on its bar. The elbows stay bent 100° (neutral). START the upper arms up in line with
 * the torso, as far as the shoulders allow (170°, the conservative side), the hands behind the head; END pulled forward
 * and down until the elbows are at the sides. ExRx lever pullover
 * (https://exrx.net/WeightExercises/LatissimusDorsi/LVPullover).
 */
private val machinePullover: ExerciseFigure by lazy {
    val seat = 41.0
    val back = 85.0
    val torso = 180.0 - back
    val hinge = Point(-20.0, -seat)
    val r = back / DEG
    val t = (Body.TORSO_RADIUS - Body.TORSO_RADIUS * cos(r)) / sin(r)
    val pelvis = hinge + unit(180.0 - back) * t + unit(90.0 - back) * Body.TORSO_RADIUS
    val legs = Reach.leg(pelvis, Point(pelvis.x + 42.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = legs)
    val shoulder = shoulderOf(pelvis, torso)
    val machine = PulloverMachine(seatX = hinge.x + PulloverMachine.SEAT_LENGTH, seatHeight = seat, backAngle = back, pivot = shoulder)
    figure(
        View.SIDE, pose(Arm(170.0, 100.0)), pose(Arm(5.0, 100.0)), listOf(Floor, machine),
        startCaption = "Upper arms in line",
        endCaption = "Elbows to your sides",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so the machine's pivot is close to your shoulder.",
            "Sit down and push the foot lever. Put your elbows on the pads and grip the bar behind you.",
            "Pull the lever forward and down until your elbows are at your sides.",
            "Return until your upper arms are in line with your torso, or as far as your shoulders allow.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH), Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
        ),
        checks = listOf(UpperArmDown(10.0, Which.END)),
    )
}

// ---- shrugs ----

/**
 * The shrug's lift: the shoulder girdle raised 7.5 cm, "as high as you can". A shrug raises the clavicle about 25°
 * (SD 5°) in a whole-cadaver study (https://pmc.ncbi.nlm.nih.gov/articles/PMC9937811/); on a clavicle of about 15 cm
 * (neutral) that lifts its outer end 6–7.5 cm (25–30°); the upper value is drawn so the rise shows at phone size.
 */
private const val SHRUG = 7.5

/** Standing tall, front view, the arms hanging [abduct]° out from the sides. */
private fun frontStand(abduct: Double, shrug: Double): Pose {
    val pelvisY = -(Body.ANKLE_HEIGHT + Segment.SHIN.length + Segment.THIGH.length)
    return Pose(0.0, pelvisY, 90.0, nearArm = Arm(abduct, 0.0), nearLeg = Leg(hip = 4.0, knee = 0.0), shrug = shrug)
}

/** The arms' angle out from the sides that puts the grips [half] cm out from the middle. */
private fun abductFor(half: Double) = asin((half - Body.SHOULDER_HALF_WIDTH) / ARM_REACH) * DEG

/**
 * Barbell shrug. Front view, so the shoulders' rise and the grip show: standing tall, the bar hanging in front of the
 * thighs on straight arms, the hands a little wider than the shoulders (25 cm out, neutral); START the shoulders down;
 * END lifted as high as they go ([SHRUG]), the arms still straight. ExRx barbell shrug
 * (https://exrx.net/WeightExercises/TrapeziusUpper/BBShrug).
 */
private val barbellShrug: ExerciseFigure by lazy {
    val a = abductFor(25.0)
    figure(
        View.FRONT, frontStand(a, 0.0), frontStand(a, SHRUG), listOf(Floor, Barbell()),
        startCaption = "Stand up straight",
        endCaption = "Shoulders up high",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand up straight holding the bar with an overhand or mixed grip, shoulder-width or a little wider.",
            "Lift your shoulders as high as you can.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START), ElbowBend(0.0, 1.0, Which.END)),
    )
}

/**
 * DB shrug. Front view: standing fully upright, a dumbbell at each side on straight arms, the palms facing in, so each
 * dumbbell's handle points at the camera and it shows end-on (Kit.kt Dumbbell.endOnFront), clear of the thighs; START
 * the shoulders down, END lifted. ExRx dumbbell shrug (https://exrx.net/WeightExercises/TrapeziusUpper/DBShrug).
 */
private val dbShrug: ExerciseFigure by lazy {
    val a = abductFor(29.0)
    val props = listOf(Floor, Dumbbell(Side.FAR, endOnFront = true), Dumbbell(Side.NEAR, endOnFront = true))
    figure(
        View.FRONT, frontStand(a, 0.0), frontStand(a, SHRUG), props,
        startCaption = "Dumbbell at each side",
        endCaption = "Shoulders up high",
        thumbnail = Which.END,
        guideLines = listOf("Stand holding a dumbbell at each side.", "Stand fully upright, or lean very slightly forward.", "Lift your shoulders as high as you can."),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START), ElbowBend(0.0, 1.0, Which.END)),
    )
}

/**
 * Trap-bar shrug. Side view, so the frame round the legs shows (as the approved Trap-bar deadlift's): standing tall
 * inside the bar, the handles at the sides on straight arms; START the shoulders down, END lifted ([SHRUG]): the
 * shoulders' tops rise towards the ears and the bar rises with them. ExRx trap bar shrug
 * (https://exrx.net/WeightExercises/TrapeziusUpper/TBShrug).
 */
private val trapBarShrug: ExerciseFigure by lazy {
    val pelvisY = -(Body.ANKLE_HEIGHT + Segment.SHIN.length + Segment.THIGH.length)
    fun pose(shrug: Double) = Pose(0.0, pelvisY, 90.0, nearArm = HANG, nearLeg = Leg(0.0, 0.0), shrug = shrug)
    figure(
        View.SIDE, pose(0.0), pose(SHRUG), listOf(Floor, TrapBar),
        startCaption = "Handles at your sides",
        endCaption = "Shoulders up high",
        thumbnail = Which.END,
        guideLines = listOf("Stand inside the trap bar, holding its handles at your sides.", "Lift your shoulders as high as you can."),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(HandlesBesideLegs(3.0, Which.START), HandlesBesideLegs(3.0, Which.END), ElbowBend(0.0, 1.0, Which.END)),
    )
}

// ---- back extensions ----

/** The 45° bench's body line. */
private const val EXTENSION_LINE = 45.0

/**
 * A back extension's two poses on the 45° bench: the legs straight along the line, the hips over the thigh pad (its
 * top edge 12 cm below the hip joint, so the belly stays off it at the bottom: the guide's set-up), the heels under the
 * roller, the soles on the plate; START the body in line with the legs, END bent at the hips to a right angle. The
 * arms are crossed on the chest, the hands at [handsFront] cm in front of the chest touch point.
 */
private fun extensionPoses(handsFront: Double): Triple<Pose, Pose, HyperBench> {
    val pelvis = Point(0.0, -105.0)
    val down = EXTENSION_LINE + 180.0
    val front = unit(EXTENSION_LINE - 90.0)
    val ankle = pelvis + unit(down) * (Segment.THIGH.length + Segment.SHIN.length)
    val pad = pelvis + unit(down) * 27.0 + front * (Body.THIGH_WIDTH / 2)
    val plate = ankle + unit(down) * Body.ANKLE_HEIGHT + front * 7.0
    val roller = ankle + unit(EXTENSION_LINE) * 9.0 + unit(EXTENSION_LINE + 90.0) * (Body.SHIN_WIDTH / 2 + HyperBench.ROLLER_RADIUS)
    val bench = HyperBench(pad = pad, footPlate = plate, roller = roller, angle = EXTENSION_LINE)
    fun pose(torso: Double): Pose {
        // The legs keep the line: the hip angle is the torso's bend from it.
        val leg = Leg(hip = EXTENSION_LINE - torso, knee = 0.0, ankle = 0.0)
        val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = HANG, nearLeg = leg).skeleton()
        val hands = bare.point(BodyPoint.CHEST) + unit(torso - 90.0) * handsFront
        return Pose(pelvis.x, pelvis.y, torso, nearArm = armTo(bare.shoulder, hands, torso), nearLeg = leg)
    }
    return Triple(pose(EXTENSION_LINE), pose(EXTENSION_LINE - 90.0), bench)
}

private val EXTENSION_CONTACTS = listOf(
    Contact(BodyPoint.THIGHS, Surface.THIGH_PAD, BOTH),
    Contact(BodyPoint.FEET, Surface.FOOT_PLATE, BOTH),
    Contact(BodyPoint.SHINS, Surface.LEG_BRACE, BOTH),
)

/**
 * Back extension. The 45° bench (ExRx 45° hyperextension, https://exrx.net/WeightExercises/ErectorSpinae/BW45HyperextensionHips);
 * START the body in line with the legs, the back neutral (not arched past the line); END lowered at the hips until the
 * torso is at a right angle to the legs. The hands crossed on the chest.
 */
private val backExtension: ExerciseFigure by lazy {
    val (top, bottom, bench) = extensionPoses(Body.HAND_WIDTH / 2 + 1.0)
    figure(
        View.SIDE, top, bottom, listOf(Floor, bench),
        startCaption = "In line with your legs",
        endCaption = "Right angle to your legs",
        thumbnail = Which.START,
        guideLines = listOf(
            "Hook your heels under the foot pad.",
            "Cross your hands on your chest, or keep them low on your body.",
            "Stop when your torso is about at a right angle to your legs.",
            "Use your lower back, glutes and hamstrings to raise your body until your back is neutral and in line with your legs.",
        ),
        contacts = EXTENSION_CONTACTS,
        checks = emptyList(),
    )
}

/**
 * Weighted back extension. The back extension with a plate held flat on the chest under the crossed arms (Kit.kt
 * ChestPlate). ExRx weighted 45° back extension (https://exrx.net/WeightExercises/ErectorSpinae/Wt45BackExtension).
 */
private val weightedBackExtension: ExerciseFigure by lazy {
    val (top, bottom, bench) = extensionPoses(ChestPlate.THICKNESS + Body.HAND_WIDTH / 2 + 1.0)
    figure(
        View.SIDE, top, bottom, listOf(Floor, bench, ChestPlate()),
        startCaption = "In line with your legs",
        endCaption = "Right angle to your legs",
        thumbnail = Which.START,
        guideLines = listOf(
            "Hook your heels under the foot pad.",
            "Hold a weight to your chest.",
            "Stop when your torso is about at a right angle to your legs.",
            "Use your lower back, glutes and hamstrings to raise your body until your back is neutral and in line with your legs.",
        ),
        contacts = EXTENSION_CONTACTS,
        checks = emptyList(),
    )
}

// ---- face pull ----

/**
 * Face pull. Standing with the feet parallel and the knees slightly bent (10°, neutral), the torso upright, facing a
 * high pulley with a rope (NSCA face pull, https://www.nsca.com/education/articles/kinetic-select/face-pull-machine/;
 * ExRx cable rear delt row with a rope, https://exrx.net/WeightExercises/DeltoidPosterior/CBStandingRearDeltRowRope).
 * START the arms straight in front of the face, the hands 12 cm out from the middle; END the rope pulled to the face,
 * the elbows out to the sides and high (the upper arms level with the shoulders, out of the picture: drawn short), the
 * forearms turned up so the hands are beside the face, 20 cm out.
 */
private val facePull: ExerciseFigure by lazy {
    val leg = standingLeg(0.0, shinLean = 5.0, knee = 10.0, torso = 90.0)
    val shoulder = shoulderOf(leg.hip, 90.0)
    fun pose(arm: Arm) = standOn(leg, 90.0, arm)
    val bare = pose(HANG).skeleton()
    val face = bare.head
    val sh = V3(shoulder.x, shoulder.y, Body.SHOULDER_HALF_WIDTH)
    val dy = face.y - shoulder.y
    val dz = 12.0 - Body.SHOULDER_HALF_WIDTH
    val ahead = sqrt(ARM_REACH * ARM_REACH - dy * dy - dz * dz)
    val start = arm3(sh, V3(sh.x + ahead, face.y, 12.0), V3(0.0, 0.0, 1.0), 90.0)
    val end = arm3(sh, V3(face.x + Body.HEAD_RADIUS + 2.0, face.y + 2.0, 20.0), V3(0.15, 0.25, 1.0), 90.0)
    val startGrip = pose(start).skeleton().near.grip
    val stack = CableStack(x = startGrip.x + 60.0, pulleyHeight = -startGrip.y + 10.0, reach = -30.0)
    figure(
        View.SIDE, pose(start), pose(end), listOf(Floor, stack, CableAttachment(Attachment.ROPE)),
        startCaption = "Arms straight in front",
        endCaption = "Rope toward your face",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a rope on a high pulley.",
            "Step back until the cable is tight with your arms straight in front of your face and your elbows pointing out.",
            "Stand with your feet parallel and your knees slightly bent. Keep your head in line with your spine and your torso upright.",
            "Pull the rope toward your face, moving your elbows out to the sides as they bend.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START)),
    )
}

/** The batch's figures by catalogue name, each built on first use (Figures.kt's laziness rule). */
private val backBatchFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf(
        "Row" to lazy { row },
        "Seated cable row" to lazy { seatedCableRow },
        "Wide-grip cable row" to lazy { wideGripCableRow },
        "One-arm cable row" to lazy { oneArmCableRow },
        "Machine row" to lazy { machineRow },
        "Chest-supported row" to lazy { chestSupportedRow },
        "Seal row" to lazy { sealRow },
        "T-bar row" to lazy { tBarRow },
        "Chest-supported T-bar row" to lazy { chestSupportedTBarRow },
        "Inverted row" to lazy { invertedRow },
        "Close-grip pulldown" to lazy { closeGripPulldown },
        "Underhand pulldown" to lazy { underhandPulldown },
        "Machine pulldown" to lazy { machinePulldown },
        "One-arm pulldown" to lazy { oneArmPulldown },
        "Straight-arm pulldown" to lazy { straightArmPulldown },
        "Chin-up" to lazy { chinUp },
        "Weighted pull-up" to lazy { weightedPullUp },
        "Weighted chin-up" to lazy { weightedChinUp },
        "Assisted pull-up" to lazy { assistedPullUp },
        "Assisted chin-up" to lazy { assistedChinUp },
        "Band-assisted pull-up" to lazy { bandAssistedPullUp },
        "Neutral-grip pull-up" to lazy { neutralGripPullUp },
        "DB pullover" to lazy { dbPullover },
        "Machine pullover" to lazy { machinePullover },
        "Barbell shrug" to lazy { barbellShrug },
        "DB shrug" to lazy { dbShrug },
        "Trap-bar shrug" to lazy { trapBarShrug },
        "Back extension" to lazy { backExtension },
        "Weighted back extension" to lazy { weightedBackExtension },
        "Face pull" to lazy { facePull },
    )
}

internal val backFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf("Pull-up" to lazy { pullUp }, "Lat pulldown" to lazy { latPulldown }, "One-arm DB row" to lazy { oneArmRow }) + backBatchFigures
}
