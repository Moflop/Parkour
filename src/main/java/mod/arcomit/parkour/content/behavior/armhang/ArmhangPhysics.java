package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.utils.BlockCollisions;
import net.minecraft.util.Mth;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 手臂悬挂时的物理约束。
 * <p>
 * 每 tick 清除下落速度，并将本地碰撞体逐步收敛到真实抓点下方，使眼睛、躯干和双手保持一致的悬挂高度。
 * 高度收敛和墙面吸附均经过碰撞求解；若移动破坏挂点或身体净空，则恢复到移动前的位置。
 * 吸附力仅为 {@value ParkourConstants#WALL_ADHESION_FORCE} 倍墙面法向量，用于抵消浮点误差造成的漂移。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ArmhangPhysics {

	/**
 * 每 tick 执行悬浮、高度收敛和墙面吸附。重置摔落高度，退出后重新累计实际下落距离。
	 *
	 * @param player           目标玩家，不可为 null
	 * @param wallMovementData 墙面移动数据，从中读取当前悬挂方向，不可为 null
	 */
	public static void applyLevitateAndAdhesion(Player player,
			WallMovementData wallMovementData) {
		Direction wallCollisionDirection = wallMovementData.getArmhang();
		if (wallCollisionDirection != null && wallCollisionDirection.getAxis().isHorizontal()) {
			DynamicCollisions.clearSurfaceInertia(player);
			// 悬空
			player.setDeltaMovement(Vec3.ZERO);
			settleGripHeight(player, wallCollisionDirection);

			// 墙面吸附
			tryAdhesion(player, wallCollisionDirection);

			player.resetFallDistance();
		}
	}

	/** The broad acquisition sensor must not leave the eyes half a block above the palms. */
	private static void settleGripHeight(Player player, Direction direction) {
		ArmhangGrip grip = ArmhangData.get(player).grip;
		if (!player.isLocalPlayer() || grip == null || !ArmhangCollision.hasValidHangPoint(player, direction)) return;
		double hangingHeight = player.getBbHeight() - 0.03 * player.getScale();
		double targetY = grip.logical(grip.localCenter()).y - hangingHeight;
		double moveY = Mth.clamp(targetY - player.getY(), -0.16, 0.16);
		if (Math.abs(moveY) < 1.0E-5) return;
		ArmhangMoveSnapshot before = ArmhangMoveSnapshot.capture(player);
		DynamicCollisions.move(player, MoverType.PLAYER, new Vec3(0, moveY, 0));
		if (!ArmhangCollision.hasValidHangPoint(player, direction)
				|| !BlockCollisions.noSensorCollision(player, player.getBoundingBox().deflate(ParkourConstants.AABB_DEFLATE_EPSILON))) {
			before.restore(player);
		}
	}

	/** A tilted wall's collision response must not push an already valid grip off the ledge. */
	public static boolean tryAdhesion(Player player, Direction direction) {
		if (!ArmhangCollision.hasValidHangPoint(player, direction)) return false;
		ArmhangMoveSnapshot before = ArmhangMoveSnapshot.capture(player);
		Vec3 normal = DynamicSurfaceMotion.wallNormal(player, direction);
		DynamicCollisions.move(player, MoverType.PLAYER,
				normal.scale(ParkourConstants.WALL_ADHESION_FORCE));
		if (!ArmhangCollision.hasValidHangPoint(player, direction)) {
			// Server positions remain owned by the normal client movement packets.
			if (player.isLocalPlayer()) before.restore(player);
			return false;
		}
		if (before.onGround()) player.setOnGround(true);
		return true;
	}
}
