package com.powertracker.domain.model

import kotlinx.serialization.Serializable

/** Which side of the body was worked first or logged in a single-sided set. */
@Serializable
enum class Side { RIGHT, LEFT }

/** One half (right or left) of a single-sided set. */
@Serializable
data class SideLog(
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val failed: Boolean,
)

/** Both halves of a single-sided set, stored alongside the set's weaker-side summary. */
@Serializable
data class SplitSides(
    val right: SideLog,
    val left: SideLog,
)
