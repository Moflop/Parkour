package mod.arcomit.parkour.content.behavior.armhang.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import mod.arcomit.parkour.content.context.ParkourContext;
import mod.arcomit.parkour.content.context.WallMovementData;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 垂挂动画修改器：作为协调者，统筹 State 更新并将 Math 计算出的帧数据应用到指定骨骼上。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class ArmhangPlayerAnimModifier extends AbstractModifier {

	private final Player player;
	private final ArmhangAnimState animState = new ArmhangAnimState();
	private int lastUpdateTick = -1;

	/** 当前渲染帧的预计算结果，同一帧内的多个骨骼复用此数据，避免重复计算 */
	private ArmhangAnimMath.FrameData currentFrameData;

	/**
	 * @param player 绑定的玩家实例，不可为 null
	 */
	public ArmhangPlayerAnimModifier(Player player) {
		this.player = player;
	}

	/**
	 * 在每一帧骨骼渲染开始前，一次性计算所有骨骼的插值偏移数据。
	 * 若当前不在悬挂状态（armhangDir 为 null），帧数据置空，后续骨骼变换将被跳过。
	 */
	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);

		WallMovementData wallMovementData =
				ParkourContext.get(this.player).wall();
		Direction armhangDir = wallMovementData.getArmhang();

		if (armhangDir != null) {
			this.currentFrameData =
					ArmhangAnimMath.calculate(this.player, this.animState,
							state.getPartialTick(),
							armhangDir.toYRot());
		} else {
			this.currentFrameData = null;
		}
	}

	/**
	 * 每个游戏 tick 更新一次动画状态机（而非每个渲染帧），避免状态跳帧。
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
	 * 将预计算的帧数据叠加到指定骨骼的旋转变换上。
	 * <p>
	 * 当前影响头部（朝向）、身体（Y 旋转+弹跳）、左臂和右臂（摆动+下垂）。
	 * 若帧数据为空（非悬挂状态），直接返回原始骨骼不做修改。
	 *
	 * @param bone 要变换的骨骼，不可为 null
	 * @return 叠加了悬挂动画偏移的骨骼，不可为 null
	 */
	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		bone = super.get3DTransform(bone);

		if (this.currentFrameData == null) {
			return bone;
		}

		String boneName = bone.getName();
		switch (boneName) {
			case "head":
				bone.rotX += this.currentFrameData.headRotX;
				bone.rotY += this.currentFrameData.headRotY;
				break;
			case "body":
				bone.rotY += this.currentFrameData.bodyRotY;
				bone.positionY += this.currentFrameData.bodyPosY;
				break;
			case "left_arm":
				bone.rotX += this.currentFrameData.leftArmRotX;
				bone.rotY += this.currentFrameData.leftArmRotY;
				bone.rotZ += this.currentFrameData.leftArmRotZ;
				break;
			case "right_arm":
				bone.rotX += this.currentFrameData.rightArmRotX;
				bone.rotY += this.currentFrameData.rightArmRotY;
				bone.rotZ += this.currentFrameData.rightArmRotZ;
				break;
		}

		return bone;
	}
}
