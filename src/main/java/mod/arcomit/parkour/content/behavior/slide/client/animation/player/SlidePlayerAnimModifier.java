package mod.arcomit.parkour.content.behavior.slide.client.animation.player;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import mod.arcomit.parkour.utils.BoneRotator;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 滑铲动画修改器 —— 将{@link SlideAnimMath}计算的帧数据应用到玩家骨骼。
 * <p>
 * 头部：覆盖头部Y轴旋转为头部与身体的偏航差，避免滑铲时头部锁定在身体前方。 身体：施加X轴横向位置偏移，产生周期性抖动效果。
 *
 * @author Mitok
 * @since 2026-06-08
 */
@OnlyIn(Dist.CLIENT)
public class SlidePlayerAnimModifier extends AbstractModifier {
	private final Player player;
	private final SlideAnimState animState = new SlideAnimState();
	/** 当前帧的计算结果，由setupAnim填充 */
	private SlideAnimMath.SlideFrameData currentFrame;

	public SlidePlayerAnimModifier(Player player) {
		this.player = player;
	}

	/**
	 * 每帧渲染前计算一次骨骼数据。
	 */
	@Override
	public void setupAnim(AnimationData state) {
		super.setupAnim(state);
		this.currentFrame = SlideAnimMath.calculate(this.player, this.animState,
				state.getPartialTick());
	}

	/**
	 * 推进动画状态机。
	 */
	@Override
	public void tick(AnimationData state) {
		super.tick(state);
		this.animState.tick(this.player);
	}

	/**
	 * 按骨骼名称分发当前帧数据：
	 * <ul>
	 *   <li>head —— 应用相对旋转（头部偏航角）</li>
	 *   <li>body —— X轴横向偏移</li>
	 * </ul>
	 * 未命中的骨骼保持原样。
	 */
	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		bone = super.get3DTransform(bone);
		if (this.currentFrame == null)
			return bone;

		switch (bone.getName()) {
			case "head" -> BoneRotator.applyRelativeRotation(bone,
					currentFrame.headYawRad, 0.0f);

			case "body" -> bone.positionX += currentFrame.bodyPosX;

			// 未来如果需要加手臂晃动，只需在这里继续添加 case 即可
		}

		return bone;
	}
}
