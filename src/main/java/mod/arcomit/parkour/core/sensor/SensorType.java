package mod.arcomit.parkour.core.sensor;

/**
 * 传感器类型枚举，每种类型对应一种跑酷动作的环境判定需求。
 * <ul>
 *   <li>{@link #HEAD_FEET} -- 滑墙/跑墙/爬墙：检测玩家头部和脚部前方是否有可碰撞方块</li>
 *   <li>{@link #WALL_JUMP} -- 蹬墙跳：检测玩家脚部高度前方是否有墙面</li>
 *   <li>{@link #ARMHANG_EYE} -- 手臂悬挂（视线高度）：检测玩家视线高度前方是否有横梁，上方通畅且下方有支撑才能悬挂</li>
 *   <li>{@link #ARMHANG_TOP} -- 手臂悬挂（头顶高度）：检测玩家头顶高度前方是否有横梁，同样要求上方通畅且下方有支撑</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public enum SensorType {
	HEAD_FEET, WALL_JUMP, ARMHANG_EYE, ARMHANG_TOP
}
