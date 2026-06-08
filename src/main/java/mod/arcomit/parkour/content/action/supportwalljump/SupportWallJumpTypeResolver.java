package mod.arcomit.parkour.content.action.supportwalljump;

import mod.arcomit.parkour.utils.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 支撑蹬墙跳类型判定器——根据玩家朝向与垂挂墙面方向的夹角决定跳跃类型。
 *
 * <p>判定规则：玩家朝向与垂挂墙面方向夹角<={@value #UP_ANGLE_THRESHOLD}度为UP（向上跳），
 * 其余为VIEW（向前跳）。参数为null返回NONE。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SupportWallJumpTypeResolver {
	/** 判定上跳（UP）的角度阈值（度） */
	private static final float UP_ANGLE_THRESHOLD = 75.0F;

	/**
	 * 根据玩家与垂挂墙面的相对关系解析跳跃类型。
	 *
	 * @param player 执行跳跃的玩家；为null时返回NONE
	 * @param armhangDir 垂挂状态记录的墙面方向；为null时返回NONE
	 * @return 解析出的跳跃类型，NONE表示参数无效
	 */
	public static SupportWallJumpType resolve(Player player, Direction armhangDir) {
		if (player == null || armhangDir == null) {
			return SupportWallJumpType.NONE;
		}

		if (Directions.isFacing(player, armhangDir, UP_ANGLE_THRESHOLD)) {
			return SupportWallJumpType.UP;
		}

		return SupportWallJumpType.VIEW;
	}
}
