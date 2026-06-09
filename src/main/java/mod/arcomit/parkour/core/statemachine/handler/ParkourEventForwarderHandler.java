package mod.arcomit.parkour.core.statemachine.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.event.LivingJumpCancellableEvent;
import mod.arcomit.parkour.core.statemachine.ParkourStateMachine;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 跑酷状态机的事件转发器，监听 NeoForge 总线的物理事件并桥接到状态机。
 *
 * <p>职责：将原版 Tick、摔落、跳跃事件转换为状态机的 {@code tryFallTransition} / {@code tryJumpTransition} /
 * {@code tick} 调用。 自身不含任何业务逻辑，仅做类型检查和事件转发。
 *
 * <p>双端均注册，但摔落事件在客户端会通过 {@code ParkourChecks.isFallUnsafe} 做额外的安全性过滤。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class ParkourEventForwarderHandler {

	/**
	 * 玩家每 tick 开始前触发，将 tick 转发给状态机驱动状态生命周期和转换评估。
	 *
	 * <p>使用 {@code PlayerTickEvent.Pre} 阶段以确保在大多数原版逻辑之前执行跑酷状态更新。
	 * 双端均会触发——服务端执行权威逻辑，客户端执行预测和渲染。
	 *
	 * @param event Pre 阶段的玩家 tick 事件，不为 null
	 */
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		ParkourStateMachine.tick(player);
	}

	/**
	 * 玩家摔落时触发，过滤安全摔落后转发给状态机评估摔落转换规则。
	 *
	 * <p>只有不安全摔落（如从致命高度坠落）才会进入转换评估；安全摔落直接跳过以避免不必要的状态切换。
	 * 双端均会触发。
	 *
	 * @param event 原版摔落事件，可能被匹配的转换规则取消伤害，不为 null
	 */
	@SubscribeEvent
	public static void onPlayerFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!ParkourChecks.isFallUnsafe(player))
			return;
		ParkourContext context = ParkourContext.get(player);
		ParkourStateMachine.tryFallTransition(player, context, event);
	}

	/**
	 * 玩家跳跃时触发，转发给状态机评估跳跃转换规则。
	 *
	 * <p>匹配的转换规则可能取消原版跳跃行为（如替换为自定义跳跃动作）。
	 * 双端均会触发。
	 *
	 * @param event 可取消的跳跃事件，可能被匹配的转换规则取消，不为 null
	 */
	@SubscribeEvent
	public static void onPlayerJump(LivingJumpCancellableEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		ParkourContext context = ParkourContext.get(player);
		ParkourStateMachine.tryJumpTransition(player, context, event);
	}
}
