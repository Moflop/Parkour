package mod.arcomit.parkour.content.mechanic.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.mechanic.freestyle.FreestyleMechanic;
import mod.arcomit.parkour.content.mechanic.jumpstrength.JumpStrengthIncreaseMechanic;
import mod.arcomit.parkour.content.mechanic.movespeed.MoveSpeedIncreaseMechanic;
import mod.arcomit.parkour.content.mechanic.shallowswim.ShallowSwimmingMechanic;
import mod.arcomit.parkour.content.mechanic.stepheight.StepHeightIncreaseMechanic;
import mod.arcomit.parkour.content.mechanic.swimimprovements.SwimmingImprovementsMechanic;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 中央 Tick 处理器，统一分发所有机制的更新逻辑。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class MechanicTickHandler {

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();

		// 属性修改类机制 (内部已做性能优化，不会每 tick 重复设值)
		StepHeightIncreaseMechanic.handle(player);
		MoveSpeedIncreaseMechanic.handle(player);
		JumpStrengthIncreaseMechanic.handle(player);

		// 状态判断类机制
		FreestyleMechanic.handle(player);
		ShallowSwimmingMechanic.handle(player);
		SwimmingImprovementsMechanic.handle(player);
	}
}
