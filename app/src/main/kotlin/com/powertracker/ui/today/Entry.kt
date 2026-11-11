package com.powertracker.ui.today

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.formatRpe
import kotlin.math.roundToLong

const val MAX_KG = 1000.0
const val MAX_REPS = 99
const val MIN_RPE = 6.0
const val MAX_RPE = 10.0

/** What a typed number is for: a weight in kilograms, the reps in a set, or its RPE. */
enum class EntryKind { KG, REPS, RPE }

sealed interface EntryResult {
    data class Ok(val value: Double) : EntryResult

    enum class Error : EntryResult { EMPTY, NAN, RANGE }
}

// Digits with at most one decimal mark, a point or a comma. No sign and no exponent.
private val NUMBER = Regex("""\d+[.,]?\d*|[.,]\d+""")

// "1,000": a comma with exactly three digits after it is a thousands separator, not a decimal mark.
private val THOUSANDS = Regex("""\d*,\d{3}""")

/**
 * Reads what was typed into a number field. A comma counts as the decimal point. Weight is 0–1000 kg
 * rounded to two decimals, reps a whole number 1–99, RPE 6–10 snapped to the nearest half.
 */
fun parseEntry(text: String, kind: EntryKind): EntryResult {
    val typed = text.trim()
    if (typed.isEmpty()) return EntryResult.Error.EMPTY
    if (!NUMBER.matches(typed) || THOUSANDS.matches(typed)) return EntryResult.Error.NAN
    val n = typed.replace(',', '.').toDouble()
    return when (kind) {
        EntryKind.KG -> if (n in 0.0..MAX_KG) EntryResult.Ok((n * 100).roundToLong() / 100.0) else EntryResult.Error.RANGE
        EntryKind.REPS -> if (n in 1.0..MAX_REPS.toDouble() && n % 1.0 == 0.0) EntryResult.Ok(n) else EntryResult.Error.RANGE
        EntryKind.RPE -> if (n in MIN_RPE..MAX_RPE) EntryResult.Ok((n * 2).roundToLong() / 2.0) else EntryResult.Error.RANGE
    }
}

/** The short message under a refused entry; an empty field is not an error. */
fun entryMessage(kind: EntryKind, error: EntryResult.Error): String? = when (error) {
    EntryResult.Error.EMPTY -> null
    EntryResult.Error.NAN -> "Numbers only"
    EntryResult.Error.RANGE -> when (kind) {
        EntryKind.KG -> "0 to ${formatKg(MAX_KG)} kg"
        EntryKind.REPS -> "Whole, 1 to $MAX_REPS"
        EntryKind.RPE -> "${formatRpe(MIN_RPE)} to ${formatRpe(MAX_RPE)}"
    }
}

/** The editor value a field of [kind] types into. */
internal fun SetEditor.valueOf(kind: EntryKind): Double? = when (kind) {
    EntryKind.KG -> weightKg
    EntryKind.REPS -> reps.toDouble()
    EntryKind.RPE -> rpe
}

internal fun SetEditor.with(kind: EntryKind, value: Double): SetEditor = when (kind) {
    EntryKind.KG -> copy(weightKg = value)
    EntryKind.REPS -> copy(reps = value.toInt())
    EntryKind.RPE -> copy(rpe = value)
}

/**
 * Text in a field that is not the editor's value (yet). [base] is the editor value it was typed over:
 * when that has changed since (the coach's Apply), the text is stale and is never saved over the
 * newer value. [error] is set once the text has been refused.
 */
internal data class Draft(val text: String, val base: Double?, val error: String? = null)

internal typealias Drafts = Map<EntryKind, Draft>

/** Keeps what is being typed across a configuration change. */
internal val DraftsSaver: Saver<Drafts, Any> = listSaver(
    save = { drafts -> drafts.flatMap { (kind, draft) -> listOf(kind.name, draft.text, draft.base, draft.error) } },
    restore = { saved -> saved.chunked(4).associate { (kind, text, base, error) -> EntryKind.valueOf(kind as String) to Draft(text as String, base as Double?, error as String?) } },
)

internal data class Settled(val editor: SetEditor, val drafts: Drafts)

/** Puts every typed number that is good into the editor. What was refused stays, with its message; the rest is dropped. */
internal fun settle(editor: SetEditor, drafts: Drafts): Settled {
    var next = editor
    val kept = mutableMapOf<EntryKind, Draft>()
    for ((kind, draft) in drafts) {
        if (draft.base != editor.valueOf(kind)) continue
        if (draft.error != null) {
            kept[kind] = draft
            continue
        }
        when (val entry = parseEntry(draft.text, kind)) {
            is EntryResult.Ok -> next = next.with(kind, entry.value)
            is EntryResult.Error -> entryMessage(kind, entry)?.let { kept[kind] = draft.copy(error = it) }
        }
    }
    return Settled(next, kept)
}
