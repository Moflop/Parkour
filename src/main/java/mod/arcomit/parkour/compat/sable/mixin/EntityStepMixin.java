package mod.arcomit.parkour.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import mod.arcomit.parkour.compat.sable.SableWallSeamCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.compat.sable.SableStepHeightCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Keeps vanilla's second-stage step from entering a dynamic ceiling. */
@Mixin(Entity.class)
public abstract class EntityStepMixin {
	@WrapMethod(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")
	private Vec3 parkour$slideAcrossWallSeam(Vec3 requested, Operation<Vec3> original) {
		Entity entity = (Entity) (Object) this;
		Vec3 clipped = original.call(requested);
		Vec3 falling = SableWallSeamCompat.slideOffStackedSeam(entity, requested, clipped);
		return falling.equals(clipped) ? SableWallSeamCompat.slide(entity, requested, clipped) : falling;
	}

	@Unique
	private static final double PARKOUR_STEP_CLEARANCE_EPSILON = 1.0E-7;

	@WrapOperation(
			method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;")
	)
	private Vec3 parkour$checkDynamicStepClearance(Vec3 requestedMotion, AABB bounds,
			List<VoxelShape> shapes, Operation<Vec3> original) {
		Vec3 candidate = original.call(requestedMotion, bounds, shapes);
		Entity entity = (Entity) (Object) this;
		if (candidate.y <= 0.0 || !SableStepHeightCompat.isEnabled(entity)) return candidate;

		// Follow vanilla's upward-then-horizontal route. A single diagonal sweep
		// would include the step itself and incorrectly reject a clear staircase.
		AABB upwardPath = bounds.expandTowards(0.0, candidate.y, 0.0)
				.deflate(PARKOUR_STEP_CLEARANCE_EPSILON);
		AABB raisedBounds = bounds.move(0.0, candidate.y, 0.0);
		boolean zFirst = Math.abs(requestedMotion.x) < Math.abs(requestedMotion.z);
		double firstX = zFirst ? 0.0 : candidate.x;
		double firstZ = zFirst ? candidate.z : 0.0;
		AABB firstHorizontalPath = raisedBounds.expandTowards(firstX, 0.0, firstZ)
				.deflate(PARKOUR_STEP_CLEARANCE_EPSILON);
		AABB secondHorizontalPath = raisedBounds.move(firstX, 0.0, firstZ)
				.expandTowards(candidate.x - firstX, 0.0, candidate.z - firstZ)
				.deflate(PARKOUR_STEP_CLEARANCE_EPSILON);
		if (DynamicCollisions.hasBlockCollision(entity.level(), entity, upwardPath, null)
				|| DynamicCollisions.hasBlockCollision(entity.level(), entity, firstHorizontalPath, null)
				|| DynamicCollisions.hasBlockCollision(entity.level(), entity, secondHorizontalPath, null)) {
			// Zero horizontal progress cannot win against vanilla's non-step result.
			return Vec3.ZERO;
		}
		return candidate;
	}
}
