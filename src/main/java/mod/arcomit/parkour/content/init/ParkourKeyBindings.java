package mod.arcomit.parkour.content.init;

import com.mojang.blaze3d.platform.InputConstants;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.client.input.ParkourKeyMapping;
import mod.arcomit.parkour.core.input.ParkourInputActions;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端跑酷按键绑定管理。
 *
 * <p>定义并注册所有自定义按键映射（当前仅滑铲键 V），在客户端初始化时通过
 * {@link RegisterKeyMappingsEvent} 事件注册到原版按键系统中。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ParkourKeyBindings {
	private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ParkourMod.prefix("default"));

	/** 滑铲/翻滚键，默认 V，触发 {@link ParkourInputActions#SLIDE} */
	public static final ParkourKeyMapping SLIDE_KEY =
			new ParkourKeyMapping("key." + ParkourMod.MODID + ".roll_slide",
					InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY,
					ParkourInputActions.SLIDE);

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.registerCategory(CATEGORY);
		event.register(SLIDE_KEY);
	}
}
