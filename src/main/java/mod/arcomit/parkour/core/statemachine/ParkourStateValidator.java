package mod.arcomit.parkour.core.statemachine;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 状态合法性验证器，每 tick 检查当前状态是否仍满足维持条件。
 *
 * <p>当 {@link IParkourState#isValid} 返回 false 时，状态机将强制回退到默认状态。
 * 这是防止玩家"卡"在非法状态中的最后一道防线（例如落地后仍处于空中动作状态）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourStateValidator {

	/**
	 * 检查当前状态是否仍然合法。
	 *
	 * <p>当前状态为 null 时视为无效，会触发回退到默认状态。
	 *
	 * @param player  待校验的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @return true 表示可以继续维持当前状态；false 表示状态非法，需要回退
	 */
	public static boolean isCurrentStateValid(@NotNull Player player,
			@NotNull ParkourContext context) {
		IParkourState currentState = context.state().getState();
		return currentState != null && currentState.isValid(player, context);
	}
}
