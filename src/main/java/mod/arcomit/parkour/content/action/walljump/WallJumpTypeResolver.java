package mod.arcomit.parkour.content.action.walljump;

import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.utils.BlockCollisions;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 蹬墙跳类型判定器——根据玩家朝向与墙面方向的几何关系决定跳跃类型。
 *
 * <p>判定优先级：面前有脚手架阻挡 -> UP；朝向与墙面夹角<={@value #PARALLEL_JUMP_ANGLE}度 -> PARALLEL；
 * 其余 -> VIEW。参数为null时返回NONE。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpTypeResolver {
	/** 判定侧跳（PARALLEL）的角度阈值（度），玩家朝向与墙面方向夹角在此范围内判定为侧跳 */
	private static final float PARALLEL_JUMP_ANGLE = 110;

	/**
	 * 根据玩家与墙面的相对关系解析跳跃类型。
	 *
	 * @param player 执行跳跃的玩家；为null时返回NONE
	 * @param wallDir 目标墙面方向；为null时返回NONE
	 * @return 解析出的跳跃类型，NONE表示当前不可跳跃
	 */
	public static WallJumpType resolve(Player player, Direction wallDir) {
		if (player == null || wallDir == null) {
			return WallJumpType.NONE;
		}

		if (isBlockInFrontOfPlayer(player)) {
			return WallJumpType.UP;
		}

		if (Directions.isFacing(player, wallDir, PARALLEL_JUMP_ANGLE)) {
			return WallJumpType.PARALLEL;
		}

		return WallJumpType.VIEW;
	}

	/**
	 * 检查玩家正前方是否被脚手架类方块阻挡。
	 *
	 * @param player 待检查的玩家，不可为null
	 * @return true表示前方有障碍物，应触发上跳
	 */
	public static boolean isBlockInFrontOfPlayer(Player player) {
		return BlockCollisions.isBlockedTowards(player,
				player.getDirection(), ParkourTags.Blocks.SCAFFOLDING_BLOCKS);
	}
}
