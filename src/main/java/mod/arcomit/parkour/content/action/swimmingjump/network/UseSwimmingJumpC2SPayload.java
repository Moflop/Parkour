package mod.arcomit.parkour.content.action.swimmingjump.network;

import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.swimmingjump.SwimmingJumpAction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 游泳跳跃客户端->服务端网络包——客户端执行游泳跳跃后通知服务端同步执行。
 *
 * <p>无载荷事件信号包，使用 {@code StreamCodec.unit} 编码。
 * 协议标识为 {@code "freestyle_jump"}。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public record UseSwimmingJumpC2SPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UseSwimmingJumpC2SPayload> TYPE =
			new CustomPacketPayload.Type<>(ParkourMod.prefix("freestyle_jump"));
	public static final StreamCodec<ByteBuf, UseSwimmingJumpC2SPayload> STREAM_CODEC =
			StreamCodec.unit(new UseSwimmingJumpC2SPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}


	/**
	 * 服务端包处理器——在服务端线程重放游泳跳跃逻辑并重置连接位置
	 * 以避免反作弊检测触发回拉。
	 */
	public static class Server {

		/**
		 * 在服务端主线程上排队执行游泳跳跃。
		 *
		 * @param packet 收到的客户端包
		 * @param context 网络上下文，用于获取玩家
		 * @sideeffect 执行游泳跳跃（服务端）
		 * @sideeffect 重置玩家连接位置记录（防反作弊误判）
		 */
		public static void handle(UseSwimmingJumpC2SPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					SwimmingJumpAction.execute(player);
					// 避免触发服务端反作弊回拉
					if (player.connection != null) {
						player.connection.resetPosition();
					}
				}
			});
		}
	}
}
