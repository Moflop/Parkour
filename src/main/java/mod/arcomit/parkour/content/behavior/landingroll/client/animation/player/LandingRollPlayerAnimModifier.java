package mod.arcomit.parkour.content.behavior.landingroll.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 落地翻滚动画修改器 —— 协调{@link LandingRollAnimState}的tick推进和
 * {@link LandingRollAnimMath}的帧计算，将计算结果按骨骼名称分发到对应关节。
 * <p>
 * 只作用于消退过渡阶段（fadeOut），在此阶段之前自定义动画完全主导骨骼。 消退期内按权重将头部和四肢逐步过渡回原版行走/旋转姿态，实现翻滚结束时的自然衔接。
 *
 * @author Mitok
 * @since 2026-06-08
 */
public class LandingRollPlayerAnimModifier extends AbstractModifier {

	private final Player player;
	/** 翻滚动画总时长（刻） */
	private final int totalDuration;
	/** 最后几刻用于原版动画消退过渡 */
	private final int fadeOutDuration;

	private final LandingRollAnimState animState = new LandingRollAnimState();
	/** 当前帧的骨骼目标数据，在setupAnim中计算一次 */
	private LandingRollAnimMath.FrameData currentFrameData;

	/**
	 * @param player          目标玩家
	 * @param totalDuration   翻滚动画总时长（刻）
	 * @param fadeOutDuration 消退过渡时长（刻），建议占总时长的1/4~1/2
	 */
	public LandingRollPlayerAnimModifier(Player player, int totalDuration,
			int fadeOutDuration) {
		this.player = player;
		this.totalDuration = totalDuration;
		this.fadeOutDuration = fadeOutDuration;
	}

	/**
	 * 每帧渲染前统一计算一次骨骼目标数据，避免各骨骼重复计算。
	 */
	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		this.currentFrameData = LandingRollAnimMath.calculate(this.player, this.animState,
				state.getPartialTick(), this.totalDuration, this.fadeOutDuration);
	}

	/**
	 * 将tick逻辑委托给动画状态机。
	 */
	@Override
	public void tick(AnimationData state) {
		super.tick(state);
		this.animState.tick(this.player);
	}

	/**
	 * 按骨骼名称将当前帧的旋转目标以权重线性插值到对应关节。
	 * <ul>
	 *   <li>头部：使用最短路径弧度插值</li>
	 *   <li>四肢：普通线性插值，非X轴旋转归零</li>
	 * </ul>
	 */
	@Override
	public void get3DTransform(@NotNull PlayerAnimBone bone) {
		super.get3DTransform(bone);

		if (this.currentFrameData == null || !this.currentFrameData.shouldApplyVanilla) {
			return;
		}

		String boneName = bone.getName();
		float weight = this.currentFrameData.vanillaWeight;

		switch (boneName) {
			case "head":
				bone.rotation.x = LandingRollAnimMath.rotLerpRadians(weight,
						bone.rotation.x,
						this.currentFrameData.headTargetRotX);
				bone.rotation.y = LandingRollAnimMath.rotLerpRadians(weight,
						bone.rotation.y,
						this.currentFrameData.headTargetRotY);
				bone.rotation.z = LandingRollAnimMath.rotLerpRadians(weight,
						bone.rotation.z,
						this.currentFrameData.headTargetRotZ);
				break;
			case "left_leg":
				bone.rotation.x = Mth.lerp(weight, bone.rotation.x,
						this.currentFrameData.leftLegTargetRotX);
				bone.rotation.y = Mth.lerp(weight, bone.rotation.y, 0f);
				bone.rotation.z = Mth.lerp(weight, bone.rotation.z, 0f);
				break;
			case "right_leg":
				bone.rotation.x = Mth.lerp(weight, bone.rotation.x,
						this.currentFrameData.rightLegTargetRotX);
				bone.rotation.y = Mth.lerp(weight, bone.rotation.y, 0f);
				bone.rotation.z = Mth.lerp(weight, bone.rotation.z, 0f);
				break;
			case "left_arm":
				bone.rotation.x = Mth.lerp(weight, bone.rotation.x,
						this.currentFrameData.leftArmTargetRotX);
				bone.rotation.y = Mth.lerp(weight, bone.rotation.y, 0f);
				bone.rotation.z = Mth.lerp(weight, bone.rotation.z, 0f);
				break;
			case "right_arm":
				bone.rotation.x = Mth.lerp(weight, bone.rotation.x,
						this.currentFrameData.rightArmTargetRotX);
				bone.rotation.y = Mth.lerp(weight, bone.rotation.y, 0f);
				bone.rotation.z = Mth.lerp(weight, bone.rotation.z, 0f);
				break;
		}


	}
}
