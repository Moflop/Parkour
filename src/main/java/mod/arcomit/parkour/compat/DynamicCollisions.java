package mod.arcomit.parkour.compat;

import mod.arcomit.parkour.compat.create.CreateCollisionProvider;
import mod.arcomit.parkour.compat.create.CreateCorrectionCollision;
import mod.arcomit.parkour.compat.create.CreateMovementCompat;
import mod.arcomit.parkour.compat.sable.SableCollisionProvider;
import mod.arcomit.parkour.compat.sable.SableClimbingCompat;
import mod.arcomit.parkour.compat.sable.SableMovementCompat;
import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.content.mechanic.entitysupport.EntitySupport;
import mod.arcomit.parkour.utils.FilteredBlockCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
		private static final boolean CREATE = ModList.get().isLoaded("create");
		private static final boolean SABLE = ModList.get().isLoaded("sable");
		private static final List<DynamicCollisionProvider> ALL = create();

		private static List<DynamicCollisionProvider> create() {
			List<DynamicCollisionProvider> providers = new ArrayList<>();
			if (CREATE) providers.add(new CreateCollisionProvider());
			if (SABLE) providers.add(new SableCollisionProvider());
			return List.copyOf(providers);
		}
	}

	public static boolean isEnabled() {
		return !Providers.ALL.isEmpty();
	}

	/** Live frames used by attachments such as swing bars; integrations remain optional. */
	public static List<DynamicCollisionSource> sources(Level level, AABB bounds) {
		List<DynamicCollisionSource> result = new ArrayList<>();
		for (DynamicCollisionProvider provider : Providers.ALL) {
			for (DynamicCollisionSource source : provider.sources(level, bounds)) {
				if (source.isValid()) result.add(source);
			}
		}
		return result;
	}

	/** Align server wall queries with Create's current pose, including packet-time queries. */
	public static void resolveServerContact(Player player) {
		if (Providers.CREATE && player instanceof ServerPlayer && CreateMovementCompat.isEnabled(player)) {
			CreateCorrectionCollision.resolveServerPenetration(player);
		}
	}

	/** Optional Sable lookup; ordinary ladders retain their native implementation. */
	@Nullable
	public static BlockPos findClimbablePole(LivingEntity entity) {
		return Providers.SABLE ? SableClimbingCompat.findClimbablePole(entity) : null;
	}

	/** Null means no confirmed dynamic ladder contact; zero means climb without a boost. */
	@Nullable
	public static Double findUpwardClimbBoostFactor(Player player) {
		return Providers.SABLE ? SableClimbingCompat.findUpwardClimbBoostFactor(player) : null;
	}

	/** Additional corrections still use the normal collision solver, without replaying inertia. */
	public static void move(Player player, MoverType type, Vec3 motion) {
		if (player instanceof ServerPlayer && !DynamicSurfaceMotion.supportsServerPlayerCorrections(player)) {
			return;
		}
		if (Providers.CREATE) motion = CreateCorrectionCollision.clip(player, motion);
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
		if (EntitySupport.findSupport(player) != null) return true;
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

	/** Support for a candidate body in the collision solver's own frame. */
	public static boolean hasTransformedSupport(Level level, Entity entity, AABB box,
			UnaryOperator<Vec3> toWorld) {
		AABB search = CollisionGeometry.transformBounds(box, toWorld);
		for (DynamicCollisionProvider provider : Providers.ALL) {
			for (DynamicCollisionSource source : provider.sources(level, search)) {
				if (source.isValid() && source.hasSupport(box, toWorld, entity, null)) return true;
			}
		}
		return false;
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

	/**
	 * Sensor lookup with a small forward-only tolerance for tilted dynamic surfaces.
	 * The caller must keep the ordinary block query separate so vanilla collision
	 * behavior remains exact; this overload only enumerates optional structures.
	 */
	@Nullable
	public static DynamicBlockHit findRelaxedSensorCollision(Level level, Entity entity, AABB box,
			Direction direction, double tolerance, @Nullable TagKey<Block> skipTag) {
		return findRelaxedSensorCollision(level, entity, box, direction, tolerance, skipTag,
				source -> true, hit -> true);
	}

	/**
	 * Preserve exact contacts before considering extra reach on an opted-in source.
	 */
	@Nullable
	public static DynamicBlockHit findRelaxedSensorCollision(Level level, Entity entity, AABB box,
			Direction direction, double tolerance, @Nullable TagKey<Block> skipTag,
			Predicate<DynamicCollisionSource> acceptsSource, Predicate<DynamicBlockHit> acceptsHit) {
		DynamicBlockHit exact = findSensorCollision(level, entity, box, skipTag, acceptsSource, acceptsHit);
		if (exact != null || direction == null || direction.getAxis() == Direction.Axis.Y || tolerance <= 0) return exact;
		AABB searchBox = sensorSearchBounds(entity, box.expandTowards(direction.getStepX() * tolerance,
				0, direction.getStepZ() * tolerance));
		for (DynamicCollisionProvider provider : Providers.ALL) {
			for (DynamicCollisionSource source : provider.sources(level, searchBox)) {
				if (!source.isValid() || !acceptsSource.test(source)) continue;
				double reach = source.wallSensorTolerance(entity, direction, tolerance);
				if (!Double.isFinite(reach) || reach <= 0) continue;
				reach = Math.min(tolerance, reach);
				AABB relaxed = box.expandTowards(direction.getStepX() * reach, 0, direction.getStepZ() * reach);
				DynamicBlockHit hit = source.findCollision(relaxed, source.sensorTransform(entity), entity, skipTag,
						candidate -> acceptsHit.test(candidate) && isReachableWallSide(entity, direction, candidate));
				if (hit != null) return hit;
			}
		}
		return null;
	}

	/** Extra reach cannot turn a sloping floor or ceiling into a wall at the player's feet. */
	private static boolean isReachableWallSide(Entity entity, Direction direction, DynamicBlockHit hit) {
		DynamicCollisionSource source = hit.source();
		var transform = source.sensorTransform(entity);
		Vec3 pivot = entity.position(), local = source.toLocal(pivot);
		Vec3 toward = transform.apply(pivot.add(Vec3.atLowerCornerOf(direction.getNormal())))
				.subtract(transform.apply(pivot));
		Vec3 localToward = source.toLocal(pivot.add(toward)).subtract(local);
		Direction face = Direction.getNearest(localToward.x, localToward.y, localToward.z);
		if (face.getAxis() == Direction.Axis.Y || CollisionGeometry.faceDistance(local, hit.localBox(), face) < 0) return false;
		AABB body = CollisionGeometry.transformBounds(entity.getBoundingBox(), point -> source.toLocal(transform.apply(point)));
		return body.minY < hit.localBox().maxY - ParkourConstants.AABB_DEFLATE_EPSILON
				&& body.maxY > hit.localBox().minY + ParkourConstants.AABB_DEFLATE_EPSILON;
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
