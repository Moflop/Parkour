package mod.arcomit.parkour.content.behavior.barswing;

import mod.arcomit.parkour.compat.DynamicCollisionSource;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 支点保存在结构局部空间，前后摆动使用朝向与世界重力组成的竖直平面。 */
public final class BarSwingAnchor {
	private BarSwingAnchor() {}

	public record Frame(Vec3 anchor, Vec3 axis, Vec3 forward, Vec3 down) {}

	public static Frame frame(Vec3 anchor, Vec3 axis, Vec3 forwardHint) {
		axis = axis.normalize();
		Vec3 down = new Vec3(0, -1, 0);
		Vec3 forward = down.cross(axis).normalize();
		if (forward.dot(forwardHint) < 0) forward = forward.scale(-1);
		return new Frame(anchor, axis, forward, down);
	}

	public static Frame frame(BarSwingData data, float partialTick, boolean render) {
		DynamicCollisionSource source = data.source;
		if (source == null || !source.isValid()) return new Frame(data.anchor, data.barDirection, data.forward, data.down);
		var transform = (java.util.function.UnaryOperator<Vec3>) point -> render
				? source.toRenderWorld(point, partialTick) : source.toWorld(point);
		Vec3 anchor = transform.apply(data.localAnchor);
		Vec3 axis = data.axis == net.minecraft.core.Direction.Axis.X ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
		return frame(anchor, transform.apply(data.localAnchor.add(axis)).subtract(anchor),
				transform.apply(data.localAnchor.add(data.localForward)).subtract(anchor));
	}

	public static boolean refresh(Player player) {
		var data = BarSwingData.get(player);
		if (data.source == null && data.sourceId != null) {
			for (var source : DynamicCollisions.sources(player.level(), new AABB(data.anchor, data.anchor).inflate(8))) {
				if (data.sourceId.equals(source.networkId())) { data.source = source; break; }
			}
			if (data.source == null) return false;
		}
		if (data.source == null) return true;
		if (!data.source.isValid()) return false;
		Frame frame = frame(data, 1, false);
		Vec3 displacement = frame.anchor.subtract(data.anchor);
		if (!Double.isFinite(displacement.lengthSqr()) || displacement.lengthSqr() > 16
				|| Math.abs(frame.axis.y) > 0.85) return false;
		if (player.isLocalPlayer()) {
			float yaw = Mth.wrapDegrees(Directions.horizontalYaw(frame.forward) - Directions.horizontalYaw(data.forward));
			yaw = data.source.removeNativeYaw(player, yaw, frame.forward);
			player.setYRot(player.getYRot() + yaw);
			player.yRotO += yaw;
		}
		data.anchorMotion = data.source.pointMotion(frame.anchor);
		data.anchor = frame.anchor;
		data.barDirection = frame.axis;
		data.forward = frame.forward;
		data.down = frame.down;
		return true;
	}
}
