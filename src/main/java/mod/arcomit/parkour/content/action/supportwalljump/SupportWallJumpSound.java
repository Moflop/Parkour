package mod.arcomit.parkour.content.action.supportwalljump;

import mod.arcomit.parkour.content.init.ParkourSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 支撑蹬墙跳音效——与蹬墙跳共用同一音效资源（{@code WALL_JUMP}）， 仅在服务端播放。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SupportWallJumpSound {
	/** 音量，1.5倍标准音量 */
	private static final float WALL_JUMP_SOUND_VOLUME = 1.5f;
	/** 音调，标准音调无偏移 */
	private static final float WALL_JUMP_SOUND_PITCH = 1.0f;

	/**
	 * 在玩家所在位置播放蹬墙跳音效。客户端侧调用无效（静默跳过）。
	 *
	 * @param player 音效附着的玩家，不可为null
	 */
	public static void play(Player player) {
		Level level = player.level();
		if (!level.isClientSide) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					ParkourSounds.WALL_JUMP.get(), SoundSource.PLAYERS,
					WALL_JUMP_SOUND_VOLUME, WALL_JUMP_SOUND_PITCH);
		}
	}
}
