package mod.arcomit.parkour.content.init;

import mod.arcomit.parkour.ParkourMod;
// import mod.arcomit.parkour.v2.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.content.behavior.backstep.BackstepState;
import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.behavior.crawl.CrawlState;
import mod.arcomit.parkour.content.behavior.landingroll.LandingRollState;
import mod.arcomit.parkour.content.behavior.slide.SlideState;
import mod.arcomit.parkour.content.behavior.speedvault.SpeedVaultState;
import mod.arcomit.parkour.content.behavior.wallclimb.WallClimbState;
import mod.arcomit.parkour.content.behavior.wallrun.WallRunState;
import mod.arcomit.parkour.content.behavior.wallslide.WallSlideState;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 跑酷状态注册集。
 *
 * <p>将所有跑酷动作状态（滑铲、墙跑、蹬墙跳等）注册到{@link ParkourRegistries#PARKOUR_STATE_REGISTRY}中。
 * 每个状态对应一种跑酷动作，包含进入条件、Tick行为、退出逻辑和客户端动画配置。
 * 状态实例通过 {@link net.neoforged.neoforge.registries.DeferredHolder} 延迟初始化，
 * 通过 {@link IParkourState#getId()} 返回的 {@link net.minecraft.resources.ResourceLocation} 作为注册键。
 *
 * <p>默认状态 {@link #DEFAULT} 代表原版无跑酷动作的普通状态。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class ParkourStates {
	private static final DeferredRegister<IParkourState> PARKOUR_STATES =
			DeferredRegister.create(ParkourRegistries.PARKOUR_STATE_REGISTRY,
					ParkourMod.MODID);
	public static final DeferredHolder<IParkourState, BackstepState> BACKSTEP =
			PARKOUR_STATES.register("backstep", BackstepState::new);
	public static final DeferredHolder<IParkourState, LandingRollState> LANDING_ROLL =
			PARKOUR_STATES.register("landing_roll", LandingRollState::new);	public static final DeferredHolder<IParkourState, DefaultState> DEFAULT =
			PARKOUR_STATES.register("default", DefaultState::new);

	public static void register(IEventBus modEventBus) {
		PARKOUR_STATES.register(modEventBus);
	}
	public static final DeferredHolder<IParkourState, CrawlState> CRAWL =
			PARKOUR_STATES.register("crawl", CrawlState::new);



	public static final DeferredHolder<IParkourState, SlideState> SLIDE =
			PARKOUR_STATES.register("slide", SlideState::new);



	public static final DeferredHolder<IParkourState, WallSlideState> WALL_SLIDE =
			PARKOUR_STATES.register("wall_slide", WallSlideState::new);

	public static final DeferredHolder<IParkourState, WallRunState> WALL_RUN =
			PARKOUR_STATES.register("wall_run", WallRunState::new);

	public static final DeferredHolder<IParkourState, WallClimbState> WALL_CLIMB =
			PARKOUR_STATES.register("wall_climb", WallClimbState::new);

	public static final DeferredHolder<IParkourState, ArmhangState> ARMHANG =
			PARKOUR_STATES.register("armhang", ArmhangState::new);

	public static final DeferredHolder<IParkourState, SpeedVaultState> SPEED_VAULT =
			PARKOUR_STATES.register("speed_vault", SpeedVaultState::new);


}
