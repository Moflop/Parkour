package mod.arcomit.parkour.compat;

import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.UnaryOperator;
import java.util.function.Predicate;
import java.util.UUID;

/** A live structure transform. Positions and collision queries use world coordinates. */
public interface DynamicCollisionSource {
	Object identity();
	boolean isValid();
	Vec3 toLocal(Vec3 world);
	Vec3 toWorld(Vec3 local);

	/** Stable reference for server-authored attachments shown by other clients. */
	default UUID networkId() { return null; }

	/** Visual pose for this frame; collision and movement keep using the logical pose. */
	default Vec3 toRenderWorld(Vec3 local, float partialTick) {
		return toWorld(local);
	}

	/** False when this engine relies on client collision movement and synced player positions. */
	default boolean supportsServerPlayerCorrections() {
		return true;
	}

	/** Displacement of this material point over one physics tick, in blocks per tick. */
	default Vec3 pointMotion(Vec3 worldPoint) {
		return Vec3.ZERO;
	}

	@Nullable
	default DynamicBlockHit findCollision(AABB worldBox, @Nullable Entity entity,
			@Nullable TagKey<Block> skipTag) {
		return findCollision(worldBox, UnaryOperator.identity(), entity, skipTag);
	}

	/** Rotates parkour sensors to match this engine's player hitbox; clearance queries stay in world space. */
	default UnaryOperator<Vec3> sensorTransform(Entity entity) {
		return UnaryOperator.identity();
	}

	/** Forward reach compensating for a wall's tilt; engines opt in independently. */
	default double wallSensorTolerance(Entity entity, Direction direction, double maximum) {
		return 0;
	}

	@Nullable
	default DynamicBlockHit findCollision(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
			@Nullable Entity entity, @Nullable TagKey<Block> skipTag) {
		return findCollision(queryBox, queryToWorld, entity, skipTag, hit -> true);
	}

	/** Continue scanning when a hit belongs to a different face of the same structure. */
	@Nullable
	DynamicBlockHit findCollision(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
			@Nullable Entity entity, @Nullable TagKey<Block> skipTag, Predicate<DynamicBlockHit> acceptsHit);

	/** Checks every candidate shape for an upward face intersecting the foot probe. */
	boolean hasSupport(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
			@Nullable Entity entity, @Nullable TagKey<Block> skipTag);

	/** True when the owning physics engine already moves this player with this structure. */
	default boolean isNativeCarrier(Player player) {
		return false;
	}

	default Vec3 removeNativeMotion(Player player, Vec3 worldPoint, Vec3 totalMotion) {
		return isNativeCarrier(player) ? Vec3.ZERO : totalMotion;
	}

	default Vec3 jumpMotion(Player player, Vec3 worldPoint) {
		return removeNativeMotion(player, worldPoint, pointMotion(worldPoint));
	}

	default float removeNativeYaw(Player player, float totalYaw, Vec3 currentWorldNormal) {
		return isNativeCarrier(player) ? 0 : totalYaw;
	}
}
