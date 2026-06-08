package mod.arcomit.parkour.core.proxy.api;

/**
 * Minecraft客户端设置代理接口。
 * <p>
 * 隔离对 {@code Minecraft.getInstance()} 的访问，用于查询客户端
 * 运行时的全局状态。服务端无MC实例，通过此接口返回安全的默认值。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface IMinecraftProxy {
	/**
	 * 当前玩家的视角是否为第一人称。
	 *
	 * @return 第一人称时返回 {@code true}，第三人称视角或服务端返回 {@code false}
	 */
	boolean isFirstPerson();
}
