package com.powertracker.data

import androidx.room.withTransaction
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.domain.library.Library

/**
 * Turns a catalogue exercise into a row of the user's data, the first time it is used. Nothing is
 * inserted up front: the catalogue is browsed as it is, and a row appears only with the plan item
 * or set that needs it.
 */
class CatalogueResolver(private val db: AppDatabase) {
    /**
     * The id of the exercise row for the catalogue exercise [name] (a name or an alias). A row with
     * that name, ignoring case, is reused whoever made it; if the only one is archived it is restored;
     * otherwise a built-in row is inserted from the catalogue.
     *
     * Call it inside the `withTransaction` that saves the plan item or set, so a failure there leaves
     * no row behind. It joins that transaction.
     */
    suspend fun resolve(name: String): Long {
        val entry = requireNotNull(Library.find(name)) { "$name is not in the exercise catalogue" }
        return db.withTransaction {
            // Oldest first: with two rows of one name, the same one is picked every time.
            val same = db.exercises().all().filter { it.name.equals(entry.name, ignoreCase = true) }
            val live = same.firstOrNull { !it.archived }
            val archived = same.firstOrNull()
            when {
                live != null -> live.id
                archived != null -> archived.id.also { db.exercises().restore(it) }
                else -> db.exercises().insert(
                    ExerciseEntity(name = entry.name, category = entry.category, parentLift = entry.parentLift, muscles = entry.muscles, isBuiltIn = true),
                )
            }
        }
    }
}
