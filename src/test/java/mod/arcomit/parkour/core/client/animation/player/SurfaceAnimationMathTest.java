package mod.arcomit.parkour.core.client.animation.player;

import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurfaceAnimationMathTest {
	private static final float TOLERANCE = 1.0E-4f;

	@Test
	void modelFacesTheExactSurfaceAngleBetweenWorldAxes() {
		float correction = SurfaceAnimationMath.bodyYawRadians(37.0f, 0.0f, 0.0f, 0.5f);
		assertEquals(37.0f, correction * Mth.RAD_TO_DEG, TOLERANCE);
	}

	@Test
	void bodyCrossingTheYawBoundaryDoesNotTurnAwayFromTheWall() {
		assertEquals(0.0f, SurfaceAnimationMath.bodyYawRadians(180.0f,
				179.0f, -179.0f, 0.5f), TOLERANCE);
		assertEquals(0.0f, SurfaceAnimationMath.bodyYawRadians(-180.0f,
				-179.0f, 179.0f, 0.5f), TOLERANCE);
	}

	@Test
	void headCrossingTheYawBoundaryRemainsRelativeToTheSurface() {
		assertEquals(10.0f, SurfaceAnimationMath.relativeYaw(179.0f,
				-179.0f, 170.0f, 0.5f), TOLERANCE);
		assertEquals(-10.0f, SurfaceAnimationMath.relativeYaw(-179.0f,
				179.0f, -170.0f, 0.5f), TOLERANCE);
	}

	@Test
	void continuousStructureRotationDoesNotAccumulateAnimationLag() {
		for (int frame = 0; frame < 720; frame++) {
			float surfaceYaw = Mth.wrapDegrees(37.0f + frame * 0.5f);
			float bodyYaw = Mth.wrapDegrees(surfaceYaw - 24.0f);
			float correction = SurfaceAnimationMath.bodyYawRadians(surfaceYaw,
					bodyYaw, bodyYaw, 0.5f) * Mth.RAD_TO_DEG;
			assertEquals(0.0f, Mth.wrapDegrees(bodyYaw + correction - surfaceYaw), TOLERANCE);
		}
	}
}
