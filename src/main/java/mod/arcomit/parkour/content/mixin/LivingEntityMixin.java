package mod.arcomit.parkour.content.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.event.LivingJumpCancellableEvent;
import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 攀爬机制优化Mixin。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	@Unique
	private static final Double DOWN_CLIMB_ACCELERATE_LOOK_MIN_PITCH = 20.0;
	@Unique
	private static final Double DOWN_CLIMB_ACCELERATE_LOOK_MAX_PITCH = 90.0;
	@Unique
	private static final int CLIMB_ACCELERATION_START_TICK = 0;
	@Unique
	private static final int CLIMB_ACCELERATION_FINISH_TICK = 60;
	@Shadow
	public boolean jumping;
	// 记录向下攀爬的刻数，用于加速
	@Unique
	private int climbDownTicks = 0;
	// 记录向上攀爬的刻数，用于加速
	@Unique
	private int climbUpTicks = 0;
	// 标记当前刻是否正在向上攀爬
	@Unique
	private boolean climbingUpThisTick = false;

	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Shadow
	public abstract boolean onClimbable();

	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	public void InjectOnClimbable(CallbackInfoReturnable<Boolean> cir) {
		if (!ParkourConfig.canClimbMoreBlocks)
			return;

		LivingEntity entity = (LivingEntity) (Object) this;

		BlockPos pos = entity.blockPosition();
		if (isClimbablePole(entity.level(), pos)) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * 判断方块是否可攀爬
	 */
	@Unique
	private boolean isClimbablePole(Level world, BlockPos blockPos) {
		BlockState blockState = world.getBlockState(blockPos);
		if (!blockState.is(ParkourTags.Blocks.CLIMBABLE)) {
			return false;
		}
		Block block = blockState.getBlock();
		BlockPos aboveBlockPos = blockPos.above();
		BlockPos belowBlockPos = blockPos.below();
		boolean isAboveStacked =
				isSameBlockType(world, aboveBlockPos, block) && isVerticalPlace(
						world, aboveBlockPos);
		boolean isBelowStacked =
				isSameBlockType(world, belowBlockPos, block) && isVerticalPlace(
						world, belowBlockPos);
		boolean isStackedVerticalPlace =
				isVerticalPlace(blockState) && (isAboveStacked || isBelowStacked);
		if (!isStackedVerticalPlace) {
			return false;
		}
		return true;
	}

	@Unique
	private boolean isVerticalPlace(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return isVerticalPlace(state);
	}

	@Unique
	private boolean isVerticalPlace(BlockState state) {
		Block checkBlock = state.getBlock();
		if (checkBlock instanceof RotatedPillarBlock) {
			return state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y;
		}
		if (checkBlock instanceof EndRodBlock) {
			Direction facing = state.getValue(EndRodBlock.FACING);
			return facing == Direction.UP || facing == Direction.DOWN;
		}
		return true;
	}

	/**
	 * 判断方块是否与目标方块类型相同
	 */
	@Unique
	private boolean isSameBlockType(Level world, BlockPos pos, Block targetBlock) {
		return world.isLoaded(pos) && world.getBlockState(pos).getBlock() == targetBlock;
	}

	/**
	 * 加速向上攀爬。 修改垂直移动速度参数，实现随时间加速的效果。
	 */
	@ModifyArg(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"),
			index = 1)
	private double accelerateUpClimbing(double vanillaClimbSpeed) {
		climbingUpThisTick = true;
		// 基础速度提升
		if (ParkourConfig.enableUpClimbSpeedIncrease) {
			vanillaClimbSpeed *= ParkourConfig.upClimbSpeedMultiplier;
		}
		if (ParkourConfig.enableClimbAccelerationOverTime) {
			// 在3秒（60刻）内，速度线性增加
			double maxSpeed =
					vanillaClimbSpeed * ParkourConfig.upClimbAccelerationMultiplier;
			double climbYSpeed =
					Mth.clampedMap(climbUpTicks, CLIMB_ACCELERATION_START_TICK,
							CLIMB_ACCELERATION_FINISH_TICK,
							vanillaClimbSpeed, maxSpeed);
			return Math.max(this.getDeltaMovement().y, climbYSpeed);
		} else {
			return Math.max(this.getDeltaMovement().y, vanillaClimbSpeed);
		}
	}

	/**
	 * 加速向下攀爬。 根据玩家的视角（俯仰角）调整下行速度。向下看时速度更快。
	 */
	@WrapOperation(method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
			at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D"))
	private double accelerateDownClimbing(double currentYSpeed, double vanillaDownSpeed, Operation<Double> original) {
		if (ParkourConfig.enableDownClimbSpeedIncrease) {
			double maxSpeedIncrease =
					vanillaDownSpeed * ParkourConfig.downClimbSpeedMultiplier;
			vanillaDownSpeed = Mth.clampedMap(this.getXRot(),
					DOWN_CLIMB_ACCELERATE_LOOK_MIN_PITCH,
					DOWN_CLIMB_ACCELERATE_LOOK_MAX_PITCH, vanillaDownSpeed,
					maxSpeedIncrease);
			;
		}
		if (ParkourConfig.enableClimbAccelerationOverTime) {
			double acceleratedSpeed =
					vanillaDownSpeed * ParkourConfig.downClimbAccelerationMultiplier;
			vanillaDownSpeed = Mth.clampedMap(climbDownTicks,
					CLIMB_ACCELERATION_START_TICK,
					CLIMB_ACCELERATION_FINISH_TICK, vanillaDownSpeed,
					acceleratedSpeed);
		}
		return original.call(currentYSpeed, vanillaDownSpeed);
	}


	/**
	 * 更新攀爬计时器。 在每刻结束时调用，用于统计连续攀爬的时间。
	 */
	@Inject(method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
			at = @At("RETURN"), cancellable = true)
	private void updateClimbTimer(CallbackInfoReturnable<Vec3> cir) {

		Vec3 movement = cir.getReturnValue();
		// 如果在梯子上，且正在向下移动，且视角向下超过20度，增加下行计时器
		if (this.onClimbable() && movement.y < 0 && this.getXRot() > DOWN_CLIMB_ACCELERATE_LOOK_MIN_PITCH.intValue()) {
			climbDownTicks++;
		} else {
			climbDownTicks = 0;
		}

		if (climbingUpThisTick) {
			climbUpTicks++;
			climbingUpThisTick = false;
		} else {
			climbUpTicks = 0;
		}
		cir.setReturnValue(movement);
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
		if ((!this.onGround() && !this.jumping) || !ParkourConfig.climbableBlockNotSlowDown) {
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
