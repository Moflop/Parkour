package mod.arcomit.parkour.utils;

import mod.arcomit.parkour.ParkourConfig;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * 跑酷动作条件判定
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourChecks {

	/**
	 * 判断玩家是否处于允许跑酷动作的基础状态。 排除观察者、骑乘、睡觉、三叉戟激流、鞘翅滑翔、创造飞行等与跑酷互斥的状态。
	 *
	 * @param player 目标玩家，不可为null
	 * @return true 表示玩家可以进行跑酷动作
	 */
	public static boolean canPerformAction(Player player) {
		return !player.isSpectator() && !player.isPassenger() && !player.isSleeping() && !player.isAutoSpinAttack() && !player.isFallFlying() && !player.getAbilities().flying;
	}

	/**
	 * 判断玩家是否允许执行跑酷行为动作（如蹲跳、攀爬等需要特定姿态的操作）。 在 {@link #canPerformAction(Player)}
	 * 基础上，额外排除游泳姿态（强制姿态除外）。
	 *
	 * @param player 目标玩家，不可为null
	 * @return true 表示玩家姿态允许执行跑酷行为
	 */
	public static boolean canPerformBehavior(Player player) {
		return canPerformAction(
				player) && (player.getForcedPose() != null || player.getPose() != Pose.SWIMMING);
	}

	/**
	 * 判断玩家当前坠落距离是否超过安全阈值，超限后将触发受伤判定。 阈值由 {@link ParkourConfig#safeFallHeight} 配置。
	 *
	 * @param player 目标玩家，不可为null
	 * @return true 表示坠落高度已达到受伤标准
	 */
	public static boolean isFallUnsafe(Player player) {
		return player.fallDistance > ParkourConfig.safeFallHeight;
	}
}
