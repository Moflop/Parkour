package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * 跑酷自定义注册表容器。
 *
 * <p>通过 {@link net.neoforged.neoforge.registries.NewRegistryEvent} 创建自定义注册表。
 * 当前包含一个同步型跑酷状态注册表，客户端和服务端均可通过该注册表查找状态实例。 注册表默认键指向 {@link ParkourStates#DEFAULT}，用于在查找失败时回退。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class ParkourRegistries {

	/** 跑酷状态自定义注册表的 ResourceKey */
	private static final ResourceKey<Registry<IParkourState>> PARKOUR_STATE_REGISTRY_KEY =
			ResourceKey.createRegistryKey(ParkourMod.prefix("parkour_state"));
	/** 跑酷状态注册表实例，同步型（客户端和服务端均可访问），默认键为 "parkour:default" */
	public static final Registry<IParkourState> PARKOUR_STATE_REGISTRY =
			new RegistryBuilder<>(PARKOUR_STATE_REGISTRY_KEY).sync(true)
					.defaultKey(ParkourMod.prefix("default")).create();

	/**
	 * 向 NeoForge 注册自定义注册表。通过 {@link NewRegistryEvent} 事件触发。
	 */
	@SubscribeEvent
	public static void registerRegistries(NewRegistryEvent event) {
		event.register(PARKOUR_STATE_REGISTRY);
	}
}
