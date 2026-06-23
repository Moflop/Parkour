package mod.arcomit.parkour.core.client.animation.player;

import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.content.init.ParkourRegistries;
import mod.arcomit.parkour.core.client.animation.player.network.RequestPlayOneOffAnimC2SPayload;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端玩家动画管理器，驱动跑酷状态下的骨骼动画播放与网络同步。
 * <p>
 * 管理两类动画通道：
 * <ul>
 *   <li><b>状态动画（Layer 1000）</b>：随跑酷状态机切换而改变的循环动画，
 *       如站立滑铲姿态，支持变体和时间偏移</li>
 *   <li><b>动作动画（Layer 2000）</b>：一次性触发的动作表演，如前空翻，
 *       支持淡入过渡和打断策略</li>
 * </ul>
 * <p>
 * 每个玩家的动画控制器按 UUID 隔离管理，玩家登出或离开世界时自动清理。
 * 动作动画通过 C2S 网络包请求服务端广播，实现多人同步。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class PlayerAnimationManager {

	public static final PlayerAnimationManager INSTANCE = new PlayerAnimationManager();

	/**
	 * 状态动画层的 PAL 层 ID，取值 1000 避免与 PAL 内部层冲突。
	 */
	public static final int PARKOUR_STATE_LAYER_ID = 1000;
	/**
	 * 动作动画层的 PAL 层 ID，取值 2000，高于状态层以确保覆盖渲染。
	 */
	public static final int PARKOUR_ACTION_LAYER_ID = 2000;

	private final Map<UUID, PlayerAnimationController> stateControllers = new HashMap<>();
	private final Map<UUID, PlayerAnimationController> actionControllers = new HashMap<>();
	private final Map<UUID, Boolean> actionInterruptible = new HashMap<>();

	private PlayerAnimationManager() {
	}

	/**
	 * 玩家登出时清理所有动画控制器。
	 */
	@SubscribeEvent
	public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
		INSTANCE.clearAll();
	}

	/**
	 * 其他玩家实体离开客户端视野时移除其动画控制器。
	 */
	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (event.getLevel()
				.isClientSide() && event.getEntity() instanceof AbstractClientPlayer player) {
			INSTANCE.removePlayer(player.getUUID());
		}
	}

	/**
	 * 按 UUID 获取或新建玩家的状态动画控制器。
	 *
	 * @param player 目标客户端玩家实例
	 * @return 该玩家的状态动画控制器，保证非 null
	 */
	private PlayerAnimationController getOrCreateStateController(AbstractClientPlayer player) {
		return stateControllers.computeIfAbsent(player.getUUID(),
				uuid -> new PlayerAnimationController(player,
						(animController, state, animationSetter) -> PlayState.CONTINUE));
	}

	/**
	 * 按 UUID 获取或新建玩家的动作动画控制器。
	 */
	private PlayerAnimationController getOrCreateActionController(AbstractClientPlayer player) {
		return actionControllers.computeIfAbsent(player.getUUID(),
				uuid -> new PlayerAnimationController(player,
						(animController, state, animationSetter) -> PlayState.CONTINUE));
	}

	/**
	 * 播放一次性动作动画，淡入时长默认为 0。
	 *
	 * @param player        目标玩家，若为本地玩家则同时发送网络请求以同步至其他客户端
	 * @param animationId   动作动画标识符
	 * @param interruptible 是否可被后续状态动画打断
	 */
	public void playOneOffAnimation(AbstractClientPlayer player, Identifier animationId,
			boolean interruptible) {
		playOneOffAnimation(player, animationId, interruptible, 0);
	}

	/**
	 * 播放一次性动作动画，支持淡入过渡。
	 * <p>
	 * 若该玩家已有正在播放的动作，会先停止旧动作并移除旧层。 动画通过 {@link ClientAnimationRegistry} 查找对应的 Modifier
	 * 工厂动态构造姿态约束。 对于本地玩家，同时向服务端发送 {@link RequestPlayOneOffAnimC2SPayload} 以广播给其他客户端。
	 *
	 * @param player        目标玩家，不可为 null
	 * @param animationId   动作动画标识符，需已通过 {@link ClientAnimationRegistry#registerActionModifier}
	 *                      注册
	 * @param interruptible 是否可被状态动画打断
	 * @param fadeTicks     淡入过渡时长，单位 tick（20 tick = 1 秒），0 表示无过渡直接触发
	 */
	public void playOneOffAnimation(AbstractClientPlayer player, Identifier animationId,
			boolean interruptible, int fadeTicks) {
		AvatarAnimManager manager = PlayerAnimationAccess.getPlayerAnimManager(player);
		UUID uuid = player.getUUID();

		if (actionControllers.containsKey(uuid)) {
			PlayerAnimationController oldController = actionControllers.get(uuid);
			oldController.stop();
			oldController.removeAllModifiers();
			manager.removeLayer(PARKOUR_ACTION_LAYER_ID);
		}

		PlayerAnimationController newActionController =
				new PlayerAnimationController(player,
						(animController, state, animationSetter) -> PlayState.CONTINUE);

		AbstractModifier modifier =
				ClientAnimationRegistry.getActionModifier(animationId, player);
		if (modifier != null) {
			newActionController.addModifierLast(modifier);
		}

		actionControllers.put(uuid, newActionController);

		if (fadeTicks > 0) {
			AbstractFadeModifier fadeModifier =
					AbstractFadeModifier.standardFadeIn(fadeTicks,
							EasingType.LINEAR);
			newActionController.replaceAnimationWithFade(fadeModifier, animationId);
		} else {
			newActionController.triggerAnimation(animationId, 0);
		}

		manager.addAnimLayer(PARKOUR_ACTION_LAYER_ID, newActionController);
		actionInterruptible.put(uuid, interruptible);

		if (player.isLocalPlayer()) {
			ClientPacketDistributor.sendToServer(
					new RequestPlayOneOffAnimC2SPayload(animationId,
							interruptible, fadeTicks));
		}
	}

	/**
	 * 根据当前跑酷状态切换并播放对应的循环动画。
	 * <p>
	 * 从 {@link ParkourContext} 中读取当前状态和变体信息，在动作层播放时检查打断策略：
	 * <ul>
	 *   <li>若动作动画不可打断且状态动画存在有效资源，则保留动作播放</li>
	 *   <li>否则停止动作动画，切换到新的状态动画</li>
	 * </ul>
	 * 动画从 {@code ticksInState} 偏移量开始播放，确保动画进度与状态已持续时间一致。
	 * 若当前状态未注册动画，则清空该玩家的状态动画层。
	 *
	 * @param player 目标客户端玩家，不可为 null
	 */
	public void playStateAnimation(AbstractClientPlayer player) {
		StateData stateData = ParkourContext.get(player).state();
		IParkourState currentState = stateData.getState();

		if (currentState == null) {
			return;
		}

		UUID uuid = player.getUUID();

		Identifier stateId = ParkourRegistries.PARKOUR_STATE_REGISTRY.getKey(currentState);
		PlayerAnimation targetAnim = ClientAnimationRegistry.getAnimation(stateId,
				stateData.getAnimVariant());

		if (actionControllers.containsKey(uuid)) {
			PlayerAnimationController actionController = actionControllers.get(uuid);
			if (actionController.isActive()) {
				boolean interruptible =
						actionInterruptible.getOrDefault(uuid, true);
				if (targetAnim != null || interruptible) {
					actionController.stop();
				}
			}
		}

		PlayerAnimationController stateController = getOrCreateStateController(player);
		AvatarAnimManager manager = PlayerAnimationAccess.getPlayerAnimManager(player);

		if (targetAnim != null) {
			stateController.removeAllModifiers();

			IModifierFactory factory =
					ClientAnimationRegistry.getModifierFactory(stateId);
			if (factory != null) {
				factory.apply(stateController, player, currentState,
						stateData.getAnimVariant());
			}

			int offsetTicks = stateData.getTicksInState();
			stateController.triggerAnimation(targetAnim.id, offsetTicks);
			manager.removeLayer(PARKOUR_STATE_LAYER_ID);
			manager.addAnimLayer(PARKOUR_STATE_LAYER_ID, stateController);
		} else {
			stateController.stop();
			stateController.removeAllModifiers();
			manager.removeLayer(PARKOUR_STATE_LAYER_ID);
		}
	}

	/**
	 * 停止并移除所有玩家（包括本地和远程）的动画控制器。 通常在客户端登出或资源重载时调用。
	 */
	public void clearAll() {
		stateControllers.values().forEach(PlayerAnimationController::stop);
		actionControllers.values().forEach(PlayerAnimationController::stop);
		stateControllers.clear();
		actionControllers.clear();
		actionInterruptible.clear();
	}

	/**
	 * 移除指定玩家的所有动画控制器。 在该玩家实体离开客户端视野时调用。
	 *
	 * @param uuid 要移除的玩家 UUID
	 */
	public void removePlayer(UUID uuid) {
		PlayerAnimationController stateController = stateControllers.remove(uuid);
		if (stateController != null)
			stateController.stop();

		PlayerAnimationController actionController = actionControllers.remove(uuid);
		if (actionController != null)
			actionController.stop();

		actionInterruptible.remove(uuid);
	}
}
