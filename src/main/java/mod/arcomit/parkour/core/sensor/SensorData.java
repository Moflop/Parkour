package mod.arcomit.parkour.core.sensor;

import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;

/**
 * 传感器数据载体，按水平方向（东西南北）分别持有独立的{@link CollisionCache}实例。
 * <p>
 * 每种传感器类型在{@link SensorDataManager}中对应一个 SensorData，每个方向独立缓存，
 * 避免不同面之间的碰撞检测相互干扰。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SensorData {
	private final Map<Direction, CollisionCache> directionCollisionCaches =
			new EnumMap<>(Direction.class);

	public SensorData() {
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			directionCollisionCaches.put(dir, new CollisionCache());
		}
	}

	/**
	 * 获取指定水平方向的碰撞缓存。
	 *
	 * @param direction 水平方向（东西南北），不能为 null；传入非水平方向将抛出异常
	 * @return 该方向对应的 CollisionCache，永不为 null
	 * @throws IllegalArgumentException 如果 direction 不是水平方向
	 */
	public CollisionCache getCollisionCache(Direction direction) {
		CollisionCache cache = directionCollisionCaches.get(direction);
		if (cache == null) {
			throw new IllegalArgumentException("Unsupported direction: " + direction);
		}
		return cache;
	}
}
