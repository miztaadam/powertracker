package com.powertracker.ui.today

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.StoredSet
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.ModeTheme
import com.powertracker.ui.components.TickDrawIn
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The owner's extras on the real Today screen (1.16, Task 2c): a set's tick draws itself in, and the "Skipped today"
 * line rises in, only on the event itself; never on open, restore, scrolling back, a side switch, or with motion off.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class EventMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun resetScale() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    private val date = LocalDate(2026, 11, 16)
    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)
    private val bench = Exercise(2, "Bench", Category.BENCH, null, emptyList(), isBuiltIn = true)
    private val row = Exercise(3, "Barbell row", Category.ACCESSORY, null, emptyList(), isBuiltIn = true)

    private fun stored(id: Long, exercise: Exercise) = StoredSet(id, 1, LoggedSet(exercise.id, 100.0, 5, 8.0, false, null, isTopSet = false))

    private fun item(exercise: Exercise, sets: List<StoredSet> = emptyList()) = TodayItem(
        exercise = exercise,
        planned = PlanItem(exercise.id * 10, DayOfWeek.MONDAY, exercise.id, 5, 5, 8.0, exercise.id.toInt()),
        suggestion = null,
        sets = sets,
        editor = SetEditor(100.0, 5, 8.0),
        targetSets = 5,
        perSide = null,
        lastWeightKg = 100.0,
    )

    private fun state(vararg items: TodayItem, skipped: List<Exercise> = emptyList()) = TodayState(
        date, loading = false, sessionId = 1, items = items.toList(),
        skippedToday = skipped.map { SkippedToday(it.id, it.name, skipId = it.id) },
    )

    private var today by mutableStateOf(TodayState(date, loading = true, sessionId = 1))
    private var themeMode by mutableStateOf(TrainingMode.POWERLIFTING)
    private var run by mutableIntStateOf(0)

    /** Log set stores the editor's set at once, as the view model does once saved: the next set id each time. */
    private val actions = TodayActions(
        onLogSet = { tapped, _ ->
            today = today.copy(items = today.items.map { if (it.exercise.id == tapped.exercise.id) it.copy(sets = it.sets + stored(100L + it.sets.size + 1, it.exercise)) else it })
        },
    )

    private fun show(start: TodayState, motion: Boolean = true, content: @Composable () -> Unit = { TodayContent(today, actions) }) {
        today = start
        compose.mainClock.autoAdvance = false
        compose.setContent {
            key(run) {
                CompositionLocalProvider(LocalMotion provides motion) { PowertrackerTheme(mode = themeMode) { content() } }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
    }

    /** A new state, as the view model would emit it: seen by the next frame. */
    private fun set(next: TodayState) {
        today = next
        Snapshot.sendApplyNotifications()
    }

    /** Today's list: the tallest thing that scrolls (the steppers' fields scroll too). */
    private fun todayList(): androidx.compose.ui.test.SemanticsNodeInteraction {
        val id = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().maxBy { it.boundsInRoot.height }.id
        return compose.onNode(androidx.compose.ui.test.SemanticsMatcher("Today's list") { it.id == id })
    }

    /** What is drawn where set 1's row is, as the screen draws it now. */
    private fun drawnSetRow(): android.graphics.Bitmap {
        val view = (compose.onRoot().fetchSemanticsNode().root as androidx.compose.ui.platform.ViewRootForTest).view
        val whole = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(whole))
        val r = compose.onNode(hasText("Set 1:", substring = true)).fetchSemanticsNode().boundsInRoot
        return android.graphics.Bitmap.createBitmap(whole, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
    }

    private fun tick(number: Int): (() -> Float)? =
        compose.onNode(hasText("Set $number:", substring = true)).fetchSemanticsNode().config.getOrNull(TickDrawIn)

    private fun rise(): (() -> Float)? =
        compose.onNode(hasTestTag("skipped-today")).fetchSemanticsNode().config.getOrNull(RiseIn)

    /** For a while after, frame by frame: [check] each frame. */
    private fun everyFrame(ms: Long = EVENT_MOVE_MS + 200L, check: () -> Unit) {
        // From the first frames that draw the change.
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        var t = 0L
        while (t <= ms) {
            check()
            compose.mainClock.advanceTimeByFrame()
            t += 16
        }
    }

    @Test
    fun theEffects_takeTheGuidesMoveAndFade() {
        assertEquals(Guide.MOVE_MS, EVENT_MOVE_MS)
        assertEquals(Guide.FADE_MS, EVENT_FADE_MS)
    }

    @Test
    fun tick_drawsIn_forASetLoggedNow_thenIsStill() {
        show(state(item(squat), item(bench)))
        compose.onNodeWithText("Log set 1").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        val drawIn = assertNotNull_(tick(1))
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS / 2L)
        val mid = drawIn()
        assertTrue("mid-draw: $mid", mid > 0.2f && mid < 0.8f)
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS.toLong())
        assertNull("still once drawn", tick(1))
        // The next set logged draws its own; the first stays still.
        compose.onNodeWithText("Log set 2").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertNotNull(tick(2))
        assertNull(tick(1))
    }

    @Test
    fun tick_isStill_forSetsAlreadyLoggedWhenTheScreenOpens() {
        set(TodayState(date, loading = true, sessionId = 1))
        show(today)
        // The screen loads with the sets there: not an event.
        set(state(item(squat, listOf(stored(1, squat), stored(2, squat))), item(bench)))
        everyFrame {
            assertNull(tick(1))
            assertNull(tick(2))
        }
    }

    @Test
    fun tick_isStill_onRestoreFromTheSavedState() {
        val restore = StateRestorationTester(compose)
        set(state(item(squat), item(bench)))
        compose.mainClock.autoAdvance = false
        restore.setContent { CompositionLocalProvider(LocalMotion provides true) { PowertrackerTheme { TodayContent(today, actions) } } }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Log set 1").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertNotNull(tick(1))
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS + 500L)
        // Restored (a rotation; the restore runs its frames to idle): the set is there, not logged now. The next set
        // logged draws its own tick.
        compose.mainClock.autoAdvance = true
        restore.emulateSavedInstanceStateRestore()
        compose.mainClock.autoAdvance = false
        everyFrame { assertNull(tick(1)) }
        compose.onNodeWithText("Log set 2").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertNotNull(tick(2))
        assertNull(tick(1))
    }

    @Test
    @Config(qualifiers = "w411dp-h700dp-xxhdpi")
    fun tick_isStill_whenItsRowScrollsBackIntoView() {
        show(state(item(squat), *more.map { item(it) }.toTypedArray()))
        compose.onNodeWithText("Log set 1").performClick()
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS + 500L)
        assertNull(tick(1))
        todayList().performScrollToIndex(more.size)
        compose.mainClock.advanceTimeBy(500)
        assertEquals(0, compose.onAllNodes(hasText("Set 1:", substring = true)).fetchSemanticsNodes().size)
        todayList().performScrollToIndex(0)
        compose.mainClock.advanceTimeByFrame()
        everyFrame { assertNull(tick(1)) }
    }

    @Test
    fun tick_isStill_onASideSwitch() {
        show(state(item(squat, listOf(stored(1, squat))), item(bench)))
        themeMode = TrainingMode.BODYBUILDING
        Snapshot.sendApplyNotifications()
        set(today.copy(mode = TrainingMode.BODYBUILDING))
        everyFrame { assertNull(tick(1)) }
    }

    @Test
    fun tick_drawsIn_forACatalogueExercisesFirstSet_whoseIdChangesAsItSaves() {
        val standIn = squat.copy(id = -5)
        val saved = squat.copy(id = 55)
        show(state(item(standIn), item(bench)))
        // Its first set saves its row: the item comes back under the new id, the card staying where it was.
        set(
            today.copy(
                items = listOf(item(saved, listOf(stored(101, saved))), item(bench)),
                resolved = mapOf(standIn.id to saved.id),
            ),
        )
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertNotNull(tick(1))
    }

    @Test
    fun tick_withMotionOff_isStill() {
        show(state(item(squat), item(bench)), motion = false)
        compose.onNodeWithText("Log set 1").performClick()
        everyFrame { assertNull(tick(1)) }
    }

    @Test
    fun tick_withTheSystemsAnimationsOff_isStill() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        set(state(item(squat), item(bench)))
        compose.mainClock.autoAdvance = false
        compose.setContent { ModeTheme(flowOf(AppSettings()), modeSwitch = null) { TodayContent(today, actions) } }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Log set 1").performClick()
        everyFrame { assertNull(tick(1)) }
    }

    @Test
    fun tick_atRest_isTheTickOfASetThatWasAlreadyThere() {
        show(state(item(squat), item(bench)))
        compose.onNodeWithText("Log set 1").performClick()
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS + 1_000L)
        val drawn = drawnSetRow()
        // The same screen opened with that set already logged.
        assertNull(tick(1))
        run++
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(1_000)
        assertNull(tick(1))
        val still = drawnSetRow()
        assertTrue("the settled tick is the still one", drawn.sameAs(still))
    }

    @Test
    fun line_risesIn_whenAnExerciseIsSkippedNow_thenIsStill() {
        show(state(item(squat), item(bench), item(row)))
        set(state(item(squat), item(bench), skipped = listOf(row)))
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        val riseIn = assertNotNull_(rise())
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS / 2L)
        val mid = riseIn()
        assertTrue("mid-rise: $mid", mid > 0.2f && mid < 0.8f)
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS.toLong())
        assertNull(rise())
        // A second skip while the line shows: the line is already there and stays where it is.
        set(state(item(squat), skipped = listOf(row, bench)))
        everyFrame { assertNull(rise()) }
    }

    private val more = (10L..20L).map { Exercise(it, "Lift $it", Category.ACCESSORY, null, emptyList(), isBuiltIn = true) }

    @Test
    @Config(qualifiers = "w411dp-h700dp-xxhdpi")
    fun line_isStill_whenFirstScrolledTo_afterASkipMadeOffScreen() {
        show(state(item(squat), *more.map { item(it) }.toTypedArray(), item(row)))
        set(state(item(squat), *more.map { item(it) }.toTypedArray(), skipped = listOf(row)))
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS + 500L)
        assertEquals("off-screen", 0, compose.onAllNodes(hasTestTag("skipped-today")).fetchSemanticsNodes().size)
        // The list: the items, Add exercise, then the line.
        todayList().performScrollToIndex(more.size + 2)
        everyFrame { assertNull(rise()) }
    }

    @Test
    @Config(qualifiers = "w411dp-h700dp-xxhdpi")
    fun line_isStill_whenScrolledAwayMidRiseAndBack() {
        show(state(item(squat), *more.map { item(it) }.toTypedArray(), item(row)))
        todayList().performScrollToIndex(more.size + 2)
        compose.mainClock.advanceTimeBy(500)
        set(state(item(squat), *more.map { item(it) }.toTypedArray(), skipped = listOf(row)))
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertNotNull("it rises where it is seen", rise())
        todayList().performScrollToIndex(0)
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS / 2L)
        todayList().performScrollToIndex(more.size + 2)
        everyFrame { assertNull(rise()) }
    }

    @Test
    fun line_isStill_whenTheScreenOpensWithSkipsThere() {
        set(TodayState(date, loading = true, sessionId = 1))
        show(today)
        set(state(item(squat), item(bench), skipped = listOf(row)))
        everyFrame { assertNull(rise()) }
    }

    @Test
    fun line_isStill_onRestoreFromTheSavedState() {
        val restore = StateRestorationTester(compose)
        set(state(item(squat), item(bench), item(row)))
        compose.mainClock.autoAdvance = false
        restore.setContent { CompositionLocalProvider(LocalMotion provides true) { PowertrackerTheme { TodayContent(today, actions) } } }
        compose.mainClock.advanceTimeBy(1_000)
        set(state(item(squat), item(bench), skipped = listOf(row)))
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertNotNull(rise())
        compose.mainClock.advanceTimeBy(EVENT_MOVE_MS + 500L)
        compose.mainClock.autoAdvance = true
        restore.emulateSavedInstanceStateRestore()
        compose.mainClock.autoAdvance = false
        everyFrame { assertNull(rise()) }
    }

    @Test
    fun line_isStill_onASideSwitch() {
        show(state(item(squat), item(bench)))
        themeMode = TrainingMode.BODYBUILDING
        Snapshot.sendApplyNotifications()
        set(state(item(squat), skipped = listOf(bench)).copy(mode = TrainingMode.BODYBUILDING))
        everyFrame { assertNull(rise()) }
    }

    @Test
    fun line_withMotionOff_isStill() {
        show(state(item(squat), item(bench), item(row)), motion = false)
        set(state(item(squat), item(bench), skipped = listOf(row)))
        everyFrame { assertNull(rise()) }
    }

    private fun <T> assertNotNull_(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    private var guideTime by mutableStateOf(EventClock(0, 0))

    /** Today drawn as the guide draws it: on the guide's time, which the test moves (held, the frame clock goes on). */
    private fun showOnTheGuidesClock(start: TodayState) = show(start) {
        CompositionLocalProvider(LocalEventClock provides guideTime) { TodayContent(today, actions) }
    }

    private fun guideAt(nowMs: Int, stepStartMs: Int) {
        guideTime = EventClock(nowMs, stepStartMs)
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun tick_onTheGuidesClock_followsIt_fromItsStepsStart_andHoldsWhenItHolds() {
        guideTime = EventClock(1_000, 900)
        showOnTheGuidesClock(state(item(squat), item(bench)))
        // The set comes in a step that started at 2000; the guide's time is already a little into it.
        guideAt(2_070, 2_000)
        set(state(item(squat, listOf(stored(101, squat))), item(bench)))
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(70f / EVENT_MOVE_MS, assertNotNull_(tick(1))(), 1e-4f)
        // Held: the frame clock goes on, the tick does not.
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(70f / EVENT_MOVE_MS, assertNotNull_(tick(1))(), 1e-4f)
        guideAt(2_140, 2_000)
        assertEquals(0.5f, assertNotNull_(tick(1))(), 1e-4f)
        // Over at the end of the move; a later step does not start it again.
        guideAt(2_000 + EVENT_MOVE_MS, 2_000)
        assertNull(tick(1))
        guideAt(3_200, 3_180)
        compose.mainClock.advanceTimeByFrame()
        assertNull(tick(1))
    }

    @Test
    fun line_onTheGuidesClock_followsIt_andHoldsWhenItHolds() {
        guideTime = EventClock(1_000, 900)
        showOnTheGuidesClock(state(item(squat), item(bench), item(row)))
        guideAt(5_000, 5_000)
        set(state(item(squat), item(bench), skipped = listOf(row)))
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(0f, assertNotNull_(rise())(), 1e-4f)
        guideAt(5_000 + EVENT_MOVE_MS - EVENT_FADE_MS / 2, 5_000)
        val p = assertNotNull_(rise())()
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(p, assertNotNull_(rise())(), 1e-6f)
        // Half-way through its fade: half in, half of its rise to go.
        assertEquals(0.5f, riseFade(p), 0.01f)
        guideAt(5_000 + EVENT_MOVE_MS, 5_000)
        assertNull(rise())
    }

    @Test
    fun theRise_isTheLastFadeOfItsTime() {
        assertEquals(0f, riseFade(0f))
        assertEquals(0f, riseFade((EVENT_MOVE_MS - EVENT_FADE_MS).toFloat() / EVENT_MOVE_MS))
        assertEquals(1f, riseFade(1f))
    }
}
