package mod.arcomit.parkour.core.statemachine.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.client.event.InputJustPressedEvent;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 客户端输入事件转发器，将本地玩家的按键按下事件桥接到状态机的输入转换系统。
 *
 * <p>监听自定义的 {@link InputJustPressedEvent}（刚按下的瞬间触发，非持续按住），
 * 从中提取按键对应的 {@link ParkourInputActions} 动作枚举，转发给状态机评估输入驱动的状态转换。
 *
 * <p>仅在客户端注册（{@code Dist.CLIENT}），通过 {@link OnlyIn} 和 {@code @EventBusSubscriber} 双重保证。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientParkourEventForwarderHandler {

	/**
	 * 本地玩家按下跑酷动作键时触发，从事件中提取输入动作并转发给状态机。
	 *
	 * <p>通过 {@code Minecraft.getInstance().player} 获取本地玩家，若为 null（如尚未进入世界）则静默跳过。
	 * 匹配的输入转换将执行本地预测切换并请求服务端校验。
	 *
	 * <p>副作用：可能触发状态切换和网络发包。
	 *
	 * @param event 按键刚按下事件，包含触发按键的输入动作映射，不为 null
	 */
	@SubscribeEvent
	public static void onKeyJustPressed(InputJustPressedEvent event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;
		ParkourContext context = ParkourContext.get(player);
		ParkourStateMachine.tryInputTransition(player, context,
				event.getKeyMapping().getInputAction());
	}
}
