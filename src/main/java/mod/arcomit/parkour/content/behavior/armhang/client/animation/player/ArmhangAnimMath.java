package mod.arcomit.parkour.content.behavior.armhang.client.animation.player;

import mod.arcomit.parkour.content.behavior.armhang.ArmhangData;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.core.client.animation.player.SurfaceAnimationMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** The body loads and shifts underneath the independent hand constraints. */
public final class ArmhangAnimMath {
	private ArmhangAnimMath() {}

	public static FrameData calculate(Player player, ArmhangAnimState state, float partial,
			float targetYaw, Vec3 normal, Vec3 leftPalm, Vec3 rightPalm, boolean mantle) {
		float phase = Mth.lerp(partial, state.animPhaseO, state.animPhase);
		float amplitude = Mth.lerp(partial, state.amplitudeO, state.amplitude);
		float look = Mth.lerp(partial, state.lookAwayWeightO, state.lookAwayWeight);
		float turn = mantle ? 0 : look * state.currentLookAwaySign * 25 * Mth.DEG_TO_RAD;
		var context = ParkourContext.get(player);
		double age = Math.max(0, context.state().getTicksInState() - 1 + partial);
		double drop = mantle ? 0 : ArmhangPoseMath.catchDrop(age, ArmhangData.get(player).catchImpact);
		double sway = mantle ? 0 : Math.sin(phase) * amplitude * .018;
		Vec3 feet = player.getPosition(partial);
		var rig = mantle ? ArmhangPoseMath.mantleRig(feet, normal, leftPalm, rightPalm, player.getScale(), age)
				: ArmhangPoseMath.hangingRig(feet, normal, leftPalm, rightPalm, player.getScale(), drop, sway, turn);
		float modelYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		Vec3 worldOffset = ArmhangPoseMath.rootOffset(rig, feet, player.getScale());
		Vec3 offset = new Vec3(worldOffset.x * Mth.cos(modelYaw) + worldOffset.z * Mth.sin(modelYaw),
				worldOffset.y, worldOffset.x * Mth.sin(modelYaw) - worldOffset.z * Mth.cos(modelYaw)).scale(16);
		float pitch = Mth.lerp(partial, player.xRotO, player.getXRot()) * Mth.DEG_TO_RAD;
		float headYaw = SurfaceAnimationMath.relativeYaw(player.yHeadRotO,
				player.getYHeadRot(), targetYaw, partial) * Mth.DEG_TO_RAD - turn;
		float progress = mantle ? Mth.clamp((float) age / 14, 0, 1) : 0;
		float pull = (float) Math.sin(progress * Math.PI);
		return new FrameData(rig, ArmhangPoseMath.bodyRotation(rig, modelYaw), offset,
				pitch + (mantle ? pull * .28F : 0), headYaw, 0,
				mantle ? pull * .85F : (.10F + amplitude * .06F * Mth.sin(phase)),
				mantle ? pull * .35F : (.18F - amplitude * .06F * Mth.sin(phase)));
	}

	public record FrameData(ArmhangPoseMath.Rig rig, Vec3 rotation, Vec3 offset,
			float headPitch, float headYaw, float torsoPitch, float leftLegPitch, float rightLegPitch) {}
}
