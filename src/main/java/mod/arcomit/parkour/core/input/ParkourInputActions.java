package mod.arcomit.parkour.core.input;

/**
 * 跑酷自定义按键动作枚举。
 * <p>
 * 定义跑酷模组中可由玩家主动触发的额外操作。每个枚举值对应一个独立的按键绑定项，
 * 通过 NeoForge 的按键映射系统注册到原版操作选项中。
 * <p>
 * {@code NONE} 作为哨兵值，表示无操作。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public enum ParkourInputActions {
	/** 哨兵值，表示没有触发任何跑酷动作。 */
	NONE,
	/** 滑铲动作，玩家按下绑定键时进入滑铲状态。 */
	SLIDE
}
