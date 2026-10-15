package mod.arcomit.parkour.core.client.animation.player.modifier;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import org.jetbrains.annotations.NotNull;

/** 在动作最后几帧连续淡出；保留原始关键帧，不在淡出起点冻结姿势。 */
public class FadeOutModifier extends AbstractModifier {
	private final int fadeTicks;
	private float progress;
	private final PlayerAnimBone incoming = new PlayerAnimBone("");

	public FadeOutModifier(int fadeTicks) {
		this.fadeTicks = fadeTicks;
	}

	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		var controller = getController();
		if (controller == null || controller.getCurrentAnimation() == null) return;
		float length = (float) controller.getCurrentAnimation().animation().length();
		float duration = Math.min(length, fadeTicks);
		progress = duration > 0 ? Math.clamp((controller.getAnimationTicks() - length + duration) / duration, 0, 1) : 1;
		progress = progress * progress * (3 - 2 * progress);
	}

	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		incoming.copyOtherBone(bone);
		bone = super.get3DTransform(bone);
		// 旋转使用最短弧，避免 ±π 两侧的姿势在过渡期间绕回一整圈。
		float x = bone.rotX + wrap(incoming.rotX - bone.rotX) * progress;
		float y = bone.rotY + wrap(incoming.rotY - bone.rotY) * progress;
		float z = bone.rotZ + wrap(incoming.rotZ - bone.rotZ) * progress;
		bone.scale(1 - progress).add(incoming.scale(progress));
		bone.rotX = x; bone.rotY = y; bone.rotZ = z;
		return bone;
	}

	private static float wrap(float angle) {
		return (float) Math.atan2(Math.sin(angle), Math.cos(angle));
	}
}
