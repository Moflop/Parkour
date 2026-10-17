package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 跑酷设施的创造模式物品栏分页。 */
public class ParkourCreativeModeTabs {
	private static final DeferredRegister<CreativeModeTab> TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ParkourMod.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PARKOUR = TABS.register("parkour",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.parkour"))
					.icon(() -> new ItemStack(ParkourBlocks.HORIZONTAL_BAR.get()))
					.displayItems((parameters, output) -> output.accept(ParkourBlocks.HORIZONTAL_BAR.get()))
					.build());

	public static void register(IEventBus bus) {
		TABS.register(bus);
	}
}
