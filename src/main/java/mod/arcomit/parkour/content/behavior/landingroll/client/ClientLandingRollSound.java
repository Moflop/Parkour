package mod.arcomit.parkour.content.behavior.landingroll.client;

import mod.arcomit.parkour.content.init.ParkourSounds;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 落地翻滚音效 —— 客户端播放自定义翻滚音效，绑定到玩家实体位置。
 * <p>
 * 音效使用随机种子确保每次播放音色略有差异。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientLandingRollSound {
	private static final float ROLL_SOUND_VOLUME = 1.0f;
	private static final float ROLL_SOUND_PITCH = 1.0f;

	/**
	 * 播放翻滚音效，音源为玩家类别，位置跟随玩家实体。
	 */
	public static void play(Player player) {
		ParkourProxies.SOUND_PROXY.playEntityBoundSound(ParkourSounds.LANDING_ROLL.get(),
				SoundSource.PLAYERS, ROLL_SOUND_VOLUME, ROLL_SOUND_PITCH, player,
				player.getRandom().nextLong());
	}
}
