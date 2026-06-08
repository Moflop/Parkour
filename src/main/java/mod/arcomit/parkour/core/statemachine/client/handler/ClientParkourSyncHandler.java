package mod.arcomit.parkour.core.statemachine.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;


/**
 * 客户端实体加入世界时的跑酷状态同步处理器。
 *
 * <p>当远程玩家进入本地客户端的视野（加入世界）时，Minecraft 默认只同步基础实体数据。
 * 跑酷状态中的自定义姿势、碰撞箱尺寸和动画参数不会自动应用。
 * 此处理器在实体加入世界时检查其跑酷状态并主动应用视觉表现。
 *
 * <p>仅在客户端注册（{@code Dist.CLIENT}），仅处理非本地玩家。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = ParkourMod.MODID)
public class ClientParkourSyncHandler {

	/**
	 * 实体加入世界时触发，为非本地玩家应用其当前跑酷状态的视觉表现。
	 *
	 * <p>跳过本地玩家和默认状态的玩家（默认状态使用原版默认外观，无需额外处理）。
	 * 对非默认状态的远程玩家：应用强制姿势、更新玩家模型姿态、刷新碰撞箱尺寸、
	 * 播放状态关联的动画。
	 *
	 * <p>副作用：修改玩家的强制姿势、触发碰撞箱重算、播放动画。
	 *
	 * @param event 实体加入世界事件，不为 null
	 */
	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.isLocalPlayer()) {
			return;
		}

		IParkourState currentState = ParkourContext.get(player).state().getState();
		if (currentState instanceof DefaultState) {
			return;
		}

		player.setForcedPose(currentState.getLinkedPose());
		player.updatePlayerPose();
		player.refreshDimensions();
		if (player.level().isClientSide()) {
			ParkourProxies.PLAYER_ANIM_PROXY.playStateAnimation(player);
		}
	}
}
