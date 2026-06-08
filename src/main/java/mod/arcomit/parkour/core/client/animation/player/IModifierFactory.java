package mod.arcomit.parkour.core.client.animation.player;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 姿态 Modifier 的构造工厂。
 * <p>
 * 在播放状态动画前调用，允许向动画控制器注入自定义的关节约束和姿态调整器（如限制挥臂范围、
 * 调整躯干倾斜角度）。每个跑酷状态可注册独立的工厂实现。
 * <p>
 * 实现类通常以 Lambda 表达式注册到 {@link ClientAnimationRegistry}。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface IModifierFactory {
	/**
	 * 向指定动画控制器附加姿态调整器。
	 * <p>
	 * 副效应：会直接调用控制器的 addModifier 系列方法修改其内部 Modifier 链。
	 *
	 * @param controller 当前状态动画的控制器，预置了基础动画，Modifier 将追加到其末尾
	 * @param player     目标客户端玩家，用于获取身体尺寸、装备等上下文字段
	 * @param state      玩家当前所处的跑酷状态实例，可从中读取状态专属配置（如滑铲高度参数）
	 * @param variant    动画变体索引，由状态的动画变体数据决定，从 0 开始
	 */
	void apply(PlayerAnimationController controller, AbstractClientPlayer player,
			IParkourState state, int variant);
}
