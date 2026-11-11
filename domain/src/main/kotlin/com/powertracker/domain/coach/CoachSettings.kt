package com.powertracker.domain.coach

data class CoachSettings(
    val defaultRpe: Double = 8.0,
    val changePlates: Boolean = false,
    val stallWeeks: Int = 3,
    val deadliftStallWeeks: Int = 4,
)
