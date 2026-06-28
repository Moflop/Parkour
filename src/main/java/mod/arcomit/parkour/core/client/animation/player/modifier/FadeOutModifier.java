package mod.arcomit.parkour.core.client.animation.player.modifier;

import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.AnimationSnapshot;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.bones.ToggleablePlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import mod.arcomit.parkour.core.client.animation.player.ParkourAnimationController;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 动画淡出修饰器
 *
 * @author Arcomit
 * @since 2026-06-27
 */
public class FadeOutModifier extends AbstractModifier {
	private final int targetFadeTicks;
	private final Float2FloatFunction transformer = EasingType.LINEAR.buildTransformer(null);

	private int time = 0;
	private float currentProgress = 0.0f;

	// 状态控制
	private boolean isFading = false;
	private int fadeStartTime = 0;
	private boolean isSnapshotting = false;

	private float cachedAnimationLength = -1.0f;
	// 记录运行时实际受限的淡出时长（防止超过动画总时长）
	private float runtimeFadeTicks = 0.0f;

	// 快照数据
	private AnimationSnapshot snapshot = null;
	private Map<String, PlayerAnimBone> reusableBones = null;

	public FadeOutModifier(int fadeTicks) {
		this.targetFadeTicks = fadeTicks;
	}

	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		float tickDelta = state.getPartialTick();
		float actualTime = this.time + tickDelta;

		if (!this.isFading) {
			if (this.cachedAnimationLength < 0) {
				this.cachedAnimationLength = getAnimationLength();
				// 核心修复：实际淡出时间不能超过动画总时长。
				// 如果动画只有 5 ticks，要求淡出 10 ticks，则实际淡出强制缩短为 5 ticks
				this.runtimeFadeTicks = Math.min((float) this.targetFadeTicks, this.cachedAnimationLength);
			}

			if (this.cachedAnimationLength > 0 && actualTime >= this.cachedAnimationLength - this.runtimeFadeTicks) {
				startFade();
			}
		}

		if (this.isFading) {
			float fadeActualTime = (this.time - this.fadeStartTime) + tickDelta;

			// 增加 runtimeFadeTicks <= 0 的安全检查，防止除以 0
			float rawProgress = this.runtimeFadeTicks > 0
					? Math.max(0.0f, Math.min(fadeActualTime / this.runtimeFadeTicks, 1.0f))
					: 1.0f;

			this.currentProgress = this.transformer.apply(rawProgress);
		}
	}

	@Override
	public void tick(AnimationData state) {
		super.tick(state);
		this.time++;

		// 触发清理时使用 runtimeFadeTicks
		if (this.isFading && (this.time - this.fadeStartTime) >= this.runtimeFadeTicks) {
			if (this.getController() != null) {
				this.getController().triggerAnimation(RawAnimation.begin());
			}
		}
	}

	@Override
	public void get3DTransform(@NotNull PlayerAnimBone bone) {
		if (this.isSnapshotting) {
			super.get3DTransform(bone);
			return;
		}

		if (this.isFading && this.currentProgress >= 1.0f) {
			return;
		}

		if (this.isFading && this.snapshot != null) {
			PlayerAnimBone snapshotBone = this.reusableBones.get(bone.getName());
			if (snapshotBone != null) {
				snapshotBone.copyOtherBone(bone);
				this.snapshot.get3DTransform(snapshotBone);
				bone.scale(this.currentProgress).add(snapshotBone.scale(1.0f - this.currentProgress));
				return;
			}
		}

		super.get3DTransform(bone);
	}

	@Override
	public boolean canRemove() {
		return this.isFading && this.currentProgress >= 1.0f;
	}

	private void startFade() {
		this.isFading = true;
		this.fadeStartTime = this.time;

		if (this.getController() instanceof ParkourAnimationController snapshotController) {
			Map<String, AdvancedPlayerAnimBone> bones = snapshotController.getBone();
			int boneCount = bones.size();
			this.reusableBones = new HashMap<>(boneCount);
			Map<String, ToggleablePlayerAnimBone> snapshots = new HashMap<>(boneCount);

			this.isSnapshotting = true;
			try {
				for (PlayerAnimBone baseBone : bones.values()) {
					String boneName = baseBone.getName();
					this.reusableBones.put(boneName, new PlayerAnimBone(boneName));

					PlayerAnimBone snapshotBone = new PlayerAnimBone(boneName);
					snapshotController.get3DTransform(snapshotBone);

					snapshotBone.rotation.x = wrapRadian(snapshotBone.rotation.x);
					snapshotBone.rotation.y = wrapRadian(snapshotBone.rotation.y);
					snapshotBone.rotation.z = wrapRadian(snapshotBone.rotation.z);

					snapshots.put(boneName, new ToggleablePlayerAnimBone(snapshotBone));
				}
				this.snapshot = new AnimationSnapshot(snapshots);
			} finally {
				this.isSnapshotting = false;
			}
		}
	}

	private float getAnimationLength() {
		if (getController() instanceof AnimationController controller && controller.getCurrentAnimation() != null) {
			return (float) controller.getCurrentAnimation().animation().length();
		}
		return 0.0f;
	}

	/**
	 * 将弧度限制在 [-π, π] 范围内
	 */
	private float wrapRadian(float radian) {
		float pi2 = (float) (Math.PI * 2);
		radian = radian % pi2;
		if (radian >= Math.PI) radian -= pi2;
		if (radian < -Math.PI) radian += pi2;
		return radian;
	}
}
