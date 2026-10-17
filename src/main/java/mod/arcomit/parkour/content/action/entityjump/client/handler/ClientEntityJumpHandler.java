package mod.arcomit.parkour.content.action.entityjump.client.handler;

import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonAction;
import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonParkourRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.entityjump.EntityJumpAction;
import mod.arcomit.parkour.content.action.entityjump.network.EntityJumpC2SPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** 原版跳跃绑定的单次按下触发借力，兼容键盘、扫描码和鼠标。 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientEntityJumpHandler {

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onKeyAction(InputEvent.Key event) {
		Minecraft mc = Minecraft.getInstance();
		if (event.getAction() == InputConstants.PRESS
				&& mc.options.keyJump.matches(event.getKey(), event.getScanCode())) tryJump(mc);
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onMouseAction(InputEvent.MouseButton.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (event.getAction() == InputConstants.PRESS
				&& mc.options.keyJump.matchesMouse(event.getButton())) tryJump(mc);
	}

	private static void tryJump(Minecraft mc) {
		var player = mc.player;
		if (player == null || mc.screen != null || mc.isPaused() || !mc.isWindowActive()
				|| mc.options.keyShift.isDown()) return;
		if (EntityJumpAction.execute(player)) {
			FirstPersonParkourRenderer.play(FirstPersonAction.ENTITY_JUMP);
			player.sendPosition();
			PacketDistributor.sendToServer(new EntityJumpC2SPayload());
		}
	}
}
