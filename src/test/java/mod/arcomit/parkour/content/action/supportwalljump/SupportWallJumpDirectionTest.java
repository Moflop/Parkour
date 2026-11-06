package mod.arcomit.parkour.content.action.supportwalljump;

import mod.arcomit.parkour.content.action.walljump.WallJumpType;
import mod.arcomit.parkour.content.action.walljump.WallJumpTypeResolver;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class SupportWallJumpDirectionTest {
	@Test
	void absentAndVerticalWallsCannotSelectJumpTypes() {
		for (Direction invalid : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertSame(SupportWallJumpType.NONE, SupportWallJumpTypeResolver.resolve(null, invalid));
			assertSame(WallJumpType.NONE, WallJumpTypeResolver.resolve(null, invalid));
		}
	}
}
