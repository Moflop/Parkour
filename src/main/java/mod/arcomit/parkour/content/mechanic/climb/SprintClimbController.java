package mod.arcomit.parkour.content.mechanic.climb;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 疾跑只修饰原版上下梯意图；短缓入、松键减速与离梯收速均在本地移动模拟中完成。
 * 每个实体独享实例，速度所有权和计时器不存档、不参与状态同步。
 */
public final class SprintClimbController {
	private static final int RAMP_TICKS = 8;
	// 上行是原版 handleRelativeFriction 返回值，travel 随后还会施加重力与阻力。
	private static final double VANILLA_UP_SPEED = 0.2;
	private static final double VANILLA_DOWN_SPEED = 0.15F;
	private static final double MAX_UP_SPEED = 0.5;
	private static final double MAX_DOWN_SPEED = 0.45;
	private static final double EPSILON = 1.0E-6;

	private int upTicks;
	private int downTicks;
	private boolean active;
	private boolean sprint;
	private boolean brake;
	private boolean climbedUp;
	private boolean boostedUp;
	private boolean carryingUpBoost;
	private boolean processedMovement;
	private double lastUpVelocity = Double.NaN;
	private double appliedUpSpeed;

	/** Called before the complete travel step, including optional structure transport. */
	public void beginTravel(Player player) {
		boolean ownsVelocity = Math.abs(player.getDeltaMovement().y - lastUpVelocity) < EPSILON;
		lastUpVelocity = Double.NaN;
		active = canControl(player);
		climbedUp = false;
		boostedUp = false;
		processedMovement = false;
		carryingUpBoost = ownsVelocity;
		if (!active) {
			reset();
			return;
		}
		if ((!ParkourConfig.enableUpClimbSpeedIncrease && !ParkourConfig.enableDownClimbSpeedIncrease)
				|| !onLadder(player)) {
			// A ladder may disappear or move away between ticks. Do not retain its boost.
			if (ownsVelocity) player.setDeltaMovement(limitUp(player.getDeltaMovement(), VANILLA_UP_SPEED));
			reset();
			return;
		}
		boolean sneaking = ParkourProxies.INPUT_PROXY.getShiftKeyDown(player);
		brake = sneaking && !ParkourProxies.INPUT_PROXY.getJumping(player);
		sprint = !sneaking && ParkourProxies.INPUT_PROXY.getSprintKeyDown(player);
	}

	/** The original ladder clamp still supplies horizontal movement and fall-distance handling. */
	public Vec3 beforeMove(Player player, Vec3 incoming, Vec3 vanilla) {
		if (!active || !onLadder(player)) return vanilla;
		if (brake) return withY(vanilla, 0.0);

		double y = vanilla.y;
		if (sprint && ParkourConfig.enableDownClimbSpeedIncrease && ParkourConfig.downClimbSpeedMultiplier > 1.0
				&& incoming.y < 0.0
				&& !ParkourProxies.INPUT_PROXY.getJumping(player)) {
			double limit = targetSpeed(VANILLA_DOWN_SPEED, ParkourConfig.downClimbSpeedMultiplier,
					downTicks, MAX_DOWN_SPEED);
			y = Math.max(incoming.y, -limit);
		}
		if (carryingUpBoost && y > 0.0) {
			// Clamp BEFORE move: changing only the later ascent value is one tick too late.
			boolean hasClimbInput = ParkourProxies.INPUT_PROXY.getJumping(player)
					|| ParkourProxies.INPUT_PROXY.getMoveVector(player).lengthSquared() > EPSILON;
			double target = targetSpeed(VANILLA_UP_SPEED, ParkourConfig.upClimbSpeedMultiplier,
					upTicks, MAX_UP_SPEED);
			double extra = sprint && hasClimbInput && ParkourConfig.enableUpClimbSpeedIncrease
					&& ParkourConfig.upClimbSpeedMultiplier > 1.0
					? (target - VANILLA_UP_SPEED) * ClimbableEdge.upwardBoostFactor(player) : 0.0;
			y = Math.min(y, VANILLA_UP_SPEED + extra);
		}
		return withY(vanilla, y);
	}

	/** Invoked only by vanilla's collision/jump ascent branch, after collision resolution. */
	public double upwardSpeed(Player player, double vanilla) {
		if (!active || !onLadder(player)) return vanilla;
		climbedUp = true;
		if (brake) return 0.0;
		if (!sprint || !ParkourConfig.enableUpClimbSpeedIncrease || ParkourConfig.upClimbSpeedMultiplier <= 1.0) {
			return vanilla;
		}

		double target = targetSpeed(vanilla, ParkourConfig.upClimbSpeedMultiplier,
				upTicks, MAX_UP_SPEED);
		appliedUpSpeed = vanilla + (target - vanilla) * ClimbableEdge.upwardBoostFactor(player);
		boostedUp = appliedUpSpeed > vanilla + EPSILON;
		return appliedUpSpeed;
	}

	/** A single update per movement step; reversing direction never inherits the other ramp. */
	public Vec3 afterMove(Player player, Vec3 movement) {
		processedMovement = true;
		if (!active) return movement;
		if (!onLadder(player)) {
			Vec3 result = carryingUpBoost || boostedUp ? limitUp(movement, VANILLA_UP_SPEED) : movement;
			reset();
			return result;
		}
		if (carryingUpBoost && !climbedUp) {
			movement = limitUp(movement, VANILLA_UP_SPEED);
		}
		if (sprint && climbedUp && ParkourConfig.enableUpClimbSpeedIncrease && ParkourConfig.upClimbSpeedMultiplier > 1.0) {
			upTicks = Math.min(upTicks + 1, RAMP_TICKS);
			downTicks = 0;
		} else if (sprint && !climbedUp && movement.y < 0.0 && !player.onGround()
				&& ParkourConfig.enableDownClimbSpeedIncrease && ParkourConfig.downClimbSpeedMultiplier > 1.0) {
			downTicks = Math.min(downTicks + 1, RAMP_TICKS);
			upTicks = 0;
		} else {
			upTicks = 0;
			downTicks = 0;
		}
		return movement;
	}

	public void finishTravel(Player player) {
		if (!active || !processedMovement || !canControl(player)) {
			reset();
			return;
		}
		Vec3 velocity = player.getDeltaMovement();
		// Larger impulses from later integrations must not be treated as our climb velocity.
		boolean ownsVelocity = boostedUp && velocity.y > 0.0 && velocity.y <= appliedUpSpeed + EPSILON;
		if (!onLadder(player)) {
			if (ownsVelocity) player.setDeltaMovement(limitUp(velocity, VANILLA_UP_SPEED));
			reset();
			return;
		}
		lastUpVelocity = ownsVelocity ? velocity.y : Double.NaN;
	}

	private static boolean canControl(Player player) {
		return player.isLocalPlayer() && player.isAlive() && ParkourChecks.canPerformAction(player)
				&& !player.isInWater() && !player.isInLava()
				&& ParkourContext.get(player).state().getState() instanceof DefaultState;
	}

	private static boolean onLadder(Player player) {
		// Scaffolding has its own sneak-to-descend controls.
		return player.onClimbable() && !player.getInBlockState().isScaffolding(player);
	}

	private static double targetSpeed(double vanilla, double multiplier, int ticks, double maximum) {
		// One setting owns each direction's target. Smoothing must not add another multiplier.
		if (multiplier <= 1.0) return vanilla;
		double target = Math.min(vanilla * multiplier, maximum);
		if (!ParkourConfig.enableClimbAccelerationOverTime) return target;
		double progress = Mth.clamp((ticks + 1.0) / RAMP_TICKS, 0.0, 1.0);
		progress = progress * progress * (3.0 - 2.0 * progress);
		return Mth.lerp(progress, vanilla, target);
	}

	private static Vec3 limitUp(Vec3 movement, double limit) {
		return movement.y > limit ? withY(movement, limit) : movement;
	}

	private static Vec3 withY(Vec3 movement, double y) {
		return new Vec3(movement.x, y, movement.z);
	}

	private void reset() {
		upTicks = 0;
		downTicks = 0;
		active = false;
		carryingUpBoost = false;
		boostedUp = false;
		lastUpVelocity = Double.NaN;
	}
}
