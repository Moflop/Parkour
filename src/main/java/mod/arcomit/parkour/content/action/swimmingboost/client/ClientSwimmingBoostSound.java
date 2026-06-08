package mod.arcomit.parkour.content.action.swimmingboost.client;

import mod.arcomit.parkour.content.action.swimmingboost.SwimmingBoostSound;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端游泳加速音效——通过音效代理以实体绑定方式播放，确保仅在
 * 客户端侧发声（避免服务端重复播放）。
 *
 * <p>与 {@link SwimmingBoostSound} 共用相同的音量和音调常量，
 * 但走自定义代理通道以精确控制客户端播放时机。</p>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientSwimmingBoostSound {

	/**
	 * 在客户端以实体绑定方式播放游泳加速音效。
	 *
	 * @param player 音效附着的玩家实体，不可为null
	 */
	public static void play(Player player) {
		ParkourProxies.SOUND_PROXY.playEntityBoundSound(
				SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.PLAYERS,
				SwimmingBoostSound.SWIMMING_BOOST_SOUND_VOLUME,
				SwimmingBoostSound.SWIMMING_BOOST_SOUND_PITCH, player,
				player.getRandom().nextLong());
	}
}
