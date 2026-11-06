package mod.arcomit.parkour.content.behavior.armhang.client.animation.player;

import net.minecraft.world.phys.Vec3;

/** PAL model geometry shared by the first- and third-person ledge poses. */
public final class ArmhangPoseMath {
	public static final double PIXEL = .9375 / 16;
	public static final float FIRST_PERSON_THICKNESS = .60F;
	private static final double PAL_ORIGIN = 1.501 * .9375 - .75;
	private static final double PALM_Y = 10;

	private ArmhangPoseMath() {}

	public record Rig(Vec3 origin, Vec3 left, Vec3 up, Vec3 back, double pixel,
			Vec3 leftPalm, Vec3 rightPalm) {
		public Vec3 toWorld(Vec3 model) {
			return origin.add(left.scale(model.x * pixel)).subtract(up.scale(model.y * pixel))
					.add(back.scale(model.z * pixel));
		}

		public Vec3 toModel(Vec3 world) {
			Vec3 delta = world.subtract(origin);
			return new Vec3(delta.dot(left), -delta.dot(up), delta.dot(back)).scale(1 / pixel);
		}

		public Vec3 palm(int side) { return side > 0 ? leftPalm : rightPalm; }

		public Vec3 shoulder(int side, boolean slim) {
			return toWorld(new Vec3(side * (slim ? 4.75 : 5.25), slim ? 2.5 : 2, 0));
		}
	}

	public record Arm(Vec3 pivot, float pitch, float roll, float length) {}

	/** The torso follows gravity; tilt of the actual ledge remains in the independent palm targets. */
	public static Rig rig(Vec3 feet, Vec3 normal, Vec3 leftPalm, Vec3 rightPalm,
			double scale, double drop, double sway, double turn) {
		Vec3 facing = new Vec3(normal.x, 0, normal.z).normalize();
		if (facing.lengthSqr() < 1E-8) facing = new Vec3(0, 0, 1);
		Vec3 left = new Vec3(facing.z, 0, -facing.x);
		Vec3 turnedLeft = left.scale(Math.cos(turn)).add(facing.scale(Math.sin(turn)));
		Vec3 up = new Vec3(0, 1, 0);
		Vec3 back = up.cross(turnedLeft);
		Vec3 origin = feet.add(0, (PAL_ORIGIN + .75) * scale - drop, 0).add(left.scale(sway));
		return new Rig(origin, turnedLeft, up, back, PIXEL * scale, leftPalm, rightPalm);
	}

	/** A slab and a full block may catch at different eye heights; shoulders still hang beneath the palms. */
	public static Rig hangingRig(Vec3 feet, Vec3 normal, Vec3 leftPalm, Vec3 rightPalm,
			double scale, double drop, double sway, double turn) {
		Rig base = rig(feet, normal, leftPalm, rightPalm, scale, drop, sway, turn);
		Vec3 origin = new Vec3(base.origin.x, (leftPalm.y + rightPalm.y) * .5 - .32 * scale - drop,
				base.origin.z);
		return new Rig(origin, base.left, base.up, base.back, base.pixel, leftPalm, rightPalm);
	}

	/** A pulling torso travels around the palms; it does not rise rigidly with the clearance hitbox. */
	public static Rig mantleRig(Vec3 feet, Vec3 normal, Vec3 leftPalm, Vec3 rightPalm,
			double scale, double age) {
		Rig standing = rig(feet, normal, leftPalm, rightPalm, scale, 0, 0, 0);
		Vec3 facing = standing.back.scale(-1);
		Vec3 center = leftPalm.add(rightPalm).scale(.5);
		double phase = Math.clamp(age / 9, 0, 1) * Math.PI;
		double rise = -.43 * scale * Math.cos(phase);
		double depth = Math.sqrt(square(PALM_Y * PIXEL * scale) - square(rise) - square(.07 * scale));
		Vec3 heldOrigin = center.subtract(facing.scale(depth)).add(0, rise + 2 * PIXEL * scale, 0);
		double recovery = smoothStep((float) ((age - 8) / 6));
		Vec3 origin = heldOrigin.lerp(standing.origin, recovery);
		double lean = .28 * Math.sin(Math.clamp(age / 14, 0, 1) * Math.PI);
		Vec3 up = new Vec3(0, Math.cos(lean), 0).add(facing.scale(Math.sin(lean)));
		Vec3 back = up.cross(standing.left).normalize();
		Rig pulling = new Rig(origin, standing.left, up, back, standing.pixel, leftPalm, rightPalm);
		return new Rig(origin, pulling.left, up, back, pulling.pixel,
				releasedPalm(pulling, 1, smoothStep((float) ((age - 8) / 5))),
				releasedPalm(pulling, -1, smoothStep((float) ((age - 9) / 5))));
	}

	private static Vec3 releasedPalm(Rig rig, int side, double release) {
		if (release == 0) return rig.palm(side);
		Vec3 shoulder = rig.shoulder(side, false);
		Vec3 toward = rig.palm(side).subtract(shoulder);
		Vec3 down = rig.up.scale(-1);
		Vec3 direction = toward.normalize().lerp(down, release).normalize();
		if (direction.lengthSqr() < 1E-8) direction = down;
		double length = PALM_Y * rig.pixel;
		// Keep the releasing hand on an arm-length arc as the torso rises, so the
		// collision body's early lift cannot pull the shoulder out of the jacket.
		double reach = Math.clamp(toward.length() * (1 - release) + length * release,
				length - .08, length + .08);
		return shoulder.add(direction.scale(reach));
	}

	private static double square(double value) { return value * value; }

	/** Fixed third-person limb size, with a shoulder adjustment; the palm is never moved to fit. */
	public static Arm arm(Rig rig, Vec3 shoulder, Vec3 palm, double centerX, boolean firstPerson) {
		Vec3 start = rig.toModel(shoulder), end = rig.toModel(palm);
		Vec3 delta = end.subtract(start);
		double length = Math.max(.001, delta.length());
		double pitch = Math.atan2(delta.z, -Math.sqrt(delta.x * delta.x + delta.y * delta.y));
		if (pitch > 0) pitch -= Math.PI * 2;
		double roll = Math.atan2(delta.x, -delta.y);
		double centerOffset = centerX * (firstPerson ? FIRST_PERSON_THICKNESS : 1);
		Vec3 center = new Vec3(Math.cos(roll) * centerOffset, Math.sin(roll) * centerOffset, 0);
		if (!firstPerson) start = end.subtract(delta.scale(PALM_Y / length));
		return new Arm(start.subtract(center), (float) pitch, (float) roll,
				firstPerson ? (float) (length / PALM_Y) : 1);
	}

	/** Both perspectives share their palm target; only the unseen shoulder changes for the camera. */
	public static Vec3 cameraShoulder(Vec3 camera, Vec3 look, Vec3 up, Vec3 left, int side) {
		// Like the bar rig, the shoulder stays behind the camera and outside its
		// central view. The shallower drop keeps the nearby ledge visible without
		// pushing a full-width arm through the near plane beside the crosshair.
		return camera.subtract(look.scale(.32)).subtract(up.scale(.55)).add(left.scale(side * .45));
	}

	public static Vec3 bodyRotation(Rig rig, double modelYaw) {
		double sin = Math.sin(modelYaw - Math.PI), cos = Math.cos(modelYaw - Math.PI);
		double pitch = -Math.atan2(-sin * rig.up.x + cos * rig.up.z, -sin * rig.back.x + cos * rig.back.z);
		double yaw = Math.asin(Math.clamp(sin * rig.left.x - cos * rig.left.z, -1, 1));
		double roll = Math.atan2(-rig.left.y, -cos * rig.left.x - sin * rig.left.z);
		return new Vec3(pitch, yaw, roll);
	}

	public static Vec3 rootOffset(Rig rig, Vec3 feet, double scale) {
		return rig.origin.subtract(feet).scale(1 / scale).subtract(rig.up.scale(PAL_ORIGIN)).add(0, -.75, 0);
	}

	/** A short loaded dip and one damped recovery; it never introduces continuous camera shake. */
	public static double catchDrop(double age, double impact) {
		if (age <= 0 || age >= 10) return 0;
		double progress = age / 10;
		return Math.clamp(impact, 0, 1) * .065 * Math.sin(progress * Math.PI) * Math.exp(-progress * 2);
	}

	public static float smoothStep(float value) {
		float t = Math.clamp(value, 0, 1);
		return t * t * (3 - 2 * t);
	}
}
