package mod.arcomit.parkour.core.statemachine.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 服务端同步处理器，处理跨维度传送和实体追踪等需要状态同步的场景。
 *
 * <p>自动注册到 NeoForge 事件总线，监听以下事件：
 * <ul>
 *   <li>玩家切换维度时，强制回退到默认状态并同步客户端，因为跨维度后客户端状态可能不正确</li>
 *   <li>其他玩家开始追踪目标玩家时，调用目标玩家当前状态的 {@link IParkourState#onTrackingStart}
 *       以向追踪者发送状态特有的额外数据（如自定义动画参数）</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class ParkourSyncHandler {

	/**
	 * 玩家切换维度时触发，强制回退到默认状态以避免跨维度状态残留。
	 *
	 * <p>跨维度传送会重置客户端状态，如果玩家在跑酷动作中传送，客户端可能处于不一致状态。
	 * 因此在服务端强制执行默认状态并通过网络同步覆盖客户端。
	 *
	 * @param event 维度变更事件，不为 null
	 */
	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			ParkourContext context = ParkourContext.get(serverPlayer);
			ParkourStateMachine.resetToDefaultStateAndSync(serverPlayer, context);
		}
	}

	/**
	 * 玩家开始追踪另一个实体时触发，用于向新追踪者发送目标玩家的状态特有数据。
	 *
	 * <p>Minecraft 的实体追踪系统只在玩家进入视野时同步基础数据。如果目标玩家当前处于特殊跑酷状态
	 * （如自定义碰撞箱、特殊动画），追踪者不会自动获得这些信息。此方法通过
	 * {@link IParkourState#onTrackingStart} 向追踪者补发这些额外数据包。
	 *
	 * @param event 开始追踪事件，不为 null
	 */
	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof Player targetPlayer) {
			if (event.getEntity() instanceof ServerPlayer tracker) {
				ParkourContext context = ParkourContext.get(targetPlayer);
				IParkourState currentState = context.state().getState();
				if (currentState != null) {
					currentState.onTrackingStart(tracker, targetPlayer,
							context);
				}
			}
		}
	}
}
