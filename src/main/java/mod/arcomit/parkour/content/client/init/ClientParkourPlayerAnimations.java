package mod.arcomit.parkour.content.client.init;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.armhang.client.animation.player.ArmhangPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.landingroll.client.animation.player.LandingRollPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.slide.client.animation.player.SlidePlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.speedvault.client.animation.player.SpeedVaultAnimModifier;
import mod.arcomit.parkour.content.behavior.wallclimb.client.animation.player.WallClimbPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.wallrun.client.animation.player.WallRunPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.wallslide.client.animation.player.WallSlidePlayerAnimModifier;
import mod.arcomit.parkour.content.init.ParkourAnimationIds;
import mod.arcomit.parkour.core.client.animation.player.PlayerAnimation;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跑酷玩家动画常量注册表。
 *
 * <p>集中定义所有跑酷动作对应的 GeckoLib 动画资源。
 * 每个动画由{@link PlayerAnimation}封装， 指向 {@code assets/parkour/animations/player/} 下的 JSON 动画文件。
 *
 * <p>方向性动画（如墙跑左右、速过左右）使用左/右分离的策略，
 * 由状态机根据墙面方向选择对应的动画实例。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ClientParkourPlayerAnimations {

	public static final Map<Identifier, PlayerAnimation> PLAYER_ANIMATIONS = new HashMap<>();

	static {
		PLAYER_ANIMATIONS.put(ParkourAnimationIds.EMPTY,
				new PlayerAnimation(
						ParkourMod.prefix("empty"),
						3, 5,
						(player, variant) -> List.of(
								new WallSlidePlayerAnimModifier(player)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.SLIDE_1,
				new PlayerAnimation(
						ParkourMod.prefix("slide_1"),
						3, 5,
						(player, variant) -> List.of(
								new SlidePlayerAnimModifier(player)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.SLIDE_2,
				new PlayerAnimation(
						ParkourMod.prefix("slide_2"),
						3, 5,
						(player, variant) -> List.of(
								new SlidePlayerAnimModifier(player)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.LANDING_ROLL,
				new PlayerAnimation(
						ParkourMod.prefix("landing_roll"),
						3, 5,
						(player, variant) -> List.of(
								new LandingRollPlayerAnimModifier(player, 18, 6)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.WALL_RUN_LEFT,
				new PlayerAnimation(
						ParkourMod.prefix("wall_run_left"),
						3, 5,
						(player, variant) -> List.of(
								new WallRunPlayerAnimModifier(player, variant == 0)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.WALL_RUN_RIGHT,
				new PlayerAnimation(
						ParkourMod.prefix("wall_run_right"),
						3, 5,
						(player, variant) -> List.of(
								new WallRunPlayerAnimModifier(player, variant == 0)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.WALL_CLIMB,
				new PlayerAnimation(
						ParkourMod.prefix("wall_climb"),
						3, 5,
						(player, variant) -> List.of(
								new WallClimbPlayerAnimModifier(player)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.ARMHANG,
				new PlayerAnimation(
						ParkourMod.prefix("armhang"),
						3, 5,
						(player, variant) -> List.of(
								new ArmhangPlayerAnimModifier(player)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.SPEED_VAULT_LEFT,
				new PlayerAnimation(
						ParkourMod.prefix("speed_vault_left"),
						2, 2,
						(player, variant) -> List.of(
								new SpeedVaultAnimModifier(player, 10)
						)
				)
		);

		PLAYER_ANIMATIONS.put(ParkourAnimationIds.SPEED_VAULT_RIGHT,
				new PlayerAnimation(
						ParkourMod.prefix("speed_vault_right"),
						2, 2,
						(player, variant) -> List.of(
								new SpeedVaultAnimModifier(player, 10)
						)
				)
		);
	}
}
