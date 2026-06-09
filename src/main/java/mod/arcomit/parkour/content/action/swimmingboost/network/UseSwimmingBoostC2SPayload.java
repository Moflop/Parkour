package mod.arcomit.parkour.content.action.swimmingboost.network;

import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.swimmingboost.SwimmingBoostAction;
import mod.arcomit.parkour.content.action.swimmingboost.SwimmingBoostSound;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.SwimMovementData;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 游泳加速客户端->服务端网络包——客户端执行加速后通知服务端同步执行， 并在服务端播放音效。
 *
 * <p>无载荷事件信号包，使用 {@code StreamCodec.unit} 编码。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public record UseSwimmingBoostC2SPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UseSwimmingBoostC2SPayload> TYPE =
			new CustomPacketPayload.Type<>(ParkourMod.prefix("swimming_boost"));
	public static final StreamCodec<ByteBuf, UseSwimmingBoostC2SPayload> STREAM_CODEC =
			StreamCodec.unit(new UseSwimmingBoostC2SPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}


	/**
	 * 服务端包处理器——在服务端线程重放游泳加速逻辑并播放音效， 最后重置连接位置以避免反作弊回拉。
	 */
	public static class Server {

		/**
		 * 在服务端主线程上排队执行游泳加速。
		 *
		 * @param packet  收到的客户端包
		 * @param context 网络上下文，用于获取玩家
		 * @sideeffect 执行游泳加速（服务端）
		 * @sideeffect 成功时在服务端播放音效
		 * @sideeffect 重置玩家连接位置记录（防反作弊误判）
		 */
		public static void handle(UseSwimmingBoostC2SPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					SwimMovementData swimData =
							ParkourContext.get(player).swim();
					if (SwimmingBoostAction.execute(player, swimData)) {
						SwimmingBoostSound.play(player);
					}

					// 避免触发服务端反作弊回拉
					if (player.connection != null) {
						player.connection.resetPosition();
					}
				}
			});
		}
	}
}
