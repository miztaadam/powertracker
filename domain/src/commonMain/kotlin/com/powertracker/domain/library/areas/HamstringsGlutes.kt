package com.powertracker.domain.library.areas

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.FormGuide
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.Sources
import com.powertracker.domain.model.Category

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
)
