package mod.arcomit.parkour.content.behavior.armhang.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangData;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangGrip;
import mod.arcomit.parkour.content.context.ParkourContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Server-authored palm contacts for observers; clients never choose arbitrary world anchors. */
public record SyncArmhangGripS2CPayload(int entityId, Direction direction, BlockPos block, UUID sourceId, float catchImpact,
		Vec3 center, Vec3 normal, Vec3 left, Vec3 right, Vec3 worldCenter, Vec3 worldNormal, Vec3 worldLeft, Vec3 worldRight)
		implements CustomPacketPayload {
	public static final Type<SyncArmhangGripS2CPayload> TYPE = new Type<>(ParkourMod.prefix("armhang_grip_s2c"));
	public static final StreamCodec<FriendlyByteBuf, SyncArmhangGripS2CPayload> STREAM_CODEC = StreamCodec.of((buf, value) -> {
		buf.writeVarInt(value.entityId);
		buf.writeEnum(value.direction);
		buf.writeBlockPos(value.block);
		buf.writeBoolean(value.sourceId != null);
		if (value.sourceId != null) buf.writeUUID(value.sourceId);
		buf.writeFloat(value.catchImpact);
		buf.writeVec3(value.center); buf.writeVec3(value.normal); buf.writeVec3(value.left); buf.writeVec3(value.right);
		buf.writeVec3(value.worldCenter); buf.writeVec3(value.worldNormal); buf.writeVec3(value.worldLeft); buf.writeVec3(value.worldRight);
	}, buf -> new SyncArmhangGripS2CPayload(buf.readVarInt(), buf.readEnum(Direction.class), buf.readBlockPos(),
			buf.readBoolean() ? buf.readUUID() : null, buf.readFloat(), buf.readVec3(), buf.readVec3(), buf.readVec3(), buf.readVec3(),
			buf.readVec3(), buf.readVec3(), buf.readVec3(), buf.readVec3()));

	public static SyncArmhangGripS2CPayload of(Player player) {
		var grip = ArmhangData.get(player).grip;
		Vec3 center = grip.logical(grip.localCenter());
		return new SyncArmhangGripS2CPayload(player.getId(), ParkourContext.get(player).wall().getArmhang(), grip.block(),
				grip.source() == null ? null : grip.source().networkId(), (float) ArmhangData.get(player).catchImpact, grip.localCenter(), grip.localNormal(),
				grip.localLeftHand(), grip.localRightHand(), center,
				grip.logical(grip.localCenter().add(grip.localNormal())).subtract(center).normalize(),
				grip.logical(grip.localLeftHand()), grip.logical(grip.localRightHand()));
	}

	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

	public static void handle(SyncArmhangGripS2CPayload packet, IPayloadContext context) {
		if (!packet.direction.getAxis().isHorizontal()) return;
		context.enqueueWork(() -> {
			if (context.player().level().getEntity(packet.entityId) instanceof Player player && !player.isLocalPlayer()) {
				var data = ArmhangData.get(player);
				data.catchImpact = packet.catchImpact;
				ParkourContext.get(player).wall().setArmhang(packet.direction);
				ArmhangGrip old = data.grip;
				data.pendingLocalGrip = new ArmhangGrip(packet.block, null, packet.center, packet.normal, packet.left, packet.right);
				data.grip = new ArmhangGrip(packet.block, null, packet.worldCenter, packet.worldNormal, packet.worldLeft, packet.worldRight);
				data.pendingSourceId = packet.sourceId;
				data.active = true;
				data.resolveSource(player);
				ArmhangGrip next = data.grip;
				data.grip = old;
				data.acceptGrip(player, next);
			}
		});
	}
}
