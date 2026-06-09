package mod.arcomit.parkour.content.action.walljump;

import mod.arcomit.parkour.content.context.JumpData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 蹬墙跳动作——玩家贴着墙面时按跳跃键向后弹跳。
 *
 * <p>执行流程：准入检查 -> 墙面方向解析 -> 跳型判定 -> 防重复墙面检查 ->
 * 速度应用 -> 记录最近跳跃墙面（仅单面墙）-> 状态重置与音效。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpAction {

	/**
	 * 执行蹬墙跳完整流程。不满足准入条件时静默返回，不做任何操作。
	 *
	 * @param player 执行动作的玩家，不可为null
	 * @sideeffect 直接修改玩家速度向量（{@code setDeltaMovement}）
	 * @sideeffect 重置玩家状态机到默认状态
	 * @sideeffect 在服务端播放蹬墙跳音效
	 * @sideeffect 重置玩家摔落距离
	 */
	public static void execute(Player player) {
		ParkourContext context = ParkourContext.get(player);
		IParkourState currentState = context.state().getState();
		if (!WallJumpEligibilityChecker.check(player, currentState))
			return;

		Direction wallDir =
				WallJumpDirectionResolver.resolveWallDirection(player, currentState,
						context);
		if (wallDir == null)
			return;

		WallJumpType wallJumpType = WallJumpTypeResolver.resolve(player, wallDir);
		if (wallJumpType == WallJumpType.NONE)
			return;

		JumpData jumpData = context.jump();
		if (WallJumpSameWallGuard.isSameWallAsLastJump(jumpData, wallJumpType, wallDir))
			return;

		Vec3 velocity = WallJumpVelocity.computeJumpVelocity(player, wallJumpType);
		player.setDeltaMovement(velocity);

		// 更新最近跳跃方向（只挨着一面墙时记录，如果挨着多面前可以无限次跳跃所以无需记录。）
		if (WallJumpSameWallGuard.isAgainstSingleWall(player)) {
			WallJumpSameWallGuard.recordLastJumpWall(jumpData, wallJumpType, wallDir);
		}

		jumpData.setJumped(true);
		jumpData.setTicksSinceLastJump(0);
		ParkourStateMachine.resetToDefaultState(player, context);
		WallJumpSound.play(player);
		player.resetFallDistance();
	}
}
