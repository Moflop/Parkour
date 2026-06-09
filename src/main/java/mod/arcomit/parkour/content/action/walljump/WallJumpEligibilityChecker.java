package mod.arcomit.parkour.content.action.walljump;

import mod.arcomit.parkour.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.behavior.wallclimb.WallClimbState;
import mod.arcomit.parkour.content.behavior.wallrun.WallRunState;
import mod.arcomit.parkour.content.behavior.wallslide.WallSlideState;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.world.entity.player.Player;

/**
 * 蹬墙跳准入条件检查——判断玩家在当前状态下是否允许触发蹬墙跳。
 *
 * <p>准入规则按状态分类：
 * <ul>
 *   <li>跑墙/滑墙/垂挂状态：无条件允许（垂挂为预留）</li>
 *   <li>默认状态：仅当玩家在空中下落或正在攀爬可攀爬方块时允许</li>
 *   <li>爬墙状态：仅当玩家不在地面时允许</li>
 * </ul>
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpEligibilityChecker {

	/**
	 * 检查玩家在当前状态下是否满足蹬墙跳的触发条件。
	 *
	 * @param player       待检查的玩家，不可为null
	 * @param currentState 玩家当前的行为状态，不可为null
	 * @return true表示允许蹬墙跳，false表示当前不满足条件
	 */
	public static boolean check(Player player, IParkourState currentState) {
		if (currentState instanceof WallRunState || currentState instanceof WallSlideState || currentState instanceof ArmhangState) {
			return true; // TODO: 垂挂
		} else if (currentState instanceof DefaultState) {
			if (!player.onGround() && player.getDeltaMovement().y() < 0) {
				return true;
			} else if (player.onClimbable()) {
				return true;
			}
		} else if (currentState instanceof WallClimbState) {
			if (!player.onGround()) {
				return true;
			}
		}
		return false;
	}
}
