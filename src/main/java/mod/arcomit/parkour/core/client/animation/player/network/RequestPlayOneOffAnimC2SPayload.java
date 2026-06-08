package mod.arcomit.parkour.core.client.animation.player.network;

import mod.arcomit.parkour.ParkourMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端请求播放动作动画的网络包（C2S）。
 * <p>
 * 本地玩家在预测执行动作后，通过此包通知服务端。服务端接收后将其广播给
 * 周围所有客户端，使其他玩家也能看到该动作。
 *
 * @param actionId      动作动画标识符
 * @param interruptible 是否可被后续状态动画打断
 * @param fadeTicks     淡入过渡时长（tick），0 表示立即触发
 * @author Mitok
 * @since 2026-06-08
 */
public record RequestPlayOneOffAnimC2SPayload(ResourceLocation actionId, boolean interruptible,
                                              int fadeTicks) implements CustomPacketPayload {
	public static final Type<RequestPlayOneOffAnimC2SPayload> TYPE =
			new Type<>(ParkourMod.prefix("request_play_action"));

	public static final StreamCodec<FriendlyByteBuf, RequestPlayOneOffAnimC2SPayload>
			STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC,
			RequestPlayOneOffAnimC2SPayload::actionId, ByteBufCodecs.BOOL,
			RequestPlayOneOffAnimC2SPayload::interruptible, ByteBufCodecs.INT,
			RequestPlayOneOffAnimC2SPayload::fadeTicks,
			RequestPlayOneOffAnimC2SPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * 服务端收包处理器。
	 * <p>
	 * 将请求封装为 {@link BroadcastPlayOneOffAnimS2CPayload}，
	 * 通过 {@code sendToPlayersTrackingEntityAndSelf} 广播给
	 * 追踪该玩家的所有客户端（含发送者自己）。
	 */
	public static class Server {
		public static void handle(RequestPlayOneOffAnimC2SPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer sender) {
					PacketDistributor.sendToPlayersTrackingEntityAndSelf(sender,
							new BroadcastPlayOneOffAnimS2CPayload(
									sender.getId(),
									packet.actionId(),
									packet.interruptible(),
									packet.fadeTicks()));
				}
			});
		}
	}
}
