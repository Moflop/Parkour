package mod.arcomit.parkour.core.proxy.server;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import mod.arcomit.parkour.core.proxy.api.IPlayerAnimProxy;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * {@link IPlayerAnimProxy} 的服务端空操作实现。
 * <p>
 * 玩家动画系统（{@code PlayerAnimationManager}）仅在客户端存在并渲染， 服务端无动画管线，所有播放请求静默丢弃。这在逻辑上是合理的——
 * 动画是纯视觉效果，不影响服务端游戏状态。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ServerDummyPlayerAnimProxy implements IPlayerAnimProxy {
	@Override
	public void playStateAnimation(Player player) {
	}

	@Override
	public void playActionAnimation(Player player, Identifier animId) {
	}
}
