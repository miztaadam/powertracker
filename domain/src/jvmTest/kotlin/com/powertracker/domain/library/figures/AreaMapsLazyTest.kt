package com.powertracker.domain.library.figures

import java.lang.reflect.Modifier
import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The laziness rule (Figures.kt): building the area maps solves no pose. Run in a fresh class space, so no other test
 * in this JVM can have built a figure first: the domain's classes are loaded again by a child-first class loader, the
 * real area maps are built through Figures.all's keys, size and `in`, and then, by reflection, every area file's
 * figure delegates (`private val x by lazy`) and every map entry must still be unbuilt, and no area file may hold an
 * eagerly built figure, pose or skeleton. Looking one figure up then builds that one only.
 */
class AreaMapsLazyTest {
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

    private val areaFiles = listOf("MainLifts", "Chest", "Back", "Shoulders", "Arms", "Quads", "HamstringsGlutes", "Calves", "Core")
        .map { "com.powertracker.domain.library.figures.areas.${it}Kt" }

    /** Every static Lazy field of the area files (the figures' and shared values' `by lazy` delegates), by name. */
    private fun lazies(loader: ClassLoader): Map<String, Lazy<*>> = buildMap {
        for (file in areaFiles) {
            val c = Class.forName(file, false, loader)
            for (f in c.declaredFields) {
                if (!Modifier.isStatic(f.modifiers) || !Lazy::class.java.isAssignableFrom(f.type)) continue
                f.isAccessible = true
                put("${c.simpleName}.${f.name}", f.get(null) as Lazy<*>)
            }
        }
    }

    @Test
    fun buildingTheAreaMaps_solvesNoPose() {
        val loader = Fresh(javaClass.classLoader)
        assertTrue(Class.forName(Figures::class.java.name, false, loader) !== Figures::class.java, "a fresh class space")
        val figuresClass = Class.forName(Figures::class.java.name, true, loader)
        val figures = figuresClass.getField("INSTANCE").get(null)
        val all = figuresClass.getMethod("getAll").invoke(figures) as Map<*, *>
        // Names, size and `in`: the Exercises tab's questions.
        val names = all.keys.map { it as String }
        assertTrue(names.size >= 61 && "Bench" in all && "Push-up" in all && all.size == names.size)

        val delegates = lazies(loader)
        val figureDelegates = delegates.filterKeys { it.endsWith("\$delegate") }
        assertTrue(figureDelegates.size >= names.size, "every figure is a lazy delegate: ${figureDelegates.size} for ${names.size}")
        // The area maps themselves are built (Figures.all asked for them), but nothing in them. (The areas not drawn yet
        // hold a plain empty map.)
        val maps = delegates.filterKeys { it.endsWith("Figures\$delegate") }
        assertTrue(maps.size >= 6 && "ChestKt.chestFigures\$delegate" in maps, maps.keys.toString())
        assertTrue(maps.values.all { it.isInitialized() })
        val built = delegates.filter { (k, v) -> k !in maps && v.isInitialized() }.keys
        assertEquals(emptySet(), built, "built while listing the names")
        for (map in maps.values) {
            for ((name, entry) in map.value as Map<*, *>) assertTrue(!(entry as Lazy<*>).isInitialized(), "$name built")
        }
        // No area file holds a figure, pose or skeleton built eagerly at class load.
        val eager = areaFiles.flatMap { file ->
            Class.forName(file, false, loader).declaredFields.filter { f ->
                Modifier.isStatic(f.modifiers) && f.type.simpleName in setOf("ExerciseFigure", "Pose", "Skeleton", "Scene")
            }.map { "${file.substringAfterLast('.')}.${it.name}" }
        }
        assertEquals(emptyList(), eager)

        // Looking one up builds that one (and what it shares), not the others.
        assertTrue(all["DB press"] != null)
        val after = lazies(loader).filter { (k, v) -> k !in maps && v.isInitialized() }.keys
        assertTrue("ChestKt.dbPress\$delegate" in after, after.toString())
        assertTrue(after.none { it.startsWith("MainLiftsKt.") || it.startsWith("BackKt.") }, after.toString())
        assertTrue("ChestKt.pushUpFigure\$delegate" !in after && "ChestKt.dbFly\$delegate" !in after, after.toString())
        loader.close()
    }
}
