package mod.arcomit.parkour.content.action.walljump;

import mod.arcomit.parkour.content.context.JumpData;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WallJumpDirectionTest {
	@Test
	void absentAndInvalidHistoryCannotBlockARealHorizontalWall() {
		for (WallJumpType type : new WallJumpType[]{WallJumpType.UP, WallJumpType.PARALLEL, WallJumpType.VIEW}) {
			JumpData data = new JumpData();
			for (int raw : new int[]{-1, 0, 1, -2, 6, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
				data.setLastUpJumpRaw(raw);
				data.setLastParallelJumpRaw(raw);
				data.setLastViewJumpRaw(raw);
				for (Direction wall : Direction.Plane.HORIZONTAL) {
					assertFalse(WallJumpSameWallGuard.isSameWallAsLastJump(data, type, wall),
							"Invalid history must not wrap into a real wall: " + raw + "/" + type + "/" + wall);
				}
			}
		}
	}

	@Test
	void realHorizontalWallHistoryStillPreventsRepeatedJumps() {
		for (WallJumpType type : new WallJumpType[]{WallJumpType.UP, WallJumpType.PARALLEL, WallJumpType.VIEW}) {
			for (Direction wall : Direction.Plane.HORIZONTAL) {
				JumpData data = new JumpData();
				WallJumpSameWallGuard.recordLastJumpWall(data, type, wall);
				assertTrue(WallJumpSameWallGuard.isSameWallAsLastJump(data, type, wall));
				assertFalse(WallJumpSameWallGuard.isSameWallAsLastJump(data, type, wall.getOpposite()));
			}
		}
	}
}
