package mod.arcomit.parkour.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 方向计算工具 —— 从候选方向中选最优朝向、判断玩家是否正对某个方向等。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class Directions {

	/**
	 * 从候选方向列表中选出相邻方块中心离玩家水平位置最近的一个。 仅在 XZ 平面计算距离，忽略 Y 轴差异。列表为空时返回 null。
	 *
	 * @param player 目标玩家，不可为null
	 * @param dirs   候选方向列表，可为空列表
	 * @return 最近的方向，列表为空时返回 null
	 */
	public static Direction findClosest(Player player, List<Direction> dirs) {
		Direction closest = null;
		double closestSq = Double.MAX_VALUE;
		Vec3 pos = player.position();
		BlockPos blockPos = player.blockPosition();

		for (Direction dir : dirs) {
			BlockPos neighborPos = blockPos.relative(dir);

			double deltaX = pos.x - neighborPos.getCenter().x;
			double deltaZ = pos.z - neighborPos.getCenter().z;
			double distSq = deltaX * deltaX + deltaZ * deltaZ;

			if (distSq < closestSq) {
				closestSq = distSq;
				closest = dir;
			}
		}
		return closest;
	}

	/**
	 * 判断玩家水平朝向与目标方向之间的偏角是否在允许范围内。 比较时仅考虑 Yaw（水平旋转），自动处理 360 度环绕差值。
	 *
	 * @param player   目标玩家，不可为null
	 * @param dir      目标方向
	 * @param maxAngle 允许的最大偏角（度），取值 0~180
	 * @return true 表示玩家正对该方向（偏角不超过 maxAngle）
	 */
	public static boolean isFacing(Player player, Direction dir, float maxAngle) {
		float yaw = player.getYRot();
		float targetYaw = dir.toYRot();

		float diff = Math.abs(yaw - targetYaw) % 360.0f;
		if (diff > 180.0f) {
			diff = 360.0f - diff;
		}

		return diff <= maxAngle;
	}
}
