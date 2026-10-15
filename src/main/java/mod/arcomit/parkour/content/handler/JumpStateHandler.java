package mod.arcomit.parkour.content.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.content.context.JumpData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import mod.arcomit.parkour.content.mechanic.entitysupport.EntitySupport;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 玩家落地时清除所有跳跃相关的状态标记。
 * <p>
 * 确保上一次跳跃的视角方向（上跳、平行跳、视角跳）、墙壁碰撞记录、 攀爬状态等数据不会残留到下一次跳跃，避免跨跳跃的状态污染。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class JumpStateHandler {

	@SubscribeEvent
	public static void onGround(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		JumpData jumpData = ParkourContext.get(player).jump();
		WallMovementData wallMovementData = ParkourContext.get(player).wall();
		if (DynamicCollisions.isOnGround(player)) {
			// 服务端不能仅信任移动包的 onGround 标记来刷新空中次数。
			AABB body = player.getBoundingBox();
			AABB feet = new AABB(body.minX + 1.0E-5, body.minY - 0.05, body.minZ + 1.0E-5,
					body.maxX - 1.0E-5, body.minY, body.maxZ - 1.0E-5);
			if (jumpData.isEntityJumpUsed() && (EntitySupport.findSupport(player) != null
					|| player.level().getBlockCollisions(player, feet).iterator().hasNext()
					|| !player.level().getEntityCollisions(player, feet).isEmpty()
					|| DynamicCollisions.hasSensorSupport(player.level(), player, feet, null))) {
				jumpData.setEntityJumpUsed(false);
			}
			if (jumpData.getLastViewJump() != null) {
				jumpData.resetLastViewJump();
			}
			if (jumpData.getLastUpJump() != null) {
				jumpData.resetLastUpJump();
			}
			if (jumpData.getLastParallelJump() != null) {
				jumpData.resetLastParallelJump();
			}
			if (wallMovementData.getRunCollision() != null) {
				wallMovementData.resetRunCollision();
			}
			if (wallMovementData.getClimb() != null) {
				wallMovementData.resetClimb();
			}
		}
	}
}
