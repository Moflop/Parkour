package mod.arcomit.parkour.content.behavior.wallclimb;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.ParkourConstants;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 爬墙物理计算 —— 施加垂直爬升速度、水平墙面吸附力，并重置跌落状态。
 *
 * <p>吸附力通过 {@link Player#move} 实现，不直接修改速度，以保证物理引擎
 * 能正确检测碰撞。同时保存并恢复 onGround 状态，防止贴墙时意外触发落地逻辑。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallClimbPhysics {

    /**
     * 施加垂直爬升速度和水平墙面吸附。
     *
     * <p>垂直速度由配置项 {@code wallClimbSpeed} 决定；
     * 吸附力将玩家推向面前墙体，防止飘离墙面；
     * 每 tick 重置跌落高度，确保爬墙结束后不会因累计高度而受伤。
     *
     * @param player 目标玩家，不能为 null，速度直接被覆写，位置通过 move 偏移
     */
    public static void applyClimbAndAdhesion(Player player) {
		// 垂直向上爬升
		player.setDeltaMovement(0, ParkourConfig.wallClimbSpeed, 0);

		// 墙面吸附
		Direction facing = player.getDirection();
		Vec3 facingVec = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 adhesionForce = facingVec.scale(ParkourConstants.WALL_ADHESION_FORCE);

		boolean wasOnGround = player.onGround();
		player.move(MoverType.PLAYER, adhesionForce);
		if (wasOnGround) {
			// 恢复 onGround 状态，防止由于水平贴墙导致的意外状态结束
			player.setOnGround(true);
		}

		player.resetFallDistance();
	}
}
