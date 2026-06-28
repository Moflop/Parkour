package mod.arcomit.parkour.core.client.animation.player.network;

import mod.arcomit.parkour.ParkourMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端请求播放动作动画的网络包（C2S）。
 * <p>
 * 本地玩家在预测执行动作后，通过此包通知服务端。服务端接收后将其广播给 周围所有客户端，使其他玩家也能看到该动作。
 *
 * @param actionAnimId 动作动画标识符
 * @author Mitok
 * @since 2026-06-08
 */
public record RequestPlayActionAnimC2SPayload(Identifier actionAnimId)
		implements CustomPacketPayload {
	public static final Type<RequestPlayActionAnimC2SPayload> TYPE =
			new Type<>(ParkourMod.prefix("request_play_action_anim_c2s"));

	public static final StreamCodec<FriendlyByteBuf, RequestPlayActionAnimC2SPayload>
			STREAM_CODEC = StreamCodec.composite(Identifier.STREAM_CODEC,
			RequestPlayActionAnimC2SPayload::actionAnimId,
			RequestPlayActionAnimC2SPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * 服务端收包处理器。
	 * <p>
	 * 将请求封装为 {@link BroadcastPlayActionAnimS2CPayload}， 通过
	 * {@code sendToPlayersTrackingEntityAndSelf} 广播给 追踪该玩家的所有客户端（含发送者自己）。
	 */
	public static class Server {
		public static void handle(RequestPlayActionAnimC2SPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer sender) {
					PacketDistributor.sendToPlayersTrackingEntityAndSelf(sender,
							new BroadcastPlayActionAnimS2CPayload(
									sender.getId(),
									packet.actionAnimId()));
				}
			});
		}
	}
}
