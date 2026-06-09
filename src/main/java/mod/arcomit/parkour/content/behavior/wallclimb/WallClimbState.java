package mod.arcomit.parkour.content.behavior.wallclimb;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.behavior.wallclimb.server.ServerWallClimbSound;
import mod.arcomit.parkour.content.context.JumpData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 爬墙状态 —— 玩家面向墙壁按住跳跃键向上攀爬。
 *
 * <p>进入条件：玩家冲刺中、面朝墙壁、头部和脚部均接触墙体、未处于危险下落状态。
 * 持续期间服务器每 tick 播放对应墙面材质的脚步声。 松开跳跃键或停止前进输入即退出，退出时速度归零以避免手感卡顿。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallClimbState extends AbstractParkourState {
	/** 最近一次跳跃后允许进入爬墙的最大 tick 数 */
	public static final int MAX_TICKS_SINCE_JUMP = 15;

	/**
	 * 构造并注册状态转换：每客户端 tick 检测前进输入是否归零或跳跃键是否松开， 满足任一条件则切回默认状态。
	 */
	public WallClimbState() {
		registerTransitions(IParkourStateTransition.onLocalTick(ParkourStates.DEFAULT::get,
				(player, context) -> ParkourProxies.INPUT_PROXY.getForwardImpulse(
						player) <= ParkourConstants.ZERO_THRESHOLD || !ParkourProxies.INPUT_PROXY.getJumping(
						player)));
	}

	/**
	 * 检查玩家当前环境是否满足爬墙的基础要求。
	 *
	 * @param player 目标玩家，不能为 null
	 * @return 配置启用、冲刺中、不在地面、不在可攀爬方块、不在水/熔岩中、 可执行跑酷行为、且存在有效墙壁碰撞点时返回 true
	 */
	public static boolean meetsBaseConditions(Player player) {
		// 快速失败：基础状态不满足墙跑条件
		if (!ParkourConfig.enableWallClimb || !player.isSprinting() || player.onGround() || player.onClimbable() || player.isInWater() || player.isInLava() || !ParkourChecks.canPerformBehavior(
				player)) {
			return false;
		}

		if (!WallClimbCollision.hasValidClimbPoint(player)) {
			return false;
		}
		return true;
	}

	/**
	 * 进入爬墙状态时记录当前面朝方向作为攀爬方向，同时重置上跳方向记录， 使玩家在爬墙后可以向反方向再次跳跃（爬墙刷新跳跃次数）。
	 */
	@Override
	public void onEnter(Player player, ParkourContext context) {
		super.onEnter(player, context);
		WallMovementData wallMovementData = context.wall();
		JumpData jumpData = context.jump();

		wallMovementData.setClimb(player.getDirection());
		jumpData.resetLastUpJump();
	}

	/**
	 * 客户端进入爬墙时向服务端同步当前坐标，校准位置以防客户端漂移。
	 */
	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
	}

	/**
	 * 每模拟 tick 施加垂直爬升速度和墙面吸附力，同时重置跌落高度防止落地受伤。
	 */
	@Override
	public void onSimulationTick(Player player, ParkourContext context) {
		WallClimbPhysics.applyClimbAndAdhesion(player);
	}

	/**
	 * 服务端每 tick 根据玩家移动距离播放对应墙面材质的脚步声。
	 */
	@Override
	public void onServerTick(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();
		ServerWallClimbSound.playFootstepSound(player, wallMovementData);
	}

	/**
	 * 退出爬墙时将速度归零，避免残留速度干扰后续操作（如墙跳）。
	 */
	@Override
	public void onSimulationExit(Player player, ParkourContext context) {
		player.setDeltaMovement(Vec3.ZERO);
	}

	/**
	 * @return 爬墙期间始终保持站立姿态
	 */
	@Override
	public Pose getLinkedPose() {
		return Pose.STANDING;
	}

	/**
	 * 判断玩家是否可以从当前状态进入爬墙。
	 *
	 * <p>额外限制：当前面朝方向不得与上次爬墙方向相同（防止同面墙重复触发），
	 * 且玩家不得处于不安全的下落状态。
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
		Direction availableWallDir = player.getDirection();
		WallMovementData wallMovementData = context.wall();
		if (availableWallDir == wallMovementData.getClimb()) {
			return false;
		}
		return !ParkourChecks.isFallUnsafe(player);
	}

	/**
	 * 判断爬墙状态是否仍然有效。
	 *
	 * <p>除基础条件外，受 {@code wallClimbDuration} 配置项限制最大持续时间，
	 * 超时自动退出。
	 *
	 * @param player  目标玩家，不能为 null
	 * @param context 跑酷上下文，不能为 null
	 * @return 状态有效返回 true，超时或不满足基础条件返回 false
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		if (!meetsBaseConditions(player)) {
			return false;
		}
		StateData stateData = context.state();
		return stateData.getTicksInState() < ParkourConfig.wallClimbDuration;
	}
}
