package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 跑酷方块标签定义。
 *
 * <p>通过数据包动态配置哪些方块纳入跑酷系统的语义分类：
 * <ul>
 *   <li>{@code climbable} - 可攀爬方块（扩展原版梯子/藤蔓概念）</li>
 *   <li>{@code common_ignored_blocks} - 墙体检测中忽略的方块（如草、花等装饰性方块）</li>
 *   <li>{@code scaffolding_blocks} - 脚手架类方块，垂挂可附着</li>
 * </ul>
 *
 * <p>标签在 {@code data/parkour/tags/blocks/} 下通过 JSON 文件定义。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourTags {
	private static TagKey<Block> createBlockTag(String name) {
		return TagKey.create(Registries.BLOCK, ParkourMod.prefix(name));
	}


	public static class Blocks {
		public static final TagKey<Block> CLIMBABLE = createBlockTag("climbable");
		public static final TagKey<Block> COMMON_IGNORED_BLOCKS =
				createBlockTag("common_ignored_blocks");
		public static final TagKey<Block> SCAFFOLDING_BLOCKS =
				createBlockTag("scaffolding_blocks");
	}
}
