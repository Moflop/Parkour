package mod.arcomit.parkour.core.sensor.impl;

import mod.arcomit.parkour.ParkourConstants;
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
 * 手臂悬挂（头顶高度）碰撞传感器。
 * <p>
 * 在玩家身高高度（{@link Player#getBbHeight()}）沿指定水平方向生成两个 AABB 检测盒： 一个<b>底部盒</b>（从身高高度向下延伸约
 * {@value ParkourConstants#ARMHANG_GRIP_HEIGHT_RATIO} 身高）用于确认下方有方块支撑，
 * 一个<b>顶部盒</b>（从身高高度向上延伸至头顶上方）用于确认上方通畅无方块阻挡。 只有底部有碰撞且顶部无碰撞时，才判定为满足悬挂条件。
 * </p>
 * <p>
 * 与 {@link ArmhangEyeSensor} 的区别：本传感器以<b>头顶高度</b>为基准检测， 适用于判断玩家头顶上方的横梁是否可抓握。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ArmhangTopSensor {
	private static final TagKey<Block> IGNORED_BLOCKS_TAG =
			ParkourTags.Blocks.SCAFFOLDING_BLOCKS;

	/**
	 * 获取指定方向经缓存更新的碰撞检测盒列表。
	 *
	 * @param player    目标玩家，不能为 null
	 * @param direction 水平方向，不能为 null
	 * @return 碰撞盒列表（索引0为底部支撑盒，索引1为顶部通畅盒）；缓存命中时返回已有列表，不触发重新计算
	 */
	public static List<AABB> getBoxes(Player player, Direction direction) {
		SensorData data = SensorDataManager.get(player).getData(SensorType.ARMHANG_TOP);
		CollisionCache collisionCache = data.getCollisionCache(direction);
		updateCacheIfNeeded(collisionCache, player, direction);
		return collisionCache.getCollisionBoxes();
	}

	/**
	 * 判断玩家在指定方向是否满足手臂悬挂条件（底部有支撑且顶部通畅）。
	 *
	 * @param player    目标玩家，不能为 null
	 * @param direction 水平方向，不能为 null
	 * @return true 表示该方向可以悬挂
	 */
	public static boolean isValidCollision(Player player, Direction direction) {
		SensorData data = SensorDataManager.get(player).getData(SensorType.ARMHANG_TOP);
		CollisionCache collisionCache = data.getCollisionCache(direction);
		updateCacheIfNeeded(collisionCache, player, direction);
		return collisionCache.isCollided();
	}

	/**
	 * 按 tick+位置双维度检查缓存有效性，失效时重新计算碰撞并回写。
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

			List<AABB> boxes =
					buildOffsetBoxes(player, direction, player.getBbHeight());
			collisionCache.setCollisionBoxes(boxes);

			if (!meetsArmhangCondition(player, boxes)) {
				collisionCache.setCollided(false);
				return;
			}
			collisionCache.setCollided(true);
			return;
		}

		List<AABB> boxes = collisionCache.getCollisionBoxes();
		if (!meetsArmhangCondition(player, boxes)) {
			collisionCache.setCollided(false);
			return;
		}
		collisionCache.setCollided(true);
	}

	/**
	 * 检查底部支撑盒有方块碰撞且顶部通畅盒无方块碰撞。
	 *
	 * @param player 目标玩家，不能为 null
	 * @param boxes  检测盒列表（索引0=底部，索引1=顶部），不能为 null
	 * @return true 满足悬挂环境条件
	 */
	private static boolean meetsArmhangCondition(Player player, List<AABB> boxes) {
		AABB bottomBox = boxes.get(0);
		AABB topBox = boxes.get(1);
		boolean bottomHasSupport =
				BlockCollisions.hasBlockCollision(player.level(), bottomBox,
						IGNORED_BLOCKS_TAG);
		boolean topIsClear = !BlockCollisions.hasBlockCollision(player.level(), topBox,
				IGNORED_BLOCKS_TAG);
		if (bottomHasSupport && topIsClear) {
			return true;
		}
		return false;
	}

	/**
	 * 基于玩家包围盒构造身高高度的两个碰撞检测盒（底部支撑+顶部通畅）， 并按方向偏移
	 * {@value ParkourConstants#ARMHANG_SENSOR_CHECK_DISTANCE} 格后返回。
	 * <p>底部盒从 baseHeight 向下延伸约 {@value ParkourConstants#ARMHANG_GRIP_HEIGHT_RATIO} 身高，顶部盒从
	 * baseHeight 向上延伸至头顶上方。</p>
	 *
	 * @param player     目标玩家，不能为 null
	 * @param direction  水平方向，不能为 null
	 * @param baseHeight 检测基准高度（本传感器使用玩家总身高）
	 * @return 包含底部盒和顶部盒的列表，永不返回 null
	 */
	private static List<AABB> buildOffsetBoxes(Player player, Direction direction,
			double baseHeight) {
		Vec3 pos = player.position();
		double halfWidth = player.getBbWidth() / 2;
		double height = player.getBbHeight();
		double topOffset = height - player.getEyeHeight();

		double minX = pos.x - halfWidth;
		double maxX = pos.x + halfWidth;
		double minZ = pos.z - halfWidth;
		double maxZ = pos.z + halfWidth;
		double baseY = pos.y + baseHeight;

		AABB bottomBox = new AABB(minX, baseY, minZ, maxX,
				baseY - (height * ParkourConstants.ARMHANG_GRIP_HEIGHT_RATIO),
				maxZ).deflate(ParkourConstants.AABB_DEFLATE_EPSILON);

		AABB topBox = new AABB(minX, baseY, minZ, maxX, baseY + topOffset, maxZ).deflate(
				ParkourConstants.AABB_DEFLATE_EPSILON);

		double dx = direction.getStepX() * ParkourConstants.ARMHANG_SENSOR_CHECK_DISTANCE;
		double dz = direction.getStepZ() * ParkourConstants.ARMHANG_SENSOR_CHECK_DISTANCE;

		List<AABB> boxes = new ArrayList<>();
		boxes.add(bottomBox.move(dx, 0, dz));
		boxes.add(topBox.move(dx, 0, dz));
		return boxes;
	}
}
