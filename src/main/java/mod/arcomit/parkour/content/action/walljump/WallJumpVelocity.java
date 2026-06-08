package mod.arcomit.parkour.content.action.walljump;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 蹬墙跳速度计算器——根据跳跃类型算出施加给玩家的速度向量。
 *
 * <p>UP类型纯垂直向上；PARALLEL和VIEW均为玩家水平朝向加垂直分量，
 * 水平速度0.7、垂直速度0.4方/秒。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpVelocity {
	/** 后跳/侧跳的水平速度分量 */
	private static final double WALL_JUMP_FORWARD_HORIZONTAL = 0.7;
	/** 后跳/侧跳的垂直速度分量 */
	private static final double WALL_JUMP_FORWARD_VERTICAL = 0.4;
	/** 上跳的纯垂直速度 */
	private static final double WALL_JUMP_UPWARD_VERTICAL = 0.4;

	/**
	 * 根据跳跃类型和玩家朝向计算弹跳速度向量。
	 *
	 * @param player 执行跳跃的玩家，不可为null
	 * @param type 蹬墙跳类型，不能为NONE（应在上层过滤）
	 * @return 速度向量，NONE类型返回零向量
	 */
	static Vec3 computeJumpVelocity(Player player, WallJumpType type) {
		return switch (type) {
			case UP -> new Vec3(0, WALL_JUMP_UPWARD_VERTICAL, 0);
			case PARALLEL, VIEW -> {
				Vec3 look = player.getLookAngle();
				Vec3 jumpDir = new Vec3(look.x, 0, look.z).normalize();
				yield jumpDir.scale(WALL_JUMP_FORWARD_HORIZONTAL)
						.add(0, WALL_JUMP_FORWARD_VERTICAL, 0);
			}
			default -> Vec3.ZERO;
		};
	}
}
