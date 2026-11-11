package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

/** BodyArea.CHEST entries. */
internal val chest: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "DB press",
        aliases = listOf("Dumbbell bench press", "DB bench press"),
        area = BodyArea.CHEST,
        equipment = listOf(Equipment.DUMBBELL),
        muscles = listOf("Pecs", "Front delts"),
        kind = Kind.ACCESSORY,
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
)
