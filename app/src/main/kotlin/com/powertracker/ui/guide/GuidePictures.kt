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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideTap
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.EventClock
import com.powertracker.ui.today.LocalEventClock
import com.powertracker.ui.today.LocalTapAnchors
import com.powertracker.ui.today.TapAnchor
import com.powertracker.ui.today.TapAnchors
import com.powertracker.ui.today.TapSpot
import com.powertracker.ui.guide.pictures.HoldsBackPicture
import com.powertracker.ui.guide.pictures.MonthsLaterPicture
import com.powertracker.ui.guide.pictures.PlanPicture
import com.powertracker.ui.guide.pictures.SidePicture
import com.powertracker.ui.guide.pictures.TargetsPicture
import com.powertracker.ui.guide.pictures.WelcomePicture
import com.powertracker.ui.guide.pictures.YourTurnPicture
import androidx.compose.ui.graphics.Color
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import kotlin.math.min
import kotlin.math.roundToInt

/** The phone a picture's screen is laid out on before it is shrunk into the frame: the mockups' phone width. */
internal val PHONE_WIDTH = 412.dp

/** The strip a phone's status bar takes at its top. */
internal val PHONE_STATUS_BAR = 24.dp

/**
 * The phone's height, the same for both sides: the mockups' 915 dp, which holds screen 4's day
 * ([com.powertracker.domain.guide.GuideSample.todayItems]) on either side, so its last step shows the ticked set and the
 * "Skipped today" line at the foot of the list.
 */
internal val PHONE_HEIGHT = 915.dp

/** What a screen reader hears for a picture, the one thing of it in the accessibility tree. */
private val PICTURE_DESCRIPTIONS = mapOf(
    GuideScreenId.WELCOME to "The Grit mark, then the colours of its two sides, powerlifting and bodybuilding.",
    GuideScreenId.SIDE to "The hammer of the powerlifting side and the pump of the bodybuilding side, side by side.",
    GuideScreenId.PLAN to "Example of the Plan screen: a 3-day template is chosen, its weekdays are picked, one exercise is swapped, then the week is saved.",
    GuideScreenId.TODAY to "Example of the Today screen: a set's weight, reps and effort are put in and the set is ticked, then an exercise is skipped.",
    GuideScreenId.TARGETS to "Example of the coach's target for one lift, session after session.",
    GuideScreenId.HOLDS_BACK to "Example of the Coach screen: its cards for when it holds back, one after another.",
    GuideScreenId.MONTHS_LATER to "Example of the Progress screen months later: the weeks fill in and the bests climb.",
    GuideScreenId.YOUR_TURN to "The Plan screen as it opens on a new plan, ready for a template.",
)

fun pictureDescription(id: GuideScreenId): String = PICTURE_DESCRIPTIONS.getValue(id)

/** The step a picture shows, for tests: its content is out of the accessibility tree. */
val GuidePictureStep = SemanticsPropertyKey<GuideStep>("GuidePictureStep")
private var SemanticsPropertyReceiver.guidePictureStep by GuidePictureStep

/** Where in its steps a picture is (its step and the move's progress), for tests. */
val GuidePictureFrame = SemanticsPropertyKey<GuideFrame>("GuidePictureFrame")
private var SemanticsPropertyReceiver.guidePictureFrame by GuidePictureFrame

/** Whether a picture is a drawn phone in its outline (the six screens of [OUTLINED]), for tests. */
val GuidePictureOutlined = SemanticsPropertyKey<Boolean>("GuidePictureOutlined")
private var SemanticsPropertyReceiver.guidePictureOutlined by GuidePictureOutlined

/** While a tap ring plays: where the control it rings is, in the drawn phone's own space (unscaled px), for tests. */
val GuideTapBox = SemanticsPropertyKey<Rect>("GuideTapBox")
private var SemanticsPropertyReceiver.guideTapBox by GuideTapBox

/** The control on [mode]'s drawn screen that a [GuideTap] rings. */
internal fun anchorOf(tap: GuideTap, mode: TrainingMode): TapSpot = when (tap) {
    GuideTap.LOG_SET -> TapSpot(TapAnchor.LOG_SET)
    GuideTap.CHOOSE_TEMPLATE -> TapSpot(TapAnchor.CHOOSE_TEMPLATE)
    GuideTap.TEMPLATE -> TapSpot(TapAnchor.TEMPLATE, GuideSample.template(mode).id)
    GuideTap.SWAP -> TapSpot(TapAnchor.SWAP, GuideSample.swap(mode).from)
    GuideTap.USE_THIS_PLAN -> TapSpot(TapAnchor.USE_THIS_PLAN)
}

/**
 * The ring's ink on the control it rings: a primary button's own ink (Log set, Choose a template, Use this plan), else
 * the side's accent text (a template's card, a Swap button), so it shows on that control on either side and scheme.
 */
@Composable
private fun ringInk(tap: GuideTap?): Color = when (tap) {
    GuideTap.TEMPLATE, GuideTap.SWAP -> PowertrackerTheme.colors.accentText
    else -> PowertrackerTheme.colors.onAccent
}

/*
 * The tap ring (1.16, the sketch the owner approved): RING_SIZE across with a RING_LINE line at first, growing from
 * RING_SCALE_FROM to RING_SCALE_TO times that as it fades from RING_OPACITY_FROM to nothing. The web's guide draws the
 * same.
 */
private val RING_SIZE = 14.dp
private val RING_LINE = 2.dp
private const val RING_SCALE_FROM = 0.6f
private const val RING_SCALE_TO = 4.2f
private const val RING_OPACITY_FROM = 0.9f

/**
 * How many times a picture has been composed with its clock read (each frame of a move is one), for tests: while a
 * step holds, none is.
 */
internal var guidePictureCompositions: Int = 0

/** Every drag and tap on the picture stops here, unconsumed: the screen drawn under it gets none, the pager still swipes. */
private val NoTouches = Modifier.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial)
    }
}

/**
 * Screen [id]'s picture at [step], in its frame: the real screen content with the guide's sample state, laid out on a
 * phone and shrunk to fit, with no-op actions. It takes no touches and no focus, and is one description to a screen
 * reader. Each screen's picture is in its own file under `pictures/`; this frames it and rings its taps.
 */
@Composable
internal fun GuidePicture(id: GuideScreenId, mode: TrainingMode, frame: GuideFrame?, modifier: Modifier = Modifier, elapsedMs: Int = 0) {
    val shape = MaterialTheme.shapes.large
    val description = PICTURE_DESCRIPTIONS[id]
    val step = frame?.step
    val drawn = step != null
    // Where the drawn screen's controls are, for the tap ring (the screen says, to the guide only).
    val anchors = remember { TapAnchors() }
    val ringing = frame?.tap?.let { anchors.boxes[anchorOf(it, mode)] }
    // WELCOME and SIDE are no screen of the app: their art fills the picture's place, with no phone and no outline.
    val outlined = id in OUTLINED
    Box(
        modifier
            // A picture not drawn yet is an empty frame, filling its place; so is art with no outline.
            .then(if (drawn && outlined) Modifier else Modifier.fillMaxSize())
            .then(if (outlined) Modifier.clip(shape).background(MaterialTheme.colorScheme.background).border(1.dp, PowertrackerTheme.colors.hairline, shape) else Modifier)
            .clearAndSetSemantics {
                testTag = "guide-picture"
                guidePictureOutlined = outlined
                if (description != null) contentDescription = description
                if (frame != null) {
                    guidePictureStep = frame.step
                    guidePictureFrame = frame
                }
                if (ringing != null) guideTapBox = ringing
            },
        contentAlignment = Alignment.Center,
    ) {
        if (drawn && !outlined) {
            Box(Modifier.fillMaxSize().focusProperties { onEnter = { cancelFocusChange() } }.focusGroup()) { PictureContent(id, mode, frame) }
        } else if (drawn) {
            Phone(PHONE_HEIGHT) {
                // The guide's time drives the screen's own one-off motion (a set's tick drawing in, the skipped line
                // rising): a held clock holds it, a replay replays it.
                val time = EventClock(nowMs = elapsedMs, stepStartMs = Guide.stepStartMs(frame.stepIndex))
                CompositionLocalProvider(LocalEventClock provides time, LocalTapAnchors provides anchors) {
                    Box(Modifier.fillMaxSize().onGloballyPositioned { anchors.placeRoot(it) }) {
                        PictureContent(id, mode, frame)
                        TapRing(frame, anchors, mode)
                    }
                }
            }
        }
        Box(Modifier.matchParentSize().then(NoTouches))
    }
}

/** Screen [id]'s drawn phone at [frame]: each picture is its own file under `pictures/`, screen 4's below. */
@Composable
internal fun PictureContent(id: GuideScreenId, mode: TrainingMode, frame: GuideFrame) {
    when (id) {
        GuideScreenId.WELCOME -> WelcomePicture(frame)
        GuideScreenId.SIDE -> SidePicture()
        GuideScreenId.PLAN -> PlanPicture(mode, frame)
        GuideScreenId.TODAY -> TodayPicture(mode, frame.step, frame.progress)
        GuideScreenId.TARGETS -> TargetsPicture(mode, frame)
        GuideScreenId.HOLDS_BACK -> HoldsBackPicture(mode, frame)
        GuideScreenId.MONTHS_LATER -> MonthsLaterPicture(mode, frame)
        GuideScreenId.YOUR_TURN -> YourTurnPicture(mode)
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
 * The tap ring over the drawn screen, in its own space: on the control [frame] rings, while it does, as far through as
 * its time says. It draws nothing otherwise.
 */
@Composable
private fun TapRing(frame: GuideFrame, anchors: TapAnchors, mode: TrainingMode) {
    val tap = frame.tap
    val color = ringInk(tap)
    val spot = tap?.let { anchorOf(it, mode) }
    val p = frame.tapProgress.toFloat()
    Box(
        Modifier.fillMaxSize().drawBehind {
            val box = spot?.let { anchors.boxes[it] } ?: return@drawBehind
            val scale = RING_SCALE_FROM + (RING_SCALE_TO - RING_SCALE_FROM) * p
            val line = RING_LINE.toPx() * scale
            val outer = RING_SIZE.toPx() / 2 * scale
            drawCircle(
                color.copy(alpha = color.alpha * RING_OPACITY_FROM * (1f - p)),
                radius = outer - line / 2,
                center = box.center,
                style = Stroke(width = line),
            )
        },
    )
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
