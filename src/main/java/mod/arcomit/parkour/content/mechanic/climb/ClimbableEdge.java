package mod.arcomit.parkour.content.mechanic.climb;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.utils.ClimbablePoles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.Optional;
import java.util.function.Predicate;

/** Read-only lookahead along the climbable column the player currently touches. */
public final class ClimbableEdge {

	private static final double BRAKE_DISTANCE = 0.35;
	private static final double FULL_BOOST_DISTANCE = 1.25;
	private static final double BOUNDARY_EPSILON = 1.0E-9;

	private ClimbableEdge() {}

	/** Call after onClimbable has established this movement step's contact. */
	public static double upwardBoostFactor(Player player) {
		if (player.isSpectator()) return 0.0;
		BlockPos feet = player.blockPosition();
		// Match Parkour's onClimbable head injection before the native ladder query.
		if (ParkourConfig.canClimbMoreBlocks && ClimbablePoles.isClimbable(player.level(), feet)) {
			return upwardBoostFactor(player.getY(), feet, pos -> isWorldClimbable(player, pos));
		}
		Double dynamicFactor = DynamicCollisions.findUpwardClimbBoostFactor(player);
		if (dynamicFactor != null) return dynamicFactor;

		// CommonHooks is safe for the CURRENT contact only. In fullBoundingBoxLadders
		// mode it ignores its position argument and always searches the player's box.
		Optional<BlockPos> contact = CommonHooks.isLivingOnLadder(
				player.level().getBlockState(feet), player.level(), feet, player);
		return contact.map(pos -> upwardBoostFactor(player.getY(), pos,
				candidate -> isWorldClimbable(player, candidate))).orElse(0.0);
	}

	/** Coordinates and the predicate must use the same upright block grid. */
	public static double upwardBoostFactor(double feetY, BlockPos contact, Predicate<BlockPos> isClimbable) {
		if (!Double.isFinite(feetY) || !isClimbable.test(contact)) return 0.0;
		BlockPos top = contact;
		// Never search another column or skip a gap to reach an unrelated ladder.
		while (top.getY() + 1.0 - feetY < FULL_BOOST_DISTANCE) {
			BlockPos next = top.above();
			if (!isClimbable.test(next)) break;
			top = next;
		}
		return factorForDistance(top.getY() + 1.0 - feetY);
	}

	/**
	 * Follow the actual upward movement through a structure's block grid. localUp is
	 * one WORLD block of upward travel transformed into that grid, so the distance
	 * stays in world units even when the structure is pitched, rolled or scaled.
	 */
	public static double upwardBoostFactor(Vec3 localFeet, Vec3 localUp, Predicate<BlockPos> isClimbable) {
		if (!finite(localFeet) || !finite(localUp) || localUp.lengthSqr() < BOUNDARY_EPSILON) return 0.0;
		BlockPos cell = BlockPos.containing(localFeet);
		double distance = 0.0;
		// Walk grid boundaries, not spaced samples: even a short gap ends this grip.
		// A rigid pose crosses at most a handful of cells over the lookahead distance.
		for (int crossings = 0; crossings < 32; crossings++) {
			if (!isClimbable.test(cell)) return factorForDistance(distance);
			double x = nextBoundary(localFeet.x, localUp.x, cell.getX());
			double y = nextBoundary(localFeet.y, localUp.y, cell.getY());
			double z = nextBoundary(localFeet.z, localUp.z, cell.getZ());
			double next = Math.min(x, Math.min(y, z));
			if (next >= FULL_BOOST_DISTANCE) return 1.0;
			if (!Double.isFinite(next) || next < distance - BOUNDARY_EPSILON) return 0.0;
			distance = Math.max(0.0, next);
			cell = cell.offset(
					x <= next + BOUNDARY_EPSILON ? (int) Math.signum(localUp.x) : 0,
					y <= next + BOUNDARY_EPSILON ? (int) Math.signum(localUp.y) : 0,
					z <= next + BOUNDARY_EPSILON ? (int) Math.signum(localUp.z) : 0);
		}
		return 0.0;
	}

	private static double nextBoundary(double origin, double direction, int cell) {
		if (Math.abs(direction) < BOUNDARY_EPSILON) return Double.POSITIVE_INFINITY;
		return ((direction > 0.0 ? cell + 1.0 : cell) - origin) / direction;
	}

	private static double factorForDistance(double remaining) {
		return Mth.clamp((remaining - BRAKE_DISTANCE) / (FULL_BOOST_DISTANCE - BRAKE_DISTANCE), 0.0, 1.0);
	}

	private static boolean finite(Vec3 vector) {
		return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}

	private static boolean isWorldClimbable(Player player, BlockPos pos) {
		BlockPos feet = player.blockPosition();
		return player.level().isLoaded(pos)
				&& (player.level().getBlockState(pos).isLadder(player.level(), pos, player)
				|| ParkourConfig.canClimbMoreBlocks && pos.getX() == feet.getX() && pos.getZ() == feet.getZ()
						&& ClimbablePoles.isClimbable(player.level(), pos));
	}
}
