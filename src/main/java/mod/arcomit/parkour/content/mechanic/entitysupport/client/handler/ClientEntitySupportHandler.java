package mod.arcomit.parkour.content.mechanic.entitysupport.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 补齐本刻中晚于玩家更新的实体位移，避免站在移动实体上时始终落后一帧。 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientEntitySupportHandler {

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		var player = mc.player;
		if (player == null || mc.isPaused()) return;
		var before = player.position();
		var support = ParkourContext.get(player).ground().getEntitySupport();
		support.beforeTravel(player);
		support.afterTravel(player);
		if (!player.position().equals(before)) player.sendPosition();
	}
}
