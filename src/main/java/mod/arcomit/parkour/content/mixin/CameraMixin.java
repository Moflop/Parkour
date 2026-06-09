package mod.arcomit.parkour.content.mixin;

import mod.arcomit.parkour.core.client.animation.camera.CameraAnimationManager;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 将相机动画位移注入原版Camera的setup流程。
 * <p>
 * 在每帧相机计算完成后，叠加由{@code CameraAnimationManager}驱动的额外位移， 实现跑酷动作的相机跟随动画（如翻滚时的视角下沉、蹬墙跳时的相机偏移）。
 * 仅在动画管理器处于播放状态且相机未脱离眼睛时生效。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	/** 像素单位到Minecraft方块单位的缩放系数（1/16） */
	private static final float PIXEL_TO_BLOCK_SCALE = 0.0625F;

	@Shadow
	public abstract Vec3 getPosition();

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Shadow
	public abstract float getYRot();

	@Inject(method = "setup", at = @At("TAIL"))
	public void parkour$applyCameraPositionAnimation(BlockGetter level, Entity entity,
			boolean detached, boolean thirdPersonReverse, float partialTick,
			CallbackInfo ci) {
		// 如果摄像机已脱离眼睛（第三人称），直接跳过不应用位移
		if (detached) {
			return;
		}

		CameraAnimationManager controller = CameraAnimationManager.INSTANCE;

		if (controller.isPlaying()) {
			float[] position = controller.getCurrentPosition();

			if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
				Vec3 currentPos = this.getPosition();

				Vector3f moveVector =
						new Vector3f(position[0] * PIXEL_TO_BLOCK_SCALE,
								position[1] * PIXEL_TO_BLOCK_SCALE,
								-position[2] * PIXEL_TO_BLOCK_SCALE);

				moveVector.rotateY(-this.getYRot() * Mth.DEG_TO_RAD);

				this.setPosition(currentPos.x + moveVector.x,
						currentPos.y + moveVector.y,
						currentPos.z + moveVector.z);
			}
		}
	}
}
