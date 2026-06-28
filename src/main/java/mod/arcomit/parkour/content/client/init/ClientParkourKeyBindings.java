package mod.arcomit.parkour.content.client.init;

import com.mojang.blaze3d.platform.InputConstants;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.client.input.ParkourKeyMapping;
import mod.arcomit.parkour.core.input.ParkourInputActions;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
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
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientParkourKeyBindings {
	private static final String CATEGORY = "key.categories." + ParkourMod.MODID;

	/** 滑铲/翻滚键，默认 V，触发 {@link ParkourInputActions#SLIDE} */
	public static final ParkourKeyMapping SLIDE_KEY =
			new ParkourKeyMapping("key." + ParkourMod.MODID + ".roll_slide",
					InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY,
					ParkourInputActions.SLIDE);

	/** 开启/关闭简单跑酷模式 */
	public static final KeyMapping ENABLE_PARKOUR_KEY = new KeyMapping(
			"key." + ParkourMod.MODID + ".enable_parkour",
			KeyConflictContext.IN_GAME,
			KeyModifier.CONTROL,
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_P,
			CATEGORY
	);

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(SLIDE_KEY);
		event.register(ENABLE_PARKOUR_KEY);
	}
}
