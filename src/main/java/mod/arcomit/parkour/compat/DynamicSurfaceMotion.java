package mod.arcomit.parkour.compat;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.content.behavior.speedvault.SpeedVaultState;
import mod.arcomit.parkour.content.behavior.wallclimb.WallClimbState;
import mod.arcomit.parkour.content.behavior.wallrun.WallRunState;
import mod.arcomit.parkour.content.behavior.wallslide.WallSlideState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.content.init.ParkourAttachmentTypes;
import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.core.sensor.impl.ArmhangEyeSensor;
import mod.arcomit.parkour.core.sensor.impl.ArmhangTopSensor;
import mod.arcomit.parkour.core.sensor.impl.HeadFeetSensor;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import mod.arcomit.parkour.utils.BlockCollisions;
import mod.arcomit.parkour.utils.FilteredBlockCollisions;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Local anchors keep wall actions attached without replacing either mod's collision solver. */
public final class DynamicSurfaceMotion {
	private DynamicSurfaceMotion() {}

	/** Runtime-only attachment: never serialized, copied on respawn, or accepted from a packet. */
	public static final class Contact {
		private DynamicCollisionSource source;
		private Level level;
		private IParkourState state;
		private Vec3 localAnchor, worldAnchor, localNormal, worldNormal;
		private Vec3 localRunDirection;
		private Vec3 lastSoundLocal;

		private void clear() {
			source = null;
			level = null;
			state = null;
			localAnchor = worldAnchor = localNormal = worldNormal = localRunDirection = null;
			lastSoundLocal = null;
		}
	}

	private static Contact contact(Player player) {
		return player.getData(ParkourAttachmentTypes.DYNAMIC_SURFACE_CONTACT);
	}

	/** Read the current grip without capturing or changing its anchor. */
	@Nullable
	public static DynamicCollisionSource attachedSource(Player player, Direction direction) {
		Contact contact = contact(player);
		return matchesDirection(player, contact, direction) ? contact.source : null;
	}

	/** Server corrections must only run when the owning engine resolves them against the wall. */
	public static boolean supportsServerPlayerCorrections(Player player) {
		if (!DynamicCollisions.isEnabled()) return true;
		ParkourContext context = ParkourContext.get(player);
		IParkourState state = context.state().getState();
		Direction direction = direction(player, state, context.wall());
		if (direction == null) return true;
		DynamicBlockHit hit = findSupport(player, state, direction);
		return hit == null || hit.source().supportsServerPlayerCorrections();
	}

	/** Runs before parkour's sensors/validity checks. Server players retain normal position sync. */
	public static void beforeTick(Player player) {
		if (!DynamicCollisions.isEnabled()) return;
		Contact contact = contact(player);
		IParkourState state = ParkourContext.get(player).state().getState();
		if (contact.source == null) return;
		if (contact.state != state || contact.level != player.level() || !contact.source.isValid()
				|| !player.isAlive() || player.isPassenger()
				|| (DynamicCollisions.isOnGround(player) && !(state instanceof SpeedVaultState))
				|| player.position().distanceToSqr(contact.worldAnchor) > 256) {
			contact.clear();
			return;
		}
		Vec3 target = contact.source.toWorld(contact.localAnchor);
		Vec3 displacement = target.subtract(contact.worldAnchor);
		if (!finite(displacement) || displacement.lengthSqr() > 256) {
			contact.clear();
			return;
		}
		Vec3 remaining = displacement.lengthSqr() < 1.0E-14 ? Vec3.ZERO
				: contact.source.removeNativeMotion(player, player.position(), displacement);
		if (!finite(remaining) || remaining.lengthSqr() > 256) {
			contact.clear();
			return;
		}
		// Create can push side contacts at ClientTick.Post, after our anchor was captured.
		// A native parent was already subtracted above; observed motion can contain that
		// same contribution and must not cancel the remaining child-contraption motion.
		if (!contact.source.isNativeCarrier(player)) {
			remaining = SurfaceMotionMath.remainingMotion(remaining,
					player.position().subtract(contact.worldAnchor));
		}
		if (player.isLocalPlayer() && remaining.lengthSqr() > 1.0E-14) {
			DynamicCollisions.move(player, MoverType.SELF, remaining);
		}

		Vec3 normal = transformDirection(contact.source, contact.localAnchor, contact.localNormal);
		if (player.isLocalPlayer() && normal.horizontalDistanceSqr() > 1.0E-8
				&& contact.worldNormal.horizontalDistanceSqr() > 1.0E-8) {
			double angle = Math.atan2(normal.z, normal.x) - Math.atan2(contact.worldNormal.z, contact.worldNormal.x);
			float yaw = contact.source.removeNativeYaw(player,
					Mth.wrapDegrees((float) Math.toDegrees(angle)), normal);
			player.setYRot(player.getYRot() + yaw);
			player.yRotO += yaw;
		}
		updateDirections(player, contact, normal);
	}

	/** Refresh after vanilla movement so sliding/running input changes the local attachment point. */
	public static void capture(Player player) {
		if (!DynamicCollisions.isEnabled() || (player.level().isClientSide() && !player.isLocalPlayer())) return;
		Contact contact = contact(player);
		ParkourContext context = ParkourContext.get(player);
		IParkourState state = context.state().getState();
		Direction direction = direction(player, state, context.wall());
		if (direction == null || !player.isAlive() || player.isPassenger()
				|| (DynamicCollisions.isOnGround(player) && !(state instanceof SpeedVaultState))) {
			contact.clear();
			return;
		}
		DynamicBlockHit hit = findSupport(player, state, direction);
		if (hit == null) {
			contact.clear();
			return;
		}
		boolean same = contact.source != null && contact.source.identity().equals(hit.source().identity())
				&& contact.level == player.level() && contact.state == state;
		contact.source = hit.source();
		contact.level = player.level();
		contact.state = state;
		contact.worldAnchor = player.position();
		contact.localAnchor = contact.source.toLocal(contact.worldAnchor);
		if (!same) contact.lastSoundLocal = null;
		Vec3 candidateNormal = localWallNormal(player, contact.source, direction);
		if (!same || sensorDirection(player, contact.source,
				transformDirection(contact.source, contact.localAnchor, contact.localNormal)) != direction) {
			contact.localNormal = candidateNormal;
		}
		contact.worldNormal = transformDirection(contact.source, contact.localAnchor, contact.localNormal);
		if (state instanceof WallRunState && (!same || contact.localRunDirection == null)) {
			Direction run = context.wall().getRunMove();
			if (run != null) contact.localRunDirection = contact.source.toLocal(
					player.position().add(Vec3.atLowerCornerOf(run.getNormal()))).subtract(contact.localAnchor);
		}
	}

	@Nullable
	private static Direction direction(Player player, IParkourState state, WallMovementData wall) {
		if (state instanceof ArmhangState) return wall.getArmhang();
		if (state instanceof WallRunState) return wall.getRunCollision();
		if (state instanceof WallSlideState) return wall.getSlide();
		if (state instanceof WallClimbState) return DynamicSurfaceFacing.climbDirection(player);
		if (state instanceof SpeedVaultState) return player.getDirection();
		return null;
	}

	@Nullable
	private static DynamicBlockHit findSupport(Player player, IParkourState state, Direction direction) {
		TagKey<Block> skip = ParkourTags.Blocks.COMMON_IGNORED_BLOCKS;
		AABB box;
		if (state instanceof ArmhangState) {
			skip = ParkourTags.Blocks.SCAFFOLDING_BLOCKS;
			if (ArmhangEyeSensor.isValidCollision(player, direction)) {
				box = ArmhangEyeSensor.getBoxes(player, direction).getFirst();
			} else if (ArmhangTopSensor.isValidCollision(player, direction)) {
				box = ArmhangTopSensor.getBoxes(player, direction).getFirst();
			} else return null;
		} else if (state instanceof SpeedVaultState) {
			box = BlockCollisions.buildBox(player, direction, 0, 0.85, 1);
		} else {
			box = HeadFeetSensor.getBoxes(player, direction).getFirst();
		}
		// A static wall in the same sensor has priority over a passing structure behind it.
		if (new FilteredBlockCollisions<>(player.level(), player, box, false, skip,
				(pos, shape) -> shape).hasNext()) return null;
		if (state instanceof WallRunState || state instanceof WallClimbState
				|| state instanceof WallSlideState) {
			return DynamicCollisions.findRelaxedSensorCollision(player.level(), player, box,
					direction, ParkourConstants.DYNAMIC_WALL_SENSOR_TOLERANCE, skip);
		}
		return DynamicCollisions.findSensorCollision(player.level(), player, box, skip);
	}

	private static void updateDirections(Player player, Contact contact, Vec3 normal) {
		if (normal.horizontalDistanceSqr() < 1.0E-8) return;
		Direction direction = sensorDirection(player, contact.source, normal);
		WallMovementData wall = ParkourContext.get(player).wall();
		if (contact.state instanceof ArmhangState) wall.setArmhang(direction);
		if (contact.state instanceof WallSlideState) wall.setSlide(direction);
		if (contact.state instanceof WallClimbState) wall.setClimb(direction);
		if (contact.state instanceof WallRunState) {
			wall.setRunCollision(direction);
			if (contact.localRunDirection != null) wall.setRunMove(worldDirection(
					contact.source, contact.localAnchor, contact.localRunDirection));
		}
	}

	private static Direction worldDirection(DynamicCollisionSource source, Vec3 anchor, Vec3 localDirection) {
		Vec3 world = transformDirection(source, anchor, localDirection);
		return Direction.getNearest(world.x, 0, world.z);
	}

	private static Direction sensorDirection(Player player, DynamicCollisionSource source, Vec3 normal) {
		return CollisionGeometry.sensorDirection(normal, player.position(), source.sensorTransform(player));
	}

	private static Vec3 transformDirection(DynamicCollisionSource source, Vec3 anchor, Vec3 direction) {
		return source.toWorld(anchor.add(direction)).subtract(source.toWorld(anchor)).normalize();
	}

	private static Vec3 localWallNormal(Player player, DynamicCollisionSource source, Direction direction) {
		Vec3 cardinal = Vec3.atLowerCornerOf(direction.getNormal());
		// Snap in block space, retaining the exact rotated face normal in world space.
		var sensorTransform = source.sensorTransform(player);
		Vec3 sensorFacing = sensorTransform.apply(player.position().add(cardinal))
				.subtract(sensorTransform.apply(player.position()));
		Vec3 localFacing = source.toLocal(player.position().add(sensorFacing))
				.subtract(source.toLocal(player.position()));
		Direction face = Direction.getNearest(localFacing.x, localFacing.y, localFacing.z);
		return Vec3.atLowerCornerOf(face.getNormal());
	}

	public static Vec3 wallNormal(Player player, Direction fallback) {
		Contact contact = contact(player);
		// Entry physics and corner changes happen before the end-of-tick capture.
		if (DynamicCollisions.isEnabled() && !matchesDirection(player, contact, fallback)) {
			capture(player);
		}
		if (matchesDirection(player, contact, fallback)) {
			Vec3 normal = transformDirection(contact.source, contact.localAnchor, contact.localNormal);
			if (normal.horizontalDistanceSqr() > 1.0E-8) return new Vec3(normal.x, 0, normal.z).normalize();
		}
		// A wall jump from the default state has a sensor slot but no persistent grip.
		if (DynamicCollisions.isEnabled()) {
			AABB box = BlockCollisions.buildBox(player, fallback, 0, 1, 0.2);
			if (!new FilteredBlockCollisions<>(player.level(), player, box, false,
					ParkourTags.Blocks.COMMON_IGNORED_BLOCKS, (pos, shape) -> shape).hasNext()) {
				IParkourState state = ParkourContext.get(player).state().getState();
				DynamicBlockHit hit = (state instanceof ArmhangState)
						? DynamicCollisions.findSensorCollision(player.level(), player, box,
								ParkourTags.Blocks.COMMON_IGNORED_BLOCKS)
						: DynamicCollisions.findRelaxedSensorCollision(player.level(), player, box,
								fallback, ParkourConstants.DYNAMIC_WALL_SENSOR_TOLERANCE,
								ParkourTags.Blocks.COMMON_IGNORED_BLOCKS);
				if (hit != null) {
					Vec3 normal = transformDirection(hit.source(), hit.source().toLocal(player.position()),
							localWallNormal(player, hit.source(), fallback));
					if (normal.horizontalDistanceSqr() > 1.0E-8) return new Vec3(normal.x, 0, normal.z).normalize();
				}
			}
		}
		return Vec3.atLowerCornerOf(fallback.getNormal());
	}

	/** Read-only frame query: rendering must not recapture movement anchors or change wall state. */
	public static boolean hasRenderSurface(Player player, Direction direction) {
		return direction != null && renderContact(player, direction) != null;
	}

	public static Vec3 renderWallNormal(Player player, Direction fallback, float partialTick) {
		if (fallback == null) return Vec3.directionFromRotation(0, player.getYRot());
		RenderContact render = renderContact(player, fallback);
		if (render == null) return Vec3.atLowerCornerOf(fallback.getNormal());
		Vec3 normal = render.source().toRenderWorld(render.anchor().add(render.normal()), partialTick)
				.subtract(render.source().toRenderWorld(render.anchor(), partialTick));
		return finite(normal) && normal.horizontalDistanceSqr() > 1.0E-8
				? new Vec3(normal.x, 0, normal.z).normalize() : Vec3.atLowerCornerOf(fallback.getNormal());
	}

	public static Vec3 renderWallTangent(Player player, Direction wall, Vec3 intended, float partialTick) {
		if (wall == null) return intended.normalize();
		RenderContact render = renderContact(player, wall);
		if (render == null) return intended.normalize();
		Vec3 normal = renderWallNormal(player, wall, partialTick);
		Vec3 tangent = new Vec3(-normal.z, 0, normal.x);
		Vec3 localIntended = render.source().toLocal(player.position().add(intended))
				.subtract(render.source().toLocal(player.position()));
		Vec3 visualIntended = render.source().toRenderWorld(render.anchor().add(localIntended), partialTick)
				.subtract(render.source().toRenderWorld(render.anchor(), partialTick));
		return tangent.dot(visualIntended) < 0 ? tangent.scale(-1) : tangent;
	}

	@Nullable
	private static RenderContact renderContact(Player player, Direction direction) {
		if (!DynamicCollisions.isEnabled()) return null;
		Contact contact = contact(player);
		if (matchesDirection(player, contact, direction)) {
			return new RenderContact(contact.source, contact.localAnchor, contact.localNormal);
		}
		// Observers do not run local movement capture, so resolve their synced sensor slot directly.
		DynamicBlockHit hit = findSupport(player, ParkourContext.get(player).state().getState(), direction);
		return hit == null ? null : new RenderContact(hit.source(), hit.source().toLocal(player.position()),
				localWallNormal(player, hit.source(), direction));
	}

	private record RenderContact(DynamicCollisionSource source, Vec3 anchor, Vec3 normal) {}

	private static boolean matchesDirection(Player player, Contact contact, Direction direction) {
		return contact.source != null && contact.level == player.level() && contact.source.isValid()
				&& contact.state == ParkourContext.get(player).state().getState()
				&& sensorDirection(player, contact.source,
						transformDirection(contact.source, contact.localAnchor, contact.localNormal)) == direction;
	}

	/** Keep motion tangent to a rotated wall while preserving the player's chosen travel sign. */
	public static Vec3 wallTangent(Player player, Direction wall, Vec3 intended) {
		Vec3 normal = wallNormal(player, wall);
		if (contact(player).source == null) return intended.normalize();
		Vec3 tangent = new Vec3(-normal.z, 0, normal.x);
		return tangent.dot(intended) < 0 ? tangent.scale(-1) : tangent;
	}

	public static Vec3 jumpVelocity(Player player, Direction wall, Vec3 relativeVelocity) {
		// Existing jump vectors are already in world space and follow the player's view.
		return relativeVelocity.add(jumpMotion(player, wall));
	}

	public static Vec3 jumpMotion(Player player, Direction direction) {
		return jumpMotion(player, direction, false);
	}

	/** Wall jumps must inherit from the same tilted surface accepted by their foot probe. */
	public static Vec3 wallJumpVelocity(Player player, Direction wall, Vec3 relativeVelocity) {
		return relativeVelocity.add(jumpMotion(player, wall, true));
	}

	private static Vec3 jumpMotion(Player player, Direction direction, boolean wallJump) {
		if (!DynamicCollisions.isEnabled()) return Vec3.ZERO;
		AABB box = BlockCollisions.buildBox(player, direction, 0, 1, 0.2);
		if (new FilteredBlockCollisions<>(player.level(), player, box, false,
				ParkourTags.Blocks.COMMON_IGNORED_BLOCKS, (pos, shape) -> shape).hasNext()) return Vec3.ZERO;
		DynamicBlockHit hit = wallJump
				? DynamicCollisions.findRelaxedSensorCollision(player.level(), player, box, direction,
						ParkourConstants.DYNAMIC_WALL_SENSOR_TOLERANCE, ParkourTags.Blocks.COMMON_IGNORED_BLOCKS)
				: DynamicCollisions.findSensorCollision(player.level(), player, box, ParkourTags.Blocks.COMMON_IGNORED_BLOCKS);
		Vec3 motion = hit == null ? Vec3.ZERO : hit.source().jumpMotion(player, player.position());
		return finite(motion) && motion.lengthSqr() <= 256 ? motion : Vec3.ZERO;
	}

	/** Hanging footsteps count motion along the surface, excluding the structure's own travel. */
	public static double movementDistance(Player player, Vec3 previousWorldPosition) {
		Contact contact = contact(player);
		if (contact.source == null || contact.level != player.level() || !contact.source.isValid()) {
			return player.position().distanceTo(previousWorldPosition);
		}
		Vec3 local = contact.source.toLocal(player.position());
		double distance = contact.lastSoundLocal == null ? 0
				: contact.source.toWorld(local).distanceTo(contact.source.toWorld(contact.lastSoundLocal));
		contact.lastSoundLocal = local;
		return distance;
	}

	private static boolean finite(Vec3 vector) {
		return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
