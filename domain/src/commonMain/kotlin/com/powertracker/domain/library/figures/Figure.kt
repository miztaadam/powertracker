package com.powertracker.domain.library.figures

/** Which way the camera looks: from the lifter's side (sagittal plane) or from in front (frontal plane). */
enum class View { SIDE, FRONT }

/** The two panels: the start position and the end position. */
enum class Which { START, END }

/** One pose and the kit around it. Held pieces follow from the pose; fixed pieces stand in the scene. */
data class Scene(val pose: Pose, val props: List<Prop>)

/** Named places on the body that contacts and key checks refer to. Plural points mean both sides. */
enum class BodyPoint { HEAD, CHEST, UPPER_BACK, HIPS, THIGHS, KNEES, FEET, NEAR_FOOT, FAR_FOOT, HANDS, NEAR_HAND, FAR_HAND }

/**
 * A surface of the kit that the body rests on, pushes or holds. A [grip] surface is held: the hands wrap around it,
 * so a hand contact there is measured from the grip point and may sit inside it.
 */
enum class Surface(val grip: Boolean = false) { FLOOR, BENCH, SEAT, THIGH_PAD, BACK_PAD, FOOT_PLATE, PULL_UP_BAR(grip = true) }

/** [point] rests on [surface] (within the contact tolerance) in the panels in [at]. */
data class Contact(val point: BodyPoint, val surface: Surface, val at: Set<Which>)

/** A key position the guide names, confirmed by [FigureChecks]. */
sealed interface KeyCheck {
    val at: Which
}

/** The bar touches [point] (bench press: the chest at the bottom). */
data class BarTouches(val point: BodyPoint, override val at: Which = Which.START) : KeyCheck

/** Elbows locked with the bar straight over the shoulders (bench press and strict press lockout). */
data class LockoutOverShoulders(override val at: Which = Which.END) : KeyCheck

/** The bar is over the middle of the foot (deadlift and squat balance). */
data class BarOverMidfoot(override val at: Which = Which.START) : KeyCheck

/** The hip joint is at least [belowKnee] cm lower than the knee joint (squat depth as the guide gives it). */
data class HipDepth(val belowKnee: Double, override val at: Which = Which.END) : KeyCheck

/** One exercise's two-pose picture. Keyed by catalogue name in [Figures]. */
data class ExerciseFigure(
    val view: View,
    val start: Scene,
    val end: Scene,
    /** At most 24 characters, in the guide's wording. */
    val startCaption: String,
    val endCaption: String,
    /** The more telling pose, shown as the Exercises tab thumbnail. */
    val thumbnail: Which,
    /** The guide's setup and steps lines this picture shows, verbatim. */
    val guideLines: List<String>,
    val contacts: List<Contact>,
    val checks: List<KeyCheck>,
) {
    fun scene(which: Which): Scene = if (which == Which.START) start else end
}

/** A body probe: the capsule from [a] to [b] with [radius] (a circle when a == b). */
internal data class Probe(val label: String, val a: Point, val b: Point, val radius: Double, val handGrip: Point? = null)

/** The probes for [point]: one, or one per side for the plural points. */
internal fun Skeleton.probes(point: BodyPoint): List<Probe> {
    fun foot(side: Side) = side(side).let { Probe("${side.label} foot", it.heel, it.toe, Body.FOOT_WIDTH / 2) }
    fun hand(side: Side) = side(side).let { Probe("${side.label} hand", it.wrist, it.hand, Body.HAND_WIDTH / 2, it.grip) }
    return when (point) {
        BodyPoint.HEAD -> listOf(Probe("head", head, head, Body.HEAD_RADIUS))
        BodyPoint.CHEST -> chest().let { listOf(Probe("chest", it, it, 0.0)) }
        BodyPoint.UPPER_BACK -> onTorso(Segment.TORSO.length - 8.0, 0.0).let { listOf(Probe("upper back", it, it, Body.TORSO_RADIUS)) }
        BodyPoint.HIPS -> listOf(Probe("hips", pelvis, pelvis, Body.TORSO_RADIUS))
        BodyPoint.THIGHS -> Side.entries.map { s -> side(s).let { Probe("${s.label} thigh", it.hip, it.knee, Body.THIGH_WIDTH / 2) } }
        BodyPoint.KNEES -> Side.entries.map { s -> side(s).knee.let { Probe("${s.label} knee", it, it, Body.SHIN_WIDTH / 2) } }
        BodyPoint.FEET -> Side.entries.map(::foot)
        BodyPoint.NEAR_FOOT -> listOf(foot(Side.NEAR))
        BodyPoint.FAR_FOOT -> listOf(foot(Side.FAR))
        BodyPoint.HANDS -> Side.entries.map(::hand)
        BodyPoint.NEAR_HAND -> listOf(hand(Side.NEAR))
        BodyPoint.FAR_HAND -> listOf(hand(Side.FAR))
    }
}

internal val Side.label: String get() = name.lowercase()

/** The chest touch point: on the front of the torso, [Body.CHEST_BELOW_SHOULDER] below the shoulder joint. */
internal fun Skeleton.chest(): Point = onTorso(Segment.TORSO.length - Body.CHEST_BELOW_SHOULDER, Body.CHEST_DEPTH)

/** A representative point for [point] (the near side's for the plural points). */
fun Skeleton.point(point: BodyPoint): Point = probes(point).first().let { (it.a + it.b) * 0.5 }
