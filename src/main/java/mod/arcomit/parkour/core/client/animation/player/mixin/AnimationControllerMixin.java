package mod.arcomit.parkour.core.client.animation.player.mixin;

import com.zigythebird.playeranimcore.animation.AnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 修复 Player Animation Library 循环动画的 NaN 缺陷。
 * <p>
 * PAL 内部在计算循环动画的 adjustedTick 时可能产生负值，经后续插值运算后传播为 NaN， 导致骨骼矩阵异常和渲染崩坏。本 Mixin 通过 {@code @ModifyArg}
 * 在 {@code processCurrentAnimation} 的入口处将参数钳制为非负值。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(value = AnimationController.class, remap = false)
public class AnimationControllerMixin {

	/**
	 * 在传入 processCurrentAnimation 之前拦截并钳制 adjustedTick。
	 *
	 * @param adjustedTick PAL 计算出的当前帧进度浮点值，可能为负
	 * @return 钳制后的非负值，阻断 NaN 传播链
	 */
	@ModifyArg(method = "process(Lcom/zigythebird/playeranimcore/animation/AnimationData;)V",
			at = @At(value = "INVOKE",
					target = "Lcom/zigythebird/playeranimcore/animation/AnimationController;processCurrentAnimation(FLcom/zigythebird/playeranimcore/animation/AnimationData;)V"),
			index = 0)
	private float pal$clampLoopAdjustedTick(float adjustedTick) {
		return Math.max(0.0F, adjustedTick);
	}
}
