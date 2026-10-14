package mod.arcomit.parkour.content.behavior.wallrun;

import mod.arcomit.parkour.compat.DynamicSurfaceFacing;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.sensor.impl.HeadFeetSensor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 负责墙跑状态下的碰撞检测与环境合法性校验。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallRunCollision {

	/**
	 * 检测玩家左右两侧是否存在有效墙体碰撞。
	 *
	 * <p>优先返回距离玩家更近的一侧。如果两侧都碰撞，取水平距离更短者；
	 * 仅一侧碰撞则返回该侧；均无碰撞返回 null。
	 *
	 * @param player 目标玩家，不能为 null
	 * @return 碰撞墙体所在的方向，两侧均无碰撞时返回 null
	 */
	public static Direction findFirstWallCollisionDirection(Player player) {
		return DynamicSurfaceFacing.runDirection(player);
	}

	/**
	 * 验证跑墙过程中记录的墙体碰撞方向是否仍然有效。
	 *
	 * @param player  目标玩家，不能为 null
	 * @param context 跑酷上下文，记录有之前锁定的碰撞方向，不能为 null
	 * @return 碰撞方向仍有效返回 true，方向为 null 或墙体消失返回 false
	 */
	public static boolean wallCollisionIsValid(Player player, ParkourContext context) {
		Direction wallDir = Direction.from3DDataValue(context.wall().getRunCollisionRaw());
		if (wallDir == null) {
			return false;
		}
		return checkWallCollision(player, wallDir);
	}

	/**
	 * 检测特定方向的墙壁碰撞
	 */
	private static boolean checkWallCollision(Player player, Direction dir) {
		return HeadFeetSensor.isValidCollision(player, dir);
	}
}
