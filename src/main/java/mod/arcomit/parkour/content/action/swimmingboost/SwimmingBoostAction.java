package mod.arcomit.parkour.content.action.swimmingboost;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.context.SwimMovementData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 水中推进逻辑类。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SwimmingBoostAction {

	/**
	 * 在水中沿当前运动方向（或视线方向）施加一次速度推进。
	 *
	 * <p>推进方向判定：
	 * <ul>
	 *   <li>玩家在运动中：沿当前运动方向推进（顺势加速）</li>
	 *   <li>玩家静止：沿视线方向推进</li>
	 * </ul>
	 * </p>
	 *
	 * @param player   执行推进的玩家，不可为null
	 * @param swimData 游泳数据容器，不可为null
	 * @return true表示推进成功执行，false表示不满足准入条件
	 * @sideeffect 直接修改玩家速度向量（叠加推进速度）
	 * @sideeffect 写入 swimData 的冷却时间字段
	 */
	public static boolean execute(Player player, SwimMovementData swimData) {
		if (!SwimmingBoostEligibilityChecker.check(player, swimData)) {
			return false;
		}

		swimData.setBoostCooldown(ParkourConfig.swimmingBoostCooldown);
		Vec3 deltaMovement = player.getDeltaMovement();

		Vec3 boostDirection;
		boolean isMoving = deltaMovement.lengthSqr() >= ParkourConstants.ZERO_THRESHOLD;
		if (isMoving) {
			boostDirection = deltaMovement.normalize();
		} else {
			boostDirection = player.getLookAngle();
		}

		Vec3 boostVelocity =
				boostDirection.scale(ParkourConfig.swimmingBoostSpeedMultiplier);
		player.setDeltaMovement(deltaMovement.add(boostVelocity));

		return true;
	}
}
