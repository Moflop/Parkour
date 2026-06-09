package mod.arcomit.parkour.content.behavior.wallslide.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C 网络包 —— 将玩家的滑墙方向变更广播给所有追踪该玩家的客户端。
 *
 * <p>携带实体 ID 和新的墙体方向。客户端收到后仅对非本地玩家（RemotePlayer）
 * 更新滑墙方向缓存，因为本地玩家已在本地自行更新。
 *
 * @param entityId  发生滑墙方向变更的玩家实体 ID
 * @param direction 新的滑墙方向（水平方向），不能为 null
 * @author Mitok
 * @since 2026-06-08
 */
public record BroadcastWallSlideDirS2CPayload(int entityId, Direction direction)
		implements CustomPacketPayload {
	public static final Type<BroadcastWallSlideDirS2CPayload> TYPE =
			new Type<>(ParkourMod.prefix("broadcast_wall_slide_dir_s2c"));

	public static final StreamCodec<FriendlyByteBuf, BroadcastWallSlideDirS2CPayload>
			STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT,
			BroadcastWallSlideDirS2CPayload::entityId, Direction.STREAM_CODEC,
			BroadcastWallSlideDirS2CPayload::direction,
			BroadcastWallSlideDirS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * 客户端侧数据包处理器。
	 */
	public static class Client {
		/**
		 * 在主线程将接收到的滑墙方向写入对应 RemotePlayer 的墙体数据中， 使第三方玩家能看到正确的滑墙动画朝向。
		 */
		public static void handle(BroadcastWallSlideDirS2CPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				ClientLevel level = Minecraft.getInstance().level;
				if (level != null) {
					Entity entity = level.getEntity(packet.entityId());
					// 只需给其他人(RemotePlayer)更新方向。LocalPlayer 自己在本地早就更新了。
					if (entity instanceof Player player && !player.isLocalPlayer()) {
						ParkourContext.get(player).wall()
								.setSlide(packet.direction());
					}
				}
			});
		}
	}
}
