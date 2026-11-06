package mod.arcomit.parkour.content.behavior.armhang.client;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangCollision;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangMoveSnapshot;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangData;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangLedge;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangMotion;
import mod.arcomit.parkour.content.context.InputData;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端悬挂平移处理。
 * <p>
 * 沿抓沿切线加减速移动；转头观察不会锁住横移。每个小步都验证抓点，边缘无路时保留原挂点。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientArmhangMovement {

	/**
	 * 根据玩家输入执行悬挂状态下的水平移动。
	 * <p>
	 * 移动方向由抓沿法向量和横向输入决定，最高速度由 {@code ParkourConfig.armhangMoveSpeed} 控制。
	 * 若移动后失去悬挂点，会尝试外角旋转（绕过墙角）而非直接退出。
	 *
	 * @param player           目标玩家，不可为 null
	 * @param inputData        输入数据，提供横向移动方向和强度，不可为 null
	 * @param wallMovementData 墙面移动数据，提供当前悬挂方向，不可为 null
	 */
	public static void applyArmhangMovement(Player player, InputData inputData,
			WallMovementData wallMovementData) {
		Direction armhangDir = wallMovementData.getArmhang();
		if (armhangDir == null || !armhangDir.getAxis().isHorizontal()) return;
		ArmhangData data = ArmhangData.get(player);
		data.shimmySpeed = ArmhangMotion.shimmySpeed(data.shimmySpeed, inputData.getLeftImpulse(), ParkourConfig.armhangMoveSpeed);
		if (Math.abs(data.shimmySpeed) < 1.0E-6) return;
		// Small verified steps keep configured high speeds from skipping narrow edges or corners.
		int steps = Math.max(1, (int) Math.ceil(Math.abs(data.shimmySpeed) / 0.045));
		for (int step = 0; step < steps; step++) {
			Direction direction = wallMovementData.getArmhang();
			Vec3 normal = DynamicSurfaceMotion.wallNormal(player, direction);
			Vec3 tangent = new Vec3(normal.z, 0, -normal.x);
			ArmhangMoveSnapshot before = ArmhangMoveSnapshot.capture(player);
			DynamicCollisions.move(player, MoverType.PLAYER, tangent.scale(data.shimmySpeed / steps));
			if (!ArmhangCollision.hasValidHangPoint(player, direction)) {
				ClientArmhangRotation.tryOutsideCornerRotation(player, (float) data.shimmySpeed, direction, wallMovementData, before);
			}
			double travelled = player.position().subtract(before.position()).dot(tangent);
			data.shimmyDistance += travelled;
			if (Math.abs(travelled) < 1.0E-5 && direction == wallMovementData.getArmhang()) {
				data.shimmySpeed = 0;
				break;
			}
		}
		data.updateGrip(player, ArmhangLedge.find(player, wallMovementData.getArmhang()));
	}

}
