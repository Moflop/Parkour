package mod.arcomit.parkour.content.behavior.armhang.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端向服务端同步本地悬挂方向。
 * <p>
 * 客户端在完成外角旋转、切换悬挂方向后发送此包。服务端收到后更新该玩家的 悬挂方向，并立即通过 {@link BroadcastArmhangDirS2CPayload} 广播给追踪者，
 * 保证其他玩家看到的该玩家悬挂姿态与实际一致。
 *
 * @param direction 新的悬挂方向，不可为 null
 * @author Mitok
 * @since 2026-06-08
 */
public record SyncArmhangDirC2SPayload(Direction direction) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncArmhangDirC2SPayload> TYPE =
			new CustomPacketPayload.Type<>(ParkourMod.prefix("sync_armhang_dir_c2s"));

	public static final StreamCodec<FriendlyByteBuf, SyncArmhangDirC2SPayload> STREAM_CODEC =
			StreamCodec.composite(Direction.STREAM_CODEC,
					SyncArmhangDirC2SPayload::direction,
					SyncArmhangDirC2SPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * 服务端接收处理：更新玩家悬挂方向，并向所有追踪者（含自己）广播新方向。
	 */
	public static class Server {
		/**
		 * @param packet  包含新悬挂方向，不可为 null
		 * @param context 网络上下文，发送者为服务端玩家，不可为 null
		 */
		public static void handle(SyncArmhangDirC2SPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					ParkourContext state = ParkourContext.get(player);
					state.wall().setArmhang(packet.direction);

					PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
							new BroadcastArmhangDirS2CPayload(
									player.getId(),
									packet.direction));
				}
			});
		}
	}
}
