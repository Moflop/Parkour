package mod.arcomit.parkour.content.mechanic.swimimprovements;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.world.entity.player.Player;

/**
 * 游泳状态下的移动优化：当玩家停止移动输入时自动退出游泳姿态。
 * <p>
 * 解决原版游泳松开方向键后仍然惯性前游、无法自然停下的问题。 仅客户端执行——检测本地输入后直接切换自身状态。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SwimmingImprovementsMechanic {

	public static void handle(Player player) {
		if (!ParkourConfig.enableStopSwimmingWhenIdle) {
			return;
		}

		if (player.level().isClientSide() && player.isLocalPlayer()) {
			handleClientLogic(player);
		}
	}

	private static void handleClientLogic(Player player) {
		if (!player.isSwimming()) {
			return;
		}

		boolean playerStopMove = !ParkourProxies.INPUT_PROXY.hasForwardImpulse(player);
		if (playerStopMove) {
			player.setSprinting(false);
			player.setSwimming(false);
		}
	}
}
