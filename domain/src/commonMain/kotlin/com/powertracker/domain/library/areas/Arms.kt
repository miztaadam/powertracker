package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

// Shared lines, so the bracing wording is the same in every arm guide. OVER_FACE_SPOT is in Shared.kt.
private const val STAND_BRACE = "Stand tall and brace your abs, so your body stays still."
private const val SEATED_BRACE = "Sit tall and brace your abs lightly, and keep breathing."
private const val LIGHT_BRACE = "Brace your abs lightly, and keep breathing."
private const val CABLE_BRACE = "Brace your abs, and pull your shoulder blades down and back."
private const val BODY_BRACE = "Brace your trunk and keep your body in one straight line."
private const val ELBOWS_AT_SIDES = "Elbows at your sides."
private const val LOWER_TO_STRAIGHT = "Lower under control until your arms are straight."
private const val CURL_TO_VERTICAL = "Bend your elbows to curl the weight up until your forearms are upright."
private const val SWITCH_ARMS = "Do all your reps, then switch arms."
private const val SWINGING = "Swinging your body to lift the weight."
private const val ELBOWS_FORWARD = "Letting your elbows drift forward."
private const val DROPPING = "Dropping the weight on the way down."
private const val HALF_REPS = "Stopping short of straight arms at the bottom."
private const val ARM_ON_PAD = "Keep the back of your upper arm on the pad the whole time."
private const val PAD_SEAT = "Set the seat so your armpit rests near the top of the pad."
private const val PUSHDOWN_ELBOWS = "Letting your elbows drift forward. It brings in your back muscles and takes work off your triceps."
private const val PUSHDOWN_RETURN = "Let the weight come back up slowly. Stop before your elbows start to move forward."
private const val STAY_CLOSE = "Stand close to the cable."
private const val LOCKING = "Locking your elbows hard at the bottom. Stop just short."
private const val TOO_FAR = "Standing too far from the cable, which takes the load off at the top."
private const val ELBOWS_LOW = "Letting your elbows sit well below your wrists, which takes the load off part of the range."
private const val WRIST_SEAT = "Sit and rest your forearms on your thighs, with your wrists just past your knees."

private val BICEPS = listOf("Biceps", "Brachialis")
private val BRACHIALIS = listOf("Brachialis", "Biceps")
private val HAMMER = listOf("Forearms", "Brachialis", "Biceps")
private val TRICEPS = listOf("Triceps")
private val DIP_MUSCLES = listOf("Triceps", "Pecs", "Front delts")

private fun armEntry(
    name: String,
    aliases: List<String>,
    equipment: List<Equipment>,
    muscles: List<String>,
    guide: FormGuide,
    sources: List<SourceId>,
    kind: Kind = Kind.ACCESSORY,
) = LibraryExercise(
    name, aliases, BodyArea.ARMS, equipment, muscles, kind, Category.ACCESSORY,
    parentLift = null, guide = guide, sources = sources,
)


/** BodyArea.ARMS entries. */
internal val arms: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "JM press",
        aliases = listOf("Blakely press"),
        area = BodyArea.ARMS,
        equipment = listOf(Equipment.BARBELL),
        muscles = listOf("Triceps"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A mix of a close-grip bench press and a triceps extension. It builds triceps strength for the bench lockout.",
            setup = listOf(
                RACK_SAFETY,
                "Lie on the bench with your feet flat on the floor.",
                "Grip the bar with your hands about 40 cm apart.",
                BENCH_BRACE,
            ),
            steps = listOf(
                "Lift the bar out and hold it over your shoulders with straight arms.",
                "Lower the bar toward your upper chest, keeping your elbows high.",
                "Stop when your forearms press against your upper arms.",
                "Press back up until your elbows lock.",
                RACK_IT,
            ),
            cues = listOf("Elbows stay high.", "Bar between your chin and upper chest."),
            mistakes = listOf(
                "Letting your elbows drop, so it turns into a close-grip bench.",
                "Lowering the bar to your lower chest instead of between your chin and upper chest.",
            ),
        ),
        sources = listOf(Sources.EXRX_JM_PRESS, Sources.NSCA_STANDARDS_SUMMARY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    LibraryExercise(
        name = "Curl",
        aliases = listOf("Biceps curl", "Barbell curl"),
        area = BodyArea.ARMS,
        equipment = listOf(Equipment.BARBELL, Equipment.DUMBBELL),
        muscles = listOf("Biceps"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A curl for the front of your upper arm. It mainly works your biceps.",
            setup = listOf(
                "Stand tall holding a barbell, or a dumbbell in each hand, with an underhand grip about shoulder-width apart.",
                "Line up your wrists, elbows and shoulders, and keep your elbows at your sides.",
                "Brace your trunk so your body stays still.",
            ),
            steps = listOf(
                "Bend your elbows to curl the weight up until your forearms are upright.",
                "Lower it slowly until your arms are straight.",
            ),
            cues = listOf("Elbows at your sides.", "Chest still.", "Lower slowly."),
            mistakes = listOf(
                "Swinging your body to lift the weight.",
                "Letting your elbows drift far forward.",
                "Dropping the weight on the way down.",
            ),
        ),
        sources = listOf(Sources.EXRX_BB_CURL, Sources.ACE_BICEP_CURL),
    ),

    // ---- Curls ----
    armEntry(
        "EZ-bar curl", listOf("EZ curl", "EZ barbell curl", "Cambered bar curl"),
        listOf(Equipment.BARBELL), BICEPS,
        FormGuide(
            purpose = "A curl with the bent EZ bar. It works your biceps.",
            setup = listOf(
                "Stand tall holding an EZ bar with an underhand grip, about shoulder-width apart.",
                "Let your arms hang straight, with your elbows at your sides.",
                STAND_BRACE,
            ),
            steps = listOf(
                "Keep your elbows at your sides and curl the bar up until your forearms are upright.",
                LOWER_TO_STRAIGHT,
            ),
            cues = listOf(ELBOWS_AT_SIDES, "Lower slowly."),
            mistakes = listOf(SWINGING, DROPPING, HALF_REPS),
        ),
        listOf(Sources.EXRX_BB_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "DB curl",
        listOf("Dumbbell curl", "Dumbbell biceps curl", "Alternating DB curl", "Alternating dumbbell curl"),
        listOf(Equipment.DUMBBELL), BICEPS,
        FormGuide(
            purpose = "A standing curl with a dumbbell in each hand, turning your palm up as you lift. It works your biceps, one arm at a time.",
            setup = listOf(
                "Stand with a dumbbell in each hand at your sides, palms facing in and arms straight.",
                STAND_BRACE,
            ),
            steps = listOf(
                "Keep your elbow at your side. Curl one dumbbell up, turning your palm up as you go, until your forearm is upright and your palm faces your shoulder.",
                "Lower it to the start.",
                "Repeat with the other arm, and keep alternating.",
            ),
            cues = listOf(ELBOWS_AT_SIDES, "Turn your palm up as you curl."),
            mistakes = listOf(SWINGING, DROPPING, HALF_REPS),
        ),
        listOf(Sources.EXRX_DB_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "Seated DB curl", listOf("Seated dumbbell curl", "Seated alternating curl", "Seated dumbbell biceps curl"),
        listOf(Equipment.DUMBBELL), BICEPS,
        FormGuide(
            purpose = "A dumbbell curl done sitting on a bench, so your legs can't help. It works your biceps.",
            setup = listOf(
                "Sit on the end of a bench with your legs together.",
                "Hold a dumbbell in each hand, palms facing in and arms hanging straight down.",
                SEATED_BRACE,
            ),
            steps = listOf(
                "Keep your elbow back at your side. Curl one dumbbell up, turning your palm up, until your forearm is upright and your palm faces your shoulder.",
                "Lower it to the start.",
                "Repeat with the other arm, and keep alternating.",
            ),
            cues = listOf(ELBOWS_AT_SIDES, "Turn your palm up as you curl."),
            mistakes = listOf(SWINGING, DROPPING),
        ),
        listOf(Sources.EXRX_DB_SEATED_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "Hammer curl",
        listOf("DB hammer curl", "Dumbbell hammer curl", "Alternating hammer curl"),
        listOf(Equipment.DUMBBELL), HAMMER,
        FormGuide(
            purpose = "A dumbbell curl with your palms facing in, like holding a hammer. It works your forearms and the muscles at the front of your upper arm.",
            setup = listOf(
                "Stand with one foot a little in front of the other, for balance.",
                "Hold a dumbbell in each hand beside your thighs, palms facing your body and arms straight.",
                "Brace your abs and pull your shoulder blades down and back. Keep your head in line with your spine.",
            ),
            steps = listOf(
                "Breathe out and bend your elbows to curl the dumbbells up to the front of your shoulders. Keep your palms facing in.",
                "Breathe in and lower them until your elbows are straight.",
                "You can curl both arms together, or one at a time.",
            ),
            cues = listOf("Wrists straight.", "Elbows stay back.", "Torso upright."),
            mistakes = listOf(ELBOWS_FORWARD, "Shrugging your shoulders.", "Bending your wrists."),
        ),
        listOf(Sources.ACE_HAMMER_CURL, Sources.EXRX_DB_HAMMER_CURL),
    ),
    armEntry(
        "Cable hammer curl", listOf("Rope hammer curl", "Cable rope curl", "Rope curl", "Cable rope hammer curl"),
        listOf(Equipment.CABLE), HAMMER,
        FormGuide(
            purpose = "A hammer curl with a rope on a low cable. It works your forearms and the muscles at the front of your upper arm.",
            setup = listOf(
                "Attach a rope to the low pulley.",
                "Hold the rope with your palms facing in. Stand upright with your arms straight down.",
                STAND_BRACE,
            ),
            steps = listOf(
                "Keep your elbows at your sides. Curl the rope forward and up with both arms until your forearms are upright.",
                LOWER_TO_STRAIGHT,
            ),
            cues = listOf(ELBOWS_AT_SIDES, "Palms face in."),
            mistakes = listOf(SWINGING, HALF_REPS),
        ),
        listOf(Sources.EXRX_CB_HAMMER_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "Incline DB curl", listOf("Incline curl", "Incline dumbbell curl", "Incline biceps curl", "Alternating incline curl"),
        listOf(Equipment.DUMBBELL), BICEPS,
        FormGuide(
            purpose = "A dumbbell curl done lying back on an incline bench, so your arms start behind your body. It works your biceps.",
            setup = listOf(
                "Set a bench to an incline and sit back against it.",
                "Hold a dumbbell in each hand with your arms hanging straight down and your palms facing in.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Keep your elbow back at your side. Curl one dumbbell up, turning your palm up, until your forearm is upright and your palm faces your shoulder.",
                "Lower it to the start.",
                "Repeat with the other arm, and keep alternating.",
            ),
            cues = listOf("Back on the bench.", ELBOWS_AT_SIDES),
            mistakes = listOf(DROPPING, HALF_REPS),
        ),
        listOf(Sources.EXRX_DB_INCLINE_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "Preacher curl",
        listOf("Scott curl", "Barbell preacher curl", "EZ preacher curl", "EZ bar preacher curl"),
        listOf(Equipment.BARBELL), BRACHIALIS,
        FormGuide(
            purpose = "A curl with the backs of your arms on a sloped pad. It works the muscles at the front of your upper arm without help from your body.",
            setup = listOf(
                PAD_SEAT,
                "Sit with the backs of your upper arms on the pad.",
                "Hold the bar with an underhand grip, about shoulder-width apart.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Curl the bar up until your forearms are upright.",
                LOWER_TO_STRAIGHT,
                ARM_ON_PAD,
            ),
            cues = listOf("Arms stay on the pad.", "Lower to straight arms."),
            mistakes = listOf("Lifting your upper arms off the pad.", DROPPING, HALF_REPS),
        ),
        listOf(Sources.EXRX_BB_PREACHER_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "DB preacher curl",
        listOf("Dumbbell preacher curl", "One-arm preacher curl", "Single-arm preacher curl", "One-arm DB preacher curl"),
        listOf(Equipment.DUMBBELL), BRACHIALIS,
        FormGuide(
            purpose = "A one-arm preacher curl with a dumbbell. It works the muscles at the front of your upper arm, one arm at a time.",
            setup = listOf(
                PAD_SEAT,
                "Sit with the back of your working arm on the pad, holding the dumbbell with your palm up.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Curl the dumbbell up until your forearm is upright.",
                "Lower under control until your arm is straight.",
                ARM_ON_PAD,
                SWITCH_ARMS,
            ),
            cues = listOf("Arm stays on the pad.", "Lower to a straight arm."),
            mistakes = listOf("Lifting your upper arm off the pad.", DROPPING),
        ),
        listOf(Sources.EXRX_DB_PREACHER_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "Machine curl",
        listOf("Machine preacher curl", "Biceps curl machine", "Lever preacher curl", "Seated machine curl", "Machine biceps curl"),
        listOf(Equipment.MACHINE), BRACHIALIS,
        FormGuide(
            purpose = "A seated curl on a machine, with your arms on a sloped pad. It works the muscles at the front of your upper arm.",
            setup = listOf(
                "Set the seat so the middle of your elbows lines up with the machine's pivot.",
                "Sit with your arms over the pad. Grip the handles firmly, with your wrists straight.",
                "Brace your abs and pull your shoulder blades down and back.",
            ),
            steps = listOf(
                "Breathe out and curl the handles toward your chest.",
                "Pause at the top.",
                "Breathe in and lower slowly until your arms are nearly straight.",
                "You can also work one arm at a time.",
            ),
            cues = listOf("Elbows in line with the pivot.", "Arms stay on the pad.", "Lower slowly."),
            mistakes = listOf("Swinging your body back to lift the weight. It strains your lower back.", "Lifting your upper arms off the pad."),
        ),
        listOf(Sources.ACE_SEATED_BICEPS_CURL, Sources.EXRX_LV_PREACHER_CURL),
    ),
    armEntry(
        "Cable curl", listOf("Cable biceps curl", "Standing cable curl", "Cable bar curl", "Straight-bar cable curl"),
        listOf(Equipment.CABLE), BICEPS,
        FormGuide(
            purpose = "A standing curl with a bar on a low cable. It works your biceps.",
            setup = listOf(
                "Attach a bar to the low pulley.",
                "Hold the bar with an underhand grip, about shoulder-width apart, and stand close to the pulley.",
                STAND_BRACE,
            ),
            steps = listOf(
                "Keep your elbows at your sides and curl the bar up until your forearms are upright.",
                LOWER_TO_STRAIGHT,
            ),
            cues = listOf(ELBOWS_AT_SIDES, "Lower slowly."),
            mistakes = listOf(SWINGING, HALF_REPS),
        ),
        listOf(Sources.EXRX_CB_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "One-arm cable curl", listOf("Single-arm cable curl", "Cable one-arm curl", "One-arm cable biceps curl"),
        listOf(Equipment.CABLE), BICEPS,
        FormGuide(
            purpose = "A curl with a single handle on a low cable. It works your biceps, one arm at a time.",
            setup = listOf(
                "Attach a single handle to the low pulley.",
                "Hold it with an underhand grip and your elbow at your side. You can face the pulley, or stand with your working side toward it.",
                STAND_BRACE,
            ),
            steps = listOf(
                "Keep your elbow at your side and curl the handle up until your forearm is upright.",
                "Lower under control until your arm is straight.",
                SWITCH_ARMS,
            ),
            cues = listOf("Elbow at your side.", "Lower slowly."),
            mistakes = listOf(SWINGING, HALF_REPS),
        ),
        listOf(Sources.EXRX_CB_ONE_ARM_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "Concentration curl",
        listOf("DB concentration curl", "Dumbbell concentration curl", "Seated concentration curl"),
        listOf(Equipment.DUMBBELL), BRACHIALIS,
        FormGuide(
            purpose = "A seated one-arm curl with your arm braced on your thigh. It works the muscles at the front of your upper arm.",
            setup = listOf(
                "Sit on a bench with a dumbbell between your feet.",
                "Hold the dumbbell and rest the back of your upper arm against your inner thigh.",
                "Lean into that leg to lift your elbow slightly.",
                SEATED_BRACE,
            ),
            steps = listOf(
                "Curl the dumbbell up toward the front of your shoulder.",
                "Lower under control until your arm is straight.",
                SWITCH_ARMS,
            ),
            cues = listOf("Upper arm against your inner thigh.", "Lower to a straight arm."),
            mistakes = listOf("Moving your upper arm away from your inner thigh.", HALF_REPS),
        ),
        listOf(Sources.EXRX_DB_CONCENTRATION_CURL, Sources.ACE_BRACE_OR_BREATHE),
    ),
    armEntry(
        "Drag curl", listOf("Barbell drag curl", "Bar drag curl"),
        listOf(Equipment.BARBELL), BICEPS,
        FormGuide(
            purpose = "A barbell curl where you drag the bar up your body. It works your biceps.",
            setup = listOf(
                "Stand tall holding a barbell with an underhand grip, arms straight.",
                STAND_BRACE,
            ),
            steps = listOf(
                "Pull the bar straight up, letting your elbows travel back. Keep the bar close to your hips and waist.",
                "Once your forearms pass level with the floor, let your elbows move forward.",
                "Keep the bar going up past your chest until your forearms are upright.",
                LOWER_TO_STRAIGHT,
            ),
            cues = listOf("Bar close to your body.", "Elbows back, then forward."),
            mistakes = listOf("Letting the bar drift away from your body.", SWINGING),
        ),
        listOf(Sources.EXRX_BB_DRAG_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "Spider curl", listOf("Prone incline curl", "Barbell spider curl", "EZ spider curl"),
        listOf(Equipment.BARBELL), BRACHIALIS,
        FormGuide(
            purpose = "A curl done lying face down on an incline bench, with your arms hanging straight down. It works the muscles at the front of your upper arm.",
            setup = listOf(
                "Lie face down on an incline bench, with your shoulders near the top.",
                "Rest your knees on the seat, or put your legs out to the sides.",
                "Hold a curl bar with an underhand grip, about shoulder-width apart.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Curl the bar up as far as your elbows bend.",
                LOWER_TO_STRAIGHT,
            ),
            cues = listOf("Chest on the bench.", "Lower to straight arms."),
            mistakes = listOf(HALF_REPS, "Curling only part of the way up."),
        ),
        listOf(Sources.EXRX_BB_PRONE_INCLINE_CURL, Sources.ACE_BRACE_OR_BREATHE, Sources.ACE_BICEP_CURL),
    ),
    armEntry(
        "Reverse curl",
        listOf("Reverse-grip curl", "Barbell reverse curl", "Overhand curl", "Reverse biceps curl", "EZ reverse curl"),
        listOf(Equipment.BARBELL), listOf("Forearms", "Brachialis", "Biceps", "Grip"),
        FormGuide(
            purpose = "A curl with your palms facing down. It works your forearms and grip, and the muscles at the front of your upper arm.",
            setup = listOf(
                "Stand holding a straight bar or an EZ bar with your palms down, about shoulder-width apart.",
                "Line up your hands with your shoulders and elbows.",
                STAND_BRACE,
            ),
            steps = listOf(
                "Keep your elbows close to your sides and curl the bar up until your forearms are upright. Lead with the backs of your hands.",
                "Lower slowly until your arms are straight.",
            ),
            cues = listOf("Palms down.", "Elbows close to your sides.", "Lower slowly."),
            mistakes = listOf(SWINGING, "Letting your elbows move away from your sides.", DROPPING),
        ),
        listOf(Sources.ACE_REVERSE_BICEP_CURL, Sources.EXRX_BB_REVERSE_CURL, Sources.ACE_GRIP_STRENGTH),
    ),

    // ---- Triceps: pushdowns and extensions ----
    armEntry(
        "Triceps pushdown",
        listOf(
            "Tricep pushdown", "V-bar pushdown", "Straight-bar pushdown", "Bar pushdown",
        ),
        listOf(Equipment.CABLE), TRICEPS,
        FormGuide(
            purpose = "A cable push down with a bar or V-bar on a high pulley. It works your triceps.",
            setup = listOf(
                "Attach a short bar or V-bar to the high pulley, above head height.",
                "Face the pulley and stand close to it. Hold the bar with a narrow grip, palms down.",
                "Tuck your elbows in at your sides.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Push the bar down until your arms are nearly straight. Keep your elbows at your sides.",
                PUSHDOWN_RETURN,
            ),
            cues = listOf(ELBOWS_AT_SIDES, STAY_CLOSE),
            mistakes = listOf(PUSHDOWN_ELBOWS, LOCKING, TOO_FAR),
        ),
        listOf(Sources.EXRX_CB_PUSHDOWN, Sources.EXRX_CB_PUSHDOWN_VBAR, Sources.ACE_TRICEPS_PUSHDOWNS),
    ),
    armEntry(
        "Rope pushdown",
        listOf("Rope tricep pushdown", "Rope triceps pushdown", "Rope pressdown", "Cable rope pushdown"),
        listOf(Equipment.CABLE), TRICEPS,
        FormGuide(
            purpose = "A cable push down with a rope, turning your palms down at the bottom. It works your triceps.",
            setup = listOf(
                "Attach a rope to the high pulley, above head height.",
                "Face the pulley and stand close to it. Hold the rope with your palms facing in.",
                "Tuck your elbows in at your sides.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Push the rope down until your arms are nearly straight. Turn your palms down at the bottom.",
                "Let the rope come back up slowly, turning your hands back to the start. Stop before your elbows start to move forward.",
            ),
            cues = listOf(ELBOWS_AT_SIDES, "Turn your palms down at the bottom."),
            mistakes = listOf(PUSHDOWN_ELBOWS, LOCKING, TOO_FAR),
        ),
        listOf(Sources.EXRX_CB_PUSHDOWN_ROPE, Sources.ACE_TRICEPS_PUSHDOWNS),
    ),
    armEntry(
        "One-arm pushdown",
        listOf("Single-arm pushdown", "Underhand pushdown", "One-arm reverse-grip pushdown", "Single-arm cable pushdown"),
        listOf(Equipment.CABLE), TRICEPS,
        FormGuide(
            purpose = "A cable push down with one hand, palm up. It works your triceps, one arm at a time.",
            setup = listOf(
                "Attach a single handle to the high pulley.",
                "Hold the handle with your palm facing up, and step close to the cable.",
                "Keep your elbow at your side.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Push the handle down until your arm is nearly straight.",
                "Let it come back up slowly. Stop before your elbow starts to move forward.",
                SWITCH_ARMS,
            ),
            cues = listOf("Elbow at your side.", STAY_CLOSE),
            mistakes = listOf("Letting your elbow drift forward.", TOO_FAR),
        ),
        listOf(Sources.EXRX_CB_ONE_ARM_PUSHDOWN, Sources.ACE_TRICEPS_PUSHDOWNS),
    ),
    armEntry(
        "Cable overhead triceps extension",
        listOf("Overhead rope extension", "Cable overhead extension", "Rope overhead extension", "Overhead cable tricep extension"),
        listOf(Equipment.CABLE), TRICEPS,
        FormGuide(
            purpose = "A triceps extension with a rope, pressing from behind your head to overhead. It works your triceps with your arms up.",
            setup = listOf(
                "Attach a rope to the low pulley.",
                "Hold the rope just under its knobbed ends. Turn away from the machine and stand with one foot in front of the other.",
                "Bring the rope up behind your head, with your elbows pointing up.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Straighten your arms to raise the rope overhead.",
                "Lower under control until your forearms touch your upper arms.",
                "Keep your elbows pointing up. You can also do this seated on a bench with back support.",
            ),
            cues = listOf("Elbows point up.", "Forearms to upper arms."),
            mistakes = listOf("Letting your elbows drop forward.", "Stopping before your forearms touch your upper arms."),
        ),
        listOf(Sources.EXRX_CB_TRI_EXT_ROPE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    armEntry(
        "DB overhead triceps extension",
        listOf(
            "Dumbbell overhead triceps extension", "DB overhead tricep extension", "Standing dumbbell triceps extension",
        ),
        listOf(Equipment.DUMBBELL), TRICEPS,
        FormGuide(
            purpose = "A triceps extension holding one dumbbell in both hands, lowering it behind your head. It works your triceps with your arms up.",
            setup = listOf(
                "Stand with one foot a little in front of the other. Hold one dumbbell with both hands.",
                "Brace your abs and pull your shoulder blades down and back. Keep your head in line with your spine.",
                "Press the dumbbell overhead until your arms are upright.",
            ),
            steps = listOf(
                "Breathe in and bend your elbows to lower the dumbbell slowly behind your head. Keep your upper arms still.",
                "Stop when your forearms are about level with the floor. Don't let the dumbbell touch your head.",
                "Breathe out and straighten your elbows to press it back up.",
                "You can also do this seated on a bench with back support.",
            ),
            cues = listOf("Upper arms still.", "Lower slowly."),
            mistakes = listOf("Hitting the back of your head or neck with the dumbbell.", "Letting your upper arms move during the rep."),
        ),
        listOf(Sources.ACE_TRICEPS_EXTENSION, Sources.EXRX_DB_TRI_EXT),
    ),
    armEntry(
        "Skull crusher",
        listOf(
            "Skull crushers", "Barbell lying triceps extension", "EZ bar lying triceps extension", "EZ bar skull crusher",
            "French press",
        ),
        listOf(Equipment.BARBELL), TRICEPS,
        FormGuide(
            purpose = "A lying triceps extension, lowering a bar toward your forehead. It works your triceps.",
            setup = listOf(
                OVER_FACE_SPOT,
                "Sit on the end of a flat bench with the bar across your lap. Hold it with your palms down and your thumbs wrapped around it.",
                "Lie back on the bench with your feet flat on the floor.",
                "Press the bar up over your chest until your arms are straight but not locked, so the bar is above your face. Keep your wrists straight.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Breathe in and bend your elbows to lower the bar slowly toward your forehead. Keep your upper arms upright.",
                "Stop when the bar is just above your forehead, or sooner if your upper arms start to drift back.",
                "Breathe out and press the bar back up, without locking your elbows.",
            ),
            cues = listOf("Upper arms upright.", "Slow near your forehead.", "Elbows about shoulder-width apart."),
            mistakes = listOf("Letting your upper arms drift back.", "Letting the bar drop fast toward your face."),
        ),
        listOf(
            Sources.ACE_LYING_BB_TRI_EXT, Sources.EXRX_BB_SKULL_CRUSHER,
            Sources.NSCA_STANDARDS_SUMMARY, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    armEntry(
        "DB lying triceps extension",
        listOf("Dumbbell lying triceps extension", "DB skull crusher", "Dumbbell skull crusher"),
        listOf(Equipment.DUMBBELL), TRICEPS,
        FormGuide(
            purpose = "A lying triceps extension with a dumbbell in each hand, lowering them beside your head. It works your triceps.",
            setup = listOf(
                OVER_FACE_SPOT,
                "Lie on a flat bench with your feet flat on the floor.",
                "Start with your arms straight and the dumbbells held up above your shoulders, not over your face.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Bend your elbows to lower the dumbbells slowly until they are beside your head.",
                "Straighten your arms to raise them back up.",
            ),
            cues = listOf("Dumbbells beside your head, not over your face.", "Lower slowly."),
            mistakes = listOf("Lowering the dumbbells toward your face instead of beside your head.", DROPPING),
        ),
        listOf(
            Sources.EXRX_DB_LYING_TRI_EXT, Sources.EXRX_DB_DECLINE_TRI_EXT, Sources.NSCA_STANDARDS_SUMMARY,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    armEntry(
        "Machine triceps extension",
        listOf("Triceps extension machine", "Lever triceps extension", "Seated triceps extension machine", "Tricep extension machine"),
        listOf(Equipment.MACHINE), TRICEPS,
        FormGuide(
            purpose = "A seated triceps extension on a machine, with your upper arms on a pad. It works your triceps.",
            setup = listOf(
                "Set the seat so the backs of your upper arms rest on the pad.",
                "Sit and grip the handles. Line up your elbows with the machine's pivot.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Push the handles down until your arms are straight.",
                "Return under control until your forearms touch your upper arms.",
            ),
            cues = listOf("Elbows in line with the pivot.", "Upper arms stay on the pad."),
            mistakes = listOf("Lifting your upper arms off the pad.", "Sitting so your elbows are out of line with the pivot."),
        ),
        listOf(Sources.EXRX_LV_TRI_EXT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    armEntry(
        "Triceps kickback", listOf("Tricep kickback", "Kickback", "DB kickback", "Dumbbell kickback", "Dumbbell triceps kickback"),
        listOf(Equipment.DUMBBELL), TRICEPS,
        FormGuide(
            purpose = "A bent-over move where you straighten your arm behind you. It works your triceps, one arm at a time.",
            setup = listOf(
                "Stand with one foot in front of the other, holding a dumbbell in one hand.",
                "Brace your abs and lean forward. Rest your free hand on your thigh or knee.",
                "Pull your shoulder blade down and back.",
                "Hold your upper arm close to your side. Bend your elbow so your forearm hangs straight down.",
            ),
            steps = listOf(
                "Straighten your elbow to move the dumbbell back. Keep your upper arm still.",
                "Return it slowly to the start, without moving your body.",
                SWITCH_ARMS,
            ),
            cues = listOf("Upper arm still.", "Back flat."),
            mistakes = listOf("Letting your lower back sag.", "Twisting your body.", "Swinging your upper arm."),
        ),
        listOf(Sources.ACE_TRICEPS_KICKBACK, Sources.EXRX_DB_KICKBACK),
    ),

    // ---- Triceps: dips and push-ups ----
    armEntry(
        "Triceps dip", listOf("Dips", "Dip", "Tricep dip", "Weighted dip", "Weighted triceps dip"),
        listOf(Equipment.BODYWEIGHT), DIP_MUSCLES,
        FormGuide(
            purpose = "A bodyweight dip on parallel bars with your body upright. It works your triceps, with help from your chest and front shoulders.",
            setup = listOf(
                "Grip shoulder-width bars and push up until your arms are straight, with your shoulders over your hands.",
                "Keep your hips straight, so your body stays upright.",
                BODY_BRACE,
                "To add weight, hang it from a dip belt or hold a dumbbell between your lower legs.",
            ),
            steps = listOf(
                "Lower yourself by bending your arms. Stay upright.",
                // No allowed source gives a positional depth for the bar dip (ExRx gives only "a slight stretch").
                // Upper arms level is ACE's bench-dip limit, carried over as the conservative limit: PMC9603242
                // found the bar dip needs less shoulder extension than the bench dip.
                "Stop when your upper arms are level with the floor.",
                "Push back up until your arms are straight.",
            ),
            cues = listOf("Body upright.", "Upper arms level, no deeper."),
            mistakes = listOf(
                "Going deeper than upper arms level with the floor. Deep dips take your shoulders past their normal range.",
                "Leaning forward. Stay upright to keep the work on your triceps.",
            ),
        ),
        listOf(
            Sources.EXRX_WT_TRI_DIP, Sources.EXRX_ST_TRI_DIP, Sources.PMC_BENCH_BAR_RING_DIPS,
            Sources.ACE_DANGEROUS_DIPS, Sources.ACE_BRACE_OR_BREATHE,
        ),
        kind = Kind.COMPOUND,
    ),
    armEntry(
        "Close-grip push-up", listOf("Close-grip pushup"),
        listOf(Equipment.BODYWEIGHT), DIP_MUSCLES,
        FormGuide(
            purpose = "A push-up with your hands close together. It works your triceps, with help from your chest and front shoulders.",
            setup = listOf(
                "Place your hands on the floor under your shoulders, or a little narrower.",
                "Push up onto your toes with straight arms.",
                BODY_BRACE,
            ),
            steps = listOf(
                "Bend your arms to lower your body toward the floor. Keep your body straight.",
                "Push back up until your arms are straight.",
            ),
            cues = listOf("Body in one straight line.", "Hands under your shoulders."),
            mistakes = listOf("Letting your hips sag.", "Lifting your hips."),
        ),
        listOf(Sources.EXRX_CLOSE_GRIP_PUSH_UP, Sources.ACE_PUSH_UP),
        kind = Kind.COMPOUND,
    ),
    armEntry(
        "Triangle push-up", listOf("Diamond push-up", "Diamond pushup", "Triangle pushup"),
        listOf(Equipment.BODYWEIGHT), DIP_MUSCLES,
        FormGuide(
            purpose = "A harder close-grip push-up, with your hands together under your chest. It works your triceps hard.",
            setup = listOf(
                "Place your hands together on the floor under your chest, so your thumbs and index fingers make a triangle.",
                "Push up onto your toes with straight arms.",
                "Brace your trunk so your body stays rigid. Keep your head in line with your spine.",
            ),
            steps = listOf(
                "Bend your elbows to lower your chest toward the floor. Keep your body rigid.",
                "Push back up until your elbows are straight.",
            ),
            cues = listOf("Body rigid.", "Head in line with your spine."),
            mistakes = listOf("Letting your hips sag.", "Lifting your hips."),
        ),
        listOf(Sources.ACE_TRICEPS_STUDY, Sources.ACE_PUSH_UP),
        kind = Kind.COMPOUND,
    ),

    // ---- Forearms and grip ----
    armEntry(
        "Wrist curl",
        listOf("Barbell wrist curl", "DB wrist curl", "Dumbbell wrist curl", "Seated wrist curl"),
        listOf(Equipment.BARBELL, Equipment.DUMBBELL), listOf("Forearms", "Grip"),
        FormGuide(
            purpose = "A small curl at the wrist, palms up. It works the muscles on the inside of your forearm, which helps your grip.",
            setup = listOf(
                "Hold a barbell with an underhand grip, hands about shoulder-width apart or closer. Or hold a dumbbell in one hand.",
                WRIST_SEAT,
                SEATED_BRACE,
            ),
            steps = listOf(
                "Let the bar roll down out of your palms to your fingers.",
                "Curl it back up by bending your wrists, as high as you can.",
                "Lower under control and repeat.",
            ),
            cues = listOf("Only your wrists move.", "Elbows about level with your wrists."),
            mistakes = listOf("Lifting your forearms off your thighs.", ELBOWS_LOW),
        ),
        listOf(Sources.EXRX_BB_WRIST_CURL, Sources.EXRX_DB_WRIST_CURL, Sources.ACE_GRIP_STRENGTH, Sources.ACE_BRACE_OR_BREATHE),
    ),
    armEntry(
        "Reverse wrist curl",
        listOf("Barbell reverse wrist curl", "DB reverse wrist curl", "Dumbbell reverse wrist curl", "Wrist extension"),
        listOf(Equipment.BARBELL, Equipment.DUMBBELL), listOf("Forearms"),
        FormGuide(
            purpose = "A small lift at the wrist, palms down. It works the muscles on the back of your forearm, which grip training needs too.",
            setup = listOf(
                "Hold a barbell with an overhand grip, hands about shoulder-width apart or closer. Or hold a dumbbell in one hand.",
                WRIST_SEAT,
                SEATED_BRACE,
            ),
            steps = listOf(
                "Raise the weight by bending your wrists back, so your knuckles point up as high as you can.",
                "Lower it until your knuckles point down as far as you can.",
            ),
            cues = listOf("Only your wrists move.", "Elbows about level with your wrists."),
            mistakes = listOf("Lifting your forearms off your thighs.", ELBOWS_LOW),
        ),
        listOf(
            Sources.EXRX_BB_REVERSE_WRIST_CURL, Sources.EXRX_DB_REVERSE_WRIST_CURL, Sources.ACE_GRIP_STRENGTH,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
)
