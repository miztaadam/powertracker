package com.powertracker.domain.library

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.library.areas.OVER_FACE_SPOT
import com.powertracker.domain.library.areas.RACK_IT
import com.powertracker.domain.library.areas.RACK_SAFETY
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
    fun catalogue_hasAtLeast200Entries() {
        assertTrue(all.size >= 200, "only ${all.size} entries")
    }

    @Test
    fun noAreaFileIsEmpty() {
        val empty = BodyArea.entries.filter { area -> all.none { it.area == area } }
        assertTrue(empty.isEmpty(), "areas with no entries: $empty")
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
            assertTrue("/blog/" !in s.url, "${id.key}: blog post")
            assertTrue(s.title.isNotBlank() && s.publisher.isNotBlank(), id.key)
            assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}").matches(s.accessed), "${id.key}: accessed")
        }
        // The RPE sheet and the stretch and squeeze lines cite the registry too.
        val used = all.flatMap { it.sources }.toSet() + RpeGuide.sources + MuscleFocuses.all.values.flatMap { it.sources }
        assertEquals(Sources.all.keys, used, "registry holds sources no entry, the RPE sheet or the lines use")
    }

    @Test
    fun everyGuide_bracesInSetup() {
        for (e in all) {
            assertTrue(e.guide.setup.any { "brace" in it.lowercase() }, "${e.name}: setup has no bracing line")
        }
    }

    private fun lines(g: FormGuide) = g.setup + g.steps + g.cues + g.mistakes

    /** Barbell work set up in a rack: the owner's "heavy barbell work in a rack". */
    private fun isRackedBarbell(e: LibraryExercise): Boolean {
        val barbell = e.equipment.any { it == Equipment.BARBELL || it == Equipment.SPECIALTY_BAR }
        val rackWords = listOf("in the rack", "from a rack", "power rack", "out of the rack")
        val racked = (e.guide.setup + e.guide.steps).any { line -> line == RACK_IT || rackWords.any { it in line.lowercase() } }
        return barbell && racked
    }

    // A free weight moved over the face (NSCA spotting rule). The standing DB overhead triceps extension goes over the
    // head, not the face, so it is not one.
    private val overFace = setOf("Skull crusher", "DB lying triceps extension", "DB pullover")

    // Pin squat and Pin press start from the pins, and their own setup lines already name the safety pins.
    private val pinsAreTheSetup = setOf("Pin squat", "Pin press")

    @Test
    fun rackedBarbellWork_andOverFaceWork_haveTheShortLine_once() {
        for (e in all) {
            val setup = e.guide.setup
            if (isRackedBarbell(e) && e.name !in pinsAreTheSetup) {
                assertEquals(1, setup.count { it == RACK_SAFETY }, "${e.name}: racked barbell work needs the short rack line once")
            }
            if (e.name in overFace) {
                assertEquals(1, setup.count { it == OVER_FACE_SPOT }, "${e.name}: over-face work needs the short spotter line once")
            }
        }
        assertEquals(overFace, all.filter { e -> e.name in overFace }.map { it.name }.toSet(), "over-face entries renamed")
        assertEquals("Set the safety pins, or use a spotter.", RACK_SAFETY)
        assertEquals("Use a spotter.", OVER_FACE_SPOT)
    }

    @Test
    fun spotterLine_onlyWhereTheOwnerWantsIt() {
        for (e in all) {
            val g = e.guide
            for (line in lines(g).filter { "spot" in it.lowercase() }) {
                assertTrue(line == RACK_SAFETY || line == OVER_FACE_SPOT, "${e.name}: spotter line not in short form: $line")
            }
            assertTrue((g.steps + g.cues + g.mistakes).none { "spot" in it.lowercase() }, "${e.name}: spotter outside the setup")
            if (RACK_SAFETY in g.setup) assertTrue(isRackedBarbell(e), "${e.name}: rack line on work that is not racked barbell work")
            if (OVER_FACE_SPOT in g.setup) assertTrue(e.name in overFace, "${e.name}: spotter line on a lighter move")
        }
    }

    @Test
    fun noBeginnerOrSensationWording() {
        val banned = listOf(
            // beginner, etiquette and equipment-check reminders
            "collar", "clip", "start light", "empty bar", "warm up", "warm-up", "trainer", "check that", "check the",
            "clear the area", "wobble",
            // regress and start-light lines
            "if you can't do", "to make it easier", "too hard", "before you're ready", "new to you", "before you have",
            "too soon", "too fast", "over time", "add weight slowly", "use less weight", "adding weight",
            // sensation range limits
            "you feel", "feels", "slight stretch", "mild stretch", "at the stretch", "tension in", "comfortable",
            // jargon
            "stay tight",
        )
        for (e in all) {
            for (line in listOf(e.guide.purpose) + lines(e.guide)) {
                val lower = line.lowercase()
                for (b in banned) assertTrue(b !in lower, "${e.name}: \"$b\" in \"$line\"")
            }
        }
    }

    @Test
    fun noGenericAlias() {
        // Each alias names exactly one entry: it is not part of another entry's name. "Bench press" is the competition
        // lift's own name, "Leg ext" an abbreviation, and "Dip"/"Dips" stay on Triceps dip by ruling.
        val allowed = setOf("bench press", "leg ext", "dip", "dips")
        for (e in all) {
            for (alias in e.aliases.map { Library.normalise(it) }) {
                if (alias in allowed) continue
                val others = all.filter { it !== e && alias in Library.normalise(it.name) }.map { it.name }
                assertTrue(others.isEmpty(), "${e.name}: alias \"$alias\" also fits $others")
            }
        }
        // Removed in the final review: each fitted several entries.
        val removed = listOf(
            "Shoulder press", "Shrug", "Reverse fly", "External rotation", "Hip abduction", "Cable fly", "High cable fly",
            "Cable row", "T-bar", "Incline DB", "Incline bench", "Narrow push-up", "Forearm curl",
        )
        for (generic in removed) assertNull(Library.find(generic), generic)
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
        assertEquals("Triceps kickback", Library.find("kickback")?.name)
        assertEquals("Triceps dip", Library.find("dip")?.name)
        assertEquals("Triceps dip", Library.find("dips")?.name)
        assertNull(Library.find("narrow push-up"), "generic: fits Close-grip and Triangle push-up")
        assertNull(Library.find("forearm curl"), "generic: fits Wrist curl and Reverse wrist curl")
        assertNull(Library.find("romanian"), "find matches whole names only")
        assertNull(Library.find("Not an exercise"))
    }

    /** The rank the search rules give [e] for [q]: -1 exact name or alias, 0 name starts, 1 name contains, 2 alias, 3 muscle, null none. */
    private fun ruleRank(e: LibraryExercise, q: String): Int? {
        val n = Library.normalise(q)
        val name = Library.normalise(e.name)
        return when {
            name == n || e.aliases.any { Library.normalise(it) == n } -> -1
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

        // "deadlift": Deadlift is exactly it, Deficit deadlift contains it, RDL has it only in an alias.
        assertEquals(listOf(-1, 1, 2), listOf("Deadlift", "Deficit deadlift", "RDL").map { rankOf(it, "deadlift") })
        // "dead": Deadlift only starts with it.
        assertEquals(0, rankOf("Deadlift", "dead"))
        // "overhead press": an exact alias of Strict press (its former name) comes before other aliases containing it.
        assertEquals(listOf(-1, 2), listOf("Strict press", "DB shoulder press").map { rankOf(it, "overhead press") })
        val deadlift = names(assertRankedAndAlphabetical("deadlift"))
        assertTrue(deadlift.indexOf("Deadlift") < deadlift.indexOf("Deficit deadlift"), "$deadlift")
        assertTrue(deadlift.indexOf("Deficit deadlift") < deadlift.indexOf("RDL"), "$deadlift")

        // "back": Squat via its alias "Back squat"; Row via the muscle "Upper back".
        assertEquals(listOf(2, 3), listOf("Squat", "Row").map { rankOf(it, "back") })
        val back = names(assertRankedAndAlphabetical("back"))
        assertTrue(back.indexOf("Squat") in 0 until back.indexOf("Row"), "$back")

        for (q in listOf("press", "squat", "triceps", "glutes", "pull", "overhead press", "bench press")) assertRankedAndAlphabetical(q)
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
        val quadSquats = Library.search("squat", areas = setOf(BodyArea.QUADS))
        assertTrue("Bulgarian split squat" in names(quadSquats) && "Goblet squat" in names(quadSquats), "${names(quadSquats)}")
        assertTrue(quadSquats.all { it.area == BodyArea.QUADS })
        assertEquals(
            listOf("Barbell calf raise", "Barbell glute bridge", "Good morning", "Hip thrust", "RDL", "Stiff-leg deadlift"),
            names(Library.search("", setOf(BodyArea.HAMSTRINGS_GLUTES, BodyArea.CALVES), setOf(Equipment.BARBELL))),
        )
    }

    @Test
    fun chest_findsTheNamesPeopleType() {
        val expected = mapOf(
            "DB bench" to "DB press",
            "incline dumbbell press" to "Incline DB press",
            "barbell incline bench press" to "Incline bench press",
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
    fun back_findsTheNamesPeopleType() {
        val expected = mapOf(
            "BB row" to "Row",
            "Pendlay row" to "Row",
            "Seal row" to "Seal row",
            "lat pulldown" to "Lat pulldown",
            "chin up" to "Chin-up",
            "chinup" to "Chin-up",
            "straight-arm pulldown" to "Straight-arm pulldown",
            "DB row" to "One-arm DB row",
            "seated row" to "Seated cable row",
            "lever T-bar row" to "T-bar row",
            "hyperextension" to "Back extension",
            "BB shrug" to "Barbell shrug",
            "face pull" to "Face pull",
            "pullup" to "Pull-up",
        )
        for ((typed, name) in expected) {
            assertEquals(name, Library.find(typed)?.name, typed)
            assertTrue(name in names(Library.search(typed)), "$typed: search misses $name")
        }
        // Typed the way people type, the intended entry is the top search result.
        val firstHit = mapOf(
            "lat pulldown" to "Lat pulldown",
            "chin up" to "Chin-up",
            "chinup" to "Chin-up",
            "pendlay" to "Row",
            "seal row" to "Seal row",
            "straight arm" to "Straight-arm pulldown",
            "hyperextension" to "Back extension",
            "shrug" to "Barbell shrug",
            "face pull" to "Face pull",
            "t bar" to "T-bar row",
        )
        for ((typed, name) in firstHit) assertEquals(name, Library.search(typed).firstOrNull()?.name, typed)
        val back = all.filter { it.area == BodyArea.BACK }
        assertTrue(back.size >= 32, "${back.size} back entries")
        for (e in back) {
            assertEquals(Category.ACCESSORY, e.category, e.name)
            assertNull(e.parentLift, e.name)
        }
    }

    @Test
    fun shoulders_findTheNamesPeopleType() {
        val expected = mapOf(
            "OHP" to "Strict press",
            "military press" to "Strict press",
            "overhead press" to "Strict press",
            "seated military press" to "Seated barbell press",
            "dumbbell shoulder press" to "DB shoulder press",
            "shoulder press machine" to "Machine shoulder press",
            "landmine press" to "Landmine press",
            "laterals" to "Lateral raise",
            "side raise" to "Lateral raise",
            "front raise" to "Front raise",
            "rear delt fly" to "Rear delt raise",
            "bent-over reverse fly" to "Rear delt raise",
            "reverse pec deck" to "Machine reverse fly",
            "rear delt row" to "Cable rear delt row",
            "band external rotation" to "Cable external rotation",
            "KB press" to "Kettlebell press",
        )
        for ((typed, name) in expected) {
            assertEquals(name, Library.find(typed)?.name, typed)
            assertTrue(name in names(Library.search(typed)), "$typed: search misses $name")
        }
        val firstHit = mapOf(
            "overhead press" to "Strict press",
            "strict press" to "Strict press",
            "lateral raise" to "Lateral raise",
            "arnold" to "Arnold press",
            "landmine" to "Landmine press",
        )
        for ((typed, name) in firstHit) assertEquals(name, Library.search(typed).firstOrNull()?.name, typed)
        val shoulders = all.filter { it.area == BodyArea.SHOULDERS }
        // 24 since the overhead press moved to the main lifts as Strict press.
        assertTrue(shoulders.size >= 24, "${shoulders.size} shoulder entries")
        for (e in shoulders) {
            assertEquals(Category.ACCESSORY, e.category, e.name)
            assertNull(e.parentLift, e.name)
        }
        // Left out: behind-the-neck pressing (no allowed source gives a safe technique) and upright rows (ACE advises against them).
        assertNull(Library.find("behind the neck press"))
        assertNull(Library.find("upright row"))
    }

    @Test
    fun arms_findTheNamesPeopleType() {
        val expected = mapOf(
            "EZ curl" to "EZ-bar curl",
            "barbell curl" to "Curl",
            "dumbbell curl" to "DB curl",
            "hammer curl" to "Hammer curl",
            "Scott curl" to "Preacher curl",
            "rope pushdown" to "Rope pushdown",
            "tricep pushdown" to "Triceps pushdown",
            "skull crusher" to "Skull crusher",
            "French press" to "Skull crusher",
            "dumbbell overhead triceps extension" to "DB overhead triceps extension",
            "dips" to "Triceps dip",
            "weighted dip" to "Triceps dip",
            "kickback" to "Triceps kickback",
            "diamond push-up" to "Triangle push-up",
            "wrist curl" to "Wrist curl",
        )
        for ((typed, name) in expected) {
            assertEquals(name, Library.find(typed)?.name, typed)
            assertTrue(name in names(Library.search(typed)), "$typed: search misses $name")
        }
        val firstHit = mapOf(
            "dips" to "Triceps dip",
            "skull" to "Skull crusher",
            "rope pushdown" to "Rope pushdown",
            "preacher" to "Preacher curl",
            "concentration" to "Concentration curl",
        )
        for ((typed, name) in firstHit) assertEquals(name, Library.search(typed).firstOrNull()?.name, typed)
        // Generic names that fit several entries are not aliases of one of them.
        for (generic in listOf("pushdown", "triceps extension", "tricep extension", "lying triceps extension", "alternating curl")) {
            assertNull(Library.find(generic), generic)
        }
        // The triceps dip and the chest dip stay separate entries.
        assertEquals("Chest dip", Library.find("parallel bar dip")?.name)
        val arms = all.filter { it.area == BodyArea.ARMS }
        assertTrue(arms.size >= 30, "${arms.size} arm entries")
        for (e in arms) {
            assertEquals(Category.ACCESSORY, e.category, e.name)
            assertNull(e.parentLift, e.name)
        }
        // Left out: bench dips (ACE warns they can put too much force on the shoulder) and a loaded barbell
        // held overhead for triceps extensions (no allowed source gives a way to get it there safely).
        assertNull(Library.find("bench dip"))
    }

    @Test
    fun quads_findTheNamesPeopleType() {
        val expected = mapOf(
            "goblet" to "Goblet squat",
            "KB goblet squat" to "Goblet squat",
            "air squat" to "Bodyweight squat",
            "hack" to "Hack squat",
            "45 degree leg press" to "Leg press",
            "single leg leg press" to "Single-leg leg press",
            "walking lunge" to "Walking lunge",
            "forward lunge" to "Lunge",
            "rear lunge" to "Reverse lunge",
            "side lunge" to "Lateral lunge",
            "split squat" to "Split squat",
            "step up" to "Step-up",
            "leg ext" to "Leg extension",
            "squat jump" to "Jump squat",
            "prowler push" to "Sled push",
            "bench pistol squat" to "Single-leg box squat",
            "smith machine squat" to "Smith squat",
        )
        for ((typed, name) in expected) {
            assertEquals(name, Library.find(typed)?.name, typed)
            assertTrue(name in names(Library.search(typed)), "$typed: search misses $name")
        }
        val firstHit = mapOf(
            "goblet" to "Goblet squat",
            "hack" to "Hack squat",
            "leg press" to "Leg press",
            "walking lunge" to "Walking lunge",
            "leg ext" to "Leg extension",
            "zercher" to "Zercher squat",
            "curtsy" to "Curtsy lunge",
            "step up" to "Step-up",
        )
        for ((typed, name) in firstHit) assertEquals(name, Library.search(typed).firstOrNull()?.name, typed)
        // Generic names that fit several entries are not aliases of one of them.
        for (generic in listOf("squat machine", "machine squat", "box step-up", "machine leg extension", "sled", "dumbbell lunges")) {
            assertNull(Library.find(generic), generic)
        }
        // The two squat variations sit with the main lifts, under the squat.
        for (name in listOf("High-bar squat", "Tempo squat")) {
            val e = assertNotNull(Library.find(name), name)
            assertEquals(BodyArea.MAIN_LIFTS, e.area, name)
            assertEquals(Category.VARIATION, e.category, name)
            assertEquals(com.powertracker.domain.model.MainLift.SQUAT, e.parentLift, name)
        }
        val quads = all.filter { it.area == BodyArea.QUADS }
        assertTrue(quads.size >= 27, "${quads.size} quad entries")
        for (e in quads) {
            assertEquals(Category.ACCESSORY, e.category, e.name)
            assertNull(e.parentLift, e.name)
        }
        assertEquals(Kind.ACCESSORY, Library.find("Leg extension")?.kind)
        assertEquals(Kind.COMPOUND, Library.find("Leg press")?.kind)
        // Left out: no allowed source gives a safe technique for the sissy squat (ExRx lists it among the moves
        // with the most knee torque), the cossack squat or the backward sled drag (forum posts only).
        for (missing in listOf("sissy squat", "cossack squat", "sled drag")) assertNull(Library.find(missing), missing)
    }

    @Test
    fun hamstringsGlutes_findTheNamesPeopleType() {
        val expected = mapOf(
            "SLDL" to "Stiff-leg deadlift",
            "stiff-leg" to "Stiff-leg deadlift",
            "dumbbell RDL" to "DB RDL",
            "single leg RDL" to "Single-leg RDL",
            "GHR" to "Glute-ham raise",
            "Nordic" to "Nordic curl",
            "nordic hamstring curl" to "Nordic curl",
            "KB swing" to "Kettlebell swing",
            "glute bridge" to "Glute bridge",
            "seated hip abduction" to "Machine hip abduction",
            "hip adduction" to "Machine hip adduction",
            "donkey kick" to "Quadruped hip extension",
            "reverse hyperextension" to "Reverse hyper",
            "swiss ball leg curl" to "Stability ball leg curl",
            "prone leg curl" to "Lying leg curl",
            "lever hip thrust" to "Machine hip thrust",
            "dumbbell hip thrust" to "DB hip thrust",
            "hex bar deadlift" to "Trap-bar deadlift",
            "sumo" to "Sumo deadlift",
            "clam" to "Clamshell",
        )
        for ((typed, name) in expected) {
            assertEquals(name, Library.find(typed)?.name, typed)
            assertTrue(name in names(Library.search(typed)), "$typed: search misses $name")
        }
        val firstHit = mapOf(
            "good morning" to "Good morning",
            "nordic" to "Nordic curl",
            "seated leg curl" to "Seated leg curl",
            "glute bridge" to "Glute bridge",
            "kettlebell swing" to "Kettlebell swing",
            "clamshell" to "Clamshell",
        )
        for ((typed, name) in firstHit) assertEquals(name, Library.search(typed).firstOrNull()?.name, typed)
        // Generic names that fit several entries are not aliases of one of them.
        for (generic in listOf("leg curl", "hamstring curl", "swing", "hip bridge")) {
            assertNull(Library.find(generic), generic)
        }
        // The two deadlift variations sit with the main lifts, under the deadlift.
        for (name in listOf("Sumo deadlift", "Trap-bar deadlift")) {
            val e = assertNotNull(Library.find(name), name)
            assertEquals(BodyArea.MAIN_LIFTS, e.area, name)
            assertEquals(Category.VARIATION, e.category, name)
            assertEquals(com.powertracker.domain.model.MainLift.DEADLIFT, e.parentLift, name)
        }
        val area = all.filter { it.area == BodyArea.HAMSTRINGS_GLUTES }
        assertTrue(area.size >= 28, "${area.size} hamstring and glute entries")
        for (e in area) {
            assertEquals(Category.ACCESSORY, e.category, e.name)
            assertNull(e.parentLift, e.name)
        }
        for (name in listOf("Lying leg curl", "Nordic curl", "Machine hip abduction", "Quadruped hip extension")) {
            assertEquals(Kind.ACCESSORY, Library.find(name)?.kind, name)
        }
        for (name in listOf("Good morning", "Glute bridge", "Kettlebell swing", "Stiff-leg deadlift")) {
            assertEquals(Kind.COMPOUND, Library.find(name)?.kind, name)
        }
        // Left out: no allowed source gives a technique for the cable pull-through or the snatch-grip deadlift
        // (ExRx forum only), or for a single-leg hip thrust on a bench. The cable glute kickback was removed in
        // review: its source states no mistakes.
        for (missing in listOf("cable pull through", "pull through", "snatch grip deadlift", "single leg hip thrust", "glute kickback")) {
            assertNull(Library.find(missing), missing)
        }
    }

    @Test
    fun calvesAndCore_findTheNamesPeopleType() {
        val expected = mapOf(
            "donkey calf raise" to "Donkey calf raise",
            "calf press" to "Leg press calf raise",
            "seated heel raise" to "Seated dumbbell calf raise",
            "single leg calf raise" to "Single-leg calf raise",
            "DB calf raise" to "Dumbbell calf raise",
            "smith machine calf raise" to "Smith calf raise",
            "machine calf raise" to "Standing calf raise",
            "farmer carry" to "Farmer's walk",
            "farmers walk" to "Farmer's walk",
            "suitcase carry" to "Suitcase carry",
            "waiters walk" to "Waiter's carry",
            "overhead carry" to "Waiter's carry",
            "pinch carry" to "Plate pinch carry",
            "Pallof" to "Pallof press",
            "anti-rotation press" to "Pallof press",
            "woodchop" to "Cable woodchop",
            "ab wheel" to "Ab wheel rollout",
            "swiss ball rollout" to "Stability ball rollout",
            "rope crunch" to "Cable crunch",
            "ab crunch machine" to "Machine crunch",
            "bicycle crunches" to "Bicycle crunch",
            "deadbug" to "Dead bug",
            "birddog" to "Bird dog",
            "forearm plank" to "Front plank",
            "side bridge" to "Side plank",
        )
        for ((typed, name) in expected) {
            assertEquals(name, Library.find(typed)?.name, typed)
            assertTrue(name in names(Library.search(typed)), "$typed: search misses $name")
        }
        val firstHit = mapOf(
            "farmer" to "Farmer's walk",
            "pallof" to "Pallof press",
            "plank" to "Front plank",
            "seated calf" to "Seated calf raise",
            "reverse crunch" to "Reverse crunch",
        )
        for ((typed, name) in firstHit) assertEquals(name, Library.search(typed).firstOrNull()?.name, typed)
        // Generic names that fit several entries are not aliases of one of them.
        for (generic in listOf("calf raise", "plank", "crunch", "leg raise", "carry", "rollout", "heel raise")) {
            assertNull(Library.find(generic), generic)
        }
        // A leg-press calf raise must not take the leg press's names.
        assertEquals("Leg press", Library.find("sled leg press")?.name)
        for (area in listOf(BodyArea.CALVES, BodyArea.CORE)) {
            val entries = all.filter { it.area == area }
            for (e in entries) {
                assertEquals(Category.ACCESSORY, e.category, e.name)
                assertNull(e.parentLift, e.name)
            }
        }
        assertTrue(all.count { it.area == BodyArea.CALVES } >= 8, "calf entries")
        assertTrue(all.count { it.area == BodyArea.CORE } >= 17, "core entries")
        // Loaded carries are COMPOUND; every other calf and core entry is ACCESSORY.
        val carries = setOf("Farmer's walk", "Suitcase carry", "Waiter's carry", "Plate pinch carry")
        for (e in all.filter { it.area == BodyArea.CALVES || it.area == BodyArea.CORE }) {
            assertEquals(if (e.name in carries) Kind.COMPOUND else Kind.ACCESSORY, e.kind, e.name)
        }
        // Left out: no allowed source gives a training technique for a back-extension hold (only endurance tests),
        // or a positional limit and sourced mistakes for the dumbbell side bend. The hanging and captain's chair raises
        // were removed in review: their sources give only one mistake (swinging).
        val leftOut = listOf(
            "back extension hold", "side bend", "dumbbell side bend",
            "hanging leg raise", "hanging knee raise", "captain's chair", "captain's chair knee raise",
        )
        for (missing in leftOut) assertNull(Library.find(missing), missing)
    }

    @Test
    fun search_emptyQueryReturnsAllInFilters() {
        assertEquals(alphabetical(all), Library.search(""))
        assertEquals(alphabetical(all), Library.search("  - "))
        val back = Library.search("", areas = setOf(BodyArea.BACK))
        assertEquals(alphabetical(all.filter { it.area == BodyArea.BACK }), back)
        assertEquals(emptyList(), Library.search("zzz no such exercise"))
    }

    @Test
    fun strictPressUpgradeNames_areFrozen() {
        // The names the one-time upgrade matches: the entry's name and the aliases it had when the upgrade shipped, normalised.
        assertEquals(
            setOf(
                "strict press", "overhead press", "ohp", "military press", "standing press", "barbell overhead press",
                "standing barbell press", "barbell military press",
            ),
            Library.STRICT_PRESS_UPGRADE_NAMES,
        )
        // Today that is exactly what the catalogue finds as Strict press; a later alias must not widen the upgrade.
        val entry = assertNotNull(Library.find("Strict press"))
        assertEquals((listOf(entry.name) + entry.aliases).map { Library.normalise(it) }.toSet(), Library.STRICT_PRESS_UPGRADE_NAMES)
        assertTrue(Library.isStrictPressUpgradeName("Overhead-Press "))
        assertTrue(Library.isStrictPressUpgradeName("OHP"))
        assertTrue(!Library.isStrictPressUpgradeName("Seated barbell press"))
    }
}
