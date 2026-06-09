package mod.arcomit.parkour.content.mixin;

import mod.arcomit.parkour.ParkourConfig;
import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端全方向冲刺输入Mixin。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(Input.class)
public abstract class InputMixin {
	/** 浮点输入比较的容差值，排除浮点舍入误差导致的微小值 */
	private static final double MIN_INPUT_MAGNITUDE = 1E-5;

	@Shadow
	public float leftImpulse;
	@Shadow
	public float forwardImpulse;

	@Inject(method = "hasForwardImpulse", at = @At("HEAD"), cancellable = true)
	private void hasImpulse(CallbackInfoReturnable<Boolean> cir) {
		if (ParkourConfig.enableOmniSprint) {
			cir.setReturnValue(
					Math.abs(this.forwardImpulse) > MIN_INPUT_MAGNITUDE || Math.abs(
							this.leftImpulse) > MIN_INPUT_MAGNITUDE);
		}
	}
}
