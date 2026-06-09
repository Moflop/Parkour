package mod.arcomit.parkour.content.behavior.speedvault.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 速过 (Speed Vault) 动画修改器：协调 State 更新并将 Math 数据应用到骨骼上。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class SpeedVaultAnimModifier extends AbstractModifier {

	private final Player player;
	private final int totalDuration;
	private final int fadeOutDuration;

	private final SpeedVaultAnimState animState = new SpeedVaultAnimState();
	private SpeedVaultAnimMath.FrameData currentFrameData;

	/**
	 * @param player        动画所属玩家，不能为 null
	 * @param totalDuration 动画总 tick 数，溢出阶段自动取总时长的 10%（至少 1 tick）
	 */
	public SpeedVaultAnimModifier(Player player, int totalDuration) {
		this.player = player;
		this.totalDuration = totalDuration;
		this.fadeOutDuration = Math.max(1, (int) (totalDuration * 0.10f));
	}

	/**
	 * 每帧动画开始前，调用 {@link SpeedVaultAnimMath#calculate} 计算出当前帧的完整骨骼数据， 缓存到
	 * {@code currentFrameData} 供后续 {@code get3DTransform} 使用。
	 */
	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		this.currentFrameData = SpeedVaultAnimMath.calculate(this.player, this.animState,
				state.getPartialTick(), this.totalDuration, this.fadeOutDuration);
	}

	/**
	 * 每帧推进动画计时器，保证动画只随时间前进而不受帧率影响。
	 */
	@Override
	public void tick(AnimationData state) {
		super.tick(state);
		this.animState.tick(this.player);
	}

	/**
	 * 将当前帧的骨骼数据应用到指定骨骼上。
	 *
	 * <p>"head" 骨骼直接覆写旋转值；四肢骨骼在溢出阶段
	 * 以 {@code vanillaWeight} 为权重向原版动画插值，其余阶段保持手部自然下垂。
	 *
	 * @param bone 待修改的骨骼，不能为 null
	 * @return 修改后的骨骼（与原 bone 为同一对象）
	 */
	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		bone = super.get3DTransform(bone);
		if (this.currentFrameData == null) {
			return bone;
		}

		String boneName = bone.getName();

		// 头部覆盖
		if (boneName.equals("head")) {
			bone.rotX = this.currentFrameData.headRotX;
			bone.rotY = this.currentFrameData.headRotY;
			bone.rotZ = this.currentFrameData.headRotZ;
			return bone;
		}

		// 四肢原版接管
		if (this.currentFrameData.applyVanillaOverride) {
			float vanillaWeight = this.currentFrameData.vanillaWeight;
			switch (boneName) {
				case "left_leg":
					bone.rotX = Mth.lerp(vanillaWeight, bone.rotX,
							this.currentFrameData.leftLegRotX);
					bone.rotY = Mth.lerp(vanillaWeight, bone.rotY, 0f);
					bone.rotZ = Mth.lerp(vanillaWeight, bone.rotZ, 0f);
					break;
				case "right_leg":
					bone.rotX = Mth.lerp(vanillaWeight, bone.rotX,
							this.currentFrameData.rightLegRotX);
					bone.rotY = Mth.lerp(vanillaWeight, bone.rotY, 0f);
					bone.rotZ = Mth.lerp(vanillaWeight, bone.rotZ, 0f);
					break;
				case "left_arm":
					bone.rotX = Mth.lerp(vanillaWeight, bone.rotX,
							this.currentFrameData.leftArmRotX);
					bone.rotY = Mth.lerp(vanillaWeight, bone.rotY, 0f);
					bone.rotZ = Mth.lerp(vanillaWeight, bone.rotZ, 0f);
					break;
				case "right_arm":
					bone.rotX = Mth.lerp(vanillaWeight, bone.rotX,
							this.currentFrameData.rightArmRotX);
					bone.rotY = Mth.lerp(vanillaWeight, bone.rotY, 0f);
					bone.rotZ = Mth.lerp(vanillaWeight, bone.rotZ, 0f);
					break;
			}
		}

		return bone;
	}
}
