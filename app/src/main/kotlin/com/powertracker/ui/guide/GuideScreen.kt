package com.powertracker.ui.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.components.Pill
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.plan.builder.BUILD_MY_PLAN
import com.powertracker.ui.plan.templates.CHOOSE_A_TEMPLATE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How the guide was left. */
enum class GuideExit { SKIPPED, DONE, CHOOSE_TEMPLATE, BUILD_PLAN, SEE_PLAN }

/** The guide's own control words and labels: the screens' copy comes from the domain ([Guide.screen]). */
const val SKIP = "Skip"
const val BACK = "Back"
const val NEXT = "Next"
const val DONE = "Done"
const val EXAMPLE = "Example"
const val SEE_MY_PLAN = "See my plan"

/** Screen 2's buttons on a first launch: each side by its name. */
fun sideName(mode: TrainingMode) = if (mode == TrainingMode.BODYBUILDING) "Bodybuilding" else "Powerlifting"

/** Screen 8's buttons (Task 3): with no plan on the side on, [CHOOSE_A_TEMPLATE] and on bodybuilding [BUILD_MY_PLAN]; else Done and See my plan. */
internal data class LastButtons(val primary: Pair<String, GuideExit>, val secondary: Pair<String, GuideExit>?)

internal fun lastButtons(mode: TrainingMode, hasPlan: Boolean): LastButtons = when {
    hasPlan -> LastButtons(DONE to GuideExit.DONE, SEE_MY_PLAN to GuideExit.SEE_PLAN)
    mode == TrainingMode.BODYBUILDING -> LastButtons(CHOOSE_A_TEMPLATE to GuideExit.CHOOSE_TEMPLATE, BUILD_MY_PLAN to GuideExit.BUILD_PLAN)
    else -> LastButtons(CHOOSE_A_TEMPLATE to GuideExit.CHOOSE_TEMPLATE, null)
}

/** What the row of dots says: the one screen shown of how many. */
fun dotsLabel(index: Int, count: Int) = "Screen ${index + 1} of $count"

/**
 * A picture's time: how many milliseconds into its steps it is ([Guide.frame] turns that into a step and its move's
 * progress). Tests hold it ([HeldGuideClock]); the guide runs on [TickingGuideClock].
 */
@Stable
interface GuideClock {
    /**
     * Milliseconds into a picture of [steps], from 0 when this call first composed (a new run of the picture is a new
     * composition: the guide keys it). While not [playing] it stands still where it is, and it goes on from there.
     */
    @Composable
    fun elapsedMs(steps: List<GuideStep>, playing: Boolean): Int
}

/**
 * Real time, on the frame clock while a step moves or a tap ring plays before it (a new value each frame) and idle
 * while it holds (one timer to the next step's motion, so nothing is drawn); it stops for good once the last step has
 * settled. It stands still while the guide is not on screen (the lifecycle below STARTED), and goes on from there when
 * it is back.
 */
object TickingGuideClock : GuideClock {
    @Composable
    override fun elapsedMs(steps: List<GuideStep>, playing: Boolean): Int {
        var elapsed by remember { mutableIntStateOf(0) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(steps, playing, lifecycle) {
            if (!playing) return@LaunchedEffect
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val done = Guide.doneMs(steps.size)
                while (elapsed < done) {
                    val index = (steps.size - 1 downTo 0).first { Guide.motionStartMs(steps, it) <= elapsed }
                    val settled = Guide.settledMs(index)
                    if (elapsed < settled) {
                        // A move, or a tap ring and the move after it: one value a frame, from where it stands.
                        val base = withFrameMillis { it } - elapsed
                        while (elapsed < settled) {
                            val now = withFrameMillis { it }
                            elapsed = minOf(settled, (now - base).toInt())
                        }
                    } else {
                        // A hold: nothing changes until the next step's motion starts.
                        val next = Guide.motionStartMs(steps, index + 1)
                        delay((next - elapsed).toLong())
                        elapsed = next
                    }
                }
            }
        }
        return elapsed
    }
}

/** A clock a test moves by hand: every picture is [heldMs] into its steps (held within them). */
class HeldGuideClock(heldMs: Int = 0) : GuideClock {
    var heldMs by mutableIntStateOf(heldMs)

    /** Holds every picture at step [index], settled. */
    fun holdStep(index: Int) {
        heldMs = Guide.settledMs(index)
    }

    @Composable
    override fun elapsedMs(steps: List<GuideStep>, playing: Boolean): Int = heldMs.coerceIn(0, Guide.doneMs(steps.size))
}

@Composable
fun rememberGuideClock(): GuideClock = TickingGuideClock

/**
 * The guide (1.16): [Guide.SCREENS] in a horizontal pager, a row of dots, Skip at the top right, Back and Next at the
 * bottom; a swipe or the system Back works too. [mode] is the side whose copy and colours it shows. With [motionOn]
 * false every picture shows its last step, still. Writes nothing: what an exit and a side pick do is the caller's
 * ([onClose], [onPickSide]). Only on a [firstLaunch] does screen 2 offer the two sides; picking the side already on
 * calls nothing. Screen 8 offers what [lastButtons] gives for [hasPlan]. [startAt] opens it on a later screen (the
 * screenshots and tests).
 */
@Composable
fun GuideScreen(
    mode: TrainingMode,
    firstLaunch: Boolean,
    hasPlan: Boolean,
    motionOn: Boolean,
    onPickSide: (TrainingMode) -> Unit,
    onClose: (GuideExit) -> Unit,
    clock: GuideClock = rememberGuideClock(),
    startAt: GuideScreenId = GuideScreenId.WELCOME,
) {
    val screens = Guide.SCREENS
    // Saved with the activity: a rotation keeps the screen.
    val pager = rememberPagerState(initialPage = screens.indexOf(startAt)) { screens.size }
    val scope = rememberCoroutineScope()
    val close by rememberUpdatedState(onClose)
    val pick by rememberUpdatedState(onPickSide)
    BackHandler { close(GuideExit.SKIPPED) }

    fun go(page: Int) {
        scope.launch { if (motionOn) pager.animateScrollToPage(page) else pager.scrollToPage(page) }
    }

    // Each time a page becomes the one settled on, its picture starts a new run: built afresh at its first step, so it
    // never plays backwards from where it was left. Pausing (a drag) is not a new run.
    var runs by remember { mutableStateOf(emptyMap<Int, Int>()) }
    LaunchedEffect(pager) {
        var settled = pager.settledPage
        snapshotFlow { pager.settledPage }.collect { page ->
            if (page != settled) {
                settled = page
                runs = runs + (page to (runs[page] ?: 0) + 1)
            }
        }
    }

    val current = pager.currentPage
    val last = current == screens.lastIndex
    val buttons = remember(mode, hasPlan) { lastButtons(mode, hasPlan) }
    // A second button under Back and the first (screen 8's See my plan, or Build my plan) takes room from the page while
    // it shows: measured once, every other page keeps that room out of its picture, so the phone is one size on all.
    var secondRoom by remember { mutableStateOf(0.dp) }
    // The guide is a pane titled with the screen shown: a page change is a new title, which a screen reader reads out.
    val shownTitle = remember(screens[current], mode) { Guide.screen(screens[current], mode).title }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .semantics { paneTitle = shownTitle }
            .testTag("guide"),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.s, vertical = Space.xs), horizontalArrangement = Arrangement.End) {
            QuietButton(SKIP, { close(GuideExit.SKIPPED) }, Modifier.testTag("guide-skip"))
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth().testTag("guide-pages")) { page ->
            val id = screens[page]
            val steps = remember(id, mode) { Guide.steps(id, mode) }
            key(runs[page] ?: 0) {
                val playing = motionOn && page == pager.currentPage && !pager.isScrollInProgress
                val elapsed = clock.elapsedMs(steps, playing)
                SideEffect { guidePictureCompositions++ }
                // With motion off: the last step, still.
                val t = if (motionOn) elapsed else Guide.doneMs(steps.size)
                val frame = Guide.frame(steps, t)
                GuidePage(id, mode, current = page == current, frame = frame, elapsedMs = t, reserve = if (last && buttons.secondary != null) 0.dp else secondRoom) {
                    if (firstLaunch && id == GuideScreenId.SIDE) SideButtons(mode) { if (it != mode) pick(it) }
                }
            }
        }
        Dots(current, screens.size, Modifier.align(Alignment.CenterHorizontally).padding(vertical = Space.m))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Space.gutter).padding(bottom = Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
                SecondaryButton(
                    BACK, { go(current - 1) }, Modifier.testTag("guide-back"),
                    enabled = current > 0, icon = Icons.AutoMirrored.Filled.ArrowBack,
                )
                // On screen 8, Next becomes its first button: Choose a template, or Done with a plan.
                PrimaryButton(
                    if (last) buttons.primary.first else NEXT,
                    { if (last) close(buttons.primary.second) else go(current + 1) },
                    Modifier.weight(1f).testTag("guide-next"),
                    icon = if (last) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                )
            }
            // Below it: Build my plan (bodybuilding, no plan), or See my plan.
            val secondary = buttons.secondary
            if (last && secondary != null) {
                SecondaryButton(secondary.first, { close(secondary.second) }, Modifier.fillMaxWidth().testTag("guide-last-secondary"))
            } else {
                // Measured, not drawn: the room a second button takes (one is on screen 8 with a plan, and on
                // bodybuilding's without). Every page keeps it out of its picture, on both sides, so the phone is one
                // size everywhere. Its gap above is already taken here (the column spaces this empty child too).
                val density = LocalDensity.current
                Layout({ SecondaryButton(secondary?.first ?: SEE_MY_PLAN, {}, Modifier.fillMaxWidth().clearAndSetSemantics {}) }) { measurables, constraints ->
                    val height = measurables.single().measure(constraints.copy(minHeight = 0)).height
                    val room = with(density) { height.toDp() }
                    if (room != secondRoom) secondRoom = room
                    layout(0, 0) {}
                }
            }
        }
    }
}

/** Screen 2 on a first launch: the two sides, the one on drawn as chosen. A tap sets the real side (the caller's). */
@Composable
private fun SideButtons(mode: TrainingMode, onPick: (TrainingMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        for (side in TrainingMode.entries) {
            val tag = Modifier.weight(1f).testTag("guide-side-${side.name.lowercase()}").semantics { selected = side == mode }
            if (side == mode) {
                PrimaryButton(sideName(side), { onPick(side) }, tag, icon = Icons.Filled.Check)
            } else {
                SecondaryButton(sideName(side), { onPick(side) }, tag)
            }
        }
    }
}

/** The screens whose picture is a drawn phone, in its outline: one size on all of them (spec point 11). */
internal val OUTLINED: Set<GuideScreenId> = setOf(
    GuideScreenId.PLAN, GuideScreenId.TODAY, GuideScreenId.TARGETS, GuideScreenId.HOLDS_BACK, GuideScreenId.MONTHS_LATER, GuideScreenId.YOUR_TURN,
)

/**
 * The height the longest text of the outlined screens takes, on either side, at [width] and the default font size: what
 * every outlined screen leaves its text, so its phone is the same size on all of them. Measured, not laid out.
 */
@Composable
private fun outlinedTextRoom(width: Dp): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val title = MaterialTheme.typography.headlineMedium
    val body = MaterialTheme.typography.bodyLarge
    return remember(width, density.density, title, body) {
        val unscaled = Density(density.density, fontScale = 1f)
        val constraints = Constraints(maxWidth = with(density) { width.roundToPx() }.coerceAtLeast(0))
        val gap = with(density) { Space.s.roundToPx() }
        val px = TrainingMode.entries.flatMap { mode -> OUTLINED.map { Guide.screen(it, mode) } }.maxOf { screen ->
            measurer.measure(screen.title, title, constraints = constraints, density = unscaled).size.height +
                screen.lines.sumOf { gap + measurer.measure(it.text, body, constraints = constraints, density = unscaled).size.height }
        }
        with(density) { px.toDp() }
    }
}

/**
 * One screen: its title and lines from the domain, then its picture. On the outlined screens the picture has the same
 * room on every one ([outlinedTextRoom]): at a larger font the text scrolls, and the phone keeps its size. WELCOME and
 * SIDE draw no phone: their art takes what the text leaves.
 */
@Composable
private fun GuidePage(id: GuideScreenId, mode: TrainingMode, current: Boolean, frame: GuideFrame, elapsedMs: Int, reserve: Dp = 0.dp, below: @Composable () -> Unit = {}) {
    val screen = remember(id, mode) { Guide.screen(id, mode) }
    val outlined = id in OUTLINED
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = Space.gutter)) {
        val room = if (outlined) outlinedTextRoom(maxWidth) else 0.dp
        val artHeight = (maxHeight - reserve - room).coerceAtLeast(0.dp)
        val textMax = maxHeight * 0.6f
        Column(Modifier.fillMaxSize()) {
            // The text wraps and is never cut: past its room it scrolls.
            val text = if (outlined) Modifier.weight(1f) else Modifier.heightIn(max = textMax)
            Column(text.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(
                    screen.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }.then(if (current) Modifier.testTag("guide-title") else Modifier),
                )
                for (line in screen.lines) {
                    Text(line.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(
                (if (outlined) Modifier.height(artHeight) else Modifier.weight(1f)).fillMaxWidth().padding(top = Space.l, bottom = Space.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                // The label sits above the picture, outside its single description. An outlined screen without one keeps
                // its place, unseen, so its phone is the size of the others'.
                if (screen.example) {
                    Pill(EXAMPLE, if (current) Modifier.testTag("guide-example") else Modifier)
                } else if (outlined) {
                    Pill(EXAMPLE, Modifier.alpha(0f).clearAndSetSemantics {})
                }
                GuidePicture(id, mode, frame, Modifier.weight(1f, fill = !outlined), elapsedMs = elapsedMs)
                below()
            }
        }
    }
}

/**
 * A dot per screen, the current one in the side's accent; one description for a screen reader (the dots merge into
 * it, and say nothing of their own: each is only tagged, for tests to count).
 */
@Composable
private fun Dots(current: Int, count: Int, modifier: Modifier = Modifier) {
    val pt = PowertrackerTheme.colors
    val label = dotsLabel(current, count)
    Row(
        modifier.testTag("guide-dots").semantics(mergeDescendants = true) { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        for (i in 0 until count) {
            Box(Modifier.size(8.dp).background(if (i == current) pt.accent else pt.hairline, CircleShape).testTag("guide-dot"))
        }
    }
}
