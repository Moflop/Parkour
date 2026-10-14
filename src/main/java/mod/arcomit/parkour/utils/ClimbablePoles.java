package mod.arcomit.parkour.utils;

import mod.arcomit.parkour.content.init.ParkourTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The same tag, orientation and stacking rules in world and structure coordinates. */
public final class ClimbablePoles {

	private ClimbablePoles() {}

	public static boolean isClimbable(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(ParkourTags.Blocks.CLIMBABLE) && isVertical(state)
				&& (isStacked(level, pos.above(), state.getBlock())
				|| isStacked(level, pos.below(), state.getBlock()));
	}

	private static boolean isStacked(Level level, BlockPos pos, Block block) {
		if (!level.isLoaded(pos)) return false;
		BlockState neighbor = level.getBlockState(pos);
		return neighbor.is(block) && isVertical(neighbor);
	}

	private static boolean isVertical(BlockState state) {
		if (state.getBlock() instanceof RotatedPillarBlock) {
			return state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y;
		}
		if (state.getBlock() instanceof EndRodBlock) {
			return state.getValue(EndRodBlock.FACING).getAxis() == Direction.Axis.Y;
		}
		return true;
	}
}
