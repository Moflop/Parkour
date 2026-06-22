package mod.arcomit.parkour.core.proxy.client;

import mod.arcomit.parkour.ClientParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.proxy.api.IGetClientConfigProxy;
import net.minecraft.world.entity.player.Player;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-22
 */
public class ClientGetClientConfigProxyImpl implements IGetClientConfigProxy {
	@Override
	public boolean isEnableParkour(ParkourContext context) {
		return ClientParkourConfig.enableParkour;
	}
}
