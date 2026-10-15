package mod.arcomit.parkour.compat.create;

import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider.CollisionResponse;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import mod.arcomit.parkour.compat.DynamicCollisionSource;
import mod.arcomit.parkour.compat.create.mixin.Matrix3dAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Immediate collision for extra parkour movement, before Create's end-of-tick collider. */
public final class CreateCorrectionCollision {
    private static final double EPSILON = 1.0E-7;
    private static final double SKIN = 1.0E-5;
    private static final double MAX_SWEEP_LENGTH = 0.25;
    private static final double MAX_SERVER_SEPARATION = 1.0;
    private static final int MAX_CONTACTS = 4;

    private CreateCorrectionCollision() {}

    /** Returns only allowed input movement; native recovery and carrier motion remain Create's. */
    public static Vec3 clip(Player player, Vec3 worldMotion) {
        // Server travel can integrate Create's synced velocity without its client-only collider.
        if (!(player.isLocalPlayer() || player instanceof ServerPlayer)
                || player.noPhysics || worldMotion.lengthSqr() < EPSILON * EPSILON) {
            return worldMotion;
        }
        AABB originalBounds = player.getBoundingBox();
        AABB bounds = originalBounds.deflate(SKIN);
        AABB search = bounds.inflate(worldMotion.length() + player.getBbWidth());
        List<Target> targets = targets(player, bounds, search);
        if (targets.isEmpty()) return worldMotion;
        List<UnaryOperator<Vec3>> dynamicClippers = new ArrayList<>(targets.size());
        for (Target target : targets) dynamicClippers.add(target::clip);
        return clipCombined(worldMotion, motion -> Entity.collideBoundingBox(player, motion,
                originalBounds, player.level(), player.level().getEntityCollisions(player,
                        originalBounds.expandTowards(motion))), dynamicClippers);
    }

    static Vec3 clipCombined(Vec3 motion, UnaryOperator<Vec3> staticClip,
            List<UnaryOperator<Vec3>> dynamicClippers) {
        if (dynamicClippers.isEmpty()) return motion;
        Vec3 allowed = motion;
        // A later static collision can shorten a slide around a finite dynamic wall.
        // Every solver must therefore accept the same displacement from the original body.
        for (int pass = 0; pass < MAX_CONTACTS; pass++) {
            Vec3 before = allowed;
            allowed = staticClip.apply(allowed);
            for (UnaryOperator<Vec3> dynamicClip : dynamicClippers) allowed = dynamicClip.apply(allowed);
            if (allowed.distanceToSqr(before) < EPSILON * EPSILON) break;
        }
        // Check after the final pass too: it may have reached the fixed point at the limit.
        if (staticClip.apply(allowed).distanceToSqr(allowed) >= EPSILON * EPSILON) return Vec3.ZERO;
        for (UnaryOperator<Vec3> dynamicClip : dynamicClippers) {
            if (dynamicClip.apply(allowed).distanceToSqr(allowed) >= EPSILON * EPSILON) return Vec3.ZERO;
        }
        return allowed.distanceToSqr(motion) < EPSILON * EPSILON ? motion : allowed;
    }

    /** Align server sensor positions with the current structure pose, without replaying transport. */
    public static void resolveServerPenetration(Player player) {
        if (!(player instanceof ServerPlayer) || player.noPhysics || player.isPassenger()) return;
        AABB bounds = player.getBoundingBox().deflate(SKIN);
        List<Target> targets = targets(player, bounds,
                bounds.inflate(MAX_SERVER_SEPARATION + player.getBbWidth()));
        Vec3 separation = Vec3.ZERO;
        for (int pass = 0; pass < MAX_CONTACTS; pass++) {
            Vec3 before = separation;
            for (Target target : targets) {
                if (!target.overlaps(player, bounds, separation)) continue;
                OrientedBB shifted = target.body().copy();
                shifted.move(target.toLocalDisplacement(separation));
                Vec3 local = separationAgainstShapes(shifted, target.shapes(), MAX_SERVER_SEPARATION);
                separation = separation.add(target.toWorldDisplacement(local));
                if (!withinSeparationLimit(separation, MAX_SERVER_SEPARATION)) return;
            }
            if (separation.distanceToSqr(before) < EPSILON * EPSILON) break;
        }
        if (separation.lengthSqr() < EPSILON * EPSILON) return;
        for (Target target : targets) {
            if (target.overlaps(player, bounds, separation)) return;
        }
        // Use vanilla's complete static/entity sweep before applying the dynamic-only recovery.
        Vec3 unobstructed = Entity.collideBoundingBox(player, separation, bounds, player.level(),
                player.level().getEntityCollisions(player, bounds.expandTowards(separation)));
        if (unobstructed.distanceToSqr(separation) > EPSILON * EPSILON
                || !player.level().noCollision(player, bounds.move(separation))) return;
        player.setPos(player.position().add(separation));
    }

    private static List<Target> targets(Player player, AABB bounds, AABB search) {
        List<Target> targets = new ArrayList<>();
        for (DynamicCollisionSource source : new CreateCollisionProvider().sources(player.level(), search)) {
            if (!source.isValid()) continue;
            CollisionList shapes = new CollisionList();
            CollisionList.Populate populate = new CollisionList.Populate(shapes);
            source.findCollision(search, UnaryOperator.identity(), player, null, hit -> {
                AABB box = hit.localBox();
                populate.consume(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                return false;
            });
            if (shapes.size == 0) continue;
            UnaryOperator<Vec3> sensor = source.sensorTransform(player);
            Vec3 worldCenter = sensor.apply(bounds.getCenter());
            Vec3 localCenter = source.toLocal(worldCenter);
            UnaryOperator<Vec3> sensorToLocal = point -> source.toLocal(sensor.apply(point));
            Vec3 pivot = bounds.getCenter();
            Matrix3d rotation = matrix(
                    sensorToLocal.apply(pivot.add(1, 0, 0)).subtract(localCenter).normalize(),
                    sensorToLocal.apply(pivot.add(0, 1, 0)).subtract(localCenter).normalize(),
                    sensorToLocal.apply(pivot.add(0, 0, 1)).subtract(localCenter).normalize());
            OrientedBB body = new OrientedBB(localCenter,
                    new Vec3(bounds.getXsize(), bounds.getYsize(), bounds.getZsize()).scale(0.5), rotation);
            targets.add(new Target(source, body, shapes, worldCenter));
        }
        return targets;
    }

    static Vec3 separationAgainstShapes(OrientedBB originalBody, CollisionList shapes, double maximum) {
        OrientedBB body = originalBody.copy();
        CollisionList scratch = new CollisionList();
        Vec3 separation = Vec3.ZERO;
        for (int pass = 0; pass < MAX_CONTACTS; pass++) {
            Vec3 correction = collide(shapes, scratch, body, Vec3.ZERO).collisionResponse;
            if (correction.lengthSqr() < EPSILON * EPSILON) return separation;
            separation = separation.add(correction);
            if (!withinSeparationLimit(separation, maximum)) return Vec3.ZERO;
            body.move(correction);
        }
        return collide(shapes, scratch, body, Vec3.ZERO).collisionResponse.lengthSqr() < EPSILON * EPSILON
                ? separation : Vec3.ZERO;
    }

    private static boolean withinSeparationLimit(Vec3 separation, double maximum) {
        return Double.isFinite(separation.x) && Double.isFinite(separation.y) && Double.isFinite(separation.z)
                && separation.lengthSqr() <= maximum * maximum;
    }

    static Vec3 clipAgainstShapes(OrientedBB originalBody, CollisionList shapes, Vec3 motion) {
        if (shapes.size == 0 || motion.lengthSqr() < EPSILON * EPSILON) return motion;
        CollisionList scratch = new CollisionList();
        OrientedBB body = originalBody.copy();
        CollisionResponse initial = collide(shapes, scratch, body, Vec3.ZERO);
        // Query from the separated body, but never add this native recovery to the player's move.
        body.move(initial.collisionResponse);
        int count = Math.max(1, (int) Math.ceil(motion.length() / MAX_SWEEP_LENGTH));
        Vec3 segment = motion.scale(1.0 / count);
        Vec3 total = Vec3.ZERO;
        // Create's broad phase tests the endpoint. Short continuous sweeps also cover thin
        // obstacles midway through a long structure-following displacement.
        for (int part = 0; part < count; part++) {
            Vec3 travel = sweep(shapes, scratch, body, segment);
            total = total.add(travel);
        }
        return total.distanceToSqr(motion) < EPSILON * EPSILON ? motion : total;
    }

    private static Vec3 sweep(CollisionList shapes, CollisionList scratch, OrientedBB body, Vec3 motion) {
        Vec3 remaining = motion;
        Vec3 travelled = Vec3.ZERO;
        for (int contact = 0; contact < MAX_CONTACTS && remaining.lengthSqr() > EPSILON * EPSILON; contact++) {
            CollisionResponse hit = collide(shapes, scratch, body, remaining);
            if (hit.collisionResponse.lengthSqr() > EPSILON * EPSILON) {
                // Multiple contact planes contribute to this recovery. It is not a
                // surface normal; sweep again to obtain the individual impact normal.
                body.move(hit.collisionResponse);
                continue;
            }
            if (hit.temporalResponse >= 1) {
                body.move(remaining);
                return travelled.add(remaining);
            }
            double fraction = Math.max(0, hit.temporalResponse - SKIN / remaining.length());
            Vec3 advance = remaining.scale(fraction);
            body.move(advance);
            travelled = travelled.add(advance);
            Vec3 tail = remaining.scale(1 - fraction);
            remaining = removeInward(tail, hit.normal);
            if (remaining.distanceToSqr(tail) < EPSILON * EPSILON) break;
        }
        return travelled;
    }

    private static CollisionResponse collide(CollisionList shapes, CollisionList scratch, OrientedBB body, Vec3 motion) {
        return ContinuousOBBCollider.collideMany(shapes, scratch, body, motion, 0, false);
    }

    private static Vec3 removeInward(Vec3 motion, Vec3 normal) {
        double lengthSquared = normal.lengthSqr();
        if (lengthSquared < EPSILON * EPSILON) return motion;
        double dot = motion.dot(normal);
        return dot < 0 ? motion.subtract(normal.scale(dot / lengthSquared)) : motion;
    }

    private static Matrix3d matrix(Vec3 x, Vec3 y, Vec3 z) {
        Matrix3d matrix = new Matrix3d();
        Matrix3dAccessor access = (Matrix3dAccessor) matrix;
        access.parkour$setM00(x.x);
        access.parkour$setM10(x.y);
        access.parkour$setM20(x.z);
        access.parkour$setM01(y.x);
        access.parkour$setM11(y.y);
        access.parkour$setM21(y.z);
        access.parkour$setM02(z.x);
        access.parkour$setM12(z.y);
        access.parkour$setM22(z.z);
        return matrix;
    }

    private record Target(DynamicCollisionSource source, OrientedBB body, CollisionList shapes, Vec3 worldCenter) {
        private Vec3 clip(Vec3 motion) {
            Vec3 localMotion = toLocalDisplacement(motion);
            Vec3 localAllowed = clipAgainstShapes(body, shapes, localMotion);
            if (localAllowed.distanceToSqr(localMotion) < EPSILON * EPSILON) return motion;
            return toWorldDisplacement(localAllowed);
        }

        private Vec3 toLocalDisplacement(Vec3 world) {
            return source.toLocal(worldCenter.add(world)).subtract(body.getCenter());
        }

        private Vec3 toWorldDisplacement(Vec3 local) {
            return source.toWorld(body.getCenter().add(local)).subtract(source.toWorld(body.getCenter()));
        }

        private boolean overlaps(Player player, AABB bounds, Vec3 offset) {
            UnaryOperator<Vec3> sensor = source.sensorTransform(player);
            return source.findCollision(bounds, point -> sensor.apply(point).add(offset), player, null) != null;
        }
    }
}
