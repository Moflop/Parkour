package mod.arcomit.parkour.content.behavior.wallclimb;

import mod.arcomit.parkour.core.sensor.impl.HeadFeetSensor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * 爬墙碰撞检测 —— 委托 {@link HeadFeetSensor} 判断玩家头部和脚部是否同时接触面前墙体。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class WallClimbCollision {

    /**
     * 检查玩家面朝方向是否存在有效攀爬点。
     *
     * <p>通过 {@link HeadFeetSensor#isValidCollision} 验证头部和脚部位置
     * 是否均与墙体碰撞，确保玩家全身贴墙。
     *
     * @param player 目标玩家，不能为 null
     * @return 头部和脚部均接触面朝方向的墙体时返回 true
     */
    public static boolean hasValidClimbPoint(Player player) {
        Direction facing = player.getDirection();
        return HeadFeetSensor.isValidCollision(player, facing);
    }
}
