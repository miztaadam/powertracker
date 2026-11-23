package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.Sources

// Each line is worked out from the exercise's own guide (areas/Chest.kt): the stretch at the guide's lowest or most
// open point, never further; the squeeze at the end of its lifting step. Every chest exercise lists "Pecs" first, so
// every line names the chest. The guide's pages come first, then the idea's sources (added by [lines]).

private const val BAR_TO_CHEST = "Your chest stretches as the bar comes down to your chest."
private const val BAR_UP = "Squeeze your chest as you press the bar up."
// The guides lower the dumbbells "to the sides of your chest", no deeper (owner decision 2: never "below the bench").
private const val DBS_TO_CHEST = "Your chest stretches as the dumbbells reach your chest."
private const val DBS_UP = "Squeeze your chest as you press the dumbbells up."
// The fly guides stop at shoulder level ("Stop at shoulder level"; lowering below it is a mistake).
private const val DBS_SHOULDER_LEVEL = "Your chest stretches as the dumbbells reach shoulder level."
private const val HANDS_SHOULDER_LEVEL = "Your chest stretches as your hands reach shoulder level."
// The machine guides let the handles come back to the start, and no further; they say not to lock the elbows.
private const val HANDLES_BACK = "Your chest stretches as the handles come back to the start."
private const val ARMS_STRAIGHTEN = "Squeeze your chest as your arms straighten."
private const val HANDLES_TOGETHER = "Squeeze your chest as the handles come together."
private const val PUSH_TO_STRAIGHT = "Squeeze your chest as you push up to straight arms."
// The dip guides stop with the upper arms level with the floor: "no deeper".
private const val DIP_BOTTOM = "Your chest stretches with your upper arms level with the floor."
// The push-up guides lower the chest toward the floor and name no stop short of it, so they lower to or near the
// floor (ACE 41: until the chest or chin touches it).
private const val TOWARD_FLOOR = "Your chest stretches as you lower toward the floor."

/** BodyArea.CHEST: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val chestFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf(
        // The pilot sample. ExRx: lower "to sides of chest until slight stretch is felt in chest or shoulder".
        "DB press" to lines(DBS_TO_CHEST, DBS_UP, Sources.EXRX_DB_BENCH, Sources.ACE_DB_CHEST_PRESS),

        // ---- Barbell ----
        "Incline bench press" to lines(BAR_TO_CHEST, BAR_UP, Sources.EXRX_BB_INCLINE_BENCH, Sources.EXRX_BENCH_ANALYSES),
        "Decline bench press" to lines(BAR_TO_CHEST, BAR_UP, Sources.EXRX_BB_DECLINE_BENCH, Sources.EXRX_BENCH_ANALYSES),

        // ---- Dumbbell ----
        // ExRx: "Lower weight to sides of upper chest until slight stretch is felt in chest or shoulder."
        "Incline DB press" to lines(
            "Your chest stretches as the dumbbells reach your upper chest.", DBS_UP,
            Sources.EXRX_DB_INCLINE_BENCH, Sources.ACE_INCLINE_CHEST_PRESS,
        ),
        "Decline DB press" to lines(DBS_TO_CHEST, DBS_UP, Sources.EXRX_DB_DECLINE_BENCH, Sources.ACE_DB_CHEST_PRESS),
        // The pilot sample. ExRx: "Lower dumbbells to sides until chest muscles are stretched"; ACE stops at shoulder
        // or chest level, and the guide counts lower than the shoulders as a mistake.
        "DB fly" to lines(
            DBS_SHOULDER_LEVEL, "Squeeze your chest as the dumbbells come together.",
            Sources.EXRX_DB_FLY, Sources.ACE_LYING_CHEST_FLY,
        ),
        "Incline DB fly" to lines(
            DBS_SHOULDER_LEVEL, "Squeeze your chest as the dumbbells come nearly together.",
            Sources.EXRX_DB_INCLINE_FLY, Sources.ACE_LYING_CHEST_FLY,
        ),
        "Decline DB fly" to lines(
            DBS_SHOULDER_LEVEL, "Squeeze your chest as the dumbbells nearly touch.",
            Sources.EXRX_DB_DECLINE_FLY, Sources.ACE_LYING_CHEST_FLY,
        ),

        // ---- Machine ----
        // ExRx: return "until slight stretch is felt in chest or shoulders". The guides: back to the start.
        "Machine chest press" to lines(HANDLES_BACK, ARMS_STRAIGHTEN, Sources.EXRX_LV_CHEST_PRESS, Sources.ACE_SEATED_CHEST_PRESS),
        "Incline machine press" to lines(HANDLES_BACK, ARMS_STRAIGHTEN, Sources.EXRX_LV_INCLINE_CHEST_PRESS, Sources.ACE_SEATED_CHEST_PRESS),
        "Decline machine press" to lines(HANDLES_BACK, ARMS_STRAIGHTEN, Sources.EXRX_LV_DECLINE_CHEST_PRESS, Sources.ACE_SEATED_CHEST_PRESS),
        // The guide counts letting the handles pull the arms back past the start as a mistake.
        "Pec deck" to lines(HANDLES_BACK, HANDLES_TOGETHER, Sources.EXRX_LV_PEC_DECK, Sources.EXRX_LV_SEATED_FLY),

        // ---- Smith machine ----
        "Smith bench press" to lines(BAR_TO_CHEST, BAR_UP, Sources.EXRX_SM_BENCH, Sources.EXRX_BENCH_ANALYSES),
        "Smith incline press" to lines(BAR_TO_CHEST, BAR_UP, Sources.EXRX_SM_INCLINE_BENCH, Sources.EXRX_BENCH_ANALYSES),
        "Smith decline press" to lines(
            "Your chest stretches as the bar comes down to your lower chest.", BAR_UP,
            Sources.EXRX_SM_DECLINE_BENCH, Sources.EXRX_BENCH_ANALYSES,
        ),

        // ---- Cable ----
        // ExRx: "Return to starting position until chest muscles are stretched." The guide: back to the start.
        "Cable crossover" to lines(
            "Your chest stretches as your arms go back out to your sides.", "Squeeze your chest as your hands come together.",
            Sources.EXRX_CB_STANDING_FLY, Sources.ACE_STANDING_CHEST_FLY,
        ),
        // The guide: back to the start, hands never further back than the armpits.
        "Low-to-high cable fly" to lines(
            "Your chest stretches as your arms open back to the start.", "Squeeze your chest as your hands meet at face height.",
            Sources.EXRX_CB_STANDING_INCLINE_FLY, Sources.ACE_STANDING_INCLINE_CABLE_FLYES,
        ),
        // ExRx: return "until slight stretch is felt in chest or shoulders"; the guide stops the handles close to the chest.
        "Cable chest press" to lines(
            "Your chest stretches as the handles come close to your chest.", ARMS_STRAIGHTEN,
            Sources.EXRX_CB_STANDING_CHEST_PRESS, Sources.ACE_SEATED_CABLE_PRESS,
        ),
        "Lying cable fly" to lines(HANDS_SHOULDER_LEVEL, HANDLES_TOGETHER, Sources.EXRX_CB_LYING_FLY, Sources.ACE_LYING_CHEST_FLY),
        "Incline bench cable fly" to lines(HANDS_SHOULDER_LEVEL, HANDLES_TOGETHER, Sources.EXRX_CB_INCLINE_FLY, Sources.ACE_LYING_CHEST_FLY),
        // The guide lets the handles go back "under control" to the start, hands never further back than the armpits.
        "Seated cable fly" to lines(
            "Your chest stretches as the handles go back to the start.", HANDLES_TOGETHER,
            Sources.EXRX_CB_SEATED_FLY,
        ),

        // ---- Dips ----
        "Chest dip" to lines(DIP_BOTTOM, PUSH_TO_STRAIGHT, Sources.EXRX_CHEST_DIP, Sources.ACE_DANGEROUS_DIPS),
        "Assisted dip" to lines(DIP_BOTTOM, PUSH_TO_STRAIGHT, Sources.EXRX_ASSISTED_CHEST_DIP, Sources.ACE_DANGEROUS_DIPS),

        // ---- Push-ups ----
        "Push-up" to lines(TOWARD_FLOOR, "Squeeze your chest as you push the floor away.", Sources.EXRX_PUSH_UP, Sources.ACE_PUSH_UP),
        "Incline push-up" to lines(
            "Your chest stretches as you lower it to the edge.", PUSH_TO_STRAIGHT,
            Sources.EXRX_INCLINE_PUSH_UP, Sources.ACE_PUSH_UP,
        ),
        // "All the way down", but with the body straight: the guide counts arching to get lower as a mistake.
        "Decline push-up" to lines(
            "Your chest stretches at the bottom, with your body straight.", PUSH_TO_STRAIGHT,
            Sources.EXRX_DECLINE_PUSH_UP, Sources.ACE_PUSH_UP,
        ),
        "Knee push-up" to lines(
            TOWARD_FLOOR, "Squeeze your chest as you push back up.",
            Sources.EXRX_KNEE_PUSH_UP, Sources.ACE_BENT_KNEE_PUSH_UP,
        ),
        // The arm near the body does the work, so the lines are about its side.
        "Archer push-up" to lines(
            "Your chest stretches on the bent arm's side as you lower.", "Squeeze your chest as you press up with the bent arm.",
            Sources.EXRX_ARCHER_PUSH_UP, Sources.ACE_PUSH_UP,
        ),
        // A fast push-up, but the chest still goes from long to short: the squeeze is the push off the floor.
        "Clap push-up" to lines(
            TOWARD_FLOOR, "Squeeze your chest as you push off the floor.",
            Sources.EXRX_CLAP_PUSH_UP, Sources.ACE_PUSH_UP,
        ),
        "Weighted push-up" to lines(
            "Your chest stretches as you bend your elbows to lower.", PUSH_TO_STRAIGHT,
            Sources.EXRX_WEIGHTED_PUSH_UP, Sources.ACE_PUSH_UP,
        ),
    )
}
