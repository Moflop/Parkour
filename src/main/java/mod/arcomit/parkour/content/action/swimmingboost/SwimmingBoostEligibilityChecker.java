package mod.arcomit.parkour.content.action.swimmingboost;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.SwimMovementData;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.player.Player;

/**
 * 游泳加速准入条件检查——所有条件缺一不可：
 * <ol>
 *   <li>配置项 {@code enableSwimmingBoost} 已启用</li>
 *   <li>加速冷却已结束（cooldown <= 0）</li>
 *   <li>玩家处于游泳状态</li>
 *   <li>通过通用动作检查（{@code canPerformAction}）</li>
 * </ol>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SwimmingBoostEligibilityChecker {
	/**
	 * 检查所有准入条件是否同时满足。
	 *
	 * @param player   待检查的玩家，不可为null
	 * @param swimData 游泳数据容器，不可为null；从中读取冷却时间
	 * @return true表示可执行加速，任一条件不满足返回false
	 */
	public static boolean check(Player player, SwimMovementData swimData) {
		return ParkourConfig.enableSwimmingBoost && swimData.getBoostCooldown() <= 0 && player.isSwimming() && ParkourChecks.canPerformAction(
				player);
	}
}
