package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

// Shared lines, so the safety and bracing wording is the same in every chest guide.
private const val DB_BRACE = "Brace your trunk and don't arch your lower back."
private const val FLY_BRACE = "Brace your trunk lightly, and keep breathing."
private const val MACHINE_BRACE = "Brace your abs, but don't press your lower back into the backrest."
private const val CABLE_BRACE = "Brace your abs so your lower back doesn't arch."
private const val BODY_BRACE = "Brace your trunk."
private const val PUSH_UP_BRACE = "Brace your trunk and squeeze your glutes and thighs. Keep your head in line with your spine."
private const val SPOT_HEAVY = "Ask for a spot when you go heavy."
private const val SMITH_BRACE = "Breathe in and brace before you unhook the bar."
private const val SMITH_UNHOOK = "Unhook the bar by lifting it and turning it back."
private const val SMITH_REHOOK = "After the last rep, turn the bar to set it back on the hooks."
private const val DECLINE_SAFETY = "Use a spotter. If your bench is in a power rack, set its safety arms too."
private const val SMITH_STOPS = "Set the safety stops so the bar can't come down below your chest."
private const val BAR_GRIP = "Grip the bar wider than your shoulders."
private const val PRESS_UP = "Press the bar up until your arms are straight."
private const val TOO_WIDE = "Gripping so wide that the range of motion gets short."
private const val NO_SPOT = "Pressing without a spotter or safety arms."
private const val FAILURE_ALONE = "Training to failure without a spotter."
private const val ELBOWS_CHANGE = "Changing the bend in your elbows during the rep."
private const val LEANING = "Leaning forward, which adds stress to your shoulders. It often means the weight is too heavy."
private const val BODY_STRAIGHT = "Body in one straight line."
private const val HIPS_SAG = "Letting your hips sag."

private val PRESS_MUSCLES = listOf("Pecs", "Front delts", "Triceps")
private val FLY_MUSCLES = listOf("Pecs", "Front delts")

private fun chestEntry(
    name: String,
    aliases: List<String>,
    equipment: List<Equipment>,
    muscles: List<String>,
    kind: Kind,
    guide: FormGuide,
    sources: List<SourceId>,
) = LibraryExercise(
    name, aliases, BodyArea.CHEST, equipment, muscles, kind, Category.ACCESSORY,
    parentLift = null, guide = guide, sources = sources,
)

/** BodyArea.CHEST entries. */
internal val chest: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "DB press",
        aliases = listOf("Dumbbell bench press", "DB bench press", "DB bench"),
        area = BodyArea.CHEST,
        equipment = listOf(Equipment.DUMBBELL),
        muscles = listOf("Pecs", "Front delts"),
        kind = Kind.COMPOUND,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A bench press with a dumbbell in each hand. Each arm works on its own, through a longer range than with a barbell.",
            setup = listOf(
                "Sit on the end of a flat bench with the dumbbells resting on your lower thighs.",
                "Bring the dumbbells up to your shoulders and lie back. Hold them at the sides of your chest.",
                "Keep your back neutral and brace your trunk.",
                "Use a spotter for heavy sets.",
            ),
            steps = listOf(
                "Press the dumbbells up until your arms are straight. They move slightly in, ending over your shoulders.",
                "Lower them under control to the sides of your chest, until you feel a slight stretch.",
                "Repeat for all your reps.",
            ),
            cues = listOf(
                "Up and slightly in.",
                "Control the bottom.",
                "Elbows closer to your sides works the triceps more; wider works the chest more.",
            ),
            mistakes = listOf("Bouncing the weights at the bottom.", "Arching your back off the bench."),
        ),
        sources = listOf(Sources.EXRX_DB_BENCH, Sources.ACE_DB_CHEST_PRESS),
    ),

    // ---- Barbell ----
    chestEntry(
        "Incline bench press", listOf("Incline bench", "Barbell incline bench press"), listOf(Equipment.BARBELL), PRESS_MUSCLES,
        Kind.COMPOUND,
        FormGuide(
            purpose = "A barbell bench press on an incline bench. It puts more of the work on the upper part of your chest.",
            setup = listOf(BENCH_SAFETY, "Lie back on the incline bench.", BAR_GRIP, BENCH_BRACE),
            steps = listOf(
                "Lift the bar out and hold it over your upper chest with straight arms.",
                "Lower the bar to your chest.",
                PRESS_UP,
                RACK_IT,
            ),
            cues = listOf("Start with the bar over your upper chest.", "Not too wide a grip.", "Press until your arms are straight."),
            mistakes = listOf(TOO_WIDE, NO_SPOT),
        ),
        listOf(Sources.EXRX_BB_INCLINE_BENCH, Sources.EXRX_BB_BENCH, Sources.NSCA_STANDARDS_SUMMARY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "Decline bench press", listOf("Decline bench", "Barbell decline bench press"), listOf(Equipment.BARBELL), PRESS_MUSCLES,
        Kind.COMPOUND,
        FormGuide(
            purpose = "A barbell bench press on a decline bench. It works mainly the lower part of your chest.",
            setup = listOf(DECLINE_SAFETY, "Lie back on the decline bench with your feet hooked under the leg brace.", BAR_GRIP, BENCH_BRACE),
            steps = listOf(
                "Lift the bar out and hold it over your chest with straight arms.",
                "Lower the bar to your chest.",
                PRESS_UP,
                RACK_IT,
            ),
            cues = listOf("Feet stay hooked under the brace.", "Not too wide a grip."),
            mistakes = listOf(TOO_WIDE, NO_SPOT),
        ),
        listOf(Sources.EXRX_BB_DECLINE_BENCH, Sources.NSCA_STANDARDS_SUMMARY, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Dumbbell ----
    chestEntry(
        "Incline DB press", listOf("Incline dumbbell press", "Incline dumbbell bench press", "Incline DB"),
        listOf(Equipment.DUMBBELL), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A dumbbell press on an incline bench. It puts more of the work on the upper part of your chest.",
            setup = listOf(
                "Set the bench to an incline of 45 to 60 degrees.",
                "Sit on the bench with the dumbbells resting on your thighs.",
                "Kick the dumbbells up to your shoulders as you lie back. Hold them at the sides of your chest.",
                "Keep your head, shoulders and buttocks on the bench and your feet on the floor.",
                "Pull your shoulder blades back and down.",
                DB_BRACE,
                SPOT_HEAVY,
            ),
            steps = listOf(
                "Press the dumbbells up until your arms are straight. They move in a slight arc, ending over your shoulders.",
                "Lower them under control to your upper chest, until you feel a slight stretch.",
                "If you have a spotter, they take the dumbbells from you at the end of the set.",
            ),
            cues = listOf("Finish over your shoulders.", "Shoulder blades back and down.", "Wrists straight."),
            mistakes = listOf("Bouncing the dumbbells off your chest.", "Arching your lower back off the bench.", FAILURE_ALONE),
        ),
        listOf(Sources.EXRX_DB_INCLINE_BENCH, Sources.ACE_INCLINE_CHEST_PRESS, Sources.EXRX_GYM_ETIQUETTE),
    ),
    chestEntry(
        "Decline DB press", listOf("Decline dumbbell press", "Decline dumbbell bench press"),
        listOf(Equipment.DUMBBELL), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A dumbbell press on a decline bench. It works mainly the lower part of your chest.",
            setup = listOf(
                "Sit on the decline bench with your feet under the leg brace and the dumbbells resting on your thighs.",
                "Lie back and hold the dumbbells at the sides of your chest, with each forearm under its dumbbell.",
                DB_BRACE,
                SPOT_HEAVY,
            ),
            steps = listOf(
                "Press the dumbbells up, elbows out to the sides, until your arms are straight.",
                "They move in a slight arc, ending over your shoulders.",
                "Lower them to the sides of your chest, until you feel a slight stretch in your chest or shoulders.",
            ),
            cues = listOf("Finish over your shoulders.", "Elbows out to the sides.", "Stop at a slight stretch."),
            mistakes = listOf("Lowering further than a slight stretch in your chest or shoulders.", FAILURE_ALONE),
        ),
        listOf(Sources.EXRX_DB_DECLINE_BENCH, Sources.ACE_DB_CHEST_PRESS, Sources.EXRX_GYM_ETIQUETTE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "DB fly", listOf("Dumbbell fly", "Dumbbell flye", "Chest fly", "Flat fly"), listOf(Equipment.DUMBBELL), FLY_MUSCLES,
        Kind.ACCESSORY,
        FormGuide(
            purpose = "A dumbbell fly on a flat bench. It works your chest through a wide hugging arc.",
            setup = listOf(
                "Lie on a flat bench with a dumbbell in each hand. Put your feet on the floor, or on a raised platform, so your spine stays neutral.",
                "Keep your head, shoulders and buttocks on the bench.",
                "Pull your shoulder blades down and back.",
                "Hold the dumbbells over your chest, arms straight but elbows slightly bent.",
                FLY_BRACE,
                "A spotter can hand you the dumbbells.",
            ),
            steps = listOf(
                "Breathe in and lower the dumbbells out to the sides in a wide arc, until they are level with your shoulders or chest.",
                "Keep the bend in your elbows the same.",
                "Breathe out and bring them back up along the same arc.",
            ),
            cues = listOf("Elbows stay slightly bent.", "Wide hugging arc.", "Wrists straight."),
            mistakes = listOf(
                ELBOWS_CHANGE,
                "Lowering the dumbbells below the level of your shoulders.",
                "Lifting your head or buttocks off the bench.",
            ),
        ),
        listOf(Sources.EXRX_DB_FLY, Sources.ACE_LYING_CHEST_FLY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "Incline DB fly", listOf("Incline dumbbell fly", "Incline fly"), listOf(Equipment.DUMBBELL), FLY_MUSCLES, Kind.ACCESSORY,
        FormGuide(
            purpose = "A dumbbell fly on an incline bench. It works mainly the upper part of your chest.",
            setup = listOf(
                "Lie back on an incline bench with a dumbbell in each hand.",
                "Hold the dumbbells over your upper chest, elbows slightly bent and pointing out to the sides.",
                FLY_BRACE,
            ),
            steps = listOf(
                "Lower the dumbbells out to the sides of your shoulders. Keep the bend in your elbows fixed.",
                "When you feel a stretch in your chest or shoulders, bring the dumbbells back up in a hugging motion.",
                "Stop when they are nearly together over your upper chest.",
            ),
            cues = listOf("Elbows fixed, slightly bent.", "Stop at the stretch.", "Expect less range than on a flat bench."),
            mistakes = listOf(ELBOWS_CHANGE, "Forcing the same range as a flat fly. Your shoulders have less range on an incline."),
        ),
        listOf(Sources.EXRX_DB_INCLINE_FLY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "Decline DB fly", listOf("Decline dumbbell fly", "Decline fly"), listOf(Equipment.DUMBBELL), FLY_MUSCLES, Kind.ACCESSORY,
        FormGuide(
            purpose = "A dumbbell fly on a decline bench. It works mainly the lower part of your chest.",
            setup = listOf(
                "Sit on the decline bench with your feet under the leg brace.",
                "Lie back and hold the dumbbells over your upper abdomen, elbows slightly bent.",
                FLY_BRACE,
            ),
            steps = listOf(
                "Lower the dumbbells out to the sides until you feel a stretch in your chest. Keep the bend in your elbows fixed.",
                "Bring them back together in a wide hugging arc, until they nearly touch.",
            ),
            cues = listOf("Elbows fixed and pointing out.", "Stop at the stretch."),
            mistakes = listOf(ELBOWS_CHANGE, "Holding the dumbbells over your face instead of your upper abdomen."),
        ),
        listOf(Sources.EXRX_DB_DECLINE_FLY, Sources.EXRX_DB_DECLINE_BENCH, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Machine ----
    chestEntry(
        "Machine chest press", listOf("Chest press machine", "Seated chest press", "Lever chest press"),
        listOf(Equipment.MACHINE), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A seated press on a chest press machine. It works your chest, front shoulders and triceps.",
            setup = listOf(
                "Set the seat so the handles are level with the middle of your chest.",
                "Sit with your back and shoulder blades against the backrest.",
                "If the machine has a foot lever, push it to bring the handles forward.",
                "Grip the handles with your wrists in line with your forearms, then let go of the foot lever.",
                MACHINE_BRACE,
            ),
            steps = listOf(
                "Breathe out and press the handles forward until your arms are straight, without locking your elbows.",
                "Let the handles come back slowly, until you feel a slight stretch in your chest.",
                "After the last rep, push the foot lever again before you let go of the handles.",
            ),
            cues = listOf("Shoulder blades stay on the backrest.", "Head in line with your spine.", "Don't lock your elbows."),
            mistakes = listOf("Rounding your shoulders forward off the backrest as you press.", "Arching your lower back.", TOO_WIDE),
        ),
        listOf(Sources.EXRX_LV_CHEST_PRESS, Sources.ACE_SEATED_CHEST_PRESS, Sources.ACE_SEATED_CLOSE_GRIP_CHEST_PRESS),
    ),
    chestEntry(
        "Incline machine press", listOf("Incline chest press machine", "Machine incline press", "Lever incline chest press"),
        listOf(Equipment.MACHINE), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A chest press on an incline machine. It puts more of the work on the upper part of your chest.",
            setup = listOf(
                "Set the seat so your upper chest is just above the handles.",
                "Sit with your back against the backrest.",
                "If the machine has a foot lever, push it to bring the handles into reach.",
                "Grip the handles wide, elbows out to the sides, then let go of the foot lever.",
                MACHINE_BRACE,
            ),
            steps = listOf(
                "Press until your arms are straight, without locking your elbows.",
                "Let the handles come back slowly, until you feel a slight stretch in your chest or shoulders.",
            ),
            cues = listOf("Back and shoulder blades on the pad.", "Slow on the way back.", "Not too wide a grip."),
            mistakes = listOf("Rounding your shoulders forward off the pad.", TOO_WIDE),
        ),
        listOf(Sources.EXRX_LV_INCLINE_CHEST_PRESS, Sources.ACE_SEATED_CHEST_PRESS, Sources.ACE_SEATED_CLOSE_GRIP_CHEST_PRESS),
    ),
    chestEntry(
        "Decline machine press", listOf("Decline chest press machine", "Machine decline press"),
        listOf(Equipment.MACHINE), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A chest press on a decline machine. It works mainly the lower part of your chest.",
            setup = listOf(
                "Set the seat so the handles are about level with your lower chest.",
                "Grip the handles wide and overhand, elbows out to the sides just below your shoulders.",
                MACHINE_BRACE,
            ),
            steps = listOf(
                "Press until your arms are straight.",
                "Let the weight come back until you feel a slight stretch in your chest.",
            ),
            cues = listOf("Elbows just below your shoulders.", "Stop at a slight stretch."),
            mistakes = listOf(TOO_WIDE, "Rounding your shoulders forward off the backrest."),
        ),
        listOf(Sources.EXRX_LV_DECLINE_CHEST_PRESS, Sources.ACE_SEATED_CHEST_PRESS, Sources.ACE_SEATED_CLOSE_GRIP_CHEST_PRESS),
    ),
    chestEntry(
        "Pec deck", listOf("Pec deck fly", "Machine fly", "Pec fly", "Butterfly"), listOf(Equipment.MACHINE), FLY_MUSCLES,
        Kind.ACCESSORY,
        FormGuide(
            purpose = "A fly on a seated machine. It works your chest through a hugging motion.",
            setup = listOf(
                "Sit with your back on the pad.",
                "Grip the handles at shoulder height. On a machine with arm pads, rest your forearms on the pads.",
                "Bend your elbows slightly.",
                FLY_BRACE,
            ),
            steps = listOf(
                "Bring the handles forward and together.",
                "Let them go back until you feel a mild stretch in your chest or shoulders.",
                "Repeat for all your reps.",
            ),
            cues = listOf("Back on the pad.", "Elbows slightly bent.", "Stop at a mild stretch."),
            mistakes = listOf("Letting the handles pull your arms back past a mild stretch.", "Leaning forward off the pad."),
        ),
        listOf(Sources.EXRX_LV_SEATED_FLY, Sources.EXRX_LV_PEC_DECK, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Smith machine ----
    chestEntry(
        "Smith bench press", listOf("Smith machine bench press", "Smith bench"), listOf(Equipment.SMITH), PRESS_MUSCLES,
        Kind.COMPOUND,
        FormGuide(
            purpose = "A bench press on a Smith machine, where the bar runs on fixed rails. It works your chest, front shoulders and triceps.",
            setup = listOf(SMITH_STOPS, "Lie on the bench with your chest under the bar.", BAR_GRIP, SPOT_HEAVY, SMITH_BRACE),
            steps = listOf(SMITH_UNHOOK, "Lower the bar to your chest.", PRESS_UP, SMITH_REHOOK),
            cues = listOf("Chest under the bar.", "Not too wide a grip."),
            mistakes = listOf(TOO_WIDE, FAILURE_ALONE),
        ),
        listOf(Sources.EXRX_SM_BENCH, Sources.EXRX_GYM_ETIQUETTE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "Smith incline press", listOf("Smith machine incline press", "Smith incline bench press", "Smith incline"),
        listOf(Equipment.SMITH), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "An incline bench press on a Smith machine. It puts more of the work on the upper part of your chest.",
            setup = listOf(
                SMITH_STOPS,
                "Lie back on an incline bench in the Smith machine, with the bar over your chest.",
                BAR_GRIP,
                SPOT_HEAVY,
                SMITH_BRACE,
            ),
            steps = listOf(SMITH_UNHOOK, "Lower the bar to your chest.", PRESS_UP, SMITH_REHOOK),
            cues = listOf("Bar over your chest.", "Not too wide a grip."),
            mistakes = listOf(TOO_WIDE, FAILURE_ALONE),
        ),
        listOf(Sources.EXRX_SM_INCLINE_BENCH, Sources.EXRX_GYM_ETIQUETTE, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "Smith decline press", listOf("Smith machine decline press", "Smith decline bench press"),
        listOf(Equipment.SMITH), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A decline bench press on a Smith machine. It works mainly the lower part of your chest.",
            setup = listOf(
                SMITH_STOPS,
                "Lie back on a decline bench in the Smith machine, feet under the leg brace and your lower chest under the bar.",
                BAR_GRIP,
                SPOT_HEAVY,
                SMITH_BRACE,
            ),
            steps = listOf(SMITH_UNHOOK, "Lower the bar to your lower chest.", PRESS_UP, SMITH_REHOOK),
            cues = listOf("Lower chest under the bar.", "Feet stay under the brace."),
            mistakes = listOf(TOO_WIDE, FAILURE_ALONE),
        ),
        listOf(Sources.EXRX_SM_DECLINE_BENCH, Sources.EXRX_GYM_ETIQUETTE, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Cable ----
    chestEntry(
        "Cable crossover",
        listOf("Cable fly", "Standing cable fly", "Cable crossover fly", "Crossovers", "High-to-low cable fly", "High cable fly"),
        listOf(Equipment.CABLE), FLY_MUSCLES, Kind.ACCESSORY,
        FormGuide(
            purpose = "A standing chest fly between two high cable pulleys. It works your chest through a hugging motion.",
            setup = listOf(
                "Set both pulleys high, with a handle on each.",
                "Hold one handle in each hand and step forward, so your arms are out to the sides.",
                "Bend your hips and knees slightly, and bend your elbows slightly.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Bring your hands together in front of you in a hugging arc. Keep the bend in your elbows fixed.",
                "Pause for about a second with your hands together.",
                "Let your arms go back slowly, until you feel a stretch in your chest.",
            ),
            cues = listOf("Elbows fixed, slightly bent.", "Stay upright.", "Slow on the way back."),
            mistakes = listOf(LEANING, "Arching your lower back."),
        ),
        listOf(Sources.EXRX_CB_STANDING_FLY, Sources.ACE_STANDING_CHEST_FLY, Sources.ACE_STANDING_INCLINE_CABLE_FLYES),
    ),
    chestEntry(
        "Low-to-high cable fly", listOf("Standing incline cable fly", "Low cable fly"),
        listOf(Equipment.CABLE), FLY_MUSCLES, Kind.ACCESSORY,
        FormGuide(
            purpose = "A standing cable fly that moves from low to high. It works mainly the upper part of your chest.",
            setup = listOf(
                "Set both pulleys low, between knee and hip height, with a handle on each.",
                "Hold one handle in each hand. Step forward until the cables are tight, and stand with your feet staggered.",
                "Start with your arms down and out to your sides, elbows slightly bent.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Bring your hands forward and up in an arc, until they meet at about face height.",
                "Keep a slight bend in your elbows.",
                "Lower your arms back slowly, until you feel a stretch in your chest or shoulders.",
                "As your arms open, don't let your hands go back past the level of your armpits.",
            ),
            cues = listOf("Up and in.", "Slight bend in your elbows.", "Stay upright."),
            mistakes = listOf(LEANING, "Arching your lower back."),
        ),
        listOf(Sources.EXRX_CB_STANDING_INCLINE_FLY, Sources.ACE_STANDING_INCLINE_CABLE_FLYES),
    ),
    chestEntry(
        "Cable chest press", listOf("Standing cable chest press", "Standing cable press"),
        listOf(Equipment.CABLE), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A standing press between two cable pulleys. It works your chest, front shoulders and triceps.",
            setup = listOf(
                "Set both pulleys at shoulder height, with a handle on each.",
                "Stand between them with your back to the machine, a handle in each hand.",
                "Take a split stance, one foot forward and one back.",
                "Hold the handles beside your chest, elbows out to the sides.",
                "Pull your shoulder blades back and down.",
                CABLE_BRACE,
            ),
            steps = listOf(
                "Press the handles forward until your arms are straight and level.",
                "Let them come back until you feel a slight stretch in your chest or shoulders.",
            ),
            cues = listOf("Split stance.", "Press straight forward.", "Shoulder blades back and down."),
            mistakes = listOf("Rounding your shoulders forward as you press.", "Pushing your head forward."),
        ),
        listOf(Sources.EXRX_CB_STANDING_CHEST_PRESS, Sources.ACE_SEATED_CABLE_PRESS),
    ),
    chestEntry(
        "Lying cable fly", listOf("Cable lying fly", "Flat cable fly", "Cable bench fly"),
        listOf(Equipment.CABLE), FLY_MUSCLES, Kind.ACCESSORY,
        FormGuide(
            purpose = "A chest fly lying on a flat bench between two low cable pulleys. It works your chest through a hugging motion.",
            setup = listOf(
                "Set both pulleys low, with a handle on each.",
                "Place a flat bench in the middle, square to the pulleys. Lie on it with a handle in each hand.",
                "Bend your elbows slightly.",
                FLY_BRACE,
            ),
            steps = listOf(
                "Bring the handles together over your chest in a hugging arc. Keep the bend in your elbows fixed.",
                "Let them go back out until you feel a slight stretch.",
            ),
            cues = listOf("Elbows fixed.", "Hugging arc."),
            mistakes = listOf(ELBOWS_CHANGE, "Letting the cables pull your arms back past a slight stretch."),
        ),
        listOf(Sources.EXRX_CB_LYING_FLY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "Incline bench cable fly", listOf("Incline cable fly on bench", "Cable incline bench fly"),
        listOf(Equipment.CABLE), FLY_MUSCLES, Kind.ACCESSORY,
        FormGuide(
            purpose = "A cable fly on an incline bench between two low pulleys. It works mainly the upper part of your chest.",
            setup = listOf(
                "Set an incline bench between two low pulleys.",
                "Sit with a handle in each hand and lie back on the bench.",
                "Press the handles up over your shoulders until your arms point straight up.",
                "Bend your elbows slightly, pointing them out to the sides.",
                FLY_BRACE,
            ),
            steps = listOf(
                "Lower the handles out to the sides of your shoulders. Keep the bend in your elbows fixed.",
                "When you feel a stretch in your chest or shoulders, bring them back together over your upper chest in a hugging motion.",
            ),
            cues = listOf("Elbows fixed, slightly bent.", "Expect less range than on a flat bench."),
            mistakes = listOf(ELBOWS_CHANGE, "Forcing the same range as a flat fly. Your shoulders have less range on an incline."),
        ),
        listOf(Sources.EXRX_CB_INCLINE_FLY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "Seated cable fly", listOf("Cable seated fly"), listOf(Equipment.CABLE), FLY_MUSCLES, Kind.ACCESSORY,
        FormGuide(
            purpose = "A seated chest fly on a cable machine. It works your chest through a hugging motion.",
            setup = listOf(
                "Sit on the seat and hold a handle on each side.",
                "Bend your elbows slightly.",
                FLY_BRACE,
            ),
            steps = listOf(
                "Keep your elbows high and bring the handles together in a hugging arc. Keep the bend in your elbows fixed.",
                "Let them go back until you feel a slight stretch.",
            ),
            cues = listOf("Elbows high.", "Elbows fixed, slightly bent."),
            mistakes = listOf(ELBOWS_CHANGE, "Letting the cables pull your arms back past a slight stretch."),
        ),
        listOf(Sources.EXRX_CB_SEATED_FLY, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // ---- Dips ----
    chestEntry(
        "Chest dip", listOf("Parallel bar dip", "Weighted chest dip"),
        listOf(Equipment.BODYWEIGHT), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A bodyweight dip on parallel bars. It works your lower chest, front shoulders and triceps.",
            setup = listOf(
                "Grip the bars and push up until your arms are straight.",
                "Bend your knees and hips slightly.",
                BODY_BRACE,
                "To add weight, hang it from a dip belt or hold a dumbbell between your ankles.",
            ),
            steps = listOf(
                "Lower yourself by bending your arms. Let your elbows flare out to the sides.",
                "When you feel a slight stretch in your chest or shoulders, push back up until your arms are straight.",
            ),
            cues = listOf("Stop at a slight stretch.", "Push up to straight arms."),
            mistakes = listOf(
                "Going deeper than a slight stretch in your chest or shoulders.",
                "Doing full dips before you're ready. A machine or a partner can help until you are.",
            ),
        ),
        listOf(Sources.EXRX_CHEST_DIP, Sources.EXRX_WEIGHTED_CHEST_DIP, Sources.ACE_BRACE_OR_BREATHE),
    ),
    chestEntry(
        "Assisted dip", listOf("Machine-assisted dip", "Assisted chest dip"), listOf(Equipment.MACHINE), PRESS_MUSCLES,
        Kind.COMPOUND,
        FormGuide(
            purpose = "A chest dip on a machine that takes some of your bodyweight. It builds up to full dips.",
            setup = listOf(
                "Pick the assist weight. It is taken off your bodyweight, so more weight makes the dip easier.",
                "Grip the bars with your arms straight and your shoulders over your hands.",
                "Step down onto the assist lever with your hips and knees bent. On some machines you kneel on a pad instead.",
                BODY_BRACE,
            ),
            steps = listOf(
                "Lower yourself by bending your arms. Let your elbows flare out to the sides.",
                "When you feel a slight stretch in your chest or shoulders, push back up until your arms are straight.",
            ),
            cues = listOf("Shoulders over your hands at the top.", "Stop at a slight stretch."),
            mistakes = listOf(
                "Going deeper than a slight stretch in your chest or shoulders.",
                "Losing the position with your shoulders over your hands.",
            ),
        ),
        listOf(
            Sources.EXRX_ASSISTED_CHEST_DIP, Sources.EXRX_ASSISTED_CHEST_DIP_KNEELING, Sources.EXRX_ACTUAL_RESISTANCE,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),

    // ---- Push-ups ----
    chestEntry(
        "Push-up", listOf("Pushup", "Press-up"), listOf(Equipment.BODYWEIGHT), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A bodyweight press from the floor. It works your chest, front shoulders and triceps while your trunk holds you straight.",
            setup = listOf(
                "Place your hands on the floor about shoulder-width apart, or slightly wider.",
                "Push up onto your toes with straight arms, so your body is in a straight line.",
                PUSH_UP_BRACE,
                "If full push-ups are too hard, put your hands on a raised surface or do them on your knees.",
            ),
            steps = listOf(
                "Bend your elbows to lower your chest toward the floor. Keep your body straight.",
                "Push the floor away until your elbows are straight.",
                "Repeat for all your reps.",
            ),
            cues = listOf(BODY_STRAIGHT, "Push the floor away."),
            mistakes = listOf("Letting your lower back sag.", "Lifting your hips."),
        ),
        listOf(Sources.EXRX_PUSH_UP, Sources.ACE_PUSH_UP),
    ),
    chestEntry(
        "Incline push-up", listOf("Incline pushup", "Hands-elevated push-up"), listOf(Equipment.BODYWEIGHT), PRESS_MUSCLES,
        Kind.COMPOUND,
        FormGuide(
            purpose = "A push-up with your hands on a bench or platform. It is easier than a floor push-up, and a higher surface makes it easier still.",
            setup = listOf(
                "Place your hands on the edge of a bench or platform, slightly wider than your shoulders.",
                "Walk your feet back until your arms and body are straight.",
                PUSH_UP_BRACE,
            ),
            steps = listOf(
                "Bend your arms to lower your chest to the edge.",
                "Push back up until your arms are straight.",
            ),
            cues = listOf(BODY_STRAIGHT, "Chest to the edge."),
            mistakes = listOf("Bending at the hips.", "Stopping short of the edge."),
        ),
        listOf(Sources.EXRX_INCLINE_PUSH_UP, Sources.ACE_PUSH_UP),
    ),
    chestEntry(
        "Decline push-up", listOf("Decline pushup", "Feet-elevated push-up"), listOf(Equipment.BODYWEIGHT), PRESS_MUSCLES,
        Kind.COMPOUND,
        FormGuide(
            purpose = "A push-up with your feet raised on a bench. It is harder than a floor push-up and works more of your upper chest.",
            setup = listOf(
                "Kneel on the floor with a bench behind you.",
                "Place your hands on the floor slightly wider than your shoulders.",
                "Put your feet up on the bench and straighten your arms, with your body straight.",
                PUSH_UP_BRACE,
            ),
            steps = listOf(
                "Bend your arms to lower your upper body. Keep your body straight.",
                "Tip your head back slightly, so you can go all the way down without arching your back.",
                "Push up until your arms are straight.",
            ),
            cues = listOf(BODY_STRAIGHT, "A higher bench makes it harder."),
            mistakes = listOf("Arching your back to get lower.", "Starting with a bench that is too high."),
        ),
        listOf(Sources.EXRX_DECLINE_PUSH_UP, Sources.ACE_PUSH_UP),
    ),
    chestEntry(
        "Knee push-up", listOf("Kneeling push-up", "Bent-knee push-up", "Push-up on knees"),
        listOf(Equipment.BODYWEIGHT), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A push-up done from your knees. It is a lighter way to build the strength for full push-ups.",
            setup = listOf(
                "Kneel on a mat. Place your hands on the floor slightly wider than your shoulders.",
                "Push up on straight arms, so your body is straight from your knees to your head.",
                "Brace your trunk and keep your back flat.",
                "To take stress off your wrists, you can hold dumbbell handles instead of putting your hands flat.",
            ),
            steps = listOf(
                "Bend your arms to lower your chest toward the floor. Keep your body straight.",
                "Push back up until your arms are straight.",
            ),
            cues = listOf("Straight line from knees to head.", "Back flat."),
            mistakes = listOf("Bending at the hips.", "Letting your back sag."),
        ),
        listOf(Sources.EXRX_KNEE_PUSH_UP, Sources.ACE_BENT_KNEE_PUSH_UP),
    ),
    chestEntry(
        "Archer push-up", emptyList(), listOf(Equipment.BODYWEIGHT), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A push-up with one arm out to the side, so the other arm does most of the work. It is a step toward one-arm push-ups.",
            setup = listOf(
                "Get into a push-up position on your toes.",
                "Place one hand near your ribs, with the other arm reaching out to the side at arm's length.",
                "Turn your head toward the straight arm.",
                PUSH_UP_BRACE,
            ),
            steps = listOf(
                "Press up with the arm near your body until it is straight. Keep the other arm and your body fairly straight.",
                "Lower back down.",
                "To work the other side, turn your head and shift your body toward the other arm.",
            ),
            cues = listOf("Body fairly straight.", "The bent arm does the work."),
            mistakes = listOf("Twisting more than a little.", "Trying it before standard push-ups are easy."),
        ),
        listOf(Sources.EXRX_ARCHER_PUSH_UP, Sources.ACE_PUSH_UP),
    ),
    chestEntry(
        "Clap push-up", listOf("Clapping push-up", "Plyo push-up"), listOf(Equipment.BODYWEIGHT), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A fast push-up where you push off the floor and clap. It trains upper-body power.",
            setup = listOf(
                "Build a solid strength base with standard push-ups first.",
                "Get into a push-up position, hands shoulder-width or slightly wider, arms and body straight.",
                PUSH_UP_BRACE,
                "To make it easier, do it from your knees.",
            ),
            steps = listOf(
                "Lower your chest toward the floor.",
                "Push up as fast as you can, so your hands leave the floor.",
                "Clap quickly, then put your hands back in place and catch yourself.",
            ),
            cues = listOf("Hips and waist straight.", "Catch yourself, then go again."),
            mistakes = listOf("Trying it before you have a solid strength base.", HIPS_SAG),
        ),
        listOf(Sources.EXRX_CLAP_PUSH_UP, Sources.ACE_PUSH_UP),
    ),
    chestEntry(
        "Weighted push-up", emptyList(), listOf(Equipment.BODYWEIGHT), PRESS_MUSCLES, Kind.COMPOUND,
        FormGuide(
            purpose = "A push-up with weight plates on your back. The extra weight makes it harder than a bodyweight push-up.",
            setup = listOf(
                "Get into a push-up position, hands slightly wider than your shoulders, body straight.",
                "A partner places weight plates on your back.",
                PUSH_UP_BRACE,
            ),
            steps = listOf(
                "Bend your elbows to lower your chest. Keep your body rigid.",
                "Push up until your arms are straight.",
                "Your partner takes the plates off at the end of the set.",
            ),
            cues = listOf("Body rigid.", "Your partner keeps the plates from sliding."),
            mistakes = listOf("Letting the plates slide off your back.", HIPS_SAG),
        ),
        listOf(Sources.EXRX_WEIGHTED_PUSH_UP, Sources.EXRX_WEIGHTED_PUSH_UP_HANDLES, Sources.ACE_PUSH_UP),
    ),
)
