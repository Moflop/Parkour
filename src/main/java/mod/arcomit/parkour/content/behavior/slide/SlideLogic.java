package mod.arcomit.parkour.content.behavior.slide;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.GroundMovementData;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.world.entity.player.Player;

/**
 * 滑铲共享逻辑 —— 提供后撤步和滑铲共用的冷却设置。
 * <p>
 * 滑铲和后撤步共享同一个冷却计时器，防止两者连续无缝切换。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SlideLogic {

	/**
	 * 设置滑铲冷却计时为配置值，优先级高于当前剩余冷却。
	 * <p>
	 * 副作用：直接修改玩家地面上下文中的冷却值。
	 */
	public static void setCooldown(Player player, ParkourContext context) {
		GroundMovementData groundMovementData = context.ground();
		groundMovementData.setSlideCooldown(ParkourConfig.slideCooldown);
	}
}
