package mod.arcomit.parkour.content.behavior.landingroll.client.handler;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.landingroll.network.SetLandingRollWindowC2SPayload;
import mod.arcomit.parkour.content.client.event.InputJustPressedEvent;
import mod.arcomit.parkour.content.client.init.ParkourKeyBindings;
import mod.arcomit.parkour.content.client.input.ParkourKeyMapping;
import mod.arcomit.parkour.content.context.GroundMovementData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 客户端落地翻滚窗口设置器 —— 监听滑铲键按下事件，当玩家处于危险坠落高度时 激活翻滚窗口计时器，并同步到服务端。
 * <p>
 * 翻滚窗口是一个倒计时：空中按下滑铲键后，若在窗口耗尽前落地则触发落地翻滚。 窗口已激活时重复按键不会重置计时。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientLandingRollHandler {

	/**
	 * 当玩家在空中且按下的是滑铲键时，启动落地翻滚窗口并通知服务端。
	 * <p>
	 * 副作用：客户端本地设置窗口计时，同时发送网络包到服务端同步状态。
	 */
	@SubscribeEvent
	public static void trySetLandingRollWindowOnInput(InputJustPressedEvent event) {
		ParkourKeyMapping key = event.getKeyMapping();
		if (key != ParkourKeyBindings.SLIDE_KEY) {
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !ParkourChecks.isFallUnsafe(player)) {
			return;
		}

		GroundMovementData groundMovementData = ParkourContext.get(player).ground();
		if (groundMovementData.getLandingRollWindow() > 0) {
			return;
		}

		groundMovementData.setLandingRollWindow(ParkourConfig.landingRollWindow);
		ClientPacketDistributor.sendToServer(new SetLandingRollWindowC2SPayload());
	}
}
