package mod.arcomit.parkour.compat.sable;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import mod.arcomit.parkour.compat.CollisionGeometry;
import mod.arcomit.parkour.compat.DynamicBlockHit;
import mod.arcomit.parkour.compat.DynamicCollisionProvider;
import mod.arcomit.parkour.compat.DynamicCollisionSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.function.Predicate;

/** Parkour sensor access to Sable physics structures, including Aeronautics. */
public final class SableCollisionProvider implements DynamicCollisionProvider {

	@Override
	public List<DynamicCollisionSource> sources(Level level, AABB worldBox) {
		SubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container == null) {
			return List.of();
		}
		List<DynamicCollisionSource> sources = new ArrayList<>();
		// Include shapes extending past their owning block, for example fences.
		for (SubLevel subLevel : container.queryIntersecting(new BoundingBox3d(worldBox.inflate(1.0)))) {
			if (!subLevel.isRemoved()) {
				sources.add(new Source(subLevel));
			}
		}
		return sources;
	}

	private record Source(SubLevel subLevel) implements DynamicCollisionSource {
		@Override
		public Object identity() {
			return subLevel.getUniqueId();
		}

		@Override
		public boolean isValid() {
			return !subLevel.isRemoved();
		}

		@Override
		public boolean supportsServerPlayerCorrections() {
			// Sable's ServerPlayer collider returns motion without resolving plot blocks.
			return false;
		}

		@Override
		public Vec3 toLocal(Vec3 world) {
			return subLevel.logicalPose().transformPositionInverse(world);
		}

		@Override
		public Vec3 toWorld(Vec3 local) {
			return subLevel.logicalPose().transformPosition(local);
		}

		@Override
		public Vec3 toRenderWorld(Vec3 local, float partialTick) {
			return subLevel instanceof ClientSubLevelAccess client
					? client.renderPose(partialTick).transformPosition(local) : toWorld(local);
		}

		@Override
		public UnaryOperator<Vec3> sensorTransform(Entity entity) {
			Vec3 pivot = entity.position();
			double yaw = SubLevelEntityCollision.getHitBoxYaw(subLevel.logicalPose());
			double cosine = Math.cos(yaw);
			double sine = Math.sin(yaw);
			// Match JOML's positive yaw, retaining double precision around the player's feet.
			return point -> {
				double dx = point.x - pivot.x;
				double dz = point.z - pivot.z;
				return new Vec3(pivot.x + cosine * dx + sine * dz,
						point.y, pivot.z + cosine * dz - sine * dx);
			};
		}

		@Override
		public Vec3 pointMotion(Vec3 worldPoint) {
			Vec3 local = toLocal(worldPoint);
			return toWorld(local).subtract(subLevel.lastPose().transformPosition(local));
		}

		@Override
		public double wallSensorTolerance(Entity entity, Direction direction, double maximum) {
			Vec3 pivot = entity.position();
			var transform = sensorTransform(entity);
			Vec3 toward = transform.apply(pivot.add(Vec3.atLowerCornerOf(direction.getNormal())))
					.subtract(transform.apply(pivot));
			Vec3 local = toLocal(pivot);
			Vec3 localToward = toLocal(pivot.add(toward)).subtract(local);
			Direction face = Direction.getNearest(localToward.x, localToward.y, localToward.z);
			// Only local block sides are walls. Tilting a floor cannot give it extra reach.
			if (face.getAxis() == Direction.Axis.Y) return 0;
			Vec3 normal = toWorld(local.add(Vec3.atLowerCornerOf(face.getNormal()))).subtract(toWorld(local));
			double horizontal = normal.horizontalDistance();
			if (horizontal < 1.0E-8 || Math.abs(normal.y) < 1.0E-8) return 0;
			// A standing body stays upright while the wall recedes with height. Cover
			// that horizontal separation, bounded so a distant wall is never grabbed.
			double reach = entity.getBbHeight() * Math.abs(normal.y) / horizontal;
			return Double.isFinite(reach) ? Math.min(maximum, reach) : 0;
		}

		@Override
		public Vec3 jumpMotion(Player player, Vec3 worldPoint) {
			Vec3 additionalMotion = removeNativeMotion(player, worldPoint, pointMotion(worldPoint));
			// Sable adds this separate inertia to client movement only while untracked.
			// Account for it without changing the vector owned by Sable's travel hook.
			if (player.isLocalPlayer() && player instanceof EntityMovementExtension movement
					&& movement.sable$getTrackingSubLevel() == null
					&& player instanceof LivingEntityMovementExtension livingMovement) {
				Vector3d inherited = livingMovement.sable$getInheritedVelocity();
				return additionalMotion.subtract(inherited.x, inherited.y, inherited.z);
			}
			return additionalMotion;
		}

		@Override
		public boolean isNativeCarrier(Player player) {
			return player instanceof EntityMovementExtension movement
					&& movement.sable$getTrackingSubLevel() == subLevel;
		}

		@Override
		@Nullable
		public DynamicBlockHit findCollision(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
				@Nullable Entity entity, @Nullable TagKey<Block> skipTag, Predicate<DynamicBlockHit> acceptsHit) {
			return findCollision(queryBox, queryToWorld, entity, skipTag, false, acceptsHit);
		}

		@Override
		public boolean hasSupport(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
				@Nullable Entity entity, @Nullable TagKey<Block> skipTag) {
			return findCollision(queryBox, queryToWorld, entity, skipTag, true, hit -> true) != null;
		}

		@Nullable
		private DynamicBlockHit findCollision(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
				@Nullable Entity entity, @Nullable TagKey<Block> skipTag, boolean supportOnly,
				Predicate<DynamicBlockHit> acceptsHit) {
			BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
			if (!isValid() || plotBounds == null) {
				return null;
			}
			AABB localBounds = CollisionGeometry.transformBounds(queryBox,
					point -> toLocal(queryToWorld.apply(point)));
			if (!Double.isFinite(localBounds.minX) || !Double.isFinite(localBounds.minY)
					|| !Double.isFinite(localBounds.minZ) || !Double.isFinite(localBounds.maxX)
					|| !Double.isFinite(localBounds.maxY) || !Double.isFinite(localBounds.maxZ)) {
				return null;
			}
			int minX = Math.max(plotBounds.minX(), Mth.floor(localBounds.minX) - 1);
			int minY = Math.max(plotBounds.minY(), Mth.floor(localBounds.minY) - 1);
			int minZ = Math.max(plotBounds.minZ(), Mth.floor(localBounds.minZ) - 1);
			int maxX = Math.min(plotBounds.maxX(), Mth.floor(localBounds.maxX) + 1);
			int maxY = Math.min(plotBounds.maxY(), Mth.floor(localBounds.maxY) + 1);
			int maxZ = Math.min(plotBounds.maxZ(), Mth.floor(localBounds.maxZ) + 1);
			if (minX > maxX || minY > maxY || minZ > maxZ) {
				return null;
			}
			Level level = subLevel.getLevel();
			CollisionContext context = entity == null ? CollisionContext.empty()
					: new LocalCollisionContext(entity, toLocal(entity.position()).y);
			LevelChunk cachedChunk = null;
			int cachedChunkX = Integer.MIN_VALUE;
			int cachedChunkZ = Integer.MIN_VALUE;
			for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				int chunkX = pos.getX() >> 4;
				int chunkZ = pos.getZ() >> 4;
				if (chunkX != cachedChunkX || chunkZ != cachedChunkZ) {
					cachedChunkX = chunkX;
					cachedChunkZ = chunkZ;
					cachedChunk = subLevel.getPlot().getChunk(
							subLevel.getPlot().toLocal(new ChunkPos(chunkX, chunkZ)));
				}
				if (cachedChunk == null) {
					continue;
				}
				BlockState state = cachedChunk.getBlockState(pos);
				if (state.isAir() || (skipTag != null && state.is(skipTag))) {
					continue;
				}
				// Pose-local coordinates are the actual, remote plot block positions.
				// EmbeddedPlotLevelAccessor expects center-relative coordinates instead.
				VoxelShape shape = state.getCollisionShape(level, pos, context);
				for (AABB box : shape.toAabbs()) {
					AABB localBox = box.move(pos);
					if (localBounds.intersects(localBox) && (supportOnly
							? CollisionGeometry.hasSupport(queryBox, queryToWorld, localBox, this::toWorld)
							: CollisionGeometry.intersects(queryBox, queryToWorld, localBox, this::toWorld))) {
						DynamicBlockHit hit = new DynamicBlockHit(this, level, pos.immutable(), state, localBox);
						if (acceptsHit.test(hit)) return hit;
					}
				}
			}
			return null;
		}
	}

	/** Preserve item, fluid, descent and entity context while using plot-space height. */
	private static final class LocalCollisionContext extends EntityCollisionContext {
		private final double localFeetY;

		private LocalCollisionContext(Entity entity, double localFeetY) {
			super(entity);
			this.localFeetY = localFeetY;
		}

		@Override
		public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
			return localFeetY > pos.getY() + shape.max(Direction.Axis.Y) - 1.0E-5;
		}
	}
}
