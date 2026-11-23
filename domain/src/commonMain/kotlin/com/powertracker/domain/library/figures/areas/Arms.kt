package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.OVER_FACE_SPOT
import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.*
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sqrt

// Figures for the library's Arms area (library/areas/Arms.kt), keyed by catalogue name (Task 9).
//
// Camera: the side view for every arm exercise. Curls and triceps work bend and straighten the elbow in the sagittal
// plane, and what each guide is about shows from the side: where the upper arm is (hanging at the side for the
// standing curls and pushdowns, behind the body on the incline curl, forward on the pad for the preacher, machine and
// spider curls, on the thigh for the concentration curl, beside the head for the overhead extensions, upright for the
// lying extensions, back along the torso for the kickback), that it stays put between the two panels while the
// forearm moves, and the stop points. The grip shows through the dumbbell: palms in (a hammer grip, or the start of a
// supinating curl) puts the handle in profile, square to the forearm; palms up puts it across the body, end-on, a disc.
// The two close push-ups stay side-on as well: the guides' difference that shows from the side is where the hands are
// along the body (under the shoulders, or together under the chest); their spacing across the body cannot show, so
// the captions name where the hands are, not how far apart. A reverse curl looks like a curl from the side (only the
// palms turn), so its captions name what the picture shows.
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

private val HANG = Arm(0.0, 0.0)
private val FEET_DOWN = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH))

// Guide lines shared by several arm guides (library/areas/Arms.kt), verbatim.
private const val LOWER_TO_STRAIGHT = "Lower under control until your arms are straight."
private const val ARM_ON_PAD = "Keep the back of your upper arm on the pad the whole time."
private const val PAD_SEAT = "Set the seat so your armpit rests near the top of the pad."
private const val WRIST_SEAT = "Sit and rest your forearms on your thighs, with your wrists just past your knees."

/** A dumbbell held palms in: the handle in profile, square to the forearm (Kit.kt Dumbbell.squareToForearm). */
private fun palmsIn(side: Side) = Dumbbell(side, neutral = true, squareToForearm = true)

// ---- standing ----

private val tall by lazy { standingLeg(0.0, shinLean = 0.0, knee = 0.0, torso = 90.0) }

private fun stand(near: Arm, far: Arm = near) = standOn(tall, 90.0, near, far)

/**
 * "One foot a little in front of the other": the near (back) foot [half] cm behind the hips and the far one [half] in
 * front, the hips lowered [drop] cm so the knees are just soft (neutral).
 */
private class Stagger(val pelvis: Point, val back: Leg, val front: Leg)

private fun stagger(torso: Double = 90.0, half: Double = 15.0, drop: Double = 2.0): Stagger {
    val pelvis = Point(0.0, tall.hip.y + drop)
    return Stagger(
        pelvis,
        Reach.leg(pelvis, Point(-half, -Body.ANKLE_HEIGHT), torso, foot = 0.0),
        Reach.leg(pelvis, Point(half, -Body.ANKLE_HEIGHT), torso, foot = 0.0),
    )
}

private fun Stagger.pose(near: Arm, far: Arm = near, torso: Double = 90.0) =
    Pose(pelvis.x, pelvis.y, torso, nearArm = near, nearLeg = back, farArm = far, farLeg = front)

/**
 * The top of a standing curl: the upper arm 8° forward of hanging, the elbow bent 145°, so the forearm is 27° short of
 * upright. ExRx's barbell curl (https://exrx.net/WeightExercises/Biceps/BBCurl) lets the elbows travel forward slightly
 * at the top so the forearms end up near vertical, no more; with the elbow's 150° limit (Body.kt) and the elbows kept
 * at the sides, as the guides ask, the figure stops here (8° and 145° are neutral values inside both).
 */
private val CURL_TOP = Arm(shoulder = 8.0, elbow = 145.0)

/** Straight arms hanging just forward, the grips [clear] cm in front of the hip joint (a bar in front of the thighs). */
private fun inFront(clear: Double) = Arm(shoulder = asin(clear / ARM_REACH) * DEG, elbow = 0.0)

/** A bar's straight-arm start: its shaft just in front of the thighs. */
private val BAR_HANG by lazy { inFront(Body.THIGH_WIDTH / 2 + Barbell.SHAFT_RADIUS + 0.5) }

private val CURL_CHECKS = listOf(
    UpperArmDown(10.0, Which.START), UpperArmDown(10.0, Which.END), ElbowBend(0.0, 1.0, Which.START), ElbowBend(140.0, 148.0, Which.END),
)

/**
 * Curl. Standing tall, a barbell in both hands; START the arms straight, the bar in front of the thighs; END curled
 * until the forearms are near upright, the elbows still at the sides (CURL_TOP). ExRx barbell curl
 * (https://exrx.net/WeightExercises/Biceps/BBCurl), ACE biceps curl
 * (https://www.acefitness.org/resources/everyone/exercise-library/70/bicep-curl/).
 */
private val curl: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(BAR_HANG), stand(CURL_TOP), listOf(Floor, Barbell()),
        startCaption = "Arms straight",
        endCaption = "Forearms upright",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand tall holding a barbell, or a dumbbell in each hand, with an underhand grip about shoulder-width apart.",
            "Line up your wrists, elbows and shoulders, and keep your elbows at your sides.",
            "Bend your elbows to curl the weight up until your forearms are upright.",
            "Lower it slowly until your arms are straight.",
        ),
        contacts = FEET_DOWN,
        checks = CURL_CHECKS,
    )
}

/**
 * EZ-bar curl. The Curl's poses with the EZ bar (KitArms.kt EZBar): from the side it is end-on like the barbell, told
 * apart by its smaller plates.
 */
private val ezBarCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(BAR_HANG), stand(CURL_TOP), listOf(Floor, EZBar),
        startCaption = "Arms hang straight",
        endCaption = "Forearms upright",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand tall holding an EZ bar with an underhand grip, about shoulder-width apart.",
            "Let your arms hang straight, with your elbows at your sides.",
            "Keep your elbows at your sides and curl the bar up until your forearms are upright.",
            LOWER_TO_STRAIGHT,
        ),
        contacts = FEET_DOWN,
        checks = CURL_CHECKS,
    )
}

/**
 * Reverse curl. The Curl's poses, palms down. From the side only the palms differ from a curl, which no side view
 * shows, so the captions name what does show: the hands, elbows and shoulders lined up, then the forearms upright.
 * ExRx barbell reverse curl (https://exrx.net/WeightExercises/Brachioradialis/BBReverseCurl).
 */
private val reverseCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(BAR_HANG), stand(CURL_TOP), listOf(Floor, Barbell()),
        startCaption = "Lined up with shoulders",
        endCaption = "Forearms upright",
        thumbnail = Which.END,
        guideLines = listOf(
            "Line up your hands with your shoulders and elbows.",
            "Keep your elbows close to your sides and curl the bar up until your forearms are upright. Lead with the backs of your hands.",
            "Lower slowly until your arms are straight.",
        ),
        contacts = FEET_DOWN,
        checks = CURL_CHECKS,
    )
}

/**
 * DB curl. Standing, a dumbbell at each side, palms in (in profile); END one arm curled with the palm turned up to face
 * the shoulder (the dumbbell now across the body: end-on), the other arm still hanging. ExRx dumbbell curl
 * (https://exrx.net/WeightExercises/Biceps/DBCurl).
 */
private val dbCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(HANG), stand(CURL_TOP, HANG), listOf(Floor, palmsIn(Side.FAR), palmsIn(Side.NEAR)),
        endProps = listOf(Floor, palmsIn(Side.FAR), Dumbbell(Side.NEAR)),
        startCaption = "Palms facing in",
        endCaption = "Palm faces your shoulder",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand with a dumbbell in each hand at your sides, palms facing in and arms straight.",
            "Keep your elbow at your side. Curl one dumbbell up, turning your palm up as you go, until your forearm is upright and your palm faces your shoulder.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(UpperArmDown(10.0, Which.START), UpperArmDown(10.0, Which.END), ElbowBend(140.0, 148.0, Which.END, Side.NEAR)),
    )
}

/**
 * Hammer curl. Staggered stance, a dumbbell beside each thigh, palms facing the body (in profile, square to the
 * forearm, in both panels: the palms stay in); END both curled to the front of the shoulders (CURL_TOP). ACE hammer
 * curl (https://www.acefitness.org/resources/everyone/exercise-library/10/hammer-curl/), ExRx dumbbell hammer curl
 * (https://exrx.net/WeightExercises/Brachioradialis/DBHammerCurl).
 */
private val hammerCurl: ExerciseFigure by lazy {
    val st = stagger()
    figure(
        View.SIDE, st.pose(HANG), st.pose(CURL_TOP), listOf(Floor, palmsIn(Side.FAR), palmsIn(Side.NEAR)),
        startCaption = "Palms facing your body",
        endCaption = "To front of shoulders",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand with one foot a little in front of the other, for balance.",
            "Hold a dumbbell in each hand beside your thighs, palms facing your body and arms straight.",
            "Breathe out and bend your elbows to curl the dumbbells up to the front of your shoulders. Keep your palms facing in.",
        ),
        contacts = FEET_DOWN,
        checks = CURL_CHECKS,
    )
}

/** A low pulley in front of the lifter, who faces it (the column 50 cm in front, the pulley 15 cm up; neutral). */
private val lowPulleyInFront = CableStack(x = 50.0, pulleyHeight = 15.0, reach = -12.0)

/**
 * Cable curl. Facing a low pulley, standing close to it, a straight bar in both hands; START arms straight, END curled
 * (CURL_TOP). The cable runs from the pulley to the bar in both panels. ExRx cable curl
 * (https://exrx.net/WeightExercises/Biceps/CBCurl).
 */
private val cableCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(BAR_HANG), stand(CURL_TOP), listOf(Floor, lowPulleyInFront, CableAttachment(Attachment.STRAIGHT_BAR)),
        startCaption = "Arms straight",
        endCaption = "Forearms upright",
        thumbnail = Which.END,
        guideLines = listOf(
            "Attach a bar to the low pulley.",
            "Hold the bar with an underhand grip, about shoulder-width apart, and stand close to the pulley.",
            "Keep your elbows at your sides and curl the bar up until your forearms are upright.",
            LOWER_TO_STRAIGHT,
        ),
        contacts = FEET_DOWN,
        checks = CURL_CHECKS,
    )
}

/** Cable hammer curl. The cable curl with a rope held palms in. ExRx cable hammer curl (https://exrx.net/WeightExercises/Brachioradialis/CBHammerCurl). */
private val cableHammerCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(BAR_HANG), stand(CURL_TOP), listOf(Floor, lowPulleyInFront, CableAttachment(Attachment.ROPE)),
        startCaption = "Arms straight down",
        endCaption = "Forearms upright",
        thumbnail = Which.END,
        guideLines = listOf(
            "Attach a rope to the low pulley.",
            "Hold the rope with your palms facing in. Stand upright with your arms straight down.",
            "Keep your elbows at your sides. Curl the rope forward and up with both arms until your forearms are upright.",
            LOWER_TO_STRAIGHT,
        ),
        contacts = FEET_DOWN,
        checks = CURL_CHECKS,
    )
}

/**
 * One-arm cable curl. Facing the low pulley, a single handle in the near hand, the free arm hanging. ExRx cable
 * one-arm curl (https://exrx.net/WeightExercises/Biceps/CBOneArmCurl).
 */
private val oneArmCableCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(BAR_HANG, HANG), stand(CURL_TOP, HANG), listOf(Floor, lowPulleyInFront, CableHandle(Side.NEAR)),
        startCaption = "Arm straight",
        endCaption = "Forearm upright",
        thumbnail = Which.END,
        guideLines = listOf(
            "Attach a single handle to the low pulley.",
            "Hold it with an underhand grip and your elbow at your side. You can face the pulley, or stand with your working side toward it.",
            "Keep your elbow at your side and curl the handle up until your forearm is upright.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(UpperArmDown(10.0, Which.START), UpperArmDown(10.0, Which.END), ElbowBend(140.0, 148.0, Which.END, Side.NEAR)),
    )
}

/**
 * Drag curl. Standing, a barbell in both hands; START arms straight; END the bar dragged up the body to the upper
 * abdomen (0.5 cm off it), the elbows travelled back behind the body and the forearms about level: the guide's
 * position "once your forearms pass level" (its first two steps), the one that tells this curl apart. The top, the
 * forearms upright, is the Curl's. ExRx barbell drag curl (https://exrx.net/WeightExercises/Biceps/BBDragCurl).
 */
private val dragCurl: ExerciseFigure by lazy {
    val bare = stand(HANG).skeleton()
    val bar = bare.onTorso(Segment.TORSO.length - Body.CHEST_BELOW_SHOULDER - 10.0, Body.CHEST_DEPTH - 1.5 + Barbell.SHAFT_RADIUS + 0.5)
    val up = Reach.arm(bare.shoulder, bar, 90.0)
    figure(
        View.SIDE, stand(BAR_HANG), stand(up), listOf(Floor, Barbell()),
        startCaption = "Arms straight",
        endCaption = "Elbows travel back",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand tall holding a barbell with an underhand grip, arms straight.",
            "Pull the bar straight up, letting your elbows travel back. Keep the bar close to your hips and waist.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(0.0, 1.0, Which.START), BarTouches(BodyPoint.UPPER_ABDOMEN, Which.END)),
    )
}

// ---- seated ----

/** Sitting on the end of a flat bench: the hips on its pad, which runs back behind the lifter (only 15 cm under the thighs). */
private val SEAT_PELVIS = Point(0.0, -(FlatBench.HEIGHT + Body.TORSO_RADIUS))
private val endOfBench = FlatBench(x = SEAT_PELVIS.x + 15.0 - FlatBench.LENGTH)

/** Seated legs: the feet flat [ahead] cm in front of the hips (knees about 90°, legs together; neutral). */
private fun seatedLegs(torso: Double, ahead: Double = 44.0) = Reach.leg(SEAT_PELVIS, Point(SEAT_PELVIS.x + ahead, -Body.ANKLE_HEIGHT), torso, foot = 0.0)

private fun seated(near: Arm, far: Arm = near, torso: Double = 90.0) =
    Pose(SEAT_PELVIS.x, SEAT_PELVIS.y, torso, nearArm = near, nearLeg = seatedLegs(torso), farArm = far)

private val SEATED_CONTACTS = listOf(Contact(BodyPoint.HIPS, Surface.BENCH, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH))

/**
 * Seated DB curl. Sitting tall on the end of a bench, legs together, arms hanging, palms in; END one arm curled, palm
 * up (as the DB curl). ExRx dumbbell seated curl (https://exrx.net/WeightExercises/Biceps/DBSeatedCurl).
 */
private val seatedDbCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, seated(HANG), seated(CURL_TOP, HANG), listOf(Floor, endOfBench, palmsIn(Side.FAR), palmsIn(Side.NEAR)),
        endProps = listOf(Floor, endOfBench, palmsIn(Side.FAR), Dumbbell(Side.NEAR)),
        startCaption = "Arms hanging straight",
        endCaption = "Palm faces your shoulder",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on the end of a bench with your legs together.",
            "Hold a dumbbell in each hand, palms facing in and arms hanging straight down.",
            "Keep your elbow back at your side. Curl one dumbbell up, turning your palm up, until your forearm is upright and your palm faces your shoulder.",
        ),
        contacts = SEATED_CONTACTS,
        checks = listOf(UpperArmDown(10.0, Which.START), UpperArmDown(10.0, Which.END), ElbowBend(140.0, 148.0, Which.END, Side.NEAR)),
    )
}

/** The shoulder a torso's length from [pelvis] and an upper arm's length from [elbow]: the higher of the two. */
private fun shoulderFor(pelvis: Point, elbow: Point): Point {
    val r1 = Segment.TORSO.length
    val r2 = Segment.UPPER_ARM.length
    val d = elbow - pelvis
    val dist = d.length()
    val a = (r1 * r1 - r2 * r2 + dist * dist) / (2 * dist)
    val h = sqrt(r1 * r1 - a * a)
    val mid = pelvis + d * (a / dist)
    val perp = Point(-d.y / dist, d.x / dist)
    return listOf(mid + perp * h, mid - perp * h).minBy { it.y }
}

/** A seated body leaning forward so its near elbow is at [elbow]: the torso angle, the shoulder and the upper arm's screen angle. */
private class Lean(val torso: Double, val shoulder: Point, val upper: Double)

private fun leanTo(elbow: Point): Lean {
    val sh = shoulderFor(SEAT_PELVIS, elbow)
    return Lean(angleTo(sh - SEAT_PELVIS), sh, angleTo(elbow - sh))
}

/** The arm of [lean] with its forearm at screen angle [fore] and the wrist bent [wrist]. */
private fun Lean.arm(fore: Double, wrist: Double = 0.0) =
    Arm(shoulder = fold180(upper - torso - 180.0), elbow = fold180(fore - upper), wrist = wrist)

/** The near thigh's hip and knee for a seated pose (the legs do not depend on the torso's angle). */
private val seatedThigh by lazy { seated(HANG).skeleton().near.let { it.hip to it.knee } }

/** A point [above] cm over the near thigh's centre line, [along] of the way from the hip to the knee. */
private fun onThigh(along: Double, above: Double): Point {
    val (hip, knee) = seatedThigh
    val d = (knee - hip).let { it * (1.0 / it.length()) }
    return hip + (knee - hip) * along + Point(d.y, -d.x) * above
}

/**
 * Concentration curl. Sitting on a bench leaning forward, the back of the upper arm against the thigh near the knee
 * (seen from the side the arm is drawn over the near thigh: inner or outer cannot show), the dumbbell held palm up
 * (end-on); START the arm straight, the dumbbell between the feet; END curled up towards the front of the shoulder
 * (the elbow 145°, the figure's working limit). The elbow rests 8 cm above the thigh's centre line (on its inside,
 * neutral). The free arm hangs straight down from the shoulder, in front of the bench, beside the legs (the guide
 * leaves it unplaced). A hand on the far thigh was tried: with the shoulder leaning out over the knee it is too close
 * for the arm to reach in the picture's plane, and the elbow folds up behind the back.
 * ExRx dumbbell concentration curl (https://exrx.net/WeightExercises/Brachialis/DBConcentrationCurl).
 */
private val concentrationCurl: ExerciseFigure by lazy {
    val elbow = onThigh(0.8, 8.0)
    val lean = leanTo(elbow)
    val down = lean.arm(lean.upper)
    val up = lean.arm(lean.upper + 145.0)
    // The free arm straight down: 270° on screen, so its shoulder angle is 270 - (torso + 180).
    val free = Arm(shoulder = 270.0 - (lean.torso + 180.0), elbow = 0.0)
    figure(
        View.SIDE, seated(down, free, lean.torso), seated(up, free, lean.torso), listOf(Floor, endOfBench, Dumbbell(Side.NEAR)),
        startCaption = "Arm against your thigh",
        endCaption = "Toward your shoulder",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on a bench with a dumbbell between your feet.",
            "Hold the dumbbell and rest the back of your upper arm against your inner thigh.",
            "Curl the dumbbell up toward the front of your shoulder.",
            "Lower under control until your arm is straight.",
        ),
        contacts = SEATED_CONTACTS,
        checks = listOf(ElbowBend(0.0, 1.0, Which.START, Side.NEAR), ElbowBend(140.0, 148.0, Which.END, Side.NEAR)),
    )
}

/**
 * The wrist curls' arms: leaning forward on the bench, both forearms lying on the thighs (their centre lines on top of
 * the thighs, along them), the wrists 4 cm past the knees; the near hand bent [wrist]°, the far one straight.
 */
private fun wristCurlPose(wrist: Double): Pose {
    val (hip, knee) = seatedThigh
    val d = (knee - hip).let { it * (1.0 / it.length()) }
    val lift = Body.THIGH_WIDTH / 2 + Body.FOREARM_WIDTH / 2
    val w = knee + d * 4.0 + Point(d.y, -d.x) * lift
    val elbow = w - d * Segment.FOREARM.length
    val lean = leanTo(elbow)
    val fore = angleTo(d)
    return seated(lean.arm(fore, wrist), lean.arm(fore), lean.torso)
}

/**
 * The wrist's bends: 50° each way, inside the figure's conservative 60° flexion / 70° extension limits (Body.kt, from
 * https://exrx.net/Testing/JointROM). The guides' "as high as you can" and "as far as you can" are drawn there.
 */
private const val WRIST_BEND = 50.0

private val WRIST_CONTACTS = SEATED_CONTACTS

/**
 * Wrist curl. Seated, the forearms on the thighs, the wrists just past the knees, a dumbbell in the near hand palm up
 * (the guide's "Or hold a dumbbell in one hand"; end-on); START the wrist bent back, the hand down, the weight rolled
 * towards the fingers; END the wrist curled up. Only the hand moves. ExRx dumbbell wrist curl
 * (https://exrx.net/WeightExercises/WristFlexors/DBWristCurl).
 */
private val wristCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, wristCurlPose(-WRIST_BEND), wristCurlPose(WRIST_BEND), listOf(Floor, endOfBench, Dumbbell(Side.NEAR)),
        startCaption = "Down to your fingers",
        endCaption = "Curl up with your wrist",
        thumbnail = Which.END,
        guideLines = listOf(
            "Hold a barbell with an underhand grip, hands about shoulder-width apart or closer. Or hold a dumbbell in one hand.",
            WRIST_SEAT,
            "Let the bar roll down out of your palms to your fingers.",
            "Curl it back up by bending your wrists, as high as you can.",
        ),
        contacts = WRIST_CONTACTS,
        checks = emptyList(),
    )
}

/**
 * Reverse wrist curl. The wrist curl's seat, the dumbbell held palm down; START the knuckles pointing down (the wrist
 * bent down 50°), END raised until they point up (bent back 50°). The figure's wrist angle is measured the same way
 * both times (up is +), so for the palm-down hand up is extension and down is flexion; 50° keeps both inside the
 * smaller 60° limit. Its thumbnail is START (the hand down), the wrist curl's END (the hand up), so the two rows differ
 * on the Exercises tab. ExRx dumbbell reverse wrist curl (https://exrx.net/WeightExercises/WristExtensors/DBReverseWristCurl).
 */
private val reverseWristCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, wristCurlPose(-WRIST_BEND), wristCurlPose(WRIST_BEND), listOf(Floor, endOfBench, Dumbbell(Side.NEAR)),
        startCaption = "Knuckles point down",
        endCaption = "Knuckles point up",
        thumbnail = Which.START,
        guideLines = listOf(
            "Hold a barbell with an overhand grip, hands about shoulder-width apart or closer. Or hold a dumbbell in one hand.",
            WRIST_SEAT,
            "Raise the weight by bending your wrists back, so your knuckles point up as high as you can.",
            "Lower it until your knuckles point down as far as you can.",
        ),
        contacts = WRIST_CONTACTS,
        checks = emptyList(),
    )
}

// ---- incline and prone curls ----

/**
 * The incline curl's bench: back at 60°. ExRx's alternating incline curl
 * (https://exrx.net/WeightExercises/Biceps/DBInclineCurl) sets the bench at 45–60°; 60° is the more upright,
 * conservative end (the arms hang less far behind the body).
 */
private const val INCLINE_CURL = 60.0

/** Sitting back on a bench whose back pad is [back]° from level: the pelvis touching the seat and the pad (as Chest.kt's). */
private fun reclinedPelvis(hinge: Point, back: Double): Point {
    val r = back / DEG
    val t = (Body.TORSO_RADIUS - Body.TORSO_RADIUS * cos(r)) / kotlin.math.sin(r)
    return hinge + unit(180.0 - back) * t + unit(90.0 - back) * Body.TORSO_RADIUS
}

/**
 * Incline DB curl. Sitting back on the bench at 60°, back and head on the pad, feet flat; the arms hanging straight
 * down, behind the body, palms in (in profile); END one arm curled with the upper arm still hanging straight down
 * (the elbow "back at your side"), the palm turned up (end-on). With the upper arm vertical the elbow's 145° leaves the
 * forearm 35° short of upright.
 */
private val inclineDbCurl: ExerciseFigure by lazy {
    val bench = AdjustableBench(x = 30.0, backAngle = INCLINE_CURL)
    val torso = 180.0 - INCLINE_CURL
    val pelvis = reclinedPelvis(adjustableHinge(bench), INCLINE_CURL)
    val legs = Reach.leg(pelvis, Point(pelvis.x + 46.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    // The upper arm straight down: at 270° on screen, so its shoulder angle is 270 - (torso + 180).
    val hangDown = Arm(shoulder = 270.0 - (torso + 180.0), elbow = 0.0)
    fun pose(near: Arm, far: Arm = near) = Pose(pelvis.x, pelvis.y, torso, nearArm = near, nearLeg = legs, farArm = far)
    figure(
        View.SIDE, pose(hangDown), pose(hangDown.copy(elbow = 145.0), hangDown), listOf(Floor, bench, palmsIn(Side.FAR), palmsIn(Side.NEAR)),
        endProps = listOf(Floor, bench, palmsIn(Side.FAR), Dumbbell(Side.NEAR)),
        startCaption = "Arms hang straight down",
        endCaption = "Palm faces your shoulder",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a bench to an incline and sit back against it.",
            "Hold a dumbbell in each hand with your arms hanging straight down and your palms facing in.",
            "Keep your elbow back at your side. Curl one dumbbell up, turning your palm up, until your forearm is upright and your palm faces your shoulder.",
        ),
        contacts = listOf(
            Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH), Contact(BodyPoint.HEAD, Surface.BACK_PAD, BOTH),
        ),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START), ElbowBend(140.0, 148.0, Which.END, Side.NEAR)),
    )
}

/**
 * The toe of a foot standing on the floor: the ankle's height that puts the foot capsule's toe end on the floor for a
 * foot at screen angle [foot] (as Back.kt's).
 */
private fun ankleOnToes(ankleX: Double, foot: Double): Point {
    val r = Body.FOOT_WIDTH / 2
    val sole = unit(foot - 90.0) * (Body.ANKLE_HEIGHT - r)
    val toe = sole + unit(foot) * (Segment.FOOT.length * 0.8 - r)
    return Point(ankleX, -r - toe.y)
}

/**
 * Spider curl. Face down on the approved chest-supported row's incline (30°, Back.kt), the chest on the pad, the pad
 * ending 5 cm past the chest so the shoulders are near its top and the arms hang over it; the legs trail behind on the
 * toes (knees bent 15°), as there. An EZ (curl) bar; START the arms hanging straight down; END curled as far as the
 * elbows bend (145°), the upper arms still vertical. ExRx barbell prone incline curl
 * (https://exrx.net/WeightExercises/Brachialis/BBProneInclineCurl).
 */
private val spiderCurl: ExerciseFigure by lazy {
    val angle = 30.0
    val low = Point(0.0, -62.0)
    val touch = low + unit(angle) * 36.0
    val bench = ProneBench(low = low, angle = angle, length = 41.0)
    val pelvis = touch - unit(angle) * (Segment.TORSO.length - Body.CHEST_BELOW_SHOULDER) + unit(angle + 90.0) * Body.CHEST_DEPTH
    val trail = sqrt(
        Segment.THIGH.length * Segment.THIGH.length + Segment.SHIN.length * Segment.SHIN.length +
            2 * Segment.THIGH.length * Segment.SHIN.length * cos(15.0 / DEG),
    )
    val ankle = ankleOnToes(0.0, foot = -55.0).let { a ->
        val dy = a.y - pelvis.y
        Point(pelvis.x - sqrt(trail * trail - dy * dy), a.y)
    }
    val legs = Reach.leg(pelvis, ankle, angle, foot = -55.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, angle, nearArm = arm, nearLeg = legs)
    val down = Arm(shoulder = 270.0 - (angle + 180.0), elbow = 0.0)
    figure(
        View.SIDE, pose(down), pose(down.copy(elbow = 145.0)), listOf(Floor, bench, EZBar),
        startCaption = "Arms hang straight down",
        endCaption = "As far as elbows bend",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie face down on an incline bench, with your shoulders near the top.",
            "Hold a curl bar with an underhand grip, about shoulder-width apart.",
            "Curl the bar up as far as your elbows bend.",
            LOWER_TO_STRAIGHT,
        ),
        contacts = listOf(Contact(BodyPoint.CHEST, Surface.CHEST_PAD, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(0.0, 1.0, Which.START), ElbowBend(140.0, 148.0, Which.END)),
    )
}

// ---- on an arm pad ----

/**
 * A seated body upright on a seat [seatHeight] up, its upper arms forward and down at [padAngle]° below level on an arm
 * pad whose top face is under them (the armpit 6 cm from the pad's top end, as "your armpit rests near the top of the
 * pad"); the feet flat 55 cm in front of the hips, so the thighs slope down under the pad and leave it in view (neutral).
 */
private class PadBody(val seatHeight: Double, val padAngle: Double) {
    val pelvis = Point(0.0, -(seatHeight + Body.TORSO_RADIUS))
    val shoulder = shoulderOf(pelvis, 90.0)
    val legs = Reach.leg(pelvis, Point(pelvis.x + 55.0, -Body.ANKLE_HEIGHT), 90.0, foot = 0.0)
    val padTop = shoulder + unit(-padAngle) * 6.0 + unit(-padAngle - 90.0) * (Body.UPPER_ARM_WIDTH / 2)
    val elbow = shoulder + unit(-padAngle) * Segment.UPPER_ARM.length

    /** The arm on the pad with the elbow bent [bend]°. */
    fun arm(bend: Double) = Arm(shoulder = 90.0 - padAngle, elbow = bend)

    fun pose(near: Arm, far: Arm = near) = Pose(pelvis.x, pelvis.y, 90.0, nearArm = near, nearLeg = legs, farArm = far)
}

private val PAD_CONTACTS = listOf(
    Contact(BodyPoint.HIPS, Surface.SEAT, BOTH), Contact(BodyPoint.ELBOWS, Surface.ARM_PAD, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
)

/**
 * The preacher bench's pad: 45° below level. ExRx's standing preacher curl on an incline bench
 * (https://exrx.net/WeightExercises/Brachialis/DBStandingPreacherCurlInclineBench) sets the pad at 45–55° when no
 * preacher bench is to hand; 45° is used for the bench itself (neutral otherwise). Seat 55 cm up (neutral).
 */
private val preacher by lazy { PadBody(seatHeight = 55.0, padAngle = 45.0) }

/** The hips sit at the back of the seat, so the thighs slope down clear of its front edge. */
private val preacherBench by lazy { PreacherBench(seatX = -10.0, seatHeight = 55.0, padTop = preacher.padTop, padAngle = 45.0) }

/** The preacher's top: the forearm upright, 90° above the pad's 45° slope down: a 135° bend. */
private const val PREACHER_TOP = 135.0

/**
 * Preacher curl. Seated at the preacher bench (KitArms.kt PreacherBench), the backs of the upper arms on the pad, an EZ
 * bar (the guide's "the bar"; EZ is the usual preacher bar); START lowered to straight arms along the pad; END curled
 * until the forearms are upright. ExRx barbell preacher curl (https://exrx.net/WeightExercises/Brachialis/BBPreacherCurl).
 */
private val preacherCurl: ExerciseFigure by lazy {
    figure(
        View.SIDE, preacher.pose(preacher.arm(0.0)), preacher.pose(preacher.arm(PREACHER_TOP)), listOf(Floor, preacherBench, EZBar),
        startCaption = "Arms straight on the pad",
        endCaption = "Forearms upright",
        thumbnail = Which.END,
        guideLines = listOf(
            PAD_SEAT,
            "Sit with the backs of your upper arms on the pad.",
            "Curl the bar up until your forearms are upright.",
            LOWER_TO_STRAIGHT,
            ARM_ON_PAD,
        ),
        contacts = PAD_CONTACTS,
        checks = listOf(ElbowBend(0.0, 1.0, Which.START), ElbowBend(PREACHER_TOP - 1.0, PREACHER_TOP + 1.0, Which.END)),
    )
}

/**
 * DB preacher curl. The preacher curl with one dumbbell in the near hand, palm up (end-on); the free arm rests
 * straight on the pad in both panels.
 */
private val dbPreacherCurl: ExerciseFigure by lazy {
    val rest = preacher.arm(0.0)
    figure(
        View.SIDE, preacher.pose(rest), preacher.pose(preacher.arm(PREACHER_TOP), rest), listOf(Floor, preacherBench, Dumbbell(Side.NEAR)),
        startCaption = "Arm straight on the pad",
        endCaption = "Forearm upright",
        thumbnail = Which.END,
        guideLines = listOf(
            PAD_SEAT,
            "Sit with the back of your working arm on the pad, holding the dumbbell with your palm up.",
            "Curl the dumbbell up until your forearm is upright.",
            "Lower under control until your arm is straight.",
            ARM_ON_PAD,
        ),
        contacts = PAD_CONTACTS,
        checks = listOf(ElbowBend(0.0, 1.0, Which.START, Side.NEAR), ElbowBend(PREACHER_TOP - 1.0, PREACHER_TOP + 1.0, Which.END, Side.NEAR)),
    )
}

/** An arm machine for [body]: its pad under the upper arms, the lever's pivot at the elbows, the stack 20 cm past the straight arm's hands. */
private fun armMachine(body: PadBody) = ArmPadMachine(
    seatX = -12.0, seatHeight = body.seatHeight, padTop = body.padTop, padAngle = body.padAngle, pivot = body.elbow,
    stackX = body.elbow.x + FORE_LINK + 20.0,
)

/**
 * Machine curl. Seated at a curl machine (KitArms.kt ArmPadMachine), the arms over its pad at 40° (neutral), the
 * elbows on the lever's pivot; START the arms nearly straight (15°, the guide's "nearly straight", neutral); END the
 * handles curled toward the chest until the forearms are upright (130°). ExRx lever preacher curl
 * (https://exrx.net/WeightExercises/Brachialis/LVPreacherCurlH), ACE seated biceps curl
 * (https://www.acefitness.org/resources/everyone/exercise-library/184/seated-biceps-curl/).
 */
private val machineCurl: ExerciseFigure by lazy {
    val body = PadBody(seatHeight = 50.0, padAngle = 40.0)
    figure(
        View.SIDE, body.pose(body.arm(15.0)), body.pose(body.arm(130.0)), listOf(Floor, armMachine(body)),
        startCaption = "Arms nearly straight",
        endCaption = "Curl toward your chest",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so the middle of your elbows lines up with the machine's pivot.",
            "Sit with your arms over the pad. Grip the handles firmly, with your wrists straight.",
            "Breathe out and curl the handles toward your chest.",
            "Breathe in and lower slowly until your arms are nearly straight.",
        ),
        contacts = PAD_CONTACTS,
        checks = listOf(ElbowBend(10.0, 20.0, Which.START), ElbowBend(125.0, 135.0, Which.END)),
    )
}

/**
 * Machine triceps extension. Seated at the extension machine, the backs of the upper arms on its pad at 35° below
 * level (neutral), the elbows on the pivot; START the forearms folded back to the upper arms (145°); END the handles
 * pushed down until the arms are straight. ExRx lever triceps extension (https://exrx.net/WeightExercises/Triceps/LVTriExt).
 */
private val machineTricepsExtension: ExerciseFigure by lazy {
    val body = PadBody(seatHeight = 50.0, padAngle = 35.0)
    figure(
        View.SIDE, body.pose(body.arm(145.0)), body.pose(body.arm(0.0)), listOf(Floor, armMachine(body)),
        startCaption = "Forearms to upper arms",
        endCaption = "Arms straight",
        thumbnail = Which.START,
        guideLines = listOf(
            "Set the seat so the backs of your upper arms rest on the pad.",
            "Sit and grip the handles. Line up your elbows with the machine's pivot.",
            "Push the handles down until your arms are straight.",
            "Return under control until your forearms touch your upper arms.",
        ),
        contacts = PAD_CONTACTS,
        checks = listOf(ElbowBend(140.0, 148.0, Which.START), ElbowBend(0.0, 1.0, Which.END)),
    )
}

// ---- pushdowns ----

/**
 * A high pulley in front of the lifter, who faces it and stands close: the column 60 cm in front, the pulley 195 cm up
 * ("above head height") on an arm reaching 22 cm towards the lifter, so the hands stay clear of the column (neutral).
 */
private val highPulleyInFront = CableStack(x = 60.0, pulleyHeight = 195.0, reach = -22.0)

/**
 * The pushdowns' arms: the upper arm at the side (3° forward of hanging, neutral), START the elbow bent 100° (the
 * forearm 13° above level: "about level" is a neutral value; ExRx's pushdown, https://exrx.net/WeightExercises/Triceps/CBPushdown,
 * returns until the forearm is close to the upper arm, and the guides stop before the elbows move forward); END 12°
 * short of straight ("nearly straight", neutral).
 */
private val PUSH_TOP = Arm(shoulder = 3.0, elbow = 100.0)
private val PUSH_DOWN = Arm(shoulder = 3.0, elbow = 12.0)

private val PUSHDOWN_CHECKS = listOf(
    UpperArmDown(6.0, Which.START), UpperArmDown(6.0, Which.END), ElbowBend(5.0, 20.0, Which.END, Side.NEAR),
)

/** Triceps pushdown. Facing the high pulley, a short straight bar in both hands. ExRx cable pushdown (URL above). */
private val tricepsPushdown: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(PUSH_TOP), stand(PUSH_DOWN), listOf(Floor, highPulleyInFront, CableAttachment(Attachment.STRAIGHT_BAR)),
        startCaption = "Elbows at your sides",
        endCaption = "Arms nearly straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Attach a short bar or V-bar to the high pulley, above head height.",
            "Face the pulley and stand close to it. Hold the bar with a narrow grip, palms down.",
            "Tuck your elbows in at your sides.",
            "Push the bar down until your arms are nearly straight. Keep your elbows at your sides.",
        ),
        contacts = FEET_DOWN,
        checks = PUSHDOWN_CHECKS,
    )
}

/** Rope pushdown. The pushdown with a rope (ExRx, https://exrx.net/WeightExercises/Triceps/CBPushdownRope). */
private val ropePushdown: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(PUSH_TOP), stand(PUSH_DOWN), listOf(Floor, highPulleyInFront, CableAttachment(Attachment.ROPE)),
        startCaption = "Tuck your elbows in",
        endCaption = "Push the rope down",
        thumbnail = Which.END,
        guideLines = listOf(
            "Attach a rope to the high pulley, above head height.",
            "Tuck your elbows in at your sides.",
            "Push the rope down until your arms are nearly straight. Turn your palms down at the bottom.",
        ),
        contacts = FEET_DOWN,
        checks = PUSHDOWN_CHECKS,
    )
}

/**
 * One-arm pushdown. One handle in the near hand, palm up, the free arm hanging (ExRx cable one-arm pushdown,
 * https://exrx.net/WeightExercises/Triceps/CBOneArmPushdown).
 */
private val oneArmPushdown: ExerciseFigure by lazy {
    figure(
        View.SIDE, stand(PUSH_TOP, HANG), stand(PUSH_DOWN, HANG), listOf(Floor, highPulleyInFront, CableHandle(Side.NEAR)),
        startCaption = "Elbow at your side",
        endCaption = "Arm nearly straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Attach a single handle to the high pulley.",
            "Keep your elbow at your side.",
            "Push the handle down until your arm is nearly straight.",
        ),
        contacts = FEET_DOWN,
        checks = PUSHDOWN_CHECKS,
    )
}

// ---- overhead extensions ----

/**
 * Cable overhead triceps extension. Facing away from a low pulley behind (the column 75 cm behind, the pulley 15 cm
 * up; neutral), staggered stance, a rope in both hands; the upper arms beside the head, elbows pointing up (165° of
 * flexion, 15° forward of vertical, neutral); START the forearms folded down behind the head to the upper arms (145°);
 * END straightened overhead. The cable runs from the pulley up behind the back to the rope. ExRx cable standing
 * triceps extension (https://exrx.net/WeightExercises/Triceps/CBStandingTricepsExtensionRope).
 */
private val cableOverheadTricepsExtension: ExerciseFigure by lazy {
    val st = stagger()
    figure(
        View.SIDE, st.pose(Arm(165.0, 145.0)), st.pose(Arm(165.0, 0.0)),
        listOf(Floor, CableStack(x = -75.0, pulleyHeight = 15.0, reach = 12.0), CableAttachment(Attachment.ROPE)),
        startCaption = "Rope behind your head",
        endCaption = "Arms straight overhead",
        thumbnail = Which.START,
        guideLines = listOf(
            "Hold the rope just under its knobbed ends. Turn away from the machine and stand with one foot in front of the other.",
            "Bring the rope up behind your head, with your elbows pointing up.",
            "Straighten your arms to raise the rope overhead.",
            "Lower under control until your forearms touch your upper arms.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(140.0, 148.0, Which.START), ElbowBend(0.0, 1.0, Which.END)),
    )
}

/**
 * DB overhead triceps extension. Staggered stance, one dumbbell in both hands under its top plate, hanging from the
 * palms (KitArms.kt OverheadDumbbell); the upper arms upright (175°, beside the head); START pressed up with straight
 * arms; END lowered behind the head until the forearms are level (a 95° bend), the dumbbell clear of the head.
 * ACE triceps extension (https://www.acefitness.org/resources/everyone/exercise-library/74/triceps-extension/), ExRx
 * dumbbell triceps extension (https://exrx.net/WeightExercises/Triceps/DBTriExt).
 */
private val dbOverheadTricepsExtension: ExerciseFigure by lazy {
    val st = stagger()
    figure(
        View.SIDE, st.pose(Arm(175.0, 0.0)), st.pose(Arm(175.0, 95.0)), listOf(Floor, OverheadDumbbell),
        startCaption = "Arms upright overhead",
        endCaption = "Forearms about level",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand with one foot a little in front of the other. Hold one dumbbell with both hands.",
            "Press the dumbbell overhead until your arms are upright.",
            "Breathe in and bend your elbows to lower the dumbbell slowly behind your head. Keep your upper arms still.",
            "Stop when your forearms are about level with the floor. Don't let the dumbbell touch your head.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(0.0, 1.0, Which.START), ElbowBend(90.0, 100.0, Which.END)),
    )
}

// ---- lying extensions ----

/**
 * A spotter half-kneeling at the head end of the bench, the hands near the lifter's wrists without holding them: the
 * guide's "Use a spotter." for a weight moved over the face (as the DB pullover's spotter, Back.kt; ACE's chest press
 * and lying chest fly pages place the spotter so, https://www.acefitness.org/resources/everyone/exercise-library/19/chest-press/).
 */
private fun spotter(p: Pose): Spotter {
    val s = p.skeleton()
    val kneel = Leg(hip = 10.0, knee = 90.0, ankle = -45.0)
    val front = Leg(hip = 100.0, knee = 90.0, ankle = 0.0)
    val pelvisY = -(Segment.THIGH.length + Body.SHIN_WIDTH / 2)
    val sp = Pose(s.head.x - 45.0, pelvisY, 80.0, nearArm = HANG, nearLeg = front, farLeg = kneel)
    val tsh = sp.skeleton().shoulder
    val target = s.near.wrist + Point(-8.0, 6.0)
    val reach = if ((target - tsh).length() < ARM_REACH - 1.0) armTo(tsh, target, 80.0) else straightArm(tsh, target, 80.0)
    return Spotter(sp.copy(nearArm = reach, farArm = reach))
}

/** The upper arm straight up from the shoulder, lying face up ([FLAT_TORSO] 180°): a 90° shoulder angle. */
private const val UPRIGHT = 90.0

/** The first bend (from [from], in 0.5° steps) where [done] holds for the upright arm lying on the bench. */
private fun bendUntil(from: Double, done: (Skeleton) -> Boolean): Double {
    var e = from
    while (e < MAX_ELBOW && !done(flatPose(Arm(UPRIGHT, e)).skeleton())) e += 0.5
    return e
}

/**
 * Skull crusher. Lying on the flat bench as the Bench (Chest.kt's lying pose), an EZ bar; START the arms straight but
 * not locked (5°), the upper arms upright; END the elbows bent, the upper arms still upright, until the bar is just
 * above the forehead (3 cm clear of the head). A spotter at the head (the guide's safety line). ACE lying barbell
 * triceps extension (https://www.acefitness.org/resources/everyone/exercise-library/36/lying-barbell-triceps-extensions/),
 * ExRx skull crusher (https://exrx.net/WeightExercises/Triceps/BBLyingTriExtSC).
 */
private val skullCrusher: ExerciseFigure by lazy {
    val start = flatPose(Arm(UPRIGHT, 5.0))
    val e = bendUntil(5.0) { s -> (s.near.grip - s.head).length() - Body.HEAD_RADIUS - Barbell.SHAFT_RADIUS <= 3.0 }
    val end = flatPose(Arm(UPRIGHT, e))
    figure(
        View.SIDE, start, end, listOf(Floor, spotter(start), FlatBench(), EZBar),
        endProps = listOf(Floor, spotter(end), FlatBench(), EZBar),
        startCaption = "Straight, not locked",
        endCaption = "Just above your forehead",
        thumbnail = Which.END,
        guideLines = listOf(
            OVER_FACE_SPOT,
            "Lie back on the bench with your feet flat on the floor.",
            "Press the bar up over your chest until your arms are straight but not locked, so the bar is above your face. Keep your wrists straight.",
            "Breathe in and bend your elbows to lower the bar slowly toward your forehead. Keep your upper arms upright.",
            "Stop when the bar is just above your forehead, or sooner if your upper arms start to drift back.",
        ),
        contacts = LYING_CONTACTS,
        checks = listOf(ElbowBend(3.0, 8.0, Which.START)),
    )
}

/**
 * DB lying triceps extension. Lying as the skull crusher, a dumbbell in each hand (the guide gives no grip, so the
 * usual end-on drawing); START the arms straight up over the shoulders; END the elbows bent until the dumbbells are
 * beside the head: their centres at the head's centre, front to back, and 2 cm above it. The guide does not keep the
 * upper arms upright here, and with the forearm's fixed length the dumbbells can only reach beside the head with the
 * upper arms tipped back towards it (the elbows pointing up and a little back). A spotter at the head. ExRx dumbbell
 * lying triceps extension (https://exrx.net/WeightExercises/Triceps/DBLyingTriExt).
 */
private val dbLyingTricepsExtension: ExerciseFigure by lazy {
    val start = flatPose(Arm(UPRIGHT, 0.0))
    val bare = flatPose(HANG).skeleton()
    val end = flatPose(Reach.arm(bare.shoulder, bare.head + Point(0.0, -2.0), FLAT_TORSO))
    val dbs = listOf(Dumbbell(Side.FAR), Dumbbell(Side.NEAR))
    figure(
        View.SIDE, start, end, listOf(Floor, spotter(start), FlatBench()) + dbs,
        endProps = listOf(Floor, spotter(end), FlatBench()) + dbs,
        startCaption = "Above your shoulders",
        endCaption = "Beside your head",
        thumbnail = Which.END,
        guideLines = listOf(
            OVER_FACE_SPOT,
            "Lie on a flat bench with your feet flat on the floor.",
            "Start with your arms straight and the dumbbells held up above your shoulders, not over your face.",
            "Bend your elbows to lower the dumbbells slowly until they are beside your head.",
        ),
        contacts = LYING_CONTACTS,
        checks = listOf(LockoutOverShoulders(Which.START)),
    )
}

/**
 * JM press. Lying on the bench in the rack as the Bench, the pins under the chest (the guide's safety line); START the
 * bar held over the shoulders with straight arms; END lowered with the elbows high, the upper arms pointing up and
 * towards the feet at 30° above level (neutral), until the forearms press against the upper arms (the elbow at the
 * figure's working limit, 148°). With the fixed lengths the bar then stops over the shoulder joints' line, between the
 * chin and the upper chest (the guide's cue), about 17 cm above the body's axis. ExRx JM press
 * (https://exrx.net/WeightExercises/Triceps/BBJMPress).
 */
private val jmPress: ExerciseFigure by lazy {
    // Lying face up (180°): an upper arm pointing 30° above level towards the feet (+x) is a 30° shoulder angle.
    val down = Arm(shoulder = 30.0, elbow = MAX_ELBOW)
    val props = listOf(Floor, FlatBench(), PowerRack(x = 2.0, jHookHeight = 110.0, safetyHeight = 60.0), Barbell())
    figure(
        View.SIDE, flatPose(Arm(UPRIGHT, 0.0)), flatPose(down), props,
        startCaption = "Over your shoulders",
        endCaption = "Forearms on upper arms",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Lie on the bench with your feet flat on the floor.",
            "Lift the bar out and hold it over your shoulders with straight arms.",
            "Lower the bar toward your upper chest, keeping your elbows high.",
            "Stop when your forearms press against your upper arms.",
        ),
        contacts = LYING_CONTACTS,
        checks = listOf(LockoutOverShoulders(Which.START), ElbowBend(MAX_ELBOW - 1.0, MAX_ELBOW, Which.END)),
    )
}

// ---- kickback ----

/**
 * The kickback's lean: the torso 25° above level (neutral; the guide says "lean forward"). ACE's triceps kickback
 * (https://www.acefitness.org/resources/everyone/exercise-library/55/triceps-kickback/) keeps the upper arm parallel and
 * close to the torso, ExRx's (https://exrx.net/WeightExercises/Triceps/DBKickback) parallel to the floor; the upper arm
 * is drawn between the two, 12.5° up from the torso's line. ACE's elbow starts at 90° or at least with the forearm
 * hanging straight down: the forearm hangs vertical.
 */
private const val KICK_TORSO = 25.0

/**
 * Triceps kickback. Staggered stance (the near, working side's foot back), leaning forward, the free (far) hand on the
 * front thigh by the knee; the working upper arm close to the side; START the forearm hanging straight down; END the
 * elbow straightened to move the dumbbell back, the upper arm still. The guide gives no grip: the usual end-on drawing.
 */
private val tricepsKickback: ExerciseFigure by lazy {
    val pelvis = Point(0.0, -85.0)
    val back = Reach.leg(pelvis, Point(-32.0, -Body.ANKLE_HEIGHT), KICK_TORSO, foot = 0.0)
    val front = Reach.leg(pelvis, Point(22.0, -Body.ANKLE_HEIGHT), KICK_TORSO, foot = 0.0)
    val upper = -KICK_TORSO / 2
    val hanging = Arm(shoulder = upper, elbow = 270.0 - (KICK_TORSO + 180.0 + upper))
    val bare = Pose(pelvis.x, pelvis.y, KICK_TORSO, nearArm = hanging, nearLeg = back, farLeg = front).skeleton()
    // The free hand on the top of the front thigh, a hand's width short of the knee.
    val k = bare.far.knee
    val h = bare.far.hip
    val d = (k - h).let { it * (1.0 / it.length()) }
    val handOnThigh = k - d * 8.0 + Point(d.y, -d.x) * (Body.THIGH_WIDTH / 2 + Body.HAND_WIDTH / 2)
    val free = armTo(bare.shoulder, handOnThigh, KICK_TORSO)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, KICK_TORSO, nearArm = arm, nearLeg = back, farArm = free, farLeg = front)
    figure(
        View.SIDE, pose(hanging), pose(Arm(upper, 0.0)), listOf(Floor, Dumbbell(Side.NEAR)),
        startCaption = "Forearm hangs straight",
        endCaption = "Straighten your elbow",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand with one foot in front of the other, holding a dumbbell in one hand.",
            "Brace your abs and lean forward. Rest your free hand on your thigh or knee.",
            "Hold your upper arm close to your side. Bend your elbow so your forearm hangs straight down.",
            "Straighten your elbow to move the dumbbell back. Keep your upper arm still.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(0.0, 1.0, Which.END, Side.NEAR)),
    )
}

// ---- dip and push-ups ----

/**
 * Triceps dip. Parallel bars (the chest dip's, 120 cm); START pushed up on straight arms, the shoulders over the hands,
 * the torso upright (3° forward), the hips nearly straight (10°) and the knees bent back 45° (neutral; the chest dip's
 * legs, with the hips straighter), clear of the floor; END
 * lowered by bending the arms, staying upright (the torso 10° forward, against the chest dip's 32°), the elbows back,
 * in the plane (not flared), the forearms leaning back 20° (neutral).
 * - Depth: the guide stops at the upper arms level. An upright torso with the upper arms level would take the shoulder
 *   90° behind the body, past the figure's 60° extension limit (Body.kt, PMC13392859), so the figure stops at that
 *   limit: the upper arms 22° short of level, the conservative side of the guide's "no deeper".
 * ExRx weighted triceps dip (https://exrx.net/WeightExercises/Triceps/WtTriDip): shoulders above the hands at the top.
 */
private val tricepsDip: ExerciseFigure by lazy {
    val bars = DipBars(x = 0.0)
    val grip = Point(0.0, -bars.height)
    val leg = Leg(hip = 10.0, knee = 45.0, ankle = -25.0)
    val topTorso = 87.0
    val topShoulder = grip + Point(0.0, -ARM_REACH)
    val topPelvis = topShoulder - unit(topTorso) * Segment.TORSO.length
    val start = Pose(topPelvis.x, topPelvis.y, topTorso, nearArm = Arm(fold180(270.0 - topTorso - 180.0), 0.0), nearLeg = leg)
    val torso = 80.0
    val elbow = grip + unit(90.0 + 20.0) * FORE_LINK
    val shortOfLevel = 22.0
    val shoulder = elbow + unit(shortOfLevel) * Segment.UPPER_ARM.length
    val pelvis = shoulder - unit(torso) * Segment.TORSO.length
    val upper = angleTo(elbow - shoulder)
    val fore = angleTo(grip - elbow)
    val bottom = Arm(shoulder = fold180(upper - torso - 180.0), elbow = fold180(fore - upper))
    val end = Pose(pelvis.x, pelvis.y, torso, nearArm = bottom, nearLeg = leg.copy(hip = leg.hip + topTorso - torso))
    figure(
        View.SIDE, start, end, listOf(Floor, bars),
        startCaption = "Shoulders over hands",
        endCaption = "Lower, stay upright",
        thumbnail = Which.END,
        guideLines = listOf(
            "Grip shoulder-width bars and push up until your arms are straight, with your shoulders over your hands.",
            "Keep your hips straight, so your body stays upright.",
            "Lower yourself by bending your arms. Stay upright.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.DIP_BAR, BOTH)),
        checks = listOf(ShouldersOverHands(5.0, Which.START), ElbowBend(0.0, 2.0, Which.START)),
    )
}

/**
 * A close push-up's two poses, built as Chest.kt's push-up: START on the toes with straight arms, the body straight,
 * the hands on the floor [handsOut] cm out from the body's middle and the arms along [armAngle]° (world; -90 is
 * straight down); END lowered, the hands and feet staying put, until the chest is [gap] cm off the floor, the elbows
 * bending towards [hint].
 */
private fun closePushUp(handsOut: Double, armAngle: Double, hint: V3, gap: Double): Pair<Pose, Pose> {
    // The hand bent the push-up's 60° from the forearm, its fingers on the floor (Chest.kt's PUSH_UP_WRIST).
    val hand = armAngle + PUSH_UP_WRIST
    val armLen = Segment.UPPER_ARM.length + Segment.FOREARM.length
    val zOut = handsOut - Body.SHOULDER_HALF_WIDTH
    val out = asin(zOut / armLen) * DEG
    val armDrawn = sqrt(armLen * armLen - zOut * zOut)
    fun topArm(torso: Double) = Arm(fold180(armAngle - torso - 180.0), 0.0, wrist = fold180(hand - armAngle), out = out, forearmOut = out)
    fun topWrist(torso: Double): Point = pronePose(torso, Support.TOES, 0.0, topArm(torso)).skeleton().shoulder + unit(armAngle) * armDrawn
    val scale = cos(out / DEG)
    val top = solveAngle(-40.0, 60.0) { t -> -handBottom(topWrist(t), hand, scale) }
    val start = pronePose(top, Support.TOES, 0.0, topArm(top))
    val w = topWrist(top)
    fun low(t: Double) = pronePose(t, Support.TOES, 0.0, SIDE_ARMS_DOWN)
    val t = solveAngle(top - 40.0, top) { a -> -low(a).skeleton().point(BodyPoint.CHEST).y - gap }
    val lowPose = low(t)
    val bent = pushArm(lowPose, V3(w.x, w.y, handsOut), hint, 0.0, flat = false)
    return start to lowPose.copy(nearArm = bent, farArm = bent)
}

private val PUSH_UP_CONTACTS = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HANDS, Surface.FLOOR, BOTH))

/**
 * Close-grip push-up. The push-up with the hands under the shoulders (at the shoulder joints' width, so the arms lie
 * in the picture's plane, straight down), the elbows bending back along the body; END the chest lowered to 6 cm off
 * the floor (the Push-up's depth). ExRx close-grip push-up (https://exrx.net/WeightExercises/Triceps/BWCloseGripPushup).
 */
private val closeGripPushUp: ExerciseFigure by lazy {
    val (start, end) = closePushUp(Body.SHOULDER_HALF_WIDTH, -90.0, V3(-1.0, -0.8, 0.15), CHEST_GAP)
    figure(
        View.SIDE, start, end, listOf(Floor),
        startCaption = "Hands under shoulders",
        endCaption = "Lower toward the floor",
        thumbnail = Which.START,
        guideLines = listOf(
            "Place your hands on the floor under your shoulders, or a little narrower.",
            "Push up onto your toes with straight arms.",
            "Bend your arms to lower your body toward the floor. Keep your body straight.",
        ),
        contacts = PUSH_UP_CONTACTS,
        checks = listOf(ChestOffFloor(4.0, 9.0, Which.END), ElbowBend(0.0, 2.0, Which.START)),
    )
}

/**
 * The triangle push-up's straight arms at the top: [TRIANGLE_ARM]° on screen, 7° back from straight down towards the
 * feet. The body pivots on the toes as it lowers, so the chest moves towards the head; at 7° the hands are under the
 * chest at the top and still under it at the bottom (within 6 cm both times; neutral).
 */
private const val TRIANGLE_ARM = -97.0

/**
 * Triangle push-up. The hands together under the chest: at the body's middle (18 cm in from each shoulder joint) and
 * [TRIANGLE_ARM], back from straight down towards the feet; the elbows bending out and back.
 * The chest stops 10 cm off the floor, above the hands (neutral; "toward the floor"). ACE's triceps study
 * (https://www.acefitness.org/certifiednewsarticle/3008/ace-study-identifies-best-triceps-exercises/).
 */
private val trianglePushUp: ExerciseFigure by lazy {
    val (start, end) = closePushUp(0.0, TRIANGLE_ARM, V3(-0.5, -1.0, 0.8), 10.0)
    figure(
        View.SIDE, start, end, listOf(Floor),
        startCaption = "Hands under your chest",
        endCaption = "Chest toward the floor",
        thumbnail = Which.START,
        guideLines = listOf(
            "Place your hands together on the floor under your chest, so your thumbs and index fingers make a triangle.",
            "Push up onto your toes with straight arms.",
            "Bend your elbows to lower your chest toward the floor. Keep your body rigid.",
        ),
        contacts = PUSH_UP_CONTACTS,
        checks = listOf(ChestOffFloor(8.0, 13.0, Which.END), ElbowBend(0.0, 2.0, Which.START)),
    )
}

/** The Arms area's figures: each map entry is lazy, so listing the map builds none (Figures.kt). */
internal val armFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf(
        "JM press" to lazy { jmPress },
        "Curl" to lazy { curl },
        "EZ-bar curl" to lazy { ezBarCurl },
        "DB curl" to lazy { dbCurl },
        "Seated DB curl" to lazy { seatedDbCurl },
        "Hammer curl" to lazy { hammerCurl },
        "Cable hammer curl" to lazy { cableHammerCurl },
        "Incline DB curl" to lazy { inclineDbCurl },
        "Preacher curl" to lazy { preacherCurl },
        "DB preacher curl" to lazy { dbPreacherCurl },
        "Machine curl" to lazy { machineCurl },
        "Cable curl" to lazy { cableCurl },
        "One-arm cable curl" to lazy { oneArmCableCurl },
        "Concentration curl" to lazy { concentrationCurl },
        "Drag curl" to lazy { dragCurl },
        "Spider curl" to lazy { spiderCurl },
        "Reverse curl" to lazy { reverseCurl },
        "Triceps pushdown" to lazy { tricepsPushdown },
        "Rope pushdown" to lazy { ropePushdown },
        "One-arm pushdown" to lazy { oneArmPushdown },
        "Cable overhead triceps extension" to lazy { cableOverheadTricepsExtension },
        "DB overhead triceps extension" to lazy { dbOverheadTricepsExtension },
        "Skull crusher" to lazy { skullCrusher },
        "DB lying triceps extension" to lazy { dbLyingTricepsExtension },
        "Machine triceps extension" to lazy { machineTricepsExtension },
        "Triceps kickback" to lazy { tricepsKickback },
        "Triceps dip" to lazy { tricepsDip },
        "Close-grip push-up" to lazy { closeGripPushUp },
        "Triangle push-up" to lazy { trianglePushUp },
        "Wrist curl" to lazy { wristCurl },
        "Reverse wrist curl" to lazy { reverseWristCurl },
    )
}
