package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.block.HorizontalBarBlock;
import mod.arcomit.parkour.content.item.HorizontalBarItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 跑酷设施的方块与对应物品注册。 */
public class ParkourBlocks {
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ParkourMod.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ParkourMod.MODID);
	public static final DeferredBlock<HorizontalBarBlock> HORIZONTAL_BAR = BLOCKS.register("horizontal_bar",
			() -> new HorizontalBarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
					.strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));

	public static void register(IEventBus bus) {
		ITEMS.register("horizontal_bar", () -> new HorizontalBarItem(HORIZONTAL_BAR.get(), new Item.Properties()));
		BLOCKS.register(bus);
		ITEMS.register(bus);
		bus.addListener(ParkourBlocks::addCreativeItems);
	}

	private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(HORIZONTAL_BAR);
	}
}
