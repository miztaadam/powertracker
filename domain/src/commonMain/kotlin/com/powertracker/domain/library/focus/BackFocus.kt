package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.Sources

// Each line is worked out from the exercise's own guide (areas/Back.kt): the stretch where the guide's return ends,
// never further; the squeeze where its pull ends. A line names the exercise's first muscle (lats for the pulls, upper
// back for most rows), except on the back extensions (see there). The guide's pages come first, then the idea's
// sources (added by [lines] and [hamstringLines]).

// The pull-up and chin-up guides lower "until your arms and shoulders are fully stretched". The line names the arms
// only: the pull-up guide also asks the shoulders to stay back and down.
private const val LATS_HANG = "Your lats stretch as you lower to straight arms."
// The pulldown guides return "until your arms and shoulders are fully stretched" (the pilot sample).
private const val LATS_ARMS_UP = "Your lats stretch with your arms straight above you."
private const val LATS_CHIN_OVER = "Squeeze your lats as your chin clears the bar."
private const val LATS_NECK_TO_HANDS = "Squeeze your lats as your neck reaches the height of your hands."
private const val LATS_ELBOWS_SIDES = "Squeeze your lats as your elbows reach your sides."
// The barbell row lowers "until your arms are straight and your shoulders are stretched down".
private const val LATS_ARMS_DOWN = "Your lats stretch as your arms hang straight down."
// The bent-over and chest-down rows lower "until your arms are straight and your shoulders are stretched down".
private const val UPPER_BACK_HANG = "Your upper back stretches as your arms hang straight down."
// The seated rows return to straight arms with the torso upright: the shoulders go no further forward than that.
private const val UPPER_BACK_STRAIGHT_ARMS = "Your upper back stretches as you return to straight arms."
private const val UPPER_BACK_SHOULDERS_BACK = "Squeeze your upper back as you pull your shoulders back."
// The shrug guides: lift "as high as you can"; a level slope can be enough. The squeeze is the guide's top.
private const val TRAPS_DOWN = "Your traps stretch as you lower your shoulders back down."
private const val TRAPS_UP = "Squeeze your traps as your shoulders reach their highest."
// The back extension guides keep the back neutral and bend at the hips, stopping with the torso at about a right
// angle to the legs. The lower back is held still, so the lines name the hamstrings and glutes, which do move.
private const val HAMSTRINGS_RIGHT_ANGLE = "Your hamstrings stretch as your hips bend to a right angle."
private const val GLUTES_IN_LINE = "Squeeze your glutes as you rise in line with your legs."

/** BodyArea.BACK: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val backFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf(
        // Lats first. ExRx: pull to the upper waist; the torso stays still.
        "Row" to lines(LATS_ARMS_DOWN, "Squeeze your lats as the bar reaches your upper waist.", Sources.EXRX_BB_ROW),
        // ExRx and ACE: lower until the arms are fully extended; pull until the chin is above the bar.
        "Pull-up" to lines(LATS_HANG, LATS_CHIN_OVER, Sources.EXRX_PULL_UP, Sources.ACE_PULL_UPS),

        // ---- Rows ----
        // ExRx: "return until the arm is extended and the shoulder is stretched downward".
        "One-arm DB row" to lines(
            "Your upper back stretches as your arm hangs straight down.",
            "Squeeze your upper back as the dumbbell reaches your ribs.",
            Sources.EXRX_DB_BENT_OVER_ROW, Sources.ACE_SINGLE_ARM_ROW,
        ),
        // ExRx: "Return until arms are extended and shoulders are stretched forward while keeping torso upright."
        "Seated cable row" to lines(
            UPPER_BACK_STRAIGHT_ARMS, UPPER_BACK_SHOULDERS_BACK,
            Sources.EXRX_CB_STRAIGHT_BACK_SEATED_ROW, Sources.ACE_CABLE_SEATED_ROW,
        ),
        "Wide-grip cable row" to lines(
            UPPER_BACK_STRAIGHT_ARMS, UPPER_BACK_SHOULDERS_BACK,
            Sources.EXRX_CB_WIDE_GRIP_STR_BACK_ROW, Sources.EXRX_CB_WIDE_GRIP_SEATED_ROW,
        ),
        // The guide pulls the shoulder back at the end, then "slowly straighten your arm".
        "One-arm cable row" to lines(
            "Your upper back stretches as you slowly straighten your arm.",
            "Squeeze your upper back as you pull your shoulder back.",
            Sources.EXRX_CB_ONE_ARM_ROW, Sources.ACE_CABLE_SINGLE_ARM_ROW,
        ),
        // Straight arms, torso upright: the guide counts rounding the shoulders forward on the way back as a mistake.
        "Machine row" to lines(
            UPPER_BACK_STRAIGHT_ARMS, "Squeeze your upper back as your elbows pass your torso.",
            Sources.EXRX_LV_SEATED_ROW, Sources.ACE_SEATED_ROW,
        ),
        "Chest-supported row" to lines(
            UPPER_BACK_HANG, "Squeeze your upper back as your shoulder blades come together.",
            Sources.EXRX_DB_INCLINE_ROW, Sources.EXRX_DB_LYING_ROW,
        ),
        // The bar goes to the upper waist or lower chest, the dumbbells to the sides: "the weight" covers both.
        "Seal row" to lines(
            UPPER_BACK_HANG, "Squeeze your upper back as the weight reaches your body.",
            Sources.EXRX_CAMBERED_LYING_ROW, Sources.EXRX_DB_LYING_ROW,
        ),
        // The torso stays down and the back flat; only the arms move.
        "T-bar row" to lines(
            UPPER_BACK_HANG, "Squeeze your upper back as the handles reach your torso.",
            Sources.EXRX_LV_T_BAR_ROW, Sources.ACE_BENT_OVER_ROW,
        ),
        // ExRx: pull as high as possible; near the top, pull the shoulders back.
        "Chest-supported T-bar row" to lines(UPPER_BACK_HANG, UPPER_BACK_SHOULDERS_BACK, Sources.EXRX_LV_INCLINE_ROW),
        // ExRx: "Return until arms are extended and shoulders are stretched forward."
        "Inverted row" to lines(
            "Your upper back stretches as your arms straighten below the bar.",
            "Squeeze your upper back as your chest reaches the bar.",
            Sources.EXRX_INVERTED_ROW,
        ),

        // ---- Pulldowns ----
        // The pilot sample's stretch; its squeeze is reworded into the "Squeeze your ... as" form the lines share.
        "Lat pulldown" to lines(
            LATS_ARMS_UP, "Squeeze your lats as the bar reaches your upper chest.",
            Sources.EXRX_CB_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN,
        ),
        "Close-grip pulldown" to lines(
            LATS_ARMS_UP, "Squeeze your lats as the handle reaches your upper chest.",
            Sources.EXRX_CB_CLOSE_GRIP_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN,
        ),
        // The guide: to the upper chest, "until your elbows are at your sides".
        "Underhand pulldown" to lines(
            LATS_ARMS_UP, LATS_ELBOWS_SIDES,
            Sources.EXRX_CB_UNDERHAND_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN,
        ),
        "Machine pulldown" to lines(
            LATS_ARMS_UP, "Squeeze your lats as the lever reaches your upper chest.",
            Sources.EXRX_LV_FRONT_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN,
        ),
        // The guide: "Return until your arm is fully straight, letting your shoulder rise."
        "One-arm pulldown" to lines(
            "Your lats stretch with your arm straight and your shoulder up.",
            "Squeeze your lats as the handle reaches the side of your chest.",
            Sources.EXRX_CB_ONE_ARM_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN,
        ),
        // ExRx and the guide: pull "forward and down as far as possible", then back "forward and overhead".
        "Straight-arm pulldown" to lines(
            "Your lats stretch as the bar returns overhead.", "Squeeze your lats as you pull the bar all the way down.",
            Sources.EXRX_CB_SEATED_PULLOVER,
        ),

        // ---- Pull-ups and chin-ups ----
        // ExRx: pull "until the elbows are to the sides"; lower until arms and shoulders are fully extended.
        "Chin-up" to lines(LATS_HANG, LATS_ELBOWS_SIDES, Sources.EXRX_CHIN_UP, Sources.ACE_PULL_UPS),
        "Weighted pull-up" to lines(LATS_HANG, LATS_CHIN_OVER, Sources.EXRX_WEIGHTED_PULL_UP, Sources.ACE_PULL_UPS),
        "Weighted chin-up" to lines(LATS_HANG, LATS_ELBOWS_SIDES, Sources.EXRX_WEIGHTED_CHIN_UP, Sources.ACE_PULL_UPS),
        // ExRx: "Pull body up until neck reaches height of hands."
        "Assisted pull-up" to lines(LATS_HANG, LATS_NECK_TO_HANDS, Sources.EXRX_ASSISTED_PULL_UP, Sources.ACE_PULL_UPS),
        // The guide counts cutting the bottom short as a mistake.
        "Assisted chin-up" to lines(LATS_HANG, LATS_ELBOWS_SIDES, Sources.EXRX_ASSISTED_CHIN_UP, Sources.ACE_PULL_UPS),
        "Band-assisted pull-up" to lines(
            LATS_HANG, "Squeeze your lats as your chin rises above the bar.",
            Sources.EXRX_BAND_ASSISTED_PULL_UP, Sources.ACE_PULL_UPS,
        ),
        "Neutral-grip pull-up" to lines(LATS_HANG, LATS_NECK_TO_HANDS, Sources.EXRX_NEUTRAL_GRIP_PULL_UP, Sources.ACE_PULL_UPS),

        // ---- Pullovers ----
        // Pecs first. ExRx and the guide: lower until the upper arms are (about) in line with the torso, no further;
        // forcing it lower is a mistake.
        "DB pullover" to lines(
            "Your chest stretches with your upper arms level with your torso.",
            "Squeeze your chest as you bring the dumbbell back over you.",
            Sources.EXRX_DB_PULLOVER,
        ),
        // ExRx: return "until shoulders are fully flexed, or upper arms are parallel to torso". The guide's mistake:
        // forcing the range further than the shoulders allow.
        "Machine pullover" to lines(
            "Your lats stretch as your arms return in line with your torso.", LATS_ELBOWS_SIDES,
            Sources.EXRX_LV_PULLOVER,
        ),

        // ---- Shrugs ----
        "Barbell shrug" to lines(TRAPS_DOWN, TRAPS_UP, Sources.EXRX_BB_SHRUG),
        "DB shrug" to lines(TRAPS_DOWN, TRAPS_UP, Sources.EXRX_DB_SHRUG),
        "Trap-bar shrug" to lines(TRAPS_DOWN, TRAPS_UP, Sources.EXRX_TRAP_BAR_SHRUG, Sources.EXRX_DB_SHRUG),

        // ---- Back extensions ----
        // ExRx: lower "until mild stretch is felt or torso is approximately perpendicular to legs"; do not lower
        // "beyond mild stretch throughout hamstrings and low back". The guide stops at a right angle.
        "Back extension" to hamstringLines(HAMSTRINGS_RIGHT_ANGLE, GLUTES_IN_LINE, Sources.EXRX_45_HYPEREXTENSION),
        "Weighted back extension" to hamstringLines(
            HAMSTRINGS_RIGHT_ANGLE, GLUTES_IN_LINE, Sources.EXRX_WT_45_HYPEREXTENSION, Sources.EXRX_WT_45_BACK_EXTENSION,
        ),

        // ---- Face pulls ----
        // Rear delts first. The guide's start: "arms straight in front of your face"; its end: pulling the rope
        // toward your face with the arms turned out.
        "Face pull" to lines(
            "Your rear delts stretch as your arms straighten in front of you.",
            "Squeeze your rear delts as you pull the rope toward your face.",
            Sources.NSCA_FACE_PULL, Sources.EXRX_CB_REAR_DELT_ROW_ROPE,
        ),
    )
}
