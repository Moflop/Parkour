package mod.arcomit.parkour.content.context;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HorizontalDirectionDataTest {
	private static final int[] INVALID_IDS = {-1, -2, -4, -6, Integer.MIN_VALUE, 0, 1, 6, 8, 10, Integer.MAX_VALUE};

	@Test
	void decoderRejectsSentinelsVerticalAndWrappedIds() {
		for (int raw : INVALID_IDS) assertNull(Directions.horizontalFrom3DDataValue(raw), "raw=" + raw);
		assertFalse(Directions.isHorizontal(null));
		for (Direction direction : Direction.values()) {
			assertEquals(direction.getAxis().isHorizontal(), Directions.isHorizontal(direction));
		}
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			assertSame(direction, Directions.horizontalFrom3DDataValue(direction.get3DDataValue()));
		}
	}

	@Test
	void everyWallGetterRejectsInvalidIdsFromConstructorsAndRawSetters() {
		for (int raw : INVALID_IDS) {
			assertNoWallDirections(new WallMovementData(raw, raw, raw, raw, raw, raw, 7, 0.75));
			WallMovementData data = new WallMovementData();
			data.setRunCollisionRaw(raw); assertNull(data.getRunCollision());
			data.setRunMoveRaw(raw); assertNull(data.getRunMove());
			data.setClimbRaw(raw); assertNull(data.getClimb());
			data.setSlideRaw(raw); assertNull(data.getSlide());
			data.setArmhangRaw(raw); assertNull(data.getArmhang());
		}
	}

	@Test
	void typedVerticalCachesCannotBypassWallValidation() {
		for (Direction invalid : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			WallMovementData data = new WallMovementData();
			data.setRunCollision(invalid); assertNull(data.getRunCollision());
			data.setRunMove(invalid); assertNull(data.getRunMove());
			data.setClimb(invalid); assertNull(data.getClimb());
			data.setSlide(invalid); assertNull(data.getSlide());
			data.setArmhang(invalid); assertNull(data.getArmhang());
			WallMovementData copy = new WallMovementData();
			copy.copyFrom(data); assertNoWallDirections(copy);
		}
	}

	@Test
	void allJumpHistoryGettersRejectInvalidRawAndTypedDirections() {
		for (int raw : INVALID_IDS) {
			assertNoJumpDirections(new JumpData(true, raw, raw, raw, raw, 9, 3));
			JumpData data = new JumpData();
			data.setLastViewJump(Direction.EAST); data.setLastViewJumpRaw(raw); assertNull(data.getLastViewJump());
			data.setLastUpJump(Direction.SOUTH); data.setLastUpJumpRaw(raw); assertNull(data.getLastUpJump());
			data.setLastParallelJump(Direction.WEST); data.setLastParallelJumpRaw(raw); assertNull(data.getLastParallelJump());
		}
		for (Direction invalid : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			JumpData data = new JumpData();
			data.setLastViewJump(invalid); assertNull(data.getLastViewJump());
			data.setLastUpJump(invalid); assertNull(data.getLastUpJump());
			data.setLastParallelJump(invalid); assertNull(data.getLastParallelJump());
			JumpData copy = new JumpData(); copy.copyFrom(data); assertNoJumpDirections(copy);
		}
	}

	@Test
	void validJumpHistoryKeepsAllFourHorizontalWalls() {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			JumpData data = new JumpData();
			data.setLastViewJump(direction); assertSame(direction, data.getLastViewJump());
			data.setLastUpJumpRaw(direction.get3DDataValue()); assertSame(direction, data.getLastUpJump());
			data.setLastParallelJump(direction); assertSame(direction, data.getLastParallelJump());
		}
	}

	@Test
	void codecsDoNotTurnInvalidStoredIdsIntoWalls() {
		for (int raw : INVALID_IDS) {
			WallMovementData wall = new WallMovementData(raw, raw, raw, raw, raw, raw, 7, 0.75);
			var wallJson = WallMovementData.CODEC.encodeStart(JsonOps.INSTANCE, wall).getOrThrow();
			assertNoWallDirections(WallMovementData.CODEC.parse(JsonOps.INSTANCE, wallJson).getOrThrow());
			JumpData jump = new JumpData(true, raw, raw, raw, raw, 9, 3);
			var jumpJson = JumpData.CODEC.encodeStart(JsonOps.INSTANCE, jump).getOrThrow();
			assertNoJumpDirections(JumpData.CODEC.parse(JsonOps.INSTANCE, jumpJson).getOrThrow());
			ByteBuf buffer = Unpooled.buffer();
			try {
				WallMovementData.STREAM_CODEC.encode(buffer, wall);
				assertNoWallDirections(WallMovementData.STREAM_CODEC.decode(buffer));
				JumpData.STREAM_CODEC.encode(buffer, jump);
				assertNoJumpDirections(JumpData.STREAM_CODEC.decode(buffer));
				assertEquals(0, buffer.readableBytes());
			} finally { buffer.release(); }
		}
	}

	private static void assertNoWallDirections(WallMovementData data) {
		assertNull(data.getRunCollision()); assertNull(data.getRunMove());
		assertNull(data.getClimb()); assertNull(data.getSlide()); assertNull(data.getArmhang());
	}

	private static void assertNoJumpDirections(JumpData data) {
		assertNull(data.getLastViewJump()); assertNull(data.getLastUpJump()); assertNull(data.getLastParallelJump());
	}
}
