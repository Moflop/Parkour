package mod.arcomit.parkour.content.action.swimmingboost;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 游泳加速音效——在任意端播放水下进入音效，为加速提供听觉反馈。
 *
 * <p>使用原版 {@code AMBIENT_UNDERWATER_ENTER} 音效，音量0.9、音调0.8。
 * 不区分客户端/服务端，直接通过Level播放。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SwimmingBoostSound {
	/** 音量，略低于标准音量以保持自然 */
	public static final float SWIMMING_BOOST_SOUND_VOLUME = 0.9f;
	/** 音调，略低于标准以获得低沉水流感 */
	public static final float SWIMMING_BOOST_SOUND_PITCH = 0.8f;

	/**
	 * 在玩家位置播放游泳加速音效（双端可用）。
	 *
	 * @param player 音效附着的玩家，不可为null
	 */
	public static void play(Player player) {
		Level level = player.level();
		level.playSound(player, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.PLAYERS,
				SWIMMING_BOOST_SOUND_VOLUME, SWIMMING_BOOST_SOUND_PITCH);
	}
}
