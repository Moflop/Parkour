package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.behavior.armhang.client.ClientArmhangMovement;
import mod.arcomit.parkour.content.behavior.armhang.server.ServerArmhangSound;
import mod.arcomit.parkour.content.context.InputData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.content.init.ParkourSounds;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 手臂悬挂状态。
 * <p>
 * 玩家向前走到方块边缘时自动抓住边缘悬挂。悬挂时可以左右平移，按下跳跃键翻上平台， 按下潜行键主动退出。退出后有短暂冷却防止立即重新挂上同一位置。
 * <p>
 * 该状态在逻辑端与服务端同时运行（模拟端），客户端额外负责移动输入和音效表现。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ArmhangState extends AbstractParkourState {
	/** 退出悬挂后的冷却时长（单位：刻），防止退出后立即重新进入 */
	private static final int EXIT_COOLDOWN_TICKS = 5;

	/**
	 * 注册状态转换规则：本状态下按下潜行键（Shift）即退回到默认状态。
	 */
	public ArmhangState() {
		registerTransitions(IParkourStateTransition.onLocalTick(ParkourStates.DEFAULT::get,
				(player, context) -> {
					if (ParkourProxies.INPUT_PROXY.getShiftKeyDown(player)) {
						return true;
					}
					return false;
				}));
	}

	/**
	 * 检查玩家是否满足手臂悬挂的所有基础前置条件。
	 *
	 * @return true 表示可以继续判断悬挂点碰撞，false 则直接拒绝进入或维持状态
	 */
	private static boolean meetsBaseConditions(Player player) {
		if (!ParkourConfig.enableArmhang || player.onClimbable() || player.isInWater() || player.isInLava() || !ParkourChecks.canPerformBehavior(
				player)) {
			return false;
		}
		return true;
	}

	/**
	 * 进入手臂悬挂状态时，记录当前面向方向作为悬挂方向。
	 */
	@Override
	public void onEnter(Player player, ParkourContext context) {
		super.onEnter(player, context);
		WallMovementData wallMovementData = context.wall();
		wallMovementData.setArmhang(player.getDirection());
	}

	/**
	 * 进入状态的客户端表现：播放悬挂音效，并向服务端同步玩家位置。
	 */
	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ParkourProxies.SOUND_PROXY.playEntityBoundSound(ParkourSounds.ARMHANG.get(),
				SoundSource.PLAYERS, 1.0F, 1.0F, player,
				player.getRandom().nextLong());
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
	}

	/**
	 * 进入状态时设置玩家起始坐标用于后续垂挂移动音效播放。
	 */
	@Override
	public void onServerEnter(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();
		wallMovementData.setArmhangLastPos(player.position());
		wallMovementData.setArmhangMoveDist(0f);
	}

	/**
	 * 进入状态时立即应用一次悬浮与吸附物理，消除进入前的垂直速度， 避免玩家因残余动量在下一 tick 满足退出条件而闪退。并并根据配置决定是否重置爬墙
	 */
	@Override
	public void onSimulationEnter(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();
		ArmhangPhysics.applyLevitateAndAdhesion(player, wallMovementData);

		if (ParkourConfig.armhangResetWallClimb) {
			wallMovementData.resetClimb();
		}

		context.state().setStateInvalidTicks(0);
	}

	/**
	 * 每 tick 执行悬挂物理（悬浮+吸附）和墙角方向旋转检测。
	 */
	@Override
	public void onSimulationTick(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();
		ArmhangPhysics.applyLevitateAndAdhesion(player, wallMovementData);
		ArmhangRotation.tryInsideCornerRotation(player, wallMovementData);
	}

	/**
	 * 客户端每 tick 执行悬挂平移和移动音效。
	 */
	@Override
	public void onClientTick(Player player, ParkourContext context) {
		InputData inputData = context.input();
		WallMovementData wallMovementData = context.wall();
		if (player.isLocalPlayer()) {
			ClientArmhangMovement.applyArmhangMovement(player, inputData,
					wallMovementData);
		}
	}

	@Override
	public void onServerTick(Player player, ParkourContext context) {
		ServerArmhangSound.playMovementSound(player, context.wall());
	}

	/**
	 * 退出悬挂时设置 {@value #EXIT_COOLDOWN_TICKS} tick（0.25 秒）冷却，防止主动退出后立刻被判定重新进入。
	 */
	@Override
	public void onSimulationExit(Player player, ParkourContext context) {
		WallMovementData wallMovementData = context.wall();
		wallMovementData.setArmhangCooldown(EXIT_COOLDOWN_TICKS);
	}

	/**
	 * 判断是否允许进入手臂悬挂状态。
	 * <p>
	 * 准入条件：功能开启且不在攀爬/水中/熔岩中；不在冷却期内； 当前摔落高度危险（或虽安全但离地足够远）；玩家前方存在有效的悬挂碰撞点。
	 *
	 * @return true 允许进入，false 拒绝
	 */
	@Override
	public boolean canEnter(Player player, ParkourContext context) {
		if (!meetsBaseConditions(player)) {
			return false;
		}
		WallMovementData wallMovementData = context.wall();
		if (wallMovementData.getArmhangCooldown() > 0) {
			return false;
		}
		if (!ParkourChecks.isFallUnsafe(player) && ArmhangCollision.isTooCloseToGround(
				player)) {
			return false;
		}
		if (!ArmhangCollision.hasValidHangPoint(player, player.getDirection())) {
			return false;
		}
		return true;
	}

	/**
	 * 判断手臂悬挂状态是否仍然有效（每 tick 被状态机调用以决定是否退出）。
	 * <p>
	 * 需要基础条件满足、悬挂方向未丢失、且该方向的悬挂点仍然可用。
	 *
	 * @return true 保持悬挂，false 退出状态
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		if (!meetsBaseConditions(player)) {
			return false;
		}
		WallMovementData wallMovementData = context.wall();
		Direction armhangDir = wallMovementData.getArmhang();
		if (armhangDir == null) {
			return false;
		}

		// 墙壁检测可能因网络位置不同步偶尔失效，允许短暂的无效状态（宽容期）以提升体验稳定性。超过宽容期则退出。
		boolean hasValidPoint = ArmhangCollision.hasValidHangPoint(player, armhangDir);
		StateData state = context.state();
		if (!hasValidPoint) {
			if (player.level().isClientSide()) {
				return false;
			}

			int currentInvalidTicks = state.getStateInvalidTicks() + 1;
			state.setStateInvalidTicks(currentInvalidTicks);

			// 4 Ticks (0.2秒)
			if (currentInvalidTicks > 4) {
				return false;
			}

			// 在服务端宽限期内，强行返回 true 维持状态
			return true;
		} else {
			// 只要在任意一个 tick 再次检测到了墙壁，立刻清零失效计数器（刷新宽限期）
			if (!player.level().isClientSide() && state.getStateInvalidTicks() > 0) {
				state.setStateInvalidTicks(0);
			}
			return true;
		}

	}
}
