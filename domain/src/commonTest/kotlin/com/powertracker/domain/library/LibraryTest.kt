package com.powertracker.domain.library

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.model.Category
import com.powertracker.domain.seed.SeedLibrary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryTest {
    private val all = Library.all()
    private fun names(results: List<LibraryExercise>) = results.map { it.name }
    private fun alphabetical(results: List<LibraryExercise>) =
        results.sortedWith(compareBy({ Library.normalise(it.name) }, { it.name }))

    @Test
    fun names_areUniqueIgnoringCase_andNeverCollideWithAliases() {
        val nameKeys = all.map { Library.normalise(it.name) }
        assertEquals(nameKeys.size, nameKeys.toSet().size, "duplicate names: ${nameKeys.groupBy { it }.filterValues { it.size > 1 }.keys}")
        val aliasKeys = all.flatMap { e -> e.aliases.map { Library.normalise(it) } }
        assertEquals(aliasKeys.size, aliasKeys.toSet().size, "duplicate aliases")
        val collisions = aliasKeys.toSet() intersect nameKeys.toSet()
        assertTrue(collisions.isEmpty(), "aliases colliding with names: $collisions")
        assertTrue(all.all { e -> e.aliases.none { it.isBlank() } })
    }

    @Test
    fun everyGuide_isComplete() {
        val sentenceEnd = Regex("[.!?](\\s|$)")
        for (e in all) {
            val g = e.guide
            val sentences = sentenceEnd.findAll(g.purpose.trim()).count()
            assertTrue(sentences in 1..2, "${e.name}: purpose has $sentences sentences")
            assertTrue(g.setup.isNotEmpty(), "${e.name}: setup")
            assertTrue(g.steps.isNotEmpty(), "${e.name}: steps")
            assertTrue(g.cues.size in 2..3, "${e.name}: ${g.cues.size} cues")
            assertTrue(g.mistakes.size in 2..4, "${e.name}: ${g.mistakes.size} mistakes")
            val lines = listOf(g.purpose) + g.setup + g.steps + g.cues + g.mistakes
            assertTrue(lines.none { it.isBlank() }, "${e.name}: blank line")
            // The UI appends the safety line; guides never advise training through pain.
            assertTrue(lines.none { Library.SAFETY_LINE in it }, "${e.name}: safety line in guide data")
            assertTrue(lines.none { "through pain" in it.lowercase() || "through the pain" in it.lowercase() }, e.name)
            assertTrue(e.equipment.isNotEmpty(), "${e.name}: equipment")
            assertTrue(e.muscles.isNotEmpty(), "${e.name}: muscles")
        }
        assertEquals(
            "Stop if you feel sharp pain. For an injury or a condition, check with a doctor or physiotherapist.",
            Library.SAFETY_LINE,
        )
    }

    @Test
    fun everyEntry_hasKnownSources() {
        val allowed = listOf("acefitness.org", "exrx.net", "nsca.com", "acsm.org", "pmc.ncbi.nlm.nih.gov", "powerlifting.sport")
        for (e in all) {
            assertTrue(e.sources.isNotEmpty(), "${e.name}: no sources")
            for (id in e.sources) assertNotNull(Sources.all[id], "${e.name}: unknown source ${id.key}")
        }
        for ((id, s) in Sources.all) {
            val host = s.url.removePrefix("https://").substringBefore('/')
            assertTrue(s.url.startsWith("https://"), "${id.key}: not https")
            assertTrue(allowed.any { host == it || host == "www.$it" }, "${id.key}: $host is not an allowed source")
            assertTrue("/forum/" !in s.url, "${id.key}: forum thread")
            assertTrue(s.title.isNotBlank() && s.publisher.isNotBlank(), id.key)
            assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}").matches(s.accessed), "${id.key}: accessed")
        }
        val used = all.flatMap { it.sources }.toSet()
        assertEquals(Sources.all.keys, used, "registry holds sources no entry uses")
    }

    @Test
    fun everyGuide_bracesInSetup_andRackedBarbellsNameSafetyOrSpotter() {
        for (e in all) {
            val setup = e.guide.setup.map { it.lowercase() }
            assertTrue(setup.any { "brace" in it }, "${e.name}: setup has no bracing line")
            val barbell = e.equipment.any { it == Equipment.BARBELL || it == Equipment.SPECIALTY_BAR }
            if (barbell && setup.any { "rack" in it }) {
                assertTrue(setup.any { "safety" in it || "spotter" in it }, "${e.name}: racked barbell without safety pins, arms or spotter")
            }
        }
    }

    @Test
    fun everySeedName_isInTheCatalogue() {
        for (seed in SeedLibrary.exercises()) {
            val entry = all.singleOrNull { it.name == seed.name }
            assertNotNull(entry, "missing ${seed.name}")
            assertEquals(seed.category, entry.category, seed.name)
            assertEquals(seed.parentLift, entry.parentLift, seed.name)
            assertEquals(seed.muscles, entry.muscles, seed.name)
        }
    }

    @Test
    fun everyAccessoryMapName_isInTheCatalogue() {
        val catalogueNames = all.map { it.name }.toSet()
        assertEquals(emptySet(), AccessoryMap.allExerciseNames - catalogueNames)
    }

    @Test
    fun variations_haveAParentLift() {
        for (e in all) {
            if (e.category == Category.VARIATION) {
                assertNotNull(e.parentLift, "${e.name}: VARIATION without parentLift")
            } else {
                assertNull(e.parentLift, "${e.name}: parentLift on ${e.category}")
            }
        }
    }

    @Test
    fun search_normalisesHyphensSpacesAndCase() {
        for (q in listOf("pull up", "PULL-UP", "pull  up", " Pull_up ")) {
            assertEquals("Pull-up", Library.search(q).firstOrNull()?.name, q)
            assertEquals("Pull-up", Library.find(q)?.name, q)
        }
        assertEquals("developpe couche", Library.normalise("  Développé--COUCHÉ "))
        assertEquals("a b c", Library.normalise("A_b - c"))
    }

    @Test
    fun search_findsAliases() {
        assertEquals("RDL", Library.search("rdl").first().name)
        assertEquals("RDL", Library.search("romanian").first().name)
        assertEquals("RDL", Library.find("Romanian deadlift")?.name)
        assertEquals("Squat", Library.find("back squat")?.name)
        assertEquals("Bulgarian split squat", Library.find("RFESS")?.name)
        assertNull(Library.find("romanian"), "find matches whole names only")
        assertNull(Library.find("Not an exercise"))
    }

    /** The rank the search rules give [e] for [q]: 0 name starts, 1 name contains, 2 alias, 3 muscle, null none. */
    private fun ruleRank(e: LibraryExercise, q: String): Int? {
        val n = Library.normalise(q)
        val name = Library.normalise(e.name)
        return when {
            name.startsWith(n) -> 0
            n in name -> 1
            e.aliases.any { n in Library.normalise(it) } -> 2
            e.muscles.any { n in Library.normalise(it) } -> 3
            else -> null
        }
    }

    private fun assertRankedAndAlphabetical(q: String): List<LibraryExercise> {
        val results = Library.search(q)
        assertEquals(all.filter { ruleRank(it, q) != null }.toSet(), results.toSet(), q)
        val expected = results.sortedWith(compareBy<LibraryExercise> { ruleRank(it, q) }
            .thenBy { Library.normalise(it.name) }.thenBy { it.name })
        assertEquals(names(expected), names(results), q)
        return results
    }

    @Test
    fun search_ranksNamesBeforeAliasesBeforeMuscles() {
        val byName = all.associateBy { it.name }
        fun rankOf(name: String, q: String) = ruleRank(byName.getValue(name), q)

        // "deadlift": Deadlift starts with it, Deficit deadlift contains it, RDL has it only in an alias.
        assertEquals(listOf(0, 1, 2), listOf("Deadlift", "Deficit deadlift", "RDL").map { rankOf(it, "deadlift") })
        val deadlift = names(assertRankedAndAlphabetical("deadlift"))
        assertTrue(deadlift.indexOf("Deadlift") < deadlift.indexOf("Deficit deadlift"), "$deadlift")
        assertTrue(deadlift.indexOf("Deficit deadlift") < deadlift.indexOf("RDL"), "$deadlift")

        // "back": Squat via its alias "Back squat"; Row via the muscle "Upper back".
        assertEquals(listOf(2, 3), listOf("Squat", "Row").map { rankOf(it, "back") })
        val back = names(assertRankedAndAlphabetical("back"))
        assertTrue(back.indexOf("Squat") in 0 until back.indexOf("Row"), "$back")

        for (q in listOf("press", "squat", "triceps", "glutes", "pull")) assertRankedAndAlphabetical(q)
    }

    @Test
    fun search_filtersByAreaAndEquipment() {
        val mainLifts = Library.search("", areas = setOf(BodyArea.MAIN_LIFTS))
        assertTrue(mainLifts.all { it.area == BodyArea.MAIN_LIFTS })
        assertTrue(names(mainLifts).containsAll(listOf("Squat", "Bench", "Deadlift", "Close-grip bench")))

        val dumbbell = Library.search("", equipment = setOf(Equipment.DUMBBELL))
        assertTrue(dumbbell.all { Equipment.DUMBBELL in it.equipment })
        assertTrue("DB press" in names(dumbbell))

        val anyOf = names(Library.search("", equipment = setOf(Equipment.SPECIALTY_BAR, Equipment.BODYWEIGHT)))
        assertTrue(anyOf.containsAll(listOf("Safety-bar squat", "Pull-up")), "$anyOf")
        assertTrue("Squat" !in anyOf)

        val chestDumbbell = Library.search("", setOf(BodyArea.CHEST), setOf(Equipment.DUMBBELL))
        assertTrue("DB press" in names(chestDumbbell))
        assertTrue(chestDumbbell.all { it.area == BodyArea.CHEST && Equipment.DUMBBELL in it.equipment })
        assertEquals(alphabetical(chestDumbbell), chestDumbbell)
        assertEquals(listOf("Bulgarian split squat"), names(Library.search("squat", areas = setOf(BodyArea.QUADS))))
        assertEquals(
            listOf("Hip thrust", "RDL"),
            names(Library.search("", setOf(BodyArea.HAMSTRINGS_GLUTES, BodyArea.CALVES), setOf(Equipment.BARBELL))),
        )
    }

    @Test
    fun chest_findsTheNamesPeopleType() {
        val expected = mapOf(
            "DB bench" to "DB press",
            "incline DB" to "Incline DB press",
            "Incline bench" to "Incline bench press",
            "pec deck" to "Pec deck",
            "cable crossover" to "Cable crossover",
            "Dumbbell fly" to "DB fly",
            "parallel bar dip" to "Chest dip",
            "pushup" to "Push-up",
            "Smith machine bench press" to "Smith bench press",
        )
        for ((typed, name) in expected) assertEquals(name, Library.find(typed)?.name, typed)
        assertEquals("Pec deck", Library.search("pec deck").first().name)
        val chest = all.filter { it.area == BodyArea.CHEST }
        assertTrue(chest.size >= 30, "${chest.size} chest entries")
        for (e in chest) {
            assertEquals(Category.ACCESSORY, e.category, e.name)
            assertNull(e.parentLift, e.name)
        }
    }

    @Test
    fun search_emptyQueryReturnsAllInFilters() {
        assertEquals(alphabetical(all), Library.search(""))
        assertEquals(alphabetical(all), Library.search("  - "))
        val back = Library.search("", areas = setOf(BodyArea.BACK))
        assertEquals(alphabetical(all.filter { it.area == BodyArea.BACK }), back)
        assertEquals(emptyList(), Library.search("zzz no such exercise"))
    }
}
