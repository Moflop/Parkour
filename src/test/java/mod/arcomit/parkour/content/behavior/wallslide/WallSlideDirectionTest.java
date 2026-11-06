package mod.arcomit.parkour.content.behavior.wallslide;

import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WallSlideDirectionTest {
	@Test
	void missingAndVerticalWallPointsAreRejectedBeforeWorldAccess() {
		for (Direction direction : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertFalse(WallSlideCollision.hasValidWallPoint(null, direction));
		}
	}

	@Test
	void absentAndVerticalWallDirectionsDoNotApplySlidePhysics() {
		for (Direction direction : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			WallMovementData wall = new WallMovementData();
			wall.setSlide(direction);
			assertDoesNotThrow(() -> WallSlidePhysics.applySlowdownAndAdhesion(null, wall));
		}
	}

	@Test
	void clearingCachedSlideDoesNotApplyPhysics() {
		WallMovementData wall = new WallMovementData();
		wall.setSlide(Direction.EAST);
		wall.resetSlide();
		assertDoesNotThrow(() -> WallSlidePhysics.applySlowdownAndAdhesion(null, wall));
	}
}
