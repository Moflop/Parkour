package mod.arcomit.parkour.content.behavior.speedvault;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.behavior.speedvault.client.ClientSpeedVaultEffects;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import mod.arcomit.parkour.utils.Obstacles;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Speed Vault 状态 —— 玩家用手撑住障碍物快速翻越。
 *
 * <p>进入条件：玩家面向的障碍物高度在 1.1~1.6 格之间，且上方有足够空间。
 * 进入后持续 2 tick 即自动退出，退出时给予短暂的速度提升效果。 按住潜行键可提前退回到默认状态。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SpeedVaultState extends AbstractParkourState {
	/** 进入速过状态的最大允许下落速度（单位：方/秒），超过此值不允许进入 */
	private static final double MAX_ENTER_FALL_SPEED = -0.1;

	/**
	 * 构造并注册状态转换：每客户端 tick 检测潜行键，按下即切回默认状态。
	 */
	public SpeedVaultState() {
		registerTransitions(IParkourStateTransition.onLocalTick(ParkourStates.DEFAULT::get,
				(player, context) -> ParkourProxies.INPUT_PROXY.getShiftKeyDown(
						player)));
	}

	/**
	 * 检查玩家的基础环境是否允许进入速过状态。
	 *
	 * @param player 目标玩家，不能为 null
	 * @return 所有基础条件满足返回 true，以下任一情况返回 false： 配置禁用、玩家在攀爬梯子/藤蔓、潜行中、在水中或熔岩中、或不可执行跑酷行为
	 */
	private static boolean meetsBaseConditions(Player player) {
		if (!ParkourConfig.enableSpeedVault || player.onClimbable() || player.isCrouching() || player.isInWater() || player.isInLava() || !ParkourChecks.canPerformBehavior(
				player)) {
			return false;
		}
		return true;
	}

	/**
	 * 客户端进入速过状态时触发：播放翻越音效并随机选择左手/右手动画， 同时将当前坐标发送给服务端以校准位置。
	 */
	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ClientSpeedVaultEffects.playSoundAndAnim(player);
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
	}

	/**
	 * 每模拟 tick 根据障碍物高度计算垂直速度、根据 WASD 输入计算水平速度， 直接设置玩家速度实现翻越位移。
	 */
	@Override
	public void onSimulationTick(Player player, ParkourContext context) {
		SpeedVaultPhysics.applyVaultMovement(player, context.wall());
	}

	/**
	 * 退出速过状态时，根据玩家当前移动输入赋予一个向前的水平初速度， 垂直速度交由重力接管。
	 */
	@Override
	public void onSimulationExit(Player player, ParkourContext context) {
		SpeedVaultPhysics.applyExitVelocity(player);
	}

	/**
	 * 服务端退出速过状态时，给予玩家 2 秒的速度 I 效果作为翻越成功的奖励。
	 */
	@Override
	public void onServerExit(Player player, ParkourContext context) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
				ParkourConstants.EXIT_SPEED_BOOST_DURATION,
				ParkourConstants.EXIT_SPEED_BOOST_AMPLIFIER));
	}

	/**
	 * 判断玩家是否满足速过障碍物的条件。
	 *
	 * <p>需要同时满足：基础环境条件、面朝方向有合适高度的障碍物、
	 * 障碍物上方有通行空间、玩家在地面或非快速下落状态。 通过后会将障碍物高度记录到 {@code context.wall()} 中供物理计算使用。
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

		Direction facing = player.getDirection();

		if (!SpeedVaultCollision.hasValidVaultPoint(player, facing)) {
			return false;
		}

		double verticalVel = player.getDeltaMovement().y();
		if (!player.onGround() && verticalVel < MAX_ENTER_FALL_SPEED) {
			return false;
		}

		// 记录数据
		double obstacleHeight = Obstacles.findHeight(player, facing);
		context.wall().setObstaclesHeight(obstacleHeight);
		return true;
	}

	/**
	 * 判断速过状态是否仍然有效。
	 *
	 * <p>除基础条件外，还限制最大持续 tick 数（{@value SpeedVaultPhysics#MAX_VAULT_TICK}），
	 * 超时自动退出。
	 *
	 * @param player  目标玩家，不能为 null
	 * @param context 跑酷上下文，不能为 null
	 * @return 状态仍然有效返回 true，超时或基础条件不满足返回 false
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		if (!meetsBaseConditions(player)) {
			return false;
		}

		StateData stateData = context.state();
		if (stateData.getTicksInState() > SpeedVaultPhysics.MAX_VAULT_TICK) {
			return false;
		}

		return true;
	}
}
