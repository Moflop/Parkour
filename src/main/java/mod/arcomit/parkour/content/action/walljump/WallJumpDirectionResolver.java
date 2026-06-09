package mod.arcomit.parkour.content.action.walljump;

import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.behavior.wallclimb.WallClimbState;
import mod.arcomit.parkour.content.behavior.wallrun.WallRunState;
import mod.arcomit.parkour.content.behavior.wallslide.WallSlideState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 蹬墙跳墙面方向解析器——根据玩家当前行为状态判断弹跳应参照哪面墙。
 *
 * <p>不同状态下墙面信息存储位置不同：跑墙/滑墙从 WallMovementData 取，
 * 默认/爬墙状态则通过碰撞检测找出最近的墙面。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpDirectionResolver {

	/**
	 * 根据当前行为状态解析弹跳基准墙面方向。
	 *
	 * @param player  执行蹬墙跳的玩家，不可为null
	 * @param state   当前行为状态，不可为null
	 * @param context 跑酷上下文，不可为null
	 * @return 弹跳应参照的墙面方向；无法判定时返回null（调用方应立即中止执行）
	 */
	static Direction resolveWallDirection(Player player, IParkourState state,
			ParkourContext context) {
		if (state instanceof WallRunState) {
			WallMovementData wallMovementData = context.wall();
			return wallMovementData.getRunCollision();
		} else if (state instanceof WallSlideState) {
			WallMovementData wallMovementData = context.wall();
			return wallMovementData.getSlide();
		} else if (state instanceof DefaultState || state instanceof WallClimbState) {
			return WallJumpCollisionFinder.findClosestCollisionDir(player);
		}
		// else if (state == ParkourStates.ARMHANG.get()) {
		// WallMovementData wallMovementData = context.wall();
		// Direction armhangDir = wallMovementData.getArmhang();
		// if (!Directions.isFacing(player, armhangDir,
		// 75.0f)){
		// return wallMovementData.getArmhang();
		// }
		// return null;
		// }
		// 为未来垂挂预留扩展点
		// if (state == ParkourStates.ARMHANG.get()) {
		// return Direction.from3DDataValue(wallData.getArmHangingDir());
		// }
		return null;
	}
}
