package mod.arcomit.parkour.core.client.animation.firstperson;

import org.junit.jupiter.api.Test;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static org.junit.jupiter.api.Assertions.*;

class FirstPersonMotionTest {
    @Test void endingActionsFinishTheirExitBeforeReleasingOwnership() {
        var lowered = FirstPersonPose.sample(FirstPersonAction.NONE, 0, 0, 1);
        for (var action : FirstPersonAction.values()) {
            if (action == FirstPersonAction.NONE) continue;
            var motion = new FirstPersonMotion();
            if (action.duration > 0) motion.play(action, 1);
            for (int tick = 0; tick < Math.max(8, action.duration); tick++) {
                motion.tick(action.duration > 0 ? FirstPersonAction.NONE : action, 1, .1f);
            }
            if (action == FirstPersonAction.SWIM_BOOST) {
                assertEquals(FirstPersonPose.sample(action, action.duration - 1, 0, 1), motion.pose(1));
                motion.tick(FirstPersonAction.NONE, 1, 0);
                assertFalse(motion.isActive(), action.name());
                continue;
            }
            var previous = motion.pose(1);
            int loweringTicks = action == FirstPersonAction.SUPPORT_JUMP_UP
                    || action == FirstPersonAction.VAULT_JUMP ? 3 : 5;
            for (int tick = 0; tick < loweringTicks; tick++) {
                motion.tick(FirstPersonAction.NONE, 1, 0);
                assertTrue(motion.isActive(), action.name());
                assertEquals(previous, motion.pose(0));
                var current = motion.pose(1);
                assertTrue(current.left().y() <= previous.left().y());
                assertTrue(current.right().y() <= previous.right().y());
                previous = current;
            }
            assertEquals(lowered, motion.pose(1));
            motion.tick(FirstPersonAction.NONE, 1, 0);
            assertFalse(motion.isActive(), action.name());
            assertEquals(lowered, motion.pose(0));
        }
    }

    @Test void anotherActionCanInterruptLoweringWithoutDroppingRenderOwnership() {
        var motion = new FirstPersonMotion();
        for (int i = 0; i < 8; i++) motion.tick(FirstPersonAction.CRAWL, 1, .1f);
        motion.tick(FirstPersonAction.NONE, 1, 0);
        motion.tick(FirstPersonAction.NONE, 1, 0);
        var before = motion.pose(1);
        motion.play(FirstPersonAction.WALL_JUMP, -1);
        motion.tick(FirstPersonAction.NONE, -1, 0);
        assertEquals(before, motion.pose(0));
        assertEquals(FirstPersonAction.WALL_JUMP, motion.action());
        assertTrue(motion.isActive());
        motion.reset();
        motion.tick(FirstPersonAction.NONE, 1, 0);
        assertFalse(motion.isActive());
    }

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

    @Test void allPosesKeepShouldersBehindCameraAndHaveFiniteNonzeroReaches() {
        for (var action : FirstPersonAction.values()) for (int side : new int[]{-1, 1}) {
            for (int tick = 0; tick < 100; tick++) {
                var pose = FirstPersonPose.sample(action, tick, tick * .2f, side);
                for (int hand : new int[]{-1, 1}) {
                    var arm = hand < 0 ? pose.left() : pose.right();
                    assertTrue(arm.z() > .1f);
                    assertTrue(Float.isFinite(arm.dx()) && Float.isFinite(arm.dy()) && Float.isFinite(arm.dz()));
                    assertTrue(arm.dx() * arm.dx() + arm.dy() * arm.dy() + arm.dz() * arm.dz() > 1.0e-6f);
                    // Wall running's free arm deliberately swings behind the camera.
                    if (action != FirstPersonAction.WALL_RUN || hand == side) {
                        assertTrue(arm.dz() < -.2f);
                    }
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
