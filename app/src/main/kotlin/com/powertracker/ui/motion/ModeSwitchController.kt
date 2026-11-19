package com.powertracker.ui.motion

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.motion.DropFrame
import com.powertracker.domain.motion.ModeSwitchMotion
import com.powertracker.domain.motion.Point
import com.powertracker.domain.motion.PumpFrame

/** Which animation: the hammer drop goes into powerlifting, the pump into bodybuilding. */
enum class ModeSwitchKind {
    DROP,
    PUMP;

    /** The side this animation goes to. */
    val target: TrainingMode get() = if (this == DROP) TrainingMode.POWERLIFTING else TrainingMode.BODYBUILDING

    /** When the new side shows: the drop's impact, the pump's third beat (ms from the tap). */
    val moment: Double get() = if (this == DROP) ModeSwitchMotion.DROP.impact else ModeSwitchMotion.PUMP.beats[2]

    val end: Double get() = if (this == DROP) ModeSwitchMotion.DROP.end else ModeSwitchMotion.PUMP.end

    companion object {
        fun into(mode: TrainingMode) = if (mode == TrainingMode.POWERLIFTING) DROP else PUMP
    }
}

/** How a running animation ended: the clock ran out, a tap, the switch again, Settings closed, or the app went away. */
enum class ModeSwitchEnd { END, SKIP, FLIP, LEAVE, HIDDEN }

/** Where the overlay sits in the window: its top-left in root pixels, its size in pixels, and the density. */
data class ModeSwitchGeometry(val origin: Offset, val size: Size, val density: Float) {
    val widthDp: Double get() = size.width / density.toDouble()
    val heightDp: Double get() = size.height / density.toDouble()

    /** A root-pixel point in the overlay's dp, the motion model's units. */
    fun toDp(root: Offset): Offset = Offset((root.x - origin.x) / density, (root.y - origin.y) / density)
}

/**
 * The mode switch animations' controller (1.15): one clock, the side the theme shows, and the vibration and sound.
 * Only the Settings "Bodybuilding mode" switch calls [start]; a restore, or anything else that changes the stored side,
 * changes the look with no animation.
 *
 * States: idle ([kind] null) → running ([kind] set, [tMs] from 0) → idle.
 * - **[start]**, at the tap, before the setting is stored (it still is, at once, by the caller). A running animation is
 *   skipped first ([ModeSwitchEnd.FLIP]). The vibration and sound start. With motion off (reduce motion) nothing runs
 *   and the new side shows at once; otherwise [shownMode] holds the old side.
 * - **The moment** ([ModeSwitchKind.moment]): [shownMode] turns to the new side, the drop's impact or the pump's third beat.
 * - **[finish]** ends it at its end state from anywhere. Every way out but [ModeSwitchEnd.END] stops the sound and
 *   cancels the vibration (at the end, the sound's tail plays out). [shownMode] keeps the new side until the stored
 *   side says the same.
 *
 * [shownMode] is null when the theme follows the stored side. The clock is a pure step ([advanceTo]); the overlay's
 * frame loop drives it, and [hold] stops it at a moment for tests and screenshots.
 */
@Stable
class ModeSwitchController(private val feedback: ModeSwitchFeedback) {
    /** Motion on: off with the phone's reduce-motion setting. Set by the theme host. */
    var motionOn: Boolean = true

    var kind by mutableStateOf<ModeSwitchKind?>(null)
        private set

    private val clock = mutableDoubleStateOf(0.0)

    /** The running animation's time, ms from the tap. Read it in draw and layer blocks only, so nothing recomposes. */
    val tMs: Double get() = clock.doubleValue

    /** The side the theme shows, or null to follow the stored side. */
    var shownMode by mutableStateOf<TrainingMode?>(null)
        private set

    /** Bumped by each [start] that runs: the frame loop's key. */
    var run by mutableIntStateOf(0)
        private set

    /** The clock is held ([hold]): the frame loop leaves it alone. */
    var held: Boolean = false
        private set

    /** Whether the running animation's shapes are ready to draw (set by the overlay). */
    var artReady by mutableStateOf(false)
        internal set

    /** The overlay's place in the window, set by [ModeSwitchHost]. */
    var geometry: ModeSwitchGeometry? = null
        internal set

    /** The switch's centre and its row, in root pixels, as measured at the tap. */
    var switchCenter: Offset? = null
        private set
    var switchRow: Rect? = null
        private set

    private var stored: TrainingMode? = null
    private var momentPassed = false

    /** The pump's source in the overlay's dp: the switch, or null if it could not be measured. */
    fun source(): Offset? {
        val c = switchCenter ?: return null
        return geometry?.toDp(c)
    }

    /** Where the pump starts, in dp: the measured switch, or the bottom centre if it could not be measured. */
    fun pumpSource(): Point {
        source()?.let { return Point(it.x.toDouble(), it.y.toDouble()) }
        val g = geometry ?: return Point(0.0, 0.0)
        return ModeSwitchMotion.hitPoint(g.widthDp, g.heightDp)
    }

    private var cachedAt = Double.NaN
    private var cachedRun = -1
    private var cachedGeometry: ModeSwitchGeometry? = null
    private var cachedDrop: DropFrame? = null
    private var cachedPump: PumpFrame? = null

    /** The running drop at [tMs], worked out once per moment and shared by the overlay, the content and the stamp. */
    internal fun dropFrame(): DropFrame? {
        if (kind != ModeSwitchKind.DROP) return null
        val t = tMs
        val g = geometry ?: return null
        if (t != cachedAt || run != cachedRun || g != cachedGeometry || cachedDrop == null) {
            ModeSwitchProbe.frames++
            cachedDrop = ModeSwitchMotion.dropFrame(t, g.widthDp, g.heightDp)
            cachedPump = null
            cachedAt = t
            cachedRun = run
            cachedGeometry = g
        }
        return cachedDrop
    }

    /** The running pump at [tMs], as [dropFrame]. */
    internal fun pumpFrame(): PumpFrame? {
        if (kind != ModeSwitchKind.PUMP) return null
        val t = tMs
        val g = geometry ?: return null
        if (t != cachedAt || run != cachedRun || g != cachedGeometry || cachedPump == null) {
            ModeSwitchProbe.frames++
            cachedPump = ModeSwitchMotion.pumpFrame(t, g.widthDp, g.heightDp, pumpSource())
            cachedDrop = null
            cachedAt = t
            cachedRun = run
            cachedGeometry = g
        }
        return cachedPump
    }

    /** Starts [kind] from the switch at [switchCenter] (root px), whose row is [switchRow] (a press there is a flip). */
    fun start(kind: ModeSwitchKind, switchCenter: Offset? = null, switchRow: Rect? = null) {
        finish(ModeSwitchEnd.FLIP)
        feedback.start(kind)
        val to = kind.target
        if (!motionOn) {
            shownMode = if (stored == to) null else to
            return
        }
        this.switchCenter = switchCenter
        this.switchRow = switchRow
        clock.doubleValue = 0.0
        momentPassed = false
        held = false
        artReady = false
        shownMode = if (to == TrainingMode.POWERLIFTING) TrainingMode.BODYBUILDING else TrainingMode.POWERLIFTING
        this.kind = kind
        run++
    }

    /** Moves the clock to [t] ms: the moment turns the side, the end finishes. */
    fun advanceTo(t: Double) {
        val k = kind ?: return
        clock.doubleValue = t
        if (!momentPassed && t >= k.moment) {
            momentPassed = true
            shownMode = k.target
        }
        if (t >= k.end) finish(ModeSwitchEnd.END)
    }

    /** One frame of [dtMs], from the frame loop; nothing while held. */
    fun advanceBy(dtMs: Double) {
        if (held) return
        advanceTo(tMs + dtMs)
    }

    /** Holds the clock at [t] ms (tests and screenshots). */
    fun hold(t: Double) {
        held = true
        advanceTo(t)
    }

    /** A tap anywhere but the switch: to the end state, and the tap does nothing else. */
    fun skip() = finish(ModeSwitchEnd.SKIP)

    fun finish(reason: ModeSwitchEnd) {
        val k = kind ?: return
        kind = null
        held = false
        artReady = false
        if (reason != ModeSwitchEnd.END) feedback.stop()
        shownMode = if (stored == k.target) null else k.target
    }

    /** The side the stored settings hold, as last read. Once idle and in agreement, the theme follows it again. */
    fun noteStored(mode: TrainingMode) {
        stored = mode
        if (kind == null && shownMode == mode) shownMode = null
    }

    /** The host is gone: nothing may keep running or playing. */
    fun dispose() {
        finish(ModeSwitchEnd.LEAVE)
        feedback.release()
    }
}

/** The app's controller, provided by the theme host; null outside the app (previews, showcases, most tests). */
val LocalModeSwitch = staticCompositionLocalOf<ModeSwitchController?> { null }

/** For tests: counts of work done, and each overlay frame's draw time. */
internal object ModeSwitchProbe {
    @Volatile var frames = 0
    @Volatile var artBuilds = 0
    var onDraw: ((Long) -> Unit)? = null

    fun reset() {
        frames = 0
        artBuilds = 0
        onDraw = null
    }
}
