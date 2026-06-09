package mod.arcomit.parkour.content.behavior.speedvault.client.animation.player;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 负责速过动画的骨骼计算，生成该帧所有骨骼的偏移数据。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SpeedVaultAnimMath {

	/**
	 * 逐帧计算速过动画的所有骨骼旋转数据。
	 *
	 * <p>头部旋转完全由代码控制；四肢在溢出阶段（最后 {@code fadeOutDuration} tick）
	 * 逐渐交还给原版走路动画的摆动值，实现动画到原版的平滑过渡。
	 *
	 * @param player          目标玩家，不能为 null
	 * @param state           动画内部计时状态，不能为 null
	 * @param partialTick     当前帧的部分 tick 插值因子，范围 [0, 1)
	 * @param totalDuration   动画总持续 tick 数，大于 0
	 * @param fadeOutDuration 溢出过渡阶段的 tick 数，大于 0
	 * @return 当前帧的完整骨骼数据，永远不会为 null
	 */
	public static FrameData calculate(Player player, SpeedVaultAnimState state,
			float partialTick, int totalDuration, int fadeOutDuration) {
		FrameData data = new FrameData();

		// 头部计算
		float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
		float headYaw = Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot());
		float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
		float netYaw = headYaw - bodyYaw;

		data.headRotX = pitch * Mth.DEG_TO_RAD;
		data.headRotY = netYaw * Mth.DEG_TO_RAD;
		data.headRotZ = 0f;

		// 四肢原版接管计算
		float smoothTime = state.currentTick + partialTick;
		float fadeStartTime = totalDuration - fadeOutDuration;

		if (smoothTime >= fadeStartTime) {
			data.applyVanillaOverride = true;
			data.vanillaWeight =
					Mth.clamp((smoothTime - fadeStartTime) / fadeOutDuration,
							0f, 1f);

			float swingAmount = player.walkAnimation.speed(partialTick);
			float swingProgress = player.walkAnimation.position(partialTick);

			float legSwing = Mth.cos(swingProgress * 0.6662f) * 1.4f * swingAmount;
			float armSwing =
					Mth.cos(swingProgress * 0.6662f + (float) Math.PI) * 2.0f * swingAmount * 0.5f;

			data.leftLegRotX = -legSwing;
			data.rightLegRotX = legSwing;
			data.leftArmRotX = -armSwing;
			data.rightArmRotX = armSwing;
		} else {
			data.applyVanillaOverride = false;
		}

		return data;
	}


	/**
	 * 单帧骨骼数据容器。
	 *
	 * <p>头部旋转直接赋值；当 {@code applyVanillaOverride} 为 true 时，
	 * 四肢以 {@code vanillaWeight} 为权重向原版走路动画的摆幅插值。
	 */
	public static class FrameData {
		/** 头部绕 X/Y/Z 轴旋转（弧度） */
		public float headRotX, headRotY, headRotZ;
		/** 左右臂绕 X 轴旋转（弧度） */
		public float leftArmRotX, rightArmRotX;
		/** 左右腿绕 X 轴旋转（弧度） */
		public float leftLegRotX, rightLegRotX;

		/** 是否需要覆盖原版四肢动画 */
		public boolean applyVanillaOverride;
		/** 向原版动画插值的权重，范围 [0, 1] */
		public float vanillaWeight;
	}
}
