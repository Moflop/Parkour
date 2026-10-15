package mod.arcomit.parkour.content.action.entityjump;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.mechanic.entitysupport.EntitySupport;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 靠近实体时再次按跳跃键借力；每次腾空一次，保留水平惯性。 */
public class EntityJumpAction {

	public static boolean execute(Player player) {
		var context = ParkourContext.get(player);
		var jump = context.jump();
		if (!ParkourConfig.enableEntityJump || !player.isAlive() || player.onGround()
				|| jump.isEntityJumpUsed() || !(context.state().getState() instanceof DefaultState)
				|| !EntitySupport.isParkourEnabled(player)
				|| !ParkourChecks.canPerformBehavior(player) || player.onClimbable()
				|| player.isInWater() || player.isInLava() || player.isCrouching()
				|| ParkourProxies.INPUT_PROXY.getShiftKeyDown(player)) return false;
		// 其他跳跃处理器先处理同一次按下，避免一次输入同时执行两个动作。
		if (player.isLocalPlayer() && jump.getTicksSinceLastJump() == 0) return false;
		if (!EntitySupport.hasJumpContact(player, ParkourConfig.entityJumpReach)) return false;

		double strength = player.getAttributeValue(Attributes.JUMP_STRENGTH) + player.getJumpBoostPower();
		if (strength <= 0) return false;
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.x, Math.max(velocity.y, strength), velocity.z);
		player.setOnGround(false);
		player.hasImpulse = true;
		player.resetFallDistance();
		jump.setEntityJumpUsed(true);
		jump.setSpeedVaultJumpWindow(0);
		jump.setJumped(true);
		jump.setTicksSinceLastJump(0);
		return true;
	}
}
