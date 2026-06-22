package mod.arcomit.parkour.core.proxy.server;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.proxy.api.IGetClientConfigProxy;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-22
 */
public class ServerGetClientConfigProxy implements IGetClientConfigProxy {
	@Override
	public boolean isEnableParkour(ParkourContext context) {
		return context.state().isParkourEnabled();
	}
}
