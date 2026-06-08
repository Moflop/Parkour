package mod.arcomit.parkour.content.behavior.speedvault;

import mod.arcomit.parkour.utils.Obstacles;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 负责 Speed Vault 状态下的碰撞检测与环境合法性校验。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SpeedVaultCollision {
	/** 可速过障碍物的最小高度（单位：格） */
	private static final double MIN_OBSTACLE_HEIGHT = 1.1;
	/** 可速过障碍物的最大高度（单位：格） */
	private static final float MAX_OBSTACLE_HEIGHT = 1.6F;

	/**
	 * 检查玩家前方是否存在可速过的障碍物。
	 *
	 * <p>条件：障碍物高度在 ({@value #MIN_OBSTACLE_HEIGHT}, {@value #MAX_OBSTACLE_HEIGHT}] 格之间，
	 * 且障碍物顶部上方有足够的玩家通行空间。
	 *
	 * @param player 目标玩家，不能为 null
	 * @param facing 玩家面朝的水平方向，不能为 null
	 * @return 障碍物满足高度范围和头顶空间条件时返回 true
	 */
	public static boolean hasValidVaultPoint(Player player, Direction facing) {
		double obstacleHeight = Obstacles.findHeight(player, facing);
		if (obstacleHeight <= MIN_OBSTACLE_HEIGHT || obstacleHeight > MAX_OBSTACLE_HEIGHT) {
			return false;
		}

		if (!Obstacles.hasSpaceAbove(player, facing, obstacleHeight)) {
			return false;
		}

		return true;
	}
}
