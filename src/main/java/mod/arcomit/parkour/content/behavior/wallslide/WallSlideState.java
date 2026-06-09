package mod.arcomit.parkour.content.behavior.wallslide;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * 墙滑状态 —— 玩家贴着墙壁减速下滑。
 *
 * <p>进入条件：不在地面、不在可攀爬方块、附近存在有效墙体。
 * 水平速度乘以 0.8、垂直速度乘以 0.7 逐步减速，同时吸附在墙面上。 每 tick 自动检测并切换最近的墙体方向，方向变化时通过 S2C 包同步到其他客户端。
 * 与跑墙/爬墙不同，滑墙不要求玩家冲刺。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallSlideState extends AbstractParkourState {

	/**
	 * 构造并注册状态转换：每客户端 tick 检测跳跃释放宽限期是否结束， 宽限期过后自动退回到默认状态。
	 */
	public WallSlideState() {
		registerTransitions(IParkourStateTransition.onLocalTick(ParkourStates.DEFAULT::get,
				(player, context) -> this.isGracePeriodOver(context)));
	}

	/**
	 * 检查玩家当前环境是否满足墙滑的基础要求。
	 *
	 * @param player  目标玩家，不能为 null
	 * @param context 跑酷上下文，用于缓存墙体方向查询
	 * @return 配置启用、不在地面、不在可攀爬方块、不在水/熔岩中、 可执行跑酷行为、且附近存在有效墙体时返回 true
	 */
	private static boolean meetsBaseConditions(Player player, ParkourContext context) {
		// 快速失败：基础状态不满足滑墙条件
		if (!ParkourConfig.enableWallSlide || player.onGround() || player.onClimbable() || player.isInWater() || player.isInLava() || !ParkourChecks.canPerformBehavior(
				player)) {
			return false;
		}

		// 检测附近是否有可以依附的墙
		WallMovementData wallMovementData = context.wall();
		if (WallSlideCollision.findAvailableWallDirection(player,
				wallMovementData) == null) {
			return false;
		}
		return true;
	}

	/**
	 * @return 跳跃释放宽限期剩余 tick 小于等于 0 时为 true
	 */
	private boolean isGracePeriodOver(ParkourContext context) {
		return context.jump().getJumpReleaseGraceTicks() <= 0;
	}

	/**
	 * 进入墙滑时立即探测并初始化滑墙方向，若方向变化则广播到其他客户端。
	 */
	@Override
	public void onEnter(Player player, ParkourContext context) {
		super.onEnter(player, context);
		WallMovementData wallMovementData = context.wall();
		WallSlideDirection.trySwitchCollisionDirection(player, wallMovementData);
	}

	/**
	 * 客户端进入时向服务端同步坐标。
	 */
	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
	}

	/**
	 * 模拟侧进入时立即施加一次减速和吸附，确保第一帧就有正确的物理表现。
	 */
	@Override
	public void onSimulationEnter(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();
		WallSlidePhysics.applySlowdownAndAdhesion(player, wallMovementData);
	}

	/**
	 * 每模拟 tick 先检测并更新滑墙方向（含跨端同步），再施加减速和吸附力。
	 */
	@Override
	public void onSimulationTick(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();

		WallSlideDirection.trySwitchCollisionDirection(player, wallMovementData);
		WallSlidePhysics.applySlowdownAndAdhesion(player, wallMovementData);
	}

	/**
	 * 退出墙滑时重置所有滑墙相关数据（方向、碰撞记录等）。
	 */
	@Override
	public void onExit(Player player, ParkourContext context) {
		super.onExit(player, context);
		WallMovementData wallMovementData = context.wall();
		wallMovementData.resetSlide();
	}

	/**
	 * @return 墙滑期间保持站立姿态
	 */
	@Override
	public Pose getLinkedPose() {
		return Pose.STANDING;
	}

	/**
	 * 进入条件与有效性条件相同，均由 {@link #meetsBaseConditions} 判定。
	 */
	@Override
	public boolean canEnter(Player player, ParkourContext context) {
		return meetsBaseConditions(player, context);
	}

	/**
	 * 只要基础条件满足即维持墙滑状态，无持续时间限制。
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		return meetsBaseConditions(player, context);
	}
}
