package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

// Shared lines, so the bracing and hinge wording is the same in every hamstring and glute guide.
private const val BRACE_TRUNK = "Brace your trunk before each rep."
private const val LIGHT_BRACE = "Brace your trunk lightly, and keep breathing."
private const val RIGID_BRACE = "Brace your trunk and keep it rigid."
// Hinge limit: NSCA's RDL page stops the bar at about the top of the shin, or sooner if a neutral spine can't be
// held. The "back starts to round" stop is carried over to every hinge here, as the conservative positional limit.
private const val STOP_IF_ROUNDING = "Stop sooner if your back starts to round."
// Two-leg hinge depth. NSCA RDL: to about the top of the shin. ACE ProSource RDL: about knee height. The higher
// stop is used for both the stiff-leg deadlift and the DB RDL.
private const val HINGE_DEPTH = "Lower until the weight is about at your knees. $STOP_IF_ROUNDING"
private const val BACK_ROUNDS = "Letting your back round."
private const val HIPS_BACK_NOT_DOWN = "Hips back, not down."
private const val SWITCH_LEGS = "Do all your reps on one leg, then switch."
private const val SWITCH_SIDES = "Do all your reps on one side, then switch."
private const val KNEE_ON_PIVOT = "Line up your knees with the machine's pivot."
// ACE Glute Bridge (49): keeping the abs braced helps avoid arching the lower back.
private const val BRIDGE_ARCH = "Arching your lower back at the top."
// ExRx Barbell Hip Thrust: torso rigid; avoid the chest arching up and the pelvis tipping forward.
private const val THRUST_CHEST = "Lifting your chest and arching your lower back at the top."
private const val THRUST_PELVIS = "Tipping your pelvis forward at the top."

private val HINGE_MUSCLES = listOf("Hamstrings", "Glutes", "Lower back")
private val BRIDGE_MUSCLES = listOf("Glutes", "Hamstrings")
private val CURL_MUSCLES = listOf("Hamstrings")
private val ABDUCTION_MUSCLES = listOf("Glutes")

private fun hamGluteEntry(
    name: String,
    aliases: List<String>,
    equipment: List<Equipment>,
    muscles: List<String>,
    guide: FormGuide,
    sources: List<SourceId>,
    kind: Kind = Kind.COMPOUND,
) = LibraryExercise(
    name, aliases, BodyArea.HAMSTRINGS_GLUTES, equipment, muscles, kind, Category.ACCESSORY,
    parentLift = null, guide = guide, sources = sources,
)

/** BodyArea.HAMSTRINGS_GLUTES entries. */
internal val hamstringsGlutes: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "Hip thrust",
        aliases = listOf("Barbell hip thrust"),
        area = BodyArea.HAMSTRINGS_GLUTES,
        equipment = listOf(Equipment.BARBELL),
        muscles = listOf("Glutes", "Hips", "Upper back"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A hip lift with your upper back on a bench and a barbell across your hips. It mainly works your glutes.",
            setup = listOf(
                "Sit on the floor with your upper back against a bench about 40 to 45 cm high.",
                "Roll the bar over your hips so it sits across the top of your hips and lower belly. Pad it if needed.",
                "Plant your feet flat, about shoulder-width apart, with your knees bent.",
                "Brace your trunk and keep it rigid.",
            ),
            steps = listOf(
                "Drive through your feet and lift your hips until your body is straight.",
                "Lower your hips under control.",
            ),
            cues = listOf("Move at the hips; keep your trunk rigid.", "Keep your chest down at the top.", "Stay steady on the bench without sliding."),
            mistakes = listOf(
                "Lifting your chest and arching your lower back at the top.",
                "Tipping your pelvis forward at the top.",
            ),
        ),
        sources = listOf(Sources.EXRX_HIP_THRUST, Sources.ACE_HIP_THRUST),
    ),
    LibraryExercise(
        name = "RDL",
        aliases = listOf("Romanian deadlift"),
        area = BodyArea.HAMSTRINGS_GLUTES,
        equipment = listOf(Equipment.BARBELL),
        muscles = listOf("Hamstrings", "Lats", "Glutes", "Upper back"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A hip hinge that starts from standing. It builds your hamstrings, glutes and back.",
            setup = listOf(
                "Take the bar from a rack with the safety pins set, or deadlift it up from the floor.",
                "Grip the bar a little wider than your shoulders.",
                "Stand tall with your knees slightly bent.",
                "Brace your trunk before each rep.",
            ),
            steps = listOf(
                "Push your hips back. Keep the same slight bend in your knees.",
                "Slide the bar down close to your legs.",
                "Lower until you feel tension in the back of your thighs. For most people this is about knee height.",
                "Stand back up by driving your hips forward, keeping the bar close.",
            ),
            cues = listOf("Hips back, not down.", "Bar close to your legs.", "Back neutral."),
            mistakes = listOf(
                "Rounding your back.",
                "Bending your knees so it turns into a squat.",
                "Letting the bar drift away from your legs.",
            ),
        ),
        sources = listOf(Sources.EXRX_RDL, Sources.ACE_RDL, Sources.NSCA_RDL),
    ),
    // ---- Hinges ----
    hamGluteEntry(
        "Stiff-leg deadlift", listOf("SLDL", "Stiff-leg", "Stiff-legged deadlift", "Straight-back stiff-leg deadlift"),
        listOf(Equipment.BARBELL), HINGE_MUSCLES,
        FormGuide(
            purpose = "A hip hinge from standing with your knees only slightly bent. It works your hamstrings, glutes and lower back.",
            setup = listOf(
                "Stand with your feet flat under the bar.",
                "Grip the bar shoulder-width apart or a little wider, and stand up with it.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Bend your knees slightly and keep your back straight.",
                "Bend at your hips to lower the bar close to your legs.",
                HINGE_DEPTH,
                "Stand back up by straightening your hips. If your shoulders have rounded, pull them back at the top.",
            ),
            cues = listOf("Arms and back straight.", "Bar close to your legs.", HIPS_BACK_NOT_DOWN),
            mistakes = listOf(BACK_ROUNDS, "Bouncing the bar at the bottom.", "Letting the bar drift away from your legs."),
        ),
        listOf(
            Sources.EXRX_BB_SB_STIFF_LEG_DL, Sources.EXRX_BB_STIFF_LEG_DL, Sources.EXRX_BB_STRAIGHT_LEG_DL,
            Sources.NSCA_RDL, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    // No "Romanian" alias here, so a search for "romanian" still lists RDL first.
    hamGluteEntry(
        "DB RDL", listOf("Dumbbell RDL"),
        listOf(Equipment.DUMBBELL), HINGE_MUSCLES,
        FormGuide(
            purpose = "A Romanian deadlift with a dumbbell in each hand. It works your hamstrings and glutes.",
            setup = listOf(
                "Stand tall holding a dumbbell in each hand.",
                "Keep your knees slightly bent and your back straight.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Push your hips back. Keep the same slight bend in your knees.",
                "Lower the dumbbells close to your legs.",
                HINGE_DEPTH,
                "Push your heels into the floor and drive your hips forward to stand up.",
            ),
            cues = listOf(HIPS_BACK_NOT_DOWN, "Weights close to your legs.", "Keep your neck in line with your back and look at the floor."),
            mistakes = listOf(
                BACK_ROUNDS,
                "Bending your knees so it turns into a squat.",
                "Letting the weights drift away from your legs.",
            ),
        ),
        listOf(Sources.ACE_RDL_TECHNIQUE, Sources.ACE_RDL, Sources.NSCA_RDL, Sources.ACE_BRACE_OR_BREATHE),
    ),
    hamGluteEntry(
        "Single-leg RDL", listOf("SL RDL", "Single-leg Romanian deadlift", "Single-leg deadlift", "DB single-leg RDL"),
        listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT), BRIDGE_MUSCLES,
        FormGuide(
            purpose = "A hip hinge on one leg. It works the hamstrings and glutes of your standing leg, and your balance.",
            setup = listOf(
                "Stand with your feet about hip-width apart, back tall and knees slightly bent.",
                "Hold a dumbbell in the hand opposite your standing leg, or use no weight.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Push your hips back and lift one foot off the floor.",
                "Keep your back straight as that leg reaches straight back behind you.",
                "Keep a slight bend in your standing knee.",
                "Lower until the weight reaches the floor. $STOP_IF_ROUNDING",
                "Stand back up by squeezing the glute of your standing leg.",
                SWITCH_LEGS,
            ),
            cues = listOf("Back straight.", "Standing knee slightly bent.", "Back leg in line with your body."),
            mistakes = listOf(
                "Rounding your back to reach the floor.",
                "Bending at your lower back instead of pushing your hips back.",
            ),
        ),
        listOf(Sources.ACE_SA_SL_RDL, Sources.ACE_SL_RDL, Sources.EXRX_DB_SL_STIFF_LEG_DL, Sources.ACE_RDL_TECHNIQUE, Sources.NSCA_RDL),
    ),
    hamGluteEntry(
        "Good morning", listOf("Barbell good morning", "Bent-knee good morning"),
        listOf(Equipment.BARBELL), HINGE_MUSCLES,
        FormGuide(
            purpose = "A hip hinge with the bar across your upper back. It works your hamstrings, glutes and lower back.",
            setup = listOf(
                "Take the bar from a rack.",
                RACK_SAFETY,
                "Rest the bar across the back of your shoulders and grip it out to the sides.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Bend at your hips to lower your torso forward. Let your knees bend slightly.",
                "Keep your back straight.",
                // ExRx bent-knee good morning: torso down to parallel. The straight-knee page gives only a
                // stretch limit, so parallel is the lowest point used, with the neutral-spine stop.
                "Lower until your torso is level with the floor, no further. $STOP_IF_ROUNDING",
                "Raise your torso by straightening your hips until you stand tall.",
                RACK_IT,
            ),
            cues = listOf(HIPS_BACK_NOT_DOWN, "Back flat.", "Knees slightly bent."),
            mistakes = listOf(BACK_ROUNDS, "Lowering your torso past level with the floor."),
        ),
        listOf(Sources.EXRX_BB_BENT_KNEE_GOOD_MORNING, Sources.EXRX_BB_GOOD_MORNING, Sources.NSCA_RDL, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Leg curls ----
    hamGluteEntry(
        "Lying leg curl", listOf("Prone leg curl", "Lying hamstring curl", "Machine lying leg curl"),
        listOf(Equipment.MACHINE), CURL_MUSCLES,
        FormGuide(
            purpose = "A machine curl lying face down. It works your hamstrings.",
            setup = listOf(
                "Lie face down on the bench with your knees just past its edge.",
                KNEE_ON_PIVOT,
                "Set the pad on the back of your lower legs, above your heels.",
                "Grip the handles.",
                "Brace your abs and keep your lower back still.",
            ),
            steps = listOf(
                "Bend your knees to raise the pad toward the backs of your thighs.",
                "Lower the pad until your knees are straight.",
            ),
            cues = listOf("Hips and torso stay on the bench.", "Lower back still."),
            mistakes = listOf("Lifting your hips or arching your lower back.", "Setting the pad on your heels."),
        ),
        listOf(Sources.EXRX_LV_LYING_LEG_CURL, Sources.ACE_LYING_HAMSTRINGS_CURL),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Seated leg curl", listOf("Seated hamstring curl", "Machine seated leg curl"),
        listOf(Equipment.MACHINE), CURL_MUSCLES,
        FormGuide(
            purpose = "A machine curl done sitting down. It works your hamstrings.",
            setup = listOf(
                "Sit with your back against the back pad.",
                KNEE_ON_PIVOT,
                "Rest the backs of your lower legs on the pad, just above your ankles.",
                "Lower the lap pad onto your thighs, just above your knees.",
                "Grip the handles.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Bend your knees to pull the pad back under you.",
                "Let it come back until your legs are straight.",
            ),
            cues = listOf("Knees in line with the pivot.", "Back on the pad."),
            mistakes = listOf("Sitting with your knees out of line with the pivot.", "Resting the pad on your heels instead of above your ankles."),
        ),
        listOf(Sources.EXRX_LV_SEATED_LEG_CURL, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Standing leg curl", listOf("Machine standing leg curl", "Standing hamstring curl"),
        listOf(Equipment.MACHINE), CURL_MUSCLES,
        FormGuide(
            purpose = "A machine curl standing on one leg. It works each hamstring on its own.",
            setup = listOf(
                "Stand in the machine with the pad behind one lower leg.",
                "Line up that knee with the machine's pivot.",
                "Bend forward at your hips and grip the handles.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Bend your knee to raise the pad toward the back of your thigh.",
                "Lower until your knee is straight.",
                SWITCH_LEGS,
            ),
            cues = listOf("Knee in line with the pivot.", "Stay bent forward at your hips."),
            mistakes = listOf(
                "Standing straight up. Leaning forward at your hips lets your hamstrings work through more of the curl.",
                "Standing with your knee out of line with the pivot.",
            ),
        ),
        listOf(Sources.EXRX_LV_STANDING_LEG_CURL, Sources.EXRX_LV_STANDING_LEG_CURL_MULTI_HIP, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Cable leg curl", listOf("Cable standing leg curl", "Standing cable leg curl"),
        listOf(Equipment.CABLE), CURL_MUSCLES,
        FormGuide(
            purpose = "A standing curl with one ankle attached to a low cable. It works each hamstring on its own.",
            setup = listOf(
                "Attach a foot harness to a low pulley and put it on one ankle.",
                "Grip the support bar with both hands and step back with your other foot.",
                "Keep your arms straight to hold you up, with your working foot just off the floor.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Bend your knee to pull the cable back until your knee is fully bent.",
                "Straighten your knee to return.",
                SWITCH_LEGS,
            ),
            cues = listOf("Hips stay still.", "Arms straight to hold you up."),
            mistakes = listOf(
                "Letting your hips sag or drift forward.",
                "Using a leather ankle cuff with heavy weights. It can tear; use a foot harness.",
            ),
        ),
        listOf(Sources.EXRX_CB_STANDING_LEG_CURL, Sources.EXRX_CB_BENT_OVER_LEG_CURL, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Stability ball leg curl", listOf("Swiss ball leg curl", "Ball leg curl", "Stability ball hamstring curl", "Swiss ball hamstring curl"),
        listOf(Equipment.BODYWEIGHT), BRIDGE_MUSCLES,
        FormGuide(
            purpose = "A hip lift and leg curl with your heels on a stability ball. It works your hamstrings and glutes.",
            setup = listOf(
                "Lie on your back with your lower legs and heels on the ball, feet about hip-width apart.",
                "Put your arms out to the sides on the floor.",
                "Point your toes up toward the ceiling.",
                "Brace your trunk to keep your lower back flat.",
            ),
            steps = listOf(
                "Squeeze your glutes to lift your hips until your body is in a straight line.",
                "Pull your heels toward your hips, letting the ball roll in.",
                "Roll the ball back out, then lower your hips and legs to the start.",
            ),
            cues = listOf("Body in a straight line at the top.", "Torso stays in line with your thighs."),
            mistakes = listOf("Pushing your hips too high and arching your lower back.", "Letting your hips drop as you curl."),
        ),
        listOf(Sources.ACE_STABILITY_BALL_HAMSTRING_CURL),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Nordic curl", listOf("Nordic", "Nordic hamstring curl", "Nordic hamstring exercise"),
        listOf(Equipment.BODYWEIGHT), CURL_MUSCLES,
        FormGuide(
            purpose = "A kneeling curl where you lower your body forward under control. It works your hamstrings hard on the way down.",
            setup = listOf(
                "Kneel on a thick pad with your ankles under a fixed bar or pad.",
                "Keep your hips straight, so your body is in one line from your knees to your head.",
                "Hold your arms out to the sides with your elbows slightly bent and your palms facing forward.",
                "Brace your trunk.",
            ),
            steps = listOf(
                "Lower your body forward by straightening your knees. Keep your hips straight.",
                "Control the way down with your hamstrings for as long as you can.",
                "Only when your body starts to fall, touch down with your fingertips out to each side. Let your arms take the rest of the way down.",
                "Push off the floor just enough to help your hamstrings pull you back up.",
            ),
            cues = listOf("Hips straight.", "Hamstrings control the way down."),
            mistakes = listOf(
                "Bending at your hips as you lower.",
                "Pushing off the floor harder than you need to.",
            ),
        ),
        listOf(Sources.EXRX_SELF_ASSISTED_INVERSE_LEG_CURL_FLOOR, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    // The knee-bending curl on a glute-ham machine. ExRx calls it the inverse leg curl; its own "Glute-Ham Raise"
    // page is a different hip-bending move, which is not added here.
    hamGluteEntry(
        "Glute-ham raise", listOf("GHR", "Inverse leg curl"),
        listOf(Equipment.MACHINE, Equipment.BODYWEIGHT), BRIDGE_MUSCLES,
        FormGuide(
            purpose = "The knee-bending curl on a glute-ham machine: you lower your body until it is level, then curl back up with your knees. It works your hamstrings.",
            setup = listOf(
                "Adjust the pads, then kneel upright in the machine.",
                "Put your knees on the near side of the padded hump, your ankles between the ankle pads and your feet on the platform.",
                "Cross your arms over your chest, or put your hands behind your back.",
                "Brace your trunk.",
            ),
            steps = listOf(
                "Straighten your knees to lower your body until it is level with the floor.",
                "Bend your knees to pull yourself back up. Let your hips bend only slightly.",
            ),
            cues = listOf("Lower to level with the floor, no further.", "Hips bend only slightly."),
            mistakes = listOf(
                "Lowering past level with the floor.",
                "Letting it turn into a hip bend. Bending a lot at the hips is a different version; in this one your knees do the work.",
            ),
        ),
        listOf(Sources.EXRX_INVERSE_LEG_CURL, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),

    // ---- Bridges and thrusts ----
    hamGluteEntry(
        "Glute bridge", listOf("Bodyweight glute bridge", "Floor glute bridge"),
        listOf(Equipment.BODYWEIGHT), BRIDGE_MUSCLES,
        FormGuide(
            purpose = "A hip lift lying on your back. It works your glutes.",
            setup = listOf(
                "Lie on your back with your knees bent and your feet flat, hip-width apart.",
                "Gently brace your abs to flatten your lower back to the floor. Keep that brace the whole time.",
            ),
            steps = listOf(
                "Breathe out and lift your hips by squeezing your glutes. Press your heels into the floor.",
                "Breathe in and lower your hips under control.",
            ),
            cues = listOf("Heels pressed into the floor.", "Abs braced."),
            mistakes = listOf(BRIDGE_ARCH, "Letting your brace go at the top."),
        ),
        listOf(Sources.ACE_GLUTE_BRIDGE, Sources.ACE_SINGLE_LEG_GLUTE_BRIDGE),
    ),
    hamGluteEntry(
        "Single-leg glute bridge", listOf("Single-leg hip bridge", "One-leg glute bridge", "Single-leg bridge"),
        listOf(Equipment.BODYWEIGHT), BRIDGE_MUSCLES,
        FormGuide(
            purpose = "A hip lift on one leg, lying on your back. It works each glute on its own.",
            setup = listOf(
                "Lie on your back with your arms along your sides.",
                "Bend one knee with that foot flat on the floor. Keep the other leg straight.",
                "Brace your abs.",
            ),
            steps = listOf(
                "Breathe out, press your planted foot into the floor and lift your hips by squeezing that glute.",
                "Keep your straight leg in line with your hips.",
                "Breathe in and lower under control.",
                SWITCH_LEGS,
            ),
            cues = listOf("Push through your planted foot.", "Straight leg stays in line with your body."),
            mistakes = listOf(BRIDGE_ARCH, "Letting your straight leg drop below your hips."),
        ),
        listOf(Sources.ACE_SINGLE_LEG_GLUTE_BRIDGE, Sources.EXRX_SINGLE_LEG_HIP_BRIDGE, Sources.ACE_GLUTE_BRIDGE),
    ),
    hamGluteEntry(
        "Barbell glute bridge", listOf("Barbell hip bridge", "BB glute bridge"),
        listOf(Equipment.BARBELL), BRIDGE_MUSCLES,
        FormGuide(
            purpose = "A glute bridge on the floor with a barbell across your hips. It works your glutes against a heavier load.",
            setup = listOf(
                "Lie on your back under the bar, so it sits across the top of your hips. Pad it if needed.",
                "Grip the bar with your palms down.",
                "Bend your knees and set your feet flat on the floor.",
                "Brace your abs to keep your lower back from arching.",
            ),
            steps = listOf(
                "Press your feet into the floor and lift your hips toward the ceiling.",
                "Slowly lower your tailbone back to the floor.",
            ),
            cues = listOf("Hands keep the bar steady on your hips.", "Feet flat."),
            mistakes = listOf(BRIDGE_ARCH, "Dropping your hips fast instead of lowering slowly."),
        ),
        listOf(Sources.ACE_HIP_BRIDGE, Sources.EXRX_HIP_THRUST, Sources.ACE_GLUTE_BRIDGE),
    ),
    hamGluteEntry(
        "DB hip thrust", listOf("Dumbbell hip thrust", "Elevated glute bridge"),
        listOf(Equipment.DUMBBELL), listOf("Glutes", "Hamstrings"),
        FormGuide(
            purpose = "A hip lift with your upper back on a bench and dumbbells on your hips. It mainly works your glutes.",
            setup = listOf(
                "Lie across a flat bench with your upper back and shoulders on it.",
                "Press your feet into the floor about hip-width apart.",
                "Hold a dumbbell in each hand and rest them on your hips.",
                RIGID_BRACE,
            ),
            steps = listOf(
                "Lower your tailbone toward the floor, keeping the weights on your hips.",
                "Press your feet into the floor and squeeze your glutes to lift your hips until your body is straight.",
            ),
            cues = listOf("Move at the hips; keep your trunk rigid.", "Weights stay on your hips."),
            mistakes = listOf(THRUST_CHEST, THRUST_PELVIS),
        ),
        listOf(Sources.ACE_ELEVATED_GLUTE_BRIDGE, Sources.EXRX_HIP_THRUST),
    ),
    hamGluteEntry(
        "Machine hip thrust", listOf("Lever hip thrust", "Hip thrust machine", "Plate-loaded hip thrust"),
        listOf(Equipment.MACHINE), listOf("Glutes", "Hamstrings"),
        FormGuide(
            purpose = "A hip thrust on a machine, with a pad or belt across your hips. It mainly works your glutes.",
            setup = listOf(
                "Lie on the machine with your knees bent and your feet on the platform, shoulder-width apart or a little narrower.",
                "Set your hips just below the padded bar or belt. Lock or tighten it snugly over your hips.",
                RIGID_BRACE,
            ),
            steps = listOf(
                "Lift the weight by straightening your hips until your body is straight.",
                "Lower under control.",
            ),
            cues = listOf("Reach full hip extension every rep.", "Move at the hips; keep your trunk rigid."),
            mistakes = listOf(
                "Using a load too heavy to straighten your hips fully.",
                // Carried over from ExRx's barbell hip thrust page.
                THRUST_CHEST,
            ),
        ),
        listOf(Sources.EXRX_LV_HIP_THRUST, Sources.EXRX_HIP_THRUST),
    ),

    // ---- Swings ----
    hamGluteEntry(
        "Kettlebell swing", listOf("KB swing", "Two-arm kettlebell swing", "Russian swing", "Two-handed kettlebell swing"),
        listOf(Equipment.KETTLEBELL), BRIDGE_MUSCLES,
        FormGuide(
            purpose = "A fast hip hinge that swings a kettlebell up in front of you. It works your glutes and hamstrings.",
            setup = listOf(
                "Stand with your feet about shoulder-width apart and the kettlebell on the floor between them.",
                "Hinge at your hips and grip the handle with both hands, palms down.",
                "Stand up so the bell hangs between your thighs, arms straight.",
                "Brace your trunk and keep your back flat.",
            ),
            steps = listOf(
                "Hinge at your hips to swing the bell back between your legs. Keep your back straight and your shins upright.",
                "Drive your hips forward and straighten your knees to swing the bell forward and up.",
                // ACE: chest or shoulder height; NSCA: about eye level. The lower range is used.
                "Let it rise to chest or shoulder height, with your arms straight.",
                "Let the bell fall back. As your arms meet your body, hinge again and guide it back between your legs.",
                "To finish, slow the bell down and set it on the floor.",
            ),
            cues = listOf("Power from your hips, not your arms.", "Arms straight.", "Hinge, don't squat."),
            mistakes = listOf(
                "Squatting instead of hinging.",
                "Lifting the bell with your arms and shoulders.",
                "Bending your knees deeply.",
            ),
        ),
        listOf(Sources.ACE_KB_SWING, Sources.ACE_KB_SWING_TECHNIQUE, Sources.NSCA_KB_SWING, Sources.EXRX_KB_TWO_ARM_SWING),
    ),
    hamGluteEntry(
        "Single-arm kettlebell swing", listOf("One-arm kettlebell swing", "One-arm KB swing", "Single-arm KB swing"),
        listOf(Equipment.KETTLEBELL), BRIDGE_MUSCLES,
        FormGuide(
            purpose = "A kettlebell swing holding the bell in one hand. It works your glutes and hamstrings, and your trunk works against the twist.",
            setup = listOf(
                "Stand with your feet about shoulder-width apart and your knees slightly bent.",
                "Hold the bell in one hand, palm down, and grip the handle hard.",
                "Brace your trunk and keep your back straight.",
            ),
            steps = listOf(
                "Sink into your hips and draw the bell back between your legs.",
                "Push your hips forward and pull your knees back to swing the bell up in front of you.",
                "Let it rise to about chest height, then guide it back down between your legs.",
                "Let your free arm swing back as you hinge, and forward as your hips drive.",
                "Do all your reps on one arm, then switch.",
            ),
            cues = listOf("Power from your hips and legs, not your shoulder.", "Back straight."),
            mistakes = listOf("Lifting the bell with your shoulder.", "Squatting instead of hinging."),
        ),
        listOf(Sources.ACE_KB_SINGLE_ARM_SWING, Sources.ACE_KB_SWING_TECHNIQUE),
    ),

    // ---- Hip extensions ----
    // Named "Reverse hyper" so a search for "hyperextension" still lists the back extension first.
    hamGluteEntry(
        "Reverse hyper", listOf("Reverse hyperextension", "Reverse hyper-extension"),
        listOf(Equipment.MACHINE, Equipment.BODYWEIGHT), listOf("Glutes", "Hamstrings"),
        FormGuide(
            purpose = "Lying face down on a bench and raising your straight legs behind you. It works your glutes and hamstrings.",
            setup = listOf(
                "Lie face down with your torso and waist on the bench, and grip the handles.",
                "Let your legs hang straight, with your feet off the floor.",
                BRACE_TRUNK,
            ),
            steps = listOf(
                "Raise your legs by straightening your hips until your legs are nearly in line with your body.",
                "Lower your legs back to the start.",
            ),
            cues = listOf("Legs straight.", "Move at the hips."),
            mistakes = listOf(
                "Bending your knees a lot. It takes your hamstrings out of the lift.",
                "Resting your feet on the floor between reps.",
            ),
        ),
        listOf(Sources.EXRX_REVERSE_HYPER, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Quadruped hip extension", listOf("Donkey kick", "Bent-knee hip extension", "Quadruped bent-knee hip extension"),
        listOf(Equipment.BODYWEIGHT), listOf("Glutes"),
        FormGuide(
            purpose = "On all fours, lifting one bent leg behind you. It works each glute on its own.",
            setup = listOf(
                "Kneel on all fours with your hands under your shoulders and your knees under your hips.",
                "Set your knees and feet about hip-width apart.",
                "Brace your trunk so your back stays neutral, without sagging or arching.",
            ),
            steps = listOf(
                "Squeeze the glute on one side to lift that leg. Keep the knee bent and move only at the hip.",
                "Lower it back down.",
                SWITCH_LEGS,
            ),
            cues = listOf("Knee stays bent.", "Move only at the hip."),
            mistakes = listOf("Letting your lower back sag.", "Arching your lower back to lift the leg higher."),
        ),
        listOf(Sources.ACE_QUADRUPED_HIP_EXTENSION),
        kind = Kind.ACCESSORY,
    ),

    // ---- Abduction and adduction ----
    hamGluteEntry(
        "Machine hip abduction", listOf("Hip abduction", "Seated hip abduction", "Hip abduction machine", "Abductor machine"),
        listOf(Equipment.MACHINE), ABDUCTION_MUSCLES,
        FormGuide(
            purpose = "Pushing your legs apart against the pads of a seated machine. It works the outer hip muscles.",
            setup = listOf(
                "Sit with your legs inside the side pads. Put your heels on the foot bars if there are any.",
                "Use the lever to bring the pads together, then lock it.",
                "Sit back against the back pad and grip the handles.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Push your legs apart.",
                "Bring them back to the start under control.",
            ),
            cues = listOf("Back against the pad.", "Lever locked before you start."),
            mistakes = listOf("Starting without the lever locked.", "Leaning forward off the back pad."),
        ),
        listOf(Sources.EXRX_LV_SEATED_HIP_ABDUCTION, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Cable hip abduction", listOf("Standing cable hip abduction", "Cable abduction"),
        listOf(Equipment.CABLE), ABDUCTION_MUSCLES,
        FormGuide(
            purpose = "Lifting one leg out to the side against a low cable. It works the outer hip muscles of that leg.",
            setup = listOf(
                "Stand side-on to a low pulley and attach the cuff to your far ankle.",
                "Step away from the stack and hold the bar.",
                "Balance on the leg nearest the pulley, with your cuffed leg crossed in front of it.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Lift your cuffed leg out to the side, away from the pulley.",
                // ACE side-lying hip abduction's stop is carried over: past about 45 degrees the whole hip moves.
                "Stop when your hips start to tilt.",
                "Return under control.",
                SWITCH_LEGS,
            ),
            cues = listOf("Knee straight and pointing forward.", "Hips level."),
            mistakes = listOf("Lifting so high that your hips tilt.", "Leaning your torso to lift the leg higher."),
        ),
        listOf(Sources.EXRX_CB_HIP_ABDUCTION, Sources.ACE_SIDE_LYING_HIP_ABDUCTION, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Side-lying hip abduction", listOf("Side-lying leg raise", "Lying hip abduction"),
        listOf(Equipment.BODYWEIGHT), ABDUCTION_MUSCLES,
        FormGuide(
            purpose = "Lying on your side and lifting your top leg. It works the outer hip muscles.",
            setup = listOf(
                "Lie on your side with your legs straight.",
                "Stack your hips and shoulders, with your head in line with your spine.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Breathe out and lift your top leg with the knee straight and pointing forward.",
                "Stop when your hips start to tilt.",
                "Lower it slowly.",
                SWITCH_SIDES,
            ),
            cues = listOf("Knee points forward, not up.", "Hips stacked."),
            mistakes = listOf("Lifting so high that your whole hip tilts.", "Turning your knee up toward the ceiling."),
        ),
        listOf(Sources.ACE_SIDE_LYING_HIP_ABDUCTION, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Clamshell", listOf("Clam", "Clams", "Clam shell", "Banded clamshell"),
        listOf(Equipment.BODYWEIGHT, Equipment.BAND), ABDUCTION_MUSCLES,
        FormGuide(
            purpose = "Lying on your side with your knees bent and opening your top knee. It works the outer hip muscles.",
            setup = listOf(
                "Lie on your side with your hips stacked and both knees bent.",
                "Keep your feet together.",
                "For more resistance, put a mini band just above your knees.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Squeeze your glutes and lift your top knee, keeping your feet together.",
                "Stop before your pelvis moves or your hips roll back.",
                "Lower the knee.",
                SWITCH_SIDES,
            ),
            cues = listOf("Feet stay together.", "Pelvis stays still."),
            mistakes = listOf("Rolling your hips back to lift the knee higher.", "Letting your feet come apart."),
        ),
        listOf(Sources.ACE_GLUTE_MEDIUS_EXERCISES, Sources.ACE_JOINT_FRIENDLY_LOWER_BODY, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Lateral band walk", listOf("Band walk", "Side band walk", "Banded side step"),
        listOf(Equipment.BAND), ABDUCTION_MUSCLES,
        FormGuide(
            purpose = "Stepping sideways with a small band around your legs. It works the outer hip muscles.",
            setup = listOf(
                "Put a small band around your ankles or just above your knees.",
                "Stand with your feet about hip-width apart and your hips and knees slightly bent.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Step to the side, keeping tension on the band.",
                "Bring your other foot in, but keep the band tight.",
                "Take your steps one way, then come back the other way.",
            ),
            cues = listOf("Hips level.", "Band tight the whole time."),
            mistakes = listOf("Letting the band go slack between steps.", "Letting your hips drop or tilt."),
        ),
        listOf(Sources.ACE_JOINT_FRIENDLY_LOWER_BODY, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
    hamGluteEntry(
        "Machine hip adduction", listOf("Hip adduction", "Seated hip adduction", "Hip adduction machine", "Adductor machine"),
        listOf(Equipment.MACHINE), listOf("Adductors"),
        FormGuide(
            purpose = "Squeezing your legs together against the pads of a seated machine. It works your inner thighs.",
            setup = listOf(
                "Sit with your legs outside the pads.",
                "Set the lever to your starting width, then lock it.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Squeeze your legs together.",
                "Let them back out to the start under control.",
            ),
            cues = listOf("Legs come all the way together.", "Lever locked before you start."),
            mistakes = listOf("Starting without the lever locked.", "Stopping short of bringing your legs together."),
        ),
        listOf(Sources.EXRX_LV_SEATED_HIP_ADDUCTION, Sources.ACE_BRACE_OR_BREATHE),
        kind = Kind.ACCESSORY,
    ),
)
