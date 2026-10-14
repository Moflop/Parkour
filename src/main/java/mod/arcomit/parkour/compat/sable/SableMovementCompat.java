package mod.arcomit.parkour.compat.sable;

import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/** Keeps Sable inertia in vanilla travel rather than replaying it in parkour corrections. */
public final class SableMovementCompat {
	private static final ThreadLocal<Player> CORRECTION_PLAYER = new ThreadLocal<>();

	private SableMovementCompat() {}

	/** Sable reports every tracked server player as grounded without testing foot support. */
	public static boolean needsGroundCheck(Player player) {
		return player instanceof ServerPlayer && player instanceof EntityMovementExtension movement
				&& movement.sable$getTrackingSubLevel() != null;
	}

	/** Only corrections issued through this helper suppress the solver's tracking transport. */
	public static boolean isCorrectionMove(Entity entity) {
		return CORRECTION_PLAYER.get() == entity;
	}

	/** Attaching to a wall ends inertia left over from a previously released structure. */
	public static void beginGrip(Player player) {
		if (player.isLocalPlayer() && player instanceof EntityMovementExtension movement
				&& movement.sable$getTrackingSubLevel() == null
				&& player instanceof LivingEntityMovementExtension livingMovement) {
			livingMovement.sable$getInheritedVelocity().zero();
		}
	}

	public static void move(Player player, MoverType type, Vec3 motion) {
		if (player.isLocalPlayer() && player instanceof LivingEntityMovementExtension movement) {
			Player previous = CORRECTION_PLAYER.get();
			CORRECTION_PLAYER.set(player);
			try {
				withoutInheritedMotion(movement.sable$getInheritedVelocity(), () -> player.move(type, motion));
			} finally {
				if (previous == null) {
					CORRECTION_PLAYER.remove();
				} else {
					CORRECTION_PLAYER.set(previous);
				}
			}
		} else {
			player.move(type, motion);
		}
	}

	/** Package-visible so restoration can be checked without constructing a Minecraft player. */
	static void withoutInheritedMotion(Vector3d inherited, Runnable movement) {
		double x = inherited.x;
		double y = inherited.y;
		double z = inherited.z;
		inherited.zero();
		try {
			movement.run();
		} finally {
			inherited.set(x, y, z);
		}
	}
}
