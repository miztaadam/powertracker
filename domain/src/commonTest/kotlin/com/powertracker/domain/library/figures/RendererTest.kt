package com.powertracker.domain.library.figures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RendererTest {
    private val figure = FigureFixtures.benchDraft()

    @Test
    fun renderer_drawOrder_farBeforeBody_nearAfterWithGap() {
        for (which in Which.entries) {
            val shapes = FigureRenderer.shapes(figure, which)
            val roles = shapes.map { it.role }
            val lastFar = roles.lastIndexOf(Role.BODY_FAR)
            val firstEquipment = roles.indexOf(Role.EQUIPMENT)
            val lastEquipment = roles.lastIndexOf(Role.EQUIPMENT)
            val firstBody = roles.indexOf(Role.BODY)
            val firstGap = roles.indexOf(Role.GAP)
            val firstHeld = roles.indexOfFirst { it == Role.ACCENT || it == Role.ACCENT_FILL }
            assertTrue(lastFar >= 0 && firstEquipment >= 0 && firstBody >= 0 && firstGap >= 0 && firstHeld >= 0, roles.toString())
            assertTrue(lastFar < firstEquipment, "far limbs before fixed equipment")
            assertTrue(lastEquipment < firstBody, "fixed equipment before the body")
            assertTrue(firstBody < firstGap, "body before the near-limb gaps")
            assertTrue(roles.lastIndexOf(Role.BODY) < firstHeld, "held equipment last")
            assertTrue(roles.subList(firstHeld, roles.size).all { it == Role.ACCENT || it == Role.ACCENT_FILL })

            // Every near-limb capsule after the first gap sits on a wider GAP capsule with the same ends, drawn before it.
            val nearLimbs = shapes.withIndex().filter { (i, s) -> i > firstGap && s.role == Role.BODY && s is Capsule }
            assertEquals(6, nearLimbs.size, "near arm and leg: upper arm, forearm, hand, thigh, shin, foot")
            for ((i, limb) in nearLimbs) {
                limb as Capsule
                val gap = shapes.subList(firstGap, i).filterIsInstance<Capsule>().firstOrNull {
                    it.role == Role.GAP && it.x1 == limb.x1 && it.y1 == limb.y1 && it.x2 == limb.x2 && it.y2 == limb.y2
                }
                assertTrue(gap != null && gap.width > limb.width, "no gap under $limb")
            }
            // The far limbs are the same six segments.
            assertEquals(6, shapes.count { it.role == Role.BODY_FAR })
        }
    }

    @Test
    fun bothPanels_shareTransform() {
        val start = FigureRenderer.shapes(figure, Which.START)
        val end = FigureRenderer.shapes(figure, Which.END)
        // The bench, rack and floor do not move between the poses, so they land on the same view-box points.
        assertEquals(start.filter { it.role == Role.EQUIPMENT }, end.filter { it.role == Role.EQUIPMENT })
        // A fixed world point (the pelvis) maps to the same view-box point in both panels.
        assertEquals(
            FigureRenderer.toViewBox(figure, figure.start.pose.skeleton().pelvis),
            FigureRenderer.toViewBox(figure, figure.end.pose.skeleton().pelvis),
        )
        // The floor sits on the bottom margin.
        val floor = start.filterIsInstance<Capsule>().first { it.role == Role.EQUIPMENT }
        assertEquals(FigureRenderer.viewBox.height * (1 - FigureRenderer.MARGIN), floor.y1 + floor.width / 2, 0.02)
    }

    @Test
    fun allShapesInsideViewBox() {
        assertEquals(ViewBox(400.0, 300.0), FigureRenderer.viewBox)
        for (which in Which.entries) {
            for (shape in FigureRenderer.shapes(figure, which)) {
                val b = shape.bounds()
                assertTrue(b.minX >= -0.01 && b.minY >= -0.01 && b.maxX <= 400.01 && b.maxY <= 300.01, "$shape outside the view box")
            }
        }
    }

    @Test
    fun shapesAreCached() {
        assertSame(FigureRenderer.shapes(figure, Which.START), FigureRenderer.shapes(figure, Which.START))
        assertSame(FigureRenderer.shapes(figure, Which.END), FigureRenderer.shapes(figure, Which.END))
    }
}
