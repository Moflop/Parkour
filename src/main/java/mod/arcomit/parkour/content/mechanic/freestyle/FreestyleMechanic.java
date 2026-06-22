package mod.arcomit.parkour.content.mechanic.freestyle;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 水面自由式移动：锁定Y轴速度为0，使玩家悬停在水面上水平滑行。
 * <p>
 * 仅在游泳且未下潜时生效，用于实现水面跑酷——玩家不会因跳跃或下落而沉入水中， 能持续在水面上方自由移动。垂直速度向上的玩家才允许进入自由式，下落中的玩家不触发。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class FreestyleMechanic {

	public static void handle(Player player) {
		if (!canFreestyle(player)) {
			return;
		}

		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, 0, motion.z); // isFalling 的逻辑合并在 canFreestyle 里了
	}

	public static boolean canFreestyle(Player player) {
		if (ParkourChecks.isVanillaState(ParkourContext.get(player)) || !ParkourConfig.enableFreestyle || !player.isSwimming() || player.isUnderWater() || !ParkourChecks.canPerformAction(
				player)) {
			return false;
		}
		return player.getDeltaMovement().y >= 0; // 不是在下落状态
	}
}
