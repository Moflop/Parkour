package mod.arcomit.parkour.content.mixin;

import mod.arcomit.parkour.core.client.animation.camera.CameraAnimationManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 将相机动画位移注入原版Camera的update流程。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	private static final float PIXEL_TO_BLOCK_SCALE = 0.0625F;

	@Shadow
	public abstract Vec3 position();

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Shadow
	public abstract float yRot();

	@Inject(method = "update", at = @At("TAIL"))
	public void parkour$applyCameraPositionAnimation(DeltaTracker deltaTracker,
			CallbackInfo ci) {
		CameraAnimationManager controller = CameraAnimationManager.INSTANCE;

		if (controller.isPlaying()) {
			float[] position = controller.getCurrentPosition();

			if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
				Vec3 currentPos = this.position();

				Vector3f moveVector =
						new Vector3f(position[0] * PIXEL_TO_BLOCK_SCALE,
								position[1] * PIXEL_TO_BLOCK_SCALE,
								-position[2] * PIXEL_TO_BLOCK_SCALE);

				moveVector.rotateY(-this.yRot() * Mth.DEG_TO_RAD);

				this.setPosition(currentPos.x + moveVector.x,
						currentPos.y + moveVector.y,
						currentPos.z + moveVector.z);
			}
		}
	}
}
