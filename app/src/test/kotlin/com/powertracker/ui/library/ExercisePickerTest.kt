package com.powertracker.ui.library

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Native graphics, so text is measured at its real size: the layout checks here depend on it.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class ExercisePickerTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository

    /** What the picker reported, in order. */
    private val picked = mutableStateListOf<PickerEntry>()
    private val created = mutableStateListOf<String>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings() } }
    }

    private fun stored(): List<Exercise> = runBlocking { training.exercises().first() }

    private fun rows(name: String): List<ExerciseEntity> = runBlocking { db.exercises().all() }.filter { it.name.equals(name, ignoreCase = true) }

    private fun plan(): List<PlanItem> = runBlocking { training.plan().first() }

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    /**
     * Whether the list holds a row tagged [tag]. The list is lazy and longer than the screen, so this scrolls the row
     * into view first; the checks don't depend on how many entries the catalogue has.
     */
    private fun shows(tag: String): Boolean {
        scrollToRow(tag)
        return compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    /** Scrolls the lazy list until the row tagged [tag] is on screen; does nothing when the list has no such row. */
    private fun scrollToRow(tag: String) {
        if (compose.onAllNodes(hasTestTag("exercise-list")).fetchSemanticsNodes().isEmpty()) return
        runCatching { compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag(tag)) }
        compose.waitForIdle()
    }

    /** Scrolls the list back to the top, where the search box and the chips are. */
    private fun scrollToTop() {
        compose.onNodeWithTag("exercise-list").performScrollToIndex(0)
        compose.waitForIdle()
    }

    /** The chip rows scroll sideways: bring the chip on screen, then tap it. */
    private fun tapChip(tag: String) {
        scrollToTop()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun search(text: String) {
        compose.onNodeWithTag("exercise-search").performTextReplacement(text)
        compose.waitForIdle()
    }

    /** The picker on its own, over the seeded exercises (and [extra]), reporting into [picked] and [created]. */
    private fun showPicker(extra: List<Exercise> = emptyList(), recentIds: List<Long> = emptyList()) {
        val exercises = stored() + extra
        compose.setContent {
            PowertrackerTheme {
                ExercisePickerContent(
                    "Add exercise", exercises, recentIds,
                    onPick = { picked += it },
                    onCreateCustom = { name, _, _, _ -> created += name },
                    onClose = {},
                )
            }
        }
        compose.waitForIdle()
    }

    /** The real Plan screen with the picker open for Monday. */
    private fun showPlanPicker() {
        compose.setContent { PowertrackerTheme { PlanScreen(training, settings, onOpenSettings = {}) } }
        waitFor { shows("add-MONDAY") }
        compose.onNodeWithTag("add-MONDAY").performClick()
        waitFor { shows("exercise-search") }
    }

    private fun own(id: Long, name: String, muscles: List<String> = emptyList()) = Exercise(id, name, Category.ACCESSORY, null, muscles, isBuiltIn = false)

    @Test
    fun search_filtersAsYouType() {
        showPicker()
        assertTrue(shows("exercise-Squat") && shows("exercise-Pull-up"))

        search("pu")
        assertTrue(shows("exercise-Pull-up"))
        assertFalse(shows("exercise-Squat"))

        // Typed the way people type: no hyphen, any case, an alias.
        search("pull up")
        assertTrue(shows("exercise-Pull-up"))
        search("PULL-UP")
        assertTrue(shows("exercise-Pull-up"))
        search("romanian")
        assertTrue(shows("exercise-RDL"))
        assertFalse(shows("exercise-Pull-up"))

        // Every row on screen is one the shared search returns, in its order.
        search("squat")
        scrollToTop()
        val expected = Library.search("squat").map { it.name }
        assertTrue(expected.size > 1)
        val tops = expected.map { compose.onNodeWithTag("exercise-$it").getUnclippedBoundsInRoot().top }
        assertEquals(tops.sortedBy { it.value }, tops)
        assertFalse(shows("exercise-Bench"))

        scrollToTop()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.waitForIdle()
        assertTrue(shows("exercise-Bench"))
    }

    @Test
    fun areaAndEquipmentChips_filter() {
        showPicker()

        tapChip("area-BACK")
        compose.waitForIdle()
        compose.onNodeWithTag("area-BACK").assertIsOn()
        compose.onNodeWithTag("area-all").assertIsOff()
        for (e in Library.all()) assertEquals(e.name, e.area == BodyArea.BACK, shows("exercise-${e.name}"))
        assertTrue(shows("exercise-Row") && shows("exercise-Pull-up"))

        // Equipment narrows it; a second equipment chip widens it again (any of the chosen).
        tapChip("equipment-BODYWEIGHT")
        compose.waitForIdle()
        assertTrue(shows("exercise-Pull-up"))
        assertFalse(shows("exercise-Row"))
        tapChip("equipment-BARBELL")
        compose.waitForIdle()
        compose.onNodeWithTag("equipment-BODYWEIGHT").assertIsOn()
        compose.onNodeWithTag("equipment-BARBELL").assertIsOn()
        assertTrue(shows("exercise-Pull-up") && shows("exercise-Row"))

        // One body area at a time.
        tapChip("area-ARMS")
        compose.waitForIdle()
        compose.onNodeWithTag("area-BACK").assertIsOff()
        val arms = Library.search("", setOf(BodyArea.ARMS), setOf(Equipment.BODYWEIGHT, Equipment.BARBELL)).map { it.name }
        assertTrue(arms.isNotEmpty())
        for (e in Library.all()) assertEquals(e.name, e.name in arms, shows("exercise-${e.name}"))

        // "All" with no equipment brings everything back.
        tapChip("area-all")
        tapChip("equipment-BODYWEIGHT")
        tapChip("equipment-BARBELL")
        compose.waitForIdle()
        assertTrue(shows("exercise-Squat") && shows("exercise-Pull-up") && shows("exercise-Curl"))
    }

    @Test
    fun emptyResult_showsMessage() {
        showPicker()

        search("zercher carry")

        compose.onNodeWithText("No exercises match. Try fewer filters, or create your own.").assertExists()
        assertFalse(shows("exercise-Squat"))
        // The custom-exercise action comes with it, and starts from what was typed.
        compose.onNodeWithText("Create your own").performClick()
        compose.onNodeWithText("Add custom exercise").assertExists()
        compose.onNode(hasText("zercher carry") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("Create").performClick()
        compose.waitForIdle()
        assertEquals(listOf("zercher carry"), created.toList())
        assertTrue(picked.isEmpty())

        // Filters with nothing in them say the same.
        search("")
        tapChip("area-BACK")
        tapChip("equipment-KETTLEBELL")
        compose.waitForIdle()
        assertTrue(Library.search("", setOf(BodyArea.BACK), setOf(Equipment.KETTLEBELL)).isEmpty())
        compose.onNodeWithText("No exercises match. Try fewer filters, or create your own.").assertExists()
    }

    @Test
    fun createYourOwn_isOfferedBelowTheList() {
        showPicker()

        compose.onNodeWithTag("exercise-list").performScrollToNode(hasText("Create your own"))
        compose.onNodeWithText("Create your own").performClick()
        compose.onNodeWithText("Name").performTextReplacement("  Tyre flip ")
        compose.onNodeWithText("Create").performClick()
        compose.waitForIdle()

        assertEquals(listOf("Tyre flip"), created.toList())
    }

    @Test
    fun pickerInfo_opensGuideWithoutPicking() {
        // On its own: the info button reports no pick.
        showPicker()
        scrollToRow("exercise-Squat")
        compose.checkInfoButtonOpensGuide("Squat")
        assertTrue("reading the guide picked the exercise", picked.isEmpty())
        compose.onNodeWithTag("exercise-Squat").performClick()
        assertEquals(listOf("Squat"), picked.map { it.name })
    }

    @Test
    fun pickerInfo_opensGuideWithoutPicking_inThePlan() {
        // A library exercise with no row yet: reading its guide must not store one either.
        runBlocking { rows("Curl").forEach { db.exercises().delete(it.id) } }
        val before = runBlocking { db.exercises().all() }
        showPlanPicker()

        scrollToRow("exercise-Curl")
        compose.checkInfoButtonOpensGuide("Curl")

        assertTrue("the picker stays open", shows("exercise-search"))
        assertTrue(plan().isEmpty())
        assertEquals(before, runBlocking { db.exercises().all() })

        // Tapping the row itself is what opens the add form, and its Add stores it.
        scrollToRow("exercise-Curl")
        compose.onNodeWithTag("exercise-Curl").performClick()
        confirmAdd()
        waitFor { plan().size == 1 }
        assertFalse(shows("exercise-search"))
        assertEquals(rows("Curl").single().id, plan().single().exerciseId)
    }

    @Test
    fun restoredBuiltIn_appearsOnce_andIsReused() {
        // A restore replaces the table: "Curl" comes back as an ordinary row under whatever id the backup had.
        val restored = runBlocking {
            rows("Curl").forEach { db.exercises().delete(it.id) }
            db.exercises().insertAll(listOf(ExerciseEntity(77, "Curl", Category.ACCESSORY, null, listOf("Biceps"), isBuiltIn = true)))
            // Recently used, so it is also one of "Your exercises".
            training.logSet(training.startSession(LocalDate(2026, 11, 9), null, null), LoggedSet(77, 30.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false))
            rows("Curl").single()
        }
        assertEquals(77L, restored.id)

        // Browsing and searching both list it once, tied to the restored row.
        for (query in listOf("", "curl")) {
            val list = pickerList(query, null, emptySet(), stored(), recentIds = listOf(77))
            val curls = (list.yours + list.all).filter { it.name.equals("Curl", ignoreCase = true) }
            assertEquals("query \"$query\"", listOf(77L), curls.map { it.exerciseId })
            assertEquals("Curl", curls.single().catalogueName)
        }

        showPlanPicker()
        scrollToRow("exercise-Curl")
        assertEquals(1, compose.onAllNodes(hasTestTag("exercise-Curl")).fetchSemanticsNodes().size)
        compose.onNodeWithTag("exercise-Curl").performClick()
        confirmAdd()
        waitFor { plan().size == 1 }

        assertEquals(77L, plan().single().exerciseId)
        assertEquals(listOf(restored), rows("Curl"))
    }

    /** Picking on Plan opens the sets/reps form (1.12); its Add stores the item. */
    private fun confirmAdd() {
        waitFor { compose.onAllNodes(hasText("Add") and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty() }
        // Past the moment in which the new form ignores taps (the second tap of a double tap on the row).
        compose.mainClock.advanceTimeBy(com.powertracker.ui.plan.ADD_FORM_TAP_WINDOW_MS)
        compose.onNode(hasText("Add") and hasAnyAncestor(isDialog())).performClick()
    }

    /** Runs [node]'s click twice in one go on the UI thread, as two taps landing in the same frame would. */
    private fun doubleTap(node: SemanticsNodeInteraction) {
        val click = node.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread {
            click()
            click()
        }
    }

    @Test
    fun pickerRow_doubleTap_addsOnce() {
        showPlanPicker()
        search("curl")

        // Two taps inside one frame, before the picker can close: the second picks nothing more.
        doubleTap(compose.onNodeWithTag("exercise-Curl"))
        confirmAdd()
        waitFor { plan().isNotEmpty() }
        compose.waitForIdle()
        assertEquals(1, plan().size)

        // The same for "Create" in the custom-exercise dialog: one row, one plan item.
        compose.onNodeWithTag("add-MONDAY").performClick()
        waitFor { shows("exercise-search") }
        search("Tyre flip")
        compose.onNodeWithText("Create your own").performClick()
        doubleTap(compose.onNodeWithText("Create"))
        confirmAdd()
        waitFor { plan().size >= 2 }
        compose.waitForIdle()
        assertEquals(1, rows("Tyre flip").size)
        assertEquals(2, plan().size)
    }

    @Test
    fun createCustom_withCatalogueName_usesTheCatalogueEntry() {
        val before = runBlocking { db.exercises().all() }
        val squat = rows("Squat").single()
        showPlanPicker()

        // A catalogue alias typed as a new exercise: the catalogue's Squat is used, and no row is made.
        search("zzz")
        compose.onNodeWithText("Create your own").performClick()
        compose.onNodeWithText("Name").performTextReplacement(" Back squat ")
        compose.onNodeWithText("Create").performClick()
        confirmAdd()
        waitFor { plan().size == 1 }
        assertEquals(squat.id, plan().single().exerciseId)
        assertEquals(before, runBlocking { db.exercises().all() })

        // A catalogue name with no row yet gets its row the catalogue way: built in, with the catalogue muscles.
        runBlocking { rows("Leg press").forEach { db.exercises().delete(it.id) } }
        compose.onNodeWithTag("add-MONDAY").performClick()
        waitFor { shows("exercise-search") }
        search("zzz")
        compose.onNodeWithText("Create your own").performClick()
        compose.onNodeWithText("Name").performTextReplacement("leg press")
        compose.onNodeWithText("Create").performClick()
        confirmAdd()
        waitFor { plan().size == 2 }
        val legPress = rows("Leg press").single()
        assertEquals("Leg press", legPress.name)
        assertTrue(legPress.isBuiltIn)
        assertEquals(Library.find("Leg press")!!.muscles, legPress.muscles)
        assertEquals(legPress.id, plan().last().exerciseId)
    }

    @Test
    fun yourExercises_listedFirst_whenNoQuery() {
        val bench = stored().first { it.name == "Bench" }
        val sled = own(900, "Tyre flip", listOf("Quads"))
        val archived = own(901, "Old lift").copy(archived = true)

        // Recently used first, then the lifter's own; none of them again under "All exercises".
        val list = pickerList("", null, emptySet(), stored() + sled + archived, recentIds = listOf(bench.id))
        assertEquals(listOf("Bench", "Tyre flip"), list.yours.map { it.name })
        assertEquals(listOf(bench.id, 900L), list.yours.map { it.exerciseId })
        assertEquals(Library.search("").map { it.name } - "Bench", list.all.map { it.name })
        // A search or a filter drops the section.
        assertTrue(pickerList("s", null, emptySet(), stored() + sled, listOf(bench.id)).yours.isEmpty())
        assertTrue(pickerList("", BodyArea.BACK, emptySet(), stored() + sled, listOf(bench.id)).yours.isEmpty())
        // The lifter's own exercise is searched by name and by muscle, and listed before the catalogue.
        assertEquals("Tyre flip", pickerList("tyre", null, emptySet(), stored() + sled, emptyList()).all.single().name)
        assertEquals("Tyre flip", pickerList("quads", null, emptySet(), stored() + sled, emptyList()).all.first().name)

        showPicker(extra = listOf(sled, archived), recentIds = listOf(bench.id))
        fun top(tag: String) = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().top
        val yours = compose.onNodeWithText("YOUR EXERCISES").getUnclippedBoundsInRoot().top
        val all = compose.onNodeWithText("ALL EXERCISES").getUnclippedBoundsInRoot().top
        assertTrue(yours < top("exercise-Bench") && top("exercise-Bench") < top("exercise-Tyre flip") && top("exercise-Tyre flip") < all)
        val firstCatalogueRow = list.all.first().name
        assertTrue(all < top("exercise-$firstCatalogueRow"))
        assertEquals(1, compose.onAllNodes(hasTestTag("exercise-Bench")).fetchSemanticsNodes().size)
        assertFalse(shows("exercise-Old lift"))

        scrollToTop()
        search("s")
        compose.onNodeWithText("YOUR EXERCISES").assertDoesNotExist()
        compose.onNodeWithText("ALL EXERCISES").assertDoesNotExist()
    }

    @Test
    fun compoundDefaults3x5_accessory3x10() {
        assertEquals(Kind.COMPOUND, Library.find("Squat")!!.kind)
        assertEquals(Kind.ACCESSORY, Library.find("Curl")!!.kind)
        runBlocking { settings.update { it.copy(defaultRpe = 7.5) } }
        showPlanPicker()

        search("squat")
        compose.onNodeWithTag("exercise-Squat").performClick()
        confirmAdd()
        waitFor { plan().size == 1 }
        compose.onNodeWithTag("add-MONDAY").performClick()
        waitFor { shows("exercise-search") }
        search("curl")
        compose.onNodeWithTag("exercise-Curl").performClick()
        confirmAdd()
        waitFor { plan().size == 2 }

        val squat = stored().first { it.name == "Squat" }
        val curl = stored().first { it.name == "Curl" }
        assertEquals(
            listOf(
                PlanItem(0, DayOfWeek.MONDAY, squat.id, sets = 3, reps = 5, targetRpe = 7.5, order = 0),
                PlanItem(0, DayOfWeek.MONDAY, curl.id, sets = 3, reps = 10, targetRpe = 7.5, order = 1),
            ),
            plan().map { it.copy(id = 0) },
        )
        waitFor { compose.onAllNodesWithText("Curl  3 × 10 @ 7.5").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Squat  3 × 5 @ 7.5").assertExists()
    }

    private fun gridShown(): Boolean = compose.onAllNodes(hasTestTag("muscle-CHEST")).fetchSemanticsNodes().isNotEmpty()

    private fun tapMuscle(group: MuscleGroup) {
        scrollToTop()
        compose.onNodeWithTag("muscle-${group.name}").performClick()
        compose.waitForIdle()
    }

    /** The names a muscle group lists: the catalogue's, and [own] exercises of the lifter's, alphabetically. */
    private fun inGroup(group: MuscleGroup, equipment: Set<Equipment> = emptySet(), own: List<String> = emptyList()): List<String> =
        (Library.byMuscle(group, equipment).map { it.name } + own).sortedBy { Library.normalise(it) }

    @Test
    fun muscleGrid_showsWhenNoQueryOrArea() {
        val bench = stored().first { it.name == "Bench" }
        showPicker(recentIds = listOf(bench.id))

        // Every group with an exercise has a tile, in the domain's order, above the lifter's own exercises.
        compose.onNodeWithText("BROWSE BY MUSCLE").assertExists()
        val groups = Library.muscleGroups()
        assertEquals(MuscleGroup.entries.toList(), groups)
        val boxes = groups.map { g ->
            compose.onNode(hasTestTag("muscle-${g.name}") and hasText(g.label) and hasClickAction()).assertExists().getUnclippedBoundsInRoot()
        }
        val readingOrder = boxes.sortedWith(compareBy({ it.top.value }, { it.left.value }))
        assertEquals(readingOrder, boxes)
        assertTrue(boxes.maxOf { it.bottom } < compose.onNodeWithText("YOUR EXERCISES").getUnclippedBoundsInRoot().top)
        assertTrue(compose.onNodeWithTag("area-all").getUnclippedBoundsInRoot().bottom < boxes.first().top)

        // A search hides it; clearing the search brings it back.
        search("s")
        assertFalse(gridShown())
        compose.onNodeWithText("BROWSE BY MUSCLE").assertDoesNotExist()
        search("")
        assertTrue(gridShown())

        // So does a body area; "All" brings it back. Equipment leaves it where it is.
        tapChip("area-BACK")
        assertFalse(gridShown())
        tapChip("area-all")
        assertTrue(gridShown())
        tapChip("equipment-DUMBBELL")
        assertTrue(gridShown())
        assertTrue(shows("exercise-DB press"))
        assertFalse(shows("exercise-Squat"))
    }

    @Test
    fun tapMuscle_listsItsExercises() {
        val bench = stored().first { it.name == "Bench" }
        val mine = own(900, "Tyre flip", listOf("pecs", "Whole body"))
        val unmapped = own(901, "Tyre drag", listOf("Whole body"))
        val archived = own(902, "Old press", listOf("Chest")).copy(archived = true)
        val exercises = stored() + mine + unmapped + archived

        // The catalogue's exercises and the lifter's own whose muscles are in the group, as one alphabetical list.
        val chest = pickerList("", null, emptySet(), exercises, listOf(bench.id), MuscleGroup.CHEST)
        assertTrue(chest.yours.isEmpty())
        assertEquals(inGroup(MuscleGroup.CHEST, own = listOf("Tyre flip")), chest.all.map { it.name })
        assertEquals(bench.id, chest.all.first { it.name == "Bench" }.exerciseId)
        assertEquals(900L, chest.all.first { it.name == "Tyre flip" }.exerciseId)
        // A muscle string with no group puts an exercise in none of them.
        for (g in MuscleGroup.entries) {
            assertFalse(g.label, pickerList("", null, emptySet(), exercises, emptyList(), g).all.any { it.name == "Tyre drag" || it.name == "Old press" })
        }
        // It is still found by search and under "Your exercises".
        assertEquals("Tyre drag", pickerList("tyre drag", null, emptySet(), exercises, emptyList()).all.single().name)
        assertTrue(pickerList("", null, emptySet(), exercises, emptyList()).yours.any { it.name == "Tyre drag" })

        showPicker(extra = listOf(mine, unmapped, archived), recentIds = listOf(bench.id))
        tapMuscle(MuscleGroup.CHEST)
        compose.onNodeWithText("CHEST EXERCISES").assertExists()
        compose.onNodeWithText("YOUR EXERCISES").assertDoesNotExist()
        compose.onNodeWithText("ALL EXERCISES").assertDoesNotExist()
        assertFalse(gridShown())
        val listed = chest.all.map { it.name }.toSet()
        for (e in Library.all()) assertEquals(e.name, e.name in listed, shows("exercise-${e.name}"))
        assertTrue(shows("exercise-Tyre flip"))
        assertFalse(shows("exercise-Tyre drag"))
        assertFalse(shows("exercise-Old press"))

        // A row picks, as anywhere else in the picker.
        scrollToRow("exercise-Bench")
        compose.onNodeWithTag("exercise-Bench").performClick()
        assertEquals(listOf("Bench"), picked.map { it.name })
        assertEquals(listOf(bench.id), picked.map { it.exerciseId })
    }

    @Test
    fun muscleAndEquipment_combine() {
        val mine = own(900, "Tyre flip", listOf("Chest"))
        val exercises = stored() + mine
        val dumbbell = pickerList("", null, setOf(Equipment.DUMBBELL), exercises, emptyList(), MuscleGroup.CHEST).all.map { it.name }
        // The lifter's own exercises have no equipment, so a chosen equipment leaves them out, as it does everywhere.
        assertEquals(inGroup(MuscleGroup.CHEST, setOf(Equipment.DUMBBELL)), dumbbell)
        assertTrue("DB press" in dumbbell && "Bench" !in dumbbell)

        showPicker(extra = listOf(mine))
        tapMuscle(MuscleGroup.CHEST)
        tapChip("equipment-DUMBBELL")
        compose.onNodeWithTag("equipment-DUMBBELL").assertIsOn()
        compose.onNodeWithText("CHEST EXERCISES").assertExists()
        assertTrue(shows("exercise-DB press"))
        assertFalse(shows("exercise-Bench"))
        assertFalse(shows("exercise-Tyre flip"))

        // A second equipment widens it (any of the chosen); none brings the whole group back.
        tapChip("equipment-BARBELL")
        assertTrue(shows("exercise-DB press") && shows("exercise-Bench"))
        assertFalse(shows("exercise-Squat"))
        tapChip("equipment-DUMBBELL")
        tapChip("equipment-BARBELL")
        assertTrue(shows("exercise-Tyre flip") && shows("exercise-Bench") && shows("exercise-DB press"))

        // An equipment chosen before the muscle counts too; nothing left says so, and the way back stays.
        tapChip("equipment-SPECIALTY_BAR")
        assertTrue(Library.byMuscle(MuscleGroup.CHEST, setOf(Equipment.SPECIALTY_BAR)).isEmpty())
        compose.onNodeWithText(NO_MATCH).assertExists()
        compose.onNodeWithText("All muscles").assertExists()
        compose.onNodeWithText("All muscles").performClick()
        compose.waitForIdle()
        tapMuscle(MuscleGroup.QUADS)
        val quads = inGroup(MuscleGroup.QUADS, setOf(Equipment.SPECIALTY_BAR))
        assertTrue(quads.isNotEmpty())
        for (name in quads) assertTrue(name, shows("exercise-$name"))
        assertFalse(shows("exercise-Squat"))
    }

    @Test
    fun typing_leavesMuscleMode() {
        showPicker()
        tapMuscle(MuscleGroup.CHEST)
        assertFalse(shows("exercise-Squat"))

        // The search is the ordinary one, over everything.
        search("squat")
        compose.onNodeWithText("CHEST EXERCISES").assertDoesNotExist()
        compose.onNodeWithText("All muscles").assertDoesNotExist()
        for (name in Library.search("squat").map { it.name }) assertTrue(name, shows("exercise-$name"))
        assertFalse(shows("exercise-Bench"))

        // Clearing it is back at the grid, not in the muscle.
        search("")
        assertTrue(gridShown())
        compose.onNodeWithText("CHEST EXERCISES").assertDoesNotExist()
        assertTrue(shows("exercise-Squat"))

        // A body area leaves the muscle too.
        tapMuscle(MuscleGroup.CHEST)
        tapChip("area-BACK")
        compose.onNodeWithText("CHEST EXERCISES").assertDoesNotExist()
        assertTrue(shows("exercise-Pull-up"))
        assertFalse(shows("exercise-Bench"))
    }

    @Test
    fun backFromMuscle_returnsToGrid() {
        val bench = stored().first { it.name == "Bench" }
        showPicker(recentIds = listOf(bench.id))
        tapMuscle(MuscleGroup.CALVES)
        assertFalse(gridShown())
        assertFalse(shows("exercise-Bench"))

        scrollToTop()
        compose.onNodeWithText("All muscles").assertHeightIsAtLeast(48.dp).performClick()
        compose.waitForIdle()
        assertTrue(gridShown())
        compose.onNodeWithText("CALVES EXERCISES").assertDoesNotExist()
        compose.onNodeWithText("All muscles").assertDoesNotExist()
        compose.onNodeWithText("YOUR EXERCISES").assertExists()
        assertTrue(shows("exercise-Bench") && shows("exercise-Squat"))

        // Another muscle can be opened straight away.
        tapMuscle(MuscleGroup.QUADS)
        compose.onNodeWithText("QUADS EXERCISES").assertExists()
        assertTrue(shows("exercise-Squat"))
    }

    @Test
    fun muscleInfo_opensGuideWithoutPicking() {
        showPicker()
        tapMuscle(MuscleGroup.QUADS)
        scrollToRow("exercise-Squat")
        compose.checkInfoButtonOpensGuide("Squat")
        assertTrue("reading the guide picked the exercise", picked.isEmpty())
        // The picker is where it was.
        compose.onNodeWithText("QUADS EXERCISES").assertExists()
        scrollToRow("exercise-Squat")
        compose.onNodeWithTag("exercise-Squat").performClick()
        assertEquals(listOf("Squat"), picked.map { it.name })
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp-xxhdpi")
    fun muscleGrid_fitsASmallPhone_withFullSizeTargets() {
        showPicker()
        val width = compose.onRoot().getUnclippedBoundsInRoot().width
        assertEquals(360.dp, width)
        val tiles = Library.muscleGroups().map { g ->
            val tile = compose.onNodeWithTag("muscle-${g.name}")
            tile.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            // The name is on one line: nothing wrapped or cut.
            val text = compose.onNode(hasText(g.label) and hasAnyAncestor(hasTestTag("muscle-${g.name}")), useUnmergedTree = true).fetchSemanticsNode()
            val layouts = mutableListOf<TextLayoutResult>()
            text.config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
            assertEquals(g.label, 1, layouts.single().lineCount)
            // And it lies inside its tile.
            val box = tile.getUnclippedBoundsInRoot()
            val name = compose.onNode(hasText(g.label) and hasAnyAncestor(hasTestTag("muscle-${g.name}")), useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue(g.label, name.left >= box.left && name.right <= box.right && name.top >= box.top && name.bottom <= box.bottom)
            tile.getUnclippedBoundsInRoot()
        }
        for (b in tiles) assertTrue("a tile overflows", b.left >= 0.dp && b.right <= width)
        // Two or three to a row, none overlapping.
        val perRow = tiles.groupBy { it.top }.values.map { it.size }
        assertTrue("$perRow", perRow.dropLast(1).all { it == perRow.first() } && perRow.first() in 2..3)
        for (row in tiles.groupBy { it.top }.values) row.zipWithNext { a, b -> assertTrue(a.right <= b.left) }

        // Inside a muscle: the way back and the rows.
        tapMuscle(MuscleGroup.UPPER_BACK)
        val back = compose.onNodeWithText("All muscles")
        back.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        assertTrue(back.getUnclippedBoundsInRoot().right <= width)
        val heading = compose.onNodeWithText("UPPER BACK EXERCISES").getUnclippedBoundsInRoot()
        assertTrue(heading.left >= 0.dp && heading.right <= width)
        for (name in inGroup(MuscleGroup.UPPER_BACK)) {
            scrollToRow("exercise-$name")
            val bounds = compose.onNodeWithTag("exercise-$name").getUnclippedBoundsInRoot()
            assertTrue("$name row overflows", bounds.left >= 0.dp && bounds.right <= width)
            compose.onNodeWithContentDescription("Form guide for $name").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp-xxhdpi")
    fun picker_fitsASmallPhone_withFullSizeTargets() {
        showPicker(extra = listOf(own(900, "Sled push with a long name to wrap", listOf("Quads", "Glutes", "Hamstrings", "Calves"))), recentIds = listOf(900))
        val width = compose.onRoot().getUnclippedBoundsInRoot().width
        assertEquals(360.dp, width)

        // Rows, search and the header stay inside the screen.
        val names = Library.all().map { it.name } + "Sled push with a long name to wrap"
        for (name in names) {
            scrollToRow("exercise-$name")
            val row = compose.onNodeWithTag("exercise-$name")
            val bounds = row.getUnclippedBoundsInRoot()
            assertTrue("$name row overflows", bounds.left >= 0.dp && bounds.right <= width)
            row.assertHeightIsAtLeast(48.dp)
            val info = compose.onNodeWithContentDescription("Form guide for $name")
            info.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            val b = info.getUnclippedBoundsInRoot()
            assertTrue("$name info button overflows", b.right <= width && b.top >= bounds.top && b.bottom <= bounds.bottom)
        }
        scrollToTop()
        val search = compose.onNodeWithTag("exercise-search")
        search.assertHeightIsAtLeast(48.dp)
        assertTrue(search.getUnclippedBoundsInRoot().right <= width)
        compose.onNodeWithContentDescription("Close").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)

        // Chips are full-size targets; the rows of chips scroll sideways instead of overflowing.
        val chips = listOf("area-all") + BodyArea.entries.map { "area-${it.name}" } + Equipment.entries.map { "equipment-${it.name}" }
        for (tag in chips) {
            val chip = compose.onNodeWithTag(tag).performScrollTo()
            chip.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            val b = chip.getUnclippedBoundsInRoot()
            assertTrue("$tag is cut off", b.left >= 0.dp && b.right <= width)
        }
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasText("Create your own"))
        compose.onNodeWithText("Create your own").assertHeightIsAtLeast(48.dp)
        scrollToRow("exercise-Squat")
        assertEquals(0, compose.onAllNodesWithContentDescription("Form guide for Squat").fetchSemanticsNodes().size - 1)
    }

    @Test
    fun chestThenBarbell_listsBench_withItsVariations() {
        showPicker()
        tapChip("area-CHEST")
        tapChip("equipment-BARBELL")
        assertTrue(shows("exercise-Bench"))
        assertTrue(shows("exercise-Close-grip bench") && shows("exercise-Incline bench press"))
        assertFalse(shows("exercise-Squat"))
    }
}
