package com.powertracker.data.db

import com.powertracker.domain.seed.SeedLibrary as SharedSeed

/** Built-in exercises inserted when the database is first created; the list itself is shared with the web app. */
object SeedLibrary {
    /** Ids are left to auto-increment: inserted in order they come out as the shared list's 1-based ids. */
    fun exercises(): List<ExerciseEntity> = SharedSeed.exercises().map {
        ExerciseEntity(name = it.name, category = it.category, parentLift = it.parentLift, muscles = it.muscles, isBuiltIn = it.isBuiltIn)
    }
}
