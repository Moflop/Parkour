package mod.arcomit.parkour.content.handler;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.context.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 每Tick推进所有跑酷冷却计时器和窗口倒计时。
 * <p>
 * 统一管理：滑铲冷却、落地翻滚允许窗口、水中推进冷却、垂挂冷却，
 * 以及距上一次跳跃的tick计数（上限{@value #MAX_JUMP_TICK_RECORD}tick=5秒）。冷却值大于0时逐tick递减。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@EventBusSubscriber(modid = ParkourMod.MODID)
public class TimerHandler {
	/** 距上次跳跃的tick计数上限（100tick=5秒），超过此值不再递增 */
	private static final int MAX_JUMP_TICK_RECORD = 100;

	@SubscribeEvent
	public static void tickTimers(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		ParkourContext context = ParkourContext.get(player);
		GroundMovementData groundMovementData = context.ground();
		SwimData swimData = context.swim();

		// 滑铲冷却
		int slideCooldown = groundMovementData.getSlideCooldown();
		if (slideCooldown > 0)
			groundMovementData.setSlideCooldown(slideCooldown - 1);

		// 落地翻滚窗口倒计时
		int rollWindow = groundMovementData.getLandingRollWindow();
		if (rollWindow > 0)
			groundMovementData.setLandingRollWindow(rollWindow - 1);

		// 水中推进冷却
		int swimmingBoostCooldown = swimData.getBoostCooldown();
		if (swimmingBoostCooldown > 0)
			swimData.setBoostCooldown(swimmingBoostCooldown - 1);

		// 距离上一次跳跃的Tick
		if (player.isLocalPlayer()) {
			JumpData jumpData = ParkourContext.get(player).jump();
			int ticksSinceLastJump = jumpData.getTicksSinceLastJump();
			if (ticksSinceLastJump < MAX_JUMP_TICK_RECORD) {
				jumpData.setTicksSinceLastJump(ticksSinceLastJump + 1);
			}
		}

		WallMovementData wallMovementData = context.wall();
		// 垂挂冷却
		int armhangCooldown = wallMovementData.getArmhangCooldown();
		if (armhangCooldown > 0)
			wallMovementData.setArmhangCooldown(armhangCooldown - 1);
	}


}
