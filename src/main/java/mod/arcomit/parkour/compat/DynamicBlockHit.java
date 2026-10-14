package mod.arcomit.parkour.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Keeps the material and its local position together, including for contextual sound types. */
public record DynamicBlockHit(DynamicCollisionSource source, Level blockLevel,
		BlockPos localPos, BlockState state, AABB localBox) {
	public DynamicBlockHit {
		localPos = localPos.immutable();
	}

	public Vec3 worldCenter() {
		return source.toWorld(localBox.getCenter());
	}
}
