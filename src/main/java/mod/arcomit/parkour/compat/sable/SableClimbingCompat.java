package mod.arcomit.parkour.compat.sable;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mod.arcomit.parkour.utils.ClimbablePoles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
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
}
