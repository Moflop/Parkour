package mod.arcomit.parkour.content.behavior.wallclimb;

import mod.arcomit.parkour.content.behavior.wallclimb.server.ServerWallClimbSound;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WallClimbDirectionTest {
	@Test
	void absentAndVerticalClimbDirectionsSkipSoundBeforeWorldAccess() {
		for (Direction direction : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			WallMovementData wall = new WallMovementData();
			wall.setClimb(direction);
			assertDoesNotThrow(() -> ServerWallClimbSound.playFootstepSound(null, wall));
		}
	}

	@Test
	void clearingCachedClimbSkipsSoundBeforeWorldAccess() {
		WallMovementData wall = new WallMovementData();
		wall.setClimb(Direction.EAST);
		wall.resetClimb();
		assertDoesNotThrow(() -> ServerWallClimbSound.playFootstepSound(null, wall));
	}
}
