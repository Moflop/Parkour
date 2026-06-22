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
	}
}
