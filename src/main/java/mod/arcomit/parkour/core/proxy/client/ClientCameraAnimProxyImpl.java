package mod.arcomit.parkour.core.proxy.client;

import mod.arcomit.parkour.core.client.animation.camera.CameraAnimationManager;
import mod.arcomit.parkour.core.proxy.api.ICameraAnimProxy;
import net.minecraft.resources.ResourceLocation;

/**
 * {@link ICameraAnimProxy} 的客户端实现。
 * <p>
 * 直接委托给 {@link CameraAnimationManager} 单例，在客户端执行真实的 摄像机动画播放与停止逻辑。仅在物理客户端 {@code Dist.CLIENT} 端加载。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientCameraAnimProxyImpl implements ICameraAnimProxy {
	@Override
	public void playAnimation(ResourceLocation animationId) {
		CameraAnimationManager.INSTANCE.play(animationId);
	}

	@Override
	public void stopAnimation() {
		CameraAnimationManager.INSTANCE.stop();
	}
}
