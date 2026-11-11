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
    )
}
