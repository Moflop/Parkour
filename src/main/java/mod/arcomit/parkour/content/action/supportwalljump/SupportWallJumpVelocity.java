package mod.arcomit.parkour.content.action.supportwalljump;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 支撑蹬墙跳速度计算器——根据跳跃类型算出施加给玩家的速度向量。
 *
 * <p>UP类型：较低水平速度0.2 + 较高垂直速度0.6（向上窜升感）。
 * VIEW类型：较高水平速度0.7 + 垂直速度0.4（向前弹跳感）。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SupportWallJumpVelocity {
	/** 上跳的水平速度分量，较小以保证玩家不飞太远 */
	private static final double SUPPORT_WALL_JUMP_UPWARD_HORIZONTAL_SPEED = 0.2;
	/** 上跳的垂直速度分量，较大以提供窜升感 */
	private static final double SUPPORT_WALL_JUMP_UPWARD_VERTICAL_SPEED = 0.55;

	/** 前跳的水平速度分量 */
	private static final double SUPPORT_WALL_JUMP_FORWARD_HORIZONTAL_SPEED = 0.7;
	/** 前跳的垂直速度分量 */
	private static final double SUPPORT_WALL_JUMP_FORWARD_VERTICAL_SPEED = 0.4;

	/**
	 * 根据跳跃类型和玩家朝向计算弹跳速度向量。
	 *
	 * @param player 执行跳跃的玩家，不可为null
	 * @param type   支撑蹬墙跳类型，不能为NONE（应在上层过滤）
	 * @return 速度向量，NONE类型返回零向量
	 */
	static Vec3 computeJumpVelocity(Player player, SupportWallJumpType type) {
		return switch (type) {
			case UP -> {
				Vec3 look = player.getLookAngle();
				Vec3 jumpDir = new Vec3(look.x, 0, look.z).normalize();
				yield jumpDir.scale(SUPPORT_WALL_JUMP_UPWARD_HORIZONTAL_SPEED)
						.add(0, SUPPORT_WALL_JUMP_UPWARD_VERTICAL_SPEED, 0);
			}
			case VIEW -> {
				Vec3 look = player.getLookAngle();
				Vec3 jumpDir = new Vec3(look.x, 0, look.z).normalize();
				yield jumpDir.scale(SUPPORT_WALL_JUMP_FORWARD_HORIZONTAL_SPEED)
						.add(0, SUPPORT_WALL_JUMP_FORWARD_VERTICAL_SPEED,
								0);
			}
			default -> Vec3.ZERO;
		};
	}
}
