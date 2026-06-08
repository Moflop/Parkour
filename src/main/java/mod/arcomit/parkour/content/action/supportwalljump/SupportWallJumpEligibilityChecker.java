package mod.arcomit.parkour.content.action.supportwalljump;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.world.entity.player.Player;

/**
 * 支撑蹬墙跳准入条件检查——仅在垂挂状态下允许触发。
 *
 * <p>前置条件：
 * <ol>
 *   <li>配置项 {@code enableSupportWallJump} 已启用</li>
 *   <li>玩家当前处于垂挂（ArmhangState）状态</li>
 * </ol>
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SupportWallJumpEligibilityChecker {

	/**
	 * 检查玩家是否满足支撑蹬墙跳的触发条件。
	 *
	 * @param player 待检查的玩家，不可为null
	 * @param currentState 玩家当前的行为状态，不可为null
	 * @return true表示允许支撑蹬墙跳，false表示功能未启用或不在垂挂状态
	 */
	public static boolean check(Player player, IParkourState currentState) {
		if (!ParkourConfig.enableSupportWallJump) {
			return false;
		}
		if (!(currentState instanceof ArmhangState)) {
			return false;
		}

		return true;
	}
}
