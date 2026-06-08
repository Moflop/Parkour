package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 手臂悬挂时的物理约束。
 * <p>
 * 每 tick 将玩家速度清零以实现悬浮，同时沿悬挂方向施加微小的水平吸附力，
 * 让玩家紧贴墙壁。吸附力仅 {@value ParkourConstants#WALL_ADHESION_FORCE} 倍墙面法向量，不足以推动玩家穿过方块，
 * 但能抵消浮点误差造成的漂移。吸附移动后恢复 onGround 状态，
 * 防止水平贴墙动作意外结束悬挂。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ArmhangPhysics {

	/**
	 * 每 tick 执行悬浮（速度归零）和墙面吸附。
	 * 同时重置摔落高度，确保退出悬挂后不会承受不应有的摔落伤害。
	 *
	 * @param player           目标玩家，不可为 null
	 * @param wallMovementData 墙面移动数据，从中读取当前悬挂方向，不可为 null
	 */
	public static void applyLevitateAndAdhesion(Player player,
			WallMovementData wallMovementData) {
		Direction wallCollisionDirection = wallMovementData.getArmhang();
		if (wallCollisionDirection != null) {
			// 悬空
			player.setDeltaMovement(Vec3.ZERO);

			// 墙面吸附
			Vec3 wallNormal = new Vec3(wallCollisionDirection.getStepX(), 0,
					wallCollisionDirection.getStepZ());
			Vec3 adhesionForce = wallNormal.scale(ParkourConstants.WALL_ADHESION_FORCE);
			boolean wasOnGround = player.onGround();
			player.move(MoverType.PLAYER, adhesionForce);
			if (wasOnGround) {
				// 恢复 onGround 状态，防止由于水平贴墙导致的意外状态结束
				player.setOnGround(true);
			}

			player.resetFallDistance();
		}
	}
}
