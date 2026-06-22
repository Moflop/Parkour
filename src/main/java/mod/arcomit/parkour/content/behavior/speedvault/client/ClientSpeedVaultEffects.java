package mod.arcomit.parkour.content.behavior.speedvault.client;

import mod.arcomit.parkour.content.client.init.ParkourPlayerAnimations;
import mod.arcomit.parkour.content.init.ParkourSounds;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 负责处理客户端 Speed Vault 状态下的表现逻辑（音效与动画）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientSpeedVaultEffects {

	/**
	 * 播放速过音效，并在本地玩家身上随机播放左手或右手翻越动画。
	 *
	 * <p>音效从玩家位置发出，音量和音调固定；动画为一次性播放，随机选择左/右侧。
	 *
	 * @param player 目标玩家，不能为 null
	 */
	public static void playSoundAndAnim(Player player) {
		ParkourProxies.SOUND_PROXY.playEntityBoundSound(ParkourSounds.SPEED_VAULT.get(),
				SoundSource.PLAYERS, 1.0F, 1.0F, player,
				player.getRandom().nextLong());

		if (player.isLocalPlayer()) {
			ParkourProxies.PLAYER_ANIM_PROXY.playOneOffAnimation(player,
					Math.random() < 0.5 ?
							ParkourPlayerAnimations.SPEED_VAULT_LEFT.id :
							ParkourPlayerAnimations.SPEED_VAULT_RIGHT.id,
					false, 2);
		}
	}
}
