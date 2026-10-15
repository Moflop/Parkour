package mod.arcomit.parkour.compat;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.core.sensor.impl.ArmhangEyeSensor;
import mod.arcomit.parkour.core.sensor.impl.ArmhangTopSensor;
import mod.arcomit.parkour.core.sensor.impl.HeadFeetSensor;
import mod.arcomit.parkour.utils.FilteredBlockCollisions;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Resolves sensor slots from the actual wall and horizontal view, before a contact exists. */
public final class DynamicSurfaceFacing {
	private static final double FRONT_DOT = Math.sqrt(0.5);
	private static final double ANGLE_EPSILON = 1.0E-12;

	private DynamicSurfaceFacing() {}

	@Nullable
	public static Direction hangDirection(Player player) {
		return frontDirection(player, true);
	}

	@Nullable
	public static Direction climbDirection(Player player) {
		return frontDirection(player, false);
	}

	@Nullable
	private static Direction frontDirection(Player player, boolean hanging) {
		Direction facing = player.getDirection();
		Candidate facingCandidate = wallCandidate(player, facing, hanging);
		// Static blocks retain their original world-cardinal selection.
		if (facingCandidate != null && !facingCandidate.dynamic()) return facing;
		Vec3 look = horizontalLook(player);
		Direction best = null;
		double bestDot = FRONT_DOT - ANGLE_EPSILON;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			Candidate candidate = direction == facing ? facingCandidate
					: wallCandidate(player, direction, hanging);
			if (candidate == null || !candidate.dynamic()) continue;
			double dot = look.dot(candidate.normal());
			if (dot > bestDot) {
				bestDot = dot;
				best = direction;
			}
		}
		return best;
	}

	@Nullable
	public static Direction runDirection(Player player) {
		Direction facing = player.getDirection();
		Direction right = facing.getClockWise(), left = facing.getCounterClockWise();
		Vec3 look = horizontalLook(player);
		Candidate best = null;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			Candidate candidate = wallCandidate(player, direction, false);
			if (candidate == null) continue;
			if (candidate.dynamic()) {
				if (Math.abs(look.dot(candidate.normal())) > FRONT_DOT + ANGLE_EPSILON) continue;
			} else if (direction != right && direction != left) continue;
			if (best == null || candidate.distanceSquared() < best.distanceSquared()
					|| (candidate.distanceSquared() == best.distanceSquared() && direction == left)) {
				best = candidate;
			}
		}
		return best == null ? null : best.direction();
	}

	public static boolean wallOnLeft(Player player, Direction direction) {
		Candidate candidate = wallCandidate(player, direction, false);
		if (candidate == null || !candidate.dynamic()) {
			return direction == player.getDirection().getCounterClockWise();
		}
		Vec3 look = horizontalLook(player), normal = candidate.normal();
		return look.z * normal.x - look.x * normal.z > 0;
	}

	/** Uses the original full-height 0.05-block probe, including thin and half-height obstacles. */
	public static boolean blockedAhead(Player player, @Nullable TagKey<Block> skipTag) {
		Vec3 look = horizontalLook(player);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			AABB box = player.getBoundingBox().deflate(ParkourConstants.AABB_DEFLATE_EPSILON)
					.expandTowards(direction.getStepX() * 0.05, 0, direction.getStepZ() * 0.05);
			if (direction == player.getDirection() && hasStaticCollision(player, box, skipTag)) return true;
			DynamicBlockHit hit = DynamicCollisions.findSensorCollision(player.level(), player, box, skipTag,
					source -> direction == CollisionGeometry.sensorDirection(look,
							player.position(), source.sensorTransform(player)),
					candidate -> facesPlayer(player, direction, candidate, box, skipTag));
			if (hit != null) return true;
		}
		return false;
	}

	@Nullable
	private static Candidate wallCandidate(Player player, Direction direction, boolean hanging) {
		AABB box;
		TagKey<Block> skip;
		if (hanging) {
			skip = ParkourTags.Blocks.ARMHANG_IGNORED_BLOCKS;
			if (ArmhangEyeSensor.isValidCollision(player, direction)) {
				box = ArmhangEyeSensor.getBoxes(player, direction).getFirst();
			} else if (ArmhangTopSensor.isValidCollision(player, direction)) {
				box = ArmhangTopSensor.getBoxes(player, direction).getFirst();
			} else return null;
		} else {
			if (!HeadFeetSensor.isValidCollision(player, direction)) return null;
			box = HeadFeetSensor.getBoxes(player, direction).getFirst();
			skip = ParkourTags.Blocks.COMMON_IGNORED_BLOCKS;
		}
		if (hasStaticCollision(player, box, skip)) {
			Vec3 neighbor = player.blockPosition().relative(direction).getCenter();
			double dx = player.getX() - neighbor.x, dz = player.getZ() - neighbor.z;
			return new Candidate(direction, Vec3.atLowerCornerOf(direction.getNormal()),
					false, dx * dx + dz * dz);
		}
		DynamicBlockHit hit = hanging
				? DynamicCollisions.findSensorCollision(player.level(), player, box, skip,
						source -> true, candidate -> facesPlayer(player, direction, candidate, box, skip))
				: DynamicCollisions.findRelaxedSensorCollision(player.level(), player, box,
						direction, ParkourConstants.DYNAMIC_WALL_SENSOR_TOLERANCE, skip,
						source -> true, candidate -> facesPlayer(player, direction, candidate,
							relaxedWallQuery(player, direction, box, candidate.source()), skip));
		if (hit == null) return null;
		DynamicCollisionSource source = hit.source();
		Vec3 pivot = player.position(), local = source.toLocal(pivot);
		var sensorTransform = source.sensorTransform(player);
		Vec3 sensorFacing = sensorTransform.apply(pivot.add(Vec3.atLowerCornerOf(direction.getNormal())))
				.subtract(sensorTransform.apply(pivot));
		Vec3 localFacing = source.toLocal(pivot.add(sensorFacing)).subtract(local);
		Direction face = Direction.getNearest(localFacing.x, localFacing.y, localFacing.z);
		Vec3 normal = source.toWorld(local.add(Vec3.atLowerCornerOf(face.getNormal())))
				.subtract(source.toWorld(local)).normalize();
		if (normal.horizontalDistanceSqr() < 1.0E-8) return null;
		Vec3 horizontalNormal = new Vec3(normal.x, 0, normal.z).normalize();
		double gap = CollisionGeometry.faceDistance(local, hit.localBox(), face);
		return new Candidate(direction, horizontalNormal, true, Math.max(0, gap) * Math.max(0, gap));
	}

	private static AABB relaxedWallQuery(Player player, Direction direction, AABB box, DynamicCollisionSource source) {
		double reach = source.wallSensorTolerance(player, direction, ParkourConstants.DYNAMIC_WALL_SENSOR_TOLERANCE);
		if (!Double.isFinite(reach) || reach <= 0) return box;
		reach = Math.min(reach, ParkourConstants.DYNAMIC_WALL_SENSOR_TOLERANCE);
		return box.expandTowards(direction.getStepX() * reach, 0, direction.getStepZ() * reach);
	}

	private static boolean facesPlayer(Player player, Direction direction, DynamicBlockHit hit,
			AABB query, @Nullable TagKey<Block> skip) {
		DynamicCollisionSource source = hit.source();
		Vec3 pivot = player.position(), local = source.toLocal(pivot);
		var transform = source.sensorTransform(player);
		Vec3 toward = transform.apply(pivot.add(Vec3.atLowerCornerOf(direction.getNormal())))
				.subtract(transform.apply(pivot));
		Vec3 localToward = source.toLocal(pivot.add(toward)).subtract(local);
		Direction face = Direction.getNearest(localToward.x, localToward.y, localToward.z);
		// An overlapping side probe does not create a new face on a broad ledge.
		if (CollisionGeometry.faceDistance(local, hit.localBox(), face)
				< -ParkourConstants.AABB_DEFLATE_EPSILON) return false;
		List<AABB> occluders = new ArrayList<>();
		// Adjacent voxels hide internal faces, including unions of partial shapes.
		source.findCollision(outsideFace(hit.localBox(), face), source::toWorld, player, skip, neighbor -> {
			occluders.add(neighbor.localBox());
			return false;
		});
		return CollisionGeometry.hasExposedFace(query, point -> source.toLocal(transform.apply(point)),
				hit.localBox(), face, occluders);
	}

	private static AABB outsideFace(AABB box, Direction toward) {
		double depth = 2.0E-4;
		return switch (toward) {
			case EAST -> new AABB(box.minX - depth, box.minY, box.minZ, box.minX, box.maxY, box.maxZ);
			case WEST -> new AABB(box.maxX, box.minY, box.minZ, box.maxX + depth, box.maxY, box.maxZ);
			case SOUTH -> new AABB(box.minX, box.minY, box.minZ - depth, box.maxX, box.maxY, box.minZ);
			case NORTH -> new AABB(box.minX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ + depth);
			case UP -> new AABB(box.minX, box.minY - depth, box.minZ, box.maxX, box.minY, box.maxZ);
			case DOWN -> new AABB(box.minX, box.maxY, box.minZ, box.maxX, box.maxY + depth, box.maxZ);
		};
	}

	private static boolean hasStaticCollision(Player player, AABB box, @Nullable TagKey<Block> skip) {
		return new FilteredBlockCollisions<>(player.level(), player, box, false, skip,
				(pos, shape) -> shape).hasNext();
	}

	private static Vec3 horizontalLook(Player player) {
		return Vec3.directionFromRotation(0, player.getYRot());
	}

	private record Candidate(Direction direction, Vec3 normal, boolean dynamic, double distanceSquared) {}
}
