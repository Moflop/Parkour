package mod.arcomit.parkour.core.statemachine;

import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.event.LivingJumpCancellableEvent;
import mod.arcomit.parkour.core.input.ParkourInputActions;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import mod.arcomit.parkour.core.statemachine.state.IParkourStateTransition;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * 转换条件评估器，遍历当前状态的转换规则并决定是否执行状态切换。
 *
 * <p>核心设计原则：
 * <ul>
 *   <li><b>无副作用转换</b>（Tick、Input）：走性能优化路线——先执行轻量 condition 判断，短路后再做较重的 canEnter 校验</li>
 *   <li><b>有副作用转换</b>（Jump、Fall）：走安全防御路线——先校验 canEnter，确保能进入目标状态后再触发可能取消原版事件的 condition</li>
 * </ul>
 *
 * <p>两条路线的判断顺序不可互换：事件转换中 condition 可能附带 {@code setCanceled(true)}，如果 canEnter 在后，
 * 一旦 canEnter 返回 false 就会"吞掉"原版事件而不发生任何切换。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourTransitionEvaluator {

	/**
	 * 【性能优化版】：针对无副作用的常规转换（如 Tick、Input）。 先执行极轻量的 condition 判断，如果不满足则直接短路，跳过开销可能较大的 canEnter
	 * 校验。
	 */
	private static void evaluateSafeTransitionIf(Player player, ParkourContext context,
			Predicate<IParkourStateTransition> condition,
			BiConsumer<Player, IParkourState> transitionAction) {
		IParkourState currentState = context.state().getState();
		if (currentState == null)
			return;

		for (IParkourStateTransition transition : currentState.getTransitions()) {
			IParkourState targetState = transition.getTargetState();

			// 性能优先：轻量级 condition 在前，重量级 canEnter 在后
			if (condition.test(transition) && targetState.canEnter(player, context)) {
				transitionAction.accept(player, targetState);
				return;
			}
		}
	}

	/**
	 * 【安全优先版】：针对带有副作用的事件转换（如 Jump、Fall 会取消原版事件）。 为了避免短路导致误吞原版事件，必须先执行 canEnter，确保真正能进入状态后，再触发
	 * condition。
	 */
	private static void evaluateEventTransitionIf(Player player, ParkourContext context,
			Predicate<IParkourStateTransition> condition,
			BiConsumer<Player, IParkourState> transitionAction) {
		IParkourState currentState = context.state().getState();
		if (currentState == null)
			return;

		for (IParkourStateTransition transition : currentState.getTransitions()) {
			IParkourState targetState = transition.getTargetState();

			// 逻辑安全优先：canEnter 在前，带有 setCanceled 副作用的 condition 在后
			if (targetState.canEnter(player, context) && condition.test(transition)) {
				transitionAction.accept(player, targetState);
				return;
			}
		}
	}

	// ==========================================
	// 常规无副作用评估（走性能优化路线）
	// ==========================================

	/**
	 * 评估当前状态中基于 Tick 的转换规则，匹配即执行权威切换。
	 *
	 * <p>在服务端和客户端本地玩家上调用。遍历当前状态的转换规则列表，
	 * 逐个检查 {@link IParkourStateTransition#shouldTransitionOnTick}， 首次匹配即切换并返回（短路评估）。
	 *
	 * @param player  当前玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	public static void evaluateTickTransitions(Player player, ParkourContext context) {
		evaluateSafeTransitionIf(player, context,
				transition -> transition.shouldTransitionOnTick(player, context),
				(p, targetState) -> ParkourStateMachine.transitionTo(p, context,
						targetState));
	}

	/**
	 * 评估当前状态中基于客户端本地预测的 Tick 转换规则。
	 *
	 * <p>仅在客户端本地玩家上调用。匹配后执行本地预测切换（立即生效+网络请求确认）。
	 *
	 * @param player  当前玩家，必须为 {@link Player#isLocalPlayer()} == true，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	public static void evaluateLocalTickTransitions(Player player, ParkourContext context) {
		if (!player.isLocalPlayer())
			return;
		evaluateSafeTransitionIf(player, context,
				transition -> transition.shouldTransitionOnLocalTick(player,
						context),
				(p, targetState) -> ParkourStateMachine.localTransitionTo(p,
						context, targetState));
	}

	/**
	 * 评估基于按键输入的转换规则，仅在本地玩家上执行。
	 *
	 * <p>匹配的转换将走本地预测流程：客户端立即切换，向服务端发送确认请求。
	 *
	 * @param player      当前玩家，必须为本地玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param inputAction 当前按下的动作键枚举，不为 null
	 */
	public static void evaluateInputTransitions(Player player, ParkourContext context,
			ParkourInputActions inputAction) {
		if (!player.isLocalPlayer())
			return;
		evaluateSafeTransitionIf(player, context,
				transition -> transition.shouldTransitionOnInput(player, context,
						inputAction),
				(p, targetState) -> ParkourStateMachine.localTransitionTo(p,
						context, targetState));
	}

	// ==========================================
	// 带有副作用的事件评估（走安全防御路线）
	// ==========================================

	/**
	 * 评估基于摔落事件的转换规则，匹配则执行权威切换。
	 *
	 * <p>采用安全优先策略：先确认目标状态允许进入，再执行 condition（condition 可能取消原版摔落伤害）。
	 *
	 * @param player  摔落的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @param event   原版摔落事件，可能被转换规则取消，不为 null
	 */
	public static void evaluateFallTransitions(Player player, ParkourContext context,
			LivingFallEvent event) {
		evaluateEventTransitionIf(player, context,
				transition -> transition.shouldTransitionOnFall(player, context,
						event),
				(p, targetState) -> ParkourStateMachine.transitionTo(p, context,
						targetState));
	}

	/**
	 * 评估基于跳跃事件的转换规则，匹配则执行权威切换。
	 *
	 * <p>采用安全优先策略：先确认目标状态允许进入，再执行 condition（condition 可能取消原版跳跃行为）。
	 *
	 * @param player  跳跃的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @param event   可取消的跳跃事件，可能被转换规则取消，不为 null
	 */
	public static void evaluateJumpTransitions(Player player, ParkourContext context,
			LivingJumpCancellableEvent event) {
		evaluateEventTransitionIf(player, context,
				transition -> transition.shouldTransitionOnJump(player, context,
						event),
				(p, targetState) -> ParkourStateMachine.transitionTo(p, context,
						targetState));
	}
}
