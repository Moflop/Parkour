package mod.arcomit.parkour.content.behavior.barswing;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** 与玩家/世界解耦的单摆积分及跃出速度计算，单位为方块和 tick。 */
public final class BarSwingMath {
	public static final double LENGTH = 2.2;
	public static final double MAX_ANGLE = 1.0;
	public static final double MAX_ANGULAR_SPEED = 0.23;

	private BarSwingMath() {}

	public record Step(double angle, double speed) {}

	/** 前进输入顺着当前摆动加力，后退输入刹摆；无输入时保留自然惯性。 */
	public static Step step(double angle, double speed, double input) {
		double drive = input > 0 ? Math.copySign(0.008 * input, Math.abs(speed) > 1.0E-5 ? speed : -angle + 0.001) : 0;
		speed = (speed - 0.032 * Math.sin(angle) + drive) * (input < 0 ? 0.83 : 0.994);
		speed = Mth.clamp(speed, -MAX_ANGULAR_SPEED, MAX_ANGULAR_SPEED);
		// 以最高摆角的势能限制加力，接近端点时自然减速，避免撞上角度上限突然反弹。
		double energySpeed = Math.sqrt(Math.max(0, 0.064 * (Math.cos(angle) - Math.cos(MAX_ANGLE))));
		if (angle * speed > 0) speed = Mth.clamp(speed, -energySpeed, energySpeed);
		double next = angle + speed;
		if (Math.abs(next) > MAX_ANGLE) {
			next = Math.copySign(MAX_ANGLE, next);
			speed = 0;
		}
		return new Step(next, speed);
	}

	public static Vec3 position(Vec3 anchor, Vec3 forward, double angle) {
		return position(anchor, forward, new Vec3(0, -1, 0), angle);
	}

	public static Vec3 position(Vec3 anchor, Vec3 forward, Vec3 down, double angle) {
		return anchor.add(forward.scale(LENGTH * Math.sin(angle))).add(down.scale(LENGTH * Math.cos(angle)));
	}

	public static Vec3 tangent(Vec3 forward, double angle, double speed) {
		return tangent(forward, new Vec3(0, -1, 0), angle, speed);
	}

	public static Vec3 tangent(Vec3 forward, Vec3 down, double angle, double speed) {
		return forward.scale(LENGTH * Math.cos(angle) * speed).subtract(down.scale(LENGTH * Math.sin(angle) * speed));
	}

	public static float clampYaw(float yaw, float forwardYaw) {
		return forwardYaw + Mth.clamp(Mth.wrapDegrees(yaw - forwardYaw), -75, 75);
	}

	/** 宽容的起跳窗口：始终有基础抬升，摆荡动量决定额外距离，视线只选择横杆的哪一侧。 */
	public static Vec3 launch(Vec3 forward, double angle, double speed, Vec3 look, double strength) {
		double sign = look.dot(forward) < -0.25 ? -1 : 1;
		double horizontal = Mth.clamp(0.52 + Math.abs(speed) * 1.35, 0.52, 0.83) * strength;
		double vertical = Mth.clamp(0.52 + Math.max(0, LENGTH * Math.sin(angle) * speed) * 0.55, 0.52, 0.72) * strength;
		// 后摆高点跃向前方时先跨过横杆，避免脚尖擦杆触发原生结构的落地减速。
		double clearance = Mth.clamp((-angle - 0.45) / 0.45, 0, 1);
		clearance = clearance * clearance * (3 - 2 * clearance);
		vertical = Math.max(vertical, (0.52 + 0.15 * clearance) * strength);
		return forward.scale(sign * horizontal).add(0, vertical, 0);
	}
}
