package mod.arcomit.parkour.content.action.walljump;

import mod.arcomit.parkour.core.sensor.impl.WallJumpSensor;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 壁面碰撞检测器——找出玩家当前接触的所有墙面方向。
 *
 * <p>扫描水平四方向，通过碰撞传感器判定哪些方向上玩家紧贴墙壁。
 * 同时提供找出最近一面墙的便捷方法。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpCollisionFinder {

	/**
	 * 扫描水平方向，返回所有与玩家发生有效碰撞的墙面方向。
	 *
	 * @param player 待检测的玩家，不可为null
	 * @return 所有碰撞方向的列表，无碰撞时返回空列表而非null
	 */
	static List<Direction> findCollisionDirs(Player player) {
		return Direction.Plane.HORIZONTAL.stream()
				.filter(dir -> WallJumpSensor.isValidCollision(player, dir))
				.collect(Collectors.toList());
	}

	/**
	 * 在所有碰撞墙面中找出玩家朝向最接近的那一面。
	 *
	 * @param player 待检测的玩家，不可为null
	 * @return 最接近玩家朝向的墙面方向；无任何碰撞时返回null
	 */
	static Direction findClosestCollisionDir(Player player) {
		List<Direction> collidedDirs = findCollisionDirs(player);
		return Directions.findClosest(player, collidedDirs);
	}


}
