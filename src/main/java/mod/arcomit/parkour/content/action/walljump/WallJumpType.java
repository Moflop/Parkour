package mod.arcomit.parkour.content.action.walljump;

/**
 * 蹬墙跳类型——根据玩家朝向与墙面方向的关系分类。
 *
 * <ul>
 *   <li>{@code NONE}：不可跳跃（面前有脚手架方块阻挡）</li>
 *   <li>{@code UP}：上跳，沿墙面向上弹起</li>
 *   <li>{@code PARALLEL}：侧跳，玩家朝向与墙面方向夹角在110度内</li>
 *   <li>{@code VIEW}：后跳（向前跳），玩家背对墙面，向后弹开</li>
 * </ul>
 *
 * @author Mitok
 * @since 2026-06-08
 */
public enum WallJumpType {
	NONE, UP, PARALLEL, VIEW
}
