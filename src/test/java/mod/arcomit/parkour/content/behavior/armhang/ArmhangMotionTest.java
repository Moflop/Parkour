package mod.arcomit.parkour.content.behavior.armhang;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmhangMotionTest {
    @Test
    void heldJumpMustBeReleasedAfterCatchingBeforeAnotherAction() {
        var input = new ArmhangData();
        for (int tick = 0; tick < 40; tick++) assertFalse(input.pressJump(true));
        assertFalse(input.pressJump(false));
        assertTrue(input.pressJump(true));
        for (int tick = 0; tick < 40; tick++) assertFalse(input.pressJump(true));
        assertFalse(input.pressJump(false));
        assertFalse(input.pressJump(false));
        assertTrue(input.pressJump(true));
        assertFalse(input.pressJump(true));
    }

    @Test
    void shimmyAcceleratesWithoutAFullSpeedFirstTickAndStopsPromptly() {
        for (double maximum : new double[]{0.05, 0.15, 1}) {
            double speed = ArmhangMotion.shimmySpeed(0, 1, maximum);
            assertTrue(speed > 0 && speed < maximum, "A first stroke must ease into movement");
            for (int tick = 0; tick < 3; tick++) speed = ArmhangMotion.shimmySpeed(speed, 1, maximum);
            assertEquals(maximum, speed, 1e-12);
            for (int tick = 0; tick < 2; tick++) speed = ArmhangMotion.shimmySpeed(speed, 0, maximum);
            assertEquals(0, speed, 1e-12, "Releasing must not slide along the ledge");
            speed = maximum;
            for (int tick = 0; tick < 6; tick++) {
                speed = ArmhangMotion.shimmySpeed(speed, -1, maximum);
                assertTrue(Math.abs(speed) <= maximum + 1e-12);
            }
            assertEquals(-maximum, speed, 1e-12, "Reversing input must finish the old stroke promptly");
        }
    }

    @Test
    void analogInputKeepsItsStrengthAndMalformedInputsCannotMove() {
        double speed = 0;
        for (int tick = 0; tick < 8; tick++) speed = ArmhangMotion.shimmySpeed(speed, 0.25, 0.15);
        assertEquals(0.15 * 0.25, speed, 1e-12);
        for (double invalid : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertEquals(0, ArmhangMotion.shimmySpeed(invalid, 1, .15));
            assertEquals(0, ArmhangMotion.shimmySpeed(0, invalid, .15));
            assertEquals(0, ArmhangMotion.shimmySpeed(0, 1, invalid));
        }
        assertEquals(0, ArmhangMotion.shimmySpeed(.1, 1, 0));
        assertEquals(0, ArmhangMotion.shimmySpeed(.1, 1, -1));
    }

    @Test
    void mantleClearsTheLipBeforeMovingTheFootprintOntoThePlatform() {
        Vec3 start = new Vec3(400.7, 201.3, -20.5);
        for (Vec3 inward : new Vec3[]{new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
                new Vec3(0, 0, 1), new Vec3(0, 0, -1)}) {
            Vec3 lift = start.add(0, 1.74, 0), target = lift.add(inward.scale(.8));
            assertEquals(start, ArmhangMotion.mantlePosition(start, lift, target, -20));
            assertEquals(start, ArmhangMotion.mantlePosition(start, lift, target, 0));
            assertEquals(0, lift.distanceTo(ArmhangMotion.mantlePosition(start, lift, target,
                    ArmhangMotion.LIFT_TICKS)), 1e-12);
            assertEquals(0, target.distanceTo(ArmhangMotion.mantlePosition(start, lift, target,
                    ArmhangMotion.MANTLE_TICKS)), 1e-12);
            assertEquals(0, target.distanceTo(ArmhangMotion.mantlePosition(start, lift, target, 50)), 1e-12);
            Vec3 previous = start;
            for (double tick = .125; tick <= ArmhangMotion.MANTLE_TICKS; tick += .125) {
                Vec3 current = ArmhangMotion.mantlePosition(start, lift, target, tick);
                if (tick <= ArmhangMotion.LIFT_TICKS) {
                    assertEquals(start.x, current.x, 1e-12);
                    assertEquals(start.z, current.z, 1e-12);
                    assertTrue(current.y >= previous.y - 1e-12 && current.y <= lift.y + 1e-12);
                } else {
                    assertEquals(lift.y, current.y, 1e-12);
                    double along = current.subtract(lift).dot(inward);
                    assertTrue(along >= -1e-12 && along <= .8 + 1e-12);
                    assertTrue(current.subtract(previous).dot(inward) >= -1e-12);
                }
                assertTrue(current.distanceTo(previous) < .07, "The two movement phases must remain continuous");
                previous = current;
            }
        }
    }

    @Test
    void easingNeverOvershootsItsGripOrLandingEndpoint() {
        assertEquals(0, ArmhangMotion.smooth(-1));
        assertEquals(0, ArmhangMotion.smooth(0));
        assertEquals(1, ArmhangMotion.smooth(1));
        assertEquals(1, ArmhangMotion.smooth(2));
        double previous = 0;
        for (double t = 0; t <= 1; t += .01) {
            double eased = ArmhangMotion.smooth(t);
            assertTrue(eased >= previous && eased >= 0 && eased <= 1);
            previous = eased;
        }
    }
}
