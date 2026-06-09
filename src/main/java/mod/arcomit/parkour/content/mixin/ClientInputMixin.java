package mod.arcomit.parkour.content.mixin;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.duck.IClientInputMixin;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
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
@Mixin(ClientInput.class)
public abstract class ClientInputMixin implements IClientInputMixin {
	/** 浮点输入比较的容差值，排除浮点舍入误差导致的微小值 */
	private static final double MIN_INPUT_MAGNITUDE = 1E-5;

	@Shadow
	protected Vec2 moveVector;

	@Inject(method = "hasForwardImpulse", at = @At("HEAD"), cancellable = true)
	private void hasImpulse(CallbackInfoReturnable<Boolean> cir) {
		if (ParkourConfig.enableOmniSprint) {
			cir.setReturnValue(
					Math.abs(this.moveVector.y) > MIN_INPUT_MAGNITUDE || Math.abs(
							this.moveVector.x) > MIN_INPUT_MAGNITUDE);
		}
	}

	@Override
	public void setMoveVector(float x, float y) {
		this.moveVector = new Vec2(x, y);
	}

	@Override
	public void setMoveVectorX(float x) {
		this.moveVector = new Vec2(x, this.moveVector.y);
	}

	@Override
	public void setMoveVectorY(float y) {
		this.moveVector = new Vec2(this.moveVector.x, y);
	}
}
