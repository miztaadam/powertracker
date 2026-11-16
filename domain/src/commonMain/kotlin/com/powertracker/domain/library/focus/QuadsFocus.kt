package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.Sources

// Each line is worked out from the exercise's own guide (areas/Quads.kt): the stretch where the guide's lowering ends,
// never deeper; the squeeze where its standing or pressing step ends. A line names the exercise's first muscle: quads
// for every exercise but the curtsy lunge, which lists glutes first. Single-leg and split work names the working leg.
// The guide's pages come first, then the idea's sources (added by [lines]). A page that asks for more range than the
// guide's step or mistakes allow, at the point the line names, is not cited for that line (controller ruling,
// 2026-11-16).

// "At the bottom": the guide's own depth, with its stop-sooner rule (heels lift, back rounds), never "below parallel".
private const val SQUAT_BOTTOM = "Your quads stretch at the bottom of your squat."
// The guides' "Stand up by straightening your knees and hips."
private const val SQUAT_STAND = "Squeeze your quads as your hips and knees straighten."
// The split guides lower until the back knee is almost touching (or just above) the floor, and never onto it.
private const val FRONT_LEG_LOW = "Your quads stretch in your front leg, back knee near the floor."
private const val FRONT_LEG_STRAIGHTENS = "Squeeze your quads as your front leg straightens."
private const val FRONT_FOOT_STAND = "Squeeze your quads as you push through your front foot to stand."
// The leg press guides lower to about a right angle at the knee and stop sooner if the hips lift off the pad; they
// press until the legs are "straight but not locked".
private const val PRESS_BOTTOM = "Your quads stretch at the bottom, hips still on the pad."

/** BodyArea.QUADS: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val quadsFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf<Pair<String, MuscleFocus>>(
        // ExRx DBSingleLegSplitSquat: rear foot on the bench, lower until the rear knee nearly touches the floor; the
        // guide's "nearly touching", and "Drive up through your front foot".
        "Bulgarian split squat" to lines(
            FRONT_LEG_LOW, "Squeeze your quads as you drive up through your front foot.",
            Sources.EXRX_DB_SINGLE_LEG_SPLIT_SQUAT,
        ),

        // ---- Free-weight and bodyweight squats ----
        // ExRx KBGobletSquat: "just past parallel", the guide's depth. ACE 135 gives the stop-sooner rule. ACE 362
        // (Goblet Squat) lowers "until the hips are below the knees", which may be deeper, so it is not cited.
        "Goblet squat" to lines(SQUAT_BOTTOM, SQUAT_STAND, Sources.EXRX_KB_GOBLET_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT),
        "DB squat" to lines(SQUAT_BOTTOM, SQUAT_STAND, Sources.EXRX_DB_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT),
        // The guide goes to "about level" (ACE 135: thighs roughly parallel, or sooner if the heels lift or the torso
        // rounds). ExRx BWSquat goes "just past parallel", past the guide, so it is not cited.
        "Bodyweight squat" to lines(
            SQUAT_BOTTOM, "Squeeze your quads as you push through your feet to stand.",
            Sources.ACE_BODYWEIGHT_SQUAT,
        ),
        "Kettlebell front squat" to lines(SQUAT_BOTTOM, SQUAT_STAND, Sources.EXRX_KB_FRONT_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT),
        // ExRx: thighs "near parallel"; NSCA: thighs parallel, or only as deep as technique holds.
        "Zercher squat" to lines(SQUAT_BOTTOM, SQUAT_STAND, Sources.EXRX_BB_ZERCHER_SQUAT, Sources.NSCA_ZERCHER_SQUAT),
        // ExRx and the guide: squat on one leg until the buttocks touch the box, then stand until the knee and hip are
        // straight. The box sets the depth.
        "Single-leg box squat" to lines(
            "Your quads stretch as you lower on one leg to touch the box.",
            "Squeeze your quads as your standing leg straightens.",
            Sources.EXRX_SINGLE_LEG_BOX_SQUAT,
        ),
        // A fast squat, but the quads still go from long to short (as with the clap push-up in the chest batch): the
        // stretch is the dip, which stops before the heels lift (ACE 222), and the squeeze is the jump.
        "Jump squat" to lines(
            "Your quads stretch as you dip, stopping before your heels lift.",
            "Squeeze your quads as you jump up.",
            Sources.ACE_SQUAT_JUMP, Sources.ACE_SQUAT_JUMPS,
        ),

        // ---- Smith machine ----
        // ExRx SMSquat and SMFrontSquat: "just past parallel", the guide's depth, which stops sooner if the heels lift.
        "Smith squat" to lines(SQUAT_BOTTOM, SQUAT_STAND, Sources.EXRX_SM_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT),
        "Smith front squat" to lines(SQUAT_BOTTOM, SQUAT_STAND, Sources.EXRX_SM_FRONT_SQUAT, Sources.ACE_BODYWEIGHT_SQUAT),
        // ExRx SMSplitSquat: bend both legs until the rear knee nearly reaches the floor, then straighten both legs.
        "Smith split squat" to lines(
            FRONT_LEG_LOW, "Squeeze your quads as both legs straighten to stand.",
            Sources.EXRX_SM_SPLIT_SQUAT,
        ),

        // ---- Machines ----
        // ExRx SL45LegPress lowers "until the knees are just short of complete flexion", past the guide's right angle,
        // so it is not cited. ACE 154 keeps the knees at about a right angle, the tailbone on the pad, the thighs off
        // the ribs and the knees unlocked: the guide's limits. The stretch names the hips on the pad, the guide's
        // lower-back warning.
        "Leg press" to lines(
            PRESS_BOTTOM, "Squeeze your quads as your legs straighten, knees not locked.",
            Sources.ACE_SEATED_LEG_PRESS,
        ),
        // ExRx SLSingleLeg45LegPress also lowers to just short of complete flexion, so ACE 154 (which lists the
        // single-leg press as a variation) is cited alone.
        "Single-leg leg press" to lines(
            PRESS_BOTTOM, "Squeeze your quads as your working leg straightens, not locked.",
            Sources.ACE_SEATED_LEG_PRESS,
        ),
        // ExRx SLHackSquat: lower "until knees are just short of complete flexion", the guide's own words.
        "Hack squat" to lines(
            "Your quads stretch with your knees just short of fully bent.",
            "Squeeze your quads as your knees and hips straighten.",
            Sources.EXRX_SL_HACK_SQUAT,
        ),
        // ExRx LVSquat and LVSquatPL: "just past parallel", the guide's depth.
        "Lever squat" to lines(SQUAT_BOTTOM, SQUAT_STAND, Sources.EXRX_LV_SQUAT, Sources.EXRX_LV_SQUAT_PL),
        // The pilot sample. No source sentence for the stretch was found: it is the guide's start, knees bent.
        "Leg extension" to lines(
            "Your quads stretch with your knees bent at the start.",
            "Squeeze your quads as your legs straighten.",
            Sources.EXRX_LV_LEG_EXTENSION,
        ),
        "Single-leg leg extension" to lines(
            "Your quads stretch with your knee bent at the start.",
            "Squeeze your quads as your working leg straightens.",
            Sources.EXRX_LV_ALT_LEG_EXTENSION, Sources.EXRX_LV_LEG_EXTENSION,
        ),
        // Not a lift and lower: the guide stays low behind the sled and drives it with quick steps. Nothing is lowered
        // under load and no step ends with the knees straight, so there is no stretch or squeeze to aim for (F5).
        "Sled push" to MuscleFocus.NoFocus(listOf(Sources.EXRX_SLED_PUSH)),

        // ---- Lunges ----
        // ACE 94: lower until the front thigh is parallel to the floor; ExRx DBLunge: until the rear knee almost
        // touches the floor. The guide's LUNGE_DEPTH is both together.
        "Lunge" to lines(
            FRONT_LEG_LOW, "Squeeze your quads as you push off your front leg to stand.",
            Sources.ACE_FORWARD_LUNGE, Sources.EXRX_DB_LUNGE,
        ),
        "Walking lunge" to lines(
            FRONT_LEG_LOW, "Squeeze your quads as you stand up on your front leg.",
            Sources.ACE_FORWARD_LUNGE, Sources.EXRX_DB_WALKING_LUNGE,
        ),
        // ExRx DBRearLunge: lower by bending the front knee and hip, stand by straightening the front leg.
        "Reverse lunge" to lines(FRONT_LEG_LOW, FRONT_FOOT_STAND, Sources.EXRX_DB_REAR_LUNGE, Sources.ACE_REVERSE_LUNGE),
        // ACE 319 is the barbell reverse lunge: step back and lower the back knee toward the floor.
        "Barbell reverse lunge" to lines(FRONT_LEG_LOW, FRONT_FOOT_STAND, Sources.ACE_REVERSE_LUNGE, Sources.EXRX_DB_REAR_LUNGE),
        "Barbell lunge" to lines(FRONT_LEG_LOW, FRONT_LEG_STRAIGHTENS, Sources.ACE_FORWARD_LUNGE, Sources.EXRX_BB_LUNGE),
        // The guide names no depth: hips back, the stepping knee bent with its shin near upright. The quads of that
        // leg are longest there. ACE 364: push the stepping hip back, press that foot to return to the middle.
        "Lateral lunge" to lines(
            "Your quads stretch as you sit back on the leg you stepped onto.",
            "Squeeze your quads as you push back to the middle.",
            Sources.ACE_LATERAL_LUNGE, Sources.EXRX_DB_SIDE_LUNGE,
        ),
        // Glutes first in its list. The guide (and ACE) gives no depth, only the front knee kept over the heel.
        "Curtsy lunge" to lines(
            "Your glutes stretch as you lower, front knee over your heel.",
            "Squeeze your glutes as you push up through your front leg.",
            Sources.ACE_BUTT_TONING_MOVES,
        ),

        // ---- Split squats and step-ups ----
        // ExRx DBSplitSquat: the rear knee bends until it almost touches the floor; stand by straightening the front leg.
        "Split squat" to lines(FRONT_LEG_LOW, FRONT_LEG_STRAIGHTENS, Sources.EXRX_DB_SPLIT_SQUAT),
        // The stretch is the start of each rep: the lead foot on a box no higher than the knee, the knee bent. The
        // squeeze is the guide's "Stand up on the box by straightening that leg".
        "Step-up" to lines(
            "Your quads stretch with your lead foot on the box, knee bent.",
            "Squeeze your quads as your lead leg straightens on the box.",
            Sources.EXRX_DB_STEP_UP, Sources.ACE_STEP_UP,
        ),
        // ExRx DBLateralStepUp: foot on the bench to the side, stand on it by straightening that leg.
        "Lateral step-up" to lines(
            "Your quads stretch with your near foot on the box, knee bent.",
            "Squeeze your quads as the leg on the box straightens.",
            Sources.EXRX_DB_LATERAL_STEP_UP,
        ),
    )
}
