package mod.arcomit.parkour.content.behavior.wallslide.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 滑墙动画修改器 —— 协调状态机、骨骼计算和骨骼覆写。
 *
 * <p>每帧 setupAnim 计算骨骼数据，tick 推进动画状态机，
 * get3DTransform 将计算结果覆写到 head、left_arm、right_arm、body 骨骼。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class WallSlidePlayerAnimModifier extends AbstractModifier {

	private final Player player;
	private final WallSlideAnimState animState = new WallSlideAnimState();
	private int lastUpdateTick = -1;

	private WallSlideAnimMath.FrameData currentFrameData;

	/**
	 * @param player 动画所属玩家，不能为 null
	 */
	public WallSlidePlayerAnimModifier(Player player) {
		this.player = player;
	}

	/**
	 * 每帧动画开始前计算当前帧骨骼数据，供 get3DTransform 使用。
	 */
	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		this.currentFrameData = WallSlideAnimMath.calculate(this.player, this.animState,
				state.getPartialTick());
	}

	/**
	 * 每游戏 tick 推进动画状态机（背对/面朝墙判定），同一 tick 内不重复推进。
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
	 * <p>head：直接覆写旋转；右/左手臂：直接覆写 X/Y/Z 旋转（仅在有墙体姿态时）；
	 * body：追加 Y 轴位置偏移模拟滑落起伏。
	 *
	 * @param bone 待修改的骨骼，不能为 null
	 * @return 修改后的骨骼（与原 bone 为同一对象），currentFrameData 为 null 或无墙体姿态时原样返回
	 */
	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		bone = super.get3DTransform(bone);
		if (this.currentFrameData == null)
			return bone;

		String boneName = bone.getName();

		if (boneName.equals("head")) {
			bone.rotX = this.currentFrameData.headRotX;
			bone.rotY = this.currentFrameData.headRotY;
			return bone;
		}

		if (!this.currentFrameData.hasWallPose)
			return bone;

		switch (boneName) {
			case "right_arm":
				bone.rotX = this.currentFrameData.rightArmRotX;
				bone.rotY = this.currentFrameData.rightArmRotY;
				bone.rotZ = this.currentFrameData.rightArmRotZ;
				break;
			case "left_arm":
				bone.rotX = this.currentFrameData.leftArmRotX;
				bone.rotY = this.currentFrameData.leftArmRotY;
				bone.rotZ = this.currentFrameData.leftArmRotZ;
				break;
			case "body":
				bone.positionY += this.currentFrameData.bodyPosY;
				break;
		}

		return bone;
	}
}
