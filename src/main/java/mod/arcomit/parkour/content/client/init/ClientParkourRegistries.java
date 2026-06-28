package mod.arcomit.parkour.content.client.init;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.core.client.animation.player.PlayerAnimation;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-27
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientParkourRegistries {

	private static final ResourceKey<Registry<PlayerAnimation>> PLAYER_ANIMATION_REGISTRY_KEY =
			ResourceKey.createRegistryKey(ParkourMod.prefix("player_animation"));

	public static final Registry<PlayerAnimation> PLAYER_ANIMATION_REGISTRY =
			new RegistryBuilder<>(PLAYER_ANIMATION_REGISTRY_KEY).sync(false)
					.defaultKey(ParkourMod.prefix("empty")).create();

	@SubscribeEvent
	public static void registerRegistries(NewRegistryEvent event) {
		event.register(PLAYER_ANIMATION_REGISTRY);
	}
}
