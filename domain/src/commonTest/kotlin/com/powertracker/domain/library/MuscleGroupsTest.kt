package com.powertracker.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MuscleGroupsTest {
    private val all = Library.all()

    @Test
    fun everyCatalogueMuscle_mapsToOneGroup() {
        val strings = all.flatMap { it.muscles }.toSet()
        val unmapped = strings.filter { MuscleGroups.of(it) == null }
        assertTrue(unmapped.isEmpty(), "muscles with no group: $unmapped")
        // One table, one group per string, however it is typed.
        for (s in strings) {
            assertEquals(MuscleGroups.of(s), MuscleGroups.of("  ${s.uppercase()} "), s)
            assertEquals(1, MuscleGroups.table.count { (key, _) -> key == Library.normalise(s) }, s)
        }
        assertEquals(MuscleGroups.table.size, MuscleGroups.table.map { it.first }.toSet().size, "a muscle is listed twice")
        // The table holds nothing the catalogue does not use, apart from the groups' own names.
        val known = strings.map { Library.normalise(it) } + MuscleGroup.entries.map { Library.normalise(it.label) }
        assertEquals(emptyList(), MuscleGroups.table.map { it.first }.filter { it !in known })
        // Every group answers to its own name, so an exercise of the lifter's own tagged "Chest" is found.
        for (g in MuscleGroup.entries) assertEquals(g, MuscleGroups.of(g.label))

        assertEquals(
            listOf(
                "Chest", "Lats", "Upper back", "Traps", "Lower back", "Front delts", "Side delts", "Rear delts", "Biceps",
                "Triceps", "Forearms", "Quads", "Hamstrings", "Glutes", "Adductors", "Calves", "Abs", "Obliques",
            ),
            MuscleGroup.entries.map { it.label },
        )
        // The judgement calls.
        assertEquals(MuscleGroup.CHEST, MuscleGroups.of("Pecs"))
        assertEquals(MuscleGroup.REAR_DELTS, MuscleGroups.of("Rotator cuff"))
        assertEquals(MuscleGroup.BICEPS, MuscleGroups.of("Brachialis"))
        assertEquals(MuscleGroup.FOREARMS, MuscleGroups.of("Grip"))
        assertEquals(MuscleGroup.GLUTES, MuscleGroups.of("Hips"))
        assertEquals(MuscleGroup.LOWER_BACK, MuscleGroups.of("Position"))
        assertEquals(MuscleGroup.ABS, MuscleGroups.of("Trunk"))
        assertEquals(MuscleGroup.ABS, MuscleGroups.of("Abs"))
        assertEquals(MuscleGroup.OBLIQUES, MuscleGroups.of("Obliques"))
        // Anything else belongs to no group.
        assertNull(MuscleGroups.of("Whole body"))
        assertNull(MuscleGroups.of(""))
    }

    @Test
    fun byMuscle_isAlphabeticalAndFiltersEquipment() {
        for (g in MuscleGroup.entries) {
            val expected = all.filter { e -> e.muscles.any { MuscleGroups.of(it) == g } }
                .sortedWith(compareBy({ Library.normalise(it.name) }, { it.name }))
            assertEquals(expected.map { it.name }, Library.byMuscle(g).map { it.name }, g.label)
        }
        val chest = Library.byMuscle(MuscleGroup.CHEST).map { it.name }
        assertTrue("Bench" in chest && "DB press" in chest && "Triceps dip" in chest, "$chest")
        assertTrue("Squat" !in chest)
        // A muscle it works second counts as much as the first.
        assertTrue("Hammer curl" in Library.byMuscle(MuscleGroup.BICEPS).map { it.name })

        // Any of the chosen equipment.
        val dumbbell = Library.byMuscle(MuscleGroup.CHEST, setOf(Equipment.DUMBBELL))
        assertTrue(dumbbell.isNotEmpty() && dumbbell.size < chest.size)
        assertTrue(dumbbell.all { Equipment.DUMBBELL in it.equipment })
        assertEquals(chest.filter { name -> all.first { it.name == name }.equipment.any { it == Equipment.DUMBBELL || it == Equipment.CABLE } },
            Library.byMuscle(MuscleGroup.CHEST, setOf(Equipment.DUMBBELL, Equipment.CABLE)).map { it.name })
        assertEquals(emptyList(), Library.byMuscle(MuscleGroup.CALVES, setOf(Equipment.SPECIALTY_BAR)).map { it.name })
    }

    @Test
    fun muscleGroups_skipsEmptyGroups() {
        // The catalogue as it ships: only groups that list something, in the enum's order.
        val shipped = Library.muscleGroups()
        assertEquals(MuscleGroup.entries.filter { Library.byMuscle(it).isNotEmpty() }, shipped)
        assertTrue(shipped.isNotEmpty())

        // A smaller catalogue leaves the other groups out.
        val few = all.filter { it.name == "Standing calf raise" || it.name == "Floor crunch" || it.name == "Bench" }
        assertEquals(
            listOf(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.TRICEPS, MuscleGroup.CALVES, MuscleGroup.ABS),
            MuscleGroups.present(few),
        )
        assertEquals(emptyList(), MuscleGroups.present(emptyList()))
    }
}
