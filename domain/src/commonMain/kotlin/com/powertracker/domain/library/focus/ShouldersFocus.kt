package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.Sources

// Each line is worked out from the exercise's own guide (areas/Shoulders.kt): the stretch where the guide's lowering
// or return ends, never further; the squeeze at the top the guide names, never higher. A line names the exercise's
// first muscle: front delts for the presses and front raises, side delts for the lateral raises, rear delts for the
// rear delt work, the rotator cuff for the rotations. The guide's pages come first, then the idea's sources (added by
// [lines]). Where a guide page goes further than the guide (ExRx's front raises go above horizontal, its seated
// military press lowers to the upper chest), the line follows the guide and cites a page that stops where it does.

private const val FRONT_STRAIGHT_ARMS = "Squeeze your front delts as you press to straight arms."
private const val FRONT_STRAIGHT_ARM = "Squeeze your front delts as you press to a straight arm."
// The lateral and rear raise guides raise the elbows to shoulder height, not the dumbbells, and keep the elbows bent.
private const val SIDE_DOWN = "Your side delts stretch as your arms lower to your sides."
private const val SIDE_ELBOWS_UP = "Squeeze your side delts as your elbows reach shoulder height."
// The front raise guides stop at shoulder height; higher is a mistake.
private const val FRONT_ARMS_UP = "Squeeze your front delts as your arms reach shoulder height."
private const val REAR_DOWN = "Your rear delts stretch as you lower the dumbbells back down."
private const val REAR_ELBOWS_UP = "Squeeze your rear delts as your elbows reach shoulder height."

/** BodyArea.SHOULDERS: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val shouldersFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf(
        // ---- Presses ----
        // The guide and ACE: lower to the front of the shoulders, "to about chin height". ExRx's page lowers to the
        // upper chest, deeper than the guide, so it is not cited for the line.
        "Seated barbell press" to lines(
            "Your front delts stretch as the bar comes down to chin height.", FRONT_STRAIGHT_ARMS,
            Sources.ACE_SEATED_BB_SHOULDER_PRESS,
        ),
        // ExRx: "Lower to sides of shoulders"; the guide: "lower them under control to shoulder height".
        "DB shoulder press" to lines(
            "Your front delts stretch as the dumbbells reach shoulder height.", FRONT_STRAIGHT_ARMS,
            Sources.EXRX_DB_SHOULDER_PRESS, Sources.ACE_SEATED_OVERHEAD_PRESS,
        ),
        "One-arm DB press" to lines(
            "Your front delts stretch as the dumbbell reaches your shoulder.", FRONT_STRAIGHT_ARM,
            Sources.EXRX_DB_ONE_ARM_SHOULDER_PRESS, Sources.ACE_SEATED_OVERHEAD_PRESS,
        ),
        // The guide lowers "the same way, back to the start": palms facing you, in front of the shoulders.
        "Arnold press" to lines(
            "Your front delts stretch as the dumbbells return to the start.", FRONT_STRAIGHT_ARMS,
            Sources.EXRX_DB_ARNOLD_PRESS,
        ),
        // The guide presses until the elbows are "nearly straight" (locking them hard is a mistake), and lowers back
        // to the start, with the handles set level with the shoulders.
        "Machine shoulder press" to lines(
            "Your front delts stretch as the handles come back to the start.",
            "Squeeze your front delts as your elbows nearly straighten.",
            Sources.ACE_SEATED_MACHINE_SHOULDER_PRESS, Sources.EXRX_LV_SHOULDER_PRESS,
        ),
        // The guide lowers to the front of the shoulders, with the safety stops set so the bar goes no lower.
        "Smith shoulder press" to lines(
            "Your front delts stretch as the bar lowers to your shoulders.", FRONT_STRAIGHT_ARMS,
            Sources.EXRX_SM_SHOULDER_PRESS,
        ),
        // ExRx and the guide: press up and forward until the arm is straight, lower back to the shoulder.
        "Landmine press" to lines(
            "Your front delts stretch as the bar comes back to your shoulder.",
            "Squeeze your front delts as you press up and forward.",
            Sources.EXRX_LV_ONE_ARM_SHOULDER_PRESS,
        ),
        // The guide pulls the elbow "back down to your ribs" and presses "straight overhead".
        "Kettlebell press" to lines(
            "Your front delts stretch as your elbow comes down to your ribs.",
            "Squeeze your front delts as you press the kettlebell overhead.",
            Sources.ACE_SINGLE_ARM_OVERHEAD_PRESS,
        ),

        // ---- Lateral raises ----
        // ExRx: the elbows rise to shoulder height, pointing back; ACE: raise to shoulder level.
        "Lateral raise" to lines(SIDE_DOWN, SIDE_ELBOWS_UP, Sources.EXRX_DB_LATERAL_RAISE, Sources.ACE_LATERAL_RAISE),
        "Seated lateral raise" to lines(SIDE_DOWN, SIDE_ELBOWS_UP, Sources.EXRX_DB_SEATED_LATERAL_RAISE),
        "Cable lateral raise" to lines(
            "Your side delts stretch as your arm lowers to your side.",
            "Squeeze your side delts as your elbow reaches shoulder height.",
            Sources.EXRX_CB_ONE_ARM_LATERAL_RAISE,
        ),
        // The guide and ExRx: until the upper arms are level with the floor (horizontal). With the elbows at shoulder
        // height the upper arms are level, so the shared line names the same top, with a point the lifter can see.
        "Machine lateral raise" to lines(
            SIDE_DOWN, SIDE_ELBOWS_UP,
            Sources.EXRX_LV_LATERAL_RAISE, Sources.EXRX_DB_LATERAL_RAISE,
        ),

        // ---- Front raises ----
        // ACE: "Stop when your arms are roughly level with your shoulders." ExRx goes above horizontal, so it is not
        // cited for the line.
        "Front raise" to lines(
            "Your front delts stretch as the dumbbells lower to your thighs.", FRONT_ARMS_UP,
            Sources.ACE_FRONT_RAISE,
        ),
        // ExRx's barbell page goes past horizontal, at the very point the squeeze names, so it is not cited for the
        // line; ACE's front raise stops at shoulder height, as the guide does.
        "Barbell front raise" to lines(
            "Your front delts stretch as the bar lowers to your thighs.",
            "Squeeze your front delts as the bar reaches shoulder height.",
            Sources.ACE_FRONT_RAISE,
        ),
        // The guide starts with the arm slightly back at the side and stops at about shoulder height. ExRx's cable
        // page goes well above horizontal, so ACE's shoulder-level stop is cited instead.
        "Cable front raise" to lines(
            "Your front delts stretch as your arm lowers to your side.",
            "Squeeze your front delts as the handle reaches shoulder height.",
            Sources.ACE_FRONT_RAISE,
        ),

        // ---- Rear delts ----
        // ExRx: bent over with the torso close to level, raise until the elbows are at shoulder height.
        "Rear delt raise" to lines(REAR_DOWN, REAR_ELBOWS_UP, Sources.EXRX_DB_REAR_LATERAL_RAISE),
        "Seated rear delt raise" to lines(REAR_DOWN, REAR_ELBOWS_UP, Sources.EXRX_DB_SEATED_REAR_LATERAL_RAISE),
        "Chest-supported rear delt raise" to lines(REAR_DOWN, REAR_ELBOWS_UP, Sources.EXRX_DB_LYING_REAR_LATERAL_RAISE),
        // ExRx and the guide: pull until the elbows are just behind the torso; far past it is a mistake.
        "Machine reverse fly" to lines(
            "Your rear delts stretch as the handles return in front of you.",
            "Squeeze your rear delts as your elbows go just behind you.",
            Sources.EXRX_LV_REVERSE_FLY,
        ),
        // The guide pulls the handles out to the sides at shoulder height, and names no further point.
        "Cable reverse fly" to lines(
            "Your rear delts stretch as your hands return in front of you.",
            "Squeeze your rear delts as you pull your arms out to the sides.",
            Sources.EXRX_CB_REVERSE_FLY,
        ),
        // The guide: "Stop when your elbows are slightly behind your back", torso upright on the return.
        "Cable rear delt row" to lines(
            "Your rear delts stretch as your arms straighten in front of you.",
            "Squeeze your rear delts as your elbows go slightly behind you.",
            Sources.EXRX_CB_REAR_DELT_ROW,
        ),

        // ---- Rotator cuff ----
        // Not holds: each guide turns the arm through a range, so two lines (the pause is inside a moving rep).
        // External rotation starts with the forearm across the belly. The guide names no outer end point, so the
        // squeeze names none (ExRx's standing page says "as far as possible", and is not cited for the line). The
        // guide covers a band as well, so no line says cable.
        "Cable external rotation" to lines(
            "Your rotator cuff stretches with your forearm across your belly.",
            "Squeeze your rotator cuff as you turn your arm outward.",
            Sources.ACE_ROTATOR_CUFF_EXTERNAL_ROTATION,
        ),
        // ExRx: elbow against the side, forearm across the belly; lift the dumbbell by turning the shoulder.
        "Side-lying external rotation" to lines(
            "Your rotator cuff stretches as you lower the dumbbell back down.",
            "Squeeze your rotator cuff as you turn the dumbbell up.",
            Sources.EXRX_DB_LYING_EXTERNAL_ROTATION,
        ),
        // The other way round: the lifting step ends with the forearm across the belly (ExRx too). The guide names no
        // start angle, and the cable pulls the arm outward, so the stretch stops at the start rather than "out".
        "Cable internal rotation" to lines(
            "Your rotator cuff stretches as your arm turns back to the start.",
            "Squeeze your rotator cuff as your forearm crosses your belly.",
            Sources.EXRX_CB_INTERNAL_ROTATION,
        ),
    )
}
