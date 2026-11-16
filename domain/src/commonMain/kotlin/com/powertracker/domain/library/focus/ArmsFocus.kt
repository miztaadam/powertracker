package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.Sources

// Each line is worked out from the exercise's own guide (areas/Arms.kt): the stretch where the guide's lowering or
// return ends, never further; the squeeze where its lifting step ends. A line names the exercise's first muscle:
// biceps for the curls, forearms for the hammer and reverse curls and the wrist curls, triceps for the rest. The
// curls that list "Brachialis" first name the biceps, the second muscle in the same list: it is the plain name for
// the front of the upper arm, and both bend the elbow together. The guide's pages come first, then the idea's
// sources (added by [lines]). A page that asks for more range than the guide's step or mistakes allow, at the point
// the line names, is not cited for that line (controller ruling, 2026-11-16).

// The curl guides lower "until your arms are straight" and curl "until your forearms are upright".
private const val BICEPS_DOWN = "Your biceps stretch as you lower to straight arms."
private const val BICEPS_UP = "Squeeze your biceps as your forearms come upright."
private const val BICEPS_DOWN_ONE = "Your biceps stretch as you lower to a straight arm."
private const val BICEPS_UP_ONE = "Squeeze your biceps as your forearm comes upright."
// The dumbbell curls turn the palm up "until your forearm is upright and your palm faces your shoulder".
private const val BICEPS_PALM_UP = "Squeeze your biceps as your palm turns to face your shoulder."
private const val FOREARMS_DOWN = "Your forearms stretch as you lower to straight arms."
// The pushdown guides return "slowly. Stop before your elbows start to move forward", and push down until the arms
// are "nearly straight" (locking hard is a mistake), so no squeeze says straight or lock.
private const val TRICEPS_PUSH_NEARLY = "Squeeze your triceps as your arms nearly straighten."
private const val TRICEPS_PUSH_UP = "Squeeze your triceps as you push up to straight arms."
// The push-up guides lower "toward the floor" and name no stop short of it, so they lower to or near the floor.
private const val TRICEPS_TOWARD_FLOOR = "Your triceps stretch as you lower toward the floor."

/** BodyArea.ARMS: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val armsFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf(
        // ExRx: the bar is lowered toward the upper chest until the forearms meet the biceps, and pressed back up.
        // The guide presses "until your elbows lock", as the close-grip bench does, so the squeeze says lock.
        "JM press" to lines(
            "Your triceps stretch as your forearms meet your upper arms.",
            "Squeeze your triceps as your elbows lock.",
            Sources.EXRX_JM_PRESS,
        ),

        // ---- Curls ----
        // ExRx BBCurl: raise "until forearms are vertical"; lower to straight elbows. ACE 70 curls toward the shoulders.
        "Curl" to lines(BICEPS_DOWN, BICEPS_UP, Sources.EXRX_BB_CURL, Sources.ACE_BICEP_CURL),
        "EZ-bar curl" to lines(BICEPS_DOWN, BICEPS_UP, Sources.EXRX_BB_CURL, Sources.ACE_BICEP_CURL),
        // The guides start with the arms straight at the sides and lower "to the start".
        "DB curl" to lines(BICEPS_DOWN_ONE, BICEPS_PALM_UP, Sources.EXRX_DB_CURL),
        "Seated DB curl" to lines(BICEPS_DOWN_ONE, BICEPS_PALM_UP, Sources.EXRX_DB_SEATED_CURL),
        // Forearms first: the hammer curls work the forearm muscle that bends the elbow, long to short like a curl.
        "Hammer curl" to lines(
            FOREARMS_DOWN, "Squeeze your forearms as the dumbbells reach your shoulders.",
            Sources.ACE_HAMMER_CURL, Sources.EXRX_DB_HAMMER_CURL,
        ),
        "Cable hammer curl" to lines(
            FOREARMS_DOWN, "Squeeze your forearms as you curl the rope forward and up.",
            Sources.EXRX_CB_HAMMER_CURL,
        ),
        // The guide's start: arms "hanging straight down" on the incline. The line goes no further back than that.
        "Incline DB curl" to lines(
            "Your biceps stretch with your arm hanging straight down.", BICEPS_PALM_UP,
            Sources.EXRX_DB_INCLINE_CURL,
        ),
        // The guides lower to straight arms on the pad (their own cue); none says lock.
        "Preacher curl" to lines(BICEPS_DOWN, BICEPS_UP, Sources.EXRX_BB_PREACHER_CURL),
        "DB preacher curl" to lines(BICEPS_DOWN_ONE, BICEPS_UP_ONE, Sources.EXRX_DB_PREACHER_CURL),
        // The guide lowers "until your arms are nearly straight" (ACE 184: extended, not locked). ExRx LVPreacherCurlH
        // lowers until the arms are fully extended, past the guide, so it is not cited for the line.
        "Machine curl" to lines(
            "Your biceps stretch as your arms lower to nearly straight.",
            "Squeeze your biceps as you curl the handles toward your chest.",
            Sources.ACE_SEATED_BICEPS_CURL,
        ),
        "Cable curl" to lines(BICEPS_DOWN, BICEPS_UP, Sources.EXRX_CB_CURL),
        "One-arm cable curl" to lines(BICEPS_DOWN_ONE, BICEPS_UP_ONE, Sources.EXRX_CB_ONE_ARM_CURL),
        // ExRx and the guide: curl toward the front of the shoulder, lower until the arm is straight.
        "Concentration curl" to lines(
            BICEPS_DOWN_ONE, "Squeeze your biceps as you curl to the front of your shoulder.",
            Sources.EXRX_DB_CONCENTRATION_CURL,
        ),
        // The guide's top is the usual one: "until your forearms are upright".
        "Drag curl" to lines(BICEPS_DOWN, BICEPS_UP, Sources.EXRX_BB_DRAG_CURL),
        // The guide curls "as far as your elbows bend" (ExRx: to full flexion).
        "Spider curl" to lines(
            BICEPS_DOWN, "Squeeze your biceps as your elbows bend all the way.",
            Sources.EXRX_BB_PRONE_INCLINE_CURL,
        ),
        // ExRx BBReverseCurl: overhand grip, raise until the forearms are vertical, lower until the arms are fully
        // extended. ACE 310: lift toward the shoulders with the elbows tucked, lower slowly.
        "Reverse curl" to lines(
            FOREARMS_DOWN, "Squeeze your forearms as you curl the bar up, palms down.",
            Sources.EXRX_BB_REVERSE_CURL, Sources.ACE_REVERSE_BICEP_CURL,
        ),

        // ---- Triceps: pushdowns and extensions ----
        // The stretch is the guide's top, with the elbows still at the sides. ACE 185 returns until the elbows begin to
        // drift forward (the guide's stop) and pushes down just short of lockout, so it is cited alone. The ExRx
        // pushdown pages let the elbow travel up at the top, past the guide's stop at the point the stretch names, so
        // they are not cited for these lines (they stay on the guides' own lists).
        "Triceps pushdown" to lines(
            "Your triceps stretch as the bar rises, elbows at your sides.", TRICEPS_PUSH_NEARLY,
            Sources.ACE_TRICEPS_PUSHDOWNS,
        ),
        "Rope pushdown" to lines(
            "Your triceps stretch as the rope rises, elbows at your sides.",
            "Squeeze your triceps as you turn your palms down at the bottom.",
            Sources.ACE_TRICEPS_PUSHDOWNS,
        ),
        "One-arm pushdown" to lines(
            "Your triceps stretch as the handle rises, elbow at your side.",
            "Squeeze your triceps as your arm nearly straightens.",
            Sources.ACE_TRICEPS_PUSHDOWNS,
        ),
        // ExRx and the guide: lower until the forearms touch the upper arms; straighten the arms overhead.
        "Cable overhead triceps extension" to lines(
            "Your triceps stretch as your forearms reach your upper arms.",
            "Squeeze your triceps as your arms straighten overhead.",
            Sources.EXRX_CB_TRI_EXT_ROPE,
        ),
        // The guide stops "when your forearms are about level with the floor" (ACE 74: at a right-angle bend, or when
        // the upper arms start to move back). ExRx DBTriExt lowers further, so it is not cited for the line.
        "DB overhead triceps extension" to lines(
            "Your triceps stretch as your forearms near level with the floor.",
            "Squeeze your triceps as you press back to upright arms.",
            Sources.ACE_TRICEPS_EXTENSION,
        ),
        // The guide stops "just above your forehead, or sooner" and presses up "without locking your elbows", so the
        // squeeze names no lockout. ACE 36 stops at the same point ("just above the forehead, or sooner if the upper
        // arms begin to drift back"), so it comes first; ExRx lowers toward the forehead and goes no further.
        "Skull crusher" to lines(
            "Your triceps stretch as the bar nears your forehead.",
            "Squeeze your triceps as you press the bar back up.",
            Sources.ACE_LYING_BB_TRI_EXT, Sources.EXRX_BB_SKULL_CRUSHER,
        ),
        // ExRx and the guide: lower until the dumbbells are beside the head.
        "DB lying triceps extension" to lines(
            "Your triceps stretch as the dumbbells come beside your head.",
            "Squeeze your triceps as you straighten your arms.",
            Sources.EXRX_DB_LYING_TRI_EXT,
        ),
        "Machine triceps extension" to lines(
            "Your triceps stretch as your forearms return to your upper arms.",
            "Squeeze your triceps as your arms straighten.",
            Sources.EXRX_LV_TRI_EXT,
        ),
        // The guide starts with the forearm hanging straight down and straightens the elbow to move the dumbbell back.
        // ACE 55 first: ExRx's option of raising the elbow above the shoulder is not the guide's upper arm at the side.
        "Triceps kickback" to lines(
            "Your triceps stretch as your forearm hangs straight down.",
            "Squeeze your triceps as your arm straightens behind you.",
            Sources.ACE_TRICEPS_KICKBACK, Sources.EXRX_DB_KICKBACK,
        ),

        // ---- Triceps: dips and push-ups ----
        // The guide stops with the upper arms level with the floor, "no deeper" (ACE 1657 warns of deep dips).
        "Triceps dip" to lines(
            "Your triceps stretch with your upper arms level with the floor.", TRICEPS_PUSH_UP,
            Sources.EXRX_WT_TRI_DIP, Sources.ACE_DANGEROUS_DIPS,
        ),
        // ExRx: body rigid, lower by bending the elbows, press up to straight arms. ACE 41 lowers until the chest or chin
        // touches the floor, which the guide's "toward the floor" allows, so it is the second source.
        "Close-grip push-up" to lines(
            TRICEPS_TOWARD_FLOOR, TRICEPS_PUSH_UP,
            Sources.EXRX_CLOSE_GRIP_PUSH_UP, Sources.ACE_PUSH_UP,
        ),
        // The ACE study's triangle push-up: hands in a triangle, torso rigid, arms extended fully at the top.
        "Triangle push-up" to lines(
            TRICEPS_TOWARD_FLOOR, "Squeeze your triceps as your elbows straighten.",
            Sources.ACE_TRICEPS_STUDY,
        ),

        // ---- Forearms and grip ----
        // Not holds: both wrist curls move the wrist through a range, so two lines each. There is no grip hold among
        // the arms (Farmer's walk is in the core batch).
        // ExRx and the guide: let the weight roll down to the fingers, then curl the wrists up. The squeeze names no
        // height: the guide's "as high as you can" is left to the guide.
        "Wrist curl" to lines(
            "Your forearms stretch as the weight rolls down to your fingers.",
            "Squeeze your forearms as you curl your wrists up.",
            Sources.EXRX_BB_WRIST_CURL, Sources.EXRX_DB_WRIST_CURL,
        ),
        // ExRx and the guide: knuckles up, then down.
        "Reverse wrist curl" to lines(
            "Your forearms stretch as your knuckles point down.",
            "Squeeze your forearms as your knuckles point up.",
            Sources.EXRX_BB_REVERSE_WRIST_CURL, Sources.EXRX_DB_REVERSE_WRIST_CURL,
        ),
    )
}
