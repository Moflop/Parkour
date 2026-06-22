package mod.arcomit.parkour.content.client.handler;

import mod.arcomit.parkour.ClientParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.client.init.ParkourKeyBindings;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.network.SyncParkourEnabledC2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-21
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientSyncParkourEnabledHandler {

	@SubscribeEvent
	public static void onKeyPressed(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		while (ParkourKeyBindings.ENABLE_PARKOUR_KEY.consumeClick()) {
			boolean newState = ClientParkourConfig.toggleParkour();

			ParkourContext.get(mc.player).state().setParkourEnabled(newState);
			ClientPacketDistributor.sendToServer(new SyncParkourEnabledC2SPayload(newState));

			String translationKey = newState ? "message.parkour.enabled" : "message.parkour.disabled";
			Minecraft.getInstance().gui.setOverlayMessage(Component.translatable(translationKey), false);
		}
	}

	@SubscribeEvent
	public static void onLocalPlayerJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() && event.getEntity() instanceof LocalPlayer player) {
			boolean isEnabled = ClientParkourConfig.enableParkour;
			ClientPacketDistributor.sendToServer(new SyncParkourEnabledC2SPayload(isEnabled));
		}
	}
}
