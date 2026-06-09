package mod.arcomit.parkour.content.action.swimmingjump;

import mod.arcomit.parkour.content.mechanic.freestyle.FreestyleMechanic;
import net.minecraft.world.entity.player.Player;

/**
 * 游泳跳跃准入条件检查——玩家需同时满足：
 * <ol>
 *   <li>正在按跳跃键（{@code player.jumping == true}）</li>
 *   <li>通过自由泳机制检查（{@code FreestyleMechanic.canFreestyle}）</li>
 * </ol>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SwimmingJumpEligibilityChecker {

	/**
	 * 检查游泳跳跃的两个前置条件是否同时满足。
	 *
	 * @param player 待检查的玩家，不可为null
	 * @return true表示可执行游泳跳跃，任一条件不满足返回false
	 */
	public static boolean check(Player player) {
		return player.jumping && FreestyleMechanic.canFreestyle(player);
	}
}
