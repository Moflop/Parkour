package mod.arcomit.parkour.content.behavior.landingroll.network;

import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.GroundMovementData;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端到服务端网络包：通知服务端设置落地翻滚窗口计时。
 * <p>
 * 由于是单向通知（无参数），使用{@code StreamCodec.unit}实现空载荷编解码。 服务端接收到后立即设置相同的窗口值，确保双方状态一致。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public record SetLandingRollWindowC2SPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SetLandingRollWindowC2SPayload> TYPE =
			new CustomPacketPayload.Type<>(
					ParkourMod.prefix("set_landing_roll_window_c2s"));

	public static final StreamCodec<ByteBuf, SetLandingRollWindowC2SPayload> STREAM_CODEC =
			StreamCodec.unit(new SetLandingRollWindowC2SPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * 服务端包处理器，在主线程工作队列中执行。
	 */
	public static class Server {
		/**
		 * 将收到的翻滚窗口值写入服务端玩家的地面上下文数据。
		 */
		public static void handle(SetLandingRollWindowC2SPayload payload,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					GroundMovementData groundMovementData =
							ParkourContext.get(player).ground();
					groundMovementData.setLandingRollWindow(
							ParkourConfig.landingRollWindow);
				}
			});
		}
	}
}
