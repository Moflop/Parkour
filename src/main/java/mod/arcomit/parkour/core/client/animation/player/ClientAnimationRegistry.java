package mod.arcomit.parkour.core.client.animation.player;

import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * 客户端动画资源注册表，统一管理三类动画资源的注册与查找。
 * <ul>
 *   <li><b>状态动画</b>：跑酷状态下的循环动画（如滑铲姿态），通过 variant 支持变体</li>
 *   <li><b>Modifier 工厂</b>：为状态动画附加姿态调整器的构造逻辑</li>
 *   <li><b>动作动画</b>：一次性触发的动作（如前空翻），按玩家实例生成 Modifier</li>
 * </ul>
 * 所有注册表均为静态共享，在模组初始化阶段填充。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientAnimationRegistry {

	private static final Map<Identifier, IntFunction<PlayerAnimation>> STATE_ANIMATIONS =
			new HashMap<>();
	private static final Map<Identifier, IModifierFactory> MODIFIER_FACTORIES = new HashMap<>();
	private static final Map<Identifier, Function<AbstractClientPlayer, AbstractModifier>>
			ACTION_MODIFIERS = new HashMap<>();

	/**
	 * 注册状态动画提供方。
	 *
	 * @param stateId      对应跑酷状态的注册表键
	 * @param animProvider 接受 variant 参数并返回对应 PlayerAnimation，variant 由当前状态的动画变体数据决定
	 */
	public static void registerStateAnimation(Identifier stateId,
			IntFunction<PlayerAnimation> animProvider) {
		STATE_ANIMATIONS.put(stateId, animProvider);
	}

	/**
	 * 注册状态的 Modifier 工厂，用于在播放状态动画前附加姿态调整。
	 *
	 * @param stateId 对应跑酷状态的注册表键
	 * @param factory 工厂实例，不可为 null
	 */
	public static void registerModifierFactory(Identifier stateId, IModifierFactory factory) {
		MODIFIER_FACTORIES.put(stateId, factory);
	}

	/**
	 * @param stateId 跑酷状态的注册表键
	 * @param variant 动画变体索引，由状态数据提供
	 * @return 对应变体的 PlayerAnimation，未注册时返回 null
	 */
	public static PlayerAnimation getAnimation(Identifier stateId, int variant) {
		IntFunction<PlayerAnimation> provider = STATE_ANIMATIONS.get(stateId);
		return provider != null ? provider.apply(variant) : null;
	}

	/**
	 * @param stateId 跑酷状态的注册表键
	 * @return 对应的 Modifier 工厂，未注册时返回 null
	 */
	public static IModifierFactory getModifierFactory(Identifier stateId) {
		return MODIFIER_FACTORIES.get(stateId);
	}

	/**
	 * 注册动作动画的 Modifier 构造工厂。
	 * <p>
	 * 区别于状态动画：动作 Modifier 按玩家实例生成， 因为动作可能需要根据玩家当前姿态动态调整关节约束。
	 *
	 * @param actionId 动作标识符，与动画文件名对应
	 * @param factory  接受玩家实例并返回对应的 Modifier，不可为 null
	 */
	public static void registerActionModifier(Identifier actionId,
			Function<AbstractClientPlayer, AbstractModifier> factory) {
		ACTION_MODIFIERS.put(actionId, factory);
	}

	/**
	 * @param actionId 动作标识符
	 * @param player   目标客户端玩家实例，不可为 null
	 * @return 为该玩家实例构造的 Modifier，未注册时返回 null
	 */
	public static AbstractModifier getActionModifier(Identifier actionId,
			AbstractClientPlayer player) {
		Function<AbstractClientPlayer, AbstractModifier> factory =
				ACTION_MODIFIERS.get(actionId);
		return factory != null ? factory.apply(player) : null;
	}
}
