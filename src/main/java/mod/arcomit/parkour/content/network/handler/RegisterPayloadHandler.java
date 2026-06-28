package mod.arcomit.parkour.content.network.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.action.supportwalljump.network.SupportWallJumpC2SPayload;
import mod.arcomit.parkour.content.action.swimmingboost.network.UseSwimmingBoostC2SPayload;
import mod.arcomit.parkour.content.action.swimmingjump.network.UseSwimmingJumpC2SPayload;
import mod.arcomit.parkour.content.action.walljump.network.WallJumpC2SPayload;
import mod.arcomit.parkour.content.behavior.armhang.network.BroadcastArmhangDirS2CPayload;
import mod.arcomit.parkour.content.behavior.armhang.network.SyncArmhangDirC2SPayload;
import mod.arcomit.parkour.content.behavior.landingroll.network.SetLandingRollWindowC2SPayload;
import mod.arcomit.parkour.content.behavior.wallrun.network.BroadcastWallRunDirS2CPayload;
import mod.arcomit.parkour.content.behavior.wallslide.network.BroadcastWallSlideDirS2CPayload;
import mod.arcomit.parkour.content.network.SyncParkourEnabledC2SPayload;
import mod.arcomit.parkour.core.client.animation.player.network.BroadcastPlayActionAnimS2CPayload;
import mod.arcomit.parkour.core.client.animation.player.network.RequestPlayActionAnimC2SPayload;
import mod.arcomit.parkour.core.statemachine.network.BroadcastStateChangeS2CPayload;
import mod.arcomit.parkour.core.statemachine.network.ForceLocalPlayerStateS2CPayload;
import mod.arcomit.parkour.core.statemachine.network.RequestStateTransitionC2SPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 注册网络通信数据包处理器。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class RegisterPayloadHandler {

	@SubscribeEvent
	public static void register(RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");

		registrar.playToServer(SetLandingRollWindowC2SPayload.TYPE,
				SetLandingRollWindowC2SPayload.STREAM_CODEC,
				(payload, context) -> SetLandingRollWindowC2SPayload.Server.handle(
						payload, context));

		registrar.playToServer(UseSwimmingBoostC2SPayload.TYPE,
				UseSwimmingBoostC2SPayload.STREAM_CODEC,
				(payload, context) -> UseSwimmingBoostC2SPayload.Server.handle(
						payload, context));

		registrar.playToServer(WallJumpC2SPayload.TYPE, WallJumpC2SPayload.STREAM_CODEC,
				(payload, context) -> WallJumpC2SPayload.Server.handle(payload,
						context));
		registrar.playToServer(SupportWallJumpC2SPayload.TYPE,
				SupportWallJumpC2SPayload.STREAM_CODEC,
				(payload, context) -> SupportWallJumpC2SPayload.Server.handle(
						payload, context));
		registrar.playToServer(UseSwimmingJumpC2SPayload.TYPE,
				UseSwimmingJumpC2SPayload.STREAM_CODEC,
				(payload, context) -> UseSwimmingJumpC2SPayload.Server.handle(
						payload, context));
		registrar.playToServer(SyncArmhangDirC2SPayload.TYPE,
				SyncArmhangDirC2SPayload.STREAM_CODEC,
				(payload, context) -> SyncArmhangDirC2SPayload.Server.handle(
						payload, context));

		registrar.playToServer(RequestStateTransitionC2SPayload.TYPE,
				RequestStateTransitionC2SPayload.STREAM_CODEC,
				(payload, context) -> RequestStateTransitionC2SPayload.Server.handle(
						payload, context));

		registrar.playToClient(ForceLocalPlayerStateS2CPayload.TYPE,
				ForceLocalPlayerStateS2CPayload.STREAM_CODEC,
				(payload, context) -> ForceLocalPlayerStateS2CPayload.Client.handle(
						payload, context));

		registrar.playToClient(BroadcastStateChangeS2CPayload.TYPE,
				BroadcastStateChangeS2CPayload.STREAM_CODEC,
				(payload, context) -> BroadcastStateChangeS2CPayload.Client.handle(
						payload, context));

		registrar.playToServer(RequestPlayActionAnimC2SPayload.TYPE,
				RequestPlayActionAnimC2SPayload.STREAM_CODEC,
				(payload, context) -> RequestPlayActionAnimC2SPayload.Server.handle(
						payload, context));
		registrar.playToClient(BroadcastPlayActionAnimS2CPayload.TYPE,
				BroadcastPlayActionAnimS2CPayload.STREAM_CODEC,
				(payload, context) -> BroadcastPlayActionAnimS2CPayload.Client.handle(
						payload, context));

		registrar.playToClient(BroadcastWallSlideDirS2CPayload.TYPE,
				BroadcastWallSlideDirS2CPayload.STREAM_CODEC,
				(payload, context) -> BroadcastWallSlideDirS2CPayload.Client.handle(
						payload, context));

		registrar.playToClient(BroadcastArmhangDirS2CPayload.TYPE,
				BroadcastArmhangDirS2CPayload.STREAM_CODEC,
				(payload, context) -> BroadcastArmhangDirS2CPayload.Client.handle(
						payload, context));

		registrar.playToServer(SyncParkourEnabledC2SPayload.TYPE,
				SyncParkourEnabledC2SPayload.STREAM_CODEC,
				(payload, context) -> SyncParkourEnabledC2SPayload.Server.handle(payload,
						context));

		registrar.playToClient(BroadcastWallRunDirS2CPayload.TYPE,
				BroadcastWallRunDirS2CPayload.STREAM_CODEC,
				(payload, context) -> BroadcastWallRunDirS2CPayload.Client.handle(
						payload, context));
	}
}
