package mod.arcomit.parkour.content.mechanic.fallheight.handler;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * 比牢大更能抗摔处理器。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class SafeFallHeightIncreaseHandler {
	private static final float VANILLA_SAFE_FALL_DISTANCE = 3.0f; // 原版玩家安全落地的距离

	@SubscribeEvent
	public static void adjustFallDistanceForSafeFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}

		if (!ParkourConfig.enableSafeFallHeightIncrease) {
			return;
		}

		if (ParkourChecks.isVanillaState(ParkourContext.get(player))) {
			return;
		}

		float distance = (float) event.getDistance();
		float adjustment =
				(float) ParkourConfig.safeFallHeight - VANILLA_SAFE_FALL_DISTANCE;
		event.setDistance(distance - adjustment);
	}
}
