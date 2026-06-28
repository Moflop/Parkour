package mod.arcomit.parkour.core.proxy.api;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家动画代理接口。
 * <p>
 * 封装对 {@code PlayerAnimationManager} 的调用——播放与跑酷状态 绑定的循环动画和一次性动作动画。所有动画仅在客户端渲染， 服务端调用时自动降级为空操作。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface IPlayerAnimProxy {
	/**
	 * 根据玩家当前所处的跑酷状态自动选择并播放对应的循环动画。
	 * <p>
	 * 内部通过状态名称和动画注册表的映射关系决定具体播放哪段动画。 如果玩家不是 {@code AbstractClientPlayer} 实例（例如在服务端），则无操作。
	 * </p>
	 *
	 * @param player 需要更新动画的玩家，不可为null
	 */
	void playStateAnimation(Player player);

	/**
	 * 播放一段一次性动作动画，可被后续动画中断。
	 *
	 * @param player        目标玩家，不可为null
	 * @param animId        动画资源标识，需在玩家动画注册表中已注册，不可为null
	 */
	void playActionAnimation(Player player, Identifier animId);
}
