package mod.arcomit.parkour.content.behavior.barswing.client;

import mod.arcomit.parkour.content.behavior.barswing.BarSwingData;
import mod.arcomit.parkour.content.block.HorizontalBarBlock;
import mod.arcomit.parkour.content.init.ParkourBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 只查询渲染握点可用的连续杆段，不改变实际支点或摆荡状态。 */
public record BarSwingGripRange(double min, double max) {
	public static BarSwingGripRange find(Player player, BarSwingData data) {
		if (data.bar == null || data.source != null && !data.source.isValid()) return new BarSwingGripRange(0, 0);
		Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, data.axis);
		int low = 0, high = 0;
		while (low > -2 && hasBar(player, data, data.bar.relative(direction, low - 1))) low--;
		while (high < 2 && hasBar(player, data, data.bar.relative(direction, high + 1))) high++;
		double localAnchor = data.axis == Direction.Axis.X ? data.localAnchor.x : data.localAnchor.z;
		double coordinate = data.axis == Direction.Axis.X ? data.bar.getX() : data.bar.getZ();
		// 返回杆身端点；首次抓握时独立选点，随后固定在杆上的对应位置。
		return new BarSwingGripRange(coordinate + low + 0.04 - localAnchor,
				coordinate + high + 0.96 - localAnchor);
	}

	private static boolean hasBar(Player player, BarSwingData data, BlockPos pos) {
		if (data.source == null) return matches(player.level().getBlockState(pos), data.axis);
		return data.source.findCollision(new AABB(pos), data.source::toWorld, player, null,
				hit -> hit.localPos().equals(pos) && matches(hit.state(), data.axis)) != null;
	}

	private static boolean matches(BlockState state, Direction.Axis axis) {
		return state.is(ParkourBlocks.HORIZONTAL_BAR.get()) && state.getValue(HorizontalBarBlock.AXIS) == axis
				&& !state.getValue(HorizontalBarBlock.WATERLOGGED);
	}
}
