package mod.arcomit.parkour.content.behavior.base;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.behavior.wallclimb.WallClimbState;
import mod.arcomit.parkour.content.behavior.wallrun.WallRunState;
import mod.arcomit.parkour.content.event.LivingJumpCancellableEvent;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.input.ParkourInputActions;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * 默认状态 —— 所有跑酷行为的入口和回退目标。
 * <p>
 * 玩家处于空闲/行走等常规移动时均在此状态内，它根据玩家输入和环境条件
 * 将控制权分发给滑铲、后撤步、爬行、壁跑、落地翻滚等专项行为状态。
 * <p>
 * 转移类型分为三类：按键触发（滑铲键 -> 根据移动方向分流到爬行/滑铲/后撤步）、
 * 每Tick轮询（壁跑/壁攀/壁滑/悬挂）和事件驱动（坠落 -> 落地翻滚，跳跃 -> 速越）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class DefaultState extends AbstractParkourState {

	public DefaultState() {
		registerTransitions(
				// 输入类转移
				IParkourStateTransition.onInput(ParkourStates.CRAWL::get,
						ParkourInputActions.SLIDE, this::canCrawl),
				IParkourStateTransition.onInput(ParkourStates.SLIDE::get,
						ParkourInputActions.SLIDE, this::canSlide),
				IParkourStateTransition.onInput(ParkourStates.BACKSTEP::get,
						ParkourInputActions.SLIDE, this::canBackstep),

				// Tick类转移 (注意：WallRun 和 WallClimb 作为高优先级判定放在前面)
				IParkourStateTransition.onLocalTick(ParkourStates.ARMHANG::get,
						this::canArmHang),
				IParkourStateTransition.onLocalTick(ParkourStates.WALL_RUN::get,
						this::canWallRun),
				IParkourStateTransition.onLocalTick(ParkourStates.WALL_CLIMB::get,
						this::canWallClimb),
				IParkourStateTransition.onLocalTick(ParkourStates.WALL_SLIDE::get,
						this::canWallSlide),

				// 事件类转移 (跳跃、坠落)
				buildLandingRollTransition(), buildSpeedVaultTransition());
	}

	// ==========================================
	// 状态转移条件判断 (Predicates)
	// ==========================================

	/**
	 * 爬行准入：玩家静止站立时按下滑铲键。
	 */
	private boolean canCrawl(Player player, ParkourContext context) {
		return !isMoving(player);
	}

	/**
	 * 滑铲准入：玩家有移动冲量且方向朝前或静止（非后退）。
	 */
	private boolean canSlide(Player player, ParkourContext context) {
		return isMoving(player) && getForwardImpulse(player) >= 0;
	}

	/**
	 * 后撤步准入：玩家在<span>地面上</span>后退（移动冲量为负）时按下滑铲键。
	 */
	private boolean canBackstep(Player player, ParkourContext context) {
		return isMoving(player) && getForwardImpulse(player) < 0 && player.onGround();
	}

	/**
	 * 手臂悬挂准入：刚完成跳跃、未按潜行键、且处于下落过程中。
	 */
	private boolean canArmHang(Player player, ParkourContext context) {
		return context.jump().isJumped() && !isShiftKeyDown(player) && isFalling(
				player);
	}

	/**
	 * 壁跑准入：跳跃后的有效窗口期内，有足够的前向冲量且处于下落中。
	 */
	private boolean canWallRun(Player player, ParkourContext context) {
		int ticksSinceJump = context.jump().getTicksSinceLastJump();
		return isJumping(
				player) && ticksSinceJump <= WallRunState.MAX_TICKS_SINCE_JUMP && getForwardImpulse(
				player) > ParkourConstants.ZERO_THRESHOLD && isFalling(player);
	}

	/**
	 * 壁攀准入：与壁跑条件相似，但额外要求未按潜行键。
	 */
	private boolean canWallClimb(Player player, ParkourContext context) {
		int ticksSinceJump = context.jump().getTicksSinceLastJump();
		return isJumping(
				player) && ticksSinceJump <= WallClimbState.MAX_TICKS_SINCE_JUMP && getForwardImpulse(
				player) > ParkourConstants.ZERO_THRESHOLD && !isShiftKeyDown(
				player) && isFalling(player);
	}

	/**
	 * 壁滑准入：跳跃后且处于下落中即可尝试。
	 */
	private boolean canWallSlide(Player player, ParkourContext context) {
		return isJumping(player) && isFalling(player);
	}

	// ==========================================
	// 复杂事件转移构建 (Factories)
	// ==========================================

	/**
	 * 构建落地翻滚的事件转移。
	 * <p>
	 * 副作用：取消原版坠落伤害事件并将伤害倍率置0，由翻滚状态自行处理减伤。
	 */
	private IParkourStateTransition buildLandingRollTransition() {
		return new IParkourStateTransition() {
			@Override
			public IParkourState getTargetState() {
				return ParkourStates.LANDING_ROLL.get();
			}

			@Override
			public boolean shouldTransitionOnFall(Player player, ParkourContext context,
					LivingFallEvent event) {
				event.setDamageMultiplier(0);
				event.setCanceled(true);
				return true;
			}
		};
	}

	/**
	 * 构建速越的事件转移。
	 * <p>
	 * 副作用：取消原版跳跃事件，由速越状态接管跳跃行为。
	 */
	private IParkourStateTransition buildSpeedVaultTransition() {
		return new IParkourStateTransition() {
			@Override
			public IParkourState getTargetState() {
				return ParkourStates.SPEED_VAULT.get();
			}

			@Override
			public boolean shouldTransitionOnJump(Player player, ParkourContext context,
					LivingJumpCancellableEvent event) {
				event.setCanceled(true);
				return true;
			}
		};
	}

	// ==========================================
	// 代理调用简写
	// ==========================================

	private boolean isMoving(Player player) {
		return ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.isMoving(player);
	}

	private float getForwardImpulse(Player player) {
		return ParkourProxies.INPUT_PROXY.getForwardImpulse(player);
	}

	private boolean isJumping(Player player) {
		return ParkourProxies.INPUT_PROXY.getJumping(player);
	}

	private boolean isShiftKeyDown(Player player) {
		return ParkourProxies.INPUT_PROXY.getShiftKeyDown(player);
	}

	private boolean isFalling(Player player) {
		return player.getDeltaMovement().y() < 0;
	}
}
