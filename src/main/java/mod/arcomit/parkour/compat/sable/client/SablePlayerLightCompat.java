package mod.arcomit.parkour.compat.sable.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Keep Sable's precise render probe from inheriting a shadow sampled at a different point. */
public final class SablePlayerLightCompat {

	private SablePlayerLightCompat() {}

	/**
	 * Sable 2.0.6 first samples ship skylight at the centre of the vanilla world block,
	 * then samples again at the interpolated eye in getPackedLightCoords. The second
	 * pass takes the minimum, so a block centre inside a wall permanently darkens an
	 * eye outside that wall. Supply only the world baseline here; Sable's second pass
	 * still applies all ship shadows and its skylight scale at the precise probe.
	 */
	@Nullable
	public static BlockPos worldSkyProbe(Entity entity, BlockPos vanillaBlock, float partialTick) {
		if (!(entity instanceof Player player)
				|| ParkourChecks.isVanillaState(ParkourContext.get(player))) return null;

		// Match Sable's RETURN hook, including any entity-specific light-probe offset.
		Vec3 probeOffset = entity.getLightProbePosition(partialTick).subtract(entity.getEyePosition(partialTick));
		Vec3 renderProbe = Sable.HELPER.getEyePositionInterpolated(entity, partialTick).add(probeOffset);
		BlockPos renderBlock = BlockPos.containing(renderProbe);
		Level level = entity.level();
		if (intersectsStructure(level, vanillaBlock)
				|| (!renderBlock.equals(vanillaBlock) && intersectsStructure(level, renderBlock))) {
			return renderBlock;
		}
		return null;
	}

	private static boolean intersectsStructure(Level level, BlockPos probeBlock) {
		for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(probeBlock))) {
			if (!subLevel.isRemoved()) return true;
		}
		return false;
	}
}
