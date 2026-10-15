package mod.arcomit.parkour.content.behavior.barswing.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** 向旁观玩家同步抓握支点；摆角从正常实体位置插值得到，不覆盖本地预测。 */
public record SyncBarSwingS2CPayload(int entityId, BlockPos bar, Direction.Axis axis, Vec3 anchor, Vec3 forward,
		java.util.UUID sourceId, Vec3 localAnchor, Vec3 localForward) implements CustomPacketPayload {
	public static final Type<SyncBarSwingS2CPayload> TYPE = new Type<>(ParkourMod.prefix("bar_swing_s2c"));
	public static final StreamCodec<FriendlyByteBuf, SyncBarSwingS2CPayload> STREAM_CODEC = StreamCodec.of(
			(buf, value) -> {
				buf.writeVarInt(value.entityId);
				buf.writeBlockPos(value.bar);
				buf.writeEnum(value.axis);
				buf.writeVec3(value.anchor);
				buf.writeVec3(value.forward);
				buf.writeBoolean(value.sourceId != null);
				if (value.sourceId != null) buf.writeUUID(value.sourceId);
				buf.writeVec3(value.localAnchor);
				buf.writeVec3(value.localForward);
			},
			buf -> new SyncBarSwingS2CPayload(buf.readVarInt(), buf.readBlockPos(), buf.readEnum(Direction.Axis.class),
					buf.readVec3(), buf.readVec3(), buf.readBoolean() ? buf.readUUID() : null, buf.readVec3(), buf.readVec3()));

	public static SyncBarSwingS2CPayload of(Player player) {
		var data = BarSwingData.get(player);
		return new SyncBarSwingS2CPayload(player.getId(), data.bar, data.axis, data.anchor, data.forward,
				data.sourceId, data.localAnchor, data.localForward);
	}

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(SyncBarSwingS2CPayload packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player().level().getEntity(packet.entityId) instanceof Player player && !player.isLocalPlayer()) {
				var data = BarSwingData.get(player);
				data.bar = packet.bar;
				data.anchor = packet.anchor;
				data.forward = packet.forward;
				data.axis = packet.axis;
				data.dimension = player.level().dimension();
				data.source = null;
				data.sourceId = packet.sourceId;
				data.localAnchor = packet.localAnchor;
				data.localForward = packet.localForward;
				data.down = new Vec3(0, -1, 0);
				data.barDirection = packet.axis == Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
				data.active = true;
			}
		});
	}
}
