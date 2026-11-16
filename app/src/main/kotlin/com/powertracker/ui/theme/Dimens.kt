package com.powertracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 4dp spacing scale. Use these, not ad-hoc values. */
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Left and right screen margin. */
    val gutter = 16.dp

    /** Minimum size of anything tappable. */
    val touch = 48.dp
}

/** Shadow heights. Cards are soft and layered: tone steps and one soft shadow, no border ([CardShadow]). */
object Elevation {
    /** `PtCard` and the web's `Card`: the drop shadow's y offset is about this, its blur about three times it. */
    val card = 8.dp
}

val PowertrackerShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
