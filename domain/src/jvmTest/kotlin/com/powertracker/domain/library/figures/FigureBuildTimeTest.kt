package com.powertracker.domain.library.figures

import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every catalogue figure builds quickly (Task 14): a figure is built, and its two poses drawn, the first time its guide
 * opens or its row scrolls in (on Android on the main thread), so a slow one is felt. Each figure is built at least
 * three times, each in a fresh class space (the domain's classes loaded again, as AreaMapsLazyTest does, so nothing
 * is cached from an earlier build) after a warm-up of ten other figures; the best of its first three times must be
 * under [LIMIT_MS]. Most figures take about a millisecond; the bound is generous, and the best of three keeps a loaded
 * machine from failing it.
 */
class FigureBuildTimeTest {
    /** Loads `com.powertracker.domain.*` itself (child first), and everything else (Kotlin, the JDK) from the parent. */
    private class Fresh(parent: ClassLoader) : URLClassLoader(
        arrayOf(Figures::class.java.protectionDomain.codeSource.location),
        parent,
    ) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(getClassLoadingLock(name)) {
            if (!name.startsWith("com.powertracker.domain.")) return super.loadClass(name, resolve)
            findLoadedClass(name) ?: findClass(name).also { if (resolve) resolveClass(it) }
        }
    }

    /** One round in a fresh class space: every figure's build and draw time in milliseconds, by name. */
    private fun round(names: List<String>, warmUp: List<String>): Map<String, Double> {
        val loader = Fresh(javaClass.classLoader)
        try {
            val figuresClass = Class.forName(Figures::class.java.name, true, loader)
            val all = figuresClass.getMethod("getAll").invoke(figuresClass.getField("INSTANCE").get(null)) as Map<*, *>
            val rendererClass = Class.forName(FigureRenderer::class.java.name, true, loader)
            val renderer = rendererClass.getField("INSTANCE").get(null)
            val figureClass = Class.forName(ExerciseFigure::class.java.name, true, loader)
            val whichClass = Class.forName(Which::class.java.name, true, loader)
            val shapes = rendererClass.getMethod("shapes", figureClass, whichClass)
            val panels = whichClass.enumConstants.toList()
            fun build(name: String) {
                val figure = all[name] ?: error("no figure for $name")
                for (which in panels) shapes.invoke(renderer, figure, which)
            }
            warmUp.forEach(::build)
            return names.filter { it !in warmUp }.associateWith { name ->
                val t0 = System.nanoTime()
                build(name)
                (System.nanoTime() - t0) / 1e6
            }
        } finally {
            loader.close()
        }
    }

    @Test
    fun everyFigure_buildsWithinItsBound_bestOfThree() {
        val names = Figures.all.keys.sorted()
        // Four rounds, each warmed up on its own ten figures, so every figure is timed in at least three of them.
        val rounds = (0 until 4).map { r -> round(names, names.subList(10 * r, 10 * r + 10)) }
        val best = names.associateWith { n -> rounds.mapNotNull { it[n] }.take(3).also { check(it.size == 3) { n } }.min() }
        // The slowest five, for the record (the test's standard output).
        println("slowest five (best of three, ms): " + best.entries.sortedByDescending { it.value }.take(5).joinToString { "${it.key} ${round1(it.value)}" })
        val slow = best.filterValues { it >= LIMIT_MS }.mapValues { (_, ms) -> round1(ms) }
        assertEquals(emptyMap(), slow, "figures slower than $LIMIT_MS ms to build (best of three)")
    }

    private companion object {
        const val LIMIT_MS = 50.0
    }
}
