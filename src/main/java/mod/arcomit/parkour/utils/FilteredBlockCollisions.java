package mod.arcomit.parkour.utils;

import com.google.common.collect.AbstractIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * 过滤了部分方块的碰撞检测迭代器。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class FilteredBlockCollisions<T> extends AbstractIterator<T> {
	private static final double EPSILON = 1.0E-7;
	private static final int BLOCK_MARGIN = 1;
	private static final double BLOCK_SIZE = 1.0;

	private final CollisionGetter getter;
	private final CollisionContext context;
	private final AABB bounds;
	private final VoxelShape boundsShape;
	private final boolean filterSuffocating;
	private final TagKey<Block> skipTag;
	private final BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> mapper;
	private final Cursor3D cursor;
	private final BlockPos.MutableBlockPos mutablePos;

	@Nullable
	private BlockGetter cachedChunk;
	private long cachedChunkPos;

	/**
	 * 构造方块碰撞迭代器，按指定包围盒遍历所有可能碰撞的方块，并跳过标记标签。
	 *
	 * @param collisionGetter   用于获取区块碰撞数据的关卡访问器，不可为null
	 * @param entity            碰撞上下文关联的实体，可为null（此时使用空上下文）
	 * @param bounds            检测范围包围盒，不可为null；内部会在各方向扩展 1 格边界
	 * @param filterSuffocating true 时只保留窒息方块（如完整固体方块），false 则不过滤
	 * @param skipTag           需排除的方块标签，标记的方块直接跳过
	 * @param mapper            将方块坐标与碰撞形状映射为结果类型 T 的函数
	 */
	public FilteredBlockCollisions(CollisionGetter collisionGetter, @Nullable Entity entity,
			AABB bounds, boolean filterSuffocating, TagKey<Block> skipTag,
			BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> mapper) {
		this.getter = collisionGetter;
		this.context = entity == null ?
				CollisionContext.empty() :
				CollisionContext.of(entity);
		this.bounds = bounds;
		this.boundsShape = Shapes.create(bounds);
		this.filterSuffocating = filterSuffocating;
		this.skipTag = skipTag;
		this.mapper = mapper;
		this.mutablePos = new BlockPos.MutableBlockPos();

		int minX = Mth.floor(bounds.minX - EPSILON) - BLOCK_MARGIN;
		int maxX = Mth.floor(bounds.maxX + EPSILON) + BLOCK_MARGIN;
		int minY = Mth.floor(bounds.minY - EPSILON) - BLOCK_MARGIN;
		int maxY = Mth.floor(bounds.maxY + EPSILON) + BLOCK_MARGIN;
		int minZ = Mth.floor(bounds.minZ - EPSILON) - BLOCK_MARGIN;
		int maxZ = Mth.floor(bounds.maxZ + EPSILON) + BLOCK_MARGIN;
		this.cursor = new Cursor3D(minX, minY, minZ, maxX, maxY, maxZ);

	}

	/**
	 * 按区块坐标获取区块数据，命中缓存则直接返回。
	 *
	 * @return 区块数据，若对应区块未加载则返回 null
	 */
	@Nullable
	private BlockGetter getChunk(int x, int z) {
		int sectionX = SectionPos.blockToSectionCoord(x);
		int sectionZ = SectionPos.blockToSectionCoord(z);
		long key = ChunkPos.pack(sectionX, sectionZ);
		if (this.cachedChunk != null && this.cachedChunkPos == key) {
			return this.cachedChunk;
		} else {
			BlockGetter chunk = this.getter.getChunkForCollisions(sectionX, sectionZ);
			this.cachedChunk = chunk;
			this.cachedChunkPos = key;
			return chunk;
		}
	}

	/**
	 * 迭代查找下一个与包围盒碰撞且未被过滤的方块，通过 mapper 转换为结果类型。
	 * <p>
	 * 过滤规则：跳过 skipTag 方块、非窒息方块（当开启过滤时）、无大型碰撞形状的方块 以及非移动中的活塞方块。完整方块直接检查包围盒相交，非完整方块用 VoxelShape
	 * 做布尔与运算。
	 *
	 * @return mapper 映射后的结果，遍历完毕时返回 {@link #endOfData()}
	 */
	@Override
	protected T computeNext() {
		while (this.cursor.advance()) {
			int x = this.cursor.nextX();
			int y = this.cursor.nextY();
			int z = this.cursor.nextZ();
			int type = this.cursor.getNextType();
			if (type == Cursor3D.TYPE_CORNER) {
				continue;
			}
			BlockGetter chunk = this.getChunk(x, z);
			if (chunk == null) {
				continue;
			}
			this.mutablePos.set(x, y, z);
			BlockState state = chunk.getBlockState(this.mutablePos);

			if (state.is(this.skipTag)) {
				continue;
			}

			boolean suffocating = !this.filterSuffocating || state.isSuffocating(chunk,
					this.mutablePos);
			if (!suffocating) {
				continue;
			}
			boolean hasLargeShape =
					type != Cursor3D.TYPE_FACE || state.hasLargeCollisionShape();
			if (!hasLargeShape) {
				continue;
			}
			boolean isPiston = type != Cursor3D.TYPE_EDGE || state.is(
					Blocks.MOVING_PISTON);
			if (!isPiston) {
				continue;
			}

			VoxelShape shape = state.getCollisionShape(this.getter, this.mutablePos,
					this.context);
			if (shape == Shapes.block()) {
				if (this.bounds.intersects(x, y, z, x + BLOCK_SIZE, y + BLOCK_SIZE,
						z + BLOCK_SIZE)) {
					return this.mapper.apply(this.mutablePos,
							shape.move(x, y, z));
				}
			} else {
				VoxelShape moved = shape.move(x, y, z);
				if (!moved.isEmpty() && Shapes.joinIsNotEmpty(moved,
						this.boundsShape, BooleanOp.AND)) {
					return this.mapper.apply(this.mutablePos, moved);
				}
			}
		}
		return this.endOfData();
	}
}
