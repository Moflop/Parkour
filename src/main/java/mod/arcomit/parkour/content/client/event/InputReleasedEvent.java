package mod.arcomit.parkour.content.client.event;

import lombok.Getter;
import mod.arcomit.parkour.content.client.input.ParkourKeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.Event;

/**
 * 按键释放事件，在玩家松开跑酷自定义按键时触发。
 *
 * <p>携带 {@code pressedTicks} 表示该次按住的持续 tick 数，供需要区分
 * "短按"和"长按"的跑酷动作（如滑铲 vs 翻滚）使用。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@Getter
public class InputReleasedEvent extends Event {
	private final ParkourKeyMapping keyMapping;
	private final int pressedTicks;

	public InputReleasedEvent(ParkourKeyMapping keyMapping, int pressedTicks) {
		this.keyMapping = keyMapping;
		this.pressedTicks = pressedTicks;
	}
}
