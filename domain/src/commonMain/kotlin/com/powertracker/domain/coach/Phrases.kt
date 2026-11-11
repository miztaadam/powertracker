package com.powertracker.domain.coach

import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint

/** Lifter-facing wording for where in the lift a rep was missed. */
internal fun missPhrase(lift: MainLift, miss: MissPoint): String = when (miss) {
    MissPoint.LOCKOUT -> "at lockout"
    MissPoint.MID -> "mid-range"
    MissPoint.FLOOR -> when (lift) {
        MainLift.SQUAT -> "out of the hole"
        MainLift.BENCH -> "off the chest"
        MainLift.DEADLIFT -> "off the floor"
        MainLift.STRICT_PRESS -> "off the shoulders"
    }
}
