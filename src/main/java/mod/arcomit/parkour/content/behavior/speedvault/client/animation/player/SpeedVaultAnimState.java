package mod.arcomit.parkour.content.behavior.speedvault.client.animation.player;

import net.minecraft.world.entity.player.Player;

/**
 * 负责维护 Speed Vault 动画独立的时间与状态更新。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SpeedVaultAnimState {
	/** 当前动画已播放的 tick 数 */
	public int currentTick = 0;
	/** 上一次更新时的玩家 tick 计数，用于防止同一 tick 内重复推进 */
	private int lastUpdateTick = -1;

	/**
	 * 每客户端帧推进动画计时。利用玩家 {@code tickCount} 去重， 同一游戏 tick 内只推进一次，保证动画速度与游戏时间同步。
	 *
	 * @param player 目标玩家，不能为 null
	 */
	public void tick(Player player) {
		if (player.tickCount != this.lastUpdateTick) {
			this.lastUpdateTick = player.tickCount;
			this.currentTick++;
		}
	}
}
