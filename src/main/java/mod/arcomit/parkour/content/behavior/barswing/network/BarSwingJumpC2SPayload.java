package mod.arcomit.parkour.content.behavior.barswing.network;

import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingJump;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** 仅发送起跳意图，不接受客户端提供的锚点或速度。 */
public record BarSwingJumpC2SPayload() implements CustomPacketPayload {
	public static final Type<BarSwingJumpC2SPayload> TYPE = new Type<>(ParkourMod.prefix("bar_swing_jump_c2s"));
	public static final StreamCodec<ByteBuf, BarSwingJumpC2SPayload> STREAM_CODEC = StreamCodec.unit(new BarSwingJumpC2SPayload());

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(BarSwingJumpC2SPayload packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player && BarSwingJump.execute(player)) {
				player.connection.resetPosition();
			}
		});
	}
}
