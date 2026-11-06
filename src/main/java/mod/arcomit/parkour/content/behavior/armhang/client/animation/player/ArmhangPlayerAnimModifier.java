package mod.arcomit.parkour.content.behavior.armhang.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import mod.arcomit.parkour.content.behavior.armhang.ArmhangData;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/** Both views solve the model's palms against the same material points on the ledge. */
public final class ArmhangPlayerAnimModifier extends AbstractModifier {
	private final Player player;
	private final boolean mantle;
	private final ArmhangAnimState animState = new ArmhangAnimState();
	private int lastUpdateTick = -1;
	private ArmhangAnimMath.FrameData frame;

	public ArmhangPlayerAnimModifier(Player player) { this(player, false); }
	public ArmhangPlayerAnimModifier(Player player, boolean mantle) {
		this.player = player;
		this.mantle = mantle;
	}

	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		var data = ArmhangData.get(player);
		float partial = state.getPartialTick();
		Vec3 left = ArmhangData.renderLeftHand(player, partial);
		Vec3 right = ArmhangData.renderRightHand(player, partial);
		if (data.grip == null || left == null || right == null) {
			frame = null;
			return;
		}
		Vec3 normal = data.grip.normal(partial);
		float yaw = Directions.horizontalYaw(normal);
		frame = ArmhangAnimMath.calculate(player, animState, partial, yaw, normal, left, right, mantle);
	}

	@Override
	public void tick(AnimationData state) {
		super.tick(state);
		if (player.tickCount != lastUpdateTick) {
			lastUpdateTick = player.tickCount;
			animState.tick(player);
		}
	}

	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		bone = super.get3DTransform(bone);
		if (frame == null) return bone;
		switch (bone.getName()) {
			case "body" -> {
				bone.rotX += (float) frame.rotation().x;
				bone.rotY += (float) frame.rotation().y;
				bone.rotZ += (float) frame.rotation().z;
				bone.positionX += (float) frame.offset().x;
				bone.positionY += (float) frame.offset().y;
				bone.positionZ += (float) frame.offset().z;
			}
			case "head" -> { bone.rotX += frame.headPitch(); bone.rotY += frame.headYaw(); }
			case "torso" -> bone.rotX += frame.torsoPitch();
			case "left_arm" -> applyArm(bone, 1);
			case "right_arm" -> applyArm(bone, -1);
			case "left_leg" -> bone.rotX += frame.leftLegPitch();
			case "right_leg" -> bone.rotX += frame.rightLegPitch();
		}
		return bone;
	}

	private void applyArm(PlayerAnimBone bone, int side) {
		boolean slim = player instanceof AbstractClientPlayer client && client.getSkin().model() == PlayerSkin.Model.SLIM;
		Vec3 shoulder = frame.rig().shoulder(side, slim);
		var mc = Minecraft.getInstance();
		boolean firstPerson = player == mc.getCameraEntity() && FirstPersonMode.isFirstPersonPass();
		if (firstPerson) {
			var camera = mc.gameRenderer.getMainCamera();
			shoulder = ArmhangPoseMath.cameraShoulder(camera.getPosition(), new Vec3(camera.getLookVector()),
					new Vec3(camera.getUpVector()), new Vec3(camera.getLeftVector()), side);
		}
		double centerX = side * (slim ? .5 : 1), shoulderY = slim ? 2.5 : 2;
		var arm = ArmhangPoseMath.arm(frame.rig(), shoulder, frame.rig().palm(side), centerX, firstPerson);
		bone.positionX = (float) (arm.pivot().x - side * 5);
		bone.positionY = (float) (shoulderY - arm.pivot().y);
		bone.positionZ = (float) arm.pivot().z;
		bone.rotX = arm.pitch();
		bone.rotY = 0;
		bone.rotZ = arm.roll();
		bone.scaleX = bone.scaleZ = firstPerson ? ArmhangPoseMath.FIRST_PERSON_THICKNESS : 1;
		bone.scaleY = arm.length();
	}
}
