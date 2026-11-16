package com.powertracker.ui.library

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.AppShell
import com.powertracker.ui.PtBottomBar
import com.powertracker.ui.Tab
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.plan.PlanAdd
import com.powertracker.ui.plan.PlanViewModel
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayAdd
import com.powertracker.ui.today.TodayViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.powertracker.ui.components.FakeHapticFeedback
import com.powertracker.ui.components.WithHaptics

/** The Exercises tab: browsing with the Muscles drop-down, the guide sheet's actions, and what they store. */
// Native graphics, so text is measured at its real size: the layout checks here depend on it.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class ExercisesScreenTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var todayVm: TodayViewModel

    /** Where the snackbar actions asked to go, in order. */
    private val opened = mutableStateListOf<String>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings() } }
    }

    @After
    fun tearDown() {
        // The view models made here have no owner to clear them: stop their work before the database goes.
        for (vm in viewModels) vm.viewModelScope.cancel()
    }

    private val viewModels = mutableListOf<ViewModel>()

    private fun stored(): List<Exercise> = runBlocking { training.exercises().first() }

    private fun rows(name: String): List<ExerciseEntity> = runBlocking { db.exercises().all() }.filter { it.name.equals(name, ignoreCase = true) }

    private fun plan(): List<PlanItem> = runBlocking { training.plan().first() }

    private fun id(name: String): Long = stored().first { it.name == name }.id

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun has(tag: String): Boolean = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun says(text: String): Boolean = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Whether the list holds a row tagged [tag]; the list is lazy, so this scrolls the row into view first. */
    private fun shows(tag: String): Boolean {
        if (has("exercise-list")) runCatching { compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag(tag)) }
        compose.waitForIdle()
        return has(tag)
    }

    private fun scrollToTop() {
        compose.onNodeWithTag("exercise-list").performScrollToIndex(0)
        compose.waitForIdle()
    }

    private fun tapChip(tag: String) {
        scrollToTop()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun search(text: String) {
        compose.onNodeWithTag("exercise-search").performTextReplacement(text)
        compose.waitForIdle()
    }

    /** Opens the Muscles drop-down, ticks or unticks [groups], and closes it. */
    private fun tick(vararg groups: MuscleGroup) {
        tapChip("muscles-dropdown")
        waitFor { has("muscles-sheet") }
        for (g in groups) compose.onNodeWithTag("muscle-option-${g.name}").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        waitFor { !has("muscles-sheet") }
    }

    /** Opens the guide sheet of the row for [name]. */
    private fun openGuide(name: String) {
        assertTrue("$name is listed", shows("exercise-$name"))
        compose.onNodeWithTag("exercise-$name").performClick()
        waitFor { compose.guideIsOpen() }
    }

    private fun inGuide(text: String): Boolean =
        compose.onAllNodes(hasText(text) and hasAnyAncestor(hasTestTag("form-guide"))).fetchSemanticsNodes().isNotEmpty()

    /** Taps "Add to plan" in the open guide and waits for the day chooser. */
    private fun chooseDay() {
        compose.onNodeWithText("Add to plan").performClick()
        waitFor { has("day-chooser") }
    }

    /** Runs [node]'s click twice in one go on the UI thread, as two taps landing in the same frame would. */
    private fun doubleTap(node: SemanticsNodeInteraction) {
        val click = node.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread {
            click()
            click()
        }
    }

    private fun startSession() = runBlocking { training.startSession(today, null, null) }

    /** The tab on its own, with the Today view model the app would share with the Today tab. */
    private fun show(repository: TrainingRepository = training) {
        todayVm = TodayViewModel(repository, advice, settings) { today }.also { viewModels += it }
        compose.setContent {
            PowertrackerTheme {
                ExercisesScreen(repository, settings, todayVm, onOpenPlan = { opened += "plan" }, onOpenToday = { opened += "today" }, today = { today })
            }
        }
        waitFor { has("exercise-list") && !todayVm.state.value.loading }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun exercisesTab_isTheFourthOfFiveTabs_andFits() {
        assertEquals(listOf("Today", "Coach", "Progress", "Exercises", "Plan"), Tab.entries.map { it.label })
        assertEquals("exercises", Tab.EXERCISES.route)
        assertEquals(Tab.entries.size, Tab.entries.map { it.icon }.distinct().size)

        compose.setContent { PowertrackerTheme { PtBottomBar(Tab.EXERCISES.route) {} } }
        compose.waitForIdle()

        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        assertEquals(360.dp, screen.right - screen.left)
        var edge = 0.dp
        for (tab in Tab.entries) {
            val node = compose.onNodeWithTag("tab-${tab.route}")
            node.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            val bounds = node.getUnclippedBoundsInRoot()
            // Side by side, in order, inside the 360 dp screen.
            assertTrue("${tab.label} starts at ${bounds.left}, after $edge", bounds.left >= edge)
            assertTrue("${tab.label} ends at ${bounds.right}", bounds.right <= screen.right)
            edge = bounds.right
            compose.onNode(hasText(tab.label) and hasAnyAncestor(hasTestTag("tab-${tab.route}")), useUnmergedTree = true).assertExists()
        }
        compose.onNodeWithTag("tab-exercises").assertIsSelected()
        // No label is cut off or ellipsised.
        compose.assertTextsFit()
    }

    /** [node] and the texts in it are inside the 360 dp screen, whole. */
    private fun assertFits(node: SemanticsNodeInteraction) {
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue("runs off the screen (${bounds.left} to ${bounds.right})", bounds.left >= 0.dp && bounds.right <= 360.dp)
        fun texts(n: androidx.compose.ui.semantics.SemanticsNode): List<androidx.compose.ui.semantics.SemanticsNode> = listOf(n) + n.children.flatMap(::texts)
        for (text in texts(node.fetchSemanticsNode())) {
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            text.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            for (layout in layouts) {
                val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f
                assertTrue("wider than its box", widest <= layout.size.width + 1f)
                assertFalse("ellipsised", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
                assertEquals("on one line", 1, layout.lineCount)
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun exercisesTab_fitsTheSmallestPhone() {
        show()
        // The header and the drop-down. (The chip rows scroll sideways, as in the picker.)
        assertFits(compose.onNodeWithText("Exercises"))
        assertFits(compose.onNodeWithText("${exerciseCount(stored())} EXERCISES"))
        assertFits(compose.onNodeWithTag("muscles-dropdown", useUnmergedTree = true).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp))

        // Ticked chips wrap instead of running off the side.
        val long = listOf(MuscleGroup.UPPER_BACK, MuscleGroup.LOWER_BACK, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, MuscleGroup.HAMSTRINGS)
        tick(*long.toTypedArray())
        scrollToTop()
        for (g in long) assertFits(compose.onNodeWithTag("muscle-chip-${g.name}", useUnmergedTree = true).assertHeightIsAtLeast(48.dp))
        assertFits(compose.onNodeWithTag("muscles-clear", useUnmergedTree = true).assertHeightIsAtLeast(48.dp))
        assertFits(compose.onNodeWithTag("muscles-dropdown", useUnmergedTree = true))

        // The drop-down's sheet: every group, whole, each a full-size target.
        tapChip("muscles-dropdown")
        waitFor { has("muscles-sheet") }
        for (g in MuscleGroup.entries) assertFits(compose.onNodeWithTag("muscle-option-${g.name}", useUnmergedTree = true).assertHeightIsAtLeast(48.dp))
        assertFits(compose.onNodeWithText("EXERCISES THAT WORK ANY OF THEM"))
        compose.onNodeWithContentDescription("Close").performClick()
        waitFor { !has("muscles-sheet") }

        // The guide's actions side by side, and the day chooser.
        scrollToTop()
        compose.onNodeWithTag("muscles-clear").performClick()
        startSession()
        openGuide("Squat")
        waitFor { says("Add to today") }
        for (label in listOf("Add to plan", "Add to today")) assertFits(compose.onNode(hasText(label) and hasAnyAncestor(hasTestTag("guide-actions")), useUnmergedTree = true))
        chooseDay()
        for (day in listOf("MONDAY", "WEDNESDAY", "FRIDAY")) assertFits(compose.onNodeWithTag("day-$day", useUnmergedTree = true).assertHeightIsAtLeast(48.dp))
    }

    @Test
    fun exercisesTab_browseByMuscleAndSearchWork() {
        val own = runBlocking { training.addExercise(Exercise(0, "Tyre flip", Category.ACCESSORY, null, listOf("Quads"), isBuiltIn = false)) }
        show()

        // The header counts the catalogue and the lifter's own exercises.
        compose.onNodeWithText("Exercises").assertExists()
        val count = exerciseCount(stored())
        assertEquals(Library.all().size + stored().count { e -> Library.all().none { it.name.equals(e.name, ignoreCase = true) } }, count)
        assertTrue(count > Library.all().size)
        compose.onNodeWithText("$count EXERCISES").assertExists()

        // The tab has the drop-down, not the picker's tile grid.
        assertFalse(says("BROWSE BY MUSCLE"))
        assertFalse(has("muscle-CHEST"))
        compose.onNodeWithTag("muscles-dropdown").assertExists()

        // Nothing ticked: the lifter's own first, then every exercise under its body area.
        compose.onNodeWithText("YOUR EXERCISES").assertExists()
        assertTrue(shows("exercise-Tyre flip"))
        for (area in com.powertracker.domain.library.BodyArea.entries) assertTrue(area.name, shows("area-heading-${area.name}"))
        val core = Library.all().last { it.area == com.powertracker.domain.library.BodyArea.CORE }.name
        for (e in listOf("Squat", "Hammer curl", core)) assertTrue(e, shows("exercise-$e"))
        assertEquals(Library.all().size + 1, pickerList("", null, emptySet(), stored(), emptyList()).let { it.yours.size + it.all.size })

        // By muscle.
        tick(MuscleGroup.QUADS)
        assertTrue(shows("exercise-Squat") && shows("exercise-Tyre flip"))
        assertFalse(shows("exercise-Bench"))
        assertFalse(has("area-heading-CHEST"))

        // By search, typed the way people type.
        scrollToTop()
        compose.onNodeWithTag("muscles-clear").performClick()
        search("pull up")
        assertTrue(shows("exercise-Pull-up"))
        assertFalse(shows("exercise-Squat"))
        assertTrue(own > 0)
    }

    @Test
    fun musclesDropdown_multiSelect_showsAnyOf() {
        show()

        // All 18 groups, in the enum's order, none ticked.
        tapChip("muscles-dropdown")
        waitFor { has("muscles-sheet") }
        assertEquals(18, MuscleGroup.entries.size)
        var last = Pair(-1f, -1f)
        for (g in MuscleGroup.entries) {
            val node = compose.onNodeWithTag("muscle-option-${g.name}")
            node.assertIsOff().assertTextContains(g.label)
            val at = node.fetchSemanticsNode().positionInRoot.let { Pair(it.y, it.x) }
            assertTrue("${g.label} comes after the one before", at.first > last.first || (at.first == last.first && at.second > last.second))
            last = at
        }
        compose.onNodeWithTag("muscle-option-CHEST").performClick()
        compose.onNodeWithTag("muscle-option-BICEPS").performClick()
        compose.onNodeWithTag("muscle-option-CHEST").assertIsOn()
        compose.onNodeWithTag("muscle-option-LATS").assertIsOff()
        compose.onNodeWithContentDescription("Close").performClick()
        waitFor { !has("muscles-sheet") }

        // Exercises that work either group, and no others.
        val groups = setOf(MuscleGroup.CHEST, MuscleGroup.BICEPS)
        val listed = pickerList("", null, emptySet(), stored(), emptyList(), muscles = groups)
        assertTrue(listed.yours.isEmpty())
        val catalogue = Library.byMuscles(groups).map { it.name }
        assertEquals(catalogue, listed.all.filter { it.library != null }.map { it.name })
        assertTrue(listed.all.all { e -> groups.any { MuscleGroups.works(e.muscles, it) } })
        for (name in listOf("Bench", "DB press", "Hammer curl", "Pull-up")) assertTrue(name, shows("exercise-$name"))
        for (name in listOf("Squat", "Leg press", "Deadlift")) assertFalse(name, shows("exercise-$name"))
        // More than either group alone.
        assertTrue(catalogue.size > Library.byMuscle(MuscleGroup.CHEST).size && catalogue.size > Library.byMuscle(MuscleGroup.BICEPS).size)
    }

    @Test
    fun musclesDropdown_combinesWithEquipmentAndSearch() {
        show()
        tick(MuscleGroup.CHEST, MuscleGroup.TRICEPS)
        assertTrue(shows("exercise-Bench") && shows("exercise-DB press"))

        // Any ticked equipment, on top of any ticked muscle.
        tapChip("equipment-DUMBBELL")
        val groups = setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS)
        val dumbbell = Library.byMuscles(groups, setOf(Equipment.DUMBBELL)).map { it.name }
        assertEquals(dumbbell, pickerList("", null, setOf(Equipment.DUMBBELL), stored(), emptyList(), muscles = groups).all.map { it.name })
        assertTrue(shows("exercise-DB press"))
        assertFalse(shows("exercise-Bench"))
        // A dumbbell exercise for another muscle stays out.
        assertTrue(Library.find("Hammer curl")!!.equipment.contains(Equipment.DUMBBELL))
        assertFalse(shows("exercise-Hammer curl"))

        // And the search narrows that again, without leaving the muscles.
        search("press")
        val pressed = Library.search("press", emptySet(), setOf(Equipment.DUMBBELL)).map { it.name }.filter { it in dumbbell }
        assertTrue(pressed.isNotEmpty() && pressed.size < dumbbell.size)
        assertEquals(pressed, pickerList("press", null, setOf(Equipment.DUMBBELL), stored(), emptyList(), muscles = groups).all.map { it.name })
        for (name in pressed) assertTrue(name, shows("exercise-$name"))
        for (name in dumbbell - pressed.toSet()) assertFalse(name, shows("exercise-$name"))
        assertTrue(has("muscle-chip-CHEST") || run { scrollToTop(); has("muscle-chip-CHEST") })

        // The body area counts too.
        search("")
        tapChip("area-ARMS")
        val arms = dumbbell.filter { Library.find(it)!!.area == com.powertracker.domain.library.BodyArea.ARMS }
        assertTrue(arms.isNotEmpty() && arms.size < dumbbell.size)
        for (name in arms) assertTrue(name, shows("exercise-$name"))
        assertFalse(shows("exercise-DB press"))
    }

    @Test
    fun musclesDropdown_chipsRemoveAndClear() {
        show()
        assertFalse(has("muscles-clear"))
        tick(MuscleGroup.CHEST, MuscleGroup.QUADS)

        scrollToTop()
        compose.onNodeWithTag("muscle-chip-CHEST").assertTextContains("Chest").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("muscle-chip-QUADS").assertTextContains("Quads")
        compose.onNodeWithTag("muscles-clear").assertTextContains("Clear").assertHeightIsAtLeast(48.dp)
        assertTrue(shows("exercise-Bench") && shows("exercise-Squat"))

        // A chip takes its own group away.
        scrollToTop()
        compose.onNodeWithTag("muscle-chip-CHEST").performClick()
        compose.waitForIdle()
        assertFalse(has("muscle-chip-CHEST"))
        assertTrue(has("muscle-chip-QUADS"))
        assertTrue(shows("exercise-Squat"))
        assertFalse(shows("exercise-Bench"))
        // The drop-down agrees.
        tapChip("muscles-dropdown")
        waitFor { has("muscles-sheet") }
        compose.onNodeWithTag("muscle-option-CHEST").assertIsOff()
        compose.onNodeWithTag("muscle-option-QUADS").assertIsOn()
        compose.onNodeWithTag("muscle-option-GLUTES").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        waitFor { !has("muscles-sheet") }
        scrollToTop()
        assertTrue(has("muscle-chip-GLUTES"))

        // "Clear" takes them all, and everything is listed again by body area.
        compose.onNodeWithTag("muscles-clear").performClick()
        compose.waitForIdle()
        assertFalse(has("muscle-chip-QUADS") || has("muscle-chip-GLUTES") || has("muscles-clear"))
        assertTrue(shows("exercise-Bench") && shows("exercise-Squat") && shows("area-heading-CORE"))
    }

    @Test
    fun tapRow_opensGuideWithActions() {
        show()
        val before = runBlocking { db.exercises().all() }

        openGuide("Squat")
        val entry = Library.find("Squat")!!
        assertTrue(inGuide(entry.guide.purpose) && inGuide(entry.guide.steps.first()))
        // The safety line is still the last of the guide; the actions come after it.
        val safety = compose.onNodeWithTag("guide-safety").fetchSemanticsNode().boundsInRoot
        val actions = compose.onNodeWithTag("guide-actions").fetchSemanticsNode().boundsInRoot
        assertTrue("actions (${actions.top}) are below the safety line (${safety.bottom})", actions.top >= safety.bottom)
        compose.onNode(hasText("Add to plan") and hasAnyAncestor(hasTestTag("guide-actions"))).assertExists().assertHeightIsAtLeast(48.dp)
        // No session today: nothing to add to.
        assertFalse(says("Add to today"))
        compose.onNodeWithContentDescription("Close guide").performClick()
        waitFor { !compose.guideIsOpen() }

        // The info button opens the same sheet.
        compose.onAllNodesWithContentDescription("Form guide for Squat")[0].assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        waitFor { compose.guideIsOpen() }
        assertTrue(inGuide(entry.guide.purpose))
        compose.onNode(hasText("Add to plan") and hasAnyAncestor(hasTestTag("guide-actions"))).assertExists()
        compose.onNodeWithContentDescription("Close guide").performClick()
        waitFor { !compose.guideIsOpen() }

        // Reading a guide stores nothing.
        assertEquals(before, runBlocking { db.exercises().all() })
        assertTrue(plan().isEmpty())
    }

    @Test
    fun addToPlan_choosesDay_andUsesDefaults() {
        runBlocking {
            settings.update { it.copy(trainingDays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), defaultRpe = 7.5) }
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, id("Bench"), sets = 4, reps = 6, targetRpe = 8.0, order = 3))
        }
        show()
        val accessory = Library.find("Hammer curl")!!
        assertEquals(Kind.ACCESSORY, accessory.kind)
        assertTrue(rows("Hammer curl").isEmpty())

        // The training days, in weekday order, and no others.
        openGuide("Hammer curl")
        chooseDay()
        for (day in listOf("MONDAY", "WEDNESDAY", "FRIDAY")) compose.onNodeWithTag("day-$day").assertHeightIsAtLeast(48.dp)
        val tops = listOf("MONDAY", "WEDNESDAY", "FRIDAY").map { compose.onNodeWithTag("day-$it").fetchSemanticsNode().positionInRoot.y }
        assertEquals(tops.sorted(), tops)
        for (day in listOf("TUESDAY", "THURSDAY", "SATURDAY", "SUNDAY")) assertFalse(has("day-$day"))

        compose.onNodeWithTag("day-WEDNESDAY").performClick()
        waitFor { says("Added to Wednesday") }
        assertFalse(compose.guideIsOpen() || has("day-chooser"))
        // An accessory joins at 3 × 10 and the default RPE, after what the day already has; its row comes with it.
        val row = rows("Hammer curl").single()
        assertTrue(row.isBuiltIn)
        assertEquals(PlanItem(0, DayOfWeek.WEDNESDAY, row.id, sets = 3, reps = 10, targetRpe = 7.5, order = 4), plan().last().copy(id = 0))
        assertEquals(2, plan().size)

        compose.onNodeWithText("Open Plan").performClick()
        waitFor { opened.toList() == listOf("plan") }

        // A compound lift joins at 3 × 5.
        openGuide("Squat")
        chooseDay()
        compose.onNodeWithTag("day-MONDAY").performClick()
        waitFor { says("Added to Monday") }
        assertEquals(PlanItem(0, DayOfWeek.MONDAY, id("Squat"), sets = 3, reps = 5, targetRpe = 7.5, order = 0), plan().first { it.weekday == DayOfWeek.MONDAY }.copy(id = 0))
        assertEquals(1, rows("Squat").size)

        // No training days set: all seven.
        runBlocking { settings.update { it.copy(trainingDays = emptySet()) } }
        openGuide("Deadlift")
        chooseDay()
        for (day in DayOfWeek.entries) compose.onNodeWithTag("day-${day.name}").assertExists()
        assertEquals(DayOfWeek.entries.toList(), planDayChoices(emptySet()))
    }

    @Test
    fun addToPlan_alreadyOnDay_saysSo() {
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, id("Squat"), sets = 5, reps = 3, targetRpe = 8.0, order = 0)) }
        show()
        val before = plan()

        openGuide("Squat")
        chooseDay()
        compose.onNodeWithTag("day-MONDAY").performClick()
        waitFor { says("Already in your plan on Monday.") }
        assertFalse(says("Open Plan"))
        assertEquals(before, plan())

        // Another day is not a duplicate.
        openGuide("Squat")
        chooseDay()
        compose.onNodeWithTag("day-FRIDAY").performClick()
        waitFor { says("Added to Friday") }
        assertEquals(2, plan().size)
    }

    @Test
    fun addToToday_onlyDuringOpenSession() {
        show()

        // Before the session: only the plan.
        openGuide("Hammer curl")
        assertTrue(says("Add to plan"))
        assertFalse(says("Add to today"))

        // The session starts (on the Today tab): the open sheet gets the second action.
        startSession()
        waitFor { says("Add to today") }
        compose.onNode(hasText("Add to today") and hasAnyAncestor(hasTestTag("guide-actions"))).assertHeightIsAtLeast(48.dp).performClick()
        waitFor { says("Added to today") }
        assertFalse(compose.guideIsOpen())
        assertEquals(listOf("Hammer curl"), todayVm.state.value.items.map { it.exercise.name })
        compose.onNodeWithText("Open Today").performClick()
        waitFor { opened.toList() == listOf("today") }

        // Already there: said, not added twice.
        openGuide("Hammer curl")
        compose.onNodeWithText("Add to today").performClick()
        waitFor { says("Already in today's session.") }
        assertFalse(says("Open Today"))
        assertEquals(1, todayVm.state.value.items.size)

        // Finished: the action goes again.
        openGuide("Squat")
        assertTrue(says("Add to today"))
        todayVm.finishSession()
        waitFor { todayVm.state.value.summary != null }
        waitFor { !says("Add to today") }
        assertTrue(says("Add to plan"))
    }

    @Test
    fun addToToday_createsNoRowUntilFirstSet() {
        startSession()
        show()
        val before = runBlocking { db.exercises().all() }
        assertTrue(rows("Hammer curl").isEmpty())

        openGuide("Hammer curl")
        compose.onNodeWithText("Add to today").performClick()
        waitFor { says("Added to today") }

        // On Today under a stand-in id, with nothing stored.
        val item = todayVm.state.value.items.single()
        assertEquals("Hammer curl", item.exercise.name)
        assertTrue(item.exercise.id < 0)
        assertEquals(before, runBlocking { db.exercises().all() })
        assertTrue(plan().isEmpty())

        // The first set brings the row, the way Today's own "Add exercise" does.
        todayVm.logSet(null, item.exercise.id, LoggedSet(item.exercise.id, 14.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false))
        waitFor { rows("Hammer curl").size == 1 }
        val row = rows("Hammer curl").single()
        waitFor { todayVm.state.value.items.singleOrNull()?.let { it.exercise.id == row.id && it.sets.size == 1 } == true }
        assertTrue(row.isBuiltIn)

        // One the lifter already has a row for is added under that row.
        openGuide("Squat")
        compose.onNodeWithText("Add to today").performClick()
        waitFor { todayVm.state.value.items.size == 2 }
        assertEquals(id("Squat"), todayVm.state.value.items.last().exercise.id)
        assertEquals(1, rows("Squat").size)
    }

    @Test
    fun addToToday_afterFinish_doesNotAdd() {
        startSession()
        show()
        openGuide("Hammer curl")
        assertTrue(says("Add to today"))
        // The button as it was while the session was open: what a tap in flight would still run.
        val staleTap = compose.onNodeWithText("Add to today").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!

        // The session is finished in another view.
        todayVm.finishSession()
        waitFor { todayVm.state.value.summary != null }

        // A tap that lands anyway is checked against the session as it is now: nothing is added, and it says so.
        compose.runOnUiThread { staleTap() }
        waitFor { says(SAVE_FAILED) }
        assertFalse(says("Added to today") || says("Open Today"))
        assertTrue(todayVm.state.value.items.isEmpty())

        // The view model refuses on its own, too, whoever asks.
        val results = mutableListOf<TodayAdd>()
        todayVm.addToOpenSession("Hammer curl", null) { results += it }
        waitFor { results.size == 1 }
        todayVm.addToOpenSession(null, id("Squat")) { results += it }
        waitFor { results.size == 2 }
        assertEquals(listOf(TodayAdd.REFUSED, TodayAdd.REFUSED), results)

        // And the button is gone from the sheet.
        openGuide("Squat")
        assertFalse(says("Add to today"))

        // Back to logging: the finished session did not quietly pick anything up.
        todayVm.dismissSummary()
        waitFor { todayVm.state.value.summary == null }
        assertTrue(todayVm.state.value.items.isEmpty())
        assertTrue(rows("Hammer curl").isEmpty())
    }

    @Test
    fun actions_doubleTap_addOnce() {
        startSession()
        show()

        // Two taps on a day inside one frame: one plan item, and "Added", not "Already in your plan".
        openGuide("Hammer curl")
        chooseDay()
        doubleTap(compose.onNodeWithTag("day-MONDAY"))
        waitFor { plan().isNotEmpty() }
        waitFor { says("Added to Monday") }
        assertEquals(1, plan().size)
        assertEquals(1, rows("Hammer curl").size)
        assertFalse(says("Already in your plan on Monday."))

        // Two taps on "Add to plan" open one chooser.
        openGuide("Squat")
        doubleTap(compose.onNodeWithText("Add to plan"))
        waitFor { has("day-chooser") }
        assertEquals(1, compose.onAllNodes(hasTestTag("day-chooser")).fetchSemanticsNodes().size)
        compose.onNodeWithContentDescription("Close").performClick()
        waitFor { !has("day-chooser") }

        // Two taps on "Add to today": one card, and "Added", not "Already in today's session".
        openGuide("Goblet squat")
        doubleTap(compose.onNodeWithText("Add to today"))
        waitFor { says("Added to today") }
        assertEquals(listOf("Goblet squat"), todayVm.state.value.items.map { it.exercise.name })
        assertFalse(says("Already in today's session."))

        // The view models hold their own guard, whatever screen calls them: two calls in a row answer once.
        val planned = mutableListOf<PlanAdd>()
        val added = mutableListOf<TodayAdd>()
        val planVm = PlanViewModel(training, settings) { today }.also { viewModels += it }
        val curl = pickerList("", null, emptySet(), stored(), emptyList()).let { it.yours + it.all }.first { it.name == "Curl" }
        compose.runOnUiThread {
            repeat(2) { planVm.addOnce(DayOfWeek.FRIDAY, curl) { planned += it } }
            repeat(2) { todayVm.addToOpenSession(curl.catalogueName, curl.exerciseId) { added += it } }
        }
        waitFor { planned.isNotEmpty() && added.isNotEmpty() }
        compose.waitForIdle()
        assertEquals(listOf(PlanAdd.ADDED), planned.toList())
        assertEquals(listOf(TodayAdd.ADDED), added.toList())
        assertEquals(1, plan().count { it.weekday == DayOfWeek.FRIDAY })
        assertEquals(2, todayVm.state.value.items.size)
    }

    @Test
    fun failedWrite_showsMessage_andChangesNothing() {
        val failing = object : TrainingRepository(db) {
            override suspend fun savePlanItem(item: PlanItem, catalogueName: String?): Long = inTransaction {
                // Fails after the exercise row was written, inside the transaction that must take it back.
                catalogueName?.let { resolveCatalogue(it) }
                throw IllegalStateException("disk full")
            }
        }
        val before = runBlocking { db.exercises().all() }
        show(failing)

        openGuide("Hammer curl")
        chooseDay()
        compose.onNodeWithTag("day-MONDAY").performClick()
        waitFor { says("Couldn't save — try again.") }
        assertEquals("Couldn't save — try again.", SAVE_FAILED)
        assertFalse(says("Added to Monday") || says("Open Plan"))
        assertTrue(plan().isEmpty())
        assertEquals(before, runBlocking { db.exercises().all() })

        // Not stuck: the next try is taken (and fails the same way), rather than swallowed by the double-tap guard.
        openGuide("Squat")
        chooseDay()
        compose.onNodeWithTag("day-FRIDAY").performClick()
        waitFor { says("Couldn't save — try again.") }
        assertTrue(plan().isEmpty())
    }

    @Test
    fun customExercise_showsNoGuideText_withActions() {
        val own = runBlocking { training.addExercise(Exercise(0, "Tyre flip", Category.ACCESSORY, null, listOf("Quads", "Glutes"), isBuiltIn = false)) }
        startSession()
        show()

        // Under "Your exercises", as in the picker.
        compose.onNodeWithText("YOUR EXERCISES").assertExists()
        openGuide("Tyre flip")
        assertTrue(inGuide(NO_GUIDE))
        compose.onNodeWithText(Library.SAFETY_LINE).assertExists()
        compose.onNode(hasText("Add to plan") and hasAnyAncestor(hasTestTag("guide-actions"))).assertExists()
        compose.onNode(hasText("Add to today") and hasAnyAncestor(hasTestTag("guide-actions"))).assertExists()

        // Both work on the lifter's own row, and make no other.
        chooseDay()
        compose.onNodeWithTag("day-MONDAY").performClick()
        waitFor { says("Added to Monday") }
        assertEquals(PlanItem(0, DayOfWeek.MONDAY, own, sets = 3, reps = 10, targetRpe = AppSettings().defaultRpe, order = 0), plan().single().copy(id = 0))

        openGuide("Tyre flip")
        compose.onNodeWithText("Add to today").performClick()
        waitFor { says("Added to today") }
        assertEquals(listOf(own), todayVm.state.value.items.map { it.exercise.id })
        assertEquals(1, rows("Tyre flip").size)
    }

    @Test
    fun tabState_survivesSwitchingTabs() {
        compose.setContent { PowertrackerTheme { AppShell(training, advice, settings, BackupService(db, settings)) } }
        waitFor { has("tab-exercises") }
        compose.onNodeWithTag("tab-exercises").performClick()
        waitFor { has("exercise-list") }
        compose.onNodeWithTag("tab-exercises").assertIsSelected()

        search("curl")
        tapChip("equipment-DUMBBELL")
        tapChip("area-ARMS")
        tick(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
        assertTrue(shows("exercise-Hammer curl"))
        assertFalse(shows("exercise-Squat"))

        // Over to Plan and back, and to Today and back.
        for (other in listOf("tab-plan", "tab-today")) {
            compose.onNodeWithTag(other).performClick()
            waitFor { !has("exercise-list") }
            compose.onNodeWithTag("tab-exercises").performClick()
            waitFor { has("exercise-list") }

            scrollToTop()
            compose.onNodeWithTag("exercise-search").assertTextContains("curl")
            compose.onNodeWithTag("equipment-DUMBBELL").performScrollTo().assertIsOn()
            compose.onNodeWithTag("equipment-BARBELL").performScrollTo().assertIsOff()
            compose.onNodeWithTag("area-ARMS").performScrollTo().assertIsOn()
            assertTrue(has("muscle-chip-BICEPS") && has("muscle-chip-FOREARMS"))
            assertTrue(shows("exercise-Hammer curl"))
            assertFalse(shows("exercise-Squat"))
        }

        // The tab adds to the session the Today tab shows: one Today view model between them.
        startSessionToday()
        scrollToTop()
        assertTrue(shows("exercise-Hammer curl"))
        compose.onNodeWithTag("exercise-Hammer curl").performClick()
        waitFor { says("Add to today") }
        compose.onNodeWithText("Add to today").performClick()
        waitFor { says("Open Today") }
        compose.onNodeWithText("Open Today").performClick()
        waitFor { !has("exercise-list") }
        compose.onNodeWithTag("tab-today").assertIsSelected()
        waitFor { says("Hammer curl") }
        assertTrue(rows("Hammer curl").isEmpty())
    }

    /** A session on the device's own today: the shell runs on the real clock. */
    private fun startSessionToday() = runBlocking { training.startSession(com.powertracker.platform.systemToday(), null, null) }

    // ---- The buzz (stage 2): Add to plan and Add to today tick once, when the add is accepted ----

    @Test
    fun guideSheet_addToPlan_andAddToToday_tickOnce_evenOnADoubleTap() {
        val haptics = FakeHapticFeedback()
        startSession()
        todayVm = TodayViewModel(training, advice, settings) { today }.also { viewModels += it }
        compose.setContent {
            PowertrackerTheme {
                WithHaptics(haptics) {
                    ExercisesScreen(training, settings, todayVm, onOpenPlan = {}, onOpenToday = {}, today = { today })
                }
            }
        }
        waitFor { has("exercise-list") && !todayVm.state.value.loading }

        // Opening a guide and the day chooser only shows things: no buzz.
        openGuide("Hammer curl")
        chooseDay()
        assertEquals(0, haptics.calls.size)
        doubleTap(compose.onNodeWithTag("day-MONDAY"))
        waitFor { says("Added to Monday") }
        assertEquals(listOf(HapticFeedbackType.ContextClick), haptics.calls.toList())

        openGuide("Goblet squat")
        waitFor { says("Add to today") }
        doubleTap(compose.onNodeWithText("Add to today"))
        waitFor { says("Added to today") }
        assertEquals(2, haptics.ticks)
        assertEquals(2, haptics.calls.size)
    }

    @Test
    fun guideSheet_withVibrateOnTapsOff_addsWithoutABuzz() {
        val haptics = FakeHapticFeedback()
        todayVm = TodayViewModel(training, advice, settings) { today }.also { viewModels += it }
        compose.setContent {
            PowertrackerTheme {
                WithHaptics(haptics, vibrateOnTaps = false) {
                    ExercisesScreen(training, settings, todayVm, onOpenPlan = {}, onOpenToday = {}, today = { today })
                }
            }
        }
        waitFor { has("exercise-list") && !todayVm.state.value.loading }
        openGuide("Hammer curl")
        chooseDay()
        compose.onNodeWithTag("day-MONDAY").performClick()
        waitFor { says("Added to Monday") }
        assertEquals(0, haptics.calls.size)
    }
}
