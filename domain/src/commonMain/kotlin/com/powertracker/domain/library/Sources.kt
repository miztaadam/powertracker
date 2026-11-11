package com.powertracker.domain.library

/** A reputable source a guide was checked against. Never shown in the app; listed in docs/library/sources.md. */
data class Source(val title: String, val publisher: String, val url: String, val accessed: String)

/** Every source the catalogue cites, keyed by [SourceId]. Only URLs returned by research searches are recorded. */
object Sources {
    private const val ACE = "American Council on Exercise (ACE)"
    private const val EXRX = "ExRx.net"
    private const val IPF = "International Powerlifting Federation (IPF)"
    private const val NSCA = "National Strength and Conditioning Association (NSCA)"
    private const val PMC = "PubMed Central (peer-reviewed)"
    private const val L1 = "2026-11-11"
    private const val L4 = "2026-11-11"
    private const val L5 = "2026-11-11"
    private const val L6 = "2026-11-11"
    private const val L7 = "2026-11-11"

    // Main lifts and their variations
    val IPF_RULES_2026 = SourceId("ipf-technical-rules-2026")
    val ACE_BACK_SQUAT = SourceId("ace-back-squat")
    val EXRX_BB_SQUAT = SourceId("exrx-barbell-squat")
    val PMC_BACK_SQUAT_ASSESSMENT = SourceId("pmc-back-squat-assessment")
    val EXRX_BB_FRONT_SQUAT = SourceId("exrx-barbell-front-squat")
    val ACE_FRONT_SQUAT = SourceId("ace-front-squat")
    val ACE_FRONT_SQUAT_TO_PRESS = SourceId("ace-front-squat-to-overhead-press")
    val EXRX_SAFETY_BAR_SQUAT = SourceId("exrx-safety-barbell-squat")
    val PMC_SAFETY_BAR_SQUAT = SourceId("pmc-safety-bar-high-low-bar-squat")
    val EXRX_BB_BOX_SQUAT = SourceId("exrx-barbell-box-squat")
    val PMC_STICKING_POINT_REVIEW = SourceId("pmc-sticking-point-review")
    val PMC_STICKING_POINT_THREE_LIFTS = SourceId("pmc-sticking-point-three-lifts")
    val EXRX_BB_BENCH = SourceId("exrx-barbell-bench-press")
    val EXRX_BB_BENCH_POWER = SourceId("exrx-barbell-bench-press-power-lift")
    val EXRX_BENCH_ANALYSES = SourceId("exrx-bench-press-analyses")
    val PMC_PRESS_STICKING_REGION = SourceId("pmc-bench-overhead-sticking-region")
    val PMC_BENCH_WITHOUT_LEGS = SourceId("pmc-bench-press-disabled-able-bodied")
    val ACE_CLOSE_GRIP_BENCH = SourceId("ace-close-grip-bench-press")
    val EXRX_CLOSE_GRIP_BENCH = SourceId("exrx-barbell-close-grip-bench-press")
    val EXRX_BOARD_PRESS = SourceId("exrx-barbell-board-press")
    val EXRX_BB_DEADLIFT = SourceId("exrx-barbell-deadlift")
    val ACE_DEADLIFT = SourceId("ace-deadlift")
    val ACE_HOW_TO_DEADLIFT = SourceId("ace-prosource-how-to-deadlift")
    val EXRX_DEFICIT_DEADLIFT = SourceId("exrx-barbell-deficit-deadlift")
    val EXRX_RACK_PULL = SourceId("exrx-barbell-rack-pull")
    val EXRX_BB_BLOCK_PULL = SourceId("exrx-barbell-block-pull")

    // Accessories
    val EXRX_DB_SINGLE_LEG_SPLIT_SQUAT = SourceId("exrx-dumbbell-single-leg-split-squat")
    val PMC_BULGARIAN_SPLIT_SQUAT = SourceId("pmc-bulgarian-split-squat-activation")
    val EXRX_HIP_THRUST = SourceId("exrx-barbell-hip-thrust")
    val ACE_HIP_THRUST = SourceId("ace-understanding-barbell-hip-thrust")
    val EXRX_DB_BENCH = SourceId("exrx-dumbbell-bench-press")
    val ACE_DB_CHEST_PRESS = SourceId("ace-chest-press")
    val EXRX_JM_PRESS = SourceId("exrx-barbell-jm-bench-press")
    val EXRX_RDL = SourceId("exrx-romanian-deadlift")
    val ACE_RDL = SourceId("ace-romanian-deadlift")
    val NSCA_RDL = SourceId("nsca-romanian-deadlift")
    val EXRX_BB_CURL = SourceId("exrx-barbell-curl")
    val ACE_BICEP_CURL = SourceId("ace-bicep-curl")
    val EXRX_BB_ROW = SourceId("exrx-barbell-bent-over-row")
    val EXRX_PULL_UP = SourceId("exrx-pull-up")
    val ACE_PULL_UPS = SourceId("ace-pull-ups")

    // Chest (L4)
    val EXRX_BB_INCLINE_BENCH = SourceId("exrx-barbell-incline-bench-press")
    val EXRX_BB_DECLINE_BENCH = SourceId("exrx-barbell-decline-bench-press")
    val EXRX_DB_INCLINE_BENCH = SourceId("exrx-dumbbell-incline-bench-press")
    val EXRX_DB_DECLINE_BENCH = SourceId("exrx-dumbbell-decline-bench-press")
    val EXRX_DB_FLY = SourceId("exrx-dumbbell-fly")
    val EXRX_DB_INCLINE_FLY = SourceId("exrx-dumbbell-incline-fly")
    val EXRX_DB_DECLINE_FLY = SourceId("exrx-dumbbell-decline-fly")
    val EXRX_LV_CHEST_PRESS = SourceId("exrx-lever-chest-press")
    val EXRX_LV_INCLINE_CHEST_PRESS = SourceId("exrx-lever-incline-chest-press")
    val EXRX_LV_DECLINE_CHEST_PRESS = SourceId("exrx-lever-decline-chest-press-plate-loaded")
    val EXRX_LV_SEATED_FLY = SourceId("exrx-lever-isolateral-seated-fly")
    val EXRX_LV_PEC_DECK = SourceId("exrx-lever-isolateral-pec-deck-fly")
    val EXRX_SM_BENCH = SourceId("exrx-smith-bench-press")
    val EXRX_SM_INCLINE_BENCH = SourceId("exrx-smith-incline-bench-press")
    val EXRX_SM_DECLINE_BENCH = SourceId("exrx-smith-decline-bench-press")
    val EXRX_GYM_ETIQUETTE = SourceId("exrx-gym-etiquette")
    val EXRX_CB_STANDING_FLY = SourceId("exrx-cable-isolateral-standing-fly")
    val EXRX_CB_STANDING_INCLINE_FLY = SourceId("exrx-cable-standing-incline-fly")
    val EXRX_CB_STANDING_CHEST_PRESS = SourceId("exrx-cable-isolateral-standing-chest-press")
    val EXRX_CB_LYING_FLY = SourceId("exrx-cable-isolateral-lying-fly")
    val EXRX_CB_INCLINE_FLY = SourceId("exrx-cable-isolateral-incline-fly")
    val EXRX_CB_SEATED_FLY = SourceId("exrx-cable-isolateral-cable-fly")
    val EXRX_CHEST_DIP = SourceId("exrx-chest-dip")
    val EXRX_WEIGHTED_CHEST_DIP = SourceId("exrx-weighted-chest-dip")
    val EXRX_ASSISTED_CHEST_DIP = SourceId("exrx-machine-assisted-chest-dip")
    val EXRX_ASSISTED_CHEST_DIP_KNEELING = SourceId("exrx-machine-assisted-chest-dip-kneeling")
    val EXRX_ACTUAL_RESISTANCE = SourceId("exrx-calculating-actual-resistance")
    val EXRX_PUSH_UP = SourceId("exrx-push-up")
    val EXRX_INCLINE_PUSH_UP = SourceId("exrx-incline-push-up")
    val EXRX_DECLINE_PUSH_UP = SourceId("exrx-decline-push-up")
    val EXRX_KNEE_PUSH_UP = SourceId("exrx-push-up-on-knees")
    val EXRX_ARCHER_PUSH_UP = SourceId("exrx-archer-push-up")
    val EXRX_CLAP_PUSH_UP = SourceId("exrx-clap-push-up")
    val EXRX_WEIGHTED_PUSH_UP = SourceId("exrx-weighted-push-up")
    val EXRX_WEIGHTED_PUSH_UP_HANDLES = SourceId("exrx-weighted-push-up-on-handles")
    val ACE_INCLINE_CHEST_PRESS = SourceId("ace-incline-chest-press")
    val ACE_LYING_CHEST_FLY = SourceId("ace-lying-chest-fly")
    val ACE_SEATED_CHEST_PRESS = SourceId("ace-seated-chest-press")
    val ACE_SEATED_CABLE_PRESS = SourceId("ace-seated-cable-press")
    val ACE_STANDING_CHEST_FLY = SourceId("ace-standing-chest-fly")
    val ACE_STANDING_INCLINE_CABLE_FLYES = SourceId("ace-standing-incline-cable-flyes")
    val ACE_PUSH_UP = SourceId("ace-push-up")
    val ACE_BENT_KNEE_PUSH_UP = SourceId("ace-bent-knee-push-up")
    val ACE_BRACE_OR_BREATHE = SourceId("ace-spinal-stability-brace-or-breathe")
    val NSCA_STANDARDS_SUMMARY = SourceId("nsca-standards-and-guidelines-summary")

    val ACE_SEATED_CLOSE_GRIP_CHEST_PRESS = SourceId("ace-seated-close-grip-chest-press")
    val EXRX_BB_FLOOR_PRESS = SourceId("exrx-barbell-floor-press")
    val PMC_GRIP_BENCH_VS_SEATED_PRESS = SourceId("pmc-bench-seated-chest-press-grips")
    val PMC_GRIP_WIDTH_NOVICE_TRAINED = SourceId("pmc-bench-grip-width-novice-trained")

    // Back (L5)
    val EXRX_DB_BENT_OVER_ROW = SourceId("exrx-dumbbell-bent-over-row")
    val ACE_SINGLE_ARM_ROW = SourceId("ace-single-arm-row")
    val ACE_CABLE_SINGLE_ARM_ROW = SourceId("ace-cable-single-arm-row")
    val EXRX_CB_STRAIGHT_BACK_SEATED_ROW = SourceId("exrx-cable-straight-back-seated-row")
    val EXRX_CB_SEATED_ROW = SourceId("exrx-cable-seated-row")
    val EXRX_CB_WIDE_GRIP_STR_BACK_ROW = SourceId("exrx-cable-wide-grip-straight-back-seated-row")
    val EXRX_CB_WIDE_GRIP_SEATED_ROW = SourceId("exrx-cable-wide-grip-seated-row")
    val EXRX_CB_ONE_ARM_ROW = SourceId("exrx-cable-one-arm-seated-row")
    val ACE_SEATED_ROW = SourceId("ace-seated-row")
    val ACE_CABLE_SEATED_ROW = SourceId("ace-cable-seated-row")
    val EXRX_WT_45_HYPEREXTENSION = SourceId("exrx-weighted-45-hyperextension")
    val EXRX_LV_SEATED_ROW = SourceId("exrx-lever-seated-row")
    val EXRX_DB_INCLINE_ROW = SourceId("exrx-dumbbell-incline-row")
    val EXRX_DB_LYING_ROW = SourceId("exrx-dumbbell-lying-row")
    val EXRX_CAMBERED_LYING_ROW = SourceId("exrx-cambered-barbell-lying-row")
    val EXRX_LV_T_BAR_ROW = SourceId("exrx-lever-t-bar-row")
    val EXRX_LV_CLOSE_GRIP_T_BAR_ROW = SourceId("exrx-lever-close-grip-t-bar-row")
    val EXRX_LV_CLOSE_GRIP_T_BAR_ROW_LM = SourceId("exrx-lever-close-grip-t-bar-row-landmine")
    val EXRX_LV_INCLINE_ROW = SourceId("exrx-lever-incline-row")
    val EXRX_INVERTED_ROW = SourceId("exrx-inverted-row")
    val ACE_BENT_OVER_ROW = SourceId("ace-bent-over-row")
    val EXRX_CB_PULLDOWN = SourceId("exrx-cable-pulldown")
    val ACE_SEATED_LAT_PULLDOWN = SourceId("ace-seated-lat-pulldown")
    val EXRX_CB_CLOSE_GRIP_PULLDOWN = SourceId("exrx-cable-close-grip-pulldown")
    val EXRX_CB_UNDERHAND_PULLDOWN = SourceId("exrx-cable-underhand-pulldown")
    val EXRX_LV_FRONT_PULLDOWN = SourceId("exrx-lever-front-pulldown")
    val EXRX_CB_ONE_ARM_PULLDOWN = SourceId("exrx-cable-one-arm-pulldown")
    val EXRX_CB_SEATED_PULLOVER = SourceId("exrx-cable-seated-pullover")
    val EXRX_CHIN_UP = SourceId("exrx-chin-up")
    val EXRX_WEIGHTED_PULL_UP = SourceId("exrx-weighted-pull-up")
    val EXRX_WEIGHTED_CHIN_UP = SourceId("exrx-weighted-chin-up")
    val EXRX_ASSISTED_PULL_UP = SourceId("exrx-machine-assisted-pull-up")
    val EXRX_ASSISTED_CHIN_UP = SourceId("exrx-machine-assisted-chin-up")
    val EXRX_BAND_ASSISTED_PULL_UP = SourceId("exrx-band-assisted-pull-up")
    val EXRX_NEUTRAL_GRIP_PULL_UP = SourceId("exrx-neutral-grip-pull-up")
    val EXRX_PARALLEL_CLOSE_GRIP_PULL_UP = SourceId("exrx-parallel-close-grip-pull-up")
    val EXRX_DB_PULLOVER = SourceId("exrx-dumbbell-pullover")
    val EXRX_LV_PULLOVER = SourceId("exrx-lever-pullover")
    val EXRX_BB_SHRUG = SourceId("exrx-barbell-shrug")
    val EXRX_DB_SHRUG = SourceId("exrx-dumbbell-shrug")
    val EXRX_TRAP_BAR_SHRUG = SourceId("exrx-trap-bar-shrug")
    val EXRX_45_HYPEREXTENSION = SourceId("exrx-45-hyperextension-hands-behind-hips")
    val EXRX_WT_45_BACK_EXTENSION = SourceId("exrx-weighted-45-back-extension")
    val PMC_ROMAN_CHAIR = SourceId("pmc-roman-chair-pain-thresholds")
    val PMC_BACK_EXTENSION_LOADING = SourceId("pmc-back-extension-loading")
    val NSCA_FACE_PULL = SourceId("nsca-face-pull-machine")
    val EXRX_CB_REAR_DELT_ROW_ROPE = SourceId("exrx-cable-standing-rear-delt-row-rope")

    // Shoulders (L6)
    val EXRX_BB_MILITARY_PRESS = SourceId("exrx-barbell-military-press")
    val ACE_STANDING_SHOULDER_PRESS = SourceId("ace-standing-shoulder-press")
    val EXRX_BB_SEATED_MILITARY_PRESS = SourceId("exrx-barbell-seated-military-press")
    val ACE_SEATED_BB_SHOULDER_PRESS = SourceId("ace-seated-shoulder-press-barbell")
    val PMC_UPPER_EXTREMITY_LIFTING_INJURIES = SourceId("pmc-upper-extremity-weightlifting-injuries")
    val ACE_SEATED_OVERHEAD_PRESS = SourceId("ace-seated-overhead-press")
    val EXRX_DB_SHOULDER_PRESS = SourceId("exrx-dumbbell-shoulder-press")
    val EXRX_DB_ONE_ARM_SHOULDER_PRESS = SourceId("exrx-dumbbell-one-arm-shoulder-press")
    val EXRX_DB_ARNOLD_PRESS = SourceId("exrx-dumbbell-arnold-press")
    val EXRX_LV_SHOULDER_PRESS = SourceId("exrx-lever-shoulder-press")
    val ACE_SEATED_MACHINE_SHOULDER_PRESS = SourceId("ace-seated-shoulder-press-machine")
    val ACE_SEATED_MACHINE_CLOSE_GRIP_SHOULDER_PRESS = SourceId("ace-seated-machine-close-grip-shoulder-press")
    val EXRX_SM_SHOULDER_PRESS = SourceId("exrx-smith-shoulder-press")
    val EXRX_LV_ONE_ARM_SHOULDER_PRESS = SourceId("exrx-lever-one-arm-shoulder-press-landmine")
    val ACE_LANDMINE_WORKOUT_BUILDER = SourceId("ace-workout-builder-landmine-training")
    val ACE_SINGLE_ARM_OVERHEAD_PRESS = SourceId("ace-single-arm-overhead-press")
    val EXRX_DB_LATERAL_RAISE = SourceId("exrx-dumbbell-lateral-raise")
    val ACE_LATERAL_RAISE = SourceId("ace-lateral-raise")
    val EXRX_ERRORS = SourceId("exrx-common-resistance-training-mistakes")
    val EXRX_CB_ONE_ARM_LATERAL_RAISE = SourceId("exrx-cable-one-arm-lateral-raise")
    val EXRX_LV_LATERAL_RAISE = SourceId("exrx-lever-isolateral-lateral-raise")
    val EXRX_DB_SEATED_LATERAL_RAISE = SourceId("exrx-dumbbell-seated-lateral-raise")
    val EXRX_DB_FRONT_RAISE = SourceId("exrx-dumbbell-front-raise")
    val ACE_FRONT_RAISE = SourceId("ace-front-raise")
    val EXRX_BB_FRONT_RAISE = SourceId("exrx-barbell-front-raise")
    val EXRX_CB_ONE_ARM_FRONT_RAISE = SourceId("exrx-cable-one-arm-front-raise")
    val EXRX_DB_REAR_LATERAL_RAISE = SourceId("exrx-dumbbell-rear-lateral-raise")
    val EXRX_DB_SEATED_REAR_LATERAL_RAISE = SourceId("exrx-dumbbell-seated-rear-lateral-raise")
    val EXRX_DB_LYING_REAR_LATERAL_RAISE = SourceId("exrx-dumbbell-lying-rear-lateral-raise")
    val EXRX_LV_REVERSE_FLY = SourceId("exrx-lever-seated-reverse-fly-overhand")
    val EXRX_CB_REVERSE_FLY = SourceId("exrx-cable-isolateral-reverse-fly")
    val EXRX_CB_REAR_DELT_ROW = SourceId("exrx-cable-rear-delt-row")
    val EXRX_CB_EXTERNAL_ROTATION = SourceId("exrx-cable-standing-shoulder-external-rotation")
    val ACE_ROTATOR_CUFF_EXTERNAL_ROTATION = SourceId("ace-rotator-cuff-external-rotation")
    val EXRX_DB_LYING_EXTERNAL_ROTATION = SourceId("exrx-dumbbell-lying-shoulder-external-rotation")
    val EXRX_CB_INTERNAL_ROTATION = SourceId("exrx-cable-standing-shoulder-internal-rotation")

    // Arms (L7)
    val EXRX_DB_CURL = SourceId("exrx-dumbbell-alternating-curl")
    val EXRX_DB_SEATED_CURL = SourceId("exrx-dumbbell-seated-curl")
    val EXRX_DB_HAMMER_CURL = SourceId("exrx-dumbbell-alternating-hammer-curl")
    val ACE_HAMMER_CURL = SourceId("ace-hammer-curl")
    val EXRX_CB_HAMMER_CURL = SourceId("exrx-cable-hammer-curl")
    val EXRX_DB_INCLINE_CURL = SourceId("exrx-dumbbell-alternating-incline-curl")
    val EXRX_BB_PREACHER_CURL = SourceId("exrx-barbell-preacher-curl")
    val EXRX_DB_PREACHER_CURL = SourceId("exrx-dumbbell-one-arm-preacher-curl")
    val EXRX_LV_PREACHER_CURL = SourceId("exrx-lever-isolateral-preacher-curl-plate-loaded")
    val ACE_SEATED_BICEPS_CURL = SourceId("ace-seated-biceps-curl")
    val EXRX_CB_CURL = SourceId("exrx-cable-curl")
    val EXRX_CB_ONE_ARM_CURL = SourceId("exrx-cable-one-arm-curl")
    val EXRX_DB_CONCENTRATION_CURL = SourceId("exrx-dumbbell-concentration-curl")
    val EXRX_BB_DRAG_CURL = SourceId("exrx-barbell-drag-curl")
    val EXRX_BB_PRONE_INCLINE_CURL = SourceId("exrx-barbell-prone-incline-curl")
    val EXRX_BB_REVERSE_CURL = SourceId("exrx-barbell-reverse-curl")
    val ACE_REVERSE_BICEP_CURL = SourceId("ace-reverse-bicep-curl")
    val ACE_GRIP_STRENGTH = SourceId("ace-how-to-improve-grip-strength")
    val EXRX_CB_PUSHDOWN = SourceId("exrx-cable-pushdown")
    val EXRX_CB_PUSHDOWN_VBAR = SourceId("exrx-cable-pushdown-v-bar")
    val ACE_TRICEPS_PUSHDOWNS = SourceId("ace-triceps-pushdowns")
    val EXRX_CB_PUSHDOWN_ROPE = SourceId("exrx-cable-pushdown-rope")
    val EXRX_CB_ONE_ARM_PUSHDOWN = SourceId("exrx-cable-one-arm-pushdown")
    val EXRX_CB_TRI_EXT_ROPE = SourceId("exrx-cable-triceps-extension-rope")
    val ACE_TRICEPS_EXTENSION = SourceId("ace-triceps-extension")
    val EXRX_DB_TRI_EXT = SourceId("exrx-dumbbell-triceps-extension")
    val ACE_LYING_BB_TRI_EXT = SourceId("ace-lying-barbell-triceps-extensions")
    val EXRX_BB_SKULL_CRUSHER = SourceId("exrx-barbell-lying-triceps-extension-skull-crusher")
    val ACE_BARBELL_EXERCISES = SourceId("ace-9-exercises-with-a-barbell")
    val EXRX_DB_LYING_TRI_EXT = SourceId("exrx-dumbbell-lying-triceps-extension")
    val EXRX_DB_DECLINE_TRI_EXT = SourceId("exrx-dumbbell-decline-triceps-extension")
    val EXRX_LV_TRI_EXT = SourceId("exrx-lever-triceps-extension")
    val ACE_TRICEPS_KICKBACK = SourceId("ace-triceps-kickback")
    val EXRX_DB_KICKBACK = SourceId("exrx-dumbbell-kickback")
    val EXRX_WT_TRI_DIP = SourceId("exrx-weighted-triceps-dip")
    val EXRX_ST_TRI_DIP = SourceId("exrx-suspended-triceps-dip")
    val ACE_TONE_TRICEPS = SourceId("ace-tone-up-your-triceps")
    val ACE_DANGEROUS_DIPS = SourceId("ace-follow-up-dangerous-dips")
    val PMC_BENCH_BAR_RING_DIPS = SourceId("pmc-bench-bar-ring-dips")
    val EXRX_CLOSE_GRIP_PUSH_UP = SourceId("exrx-close-grip-push-up")
    val EXRX_BB_WRIST_CURL = SourceId("exrx-barbell-wrist-curl")
    val EXRX_DB_WRIST_CURL = SourceId("exrx-dumbbell-wrist-curl")
    val EXRX_BB_REVERSE_WRIST_CURL = SourceId("exrx-barbell-reverse-wrist-curl")
    val EXRX_DB_REVERSE_WRIST_CURL = SourceId("exrx-dumbbell-reverse-wrist-curl")

    val all: Map<SourceId, Source> = mapOf(
        IPF_RULES_2026 to Source(
            "IPF Technical Rulebook 2026", IPF,
            "https://www.powerlifting.sport/fileadmin/ipf/data/rules/technical-rules/english/2026_IPF_Technical_Rulebook__effective_01_March_2026__v3.pdf", L1,
        ),
        ACE_BACK_SQUAT to Source(
            "Back Squat Exercise Guide", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/11/back-squat/", L1,
        ),
        EXRX_BB_SQUAT to Source("Barbell Squat", EXRX, "https://exrx.net/WeightExercises/Quadriceps/BBSquat", L1),
        PMC_BACK_SQUAT_ASSESSMENT to Source(
            "The back squat: A proposed assessment of functional deficits and technical factors that limit performance", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC4262933/", L1,
        ),
        EXRX_BB_FRONT_SQUAT to Source("Barbell Front Squat", EXRX, "https://exrx.net/WeightExercises/Quadriceps/BBFrontSquat", L1),
        ACE_FRONT_SQUAT to Source(
            "Exercise Library: Front Squat", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/22/front-squat/", L1,
        ),
        ACE_FRONT_SQUAT_TO_PRESS to Source(
            "Front Squat to Overhead Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/315/front-squat-to-overhead-press/", L1,
        ),
        EXRX_SAFETY_BAR_SQUAT to Source("Safety Barbell Squat", EXRX, "https://exrx.net/WeightExercises/GluteusMaximus/SBSquat", L1),
        PMC_SAFETY_BAR_SQUAT to Source(
            "A Biomechanical Comparison of the Safety-Bar, High-Bar and Low-Bar Squat around the Sticking Region among " +
                "Recreationally Resistance-Trained Men and Women", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC8392107/", L1,
        ),
        EXRX_BB_BOX_SQUAT to Source("Barbell Box Squat", EXRX, "https://exrx.net/WeightExercises/GluteusMaximus/BBBoxSquat", L1),
        PMC_STICKING_POINT_REVIEW to Source(
            "Understanding and Overcoming the Sticking Point in Resistance Exercise", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC4887540/", L1,
        ),
        PMC_STICKING_POINT_THREE_LIFTS to Source(
            "The Sticking Point in the Bench Press, the Squat, and the Deadlift: Similarities and Differences, and Their " +
                "Significance for Research and Practice", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC5357260/", L1,
        ),
        EXRX_BB_BENCH to Source("Barbell Bench Press", EXRX, "https://exrx.net/WeightExercises/PectoralSternal/BBBenchPress", L1),
        EXRX_BB_BENCH_POWER to Source(
            "Barbell Bench Press (power lift)", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/BBBenchPressPowerLift", L1,
        ),
        EXRX_BENCH_ANALYSES to Source("Bench Press Analyses", EXRX, "https://exrx.net/Kinesiology/BenchPress", L1),
        PMC_PRESS_STICKING_REGION to Source(
            "Decoding the Contribution of Shoulder and Elbow Mechanics to Barbell Kinematics and the Sticking Region in " +
                "Bench and Overhead Press Exercises", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC12372072/", L1,
        ),
        PMC_BENCH_WITHOUT_LEGS to Source(
            "Neuromuscular Control During the Bench Press Movement in an Elite Disabled and Able-Bodied Athlete", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC5765801/", L1,
        ),
        ACE_CLOSE_GRIP_BENCH to Source(
            "Exercise Library: Close-grip Bench Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/311/close-grip-bench-press/", L1,
        ),
        EXRX_CLOSE_GRIP_BENCH to Source(
            "Barbell Close Grip Bench Press", EXRX, "https://exrx.net/WeightExercises/Triceps/BBCloseGripBenchPress", L1,
        ),
        EXRX_BOARD_PRESS to Source("Barbell Board Press", EXRX, "https://exrx.net/WeightExercises/PectoralSternal/BBBoardPress", L1),
        EXRX_BB_DEADLIFT to Source("Barbell Deadlift", EXRX, "https://exrx.net/WeightExercises/GluteusMaximus/BBDeadlift", L1),
        ACE_DEADLIFT to Source("Deadlift", ACE, "https://www.acefitness.org/resources/everyone/exercise-library/6/deadlift/", L1),
        ACE_HOW_TO_DEADLIFT to Source(
            "ProSource September 2015: Technique Series: How to Deadlift", ACE,
            "https://www.acefitness.org/continuing-education/prosource/september-2015/5579/technique-series-how-to-deadlift/", L1,
        ),
        EXRX_DEFICIT_DEADLIFT to Source(
            "Barbell Deficit Deadlift", EXRX, "https://exrx.net/WeightExercises/ErectorSpinae/BBDeficitDeadlift", L1,
        ),
        EXRX_BB_BLOCK_PULL to Source("Barbell Block Pull", EXRX, "https://exrx.net/WeightExercises/GluteusMaximus/BBBlockPull", L1),
        EXRX_RACK_PULL to Source("Barbell Rack Pull", EXRX, "https://exrx.net/WeightExercises/ErectorSpinae/BBRackPull", L1),
        EXRX_DB_SINGLE_LEG_SPLIT_SQUAT to Source(
            "Dumbbell Single Leg Split Squat", EXRX, "https://exrx.net/WeightExercises/Quadriceps/DBSingleLegSplitSquat", L1,
        ),
        PMC_BULGARIAN_SPLIT_SQUAT to Source(
            "Effects of rear-foot instability devices on lower-limb muscle activation during the Bulgarian split squat in " +
                "male football players", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC12819420/", L1,
        ),
        EXRX_HIP_THRUST to Source("Barbell Hip Thrust", EXRX, "https://exrx.net/WeightExercises/GluteusMaximus/BBHipThrust", L1),
        ACE_HIP_THRUST to Source(
            "Certified November 2019: Understanding the Barbell Hip Thrust", ACE,
            "https://www.acefitness.org/continuing-education/certified/november-2019/7403/understanding-the-barbell-hip-thrust/", L1,
        ),
        EXRX_DB_BENCH to Source("Dumbbell Bench Press", EXRX, "https://exrx.net/WeightExercises/PectoralSternal/DBBenchPress", L1),
        ACE_DB_CHEST_PRESS to Source(
            "How To Do a Chest Press", ACE, "https://www.acefitness.org/resources/everyone/exercise-library/19/chest-press/", L1,
        ),
        EXRX_JM_PRESS to Source("Barbell JM Bench Press", EXRX, "https://exrx.net/WeightExercises/Triceps/BBJMPress", L1),
        EXRX_RDL to Source("Romanian Deadlift", EXRX, "https://exrx.net/WeightExercises/OlympicLifts/RomanianDeadlift", L1),
        ACE_RDL to Source(
            "Exercise Library: Romanian Deadlift", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/317/romanian-deadlift/", L1,
        ),
        NSCA_RDL to Source(
            "Romanian Deadlift (RDL)", NSCA, "https://www.nsca.com/education/articles/kinetic-select/romanian-deadlift-rdl/", L1,
        ),
        EXRX_BB_CURL to Source("Barbell Curl", EXRX, "https://exrx.net/WeightExercises/Biceps/BBCurl", L1),
        ACE_BICEP_CURL to Source(
            "Exercise Library: Bicep Curl", ACE, "https://www.acefitness.org/resources/everyone/exercise-library/70/bicep-curl/", L1,
        ),
        EXRX_BB_ROW to Source("Barbell Bent-over Row", EXRX, "https://exrx.net/WeightExercises/BackGeneral/BBBentOverRow", L1),
        EXRX_PULL_UP to Source("Pull-up", EXRX, "https://exrx.net/WeightExercises/LatissimusDorsi/BWPullup", L1),
        ACE_PULL_UPS to Source(
            "Exercise Library: Pull-ups", ACE, "https://www.acefitness.org/resources/everyone/exercise-library/191/pull-ups/", L1,
        ),
        EXRX_BB_INCLINE_BENCH to Source(
            "Barbell Incline Bench Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralClavicular/BBInclineBenchPress", L4,
        ),
        EXRX_BB_DECLINE_BENCH to Source(
            "Barbell Decline Bench Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/BBDeclineBenchPress", L4,
        ),
        EXRX_DB_INCLINE_BENCH to Source(
            "Dumbbell Incline Bench Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralClavicular/DBInclineBenchPress", L4,
        ),
        EXRX_DB_DECLINE_BENCH to Source(
            "Dumbbell Decline Bench Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/DBDeclineBenchPress", L4,
        ),
        EXRX_DB_FLY to Source(
            "Dumbbell Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/DBFly", L4,
        ),
        EXRX_DB_INCLINE_FLY to Source(
            "Dumbbell Incline Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralClavicular/DBInclineFly", L4,
        ),
        EXRX_DB_DECLINE_FLY to Source(
            "Dumbbell Decline Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/DBDeclineFly", L4,
        ),
        EXRX_LV_CHEST_PRESS to Source(
            "Lever Chest Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/LVChestPress", L4,
        ),
        EXRX_LV_INCLINE_CHEST_PRESS to Source(
            "Lever Incline Chest Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralClavicular/LVInclineChestPress", L4,
        ),
        EXRX_LV_DECLINE_CHEST_PRESS to Source(
            "Lever Decline Chest Press (plate loaded)", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/LVDeclineChestPressPL", L4,
        ),
        EXRX_LV_SEATED_FLY to Source(
            "Lever Isolateral Seated Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/LVSeatedFly", L4,
        ),
        EXRX_LV_PEC_DECK to Source(
            "Lever Isolateral Pec Deck Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/LVPecDeckFly", L4,
        ),
        EXRX_SM_BENCH to Source(
            "Smith Bench Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/SMBenchPress", L4,
        ),
        EXRX_SM_INCLINE_BENCH to Source(
            "Smith Incline Bench Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralClavicular/SMInclineBenchPress", L4,
        ),
        EXRX_SM_DECLINE_BENCH to Source(
            "Smith Decline Bench Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/SMDeclineBenchPress", L4,
        ),
        EXRX_GYM_ETIQUETTE to Source(
            "Gym Etiquette", EXRX,
            "https://exrx.net/ExInfo/Etiquette", L4,
        ),
        EXRX_CB_STANDING_FLY to Source(
            "Cable Isolateral Standing Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/CBStandingFly", L4,
        ),
        EXRX_CB_STANDING_INCLINE_FLY to Source(
            "Cable Standing Incline Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralClavicular/CBStandingInclineFly", L4,
        ),
        EXRX_CB_STANDING_CHEST_PRESS to Source(
            "Cable Isolateral Standing Chest Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/CBStandingChestPress", L4,
        ),
        EXRX_CB_LYING_FLY to Source(
            "Cable Isolateral Lying Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/CBLyingFly", L4,
        ),
        EXRX_CB_INCLINE_FLY to Source(
            "Cable Isolateral Incline Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralClavicular/CBInclineFly", L4,
        ),
        EXRX_CB_SEATED_FLY to Source(
            "Cable Isolateral Cable Fly", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/CBSeatedFly", L4,
        ),
        EXRX_CHEST_DIP to Source(
            "Chest Dip", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/BWChestDip", L4,
        ),
        EXRX_WEIGHTED_CHEST_DIP to Source(
            "Weighted Chest Dip", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/WtChestDip", L4,
        ),
        EXRX_ASSISTED_CHEST_DIP to Source(
            "Machine-assisted Chest Dip", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/AsChestDip", L4,
        ),
        EXRX_ASSISTED_CHEST_DIP_KNEELING to Source(
            "Machine-assisted Chest Dip (kneeling)", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/AsChestDipKneeling", L4,
        ),
        EXRX_ACTUAL_RESISTANCE to Source(
            "Calculating Actual Resistance", EXRX,
            "https://exrx.net/WeightTraining/Bodyweight", L4,
        ),
        EXRX_PUSH_UP to Source(
            "Push-up", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/BWPushup", L4,
        ),
        EXRX_INCLINE_PUSH_UP to Source(
            "Incline Push-up", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/BWInclinePushup", L4,
        ),
        EXRX_DECLINE_PUSH_UP to Source(
            "Decline Push-up", EXRX,
            "https://exrx.net/WeightExercises/PectoralClavicular/BWDeclinePushup", L4,
        ),
        EXRX_KNEE_PUSH_UP to Source(
            "Push-up (on knees)", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/BWPushupKnee", L4,
        ),
        EXRX_ARCHER_PUSH_UP to Source(
            "Archer Push-up", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/BWArcherPushup", L4,
        ),
        EXRX_CLAP_PUSH_UP to Source(
            "Clap Push-up", EXRX,
            "https://exrx.net/Plyometrics/ClapPushUp", L4,
        ),
        EXRX_WEIGHTED_PUSH_UP to Source(
            "Weighted Push-up", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/WtPushup", L4,
        ),
        EXRX_WEIGHTED_PUSH_UP_HANDLES to Source(
            "Weighted Push-up (on handles)", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/WtPushupHandles", L4,
        ),
        ACE_INCLINE_CHEST_PRESS to Source(
            "Exercise Library: Incline Chest Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/25/incline-chest-press/", L4,
        ),
        ACE_LYING_CHEST_FLY to Source(
            "Exercise Library: Lying Chest Fly", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/21/lying-chest-fly/", L4,
        ),
        ACE_SEATED_CHEST_PRESS to Source(
            "Seated Chest Press Exercise Guide", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/188/seated-chest-press/", L4,
        ),
        ACE_SEATED_CABLE_PRESS to Source(
            "Exercise Library: Seated Cable Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/165/seated-cable-press/", L4,
        ),
        ACE_STANDING_CHEST_FLY to Source(
            "Exercise Library: Standing Chest Fly", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/160/standing-chest-fly/", L4,
        ),
        ACE_STANDING_INCLINE_CABLE_FLYES to Source(
            "Exercise Library: Standing Incline Cable Flyes", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/163/standing-incline-cable-flyes/", L4,
        ),
        ACE_PUSH_UP to Source(
            "Exercise Library: Push-ups", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/41/push-up/", L4,
        ),
        ACE_BENT_KNEE_PUSH_UP to Source(
            "Exercise Library: Bent-knee Push-up", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/13/bent-knee-push-up/", L4,
        ),
        ACE_BRACE_OR_BREATHE to Source(
            "Certified November 2025: Spinal Stability in the Real World: When to Brace and When to Breathe", ACE,
            "https://www.acefitness.org/continuing-education/certified/november-2025/9006/spinal-stability-in-the-real-world-when-to-brace-and-when-to-breathe/", L4,
        ),
        NSCA_STANDARDS_SUMMARY to Source(
            "Strength and Conditioning Professional Standards and Guidelines: Summary (rev. Dec. 2018)", NSCA,
            "https://www.nsca.com/contentassets/477221070254471aab20116550658891/standards-and-guidelines-summary-rev.-dec.-2018.pdf", L4,
        ),
        ACE_SEATED_CLOSE_GRIP_CHEST_PRESS to Source(
            "Exercise Library: Seated Close-Grip Chest Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/189/seated-close-grip-chest-press/", L4,
        ),
        EXRX_BB_FLOOR_PRESS to Source(
            "Barbell Floor Press", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/BBFloorPress", L4,
        ),
        PMC_GRIP_BENCH_VS_SEATED_PRESS to Source(
            "Comparison of Muscle Activity between the Horizontal Bench Press and the Seated Chest Press Exercises Using Several Grips", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC10203828/", L4,
        ),
        PMC_GRIP_WIDTH_NOVICE_TRAINED to Source(
            "The Effect of Grip Width on Muscle Strength and Electromyographic Activity in Bench Press among Novice- and Resistance-Trained Men", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC8296276/", L4,
        ),
        EXRX_DB_BENT_OVER_ROW to Source(
            "Dumbbell Bent-over Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/DBBentOverRow", L5,
        ),
        ACE_SINGLE_ARM_ROW to Source(
            "Single Arm Row Exercise Guide", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/126/single-arm-row/", L5,
        ),
        ACE_CABLE_SINGLE_ARM_ROW to Source(
            "Exercise Library: Single Arm Row (cable)", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/337/single-arm-row/", L5,
        ),
        EXRX_CB_STRAIGHT_BACK_SEATED_ROW to Source(
            "Cable Straight Back Seated Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/CBStraightBackSeatedRow", L5,
        ),
        EXRX_CB_SEATED_ROW to Source(
            "Cable Seated Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/CBSeatedRow", L5,
        ),
        EXRX_CB_WIDE_GRIP_STR_BACK_ROW to Source(
            "Cable Wide Grip Straight Back Seated Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/CBWideGripStrBackSeatedRow", L5,
        ),
        EXRX_CB_WIDE_GRIP_SEATED_ROW to Source(
            "Cable Wide Grip Seated Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/CBWideGripSeatedRow", L5,
        ),
        EXRX_CB_ONE_ARM_ROW to Source(
            "Cable One Arm Seated Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/CBOneArmRow", L5,
        ),
        ACE_SEATED_ROW to Source(
            "Seated Row", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/168/seated-row/", L5,
        ),
        EXRX_LV_SEATED_ROW to Source(
            "Lever Seated Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/LVSeatedRow", L5,
        ),
        EXRX_DB_INCLINE_ROW to Source(
            "Dumbbell Incline Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/DBInclineRow", L5,
        ),
        EXRX_DB_LYING_ROW to Source(
            "Dumbbell Lying Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/DBLyingRow", L5,
        ),
        EXRX_CAMBERED_LYING_ROW to Source(
            "Cambered Barbell Lying Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/CamberedBarLyingRow", L5,
        ),
        EXRX_LV_T_BAR_ROW to Source(
            "Lever T-bar Row (plate loaded)", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/LVTBarRow", L5,
        ),
        EXRX_LV_CLOSE_GRIP_T_BAR_ROW to Source(
            "Lever Close Grip T-bar Row (plate loaded)", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/LVCloseGripTBarRow", L5,
        ),
        EXRX_LV_CLOSE_GRIP_T_BAR_ROW_LM to Source(
            "Lever Close Grip T-bar Row (plate loaded, landmine)", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/LVCloseGripTBarRowLM", L5,
        ),
        EXRX_LV_INCLINE_ROW to Source(
            "Lever Incline Row (plate loaded)", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/LVInclineRow", L5,
        ),
        EXRX_INVERTED_ROW to Source(
            "Inverted Row", EXRX,
            "https://exrx.net/WeightExercises/BackGeneral/BWSupineRow", L5,
        ),
        ACE_BENT_OVER_ROW to Source(
            "Bent-Over Row", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/12/bent-over-row/", L5,
        ),
        EXRX_CB_PULLDOWN to Source(
            "Cable Pulldown", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/CBFrontPulldown", L5,
        ),
        ACE_SEATED_LAT_PULLDOWN to Source(
            "Seated Lat Pulldown", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/158/seated-lat-pulldown/", L5,
        ),
        EXRX_CB_CLOSE_GRIP_PULLDOWN to Source(
            "Cable Close Grip Pulldown", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/CBCloseGripPulldown", L5,
        ),
        EXRX_CB_UNDERHAND_PULLDOWN to Source(
            "Cable Underhand Pulldown", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/CBUnderhandPulldown", L5,
        ),
        EXRX_LV_FRONT_PULLDOWN to Source(
            "Lever Front Pulldown", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/LVFrontPulldown", L5,
        ),
        EXRX_CB_ONE_ARM_PULLDOWN to Source(
            "Cable One Arm Pulldown", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/CBOneArmPulldown", L5,
        ),
        EXRX_CB_SEATED_PULLOVER to Source(
            "Cable Seated Pullover (straight-arm pulldown)", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/CBSeatedStraightArmPullover", L5,
        ),
        EXRX_CHIN_UP to Source(
            "Chin-up", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/BWUnderhandChinup", L5,
        ),
        EXRX_WEIGHTED_PULL_UP to Source(
            "Weighted Pull-up", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/WtPullup", L5,
        ),
        EXRX_WEIGHTED_CHIN_UP to Source(
            "Weighted Chin-up", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/WtUnderhandChinup", L5,
        ),
        EXRX_ASSISTED_PULL_UP to Source(
            "Machine-assisted Pull-up", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/AsPullupKneeling", L5,
        ),
        EXRX_ASSISTED_CHIN_UP to Source(
            "Machine-assisted Chin-up", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/AsUnderhandChinup", L5,
        ),
        EXRX_BAND_ASSISTED_PULL_UP to Source(
            "Band-assisted Pull-up", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/ASPullupBand", L5,
        ),
        EXRX_NEUTRAL_GRIP_PULL_UP to Source(
            "Neutral Grip Pull-up", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/BWPullupN", L5,
        ),
        EXRX_PARALLEL_CLOSE_GRIP_PULL_UP to Source(
            "Parallel Close Grip Pull-up", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/BWCloseGripChinup", L5,
        ),
        EXRX_DB_PULLOVER to Source(
            "Dumbbell Pullover", EXRX,
            "https://exrx.net/WeightExercises/PectoralSternal/DBPullover", L5,
        ),
        EXRX_LV_PULLOVER to Source(
            "Lever Pullover", EXRX,
            "https://exrx.net/WeightExercises/LatissimusDorsi/LVPullover", L5,
        ),
        EXRX_BB_SHRUG to Source(
            "Barbell Shrug", EXRX,
            "https://exrx.net/WeightExercises/TrapeziusUpper/BBShrug", L5,
        ),
        EXRX_DB_SHRUG to Source(
            "Dumbbell Shrug", EXRX,
            "https://exrx.net/WeightExercises/TrapeziusUpper/DBShrug", L5,
        ),
        EXRX_TRAP_BAR_SHRUG to Source(
            "Trap Bar Shrug", EXRX,
            "https://exrx.net/WeightExercises/TrapeziusUpper/TBShrug", L5,
        ),
        EXRX_45_HYPEREXTENSION to Source(
            "45° Hyperextension (hands behind hips)", EXRX,
            "https://exrx.net/WeightExercises/ErectorSpinae/BW45HyperextensionHips", L5,
        ),
        EXRX_WT_45_BACK_EXTENSION to Source(
            "Weighted 45° Back Extension (on hyperextension apparatus)", EXRX,
            "https://exrx.net/WeightExercises/ErectorSpinae/Wt45BackExtension", L5,
        ),
        PMC_ROMAN_CHAIR to Source(
            "Reliability of Pain Thresholds and Exercise-Induced Hypoalgesia Following 45-Degree Roman Chair Exercise", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC11964695/", L5,
        ),
        PMC_BACK_EXTENSION_LOADING to Source(
            "Loading conditions in the spine, hip and knee during different executions of back extension exercises", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC5404334/", L5,
        ),
        NSCA_FACE_PULL to Source(
            "Face Pull (Machine)", NSCA,
            "https://www.nsca.com/education/articles/kinetic-select/face-pull-machine/", L5,
        ),
        EXRX_CB_REAR_DELT_ROW_ROPE to Source(
            "Cable Standing Rear Delt Row (with rope)", EXRX,
            "https://exrx.net/WeightExercises/DeltoidPosterior/CBStandingRearDeltRowRope", L5,
        ),
        ACE_CABLE_SEATED_ROW to Source(
            "Seated Row (cable)", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/48/seated-row/", L5,
        ),
        EXRX_WT_45_HYPEREXTENSION to Source(
            "Weighted 45° Hyperextension", EXRX,
            "https://exrx.net/WeightExercises/ErectorSpinae/Wt45Hyperextension", L5,
        ),
        EXRX_BB_MILITARY_PRESS to Source(
            "Barbell Military Press", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/BBMilitaryPress", L6,
        ),
        ACE_STANDING_SHOULDER_PRESS to Source(
            "Exercise Library: Standing Shoulder Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/71/standing-shoulder-press/", L6,
        ),
        EXRX_BB_SEATED_MILITARY_PRESS to Source(
            "Barbell Seated Military Press", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/BBSeatedMilitaryPress", L6,
        ),
        ACE_SEATED_BB_SHOULDER_PRESS to Source(
            "Exercise Library: Seated Shoulder Press (barbell)", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/43/seated-shoulder-press/", L6,
        ),
        PMC_UPPER_EXTREMITY_LIFTING_INJURIES to Source(
            "Upper extremity weightlifting injuries: Diagnosis and management", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC5895929/", L6,
        ),
        ACE_SEATED_OVERHEAD_PRESS to Source(
            "Seated Overhead Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/45/seated-overhead-press/", L6,
        ),
        EXRX_DB_SHOULDER_PRESS to Source(
            "Dumbbell Shoulder Press", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/DBShoulderPress", L6,
        ),
        EXRX_DB_ONE_ARM_SHOULDER_PRESS to Source(
            "Dumbbell One Arm Shoulder Press", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/DBOneArmShoulderPress", L6,
        ),
        EXRX_DB_ARNOLD_PRESS to Source(
            "Dumbbell Arnold Press", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/DBArnoldPress", L6,
        ),
        EXRX_LV_SHOULDER_PRESS to Source(
            "Lever Shoulder Press", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/LVShoulderPress", L6,
        ),
        ACE_SEATED_MACHINE_SHOULDER_PRESS to Source(
            "Seated Shoulder Press (machine)", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/186/seated-shoulder-press/", L6,
        ),
        ACE_SEATED_MACHINE_CLOSE_GRIP_SHOULDER_PRESS to Source(
            "Exercise Library: Seated Machine Close-Grip Shoulder Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/187/seated-machine-close-grip-shoulder-press/", L6,
        ),
        EXRX_SM_SHOULDER_PRESS to Source(
            "Smith Shoulder Press", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/SMShoulderPress", L6,
        ),
        ACE_LANDMINE_WORKOUT_BUILDER to Source(
            "Certified September 2025: The ACE Workout Builder for Landmine Training", ACE,
            "https://www.acefitness.org/continuing-education/certified/september-2025/8948/the-ace-workout-builder-for-landmine-training/", L6,
        ),
        EXRX_LV_ONE_ARM_SHOULDER_PRESS to Source(
            "Lever One Arm Shoulder Press (plate loaded)", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/LVOneArmShoulderPress", L6,
        ),
        ACE_SINGLE_ARM_OVERHEAD_PRESS to Source(
            "Single-arm Overhead Press", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/395/single-arm-overhead-press/", L6,
        ),
        EXRX_DB_LATERAL_RAISE to Source(
            "Dumbbell Lateral Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidLateral/DBLateralRaise", L6,
        ),
        ACE_LATERAL_RAISE to Source(
            "Lateral Raise", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/26/lateral-raise/", L6,
        ),
        EXRX_ERRORS to Source(
            "Common Resistance Training Mistakes", EXRX,
            "https://exrx.net/Kinesiology/Errors", L6,
        ),
        EXRX_CB_ONE_ARM_LATERAL_RAISE to Source(
            "Cable One Arm Lateral Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidLateral/CBOneArmLateralRaise", L6,
        ),
        EXRX_DB_SEATED_LATERAL_RAISE to Source(
            "Dumbbell Seated Lateral Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidLateral/DBSeatedLateralRaise", L6,
        ),
        EXRX_LV_LATERAL_RAISE to Source(
            "Lever Isolateral Lateral Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidLateral/LVLateralRaise", L6,
        ),
        EXRX_DB_FRONT_RAISE to Source(
            "Dumbbell Front Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/DBFrontRaise", L6,
        ),
        ACE_FRONT_RAISE to Source(
            "Front Raise", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/54/front-raise/", L6,
        ),
        EXRX_BB_FRONT_RAISE to Source(
            "Barbell Front Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/BBFrontRaise", L6,
        ),
        EXRX_CB_ONE_ARM_FRONT_RAISE to Source(
            "Cable One Arm Front Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidAnterior/CBFrontRaise", L6,
        ),
        EXRX_DB_REAR_LATERAL_RAISE to Source(
            "Dumbbell Rear Lateral Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidPosterior/DBRearLateralRaise", L6,
        ),
        EXRX_DB_SEATED_REAR_LATERAL_RAISE to Source(
            "Dumbbell Seated Rear Lateral Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidPosterior/DBSeatedRearLateralRaise", L6,
        ),
        EXRX_DB_LYING_REAR_LATERAL_RAISE to Source(
            "Dumbbell Lying Rear Lateral Raise", EXRX,
            "https://exrx.net/WeightExercises/DeltoidPosterior/DBLyingRearLateralRaise", L6,
        ),
        EXRX_LV_REVERSE_FLY to Source(
            "Lever Seated Reverse Fly (overhand grip)", EXRX,
            "https://exrx.net/WeightExercises/DeltoidPosterior/LVReverseFlyOverhand", L6,
        ),
        EXRX_CB_REVERSE_FLY to Source(
            "Cable Isolateral Reverse Fly", EXRX,
            "https://exrx.net/WeightExercises/DeltoidPosterior/CBStandingReverseFly", L6,
        ),
        EXRX_CB_REAR_DELT_ROW to Source(
            "Cable Rear Delt Row", EXRX,
            "https://exrx.net/WeightExercises/DeltoidPosterior/CBRearDeltRow", L6,
        ),
        EXRX_CB_EXTERNAL_ROTATION to Source(
            "Cable Standing Shoulder External Rotation", EXRX,
            "https://exrx.net/WeightExercises/Infraspinatus/CBStandingExternalRotation", L6,
        ),
        ACE_ROTATOR_CUFF_EXTERNAL_ROTATION to Source(
            "Rotator Cuff External Rotation", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/352/rotator-cuff-external-rotation/", L6,
        ),
        EXRX_DB_LYING_EXTERNAL_ROTATION to Source(
            "Dumbbell Lying Shoulder External Rotation", EXRX,
            "https://exrx.net/WeightExercises/Infraspinatus/DBLyingExternalRotation", L6,
        ),
        EXRX_CB_INTERNAL_ROTATION to Source(
            "Cable Standing Shoulder Internal Rotation", EXRX,
            "https://exrx.net/WeightExercises/Subscapularis/CBStandingInternalRotation", L6,
        ),
        EXRX_DB_CURL to Source(
            "Dumbbell Alternating Curl", EXRX,
            "https://exrx.net/WeightExercises/Biceps/DBCurl", L7,
        ),
        EXRX_DB_SEATED_CURL to Source(
            "Dumbbell Seated Curl", EXRX,
            "https://exrx.net/WeightExercises/Biceps/DBSeatedCurl", L7,
        ),
        EXRX_DB_HAMMER_CURL to Source(
            "Dumbbell Alternating Hammer Curl", EXRX,
            "https://exrx.net/WeightExercises/Brachioradialis/DBHammerCurl", L7,
        ),
        ACE_HAMMER_CURL to Source(
            "Exercise Library: Hammer Curl", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/10/hammer-curl/", L7,
        ),
        EXRX_CB_HAMMER_CURL to Source(
            "Cable Hammer Curl", EXRX,
            "https://exrx.net/WeightExercises/Brachioradialis/CBHammerCurl", L7,
        ),
        EXRX_DB_INCLINE_CURL to Source(
            "Dumbbell Alternating Incline Curl", EXRX,
            "https://exrx.net/WeightExercises/Biceps/DBInclineCurl", L7,
        ),
        EXRX_BB_PREACHER_CURL to Source(
            "Barbell Preacher Curl", EXRX,
            "https://exrx.net/WeightExercises/Brachialis/BBPreacherCurl", L7,
        ),
        EXRX_DB_PREACHER_CURL to Source(
            "Dumbbell One Arm Preacher Curl", EXRX,
            "https://exrx.net/WeightExercises/Brachialis/DBPreacherCurl", L7,
        ),
        EXRX_LV_PREACHER_CURL to Source(
            "Lever Isolateral Preacher Curl (plate loaded)", EXRX,
            "https://exrx.net/WeightExercises/Brachialis/LVPreacherCurlH", L7,
        ),
        ACE_SEATED_BICEPS_CURL to Source(
            "Exercise Library: Seated Biceps Curl", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/184/seated-biceps-curl/", L7,
        ),
        EXRX_CB_CURL to Source(
            "Cable Curl", EXRX,
            "https://exrx.net/WeightExercises/Biceps/CBCurl", L7,
        ),
        EXRX_CB_ONE_ARM_CURL to Source(
            "Cable One Arm Curl", EXRX,
            "https://exrx.net/WeightExercises/Biceps/CBOneArmCurl", L7,
        ),
        EXRX_DB_CONCENTRATION_CURL to Source(
            "Dumbbell Concentration Curl", EXRX,
            "https://exrx.net/WeightExercises/Brachialis/DBConcentrationCurl", L7,
        ),
        EXRX_BB_DRAG_CURL to Source(
            "Barbell Drag Curl", EXRX,
            "https://exrx.net/WeightExercises/Biceps/BBDragCurl", L7,
        ),
        EXRX_BB_PRONE_INCLINE_CURL to Source(
            "Barbell Prone Incline Curl", EXRX,
            "https://exrx.net/WeightExercises/Brachialis/BBProneInclineCurl", L7,
        ),
        EXRX_BB_REVERSE_CURL to Source(
            "Barbell Reverse Curl", EXRX,
            "https://exrx.net/WeightExercises/Brachioradialis/BBReverseCurl", L7,
        ),
        ACE_REVERSE_BICEP_CURL to Source(
            "Exercise Library: Reverse Bicep Curl", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/310/reverse-bicep-curl/", L7,
        ),
        ACE_GRIP_STRENGTH to Source(
            "How to Improve Grip Strength", ACE,
            "https://www.acefitness.org/resources/pros/expert-articles/5765/how-to-improve-grip-strength/", L7,
        ),
        EXRX_CB_PUSHDOWN to Source(
            "Cable Pushdown", EXRX,
            "https://exrx.net/WeightExercises/Triceps/CBPushdown", L7,
        ),
        EXRX_CB_PUSHDOWN_VBAR to Source(
            "Cable Pushdown (with V-bar)", EXRX,
            "https://exrx.net/WeightExercises/Triceps/CBPushdownVBar", L7,
        ),
        ACE_TRICEPS_PUSHDOWNS to Source(
            "Exercise Library: Triceps Pushdowns", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/185/triceps-pushdowns/", L7,
        ),
        EXRX_CB_PUSHDOWN_ROPE to Source(
            "Cable Pushdown (with rope attachment)", EXRX,
            "https://exrx.net/WeightExercises/Triceps/CBPushdownRope", L7,
        ),
        EXRX_CB_ONE_ARM_PUSHDOWN to Source(
            "Cable One Arm Pushdown", EXRX,
            "https://exrx.net/WeightExercises/Triceps/CBOneArmPushdown", L7,
        ),
        EXRX_CB_TRI_EXT_ROPE to Source(
            "Cable Triceps Extension (with rope)", EXRX,
            "https://exrx.net/WeightExercises/Triceps/CBStandingTricepsExtensionRope", L7,
        ),
        ACE_TRICEPS_EXTENSION to Source(
            "Exercise Library: Triceps Extension", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/74/triceps-extension/", L7,
        ),
        EXRX_DB_TRI_EXT to Source(
            "Dumbbell Triceps Extension", EXRX,
            "https://exrx.net/WeightExercises/Triceps/DBTriExt", L7,
        ),
        ACE_LYING_BB_TRI_EXT to Source(
            "Exercise Library: Lying Barbell Triceps Extensions", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/36/lying-barbell-triceps-extensions/", L7,
        ),
        EXRX_BB_SKULL_CRUSHER to Source(
            "Barbell Lying Triceps Extension \"Skull Crusher\"", EXRX,
            "https://exrx.net/WeightExercises/Triceps/BBLyingTriExtSC", L7,
        ),
        ACE_BARBELL_EXERCISES to Source(
            "9 Exercises You Didn't Know You Could Do With a Barbell", ACE,
            "https://www.acefitness.org/resources/everyone/blog/5366/9-exercises-you-didn-t-know-you-could-do-with-a-barbell/", L7,
        ),
        EXRX_DB_LYING_TRI_EXT to Source(
            "Dumbbell Lying Triceps Extension", EXRX,
            "https://exrx.net/WeightExercises/Triceps/DBLyingTriExt", L7,
        ),
        EXRX_LV_TRI_EXT to Source(
            "Lever Triceps Extension", EXRX,
            "https://exrx.net/WeightExercises/Triceps/LVTriExt", L7,
        ),
        ACE_TRICEPS_KICKBACK to Source(
            "Exercise Library: Triceps Kickback", ACE,
            "https://www.acefitness.org/resources/everyone/exercise-library/55/triceps-kickback/", L7,
        ),
        EXRX_DB_KICKBACK to Source(
            "Dumbbell Kickback", EXRX,
            "https://exrx.net/WeightExercises/Triceps/DBKickback", L7,
        ),
        EXRX_WT_TRI_DIP to Source(
            "Weighted Triceps Dip", EXRX,
            "https://exrx.net/WeightExercises/Triceps/WtTriDip", L7,
        ),
        EXRX_ST_TRI_DIP to Source(
            "Suspended Triceps Dip", EXRX,
            "https://exrx.net/WeightExercises/Triceps/STTriDip", L7,
        ),
        ACE_TONE_TRICEPS to Source(
            "Tone Up Your Triceps with These Three Exercises", ACE,
            "https://www.acefitness.org/resources/everyone/blog/4930/tone-up-your-triceps-with-these-three-exercises/", L7,
        ),
        ACE_DANGEROUS_DIPS to Source(
            "Follow-up Q and A: Dangerous Dips", ACE,
            "https://www.acefitness.org/certifiednewsarticle/1657/follow-up-q-and-a-dangerous-dips/", L7,
        ),
        EXRX_CLOSE_GRIP_PUSH_UP to Source(
            "Close Grip Push-up", EXRX,
            "https://exrx.net/WeightExercises/Triceps/BWCloseGripPushup", L7,
        ),
        EXRX_BB_WRIST_CURL to Source(
            "Barbell Wrist Curl", EXRX,
            "https://exrx.net/WeightExercises/WristFlexors/BBWristCurl", L7,
        ),
        EXRX_DB_WRIST_CURL to Source(
            "Dumbbell Wrist Curl", EXRX,
            "https://exrx.net/WeightExercises/WristFlexors/DBWristCurl", L7,
        ),
        EXRX_BB_REVERSE_WRIST_CURL to Source(
            "Barbell Reverse Wrist Curl", EXRX,
            "https://exrx.net/WeightExercises/WristExtensors/BBReverseWristCurl", L7,
        ),
        EXRX_DB_REVERSE_WRIST_CURL to Source(
            "Dumbbell Reverse Wrist Curl", EXRX,
            "https://exrx.net/WeightExercises/WristExtensors/DBReverseWristCurl", L7,
        ),
        EXRX_DB_DECLINE_TRI_EXT to Source(
            "Dumbbell Decline Triceps Extension", EXRX,
            "https://exrx.net/WeightExercises/Triceps/DBDeclineTriExt", L7,
        ),
        PMC_BENCH_BAR_RING_DIPS to Source(
            "Bench, Bar, and Ring Dips: Do Kinematics and Muscle Activity Differ?", PMC,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC9603242/", L7,
        ),
    )
}
