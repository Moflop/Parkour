package mod.arcomit.parkour.content.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangTravelGuard;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingPhysics;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.event.LivingJumpCancellableEvent;
import mod.arcomit.parkour.content.mechanic.climb.SprintClimbController;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.utils.ParkourChecks;
import mod.arcomit.parkour.utils.ClimbablePoles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 攀爬机制优化Mixin。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	@Unique
	private final SprintClimbController parkour$sprintClimb = new SprintClimbController();
	@Shadow
	public boolean jumping;
	@Shadow
	private Optional<BlockPos> lastClimbablePos;

	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Shadow
	public abstract boolean onClimbable();

	/** Includes Sable's travel-return transport in ledge and ladder exit handling. */
	@WrapMethod(method = "travel(Lnet/minecraft/world/phys/Vec3;)V")
	private void parkour$guardTravel(Vec3 input, Operation<Void> original) {
		Player player = (Object) this instanceof Player value ? value : null;
		if (player != null && BarSwingPhysics.travel(player)) return;
		ArmhangTravelGuard.Snapshot before = player == null ? null : ArmhangTravelGuard.begin(player);
		if (player != null) parkour$sprintClimb.beginTravel(player);
		if (player != null) ParkourContext.get(player).ground().getEntitySupport().beforeTravel(player);
		try {
			original.call(input);
		} finally {
			if (player != null) {
				ArmhangTravelGuard.finish(player, before);
				parkour$sprintClimb.finishTravel(player);
				ParkourContext.get(player).ground().getEntitySupport().afterTravel(player);
				BarSwingPhysics.finishReleaseTravel(player);
			}
		}
	}

	/** Hanging movement already checks the ledge and rolls back unsafe side steps. */
	@ModifyVariable(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"),
			argsOnly = true, ordinal = 0)
	private Vec3 parkour$armhangTravelInput(Vec3 input) {
		if ((Object) this instanceof Player player
				&& ParkourContext.get(player).state().getState() instanceof ArmhangState) {
			return Vec3.ZERO;
		}
		return input;
	}

	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	public void InjectOnClimbable(CallbackInfoReturnable<Boolean> cir) {
		if (!ParkourConfig.canClimbMoreBlocks || ((Object) this instanceof Player player && ParkourChecks.isVanillaState(
				ParkourContext.get(player)))) {
			return;
		}

		LivingEntity entity = (LivingEntity) (Object) this;

		BlockPos pos = entity.blockPosition();
		if (ClimbablePoles.isClimbable(entity.level(), pos)) {
			cir.setReturnValue(true);
			return;
		}
		BlockPos dynamicPos = DynamicCollisions.findClimbablePole(entity);
		if (dynamicPos != null) {
			// Native Sable ladders also retain their plot position for fall tracking.
			lastClimbablePos = Optional.of(dynamicPos);
			cir.setReturnValue(true);
		}
	}

	/** 原版决定何时上爬，疾跑键只改变这次上爬的目标速度。 */
	@ModifyArg(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"),
			index = 1)
	private double accelerateUpClimbing(double vanillaClimbSpeed) {
		return (Object) this instanceof Player player
				? parkour$sprintClimb.upwardSpeed(player, vanillaClimbSpeed) : vanillaClimbSpeed;
	}

	/** 下滑加速、潜行刹停和梯顶减速必须在实际 move 之前处理。 */
	@WrapOperation(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 parkour$controlClimbMovement(LivingEntity entity, Vec3 incoming, Operation<Vec3> original) {
		Vec3 vanilla = original.call(entity, incoming);
		return entity instanceof Player player
				? parkour$sprintClimb.beforeMove(player, incoming, vanilla) : vanilla;
	}

	/** 本刻越过梯顶或横向离梯时，不让额外上冲速度进入空中运动。 */
	@ModifyReturnValue(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
			at = @At("RETURN"))
	private Vec3 parkour$finishClimbMovement(Vec3 movement) {
		return (Object) this instanceof Player player
				? parkour$sprintClimb.afterMove(player, movement) : movement;
	}

	/**
	 * 优化碰撞判定。 仅当玩家有移动输入时才计算水平碰撞
	 */
	@ModifyExpressionValue(
			method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;horizontalCollision:Z", opcode = 180) // GETFIELD
	)
	private boolean optimizeHorizontalCollision(boolean original) {
		// 这里的 original 就是原版读取到的 livingEntity.horizontalCollision 的值
		LivingEntity livingEntity = (LivingEntity) (Object) this;

		if (this.level().isClientSide() && (livingEntity instanceof Player player)) {
			if (ParkourChecks.isVanillaState(ParkourContext.get(player))) {
				return original;
			}
			// 只有在有输入向量时才返回真实的碰撞状态
			return original && ParkourProxies.INPUT_PROXY.getMoveVector(player).length() > 0;
		}
		// 否则返回原版的碰撞状态
		return original;
	}


	/**
	 * 优化攀爬的水平移动限制，在地面时或在跳跃时不减速
	 */
	@WrapOperation(method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D"))
	private double modifyHorizontalClimbSpeed(double speed, double vanillaSpeedMin, double vanillaSpeedMax, Operation<Double> original) {
		if ((!this.onGround() && !this.jumping) || !ParkourConfig.climbableBlockNotSlowDown || ((Object) this instanceof Player player && ParkourChecks.isVanillaState(
				ParkourContext.get(player)))) {
			return original.call(speed, vanillaSpeedMin, vanillaSpeedMax);
		} else {
			return speed;
		}
	}


	@Inject(method = "jumpFromGround", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"),
			cancellable = true)
	private void onJumpFromGround(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;

		LivingJumpCancellableEvent event = new LivingJumpCancellableEvent(self);
		NeoForge.EVENT_BUS.post(event);

		if (event.isCanceled()) {
			ci.cancel();
		}
	}
}
