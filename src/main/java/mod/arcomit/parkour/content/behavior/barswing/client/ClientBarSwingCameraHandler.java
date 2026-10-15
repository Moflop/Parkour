package mod.arcomit.parkour.content.behavior.barswing.client;

import mod.arcomit.parkour.ClientParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingData;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingState;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** 轻微、可关闭的摆荡俯仰，进入与松手均平滑过渡，不改变玩家瞄准角。 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientBarSwingCameraHandler {
	private static float pitch, previousPitch;

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		var mc = Minecraft.getInstance();
		if (mc.player == null) { pitch = previousPitch = 0; return; }
		if (mc.isPaused()) return;
		previousPitch = pitch;
		float target = ClientParkourConfig.barSwingCamera
				&& ParkourContext.get(mc.player).state().getState() instanceof BarSwingState
				? (float) -BarSwingData.get(mc.player).angle * 5 : 0;
		pitch = Mth.lerp(0.3F, pitch, target);
	}

	@SubscribeEvent
	public static void camera(ViewportEvent.ComputeCameraAngles event) {
		var mc = Minecraft.getInstance();
		if (mc.player != null && ClientParkourConfig.barSwingCamera && mc.options.getCameraType().isFirstPerson()) {
			event.setPitch(event.getPitch() + Mth.lerp((float) event.getPartialTick(), previousPitch, pitch));
		}
	}
}
