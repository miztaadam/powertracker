package com.powertracker.domain.model

import com.powertracker.domain.library.MuscleGroup
import kotlinx.datetime.LocalDate

/** How the lifter rated one body part at a check-in. The names are stored in databases and backups. */
enum class MuscleRating { ON_TRACK, GROWING_WELL, LAGGING }

/**
 * A bodybuilding check-in: on [date] the lifter marked the body parts in [lagging] and the ones in [growingWell]; every
 * other group is on track. There is one check-in per date: saving another on the same date replaces it.
 */
data class CheckIn(
    val date: LocalDate,
    val lagging: Set<MuscleGroup>,
    val growingWell: Set<MuscleGroup>,
    val id: Long = 0,
) {
    init {
        require(lagging.none { it in growingWell }) { "lagging and growingWell share ${lagging.filter { it in growingWell }}" }
    }

    fun rating(group: MuscleGroup): MuscleRating = when (group) {
        in lagging -> MuscleRating.LAGGING
        in growingWell -> MuscleRating.GROWING_WELL
        else -> MuscleRating.ON_TRACK
    }
}
