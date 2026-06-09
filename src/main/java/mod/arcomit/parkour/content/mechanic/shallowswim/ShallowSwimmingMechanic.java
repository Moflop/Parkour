package mod.arcomit.parkour.content.mechanic.shallowswim;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * 允许玩家在仅有一格深的浅水中触发游泳姿态。
 * <p>
 * 进入水源后若有疾跑意图，自动从疾跑切换为游泳，使浅水区域的跑酷过渡更流畅。 客户端负责检测输入并触发，服务端负责同步游泳状态。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ShallowSwimmingMechanic {

	public static void handle(Player player) {
		ParkourContext state = ParkourContext.get(player);
		if (!canShallowSwim(player, state)) {
			return;
		}

		if (player.level().isClientSide()) {
			if (player.isLocalPlayer()) {
				handleLocalShallowSwimming(player);
			}
		} else {
			handleServerShallowSwimming(player);
		}
	}

	private static void handleLocalShallowSwimming(Player localPlayer) {
		if (localPlayer.isSwimming()) {
			return;
		}
		boolean wantsSprint =
				Minecraft.getInstance().options.keySprint.isDown() || localPlayer.isSprinting();
		if (wantsSprint && ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.hasEnoughFoodToStartSprinting(
				localPlayer) && ParkourProxies.INPUT_PROXY.hasForwardImpulse(
				localPlayer)) {
			localPlayer.setSprinting(true);
			localPlayer.setSwimming(true);
		}
	}

	private static void handleServerShallowSwimming(Player player) {
		player.setSwimming(player.isSprinting());
	}

	private static boolean canShallowSwim(Player player, ParkourContext state) {
		return ParkourConfig.enableShallowSwimming && player.isInWater() && ParkourChecks.canPerformAction(
				player);
	}
}
