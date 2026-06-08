package mod.arcomit.parkour.core.proxy.server;

import mod.arcomit.parkour.core.proxy.api.ICameraAnimProxy;
import net.minecraft.resources.ResourceLocation;

/**
 * {@link ICameraAnimProxy} 的服务端空操作实现。
 * <p>
 * 服务端不存在摄像机动画系统（{@code CameraAnimationManager} 为客户端类），
 * 所有方法均为空实现，防止服务端调用导致 {@code NoClassDefFoundError}。
 * 在启动时设为默认代理，进入客户端后由实际实现替换。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerDummyCameraAnimProxy implements ICameraAnimProxy {
	@Override
	public void playAnimation(ResourceLocation animationId) {
	}

	@Override
	public void stopAnimation() {
	}
}
