package mod.arcomit.parkour.content.mechanic.entitysupport;

import mod.arcomit.parkour.compat.DynamicCollisions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 随脚下实体平移；位移经过正常碰撞，起跳、走离或实体传送时立即脱离。 */
public class EntitySupportMotion {

	private Entity support;
	private Vec3 anchor;
	private Vec3 playerAnchor;

	public void beforeTravel(Player player) {
		if (!player.isLocalPlayer()) return;
		if (support == null || !EntitySupport.isEnabled(player) || !EntitySupport.canSupport(player, support)
				|| player.getDeltaMovement().y > 0 || !player.onGround()
				|| player.position().distanceToSqr(playerAnchor) > 4) {
			support = null;
			return;
		}
		Vec3 next = topCenter(support);
		Vec3 motion = next.subtract(anchor);
		support = null;
		if (motion.lengthSqr() > 4 || motion.lengthSqr() < 1.0E-14) return;
		DynamicCollisions.move(player, MoverType.SELF, motion);
		if (EntitySupport.findSupport(player) != null) player.setOnGround(true);
	}

	public void afterTravel(Player player) {
		if (!player.isLocalPlayer()) return;
		support = player.onGround() && player.getDeltaMovement().y <= 0 ? EntitySupport.findSupport(player) : null;
		anchor = support == null ? null : topCenter(support);
		playerAnchor = player.position();
	}

	private static Vec3 topCenter(Entity entity) {
		var box = entity.getBoundingBox();
		return new Vec3((box.minX + box.maxX) * 0.5, box.maxY, (box.minZ + box.maxZ) * 0.5);
	}
}
