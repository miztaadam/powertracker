package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

// Shared lines, so the calf wording is the same in every guide.
private const val BRACE_TRUNK = "Brace your trunk before each rep."
private const val BALLS_ON_BLOCK = "Put the balls of your feet on the calf block, with your heels hanging off the edge."
private const val RAISE_HEELS = "Raise your heels by pointing your ankles."
// Bottom of the rep. ExRx's calf pages lower "until calves are stretched", which is a sensation limit. ACE's step
// heel raises (expert article 5771) give the positional one, "lower the heels below the edge of the step", so it is
// carried over to every calf raise here, with ACE's "slowly".
private const val LOWER_HEELS = "Slowly lower your heels below the edge of the block."
// ExRx: keep the knees straight, or bend them slightly only during the stretch, or the quads join in.
private const val KNEES_STRAIGHT = "Knees straight, or only slightly bent."
private const val BENDING_KNEES = "Bending your knees to push the weight up, so your thighs do the work."
// ACE 5771: make sure the balls of the feet are securely on top of the step. ExRx SL45CalfPress: reposition if they slip.
private const val FEET_SLIP = "Letting your feet slip on the block. Stop and reset them if they move."
// ACE 51 and 5771 lower slowly; ExRx Errors: momentum takes the load off the muscle.
private const val BOUNCING = "Bouncing out of the bottom, so momentum does the work."
private const val SWITCH_LEGS = "Do all your reps on one leg, then switch."

private val CALF_MUSCLES = listOf("Calves")

private fun calfEntry(
    name: String,
    aliases: List<String>,
    equipment: List<Equipment>,
    guide: FormGuide,
    sources: List<SourceId>,
) = LibraryExercise(
    name, aliases, BodyArea.CALVES, equipment, CALF_MUSCLES, Kind.ACCESSORY, Category.ACCESSORY,
    parentLift = null, guide = guide, sources = sources,
)

/** BodyArea.CALVES entries. */
internal val calves: List<LibraryExercise> = listOf(
    // No "Calf raise" alias anywhere: it fits every entry here.
    calfEntry(
        "Standing calf raise", listOf("Machine calf raise", "Standing calf raise machine", "Lever standing calf raise"),
        listOf(Equipment.MACHINE),
        FormGuide(
            purpose = "A heel raise on a machine with the pads on your shoulders. It works your calves with your knees straight.",
            setup = listOf(
                "Set the shoulder pads so you can stand under them with your legs straight.",
                "Step under the pads and put them on your shoulders.",
                BALLS_ON_BLOCK,
                "Hold the handles and stand up by straightening your hips and knees.",
                BRACE_TRUNK,
            ),
            steps = listOf(RAISE_HEELS, LOWER_HEELS),
            cues = listOf(KNEES_STRAIGHT, "Slow on the way down."),
            mistakes = listOf(BENDING_KNEES, BOUNCING, FEET_SLIP),
        ),
        listOf(
            Sources.EXRX_LV_STANDING_CALF_RAISE, Sources.ACE_STRONGER_CALVES, Sources.EXRX_SL_45_CALF_PRESS,
            Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    calfEntry(
        "Seated calf raise", listOf("Seated calf raise machine", "Lever seated calf raise"),
        listOf(Equipment.MACHINE),
        FormGuide(
            purpose = "A heel raise on a machine with your knees bent and the pads on your thighs. With your knees bent, it mainly works the soleus, the lower calf muscle under the main one.",
            setup = listOf(
                "Sit facing the machine.",
                "Put the balls of your feet on the platform, with your heels hanging off the edge.",
                "Set the pads on your lower thighs.",
                BRACE_TRUNK,
                "Push your heels up a little to lift the pads, then release the support lever. Rest your hands on the pads.",
            ),
            steps = listOf(
                RAISE_HEELS,
                "Slowly lower your heels below the edge of the platform.",
                "After the last rep, put the support lever back before you take your feet off.",
            ),
            cues = listOf("Slow on the way down.", "Balls of your feet stay on the platform."),
            mistakes = listOf(BOUNCING, "Letting your feet slip on the platform. Stop and reset them if they move."),
        ),
        listOf(
            Sources.EXRX_LV_SEATED_CALF_RAISE, Sources.EXRX_LV_SEATED_CALF_RAISE_PL, Sources.ACE_STRONGER_CALVES,
            Sources.EXRX_SL_45_CALF_PRESS, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    // Named so it never takes the leg press's names ("Sled leg press" stays with Leg press in Quads.kt).
    calfEntry(
        "Leg press calf raise", listOf("Calf press", "Leg press calf press", "45-degree calf press"),
        listOf(Equipment.MACHINE),
        FormGuide(
            purpose = "A calf raise done on a 45-degree leg press, pushing the platform with your toes. It works your calves with your legs straight.",
            setup = listOf(
                "Sit with your back against the back pad and hold the side handles.",
                "Put your toes and the balls of your feet on the lower edge of the platform, with your heels and arches hanging off.",
                BRACE_TRUNK,
                "Push the platform up until your legs are straight. Release the lock handles.",
            ),
            steps = listOf(
                "Push the platform away by pointing your ankles.",
                "Slowly let your heels come back below the edge of the platform.",
                // Operational step, carried over from the leg press guide (ExRx single-leg leg press).
                "After the last rep, put the lock handles back on before you get off.",
            ),
            cues = listOf(KNEES_STRAIGHT, "Slow on the way down."),
            mistakes = listOf(
                BENDING_KNEES,
                "Letting your feet slip on the platform. Stop and reset them if they move.",
                BOUNCING,
            ),
        ),
        listOf(
            Sources.EXRX_SL_45_CALF_PRESS, Sources.ACE_STRONGER_CALVES, Sources.EXRX_SL_SINGLE_LEG_PRESS,
            Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    calfEntry(
        "Donkey calf raise", listOf("Machine donkey calf raise", "Lever donkey calf raise"),
        listOf(Equipment.MACHINE),
        FormGuide(
            purpose = "A heel raise bent over at the hips, with the machine pad on your lower back and hips. It works your calves with your knees straight.",
            setup = listOf(
                "Get under the machine with the pad on your lower back and hips.",
                "Rest your forearms on the supports.",
                BALLS_ON_BLOCK,
                "Straighten your knees to lift the pad.",
                BRACE_TRUNK,
            ),
            steps = listOf(RAISE_HEELS, LOWER_HEELS),
            cues = listOf(KNEES_STRAIGHT, "Slow on the way down."),
            mistakes = listOf(BENDING_KNEES, BOUNCING, FEET_SLIP),
        ),
        listOf(
            Sources.EXRX_LV_DONKEY_CALF_RAISE, Sources.ACE_STRONGER_CALVES, Sources.EXRX_SL_45_CALF_PRESS,
            Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    calfEntry(
        "Barbell calf raise", listOf("Barbell standing calf raise", "Standing barbell calf raise"),
        listOf(Equipment.BARBELL),
        FormGuide(
            purpose = "A standing heel raise with a barbell across your upper back. It works your calves with heavy loads.",
            setup = listOf(
                "Set the bar in a power rack at upper-chest height, with a calf block under it.",
                RACK_SAFETY,
                "Step under the bar and rest it across your upper back. Grip it just outside your shoulders.",
                "Put the balls of your feet on the block, heels hanging off, feet about hip-width apart and pointing forward.",
                "Keep your chest up and squeeze your shoulder blades together.",
                BRACE_TRUNK,
                "Lift the bar out of the rack by straightening your hips and knees.",
            ),
            steps = listOf("$RAISE_HEELS Pause at the top.", LOWER_HEELS, RACK_IT),
            cues = listOf(KNEES_STRAIGHT, "Chest up, back straight."),
            mistakes = listOf(BENDING_KNEES, BOUNCING, FEET_SLIP),
        ),
        listOf(
            Sources.EXRX_BB_STANDING_CALF_RAISE, Sources.ACE_BB_CALF_RAISES, Sources.ACE_STRONGER_CALVES,
            Sources.EXRX_SL_45_CALF_PRESS, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    calfEntry(
        "Smith calf raise", listOf("Smith machine calf raise", "Smith standing calf raise"),
        listOf(Equipment.SMITH),
        FormGuide(
            purpose = "A standing heel raise with the Smith machine bar across your upper back. It works your calves with your knees straight.",
            setup = listOf(
                "Set the bar at upper-chest height, with a calf block under it.",
                // Smith stops: ExRx Smith seated calf raise (stops can hold the bar at its lowest point). Operational.
                "Set the safety stops just below your lowest position.",
                "Step under the bar and rest it across your upper back. Hold the bar at the sides.",
                BALLS_ON_BLOCK,
                BRACE_TRUNK,
                "Stand up and unhook the bar by turning it back off the hooks.",
            ),
            steps = listOf(RAISE_HEELS, LOWER_HEELS, "After the last rep, turn the bar forward onto the hooks."),
            cues = listOf(KNEES_STRAIGHT, "Slow on the way down."),
            mistakes = listOf(BENDING_KNEES, BOUNCING, FEET_SLIP),
        ),
        listOf(
            Sources.EXRX_SM_STANDING_CALF_RAISE, Sources.EXRX_SM_SEATED_CALF_RAISE, Sources.ACE_STRONGER_CALVES,
            Sources.EXRX_SL_45_CALF_PRESS, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    calfEntry(
        "Dumbbell calf raise", listOf("DB calf raise", "Dumbbell standing calf raise"),
        listOf(Equipment.DUMBBELL),
        FormGuide(
            purpose = "A standing heel raise on both feet with a dumbbell in one hand. It works your calves with little setup.",
            setup = listOf(
                "Hold a dumbbell at your side in one hand. Hold a support with the other hand for balance.",
                "Put the balls of your feet on a calf block or step, with your heels hanging off the edge.",
                BRACE_TRUNK,
            ),
            steps = listOf(RAISE_HEELS, LOWER_HEELS),
            cues = listOf(KNEES_STRAIGHT, "Use the support for balance only."),
            mistakes = listOf(BENDING_KNEES, BOUNCING, FEET_SLIP),
        ),
        listOf(
            Sources.EXRX_DB_STANDING_CALF_RAISE, Sources.ACE_STRONGER_CALVES, Sources.EXRX_SL_45_CALF_PRESS,
            Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    calfEntry(
        "Single-leg calf raise", listOf("One-leg calf raise", "Single-leg heel raise", "Dumbbell single-leg calf raise"),
        listOf(Equipment.BODYWEIGHT, Equipment.DUMBBELL),
        FormGuide(
            purpose = "A heel raise on one foot on a step. It works each calf on its own.",
            setup = listOf(
                "Stand on a step on one foot, with your heel hanging off the edge. Make sure the ball of your foot is firmly on the step.",
                "Hold a support for balance. To make it harder, hold a dumbbell in the hand on the same side as your working leg.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Start standing on the step on one foot, heel hanging off the edge.",
                "Slowly lower your heel below the edge of the step.",
                "Raise your heel by pointing your ankle, keeping your knee straight.",
                SWITCH_LEGS,
            ),
                    cues = listOf("Knee straight.", "Use the support for balance only."),
            mistakes = listOf(BENDING_KNEES, BOUNCING, "Letting your foot slip on the step. Stop and reset it if it moves."),
        ),
        listOf(
            Sources.ACE_STRONGER_CALVES, Sources.EXRX_DB_SINGLE_LEG_CALF_RAISE, Sources.EXRX_SL_45_CALF_PRESS,
            Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    calfEntry(
        "Seated dumbbell calf raise", listOf("Seated heel raise", "Seated DB calf raise"),
        listOf(Equipment.DUMBBELL),
        FormGuide(
            purpose = "A seated heel raise with a weight across your thighs and your feet on a step. It works your calves with your knees bent, so it needs no machine.",
            setup = listOf(
                "Sit on a bench with both feet on a step and your heels hanging off the edge.",
                "Put a dumbbell or a plate across the tops of your thighs.",
                "Make sure the balls of your feet are firmly on the step.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Slowly lower your heels below the edge of the step.",
                "Raise your heels by pointing your ankles.",
            ),
            cues = listOf("Slow on the way down.", "Balls of your feet stay on the step."),
            mistakes = listOf(BOUNCING, "Letting your feet slip on the step. Stop and reset them if they move."),
        ),
        listOf(Sources.ACE_STRONGER_CALVES, Sources.EXRX_SL_45_CALF_PRESS, Sources.EXRX_ERRORS, Sources.ACE_BRACE_OR_BREATHE),
    ),
)
