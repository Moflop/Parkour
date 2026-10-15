package mod.arcomit.parkour.content.mechanic.entitysupport;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntitySupportGeometryTest {

	private static final AABB SURFACE = new AABB(-0.45, 0, -0.45, 0.45, 1.4, 0.45);
	private static final AABB BODY = new AABB(-0.3, 1.4, -0.3, 0.3, 3.2, 0.3);

	@Test
	void onlyTheTopSupportsPlayers() {
		assertTrue(EntitySupportGeometry.isAbove(BODY, SURFACE));
		assertTrue(EntitySupportGeometry.isSupported(BODY, SURFACE));
		assertFalse(EntitySupportGeometry.isAbove(BODY.move(0, -0.1, 0), SURFACE));
		assertFalse(EntitySupportGeometry.isSupported(BODY.move(0, -0.1, 0), SURFACE));
		assertFalse(EntitySupportGeometry.isSupported(BODY.move(0, 0.1, 0), SURFACE));
	}

	@Test
	void edgeOverlapSupportsButExactSeparationDoesNot() {
		assertTrue(EntitySupportGeometry.isSupported(BODY.move(0.749, 0, 0), SURFACE));
		assertFalse(EntitySupportGeometry.isSupported(BODY.move(0.75, 0, 0), SURFACE));
		assertFalse(EntitySupportGeometry.isSupported(BODY.move(0, 0, -0.75), SURFACE));
	}

	@Test
	void reachUsesBoxDistanceAndRejectsDiagonalCornersOutsideTheRadius() {
		assertEquals(0, EntitySupportGeometry.distanceSquared(BODY, SURFACE));
		assertEquals(0.25, EntitySupportGeometry.distanceSquared(BODY.move(1.25, 0, 0), SURFACE), 1.0E-9);
		assertEquals(0.5, EntitySupportGeometry.distanceSquared(BODY.move(1.25, 0, 1.25), SURFACE), 1.0E-9);
		assertEquals(0.25, EntitySupportGeometry.distanceSquared(BODY.move(0, 0.5, 0), SURFACE), 1.0E-9);
	}

	@Test
	void closestPointClampsEveryAxisForOcclusionChecks() {
		assertEquals(new Vec3(0.45, 1.4, -0.45),
				EntitySupportGeometry.closestPoint(SURFACE, new Vec3(2, 3, -2)));
		assertEquals(new Vec3(0, 1, 0), EntitySupportGeometry.closestPoint(SURFACE, new Vec3(0, 1, 0)));
	}

	@Test
	void verticalInterpolationMatchesOnlyTheRecordedPath() {
		AABB after = SURFACE.move(1, 1, 0);
		AABB body = BODY.move(.5, .5, 0);
		assertEquals(SURFACE.move(.5, .5, 0), EntitySupportGeometry.interpolateSupport(body, SURFACE, after));
		assertNull(EntitySupportGeometry.interpolateSupport(body.move(2, 0, 0), SURFACE, after));
	}

	@Test
	void interpolationNeverExtrapolatesBeyondTheRecordedHeights() {
		AABB after = SURFACE.move(0, 1, 0);
		assertNull(EntitySupportGeometry.interpolateSupport(BODY.move(0, 1.1, 0), SURFACE, after));
		assertNull(EntitySupportGeometry.interpolateSupport(BODY.move(0, -.1, 0), SURFACE, after));
		assertNull(EntitySupportGeometry.interpolateSupport(BODY, SURFACE, SURFACE));
	}
}
