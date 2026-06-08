package mod.arcomit.parkour.content.action.walljump;

import mod.arcomit.parkour.content.init.ParkourSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 蹬墙跳音效——在服务端播放蹬墙跳的定制音效。
 *
 * <p>仅在服务端播放（{@code !level.isClientSide}），客户端无需额外处理。
 * 音效源固定在玩家位置，以玩家分类播放。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallJumpSound {
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
