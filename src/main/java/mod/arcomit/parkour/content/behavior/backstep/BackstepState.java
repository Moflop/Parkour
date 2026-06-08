package mod.arcomit.parkour.content.behavior.backstep;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.behavior.backstep.client.ClientBackstepSound;
import mod.arcomit.parkour.content.behavior.backstep.client.ClientBackstepVelocity;
import mod.arcomit.parkour.content.behavior.slide.SlideLogic;
import mod.arcomit.parkour.content.context.GroundMovementData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.player.Player;

/**
 * 后撤步状态 —— 玩家快速后退闪避，持续{@value #BACKSTEP_DURATION}刻且期间无敌。
 * <p>
 * 触发方式：在地面上向后移动（S键）时按下滑铲键。
 * 进入后会设置滑铲冷却以防止立即再次触发，客户端播放音效并施加后退速度。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class BackstepState extends AbstractParkourState {

	/** 后撤步持续时间（单位：刻） */
	public static final int BACKSTEP_DURATION = 3;

	/**
	 * 检查环境与玩家状态是否满足后撤步的最低要求。
	 *
	 * @return true 如果滑铲功能已启用、玩家不在水/岩浆中、前方坠落距离在安全范围内
	 */
	private static boolean meetsBaseConditions(Player player) {
		if (!ParkourConfig.enableSlide || player.isInWater() || player.isInLava() || !ParkourChecks.canPerformBehavior(
				player)) {
			return false;
		}
		if (ParkourChecks.isFallUnsafe(player)) {
			return false;
		}
		return true;
	}

	/**
	 * 进入状态时设置滑铲冷却计时器，避免连续触发。
	 */
	@Override
	public void onEnter(Player player, ParkourContext context) {
		super.onEnter(player, context);
		SlideLogic.setCooldown(player, context);
	}

	/**
	 * 客户端进入时播放后撤步音效，本地玩家额外施加后退速度并同步位置给服务端。
	 */
	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ClientBackstepSound.playSound(player);
		if (player.isLocalPlayer()) {
			ClientBackstepVelocity.applyVelocityAndSendPosition(player);
		}
	}

	@Override
	public boolean canEnter(Player player, ParkourContext context) {
		GroundMovementData groundMovementData = context.ground();
		return meetsBaseConditions(player) && groundMovementData.getSlideCooldown() <= 0;
	}

	/**
	 * 状态持续的合法性：基础条件始终满足且已停留时间未超过{@value #BACKSTEP_DURATION}刻。
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		StateData stateData = context.state();
		return meetsBaseConditions(
				player) && stateData.getTicksInState() < BACKSTEP_DURATION;
	}
}
