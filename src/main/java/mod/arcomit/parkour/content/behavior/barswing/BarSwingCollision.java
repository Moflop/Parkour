package mod.arcomit.parkour.content.behavior.barswing;

import mod.arcomit.parkour.compat.DynamicCollisionSource;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.content.block.HorizontalBarBlock;
import mod.arcomit.parkour.content.init.ParkourBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;

/** 在世界和可选移动结构中查询可达横杆，所有抓握条件都由服务端独立复核。 */
public final class BarSwingCollision {
	private BarSwingCollision() {}

	public record Grip(BlockPos pos, Direction.Axis axis, Vec3 anchor, Vec3 forward, Vec3 down,
			Vec3 barDirection, double angle, DynamicCollisionSource source, Vec3 localAnchor, Vec3 localForward) {}

	public static Grip findGrip(Player player) {
		Vec3 feet = player.position();
		AABB search = new AABB(feet.add(-1.4, 1.6, -1.4), feet.add(1.4, 2.8, 1.4));
		var grips = new ArrayList<Grip>();
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(search.minX, search.minY, search.minZ),
				BlockPos.containing(search.maxX, search.maxY, search.maxZ))) {
			Grip grip = candidate(player, pos, player.level().getBlockState(pos), null);
			if (grip != null) grips.add(grip);
		}
		for (var source : DynamicCollisions.sources(player.level(), search)) {
			var visited = new HashSet<BlockPos>();
			source.findCollision(search, point -> point, player, null, hit -> {
				if (visited.add(hit.localPos())) {
					Grip grip = candidate(player, hit.localPos(), hit.state(), source);
					if (grip != null) grips.add(grip);
				}
				return false;
			});
		}
		return grips.stream().min(java.util.Comparator.comparingDouble(g -> g.anchor.distanceToSqr(player.getEyePosition()))).orElse(null);
	}

	private static Grip candidate(Player player, BlockPos pos, BlockState state, DynamicCollisionSource source) {
		if (!state.is(ParkourBlocks.HORIZONTAL_BAR.get()) || state.getValue(HorizontalBarBlock.WATERLOGGED)) return null;
		Direction.Axis axis = state.getValue(HorizontalBarBlock.AXIS);
		Vec3 feet = player.position(), center = Vec3.atCenterOf(pos);
		Vec3 localAxis = axis == Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
		Vec3 worldCenter = source == null ? center : source.toWorld(center);
		Vec3 worldAxis = source == null ? localAxis : source.toWorld(center.add(localAxis)).subtract(worldCenter).normalize();
		if (Math.abs(worldAxis.y) > 0.85) return null;
		// 悬挂位置在握点正下方；不能把脚部的高度差投影成沿斜杆滑动。
		double alongAxis = feet.subtract(worldCenter).dot(worldAxis.multiply(1, 0, 1)) / worldAxis.horizontalDistanceSqr();
		Vec3 local = center.add(localAxis.scale(alongAxis));
		Vec3 anchor = axis == Direction.Axis.X
				? new Vec3(Mth.clamp(local.x, pos.getX() + 0.08, pos.getX() + 0.92), pos.getY() + 0.5, pos.getZ() + 0.5)
				: new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, Mth.clamp(local.z, pos.getZ() + 0.08, pos.getZ() + 0.92));
		Vec3 world = source == null ? anchor : source.toWorld(anchor);
		var frame = BarSwingAnchor.frame(world, worldAxis, player.getLookAngle());
		if (player.getLookAngle().dot(frame.forward()) < 0.35) return null;
		Vec3 relative = feet.subtract(world);
		double along = relative.dot(frame.forward()), height = relative.dot(frame.down());
		if (height < 1.7 || height > 2.65 || Math.abs(along) > 1 || Math.abs(relative.dot(worldAxis.multiply(1, 0, 1).normalize())) > 0.4) return null;
		double angle = Math.atan2(along, height);
		Vec3 target = BarSwingMath.position(world, frame.forward(), frame.down(), angle);
		if (target.distanceToSqr(feet) > 0.45 * 0.45
				|| !clear(player, BarSwingState.HANGING_SIZE.scale(player.getScale()).makeBoundingBox(target))) return null;
		var hit = player.level().clip(new ClipContext(player.getEyePosition(), world,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		// Sable 的原版射线入口也会命中远端 plot 坐标中的横杆本身。
		if (hit.getType() != HitResult.Type.MISS && !hit.getBlockPos().equals(pos)) return null;
		Vec3 eye = player.getEyePosition();
		AABB rayBounds = new AABB(eye, world).inflate(0.001);
		for (var obstacle : DynamicCollisions.sources(player.level(), rayBounds)) {
			if (obstacle.findCollision(rayBounds, point -> point, player, null, shape -> {
				if (source != null && obstacle.identity().equals(source.identity()) && shape.localPos().equals(pos)) return false;
				return shape.localBox().clip(obstacle.toLocal(eye), obstacle.toLocal(world)).isPresent();
			}) != null) return null;
		}
		Vec3 localForward = source == null ? frame.forward() : source.toLocal(world.add(frame.forward())).subtract(anchor).normalize();
		return new Grip(pos.immutable(), axis, world, frame.forward(), frame.down(), worldAxis, angle, source, anchor, localForward);
	}

	public static boolean clear(Player player, AABB box) {
		return clear(player, box, Vec3.ZERO);
	}

	/** 圆弧采样的扫掠方向保持世界坐标，身体截面匹配结构引擎实际使用的朝向。 */
	public static boolean clearTravel(Player player, Vec3 from, Vec3 to) {
		return clear(player, player.getBoundingBox().move(from.subtract(player.position())), to.subtract(from));
	}

	private static boolean clear(Player player, AABB box, Vec3 travel) {
		AABB swept = box.expandTowards(travel).deflate(1.0E-5);
		if (!player.level().noCollision(player, swept)) return false;
		Vec3 shift = box.getCenter().subtract(player.getBoundingBox().getCenter());
		double padding = Math.max(box.getXsize(), box.getZsize()) * 0.5;
		for (var source : DynamicCollisions.sources(player.level(), swept.inflate(padding, 0, padding))) {
			var rotation = source.sensorTransform(player);
			Vec3 pivot = player.position();
			Vec3 origin = rotation.apply(pivot);
			Vec3 x = rotation.apply(pivot.add(1, 0, 0)).subtract(origin);
			Vec3 z = rotation.apply(pivot.add(0, 0, 1)).subtract(origin);
			Vec3 localTravel = new Vec3(travel.dot(x), travel.y, travel.dot(z));
			if (source.findCollision(box.expandTowards(localTravel).deflate(1.0E-5),
					point -> rotation.apply(point.subtract(shift)).add(shift), player, null, hit -> true) != null) return false;
		}
		return true;
	}

	/** 仅为真实横杆截断的跃出保留一次动量；其他障碍物仍使用正常碰撞。 */
	public static boolean blocksRelease(Player player, BarSwingData data, Vec3 movement) {
		AABB path = player.getBoundingBox().expandTowards(movement.scale(4));
		if (data.source != null) return data.source.findCollision(path, point -> point, player, null,
				hit -> validBar(hit.state(), data.axis)) != null;
		var shape = player.level().getBlockState(data.bar).getCollisionShape(player.level(), data.bar);
		return shape.toAabbs().stream().anyMatch(box -> box.move(data.bar).intersects(path));
	}

	public static boolean hasBar(Player player, BarSwingData data) {
		if (data.bar == null || data.dimension != player.level().dimension()) return false;
		if (data.source != null) {
			return data.source.isValid() && data.source.findCollision(new AABB(data.bar), data.source::toWorld,
					player, null, hit -> hit.localPos().equals(data.bar) && validBar(hit.state(), data.axis)) != null;
		}
		return data.sourceId == null && player.level().hasChunkAt(data.bar) && validBar(player.level().getBlockState(data.bar), data.axis);
	}

	private static boolean validBar(BlockState state, Direction.Axis axis) {
		return state.is(ParkourBlocks.HORIZONTAL_BAR.get()) && state.getValue(HorizontalBarBlock.AXIS) == axis
				&& !state.getValue(HorizontalBarBlock.WATERLOGGED);
	}
}
