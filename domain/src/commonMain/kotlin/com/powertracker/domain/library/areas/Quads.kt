package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

/** BodyArea.QUADS entries. */
internal val quads: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "Bulgarian split squat",
        aliases = listOf("BSS", "Rear-foot-elevated split squat", "RFESS"),
        area = BodyArea.QUADS,
        equipment = listOf(Equipment.DUMBBELL, Equipment.BARBELL),
        muscles = listOf("Quads", "Glutes"),
        kind = Kind.ACCESSORY,
        category = Category.ACCESSORY,
        parentLift = null,
        guide = FormGuide(
            purpose = "A single-leg squat with your rear foot up on a bench. It trains each leg on its own, mostly the quads and glutes.",
            setup = listOf(
                "Hold a dumbbell in each hand at your sides. With a barbell, take it from a rack with the safety pins set, or use a spotter.",
                "Stand facing away from a bench.",
                "Reach one leg back and rest the top of that foot on the bench.",
                "Brace your trunk.",
            ),
            steps = listOf(
                "Bend your front knee and hip to lower yourself.",
                "Go down until your back knee is nearly touching the floor.",
                "Drive up through your front foot.",
                "Do all your reps on one leg, then switch.",
            ),
            cues = listOf("Front knee points the same way as your foot.", "Torso upright."),
            mistakes = listOf(
                "Letting your front knee cave in.",
                "Leaning far forward.",
                "Dropping fast so your back knee hits the floor.",
            ),
        ),
        sources = listOf(Sources.EXRX_DB_SINGLE_LEG_SPLIT_SQUAT, Sources.PMC_BULGARIAN_SPLIT_SQUAT),
    ),
)
