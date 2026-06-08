package mod.arcomit.parkour.content.behavior.slide.client.animation.player;

import net.minecraft.world.entity.player.Player;

/**
 * 滑铲动画状态机 —— 当前为占位结构，为未来可能的tick相关动画逻辑预留。
 * <p>
 * 保持与其他行为（如Armhang）一致的架构风格，方便后续统一扩展。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class SlideAnimState {
	/**
	 * 每帧推进动画状态。当前无具体逻辑，仅为未来扩展预留接口。
	 */
	public void tick(Player player) {
	}
}
