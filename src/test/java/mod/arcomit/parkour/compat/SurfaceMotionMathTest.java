package mod.arcomit.parkour.compat;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurfaceMotionMathTest {
	private static final double TOLERANCE = 1.0E-10;

	@Test
	void noTransportAppliedLeavesTheFullMovement() {
		Vec3 expected = new Vec3(0.4, 0.2, -0.6);
		assertVector(expected, SurfaceMotionMath.remainingMotion(expected, Vec3.ZERO));
	}

	@Test
	void fullyAppliedTransportIsNotAddedAgain() {
		Vec3 expected = new Vec3(-0.3, 0.5, 0.4);
		assertVector(Vec3.ZERO, SurfaceMotionMath.remainingMotion(expected, expected));
	}

	@Test
	void partiallyAppliedTransportLeavesOnlyTheRemainder() {
		Vec3 expected = new Vec3(0.4, 0.2, -0.6);
		assertVector(new Vec3(0.3, 0.15, -0.45),
				SurfaceMotionMath.remainingMotion(expected, new Vec3(0.1, 0.05, -0.15)));
	}

	@Test
	void movementBeyondTheAnchorDoesNotPullThePlayerBack() {
		assertVector(Vec3.ZERO, SurfaceMotionMath.remainingMotion(new Vec3(0, 0.5, 0),
				new Vec3(0, 1.2, 0)));
	}

	@Test
	void oppositeExternalMovementDoesNotIncreaseTransport() {
		Vec3 expected = new Vec3(0.4, 0.2, -0.6);
		assertVector(expected, SurfaceMotionMath.remainingMotion(expected, expected.scale(-2)));
	}

	@Test
	void perpendicularExternalMovementIsNotCancelled() {
		Vec3 expected = new Vec3(2, 0, 2);
		assertVector(expected, SurfaceMotionMath.remainingMotion(expected, new Vec3(-3, 4, 3)));
	}

	@Test
	void partialTransportWithSidewaysInputKeepsTheInputIndependent() {
		assertVector(new Vec3(1.5, 0, 0), SurfaceMotionMath.remainingMotion(
				new Vec3(2, 0, 0), new Vec3(0.5, 8, -4)));
	}

	@Test
	void stationaryStructureDoesNotCorrectUnrelatedMovement() {
		assertVector(Vec3.ZERO, SurfaceMotionMath.remainingMotion(Vec3.ZERO,
				new Vec3(3, -4, 5)));
	}

	private static void assertVector(Vec3 expected, Vec3 actual) {
		assertEquals(expected.x, actual.x, TOLERANCE, "x");
		assertEquals(expected.y, actual.y, TOLERANCE, "y");
		assertEquals(expected.z, actual.z, TOLERANCE, "z");
	}
}
