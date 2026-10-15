package mod.arcomit.parkour.content.behavior.barswing;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.init.ParkourAnimationIds;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 再次按下跳跃键时借力跃出；客户端和服务端复用准入及速度上限。 */
public final class BarSwingJump {
	private BarSwingJump() {}

	public static boolean execute(Player player) {
		var context = ParkourContext.get(player);
		var data = BarSwingData.get(player);
		if (!(context.state().getState() instanceof BarSwingState state) || !state.isValid(player, context)
				|| player.isShiftKeyDown()) return false;
		Vec3 velocity = BarSwingMath.launch(data.forward, data.angle, data.angularVelocity,
				player.getLookAngle(), ParkourConfig.barSwingJumpStrength);
		if (data.source != null) velocity = velocity.add(data.source.jumpMotion(player, player.position()));
		data.releaseAssist = BarSwingCollision.blocksRelease(player, data, velocity);
		data.releaseMotion = velocity;
		ParkourStateMachine.resetToDefaultState(player, context);
		context.jump().setJumped(true);
		context.jump().setTicksSinceLastJump(0);
		context.jump().setSpeedVaultJumpWindow(0);
		player.setDeltaMovement(velocity);
		// 原版先求解竖直碰撞，杆正下方松手会先顶到杆、再向外移动。只为这一帧保留抬升。
		data.releaseLift = player.isLocalPlayer() && !BarSwingCollision.clear(player,
				player.getBoundingBox().expandTowards(0, velocity.y, 0)) ? velocity.y : 0;
		data.releaseTick = player.tickCount;
		player.setOnGround(false);
		player.hasImpulse = true;
		ParkourProxies.PLAYER_ANIM_PROXY.playActionAnimation(player, ParkourAnimationIds.BAR_SWING_JUMP);
		return true;
	}
}
