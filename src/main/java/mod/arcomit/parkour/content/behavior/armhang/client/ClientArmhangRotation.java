package mod.arcomit.parkour.content.behavior.armhang.client;

import mod.arcomit.parkour.content.behavior.armhang.ArmhangCollision;
import mod.arcomit.parkour.content.behavior.armhang.network.SyncArmhangDirC2SPayload;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 客户端外角旋转处理——玩家在悬挂状态下绕过凸墙角拐弯。
 * <p>
 * 当玩家水平移动到墙角尽头、失去当前面悬挂点时，先沿原方向微推一小段 使玩家接触到侧面，然后检测侧面是否可悬挂。若能则切换方向并同步服务端， 若不能则将玩家位置回退到移动前。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientArmhangRotation {
	/** 绕过墙角时沿原方向微推的距离（格），使碰撞盒接触侧面 */
	private static final float CORNER_PUSH_DISTANCE = 0.05f;

	/**
	 * 尝试外角旋转：微推玩家使其接触相邻墙面，检测并切换悬挂方向。 若切换成功则向服务端同步新的悬挂方向和位置；若失败则回退位置。
	 *
	 * @param player           目标玩家，不可为 null，其位置可能被微推或回退
	 * @param leftImpulse      横向输入值，正数为顺时针旋转，负数为逆时针
	 * @param armhangDir       当前悬挂方向，不可为 null
	 * @param wallMovementData 墙面移动数据，切换成功时其悬挂方向会被更新，不可为 null
	 * @param beforeMovePos    水平移动前的位置，用于失败时回退，不可为 null
	 */
	public static void tryOutsideCornerRotation(Player player, float leftImpulse,
			Direction armhangDir, WallMovementData wallMovementData,
			Vec3 beforeMovePos) {

		// 沿原悬挂方向微推，使玩家能够接触到侧面
		Vec3 pushOffset = new Vec3(armhangDir.getStepX(), 0, armhangDir.getStepZ()).scale(
				CORNER_PUSH_DISTANCE);
		player.move(MoverType.PLAYER, pushOffset);

		// 根据输入方向确定新的悬挂方向
		Direction newDirection =
				(leftImpulse > 0) ? armhangDir.getClockWise() // 左移 → 顺时针（左转）
						: armhangDir.getCounterClockWise(); // 右移 → 逆时针（右转）

		if (ArmhangCollision.hasValidHangPoint(player, newDirection)) {
			wallMovementData.setArmhang(newDirection);
			// 向服务端同步坐标，避免新垂挂方向被判定为无效导致退出垂挂
			ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
			ClientPacketDistributor.sendToServer(
					new SyncArmhangDirC2SPayload(newDirection));
		} else {
			player.setPos(beforeMovePos.x, beforeMovePos.y, beforeMovePos.z);
		}
	}
}
