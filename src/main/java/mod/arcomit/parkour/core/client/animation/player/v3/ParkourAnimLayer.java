package mod.arcomit.parkour.core.client.animation.player.v3;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.animation.layered.ModifierLayer;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.event.EventResult;
import lombok.Getter;
import net.minecraft.world.entity.Avatar;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-23
 */
public class ParkourAnimLayer extends ModifierLayer<IAnimation> {

	private final PlayerAnimationController CONTROLLER;

	@Getter
	private ParkourAnimType currentType = null;

	public ParkourAnimLayer(Avatar avatar) {
		CONTROLLER = new PlayerAnimationController(avatar,
				(controller, state, setter) -> PlayState.CONTINUE);
		CONTROLLER.setCustomInstructionKeyframeHandler((tick, ctrl, keyFrameData, animData) -> {
			if (this.currentType == ParkourAnimType.ACTION) {
				return this.handleCustomInstruction(keyFrameData.getInstructions(), ctrl, tick);
			}
			return EventResult.PASS;
		});
	}

	public void playAnimation(RawAnimation rawAnimation, ParkourAnimType animType, int fadeTicks, Iterable<? extends AbstractModifier> modifiers) {
		if (this.currentType != null && !this.isActive()) {
			this.currentType = null;
		}

		if (this.isActive() && !animType.canOverride(this.currentType)) {
			return;
		}

		CONTROLLER.removeAllModifiers();
		if (modifiers != null) {
			for (AbstractModifier modifier : modifiers) {
				CONTROLLER.addModifierLast(modifier);
			}
		}

		AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(fadeTicks, EasingType.LINEAR);
		if (this.getAnimation() == null) {
			// 如果当前没绑定动画控制器，动画控制器直接播放动画，让Layer处理过渡
			CONTROLLER.triggerAnimation(rawAnimation);
			this.removeModifierIf(modifier -> modifier instanceof AbstractFadeModifier);
			this.replaceAnimationWithFade(fadeModifier, CONTROLLER);
		}else {
			// 如果绑定了动画控制器，让动画控制器处理过渡
			CONTROLLER.replaceAnimationWithFade(fadeModifier, rawAnimation);
		}
		this.currentType = animType;
	}

	public void stopAnimation(int fadeTicks) {
		// ACTION动画无法被停止，但是可以被playAnimation覆盖
		if (this.currentType == ParkourAnimType.ACTION) return;

		if (this.isActive()) {
			this.removeModifierIf(modifier -> modifier instanceof AbstractFadeModifier);
			AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(fadeTicks, EasingType.LINEAR);
			this.replaceAnimationWithFade(fadeModifier, null);
		}
		this.currentType = null;
	}

	protected EventResult handleCustomInstruction(String instruction, AnimationController ctrl, float tick) {
		if (instruction.contains("fade_out")) {
			var animation = ctrl.getCurrentAnimationInstance();
			if (animation != null) {
				float remainingTicks = animation.length() - tick;
				int fadeTicks = Math.max(1, Math.round(remainingTicks));
				this.stopAnimation(fadeTicks);
			}
			return EventResult.SUCCESS;
		}
		return EventResult.PASS;
	}
}
