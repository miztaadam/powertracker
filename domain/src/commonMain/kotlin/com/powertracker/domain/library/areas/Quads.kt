package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

// Shared lines, so the safety and bracing wording is the same in every quad guide.
private const val BRACE_TRUNK = "Brace your trunk before each rep."
private const val LIGHT_BRACE = "Brace your trunk lightly, and keep breathing."
private const val SHOULDERS_SET = "Pull your shoulders down and back, without arching your lower back."
private const val DBS_AT_SIDES = "Hold a dumbbell in each hand at your sides."
private const val HIPS_BACK = "Push your hips back and let your knees bend forward. Keep your back straight."
// Depth: ExRx's squat pages. The stop-sooner rule is ACE's bodyweight squat limit (ACE 135), used for every squat here.
private const val SQUAT_DEPTH = "Go down until your thighs are just past level with the floor. Stop sooner if your heels lift or your back starts to round."
private const val STAND_UP = "Stand up by straightening your knees and hips."
private const val KNEES_TRACK = "Knees point the same way as your feet."
private const val WHOLE_FOOT = "Weight over your whole foot."
private const val UPRIGHT = "Torso upright."
private const val KNEES_CAVE = "Letting your knees cave in."
private const val FRONT_KNEE_CAVES = "Letting your front knee cave in."
private const val HEELS_LIFT = "Heels lifting off the floor."
private const val BACK_ROUNDS = "Letting your back round."
private const val LEANING = "Leaning your torso far forward."
// ACE 94 stops at the front thigh level with the floor; ExRx stops with the back knee just above the floor. Both together.
private const val LUNGE_DEPTH = "Lower until your front thigh is about level with the floor and your back knee is just above it. Don't let your knee touch the floor."
private const val SWITCH_EACH_REP = "Switch legs each rep."
private const val SWITCH_LEGS = "Do all your reps on one leg, then switch."
private const val KNEE_HITS = "Dropping fast so your back knee hits the floor."
// Smith machine: ExRx says the stops can hold the bar at its lowest point (Smith seated calf raise). Setting them is an
// operational step; the height is carried over from the rack rule.
private const val SMITH_RACK = "Set the bar at upper-chest height."
private const val SMITH_SAFETY = "Set the safety stops just below your lowest position."
private const val SMITH_UNHOOK = "Unhook the bar by turning it back off the hooks."
private const val SMITH_REHOOK = "After the last rep, turn the bar forward onto the hooks."
private const val NO_STOPS = "Training without the safety stops set."
// ACE 319 gives the grip for the barbell reverse lunge; ExRx BBLunge says only "grasp bar to sides".
private const val BAR_ON_UPPER_BACK = "Step under the bar and rest it across your upper back."
// Box height: ACE "6 Lower Body Exercises to Do Instead of Squats" (knee height or a little lower).
private const val BOX_HEIGHT = "Use a sturdy box or bench no higher than your knee."

private val SQUAT_MUSCLES = listOf("Quads", "Glutes")
private val LEG_MUSCLES = listOf("Quads", "Glutes", "Adductors")

private fun quadEntry(
    name: String,
    aliases: List<String>,
    equipment: List<Equipment>,
    muscles: List<String>,
    guide: FormGuide,
    sources: List<SourceId>,
    kind: Kind = Kind.COMPOUND,
) = LibraryExercise(
    name, aliases, BodyArea.QUADS, equipment, muscles, kind, Category.ACCESSORY,
    parentLift = null, guide = guide, sources = sources,
)

/** BodyArea.QUADS entries. */
internal val quads: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "Bulgarian split squat",
        aliases = listOf("BSS", "Rear-foot-elevated split squat", "RFESS"),
        area = BodyArea.QUADS,
        equipment = listOf(Equipment.DUMBBELL, Equipment.BARBELL),
        muscles = listOf("Quads", "Glutes"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A single-leg squat with your rear foot up on a bench. It trains each leg on its own, mostly the quads and glutes.",
            setup = listOf(
                "Hold a dumbbell in each hand at your sides. With a barbell, take it from a rack with the safety pins set, or use a spotter.",
                "Stand facing away from a bench.",
                "Reach one leg back and rest the top of that foot on the bench.",
                "Brace your trunk.",
            ),
            steps = listOf(
                "Bend your front knee and hip to lower yourself.",
                "Go down until your back knee is nearly touching the floor.",
                "Drive up through your front foot.",
                "Do all your reps on one leg, then switch.",
            ),
            cues = listOf("Front knee points the same way as your foot.", "Torso upright."),
            mistakes = listOf(
                "Letting your front knee cave in.",
                "Leaning far forward.",
                "Dropping fast so your back knee hits the floor.",
            ),
        ),
        sources = listOf(Sources.EXRX_DB_SINGLE_LEG_SPLIT_SQUAT, Sources.PMC_BULGARIAN_SPLIT_SQUAT),
    ),

    // ---- Free-weight and bodyweight squats ----
    quadEntry(
        "Goblet squat", listOf("Goblet", "DB goblet squat", "Dumbbell goblet squat", "KB goblet squat", "Kettlebell goblet squat"),
        listOf(Equipment.DUMBBELL, Equipment.KETTLEBELL), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A squat holding one dumbbell or kettlebell at your chest. It trains your quads and glutes, and the weight in front helps you stay upright.",
            setup = listOf(
                "Stand with your feet about shoulder-width apart or a little wider.",
                "Hold one dumbbell upright against your chest with both hands, or a kettlebell by the sides of its handle.",
                "Keep your elbows close to your ribs.",
                BRACE_TRUNK,
            ),
            steps = listOf(HIPS_BACK, SQUAT_DEPTH, STAND_UP),
            cues = listOf(KNEES_TRACK, "Chest up.", WHOLE_FOOT),
            mistakes = listOf(KNEES_CAVE, HEELS_LIFT, BACK_ROUNDS),
        ),
        listOf(Sources.EXRX_KB_GOBLET_SQUAT, Sources.ACE_GOBLET_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "DB squat", listOf("Dumbbell squat"), listOf(Equipment.DUMBBELL), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A squat holding a dumbbell in each hand. It trains your quads and glutes without a bar on your back.",
            setup = listOf(DBS_AT_SIDES, "Stand with your feet about shoulder-width apart.", BRACE_TRUNK),
            steps = listOf(HIPS_BACK, SQUAT_DEPTH, STAND_UP),
            cues = listOf(KNEES_TRACK, "Chest up.", WHOLE_FOOT),
            mistakes = listOf(KNEES_CAVE, HEELS_LIFT, BACK_ROUNDS),
        ),
        listOf(Sources.EXRX_DB_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Bodyweight squat", listOf("Air squat", "Body-weight squat"), listOf(Equipment.BODYWEIGHT), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A squat with no added weight. It trains your quads and glutes.",
            setup = listOf(
                "Stand with your feet a little wider than your hips, toes turned slightly out.",
                SHOULDERS_SET,
                "Brace your trunk and lift your chest.",
            ),
            steps = listOf(
                "Push your hips back, then down, so your hips and knees bend together. Keep your back as flat as you can.",
                "Go down until your thighs are about level with the floor. Stop sooner if your heels start to lift or your back starts to round.",
                "Push through your feet to stand back up, keeping your chest up.",
            ),
            cues = listOf("Knees over your second toes.", "Back flat.", "Feet flat on the floor."),
            mistakes = listOf("Letting your ankles or knees cave in.", HEELS_LIFT, BACK_ROUNDS),
        ),
        listOf(Sources.ACE_BODYWEIGHT_SQUAT, Sources.EXRX_BW_SQUAT),
    ),
    quadEntry(
        "Kettlebell front squat", listOf("KB front squat", "Double kettlebell front squat"), listOf(Equipment.KETTLEBELL), SQUAT_MUSCLES + "Trunk",
        FormGuide(
            purpose = "A squat holding two kettlebells in front of your shoulders. It trains your quads and glutes, and your trunk works to keep you upright.",
            setup = listOf(
                // No allowed source says how to get the kettlebells there, so this gives only the start position.
                "Start with a kettlebell in front of each shoulder, arms close to your body and wrists straight.",
                "Stand with your feet shoulder-width apart or a little wider.",
                BRACE_TRUNK,
            ),
            steps = listOf(HIPS_BACK, SQUAT_DEPTH, STAND_UP),
            cues = listOf("Wrists straight.", KNEES_TRACK, "Chest up."),
            mistakes = listOf(KNEES_CAVE, HEELS_LIFT, BACK_ROUNDS),
        ),
        listOf(Sources.EXRX_KB_FRONT_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Zercher squat", listOf("Barbell Zercher squat"), listOf(Equipment.BARBELL), listOf("Quads", "Glutes", "Upper back", "Trunk"),
        FormGuide(
            purpose = "A squat with the bar held in the crook of your elbows. The weight sits in front of you, so you squat with an upright torso.",
            setup = listOf(
                "Set the bar in the rack at about the height of your upper waist.",
                RACK_SAFETY,
                "Put your forearms under the bar and rest it in the crook of your elbows, about shoulder-width apart.",
                "Cross your wrists and pull the bar in close against your body.",
                "Stand with your feet shoulder-width apart or a little wider.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Lift the bar out and step back.",
                "Bend your hips and knees together, keeping your torso upright and your head in line with your spine.",
                "Go down until your thighs are about level with the floor. Stop sooner if your shoulders or back start to round.",
                STAND_UP,
                "Put the bar back in the rack after the last rep.",
            ),
            cues = listOf(UPRIGHT, "Bar close to your body.", KNEES_TRACK),
            mistakes = listOf(
                "Letting your shoulders and back round forward.",
                "Letting your knees drift out of line with your feet.",
            ),
        ),
        listOf(Sources.EXRX_BB_ZERCHER_SQUAT, Sources.NSCA_ZERCHER_SQUAT),
    ),
    quadEntry(
        "Single-leg box squat", listOf("Box pistol", "Bench pistol squat", "Single-leg bench squat"), listOf(Equipment.BODYWEIGHT), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A one-leg squat down to a box. It trains each leg on its own, and the box sets how deep you go.",
            setup = listOf(
                "Stand in front of a sturdy bench or box, facing away from it. Its height sets your depth.",
                "Hold your arms out in front of you.",
                "Balance on one leg, with the other foot just off the floor.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Squat down on one leg, keeping your other foot off the floor.",
                "Lower until your buttocks touch the box. Don't drop onto it.",
                "Stand back up until your knee and hip are straight.",
                SWITCH_LEGS,
            ),
            cues = listOf("Back straight.", "Knee points the same way as your foot."),
            mistakes = listOf("Letting your knee cave in.", "Dropping onto the box.", "Letting your other foot touch the floor."),
        ),
        listOf(Sources.EXRX_SINGLE_LEG_BOX_SQUAT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Jump squat", listOf("Squat jump", "Squat jumps"), listOf(Equipment.BODYWEIGHT), SQUAT_MUSCLES + "Calves",
        FormGuide(
            purpose = "A squat that ends in a jump. It trains your legs to produce force fast.",
            setup = listOf(
                "Stand with your feet hip-width apart and your arms at your sides.",
                SHOULDERS_SET,
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Push your hips back and bend your knees, stopping before your heels lift.",
                "Jump up explosively.",
                "Land softly on the middle of your feet, then roll back toward your heels.",
                "As you land, push your hips back and down to soak up the landing.",
            ),
            cues = listOf("Land softly and quietly.", "Hips back on landing.", "Back flat."),
            mistakes = listOf(
                "Landing with your knees locked.",
                "Letting your knees shoot far forward on landing.",
                "Landing hard and loud.",
            ),
        ),
        listOf(Sources.ACE_SQUAT_JUMPS, Sources.ACE_SQUAT_JUMP),
    ),

    // ---- Smith machine ----
    quadEntry(
        "Smith squat", listOf("Smith machine squat"), listOf(Equipment.SMITH), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A squat with the bar fixed in a Smith machine. The bar moves on rails, so you don't have to balance it.",
            setup = listOf(
                SMITH_RACK,
                SMITH_SAFETY,
                "Rest the bar across the back of your shoulders and grip it out to the sides.",
                "Place your feet under the bar.",
                BRACE_TRUNK,
                SMITH_UNHOOK,
            ),
            steps = listOf(HIPS_BACK, SQUAT_DEPTH, STAND_UP, SMITH_REHOOK),
            cues = listOf(KNEES_TRACK, WHOLE_FOOT, "Head faces forward."),
            mistakes = listOf(HEELS_LIFT, KNEES_CAVE, NO_STOPS),
        ),
        listOf(
            Sources.EXRX_SM_SQUAT, Sources.EXRX_SM_SEATED_CALF_RAISE,
            Sources.ACE_BODYWEIGHT_SQUAT, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    quadEntry(
        "Smith front squat", listOf("Smith machine front squat"), listOf(Equipment.SMITH), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A front squat in a Smith machine, with the bar on the front of your shoulders. It puts more of the work on your quads.",
            setup = listOf(
                SMITH_RACK,
                SMITH_SAFETY,
                "Grip the bar palms down, shoulder-width apart.",
                "Rest the bar on the front of your shoulders with your elbows pointing forward.",
                "Place your feet under the bar.",
                BRACE_TRUNK,
                SMITH_UNHOOK,
            ),
            steps = listOf(HIPS_BACK, SQUAT_DEPTH, STAND_UP, SMITH_REHOOK),
            cues = listOf("Elbows point forward.", KNEES_TRACK, WHOLE_FOOT),
            mistakes = listOf(HEELS_LIFT, KNEES_CAVE, NO_STOPS),
        ),
        listOf(
            Sources.EXRX_SM_FRONT_SQUAT, Sources.EXRX_SM_SEATED_CALF_RAISE,
            Sources.ACE_BODYWEIGHT_SQUAT, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    quadEntry(
        "Smith split squat", listOf("Smith machine split squat"), listOf(Equipment.SMITH), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A split squat with the bar fixed in a Smith machine. It trains each leg in turn without you having to balance a bar.",
            setup = listOf(
                SMITH_RACK,
                SMITH_SAFETY,
                "Rest the bar across the back of your shoulders and grip it out to the sides.",
                "Place one foot slightly forward, and the other well back on the ball of the foot.",
                BRACE_TRUNK,
                SMITH_UNHOOK,
            ),
            steps = listOf(
                "Bend both knees and hips to lower until your back knee is almost touching the floor.",
                "Straighten both legs to stand back up.",
                SWITCH_LEGS,
                SMITH_REHOOK,
            ),
            cues = listOf(UPRIGHT, KNEES_TRACK),
            mistakes = listOf(LEANING, FRONT_KNEE_CAVES, NO_STOPS),
        ),
        listOf(
            Sources.EXRX_SM_SPLIT_SQUAT, Sources.EXRX_SM_SEATED_CALF_RAISE,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),

    // ---- Machines ----
    quadEntry(
        "Leg press", listOf("45-degree leg press", "Sled leg press"), listOf(Equipment.MACHINE), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A machine press where you push a weighted platform away with your legs. It trains your quads and glutes with no load on your back.",
            setup = listOf(
                "Sit with your back and tailbone flat against the back pad.",
                "Place your feet on the platform, toes pointing forward. Lower on the platform puts more work on your quads; higher puts more on your glutes.",
                "Set the safety stops below your lowest position.",
                "Brace your trunk to keep your lower back still.",
                "Push the platform up until your legs are nearly straight. Release the lock handles, then hold the side handles.",
            ),
            steps = listOf(
                "Lower the platform under control by bending your hips and knees.",
                "Lower until your knees bend to about 90 degrees. Stop sooner if your hips start to lift off the pad. Don't let your thighs press into your ribs.",
                "Push back up through your heels and the balls of your feet until your legs are straight but not locked.",
                // ExRx's single-leg page says to re-engage the dock levers before dismounting.
                "After the last rep, put the lock handles back on before you get off.",
            ),
            cues = listOf("Knees point the same way as your toes.", "Heels stay on the platform.", "Back and tailbone stay on the pad."),
            mistakes = listOf(
                "Locking your knees at the top.",
                "Lifting your hips off the pad or rounding your lower back at the bottom.",
                "Letting your heels lift off the platform.",
            ),
        ),
        listOf(
            Sources.EXRX_SL_45_LEG_PRESS, Sources.EXRX_SL_45_LEG_PRESS_GLUTES, Sources.ACE_SEATED_LEG_PRESS,
            Sources.EXRX_SL_SINGLE_LEG_PRESS,
        ),
    ),
    quadEntry(
        "Single-leg leg press", listOf("One-leg leg press", "Single-leg 45-degree leg press"), listOf(Equipment.MACHINE), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A leg press with one leg at a time. It trains each leg on its own.",
            setup = listOf(
                "Sit with your back flat against the back pad.",
                "Place one foot on the platform and the other on the floor below it.",
                "Set the safety stops below your lowest position.",
                "Brace your trunk to keep your lower back still.",
                "Push the platform up with your working leg. Release the lock handles, then hold the side handles.",
            ),
            steps = listOf(
                "Lower the platform until your knee bends to about 90 degrees. Stop sooner if your hips start to lift off the pad. Don't let your thigh press into your ribs.",
                "Push back up until your leg is straight but not locked.",
                SWITCH_LEGS,
                "After the last rep, put the lock handles back on before you get off.",
            ),
            cues = listOf("Knee points the same way as your foot.", "Heel stays on the platform."),
            mistakes = listOf("Letting your heel lift off the platform.", "Lifting your hips off the pad at the bottom.", "Locking your knee at the top."),
        ),
        listOf(Sources.EXRX_SL_SINGLE_LEG_PRESS, Sources.ACE_SEATED_LEG_PRESS, Sources.EXRX_SL_45_LEG_PRESS_GLUTES),
    ),
    quadEntry(
        "Hack squat", listOf("Hack", "Machine hack squat", "Sled hack squat"), listOf(Equipment.MACHINE), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A machine squat on a sled, with your back against a pad and your shoulders under pads. It trains your quads and glutes.",
            setup = listOf(
                "Lean back on the sled with your shoulders under the pads.",
                "Place your feet on the platform. Lower puts more work on your quads; higher puts more on your glutes.",
                BRACE_TRUNK,
                "Straighten your legs and release the lock.",
            ),
            steps = listOf(
                "Lower by bending your hips and knees until your knees are just short of fully bent.",
                "Push back up by straightening your knees and hips.",
                "After the last rep, put the lock back on with your legs straight before you get off.",
            ),
            cues = listOf(KNEES_TRACK, "Heels stay down.", "Push through your whole foot."),
            mistakes = listOf("Letting your heels lift.", KNEES_CAVE, "Getting off without locking the sled."),
        ),
        listOf(Sources.EXRX_SL_HACK_SQUAT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Lever squat", listOf("Plate-loaded lever squat"), listOf(Equipment.MACHINE), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A squat on a machine with a padded lever across your shoulders. It trains your quads and glutes on a fixed path.",
            setup = listOf(
                "Set the lever height so your hips can go down level with your knees or slightly lower.",
                "Squat down and put your shoulders under the pads.",
                "Place your feet shoulder-width apart, directly under your shoulders.",
                BRACE_TRUNK,
                "Straighten your legs and release the support lever.",
            ),
            steps = listOf(
                HIPS_BACK,
                "Go down until your thighs are just past level with the floor.",
                STAND_UP,
                "After the last rep, put the support lever back on before you step out.",
            ),
            cues = listOf(KNEES_TRACK, WHOLE_FOOT, "Head faces forward."),
            mistakes = listOf(HEELS_LIFT, KNEES_CAVE, "Stepping out without putting the support lever back on."),
        ),
        listOf(Sources.EXRX_LV_SQUAT, Sources.EXRX_LV_SQUAT_PL, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Leg extension", listOf("Leg ext", "Quad extension"), listOf(Equipment.MACHINE), listOf("Quads"),
        FormGuide(
            purpose = "A seated machine exercise where you straighten your knees against a pad. It works your quads on their own.",
            setup = listOf(
                "Sit with your back against the back pad.",
                "Put the front of your lower legs under the lower pad.",
                "Line up your knees with the machine's pivot.",
                "Hold the handles at your sides. Use the seat belt if there is one.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Straighten your knees to lift the pad until your legs are straight.",
                "Bend your knees to lower the pad back to the start.",
            ),
            cues = listOf("Knees in line with the pivot.", "Hips stay on the seat."),
            mistakes = listOf("Your hips rising off the seat on heavy sets.", "Sitting with your knees out of line with the pivot."),
        ),
        listOf(Sources.EXRX_LV_LEG_EXTENSION, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    quadEntry(
        "Single-leg leg extension", listOf("One-leg leg extension", "Alternating leg extension"), listOf(Equipment.MACHINE), listOf("Quads"),
        FormGuide(
            purpose = "A leg extension with one leg at a time. It works each quad on its own.",
            setup = listOf(
                "Sit with your back against the back pad.",
                "Put the front of your lower legs under the pad or pads.",
                "Line up your knees with the machine's pivot.",
                "Hold the handles at your sides.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Straighten one knee to lift the pad until that leg is straight.",
                "Bend your knee to lower the pad back to the start.",
                "Switch legs.",
            ),
            cues = listOf("Knees in line with the pivot.", "Hips stay on the seat."),
            mistakes = listOf("Your hips rising off the seat.", "Sitting with your knees out of line with the pivot."),
        ),
        listOf(Sources.EXRX_LV_ALT_LEG_EXTENSION, Sources.EXRX_LV_LEG_EXTENSION, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    quadEntry(
        "Sled push", listOf("Prowler push"), listOf(Equipment.MACHINE), listOf("Quads", "Glutes", "Calves"),
        FormGuide(
            purpose = "Driving a weighted sled forward along the floor. It trains leg drive and power.",
            setup = listOf(
                "Stand facing the sled with your feet staggered.",
                "Bend down and grip the handles with both hands.",
                "Set your hips low and your back foot back, with your torso nearly upright and your arms straight.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "To start, lean into the sled, with your heels off the ground.",
                "Drive the sled forward with quick steps, keeping your weight low behind it.",
            ),
            cues = listOf("Arms straight.", "Stay low behind the sled.", "Heels off the ground."),
            mistakes = listOf("Standing up tall, so your weight is no longer behind the sled.", "Letting your arms bend."),
        ),
        listOf(Sources.EXRX_SLED_PUSH, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Lunges ----
    quadEntry(
        "Lunge", listOf("Forward lunge", "DB lunge", "Dumbbell lunge"), listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A step forward into a deep split stance. It trains your quads and glutes one leg at a time.",
            setup = listOf(
                "Stand with your feet together. Hold a dumbbell in each hand at your sides, or use no weight.",
                SHOULDERS_SET,
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Step forward with one leg. Land on your heel, then your forefoot.",
                LUNGE_DEPTH,
                "Push off your front leg to step back to standing.",
                SWITCH_EACH_REP,
            ),
            cues = listOf(UPRIGHT, "Front knee points the same way as your foot."),
            mistakes = listOf(FRONT_KNEE_CAVES, LEANING, KNEE_HITS),
        ),
        listOf(Sources.ACE_FORWARD_LUNGE, Sources.EXRX_DB_LUNGE, Sources.ACE_DB_LUNGE),
    ),
    quadEntry(
        "Walking lunge", listOf("DB walking lunge", "Dumbbell walking lunge"), listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), SQUAT_MUSCLES,
        FormGuide(
            purpose = "Lunges that travel forward, one step after another. It trains your quads and glutes and your balance.",
            setup = listOf(DBS_AT_SIDES + " Or use no weight.", SHOULDERS_SET, BRACE_TRUNK),
            steps = listOf(
                "Step forward with one leg. Land on your heel, then your forefoot.",
                LUNGE_DEPTH,
                "Stand up on your front leg, using your back leg to help.",
                "Bring your back leg forward into the next lunge. Keep alternating legs.",
            ),
            cues = listOf(UPRIGHT, "Front knee points the same way as your foot."),
            mistakes = listOf(FRONT_KNEE_CAVES, LEANING, KNEE_HITS),
        ),
        listOf(Sources.EXRX_DB_WALKING_LUNGE, Sources.ACE_LUNGE_VARIATIONS, Sources.ACE_FORWARD_LUNGE),
    ),
    quadEntry(
        "Reverse lunge", listOf("Rear lunge", "DB reverse lunge", "Backward lunge"), listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A lunge where you step back instead of forward. It trains your quads and glutes one leg at a time.",
            setup = listOf(DBS_AT_SIDES + " Or use no weight.", "Stand with your feet about hip-width apart.", BRACE_TRUNK),
            steps = listOf(
                "Step back with one leg, landing on the ball of that foot well behind you.",
                LUNGE_DEPTH,
                "Push through your front foot to stand, and bring your back leg forward beside it.",
                SWITCH_EACH_REP,
            ),
            cues = listOf(UPRIGHT, "Front knee points the same way as your foot."),
            mistakes = listOf(FRONT_KNEE_CAVES, LEANING, KNEE_HITS),
        ),
        listOf(Sources.EXRX_DB_REAR_LUNGE, Sources.ACE_REVERSE_LUNGE, Sources.ACE_LUNGE_VARIATIONS, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Barbell reverse lunge", listOf("BB reverse lunge", "Barbell rear lunge"), listOf(Equipment.BARBELL), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A reverse lunge with a barbell across your upper back. It lets you load each leg more heavily.",
            setup = listOf(
                "Set the bar in the rack at about shoulder height.",
                RACK_SAFETY,
                BAR_ON_UPPER_BACK,
                "Grip the bar a little wider than your shoulders.",
                BRACE_TRUNK,
                "Stand up to lift the bar off, and take one step back. Set your feet about hip-width apart.",
            ),
            steps = listOf(
                "Step back with one leg, landing on the ball of that foot.",
                LUNGE_DEPTH,
                "Press your front foot into the floor to stand, and bring your back leg forward.",
                SWITCH_EACH_REP,
                "Put the bar back in the rack after the last rep.",
            ),
            cues = listOf("Chest up.", "Hold the bar on both sides the whole time.", "Front knee points the same way as your foot."),
            mistakes = listOf(FRONT_KNEE_CAVES, LEANING, KNEE_HITS),
        ),
        listOf(Sources.ACE_REVERSE_LUNGE, Sources.ACE_LUNGE_VARIATIONS, Sources.EXRX_DB_REAR_LUNGE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Barbell lunge", listOf("BB lunge", "Barbell forward lunge"), listOf(Equipment.BARBELL), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A forward lunge with a barbell across your upper back. It lets you load each leg more heavily.",
            setup = listOf(
                "Set the bar in the rack at upper-chest height.",
                RACK_SAFETY,
                BAR_ON_UPPER_BACK,
                "Grip the bar out to the sides.",
                BRACE_TRUNK,
                "Lift the bar out and step back.",
            ),
            steps = listOf(
                "Step forward with one leg. Land on your heel, then your forefoot.",
                LUNGE_DEPTH,
                "Drive back up to standing by straightening the hip and knee of your front leg.",
                SWITCH_EACH_REP,
                "Put the bar back in the rack after the last rep.",
            ),
            cues = listOf(UPRIGHT, "Hold the bar on both sides the whole time.", "Front knee points the same way as your foot."),
            mistakes = listOf(FRONT_KNEE_CAVES, LEANING, KNEE_HITS),
        ),
        listOf(Sources.EXRX_BB_LUNGE, Sources.ACE_LUNGE_VARIATIONS, Sources.ACE_FORWARD_LUNGE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Lateral lunge", listOf("Side lunge", "DB side lunge", "DB lateral lunge"), listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), LEG_MUSCLES,
        FormGuide(
            purpose = "A lunge out to the side. It trains your quads, glutes and inner thighs.",
            setup = listOf(
                "Stand with your feet hip-width apart. Hold a dumbbell in each hand, palms facing each other, or use no weight.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Step straight out to one side. Land on your heel, then your forefoot.",
                "Push your hips back and bend the knee of the leg you stepped onto. Keep that shin close to upright and reach the weights toward that foot.",
                "Press that foot into the floor to push back to the middle.",
                "Switch sides each rep.",
            ),
            cues = listOf("Chest up.", "Knee points the same way as your foot."),
            mistakes = listOf(KNEES_CAVE, LEANING),
        ),
        listOf(Sources.EXRX_DB_SIDE_LUNGE, Sources.ACE_LATERAL_LUNGE, Sources.ACE_TOTAL_BODY_DB_WORKOUT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Curtsy lunge", emptyList(), listOf(Equipment.BODYWEIGHT, Equipment.DUMBBELL), listOf("Glutes", "Quads"),
        FormGuide(
            purpose = "A lunge where your back leg crosses behind your front leg, like a curtsy. It trains your glutes and quads.",
            setup = listOf(
                "Stand with your feet wide apart.",
                "Keep your chest up.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Step one leg back and behind the other, like a curtsy.",
                // No allowed non-blog page gives a curtsy-lunge depth, so the knee position is the limit.
                "Bend both knees to lower yourself, keeping your front knee over your heel.",
                "Push up through your front leg to return to the wide stance.",
                "Do all your reps, then switch sides.",
            ),
            cues = listOf("Front knee stays over your heel.", "Chest up."),
            mistakes = listOf(
                "Letting your front knee drift off its line over your heel as you lower.",
                "Leaning your chest forward.",
            ),
        ),
        listOf(Sources.ACE_BUTT_TONING_MOVES, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Split squats and step-ups ----
    quadEntry(
        "Split squat", listOf("DB split squat", "Dumbbell split squat", "Static lunge"), listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), SQUAT_MUSCLES,
        FormGuide(
            purpose = "A squat in a split stance, with both feet on the floor. It trains your quads and glutes one leg at a time.",
            setup = listOf(
                DBS_AT_SIDES + " Or use no weight.",
                "Stand with your feet far apart, one in front and one behind.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Bend your front knee and hip to lower yourself. Let your back heel come up.",
                "Go down until your back knee is almost touching the floor.",
                "Stand back up by straightening your front leg.",
                SWITCH_LEGS,
            ),
            cues = listOf(UPRIGHT, KNEES_TRACK),
            mistakes = listOf(FRONT_KNEE_CAVES, LEANING, KNEE_HITS),
        ),
        listOf(Sources.EXRX_DB_SPLIT_SQUAT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Step-up", listOf("DB step-up", "Dumbbell step-up"), listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), SQUAT_MUSCLES,
        FormGuide(
            purpose = "Stepping up onto a box. It trains your quads and glutes one leg at a time.",
            setup = listOf(
                BOX_HEIGHT,
                "Stand facing it with your feet about hip-width apart. Hold a dumbbell in each hand, palms facing in, or use no weight.",
                "Pull your shoulder blades down and back. Don't shrug.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Place one foot on the box.",
                "Stand up on the box by straightening that leg, then bring your other foot up.",
                "Step down with your other leg first, then bring your lead foot down.",
                "Switch lead legs each rep.",
            ),
            cues = listOf("Lead knee over your second toe.", UPRIGHT),
            mistakes = listOf("Letting your knee or ankle collapse in or out.", "Using a box higher than your knee."),
        ),
        listOf(Sources.EXRX_DB_STEP_UP, Sources.ACE_STEP_UP, Sources.ACE_SQUAT_ALTERNATIVES, Sources.ACE_BRACE_OR_BREATHE),
    ),
    quadEntry(
        "Lateral step-up", listOf("Side step-up", "DB lateral step-up"), listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), SQUAT_MUSCLES,
        FormGuide(
            purpose = "Stepping up onto a box beside you. It trains your quads and glutes one leg at a time.",
            setup = listOf(
                // The knee-height limit is ACE's for the forward step-up, carried over.
                BOX_HEIGHT,
                "Stand side-on to it. Hold a dumbbell in each hand at your sides, or use no weight.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Lift the leg nearest the box and place that foot on it, a little forward of your standing knee.",
                "Stand up on the box by straightening that leg.",
                "Step back down to the start.",
                "Do all your reps, then switch sides.",
            ),
            cues = listOf(UPRIGHT, "Knee points the same way as your foot."),
            mistakes = listOf(KNEES_CAVE, LEANING),
        ),
        listOf(Sources.EXRX_DB_LATERAL_STEP_UP, Sources.ACE_SQUAT_ALTERNATIVES, Sources.ACE_BRACE_OR_BREATHE),
    ),
)
