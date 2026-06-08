package mod.arcomit.parkour.content.client.event;

import lombok.Getter;
import mod.arcomit.parkour.content.client.input.ParkourKeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.Event;

/**
 * 按键刚按下事件，在玩家按下跑酷自定义按键的瞬间触发一次（非持续按住）。
 *
 * <p>仅在按键状态从"未按下"变为"按下"的那个 tick 触发，持续按住不会重复投递。
 * 与 {@link InputReleasedEvent} 配对使用。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@Getter
public class InputJustPressedEvent extends Event {
	private final ParkourKeyMapping keyMapping;

	public InputJustPressedEvent(ParkourKeyMapping keyMapping) {
		this.keyMapping = keyMapping;
	}
}
