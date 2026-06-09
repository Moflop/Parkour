package mod.arcomit.parkour.content.behavior.armhang;

import mod.arcomit.parkour.core.sensor.impl.ArmhangEyeSensor;
import mod.arcomit.parkour.core.sensor.impl.ArmhangTopSensor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 手臂悬挂的碰撞检测工具。
 * <p>
 * 悬挂需要两个条件同时满足：脚底有可站的方块支撑盒（通过 ArmhangTopSensor 检测）， 头部有可抓的墙壁（通过 ArmhangEyeSensor
 * 检测）。此外还要排除地面高度过近的情况， 防止玩家在平地栏杆上误触发悬挂。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ArmhangCollision {
	/**
	 * 脚底离地阈值（单位：格）。
	 * <p>
	 * 悬挂时头部以上 1.02 格有碰撞，栅栏高度 1.5，1.5 - 1.02 = 0.48， 因此玩家站在地面上时不会被误判为可悬挂。
	 */
	private static final double GROUND_CLEARANCE = 0.48;

	/**
	 * 判断玩家脚底是否过于接近地面。
	 * <p>
	 * 以脚底向下 {@value #GROUND_CLEARANCE} 格的范围做碰撞检测。若该区域与方块碰撞，说明离地过近。
	 *
	 * @param player 目标玩家，不可为 null
	 * @return true 表示离地过近不应悬挂
	 */
	public static boolean isTooCloseToGround(Player player) {
		AABB feetBox = player.getBoundingBox();
		AABB checkBox = new AABB(feetBox.minX, feetBox.minY - GROUND_CLEARANCE,
				feetBox.minZ, feetBox.maxX, feetBox.minY, feetBox.maxZ);
		return !player.level().noCollision(player, checkBox);
	}

	/**
	 * 检查玩家在指定方向上是否存在有效的悬挂点。
	 * <p>
	 * 采用双传感器逻辑：眼部传感器（检测头部可抓墙面）和顶部传感器（检测脚底有支撑盒）， 任一传感器返回 true 即认为可以悬挂。这样能兼容不同方块形状（如栅栏、台阶边缘）。
	 *
	 * @param player    目标玩家，不可为 null
	 * @param direction 待检测的悬挂方向，不可为 null
	 * @return true 表示该方向可悬挂
	 */
	public static boolean hasValidHangPoint(Player player, Direction direction) {
		return ArmhangEyeSensor.isValidCollision(player,
				direction) || ArmhangTopSensor.isValidCollision(player, direction);
	}
}
