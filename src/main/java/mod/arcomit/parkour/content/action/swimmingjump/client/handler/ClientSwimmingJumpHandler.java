package mod.arcomit.parkour.content.action.swimmingjump.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.swimmingjump.SwimmingJumpAction;
import mod.arcomit.parkour.content.action.swimmingjump.network.UseSwimmingJumpC2SPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 游泳跳跃客户端输入处理器——每Tick检查并触发游泳跳跃， 将位置和事件同步到服务端。
 *
 * <p>仅对本地玩家生效，以LOWEST优先级处理。准入条件不满足时
 * {@link SwimmingJumpAction#execute} 会静默返回。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientSwimmingJumpHandler {

	/**
	 * 在玩家Tick结束时尝试触发游泳跳跃。
	 *
	 * @param event 玩家Tick后事件
	 * @sideeffect 执行游泳跳跃（修改玩家垂直速度）
	 * @sideeffect 发送客户端位置和网络包到服务端
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void trySwimmingJump(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (!(player instanceof LocalPlayer localPlayer)) {
			return;
		}

		if (SwimmingJumpAction.execute(localPlayer)) {
			localPlayer.sendPosition();
			PacketDistributor.sendToServer(new UseSwimmingJumpC2SPayload());
		}
	}
}
