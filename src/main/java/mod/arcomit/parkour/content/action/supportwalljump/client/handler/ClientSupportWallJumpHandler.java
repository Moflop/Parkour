package mod.arcomit.parkour.content.action.supportwalljump.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.supportwalljump.SupportWallJumpAction;
import mod.arcomit.parkour.content.action.supportwalljump.network.SupportWallJumpC2SPayload;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
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
 * 支撑蹬墙跳客户端输入处理器——每Tick检查跳跃键状态， 若玩家按住跳跃键则触发支撑蹬墙跳。
 *
 * <p>仅在客户端注册，以LOWEST优先级处理，确保其他Tick逻辑先执行。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientSupportWallJumpHandler {

	/**
	 * 在玩家Tick结束时检查跳跃键状态并尝试触发支撑蹬墙跳。
	 *
	 * <p>只对本地玩家生效。若支撑蹬墙跳准入条件不满足，{@link SupportWallJumpAction#execute}
	 * 会静默返回。</p>
	 *
	 * @param event 玩家Tick后事件
	 * @sideeffect 执行支撑蹬墙跳动作
	 * @sideeffect 发送客户端位置和网络包到服务端
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void trySwimmingJump(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (!(player instanceof LocalPlayer localPlayer)) {
			return;
		}
		if (!ParkourProxies.INPUT_PROXY.getJumping(player)) {
			return;
		}

		SupportWallJumpAction.execute(localPlayer);
		localPlayer.sendPosition();
		PacketDistributor.sendToServer(new SupportWallJumpC2SPayload());
	}
}
