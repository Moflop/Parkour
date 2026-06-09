package mod.arcomit.parkour.content.behavior.landingroll;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.behavior.landingroll.client.ClientLandingRollAnimation;
import mod.arcomit.parkour.content.behavior.landingroll.client.ClientLandingRollSound;
import mod.arcomit.parkour.content.context.GroundMovementData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * 落地翻滚状态 —— 从高处坠落时通过翻滚动作减少伤害，持续{@value #LANDING_ROLL_DURATION}刻。
 * <p>
 * 进入条件：必须在空中提前按下滑铲键激活翻滚窗口，落地时该窗口仍有剩余时间。 客户端播放翻滚动画和音效，服务端给玩家短暂的速度效果，碰撞箱缩小模拟蜷缩姿态。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class LandingRollState extends AbstractParkourState {
	/** 翻滚动画完整时长（单位：刻） */
	public static final int LANDING_ROLL_DURATION = 8;

	/**
	 * 检查落地翻滚功能是否启用且玩家环境允许。
	 *
	 * @return true 如果功能已启用且玩家不在水/岩浆中
	 */
	public static boolean meetsBaseConditions(Player player) {
		return ParkourConfig.enableLandingRoll && !player.isInWater() && !player.isInLava() && ParkourChecks.canPerformBehavior(
				player);
	}

	/**
	 * 进入翻滚状态时清空翻滚窗口计时，防止后续连续触发。
	 */
	@Override
	public void onEnter(Player player, ParkourContext context) {
		super.onEnter(player, context);
		context.ground().setLandingRollWindow(0);
	}

	/**
	 * 服务端给予玩家40刻（2秒）的速度I效果，增强翻滚后的移动手感。
	 */
	@Override
	public void onServerEnter(Player player, ParkourContext context) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
				ParkourConstants.EXIT_SPEED_BOOST_DURATION,
				ParkourConstants.EXIT_SPEED_BOOST_AMPLIFIER));
	}

	/**
	 * 客户端播放翻滚音效，本地玩家额外播放摄像机动画和角色骨骼动画。
	 */
	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ClientLandingRollSound.play(player);
		if (player.isLocalPlayer()) {
			ClientLandingRollAnimation.playCameraAndPlayerAnim(player);
		}
	}

	/**
	 * 翻滚期间碰撞箱缩小为{@value ParkourConstants#CROUCH_HITBOX_SIZE}x{@value
	 * ParkourConstants#CROUCH_HITBOX_SIZE}，
	 * 视点高度降至{@value ParkourConstants#CROUCH_EYE_HEIGHT}格，模拟蜷缩姿态。
	 */
	@Override
	public EntityDimensions getCustomDimensions(Player player) {
		return EntityDimensions.fixed(ParkourConstants.CROUCH_HITBOX_SIZE,
						ParkourConstants.CROUCH_HITBOX_SIZE)
				.withEyeHeight(ParkourConstants.CROUCH_EYE_HEIGHT);
	}

	/**
	 * @return 使用站立姿态{@link Pose#STANDING}，由动画修改器接管骨骼旋转
	 */
	@Override
	public Pose getLinkedPose() {
		return Pose.STANDING;
	}

	@Override
	public boolean canEnter(Player player, ParkourContext context) {
		GroundMovementData groundMovementData = context.ground();
		return meetsBaseConditions(player) && groundMovementData.getLandingRollWindow() > 0;
	}

	/**
	 * 状态持续的合法性：基础条件一直满足且停留时间未超过{@value #LANDING_ROLL_DURATION}刻。
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		StateData stateData = context.state();
		return meetsBaseConditions(
				player) && stateData.getTicksInState() < LANDING_ROLL_DURATION;
	}
}
