package com.powertracker.ui.components

import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.stack.PlateStack

/** Formats 8.0 as "8" and 7.5 as "7.5". */
fun formatRpe(rpe: Double): String = if (rpe % 1.0 == 0.0) rpe.toInt().toString() else rpe.toString()

/** Formats 155.0 as "155" and 152.5 as "152.5". */
fun formatKg(kg: Double): String = if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()

/** A weight with its unit: "152.5 kg", or "plate 7½" for a plate number (1.17). [formatKg] stays kg-only. */
fun formatLoad(value: Double, unit: LoadUnit): String = when (unit) {
    LoadUnit.KG -> "${formatKg(value)} kg"
    LoadUnit.PLATES -> PlateStack.text(value)
}
