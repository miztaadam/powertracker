package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.Sources

// Each line is worked out from the exercise's own guide (areas/Calves.kt, areas/Core.kt): the stretch where the
// guide's lowering or return ends, never further; the squeeze where its lifting step ends. The guide's pages come
// first, then the idea's sources (added by [lines]). A page that asks for more range than the guide's step or
// mistakes allow, at the point the line names, is not cited for that line (controller ruling, 2026-11-16).
//
// Calves follow the standing calf raise pilot. The stretch is the guide's LOWER_HEELS ("below the edge of the
// block", or the guide's own platform or step); the squeeze is RAISE_HEELS at its top. Every calf list is only
// "Calves", so every line names the calves, the seated ones and the single-leg one too (the test takes no "calf"
// or "soleus"). The pilot squeeze, "Squeeze your calves with your heels as high as they go.", is reworded to the
// test's "Squeeze your X as ..." with the same point: ExRx's calf pages raise the heels "as high as possible". It is
// the ankle's range, not the spine's, so F4's "as far as you can on a loaded spine" does not apply.
//
// Core: F5 decides. A guide that holds the trunk still while the body is held, carried, or the limbs move (planks,
// carries, the Pallof press, the dead bug, the bird dog) gets the fixed line. A guide whose steps move the trunk
// (crunches, the chop) or roll the body out and back (rollouts) gets two lines, the stretch only as far as the
// guide's range and the rollouts keeping their guide's low-back stop (F4).

private const val CALVES_SQUEEZE = "Squeeze your calves as your heels rise as high as they go."
private const val CALVES_BELOW_BLOCK = "Your calves stretch as your heels drop below the block."
private const val ABS_RETURN_HIPS_STILL = "Your abs stretch as you return to the start, hips still."

/** BodyArea.CALVES and BodyArea.CORE: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val calvesCoreFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf<Pair<String, MuscleFocus>>(
        // ---- Calves ----
        // The pilot, re-checked. ExRx LVStandingCalfRaise: "Raise heels by extending ankles as high as possible.
        // Lower heels by bending ankles until calves are stretched." ACE 5771 lowers the heels below the step's edge.
        "Standing calf raise" to lines(
            CALVES_BELOW_BLOCK, CALVES_SQUEEZE,
            Sources.EXRX_LV_STANDING_CALF_RAISE, Sources.ACE_STRONGER_CALVES,
        ),
        // The guide's own word is "platform". ExRx LVSeatedCalfRaise: same raise and lower, mainly the soleus.
        "Seated calf raise" to lines(
            "Your calves stretch as your heels drop below the platform.", CALVES_SQUEEZE,
            Sources.EXRX_LV_SEATED_CALF_RAISE, Sources.ACE_STRONGER_CALVES,
        ),
        // The guide pushes the platform away by pointing the ankles; the heels never rise, so the squeeze names the
        // ankles at the end of that push. ExRx SL45CalfPress: push the sled by extending the ankles, heels drop
        // until the calves stretch.
        "Leg press calf raise" to lines(
            "Your calves stretch as your heels come back below the platform.",
            "Squeeze your calves as you point your ankles all the way.",
            Sources.EXRX_SL_45_CALF_PRESS, Sources.ACE_STRONGER_CALVES,
        ),
        "Donkey calf raise" to lines(
            CALVES_BELOW_BLOCK, CALVES_SQUEEZE,
            Sources.EXRX_LV_DONKEY_CALF_RAISE, Sources.ACE_STRONGER_CALVES,
        ),
        // The guide's "Pause at the top" is its own; the squeeze gives no pause length (F3).
        "Barbell calf raise" to lines(
            CALVES_BELOW_BLOCK, CALVES_SQUEEZE,
            Sources.EXRX_BB_STANDING_CALF_RAISE, Sources.ACE_STRONGER_CALVES,
        ),
        "Smith calf raise" to lines(
            CALVES_BELOW_BLOCK, CALVES_SQUEEZE,
            Sources.EXRX_SM_STANDING_CALF_RAISE, Sources.ACE_STRONGER_CALVES,
        ),
        "Dumbbell calf raise" to lines(
            CALVES_BELOW_BLOCK, CALVES_SQUEEZE,
            Sources.EXRX_DB_STANDING_CALF_RAISE, Sources.ACE_STRONGER_CALVES,
        ),
        // One heel on a step. "Calves" because the test takes no "calf"; the heel names the working leg.
        "Single-leg calf raise" to lines(
            "Your calves stretch as your heel drops below the step.",
            "Squeeze your calves as your heel rises as high as it goes.",
            Sources.EXRX_DB_SINGLE_LEG_CALF_RAISE, Sources.ACE_STRONGER_CALVES,
        ),
        // ACE 5771's seated variation: heels below the edge of the step, then up.
        "Seated dumbbell calf raise" to lines(
            "Your calves stretch as your heels drop below the step.", CALVES_SQUEEZE,
            Sources.ACE_STRONGER_CALVES,
        ),

        // ---- Planks ----
        // A hold: the abs keep the trunk still, so there is nothing to stretch or squeeze.
        "Front plank" to MuscleFocus.NoFocus(listOf(Sources.ACE_FRONT_PLANK, Sources.EXRX_FRONT_PLANK)),
        // A hold, like the front plank: the guide holds a straight line, and lowering the hips ends the hold, not a
        // rep. ExRx BWSidePlank: "Muscles are exercised isometrically" (the moving version is the side bridge).
        "Side plank" to MuscleFocus.NoFocus(listOf(Sources.ACE_SIDE_PLANK_STRAIGHT_LEG, Sources.EXRX_SIDE_PLANK)),

        // ---- Carries ----
        // Carries: the guide walks with the trunk upright and still (NSCA: an upright torso, no sideways bend).
        "Farmer's walk" to MuscleFocus.NoFocus(listOf(Sources.ACE_FARMERS_CARRY, Sources.NSCA_LOADED_CARRIES)),
        "Suitcase carry" to MuscleFocus.NoFocus(listOf(Sources.ACE_SUITCASE_CARRY, Sources.NSCA_LOADED_CARRIES)),
        "Waiter's carry" to MuscleFocus.NoFocus(listOf(Sources.NSCA_CORE_TRAINING_CONCEPTS, Sources.NSCA_LOADED_CARRIES)),
        // Grip is its only muscle, and the guide squeezes the plates the whole walk: no long or short point.
        "Plate pinch carry" to MuscleFocus.NoFocus(listOf(Sources.NSCA_LOADED_CARRIES, Sources.ACE_GRIP_STRENGTH)),

        // ---- Anti-rotation and rotation ----
        // Anti-rotation: the arms move, the trunk does not (ACE: core stability without rotating the torso).
        "Pallof press" to MuscleFocus.NoFocus(
            listOf(Sources.ACE_STANDING_ANTI_ROTATION_PRESS, Sources.ACE_ANTI_ROTATION_PROGRESSION),
        ),
        // Not anti-rotation: the guide turns the chest and belly together under load, from above the near shoulder
        // to the far hip, and returns under control. "Trains you to stay steady" would be false for it. The lines
        // name the handle's two ends, not a twist of the spine. ACE 349: the chest and stomach turn together.
        "Cable woodchop" to lines(
            "Your obliques stretch with the handle above your near shoulder.",
            "Squeeze your obliques as the handle sweeps down to your far hip.",
            Sources.ACE_STANDING_ROTATIONAL_CHOP,
        ),

        // ---- Rollouts ----
        // The stretch keeps the guide's stop. ExRx BWKneelingWheelRollout rolls out as far as the shoulders go,
        // lowering the torso to the floor, past the guide's stop, so it is not cited. The two ACE pages the guide's
        // stop comes from are: TRX front rollout (no arching or sagging) and the stability ball kneeling roll-out
        // ("stop if your hips start to sag").
        "Ab wheel rollout" to lines(
            "Your abs stretch as you roll out, stopping before your hips sag.",
            "Squeeze your abs as you pull the wheel back to your knees.",
            Sources.ACE_TRX_FRONT_ROLLOUT, Sources.ACE_STABILITY_BALL_CORE,
        ),
        // The same rollout on forearms, with the same stop (ACE 5123: "stop if your hips start to sag").
        "Stability ball rollout" to lines(
            "Your abs stretch as you push the ball out, before your hips sag.",
            "Squeeze your abs as you roll the ball back to the start.",
            Sources.ACE_STABILITY_BALL_CORE,
        ),

        // ---- Crunches ----
        // The guide's range is the floor and back: the stretch is its "Lower slowly back down", no arching.
        "Floor crunch" to lines(
            "Your abs stretch as you lower slowly back to the floor.",
            "Squeeze your abs as your ribs curl toward your hips.",
            Sources.ACE_CRUNCH,
        ),
        // The stretch stops where the guide does: knees back over the hips, never past (its mistake; ACE 76).
        "Reverse crunch" to lines(
            "Your abs stretch as you lower, knees back over your hips.",
            "Squeeze your abs as your hips roll up toward your ribs.",
            Sources.ACE_REVERSE_CRUNCH,
        ),
        // ExRx CBKneelingCrunch: flex the waist, elbows toward the middle of the thighs, hips still; return. It
        // starts from the kneeling start, not an arched back, so it can be cited.
        "Cable crunch" to lines(
            ABS_RETURN_HIPS_STILL, "Squeeze your abs as your elbows come toward your thighs.",
            Sources.EXRX_CB_KNEELING_CRUNCH,
        ),
        // ExRx LVSeatedCrunchChestPad: hips still, the spine curls into a C shape; return.
        "Machine crunch" to lines(
            ABS_RETURN_HIPS_STILL, "Squeeze your abs as you curl your trunk into a C shape.",
            Sources.EXRX_LV_SEATED_CRUNCH,
        ),
        // Obliques first in its list. The guide has no lowering step: it turns one elbow toward the opposite knee,
        // then switches. Each side's obliques lengthen as the trunk turns back the other way, so the stretch is the
        // switch. ACE 241: rotate from the torso, not the hips; controlled, low back pressed down.
        "Bicycle crunch" to lines(
            "Your obliques stretch as you turn back to switch sides.",
            "Squeeze your obliques as your elbow turns to the opposite knee.",
            Sources.ACE_BICYCLE_CRUNCH,
        ),

        // ---- Stability drills ----
        // The guide's purpose: the abs keep the lower back still while the limbs move (ACE 147: the pelvis and ribs
        // stay still). The trunk muscles do not go from long to short.
        "Dead bug" to MuscleFocus.NoFocus(listOf(Sources.ACE_DEAD_BUG)),
        // The same: the trunk stays still while an arm and a leg lift (ACE 14: the limbs rise only as high as the
        // low back holds its position).
        "Bird dog" to MuscleFocus.NoFocus(listOf(Sources.ACE_BIRD_DOG)),
    )
}
