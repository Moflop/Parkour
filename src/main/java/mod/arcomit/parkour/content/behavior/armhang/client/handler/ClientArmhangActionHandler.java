package mod.arcomit.parkour.content.behavior.armhang.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.content.action.supportwalljump.SupportWallJumpAction;
import mod.arcomit.parkour.content.action.supportwalljump.network.SupportWallJumpC2SPayload;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangData;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Current key binding, including mouse bindings; release the approach jump before choosing an exit. */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public final class ClientArmhangActionHandler {
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void tick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof LocalPlayer player)) return;
		var mc = Minecraft.getInstance();
		var context = ParkourContext.get(player);
		if (!(context.state().getState() instanceof ArmhangState) || mc.screen != null || mc.isPaused()) return;
		var data = ArmhangData.get(player);
		if (!data.pressJump(mc.options.keyJump.isDown()) || mc.options.keyShift.isDown()) return;
		var direction = context.wall().getArmhang();
		if (direction == null) return;
		if (Directions.isFacing(player, DynamicSurfaceMotion.wallNormal(player, direction), 75)) {
			if (!ParkourStates.ARMHANG_MANTLE.get().canEnter(player, context)) return;
			player.sendPosition();
			ParkourStateMachine.localTransitionTo(player, ParkourStates.ARMHANG_MANTLE.get());
		} else if (SupportWallJumpAction.execute(player)) {
			player.sendPosition();
			PacketDistributor.sendToServer(new SupportWallJumpC2SPayload());
		}
	}
}
