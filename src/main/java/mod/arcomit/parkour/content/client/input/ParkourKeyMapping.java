package mod.arcomit.parkour.content.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import mod.arcomit.parkour.content.client.event.InputJustPressedEvent;
import mod.arcomit.parkour.content.client.event.InputReleasedEvent;
import mod.arcomit.parkour.core.input.ParkourInputActions;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 跑酷自定义按键映射。
 *
 * <p>扩展原版 {@link KeyMapping}，在按键按下/释放时自动向 NeoForge 事件总线
 * 投递 {@link InputJustPressedEvent}（刚按下的瞬间触发一次）和 {@link InputReleasedEvent}（包含持续按住的总 tick 数）。 持续按住时每
 * tick 递增 {@code pressedTicks}，松开后清零。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourKeyMapping extends KeyMapping {
	/** 当前这次按下的持续 tick 数，松开后清零 */
	private int pressedTicks;
	/** 该按键对应的跑酷输入动作枚举 */
	@Getter
	private ParkourInputActions inputAction = ParkourInputActions.NONE;

	/**
	 * @param name        按键的翻译键名，如 "key.parkour.roll_slide"
	 * @param inputType   输入设备类型（键盘/鼠标）
	 * @param keyCode     GLFW 按键码
	 * @param category    按键分组类别的翻译键
	 * @param inputAction 对应的跑酷输入动作，不为 null
	 */
	public ParkourKeyMapping(String name, InputConstants.Type inputType, int keyCode,
			String category, ParkourInputActions inputAction) {
		super(name, inputType, keyCode, KeyMapping.Category.GAMEPLAY, 0);
		this.inputAction = inputAction;
	}

	/**
	 * 重写按键按下/释放行为，在调用父类逻辑之前向 NeoForge 事件总线投递输入事件。
	 *
	 * <p>持续按住时每 tick 递增内部计数器（投递 {@code InputJustPressedEvent} 仅触发一次），
	 * 释放时投递 {@code InputReleasedEvent} 并携带总按住时长，然后清零计数器。
	 *
	 * @param value true 表示按下，false 表示释放
	 */
	@Override
	public void setDown(boolean value) {
		if (this.isDown() == value) {
			if (this.isDown()) {
				pressedTicks++;
			}
			return;
		}
		if (value) {
			NeoForge.EVENT_BUS.post(new InputJustPressedEvent(this));
		} else {
			NeoForge.EVENT_BUS.post(new InputReleasedEvent(this, pressedTicks));
			pressedTicks = 0;
		}
		super.setDown(value);
	}
}
