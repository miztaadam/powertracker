package com.powertracker.ui.components

/** Formats 8.0 as "8" and 7.5 as "7.5". */
fun formatRpe(rpe: Double): String = if (rpe % 1.0 == 0.0) rpe.toInt().toString() else rpe.toString()

/** Formats 155.0 as "155" and 152.5 as "152.5". */
fun formatKg(kg: Double): String = if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
