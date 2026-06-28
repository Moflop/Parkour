package mod.arcomit.parkour.content.behavior.slide;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.behavior.slide.client.ClientSlideLogic;
import mod.arcomit.parkour.content.behavior.slide.client.ClientSlideSound;
import mod.arcomit.parkour.content.client.init.ClientParkourPlayerAnimations;
import mod.arcomit.parkour.content.context.GroundMovementData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.init.ParkourAnimationIds;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 滑铲状态 —— 玩家在地面向前滑行通过低矮空间，持续{@value #SLIDE_DURATION}刻。
 * <p>
 * 触发方式：向前或静止时按下滑铲键（且冷却计时器已归零）。 会在随机0或1两种变体中选择一种播放对应动画。 退出条件：玩家松开移动键（forward impulse为0）或超过最大持续时间。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SlideState extends AbstractParkourState {
	/** 滑铲最大持续时间（单位：刻） */
	public static final int SLIDE_DURATION = 10;
	/** 滑铲动画变体总数（0和1两种） */
	private static final int ANIM_VARIANT_COUNT = 2;

	public SlideState() {
		registerTransitions(IParkourStateTransition.onLocalTick(ParkourStates.DEFAULT::get,
				(player, context) -> ParkourProxies.INPUT_PROXY.getBackward(
						player)));
	}

	/**
	 * 检查滑铲功能是否启用且玩家环境允许。
	 *
	 * @return true 如果功能已启用、玩家不在水/岩浆中、前方坠落安全
	 */
	public static boolean meetsBaseConditions(Player player) {
		return ParkourConfig.enableSlide && !ParkourChecks.isFallUnsafe(
				player) && !player.isInWater() && !player.isInLava() && ParkourChecks.canPerformBehavior(
				player);
	}

	/**
	 * 客户端播放滑铲音效，本地玩家额外施加物理推进并同步位置到服务端。
	 */
	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ClientSlideSound.play(player);
		if (player.isLocalPlayer()) {
			ClientSlideLogic.applyPhysicsAndSendPosition(player);
		}
	}

	/**
	 * 进入滑铲时设置冷却计时器，防止连续触发。
	 */
	@Override
	public void onSimulationEnter(Player player, ParkourContext context) {
		context.ground().setSlideCooldown(ParkourConfig.slideCooldown);
	}

	/**
	 * 滑铲期间碰撞箱缩小为0.6x0.6，视点降至0.4格，模拟压低姿态。
	 */
	@Override
	public EntityDimensions getCustomDimensions(Player player) {
		return EntityDimensions.fixed(ParkourConstants.CROUCH_HITBOX_SIZE,
						ParkourConstants.CROUCH_HITBOX_SIZE)
				.withEyeHeight(ParkourConstants.CROUCH_EYE_HEIGHT);
	}

	/**
	 * @return 随机返回0或1，用于选择滑铲动画的两种变体
	 */
	@Override
	public int generateVariant(Player player) {
		return ThreadLocalRandom.current().nextInt(ANIM_VARIANT_COUNT);
	}

	/**
	 * @return 使用站立姿态{@link Pose#STANDING}，由动画修改器接管骨骼
	 */
	@Override
	public Pose getLinkedPose() {
		return Pose.STANDING;
	}

	@Override
	public boolean canEnter(Player player, ParkourContext context) {
		GroundMovementData groundMovementData = context.ground();
		return meetsBaseConditions(player) && groundMovementData.getSlideCooldown() <= 0;
	}

	/**
	 * 状态持续的合法性：基础条件一直满足且停留时间未超过{@value #SLIDE_DURATION}刻。
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		StateData stateData = context.state();
		return meetsBaseConditions(player) && stateData.getTicksInState() < SLIDE_DURATION;
	}

	@Override
	public Identifier animId(Player player) {
		int variant = ParkourContext.get(player).state().getAnimVariant();
		return variant == 1 ? ParkourAnimationIds.SLIDE_2 : ParkourAnimationIds.SLIDE_1;
	}

}
