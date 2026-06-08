package mod.arcomit.parkour.content.behavior.crawl;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.input.ParkourInputActions;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * 爬行状态 —— 玩家降低身高以爬行姿态通过1格高的低矮空间。
 * <p>
 * 触发方式：静止站立时按下滑铲键。使用游泳姿态（SWIMMING）实现视觉上的
 * 匍匐效果，再次按下同一按键退出回到默认状态。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class CrawlState extends AbstractParkourState {

	public CrawlState() {
		registerTransitions(
				// 玩家按下取消键（滑铲键）时，退回默认状态
				IParkourStateTransition.onInput(ParkourStates.DEFAULT::get,
						ParkourInputActions.SLIDE));
	}

	/**
	 * 检查跑酷配置和玩家环境是否允许爬行。
	 *
	 * @return true 如果爬行功能已启用、玩家未在游泳/水中/岩浆中且前方坠落安全
	 */
	public static boolean meetsBaseConditions(Player player) {
		if (!ParkourConfig.enableCrawl || player.isSwimming() || !ParkourChecks.canPerformBehavior(
				player)) {
			return false;
		}
		if (ParkourChecks.isFallUnsafe(player)) {
			return false;
		}
		return true;
	}

	/**
	 * @return 始终返回游泳姿态{@link Pose#SWIMMING}，利用原版游泳机制实现匍匐视觉效果
	 */
	@Override
	public Pose getLinkedPose() {
		return Pose.SWIMMING;
	}

	@Override
	public boolean canEnter(Player player, ParkourContext context) {
		return meetsBaseConditions(player);
	}

	/**
	 * 状态维持：只要基础环境仍然满足即可一直保持爬行。
	 */
	@Override
	public boolean isValid(Player player, ParkourContext context) {
		return meetsBaseConditions(player);
	}
}
