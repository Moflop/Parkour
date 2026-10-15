package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.ParkourConstants;
import mod.arcomit.parkour.compat.CollisionGeometry;
import mod.arcomit.parkour.compat.DynamicCollisionSource;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.core.sensor.impl.ArmhangEyeSensor;
import mod.arcomit.parkour.core.sensor.impl.ArmhangTopSensor;
import mod.arcomit.parkour.utils.BlockCollisions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Keeps the complete vanilla/Sable travel step from discarding a verified dynamic ledge grip. */
public final class ArmhangTravelGuard {
	private ArmhangTravelGuard() {}

	public record Snapshot(Level level, DynamicCollisionSource source, Vec3 localPosition,
			Vec3 localNormal, Direction direction, ArmhangMoveSnapshot movement) {}

	@Nullable
	public static Snapshot begin(Player player) {
		if (!player.isLocalPlayer() || !DynamicCollisions.isEnabled()
				|| !(ParkourContext.get(player).state().getState() instanceof ArmhangState)) return null;
		Direction direction = ParkourContext.get(player).wall().getArmhang();
		if (direction == null || !ArmhangCollision.hasValidHangPoint(player, direction)) return null;
		DynamicSurfaceMotion.capture(player);
		DynamicCollisionSource source = DynamicSurfaceMotion.attachedSource(player, direction);
		if (source == null || !source.isValid()) return null;
		Vec3 pivot = player.position(), local = source.toLocal(pivot);
		var transform = source.sensorTransform(player);
		Vec3 sensorFacing = transform.apply(pivot.add(Vec3.atLowerCornerOf(direction.getNormal())))
				.subtract(transform.apply(pivot));
		Vec3 localFacing = source.toLocal(pivot.add(sensorFacing)).subtract(local);
		Direction face = Direction.getNearest(localFacing.x, localFacing.y, localFacing.z);
		return new Snapshot(player.level(), source, local, Vec3.atLowerCornerOf(face.getNormal()),
				direction, ArmhangMoveSnapshot.capture(player));
	}

	public static void finish(Player player, @Nullable Snapshot before) {
		if (before == null || !player.isLocalPlayer() || player.level() != before.level()
				|| !before.source().isValid() || !player.isAlive() || player.isPassenger()
				|| !(ParkourContext.get(player).state().getState() instanceof ArmhangState)) return;
		WallMovementData wall = ParkourContext.get(player).wall();
		Direction current = wall.getArmhang();
		Direction direction = direction(player, before);
		if (current != before.direction() && current != direction) return;
		if (ArmhangCollision.hasValidHangPoint(player, direction)) {
			wall.setArmhang(direction);
			return;
		}
		Vec3 safePosition = before.source().toWorld(before.localPosition());
		if (!finite(safePosition)) return;
		ArmhangMoveSnapshot after = ArmhangMoveSnapshot.capture(player);
		boolean restored = false;
		try {
			// Test the old material point in the structure's current pose, never in its old world pose.
			player.setPos(safePosition);
			direction = direction(player, before);
			if (!hasSameSourceGrip(player, direction, before.source())
					|| !BlockCollisions.noSensorCollision(player,
						player.getBoundingBox().deflate(ParkourConstants.AABB_DEFLATE_EPSILON))) return;
			before.movement().restore(player, safePosition);
			player.setDeltaMovement(Vec3.ZERO);
			player.resetFallDistance();
			DynamicCollisions.clearSurfaceInertia(player);
			wall.setArmhang(direction);
			restored = true;
		} finally {
			if (!restored) after.restore(player);
		}
		DynamicSurfaceMotion.capture(player);
	}

	private static Direction direction(Player player, Snapshot snapshot) {
		DynamicCollisionSource source = snapshot.source();
		Vec3 normal = source.toWorld(snapshot.localPosition().add(snapshot.localNormal()))
				.subtract(source.toWorld(snapshot.localPosition()));
		return CollisionGeometry.sensorDirection(normal, player.position(), source.sensorTransform(player));
	}

	private static boolean hasSameSourceGrip(Player player, Direction direction, DynamicCollisionSource source) {
		AABB box;
		if (ArmhangEyeSensor.isValidCollision(player, direction)) {
			box = ArmhangEyeSensor.getBoxes(player, direction).getFirst();
		} else if (ArmhangTopSensor.isValidCollision(player, direction)) {
			box = ArmhangTopSensor.getBoxes(player, direction).getFirst();
		} else return false;
		return source.findCollision(box, source.sensorTransform(player), player,
				ParkourTags.Blocks.ARMHANG_IGNORED_BLOCKS) != null;
	}

	private static boolean finite(Vec3 vector) {
		return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
