package mod.arcomit.parkour;

import com.mojang.logging.LogUtils;
import mod.arcomit.parkour.content.init.ParkourAttachmentTypes;
import mod.arcomit.parkour.content.init.ParkourSounds;
import mod.arcomit.parkour.content.init.ParkourStates;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * 跑酷模组主入口。
 *
 * <p>NeoForge {@code @Mod} 注解标记的入口点，负责：
 * <ul>
 *   <li>注册服务器配置（通过 {@link ParkourConfig}）</li>
 *   <li>注册 NeoForge Attachment 类型（{@link ParkourAttachmentTypes}）</li>
 *   <li>注册跑酷状态到自定义注册表（{@link ParkourStates}）</li>
 *   <li>注册音效事件（{@link ParkourSounds}）</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@Mod(ParkourMod.MODID)
public class ParkourMod {
	public static final String MODID = "parkour";
	public static final Logger LOGGER = LogUtils.getLogger();

	public ParkourMod(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.SERVER, ParkourConfig.SPEC);
		ParkourAttachmentTypes.register(modEventBus);
		ParkourStates.register(modEventBus);
		ParkourSounds.register(modEventBus);
		LOGGER.info("Parkour mod initialized!");
	}

	/**
	 * 构造模组命名空间下的 {@link Identifier}。
	 *
	 * @param path 资源路径片段，如 "slide" 生成 "parkour:slide"
	 * @return 带有本模组命名空间的资源位置
	 */
	public static Identifier prefix(String path) {
		return Identifier.fromNamespaceAndPath(MODID, path);
	}
}
