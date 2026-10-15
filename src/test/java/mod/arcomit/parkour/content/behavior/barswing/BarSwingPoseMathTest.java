package mod.arcomit.parkour.content.behavior.barswing;

import mod.arcomit.parkour.content.behavior.barswing.client.BarSwingPoseMath;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BarSwingPoseMathTest {
	@Test
	void visibleFingertipsTouchTheShaftWithoutStretchingScaledThirdPersonArms() {
		var frame = BarSwingAnchor.frame(new Vec3(0, 4, 0), new Vec3(1, .5, 0).normalize(), new Vec3(0, 0, 1));
		for (double scale : new double[]{.5, 1, 2}) for (double angle : new double[]{-.8, 0, .8}) {
			var rig = BarSwingPoseMath.rig(frame, BarSwingMath.position(frame.anchor(), frame.forward(), angle), angle, scale);
			for (int side : new int[]{-1, 1}) {
				var arm = BarSwingPoseMath.arm(rig, rig.shoulder(side), rig.grip(side), side, false);
				assertEquals(1, arm.length());
				assertTrue(rig.toWorld(palm(arm, side, 10)).distanceTo(rig.grip(side)) <= .060001);
			}
		}
	}

	@Test
	void vanillaWalkOnlyReturnsAfterTheReleaseHasSettled() {
		float previous = 0;
		for (float tick = 0; tick < 24; tick += .1F) {
			float weight = mod.arcomit.parkour.content.behavior.barswing.client.BarSwingJumpPlayerAnimModifier.recoveryWeight(tick);
			assertTrue(weight >= previous && weight <= 1);
			if (tick <= 13) assertEquals(0, weight);
			if (tick >= 21) assertEquals(1, weight);
			previous = weight;
		}
	}

	@Test
	void gravityPoseKeepsRigidArmsConnectedWhileLimitingSidewaysDrift() {
		for (double tilt : new double[]{-1, -.7, -.35, 0, .35, .7, 1}) {
			var frame = BarSwingAnchor.frame(new Vec3(4, 8, 12), new Vec3(Math.cos(tilt), Math.sin(tilt), 0), new Vec3(0, 0, 1));
			for (double swing = -1; swing <= 1; swing += .05) for (double range : new double[]{.46, 1.5, Double.POSITIVE_INFINITY}) for (boolean slim : new boolean[]{false, true}) {
				Vec3 feet = BarSwingMath.position(frame.anchor(), frame.forward(), frame.down(), swing);
				var rig = BarSwingPoseMath.rig(frame, feet, swing, 1, -range, range, slim);
				Vec3 expectedUp = new Vec3(0, Math.cos(swing), 0).subtract(frame.forward().scale(Math.sin(swing)));
				assertEquals(0, rig.up().distanceTo(expectedUp), 1e-9);
				if (Double.isInfinite(range)) assertEquals(0, rig.origin().subtract(feet).dot(rig.left()), 1e-9);
				assertEquals(0, rig.up().dot(rig.left()), 1e-9);
				for (int side : new int[]{-1, 1}) {
					double center = side * (slim ? .5 : 1);
					Vec3 shoulder = rig.shoulder(side);
					double height = rig.toModel(shoulder).y;
					assertTrue(height >= -5.001 && height <= 8.001, "Shoulder height=" + height + " tilt=" + tilt + " swing=" + swing + " range=" + range);
					var arm = BarSwingPoseMath.arm(rig, shoulder, rig.grip(side), center, false);
					assertEquals(1, arm.length());
					assertTrue(torsoGap(arm, center, slim) * BarSwingPoseMath.PIXEL < .2,
							"Detached arm: tilt=" + tilt + " swing=" + swing + " range=" + range + " slim=" + slim);
					assertEquals(0, rig.toWorld(palm(arm, center)).distanceTo(rig.grip(side)), 1e-6,
							"Grip drift: tilt=" + tilt + " swing=" + swing + " slim=" + slim);
					assertTrue(rig.grip(side).y >= shoulder.y + .025, "Palm below shoulder: tilt=" + tilt + " swing=" + swing);
				}
			}
		}
	}

	@Test
	void restingTorsoAlignsHorizontallyAndHeadClearsTheRod() {
		for (double tilt : new double[]{-1, -.7, -.35, 0, .35, .7, 1}) for (double swing : new double[]{-1, -.5, 0, .5, 1}) for (double range : new double[]{.46, 1.5}) {
			var frame = BarSwingAnchor.frame(new Vec3(4, 8, 12), new Vec3(Math.cos(tilt), Math.sin(tilt), 0), new Vec3(0, 0, 1));
			Vec3 feet = BarSwingMath.position(frame.anchor(), frame.forward(), frame.down(), swing);
			for (boolean slim : new boolean[]{false, true}) for (double yaw : new double[]{-75, -60, 0, 60, 75}) for (double pitch : new double[]{-75, -32.5, -10, 42.5, 65}) {
				double headPitch = Math.toRadians(pitch) + swing * .6;
				var rig = BarSwingPoseMath.rig(frame, feet, swing, 1, -range, range, slim, headPitch, Math.toRadians(yaw));
				assertFalse(BarSwingPoseMath.headIntersects(rig, headPitch, Math.toRadians(yaw), 0), "Head clip: tilt=" + tilt + " swing=" + swing + " range=" + range);
				if (swing == 0) assertTrue(rig.origin().subtract(feet).horizontalDistance() <= .53);
				for (int side : new int[]{-1, 1}) {
					double center = side * (slim ? .5 : 1);
					var arm = BarSwingPoseMath.arm(rig, rig.shoulder(side), rig.grip(side), center, false);
					assertTrue(torsoGap(arm, center, slim) * BarSwingPoseMath.PIXEL < .2, "Look shoulder gap: tilt=" + tilt + " swing=" + swing + " pitch=" + pitch + " yaw=" + yaw + " slim=" + slim + "\n" + rig);
					assertTrue(rig.grip(side).y >= rig.shoulder(side).y + .025, "Look palm below shoulder");
				}
			}
		}
	}

	@Test
	void gripsNearEitherRodEndKeepShouldersConnected() {
		for (double tilt : new double[]{-.7, 0, .7}) for (double shift : new double[]{-.38, .38}) for (double swing : new double[]{-.8, 0, .8}) {
			var frame = BarSwingAnchor.frame(new Vec3(4, 8, 12), new Vec3(Math.cos(tilt), Math.sin(tilt), 0), new Vec3(0, 0, 1));
			Vec3 feet = BarSwingMath.position(frame.anchor(), frame.forward(), frame.down(), swing);
			var rig = BarSwingPoseMath.rig(frame, feet, swing, 1, -.46 + shift, .46 + shift);
			for (int side : new int[]{-1, 1}) {
				var arm = BarSwingPoseMath.arm(rig, rig.shoulder(side), rig.grip(side), side, false);
				double y = rig.toModel(rig.shoulder(side)).y;
				assertTrue(y >= -5.001 && y <= 8.001, "End shoulder=" + y + " tilt=" + tilt + " shift=" + shift + " swing=" + swing);
				assertTrue(torsoGap(arm, side, false) * BarSwingPoseMath.PIXEL < .2);
			}
		}
	}

	@Test
	void longBarGripsDoNotJumpWhileTheBodySwings() {
		for (double tilt : new double[]{-.7, -.35, 0, .35, .7}) {
			var frame = BarSwingAnchor.frame(new Vec3(4, 8, 12), new Vec3(Math.cos(tilt), Math.sin(tilt), 0), new Vec3(0, 0, 1));
			BarSwingPoseMath.Rig previous = null;
			for (double swing = -1; swing <= 1; swing += .005) {
				Vec3 feet = BarSwingMath.position(frame.anchor(), frame.forward(), frame.down(), swing);
				var rig = BarSwingPoseMath.rig(frame, feet, swing, 1, -1.5, 1.5);
				if (previous != null) for (int side : new int[]{-1, 1})
					assertTrue(rig.grip(side).distanceTo(previous.grip(side)) < .05, "Grip jump: tilt=" + tilt + " swing=" + swing + " side=" + side + " distance=" + rig.grip(side).distanceTo(previous.grip(side)) + "\nPrevious=" + previous + "\nCurrent=" + rig);
				previous = rig;
			}
		}
	}

	/** Separating-axis check against vanilla's torso and full, unscaled arm cuboids. */
	private static double torsoGap(BarSwingPoseMath.Arm arm, double center, boolean slim) {
		var transform = new Matrix4d().translation(arm.pivot().x, arm.pivot().y, arm.pivot().z)
				.rotateZ(arm.roll()).rotateX(arm.pitch());
		var delta = transform.transformPosition(center, 4, 0, new Vector3d()).sub(0, 6, 0);
		Vector3d[] axes = {new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, 1)};
		Vector3d[] rotated = java.util.Arrays.stream(axes).map(v -> transform.transformDirection(new Vector3d(v))).toArray(Vector3d[]::new);
		var normals = new java.util.ArrayList<Vector3d>(java.util.List.of(axes));
		normals.addAll(java.util.List.of(rotated));
		for (Vector3d a : axes) for (Vector3d b : rotated) normals.add(a.cross(b, new Vector3d()));
		double gap = -Double.MAX_VALUE;
		for (Vector3d n : normals) if (n.lengthSquared() > 1e-12) {
			n = new Vector3d(n).normalize();
			double armExtent = (slim ? 1.5 : 2) * Math.abs(n.dot(rotated[0])) + 6 * Math.abs(n.dot(rotated[1])) + 2 * Math.abs(n.dot(rotated[2]));
			gap = Math.max(gap, Math.abs(delta.dot(n)) - armExtent - 4 * Math.abs(n.x) - 6 * Math.abs(n.y) - 2 * Math.abs(n.z));
		}
		return gap;
	}

	@Test
	void rootCompensationMatchesTheActualVanillaAndPalTransformOrder() {
		Vec3 feet = new Vec3(2, 5, -3), anchor = new Vec3(2.2, 7.1, -2.5);
		for (double yaw : new double[]{-175, -90, 0, 45, 179}) for (double angle : new double[]{-1, 0, 1}) for (double tilt : new double[]{-.8, 0, .8}) {
			double rad = Math.toRadians(yaw), bodyYaw = rad + 0.6;
			Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
			var frame = BarSwingAnchor.frame(anchor, new Vec3(forward.z, tilt, -forward.x).normalize(), forward);
			var rig = BarSwingPoseMath.rig(frame, feet, angle, 1);
			Vec3 rotation = BarSwingPoseMath.bodyRotation(rig, bodyYaw);
			Vec3 offset = BarSwingPoseMath.rootOffset(rig, feet, 1);
			double x = offset.x * Math.cos(bodyYaw) + offset.z * Math.sin(bodyYaw);
			double z = offset.x * Math.sin(bodyYaw) - offset.z * Math.cos(bodyYaw);
			var actual = new Matrix4d().translation(feet.x, feet.y, feet.z).rotateY(Math.PI - bodyYaw)
					.translate(-x, offset.y + .75, z).rotateZYX(rotation.z, -rotation.y, -rotation.x)
					.translate(0, -.75, 0).scale(-.9375, -.9375, .9375).translate(0, -1.501, 0);
			for (Vec3 point : new Vec3[]{Vec3.ZERO, new Vec3(6, -8, 0), new Vec3(-5, 2, 1), new Vec3(0, 24, 0)}) {
				Vector3d world = actual.transformPosition(point.x / 16, point.y / 16, point.z / 16, new Vector3d());
				assertEquals(0, rig.toWorld(point).distanceTo(new Vec3(world.x, world.y, world.z)), 1e-6,
						"yaw=" + yaw + " tilt=" + tilt + " angle=" + angle + " point=" + point + " rotation=" + rotation);
			}
		}
	}

	@Test
	void firstPersonShouldersStayBehindTheCameraWithoutMovingTheHands() {
		var frame = BarSwingAnchor.frame(new Vec3(0, 2.5, 0), new Vec3(1, .4, 0).normalize(), new Vec3(0, 0, 1));
		for (double swing : new double[]{-1, 0, 1}) for (double yaw : new double[]{-75, 0, 75}) for (double pitch : new double[]{-75, -10, 65}) {
			Vec3 feet = BarSwingMath.position(frame.anchor(), frame.forward(), frame.down(), swing);
			var rig = BarSwingPoseMath.rig(frame, feet, swing, 1);
			Vec3 camera = feet.add(0, 1.62, 0);
			Vec3 look = Vec3.directionFromRotation((float) pitch, (float) yaw);
			Vec3 left = new Vec3(Math.cos(Math.toRadians(yaw)), 0, Math.sin(Math.toRadians(yaw)));
			Vec3 up = look.cross(left).normalize();
			for (int side : new int[]{-1, 1}) {
				Vec3 shoulder = BarSwingPoseMath.cameraShoulder(camera, look, up, left, side);
				assertTrue(shoulder.subtract(camera).dot(look) < -.27);
				var arm = BarSwingPoseMath.arm(rig, shoulder, rig.grip(side), side, true);
				assertEquals(0, rig.toWorld(palm(arm, side, 10)).distanceTo(rig.grip(side)), 1e-6);
				assertTrue(Float.isFinite(arm.length()) && arm.length() > 0);
			}
		}
	}

	private static Vec3 palm(BarSwingPoseMath.Arm arm, double center) {
		return palm(arm, center, 11);
	}

	private static Vec3 palm(BarSwingPoseMath.Arm arm, double center, double y) {
		var point = new Matrix4d().translation(arm.pivot().x, arm.pivot().y, arm.pivot().z)
				.rotateZ(arm.roll()).rotateX(arm.pitch()).scale(1, arm.length(), 1)
				.transformPosition(center, y, 0, new Vector3d());
		return new Vec3(point.x, point.y, point.z);
	}
}
