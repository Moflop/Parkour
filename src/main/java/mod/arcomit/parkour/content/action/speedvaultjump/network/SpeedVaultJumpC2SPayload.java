package mod.arcomit.parkour.content.action.speedvaultjump.network;

import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.speedvaultjump.SpeedVaultJumpAction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 速过跳跃客户端->服务端网络包——客户端完成本地预测后，请求服务端校验并执行相同动作。
 *
 * <p>无载荷事件信号包，使用 {@code StreamCodec.unit} 编码。</p>
 *
 * @author Mitok
 * @since 2026-10-14
 */
public record SpeedVaultJumpC2SPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpeedVaultJumpC2SPayload> TYPE =
			new CustomPacketPayload.Type<>(ParkourMod.prefix("speed_vault_jump_c2s"));
	public static final StreamCodec<ByteBuf, SpeedVaultJumpC2SPayload> STREAM_CODEC =
			StreamCodec.unit(new SpeedVaultJumpC2SPayload());

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * 服务端包处理器——校验速过跳跃资格，成功后重置连接位置记录，避免回拉。
	 */
	public static class Server {

		/**
		 * 在服务端主线程执行速过跳跃。重复请求或已过期的跳跃窗口不会生效。
		 *
		 * @param packet  收到的客户端包
		 * @param context 网络上下文，用于获取玩家
		 */
		public static void handle(SpeedVaultJumpC2SPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					if (SpeedVaultJumpAction.execute(player)) {
						if (player.connection != null) {
							player.connection.resetPosition();
						}
					}
				}
			});
		}
	}
}
