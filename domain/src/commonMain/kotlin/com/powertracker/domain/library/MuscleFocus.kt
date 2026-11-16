package com.powertracker.domain.library

import com.powertracker.domain.library.focus.armsFocus
import com.powertracker.domain.library.focus.backFocus
import com.powertracker.domain.library.focus.calvesCoreFocus
import com.powertracker.domain.library.focus.chestFocus
import com.powertracker.domain.library.focus.hamstringsGlutesFocus
import com.powertracker.domain.library.focus.mainLiftsFocus
import com.powertracker.domain.library.focus.quadsFocus
import com.powertracker.domain.library.focus.shouldersFocus

/**
 * Where a catalogue exercise's target muscle is stretched and where it is squeezed, for the bodybuilding side.
 * The lines are worked out from the exercise's own guide (never more range than its steps allow), not quoted.
 */
sealed interface MuscleFocus {
    /** At least one; each is a key of [Sources.all]. */
    val sources: List<SourceId>

    /** Two lines: each one sentence of at most 64 characters. */
    data class Lines(val stretch: String, val squeeze: String, override val sources: List<SourceId>) : MuscleFocus

    /** A hold or a carry: nothing to stretch or squeeze. The UI shows [Library.NO_FOCUS_LINE] in the guide. */
    data class NoFocus(override val sources: List<SourceId>) : MuscleFocus
}

/**
 * Every exercise's lines, keyed by catalogue name exactly, joined from the area files under `focus/`. Each area file
 * holds a `by lazy` list of pairs (not a map, so a name written twice is caught here rather than silently dropped).
 * Nothing is built until the first [Library.focus] call, so `Library.all()` and the picker do not pay for it.
 */
internal object MuscleFocuses {
    /** The area lists, in catalogue order. Tests read these to check each name appears once. */
    internal val areas: List<List<Pair<String, MuscleFocus>>> by lazy { listOf(mainLiftsFocus, chestFocus, backFocus, shouldersFocus, armsFocus, quadsFocus, hamstringsGlutesFocus, calvesCoreFocus) }

    internal val all: Map<String, MuscleFocus> by lazy {
        val merged = LinkedHashMap<String, MuscleFocus>()
        for (area in areas) for ((name, focus) in area) {
            require(merged.put(name, focus) == null) { "two focus entries for $name" }
        }
        merged
    }
}
