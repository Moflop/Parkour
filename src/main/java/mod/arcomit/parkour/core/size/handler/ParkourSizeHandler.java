package mod.arcomit.parkour.core.size.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

/**
 * 跑酷状态下玩家碰撞箱尺寸的动态修改。
 * <p>
 * 监听 {@link EntityEvent.Size} 事件，根据玩家当前所处的跑酷状态 动态调整其碰撞箱宽高。例如滑铲状态需要降低碰撞箱高度以适配低矮通道。
 * <p>
 * 服务端玩家在登录完成前（connection == null）跳过处理，避免空指针。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class ParkourSizeHandler {

	/**
	 * 在实体碰撞箱尺寸计算阶段，用跑酷状态定义的自定义尺寸覆盖默认值。
	 * <p>
	 * 副效应：通过 {@code event.setNewSize} 修改了碰撞箱尺寸， 直接影响后续的碰撞检测和视点高度计算。
	 *
	 * @param event NeoForge 实体尺寸事件，允许动态设置新尺寸
	 */
	@SubscribeEvent
	public static void applyStateDimensions(EntityEvent.Size event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}

		if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection == null) {
			return;
		}

		ParkourContext context = ParkourContext.get(player);

		StateData stateData = context.state();

		IParkourState currentState = stateData.getState();

		EntityDimensions customSize = currentState.getCustomDimensions(player);
		if (customSize != null) {
			event.setNewSize(customSize);
		}
	}
}
