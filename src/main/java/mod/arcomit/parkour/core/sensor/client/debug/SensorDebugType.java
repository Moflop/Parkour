package mod.arcomit.parkour.core.sensor.client.debug;

/**
 * 传感器调试可视化类型枚举，控制客户端渲染模式下应显示哪种传感器的碰撞盒。
 * <ul>
 *   <li>{@link #NONE} -- 隐藏全部调试渲染</li>
 *   <li>{@link #WALL_SLIDE} -- 仅渲染滑墙动作的 HeadFeet 碰撞盒</li>
 *   <li>{@link #WALL_RUN} -- 仅渲染跑墙动作左右两侧的 HeadFeet 碰撞盒</li>
 *   <li>{@link #WALL_CLIMB} -- 仅渲染爬墙动作正前方的 HeadFeet 碰撞盒</li>
 *   <li>{@link #WALL_JUMP} -- 仅渲染蹬墙跳动作的 WallJump 碰撞盒</li>
 *   <li>{@link #ARMHANG_EYE} -- 仅渲染手臂悬挂（视线高度）碰撞盒</li>
 *   <li>{@link #ARMHANG_TOP} -- 仅渲染手臂悬挂（头顶高度）碰撞盒</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public enum SensorDebugType {
	NONE, WALL_SLIDE, WALL_RUN, WALL_CLIMB, WALL_JUMP, ARMHANG_EYE, ARMHANG_TOP
}
