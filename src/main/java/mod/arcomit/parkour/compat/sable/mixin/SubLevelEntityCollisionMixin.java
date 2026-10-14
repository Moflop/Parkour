package mod.arcomit.parkour.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import mod.arcomit.parkour.compat.sable.SableMovementCompat;
import mod.arcomit.parkour.compat.sable.SableStepClearance;
import mod.arcomit.parkour.compat.sable.SableStepHeightCompat;
import mod.arcomit.parkour.compat.sable.SableWallSeamCompat;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Quaterniond;

/** Keep the same wall sprint behavior when Sable performs collision resolution. */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
public abstract class SubLevelEntityCollisionMixin {
	@WrapMethod(method = "collide", remap = false)
	private static SubLevelEntityCollision.CollisionInfo parkour$slideAcrossStaticWallSeam(
			Entity entity, Vec3 requested, Vec3 velocity, LevelReusedVectors sink,
			Operation<SubLevelEntityCollision.CollisionInfo> original) {
		Vec3 beforeVelocity = entity.getDeltaMovement();
		SubLevel beforeTracking = ((EntityMovementExtension) entity).sable$getTrackingSubLevel();
		SubLevelEntityCollision.CollisionInfo result = original.call(entity, requested, velocity, sink);
		Vec3 intended = beforeTracking == null ? requested.add(velocity) : requested;
		Vec3 repaired = SableWallSeamCompat.slideDynamic(entity, intended, result.motion);
		if (!repaired.equals(result.motion)) {
			Vec3 afterVelocity = entity.getDeltaMovement();
			// Sable also removes velocity along the rejected seam normal. Restore
			// only the tangent whose movement the clearance check actually recovered.
			entity.setDeltaMovement(
					Math.abs(repaired.x) > Math.abs(result.motion.x) + .002 ? beforeVelocity.x : afterVelocity.x,
					afterVelocity.y,
					Math.abs(repaired.z) > Math.abs(result.motion.z) + .002 ? beforeVelocity.z : afterVelocity.z);
			if (Math.abs(repaired.y - result.motion.y) > 1.0E-7
					&& Math.abs(repaired.y - intended.y) <= 1.0E-7) {
				// The checked outward route removed this lip contact. Leaving its
				// grounded flag set would zero jump velocity after movement, even
				// though the full requested vertical path was recovered.
				result.verticalCollision = false;
				result.verticalCollisionBelow = false;
				result.trackingSubLevel = beforeTracking;
				entity.setOnGround(false);
				entity.setDeltaMovement(entity.getDeltaMovement().x, beforeVelocity.y,
						entity.getDeltaMovement().z);
			}
			result.motion = repaired;
		}
		return result;
	}

	@WrapOperation(
			method = "collide",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSprinting(Z)V", remap = false),
			remap = false
	)
	private static void parkour$preserveWallSprint(Entity entity, boolean sprinting,
			Operation<Void> original) {
		if (!sprinting && entity instanceof Player player && player.isSprinting()
				&& !ParkourChecks.isVanillaState(ParkourContext.get(player))
				&& ParkourChecks.canPerformBehavior(player)) {
			return;
		}
		original.call(entity, sprinting);
	}

	/** Corrective moves resolve collisions at the player's current position. */
	@WrapOperation(
			method = "collide",
			at = @At(value = "INVOKE",
					target = "Ldev/ryanhcode/sable/companion/math/Pose3d;transformPosition(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;",
					ordinal = 0, remap = false),
			remap = false
	)
	private static Vector3d parkour$avoidRepeatedTrackingTransport(Pose3d currentPose,
			Vector3d localPosition, Operation<Vector3d> original,
			@Local(argsOnly = true) Entity entity, @Local(ordinal = 1) Pose3d previousPose) {
		if (SableMovementCompat.isCorrectionMove(entity)) {
			// The preceding inverse used this same previous substep pose. Reapply it
			// to restore the world point while keeping native tracking and SAT intact.
			return previousPose.transformPosition(localPosition);
		}
		return original.call(currentPose, localPosition);
	}

	/** Include ceiling blocks reached only by a raised candidate body. */
	@WrapOperation(method = "tryStepUp",
			at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision;hasCollision(Ldev/ryanhcode/sable/util/LevelAccelerator;Ldev/ryanhcode/sable/api/math/LevelReusedVectors;Ldev/ryanhcode/sable/companion/math/Pose3dc;Ljava/lang/Iterable;Ldev/ryanhcode/sable/api/math/OrientedBoundingBox3d;Ldev/ryanhcode/sable/api/math/OrientedBoundingBox3d;Lorg/joml/Vector3d;)Z", remap = false),
			remap = false)
	private static boolean parkour$includeRaisedBodyBlocks(LevelAccelerator accel, LevelReusedVectors sink,
			Pose3dc pose, Iterable<BlockPos> blocks, OrientedBoundingBox3d body,
			OrientedBoundingBox3d cube, Vector3d center, Operation<Boolean> original,
			@Local(argsOnly = true) Entity entity) {
		Iterable<BlockPos> candidates = SableStepHeightCompat.isEnabled(entity)
				? SableStepClearance.candidateBlocks(pose, body, center) : blocks;
		return original.call(accel, sink, pose, candidates, body, cube, center);
	}

	/** Reject unsafe proposals before the solver commits any upward displacement. */
	@WrapOperation(method = "collide",
			at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision;tryStepUp(Lnet/minecraft/world/entity/Entity;Ldev/ryanhcode/sable/util/LevelAccelerator;Ldev/ryanhcode/sable/api/math/LevelReusedVectors;Ldev/ryanhcode/sable/companion/math/Pose3dc;Ljava/lang/Iterable;Lorg/joml/Vector3dc;Lnet/minecraft/world/phys/AABB;Ldev/ryanhcode/sable/api/math/OrientedBoundingBox3d;Ldev/ryanhcode/sable/api/math/OrientedBoundingBox3d;Lorg/joml/Vector3dc;Lorg/joml/Vector3dc;Lorg/joml/Vector3d;)Z", remap = false),
			remap = false)
	private static boolean parkour$validateStepBeforeMovement(Entity entity, LevelAccelerator accel,
			LevelReusedVectors sink, Pose3dc pose, Iterable<BlockPos> blocks, Vector3dc center,
			AABB bounds, OrientedBoundingBox3d body, OrientedBoundingBox3d cube, Vector3dc mtv,
			Vector3dc normal, Vector3d motion, Operation<Boolean> original,
			@Local(name = "subLevel") SubLevel subLevel) {
		if (!SableStepHeightCompat.isEnabled(entity)) {
			return original.call(entity, accel, sink, pose, blocks, center, bounds, body, cube, mtv, normal, motion);
		}
		Vector3d before = new Vector3d(motion);
		Quaterniond orientation = new Quaterniond(body.getOrientation());
		Vector3d up = new Vector3d(sink.entityUpDirection);
		boolean stepped = original.call(entity, accel, sink, pose, blocks, center, bounds, body, cube, mtv, normal, motion);
		if (stepped && motion.y > before.y + 1.0E-7) {
			// Horizontal contact can be resolved before the smaller ground contact.
			// Query from the grounded reference height instead of treating unresolved
			// downward input as existing penetration. Native collision resolution
			// still owns the actual motion; the full head clearance remains checked.
			double pendingGroundCorrection = SableStepHeightCompat.groundedStepReferenceCorrection(
					before, mtv, up);
			Vector3d baseline = new Vector3d(center).add(before).add(mtv).fma(pendingGroundCorrection, up)
					.add(SableStepHeightCompat.groundedContactRetreat(pendingGroundCorrection, normal, up));
			Vector3d proposed = new Vector3d(center).add(motion).add(mtv).fma(pendingGroundCorrection, up);
			if (!SableStepClearance.isStepClear(entity, bounds, orientation, baseline, proposed,
					accel, subLevel, pose)) {
				motion.set(before);
				return false;
			}
		}
		return stepped;
	}

	/** Native padding rejects narrow openings that fit the player's actual pose. */
	@WrapOperation(
			method = "tryStepUp",
			at = @At(value = "INVOKE", target = "Lorg/joml/Vector3d;add(DDD)Lorg/joml/Vector3d;",
					remap = false),
			remap = false
	)
	private static Vector3d parkour$useActualStepHeight(Vector3d dimensions, double x, double y, double z,
			Operation<Vector3d> original, @Local(argsOnly = true) Entity entity) {
		// The full upward/horizontal sweep above validates the actual body against
		// this ship, other structures and vanilla blocks. Inflating the probe would
		// otherwise make a 0.6-wide slide require 0.7 blocks of horizontal space.
		return SableStepHeightCompat.isEnabled(entity)
				? original.call(dimensions, 0.0, 0.0, 0.0)
				: original.call(dimensions, x, y, z);
	}

	@WrapOperation(
			method = "tryStepUp",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;maxUpStep()F",
					remap = false),
			remap = false
	)
	private static float parkour$refreshSprintStepHeight(Entity entity, Operation<Float> original) {
		SableStepHeightCompat.refreshStepHeight(entity);
		return original.call(entity);
	}

	/** Include a non-grid maximum height without ever probing beyond that maximum. */
	@ModifyConstant(method = "tryStepUp", constant = @Constant(doubleValue = 0.0625, ordinal = 1),
			remap = false)
	private static double parkour$includeMaximumStepHeight(double increment,
			@Local(argsOnly = true) Entity entity, @Local(ordinal = 1) double maximumHeight,
			@Local(ordinal = 2) double currentHeight) {
		return SableStepHeightCompat.isEnabled(entity)
				? SableStepHeightCompat.stepIncrement(currentHeight, maximumHeight, increment) : increment;
	}
}
