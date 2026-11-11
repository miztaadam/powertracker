package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

/** BodyArea.BACK entries. */
internal val back: List<LibraryExercise> = listOf(
    LibraryExercise(
        name = "Row",
        aliases = listOf("Bent-over row", "Barbell row"),
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
        aliases = emptyList(),
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
)
