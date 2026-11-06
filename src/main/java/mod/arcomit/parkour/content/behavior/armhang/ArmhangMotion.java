package mod.arcomit.parkour.content.behavior.armhang;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Small deterministic movement rules shared by prediction, collision planning and tests. */
public final class ArmhangMotion {
	public static final int MANTLE_TICKS = 14;
	public static final int LIFT_TICKS = 9;
	private ArmhangMotion() {}

	public static double shimmySpeed(double previous, double input, double maximum) {
		if (!Double.isFinite(previous) || !Double.isFinite(input) || !Double.isFinite(maximum) || maximum <= 0) return 0;
		double target = Mth.clamp(input, -1, 1) * maximum;
		// Three ticks to full speed; release and reversal stop the old stroke promptly.
		double step = maximum * (Math.abs(target) < 1.0E-6 || previous * target < 0 ? 0.55 : 0.34);
		return previous + Mth.clamp(target - previous, -step, step);
	}

	public static double smooth(double value) {
		double t = Mth.clamp(value, 0, 1);
		return t * t * (3 - 2 * t);
	}

	/** Lift the entire body clear of the lip before moving its footprint onto the platform. */
	public static Vec3 mantlePosition(Vec3 start, Vec3 lift, Vec3 target, double tick) {
		return tick <= LIFT_TICKS ? start.lerp(lift, smooth(tick / LIFT_TICKS))
				: lift.lerp(target, smooth((tick - LIFT_TICKS) / (MANTLE_TICKS - LIFT_TICKS)));
	}
}
