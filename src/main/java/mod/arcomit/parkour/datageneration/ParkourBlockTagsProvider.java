package mod.arcomit.parkour.datageneration;

import mod.arcomit.parkour.content.init.ParkourTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * 数据生成阶段自动收集可攀爬方块，生成方块标签JSON。
 * <p>
 * 将栅栏门、锁链、末地烛及所有原版栅栏归入自定义的{@code CLIMBABLE}标签， 供游戏内攀爬逻辑查询。同时生成脚手架方块标签和通用忽略方块标签。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourBlockTagsProvider extends BlockTagsProvider {
	public ParkourBlockTagsProvider(PackOutput output,
			CompletableFuture<HolderLookup.Provider> lookupProvider, String modId,
			@Nullable ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, modId, existingFileHelper);
	}

	private static boolean isCrossCollisionBlock(Block block) {
		return block instanceof CrossCollisionBlock;
	}

	private static boolean isNotFullRotatedPillarBlock(Block block) {
		return block instanceof ChainBlock;
	}

	private static boolean isEndRodBlock(Block block) {
		return block instanceof EndRodBlock;
	}

	@Override
	protected void addTags(HolderLookup.@NotNull Provider provider) {
		BuiltInRegistries.BLOCK.stream().filter(block -> isCrossCollisionBlock(
				block) || isNotFullRotatedPillarBlock(block) || isEndRodBlock(
				block)).forEach(this.tag(ParkourTags.Blocks.CLIMBABLE)::add);
		this.tag(ParkourTags.Blocks.CLIMBABLE).addTag(BlockTags.FENCES);
		this.tag(ParkourTags.Blocks.COMMON_IGNORED_BLOCKS)
				.addTag(ParkourTags.Blocks.CLIMBABLE);
		this.tag(ParkourTags.Blocks.COMMON_IGNORED_BLOCKS).addTag(BlockTags.CLIMBABLE);
		this.tag(ParkourTags.Blocks.SCAFFOLDING_BLOCKS).add(Blocks.SCAFFOLDING);
	}
}
