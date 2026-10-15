package mod.arcomit.parkour.content.behavior.barswing.client;

import com.mojang.blaze3d.platform.InputConstants;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingJump;
import mod.arcomit.parkour.content.behavior.barswing.network.BarSwingJumpC2SPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** 使用现有跳跃绑定的按下沿，兼容鼠标、键盘与改键；长按不会自动松手。 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientBarSwingInputHandler {
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onKey(InputEvent.Key event) {
		var mc = Minecraft.getInstance();
		if (event.getAction() == InputConstants.PRESS && mc.options.keyJump.matches(event.getKey(), event.getScanCode())) tryJump(mc);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMouse(InputEvent.MouseButton.Post event) {
		var mc = Minecraft.getInstance();
		if (event.getAction() == InputConstants.PRESS && mc.options.keyJump.matchesMouse(event.getButton())) tryJump(mc);
	}

	private static void tryJump(Minecraft mc) {
		if (mc.player == null || mc.screen != null || mc.isPaused() || !mc.isWindowActive() || mc.options.keyShift.isDown()) return;
		// 先发当前普通位置，服务端据此验证抓握位置和松手方向。
		if (BarSwingJump.execute(mc.player)) {
			mc.player.sendPosition();
			PacketDistributor.sendToServer(new BarSwingJumpC2SPayload());
		}
	}
}
