package com.powertracker.ui.today

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.powertracker.data.StoredSet
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.plan.DaySection
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanRow
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.plan.planItemText
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Layout motion (stage 3): with motion on and the clock driven by hand, a logged set slides in, a deleted one slides
 * out, the open card changes by sliding, Plan's rows slide past each other, and three quick taps on Log set log three
 * sets in order. Everything has settled by 350 ms. With motion off, nothing moves: the end state is there at once.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class LayoutMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private val date = LocalDate(2026, 11, 16)
    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)
    private val bench = Exercise(2, "Bench", Category.BENCH, null, emptyList(), isBuiltIn = true)
    private val row = Exercise(3, "Barbell row", Category.ACCESSORY, null, emptyList(), isBuiltIn = true)

    private fun stored(id: Long, exercise: Exercise, kg: Double) = StoredSet(id, 1, LoggedSet(exercise.id, kg, 5, 8.0, false, null, isTopSet = false))

    private fun item(exercise: Exercise, sets: List<StoredSet> = emptyList(), target: Int = 5) = TodayItem(
        exercise = exercise,
        planned = PlanItem(exercise.id * 10, DayOfWeek.MONDAY, exercise.id, target, 5, 8.0, exercise.id.toInt()),
        suggestion = null,
        sets = sets,
        editor = SetEditor(100.0, 5, 8.0),
        targetSets = target,
        perSide = null,
        lastWeightKg = 100.0,
    )

    private var today by mutableStateOf(TodayState(date, loading = false, sessionId = 1))
    private val logged = mutableListOf<Int>()

    /** Today, with Log set and Delete set changing the state at once, as the view model does once saved. */
    private fun showToday(start: TodayState, motion: Boolean = true) {
        today = start
        val actions = TodayActions(
            onLogSet = { tapped, _ ->
                val items = today.items.map { current ->
                    if (current.exercise.id != tapped.exercise.id) return@map current
                    val number = current.sets.size + 1
                    logged += number
                    current.copy(sets = current.sets + stored(100L + number, current.exercise, 100.0 + number))
                }
                today = today.copy(items = items)
            },
            onDeleteSet = { id -> today = today.copy(items = today.items.map { it.copy(sets = it.sets.filter { set -> set.id != id }) }) },
        )
        show(motion) { TodayContent(today, actions) }
    }

    private fun show(motion: Boolean, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { PowertrackerTheme { CompositionLocalProvider(LocalMotion provides motion, content = content) } }
        compose.mainClock.advanceTimeBy(1_000)
    }

    private fun SemanticsNodeInteraction.bounds(): Rect = fetchSemanticsNode().boundsInRoot

    private fun setRows(number: Int) = compose.onAllNodes(hasText("Set $number:", substring = true)).fetchSemanticsNodes()

    private fun advance(ms: Long) = compose.mainClock.advanceTimeBy(ms)

    @Test
    fun theMoveSpring_settlesInAbout280ms_andNeverPast350() {
        val spring = moveSpring(1f).vectorize(Float.VectorConverter)
        fun ms(px: Float) = spring.getDurationNanos(AnimationVector1D(0f), AnimationVector1D(px), AnimationVector1D(0f)) / 1_000_000
        // A card-sized move (100 dp at xxhdpi, in pixels) takes about 280 ms.
        assertTrue("a card's move takes ${ms(300f)} ms", ms(300f) in 250..310)
        // From a set row (48 dp) to more than a full card opens or closes by (600 dp): never past 350 ms.
        for (px in listOf(144f, 300f, 900f, 1800f)) assertTrue("a $px px move takes ${ms(px)} ms", ms(px) <= 350)
        assertEquals(150, FADE_MS)
    }

    @Test
    fun aLoggedSet_slidesIn_andSettles() {
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, listOf(stored(1, squat, 100.0))))))
        // Missed sits beside Log set and is not pressed, so its bounds show only the slide.
        val before = compose.onNodeWithText("Missed").bounds()

        compose.onNodeWithText("Log set 2").performClick()
        advance(64)
        // The new row is there, still opening: the buttons below it are on their way down.
        assertEquals(1, setRows(2).size)
        val mid = compose.onNodeWithText("Missed").bounds()
        // 350 ms from the tap: settled.
        advance(286)
        val end = compose.onNodeWithText("Missed").bounds()
        assertTrue("mid ${mid.top} between ${before.top} and ${end.top}", mid.top > before.top && mid.top < end.top)
        advance(1_000)
        assertEquals(end, compose.onNodeWithText("Missed").bounds())
        assertEquals(1, setRows(2).size)
        assertEquals(listOf(2), logged)
    }

    @Test
    fun aDeletedSet_slidesOut_andGoes() {
        val sets = listOf(stored(1, squat, 100.0), stored(2, squat, 102.5))
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, sets))))
        val before = compose.onNodeWithText("Missed").bounds()

        compose.onNode(hasText("Set 2:", substring = true)).performClick()
        advance(1_000)
        compose.onNodeWithText("Delete set").performClick()
        advance(64)
        // Still closing: the buttons are on their way up. The row going is not read out.
        assertEquals(0, setRows(2).size)
        val mid = compose.onNodeWithText("Missed").bounds()
        // 350 ms from the tap: settled.
        advance(286)
        val end = compose.onNodeWithText("Missed").bounds()
        assertTrue("mid ${mid.top} between ${end.top} and ${before.top}", mid.top < before.top && mid.top > end.top)
        advance(1_000)
        assertEquals(end, compose.onNodeWithText("Missed").bounds())
        compose.onNodeWithText("Log set 2").bounds()
        assertEquals(1, setRows(1).size)
    }

    /** Whether the band of [box] below [fromTop] is the card's colour all down its middle, not the page's. */
    private fun surfaceFills(box: Rect, fromTop: Float): Boolean {
        val view = (compose.onRoot().fetchSemanticsNode().root as androidx.compose.ui.platform.ViewRootForTest).view
        val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val page = bitmap.getPixel(2, (box.top + fromTop).toInt())
        val x = box.left.toInt() + 6
        val ys = ((box.top + fromTop).toInt() until (box.bottom - 8).toInt())
        return ys.count { bitmap.getPixel(x, it) != page } > ys.count() * 9 / 10
    }

    private fun itemBounds(exercise: Exercise) = compose.onNode(hasTestTag("exercise-${exercise.id}")).bounds()

    @Test
    fun theOpenCard_changesBySliding_theClosingOneShrinksWithItsContent() {
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, listOf(stored(1, squat, 100.0))), item(bench), item(row))))
        val squatOpen = itemBounds(squat).height
        val rowClosed = itemBounds(bench).height

        compose.onNodeWithText("Barbell row").performClick()
        advance(16)
        // Frame by frame up to 350 ms from the tap: Squat's card closes through every height between open and a row,
        // and nowhere in the list is there a gap taller than one row.
        val heights = mutableListOf<Float>()
        var closingCardWasDrawn = false
        for (t in 1..21) {
            val items = listOf(squat, bench, row).map { itemBounds(it) }
            heights += items[0].height
            // Below where the row will be, the closing box shows the card's surface, not the page behind it.
            if (items[0].height > rowClosed + 40) closingCardWasDrawn = surfaceFills(items[0], fromTop = rowClosed + 8)
            items.zipWithNext { a, b -> assertTrue("gap ${b.top - a.bottom} at ${t * 16} ms", b.top - a.bottom < rowClosed) }
            advance(16)
        }
        assertTrue("Squat closing: $heights", heights.any { it < squatOpen - 10 && it > rowClosed + 10 })
        assertTrue("the closing card is drawn, not a blank", closingCardWasDrawn)
        assertTrue("never below a row: $heights", heights.all { it >= rowClosed - 1 })
        // Settled by 350 ms: Squat is its row, the row's card is open.
        val settled = listOf(squat, bench, row).map { itemBounds(it) }
        assertEquals(rowClosed, settled[0].height, 1f)
        advance(1_000)
        assertEquals(settled, listOf(squat, bench, row).map { itemBounds(it) })
        assertEquals(1, compose.onAllNodes(hasText("Log set 1")).fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodes(hasText("1 of 5")).fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun narrowPhone_atFontScale130_openingAnotherExercise_endsWithNothingCut() {
        today = TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, listOf(stored(1, squat, 100.0))), item(bench)))
        show(motion = true) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.3f)) {
                TodayContent(today, TodayActions())
            }
        }
        compose.onNodeWithText("Bench").performClick()
        advance(1_000)
        // Every part of the open card and of the closed row lies inside its item, which nothing is drawn past.
        for (exercise in listOf(squat, bench)) {
            val box = itemBounds(exercise)
            compose.onAllNodes(hasAnyAncestor(hasTestTag("exercise-${exercise.id}"))).fetchSemanticsNodes().forEach { node ->
                val b = node.boundsInRoot
                assertTrue("${node.config} at $b outside $box", b.top >= box.top - 0.5f && b.bottom <= box.bottom + 0.5f && b.left >= box.left - 0.5f && b.right <= box.right + 0.5f)
            }
        }
        val root = compose.onRoot().bounds()
        assertTrue(itemBounds(bench).right <= root.right)
        compose.onNodeWithText("Log set 1").bounds()
    }

    @Test
    fun logSet_timedFromTapToTheRowInPlace() {
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, listOf(stored(1, squat, 100.0))))))
        compose.onNodeWithText("Log set 2").performClick()
        // Where Missed sits, frame by frame, for a second.
        val frames = (0 until 63).map { advance(16); setRows(2).size to compose.onNodeWithText("Missed").bounds() }
        val shownAt = (frames.indexOfFirst { it.first == 1 } + 1) * 16
        val final = frames.last().second
        val ms = (frames.indexOfFirst { it.second == final } + 1) * 16
        println("LOGSET row shown at $shownAt ms, in place at $ms ms")
        assertTrue("row shown at $shownAt ms", shownAt <= 32)
        assertTrue("in place at $ms ms", ms <= 350)
    }

    @Test
    fun threeQuickLogs_logThreeSets_inOrder() {
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat))))

        compose.onNodeWithText("Log set 1").performClick()
        advance(16)
        compose.onNodeWithText("Log set 2").performClick()
        advance(16)
        compose.onNodeWithText("Log set 3").performClick()
        advance(1_000)

        assertEquals(listOf(1, 2, 3), logged)
        for (n in 1..3) assertEquals("set $n drawn once", 1, setRows(n).size)
        val tops = (1..3).map { setRows(it).single().boundsInRoot.top }
        assertEquals(tops.sorted(), tops)
        compose.onNodeWithText("Log set 4").bounds()
    }

    @Test
    fun anotherExerciseOpened_whileASetSlidesIn_losesNothing() {
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat), item(bench))))

        compose.onNodeWithText("Log set 1").performClick()
        advance(32)
        compose.onNodeWithText("Bench").performClick()
        advance(1_000)

        assertEquals(listOf(1), logged)
        // Squat is a closed row with its one set; Bench is open, drawn once, with nothing logged.
        assertEquals(1, compose.onAllNodes(hasText("1 of 5")).fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodes(hasText("Log set 1")).fetchSemanticsNodes().size)
        assertEquals(0, setRows(1).size)
        assertEquals(1, compose.onAllNodes(hasText("EXERCISE 2 OF 2")).fetchSemanticsNodes().size)
    }

    @Test
    fun deleteTheLastSet_thenLogOneAtOnce_endsWithTheNewSetOnly() {
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, listOf(stored(1, squat, 90.0))))))

        compose.onNode(hasText("Set 1:", substring = true)).performClick()
        advance(1_000)
        compose.onNodeWithText("Delete set").performClick()
        advance(32)
        compose.onNodeWithText("Log set 1").performClick()
        advance(1_000)

        assertEquals(listOf(1), logged)
        assertEquals(listOf("Set 1: 101 kg × 5 @ 8, done"), setRows(1).map { it.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString() })
    }

    @Test
    fun aCatalogueExercise_keepsItsCard_whenItsRowIsSaved() {
        // Added from the catalogue before it had a row: shown under a stand-in id until its first set saves the row.
        val standIn = squat.copy(id = -7)
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(standIn).copy(planned = null, targetSets = null))))
        val before = compose.onNodeWithText("Missed").bounds()
        compose.runOnUiThread {
            today = today.copy(items = listOf(item(squat, listOf(stored(1, squat, 100.0))).copy(planned = null, targetSets = null)), resolved = mapOf(-7L to squat.id))
        }
        compose.waitForIdle()
        advance(64)
        // The same card, its first set sliding in; not a new card fading in with the set already there.
        val mid = compose.onNodeWithText("Missed").bounds()
        advance(1_000)
        val end = compose.onNodeWithText("Missed").bounds()
        assertTrue("mid ${mid.top} between ${before.top} and ${end.top}", mid.top > before.top && mid.top < end.top)
        assertEquals(1, compose.onAllNodes(hasText("EXERCISE 1 OF 1")).fetchSemanticsNodes().size)
        assertEquals(1, setRows(1).size)
    }

    @Test
    fun anAddedExercise_fadesIn_andWhatIsBelowSlides() {
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat), item(bench))))
        val before = compose.onNodeWithText("Add exercise").bounds()
        compose.runOnUiThread { today = today.copy(items = today.items + item(row)) }
        compose.waitForIdle()
        advance(64)
        val mid = compose.onNodeWithText("Add exercise").bounds()
        advance(286)
        val end = compose.onNodeWithText("Add exercise").bounds()
        assertTrue("mid ${mid.top} between ${before.top} and ${end.top}", mid.top > before.top && mid.top < end.top)
        advance(1_000)
        assertEquals(end, compose.onNodeWithText("Add exercise").bounds())
        assertEquals(1, compose.onAllNodes(hasText("Barbell row")).fetchSemanticsNodes().size)
    }

    @Test
    fun aRemovedExercise_fadesOut_takesNoTaps_andWhatIsBelowSlidesUp() {
        // An exercise added today with nothing logged has Remove; Bench below it has a set, so its button is "Log set 2".
        val added = item(squat).copy(planned = null, targetSets = null)
        var removed = 0
        today = TodayState(date, loading = false, sessionId = 1, items = listOf(added, item(bench, listOf(stored(1, bench, 100.0)))))
        val actions = TodayActions(
            onLogSet = { tapped, _ -> logged += tapped.exercise.id.toInt() },
            onRemoveExercise = { id ->
                removed++
                today = today.copy(items = today.items.filter { it.exercise.id != id })
            },
        )
        show(motion = true) { TodayContent(today, actions) }
        val before = compose.onNodeWithText("Add exercise").bounds()
        val logButton = compose.onNodeWithText("Log set 1").bounds()

        compose.onNodeWithText("Remove").performClick()
        advance(32)
        // A tap where the leaving card's Log set was never logs a set for it.
        compose.onRoot().performTouchInput { click(logButton.center) }
        advance(32)
        val mid = compose.onNodeWithText("Add exercise").bounds()
        advance(286)
        val end = compose.onNodeWithText("Add exercise").bounds()
        assertTrue("mid ${mid.top} between ${end.top} and ${before.top}", mid.top < before.top && mid.top > end.top)
        advance(1_000)
        assertEquals(end, compose.onNodeWithText("Add exercise").bounds())
        assertEquals(1, removed)
        assertTrue("logged $logged", squat.id.toInt() !in logged)
        assertEquals(0, compose.onAllNodes(hasText("Squat")).fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun next_onASmallPhone_scrollsToTheOpenedCard_whileItSlides() {
        val more = (4L..6L).map { Exercise(it, "Accessory $it", Category.ACCESSORY, null, emptyList(), isBuiltIn = true) }
        val lastSetLeft = item(squat, listOf(stored(1, squat, 100.0)), target = 2)
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(lastSetLeft, item(bench), item(row)) + more.map { item(it) }))
        compose.onNodeWithText("Log set 2").performClick()
        advance(1_000)

        compose.onNodeWithText("Next: Bench").performClick()
        advance(96)
        val root = compose.onRoot().bounds()
        advance(1_000)
        val heading = compose.onNodeWithText("Bench").bounds()
        val log = compose.onNodeWithText("Log set 1").bounds()
        // Bench is open and in view, and has stopped moving.
        assertTrue("Bench at $heading in $root", heading.top >= root.top && heading.bottom <= root.bottom)
        assertTrue("Log set 1 at $log in $root", log.top >= root.top && log.bottom <= root.bottom)
        advance(1_000)
        assertEquals(heading, compose.onNodeWithText("Bench").bounds())
        assertEquals(1, compose.onAllNodes(hasText("Log set 1")).fetchSemanticsNodes().size)
    }

    @Test
    fun withMotionOff_aLoggedSetIsThereAtOnce() {
        showToday(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, listOf(stored(1, squat, 100.0))))), motion = false)
        compose.onNodeWithText("Log set 2").performClick()
        // The first frame that has the new set has everything in its final place.
        while (setRows(2).isEmpty()) advance(16)
        val first = compose.onNodeWithText("Missed").bounds()
        advance(1_000)
        assertEquals(first, compose.onNodeWithText("Missed").bounds())
        assertEquals(listOf(2), logged)
        assertEquals(1, setRows(2).size)
    }

    // Plan

    private val planItems = listOf(squat, bench, row).mapIndexed { i, e -> PlanItem(10L + i, DayOfWeek.MONDAY, e.id, 3, 5, 8.0, i) }
    private val names = mapOf(squat.id to squat.name, bench.id to bench.name, row.id to row.name)
    private var plan by mutableStateOf(PlanUiState())

    private fun planState(items: List<PlanItem>) = PlanUiState(sections = listOf(DaySection(DayOfWeek.MONDAY, items.map { PlanRow(it, names.getValue(it.exerciseId)) })))

    private fun showPlan() {
        plan = planState(planItems)
        show(motion = true) {
            PlanContent(
                plan,
                onOpenSettings = {},
                onAdd = {},
                onEdit = {},
                onMove = { moved, by ->
                    val items = plan.sections.single().rows.map { it.item }.toMutableList()
                    val i = items.indexOfFirst { it.id == moved.id }
                    items.add(i + by, items.removeAt(i))
                    plan = planState(items)
                },
                onDelete = { gone -> plan = planState(plan.sections.single().rows.map { it.item }.filter { it.id != gone.id }) },
                onSetBaseline = {},
            )
        }
    }

    private fun planRow(item: PlanItem) = compose.onNodeWithText(planItemText(names.getValue(item.exerciseId), item))

    @Test
    fun planMoveDown_andMoveUp_slideTheTwoRowsPastEachOther() {
        showPlan()
        val (a, b) = planItems
        val aTop = planRow(a).bounds().top
        val bTop = planRow(b).bounds().top

        compose.onAllNodesWithContentDescription("Move down")[0].performClick()
        advance(64)
        val aMid = planRow(a).bounds().top
        val bMid = planRow(b).bounds().top
        assertTrue("a moving down: $aTop < $aMid < $bTop", aMid > aTop && aMid < bTop)
        assertTrue("b moving up: $aTop < $bMid < $bTop", bMid > aTop && bMid < bTop)
        // 350 ms from the tap: settled.
        advance(286)
        assertEquals(bTop, planRow(a).bounds().top)
        assertEquals(aTop, planRow(b).bounds().top)

        // Now a is second: move it back up.
        compose.onAllNodesWithContentDescription("Move up")[1].performClick()
        advance(64)
        val aBack = planRow(a).bounds().top
        assertTrue("a moving up: $aTop < $aBack < $bTop", aBack > aTop && aBack < bTop)
        // 350 ms from the tap: settled.
        advance(286)
        assertEquals(aTop, planRow(a).bounds().top)
        assertEquals(bTop, planRow(b).bounds().top)
    }

    @Test
    fun planDelete_slidesTheRowOut_andTheRestCloseUp() {
        showPlan()
        val (a, b, c) = planItems
        val bTop = planRow(b).bounds().top
        val cTop = planRow(c).bounds().top
        compose.runOnUiThread { plan = planState(listOf(a, c)) }
        compose.waitForIdle()
        advance(64)
        val cMid = planRow(c).bounds().top
        assertTrue("c closing up: $bTop < $cMid < $cTop", cMid > bTop && cMid < cTop)
        // 350 ms from the tap: settled.
        advance(286)
        assertEquals(bTop, planRow(c).bounds().top)
        assertEquals(0, compose.onAllNodes(hasText(planItemText(names.getValue(b.exerciseId), b))).fetchSemanticsNodes().size)
    }
}
