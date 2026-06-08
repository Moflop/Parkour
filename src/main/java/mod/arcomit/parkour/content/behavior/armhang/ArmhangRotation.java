package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 悬挂时的内角旋转——当玩家在墙角处转身面向另一个可悬挂面时，
 * 自动将悬挂方向切换到新的面向方向。
 * <p>
 * 外角旋转（绕过墙角拐弯）的逻辑在客户端的 {@code ClientArmhangRotation} 中处理。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ArmhangRotation {

	/**
	 * 检测玩家当前面向方向是否存在悬挂点，若存在则更新悬挂方向。
	 * 用于实现内角旋转：玩家从一面墙转向相邻的另一面墙时自动吸附。
	 *
	 * @param player           目标玩家，不可为 null
	 * @param wallMovementData 墙面移动数据，悬挂方向可能被此方法修改，不可为 null
	 */
	public static void tryInsideCornerRotation(Player player,
			WallMovementData wallMovementData) {
		Direction facing = player.getDirection();
		// 依赖重构后的 CollisionLogic
		if (ArmhangCollision.hasValidHangPoint(player, facing)) {
			wallMovementData.setArmhang(facing);
		}
	}
}
