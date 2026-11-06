package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.compat.DynamicCollisionSource;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.compat.CollisionGeometry;
import mod.arcomit.parkour.utils.BlockCollisions;
import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.core.sensor.impl.ArmhangEyeSensor;
import mod.arcomit.parkour.core.sensor.impl.ArmhangTopSensor;
import mod.arcomit.parkour.utils.FilteredBlockCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;

/** Resolves the exposed top edge of the actual collision shape, including slabs and moving blocks. */
public final class ArmhangLedge {
	private ArmhangLedge() {}

	public static ArmhangGrip find(Player player, Direction direction) {
		if (direction == null || !direction.getAxis().isHorizontal()) return null;
		AABB probe;
		if (ArmhangEyeSensor.isValidCollision(player, direction)) probe = ArmhangEyeSensor.getBoxes(player, direction).getFirst();
		else if (ArmhangTopSensor.isValidCollision(player, direction)) probe = ArmhangTopSensor.getBoxes(player, direction).getFirst();
		else return null;
		var candidates = new ArrayList<ArmhangGrip>();
		var blocks = new FilteredBlockCollisions<>(player.level(), player, probe, false,
				ParkourTags.Blocks.ARMHANG_IGNORED_BLOCKS, (pos, shape) -> shape);
		while (blocks.hasNext()) {
			for (AABB box : blocks.next().toAabbs()) {
				if (!box.intersects(probe)) continue;
				ArmhangGrip grip = candidate(player, direction, box, null);
				if (grip != null) candidates.add(grip);
			}
		}
		for (var source : DynamicCollisions.sources(player.level(), probe.inflate(0.5))) {
			source.findCollision(probe, source.sensorTransform(player), player,
					ParkourTags.Blocks.ARMHANG_IGNORED_BLOCKS, hit -> {
				ArmhangGrip grip = candidate(player, direction, hit.localBox(), source);
				if (grip != null) candidates.add(grip);
				return false;
			});
		}
		return candidates.stream().min(Comparator.comparingDouble(g -> g.logical(g.localCenter()).distanceToSqr(player.getEyePosition()))).orElse(null);
	}

	private static ArmhangGrip candidate(Player player, Direction direction, AABB box, DynamicCollisionSource source) {
		Vec3 feet = player.position();
		Vec3 local = source == null ? feet : source.toLocal(feet);
		Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
		if (source != null) {
			var sensor = source.sensorTransform(player);
			Vec3 toward = sensor.apply(feet.add(normal)).subtract(sensor.apply(feet));
			Vec3 localToward = source.toLocal(feet.add(toward)).subtract(local);
			Direction face = Direction.getNearest(localToward.x, localToward.y, localToward.z);
			if (!face.getAxis().isHorizontal()) return null;
			normal = Vec3.atLowerCornerOf(face.getNormal());
		}
		Vec3 tangent = new Vec3(normal.z, 0, -normal.x);
		Direction face = Direction.getNearest(normal.x, normal.y, normal.z);
		if (CollisionGeometry.faceDistance(local, box, face) < -0.003) return null;
		Vec3 center = contact(box, local, normal);
		Vec3 left = contact(box, local.add(tangent.scale(0.19)), normal);
		Vec3 right = contact(box, local.add(tangent.scale(-0.19)), normal);
		Vec3 world = source == null ? center : source.toWorld(center);
		if (world.y < feet.y + player.getBbHeight() * 0.55 || world.y > feet.y + player.getBbHeight() + 0.2) return null;
		Vec3 worldNormal = source == null ? normal : source.toWorld(center.add(normal)).subtract(world).normalize();
		if (Math.abs(worldNormal.y) > 0.65) return null;
		// A collision box's buried internal top is not a ledge, even in a multi-box shape.
		for (Vec3 palm : new Vec3[]{left, right}) {
			AABB clearance = new AABB(palm.add(-0.008, 0.004, -0.008), palm.add(0.008, 0.04, 0.008));
			if (source == null ? BlockCollisions.hasBlockCollision(player, clearance, null)
					: source.findCollision(clearance, source::toWorld, player, null) != null) return null;
		}
		return new ArmhangGrip(BlockPos.containing(box.getCenter()), source, center, normal, left, right);
	}

	private static Vec3 contact(AABB box, Vec3 desired, Vec3 normal) {
		double insetX = Math.min(0.025, box.getXsize() * 0.2), insetZ = Math.min(0.025, box.getZsize() * 0.2);
		double x = Mth.clamp(desired.x, box.minX + insetX, box.maxX - insetX);
		double z = Mth.clamp(desired.z, box.minZ + insetZ, box.maxZ - insetZ);
		if (normal.x > 0) x = box.minX + insetX;
		if (normal.x < 0) x = box.maxX - insetX;
		if (normal.z > 0) z = box.minZ + insetZ;
		if (normal.z < 0) z = box.maxZ - insetZ;
		return new Vec3(x, box.maxY + 0.012, z);
	}
}
