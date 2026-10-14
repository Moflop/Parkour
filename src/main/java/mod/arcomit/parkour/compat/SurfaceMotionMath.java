package mod.arcomit.parkour.compat;

import net.minecraft.world.phys.Vec3;

/** Subtract only transport already applied along the structure's movement. */
public final class SurfaceMotionMath {
	private SurfaceMotionMath() {}

	public static Vec3 remainingMotion(Vec3 expected, Vec3 observed) {
		double lengthSquared = expected.lengthSqr();
		if (lengthSquared < 1.0E-14) return Vec3.ZERO;
		double fraction = Math.clamp(observed.dot(expected) / lengthSquared, 0, 1);
		// Perpendicular movement (input, knockback, etc.) must not be pulled back to an old anchor.
		return expected.scale(1 - fraction);
	}
}
