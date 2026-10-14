package mod.arcomit.parkour.content.action.speedvaultjump;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.behavior.speedvault.SpeedVaultPhysics;
import mod.arcomit.parkour.content.behavior.speedvault.SpeedVaultState;
import mod.arcomit.parkour.content.context.JumpData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 速过接跳动作——再次按下跳跃键时立即结束翻越并起跳。
 *
 * <p>双端复用相同准入和物理逻辑，自然结束后的短暂窗口也可接跳；每次翻越仅允许接跳一次。
 *
 * @author Mitok
 * @since 2026-10-14
 */
public class SpeedVaultJumpAction {

	/**
	 * 执行速过接跳。不满足准入条件时静默返回。
	 *
	 * @param player 执行动作的玩家，不能为 null
	 * @return 是否成功接跳，客户端成功后需发送对应的动作包
	 */
	public static boolean execute(Player player) {
		ParkourContext context = ParkourContext.get(player);
		IParkourState currentState = context.state().getState();
		JumpData jumpData = context.jump();
		boolean vaulting = currentState instanceof SpeedVaultState;
		if (!vaulting && (!(currentState instanceof DefaultState)
				|| jumpData.getSpeedVaultJumpWindow() <= 0)) {
			jumpData.setSpeedVaultJumpWindow(0);
			return false;
		}
		if (!ParkourConfig.enableSpeedVault || !ParkourConfig.enableSpeedVaultJump || !player.isAlive()
				|| !ParkourProxies.GET_CLIENT_CONFIG_PROXY.isEnableParkour(context)
				|| !ParkourChecks.canPerformBehavior(player) || player.onClimbable()
				|| player.isInWater() || player.isInLava() || player.isCrouching()
				|| ParkourProxies.INPUT_PROXY.getShiftKeyDown(player)) {
			return false;
		}

		Vec3 velocity = SpeedVaultPhysics.computeJumpVelocity(player, context.wall(), vaulting);
		if (vaulting) {
			velocity = DynamicSurfaceMotion.jumpVelocity(player, player.getDirection(), velocity);
		}
		// 退出时会重设速度并可能开启宽限，必须先退出，再消耗窗口和施加跳跃。
		ParkourStateMachine.resetToDefaultState(player, context);
		jumpData.setSpeedVaultJumpWindow(0);
		jumpData.setJumped(true);
		jumpData.setTicksSinceLastJump(0);
		player.setDeltaMovement(velocity);
		player.setOnGround(false);
		player.hasImpulse = true;
		player.resetFallDistance();
		return true;
	}
}
