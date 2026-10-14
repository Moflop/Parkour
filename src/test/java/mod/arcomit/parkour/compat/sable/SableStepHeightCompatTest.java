package mod.arcomit.parkour.compat.sable;

import org.junit.jupiter.api.Test;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SableStepHeightCompatTest {
	@Test
	void liftingTheReferenceKeepsItOutsideTheResolvedContactPlane() {
		Vector3d up = new Vector3d(0, 1, 0);
		for (int side : new int[]{-1, 1}) {
			Vector3d normal = new Vector3d(0, -.001, side).normalize();
			Vector3d retreat = SableStepHeightCompat.groundedContactRetreat(.08, normal, up);
			assertEquals(0, retreat.dot(up), 1e-12, "Contact correction must not lift the query again");
			assertEquals(0, new Vector3d(up).mul(.08).add(retreat).dot(normal), 1e-12,
					"Ground correction must not re-enter the wall which native SAT just resolved");
			assertTrue(retreat.length() > 0 && retreat.length() < .002);
		}
		assertEquals(0, SableStepHeightCompat.groundedContactRetreat(.08,
				new Vector3d(0, .001, 1).normalize(), up).lengthSquared(), 0);
		assertEquals(0, SableStepHeightCompat.groundedContactRetreat(.08,
				new Vector3d(0, -.2, 1).normalize(), up).lengthSquared(), 0,
				"A larger overhang must retain its collision rather than get a large retreat");
	}

	@Test
	void tiltedStepUsesTheGroundPlaneFromEitherApproach() {
		Vector3d up = new Vector3d(0, 1, 0);
		for (int side : new int[]{-1, 1}) {
			// Opposite faces of a barely tilted step have opposite vertical normals.
			Vector3d incoming = new Vector3d(-side * .2, -.08, 0);
			Vector3d contact = new Vector3d(side, side * .001, 0).normalize(.025);
			double correction = SableStepHeightCompat.groundedStepReferenceCorrection(incoming, contact, up);
			Vector3d sweepStart = new Vector3d(incoming).add(contact).fma(correction, up);
			assertEquals(0, sweepStart.dot(up), 1e-12,
					"A legal step cannot begin its clearance sweep inside the supporting floor");
			assertEquals(incoming.x + contact.x, sweepStart.x, 0,
					"Ground reference correction must preserve the resolved wall contact");
		}
	}

	@Test
	void resolvedSupportAndUpwardMovementDoNotReceiveExtraLift() {
		Vector3d up = new Vector3d(0, 1, 0);
		assertEquals(0, SableStepHeightCompat.groundedStepReferenceCorrection(
				new Vector3d(.2, -.08, 0), new Vector3d(-.025, .08, 0), up), 0);
		assertEquals(0, SableStepHeightCompat.groundedStepReferenceCorrection(
				new Vector3d(.2, .1, 0), new Vector3d(-.025, -.001, 0), up), 0);
	}

	@Test
	void groundedReferenceFollowsTheNativePlayerUpDirection() {
		Vector3d up = new Vector3d(.6, .8, 0);
		Vector3d motion = new Vector3d(up).mul(-.08).add(0, 0, .2);
		Vector3d contact = new Vector3d(up).mul(-.000025).add(0, 0, -.025);
		double correction = SableStepHeightCompat.groundedStepReferenceCorrection(motion, contact, up);
		assertEquals(.080025, correction, 1e-12);
		assertEquals(0, new Vector3d(motion).add(contact).fma(correction, up).dot(up), 1e-12);
	}

	@Test
	void testsConfiguredEndpointWithoutTestingAnOverheightObstacle() {
		for (double maximum : new double[]{0.6F, 1.0F, 1.1F, 1.5F}) {
			List<Double> heights = testedHeights(maximum);
			assertEquals(maximum, heights.getLast(), 0.0);
			assertTrue(heights.stream().allMatch(height -> height <= maximum));
			assertTrue(heights.stream().anyMatch(height -> height >= maximum),
					"A collision-free step exactly at the configured limit must be tested");
			assertFalse(heights.stream().anyMatch(height -> height >= maximum + 0.001),
					"A higher obstacle must not become reachable by rounding the limit upward");
		}
	}

	@Test
	void blockedMaximumTerminatesInsteadOfRetestingForever() {
		double maximum = 1.1F;
		double increment = SableStepHeightCompat.stepIncrement(maximum, maximum, 0.0625);
		assertTrue(increment > 0);
		assertTrue(maximum + increment > maximum);
		assertEquals(19, testedHeights(maximum).size());
	}

	@Test
	void exactGridHeightHasNoDuplicateEndpoint() {
		List<Double> heights = testedHeights(1.0);
		assertEquals(17, heights.size());
		assertEquals(1, heights.stream().filter(height -> height == 1.0).count());
	}

	private static List<Double> testedHeights(double maximum) {
		List<Double> heights = new ArrayList<>();
		for (double height = 0; height <= maximum;
				height += SableStepHeightCompat.stepIncrement(height, maximum, 0.0625)) {
			assertTrue(heights.size() < 100, "Step search must terminate at a blocked maximum");
			heights.add(height);
		}
		return heights;
	}
}
