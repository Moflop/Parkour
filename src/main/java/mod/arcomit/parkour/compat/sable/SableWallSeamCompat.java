package mod.arcomit.parkour.compat.sable;

import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Resolve tiny wall-plane mismatches before vanilla clips movement along the seam. */
public final class SableWallSeamCompat {
	private static final double SEAM_SKIN = 0.002;
	private static final double EPSILON = 1.0E-7;

	private SableWallSeamCompat() {}

	/** Sable may clip the tangent before vanilla ever sees the requested movement. */
	public static Vec3 slideDynamic(Entity entity, Vec3 requested, Vec3 dynamicMotion) {
		if (!isLocalParkourPlayer(entity)) return dynamicMotion;
		if (Math.abs(requested.x - dynamicMotion.x) <= EPSILON
				&& Math.abs(requested.y - dynamicMotion.y) <= EPSILON
				&& Math.abs(requested.z - dynamicMotion.z) <= EPSILON) return dynamicMotion;
		AABB bounds = entity.getBoundingBox();
		List<VoxelShape> entities = entity.level().getEntityCollisions(entity,
				bounds.expandTowards(dynamicMotion).inflate(SEAM_SKIN));
		Vec3 clipped = Entity.collideBoundingBox(entity, dynamicMotion, bounds, entity.level(), entities);
		Vec3 vertical = slideOffStackedSeam(entity, requested, clipped);
		Vec3 repaired = vertical.equals(clipped) ? slide(entity, requested, clipped) : vertical;
		// Vanilla still needs the unresolved vertical input to establish onGround.
		return repaired.equals(clipped) ? dynamicMotion
				: new Vec3(repaired.x, vertical.equals(clipped) ? dynamicMotion.y : repaired.y, repaired.z);
	}

	private static boolean isLocalParkourPlayer(Entity entity) {
		return entity instanceof Player player && player.isLocalPlayer()
				&& !ParkourChecks.isVanillaState(ParkourContext.get(player))
				&& ParkourChecks.canPerformBehavior(player);
	}

	/** Clear microscopic lips both above the head and below the feet. */
	public static Vec3 slideOffStackedSeam(Entity entity, Vec3 requested, Vec3 clipped) {
		double verticalSign = Math.signum(requested.y);
		if (!isLocalParkourPlayer(entity) || Math.abs(requested.y) <= EPSILON
				|| Math.abs(requested.y - clipped.y) <= EPSILON) return clipped;
		// A real step rises against downward input. Only microscopic lip lifts
		// belong to seam recovery; preserve the solver's full-height step.
		if (requested.y < 0 && clipped.y > SEAM_SKIN) return clipped;
		AABB bounds = entity.getBoundingBox();
		List<VoxelShape> entities = entity.level().getEntityCollisions(entity,
				bounds.expandTowards(requested).inflate(SEAM_SKIN));
		for (Vec3 outward : new Vec3[]{new Vec3(-SEAM_SKIN, 0, 0), new Vec3(SEAM_SKIN, 0, 0),
				new Vec3(0, 0, -SEAM_SKIN), new Vec3(0, 0, SEAM_SKIN)}) {
			// Probe the body side above the feet, so a dynamic floor alone cannot
			// turn an ordinary static platform edge into a wall seam.
			AABB side = bounds.deflate(EPSILON, Math.min(.05, bounds.getYsize() / 4), EPSILON)
					.move(outward.scale(-1));
			if (!hasWallContinuation(entity, side, requested)) continue;
			Vec3 allowed = Entity.collideBoundingBox(entity, outward, bounds, entity.level(), entities);
			if (allowed.distanceToSqr(outward) > EPSILON * EPSILON
					|| DynamicCollisions.hasBlockCollision(entity.level(), entity,
					bounds.move(outward).deflate(EPSILON), null)) continue;
			// Keep the resolved tangent and remove only input back into the wall.
			Vec3 tangent = outward.x != 0 ? new Vec3(0, requested.y, clipped.z)
					: new Vec3(clipped.x, requested.y, 0);
			AABB shifted = bounds.move(outward);
			Vec3 candidate = Entity.collideBoundingBox(entity, tangent, shifted, entity.level(), entities);
			// Sable can also push the feet slightly UP onto the lower wall's lip
			// during a jump. Recover the requested path, not that artificial support.
			boolean restoresVertical = Math.abs(candidate.y - requested.y) <= EPSILON;
			if (!restoresVertical && verticalSign * (candidate.y - clipped.y) <= EPSILON) continue;
			if (DynamicCollisions.hasBlockCollision(entity.level(), entity,
					shifted.expandTowards(0, candidate.y, 0).deflate(EPSILON), null)
					|| DynamicCollisions.hasBlockCollision(entity.level(), entity,
					shifted.move(0, candidate.y, 0).expandTowards(candidate.x, 0, candidate.z)
							.deflate(EPSILON), null)) continue;
			return candidate.add(outward);
		}
		return clipped;
	}

	public static Vec3 slide(Entity entity, Vec3 requested, Vec3 clipped) {
		if (!isLocalParkourPlayer(entity)) return clipped;
		boolean blockedX = Math.abs(requested.x - clipped.x) > EPSILON;
		boolean blockedZ = Math.abs(requested.z - clipped.z) > EPSILON;
		if (!blockedX && !blockedZ) return clipped;
		AABB bounds = entity.getBoundingBox();
		for (int sign : new int[]{-1, 1}) {
			if (blockedZ) {
				Vec3 candidate = trySlide(entity, bounds, requested, clipped, new Vec3(sign * SEAM_SKIN, 0, 0));
				if (candidate != null) return candidate;
			}
			if (blockedX) {
				Vec3 candidate = trySlide(entity, bounds, requested, clipped, new Vec3(0, 0, sign * SEAM_SKIN));
				if (candidate != null) return candidate;
			}
		}
		return clipped;
	}

	private static Vec3 trySlide(Entity entity, AABB bounds, Vec3 requested, Vec3 clipped, Vec3 outward) {
		// Keep the solved vertical movement, including support from a dynamic deck.
		Vec3 tangent = outward.x != 0 ? new Vec3(0, clipped.y, requested.z)
				: new Vec3(requested.x, clipped.y, 0);
		AABB inward = bounds.move(outward.scale(-1)).deflate(EPSILON);
		// The current wall can be static when entering the structure. Require both
		// side contact and a dynamic continuation within the requested tangent path.
		if (!DynamicCollisions.hasBlockCollision(entity.level(), entity,
				inward, null)
				&& !entity.level().getBlockCollisions(entity, inward).iterator().hasNext()) return null;
		if (!DynamicCollisions.hasBlockCollision(entity.level(), entity,
				inward.expandTowards(tangent.x, 0, tangent.z), null)) return null;
		List<VoxelShape> entities = entity.level().getEntityCollisions(entity,
				bounds.expandTowards(requested).inflate(SEAM_SKIN));
		Vec3 allowedOutward = Entity.collideBoundingBox(entity, outward, bounds, entity.level(), entities);
		if (allowedOutward.distanceToSqr(outward) > EPSILON * EPSILON) return null;
		if (DynamicCollisions.hasBlockCollision(entity.level(), entity,
				bounds.move(outward).deflate(EPSILON), null)) return null;
		AABB shifted = bounds.move(outward);
		// Resolve only the tangent: replaying input into the wall would consume the
		// outward clearance again, with different results on the two wall faces.
		Vec3 along = Entity.collideBoundingBox(entity, tangent, shifted, entity.level(), entities);
		double progress = outward.x != 0 ? Math.abs(along.z) - Math.abs(clipped.z)
				: Math.abs(along.x) - Math.abs(clipped.x);
		if (progress <= EPSILON || Math.abs(along.y - clipped.y) > EPSILON) return null;
		// Keep all dynamic walls in the clearance query; never discard a contact
		// normal or replace a block's shape with a fabricated continuation.
		if (DynamicCollisions.hasBlockCollision(entity.level(), entity,
				shifted.expandTowards(0, along.y, 0).deflate(EPSILON), null)
				|| DynamicCollisions.hasBlockCollision(entity.level(), entity,
				shifted.move(0, along.y, 0).expandTowards(along.x, 0, along.z).deflate(EPSILON), null)) return null;
		return along.add(outward);
	}

	private static boolean hasWallContinuation(Entity entity, AABB side, Vec3 tangent) {
		return (DynamicCollisions.hasBlockCollision(entity.level(), entity, side, null)
				|| entity.level().getBlockCollisions(entity, side).iterator().hasNext())
				&& DynamicCollisions.hasBlockCollision(entity.level(), entity,
						side.expandTowards(tangent), null);
	}
}
