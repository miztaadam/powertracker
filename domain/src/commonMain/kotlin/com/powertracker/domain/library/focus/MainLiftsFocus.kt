package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources

// Each line is worked out from the exercise's own guide (areas/MainLifts.kt): the stretch at the guide's lowest point,
// never deeper; the squeeze at the end of its lifting step. The guide's pages come first, then the idea's sources.

/**
 * Two lines resting on the guide's own pages ([page], and [also] when given) and on the idea: a muscle shortens as it
 * lifts, and is long at the bottom.
 */
internal fun lines(stretch: String, squeeze: String, page: SourceId, also: SourceId? = null): MuscleFocus.Lines =
    MuscleFocus.Lines(stretch, squeeze, listOfNotNull(page, also) + Sources.PMC_LENGTHENED_PARTIALS + Sources.ACE_MUSCLES_IN_MOTION)

/** As [lines], for a hamstrings stretch, which also rests on the hamstrings study. */
internal fun hamstringLines(stretch: String, squeeze: String, page: SourceId, also: SourceId? = null): MuscleFocus.Lines =
    MuscleFocus.Lines(
        stretch, squeeze,
        listOfNotNull(page, also) + Sources.PMC_LONG_MUSCLE_LENGTH_HAMSTRINGS + Sources.PMC_LENGTHENED_PARTIALS +
            Sources.ACE_MUSCLES_IN_MOTION,
    )

private const val CHEST_PRESS_UP = "Squeeze your chest as you press the bar up."
private const val QUADS_STAND_LOCKED = "Squeeze your quads as you stand tall with your knees locked."
private const val QUADS_STRAIGHTEN = "Squeeze your quads as your hips and knees straighten."
private const val QUADS_BOTTOM = "Your quads stretch at the bottom of your squat."
private const val CHEST_TO_CHEST = "Your chest stretches as the bar comes down to your chest."
private const val TRICEPS_LOCK = "Squeeze your triceps as your elbows lock."
private const val GLUTES_HIPS_BACK = "Your glutes stretch as you push your hips back to lower."

/** BodyArea.MAIN_LIFTS: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val mainLiftsFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf(
        // Competition depth, exactly as the guide gives it: hip crease below the top of the knee.
        "Squat" to lines(
            "Your quads stretch with your hip crease below your knee.", QUADS_STAND_LOCKED,
            Sources.EXRX_BB_SQUAT, Sources.ACE_BACK_SQUAT,
        ),
        // The bar goes to the chest and pauses there; no deeper.
        "Bench" to lines(
            "Your chest stretches in the pause with the bar on your chest.", CHEST_PRESS_UP,
            Sources.EXRX_BB_BENCH, Sources.EXRX_BB_BENCH_POWER,
        ),
        "Deadlift" to hamstringLines(
            "Your hamstrings stretch as you push your hips back to lower.",
            "Squeeze your glutes as you stand tall with your knees locked.",
            Sources.EXRX_BB_DEADLIFT, Sources.ACE_DEADLIFT,
        ),
        "Strict press" to lines(
            "Your front delts stretch with the bar at the front of your neck.",
            "Squeeze your front delts as you press to straight arms.",
            Sources.EXRX_BB_MILITARY_PRESS, Sources.ACE_STANDING_SHOULDER_PRESS,
        ),

        // ---- Squat variations ----
        "Pause squat" to lines(
            "Your quads stretch in the pause at your normal depth.", "Squeeze your quads as you stand tall.",
            Sources.EXRX_BB_SQUAT, Sources.ACE_BACK_SQUAT,
        ),
        // "At the bottom", not "thighs level": the guide says to stop sooner if the heels lift or the back rounds.
        "Front squat" to lines(QUADS_BOTTOM, QUADS_STRAIGHTEN, Sources.EXRX_BB_FRONT_SQUAT, Sources.ACE_FRONT_SQUAT),
        // The catalogue lists only the upper back and trunk for these two, and the guide holds both steady.
        "Pin squat" to MuscleFocus.NoFocus(listOf(Sources.EXRX_BB_SQUAT, Sources.PMC_STICKING_POINT_REVIEW)),
        "Safety-bar squat" to MuscleFocus.NoFocus(listOf(Sources.EXRX_SAFETY_BAR_SQUAT, Sources.PMC_SAFETY_BAR_SQUAT)),
        "Box squat" to lines(
            "Your glutes stretch as you sit back onto the box.", "Squeeze your glutes as you drive up to standing.",
            Sources.EXRX_BB_BOX_SQUAT,
        ),
        "High-bar squat" to lines(QUADS_BOTTOM, QUADS_STRAIGHTEN, Sources.EXRX_BB_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT),
        "Tempo squat" to lines(
            "Your quads stretch as you slowly reach your normal depth.", "Squeeze your quads as you drive up to standing.",
            Sources.EXRX_BB_SQUAT, Sources.PMC_ECCENTRIC_TEMPO_SQUAT,
        ),

        // ---- Bench variations ----
        // The bar never touches the chest.
        "Spoto press" to lines(
            "Your chest stretches with the bar held just above your chest.", CHEST_PRESS_UP,
            Sources.EXRX_BB_BENCH, Sources.EXRX_BENCH_ANALYSES,
        ),
        "Long-pause bench" to lines(
            "Your chest stretches in the long pause on your chest.", CHEST_PRESS_UP,
            Sources.EXRX_BENCH_ANALYSES, Sources.EXRX_BB_BENCH_POWER,
        ),
        // A partial range, but the chest still goes from longer to shorter: the stretch is at the pins.
        "Pin press" to lines(
            "Your chest stretches as the bar settles on the pins.", CHEST_PRESS_UP,
            Sources.EXRX_BB_BENCH, Sources.PMC_STICKING_POINT_REVIEW,
        ),
        "Larsen press" to lines(CHEST_TO_CHEST, CHEST_PRESS_UP, Sources.EXRX_BB_BENCH, Sources.PMC_BENCH_WITHOUT_LEGS),
        "Close-grip bench" to lines(
            "Your triceps stretch as the bar comes down to your chest.", TRICEPS_LOCK,
            Sources.EXRX_CLOSE_GRIP_BENCH, Sources.ACE_CLOSE_GRIP_BENCH,
        ),
        "Board press" to lines(
            "Your triceps stretch as the bar touches the board.", TRICEPS_LOCK,
            Sources.EXRX_BOARD_PRESS, Sources.PMC_STICKING_POINT_REVIEW,
        ),
        // The guide says the floor press is said to take away the stretch and rebound of a normal bench, so the
        // line says where the chest is longest without calling it a stretch.
        "Barbell floor press" to lines(
            "Your chest is at its longest as your upper arms touch the floor.", "Squeeze your chest as you press to straight arms.",
            Sources.EXRX_BB_FLOOR_PRESS,
        ),
        "Wide-grip bench" to lines(CHEST_TO_CHEST, CHEST_PRESS_UP, Sources.EXRX_BB_BENCH, Sources.EXRX_BB_BENCH_POWER),

        // ---- Deadlift variations ----
        "Deficit deadlift" to lines(
            "Your quads stretch as the bar settles on the floor.", QUADS_STAND_LOCKED,
            Sources.EXRX_DEFICIT_DEADLIFT, Sources.EXRX_BB_DEADLIFT,
        ),
        "Paused deadlift" to lines(
            "Your quads stretch as the bar settles on the floor.", QUADS_STAND_LOCKED,
            Sources.EXRX_BB_DEADLIFT, Sources.ACE_DEADLIFT,
        ),
        "Paused deadlift at the knee" to hamstringLines(
            "Your hamstrings stretch in the pause, the bar below your knees.", "Squeeze your hamstrings as you stand tall.",
            Sources.EXRX_BB_DEADLIFT, Sources.ACE_DEADLIFT,
        ),
        "Block pull" to lines(
            "Your glutes stretch as you lower the bar to the blocks.", "Squeeze your glutes as you stand tall with your knees locked.",
            Sources.EXRX_BB_BLOCK_PULL, Sources.EXRX_RACK_PULL,
        ),
        "Sumo deadlift" to lines(
            GLUTES_HIPS_BACK, "Squeeze your glutes as you stand tall with your shoulders back.",
            Sources.EXRX_BB_SUMO_DEADLIFT,
        ),
        "Trap-bar deadlift" to lines(
            GLUTES_HIPS_BACK, "Squeeze your glutes as your hips and knees straighten fully.",
            Sources.EXRX_TB_DEADLIFT,
        ),
    )
}
