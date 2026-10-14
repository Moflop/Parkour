package mod.arcomit.parkour.compat;

import mod.arcomit.parkour.compat.create.CreateCollisionProvider;
import mod.arcomit.parkour.compat.sable.SableCollisionProvider;
import mod.arcomit.parkour.compat.sable.SableClimbingCompat;
import mod.arcomit.parkour.compat.sable.SableMovementCompat;
import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.utils.FilteredBlockCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** Additional parkour queries; physical collision resolution stays with Create/Sable. */
public final class DynamicCollisions {
	private DynamicCollisions() {}

	// Lazy loading keeps either integration optional on both logical sides.
	private static class Providers {
		private static final boolean SABLE = ModList.get().isLoaded("sable");
		private static final List<DynamicCollisionProvider> ALL = create();

		private static List<DynamicCollisionProvider> create() {
			List<DynamicCollisionProvider> providers = new ArrayList<>();
			if (ModList.get().isLoaded("create")) providers.add(new CreateCollisionProvider());
			if (SABLE) providers.add(new SableCollisionProvider());
			return List.copyOf(providers);
		}
	}

	public static boolean isEnabled() {
		return !Providers.ALL.isEmpty();
	}

	/** Optional Sable lookup; ordinary ladders retain their native implementation. */
	@Nullable
	public static BlockPos findClimbablePole(LivingEntity entity) {
		return Providers.SABLE ? SableClimbingCompat.findClimbablePole(entity) : null;
	}

	/** Additional corrections still use the normal collision solver, without replaying inertia. */
	public static void move(Player player, MoverType type, Vec3 motion) {
		if (player instanceof ServerPlayer && !DynamicSurfaceMotion.supportsServerPlayerCorrections(player)) {
			return;
		}
		if (Providers.SABLE) SableMovementCompat.move(player, type, motion);
		else player.move(type, motion);
	}

	/** Call only while a wall action is active, before setting its relative velocity. */
	public static void clearSurfaceInertia(Player player) {
		if (Providers.SABLE) SableMovementCompat.beginGrip(player);
	}

	/** Sable reports tracked server players as grounded even when only touching a wall. */
	public static boolean isOnGround(Player player) {
		if (!player.onGround()) return false;
		if (!Providers.SABLE || !SableMovementCompat.needsGroundCheck(player)) return true;
		AABB bounds = player.getBoundingBox();
		double skin = ParkourConstants.AABB_DEFLATE_EPSILON;
		AABB support = new AABB(bounds.minX + skin, bounds.minY - 0.05,
				bounds.minZ + skin, bounds.maxX - skin, bounds.minY + skin, bounds.maxZ - skin);
		return new FilteredBlockCollisions<>(player.level(), player, support, false, null,
				(pos, shape) -> shape).hasNext()
				|| hasSensorSupport(player.level(), player, support, null);
	}

	/** A nearby side wall cannot substitute for an upward-facing support surface. */
	public static boolean hasSensorSupport(Level level, Entity entity, AABB box,
			@Nullable TagKey<Block> skipTag) {
		AABB searchBox = sensorSearchBounds(entity, box);
		for (DynamicCollisionProvider provider : Providers.ALL) {
			for (DynamicCollisionSource source : provider.sources(level, searchBox)) {
				if (source.isValid() && source.hasSupport(box, source.sensorTransform(entity), entity, skipTag)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasBlockCollision(Level level, @Nullable Entity entity, AABB box,
			@Nullable TagKey<Block> skipTag) {
		return findCollision(level, entity, box, skipTag) != null;
	}

	/** Check an already oriented body without rotating its world-space movement again. */
	public static boolean hasTransformedBlockCollision(Level level, @Nullable Entity entity,
			AABB localBox, UnaryOperator<Vec3> queryToWorld, @Nullable TagKey<Block> skipTag) {
		return hasTransformedBlockCollision(level, entity, localBox, queryToWorld, skipTag, source -> true);
	}

	/** Excludes a structure when its caller already checks that structure at a substep pose. */
	public static boolean hasTransformedBlockCollision(Level level, @Nullable Entity entity,
			AABB localBox, UnaryOperator<Vec3> queryToWorld, @Nullable TagKey<Block> skipTag,
			Predicate<DynamicCollisionSource> acceptsSource) {
		AABB searchBox = CollisionGeometry.transformBounds(localBox, queryToWorld);
		for (DynamicCollisionProvider provider : Providers.ALL) {
			for (DynamicCollisionSource source : provider.sources(level, searchBox)) {
				if (source.isValid() && acceptsSource.test(source)
						&& source.findCollision(localBox, queryToWorld, entity, skipTag,
						hit -> true) != null) return true;
			}
		}
		return false;
	}

	/** Match the structure's native entity box when querying a player-relative sensor. */
	@Nullable
	public static DynamicBlockHit findSensorCollision(Level level, Entity entity, AABB box,
			@Nullable TagKey<Block> skipTag) {
		return findSensorCollision(level, entity, box, skipTag, source -> true);
	}

	/** Filter sources before taking a hit, so an unrelated structure cannot hide a valid one. */
	@Nullable
	public static DynamicBlockHit findSensorCollision(Level level, Entity entity, AABB box,
			@Nullable TagKey<Block> skipTag, Predicate<DynamicCollisionSource> acceptsSource) {
		return findSensorCollision(level, entity, box, skipTag, acceptsSource, hit -> true);
	}

	@Nullable
	public static DynamicBlockHit findSensorCollision(Level level, Entity entity, AABB box,
			@Nullable TagKey<Block> skipTag, Predicate<DynamicCollisionSource> acceptsSource,
			Predicate<DynamicBlockHit> acceptsHit) {
		AABB searchBox = sensorSearchBounds(entity, box);
		for (DynamicCollisionProvider provider : Providers.ALL) {
			for (DynamicCollisionSource source : provider.sources(level, searchBox)) {
				if (!source.isValid() || !acceptsSource.test(source)) continue;
				DynamicBlockHit hit = source.findCollision(box, source.sensorTransform(entity), entity, skipTag, acceptsHit);
				if (hit != null) return hit;
			}
		}
		return null;
	}

	private static AABB sensorSearchBounds(Entity entity, AABB box) {
		// Rotating a sensor can move its corners beyond the original world bounds.
		double radius = box.getCenter().distanceTo(entity.position())
				+ Math.max(box.getXsize(), box.getZsize());
		return box.inflate(radius, 0, radius);
	}

	@Nullable
	public static DynamicBlockHit findCollision(Level level, @Nullable Entity entity, AABB box,
			@Nullable TagKey<Block> skipTag) {
		for (DynamicCollisionProvider provider : Providers.ALL) {
			for (DynamicCollisionSource source : provider.sources(level, box)) {
				if (!source.isValid()) continue;
				DynamicBlockHit hit = source.findCollision(box, entity, skipTag);
				if (hit != null) return hit;
			}
		}
		return null;
	}
}
