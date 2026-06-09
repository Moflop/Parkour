package mod.arcomit.parkour.core.statemachine.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.init.ParkourRegistries;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 广播玩家状态改变给其他追踪该实体的客户端
 *
 * @author Mitok
 * @since 2026-06-08
 */
public record BroadcastStateChangeS2CPayload(int entityId, ResourceLocation stateId,
                                             int animVariant) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BroadcastStateChangeS2CPayload> TYPE =
			new CustomPacketPayload.Type<>(ParkourMod.prefix("broadcast_state_change"));

	public static final StreamCodec<FriendlyByteBuf, BroadcastStateChangeS2CPayload>
			STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT,
			BroadcastStateChangeS2CPayload::entityId, ResourceLocation.STREAM_CODEC,
			BroadcastStateChangeS2CPayload::stateId, ByteBufCodecs.INT,
			BroadcastStateChangeS2CPayload::animVariant,
			BroadcastStateChangeS2CPayload::new);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}


	public static class Client {
		/**
		 * 收到广播后将远程玩家的跑酷状态切换到服务端指定的状态。
		 *
		 * <p>通过实体 ID 在客户端世界查找目标玩家，跳过本地玩家（本地玩家通过预测自行切换），
		 * 仅处理其他远程玩家以保证其动画和姿势与当前服务端状态一致。
		 *
		 * @param packet  包含实体ID、目标状态ID和动画变体的广播包，不为 null
		 * @param context 网络上下文，用于切换到主线程执行
		 */
		public static void handle(BroadcastStateChangeS2CPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				ClientLevel level = Minecraft.getInstance().level;
				if (level != null) {
					Entity entity = level.getEntity(packet.entityId());
					// 本地玩家(LocalPlayer)已经通过预测切换了状态，只处理远程玩家(RemotePlayer)
					if (entity != null && entity instanceof Player player && !player.isLocalPlayer()) {
						IParkourState newState =
								ParkourRegistries.PARKOUR_STATE_REGISTRY.get(
										packet.stateId());
						if (newState != null) {
							ParkourStateMachine.transitionTo(player,
									newState,
									packet.animVariant());
						}
					}
				}
			});
		}
	}
}
