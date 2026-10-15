package mod.arcomit.parkour.content.mechanic.entitysupport;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

/** 实体顶部的单向支撑几何；侧面和底部仍保持原版交互。 */
public final class EntitySupportGeometry {

	public static final double EPSILON = 1.0E-7;
	public static final double CONTACT_TOLERANCE = 0.025;

	private EntitySupportGeometry() {}

	public static boolean isAbove(AABB body, AABB surface) {
		return body.minY >= surface.maxY - EPSILON;
	}

	public static boolean isSupported(AABB body, AABB surface) {
		return Math.abs(body.minY - surface.maxY) <= CONTACT_TOLERANCE
				&& body.maxX > surface.minX + EPSILON && body.minX < surface.maxX - EPSILON
				&& body.maxZ > surface.minZ + EPSILON && body.minZ < surface.maxZ - EPSILON;
	}

	/** 客户端可显示两个服务端 tick 之间的高度；只接受真实相邻位置之间的插值。 */
	@Nullable
	public static AABB interpolateSupport(AABB body, AABB before, AABB after) {
		double deltaY = after.maxY - before.maxY;
		if (Math.abs(deltaY) < EPSILON) return null;
		double fraction = (body.minY - before.maxY) / deltaY;
		if (fraction < 0 || fraction > 1) return null;
		AABB surface = new AABB(
				before.minX + (after.minX - before.minX) * fraction,
				before.minY + (after.minY - before.minY) * fraction,
				before.minZ + (after.minZ - before.minZ) * fraction,
				before.maxX + (after.maxX - before.maxX) * fraction,
				body.minY,
				before.maxZ + (after.maxZ - before.maxZ) * fraction);
		return isSupported(body, surface) ? surface : null;
	}

	/** 到碰撞箱的最近点，用实际箱体距离而非实体中心距离判定借力。 */
	public static Vec3 closestPoint(AABB bounds, Vec3 point) {
		return new Vec3(Math.clamp(point.x, bounds.minX, bounds.maxX),
				Math.clamp(point.y, bounds.minY, bounds.maxY),
				Math.clamp(point.z, bounds.minZ, bounds.maxZ));
	}

	public static double distanceSquared(AABB first, AABB second) {
		double x = Math.max(0, Math.max(first.minX - second.maxX, second.minX - first.maxX));
		double y = Math.max(0, Math.max(first.minY - second.maxY, second.minY - first.maxY));
		double z = Math.max(0, Math.max(first.minZ - second.maxZ, second.minZ - first.maxZ));
		return x * x + y * y + z * z;
	}
}
