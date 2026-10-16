package mod.arcomit.parkour.content.behavior.wallrun.network;

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
 * 服务端向追踪者同步玩家的跑墙运动方向。
 * <p>
 * 进入跑墙状态时广播方向，并在新追踪者进入视野时补发，使远程玩家的动画朝向与运动方向一致。
 *
 * @param entityId  目标玩家的实体 ID，不可为负数
 * @param direction 跑墙运动方向，不可为 null
 * @author Mitok
 * @since 2026-06-08
 */
public record BroadcastWallRunDirS2CPayload(int entityId, Direction direction)
		implements CustomPacketPayload {
	public static final Type<BroadcastWallRunDirS2CPayload> TYPE =
			new Type<>(ParkourMod.prefix("broadcast_wall_run_dir_s2c"));

	public static final StreamCodec<FriendlyByteBuf, BroadcastWallRunDirS2CPayload>
			STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT,
			BroadcastWallRunDirS2CPayload::entityId, Direction.STREAM_CODEC,
			BroadcastWallRunDirS2CPayload::direction,
			BroadcastWallRunDirS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}


	/**
	 * 客户端接收处理：为远程玩家更新跑墙运动方向，保留本地玩家预测的方向。
	 */
	public static class Client {
		/**
		 * @param packet  包含目标实体 ID 和新方向，不可为 null
		 * @param context 网络上下文，不可为 null
		 */
		public static void handle(BroadcastWallRunDirS2CPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				ClientLevel level = Minecraft.getInstance().level;
				if (level != null) {
					Entity entity = level.getEntity(packet.entityId());
					// 只需给其他人(RemotePlayer)更新方向。LocalPlayer 自己在本地早就更新了。
					if (entity instanceof Player player && !player.isLocalPlayer()) {
						ParkourContext.get(player).wall()
								.setRunMove(packet.direction());
					}
				}
			});
		}
	}
}
