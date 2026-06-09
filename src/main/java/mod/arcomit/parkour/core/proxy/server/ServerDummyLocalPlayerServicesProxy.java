package mod.arcomit.parkour.core.proxy.server;

import mod.arcomit.parkour.core.proxy.api.ILocalPlayerServicesProxy;
import net.minecraft.world.entity.player.Player;

/**
 * {@link ILocalPlayerServicesProxy} 的服务端空操作实现。
 * <p>
 * 服务端不存在 {@code LocalPlayer} 实例，其专有方法 （位置同步、移动判别、饱食度检查）全部以安全的默认值替代。 这些操作原本就是客户端才有的概念，服务端不参与。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerDummyLocalPlayerServicesProxy implements ILocalPlayerServicesProxy {
	@Override
	public void sendPosition(Player player) {
	}

	@Override
	public boolean isMoving(Player player) {
		return false;
	}

	@Override
	public boolean hasEnoughFoodToStartSprinting(Player player) {
		return false;
	}
}
