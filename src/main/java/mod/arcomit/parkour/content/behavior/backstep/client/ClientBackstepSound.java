package mod.arcomit.parkour.content.behavior.backstep.client;

import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 后撤步音效 —— 客户端播放已绑定在玩家实体上的疾风滑行音效。
 * <p>
 * 音效使用随机种子，确保每次播放的音色略有变化，增强反馈感。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientBackstepSound {
	private static final float BACKSTEPS_SOUND_VOLUME = 0.6f;
	private static final float BACKSTEPS_SOUND_PITCH = 1.4f;

	/**
	 * 播放后撤步音效，绑定到玩家实体位置，同一声源类别为玩家。
	 */
	public static void playSound(Player player) {
		ParkourProxies.SOUND_PROXY.playEntityBoundSound(SoundEvents.BREEZE_SLIDE,
				SoundSource.PLAYERS, BACKSTEPS_SOUND_VOLUME, BACKSTEPS_SOUND_PITCH,
				player, player.getRandom().nextLong());
	}
}
