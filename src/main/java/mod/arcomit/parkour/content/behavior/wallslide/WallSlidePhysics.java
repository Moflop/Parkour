package mod.arcomit.parkour.content.behavior.wallslide;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 负责滑墙状态下的物理受力与运动减速控制。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallSlidePhysics {
	private static final double HORIZONTAL_SLOWDOWN = 0.8; // 水平方向减速比例
	private static final double VERTICAL_SLOWDOWN = 0.7; // 垂直方向减速比例

	/**
	 * 对玩家施加减速和墙面吸附。
	 *
	 * <p>水平速度乘以 {@value #HORIZONTAL_SLOWDOWN}，垂直速度乘以 {@value #VERTICAL_SLOWDOWN}，
	 * 使玩家沿墙面缓慢下滑；同时向墙体法线方向施加 {@value ParkourConstants#WALL_ADHESION_FORCE} 的吸附力， 通过
	 * {@link Player#move} 实现以保证碰撞检测正确。
	 *
	 * @param player           目标玩家，不能为 null，速度被覆写，位置被 move 偏移
	 * @param wallMovementData 墙体数据，包含当前滑墙方向，不能为 null
	 */
	public static void applySlowdownAndAdhesion(Player player,
			WallMovementData wallMovementData) {
		Direction wallCollisionDirection = wallMovementData.getSlide();

		if (wallCollisionDirection != null) {
			Vec3 motion = player.getDeltaMovement();

			// 修改降落速度
			Vec3 slowedMotion = motion.multiply(HORIZONTAL_SLOWDOWN, VERTICAL_SLOWDOWN,
					HORIZONTAL_SLOWDOWN);
			player.setDeltaMovement(slowedMotion);

			// 墙面吸附
			Vec3 wallNormal = new Vec3(wallCollisionDirection.getStepX(), 0,
					wallCollisionDirection.getStepZ());
			Vec3 adhesionForce = wallNormal.scale(ParkourConstants.WALL_ADHESION_FORCE);
			boolean wasOnGround = player.onGround();

			player.move(MoverType.PLAYER, adhesionForce);

			if (wasOnGround) {
				// Move方法在水平移动时会让onGround改为false，为了防止落地也无法结束滑墙需要恢复 onGround 状态
				player.setOnGround(true);
			}

			player.resetFallDistance();
		}
	}
}
