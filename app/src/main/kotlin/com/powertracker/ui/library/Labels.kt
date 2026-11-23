package com.powertracker.ui.library

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment

/** The body area as the picker's chips and the guide sheet name it. */
fun BodyArea.label(): String = when (this) {
    BodyArea.MAIN_LIFTS -> "Main lifts"
    BodyArea.CHEST -> "Chest"
    BodyArea.BACK -> "Back"
    BodyArea.SHOULDERS -> "Shoulders"
    BodyArea.ARMS -> "Arms"
    BodyArea.QUADS -> "Quads"
    BodyArea.HAMSTRINGS_GLUTES -> "Hamstrings & glutes"
    BodyArea.CALVES -> "Calves"
    BodyArea.CORE -> "Core"
}

fun Equipment.label(): String = when (this) {
    Equipment.BARBELL -> "Barbell"
    Equipment.DUMBBELL -> "Dumbbell"
    Equipment.KETTLEBELL -> "Kettlebell"
    Equipment.CABLE -> "Cable"
    Equipment.MACHINE -> "Machine"
    Equipment.BODYWEIGHT -> "Bodyweight"
    Equipment.BAND -> "Band"
    Equipment.SPECIALTY_BAR -> "Specialty bar"
    Equipment.SMITH -> "Smith machine"
}
