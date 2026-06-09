package mod.arcomit.parkour.core.proxy.client;

import mod.arcomit.parkour.core.proxy.api.IMinecraftProxy;
import net.minecraft.client.Minecraft;

/**
 * {@link IMinecraftProxy} 的客户端实现。
 * <p>
 * 通过 {@code Minecraft.getInstance()} 获取当前客户端实例， 查询视角类型等全局设置。仅在客户端环境下有效。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientMinecraftProxyImpl implements IMinecraftProxy {
	@Override
	public boolean isFirstPerson() {
		return Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}
}
