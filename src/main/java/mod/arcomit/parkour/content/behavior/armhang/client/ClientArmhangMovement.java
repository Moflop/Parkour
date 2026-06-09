package mod.arcomit.parkour.content.behavior.armhang.client;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangCollision;
import mod.arcomit.parkour.content.context.InputData;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端悬挂平移处理。
 * <p>
 * 仅当玩家面向悬挂方向（误差 75 度以内）且有横向输入时才执行移动。 移动后若悬挂点依然有效则结束；否则委托给外角旋转处理。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientArmhangMovement {

	/**
	 * 根据玩家输入执行悬挂状态下的水平移动。
	 * <p>
	 * 移动方向由悬挂方向的法向量和输入的左右符号共同决定， 速度由 {@code ParkourConfig.armhangMoveSpeed} 控制。
	 * 若移动后失去悬挂点，会尝试外角旋转（绕过墙角）而非直接退出。
	 *
	 * @param player           目标玩家，不可为 null
	 * @param inputData        输入数据，提供横向移动方向和强度，不可为 null
	 * @param wallMovementData 墙面移动数据，提供当前悬挂方向，不可为 null
	 */
	public static void applyArmhangMovement(Player player, InputData inputData,
			WallMovementData wallMovementData) {
		float leftImpulse = inputData.getLeftImpulse();
		Direction armhangDir = wallMovementData.getArmhang();

		// 玩家必须面向悬挂方向且有横向输入
		if (!Directions.isFacing(player, armhangDir, 75.0f) || leftImpulse == 0.0f) {
			return;
		}

		Vec3 horizontalMovement = computeHorizontalMovement(leftImpulse, armhangDir);
		Vec3 beforeMovePos = player.position();
		player.move(MoverType.PLAYER, horizontalMovement);

		// 移动后仍能保持悬挂，正常结束
		if (ArmhangCollision.hasValidHangPoint(player, armhangDir)) {
			return;
		}

		// 否则尝试外角旋转，将处理权委派给边界处理类
		ClientArmhangRotation.tryOutsideCornerRotation(player, leftImpulse, armhangDir,
				wallMovementData, beforeMovePos);
	}

	/**
	 * 根据输入方向和悬挂方向计算水平移动向量。 速度大小为配置的固定值，方向为悬挂法线方向（左右）乘以输入符号。
	 *
	 * @param leftImpulse 横向输入值，正数右移负数左移，可为任意浮点数
	 * @param armhangDir  当前悬挂方向，不可为 null
	 * @return 水平移动向量，速度固定由配置决定
	 */
	private static Vec3 computeHorizontalMovement(float leftImpulse, Direction armhangDir) {
		Vec3 rightDirection = new Vec3(armhangDir.getStepZ(), 0, -armhangDir.getStepX());
		float sign = Math.signum(leftImpulse);
		return rightDirection.scale(sign * ParkourConfig.armhangMoveSpeed);
	}
}
