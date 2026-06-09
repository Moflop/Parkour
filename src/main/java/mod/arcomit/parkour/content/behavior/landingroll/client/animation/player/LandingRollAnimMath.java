package mod.arcomit.parkour.content.behavior.landingroll.client.animation.player;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 落地翻滚动画的骨骼数学计算层，每帧调用一次生成所有骨骼的偏移数据。
 * <p>
 * 核心逻辑：在动画退出过渡期（fadeOut）内，计算原版骨骼旋转目标（头部朝向、 四肢步态摆动），再由外部修改器按权重线性插值到这些目标，实现翻滚结束时的平滑过渡。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class LandingRollAnimMath {

	/**
	 * 计算当前帧的骨骼数据。
	 *
	 * @param player          目标玩家
	 * @param state           动画状态，记录已播放的tick数
	 * @param partialTick     部分tick值，用于平滑插值（范围 [0, 1)）
	 * @param totalDuration   翻滚动画总时长（刻）
	 * @param fadeOutDuration 过渡消退时长（刻）
	 * @return 帧数据，若当前时间尚未进入消退阶段则{@link FrameData#shouldApplyVanilla}为false
	 */
	public static FrameData calculate(Player player, LandingRollAnimState state,
			float partialTick, int totalDuration, int fadeOutDuration) {
		FrameData data = new FrameData();
		float smoothTime = state.currentTick + partialTick;
		float fadeStartTime = totalDuration - fadeOutDuration;

		if (smoothTime < fadeStartTime) {
			data.shouldApplyVanilla = false;
			return data;
		}

		data.shouldApplyVanilla = true;
		data.vanillaWeight =
				Mth.clamp((smoothTime - fadeStartTime) / fadeOutDuration, 0f, 1f);

		// 头部的目标旋转 (原版)
		float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
		float headYaw = Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot());
		float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
		float netYaw = headYaw - bodyYaw;

		data.headTargetRotX = pitch * Mth.DEG_TO_RAD;
		data.headTargetRotY = netYaw * Mth.DEG_TO_RAD;
		data.headTargetRotZ = 0f;

		// 四肢步态摆动 (原版)
		float swingAmount = player.walkAnimation.speed(partialTick);
		float swingProgress = player.walkAnimation.position(partialTick);

		float legSwing = Mth.cos(swingProgress * 0.6662f) * 1.4f * swingAmount;
		float armSwing =
				Mth.cos(swingProgress * 0.6662f + (float) Math.PI) * 2.0f * swingAmount * 0.5f;

		data.leftLegTargetRotX = -legSwing;
		data.rightLegTargetRotX = legSwing;
		data.leftArmTargetRotX = -armSwing;
		data.rightArmTargetRotX = armSwing;

		return data;
	}

	/**
	 * 弧度的最短路径线性插值，确保旋转角始终在 [-π, π] 区间内。
	 *
	 * @param delta 插值系数（范围 [0, 1]）
	 * @param start 起始弧度
	 * @param end   目标弧度
	 * @return 最短路径上的插值结果
	 */
	public static float rotLerpRadians(float delta, float start, float end) {
		float diff = end - start;
		while (diff < -(float) Math.PI) {
			diff += (float) Math.PI * 2f;
		}
		while (diff >= (float) Math.PI) {
			diff -= (float) Math.PI * 2f;
		}
		return start + delta * diff;
	}

	/**
	 * 单帧的骨骼目标数据 —— 由{@link #calculate}填充，供{@link LandingRollPlayerAnimModifier}消费。
	 */
	public static class FrameData {
		/** 是否应应用原版骨骼插值（仅在消退阶段为true） */
		public boolean shouldApplyVanilla;
		/** 原版混合权重，0=完全自定义动画，1=完全原版动画 */
		public float vanillaWeight;
		public float headTargetRotX, headTargetRotY, headTargetRotZ;
		public float leftLegTargetRotX, rightLegTargetRotX;
		public float leftArmTargetRotX, rightArmTargetRotX;
	}
}
