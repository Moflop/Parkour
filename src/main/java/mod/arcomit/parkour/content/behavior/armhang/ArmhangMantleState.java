package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.init.ParkourAnimationIds;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.utils.Directions;
import mod.arcomit.parkour.utils.ParkourChecks;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import mod.arcomit.parkour.content.behavior.armhang.network.SyncArmhangGripS2CPayload;

/** A deliberate climb from a verified grip; its collision path is independent of wall jumping. */
public final class ArmhangMantleState extends AbstractParkourState {
	@Override public boolean canEnter(Player player, ParkourContext context) {
		var direction = context.wall().getArmhang();
		return context.state().getState() instanceof ArmhangState && ParkourConfig.enableArmhang
				&& ParkourChecks.canPerformBehavior(player) && !player.isShiftKeyDown()
				&& direction != null && Directions.isFacing(player, DynamicSurfaceMotion.wallNormal(player, direction), 75)
				&& ArmhangMantle.plan(player) != null;
	}

	@Override public void onSimulationEnter(Player player, ParkourContext context) {
		var data = ArmhangData.get(player);
		data.mantle = ArmhangMantle.plan(player);
		data.mantleTicks = 0;
		data.mantleBlocked = data.mantle == null;
		data.shimmySpeed = 0;
		data.active = data.mantle != null;
		if (data.mantle != null) data.grip = data.mantle.grip();
		player.setDeltaMovement(Vec3.ZERO);
	}

	@Override public void onSimulationTick(Player player, ParkourContext context) {
		if (!player.level().isClientSide()) ArmhangMantle.observeServerProgress(player, context.state().getTicksInState());
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
	}

	@Override public void onServerEnter(Player player, ParkourContext context) {
		if (ArmhangData.get(player).grip != null) PacketDistributor.sendToPlayersTrackingEntity(player, SyncArmhangGripS2CPayload.of(player));
	}
	@Override public void onTrackingStart(ServerPlayer tracker, Player target, ParkourContext context) {
		if (ArmhangData.get(target).grip != null) PacketDistributor.sendToPlayer(tracker, SyncArmhangGripS2CPayload.of(target));
	}
	@Override public void onClientTick(Player player, ParkourContext context) {
		if (!player.isLocalPlayer()) ArmhangData.get(player).resolveSource(player);
	}

	@Override public boolean isValid(Player player, ParkourContext context) {
		var data = ArmhangData.get(player);
		// The server observes normal client movement packets rather than integrating the path twice.
		return !data.mantleBlocked && ParkourConfig.enableArmhang && ParkourChecks.canPerformBehavior(player)
				&& ParkourProxies.GET_CLIENT_CONFIG_PROXY.isEnableParkour(context)
				&& !player.isInWater() && !player.isInLava()
				&& context.state().getTicksInState() <= ArmhangMotion.MANTLE_TICKS + 8
				&& ArmhangMantle.canContinue(player, data.mantle);
	}

	@Override public void onSimulationExit(Player player, ParkourContext context) {
		ArmhangData.get(player).active = false;
		context.wall().setArmhangCooldown(10);
		player.setDeltaMovement(Vec3.ZERO);
	}
	@Override public Pose getLinkedPose() { return Pose.STANDING; }
	@Override public ResourceLocation animId(Player player) { return ParkourAnimationIds.ARMHANG_MANTLE; }
}
