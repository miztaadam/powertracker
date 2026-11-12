package com.powertracker.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.liftLabel

/**
 * The one-time strict press upgrade (strict press spec §4.1), run by [MIGRATION_1_2] and when a version-1 backup is
 * restored.
 *
 * The rows that match are the non-archived ones whose name is "Strict press" or one of the aliases the entry had when
 * the upgrade shipped ("Overhead press", "OHP", ...): [Library.STRICT_PRESS_UPGRADE_NAMES], matched as [Library.find]
 * matches names. Frozen, so a later alias never widens it. The oldest (lowest id) becomes the main lift: named
 * "Strict press", category STRICT_PRESS, built in. The others become STRICT_PRESS under their own names. Ids never
 * change, so every set, plan item and baseline stays attached. Running it again changes nothing more.
 */
object StrictPressUpgrade {
    private val entry = requireNotNull(Library.find(liftLabel(MainLift.STRICT_PRESS))) { "Strict press is missing from the catalogue" }

    /** The ids of the matching rows, oldest first: the first becomes the main lift, the rest are only re-categorised. */
    fun matching(rows: List<Triple<Long, String, Boolean>>): List<Long> =
        rows.filter { (_, name, archived) -> !archived && Library.isStrictPressUpgradeName(name) }.map { it.first }.sorted()

    /** Applies the rule to restored rows before they are written. */
    fun apply(rows: List<ExerciseEntity>): List<ExerciseEntity> {
        val ids = matching(rows.map { Triple(it.id, it.name, it.archived) })
        val main = ids.firstOrNull()
        return rows.map { row ->
            when (row.id) {
                main -> row.copy(name = entry.name, category = Category.STRICT_PRESS, isBuiltIn = true)
                in ids -> row.copy(category = Category.STRICT_PRESS)
                else -> row
            }
        }
    }

    /** Applies the rule to the exercise table in place. Data only: the schema is untouched. */
    fun run(db: SupportSQLiteDatabase) {
        val rows = db.query("SELECT id, name, archived FROM exercise").use { c ->
            buildList { while (c.moveToNext()) add(Triple(c.getLong(0), c.getString(1), c.getInt(2) != 0)) }
        }
        val category = Converters().categoryToDb(Category.STRICT_PRESS)
        for ((i, id) in matching(rows).withIndex()) {
            if (i == 0) {
                db.execSQL("UPDATE exercise SET name = ?, category = ?, isBuiltIn = 1 WHERE id = ?", arrayOf<Any>(entry.name, category, id))
            } else {
                db.execSQL("UPDATE exercise SET category = ? WHERE id = ?", arrayOf<Any>(category, id))
            }
        }
    }
}
