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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideScreenId
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

/** Which step a picture shows. Tests hold it ([HeldGuideClock]); the guide runs on [TickingGuideClock]. */
@Stable
interface GuideClock {
    /** The step, from 0 to [count] - 1, of a picture that started when [playing] last became true; 0 while not playing. */
    @Composable
    fun step(count: Int, playing: Boolean): Int
}

/** One step every [Guide.STEP_MS], from the first, holding on the last. */
object TickingGuideClock : GuideClock {
    @Composable
    override fun step(count: Int, playing: Boolean): Int {
        // Started afresh each time the picture comes on screen, and back at 0 at once when it leaves.
        var step by remember(count, playing) { mutableIntStateOf(0) }
        LaunchedEffect(count, playing) {
            if (!playing) return@LaunchedEffect
            while (step < count - 1) {
                delay(Guide.STEP_MS.toLong())
                step++
            }
        }
        return step
    }
}

/** A clock a test moves by hand: every picture shows step [held] (within its own steps). */
class HeldGuideClock(held: Int = 0) : GuideClock {
    var held by mutableIntStateOf(held)

    @Composable
    override fun step(count: Int, playing: Boolean): Int = held.coerceIn(0, count - 1)
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

    val current = pager.currentPage
    val last = current == screens.lastIndex
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .testTag("guide"),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.s, vertical = Space.xs), horizontalArrangement = Arrangement.End) {
            QuietButton(SKIP, { close(GuideExit.SKIPPED) }, Modifier.testTag("guide-skip"))
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth().testTag("guide-pages")) { page ->
            val id = screens[page]
            val steps = remember(id, mode) { Guide.steps(id, mode) }
            val playing = motionOn && page == pager.currentPage && !pager.isScrollInProgress
            val ticked = clock.step(steps.size, playing)
            val step = if (motionOn) ticked else steps.lastIndex
            GuidePage(id, mode, current = page == current, step = steps.getOrNull(step))
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
private fun GuidePage(id: GuideScreenId, mode: TrainingMode, current: Boolean, step: GuideStep?) {
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
                GuidePicture(id, mode, step, Modifier.weight(1f, fill = false))
            }
        }
    }
}

/** A dot per screen, the current one in the side's accent; one description for a screen reader. */
@Composable
private fun Dots(current: Int, count: Int, modifier: Modifier = Modifier) {
    val pt = PowertrackerTheme.colors
    val label = dotsLabel(current, count)
    Row(
        modifier.testTag("guide-dots").clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        for (i in 0 until count) {
            Box(Modifier.size(8.dp).background(if (i == current) pt.accent else pt.hairline, CircleShape))
        }
    }
}
