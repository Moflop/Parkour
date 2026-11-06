package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.content.behavior.armhang.client.animation.player.ArmhangPoseMath;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmhangPoseMathTest {
	@Test
	void bothSkinsKeepOriginalThirdPersonArmLengthAndExactPalmContact() {
		for (boolean slim : new boolean[]{false, true}) for (double scale : new double[]{.5, 1, 1.5}) {
			for (double yaw : new double[]{-179, -90, -37, 0, 90, 179}) for (double turn : new double[]{-25, 0, 25}) {
				var rig = rig(yaw, turn, scale);
				for (int side : new int[]{-1, 1}) {
					double center = side * (slim ? .5 : 1);
					var arm = ArmhangPoseMath.arm(rig, rig.shoulder(side, slim), rig.palm(side), center, false);
					assertEquals(1, arm.length(), "Third-person arms may not stretch to chase a ledge");
					assertEquals(0, rig.toWorld(palm(arm, center)).distanceTo(rig.palm(side)), 1E-6);
				}
			}
		}
	}

	@Test
	void firstPersonAndThirdPersonPalmsShareTheSameWorldPointDuringExtremeViewTurns() {
		for (double yaw : new double[]{-179, -90, 0, 90, 179}) for (float pitch : new float[]{-85, 0, 85}) {
			var rig = rig(37, 15, 1);
			Vec3 camera = new Vec3(2, 21.62, 3);
			Vec3 look = Vec3.directionFromRotation(pitch, (float) yaw);
			Vec3 left = new Vec3(Math.cos(Math.toRadians(yaw)), 0, Math.sin(Math.toRadians(yaw)));
			Vec3 up = look.cross(left).normalize();
			for (int side : new int[]{-1, 1}) {
				Vec3 shoulder = ArmhangPoseMath.cameraShoulder(camera, look, up, left, side);
				assertTrue(shoulder.subtract(camera).dot(look) < -.31, "Hide the shoulder cut behind the camera");
				var first = ArmhangPoseMath.arm(rig, shoulder, rig.palm(side), side, true);
				var third = ArmhangPoseMath.arm(rig, rig.shoulder(side, false), rig.palm(side), side, false);
				assertTrue(Float.isFinite(first.length()) && first.length() > 0);
				assertEquals(0, rig.toWorld(palm(first, side, ArmhangPoseMath.FIRST_PERSON_THICKNESS))
						.distanceTo(rig.toWorld(palm(third, side))), 1E-6);
			}
		}
	}

	@Test
	void loadedCatchSettlesWithoutChangingTheMaterialPointOfEitherHand() {
		var neutral = rig(37, 0, 1);
		double largest = 0;
		for (double age = 0; age <= 15; age += .25) {
			double drop = ArmhangPoseMath.catchDrop(age, 1);
			largest = Math.max(largest, drop);
			assertTrue(drop >= 0 && drop < .065);
			var loaded = ArmhangPoseMath.rig(new Vec3(2, 20, 3), new Vec3(0, 0, 1),
					neutral.leftPalm(), neutral.rightPalm(), 1, drop, 0, 0);
			for (int side : new int[]{-1, 1}) {
				var arm = ArmhangPoseMath.arm(loaded, loaded.shoulder(side, false), loaded.palm(side), side, false);
				assertEquals(0, loaded.toWorld(palm(arm, side)).distanceTo(neutral.palm(side)), 1E-6);
			}
		}
		assertTrue(largest > .015, "A hard catch has a visible short body load");
		assertEquals(0, ArmhangPoseMath.catchDrop(0, 1));
		assertEquals(0, ArmhangPoseMath.catchDrop(10, 1));
		assertEquals(0, ArmhangPoseMath.catchDrop(3, 0));
	}

	@Test
	void liftingHitboxDoesNotDislocateTheShouldersDuringTheMantle() {
		for (boolean slim : new boolean[]{false, true}) for (double yaw : new double[]{-90, 0, 37, 179}) {
			Vec3 normal = Vec3.directionFromRotation(0, (float) yaw);
			Vec3 tangent = new Vec3(normal.z, 0, -normal.x);
			Vec3 center = new Vec3(2, 23, 3);
			Vec3 start = center.subtract(normal.scale(.34)).add(0, -1.7, 0);
			Vec3 lift = new Vec3(start.x, center.y + .018, start.z);
			Vec3 target = center.add(normal.scale(.42)).add(0, .018, 0);
			for (double age = 0; age <= 14; age += .25) {
				double progress = age <= 9 ? age / 9 : (age - 9) / 5;
				double eased = progress * progress * (3 - 2 * progress);
				Vec3 feet = age <= 9 ? start.lerp(lift, eased) : lift.lerp(target, eased);
				var rig = ArmhangPoseMath.mantleRig(feet, normal, center.add(tangent.scale(.23)),
						center.subtract(tangent.scale(.23)), 1, age);
				for (int side : new int[]{-1, 1}) {
					double centerX = side * (slim ? .5 : 1);
					Vec3 shoulder = rig.shoulder(side, slim);
					var arm = ArmhangPoseMath.arm(rig, shoulder, rig.palm(side), centerX, false);
					var actualShoulder = rig.toWorld(new Vec3(arm.pivot().x + Math.cos(arm.roll()) * centerX,
							arm.pivot().y + Math.sin(arm.roll()) * centerX, arm.pivot().z));
					assertTrue(actualShoulder.distanceTo(shoulder) < .20,
							"Shoulder separated at mantle tick " + age + ": " + actualShoulder.distanceTo(shoulder));
					assertEquals(0, rig.toWorld(palm(arm, centerX)).distanceTo(rig.palm(side)), 1E-6);
				}
			}
		}
	}

	@Test
	void differentLedgeHeightsKeepTheShouldersInTheJacket() {
		for (boolean slim : new boolean[]{false, true}) for (double height : new double[]{1.05, 1.4, 1.65, 1.8, 2}) {
			for (double turn : new double[]{-25, 0, 25}) {
				Vec3 left = new Vec3(.19, height + .03, .325);
				Vec3 right = new Vec3(-.19, height - .03, .325);
				var rig = ArmhangPoseMath.hangingRig(Vec3.ZERO, new Vec3(0, 0, 1), left, right,
						1, .02, .01, Math.toRadians(turn));
				for (int side : new int[]{-1, 1}) {
					double center = side * (slim ? .5 : 1);
					Vec3 shoulder = rig.shoulder(side, slim);
					var arm = ArmhangPoseMath.arm(rig, shoulder, rig.palm(side), center, false);
					var actual = rig.toWorld(new Vec3(arm.pivot().x + Math.cos(arm.roll()) * center,
							arm.pivot().y + Math.sin(arm.roll()) * center, arm.pivot().z));
					assertTrue(actual.distanceTo(shoulder) < .20);
				}
			}
		}
	}

	@Test
	void settledFirstPersonHandsAndForearmsAreInsideBothFieldOfViews() {
		for (double fov : new double[]{70, 110}) for (float yaw : new float[]{-179, -90, 0, 37, 90, 179}) {
			Vec3 forward = Vec3.directionFromRotation(0, yaw);
			Vec3 left = new Vec3(forward.z, 0, -forward.x);
			Vec3 up = new Vec3(0, 1, 0), camera = new Vec3(2, 21.61, 3);
			double vertical = Math.tan(Math.toRadians(fov * .5)), horizontal = vertical * 854 / 480;
			for (int side : new int[]{-1, 1}) {
				Vec3 palm = camera.add(forward.scale(.325)).add(up.scale(.14)).add(left.scale(side * .19));
				Vec3 shoulder = ArmhangPoseMath.cameraShoulder(camera, forward, up, left, side);
				for (double amount : new double[]{1}) {
					Vec3 point = shoulder.lerp(palm, amount).subtract(camera);
					double depth = point.dot(forward);
					assertTrue(depth > .05, "The palms must clear the near plane");
					assertTrue(Math.abs(point.dot(up)) < depth * vertical,
							"The palms must fit the vertical field of view");
					assertTrue(Math.abs(point.dot(left)) < depth * horizontal,
							"The palms must fit the horizontal field of view");
				}
				int visible = 0;
				for (int step = 0; step <= 20; step++) {
					Vec3 point = shoulder.lerp(palm, step / 20.0).subtract(camera);
					double depth = point.dot(forward);
					if (depth > .05 && Math.abs(point.dot(up)) < depth * vertical
							&& Math.abs(point.dot(left)) < depth * horizontal) visible++;
				}
				assertTrue(visible >= 4, "The world-space forearm must actually enter the view at FOV " + fov);
			}
		}
	}

	@Test
	void firstPersonArmVolumesLeaveTheCrosshairAndCentralViewClear() {
		for (boolean slim : new boolean[]{false, true}) for (double fov : new double[]{70, 110}) {
			Vec3 camera = new Vec3(0, 1.61, 0), forward = new Vec3(0, 0, 1);
			Vec3 up = new Vec3(0, 1, 0), left = new Vec3(1, 0, 0);
			double tangent = Math.tan(Math.toRadians(fov * .5));
			Vec3 leftPalm = camera.add(.19, .15, .325), rightPalm = camera.add(-.19, .15, .325);
			var rig = ArmhangPoseMath.hangingRig(Vec3.ZERO, forward, leftPalm, rightPalm, 1, 0, 0, 0);
			for (int side : new int[]{-1, 1}) {
				Vec3 shoulder = ArmhangPoseMath.cameraShoulder(camera, forward, up, left, side);
				double center = side * (slim ? .5 : 1);
				var arm = ArmhangPoseMath.arm(rig, shoulder, rig.palm(side), center, true);
				double thickness = ArmhangPoseMath.FIRST_PERSON_THICKNESS;
				var transform = new Matrix4d().translation(arm.pivot().x, arm.pivot().y, arm.pivot().z)
						.rotateZ(arm.roll()).rotateX(arm.pitch()).scale(thickness, arm.length(), thickness);
				var inverse = new Matrix4d(transform).invert();
				Vec3 modelCamera = rig.toModel(camera);
				for (double x : new double[]{-.16, -.08, 0, .08, .16}) {
					for (double y : new double[]{-.12, -.06, 0, .06, .12}) {
						Vec3 worldRay = forward.add(left.scale(x * tangent * 854 / 480)).add(up.scale(y * tangent));
						Vec3 modelEnd = rig.toModel(camera.add(worldRay));
						Vector3d origin = inverse.transformPosition(modelCamera.x, modelCamera.y, modelCamera.z, new Vector3d());
						Vector3d direction = inverse.transformDirection(modelEnd.x - modelCamera.x, modelEnd.y - modelCamera.y,
								modelEnd.z - modelCamera.z, new Vector3d());
						assertFalse(rayHitsArm(origin, direction, center, slim),
								"The rendered arm volume may not fill the central view at FOV " + fov);
					}
				}
			}
		}
	}

	private static boolean rayHitsArm(Vector3d origin, Vector3d direction, double center, boolean slim) {
		double halfWidth = slim ? 1.5 : 2;
		double[] min = {center - halfWidth, -2, -2}, max = {center + halfWidth, 10, 2};
		double[] start = {origin.x, origin.y, origin.z}, delta = {direction.x, direction.y, direction.z};
		double near = .05, far = Double.POSITIVE_INFINITY;
		for (int axis = 0; axis < 3; axis++) {
			if (Math.abs(delta[axis]) < 1E-8) {
				if (start[axis] < min[axis] || start[axis] > max[axis]) return false;
			} else {
				double a = (min[axis] - start[axis]) / delta[axis], b = (max[axis] - start[axis]) / delta[axis];
				near = Math.max(near, Math.min(a, b));
				far = Math.min(far, Math.max(a, b));
				if (near > far) return false;
			}
		}
		return near <= far;
	}

	private static ArmhangPoseMath.Rig rig(double yaw, double turn, double scale) {
		Vec3 normal = Vec3.directionFromRotation(0, (float) yaw);
		Vec3 tangent = new Vec3(normal.z, 0, -normal.x);
		Vec3 feet = new Vec3(2, 20, 3);
		Vec3 center = feet.add(normal.scale(.34 * scale)).add(0, 1.65 * scale, 0);
		Vec3 left = center.add(tangent.scale(.23 * scale)).add(0, .025 * scale, 0);
		Vec3 right = center.subtract(tangent.scale(.23 * scale)).add(0, -.025 * scale, 0);
		return ArmhangPoseMath.rig(feet, normal, left, right, scale, .02 * scale, .01 * scale, Math.toRadians(turn));
	}

	private static Vec3 palm(ArmhangPoseMath.Arm arm, double center) {
		return palm(arm, center, 1);
	}

	private static Vec3 palm(ArmhangPoseMath.Arm arm, double center, double thickness) {
		var point = new Matrix4d().translation(arm.pivot().x, arm.pivot().y, arm.pivot().z)
				.rotateZ(arm.roll()).rotateX(arm.pitch()).scale(thickness, arm.length(), thickness)
				.transformPosition(center, 10, 0, new Vector3d());
		return new Vec3(point.x, point.y, point.z);
	}
}
