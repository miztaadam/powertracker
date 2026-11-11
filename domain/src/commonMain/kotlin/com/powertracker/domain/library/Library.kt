package com.powertracker.domain.library

import com.powertracker.domain.library.areas.arms
import com.powertracker.domain.library.areas.back
import com.powertracker.domain.library.areas.calves
import com.powertracker.domain.library.areas.chest
import com.powertracker.domain.library.areas.core
import com.powertracker.domain.library.areas.hamstringsGlutes
import com.powertracker.domain.library.areas.mainLifts
import com.powertracker.domain.library.areas.quads
import com.powertracker.domain.library.areas.shoulders

/** The exercise catalogue: static data compiled into both apps, and the search both pickers use. */
object Library {
    /** The fixed last line of every guide sheet. Kept out of the guide data; the UI appends it. */
    const val SAFETY_LINE = "Stop if you feel sharp pain. For an injury or a condition, check with a doctor or physiotherapist."

    private val catalogue: List<LibraryExercise> by lazy {
        mainLifts + chest + back + shoulders + arms + quads + hamstringsGlutes + calves + core
    }

    private val byKey: Map<String, LibraryExercise> by lazy {
        buildMap {
            for (e in catalogue) {
                put(normalise(e.name), e)
                for (alias in e.aliases) put(normalise(alias), e)
            }
        }
    }

    private val alphabetical = compareBy<LibraryExercise>({ normalise(it.name) }, { it.name })

    /** The whole catalogue, area by area. */
    fun all(): List<LibraryExercise> = catalogue

    /** The entry whose name or alias equals [name] once both are normalised; null when there is none. */
    fun find(name: String): LibraryExercise? = byKey[normalise(name)]

    /**
     * Entries in [areas] (empty = any) that need any of [equipment] (empty = any) and match [query]. Ranked:
     * an exact name or alias, names starting with the query, names containing it, aliases, then muscles; alphabetical within a rank.
     * A blank query returns every entry in the filters, alphabetically.
     */
    fun search(
        query: String,
        areas: Set<BodyArea> = emptySet(),
        equipment: Set<Equipment> = emptySet(),
    ): List<LibraryExercise> {
        val q = normalise(query)
        val filtered = catalogue.filter { e ->
            (areas.isEmpty() || e.area in areas) && (equipment.isEmpty() || e.equipment.any { it in equipment })
        }
        if (q.isEmpty()) return filtered.sortedWith(alphabetical)
        return filtered
            .mapNotNull { e -> rank(e, q)?.let { it to e } }
            .sortedWith(compareBy<Pair<Int, LibraryExercise>> { it.first }.thenBy(alphabetical) { it.second })
            .map { it.second }
    }

    /** The muscle groups with at least one catalogue exercise, in the enum's order. */
    fun muscleGroups(): List<MuscleGroup> = groups

    private val groups: List<MuscleGroup> by lazy { MuscleGroups.present(catalogue) }

    /** Every entry that works [group] with any of its muscles and needs any of [equipment] (empty = any), alphabetically. */
    fun byMuscle(group: MuscleGroup, equipment: Set<Equipment> = emptySet()): List<LibraryExercise> =
        catalogue
            .filter { e -> MuscleGroups.works(e.muscles, group) && (equipment.isEmpty() || e.equipment.any { it in equipment }) }
            .sortedWith(alphabetical)

    private fun rank(e: LibraryExercise, q: String): Int? {
        val name = normalise(e.name)
        return when {
            // An exact name or alias first, so a former name ("Overhead press") still leads to its entry.
            name == q || e.aliases.any { normalise(it) == q } -> -1
            name.startsWith(q) -> 0
            q in name -> 1
            e.aliases.any { q in normalise(it) } -> 2
            e.muscles.any { q in normalise(it) } -> 3
            else -> null
        }
    }

    /** Lowercase, diacritics stripped, `-` and `_` as spaces, runs of spaces collapsed, ends trimmed. */
    fun normalise(text: String): String {
        val out = StringBuilder(text.length)
        for (c in text.lowercase()) {
            when {
                c in '̀'..'ͯ' -> Unit // combining marks left by decomposed input
                c == '-' || c == '_' || c.isWhitespace() -> if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
                else -> out.append(folds[c] ?: c)
            }
        }
        return out.toString().trimEnd()
    }

    private val folds: Map<Char, Char> = buildMap {
        fun fold(chars: String, base: Char) = chars.forEach { put(it, base) }
        fold("àáâãäåāăą", 'a')
        fold("çćĉċč", 'c')
        fold("ďđ", 'd')
        fold("èéêëēĕėęě", 'e')
        fold("ĝğġģ", 'g')
        fold("ĥħ", 'h')
        fold("ìíîïĩīĭįı", 'i')
        fold("ĵ", 'j')
        fold("ķ", 'k')
        fold("ĺļľŀł", 'l')
        fold("ñńņňŉ", 'n')
        fold("òóôõöøōŏő", 'o')
        fold("ŕŗř", 'r')
        fold("śŝşš", 's')
        fold("ţťŧ", 't')
        fold("ùúûüũūŭůűų", 'u')
        fold("ŵ", 'w')
        fold("ýÿŷ", 'y')
        fold("źżž", 'z')
    }
}
