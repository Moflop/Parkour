package mod.arcomit.parkour.core.client.animation.player;

import net.minecraft.util.Mth;

/** Angular compensation shared by animations attached to a rendered wall face. */
public final class SurfaceAnimationMath {
	private SurfaceAnimationMath() {}

	/** Align the model to this frame's surface yaw, including continuous structure rotation. */
	public static float bodyYawRadians(float surfaceYaw, float previousBodyYaw,
			float bodyYaw, float partialTick) {
		return -relativeYaw(previousBodyYaw, bodyYaw, surfaceYaw, partialTick) * Mth.DEG_TO_RAD;
	}

	/** Interpolate across the shortest arc before subtracting the current surface frame. */
	public static float relativeYaw(float previousYaw, float yaw, float surfaceYaw,
			float partialTick) {
		return Mth.wrapDegrees(Mth.rotLerp(partialTick, previousYaw, yaw) - surfaceYaw);
	}
}
