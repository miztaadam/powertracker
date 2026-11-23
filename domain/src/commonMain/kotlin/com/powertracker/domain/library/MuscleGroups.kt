package com.powertracker.domain.library

/** The muscle groups the picker browses by, in the order its grid shows them. */
enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    LATS("Lats"),
    UPPER_BACK("Upper back"),
    TRAPS("Traps"),
    LOWER_BACK("Lower back"),
    FRONT_DELTS("Front delts"),
    SIDE_DELTS("Side delts"),
    REAR_DELTS("Rear delts"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    ADDUCTORS("Adductors"),
    CALVES("Calves"),
    ABS("Abs"),
    OBLIQUES("Obliques"),
}

/**
 * Which [MuscleGroup] each muscle string belongs to. The strings stay as they are wherever they are shown
 * (`LibraryExercise.muscles`, `Exercise.muscles`); this is the one place that sorts them into groups.
 * Every string the catalogue uses is here exactly once. An exercise is in a group when any of its
 * muscles is, first or not. A string that is not here (one the lifter typed for an exercise of their own)
 * belongs to no group.
 */
object MuscleGroups {
    /** Normalised muscle string to its group. A group's own name is always one of its strings. */
    internal val table: List<Pair<String, MuscleGroup>> = listOf(
        // "Pecs" is the catalogue's word for the chest; the group carries the name a newcomer looks for.
        // "Upper chest" is the coach's word for strict press misses off the shoulders (AccessoryMap, fixed).
        MuscleGroup.CHEST to listOf("Chest", "Pecs", "Upper chest"),
        MuscleGroup.LATS to listOf("Lats"),
        MuscleGroup.UPPER_BACK to listOf("Upper back"),
        MuscleGroup.TRAPS to listOf("Traps"),
        // "Position" is the coach's set-up word for deadlift variations that train holding the back flat
        // (it comes from AccessoryMap, which is fixed). It maps here so they list under Lower back.
        MuscleGroup.LOWER_BACK to listOf("Lower back", "Position"),
        // "Delts" is the coach's word for strict press misses mid-range (AccessoryMap, fixed). Pressing overhead works
        // the front of the shoulder first (the catalogue lists every overhead press as "Front delts" first).
        MuscleGroup.FRONT_DELTS to listOf("Front delts", "Delts"),
        MuscleGroup.SIDE_DELTS to listOf("Side delts"),
        // The rotator cuff has no tile of its own (three exercises). Its rotation drills are done for
        // the back of the shoulder, and are looked for beside the rear delt work.
        MuscleGroup.REAR_DELTS to listOf("Rear delts", "Rotator cuff"),
        // The brachialis bends the elbow under the biceps; every exercise that names it is a curl.
        MuscleGroup.BICEPS to listOf("Biceps", "Brachialis"),
        MuscleGroup.TRICEPS to listOf("Triceps"),
        // Grip is trained by the forearm muscles.
        MuscleGroup.FOREARMS to listOf("Forearms", "Grip"),
        MuscleGroup.QUADS to listOf("Quads"),
        MuscleGroup.HAMSTRINGS to listOf("Hamstrings"),
        // "Hips" on the box squat and hip thrust means driving the hips through, which the glutes do.
        MuscleGroup.GLUTES to listOf("Glutes", "Hips"),
        MuscleGroup.ADDUCTORS to listOf("Adductors"),
        MuscleGroup.CALVES to listOf("Calves"),
        // "Trunk" on the front-loaded squats means bracing against the bar, which is the abs' job there.
        // Abs and obliques stay two groups: the catalogue tells them apart on every core exercise.
        MuscleGroup.ABS to listOf("Abs", "Trunk"),
        MuscleGroup.OBLIQUES to listOf("Obliques"),
    ).flatMap { (group, muscles) -> muscles.map { Library.normalise(it) to group } }

    private val byMuscle: Map<String, MuscleGroup> = table.toMap()

    /** The group [muscle] belongs to, ignoring case, hyphens and spacing; null when it belongs to none. */
    fun of(muscle: String): MuscleGroup? = byMuscle[Library.normalise(muscle)]

    /** True when any of [muscles] belongs to [group]. */
    fun works(muscles: List<String>, group: MuscleGroup): Boolean = muscles.any { of(it) == group }

    /** The groups that at least one of [exercises] works, in the enum's order. */
    internal fun present(exercises: List<LibraryExercise>): List<MuscleGroup> {
        val found = exercises.flatMapTo(HashSet()) { e -> e.muscles.mapNotNull(::of) }
        return MuscleGroup.entries.filter { it in found }
    }
}
