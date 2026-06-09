package mod.arcomit.parkour.core.statemachine.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.init.ParkourRegistries;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 拒绝客户端的状态转换请求，并通知其回滚到当前合法状态
 *
 * @author Mitok
 * @since 2026-06-08
 */
public record ForceLocalPlayerStateS2CPayload(ResourceLocation correctStateId, int animVariant)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ForceLocalPlayerStateS2CPayload> TYPE =
			new CustomPacketPayload.Type<>(ParkourMod.prefix("sync_state_to_local"));

	public static final StreamCodec<FriendlyByteBuf, ForceLocalPlayerStateS2CPayload>
			STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC,
			ForceLocalPlayerStateS2CPayload::correctStateId, ByteBufCodecs.INT,
			ForceLocalPlayerStateS2CPayload::animVariant,
			ForceLocalPlayerStateS2CPayload::new);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}


	public static class Client {
		/**
		 * 服务端强制回滚本地玩家到指定的合法状态。
		 *
		 * <p>当客户端发起的预测性状态转换被服务端校验拒绝后，服务端通过此包告知
		 * 客户端当前合法的状态，客户端无条件执行切换以消除两端不一致。
		 *
		 * @param packet  包含服务端认定的合法状态ID和动画变体，不为 null
		 * @param context 网络上下文，用于切换到主线程执行
		 */
		public static void handle(ForceLocalPlayerStateS2CPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				Player player = Minecraft.getInstance().player;
				if (player != null) {
					IParkourState correctState =
							ParkourRegistries.PARKOUR_STATE_REGISTRY.get(
									packet.correctStateId());
					if (correctState != null) {
						ParkourStateMachine.transitionTo(player,
								correctState, packet.animVariant());
					}
				}
			});
		}
	}
}
