package mod.arcomit.parkour.utils;

import mod.arcomit.parkour.ParkourConstants;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 前方障碍物高度检测 —— 通过切片扫描找出阻挡方块位置，用于蹲跳/攀爬决策。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class Obstacles {
	/** 垂直扫描步长（单位：格），控制高度切片精度 */
	private static final double SCAN_STEP = 0.1;
	/** 向前检测距离（单位：格），沿视线方向扩展切片盒以探测前方方块 */
	private static final double SCAN_REACH = 1.0;

	/**
	 * 从玩家头顶向脚底逐层切片扫描，找出前方第一个阻挡方块的高度位置。
	 * <p>
	 * 以 {@value #SCAN_STEP} 格为步长，从头顶向下构建水平切片盒，沿玩家视线方向扩展 {@value #SCAN_REACH} 格做碰撞检测。
	 * 可用于判断跑酷中面前障碍物的高度，以便决定是否需要蹲跳或攀爬。
	 *
	 * @param player    目标玩家，不可为null
	 * @param direction 检测方向（当前实现中使用的是玩家视线方向，而非 direction 参数）
	 * @return 第一个碰撞点的相对高度（以玩家脚底为 0），无障碍物时返回 0
	 */
	public static double findHeight(Player player, Direction direction) {
		Vec3 pos = player.position();
		Level level = player.level();

		double halfWidth = player.getBbWidth() / 2;
		double minX = pos.x - halfWidth;
		double maxX = pos.x + halfWidth;
		double minZ = pos.z - halfWidth;
		double maxZ = pos.z + halfWidth;

		double baseHeight = player.getBbHeight() + SCAN_STEP;

		int steps = (int) Math.round(baseHeight / SCAN_STEP);

		Vec3 lookVec = player.getLookAngle();
		Vec3 facing = new Vec3(lookVec.x, 0, lookVec.z).normalize();

		for (int i = 0; i < steps; i++) {
			double y = baseHeight - (SCAN_STEP * i);
			double sliceTop = pos.y + y;
			double sliceBottom = pos.y + baseHeight - (SCAN_STEP * (i + 1));

			AABB sliceBox = new AABB(minX, sliceBottom, minZ, maxX, sliceTop,
					maxZ).inflate(ParkourConstants.AABB_DEFLATE_EPSILON);

			AABB checkBox = sliceBox.expandTowards(facing.x * SCAN_REACH, 0,
					facing.z * SCAN_REACH);

			if (!level.noCollision(player, checkBox)) {
				return y;
			}
		}

		return 0;
	}


	/**
	 * 检测玩家在某方向上、从指定高度起一整格身高的空间是否无碰撞。 常用于翻越障碍前判断上方是否有足够空间让玩家通过。
	 *
	 * @param player     目标玩家，不可为null
	 * @param direction  检测方向
	 * @param baseHeight 检测起始高度（相对玩家脚底），例如 1.0 表示从玩家腰部以上开始检测
	 * @return true 表示该空间无障碍物，玩家可以通过
	 */
	public static boolean hasSpaceAbove(Player player, Direction direction, double baseHeight) {
		Vec3 pos = player.position();
		Level level = player.level();

		double halfWidth = player.getBbWidth() / 2;
		double height = player.getBbHeight();

		AABB baseBox = new AABB(pos.x - halfWidth, pos.y + baseHeight, pos.z - halfWidth,
				pos.x + halfWidth, pos.y + baseHeight + height, pos.z + halfWidth);
		AABB checkBox = baseBox.expandTowards(direction.getStepX() * SCAN_REACH,
				direction.getStepY() * SCAN_REACH,
				direction.getStepZ() * SCAN_REACH);
		if (level.noCollision(player, checkBox)) {
			return true;
		}

		return false;
	}
}
