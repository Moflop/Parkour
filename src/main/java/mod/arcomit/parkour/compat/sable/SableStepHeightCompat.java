package mod.arcomit.parkour.compat.sable;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.mechanic.stepheight.StepHeightIncreaseMechanic;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3dc;
import org.joml.Vector3d;

/** Makes Sable's step clearance test respect the configured height, including its endpoint. */
public final class SableStepHeightCompat {
	private SableStepHeightCompat() {}

	public static boolean isEnabled(Entity entity) {
		return ParkourConfig.enableStepHeightIncrease && entity instanceof Player player
				&& !ParkourChecks.isVanillaState(ParkourContext.get(player))
				&& ParkourChecks.canPerformBehavior(player);
	}

	/** Sprint can change before the ordinary Post-tick attribute refresh. */
	public static void refreshStepHeight(Entity entity) {
		if (isEnabled(entity)) StepHeightIncreaseMechanic.handle((Player) entity);
	}

	/**
	 * A sloping side contact can push down before the ground contact is resolved.
	 * Include that correction when locating the grounded start of the clearance
	 * sweep, otherwise the same legal step is rejected on the wall's downhill face.
	 * This changes only the query reference; the native solver owns actual motion.
	 */
	public static double groundedStepReferenceCorrection(Vector3dc motion,
			Vector3dc contactCorrection, Vector3dc up) {
		return Math.max(0.0, -motion.dot(up) - contactCorrection.dot(up));
	}

	/** Raising the reference point must keep it outside a slightly overhanging contact plane. */
	public static Vector3d groundedContactRetreat(double groundCorrection, Vector3dc normal, Vector3dc up) {
		double vertical = normal.dot(up);
		double penetration = -groundCorrection * vertical;
		if (penetration <= 0.0) return new Vector3d();
		Vector3d horizontal = new Vector3d(normal).fma(-vertical, up);
		double horizontalLengthSquared = horizontal.lengthSquared();
		if (horizontalLengthSquared < 1.0E-12) return new Vector3d();
		horizontal.mul(penetration / horizontalLengthSquared);
		// This only repairs near-grid contacts; a visibly leaning wall still uses
		// the native rejection, rather than inventing a large unrequested retreat.
		return horizontal.lengthSquared() <= .002 * .002 ? horizontal : horizontal.zero();
	}

	/** The iteration after the endpoint still advances, so a blocked endpoint terminates. */
	public static double stepIncrement(double currentHeight, double maximumHeight, double increment) {
		if (currentHeight < maximumHeight) return Math.min(increment, maximumHeight - currentHeight);
		return increment;
	}
}
