package mod.arcomit.parkour.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamicSurfaceDirectionTest {
	@Test
	void invalidWallsCannotQueryOrCaptureDynamicSurfaces() {
		for (Direction invalid : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertNull(DynamicSurfaceMotion.attachedSource(null, invalid));
			assertFalse(DynamicSurfaceMotion.hasRenderSurface(null, invalid));
			assertEquals(Vec3.ZERO, DynamicSurfaceMotion.wallNormal(null, invalid));
			assertEquals(Vec3.ZERO, DynamicSurfaceMotion.readOnlyWallNormal(null, invalid));
			assertEquals(Vec3.ZERO, DynamicSurfaceMotion.jumpMotion(null, invalid));
		}
	}

	@Test
	void invalidWallsDoNotAddSurfaceMotionToJumpsOrTangents() {
		Vec3 velocity = new Vec3(0.12, 0.42, -0.08);
		for (Direction invalid : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertEquals(velocity, DynamicSurfaceMotion.jumpVelocity(null, invalid, velocity));
			assertEquals(velocity, DynamicSurfaceMotion.wallJumpVelocity(null, invalid, velocity));
			assertEquals(velocity.normalize(), DynamicSurfaceMotion.wallTangent(null, invalid, velocity));
			assertEquals(velocity.normalize(), DynamicSurfaceMotion.renderWallTangent(null, invalid, velocity, 0.5f));
		}
	}
}
