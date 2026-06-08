package mod.arcomit.parkour.content.behavior.slide.client;

import mod.arcomit.parkour.content.init.ParkourSounds;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 滑铲音效 —— 客户端播放自定义滑铲音效，绑定到玩家实体位置。
 * <p>
 * 音效使用随机种子确保每次播放音色略有差异。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientSlideSound {
	private static final float SLIDE_SOUND_VOLUME = 1.0f;
	private static final float SLIDE_SOUND_PITCH = 1.0f;

	/**
	 * 播放滑铲音效，音源类别为玩家，位置跟随玩家实体。
	 */
	public static void play(Player player) {
		ParkourProxies.SOUND_PROXY.playEntityBoundSound(ParkourSounds.SLIDE.get(),
				SoundSource.PLAYERS, SLIDE_SOUND_VOLUME, SLIDE_SOUND_PITCH, player,
				player.getRandom().nextLong());
	}
}
