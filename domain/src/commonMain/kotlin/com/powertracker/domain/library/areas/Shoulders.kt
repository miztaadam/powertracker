package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

// Shared lines, so the safety and bracing wording is the same in every shoulder guide.
private const val BAR_SAFETY = "Use a spotter. If you press in a power rack, set its safety pins too."
private const val OVERHEAD_SPOT = "Use a spotter. They stand behind you, with their hands near your wrists."
private const val SPOTTER_BEHIND = "Use a spotter. They stand behind you, ready to help."
private const val PRESS_BRACE = "Breathe in and brace your abs before each rep."
private const val MACHINE_BRACE = "Brace your abs, but don't press your lower back into the backrest."
private const val LIGHT_BRACE = "Brace your abs lightly, and keep breathing."
private const val BENT_BRACE = "Brace your abs and keep your back flat."
private const val SMITH_BRACE = "Breathe in and brace before you unhook the bar."
private const val SPOT_HEAVY = "Ask for a spot when you go heavy."
private const val PRESS_OVERHEAD = "Press overhead until your arms are straight."
private const val LOWER_SLOWLY = "Lower under control."
private const val SWITCH_ARMS = "Do all your reps, then switch arms."
private const val ELBOWS_TO_SHOULDER = "Raise your upper arms out to the sides until your elbows are at shoulder height."
private const val SAME_BEND = "Keep the bend in your elbows the same."
private const val BEHIND_NECK = "Lowering the bar behind your neck."
private const val NO_SPOTTER = "Pressing without a spotter."
private const val ARCHING = "Arching your lower back to get the weight up."
private const val THROWING = "Throwing the weight up with your hips or back. Use a lighter weight."
private const val ELBOWS_BELOW_WRISTS = "Letting your elbows drop below your wrists, which moves the work to the front of your shoulders."
private const val ELBOWS_BEHIND = "Letting your elbows travel behind your shoulders."
private const val NOT_BENT_OVER = "Not bending over far enough, which moves the work away from your rear delts."
private const val ELBOWS_TO_LATS = "Letting your elbows drop toward your sides, which moves the work to your lats."
private const val ELBOW_AT_SIDE = "Bend your elbow and keep it against your side, with your forearm across your belly."
private const val ELBOW_DRIFTS = "Letting your elbow drift away from your side."
private const val BEND_CHANGES = "Changing the bend in your elbow during the rep."

private val PRESS_MUSCLES = listOf("Front delts", "Side delts", "Triceps")
private val SIDE = listOf("Side delts")
private val FRONT = listOf("Front delts")
private val REAR = listOf("Rear delts", "Upper back")
private val CUFF = listOf("Rotator cuff")

private fun shoulderEntry(
    name: String,
    aliases: List<String>,
    equipment: List<Equipment>,
    muscles: List<String>,
    kind: Kind,
    guide: FormGuide,
    sources: List<SourceId>,
) = LibraryExercise(
    name, aliases, BodyArea.SHOULDERS, equipment, muscles, kind, Category.ACCESSORY,
    parentLift = null, guide = guide, sources = sources,
)

/** BodyArea.SHOULDERS entries. */
internal val shoulders: List<LibraryExercise> = listOf(
    // ---- Presses ----
    shoulderEntry(
        "Overhead press",
        listOf(
            "OHP", "Military press", "Shoulder press", "Standing press", "Barbell overhead press", "Standing barbell press",
            "Barbell military press",
        ),
        listOf(Equipment.BARBELL), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A standing barbell press from your shoulders to overhead. It works your shoulders and triceps.",
            setup = listOf(
                BAR_SAFETY,
                "Set the bar in the rack at about shoulder height.",
                "Grip the bar a little wider than your shoulders.",
                "Take the bar across the front of your shoulders and step back.",
                "Stand with your feet shoulder-width apart, or with one foot a little in front of the other.",
                PRESS_BRACE,
            ),
            steps = listOf(
                "Press the bar overhead until your arms are straight.",
                "Keep your back straight and tall.",
                "Lower the bar under control to the front of your neck.",
                RACK_IT,
            ),
            cues = listOf("Back straight and tall.", "Press to straight arms."),
            mistakes = listOf("Leaning back instead of keeping your back straight and tall.", BEHIND_NECK, NO_SPOTTER),
        ),
        listOf(
            Sources.EXRX_BB_MILITARY_PRESS, Sources.ACE_STANDING_SHOULDER_PRESS, Sources.NSCA_STANDARDS_SUMMARY,
            Sources.PMC_UPPER_EXTREMITY_LIFTING_INJURIES, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    shoulderEntry(
        "Seated barbell press",
        listOf("Seated military press", "Seated barbell shoulder press", "Seated OHP"),
        listOf(Equipment.BARBELL), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A barbell press done seated, with your back supported. It works your shoulders and triceps.",
            setup = listOf(
                "Set the rack's safety pins, and use a spotter.",
                "Sit on a shoulder-press bench in the rack, with your back straight against the pad.",
                "Grip the bar a little wider than your shoulders.",
                "Press your feet into the floor and brace your abs.",
            ),
            steps = listOf(
                "Lift the bar out of the rack.",
                "Lower it to the front of your shoulders, to about chin height.",
                "Press it overhead until your arms are straight, keeping your elbows forward.",
                RACK_IT,
            ),
            cues = listOf("Back on the pad.", "Elbows forward.", "Feet pressed into the floor."),
            mistakes = listOf("Arching your back off the pad.", BEHIND_NECK, NO_SPOTTER),
        ),
        listOf(
            Sources.ACE_SEATED_BB_SHOULDER_PRESS, Sources.EXRX_BB_SEATED_MILITARY_PRESS, Sources.NSCA_STANDARDS_SUMMARY,
            Sources.PMC_UPPER_EXTREMITY_LIFTING_INJURIES,
        ),
    ),
    shoulderEntry(
        "DB shoulder press",
        listOf(
            "Dumbbell shoulder press", "Seated DB press", "Seated dumbbell press", "Seated dumbbell shoulder press",
            "DB overhead press", "Dumbbell overhead press",
        ),
        listOf(Equipment.DUMBBELL), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A seated press with a dumbbell in each hand. It works your shoulders and triceps, each arm on its own.",
            setup = listOf(
                "Sit on a bench with back support. Keep your head, back and buttocks against it, and your feet flat on the floor.",
                "Start with the dumbbells at your shoulders, palms facing forward and wrists straight.",
                "Hold them beside your shoulders, with your elbows under your wrists.",
                "Brace your abs. Pull your shoulder blades down and back, and hold them there for the whole set.",
                OVERHEAD_SPOT,
            ),
            steps = listOf(
                "Breathe out and press the dumbbells overhead until your elbows are straight.",
                "Breathe in and lower them under control to shoulder height.",
                "Keep your torso still the whole time.",
            ),
            cues = listOf("Elbows under wrists.", "Back against the bench.", "Pull the weights down; don't let them drop."),
            mistakes = listOf(ARCHING, "Letting the dumbbells drop on the way down."),
        ),
        listOf(Sources.ACE_SEATED_OVERHEAD_PRESS, Sources.EXRX_DB_SHOULDER_PRESS, Sources.NSCA_STANDARDS_SUMMARY),
    ),
    shoulderEntry(
        "One-arm DB press",
        listOf("Single-arm DB press", "One-arm dumbbell shoulder press", "Single-arm dumbbell press", "One-arm overhead press"),
        listOf(Equipment.DUMBBELL), listOf("Front delts", "Triceps"), Kind.COMPOUND,
        FormGuide(
            purpose = "A shoulder press with one dumbbell, one arm at a time. It works your front delts and triceps.",
            setup = listOf(
                "Stand tall. Start with a dumbbell at one shoulder, with your elbow under your wrist.",
                PRESS_BRACE,
                SPOTTER_BEHIND,
            ),
            steps = listOf(
                "Press the dumbbell overhead until your arm is straight.",
                "Lower it back to your shoulder.",
                "Keep your torso upright.",
                SWITCH_ARMS,
            ),
            cues = listOf("Elbow under wrist.", "Torso upright."),
            mistakes = listOf(ARCHING, "Letting your elbow drift out from under your wrist."),
        ),
        listOf(
            Sources.EXRX_DB_ONE_ARM_SHOULDER_PRESS, Sources.ACE_SEATED_OVERHEAD_PRESS, Sources.NSCA_STANDARDS_SUMMARY,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    shoulderEntry(
        "Arnold press",
        listOf("Arnold dumbbell press", "DB Arnold press", "Dumbbell Arnold press"),
        listOf(Equipment.DUMBBELL), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A dumbbell press that starts with your palms facing you and opens out as you press. It works your front and side delts.",
            setup = listOf(
                "Stand. Start with the dumbbells in front of your shoulders, palms facing you, with your elbows under your wrists.",
                PRESS_BRACE,
                SPOTTER_BEHIND,
            ),
            steps = listOf(
                "Move your elbows out to the sides.",
                "Then press the dumbbells overhead until your arms are straight.",
                "Lower them the same way, back to the start.",
            ),
            cues = listOf("Elbows out, then press.", "Reverse the path on the way down."),
            mistakes = listOf(
                "Just twisting your forearms instead of moving your elbows out to the sides.",
                ARCHING,
            ),
        ),
        listOf(
            Sources.EXRX_DB_ARNOLD_PRESS, Sources.ACE_SEATED_OVERHEAD_PRESS, Sources.NSCA_STANDARDS_SUMMARY,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    shoulderEntry(
        "Machine shoulder press",
        listOf("Shoulder press machine", "Lever shoulder press", "Seated machine shoulder press", "Machine overhead press"),
        listOf(Equipment.MACHINE), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A seated shoulder press on a machine. It works your shoulders and triceps.",
            setup = listOf(
                "Set the seat so the handles are level with your shoulders or a little above.",
                "Sit with your back against the pad.",
                "Grip the handles with your palms facing forward.",
                MACHINE_BRACE,
            ),
            steps = listOf(
                "Press the handles up until your elbows are nearly straight.",
                "Lower them under control, then repeat.",
            ),
            cues = listOf("Back on the pad.", "Control the way down."),
            mistakes = listOf("Locking your elbows hard at the top.", "Letting the weight drop between reps."),
        ),
        listOf(
            Sources.ACE_SEATED_MACHINE_SHOULDER_PRESS, Sources.EXRX_LV_SHOULDER_PRESS,
            Sources.ACE_SEATED_MACHINE_CLOSE_GRIP_SHOULDER_PRESS,
        ),
    ),
    shoulderEntry(
        "Smith shoulder press",
        listOf("Smith machine shoulder press", "Smith overhead press", "Smith military press"),
        listOf(Equipment.SMITH), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A seated shoulder press on a Smith machine, where the bar runs on fixed rails. It works your shoulders and triceps.",
            setup = listOf(
                "Set the safety stops so the bar can't come down below your shoulders.",
                "Sit on the bench with the bar in front of your shoulders.",
                "On an angled Smith machine, face away from the angle, so the bar starts in front of you and moves overhead.",
                "Grip the bar wider than your shoulders, but not so wide that the range gets short.",
                SPOT_HEAVY,
                SMITH_BRACE,
            ),
            steps = listOf(
                "Unhook the bar by lifting it and turning it back.",
                PRESS_OVERHEAD,
                "Lower it to the front of your shoulders. Pull your head back slightly so the bar doesn't touch it.",
                "After the last rep, turn the bar to set it back on the hooks.",
            ),
            cues = listOf("Head back slightly.", "Not too wide a grip."),
            mistakes = listOf("Gripping so wide that the range of motion gets short.", "Training to failure without a spotter."),
        ),
        listOf(Sources.EXRX_SM_SHOULDER_PRESS, Sources.EXRX_GYM_ETIQUETTE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Landmine press",
        listOf("One-arm landmine press", "Single-arm landmine press", "Landmine shoulder press"),
        listOf(Equipment.BARBELL), listOf("Front delts", "Triceps"), Kind.COMPOUND,
        FormGuide(
            purpose = "A one-arm press of the end of a barbell, up and forward. It works your front delts and triceps.",
            setup = listOf(
                "A landmine holds one end of a barbell on the floor, in a special attachment or a corner of the room. Load the other end.",
                "Stand beside the loaded end. Keep your back straight as you lift it off the floor.",
                "Hold the end of the bar near your shoulder.",
                "Set your feet far apart, with the leg on the pressing side back and the other leg forward and bent.",
                PRESS_BRACE,
            ),
            steps = listOf(
                "Press the end of the bar up and forward until your arm is straight.",
                "Lower it back to your shoulder.",
                "Do all your reps, then switch sides.",
            ),
            cues = listOf("Up and forward.", "Back straight."),
            mistakes = listOf(
                "Rounding your back as you lift the bar off the floor.",
                "Pushing with your legs. In this version, only your arm and shoulder move the bar.",
            ),
        ),
        listOf(Sources.EXRX_LV_ONE_ARM_SHOULDER_PRESS, Sources.ACE_LANDMINE_WORKOUT_BUILDER, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Kettlebell press",
        listOf("KB press", "Single-arm kettlebell press", "One-arm kettlebell press", "Kettlebell overhead press"),
        listOf(Equipment.KETTLEBELL), listOf("Front delts", "Triceps"), Kind.COMPOUND,
        FormGuide(
            purpose = "A one-arm press of a kettlebell from your shoulder to overhead. It works your front delts and triceps.",
            setup = listOf(
                "Stand with your feet hip-width apart, one foot slightly in front of the other.",
                "Start with the kettlebell on the same side as your front foot, held in front of your chest near your shoulder and resting on your forearm.",
                PRESS_BRACE,
                SPOTTER_BEHIND,
            ),
            steps = listOf(
                "Push your front foot into the floor to shift your weight forward, and press the kettlebell straight overhead.",
                "Pull your elbow back down to your ribs to lower it.",
                SWITCH_ARMS,
            ),
            cues = listOf("Press straight up.", "Elbow back down to your ribs."),
            mistakes = listOf(
                "Letting the kettlebell drop instead of pulling it down.",
                "Pressing out to the side instead of straight overhead.",
            ),
        ),
        listOf(Sources.ACE_SINGLE_ARM_OVERHEAD_PRESS, Sources.NSCA_STANDARDS_SUMMARY, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Lateral raises ----
    shoulderEntry(
        "Lateral raise",
        listOf("DB lateral raise", "Dumbbell lateral raise", "Laterals", "Side raise", "Side lateral raise", "Standing lateral raise"),
        listOf(Equipment.DUMBBELL), SIDE, Kind.ACCESSORY,
        FormGuide(
            purpose = "A raise of your arms out to the sides with dumbbells. It works the side of your shoulders.",
            setup = listOf(
                "Stand holding a dumbbell in each hand, palms facing your body.",
                "Set your feet a little wider than hip-width, or in a split stance.",
                "Bend your elbows slightly and point them back.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                ELBOWS_TO_SHOULDER,
                "At the top, your elbows are out to the sides of your shoulders. The dumbbells don't need to be.",
                LOWER_SLOWLY,
            ),
            cues = listOf("Elbows to shoulder height.", "Elbows slightly bent and pointing back."),
            mistakes = listOf(THROWING, ELBOWS_BELOW_WRISTS, ELBOWS_BEHIND),
        ),
        listOf(Sources.EXRX_DB_LATERAL_RAISE, Sources.ACE_LATERAL_RAISE, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Seated lateral raise",
        listOf("Seated DB lateral raise", "Seated dumbbell lateral raise", "Seated side raise"),
        listOf(Equipment.DUMBBELL), SIDE, Kind.ACCESSORY,
        FormGuide(
            purpose = "A lateral raise done seated on the end of a bench. It works the side of your shoulders.",
            setup = listOf(
                "Sit on the end of a bench with your legs together, holding a dumbbell in each hand.",
                "Let your arms hang down, slightly bent, with your palms facing in.",
                LIGHT_BRACE,
            ),
            steps = listOf(ELBOWS_TO_SHOULDER, SAME_BEND, LOWER_SLOWLY),
            cues = listOf("Elbows to shoulder height.", "Same elbow bend all the way."),
            mistakes = listOf(THROWING, ELBOWS_BELOW_WRISTS),
        ),
        listOf(Sources.EXRX_DB_SEATED_LATERAL_RAISE, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Cable lateral raise",
        listOf("One-arm cable lateral raise", "Single-arm cable lateral raise", "Cable side raise"),
        listOf(Equipment.CABLE), SIDE, Kind.ACCESSORY,
        FormGuide(
            purpose = "A one-arm lateral raise with a low cable. It works the side of your shoulder.",
            setup = listOf(
                "Set a handle on a low pulley.",
                "Stand side-on to the pulley, with your free arm nearest to it. Grip the handle with your other hand.",
                "Bend your elbow slightly and point it back.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Raise your arm out to the side, away from the pulley, until your elbow is at shoulder height.",
                LOWER_SLOWLY,
                SWITCH_ARMS,
            ),
            cues = listOf("Elbow to shoulder height.", "Elbow slightly bent."),
            mistakes = listOf("Letting your elbow drop, which moves the work to the front of your shoulder.", THROWING),
        ),
        listOf(Sources.EXRX_CB_ONE_ARM_LATERAL_RAISE, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Machine lateral raise",
        listOf("Lateral raise machine", "Lever lateral raise", "Machine side raise"),
        listOf(Equipment.MACHINE), SIDE, Kind.ACCESSORY,
        FormGuide(
            purpose = "A lateral raise on a machine, pushing out against pads on your arms. It works the side of your shoulders.",
            setup = listOf(
                "Sit in the machine with your bent arms between the pads and your sides, so the pads rest on the outside of your arms.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Raise your arms out to the sides until your upper arms are level with the floor.",
                LOWER_SLOWLY,
            ),
            cues = listOf("Upper arms to level.", "If your elbows drift behind you, lean forward slightly."),
            mistakes = listOf(THROWING, ELBOWS_BEHIND),
        ),
        listOf(Sources.EXRX_LV_LATERAL_RAISE, Sources.EXRX_ERRORS, Sources.EXRX_DB_LATERAL_RAISE, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Front raises ----
    shoulderEntry(
        "Front raise",
        listOf("DB front raise", "Dumbbell front raise"),
        listOf(Equipment.DUMBBELL), FRONT, Kind.ACCESSORY,
        FormGuide(
            purpose = "A raise of dumbbells straight out in front of you. It works the front of your shoulders.",
            setup = listOf(
                "Stand holding a dumbbell in each hand, resting against your thighs, palms facing your thighs.",
                "Brace your abs, and pull your shoulder blades down and back.",
            ),
            steps = listOf(
                "Breathe out and raise the dumbbells forward and up until your arms are at shoulder height.",
                "Keep your elbows straight or slightly bent.",
                "Breathe in and lower them slowly.",
            ),
            cues = listOf("Arms to shoulder height.", "Shoulder blades down and back.", "Lower slowly."),
            mistakes = listOf("Raising the dumbbells higher than your shoulders.", "Dropping the weights fast on the way down."),
        ),
        listOf(Sources.ACE_FRONT_RAISE, Sources.EXRX_DB_FRONT_RAISE),
    ),
    shoulderEntry(
        "Barbell front raise",
        listOf("BB front raise"),
        listOf(Equipment.BARBELL), FRONT, Kind.ACCESSORY,
        FormGuide(
            purpose = "A front raise with a barbell. It works the front of your shoulders.",
            setup = listOf(
                "Stand holding a barbell in front of your thighs, palms facing your thighs.",
                "Keep your elbows nearly straight.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Raise the bar forward and up to about shoulder height.",
                LOWER_SLOWLY,
            ),
            cues = listOf("Bar to shoulder height.", "Elbows nearly straight."),
            mistakes = listOf("Raising the bar higher than your shoulders.", "Leaning back or using your hips to swing the bar up."),
        ),
        listOf(Sources.EXRX_BB_FRONT_RAISE, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Cable front raise",
        listOf("One-arm cable front raise", "Single-arm cable front raise"),
        listOf(Equipment.CABLE), FRONT, Kind.ACCESSORY,
        FormGuide(
            purpose = "A one-arm front raise with a low cable. It works the front of your shoulder.",
            setup = listOf(
                "Set a handle on a low pulley and grip it with one hand.",
                "Stand a little away from the pulley, with your arm slightly back at your side.",
                "Keep your elbow straight or slightly bent.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Raise the handle forward and up to about shoulder height.",
                LOWER_SLOWLY,
                SWITCH_ARMS,
            ),
            cues = listOf("Arm to shoulder height.", "Elbow straight or slightly bent."),
            mistakes = listOf("Raising the handle higher than your shoulder.", "Leaning back or using your hips to swing the handle up."),
        ),
        listOf(Sources.EXRX_CB_ONE_ARM_FRONT_RAISE, Sources.ACE_FRONT_RAISE, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Rear delts ----
    shoulderEntry(
        "Rear delt raise",
        listOf(
            "Rear delt fly", "Reverse fly", "Bent-over reverse fly", "Bent-over lateral raise", "Bent-over rear delt raise",
            "Rear lateral raise", "DB rear delt raise", "Dumbbell reverse fly",
        ),
        listOf(Equipment.DUMBBELL), REAR, Kind.ACCESSORY,
        FormGuide(
            purpose = "A bent-over raise of dumbbells out to the sides. It works your rear delts and upper back.",
            setup = listOf(
                "Hold a dumbbell in each hand, palms facing each other.",
                "Bend your knees and bend over at your hips, until your torso is close to level with the floor.",
                "Bend your elbows slightly.",
                BENT_BRACE,
            ),
            steps = listOf(
                ELBOWS_TO_SHOULDER,
                "Keep your upper arms straight out from your shoulders, and keep the bend in your elbows the same.",
                LOWER_SLOWLY,
            ),
            cues = listOf("Torso close to level.", "Elbows straight out from your shoulders."),
            mistakes = listOf(
                "Rounding your lower back. If it rounds, bend your knees more or don't bend over as far.",
                NOT_BENT_OVER,
                "Letting your elbows drift toward your hips, which moves the work to your lats.",
                "Throwing the weight up.",
            ),
        ),
        listOf(Sources.EXRX_DB_REAR_LATERAL_RAISE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Seated rear delt raise",
        listOf("Seated rear lateral raise", "Seated reverse fly", "Seated bent-over raise"),
        listOf(Equipment.DUMBBELL), REAR, Kind.ACCESSORY,
        FormGuide(
            purpose = "A rear delt raise done sitting, with your chest resting on your thighs. It works your rear delts and upper back.",
            setup = listOf(
                "Sit on the edge of a bench, holding a dumbbell in each hand.",
                "Bend over and rest your chest on your thighs.",
                "Bend your elbows slightly.",
                LIGHT_BRACE,
            ),
            steps = listOf(ELBOWS_TO_SHOULDER, SAME_BEND, LOWER_SLOWLY),
            cues = listOf("Chest on your thighs.", "Elbows to shoulder height."),
            mistakes = listOf(
                "Sitting up as you lift, which moves the work away from your rear delts.",
                "Letting your elbows drift toward your hips, which moves the work to your lats.",
            ),
        ),
        listOf(Sources.EXRX_DB_SEATED_REAR_LATERAL_RAISE, Sources.EXRX_DB_REAR_LATERAL_RAISE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Chest-supported rear delt raise",
        listOf("Lying rear delt raise", "Prone rear delt raise", "Lying rear lateral raise", "Chest-supported reverse fly"),
        listOf(Equipment.DUMBBELL), REAR, Kind.ACCESSORY,
        FormGuide(
            purpose = "A rear delt raise lying face down on a bench. It works your rear delts and upper back, with your chest supported.",
            setup = listOf(
                "Set a bench flat or close to flat, high enough that the dumbbells clear the floor.",
                "Lie face down on it, holding a dumbbell in each hand.",
                "Bend your elbows slightly.",
                LIGHT_BRACE,
            ),
            steps = listOf(ELBOWS_TO_SHOULDER, SAME_BEND, LOWER_SLOWLY),
            cues = listOf("Bench close to flat.", "Elbows to shoulder height."),
            mistakes = listOf(
                "Setting the bench at a steep incline, which moves the work away from your rear delts.",
                "Letting your elbows drift toward your hips, which moves the work to your lats.",
            ),
        ),
        listOf(Sources.EXRX_DB_LYING_REAR_LATERAL_RAISE, Sources.EXRX_DB_REAR_LATERAL_RAISE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Machine reverse fly",
        listOf("Reverse pec deck", "Rear delt machine", "Reverse machine fly", "Lever reverse fly", "Machine rear delt fly"),
        listOf(Equipment.MACHINE), REAR, Kind.ACCESSORY,
        FormGuide(
            purpose = "A reverse fly on a machine, facing the pad. It works your rear delts and upper back.",
            setup = listOf(
                "Set the seat so the handles are at shoulder height.",
                "Sit facing the machine with your chest against the pad.",
                "Grip the handles palms down, with your elbows raised to shoulder height.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Pull the handles apart and back until your elbows are just behind your torso.",
                "Keep your elbows at shoulder height.",
                "Return under control.",
            ),
            cues = listOf("Elbows at shoulder height.", "Chest on the pad."),
            mistakes = listOf(ELBOWS_TO_LATS, "Pulling your elbows far past your torso."),
        ),
        listOf(Sources.EXRX_LV_REVERSE_FLY, Sources.EXRX_CB_REVERSE_FLY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Cable reverse fly",
        listOf("Cable rear delt fly", "Standing cable reverse fly", "Cable rear delt raise"),
        listOf(Equipment.CABLE), REAR, Kind.ACCESSORY,
        FormGuide(
            purpose = "A reverse fly with two cables at shoulder height. It works your rear delts and upper back.",
            setup = listOf(
                "Set two pulleys close together, at about shoulder height.",
                "Face the pulleys and grip a handle in each hand. Step back until the cables are tight.",
                "Stand with your feet staggered. Keep your arms straight or slightly bent, with your elbows pointing out.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Pull the handles out to the sides.",
                "Keep your upper arms moving level, at shoulder height, and keep the bend in your elbows the same.",
                "Return under control.",
            ),
            cues = listOf("Arms level at shoulder height.", "Elbows stay fixed."),
            mistakes = listOf(
                "Letting your arms drop below shoulder height, which brings in your lats.",
                "Bending and straightening your elbows during the rep.",
            ),
        ),
        listOf(Sources.EXRX_CB_REVERSE_FLY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Cable rear delt row",
        listOf("Rear delt row", "Seated cable rear delt row", "Seated rear delt row"),
        listOf(Equipment.CABLE), REAR, Kind.ACCESSORY,
        FormGuide(
            purpose = "A seated cable row to the upper chest, with your elbows up and out. It works your rear delts and upper back.",
            setup = listOf(
                "Attach a bar to a seated cable row.",
                "Sit slightly forward to grip the bar palms down, with your hands about elbow-width apart.",
                "Sit up straight and slide your hips back until your knees are only slightly bent.",
                "Brace your abs and keep your back straight.",
            ),
            steps = listOf(
                "Pull the bar toward your upper chest, just below your neck, with your elbows up and out to the sides.",
                "Keep your upper arms level.",
                "Stop when your elbows are slightly behind your back.",
                "Return under control.",
            ),
            cues = listOf("Elbows up and out.", "Torso upright."),
            mistakes = listOf(ELBOWS_TO_LATS, "Leaning forward past upright, which also brings in your lats."),
        ),
        listOf(Sources.EXRX_CB_REAR_DELT_ROW, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Rotator cuff ----
    shoulderEntry(
        "Cable external rotation",
        listOf(
            "External rotation", "Band external rotation", "Cable shoulder external rotation", "Standing external rotation",
            "Rotator cuff external rotation",
        ),
        listOf(Equipment.CABLE, Equipment.BAND), CUFF, Kind.ACCESSORY,
        FormGuide(
            purpose = "Turning your arm outward against a cable or band, with your elbow at your side. It works your rotator cuff.",
            setup = listOf(
                "Set a cable handle at about waist height. With a band, fix it at the same height.",
                "Stand side-on to it and hold the handle in the hand farther from it.",
                ELBOW_AT_SIDE,
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Turn your arm outward, moving your hand away from your body.",
                "Pause briefly, then return slowly.",
                SWITCH_ARMS,
            ),
            cues = listOf("Elbow against your side.", "Same elbow bend all the way."),
            mistakes = listOf(ELBOW_DRIFTS, BEND_CHANGES),
        ),
        listOf(Sources.EXRX_CB_EXTERNAL_ROTATION, Sources.ACE_ROTATOR_CUFF_EXTERNAL_ROTATION, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Side-lying external rotation",
        listOf("Lying external rotation", "DB external rotation", "Dumbbell external rotation", "Side-lying DB external rotation"),
        listOf(Equipment.DUMBBELL), CUFF, Kind.ACCESSORY,
        FormGuide(
            purpose = "Turning your upper arm outward with a dumbbell while lying on your side. It works your rotator cuff.",
            setup = listOf(
                "Lie on your side, with your legs apart for support.",
                "Hold a dumbbell in your top hand.",
                ELBOW_AT_SIDE,
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Turn your upper arm outward to raise the dumbbell, keeping your elbow against your side.",
                LOWER_SLOWLY,
                "Do all your reps, then switch sides.",
            ),
            cues = listOf("Elbow against your side.", "Same elbow bend all the way."),
            mistakes = listOf(
                ELBOW_DRIFTS,
                BEND_CHANGES,
                "Doing it standing with a dumbbell. Lie on your side, so the weight works against the turn.",
            ),
        ),
        listOf(Sources.EXRX_DB_LYING_EXTERNAL_ROTATION, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE),
    ),
    shoulderEntry(
        "Cable internal rotation",
        listOf("Internal rotation", "Cable shoulder internal rotation", "Standing internal rotation"),
        listOf(Equipment.CABLE), CUFF, Kind.ACCESSORY,
        FormGuide(
            purpose = "Turning your arm inward against a cable, with your elbow at your side. It works your rotator cuff.",
            setup = listOf(
                "Set a handle on a cable at about elbow height.",
                "Stand side-on to the pulley and hold the handle in the hand nearer to it.",
                "Bend your elbow and keep it against your side.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Turn your arm inward, bringing your forearm across your belly.",
                "Return under control.",
                SWITCH_ARMS,
            ),
            cues = listOf("Elbow against your side.", "Same elbow bend all the way."),
            mistakes = listOf(ELBOW_DRIFTS, BEND_CHANGES),
        ),
        listOf(Sources.EXRX_CB_INTERNAL_ROTATION, Sources.ACE_BRACE_OR_BREATHE),
    ),
)
