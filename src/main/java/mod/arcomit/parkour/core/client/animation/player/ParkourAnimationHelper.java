package mod.arcomit.parkour.core.client.animation.player;

import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonAction;
import mod.arcomit.parkour.core.client.animation.firstperson.FirstPersonParkourRenderer;
import mod.arcomit.parkour.content.init.ParkourAnimationIds;
import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import mod.arcomit.parkour.content.client.handler.ClientParkourSetupHandler;
import mod.arcomit.parkour.content.client.init.ClientParkourPlayerAnimations;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.client.animation.player.network.RequestPlayActionAnimC2SPayload;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

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

		if (controller.playAnimation(RawAnimation.begin().thenLoop(animData),
				ParkourAnimType.STATE, anim.fadeInTicks,
				anim.getAnimationModifiers(player, state.getAnimVariant()))) configureFirstPerson(controller, anim);
	}

	public static void playActionAnim(AbstractClientPlayer player, ResourceLocation animId) {
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

		if (!controller.playFadeOutAnim(
				RawAnimation.begin().then(animData, Animation.LoopType.PLAY_ONCE),
				ParkourAnimType.ACTION, anim.fadeInTicks, anim.fadeOutTicks,
				anim.getAnimationModifiers(player, 0))) return;
		configureFirstPerson(controller, anim);

		if (player.isLocalPlayer()) {
			if (animId.equals(ParkourAnimationIds.LANDING_ROLL)) FirstPersonParkourRenderer.play(FirstPersonAction.ROLL);
			if (animId.equals(ParkourAnimationIds.SPEED_VAULT_LEFT)) FirstPersonParkourRenderer.play(FirstPersonAction.VAULT, -1);
			if (animId.equals(ParkourAnimationIds.SPEED_VAULT_RIGHT)) FirstPersonParkourRenderer.play(FirstPersonAction.VAULT, 1);
			PacketDistributor.sendToServer(
					new RequestPlayActionAnimC2SPayload(animId));
		}
	}

	private static void configureFirstPerson(ParkourAnimationController controller, PlayerAnimation animation) {
		controller.setFirstPersonMode(animation.firstPerson ? FirstPersonMode.THIRD_PERSON_MODEL : FirstPersonMode.NONE);
		controller.setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, true, true));
		controller.setFirstPersonTransitionLength(animation.firstPerson ? 3 : 0);
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

	private static Animation getAnimData(ResourceLocation animId) {
		if (!PlayerAnimResources.hasAnimation(animId)) {
			PlayerAnimLibMod.LOGGER.error(
					"Could not find animation with the name:" + animId);
			return null;
		}
		return PlayerAnimResources.getAnimation(animId);
	}
}
