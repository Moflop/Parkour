package mod.arcomit.parkour.content.action.supportwalljump;

/**
 * 支撑蹬墙跳类型——仅两种有效跳跃方向（简化版，无侧跳）。
 *
 * <ul>
 *   <li>{@code NONE}：不可跳跃</li>
 *   <li>{@code UP}：向上跳，玩家面向垂挂墙面方向（75度夹角内）</li>
 *   <li>{@code VIEW}：向前跳，玩家背对垂挂墙面方向</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public enum SupportWallJumpType {
	NONE, UP, VIEW
}
