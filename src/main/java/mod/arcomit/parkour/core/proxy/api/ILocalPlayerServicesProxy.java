package mod.arcomit.parkour.core.proxy.api;

import net.minecraft.world.entity.player.Player;

/**
 * 本地玩家专属服务代理接口。
 * <p>
 * 封装仅在客户端可用的 {@code LocalPlayer} 方法——位置同步、移动状态、 饥饿度检查等。状态机通过此接口获取这些信息，避免在服务端触发
 * {@code NoClassDefFoundError}。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface ILocalPlayerServicesProxy {

	/**
	 * 将当前玩家的位置同步到服务端。
	 * <p>
	 * 在客户端触发后，服务端会通过数据包更新该玩家的位置。 服务端调用时为空操作。
	 * </p>
	 *
	 * @param player 需要同步位置的玩家，不可为null
	 */
	void sendPosition(Player player);

	/**
	 * 判断玩家当前是否有移动输入（横向或纵向任一方向有非零力度）。
	 *
	 * @param player 目标玩家，不可为null
	 * @return 有移动输入时返回 {@code true}，服务端返回 {@code false}
	 */
	boolean isMoving(Player player);

}
