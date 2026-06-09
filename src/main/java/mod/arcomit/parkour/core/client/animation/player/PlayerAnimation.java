package mod.arcomit.parkour.core.client.animation.player;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 玩家动画的轻量级句柄，仅保存动画资源标识符。
 * <p>
 * 实际动画数据由 PAL ({@code Player Animation Library}) 管理， 本类作为跑酷模组与 PAL 之间的桥梁——通过 id 在 PAL
 * 的动画存储中定位具体的骨骼动画资源。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class PlayerAnimation {

	/**
	 * 动画资源标识符，指向 PAL 动画注册表中的具体动画。
	 */
	public final ResourceLocation id;

	/**
	 * @param id 动画资源标识符，不可为 null，需在 PAL 侧有对应的动画文件
	 */
	public PlayerAnimation(ResourceLocation id) {
		this.id = id;
	}
}
