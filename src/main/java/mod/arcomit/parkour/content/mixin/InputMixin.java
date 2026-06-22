package mod.arcomit.parkour.content.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

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

	@ModifyReturnValue(method = "hasForwardImpulse", at = @At("RETURN"))
	private boolean modifyHasImpulse(boolean original) {
		if (ParkourConfig.enableOmniSprint && Minecraft.getInstance().player != null && !ParkourChecks.isVanillaState(
				ParkourContext.get(Minecraft.getInstance().player))) {
			return original || Math.abs(
					this.forwardImpulse) > MIN_INPUT_MAGNITUDE || Math.abs(
					this.leftImpulse) > MIN_INPUT_MAGNITUDE;
		}
		return original;
	}
}
