package mod.arcomit.parkour.compat.create;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.function.UnaryOperator;

/** Geometry shared with Create's upright player OBB and pitched wall probes. */
final class CreateSensorGeometry {
	private CreateSensorGeometry() {}

	static UnaryOperator<Vec3> sensorTransform(Vec3 pivot, float yawOffset) {
		if (yawOffset == 0) return UnaryOperator.identity();
		return point -> VecHelper.rotate(point.subtract(pivot), yawOffset, Direction.Axis.Y).add(pivot);
	}

	static double wallSensorTolerance(Vec3 normal, double height, double maximum) {
		double horizontal = normal.horizontalDistance();
		if (horizontal < 1.0E-8 || Math.abs(normal.y) < 1.0E-8 || height <= 0 || maximum <= 0) return 0;
		double reach = height * Math.abs(normal.y) / horizontal;
		return Double.isFinite(reach) ? Math.min(maximum, reach) : 0;
	}
}
