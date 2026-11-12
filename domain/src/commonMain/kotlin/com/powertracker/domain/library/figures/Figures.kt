package com.powertracker.domain.library.figures

import com.powertracker.domain.library.figures.areas.armFigures
import com.powertracker.domain.library.figures.areas.backFigures
import com.powertracker.domain.library.figures.areas.calfFigures
import com.powertracker.domain.library.figures.areas.chestFigures
import com.powertracker.domain.library.figures.areas.coreFigures
import com.powertracker.domain.library.figures.areas.hamstringGluteFigures
import com.powertracker.domain.library.figures.areas.mainLiftFigures
import com.powertracker.domain.library.figures.areas.quadFigures
import com.powertracker.domain.library.figures.areas.shoulderFigures
import kotlin.concurrent.Volatile
import kotlin.math.roundToLong

/**
 * Every exercise figure, keyed by catalogue name, merged from the area files under `figures/areas/`.
 *
 * The rule for area files: an area's map must not build its figures. Each entry is a `Lazy` (`"Name" to lazy { x }`
 * over a private `by lazy` figure), so listing the map solves no pose, and a figure is built, and its poses solved,
 * only when it is first looked up. Looking up one exercise must never build another.
 */
object Figures {
    /**
     * The figures by name. Its names, size and `in` build nothing; `get` builds only the figure asked for; walking its
     * entries or values (the checks, the fingerprint) builds them all.
     */
    val all: Map<String, ExerciseFigure> by lazy {
        lazyFigures(
            listOf(
                mainLiftFigures, chestFigures, backFigures, shoulderFigures, armFigures, quadFigures,
                hamstringGluteFigures, calfFigures, coreFigures,
            ),
        )
    }

    /** Test-only entries, consulted before [all] by [byName] and never part of [all] or the fingerprint. */
    @Volatile private var testFigures: Map<String, ExerciseFigure> = emptyMap()

    internal fun registerForTest(name: String, figure: ExerciseFigure) {
        testFigures = testFigures + (name to figure)
    }

    internal fun unregisterForTest(name: String) {
        testFigures = testFigures - name
    }

    /** The figure stored under the catalogue name [name] exactly; `Library.figure` resolves aliases first. */
    internal fun byName(name: String): ExerciseFigure? = testFigures[name] ?: all[name]

    /** Whether a figure is stored under the catalogue name [name] exactly, without building it. */
    internal fun has(name: String): Boolean = name in testFigures || name in all

    /**
     * A hash of every figure's shape lists (both panels, names in order, numbers in hundredths), so the JVM and JS
     * builds can be checked against one golden value.
     */
    fun fingerprint(): String {
        val text = StringBuilder()
        for (name in all.keys.sorted()) {
            val figure = all.getValue(name)
            text.append(name).append('|').append(figure.startCaption).append('|').append(figure.endCaption).append('|')
                .append(figure.thumbnail.name).append('\n')
            for (which in Which.entries) {
                for (shape in FigureRenderer.shapes(figure, which)) {
                    val (kind, numbers) = when (shape) {
                        is Circle -> "c" to listOf(shape.cx, shape.cy, shape.r)
                        is Capsule -> "k" to listOf(shape.x1, shape.y1, shape.x2, shape.y2, shape.width)
                        is RoundRect -> "r" to listOf(shape.x, shape.y, shape.w, shape.h, shape.radius)
                        is Polygon -> "p" to shape.points
                    }
                    text.append(kind).append(shape.role.ordinal)
                    for (n in numbers) text.append(',').append((n * 100).roundToLong())
                    text.append(';')
                }
                text.append('\n')
            }
        }
        // FNV-1a, 64-bit, over the UTF-16 code units.
        var hash = 0xcbf29ce484222325UL
        for (ch in text) {
            hash = hash xor ch.code.toULong()
            hash *= 0x100000001b3UL
        }
        return "${all.size}:" + hash.toString(16).padStart(16, '0')
    }
}

/** The areas' figures as one map that builds each figure on first use; two figures under one name are refused. */
internal fun lazyFigures(areas: List<Map<String, Lazy<ExerciseFigure>>>): Map<String, ExerciseFigure> {
    val merged = LinkedHashMap<String, Lazy<ExerciseFigure>>()
    for (area in areas) for ((name, figure) in area) {
        require(merged.put(name, figure) == null) { "two figures for $name" }
    }
    return LazyFigureMap(merged)
}

private class LazyFigureMap(private val figures: Map<String, Lazy<ExerciseFigure>>) : AbstractMap<String, ExerciseFigure>() {
    override val keys: Set<String> get() = figures.keys
    override val size: Int get() = figures.size
    override fun containsKey(key: String): Boolean = key in figures
    override fun get(key: String): ExerciseFigure? = figures[key]?.value
    override fun isEmpty(): Boolean = figures.isEmpty()

    override val entries: Set<Map.Entry<String, ExerciseFigure>> by lazy {
        figures.entries.mapTo(LinkedHashSet()) { (name, figure) ->
            object : Map.Entry<String, ExerciseFigure> {
                override val key: String = name
                override val value: ExerciseFigure get() = figure.value
            }
        }
    }
}
