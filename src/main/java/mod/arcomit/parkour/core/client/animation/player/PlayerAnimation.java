package mod.arcomit.parkour.core.client.animation.player;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 玩家动画的轻量级句柄，仅保存动画资源标识符。
 * <p>
 * 实际动画数据由 PAL ({@code Player Animation Library}) 管理， 本类作为跑酷模组与 PAL 之间的桥梁——通过 id 在 PAL
 * 的动画存储中定位具体的骨骼动画资源。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class PlayerAnimation {

	/**
	 * 动画资源标识符，指向 PAL 动画注册表中的具体动画。
	 */
	public final Identifier animName;

	public final int fadeInTicks;

	public final int fadeOutTicks;

	public final PlayerAnimModifierProvider modifierProvider;

	/**
	 * @param animName 动画资源标识符，不可为 null，需在 PAL 侧有对应的动画文件
	 * @param fadeInTicks 淡入过渡时长，单位 tick
	 * @param fadeOutTicks 淡出过渡时长，单位 tick
	 */
	public PlayerAnimation(Identifier animName, int fadeInTicks, int fadeOutTicks) {
		this(animName, fadeInTicks, fadeOutTicks, (player, variant) -> List.of());
	}

	/**
	 * 全参数构造器（支持传入自定义 Modifier 方法）
	 */
	public PlayerAnimation(Identifier animName, int fadeInTicks, int fadeOutTicks, PlayerAnimModifierProvider modifierProvider) {
		this.animName = animName;
		this.fadeInTicks = fadeInTicks;
		this.fadeOutTicks = fadeOutTicks;
		this.modifierProvider = modifierProvider;
	}

	// 包装一个快捷调用的方法
	public List<AbstractModifier> getAnimationModifiers(Player player, int variant) {
		return this.modifierProvider.getModifiers(player, variant);
	}
}
