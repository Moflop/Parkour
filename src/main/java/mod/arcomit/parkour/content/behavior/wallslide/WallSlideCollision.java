package mod.arcomit.parkour.content.behavior.wallslide;

import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.core.sensor.impl.HeadFeetSensor;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/**
 * 滑墙碰撞检测 —— 查找距离玩家最近的有效墙体方向。
 *
 * <p>优先复用上次缓存的滑墙方向，避免频繁切换方向导致动画抖动；
 * 缓存无效时遍历四个水平方向，返回距离最近的有效碰撞方向。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallSlideCollision {

    /**
     * 查找当前最近的有效滑墙方向。
     *
     * <p>优先检查缓存方向是否仍有效（通过 {@link HeadFeetSensor#isValidCollision}），
     * 无效则遍历所有水平方向并返回距离玩家最近的一个。
     *
     * @param player            目标玩家，不能为 null
     * @param wallMovementData 墙体数据，携带缓存方向
     * @return 最近的有效墙体方向，无任何墙体时返回 null
     */
    public static Direction findAvailableWallDirection(Player player,
            WallMovementData wallMovementData) {
		Direction cachedDir = wallMovementData.getSlide();

		// 优先检测当前缓存的滑墙方向是否依然有效
		if (cachedDir != null && hasValidWallPoint(player, cachedDir)) {
			return cachedDir;
		}

		// 如果缓存无效或当前没有缓存方向，则遍历所有水平方向
		ArrayList<Direction> collisionDirections = new ArrayList<>(4);

		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (hasValidWallPoint(player, dir)) {
				collisionDirections.add(dir);
			}
		}

		return Directions.findClosest(player, collisionDirections);
	}

    /**
     * 检查玩家在指定方向是否存在有效墙体碰撞（头部和脚部均接触墙体）。
     *
     * @param player    目标玩家，不能为 null
     * @param direction 待检查的水平方向，不能为 null
     * @return 头部和脚部传感器同时检测到墙体碰撞时返回 true
     */
    public static boolean hasValidWallPoint(Player player, Direction direction) {
        return HeadFeetSensor.isValidCollision(player, direction);
    }
}
