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

    @Test
    fun headCircle_topAtTheCrown_standingStatureExact() {
        val legStraight = Leg(0.0, 0.0)
        val pelvisY = -(Body.ANKLE_HEIGHT + Segment.SHIN.length + Segment.THIGH.length)
        val s = Pose(0.0, pelvisY, nearArm = Arm(0.0, 0.0), nearLeg = legStraight).skeleton()
        assertEquals(-Body.HEIGHT, s.head.y - Body.HEAD_RADIUS, 1e-6)
        // The chin is where the head segment starts.
        assertEquals(-Body.HEIGHT + Segment.HEAD.length, s.chin().y, 1e-6)
    }

    @Test
    fun jointLimits_weightBearingAnkle_trunkThighHip() {
        val pose = Pose(0.0, -90.0, nearArm = Arm(0.0, 0.0), nearLeg = Leg(135.0, 120.0, ankle = 33.0))
        assertEquals(emptyList(), JointLimits.problems(pose, View.SIDE))
        assertTrue(JointLimits.problems(pose.copy(nearLeg = Leg(135.0, 120.0, ankle = 40.0)), View.SIDE).single().startsWith("near ankle"))
        assertTrue(JointLimits.problems(pose.copy(nearLeg = Leg(145.0, 120.0)), View.SIDE).single().startsWith("near hip"))
        // The front view keeps abduction's range.
        assertTrue(JointLimits.problems(pose.copy(nearLeg = Leg(45.0, 0.0)), View.FRONT).isNotEmpty())
    }

    @Test
    fun armOutOfThePlane_drawsTheUpperArmShorter_andChecksTheTrueElbow() {
        val flat = Pose(0.0, -100.0, nearArm = Arm(0.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton()
        val out = Pose(0.0, -100.0, nearArm = Arm(0.0, 0.0, out = 60.0), nearLeg = Leg(0.0, 0.0)).skeleton()
        assertEquals(Segment.UPPER_ARM.length, (flat.near.elbow - flat.near.shoulder).length(), 1e-9)
        assertEquals(Segment.UPPER_ARM.length * 0.5, (out.near.elbow - out.near.shoulder).length(), 1e-9)
        // A drawn fold of 170° with the upper arm 60° out is a true bend of about 119°: inside the elbow's range.
        assertEquals(119.5, JointLimits.trueElbow(Arm(0.0, 170.0, out = 60.0)), 0.1)
        assertEquals(emptyList(), JointLimits.problems(Pose(0.0, -100.0, nearArm = Arm(10.0, 170.0, out = 60.0), nearLeg = Leg(0.0, 0.0)), View.SIDE))
        assertEquals(2, JointLimits.problems(Pose(0.0, -100.0, nearArm = Arm(10.0, 170.0), nearLeg = Leg(0.0, 0.0)), View.SIDE).size) // near and far
        assertEquals(2, JointLimits.problems(Pose(0.0, -100.0, nearArm = Arm(0.0, 0.0, out = 95.0), nearLeg = Leg(0.0, 0.0)), View.SIDE).size)
    }
    @Test
    fun forearmAndThighOutOfThePlane_drawnShorter_limitsOnTheTrueAngles() {
        val straight = Pose(0.0, -90.0, nearArm = Arm(90.0, 0.0, out = 20.0, forearmOut = 20.0), nearLeg = Leg(0.0, 0.0))
        val s = straight.skeleton()
        val reach = Segment.UPPER_ARM.length + Segment.FOREARM.length + Segment.HAND.length
        assertEquals(reach * kotlin.math.cos(20.0 * kotlin.math.PI / 180), (s.near.hand - s.near.shoulder).length(), 1e-6)
        // A straight arm out of the plane is still straight: its true elbow is 0.
        assertEquals(0.0, JointLimits.trueElbow(straight.nearArm), 1e-6)
        assertEquals(emptyList(), JointLimits.problems(straight, View.SIDE))
        assertTrue(JointLimits.problems(straight.copy(nearArm = Arm(90.0, 0.0, forearmOut = 95.0)), View.SIDE).any { "forearm" in it })
        // The front view ignores it.
        val front = straight.skeleton(View.FRONT)
        assertEquals(reach, (front.near.hand - front.near.shoulder).length(), 1e-6)

        val wide = Pose(0.0, -90.0, nearArm = Arm(0.0, 0.0), nearLeg = Leg(90.0, 90.0, out = 40.0))
        val w = wide.skeleton()
        assertEquals(Segment.THIGH.length * kotlin.math.cos(40.0 * kotlin.math.PI / 180), (w.near.knee - w.near.hip).length(), 1e-6)
        assertEquals(Segment.SHIN.length, (w.near.ankle - w.near.knee).length(), 1e-6) // the shin stays in the plane
        assertEquals(emptyList(), JointLimits.problems(wide, View.SIDE))
        // Beyond the 40° hip abduction range is reported.
        val tooWide = JointLimits.problems(wide.copy(nearLeg = Leg(90.0, 90.0, out = 45.0)), View.SIDE)
        assertEquals(1, tooWide.size, tooWide.toString())
        assertTrue(tooWide.single().startsWith("near hip abduction"), tooWide.toString())
    }

    @Test
    fun frontView_leanAndThighTowardsTheCamera_drawnShorter_limitsOnTheTrueAngles() {
        // A sumo-like set-up seen from the front: the torso leaning 40° towards the camera, the thighs drawn near level.
        val leg = Leg(hip = 85.0, knee = 85.0, out = 50.0)
        val pose = Pose(0.0, -50.0, 90.0, nearArm = Arm(0.0, 0.0), nearLeg = leg, lean = 40.0)
        val s = pose.skeleton(View.FRONT)
        assertEquals(Segment.TORSO.length * kotlin.math.cos(40.0 * kotlin.math.PI / 180), s.pelvis.y - s.shoulder.y, 1e-6)
        assertEquals(Segment.THIGH.length * kotlin.math.cos(50.0 * kotlin.math.PI / 180), (s.near.knee - s.near.hip).length(), 1e-6)
        // The side view ignores the lean.
        assertEquals(Segment.TORSO.length, (pose.skeleton().shoulder - pose.skeleton().pelvis).length(), 1e-6)
        // Drawn 85° out, the true abduction is about 40°; the hip flexion is the 3D trunk–thigh angle.
        assertEquals(39.8, JointLimits.trueAbduction(leg, View.FRONT), 0.1)
        assertEquals(0.0, JointLimits.trueFrontHipFlexion(Leg(0.0, 0.0), 0.0), 1e-6)
        assertEquals(90.0, JointLimits.trueFrontHipFlexion(Leg(0.0, 0.0, out = 90.0), 0.0), 1e-6)
        assertEquals(emptyList(), JointLimits.problems(pose, View.FRONT))
        // Too far out of the middle plane is reported, though the drawn hip angle is not checked on its own.
        val wide = JointLimits.problems(pose.copy(nearLeg = leg.copy(out = 30.0), farLeg = leg), View.FRONT)
        assertTrue(wide.single().startsWith("near hip abduction"), wide.toString())
        // Leaning far over a thigh pointed at the camera folds the hip past its range.
        val folded = JointLimits.problems(pose.copy(nearLeg = Leg(0.0, 0.0, out = 89.0), farLeg = Leg(0.0, 0.0), lean = 60.0), View.FRONT)
        assertTrue(folded.single().startsWith("near hip flexion"), folded.toString())
    }
}
