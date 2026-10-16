package mod.arcomit.parkour.content.behavior.barswing;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.content.behavior.barswing.network.SyncBarSwingS2CPayload;
import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.init.ParkourAnimationIds;
import mod.arcomit.parkour.content.init.ParkourSounds;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.AbstractParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** 横杆摆荡：腾空接近时自动抓杆，前进加力、后退刹摆、潜行松手。 */
public class BarSwingState extends AbstractParkourState {
	public static final EntityDimensions HANGING_SIZE = EntityDimensions.scalable(0.6F, 1.35F).withEyeHeight(1.17F);

	@Override
	public EntityDimensions getCustomDimensions(Player player) {
		return HANGING_SIZE;
	}

	@Override
	public Pose getLinkedPose() {
		return Pose.STANDING;
	}

	public BarSwingState() {
		registerTransitions(IParkourStateTransition.onLocalTick(ParkourStates.DEFAULT::get,
				(player, context) -> ParkourProxies.INPUT_PROXY.getShiftKeyDown(player)));
	}

	public static boolean meetsBaseConditions(Player player, ParkourContext context) {
		return ParkourConfig.enableBarSwing && player.isAlive() && ParkourChecks.canPerformBehavior(player)
				&& ParkourProxies.GET_CLIENT_CONFIG_PROXY.isEnableParkour(context)
				&& !player.isInWater() && !player.isInLava();
	}

	@Override
	public boolean canEnter(Player player, ParkourContext context) {
		return context.state().getState() instanceof DefaultState && meetsBaseConditions(player, context)
				&& !DynamicCollisions.isOnGround(player) && !player.isShiftKeyDown() && !player.onClimbable()
				&& player.tickCount >= BarSwingData.get(player).cooldownUntil
				&& BarSwingCollision.findGrip(player) != null;
	}

	@Override
	public void onSimulationEnter(Player player, ParkourContext context) {
		BarSwingData data = BarSwingData.get(player);
		var grip = BarSwingCollision.findGrip(player);
		data.active = grip != null;
		if (grip == null) {
			return;
		}
		data.bar = grip.pos();
		data.axis = grip.axis();
		data.anchor = grip.anchor();
		data.forward = grip.forward();
		data.down = grip.down();
		data.barDirection = grip.barDirection();
		data.source = grip.source();
		data.sourceId = data.source == null ? null : data.source.networkId();
		data.localAnchor = grip.localAnchor();
		data.localForward = grip.localForward();
		data.anchorMotion = Vec3.ZERO;
		DynamicCollisions.clearSurfaceInertia(player);
		data.dimension = player.level().dimension();
		data.releaseLift = 0;
		data.angle = data.previousAngle = grip.angle();
		data.angularVelocity = Mth.clamp(player.getDeltaMovement().dot(data.forward) / BarSwingMath.LENGTH, 0.07, 0.18);
		data.velocity = BarSwingMath.tangent(data.forward, data.down, data.angle, data.angularVelocity);
		context.jump().setSpeedVaultJumpWindow(0);
		// 成功抓杆后刷新同墙动作次数，允许摆荡跃出后再次使用原来的墙面。
		// 此入口由本地预测和服务端共同执行；未找到抓点时不会清除记录。
		//context.wall().resetRunCollision();
		context.wall().resetClimb();
		context.jump().resetLastViewJump();
		context.jump().resetLastUpJump();
		context.jump().resetLastParallelJump();
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
	}

	@Override
	public void onClientEnter(Player player, ParkourContext context) {
		ParkourProxies.SOUND_PROXY.playEntityBoundSound(ParkourSounds.ARMHANG.get(), SoundSource.PLAYERS,
				0.65F, 1.1F, player, player.getRandom().nextLong());
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
	}

	@Override
	public void onServerEnter(Player player, ParkourContext context) {
		if (BarSwingData.get(player).active) {
			PacketDistributor.sendToPlayersTrackingEntity(player, SyncBarSwingS2CPayload.of(player));
		}
	}

	@Override
	public void onTrackingStart(ServerPlayer tracker, Player target, ParkourContext context) {
		if (BarSwingData.get(target).active) {
			PacketDistributor.sendToPlayer(tracker, SyncBarSwingS2CPayload.of(target));
		}
	}

	@Override
	public void onSimulationTick(Player player, ParkourContext context) {
		BarSwingData data = BarSwingData.get(player);
		player.resetFallDistance();
		data.active &= BarSwingAnchor.refresh(player);
		BarSwingView.clamp(player);
		// ServerPlayer 的位置由正常移动包驱动；从权威位置采样角度，避免再次积分产生双倍位移。
		if (!player.level().isClientSide() && data.active) {
			Vec3 relative = player.position().subtract(data.anchor);
			data.previousAngle = data.angle;
			data.angle = Math.atan2(relative.dot(data.forward), relative.dot(data.down));
			data.angularVelocity = Mth.clamp(data.angle - data.previousAngle,
					-BarSwingMath.MAX_ANGULAR_SPEED, BarSwingMath.MAX_ANGULAR_SPEED);
			data.velocity = BarSwingMath.tangent(data.forward, data.down, data.angle, data.angularVelocity);
		}
	}

	@Override
	public void onClientTick(Player player, ParkourContext context) {
		if (!player.isLocalPlayer()) BarSwingAnchor.refresh(player);
	}

	@Override
	public boolean isValid(Player player, ParkourContext context) {
		BarSwingData data = BarSwingData.get(player);
		Vec3 offset = player.position().subtract(data.anchor);
		double alongBar = offset.dot(data.barDirection.multiply(1, 0, 1).normalize());
		return data.active && meetsBaseConditions(player, context) && BarSwingCollision.hasBar(player, data)
				&& Math.abs(offset.length() - BarSwingMath.LENGTH) < 0.55
				&& Math.abs(alongBar) < 0.45 && offset.y < -0.75
				&& (!DynamicCollisions.isOnGround(player) || context.state().getTicksInState() < 3);
	}

	@Override
	public void onSimulationExit(Player player, ParkourContext context) {
		BarSwingData data = BarSwingData.get(player);
		data.active = false;
		data.cooldownUntil = player.tickCount + 10;
		context.wall().setArmhangCooldown(5);
		Vec3 release = data.velocity;
		if (data.source != null && data.source.isValid()) {
			release = BarSwingMath.tangent(data.forward, data.down, data.angle, data.angularVelocity)
					.add(data.source.jumpMotion(player, player.position()));
		}
		player.setDeltaMovement(release);
		player.resetFallDistance();
	}

	@Override
	public ResourceLocation animId(Player player) {
		return ParkourAnimationIds.BAR_SWING;
	}
}
