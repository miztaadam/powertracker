package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

/** BodyArea.ARMS entries. */
internal val arms: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "JM press",
        aliases = listOf("Blakely press"),
        area = BodyArea.ARMS,
        equipment = listOf(Equipment.BARBELL),
        muscles = listOf("Triceps"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A mix of a close-grip bench press and a triceps extension. It builds triceps strength for the bench lockout.",
            setup = listOf(
                "Set the safety arms just below the height of your chest, or use a spotter.",
                "Lie on the bench with your feet flat on the floor.",
                "Grip the bar with your hands about 40 cm apart.",
                "Breathe in and brace before you lift the bar out.",
            ),
            steps = listOf(
                "Lift the bar out and hold it over your shoulders with straight arms.",
                "Lower the bar toward your upper chest, keeping your elbows high.",
                "Stop when your forearms press against your upper arms.",
                "Press back up until your elbows lock.",
                "Put the bar back in the rack after the last rep.",
            ),
            cues = listOf("Elbows stay high.", "Bar between your chin and upper chest."),
            mistakes = listOf(
                "Letting your elbows drop, so it turns into a close-grip bench.",
                "Adding weight before the movement feels smooth.",
            ),
        ),
        sources = listOf(Sources.EXRX_JM_PRESS),
    ),
    LibraryExercise(
        name = "Curl",
        aliases = listOf("Biceps curl", "Barbell curl"),
        area = BodyArea.ARMS,
        equipment = listOf(Equipment.BARBELL, Equipment.DUMBBELL),
        muscles = listOf("Biceps"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A curl for the front of your upper arm. It mainly works your biceps.",
            setup = listOf(
                "Stand tall holding a barbell, or a dumbbell in each hand, with an underhand grip about shoulder-width apart.",
                "Line up your wrists, elbows and shoulders, and keep your elbows at your sides.",
                "Brace your trunk so your body stays still.",
            ),
            steps = listOf(
                "Bend your elbows to curl the weight up until your forearms are upright.",
                "Lower it slowly until your arms are straight.",
            ),
            cues = listOf("Elbows at your sides.", "Chest still.", "Lower slowly."),
            mistakes = listOf(
                "Swinging your body to lift the weight.",
                "Letting your elbows drift far forward.",
                "Dropping the weight on the way down.",
            ),
        ),
        sources = listOf(Sources.EXRX_BB_CURL, Sources.ACE_BICEP_CURL),
    ),
)
