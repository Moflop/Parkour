package mod.arcomit.parkour.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.function.Predicate;

/** Create 6 动态结构的方块碰撞查询。仅在加载 Create 后实例化。 */
public final class CreateCollisionProvider implements DynamicCollisionProvider {
	private static final boolean SABLE_LOADED = ModList.get().isLoaded("sable");

	@Nullable
	public static DynamicCollisionSource source(AbstractContraptionEntity entity) {
		Contraption contraption = entity.getContraption();
		return contraption == null ? null : new Source(entity, contraption);
	}

	@Override
	public List<DynamicCollisionSource> sources(Level level, AABB worldBounds) {
		List<DynamicCollisionSource> sources = new ArrayList<>();
		// 结构原点可能远离玩家；按原版实体分区查询会遗漏大型结构的远端。
		for (WeakReference<AbstractContraptionEntity> reference :
				ContraptionHandler.loadedContraptions.get(level).values()) {
			AbstractContraptionEntity entity = reference.get();
			if (entity == null || entity.level() != level || entity.getContraption() == null) {
				continue;
			}
			Source source = new Source(entity, entity.getContraption());
			if (source.isValid() && CollisionGeometry.transformBounds(
					source.contraption.bounds.inflate(1), source::toWorld).intersects(worldBounds)) {
				sources.add(source);
			}
		}
		return sources;
	}

	private record Source(AbstractContraptionEntity entity, Contraption contraption)
			implements DynamicCollisionSource {

		@Override
		public Object identity() {
			return entity;
		}

		@Override
		public boolean isValid() {
			return entity.isAlive() && entity.collisionEnabled()
					&& entity.getContraption() == contraption && !contraption.disassembled
					&& contraption.bounds != null;
		}

		@Override
		public boolean supportsServerPlayerCorrections() {
			// Create resolves local players on ClientTick.Post and skips ServerPlayer.
			return false;
		}

		@Override
		public Vec3 toLocal(Vec3 world) {
			Vec3 parentLocal = SABLE_LOADED ? SableBridge.toLocal(entity, world) : world;
			return entity.toLocalVector(parentLocal, 1.0F);
		}

		@Override
		public Vec3 toWorld(Vec3 local) {
			Vec3 parentLocal = entity.toGlobalVector(local, 1.0F);
			return SABLE_LOADED ? SableBridge.toWorld(entity, parentLocal) : parentLocal;
		}

		@Override
		public Vec3 toRenderWorld(Vec3 local, float partialTick) {
			Vec3 parentLocal = entity.toGlobalVector(local, partialTick);
			return SABLE_LOADED ? SableBridge.toRenderWorld(entity, parentLocal, partialTick) : parentLocal;
		}

		@Override
		public Vec3 pointMotion(Vec3 worldPoint) {
			Vec3 localPoint = toLocal(worldPoint);
			Vec3 current = entity.toGlobalVector(localPoint, 1.0F);
			Vec3 previous = entity.isPrevPosInvalid() ? current
					: entity.toGlobalVector(localPoint, 0.0F, true);
			if (SABLE_LOADED) {
				current = SableBridge.toWorld(entity, current);
				previous = SableBridge.toPreviousWorld(entity, previous);
			}
			return current.subtract(previous);
		}

		@Override
		public UnaryOperator<Vec3> sensorTransform(Entity collidingEntity) {
			// Create's OBB excludes this outer yaw from its basis on pitched vehicles.
			return CreateSensorGeometry.sensorTransform(collidingEntity.position(),
					entity.getRotationState().getYawOffset());
		}

		@Override
		public double wallSensorTolerance(Entity collidingEntity, Direction direction, double maximum) {
			Vec3 pivot = collidingEntity.position();
			UnaryOperator<Vec3> transform = sensorTransform(collidingEntity);
			Vec3 toward = transform.apply(pivot.add(Vec3.atLowerCornerOf(direction.getNormal())))
					.subtract(transform.apply(pivot));
			Vec3 local = toLocal(pivot);
			Vec3 localToward = toLocal(pivot.add(toward)).subtract(local);
			Direction face = Direction.getNearest(localToward.x, localToward.y, localToward.z);
			// A tilted local floor is not an extra-reach wall.
			if (face.getAxis() == Direction.Axis.Y) return 0;
			Vec3 normal = toWorld(local.add(Vec3.atLowerCornerOf(face.getNormal()))).subtract(toWorld(local));
			return CreateSensorGeometry.wallSensorTolerance(normal, collidingEntity.getBbHeight(), maximum);
		}

		@Override
		@Nullable
		public DynamicBlockHit findCollision(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
				@Nullable Entity collidingEntity, @Nullable TagKey<Block> skipTag, Predicate<DynamicBlockHit> acceptsHit) {
			return findCollision(queryBox, queryToWorld, collidingEntity, skipTag, false, acceptsHit);
		}

		@Override
		public boolean hasSupport(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
				@Nullable Entity collidingEntity, @Nullable TagKey<Block> skipTag) {
			return findCollision(queryBox, queryToWorld, collidingEntity, skipTag, true, hit -> true) != null;
		}

		@Nullable
		private DynamicBlockHit findCollision(AABB queryBox, UnaryOperator<Vec3> queryToWorld,
				@Nullable Entity collidingEntity, @Nullable TagKey<Block> skipTag, boolean supportOnly,
				Predicate<DynamicBlockHit> acceptsHit) {
			if (!isValid()) {
				return null;
			}
			AABB localBounds = CollisionGeometry.transformBounds(queryBox,
					point -> toLocal(queryToWorld.apply(point)));
			if (!Double.isFinite(localBounds.minX) || !Double.isFinite(localBounds.minY)
					|| !Double.isFinite(localBounds.minZ) || !Double.isFinite(localBounds.maxX)
					|| !Double.isFinite(localBounds.maxY) || !Double.isFinite(localBounds.maxZ)) {
				return null;
			}
			// 栅栏等碰撞形状可伸出方块单元，因此额外扫描一格边界。
			BlockPos min = BlockPos.containing(localBounds.minX - 1, localBounds.minY - 1,
					localBounds.minZ - 1);
			BlockPos max = BlockPos.containing(localBounds.maxX + 1, localBounds.maxY + 1,
					localBounds.maxZ + 1);
			Level blockLevel = contraption.getContraptionWorld();
			UnaryOperator<Vec3> entityToWorld = collidingEntity == null ? UnaryOperator.identity()
					: sensorTransform(collidingEntity);
			CollisionContext context = collidingEntity == null ? CollisionContext.empty()
					: new LocalCollisionContext(collidingEntity,
						CollisionGeometry.transformBounds(collidingEntity.getBoundingBox(),
							point -> toLocal(entityToWorld.apply(point))).minY);

			for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
				StructureBlockInfo info = contraption.getBlocks().get(pos);
				if (info == null || contraption.isHiddenInPortal(pos)) {
					continue;
				}
				BlockState state = info.state();
				if (skipTag != null && state.is(skipTag)) {
					continue;
				}
				VoxelShape shape = state.getCollisionShape(blockLevel, pos, context);
				for (AABB box : shape.toAabbs()) {
					AABB localBox = box.move(pos);
					if (localBounds.intersects(localBox) && (supportOnly
							? CollisionGeometry.hasSupport(queryBox, queryToWorld, localBox, this::toWorld)
							: CollisionGeometry.intersects(queryBox, queryToWorld, localBox, this::toWorld))) {
						DynamicBlockHit hit = new DynamicBlockHit(this, blockLevel, pos.immutable(), state, localBox);
						if (acceptsHit.test(hit)) return hit;
					}
				}
			}
			return null;
		}

		@Override
		public boolean isNativeCarrier(Player player) {
			return isCreateCarrier(player)
					|| (SABLE_LOADED && SableBridge.isNativeCarrier(entity, player));
		}

		@Override
		public Vec3 removeNativeMotion(Player player, Vec3 worldPoint, Vec3 totalMotion) {
			if (totalMotion.lengthSqr() < 1.0E-12 || isCreateCarrier(player)) {
				return Vec3.ZERO;
			}
			return SABLE_LOADED ? SableBridge.removeNativeMotion(entity, player, worldPoint, totalMotion)
					: totalMotion;
		}

		@Override
		public Vec3 jumpMotion(Player player, Vec3 worldPoint) {
			Vec3 motion = removeNativeMotion(player, worldPoint, pointMotion(worldPoint));
			return SABLE_LOADED ? SableBridge.removeInheritedMotion(player, motion) : motion;
		}

		@Override
		public float removeNativeYaw(Player player, float totalYaw, Vec3 currentWorldNormal) {
			if (Math.abs(totalYaw) < 1.0E-6F || isCreateCarrier(player)) {
				return 0;
			}
			return SABLE_LOADED ? SableBridge.removeNativeYaw(entity, player, totalYaw, currentWorldNormal)
					: totalYaw;
		}

		private boolean isCreateCarrier(Player player) {
			// Create 仅对顶面接触施加携带位移。其记录保留三 tick，不可只判断 containsKey。
			MutableInt collisionAge = entity.collidingEntities.get(player);
			return player.onGround() && collisionAge != null && collisionAge.intValue() <= 1;
		}
	}

	/** 单独隔离可选 Sable 引用；每次读取当前父结构，以处理实体移入和移出物理结构。 */
	private static final class SableBridge {
		private static Vec3 toRenderWorld(AbstractContraptionEntity entity, Vec3 parentLocal, float partialTick) {
			SubLevel parent = Sable.HELPER.getContaining(entity);
			if (parent instanceof ClientSubLevelAccess client) {
				return client.renderPose(partialTick).transformPosition(parentLocal);
			}
			return parent == null ? parentLocal : parent.logicalPose().transformPosition(parentLocal);
		}

		private static Vec3 toLocal(AbstractContraptionEntity entity, Vec3 world) {
			SubLevel parent = Sable.HELPER.getContaining(entity);
			return parent == null ? world : parent.logicalPose().transformPositionInverse(world);
		}

		private static Vec3 toWorld(AbstractContraptionEntity entity, Vec3 parentLocal) {
			SubLevel parent = Sable.HELPER.getContaining(entity);
			return parent == null ? parentLocal : parent.logicalPose().transformPosition(parentLocal);
		}

		private static Vec3 toPreviousWorld(AbstractContraptionEntity entity, Vec3 parentLocal) {
			SubLevel parent = Sable.HELPER.getContaining(entity);
			return parent == null ? parentLocal : parent.lastPose().transformPosition(parentLocal);
		}

		private static boolean isNativeCarrier(AbstractContraptionEntity entity, Player player) {
			SubLevel parent = Sable.HELPER.getContaining(entity);
			return parent != null && player instanceof EntityMovementExtension movement
					&& movement.sable$getTrackingSubLevel() == parent;
		}

		private static Vec3 removeNativeMotion(AbstractContraptionEntity entity, Player player,
				Vec3 worldPoint, Vec3 totalMotion) {
			SubLevel parent = Sable.HELPER.getContaining(entity);
			if (parent == null || !(player instanceof EntityMovementExtension movement)
					|| movement.sable$getTrackingSubLevel() != parent) {
				return totalMotion;
			}
			// Sable 携带的是父结构；仍须补上子轴承或活塞相对父结构产生的运动。
			Vec3 parentLocal = parent.logicalPose().transformPositionInverse(worldPoint);
			Vec3 parentMotion = worldPoint.subtract(parent.lastPose().transformPosition(parentLocal));
			return totalMotion.subtract(parentMotion);
		}

		private static Vec3 removeInheritedMotion(Player player, Vec3 motion) {
			if (!player.isLocalPlayer() || !(player instanceof EntityMovementExtension movement)
					|| movement.sable$getTrackingSubLevel() != null
					|| !(player instanceof LivingEntityMovementExtension livingMovement)) {
				return motion;
			}
			var inherited = livingMovement.sable$getInheritedVelocity();
			return motion.subtract(inherited.x, inherited.y, inherited.z);
		}

		private static float removeNativeYaw(AbstractContraptionEntity entity, Player player,
				float totalYaw, Vec3 currentWorldNormal) {
			SubLevel parent = Sable.HELPER.getContaining(entity);
			if (parent == null || !(player instanceof EntityMovementExtension movement)
					|| movement.sable$getTrackingSubLevel() != parent) {
				return totalYaw;
			}
			Vec3 parentNormal = parent.logicalPose().transformNormalInverse(currentWorldNormal);
			Vec3 previousWorldNormal = parent.lastPose().transformNormal(parentNormal);
			if (previousWorldNormal.horizontalDistanceSqr() < 1.0E-8
					|| currentWorldNormal.horizontalDistanceSqr() < 1.0E-8) {
				return totalYaw;
			}
			double parentAngle = Math.atan2(currentWorldNormal.z, currentWorldNormal.x)
					- Math.atan2(previousWorldNormal.z, previousWorldNormal.x);
			float parentYaw = Mth.wrapDegrees((float) Math.toDegrees(parentAngle));
			return Mth.wrapDegrees(totalYaw - parentYaw);
		}
	}

	/** 保留实体属性，但让脚底高度处于结构局部坐标系，供脚手架等上下文相关形状使用。 */
	private static final class LocalCollisionContext extends EntityCollisionContext {
		private final double localBottom;

		private LocalCollisionContext(Entity entity, double localBottom) {
			super(entity);
			this.localBottom = localBottom;
		}

		@Override
		public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
			return localBottom > pos.getY() + shape.max(Direction.Axis.Y) - 1.0E-5;
		}
	}
}
