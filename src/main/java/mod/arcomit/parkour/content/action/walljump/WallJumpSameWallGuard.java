package mod.arcomit.parkour.content.action.walljump;

import mod.arcomit.parkour.content.context.JumpData;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 同墙防连跳守卫——阻止玩家在同一面墙上连续蹬跳，防止原地无限刷跳跃。
 *
 * <p>核心逻辑：玩家只有在只接触单一墙面时才会记录最近跳跃墙面，
 * 接触多面墙时不记录，允许在各面墙之间自由跳跃。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpSameWallGuard {

	/**
	 * 判断本次蹬跳墙面是否与同类型的上一次蹬跳墙面相同。
	 *
	 * @param data 跳跃数据容器，不可为null；NONE类型不会调用此方法
	 * @param type 蹬墙跳类型，取值 UP/PARALLEL/VIEW 之一
	 * @param wallDir 本次弹跳的墙面方向，不可为null
	 * @return true表示同一面墙，应阻止跳跃；false表示允许跳跃
	 */
	static boolean isSameWallAsLastJump(JumpData data, WallJumpType type, Direction wallDir) {
		Direction lastDir = switch (type) {
			case UP -> data.getLastUpJump();
			case PARALLEL -> data.getLastParallelJump();
			case VIEW -> data.getLastViewJump();
			default -> null; // NONE 不可能到达这里
		};
		return wallDir == lastDir;
	}

	/**
	 * 判断玩家是否只贴着一面墙。
	 *
	 * @param player 待检测的玩家，不可为null
	 * @return true表示只接触单面墙，此时需记录跳跃；多面墙时无需记录
	 */
	static boolean isAgainstSingleWall(Player player) {
		return WallJumpCollisionFinder.findCollisionDirs(player).size() == 1;
	}

	/**
	 * 将本次蹬墙跳的墙面方向记录到跳跃数据中，供下次同类型跳跃查重。
	 *
	 * <p>VIEW类型会同时更新VIEW和PARALLEL两条记录，因为向前跳与侧跳共享
	 * 同向判定逻辑。</p>
	 *
	 * @param data 跳跃数据容器，不可为null
	 * @param type 蹬墙跳类型
	 * @param wallDir 本次的墙面方向，不可为null
	 * @sideeffect 写入 JumpData 的相应字段
	 */
	static void recordLastJumpWall(JumpData data, WallJumpType type, Direction wallDir) {
		switch (type) {
			case UP -> data.setLastUpJump(wallDir);
			case PARALLEL -> data.setLastParallelJump(wallDir);
			case VIEW -> {
				data.setLastViewJump(wallDir);
				data.setLastParallelJump(wallDir); // VIEW 同时更新 PARALLEL 记录
			}
		}
	}
}
