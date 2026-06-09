package mod.arcomit.parkour.core.statemachine.network;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.init.ParkourRegistries;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端请求状态转换的网络包（C-&gt;S）。
 *
 * <p>本地玩家在客户端预测性地切换跑酷状态后，通过此包向服务端发起同步请求。
 * 服务端收到后调用目标状态的 {@link IParkourState#canEnter} 进行权威校验：
 * 合法则执行转换并广播；不合法则通过{@link ForceLocalPlayerStateS2CPayload}强制回滚。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public record RequestStateTransitionC2SPayload(Identifier targetStateId, int animVariant)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RequestStateTransitionC2SPayload> TYPE =
			new CustomPacketPayload.Type<>(
					ParkourMod.prefix("request_state_transition"));

	public static final StreamCodec<FriendlyByteBuf, RequestStateTransitionC2SPayload>
			STREAM_CODEC = StreamCodec.composite(Identifier.STREAM_CODEC,
			RequestStateTransitionC2SPayload::targetStateId, ByteBufCodecs.INT,
			RequestStateTransitionC2SPayload::animVariant,
			RequestStateTransitionC2SPayload::new);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}


	public static class Server {
		/**
		 * 服务端收到客户端的状态转换请求后进行权威校验。
		 *
		 * <p>通过 {@link IParkourState#canEnter} 二次确认玩家是否满足进入目标状态的条件。
		 * 合法则执行转换；不合法则通过 {@link ForceLocalPlayerStateS2CPayload} 将客户端回滚到当前服务端认定的合法状态。
		 *
		 * @param packet  包含客户端请求的目标状态ID和动画变体，不为 null
		 * @param context 网络上下文，从中提取发包的 ServerPlayer
		 */
		public static void handle(RequestStateTransitionC2SPayload packet,
				IPayloadContext context) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					ParkourContext pkContext = ParkourContext.get(player);
					Identifier stateId = packet.targetStateId();
					int animVariant = packet.animVariant();
					IParkourState targetState =
							ParkourRegistries.PARKOUR_STATE_REGISTRY.get(
											stateId).map(r -> r.value())
									.orElse(null);

					if (targetState != null && targetState.canEnter(player,
							pkContext)) {
						ParkourStateMachine.transitionTo(player, pkContext,
								targetState, animVariant);
					} else {
						StateData stateData =
								ParkourContext.get(player).state();
						IParkourState currentState = stateData.getState();
						Identifier currentStateId =
								ParkourRegistries.PARKOUR_STATE_REGISTRY.getKey(
										currentState);
						int variant = stateData.getAnimationVariant();

						if (currentStateId != null) {
							PacketDistributor.sendToPlayer(player,
									new ForceLocalPlayerStateS2CPayload(
											currentStateId,
											variant));
						}
					}
				}
			});
		}
	}
}
