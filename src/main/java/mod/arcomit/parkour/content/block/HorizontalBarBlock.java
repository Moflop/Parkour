package mod.arcomit.parkour.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 可横向拼接的跑酷横杆，放置时与玩家朝向垂直。 */
public class HorizontalBarBlock extends Block implements SimpleWaterloggedBlock {
	public static final MapCodec<HorizontalBarBlock> CODEC = simpleCodec(HorizontalBarBlock::new);
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<Connection> NEGATIVE = EnumProperty.create("negative", Connection.class);
	public static final EnumProperty<Connection> POSITIVE = EnumProperty.create("positive", Connection.class);
	private static final java.util.Map<BlockState, VoxelShape> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();

	public enum Connection implements StringRepresentable {
		OPEN(0), BAR(0), FULL(0), FENCE(6), WALL(4), WALL_SIDE(5), PANE(7);
		public final int extension;
		Connection(int extension) { this.extension = extension; }
		@Override
		public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
	}

	public HorizontalBarBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(WATERLOGGED, false)
				.setValue(NEGATIVE, Connection.OPEN).setValue(POSITIVE, Connection.OPEN));
	}

	@Override
	public MapCodec<HorizontalBarBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, WATERLOGGED, NEGATIVE, POSITIVE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction.Axis axis = face.getAxis().isHorizontal() ? face.getAxis()
				: context.getHorizontalDirection().getClockWise().getAxis();
		BlockState state = defaultBlockState().setValue(AXIS, axis).setValue(WATERLOGGED,
				context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
		for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
			Direction direction = Direction.fromAxisAndDirection(axis, sign);
			state = state.setValue(sign == Direction.AxisDirection.POSITIVE ? POSITIVE : NEGATIVE,
					connection(context.getLevel(), context.getClickedPos(), direction));
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.computeIfAbsent(state, HorizontalBarBlock::shape);
	}

	private static VoxelShape shape(BlockState state) {
		boolean x = state.getValue(AXIS) == Direction.Axis.X;
		VoxelShape result = x ? box(2, 7, 7, 14, 9, 9) : box(7, 7, 2, 9, 9, 14);
		for (boolean positive : new boolean[]{false, true}) {
			Connection end = state.getValue(positive ? POSITIVE : NEGATIVE);
			double min = positive ? 14 : -end.extension, max = positive ? 16 + end.extension : 2;
			result = Shapes.or(result, x ? box(min, 7, 7, max, 9, 9) : box(7, 7, min, 9, 9, max));
			if (end != Connection.BAR) {
				double collarMin = positive ? max - 2 : min, collarMax = collarMin + 2;
				result = Shapes.or(result, x ? box(collarMin, 6, 6, collarMax, 10, 10)
						: box(6, 6, collarMin, 10, 10, collarMax));
			}
		}
		return result.optimize();
	}

	private static Connection connection(BlockGetter level, BlockPos pos, Direction direction) {
		BlockPos neighborPos = pos.relative(direction);
		BlockState neighbor = level.getBlockState(neighborPos);
		if (neighbor.getBlock() instanceof HorizontalBarBlock && neighbor.getValue(AXIS) == direction.getAxis()) return Connection.BAR;
		if (neighbor.is(BlockTags.FENCES) || neighbor.is(BlockTags.WALLS) || neighbor.getBlock() instanceof IronBarsBlock) {
			double gap = 1;
			Direction.Axis across = direction.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
			for (var box : neighbor.getShape(level, neighborPos).toAabbs()) {
				if (box.minY > 0.5 || box.maxY < 0.5 || box.min(across) > 0.5 || box.max(across) < 0.5) continue;
				gap = Math.min(gap, direction.getAxisDirection() == Direction.AxisDirection.POSITIVE
						? box.min(direction.getAxis()) : 1 - box.max(direction.getAxis()));
			}
			if (gap >= 1) return Connection.OPEN;
			int pixels = (int) Math.round(gap * 16);
			return switch (pixels) {
				case 0 -> Connection.FULL;
				case 4 -> Connection.WALL;
				case 5 -> Connection.WALL_SIDE;
				case 6 -> Connection.FENCE;
				default -> Connection.PANE;
			};
		}
		return neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite()) ? Connection.FULL : Connection.OPEN;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		Direction positive = Direction.fromAxisAndDirection(state.getValue(AXIS), Direction.AxisDirection.POSITIVE);
		Direction rotated = rotation.rotate(positive);
		BlockState result = state.setValue(AXIS, rotated.getAxis());
		return rotated.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? swapEnds(result) : result;
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
		if (adjacent.getBlock() instanceof HorizontalBarBlock && side.getAxis() == state.getValue(AXIS)
				&& adjacent.getValue(AXIS) == state.getValue(AXIS)) return true;
		return super.skipRendering(state, adjacent, side);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		Direction positive = Direction.fromAxisAndDirection(state.getValue(AXIS), Direction.AxisDirection.POSITIVE);
		return mirror.mirror(positive) == positive ? state : swapEnds(state);
	}

	private static BlockState swapEnds(BlockState state) {
		return state.setValue(NEGATIVE, state.getValue(POSITIVE)).setValue(POSITIVE, state.getValue(NEGATIVE));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (direction.getAxis() == state.getValue(AXIS)) {
			state = state.setValue(direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? POSITIVE : NEGATIVE,
					connection(level, pos, direction));
		}
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}
}
