package mod.arcomit.parkour.core.client.animation.player;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.animation.layered.AnimationSnapshot;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.AdvancedBoneSnapshot;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import lombok.Getter;
import mod.arcomit.parkour.core.client.animation.player.modifier.FadeOutModifier;
import mod.arcomit.parkour.core.client.animation.player.modifier.ImmediateFadeOutModifier;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * 修复后的跑酷动画控制器
 *
 * @author Arcomit
 * @since 2026-06-26
 */
public class ParkourAnimationController extends PlayerAnimationController {

	@Getter
	private ParkourAnimType currentType = null;

	public ParkourAnimationController(AbstractClientPlayer avatar, AnimationStateHandler animationHandler) {
		super(avatar, animationHandler);
	}

	/**
	 * 提取公共的过渡动画快照与淡入逻辑
	 */
	private void applyFadeTransition(int fadeTicks) {
		AbstractFadeModifier fadeModifier = AbstractFadeModifier.standardFadeIn(fadeTicks, EasingType.LINEAR);

		// 只有当前处于活跃状态（正在播放动画）时，才需要提取上一段动画的快照来实现平滑过渡。
		// 如果是闲置状态，则不需要快照，直接从默认姿势缓入。
		if (this.isActive()) {
			Map<String, AdvancedBoneSnapshot> snapshots = new HashMap<>();

			for (PlayerAnimBone baseBone : this.bones.values()) {
				PlayerAnimBone snapshotBone = new PlayerAnimBone(baseBone.getName());
				this.get3DTransform(snapshotBone);
				// 存入全身快照
				var snapshot = new AdvancedBoneSnapshot(snapshotBone);
				snapshot.positionXEnabled = snapshot.positionYEnabled = snapshot.positionZEnabled = true;
				snapshot.rotXEnabled = snapshot.rotYEnabled = snapshot.rotZEnabled = true;
				snapshot.scaleXEnabled = snapshot.scaleYEnabled = snapshot.scaleZEnabled = true;
				snapshot.bendEnabled = true;
				snapshots.put(baseBone.getName(), snapshot);
			}
			fadeModifier.setTransitionAnimation(new AnimationSnapshot(snapshots));
		}

		// 清除旧的修饰器并添加新的。你的思路完全正确：
		// addModifierLast 底层会自己去调 linkModifiers() 重新接管链条，所以毫无问题。
		this.removeAllModifiers();
		this.addModifierLast(fadeModifier);
	}

	/**
	 * 检查并清理失效的动画状态
	 */
	private void cleanUpInactiveState() {
		if (this.currentType != null && !this.isActive()) {
			this.currentType = null;
		}
	}

	/**
	 * 统一添加额外的修饰器
	 */
	private void appendModifiers(Iterable<? extends AbstractModifier> modifiers) {
		if (modifiers != null) {
			modifiers.forEach(this::addModifierLast);
		}
	}

	/**
	 * 一般用于播放持续的状态动画
	 */
	public boolean playAnimation(RawAnimation rawAnimation, ParkourAnimType animType, int fadeTicks, Iterable<? extends AbstractModifier> modifiers) {
		cleanUpInactiveState();

		if (this.isActive() && !animType.canOverride(this.currentType)) {
			return false;
		}

		applyFadeTransition(fadeTicks);
		appendModifiers(modifiers);
		this.triggerAnimation(rawAnimation);

		this.currentType = animType;
		return true;
	}

	/**
	 * 一般用于播放一次性的动作动画，播放完后会自动淡出
	 */
	public boolean playFadeOutAnim(RawAnimation rawAnimation, ParkourAnimType animType, int fadeTicks, int fadeOutTicks, Iterable<? extends AbstractModifier> modifiers) {
		if (!this.playAnimation(rawAnimation, animType, fadeTicks, modifiers)) return false;
		if (fadeOutTicks > 0) {
			this.addModifierLast(new FadeOutModifier(fadeOutTicks));
		}
		return true;
	}

	public void stopAnimation(int fadeTicks) {
		if (this.currentType == ParkourAnimType.ACTION) return;
		// 同一默认状态可能同步两次，不能重新截取快照或重新启动淡出。
		if (this.currentType == null && this.isActive()) return;

		if (this.isActive()) {
			if (fadeTicks <= 0) {
				finishAnimation();
				return;
			}
			this.addModifierLast(new ImmediateFadeOutModifier(this, fadeTicks));
		}

		this.currentType = null;
		if (!this.isActive()) finishAnimation();
	}

	/** 结束当前播放所有权；PAL 会把刚触发的空动画也标记为 RUNNING。 */
	public void finishAnimation() {
		this.stopTriggeredAnimation();
		this.stop();
		this.removeAllModifiers();
		this.currentType = null;
		resetFirstPersonMode();
	}

	@Override
	public void tick(AnimationData state) {
		super.tick(state);
		// 未渲染的远端玩家也必须按 tick 结束动作，不能依赖下一次 setupAnim。
		if (!this.isActive() || this.currentType == ParkourAnimType.ACTION && this.getCurrentAnimation() != null
				&& this.getAnimationTicks() >= this.getCurrentAnimation().animation().length()) finishAnimation();
	}

	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		if (!this.isActive()) finishAnimation();
	}

	/** 清除一次性动作或状态动画设置的第一人称模型渲染模式。 */
	public void resetFirstPersonMode() {
		this.setFirstPersonMode(FirstPersonMode.NONE);
		this.setFirstPersonTransitionLength(0);
	}

	public Map<String, AdvancedPlayerAnimBone> getBone() {
		return this.bones;
	}
}
