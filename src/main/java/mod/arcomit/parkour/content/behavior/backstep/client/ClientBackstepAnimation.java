package mod.arcomit.parkour.content.behavior.backstep.client;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-22
 */
public class ClientBackstepAnimation {
	/**
	 * 播放后撤步玩家动画。仅对本地玩家生效。
	 * <p>
	 * 副作用：第一人称下会修改玩家的xRot为0。
	 */
	public static void playPlayerAnim(Player player) {
		if (!player.isLocalPlayer())
			return;

		// 播放玩家后撤步动画
		Identifier playerAnimId = ParkourMod.prefix("backstep_back");
		ParkourProxies.PLAYER_ANIM_PROXY.playOneOffAnimation(player, playerAnimId, false, 0);
	}
}
