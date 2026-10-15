package mod.arcomit.parkour.compat.create;

import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateCorrectionCollisionTest {
    private static final double TOLERANCE = 5.0E-4;

    @Test
    void clearPathPreservesTheExactRequestedMovement() {
        Vec3 requested = new Vec3(-0.15, 0.12, 0.3);
        assertEquals(requested, CreateCorrectionCollision.clipAgainstShapes(body(-0.35), wall(), requested));
    }

    @Test
    void approachingWallStopsAtTheFaceAndKeepsTangentMotion() {
        Vec3 result = CreateCorrectionCollision.clipAgainstShapes(body(-0.35), wall(), new Vec3(0.2, 0.12, 0.4));
        assertEquals(0.05, result.x, TOLERANCE);
        assertEquals(0.12, result.y, TOLERANCE);
        assertEquals(0.4, result.z, TOLERANCE);
    }

    @Test
    void existingOverlapAllowsOutwardAndTangentMovementWithoutAddingRecovery() {
        Vec3 requested = new Vec3(-0.1, 0, 0.25);
        assertEquals(requested, CreateCorrectionCollision.clipAgainstShapes(body(-0.25), wall(), requested));
        Vec3 result = CreateCorrectionCollision.clipAgainstShapes(body(-0.25), wall(), new Vec3(0.1, 0, 0.25));
        assertEquals(0, result.x, TOLERANCE);
        assertEquals(0.25, result.z, TOLERANCE);
    }

    @Test
    void overlappingFloorAndWallCannotTurnHorizontalAdhesionIntoAStep() {
        OrientedBB body = new OrientedBB(new AABB(-0.55, -0.05, -0.3, 0.05, 1.75, 0.3));
        CollisionList corner = shapes(new AABB(-10, -1, -10, 10, 0, 10),
                new AABB(0, -10, -10, 1, 10, 10));
        for (double inward : new double[]{0.4, 1.0}) {
            Vec3 result = CreateCorrectionCollision.clipAgainstShapes(body, corner, new Vec3(inward, 0, 0.25));
            assertEquals(0, result.x, TOLERANCE);
            assertEquals(0, result.y, 1.0E-9);
            assertEquals(0.25, result.z, TOLERANCE);
        }
        Vec3 outward = new Vec3(-0.1, 0, 0.25);
        assertEquals(outward, CreateCorrectionCollision.clipAgainstShapes(body, corner, outward));
    }

    @Test
    void longCorrectionCannotTunnelThroughAThinPlate() {
        CollisionList plate = shapes(new AABB(0.9, -10, -10, 0.91, 10, 10));
        Vec3 result = CreateCorrectionCollision.clipAgainstShapes(body(0), plate, new Vec3(3, 0, 0));
        assertEquals(0.6, result.x, TOLERANCE);
        assertEquals(0, result.y, TOLERANCE);
    }

    @Test
    void aGapBetweenSeparateShapesRemainsPassable() {
        CollisionList doorway = shapes(new AABB(0, -10, -3, 1, 10, -0.4),
                new AABB(0, -10, 0.4, 1, 10, 3));
        Vec3 requested = new Vec3(2, 0, 0);
        assertEquals(requested, CreateCorrectionCollision.clipAgainstShapes(body(-0.35), doorway, requested));
    }

    @Test
    void yawedPlayerStopsAtActualObbSurface() {
        double halfWidth = Math.sqrt(0.18);
        OrientedBB body = new OrientedBB(new Vec3(-halfWidth - 0.05, 1, 0),
                new Vec3(0.3, 0.9, 0.3), new Matrix3d().asYRotation((float) (Math.PI / 4)));
        Vec3 result = CreateCorrectionCollision.clipAgainstShapes(body, wall(), new Vec3(0.2, 0, 0.2));
        assertTrue(result.x >= 0);
        assertEquals(0.05, result.x, TOLERANCE);
        assertEquals(0.2, result.z, TOLERANCE);
    }

    @Test
    void staticBarrierCannotShortenASlideBackIntoAFiniteDynamicWall() {
        AABB bounds = new AABB(-0.65, 0.1, -0.5, -0.05, 1.9, 0.1);
        AABB wall = new AABB(0, -10, 0, 0.1, 10, 1);
        CollisionList dynamic = shapes(wall);
        var statics = List.of(Shapes.create(new AABB(-10, -10, 1.4, 10, 10, 2.4)));
        UnaryOperator<Vec3> staticClip = motion -> new Vec3(motion.x, motion.y,
                Shapes.collide(Direction.Axis.Z, bounds, statics, motion.z));
        UnaryOperator<Vec3> dynamicClip = motion -> CreateCorrectionCollision.clipAgainstShapes(
                new OrientedBB(bounds), dynamic, motion);

        Vec3 result = CreateCorrectionCollision.clipCombined(new Vec3(1, 0, 2),
                staticClip, List.of(dynamicClip));

        assertEquals(0.05, result.x, TOLERANCE);
        assertEquals(0, result.y, 1.0E-9);
        assertEquals(1.3, result.z, TOLERANCE);
        assertFalse(bounds.move(result).intersects(wall));
        assertEquals(result, staticClip.apply(result));
        assertEquals(result, dynamicClip.apply(result));
    }

    @Test
    void mixedShortCorrectionPreservesVerticalAndUnblockedTangentMotion() {
        OrientedBB body = body(-0.35);
        AABB bounds = body.getAsAABB();
        var statics = List.of(Shapes.create(new AABB(-10, -10, 0.55, 10, 10, 1.55)));
        UnaryOperator<Vec3> staticClip = motion -> new Vec3(motion.x, motion.y,
                Shapes.collide(Direction.Axis.Z, bounds, statics, motion.z));
        Vec3 result = CreateCorrectionCollision.clipCombined(new Vec3(0.2, 0.12, 0.4),
                staticClip, List.of(motion -> CreateCorrectionCollision.clipAgainstShapes(body, wall(), motion)));

        assertEquals(0.05, result.x, TOLERANCE);
        assertEquals(0.12, result.y, 1.0E-9);
        assertEquals(0.25, result.z, TOLERANCE);
    }

    @Test
    void combinedClippingPreservesOutwardAndVerticalMovementFromAnExistingOverlap() {
        Vec3 requested = new Vec3(-0.1, 0.12, 0.25);
        Vec3 result = CreateCorrectionCollision.clipCombined(requested, UnaryOperator.identity(),
                List.of(motion -> CreateCorrectionCollision.clipAgainstShapes(body(-0.25), wall(), motion)));

        assertEquals(requested, result);
    }

    @Test
    void absentDynamicTargetsDoNotChangeOrdinaryStaticMovement() {
        Vec3 requested = new Vec3(0.2, 0.12, 0.4);
        assertEquals(requested, CreateCorrectionCollision.clipCombined(requested,
                motion -> { throw new AssertionError("Static movement must remain with vanilla"); }, List.of()));
    }

    @Test
    void aResultThatHasNotConvergedWithinThePassLimitIsBlocked() {
        Vec3 result = CreateCorrectionCollision.clipCombined(new Vec3(1, 0, 0),
                UnaryOperator.identity(), List.of(motion -> motion.scale(0.5)));
        assertEquals(Vec3.ZERO, result);
    }

    @Test
    void theLastPassCanReachAnAcceptedFixedPoint() {
        Vec3 result = CreateCorrectionCollision.clipCombined(new Vec3(1, 0.12, 0.4),
                UnaryOperator.identity(), List.of(motion -> new Vec3(Math.max(0, motion.x - 0.25),
                        motion.y, motion.z)));
        assertEquals(new Vec3(0, 0.12, 0.4), result);
    }

    @Test
    void serverSeparationLeavesAClearBodyUnchanged() {
        assertEquals(Vec3.ZERO, CreateCorrectionCollision.separationAgainstShapes(body(-0.35), wall(), 1));
    }

    @Test
    void serverSeparationRemovesCurrentWallOverlapWithoutCarrierMotion() {
        Vec3 separated = CreateCorrectionCollision.separationAgainstShapes(body(-0.25), wall(), 1);
        assertEquals(-0.05, separated.x, TOLERANCE);
        assertEquals(0, separated.y, 1.0E-9);
        assertEquals(0, separated.z, 1.0E-9);
        assertTrue(body(-0.25).getAsAABB().move(separated).maxX < 0);
    }

    @Test
    void serverSeparationResolvesBothFacesOfAnExistingCorner() {
        AABB bounds = new AABB(-0.55, -0.05, -0.3, 0.05, 1.75, 0.3);
        CollisionList corner = shapes(new AABB(-10, -1, -10, 10, 0, 10),
                new AABB(0, -10, -10, 1, 10, 10));
        Vec3 separated = CreateCorrectionCollision.separationAgainstShapes(new OrientedBB(bounds), corner, 1);
        assertEquals(-0.05, separated.x, TOLERANCE);
        assertEquals(0.05, separated.y, TOLERANCE);
        assertEquals(0, separated.z, 1.0E-9);
        assertTrue(bounds.move(separated).minY > 0);
        assertTrue(bounds.move(separated).maxX < 0);
    }

    @Test
    void serverSeparationRejectsLargeDisplacements() {
        CollisionList embedded = shapes(new AABB(-10, -10, -10, 10, 10, 10));
        assertEquals(Vec3.ZERO, CreateCorrectionCollision.separationAgainstShapes(body(0), embedded, 1));
    }

    private static OrientedBB body(double x) {
        return new OrientedBB(new AABB(x - 0.3, 0.1, -0.3, x + 0.3, 1.9, 0.3));
    }

    private static CollisionList wall() {
        return shapes(new AABB(0, -10, -10, 1, 10, 10));
    }

    private static CollisionList shapes(AABB... boxes) {
        CollisionList shapes = new CollisionList();
        CollisionList.Populate populate = new CollisionList.Populate(shapes);
        for (AABB box : boxes) populate.consume(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        return shapes;
    }
}
