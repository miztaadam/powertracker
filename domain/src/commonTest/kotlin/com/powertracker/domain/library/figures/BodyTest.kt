package com.powertracker.domain.library.figures

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BodyTest {
    @Test
    fun segmentLengths_matchCitedTable() {
        // Fractions of stature from the cited table (Body.kt): Winter, as quoted by the PMC papers named there.
        val table = mapOf(
            Segment.UPPER_ARM to 0.186, Segment.FOREARM to 0.146, Segment.HAND to 0.108,
            Segment.THIGH to 0.245, Segment.SHIN to 0.246, Segment.FOOT to 0.152,
        )
        assertEquals(175.0, Body.HEIGHT)
        for ((segment, fraction) in table) {
            assertEquals(fraction * 175.0, segment.length, 0.1, segment.name)
        }
        // The derived head and torso close the standing height: ankle + shin + thigh + torso + neck + head = stature.
        val standing = Body.ANKLE_HEIGHT + Segment.SHIN.length + Segment.THIGH.length + Segment.TORSO.length +
            Segment.NECK.length + Segment.HEAD.length
        assertEquals(175.0, standing, 0.1)
    }

    @Test
    fun forwardKinematics_straightArmAt90_handDirectlyAboveShoulder() {
        // Lying face up (torso pointing to the left), shoulder flexed 90° with a straight elbow and wrist.
        val pose = Pose(100.0, -50.0, torso = 180.0, nearArm = Arm(shoulder = 90.0, elbow = 0.0), nearLeg = Leg(0.0, 0.0))
        val s = pose.skeleton()
        assertEquals(s.shoulder.x, s.near.elbow.x, 1e-6)
        assertEquals(s.shoulder.x, s.near.wrist.x, 1e-6)
        assertEquals(s.shoulder.x, s.near.hand.x, 1e-6)
        assertEquals(s.shoulder.y - Segment.UPPER_ARM.length - Segment.FOREARM.length - Segment.HAND.length, s.near.hand.y, 1e-6)
        assertEquals(50.4, abs(s.shoulder.x - s.pelvis.x), 0.1) // torso = 0.288 × 175

        // Standing: the same arm points straight forward (+x), level with the shoulder.
        val standing = Pose(0.0, -90.0, nearArm = Arm(90.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton()
        assertEquals(standing.shoulder.y, standing.near.hand.y, 1e-6)
        assertTrue(standing.near.hand.x > standing.shoulder.x)
        // and the head is above the shoulders, the feet below the pelvis.
        assertTrue(standing.head.y < standing.shoulder.y)
        assertTrue(standing.near.ankle.y > standing.pelvis.y)
    }

    @Test
    fun reach_placesTheGripAndAnkleOnTarget() {
        val shoulder = Point(40.0, -53.0)
        val target = Point(57.0, -66.0)
        val arm = Reach.arm(shoulder, target, torso = 180.0)
        val pose = Pose(90.4, -53.0, torso = 180.0, nearArm = arm, nearLeg = Leg(0.0, 0.0))
        val s = pose.skeleton()
        assertEquals(target.x, s.near.grip.x, 1e-6)
        assertEquals(target.y, s.near.grip.y, 1e-6)
        assertTrue(arm.elbow >= 0.0)

        val leg = Reach.leg(Point(95.0, -53.0), Point(127.0, -Body.ANKLE_HEIGHT), torso = 180.0, foot = 0.0)
        val legged = Pose(95.0, -53.0, torso = 180.0, nearArm = Arm(0.0, 0.0), nearLeg = leg).skeleton()
        assertEquals(127.0, legged.near.ankle.x, 1e-6)
        assertEquals(-Body.ANKLE_HEIGHT, legged.near.ankle.y, 1e-6)
        assertTrue(leg.knee >= 0.0)
        assertEquals(legged.near.heel.y, legged.near.toe.y, 1e-6) // foot flat
    }

    @Test
    fun jointOutsideLimits_isReported() {
        val fine = Pose(0.0, -90.0, nearArm = Arm(90.0, 30.0), nearLeg = Leg(10.0, 20.0))
        assertEquals(emptyList(), JointLimits.problems(fine, View.SIDE))

        val hyperextendedElbow = fine.copy(nearArm = Arm(90.0, -20.0))
        val problems = JointLimits.problems(hyperextendedElbow, View.SIDE)
        assertEquals(1, problems.size, problems.toString())
        assertTrue("near elbow" in problems.single(), problems.single())

        val farKnee = fine.copy(farLeg = Leg(10.0, 170.0))
        assertTrue(JointLimits.problems(farKnee, View.SIDE).single().startsWith("far knee"))

        // Shoulder extension past the most conservative cited range (60°).
        assertTrue(JointLimits.problems(fine.copy(nearArm = Arm(-70.0, 0.0), farArm = Arm(-70.0, 0.0)), View.SIDE).size == 2)
    }
}
