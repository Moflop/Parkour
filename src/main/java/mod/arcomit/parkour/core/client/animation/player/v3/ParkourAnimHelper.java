package mod.arcomit.parkour.core.client.animation.player.v3;

import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import mod.arcomit.parkour.content.client.handler.ClientParkourSetupHandler;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Function;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-23
 */
public class ParkourAnimHelper {

	public static void playStateAnim(AbstractClientPlayer player) {
		var state = ParkourContext.get(player).state();
		var currentState = state.getState();
		var stateAnimId = currentState.animId(player);

		if (stateAnimId == null) {
			var layer = getLayer(player);
			if (layer != null) layer.stopAnimation(5);
			return;
		}

		playAnimInternal(
				player,
				stateAnimId,
				currentState.getAnimationModifiers(player, state.getAnimVariant()),
				ParkourAnimType.STATE,
				currentState.fadeInTicks(player),
				animData -> RawAnimation.begin().thenLoop(animData)
		);
	}

	public static void playActionAnim(AbstractClientPlayer player, Identifier animId,
			Iterable<? extends AbstractModifier> modifiers, int fadeTicks) {
		playAnimInternal(
				player,
				animId,
				modifiers,
				ParkourAnimType.ACTION,
				fadeTicks,
				animData -> RawAnimation.begin().then(animData, Animation.LoopType.PLAY_ONCE)
		);
	}

	private static void playAnimInternal(AbstractClientPlayer player, Identifier animId,
			Iterable<? extends AbstractModifier> modifiers,
			ParkourAnimType priority, int fadeTicks,
			Function<Animation, RawAnimation> rawAnimBuilder) {
		if (animId == null) return;
		var layer = getLayer(player);
		if (layer == null) return;
		var animData = getAnimData(animId);
		if (animData == null) return;

		layer.playAnimation(rawAnimBuilder.apply(animData), priority, fadeTicks, modifiers);
	}

	private static ParkourAnimLayer getLayer(AbstractClientPlayer player) {
		var layer = PlayerAnimationAccess.getPlayerAnimationLayer(player,
				ClientParkourSetupHandler.PARKOUR_ANIM_LAYER_ID);
		if (!(layer instanceof ParkourAnimLayer parkourAnimLayer)) {
			PlayerAnimLibMod.LOGGER.debug(
					"Could not find parkour animation layer for player: " + player.getName()
							.getString());
			return null;
		}
		return parkourAnimLayer;
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
