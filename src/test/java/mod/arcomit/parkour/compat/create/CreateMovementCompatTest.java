package mod.arcomit.parkour.compat.create;

import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider.CollisionResponse;
import com.simibubi.create.foundation.collision.OrientedBB;
import mod.arcomit.parkour.compat.CollisionGeometry;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateMovementCompatTest {
    private static final double SKIN = 1.0E-5;
    private static final AABB STEP = new AABB(0, 0, -.5, 1, 1, .5);
    private static final AABB ENTERING_STEP = new AABB(-.55, 0, -.3, .05, 1.8, .3);
    private static final Vec3 WALKING = new Vec3(.2, -.08, 0);
    private static final AABB AIRBORNE = new AABB(-.9, 1, -.3, -.3, 2.8, .3);
    private static final AABB TALL_WALL = new AABB(0, -10, -1, 1, 10, 1);

    @Test
    void rejectsTheRealCreateSolverAccumulatingSeveralLegalStepsIntoOneClimb() {
        CollisionList wall = shapes(STEP, STEP.move(0, 1, 0), STEP.move(0, 2, 0), STEP.move(0, 3, 0));
        CollisionResponse flat = collide(wall, ENTERING_STEP, WALKING, 0);
        CollisionResponse stepped = collide(wall, ENTERING_STEP, WALKING, 1);
        double rise = stepped.collisionResponse.y - flat.collisionResponse.y;

        assertEquals(3.0001, rise, 1.0E-8,
                "Create independently spends the same step allowance on every stacked collider");
        assertFalse(CreateMovementCompat.withinStepHeight(rise, 1),
                "The total proposal must be rejected even though each native step was legal");
        assertTrue(flat.collisionResponse.x < 0);
        assertEquals(0, flat.collisionResponse.y, 1.0E-8,
                "Rejecting the proposal retains a horizontal wall response without lifting the player");
    }

    @Test
    void acceptsAFullBlockStepFromTheSeparatedReferenceBody() {
        CollisionList step = shapes(STEP);
        CollisionResponse flat = collide(step, ENTERING_STEP, WALKING, 0);
        CollisionResponse stepped = collide(step, ENTERING_STEP, WALKING, 1);
        Vec3 proposal = stepped.collisionResponse.subtract(flat.collisionResponse);
        AABB separated = ENTERING_STEP.move(flat.collisionResponse).deflate(SKIN);

        assertTrue(CreateMovementCompat.withinStepHeight(proposal.y, 1),
                "Create's signed separation epsilon must not reject the configured endpoint");
        assertTrue(CreateMovementCompat.stepPathClear(separated, proposal, STEP::intersects));
        assertFalse(CreateMovementCompat.stepPathClear(ENTERING_STEP.deflate(SKIN),
                proposal, STEP::intersects),
                "The post-movement body already intersects the step and is not a valid sweep origin");
    }

    @Test
    void anAirborneWallContactHasNoSupportEvenWhenItsNativeStepFitsTheBudget() {
        AABB airborne = ENTERING_STEP.move(0, .4, 0);
        CollisionList wall = shapes(STEP);
        CollisionResponse flat = collide(wall, airborne, WALKING, 0);
        CollisionResponse stepped = collide(wall, airborne, WALKING, 1);
        AABB separated = airborne.move(flat.collisionResponse);
        AABB feet = new AABB(separated.minX + SKIN, separated.minY - .03,
                separated.minZ + SKIN, separated.maxX - SKIN,
                separated.minY + SKIN, separated.maxZ - SKIN);

        assertTrue(stepped.collisionResponse.y > .5);
        assertTrue(CreateMovementCompat.withinStepHeight(
                stepped.collisionResponse.y - flat.collisionResponse.y, 1));
        assertFalse(CollisionGeometry.hasSupport(feet, UnaryOperator.identity(),
                STEP, UnaryOperator.identity()),
                "A legal-sized upward correction is not permission to step while airborne");
    }

    @Test
    void raisedRouteRejectsACeilingEvenWhenBothEndpointsAreClear() {
        AABB body = new AABB(-.3, 0, -.3, .3, 1.8, .3);
        Vec3 step = new Vec3(2, 1, 0);
        AABB ceiling = new AABB(-.2, 2, -.2, .2, 2.3, .2);

        assertFalse(ceiling.intersects(body));
        assertFalse(ceiling.intersects(body.move(step)));
        assertFalse(CreateMovementCompat.stepPathClear(body, step, ceiling::intersects),
                "Checking only the destination would pass through the low ceiling during the lift");
    }

    @Test
    void raisedRouteRejectsAnObstacleBetweenClearEndpoints() {
        AABB body = new AABB(-.3, 0, -.3, .3, 1.8, .3);
        Vec3 step = new Vec3(2, 1, 0);
        AABB obstacle = new AABB(.8, 1.2, -.2, 1.1, 2.5, .2);

        assertFalse(obstacle.intersects(body));
        assertFalse(obstacle.intersects(body.move(step)));
        assertFalse(CreateMovementCompat.stepPathClear(body, step, obstacle::intersects),
                "A thin obstacle in the raised horizontal route must not be skipped");
    }

    @Test
    void crossingTheStepItselfBelowTheRaisedBodyRemainsAllowed() {
        AABB body = new AABB(-.6, 0, -.3, -SKIN, 1.8, .3);
        Vec3 step = new Vec3(.5, 1 + SKIN, 0);

        assertTrue(CreateMovementCompat.stepPathClear(body, step, STEP::intersects));
        assertFalse(CreateMovementCompat.withinStepHeight(1.01, 1));
        assertFalse(CreateMovementCompat.withinStepHeight(Double.NaN, 1));
        assertFalse(CreateMovementCompat.withinStepHeight(Double.POSITIVE_INFINITY, 1));
    }

    @Test
    void aSideWallTimeOfImpactDoesNotConstrainIndependentJumpOrFallMotion() {
        CollisionList wall = shapes(TALL_WALL);
        for (double vertical : new double[]{-.3, .3}) {
            CollisionResponse combined = collide(wall, AIRBORNE, new Vec3(.6, vertical, 0), 0);
            CollisionResponse independent = collide(wall,
                    AIRBORNE.move(combined.collisionResponse), new Vec3(0, vertical, 0), 0);

            assertEquals(.5, combined.temporalResponse, 1.0E-8);
            assertEquals(0, combined.normal.y, 1.0E-8);
            assertTrue(combined.surfaceCollision,
                    "The native surface flag also incorrectly describes this unsupported side collision");
            assertEquals(1, independent.temporalResponse, 1.0E-8,
                    "The player's free vertical motion must not inherit the wall's horizontal impact time");
        }
    }

    @Test
    void independentVerticalRecheckPreservesALaterFloorImpact() {
        assertVerticalImpactIsRetained(-.3, new AABB(-5, 0, -5, 5, .8, 5));
    }

    @Test
    void independentVerticalRecheckPreservesALaterCeilingImpact() {
        assertVerticalImpactIsRetained(.3, new AABB(-5, 3, -5, 5, 4, 5));
    }

    private static void assertVerticalImpactIsRetained(double vertical, AABB obstacle) {
        CollisionList shapes = shapes(TALL_WALL, obstacle);
        CollisionResponse combined = collide(shapes, AIRBORNE, new Vec3(.6, vertical, 0), 0);
        CollisionResponse independent = collide(shapes,
                AIRBORNE.move(combined.collisionResponse), new Vec3(0, vertical, 0), 0);

        assertEquals(.5, combined.temporalResponse, 1.0E-8,
                "The wall is encountered before the horizontal floor or ceiling");
        assertEquals(2.0 / 3.0, independent.temporalResponse, 1.0E-8,
                "Removing the side-wall limit must still retain the later vertical impact");
    }

    private static CollisionResponse collide(CollisionList shapes, AABB body, Vec3 motion, float step) {
        return ContinuousOBBCollider.collideMany(shapes, new CollisionList(),
                new OrientedBB(body), motion, step, true);
    }

    private static CollisionList shapes(AABB... boxes) {
        CollisionList shapes = new CollisionList();
        CollisionList.Populate populate = new CollisionList.Populate(shapes);
        for (AABB box : boxes) {
            populate.consume(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }
        return shapes;
    }
}
