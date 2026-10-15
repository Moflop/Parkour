package mod.arcomit.parkour.content.behavior.barswing.client;

import mod.arcomit.parkour.content.behavior.barswing.BarSwingAnchor;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingMath;
import net.minecraft.world.phys.Vec3;

/** 仅用于渲染的抓握几何：躯干服从世界重力，左右手分别对准横杆。 */
public final class BarSwingPoseMath {
	public static final double PIXEL = 0.9375 / 16;
	private static final double PAL_ORIGIN = 1.501 * 0.9375 - 0.75;
	private static final double PALM_Y = 10;

	private BarSwingPoseMath() {}

	public record Hand(Vec3 shoulder, Vec3 palm) {}
	/** Signed distances along the frame's bar axis, retained for one grip. */
	public record GripOffsets(double left, double right) {}
	/** Model-pixel offsets chosen at acquisition, never reselected while swinging. */
	public record BodyPlacement(double hang, double depth, double lowering) {}

	public record Rig(Vec3 origin, Vec3 left, Vec3 up, Vec3 back, Vec3 anchor, Vec3 gripAxis, double pixel, Hand leftHand, Hand rightHand) {
		public Vec3 toWorld(Vec3 point) {
			return origin.add(left.scale(point.x * pixel)).subtract(up.scale(point.y * pixel)).add(back.scale(point.z * pixel));
		}

		public Vec3 toModel(Vec3 point) {
			Vec3 offset = point.subtract(origin);
			return new Vec3(offset.dot(left), -offset.dot(up), offset.dot(back)).scale(1 / pixel);
		}

		public Vec3 grip(int side) {
			return toWorld((side > 0 ? leftHand : rightHand).palm);
		}

		public Vec3 shoulder(int side) {
			return toWorld((side > 0 ? leftHand : rightHand).shoulder);
		}
	}

	public record Arm(Vec3 pivot, float pitch, float roll, float length) {}

	public static Rig rig(BarSwingAnchor.Frame frame, Vec3 feet, double angle, double scale) {
		return rig(frame, feet, angle, scale, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public static Rig rig(BarSwingAnchor.Frame frame, Vec3 feet, double angle, double scale, double minGrip, double maxGrip) {
		return rig(frame, feet, angle, scale, minGrip, maxGrip, false);
	}

	public static Rig rig(BarSwingAnchor.Frame frame, Vec3 feet, double angle, double scale, double minGrip, double maxGrip, boolean slim) {
		return rig(frame, feet, angle, scale, minGrip, maxGrip, slim, Math.toRadians(-10) + angle * 0.6, 0);
	}

	public static Rig rig(BarSwingAnchor.Frame frame, Vec3 feet, double angle, double scale, double minGrip, double maxGrip, boolean slim, double pitch, double yaw) {
		return fitAcquisitionRig(frame, feet, angle, scale, minGrip, maxGrip, slim, pitch, yaw);
	}

	public static GripOffsets gripOffsets(Rig rig, BarSwingAnchor.Frame frame) {
		return new GripOffsets(rig.grip(1).subtract(frame.anchor()).dot(frame.axis()),
				rig.grip(-1).subtract(frame.anchor()).dot(frame.axis()));
	}

	public static BodyPlacement bodyPlacement(Rig restingRig, BarSwingAnchor.Frame frame, Vec3 feet,
			double angle, double pitch, double yaw) {
		Vec3 offset = restingRig.origin.subtract(frame.anchor());
		BodyPlacement placement = new BodyPlacement(-offset.dot(restingRig.up) / restingRig.pixel,
				offset.dot(restingRig.back) / restingRig.pixel, 0);
		double scale = restingRig.pixel / PIXEL;
		double lowering = requiredLowering(bodyRig(frame, feet, angle, scale, placement), pitch, yaw, 0);
		// Reserve clearance for the swing and allowed head turns once, before the grab fades in.
		for (int step = 0; step <= 20; step++) {
			double swing = BarSwingMath.MAX_ANGLE * (step / 10.0 - 1);
			Vec3 swingFeet = BarSwingMath.position(frame.anchor(), frame.forward(), frame.down(), swing);
			Rig candidate = bodyRig(frame, swingFeet, swing, scale, placement);
			for (int lookPitch = -75; lookPitch <= 65; lookPitch += 10) {
				for (int lookYaw = -75; lookYaw <= 75; lookYaw += 15) {
					lowering = requiredLowering(candidate, Math.toRadians(lookPitch) + swing * 0.6,
							Math.toRadians(lookYaw), lowering);
				}
			}
		}
		return new BodyPlacement(placement.hang, placement.depth, lowering > 0 ? lowering + 0.5 : 0);
	}

	private static double requiredLowering(Rig rig, double pitch, double yaw, double minimum) {
		if (!headIntersects(lowered(rig, minimum), pitch, yaw, 0)) return minimum;
		double low = minimum, high = Math.max(32, minimum + 1);
		for (int i = 0; i < 12; i++) {
			double middle = (low + high) * 0.5;
			if (headIntersects(lowered(rig, middle), pitch, yaw, 0)) low = middle; else high = middle;
		}
		return high;
	}

	private static Rig lowered(Rig rig, double pixels) {
		return new Rig(rig.origin.add(0, -pixels * rig.pixel, 0), rig.left, rig.up, rig.back,
				rig.anchor, rig.gripAxis, rig.pixel, null, null);
	}

	private static Rig bodyRig(BarSwingAnchor.Frame frame, Vec3 feet, double angle, double scale, BodyPlacement placement) {
		Vec3 left = frame.forward().cross(new Vec3(0, -1, 0)).normalize();
		Vec3 up = gravityUp(frame, angle), back = up.cross(left).normalize();
		Vec3 axis = frame.axis().dot(left) < 0 ? frame.axis().scale(-1) : frame.axis();
		double pixel = PIXEL * scale;
		// A single continuous swing curve replaces the per-frame front/back clearance search.
		double depth = placement.depth * Math.max(0, 1 - square(angle / BarSwingMath.MAX_ANGLE));
		double hang = placement.hang + (Math.abs(placement.depth) - Math.abs(depth)) * 0.6;
		Vec3 base = feet.add(up.scale(frame.anchor().subtract(feet).dot(up)));
		Vec3 origin = base.subtract(up.scale(hang * pixel)).add(back.scale(depth * pixel))
				.add(0, -placement.lowering * pixel, 0);
		return new Rig(origin, left, up, back, frame.anchor(), axis, pixel, null, null);
	}

	public static Rig rig(BarSwingAnchor.Frame frame, Vec3 feet, double angle, double scale, boolean slim,
			GripOffsets grips, BodyPlacement placement) {
		Rig body = bodyRig(frame, feet, angle, scale, placement);
		Vec3 anchor = body.toModel(frame.anchor());
		Vec3 axis = new Vec3(frame.axis().dot(body.left), -frame.axis().dot(body.up), frame.axis().dot(body.back));
		double left = grips.left / body.pixel, right = grips.right / body.pixel;
		double reach = gripReach(body.pixel);
		// Refit only the shoulders; locked palms never inherit the body's clearance offset.
		Hand leftHand = fallbackHand(anchor, axis, left, left, 1, slim, reach);
		Hand rightHand = fallbackHand(anchor, axis, right, right, -1, slim, reach);
		return new Rig(body.origin, body.left, body.up, body.back, body.anchor, body.gripAxis,
				body.pixel, leftHand, rightHand);
	}

	/** Acquisition-only fitting; ongoing animation uses the cached BodyPlacement overload. */
	private static Rig fitAcquisitionRig(BarSwingAnchor.Frame frame, Vec3 feet, double angle, double scale, double minGrip, double maxGrip, boolean slim, double pitch, double yaw) {
		Vec3 forward = frame.forward();
		Vec3 left = forward.cross(new Vec3(0, -1, 0)).normalize();
		Vec3 up = gravityUp(frame, angle);
		Vec3 back = up.cross(left).normalize();
		double pixel = PIXEL * scale;
		double reach = gripReach(pixel);
		boolean reversed = frame.axis().dot(left) < 0;
		Vec3 axis = reversed ? frame.axis().scale(-1) : frame.axis();
		double min = (reversed ? -maxGrip : minGrip) / pixel, max = (reversed ? -minGrip : maxGrip) / pixel;
		Vec3 modelAxis = new Vec3(axis.dot(left), -axis.dot(up), axis.dot(back));
		// Lower the whole rig for head clearance. A small rearward offset on steep bars
		// lets rigid arms still reach without pulling the shoulders far out of the torso.
		Vec3 base = feet.add(up.scale(frame.anchor().subtract(feet).dot(up)));
		Vec3 relative = frame.anchor().subtract(base).scale(1 / pixel);
		Vec3 rod = new Vec3(relative.dot(left), -relative.dot(up), relative.dot(back));
		Rig result = null;
		double tilt = Math.clamp((Math.abs(axis.y) - 0.2) / 0.5, 0, 1);
		double minimumDepth = 5.5 * tilt * tilt * (3 - 2 * tilt) * Math.max(0, 1 - angle * angle);
		for (int direction : new int[]{1, -1}) for (double distance = minimumDepth; distance <= 8; distance += 0.0625) {
			double depth = distance * direction;
			double hang = 10.75 - distance * 0.6;
			Vec3 origin = base.subtract(up.scale(hang * pixel)).add(back.scale(depth * pixel));
			Rig candidate = new Rig(origin, left, up, back, frame.anchor(), axis, pixel, null, null);
			if (headIntersects(candidate, pitch, yaw, 0)) {
				double low = hang, high = 32;
				for (int i = 0; i < 12; i++) {
					double middle = (low + high) * 0.5;
					candidate = new Rig(base.subtract(up.scale(middle * pixel)).add(back.scale(depth * pixel)), left, up, back, frame.anchor(), axis, pixel, null, null);
					if (headIntersects(candidate, pitch, yaw, 0)) low = middle; else high = middle;
				}
				hang = high + 0.025;
				origin = base.subtract(up.scale(hang * pixel)).add(back.scale(depth * pixel));
			}
			Vec3 anchor = rod.add(0, -hang, -depth);
			Hand leftHand = hand(anchor, modelAxis, min, max, 1, slim, up, back, reach);
			Hand rightHand = hand(anchor, modelAxis, min, max, -1, slim, up, back, reach);
			if (leftHand != null && rightHand != null) {
				return new Rig(origin, left, up, back, frame.anchor(), axis, pixel, leftHand, rightHand);
			} else {
				result = new Rig(origin, left, up, back, frame.anchor(), axis, pixel,
						fallbackHand(anchor, modelAxis, min, max, 1, slim, reach), fallbackHand(anchor, modelAxis, min, max, -1, slim, reach));
			}
		}
		return result;
	}

	private static double shoulderX(boolean slim) { return slim ? 4.75 : 5.25; }
	private static double square(double value) { return value * value; }
	// Contact the shaft's surface; cap the offset inside its inscribed radius even on scaled players.
	private static double gripReach(double pixel) { return PALM_Y + Math.min(1, 0.06 / pixel); }

	private static Hand hand(Vec3 anchor, Vec3 axis, double min, double max, int side, boolean slim, Vec3 up, Vec3 back, double reach) {
		double x = side * shoulderX(slim), z = Math.clamp(anchor.z, -3, 3);
		if (min == max) return handAt(anchor, axis, min, x, z, up, back, reach);
		double available = side > 0 ? anchor.x + axis.x * max : -anchor.x - axis.x * min;
		double minimumX = Math.min(0.1, Math.max(0.025, available * 0.9));
		double a = square(axis.x) + square(axis.z);
		double b = (anchor.x - x) * axis.x + (anchor.z - z) * axis.z;
		double discriminant = b * b - a * (square(anchor.x - x) + square(anchor.z - z) - reach * reach);
		if (discriminant < 0) return null;
		min = Math.max(min, (-b - Math.sqrt(discriminant)) / a);
		max = Math.min(max, (-b + Math.sqrt(discriminant)) / a);
		if (side > 0) min = Math.max(min, (minimumX - anchor.x) / axis.x);
		else max = Math.min(max, (-minimumX - anchor.x) / axis.x);
		if (min > max) return null;
		double desired = Math.clamp(side * 8, min, max);
		Hand direct = handAt(anchor, axis, desired, x, z, up, back, reach);
		if (direct != null) return direct;
		double closest = Double.NaN;
		for (int i = 0; i <= 64; i++) {
			double t = min + (max - min) * i / 64;
			if ((Double.isNaN(closest) || Math.abs(t - desired) < Math.abs(closest - desired))
					&& handAt(anchor, axis, t, x, z, up, back, reach) != null) closest = t;
		}
		if (Double.isNaN(closest)) return null;
		double blocked = desired;
		for (int i = 0; i < 14; i++) {
			double middle = (closest + blocked) * 0.5;
			if (handAt(anchor, axis, middle, x, z, up, back, reach) == null) blocked = middle; else closest = middle;
		}
		return handAt(anchor, axis, closest, x, z, up, back, reach);
	}

	private static Hand handAt(Vec3 anchor, Vec3 axis, double t, double x, double z, Vec3 up, Vec3 back, double reach) {
		Vec3 palm = anchor.add(axis.scale(t));
		double squaredRise = reach * reach - square(palm.x - x) - square(palm.z - z);
		if (squaredRise < 0.75 * 0.75) return null;
		double rise = Math.sqrt(squaredRise), y = palm.y + rise;
		if (y < -5 || y > 8 || up.y * rise + back.y * (palm.z - z) < 0.5) return null;
		return new Hand(new Vec3(x, y, z), palm);
	}

	private static Hand fallbackHand(Vec3 anchor, Vec3 axis, double min, double max, int side, boolean slim, double reach) {
		Vec3 palm = anchor.add(axis.scale(Math.clamp(side * 8, min, max)));
		double x = side * shoulderX(slim), z = Math.clamp(anchor.z, -3, 3);
		double rise = Math.sqrt(Math.max(0, reach * reach - square(palm.x - x) - square(palm.z - z)));
		return new Hand(new Vec3(x, palm.y + rise, z), palm);
	}

	/** 以帽层外轮廓和杆身外接半径做保守相交检查。 */
	public static boolean headIntersects(Rig rig, double pitch, double yaw, double forward) {
		Vec3 p = inverseHead(rig.toModel(rig.anchor).add(0, 0, forward), pitch, yaw);
		Vec3 a = rig.gripAxis;
		Vec3 d = inverseHead(new Vec3(a.dot(rig.left), -a.dot(rig.up), a.dot(rig.back)), pitch, yaw);
		double[] origin = {p.x, p.y, p.z}, direction = {d.x, d.y, d.z};
		double radius = 0.09 / rig.pixel;
		double[] min = {-4.5, -8.5, -4.5}, max = {4.5, 0.5, 4.5};
		double[] boundaries = new double[8];
		int count = 1;
		boundaries[0] = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < 3; i++) {
			if (Math.abs(direction[i]) > 1e-9) {
				boundaries[count++] = (min[i] - origin[i]) / direction[i];
				boundaries[count++] = (max[i] - origin[i]) / direction[i];
			}
		}
		boundaries[count++] = Double.POSITIVE_INFINITY;
		java.util.Arrays.sort(boundaries, 0, count);
		// Squared line-to-box distance is quadratic between crossings of the six planes.
		for (int interval = 1; interval < count; interval++) {
			double low = boundaries[interval - 1], high = boundaries[interval];
			double sample = !Double.isFinite(low) ? high - 1 : !Double.isFinite(high) ? low + 1 : (low + high) * 0.5;
			double quadratic = 0, linear = 0;
			for (int i = 0; i < 3; i++) {
				double coordinate = origin[i] + direction[i] * sample;
				if (coordinate >= min[i] && coordinate <= max[i]) continue;
				double edge = coordinate < min[i] ? min[i] : max[i];
				quadratic += direction[i] * direction[i]; linear += direction[i] * (origin[i] - edge);
			}
			double t = quadratic > 1e-12 ? Math.clamp(-linear / quadratic, low, high) : sample;
			double distance = 0;
			for (int i = 0; i < 3; i++) {
				double coordinate = origin[i] + direction[i] * t;
				distance += square(coordinate - Math.clamp(coordinate, min[i], max[i]));
			}
			if (distance <= radius * radius) return true;
		}
		return false;
	}

	private static Vec3 inverseHead(Vec3 v, double pitch, double yaw) {
		double x = Math.cos(yaw) * v.x - Math.sin(yaw) * v.z, z = Math.sin(yaw) * v.x + Math.cos(yaw) * v.z;
		return new Vec3(x, Math.cos(pitch) * v.y + Math.sin(pitch) * z, -Math.sin(pitch) * v.y + Math.cos(pitch) * z);
	}

	/** 保留前后摆姿，但不把横杆的侧倾带进躯干。 */
	public static Vec3 gravityUp(BarSwingAnchor.Frame frame, double angle) {
		return new Vec3(0, Math.cos(angle), 0).subtract(frame.forward().scale(Math.sin(angle)));
	}

	/** 用完整基向量还原 PAL 的 Z/Y/X 顺序，避免转身时侧倾方向反转。 */
	public static Vec3 bodyRotation(Rig rig, double modelYaw) {
		double sin = Math.sin(modelYaw - Math.PI), cos = Math.cos(modelYaw - Math.PI);
		double pitch = -Math.atan2(-sin * rig.up.x + cos * rig.up.z, -sin * rig.back.x + cos * rig.back.z);
		double yaw = Math.asin(Math.max(-1, Math.min(1, sin * rig.left.x - cos * rig.left.z)));
		double roll = Math.atan2(-rig.left.y, -cos * rig.left.x - sin * rig.left.z);
		return new Vec3(pitch, yaw, roll);
	}

	/** PAL 根骨骼位移发生在原版玩家缩放、Y 翻转及模型原点平移之前。 */
	public static Vec3 rootOffset(Rig rig, Vec3 feet, double scale) {
		return rig.origin.subtract(feet).scale(1 / scale).subtract(rig.up.scale(PAL_ORIGIN)).add(0, -0.75, 0);
	}

	/** 第三人称保持原版整段手臂的尺寸，移动肩端适应高低握点；仅第一人称补偿长度。 */
	public static Arm arm(Rig rig, Vec3 shoulder, Vec3 palm, double centerX, boolean stretch) {
		Vec3 start = rig.toModel(shoulder), end = rig.toModel(palm);
		Vec3 delta = end.subtract(start);
		double length = Math.max(0.001, delta.length());
		double pitch = Math.atan2(delta.z, -Math.sqrt(delta.x * delta.x + delta.y * delta.y));
		if (pitch > 0) pitch -= Math.PI * 2;
		double roll = Math.atan2(delta.x, -delta.y);
		Vec3 center = new Vec3(Math.cos(roll) * centerX, Math.sin(roll) * centerX, 0);
		if (!stretch) start = end.subtract(delta.scale(gripReach(rig.pixel) / length));
		return new Arm(start.subtract(center), (float) pitch, (float) roll, stretch ? (float) (length / PALM_Y) : 1);
	}

	/** 第一人称肩端在实际镜头后方，广角和转头时也不会露出手臂截面。 */
	public static Vec3 cameraShoulder(Vec3 camera, Vec3 look, Vec3 up, Vec3 left, int side) {
		return camera.subtract(look.scale(0.28)).subtract(up.scale(0.85)).add(left.scale(side * 0.28));
	}
}
