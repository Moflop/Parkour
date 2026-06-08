package mod.arcomit.parkour.core.proxy;

import mod.arcomit.parkour.core.proxy.api.*;
import mod.arcomit.parkour.core.proxy.server.*;

/**
 * 跑酷模组跨端代理的统一入口。
 * <p>
 * 所有静态字段初始化时默认为服务端空操作实例
 * （{@code ServerDummy*}），确保在服务端启动阶段不会因引用
 * 客户端类而崩溃。进入客户端世界后，由各模块通过
 * {@code DistExecutor} 或类似的客户端初始化回调将对应字段替换为
 * 真实的 {@code Client*Impl} 实例。
 * </p>
 * <p>
 * 状态机等其他组件只需通过 {@code ParkourProxies.INPUT_PROXY} 等形式
 * 获取代理，无需关心当前运行环境是客户端还是服务端。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourProxies {
	/**
	 * 玩家输入代理——服务端启动时默认为 {@link ServerDummyInputProxy}，
	 * 客户端初始化后替换为 {@code ClientInputProxyImpl}。
	 */
	public static IInputProxy INPUT_PROXY = new ServerDummyInputProxy();

	/**
	 * 音效播放代理——服务端启动时默认为 {@link ServerDummySoundProxy}，
	 * 客户端初始化后替换为 {@code ClientSoundProxyImpl}。
	 */
	public static ISoundProxy SOUND_PROXY = new ServerDummySoundProxy();

	/**
	 * 玩家动画代理——服务端启动时默认为 {@link ServerDummyPlayerAnimProxy}，
	 * 客户端初始化后替换为 {@code ClientPlayerAnimProxyImpl}。
	 */
	public static IPlayerAnimProxy PLAYER_ANIM_PROXY = new ServerDummyPlayerAnimProxy();

	/**
	 * 摄像机动画代理——服务端启动时默认为 {@link ServerDummyCameraAnimProxy}，
	 * 客户端初始化后替换为 {@code ClientCameraAnimProxyImpl}。
	 */
	public static ICameraAnimProxy CAMERA_PROXY = new ServerDummyCameraAnimProxy();

	/**
	 * 本地玩家服务代理——服务端启动时默认为 {@link ServerDummyLocalPlayerServicesProxy}，
	 * 客户端初始化后替换为 {@code ClientLocalPlayerServicesProxyImpl}。
	 */
	public static ILocalPlayerServicesProxy LOCAL_PLAYER_SERVICES_PROXY =
			new ServerDummyLocalPlayerServicesProxy();

	/**
	 * Minecraft客户端代理——服务端启动时默认为 {@link ServerDummyMinecraftProxy}，
	 * 客户端初始化后替换为 {@code ClientMinecraftProxyImpl}。
	 */
	public static IMinecraftProxy MINECRAFT_PROXY = new ServerDummyMinecraftProxy();
}
