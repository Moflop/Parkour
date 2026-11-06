package mod.arcomit.parkour.core.sensor;

import mod.arcomit.parkour.core.sensor.impl.ArmhangEyeSensor;
import mod.arcomit.parkour.core.sensor.impl.ArmhangTopSensor;
import mod.arcomit.parkour.core.sensor.impl.HeadFeetSensor;
import mod.arcomit.parkour.core.sensor.impl.WallJumpSensor;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HorizontalSensorDirectionTest {
	@Test
	void invalidQueriesAreRejectedBeforeReadingPlayerOrCache() {
		for (Direction invalid : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertFalse(HeadFeetSensor.isValidCollision(null, invalid));
			assertFalse(WallJumpSensor.isValidCollision(null, invalid));
			assertFalse(ArmhangEyeSensor.isValidCollision(null, invalid));
			assertFalse(ArmhangTopSensor.isValidCollision(null, invalid));
		}
	}

	@Test
	void cachesRemainIndependentAndStrictlyHorizontal() {
		SensorData data = new SensorData();
		java.util.Set<CollisionCache> caches = new java.util.HashSet<>();
		for (Direction direction : Direction.Plane.HORIZONTAL) caches.add(data.getCollisionCache(direction));
		assertEquals(4, caches.size());
		for (Direction invalid : new Direction[]{null, Direction.UP, Direction.DOWN}) {
			assertThrows(IllegalArgumentException.class, () -> data.getCollisionCache(invalid));
		}
	}
}
