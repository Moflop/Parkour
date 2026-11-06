package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.compat.DynamicCollisionSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** A material ledge and two palm contacts, stored in the owning structure's coordinates. */
public record ArmhangGrip(BlockPos block, DynamicCollisionSource source, Vec3 localCenter,
		Vec3 localNormal, Vec3 localLeftHand, Vec3 localRightHand) {
	public Vec3 center(float partialTick) { return world(localCenter, partialTick); }
	public Vec3 leftHand(float partialTick) { return world(localLeftHand, partialTick); }
	public Vec3 rightHand(float partialTick) { return world(localRightHand, partialTick); }
	public Vec3 normal(float partialTick) {
		return world(localCenter.add(localNormal), partialTick).subtract(center(partialTick)).normalize();
	}
	public Vec3 world(Vec3 point, float partialTick) {
		return source == null ? point : source.toRenderWorld(point, partialTick);
	}
	public Vec3 logical(Vec3 point) { return source == null ? point : source.toWorld(point); }
	public boolean sameSurface(ArmhangGrip other) {
		return other != null && (source == null ? other.source == null
				: other.source != null && source.identity().equals(other.source.identity()))
				&& localNormal.dot(other.localNormal) > 0.95;
	}
}
