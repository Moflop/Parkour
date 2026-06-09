package mod.arcomit.parkour.core.proxy.server;

import mod.arcomit.parkour.core.proxy.api.IMinecraftProxy;

/**
 * {@link IMinecraftProxy} 的服务端空操作实现。
 * <p>
 * 服务端不存在 {@code Minecraft} 实例，无法查询视角类型等客户端设置。 所有查询返回安全的默认值（如 {@code isFirstPerson()} 返回
 * {@code false}）， 保证服务端不尝试访问客户端专属类。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerDummyMinecraftProxy implements IMinecraftProxy {
	@Override
	public boolean isFirstPerson() {
		return false;
	}
}
