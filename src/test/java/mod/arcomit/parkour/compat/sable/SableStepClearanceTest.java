package mod.arcomit.parkour.compat.sable;

import mod.arcomit.parkour.compat.CollisionGeometry;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SableStepClearanceTest {
	private static final AABB STEP = new AABB(0, 0, 0, 1, 1, 1);
	private static final AABB FLOOR = new AABB(-4, -1, -4, 4, 0, 4);

	@Test
	void slightlyLeaningStepCanBeEnteredFromEitherSide() {
		for (int side : new int[]{-1, 1}) {
			AABB body = touchingBody(side, .001);
			Vector3d motion = new Vector3d(0, 1.0625, -side * .0625);
			Predicate<AABB> collides = obstacles(side * .001, null);
			assertFalse(collides.test(body), "The actor starts outside the tilted step: side=" + side + " body=" + body);
			assertTrue(collides.test(body.expandTowards(0, motion.y, 0)),
					"A direct rise touches the tiny overhang and reproduces the one-sided refusal");
			assertTrue(SableStepClearance.isStepPathClear(body, motion, collides),
					"A clear retreat, rise and advance must preserve the same destination");
		}
	}

	@Test
	void lowCeilingStillRejectsTheStep() {
		Predicate<AABB> collides = obstacles(.001, new AABB(-1, 1.5, -1, 2, 2.5, 3));
		assertFalse(SableStepClearance.isStepPathClear(touchingBody(1, .001),
				new Vector3d(0, 1.0625, -.0625), collides));
	}

	@Test
	void blockedRetreatAndRealOverhangRemainSolid() {
		AABB body = touchingBody(1, .001);
		AABB rearWall = new AABB(-1, -1, body.maxZ + .0005, 2, 3, body.maxZ + 1);
		assertFalse(SableStepClearance.isStepPathClear(body, new Vector3d(0, 1.0625, -.0625),
				obstacles(.001, rearWall)), "A retreat cannot move through a block behind the player");
		assertFalse(SableStepClearance.isStepPathClear(touchingBody(1, .05),
				new Vector3d(0, 1.0625, -.0625), obstacles(.05, null)),
				"A visibly overhanging step cannot receive more than the tiny seam retreat");
	}

	private static AABB touchingBody(int side, double angle) {
		// The tilted face's Z at the actor's top (Y=.6), plus a small contact gap.
		double face = .5 + side * (.5 / Math.cos(angle) + .1 * Math.tan(angle));
		double centerZ = face + side * (.3 + 1e-7);
		return new AABB(.2, 0, centerZ - .3, .8, .6, centerZ + .3).deflate(1e-6);
	}

	private static Predicate<AABB> obstacles(double angle, AABB extra) {
		double cosine = Math.cos(angle), sine = Math.sin(angle);
		UnaryOperator<Vec3> transform = point -> {
			double y = point.y - .5, z = point.z - .5;
			return new Vec3(point.x, .5 + y * cosine - z * sine, .5 + y * sine + z * cosine);
		};
		return box -> CollisionGeometry.intersects(box, STEP, transform)
				|| box.intersects(FLOOR) || extra != null && box.intersects(extra);
	}
}
