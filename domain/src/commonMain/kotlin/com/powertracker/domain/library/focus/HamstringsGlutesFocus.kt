package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.Sources

// Each line is worked out from the exercise's own guide (areas/HamstringsGlutes.kt): the stretch where the guide's
// lowering or return ends, never further; the squeeze where its lifting step ends. Hinges follow the RDL pilot: the
// hamstrings stretch as the hips go back, the glutes squeeze as you stand. Their stretch names the guide's own stop
// (HINGE_DEPTH: the weight about at the knees, or sooner if the back rounds), never the floor or "as far as you can".
// Curls stretch the hamstrings with the knee straight at the guide's start and squeeze them at the end of the curl.
// Bridges and thrusts stretch the glutes at the guide's bottom and squeeze them at its top, with the guide's own guard
// against arching (abs braced, chest down), never "as high as you can". Single-leg work names the working leg.
// The guide's pages come first, then the idea's sources (added by [lines] and [hamstringLines]). A page that asks for
// more range than the guide's step or mistakes allow, at the point the line names, is not cited for that line
// (controller ruling, 2026-11-16).

/**
 * Two lines that rest on the guide alone (owner decision 3), for an exercise whose only allowed page asks for more
 * range than the guide at the point a line names, and no other page stops where the guide does. Only the idea's
 * sources are listed.
 */
private fun guideOnlyLines(stretch: String, squeeze: String): MuscleFocus.Lines =
    MuscleFocus.Lines(stretch, squeeze, listOf(Sources.PMC_LENGTHENED_PARTIALS, Sources.ACE_MUSCLES_IN_MOTION))

// The RDL pilot's lines. ExRx RomanianDeadlift ("until hamstring is stretched fully just before lowering bar to
// completely to floor") lowers past the guide's knees, so ACE 317 (lower "until the hamstrings feel tension", no
// height) is cited instead. ACE ProSource allows mid-shin, and NSCA goes just below the knees: neither is cited.
private const val HIPS_GO_BACK = "Your hamstrings stretch as your hips go back."
private const val STAND_TALL = "Squeeze your glutes as you stand tall."
private const val HIPS_STRAIGHTEN_STAND = "Squeeze your glutes as your hips straighten to stand."
private const val HIPS_LOWER = "Your glutes stretch as your hips lower under control."
// ACE 49: pushing the hips as high as possible is what arches the low back; the abs stay braced.
private const val HIPS_RISE_BRACED = "Squeeze your glutes as your hips rise, abs still braced."
// ExRx BBHipThrust: raise "until straight"; avoid the chest arching up (the guide's cue "Keep your chest down").
private const val BODY_STRAIGHT_CHEST_DOWN = "Squeeze your glutes as your body straightens, chest down."
private const val SWING_HINGE = "Your hamstrings stretch as you hinge to swing the bell back."

/** BodyArea.HAMSTRINGS_GLUTES: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val hamstringsGlutesFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf<Pair<String, MuscleFocus>>(
        // Glutes first in its list. The guide lowers "under control" and names no depth, so the stretch names none.
        "Hip thrust" to lines(
            "Your glutes stretch as you lower your hips under control.", BODY_STRAIGHT_CHEST_DOWN,
            Sources.EXRX_HIP_THRUST, Sources.ACE_HIP_THRUST,
        ),
        // The pilot, re-checked: see HIPS_GO_BACK.
        "RDL" to hamstringLines(HIPS_GO_BACK, STAND_TALL, Sources.ACE_RDL),

        // ---- Hinges ----
        // The guide shares HINGE_DEPTH with the RDL. Its ExRx pages lower the bar to the top of the feet (one lets the
        // lower back bend) and NSCA goes below the knees, so none is cited; ACE 317, the same hinge stopped at
        // tension, is cited off the guide's list. "Back flat" keeps the guide's limit beside the knee landmark.
        "Stiff-leg deadlift" to hamstringLines(
            "Your hamstrings stretch as the bar nears your knees, back flat.", HIPS_STRAIGHTEN_STAND,
            Sources.ACE_RDL,
        ),
        // The guide's first step is the pilot's: "Push your hips back". ACE ProSource (first on the guide's list)
        // allows mid-shin, so ACE 317 is cited.
        "DB RDL" to hamstringLines(
            HIPS_GO_BACK, "Squeeze your glutes as you drive your hips forward to stand.",
            Sources.ACE_RDL,
        ),
        // Glutes first in its list; the stretch names the hamstrings, second, as the hinge pilot does. The guide
        // lowers to the floor or sooner; the line names no depth. ExRx DBSingleLegStiffLegDeadlift: stop once the
        // stretch is felt or the weight touches the floor. The squeeze is the guide's "squeezing the glute of your
        // standing leg".
        "Single-leg RDL" to hamstringLines(
            "Your hamstrings stretch in your standing leg as you hinge down.",
            "Squeeze your glutes as your standing leg drives you back up.",
            Sources.EXRX_DB_SL_STIFF_LEG_DL, Sources.ACE_SL_RDL,
        ),
        // The guide: torso level with the floor, no further, or sooner if the back rounds. Both ExRx pages stop at
        // parallel (the straight-knee one at a mild stretch, if sooner).
        "Good morning" to hamstringLines(
            "Your hamstrings stretch as your torso nears level, back flat.", HIPS_STRAIGHTEN_STAND,
            Sources.EXRX_BB_BENT_KNEE_GOOD_MORNING, Sources.EXRX_BB_GOOD_MORNING,
        ),

        // ---- Leg curls ----
        // The stretch is the guide's "until your knees are straight", with its mistake (hips lifting) kept off.
        "Lying leg curl" to hamstringLines(
            "Your hamstrings stretch with your knees straight, hips down.",
            "Squeeze your hamstrings as the pad comes up toward your thighs.",
            Sources.EXRX_LV_LYING_LEG_CURL, Sources.ACE_LYING_HAMSTRINGS_CURL,
        ),
        "Seated leg curl" to hamstringLines(
            "Your hamstrings stretch with your legs straight in front of you.",
            "Squeeze your hamstrings as you pull the pad back under you.",
            Sources.EXRX_LV_SEATED_LEG_CURL,
        ),
        // The guide keeps the lifter bent forward at the hips, which its mistake says lets the hamstrings work
        // through more of the curl; the stretch keeps it.
        "Standing leg curl" to hamstringLines(
            "Your hamstrings stretch as your knee straightens, hips bent.",
            "Squeeze your hamstrings as the pad rises toward your thigh.",
            Sources.EXRX_LV_STANDING_LEG_CURL,
        ),
        // ExRx CBStandingLegCurl: curl "until knee is fully flexed"; keep the hip from sagging or being pulled forward.
        "Cable leg curl" to hamstringLines(
            "Your hamstrings stretch as your knee straightens, hips still.",
            "Squeeze your hamstrings as your knee bends all the way.",
            Sources.EXRX_CB_STANDING_LEG_CURL,
        ),
        // Glutes first in its list, but the curl is the hamstrings' work, and they go from long (the ball rolled
        // out, hips still up) to short (heels pulled in). The hip lift is a setup the guide holds, not the rep.
        "Stability ball leg curl" to hamstringLines(
            "Your hamstrings stretch as you roll the ball back out, hips up.",
            "Squeeze your hamstrings as you pull your heels toward your hips.",
            Sources.ACE_STABILITY_BALL_HAMSTRING_CURL,
        ),
        // The stretch is only the controlled part of the lowering (the guide's cue "Hamstrings control the way
        // down"), not the hands' part. ExRx: "Control descent only with hamstrings as low as possible."
        "Nordic curl" to hamstringLines(
            "Your hamstrings stretch while they control the way down.",
            "Squeeze your hamstrings as they pull you back up to kneeling.",
            Sources.EXRX_SELF_ASSISTED_INVERSE_LEG_CURL_FLOOR,
        ),
        // Glutes first in its list, but its guide says the knees do the work (it works the hamstrings). ExRx
        // BWHamstringRaise: lower "until horizontal by straightening knees", the guide's "level, no further".
        "Glute-ham raise" to hamstringLines(
            "Your hamstrings stretch as your body lowers to level, no lower.",
            "Squeeze your hamstrings as your knees bend to pull you back up.",
            Sources.EXRX_INVERSE_LEG_CURL,
        ),

        // ---- Bridges and thrusts ----
        "Glute bridge" to lines(HIPS_LOWER, HIPS_RISE_BRACED, Sources.ACE_GLUTE_BRIDGE),
        "Single-leg glute bridge" to lines(
            HIPS_LOWER, "Squeeze your glutes as you press through your planted foot.",
            Sources.ACE_SINGLE_LEG_GLUTE_BRIDGE, Sources.EXRX_SINGLE_LEG_HIP_BRIDGE,
        ),
        // ACE 318: "slowly lower the tailbone back to the floor".
        "Barbell glute bridge" to lines(
            "Your glutes stretch as you slowly lower your tailbone.", HIPS_RISE_BRACED,
            Sources.ACE_HIP_BRIDGE, Sources.ACE_GLUTE_BRIDGE,
        ),
        // ACE 367: lower the tailbone toward the floor; press the feet and squeeze the glutes to return.
        "DB hip thrust" to lines(
            "Your glutes stretch as your tailbone lowers toward the floor.", BODY_STRAIGHT_CHEST_DOWN,
            Sources.ACE_ELEVATED_GLUTE_BRIDGE, Sources.EXRX_HIP_THRUST,
        ),
        // ExRx LVHipThrustPL: extend the hips until straight, with a load that allows full hip extension.
        "Machine hip thrust" to lines(
            HIPS_LOWER, "Squeeze your glutes as your hips straighten, chest down.",
            Sources.EXRX_LV_HIP_THRUST, Sources.EXRX_HIP_THRUST,
        ),

        // ---- Swings ----
        // Fast, but the guide hinges the bell back (hamstrings long) and drives the hips forward to stand (glutes
        // short), as the jump squat and clap push-up get two lines. No pause in the squeeze: the bell keeps moving.
        "Kettlebell swing" to hamstringLines(
            SWING_HINGE, "Squeeze your glutes as you drive your hips forward.",
            Sources.ACE_KB_SWING, Sources.EXRX_KB_TWO_ARM_SWING,
        ),
        "Single-arm kettlebell swing" to hamstringLines(
            SWING_HINGE, "Squeeze your glutes as you push your hips forward.",
            Sources.ACE_KB_SINGLE_ARM_SWING, Sources.ACE_KB_SWING_TECHNIQUE,
        ),

        // ---- Hip extensions ----
        // ExRx BWReverseHyperextension raises the legs "as high as possible until legs are nearly straight" and lets
        // some lifters move through the lower back too: more than the guide's "nearly in line with your body", at
        // the point the squeeze names. No other allowed page covers it, so the lines rest on the guide alone.
        "Reverse hyper" to guideOnlyLines(
            "Your glutes stretch as your legs lower back to the start.",
            "Squeeze your glutes as your legs come up nearly in line.",
        ),
        // ACE 270: lift the leg only as high as the low back stays controlled; the guide's "Move only at the hip".
        "Quadruped hip extension" to lines(
            "Your glutes stretch as your knee lowers back under your hip.",
            "Squeeze your glutes as your leg lifts, moving only at the hip.",
            Sources.ACE_QUADRUPED_HIP_EXTENSION,
        ),

        // ---- Abduction and adduction ----
        // "Glutes" is the catalogue's word for the outer hip muscles here (ABDUCTION_MUSCLES). Each line keeps to the
        // guide's own range: its start, and its lift with its own stop (hips tilting, pelvis moving).
        // ExRx LVSeatedHipAbduction moves the legs apart "as far as possible", where the guide names no end, in the
        // movement the squeeze names (the cable external rotation precedent), and no other allowed page gives the
        // seated machine's range. So the lines rest on the guide alone: the pads together at the start, then apart.
        "Machine hip abduction" to guideOnlyLines(
            "Your glutes stretch with your legs together at the start.",
            "Squeeze your glutes as you push your legs apart.",
        ),
        // The guide's start crosses the cuffed leg in front (ExRx CBHipAbduction); its stop is ACE 38's hips tilting.
        "Cable hip abduction" to lines(
            "Your glutes stretch with your cuffed leg crossed in front.",
            "Squeeze your glutes as your leg lifts out, hips still level.",
            Sources.EXRX_CB_HIP_ABDUCTION, Sources.ACE_SIDE_LYING_HIP_ABDUCTION,
        ),
        // ACE 38: raise "until the hips begin to tilt upwards"; the guide keeps the hips stacked.
        "Side-lying hip abduction" to lines(
            "Your glutes stretch as you lower your top leg slowly.",
            "Squeeze your glutes as your top leg lifts, hips still stacked.",
            Sources.ACE_SIDE_LYING_HIP_ABDUCTION,
        ),
        // The guide stops "before your pelvis moves or your hips roll back"; ACE: a small move, easily overdone.
        "Clamshell" to lines(
            "Your glutes stretch as your top knee lowers to the other.",
            "Squeeze your glutes as your top knee lifts, pelvis still.",
            Sources.ACE_GLUTE_MEDIUS_EXERCISES, Sources.ACE_JOINT_FRIENDLY_LOWER_BODY,
        ),
        // Not a carry or a hold: each step takes a leg out against the band and the feet come back in, so the outer
        // hip goes from short to longer, with the guide's band kept tight. A small stretch, but not a false one
        // (the quads review's lateral lunge ruling).
        "Lateral band walk" to lines(
            "Your glutes stretch as your feet come back in, band still tight.",
            "Squeeze your glutes as you step out to the side.",
            Sources.ACE_JOINT_FRIENDLY_LOWER_BODY,
        ),
        // Adductors is its only muscle. ExRx LVSeatedHipAdduction sets the start width "until slight stretch is
        // felt", the guide's "starting width"; the guide's cue: "Legs come all the way together."
        "Machine hip adduction" to lines(
            "Your adductors stretch with your legs out at the starting width.",
            "Squeeze your adductors as your legs come all the way together.",
            Sources.EXRX_LV_SEATED_HIP_ADDUCTION,
        ),
    )
}
