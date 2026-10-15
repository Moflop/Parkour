package mod.arcomit.parkour.compat.create;

import mod.arcomit.parkour.compat.CollisionGeometry;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateSensorGeometryTest {
	@Test
	void ordinaryBearingSensorsKeepWorldAxes() {
		Vec3 pivot = new Vec3(23.5, 80, -15.75);
		Vec3 point = pivot.add(0.3, 1.8, -0.45);
		assertEquals(point, CreateSensorGeometry.sensorTransform(pivot, 0).apply(point));
	}

	@Test
	void pitchedVehicleSensorsMatchOuterCreateYawWithoutInventingSideSupport() {
		UnaryOperator<Vec3> wallToWorld = CreateSensorGeometry.sensorTransform(Vec3.ZERO, 45);
		AABB wall = new AABB(0, -10, -10, 1, 2, 10);
		Vec3 feet = wallToWorld.apply(new Vec3(-0.301, 0, 0));
		AABB footProbe = new AABB(-0.3, -0.48, -0.3, 0.3, 0, 0.3).move(feet);
		UnaryOperator<Vec3> sensors = CreateSensorGeometry.sensorTransform(feet, 45);
		assertTrue(CollisionGeometry.intersects(footProbe, wall, wallToWorld));
		assertFalse(CollisionGeometry.intersects(footProbe, sensors, wall, wallToWorld));
		AABB sideProbe = new AABB(-0.3, 0.1, -0.3, 0.45, 1.8, 0.3).move(feet);
		assertTrue(CollisionGeometry.intersects(sideProbe, sensors, wall, wallToWorld));
		assertEquals(Direction.EAST, CollisionGeometry.sensorDirection(
				wallToWorld.apply(new Vec3(1, 0, 0)), feet, sensors));
	}

	@Test
	void yawPreservesHeightAndPrecisionAtRemotePlotCoordinates() {
		Vec3 pivot = new Vec3(10_000_000.5, 71, -10_000_000.5);
		Vec3 point = CreateSensorGeometry.sensorTransform(pivot, 90).apply(pivot.add(0.25, 1.75, 0));
		assertEquals(pivot.x, point.x, 1.0E-9);
		assertEquals(pivot.y + 1.75, point.y, 1.0E-9);
		assertEquals(pivot.z - 0.25, point.z, 1.0E-9);
	}

	@Test
	void wallReachFollowsTiltAndKeepsItsConfiguredLimit() {
		double radians = Math.toRadians(5);
		Vec3 normal = new Vec3(Math.cos(radians), Math.sin(radians), 0);
		assertEquals(1.8 * Math.tan(radians), CreateSensorGeometry.wallSensorTolerance(normal, 1.8, 0.3), 1.0E-9);
		assertEquals(0.1, CreateSensorGeometry.wallSensorTolerance(normal, 1.8, 0.1), 1.0E-9);
		assertEquals(0, CreateSensorGeometry.wallSensorTolerance(new Vec3(1, 0, 0), 1.8, 0.3));
		assertEquals(0, CreateSensorGeometry.wallSensorTolerance(new Vec3(0, 1, 0), 1.8, 0.3));
	}
}
