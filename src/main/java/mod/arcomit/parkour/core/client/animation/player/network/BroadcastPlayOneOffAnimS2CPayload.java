package mod.arcomit.parkour.core.client.animation.player.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.core.client.animation.player.PlayerAnimationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端广播动作动画的网络包（S2C）。
 * <p>
 * 当玩家执行一次性动作（如前空翻）时，服务端通过此包通知 周围所有客户端播放该动作。本地玩家自身已在发起时预测播放，不需要重复处理。
 *
 * @param entityId      执行动作的实体网络 ID
 * @param actionId      动作动画标识符，与注册的动画资源键对应
 * @param interruptible 是否可被后续状态动画打断
 * @param fadeTicks     淡入过渡时长（tick），0 表示立即触发无过渡
 * @author Mitok
 * @since 2026-06-08
 */
public record BroadcastPlayOneOffAnimS2CPayload(int entityId, Identifier actionId,
                                                boolean interruptible, int fadeTicks)
		implements CustomPacketPayload {
	public static final Type<BroadcastPlayOneOffAnimS2CPayload> TYPE =
			new Type<>(ParkourMod.prefix("broadcast_play_action"));

	public static final StreamCodec<FriendlyByteBuf, BroadcastPlayOneOffAnimS2CPayload>
			STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT,
			BroadcastPlayOneOffAnimS2CPayload::entityId, Identifier.STREAM_CODEC,
			BroadcastPlayOneOffAnimS2CPayload::actionId, ByteBufCodecs.BOOL,
			BroadcastPlayOneOffAnimS2CPayload::interruptible, ByteBufCodecs.INT,
			BroadcastPlayOneOffAnimS2CPayload::fadeTicks,
			BroadcastPlayOneOffAnimS2CPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * 客户端收包处理器。
	 * <p>
	 * 在客户端世界线程中查找目标实体；仅对非本地玩家的客户端玩家播放动作， 避免与本地预测播放重复调用。
	 */
	public static class Client {
		public static void handle(BroadcastPlayOneOffAnimS2CPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				ClientLevel level = Minecraft.getInstance().level;
				if (level != null) {
					Entity entity = level.getEntity(packet.entityId());
					if (entity instanceof AbstractClientPlayer player && !player.isLocalPlayer()) {
						PlayerAnimationManager.INSTANCE.playOneOffAnimation(
								player, packet.actionId(),
								packet.interruptible(),
								packet.fadeTicks());
					}
				}
			});
		}
	}
}
