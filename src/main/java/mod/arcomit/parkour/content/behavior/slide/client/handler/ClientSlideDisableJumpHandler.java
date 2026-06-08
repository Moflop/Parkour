package mod.arcomit.parkour.content.behavior.slide.client.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.slide.SlideState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * 滑铲期间禁止跳跃 —— 客户端侧在移动输入更新时拦截跳跃键。
 * <p>
 * 由于滑铲姿态下跳跃会破坏体验和动画，此处理器在客户端层直接将跳跃输入置为false。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID, value = Dist.CLIENT)
public class ClientSlideDisableJumpHandler {

	/**
	 * 处于滑铲状态时，强制禁用跳跃输入。
	 * <p>
	 * 副作用：直接修改事件的Input对象，将jumping字段设为false。
	 */
	@SubscribeEvent
	public static void disableJumpWhileSliding(MovementInputUpdateEvent event) {
		Player player = event.getEntity();
		StateData stateData = ParkourContext.get(player).state();

		if (stateData.getState() instanceof SlideState) {
			event.getInput().jumping = false;
		}
	}

}
