package mod.arcomit.parkour.content.behavior.slide.client.animation.player;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 滑铲动画的骨骼数学计算层，每帧计算头部偏航差值和身体的左右晃动偏移。
 * <p>
 * 头部：计算headYaw和bodyYaw的差值，让头部在滑铲时保持独立朝向。 身体：基于正弦波的周期性横向抖动，周期约0.28秒，模拟滑行时的不规则晃动感。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SlideAnimMath {
	/**
	 * 计算当前帧的滑铲动画数据。
	 *
	 * @param player      目标玩家
	 * @param state       动画状态（当前仅占位，为未来扩展预留）
	 * @param partialTick 部分tick值（范围 [0, 1)）
	 * @return 帧数据，包含头部偏航角和身体X轴偏移
	 */
	public static SlideFrameData calculate(Player player, SlideAnimState state,
			float partialTick) {
		SlideFrameData data = new SlideFrameData();

		// 1. 计算头部 Yaw 的差值 (左右看)
		float headYaw = Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot());
		float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
		data.headYawRad = (headYaw - bodyYaw) * Mth.DEG_TO_RAD;

		// Body shift logic
		float smoothTick = player.tickCount + partialTick;
		float timeInSeconds = smoothTick / 20.0f;
		// 计算角频率 (Omega)。公式是：2 * PI / 周期时间
		// 0.28 秒一个完整循环
		float omega = (float) (2 * Math.PI / 0.28f);
		// 晃动幅度 (像素格)
		float amplitude = 0.2f;
		data.bodyPosX = Mth.sin(timeInSeconds * omega) * amplitude;

		return data;
	}

	/**
	 * 单帧的滑铲骨骼目标数据。
	 */
	public static class SlideFrameData {
		/** 头部相对于身体的Y轴偏航角（弧度） */
		public float headYawRad;
		/** 身体X轴偏移（格），用于横向晃动效果 */
		public float bodyPosX;
	}
}
