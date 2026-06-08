package mod.arcomit.parkour.content.action.supportwalljump;

import mod.arcomit.parkour.content.context.JumpData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 支撑蹬墙跳动作——垂挂状态下按跳跃键向前或向上弹跳。
 *
 * <p>执行流程与 WallJumpAction 类似：准入检查 -> 从 WallMovementData 取垂挂墙面方向 ->
 * 跳型判定 -> 速度应用 -> 状态重置与音效。与蹬墙跳的主要区别在于墙面方向来源固定为
 * 垂挂状态记录的墙面，且无同墙防连跳机制。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SupportWallJumpAction {

	/**
	 * 执行支撑蹬墙跳完整流程。不满足准入条件时静默返回。
	 *
	 * @param player 执行动作的玩家，不可为null
	 * @sideeffect 直接修改玩家速度向量（{@code setDeltaMovement}）
	 * @sideeffect 重置玩家状态机到默认状态
	 * @sideeffect 在服务端播放蹬墙跳音效
	 * @sideeffect 重置玩家摔落距离和跳跃计时
	 */
	public static void execute(Player player) {
		ParkourContext context = ParkourContext.get(player);
		IParkourState currentState = context.state().getState();
		if (!SupportWallJumpEligibilityChecker.check(player, currentState))
			return;

		WallMovementData wallMovementData = context.wall();
		Direction wallDir = wallMovementData.getArmhang();
		if (wallDir == null)
			return;

		SupportWallJumpType supportWallJumpType =
				SupportWallJumpTypeResolver.resolve(player, wallDir);
		if (supportWallJumpType == SupportWallJumpType.NONE)
			return;

		Vec3 velocity = SupportWallJumpVelocity.computeJumpVelocity(player,
				supportWallJumpType);
		player.setDeltaMovement(velocity);

		JumpData jumpData = context.jump();
		jumpData.setJumped(true);
		jumpData.setTicksSinceLastJump(0);
		ParkourStateMachine.resetToDefaultState(player, context);
		SupportWallJumpSound.play(player);
		player.resetFallDistance();
	}
}
