package mod.arcomit.parkour.content.behavior.backstep.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.backstep.BackstepState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.StateData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 后撤步无敌帧处理器 —— 监听服务端伤害事件，处于后撤步时直接取消所有即将到来的伤害。
 * <p>
 * 仅服务端生效：客户端侧无视，因为伤害结算由服务端裁决。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class BackstepInvincibleHandler {

	/**
	 * 当玩家处于后撤步状态时，拦截所有伤害。 副作用：直接将事件的canceled置为true，伤害事件完全中止。
	 */
	@SubscribeEvent
	public static void onPlayerIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.level()
				.isClientSide()) {
			return;
		}

		StateData stateData = ParkourContext.get(player).state();
		if (stateData.getState() instanceof BackstepState) {
			event.setCanceled(true);
		}
	}
}
