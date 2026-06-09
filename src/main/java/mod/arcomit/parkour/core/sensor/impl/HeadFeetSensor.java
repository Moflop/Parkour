package mod.arcomit.parkour.core.sensor.impl;

import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.core.sensor.CollisionCache;
import mod.arcomit.parkour.core.sensor.SensorData;
import mod.arcomit.parkour.core.sensor.SensorDataManager;
import mod.arcomit.parkour.core.sensor.SensorType;
import mod.arcomit.parkour.utils.BlockCollisions;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 头部与脚部碰撞传感器，用于滑墙、跑墙和爬墙动作的环境判定。
 * <p>
 * 沿指定水平方向生成两个 AABB 检测盒：一个位于玩家头部高度（85%~100% 身高）， 一个位于脚部高度（0%~30% 身高）。只有当<b>两个检测盒同时</b>与方块碰撞时，
 * 才认为该方向满足贴墙条件。
 * </p>
 * <p>
 * 头部+脚部同时碰撞保证了玩家前方是一面完整墙面而非半高的台阶或栅栏。 忽略的方块类型由标签 {@code COMMON_IGNORED_BLOCKS} 定义。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class HeadFeetSensor {
	private static final double FEET_BOX_MIN_HEIGHT_RATIO = 0.0;
	private static final double FEET_BOX_MAX_HEIGHT_RATIO = 0.3;
	private static final double HEAD_BOX_MIN_HEIGHT_RATIO = 0.85;
	private static final double HEAD_BOX_MAX_HEIGHT_RATIO = 1.0;
	private static final double COLLISION_CHECK_DISTANCE = 0.15;
	private static final TagKey<Block> IGNORED_BLOCKS_TAG =
			ParkourTags.Blocks.COMMON_IGNORED_BLOCKS;

	/**
	 * 获取指定方向经缓存更新的头部+脚部碰撞检测盒列表。
	 *
	 * @param player    目标玩家，不能为 null
	 * @param direction 水平方向，不能为 null
	 * @return 碰撞盒列表（索引0=头部盒，索引1=脚部盒）；缓存命中时不重新计算
	 */
	public static List<AABB> getBoxes(Player player, Direction direction) {
		SensorData data = SensorDataManager.get(player).getData(SensorType.HEAD_FEET);
		CollisionCache collisionCache = data.getCollisionCache(direction);
		updateCacheIfNeeded(collisionCache, player, direction);

		return collisionCache.getCollisionBoxes();
	}

	/**
	 * 判断玩家在指定方向是否同时被头部和脚部碰撞，即前方为完整墙面。
	 *
	 * @param player    目标玩家，不能为 null
	 * @param direction 水平方向，不能为 null
	 * @return true 表示头部和脚部同时检测到方块碰撞
	 */
	public static boolean isValidCollision(Player player, Direction direction) {
		SensorData data = SensorDataManager.get(player).getData(SensorType.HEAD_FEET);
		CollisionCache collisionCache = data.getCollisionCache(direction);
		updateCacheIfNeeded(collisionCache, player, direction);

		return collisionCache.isCollided();
	}

	/**
	 * 按 tick+位置双维度检查缓存有效性，失效时重新计算头部和脚部碰撞并回写。
	 * <p>副作用：修改 cache 的 tick、position、collisionBoxes 和 collided 字段。</p>
	 *
	 * @param collisionCache 该方向对应的碰撞缓存，不能为 null
	 * @param player         目标玩家，不能为 null
	 * @param direction      水平方向，不能为 null
	 */
	public static void updateCacheIfNeeded(CollisionCache collisionCache, Player player,
			Direction direction) {
		long currentTick = player.tickCount;
		Vec3 currentPos = player.position();

		boolean tickInvalid = !collisionCache.isValidTick(currentTick);
		boolean positionInvalid = !collisionCache.isValidPosition(currentPos);

		if (!tickInvalid && !positionInvalid) {
			return; // 缓存有效，无需重算
		}

		collisionCache.setTick(currentTick);

		if (positionInvalid) {
			collisionCache.setPosition(currentPos);

			List<AABB> boxes = new ArrayList<>();
			boxes.add(BlockCollisions.buildBox(player, direction,
					HEAD_BOX_MIN_HEIGHT_RATIO, HEAD_BOX_MAX_HEIGHT_RATIO,
					COLLISION_CHECK_DISTANCE));
			boxes.add(BlockCollisions.buildBox(player, direction,
					FEET_BOX_MIN_HEIGHT_RATIO, FEET_BOX_MAX_HEIGHT_RATIO,
					COLLISION_CHECK_DISTANCE));
			collisionCache.setCollisionBoxes(boxes);

			for (AABB box : boxes) {
				if (!BlockCollisions.hasBlockCollision(player.level(), box,
						IGNORED_BLOCKS_TAG)) {
					collisionCache.setCollided(false);
					return;
				}
			}
			collisionCache.setCollided(true);
			return;
		}

		List<AABB> boxes = collisionCache.getCollisionBoxes();
		for (AABB box : boxes) {
			if (!BlockCollisions.hasBlockCollision(player.level(), box,
					IGNORED_BLOCKS_TAG)) {
				collisionCache.setCollided(false);
				return;
			}
		}
		collisionCache.setCollided(true);
	}
}
