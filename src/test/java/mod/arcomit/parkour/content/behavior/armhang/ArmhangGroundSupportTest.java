package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.compat.CollisionGeometry;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmhangGroundSupportTest {
	private static final AABB PLAYER = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
	private static final UnaryOperator<Vec3> IDENTITY = UnaryOperator.identity();

	@Test
	void tinyLowerWallProjectionIsNotGroundOnAnySide() {
		AABB originalProbe = new AABB(-0.299, -0.48, -0.299, 0.299, 0.001, 0.299);
		AABB probe = ArmhangCollision.groundProbe(PLAYER);
		for (int side = 0; side < 4; side++) {
			UnaryOperator<Vec3> rotation = yaw(side * Math.PI / 2);
			for (double seamOffset : new double[]{0.0015, 0.002}) {
				AABB lowerWall = new AABB(0.3 - seamOffset, -1, -1, 1.3, -0.2, 1);
				AABB worldWall = CollisionGeometry.transformBounds(lowerWall, rotation);
				assertTrue(staticSupport(originalProbe, worldWall),
						"The original foot probe treats the seam's tiny top edge as ground");
				assertFalse(staticSupport(probe, worldWall));
				assertFalse(CollisionGeometry.hasSupport(probe, IDENTITY, lowerWall, rotation),
						"A dynamic lower wall must use the same seam allowance as static blocks");
			}
		}
	}

	@Test
	void recessedUpperWallDoesNotMakeTheStaticLowerWallGround() {
		for (int side = 0; side < 4; side++) {
			UnaryOperator<Vec3> rotation = yaw(side * Math.PI / 2);
			Vec3 playerOffset = rotation.apply(new Vec3(0.002, 0, 0));
			AABB probe = ArmhangCollision.groundProbe(PLAYER.move(playerOffset));
			AABB lowerWall = CollisionGeometry.transformBounds(
					new AABB(0.3, -1, -1, 1.3, -0.2, 1), rotation);
			assertFalse(staticSupport(probe, lowerWall),
					"Gripping a recessed dynamic upper wall must not manufacture static ground");
		}
	}

	@Test
	void overlappingWallSideAndCeilingDoNotCountAsGround() {
		AABB probe = ArmhangCollision.groundProbe(PLAYER);
		AABB wall = new AABB(0.2, -1, -1, 1, 2, 1);
		AABB ceiling = new AABB(-1, 0, -1, 1, 1, 1);
		assertTrue(probe.intersects(wall));
		assertTrue(probe.intersects(ceiling));
		assertFalse(staticSupport(probe, wall));
		assertFalse(staticSupport(probe, ceiling));
	}

	@Test
	void genuineOneCentimeterLedgeStillCountsAsGroundOnEverySide() {
		AABB probe = ArmhangCollision.groundProbe(PLAYER);
		AABB ledge = new AABB(0.29, -1, -1, 1.3, -0.2, 1);
		for (int side = 0; side < 4; side++) {
			UnaryOperator<Vec3> rotation = yaw(side * Math.PI / 2);
			assertTrue(staticSupport(probe, CollisionGeometry.transformBounds(ledge, rotation)));
			assertTrue(CollisionGeometry.hasSupport(probe, IDENTITY, ledge, rotation));
		}
	}

	@Test
	void nearbyFullFloorAndNarrowFenceKeepTheGroundClearanceLimit() {
		AABB probe = ArmhangCollision.groundProbe(PLAYER);
		for (double floorHeight : new double[]{0, -0.2, -0.479}) {
			assertTrue(staticSupport(probe, new AABB(-1, -1, -1, 1, floorHeight, 1)));
			assertTrue(staticSupport(probe,
					new AABB(-0.0625, -1, -0.0625, 0.0625, floorHeight, 0.0625)),
					"Narrow support beneath the player must still prevent low armhang entry");
		}
		assertFalse(staticSupport(probe, new AABB(-1, -1, -1, 1, -0.481, 1)));
	}

	@Test
	void nativeYawKeepsTheSeamClearAndDetectsARealDeck() {
		UnaryOperator<Vec3> nativeYaw = yaw(Math.PI / 4);
		AABB probe = ArmhangCollision.groundProbe(PLAYER);
		AABB seam = new AABB(0.298, -1, -1, 1.3, -0.2, 1);
		assertFalse(CollisionGeometry.hasSupport(probe, nativeYaw, seam, nativeYaw));
		AABB ledge = new AABB(0.29, -1, -1, 1.3, -0.2, 1);
		assertTrue(CollisionGeometry.hasSupport(probe, nativeYaw, ledge, nativeYaw));
		AABB deck = new AABB(-1, -0.001, -1, 1, 0, 1);
		UnaryOperator<Vec3> tiltedDeck = point -> {
			double tilt = Math.toRadians(10);
			return new Vec3(Math.cos(tilt) * point.x - Math.sin(tilt) * point.y,
					Math.sin(tilt) * point.x + Math.cos(tilt) * point.y - 0.2, point.z);
		};
		assertTrue(CollisionGeometry.hasSupport(probe, nativeYaw, deck, tiltedDeck));
	}

	private static boolean staticSupport(AABB probe, AABB obstacle) {
		return ArmhangCollision.hasGroundSupport(probe, List.of(Shapes.create(obstacle)));
	}

	private static UnaryOperator<Vec3> yaw(double angle) {
		return point -> new Vec3(Math.cos(angle) * point.x - Math.sin(angle) * point.z,
				point.y, Math.sin(angle) * point.x + Math.cos(angle) * point.z);
	}
}
