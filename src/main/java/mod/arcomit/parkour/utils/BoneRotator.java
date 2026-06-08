package mod.arcomit.parkour.utils;

import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 骨骼旋转工具，安全追加相对旋转（解决万向节死锁与基础动画覆盖问题）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class BoneRotator {

	private static final ThreadLocal<Quaternionf> LOCAL_QUAT =
			ThreadLocal.withInitial(Quaternionf::new);
	private static final ThreadLocal<Vector3f> LOCAL_VEC =
			ThreadLocal.withInitial(Vector3f::new);

	/**
	 * 在骨骼当前旋转基础上叠加相对旋转（绕自身局部坐标轴），解决万向节死锁与基础动画覆盖问题。
	 * <p>
	 * Yaw 绕局部 Y 轴，Pitch 绕局部 Z 轴，Roll 绕局部 X 轴。
	 * 使用线程局部四元数避免频繁分配，零角度调用会提前返回。
	 *
	 * @param bone     目标骨骼，不可为null；修改其 rotX/rotY/rotZ 字段
	 * @param yawRad   绕局部 Y 轴的旋转弧度
	 * @param pitchRad 绕局部 Z 轴的旋转弧度
	 * @param rollRad  绕局部 X 轴的旋转弧度
	 */
	public static void applyRelativeRotation(PlayerAnimBone bone, float yawRad, float pitchRad,
			float rollRad) {
		if (yawRad == 0f && pitchRad == 0f && rollRad == 0f) {
			return;
		}

		Quaternionf q = LOCAL_QUAT.get().rotationZYX(bone.rotX, bone.rotY, bone.rotZ);

		if (yawRad != 0f)
			q.rotateLocalY(yawRad);
		if (pitchRad != 0f)
			q.rotateLocalZ(pitchRad);
		if (rollRad != 0f)
			q.rotateLocalX(rollRad);

		Vector3f euler = LOCAL_VEC.get();
		q.getEulerAnglesZYX(euler);

		bone.rotX = euler.z;
		bone.rotY = euler.y;
		bone.rotZ = euler.x;
	}

	/**
	 * 在骨骼上叠加 yaw/pitch 相对旋转（roll=0），等价于 {@link #applyRelativeRotation(PlayerAnimBone, float, float, float)}。
	 */
	public static void applyRelativeRotation(PlayerAnimBone bone, float yawRad,
			float pitchRad) {
		applyRelativeRotation(bone, yawRad, pitchRad, 0f);
	}
}
