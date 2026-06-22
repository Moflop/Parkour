package mod.arcomit.parkour.content.network;

import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.armhang.network.SyncArmhangDirC2SPayload;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-21
 */
public record SyncParkourEnabledC2SPayload(boolean isEnabled) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncParkourEnabledC2SPayload> TYPE =
			new CustomPacketPayload.Type<>(ParkourMod.prefix("toggle_parkour_c2s"));

	public static final StreamCodec<ByteBuf, SyncParkourEnabledC2SPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.BOOL, SyncParkourEnabledC2SPayload::isEnabled,
					SyncParkourEnabledC2SPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * 服务端收到包后的处理逻辑
	 */
	public static class Server {
		public static void handle(SyncParkourEnabledC2SPayload packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				Player player = context.player();
				if (player != null) {
					ParkourContext.get(player).state().setParkourEnabled(packet.isEnabled);
				}
			});
		}
	}
}
