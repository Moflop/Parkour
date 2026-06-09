package mod.arcomit.parkour.core.statemachine;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.network.ParkourNetworkSynchronizer;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 状态转换的执行引擎，负责执行完整的状态切换生命周期。
 *
 * <p>每次状态切换都经过以下步骤：
 * <ol>
 *   <li>调用旧状态的 {@link IParkourState#onExit} 生命周期</li>
 *   <li>更新 {@link StateData} 中的当前状态、tick 计数、动画变体</li>
 *   <li>调用新状态的 {@link IParkourState#onEnter} 生命周期</li>
 *   <li>更新强制姿势、碰撞箱（仅在尺寸变化时重算）</li>
 *   <li>服务端：广播状态变更给所有追踪者</li>
 *   <li>客户端：播放状态动画</li>
 * </ol>
 *
 * <p>提供两种切换模式：{@code executeCoreTransition}（权威切换）和 {@code executeLocalTransition}（本地预测+网络请求）。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourStateEngine {

	/**
	 * 执行本地预测状态转换：先本地执行完整生命周期切换，再向服务端发送校验请求。
	 *
	 * <p>设计目的：消除客户端输入到视觉反馈之间的网络往返延迟。客户端立即切换到目标状态并播放动画，
	 * 同时通过网络包请求服务端确认。若服务端校验不通过，将以权威状态覆盖本地预测。
	 *
	 * <p>副作用：修改 {@link StateData}、玩家姿势和碰撞箱，向服务端发送网络包。
	 *
	 * @param player      状态所属的玩家，必须为本地玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 * @param animVariant 动画变体 ID，由 {@link IParkourState#generateVariant} 生成或网络同步指定
	 */
	public static void executeLocalTransition(@NotNull Player player,
			@NotNull ParkourContext context, @NotNull IParkourState targetState,
			int animVariant) {
		executeCoreTransition(player, context, targetState, animVariant);
		ParkourNetworkSynchronizer.requestServerTransition(targetState, animVariant);
	}

	/**
	 * 执行权威状态切换的完整生命周期。
	 *
	 * <p>无论服务端还是客户端调用，此方法都执行相同的核心流程。区别在于：
	 * <ul>
	 *   <li>服务端执行后会广播状态变更给所有追踪者（通过 {@link ParkourNetworkSynchronizer#broadcastStateChange}）</li>
	 *   <li>客户端执行后仅播放动画，不广播</li>
	 * </ul>
	 *
	 * <p>副作用：
	 * <ul>
	 *   <li>修改 {@link StateData}（当前状态、tick 计数、动画变体、上次状态引用）</li>
	 *   <li>修改玩家强制姿势（{@link Player#setForcedPose}）</li>
	 *   <li>若碰撞箱尺寸变化则触发 {@link Player#refreshDimensions}</li>
	 *   <li>服务端：发送网络包给所有追踪者</li>
	 *   <li>客户端：播放动画</li>
	 * </ul>
	 *
	 * @param player      状态所属的玩家，不为 null
	 * @param context     跑酷上下文，不为 null
	 * @param targetState 要切换到的目标状态，不为 null
	 * @param animVariant 动画变体 ID，用于双端一致渲染
	 */
	public static void executeCoreTransition(@NotNull Player player,
			@NotNull ParkourContext context, @NotNull IParkourState targetState,
			int animVariant) {
		StateData stateData = context.state();
		IParkourState currentState = stateData.getState();

		if (currentState != null) {
			currentState.onExit(player, context);
		}

		stateData.setState(targetState);
		stateData.setTicksInState(0);
		stateData.setAnimationVariant(animVariant);

		targetState.onEnter(player, context);

		stateData.setLastState(currentState);
		player.setForcedPose(targetState.getLinkedPose());
		player.updatePlayerPose();

		EntityDimensions currentDim = currentState != null ?
				currentState.getCustomDimensions(player) :
				null;
		EntityDimensions targetDim = targetState.getCustomDimensions(player);

		if (!Objects.equals(currentDim, targetDim)) {
			player.refreshDimensions();
		}

		if (player.level().isClientSide()) {
			ParkourProxies.PLAYER_ANIM_PROXY.playStateAnimation(player);
			if (FMLEnvironment.isProduction()) {
				ParkourMod.LOGGER.debug(
						"【Client】 Player {} transitioned to state: {} (Variant: {})",
						player.getName().getString(),
						targetState.getClass().getSimpleName(),
						animVariant);
			}
		} else {
			ParkourNetworkSynchronizer.broadcastStateChange(player, targetState,
					animVariant);
			if (FMLEnvironment.isProduction()) {
				ParkourMod.LOGGER.debug(
						"【Server】 Player {} transitioned to state: {} (Variant: {})",
						player.getName().getString(),
						targetState.getClass().getSimpleName(),
						animVariant);
			}
		}
	}
}
