package com.powertracker.domain.seed

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint

/**
 * Built-in exercises inserted when the database is first created. Ids are 1-based list positions, which is
 * what Android's auto-increment assigns when it inserts them in this order, so both apps agree on them.
 */
object SeedLibrary {
    private fun main(name: String, category: Category, muscles: List<String>) =
        Exercise(0, name, category, parentLift = null, muscles = muscles, isBuiltIn = true)

    fun exercises(): List<Exercise> {
        // name -> (heavy, first lift listing it, muscles unioned over every row listing it)
        class Acc(val heavy: Boolean, val lift: MainLift, val muscles: LinkedHashSet<String> = linkedSetOf())
        val found = LinkedHashMap<String, Acc>()
        for (lift in MainLift.entries) for (miss in MissPoint.entries) {
            val muscles = AccessoryMap.muscles(lift, miss)
            for (option in AccessoryMap.options(lift, miss)) {
                found.getOrPut(option.exerciseName) { Acc(option.heavy, lift) }.muscles += muscles
            }
        }
        check(found.keys == AccessoryMap.allExerciseNames)

        val mapped = found.map { (name, acc) ->
            Exercise(
                id = 0,
                name = name,
                category = if (acc.heavy) Category.VARIATION else Category.ACCESSORY,
                parentLift = if (acc.heavy) acc.lift else null,
                muscles = acc.muscles.toList(),
                isBuiltIn = true,
            )
        }
        val all = listOf(
            main("Squat", Category.SQUAT, listOf("Quads", "Glutes")),
            main("Bench", Category.BENCH, listOf("Pecs", "Triceps", "Front delts")),
            main("Deadlift", Category.DEADLIFT, listOf("Hamstrings", "Glutes", "Lats")),
        ) + mapped + listOf(
            main("Curl", Category.ACCESSORY, listOf("Biceps")),
            main("Row", Category.ACCESSORY, listOf("Lats", "Upper back")),
            main("Pull-up", Category.ACCESSORY, listOf("Lats", "Biceps")),
        )
        return all.mapIndexed { i, e -> e.copy(id = i + 1L) }
    }
}
