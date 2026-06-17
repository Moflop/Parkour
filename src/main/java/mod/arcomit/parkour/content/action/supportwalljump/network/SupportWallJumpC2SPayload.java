package mod.arcomit.parkour.content.action.supportwalljump.network;

import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.supportwalljump.SupportWallJumpAction;
// import mod.arcomit.nimblesteps.event.skills.SupportWallJumpHandler;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 支撑蹬墙跳客户端->服务端网络包——客户端执行支撑蹬墙跳后通知服务端同步执行。
 *
 * <p>无载荷事件信号包，使用 {@code StreamCodec.unit} 编码。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public record SupportWallJumpC2SPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SupportWallJumpC2SPayload> TYPE =
			new CustomPacketPayload.Type<>(ParkourMod.prefix("support_wall_jump"));
	public static final StreamCodec<ByteBuf, SupportWallJumpC2SPayload> STREAM_CODEC =
			StreamCodec.unit(new SupportWallJumpC2SPayload());

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}


	/**
	 * 服务端包处理器——在服务端线程执行支撑蹬墙跳并重置连接位置 以避免反作弊检测触发回拉。
	 */
	public static class Server {

		/**
		 * 在服务端主线程上排队执行支撑蹬墙跳。
		 *
		 * @param packet  收到的客户端包
		 * @param context 网络上下文，用于获取玩家
		 * @sideeffect 执行支撑蹬墙跳动作（服务端）
		 * @sideeffect 重置玩家连接位置记录（防反作弊误判）
		 */
		public static void handle(SupportWallJumpC2SPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					if (SupportWallJumpAction.execute(player)) {
						if (player.connection != null) {
							player.connection.resetPosition();
						}
					}
				}
			});
		}
	}
}
