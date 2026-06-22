package mod.arcomit.parkour.core.statemachine;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.event.LivingJumpCancellableEvent;
import mod.arcomit.parkour.content.init.ParkourStates;
import mod.arcomit.parkour.core.input.ParkourInputActions;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.network.ParkourNetworkSynchronizer;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 跑酷状态机主控制器，每 tick 驱动的状态生命周期中枢。
 *
 * <p>职责:
 * <ul>
 *   <li>驱动当前状态的 {@link IParkourState#onTick} 生命周期</li>
 *   <li>委托 {@link ParkourTransitionEvaluator} 评估转换规则并执行状态切换</li>
 *   <li>委托 {@link ParkourStateValidator} 校验当前状态合法性，非法时回退到默认状态</li>
 *   <li>提供 {@code transitionTo} / {@code localTransitionTo} 两套状态切换入口</li>
 *   <li>处理跳跃、摔落、按键输入等事件驱动的状态转换</li>
 * </ul>
 *
 * <p>所有方法均为静态方法，以玩家实例为操作目标。状态数据存储在玩家附着的 {@link ParkourContext} 中。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourStateMachine {

	/**
	 * 每 tick 调用的主循环入口，由 {@code PlayerTickEvent.Pre} 等事件驱动。
	 *
	 * <p>执行流程:
	 * <ol>
	 *   <li>自增状态已持续 tick 计数</li>
	 *   <li>调用当前状态的 {@link IParkourState#onTick} 生命周期</li>
	 *   <li>在服务端或本地玩家上评估常规 Tick 转换规则</li>
	 *   <li>校验当前状态合法性，非法则强制回退到默认状态</li>
	 *   <li>仅在本地玩家上评估客户端预测性 Tick 转换规则</li>
	 * </ol>
	 *
	 * <p>副作用：修改 {@link StateData} 中的 tick 计数，可能触发状态切换、网络同步。
	 *
	 * @param player 待驱动状态机的玩家，不为 null；若其当前状态为 null 则跳过本次 tick
	 */
	public static void tick(@NotNull Player player) {
		ParkourContext context = ParkourContext.get(player);
		StateData stateData = context.state();
		IParkourState currentState = stateData.getState();

		if (currentState == null)
			return;

		// TODO：也许应该迁移出去
		handlePassiveStateSync(player, stateData, currentState);

		// 基础 Tick 生命周期
		stateData.setTicksInState(stateData.getTicksInState() + 1);
		currentState.onTick(player, context);

		// 服务端或本地玩家执行普通 Tick 转换评估
		if (!player.level().isClientSide() || player.isLocalPlayer()) {
			ParkourTransitionEvaluator.evaluateTickTransitions(player, context);
		}

		// 合法性检查与预测性 Tick 转换评估
		if (player.level().isClientSide()) {
			if (player.isLocalPlayer()) {
				if (!ParkourStateValidator.isCurrentStateValid(player, context)) {
					resetToDefaultState(player, context);
					return;
				}
				ParkourTransitionEvaluator.evaluateLocalTickTransitions(player,
						context);
			}
		} else if (player instanceof ServerPlayer serverPlayer) {
			if (!ParkourStateValidator.isCurrentStateValid(serverPlayer, context)) {
				resetToDefaultStateAndSync(serverPlayer, context);
			}
		}
	}

	/**
	 * 当检测到状态发生变更时，被动同步姿势、碰撞箱和动画等视觉表现。
	 *
	 * <p>用于状态机外部直接修改 {@link StateData} 的场景（如网络数据包覆盖），
	 * 由调用方决定何时触发此被动同步。
	 *
	 * <p>副作用：可能修改玩家的强制姿势、触发碰撞箱重算、在客户端播放动画。
	 *
	 * @param player       状态所属的玩家，不为 null
	 * @param stateData    当前状态数据容器，不为 null
	 * @param currentState 当前状态实例，不为 null
	 */
	private static void handlePassiveStateSync(Player player, StateData stateData,
			IParkourState currentState) {
		if (stateData.getLastState() != currentState) {
			IParkourState lastState = stateData.getLastState();
			stateData.setLastState(currentState);

			player.setForcedPose(currentState.getLinkedPose());
			player.updatePlayerPose();

			EntityDimensions oldDim = lastState != null ?
					lastState.getCustomDimensions(player) :
					null;
			EntityDimensions newDim = currentState.getCustomDimensions(player);
			if (!Objects.equals(oldDim, newDim)) {
				player.refreshDimensions();
			}

			if (player.level().isClientSide()) {
				ParkourProxies.PLAYER_ANIM_PROXY.playStateAnimation(player);
			}
		}
	}

	// ==========================================
	// 事件转换入口代理 (Delegations)
	// ==========================================

	/**
	 * 将原版摔落事件转发给转换评估器，当前状态的转换规则中如有匹配则自动切状态。
	 *
	 * <p>双端均可调用。评估器内部采用安全优先策略：先校验 canEnter，再执行可能附带取消原版事件副作用的 condition。
	 *
	 * @param player  摔落的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @param event   原版摔落事件，评估器可能对其调用 {@code setCanceled(true)}，不为 null
	 */
	public static void tryFallTransition(@NotNull Player player,
			@NotNull ParkourContext context, LivingFallEvent event) {
		ParkourTransitionEvaluator.evaluateFallTransitions(player, context, event);
	}

	/**
	 * 将可取消的跳跃事件转发给转换评估器。
	 *
	 * <p>双端均可调用。如转换规则匹配，可能取消原版跳跃行为并切换到自定义状态。
	 *
	 * @param player  跳跃的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 * @param event   可取消的跳跃事件，不为 null
	 */
	public static void tryJumpTransition(@NotNull Player player,
			@NotNull ParkourContext context, LivingJumpCancellableEvent event) {
		ParkourTransitionEvaluator.evaluateJumpTransitions(player, context, event);
	}

	/**
	 * 将客户端按键输入转发给转换评估器，仅在本地玩家上评估。
	 *
	 * <p>匹配的转换将执行本地预测切换并通过网络请求服务端校验。
	 *
	 * @param player      按键的玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param inputAction 当前按下的动作键枚举，不为 null
	 */
	public static void tryInputTransition(@NotNull Player player,
			@NotNull ParkourContext context, ParkourInputActions inputAction) {
		ParkourTransitionEvaluator.evaluateInputTransitions(player, context, inputAction);
	}

	// ==========================================
	// 核心状态执行入口代理 (Delegations)
	// ==========================================

	/**
	 * 执行权威状态切换（服务端或双端统一入口），自动生成动画变体。
	 *
	 * <p>调用 {@link IParkourState#generateVariant} 生成变体 ID 后委托
	 * {@link ParkourStateEngine#executeCoreTransition} 执行完整生命周期。 在服务端执行时会广播状态变更给所有追踪者。
	 *
	 * @param player      状态所属的玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 */
	public static void transitionTo(@NotNull Player player, @NotNull ParkourContext context,
			@NotNull IParkourState targetState) {
		int variant = targetState.generateVariant(player);
		ParkourStateEngine.executeCoreTransition(player, context, targetState, variant);
	}

	/**
	 * {@link #transitionTo(Player, ParkourContext, IParkourState)} 的便捷重载，自动从玩家获取上下文。
	 *
	 * @param player      状态所属的玩家，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 */
	public static void transitionTo(@NotNull Player player,
			@NotNull IParkourState targetState) {
		transitionTo(player, ParkourContext.get(player), targetState);
	}

	/**
	 * 执行权威状态切换并指定动画变体 ID，跳过自动生成。
	 *
	 * <p>适用于网络同步等场景，变体 ID 已在发送方确定，接收方直接使用。
	 *
	 * @param player      状态所属的玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 * @param animVariant 动画变体 ID，由发送方（服务器）确定，取值范围取决于具体状态的实现
	 */
	public static void transitionTo(@NotNull Player player, @NotNull ParkourContext context,
			@NotNull IParkourState targetState, int animVariant) {
		ParkourStateEngine.executeCoreTransition(player, context, targetState, animVariant);
	}

	/**
	 * {@link #transitionTo(Player, ParkourContext, IParkourState, int)} 的便捷重载。
	 *
	 * @param player      状态所属的玩家，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 * @param animVariant 动画变体 ID
	 */
	public static void transitionTo(@NotNull Player player, @NotNull IParkourState targetState,
			int animVariant) {
		transitionTo(player, ParkourContext.get(player), targetState, animVariant);
	}

	/**
	 * 执行客户端本地预测状态切换，自动生成动画变体，并附带网络同步请求。
	 *
	 * <p>客户端立即执行完整生命周期切换（消除输入延迟），同时向服务端发送确认请求。
	 * 服务端校验通过后以权威切换覆盖，校验不通过则回退。
	 *
	 * @param player      状态所属的玩家，必须为本地玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 */
	public static void localTransitionTo(@NotNull Player player,
			@NotNull ParkourContext context, @NotNull IParkourState targetState) {
		int variant = targetState.generateVariant(player);
		ParkourStateEngine.executeLocalTransition(player, context, targetState, variant);
	}

	/**
	 * {@link #localTransitionTo(Player, ParkourContext, IParkourState)} 的便捷重载。
	 *
	 * @param player      状态所属的玩家，必须为本地玩家，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 */
	public static void localTransitionTo(@NotNull Player player,
			@NotNull IParkourState targetState) {
		localTransitionTo(player, ParkourContext.get(player), targetState);
	}

	/**
	 * 执行客户端本地预测状态切换并指定动画变体，附带网络同步请求。
	 *
	 * @param player      状态所属的玩家，必须为本地玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 * @param animVariant 动画变体 ID
	 */
	public static void localTransitionTo(@NotNull Player player,
			@NotNull ParkourContext context, @NotNull IParkourState targetState,
			int animVariant) {
		ParkourStateEngine.executeLocalTransition(player, context, targetState,
				animVariant);
	}

	/**
	 * {@link #localTransitionTo(Player, ParkourContext, IParkourState, int)} 的便捷重载。
	 *
	 * @param player      状态所属的玩家，必须为本地玩家，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 * @param animVariant 动画变体 ID
	 */
	public static void localTransitionTo(@NotNull Player player,
			@NotNull IParkourState targetState, int animVariant) {
		localTransitionTo(player, ParkourContext.get(player), targetState, animVariant);
	}

	/**
	 * 判断当前状态是否不等于默认状态，即是否需要回退。
	 *
	 * @param context 跑酷上下文，不为 null
	 * @return true 表示当前状态不是默认状态，可以执行回退
	 */
	private static boolean shouldResetToDefault(@NotNull ParkourContext context) {
		IParkourState currentState = context.state().getState();
		IParkourState defaultState = ParkourStates.DEFAULT.get();
		return currentState != defaultState;
	}

	/**
	 * 客户端侧强制回退到默认状态。
	 *
	 * <p>触发场景：合法性校验失败（如玩家落地后仍处于空中动作状态）。
	 * 仅在当前状态不是默认状态时才执行切换，避免重复操作。
	 *
	 * <p>副作用：执行状态切换，修改玩家姿势和碰撞箱。
	 *
	 * @param player  待回退的玩家，不为 null
	 * @param context 跑酷上下文，不为 null
	 */
	public static void resetToDefaultState(@NotNull Player player,
			@NotNull ParkourContext context) {
		if (shouldResetToDefault(context)) {
			if (!FMLLoader.isProduction()) {
				ParkourMod.LOGGER.debug(
						"Resetting player {} to DEFAULT state due to invalidation.",
						player.getName().getString());
			}
			ParkourStateMachine.transitionTo(player, context,
					ParkourStates.DEFAULT.get());
		}
	}

	/**
	 * 服务端侧强制回退到默认状态，并显式同步给客户端以确保双端一致。
	 *
	 * <p>触发场景：服务端合法性校验失败。与客户端回退不同，这里需要主动向客户端发送网络包
	 * 以覆盖客户端可能存在的错误预测状态。
	 *
	 * <p>副作用：执行状态切换，通过 {@link ParkourNetworkSynchronizer#forceLocalPlayerState} 强制同步客户端。
	 *
	 * @param serverPlayer 待回退的玩家，必须为服务端玩家实例，不为 null
	 * @param context      跑酷上下文，不为 null
	 */
	public static void resetToDefaultStateAndSync(@NotNull ServerPlayer serverPlayer,
			@NotNull ParkourContext context) {
		if (shouldResetToDefault(context)) {
			if (!FMLLoader.isProduction()) {
				ParkourMod.LOGGER.debug(
						"Server resetting player {} to DEFAULT state and syncing.",
						serverPlayer.getName().getString());
			}
			ParkourStateMachine.transitionTo(serverPlayer, context,
					ParkourStates.DEFAULT.get());

			ResourceLocation defaultId = ParkourStates.DEFAULT.getId();
			ParkourNetworkSynchronizer.forceLocalPlayerState(serverPlayer, defaultId,
					IParkourState.DEFAULT_ANIM_VARIANT);
		}
	}
}
