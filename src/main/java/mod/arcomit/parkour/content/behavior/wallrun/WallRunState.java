package mod.arcomit.parkour.content.behavior.wallrun;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.behavior.wallrun.client.animation.player.WallRunPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.wallrun.server.ServerWallRunSound;
import mod.arcomit.parkour.content.client.init.ParkourPlayerAnimations;
import mod.arcomit.parkour.content.context.JumpData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import mod.arcomit.parkour.utils.BlockCollisions;
import mod.arcomit.parkour.utils.Directions;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 跑墙状态 —— 玩家在竖直墙面上水平跑动。
 *
 * <p>进入条件：冲刺中、身体一侧贴墙（头部和脚部均接触）、前方无阻挡、未危险下落。
 * 运行时玩家以配置的速度沿面朝方向移动，服务端每 tick 播对应墙面材质脚步声。 停止前进输入或宽限期结束后自动退出，退出时可垂直向上跳跃（墙跳）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallRunState extends AbstractParkourState {
	/** 跳跃后允许开始跑墙的最大 tick 数 */
	public static final int MAX_TICKS_SINCE_JUMP = 15;
	/** 玩家面朝方向与跑墙运动方向的允许角度偏差上限（度），超过则状态失效 */
	private static final float FACING_ANGLE_TOLERANCE = 60;

	/**
	 * 构造并注册状态转换：每客户端 tick 检测前进输入是否归零或跳跃释放宽限期是否结束， 满足任一即切回默认状态。
	 */
	public WallRunState() {
		registerTransitions(IParkourStateTransition.onLocalTick(ParkourStates.DEFAULT::get,
				(player, context) -> ParkourProxies.INPUT_PROXY.getForwardImpulse(
						player) <= ParkourConstants.ZERO_THRESHOLD || this.isGracePeriodOver(
						context)));
	}

	/**
	 * 检查玩家当前环境是否满足跑墙的基础要求。
	 *
	 * @param player 目标玩家，不能为 null
	 * @return 配置启用、冲刺中、不在地面、不在可攀爬方块、不在水/熔岩中、 可执行跑酷行为、且前方无阻挡时返回 true
	 */
	public static boolean meetsBaseConditions(Player player) {
		// 快速失败：基础状态不满足墙跑条件
		if (!ParkourConfig.enableWallRun || !player.isSprinting() || player.onGround() || player.onClimbable() || player.isInWater() || player.isInLava() || !ParkourChecks.canPerformBehavior(
				player)) {
			return false;
		}

		// 确保正前方没有撞墙
		if (BlockCollisions.isBlockedTowards(player, player.getDirection(),
				ParkourTags.Blocks.COMMON_IGNORED_BLOCKS)) {
			return false;
		}
		return true;
	}

	/**
	 * 检查跳跃释放后的宽限期是否已过。宽限期内即使松开前进键也不会退出。
	 *
	 * @return 宽限期 tick 数小于等于 0 时返回 true
	 */
	private boolean isGracePeriodOver(ParkourContext context) {
		return context.jump().getJumpReleaseGraceTicks() <= 0;
	}

	/**
	 * 进入跑墙时初始化物理运动方向、锁定碰撞墙体方向，并重置平行跳跃记录 使玩家跑墙后可向运动方向再次跳跃。
	 */
	@Override
	public void onEnter(Player player, ParkourContext context) {
		super.onEnter(player, context);
		WallMovementData wallMovementData = context.wall();
		JumpData jumpData = context.jump();
		WallRunPhysics.setupInitialMovement(player, wallMovementData);
		jumpData.resetLastParallelJump();
	}

	/**
	 * 客户端进入跑墙时向服务端同步当前坐标。
	 */
	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
	}

	/**
	 * 每模拟 tick 施加跑墙移动速度和墙面吸附力，同时重置跌落高度。
	 */
	@Override
	public void onSimulationTick(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();
		WallRunPhysics.applyWallRunMovement(player, wallMovementData);
	}

	/**
	 * 服务端每 tick 根据玩家移动距离播放墙面材质脚步声。
	 */
	@Override
	public void onServerTick(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();
		ServerWallRunSound.playFootstepSound(player, wallMovementData);
	}

	/**
	 * 生成动画变体 ID 以区分墙在左侧还是右侧。
	 *
	 * @return 0 表示墙在玩家左侧，1 表示墙在玩家右侧
	 */
	@Override
	public int generateVariant(Player player) {
		Direction wallDir = WallRunCollision.findFirstWallCollisionDirection(player);
		if (wallDir != null) {
			Direction facing = player.getDirection();
			return wallDir == facing.getCounterClockWise() ? 0 : 1;
		}
		return 0;
	}

	/**
	 * @return 跑墙期间始终保持站立姿态
	 */
	@Override
	public Pose getLinkedPose() {
		return Pose.STANDING;
	}

	/**
	 * 判断玩家是否可以从当前状态进入跑墙。
	 *
	 * <p>额外限制：一侧存在有效墙体碰撞且该方向不同于上次跑墙方向，
	 * 且玩家未处于危险下落状态。
	 *
	 * @param player  目标玩家，不能为 null
	 * @param context 跑酷上下文，不能为 null
	 * @return 满足所有条件返回 true
	 */
	@Override
	public boolean canEnter(Player player, ParkourContext context) {
		if (!meetsBaseConditions(player)) {
			return false;
		}
		Direction availableWallDir =
				WallRunCollision.findFirstWallCollisionDirection(player);
		if (availableWallDir == null) {
			return false;
		}
		WallMovementData wallMovementData = context.wall();
		if (availableWallDir == wallMovementData.getRunCollision()) {
			return false;
		}
		return !ParkourChecks.isFallUnsafe(player);
	}

	/**
	 * 判断跑墙状态是否仍然有效。
	 *
	 * <p>除基础条件和持续时间限制外，还需验证墙体碰撞仍有效、
	 * 且玩家面朝方向与记录的运动方向偏差不超过 {@value #FACING_ANGLE_TOLERANCE} 度。
	 *
	 * @param player  目标玩家，不能为 null
	 * @param context 跑酷上下文，不能为 null
	 * @return 状态有效返回 true，任一检查失败返回 false
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		if (!meetsBaseConditions(player)) {
			return false;
		}
		StateData stateData = context.state();
		if (stateData.getTicksInState() >= ParkourConfig.wallRunDuration) {
			return false;
		}
		if (!WallRunCollision.wallCollisionIsValid(player, context)) {
			return false;
		}
		WallMovementData wallMovementData = context.wall();
		Direction movementDir = Direction.from3DDataValue(wallMovementData.getRunMoveRaw());
		return Directions.isFacing(player, movementDir, FACING_ANGLE_TOLERANCE);
	}

	@Override
	public Identifier animId(Player player) {
		int variant = ParkourContext.get(player).state().getAnimVariant();
		return variant == 0 ? ParkourPlayerAnimations.WALL_RUN_LEFT.id : ParkourPlayerAnimations.WALL_RUN_RIGHT.id;
	}

	@Override
	public List<AbstractModifier> getAnimationModifiers(Player player, int variant) {
		boolean isWallOnLeft = (variant == 0);
		return List.of(new WallRunPlayerAnimModifier(player, isWallOnLeft));
	}
}
