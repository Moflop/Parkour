package mod.arcomit.parkour.datageneration.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.datageneration.ParkourBlockTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 在 {@link GatherDataEvent.Server} 时注入跑酷方块标签数据生成。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class DataGenerationHandler {

	@SubscribeEvent
	public static void gatherData(GatherDataEvent.Client event) {
		DataGenerator dataGenerator = event.getGenerator();
		PackOutput packOutput = dataGenerator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

		ParkourBlockTagsProvider blockTags =
				new ParkourBlockTagsProvider(packOutput, lookupProvider,
						ParkourMod.MODID);
		event.addProvider(blockTags);
	}
}
