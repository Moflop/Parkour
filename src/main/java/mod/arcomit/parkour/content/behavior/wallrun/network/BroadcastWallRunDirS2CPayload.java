package mod.arcomit.parkour.content.behavior.wallrun.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.armhang.network.SyncArmhangDirC2SPayload;
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
 * 服务端广播某个玩家的悬挂方向给所有追踪该玩家的客户端。
 * <p>
 * 当服务端收到客户端发来的悬挂方向同步 （{@link SyncArmhangDirC2SPayload}）后，通过此包将方向变化广播给 周围的其他玩家，使远程玩家的动画姿态与悬挂方向一致。
 *
 * @param entityId  目标玩家的实体 ID，不可为负数
 * @param direction 新的悬挂方向，不可为 null
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
	 * 客户端接收处理：为非本地玩家的远程实体更新悬挂方向， 使第三人称视角下其他玩家的悬挂动画朝向正确。
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
