package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

// Shared lines, so the core wording is the same in every guide.
private const val BRACE_TRUNK = "Brace your trunk before each rep."
private const val LIGHT_BRACE = "Brace your trunk lightly, and keep breathing."
// Carries. NSCA loaded carries: keep the torso upright, walk slow and controlled, and on one-sided carries avoid
// any sideways bend of the spine. ACE 5616 asks for short, rapid steps; the slower NSCA rule is used for every carry.
// ACE 7099 braces the core for its carries.
private const val CARRY_BRACE = "Brace your trunk."
private const val WALK_SLOW = "Walk slowly and under control."
private const val CARRY_UPRIGHT = "Torso upright."
private const val RUSHING = "Rushing the walk, so you lose control of the weight."
private const val SIDE_BEND = "Bending sideways at your waist, toward or away from the weight."
private const val SWITCH_SIDES = "Do the same on the other side."

private fun coreEntry(
    name: String,
    aliases: List<String>,
    equipment: List<Equipment>,
    muscles: List<String>,
    guide: FormGuide,
    sources: List<SourceId>,
    kind: Kind = Kind.ACCESSORY,
) = LibraryExercise(
    name, aliases, BodyArea.CORE, equipment, muscles, kind, Category.ACCESSORY,
    parentLift = null, guide = guide, sources = sources,
)

/** BodyArea.CORE entries. No hold times or carry distances: no allowed source gives one that more than one agrees on. */
internal val core: List<LibraryExercise> = listOf(
    // ---- Planks ----
    // No "Plank" alias: it fits both planks.
    coreEntry(
        "Front plank", listOf("Forearm plank", "Elbow plank", "Prone plank"),
        listOf(Equipment.BODYWEIGHT), listOf("Abs", "Obliques"),
        FormGuide(
            purpose = "A hold on your forearms and toes with your body in a straight line. It trains your abs to keep your trunk still.",
            setup = listOf(
                "Lie face down with your elbows close to your sides and right under your shoulders, palms down.",
                "Tighten your thighs to straighten your legs, and pull your toes toward your shins.",
                "Brace your trunk to stiffen it, and keep breathing.",
            ),
            steps = listOf(
                "Lift your body off the floor onto your forearms and toes, keeping your trunk and legs stiff.",
                "Hold your body in a straight line.",
            ),
            cues = listOf("Shoulders over elbows.", "Body in a straight line."),
            mistakes = listOf(
                "Letting your lower back sag.",
                "Lifting your hips up.",
                "Bending your knees.",
                "Shrugging your shoulders up toward your ears.",
            ),
        ),
        listOf(Sources.ACE_FRONT_PLANK, Sources.EXRX_FRONT_PLANK, Sources.ACE_BRACE_OR_BREATHE),
    ),
    coreEntry(
        "Side plank", listOf("Side bridge", "Forearm side plank"),
        listOf(Equipment.BODYWEIGHT), listOf("Obliques", "Abs"),
        FormGuide(
            purpose = "A hold on one forearm and the side of one foot, with your body in a straight line. It mainly trains your obliques.",
            setup = listOf(
                "Lie on your side with your legs straight and your feet stacked.",
                "Prop yourself up on your forearm, with your elbow right under your shoulder.",
                "Keep your head in line with your spine.",
                "Brace your trunk, and keep breathing.",
            ),
            steps = listOf(
                "Lift your hips and knees off the floor, keeping the side of your bottom foot on the floor.",
                "Hold your body in a straight line from head to heels.",
                "Lower your hips back to the floor. Do all your holds on one side, then switch.",
            ),
            cues = listOf("Elbow under shoulder.", "Squeeze your belly and glutes."),
            // Direct negations of ACE 303 ("straight line from head to heels") and ACE 101 ("head in line with spine").
            mistakes = listOf(
                "Not keeping your body in a straight line from head to heels.",
                "Not keeping your head in line with your spine.",
            ),
        ),
        listOf(Sources.ACE_SIDE_PLANK_STRAIGHT_LEG, Sources.ACE_SIDE_PLANK, Sources.EXRX_SIDE_PLANK, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Carries ----
    coreEntry(
        "Farmer's walk", listOf("Farmer carry", "Farmer's carry", "Farmers walk", "Farmer walk"),
        listOf(Equipment.DUMBBELL, Equipment.KETTLEBELL), listOf("Grip", "Abs"),
        FormGuide(
            purpose = "Walking while holding a heavy weight in each hand. It builds your grip and core strength.",
            setup = listOf(
                "Stand between two dumbbells or kettlebells.",
                CARRY_BRACE,
                "Grip the handles firmly and lift the weights by driving through your heels, keeping your back straight.",
                "Stand tall with your arms straight at your sides.",
            ),
            steps = listOf(
                "Walk a set distance, keeping your back upright and your head up.",
                WALK_SLOW,
                "Turn around and walk back, then set the weights down the same way you picked them up.",
            ),
            cues = listOf("Back upright, head up.", "Slow, steady steps."),
            mistakes = listOf(
                "Rounding your back when you pick up or set down the weights.",
                "Leaning forward instead of standing tall.",
                RUSHING,
            ),
        ),
        listOf(
            Sources.ACE_FARMERS_CARRY, Sources.ACE_FUNCTIONAL_SC_EXERCISES, Sources.ACE_STRONGMAN_EXERCISES,
            Sources.NSCA_LOADED_CARRIES,
        ),
        Kind.COMPOUND,
    ),
    coreEntry(
        "Suitcase carry", listOf("One-arm farmer's walk", "Single-arm farmer's walk", "Unilateral farmer's walk", "Suitcase walk"),
        listOf(Equipment.DUMBBELL, Equipment.KETTLEBELL), listOf("Obliques", "Grip", "Abs"),
        FormGuide(
            purpose = "Walking with a weight in one hand only. The muscles at the side of your trunk work to keep you from bending toward the weight.",
            setup = listOf(
                "Stand beside a dumbbell or kettlebell.",
                CARRY_BRACE,
                "Lift it in one hand with an overhand grip, keeping your back straight.",
                "Let the arm hang straight, without the weight touching your body.",
            ),
            steps = listOf(
                "Walk a set distance, keeping your torso upright.",
                WALK_SLOW,
                "Turn around and walk back. $SWITCH_SIDES",
            ),
            cues = listOf("Stay upright; don't lean either way.", "Slow, steady steps."),
            mistakes = listOf(SIDE_BEND, RUSHING),
        ),
        listOf(Sources.ACE_SUITCASE_CARRY, Sources.NSCA_LOADED_CARRIES, Sources.NSCA_CORE_TRAINING_CONCEPTS, Sources.ACE_STRONGMAN_EXERCISES),
        Kind.COMPOUND,
    ),
    coreEntry(
        "Waiter's carry", listOf("Waiter's walk", "Waiters walk", "Overhead carry", "Single-arm overhead carry", "Kettlebell overhead carry"),
        listOf(Equipment.KETTLEBELL), listOf("Obliques", "Abs"),
        FormGuide(
            purpose = "Walking with a kettlebell held overhead in one hand. It trains your trunk and the muscles that steady your shoulder.",
            setup = listOf(
                "Hold a kettlebell in one hand at the front of your shoulder.",
                CARRY_BRACE,
                "Press it overhead until your arm is straight, palm facing forward.",
            ),
            steps = listOf(
                "Walk a set distance, keeping your trunk, arm and the kettlebell in one vertical line.",
                WALK_SLOW,
                SWITCH_SIDES,
            ),
            cues = listOf("Arm straight, weight over your shoulder.", CARRY_UPRIGHT),
            mistakes = listOf(
                "Letting the kettlebell drift out of line with your shoulder and trunk.",
                SIDE_BEND,
                RUSHING,
            ),
        ),
        listOf(Sources.NSCA_CORE_TRAINING_CONCEPTS, Sources.NSCA_LOADED_CARRIES, Sources.ACE_STRONGMAN_EXERCISES),
        Kind.COMPOUND,
    ),
    // Weight plates are barbell kit, so BARBELL is the equipment filter that finds it. The mistakes are NSCA's general
    // carry rules, which its loaded-carries article gives for every carry.
    coreEntry(
        "Plate pinch carry", listOf("Pinch carry", "Plate pinch walk", "Pinch-grip farmer's walk"),
        listOf(Equipment.BARBELL), listOf("Grip"),
        FormGuide(
            purpose = "Walking while pinching weight plates by their edges. It trains your grip.",
            setup = listOf(
                "Pinch a weight plate by its edge in each hand, thumb on one side and fingers on the other.",
                "Squeeze the plates as hard as you can.",
                CARRY_BRACE,
            ),
            steps = listOf(
                "Walk a set distance, standing tall.",
                WALK_SLOW,
                "Turn around and walk back, then set the plates down.",
            ),
            cues = listOf("Squeeze hard the whole way.", CARRY_UPRIGHT),
            mistakes = listOf("Leaning forward instead of standing tall.", RUSHING),
        ),
        listOf(Sources.ACE_GRIP_STRENGTH, Sources.NSCA_LOADED_CARRIES, Sources.ACE_STRONGMAN_EXERCISES),
        Kind.COMPOUND,
    ),

    // ---- Anti-rotation and rotation ----
    coreEntry(
        "Pallof press", listOf("Pallof", "Anti-rotation press", "Cable anti-rotation press", "Standing anti-rotation press", "Cable Pallof press"),
        listOf(Equipment.CABLE), listOf("Obliques", "Abs"),
        FormGuide(
            purpose = "Pressing a cable handle straight out from your chest while the cable pulls you to the side. It trains your trunk to resist twisting.",
            setup = listOf(
                "Set a single handle on a cable at about chest height.",
                "Stand side-on to the machine, feet hip-width apart and knees slightly bent.",
                "Hold the handle with both hands at your chest, fingers laced and elbows tucked in by your sides.",
                "Brace your trunk and stand tall.",
            ),
            steps = listOf(
                "Press the handle straight out in front of your chest. The cable should run level with your chest.",
                "Hold briefly with your arms straight.",
                "Slowly bring your elbows back to your sides.",
                "Do all your reps on one side, then turn around.",
            ),
            cues = listOf("Chest and hips face forward.", "Back straight and tall."),
            mistakes = listOf(
                "Letting the cable turn your torso toward the machine.",
                "Bringing your elbows back fast instead of slowly.",
            ),
        ),
        listOf(Sources.ACE_STANDING_ANTI_ROTATION_PRESS, Sources.ACE_ANTI_ROTATION_PROGRESSION, Sources.ACE_BRACE_OR_BREATHE),
    ),
    coreEntry(
        "Cable woodchop", listOf("Woodchop", "Wood chop", "Cable chop", "Cable wood chop", "Standing rotational chop", "High-to-low chop"),
        listOf(Equipment.CABLE), listOf("Obliques", "Abs"),
        FormGuide(
            purpose = "Sweeping a cable handle from above one shoulder down to the opposite hip. It trains your trunk and hips to turn together.",
            setup = listOf(
                "Set a single handle on a cable at shoulder height.",
                "Stand side-on to the machine, feet wider than shoulder-width.",
                "Hold the handle with both hands, fingers laced and arms straight.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Push off the foot nearer the machine to shift your weight to your other leg.",
                "At the same time, sweep the handle from above your near shoulder down toward your far hip.",
                "Return the same way, under control, pushing off your far foot.",
                "Do all your reps on one side, then switch.",
            ),
            cues = listOf("Arms straight.", "Chest and belly turn together; back straight."),
            mistakes = listOf(
                "Bending your arms to pull the handle.",
                "Twisting with your shoulders only, instead of turning your chest and belly together.",
                "Rounding your back as you turn.",
            ),
        ),
        listOf(Sources.ACE_STANDING_ROTATIONAL_CHOP, Sources.ACE_ANTI_ROTATION_PROGRESSION),
    ),

    // ---- Rollouts ----
    // Stop: ExRx rolls out "as far as you can"; that pushes to the end of range. No ab wheel page gives a positional
    // stop, so the ab wheel's stop is carried over from two related ACE pages: the stability ball kneeling plank
    // roll-out (stop before the hips sag) and the TRX front rollout (don't let the back arch or sag).
    coreEntry(
        "Ab wheel rollout", listOf("Ab wheel", "Wheel rollout", "Ab roller", "Ab roller rollout", "Kneeling wheel rollout"),
        listOf(Equipment.BODYWEIGHT), listOf("Abs"),
        FormGuide(
            purpose = "Rolling an ab wheel forward from your knees and back again. It trains your abs to keep your trunk from sagging.",
            setup = listOf(
                "Kneel on the floor and hold the wheel's handles.",
                "Put the wheel on the floor near the front of your knees, arms straight.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Roll the wheel forward, lowering your body toward the floor.",
                "Stop before your hips start to sag or your lower back starts to arch.",
                "Pull the wheel back to your knees by bending at your hips.",
            ),
            cues = listOf("Elbows straight or nearly straight.", "Trunk stiff."),
            mistakes = listOf("Letting your lower back sag or arch.", "Bending your elbows."),
        ),
        listOf(Sources.EXRX_WHEEL_ROLLOUT, Sources.ACE_STABILITY_BALL_CORE, Sources.ACE_TRX_FRONT_ROLLOUT),
    ),
    coreEntry(
        "Stability ball rollout", listOf("Swiss ball rollout", "Ball rollout", "Stability ball roll-out", "Kneeling plank rollout"),
        listOf(Equipment.BODYWEIGHT), listOf("Abs"),
        FormGuide(
            purpose = "Rolling a stability ball forward on your forearms from a kneeling plank. It trains your abs to keep your trunk still.",
            setup = listOf(
                "Kneel facing a stability ball.",
                "Rest your forearms on the ball, with your shoulders right above your elbows.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Slowly push the ball forward by straightening your arms.",
                "Stop before your hips start to sag.",
                "Roll the ball back to the start.",
            ),
            cues = listOf("Move slowly.", "Hips in line with your body."),
            mistakes = listOf("Letting your hips sag.", "Rolling the ball out fast."),
        ),
        listOf(Sources.ACE_STABILITY_BALL_CORE),
    ),

    // ---- Crunches ----
    // No "Crunch" alias: it fits every crunch here.
    coreEntry(
        "Floor crunch", listOf("Bodyweight crunch", "Basic crunch"),
        listOf(Equipment.BODYWEIGHT), listOf("Abs"),
        FormGuide(
            purpose = "Curling your upper back off the floor. It works your abs.",
            setup = listOf(
                "Lie on your back with your knees bent and your feet flat on the floor.",
                "Put your hands behind your head. Squeeze your shoulder blades together and pull your elbows back, without arching your lower back.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Curl your ribs toward your hips until your upper back is off the floor. Let your chin tuck slightly.",
                "Keep your feet, tailbone and lower back on the floor.",
                "Lower slowly back down.",
            ),
            cues = listOf("Ribs toward hips.", "Keep your elbows back the whole set."),
            // ACE 52: keep the elbows back throughout; don't arch the low back.
            mistakes = listOf(
                "Not keeping your elbows back the whole set.",
                "Arching your lower back.",
            ),
        ),
        listOf(Sources.ACE_CRUNCH, Sources.ACE_BRACE_OR_BREATHE),
    ),
    coreEntry(
        "Reverse crunch", listOf("Lying reverse crunch", "Floor reverse crunch"),
        listOf(Equipment.BODYWEIGHT), listOf("Abs", "Obliques"),
        FormGuide(
            purpose = "Rolling your hips up off the floor toward your ribs. It works your abs.",
            setup = listOf(
                "Lie on your back with your feet off the floor and your knees over your hips, bent to about 90 degrees.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Breathe out and roll your hips up off the floor toward your ribs. Keep the same bend in your knees.",
                "Lower your spine back to the floor under control, until your knees are back over your hips.",
            ),
            cues = listOf("Pull your pelvis toward your ribs.", "Knee bend stays the same."),
            mistakes = listOf(
                "Changing the bend in your knees as you roll up.",
                "Letting your knees drop past your hips on the way down.",
            ),
        ),
        listOf(Sources.ACE_REVERSE_CRUNCH),
    ),
    coreEntry(
        "Cable crunch", listOf("Kneeling cable crunch", "Rope crunch", "Cable rope crunch"),
        listOf(Equipment.CABLE), listOf("Abs"),
        FormGuide(
            purpose = "A kneeling crunch pulling a rope down from a high cable. It works your abs.",
            setup = listOf(
                "Set a rope on a high pulley.",
                "Kneel below the pulley and hold the rope with both hands.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Keep your hips still and curl your trunk down, bringing your elbows toward your thighs.",
                "Return to the start under control.",
            ),
            cues = listOf("Bend at your waist, not your hips.", "Hips stay still."),
            mistakes = listOf(
                "Bending at your hips instead of curling your trunk.",
                "Throwing your body weight down so momentum does the work.",
            ),
        ),
        listOf(Sources.EXRX_CB_KNEELING_CRUNCH, Sources.EXRX_ERRORS, Sources.EXRX_MISTAKES, Sources.ACE_BRACE_OR_BREATHE),
    ),
    coreEntry(
        "Machine crunch", listOf("Ab crunch machine", "Seated crunch machine", "Lever seated crunch", "Ab machine"),
        listOf(Equipment.MACHINE), listOf("Abs", "Obliques"),
        FormGuide(
            purpose = "A seated crunch on a machine with a chest pad. It works your abs.",
            setup = listOf(
                "Set the seat so your chest is level with the pad.",
                "Sit with the back of your hips against the hip pad. On some machines, hook your feet behind the pad under the seat.",
                "Put your hands on the outside of the pad.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Keep your hips still and curl your trunk forward and down into a C shape.",
                "Return to the start under control.",
            ),
            cues = listOf("Hips stay still.", "Curl into a C shape."),
            mistakes = listOf(
                "Moving at your hips instead of curling your trunk.",
                "Throwing the pad down so momentum does the work.",
            ),
        ),
        listOf(Sources.EXRX_LV_SEATED_CRUNCH, Sources.EXRX_ERRORS, Sources.EXRX_MISTAKES, Sources.ACE_BRACE_OR_BREATHE),
    ),
    coreEntry(
        "Bicycle crunch", listOf("Bicycle crunches", "Supine bicycle crunch"),
        listOf(Equipment.BODYWEIGHT), listOf("Obliques", "Abs"),
        FormGuide(
            purpose = "A crunch with a twist, bringing each elbow toward the opposite knee in turn. It works your abs and obliques.",
            setup = listOf(
                "Lie on your back with your knees bent, feet flat and your hands behind your head.",
                BRACE_TRUNK,
                "Lift both feet until your thighs are upright, with your knees bent to about 90 degrees.",
            ),
            steps = listOf(
                "Curl your upper body up and turn it so one elbow moves toward the opposite knee, while you straighten your other leg.",
                "Switch sides, bringing the other elbow toward the other knee.",
            ),
            cues = listOf("Lower back pressed to the floor.", "Turn from your trunk, not your hips.", "Slow and controlled."),
            mistakes = listOf(
                "Pulling your head forward with your hands.",
                "Letting your lower back lift off the floor.",
                "Rushing the reps.",
            ),
        ),
        listOf(Sources.ACE_BICYCLE_CRUNCH),
    ),

    // ---- Stability drills ----
    coreEntry(
        "Dead bug", listOf("Deadbug", "Supine dead bug"),
        listOf(Equipment.BODYWEIGHT), listOf("Abs"),
        FormGuide(
            purpose = "Lowering one arm and the opposite leg while lying on your back. It trains your abs to keep your lower back still while your limbs move.",
            setup = listOf(
                "Lie on your back with your knees bent and your feet flat.",
                "Pull your shoulders down and back, without arching your lower back or lifting your hips.",
                LIGHT_BRACE,
                "Lift your arms and legs off the floor: knees over your hips and bent to 90 degrees, elbows over your shoulders.",
            ),
            steps = listOf(
                "Lower one heel and the opposite hand toward the floor. Touch it lightly, without resting.",
                "Return to the start, then switch to the other arm and leg.",
            ),
            cues = listOf("Lower back still.", "Light touches only."),
            mistakes = listOf("Arching your lower back as your arm and leg go down.", "Lifting your hips off the floor."),
        ),
        listOf(Sources.ACE_DEAD_BUG, Sources.ACE_BRACE_OR_BREATHE),
    ),
    coreEntry(
        "Bird dog", listOf("Birddog", "Quadruped arm and leg raise"),
        listOf(Equipment.BODYWEIGHT), listOf("Abs", "Lower back"),
        FormGuide(
            purpose = "Raising one arm and the opposite leg from your hands and knees. It trains your trunk to stay still while your limbs move.",
            setup = listOf(
                "Get on your hands and knees, knees and feet hip-width apart.",
                "Brace your trunk to keep your back in its natural line, not sagging or arched.",
            ),
            steps = listOf(
                "Raise one leg and the opposite arm toward level with the floor. Raise them only as high as you can keep your lower back still.",
                "Gently lower them back to the start, then switch sides.",
            ),
            cues = listOf("Hips and shoulders level.", "Lower back still."),
            mistakes = listOf(
                "Letting your hips tilt as your leg lifts.",
                "Raising your arm or leg so high that your lower back arches.",
            ),
        ),
        listOf(Sources.ACE_BIRD_DOG),
    ),
)
