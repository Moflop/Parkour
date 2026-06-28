package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
import mod.arcomit.parkour.content.behavior.armhang.client.animation.player.ArmhangPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.landingroll.client.animation.player.LandingRollPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.slide.client.animation.player.SlidePlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.speedvault.client.animation.player.SpeedVaultAnimModifier;
import mod.arcomit.parkour.content.behavior.wallclimb.client.animation.player.WallClimbPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.wallrun.client.animation.player.WallRunPlayerAnimModifier;
import mod.arcomit.parkour.content.behavior.wallslide.client.animation.player.WallSlidePlayerAnimModifier;
import mod.arcomit.parkour.content.client.init.ClientParkourRegistries;
import mod.arcomit.parkour.core.client.animation.player.v3.PlayerAnimation;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-06-28
 */
public class ParkourAnimationIds {
	public static final Identifier EMPTY = ParkourMod.prefix("empty");

	public static final Identifier SLIDE_1 = ParkourMod.prefix("slide_1");

	public static final Identifier SLIDE_2 = ParkourMod.prefix("slide_2");

	public static final Identifier LANDING_ROLL = ParkourMod.prefix("landing_roll");

	public static final Identifier WALL_RUN_LEFT = ParkourMod.prefix("wall_run_left");

	public static final Identifier WALL_RUN_RIGHT = ParkourMod.prefix("wall_run_right");

	public static final Identifier WALL_CLIMB = ParkourMod.prefix("wall_climb");

	public static final Identifier ARMHANG = ParkourMod.prefix("armhang");

	public static final Identifier SPEED_VAULT_LEFT = ParkourMod.prefix("speed_vault_left");

	public static final Identifier SPEED_VAULT_RIGHT = ParkourMod.prefix("speed_vault_right");
}
