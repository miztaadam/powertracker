package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

// Shared lines, so the bracing and safety wording is the same in every back guide.
private const val ROW_BRACE = "Brace your trunk and keep your back flat, with your head in line with your spine."
private const val SEATED_BRACE = "Brace your trunk and keep the normal curve in your lower back."
private const val CABLE_BRACE = "Brace your trunk and keep your back straight."
private const val LIGHT_BRACE = "Brace your trunk lightly, and keep breathing."
private const val HANG_BRACE = "Cross one leg over the other, and brace your abs."
private const val PULLDOWN_BRACE = "Brace your trunk, and pull your shoulder blades down and back."
private const val SHRUG_BRACE = "Brace your trunk, but keep breathing."
private const val EXTENSION_BRACE = "Brace your trunk lightly and keep your back neutral, not arched or rounded."
private const val THIGHS_UNDER_PADS = "Sit with your thighs under the pads."
private const val PULL_TO_CHEST = "Pull the bar down to your upper chest."
private const val PULLDOWN_RETURN = "Return under control until your arms and shoulders are fully stretched."
private const val HANG_RETURN = "Lower under control until your arms and shoulders are fully stretched."
private const val ARMS_STRAIGHT_DOWN = "Lower until your arms are straight and your shoulders are stretched down."
private const val TOO_WIDE = "Gripping so wide that the range of motion gets short."
private const val LEAN_BACK = "Leaning back to move the weight."
private const val LEAN_FAR_BACK = "Leaning far back to pull the weight down."
private const val SNAP_BACK = "Letting the weight pull your arms up fast instead of controlling it."
private const val SWINGING = "Swinging or kicking to get up."
private const val DROPPING = "Dropping fast at the bottom."
private const val NO_BOUNCE = "Bouncing or pausing at the bottom."
private const val SHRUG_HEIGHT =
    "It gets harder near the top. Lifting until the slope of your shoulders is level can be enough, depending on your build and the weight."
private const val SHRUG_TOO_HEAVY = "Going so heavy that your shoulders can't rise until their slope is level."
private const val HOLD_BREATH = "Holding your breath for the whole set."

private val ROW_MUSCLES = listOf("Upper back", "Lats")
private val PULL_MUSCLES = listOf("Lats", "Biceps")

private fun backEntry(
    name: String,
    aliases: List<String>,
    equipment: List<Equipment>,
    muscles: List<String>,
    kind: Kind,
    guide: FormGuide,
    sources: List<SourceId>,
) = LibraryExercise(
    name, aliases, BodyArea.BACK, equipment, muscles, kind, Category.ACCESSORY,
    parentLift = null, guide = guide, sources = sources,
)

/** BodyArea.BACK entries. */
internal val back: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "Row",
        aliases = listOf("Bent-over row", "Barbell row", "BB row", "Pendlay row"),
        area = BodyArea.BACK,
        equipment = listOf(Equipment.BARBELL),
        muscles = listOf("Lats", "Upper back"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A bent-over barbell row. It works your lats and upper back.",
            setup = listOf(
                "Bend your knees slightly and bend over the bar with your back straight.",
                "Grip the bar overhand, with a wide grip.",
                "Brace your trunk before each rep.",
            ),
            steps = listOf(
                "Pull the bar up to your upper waist.",
                "Lower it until your arms are straight and your shoulders are stretched down.",
                "Keep your torso still. For strict reps, keep it level with the floor.",
            ),
            cues = listOf("Back straight.", "Pull to your upper waist.", "Torso still."),
            mistakes = listOf(
                "Rounding your lower back. If it rounds, bend your knees more or don't lean as far.",
                "Pulling the bar to your chest instead of your upper waist.",
            ),
        ),
        sources = listOf(Sources.EXRX_BB_ROW),
    ),
    LibraryExercise(
        name = "Pull-up",
        aliases = listOf("Pullup"),
        area = BodyArea.BACK,
        equipment = listOf(Equipment.BODYWEIGHT),
        muscles = listOf("Lats", "Biceps"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A bodyweight pull up to a bar. It works your lats and biceps.",
            setup = listOf(
                "Grip the bar overhand with a wide grip, thumbs wrapped around the bar. Not so wide that the range gets short.",
                "Hang with straight arms.",
                "Pull your shoulders back and down, and hold them there for the whole set.",
                "Cross one leg over the other, and brace your abs.",
                "If you can't do a full pull-up yet, use a band or an assisted machine.",
            ),
            steps = listOf(
                "Pull yourself up until your chin clears the bar.",
                "Keep your body upright, without swinging.",
                "Lower under control until your arms and shoulders are fully stretched.",
            ),
            cues = listOf("Shoulders back and down.", "Body upright.", "No swinging."),
            mistakes = listOf(
                "Swinging or kicking to get up.",
                "Gripping so wide that the range gets short.",
                "Dropping fast at the bottom.",
            ),
        ),
        sources = listOf(Sources.EXRX_PULL_UP, Sources.ACE_PULL_UPS),
    ),

    // ---- Rows ----
    backEntry(
        "One-arm DB row",
        listOf("DB row", "Dumbbell row", "One-arm dumbbell row", "Single-arm DB row", "Single-arm row"),
        listOf(Equipment.DUMBBELL), ROW_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A dumbbell row with one knee and one hand on a bench. It works your lats and upper back, one side at a time.",
            setup = listOf(
                "Kneel with one knee on a bench, and put the hand on the same side on the bench, under your shoulder.",
                "Set your other foot on the floor, slightly back and out to the side.",
                "Pick up the dumbbell from the floor with your free hand.",
                ROW_BRACE,
                "Keep your torso close to level with the floor. Move the knee or hand on the bench forward or back to level it.",
            ),
            steps = listOf(
                "Pull the dumbbell up to your side, until it touches your ribs or your upper arm is just past level with your torso.",
                "Keep your elbow close to your body, and don't twist your torso.",
                "Lower it under control until your arm is straight and your shoulder is stretched down.",
                "Do all your reps, then switch arms.",
            ),
            cues = listOf("Back flat.", "Pull to your ribs.", "No twisting."),
            mistakes = listOf(
                "Twisting your torso to throw the weight up.",
                "Arching your lower back.",
                "Dropping the weight instead of lowering it under control.",
            ),
        ),
        listOf(Sources.EXRX_DB_BENT_OVER_ROW, Sources.ACE_SINGLE_ARM_ROW),
    ),
    backEntry(
        "Seated cable row",
        listOf("Cable row", "Seated row", "Cable seated row", "Low row"),
        listOf(Equipment.CABLE), ROW_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A seated row on a low cable, with your back kept straight. It works your upper back and lats.",
            setup = listOf(
                "Sit slightly forward on the bench with your feet on the footplates. Grip the close-grip handle.",
                "Sit up straight, then slide your hips back until your knees are slightly bent.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Pull the handle to your waist, keeping your elbows close to your ribs.",
                "Pull your shoulders back and lift your chest.",
                "Hold briefly, then return until your arms are straight. Keep your torso upright.",
            ),
            cues = listOf("Torso upright.", "Elbows close to your ribs.", "Shoulders back at the end."),
            mistakes = listOf(LEAN_BACK, NO_BOUNCE),
        ),
        listOf(
            Sources.EXRX_CB_STRAIGHT_BACK_SEATED_ROW, Sources.EXRX_CB_SEATED_ROW, Sources.ACE_CABLE_SEATED_ROW,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    backEntry(
        "Wide-grip cable row",
        listOf("Wide-grip seated row", "Wide-grip seated cable row"),
        listOf(Equipment.CABLE), listOf("Upper back", "Rear delts", "Lats"), Kind.COMPOUND,
        FormGuide(
            purpose = "A seated cable row with a wide, overhand grip on a bar. It works your upper back, rear delts and lats.",
            setup = listOf(
                "Sit slightly forward and grip the bar overhand, wider than your shoulders.",
                "Put your feet on the footplates. Sit up straight, then slide your hips back until your knees are slightly bent.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Pull the bar to your waist.",
                "Pull your shoulders back and lift your chest.",
                "Return slowly until your arms are straight, with your back straight.",
            ),
            cues = listOf("Back straight.", "Shoulders back at the end."),
            mistakes = listOf(
                LEAN_BACK,
                NO_BOUNCE,
                "Lowering the weight past a mild stretch.",
            ),
        ),
        listOf(Sources.EXRX_CB_WIDE_GRIP_STR_BACK_ROW, Sources.EXRX_CB_WIDE_GRIP_SEATED_ROW, Sources.ACE_BRACE_OR_BREATHE),
    ),
    backEntry(
        "One-arm cable row",
        listOf("Single-arm cable row", "One-arm seated cable row"),
        listOf(Equipment.CABLE), ROW_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A seated cable row with one arm at a time. It works your upper back and lats, one side at a time.",
            setup = listOf(
                "Sit slightly forward on the bench and grip a single handle with one hand.",
                "Slide your hips back, with your knees slightly bent. Let your arm reach forward.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Pull the handle to your side.",
                "At the end, pull your shoulder back and lift your chest.",
                "Pause for a second, then slowly straighten your arm.",
                "Do all your reps, then switch arms.",
            ),
            cues = listOf("Pause at the end.", "Slow on the way back."),
            mistakes = listOf("Rushing the return.", NO_BOUNCE),
        ),
        listOf(
            Sources.EXRX_CB_ONE_ARM_ROW, Sources.EXRX_CB_SEATED_ROW, Sources.ACE_CABLE_SINGLE_ARM_ROW,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    backEntry(
        "Machine row",
        listOf("Seated machine row", "Lever seated row", "Machine seated row"),
        listOf(Equipment.MACHINE), ROW_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A seated row on a machine with a chest pad. It works your upper back and lats.",
            setup = listOf(
                "Set the seat so the handles are about level with your shoulders.",
                "Set the chest pad so your shoulders can reach forward at the end of each rep.",
                "Sit with your feet planted and your chest lightly touching the pad. Grip the handles.",
                SEATED_BRACE,
                "Pull your shoulder blades down and back, and keep your wrists straight.",
            ),
            steps = listOf(
                "Pull the handles back until your elbows pass your torso.",
                "Keep your head still and don't lean back.",
                "Return slowly until your arms are straight. Keep your torso upright.",
            ),
            cues = listOf("Wrists in line with elbows.", "Don't lean back."),
            mistakes = listOf(LEAN_BACK, "Letting your shoulders round forward on the way back."),
        ),
        listOf(Sources.EXRX_LV_SEATED_ROW, Sources.ACE_SEATED_ROW),
    ),
    backEntry(
        "Chest-supported row",
        listOf("Chest-supported DB row", "Incline DB row", "Incline dumbbell row"),
        listOf(Equipment.DUMBBELL), ROW_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A dumbbell row lying face down on an incline bench. It works your upper back and lats.",
            setup = listOf(
                "Set the bench to a slight incline. A flatter bench gives a better line of pull, if it is high enough.",
                "The bench must be high enough that your shoulders can reach forward without the dumbbells touching the floor.",
                "Lie face down on the bench with a dumbbell in each hand, arms hanging down.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Pull the dumbbells up to your sides.",
                "Near the top, squeeze your shoulder blades together.",
                ARMS_STRAIGHT_DOWN,
            ),
            cues = listOf("Pull to your sides.", "Squeeze your shoulder blades together."),
            mistakes = listOf(
                "Using a bench so low that the dumbbells hit the floor.",
                "Stopping short of straight arms at the bottom.",
            ),
        ),
        listOf(Sources.EXRX_DB_INCLINE_ROW, Sources.EXRX_DB_LYING_ROW, Sources.ACE_BRACE_OR_BREATHE),
    ),
    backEntry(
        "Seal row",
        listOf("Lying row", "Cambered bar lying row", "DB lying row"),
        listOf(Equipment.SPECIALTY_BAR, Equipment.DUMBBELL), ROW_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A row lying chest down on a high, flat bench, with a cambered bar or dumbbells. It works your upper back and lats.",
            setup = listOf(
                "Lie chest down on a high, flat bench.",
                "The bench must be high enough that your shoulders can stretch forward without the weight hitting the floor.",
                "Grip a cambered bar below the bench with an overhand grip, or hold a dumbbell in each hand.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Pull the bar to your upper waist or lower chest. With dumbbells, pull them up to your sides.",
                ARMS_STRAIGHT_DOWN,
            ),
            cues = listOf("Pull to your upper waist.", "Full stretch at the bottom."),
            mistakes = listOf(
                "Using a bench so low that the weight hits the floor.",
                "Stopping short of straight arms at the bottom.",
            ),
        ),
        listOf(Sources.EXRX_CAMBERED_LYING_ROW, Sources.EXRX_DB_LYING_ROW, Sources.ACE_BRACE_OR_BREATHE),
    ),
    backEntry(
        "T-bar row",
        listOf("T-bar", "Lever T-bar row", "Close-grip T-bar row", "Landmine row"),
        listOf(Equipment.MACHINE, Equipment.BARBELL), listOf("Upper back", "Lats", "Rear delts"), Kind.COMPOUND,
        FormGuide(
            purpose = "A bent-over row on a plate-loaded T-bar lever. It works your upper back and lats.",
            setup = listOf(
                "Bend your knees slightly and bend over the lever handles with your back straight.",
                "Grip the handles overhand, shoulder-width or wider.",
                "Brace your trunk and keep your lower back straight.",
            ),
            steps = listOf(
                "Pull the handles up to your torso.",
                ARMS_STRAIGHT_DOWN,
                "Keep your torso still and your back flat for every rep.",
            ),
            cues = listOf(
                "Lower back straight.",
                "Torso stays down.",
                "Use a close, parallel grip to work your lats more.",
            ),
            mistakes = listOf(
                "Rounding your lower back.",
                "Letting your torso rise a lot to finish the rep. That means the weight is too heavy.",
            ),
        ),
        listOf(
            Sources.EXRX_LV_T_BAR_ROW, Sources.EXRX_LV_CLOSE_GRIP_T_BAR_ROW, Sources.EXRX_LV_CLOSE_GRIP_T_BAR_ROW_LM,
            Sources.ACE_BENT_OVER_ROW, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    backEntry(
        "Chest-supported T-bar row",
        listOf("Lever incline row", "Incline lever row", "Chest-supported machine row"),
        listOf(Equipment.MACHINE), ROW_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A plate-loaded row lying chest down on an incline pad. It works your upper back and lats.",
            setup = listOf(
                "Lie with your chest on the incline pad and grip the handles overhand.",
                "If the foot platform can't be set for your height, some machines let you put your feet on the floor.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Pull the handles up as high as you can.",
                "Near the top, pull your shoulders back.",
                ARMS_STRAIGHT_DOWN,
            ),
            cues = listOf("Wrists in line with elbows.", "Shoulders back at the top."),
            mistakes = listOf(
                "Letting your wrists get out of line with your elbows, which takes work off your back.",
                "Stopping short of straight arms at the bottom.",
            ),
        ),
        listOf(Sources.EXRX_LV_INCLINE_ROW, Sources.ACE_BRACE_OR_BREATHE),
    ),
    backEntry(
        "Inverted row",
        listOf("Body row", "Supine row", "Australian pull-up"),
        listOf(Equipment.BODYWEIGHT), ROW_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A bodyweight row, pulling your chest up to a fixed bar. It works your upper back and lats.",
            setup = listOf(
                "Set a fixed bar just high enough that your arms can fully straighten when you hang under it.",
                "Check that the bar is locked in place before you start.",
                "Lie on your back under the bar and grip it overhand, with a wide grip.",
                "Brace your trunk so your body stays rigid and straight, with your heels on the floor.",
                "To make it easier, raise the bar.",
            ),
            steps = listOf(
                "Pull your chest up to the bar.",
                "At the top, pull your shoulders back and keep your chest high.",
                "Lower until your arms are straight and your shoulders are stretched forward.",
            ),
            cues = listOf("Body rigid.", "Chest to the bar."),
            mistakes = listOf("Letting your hips sag.", "Stopping short of straight arms at the bottom."),
        ),
        listOf(Sources.EXRX_INVERTED_ROW),
    ),

    // ---- Pulldowns ----
    backEntry(
        "Lat pulldown",
        listOf("Wide-grip pulldown", "Lat pull-down"),
        listOf(Equipment.CABLE), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A seated pull of a wide bar down to your chest. It works your lats and biceps.",
            setup = listOf(
                THIGHS_UNDER_PADS,
                "Reach up and grip the bar overhand, with a wide grip.",
                PULLDOWN_BRACE,
                "Lean back only slightly, using your legs to support your body.",
            ),
            steps = listOf(
                "Start the pull by drawing your shoulder blades down.",
                PULL_TO_CHEST,
                "Stop when your elbows start to move back instead of down.",
                PULLDOWN_RETURN,
            ),
            cues = listOf("Shoulders down first.", "Bar to your upper chest.", "Control the way up."),
            mistakes = listOf(
                TOO_WIDE,
                LEAN_FAR_BACK,
                "Pulling on after your elbows start to move back.",
            ),
        ),
        listOf(Sources.EXRX_CB_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN),
    ),
    backEntry(
        "Close-grip pulldown",
        listOf("V-bar pulldown", "Close-grip lat pulldown"),
        listOf(Equipment.CABLE), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A lat pulldown with a narrow, parallel handle. It works your lats and biceps.",
            setup = listOf(
                "Grip the parallel handle, palms facing each other.",
                THIGHS_UNDER_PADS,
                PULLDOWN_BRACE,
            ),
            steps = listOf(
                "Pull the handle down to your upper chest.",
                PULLDOWN_RETURN,
            ),
            cues = listOf("Handle to your upper chest.", "Control the way up."),
            mistakes = listOf(LEAN_FAR_BACK, SNAP_BACK),
        ),
        listOf(Sources.EXRX_CB_CLOSE_GRIP_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN),
    ),
    backEntry(
        "Underhand pulldown",
        listOf("Reverse-grip pulldown", "Reverse-grip lat pulldown", "Supinated pulldown"),
        listOf(Equipment.CABLE), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A lat pulldown with an underhand grip. It works your lats and biceps.",
            setup = listOf(
                "Grip the bar underhand.",
                THIGHS_UNDER_PADS,
                PULLDOWN_BRACE,
            ),
            steps = listOf(
                "Pull the bar down to your upper chest, until your elbows are at your sides.",
                PULLDOWN_RETURN,
            ),
            cues = listOf("Elbows to your sides.", "Bar to your upper chest."),
            mistakes = listOf(LEAN_FAR_BACK, SNAP_BACK),
        ),
        listOf(Sources.EXRX_CB_UNDERHAND_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN),
    ),
    backEntry(
        "Machine pulldown",
        listOf("Lever pulldown", "Machine lat pulldown", "Plate-loaded pulldown"),
        listOf(Equipment.MACHINE), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A pulldown on a lever machine instead of a cable. It works your lats and biceps.",
            setup = listOf(
                "Grip the lever handles.",
                THIGHS_UNDER_PADS,
                PULLDOWN_BRACE,
            ),
            steps = listOf(
                "Pull the lever down to your upper chest.",
                PULLDOWN_RETURN,
            ),
            cues = listOf("Shoulders down first.", "Control the way up."),
            mistakes = listOf(LEAN_FAR_BACK, SNAP_BACK),
        ),
        listOf(Sources.EXRX_LV_FRONT_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN),
    ),
    backEntry(
        "One-arm pulldown",
        listOf("Single-arm pulldown", "One-arm lat pulldown", "Single-arm lat pulldown"),
        listOf(Equipment.CABLE), listOf("Lats"), Kind.COMPOUND,
        FormGuide(
            purpose = "A cable pulldown with one arm at a time, using a single handle. It works your lats, one side at a time.",
            setup = listOf(
                "Grip a single handle in one hand.",
                THIGHS_UNDER_PADS,
                PULLDOWN_BRACE,
            ),
            steps = listOf(
                "Pull the handle down to the side of your chest.",
                "Return until your arm is fully straight, letting your shoulder rise.",
                "Do all your reps, then switch arms.",
            ),
            cues = listOf("Handle to the side of your chest.", "Let your shoulder rise at the top."),
            mistakes = listOf(LEAN_FAR_BACK, SNAP_BACK),
        ),
        listOf(Sources.EXRX_CB_ONE_ARM_PULLDOWN, Sources.ACE_SEATED_LAT_PULLDOWN),
    ),
    backEntry(
        "Straight-arm pulldown",
        listOf("Straight-arm lat pulldown", "Seated straight-arm pulldown", "Cable seated pullover"),
        listOf(Equipment.CABLE), listOf("Lats"), Kind.ACCESSORY,
        FormGuide(
            purpose = "A cable pull with almost straight arms, from overhead down in front of you. It works your lats.",
            setup = listOf(
                "Sit on the bench facing away from the high pulley.",
                "Grip the bar attachment overhead, overhand. If it is too high to reach seated, grip it first, then sit down.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Keep a slight bend in your elbows, and keep it fixed.",
                "Pull the bar forward and down as far as you can.",
                "Return it forward and overhead under control.",
            ),
            cues = listOf("Slight bend in your elbows.", "Same bend the whole set."),
            mistakes = listOf("Bending and straightening your elbows during the rep.", "Rushing the return overhead."),
        ),
        listOf(Sources.EXRX_CB_SEATED_PULLOVER, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Pull-ups and chin-ups ----
    backEntry(
        "Chin-up",
        listOf("Chinup", "Underhand pull-up"),
        listOf(Equipment.BODYWEIGHT), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A bodyweight pull up to a bar with an underhand grip. It works your lats and biceps.",
            setup = listOf(
                "Grip the bar underhand, about shoulder-width apart, thumbs wrapped around the bar.",
                "Hang with straight arms.",
                HANG_BRACE,
                "If you can't do a full chin-up yet, use a band or an assisted machine.",
            ),
            steps = listOf(
                "Pull yourself up until your elbows are at your sides.",
                "Keep your body upright, without swinging.",
                HANG_RETURN,
            ),
            cues = listOf("Elbows down to your sides.", "No swinging."),
            mistakes = listOf(SWINGING, DROPPING),
        ),
        listOf(Sources.EXRX_CHIN_UP, Sources.ACE_PULL_UPS),
    ),
    backEntry(
        "Weighted pull-up",
        listOf("Weighted pullup"),
        listOf(Equipment.BODYWEIGHT), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A pull-up with added weight on a dip belt. It works your lats and biceps harder than bodyweight alone.",
            setup = listOf(
                "Put on a dip belt with the weight hanging from it.",
                "Grip the bar overhand with a wide grip. Not so wide that the range gets short.",
                "Hang with straight arms.",
                "Brace your abs.",
            ),
            steps = listOf(
                "Pull yourself up until your chin clears the bar.",
                "Keep your body upright, without swinging.",
                HANG_RETURN,
            ),
            cues = listOf("Chin over the bar.", "No swinging."),
            mistakes = listOf(SWINGING, TOO_WIDE, DROPPING),
        ),
        listOf(Sources.EXRX_WEIGHTED_PULL_UP, Sources.ACE_PULL_UPS),
    ),
    backEntry(
        "Weighted chin-up",
        listOf("Weighted chinup"),
        listOf(Equipment.BODYWEIGHT), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A chin-up with added weight on a dip belt. It works your lats and biceps harder than bodyweight alone.",
            setup = listOf(
                "Put on a dip belt with the weight hanging from it.",
                "Grip the bar underhand, about shoulder-width apart.",
                "Hang with straight arms.",
                "Brace your abs.",
            ),
            steps = listOf(
                "Pull yourself up until your elbows are at your sides.",
                "Keep your body upright, without swinging.",
                HANG_RETURN,
            ),
            cues = listOf("Elbows down to your sides.", "No swinging."),
            mistakes = listOf(SWINGING, DROPPING),
        ),
        listOf(Sources.EXRX_WEIGHTED_CHIN_UP, Sources.ACE_PULL_UPS),
    ),
    backEntry(
        "Assisted pull-up",
        listOf("Machine-assisted pull-up", "Assisted pull-up machine"),
        listOf(Equipment.MACHINE), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A pull-up on a machine that takes some of your bodyweight. It works your lats and biceps while you build up to full pull-ups.",
            setup = listOf(
                "Set the assistance weight. It is taken off your bodyweight, so more weight makes the pull-up easier.",
                "Step up and grip the bar overhand, with a wide grip. Not so wide that the range gets short.",
                "Kneel on the pad and lower yourself until your arms are straight. Some machines have you stand on the platform instead.",
                "Brace your abs.",
            ),
            steps = listOf(
                "Pull yourself up until your neck reaches the height of your hands.",
                "Keep your body upright.",
                HANG_RETURN,
            ),
            cues = listOf("Pull to hand height.", "Control the way down."),
            mistakes = listOf(TOO_WIDE, DROPPING),
        ),
        listOf(Sources.EXRX_ASSISTED_PULL_UP, Sources.EXRX_ACTUAL_RESISTANCE, Sources.ACE_PULL_UPS),
    ),
    backEntry(
        "Assisted chin-up",
        listOf("Machine-assisted chin-up"),
        listOf(Equipment.MACHINE), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A chin-up on a machine that takes some of your bodyweight. It works your lats and biceps while you build up to full chin-ups.",
            setup = listOf(
                "Set the assistance weight. It is taken off your bodyweight, so more weight makes the chin-up easier.",
                "Step up and grip the bar underhand, shoulder-width apart.",
                "Kneel on the pad, or stand on it if your machine is built that way.",
                "Brace your abs.",
            ),
            steps = listOf(
                "Pull yourself up until your elbows are at your sides.",
                "Keep your body upright.",
                HANG_RETURN,
            ),
            cues = listOf("Elbows down to your sides.", "Control the way down."),
            mistakes = listOf(DROPPING, "Cutting the bottom short instead of fully straightening your arms."),
        ),
        listOf(Sources.EXRX_ASSISTED_CHIN_UP, Sources.EXRX_ACTUAL_RESISTANCE, Sources.ACE_PULL_UPS),
    ),
    backEntry(
        "Band-assisted pull-up",
        listOf("Banded pull-up", "Band pull-up"),
        listOf(Equipment.BAND, Equipment.BODYWEIGHT), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A pull-up with a band hung from the bar to help you up. It works your lats and biceps while you build up to full pull-ups.",
            setup = listOf(
                "Hold the hanging band with both hands, stretch it down, and step onto the inside of its loop with one foot.",
                "Straighten that leg to stretch the band further.",
                "Reach up and grip the bar overhand, with a wide grip, keeping the banded leg straight.",
                "Put your free foot on top of the banded foot, so the band can't slip off.",
                "Brace your abs.",
                "A thicker band gives more help.",
            ),
            steps = listOf(
                "Pull yourself up until your chin is above the bar.",
                HANG_RETURN,
                "At the end of the set, step down with your free foot.",
            ),
            cues = listOf("Banded leg straight.", "Free foot on the banded foot."),
            mistakes = listOf("Leaving your free foot off the banded foot, so the band can slip.", TOO_WIDE),
        ),
        listOf(Sources.EXRX_BAND_ASSISTED_PULL_UP, Sources.ACE_PULL_UPS),
    ),
    backEntry(
        "Neutral-grip pull-up",
        listOf("Parallel-grip pull-up", "Hammer-grip pull-up"),
        listOf(Equipment.BODYWEIGHT), PULL_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A pull-up on handles or parallel bars, with your palms facing each other. It works your lats and biceps.",
            setup = listOf(
                "Step up and grip the handles with your palms facing each other.",
                "Hang with straight arms.",
                HANG_BRACE,
            ),
            steps = listOf(
                "Pull yourself up until your neck reaches the height of your hands.",
                "Keep your body upright, without swinging.",
                HANG_RETURN,
            ),
            cues = listOf("Palms face each other.", "No swinging."),
            mistakes = listOf(SWINGING, DROPPING),
        ),
        listOf(Sources.EXRX_NEUTRAL_GRIP_PULL_UP, Sources.EXRX_PARALLEL_CLOSE_GRIP_PULL_UP, Sources.ACE_PULL_UPS),
    ),

    // ---- Pullovers ----
    backEntry(
        "DB pullover",
        listOf("Dumbbell pullover"),
        listOf(Equipment.DUMBBELL), listOf("Pecs", "Lats"), Kind.ACCESSORY,
        FormGuide(
            purpose = "One dumbbell moved in an arc from over your chest to behind your head. It works your chest and lats.",
            setup = listOf(
                "Lie with your upper back across a flat bench, your body at a right angle to it.",
                "Let your hips and legs extend off the bench. They act as a counterweight.",
                "Hold one dumbbell with both hands under its inner plate, over your chest, with your elbows slightly bent.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Keeping the same slight bend in your elbows, lower the dumbbell back over your head until your upper arms are about level with your torso.",
                "Bring it back over your chest.",
            ),
            cues = listOf("Same elbow bend all the way.", "Hips stay down."),
            mistakes = listOf(
                "Letting your hips rise a lot.",
                "Forcing the weight lower than your shoulders can go. How far you can go depends on your shoulder flexibility.",
            ),
        ),
        listOf(Sources.EXRX_DB_PULLOVER, Sources.ACE_BRACE_OR_BREATHE),
    ),
    backEntry(
        "Machine pullover",
        listOf("Lever pullover", "Pullover machine"),
        listOf(Equipment.MACHINE), listOf("Lats"), Kind.ACCESSORY,
        FormGuide(
            purpose = "A pullover on a lever machine, pressing with your elbows on pads. It works your lats.",
            setup = listOf(
                "Set the seat so the machine's pivot is close to your shoulder.",
                "Sit down and push the foot lever. Put your elbows on the pads and grip the bar behind you.",
                "Let go of the foot lever, and put your feet on the platform or to the sides.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Pull the lever forward and down until your elbows are at your sides.",
                "Return until your upper arms are in line with your torso, or as far as your shoulders allow.",
                "When you finish, push the foot lever before you take your arms off. Then let go of the foot lever.",
            ),
            cues = listOf("Elbows down to your sides.", "Foot lever first, then let go."),
            mistakes = listOf(
                "Taking your arms off before you push the foot lever.",
                "Forcing the range further than your shoulder flexibility allows.",
            ),
        ),
        listOf(Sources.EXRX_LV_PULLOVER, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Shrugs ----
    backEntry(
        "Barbell shrug",
        listOf("Shrug", "BB shrug"),
        listOf(Equipment.BARBELL), listOf("Traps"), Kind.ACCESSORY,
        FormGuide(
            purpose = "Lifting your shoulders straight up while holding a barbell. It works your upper traps.",
            setup = listOf(
                "Stand up straight holding the bar with an overhand or mixed grip, shoulder-width or a little wider.",
                SHRUG_BRACE,
            ),
            steps = listOf(
                "Lift your shoulders as high as you can.",
                SHRUG_HEIGHT,
                "Lower them and repeat.",
            ),
            cues = listOf("Shoulders up.", "Stand tall."),
            mistakes = listOf(SHRUG_TOO_HEAVY, HOLD_BREATH),
        ),
        listOf(Sources.EXRX_BB_SHRUG, Sources.ACE_BRACE_OR_BREATHE),
    ),
    backEntry(
        "DB shrug",
        listOf("Dumbbell shrug"),
        listOf(Equipment.DUMBBELL), listOf("Traps"), Kind.ACCESSORY,
        FormGuide(
            purpose = "Lifting your shoulders straight up while holding a dumbbell in each hand. It works your upper traps.",
            setup = listOf(
                "Stand holding a dumbbell at each side.",
                "Stand fully upright, or lean very slightly forward.",
                SHRUG_BRACE,
            ),
            steps = listOf(
                "Lift your shoulders as high as you can.",
                SHRUG_HEIGHT,
                "Lower them and repeat.",
            ),
            cues = listOf("Shoulders up.", "Stand tall."),
            mistakes = listOf(SHRUG_TOO_HEAVY, HOLD_BREATH),
        ),
        listOf(Sources.EXRX_DB_SHRUG, Sources.ACE_BRACE_OR_BREATHE),
    ),
    backEntry(
        "Trap-bar shrug",
        listOf("Hex bar shrug"),
        listOf(Equipment.SPECIALTY_BAR), listOf("Traps"), Kind.ACCESSORY,
        FormGuide(
            purpose = "Lifting your shoulders straight up while standing inside a trap bar. It works your upper traps.",
            setup = listOf(
                "Stand inside the trap bar, holding its handles at your sides.",
                SHRUG_BRACE,
            ),
            steps = listOf(
                "Lift your shoulders as high as you can.",
                SHRUG_HEIGHT,
                "Lower them and repeat.",
            ),
            cues = listOf("Shoulders up.", "Stand tall."),
            mistakes = listOf(SHRUG_TOO_HEAVY, HOLD_BREATH),
        ),
        listOf(Sources.EXRX_TRAP_BAR_SHRUG, Sources.EXRX_DB_SHRUG, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Back extensions ----
    backEntry(
        "Back extension",
        listOf("Hyperextension", "45-degree back extension", "Roman chair back extension"),
        listOf(Equipment.BODYWEIGHT), listOf("Lower back", "Glutes", "Hamstrings"), Kind.ACCESSORY,
        FormGuide(
            purpose = "Lowering and raising your upper body on a 45-degree back extension bench. It works your lower back, glutes and hamstrings.",
            setup = listOf(
                "Set the thigh pad high enough to spread your weight over your thighs, but not so high that it limits your range. Your belly shouldn't press on the pad at the bottom.",
                "Hook your heels under the foot pad.",
                "Start with your hands low on your body, which is easiest. Over time, move them up, for example crossed on your chest.",
                EXTENSION_BRACE,
            ),
            steps = listOf(
                "Lower your upper body by bending at the hips, keeping your back neutral.",
                "Stop at a mild stretch in your hamstrings.",
                "Use your lower back, glutes and hamstrings to raise your body until your back is neutral and in line with your legs.",
            ),
            cues = listOf("Back neutral.", "Stop at a mild stretch in your hamstrings.", "Rise until you're in line with your legs."),
            mistakes = listOf(
                "Arching your back at the top.",
                NO_BOUNCE,
                "Moving your arms up too soon. Raise them over time so your lower back can adapt.",
            ),
        ),
        listOf(
            Sources.EXRX_45_HYPEREXTENSION, Sources.PMC_ROMAN_CHAIR, Sources.PMC_BACK_EXTENSION_LOADING,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    backEntry(
        "Weighted back extension",
        listOf("Weighted hyperextension", "Weighted 45-degree back extension"),
        listOf(Equipment.BODYWEIGHT), listOf("Lower back", "Glutes", "Hamstrings"), Kind.ACCESSORY,
        FormGuide(
            purpose = "A back extension holding a weight to your chest. It works your lower back, glutes and hamstrings harder than bodyweight alone.",
            setup = listOf(
                "Set the thigh pad high enough to spread your weight over your thighs, but not so high that it limits your range.",
                "Hook your heels under the foot pad.",
                "Hold a weight to your chest.",
                EXTENSION_BRACE,
                "Start with bodyweight only, and add weight slowly.",
            ),
            steps = listOf(
                "Lower your upper body by bending at the hips, keeping your back neutral.",
                "Stop at a mild stretch in your hamstrings.",
                "Use your lower back, glutes and hamstrings to raise your body until your back is neutral and in line with your legs.",
            ),
            cues = listOf("Back neutral.", "Stop at a mild stretch in your hamstrings."),
            mistakes = listOf(
                "Adding weight too fast.",
                NO_BOUNCE,
                "Arching your back at the top.",
            ),
        ),
        listOf(
            Sources.EXRX_WT_45_HYPEREXTENSION, Sources.EXRX_WT_45_BACK_EXTENSION, Sources.PMC_ROMAN_CHAIR, Sources.PMC_BACK_EXTENSION_LOADING,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),

    // ---- Face pulls ----
    backEntry(
        "Face pull",
        listOf("Cable face pull", "Rope face pull"),
        listOf(Equipment.CABLE), listOf("Rear delts", "Upper back"), Kind.ACCESSORY,
        FormGuide(
            purpose = "A rope pulled from a high cable toward your face, with your elbows out. It works your rear delts and upper back.",
            setup = listOf(
                "Set a rope on a high pulley.",
                "Grip the rope overhand, palms facing the floor.",
                "Step back until the cable is tight with your arms straight in front of your face and your elbows pointing out.",
                "Stand with your feet parallel and your knees slightly bent. Keep your head in line with your spine and your torso upright.",
                LIGHT_BRACE,
            ),
            steps = listOf(
                "Pull the rope toward your face, moving your elbows out to the sides as they bend.",
                "When your upper arms are in line with your shoulders, turn your arms out and keep pulling the rope toward your face.",
                "Return the same way, under control.",
            ),
            cues = listOf("Elbows out and high.", "Same body position the whole set."),
            mistakes = listOf(
                "Letting your elbows drop, which moves the work to your lats.",
                "Leaning or moving your body to pull the weight.",
            ),
        ),
        listOf(Sources.NSCA_FACE_PULL, Sources.EXRX_CB_REAR_DELT_ROW_ROPE, Sources.ACE_BRACE_OR_BREATHE),
    ),
)
