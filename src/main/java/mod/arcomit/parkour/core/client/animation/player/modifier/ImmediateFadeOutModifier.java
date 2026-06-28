package mod.arcomit.parkour.core.client.animation.player.modifier;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.AnimationSnapshot;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.AdvancedBoneSnapshot;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.bones.IBoneEnabled;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import mod.arcomit.parkour.core.client.animation.player.ParkourAnimationController;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-27
 */
public class ImmediateFadeOutModifier extends AbstractModifier {
	private final Map<String, PlayerAnimBone> reusableBones;
	private final AnimationSnapshot snapshot;
	private final int fadeTicks;

	private final Float2FloatFunction transformer = EasingType.LINEAR.buildTransformer(null);

	private int time = 0;
	private float currentProgress = 0.0f;

	public ImmediateFadeOutModifier(ParkourAnimationController snapshotController, int fadeTicks) {
		Map<String, AdvancedPlayerAnimBone> bone = snapshotController.getBone();
		int boneCount = bone.size();
		this.reusableBones = new HashMap<>(boneCount);
		Map<String, AdvancedBoneSnapshot> snapshots = new HashMap<>(boneCount);

		for (PlayerAnimBone baseBone : bone.values()) {
			String boneName = baseBone.getName();
			this.reusableBones.put(boneName, new PlayerAnimBone(boneName)); // 创建全新骨骼

			PlayerAnimBone snapshotBone = new PlayerAnimBone(boneName);
			snapshotController.get3DTransform(snapshotBone);

			snapshotBone.rotX = wrapRadian(snapshotBone.rotX);
			snapshotBone.rotY = wrapRadian(snapshotBone.rotY);
			snapshotBone.rotZ = wrapRadian(snapshotBone.rotZ);

			AdvancedBoneSnapshot snap = new AdvancedBoneSnapshot(snapshotBone);

			// 核心修复：从 baseBone 同步通道状态
			if (baseBone instanceof IBoneEnabled enabledBone) {
				snap.positionXEnabled = enabledBone.isPositionXEnabled();
				snap.positionYEnabled = enabledBone.isPositionYEnabled();
				snap.positionZEnabled = enabledBone.isPositionZEnabled();
				snap.rotXEnabled = enabledBone.isRotXEnabled();
				snap.rotYEnabled = enabledBone.isRotYEnabled();
				snap.rotZEnabled = enabledBone.isRotZEnabled();
				snap.scaleXEnabled = enabledBone.isScaleXEnabled();
				snap.scaleYEnabled = enabledBone.isScaleYEnabled();
				snap.scaleZEnabled = enabledBone.isScaleZEnabled();
				snap.bendEnabled = enabledBone.isBendEnabled();
			} else {
				// 兜底处理
				snap.positionXEnabled = snap.positionYEnabled = snap.positionZEnabled = true;
				snap.rotXEnabled = snap.rotYEnabled = snap.rotZEnabled = true;
				snap.scaleXEnabled = snap.scaleYEnabled = snap.scaleZEnabled = true;
				snap.bendEnabled = true;
			}

			snapshots.put(boneName, snap);
		}
		this.snapshot = new AnimationSnapshot(snapshots);
		snapshotController.removeAllModifiers();

		this.fadeTicks = fadeTicks;
	}

	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		float tickDelta = state.getPartialTick();

		float actualTime = this.time + tickDelta;
		float rawProgress = Math.min(actualTime / this.fadeTicks, 1.0f);

		this.currentProgress = this.transformer.apply(rawProgress);
	}

	@Override
	public void tick(AnimationData state) {
		super.tick(state);
		this.time++;
		if (this.time >= this.fadeTicks) {
			if (this.getController() != null) {
				this.getController().triggerAnimation(RawAnimation.begin());
			}
		}
	}

	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		PlayerAnimBone snapshotBone = this.reusableBones.get(bone.getName());
		if (snapshotBone == null) {
			return bone;
		}

		snapshotBone.copyOtherBone(bone);
		this.snapshot.get3DTransform(snapshotBone);

		return bone.scale(this.currentProgress)
				.add(snapshotBone.scale(1.0f - this.currentProgress));
	}

	/**
	 * 将弧度限制在 [-π, π] 范围内
	 */
	private float wrapRadian(float radian) {
		float pi2 = (float) (Math.PI * 2);
		radian = radian % pi2;
		if (radian >= Math.PI)
			radian -= pi2;
		if (radian < -Math.PI)
			radian += pi2;
		return radian;
	}
}
