package mod.arcomit.parkour.content.behavior.barswing;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BarSwingMathTest {
	@Test
	void pumpingRestartsAfterBrakingAllTheWayToRest() {
		for (double initial : new double[]{-.8, 0, .8}) for (double input : new double[]{.3, 1}) {
			var step = new BarSwingMath.Step(initial, 0);
			for (int tick = 0; tick < 250; tick++) step = BarSwingMath.step(step.angle(), step.speed(), -1);
			double extent = 0;
			for (int tick = 0; tick < 200; tick++) {
				step = BarSwingMath.step(step.angle(), step.speed(), input);
				extent = Math.max(extent, Math.abs(step.angle()));
			}
			assertTrue(extent > .6, "Cannot restart: angle=" + initial + " input=" + input + " extent=" + extent);
		}
	}

	@Test
	void viewLimitsUseTheShortestArcAcrossNorth() {
		assertEquals(255, BarSwingMath.clampYaw(-80, 180), 1e-5);
		assertEquals(105, BarSwingMath.clampYaw(80, 180), 1e-5);
		assertEquals(181, BarSwingMath.clampYaw(-179, 180), 1e-5);
		assertEquals(-75, BarSwingMath.clampYaw(-140, 0), 1e-5);
	}

	@Test
	void tiltedBarKeepsItsSwingLengthWithoutMovingSideways() {
		Vec3 axis = new Vec3(1, .2, .3).normalize();
		var frame = BarSwingAnchor.frame(new Vec3(4, 8, 12), axis, new Vec3(0, 0, 1));
		assertEquals(0, frame.forward().dot(axis), 1e-9);
		assertEquals(new Vec3(0, -1, 0), frame.down());
		for (double angle : new double[]{-1, -.3, 0, .5, 1}) {
			Vec3 offset = BarSwingMath.position(frame.anchor(), frame.forward(), frame.down(), angle).subtract(frame.anchor());
			assertEquals(0, offset.dot(axis.multiply(1, 0, 1).normalize()), 1e-9);
			assertEquals(angle, Math.atan2(offset.dot(frame.forward()), offset.dot(frame.down())), 1e-9);
			assertEquals(2.2, offset.length(), 1e-9);
		}
	}

	@Test
	void pumpingIsBoundedAndTheConstraintKeepsItsLength() {
		var step = new BarSwingMath.Step(-0.35, 0.12);
		var anchor = new Vec3(2, 8, -3);
		var forward = new Vec3(0, 0, -1);
		for (int tick = 0; tick < 2000; tick++) {
			step = BarSwingMath.step(step.angle(), step.speed(), 1);
			assertTrue(Math.abs(step.angle()) <= BarSwingMath.MAX_ANGLE);
			assertTrue(Math.abs(step.speed()) <= BarSwingMath.MAX_ANGULAR_SPEED);
			assertEquals(2.2, BarSwingMath.position(anchor, forward, step.angle()).distanceTo(anchor), 1e-9);
		}
	}

	@Test
	void brakeDissipatesTheSwingAndReleaseStillLiftsAtEitherEnd() {
		var step = new BarSwingMath.Step(-0.8, 0.2);
		for (int tick = 0; tick < 200; tick++) step = BarSwingMath.step(step.angle(), step.speed(), -1);
		assertEquals(0, step.angle(), 0.002);
		assertEquals(0, step.speed(), 0.002);
		for (double angle : new double[]{-1, 0, 1}) {
			var velocity = BarSwingMath.launch(new Vec3(1, 0, 0), angle, 0.2, new Vec3(-1, 0, 0), 1);
			assertTrue(velocity.x < -0.5);
			assertEquals(0, velocity.z);
			assertTrue(velocity.y >= 0.52 && velocity.y <= 0.72);
		}
	}
}
