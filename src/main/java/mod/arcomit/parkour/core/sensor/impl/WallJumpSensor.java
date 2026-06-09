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
 * 蹬墙跳碰撞传感器，检测玩家脚部高度前方是否存在可蹬踏的墙面。
 * <p>
 * 沿指定水平方向生成一个位于脚部高度（0%~30% 身高）的 AABB 检测盒， 仅需该区域与方块碰撞即判定有效。
 * </p>
 * <p>
 * 与 {@link HeadFeetSensor} 不同，本传感器只关心脚部是否有墙面——蹬墙跳动作 可以沿一面墙反复蹬跳，不需要头部也接触墙面。忽略的方块类型由标签
 * {@code SCAFFOLDING_BLOCKS} 定义（脚手架等非实体方块）。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpSensor {
	private static final double BOX_MIN_HEIGHT_RATIO = 0.0;
	private static final double BOX_MAX_HEIGHT_RATIO = 0.3;
	private static final double COLLISION_CHECK_DISTANCE = 0.2;
	private static final TagKey<Block> IGNORED_BLOCKS_TAG =
			ParkourTags.Blocks.SCAFFOLDING_BLOCKS;

	/**
	 * 获取指定方向经缓存更新的蹬墙跳碰撞检测盒列表。
	 *
	 * @param player    目标玩家，不能为 null
	 * @param direction 水平方向，不能为 null
	 * @return 碰撞盒列表（仅含一个脚部高度检测盒）；缓存命中时不重新计算
	 */
	public static List<AABB> getBoxes(Player player, Direction direction) {
		SensorData data = SensorDataManager.get(player).getData(SensorType.WALL_JUMP);
		CollisionCache collisionCache = data.getCollisionCache(direction);
		updateCacheIfNeeded(collisionCache, player, direction);

		return collisionCache.getCollisionBoxes();
	}

	/**
	 * 判断玩家在指定方向脚部高度前方是否存在可蹬踏的墙面。
	 *
	 * @param player    目标玩家，不能为 null
	 * @param direction 水平方向，不能为 null
	 * @return true 表示该方向有墙面可以蹬跳
	 */
	public static boolean isValidCollision(Player player, Direction direction) {
		SensorData data = SensorDataManager.get(player).getData(SensorType.WALL_JUMP);
		CollisionCache collisionCache = data.getCollisionCache(direction);
		updateCacheIfNeeded(collisionCache, player, direction);

		return collisionCache.isCollided();
	}

	/**
	 * 按 tick+位置双维度检查缓存有效性，失效时重新计算脚部碰撞并回写。
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
			boxes.add(BlockCollisions.buildBox(player, direction, BOX_MIN_HEIGHT_RATIO,
					BOX_MAX_HEIGHT_RATIO, COLLISION_CHECK_DISTANCE));
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
