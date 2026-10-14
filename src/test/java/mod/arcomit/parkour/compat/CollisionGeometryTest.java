package mod.arcomit.parkour.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollisionGeometryTest {
	private static final double TOLERANCE = 1.0E-9;
	private static final AABB UNIT = new AABB(0, 0, 0, 1, 1, 1);
	private static final AABB CENTERED = new AABB(-1, -1, -1, 1, 1, 1);
	private static final UnaryOperator<Vec3> IDENTITY = point -> point;

	@Test
	void sensorDirectionsPreserveIdentityAndFollowNativeGridYawTransition() {
		Vec3 pivot = new Vec3(121.5, 68, -39.25);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			Vec3 normal = new Vec3(direction.getStepX(), 0.4, direction.getStepZ());
			assertEquals(direction, CollisionGeometry.sensorDirection(normal, pivot, IDENTITY));
		}

		// Native JOML yaw -44.5/-45.5 snaps to 0/-90, flipping residual yaw at 45 degrees.
		double[] structureYaw = {44.5, 45.5};
		double[] nativeResidualYaw = {-44.5, 44.5};
		Direction[] expected = {Direction.EAST, Direction.SOUTH};
		for (int i = 0; i < structureYaw.length; i++) {
			Vec3 worldNormal = rotateY(new Vec3(1, 0, 0), Math.toRadians(structureYaw[i]));
			double sensorYaw = -Math.toRadians(nativeResidualYaw[i]);
			UnaryOperator<Vec3> sensorToWorld = point -> rotateY(point.subtract(pivot), sensorYaw)
					.add(pivot);
			assertEquals(expected[i], CollisionGeometry.sensorDirection(worldNormal, pivot, sensorToWorld));
		}
	}

	@Test
	void sensorDirectionsHandleSmallPitchAndRollWhenWorldCardinalChoosesWrongSlot() {
		Vec3 pivot = new Vec3(-12.5, 71, 103.25);
		double[] structureYaw = {45.5, 46};
		double[] roll = {5, 10};
		// Residuals from Sable's clampQuaternionToGrid/getHitBoxYaw for these rigid poses.
		double[] nativeResidualYaw = {44.9377190103846, 44.8770971734846};
		for (int i = 0; i < structureYaw.length; i++) {
			// Equivalent to JOML rotateY(-yaw).rotateX(-10).rotateZ(roll) applied to +X.
			Vec3 worldNormal = rotateY(rotateX(rotateZ(new Vec3(1, 0, 0),
					Math.toRadians(roll[i])), Math.toRadians(-10)), Math.toRadians(structureYaw[i]));
			double sensorYaw = -Math.toRadians(nativeResidualYaw[i]);
			UnaryOperator<Vec3> sensorToWorld = point -> rotateY(point.subtract(pivot), sensorYaw)
					.add(pivot);
			assertEquals(Direction.EAST, Direction.getNearest(worldNormal.x, 0, worldNormal.z));
			assertEquals(Direction.SOUTH,
					CollisionGeometry.sensorDirection(worldNormal, pivot, sensorToWorld));
			assertEquals(Direction.NORTH,
					CollisionGeometry.sensorDirection(worldNormal.scale(-1), pivot, sensorToWorld));
		}
	}

	@Test
	void identityBoundsPreserveEveryExtent() {
		AABB box = new AABB(-2.5, -11, 0.125, 7.25, 3, 18.5);
		assertBounds(box, CollisionGeometry.transformBounds(box, IDENTITY));
	}

	@Test
	void translatedBoundsPreserveSizeAndOffset() {
		Vec3 shift = new Vec3(-17.5, 62.25, 94.125);
		assertBounds(UNIT.move(shift), CollisionGeometry.transformBounds(UNIT,
				point -> point.add(shift)));
	}

	@Test
	void rotatedBoundsUseMixedCornersInsteadOfOnlyMinAndMax() {
		AABB box = new AABB(-2, -3, -5, 7, 11, 13);
		double diagonal = Math.sqrt(0.5);
		UnaryOperator<Vec3> transform = point -> rotateY(point, Math.PI / 4)
				.add(3, -7, 5);
		AABB expected = new AABB(3 - 15 * diagonal, -10, 5 - 7 * diagonal,
				3 + 12 * diagonal, 4, 5 + 20 * diagonal);
		assertBounds(expected, CollisionGeometry.transformBounds(box, transform));
	}

	@Test
	void identityDetectsPositiveVolumeOverlapAndContainment() {
		assertTrue(CollisionGeometry.intersects(new AABB(0.8, 0.3, 0.3, 1.2, 0.7, 0.7),
				UNIT, IDENTITY));
		assertTrue(CollisionGeometry.intersects(new AABB(0.2, 0.2, 0.2, 0.8, 0.8, 0.8),
				UNIT, IDENTITY));
	}

	@Test
	void identityRejectsARealGap() {
		assertFalse(CollisionGeometry.intersects(new AABB(1.00001, 0.2, 0.2, 2, 0.8, 0.8),
				UNIT, IDENTITY));
	}

	@Test
	void faceEdgeAndCornerTangencyAreNotCollisions() {
		assertFalse(CollisionGeometry.intersects(new AABB(1, 0.2, 0.2, 2, 0.8, 0.8),
				UNIT, IDENTITY));
		assertFalse(CollisionGeometry.intersects(new AABB(1, 1, 0.2, 2, 2, 0.8),
				UNIT, IDENTITY));
		assertFalse(CollisionGeometry.intersects(new AABB(1, 1, 1, 2, 2, 2),
				UNIT, IDENTITY));
	}

	@Test
	void negativeWorldCoordinatesDoNotChangeIntersection() {
		Vec3 shift = new Vec3(-127.25, -31.5, -250.75);
		UnaryOperator<Vec3> transform = point -> point.add(shift);
		assertTrue(CollisionGeometry.intersects(new AABB(0.4, 0.1, 0.2, 1.2, 0.9, 0.8)
				.move(shift), UNIT, transform));
		assertFalse(CollisionGeometry.intersects(new AABB(-1, 0.1, 0.2, -0.01, 0.9, 0.8)
				.move(shift), UNIT, transform));
	}

	@Test
	void yawFortyFiveDegreesDetectsTheActualRotatedSolid() {
		UnaryOperator<Vec3> transform = point -> rotateY(point, Math.PI / 4);
		AABB query = new AABB(0.9, -0.1, -0.1, 1.1, 0.1, 0.1);
		assertTrue(CollisionGeometry.intersects(query, CENTERED, transform));
	}

	@Test
	void yawFortyFiveDegreesRejectsAnEmptyBroadPhaseCorner() {
		UnaryOperator<Vec3> transform = point -> rotateY(point, Math.PI / 4);
		AABB query = new AABB(1.2, -0.1, 1.2, 1.3, 0.1, 1.3);
		assertTrue(CollisionGeometry.transformBounds(CENTERED, transform).intersects(query),
				"The broad-phase box alone would report a false wall here");
		assertFalse(CollisionGeometry.intersects(query, CENTERED, transform));
	}

	@Test
	void rotatedFaceTangencyIsRejectedButSmallPenetrationIsDetected() {
		UnaryOperator<Vec3> transform = point -> rotateY(point, Math.PI / 4);
		double edge = Math.sqrt(0.5);
		assertFalse(CollisionGeometry.intersects(new AABB(edge, -0.1, edge, 1, 0.1, 1),
				CENTERED, transform));
		assertTrue(CollisionGeometry.intersects(new AABB(edge - 0.001, -0.1,
				edge - 0.001, 1, 0.1, 1), CENTERED, transform));
	}

	@Test
	void pitchDetectsSolidAndRejectsEmptyCorner() {
		UnaryOperator<Vec3> transform = point -> rotateX(point, Math.PI / 4);
		assertTrue(CollisionGeometry.intersects(new AABB(-0.1, 0.9, -0.1, 0.1, 1.1, 0.1),
				CENTERED, transform));
		AABB corner = new AABB(-0.1, 1.2, 1.2, 0.1, 1.3, 1.3);
		assertTrue(CollisionGeometry.transformBounds(CENTERED, transform).intersects(corner));
		assertFalse(CollisionGeometry.intersects(corner, CENTERED, transform));
	}

	@Test
	void rollDetectsSolidAndRejectsEmptyCorner() {
		UnaryOperator<Vec3> transform = point -> rotateZ(point, Math.PI / 4);
		assertTrue(CollisionGeometry.intersects(new AABB(0.9, -0.1, -0.1, 1.1, 0.1, 0.1),
				CENTERED, transform));
		AABB corner = new AABB(1.2, 1.2, -0.1, 1.3, 1.3, 0.1);
		assertTrue(CollisionGeometry.transformBounds(CENTERED, transform).intersects(corner));
		assertFalse(CollisionGeometry.intersects(corner, CENTERED, transform));
	}

	@Test
	void combinedYawPitchAndRollPreserveSolidAndLeaveBoundsCornersEmpty() {
		Vec3 shift = new Vec3(-4, 10, 9);
		UnaryOperator<Vec3> transform = point -> rotateZ(rotateX(
				rotateY(point, Math.toRadians(40)), Math.toRadians(30)),
				Math.toRadians(20)).add(shift);
		assertTrue(CollisionGeometry.intersects(new AABB(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1)
				.move(shift), CENTERED, transform));
		AABB bounds = CollisionGeometry.transformBounds(CENTERED, transform);
		AABB corner = new AABB(bounds.maxX - 0.05, bounds.maxY - 0.05,
				bounds.maxZ - 0.05, bounds.maxX - 0.01, bounds.maxY - 0.01,
				bounds.maxZ - 0.01);
		assertTrue(bounds.intersects(corner));
		assertFalse(CollisionGeometry.intersects(corner, CENTERED, transform));
	}

	@Test
	void skewEdgesSeparateBoxesEvenWhenTheirFaceAxesOverlap() {
		AABB beam = new AABB(-1, -0.25, -0.5, 1, 0.25, 0.5);
		UnaryOperator<Vec3> transform = point -> rotateZ(rotateX(rotateY(point, 0.7), 0.4), 0.3);
		// This corner passes the six face-normal tests; an edge-cross-edge axis separates it.
		Vec3 center = new Vec3(1.1482904422356564, 0.09378552807724416, -0.14929820230290902);
		AABB query = new AABB(-0.1, -0.15, -0.2, 0.1, 0.15, 0.2).move(center);
		assertTrue(CollisionGeometry.transformBounds(beam, transform).intersects(query));
		assertFalse(CollisionGeometry.intersects(query, beam, transform));
	}

	@Test
	void remotePlotCoordinatesTransformBackToNearbyWorldGeometry() {
		Vec3 plotOrigin = new Vec3(1_000_000_000, 4096, -1_000_000_000);
		Vec3 worldOrigin = new Vec3(-23, 68, 72);
		AABB plotBlock = CENTERED.move(plotOrigin);
		UnaryOperator<Vec3> transform = point -> rotateY(point.subtract(plotOrigin),
				Math.PI / 4).add(worldOrigin);
		double radius = Math.sqrt(2);
		assertBounds(new AABB(-radius, -1, -radius, radius, 1, radius).move(worldOrigin),
				CollisionGeometry.transformBounds(plotBlock, transform));
		assertTrue(CollisionGeometry.intersects(new AABB(0.9, -0.1, -0.1, 1.1, 0.1, 0.1)
				.move(worldOrigin), plotBlock, transform));
		assertFalse(CollisionGeometry.intersects(new AABB(1.2, -0.1, 1.2, 1.3, 0.1, 1.3)
				.move(worldOrigin), plotBlock, transform));
	}

	@Test
	void thinVoxelShapeKeepsItsThicknessInsteadOfBecomingAFullBlock() {
		AABB thin = new AABB(-1, -1, -0.0005, 1, 1, 0.0005);
		assertTrue(CollisionGeometry.intersects(new AABB(-0.2, -0.2, -0.0002,
				0.2, 0.2, 0.0002), thin, IDENTITY));
		assertFalse(CollisionGeometry.intersects(new AABB(-0.2, -0.2, 0.001,
				0.2, 0.2, 0.002), thin, IDENTITY));
	}

	@Test
	void rotatedThinVoxelShapeRejectsEmptySpaceInsideItsBounds() {
		AABB thin = new AABB(-1, -1, -0.0005, 1, 1, 0.0005);
		UnaryOperator<Vec3> transform = point -> rotateY(point, Math.PI / 4);
		assertTrue(CollisionGeometry.intersects(new AABB(-0.01, -0.1, -0.01,
				0.01, 0.1, 0.01), thin, transform));
		AABB gap = new AABB(0.5, -0.1, -0.6, 0.6, 0.1, -0.5);
		assertTrue(CollisionGeometry.transformBounds(thin, transform).intersects(gap));
		assertFalse(CollisionGeometry.intersects(gap, thin, transform));
	}

	@Test
	void twoIdentityTransformsPreserveOverlapGapAndTangency() {
		assertTrue(CollisionGeometry.intersects(new AABB(0.8, 0.3, 0.3, 1.2, 0.7, 0.7),
				IDENTITY, UNIT, IDENTITY));
		assertFalse(CollisionGeometry.intersects(new AABB(1.00001, 0.2, 0.2, 2, 0.8, 0.8),
				IDENTITY, UNIT, IDENTITY));
		assertFalse(CollisionGeometry.intersects(new AABB(1, 0.2, 0.2, 2, 0.8, 0.8),
				IDENTITY, UNIT, IDENTITY));
	}

	@Test
	void nativeYawFootProbeDoesNotMistakeSideWallForGround() {
		double yaw = -Math.PI / 4; // rotateY helper has the opposite sign to JOML yaw.
		UnaryOperator<Vec3> wallToWorld = point -> rotateY(point, yaw);
		AABB wall = new AABB(0, -10, -10, 1, 2, 10);
		Vec3 feet = wallToWorld.apply(new Vec3(-0.301, 0, 0));
		AABB footProbe = new AABB(-0.3, -0.48, -0.3, 0.3, 0, 0.3).move(feet);
		UnaryOperator<Vec3> nativePlayerYaw = point -> rotateY(point.subtract(feet), yaw).add(feet);

		assertTrue(CollisionGeometry.intersects(footProbe, wall, wallToWorld),
				"An axis-aligned player probe protrudes through the native rotated hitbox");
		assertTrue(CollisionGeometry.transformBounds(footProbe, nativePlayerYaw)
				.intersects(CollisionGeometry.transformBounds(wall, wallToWorld)),
				"Inflated axis-aligned bounds cannot replace the exact oriented probe");
		assertFalse(CollisionGeometry.intersects(footProbe, nativePlayerYaw, wall, wallToWorld));
		AABB actualGround = new AABB(-2, -0.4, -1, 1, -0.2, 1);
		assertTrue(CollisionGeometry.intersects(footProbe, nativePlayerYaw, actualGround, wallToWorld),
				"Matching player yaw must retain real ground detection");
	}

	@Test
	void shallowRolledWallOverlapsFootProbeWithoutSupportingPlayer() {
		UnaryOperator<Vec3> wallToWorld = point -> rotateZ(point, Math.toRadians(-5));
		AABB wall = new AABB(0, -1, -0.5, 1, 0, 0.5);
		AABB player = new AABB(-0.6, 0, -0.3, 0, 1.8, 0.3);
		AABB footProbe = new AABB(-0.599, -0.05, -0.299, -0.001, 0.001, 0.299);

		assertFalse(CollisionGeometry.intersects(player, wall, wallToWorld),
				"The player's body only touches the wall at foot height");
		assertTrue(CollisionGeometry.intersects(footProbe, wall, wallToWorld),
				"Extending a probe downward crosses the inclined wall's side");
		assertFalse(CollisionGeometry.hasSupport(footProbe, IDENTITY, wall, wallToWorld),
				"The wall's real top face remains outside the player's footprint");
	}

	@Test
	void supportRequiresAnUpwardFaceAtFootHeight() {
		AABB footProbe = new AABB(-0.299, -0.05, -0.299, 0.299, 0.001, 0.299);
		AABB wall = new AABB(0.2, -1, -1, 1, 2, 1);
		assertTrue(CollisionGeometry.intersects(footProbe, wall, IDENTITY));
		assertFalse(CollisionGeometry.hasSupport(footProbe, IDENTITY, wall, IDENTITY),
				"A wall passing through the probe is not a supporting surface");
		assertTrue(CollisionGeometry.hasSupport(footProbe, IDENTITY,
				new AABB(-1, -1, -1, 1, 0, 1), IDENTITY));
		assertFalse(CollisionGeometry.hasSupport(footProbe, IDENTITY,
				new AABB(-1, 0, -1, 1, 1, 1), IDENTITY),
				"A downward-facing ceiling is not foot support");
	}

	@Test
	void thinTiltedDeckSupportsAnIndependentlyYawedFootProbe() {
		AABB footProbe = new AABB(-0.299, -0.05, -0.299, 0.299, 0.001, 0.299);
		AABB thinDeck = new AABB(-1, -0.0005, -1, 1, 0, 1);
		UnaryOperator<Vec3> deckToWorld = point -> rotateZ(point, Math.toRadians(30));
		UnaryOperator<Vec3> playerYaw = point -> rotateY(point, Math.PI / 4);
		assertTrue(CollisionGeometry.hasSupport(footProbe, playerYaw, thinDeck, deckToWorld));
	}

	@Test
	void supportUsesTheNativeUpwardNormalThreshold() {
		AABB footProbe = new AABB(-0.299, -0.05, -0.299, 0.299, 0.001, 0.299);
		AABB thinDeck = new AABB(-1, -0.0005, -1, 1, 0, 1);
		assertTrue(CollisionGeometry.hasSupport(footProbe, IDENTITY, thinDeck,
				point -> rotateZ(point, Math.toRadians(53))),
				"A top face with normal.y above 0.6 remains ground");
		assertFalse(CollisionGeometry.hasSupport(footProbe, IDENTITY, thinDeck,
				point -> rotateZ(point, Math.toRadians(54))),
				"A top face below the ground threshold is a wall");
	}

	@Test
	void supportCanUseALocalSideFaceAfterNinetyDegreeRoll() {
		AABB footProbe = new AABB(-0.299, 0.95, -0.299, 0.299, 1.001, 0.299);
		AABB turnedBlock = new AABB(0, -1, -1, 1, 1, 1);
		assertTrue(CollisionGeometry.hasSupport(footProbe, IDENTITY, turnedBlock,
				point -> rotateZ(point, Math.PI / 2)),
				"A local side face can be the world-facing deck on a rolled structure");
	}

	@Test
	void nativeYawForwardProbeKeepsSideWallClearAndFindsRealObstacle() {
		double yaw = -Math.PI / 4;
		UnaryOperator<Vec3> wallToWorld = point -> rotateY(point, yaw);
		AABB wall = new AABB(0, -10, -10, 1, 2, 10);
		Vec3 feet = wallToWorld.apply(new Vec3(-0.301, 0, 0));
		AABB playerBounds = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3).move(feet);
		AABB forwardProbe = playerBounds.expandTowards(0, 0, 0.05);
		UnaryOperator<Vec3> nativePlayerYaw = point -> rotateY(point.subtract(feet), yaw).add(feet);

		assertTrue(CollisionGeometry.intersects(forwardProbe, wall, wallToWorld));
		assertFalse(CollisionGeometry.intersects(forwardProbe, nativePlayerYaw, wall, wallToWorld),
				"The side wall must not stop a forward probe tangent to its surface");
		AABB actualObstacle = new AABB(-1, 0, 0.32, 0, 2, 1);
		assertTrue(CollisionGeometry.intersects(forwardProbe, nativePlayerYaw,
				actualObstacle, wallToWorld));
		AABB sideProbe = playerBounds.deflate(0.001).expandTowards(0.15, 0, 0);
		assertTrue(CollisionGeometry.intersects(sideProbe, nativePlayerYaw, wall, wallToWorld),
				"A native-yaw side probe still reaches the wall needed for parkour");
	}

	@Test
	void twoOrientedBoxesRetainEdgeSeparationAfterCommonRigidTransform() {
		AABB beam = new AABB(-1, -0.25, -0.5, 1, 0.25, 0.5);
		UnaryOperator<Vec3> beamRotation = point -> rotateZ(rotateX(rotateY(point, 0.7), 0.4), 0.3);
		Vec3 center = new Vec3(1.1482904422356564, 0.09378552807724416, -0.14929820230290902);
		AABB query = new AABB(-0.1, -0.15, -0.2, 0.1, 0.15, 0.2).move(center);
		UnaryOperator<Vec3> commonTransform = point -> rotateX(rotateY(point, 0.5), -0.8)
				.add(-12.5, 67, 14.125);
		UnaryOperator<Vec3> beamToWorld = point -> commonTransform.apply(beamRotation.apply(point));
		assertTrue(CollisionGeometry.transformBounds(query, commonTransform)
				.intersects(CollisionGeometry.transformBounds(beam, beamToWorld)));
		assertFalse(CollisionGeometry.intersects(query, commonTransform, beam, beamToWorld));
		assertFalse(CollisionGeometry.intersects(beam, beamToWorld, query, commonTransform));
		assertTrue(CollisionGeometry.intersects(new AABB(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1),
				commonTransform, beam, beamToWorld));
	}

	@Test
	void broadLedgeOverlapDoesNotCreateAnAdjacentGripFace() {
		AABB ledge = new AABB(0, 1, 0, 1, 2, 1);
		Vec3 player = new Vec3(-0.02, 0.5, 0.5);
		AABB southProbe = new AABB(-0.32, 1.5, 0.4, 0.28, 1.8, 1);
		assertTrue(CollisionGeometry.intersects(southProbe, ledge, UnaryOperator.identity()));
		assertTrue(CollisionGeometry.faceDistance(player, ledge, Direction.EAST) > 0);
		assertTrue(CollisionGeometry.faceDistance(player, ledge, Direction.SOUTH) < 0,
				"A side probe overlapping the broad ledge must not change the grip's face");
		assertTrue(CollisionGeometry.faceDistance(player, ledge, Direction.NORTH) < 0);
	}

	@Test
	void realNeighborFaceRemainsAvailableAfterRejectingTheBroadLedge() {
		Vec3 player = new Vec3(-0.02, 0.5, 0.5);
		AABB broadLedge = new AABB(0, 1, 0, 1, 2, 1);
		AABB corner = new AABB(-1, 1, 0.8, 0, 2, 1.8);
		AABB southProbe = new AABB(-0.32, 1.5, 0.4, 0.28, 1.8, 1);
		UnaryOperator<Vec3> rotatedShip = point -> rotateY(point, 0.63).add(17, 30, -4);
		assertTrue(CollisionGeometry.intersects(southProbe, rotatedShip, broadLedge, rotatedShip));
		assertTrue(CollisionGeometry.intersects(southProbe, rotatedShip, corner, rotatedShip));
		assertTrue(CollisionGeometry.faceDistance(player, broadLedge, Direction.SOUTH) < 0);
		assertEquals(0.3, CollisionGeometry.faceDistance(player, corner, Direction.SOUTH), TOLERANCE);
	}

	private static void assertBounds(AABB expected, AABB actual) {
		assertEquals(expected.minX, actual.minX, TOLERANCE, "minX");
		assertEquals(expected.minY, actual.minY, TOLERANCE, "minY");
		assertEquals(expected.minZ, actual.minZ, TOLERANCE, "minZ");
		assertEquals(expected.maxX, actual.maxX, TOLERANCE, "maxX");
		assertEquals(expected.maxY, actual.maxY, TOLERANCE, "maxY");
		assertEquals(expected.maxZ, actual.maxZ, TOLERANCE, "maxZ");
	}

	private static Vec3 rotateX(Vec3 point, double radians) {
		double cosine = Math.cos(radians), sine = Math.sin(radians);
		return new Vec3(point.x, cosine * point.y - sine * point.z,
				sine * point.y + cosine * point.z);
	}

	private static Vec3 rotateY(Vec3 point, double radians) {
		double cosine = Math.cos(radians), sine = Math.sin(radians);
		return new Vec3(cosine * point.x - sine * point.z, point.y,
				sine * point.x + cosine * point.z);
	}

	private static Vec3 rotateZ(Vec3 point, double radians) {
		double cosine = Math.cos(radians), sine = Math.sin(radians);
		return new Vec3(cosine * point.x - sine * point.y,
				sine * point.x + cosine * point.y, point.z);
	}
}
