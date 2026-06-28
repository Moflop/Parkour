package mod.arcomit.parkour.core.client.animation.player.v3;

import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import mod.arcomit.parkour.content.client.handler.ClientParkourSetupHandler;
import mod.arcomit.parkour.content.client.init.ClientParkourPlayerAnimations;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.client.animation.player.network.RequestPlayActionAnimC2SPayload;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-26
 */
public class ParkourAnimationHelper {

	public static void playStateAnim(AbstractClientPlayer player) {
		var state = ParkourContext.get(player).state();
		var currentState = state.getState();
		var animId = currentState.animId(player);

		var controller = getController(player);
		if (controller == null)
			return;

		if (animId == null) {
			controller.stopAnimation(5);
			return;
		}

		PlayerAnimation anim = ClientParkourPlayerAnimations.PLAYER_ANIMATIONS.get(animId);
		if (anim == null)
			return;

		var animData = getAnimData(anim.animName);
		if (animData == null)
			return;

		controller.playAnimation(RawAnimation.begin().thenLoop(animData),
				ParkourAnimType.STATE, anim.fadeInTicks,
				anim.getAnimationModifiers(player, state.getAnimVariant()));
	}

	public static void playActionAnim(AbstractClientPlayer player, Identifier animId) {
		if (animId == null)
			return;
		var controller = getController(player);
		if (controller == null)
			return;

		PlayerAnimation anim = ClientParkourPlayerAnimations.PLAYER_ANIMATIONS.get(animId);
		if (anim == null)
			return;

		var animData = getAnimData(anim.animName);
		if (animData == null)
			return;

		controller.playFadeOutAnim(
				RawAnimation.begin().then(animData, Animation.LoopType.PLAY_ONCE),
				ParkourAnimType.ACTION, anim.fadeInTicks, anim.fadeOutTicks,
				anim.getAnimationModifiers(player, 0));

		if (player.isLocalPlayer()) {
			ClientPacketDistributor.sendToServer(
					new RequestPlayActionAnimC2SPayload(animId));
		}
	}

	private static ParkourAnimationController getController(AbstractClientPlayer player) {
		var layer = PlayerAnimationAccess.getPlayerAnimationLayer(player,
				ClientParkourSetupHandler.PARKOUR_ANIM_LAYER_ID);
		if (!(layer instanceof ParkourAnimationController parkourAnimController)) {
			PlayerAnimLibMod.LOGGER.debug(
					"Could not find parkour animation layer for player: " + player.getName()
							.getString());
			return null;
		}
		return parkourAnimController;
	}

	private static Animation getAnimData(Identifier animId) {
		if (!PlayerAnimResources.hasAnimation(animId)) {
			PlayerAnimLibMod.LOGGER.error(
					"Could not find animation with the name:" + animId);
			return null;
		}
		return PlayerAnimResources.getAnimation(animId);
	}
}
