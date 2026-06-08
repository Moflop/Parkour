package mod.arcomit.parkour.content.behavior.landingroll.client.animation.player;

import net.minecraft.world.entity.player.Player;

/**
 * 落地翻滚动画独立状态机，维护动画已播放的tick计数。
 * <p>
 * 采用tickCount去重机制：同一tick内多次调用tick不会重复递增。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class LandingRollAnimState {
	/** 动画已播放的累计tick数 */
	public int currentTick = 0;
	/** 上次tick调用时的玩家tickCount，用于防重复递增 */
	public int lastUpdateTick = -1;

	/**
	 * 每渲染帧推进一次动画计时，相同tick内重复调用无效。
	 */
	public void tick(Player player) {
		if (player.tickCount != this.lastUpdateTick) {
			this.lastUpdateTick = player.tickCount;
			this.currentTick++;
		}
	}
}
