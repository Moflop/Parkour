package mod.arcomit.parkour.compat.sable;

import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import mod.arcomit.parkour.compat.CollisionGeometry;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.utils.FilteredBlockCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.function.UnaryOperator;
import java.util.function.Predicate;

/** Read-only clearance checks for a native step before its motion is accepted. */
public final class SableStepClearance {
	private static final double CONTACT_EPSILON = 1.0E-6;
	private static final double MAX_STEP_RETREAT = 0.002;

	private SableStepClearance() {}

	/** The original movement broad phase does not include the head after stepping up. */
	public static Iterable<BlockPos> candidateBlocks(Pose3dc pose, OrientedBoundingBox3d body,
			Vector3dc center) {
		Vector3dc dimensions = body.getDimensions();
		double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
		double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
		for (int corner = 0; corner < 8; corner++) {
			Vector3d position = new Vector3d(
					((corner & 1) == 0 ? -0.5 : 0.5) * dimensions.x(),
					((corner & 2) == 0 ? -0.5 : 0.5) * dimensions.y(),
					((corner & 4) == 0 ? -0.5 : 0.5) * dimensions.z());
			body.getOrientation().transform(position);
			pose.transformPositionInverse(position.add(center));
			minX = Math.min(minX, position.x); maxX = Math.max(maxX, position.x);
			minY = Math.min(minY, position.y); maxY = Math.max(maxY, position.y);
			minZ = Math.min(minZ, position.z); maxZ = Math.max(maxZ, position.z);
		}
		// Include collision shapes which extend beyond the block that owns them.
		return BlockPos.betweenClosed(Mth.floor(minX) - 1, Mth.floor(minY) - 1,
				Mth.floor(minZ) - 1, Mth.floor(maxX) + 1, Mth.floor(maxY) + 1,
				Mth.floor(maxZ) + 1);
	}

	/**
	 * Validate the body's upward leg and subsequent horizontal leg in its own frame.
	 * The caller supplies the ordinary, unstepped collision result as the base.
	 * This avoids treating the step itself as a blockage on a diagonal swept box.
	 */
	public static boolean isStepClear(Entity entity, AABB dimensionsBox, Quaterniondc orientation,
			Vector3dc baseCenter, Vector3dc proposedCenter, LevelAccelerator accel,
			SubLevel currentSubLevel, Pose3dc currentPose) {
		Quaterniond rotation = new Quaterniond(orientation);
		Vector3d origin = new Vector3d(baseCenter);
		Vector3d localMotion = new Vector3d(proposedCenter).sub(baseCenter);
		rotation.transformInverse(localMotion);
		double halfX = dimensionsBox.getXsize() * 0.5;
		double halfY = dimensionsBox.getYsize() * 0.5;
		double halfZ = dimensionsBox.getZsize() * 0.5;
		AABB body = new AABB(-halfX, -halfY, -halfZ, halfX, halfY, halfZ)
				.deflate(CONTACT_EPSILON);
		UnaryOperator<Vec3> toWorld = point -> {
			Vector3d world = rotation.transform(new Vector3d(point.x, point.y, point.z)).add(origin);
			return new Vec3(world.x, world.y, world.z);
		};
		return isStepPathClear(body, localMotion,
				path -> hasCollision(entity, path, toWorld, accel, currentSubLevel, currentPose));
	}

	/**
	 * A nearly vertical step can lean over a body already touching its lower side.
	 * Try a tiny retreat before rising, then return to the exact native destination.
	 * Every leg keeps the full body and all obstacles in the clearance query.
	 */
	static boolean isStepPathClear(AABB body, Vector3dc motion, Predicate<AABB> collides) {
		if (!collides.test(body.expandTowards(0, motion.y(), 0))
				&& !collides.test(body.move(0, motion.y(), 0)
						.expandTowards(motion.x(), 0, motion.z()))) return true;
		double horizontalDistance = Math.hypot(motion.x(), motion.z());
		if (motion.y() <= 0 || horizontalDistance <= CONTACT_EPSILON) return false;
		double outwardX = -motion.x() / horizontalDistance * MAX_STEP_RETREAT;
		double outwardZ = -motion.z() / horizontalDistance * MAX_STEP_RETREAT;
		if (collides.test(body.expandTowards(outwardX, 0, outwardZ))) return false;
		AABB outside = body.move(outwardX, 0, outwardZ);
		if (collides.test(outside.expandTowards(0, motion.y(), 0))) return false;
		return !collides.test(outside.move(0, motion.y(), 0)
				.expandTowards(motion.x() - outwardX, 0, motion.z() - outwardZ));
	}

	private static boolean hasCollision(Entity entity, AABB body, UnaryOperator<Vec3> toWorld,
			LevelAccelerator accel, SubLevel currentSubLevel, Pose3dc currentPose) {
		AABB broadPhase = CollisionGeometry.transformBounds(body, toWorld);
		FilteredBlockCollisions<VoxelShape> staticShapes = new FilteredBlockCollisions<>(
				entity.level(), entity, broadPhase, false, null, (pos, shape) -> shape);
		while (staticShapes.hasNext()) {
			for (AABB staticBox : staticShapes.next().toAabbs()) {
				if (CollisionGeometry.intersects(body, toWorld, staticBox, UnaryOperator.identity())) {
					return true;
				}
			}
		}
		if (hasCurrentSubLevelCollision(body, toWorld, accel, currentSubLevel, currentPose)) return true;
		return DynamicCollisions.hasTransformedBlockCollision(entity.level(), entity, body, toWorld, null,
				source -> !source.identity().equals(currentSubLevel.getUniqueId()));
	}

	/** Match the pose used by the active solver substep, rather than the end-of-tick pose. */
	private static boolean hasCurrentSubLevelCollision(AABB body, UnaryOperator<Vec3> toWorld,
			LevelAccelerator accel, SubLevel currentSubLevel, Pose3dc currentPose) {
		BoundingBox3ic plotBounds = currentSubLevel.getPlot().getBoundingBox();
		if (currentSubLevel.isRemoved() || plotBounds == null) return false;
		AABB localBounds = CollisionGeometry.transformBounds(body, point -> {
			Vec3 world = toWorld.apply(point);
			Vector3d local = currentPose.transformPositionInverse(new Vector3d(world.x, world.y, world.z));
			return new Vec3(local.x, local.y, local.z);
		});
		int minX = Math.max(plotBounds.minX(), Mth.floor(localBounds.minX) - 1);
		int minY = Math.max(plotBounds.minY(), Mth.floor(localBounds.minY) - 1);
		int minZ = Math.max(plotBounds.minZ(), Mth.floor(localBounds.minZ) - 1);
		int maxX = Math.min(plotBounds.maxX(), Mth.floor(localBounds.maxX) + 1);
		int maxY = Math.min(plotBounds.maxY(), Mth.floor(localBounds.maxY) + 1);
		int maxZ = Math.min(plotBounds.maxZ(), Mth.floor(localBounds.maxZ) + 1);
		if (minX > maxX || minY > maxY || minZ > maxZ) return false;
		UnaryOperator<Vec3> blockToWorld = point -> {
			Vector3d world = currentPose.transformPosition(new Vector3d(point.x, point.y, point.z));
			return new Vec3(world.x, world.y, world.z);
		};
		for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
			var state = accel.getBlockState(pos);
			if (state.isAir()) continue;
			for (AABB box : state.getCollisionShape(accel, pos).toAabbs()) {
				if (CollisionGeometry.intersects(body, toWorld, box.move(pos), blockToWorld)) return true;
			}
		}
		return false;
	}
}
