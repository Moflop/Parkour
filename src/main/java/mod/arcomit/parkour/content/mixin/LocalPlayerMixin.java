package mod.arcomit.parkour.content.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.behavior.slide.SlideState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.init.ParkourAttachmentTypes;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 本地玩家疾跑与滑铲优化Mixin。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends LivingEntity {
	/** 全方向疾跑输入强度阈值（0.8=80%），玩家输入超过此值才允许从任意方向触发疾跑 */
	private static final float OMNI_SPRINT_INPUT_THRESHOLD = 0.8F;

	@Shadow
	public ClientInput input;

	protected LocalPlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Shadow
	@Override
	public abstract boolean isUnderWater();

	/**
	 * 全方向疾跑
	 */
	@ModifyReturnValue(method = "canStartSprinting", at = @At("RETURN"))
	private boolean modifyHasEnoughImpulseToStartSprinting(boolean original) {
		if((Object) this instanceof Player player && ParkourChecks.isVanillaState(
				ParkourContext.get(player))) {
			return original;
		}
		if (ParkourConfig.enableOmniSprint) {
			boolean omniSprintCondition = this.isUnderWater() ?
					this.input.hasForwardImpulse() :
					(Math.abs(this.input.getMoveVector().y) >= OMNI_SPRINT_INPUT_THRESHOLD ||
					 Math.abs(this.input.getMoveVector().x) >= OMNI_SPRINT_INPUT_THRESHOLD);
			return original || omniSprintCondition;
		}
		return original;
	}

	/**
	 * 滑铲不减速
	 */
	@Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
	public void slideNotSlowDown(CallbackInfoReturnable<Boolean> cir) {
		StateData stateData = this.getData(ParkourAttachmentTypes.PARKOUR_CONTEXT).state();
		if (stateData.getState() instanceof SlideState) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * 撞墙不打断疾跑
	 */
	@ModifyExpressionValue(
			method = "shouldStopRunSprinting",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z")
	)
	private boolean preventSprintInterruptionOnCollision(boolean original) {
		if((Object) this instanceof Player player && ParkourChecks.isVanillaState(
				ParkourContext.get(player))) {
			return original;
		}
		return false;
	}
}
