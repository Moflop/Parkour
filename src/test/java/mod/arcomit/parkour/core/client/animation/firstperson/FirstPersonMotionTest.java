package mod.arcomit.parkour.core.client.animation.firstperson;

import org.junit.jupiter.api.Test;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static org.junit.jupiter.api.Assertions.*;

class FirstPersonMotionTest {
    @Test void clipsFinishAndReturnHandsEvenWithoutRendering() {
        for (var action : FirstPersonAction.values()) {
            if (action.duration == 0) continue;
            var motion = new FirstPersonMotion();
            motion.play(action, 1);
            for (int i = 0; i < action.duration; i++) {
                motion.tick(FirstPersonAction.NONE, 1, 0);
                assertEquals(action, motion.action());
            }
            for (int i = 0; i < 8; i++) motion.tick(FirstPersonAction.NONE, 1, 0);
            assertEquals(FirstPersonAction.NONE, motion.action());
            assertEquals(0, motion.weight(1));
        }
    }

    @Test void successfulJumpReplacesVaultAndDoesNotRestartEveryTick() {
        var motion = new FirstPersonMotion();
        motion.play(FirstPersonAction.VAULT, -1);
        motion.tick(FirstPersonAction.NONE, -1, .1f);
        var before = motion.pose(1);
        motion.play(FirstPersonAction.VAULT_JUMP, -1);
        motion.tick(FirstPersonAction.NONE, -1, .2f);
        assertEquals(before, motion.pose(0));
        assertEquals(FirstPersonAction.VAULT_JUMP, motion.action());
        motion.reset();
        assertEquals(0, motion.weight(.5f));
        assertEquals(FirstPersonAction.NONE, motion.action());
    }

    @Test void allPosesKeepShouldersBehindCameraAndHaveFiniteForwardReaches() {
        for (var action : FirstPersonAction.values()) for (int side : new int[]{-1, 1}) {
            for (int tick = 0; tick < 100; tick++) {
                var pose = FirstPersonPose.sample(action, tick, tick * .2f, side);
                for (var arm : new FirstPersonPose.Arm[]{pose.left(), pose.right()}) {
                    assertTrue(arm.z() > .1f);
                    assertTrue(Float.isFinite(arm.dx()) && Float.isFinite(arm.dy()) && Float.isFinite(arm.dz()));
                    assertTrue(arm.dz() < -.2f);
                }
            }
        }
    }

    @Test void crawlStopsStrokingAtRestAndWallRunUsesTheCorrectSide() {
        var motion = new FirstPersonMotion();
        for (int i = 0; i < 50; i++) motion.tick(FirstPersonAction.CRAWL, 1, 0);
        var stopped = motion.pose(1);
        for (int i = 0; i < 50; i++) motion.tick(FirstPersonAction.CRAWL, 1, 0);
        assertEquals(stopped, motion.pose(1));
        var left = FirstPersonPose.sample(FirstPersonAction.WALL_RUN, 0, 0, -1);
        var right = FirstPersonPose.sample(FirstPersonAction.WALL_RUN, 0, 0, 1);
        assertEquals(-left.left().dx(), right.right().dx(), 1e-6);
        assertTrue(left.left().dx() <= 0);
        assertTrue(right.right().dx() >= 0);
    }

    @Test void catchingALedgeInterruptsTheJumpClip() {
        var motion = new FirstPersonMotion();
        motion.play(FirstPersonAction.WALL_JUMP, -1);
        motion.tick(FirstPersonAction.NONE, -1, .2f);
        motion.tick(FirstPersonAction.ARMHANG, -1, 0);
        assertEquals(FirstPersonAction.ARMHANG, motion.action());
    }

    @Test void armRotationKeepsTheHandUpWhileFollowingItsReachDirection() {
        for (Vector3f direction : new Vector3f[]{
                new Vector3f(.2f, .3f, -.9f),
                new Vector3f(0, -.99f, -.01f),
                new Vector3f(0, 1, 0)}) {
            direction.normalize();
            Quaternionf rotation = FirstPersonParkourRenderer.armRotation(new Vector3f(direction));
            Vector3f armAxis = rotation.transform(new Vector3f(0, 1, 0));
            assertTrue(armAxis.distance(direction) < 1.0e-5F);

            Vector3f expectedUp = new Vector3f(0, 1, 0).fma(-direction.y, direction);
            if (expectedUp.lengthSquared() < 1.0e-6F) {
                expectedUp.set(0, 0, -1).fma(direction.z, direction);
            }
            expectedUp.normalize();
            Vector3f handUp = rotation.transform(new Vector3f(0, 0, -1));
            assertTrue(handUp.dot(expectedUp) > .99999F);
        }
    }
}
