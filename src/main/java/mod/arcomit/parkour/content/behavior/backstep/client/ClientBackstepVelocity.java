package mod.arcomit.parkour.content.behavior.backstep.client;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.proxy.api.IInputProxy;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 后撤步速度计算与位置同步 —— 仅对本地客户端玩家生效。
 * <p>
 * 根据玩家当前的输入冲量方向和面向角度，计算后退方向的水平速度向量， 叠加到玩家当前速度上并通知服务端同步坐标。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientBackstepVelocity {

	/**
	 * 依据玩家输入冲量计算后退速度，附加到当前deltaMovement上。
	 * <p>
	 * 副作用：调用{@code sendPosition}向服务端发送位置同步包。
	 *
	 * @param player 目标玩家（仅本地玩家时生效，非本地玩家直接返回）
	 */
	public static void applyVelocityAndSendPosition(Player player) {
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

		Vec3 motion = new Vec3(motionX, 0, motionZ).normalize()
				.scale(ParkourConfig.slideBoostSpeed);
		player.setDeltaMovement(player.getDeltaMovement().add(motion));
		ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.sendPosition(player);
	}
}
