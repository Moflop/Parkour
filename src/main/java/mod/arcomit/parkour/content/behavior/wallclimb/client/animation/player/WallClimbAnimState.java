package mod.arcomit.parkour.content.behavior.wallclimb.client.animation.player;

import mod.arcomit.parkour.ParkourConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 负责墙攀的动画速度状态机运算。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallClimbAnimState {
	/** 基准爬升速度，用于归一化实际速度到动画速度倍率 */
	private static final float BASE_SPEED = 0.2f;

	/**
	 * 根据玩家垂直位移动态计算动画播放速度。
	 *
	 * <p>当玩家产生有效垂直位移（|dy| &gt; 0.01）时，将配置的爬墙速度
	 * 相对基准速度归一化后作为目标动画速度；静止时目标速度为 0。 返回时通过 0.4 权重插值实现平滑过渡，避免速度突变导致动画抖动。
	 *
	 * @param player               目标玩家，不能为 null
	 * @param currentModifierSpeed 当前动画速度倍率
	 * @return 平滑插值后的动画速度倍率，静立时趋近于 0
	 */
	public float calculateLerpedSpeed(Player player, float currentModifierSpeed) {
		double dy = player.getY() - player.yo;
		float targetSpeed = 0f;

		// 只要产生了一定程度的垂直位移，就开始播放动画
		if (dy > 0.01 || dy < -0.01) {
			float currentWallClimbSpeed = (float) ParkourConfig.wallClimbSpeed;
			targetSpeed = currentWallClimbSpeed / BASE_SPEED;
		}

		// 引入平滑过渡 (Lerp)
		return Mth.lerp(0.4f, currentModifierSpeed, targetSpeed);
	}
}
