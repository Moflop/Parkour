package mod.arcomit.parkour;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端跑酷配置。
 *
 * <p>包含仅影响玩家本地偏好的设置（如跑酷开关）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientParkourConfig {
	public static final ModConfigSpec SPEC;

	public static ModConfigSpec.BooleanValue ENABLE_PARKOUR;
	public static boolean enableParkour;
	public static ModConfigSpec.BooleanValue BAR_SWING_CAMERA;
	public static boolean barSwingCamera;
	public static ModConfigSpec.BooleanValue ENABLE_TUTORIAL_PROMPTS;
	public static boolean enableTutorialPrompts;
	public static ModConfigSpec.BooleanValue TUTORIAL_HUD_POSITION_INITIALIZED;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		builder.comment("Client-side settings let you change local preferences.")
				.translation(ParkourMod.MODID + ".configuration.client_settings")
				.push("client_settings");

		// 跑酷全局本地开关
		ENABLE_PARKOUR = builder
				.comment("Enable or disable parkour abilities locally (default: true)")
				.translation(ParkourMod.MODID + ".configuration.client_settings.enable_parkour")
				.define("enableParkour", true);

		BAR_SWING_CAMERA = builder.comment("Subtle first-person pitch while swinging on bars (default: true)")
				.translation(ParkourMod.MODID + ".configuration.client_settings.bar_swing_camera")
				.define("barSwingCamera", true);
		ENABLE_TUTORIAL_PROMPTS = builder
				.comment("Show contextual parkour key hints when Smart Key Prompts is installed (default: true)")
				.translation(ParkourMod.MODID + ".configuration.client_settings.enable_tutorial_prompts")
				.define("enableTutorialPrompts", true);
		TUTORIAL_HUD_POSITION_INITIALIZED = builder
				.comment("Tracks the one-time Smart Key Prompts default-position setup; later position choices are preserved.")
				.translation(ParkourMod.MODID + ".configuration.client_settings.tutorial_hud_position_initialized")
				.define("tutorialHudPositionInitialized", false);
		builder.pop();
		SPEC = builder.build();
	}

	/**
	 * 切换本地跑酷开关状态。
	 * 包含修改底层 Config 树、保存至磁盘以及同步更新内存静态缓存。
	 *
	 * @return 切换后的新状态
	 */
	public static boolean toggleParkour() {
		boolean newState = !enableParkour;
		ENABLE_PARKOUR.set(newState);
		ENABLE_PARKOUR.save();
		enableParkour = newState;
		return newState;
	}

	/** Local tutorial preference; no server state or ability is changed. */
	public static boolean toggleTutorialPrompts() {
		boolean newState = !enableTutorialPrompts;
		ENABLE_TUTORIAL_PROMPTS.set(newState);
		ENABLE_TUTORIAL_PROMPTS.save();
		enableTutorialPrompts = newState;
		return newState;
	}

	/**
	 * 配置加载/重载时将最新值刷新到静态原始类型字段。
	 */
	@SubscribeEvent
	static void onLoad(final ModConfigEvent event) {
		if (event instanceof ModConfigEvent.Unloading)
			return;
		if (event.getConfig().getSpec() != SPEC)
			return;

		enableParkour = ENABLE_PARKOUR.get();
		barSwingCamera = BAR_SWING_CAMERA.get();
		enableTutorialPrompts = ENABLE_TUTORIAL_PROMPTS.get();
	}
}
