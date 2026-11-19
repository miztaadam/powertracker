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
 * false every picture shows its last step, still. Writes nothing: what an exit does is the caller's ([onClose]).
 * [firstLaunch], [hasPlan] and [onPickSide] are for screens 2 and 8 (Task 3); [startAt] opens it on a later screen
 * (the screenshots).
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
                GuidePage(id, mode, current = page == current, frame = frame, elapsedMs = t)
            }
        }
        Dots(current, screens.size, Modifier.align(Alignment.CenterHorizontally).padding(vertical = Space.m))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.gutter).padding(bottom = Space.l),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(
                BACK, { go(current - 1) }, Modifier.testTag("guide-back"),
                enabled = current > 0, icon = Icons.AutoMirrored.Filled.ArrowBack,
            )
            // Screen 8's own buttons come with the first launch (Task 3); until then its Next is Done.
            PrimaryButton(
                if (last) DONE else NEXT,
                { if (last) close(GuideExit.DONE) else go(current + 1) },
                Modifier.weight(1f).testTag("guide-next"),
                icon = if (last) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
            )
        }
    }
}

/** One screen: its title and lines from the domain, then its picture, which shrinks to leave the text its room. */
@Composable
private fun GuidePage(id: GuideScreenId, mode: TrainingMode, current: Boolean, frame: GuideFrame, elapsedMs: Int) {
    val screen = remember(id, mode) { Guide.screen(id, mode) }
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = Space.gutter)) {
        val textRoom = maxHeight * 0.6f
        Column(Modifier.fillMaxSize()) {
            // The text wraps and is never cut: past its share of the height it scrolls, and the picture takes the rest.
            Column(Modifier.heightIn(max = textRoom).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.s)) {
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
                Modifier.weight(1f).fillMaxWidth().padding(top = Space.l, bottom = Space.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                // The label sits above the picture, outside its single description.
                if (screen.example) Pill(EXAMPLE, if (current) Modifier.testTag("guide-example") else Modifier)
                GuidePicture(id, mode, frame, Modifier.weight(1f, fill = false), elapsedMs = elapsedMs)
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
