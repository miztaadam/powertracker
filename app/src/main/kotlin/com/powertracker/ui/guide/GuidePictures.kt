package com.powertracker.ui.guide

import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import kotlin.math.min
import kotlin.math.roundToInt

/** The phone a picture's screen is laid out on before it is shrunk into the frame: the mockups' phone width. */
internal val PHONE_WIDTH = 412.dp

/** The strip a phone's status bar takes at its top. */
private val PHONE_STATUS_BAR = 24.dp

/**
 * The phone's height, the same for both sides: the mockups' 915 dp, which holds screen 4's day
 * ([com.powertracker.domain.guide.GuideSample.todayItems]) on either side, so its last step shows the ticked set and the
 * "Skipped today" line at the foot of the list.
 */
internal val PHONE_HEIGHT = 915.dp

/** What a screen reader hears for a picture, the one thing of it in the accessibility tree. Only built pictures have one. */
private val PICTURE_DESCRIPTIONS = mapOf(
    GuideScreenId.TODAY to "Example of the Today screen: a set's weight, reps and effort are put in and the set is ticked, then an exercise is skipped.",
)

fun pictureDescription(id: GuideScreenId): String = PICTURE_DESCRIPTIONS.getValue(id)

/** The step a picture shows, for tests: its content is out of the accessibility tree. */
val GuidePictureStep = SemanticsPropertyKey<GuideStep>("GuidePictureStep")
private var SemanticsPropertyReceiver.guidePictureStep by GuidePictureStep

/** Where in its steps a picture is (its step and the move's progress), for tests. */
val GuidePictureFrame = SemanticsPropertyKey<GuideFrame>("GuidePictureFrame")
private var SemanticsPropertyReceiver.guidePictureFrame by GuidePictureFrame

/**
 * How many times a picture has been composed with its clock read (each frame of a move is one), for tests: while a
 * step holds, none is.
 */
var guidePictureCompositions: Int = 0
    internal set

/** Every drag and tap on the picture stops here, unconsumed: the screen drawn under it gets none, the pager still swipes. */
private val NoTouches = Modifier.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial)
    }
}

/**
 * Screen [id]'s picture at [step], in its frame: the real screen content with the guide's sample state, laid out on a
 * phone and shrunk to fit, with no-op actions. It takes no touches and no focus, and is one description to a screen
 * reader. Screens whose picture isn't built yet show the empty frame.
 */
@Composable
internal fun GuidePicture(id: GuideScreenId, mode: TrainingMode, frame: GuideFrame?, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    val description = PICTURE_DESCRIPTIONS[id]
    val step = frame?.step
    val drawn = id == GuideScreenId.TODAY && step != null
    Box(
        modifier
            // A picture not drawn yet is an empty frame, filling its place.
            .then(if (drawn) Modifier else Modifier.fillMaxSize())
            .clip(shape)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, PowertrackerTheme.colors.hairline, shape)
            .clearAndSetSemantics {
                testTag = "guide-picture"
                if (description != null) contentDescription = description
                if (frame != null) {
                    guidePictureStep = frame.step
                    guidePictureFrame = frame
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (drawn) Phone(PHONE_HEIGHT) { TodayPicture(mode, step!!, frame.progress) }
        Box(Modifier.matchParentSize().then(NoTouches))
    }
}

/**
 * The Today screen at one step of logging a set (screen 4), [progress] through the move into it: the lift's editor
 * shows the domain's in-between values ([GuideSample.todayEditor]). One instance through all the steps, so the real
 * screen's own motion plays as its state changes (a logged set's block slides open, a skipped row fades out).
 */
@Composable
internal fun TodayPicture(mode: TrainingMode, step: GuideStep, progress: Double = 1.0) {
    val editor = GuideSample.todayEditor(mode, step, progress)
    val state = remember(mode, step, editor) { GuideSampleState.today(mode, step, editor) }
    val actions = remember { TodayActions() }
    // The phone's status bar: the screen's header starts below the frame's rounded corner.
    Box(Modifier.fillMaxSize().padding(top = PHONE_STATUS_BAR)) { TodayContent(state, actions) }
}

/**
 * A phone-sized screen shrunk to fit the space it is given, keeping its shape. It is laid out at the normal text size,
 * as the picture of a phone, whatever the lifter's: the guide's own text is what grows.
 */
@Composable
private fun Phone(phoneHeight: Dp, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val phoneDensity = remember(density.density) { Density(density.density, fontScale = 1f) }
    Box(
        Modifier
            .consumeWindowInsets(WindowInsets.safeDrawing)
            // Nothing inside takes focus: the keyboard and switch access skip the picture.
            .focusProperties { onEnter = { cancelFocusChange() } }
            .focusGroup()
            .layout { measurable, constraints ->
                val width = PHONE_WIDTH.roundToPx()
                val height = phoneHeight.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(width, height))
                val scale = if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                    min(constraints.maxWidth.toFloat() / width, constraints.maxHeight.toFloat() / height)
                } else {
                    1f
                }
                layout((width * scale).roundToInt(), (height * scale).roundToInt()) {
                    placeable.placeWithLayer(0, 0) {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalDensity provides phoneDensity) { content() }
    }
}
