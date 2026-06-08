package mod.arcomit.parkour.core.proxy.client;

import mod.arcomit.parkour.core.client.animation.player.PlayerAnimationManager;
import mod.arcomit.parkour.core.proxy.api.IPlayerAnimProxy;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * {@link IPlayerAnimProxy} 的客户端实现。
 * <p>
 * 将 {@code Player} 向下转型为 {@code AbstractClientPlayer}，
 * 然后委托给 {@link PlayerAnimationManager} 单例执行实际的动画播放。
 * 若传入的不是客户端玩家实例则所有方法静默跳过。
 * 标记为 {@link OnlyIn#CLIENT}，仅客户端类加载器访问。
 * </p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class ClientPlayerAnimProxyImpl implements IPlayerAnimProxy {

	@Override
	public void playStateAnimation(Player player) {
		if (player instanceof AbstractClientPlayer clientPlayer) {
			PlayerAnimationManager.INSTANCE.playStateAnimation(clientPlayer);
		}
	}

	@Override
	public void playOneOffAnimation(Player player, ResourceLocation animId,
			boolean interruptible) {
		if (player instanceof AbstractClientPlayer clientPlayer) {
			PlayerAnimationManager.INSTANCE.playOneOffAnimation(clientPlayer, animId,
					interruptible);
		}
	}

	@Override
	public void playOneOffAnimation(Player player, ResourceLocation animId,
			boolean interruptible, int fadeTicks) {
		if (player instanceof AbstractClientPlayer clientPlayer) {
			PlayerAnimationManager.INSTANCE.playOneOffAnimation(clientPlayer, animId,
					interruptible, fadeTicks);
		}
	}

}
