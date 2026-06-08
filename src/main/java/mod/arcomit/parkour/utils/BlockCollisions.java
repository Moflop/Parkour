package mod.arcomit.parkour.utils;

import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import mod.arcomit.parkour.ParkourConstants;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 方块碰撞检测与碰撞盒构建。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class BlockCollisions {
	private static final double DEFAULT_RANGE = 0.05;

	/**
	 * 根据实体朝向和高度比例构造一个用于方向性碰撞检测的包围盒。
	 * 包围盒沿指定方向扩展 {@code distance} 格，高度范围由实体身高比例决定，
	 * 默认收缩 0.001 以避免与自身脚下方块误判。
	 *
	 * @param entity         目标实体，不可为null
	 * @param direction      检测方向
	 * @param minHeightRatio 包围盒底部占实体身高的比例，0=脚底，1=头顶
	 * @param maxHeightRatio 包围盒顶部占实体身高的比例
	 * @param distance       沿方向扩展的距离（格）
	 * @return 构造好的碰撞检测包围盒，永不返回null
	 */
	public static AABB buildBox(LivingEntity entity,
			Direction direction, double minHeightRatio, double maxHeightRatio,
			double distance) {
		Vec3 pos = entity.position();
		double halfWidth = entity.getBbWidth() / 2;
		double height = entity.getBbHeight();

		AABB base = new AABB(pos.x - halfWidth,
				pos.y + height * minHeightRatio, pos.z - halfWidth,
				pos.x + halfWidth, pos.y + height * maxHeightRatio,
				pos.z + halfWidth).deflate(ParkourConstants.AABB_DEFLATE_EPSILON);

		double ox = direction.getStepX() * distance;
		double oy = direction.getStepY() * distance;
		double oz = direction.getStepZ() * distance;

		return base.expandTowards(ox, oy, oz);
	}

	/**
	 * 检测实体在指定方向上是否被固体方块阻挡，使用默认检测距离 0.05 格。
	 *
	 * @param entity   目标实体，不可为null
	 * @param direction 检测方向
	 * @param skipTag  跳过该标签下的方块（不计入碰撞），可为null
	 * @return true 表示该方向被阻挡
	 */
	public static boolean isBlockedTowards(LivingEntity entity,
			Direction direction, TagKey<Block> skipTag) {
		return isBlockedTowards(entity, direction, DEFAULT_RANGE, skipTag);
	}

	/**
	 * 检测实体在指定方向上是否被固体方块阻挡。
	 *
	 * @param entity   目标实体，不可为null
	 * @param direction 检测方向
	 * @param range    向前检测的距离（格），值越大越容易检测到远处方块
	 * @param skipTag  跳过该标签下的方块（不计入碰撞），可为null
	 * @return true 表示该方向被阻挡
	 */
	public static boolean isBlockedTowards(LivingEntity entity,
			Direction direction, double range, TagKey<Block> skipTag) {
		double ox = direction.getStepX() * range;
		double oy = direction.getStepY() * range;
		double oz = direction.getStepZ() * range;
		AABB expanded = entity.getBoundingBox().expandTowards(ox, oy, oz);
		return hasBlockCollision(entity.level(), expanded, skipTag);
	}

	/**
	 * 判断指定包围盒在关卡中是否与任何符合条件的方块发生碰撞。
	 * 会跳过 skipTag 标记的方块以及空气/非固体方块。
	 *
	 * @param level   目标关卡，不可为null
	 * @param box     待检测的包围盒，不可为null
	 * @param skipTag 需排除的方块标签，可为null
	 * @return true 表示存在碰撞方块
	 */
	public static boolean hasBlockCollision(Level level, AABB box,
			TagKey<Block> skipTag) {
		for (VoxelShape shape : filterBlocks(level, box, skipTag)) {
			if (!shape.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 创建带标签过滤的方块碰撞形状迭代器，排除 skipTag 标记的方块。
	 */
	private static Iterable<VoxelShape> filterBlocks(Level level, AABB box,
			TagKey<Block> skipTag) {
		return () -> new FilteredBlockCollisions<>(level, null, box, false,
				skipTag, (pos, shape) -> shape);
	}
}
