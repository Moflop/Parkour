package mod.arcomit.parkour.content.context;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WallMovementDataTest {

	@Test
	void freshDataHasNoWallRunDirections() {
		WallMovementData data = new WallMovementData();
		assertEquals(-1, data.getRunCollisionRaw());
		assertEquals(-1, data.getRunMoveRaw());
		assertNull(data.getRunCollision());
		assertNull(data.getRunMove());
	}

	@Test
	void rawNoDirectionSentinelDoesNotBecomeUp() {
		WallMovementData data = new WallMovementData();
		data.setRunCollision(Direction.EAST);
		data.setRunMove(Direction.NORTH);
		data.setRunCollisionRaw(-1);
		data.setRunMoveRaw(-1);
		assertNull(data.getRunCollision());
		assertNull(data.getRunMove());
	}

	@Test
	void typedHorizontalDirectionsKeepTheirSerializedValues() {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			WallMovementData data = new WallMovementData();
			data.setRunCollision(direction);
			data.setRunMove(direction);
			assertEquals(direction.get3DDataValue(), data.getRunCollisionRaw());
			assertEquals(direction.get3DDataValue(), data.getRunMoveRaw());
			assertEquals(direction, data.getRunCollision());
			assertEquals(direction, data.getRunMove());
		}
	}

	@Test
	void rawHorizontalDirectionsInvalidateThePreviousCachedDirection() {
		WallMovementData data = new WallMovementData();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			data.setRunCollision(direction.getOpposite());
			data.setRunMove(direction.getOpposite());
			data.setRunCollisionRaw(direction.get3DDataValue());
			data.setRunMoveRaw(direction.get3DDataValue());
			assertEquals(direction, data.getRunCollision());
			assertEquals(direction, data.getRunMove());
		}
	}

	@Test
	void resetRemovesPreviouslyCachedWallRunDirections() {
		WallMovementData data = new WallMovementData();
		data.setRunCollision(Direction.WEST);
		data.setRunMove(Direction.SOUTH);
		data.resetRunCollision();
		assertEquals(-1, data.getRunCollisionRaw());
		assertNull(data.getRunCollision());
		assertEquals(Direction.SOUTH, data.getRunMove());
		data.resetRunMove();
		assertEquals(-1, data.getRunMoveRaw());
		assertNull(data.getRunMove());
	}

	@Test
	void typedClimbAndSlideChangesClearTheWallRunCollision() {
		WallMovementData data = new WallMovementData();
		data.setRunCollision(Direction.EAST);
		data.setClimb(Direction.NORTH);
		assertEquals(-1, data.getRunCollisionRaw());
		assertNull(data.getRunCollision());
		assertEquals(Direction.NORTH, data.getClimb());
		data.setRunCollision(Direction.WEST);
		data.setSlide(Direction.SOUTH);
		assertEquals(-1, data.getRunCollisionRaw());
		assertNull(data.getRunCollision());
		assertNull(data.getClimb());
		assertEquals(Direction.SOUTH, data.getSlide());
	}

	@Test
	void rawClimbAndSlideChangesClearTheWallRunCollision() {
		WallMovementData data = new WallMovementData();
		data.setRunCollision(Direction.EAST);
		data.setClimbRaw(Direction.NORTH.get3DDataValue());
		assertEquals(-1, data.getRunCollisionRaw());
		assertNull(data.getRunCollision());
		assertEquals(Direction.NORTH, data.getClimb());
		data.setRunCollision(Direction.WEST);
		data.setSlideRaw(Direction.SOUTH.get3DDataValue());
		assertEquals(-1, data.getRunCollisionRaw());
		assertNull(data.getRunCollision());
		assertNull(data.getClimb());
		assertEquals(Direction.SOUTH, data.getSlide());
	}

	@Test
	void streamCodecPreservesAbsentAndHorizontalDirections() {
		assertSameData(new WallMovementData(), streamRoundTrip(new WallMovementData()));
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			WallMovementData original = wallRunData(direction);
			assertSameData(original, streamRoundTrip(original));
		}
	}

	@Test
	void persistentCodecPreservesAbsentAndHorizontalDirections() {
		assertSameData(new WallMovementData(), persistentRoundTrip(new WallMovementData()));
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			WallMovementData original = wallRunData(direction);
			assertSameData(original, persistentRoundTrip(original));
		}
	}

	private static WallMovementData wallRunData(Direction direction) {
		WallMovementData data = new WallMovementData();
		data.setRunCollision(direction);
		data.setRunMove(direction.getClockWise());
		data.setArmhang(direction.getOpposite());
		data.setArmhangCooldown(7);
		data.setObstaclesHeight(0.75);
		return data;
	}

	private static WallMovementData streamRoundTrip(WallMovementData original) {
		ByteBuf buffer = Unpooled.buffer();
		try {
			WallMovementData.STREAM_CODEC.encode(buffer, original);
			WallMovementData decoded = WallMovementData.STREAM_CODEC.decode(buffer);
			assertEquals(0, buffer.readableBytes());
			return decoded;
		} finally {
			buffer.release();
		}
	}

	private static WallMovementData persistentRoundTrip(WallMovementData original) {
		JsonElement encoded = WallMovementData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		return WallMovementData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
	}

	private static void assertSameData(WallMovementData expected, WallMovementData actual) {
		assertEquals(expected.getRunCollisionRaw(), actual.getRunCollisionRaw());
		assertEquals(expected.getRunMoveRaw(), actual.getRunMoveRaw());
		assertEquals(expected.getRunCollision(), actual.getRunCollision());
		assertEquals(expected.getRunMove(), actual.getRunMove());
		assertEquals(expected.getClimb(), actual.getClimb());
		assertEquals(expected.getSlide(), actual.getSlide());
		assertEquals(expected.getLastCollisionRaw(), actual.getLastCollisionRaw());
		assertEquals(expected.getArmhang(), actual.getArmhang());
		assertEquals(expected.getArmhangCooldown(), actual.getArmhangCooldown());
		assertEquals(expected.getObstaclesHeight(), actual.getObstaclesHeight());
	}
}
