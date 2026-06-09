package mod.arcomit.parkour.content.behavior.slide.client;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.proxy.api.IInputProxy;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端滑铲物理计算与位置同步 —— 仅对本地玩家生效。
 * <p>
 * 根据玩家输入冲量方向计算推进速度，并叠加到当前deltaMovement上。 空中启用Tap-Strafing时，还会将玩家朝向转向其实际移动方向。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientSlideLogic {

	/**
	 * 计算滑铲推进速度并同步位置到服务端。
	 * <p>
	 * 副作用：修改玩家的setDeltaMovement；若在空中且启用Tap-Strafing则修改玩家Yaw朝向； 调用sendPosition向服务端同步坐标。
	 *
	 * @param player 目标玩家（仅本地玩家时生效）
	 */
	public static void applyPhysicsAndSendPosition(Player player) {
		if (!player.isLocalPlayer())
			return;
		IInputProxy input = ParkourProxies.INPUT_PROXY;
		float forwardImpulse = input.getForwardImpulse(player);
		float leftImpulse = input.getLeftImpulse(player);

		float yRotRad = player.getYRot() * Mth.DEG_TO_RAD;
		float sin = Mth.sin(yRotRad);
		float cos = Mth.cos(yRotRad);

		double motionX = leftImpulse * cos - forwardImpulse * sin;
		double motionZ = forwardImpulse * cos + leftImpulse * sin;

		if (!player.onGround()) {
			if (ParkourConfig.enableTapStrafing) {
				double targetYRot = Math.toDegrees(Math.atan2(-motionX, motionZ));
				float currentYRot = player.getYRot();
				float diff = Mth.wrapDegrees((float) targetYRot - currentYRot);

				player.setYRot(currentYRot + diff);
				player.yRotO = player.getYRot();
			}
		}

		Vec3 motion = new Vec3(motionX, 0, motionZ).normalize()
				.scale(ParkourConfig.slideBoostSpeed);
		player.setDeltaMovement(player.getDeltaMovement().add(motion));
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
	}
}
