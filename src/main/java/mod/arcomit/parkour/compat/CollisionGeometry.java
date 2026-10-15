package mod.arcomit.parkour.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Broad-phase bounds and exact separating-axis tests for transformed voxel boxes. */
public final class CollisionGeometry {
	private static final double EPSILON = 1.0E-7;

	private CollisionGeometry() {}

	/** Signed distance to the face approached along the given local direction. */
	public static double faceDistance(Vec3 point, AABB box, Direction toward) {
		return switch (toward) {
			case EAST -> box.minX - point.x;
			case WEST -> point.x - box.maxX;
			case SOUTH -> box.minZ - point.z;
			case NORTH -> point.z - box.maxZ;
			case UP -> box.minY - point.y;
			case DOWN -> point.y - box.maxY;
		};
	}

	/** Keeps only the sensor-covered parts of a face that are not internal to adjacent shapes. */
	public static boolean hasExposedFace(AABB query, UnaryOperator<Vec3> queryToLocal,
			AABB localBox, Direction toward, List<AABB> occluders) {
		Direction.Axis axis = toward.getAxis();
		Direction.Axis u = axis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
		Direction.Axis v = axis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
		boolean positive = toward.getAxisDirection() == Direction.AxisDirection.POSITIVE;
		double plane = positive ? localBox.min(axis) : localBox.max(axis);
		List<FacePatch> remaining = List.of(new FacePatch(localBox.min(u), localBox.min(v),
				localBox.max(u), localBox.max(v)));
		for (AABB occluder : occluders) {
			// A nearby box is not an occluder unless it reaches the face from outside.
			boolean coversPlane = positive
					? occluder.min(axis) < plane - EPSILON && occluder.max(axis) >= plane - EPSILON
					: occluder.max(axis) > plane + EPSILON && occluder.min(axis) <= plane + EPSILON;
			if (!coversPlane) continue;
			List<FacePatch> exposed = new ArrayList<>();
			for (FacePatch patch : remaining) {
				patch.subtract(occluder.min(u), occluder.min(v), occluder.max(u), occluder.max(v), exposed);
			}
			if (exposed.isEmpty()) return false;
			remaining = exposed;
		}
		for (FacePatch patch : remaining) {
			if (intersects(query, queryToLocal, patch.box(axis, plane), UnaryOperator.identity())) return true;
		}
		return false;
	}

	private record FacePatch(double minU, double minV, double maxU, double maxV) {
		private void subtract(double otherMinU, double otherMinV, double otherMaxU, double otherMaxV,
				List<FacePatch> result) {
			double lowU = Math.max(minU, otherMinU), highU = Math.min(maxU, otherMaxU);
			double lowV = Math.max(minV, otherMinV), highV = Math.min(maxV, otherMaxV);
			if (highU - lowU <= EPSILON || highV - lowV <= EPSILON) {
				result.add(this);
				return;
			}
			if (lowU - minU > EPSILON) result.add(new FacePatch(minU, minV, lowU, maxV));
			if (maxU - highU > EPSILON) result.add(new FacePatch(highU, minV, maxU, maxV));
			if (lowV - minV > EPSILON) result.add(new FacePatch(lowU, minV, highU, lowV));
			if (maxV - highV > EPSILON) result.add(new FacePatch(lowU, highV, highU, maxV));
		}

		private AABB box(Direction.Axis axis, double plane) {
			return switch (axis) {
				case X -> new AABB(plane, minU, minV, plane, maxU, maxV);
				case Y -> new AABB(minU, plane, minV, maxU, plane, maxV);
				case Z -> new AABB(minU, minV, plane, maxU, maxV, plane);
			};
		}
	}

	/** Classifies a world direction in the horizontal frame used by an oriented sensor. */
	public static Direction sensorDirection(Vec3 worldDirection, Vec3 pivot,
			UnaryOperator<Vec3> sensorToWorld) {
		Vec3 origin = sensorToWorld.apply(pivot);
		Direction nearest = Direction.NORTH;
		double nearestDot = Double.NEGATIVE_INFINITY;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			Vec3 basis = sensorToWorld.apply(pivot.add(direction.getStepX(), 0,
					direction.getStepZ())).subtract(origin);
			double dot = worldDirection.dot(basis);
			if (dot > nearestDot) {
				nearestDot = dot;
				nearest = direction;
			}
		}
		return nearest;
	}

	public static AABB transformBounds(AABB box, UnaryOperator<Vec3> transform) {
		double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
		double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
		for (int corner = 0; corner < 8; corner++) {
			Vec3 point = transform.apply(new Vec3(
					(corner & 1) == 0 ? box.minX : box.maxX,
					(corner & 2) == 0 ? box.minY : box.maxY,
					(corner & 4) == 0 ? box.minZ : box.maxZ));
			minX = Math.min(minX, point.x); maxX = Math.max(maxX, point.x);
			minY = Math.min(minY, point.y); maxY = Math.max(maxY, point.y);
			minZ = Math.min(minZ, point.z); maxZ = Math.max(maxZ, point.z);
		}
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}

	/** Tests all 15 face/edge axes, so empty corners of a rotated AABB never become walls. */
	public static boolean intersects(AABB worldBox, AABB localBox,
			UnaryOperator<Vec3> toWorld) {
		return intersects(worldBox, UnaryOperator.identity(), localBox, toWorld);
	}

	/** Uses Sable's ground-normal threshold and the actual face, rather than wall-side volume. */
	public static boolean hasSupport(AABB footProbe, UnaryOperator<Vec3> probeToWorld,
			AABB localBox, UnaryOperator<Vec3> boxToWorld) {
		Vec3 center = localBox.getCenter();
		Vec3 worldCenter = boxToWorld.apply(center);
		Vec3[] basis = {
				boxToWorld.apply(center.add(1, 0, 0)).subtract(worldCenter),
				boxToWorld.apply(center.add(0, 1, 0)).subtract(worldCenter),
				boxToWorld.apply(center.add(0, 0, 1)).subtract(worldCenter)
		};
		for (int axis = 0; axis < 3; axis++) {
			Vec3 normal = basis[(axis + 1) % 3].cross(basis[(axis + 2) % 3]).normalize();
			if (normal.dot(basis[axis]) < 0) normal = normal.scale(-1);
			if (normal.y > 0.6 && intersects(footProbe, probeToWorld,
					face(localBox, axis, true), boxToWorld)) return true;
			if (normal.y < -0.6 && intersects(footProbe, probeToWorld,
					face(localBox, axis, false), boxToWorld)) return true;
		}
		return false;
	}

	/** A zero-thickness rectangle is sufficient for the separating-axis face test. */
	private static AABB face(AABB box, int axis, boolean maximum) {
		return switch (axis) {
			case 0 -> new AABB(maximum ? box.maxX : box.minX, box.minY, box.minZ,
					maximum ? box.maxX : box.minX, box.maxY, box.maxZ);
			case 1 -> new AABB(box.minX, maximum ? box.maxY : box.minY, box.minZ,
					box.maxX, maximum ? box.maxY : box.minY, box.maxZ);
			default -> new AABB(box.minX, box.minY, maximum ? box.maxZ : box.minZ,
					box.maxX, box.maxY, maximum ? box.maxZ : box.minZ);
		};
	}

	/** Exact overlap for two independently transformed boxes, including native rotated player bounds. */
	public static boolean intersects(AABB firstBox, UnaryOperator<Vec3> firstToWorld,
			AABB secondBox, UnaryOperator<Vec3> secondToWorld) {
		Vec3 firstCenter = firstToWorld.apply(firstBox.getCenter());
		Vec3 secondCenter = secondToWorld.apply(secondBox.getCenter());
		Vec3 offset = secondCenter.subtract(firstCenter);
		Vec3[] firstEdges = transformedHalfEdges(firstBox, firstToWorld, firstCenter);
		Vec3[] secondEdges = transformedHalfEdges(secondBox, secondToWorld, secondCenter);
		for (int i = 0; i < 3; i++) {
			if (separated(firstEdges[i].cross(firstEdges[(i + 1) % 3]), offset,
					firstEdges, secondEdges)) return false;
			if (separated(secondEdges[i].cross(secondEdges[(i + 1) % 3]), offset,
					firstEdges, secondEdges)) return false;
			for (Vec3 secondEdge : secondEdges) {
				if (separated(firstEdges[i].cross(secondEdge), offset,
						firstEdges, secondEdges)) return false;
			}
		}
		return true;
	}

	private static Vec3[] transformedHalfEdges(AABB box, UnaryOperator<Vec3> transform, Vec3 center) {
		Vec3 localCenter = box.getCenter();
		return new Vec3[] {
				transform.apply(localCenter.add(box.getXsize() * 0.5, 0, 0)).subtract(center),
				transform.apply(localCenter.add(0, box.getYsize() * 0.5, 0)).subtract(center),
				transform.apply(localCenter.add(0, 0, box.getZsize() * 0.5)).subtract(center)
		};
	}

	private static boolean separated(Vec3 axis, Vec3 offset, Vec3[] firstEdges, Vec3[] secondEdges) {
		double length = axis.length();
		if (length < 1.0E-12) return false;
		double radius = 0;
		for (Vec3 edge : firstEdges) radius += Math.abs(axis.dot(edge));
		for (Vec3 edge : secondEdges) radius += Math.abs(axis.dot(edge));
		return Math.abs(axis.dot(offset)) >= radius - EPSILON * length;
	}
}
