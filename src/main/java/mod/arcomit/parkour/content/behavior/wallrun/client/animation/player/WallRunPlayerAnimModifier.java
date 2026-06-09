package mod.arcomit.parkour.content.behavior.wallrun.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 跑墙动画修改器 —— 协调状态机、骨骼计算和骨骼覆写。
 *
 * <p>每帧 setupAnim 计算骨骼数据，tick 推进动画状态机，
 * get3DTransform 将计算结果应用到 head、body、四肢、torso 骨骼。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class WallRunPlayerAnimModifier extends AbstractModifier {

	private final Player player;
	/** 墙体是否在玩家左侧，决定手臂和躯干支撑方向 */
	private final boolean isWallOnLeft;
	private final WallRunAnimState animState = new WallRunAnimState();
	private int lastUpdateTick = -1;

	private WallRunAnimMath.FrameData currentFrameData;

	/**
	 * @param player       动画所属玩家，不能为 null
	 * @param isWallOnLeft 墙体是否在玩家左侧
	 */
	public WallRunPlayerAnimModifier(Player player, boolean isWallOnLeft) {
		this.player = player;
		this.isWallOnLeft = isWallOnLeft;
	}

	/**
	 * 每帧动画开始前计算当前帧骨骼数据，供后续 get3DTransform 使用。
	 */
	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		this.currentFrameData = WallRunAnimMath.calculate(this.player, this.animState,
				state.getPartialTick(), this.isWallOnLeft);
	}

	/**
	 * 每游戏 tick 推进动画状态机，同一 tick 内不重复推进。
	 */
	@Override
	public void tick(AnimationData state) {
		super.tick(state);
		if (this.player.tickCount != this.lastUpdateTick) {
			this.lastUpdateTick = this.player.tickCount;
			this.animState.tick(this.player);
		}
	}

	/**
	 * 将计算好的骨骼数据应用到动画骨骼上。
	 *
	 * <p>head：覆盖旋转值；body：追加偏航补偿和 Y 轴起伏；
	 * 有运动时四肢和躯干追加跑动摆幅（靠墙侧手臂做支撑状）。
	 *
	 * @param bone 待修改的骨骼，不能为 null
	 * @return 修改后的骨骼（与原 bone 为同一对象），currentFrameData 为 null 时原样返回
	 */
	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		bone = super.get3DTransform(bone);
		if (this.currentFrameData == null)
			return bone;

		String boneName = bone.getName();

		if (boneName.equals("head")) {
			bone.rotX = WallRunAnimMath.rotLerpRadians(bone.rotX,
					this.currentFrameData.headRotX);
			bone.rotY = WallRunAnimMath.rotLerpRadians(bone.rotY,
					this.currentFrameData.headRotY);
			bone.rotZ = WallRunAnimMath.rotLerpRadians(bone.rotZ,
					this.currentFrameData.headRotZ);
			return bone;
		}

		if (boneName.equals("body")) {
			bone.rotY += this.currentFrameData.bodyRotY;
		}

		if (this.currentFrameData.hasMotion) {
			switch (boneName) {
				case "left_leg":
					bone.rotX += this.currentFrameData.leftLegRotX;
					break;
				case "right_leg":
					bone.rotX += this.currentFrameData.rightLegRotX;
					break;
				case "left_arm":
					bone.rotX += this.currentFrameData.leftArmRotX;
					break;
				case "right_arm":
					bone.rotX += this.currentFrameData.rightArmRotX;
					break;
				case "torso":
					bone.rotY += this.currentFrameData.torsoRotY;
					break;
				case "body":
					bone.positionY += this.currentFrameData.bodyPosY;
					break;
			}
		}

		return bone;
	}
}
