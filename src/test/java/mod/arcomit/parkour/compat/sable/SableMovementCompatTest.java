package mod.arcomit.parkour.compat.sable;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SableMovementCompatTest {

	@Test
	void movementRunsWithoutInheritedVelocityAndRestoresItAfterward() {
		Vector3d inherited = new Vector3d(0.25, -0.75, 1.5);
		AtomicBoolean movementExecuted = new AtomicBoolean();

		SableMovementCompat.withoutInheritedMotion(inherited, () -> {
			movementExecuted.set(true);
			assertVector(0, 0, 0, inherited);
		});

		assertTrue(movementExecuted.get());
		assertVector(0.25, -0.75, 1.5, inherited);
	}

	@Test
	void movementExceptionRestoresVelocityAndPropagatesTheOriginalException() {
		Vector3d inherited = new Vector3d(-2.5, 0.125, 4.75);
		RuntimeException original = new RuntimeException("Movement failed");

		RuntimeException thrown = assertThrows(RuntimeException.class,
				() -> SableMovementCompat.withoutInheritedMotion(inherited, () -> {
					assertVector(0, 0, 0, inherited);
					throw original;
				}));

		assertSame(original, thrown);
		assertVector(-2.5, 0.125, 4.75, inherited);
	}

	@Test
	void nestedMovementRestoresTheOuterZeroVelocityBeforeRestoringTheOriginal() {
		Vector3d inherited = new Vector3d(3.25, -6.5, 0.0625);
		AtomicInteger movementsExecuted = new AtomicInteger();

		SableMovementCompat.withoutInheritedMotion(inherited, () -> {
			movementsExecuted.incrementAndGet();
			assertVector(0, 0, 0, inherited);
			SableMovementCompat.withoutInheritedMotion(inherited, () -> {
				movementsExecuted.incrementAndGet();
				assertVector(0, 0, 0, inherited);
			});
			assertVector(0, 0, 0, inherited);
		});

		assertEquals(2, movementsExecuted.get());
		assertVector(3.25, -6.5, 0.0625, inherited);
	}

	@Test
	void velocityChangesDuringMovementDoNotReplaceTheSavedVelocity() {
		Vector3d inherited = new Vector3d(-0.5, 2.25, -3.125);
		AtomicBoolean movementExecuted = new AtomicBoolean();

		SableMovementCompat.withoutInheritedMotion(inherited, () -> {
			movementExecuted.set(true);
			inherited.set(12, -8, 0.5);
		});

		assertTrue(movementExecuted.get());
		assertVector(-0.5, 2.25, -3.125, inherited);
	}

	private static void assertVector(double x, double y, double z, Vector3d actual) {
		assertEquals(x, actual.x, "x");
		assertEquals(y, actual.y, "y");
		assertEquals(z, actual.z, "z");
	}
}
