package mod.arcomit.parkour.content.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 原版第一人称手臂绘制前清除跑酷动画留下的模型变换。 */
@Mixin(PlayerRenderer.class)
public abstract class ParkourPlayerRendererMixin {

	@Inject(method = "renderHand", at = @At("HEAD"))
	private void parkour$resetVanillaArmPose(PoseStack stack, MultiBufferSource buffer, int packedLight,
			AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
		// renderHand 直接调用 setupAnim(LivingEntity, ...)，绕过 PAL 在
		// setupAnim(Entity, ...) 桥接方法中的重置，导致共享模型保留摆荡时的拉伸。
		// 在原版设置手部姿态前恢复初始变换；PAL 的摆荡渲染不经过此入口。
		arm.resetPose();
		sleeve.resetPose();
	}
}
