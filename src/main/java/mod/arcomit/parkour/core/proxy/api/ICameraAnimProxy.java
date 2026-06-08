package mod.arcomit.parkour.core.proxy.api;

import net.minecraft.resources.ResourceLocation;

/**
 * 摄像机动画代理接口。
 * <p>
 * 为跑酷状态机提供与摄像机动画系统的隔离——状态机不直接依赖客户端
 * {@code CameraAnimationManager}，而是通过此接口调用，在服务端自动降级为空操作。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface ICameraAnimProxy {
	/**
	 * 按动画ID播放一段摄像机动画。
	 *
	 * @param animationId 已在摄像机动画注册表中注册的资源标识，不可为null
	 */
	void playAnimation(ResourceLocation animationId);

	/**
	 * 停止当前正在播放的摄像机动画，未播放时调用无副作用。
	 */
	void stopAnimation();
}
