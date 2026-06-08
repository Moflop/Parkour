package mod.arcomit.parkour.core.proxy.api;

import net.minecraft.world.entity.player.Player;

/**
 * 网络连接状态代理接口。
 * <p>
 * 查询玩家当前的连接是否有效。客户端直接访问
 * {@code Connection} 对象，而服务端不存在连接对象，通过此接口隔离差异。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface IConnectionProxy {
	/**
	 * 判断目标玩家的网络连接是否已断开。
	 *
	 * @param player 待检查的目标玩家，不可为null
	 * @return 连接为null或已断开时返回 {@code true}；仅在客户端有效，服务端始终返回 {@code false}
	 */
	boolean isConnectionNull(Player player);
}
