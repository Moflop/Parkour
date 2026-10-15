package mod.arcomit.parkour.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider.CollisionResponse;
import com.simibubi.create.foundation.collision.OrientedBB;
import mod.arcomit.parkour.compat.CollisionGeometry;
import mod.arcomit.parkour.compat.DynamicCollisionSource;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.mechanic.stepheight.StepHeightIncreaseMechanic;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.utils.FilteredBlockCollisions;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.UnaryOperator;

/** Validates Create's proposals before they change position, grounding or velocity. */
public final class CreateMovementCompat {
    private static final double SKIN = 1.0E-5;
    private static final double SUPPORT_DEPTH = 0.03;
    private static final ThreadLocal<Player> PENDING_POSITION = new ThreadLocal<>();

    private CreateMovementCompat() {}

    public static boolean isEnabled(Entity entity) {
        return entity instanceof Player player
                && !ParkourChecks.isVanillaState(ParkourContext.get(player))
                && ParkourChecks.canPerformBehavior(player);
    }

    /** The normal movement packet must observe Create's final collision position. */
    public static boolean deferPosition(Player player) {
        if (!isEnabled(player) || !player.isLocalPlayer()
                || new CreateCollisionProvider().sources(player.level(),
                        player.getBoundingBox().inflate(2)).isEmpty()) return false;
        PENDING_POSITION.set(player);
        return true;
    }

    public static void flushPosition(Level level) {
        Player player = PENDING_POSITION.get();
        PENDING_POSITION.remove();
        if (player != null && player.level() == level && player.isAlive()) {
            ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
        }
    }

    public static CollisionResponse collide(Player player, AbstractContraptionEntity contraption,
            CollisionList shapes, CollisionList scratch, OrientedBB body, Vec3 motion,
            boolean horizontalPass) {
        DynamicCollisionSource source = CreateCollisionProvider.source(contraption);
        StepHeightIncreaseMechanic.handle(player);
        float maximumStep = player.maxUpStep();
        CollisionResponse flat = ContinuousOBBCollider.collideMany(shapes, scratch, body,
                motion, 0, horizontalPass);
        if (source == null || !source.isValid()) return flat;

        float yaw = contraption.getRotationState().getYawOffset();
        UnaryOperator<Vec3> vectorToWorld = vector -> VecHelper.rotate(
                body.getRotation().transformTransposed(vector), yaw, Direction.Axis.Y);
        Vec3 flatCorrection = vectorToWorld.apply(flat.collisionResponse);
        AABB bounds = player.getBoundingBox();
        UnaryOperator<Vec3> sensor = source.sensorTransform(player);
        UnaryOperator<Vec3> baseToWorld = point -> sensor.apply(point).add(flatCorrection);
        Vec3 relativeMotion = vectorToWorld.apply(motion);
        CollisionResponse result = flat;

        // Create has already moved the player into the obstacle. Start the step
        // sweep at the unstepped, separated body, not at that penetrating position.
        if (maximumStep > 0 && relativeMotion.y <= SKIN
                && hasSupport(player, footProbe(bounds), baseToWorld)) {
            CollisionResponse stepped = ContinuousOBBCollider.collideMany(shapes, scratch,
                    body, motion, maximumStep, horizontalPass);
            Vec3 step = vectorToWorld.apply(stepped.collisionResponse).subtract(flatCorrection);
            Vec3 queryStep = inverseVector(step, bounds.getCenter(), sensor);
            if (withinStepHeight(queryStep.y, maximumStep)
                    && stepPathClear(bounds.deflate(SKIN), queryStep, path ->
                            hasCollision(player, path, baseToWorld))) {
                result = stepped;
            }
        }

        Vec3 correction = vectorToWorld.apply(result.collisionResponse);
        UnaryOperator<Vec3> correctedToWorld = point -> sensor.apply(point).add(correction);
        // A vertical SAT pass can report a side wall as a surface. Only a real
        // upward face may reset fall distance or register the player as a rider.
        result.surfaceCollision = source.hasSupport(footProbe(bounds), correctedToWorld, player, null);

        Vec3 normal = vectorToWorld.apply(result.normal).normalize();
        if (result.temporalResponse < 1 && Math.abs(normal.y) < 0.6) {
            // The shared time of impact scales Y even when only X/Z hit a wall.
            // Recheck Y independently so floor and ceiling impacts still stop it.
            Vec3 vertical = VecHelper.rotate(new Vec3(0, relativeMotion.y, 0), -yaw, Direction.Axis.Y);
            vertical = body.getRotation().transform(vertical);
            OrientedBB separated = body.copy();
            separated.move(result.collisionResponse);
            CollisionResponse verticalHit = ContinuousOBBCollider.collideMany(shapes,
                    scratch, separated, vertical, 0, horizontalPass);
            result.temporalResponse = verticalHit.temporalResponse;
        }
        return result;
    }

    static boolean withinStepHeight(double rise, double maximum) {
        return Double.isFinite(rise) && rise >= -SKIN && rise <= maximum + 1.0E-3;
    }

    static boolean stepPathClear(AABB body, Vec3 motion,
            java.util.function.Predicate<AABB> collides) {
        return !collides.test(body.expandTowards(0, motion.y, 0))
                && !collides.test(body.move(0, motion.y, 0).expandTowards(motion.x, 0, motion.z));
    }

    private static AABB footProbe(AABB body) {
        return new AABB(body.minX + SKIN, body.minY - SUPPORT_DEPTH, body.minZ + SKIN,
                body.maxX - SKIN, body.minY + SKIN, body.maxZ - SKIN);
    }

    private static Vec3 inverseVector(Vec3 vector, Vec3 pivot, UnaryOperator<Vec3> transform) {
        Vec3 origin = transform.apply(pivot);
        return new Vec3(vector.dot(transform.apply(pivot.add(1, 0, 0)).subtract(origin)),
                vector.dot(transform.apply(pivot.add(0, 1, 0)).subtract(origin)),
                vector.dot(transform.apply(pivot.add(0, 0, 1)).subtract(origin)));
    }

    private static boolean hasSupport(Player player, AABB probe, UnaryOperator<Vec3> toWorld) {
        AABB broadPhase = CollisionGeometry.transformBounds(probe, toWorld);
        FilteredBlockCollisions<VoxelShape> blocks = new FilteredBlockCollisions<>(
                player.level(), player, broadPhase, false, null, (pos, shape) -> shape);
        while (blocks.hasNext()) {
            for (AABB block : blocks.next().toAabbs()) {
                if (CollisionGeometry.hasSupport(probe, toWorld, block, UnaryOperator.identity())) return true;
            }
        }
        return DynamicCollisions.hasTransformedSupport(player.level(), player, probe, toWorld);
    }

    private static boolean hasCollision(Player player, AABB query, UnaryOperator<Vec3> toWorld) {
        AABB broadPhase = CollisionGeometry.transformBounds(query, toWorld);
        FilteredBlockCollisions<VoxelShape> blocks = new FilteredBlockCollisions<>(
                player.level(), player, broadPhase, false, null, (pos, shape) -> shape);
        while (blocks.hasNext()) {
            for (AABB block : blocks.next().toAabbs()) {
                if (CollisionGeometry.intersects(query, toWorld, block, UnaryOperator.identity())) return true;
            }
        }
        return DynamicCollisions.hasTransformedBlockCollision(player.level(), player, query, toWorld, null);
    }
}
