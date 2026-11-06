package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingCollision;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.utils.BlockCollisions;
import mod.arcomit.parkour.utils.FilteredBlockCollisions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Validates and executes a lift-then-step mantle, without teleporting through the ledge. */
public final class ArmhangMantle {
	private ArmhangMantle() {}
	public record Plan(ArmhangGrip grip, Vec3 localStart, Vec3 localLift, Vec3 localTarget) {
		public Vec3 position(double tick) {
			return grip.logical(ArmhangMotion.mantlePosition(localStart, localLift, localTarget, tick));
		}
	}

	public static Plan plan(Player player) {
		var direction = ParkourContext.get(player).wall().getArmhang();
		ArmhangGrip grip = ArmhangLedge.find(player, direction);
		if (grip == null) return null;
		Vec3 start = player.position();
		Vec3 center = grip.logical(grip.localCenter());
		Vec3 normal = grip.logical(grip.localCenter().add(grip.localNormal())).subtract(center).multiply(1, 0, 1).normalize();
		if (normal.lengthSqr() < 0.5) return null;
		// Keep an entire standing footprint on the platform, rather than balancing on a wall face.
		Vec3 target = center.add(normal.scale(player.getBbWidth() * 0.5 + 0.12));
		target = new Vec3(target.x, center.y + 0.018, target.z);
		Vec3 lift = new Vec3(start.x, target.y, start.z);
		if (target.y - start.y < 0.5 || target.y - start.y > 2.15 || !hasLanding(player, target)) return null;
		Plan result = new Plan(grip, local(grip, start), local(grip, lift), local(grip, target));
		Vec3 previous = start;
		for (int tick = 1; tick <= ArmhangMotion.MANTLE_TICKS; tick++) {
			Vec3 next = result.position(tick);
			if (!BarSwingCollision.clearTravel(player, previous, next)) return null;
			previous = next;
		}
		return result;
	}

	private static Vec3 local(ArmhangGrip grip, Vec3 point) {
		return grip.source() == null ? point : grip.source().toLocal(point);
	}

	public static boolean hasLanding(Player player, Vec3 target) {
		AABB body = player.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(target);
		if (!BarSwingCollision.clear(player, body)) return false;
		// Require real upward support at all four corners; a fence or a sliver is not a platform.
		double half = player.getBbWidth() * 0.5 - 0.055;
		for (double x : new double[]{-half, half}) for (double z : new double[]{-half, half}) {
			AABB foot = new AABB(target.x + x - 0.022, target.y - 0.10, target.z + z - 0.022,
					target.x + x + 0.022, target.y + 0.001, target.z + z + 0.022);
			Iterable<net.minecraft.world.phys.shapes.VoxelShape> blocks = () -> new FilteredBlockCollisions<>(
					player.level(), player, foot, false, null, (pos, shape) -> shape);
			boolean supported = ArmhangCollision.hasGroundSupport(foot, blocks);
			if (!supported) {
				Vec3 shift = target.subtract(player.position());
				for (var source : DynamicCollisions.sources(player.level(), body.inflate(0.5))) {
					var rotation = source.sensorTransform(player);
					if (source.hasSupport(foot, point -> rotation.apply(point.subtract(shift)).add(shift), player, null)) {
						supported = true;
						break;
					}
				}
			}
			if (!supported) return false;
		}
		return true;
	}

	/** Observe vanilla movement packets; the server must never integrate a second mantle path. */
	static void observeServerProgress(Player player, int stateAge) {
		ArmhangData data = ArmhangData.get(player);
		Plan plan = data.mantle;
		if (player.level().isClientSide() || plan == null
				|| plan.grip().source() != null && !plan.grip().source().isValid()) return;
		// A packet may arrive before this tick's state callback. Age alone never proves movement.
		int latestTick = Math.min(ArmhangMotion.MANTLE_TICKS, Math.max(0, stateAge) + 1);
		Vec3 actual = player.position(), previous = plan.position(0);
		double closestDistance = Double.POSITIVE_INFINITY, observedTick = 0;
		for (int next = 1; next <= latestTick; next++) {
			Vec3 position = plan.position(next), segment = position.subtract(previous);
			double fraction = segment.lengthSqr() < 1.0E-10 ? 0
					: net.minecraft.util.Mth.clamp(actual.subtract(previous).dot(segment) / segment.lengthSqr(), 0, 1);
			double distance = actual.distanceToSqr(previous.add(segment.scale(fraction)));
			if (distance < closestDistance) {
				closestDistance = distance;
				observedTick = next - 1 + fraction;
			}
			previous = position;
		}
		// Allow packet/source-pose skew without treating an unrelated position as completed travel.
		double tolerance = 0.25 * player.getScale();
		if (closestDistance <= tolerance * tolerance) {
			data.mantleTicks = Math.min(latestTick, (int) Math.floor(observedTick + 1.0E-5));
		}
	}

	public static boolean canContinue(Player player, Plan plan) {
		if (plan == null || plan.grip().source() != null && !plan.grip().source().isValid()) return false;
		if (!hasLanding(player, plan.position(ArmhangMotion.MANTLE_TICKS))) return false;
		int tick = Math.min(ArmhangData.get(player).mantleTicks, ArmhangMotion.MANTLE_TICKS);
		boolean observing = !player.level().isClientSide()
				&& ParkourContext.get(player).state().getState() instanceof ArmhangMantleState;
		// Start at the received position, including fractional progress through the current segment.
		Vec3 previous = observing ? player.position() : plan.position(tick);
		if (observing && !BarSwingCollision.clear(player, player.getBoundingBox())) return false;
		for (int next = tick + 1; next <= ArmhangMotion.MANTLE_TICKS; next++) {
			Vec3 position = plan.position(next);
			if (!BarSwingCollision.clearTravel(player, previous, position)) return false;
			previous = position;
		}
		return true;
	}

	/** Called at vanilla travel so gravity/walking cannot add a second, unplanned movement. */
	public static boolean travel(Player player) {
		if (!(ParkourContext.get(player).state().getState() instanceof ArmhangMantleState)) return false;
		ArmhangData data = ArmhangData.get(player);
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		if (!player.isLocalPlayer()) return true;
		if (!canContinue(player, data.mantle)) { abort(player); return true; }
		Vec3 target = data.mantle.position(Math.min(data.mantleTicks + 1, ArmhangMotion.MANTLE_TICKS));
		if (!BarSwingCollision.clearTravel(player, player.position(), target)
				|| target.distanceToSqr(player.position()) > 2.25) { abort(player); return true; }
		DynamicCollisions.clearSurfaceInertia(player);
		DynamicCollisions.move(player, MoverType.SELF, target.subtract(player.position()));
		if (player.position().distanceToSqr(target) > 0.004) { abort(player); return true; }
		data.mantleTicks++;
		if (data.mantleTicks >= ArmhangMotion.MANTLE_TICKS) {
			ParkourStateMachine.localTransitionTo(player, ParkourStates.DEFAULT.get());
		}
		return true;
	}

	private static void abort(Player player) {
		ArmhangData.get(player).mantleBlocked = true;
		ParkourStateMachine.localTransitionTo(player, ParkourStates.DEFAULT.get());
	}
}
