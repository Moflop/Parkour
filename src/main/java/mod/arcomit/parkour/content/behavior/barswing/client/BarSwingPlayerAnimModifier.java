package mod.arcomit.parkour.content.behavior.barswing.client;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingData;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingAnchor;
import mod.arcomit.parkour.content.behavior.barswing.BarSwingMath;
import mod.arcomit.parkour.core.client.animation.player.SurfaceAnimationMath;
import mod.arcomit.parkour.utils.Directions;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/** 身体随摆荡自然下垂；双手独立贴杆，第一人称使用镜头外的肩端。 */
public class BarSwingPlayerAnimModifier extends AbstractModifier {
	private final Player player;
	private float angle, headYaw, headPitch, tuck;
	private Vec3 offset = Vec3.ZERO;
	private Vec3 rotation = Vec3.ZERO;
	private BarSwingPoseMath.Rig rig;
	private BarSwingPoseMath.GripOffsets gripOffsets;
	private BarSwingPoseMath.BodyPlacement bodyPlacement;

	public BarSwingPlayerAnimModifier(Player player) {
		this.player = player;
	}

	@Override
	public void setupAnim(AnimationData animation) {
		super.setupAnim(animation);
		var data = BarSwingData.get(player);
		float partial = animation.getPartialTick();
		var frame = BarSwingAnchor.frame(data, partial, true);
		if (player.isLocalPlayer()) {
			angle = (float) Mth.lerp(partial, data.previousAngle, data.angle);
		} else {
			Vec3 relative = player.getPosition(partial).subtract(frame.anchor());
			angle = (float) Math.atan2(relative.dot(frame.forward()), relative.dot(frame.down()));
		}
		float yaw = Directions.horizontalYaw(frame.forward());
		boolean slim = player instanceof AbstractClientPlayer client && client.getSkin().model() == PlayerSkin.Model.SLIM;
		headYaw = Mth.clamp(SurfaceAnimationMath.relativeYaw(player.yRotO, player.getYRot(), yaw, partial), -80, 80) * Mth.DEG_TO_RAD;
		headPitch = Mth.lerp(partial, player.xRotO, player.getXRot()) * Mth.DEG_TO_RAD + angle * 0.6F;
		if (gripOffsets == null) {
			var gripRange = BarSwingGripRange.find(player, data);
			// Choose once in a neutral hanging pose, independent of the entry angle and camera.
			var restingRig = BarSwingPoseMath.rig(frame, frame.anchor().add(frame.down().scale(BarSwingMath.LENGTH)),
					0, player.getScale(), gripRange.min(), gripRange.max(), slim);
			// Remote structure data can arrive after the animation starts; do not lock a provisional frame.
			if (data.bar != null && (data.sourceId == null || data.source != null && data.source.isValid())) {
				bodyPlacement = BarSwingPoseMath.bodyPlacement(restingRig, frame, player.getPosition(partial), angle, headPitch, headYaw);
				gripOffsets = BarSwingPoseMath.gripOffsets(restingRig, frame);
			}
			rig = restingRig;
		}
		if (gripOffsets != null) {
			rig = BarSwingPoseMath.rig(frame, player.getPosition(partial), angle, player.getScale(), slim, gripOffsets, bodyPlacement);
		}
		Vec3 worldOffset = BarSwingPoseMath.rootOffset(rig, player.getPosition(partial), player.getScale());
		float modelYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		rotation = BarSwingPoseMath.bodyRotation(rig, modelYaw);
		offset = new Vec3(worldOffset.x * Mth.cos(modelYaw) + worldOffset.z * Mth.sin(modelYaw), worldOffset.y,
				worldOffset.x * Mth.sin(modelYaw) - worldOffset.z * Mth.cos(modelYaw)).scale(16);
		tuck = Math.max(0, angle) * 0.22F;
	}

	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		bone = super.get3DTransform(bone);
		switch (bone.getName()) {
			case "body" -> {
				bone.rotX += (float) rotation.x;
				bone.rotY += (float) rotation.y;
				bone.rotZ += (float) rotation.z;
				// 身体随抓握高度悬垂，横向位置尽量靠近玩家。
				bone.positionX += (float) offset.x;
				bone.positionY += (float) offset.y;
				bone.positionZ += (float) offset.z;
			}
			case "head" -> { bone.rotY += headYaw; bone.rotX += headPitch; }
			case "left_arm" -> grip(bone, 1);
			case "right_arm" -> grip(bone, -1);
			case "right_leg" -> bone.rotX += tuck;
			case "left_leg" -> bone.rotX += tuck * 0.8F;
		}
		return bone;
	}

	private void grip(PlayerAnimBone bone, int side) {
		if (rig == null) return;
		boolean slim = player instanceof AbstractClientPlayer client && client.getSkin().model() == PlayerSkin.Model.SLIM;
		double centerX = side * (slim ? 0.5 : 1), shoulderY = slim ? 2.5 : 2;
		Vec3 shoulder = rig.shoulder(side);
		var mc = Minecraft.getInstance();
		boolean firstPerson = player == mc.getCameraEntity() && FirstPersonMode.isFirstPersonPass();
		if (firstPerson) {
			var camera = mc.gameRenderer.getMainCamera();
			shoulder = BarSwingPoseMath.cameraShoulder(camera.getPosition(), new Vec3(camera.getLookVector()),
					new Vec3(camera.getUpVector()), new Vec3(camera.getLeftVector()), side);
		}
		var arm = BarSwingPoseMath.arm(rig, shoulder, rig.grip(side), centerX, firstPerson);
		bone.positionX = (float) (arm.pivot().x - side * 5);
		bone.positionY = (float) (shoulderY - arm.pivot().y);
		bone.positionZ = (float) arm.pivot().z;
		bone.rotX = arm.pitch();
		bone.rotY = 0;
		bone.rotZ = arm.roll();
		bone.scaleY = arm.length();
	}
}
