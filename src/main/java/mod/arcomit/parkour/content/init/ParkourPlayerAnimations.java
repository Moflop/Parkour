package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.core.client.animation.player.PlayerAnimation;

/**
 * 跑酷玩家动画常量注册表。
 *
 * <p>集中定义所有跑酷动作对应的 GeckoLib 动画资源。
 * 每个动画由{@link mod.arcomit.parkour.core.client.animation.player.PlayerAnimation}封装，
 * 指向 {@code assets/parkour/animations/player/} 下的 JSON 动画文件。
 *
 * <p>方向性动画（如墙跑左右、速过左右）使用左/右分离的策略，
 * 由状态机根据墙面方向选择对应的动画实例。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourPlayerAnimations {
	public static final PlayerAnimation EMPTY_ANIM =
			new PlayerAnimation(ParkourMod.prefix("empty"));

	public static final PlayerAnimation SLIDE_1 =
			new PlayerAnimation(ParkourMod.prefix("slide_1"));

	public static final PlayerAnimation SLIDE_2 =
			new PlayerAnimation(ParkourMod.prefix("slide_2"));

	public static final PlayerAnimation LANDING_ROLL =
			new PlayerAnimation(ParkourMod.prefix("landing_roll"));

	public static final PlayerAnimation WALL_RUN_LEFT =
			new PlayerAnimation(ParkourMod.prefix("wall_run_left"));
	public static final PlayerAnimation WALL_RUN_RIGHT =
			new PlayerAnimation(ParkourMod.prefix("wall_run_right"));

	public static final PlayerAnimation WALL_CLIMB =
			new PlayerAnimation(ParkourMod.prefix("wall_climb"));

	public static final PlayerAnimation ARMHANG =
			new PlayerAnimation(ParkourMod.prefix("armhang"));

	public static final PlayerAnimation SPEED_VAULT_LEFT =
			new PlayerAnimation(ParkourMod.prefix("speed_vault_left"));
	public static final PlayerAnimation SPEED_VAULT_RIGHT =
			new PlayerAnimation(ParkourMod.prefix("speed_vault_right"));
}
