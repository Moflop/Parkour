package mod.arcomit.parkour.content.behavior.wallrun;

import mod.arcomit.parkour.content.behavior.wallrun.server.ServerWallRunSound;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class WallRunDirectionTest {
	@Test
	void missingWallIsRejectedBeforeAccessingPlayerOrSensors() {
		// Vanilla wraps -1 to UP; it does not decode the context's missing-direction sentinel.
		assertSame(Direction.UP, Direction.from3DDataValue(-1));
		assertInvalidWall(new WallMovementData());
	}

	@Test
	void resetWallIsRejectedBeforeAccessingPlayerOrSensors() {
		WallMovementData wall = new WallMovementData();
		wall.setRunCollision(Direction.EAST);
		wall.resetRunCollision();
		assertInvalidWall(wall);
	}

	@Test
	void verticalWallsAreRejectedBeforeAccessingPlayerOrSensors() {
		for (Direction direction : new Direction[]{Direction.UP, Direction.DOWN}) {
			WallMovementData wall = new WallMovementData();
			wall.setRunCollision(direction);
			assertInvalidWall(wall);
		}
	}

	@Test
	void missingOrVerticalMovementDoesNotApplyPhysics() {
		for (Direction movement : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			WallMovementData wall = new WallMovementData();
			wall.setRunCollision(Direction.EAST);
			wall.setRunMove(movement);
			assertDoesNotThrow(() -> WallRunPhysics.applyWallRunMovement(null, wall));
		}
	}

	private static void assertInvalidWall(WallMovementData wall) {
		wall.setRunMove(Direction.NORTH);
		// No player is needed: invalid direction data must fail before any world access.
		ParkourContext context = new ParkourContext(null, wall, null, null, null, null);
		assertFalse(WallRunCollision.wallCollisionIsValid(null, context));
		assertDoesNotThrow(() -> WallRunPhysics.applyWallRunMovement(null, wall));
		assertDoesNotThrow(() -> ServerWallRunSound.playFootstepSound(null, wall));
	}
}
