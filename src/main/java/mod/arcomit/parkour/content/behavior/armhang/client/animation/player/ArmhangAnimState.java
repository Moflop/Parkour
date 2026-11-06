package mod.arcomit.parkour.content.behavior.armhang.client.animation.player;

import mod.arcomit.parkour.compat.DynamicCollisionSource;
import mod.arcomit.parkour.compat.DynamicSurfaceMotion;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Tick interpolation for the suspended body; the hands have independent world constraints. */
public final class ArmhangAnimState {
	public float animPhaseO, animPhase, amplitudeO, amplitude;
	public float lookAwayWeightO, lookAwayWeight, currentLookAwaySign = 1;
	public float armhangYawO, armhangYaw;
	private boolean firstTick = true;
	private Vec3 previousPosition;
	private DynamicCollisionSource previousSource;

	public void tick(Player player) {
		animPhaseO = animPhase;
		amplitudeO = amplitude;
		lookAwayWeightO = lookAwayWeight;
		armhangYawO = armhangYaw;
		Direction direction = ParkourContext.get(player).wall().getArmhang();
		if (direction == null) {
			firstTick = true;
			previousPosition = null;
			return;
		}
		var grip = ArmhangData.get(player).grip;
		Vec3 normal = grip == null ? DynamicSurfaceMotion.renderWallNormal(player, direction, 1) : grip.normal(1);
		float targetYaw = Directions.horizontalYaw(normal);
		float relativeLook = Mth.wrapDegrees(player.getYRot() - targetYaw);
		float lookTarget = ArmhangPoseMath.smoothStep((Math.abs(relativeLook) - 55) / 60);
		lookAwayWeight = Mth.lerp(.3F, lookAwayWeight, lookTarget);
		// Crossing directly behind the player must not swap the supporting arm every tick.
		if (lookAwayWeight < .2F || Math.abs(relativeLook) < 145) {
			currentLookAwaySign = relativeLook >= 0 ? 1 : -1;
		}
		if (firstTick || DynamicSurfaceMotion.hasRenderSurface(player, direction)) {
			armhangYaw = armhangYawO = targetYaw;
		} else {
			armhangYaw = Mth.rotLerp(.35F, armhangYaw, targetYaw);
		}

		DynamicCollisionSource source = grip == null ? DynamicSurfaceMotion.attachedSource(player, direction) : grip.source();
		Vec3 motion = Vec3.ZERO;
		if (previousPosition != null && sameSource(source, previousSource)) {
			Vec3 previousWorld = source == null ? previousPosition : source.toWorld(previousPosition);
			motion = player.position().subtract(previousWorld);
		}
		previousPosition = source == null ? player.position() : source.toLocal(player.position());
		previousSource = source;
		firstTick = false;
		Vec3 tangent = new Vec3(normal.z, 0, -normal.x).normalize();
		double along = Mth.clamp(motion.dot(tangent), -.25, .25);
		// A translating/rotating ledge does not make stationary hands walk along it.
		animPhase += (float) (along * Math.PI * 2 / .8);
		float targetAmplitude = (float) Mth.clamp(Math.abs(along) / .12, 0, 1);
		amplitude = Mth.lerp(.35F, amplitude, targetAmplitude);
	}

	private static boolean sameSource(DynamicCollisionSource left, DynamicCollisionSource right) {
		return left == right || left != null && right != null && left.identity().equals(right.identity());
	}
}
