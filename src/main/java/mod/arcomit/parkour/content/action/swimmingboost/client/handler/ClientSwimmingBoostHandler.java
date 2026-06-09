package mod.arcomit.parkour.content.action.swimmingboost.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.swimmingboost.SwimmingBoostAction;
import mod.arcomit.parkour.content.action.swimmingboost.client.ClientSwimmingBoostSound;
import mod.arcomit.parkour.content.action.swimmingboost.network.UseSwimmingBoostC2SPayload;
import mod.arcomit.parkour.content.client.event.InputJustPressedEvent;
import mod.arcomit.parkour.content.client.input.ParkourKeyBindings;
import mod.arcomit.parkour.content.client.input.ParkourKeyMapping;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.SwimMovementData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 游泳加速客户端输入处理器——监听自定义"滑行键"按下事件， 在客户端触发游泳加速并将结果同步到服务端。
 *
 * <p>推进成功后播放客户端侧音效；无论成功与否均发送位置和网络包到服务端。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientSwimmingBoostHandler {

	/**
	 * 响应滑行键按下事件，执行游泳加速。
	 *
	 * <p>只处理 {@code SLIDE_KEY} 绑定的按键，忽略其他按键。</p>
	 *
	 * @param event 按键刚按下事件，包含按下的键映射
	 * @sideeffect 执行游泳加速（修改玩家速度）
	 * @sideeffect 成功时播放客户端音效
	 * @sideeffect 发送客户端位置和网络包到服务端
	 */
	@SubscribeEvent
	public static void trySwimmingBoostOnInput(InputJustPressedEvent event) {
		ParkourKeyMapping key = event.getKeyMapping();
		if (key != ParkourKeyBindings.SLIDE_KEY) {
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		SwimMovementData swimData = ParkourContext.get(player).swim();
		if (SwimmingBoostAction.execute(player, swimData)) {
			ClientSwimmingBoostSound.play(player);
		}

		player.sendPosition();
		PacketDistributor.sendToServer(new UseSwimmingBoostC2SPayload());
	}
}
