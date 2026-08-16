package mod.arcomit.parkour.content.behavior.landingroll.client;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.landingroll.client.animation.player.LandingRollPlayerAnimModifier;
import mod.arcomit.parkour.content.init.ParkourAnimationIds;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 客户端落地翻滚动画入口 —— 同时触发摄像机翻滚动画和玩家骨骼动画。
 * <p>
 * 第一人称时会先将玩家俯仰角归零，避免翻滚结束后视角朝天的不良体验。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientLandingRollAnimation {
	/**
	 * 播放摄像机翻滚动画和玩家骨骼翻滚动画。仅对本地玩家生效。
	 * <p>
	 * 副作用：第一人称下会修改玩家的xRot为0。
	 */
	public static void playCameraAndPlayerAnim(Player player) {
		if (!player.isLocalPlayer())
			return;
		boolean isFirstPerson = ParkourProxies.MINECRAFT_PROXY.isFirstPerson();
		if (isFirstPerson) {
			player.setXRot(0);// 第一人称时重置玩家的俯仰角，使玩家在抬头时翻滚结束也能朝前看，手感更好
		}
		// 播放摄像机翻滚动画
		Identifier cameraAnimId = Identifier.fromNamespaceAndPath("parkour",
				"camera_animations/landing_roll.json/landing_roll");
		ParkourProxies.CAMERA_PROXY.playAnimation(cameraAnimId);

		// 播放玩家翻滚动画
		ParkourProxies.PLAYER_ANIM_PROXY.playActionAnimation(player, ParkourAnimationIds.LANDING_ROLL);
	}
}
