package com.powertracker.domain.library

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
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
        // The table holds nothing the catalogue or the coach's accessory map does not use, apart from the groups' own names.
        val coachMuscles = MainLift.entries.flatMap { lift -> MissPoint.entries.flatMap { AccessoryMap.muscles(lift, it) } }
        val known = (strings + coachMuscles).map { Library.normalise(it) } + MuscleGroup.entries.map { Library.normalise(it.label) }
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
        assertEquals(MuscleGroup.FRONT_DELTS, MuscleGroups.of("Delts"))
        assertEquals(MuscleGroup.CHEST, MuscleGroups.of("Upper chest"))
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
    fun byMuscles_anyOf_alphabetical_filtersEquipment() {
        val alphabetical = compareBy<LibraryExercise>({ Library.normalise(it.name) }, { it.name })
        // Any of the ticked groups, each exercise once, alphabetically.
        val groups = setOf(MuscleGroup.CHEST, MuscleGroup.BICEPS)
        val expected = all.filter { e -> e.muscles.any { MuscleGroups.of(it) in groups } }.sortedWith(alphabetical)
        val found = Library.byMuscles(groups)
        assertEquals(expected.map { it.name }, found.map { it.name })
        assertEquals(found.map { it.name }.distinct(), found.map { it.name })
        assertTrue(found.any { it.name == "Bench" } && found.any { it.name == "Hammer curl" })
        assertTrue(found.none { it.name == "Squat" })
        assertTrue(found.size > Library.byMuscle(MuscleGroup.CHEST).size && found.size > Library.byMuscle(MuscleGroup.BICEPS).size)

        // One group is the single-muscle list.
        for (g in MuscleGroup.entries) assertEquals(Library.byMuscle(g).map { it.name }, Library.byMuscles(setOf(g)).map { it.name }, g.label)

        // Any of the chosen equipment, on top.
        val dumbbell = Library.byMuscles(groups, setOf(Equipment.DUMBBELL))
        assertTrue(dumbbell.isNotEmpty() && dumbbell.size < found.size)
        assertEquals(expected.filter { Equipment.DUMBBELL in it.equipment }.map { it.name }, dumbbell.map { it.name })
        assertEquals(
            expected.filter { e -> e.equipment.any { it == Equipment.DUMBBELL || it == Equipment.CABLE } }.map { it.name },
            Library.byMuscles(groups, setOf(Equipment.DUMBBELL, Equipment.CABLE)).map { it.name },
        )

        // No group ticked matches nothing: "any of none".
        assertEquals(emptyList(), Library.byMuscles(emptySet()))
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
