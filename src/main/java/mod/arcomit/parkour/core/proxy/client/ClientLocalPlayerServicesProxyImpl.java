package mod.arcomit.parkour.core.proxy.client;

import mod.arcomit.parkour.core.proxy.api.ILocalPlayerServicesProxy;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * {@link ILocalPlayerServicesProxy} 的客户端实现。
 * <p>
 * 将 {@code Player} 转型为 {@code LocalPlayer} 后调用其专有方法——
 * 位置数据包同步、移动输入判定、疾跑饱食度检查。
 * 若传入非本地玩家实例则所有方法返回安全的默认值。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientLocalPlayerServicesProxyImpl implements ILocalPlayerServicesProxy {
	@Override
	public void sendPosition(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			localPlayer.sendPosition();
		}
	}

	@Override
	public boolean isMoving(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			Input input = localPlayer.input;
			return input.forwardImpulse != 0 || input.leftImpulse != 0;
		}
		return false;
	}

	@Override
	public boolean hasEnoughFoodToStartSprinting(Player player) {
		if (player instanceof LocalPlayer localPlayer) {
			return localPlayer.hasEnoughFoodToStartSprinting();
		}
		return false;
	}
}
