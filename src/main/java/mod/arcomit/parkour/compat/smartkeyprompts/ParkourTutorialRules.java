package mod.arcomit.parkour.compat.smartkeyprompts;

import mod.arcomit.parkour.ParkourConfig;
import mod.arcomit.parkour.compat.DynamicCollisions;
import mod.arcomit.parkour.compat.DynamicSurfaceFacing;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.content.action.swimmingboost.SwimmingBoostEligibilityChecker;
import mod.arcomit.parkour.content.action.walljump.WallJumpEligibilityChecker;
import mod.arcomit.parkour.content.action.walljump.WallJumpType;
import mod.arcomit.parkour.content.action.walljump.WallJumpTypeResolver;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangState;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingData;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingState;
import mod.arcomit.parkour.content.behavior.base.DefaultState;
import mod.arcomit.parkour.content.behavior.crawl.CrawlState;
import mod.arcomit.parkour.content.behavior.slide.SlideState;
import mod.arcomit.parkour.content.behavior.speedvault.SpeedVaultCollision;
import mod.arcomit.parkour.content.behavior.speedvault.SpeedVaultState;
import mod.arcomit.parkour.content.behavior.wallclimb.WallClimbState;
import mod.arcomit.parkour.content.behavior.wallrun.WallRunState;
import mod.arcomit.parkour.content.behavior.wallslide.WallSlideCollision;
import mod.arcomit.parkour.content.behavior.wallslide.WallSlideState;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.init.ParkourTags;
import mod.arcomit.parkour.content.mechanic.climb.ClimbableEdge;
import mod.arcomit.parkour.content.mechanic.entitysupport.EntitySupport;
import mod.arcomit.parkour.content.mechanic.freestyle.FreestyleMechanic;
import mod.arcomit.parkour.core.proxy.ParkourProxies;
import mod.arcomit.parkour.core.sensor.impl.WallJumpSensor;
import mod.arcomit.parkour.core.statemachine.state.IParkourState;
import mod.arcomit.parkour.utils.Directions;
import mod.arcomit.parkour.utils.ParkourChecks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Contextual suggestions only: never executes an action or evaluates state-machine transitions.
 * Collision helpers may refresh their sensor caches, but no movement, cooldown, input or state
 * is changed. Keeping the model free of Smart Key Prompts types also permits diagnostic reads
 * when that optional mod is absent.
 */
public final class ParkourTutorialRules {
    private ParkourTutorialRules() {}

    public enum Binding { JUMP, SNEAK, SPRINT, SLIDE, FORWARD, BACK, LEFT, RIGHT, TUTORIAL }

    public record Key(Binding binding, boolean hold) {}

    /** Alternatives are displayed with a slash; other multi-key tips are combinations. */
    public record Tip(String id, List<Key> keys, boolean alternatives) {
        public Tip {
            keys = List.copyOf(keys);
        }

        public String translationKey() {
            return "parkour.tutorial." + id;
        }
    }

    public record TutorialGroup(String id, List<Tip> tips) {
        public static final TutorialGroup EMPTY = new TutorialGroup("", List.of());

        public TutorialGroup {
            tips = List.copyOf(tips);
        }
    }

    public static TutorialGroup select(LocalPlayer player) {
        if (player == null || !player.isAlive() || !ParkourChecks.canPerformAction(player)) {
            return TutorialGroup.EMPTY;
        }
        ParkourContext context = ParkourContext.get(player);
        if (!ParkourProxies.GET_CLIENT_CONFIG_PROXY.isEnableParkour(context)
                || ParkourChecks.isVanillaState(context)) return TutorialGroup.EMPTY;

        IParkourState state = context.state().getState();
        if (state instanceof BarSwingState) return barSwing(player);
        if (state instanceof ArmhangState) return armhang(player, context);
        if (state instanceof CrawlState) {
            return ParkourConfig.enableCrawl ? group("crawl", tip("crawl_exit", press(Binding.SLIDE)))
                    : TutorialGroup.EMPTY;
        }
        if (state instanceof WallRunState || state instanceof WallClimbState || state instanceof WallSlideState) {
            return activeWall(player, context, state);
        }
        if (!(state instanceof DefaultState)) {
            return canVaultJump(player, context, state)
                    ? group("speed_vault", tip("speed_vault_jump", press(Binding.JUMP)))
                    : TutorialGroup.EMPTY;
        }

        if (player.isInWater() || player.isSwimming()) return water(player, context);
        if (player.isInLava() || !ParkourChecks.canPerformBehavior(player)) return TutorialGroup.EMPTY;
        if (player.onClimbable()) return climbing(player, context);

        // DefaultState can accept several independent inputs in the same environment. A wall
        // or vault suggestion must not hide a slide/crawl that its input transitions still allow.
        List<Tip> tips = new ArrayList<>();
        String id = player.onGround() ? "ground" : "air";
        if (canVaultJump(player, context, state)) {
            id = "speed_vault";
            tips.add(tip("speed_vault_jump", press(Binding.JUMP)));
        }
        if (canPrepareLandingRoll(player)) {
            if (tips.isEmpty()) id = "landing";
            tips.add(tip("landing_roll", press(Binding.SLIDE)));
        }
        TutorialGroup wall = nearbyWall(player, context);
        if (!wall.tips().isEmpty()) {
            if (tips.isEmpty()) id = wall.id();
            tips.addAll(wall.tips());
        }
        if (canEntityJump(player, context)) {
            if (tips.isEmpty()) id = "entity";
            tips.add(tip("entity_jump", press(Binding.JUMP)));
        }
        TutorialGroup movement = movement(player, context);
        if (tips.isEmpty()) id = movement.id();
        tips.addAll(movement.tips());
        return group(id, tips);
    }

    private static TutorialGroup barSwing(LocalPlayer player) {
        if (!ParkourConfig.enableBarSwing || !BarSwingData.get(player).active) return TutorialGroup.EMPTY;
        List<Tip> tips = new ArrayList<>();
        tips.add(tip("bar_pump", hold(Binding.FORWARD)));
        tips.add(tip("bar_brake", hold(Binding.BACK)));
        if (!sneaking(player)) tips.add(tip("bar_jump", press(Binding.JUMP)));
        tips.add(tip("release", press(Binding.SNEAK)));
        return group("bar_swing", tips);
    }

    private static TutorialGroup armhang(LocalPlayer player, ParkourContext context) {
        if (!ParkourConfig.enableArmhang) return TutorialGroup.EMPTY;
        Direction wall = context.wall().getArmhang();
        if (wall == null) return TutorialGroup.EMPTY;
        List<Tip> tips = new ArrayList<>();
        // Same angle as ClientArmhangMovement/SupportWallJumpTypeResolver, without capturing a grip.
        boolean facesLedge = Directions.isFacing(player, DynamicSurfaceMotion.readOnlyWallNormal(player, wall), 75.0F);
        if (facesLedge) {
            tips.add(new Tip("armhang_shimmy", List.of(hold(Binding.LEFT), hold(Binding.RIGHT)), true));
        }
        if (ParkourConfig.enableSupportWallJump && !sneaking(player)) {
            tips.add(tip(facesLedge ? "armhang_up" : "armhang_jump", press(Binding.JUMP)));
        }
        tips.add(tip("release", press(Binding.SNEAK)));
        return group("armhang", tips);
    }

    private static TutorialGroup activeWall(LocalPlayer player, ParkourContext context, IParkourState state) {
        List<Tip> tips = new ArrayList<>();
        String id;
        if (state instanceof WallRunState) {
            id = "wall_run";
            if (ParkourConfig.enableWallRun) tips.add(tip(id, hold(Binding.FORWARD), hold(Binding.JUMP)));
        } else if (state instanceof WallClimbState) {
            id = "wall_climb";
            if (ParkourConfig.enableWallClimb) tips.add(tip(id, hold(Binding.FORWARD), hold(Binding.JUMP)));
        } else {
            id = "wall_slide";
            if (ParkourConfig.enableWallSlide) tips.add(tip(id, hold(Binding.JUMP)));
        }
        if (canWallJump(player, context, state)) tips.add(tip("wall_jump", press(Binding.JUMP)));
        return group(id, tips);
    }

    private static TutorialGroup water(LocalPlayer player, ParkourContext context) {
        List<Tip> tips = new ArrayList<>();
        if (!player.isSwimming() && player.isInWater() && ParkourConfig.enableShallowSwimming
                && ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.hasEnoughFoodToStartSprinting(player)
                && !sneaking(player)) {
            tips.add(tip("swim_start", hold(Binding.SPRINT), hold(Binding.FORWARD)));
        }
        if (SwimmingBoostEligibilityChecker.check(player, context.swim())) {
            tips.add(tip("swim_boost", press(Binding.SLIDE)));
        }
        // The action's extra player.jumping condition is the input being recommended.
        if (FreestyleMechanic.canFreestyle(player)) tips.add(tip("swim_jump", press(Binding.JUMP)));
        return group("water", tips);
    }

    private static TutorialGroup climbing(LocalPlayer player, ParkourContext context) {
        List<Tip> tips = new ArrayList<>();
        // Scaffolding uses different vanilla sneak controls and is excluded by the controller.
        if (!player.getInBlockState().isScaffolding(player)) {
            boolean up = ParkourConfig.enableUpClimbSpeedIncrease && ParkourConfig.upClimbSpeedMultiplier > 1;
            boolean down = ParkourConfig.enableDownClimbSpeedIncrease && ParkourConfig.downClimbSpeedMultiplier > 1;
            boolean canAccelerateUp = up && ClimbableEdge.upwardBoostFactor(player) > 0;
            boolean canAccelerateDown = down && !player.onGround()
                    && player.getDeltaMovement().y < 0 && !ParkourProxies.INPUT_PROXY.getJumping(player);
            if (!sneaking(player) && (canAccelerateUp || canAccelerateDown)) {
                // Sprint modifies vanilla climbing intent in either direction; jump is not
                // part of the accelerator binding and would incorrectly exclude wall ascent.
                tips.add(tip("climb_accelerate", hold(Binding.SPRINT)));
            }
            if (up || down) tips.add(tip("climb_brake", hold(Binding.SNEAK)));
        }
        if (canWallJump(player, context, context.state().getState())) {
            tips.add(tip("wall_jump", press(Binding.JUMP)));
        }
        return group("climbing", tips);
    }

    private static TutorialGroup nearbyWall(LocalPlayer player, ParkourContext context) {
        List<Tip> tips = new ArrayList<>();
        boolean grounded = DynamicCollisions.isOnGround(player);
        boolean falling = player.getDeltaMovement().y < 0;
        boolean recentJump = context.jump().getTicksSinceLastJump() <= WallRunState.MAX_TICKS_SINCE_JUMP;
        boolean canSprint = player.isSprinting()
                || ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.hasEnoughFoodToStartSprinting(player);
        if ((grounded || falling && recentJump) && !ParkourChecks.isFallUnsafe(player)
                && !sneaking(player) && canSprint) {
            if (ParkourConfig.enableWallRun && ParkourConfig.wallRunDuration > 0
                    && !DynamicSurfaceFacing.blockedAhead(player, ParkourTags.Blocks.COMMON_IGNORED_BLOCKS)) {
                Direction wall = DynamicSurfaceFacing.runDirection(player);
                if (wall != null && (grounded || wall != context.wall().getRunCollision())) {
                    tips.add(tip("wall_run", hold(Binding.SPRINT), hold(Binding.FORWARD), hold(Binding.JUMP)));
                }
            }
            if (tips.isEmpty() && ParkourConfig.enableWallClimb && ParkourConfig.wallClimbDuration > 0) {
                Direction wall = DynamicSurfaceFacing.climbDirection(player);
                if (wall != null && (grounded || wall != context.wall().getClimb())) {
                    tips.add(tip("wall_climb", hold(Binding.SPRINT), hold(Binding.FORWARD), hold(Binding.JUMP)));
                }
            }
        }
        if (!grounded && falling && ParkourConfig.enableWallSlide
                && WallSlideCollision.findAvailableWallDirection(player, context.wall()) != null) {
            tips.add(tip("wall_slide", hold(Binding.JUMP)));
        }
        if (canWallJump(player, context, context.state().getState())) {
            tips.add(tip("wall_jump", press(Binding.JUMP)));
        }
        return group("wall", tips);
    }

    private static TutorialGroup movement(LocalPlayer player, ParkourContext context) {
        List<Tip> tips = new ArrayList<>();
        String id = player.onGround() ? "ground" : "air";
        if (player.onGround() && ParkourConfig.enableSpeedVault && !player.isCrouching() && !sneaking(player)
                && SpeedVaultCollision.hasValidVaultPoint(player, player.getDirection())) {
            id = "speed_vault";
            tips.add(tip("speed_vault", hold(Binding.FORWARD), press(Binding.JUMP)));
        }
        boolean moving = ParkourProxies.LOCAL_PLAYER_SERVICES_PROXY.isMoving(player);
        float forward = ParkourProxies.INPUT_PROXY.getForwardImpulse(player);
        if (player.onGround() && !moving && CrawlState.meetsBaseConditions(player)) {
            tips.add(tip("crawl", press(Binding.SLIDE)));
        }
        // Match DefaultState's input predicates and the destination states' shared base
        // conditions. Slide can start in safe air; only backward dodge requires the ground.
        if (moving && context.ground().getSlideCooldown() <= 0 && SlideState.meetsBaseConditions(player)) {
            if (forward >= 0) tips.add(tip("slide", press(Binding.SLIDE)));
            else if (player.onGround()) tips.add(tip("dodge", press(Binding.SLIDE)));
        }
        return group(id, tips);
    }

    private static boolean canVaultJump(LocalPlayer player, ParkourContext context, IParkourState state) {
        return ParkourConfig.enableSpeedVault && ParkourConfig.enableSpeedVaultJump
                && (state instanceof SpeedVaultState || state instanceof DefaultState
                        && context.jump().getSpeedVaultJumpWindow() > 0)
                && ParkourChecks.canPerformBehavior(player) && !player.onClimbable()
                && !player.isInWater() && !player.isInLava() && !player.isCrouching() && !sneaking(player);
    }

    private static boolean canEntityJump(LocalPlayer player, ParkourContext context) {
        return ParkourConfig.enableEntityJump && !player.onGround() && !context.jump().isEntityJumpUsed()
                && context.jump().getTicksSinceLastJump() > 0 && !player.isCrouching() && !sneaking(player)
                && player.getAttributeValue(Attributes.JUMP_STRENGTH) + player.getJumpBoostPower() > 0
                && EntitySupport.hasJumpContact(player, ParkourConfig.entityJumpReach);
    }

    /** Mirrors the read-only portions of WallJumpAction, including its same-wall guard. */
    private static boolean canWallJump(LocalPlayer player, ParkourContext context, IParkourState state) {
        if (!ParkourConfig.enableWallJump || sneaking(player)
                || !WallJumpEligibilityChecker.check(player, state)) return false;
        Direction wall;
        if (state instanceof WallRunState) wall = context.wall().getRunCollision();
        else if (state instanceof WallSlideState) wall = context.wall().getSlide();
        else {
            List<Direction> walls = new ArrayList<>(4);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (WallJumpSensor.isValidCollision(player, direction)) walls.add(direction);
            }
            wall = Directions.findClosest(player, walls);
        }
        if (wall == null) return false;
        // resolve() calls wallNormal(), which may capture a dynamic movement anchor. Keep its
        // pure 110-degree type decision here and query the identical frame without that write.
        WallJumpType type = WallJumpTypeResolver.isBlockInFrontOfPlayer(player) ? WallJumpType.UP
                : Directions.isFacing(player, DynamicSurfaceMotion.readOnlyWallNormal(player, wall), 110.0F)
                        ? WallJumpType.PARALLEL : WallJumpType.VIEW;
        int previous = switch (type) {
            case UP -> context.jump().getLastUpJumpRaw();
            case PARALLEL -> context.jump().getLastParallelJumpRaw();
            case VIEW -> context.jump().getLastViewJumpRaw();
            case NONE -> wall.get3DDataValue();
        };
        return previous != wall.get3DDataValue();
    }

    private static boolean canPrepareLandingRoll(LocalPlayer player) {
        // Warn throughout an unsafe fall. The player chooses when to press; neither nearby
        // collision geometry nor an already armed roll window should hide this reminder.
        double safeFall = ParkourConfig.enableSafeFallHeightIncrease ? ParkourConfig.safeFallHeight : 3;
        return ParkourConfig.enableLandingRoll && !player.onGround() && player.fallDistance > safeFall;
    }

    private static boolean sneaking(LocalPlayer player) {
        return ParkourProxies.INPUT_PROXY.getShiftKeyDown(player);
    }

    private static Key press(Binding binding) { return new Key(binding, false); }
    private static Key hold(Binding binding) { return new Key(binding, true); }
    private static Tip tip(String id, Key... keys) { return new Tip(id, List.of(keys), false); }
    private static TutorialGroup group(String id, Tip... tips) { return group(id, List.of(tips)); }
    private static TutorialGroup group(String id, List<Tip> tips) {
        return tips.isEmpty() ? TutorialGroup.EMPTY : new TutorialGroup(id, tips);
    }
}
