package mod.arcomit.parkour.compat;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Optional integrations only enumerate structures intersecting the query. */
public interface DynamicCollisionProvider {
	List<DynamicCollisionSource> sources(Level level, AABB worldBounds);
}
