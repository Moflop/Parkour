package mod.arcomit.parkour.core.statemachine.network;

import mod.arcomit.parkour.content.init.ParkourRegistries;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 跑酷状态网络同步入口。
 *
 * <p>封装了跑酷状态机与 NeoForge 网络层之间的三种通信模式：
 * <ul>
 *   <li>客户端请求服务端执行状态转换（C-&gt;S）</li>
 *   <li>服务端广播状态变更给所有追踪该玩家的客户端（S-&gt;追踪者+自身）</li>
 *   <li>服务端单点纠正特定客户端的状态（S-&gt;单个客户端）</li>
 * </ul>
 *
 * <p>所有方法均通过注册表获取状态的 {@link ResourceLocation} 作为网络标识，
 * 若状态未注册则静默跳过（不发包）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourNetworkSynchronizer {

	/**
	 * 客户端请求服务端执行状态转换
	 */
	public static void requestServerTransition(IParkourState targetState, int animVariant) {
		ResourceLocation targetStateId =
				ParkourRegistries.PARKOUR_STATE_REGISTRY.getKey(targetState);
		if (targetStateId != null) {
			PacketDistributor.sendToServer(
					new RequestStateTransitionC2SPayload(targetStateId,
							animVariant));
		}
	}

	/**
	 * 服务端向所有追踪该玩家的客户端（包括自己）广播状态改变
	 */
	public static void broadcastStateChange(Player player, IParkourState targetState,
			int animVariant) {
		ResourceLocation targetStateId =
				ParkourRegistries.PARKOUR_STATE_REGISTRY.getKey(targetState);
		if (targetStateId != null) {
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
					new BroadcastStateChangeS2CPayload(player.getId(),
							targetStateId, animVariant));
		}
	}

	/**
	 * 服务端强制纠正特定客户端玩家的状态（用于回滚/重置）
	 */
	public static void forceLocalPlayerState(ServerPlayer serverPlayer,
			ResourceLocation stateId, int animVariant) {
		if (stateId != null) {
			PacketDistributor.sendToPlayer(serverPlayer,
					new ForceLocalPlayerStateS2CPayload(stateId, animVariant));
		}
	}
}
