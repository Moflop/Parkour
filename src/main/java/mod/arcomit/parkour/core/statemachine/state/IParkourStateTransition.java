package mod.arcomit.parkour.core.statemachine.state;

import mod.arcomit.parkour.content.event.LivingJumpCancellableEvent;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.input.ParkourInputActions;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * 状态转换规则接口，定义从当前状态切换到目标状态的触发条件。
 *
 * <p>支持五种触发方式：
 * <ul>
 *   <li><b>onTick</b> — 每 tick 评估，适用于双端一致的条件（如计时器到期）</li>
 *   <li><b>onInput</b> — 客户端按键触发，需要网络同步校验</li>
 *   <li><b>onLocalTick</b> — 客户端本地预测性转换，用于需要快速响应但最终由服务端确认的场景</li>
 *   <li><b>onJump</b> — 玩家跳跃事件触发，可能取消原版跳跃逻辑</li>
 *   <li><b>onFall</b> — 玩家摔落事件触发，可能取消原版摔落伤害</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public interface IParkourStateTransition {
	/**
	 * 构建一个基于 Tick 判断的状态转换规则，每帧在双端评估。
	 *
	 * @param targetState 目标状态的 Supplier，每次评估时惰性获取，不为 null
	 * @param condition   触发条件，入参为当前玩家和跑酷上下文，不为 null
	 * @return 配置好的转换规则实例，始终不为 null
	 */
	static IParkourStateTransition onTick(Supplier<IParkourState> targetState,
			BiPredicate<Player, ParkourContext> condition) {
		return new IParkourStateTransition() {
			@Override
			public IParkourState getTargetState() {
				return targetState.get();
			}

			@Override
			public boolean shouldTransitionOnTick(Player player,
					ParkourContext context) {
				return condition.test(player, context);
			}
		};
	}

	/**
	 * 构建一个基于按键输入的状态转换规则，当玩家按下指定按键时触发本地预测转换。
	 *
	 * @param targetState  目标状态的 Supplier，不为 null
	 * @param targetAction 触发此转换的按键动作枚举，不为 null
	 * @return 配置好的转换规则实例，始终不为 null
	 */
	static IParkourStateTransition onInput(Supplier<IParkourState> targetState,
			ParkourInputActions targetAction) {
		return new IParkourStateTransition() {
			@Override
			public IParkourState getTargetState() {
				return targetState.get();
			}

			@Override
			public boolean shouldTransitionOnInput(Player player,
					ParkourContext context, ParkourInputActions inputAction) {
				return inputAction == targetAction;
			}
		};
	}

	/**
	 * 构建一个基于按键输入、且需通过额外条件检查的状态转换规则。
	 *
	 * <p>只有按键匹配且额外条件同时成立时才触发转换，适用于需要体力/冷却等前置条件的按键动作。
	 *
	 * @param targetState  目标状态的 Supplier，不为 null
	 * @param targetAction 触发此转换的按键动作枚举，不为 null
	 * @param condition    额外前置条件，在按键匹配后进一步校验，不为 null
	 * @return 配置好的转换规则实例，始终不为 null
	 */
	static IParkourStateTransition onInput(Supplier<IParkourState> targetState,
			ParkourInputActions targetAction,
			BiPredicate<Player, ParkourContext> condition) {
		return new IParkourStateTransition() {
			@Override
			public IParkourState getTargetState() {
				return targetState.get();
			}

			@Override
			public boolean shouldTransitionOnInput(Player player,
					ParkourContext context, ParkourInputActions inputAction) {
				return inputAction == targetAction && condition.test(player,
						context);
			}
		};
	}

	/**
	 * 构建一个纯客户端本地 Tick 预测的状态转换规则，用于快速响应但需网络校验的场景。
	 *
	 * <p>此规则仅在本地玩家 client tick 中评估，触发后向服务端发送确认请求。
	 * 适用于贴墙、攀爬等环境判定——客户端先本地切换以消除延迟感，服务端校验后再正式确认。
	 *
	 * @param targetState 目标状态的 Supplier，不为 null
	 * @param condition   触发条件，不为 null
	 * @return 配置好的转换规则实例，始终不为 null
	 */
	static IParkourStateTransition onLocalTick(Supplier<IParkourState> targetState,
			BiPredicate<Player, ParkourContext> condition) {
		return new IParkourStateTransition() {
			@Override
			public IParkourState getTargetState() {
				return targetState.get();
			}

			@Override
			public boolean shouldTransitionOnLocalTick(Player player,
					ParkourContext context) {
				return condition.test(player, context);
			}
		};
	}

	/**
	 * 返回此转换规则的目标状态。
	 *
	 * @return 目标状态实例，不为 null
	 */
	IParkourState getTargetState();

	/**
	 * 每 tick 评估：当前状态是否应切换到目标状态。
	 *
	 * <p>由服务端和本地玩家双端调用。默认返回 false，由具体规则覆写。
	 *
	 * @param player  当前玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @return true 表示应立即执行状态切换
	 */
	default boolean shouldTransitionOnTick(Player player, ParkourContext context) {
		return false;
	}

	// ==========================================
	// 事件驱动型转换 — 默认不触发，由具体规则覆写
	// ==========================================

	/**
	 * 摔落事件触发：当前状态是否应切换到目标状态。
	 *
	 * <p>注意：此方法可能附带副作用（如取消原版摔落伤害），评估时遵循安全优先策略。
	 *
	 * @param player  当前玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @param event   原版摔落事件，可用于 {@code setCanceled(true)} 取消伤害，不为 null
	 * @return true 表示应立即执行状态切换
	 */
	default boolean shouldTransitionOnFall(Player player, ParkourContext context,
			LivingFallEvent event) {
		return false;
	}

	/**
	 * 跳跃事件触发：当前状态是否应切换到目标状态。
	 *
	 * <p>注意：此方法可能附带副作用（如取消原版跳跃），评估时遵循安全优先策略。
	 *
	 * @param player  当前玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @param event   可取消的跳跃事件，可用于 {@code setCanceled(true)} 阻止原版跳跃，不为 null
	 * @return true 表示应立即执行状态切换
	 */
	default boolean shouldTransitionOnJump(Player player, ParkourContext context,
			LivingJumpCancellableEvent event) {
		return false;
	}

	/**
	 * 按键输入触发：当前状态是否应切换到目标状态。
	 *
	 * <p>仅在客户端本地玩家上评估，触发后通过网络同步到服务端。
	 *
	 * @param player      当前玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param inputAction 玩家按下的动作键枚举，不为 null
	 * @return true 表示应立即执行本地预测转换
	 */
	default boolean shouldTransitionOnInput(Player player, ParkourContext context,
			ParkourInputActions inputAction) {
		return false;
	}

	/**
	 * 客户端本地 Tick 预测触发：当前状态是否应切换到目标状态。
	 *
	 * <p>仅在客户端本地玩家的 tick 中评估。触发后客户端立即执行切换并通过网络请求服务端确认。
	 *
	 * @param player  当前玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @return true 表示应立即执行本地预测转换
	 */
	default boolean shouldTransitionOnLocalTick(Player player, ParkourContext context) {
		return false;
	}
}
