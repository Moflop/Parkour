package mod.arcomit.parkour.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.mechanic.climb.ClimbableEdge;
import mod.arcomit.parkour.utils.ClimbablePoles;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Adds Parkour's poles to the position-based ladder query used by Sable. */
public final class SableClimbingCompat {

	private SableClimbingCompat() {}

	@Nullable
	public static BlockPos findClimbablePole(LivingEntity entity) {
		SubLevelContainer container = SubLevelContainer.getContainer(entity.level());
		if (container == null) return null;
		for (SubLevel subLevel : container.queryIntersecting(new BoundingBox3d(entity.getBoundingBox()))) {
			if (subLevel.isRemoved()) continue;
			// The inverse pose yields actual plot block coordinates. Both neighbors
			// must be checked in that same grid, even on a translated/rotated structure.
			Vec3 localFeet = subLevel.logicalPose().transformPositionInverse(entity.position());
			BlockPos pos = BlockPos.containing(localFeet);
			if (ClimbablePoles.isClimbable(entity.level(), pos)) return pos;
		}
		return null;
	}

	/** Predict the world-Y climb path in the structure's current plot coordinates. */
	@Nullable
	public static Double findUpwardClimbBoostFactor(Player player) {
		SubLevelContainer container = SubLevelContainer.getContainer(player.level());
		if (container == null) return null;
		// The native block cache can still name the previous cell after movement.
		// Use it to identify the structure, then inspect the current feet in that grid.
		SubLevel nativeSource = player.getLastClimbablePos()
				.map(pos -> Sable.HELPER.getContaining(player.level(), pos)).orElse(null);
		for (SubLevel subLevel : container.queryIntersecting(new BoundingBox3d(player.getBoundingBox()))) {
			if (subLevel.isRemoved()) continue;
			Vec3 localFeet = subLevel.logicalPose().transformPositionInverse(player.position());
			BlockPos contact = BlockPos.containing(localFeet);
			boolean pole = ParkourConfig.canClimbMoreBlocks && ClimbablePoles.isClimbable(player.level(), contact);
			Vec3 probeFeet = localFeet;
			if (!pole) {
				if (subLevel != nativeSource) continue;
				if (!isNativeLadder(player, contact)) {
					// Sable's getInBlockState samples 0.001 world blocks above the feet.
					probeFeet = subLevel.logicalPose().transformPositionInverse(player.position().add(0.0, 0.001, 0.0));
					if (!isNativeLadder(player, BlockPos.containing(probeFeet))) return 0.0;
				}
			}

			// Physics structures need not be perfectly level. Transform the actual
			// movement direction instead of disabling boosts for any nonzero tilt.
			Vec3 localUp = subLevel.logicalPose().transformPositionInverse(player.position().add(0.0, 1.0, 0.0))
					.subtract(localFeet);
			return ClimbableEdge.upwardBoostFactor(probeFeet, localUp,
					pos -> isNativeLadder(player, pos)
							|| ParkourConfig.canClimbMoreBlocks && ClimbablePoles.isClimbable(player.level(), pos));
		}
		return null;
	}

	private static boolean isNativeLadder(Player player, BlockPos pos) {
		if (!player.level().isLoaded(pos)) return false;
		var state = player.level().getBlockState(pos);
		return state.is(BlockTags.CLIMBABLE) && state.isLadder(player.level(), pos, player);
	}
}
