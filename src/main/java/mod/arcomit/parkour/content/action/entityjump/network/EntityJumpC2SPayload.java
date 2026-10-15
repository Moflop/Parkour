package mod.arcomit.parkour.content.action.entityjump.network;

import io.netty.buffer.ByteBuf;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.entityjump.EntityJumpAction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** 实体借力跳请求；服务端自行验证距离、遮挡、配置和本次腾空的使用次数。 */
public record EntityJumpC2SPayload() implements CustomPacketPayload {
	public static final Type<EntityJumpC2SPayload> TYPE = new Type<>(ParkourMod.prefix("entity_jump_c2s"));
	public static final StreamCodec<ByteBuf, EntityJumpC2SPayload> STREAM_CODEC =
			StreamCodec.unit(new EntityJumpC2SPayload());

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static class Server {
		public static void handle(EntityJumpC2SPayload packet, IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player && EntityJumpAction.execute(player)
						&& player.connection != null) player.connection.resetPosition();
			});
		}
	}
}
