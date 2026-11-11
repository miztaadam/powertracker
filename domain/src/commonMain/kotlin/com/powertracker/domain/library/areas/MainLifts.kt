package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.SourceId
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift

// Shared lines, so the bracing and set-up wording is the same in every main-lift guide. RACK_SAFETY is in Shared.kt.
private const val SQUAT_RACK = "Set the bar in the rack just below shoulder height."
private const val BAR_ON_BACK = "Step under the bar and rest it across your upper back."
private const val SQUAT_STANCE = "Stand with your feet a little wider than your shoulders."
private const val OWN_STANCE = "Set your feet in your normal squat stance."
private const val BRACE = "Take a big breath into your belly and brace your trunk before each rep."
private const val BENCH_LIE = "Lie on the bench with your head, shoulders and buttocks on the bench. Plant your feet flat on the floor."
private const val BENCH_GRIP = "Grip the bar a little wider than your shoulders."
private const val BENCH_BACK = "Keep a slight arch in your back."
private const val UNRACK = "Lift the bar out and hold it over your shoulders with straight arms."
private const val LOCKOUT = "Press the bar up until your elbows lock."
private const val PULL_STANCE = "Stand with your feet flat under the bar, about shoulder-width apart, with your shins close to it."
private const val PULL_GRIP = "Bend down and grip the bar shoulder-width or a little wider. Use both palms down, or one up and one down."
private const val PULL_CHEST = "Pull your shoulders down and back, and lift your chest."
private const val PULL_BRACE = "Brace your trunk before you pull."

private fun mainLift(
    name: String,
    aliases: List<String>,
    category: Category,
    muscles: List<String>,
    guide: FormGuide,
    sources: List<SourceId>,
) = LibraryExercise(
    name, aliases, BodyArea.MAIN_LIFTS, listOf(Equipment.BARBELL), muscles, Kind.COMPOUND, category,
    parentLift = null, guide = guide, sources = sources,
)

private fun variation(
    name: String,
    aliases: List<String>,
    parent: MainLift,
    muscles: List<String>,
    guide: FormGuide,
    sources: List<SourceId>,
    equipment: List<Equipment> = listOf(Equipment.BARBELL),
) = LibraryExercise(
    name, aliases, BodyArea.MAIN_LIFTS, equipment, muscles, Kind.COMPOUND, Category.VARIATION,
    parentLift = parent, guide = guide, sources = sources,
)

/** BodyArea.MAIN_LIFTS: squat, bench, deadlift and strict press, and the coach's heavy variations of the first three. */
internal val mainLifts: List<LibraryExercise> = listOf(
    mainLift(
        "Squat", listOf("Back squat", "Barbell back squat"), Category.SQUAT, listOf("Quads", "Glutes"),
        FormGuide(
            purpose = "The competition squat. It builds strength in your legs and hips.",
            setup = listOf(
                SQUAT_RACK,
                RACK_SAFETY,
                BAR_ON_BACK,
                "Grip the bar wider than your shoulders. Stand up to lift it off the hooks, then step back.",
                SQUAT_STANCE,
                BRACE,
            ),
            steps = listOf(
                "Bend your hips and knees to lower yourself.",
                "Keep your chest up and your back flat.",
                "Go down until the crease of your hip is below the top of your knee. This is competition depth.",
                "Drive up by straightening your hips and knees together.",
                "Stand tall with your knees locked. Breathe, then brace again for the next rep.",
                "After the last rep, walk the bar forward into the hooks.",
            ),
            cues = listOf("Knees track over your feet.", "Keep your whole foot on the floor.", "Chest up."),
            mistakes = listOf(
                "Heels lifting off the floor.",
                "Knees caving in as you stand up.",
                "Losing your brace and letting your back round.",
                "Cutting depth on heavy sets.",
            ),
        ),
        listOf(Sources.IPF_RULES_2026, Sources.ACE_BACK_SQUAT, Sources.EXRX_BB_SQUAT, Sources.PMC_BACK_SQUAT_ASSESSMENT),
    ),
    mainLift(
        "Bench", listOf("Bench press", "Barbell bench press"), Category.BENCH, listOf("Pecs", "Triceps", "Front delts"),
        FormGuide(
            purpose = "The competition bench press. It builds pressing strength in your chest, shoulders and triceps.",
            setup = listOf(RACK_SAFETY, BENCH_LIE, BENCH_GRIP, BENCH_BACK, BENCH_BRACE),
            steps = listOf(
                UNRACK,
                "Lower the bar under control to your chest.",
                "Pause with the bar still on your chest.",
                LOCKOUT,
                RACK_IT,
            ),
            cues = listOf("Feet stay planted.", "Head, shoulders and buttocks stay on the bench.", "Pause on your chest before you press."),
            mistakes = listOf(
                "Bouncing the bar off your chest.",
                "Lifting your buttocks off the bench.",
                "Gripping so wide that the range of motion gets short.",
            ),
        ),
        listOf(Sources.IPF_RULES_2026, Sources.EXRX_BB_BENCH, Sources.EXRX_BB_BENCH_POWER, Sources.EXRX_BENCH_ANALYSES),
    ),
    mainLift(
        "Deadlift", listOf("Conventional deadlift", "Barbell deadlift"), Category.DEADLIFT,
        listOf("Hamstrings", "Glutes", "Lats"),
        FormGuide(
            purpose = "The competition deadlift. It builds strength in your hips, legs and back.",
            setup = listOf(PULL_STANCE, PULL_GRIP, PULL_CHEST, PULL_BRACE),
            steps = listOf(
                "Push through your heels to start the lift.",
                "Keep the bar close to your legs as it rises. Raise it at the same rate as your hips.",
                "Stand tall with your knees locked and your shoulders back.",
                "Lower the bar under control by pushing your hips back and bending your knees.",
                "Let the bar settle on the floor before the next rep.",
            ),
            cues = listOf("Keep the bar close.", "Lift with your hips, not just your back.", "Keep your back flat."),
            mistakes = listOf(
                "Rounding your back.",
                "Lifting with your back instead of your hips.",
                "Letting the bar drift away from your legs.",
            ),
        ),
        listOf(Sources.IPF_RULES_2026, Sources.EXRX_BB_DEADLIFT, Sources.ACE_DEADLIFT, Sources.ACE_HOW_TO_DEADLIFT, Sources.ACE_BRACE_OR_BREATHE),
    ),

    // The strict press: the catalogue's "Overhead press" until it became the fourth main lift, so that name is an alias.
    mainLift(
        "Strict press",
        listOf(
            "Overhead press", "OHP", "Military press", "Standing press", "Barbell overhead press", "Standing barbell press",
            "Barbell military press",
        ),
        Category.STRICT_PRESS, listOf("Front delts", "Side delts", "Triceps"),
        FormGuide(
            purpose = "The strict press: a standing barbell press from your shoulders to overhead. It works your shoulders and triceps.",
            setup = listOf(
                "Set the bar in the rack at about shoulder height.",
                RACK_SAFETY,
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
            mistakes = listOf("Leaning back instead of keeping your back straight and tall.", BEHIND_NECK),
        ),
        listOf(
            Sources.EXRX_BB_MILITARY_PRESS, Sources.ACE_STANDING_SHOULDER_PRESS, Sources.NSCA_STANDARDS_SUMMARY,
            Sources.PMC_UPPER_EXTREMITY_LIFTING_INJURIES, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),

    // ---- Squat variations ----
    variation(
        "Pause squat", listOf("Paused squat"), MainLift.SQUAT, listOf("Quads", "Glutes"),
        FormGuide(
            purpose = "A back squat with a still pause at the bottom. It builds strength out of the bottom without a bounce.",
            setup = listOf(SQUAT_RACK, RACK_SAFETY, BAR_ON_BACK, SQUAT_STANCE, BRACE),
            steps = listOf(
                "Squat down under control to your normal depth.",
                "Hold still at the bottom for a short pause. Stay braced.",
                "Drive up by straightening your hips and knees together.",
                "Stand tall, breathe, and brace again for the next rep.",
            ),
            cues = listOf("Stay braced in the pause.", "Knees track over your feet.", "Drive up hard from a dead stop."),
            mistakes = listOf("Relaxing at the bottom.", "Bouncing out instead of pausing.", "Heels lifting off the floor."),
        ),
        listOf(Sources.EXRX_BB_SQUAT, Sources.ACE_BACK_SQUAT, Sources.PMC_STICKING_POINT_REVIEW),
    ),
    variation(
        "Front squat", listOf("Barbell front squat"), MainLift.SQUAT, listOf("Quads", "Glutes", "Upper back", "Trunk"),
        FormGuide(
            purpose = "A squat with the bar on the front of your shoulders. It puts more of the work on your quads and needs an upright trunk.",
            setup = listOf(
                "Set the bar in the rack at upper-chest height.",
                RACK_SAFETY,
                "Grip the bar about shoulder-width apart and step under it.",
                "Rest the bar across the front of your shoulders, palms facing up and elbows pointing straight ahead.",
                OWN_STANCE,
                BRACE,
            ),
            steps = listOf(
                "Lift the bar out and step back.",
                "Squat down, keeping your torso upright and your back straight.",
                "Go down until your thighs are level with the floor. Stop sooner if your heels lift or your back starts to round.",
                "Drive up by straightening your hips and knees.",
                RACK_IT,
            ),
            cues = listOf("Elbows point straight ahead.", "Weight over your whole foot.", "Knees in line with your toes."),
            mistakes = listOf(
                "Keeping your shins too upright, so your torso tips forward.",
                "Your back starting to round.",
                "Heels lifting off the floor.",
            ),
        ),
        listOf(Sources.EXRX_BB_FRONT_SQUAT, Sources.ACE_FRONT_SQUAT, Sources.ACE_FRONT_SQUAT_TO_PRESS),
    ),
    variation(
        "Pin squat", listOf("Squat from pins"), MainLift.SQUAT, listOf("Upper back", "Trunk"),
        FormGuide(
            purpose = "A squat that starts from a dead stop on the safety pins. It builds strength at the point where your squat slows down.",
            setup = listOf(
                "Set the safety pins in the rack at the height you want to start from, often just below where your squat slows down.",
                "Rest the bar on the pins. The pins also catch the bar if you can't stand up.",
                "Get under the bar and rest it across your upper back.",
                SQUAT_STANCE,
                BRACE,
            ),
            steps = listOf(
                "Drive the bar up off the pins by straightening your hips and knees together.",
                "Stand tall with your knees locked.",
                "Squat back down under control until the bar rests on the pins.",
                "Let the bar settle, brace again, then drive up for the next rep.",
            ),
            cues = listOf("Brace before the bar leaves the pins.", "Chest up.", "Knees track over your feet."),
            mistakes = listOf(
                "Relaxing on the pins and losing your brace.",
                "Dropping the bar onto the pins.",
                "Starting with your back rounded.",
            ),
        ),
        listOf(Sources.EXRX_BB_SQUAT, Sources.ACE_BACK_SQUAT, Sources.PMC_STICKING_POINT_REVIEW),
    ),
    variation(
        "Safety-bar squat", listOf("SSB squat", "Safety squat bar squat"), MainLift.SQUAT, listOf("Upper back", "Trunk"),
        FormGuide(
            purpose = "A squat with a safety squat bar, which has a padded yoke and handles. It keeps your torso more upright than a high-bar squat.",
            setup = listOf(
                SQUAT_RACK,
                RACK_SAFETY,
                "Step under the bar and rest the pads on your upper back and shoulders.",
                "Hold the handles in front of you.",
                OWN_STANCE,
                BRACE,
            ),
            steps = listOf(
                "Lift the bar out and take a few short steps back.",
                "Squat down until your thighs are just below level with the floor.",
                "Drive up by straightening your hips and knees together.",
                RACK_IT,
            ),
            cues = listOf("Knees track over your feet.", "Stay upright."),
            mistakes = listOf("Knees caving in.", "Losing your brace at the bottom.", "Cutting depth short."),
        ),
        listOf(Sources.EXRX_SAFETY_BAR_SQUAT, Sources.PMC_SAFETY_BAR_SQUAT),
        equipment = listOf(Equipment.SPECIALTY_BAR),
    ),
    variation(
        "Box squat", listOf("Barbell box squat"), MainLift.SQUAT, listOf("Glutes", "Hips"),
        FormGuide(
            purpose = "A squat to a box set at your target depth. It teaches you to sit back and stand up from a still start.",
            setup = listOf(
                SQUAT_RACK,
                RACK_SAFETY,
                "Place a sturdy box behind you at the height of your target depth.",
                BAR_ON_BACK,
                "Lift the bar out and step back until the box is just behind you.",
                OWN_STANCE,
                BRACE,
            ),
            steps = listOf(
                "Sit back by bending your hips and knees. Keep your shins close to upright.",
                "Sit on the box under control. Don't drop onto it.",
                "Pause briefly on the box while staying braced.",
                "Drive up from the box to standing.",
            ),
            cues = listOf("Sit back, not down.", "Stay braced on the box.", "Shins close to upright."),
            mistakes = listOf("Dropping onto the box.", "Relaxing on the box and losing your brace."),
        ),
        listOf(Sources.EXRX_BB_BOX_SQUAT),
    ),
    variation(
        "High-bar squat", emptyList(), MainLift.SQUAT, listOf("Quads", "Glutes"),
        FormGuide(
            purpose = "A back squat with the bar sitting higher on your back than in a low-bar squat. It usually keeps your torso more upright.",
            setup = listOf(
                SQUAT_RACK,
                RACK_SAFETY,
                "Step under the bar and rest it high on your upper back, on the top of the muscle just below your neck.",
                "Grip the bar out to the sides. Stand up to lift it off the hooks, then step back.",
                OWN_STANCE,
                BRACE,
            ),
            steps = listOf(
                "Push your hips back and let your knees bend forward. Keep your back straight and your torso upright.",
                // ExRx BBSquat gives the depth; the stop-sooner rule is ACE's bodyweight squat limit, carried over.
                "Go down until your thighs are just past level with the floor. Stop sooner if your heels lift or your back starts to round.",
                "Drive up by straightening your hips and knees together.",
                RACK_IT,
            ),
            cues = listOf("Knees point the same way as your feet.", "Weight over your whole foot.", "Chest up."),
            mistakes = listOf("Heels lifting off the floor.", "Knees caving in.", "Losing your brace and letting your back round."),
        ),
        listOf(
            Sources.EXRX_BB_SQUAT, Sources.EXRX_SQUAT_VARIATIONS, Sources.PMC_SAFETY_BAR_SQUAT,
            Sources.PMC_HIGH_LOW_BAR_ACTIVATION, Sources.ACE_BODYWEIGHT_SQUAT,
        ),
    ),
    variation(
        "Tempo squat", listOf("Tempo back squat", "Slow-eccentric squat"), MainLift.SQUAT, listOf("Quads", "Glutes"),
        FormGuide(
            purpose = "A back squat where you lower yourself slowly and stand up at normal speed. It builds control on the way down, and one study found a slower way down may help quad growth.",
            setup = listOf(SQUAT_RACK, RACK_SAFETY, BAR_ON_BACK, OWN_STANCE, BRACE),
            steps = listOf(
                "Lift the bar out and step back.",
                // PMC4262933 suggests a descent at least twice as long as the ascent; only one source gives a ratio, so no number.
                "Lower yourself slowly and under control. Take longer to go down than to come up.",
                "Go down to your normal squat depth.",
                "Drive up by straightening your hips and knees together.",
                RACK_IT,
            ),
            cues = listOf("Slow on the way down.", "Stay braced the whole way down.", "Knees point the same way as your feet."),
            mistakes = listOf(
                "Speeding up or dropping near the bottom.",
                "Losing your brace on the slow way down.",
                "Heels lifting off the floor.",
            ),
        ),
        listOf(Sources.PMC_BACK_SQUAT_ASSESSMENT, Sources.PMC_ECCENTRIC_TEMPO_SQUAT, Sources.EXRX_BB_SQUAT, Sources.ACE_BACK_SQUAT),
    ),

    // ---- Bench variations ----
    variation(
        "Spoto press", emptyList(), MainLift.BENCH, listOf("Pecs", "Front delts"),
        FormGuide(
            purpose = "A bench press that stops just above your chest. It builds control and strength near the bottom of the press.",
            setup = listOf(RACK_SAFETY, BENCH_LIE, BENCH_GRIP, BENCH_BACK, BENCH_BRACE),
            steps = listOf(
                UNRACK,
                "Lower the bar under control.",
                "Stop it just above your chest, without touching. Hold it still for a moment.",
                LOCKOUT,
                RACK_IT,
            ),
            cues = listOf("Stop the bar, don't let it sink.", "Stay braced in the pause.", "Feet stay planted."),
            mistakes = listOf("Touching your chest.", "Bouncing out of the pause.", "Losing your upper-back position."),
        ),
        listOf(Sources.EXRX_BB_BENCH, Sources.EXRX_BENCH_ANALYSES, Sources.EXRX_BB_BENCH_POWER),
    ),
    variation(
        "Long-pause bench", listOf("Long-pause bench press"), MainLift.BENCH, listOf("Pecs", "Front delts"),
        FormGuide(
            purpose = "A bench press with a longer pause on your chest than in competition. It builds strength off the chest with no help from a bounce.",
            setup = listOf(RACK_SAFETY, BENCH_LIE, BENCH_GRIP, BENCH_BACK, BENCH_BRACE),
            steps = listOf(
                UNRACK,
                "Lower the bar under control to your chest.",
                "Hold it still on your chest for a few seconds, longer than a competition pause.",
                LOCKOUT,
                RACK_IT,
            ),
            cues = listOf("Stay braced in the pause.", "Keep the bar still.", "Feet stay planted."),
            mistakes = listOf(
                "Letting the bar sink into your chest during the pause.",
                "Relaxing during the pause.",
                "Lifting your buttocks off the bench to start the press.",
            ),
        ),
        listOf(Sources.IPF_RULES_2026, Sources.EXRX_BENCH_ANALYSES, Sources.EXRX_BB_BENCH_POWER),
    ),
    variation(
        "Pin press", listOf("Bench pin press"), MainLift.BENCH, listOf("Pecs", "Triceps"),
        FormGuide(
            purpose = "A bench press from a dead stop on the safety pins. It builds strength at the point where your press slows down.",
            setup = listOf(
                "Set a bench inside a power rack. The bar starts on the hooks.",
                "Set the safety pins at the height you want to press from, often where your bench slows down. The pins are the bottom stop, and they catch the bar if you miss.",
                BENCH_LIE,
                BENCH_GRIP,
                BENCH_BACK,
                BENCH_BRACE,
            ),
            steps = listOf(
                "Lift the bar off the hooks and hold it over your shoulders with straight arms.",
                "Lower the bar under control until it rests on the pins.",
                "Let it settle for a moment. Stay braced.",
                LOCKOUT,
                RACK_IT,
            ),
            cues = listOf("Lower softly onto the pins.", "Stay braced on the pins.", "Feet stay planted."),
            mistakes = listOf(
                "Dropping the bar onto the pins.",
                "Relaxing on the pins.",
                "Losing your feet or upper-back position.",
            ),
        ),
        listOf(Sources.PMC_STICKING_POINT_REVIEW, Sources.PMC_PRESS_STICKING_REGION, Sources.EXRX_BB_BENCH, Sources.EXRX_BB_BENCH_POWER),
    ),
    variation(
        "Larsen press", emptyList(), MainLift.BENCH, listOf("Pecs", "Triceps"),
        FormGuide(
            purpose = "A bench press with your feet off the floor. With no help from your legs, your upper body does more of the work.",
            setup = listOf(
                RACK_SAFETY,
                "Lie on the bench with your head, shoulders and buttocks on the bench.",
                BENCH_GRIP,
                BENCH_BACK,
                "Lift your feet off the floor and hold your legs still.",
                BENCH_BRACE,
            ),
            steps = listOf(
                UNRACK,
                "Lower the bar under control to your chest.",
                LOCKOUT,
                RACK_IT,
            ),
            cues = listOf("Legs still.", "Shoulders and buttocks stay on the bench.", "Lower slowly, press hard."),
            mistakes = listOf("Twisting or shifting on the bench.", "Lifting your buttocks off the bench."),
        ),
        listOf(Sources.EXRX_BB_BENCH, Sources.PMC_BENCH_WITHOUT_LEGS, Sources.EXRX_BB_BENCH_POWER),
    ),
    variation(
        "Close-grip bench", listOf("Close-grip bench press", "CGBP"), MainLift.BENCH, listOf("Triceps"),
        FormGuide(
            purpose = "A bench press with a narrower grip. It puts more of the work on your triceps.",
            setup = listOf(
                RACK_SAFETY,
                BENCH_LIE,
                "Grip the bar with your hands about in line with your shoulders, or a little narrower. Not too close.",
                BENCH_BRACE,
            ),
            steps = listOf(
                UNRACK,
                "Lower the bar to your chest, keeping your elbows close to your ribs.",
                "Push your feet into the floor and press the bar back up until your elbows lock.",
                RACK_IT,
            ),
            cues = listOf("Elbows close to your ribs.", "Feet push into the floor.", "Hips stay on the bench."),
            mistakes = listOf("Gripping too close, which makes the bar less stable.", "Flaring your elbows out wide."),
        ),
        listOf(Sources.ACE_CLOSE_GRIP_BENCH, Sources.EXRX_CLOSE_GRIP_BENCH),
    ),
    variation(
        "Board press", listOf("Bench board press"), MainLift.BENCH, listOf("Triceps"),
        FormGuide(
            purpose = "A bench press to a board on your chest. It shortens the range and builds strength in the middle of the press.",
            setup = listOf(
                RACK_SAFETY,
                BENCH_LIE,
                BENCH_GRIP,
                "A partner holds the board on your lower chest, or you strap it to your body. A thicker board shortens the range more.",
                BENCH_BRACE,
            ),
            steps = listOf(
                UNRACK,
                "Lower the bar under control until it touches the board.",
                LOCKOUT,
                RACK_IT,
            ),
            cues = listOf("Lower under control to the board.", "Stay braced at the board.", "Feet stay planted."),
            mistakes = listOf("Bouncing the bar off the board.", "Letting the board slip.", "Relaxing on the board."),
        ),
        listOf(Sources.EXRX_BOARD_PRESS, Sources.PMC_STICKING_POINT_REVIEW, Sources.EXRX_BB_BENCH_POWER),
    ),

    variation(
        "Barbell floor press", listOf("Floor press"), MainLift.BENCH, listOf("Pecs", "Triceps"),
        FormGuide(
            purpose = "A barbell press lying on the floor inside a power rack. Your upper arms touch the floor at the bottom, which is said to take away the stretch and rebound of a normal bench.",
            setup = listOf(
                "Lie on the floor inside a power rack, under the bar, with your knees bent.",
                RACK_SAFETY,
                "Grip the bar overhand, a little wider than your shoulders.",
                BENCH_BRACE,
            ),
            steps = listOf(
                "Lift the bar out of the rack and hold it over your chest with straight arms.",
                "Lower the bar until your upper arms touch the floor.",
                "Pause there for a moment.",
                "Press the bar up until your arms are straight.",
                RACK_IT,
            ),
            cues = listOf("Upper arms touch, then pause.", "Press from a still start."),
            mistakes = listOf("Bouncing your arms off the floor.", "Pressing before your upper arms touch the floor."),
        ),
        listOf(Sources.EXRX_BB_FLOOR_PRESS, Sources.NSCA_STANDARDS_SUMMARY, Sources.ACE_BRACE_OR_BREATHE),
    ),
    variation(
        "Wide-grip bench", listOf("Wide-grip bench press"), MainLift.BENCH, listOf("Pecs", "Front delts"),
        FormGuide(
            purpose = "A bench press with a wider grip than your normal one. It shortens the range of motion and can put more of the work on your chest.",
            setup = listOf(
                RACK_SAFETY,
                BENCH_LIE,
                "Grip the bar wider than your normal bench grip, but no wider than you can control.",
                BENCH_BACK,
                BENCH_BRACE,
            ),
            steps = listOf(
                UNRACK,
                "Lower the bar to your chest.",
                LOCKOUT,
                RACK_IT,
            ),
            cues = listOf("Feet stay planted.", "Head, shoulders and buttocks stay on the bench."),
            mistakes = listOf(
                "Gripping so wide that the range of motion gets very short.",
                "Going very wide. Some researchers link very wide grips with a higher risk of shoulder injury. Don't go wider than you can control.",
            ),
        ),
        listOf(
            Sources.EXRX_BB_BENCH, Sources.EXRX_BB_BENCH_POWER, Sources.PMC_GRIP_WIDTH_NOVICE_TRAINED,
            Sources.PMC_GRIP_BENCH_VS_SEATED_PRESS, Sources.NSCA_STANDARDS_SUMMARY, Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),

    // ---- Deadlift variations ----
    variation(
        "Deficit deadlift", listOf("Deficit pull"), MainLift.DEADLIFT, listOf("Quads", "Lats", "Position"),
        FormGuide(
            purpose = "A deadlift standing on a low platform. It builds strength in the first part of the pull from the floor.",
            setup = listOf(
                "Stand on a weight plate or a low, sturdy platform, with the bar over your feet.",
                "Squat down and grip the bar.",
                "Keep your hips low, your shoulders high, and your arms and back straight.",
                PULL_BRACE,
            ),
            steps = listOf(
                "Stand up by straightening your hips and knees. Keep the bar close to your legs.",
                "Stand tall with your knees locked.",
                "Lower the bar by pushing your hips back, then let it settle on the floor.",
            ),
            cues = listOf("Hips low, shoulders high.", "Back flat.", "Bar close."),
            mistakes = listOf(
                "Rounding your back off the floor.",
                "Using a platform so high that you can't keep your back flat.",
            ),
        ),
        listOf(Sources.EXRX_DEFICIT_DEADLIFT, Sources.EXRX_BB_DEADLIFT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    variation(
        "Paused deadlift", listOf("Pause deadlift"), MainLift.DEADLIFT, listOf("Quads", "Lats", "Position"),
        FormGuide(
            purpose = "A deadlift with a short pause just after the bar leaves the floor. It builds strength and position in the first part of the pull.",
            setup = listOf(PULL_STANCE, PULL_GRIP, PULL_CHEST, PULL_BRACE),
            steps = listOf(
                "Pull the bar off the floor as in your normal deadlift.",
                "Stop just after it leaves the floor and hold still for a moment.",
                "Keep your back flat and the bar close during the pause.",
                "Finish the lift and stand tall with your knees locked.",
                "Lower the bar under control and let it settle on the floor.",
            ),
            cues = listOf("Stay braced in the pause.", "Bar close.", "Back flat."),
            mistakes = listOf(
                "Your back rounding during the pause.",
                "The bar drifting forward during the pause.",
                "Pausing so long that you lose your brace.",
            ),
        ),
        listOf(Sources.EXRX_BB_DEADLIFT, Sources.ACE_DEADLIFT, Sources.PMC_STICKING_POINT_REVIEW, Sources.ACE_BRACE_OR_BREATHE),
    ),
    variation(
        "Paused deadlift at the knee", listOf("Knee-pause deadlift"), MainLift.DEADLIFT, listOf("Hamstrings", "Lats"),
        FormGuide(
            purpose = "A deadlift with a pause just below the knee. It builds strength around the knee, where the deadlift often slows down.",
            setup = listOf(PULL_STANCE, PULL_GRIP, PULL_CHEST, PULL_BRACE),
            steps = listOf(
                "Pull the bar off the floor as in your normal deadlift.",
                "Stop with the bar just below your knees and hold still for a moment.",
                "Keep your back flat and the bar close during the pause.",
                "Finish the lift and stand tall with your knees locked.",
                "Lower the bar under control and let it settle on the floor.",
            ),
            cues = listOf("Stay braced in the pause.", "Bar close to your legs.", "Back flat."),
            mistakes = listOf(
                "Your back rounding during the pause.",
                "Letting the bar drift away from your legs.",
                "Pausing so long that you lose your brace.",
            ),
        ),
        listOf(
            Sources.EXRX_BB_DEADLIFT, Sources.ACE_DEADLIFT, Sources.PMC_STICKING_POINT_THREE_LIFTS, Sources.PMC_STICKING_POINT_REVIEW,
            Sources.ACE_BRACE_OR_BREATHE,
        ),
    ),
    variation(
        "Block pull", listOf("Deadlift off blocks"), MainLift.DEADLIFT, listOf("Glutes", "Upper back"),
        FormGuide(
            purpose = "A deadlift from a bar resting on blocks. It builds strength in the middle of the pull and lets you use more weight.",
            setup = listOf(
                "Set the plates on sturdy blocks, about 5, 10 or 15 cm high.",
                "Take your normal deadlift stance under the bar.",
                "Grip the bar shoulder-width or a little wider, palms down or one up and one down. Chalk or straps can help your grip.",
                "Keep your hips low, your shoulders high, and your arms and back straight.",
                PULL_BRACE,
            ),
            steps = listOf(
                "Stand up by straightening your hips and knees. Keep the bar close to your legs.",
                "Stand tall with your knees locked.",
                "Lower the bar back to the blocks under control.",
            ),
            cues = listOf("Bar close.", "Back straight.", "Knees point the same way as your feet."),
            mistakes = listOf("Rounding your back.", "Letting the bar drift away from your legs."),
        ),
        listOf(Sources.EXRX_BB_BLOCK_PULL, Sources.EXRX_RACK_PULL, Sources.ACE_BRACE_OR_BREATHE),
    ),
    variation(
        "Sumo deadlift", listOf("Sumo", "Sumo pull", "Barbell sumo deadlift"), MainLift.DEADLIFT, listOf("Glutes", "Adductors", "Quads"),
        FormGuide(
            purpose = "A deadlift with a very wide stance and your hands inside your legs. It builds strength in your hips, legs and back.",
            setup = listOf(
                "Stand with your feet very wide and under the bar.",
                "Squat down and grip the bar between your legs, shoulder-width apart, with a mixed grip: one palm up, one down.",
                "Keep your arms straight, your chest high, your hips low and your back straight.",
                PULL_BRACE,
            ),
            steps = listOf(
                "Pull the bar up by driving your feet out to the sides while lifting your chest.",
                "Straighten your knees once the bar passes them.",
                "At the top, stand tall with your shoulders back.",
                "Lower the bar by pushing your hips back with your knees pointing out. Keep your chest high and your back straight.",
            ),
            cues = listOf("Push your feet out, not just down.", "Knees out.", "Bar close."),
            mistakes = listOf(
                "Letting your knees cave in.",
                "Rounding your back.",
                "Gripping narrower than shoulder-width, which makes the lockout harder.",
            ),
        ),
        listOf(Sources.EXRX_BB_SUMO_DEADLIFT, Sources.ACE_BRACE_OR_BREATHE),
    ),
    variation(
        "Trap-bar deadlift", listOf("Hex bar deadlift"), MainLift.DEADLIFT, listOf("Glutes", "Quads", "Adductors"),
        FormGuide(
            purpose = "A deadlift standing inside a trap bar and holding its side handles. It builds strength in your hips and legs.",
            setup = listOf(
                "Stand inside the bar with your feet shoulder-width apart or narrower.",
                "Squat down with your feet flat and grip the handles.",
                "Keep your hips low, your shoulders high, and your arms and back straight.",
                PULL_BRACE,
            ),
            steps = listOf(
                "Stand up by straightening your hips and knees fully.",
                "If your shoulders have rounded, pull them back at the top.",
                "Lower the bar by pushing your hips back and letting your knees bend forward slightly. Keep your back straight.",
            ),
            cues = listOf("Hips low, shoulders high.", "Knees point the same way as your feet.", "Back straight."),
            mistakes = listOf("Rounding your back.", "Letting your knees cave in."),
        ),
        listOf(Sources.EXRX_TB_DEADLIFT, Sources.ACE_BRACE_OR_BREATHE),
        equipment = listOf(Equipment.SPECIALTY_BAR),
    ),
)
